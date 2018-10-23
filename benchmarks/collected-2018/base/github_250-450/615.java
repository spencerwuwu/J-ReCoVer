// https://searchcode.com/api/result/72637268/

/**
 * GameSession.java
 * 
 * The creation of a game session is triggered by a client. 
 * The session coordinates the game play. It is responsible for informing all connected 
 * clients of the current state of the game.     
 * 
 * Once a session is created, other clients can join the session. 
 * And once the minimum number of players has been reached, the session broadcasts a signal 
 * to all the connected clients and then begins the game.  
 * It keeps a record of the clients connected to the session.   
 * 
 * @author Osazuwa Omigie
 */

package gameModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.Marshaller.Listener;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import gameServer.Server;

public class GameSession extends Thread {
	
	private ServerSocket sessionSocket;  //game session socket
	private Server gameServer;
	private Vector<Socket> connectedClientSockets;
	private int sessionID;
	public static final int MIN_PLAYERS = 2; //minimum number of players per game
	private int timeOut; //question timeout in minute
	private ArrayList<ClientListener> clientListeners;
//	private final Lock lock = new ReentrantLock();
	private final Lock joinGameLock = new ReentrantLock();
//	private final Condition enoughPlayers  = joinGameLock.newCondition();
	
	private BufferManager bufferManager;
	
	PrintWriter outToClient;
	Vector<String> currentQuestion;
	HashMap<String,Vector<String>> resultsMap;
	
	public GameSession(Server server){
		gameServer = server;
		bufferManager = gameServer.getBufferManager();
		
//		this.gameHostQueue = gameHostQueue;
//		this.hostClientSocket = hostClientSocket;
		connectedClientSockets = new Vector<Socket>(MIN_PLAYERS);
		clientListeners = new ArrayList<ClientListener>(MIN_PLAYERS); 
		timeOut = 5500; //in milliseconds
		try{
			sessionSocket = new ServerSocket(0);//create a session socket with any available port
			sessionID = sessionSocket.getLocalPort(); //gameID is the port local port number of the session
		}catch(IOException e){
			System.out.println("problem creating session socket!");
		}
		setName("GameSession");
	}
	
	public Integer getSessionID(){
		return sessionID;
	}
	
	/**
	 * Sends a given string to all connect clients
	 * @param message
	 */
	public void broadCastMessage(String message){
		for (Socket s : connectedClientSockets){
			sendMsgToSocket(message, s);
		}
	}
	
	/**
	 * @return The total number of clients on this game session
	 */
	public int getPlayerCount(){
		return connectedClientSockets.size();
	}
	
	/**
	 * Contains the main game logic.
	 * Once this method is called, no more clients can be added to the session
	 * @param rounds - the number of times players are sent a challenge/word
	 * @return the results of the game 
	 */
	public Vector<String[]> startGame(int rounds){
		System.out.println("Game started!");
		
		//signal all connected clients that game has started
		broadCastMessage("@startGame"); 
		
		//initialize the game data results
		Vector<String[]> resultsData = new Vector<String[]>();
		
		/*
		 * the number of word challenges/rounds
		 * TODO: this could be defined by the game host 
		 */
		while(rounds > 0){
			String word = gameServer.getAword();

			//get a random word (results does not contain)
			word = gameServer.getAword(); //TODO reduce chances of repeated words per game
			
			//the first string in the round result is the received word from teh server
			String [] results = new String [clientListeners.size()+1];
			results[0] = word; 
			
			broadCastMessage(word);
			
			try {
				Thread.sleep(timeOut);  //timer or wait for clients to reply their entries
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/*Iterate through listeners collection and store answers in resultsMap*/
			for(int i=0;i<clientListeners.size();i++){
				results[i+1] = clientListeners.get(i).getAnswer(); //populate the collection of answers from all clients
			}
			
			//store result data
			resultsData.add(results);
			
			rounds--;
		}
		return resultsData;
	}


	/**
	 * Adds a client to the gameSession.
	 * This method should be called by the Server worker thread
	 */
	public void joinGame(Socket clientSocket){
		//spawn a new thread that would listen to this client's requests
		ClientListener listener = new ClientListener(clientSocket);
		
		synchronized (joinGameLock) {
			//Will be used by session to get answers from client
			clientListeners.add(listener);
			
			//add socket to collection of connected clientSockets
			connectedClientSockets.add(clientSocket);
			
			//start the clientListener thread to listen for game inputs
			listener.start(); 
			
			if(connectedClientSockets.size()==MIN_PLAYERS){
				//Session is ready to be started. Wake up the session thread to start game.  
				joinGameLock.notifyAll();
			}
		}
		
		//signal the client that they have joined the gameSession
		String message = String.format("@joinAck %d", this.sessionID);
		sendMsgToSocket(message, clientSocket);
		
	}
	
	/**
	 * send a message to a given client
	 * @param message
	 * @param client
	 */
	public void sendMsgToSocket(String message,Socket client){
		Socket clientSocket = client;
		try {
			outToClient = new PrintWriter(clientSocket.getOutputStream(),true);
			outToClient.println(message);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	/**
	 * Signal connected clients that the session has ended.
	 * Remove this game session from list of active sessions on the main server
	 */
	public void endSession(){
		//remove this session from the server's list of sessions
		gameServer.removeSession(this.sessionID);  				
		broadCastMessage("@quitGame"); //remove all clients from the session
		connectedClientSockets.clear();
	}
	
	
	@Override
	public void finalize(){
		endSession();
	}
	
	
	/**
	 * This method will only be called when starting a game session 
	 * @param socket : the socket of the game's host 
	 */
	public void doWork(Socket socket) {
		//add this session to the server's list of active sessions
		gameServer.addToMap(sessionID, this);
		
		joinGame(socket);//add the host to the session
		
		while(connectedClientSockets.size() < MIN_PLAYERS){
			synchronized (joinGameLock) {
				if(connectedClientSockets.size()==0){
					/*
					 * there must be at least one connected client (usually the host)
					 * Cancel game session if it is empty  
					 */
					return;
				}
				else{
					//wait for join game to signal that there are enough players in the session
					try {
						joinGameLock.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}
			}
		}
		
		//players are ready, now the start game;
		Vector<String []>  gameResults = startGame(2); 
		System.out.println("GAME OVER\n");
			
		for(String [] result : gameResults){
			bufferManager.putFullBuffer(result); //store the results in the word DB
//			for(int i=0;i<result.length;i++){
//				System.out.print(result[i] + ",");
//			}
//			System.out.println("\n");
		}
		
		endSession();
	}
	
	
	
	@Override
	public void run() {
		
		while(true){
			//keep checking the message queue for new host client socket
			Socket socket = gameServer.takeNewGameHostClient(); //will wait on queue if empty
			doWork(socket); 
		}
		
	}
}

/**
 * Handle user input when responding to questions
 *
 */
class ClientListener extends Thread{
	Socket clientSocket;
	String answer;
	BufferedReader inFromClient;
	
	public ClientListener(Socket socket) {
		clientSocket = socket;
		answer="";
		try {
			inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		setName("ClientListener");
	}
	
	/**
	 * returns a null string if no answer have been submitted by the client
	 * or returns the most recent entry by the the client 
	 * @return
	 */
	
	public String getAnswer(){
		String result = new String(answer);
		answer = ""; //reset answer after every read
		return result;
	}
	
	
	/**
	 * Listens for answers from clients 
	 * new answers will overwrite the answer variable. Only 1 answer per question
	 * Returns false if client has been disconnected/closed 
	 */
	public boolean listenForClientResponse(){
		
		try {
			//wait for answer from client
			answer = inFromClient.readLine(); //blocking read
			if(answer == null){
				return false; //the client has been disconnected. Kill this thread 
			}
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false; 		
	}
	
	
	@Override
	public void run() {
		boolean keepListening = true;
		while(!clientSocket.isClosed() && keepListening){
			keepListening = listenForClientResponse();
		}
	}
}

