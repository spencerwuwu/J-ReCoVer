// https://searchcode.com/api/result/2263538/

package com.group_finity.mascot;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;

/**
 * Program entry point.
 *
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Main {

	private static final Logger log = Logger.getLogger(Main.class.getName());

	// Action that matches the "Gather Around Mouse!" context menu command
	static final String BEHAVIOR_GATHER = "ChaseMouse";

	static {
		try {
			LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private final Manager manager = new Manager();

	private ArrayList<String> imageSets = new ArrayList<String>();	
	
	private Hashtable<String,Configuration> configurations = new Hashtable<String,Configuration>();	
	
	private static Main instance = new Main();

	public static Main getInstance() {
		return instance;
	}

	private static JFrame frame = new javax.swing.JFrame();
	
	public static void showError( String message ) {
        JOptionPane.showMessageDialog(frame, message,
                "Error",JOptionPane.ERROR_MESSAGE);		
	}	
	
	public static void main(final String[] args) {
		try {
			getInstance().run();
		} catch(OutOfMemoryError err) {
			log.log (Level.SEVERE, "Out of Memory Exception.  There are probably have too many " +
					"Shimeji mascots in the image folder for your computer to handle.  Select fewer" +
					" image sets or move some to the img/unused folder and try again.", err);
			Main.showError( "Out of Memory.  There are probably have too many \n" +
					"Shimeji mascots for your computer to handle.\n" +
					"Select fewer image sets or move some to the \n" +
					"img/unused folder and try again.");
			System.exit(0);
		}
	}

	public void run() {	
	
		// Get the image sets to use
		imageSets = new ImageSetChooser(frame,true).display();
		
		// Load settings
		for( String imageSet : imageSets ) {
			loadConfiguration(imageSet);
		}
		
		// Create the tray icon
		createTrayIcon();

		// Create the first mascot
		for( String imageSet : imageSets ) {
			createMascot(imageSet);
		}	
		
		getManager().start ();		
	}

	private void loadConfiguration( final String imageSet ) {

		try {
			String actionsFile = "./conf/actions.xml";		
			if( new File("./conf/"+imageSet+"/actions.xml").exists() ) {	
				actionsFile = "./conf/"+imageSet+"/actions.xml";				
			} else if( new File("./img/"+imageSet+"/conf/actions.xml").exists() ) {
				actionsFile = "./img/"+imageSet+"/conf/actions.xml";
			}				
			
			log.log(Level.INFO, imageSet+" Read Action File ({0})", actionsFile);
			
			final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					new FileInputStream( new File(actionsFile)));

			Configuration configuration = new Configuration();

			configuration.load(new Entry(actions.getDocumentElement()),imageSet);

			String behaviorsFile = "./conf/behaviors.xml";			
			if( new File("./conf/"+imageSet+"/behaviors.xml").exists() ) {	
				behaviorsFile = "./conf/"+imageSet+"/behaviors.xml";				
			} else if( new File("./img/"+imageSet+"/conf/behaviors.xml").exists() ) {
				behaviorsFile = "./img/"+imageSet+"/conf/behaviors.xml";
			}
			
			log.log(Level.INFO, imageSet+" Read Behavior File ({0})", behaviorsFile);			
			
			final Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					new FileInputStream( new File(behaviorsFile) ) );

			configuration.load(new Entry(behaviors.getDocumentElement()),imageSet);

			configuration.validate();
			
			configurations.put( imageSet, configuration );
			
		} catch (final IOException e) {
			log.log (Level.SEVERE, "Failed to load configuration files", e);
			Main.showError( "Failed to load configuration files.\nSee log for more details." );
			exit();
		} catch (final SAXException e) {
			log.log (Level.SEVERE, "Failed to load configuration files", e);
			Main.showError( "Failed to load configuration files.\nSee log for more details." );			
			exit();
		} catch (final ParserConfigurationException e) {
			log.log (Level.SEVERE, "Failed to load configuration files", e);
			Main.showError( "Failed to load configuration files.\nSee log for more details." );			
			exit();
		} catch (final ConfigurationException e) {
			log.log (Level.SEVERE, "Failed to load configuration files", e);
			Main.showError( "Failed to load configuration files.\nSee log for more details." );			
			exit();
		} catch (final Exception e) {
			log.log (Level.SEVERE, "Failed to load configuration files", e);
			Main.showError( "Failed to load configuration files.\nSee log for more details." );			
			exit();
		}		
	}

	/**
	* Create a tray icon.
	* @ Throws AWTException
	* @ Throws IOException
	*/
	private void createTrayIcon() {

		log.log (Level.INFO, "create a tray icon");

		// "Another One!" menu item
		final MenuItem increaseMenu = new MenuItem ("Another One!");
		increaseMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				createMascot();
			}
		});

		// "Follow One!" Menu item
		final MenuItem gatherMenu = new MenuItem ("Follow Mouse!");
		gatherMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				Main.this.getManager().setBehaviorAll(BEHAVIOR_GATHER);
			}
		});

		// "Reduce to One!" menu item
		final MenuItem oneMenu = new MenuItem("Reduce to One!");
		oneMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				Main.this.getManager().remainOne();
			}
		});

		// "Restore IE!" menu item
		final MenuItem restoreMenu = new MenuItem("Restore IE!");
		restoreMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				NativeFactory.getInstance().getEnvironment().restoreIE();
			}
		});

		// "Bye Everyone!" menu item
		final MenuItem closeMenu = new MenuItem("Bye Everyone!");
		closeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				exit();
			}
		});
		
		// Create the context "popup" menu.
		final PopupMenu trayPopup = new PopupMenu();

		trayPopup.add(increaseMenu);
		trayPopup.add(gatherMenu);
		trayPopup.add(oneMenu);
		trayPopup.add(restoreMenu);
		trayPopup.add(new MenuItem("-"));
		trayPopup.add(closeMenu);

		try {
			// Create the tray icon
			final TrayIcon icon = new TrayIcon(ImageIO.read(Main.class.getResource("/icon.png")), "shimeji-ee", trayPopup);
			icon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					// When the tray icon is left clicked:
					if (SwingUtilities.isLeftMouseButton(e)) {
						createMascot();
					}
				}
			});
			
			// Show tray icon
			SystemTray.getSystemTray().add(icon);

		} catch (final IOException e) {
			log.log(Level.SEVERE, "Failed to create tray icon", e);
			Main.showError( "Failed to display system tray.\nSee log for more details." );			
			exit();
		} catch (final AWTException e) {
			log.log(Level.SEVERE, "Failed to create tray icon", e);
			Main.showError( "Failed to display system tray.\nSee log for more details." );
			getManager().setExitOnLastRemoved(true);
		}

	}

	// Randomly creates a mascot
	public void createMascot() {
		int length = imageSets.size();
		int random = (int)(length * Math.random());
		createMascot( imageSets.get(random) );
	}
	
	/**
	 * Create a mascot
	 */
	public void createMascot( String imageSet ) {
		log.log(Level.INFO, "create a mascot");

		// Create one mascot
		final Mascot mascot = new Mascot( imageSet );

		// Create it outside the bounds of the screen
		mascot.setAnchor(new Point(-1000, -1000));
		
		// Randomize the initial orientation
		mascot.setLookRight(Math.random() < 0.5);

		try {
			mascot.setBehavior(getConfiguration(imageSet).buildBehavior(null, mascot));
			this.getManager().add(mascot);
		} catch (final BehaviorInstantiationException e) {
			log.log (Level.SEVERE, "Failed to initialize the first action", e);
			Main.showError( "Failed to initialize first action.\nSee log for more details." );						
			mascot.dispose();
		} catch (final CantBeAliveException e) {
			log.log (Level.SEVERE, "Fatal Error", e);
			Main.showError( "Failed to initialize first action.\nSee log for more details." );									
			mascot.dispose();
		} catch ( Exception e ) {
			log.log (Level.SEVERE, imageSet + " fatal error, can not be started.", e);
			Main.showError( "Could not create "+imageSet+".\nSee log for more details." );									
			mascot.dispose();
		}
	}

	public Configuration getConfiguration( String imageSet ) {
		return configurations.get(imageSet);
	}	
	
	private Manager getManager() {
		return this.manager;
	}

	public void exit() {
		this.getManager().disposeAll();
		this.getManager().stop();

		System.exit(0);
	}

}

