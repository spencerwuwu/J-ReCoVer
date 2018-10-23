// https://searchcode.com/api/result/73372807/

/*
 * PROJECT: Phybots at http://phybots.com/
 * ----------------------------------------------------------------------------
 *
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Phybots.
 *
 * The Initial Developer of the Original Code is Jun Kato.
 * Portions created by the Initial Developer are
 * Copyright (C) 2009 Jun Kato. All Rights Reserved.
 *
 * Contributor(s): Jun Kato
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package com.phybots.gui.workflow.layout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.phybots.gui.workflow.layout.Layout.Coordinate;
import com.phybots.gui.workflow.layout.Layout.Line;
import com.phybots.utils.Array;
import com.phybots.workflow.ControlNode;
import com.phybots.workflow.Edge;
import com.phybots.workflow.Fork;
import com.phybots.workflow.Join;
import com.phybots.workflow.Node;
import com.phybots.workflow.Transition;


public class SugiyamaLayouter {
	private static final int NUM_SWEEPS = 2;
	private Array<Layer> layers;

	// Used for cycle removal.
	private Map<Node, Vertex> nodeMap;
	private Array<Vertex> currentVertices;
	private Array<Vertex> nextVertices;
	private Set<Transition> transitions;
	private Set<Edge> nextEdges;

	// Used for crossing reduction.
	private Array<LayerElement> tmp;
	private Array<Container> containers1;
	private Array<Container> containers2;

	public SugiyamaLayouter() {

		nodeMap = new HashMap<Node, Vertex>();
		layers = new Array<Layer>();
		currentVertices = new Array<Vertex>();
		nextVertices = new Array<Vertex>();

		transitions = new HashSet<Transition>();
		nextEdges = new HashSet<Edge>();

		tmp = new Array<LayerElement>();
		containers1 = new Array<Container>();
		containers2 = new Array<Container>();
	}

	public synchronized Layout doLayout(Node initialNode) {

		if (initialNode == null) {
			return null;
		}

		// Construct a new graph.
		doCycleRemoval(initialNode);
		doNormalization();
		doCrossingReduction();
		doHorizontalCoordinateAssignment();

		/*
		System.out.println("---- Contents ----");
		for (Layer layer : layers) {
			System.out.println(layer);
		}
		System.out.println("----- Layout -----");
		printLayers();
		*/

		// Manage layout information.
		Layout layout = new Layout();
		for (Layer layer : layers) {
			for (LayerElement e : layer) {
				if (e instanceof Vertex) {
					if (!(e instanceof DummyVertex)) {
						Vertex vertex = (Vertex) e;
						layout.putNodeCoord(vertex.getNode(), vertex.getCoord());
						for (Entry<Vertex, Edge> entry :
								vertex.getChildrenEdges().entrySet()) {
							Edge edge = entry.getValue();
							// if (layout.contains(edge)) {
							//	Cannot happen.
							// }
							Vertex v = entry.getKey();
							Vertex childVertex;
							Coordinate[] coords;
							if (v instanceof DummyVertex) {
								DummyVertex dv = (DummyVertex) v;
								Vertex v2 = dv.getChildren().iterator().next();
								if (dv.isHead()) {
									childVertex = v2.getChildren().iterator().next();
									coords = new Coordinate[] {
											vertex.getCoord(),
											v.getCoord(),
											v2.getCoord(),
											childVertex.getCoord()
									};
								} else {
									childVertex = v2;
									coords = new Coordinate[] {
											vertex.getCoord(),
											v.getCoord(),
											childVertex.getCoord()
									};
								}
							} else {
								childVertex = v;
								coords = new Coordinate[] {
										vertex.getCoord(),
										childVertex.getCoord()
								};
							}

							boolean isReversed;
							if (vertex.getNode() != edge.getSource()) {
								isReversed = true;
							} else {
								isReversed = false;
							}
							layout.putEdgeCoords(edge, new Line(coords, isReversed));
						}
					}
				}
			}
		}
		/*
		currentVertices.clear();
		currentVertices.push(initialVertex);
		while (currentVertices.size() > 0) {
			nextVertices.clear();
			for (Vertex vertex : currentVertices) {
				layout.putNodeCoord(vertex.getNode(), vertex.getCoord());
				for (Entry<Vertex, Edge> entry :
						vertex.getChildrenEdges().entrySet()) {
					Edge edge = entry.getValue();
					// if (layout.contains(edge)) {
					//	Cannot happen.
					// }
					Vertex v = entry.getKey();
					Vertex childVertex;
					if (v instanceof DummyVertex) {
						Coordinate[] coords;
						DummyVertex dv = (DummyVertex) v;
						Vertex v2 = dv.getChildren().iterator().next();
						if (dv.isHead()) {
							childVertex = v2.getChildren().iterator().next();
							coords = new Coordinate[] {
									vertex.getCoord(),
									v.getCoord(),
									v2.getCoord(),
									childVertex.getCoord()
							};
						} else {
							childVertex = v2;
							coords = new Coordinate[] {
									vertex.getCoord(),
									v.getCoord(),
									childVertex.getCoord()
							};
						}
						System.out.println("----");
						System.out.println(edge.getSource());
						System.out.println(edge.getDestination());
						layout.putEdgeCoords(edge, coords);
					} else {
						childVertex = v;
					}
					if (!layout.contains(childVertex.getNode())) {
						nextVertices.push(childVertex);
					}
				}
			}
			Array<Vertex> tmp = currentVertices;
			currentVertices = nextVertices;
			nextVertices = tmp;
		}
		*/
		return layout;
	}

	/**
	 * <b>Cycle removal</b>: constructs an acyclic graph.
	 * <dl>
	 * <dt>IN</dt><dd>{@link #initialNode}</dd>
	 * <dt>OUT</dt><dd>{@link #initialVertex} and {@link #layers}, each of which contains {@link Vertex} objects.</dd>
	 * </dl>
	 */
	private void doCycleRemoval(Node initialNode) {
		nodeMap.clear();
		layers.clear();

		currentVertices.clear();
		currentVertices.push(new Vertex(initialNode));

		Layer currentLayer = new Layer(0);
		Layer nextLayer;

		int depth = 0;
		while (currentVertices.size() > 0) {
			nextLayer = new Layer(depth + 1);

			nextVertices.clear();
			BFS: for (Vertex vertex : currentVertices) {
				Node node = vertex.getNode();

				// Skip if the node should be in the next layer.
				if (nextVertices.contains(vertex)) {
					// This happens when there is a transition from a node in the same depth.
					continue;
				}

				nextEdges.clear();

				// Get control flows from the node.
				if (node instanceof ControlNode) {
					Edge[] edges = ((ControlNode) node).getEdges();
					if (node instanceof Fork) {
						nextEdges.addAll(Arrays.asList(edges));
					} else if (node instanceof Join) {

						// Skip if all of the joining nodes have not already been visited in the shallow layers.
						for (Edge edge : edges) {
							Node e = edge.getSource();
							if (!nodeMap.containsKey(e) ||
									currentLayer.contains(nodeMap.get(e))) {
								nextVertices.push(vertex);
								continue BFS; // TODO check infinite loop.
							}
						}
						nextEdges.addAll(Arrays.asList(edges));
					}
				}

				// Get transitions from the node.
				node.getTransitionsOut(transitions);
				nextEdges.addAll(transitions);

				// Set the vertex as visited.
				nodeMap.put(node, vertex);
				currentLayer.push(vertex);

				// Check if nodes to sweep in the next loop have already been visited.
				for (Edge edge : nextEdges) {
					Node nextNode;
					if (edge.getSource() == node) {
						nextNode = edge.getDestination();
					} else {
						nextNode = edge.getSource();
					}
					Vertex nextVertex;
					if (nodeMap.containsKey(nextNode)) {
						nextVertex = nodeMap.get(nextNode);
						nextVertex.linkChild(vertex, edge);
					} else {
						nextVertex = new Vertex(nextNode);
						vertex.linkChild(nextVertex, edge);
						nextVertices.push(nextVertex);
					}
				}
			}

			Array<Vertex> tmp = currentVertices;
			currentVertices = nextVertices;
			nextVertices = tmp;
			layers.push(currentLayer);
			currentLayer = nextLayer;
			depth ++;
		}
	}

	/**
	 * <b>Normalization</b>: insert dummy nodes and segments.
	 * <dl>
	 * <dt>IN</dt><dd>{@link #layers}</dd>
	 * <dt>OUT</dt><dd>{@link #layers}, each of which contains {@link Vertex}, {@link DummyVertex} and {@link Segment} objects.</dd>
	 * </dl>
	 */
	private void doNormalization() {

		for (Layer layer : layers) {
			for (LayerElement e : layer) {
				if (!(e instanceof Vertex) ||
						e instanceof DummyVertex) {
					continue;
				}
				Vertex vertex = (Vertex) e;
				for (Vertex child : vertex.getChildren()) {
					int depthDiff = child.getDepth() - e.getDepth();

					// Difference in depth

					// 1: no dummy node.
					if (depthDiff == 1) {
						// vertex.unlinkChild(child);
						// vertex.linkChild(child);
					}

					// 2: one dummy node.
					else if (depthDiff == 2) {

						DummyVertex dummy = new DummyVertex();

						Edge edge = vertex.unlinkChild(child);
						vertex.linkChild(dummy, edge);
						dummy.linkChild(child, edge);

						layers.get(e.getDepth() + 1).push(dummy);
					}

					// 3-: two dummy nodes and a segment between them.
					else if (depthDiff > 2) {

						Segment segment = new Segment();
						DummyVertex head = segment.getHead();
						DummyVertex tail = segment.getTail();

						Edge edge = vertex.unlinkChild(child);
						vertex.linkChild(head, edge);
						// if (depthDiff == 3) {
							head.linkChild(tail, edge);
						// } else {
						//	head.linkChild(segment);
						//	segment.linkChild(tail);
						// }
						tail.linkChild(child, edge);

						for (int i = vertex.getDepth() + 1; i < child.getDepth(); i ++) {
							if (i == vertex.getDepth() + 1) {
								layers.get(i).push(head);
							} else if (i == child.getDepth() - 1) {
								layers.get(i).push(tail);
							} else {
								layers.get(i).push(segment);
							}
						}
					}

					// Cannot happen.
					else {
						// System.err.println(e+"->"+child+" (layer:"+e.getDepth()+")");
					}
				}
			}
		}
	}

	/**
	 * <b>Crossing reduction</b>: reduce crossings.
	 * <dl>
	 * <dt>IN</dt><dd>{@link #layers}</dd>
	 * <dt>OUT</dt><dd>{@link #layers}</dd>
	 * </dl>
	 */
	private void doCrossingReduction() {

		Layer layer1 = layers.get(0).clone();
		Layer layer2;

		containers1.clear();
		containers1.push(new Container());
		containers1.push(new Container());

		containers2.clear();

		for (int i = 0; i < NUM_SWEEPS; i ++) {

			// Regular sweep.
			// System.out.println("--Regular sweep.");
			for (int j = 1; j < layers.size(); j ++) {
				layer2 = layers.get(j);
				minimizeCrossing(layer1, containers1, layer2, containers2, false);
				Array<Container> tmp = containers1;
				containers1 = containers2;
				containers2 = tmp;
			}
			/*
			for (Layer layer : layers) {
				System.out.println(layer);
			}
			*/

			// Reverse sweep.
			// System.out.println("--Reverse sweep.");
			for (int j = layers.size() - 2; j >= 0; j --) {
				layer2 = layers.get(j);
				minimizeCrossing(layer1, containers1, layer2, containers2, true);
				Array<Container> tmp = containers1;
				containers1 = containers2;
				containers2 = tmp;
			}
			/*
			for (Layer layer : layers) {
				System.out.println(layer);
			}
			*/
		}
	}

	/**
	 * <b>Crossing minimization</b>: minimize crossing between the two layers.
	 *
	 * @param workingLayer vertices in the previous layer. (-> replaced by the vertices in the current layer.)
	 * @param containers1 containers in the previous layer. (-> edited in this method.)
	 * @param layer2 vertices and segments in the current layer. (-> re-ordered in this method.)
	 * @param containers2 null (-> containers in the current layer.)
	 */
	private void minimizeCrossing(
			Layer workingLayer, Array<Container> containers1,
			Layer layer2, Array<Container> containers2,
			boolean isReverse) {

		/*
		System.out.print("----Layer ");
		System.out.print(workingLayer.getDepth());
		System.out.print("->");
		System.out.println(layer2.getDepth());
		*/

		crStep1(workingLayer, containers1, isReverse);
		// printLayer("Step1 (l"+workingLayer.getDepth()+"):", workingLayer, containers1);
		crStep2_1(workingLayer, containers1);
		crStep2_2(layer2, tmp, isReverse);
		// printLayer("Step2 (l"+layer2.getDepth()+"):", tmp);
		crStep3(tmp, containers1);
		// printLayer("Step3 (l"+layer2.getDepth()+"):", tmp);
		crStep4(layer2, tmp, isReverse);
		// printLayer("Step4 (l"+layer2.getDepth()+"):", tmp);

		// crStep5(); // Step.5: Count crossings.
		workingLayer.setDepth(layer2.getDepth());
		crStep6(tmp, layer2, workingLayer, containers2);

		// printLayer("Step6 (l"+layer2.getDepth()+"):", layer2, containers2);
	}

	/**
	 * Step.1:
	 * Replace head vertices by their preceding segments and merge
	 * containers around them.
	 *
	 * @param layer1
	 * @param containers1
	 * @param isReverse
	 */
	private void crStep1(Layer layer1, Array<Container> containers1,
			boolean isReverse) {
		int i = 0;
		Iterator<LayerElement> it = layer1.iterator();
		while (it.hasNext()) {
			LayerElement vertex = it.next();
			Container container = containers1.get(i ++);
			if (vertex instanceof DummyVertex) {
				DummyVertex dummyVertex = (DummyVertex) vertex;
				if (isReverse ? dummyVertex.isTail() : dummyVertex.isHead()) {
					container.append(dummyVertex.getSegment());
					if (i < containers1.size()) {
						Container nextContainer = containers1.get(i);
						container.join(nextContainer);
						containers1.remove(i --);
					}
					it.remove();
				}
			}
		}
	}

	/**
	 * Step.2-1:
	 * Assign pos values to the elements in the old layer.
	 *
	 * @param layer1
	 * @param containers1
	 */
	private void crStep2_1(Layer layer1, Array<Container> containers1) {
		int j = 0;
		int pos = 0;
		for (LayerElement e : layer1) {
			Container container = containers1.get(j ++);
			container.setMeasure(pos);
			pos += container.size();
			e.setMeasure(pos);
			pos ++;
		}
	}

	/**
	 * Step.2-2:
	 * Calculate measure values of the elements in the new layer.
	 * Insert vertices in the current layer into {@link #tmp}.
	 *
	 * @param layer2
	 * @param tmp
	 * @param isReverse
	 */
	private void crStep2_2(Layer layer2, Array<LayerElement> tmp,
			boolean isReverse) {
		tmp.clear();
		Iterator<LayerElement> it = layer2.iterator();
		while (it.hasNext()) {
			LayerElement e = it.next();
			if (e instanceof Segment) {
				// Remove segments which are included in the containers.
				it.remove();
				continue;
			} else if (e instanceof DummyVertex && (isReverse ?
					((DummyVertex) e).isHead() : ((DummyVertex) e).isTail())) {
				// Skip since this node is a tail node which is going to
				// be used in the step 4.
				continue;
			}
			Vertex vertex = (Vertex) e;
			Set<Vertex> neighbors = isReverse ? vertex.getChildren() : vertex.getParents();
			if (neighbors.isEmpty()) {
				// This cannot occur.
			} else {
				double m = 0;
				for (LayerElement neighbor : neighbors) {
					m += neighbor.getMeasure();
				}
				m /= neighbors.size();
				vertex.setMeasure(m);

				// TODO make this operation fast by using a binary tree.
				int j = 0;
				for (int i = 0; i < tmp.size(); i ++) {
					if (tmp.get(i).getMeasure() <= m) {
						j = i + 1;
						break;
					}
				}
				tmp.insert(vertex, j);
			}
		}
	}

	/**
	 * Step.3-2:
	 * Merge lists of vertices and containers.
	 *
	 * @param tmp
	 * @param containers1
	 */
	private void crStep3(Array<LayerElement> tmp, Array<Container> containers1) {
		int vi, cj;
		vi = cj = 0;
		Container container = containers1.get(cj ++);
		while (vi < tmp.size()) {
			LayerElement vertex = tmp.get(vi ++);
			double mv = vertex.getMeasure();
			while (container != null) {
				double mc = container.getMeasure();
				if (mv <= mc) {
					break;
				} else if (mv >= mc + container.size() - 1) {
					tmp.insert(container, vi - 1);
					container = containers1.get(cj ++);
					vi ++;
				} else {
					int k = (int) Math.ceil(mv - mc);
					Container c = container.split(k);
					tmp.insert(container, vi ++);
					c.setMeasure(mc + k);
					container = c;
				}
			}
		}
		if (container != null) {
			tmp.push(container);
		}
		for (; cj < containers1.size(); cj ++) {
			tmp.push(containers1.get(cj));
		}
	}

	/**
	 * Step.4:
	 * Split containers containing tail nodes.
	 *
	 * @param layer2
	 * @param tmp
	 * @param isReverse
	 */
	private void crStep4(Layer layer2, Array<LayerElement> tmp,
			boolean isReverse) {
		for (LayerElement vertex : layer2) {
			if (vertex instanceof DummyVertex) {
				DummyVertex dummy = (DummyVertex) vertex;
				if (isReverse ? dummy.isHead() : dummy.isTail()) {
					Segment segment = dummy.getSegment();
					Container container = segment.getContainer();
					for (int j = 0; j < tmp.size(); j ++) {
						LayerElement e = tmp.get(j);
						if (e instanceof Container &&
								e.equals(container)) {
							Container c = container.split(segment);
							tmp.insert(dummy, ++ j);
							tmp.insert(c, ++ j);
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Step.6:
	 * Insert empty containers between two consecutive vertices, and join two
	 * consecutive containers in the list.
	 *
	 * @param tmp
	 * @param layer2
	 * @param layer1
	 * @param containers2
	 */
	private void crStep6(Array<LayerElement> tmp,
			Layer layer2, Layer layer1,
			Array<Container> containers2) {
		layer2.clear();
		layer1.clear();
		containers2.clear();
		Container last = null;
		for (LayerElement e : tmp) {
			if (e instanceof Vertex) {
				if (last == null) {
					// for (Segment s : last) layer2.push(s);
					containers2.push(new Container());
				} else {
					for (Segment s : last) layer2.push(s);
					containers2.push(last);
					last = null;
				}
				layer2.push(e);
				layer1.push(e);
			} else if (e instanceof Container) {
				Container container = (Container) e;
				if (last == null) {
					last = container;
				} else {
					last.join(container);
				}
			} else {
				// Cannot happen.
			}
		}
		if (last != null) {
			for (Segment s : last) layer2.push(s);
			containers2.push(last);
		}
	}

	/**
	 * <b>Horizontal coordinate assignment</b>: assign x coordinates to vertices and segments.
	 * <dl>
	 * <dt>IN</dt><dd>{@link #layers}</dd>
	 * <dt>OUT</dt><dd>{@link #layers}</dd>
	 * </dl>
	 */
	private void doHorizontalCoordinateAssignment() {

		// Regular sweep.
		for (int i = 0; i < layers.size(); i ++) {
			Layer layer = layers.get(i);
			int x = 0;
			for (LayerElement e : layer) {
				if (e.getX() > x) {
					x = e.getX();
				} else {
					e.setX(x);
				}
				x ++;
			}
		}

		// Reverse sweep.
		for (int i = layers.size() - 1; i >= 0; i --) {
			Layer layer = layers.get(i);
			int x = 0;
			for (LayerElement e : layer) {
				if (e.getX() > x) {
					x = e.getX();
				} else {
					e.setX(x);
				}
				x ++;
			}
		}
	}

	/*
	private void printLayer(String prefix, Layer layer1, Array<Container> containers1) {
		System.out.print(prefix);
		System.out.print(containers1.get(0));
		for (int j = 0; j < layer1.size(); j ++) {
			System.out.print(" ");
			System.out.print(layer1.get(j));
			System.out.print(" ");
			System.out.print(containers1.get(j + 1));
		}
		System.out.println();
	}

	private void printLayer(String prefix, Array<LayerElement> elements) {
		System.out.print(prefix);
		for (LayerElement e : elements) {
			System.out.print(" ");
			System.out.print(e);
		}
		System.out.println();
	}

	private void printLayers() {
		for (Layer layer : layers) {
			System.out.print("Layer ");
			System.out.print(layer.getDepth());
			System.out.print(":");
			int x = 0;
			for (LayerElement e : layer) {
				for (int i = x; i < e.getX(); i ++) {
					System.out.print("  ");
				}
				System.out.print(" ");
				if (e instanceof Vertex) {
					if (e instanceof DummyVertex) {
						if (((DummyVertex) e).isHead()) {
							System.out.print("h");
						} else
						if (((DummyVertex) e).isTail()) {
							System.out.print("t");
						} else {
							System.out.print("d");
						}
					} else {
						System.out.print("v");
					}
				} else {
					System.out.print("|");
				}
				x = e.getX() + 1;
			}
			System.out.println();
		}
	}
	*/
}

