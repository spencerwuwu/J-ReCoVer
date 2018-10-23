// https://searchcode.com/api/result/119652866/

package com.bri8.ship.main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import rokon.Debug;
import rokon.Hotspot;
import rokon.RenderHook;
import rokon.Rokon;
import rokon.Sprite;
import rokon.TextureAtlas;
import rokon.Emitters.ExplosionEmitter;
import rokon.Handlers.BasicHandler;
import rokon.OpenGL.GLRenderer;
import rokon.SpriteModifiers.Fade;
import rokon.SpriteModifiers.FadeTo;
import android.os.Build;
import android.util.Log;

import com.bri8.ship.ShipControl;
import com.bri8.ship.Hazards.Rock;
import com.bri8.ship.Objects.Sounds;
import com.bri8.ship.Objects.Sprites;
import com.bri8.ship.Objects.Textures;
import com.bri8.ship.Ships.BoundType;
import com.bri8.ship.feature.Constants;
import com.bri8.ship.feature.Hazard;
import com.bri8.ship.feature.Lives;
import com.bri8.ship.feature.Score;
import com.bri8.ship.feature.Statistics;
import com.bri8.ship.main.path.Cubic;
import com.bri8.ship.main.path.Curve;
import com.bri8.ship.main.path.Path;
import com.scoreloop.client.android.ui.ScoreloopManagerSingleton;

public abstract class BaseGame {
	public static long loopTime;

	public static BaseGame singleton;
	protected ShipControl shipControl;
	protected Rokon rokon;

	public long _startTime;

	public Ship[] ships;
	public Path touchPath;

	public Score score;

	public boolean showTouchPath;
	public int lastX;
	public int lastY;
	public int activeShip;
	public long pathFadeStart;

	public boolean submitScore;
	protected int width, height;
	protected int a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, aa;
	protected float x, y, cosx, cosy, z, arg, ex, ey;
	protected float drawX, drawY, alpha;
	protected double distance, redDistance;
	protected Cubic[] cx, cy;
	public long gameOverTime;
	public boolean gameOver;
	public boolean gameOverWaiting;

	public float[] floatBuffer = new float[Constants.MAX_BUFFER];
	public int floatBufferLength = 0;
	public float lastBufferX, lastBufferY, tx, ty, moveX, moveY;
	public int retries = 0;

	public boolean hasFrozen;

	public ByteBuffer bb;
	public FloatBuffer _vertexBuffer;

	public SpawnManager spawnManager = new SpawnManager(this);

	public Hazard[] hazards = new Hazard[Constants.MAX_HAZARDS];

	public boolean hasRadar;

	public boolean noTouch = true;

	protected int lastSpawn = -1;
	protected long gameOverTime2 = 0;

	public boolean gamePaused = false;

	public HashMap<Integer, Sprite> landedSprites = new HashMap<Integer, Sprite>();
	protected Sprite landingSprite;

	int mindistance;
	Ship tship;

	public BaseGame(ShipControl shipControlMaster, Rokon rokon, int maxShips) {
		this.shipControl = shipControlMaster;
		this.rokon = rokon;
		Constants.MAX_SHIPS = maxShips;
		if (Build.VERSION.SDK == "3")
			bb = ByteBuffer.allocate(8 * 4);
		else
			bb = ByteBuffer.allocateDirect(8 * 4);
		bb.order(ByteOrder.nativeOrder());
		_vertexBuffer = bb.asFloatBuffer();
		singleton = this;
		hasRadar = true;
	}

	// START

	public void startGame() {
		Sounds.playAmbientSound();
		_startTime = Rokon.time;
		rokon.setRenderHook(new ShipControlRenderHook());
		score = new Score();
		spawnManager = new SpawnManager(this);
		pathFadeStart = 0;
		hasFrozen = false;
		lastSpawn = -1;
		activeShip = -1;
		showTouchPath = false;
		gameOver = false;
		submitScore = false;
		gameOverWaiting = false;
		width = (int) ShipControl.gameWidth;
		height = Rokon.getRokon().getHeight();
		ships = new Ship[Constants.MAX_SHIPS];
		rokon.freeze();
		gameOverTime2 = _startTime + 5 * 60 * 1000;

		addSpriteSet(Sprites.scoreSprite);
		addSpriteSet(Sprites.hiscoreSprite);

		Sprites.gameBar.resetDynamics();
		Sprites.gameBar.setXY((ShipControl.gameWidth / 2) - 240, 0);

		if (ShipControl.isHighRes) {
			float start = (ShipControl.gameWidth / 2) - 240 + 11;
			Debug.print("DOING HIGH RES FROM " + start);
			Sprites.scoreSprite[0].resetDynamics();
			Sprites.scoreSprite[0].setXY(start + 10, 2);
			Sprites.scoreSprite[1].resetDynamics();
			Sprites.scoreSprite[1].setXY(start + 28, 2);
			Sprites.scoreSprite[2].resetDynamics();
			Sprites.scoreSprite[2].setXY(start + 46, 2);
			Sprites.scoreSprite[3].resetDynamics();
			Sprites.scoreSprite[3].setXY(start + 64, 2);
			Sprites.scoreSprite[4].resetDynamics();
			Sprites.scoreSprite[4].setXY(start + 82, 2);

			Sprites.hiscoreSprite[0].resetDynamics();
			Sprites.hiscoreSprite[0].setXY(start + 360, 2);
			Sprites.hiscoreSprite[1].resetDynamics();
			Sprites.hiscoreSprite[1].setXY(start + 360 + 18, 2);
			Sprites.hiscoreSprite[2].resetDynamics();
			Sprites.hiscoreSprite[2].setXY(start + 360 + 18 + 18, 2);
			Sprites.hiscoreSprite[3].resetDynamics();
			Sprites.hiscoreSprite[3].setXY(start + 360 + 18 + 18 + 18, 2);
			Sprites.hiscoreSprite[4].resetDynamics();
			Sprites.hiscoreSprite[4].setXY(start + 360 + 18 + 18 + 18 + 18, 2);
		} else {
			Sprites.scoreSprite[0].resetDynamics();
			Sprites.scoreSprite[0].setXY(22, 2);
			Sprites.scoreSprite[1].resetDynamics();
			Sprites.scoreSprite[1].setXY(40, 2);
			Sprites.scoreSprite[2].resetDynamics();
			Sprites.scoreSprite[2].setXY(58, 2);
			Sprites.scoreSprite[3].resetDynamics();
			Sprites.scoreSprite[3].setXY(76, 2);
			Sprites.scoreSprite[4].resetDynamics();
			Sprites.scoreSprite[4].setXY(94, 2);

			Sprites.hiscoreSprite[0].resetDynamics();
			Sprites.hiscoreSprite[0].setXY(370, 2);
			Sprites.hiscoreSprite[1].resetDynamics();
			Sprites.hiscoreSprite[1].setXY(388, 2);
			Sprites.hiscoreSprite[2].resetDynamics();
			Sprites.hiscoreSprite[2].setXY(406, 2);
			Sprites.hiscoreSprite[3].resetDynamics();
			Sprites.hiscoreSprite[3].setXY(424, 2);
			Sprites.hiscoreSprite[4].resetDynamics();
			Sprites.hiscoreSprite[4].setXY(442, 2);
		}

		rokon.addSprite(Sprites.blackBox1, Constants.BLACK_TOP);
		rokon.addSprite(Sprites.blackBox2, Constants.BLACK_TOP);

		rokon.addSprite(Sprites.gameBar, Constants.OVERLAY_LAYER);

		if (!Lives.infinite) {
			rokon.addSprite(Sprites.lifeSymbol, Constants.OVERLAY_LAYER);
			rokon.addSprite(Sprites.lives1, Constants.OVERLAY_LAYER);
			rokon.addSprite(Sprites.lives2, Constants.OVERLAY_LAYER);
			Sprites.lifeSymbol.addModifier(new Fade(3000, 1, false));
			Sprites.lives1.addModifier(new Fade(3000, 1, false));
			Sprites.lives2.addModifier(new Fade(3000, 1, false));
		}

		score.score = 0;
		score.landed = 0;
		lastScore = -1;

		updateHiscore();
		updateScore();
		updateLives();

		startGameHook();

		Sprites.black.setVisible(false);
		Sprites.pauseBox.setVisible(false);
		gamePaused = false;
		rokon.addSprite(Sprites.black, Constants.BLACK_LAYER);
		rokon.addSprite(Sprites.pauseBox, Constants.BLACK_TOP);

		rokon.unfreeze();
		rokon.hideAd();
	}

	public void startGameHook() {
	}

	public void addSpriteSet(Sprite[] sprite) {
		for (int i = 0; i < sprite.length; i++) {
			rokon.addSprite(sprite[i], Constants.OVERLAY_LAYER_2);

		}
	}

	// game loop to check and show score

	public Hotspot highScoreHotspot;
	public boolean showingHiscore;

	public void gameLoop() {
		loopTime = System.currentTimeMillis();

		if (gameOver) {
			if (submitScore && !showingHiscore) {

				rokon.addSprite(Sprites.newHighScore, Constants.OVERLAY_LAYER_2);
				highScoreHotspot = new Hotspot(Sprites.newHighScore);
				rokon.addHotspot(highScoreHotspot);
				showingHiscore = true;
			}
			if (loopTime > gameOverTime + 4750) {

			}
			if (loopTime > gameOverTime + 4000) {
				if (Sprites.finalScoreSprite[4].isAnimating()) {
					Sprites.finalScoreSprite[4].stopAnimation();
					Sprites.finalScoreSprite[4].setTileIndex(Sprites.finalScoreSprite[4].intVar1);
					Sprites.finalLandedSprite[4].stopAnimation();
					Sprites.finalLandedSprite[4].setTileIndex(Sprites.finalLandedSprite[4].intVar1);
				}
			}
			if (loopTime > gameOverTime + 3750) {
				if (Sprites.finalScoreSprite[3].isAnimating()) {
					Sprites.finalScoreSprite[3].stopAnimation();
					Sprites.finalScoreSprite[3].setTileIndex(Sprites.finalScoreSprite[3].intVar1);
					Sprites.finalLandedSprite[3].stopAnimation();
					Sprites.finalLandedSprite[3].setTileIndex(Sprites.finalLandedSprite[3].intVar1);
				}
			}
			if (loopTime > gameOverTime + 3500) {
				if (Sprites.finalScoreSprite[2].isAnimating()) {
					Sprites.finalScoreSprite[2].stopAnimation();
					Sprites.finalScoreSprite[2].setTileIndex(Sprites.finalScoreSprite[2].intVar1);
					Sprites.finalLandedSprite[2].stopAnimation();
					Sprites.finalLandedSprite[2].setTileIndex(Sprites.finalLandedSprite[2].intVar1);
				}
			}
			if (loopTime > gameOverTime + 3250) {
				if (Sprites.finalScoreSprite[1].isAnimating()) {
					Sprites.finalScoreSprite[1].stopAnimation();
					Sprites.finalScoreSprite[1].setTileIndex(Sprites.finalScoreSprite[1].intVar1);
					Sprites.finalLandedSprite[1].stopAnimation();
					Sprites.finalLandedSprite[1].setTileIndex(Sprites.finalLandedSprite[1].intVar1);
				}
			}
			if (loopTime > gameOverTime + 3000) {
				if (Sprites.finalScoreSprite[0].isAnimating()) {
					Sprites.finalScoreSprite[0].stopAnimation();
					Sprites.finalScoreSprite[0].setTileIndex(Sprites.finalScoreSprite[0].intVar1);
					Sprites.finalLandedSprite[0].stopAnimation();
					Sprites.finalLandedSprite[0].setTileIndex(Sprites.finalLandedSprite[0].intVar1);
				}
			}
			if (gameOverWaiting && loopTime > gameOverTime + 2500) {
				gameOverWaiting = false;
				rokon.freeze();
				rokon.addSprite(Sprites.gameOver, Constants.OVERLAY_LAYER_2);
				rokon.addSprite(Sprites.gameOverScore, Constants.OVERLAY_LAYER_2);
				rokon.addSprite(Sprites.gameOverLanded, Constants.OVERLAY_LAYER_2);
				for (int i = 0; i < Sprites.finalScoreSprite.length; i++)
					rokon.addSprite(Sprites.finalScoreSprite[i], Constants.OVERLAY_LAYER_2);
				for (int i = 0; i < Sprites.finalLandedSprite.length; i++)
					rokon.addSprite(Sprites.finalLandedSprite[i], Constants.OVERLAY_LAYER_2);
				Sprites.finalScoreSprite[0].animateRandom(14, 23, 18);
				Sprites.finalScoreSprite[1].animateRandom(14, 23, 15);
				Sprites.finalScoreSprite[2].animateRandom(14, 23, 20);
				Sprites.finalScoreSprite[3].animateRandom(14, 23, 23);
				Sprites.finalScoreSprite[4].animateRandom(14, 23, 21);
				Sprites.finalLandedSprite[0].animateRandom(14, 23, 18);
				Sprites.finalLandedSprite[1].animateRandom(14, 23, 15);
				Sprites.finalLandedSprite[2].animateRandom(14, 23, 20);
				Sprites.finalLandedSprite[3].animateRandom(14, 23, 23);
				Sprites.finalLandedSprite[4].animateRandom(14, 23, 21);
				rokon.unfreeze();
				Debug.print("Score: " + score.score + " l=" + score.landed);
				Sprites.setSpriteNumberVars(Sprites.finalScoreSprite, score.score);
				Sprites.setSpriteNumberVars(Sprites.finalLandedSprite, score.landed);
			}
			return;
		}
		spawnManager.spawnLoop();
		hazardLoop();
		gameLoopHook();
	}

	public void checkCollisions() {
		if (gameOver)
			return;
		try {
			for (l = 0; l < Constants.MAX_SHIPS; l++)
				if (ships[l] != null)
					ships[l].showRed = false;

			for (l = 0; l < Constants.MAX_SHIPS; l++)
				if (ships[l] != null)
					for (o = 0; o < Constants.MAX_SHIPS; o++)
						if (ships[o] != null)
							if (o != l) {
								if (ships[o].active && ships[l].active) {
									redDistance = Math.pow((ships[o].sprite.getX() + (ships[o].size / 2)) - (ships[l].sprite.getX() + (ships[l].size / 2)), 2)
											+ Math.pow((ships[o].sprite.getY() + (ships[o].size / 2)) - (ships[l].sprite.getY() + (ships[l].size / 2)), 2);
									if (redDistance < Constants.MIN_DISTANCE_WARNING) {
										ships[o].showRed = true;
										ships[l].showRed = true;
									}
									if (redDistance < Math.pow((ships[o].size + ships[l].size) / 4, 2)) {
										ships[o].showRed = true;
										ships[l].showRed = true;
										ex = ships[l].sprite.getX() - ((ships[l].sprite.getX() - ships[o].sprite.getX()) / 2);
										ey = ships[l].sprite.getY() - ((ships[l].sprite.getY() - ships[o].sprite.getY()) / 2);
										rokon.getLayer(Constants.PARTICLE_LAYER).addEmitter(
												new ExplosionEmitter(ex, ey, Textures.particle, 30, -30, 30, -30, 30, 0, 0, 0, 0, 1, 20, -20, -100, 0.5f, 0.9f));

										if (!shipCrashedHook())
											shipCrashed();
										if (Lives.infinite || Lives.lives > 1)
											score.reduce(15);
										updateScore();
										shipCrashed(ships[o], o);
										shipCrashed(ships[l], l);
										return;
									}
								}
							}
			checkLanding();
		} catch (Exception e) {
		}
	}

	public void addToFloatBuffer(float value) {
		floatBuffer[floatBufferLength++] = value;
	}

	public int getShipCount() {
		v = 0;
		for (u = 0; u < Constants.MAX_SHIPS; u++)
			if (ships[u] != null)
				v++;
		return v;
	}

	public void shipCrashed() {
		Sounds.crashSound();
		Lives.remove();
		updateLives();
		if (!Lives.isAlive())
			gameOver();
	}

	// HAZZZARD
	public void hazardLoop() {
		try {
			for (t = 0; t < Constants.MAX_HAZARDS; t++) {
				if (hazards[t] != null) {
					hazards[t].loop(this);
				}
			}
		} catch (Exception e) {
		}
	}

	public void createHazard(Hazard hazard) {
		for (s = 0; s < Constants.MAX_HAZARDS; s++) {
			if (hazards[s] == null) {
				hazards[s] = hazard;
				return;
			}
		}
		Debug.print("NO ROOM FOR A NEW HAZARD");
	}

	public void spawnHazard(int type) {
		// -1 = fixed rock 0 = balloon, 1 = tower, 2 = geese
		if (type == -1) {
			Rock rock = new Rock(100, 160, Textures.Hazards.rockLeft);
			createHazard(rock);
			Rock rock2 = new Rock(width - 170, 120, Textures.Hazards.rockRight);
			createHazard(rock2);
		}

	}

	public void removeHazard(Hazard hazard) {
		try {
			for (s = 0; s < Constants.MAX_HAZARDS; s++) {
				if (hazards[s] != null)
					if (hazards[s].equals(hazard)) {
						hazards[s] = null;
						hazard = null;
						return;
					}
			}
		} catch (Exception e) {
		}
		Debug.print("UNABLE TO REMOVE HAZARD");
	}

	public void gameLoopHook() {
	}

	public void updateHiscore() {
		Sprites.setSpriteNumbers(Sprites.hiscoreSprite, Statistics.get(Statistics.HIGH_SCORE), true);
	}

	int lastScore = -1;

	public void updateScore() {
		if (score.score != lastScore) {
			Sprites.setSpriteNumbers(Sprites.scoreSprite, score.score, true);
		}
	}

	public long lastDraw = 0;
	public float afactor;

	public void onDrawHook(GL10 gl, int layer) {
	}

	// Ship Motion
	protected abstract void checkLanding();

	public abstract boolean isOffScreen(Ship ship);

	public abstract boolean isLandedPath(Ship ship, float x1, float y1, float x2, float y2);

	public boolean shipCrashedHook() {
		return false;
	}

	public boolean shipHazardHook() {
		return false;
	}

	public boolean shipBonusHook() {
		return false;
	}

	public void updateLives() {
		if (Lives.lives > 99)
			Lives.lives = 99;
		if (Lives.lives < 0)
			Lives.lives = 0;
		String lifeString = String.valueOf(Lives.lives);
		if (lifeString.length() == 1) {
			Sprites.lives2.setVisible(false);
			Sprites.lives1.setTileIndex(Lives.lives + 1);
		} else {
			Sprites.lives2.setVisible(true);
			Sprites.lives1.setTileIndex(Integer.parseInt(lifeString.substring(0, 1)) + 1);
			Sprites.lives2.setTileIndex(Integer.parseInt(lifeString.substring(1, 2)) + 1);
		}
	}

	// Crash
	public void shipCrashed(Ship ship, final int idx) {
		Debug.print("Destoying t=" + t);
		try {
			rokon.removeHotspot(ship.hotspot);
			this.ships[idx].active = false;
			this.ships[idx].velocity = 0;
			this.ships[idx].pathFollower.velocity = 0;
			this.ships[idx].sprite.stop();
			this.ships[idx].sprite.addModifier(new Fade(3000, 1, true, new BasicHandler() {
				@Override
				public void onFinished() {
					ShipControl.singleton.game.ships[idx].sprite.markForRemoval();
					rokon.removeSprite(Game.singleton.ships[idx].sprite, Constants.SHIP_LAYER);
					ShipControl.singleton.game.ships[idx] = null;
				}
			}));
		} catch (Exception e) {
		}
	}

	// pause

	public void pauseButton() {
		if (gamePaused) {
			rokon.unpause();
			gamePaused = false;
			Sprites.black.setVisible(false);
			Sprites.pauseBox.setVisible(false);
		} else {
			gamePaused = true;
			rokon.pause();
			Debug.print("PAUSING!!!!!!!");
			Sprites.black.setAlpha(0.7f);
			Sprites.black.setVisible(true);
			Sprites.pauseBox.setVisible(true);
		}
	}

	public void gameOver() {
		try {
			rokon.showAd();
			Debug.print("Game over..stopping ambient sound");
			Sounds.stopAmbientSound();
			// rokon.fadeAd(0.5f); //gives error only original thread can touch
			// this view etc
			if (gamePaused) {
				Sprites.pauseBox.setVisible(false);
			}

			for (int uhj = 0; uhj < ships.length; uhj++) {
				try {
					ships[uhj].sprite.stopAnimation();
				} catch (Exception e) {
				}
			}

			gameOver = true;
			gameOverWaiting = true;
			gameOverTime = loopTime;
			showingHiscore = false;

			int minutes = Math.round((Rokon.time - _startTime) / 1000 / 60);

			Statistics.increase(Statistics.MINS_PLAYED, minutes);
			Statistics.increase(Statistics.LANDED, score.landed);
			Statistics.increase(Statistics.TOTAL_SCORE, score.score);
			Statistics.increase(Statistics.GAMES, 1);

			Statistics.increase("totalmins", minutes);
			submitScore = true;

			if (score.score > Statistics.get(Statistics.HIGH_SCORE)) {
				Statistics.save(Statistics.HIGH_SCORE, score.score);
				// Submits the score. Pass null here for the game "mode"
				ScoreloopManagerSingleton.get().onGamePlayEnded((double) score.score, null);

			}

			for (l = 0; l < Constants.MAX_SHIPS; l++)
				if (ships[l] != null && (ships[l].sprite.getVelocityX() != 0 || ships[l].sprite.getVelocityY() != 0)) {
					ships[l].sprite.stop();
					ships[l].freeze = true;
				}

			rokon.resetHotspots();
			Debug.print("GAME OVER GAME OVER");
			for (l = 0; l < Constants.MAX_SHIPS; l++)
				if (ships[l] != null) {
					ships[l].sprite.stop();
					ships[l].freeze = true;
				}

			for (l = 0; l < landedSprites.size(); l++) {
				landingSprite = landedSprites.get(l);

			}

			Sprites.black.resetModifiers();
			Sprites.black.setAlpha(0f);
			Sprites.black.addModifier(new FadeTo(3000, 0.75f));
			Sprites.black.setVisible(true);
			Sprites.gameBar.accelerate(0, -10);
			Sprites.scoreSprite[0].accelerate(0, -10);
			Sprites.scoreSprite[1].accelerate(0, -10);
			Sprites.scoreSprite[2].accelerate(0, -10);
			Sprites.scoreSprite[3].accelerate(0, -10);
			Sprites.scoreSprite[4].accelerate(0, -10);
			Sprites.hiscoreSprite[0].accelerate(0, -10);
			Sprites.hiscoreSprite[1].accelerate(0, -10);
			Sprites.hiscoreSprite[2].accelerate(0, -10);
			Sprites.hiscoreSprite[3].accelerate(0, -10);
			Sprites.hiscoreSprite[4].accelerate(0, -10);
			Sprites.lifeSymbol.accelerate(0, 10);
			Sprites.lives1.accelerate(0, 10);
			Sprites.lives2.accelerate(0, 10);
		} catch (Exception e) {
			Debug.print("game over exception");
			e.printStackTrace();
		}
		if (Constants.TEST_MODE_PLAY) {
			Sounds.stopAmbientSound();
			System.exit(0);
		}
	}

	public void calculateFinePath(int index) {
		try {
			p = index;
			if (ships[p] != null && ships[p].path.count > 1) {
				floatBufferLength = 0;
				lastBufferX = -1;
				lastBufferY = -1;
				ships[p].path.padFront((int) (ships[p].sprite.getX() + (ships[p].size / 2)), (int) (ships[p].sprite.getY() + (ships[p].size / 2)));
				for (m = 0; m < ships[p].path.count; m++) {
					ships[p].path.oldx[m] = ships[p].path.x[m];
					ships[p].path.oldy[m] = ships[p].path.y[m];
				}
				ships[p].path.oldcount = ships[p].path.count;
				cx = Curve.calcNaturalCubic(ships[p].path.count - 1, ships[p].path.x);
				cy = Curve.calcNaturalCubic(ships[p].path.count - 1, ships[p].path.y);
				for (q = 0; q < cx.length; q++) {
					if (!ships[p].path.willLand) {
						for (r = 1; r <= Constants.INTERPOLATION_STEPS; r++) {
							if (!ships[p].path.willLand) {
								if (floatBufferLength < Constants.MAX_BUFFER - 2) {
									z = r / (float) Constants.INTERPOLATION_STEPS;
									tx = cx[q].eval(z);
									ty = cy[q].eval(z);
									if (lastBufferX != tx || lastBufferY != ty) {
										addToFloatBuffer(tx);
										addToFloatBuffer(ty);
										ships[p].path.addVertexDummy((int) tx, (int) ty);
										if (isLandedPath(ships[p], tx, ty, lastBufferX, lastBufferY)) {
											ships[p].path.willLand = true;
											if (shipControl.soundOn)
												Sounds.click();
											ships[p].path.oldcount = q + 1;
											break;
										}
										lastBufferX = tx;
										lastBufferY = ty;
									}
								}
							}
						}
					}
				}

				ships[p].path.vertexBuffer.position(0);
				for (q = 0; q < floatBufferLength; q++)
					ships[p].path.vertexBuffer.putFloat(floatBuffer[q]);
				ships[p].path.vertexBuffer.position(0);

				ships[p].path.vertexCount = floatBufferLength / 2;
				ships[p].path.atVertex = 0;
				ships[p].path.hasBuffer = true;
				ships[p].path.x = ships[p].path.newx;
				ships[p].path.y = ships[p].path.newy;
				ships[p].path.count = ships[p].path.newcount;
				ships[p].path.trimFirst();
			}
			retries = 0;
		} catch (Exception e) {
			retries++;
			if (retries < 5) {
				calculateFinePath(index);
				return;
			} else {
				Debug.print("ERROR IN PATH");
				e.printStackTrace();
			}
		}
	}

	// Called every FPS?
	public class ShipControlRenderHook extends RenderHook {

		public void onDrawBackground(GL10 gl) {

		}

		public void onDraw(GL10 gl, int layer) {

			if (ShipControl.changeGraphics) {
				try {
					for (int kkk = 0; kkk < TextureAtlas.MAX_TEXTURES; kkk++)
						if (Rokon.getRokon().getTextureAtlas().texId[kkk] > 0) {
							Debug.print("Changing graphics of texture " + kkk);
							if (shipControl.graphicsHi) {
								gl.glBindTexture(GL10.GL_TEXTURE_2D, kkk);
								gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
								gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
							} else {
								gl.glBindTexture(GL10.GL_TEXTURE_2D, kkk);
								gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
								gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
							}
						}
					gl.glBindTexture(GL10.GL_TEXTURE_2D, Rokon.getRokon().currentTexture);
				} catch (Exception e) {
				}
				ShipControl.changeGraphics = false;
			}

			if (shipControl.gameState != ShipControl.PLAYING)
				return;

			if (layer == Constants.OVERLAY_LAYER_2) {
				if (!gameOver)
					for (g = 0; g < Constants.MAX_SHIPS; g++) {
						if (ships[g] != null) {
							if (isOffScreen(ships[g])) {
								gl.glVertexPointer(2, GL11.GL_FLOAT, 0, GLRenderer.vertexBuffer);
								if (cosx + ships[g].size < ships[g].size)
									cosx = 5;
								if (cosx + ships[g].size > width)
									cosx = width - Constants.WARNING_SIZE - 5;
								if (cosy + ships[g].size < ships[g].size)
									cosy = 5;
								if (cosy + ships[g].size > height)
									cosy = height - Constants.WARNING_SIZE - 5;
								gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.warningCircleBuffer.buffer);
								gl.glLoadIdentity();
								gl.glTranslatef(cosx, cosy, 0);
								gl.glScalef(Constants.WARNING_SIZE, Constants.WARNING_SIZE, 0);
								gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
							}
						}
					}
			}

			if (layer == Constants.BONUS_LAYER) {
				if (gameOver && !hasFrozen) {
					for (l = 0; l < Constants.MAX_SHIPS; l++)
						if (ships[l] != null && (ships[l].sprite.getVelocityX() != 0 || ships[l].sprite.getVelocityY() != 0)) {
							ships[l].sprite.stop();
							ships[l].freeze = true;
						}
					hasFrozen = true;
				}

				if (Rokon.time > lastDraw + 100) {
					checkCollisions();
					lastDraw = Rokon.time;
				}

				gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				gl.glDisable(GL10.GL_TEXTURE_2D);

				try {
					for (g = 0; g < Constants.MAX_SHIPS; g++) {
						if (ships[g] != null) {
							gl.glVertexPointer(2, GL11.GL_FLOAT, 0, GLRenderer.vertexBuffer);
							if (ships[g].showRed) {
								gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
								gl.glEnable(GL10.GL_TEXTURE_2D);
								gl.glColor4f(1, 1, 1, 1);
								gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.circleTextureBuffer.buffer);
								gl.glLoadIdentity();
								gl.glTranslatef(ships[g].sprite.getX(), ships[g].sprite.getY(), 0);
								gl.glScalef(ships[g].size, ships[g].size, 0);
								gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
								gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
								gl.glDisable(GL10.GL_TEXTURE_2D);
							}
							if (ships[g].path.count > 0) {
								if (g == activeShip && showTouchPath) {

									// make the ship glow
									gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
									gl.glEnable(GL10.GL_TEXTURE_2D);
									gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_DST_ALPHA);
									gl.glColor4f(1, 1, 1, 1.0f);

									gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.glowTextureBuffer.buffer);
									gl.glLoadIdentity();
									gl.glTranslatef(ships[g].sprite.getX() - (ships[g].size / 2), ships[g].sprite.getY() - (ships[g].size / 2), 0);
									gl.glScalef(ships[g].size * 2, ships[g].size * 2, 0);
									gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

									// dislay port to which this ship belongs

									highlightPort(gl);

									gl.glScalef(80, 80, 0);
									gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

									gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.waypointTextureBuffer.buffer);
									// display ship path.TODO: resize line
									if (ships[g].path.hasBuffer) {
										for (h = 0; h < ships[g].path.oldcount; h++) {
											drawX = ships[g].path.oldx[h];
											drawY = ships[g].path.oldy[h];
											gl.glLoadIdentity();
											gl.glTranslatef(drawX - 15, drawY - 15, 0);
											gl.glScalef(10, 10, 0); // gl.glScalef(5,
																	// 5, 0);
											gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
										}
									} else {
										for (h = 0; h < ships[g].path.count; h++) {
											drawX = ships[g].path.x[h];
											drawY = ships[g].path.y[h];
											gl.glLoadIdentity();
											gl.glTranslatef(drawX - 15, drawY - 15, 0);
											gl.glScalef(10, 10, 0); // gl.glScalef(5,
																	// 5, 0);
											gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
										}
									}
									gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
									gl.glDisable(GL10.GL_TEXTURE_2D);
									gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
								}
								if (hasRadar && ships[g].path.hasBuffer) {
									gl.glVertexPointer(2, GL11.GL_FLOAT, 0, ships[g].path.vertexBuffer);
									gl.glLoadIdentity();
									afactor = ships[g].sprite.getAlpha();
									if (ships[g].path.willLand)
										gl.glColor4f(1f, 0.8f, 0.0f, 0.6f * afactor);
									else
										gl.glColor4f(1, 1, 1, 0.6f * afactor);

									//drawAAPath(gl, 0.5f, 0.8f);
									drawAAPath(gl, 0.8f, 0.25f);
									drawAAPath(gl, 0.24f, 0.45f);
								}
							}
						}
					}
				} catch (Exception e) {
					Debug.print("ERROR " + e.getMessage());
					e.printStackTrace();
				}
				gl.glColor4f(1, 1, 1, 1);
				gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				gl.glEnable(GL10.GL_TEXTURE_2D);
			}

			onDrawHook(gl, layer);
		}


		public void drawAAPath(GL10 gl, float offx, float offy) {
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL10.GL_BLEND);
			gl.glEnable(GL10.GL_LINE_SMOOTH);
			
			gl.glTranslatef(offx, offy, 0);
			if (ships[g].path.atVertex > 0)
				gl.glDrawArrays(GL10.GL_LINE_STRIP, ships[g].path.atVertex, ships[g].path.vertexCount - ships[g].path.atVertex);
			else
				gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, ships[g].path.vertexCount);
			gl.glTranslatef(-offx, -offy, 0);

		}

		// Show pointer to the ships port
		private void highlightPort(GL10 gl) {
			if (BoundType.EAST_BOUND_GREEN.equals(ships[g].boundType)) {
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.portHighlightBufferRight.buffer);
				gl.glLoadIdentity();
				gl.glTranslatef(rokon.getWidth() - 83, 76, 0);
			} else if (BoundType.WEST_BOUND_RED.equals(ships[g].boundType)) {
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.portHighlightBufferLeft.buffer);
				gl.glLoadIdentity();
				gl.glTranslatef(-5, 165, 0);
			} else if (BoundType.SOUTH_BOUND_YELLOW.equals(ships[g].boundType)) {
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.portHighlightBufferDown.buffer);
				gl.glLoadIdentity();
				gl.glTranslatef(rokon.getWidth() - 160, rokon.getHeight() - 100, 0);
			} else if (BoundType.NORTH_BOUND_BLACK.equals(ships[g].boundType)) {
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.portHighlightBufferUp.buffer);
				gl.glLoadIdentity();
				gl.glTranslatef(120, 50, 0);
			} else if (BoundType.NORTH_BOUND_BLUE.equals(ships[g].boundType)) {
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, Textures.portHighlightBufferUp.buffer);
				gl.glLoadIdentity();
				gl.glTranslatef(rokon.getWidth() / 2, 50, 0);
			}
		}

	}

	public abstract void spawnShip(int type);

	public void paused() {
		Debug.print("game paused");
		Sounds.stopAmbientSound();
	}

	public void unpaused() {
		Sounds.playAmbientSound();
		Debug.print("game unpaused");
	}

	public void debugMode() {
		if (Debug.DEBUG_MODE) {
			Debug.print("old tl=" + Statistics.get("totallanded"));
			Statistics.save("totallanded", 999);
			Debug.print("new tl=" + Statistics.get("totallanded"));
		}
	}

	public void onHotspotTouched(Hotspot hotspot) {
		if (rokon.isPaused()) {

			return;
		}
		try {

			if (hotspot.equals(highScoreHotspot)) {
				Debug.print("show high scores");
				// Intent intent = new Intent(ShipControl.singleton,
				// HighScores.class);
				// intent.putExtra("score", score.score);
				// ShipControl.singleton.startActivity(intent);

				double scoreResult = (double) score.score;
				// Submits the score. Pass null here for the game "mode"
				ScoreloopManagerSingleton.get().onGamePlayEnded(scoreResult, null);
				return;
			}
		} catch (Exception e) {
			Log.e("rokon",e.getMessage(),e);
		}
		if (noTouch) {
			if (!showTouchPath) {
				for (b = 0; b < Constants.MAX_SHIPS; b++) {
					if (ships[b] != null && ships[b].hotspot.equals(hotspot)) {
						Debug.print("Selected ship " + b);
						showTouchPath = true;
						activeShip = b;
						ships[b].path.resetPath();
						return;
					}
				}
			}
		}
	}
}

