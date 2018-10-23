// https://searchcode.com/api/result/11587862/

package edu.calpoly;
/**
 * 
 * @author Allen Dunlea
 * 
 * Represents the board
 * 1 based
 *
 */
 
import java.util.*;
 
public class Board{
	private static final int STDBRD = 9;
	private Stone[][] brd;
	private int brdsize;
	private int blackCapd;
	private int whiteCapd;
	private HashSet<Group> groups;
	private Janitor janitor;
	
	public Board() {
		this(STDBRD);
	}
	
	public Board(int brdsize) {
		this.brdsize = brdsize;
		clearBoard();
	}
	
	/**
	 * Initializes the board to empty
	 */
	public void clearBoard() {
		brd = new Stone[brdsize][brdsize];
		groups = new HashSet<Group>();
		janitor = new Janitor();
		blackCapd = whiteCapd = 0;
		for(int i = 0; i < brdsize; i++) {
			for(int j = 0; j < brdsize; j++) {
				brd[i][j] = new Stone(i, j);
			}
		}
		setStoneSides();
	}
	
	/**
	 * Places a stone on the board
	 * @param color of the stone
	 * @param vertex of the stone
       * @return number of stones captured (-1 if vertex is not legal)
	 */
	public int placeStone(String color, String vertex){
		return placeStone(color, getX(vertex), getY(vertex));
	}
	
	public int placeStone(String color, int x, int y) {

		Stone current = brd[x][y];
		current.setColor(color);
		
		// Check to see if this piece will be part of a group
		// Also check to see if it has stock number of open sides
		// Update accordingly.
		Stone[] nesw = new Stone[4];
		
		nesw[0] = (y+1 < brdsize) ? brd[x][y+1] : null; // North
		nesw[2] = (x+1 < brdsize) ? brd[x+1][y] : null; // East
		nesw[1] = (y-1 >= 0)      ? brd[x][y-1] : null; // South
		nesw[3] = (x-1 >= 0)      ? brd[x-1][y] : null; // West
		
		//The group the stone belongs to.
		Group curGrp = null;
		
		//Check the sides of the stone.
		for(int i = 0; i < 4; i++){
			//Is it empty?
			if(nesw[i] == null || nesw[i].isEmpty())	continue;
			
			// Reduce the liberties of each stone.
		      nesw[i].subSides();

		    //Check for Groups if we have to merge groups
		    Group found = null;
			// Get this stone's group
			for(Group c: groups){
		      if (c.contains(nesw[i]))
		    	  found = c;
			}
			
			//Are these stones the same color?
			if(current.getState() == nesw[i].getState()){				
				if(curGrp != null){
					curGrp.addAll(found);// Merge the Groups
					groups.remove(found);// Remove the Group that was merged in.
				} else{
					found.add(current);
					curGrp = found;
				}				
				curGrp.updateSides();
			
			} else {
				found.updateSides();
			}
		}
		
		//This Stone did not connect to other groups 
		// Make a group for it to join by itself
		if (curGrp == null){
		   curGrp = new Group(current);
		   groups.add(curGrp);
		}
		
	     curGrp.updateSides();
		
	     return janitor.cleanup(curGrp);
	}
	
	/**
	 * Gets the stone at the given vertex
	 * @param vertex of the form "A1"
	 * @return the stone
	 */
	public Stone getStone(String vertex) {
		return brd[getX(vertex)][getY(vertex)];
	}
	
	public Stone getStone(int x, int y) {
		if(x < 0 || y < 0 || x >= brdsize || y >= brdsize)
			return null;
		return brd[x][y];
	}
	
	/**
	 * Returns the x of the string vertex
	 * (i.e. B4 would return 2)
	 * @param vertex of the form C5 
	 * @return the x of the given vertex
	 */
	private int getX(String vertex) {
		char c = Character.toLowerCase(vertex.charAt(0));
		if(c > 'i') 
			c--;
		return c - 'a';
	}
	/**
	 * Returns the y of the string vertex
	 * (i.e. B4 would return 4)
	 * @param vertex of the form C5
	 * @return the y of the given vertex
	 */
	private int getY(String vertex) {
		
		return vertex.charAt(1) - '1';
	}
	
	/**
	 * 
	 * @param vertex
	 * @return true if the vertex is empty
	 */
	public boolean isEmpty(String vertex) {
		return brd[getX(vertex)][getY(vertex)].isEmpty();
	}
	
	public boolean isEmpty(int x, int y) {
		return brd[x][y].isEmpty();
	}
	/**
	 * 
	 * @param vertex
	 * @return true if the vertex is legal
	 */
	public boolean isLegal(String vertex) {
		return brd[getX(vertex)][getY(vertex)].isLegal();
	}
	
	public boolean isLegal(int x, int y) {
		return brd[x][y].isLegal();
	}
	
	/**
	 * Set whether a stone is legal or not
	 * @param vertex of the stone
	 * @param isLegal whether or not the stone is a legal vertex to play on
	 */
	public void setLegal(String vertex, boolean isLegal) {
		brd[getX(vertex)][getY(vertex)].setLegal(isLegal);
	}
	
	public void setLegal(int x, int y, boolean isLegal) {
		brd[x-1][y-1].setLegal(isLegal);
	}
	
	/**
	 * Returns the state at the given vertex.
	 * Stone.EMPTY, Stone.BLACK, or Stone.WHITE
	 * @param vertex
	 * @return state at the given vertex
	 */
	public int stateAt(String vertex) {
		return brd[getX(vertex)][getY(vertex)].getState();
	}
	
	public int stateAt(int x, int y ) {
		return brd[x-1][y-1].getState();
	}
	
	public String toString() {
		StringBuilder ret = new StringBuilder();
		
		for(int i = brdsize - 1; i >= 0; i--) {
			for(int j = 0; j < brdsize; j++) {
				ret.append(" " + brd[j][i]);
			}
			ret.append("\n");
		}
		return ret.toString();
	}
	
	/**
	 * @return a deep copy of the board
	 */
	public Board clone() {
		Board newb = new Board(this.brdsize);
		String color;
		
		for(int i = 0; i < newb.brdsize; i++) {
			for(int j = 0; j < newb.brdsize; j++) {
				color = brd[i][j].toString();
				if(color.compareTo(".") != 0) {
					newb.placeStone(color, i, j); 
				}
			}
		}
		return newb;
	}
	
	public boolean equals(Board newb){
		Stone[][] other = newb.getBoard();
		String color;
		// Check to see if Boards match.
		for(int i = 0; i < newb.brdsize; i++) {
			for(int j = 0; j < newb.brdsize; j++) {
				color = brd[i][j].toString();
				if(color.compareTo(other[i][j].toString()) != 0) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @return the array that represents the board
	 */
	public Stone[][] getBoard() {
		return brd;
	}
	
	/**
	 *
	 * @return the number of captured black stones.
	 */
	public int getCappedBlacks(){
	   return blackCapd;
	}
	
	/**
	 *
	 * @return the number of captured white stones.
	 */
	public int getCappedWhites(){
	   return whiteCapd;
	}
	
	/**
	 * 
	 * @return a hashset of all the empty vertices on the board
	 */
	public HashSet<Stone> emptyVertices() {
		HashSet<Stone> noMansLand = new HashSet<Stone>();
		
		for(Stone [] row : brd) {
			for(Stone s : row) {
				if(s.isEmpty())
					noMansLand.add(s);
			}
		}
		return noMansLand;
	}
	
	public int getBrdsize() {
		return brdsize;
	}
	
	private void setStoneSides(){
		for(int x = 0; x < brdsize; x++){
			for(int y = 0; y < brdsize; y++){
			Stone[] nesw = new Stone[4];
			
			nesw[0] = (y+1 < brdsize) ? brd[x][y+1] : null; // North
			nesw[2] = (x+1 < brdsize) ? brd[x+1][y] : null; // East
			nesw[1] = (y-1 >= 0)      ? brd[x][y-1] : null; // South
			nesw[3] = (x-1 >= 0)      ? brd[x-1][y] : null; // West
			brd[x][y].setNeighbors(nesw);
			}
		}
	}
	
	// functions to help me try to debug
   public HashSet<Group> getGroups(){
      return groups;
   }
	
	private class Janitor{
	   public Janitor(){
	      // Nothing atm.
	   }
	   
       /**
        * 
        * @param move group that the most recent move belongs to
        * @return number of stones captured
        */
	   public int cleanup(Group move){
		   int frags = 0;
		   HashSet<Group> newList = new HashSet<Group>();
		   
	      for(Group current: groups){
	    	  // We'll check this later, so we don't prematurely remove it.
	    	  if(current == move)
	    		  continue;
	    	  
	         if ( current.getSides() == 0){
		        // Add to list of groups no longer in use.
	            for(Stone stone: current){
                    if (stone.getState() == Stone.WHITE){
	                  whiteCapd++;
	               }
	               else{
	                  blackCapd++;
	               }
	               stone.resetStone();
	               frags++;

	            }
	         } else{
	        	 newList.add(current);
	         }
	      }
	      
	      groups = newList;
	      for(Group group: groups)
	    	  group.updateSides();
	      
	      //Did we make a suicide move?
	      move.updateSides();
	      if(move.getSides() == 0)
	      {
	    	  // Add to list of groups no longer in use.
	            for(Stone stone: move){
                     if (stone.getState() == Stone.WHITE){
                        whiteCapd++;
                     }
                     else{
                        blackCapd++;
                     }
		               stone.resetStone();
		               frags--;
                     
		            }
	    	  
	      } else{
	    	  groups.add(move);
	      }
	      return frags;
	   }
	}
}
