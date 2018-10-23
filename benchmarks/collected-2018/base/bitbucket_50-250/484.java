// https://searchcode.com/api/result/137113239/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ecosim.model;

import ecosim.utils.Utils;

/**
 *
 * @author positron
 */
public class Plant extends FoodPiece {

	private int age;
	public final int maxAge;
	//public final int reproductionAge;
	public final int reproductionIntl;
	private int nextReproduction;
	private static final double UNEATABLE_QUANTITY = 1;

	public Plant(World w, double x, double y) {
		super(w,x,y, 5 + Utils.rnd(10), FoodType.VEGETABLE);
		age = 0;
		maxAge = 10*1000 + Utils.rnd(-300,300);
		nextReproduction = (int)(maxAge*0.2 * Utils.rnd(0.8, 1.2));
		reproductionIntl = (int)(maxAge*0.2 * Utils.rnd(0.8, 1.2));
	}

	public Plant(Plant parent) {
		super(parent.w, parent.pos.x + Utils.rnd(-20,20),parent.pos.y+Utils.rnd(-20,20),
				2, FoodType.VEGETABLE);
		age = 0;
		double val = w.env.getSolarResource(pos) - 0.5;
		maxAge = parent.maxAge + (int)( (Math.random()+val )*100);
		nextReproduction = parent.nextReproduction;
		reproductionIntl = parent.reproductionIntl;
	}

	@Override
	public void tick() {
		age++;
		if(age==maxAge) {
			quantity += UNEATABLE_QUANTITY;
		}
		if(age>=maxAge) {
			quantity -= 0.002;
			return;
		}
		double act = w.env.getSolarResource(pos);
		quantity += 0.01 * act;
		if(act < 0.5) return;
		if(age > nextReproduction) {
			w.env.addFood( new Plant( this ) );
			nextReproduction =
					age + reproductionIntl
					+ Utils.rnd(-reproductionIntl, reproductionIntl)/2
					+ (int)((1/act-1) * 400);
		}
	}

	@Override
	public boolean isDead() {
		if(UNEATABLE_QUANTITY == 0) return super.isDead();
		if(age<maxAge) return false;
		return quantity<=0;
	}

	@Override
	public double reduce(double val) {
		if(age>maxAge) return super.reduce(val);
		if(quantity < val) {
			val = quantity;
			quantity = 0;
		} else {quantity -= val; }
		return val;
	}

	@Override
	public String toString() {
		return String.format("Plant "+hashCode()+"{age=%d(%.2f); q=%.10f; next repr:%d}",
				age,1d*age/maxAge, quantity, nextReproduction);
	}

}

