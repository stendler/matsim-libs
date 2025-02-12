/* ********************************************************************** *
 * project: org.matsim.*
 * AbstractProfilingEventAspect.aj
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

import jdk.jfr.Event;
import org.matsim.contrib.profiling.events.JFRMatsimEvent;

public abstract aspect AbstractProfilingEventAspect {

    abstract pointcut eventPoints(Object o);

    void around(Object o): eventPoints(o) {
        Event jfrEvent = JFRMatsimEvent.create("scoring AOP: " + o.getClass().getName());

        System.out.println("AOP profiling: " + o.getClass().getSimpleName());

        jfrEvent.begin();
        proceed(o);
        jfrEvent.commit();
    }
}
