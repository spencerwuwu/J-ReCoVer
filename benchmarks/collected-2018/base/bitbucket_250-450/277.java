// https://searchcode.com/api/result/56270845/

/*
	TUIO processing library - part of the reacTIVision project
	http://reactivision.sourceforge.net/

	Copyright (c) 2005-2008 Martin Kaltenbrunner <mkalten@iua.upf.edu>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package tbeta.tuio;

import java.awt.event.*;
import java.lang.reflect.*;
import processing.core.*;
import com.illposed.osc.*;
import java.util.*;

public class TuioClient implements OSCListener {
  
  public static final int TBETA_CURSOR_PARAM_COUNT = 9;
	
	private int port = 3333;
	private OSCPortIn oscPort;
	private Hashtable objectList = new Hashtable();
	private Vector aliveObjectList = new Vector();
	private Vector newObjectList = new Vector();
	private Hashtable cursorList = new Hashtable();
	private Vector aliveCursorList = new Vector();
	private Vector newCursorList = new Vector();

	private Vector freeCursorList = new Vector();
	private int maxFingerID = -1;

	private long currentFrame = 0;
	private long lastFrame = 0;
	private long startTime = 0;
	private long lastTime = 0;
	
	private final int UNDEFINED = -1;
	
	PApplet parent;
	Method addTuioObject, removeTuioObject, updateTuioObject, addTuioCursor, removeTuioCursor, updateTuioCursor, refresh;
	
	public TuioClient(PApplet parent) {
		this(parent,3333);
	}
	
	public TuioClient(PApplet parent, int port) {
		this.parent = parent;
		this.port=port;
		parent.registerDispose(this);
		
		try { refresh = parent.getClass().getMethod("refresh",new Class[] { Long.TYPE } ); }
		catch (Exception e) { 
			System.out.println("TUIO: missing or wrong 'refresh(long timestamp)' method implementation");
			refresh = null;
		}
		
		try { addTuioObject = parent.getClass().getMethod("addTuioObject", new Class[] { TuioObject.class }); }
		catch (Exception e) { 
			System.out.println("TUIO: missing or wrong 'addTuioObject(TuioObject tobj)' method implementation");
			addTuioObject = null;
		}
		
		try { removeTuioObject = parent.getClass().getMethod("removeTuioObject", new Class[] { TuioObject.class }); }
		catch (Exception e) { 
			System.out.println("TUIO: missing or wrong 'removeTuioObject(TuioObject tobj)' method implementation");
			removeTuioObject = null;
		}
		
		try { updateTuioObject = parent.getClass().getMethod("updateTuioObject", new Class[] { TuioObject.class }); }
		catch (Exception e) { 
			System.out.println("TUIO: missing or wrong 'updateTuioObject(TuioObject tobj)' method implementation");
			updateTuioObject = null;
		}
		
		try { addTuioCursor = parent.getClass().getMethod("addTuioCursor", new Class[] { TuioCursor.class }); }
		catch (Exception e) { 
			System.out.println("TUIO: missing or wrong 'addTuioCursor(TuioCursor tcur)' method implementation");
			addTuioCursor = null;
		}
		
		try { removeTuioCursor = parent.getClass().getMethod("removeTuioCursor", new Class[] { TuioCursor.class }); }
		catch (Exception e) { 
			System.out.println("TUIO:missing or wrong 'removeTuioCursor(TuioCursor tcur)' method implementation");
			removeTuioCursor = null;
		}
		
		try { updateTuioCursor = parent.getClass().getMethod("updateTuioCursor", new Class[] { TuioCursor.class }); }
		catch (Exception e) { 
			System.out.println("TUIO: missing or wrong 'updateTuioCursor(TuioCursor tcur)' method implementation");
			updateTuioCursor = null;
		}
		
		try {
			oscPort = new OSCPortIn(port);
			oscPort.addListener("/tuio/2Dobj",this);
			oscPort.addListener("/tuio/2Dcur",this);
			oscPort.startListening();
			startTime = System.currentTimeMillis();
			System.out.println("listening for TUIO messages on port "+port);
		} catch (Exception e) {
			System.out.println("failed to connect to port "+port);
		}
	}

	public TuioObject[] getTuioObjects() {
		return (TuioObject[])(objectList.values().toArray(new TuioObject[0]));
	}
	
	public TuioCursor[] getTuioCursors() {
		return (TuioCursor[])(cursorList.values().toArray(new TuioCursor[0]));
	}	

	public TuioObject getTuioObject(long s_id) {
		return (TuioObject)objectList.get(new Long(s_id));
	}
	
	public TuioCursor getTuioCursor(long s_id) {
		return (TuioCursor)cursorList.get(new Long(s_id));
	}	

	public void acceptMessage(Date date, OSCMessage message) {
			
		Object[] args = message.getArguments();
		String command = (String)args[0];
		String address = message.getAddress();
				
		if (address.equals("/tuio/2Dobj")) {

			if (command.equals("set")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;
				long s_id  = ((Integer)args[1]).longValue();
				int f_id  = ((Integer)args[2]).intValue();
				float xpos = ((Float)args[3]).floatValue();
				float ypos = ((Float)args[4]).floatValue();
				float angle = ((Float)args[5]).floatValue();
				float xspeed = ((Float)args[6]).floatValue();
				float yspeed = ((Float)args[7]).floatValue();
				float rspeed = ((Float)args[8]).floatValue();
				float maccel = ((Float)args[9]).floatValue();
				float raccel = ((Float)args[10]).floatValue();
				
				if (objectList.get(new Long(s_id)) == null) {
				
					TuioObject addObject = new TuioObject(s_id,f_id,xpos,ypos,angle);
					objectList.put(new Long(s_id),addObject);
					
					if (addTuioObject!=null) {
						try { addTuioObject.invoke(parent, new Object[] { addObject }); }
						catch (IllegalAccessException e) {}
						catch (IllegalArgumentException e) {}
						catch (InvocationTargetException e) {}
					}
				} else {
				
					TuioObject updateObject = (TuioObject)objectList.get(new Long(s_id));
					if ((updateObject.getX()!=xpos) || (updateObject.getY()!=ypos) || (updateObject.getAngle()!=angle) || (updateObject.getSpeedY()!=xspeed) || (updateObject.getSpeedY()!=yspeed) || (updateObject.getRotationSpeed()!=rspeed) || (updateObject.getMotionAccel()!=maccel) || (updateObject.getRotationAccel()!=raccel) ) {
						updateObject.update(xpos,ypos,angle,xspeed,yspeed,rspeed,maccel,raccel);
						
						if (updateTuioObject!=null) {
							try { updateTuioObject.invoke(parent, new Object[] { updateObject }); }
							catch (IllegalAccessException e) {}
							catch (IllegalArgumentException e) {}
							catch (InvocationTargetException e) {}
						}
					}
				}
	
				//System.out.println("set obj " +s_id+" "+f_id+" "+xpos+" "+ypos+" "+angle+" "+xspeed+" "+yspeed+" "+rspeed+" "+maccel+" "+raccel);
				
			} else if (command.equals("alive")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;
	
				for (int i=1;i<args.length;i++) {
					// get the message content
					long s_id = ((Integer)args[i]).longValue();
					newObjectList.addElement(new Long(s_id));
					// reduce the object list to the lost objects
					if (aliveObjectList.contains(new Long(s_id)))
						 aliveObjectList.removeElement(new Long(s_id));
				}
				
				// remove the remaining objects
				for (int i=0;i<aliveObjectList.size();i++) {
					TuioObject removeObject = (TuioObject)objectList.remove(aliveObjectList.elementAt(i));
					if (removeObject==null) continue;
					removeObject.remove();
					if (removeTuioObject!=null) {
						try { removeTuioObject.invoke(parent, new Object[] { removeObject }); }
						catch (IllegalAccessException e) {}
						catch (IllegalArgumentException e) {}
						catch (InvocationTargetException e) {}
					}
				}
				
				Vector buffer = aliveObjectList;
				aliveObjectList = newObjectList;
				
				// recycling of the vector
				newObjectList = buffer;
				newObjectList.clear();
					
			} else if (command.equals("fseq")) {
				if (currentFrame>=0) lastFrame = currentFrame;
				currentFrame = ((Integer)args[1]).intValue();
				
				if ((currentFrame>=lastFrame) || (currentFrame<0)) {
					long currentTime = lastTime;
					if (currentFrame>lastFrame) {
						currentTime = System.currentTimeMillis()-startTime;
						lastTime = currentTime;
					}
					
					Enumeration refreshList = objectList.elements();					
					while(refreshList.hasMoreElements()) {
						TuioObject refreshObject = (TuioObject)refreshList.nextElement();
						if (refreshObject.getUpdateTime()==UNDEFINED) refreshObject.setUpdateTime(currentTime);
					}
					
					if (refresh!=null) {
						try { refresh.invoke(parent,new Object[] { new Long(currentTime) }); }
						catch (IllegalAccessException e) {}
						catch (IllegalArgumentException e) {}
						catch (InvocationTargetException e) {}
					}
				}
			}

		} else if (address.equals("/tuio/2Dcur")) {

			if (command.equals("set")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;

				long s_id  = ((Integer)args[1]).longValue();
				float xpos = ((Float)args[2]).floatValue();
				float ypos = ((Float)args[3]).floatValue();
				float xspeed = ((Float)args[4]).floatValue();
				float yspeed = ((Float)args[5]).floatValue();
				float maccel = ((Float)args[6]).floatValue();

				float blobw = 0f, blobh = 0f;
				// find out if it's a tbeta param list and if it is get the blob's width and height
				if(TBETA_CURSOR_PARAM_COUNT == args.length) {
					blobw = ((Float)args[7]).floatValue();
					blobh = ((Float)args[8]).floatValue();
				}
        
				if (cursorList.get(new Long(s_id)) == null) {

					int f_id = cursorList.size();
					if (cursorList.size()<=maxFingerID) {
						TuioCursor closestCursor = (TuioCursor)freeCursorList.firstElement();
						Enumeration testList = freeCursorList.elements();
						while (testList.hasMoreElements()) {
							TuioCursor testCursor = (TuioCursor)testList.nextElement();
							if (testCursor.getDistance(xpos,ypos)<closestCursor.getDistance(xpos,ypos)) closestCursor = testCursor;
						}
						f_id = closestCursor.getFingerID();
						freeCursorList.removeElement(closestCursor);
					} else maxFingerID = f_id;		
					
					TuioCursor addCursor = new TuioCursor(s_id,f_id,xpos,ypos);
					cursorList.put(new Long(s_id),addCursor);

					if (addTuioCursor!=null) {
						try { addTuioCursor.invoke(parent, new Object[] { addCursor }); }
						catch (IllegalAccessException e) {}
						catch (IllegalArgumentException e) {}
						catch (InvocationTargetException e) {}
					}
				} else {
				
					TuioCursor updateCursor = (TuioCursor)cursorList.get(new Long(s_id));
					if ((updateCursor.getX()!=xpos) || (updateCursor.getY()!=ypos) || (updateCursor.getSpeedX()!=xspeed) || (updateCursor.getSpeedY()!=yspeed) || (updateCursor.getMotionAccel()!=maccel) ) {

						updateCursor.update(xpos,ypos,xspeed,yspeed,maccel, blobw, blobh);
						if (updateTuioCursor!=null) {
							try { updateTuioCursor.invoke(parent, new Object[] { updateCursor }); }
							catch (IllegalAccessException e) {}
							catch (IllegalArgumentException e) {}
							catch (InvocationTargetException e) {}
						}
					}
				}
				
				//System.out.println("set cur " + s_id+" "+xpos+" "+ypos+" "+xspeed+" "+yspeed+" "+maccel);
				
			} else if (command.equals("alive")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;
	
				for (int i=1;i<args.length;i++) {
					// get the message content
					long s_id = ((Integer)args[i]).longValue();
					newCursorList.addElement(new Long(s_id));
					// reduce the object list to the lost objects
					if (aliveCursorList.contains(new Long(s_id))) 
						aliveCursorList.removeElement(new Long(s_id));
				}
				
				// remove the remaining objects
				for (int i=0;i<aliveCursorList.size();i++) {
					TuioCursor removeCursor = (TuioCursor)cursorList.remove(aliveCursorList.elementAt(i));
					if (removeCursor==null) continue;
					removeCursor.remove();

					if (removeCursor.finger_id==maxFingerID) {
						maxFingerID = -1;
						if (cursorList.size()>0) {
							Enumeration clist = cursorList.elements();
							while (clist.hasMoreElements()) {
								int f_id = ((TuioCursor)clist.nextElement()).getFingerID();
								if (f_id>maxFingerID) maxFingerID=f_id;
							}

							Enumeration flist = freeCursorList.elements();
							while (flist.hasMoreElements()) {
								TuioCursor testCursor = (TuioCursor)flist.nextElement();
								if (testCursor.getFingerID()>=maxFingerID)freeCursorList.removeElement(testCursor);
							}
						}
					} else if (removeCursor.finger_id<maxFingerID) freeCursorList.addElement(removeCursor);					
					
					//System.out.println("remove "+id);
					if (removeTuioCursor!=null) {
						try { removeTuioCursor.invoke(parent, new Object[] { removeCursor }); }
						catch (IllegalAccessException e) {}
						catch (IllegalArgumentException e) {}
						catch (InvocationTargetException e) {}
					}
				}
				
				Vector buffer = aliveCursorList;
				aliveCursorList = newCursorList;
				
				// recycling of the vector
				newCursorList = buffer;
				newCursorList.clear();
			} else if (command.equals("fseq")) {
				if (currentFrame>=0) lastFrame = currentFrame;
				currentFrame = ((Integer)args[1]).intValue();
				
				if ((currentFrame>=lastFrame) || (currentFrame<0)) {
					long currentTime = lastTime;
					if (currentFrame>lastFrame) {
						currentTime = System.currentTimeMillis()-startTime;
						lastTime = currentTime;
					}
					
					Enumeration refreshList = cursorList.elements();					
					while(refreshList.hasMoreElements()) {
						TuioCursor refreshCursor = (TuioCursor)refreshList.nextElement();
						if (refreshCursor.getUpdateTime()==UNDEFINED) refreshCursor.setUpdateTime(currentTime);
					}
					
					if (refresh!=null) {
						try { refresh.invoke(parent,new Object[] { new Long(currentTime) }); }
						catch (IllegalAccessException e) {}
						catch (IllegalArgumentException e) {}
						catch (InvocationTargetException e) {}
					}
				}
			} 

		}
	}
	
	public void pre() {
		//method that's called just after beginFrame(), meaning that it 
		//can affect drawing.
	}

	public void draw() {
		//method that's called at the end of draw(), but before endFrame().
	}
	
	public void mouseEvent(MouseEvent e) {
		//called when a mouse event occurs in the parent applet
	}
	
	public void keyEvent(KeyEvent e) {
		//called when a key event occurs in the parent applet
	}
	
	public void post() {
		//method called after draw has completed and the frame is done.
		//no drawing allowed.
	}
	
	public void size(int width, int height) {
		//this will be called the first time an applet sets its size, but
		//also any time that it's called while the PApplet is running.
	}
	
	public void stop() {
		//can be called by users, for instance movie.stop() will shut down
		//a movie that's being played, or camera.stop() stops capturing 
		//video. server.stop() will shut down the server and shut it down
		//completely, which is identical to its "dispose" function.
	}
	
	public void dispose() {
	
		oscPort.stopListening();
		try { Thread.sleep(100); }
		catch (Exception e) {};
		oscPort.close();
	
		//this should only be called by PApplet. dispose() is what gets 
		//called when the host applet is stopped, so this should shut down
		//any threads, disconnect from the net, unload memory, etc. 
	}
}

