// https://searchcode.com/api/result/137113246/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ecosim.model;

import ecosim.utils.SpatialCollection;
import ecosim.utils.SpatialHashTable;
import ecosim.utils.Utils;
import ecosim.utils.Vector2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author positron
 */
public final class Environment implements Tickable, Serializable {

	private final World world;

	public static double EPS = 0.02;

	public boolean autoFood=false;

	public final SpatialCollection<FoodPiece> foods;

	private boolean foodsIterating = false;
	private Collection<FoodPiece> toAdd = new ArrayList<>();
	private final int resourceBucket = 25;
	private final double[][] resourceDistribution = new double[resourceBucket][resourceBucket];

	public Environment(World ww) {
		this.world = ww;
		//foods = new PointRegionQuadTree<>(WW,HH);
		foods = new SpatialHashTable<> (world.WW, world.HH, 50,50);
		for(int i=0; i<500; i++){
			addRandomPlant();
		}
//		for(int i=0; i<30; i++){
//			addFood(new Plant(world, Utils.rnd(world.WW*0.4,world.WW*0.7),
//				Utils.rnd(world.HH*0.4,world.HH*0.7) )  );
//		}
	}

	public double getSolarResource(Vector2D pos) {
		synchronized (resourceDistribution) {
			int x = (int)(pos.x/world.WW*resourceBucket);
			int y = (int)(pos.y/world.HH*resourceBucket);
			return resourceDistribution[x][y];
		}
	}
	private void recalcResource() {
		double[][] counts = new double[resourceBucket][resourceBucket];
		for(FoodPiece food : foods)
			if(food instanceof Plant) {
				Vector2D pos = food.getPosition();
				counts[(int)(pos.x/world.WW*resourceBucket)]
						[(int)(pos.y/world.HH*resourceBucket)]++;
			}
		double act = world.getSolarActivity();
		synchronized (resourceDistribution) {
			for(int i=0; i<resourceBucket; i++)
				for(int j=0; j<resourceBucket; j++)
					resourceDistribution[i][j]= act / (1+counts[i][j]);
			for(int i=1; i<resourceBucket-1; i++)
				for(int j=1; j<resourceBucket-1; j++)
					resourceDistribution[i][j]=
							(resourceDistribution[i-1][j-1] +
							 resourceDistribution[i-1][j] +
							 resourceDistribution[i-1][j+1] +
							 resourceDistribution[i]  [j-1] +
							 resourceDistribution[i]  [j] +
							 resourceDistribution[i]  [j+1] +
							 resourceDistribution[i+1][j-1] +
							 resourceDistribution[i+1][j] +
							 resourceDistribution[i+1][j+1] ) / 9;
		}
	}

	@Override
	public void tick() {
		Set<FoodPiece> toRemove = new HashSet<>();
		synchronized (foods) {
			foodsIterating = true;
			recalcResource();
			for(FoodPiece f: foods) {
				if(f.isDead()) {
					toRemove.add(f);
				}
				f.tick();
			}
			foodsIterating = false;
			if(!toRemove.isEmpty())
				foods.removeAll(toRemove);
			if(!toAdd.isEmpty()) {
				foods.addAll(toAdd);
				toAdd.clear();
			}
			if(autoFood && Math.random()>0.9)
				addRandomFood();
		}
	}

	FoodPiece locateFood(Animal seeker) {
		return foods.findNearest(seeker.getPosition() );
	}

	public void consumeFood(FoodPiece food, Animal eater) {
		double piece = eater.getFoodPiece();
		piece = food.reduce( piece );
		eater.consumeFood(piece);
	}

	public void addFood(double fx, double fy, double amt) {
		addFood(new FoodPiece.OrdinaryFoodPiece(world,fx,fy, amt) );
	}
	public void addFood(FoodPiece f) {
		synchronized (foods) {
			if(!foodsIterating)
				foods.add(f);
			else toAdd.add(f);
		}
	}

	public void addRandomPlant() {
		addFood(new Plant(world, Utils.rnd(world.WW*0.05,world.WW*0.95),
				Utils.rnd(world.HH*0.05,world.HH*0.95) )  );
	}
	public void addRandomFood() {
		addFood(new FoodPiece.OrdinaryFoodPiece(world, Utils.rnd(world.WW*0.1,world.WW*0.9),
				Utils.rnd(world.HH*0.1,world.HH*0.9), Utils.rnd(3.0, 6.0))  );
	}


}

