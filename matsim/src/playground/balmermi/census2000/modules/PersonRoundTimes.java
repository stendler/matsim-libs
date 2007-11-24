/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRoundTimes.java
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

package playground.balmermi.census2000.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PersonRoundTimes extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final TreeSet<String> types = new TreeSet<String>();
	private final static double MIN_HOME = 4.0*3600.0;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonRoundTimes() {
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		ArrayList<Object> acts_legs = person.getSelectedPlan().getActsLegs();

		// getting durations
		double[] durs = new double[(acts_legs.size()-1)/2];
		int index = 0;
		for (int i=0; i<acts_legs.size()-2; i=i+2) {
			Act act = (Act)acts_legs.get(i);
			durs[index] = act.getDur();
			index++;
		}

		// round times and calc plan dur
		double plan_dur = 0.0;
		for (int i=0; i<durs.length; i++) {
			if (durs[i] < 1800.0) { durs[i] = 1800.0; }
			else { durs[i] = Math.floor(durs[i]/3600.0 + 0.5) * 3600; }
			plan_dur += durs[i];
		}
		
		// calc home dur
		// home_dur <= 0.0: the plan is too long
		// home_dur < MIN_HOME: home dur is too small (but the plan fits)
		// else: everything is ok
		double home_dur = durs[0];
		home_dur += (24.0*3600 - plan_dur);
		
		// re-ajust the durations if home_dur is too small,
		// re-calc the plan duration and
		// re-calc the home dur
		if (home_dur < MIN_HOME) {
			System.out.println("      Person id=" + person.getId() + ", home_dur=" + home_dur + ": shrinking durations...");
			double bias = MIN_HOME - home_dur;
			bias = bias/(durs.length-1.0);
			plan_dur = durs[0];
			for (int i=1; i<durs.length; i++) {
				durs[i] = durs[i]-bias;
				if (durs[i] < 1800.0) { durs[i] = 1800.0; }
				else { durs[i] = Math.floor(durs[i]/3600.0 + 0.5) * 3600; }
				plan_dur += durs[i];
			}
			home_dur = durs[0];
			home_dur += (24.0*3600 - plan_dur);
			System.out.println("      done. (plan_dur=" + plan_dur + ", home_dur=" + home_dur + ")");
		}

		if (home_dur <= 0.0) { Gbl.errorMsg("This should not happen!"); }
		
		// setting durations and types
		index = 0;
		plan_dur = 0.0;
		for (int i=0; i<acts_legs.size()-2; i=i+2) {
			Act act = (Act)acts_legs.get(i);
			Leg leg = (Leg)acts_legs.get(i+1);
			double dur = durs[index];

			// durations
			act.setStartTime(plan_dur);
			act.setDur(dur);
			plan_dur += dur;
			act.setEndTime(plan_dur);
			leg.setDepTime(plan_dur);
			leg.setTravTime(0.0);
			leg.setArrTime(plan_dur);
			
			// types
			if (i == 0) { dur = home_dur; }
			int hours = (int)dur/3600;
			String type = act.getType() + hours;
			if (hours == 0) { type = type + ".5"; }
			act.setType(type);
			this.types.add(type);
			index++;
		}
		
		Act last_act = (Act)acts_legs.get(acts_legs.size()-1);
		last_act.setStartTime(plan_dur);
		last_act.setDur(Gbl.UNDEFINED_TIME);
		last_act.setEndTime(Gbl.UNDEFINED_TIME);
		last_act.setType(((Act)acts_legs.get(0)).getType());
	}

	public void run(Plan plan) {
	}
	
	public final void print() {
		System.out.println("-----------------------");
		System.out.println("List of activity types:");
		System.out.println("-----------------------");
		Iterator<String> it = this.types.iterator();
		while (it.hasNext()) {
			System.out.println(it.next());
		}
		System.out.println("-----------------------");
	}
}
