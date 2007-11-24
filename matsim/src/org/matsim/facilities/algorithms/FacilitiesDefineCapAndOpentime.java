/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesDefineCapAndOpentime.java
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

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.gbl.Gbl;

public class FacilitiesDefineCapAndOpentime extends FacilitiesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final int nof_persons;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesDefineCapAndOpentime(final int nof_persons) {
		super();
		this.nof_persons = nof_persons;
		if (nof_persons <= 0) {
			Gbl.errorMsg("[nof_persons=" + nof_persons + " not allowed]");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		int home_cnt = 0;
		int work_cnt = 0;
		int educ_cnt = 0;
		int shop_cnt = 0;
		int leis_cnt = 0;

		for (Facility f : facilities.getFacilities().values()) {
			Iterator<String> at_it = f.getActivities().keySet().iterator();
			while (at_it.hasNext()) {
				String at = at_it.next();
				if (at.equals("home")) { home_cnt++; }
				else if (at.equals("work")) { work_cnt++; }
				else if (at.equals("education")) { educ_cnt++; }
				else if (at.equals("shop")) { shop_cnt++; }
				else if (at.equals("leisure")) { leis_cnt++; }
				else { Gbl.errorMsg("[something is wrong!]"); }
			}
		}
		System.out.println("      home_cnt = " + home_cnt);
		System.out.println("      work_cnt = " + work_cnt);
		System.out.println("      educ_cnt = " + educ_cnt);
		System.out.println("      shop_cnt = " + shop_cnt);
		System.out.println("      leis_cnt = " + leis_cnt);

		for (Facility f : facilities.getFacilities().values()) {
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {
				Activity a = a_it.next();
				if (a.getType().equals("home")) {
					a.setCapacity(this.nof_persons/home_cnt);
					a.addOpentime(new Opentime("wk","00:00","24:00"));
				}
				else if (a.getType().equals("work")) {
					a.setCapacity(this.nof_persons/work_cnt);
					a.addOpentime(new Opentime("wkday","08:00","18:00"));
				}
				else if (a.getType().equals("education")) {
					a.setCapacity(this.nof_persons/educ_cnt);
					a.addOpentime(new Opentime("wkday","08:00","12:00"));
					a.addOpentime(new Opentime("wkday","13:00","17:00"));
				}
				else if (a.getType().equals("shop")) {
					a.setCapacity(this.nof_persons/shop_cnt);
					a.addOpentime(new Opentime("wkday","08:30","12:00"));
					a.addOpentime(new Opentime("wkday","13:30","20:00"));
					a.addOpentime(new Opentime("sat","09:00","17:00"));
				}
				else if (a.getType().equals("leisure")) {
					a.setCapacity(this.nof_persons/leis_cnt);
					a.addOpentime(new Opentime("wkday","17:00","24:00"));
					a.addOpentime(new Opentime("wkend","20:00","24:00"));
				}
				else { Gbl.errorMsg("[something is wrong!]"); }
			}
		}

		System.out.println("    done.");
	}
}
