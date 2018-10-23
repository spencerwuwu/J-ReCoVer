// https://searchcode.com/api/result/67291351/

package de.ggj14.wap.states;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

import com.esotericsoftware.minlog.Log;

import de.ggj14.wap.WapGame;
import de.ggj14.wap.ai.AI;
import de.ggj14.wap.common.communication.GameInformation;
import de.ggj14.wap.common.communication.MoveAction;
import de.ggj14.wap.common.datamodel.Faction;
import de.ggj14.wap.common.datamodel.GameState;
import de.ggj14.wap.common.gamefield.objects.Minion;
import de.ggj14.wap.frontend.WapFrontend;
import de.ggj14.wap.sound.SoundManager;
import de.ggj14.wap.sound.SoundManager.Sounds;

public class Singleplayer extends BasicGameState implements GameAspect {
	
	private ScheduledExecutorService executor;
	private final BlockingQueue<MoveAction> pendingMoveActions = new LinkedBlockingDeque<MoveAction>();
	private SoundManager soundManager;
	private int timeSinceLastFrame;
	private Faction gameOver = Faction.GREY;
	private GameState gameState;
	private WapFrontend frontend;
	private Image waitingScreen[][];
	private final Faction myFaction = Faction.PLAYER1;
	public static AI ai;
	public static GameInformation gameInformation;
	private int columns;
	private int rows;
	
	private final int leaveGameCooldown = 3000;
	private int leaveGameCounter = 0;

	private Set<Integer> countdownSoundPlayed = new HashSet<>();
	private StateBasedGame game;
	
	public Singleplayer(SoundManager sm) {
		super();
		soundManager = sm;
	}
	
	/**
	 * async send move action
	 */
	public class SendMoveAction implements Runnable {
		public void run() {
			try {
				for (;;) {
					final MoveAction moveAction = pendingMoveActions.take();
					try {
						gameState.doMoveAction(moveAction);
					} catch (Exception e) {
						Log.error("Move action could not be sent", e);
					}
				}
			} catch (InterruptedException e) {
				// Interrupt indicates shut down
			}
			Log.debug("Send move action thread has been shut down");
		}
	}

	@Override
	public void init(GameContainer arg0, StateBasedGame game) throws SlickException {
		this.game = game;
		Log.debug("Init Singleplayer-Mode");

		waitingScreen = new Image[2][6];
		waitingScreen[0][0] = new Image("screens/cooldown_orange_1.png");
		waitingScreen[0][1] = new Image("screens/cooldown_orange_1.png");
		waitingScreen[0][2] = new Image("screens/cooldown_orange_2.png");
		waitingScreen[0][3] = new Image("screens/cooldown_orange_3.png");
		waitingScreen[0][4] = new Image("screens/cooldown_orange_4.png");
		waitingScreen[0][5] = new Image("screens/startscreen_orange.png");

		waitingScreen[1][0] = new Image("screens/cooldown_blue_1.png");
		waitingScreen[1][1] = new Image("screens/cooldown_blue_1.png");
		waitingScreen[1][2] = new Image("screens/cooldown_blue_2.png");
		waitingScreen[1][3] = new Image("screens/cooldown_blue_3.png");
		waitingScreen[1][4] = new Image("screens/cooldown_blue_4.png");
		waitingScreen[1][5] = new Image("screens/startscreen_blue.png");
		
		frontend = new WapFrontend();
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics graphics) throws SlickException {
			// WAITING FOR GAME:
			if (gameState.getStartCountdown() > 0) {
				frontend.render(gameState, graphics);
				waitingScreen[myFaction.ordinal()-1][gameState
						.getStartCountdown()].draw(WapGame.getXOffset(),
						WapGame.getYOffset());
				
				if(!countdownSoundPlayed.contains(gameState.getStartCountdown())) {
					countdownSoundPlayed.add(gameState.getStartCountdown());
					soundManager.playSound(Sounds.GAME_COUNTDOWN_4_TO_1);
				}
			} 
			// GAME IS RUNNING:
			else if (gameState.getStartCountdown() == 0) {
				if(!countdownSoundPlayed.contains(gameState.getStartCountdown())) {
					countdownSoundPlayed.add(gameState.getStartCountdown());
					soundManager.playSound(Sounds.GAME_COUNTDOWN_0);
				}
				frontend.render(gameState, graphics);
				if (gameOver != Faction.GREY) {
					graphics.setFont(font);
					graphics.setColor(Color.black);
					final String colorString = gameOver.getColor().toUpperCase();
					graphics.drawString(colorString + " is prophet!",
							WapGame.getXOffset() + 323, WapGame.getYOffset() + 323);
					graphics.setColor(Color.white);
					graphics.drawString(colorString + " is prophet!",
							WapGame.getXOffset() + 320, WapGame.getYOffset() + 320);
					
					if (leaveGameCounter >= leaveGameCooldown) {
						graphics.setColor(Color.black);
						graphics.drawString("(press ESC key to continue)", WapGame.getXOffset() + 323, WapGame.getYOffset() + 403);
						graphics.setColor(Color.white);
						graphics.drawString("(press ESC key to continue)", WapGame.getXOffset() + 320, WapGame.getYOffset() + 400);
					}
				}
			}
	}
	
	@Override
	public void keyReleased(int key, char c) {
		if(game.getCurrentStateID() != State.SINGLEPLAYER.getID()){
			return;
		}
		// when ESC is pressed we want to go back to the main menu
		if (key == Input.KEY_ESCAPE) {
			game.enterState(State.MENU.getID());
		}
	}


	@Override
	public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException {
		
		// reduce count down for playing
		if(gameState.getStartCountdown() != 0 && (4-(timeSinceLastFrame/1000)) < gameState.getStartCountdown()) {
			gameState.setStartCountdown(4-(timeSinceLastFrame/1000));
			return;
		}
		
		synchronized (gameState) {
			timeSinceLastFrame += delta;
			if (gameOver == Faction.GREY) {
				ai.update(gameState);
				gameState.update(delta);
				gameOver = gameState.checkWinningCondition();

				List<MoveAction> ma = frontend.update(delta, gameState,container.getInput(), soundManager);

				if (ma != null) {
					for(MoveAction action : ma) {
						if (this.gameState.getMinionMap().get(action.getObjectID())
								.getFaction().equals(myFaction)) {
							pendingMoveActions.add(action);
						} else {
							frontend.deselectMinion();
						}
					}
				}
			} else {
				if (leaveGameCounter <= leaveGameCooldown) {
					leaveGameCounter += delta;
				}
				for (Minion mini : gameState.getMinions().values()) {
					mini.dontMove();
				}
			}
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)	throws SlickException {
		Log.debug("Entering Singleplayer-Mode");
		
		columns = 6;
		rows = 4;
		ai.init(rows, columns);
		timeSinceLastFrame = 0;
		
		leaveGameCounter = 0;
		
		gameOver = Faction.GREY;
		
		try {
			gameState = new GameState();
			gameState.init(rows, columns, 10, true);
			gameState.setStartCountdown(4);
		} catch (Exception e) {
			Log.error("Found something." + e.toString());
		}

		frontend.init(gameState, myFaction,gameInformation);

		this.executor = Executors.newScheduledThreadPool(1);
		this.executor.submit(new SendMoveAction());
		
		pendingMoveActions.clear();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)	throws SlickException {
		Log.info("Shutting down Singleplayer-Mode");
		executor.shutdownNow();
		countdownSoundPlayed.clear();
		try {
			executor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.info("Interrupt while shutting down SingleplayerGame-Executor");
			e.printStackTrace();
		}
	}

	@Override
	public int getID() {
		return State.SINGLEPLAYER.getID();
	}
}

