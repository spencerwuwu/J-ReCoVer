// https://searchcode.com/api/result/14342625/

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

public class Wolf extends SimpleAgent {

	private double energy;

	public Wolf (double energy){
		this.energy = energy;
	}

	public Wolf(){
		Parameters p = RunEnvironment.getInstance().getParameters();
		double gain = (Double)p.getValue("wolfgainfromfood");

		energy = Math.random() * 2 * gain;
	}

	@Override
	public void step() {
		Context context = ContextUtils.getContext(this);

		// Move
		move();

		// Reduce energy
		energy = energy - 1;

		// Catch sheep
		int x = getX();
		int y = getY();

		Parameters p = RunEnvironment.getInstance().getParameters();
		double gain = (Double)p.getValue("wolfgainfromfood");

		Grid<SimpleAgent> grid = (Grid<SimpleAgent>) context.getProjection("Simple Grid");

		Sheep sheep = null;
		for (SimpleAgent agent : grid.getObjectsAt(x,y)){
			if (agent instanceof Sheep)
				sheep = (Sheep)agent;
		}
		if (sheep != null){
			sheep.die();
			energy = energy + gain;
		}

		// Reproduce
		double rate = (Double)p.getValue("wolfreproduce");

		// Spawn
		if (100*Math.random() < rate){
			energy = energy / 2;
			Wolf wolf = new Wolf(energy);
			context.add(wolf);	
		}

		// Death
		if (energy < 0)
			die();
	}

	public double getEnergy() {
		return energy;
	}
	
	@Override
	public int isWolf() {
		return 1;
	}
}
