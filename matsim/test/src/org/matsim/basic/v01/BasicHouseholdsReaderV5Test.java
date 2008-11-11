/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.interfaces.basic.v01.BasicIncome.IncomePeriod;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class BasicHouseholdsReaderV5Test extends MatsimTestCase {

  private static final String TESTXML  = "testHouseholds.xml";

  private final Id id23 = new IdImpl("23");
  private final Id id24 = new IdImpl("24");
  private final Id id42 = new IdImpl("42");
  private final Id id43 = new IdImpl("43");
  private final Id id44 = new IdImpl("44");
  private final Id id45 = new IdImpl("45");
  private final Id id666 = new IdImpl("666");
  
	public void testParser() {
		List<BasicHousehold> households = new ArrayList<BasicHousehold>();
		BasicHouseholdsReaderV5 reader = new BasicHouseholdsReaderV5(households);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		
		checkContent(households);
	}

	public void checkContent(List<? extends BasicHousehold> households) {
		assertEquals(2, households.size());
		BasicHousehold hh = households.get(0);
		assertNotNull(hh);
		assertEquals(id23, hh.getId());
		assertEquals(3, hh.getMemberIds().size());
		List<Id> hhmemberIds = hh.getMemberIds();
		Collections.sort(hhmemberIds);
		assertEquals(id23, hhmemberIds.get(0));
		assertEquals(id42, hhmemberIds.get(1));
		assertEquals(id43, hhmemberIds.get(2));
		
		assertNotNull(hh.getBasicLocation());
		assertNotNull(hh.getBasicLocation().getCoord());
		assertNull(hh.getBasicLocation().getLocationId());
		assertEquals(48.28d, hh.getBasicLocation().getCoord().getX(), EPSILON);
		assertEquals(7.56d, hh.getBasicLocation().getCoord().getY(), EPSILON);
	
		assertNotNull(hh.getVehicleDefinitionIds());
		assertEquals(2, hh.getVehicleDefinitionIds().size());
		assertEquals(id23, hh.getVehicleDefinitionIds().get(0));
		assertEquals(id42, hh.getVehicleDefinitionIds().get(1));
		
		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.month, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(50000.0d, hh.getIncome().getIncome(), EPSILON);
		
		assertNotNull(hh.getLanguage());
		assertEquals("german", hh.getLanguage());
		
	
		hh = households.get(1);
		assertNotNull(hh);
		assertEquals(id24, hh.getId());
		assertEquals(2, hh.getMemberIds().size());
		assertEquals(id44, hh.getMemberIds().get(0));
		assertEquals(id45, hh.getMemberIds().get(1));
		
		assertNotNull(hh.getBasicLocation());
		assertNull(hh.getBasicLocation().getCoord());
		assertNotNull(hh.getBasicLocation().getLocationId()); 
		assertTrue(hh.getBasicLocation().isFacilityId());
		assertEquals(id666, hh.getBasicLocation().getLocationId());

		assertNotNull(hh.getVehicleDefinitionIds());
		assertEquals(1, hh.getVehicleDefinitionIds().size());
		assertEquals(id23, hh.getVehicleDefinitionIds().get(0));

		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.day, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(1000.0d, hh.getIncome().getIncome(), EPSILON);

		assertNull(hh.getLanguage());
	}
}
