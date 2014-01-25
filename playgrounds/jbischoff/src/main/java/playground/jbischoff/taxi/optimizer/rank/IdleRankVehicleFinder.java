/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.optimizer.rank;

import java.util.*;

import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.model.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.optimizer.immediaterequest.*;
import playground.michalm.taxi.schedule.TaxiSchedules;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */

public class IdleRankVehicleFinder
    implements VehicleFinder
{
    private final VrpData data;
    private final VrpPathCalculator calculator;
    private final boolean straightLineDistance;
	private DepotArrivalDepartureCharger depotarrivaldeparturecharger;
	private boolean IsElectric;
	private boolean useChargeOverTime;
	Random rnd;


    public IdleRankVehicleFinder(VrpData data, VrpPathCalculator calculator, boolean straightLineDistance)
    {
        this.data = data;
        this.calculator = calculator;
        this.straightLineDistance = straightLineDistance;
        this.IsElectric = false;
        this.useChargeOverTime = false;
        this.rnd = new Random(7);
        System.out.println("Using Straight Line Distance:" + this.straightLineDistance);
    }
    public void addDepotArrivalCharger(DepotArrivalDepartureCharger depotArrivalDepartureCharger){
    	this.depotarrivaldeparturecharger = depotArrivalDepartureCharger;
    	this.IsElectric = true;
    }
    

    public void setUseChargeOverTime(boolean useChargeOverDistance) {
		this.useChargeOverTime = useChargeOverDistance;
	}
    
	private boolean hasEnoughCapacityForTask(Vehicle veh){
    		return this.depotarrivaldeparturecharger.isChargedForTask(veh.getId());
    }
	
	private double getVehicleSoc(Vehicle veh){
		return this.depotarrivaldeparturecharger.getVehicleSoc(veh.getId());
	}
    
    
	@Override
    public Vehicle findVehicle(TaxiRequest req)
    {
    	
    	if(this.useChargeOverTime) {
    		
//    		return findHighestChargedIdleVehicleDistanceSort(req);
    		return findBestChargedVehicle(req);
//    		return findHighestChargedIdleVehicle(req);
    	
    	}
    	else return findClosestFIFOVehicle(req);
    	
    }
      
    
    private Vehicle findBestChargedVehicle(TaxiRequest req){
       	  Vehicle bestVeh = null;
             double bestDistance = 1e9;
             
             Collections.shuffle(data.getVehicles(),rnd);
             for (Vehicle veh : data.getVehicles()) {
             	if (this.IsElectric)
             		if (!this.hasEnoughCapacityForTask(veh)) continue;
             	
                 double distance = calculateDistance(req, veh);
                 
                 if (distance < bestDistance) {	
                     bestDistance = distance;
                     bestVeh = veh;
                 }
                 else if (distance == bestDistance){
               	         
                 	if (bestVeh == null)
                 		{
                 		bestVeh= veh;
                 		continue;           		
                 		}
                 		if (this.getVehicleSoc(veh)>this.getVehicleSoc(bestVeh)){  bestVeh= veh;
                 		}
                 		//higher charge, if distance is equal	
                 }
             }

             return bestVeh;
       }
    
    private Vehicle findHighestChargedIdleVehicle(TaxiRequest req){
     	  Vehicle bestVeh = null;
     	  double bestSoc=0;
          Collections.shuffle(data.getVehicles(),rnd);
          
          for (Vehicle veh : data.getVehicles()) {
        	  if (!TaxiUtils.isIdle(TaxiSchedules.getSchedule(veh), data.getTime(), true)) continue;
        	  if (this.IsElectric)   if (!this.hasEnoughCapacityForTask(veh)) continue;
        	  double soc = this.getVehicleSoc(veh);
        	  if (soc>bestSoc){
        		  bestSoc = soc;
        		  bestVeh=veh;
        	  }
          }

    	
          return bestVeh;

    }
    
    private Vehicle findHighestChargedIdleVehicleDistanceSort(TaxiRequest req){
   	  Vehicle bestVeh = null;
   	  double bestSoc=0;
        Collections.shuffle(data.getVehicles(),rnd);
        
        for (Vehicle veh : data.getVehicles()) {
      	  if (!TaxiUtils.isIdle(TaxiSchedules.getSchedule(veh), data.getTime(), true)) continue;
      	  if (this.IsElectric)   if (!this.hasEnoughCapacityForTask(veh)) continue;
      	  double soc = this.getVehicleSoc(veh);
      	  if (soc>bestSoc){
      		  bestSoc = soc;
      		  bestVeh=veh;
      	  }
      	  else if (soc == bestSoc){
      		if (bestVeh == null)
     		{
     		bestVeh= veh;
     		continue;           		
     		}
      		if (this.calculateDistance(req, veh)<this.calculateDistance(req, bestVeh)){
      			bestVeh = veh;
      		}
      	  }
        }

  	
        return bestVeh;

  }
    
    private Vehicle findClosestFIFOVehicle(TaxiRequest req){
        Collections.shuffle(data.getVehicles(),rnd);
    	  Vehicle bestVeh = null;
//          double bestDistance = Double.MAX_VALUE;
          double bestDistance = Double.MAX_VALUE/2;
          for (Vehicle veh : data.getVehicles()) {
          	if (this.IsElectric)
          		if (!this.hasEnoughCapacityForTask(veh)) continue;
          	
              double distance = calculateDistance(req, veh);
              
              if (distance < bestDistance) {	
                  bestDistance = distance;
                  bestVeh = veh;
              }
              else if (distance == bestDistance){
            	         
              	if (bestVeh == null)
              		{
              		bestVeh= veh;
              		continue;           		
              		}
              		if (veh.getSchedule().getCurrentTask().getBeginTime() < bestVeh.getSchedule().getCurrentTask().getBeginTime())
              		bestVeh= veh;
              		//FIFO, if distance is equal	
              }
          }

          return bestVeh;
    }
    
    
    private double calculateDistance(TaxiRequest req, Vehicle veh){
        return IdleVehicleFinder.calculateDistance(req, veh, data.getTime(), calculator,
                straightLineDistance);
    }
}
