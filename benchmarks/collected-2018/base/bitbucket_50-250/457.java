// https://searchcode.com/api/result/91874912/

/**
 *	Copyright (C) 2012 Marco A Asteriti
 *
 *	Permission is hereby granted, free of charge, to any person obtaining a copy 
 *	of this software and associated documentation files (the "Software"), to deal in 
 *	the Software without restriction, including without limitation the rights to use, 
 *	copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 *	the Software, and to permit persons to whom the Software is furnished to do so, 
 *	subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in all copies 
 *  or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR 
 *  A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF 
 *  OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.masteriti.geometry.dcel.voronoi;


import com.masteriti.geometry.Calc;
import com.masteriti.geometry.ParabolicArc;
import com.masteriti.geometry.Vector2d;
import com.masteriti.geometry.dcel.Cell;
import com.masteriti.geometry.dcel.HalfEdge;


public class VoronoiNodeData implements Comparable<VoronoiNodeData> {
	private Cell main;		// face containing point representing this site
	private Cell pair;		// second point in tuple if representing break point (internal nodes only)
	private HalfEdge edge;	// half edge traced by breakpoint
	
	// possible circle event associated with this arc 
	// (i.e. this is the arc that disappears if circle event is valid) 
	private CircleEvent circleEvent; 	
	
	
	// Constructors
	public VoronoiNodeData(Cell site) {
		main = site;
		pair = null;
		edge = null;
				
	}
	
	public VoronoiNodeData(Cell left, Cell right) {
		main = left;
		pair = right;
		edge = new HalfEdge(main);
        Vector2d dir = new Vector2d(left.getOriginSite(), right.getOriginSite());
        edge.setDirection(dir.leftNormal().normalized());
        left.setOuterComponentEdge(edge);
	}
	
	/**
	 * Creates a new node by copying another node.
	 * @param data
	 */
	public VoronoiNodeData(VoronoiNodeData data) {
		this.main = data.main;
		this.pair = data.pair;
		this.edge = data.edge;
		this.circleEvent = data.circleEvent;
	}

	// returns the x coordinate value of the focus point for an arc
	// in the case of a leaf node, or of breakpoint in the case of
	// an internal node.
	public double getComparableValue() {
		if(pair == null) {
			return main.getOriginSite().x;
		} else {
			return getBreakPointX(FortuneBST.sweepline, main, pair);
		}
	}

	private double getBreakPointX(double sweepline, Cell mainFocus, Cell pairFocus) {
		// find the x coordinate value of the break point given by the intersection of two
		// parabolas.
		
		// Special case: one of the two sites is exactly on the directrix (i.e. in the 
		// case two sites in the set share the exact same y coordinate.	
		if ( (sweepline == mainFocus.getOriginSite().y) || (sweepline == pairFocus.getOriginSite().y) ) {
				// check for the unlikely event both are on the same y axis
			if ((sweepline == mainFocus.getOriginSite().y) && (sweepline == pairFocus.getOriginSite().y) ) {
				// the breakpoint will be tracing a vertical line exactly in between the two foci once directrix sweeps below them.
				return (mainFocus.getOriginSite().x+pairFocus.getOriginSite().x) / 2;
			} 
			// otherwise find which of the two was on the directrix and the break point will coincide with that sites' x coordinate value.
			else if (sweepline == mainFocus.getOriginSite().y ) {
				return mainFocus.getOriginSite().x;
			}
			else 
				return pairFocus.getOriginSite().x;
		}
		
		// instantiate two arcs represented by the respective focus points and directrix.
		ParabolicArc leftArc, rightArc;
		leftArc = new ParabolicArc(sweepline, mainFocus.getOriginSite().x, mainFocus.getOriginSite().y);
		rightArc = new ParabolicArc(sweepline, pairFocus.getOriginSite().x, pairFocus.getOriginSite().y);
		
		// Reduce the intersection of two arcs in quadratic form to solve for x
		double a, b, c, D;
		a = leftArc.getA() - rightArc.getA();
		b = leftArc.getB() - rightArc.getB();
		c = leftArc.getC() - rightArc.getC();
		D = 0.0;
		
		// Evaluate the discriminant to see how many roots there are
		D = ((b*b) - (4*a*c));
		if(0.0 == D) {
			// There is only one root.  Parabolas are tangent.
			// This can only be possible if foci are equal or a parabola is
			// flipped upside-down ERROR.
			return ((0.0 - b) / (2*a));
		} else if(D > 0.0) {
          if((Math.abs(a) < Calc.EPSILON) && (Math.abs(b) > Calc.EPSILON)) {
            return -c/b;
          }
			// there are two solutions (Expected Case), 
			// solve with quadratic formula for each root.
			double sqrtD = Math.sqrt(D);
			
			// But first. determine which breakpoint we're looking at relative to the arc pair.
			// First determine which sits on top.
			//double order = mainFocus.getOriginSite().y - pairFocus.getOriginSite().y;
			
			// negative order indicates we're dealing with the right break point, positive means the left break point
			
			double result =  ((0.0 - b) + sqrtD) / (2*a);
			return result;

						
		} else {
			// TODO Debug code: negative discriminant!
			return 0;
		}
	}

	// Getters and setters
	public HalfEdge getEdge() {
		return edge;
	}

	public void setEdge(HalfEdge edge) {
		this.edge = edge;
	}

	public CircleEvent getCircleEvent() {
		return circleEvent;
	}
	
	public void setCircleEvent(CircleEvent circleEvent) {
		this.circleEvent = circleEvent;
	}

	public Cell getMain() {
		// TODO Auto-generated method stub
		return main;
	}

	public void setPair(Cell pair) {
		this.pair = pair;
		
	}

	public Cell getPair() {
		// TODO Auto-generated method stub
		return pair;
	}

	public void setMain(Cell main) {
		// TODO Auto-generated method stub
		this.main = main;
		
	}

	@Override
//	public int compareTo(VoronoiNode otherNode) {
//		double thisX = this.getComparableValue();
//		double otherX = otherNode.getComparableValue();
//		if(thisX < otherX)
//			return 1;
//		else
//			return -1;
//	}
    public int compareTo(VoronoiNodeData other) {
      double thisX = this.getComparableValue();
      double otherX = other.getComparableValue();
      thisX = otherX - thisX;
      if(Math.abs(thisX) < Calc.EPSILON) {
        return 0;
      } else if( thisX < 0) {
        return -1;
      } else {
        return 1;
      }
    }
}


