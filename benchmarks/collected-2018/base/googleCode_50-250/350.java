// https://searchcode.com/api/result/12092933/

/*
 * Time.java
 * 
 * last update: 16.01.2010 by Stefan Saru
 * 
 * author:	Alec(panovici@elcom.pub.ro)
 * 
 * Obs:
 */


package engine;

import java.util.*;

/**
 * The main thread of the simulation & the time manager.
 */
class Time {
	/**
	 * Holds the start time of the simulation, for
	 * cronometration purpose
	 */
	static long time;
	static int time_2; //0 if we are in the firsth part of the time unit
	//1 if it is the secont part ( i.e. the one when cont.
	//assignements take part)

	static Vector controlThreads = new Vector();

	/**
	 * The stuff pending to the end of the time units, time - ordered ;
	 */
	static Vector lastFinisher = new Vector(), finishList = new Vector(); 

	/**
	 * Contains a list of instructions to execute after each time unit
	 */
	static Vector afterFinish = new Vector();
	static boolean anyThreads;

	/**
	 * make it false to stop the simulation
	 */
	static boolean keepWorking;

	/**
	 * starts all the threads
	 */
	static void  startSimulation(){

		time = 0;
		time_2 = 0;

		ControlThread.doRun = true;
		ControlThread.activeThreads = 0;

		anyThreads = false;
		keepWorking = true;

		lastFinisher.clear();
		finishList.clear();

		xConsole.trace("SIMULATION TIME IS 0");
		//start the threads:
		synchronized(controlThreads){
			for(Enumeration e = controlThreads.elements() ; e.hasMoreElements() ; ){
				((Thread)e.nextElement()).start();
				anyThreads = true;
			}
		}
		xConsole.debug("started threads");
	}

	public static long oClock(){
		return time;
	}

	public static long clk2(){
		return (time << 2) + time_2;  //this returns the number of half time units.
	}

	/**
	 * Appends an instruction for the end of the oClock() + delay
	 * time unit.
	 */
	public static void addFinisher(int delay, Instruction i){
		xConsole.debug("adding finisher for #" + (delay + time) + " : " + i);
		if(finishList.size() < delay+10){
			//add some extra space to reduce the time wasted with copying stuff
			finishList.setSize(delay+10);
			lastFinisher.setSize(delay+10);
		}

		if (lastFinisher.elementAt(delay) != null)
			((Instruction)lastFinisher.elementAt(delay)).add(i);
		else finishList.setElementAt(i, delay);
		lastFinisher.setElementAt(i, delay);
	}

	/**
	 * Appends an instruction for the end of the current time unit.
	 */
	public static void addFinisher(Instruction i){
		addFinisher(0, i);
	}

	public static void addAfterFinish(Executable i){
		afterFinish.addElement(i);
	}

	static void removeThread(Thread t){
		synchronized(controlThreads){
			controlThreads.removeElement(t);
		}
		xConsole.debug("removed thread: " + t);
	}

	boolean stop;
	static void stopSimulation(){
		synchronized(controlThreads){

			ControlThread.doRun = false;

			//stop all the threads, no matter what they were doing !
			for(Enumeration e = controlThreads.elements() ; e.hasMoreElements() ; ){
				ControlThread ct = (ControlThread)e.nextElement();
				xConsole.debug("attempting to stop thread : " + ct);
				if(ct != Thread.currentThread())ct.forceStop(); //wakeUp must check wheter it still worths running
			}

			xConsole.debug("done");
			keepWorking = false;
			controlThreads.clear();
			lastFinisher.clear();
			finishList.clear();
			afterFinish.clear();
		}
	}

	/**
	 * the main simulation loop
	 */
	static void runSimulation(){
		boolean allSleepy = true;
		//prevents waiting for eny thread to become active if
		//there isn't one
		if(!anyThreads){
			xConsole.cout("nothing to do!\n");
			return;
		}
		/*
    //wait for some threads to start their jobs
    while(ControlThread.activeThreads == 0 && keepWorking) {
      int i = 100;
      i ++;
      i --;
    }
		 */

		//the main loop
		while(keepWorking){
			try{
				Thread.sleep(100);
			}catch(InterruptedException ex){
				xConsole.debug("Who dares to disturbe the TIME ?");
			}

			synchronized(controlThreads){
				if(ControlThread.activeThreads == 0){
					xConsole.cout("Nothing left to do; exiting\n");
					stopSimulation();
					return;
				}
				allSleepy = true;
				for(Enumeration e = controlThreads.elements() ; e.hasMoreElements() ; ){
					ControlThread t = (ControlThread)e.nextElement();
					synchronized(t){
						allSleepy = allSleepy && t.sleepy;
					}
				}
			}
			if(allSleepy){
				xConsole.debug("all sleepy");
				time_2 = 1;
				if(finishList.size() > 0 &&finishList.firstElement() != null){ //perform pending assignements first; this may 
					//wake up some threads
					xConsole.debug("finishing for time #" + time);
					try{
						Instruction cr = (Instruction)finishList.firstElement();
						while (cr != null) {
							cr.execute();
							cr = cr.next();
						}
					}catch(InterpretTimeException ex){
						xConsole.dumpStack(ex);
						xConsole.debug(ex.toString());
						stopSimulation();
						return;
					}catch(SimulationStoppedException ex){
						stopSimulation();
						return;
					}
					//remove the executed part:
						finishList.setElementAt(null, 0);
						lastFinisher.setElementAt(null, 0);
				}else{
					//first clean up the finish list
					//          xConsole.debug("done finalization for time #" + time);
					if(finishList.size() > 0){
						finishList.removeElementAt(0);
						lastFinisher.removeElementAt(0);
					}

					try{
						Vector tmp = afterFinish;//switch to a new finishing queue
						afterFinish = new Vector(); 
						for(Enumeration e = tmp.elements() ; e.hasMoreElements() ;)
							((Executable)e.nextElement()).execute();
					}catch(InterpretTimeException ex){
						xConsole.dumpStack(ex);
						xConsole.debug(ex.toString());
						stopSimulation();
						return;
					}catch(SimulationStoppedException ex){
						stopSimulation();
						return;
					}
					xConsole.trace("SIMULATION TIME IS " + ++time);
					time_2 = 0;
					xConsole.consumeUnit();
					synchronized(controlThreads){
						//            xConsole.debug("waking up threads...");
						for(Enumeration e = controlThreads.elements() ; e.hasMoreElements() ; )
							((ControlThread)e.nextElement()).wakeUp();  //c'm on man, wake up
						//            xConsole.debug("done");
					}
				}
			}
		}
	}
}







