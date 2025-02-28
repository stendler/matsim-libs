/* ********************************************************************** *
 * project: org.matsim.*
 * ScoringListenerProfilingAspect.aj
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 * copyright       : (C) 2025 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                           *
 * email           : info at matsim dot org                               *
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 *   This program is free software; you can redistribute it and/or modify *
 *   it under the terms of the GNU General Public License as published by *
 *   the Free Software Foundation; either version 2 of the License, or    *
 *   (at your option) any later version.                                  *
 *   See also COPYING, LICENSE and WARRANTY file                          *
 *                                                                        *
 * ********************************************************************** */

package org.matsim.contrib.profiling.aop;

//import org.matsim.core.controler.listener.ScoringListener;

public aspect ScoringListenerProfilingAspect extends AbstractProfilingEventAspect {

    /**
     * Use the full canonical name (including package) in the pointcut declaration to ensure aspectj finds the targeted class
     * or declare an import.
     */
    pointcut eventPoints():
            call(void org.matsim.core.controler.listener.ScoringListener.notifyScoring(..));

}
