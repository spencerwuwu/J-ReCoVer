// https://searchcode.com/api/result/135427538/

package masteryone;

import java.util.ArrayDeque;
import battlecode.common.*;



/**
 * Messenger Class.  Loosely based on the old Lazer6 messaging code
 * 
 * 
 * <pre>
 * MESSAGE BLOCK FORMAT------------------------------------------|           
 *    idx       0           1            2            3          |
 *    ints [ hash       , header     , data      , data..........|
 *    locs [ source     , origin     , data      , data..........|
 *    strs [-----------------------------------------------------|
 * </pre>
 * @author Cory
 *
 */
public class Messenger {
	
	//public variable
	final RobotPlayer myPlayer;

	//send component needs to be enabled
	private boolean canSend;
	public boolean shouldReceive;
	
	//static limits
	private static final int ROUND_MOD = 4;
	private static final int ID_MOD = 1024;
	private final int teamKey;
	private final int myID;
	
	//Defined indexes for readability
	public static final int idxHash = 0;
	public static final int idxHeader = 1;
	public static final int idxSender = 0;
	public static final int idxOrigin = 1;
	public static final int firstData = 2;
	public static final int minSize = firstData;
	
	ArrayDeque<Message> messageQueue;
	
	private boolean[][] hasHeard = new boolean[ROUND_MOD][];
	
	

	
	public Messenger(RobotPlayer player) {
		
		myPlayer = player;							//Assign the player
		canSend = false;							//Default robot doesn't have antennae
		messageQueue = new ArrayDeque<Message>();	//Build Queue
		shouldReceive = true;
		
				
		//Initialize our entire 'has heard' table
		for(int i=0; i<ROUND_MOD; i++) {
			hasHeard[i] = new boolean[ID_MOD];
		}
		
		//set ID and key	
		myID = myPlayer.myRC.getRobot().getID();
		if(myPlayer.myRC.getTeam()==Team.A) {
			teamKey = 131071; //first 6 digit mersenne prime
		} else {
			teamKey  = 174763; //first 6 digit wagstaff prime
		}	
	}
		
	
	
	
	/**
	 * This call is run whenever the robot gains an antennae
	 * Note that components can never be removed, so a robot cannot lose it's sending ability.
	 */
	public void enableSender() {
		canSend = true;		
	}	
	

	/**
	 * Should the robot receive messages?
	 * Useful holding messages until robots are active.
	 * @param state whether you should 
	 */
	public void toggleReceive(boolean state) {
		shouldReceive = state;		
	}
	
	
	
	
	/**
	 * Internal sending function
	 * @param m message to send where the relevant location blocks and int blocks reserved for headers
	 * and such are left blank.  sendMsg computs the hashes and inserts them in.
	 */
	private void sendMsg(MsgType type, Message m) {
		
		//debug code to make sure we're not calling something that can't be done.
		assert canSend;
		
		//fill in message
		int currTime = Clock.getRoundNum();
		
		
		
		m.ints[idxHeader] = Encoder.encodeMsgHeader(type, currTime, myID);		
		
		MapLocation myLoc = myPlayer.myRC.getLocation();
		m.locations[idxSender] = myLoc;		//sender location
		m.locations[idxOrigin] = myLoc;		//origin location
		
		
		m.ints[idxHash] = teamKey; 			//super simple hash
		
		messageQueue.add(m);		
		
		//I've heard my own message
		hasHeard[currTime%ROUND_MOD][myID%ID_MOD] = true;		
	}
	
	
	
	/**
	 * This internal function builds a <code>battlecode.common.Message</code> with 
	 * <code>iSize</code> ints and <code>lSize</code> locations
	 * @param iSize number of ints
	 * @param lSize number of locations
	 * @return
	 */
	private Message buildNewMessage(int iSize, int lSize) {
		Message m = new Message();
		m.ints = new int[minSize+iSize];
		m.locations = new MapLocation[minSize+lSize];
		return m;
	}
	
	
	public void sendNotice(MsgType t) {
		sendMsg(t,buildNewMessage(0,0));
	}
	
	public void sendInt(MsgType t, int int1)
	{
		Message m = buildNewMessage(1,0);
		m.ints[firstData       ] = int1;
		
		sendMsg(t,m);
	}
	
	public void sendLoc(MsgType t, MapLocation loc1)
	{
		Message m = buildNewMessage(0,1);
		m.locations[firstData  ] = loc1;
		
		sendMsg(t,m);	
	}
	
	public void sendIntLoc(MsgType t, int int1, MapLocation loc1)
	{
		Message m = buildNewMessage(1,1);
		m.ints[firstData       ] = int1;
		m.locations[firstData  ] = loc1;
		
		sendMsg(t,m);	
	}
	
	public void sendIntDoubleLoc(MsgType t, int int1, MapLocation loc1, MapLocation loc2)
	{
		Message m = buildNewMessage(1,2);
		m.ints[firstData] = int1;
		m.locations[firstData  ] = loc1;
		m.locations[firstData+1] = loc2;
		
		sendMsg(t,m);	
	}
	
	public void sendDoubleLoc(MsgType t, MapLocation loc1, MapLocation loc2) {
		Message m = buildNewMessage(0,2);
		m.locations[firstData  ] = loc1;
		m.locations[firstData+1] = loc2;
		
		sendMsg(t,m);				
	}
	
	public void sendDoubleIntLoc(MsgType t, int int1, int int2, MapLocation loc1)
	{
		Message m = buildNewMessage(2,1);
		m.ints[firstData       ] = int1;
		m.ints[firstData+1     ] = int2; 
		m.locations[firstData  ] = loc1;
		
		sendMsg(t,m);	
	}
	
	public void sendDoubleIntDoubleLoc(MsgType t, int int1, int int2, MapLocation loc1, MapLocation loc2)
	{
		Message m = buildNewMessage(2,2);
		m.ints[firstData       ] = int1;
		m.ints[firstData+1     ] = int2; 
		m.locations[firstData  ] = loc1;
		m.locations[firstData+1] = loc2;
		
		sendMsg(t,m);	
	}
	
	

	
	
	/**
	 * Very primitive receive function
	 */
	public void receiveAll() {
		
		//Grab the messages
		Message[] rcv = myPlayer.myRC.getAllMessages();
		
		
		//Variable instantiation to reduce bytecode usage
		Message m;
		int[] ints;
		MapLocation[] locs;
		
		
		
		//Run the main loop
		for(int i=rcv.length; --i>=0;) {
			
			m = rcv[i];
			ints = m.ints;
			locs = m.locations;
			
			
			///////////////////////////////////////////////////////////////////////////////
			////////BEGIN MESSAGE VALIDATION SYSTEM
				///////Begin inlined message validation checker
					if(ints==null) continue;
					if(ints.length<minSize) continue;
					
				//////We should have a checksum -- make sure the checksum is right.
					if(ints[idxHash]!=teamKey) continue;
					
				//////We at least have a valid int header
					MsgType t = Encoder.decodeMsgType(ints[idxHeader]);  //pull out the header
					
				//////Now make sure we have enough ints & enough maplocations
					if(ints.length!=t.numInts) continue;
					if(locs==null) continue;
					if(locs.length!=t.numLocs) continue;
			////////MESSAGE HAS BEEN VALIDATED
					
					
					
			///////////////////////////////////////////////////////////////////////////////
			////////BEGIN MESSAGE PROCESSING SYSTEMS
					
				//////Genetic Callback Messages
					if(t.shouldCallback) {
						myPlayer.myBehavior.newMessageCallback(t,rcv[i]);
					}
					
				//////Message Rebroadcasting
					if(t.shouldRebroadcast) {
						
						
						
						
						/////////////////////////////REBROADCAST VALIDATION SYSTEM
						int headerData = ints[idxHeader];
						
						///////// Check the TTL
						if(Clock.getRoundNum() - Encoder.decodeMsgTimeStamp(headerData) > t.ttl) continue; 
						
						
						///////// Check the Distance  
						/////////		(since we heard the message from the sender, we need to be farther from 
						/////////		 origin than the sender0
						MapLocation origin = locs[idxOrigin];
						if(myPlayer.myRC.getLocation().distanceSquaredTo(origin) 
								<= locs[idxSender].distanceSquaredTo(origin)) continue;
						
						
						///////// Message needs to be rebroadcasted
						/////////	(We can modify the original message file since it's already been sent to the callback
						/////////	 and the necessary processing has presumably been done)
						m.locations[idxSender] = myPlayer.myRC.getLocation();
						messageQueue.add(m);
						
						////////////////////////////////////////////////////////////
						
						
					}
		}
	}
	
	
	
	public void sendAll() throws Exception{
		if(!messageQueue.isEmpty() && !myPlayer.myBroadcaster.isActive()) {
			myPlayer.myBroadcaster.broadcast(messageQueue.pop());
		}
	}
	

}

