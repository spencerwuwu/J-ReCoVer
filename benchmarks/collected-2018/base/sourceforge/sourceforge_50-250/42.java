// https://searchcode.com/api/result/14342627/

package repast.simphony.demo.predatorprey;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

/**
 * @author Eric Tatara 
 *
 */

public class Sheep extends SimpleAgent {
	
	private double energy;
	
	public Sheep(double energy){
		this.energy = energy;
	}
	
	public Sheep(){
  	Parameters p = RunEnvironment.getInstance().getParameters();
		Double gain = (Double)p.getValue("sheepgainfromfood");
		
		energy = Math.random() * 2 * gain;
	}
	
	@Override
	public void step() {
		Context context = ContextUtils.getContext(this);
		
		// Move
		move();
		
    //	Reduce energy
		energy = energy - 1;
		
		// Eat Grass
		int x = getX();
		int y = getY();
		
		Parameters p = RunEnvironment.getInstance().getParameters();
		double gain = (Double)p.getValue("sheepgainfromfood");

		Grid<SimpleAgent> grid = (Grid<SimpleAgent>) context.getProjection("Simple Grid");

		Grass grass = null;
		for (SimpleAgent agent : grid.getObjectsAt(x,y)){
			if (agent instanceof Grass)
				grass = (Grass)agent;
		}
		if (grass != null && grass.isAlive()){
			grass.setAlive(false);
			energy = energy + gain;
		}
		
		// Reproduce
		double rate = (Double)p.getValue("sheepreproduce");
		
    // Spawn
		if (100*Math.random() < rate){
			energy = energy / 2;
			Sheep sheep = new Sheep(energy);
			context.add(sheep);
		}

		// Death
		if (energy < 0)
		 die();
	}

	public double getEnergy() {
		return energy;
	}
	
	@Override
	public int isSheep() {
		return 1;
	}
}
