/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayNet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.vis.netvis.visNet;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.vis.netvis.DisplayableLinkI;
import org.matsim.utils.vis.netvis.DisplayableNetI;

/**
 * @author gunnar
 *
 */
public class DisplayNet implements BasicNetI, DisplayableNetI {

	// -------------------- MEMBER VARIABLES --------------------

	private double minEasting;
	private double maxEasting;
	private double minNorthing;
	private double maxNorthing;

	private final Map<IdI, DisplayNode> nodes = new TreeMap<IdI, DisplayNode>();
	private final Map<IdI, DisplayLink> links = new TreeMap<IdI, DisplayLink>();

	// -------------------- CONSTRUCTION --------------------

	public DisplayNet(NetworkLayer layer) {
		// first create nodes
		for (BasicNodeI node : layer.getNodes().values()) {
			DisplayNode node2 = new DisplayNode(node.getId(), this);
			node2.setCoord(((Node) node).getCoord());
			nodes.put(node2.getId(), node2);
		}

		// second, create links
		for (BasicLinkI link : layer.getLinks().values()) {
			DisplayLink link2 = new DisplayLink(link.getId(), this);

			BasicNodeI from = this.getNodes().get(link.getFromNode().getId());
			from.addOutLink(link2);
			link2.setFromNode(from);

			BasicNodeI to = this.getNodes().get(link.getToNode().getId());
			to.addInLink(link2);
			link2.setToNode(to);

			link2.setLength_m(((Link) link).getLength());
			link2.setLanes(((Link) link).getLanes());

			links.put(link2.getId(), link2);
		}

		// third, build/complete the network
		this.build();
	}

	// -------------------- IMPLEMENTATION OF BasicNetworkI --------------------

	public void connect() {
	}

	public Map<IdI, ? extends DisplayNode> getNodes() {
		return nodes;
	}

	public Map<IdI, ? extends DisplayableLinkI> getLinks() {
		return links;
	}

	// -------------------- OVERRIDING OF TrafficNet --------------------

	public void build() {
		for (DisplayableLinkI link : getLinks().values()) {
			link.build();
		}

		minEasting = Double.POSITIVE_INFINITY;
		maxEasting = Double.NEGATIVE_INFINITY;
		minNorthing = Double.POSITIVE_INFINITY;
		maxNorthing = Double.NEGATIVE_INFINITY;

		for (DisplayNode node : getNodes().values()) {
			minEasting = Math.min(minEasting, node.getEasting());
			maxEasting = Math.max(maxEasting, node.getEasting());
			minNorthing = Math.min(minNorthing, node.getNorthing());
			maxNorthing = Math.max(maxNorthing, node.getNorthing());
		}
	}

	public double minEasting() {
		return minEasting;
	}

	public double maxEasting() {
		return maxEasting;
	}

	public double minNorthing() {
		return minNorthing;
	}

	public double maxNorthing() {
		return maxNorthing;
	}

}