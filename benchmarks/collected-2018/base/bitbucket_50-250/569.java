// https://searchcode.com/api/result/119652836/

package com.bri8.ship.Hazards;

import rokon.Debug;
import rokon.Rokon;
import rokon.Texture;
import rokon.Emitters.ExplosionEmitter;

import com.bri8.ship.Objects.Sounds;
import com.bri8.ship.Objects.Textures;
import com.bri8.ship.feature.Constants;
import com.bri8.ship.feature.Hazard;
import com.bri8.ship.main.BaseGame;
import com.bri8.ship.main.Ship;

public class Rock extends Hazard {

	public final static int COLLISION_THRESHOLD = 100;
	public final static int LIFE_SPAN = -1;
	private int i;
	private double distance;

	public Rock(int x, int y, Texture texture) {
		super(x, y, 43, 49, LIFE_SPAN, texture, 0);
	}

	public float px, py;

	@Override
	public void checkCollisions(BaseGame game) {
		try {
			for (i = 0; i < Constants.MAX_SHIPS; i++) {
				if (game.ships[i] != null && game.ships[i].active) {
					px = game.ships[i].sprite.getX() + (game.ships[i].sprite.getWidth() / 2);
					py = game.ships[i].sprite.getY() + (game.ships[i].sprite.getHeight() / 2);
					
					if (px > sprite.getX()  && px < sprite.getX()+43 )
						if (py > sprite.getY() + 0 && py < sprite.getY() +49 ) {
							collision(game, game.ships[i], i);
							break;
						}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void collision(BaseGame game, Ship ship, int idx) {
		Debug.print("CRASH!");
		Sounds.crashSoundSmall();
		Rokon.getRokon().getLayer(Constants.PARTICLE_LAYER).addEmitter(
				new ExplosionEmitter(ship.sprite.getX() + (ship.sprite.getWidth() / 2), ship.sprite.getY() + (ship.sprite.getHeight() / 2), Textures.particle, 30, -30, 30, -30, 30, 0, 0, 0, 0, 1, 20,
						-20, -100, 0.5f, 0.9f));
		game.score.reduce(Constants.HAZARD_SCORE_REDUCTION);
		game.updateScore();
		game.shipCrashed(ship, idx);
		// active = false;
		// sprite.resetModifiers();
		// sprite.resetDynamics();
		// sprite.addModifier(new Fade(1000, 1, true));
	}
}

