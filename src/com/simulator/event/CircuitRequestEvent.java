package com.simulator.event;

import com.auxiliarygraph.AuxiliaryGraph;
import com.defrag.Reconfig;
import com.defrag.ReconfigureLink;
import com.filemanager.Results;
import com.inputdata.elements.TrafficClass;
import com.launcher.SimulatorParameters;
import com.rng.Distribution;
import com.rng.distribution.ExponentialDistribution;
import com.simulator.Scheduler;
import com.simulator.elements.Generator;
import com.simulator.elements.TrafficFlow;
import jsim.event.Entity;
import jsim.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a new request event in the simulator
 *
 * @author Fran
 */
public class CircuitRequestEvent extends Event {

    /**
     * Generator responsible of the event
     */
    private Generator generator;
    /**
     * TrafficClass that generates the request
     */
    private TrafficClass trafficClass;

    private static final Logger log = LoggerFactory.getLogger(CircuitRequestEvent.class);

    /**
     * Constructor class
     */
    public CircuitRequestEvent(Entity entity, Generator generator,
                               TrafficClass trafficClass) {
        super(entity);
        this.generator = generator;
        this.trafficClass = trafficClass;

    }

    @Override
    public void occur() {
        int counter = 0;
        double holdingTime;
        // double defragEndTime = SimulatorParameters.getSimulationTime();
        boolean isUnKnown = true;  //generator.getRandomUnknown(trafficClass.getType());

        holdingTime = trafficClass.getHoldingTimeDistribution().execute();

        /** If it is unknown, get the mean holding time*/
//        if (isUnKnown)
//            holdingTime = trafficClass.getMeanHoldingTime();

        /** Get a random destination following a uniform distribution */
        TrafficFlow selectedFlow = generator.getRandomFlow(trafficClass.getType());

        int numberOfMiniGrids = (int) (trafficClass.getBw());
        /** Create a new Auxiliary Graph*/
        AuxiliaryGraph auxiliaryGraph = new AuxiliaryGraph(generator.getVertex().getVertexID(), selectedFlow.getDstNode().getVertexID(), numberOfMiniGrids, Scheduler.currentTime(), holdingTime, isUnKnown);
        if (!SimulatorParameters.getDefrag())
        /**If path is found, then add release event*/
            if (auxiliaryGraph.runShortestPathAlgorithm(selectedFlow.getListOfPaths())) {
                Event event = new CircuitReleaseEvent(new Entity(holdingTime), generator, selectedFlow, auxiliaryGraph.getNewConnection());
                Scheduler.schedule(event, holdingTime);
                log.debug("Added release event: " + generator.getVertex().getVertexID() + "-" + selectedFlow.getDstNode().getVertexID());
                //            Results.writeHoldingTime(generator,selectedFlow,trafficClass.getType(),isUnKnown,holdingTime);
            } else { /**if not, increase blocking counter*/
                selectedFlow.increaseBlockingCounter(trafficClass.getType(), auxiliaryGraph.getIfblockedDueToResourceUnavailability());
                log.debug("Connection is blocked due to resource unavailability or fragmentation");
                /** Apply Defragmentation */
                if (SimulatorParameters.getWantDefrag() == 1) {
                    // if blocked due to fragmentation
                    if (!auxiliaryGraph.getIfblockedDueToResourceUnavailability()) {
                        ReconfigureLink reconfigureLink = new ReconfigureLink();
                        int fragment = reconfigureLink.reconfigLightpaths();
                        SimulatorParameters.setDefrag(true,fragment);
                        double defragTime = SimulatorParameters.getDefragTime();
                        Event event = new DefragEvent(new Entity(defragTime));
                        Scheduler.schedule(event, defragTime);
                    }
                } else {
                    selectedFlow.increaseBlockingCounter(trafficClass.getType(), auxiliaryGraph.getIfblockedDueToResourceUnavailability());
                    log.debug("Connection is blocked due to resource unavailability or fragmentation");
                }
            }
        else {
            selectedFlow.increaseDefragBlockingcounter(trafficClass.getType());
            log.debug("Connection is blocked due to defragmentation");
        }


        /** Increase request counter for this flow */
        selectedFlow.increaseFlowRequestCounter(trafficClass.getType());

        /*********************** Results *************************/
        Results.writeBlockingResults(generator, selectedFlow);
        Results.writeLinkUtilizationResults();
        Results.increaseRequestCounter();



        /** Add a new request event */
        TrafficClass nextTrafficClass = generator.getRandomPort();
        double nextInterArrivalTime = generator.getRequestDistribution().execute();
        Event event = new CircuitRequestEvent(new Entity(nextInterArrivalTime), generator, nextTrafficClass);
        Scheduler.schedule(event, nextInterArrivalTime);
        log.debug("Added request event: " + generator.getVertex().getVertexID() + "-" + selectedFlow.getDstNode().getVertexID());
//        Results.writeInterArrivalTime(generator, selectedFlow,trafficClass.getType(),nextInterArrivalTime);
    }

}
