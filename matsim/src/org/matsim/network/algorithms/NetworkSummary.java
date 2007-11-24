/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkSummary.java
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

package org.matsim.network.algorithms;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.shared.Coord;

public class NetworkSummary extends NetworkAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private int network_capacity = 0;
	private double minX = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkSummary() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		int min_node_id = Integer.MAX_VALUE;
		int max_node_id = Integer.MIN_VALUE;
		int node_cnt = 0;
		int min_link_id = Integer.MAX_VALUE;
		int max_link_id = Integer.MIN_VALUE;
		int link_cnt = 0;

		for (Node node : network.getNodes().values()) {
			node_cnt++;
			int node_getID = Integer.parseInt(node.getId().toString());
			if (min_node_id > node_getID) { min_node_id = node_getID; }
			if (max_node_id < node_getID) { max_node_id = node_getID; }
			double x = node.getCoord().getX();
			double y = node.getCoord().getY();
			if (x > this.maxX) { this.maxX = x; }
			if (x < this.minX) { this.minX = x; }
			if (y > this.maxY) { this.maxY = y; }
			if (y < this.minY) { this.minY = y; }
		}

		for (Link link : network.getLinks().values()) {
			link_cnt++;
			int link_getID = Integer.parseInt(link.getId().toString());
			if (min_link_id > link_getID) { min_link_id = link_getID; }
			if (max_link_id < link_getID) { max_link_id = link_getID; }
			this.network_capacity += Math.ceil(link.getLength()/NetworkLayer.CELL_LENGTH);
		}

		System.out.println("      network summary:");
		System.out.println("        name             = " + network.getName());
		System.out.println("        type             = " + network.getType());
		System.out.println("        capperiod        = " + Gbl.writeTime(network.getCapacityPeriod()));
		System.out.println("        network_capacity = " + this.network_capacity + " cells");
		System.out.println("      nodes summary:");
		System.out.println("        number of nodes = " + node_cnt);
		System.out.println("        min node id     = " + min_node_id);
		System.out.println("        max node id     = " + max_node_id);
		System.out.println("      links summary:");
		System.out.println("        number of links = " + link_cnt);
		System.out.println("        min link id     = " + min_link_id);
		System.out.println("        max link id     = " + max_link_id);

		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final int getNetworkCapacity() {
		return this.network_capacity;
	}

	public final Coord getMinCoord() {
		return new Coord(this.minX, this.minY);
	}

	public final Coord getMaxCoord() {
		return new Coord(this.maxX, this.maxY);
	}
}
