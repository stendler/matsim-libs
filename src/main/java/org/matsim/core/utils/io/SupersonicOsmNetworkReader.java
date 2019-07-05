package org.matsim.core.utils.io;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@Log
public class SupersonicOsmNetworkReader {

	public static final String HIGHWAY = "highway";

	private static final Set<String> reverseTags = new HashSet<>(Arrays.asList("-1", "reverse"));
	private static final Set<String> oneWayTags = new HashSet<>(Arrays.asList("yes", "true", "1"));
	private static final Set<String> notOneWayTags = new HashSet<>(Arrays.asList("no", "false", "0"));


	private final Map<String, LinkProperties> linkProperties = LinkProperties.createLinkProperties();
	private final BiPredicate<Coord, Integer> filter;
	private final CoordinateTransformation coordinateTransformation;

	@Getter
	private final Network network;

	SupersonicOsmNetworkReader(Network network, CoordinateTransformation coordinateTransformation) {
		this(network, coordinateTransformation, (coord, level) -> true);
	}

	SupersonicOsmNetworkReader(Network network, CoordinateTransformation coordinateTransformation, BiPredicate<Coord, Integer> filter) {
		this.filter = filter;
		this.network = network;
		this.coordinateTransformation = coordinateTransformation;
	}

	@SuppressWarnings("WeakerAccess")
	public void read(Path inputFile) throws FileNotFoundException, OsmInputException {

		var handler = parse(inputFile);
		log.info("starting convertion \uD83D\uDE80");
		convert(handler.ways, handler.nodes);

	}

	private void convert(Collection<OsmWay> ways, Map<Long, OsmNode> nodes) {

		// This introduces a side effect, since the mark nodes method will mutate the cache's state. But I don't know
		// how to properly do this
		NodesCache cache = new NodesCache(nodes.keySet());

		log.info("filter for highways and mark necessary nodes");
		var filteredWays = ways.parallelStream()
				.filter(way -> {

					var map = OsmModelUtil.getTagsAsMap(way);
					return map.containsKey(HIGHWAY) && linkProperties.containsKey(map.get(HIGHWAY));
				})
				.peek(way -> this.markNodes(cache, way))
				.collect(Collectors.toList());
		// we need to collect here to finish the counting of visited nodes before actually creating matsim links

		log.info("create links");
		// convert ways to links
		filteredWays.parallelStream()
				.flatMap(way -> this.createNecessaryLinks(cache, nodes, way).stream())
				.forEach(this::addLinkToNetwork);
		log.info("done");
	}

	private NodesAndWays parse(Path inputFile) throws FileNotFoundException, OsmInputException {
		/*var reader = new PbfReader(inputFile.toFile(), true);
		var handler = new ParsingHandler();
		reader.setHandler(handler);
		reader.read();
		return handler;*/
		log.info("start reading ways");

		var waysReader = new PbfReader(inputFile.toFile(), false);
		var waysHandler = new WayHandler();
		waysReader.setHandler(waysHandler);
		waysReader.read();

		log.info("finished reading ways.");
		log.info("Kept " + waysHandler.ways.size() + "/" + waysHandler.counter + "ways");
		log.info("Marked " + waysHandler.nodeIds.size() + " nodes to be kept");
		log.info("starting to read nodes");

		var nodesReader = new PbfReader(inputFile.toFile(), false);
		var nodesHandler = new NodesHandler(waysHandler.getNodeIds());
		nodesReader.setHandler(nodesHandler);
		nodesReader.read();

		log.info("finished reading nodes");

		var result = new NodesAndWays(nodesHandler.nodes, waysHandler.ways);
		nodesHandler = null;
		waysHandler = null;
		return result;
	}

	private void markNodes(NodesCache cache, OsmWay way) {
		for (int i = 0; i < way.getNumberOfNodes(); i++) {
			long nodeId = way.getNodeId(i);
			cache.markNode(nodeId);
		}
	}

	private Collection<Link> createNecessaryLinks(NodesCache cache, Map<Long, OsmNode> nodes, OsmWay way) {
		long fromNodeId = way.getNodeId(0);

		List<Link> result = new ArrayList<>();
		for (int i = 1, subId = 1; i < way.getNumberOfNodes(); i++, subId += 2) {
			// take all nodes which are used by more than one way e.g. intersections. Definitely take the last node
			//TODO there is no loop detection yet
			if (cache.isNodeUsedByMoreThanOneWay(way.getNodeId(i)) || i == way.getNumberOfNodes() - 1) {


				OsmNode fromOsmNode = nodes.get(fromNodeId);
				OsmNode toOsmNode = nodes.get(way.getNodeId(i));
				Coord fromCoord = coordinateTransformation.transform(new Coord(fromOsmNode.getLongitude(), fromOsmNode.getLatitude()));
				Coord toCoord = coordinateTransformation.transform(new Coord(toOsmNode.getLongitude(), toOsmNode.getLatitude()));

				// we should have filtered for these before, so no testing whether they are present
				Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
				String highwayType = tags.get(HIGHWAY);
				LinkProperties properties = linkProperties.get(highwayType);

				if (filter.test(fromCoord, properties.level) || filter.test(toCoord, properties.level)) {
					Node fromNode = createNode(fromCoord, fromOsmNode.getId());
					Node toNode = createNode(toCoord, toOsmNode.getId());
					//if the way id is 1234 we will get a link id like 12340001, this is necessary because we need to generate unique
					// ids. The osm wiki says ways have no more than 2000 nodes which means that i will never be creater 1999.

					if (!isOnewayReverse(tags)) {
						Link forewardLink = createLink(way.getId() * 10000 + subId, way.getId(), tags, fromNode, toNode);
						result.add(forewardLink);
					}

					if (!isOneway(tags, properties)) {
						Link reverseLink = createLink(way.getId() * 10000 + subId + 1, way.getId(), tags, toNode, fromNode);
						result.add(reverseLink);
					}
				}
				fromNodeId = toOsmNode.getId();
			}
		}
		return result;
	}

	private Node createNode(Coord coord, long nodeId) {
		Id<Node> id = Id.createNodeId(nodeId);
		return network.getFactory().createNode(id, coord);
	}

	private Link createLink(long linkId, long wayId, Map<String, String> tags, Node fromNode, Node toNode) {

		String highwayType = tags.get(HIGHWAY);
		LinkProperties properties = linkProperties.get(highwayType);

		Link link = network.getFactory().createLink(Id.createLinkId(linkId), fromNode, toNode);
		link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
		link.setFreespeed(getFreespeed(tags, link.getLength(), properties));
		link.setCapacity(getLaneCapacity(link.getLength(), properties));
		link.setNumberOfLanes(getNumberOfLanes(tags, properties));
		link.getAttributes().putAttribute(NetworkUtils.ORIGID, wayId);
		link.getAttributes().putAttribute(NetworkUtils.TYPE, highwayType);
		// the original code has 'setOrModifyLinkAttributes' here
		return link;
	}

	private boolean isOneway(Map<String, String> tags, LinkProperties properties) {

		if (tags.containsKey("oneway")) {
			String tag = tags.get("oneway");
			if (oneWayTags.contains(tag)) return true;
			if (reverseTags.contains(tag) || notOneWayTags.contains(tag)) return false;
		}

		// no oneway tag
		if ("roundabout".equals(tags.get("junction"))) return true;

		// just return the default for this type of link
		return properties.oneway;
	}

	private boolean isOnewayReverse(Map<String, String> tags) {

		if (tags.containsKey("oneway")) {
			String tag = tags.get("oneway");
			if (oneWayTags.contains(tag) || notOneWayTags.contains(tag)) return false;
			return reverseTags.contains(tag);
		}
		return false;
	}

	private double getFreespeed(Map<String, String> tags, double linkLenght, LinkProperties properties) {
		if (tags.containsKey("maxspeed")) {
			String tag = tags.get("maxspeed");
			double speed = parseSpeedTag(tag, properties);
			double urbanSpeedFactor = speed <= 51 / 3.6 ? 0.5 : 1.0; // assume for links with max speed lower than 51km/h to be in urban areas. Reduce speed to reflect traffic lights and suc
			return speed * urbanSpeedFactor;
		} else {
			// some weir locig invented by kai and ihab. if streets are less than 300m and of level primary, secondary or tertiary make the freesped
			// higher the longer the link is...
			return properties.level > LinkProperties.LEVEL_MOTORWAY && properties.level < LinkProperties.LEVEL_SMALLER_THAN_TERTIARY && linkLenght < 300 ? ((10 + (properties.freespeed - 10) / 300 * linkLenght) / 3.6) : properties.freespeed;
		}
	}

	private double parseSpeedTag(String tag, LinkProperties properties) {

		try {
			if (tag.endsWith("mph"))
				return Double.parseDouble(tag.replace("mph", "").trim()) * 1.609344 / 3.6;
			else
				return Double.parseDouble(tag) / 3.6;
		} catch (NumberFormatException e) {
			//System.out.println("Could not parse maxspeed tag: " + tag + " ignoring it");
		}
		return properties.freespeed;
	}

	private double getLaneCapacity(double linkLength, LinkProperties properties) {
		double capacityFactor = linkLength < 100 ? 2 : 1;
		return properties.laneCapacity * capacityFactor;
	}

	private double getNumberOfLanes(Map<String, String> tags, LinkProperties properties) {
		if (tags.containsKey("lanes")) {
			String tag = tags.get("lanes");

			try {
				double totalNumberOfLanes = Double.parseDouble(tag);
				// TODO do more magic with foreward and backward lanes here
				return totalNumberOfLanes;
			} catch (NumberFormatException e) {
				return properties.lanesPerDirection;
			}

		}
		return properties.lanesPerDirection;
	}

	private synchronized void addLinkToNetwork(Link link) {

		//we have to test for presence
		if (!network.getNodes().containsKey(link.getFromNode().getId())) {
			network.addNode(link.getFromNode());
		}
		if (!network.getNodes().containsKey(link.getToNode().getId())) {
			network.addNode(link.getToNode());
		}
		if (!network.getLinks().containsKey(link.getId())) {
			network.addLink(link);
		} else {
			throw new RuntimeException("ups");
		}
	}

	@FunctionalInterface
	public interface OsmFilter {

		boolean filter(Coord coord, int linkLevel);
	}

	private static class NodesCache {

		private ConcurrentMap<Long, Integer> nodes;

		NodesCache(Collection<Long> nodes) {
			this.nodes = nodes.parallelStream().collect(Collectors.toConcurrentMap(node -> node, node -> 0));
		}

		void markNode(long nodeId) {
			nodes.merge(nodeId, 1, Integer::sum);
		}

		int getCount(long nodeId) {
			return nodes.get(nodeId);
		}

		boolean isNodeUsedByMoreThanOneWay(long nodeId) {
			return nodes.get(nodeId) > 1;
		}
	}

	@RequiredArgsConstructor
	public static class LinkProperties {

		public static final int LEVEL_MOTORWAY = 1;
		public static final int LEVEL_TRUNK = 2;
		public static final int LEVEL_PRIMARY = 3;
		public static final int LEVEL_SECONDARY = 4;
		public static final int LEVEL_TERTIARY = 5;
		public static final int LEVEL_SMALLER_THAN_TERTIARY = 6; // choose a better name

		final int level;
		final double lanesPerDirection;
		final double freespeed;
		final double freespeedFactor;
		final double laneCapacity;
		final boolean oneway;

		static LinkProperties createMotorway() {
			return new LinkProperties(LEVEL_MOTORWAY, 2, 120 / 3.6, 1.0, 2000, true);
		}

		static LinkProperties createMotorwayLink() {
			return new LinkProperties(LEVEL_MOTORWAY, 1, 80 / 3.6, 1, 1500, true);
		}

		static LinkProperties createTrunk() {
			return new LinkProperties(LEVEL_TRUNK, 1, 80 / 3.6, 1, 2000, false);
		}

		static LinkProperties createTrunkLink() {
			return new LinkProperties(LEVEL_TRUNK, 1, 50 / 3.6, 1, 1500, false);
		}

		static LinkProperties createPrimary() {
			return new LinkProperties(LEVEL_PRIMARY, 1, 80 / 3.6, 1, 1500, false);
		}

		static LinkProperties createPrimaryLink() {
			return new LinkProperties(LEVEL_PRIMARY, 1, 60 / 3.6, 1, 1500, false);
		}

		static LinkProperties createSecondary() {
			return new LinkProperties(LEVEL_SECONDARY, 1, 30 / 3.6, 1, 800, false);
		}

		static LinkProperties createSecondaryLink() {
			return new LinkProperties(LEVEL_SECONDARY, 1, 30 / 3.6, 1, 800, false);
		}

		static LinkProperties createTertiary() {
			return new LinkProperties(LEVEL_TERTIARY, 1, 25 / 3.6, 1, 600, false);
		}

		static LinkProperties createTertiaryLink() {
			return new LinkProperties(LEVEL_TERTIARY, 1, 25 / 3.6, 1, 600, false);
		}

		static LinkProperties createUnclassified() {
			return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 15 / 3.6, 1, 600, false);
		}

		static LinkProperties createResidential() {
			return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 15 / 3.6, 1, 600, false);
		}

		static LinkProperties createLivingStreet() {
			return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 10 / 3.6, 1, 300, false);
		}

		static Map<String, LinkProperties> createLinkProperties() {
			Map<String, LinkProperties> result = new HashMap<>();
			result.put("motorway", createMotorway());
			result.put("motorway_link", createMotorwayLink());
			result.put("trunk", createTrunk());
			result.put("trunk_link", createTrunkLink());
			result.put("primary", createPrimary());
			result.put("primary_link", createPrimaryLink());
			result.put("secondary", createSecondary());
			result.put("secondary_link", createSecondaryLink());
			result.put("tertiary", createTertiary());
			result.put("tertiary_link", createTertiaryLink());
			result.put("unclassified", createUnclassified());
			result.put("residential", createResidential());
			result.put("living_street", createLivingStreet());
			return result;
		}
	}

	@Getter
	private static class WayHandler implements OsmHandler {

		private final Set<OsmWay> ways = new HashSet<>();
		private final Set<Long> nodeIds = new HashSet<>();

		private int counter = 0;

		@Override
		public void handle(OsmBounds osmBounds) throws IOException {

		}

		@Override
		public void handle(OsmNode osmNode) throws IOException {

		}

		@Override
		public void handle(OsmWay osmWay) throws IOException {
			counter++;
			if (isStreet(osmWay)) {
				ways.add(osmWay);
				for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
					nodeIds.add(osmWay.getNodeId(i));
				}
			}
		}

		@Override
		public void handle(OsmRelation osmRelation) throws IOException {

		}

		@Override
		public void complete() throws IOException {

		}

		private boolean isStreet(OsmWay way) {
			var map = OsmModelUtil.getTagsAsMap(way);
			return map.containsKey(HIGHWAY);
		}
	}

	@RequiredArgsConstructor
	private static class NodesHandler implements OsmHandler {

		private final Set<Long> nodeIdsOfInterest;
		private final Map<Long, OsmNode> nodes = new HashMap<>();

		@Override
		public void handle(OsmBounds osmBounds) throws IOException {

		}

		@Override
		public void handle(OsmNode osmNode) throws IOException {

			if (nodeIdsOfInterest.contains(osmNode.getId())) {
				nodes.put(osmNode.getId(), osmNode);
			}
		}

		@Override
		public void handle(OsmWay osmWay) throws IOException {

		}

		@Override
		public void handle(OsmRelation osmRelation) throws IOException {

		}

		@Override
		public void complete() throws IOException {

		}
	}

	@RequiredArgsConstructor
	@Getter
	private static class NodesAndWays {
		private final Map<Long, OsmNode> nodes;
		private final Set<OsmWay> ways;
	}

	private static class ParsingHandler implements OsmHandler {

		private Map<Long, OsmNode> nodes = new HashMap<>();
		private Set<OsmWay> ways = new HashSet<>();
		private boolean isAtNodes = false;

		Map<Long, OsmNode> getNodes() {
			return nodes;
		}

		Set<OsmWay> getWays() {
			return ways;
		}

		@Override
		public void handle(OsmBounds osmBounds) {
			//ignore
		}

		@Override
		public void handle(OsmNode osmNode) {
			nodes.put(osmNode.getId(), osmNode);
		}

		@Override
		public void handle(OsmWay osmWay) {
			ways.add(osmWay);
			if (!isAtNodes) {
				log.info("first way");
				log.info(nodes.size() + " nodes");
			}
		}

		@Override
		public void handle(OsmRelation osmRelation) {
			//ignore
		}

		@Override
		public void complete() {
			//ignore
		}
	}
}
