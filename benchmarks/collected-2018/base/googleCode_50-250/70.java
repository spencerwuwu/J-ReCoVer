// https://searchcode.com/api/result/773235/

package extensions.m2mi.slides;
//******************************************************************************
//
// File:    Screen.java
// Package: edu.rit.slides
// Unit:    Interface edu.rit.slides.Screen
//
// This Java source file is copyright (C) 2001-2004 by Alan Kaminsky. All rights
// reserved. For further information, contact the author, Alan Kaminsky, at
// ark@cs.rit.edu.
//
// This Java source file is part of the M2MI Library ("The Library"). The
// Library is free software; you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the Free Software
// Foundation; either version 2 of the License, or (at your option) any later
// version.
//
// The Library is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
// details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html or by writing to the Free
// Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA.
//
//******************************************************************************

import edu.rit.m2mi.Eoid;
import edu.rit.slides.Projector;
import edu.rit.slides.Slide;

import java.util.EventListener;

/**
 * Interface Screen is the remote interface for an exported screen object in the
 * Slides application.
 * <P>
 * A group of one or more screen objects is attached to a multihandle for
 * interface Screen; this group of screen objects is called a <B>theatre</B>.
 * Using the multihandle, a client can invoke methods on all screens in the
 * theatre. The client is typically a {@link Projector </CODE>Projector<CODE>}
 * object.
 * <P>
 * The screen objects and the projector objects interact as follows. A projector
 * object repeatedly calls <TT>availableSlides()</TT> on a Screen multihandle to
 * tell all screen objects in the theatre which slides the projector object has
 * available. In response, the screen objects start calling <TT>getSlide()</TT>
 * on the projector object to get the individual slides one at a time.
 * <P>
 * If no <TT>availableSlides()</TT> method call arrives from a certain projector
 * within a certain <I>leasetime</I> (for example, <I>leasetime</I> = 30
 * seconds), the screen objects conclude that the projector object has gone
 * away, and the screen objects discard the slides they had obtained from that
 * projector object. To avoid correlated broadcasts, each projector object calls
 * <TT>availableSlides()</TT> at intervals chosen at random in the range (0.2
 * <I>leasetime</I>) to (0.4 <I>leasetime</I>) (for example, 6 to 12 seconds).
 * Thus, a screen object should receive at least two <TT>availableSlides()</TT>
 * method calls from a projector object before timing out. This lets the screen
 * objects tolerate the occasional loss of one <TT>availableSlides()</TT> method
 * call.
 * <P>
 * A screen object calls <TT>getSlide()</TT> on a projector object's unihandle
 * to get a certain slide. In response, the projector object calls
 * <TT>putSlide()</TT> on a Screen multihandle to send the slide to all screens
 * in the theatre.
 * <P>
 * A projector object repeatedly calls <TT>displaySlides()</TT> on a Screen
 * multihandle to tell all screens in the theatre to display a particular slide
 * or slides.
 * <P>
 * If no <TT>displaySlides()</TT> method call arrives from a certain projector
 * within a certain <I>leasetime</I> (for example, <I>leasetime</I> = 30
 * seconds), the screen objects conclude that the projector object has gone
 * away, and the screen objects stop displaying the slides from that projector
 * object. To avoid correlated broadcasts, each projector object calls
 * <TT>displaySlides()</TT> at intervals chosen at random in the range (0.2
 * <I>leasetime</I>) to (0.4 <I>leasetime</I>) (for example, 6 to 12 seconds).
 * Thus, a screen object should receive at least two <TT>displaySlides()</TT>
 * method calls from a projector object before timing out. This lets the screen
 * objects tolerate the occasional loss of one <TT>displaySlides()</TT> method
 * call.
 * <P>
 * The process of transferring slides from projector objects to screen objects
 * is broken up into separate method calls (<TT>availableSlides()</TT> --
 * <TT>getSlide()</TT> -- <TT>putSlide()</TT>) to give the screen objects
 * flexibility in when they obtain the slides. A screen object need not obtain
 * all the slides at once. A screen object can obtain slides one at a time as
 * needed. Or, to reduce the latency when displaying a slide, a screen object
 * can obtain slides a few at a time, ahead of time. Obtaining the slides one at
 * a time also lets other network traffic be interleaved with the slide traffic.
 * <P>
 * The slide itself is not sent as an argument of the <TT>displaySlides()</TT>
 * method to reduce the latency when displaying a slide or slides. The intent is
 * that the screen object would already have obtained the slides in response to
 * an earlier <TT>availableSlides()</TT> method call. The screen object can then
 * display the slides immediately, without having to wait for the slides to come
 * across the network. Of course, if the screen object does not have the
 * specified slides, the screen object will have to ask the projector object to
 * send them (<TT>getSlide()</TT>) and wait for the slides to arrive
 * (<TT>putSlide()</TT>) before the screen object can display them.
 *
 * @author  Alan Kaminsky
 * @version 03-Oct-2003
 * 
 * 
 * CHANGE: Made Screen interface extend EventListener interface such that the
 * methods in this interface will be invoked purely asynchronously by the
 * AmbientTalk symbiosis. This is crucial as it breaks a deadlock in the implementation.
 * If these methods are invoked synchronously from within Java, we can get the
 * following deadlock:
 * 
 * 1) User presses e.g. 'firstSlide' button: GUI thread invokes redisplay()
 *    in SlideProjector, which causes the thread to invoke projector.getSelectedSlideGroupIdx()
 *    Since projector is a wrapped AT object, this causes the GUI to wait for the
 *    actor to execute that method.
 * 2) The timer thread associated with the wrapped ProjectorObject's callDisplaySlides
 *    method wakes up and invokes callDisplaySlides, taking a lock on the ProjectorObject
 *    because it is a synchronized Java method.
 * 3) The actor tries to process the GUI's getSelectedSlideGroupIdx() method. However,
 *    in order to invoke this method, it must take a lock on the wrapped ProjectorObject
 *    because the method is synchronized in Java. Because the timer thread has the lock,
 *    the actor waits for the timer thread to release it.
 * 4) The timer thread invokes myTheatre.displaySlides(...) (the method defined below).
 *    If this method is not invoked purely asynchronously, the timer thread will wait
 *    for the actor to invoke the method, causing a deadlock (actor waits for timer to
 *    release lock, timer waits for actor to execute displaySlides).
 *    
 * We break the deadlock by ensuring that displaySlides can be scheduled without blocking
 * the timer thread.
 * 
 */
public interface AsyncScreen extends EventListener {

// Exported constants.

	/**
	 * Lease time in milliseconds, 30000 msec (30 sec).
	 */
	public static final int LEASE_TIME = 30000;

// Exported operations.

	/**
	 * Notify this screen that a projector has the given slides available. The
	 * given array of slide IDs is a complete list of all the slides the
	 * projector has available at this time.
	 *
	 * @param  theProjector
	 *     Unihandle for the projector.
	 * @param  theSlideIDs
	 *     Array of zero or more slide IDs (type {@link edu.rit.m2mi.Eoid
	 *     </CODE>Eoid<CODE>}) the projector has available.
	 */
	public void availableSlides
		(Projector theProjector,
		 Eoid[] theSlideIDs);

	/**
	 * Provide a slide from the given projector to this screen.
	 *
	 * @param  theProjector
	 *     Unihandle for the projector.
	 * @param  theSlideID
	 *     Slide ID (type {@link edu.rit.m2mi.Eoid </CODE>Eoid<CODE>}).
	 * @param  theSlide
	 *     The slide itself.
	 */
	public void putSlide
		(Projector theProjector,
		 Eoid theSlideID,
		 Slide theSlide);

	/**
	 * Display the given slides on this screen. The given array of slide IDs is
	 * a complete list of the slides from the given projector that are to be
	 * displayed at this time. Any slides from the given projector that had been
	 * displayed are first removed from the display, then the given slides are
	 * added to the display.
	 *
	 * @param  theProjector
	 *     Unihandle for the projector that has the slides.
	 * @param  theSlideIDs
	 *     Array of zero or more slide IDs (type {@link edu.rit.m2mi.Eoid
	 *     </CODE>Eoid<CODE>}) the projector has available that are to be
	 *     displayed.
	 */
	public void displaySlides
		(Projector theProjector,
		 Eoid[] theSlideIDs);

	}

