// https://searchcode.com/api/result/50149545/

/*******************************************************************************
 * Copyright (c) 2013 Max Gbel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Max Gbel - initial API and implementation
 ******************************************************************************/
package at.tuwien.prip.model.utils;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import at.tuwien.prip.model.document.RectangleAdjustment;
import at.tuwien.prip.model.document.segments.GenericSegment;
import at.tuwien.prip.model.document.segments.TextSegment;
import at.tuwien.prip.model.document.semantics.SemanticText;
import at.tuwien.prip.model.document.semantics.WordSemantics;
import at.tuwien.prip.model.graph.DocEdge;
import at.tuwien.prip.model.graph.DocNode;
import at.tuwien.prip.model.graph.DocumentGraph;
import at.tuwien.prip.model.graph.DocumentMatrix;
import at.tuwien.prip.model.graph.DocumentMatrix.DM_Iterator;
import at.tuwien.prip.model.graph.EdgeConstants;
import at.tuwien.prip.model.graph.ISegmentGraph;
import at.tuwien.prip.model.graph.comparators.XDocNodeComparator;
import at.tuwien.prip.model.graph.comparators.YDocNodeComparator;
import at.tuwien.prip.model.graph.hier.level.SegLevelGraph;


/**
 * DocGraphUtils.java
 * 
 *
 *
 * @author mcg <mcgoebel@gmail.com>
 * Nov 2, 2011
 */
public class DocGraphUtils 
{
	
	/**
	 * 
	 * @param b
	 * @param annoAdj
	 * @param graph
	 * @return
	 */
	public static Rectangle applyRectangleAdjustment (Rectangle b, RectangleAdjustment annoAdj, 
			ISegmentGraph graph)
	{
		double adjXScale = annoAdj.getXScale();
		if (adjXScale==0) {
			adjXScale=1;
		}
		
		double adjYScale = annoAdj.getYScale();
		if (adjYScale==0) {
			adjYScale=1;
		}

		double xScale = Math.abs(adjXScale);
		double yScale = Math.abs(adjYScale);
		
		float x1 = (float) (((b.x + annoAdj.getxOffset()) * 1) * xScale);
		float x2 = (float) (((b.x + b.width + annoAdj.getxOffset()) * 1) * xScale);
		float y1 = (float) (((b.y + annoAdj.getyOffset()) * 1) * yScale);
		float y2 = (float) (((b.y + b.height + annoAdj.getyOffset()) * 1) * yScale);
	
		java.awt.Rectangle rectangle = new java.awt.Rectangle((int)x1,(int)y1,(int)(x2-x1),(int)(y2-y1));
		if (adjXScale<0)
		{
			//flip horizontal
			rectangle = DocGraphUtils.xFlipRectangle(rectangle, graph);
		}
		if (adjYScale<0)
		{
			//flip vertical
			rectangle = DocGraphUtils.yFlipRectangle(rectangle, graph);
		}
		
		return rectangle;
	}

	/**
	 * 
	 * @param in
	 * @return
	 */
	public static ISegmentGraph generateSemanticGraph (ISegmentGraph in) 
	{
		ISegmentGraph reduction = reduceToSemanticGraph(in);

		/* the nodes */
		Map<DocNode,DocNode> nodeMap = new HashMap<DocNode,DocNode>();
		List<DocNode> nodes = new ArrayList<DocNode>();
		for (DocNode node : reduction.getNodes())
		{
			StringBuffer sb = new StringBuffer();
			List<SemanticText> semTextList = node.getSemanticAnnotations();
			for (SemanticText st : semTextList) 
			{
				for (WordSemantics sem : st.getSemantics())
				{
					sb.append(sem.name());
					sb.append(",");
				}
			}
			String text = sb.toString().substring(0, sb.length()-1) + ":\n" + node.getSegText();
			Rectangle2D bbox = node.getBoundingBox();
			TextSegment ts = new TextSegment(bbox.getBounds(), text);
			DocNode semNode = new DocNode(ts);
			semNode.setSemanticAnnotations(node.getSemanticAnnotations());
			nodes.add(semNode);
			nodeMap.put(node,semNode); //remember
		}

		/* the edges */
		List<DocEdge> edges = new ArrayList<DocEdge>();
		for (DocEdge edge : reduction.getEdges()) 
		{
			DocNode from = nodeMap.get(edge.getFrom());
			DocNode to = nodeMap.get(edge.getTo());
			if (from==null || to==null) 
				continue; //something fishy here...
			DocEdge semEdge = new DocEdge(from, to);
			edges.add(semEdge);
		}

		DocumentGraph out = new DocumentGraph();
		out.setNodes(nodes);
		out.setEdges(edges);
		return out;
	}

	/**
	 * Integrate a list of generic segments into an existing
	 * segment graph. Returns the input graph extended by the
	 * given new segments.
	 * 
	 * @param g
	 * @param segments
	 * @return
	 */
	public static ISegmentGraph union (ISegmentGraph g, 
			List<? extends GenericSegment> segments)
	{
		List<DocNode> nodes = new ArrayList<DocNode>();
		for(GenericSegment gs : segments)
		{
			nodes.add(new DocNode(gs));
		}
		g.addNodes(nodes);

		return g;
	}

	/**
	 * Reduce a graph to only those nodes that have a semantic
	 * annotation associated with them.
	 * 
	 * @param in
	 * @return
	 */
	public static ISegmentGraph reduceToSemanticGraph (ISegmentGraph in) 
	{	
		/* the nodes */
		List<DocNode> removeNodes = new ArrayList<DocNode>();
		List<DocNode> nodes = new ArrayList<DocNode>();
		for (DocNode node : in.getNodes()) 
		{
			List<SemanticText> annotations = node.getSemanticAnnotations();
			if (annotations==null || annotations.size()==0) 
			{
				removeNodes.add(node);
			}
			else 
			{
				nodes.add(node);
			}
		}

		/* the edges */
		List<DocEdge> edges = new ArrayList<DocEdge>();
		List<DocEdge> needChecking = new ArrayList<DocEdge>();
		for (DocEdge edge : in.getEdges()) 
		{
			if (nodes.contains(edge.getFrom()))
			{
				if (nodes.contains(edge.getTo()))
				{
					edges.add(edge); //both
				}
				else 
				{
					needChecking.add(edge); //only from 
				}
			}
			else 
			{
				if (nodes.contains(edge.getTo()))
				{
					needChecking.add(edge); //only to
				}
			}
		}
		for (DocEdge edge : needChecking)
		{
			if (isReachable(edge.getFrom(), edge.getTo(), in, removeNodes))
			{
				edges.add(edge);
			}
		}

		DocumentGraph out = new DocumentGraph();
		out.setNodes(nodes);
		out.setEdges(edges);
		return out;
	}

	/**
	 * Extract a random subgraph.
	 * @param dg
	 * @return
	 */
	public static ISegmentGraph getRandomSubgraph (ISegmentGraph dg)
	{
		Rectangle dgDim = dg.getDimensions();
		Random randomGenerator = new Random();

		int x = randomGenerator.nextInt(dgDim.width - 200);
		int y = randomGenerator.nextInt(dgDim.height - 200);
		int w = randomGenerator.nextInt(dgDim.width-x);
		int h = randomGenerator.nextInt(dgDim.height-y);

		Rectangle r = new Rectangle(x, y, w, h);

		return getDocumentSubgraphUnderRectangle(dg, r, true);
	}

	/**
	 * 
	 * @param dg
	 * @param bBox
	 * @return
	 */
	public static ISegmentGraph getDocumentSubgraphUnderRectangle(
			ISegmentGraph dg, Rectangle r, boolean edges) 
	{
		if (dg==null || r==null)
			return null;
		
		GenericSegment segment = new GenericSegment(r.x, r.x+r.width, r.y, r.y+r.height);
		List<DocNode> subNodes = getNodesWithIntersectingCentres(dg, segment);
		return dg.getSubGraph(subNodes, edges);
	}

	/**
	 * 
	 * @param dg
	 * @param bBox
	 * @return
	 */
	private static List<DocNode> getNodesWithIntersectingCentres(
			ISegmentGraph dg, GenericSegment bBox)
			{
		ArrayList<DocNode> retVal = new ArrayList<DocNode>();
		for (Object o : dg.getNodes())
		{
			DocNode n = (DocNode)o;
			if (SegmentUtils.horizIntersect(bBox, n.toGenericSegment().getXmid()) &&
					SegmentUtils.vertIntersect(bBox, n.toGenericSegment().getYmid()))
				retVal.add(n);
		}
		return retVal;
			}

	/**
	 *  Checks if node 'to' is reachable from node 'from' using 
	 *  the given 'via' nodes.
	 *  
	 * @param from
	 * @param to
	 * @param dg
	 * @param via
	 * @return
	 */
	public static boolean isReachable (
			DocNode from,
			DocNode to,
			ISegmentGraph dg,
			List<DocNode> via) 
	{
		if (from.equals(to)) 
		{ 
			return true;  //base case
		}
		for (DocEdge edge : dg.getEdgesFrom(from)) 
		{
			if (via.contains(edge.getTo())) 
			{
				via.remove(edge.getTo()); //avoid cycles
				if (!isReachable(edge.getTo(), to, dg, via)) 
				{
					continue;
				}
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if node 'to' is reachable from node 'from' in the 
	 * given 'orientation'.
	 * 
	 * @param from
	 * @param to
	 * @param dg
	 * @return
	 */
	public static boolean isReachable (
			DocNode from, 
			DocNode to, 
			DocumentGraph dg, 
			Orientation orientation) 
	{
		if (from.equals(to)) 
		{ 
			return true;  //base case
		}
		for (DocEdge edge : dg.getEdgesFrom(from)) 
		{
			if (edge.getOrientation()==orientation) 
			{
				if (!isReachable(edge.getTo(), to, dg, orientation)) 
				{
					continue;
				}
				return true;
			}
		}

		return false;
	}

	public static String getTextUnderRegion (Rectangle rect, ISegmentGraph g)
	{
		ISegmentGraph sub = DocGraphUtils.getDocumentSubgraphUnderRectangle(g, rect, false);
		DocGraphUtils.flipReverseDocGraph(sub);
		
		// line finding
		StringBuffer sb = new StringBuffer();
		DocNode prev = null, curr = null;
		DocumentMatrix dm = DocumentMatrix.newInstance(sub,	true);
		DM_Iterator iter = dm.iterator();
		while (iter.hasNext()) 
		{
			curr = iter.next();
			double currFontSize = 12d;
			if (curr.getFont()!=null)
				curr.getFont().getSize();

			if (prev != null)
			{
				if (prev.getSegY2() - 2 < curr.getSegY1()) 
				{
					sb.append("\n");
				}
				else if ((curr.getSegX1() - prev.getSegX2()) > 0.18 * currFontSize)
				{
					sb.append(" ");
				}
			}
			sb.append(curr.getSegText());
			prev = curr;
		}
		return sb.toString();
	}

	/**
	 * Document graphs are inverted for GUI presentation. 
	 * Revert back to original...
	 * 
	 * @param g
	 */
	public static Rectangle yFlipRectangle (Rectangle rect, ISegmentGraph g) 
	{
		java.awt.Rectangle dim = g.getDimensions();
		int y2 = dim.y + dim.height;

		int a = (int) Math.abs(y2 - rect.getMaxY());

		return new Rectangle(rect.x, a, rect.width, rect.height);
	}
	
	/**
	 * Document graphs are inverted for GUI presentation. 
	 * Revert back to original...
	 * 
	 * @param g
	 */
	public static Rectangle xFlipRectangle (Rectangle rect, ISegmentGraph g) 
	{
		java.awt.Rectangle dim = g.getDimensions();
		int x2 = dim.x + dim.width;

		int a = (int) Math.abs(x2 - rect.getMaxX());

		return new Rectangle(a, rect.y, rect.width, rect.height);
	}

	/**
	 * Document graphs are inverted for GUI presentation. 
	 * Revert back to original...
	 * 
	 * @param g
	 */
	public static void flipReverseDocGraph (ISegmentGraph g) 
	{
		java.awt.Rectangle r = g.getDimensions();
		int y2 = r.y + r.height;
		//		int x2 = r.x + r.width;

		for (DocNode node : g.getNodes()) {
			float a = Math.abs(y2 - node.getSegY2());
			float b = Math.abs(y2 - node.getSegY1());
			//			float c = Math.abs(x2 - node.getSegX2());
			//			float d = Math.abs(x2 - node.getSegX1());
			node.setSegY1(a);
			node.setSegY2(b);
			//			node.setSegX1(c);
			//			node.setSegX2(d);
		}
	}

	/**
	 * Return the top-left-most node in a document graph.
	 * @param dg
	 * @return
	 */
	protected static DocNode getSeedNode (DocumentGraph dg) 
	{
		//find top left most node as start position for algorithm...
		List<DocNode> dNodes = dg.getNodes();

		DocNode start = getTopMostDocNode(dNodes);
		boolean done = false;
		while (!done) {
			for (DocEdge e : dg.getEdgesFrom(start)) {
				DocNode other = e.getFrom().equals(start)?e.getTo():e.getFrom();
				if ( (other.getSegX1()<start.getSegX1() && 
						other.getSegY1()>= start.getSegY1()) ) {
					start = other;
					break;
				}
			}
			done = true;
		}
		return start;
	}

	//	protected static StringTokenSequence toPrincipledOrder (List<DocNode> nodes) 
	//	{
	//		StringTokenSequence result = new StringTokenSequence();
	//		
	//		//traverse via matrix iterator
	//		DocumentMatrix dm = DocumentMatrix.newInstance(nodes);
	//		Iterator<DocNode> iter = dm.iterator();
	//		DocNode node = null;
	//		while (iter.hasNext()) {
	//			
	//			node = iter.next();
	//			if (node!=null) { 
	//				
	//				result.add(new StringToken(node.getLabel()));
	//			}
	//		}
	//		return result;
	//	}
	//	
	public static DocNode getLeftMostDocNode(List<DocNode> l)
	{
		List<DocNode> sortedList = new ArrayList<DocNode>();
		for (DocNode gs : l)
			sortedList.add(gs);
		Collections.sort(l, new XDocNodeComparator());
		return l.get(0);
	}

	public static DocNode getRightMostDocNode(List<DocNode> l)
	{
		List<DocNode> sortedList = new ArrayList<DocNode>();
		for (DocNode gs : l)
			sortedList.add(gs);
		Collections.sort(l, new XDocNodeComparator());
		return l.get(l.size() - 1);
	}

	public static DocNode getNeighbourRight (ISegmentGraph g, DocNode n) 
	{
		List<DocEdge> edges = g.getEdgesFrom(n);
		for (DocEdge e : edges) 
		{
			if (e.getRelation()==EdgeConstants.ADJ_RIGHT) {
				return e.getTo();
			}
		}
		return null;
	}

	public static DocNode getNeighbourRightHier (SegLevelGraph g, DocNode n) 
	{
		DocNode right = getNeighbourRight((ISegmentGraph) g, n);
		if (right==null)
		{
			List<DocNode> lower = g.expandNode(n);
			if (lower!=null && lower.size()>0) {
				for (DocNode node : lower) {
					right = getNeighbourRight(g, node);
					if(right!=null && !lower.contains(right)) {
						break;
					}
				}
			}
		}
		return right;
	}

	public static DocNode getNeighbourLeft (ISegmentGraph g, DocNode n) 
	{
		List<DocEdge> edges = g.getEdgesTo(n);
		for (DocEdge e : edges) 
		{
			if (e.getRelation()==EdgeConstants.ADJ_RIGHT) {
				return e.getFrom();
			}
		}
		return null;
	}

	public static DocNode getNeighbourTop (ISegmentGraph g, DocNode n) 
	{
		List<DocEdge> edges = g.getEdgesTo(n);
		for (DocEdge e : edges) 
		{
			if (e.getRelation()==EdgeConstants.ADJ_BELOW) {
				return e.getTo();
			}
		}
		return null;
	}

	public static DocNode getNeighbourBottom (ISegmentGraph g, DocNode n) 
	{
		List<DocEdge> edges = g.getEdgesFrom(n);
		for (DocEdge e : edges) 
		{
			if (e.getRelation()==EdgeConstants.ADJ_ABOVE) {
				return e.getTo();
			}
		}
		return null;
	}

	public static DocNode getTopMostDocNode(List<DocNode> l)
	{
		List<DocNode> sortedList = new ArrayList<DocNode>();
		for (DocNode gs : l) {
			sortedList.add(gs);
		}

		Collections.sort(l, new YDocNodeComparator());
		return l.get(0);
	}

	public static DocNode getBottomMostDocNode(List<DocNode> l)
	{
		List<DocNode> sortedList = new ArrayList<DocNode>();
		for (DocNode gs : l)
			sortedList.add(gs);
		Collections.sort(l, new YDocNodeComparator());
		return l.get(l.size() - 1);
	}


	public static boolean intersects(DocNode n1, DocNode n2) {
		return vertIntersect(n1, n2) && horizIntersect(n1, n2);
	}

	public static boolean vertIntersect(DocNode n1, DocNode n2)
	{
		return (n1.getSegX1() >= n2.getSegY1() && n1.getSegY1() <= n2.getSegY2())
		|| (n2.getSegY1() >= n1.getSegY1() && n2.getSegY1() <= n1.getSegY2());
	}

	public static boolean horizIntersect(DocNode n1, DocNode n2)
	{
		return (n1.getSegX1() >= n2.getSegX1() && n1.getSegX1() <= n2.getSegX2())  //seg1.getX1() edge lies along seg2 boundary
		|| (n2.getSegX1() >= n1.getSegX1() && n2.getSegX1() <= n1.getSegX2()); //or seg2.getX1() edge lies along seg1 boundary
		// 13.07.10 seems sufficient
	}

	/**
	 * A node n1 contains a node n2 if all four dimensions x1, x2, x3, x4 of n2
	 * lay within those of n1, e.g. n2 is completely within n1.
	 * @param n1
	 * @param n2
	 * @return
	 */
	public static boolean contains(DocNode n1, DocNode n2) 
	{
		if (n1.equals(n2)) return false;
		if (n1.getSegType().equals(n2.getSegType())) return false;

		if ("line-fragment".equals(n2.getSegType()) && "text-fragment".equals(n1.getSegType())) {
			return false;
		}
		if ("text-block".equals(n2.getSegType()) && "line-fragment".equals(n1.getSegType())) {
			return false;
		}		
		return (n1.getSegX1()<=n2.getSegX1() && n1.getSegX2()>=n2.getSegX2() 
				&& n1.getSegY1()<=n2.getSegY1() && n1.getSegY2()>=n2.getSegY2() && 
				n1.getVolume()>=n2.getVolume());
	}

}//DocGraphUtils

