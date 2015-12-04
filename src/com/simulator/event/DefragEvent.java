package com.simulator.event;

import com.launcher.SimulatorParameters;
import jsim.event.Entity;
import jsim.event.Event;

/**
 * Created by Sandeep on 03-Dec-15.
 */
public class DefragEvent extends Event {

    public DefragEvent(Entity entity){
        super(entity);
        }

    public void occur(){
        SimulatorParameters.setDefrag(false);
    }

}
