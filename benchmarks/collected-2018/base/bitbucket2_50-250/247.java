// https://searchcode.com/api/result/66976308/

/**
 * FILE : SpecialShip.java
 * WRITER : Idan Brodet + idanbr2 + 300685278
 * WRITER : Yonatan Oren + oryonatan + 300704327
 * EXERCISE : intro2cs ex9 2011-2012  
 * DESCRIPTION:
 * Special ship, tries to run away from combat and shoots 6 cannons in different directions 
 * the shot delay was increased in order to reduce game difficulty.
 */
import intro.ex9.SpaceShipPhysics;

public class SpecialShip extends ComputerShip {
	
	private static final int SHOTS_DELAY = 24;
	
	/**
	 * Holds the cannons , represented as ExtendablePhysics objects.
	 */
	private ExtendablePhysics[] cannonsPhysics = { new ExtendablePhysics(),
			new ExtendablePhysics(),
			new ExtendablePhysics(),
			new ExtendablePhysics(),
			new ExtendablePhysics() };

	/* 
	 * Move in loops to the left
	 * while shooting all 6 cannons in different directions.
	 * @see SpaceShip#doSpecificAction(SpaceWars)
	 */
	@Override
	public void doSpecificAction(SpaceWars game) {
		SpaceShip otherShip = game.getClosestShipTo(this);
		moveInteract(otherShip, Interaction.escape);
		// Direct all cannons
		double angleDif = Math.PI / 3 ;
		for (int i = 0; i < cannonsPhysics.length; i++) {
			cannonsPhysics[i].setAngle(pos.getAngle() + angleDif * (i + 1));
			cannonsPhysics[i].setLocation(pos.getX(), pos.getY());
		}
		fire(game);
	}

	/* 
	 * Override fire to shoot special star shaped shots.
	 * @see SpaceShip#fire(SpaceWars)
	 */
	protected void fire(SpaceWars game) {
		if (shotsTurnCounter <= 0) {
			game.addShot(pos);
			// shoot all cannons
			for (SpaceShipPhysics direction : cannonsPhysics) {
				game.addShot(direction);
			}
			shotsTurnCounter = SHOTS_DELAY;
		}
	}
}

