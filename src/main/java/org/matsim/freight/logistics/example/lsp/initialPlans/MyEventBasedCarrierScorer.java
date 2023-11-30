package org.matsim.freight.logistics.example.lsp.initialPlans;

import jakarta.inject.Inject;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
class MyEventBasedCarrierScorer implements CarrierScoringFunctionFactory {

	@Inject
	private Network network;

	@Inject
	private Scenario scenario;

	private double toll;

	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		sf.addScoringFunction(new EventBasedScoring());
		sf.addScoringFunction(new LinkBasedTollScoring(toll));
		return sf;
	}

	void setToll (double toll) {
		this.toll = toll;
	}

	/**
	 * Calculate the carrier's score based on Events.
	 * Currently, it includes:
	 * - fixed costs (using CarrierTourEndEvent)
	 * - time-dependent costs (using FreightTourStart- and -EndEvent)
	 * - distance-dependent costs (using LinkEnterEvent)
	 */
	private class EventBasedScoring implements SumScoringFunction.ArbitraryEventScoring {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);
		private final double MAX_SHIFT_DURATION = 8 * 3600;
		private final Map<VehicleType, Double>  vehicleType2TourDuration = new LinkedHashMap<>();
		private final Map<VehicleType, Integer>  vehicleType2ScoredFixCosts = new LinkedHashMap<>();
		private final Map<Id<Tour>, Double> tourStartTime = new LinkedHashMap<>();
		private double score;

		public EventBasedScoring() {
			super();
		}

		@Override public void finish() {
		}

		@Override public double getScore() {
			return score;
		}

		@Override public void handleEvent(Event event) {
			log.debug(event.toString());
			if (event instanceof CarrierTourStartEvent freightTourStartEvent) {
				handleEvent(freightTourStartEvent);
			} else if (event instanceof CarrierTourEndEvent freightTourEndEvent) {
				handleEvent(freightTourEndEvent);
			} else if (event instanceof LinkEnterEvent linkEnterEvent) {
				handleEvent(linkEnterEvent);
			}
		}

		private void handleEvent(CarrierTourStartEvent event) {
			// Save time of freight tour start
			tourStartTime.put(event.getTourId(), event.getTime());
		}

		//Fix costs for vehicle usage
		private void handleEvent(CarrierTourEndEvent event) {
			//Fix costs for vehicle usage
			final VehicleType vehicleType = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

			double tourDuration = event.getTime() - tourStartTime.get(event.getTourId());
			{ //limit fixed costs of vehicles if vehicles could be reused during shift
				if (tourDuration > MAX_SHIFT_DURATION) {
					throw new RuntimeException("Duration of tour is longer than max shift defined in scoring fct, caused by event:"
							+ event + " tourDuration: " + tourDuration + " max shift duration:  " + MAX_SHIFT_DURATION);
				}

				//sum up tour durations
				if (vehicleType2TourDuration.containsKey(vehicleType)) {
					vehicleType2TourDuration.put(vehicleType, vehicleType2TourDuration.get(vehicleType) + tourDuration);
				} else {
					vehicleType2TourDuration.put(vehicleType, tourDuration);
				}

				//scoring needed?
				final double currentNuOfVehiclesNeeded = Math.ceil(vehicleType2TourDuration.get(vehicleType) / MAX_SHIFT_DURATION);
				final Integer nuAlreadyScored = vehicleType2ScoredFixCosts.get(vehicleType);
				if (nuAlreadyScored == null ) {
					log.info("Score fixed costs for vehicle type: " + vehicleType.getId().toString());
					score = score - vehicleType.getCostInformation().getFixedCosts();
					vehicleType2ScoredFixCosts.put(vehicleType, 1);
				} else if (currentNuOfVehiclesNeeded > nuAlreadyScored) {
					log.info("Score fixed costs for vehicle type: " + vehicleType.getId().toString());
					score = score - vehicleType.getCostInformation().getFixedCosts();
					vehicleType2ScoredFixCosts.put(vehicleType, vehicleType2ScoredFixCosts.get(vehicleType) + 1);
				}
			}

			// variable costs per time
			score = score - (tourDuration * vehicleType.getCostInformation().getCostsPerSecond());
		}

		private void handleEvent(LinkEnterEvent event) {
			final double distance = network.getLinks().get(event.getLinkId()).getLength();
			final double costPerMeter = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getCostInformation().getCostsPerMeter();
			// variable costs per distance
			score = score - (distance * costPerMeter);
		}

	}

	/**
	 * Calculate some toll for driving on a link
	 * This a lazy implementation of a cordon toll.
	 * A vehicle is only tolled once.
	 */
	class LinkBasedTollScoring implements SumScoringFunction.ArbitraryEventScoring {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);

		private final double toll;
		private final List<String> vehicleTypesToBeTolled = List.of("large50");
		private final List<Id<Vehicle>> tolledVehicles = new ArrayList<>();
		private double score;

		public LinkBasedTollScoring(double toll) {
			super();
			this.toll = toll;
		}

		@Override public void finish() {}

		@Override public double getScore() {
			return score;
		}

		@Override public void handleEvent(Event event) {
			if (event instanceof LinkEnterEvent linkEnterEvent) {
				handleEvent(linkEnterEvent);
			}
		}

		private void handleEvent(LinkEnterEvent event) {
//			List<String> tolledLinkList = Arrays.asList("i(5,5)R");
			List<String> tolledLinkList = Arrays.asList("i(3,4)", "i(3,6)", "i(7,5)R", "i(7,7)R", "j(4,8)R", "j(6,8)R", "j(3,4)", "j(5,4)");

			final Id<VehicleType> vehicleTypeId = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getId();

			//toll a vehicle only once.
			if (!tolledVehicles.contains(event.getVehicleId()))
				if (vehicleTypesToBeTolled.contains(vehicleTypeId.toString())) {
					if (tolledLinkList.contains(event.getLinkId().toString())) {
						log.info("Tolling caused by event: " + event);
						tolledVehicles.add(event.getVehicleId());
						score = score - toll;
					}
				}
		}
	}
}
