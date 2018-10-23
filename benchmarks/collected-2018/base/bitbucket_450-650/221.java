// https://searchcode.com/api/result/55528019/

package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AbstractEntity;
import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;

/**
 * The main hook of our game. This class with both act as a manager
 * for the display and central mediator for the game logic. 
 * 
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place. With the help of an inner class it
 * will also allow the player to control the main ship.
 * 
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alient killed, played died) and will take
 * appropriate game actions.
 * 
 * @author Kevin Glass
 */
public class Game {

    /** The speed at which the player's ship should move (pixels/sec) */
    private static final double MOVE_SPEED = 300;

    /** The interval between our players shot (ms) */
    private static final long FIRING_INTERVAL = 500;

    private static final double ALIEN_SPEEDUP_AFTER_DEATH = 1.02;

    /** The strategy that allows us to use accelerate page flipping */
    private final BufferStrategy strategy;

    /** True if the game is currently "running", i.e. the game loop is looping */
    private final boolean gameRunning = true;

    /** The list of all the entities that exist in our game */

    private final List<AbstractEntity> entities = new ArrayList<AbstractEntity>();

    /** The list of entities that need to be removed from the game this loop */
    private final List<AbstractEntity> removeList = new ArrayList<AbstractEntity>();

    /** The entity representing the player */
    private AbstractEntity ship;

    /** The time at which last fired a shot */
    private long lastFire = 0;

    /** The number of aliens left on the screen */
    private int alienCount;

    /** The message to display which waiting for a key press */
    private String message = "";

    /** True if we're holding up game play until a key has been pressed */
    private boolean waitingForKeyPress = true;

    /** True if the left cursor key is currently pressed */
    private boolean leftPressed = false;

    /** True if the right cursor key is currently pressed */
    private boolean rightPressed = false;

    /** True if we are firing */
    private boolean firePressed = false;

    /** True if game logic needs to be applied this loop, normally as a result of a game event */
    private boolean logicRequiredThisLoop = false;

    /** The last time at which we recorded the frame rate */
    private long lastFpsTime;

    /** The current number of frames recorded */
    private int fps;

    /** The normal title of the game window */
    private final String windowTitle = "Space Invaders 102";

    /** The game window that we'll update with the frame count */
    private final JFrame container;

    /**
     * Construct our game and set it running.
     */
    public Game() {
        // create a frame to contain our game
        container = new JFrame(windowTitle);

        // get hold the content of the frame and set up the resolution of the game
        final JPanel panel = (JPanel)container.getContentPane();
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setLayout(null);

        // setup our canvas size and put it into the content of the frame
        final Canvas canvas = new Canvas();
        canvas.setBounds(0, 0, 800, 600);
        panel.add(canvas);

        // Tell AWT not to bother repainting our canvas 
        // since we're going to do that our self in accelerated mode
        canvas.setIgnoreRepaint(true);

        // finally make the window visible 
        container.pack();
        container.setResizable(false);
        container.setVisible(true);

        // add a listener to respond to the user closing the window. If they
        // do we'd like to exit the game
        container.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                System.exit(0);
            }
        });

        // add a key input system (defined below) to our canvas
        // so we can respond to key pressed
        canvas.addKeyListener(new KeyInputHandler());

        // request the focus so key events come to us
        canvas.requestFocus();

        // create the buffering strategy
        // which will allow AWT to manage our accelerated graphics
        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();

        // initialise the entities in our game 
        // so there's something to see at startup
        initEntities();
    }

    /**
     * Start a fresh game, 
     * this should clear out any old data and create a new set.
     */
    private void startGame() {
        // clear out any existing entities and intialise a new set
        entities.clear();
        initEntities();

        // blank out any keyboard settings we might currently have
        leftPressed = false;
        rightPressed = false;
        firePressed = false;
    }

    /**
     * Initialise the starting state of the entities (ship and aliens). 
     * Each entitiy will be added to the overall list of entities in the game.
     */
    private void initEntities() {
        // create the player ship and place it roughly in the center of the screen
        ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
        entities.add(ship);

        // create a block of aliens (5 rows, by 12 aliens, spaced evenly)
        alienCount = 0;
        for (int row = 0; row < 5; row++) {
            for (int x = 0; x < 12; x++) {
                entities.add(new AlienEntity(this, 100 + (x * 50), (50) + row * 30));
                alienCount++;
            }
        }
    }

    /**
     * Notification from a game entity that the logic of the game
     * should be run at the next opportunity (normally as a result of some
     * game event)
     */
    public void updateLogic() {
        logicRequiredThisLoop = true;
    }

    /**
     * Remove an entity from the game.
     * The entity removed will no longer move or be drawn.
     * 
     * @param entity The entity that should be removed
     */
    public void removeEntity(final AbstractEntity entity) {
        removeList.add(entity);
    }

    /**
     * Notification that the player has died. 
     */
    public void notifyDeath() {
        message = "Oh no! They got you, try again?";
        waitingForKeyPress = true;
    }

    /**
     * Notification that the player has won since all the aliens are dead.
     */
    public void notifyWin() {
        message = "Well done! You Win!";
        waitingForKeyPress = true;
    }

    /**
     * Notification that an alien has been killed
     */
    public void notifyAlienKilled() {
        // reduce the alient count, if there are none left, the player has won!
        alienCount--;

        if (alienCount == 0) {
            notifyWin();
        }

        // if there are still some aliens left then they all need to get faster,
        // so speed up all the existing aliens
        for (int i = 0; i < entities.size(); i++) {
            final AbstractEntity entity = entities.get(i);
            if (entity instanceof AlienEntity) {
                // speed up by 2%
                entity.setHorizontalMovement(entity.getHorizontalMovement() * ALIEN_SPEEDUP_AFTER_DEATH);
            }
        }
    }

    /**
     * Attempt to fire a shot from the player. Its called "try"
     * since we must first check that the player can fire at this 
     * point, i.e. has he/she waited long enough between shots
     */
    public void tryToFire() {
        // check that we have waiting long enough to fire
        if (System.currentTimeMillis() - lastFire < FIRING_INTERVAL) {
            return;
        }

        // if we waited long enough, create the shot entity, and record the time.
        lastFire = System.currentTimeMillis();
        entities.add(new ShotEntity(this, "sprites/shot.gif", ship.getX() + 10, ship.getY() - 30));
    }

    /**
     * The main game loop. This loop is running during all game
     * play as is responsible for the following activities:
     * <p>
     * - Working out the speed of the game loop to update moves<p>
     * - Moving the game entities<p>
     * - Drawing the screen contents (entities, text)<p>
     * - Updating game events<p>
     * - Checking Input
     */
    public void gameLoop() {
        long lastLoopTime = System.currentTimeMillis();
        // long lastLoopTime = SystemTimer.getTime();

        // keep looping round til the game ends
        while (gameRunning) {
            // work out how long its been since the last update
            // this will be used to calculate how far the entities should move this loop
            final long now = System.currentTimeMillis();
            // final long now = SystemTimer.getTime();
            final long delta = now - lastLoopTime;
            lastLoopTime = now;

            // update the frame counter
            lastFpsTime += delta;
            fps++;

            // update our FPS counter if a second has passed since
            // we last recorded
            if (lastFpsTime >= 1000) {
                container.setTitle(windowTitle + " (FPS: " + fps + ")");
                lastFpsTime = 0;
                fps = 0;
            }

            // Get hold of a graphics context for the accelerated surface and blank it out
            final Graphics g = strategy.getDrawGraphics();
            g.setColor(Color.black);
            g.fillRect(0, 0, 800, 600);

            // cycle round asking each entity to move itself
            if (!waitingForKeyPress) {
                for (int i = 0; i < entities.size(); i++) {
                    entities.get(i).move(delta);
                }
            }

            // cycle round drawing all the entities we have in the game
            for (int i = 0; i < entities.size(); i++) {
                entities.get(i).draw(g);
            }

            // brute force collisions, compare every entity against
            // every other entity. If any of them collide notify 
            // both entities that the collision has occured
            for (int p = 0; p < entities.size(); p++) {
                for (int s = p + 1; s < entities.size(); s++) {
                    final AbstractEntity me = entities.get(p);
                    final AbstractEntity him = entities.get(s);

                    if (me.collidesWith(him)) {
                        me.collidedWith(him);
                        him.collidedWith(me);
                    }
                }
            }

            // remove any entity that has been marked for clear up
            entities.removeAll(removeList);
            removeList.clear();

            // if a game event has indicated that game logic should be resolved,
            // cycle round every entity requesting that their personal logic should be considered.
            if (logicRequiredThisLoop) {
                for (int i = 0; i < entities.size(); i++) {
                    entities.get(i).doLogic();
                }
                logicRequiredThisLoop = false;
            }

            // if we're waiting for an "any key" press then draw the current message 
            if (waitingForKeyPress) {
                g.setColor(Color.white);
                g.drawString(message, (800 - g.getFontMetrics().stringWidth(message)) / 2, 250);
                g.drawString("Press any key", (800 - g.getFontMetrics().stringWidth("Press any key")) / 2, 300);
            }

            // finally, we've completed drawing so clear up the graphics and flip the buffer over
            g.dispose();
            strategy.show();

            // resolve the movement of the ship. First assume the ship isn't moving.
            // If either cursor key is pressed then update the movement appropriately
            ship.setHorizontalMovement(0);

            if (leftPressed && !rightPressed) {
                ship.setHorizontalMovement(-MOVE_SPEED);
            } else if (rightPressed && !leftPressed) {
                ship.setHorizontalMovement(MOVE_SPEED);
            }

            // if we're pressing fire, attempt to fire
            if (firePressed) {
                tryToFire();
            }

            // finally pause for a bit. Note: this should run us at about
            // 100 fps but on windows this might vary each loop due to
            // a bad implementation of timer
            try {
                Thread.sleep(10);
            } catch (final Exception e) {
            }
            //            // we want each frame to take 10 milliseconds,
            //            // to do this we've recorded when we started the frame.
            //            // We add 10 milliseconds to this and then factor in the current time
            //            // to give us our final value to wait for
            //            SystemTimer.sleep(lastLoopTime + 10 - now);
        }
    }

    /**
     * A class to handle keyboard input from the user.
     * The class handles both dynamic input during game play, i.e. left/right and shoot,
     * and more static type input (i.e. press any key to continue)
     * 
     * This has been implemented as an inner class more through habbit then anything else.
     * Its perfectly normal to implement this as seperate class if slight less convienient.
     * 
     * @author Kevin Glass
     */
    private class KeyInputHandler extends KeyAdapter {

        /** The number of key presses we've had while waiting for an "any key" press */
        private int pressCount = 1;

        /**
         * Notification from AWT that a key has been pressed.
         * Note that a key being pressed is equal to being pushed down but *NOT* released.
         * Thats where keyTyped() comes in.
         *
         * @param keyEvent The details of the key that was pressed 
         */
        @Override
        public void keyPressed(final KeyEvent keyEvent) {
            // if we're waiting for an "any key" typed
            // then we don't want to do anything with just a "press"
            if (waitingForKeyPress) {
                return;
            }

            final int keyCode = keyEvent.getKeyCode();
            if (keyCode == KeyEvent.VK_LEFT) {
                leftPressed = true;
            }
            if (keyCode == KeyEvent.VK_RIGHT) {
                rightPressed = true;
            }
            if (keyCode == KeyEvent.VK_SPACE) {
                firePressed = true;
            }
        }

        /**
         * Notification from AWT that a key has been released.
         *
         * @param keyEvent The details of the key that was released 
         */
        @Override
        public void keyReleased(final KeyEvent keyEvent) {
            // if we're waiting for an "any key" typed then we don't 
            // want to do anything with just a "released"
            if (waitingForKeyPress) {
                return;
            }

            final int keyCode = keyEvent.getKeyCode();
            if (keyCode == KeyEvent.VK_LEFT) {
                leftPressed = false;
            }
            if (keyCode == KeyEvent.VK_RIGHT) {
                rightPressed = false;
            }
            if (keyCode == KeyEvent.VK_SPACE) {
                firePressed = false;
            }
        }

        /**
         * Notification from AWT that a key has been typed.
         * Note that typing a key means to both press and then release it.
         *
         * @param keyEvent The details of the key that was typed. 
         */
        @Override
        public void keyTyped(final KeyEvent keyEvent) {
            // if we're waiting for a "any key" type then check if we've recieved any recently.
            // We may have had a keyTyped() event from the user releasing the shoot or move keys,
            // hence the use of the "pressCount" counter.
            if (waitingForKeyPress) {
                if (pressCount == 1) {
                    // since we've now recieved our key typed event
                    // we can mark it as such and start our new game
                    waitingForKeyPress = false;
                    startGame();
                    pressCount = 0;
                } else {
                    pressCount++;
                }
            }

            if (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        }
    }

    /**
     * The entry point into the game.
     * We'll simply create aninstance of class which will start the display and game loop.
     * 
     * @param args The arguments that are passed into our game
     */
    public static void main(final String[] args) {
        // Start the main game loop
        // Note: this method will not return until the game has finished running.
        // Hence we are using the actual main thread to run the game.
        new Game().gameLoop();
    }
}

