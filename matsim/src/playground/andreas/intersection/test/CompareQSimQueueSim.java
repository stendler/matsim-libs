package playground.andreas.intersection.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandlerActivityEndI;
import org.matsim.events.handler.EventHandlerActivityStartI;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

import playground.andreas.intersection.sim.QSim;

/**
 * @author aneumann
 *
 */
public class CompareQSimQueueSim extends MatsimTestCase implements	EventHandlerLinkLeaveI, EventHandlerLinkEnterI, EventHandlerActivityEndI, EventHandlerActivityStartI, EventHandlerAgentArrivalI, EventHandlerAgentDepartureI, EventHandlerAgentWait2LinkI{
	
	BufferedWriter writer = null;
	
	public void testTrafficLightIntersection2arms_w_TrafficLight(){
  		  		
		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String popFileName = "src/playground/andreas/intersection/test/data/plans_2a_5000.xml.gz";
		String netFileName = "src/playground/andreas/intersection/test/data/net_2a.xml.gz";
		
		String signalSystems = "./src/playground/andreas/intersection/test/data/signalSystemConfig_2a.xml";
		String groupDefinitions = "./src/playground/andreas/intersection/test/data/signalGroupDefinition_2a.xml";

		conf.plans().setInputFile(popFileName);
		conf.network().setInputFile(netFileName);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		
		try {		
			this.writer = new BufferedWriter(new FileWriter(new File("qsim_events.txt")));
			new QSim(events, data.getPopulation(), data.getNetwork(), signalSystems, groupDefinitions, false).run();
//			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			this.writer.flush();
			this.writer.close();


			this.writer = new BufferedWriter(new FileWriter(new File("queuesim_events.txt")));

			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			this.writer.flush();
			this.writer.close();
			
			assertEquals(CRCChecksum.getCRCFromFile("qsim_events.txt"),	CRCChecksum.getCRCFromFile("queuesim_events.txt"));
			
			new File("qsim_events.txt").delete();
			new File("queuesim_events.txt").delete();

		} catch (IOException e) {
			e.printStackTrace();
		}		
		
  	}  	

	public void handleEvent(LinkEnterEnter event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public void handleEvent(LinkLeaveEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
	public void reset(int iteration) {
	}

	public void handleEvent(ActEndEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(ActStartEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentArrivalEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentDepartureEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}