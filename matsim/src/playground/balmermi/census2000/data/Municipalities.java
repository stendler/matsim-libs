/* *********************************************************************** *
 * project: org.matsim.*
 * Municipalities.java
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

package playground.balmermi.census2000.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;
import org.matsim.world.Zone;

public class Municipalities {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String MUNICIPALITY = "municipality";
	private final HashMap<IdI,Municipality> municipalities = new HashMap<IdI,Municipality>();
	private final String inputfile;

	private Random random = new Random(123);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Municipalities(String inputfile) {
		super();
		this.inputfile = inputfile;
		this.random.nextDouble(); // ignore first number
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Municipality getMunicipality(int m_id) {
		return this.municipalities.get(new Id(m_id));
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public final void parse() {
		int line_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); line_cnt++;
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				// Kanton	Kt_Name	Gem_Nr	Gem_Name	Bev_total	Eink_2000	RG_verk	Benzin_95
				// 0        1       2       3           4           5           6       7

				Integer m_id = Integer.parseInt(entries[2].trim());
				Location l = Gbl.getWorld().getLayer(MUNICIPALITY).getLocation(m_id);
				if (l == null) {
					System.out.println("    Municipality id=" + m_id + " ignored. (Does not exist in the world layer.)");
				} else {
					Municipality m = new Municipality((Zone)l);
					this.municipalities.put(l.getId(),m);

					m.k_id = Integer.parseInt(entries[0].trim());
					m.income = Double.parseDouble(entries[5].trim())/12.0; // monthly income
					m.reg_type = Integer.parseInt(entries[6].trim());
					m.fuelcost = Double.parseDouble(entries[7].trim());
				}
				line_cnt++;
			}
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("    # municipalities = " + this.municipalities.size());
		System.out.println("    # lines = " + line_cnt);
	}

	//////////////////////////////////////////////////////////////////////

	public final void print() {
		Iterator<Municipality> m_it = this.municipalities.values().iterator();
		while (m_it.hasNext()) {
			System.out.println(m_it.next().toString());
		}
	}
}
