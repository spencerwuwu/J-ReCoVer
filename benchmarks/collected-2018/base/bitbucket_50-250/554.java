// https://searchcode.com/api/result/61507228/

package tilegame;

/**************************************
 * GFXMan.java
 * Purpose:
 * A thread dedicated to updating the screen.
 * Calls EntityMan every frame for a list of entities.
 * Calls LevelMan for a scene.
 */

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.lang.Thread;
import java.awt.Rectangle;
import java.util.Vector;
import java.util.HashMap;

/**
 *
 * @author jo
 */
public class GFXMan extends JFrame implements Runnable {

	// For functionality and linking
	private Main engine;
	public boolean done = false;

	// For drawing to the screen.
	private DisplayMode displayMode = null;
	private GraphicsDevice device = null;
	private long lastTime = 0;
	private int frameCount = 0;

	// For use by other functions
	public Rectangle camera = null;
	//private HashMap <String,BufferedImage> spriteSheets; // Made obsolete by entities copying one another.

	public GFXMan( Main engine ) {
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		device = graphicsEnvironment.getDefaultScreenDevice();
		displayMode = device.getDisplayMode();
		this.engine = engine;
		camera = new Rectangle( 0, 0, device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight() );
	}

	public String getDisplayModeList() {
		String s = "";
		DisplayMode[] modes = device.getDisplayModes();
		for( int i=0; i < modes.length; ++i ) {
			DisplayMode j = modes[i];
			s += i +" - "+ j.getWidth() +"x"+ j.getHeight() +"x"+ j.getBitDepth() +"@"+ j.getRefreshRate();
		}
		return s;
	}

	public Window getFullscreenWindow() {
		return device.getFullScreenWindow();
	}

	@Override
	public Graphics2D getGraphics() {
		Window window = device.getFullScreenWindow();
		BufferStrategy strategy = window.getBufferStrategy();
		return (Graphics2D)strategy.getDrawGraphics();
	}

	public BufferedImage getHardwareMemoryChunk( int w, int h ) {
		return device.getFullScreenWindow().getGraphicsConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT );
	}

	public Rectangle getCamera() {
		return new Rectangle( camera );
	}

	public void moveCamera( int dx, int dy ) {
		camera.setLocation( camera.x+dx, camera.y+dy );
	}

	public void focusCamera( int x, int y ) {
		camera.setLocation( x, y );
	}

	public void setDisplayMode( int index ) {
		displayMode = device.getDisplayModes()[index];
		camera.setSize( displayMode.getWidth(), displayMode.getHeight() );
	}

	public void setFullscreenMode() {
		this.setUndecorated( true );
		this.setResizable( false );
		this.setBackground( Color.BLACK );
		this.setForeground( Color.WHITE );
		this.setFont( new Font("Arial", Font.PLAIN, 12) );
		this.setIgnoreRepaint( true );
		device.setFullScreenWindow( this );

		try {
			if( displayMode != null && device.isDisplayChangeSupported() ) {
				device.setDisplayMode( displayMode );
			}
		} catch( IllegalArgumentException iae ) {
			System.err.println( "ERROR Setting Fullscreen:" + iae );
			System.exit(-1);
		}

		this.createBufferStrategy( 2 );
	}

	public void unsetFullscreenMode() {
		Window w = device.getFullScreenWindow();
		w.dispose();
		device.setFullScreenWindow(null);
	}

	public void run() {
		Graphics2D g;
		while( !engine.done ) {
			g = (Graphics2D)device.getFullScreenWindow().getBufferStrategy().getDrawGraphics();
			try {
				draw(g);
			} finally {
				g.dispose();
			}

			if( !device.getFullScreenWindow().getBufferStrategy().contentsLost() ) {
				device.getFullScreenWindow().getBufferStrategy().show();
			}
			Thread.yield();
		}

		this.unsetFullscreenMode();
	}

	public void draw(Graphics2D g) {
		// TODO: Add check if(g instanceof Graphics2D)
		//g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );

		Rectangle cam = new Rectangle( camera ); // We make a copy of the camera just in case it changes between calls.
		// We don't want our enemies and level to fall out of sync.
		// Also, I'm not using the getCamera() method to reduce overhead.

		// Clear the background
		g.setPaint( java.awt.Color.BLACK );
		g.fillRect( 0, 0, cam.width, cam.height );

		// Draw map
		engine.levelman.drawLayer( g, cam, LevelMan.BACKGROUND_LAYER );
		engine.levelman.drawLayer( g, cam, LevelMan.FOREGROUND_LAYER );

		// Draw entities
		engine.entityman.draw( g, cam );

		// Draw overlay
		engine.levelman.drawLayer( g, cam, LevelMan.OVERLAY_LAYER );

		// Draw GUI
	}

	public void update() {
		Window w = device.getFullScreenWindow();
		BufferStrategy s = w.getBufferStrategy();
		if( !s.contentsLost() ) {
			s.show();
		}
	}

	private BufferedImage createBuffer() {
		Window window = device.getFullScreenWindow();
		GraphicsConfiguration config = window.getGraphicsConfiguration();
		return config.createCompatibleImage( getWidth(), getHeight(), device.getDisplayMode().getBitDepth() );
	}
}

