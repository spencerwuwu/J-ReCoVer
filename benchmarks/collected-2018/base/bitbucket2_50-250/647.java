// https://searchcode.com/api/result/42728624/

package server;

import java.util.ArrayList;

import client.player;

import utils.keyval_pair;
import utils.priority_queue;
import utils.utils;
import utils.vertex_key;

import database.unit_data;

public class unit {
	
	public int id;
	public unit_data me;
	public player co;
	public server world;
	
	public navmap los;
	
	protected boolean hasmoved;
	protected boolean hasaction;
	
	public int status;
	public int hp;
	public int supply;
	public int ammo;
		
	public unit (server whereami, player whostheboss, unit_data whoami, int whereintheworldx, int whereintheworldy) {
		
		world = whereami;
		co = whostheboss;
		me = whoami;
		los = new navmap (me.sight, me.sight, whereintheworldx, whereintheworldy, world.map_width());
		for (int i = 0; i < los.area(); i ++) los.navmap.add(new navcell());
		
		hasmoved = true;
		hasaction = true;
		
		hp = 100;
		supply = 100;
		
	}
	
	public unit() {
		hasmoved = true;
		hasaction = true;
		
		hp = 100;
		supply = 100;
	}

	public int whoami() {
		return me.id;
	}
	
	public void update_map (map newmap) {
		los.worldx = newmap.worldx;
		los.worldy = newmap.worldy;
		for (int i = 0; i < los.area(); i++) {
			los.set(i,newmap.get(i));
		} // for
		// create a new pathing
		pathing();
	}
	
	public void pathing() {
		
		// determine pathing
		// create a boolean array which indicates the cell has been walked before
		ArrayList <Boolean> walked = new ArrayList <Boolean> (los.area());
	
		// for safety, scan the line of sight for unwalkable terrain
		int walkable;
		for (walkable = 0; walkable < los.area(); walkable++) {		
			// see walking table for description
			// this means the cell is empty
			if (los.get(walkable).t == null) walked.add(true);
			else if (// this means that the terrain is unwalkable
				los.get(walkable).t.walking_cost < 1
				// this means that the target cell must have no unit in it, or it must be allied unit
				|| (los.get(walkable).u != null && los.get(walkable).u.co != this.co)
				) walked.add(true);
			else walked.add(false);
		} // for
			
		// create the priority queue
		priority_queue <vertex_key> prim_pq = new priority_queue <vertex_key> ();
		
		// set other variables (position and weight)
		int weight = 0;
		// unit position on los
		int xpos = los.width/2;
		int ypos = los.height/2;
		int pos = los.area()/2;

		// set the unit position to walked
		walked.set(utils.to_index (xpos, ypos, me.sight), true);
		walkable --; // remember that the position of the unit must be walkable, so reduce the number
		
		// set the cell where the unit is standing
		navcell temp = los.get(pos);
		temp.prevcell = pos;
		los.set(pos, temp);
		
		for (int i=0; i < walkable; i++) {
				
			// add the surrounding four neighbors
			// make sure the coordinates is not out of bound, then add
				
			if (ypos-1 > 0) { // check for north boundary
				int north = utils.to_index (xpos, ypos-1, this.me.sight); // one cell up
				// check for walkability
				if (!walked.get(north)) {
					walked.set(north, true);
					// enqueue new element
					prim_pq.add(new vertex_key (pos, north), weight+this.los.get(north).t.walking_cost);
				}
			} // north
				
			if (ypos+1 < this.me.sight) { // check for south boundary
				int south = utils.to_index (xpos, ypos+1, this.me.sight); // one cell down
				if (!walked.get(south)) {
					walked.set(south, true);
					// enqueue new element
					prim_pq.add(new vertex_key (pos, south), weight+this.los.get(south).t.walking_cost);
				}
			}// south
			
			if (xpos+1 < this.me.sight) { // check for east boundary
				int east = utils.to_index (xpos+1, ypos, this.me.sight); // one cell right
				if (!walked.get(east)) {
					walked.set(east, true);
					// enqueue new element
					prim_pq.add(new vertex_key (pos, east), weight+this.los.get(east).t.walking_cost);
				}
			} // east
				
			if (xpos-1 > 0) { // check for west boundary
				int west = utils.to_index (xpos-1, ypos, this.me.sight); // one cell left
				if (!walked.get(west)) {
					walked.set(west, true);
					// enqueue new element
					prim_pq.add(new vertex_key (pos, west), weight+this.los.get(west).t.walking_cost);
				}
			} // west
				
			// get the smallest one from the priority queue
			keyval_pair<vertex_key> next_step = new keyval_pair<vertex_key> (null, i);
			// check if the priority queue is empty
			if (prim_pq.is_empty()) break;
			next_step = prim_pq.pop();
			weight = next_step.val();
			// if the smallest available step is bigger than our unit's limit, stop the calculation
			if (weight > this.me.walk) break;
			// set the destination's previous path to source
			temp = los.get(next_step.key().dest()); // temp was declared before
			temp.prevcell = next_step.key().source();
			los.set (next_step.key().dest(), temp);
			
			// setup for our new rounds of search
			xpos = utils.getx(next_step.key().dest(), me.sight);
			ypos = utils.gety(next_step.key().dest(), me.sight);
			pos = utils.to_index (xpos, ypos, me.sight);
			
		} // for (pathing main algorithm)
	} // pathing

	public void update_pos (int worldx, int worldy) {
		los.worldx = worldx;
		los.worldy = worldy;
	}
	
	public int defend (int damage) {
		return hp-damage;
	} // defend	
	
} // unit

