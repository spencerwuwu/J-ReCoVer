// https://searchcode.com/api/result/40056196/


package DETServer;
import Interface.* ;
import DETApp.*;

import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.io.IOException;
import java.lang.Object;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


/**
 * @author m_sk
 */

//Class DETImplementation is illustrated with proper functionality

class DETImpl extends DETPOA 
{
	

	
        /*
	     * Define list of servers/hosts
	     * Define a local server and few remote servers
	     */
		   public static final String NamingServer = "localhost";
		   public static final String NamingObj = "DETCOMPUTER";
		   
		   public static final String NamingObj1 = "DETCOMPUTER";
		   
		  // public static final String NamingServer = "localhost";
		   public static final String NamingObj2 = "DETSHOE";
		   public static final String NamingObj3 = "DETBOOK";
		   
		   
		   /*
		    * Define list of items that belongs to each server. I assume that each server
		    * contains only one item at time.
		    */
		   
		   public static final String item1 = "Computer";
		   public static final String item2 = "Shoe";
		   public static final String item3 = "Book";
		    
		   
		   /*
		    * Assign value to the each item.
		    */
		   
		   public static final String ITEM = "Computer";
		   public static final double PRICE = 100;
		   public static final int QTY = 200;
		   public static final double BALANCE = 2000;
		
		 //Define semaphore lock for each type of operation
	
	       static public Object lockItm = new Object();
	       static public Object lockSell = new Object();
	       static public Object lockBuy = new Object();
	       static public Object lockTrade = new Object();
	       static public Object lockCanTrade = new Object();
	
				   
	        private ORB orb;

	      Format df = new DecimalFormat("####.00");
	      
	    //Define an object for hashtable
          Hashtable servers;  
          Hashtable itemList;  
         
        //Define an object for the vector
	      Vector otherItems;
	  
	     //Define an object for the ContainerManagedItem 
	      ContainerManagedItem itm;
	      
	      //Define a vector 
	      Vector itemDetail;
	      
	 
	 

	   //Create an object for the message container
	      MessageContainer allMessage = new MessageContainer() ;
	  
	      
	      public void setORB(ORB orb_val)	{ 
	    	  
	    	  this.orb = orb_val;
	    	  
	      }

	
	  //public DETImpl(String itemName, double p, int q, double b) throws RemoteException {
	  public DETImpl() throws RemoteException {
	
		  super( );
		
	    itm = new ContainerManagedItem(ITEM,PRICE,QTY,BALANCE);  
	      	
		servers = new Hashtable() ; 
		itemDetail= new Vector();
		
		itemList= new Hashtable();
		
		
		//Map item to particular server
		
		servers.put(item1,NamingObj1);
		servers.put(item2,NamingObj2);
		servers.put(item3,NamingObj3);
		
		
		// Create an object for the UDP Server at runtime
		
		UDPServer udpServer= new UDPServer();
		
		udpServer.start();
					
	   }
	
	
		
	  
	  /**
	   * Design By Contract
	   * Precondition: This function expects an input item and quantity from the user/client
	   * PostCondition: This function will illustrate buy operation, that client invokes buy method to
	   *                one of the server, and consequently E-trader performs buy and sell operation, in order to 
	   *                 complete that particular transaction. 
	   * 
	   */	
	  
	public int buyItem(String item, int q)
	{
		
		String NameObj;
		
		
		if(itm.name.equalsIgnoreCase(item))  // local product
		{
		//Acquire semaphore lock
			synchronized(lockItm){
				
				if (itm.getQty()>=q) // have quanity then do all action other wise return feedback
				{
					itm.setQty(itm.getQty()-q);
					itm.setBalance(itm.getBalance() + itm.getPrice() * q);
					
					System.out.println("SERVER LOG: BUY LOCAL : ITEM ("+item+") QTY ("+ q +") PRICE (" + itm.getPrice() + ") BAL("+ itm.getBalance() +")");
															
				}
				else return allMessage.NOT_ENOUGH_QUANTITY;
		 		
			}// end of lock synchronize block	
		}
		
		else // find on other servers
		{
					String ser=(String)servers.get(item); // find server deal with this item
					
					if (ser!=null)
					{
						
						//Acquire semaphore lock
						synchronized(lockBuy)	
						{			
							
							
						 try{
							 
							 NameObj=ser;
							
							// callServer method call remote server and its sellItem method
							//Vector res=(Vector) callServer(serName,item,q,itm.getBalance());
							
							 int resInt=(int) callServer(NameObj,item,q,itm.getBalance());
								
																								
							if (resInt > 0 ) // if price all mess are -ve values
							{
								int pr=resInt;
							
								//Acquire semaphore lock
								synchronized(lockItm){	
								
									itm.setBalance( itm.getBalance()- q* pr);
									
									// item, qty, price, server;
									String log= item+ "\t" + q +"\t" + pr + "\t" + NameObj;
									// add enter to server log 
									itemDetail.add(new String(log));
									
									System.out.println("SERVER LOG: BUY FROM ("+ NameObj +") : ITEM ("+  item +") QTY ("+ q +")  PRICE (" + pr + ") BAL("+ itm.getBalance() +")");
																				
									
								}// end of lock synchronize block	
								
								// add item detail in bought item list 
								Vector v=(Vector)itemList.get(item);
								if (v!=null)
									{
										Integer retVal=(Integer)v.elementAt(1);
										q = q + retVal.intValue();
									}
								
																
								Vector prqty=new Vector();
								
								prqty.add(new Double(pr));
								prqty.add(new Integer(q));
								
								itemList.put(item,prqty);
								
								return allMessage.SUCCESS; //Return success message
							}				
							
							else // if buy operation is not successfull then return error
								{
									return resInt; // error mess							
								}
							
						 }catch(Exception e){
							 System.out.println("BUY ITEM Error "+e);
						 }
						
						}// end of lock synchronize block	
					}else
					{ 
						return allMessage.PRODUCT_NOT_FOUND;
					}
				 
		
		}
				
		return allMessage.SUCCESS;
				
	}
	
	

	/**
	 * Design By Contract
	 * Precondition: This function expects no input from the user.
	 * PostCondition: This function will illustrate print report for server side. 
	 * 
	 */		
	
	public int printReport()
	{
		    //Acquire semaphore lock
		    synchronized(lockItm){
			
			String pr=df.format(Double.valueOf(itm.getPrice()));
			String bl=df.format(Double.valueOf(itm.getBalance()));
			System.out.println("");
			System.out.println("*************************  Distributed E-Trader REPORT **************************");
			System.out.println("===========================================================================");
			System.out.println("ITEM_NAME: "+ itm.name +"    QUANTITY:"+ itm.getQty() + "    PRICE : " + pr + "    BALANCE: "+ bl);
			System.out.println("===========================================================================");
		} // end of lock synchronize block	
		
		System.out.println();
			
		System.out.println("");
		System.out.println("=============================================================================") ;
		System.out.println("********************** Item Has Bought from Other E-traders ****************");
		
		Vector v;
		
		if (itemList.size()>0)
		{
		   v=(Vector)itemList.get(item1);
		   if (v!=null)
			   System.out.println(item1+"    price= "+ v.elementAt(0) + "    qty="+ v.elementAt(1) );
		   
		   v=(Vector)itemList.get(item2);
		   if (v!=null)
			   System.out.println(item2+"    price= "+ v.elementAt(0) + "    qty="+ v.elementAt(1) );
		   
		   v=(Vector)itemList.get(item3);
		   if (v!=null)
			   System.out.println(item3+"    price= "+ v.elementAt(0) + "    qty="+ v.elementAt(1) );
		}
		
		System.out.println("==============================================================================") ;
		
		System.out.println();
		System.out.println();
		System.out.println("============================================") ;
		System.out.println("Item_Name \t Quantity \t Price \t Server");
		//System.out.println("Item_NameQuantityPriceServer");
		System.out.println("============================================");
		
		for (int x=0;x<itemDetail.size();x++)
			System.out.println(itemDetail.elementAt(x));
		
		
		
		return 1;
	}
	
	

	/**
	 * Design By Contract
	 * Precondition: This function expects an input item, balance and quantity 
	 * PostCondition: This function will illustrate sell operation, that client invokes buy method to
	 *                one of the server, and consequently E-trader performs sell operation, in order to 
	 *                 complete that particular transaction. 
	 * 
	 */	

	
	//public int sellItem (String item, int q, double bal)
	public int sellItem (int q, double bal)
	{
		
		Vector v = new Vector () ;
		
				
		//if(itm.name.equalsIgnoreCase(item))  // local product
		//{
		    
		//Acquire semaphore lock
		synchronized(lockSell){
				
			   double pr=itm.getPrice();
				
			   if (bal> q*pr) // buyer have balance to buy
			   {
				 //Acquire semaphore lock
				   synchronized(lockItm){
					
					   if (itm.getQty()>=q)  // have quanity to sell
						{					
								
							itm.setQty(itm.getQty()-q);
							itm.setBalance(itm.getBalance() + pr * q);
							
				
							// return successfull response with selling price
							
							v.add(new Integer(allMessage.SUCCESS));
							
							v.add(new Double(pr));
							
							System.out.println("SERVER LOG:		SELL   : ITEM ("+  itm.name +") QUANTITY ("+ q +") PRICE (" + pr + ")   BALANCE("+ itm.getBalance() +")");
														
							return (int)pr;
						}
						else {
				
							return allMessage.NOT_ENOUGH_QUANTITY; 
						}
				   }// accitm syn
			   }
			  else {
				  return allMessage.NOT_ENOUGH_FUNDS; 
			  }
		} // end of lock synchronize block	
					
		//}
		
		//else {
		
		//	return allMessage.PRODUCT_NOT_FOUND;
		//}
		
	}
	

	/**
	 * Design By Contract
	 * Precondition: This function expects an input hostname, item, balance and quantity. 
	 * PostCondition: This function will illustrate call another e-trader server operation,  
	 *                buy or sell operation. It acts as a look up to search another e-trader. 
	 */		
  
	public int callServer(String Nameobj,String item,int q,double bal){
			
		
		
		try {
	    
	         //  call remote method sellItem      
	        
 			 //String arg1[]={"-ORBInitialHost", hostName ,"-ORBInitialPort", "1234"};
			//String arg1[]={"-ORBInitialHost", NamingServer ,"-ORBInitialPort", "1231"};
			String arg1[]={"-ORBInitialHost", "localhost" ,"-ORBInitialPort", "1234"};
		   	 
	    	 ORB orb = ORB.init(arg1, null);

	         
	         // get the root naming context
	         org.omg.CORBA.Object objRef =  orb.resolve_initial_references("NameService");
	         
	         // Use NamingContextExt instead of NamingContext, 
	         // part of the Interoperable naming Service.  
	         
	         NamingContextExt ncRef =  NamingContextExtHelper.narrow(objRef);
	  
	         
	         //String name = Nameobj ; //"DET";
	         String name = "DETSHOE";
	         
	         DET detImpl;
	         
	         detImpl = DETHelper.narrow(ncRef.resolve_str(name));

	         
	         int res = detImpl.sellItem(q,bal);        
	         	                
	         
	         //Vector res = (Vector) h.sellItem(item,q,bal); 
	         	         
	         return res;
	         
	      } // end of try block
	      catch (Exception e) {
	          System.out.println("Call Server Error Happens!: "+Nameobj+"   "+e);
	   
	         
	         return allMessage.SERVER_NOT_FOUND;
	         
	      } //End of Catch block
		
	}


	/**
	 * Design By Contract
	 * Precondition: This function expects an input item name and its quantity for sell operation and 
	 * item2 and quantity2 for buy operations 
	 * PostCondition: This trade function will perform all the trades concurrently.
	 */		
  
	
	public int tradeItems(String item1, int qty1, String item2, int qty2, String etrader) {
				
		Vector v= new Vector();	
		double pr=0; // price of trading item 
		
		//Acquire semaphore lock
		synchronized(lockTrade){
		
			
				if ( item1.equalsIgnoreCase(itm.name) )  // if this etrader dealing with this item1 
				{
					//Acquire semaphore lock
					synchronized(lockItm){
						
						if( itm.getQty() >= qty1)
						{	pr= itm.getPrice();
							itm.setQty(itm.getQty()-qty1);  // deduct qty at beging of transaction 
						}
					} // end of lock item synchronize block	
				}
				
				else // check bought item list
				{
					v=(Vector)itemList.get(item1);
					if (v!=null)
					{
						int qty =((Integer)v.elementAt(1)).intValue();  // qty
						
						if (qty>=qty1)
						{	pr= ((Double)v.elementAt(0)).doubleValue();  // price
							Vector prqty=new Vector();
							prqty.add(new Double(pr));
							prqty.add(new Integer(qty-qty1));
								
							itemList.put(item1,prqty);
						}
					}
				}
				
				if (pr<=0.0) 
					return allMessage.NOT_ENOUGH_QUANTITY;
				
				
				String args1=item1+","+ qty1 + "," + pr + "," + item2 + "," + qty2 +", end";
				
               //UDP call will be performed here
				
				String rMess=UDPcall(args1,etrader);
				
				
				
				String mess[]= rMess.split(",");
				
								
				if (mess[0].equalsIgnoreCase("canTrade")) // complete transaction 
				{
					double rprice= Double.parseDouble(mess[1]);  // retrun price
					
					tradeTransaction(item1, qty1,pr , item2, qty2, rprice, etrader);
					
				}
				else  
				{
					
						if( itm.name.equalsIgnoreCase(item1))
							itm.setQty(itm.getQty()+qty1);  // deduct qty at beging of transaction 
						else{
							v=(Vector)itemList.get(item1);
							if (v!=null)
							{
								int qty =((Integer)v.elementAt(1)).intValue();  // qty
								pr= ((Double)v.elementAt(0)).doubleValue();  // price
								
								Vector prqty=new Vector();
								prqty.add(new Double(pr));
								prqty.add(new Integer(qty+qty1));
										
								itemList.put(item1,prqty);
							}
							
						}
					 
				}
					
		}
		return 0;
	}
	

	/**
	 * Design By Contract
	 * Precondition: This function expects an input message content as a arguments and a server name
	 * 
	 * PostCondition: This UDPcall function will perform UDP sent request and reply message.
	 */		
	
	public  String UDPcall(String args1, String etrader){
		
		byte [] m = args1.getBytes();
		
		// args give message contents and destination hostname
		//Create a DataGram Socket Object a rSocket
		DatagramSocket rSocket = null;
					
		int serverPort = 6780;		                                                 
		
		String rMess="";
		
		try {
			rSocket = new DatagramSocket();    
			
			//Get the IP address of e-trader and create new object called rHost
			InetAddress rHost = InetAddress.getByName(etrader);
			
			DatagramPacket request = new DatagramPacket(m,  args1.length(), rHost, serverPort);
			
			rSocket.send(request);			                        
			
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
			
			rSocket.receive(reply);
									
			String mess= new String (reply.getData());
				
			rMess=mess.trim();
				
			
		}catch (SocketException e){
			
			   System.out.println("UDP Socket Client Message: " + e.getMessage());
		
		}catch (IOException e){
			   System.out.println("IO: " + e.getMessage());
		
		}finally {if(rSocket != null) rSocket.close();}
		
		return rMess;
	}
	
	
	/**
	 * Design By Contract
	 * Precondition: This function expects an input item name and its quantity and price for sell operation and 
	 * item2 and quantity2 and price for buy operations 
	 * 
	 * PostCondition: This trade function will perform all the trades concurrently.
	 */		
  

	public void tradeTransaction(String item1,int qty1,double price1, String item2, int qty2, double price2, String etrader){
		
		int q=0;
		double pr=0;
		
		item1=item1.trim();
		item2=item2.trim();
		
		
		Vector prqty=new Vector();
		// buy
		if (item2.equalsIgnoreCase(itm.name)) // if dealing with this item
		{	
			
			//Acquire semaphore lock
			synchronized(lockItm){
				itm.setQty(itm.getQty()+qty2);  // add qty for item sell(give)
				itm.setBalance(itm.getBalance()-(price2*qty2)); // reduce balance for item buy(give)
			}// end sny accitm
		}
		else // if etrader is not dealing with this item
		{
			Vector v=(Vector)itemList.get(item2);
			int qty=0;
			
			if (v!=null) // found in list
				 qty =((Integer)v.elementAt(1)).intValue();  // qty
				
															
			prqty.add(new Double(price2));
			prqty.add(new Integer(qty+qty2));
					
			itemList.put(item2,prqty);
											
			itm.setBalance(itm.getBalance()- (price2*qty2));  /// reduce balance for buy item
					
		}
		
		
		if (item1.equalsIgnoreCase(itm.name)) // if dealing with this item
		{
			
			//Acquire semaphore lock
			synchronized(lockItm){
				itm.setBalance(itm.getBalance()+(price1*qty1)); // add balance for item buy(give)
			}
		}
		else // if etrader is not dealing with this item
		{
			Vector v=(Vector)itemList.get(item1);
			int qty=0;
			
			if (v!=null) // found in list
				 qty =((Integer)v.elementAt(1)).intValue();  // qty
																			
			prqty.add(new Double(price1));
			prqty.add(new Integer(qty));  // qty already deducuted at beging of transaction 
					
			itemList.put(item1,prqty);
			
			//Acquire semaphore lock
			synchronized(lockItm){								
				itm.setBalance(itm.getBalance()+ (price1*qty1));  /// add balance for buy item
			}	
		}
		
		System.out.println("SERVER LOG: Trade ITEM   : ITEM sell ("+  item1 +") QUANTITY ("+ qty1 +") PRICE (" + price1 + ") ITEM buy ("+  item2 +") QTY ("+ qty2 +") PRICE (" + price2 + ")  from eTrader: "+etrader)  ;
		
		
	}

	
	
	/**
	 * Design By Contract
	 * Precondition: This function expects an input item name and its quantity for sell operation and 
	 * item2 and quantity2 for buy operations 
	 * 
	 * PostCondition: This trade function will perform all the trades concurrently.
	 */		
  
	public double canTrade(String item1,int qty1,double price, String item2, int qty2){
		
		int q=0;
		double pr=0;
		
		item2=item2.trim();
		item1=item1.trim();
				
		Vector prqty=new Vector();
		
		//Acquire semaphore lock
	         synchronized(lockCanTrade)
	{
		// sell
		if (item2.equalsIgnoreCase(itm.name)) // if dealing with this item
		
			//Acquire semaphore lock
			synchronized(lockItm){
				
				if( itm.getQty()>=qty2)
				
				{   pr= itm.getPrice();
					
					itm.setQty(itm.getQty()-qty2);  // reduce qty for item sell(give)
				
					itm.setBalance(itm.getBalance()+(pr*qty2)); // add balance for item sell(give)
					
					itm.setBalance(itm.getBalance()-(price * qty1 )); // reduce balance for item take  
					
					// add qty and update price for bought(take) time 
					Vector v=(Vector)itemList.get(item1);
					
					if (v!=null)
							q=((Integer)v.elementAt(1)).intValue();
							
					q=q+ qty1;
												
					prqty.add(new Double(price));
					
					prqty.add(new Integer(q));
					
					itemList.put(item1,prqty);
					
					
					return pr; // return price Sell item 
					
				}else 
					return allMessage.NOT_ENOUGH_QUANTITY;
		
			}
		
		else // if etrader is not dealing with this item
		{
					
			Vector v=(Vector)itemList.get(item2);
				
				if (v!=null) // found in list
				{
					
					//Acquire semaphore lock
					synchronized(lockItm){
						int qty =((Integer)v.elementAt(1)).intValue();  // qty
						
						if (qty>=qty2) // have qty
						 {
							pr= ((Double)v.elementAt(0)).doubleValue();  // price
															
							prqty.add(new Double(pr));
					
							prqty.add(new Integer(qty-qty2));
							
							itemList.put(item2,prqty);
							
							itm.setBalance(itm.getBalance()+ (pr*qty2));  /// add balance for sell item 
												
							itm.setBalance(itm.getBalance()- (price*qty1));  /// reduce balance for buy item
							
							if (item1.equalsIgnoreCase(itm.name))  // if trading with buying item
							
								itm.setQty(itm.getQty()+qty1);
							else 
							{
								Vector v1=(Vector)itemList.get(item1);
								
								if (v1!=null)
										q=((Integer)v1.elementAt(1)).intValue();
										
								q=q+ qty1;
															
								prqty.add(new Double(price));
								prqty.add(new Integer(q));
								
								itemList.put(item1,prqty);
														
							}
												
							return pr;
							
						 }else
							 return allMessage.NOT_ENOUGH_QUANTITY;
					}// end of lock item synchronize block	
				}
			
				else  // not found in bought item list
					return allMessage.PRODUCT_NOT_FOUND;
					
		}
		
	} 
		
	}
	
	//UDP Class Definition is stated below. I used UDP protocol to communicate with different servers
	
	class UDPServer extends Thread{

      //Create a DataGram Socket object and initialize to null .
		
		DatagramSocket newSocket = null;	
		
			
		public UDPServer(){
								
		}
		
		public  void run(){
			try{
		
				//Define and create a UDP Socket Object with the specific port number.
				newSocket = new DatagramSocket(6789);
		
				//Define an array called buffer Array
				byte[] buffer = new byte[1000];
	 			
				String Reply;
				
				while(true){
					
	 				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
	  				newSocket.receive(request); 
	  				
	  				String mess= new String (request.getData());
	  				
	  				mess=mess.trim();
	  				
	  				String m1[]= mess.split(",");
	  				
	  				
	  				int qty1 = Integer.parseInt(m1[1]);
	  				double pr = Double.parseDouble(m1[2]);
	  				int qty2 = Integer.parseInt(m1[4]);
	  				
	  				//Create an object for DETImpl class
	  				//DETImpl d = new DETImpl();
	  				
	  				// call canTrade method from the DETImpl class

	  				double res= canTrade(m1[0],qty1, pr,m1[3],qty2);
	  				
	  				
	  				
	  				if (res>0){
	  					Reply="canTrade,"+res +", end";
	  					System.out.println("SERVER LOG:	CAN TRADE	Trade ITEM   : ITEM buy ("+  m1[0] +") QUANTITY ("+ qty1 +") PRICE (" + pr + ") ITEM SELL ("+  m1[3] +") QUANTITY ("+ qty2 +") PRICE (" + res + ") ")  ;
	  				}
	  				else
	  					Reply="Trade Can not be performed !!";
	  				
	  				  				
	  				byte [] mm = Reply.getBytes();
	  				
	    			DatagramPacket reply = new DatagramPacket(mm, mm.length, request.getAddress(), request.getPort());
	    			newSocket.send(reply);
	    		}
			  }catch (SocketException e){
				
				System.out.println("Socket Information: " + e.getMessage());
			  }
			 catch (IOException e) {
				
				System.out.println("IO: " + e.getMessage());
				
			} 
			   
			 finally
			 
			 {if(newSocket != null)
				 newSocket.close();
			  }
	    
		}
		
		  
			
		
	}	//End of UDP of  Server class


	
} //end class
