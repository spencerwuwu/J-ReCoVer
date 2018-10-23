// https://searchcode.com/api/result/781280/

package edu.berkeley.cs169.client.rpc;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.mapitz.gwt.googleMaps.client.GLatLng;
import com.mapitz.gwt.googleMaps.client.GLatLngBounds;
import com.mapitz.gwt.googleMaps.client.GMarker;
import com.mapitz.gwt.googleMaps.client.GMarkerEventClickListener;
import com.mapitz.gwt.googleMaps.client.GMarkerEventManager;
import com.mapitz.gwt.googleMaps.client.GMarkerOptions;
import com.mapitz.gwt.googleMaps.client.GPolyline;

import edu.berkeley.cs169.client.CMF;
import edu.berkeley.cs169.client.helpers.CustomIcon;
import edu.berkeley.cs169.server.graph.Node;
import edu.berkeley.cs169.server.graph.Path;

public class RoutingCallback implements AsyncCallback {

    private Node beginNode, endNode, cafe, library, lab;

    private GMarkerEventManager markerEventMan;  
    
    // Travelling speeds, units: distance/min, used for calculating time estimate
    private static final double WALK_SPEED = 220.97585490989888;
    private static final double BIKE_SPEED = 630.97585490989888;
    
    // Used to enumerate the markers
    private int locCount = 1;

    public void onFailure(final Throwable caught) {
        GWT.log("Error in RoutingService! ", caught);
        caught.printStackTrace();
        CMF.message("Something bad happened while trying to route your request. Please try again later.");
        doThisBeforeReturn(true);
    }

    public void onSuccess(final Object result) {
        
        int numSuccess = 0; // Count the good paths that came back
        
        // Clear the map from indexes to GOverlayCollections, because we're going to fill them again!
        CMF.locPanel.clearOverlayMap();

        if (result == null) {
            CMF.message("Sorry, we couldn't find a complete path for that list of locations! Please change the list or your preferences and try again.");
            GWT.log("RoutingCallback got a null path list!", new Throwable());
            doThisBeforeReturn(false);
            return;
        }  

        // Cast result object (that came here asynchronously) into a Path 'list'
        List pathList = (List) result;

        if (pathList.size() == 0) {
            CMF.message("Sorry, we do not have any complete routes to draw for you... Please try again.");
            GWT.log("Route list came back empty?!", new Throwable());
            doThisBeforeReturn(false);
            return;
        }

        CMF.message(pathList.size() + " routes to plot. Plotting them for you now...");
        // Iterate through the pathList and get each node array
        
        // set the initial rectangle boundary to maximum value
        // this is to set the sw, ne coordinates of thee box bounding the route
        double leftbound = Double.POSITIVE_INFINITY;
        double rightbound = Double.NEGATIVE_INFINITY;
        double upbound = Double.NEGATIVE_INFINITY;
        double downbound = Double.POSITIVE_INFINITY;
        
        //goes through each route and adds up all the coordinates and finally divides
        //by the number of nodes to get the average node coordinate
        int cnt = 0;
        double totx = 0;
        double toty = 0;
        boolean first = true;
        double totdistance = 0;
        for (Iterator i = pathList.iterator(); i.hasNext();) {
            Path p = (Path) i.next();
            if (p == null) {
                locCount++; // regardless, keep the locCount correct
            } else {
                numSuccess++;
                Node[] route = p.nodes;
                
                totdistance += p.distance;
                beginNode = route[0];
                endNode = route[route.length - 1];
                cafe = p.cafe;
                lab = p.lab;
                library = p.library;
                
                if (first){	
                    leftbound = beginNode.x;
                    rightbound = beginNode.x;
                    downbound = beginNode.y;
                    upbound = beginNode.y;
                    first = false;
                }
                if (cafe != null){
                	if (cafe.x < leftbound){
                    	leftbound = cafe.x;
                    }
                    if (cafe.x > rightbound){
                        rightbound = cafe.x;
                    }
                    if (cafe.y < downbound){
                    	downbound = cafe.y;
                    }
                    	 
                    if (cafe.y > upbound){
                        upbound = cafe.y;
                    }
                    totx += cafe.x;
                    toty += cafe.y;
                    cnt += 1;
                }
                if (lab != null){
                	if (lab.x < leftbound){
                    	leftbound = lab.x;
                    }
                    if (lab.x > rightbound){
                        rightbound = lab.x;
                    }
                    if (lab.y < downbound){
                    	downbound = lab.y;
                    }
                    	 
                    if (lab.y > upbound){
                        upbound = lab.y;
                    }
                    totx += lab.x;
                    toty += lab.y;
                    cnt += 1;
                }
                
                if (library != null){
                	if (library.x < leftbound){
                    	leftbound = library.x;
                    }
                    if (library.x > rightbound){
                        rightbound = library.x;
                    }
                    if (library.y < downbound){
                    	downbound = library.y;
                    }
                    	 
                    if (library.y > upbound){
                        upbound = library.y;
                    }
                    totx += library.x;
                    toty += library.y;
                    cnt += 1;
                }
                
                //i should technically be checking both beginNode and endNode
                //when updating the four bounds, but i think endNode by itself
                //is good enough, because the current endNode is close enough to the 
                //next path's beginNode => reduce redundancy
                if (endNode.x < leftbound){
                	leftbound = endNode.x;
                }
                if (endNode.x > rightbound){
                    rightbound = endNode.x;
                }
                if (endNode.y < downbound){
                	downbound = endNode.y;
                }
                	 
                if (endNode.y > upbound){
                    upbound = endNode.y;
                }
                
                totx += (beginNode.x + endNode.x);
                toty += (beginNode.y + endNode.y);
                cnt += 2;
                
                
                // Iterate through the array and make new GLatLng objects from the Nodes
                GLatLng[] ptArray = new GLatLng[route.length];
                for (int j = 0; j < route.length; j++) {
                    Node tmp = route[j];
                    ptArray[j] = new GLatLng(tmp.x, tmp.y);
                }
    
                // Now that we have this delicious route, let's plot it easily!
                GPolyline routeLine = new GPolyline(ptArray);
                CMF.mPanel.theMap.addOverlay(routeLine);
                CMF.locPanel.addToOverlayMap(new Integer(locCount), routeLine);
                
                // And mark the beginning and ending nodes too
                markerEventMan = new GMarkerEventManager();
    
                addRouteMarker(true);
                
                // add stopover markers if necessary
                if (cafe != null){
                	addStopoverMarker(3);
                }
                if (lab != null){
                	addStopoverMarker(4);
                }
                if (library != null){
                	addStopoverMarker(5);
                }
                
                addRouteMarker(false);                      
    
                GWT.log("Just plotted a route with " + route.length + " points.", null);
            }
        }
        
        if (numSuccess < 1) {
            CMF.message("Sorry, we do not have any complete routes to draw for you... Please try again.");
            GWT.log("Route list came back with nothing but null paths?!", new Throwable());
            doThisBeforeReturn(true);
            return;
        }
        
        //average point of all the nodes of the route
        //GLatLng avgpoint = new GLatLng(totx/cnt , toty/cnt);
        GLatLng avgpoint = new GLatLng((leftbound+rightbound)/2,(downbound+upbound)/2);
        
        //      calculate sw, ne GLatLng objects and bounding box
        GLatLng sw = new GLatLng(leftbound - 0.1*(rightbound-leftbound), downbound- 0.1*(upbound-downbound));
        GLatLng ne = new GLatLng(rightbound + 0.1*(rightbound-leftbound), upbound+ 0.1*(upbound-downbound));
        GLatLngBounds box = new GLatLngBounds(sw,ne);
        
        //move to the average coordinate
        //CMF.mPanel.theMap.panTo(avgpoint);
                
        //setting the zoomlevel to be the level fitting the bound box
        CMF.mPanel.theMap.setZoom(CMF.mPanel.theMap.getBoundsZoomLevel(box));
        
        //move to the average coordinate
        CMF.mPanel.theMap.panTo(avgpoint);
        
        // Yay, we're finished!
        // Figure out what to say.
        CMF.message(genTimeAndMessage(
                totdistance,
                ((Boolean) CMF.oPanel.userOptions.get("bike")).booleanValue(),
                (numSuccess < pathList.size())));
        
        doThisBeforeReturn(true);
    }
    
	/**
	 * Always call this before returning in onSuccess or onFailure
	 */
	private void doThisBeforeReturn(boolean locsExist) {
		CMF.isMakingServerCall = false;
        CMF.stopSpin();
        if (locsExist) {
        	CMF.locPanel.ensurePanelConsistency();
        	CMF.locPanel.toggleSOCheckBoxes(true);
        }
	}
    
    /**
     * Figure out what to say to the user after routing is done.
     * @param totdistance Total distance of this route
     * @return What to say
     */
    private String genTimeAndMessage(double totdistance, boolean onBike, boolean missedPaths) {
        String timePhrase = "";
        
        // Get an estimate on time
        double tmptime = (onBike) ? 100*totdistance/BIKE_SPEED : 100*totdistance/WALK_SPEED;
        
        // To minutes? ("x.yz minutes")
        double floatminutes = tmptime * 0.01;
        
        if (floatminutes < 1) { // if less than 1 minutes display in seconds

            int seconds = (int) (floatminutes*60);
            timePhrase = (seconds == 1) ? seconds + " second." : seconds + " seconds.";
            
        } else if (floatminutes > 60) { // if more than 60 minutes, display in hours, mins
            
            int minutes = (int) floatminutes;
            int hours = minutes/60;
            minutes %= 60;
            
            timePhrase += (hours == 1) ? hours + " hour and " : hours + " hours and ";
            timePhrase += (minutes == 1) ? minutes + " minute." : minutes + " minutes.";
            
        } else {
            
            int minutes = (int) floatminutes;
            int seconds = (int) (60*(floatminutes - minutes));
            
            if (seconds >= 1) {
                timePhrase += (minutes == 1) ? minutes + " minute and " : minutes + " minutes and ";
                timePhrase += (seconds == 1) ? seconds + " second." : seconds + " seconds.";
            }
            else {
                timePhrase += (minutes == 1) ? minutes + " minute." : minutes + " minutes.";
            }
            
        }
        
        String builtMessage = "Your route is estimated to take " + timePhrase;
        return (missedPaths) ? builtMessage + " Some paths were unroutable." : builtMessage;
    }

    /**
     * Adds a marker at either the begin or end node of a route (based on isBeginNode).
     * The marker has a tooltip displaying its name and a clicklistener for
     * opening up an infoWindow.
     * 
     * @param isBeginNode
     */
    private void addRouteMarker(boolean isBeginNode) {
        GMarkerOptions options = new GMarkerOptions();
        GLatLng point;
        String name;

        if (isBeginNode) {
        	options.setIcon((locCount <= 10) ? CustomIcon.buildIcon("images/markers/icong" + locCount + ".png") : CustomIcon.GICON);
            options.setTitle(beginNode.name);
            point = new GLatLng(beginNode.x, beginNode.y);
            name = beginNode.name;
        }
        else {
        	locCount++;
            options.setIcon((locCount <= 10) ? CustomIcon.buildIcon("images/markers/iconr" + locCount + ".png") : CustomIcon.GICON);
            options.setTitle(endNode.name);
            point = new GLatLng(endNode.x, endNode.y);
            name = endNode.name;
        }

        GMarker marker = new GMarker(point, options);  
        MouseClickListener clickListener = new MouseClickListener();
        markerEventMan.addOnClickListener(marker, clickListener);
        markerEventMan.addOnDblClickListener(marker, clickListener);
        CMF.mPanel.theMap.addOverlay(marker);
        // Add to the HashMaps too
        CMF.locPanel.markNameMap.put(marker, name);
        CMF.locPanel.addToOverlayMap(new Integer((isBeginNode) ? locCount : locCount-1), marker);
        // Because begin and end markers have different locCounts
    }
    
    /**
     * adds GMarkers to the map indicating any stopovers if there are any
     * @param bcafe
     * @param blib
     * @param blab
     */
    private void addStopoverMarker(int type){
    	GMarkerOptions options = new GMarkerOptions();
    	GLatLng point;
    	String name = null;
    	GMarker marker = null;
    	if (type == 3){  // cafe stopover
    		options.setIcon(CustomIcon.FOOD_ICON);  //should change this to food_icon later
    		options.setTitle(cafe.name);
    		point = new GLatLng(cafe.x, cafe.y);
    		marker = new GMarker(point, options);  
    		name = cafe.name;
    	}else if (type == 4){  // lab stopover		
    		options.setIcon(CustomIcon.LAB_ICON);
    		options.setTitle(lab.name);
    		point = new GLatLng(lab.x, lab.y);
    		marker = new GMarker(point, options);
    		name = lab.name;
    	}else if (type == 5){ // library stopover   		
    		options.setIcon(CustomIcon.LIBRARY_ICON);
    		options.setTitle(library.name);
    		point = new GLatLng(library.x,library.y);
    		marker = new GMarker(point, options);
    		name = library.name;
    	}else{
    		// do nothing because type number does not correspond to any of the stopover types
            GWT.log("We were asked to add a stopover of type " + type, new Throwable());
    	}
    	MouseClickListener clickListener = new MouseClickListener();
        markerEventMan.addOnClickListener(marker, clickListener);
        markerEventMan.addOnDblClickListener(marker, clickListener);
        if (type == 3 || type == 4 || type == 5){
        	CMF.mPanel.theMap.addOverlay(marker);
        	// Add to the HashMap too
        	CMF.locPanel.markNameMap.put(marker, name);
            CMF.locPanel.addToOverlayMap(new Integer(locCount), marker);
        } 	
    }
    
    /**
     * Click on a route marker to change the flash object being displayed
     */
    private class MouseClickListener implements GMarkerEventClickListener {

        public void onClick(GMarker marker) {
            CMF.setSWF((String) (CMF.locPanel.markNameMap.get(marker)));
            
/**            GLatLng point = marker.getPoint();
            if (point.lat() == beginNode.x && point.lng() == beginNode.y) {
                // This is the begin node of the route.
                CMF.setSWF(beginNode.name);
            }
            if (point.lat() == endNode.x && point.lng() == endNode.y){
            	//This is the end node of the route.
            	CMF.setSWF(endNode.name);
            }
            if(cafe != null){ 
            	if (point.lat() == cafe.x && point.lng() == cafe.y){
            		CMF.setSWF(cafe.name);
            	}
            }
            if (lab != null){
            	if (point.lat() == lab.x && point.lng() == lab.y){
            		CMF.setSWF(lab.name);
            	}
            }
            if (library != null){
            	if (point.lat() == library.x && point.lng() == library.y){
            		CMF.setSWF(library.name);
            	}
            }
 **/	}
        

        public void onDblClick(GMarker marker) {
            CMF.mPanel.theMap.setCenter(marker.getPoint());
            CMF.mPanel.theMap.zoomIn();
        }

    }
}
