// https://searchcode.com/api/result/137113236/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ecosim.model;

import ecosim.model.Animal.Feeding;
import ecosim.utils.Object2D;
import ecosim.utils.Vector2D;
import java.io.Serializable;

/**
 *
 * @author positron
 */
public abstract class FoodPiece implements Object2D, Tickable,
		Eatable, Mortal, Consumable, Serializable {
	protected final Vector2D pos;
	protected final World w;

	public final FoodType type;

	protected double quantity;

	public FoodPiece(World w, double x, double y) {
		this(w,x,y,3, FoodType.VEGETABLE);
	}

	public FoodPiece(World w, double x, double y, double amt ) {
		this(w,x,y,amt, FoodType.VEGETABLE);
	}

	public FoodPiece(World w, double x, double y, double amt, FoodType type ) {
		this.pos = w.fixVector( new Vector2D(x, y) );
		this.w=w;
		quantity = amt;
		this.type = type;
	}
	public FoodPiece(World w, Vector2D p, double amt, FoodType type ) {
		this.pos = w.fixVector( p );
		this.w=w;
		quantity = amt;
		this.type = type;
	}

	@Override
	public Vector2D getPosition() {
		return pos;
	}

	@Override
	public double reduce(double val) {
		if(quantity < val) {
			val = quantity;
			quantity = 0;
		} else {quantity -= val; }
		return val;
	}

	@Override
	public boolean isDepleted() { return quantity<=Environment.EPS; }

	@Override
	public boolean isDead() {
		return isDepleted();
	}

	public double getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return type+"{quantity=" + quantity + '}';
	}

	@Override
	public boolean isEatableBy(Animal a) {
		if(type==FoodType.VEGETABLE) return a.feedingHabbit == Feeding.HERBIVORE;
		return a.feedingHabbit == Feeding.CARNIVORE;
	}


	public static class OrdinaryFoodPiece extends FoodPiece {

		public OrdinaryFoodPiece(World w, double x, double y, double amt ) {
			super(w,x,y,amt, FoodType.VEGETABLE);
		}

		@Override
		public void tick() {

		}

	}
}

