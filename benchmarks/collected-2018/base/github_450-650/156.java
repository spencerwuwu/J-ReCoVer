// https://searchcode.com/api/result/110065717/

package snepsui.Interface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import sneps.CaseFrame;
import sneps.CustomException;
import sneps.Network;
import sneps.Node;
import sneps.Relation;
import sneps.UpCable;
import sneps.UpCableSet;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ConstantDirectionalEdgeValueTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

@SuppressWarnings({"unchecked", "hiding"})
public class TestNetwork extends JPanel{
	private Network network;
    private int id = 0;
    
    /**
     * Transforms the color of each vertex depending of its type
     */
    private Transformer<String,Paint> vertexPaint;
    
    /**
     * Transforms the shape of the vertex depending on the name
     * of the vertex for all the text to be displayed inside the 
     * vertex
     */
    private Transformer<String,Integer> shape;
    
    /**
     * Transforms the name of the edge, it deletes the id appended
     *  to the relation name
     */
    private Transformer<String, String> edgeLabel;
    private VisualizationViewer<String, String> vv;
    
    public TestNetwork(Network network) {
    	this.network = network;
    	initGUI();
    }
    
	public void initGUI() {
		try {
			this.removeAll();
			setPreferredSize(new Dimension(800, 800));
			drawNetwork();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void drawNetwork() {
		Graph<String, String> graph = new DirectedSparseMultigraph<String, String>();
		final LinkedList<Node> nodesList = new LinkedList<Node>();
		
		Hashtable<String, Node> nodes = network.getNodes();
		String nodeString = ""; 
		Set<String> set = nodes.keySet();

	    Iterator<String> itr1 = set.iterator();
	    while (itr1.hasNext()) {
	    	nodeString = itr1.next();
	    	Node node = nodes.get(nodeString);
	    	String nodeName = node.getIdentifier();
	    	System.out.println(node.getIdentifier());
	    	System.out.println(nodeName);
	    	graph.addVertex(nodeName);
	    	nodesList.add(node);
	    }
	    
	    Iterator<String> itr2 = set.iterator();
//	    while (itr2.hasNext()) {
//	    	nodeString = itr2.next();
//	    	Node node = nodes.get(nodeString);
//	    	UpCableSet upCableSet = node.getUpCableSet();
//	    	for (int i = 0; i < upCableSet.getUpCables().size(); i++) {
//	    		Relation relation = upCableSet.getUpCables().get(i).getRelation();
//		    	LinkedList<Node> nodeset = upCableSet.getUpCables().get(i).getNodeSet().getNodes();
//		    	for(Node item : nodeset) {
//		    		graph.addEdge(new RelationEdge(relation.getName()).toString(),item.getIdentifier(),node.getIdentifier());
//		    		System.out.println("Relation Name: " + relation.getName());
//		    		System.out.println("Node Name: " + node.getIdentifier());
//		    		System.out.println("Upcable Node: "+ item.getIdentifier());
//		    	}
//	    	}
//	    }
	    
	    while (itr2.hasNext()) {
	    	nodeString = itr2.next();
	    	Node node = nodes.get(nodeString);
	    	UpCableSet upCableSet = node.getUpCableSet();
	    	
	    	for (int i = 0; i < upCableSet.size(); i++) {
	    		UpCable upcable = upCableSet.getUpCable(i);
	    		Relation relation = upcable.getRelation();
	    		
	    		for (int j = 0; j < upcable.getNodeSet().size(); j ++) {
	    			Node item = upcable.getNodeSet().getNode(j);
	    			graph.addEdge(new RelationEdge(relation.getName()).toString(),item.getIdentifier(),node.getIdentifier());
	    		}
	    	}
	    }
	    
	    ISOMLayout<String, String> layout = new ISOMLayout<String,String>(graph);
	    
	    shape = new Transformer<String, Integer>() {
	    	public Integer transform(String vertex) {
	    		int stringLength = 0;
	    		for(Node node : nodesList) {
    				if(vertex.equals(node.getIdentifier())) {
    					stringLength = node.getIdentifier().length();
    				}
        		}
	    		return stringLength;
	    	}
		};
        
        vertexPaint = new Transformer<String,Paint>() {
        	public Paint transform(String vertex) {
        		for(Node node : nodesList) {
    				if(node.getIdentifier().equals(vertex)) {
    					if(node.getClass().getSimpleName().equals("BaseNode")) {
    						return Color.green;
    					} else if (node.getClass().getSimpleName().equals("VariableNode")) {
    						return Color.gray;
    					} else if (node.getClass().getSimpleName().equals("PatternNode")) {
    						return Color.blue;
    					} else if (node.getClass().getSimpleName().equals("ClosedNode")) {
    						return Color.yellow;
    					} else if (node.getClass().getSimpleName().equals("Act")) {
    						return Color.magenta;
    					}
    				}
        		}
        		return Color.magenta;
        	}
        };
        
        edgeLabel = new Transformer<String, String>() {
        	public String transform(String edge) {
        		return edge.substring(0, edge.indexOf(":"));
        	}
		};
        
        VertexShapeSizeAspect<String> vssa = new VertexShapeSizeAspect<String>(graph, shape);
        
        vv =  new VisualizationViewer<String,String>(layout, new Dimension(700,470));
        
        vv.setBackground(Color.white);
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
        vv.getRenderContext().setEdgeLabelTransformer(edgeLabel);
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        vv.setVertexToolTipTransformer(new ToStringLabeller<String>());
        vv.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
        vv.getRenderContext().setVertexShapeTransformer(vssa);
        vv.getRenderContext().setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer(0.5, 0.5));
        vssa.setScaling(true);
        
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        this.add(panel);
        
        final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();

        vv.setGraphMouse(graphMouse);
        vv.addKeyListener(graphMouse.getModeKeyListener());

        JComboBox modeBox = graphMouse.getModeComboBox();
        modeBox.addItemListener(graphMouse.getModeListener());
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);

        final ScalingControl scaler = new CrossoverScalingControl();
        
        vv.scaleToLayout(scaler);

        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });

        JPanel scaleGrid = new JPanel(new GridLayout(1,0));
        scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));

        JPanel controls = new JPanel();
        scaleGrid.add(plus);
        scaleGrid.add(minus);
        controls.add(scaleGrid);
        controls.add(modeBox);
        this.add(controls, BorderLayout.SOUTH);
        
        this.validate();
        this.repaint();
	}
	
	class RelationEdge {
		String name;
		
		public RelationEdge(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name + ":" + id++;
		}
	}
	
	private final static class VertexShapeSizeAspect<String>
    extends AbstractVertexShapeTransformer <String>
    implements Transformer<String,Shape>  {
    	
        protected boolean stretch = false;
        protected boolean scale = false;
        protected boolean funny_shapes = false;
        protected Transformer<String,Integer> shape;
        protected Graph<String,String> graph;
        
        public VertexShapeSizeAspect(Graph<String, String> graph2, Transformer<String,Integer> shapeIn)
        {
        	this.graph = graph2;
            this.shape = shapeIn;
            setSizeTransformer(new Transformer<String,Integer>() {

				public Integer transform(String v) {
		            if (scale) {
		            	int shapeSize = shape.transform(v);
		            	if(shapeSize == 2 || shapeSize == 1) {
		            		return 20;
		            	} else if (shapeSize == 3) {
		            		return 30;
		            	} else {
		            		return shapeSize * 8;
		            	}
		            }
		            else
		                return 20;

				}});
            setAspectRatioTransformer(new Transformer<String,Float>() {

				public Float transform(String v) {
		            if (stretch) {
		                return (float)(graph.inDegree(v) + 1) / 
		                	(graph.outDegree(v) + 1);
		            } else {
		                return 1.0f;
		            }
				}});
        }
        
        public void setScaling(boolean scale)
        {
            this.scale = scale;
        }
        
        public Shape transform(String v)
        {
            if (funny_shapes)
            {
                if (graph.degree(v) < 5)
                {	
                    int sides = Math.max(graph.degree(v), 3);
                    return factory.getRegularPolygon(v, sides);
                }
                else
                    return factory.getRegularStar(v, graph.degree(v));
            }
            else
                return factory.getEllipse(v);
        }
    }
	
	public static void main(String[]args) {
		Network network = new Network();
		try {
			//Define the Relations
	    	Relation rr1 = network.defineRelation("member","Entity","reduce",0);
	    	Relation rr2 = network.defineRelation("class","Entity","reduce",0);
	    	Relation rr3 = network.defineRelation("object","Entity","reduce",0);
	    	Relation rr4 = network.defineRelation("isa","Entity","reduce",0);
	    	Relation rr5 = network.defineRelation("has","Entity","reduce",0);
	    	
	    	//Define the Case Frames
	    	LinkedList<Relation> relations1 = new LinkedList<Relation>();
	    	relations1.add(rr1);
	    	relations1.add(rr2);
	    	CaseFrame caseframe1 = network.defineCaseFrame("Entity", relations1);
	    	
	    	LinkedList<Relation> relations2 = new LinkedList<Relation>();
	    	relations2.add(rr3);
	    	relations2.add(rr4);
	    	CaseFrame caseframe2 = network.defineCaseFrame("Entity", relations2);
	    	
	    	LinkedList<Relation> relations3 = new LinkedList<Relation>();
	    	relations3.add(rr3);
	    	relations3.add(rr5);
	    	CaseFrame caseframe3 = network.defineCaseFrame("Entity", relations3);
	    	
	    	//Build Base Nodes
	    	//Node node = network.build("Clyde");
	    	//Node node1 = network.build("Dumbo");
	    	//Node node2 = network.build("elephant");
	    	//Node node3 = network.build("Tweety");
	    	Node node4 = network.build("canary");
	    	Node node5 = network.build("Opus");
	    	//Node node6 = network.build("bird");
	    	Node node7 = network.build("elephant"); 
	    	Node node8 = network.build("animal");
	    	Node node9 = network.build("circus elephant");
	    	//Node node10 = network.build("elephant");
	    	Node node11 = network.build("Dumbo");
	    	//Node node12 = network.build("circus elephant");
	    	Node node13 = network.build("Clyde");
	    	Node node14 = network.build("bird");
	    	Node node15 = network.build("Tweety");
	    	Node node16 = network.build("head");
	    	Node node17 = network.build("mouth");
	    	Node node18 = network.build("trunk");
	    	Node node19 = network.build("appendage");
	    	
	    	// 1) (assert member (Clyde, Dumbo) class elephant)
	    	Object[][] o1 = new Object[3][2];
	    	o1[0][0] = rr1;
	    	o1[0][1] = node13;
	    	o1[1][0] = rr1;
	    	o1[1][1] = node11;
	    	o1[2][0] = rr2;
	    	o1[2][1] = node7;
	    	
	    	Node res1 = network.build(o1,caseframe1);
	    	System.out.println("Created Node: " + res1.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res1.getIdentifier()).getIdentifier());
	    	
	    	// 2)(assert member Tweety class canary)
	    	Object[][] o2 = new Object[2][2];
	    	o2[0][0] = rr1;
	    	o2[0][1] = node15;
	    	o2[1][0] = rr2;
	    	o2[1][1] = node4;
	    	
	    	Node res2 = network.build(o2,caseframe1);
	    	System.out.println("Created Node: " + res2.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res2.getIdentifier()).getIdentifier());
	    	
	    	// 3) (assert member Opus class bird)
	    	Object[][] o3 = new Object[2][2];
	    	o3[0][0] = rr1;
	    	o3[0][1] = node5;
	    	o3[1][0] = rr2;
	    	o3[1][1] = node14;
	    	
	    	Node res3 = network.build(o3,caseframe1);
	    	System.out.println("Created Node: " + res3.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res3.getIdentifier()).getIdentifier());
	    	
	    	// 4) (assert object elephant isa animal)
	    	Object[][] o4 = new Object[2][2];
	    	o4[0][0] = rr3;
	    	o4[0][1] = node7;
	    	o4[1][0] = rr4;
	    	o4[1][1] = node8;
	    	
	    	Node res4 = network.build(o4,caseframe2);
	    	System.out.println("Created Node: " + res4.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res4.getIdentifier()).getIdentifier());
	    	
	    	// 5) (assert object circus\ elephant isa elephant)
	    	Object[][] o5 = new Object[2][2];
	    	o5[0][0] = rr3;
	    	o5[0][1] = node9;
	    	o5[1][0] = rr4;
	    	o5[1][1] = node7;
	    	
	    	Node res5 = network.build(o5,caseframe2);
	    	System.out.println("Created Node: " + res5.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res5.getIdentifier()).getIdentifier());
	    	
	    	// 6) (assert object Dumbo isa circus\ elephant)
	    	Object[][] o6 = new Object[2][2];
	    	o6[0][0] = rr3;
	    	o6[0][1] = node11;
	    	o6[1][0] = rr4;
	    	o6[1][1] = node9;
	    	
	    	Node res6 = network.build(o6,caseframe2);
	    	System.out.println("Created Node: " + res6.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res6.getIdentifier()).getIdentifier());
	    	
	    	// 7) (assert object Clyde isa animal)
	    	Object[][] o7 = new Object[2][2];
	    	o7[0][0] = rr3;
	    	o7[0][1] = node13;
	    	o7[1][0] = rr4;
	    	o7[1][1] = node8;
	    	
	    	Node res7 = network.build(o7,caseframe2);
	    	System.out.println("Created Node: " + res7.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res7.getIdentifier()).getIdentifier());
	    	
	    	// 8) (assert object bird isa animal)
	    	Object[][] o8 = new Object[2][2];
	    	o8[0][0] = rr3;
	    	o8[0][1] = node14;
	    	o8[1][0] = rr4;
	    	o8[1][1] = node8;
	    	
	    	Node res8 = network.build(o8,caseframe2);
	    	System.out.println("Created Node: " + res8.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res8.getIdentifier()).getIdentifier());
	    	
	    	// 9) (assert object Tweety isa bird)
	    	Object[][] o9 = new Object[2][2];
	    	o9[0][0] = rr3;
	    	o9[0][1] = node15;
	    	o9[1][0] = rr4;
	    	o9[1][1] = node14;
	    	
	    	Node res9 = network.build(o9,caseframe2);
	    	System.out.println("Created Node: " + res9.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res9.getIdentifier()).getIdentifier());
	    	
	    	// 10) (assert object animal has head)
	    	Object[][] o10 = new Object[2][2];
	    	o10[0][0] = rr3;
	    	o10[0][1] = node8;
	    	o10[1][0] = rr5;
	    	o10[1][1] = node16;
	    	
	    	Node res10 = network.build(o10,caseframe3);
	    	System.out.println("Created Node: " + res10.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res10.getIdentifier()).getIdentifier());
	    	
	    	// 11) (assert object head has mouth)
	    	Object[][] o11 = new Object[2][2];
	    	o11[0][0] = rr3;
	    	o11[0][1] = node16;
	    	o11[1][0] = rr5;
	    	o11[1][1] = node17;
	    	
	    	Node res11 = network.build(o11,caseframe3);
	    	System.out.println("Created Node: " + res11.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res11.getIdentifier()).getIdentifier());
	    	
	    	// 12) (assert object elephant has trunk)
	    	Object[][] o12 = new Object[2][2];
	    	o12[0][0] = rr3;
	    	o12[0][1] = node7;
	    	o12[1][0] = rr5;
	    	o12[1][1] = node18;
	    	
	    	Node res12 = network.build(o12,caseframe3);
	    	System.out.println("Created Node: " + res12.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res12.getIdentifier()).getIdentifier());
	    	
	    	// 13) (assert object trunk isa appendage)
	    	Object[][] o13 = new Object[2][2];
	    	o13[0][0] = rr3;
	    	o13[0][1] = node18;
	    	o13[1][0] = rr5;
	    	o13[1][1] = node19;
	    	
	    	Node res13 = network.build(o13,caseframe2);
	    	System.out.println("Created Node: " + res13.getIdentifier());
	    	System.out.println("Network Nodes: " + network.getNodes().get(res13.getIdentifier()).getIdentifier());
		} catch(CustomException e) {
			e.printStackTrace();
		}
		
		JFrame popupFrame = new JFrame("build");
		popupFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		TestNetwork testNetwork = new TestNetwork(network);
		popupFrame.getContentPane().add(testNetwork);
		popupFrame.pack();
		popupFrame.setVisible(true);
	}
}

