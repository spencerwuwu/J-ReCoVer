// https://searchcode.com/api/result/2186141/

/**
 * 
 */
package uk.ac.lkl.expresser.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import uk.ac.lkl.common.util.Location;
import uk.ac.lkl.migen.system.MiGenConfiguration;
import uk.ac.lkl.migen.system.expresser.model.ModelColor;

import com.google.gwt.user.client.Timer;

/**
 * Implements a fade from the previous view to the current one.
 * 
 * @author Ken Kahn
 *
 */
public class Fader extends Timer {
    
    private ExpresserCanvas canvas;
    private Collection<TileViewSnapShot> previousTileSnapshots;
    private long startTime;
    private double fadeDuration;
    
    public Fader(ExpresserCanvas canvas, 
	         List<TileView> allTileViews, 
	         HashMap<Location, TileViewSnapShot> currentTileSnapshots) {
	super();
	this.canvas = canvas;
	fadeDuration = MiGenConfiguration.getTiedNumberPlayDelay();
	// reduce by 10% so this process doesn't fall behind the animation process
	fadeDuration -= fadeDuration/10; 
	previousTileSnapshots = new ArrayList<TileViewSnapShot>();
	// ensure that the snapshots are on top
	ExpresserCanvasPanel expresserCanvasPanel = canvas.getExpresserCanvasPanel();
	int canvasLeft = expresserCanvasPanel.getAbsoluteLeft();
	int canvasTop = expresserCanvasPanel.getAbsoluteTop();
	for (Entry<Location, TileViewSnapShot> snapshotEntry : currentTileSnapshots.entrySet()) {
	    Location location = snapshotEntry.getKey();
	    TileViewSnapShot snapshot = snapshotEntry.getValue();
	    expresserCanvasPanel.add(snapshot, location.x, location.y);
	    previousTileSnapshots.add(snapshot);
	}
	for (TileView tileView : allTileViews) {
	    int left = tileView.getAbsoluteLeft()-canvasLeft;
	    int top = tileView.getAbsoluteTop()-canvasTop;
	    if (currentTileSnapshots.get(new Location(left, top)) == null) {
		// is a tile that wasn't there previously so add it as a snapshot on a blank canvas
		// this code assumes the canvas background color is White.
		TileViewSnapShot snapshot = new TileViewSnapShot(ModelColor.WHITE, tileView.getOffsetWidth(), "expresser-invisible-tile");
		expresserCanvasPanel.add(snapshot, left, top);
//		Utilities.warn("Added " + snapshot.toString() + " inside Fader constructor.");
		previousTileSnapshots.add(snapshot);
	    }
	}
	startTime = System.currentTimeMillis();
//	Utilities.warn("Fader started " + startTime);
	scheduleRepeating(20);
    }

    @Override
    public void run() {
	long now = System.currentTimeMillis();
	if (now >= startTime+fadeDuration) {
	    stop();
	    return;
	}
	double opacity = 1.0-(now-startTime)/fadeDuration;
	for (TileViewSnapShot snapshot : previousTileSnapshots) {
	    snapshot.setOpacity(opacity);
	}
//	Utilities.warn("opacity: " + opacity + " snapshot count: " + previousTileSnapshots.size() + " time: " + now);
    }

    public void stop() {
//	Utilities.warn("Fader stopped " + System.currentTimeMillis());
	ExpresserCanvasPanel expresserCanvasPanel = canvas.getExpresserCanvasPanel();
	for (TileViewSnapShot snapshot : previousTileSnapshots) {    
	    expresserCanvasPanel.remove(snapshot);
//	    Utilities.warn("Removed " + snapshot.toString() + " inside stop.");
	}
//	expresserCanvasPanel.removeAllSnaphots();
//	for (TileView tileView : allTileViews) {
//	    tileView.setOpacity(1.0);
//	}
	canvas.setFader(null);
	cancel();
    }


}

