// https://searchcode.com/api/result/119885848/

package net;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.swing.JFrame;

import util.Array;


import game.Game;
import game.Strategy;
import human.HumanDisplay;
import human.HumanModel;
import human.HumanModelData;
import human.HumanView;

/**
 * Serves as a human strategy to play against computer strategies.
 * The Model-View-Controller pattern is approximated, with this class
 * being the controller, HumanDisplay being the view, and HumanModel being
 * the model.
 * 
 * The fundamental design problem right now is that the HumanStrategy and
 * the HumanRemoteClient are actually sharing the "controller" role in this 
 * pattern. In particular, "run()" calls "runGame()" which calls "getAction()".
 * 
 * An alternative would be to reduce HumanStrategy to being a MouseListener (with the necessary
 * read/write locks),
 * and moving getAction() and all of the frame constructs into HumanRemoteClient.
 * @author maz
 *
 */
public class HumanStrategy implements MouseListener,Strategy{
	private HumanView display;
	private HumanModelData modelData;
	private HumanModel model;
	private Game g;
	
	private JFrame frame;
	private int actionChosen;
	private long timeOfObservation;
	private int localTimeout;
	
	InputStreamListener listener;
	private int actualPlayerIndex;
	
	private boolean firstTurn;
	
	public synchronized void setInputStreamListener(InputStreamListener listener){
		this.listener = listener;
	}
	
	private synchronized InputStreamListener getInputStreamListener(){
		return listener;
	}
	
	public Component getComponent(){
		return frame;
	}
	
	
	public HumanStrategy(Game g, long seed, String features){
		this(g,new HumanDisplay(g));
		HumanDisplay hdisplay = (HumanDisplay)display;
		hdisplay.addMouseListener(this);
		frame = new JFrame("Lemonade Stand Game");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(hdisplay,BorderLayout.CENTER);
		frame.pack();
		frame.validate();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	
	/**
	 * A direct construction of HumanStrategy.
	 * Afterwards, HumanView must (in some way) add HumanStrategy
	 * as a MouseListener.
	 * @param g
	 * @param display
	 */
	public HumanStrategy(Game g,HumanView display){
		this.g = g;
		actionChosen = -1;
		modelData = new HumanModelData(g.getNumPlayers());
		modelData.setBeginGame();
		model = new HumanModel(g.getNumPlayers());
		this.display = display;
		model.addListener(display);
		display.handleChange(model);
		localTimeout=55000;
				
	}
	
	public void mouseClicked(MouseEvent e) {
		int location = display.getPosition(e.getPoint());
		if (location!=-1){
			clickOnLocation(location);
		}
	}
	
	/**
	 * Public permission is granted to access this function
	 * because it allows one to simulate the clicks.
	 * @param location the "position" (i.e. action) of the click
	 */
	public void clickOnLocation(int location){
		if (chooseAction(location)){
			try{
				model.lockAndGetModel(modelData);
				modelData.setPiece(actualPlayerIndex, location);
				modelData.utilitiesVisible=false;
				modelData.message = "Action locked in...";
				model.setModelAndUnlock(modelData);
			} catch (InterruptedException ie){
				
			}
		}
	}
	
	
	private synchronized boolean chooseAction(int location) {
		if (actionChosen==-1){
			actionChosen = location;
			return true;
		}
		return false;
	}
	
	public synchronized void unlockAction() {
		timeOfObservation = System.currentTimeMillis();
		actionChosen = -1;
	}
	
	public synchronized void lockAction() {
		actionChosen = -2;
	}
	
	
	
	private synchronized int queryAction() {
		int result = -1;
		if (actionChosen>-1){
			result=actionChosen;
			actionChosen = -2;
		}
		return result;
	}
	
	
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
	
	public int getAction() {
		try{
			while(true){
				int possible=queryAction();
				if (possible!=-1){
					return possible;
				}
				
				long timeAfterObservation = System.currentTimeMillis()-timeOfObservation;
				if (timeAfterObservation>5000 && modelData.utilitiesVisible){
					model.lockAndGetModel(modelData);
					//modelData.utilitiesVisible=false;
					for(int i=0;i<modelData.positions.length;i++){
						if (i!=actualPlayerIndex){
							modelData.setFaded(i, true);
						}
					}
					modelData.message = "Please choose an action...";
					model.setModelAndUnlock(modelData);
				} 
				
				if (timeAfterObservation>localTimeout){
					model.lockAndGetModel(modelData);
					//modelData.utilitiesVisible=false;
					for(int i=0;i<modelData.positions.length;i++){
						if (i!=actualPlayerIndex){
							modelData.setFaded(i, true);
						}
					}
					modelData.message = "You timed out...";
					model.setModelAndUnlock(modelData);
					return modelData.positions[actualPlayerIndex];
					
				} else if (timeAfterObservation>localTimeout-50000){
					int secondsLeft = (int)(localTimeout-timeAfterObservation)/1000;
					String timeRemainingString = "Please choose an action.\n"+secondsLeft+" seconds left...";
					if (modelData.message==null || !modelData.message.equals(timeRemainingString)){
						model.lockAndGetModel(modelData);
						modelData.message = timeRemainingString;
						model.setModelAndUnlock(modelData);
					}
				}
				InputStreamListener currentListener = getInputStreamListener();
				
				if (currentListener!=null){
					String message = currentListener.peekAtMessage();
					if (message!=null && message.startsWith("ABORT")){
						return g.getNumActions();
					}
				}
				Thread.sleep(100);
			}
		} catch (InterruptedException ie){
			return g.getNumActions();
		} catch (IOException io){
			return g.getNumActions();
		}
	}
	
	public void observeOutcome(int[] actions) {
		
		assert(g.areLegalActions(actions));
		try{
			Array.swap(actions,0,actualPlayerIndex);
			model.lockAndGetModel(modelData);
			
			for(int i=0;i<actions.length;i++){
				modelData.setPiece(i,actions[i]);
			}
			//modelData.setFadedAll(false);
			modelData.utilitiesVisible=true;
			int[] utilityChange = g.getUtility().getUtility(actions);
			int pointsWon = utilityChange[actualPlayerIndex];
			
			modelData.message = "Points won:"+pointsWon;
			assert(modelData.scores.length==utilityChange.length);
			
			for(int i=0;i<actions.length;i++){
				modelData.scores[i]+=utilityChange[i];
			}
			model.setModelAndUnlock(modelData);
		} catch (InterruptedException ie){
			ie.printStackTrace();
		}
		timeOfObservation = System.currentTimeMillis();
		unlockAction();
	}
	
	public HumanModel getModel() {
		return model;
	}
	
	public void dispose() {
		frame.dispose();
	}
	
	public void setActualPlayerIndex(int actualPlayerIndex) {
		this.actualPlayerIndex = actualPlayerIndex;
	}
	
}

