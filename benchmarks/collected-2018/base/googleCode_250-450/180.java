// https://searchcode.com/api/result/12538197/

package dtrs;

import helpers.d;

import java.util.Hashtable;

import soenudp.Command;
import soenudp.UDPException;
import soenudp.UDPHelper;
import soenudp.UDPMessage;

public class DTRSManager {
    
    /* Addressing for Multicast MTL Group */
    String MTL_MULTICAST_ADR   = "235.255.0.1";
    int    MTL_MULTICAST_PORT  = 9200;

    /* Addressing for Multicast TOR Group */
    String TOR_MULTICAST_ADR   = "235.255.0.2";
    int    TOR_MULTICAST_PORT  = 9100;
   
    /* Used by the front-end... GMS can just look at his view instead... */
    int    LeaderPortMTL = 20020; // Just change those if you get bind
    int    LeaderPortTOR = 20060; // Note that you can only have one instance of Leader MTL and Leader TOR
    int    LeaderPort; // Will hold the correct port depending on the group             
    int    otherLeaderPort; // Will hold the other leader correct port depending on the group             
    
    int id; // The id from main() is appended to the city, Ex: MTL00 + id = MTL001
    boolean isLeader; // That the state variable that the 'ULEAD' command must change 
    boolean validTransaction = false;
    
    Hashtable<String, Integer> allShows = new Hashtable<String, Integer>();
    Hashtable<String, Hashtable<String,Integer>> reservations = new Hashtable<String, Hashtable<String,Integer>>();
    
    public void doLeader(String type, int id) throws UDPException
    {
        this.id = id;
        isLeader = true;
        String nodeID = type + "00" + id;
        
        // Whatever table/list worked with a synchronized blocks... Or use Lock object
        if (type.equals("MTL"))
        {
            this.LeaderPort = LeaderPortMTL; // Set the port
            this.otherLeaderPort = this.LeaderPortTOR;
            
            /* Populate a reservation list for Montreal here */
        	allShows.put(type + "111", 3);
    		allShows.put(type + "222", 20);
    		allShows.put(type + "333", 5);
    		allShows.put(type + "444", 40);
    		allShows.put(type + "555", 20);
        }
        else if (type.equals("TOR"))
        {
            this.LeaderPort = LeaderPortTOR; // Set the port
            this.otherLeaderPort = this.LeaderPortMTL;
            
            /* Populate a reservation list for Toronto here */
        	allShows.put(type + "111", 3);
    		allShows.put(type + "222", 20);
    		allShows.put(type + "333", 5);
    		allShows.put(type + "444", 40);
    		allShows.put(type + "555", 20);
        }
        
        UDPHelper groupHelper = new UDPHelper(); 
        groupHelper.joinGroup(MTL_MULTICAST_ADR, MTL_MULTICAST_PORT, isLeader, nodeID);
        
        UDPHelper unicast  = new UDPHelper();
        unicast.startUnicastServer(nodeID, LeaderPort); 
        d.out("DTRS " + nodeID + " PORT: " + LeaderPort + " now ready to process requests\n");
        
        UDPMessage request = new UDPMessage(); // <-- This is the object containing requests from the front-end.
        
        // This is the loop that get fed by the FIFO queue, which in return, is populated by the front-end.
        while ( ( request = unicast.getMessage( )) != null ) // One iteration is the equivalent of 1 request
        {
        	validTransaction = false;
        	
            Command command  = request.command; // CHECK, CANCEL, RESERVE, EXCHANGE
            String  showID   = request.show_id; // ShowID
            String  custID   = request.cust_id; // Customer ID
            long    tickets  = request.tickets; // Tickets to reserve/cancel or reservedTickets for exchange... 
            long    desTick  = request.desired_tickets; // Desired tickets amount for exchange
            String  desShow  = request.desired_show_id; // Desired ShowID
            
            d.out("Command: " + command + " CustID: " + custID + " ShowID: " + showID + " Tickets: " + tickets +
            		" DesiredShowID: " + desShow + " DesiredAmount: " + desTick );
            
            String errorMsg  = "";
            String succesMsg = "";
            
            switch(command)
            {
            case CHECK: // Check how many tickets are available
                
            	if(!allShows.containsKey(showID)){
    				errorMsg = "Show '" + showID + "' does not exist";
    				validTransaction = false; // just to make sure, even do it should not enter the other cases (the break;)...
    				break; 
    			} else {
                    tickets = allShows.get(showID);
                    validTransaction = true;
                    succesMsg = "Show '" + showID + "' has '" + tickets + "' tickets available.";  
    			}
            	
            	String lock = null;

        		for (String show : allShows.keySet()) {
        			if (show.equals(showID)) {
        				lock = show;
        			}
        		}

        		synchronized (lock) {
        			tickets = allShows.get(showID);
        		    validTransaction = true;
        		    succesMsg = "Show '" + showID + "' has '" + tickets + "' tickets available.";  
        		    
        		    // You must do it for every case and in every if ( bad ) else ( valid )
        		}
              break;
    
            case RESERVE:
            	
            	if(!allShows.containsKey(showID)){

                    errorMsg = "Show '" + showID + "' does not exist";
                    validTransaction = false;
    				break;
    			}
            	
            	lock = null;

        		for (String show : allShows.keySet()) {
        			if (show.equals(showID)) {
        				lock = show;
        			}
        		}

        		synchronized (lock) {

        			long addNumTickets = tickets;

        			Integer avNumTick = allShows.get(showID);
        			
        			if (tickets <= avNumTick) {
        				
        				Hashtable<String, Integer> reserveTable = reservations.get(showID);

						if (reserveTable == null) {
							reserveTable = new Hashtable<String, Integer>();

						} else if (reserveTable.containsKey(custID)) {
							Integer oldNumTick = reserveTable.get(custID);
							tickets = oldNumTick + tickets;
						}

        				allShows.put(showID, (int) (avNumTick - addNumTickets));
        				reserveTable.put(custID, (int) tickets);
        				reservations.put(showID, reserveTable);
        				
        				succesMsg = "You have successfully reserved " + tickets + " tickets for show " + showID;
        				validTransaction = true;
        			} else {
        				errorMsg = "The number of tickets you are trying to reserve is not available";
                        validTransaction = false;
        			}

        		}
                break;
                
            case CANCEL:
            	
            	if(!allShows.containsKey(showID)){
    				errorMsg = "Show '" + showID + "' does not exist";
                    validTransaction = false;
    				break;
    			}
            	
            	lock = null;

        		for (String show : allShows.keySet()) {
        			if (show.equals(showID)) {
        				lock = show;
        			}
        		}

        		synchronized (lock) {
        			int avNumTick = allShows.get(showID);
        			long numTikToRemv = tickets;

    				Hashtable<String, Integer> reserveTable = reservations.get(showID);

    				if(reserveTable == null) {
    					reserveTable = new Hashtable<String, Integer>();
    				} else if (reserveTable.containsKey(custID)) {
        				// if the customer had a reservation before for the same show,
        				// reduce the number of tickets by numberOfTickets
        				Integer oldNumTick = reserveTable.get(custID);

        				if (tickets > oldNumTick) {
            				errorMsg = "The number of ticket reservations you are canceling is not correct";
                            validTransaction = false;
            				break;
        				}

        				tickets = oldNumTick - tickets;

        				// if the customer doesn't have any reservation
        			} else {
        				errorMsg = "You don't have reservation for the show you are trying to cancel";
                        validTransaction = false;
        				break;
        			}

        			allShows.put(showID, (int) (avNumTick + numTikToRemv));

        			if (tickets == 0) {
        				reserveTable.remove(custID);
        			} else {
        				reserveTable.put(custID, (int) tickets);
        			}

        			reservations.put(showID, reserveTable);
        		}
				
				succesMsg = "You have successfully canceled " + tickets + " tickets for show " + showID;
				validTransaction = true;
        		
                break;     
                
            case EXCHANGE: // Obviously this one will require to have MTL and TOR...
            	
            	//TODO: Need to test
            	
            	if(!allShows.containsKey(showID)){
    				errorMsg = "Show '" + showID + "' does not exist";
                    validTransaction = false;
    				break;
    			}
            	
            	lock = null;

        		for (String show : allShows.keySet()) {
        			if (show.equals(showID)) {
        				lock = show;
        			}
        		}

        		synchronized (lock) {
        			int avNumTick = allShows.get(showID);
        			long numTikToRemv = tickets;

        			// check if reservedTickets <= reserved tickets for customerID and
        			// reservedShowID
        			
        			Hashtable<String, Integer> reserveTable = reservations.get(showID);
        			
        			if (reserveTable == null) {
    					reserveTable = new Hashtable<String, Integer>();
    				} else if (reserveTable.containsKey(custID)) {

        				// if the customer had a reservation before for the same show,

        				Integer oldNumTick = reserveTable.get(custID);

        				if (tickets > oldNumTick) {
        					errorMsg = "The number of ticket reservations you are canceling is not correct";
                            validTransaction = false;
            				break;
        				} else {
//        					MyDatagramSocket mySocket = null;
        						UDPHelper exchangeUnicast  = new UDPHelper();
//        				        exchangeUnicast.startUnicastServer(nodeID, LeaderPort);
        				        
//        						InetAddress receiverHost = InetAddress
//        								.getByName("localhost");
//        						int receiverPort = 1237;
//        						int myPort = 1236;
//        						String message = "exchange:" + desiredShowID + ":"
//        								+ desiredTickets + ":" + custID;
        						
        						UDPMessage message = new UDPMessage(desShow, custID, Command.RESERVE, desTick, 123, "");

//        						mySocket = new MyDatagramSocket(myPort);
        						// instantiates a datagram socket for both sending
        						// and receiving data
        						System.out.println("send exchange info...");
//        						mySocket.sendMessage(receiverHost, receiverPort,
//        								message);
        						
        						// Where to get the IP from? 
        						// Davis: I will create a command at the end... 
        						// It will work for now using 127.0.0.1

        						exchangeUnicast.startUnicastClient("EX_CLIENT " + nodeID);
        						exchangeUnicast.sendToNode("127.0.0.1", otherLeaderPort, message);
        						
        						// now wait to receive a datagram from the socket
//        						String receivedMessage = mySocket.receiveMessage();
        						UDPMessage receivedMessage = exchangeUnicast.getMessage();
        						exchangeUnicast.stopUnicast();
//        						mySocket.close();
        						System.out.println("received exchange info: "
        								+ receivedMessage.command + " " + receivedMessage.op_text);

        						System.out.println("Received msg: " + receivedMessage);

        						if (receivedMessage.command.equals(Command.SUCCESS)) {

        							tickets = oldNumTick - tickets;
        							allShows.put(showID, (int) (avNumTick + numTikToRemv));

        							if (tickets == 0) {
        								reserveTable.remove(custID);
        							} else {
        								reserveTable.put(custID, (int) tickets);
        							}

        							reservations.put(showID, reserveTable);
        						}

        						// if the message received is not canExchange
        						else {
        							errorMsg = "The number of tickets you are trying to reserve " + desTick
											+ " for " + desShow + "is not available";
                                    validTransaction = false;
                    				break;
        						}
        				}

        			} else {
        				errorMsg = "You do not have the tickets reserved";
                        validTransaction = false;
        				break;
        			}
        		}
        		succesMsg = "You have successfully exchanged " + tickets + " tickets for show " + showID
        				+ " with " + desTick + " for show " + desShow;
				validTransaction = true;
                break;   
                
            default: // If you get here just reply an error. It's not a good command.
                validTransaction = false;
                errorMsg = "Invalid command (" + command + ")";
                break;
            }
            
           
            if (validTransaction)//(Operation was successful)  
            {
                d.out("VALID Transaction! I'm replying to the Front-End: " + succesMsg + "\n");
                groupHelper.sendToGroup(request); // Pass to group
                // #2---> Skip this part for now. This is the part that you either start a thread or do spinLock(gotReply or timeout)
                UDPMessage successReply = new UDPMessage();
                successReply.command = Command.SUCCESS;
                successReply.op_text = succesMsg;
                unicast.sendToNode(request.recv_from_ip, request.recv_from_port, successReply);
            } 
            else // Invalid transaction 
            {   // Error to pass the request to the group! reply an error to the front=end....
                d.out("BAD Transaction! I'm replying to the Front-End: " + errorMsg  + "\n");
                UDPMessage errorReply = new UDPMessage();
                errorReply.command = Command.ERROR;
                errorReply.op_text = errorMsg;
                unicast.sendToNode(request.recv_from_ip, request.recv_from_port, errorReply);
            }
        } /* Back to the top of while() */
    }
}

