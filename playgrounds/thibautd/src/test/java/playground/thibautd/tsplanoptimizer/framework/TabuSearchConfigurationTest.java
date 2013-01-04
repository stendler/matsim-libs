/* *********************************************************************** *
 * project: org.matsim.*
 * TabuSearchConfigurationTest.java
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
package playground.thibautd.tsplanoptimizer.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

/**
 * @author thibautd
 */
public class TabuSearchConfigurationTest {
	@Test
	public void testLock() throws Exception {
		TabuSearchConfiguration conf = new TabuSearchConfiguration();
		// this must lock. we should test that all the getters cause a lock,
		// but it's boring.
		conf.getRandom();

		boolean gotException = false;
		try {
			conf.setEvolutionMonitor( null );
		}
		catch (IllegalStateException e) {
			gotException = true;
		}
		assertTrue( "setEvolutionMonitor is not locked!", gotException );

		gotException = false;
		try {
			conf.setFitnessFunction( null );
		}
		catch (IllegalStateException e) {
			gotException = true;
		}
		assertTrue( "setFitnessFunction is not locked!", gotException );

		gotException = false;
		try {
			conf.setMoveGenerator( null );
		}
		catch (IllegalStateException e) {
			gotException = true;
		}
		assertTrue( "setMoveGenerator is not locked!", gotException );

		gotException = false;
		try {
			conf.setRandom( null );
		}
		catch (IllegalStateException e) {
			gotException = true;
		}
		assertTrue( "setRandom is not locked!", gotException );

		gotException = false;
		try {
			conf.setTabuChecker( null );
		}
		catch (IllegalStateException e) {
			gotException = true;
		}
		assertTrue( "setTabuChecker is not locked!", gotException );
	}

	/**
	 * Test that if elements of the config implement even listenners,
	 * they are automatically added.
	 */
	@Test
	public void testCoreListenners() throws Exception {
		TabuSearchConfiguration conf = new TabuSearchConfiguration();

		EvolutionMonitor start = new StartListenerEvolutionMonitor();
		AppliedMoveListenerTabuChecker applied = new AppliedMoveListenerTabuChecker();
		EndListennerMoveGenerator end = new EndListennerMoveGenerator();

		conf.setEvolutionMonitor( start );
		conf.setTabuChecker( applied );
		conf.setMoveGenerator( end );

		List<StartListener> starts = conf.getStartListeners();
		assertEquals( "unexpected number of start listeners "+starts, starts.size() , 1 );
		assertTrue( "unexpected start listener "+starts, starts.contains( start ) );

		List<AppliedMoveListener> applieds = conf.getAppliedMoveListeners();
		assertEquals( "unexpected number of applied nove listeners "+applieds, applieds.size() , 1 );
		assertTrue( "unexpected applied listener "+applieds, applieds.contains( applied ) );

		List<EndListener> ends = conf.getEndListeners();
		assertEquals( "unexpected number of end listeners "+ends, ends.size() , 1 );
		assertTrue( "unexpected start listener "+ends, ends.contains( end ) );
	}
}

class StartListenerEvolutionMonitor implements EvolutionMonitor, StartListener {

	@Override
	public boolean continueIterations(int iteration, Solution currentBest,
			double currentBestScore) {
		return false;
	}

	@Override
	public void notifyStart(Solution startSolution, double startScore) {
	}
}

class AppliedMoveListenerTabuChecker implements TabuChecker {

	@Override
	public void notifyMove(Solution currentSolution, Move toApply,
			double resultingFitness) {
	}

	@Override
	public boolean isTabu(Solution solution, Move move) {
		return false;
	}
}

class EndListennerMoveGenerator implements MoveGenerator, EndListener {

	@Override
	public void notifyEnd(Solution bestSolution, double bestScore,
			int nIterations) {
	}

	@Override
	public Collection<Move> generateMoves() {
		return null;
	}
}
