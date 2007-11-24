/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoutingTest.java
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

package org.matsim.replanning;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

public class ReRoutingTest extends MatsimTestCase {

	public void testReRouting() {
		loadConfig(getInputDirectory() + "config.xml");

		SimulationTimer.reset(10);

		TestControler controler = new TestControler();
		controler.run(null);

		System.out.println("calculating checksums...");
		long checksum1 = CRCChecksum.getCRCFromGZFile(getInputDirectory() + "plans.xml.gz");
		long checksum2 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "ITERS/it.1/1.plans.xml.gz");
		System.out.println("checksum1 = " + checksum1);
		System.out.println("checksum2 = " + checksum2);
		assertEquals(checksum1, checksum2);
	}

	static public class TestControler extends Controler {

		@Override
		protected void setupIteration(final int iteration) {
			if (iteration == 0) {
				// do some test to ensure the scenario is correct

				int lastIter = Gbl.getConfig().controler().getLastIteration();
				if (lastIter < 1) {
					throw new IllegalArgumentException("Controler.lastIteration must be at least 1. Current value is " + lastIter);
				}
				if (lastIter > 1) {
					System.err.println("Controler.lastIteration is currently set to " + lastIter + ". Only the first iteration will be analyzed.");
				}
			}
			super.setupIteration(iteration);
		}

		@Override
		protected void runMobSim() {
			if (getIteration() == 0) {
				/* only run mobsim in iteration 0, afterwards we're no longer interested
				 * in it as we have our plans-file to compare against to check the
				 * replanning.
				 */
				super.runMobSim();
			} else {
				System.out.println("skipping mobsim, as it is not of interest in this iteration.");
			}
		}

	}
}
