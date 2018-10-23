// https://searchcode.com/api/result/56492165/

// Law, a multi-player strategy game written as a Java applet based 
// on the classic ZX Spectrum game Chaos by Julian Gallop.
// Copyright (C) 1997-2002 Jim Purbrick
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package law;

// Import java class libraries.
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.net.*;

// Import law AI package.
import law.ai.*;

/**
 *
 *	Represents the game state: the game board, whose turn it is, what
 *	phase the game's in, the alignment of the world etc. Handles GUI
 *	events by sending them to the NetGame, then responds to messages
 *	received by the NetGame through the NetGameListener interface.
 * @author James Purbrick (jcp@cs.nott.ac.uk)
 * @version 6/11/97
 */

public class Chaos extends Panel implements Data, NetGameListener, DialogListener
{
		// Define character constants to represent network messages and flags. 
		// This cuts down bandwidth requirements.
		private static final char CAST   				= 'c';
		private static final char SELECT 				= 's';
		private static final char MOVE   				= 'm';
		private static final char SHOOT  				= 'h';
		private static final char SELECT_SPELL 	= 'p';
		private static final char NO_FLAGS 			= '0';
		public static final char SELECT_MOUNT 	= 'n';
		public static final char SELECT_RIDER 	= 'r';
	
    // Define the member variables. 
    private static Image combinedWizardImages;
    private static Image combinedCreatureImages[];
    private static Image combinedAutoCreatureImages[];
    private static Image wizardPics[];
    private static Image creaturePics[][];
    private static Image autoCreaturePics[][];
    private InfoScreen infoScreen;
    private BattleScreen battleScreen;
    private SelectSpellScreen spellScreen;
    private Wizard currentWizard;    
    private Vector wizards = new Vector(); 
    private static final int CASTING = 2;
    private static final int MOVEMENT = 3;
    private int phase;
    private Creature selected; 		// The creature currently moving.
    private NetGame network;			// The interface to the network.
    private boolean selectionType;// True if selection is for movement.
    private ResourceLoader loader;// Packages up the differences between applets and applications.
    private GameBoard gameBoard;	// Representation of the board.
    private Dice dice;						// Provides random number services that can be synced for network play.
    private LawFrame frame;				// The frame containing the alignOMeter, logo and messageArea.

    // Constructor.
		
    public Chaos(NetGame n, LawFrame f)
    {
    		frame = f;
    		loader = f.getLoader();
    		network = n;
    		int i;
    		
				// Get handles on combined images.
				combinedWizardImages = loader.getImage("wizard.gif");
				combinedCreatureImages = new Image[NOCREATURESPELLS];
				for(i = 0; i < NOCREATURESPELLS; i++) 
				{
					combinedCreatureImages[i] = loader.getImage("creature" + i + ".gif");
				}
				combinedAutoCreatureImages = new Image[NOAUTOCREATURESPELLS];
				for(i = 0; i < NOAUTOCREATURESPELLS; i++) 
				{
					combinedAutoCreatureImages[i] = loader.getImage("autoCreature" + i + ".gif");
				}
				
				// Set up arrays for cached images.
				creaturePics = new Image[NOCREATURESPELLS][];
	  		autoCreaturePics = new Image[NOAUTOCREATURESPELLS][];
        
        // Set up the game board and dice.
        gameBoard = new GameBoard();
        dice = new Dice();
				
				// Set up the GUI.
				frame.add("Center", this);
				frame.pack();
        setLayout(new CardLayout());
        spellScreen = new SelectSpellScreen(this);
        add("spell screen", spellScreen);
        infoScreen = new InfoScreen();
        add("info screen", infoScreen);
        battleScreen = new BattleScreen(this,gameBoard);
				
				// Show the first screen and start up the animation.
        phase = 0;
        showScreen("player entry screen");
        validate();
        battleScreen.field.start();
        frame.addThread(battleScreen.field);
    }
    
    //-------------------------------------------------------------
    //	Image Loading methods.
    //-------------------------------------------------------------
    
    /** 
     * Loads an image made up from equal sized images in a horizontal strip,
     * then splits it up and returns the constituent images in an array.
     */
    private Image[] splitImage(Image combined, int width)
    {
    	int previousCursor = frame.getCursorType();
    	frame.setCursor(Frame.WAIT_CURSOR);
    	
    	// Load combined image.
  		MediaTracker tracker = new MediaTracker(this);
  		tracker.addImage(combined,0);
      try {tracker.waitForAll();}      
      catch (InterruptedException e){;}
      finally { if(tracker.isErrorAny()) System.err.println("Error loading image"); }
      
      // Calculate number of images present.
      int nImages = combined.getWidth(this) / width;
      int height = combined.getHeight(this);
      Image[] splitImages = new Image[nImages];
        	
      // Cut them up.
      ImageFilter cropper;
      ImageProducer combinedSource = combined.getSource();
      for(int i = 0; i < nImages; i++)
      {
      	cropper = new CropImageFilter(i * width,0,width,height);
      	splitImages[i] = createImage(new FilteredImageSource(combinedSource,cropper));
      	tracker.addImage(splitImages[i],i);
      }
      
      // Wait for images to be created.
      try {tracker.waitForAll();}      
      catch (InterruptedException e){;}
      finally { if(tracker.isErrorAny()) System.err.println("Error loading image"); }

     	frame.setCursor(previousCursor); 
      return splitImages;
    }
      	
    	
   	public Image getWizardImage(int imageNumber)
  	{
  			if(wizardPics == null)
  			{
  				wizardPics = splitImage(combinedWizardImages,SQUAREWIDTH);
        }
        return wizardPics[imageNumber];
    }

    public Image[] getCreatureImages(int i)
    {
        if(creaturePics[i] == null)
        {
        	creaturePics[i] = splitImage(combinedCreatureImages[i],SQUAREWIDTH);
        }
        return creaturePics[i];
    }

    /**
     * Returns the set of images that are used in a creatures animation
     * cut out from the combined images that are loaded to reduce download
     * size.
     */
    public Image[] getAutoCreatureImages(int i)
    {
        if(autoCreaturePics[i] == null)
        {
        	autoCreaturePics[i] = splitImage(combinedAutoCreatureImages[i],SQUAREWIDTH);
        }
        return autoCreaturePics[i];
    }
    
    /**
     * Show one of the main screens that form the game - the "battle screen"
     * etc. Uses a card layout to switch.
     */
    private void showScreen(String string)
    {
    		// if(string.equals("battle screen")) battleScreen.field.start();
    		// else if(battleScreen.field.isVisible()) battleScreen.field.stop();
        ((CardLayout) getLayout()).show(this, string);        
    }
    
	//------------------------------------------------
   	//	Event handlers.
   	//------------------------------------------------
  	
   	/** 
	  * Event handler for all the buttons.
	  */
    public boolean action(Event event, Object object)
    {        
        if (event.target == battleScreen.done)
        {      
        		frame.setCursor(Frame.DEFAULT_CURSOR);
        		network.sendNextTurn();
            return true;
        }
        if (event.target == battleScreen.cancel)
        {
            switch (phase)
            {
            case MOVEMENT: // End the movement of the selected creature.
                if(selected != null)
                {
                		// Check to see if we can select creature for ranged combat.
                		if(selectionType && selected.selectForRanged(currentWizard))
                		{
                			selectionType = false;
                			frame.setCursor(Frame.CROSSHAIR_CURSOR);
                		}
                		else
                		{
                			// Otherwise clear the selection.
                			frame.setCursor(Frame.DEFAULT_CURSOR);
                			selected.setSelected(false);
                    	selected.setMoved(true);
                    	selected = null;
                    }
                }
                break;
            case CASTING: // Cancel casting.          
                network.sendNextTurn();
                break;
            }
            return true;
        }
        return false;
    }

    /**
     * Event handler for mouse events happening on the battlefield.
     */
    public boolean mouseDown(Event event, int i1, int j)
    {
        if (event.target != battleScreen.field) return false;
     		int x = i1 - battleScreen.field.getImageOffset().x - battleScreen.field.location().x - battleScreen.location().x;
        int y = j - battleScreen.field.getImageOffset().y - battleScreen.field.location().y - battleScreen.location().y;
        Square pos = gameBoard.getSquareFromPixel(x,y);
        Creature creature = pos.getOccupant();
        
        // Right mouse only shows information.
        if (event.shiftDown() || event.metaDown())
        {
        		if(creature != null)
        		{
            	infoScreen.selected = creature;
            	showScreen("info screen");
            	return true;
            }
        }
        
        switch (phase)
        {
        case CASTING:
        		Spell s = currentWizard.getSelectedSpell();
        		
        		// Only send cast commands over the network if targets are valid.
            if(s.validTarget(pos))
            {   
            	sendCast(currentWizard.getSelectedSpellIndex(),pos, s.getParameters());            
            	         	
            	// If the spell doesn't need any more targets, move to next turn.
							if(s.getTargetsNeeded() == 0) network.sendNextTurn();	           	
						}
            break;
        case MOVEMENT:
        		// If there is currently not a selected creature, try to select 
        		// whatever was clicked on.
            if ( selected == null)
            {
            		// Try to select creature clicked on.
                if (creature != null && creature.selectForMove(currentWizard)) 
                {        
                	sendSelect(pos);
                }
                // If null was returned, signal that we can't select the creature.
                else if (creature != null) 
                {
                	frame.getMessageArea().setText("Cannot move " + creature.name);
                }
            }
            else // There is a selected creature.
            {	
            	if(selectionType) // Selection is for movement.
            	{       
            		sendMove(pos);
            		
            		// See if creature can remain selected for movement.
            		if(selected.selectForMove(currentWizard));
					     	// See if creature can be selected for shooting.
					     	else if(selected.selectForRanged(currentWizard))
					     	{
					     		selectionType = false;
					     		frame.setCursor(Frame.CROSSHAIR_CURSOR);
					     	}
					     	// Otherwise clear the selection.
								else setSelected(null);
            	}
            	else // Selection is for shooting.
            	{            	
            		sendShoot(pos);
            		
            		// See if creature can remain selected for shooting.
            		if(selected.selectForRanged(currentWizard));
            		// Otherwise clear the selection and reset cursor.
            		else 
            		{
            			frame.setCursor(Frame.DEFAULT_CURSOR);
            			setSelected(null);
            		}
            	}
            }
            break;
        }
        return true;
    }
    
    /**
     * Callback for "play again" dialog.
     */
    public void handleDialog(String choice)
    {
    	// Need to explicitly stop the battleField thread, as it
    	// may be a new one not in the frame's vector.
    	//battleScreen.field.stop();
    	
    	// Thread t = network.getThread();
    	// if(t != null && t.isAlive()) t.stop();
    	
    	if(choice.equals("Yes"))
    	{	
    		frame.stopThreads();
    		frame.remove(this);
    		frame.add("Center", frame.getLoader().getMenu());    		
    		frame.show();
    	}
    	// Frame exiting will stop all threads, so we don't need to stop
    	// battlefield in this case.
    	else frame.getLoader().exit();
    }
	
    /**
     * Event handler for all mouse up events. We're only concerned
     * with them if the infoscreen is currently showing.
     */
    public boolean mouseUp(Event event, int i, int j)
    {
        if (event.target != infoScreen) return false;
        showScreen("battle screen");
 				return true;
    }
    
    /**
      * Event handler for keyDown events.
      */
    public boolean keyDown(Event event, int i)
    {
    		Creature currentCreature;
    		
				// If a number is pressed, select all the creatures belonging
				// to the wizard of that number.
        i -= '1';

				// If number is out of range of number of wizards.
        if (i < 0 || i >= wizards.size()) return false;

        for(Enumeration e = gameBoard.getCreatures((Wizard)wizards.elementAt(i)); e.hasMoreElements(); )
        {
        	currentCreature = (Creature) e.nextElement();
        	currentCreature.setSelected(true);
        	// currentCreature.paint(BattleFieldAnimator.getBufferGraphics());
        }
        
        return true;
    }

      /**
      * Event handler for keyUp events.
      */	
    public boolean keyUp(Event event, int i)
		{
    		Creature currentCreature;
    		
				// If a number is pressed, select all the creatures belonging
				// to the wizard of that number.
        i -= '1';

				// If number is out of range of number of wizards.
        if (i < 0 || i >= wizards.size()) return false;

        for(Enumeration e = gameBoard.getCreatures((Wizard)wizards.elementAt(i)); e.hasMoreElements(); )
        {
        	currentCreature = (Creature) e.nextElement();
        	
        	// Need to keep the selected creature selected.
        	if(currentCreature != selected)
        	{
        		currentCreature.setSelected(false);
        		// currentCreature.paint(BattleFieldAnimator.getBufferGraphics());
        	}
        }
        
        return true;
    }

    /**
      * Works out what should happen when a wizard finishes moving, or
      * casting etc.
      */
    private void nextPhase()
    {
        switch(wizards.size())
        {
        	case 1:
            frame.getMessageArea().setText(((Wizard)wizards.firstElement()).name + " wins!");
	    network.sendEndGame();
          case 0:
            YesNoDialog d = new YesNoDialog("Play again?",this, frame);
            return;
        }
        
        // Clear old selections.
        setSelected(null);
        currentWizard.removeSelectedSpell();
        
        int i = wizards.indexOf(currentWizard);
        if (++i >= wizards.size())
        {
            i = 0; // Loop round to first player.
            currentWizard = (Wizard) wizards.firstElement();
            switch (phase) // Perform once-per-phase actions.
            {
            case CASTING:
                phase = MOVEMENT;
                updateAutoCreatures();
                resetAllMoves();
                break;

            case MOVEMENT:   
                phase = CASTING;
                frame.getAlignOMeter().commitAlignment();
                spellSelect();
                return;
            }
        }
        else
        {
        	currentWizard = (Wizard) wizards.elementAt(i);        
        }
		    switch (phase)
		    {
		    case CASTING:
		    		if(currentWizard.getLocal()) 
		    		{
				  // Allow casting once spell selection
				  // has ended.
				  if(battleScreen.isVisible())
				  {
				    if(currentWizard instanceof AIWizard)
				    {
				      // Don't allow users to interact with
				      // GUI.
				      enable(false);

				      // Print a status message.
				      frame.getMessageArea().setText(currentWizard.name + " casting");
				      // Get AI to cast.
				      ((AIWizard) currentWizard).doCasting();

				      // Move to next wizard.
				      network.sendNextTurn();
				    }
				    else // Non AI local.
				    {
				      enable(true);
				      if(currentWizard.getSelectedSpell() 
					 != null)
				      {
					
					frame.getMessageArea().setText(currentWizard.name + " casting " + currentWizard.getSelectedSpell().getName());
				      }
				      else network.sendNextTurn();
				    }
				  }
				}
				else // Network controlled.
				{
				  enable(false);
				  frame.getMessageArea().setText(currentWizard.name + " casting");
				}
		        return;		    
			      case MOVEMENT:
				if(currentWizard.getLocal())
				{
				  if(currentWizard instanceof AIWizard)
				  {
				    // Don't allow users to interact
				    // with GUI.
				    enable(false);

				    // Get AI to move.
				    ((AIWizard) currentWizard).doMovement();

				    // Move to next wizard.
				    network.sendNextTurn();
				  }
				  else enable(true);
				}
		    		else enable(false);
				frame.getMessageArea().setText(currentWizard.name + " movement");
		       	
				// Flash up player's creatures.
				keyDown(null,i + '1');
				Pause p = new Pause(6);
				battleScreen.field.queEffect(p);
				p.waitTillDead();
				keyUp(null,i + '1');  
				return;		
			      default:
				frame.getMessageArea().setText("");
				break;
		    }
    }    
    
    /**
      * Run the spell select phase.
      */
    private void spellSelect()
    {
    	// Group all local wizards together.
    	Vector localWizards = new Vector();
    	for(Enumeration e = wizards.elements(); e.hasMoreElements();)
    	{
    		Wizard currentWizard = (Wizard) e.nextElement();
    		if(currentWizard.getLocal() && 
		   ! (currentWizard instanceof AIWizard))
    		{
    			localWizards.addElement(currentWizard);
    		}
    	} 	

    	// If there are any local wizards, get them to 
    	// select their spells.
    	if(localWizards.size() > 0)
    	{
	  enable(true);
	  spellScreen.setWizards(localWizards.elements());
	  showScreen("spell screen");
    	}
	else endSpellSelect();
    }
    
    /**
     * End the spell select and start spell casting.
     */
    public void endSpellSelect()
    {
    	showScreen("battle screen");
    	if(currentWizard.getLocal())
  	{
	  if(currentWizard instanceof AIWizard)
	  {
	    enable(false);
	    ((AIWizard) currentWizard).doCasting();
	    network.sendNextTurn();
	  }
	  else
	  {
	    enable(true);
	    Spell s = currentWizard.getSelectedSpell();
	    if(s == null) network.sendNextTurn();
	    else frame.getMessageArea().setText(currentWizard.name + " casting " + s.getName());
	  }
      }
      else enable(false);
    }
  
    private void updateAutoCreatures()
    {
    	Creature currentCreature;
    	
        for(Enumeration e = gameBoard.getCreatures(); e.hasMoreElements(); )
        {
      		currentCreature = (Creature) e.nextElement();
      		if(currentCreature instanceof AutoCreature)
        		((AutoCreature)currentCreature).update();
        }
    }

    private void resetAllMoves()
    {
        for(Enumeration e = gameBoard.getCreatures(); e.hasMoreElements(); )
        {
        	((Creature) e.nextElement()).resetMove();
        }
    }

    public BattleFieldAnimator getBattleField()
    {
        return battleScreen.field;
    }
    
    public GameBoard getGameBoard()
    {
    		return gameBoard;
    }

    public void killWizard(Wizard wizard)
    {
        wizards.removeElement(wizard);
    }

		private void setSelected(Creature c)
		{
			if(selected != null) selected.setSelected(false);
			selected = c;
			selectionType = true;
			if(selected != null) selected.setSelected(true);
		}
		
		public void addWizard(Wizard w)
		{
			wizards.addElement(w);
			if(currentWizard == null) currentWizard = w;
		}
		
		public Dice getDice()
		{
			return dice;
		}
	
		/**
		  * Returns either the Java application or applet that the
		  * game is running in.
		  */
		public LawFrame getFrame()
		{
			return frame;
		}
   
	  
  	/**
	  * Overides enable to print diagnostic messages.
	  */
  	public void enable(boolean b)
  	{
  		if(b) System.out.println("Enabling GUI");
  		else System.out.println("Disabling GUI");
  		super.enable(b);
  	}
  	
  //-------------------------------------------------------------------------
  //	NetGameListener Methods.
  //-------------------------------------------------------------------------

  /**
    * Parses and acts on commands recieved from the (loopback) network to
    * update the game state.
    */
  public void handleCommand(String command)
	{
		Square pos;
		try
		{
			StringTokenizer s = new StringTokenizer(command.substring(1),":",false);
			switch(command.charAt(0))
			{
			case CAST:
				Spell spell;
				spell = currentWizard.setSelectedSpell(Integer.parseInt(s.nextToken()));
				pos = getSquare(s);
				int[] params = new int[s.countTokens()];
				for(int i = 0; i < params.length; i++)
				{
					params[i] = Integer.parseInt(s.nextToken());
				}
				spell.setParameters(params);
				spell.cast(pos);
				break;		
			case SELECT:
				pos = getSquare(s);
				switch(s.nextToken().charAt(0))
				{
					case SELECT_RIDER:
						setSelected(((Mount) pos.getOccupant()).getRider());
						break;
					case SELECT_MOUNT:
					case NO_FLAGS:
						setSelected(pos.getOccupant());
						break;
					default:
						System.out.println("Unknown flag in command: " + command);
				}
				break;
			case MOVE:
				selected.move(getSquare(s));
				break;
			case SHOOT:
				selected.rangedAttack(getSquare(s));
				break;
			default:
				System.out.println("Unknown command: " + command);
			} // End switch on prefix character.		
		}
		catch(NoSuchElementException e)
		{
			System.out.println("Wrong number of parameters in command: " + command);
		}
		catch(NumberFormatException e2)
		{
			System.out.println("Numeric parameter expected in command: " + command);
		}				
	}
	
	/**
	  * Calls nextPhase
	  */
	public void nextTurn()
	{
		nextPhase();
	}
	
	/** 
	  * Encodes a request to cast a spell as a network message and
	  * sends it on the (loopback) network.
	  */
	public void sendCast(int index, Square target, int[] params)
	{
		String command = String.valueOf(CAST) + index + ':' + encodeSquare(target);
		for(int i = 0; i < params.length; i++)
		{
			command += ':';
			command += params[i];
		}
		network.sendCommand(command);
	}
	
	/** 
	  * Encodes a request to select a creature as a network message and
	  * sends it on the (loopback) network.
	  */
	public void sendSelect(Square target)
	{
		network.sendCommand( String.valueOf(SELECT) + encodeSquare(target) + ':' + NO_FLAGS);
	}

		/** 
	  * Encodes a request to select a creature as a network message and
	  * sends it on the (loopback) network.
	  */
	public void sendSelect(Square target, char flag)
	{
		network.sendCommand( String.valueOf(SELECT) + encodeSquare(target) + ':' + flag);
	}
	
		/** 
	  * Encodes a request to move the selected creature as a network 
	  * message and
	  * sends it on the (loopback) network.
	  */
	public void sendMove(Square target)
	{
		network.sendCommand( String.valueOf(MOVE) + encodeSquare(target));
	}
	
		/** 
	  * Encodes a request for the selected creature to shoot as a network 
	  * message and
	  * sends it on the (loopback) network.
	  */
	public void sendShoot(Square target)
	{
		network.sendCommand( String.valueOf(SHOOT) + encodeSquare(target));
	}

		/* Not needed
	  * Encodes a request to start a new turn as a network 
	  * message and
	  * sends it on the (loopback) network.
	  
	public void sendNextTurn()
	{
	  network.sendNextTurn();
	}
		*/
	
	/**
	  * Initialises the random generator and world alignment and starts
	  * the first turn.
	  *
	  * @param randomSeed the seed fed to the random generator. By feeding
	  *                   the same seed to all games in a network game,
	  *                   they stay in sync.
	  */
	public void startGame(int randomSeed) 
	{
		dice.setSeed(randomSeed);
		frame.getAlignOMeter().resetAlignment();
		phase = MOVEMENT;
    currentWizard = (Wizard)wizards.lastElement();
    nextPhase();	
	}
	
	/**
	  * Adds a player to the game.
	  *
	  * @param i the player number (0-7)
	  * @param name the player name
	  * @local true if the player will use this machine to control the
	  *        wizard.
	  */
	public void addPlayer(int i, String name, boolean local) {}
	
		/**
	  * Removes a player from the game.
	  *
	  * @param i the player number (0-7)
	  */
	public void removePlayer(int i)
	{
		for(Enumeration e = wizards.elements(); e.hasMoreElements();)
		{
			Wizard w = (Wizard) e.nextElement();
			if(w.getPlayerNumber() == i)
			{
				w.die();
				if(w == currentWizard) nextPhase();
				break;
			}
		}
	}
		
	/**
	  * Encodes co-ordinates of a square into a string.
	  */
	private String encodeSquare(Square s)
	{
		return s.getx() + ":" + s.gety();
	}
	
	/**
	  * Reads co-ordinates from a tokenized string and returns
	  * the corresponding square.
	  */
	private Square getSquare(StringTokenizer s) throws NumberFormatException, NoSuchElementException
	{
		int x = 0, y = 0;
		x = Integer.parseInt(s.nextToken());
		y = Integer.parseInt(s.nextToken());		
		return gameBoard.getSquare(x,y);
	}	
	
	/**
	  * Return the preferred size of the game, which is based on the
	  * size of the game screen.
	  */
	public Dimension preferredSize()
  {
      return minimumSize();
  }
    
	/**
	  * Return the minimum size of the game, which is based on the
	  * size of the game screen.
	  */	
  public Dimension minimumSize()
  {
  	return screenSize;        
  }
  
}
