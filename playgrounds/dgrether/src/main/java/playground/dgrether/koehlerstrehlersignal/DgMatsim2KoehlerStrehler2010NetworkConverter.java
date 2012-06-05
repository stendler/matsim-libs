/* *********************************************************************** *
 * project: org.matsim.*
 * DgMatsim2KoehlerStrehler2010ModelConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.v20.LaneData20;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;

/**
 * @author dgrether
 *
 */
public class DgMatsim2KoehlerStrehler2010NetworkConverter {
	
	private static final Logger log = Logger.getLogger(DgMatsim2KoehlerStrehler2010NetworkConverter.class);

	public static Id convertLinkId2FromCrossingNodeId(Id linkId){
		return new IdImpl(linkId.toString() + "11");
	}
	
	public static Id convertLinkId2ToCrossingNodeId(Id linkId){
		return new IdImpl(linkId.toString() + "99");
	}
	
	public static Id convertFromLinkIdToLinkId2LightId(Id fromLinkId, Id fromLaneId, Id toLinkId){
		if (fromLaneId == null){
			return new IdImpl(fromLinkId.toString()  + "55" + toLinkId.toString());
		}
		return new IdImpl(fromLinkId.toString() + "66" + fromLaneId.toString() + "55" + toLinkId.toString());
	}

	
	private int cycle = 60;
	private Id programId = new IdImpl("1");

	private DgKSNetwork dgNetwork;
	private double timeInterval;

	public DgKSNetwork convertNetworkLanesAndSignals(Scenario sc, double startTime, double endTime) {
		log.info("Converting network ...");
		Network net = sc.getNetwork();
		this.timeInterval = endTime - startTime;
		this.dgNetwork = this.convertNetwork(net, sc.getScenarioElement(LaneDefinitions20.class), sc.getScenarioElement(SignalsData.class));
		log.info("Network converted.");
		return this.dgNetwork ;
	}

	/*
	 * codierung:
	 *   fromLink -> toLink zwei nodes + 1 light
	 */
	private DgKSNetwork convertNetwork(Network net, LaneDefinitions20 lanes, SignalsData signalsData) {
		DgKSNetwork ksnet = new DgKSNetwork();
		/* create a crossing for each node, same id
		 */
		this.convertNodes2Crossings(ksnet, net);
		/*
		 * convert all links to streets (same id) and create the from and to 
		 * nodes (ids generated from link id) for the already created corresponding 
		 * crossing 
		 */
		this.convertLinks2Streets(ksnet, net);

		//collect all ids of links that are signalized
		Set<Id> signalizedLinks = this.getSigalizedLinkIds(signalsData.getSignalSystemsData());
		//loop over links and create layout of crossing
		for (Link link : net.getLinks().values()){
			//prepare some objects/data
			DgCrossing crossing = ksnet.getCrossings().get(link.getToNode().getId()); //The node id of the matsim network is the crossing id
			Link backLink = this.getBackLink(link);
			Id backLinkId = (backLink == null) ?  null : backLink.getId();
			DgCrossingNode inLinkToNode = crossing.getNodes().get(convertLinkId2ToCrossingNodeId(link.getId()));
			LanesToLinkAssignment20 l2l = lanes.getLanesToLinkAssignments().get(link.getId());
			//create crossing layout
			if (signalizedLinks.contains(link.getId())){
				log.debug("link: " + link.getId() + " is signalized...");
				SignalSystemData system = this.getSignalSystem4SignalizedLinkId(signalsData.getSignalSystemsData(), link.getId());
				this.createCrossing4SignalizedLink(crossing, link, inLinkToNode, backLinkId, l2l, system, signalsData);
			}
			else {
				log.debug("link: " + link.getId() + " not signalized...");
				this.createCrossing4NotSignalizedLink(crossing, link, inLinkToNode, backLinkId, l2l);
			}
		}
		return ksnet;
	}

	
	private void convertNodes2Crossings(DgKSNetwork dgnet, Network net){
		for (Node node : net.getNodes().values()){
			DgCrossing crossing = new DgCrossing(node.getId());
			dgnet.addCrossing(crossing);
			DgProgram program = new DgProgram(this.programId);
			crossing.addProgram(program);
			program.setCycle(this.cycle);
		}
	}
	
	private void convertLinks2Streets(DgKSNetwork ksnet, Network net){
		for (Link link : net.getLinks().values()){
			DgCrossing fromNodeCrossing = ksnet.getCrossings().get(link.getFromNode().getId());
			DgCrossingNode fromNode = new DgCrossingNode(convertLinkId2FromCrossingNodeId(link.getId()));
			fromNodeCrossing.addNode(fromNode);
			DgCrossing toNodeCrossing = ksnet.getCrossings().get(link.getToNode().getId());
			DgCrossingNode toNode = new DgCrossingNode(convertLinkId2ToCrossingNodeId(link.getId()));
			toNodeCrossing.addNode(toNode);
			DgStreet street = new DgStreet(link.getId(), fromNode, toNode);
			long fs = Math.round((link.getLength() / link.getFreespeed()));
			street.setCost(fs);
			double capacity = link.getCapacity() / net.getCapacityPeriod() * this.timeInterval;
			street.setCapacity(capacity);
			ksnet.addStreet(street);
		}
	}
	

	private Tuple<SignalPlanData, SignalGroupSettingsData> getPlanAndSignalGroupSettings4Signal(Id signalSystemId, Id signalId, SignalsData signalsData){
		SignalSystemControllerData controllData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(signalSystemId);
		SignalPlanData signalPlan = controllData.getSignalPlanData().values().iterator().next();
		SignalGroupData signalGroup = DgSignalsUtils.getSignalGroup4SignalId(signalSystemId, signalId, signalsData.getSignalGroupsData());
		return new Tuple<SignalPlanData, SignalGroupSettingsData>(signalPlan, signalPlan.getSignalGroupSettingsDataByGroupId().get(signalGroup.getId()));
	}

	

	private Set<Id> getSigalizedLinkIds(SignalSystemsData signals){
		Map<Id, Set<Id>> signalizedLinksPerSystem = DgSignalsUtils.calculateSignalizedLinksPerSystem(signals);
		Set<Id> signalizedLinks = new HashSet<Id>();
		for (Set<Id> signalizedLinksOfSystem : signalizedLinksPerSystem.values()){
			signalizedLinks.addAll(signalizedLinksOfSystem);
		}
		return signalizedLinks;
	}
	
	/**
	 * 
	 */
	private Id createLights(Id fromLinkId, Id fromLaneId, Id outLinkId, Id backLinkId, DgCrossingNode inLinkToNode, DgCrossing crossing){
		if (backLinkId != null && backLinkId.equals(outLinkId)){
			return null; //do nothing if it is the backlink
		}
		Id lightId = convertFromLinkIdToLinkId2LightId(fromLinkId, fromLaneId, outLinkId);
		log.debug("    light id: " + lightId);
		Id convertedOutLinkId = convertLinkId2FromCrossingNodeId(outLinkId);
		log.debug("    outLinkId : " + outLinkId + " converted id: " + convertedOutLinkId);
		DgCrossingNode outLinkFromNode = crossing.getNodes().get(convertedOutLinkId);
		if (outLinkFromNode == null){
			log.error("Crossing " + crossing.getId() + " has no node with id " + convertedOutLinkId);
			throw new IllegalStateException("outLinkFromNode not found.");
		}
		DgStreet street = new DgStreet(lightId, inLinkToNode, outLinkFromNode);
		crossing.addLight(street);
		return lightId;
	}
	
	
	/**
	 * Maps a signalized MATSim Link's turning moves and signalization to lights and greens, i.e. 1 allowed turning move  => 1 light + 1 green
	 * Turning moves are given by:
	 *   a) the outLinks of the toNode of the Link, if no lanes are given and there are no turning move restrictions set for the Signal
	 *   b) the turning move restrictions of multiple signals attached to the link 
	 *   d) the turing move restrictions of the signal, if it is attached to a lane
	 *   c) the toLinks of the lanes attached to the link, if there are no turning move restrictions for the signal
	 * If there are several signals without turning move restrictions on a link or a lane nothing can be created because this is an inconsistent state of 
	 * the input data:  thus the programs/plans for the signal might be ambiguous, an exception is thrown.
	 * 
	 */
	private void createCrossing4SignalizedLink(DgCrossing crossing, Link link, DgCrossingNode inLinkToNode, Id backLinkId, LanesToLinkAssignment20 l2l, SignalSystemData system, SignalsData signalsData) {
		List<SignalData> signals4Link = this.getSignals4LinkId(system, link.getId());
		DgProgram program = crossing.getPrograms().get(this.programId);
		//first get the outlinks that are controlled by the signal
		for (SignalData signal : signals4Link){
			log.debug("    signal: " + signal.getId() + " system: " + system.getId());
			Id lightId = null;
			if (l2l == null) {
				Set<Id> outLinkIds = new HashSet<Id>();
				if (signals4Link.size() > 1 && (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty())){
					throw new IllegalStateException("more than one signal on one link but no lanes and no turning move restrictions is not allowed");
				}
				else if (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty()){
					outLinkIds.addAll(this.getTurningMoves4LinkWoLanes(link));
				}
				else { // we have turning move restrictions
					outLinkIds = signal.getTurningMoveRestrictions();
				}
				//create lights and green settings
				for (Id outLinkId : outLinkIds){
					log.debug("    outLinkId: " + outLinkId);
					lightId = this.createLights(link.getId(), null, outLinkId, backLinkId, inLinkToNode, crossing);
					log.debug("    created Light " + lightId);
					if (lightId != null){
						Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
						SignalPlanData signalPlan = planGroupSettings.getFirst();
						SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
						this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
					}
				}
			}
			else { //link with lanes
				for (Id laneId : signal.getLaneIds()){
					LaneData20 lane = l2l.getLanes().get(laneId);
					if (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty()){ //no turning move restrictions for signal -> outlinks come from lane
						for (Id outLinkId : lane.getToLinkIds()){
							log.debug("    outLinkId: " + outLinkId);
							lightId = this.createLights(link.getId(), laneId, outLinkId, backLinkId, inLinkToNode, crossing);
							log.debug("    created Light " + lightId);
							if (lightId != null){
								Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
								SignalPlanData signalPlan = planGroupSettings.getFirst();
								SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
								this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
							}
						}
					}
					else { //turning move restrictions on signal -> outlinks taken from signal
						for (Id outLinkId : signal.getTurningMoveRestrictions()){
							log.debug("    outLinkId: " + outLinkId);
							lightId = this.createLights(link.getId(), laneId, outLinkId, backLinkId, inLinkToNode, crossing);
							if (lightId != null){
								Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
								SignalPlanData signalPlan = planGroupSettings.getFirst();
								SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
								this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
							}
						}
					}
				}
			} 
		} 
	}
	
	private void createCrossing4NotSignalizedLink(DgCrossing crossing, Link link,
			DgCrossingNode inLinkToNode, Id backLinkId, LanesToLinkAssignment20 l2l) {
		DgProgram program = crossing.getPrograms().get(this.programId);
		if (l2l == null){
			List<Id> toLinks = this.getTurningMoves4LinkWoLanes(link);
			for (Id outLinkId : toLinks){
				Id lightId = this.createLights(link.getId(), null, outLinkId, backLinkId, inLinkToNode, crossing);
				if (lightId != null){
					this.createAndAddAllTimeGreen(lightId, program);
				}
			}
		}
		else {
			for (LaneData20 lane : l2l.getLanes().values()){
				if (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()){ // check for outlanes
					for (Id outLinkId : lane.getToLinkIds()){
						Id lightId = this.createLights(link.getId(), lane.getId(), outLinkId, backLinkId, inLinkToNode, crossing);
						if (lightId != null){
							this.createAndAddAllTimeGreen(lightId, program);
						}
					}
				}
			}
		}
	}

	//TODO check this again which offset is needed for green
	private void createAndAddGreen4Settings(Id lightId, DgProgram program,
		SignalGroupSettingsData groupSettings, SignalPlanData signalPlan) {
		DgGreen green = new DgGreen(lightId);
		green.setOffset(groupSettings.getOnset());
		green.setLength(this.calculateGreenTimeSeconds(groupSettings, signalPlan.getCycleTime()));
		log.debug("    green time " + green.getLength() + " offset: " + green.getOffset());
		program.addGreen(green);
	}
	
	
	private int calculateGreenTimeSeconds(SignalGroupSettingsData settings, Integer cycle){
		if (settings.getOnset() <= settings.getDropping()) {
			return settings.getDropping() - settings.getOnset();
		}
		else {
			return  settings.getDropping() + (cycle - settings.getOnset()); 
		}
	}

	
	
	private void createAndAddAllTimeGreen(Id lightId, DgProgram program){
		DgGreen green = new DgGreen(lightId);
		green.setLength(this.cycle);
		green.setOffset(0);
		program.addGreen(green);
	}
	
	
	private SignalSystemData getSignalSystem4SignalizedLinkId(SignalSystemsData signalSystems, Id linkId){
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()){
			for (SignalData signal : system.getSignalData().values()){
				if (signal.getLinkId().equals(linkId)){
					return system;
				}
			}
		}
		return null;
	}
	
	private List<SignalData> getSignals4LinkId(SignalSystemData system, Id linkId){
		List<SignalData> signals4Link = new ArrayList<SignalData>();
		for (SignalData signal : system.getSignalData().values()){
			if (signal.getLinkId().equals(linkId)){
				signals4Link.add(signal);
			}
		}
		return signals4Link;
	}

	
	private Link getBackLink(Link link){
		for (Link outLink : link.getToNode().getOutLinks().values()){
			if (link.getFromNode().equals(outLink.getToNode())){
				return outLink;
			}
		}
		return null;
	}
	
	
	private List<Id> getTurningMoves4LinkWoLanes(Link link){
		List<Id> outLinks = new ArrayList<Id>();
		for (Link outLink : link.getToNode().getOutLinks().values()){
			if (!link.getFromNode().equals(outLink.getToNode())){
				outLinks.add(outLink.getId());
			}
		}
		return outLinks;
	}
	
	
}
