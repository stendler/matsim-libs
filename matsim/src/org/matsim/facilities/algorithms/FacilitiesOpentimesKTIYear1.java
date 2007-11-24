/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesOpentimesKTIYear1.java
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

package org.matsim.facilities.algorithms;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.gbl.Gbl;

public class FacilitiesOpentimesKTIYear1 extends FacilitiesAlgorithm {

	private TreeMap<String, Opentime> openingTimes = new TreeMap<String, Opentime>();

	public FacilitiesOpentimesKTIYear1() {
		super();
	}

	@Override
	public void run(Facilities facilities) {

		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		this.loadOpeningTimes();

		for (Facility f : facilities.getFacilities().values()) {
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {

				Activity a = a_it.next();
				String actType = a.getType();

				// delete all existing open times info
				TreeMap<String, TreeSet<Opentime>> o = a.getOpentimes();
				o.clear();

				if (openingTimes.containsKey(actType)) {

					a.addOpentime(openingTimes.get(actType));

				}
				else {

					Gbl.warningMsg(
							this.getClass(),
							"run(...)",
							"For activity type " + actType + " no opening time is defined");

				}
			}

		}

		System.out.println("    done.");

	}

	private void loadOpeningTimes() {

		openingTimes.put("work", new Opentime("wkday", "7:00", "18:00"));
		openingTimes.put("shop", new Opentime("wkday", "8:00", "20:00"));
		openingTimes.put("education", new Opentime("wkday", "7:00", "18:00"));
		openingTimes.put("leisure", new Opentime("wkday", "6:00", "24:00"));

	}
}
