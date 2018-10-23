// https://searchcode.com/api/result/110065522/

package sneps;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Stack;

import snebr.Context;
import snip.ds.Channel;
import snip.ds.ReportSet;
import snip.ds.Request;
import snip.fns.*;

import match.ds.*;

/**
 * The Network class is the main class for the network management system. This class 
 * contains relations, case frames as well as the nodes that exist in the network.
 * 
 * @author Amr Khaled Dawood
 */
@SuppressWarnings("serial")
public class Network implements Serializable
{

	/**
	 * a hash table containing nodes of the network indexed by their identifiers.
	 */
	private Hashtable<String,Node> nodes;
	
	/**
	 * a hash table containing sets of molecular nodes in the network indexed by the id's
	 * of their case frames
	 */
	private Hashtable<String,NodeSet> molecularNodes;
	
	/**
	 * a hash table containing case frames indexed by their id's
	 */
	private Hashtable<String,CaseFrame> caseFrames;
	
	/**
	 * a hash table containing the relations indexed by their names.
	 */
	private Hashtable<String,Relation> relations;
	
	/**
	 * the counter used for the names of closed molecular nodes. Each closed molecular node is 
	 * named by 'M' followed by a positive number, for example: "M12".
	 */
	private int molCounter;
	
	/**
	 * the counter used for names of pattern molecular nodes. Each pattern molecular node is
	 * named by 'P' followed by a positive number, for example: "P10" 
	 */
	private int patCounter;
	
	/**
	 * the counter used for names of variable nodes. each variable node is
	 * named by 'V' followed by a positive number, for example: "V4" 
	 */
	private int varCounter;
	
	/**
	 * a list of numbers that the user used in naming nodes he created that look like 
	 * names of closed molecular nodes' names.
	 */
	private LinkedList<Integer> userDefinedMolSuffix;
	
	/**
	 * a list of numbers that the user used in naming nodes he created that look like 
	 * names of pattern molecular nodes' names.
	 */
	private LinkedList<Integer> userDefinedPatSuffix;
	
	/**
	 * a list of numbers that the user used in naming nodes he created that look like 
	 * names of variable nodes' names.
	 */
	private LinkedList<Integer> userDefinedVarSuffix;
	
	/**
	 * the instance of the class Network(assuming a Network may have only one instance).
	 */
	private static Network instance = null;
	
	/**
	 * The constructor initializes all instance variables.
	 */
	public Network()
	{
		nodes = new Hashtable<String, Node>();
		molecularNodes = new Hashtable<String,NodeSet>();
		caseFrames = new Hashtable<String, CaseFrame>();
		relations = new Hashtable<String, Relation>();
		userDefinedMolSuffix = new LinkedList<Integer>();
		userDefinedPatSuffix = new LinkedList<Integer>();
		userDefinedVarSuffix = new LinkedList<Integer>();
		molCounter = 0;
		patCounter = 0;
		varCounter = 0;
		instance = this;
	}

	/**
	 * gets the instance of the network class
	 * 
	 * @return the instance of the class Network.
	 */
	public static Network getInstance()
	{
		return instance;
	}

	/**
	 * gets the hash table of nodes
	 * 
	 * @return that hash table of the nodes
	 */
	public Hashtable<String,Node> getNodes()
	{
		return nodes;
	}

	/**
	 * gets the hash table of node sets of molecular nodes
	 * 
	 * @return the hash table of molecular nodes
	 */
	public Hashtable<String,NodeSet> getMolecularNodes()
	{
		return molecularNodes;
	}

	/**
	 * gets the hash table of case frames
	 * 
	 * @return the hash table of the case frames
	 */
	public Hashtable<String,CaseFrame> getCaseFrames()
	{
		return caseFrames;
	}

	/**
	 * gets the hash table of relations
	 * 
	 * @return the hash table of the relations
	 */
	public Hashtable<String,Relation> getRelations()
	{
		return relations;
	}
	
	/**
	 * gets the list of suffices of nodes' names that look like closed nodes' names
	 * 
	 * @return a LinkedList of numbers that the user used them as suffices to 'M' 
	 * for naming the nodes he created 
	 */
	public LinkedList<Integer> getUserDefinedMolSuffix()
	{
		return userDefinedMolSuffix;
	}

	/**
	 * gets the list of suffices of nodes' names that look like the pattern nodes' names
	 * 
	 * @return a LinkedList of numbers that the user used them as suffices to 'P' 
	 * for naming the nodes he created
	 */
	public LinkedList<Integer> getUserDefinedPatSuffix()
	{
		return userDefinedPatSuffix;
	}
	
	/**
	 * gets the list of suffices of nodes' names that look like the variable nodes' names
	 * 
	 * @return a LinkedList of Integers that the user used to name nodes that 
	 * look like variable nodes' names
	 */
	public LinkedList<Integer> getUserDefinedVarSuffix()
	{
		return userDefinedVarSuffix;
	}
	
	/**
	 * gets the relation with the given name
	 * 
	 * @param name the name of the relation 
	 * @return the Relation with the specified name
	 * @throws CustomException an exception is thrown if there is no relation with
	 * the specified name
	 */
	public Relation getRelation(String name)throws CustomException
	{
		if(! relations.containsKey(name))
			throw new CustomException("there is no relation with this name");
		else
			return relations.get(name);
	}

	/**
	 * gets the case frame with the given id
	 * 
	 * @param relString the id of the case frame
	 * @return the case frame with the given id
	 * @throws CustomException an exception is thrown if there is no case frame
	 * with the given id
	 */
	public CaseFrame getCaseFrame(String id)throws CustomException
	{
		if(! caseFrames.containsKey(id))
			throw new CustomException("there is no case frame with such set of relations");
		else
			return caseFrames.get(id);
	}
	
	/**
	 * gets the node with the given identifier
	 * 
	 * @param identifier the identifier of the node 
	 * @return the node with the given identifier
	 * @throws CustomException an exception is thrown if there is no nodes
	 * with such a name
	 */
	public Node getNode(String identifier)throws CustomException
	{
		if(! nodes.containsKey(identifier))
			throw new CustomException("there is no node with such name");
		else
			return nodes.get(identifier);
	}
	
	/**
	 * gets the molecular node with the given cable set
	 * 
	 * @param array a 2D array of Objects that represents relation-node pairs for the cable set
	 * @return the molecular node that have such a cable set
	 * @throws CustomException if the node does not exist
	 */
	public MolecularNode getMolecularNode(Object[][] array)throws CustomException
	{
		if(! cableSetExists(array))
			throw new CustomException("the node does not exist");
		else
		{
			int counter = 0;
			NodeSet intersection = new NodeSet();
			while(true)
			{
				if(array[counter][1].getClass().getSimpleName().equals("NodeSet"))
					counter++;
				else
				{
					String r = ((Relation) array[counter][0]).getName();
					NodeSet ns = ((Node) array[counter][1]).getUpCableSet().getUpCable(r).getNodeSet();
					intersection.addAll(ns);
					break;
				}
			}
			for(int i=counter;i<array.length;i++)
			{
				if(array[i][1].getClass().getSimpleName().equals("NodeSet"))
					continue;
				else
				{
					Relation r1 = (Relation) array[i][0];
					NodeSet ns1 = ((Node) array[i][1]).getUpCableSet().getUpCable(r1.getName()).getNodeSet();
					intersection = intersection.Intersection(ns1);
				}
			}
			return (MolecularNode) intersection.getNode(0);
		}
	}
	
	/**
	 * defines a relation with the given parameters
	 * 
	 * @param name the name of the relation
	 * @param type the semantic type of the node that the relation could point at
	 * @param adjust the adjustability of the relation "reduce", "expand", or "none"
	 * @param limit the minimum size of the node set containing nodes pointed to by this 
	 * relation
	 * @return the defined relation
	 * @throws CustomException an exception is thrown if the relation already exists
	 */
	public Relation defineRelation(String name,String type,String adjust,int limit)throws CustomException
	{
		if(relations.containsKey(name))
			throw new CustomException("the relation already exists");
		else
			relations.put(name,new Relation(name,type,adjust,limit));
		
		return relations.get(name);
	}
	
	/**
	 * removes the relation with the given name from the hash table of relations,
	 * and removes case frame containing this relation from the hash table of case frames
	 * 
	 * @param name the name of the relation to be removed
	 */
	public void undefineRelation(String name)
	{
		Relation r = relations.get(name);
		
		// removing case frames that contain this relation
		for (Enumeration<CaseFrame> e = caseFrames.elements(); e.hasMoreElements();)
		{
			CaseFrame caseFrame = e.nextElement();
			if(caseFrame.getRelations().contains(r))
				caseFrames.remove(caseFrame.getId());
		}

		// removing the relation
		relations.remove(name);
	}
	
	/**
	 * defines a case frame with the given parameters
	 * 
	 * @param semanticType the semantic type of the node that can have such a case frame
	 * @param relationSet a list of relations representing out-going arcs from the node 
	 * containing such a case frame
	 * @return the case frame that was just created
	 * @throws CustomException a CustomException is thrown if there is a case frame with
	 * the same set of relations
	 */
	public CaseFrame defineCaseFrame(String semanticType,
			LinkedList<Relation> relationSet)throws CustomException
	{
		CaseFrame caseFrame = new CaseFrame(semanticType,relationSet);
		if(caseFrames.containsKey(caseFrame.getId()))
			throw new CustomException("case frame already exists");
		else
		{
			caseFrames.put(caseFrame.getId(),caseFrame);
			if(! this.molecularNodes.containsKey(caseFrame.getId()))
				this.molecularNodes.put(caseFrame.getId(),new NodeSet());
		}
		
		return caseFrames.get(caseFrame.getId());
	}
	
	/**
	 * removes the case frame with the given id from the hash table of case frames
	 * 
	 * @param id the id of the case frame to be undefined
	 */
	public void undefineCaseFrame(String id)
	{
		caseFrames.remove(id);
	}

	/**
	 * defines the given path for the given relation
	 * 
	 * @param relation a Relation to define the path for
	 * @param path a Path to be defined for the given relation
	 */
	public void definePath(Relation relation,Path path)
	{
		relation.setPath(path);
	}
	
	/**
	 * sets the path in the given relation to null
	 * 
	 * @param relation a relation to undefine the path defined for it
	 */
	public void undefinePath(Relation relation)
	{
		relation.setPath(null);
	}
	
	/**
	 * removes the given node from the hash table of nodes as well as the nodes dominated only
	 * by this node
	 * 
	 * @param node a node to be removed from the network
	 * @throws CustomException a CustomException is thrown if the node is not isolated
	 */
	public void removeNode(Node node)throws CustomException
	{
		if(! node.getUpCableSet().isEmpty())
			throw new CustomException("node is not isolated");
		
		// removing the node from the hash table
		nodes.remove(node.getIdentifier());
		
		// removing child nodes of this node that have no other parents
		if(node.getClass().getSuperclass().getSimpleName().equals("MolecularNode"))
		{
			MolecularNode m = (MolecularNode) node;
			molecularNodes.get(m.getCableSet().getCaseFrame().getId()).removeNode(node);
			CableSet cableSet = m.getCableSet();
			// loop for cables
			for(int i=0;i<cableSet.size();i++)
			{
				Cable cable = cableSet.getCable(i);
				NodeSet ns = cable.getNodeSet();
				//loop for nodes in the node set
				for(int j=0;j<ns.size();j++)
				{
					Node n = ns.getNode(j);
					// loop for UpCables
					for(int k=0;k<n.getUpCableSet().size();k++)
					{
						UpCable upCable = n.getUpCableSet().getUpCable(k);
						upCable.removeNode(node);
						if(upCable.getNodeSet().isEmpty())
							n.getUpCableSet().removeUpCable(upCable);
					}
					// removing child nodes
					if(n.getUpCableSet().isEmpty())
						removeNode(n);
				}
			}
		}
	}
	
	/**
	 * builds a new variable node
	 * 
	 * @return the variable node that was just built
	 */
	public VariableNode buildVariableNode()
	{
		VariableNode node = new VariableNode(getNextVarName());
		this.nodes.put(node.getIdentifier(),node);
		return node;
	}
	
	/**
	 * builds a new base node with the given identifier
	 * 
	 * @param identifier a String representing the name of the base node
	 * @return the Node that was just built
	 * @throws CustomException an exception is thrown if the node with the given 
	 * identifier already exists 
	 */
	public Node build(String identifier)throws CustomException
	{
		if(! nodes.containsKey(identifier))
		{
			nodes.put(identifier,new BaseNode(identifier));
			if(isMolName(identifier)>-1)
				userDefinedMolSuffix.add(new Integer(isMolName(identifier)));
			if(isPatName(identifier)>-1)
				userDefinedPatSuffix.add(new Integer(isPatName(identifier)));
			if(isVarName(identifier)>-1)
				userDefinedVarSuffix.add(new Integer(isVarName(identifier)));
		}else
			throw new CustomException("the node with this name already exists");
		
		return nodes.get(identifier);
	}
	
	/**
	 * builds a molecular node with the given cable set and case frame
	 * 
	 * @param array a 2D array of Relation-Node pairs representing the cable set
	 * @param caseFrame a case frame to be implemented by the given cable set
	 * @return the node that was just built
	 * @throws CustomException an exception is thrown if the cable set already exists, 
	 * the number of nodes pointed to by a relation is violating the restriction in the
	 * relation (limit), or if the relations in the array are different from those of 
	 * the case frame
	 */
	public MolecularNode build(Object[][] array,CaseFrame caseFrame)throws CustomException
	{
		if(cableSetExists(array))
			throw new CustomException("cable set already exists");

		// check the size of the node sets
		if(! validNodeSetSize(turnIntoRelNodeSet(array)))
			throw new CustomException("wrong node set size");
		
		// check the case frame validation
		if(! followingCaseFrame(turnIntoRelNodeSet(array),caseFrame))
			throw new CustomException("not following the case frame");
		
		// create the molecular node
		MolecularNode node;
		if(isToBePattern(array))
			node = createPatNode(array,caseFrame);
		else
			node = createMolNode(array,caseFrame);
		
		// add the molecular node to the hash tables
		this.nodes.put(node.getIdentifier(),node);
		this.molecularNodes.get(node.getCableSet().getCaseFrame().getId()).addNode(node);
		
		return node;
	}
	
	/**
	 * Matches a given molecular node with nodes in the network that have the same case frame 
	 * and returns a list of triples (the matching node, the source bindings and the target bindings
	 * 
	 * @param node a MolecularNode to match with nodes in the network
	 * @return a LinkedList of 1D array of Objects of size 3 each. Index 0 in the array is the source 
	 * node itself, index 1 is the source bindings, and index 2 is the target bindings.
	 */
	public LinkedList<Object[]> match(MolecularNode node)
	{
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		
		NodeSet ns = this.getMolecularNodes().get(node.getCableSet().getCaseFrame().getId());
		for(int i=0;i<ns.size();i++)
		{
			MolecularNode m = (MolecularNode) ns.getNode(i);
			if(m.equals(node))
				continue;
			LinkedList<Substitutions> rList = new LinkedList<Substitutions>();
			rList.add(new Substitutions());
			if(hERe(m,node,rList,true))
			{
				// vere
				for(int j=0;j<rList.size();j++)
				{
					Substitutions r = rList.get(j);
					Substitutions s = new Substitutions();
					NodeSet ns1 = new NodeSet();
					NodeSet ns2 = new NodeSet();
					getVars(m,ns1);
					getVars(node,ns2);
					Substitutions sb = new Substitutions();
					Substitutions tb = new Substitutions();
					boolean flag = true;
					for(int k=0;k<ns1.size();k++)
					{
						Node n = vERe((VariableNode) ns1.getNode(k),r,s);
						if(n == null)
						{
							flag = false;
						}else
						{
							sb.putIn(new Binding((VariableNode) ns1.getNode(k),n));
						}
					}
					for(int k=0;k<ns2.size();k++)
					{
						Node n = vERe((VariableNode) ns2.getNode(k),r,s);
						if(n == null)
						{
							flag = false;
						}else
						{
							tb.putIn(new Binding((VariableNode) ns2.getNode(k),n));
						}
					}
					if(flag)
					{
						Object[] o = new Object[3];
						o[0] = m;
						o[1] = sb;
						o[2] = tb;
						result.add(o);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * updates the nodeSet by adding to it the variables dominated by the given node
	 * 
	 * @param node a MolecularNode to get the Variables dominated by it
	 * @param nodeSet a NodeSet to put the variables in
	 */
	public void getVars(MolecularNode node,NodeSet nodeSet)
	{
		CableSet cs = node.getCableSet();
		for(int i=0;i<cs.size();i++)
		{
			Cable c = cs.getCable(i);
			NodeSet ns = c.getNodeSet();
			for(int j=0;j<ns.size();j++)
			{
				Node n = ns.getNode(j);
				if(n.getClass().getSimpleName().equals("VariableNode"))
				{
					nodeSet.addNode(n);
				}else
				{
					if(n.getClass().getSuperclass().getSimpleName().equals("MolecularNode"))
						getVars((MolecularNode) n,nodeSet);
				}
			}
		}
	}
	
	public boolean hERe(Node u,Node t,LinkedList<Substitutions> rList,boolean rightOrder)
	{
		System.out.println("here    >>>>> 1");
		// if not the same constants
		if((u.getClass().getSimpleName().equals("BaseNode") &&
				t.getClass().getSimpleName().equals("ClosedNode")) || 
				(u.getClass().getSimpleName().equals("ClosedNode") &&
				t.getClass().getSimpleName().equals("BaseNode")) ||
				(u.getClass().getSimpleName().equals("BaseNode") &&
				t.getClass().getSimpleName().equals("BaseNode") &&
				t != u)||(t != u && 
				u.getClass().getSimpleName().equals("ClosedNode") &&
				t.getClass().getSimpleName().equals("ClosedNode")) || 
				(u.getClass().getSimpleName().equals("PatternNode") && 
				t.getClass().getSimpleName().equals("BaseNode")) || 
				(t.getClass().getSimpleName().equals("PatternNode") && 
				u.getClass().getSimpleName().equals("BaseNode")))
		{
			System.out.println("here    >>>>> 2");
			return false;
		}
		// if one is variable
		if(u.getClass().getSimpleName().equals("VariableNode"))
		{
			System.out.println("here    >>>>> 3");
			if(! varHERe((VariableNode) u,t,rList,rightOrder))
				return false;
		}else
		{
			if(t.getClass().getSimpleName().equals("VariableNode"))
			{
				System.out.println("here    >>>>> 4");
				if(! varHERe((VariableNode) t,u,rList,!rightOrder))
				{
					return false;
				}
			}else
			{
				// if both are molecular nodes
				if(u.getClass().getSimpleName().equals("PatternNode") ||
						t.getClass().getSimpleName().equals("PatternNode"))
				{
					System.out.println("here    >>>>> 5");
					MolecularNode t1 = (MolecularNode) u;
					MolecularNode t2 = (MolecularNode) t;
					if(t1.getCableSet().getCaseFrame() != t2.getCableSet().getCaseFrame())
						return false;
					// checking the node sets in the cables
					for(int i=0;i<t1.getCableSet().size();i++)
					{
						System.out.println("here    >>>>> 6");
						Relation r = t1.getCableSet().getCable(i).getRelation();
						NodeSet ns1 = t1.getCableSet().getCable(r.getName()).getNodeSet();
						NodeSet ns2 = t2.getCableSet().getCable(r.getName()).getNodeSet();
						if(rightOrder && ((r.getAdjust().equals("reduce") &&
							ns2.size() > ns1.size()) ||
							(r.getAdjust().equals("expand") &&
							ns2.size() < ns1.size()) ||
							(r.getAdjust().equals("none") &&
							ns1.size() != ns2.size())))
							return false;
						if((!rightOrder) && ((r.getAdjust().equals("expand") &&
								ns2.size() > ns1.size()) ||
								(r.getAdjust().equals("reduce") &&
								ns2.size() < ns1.size()) ||
								(r.getAdjust().equals("none") &&
								ns1.size() != ns2.size())))
								return false;
						System.out.println("here    >>>>> 7");
						if(setUnify(ns1,ns2,rList,rightOrder))
						{
							System.out.println("here    >>>>> 8  size >>> "+rList.size());
							continue;
						}
						if(rList.size() == 0)
							return false;
					}
				}
			}
		}
		for(int i=0;i<rList.size();i++)
		{
			System.out.print(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> "+i+" TEST :  ");
			Substitutions s = rList.get(i);
			System.out.print("( "+u.getIdentifier()+" "+t.getIdentifier()+" )  >>  ");
			for(int j=0;j<s.cardinality();j++)
			{
				System.out.print(s.getBinding(j).getVariable().getIdentifier()+"-"+s.getBinding(j).getNode().getIdentifier()+"    ");
			}
			System.out.println();
		}
		return true;
	}
	
	public boolean setUnify(NodeSet ns1,NodeSet ns2,LinkedList<Substitutions> r,boolean rightOrder)
	{
		System.out.println("setunify    >>>>> 1");
		if(ns1.size() == 0 || ns2.size() == 0)
			return true;
		boolean flag = false;
		// loop for ns1
		int size = r.size();
		for(int k=0;k<size;k++)
		{
			Substitutions rSub = new Substitutions();
			rSub.insert(r.get(0));
			r.remove(0);
			for(int i=0;i<ns1.size();i++)
			{
				System.out.println("setunify    >>>>> 2");
				Node n1 = ns1.getNode(i);
				NodeSet n1Others = new NodeSet();
				n1Others.addAll(ns1);
				n1Others.removeNode(n1);
				// loop for ns2
				for(int j=0;j<ns2.size();j++)
				{
					System.out.println("setunify    >>>>> 3");
					Node n2 = ns2.getNode(j);
					NodeSet n2Others = new NodeSet();
					n2Others.addAll(ns2);
					n2Others.removeNode(n2);
					Substitutions s = new Substitutions();
					for(int w=0;w<rSub.cardinality();w++)
					{
						s.putIn(new Binding(rSub.getBinding(w).getVariable(),rSub.getBinding(w).getNode()));
					}
					LinkedList<Substitutions> rr = new LinkedList<Substitutions>();
					rr.add(s);
					if(hERe(n1,n2,rr,rightOrder))
					{
						System.out.println("setunify    >>>>> 4");
						if(setUnify(n1Others,n2Others,rr,rightOrder))
						{
							System.out.println(r.size()+"setunify    >>>>> 5");
							for(int z=0;z<rr.size();z++)
							{
								r.add(rr.get(z));
							}

							System.out.println(r.size()+"setunify    >>>>> 6");
							flag = true;
						}
					}
				}
			}
		}
		return flag;
	}
	
	public boolean varHERe(VariableNode u,Node t,LinkedList<Substitutions> rList,boolean rightOrder)
	{
		System.out.println("varhere    >>>>> 1  " + u.getIdentifier() + t.getIdentifier());
		boolean flag = false;
		int size = rList.size();
		System.out.println(size);
		for(int i=0;i<size;i++)
		{
			Substitutions rSub = rList.get(0);
			rList.remove(0);
			if((! rSub.isBound(u)) && (!u.isRLoop()))
			{
				System.out.println(i+" varhere    >>>>> 2  " + u.getIdentifier());
				rSub.putIn(new Binding(u,t));
				u.setRLoop(false);
				System.out.println("size >>>>>>>>>>>>>>  "+rSub.cardinality());
				rList.add(rSub);
				flag = true;
			}else{
			if(! t.getClass().getSimpleName().equals("VariableNode"))
			{
				System.out.println(i+" varhere    >>>>> 3");
				Stack<VariableNode> path = source(u,rSub);
				if(! path.isEmpty())
				{
					VariableNode v = path.pop();
					collapse(path,v,rSub);
					if(((! rSub.isBound(v)) || (rSub.getBindingByVariable(v).getNode().equals(v))) && (! v.isRLoop()))
					{
						rSub.putIn(new Binding(v,t));
						v.setRLoop(false);
						rList.add(rSub);
						flag = true;
					}
					else{
					LinkedList<Substitutions> temp = new LinkedList<Substitutions>();
					temp.add(rSub);
					if(recurHERe(v,(Node) rSub.getBindingByVariable(v).getNode(),t,temp,rightOrder))
					{
						rList.addAll(temp);
						flag = true;
					}
					}
				}
			}else
			{
				System.out.println(i+" varhere    >>>>> 4");
				VariableNode tt = (VariableNode) t;
				if((! rSub.isBound(tt)) && (! tt.isRLoop()))
				{
					rSub.putIn(new Binding(tt,u));
					tt.setRLoop(false);
					rList.add(rSub);
					flag = true;
				}
				else
				{
				System.out.println(i+" varhere    >>>>> 5");
				Stack<VariableNode> path = source(u,rSub);
				if(! path.isEmpty())
				{
				VariableNode v = path.pop();
				if((! rSub.isBound(v)) && (! v.isRLoop()))
				{
					path.push(v);
					collapse(path,t,rSub);
					rList.add(rSub);
					flag = true;
				}else
				{
					System.out.println(i+" varhere    >>>>> 6");
					if(rSub.getBindingByVariable(v).getNode().equals(v) && (! v.isRLoop()))
					{
						path.push(v);
						collapse(path,t,rSub);
						rList.add(rSub);
						flag = true;
					}else
					{
						System.out.println(i+" varhere    >>>>> 7");
						Stack<VariableNode> path2 = source(tt,rSub);
						VariableNode w = path2.pop();
						path.addAll(path2);
						if(v.equals(w))
						{
							collapse(path,v,rSub);
							rList.add(rSub);
							flag = true;
						}else{
							System.out.println(i+" varhere    >>>>> 8");
							Node z = (Node) rSub.getBindingByVariable(w).getNode();
							path.push(w);
							collapse(path,v,rSub);
							if(z != null)
							{
								if((! z.equals(w)) && (! w.isRLoop()))
								{
									LinkedList<Substitutions> temp = new LinkedList<Substitutions>();
									temp.add(rSub);
									if(
								recurHERe(v,(Node)rSub.getBindingByVariable(v).getNode(),z,temp,rightOrder))
									{
										rList.addAll(temp);
										flag = true;
									}
								}
							}
						}
					}
				}
			}}}}
		}
		System.out.println("varhere    >>>>> 9");
		return flag;
	}
	
	public Stack<VariableNode> source(VariableNode x,Substitutions rSub)
	{
		System.out.println("source    >>>>> 1");
		Stack<VariableNode> path = new Stack<VariableNode>();
		path.push(x);
		while(true)
		{
			VariableNode v = path.peek();
			if(rSub.isBound(v) &&
			rSub.getBindingByVariable(v).getNode().getClass().getSimpleName().equals("VariableNode") &&
			!rSub.getBindingByVariable(v).getNode().equals(v))
			{
				path.push((VariableNode) rSub.getBindingByVariable(v).getNode());
				rSub.getBindingByVariable(v).setNode(v);
				v.setRLoop(false);
			}else
				break;
		}
		if(path.lastElement().equals(path.firstElement()) && path.size()>1)
		{
			path.pop();
		}
		System.out.print("             Path: ");
		for(int i=0;i<path.size()-1;i++)
		{
			System.out.print(path.get(i).getIdentifier()+" ");
		}
		for(int i=path.size()-1;i<path.size();i++)
		{
			System.out.println(path.get(i).getIdentifier());
		}
		return path;
	}
	
	public void collapse(Stack<VariableNode> path,Node v,Substitutions rSub)
	{
		System.out.println("collapse    >>>>> 1");
		for(int i=0;i<path.size();i++)
		{
			if(rSub.isBound(path.get(i)))
			{
				rSub.getBindingByVariable(path.get(i)).setNode(v);
				path.get(i).setRLoop(false);
			}else
				rSub.putIn(new Binding(path.get(i),v));
				path.get(i).setRLoop(false);
		}
		System.out.print("           collapse: ( ");
		for(int i=0;i<path.size();i++)
		{
			System.out.print(path.get(i).getIdentifier()+" ");
		}
		System.out.println(") > "+v.getIdentifier());
	}
	
	public boolean recurHERe(VariableNode v,Node y,Node t,LinkedList<Substitutions> rList,boolean rightOrder)
	{
		System.out.println("recurhere    >>>>> 1");
		boolean flag = false;
		v.setRLoop(true);
		if(hERe(y,t,rList,rightOrder))
		{
			for(int i=0;i<rList.size();i++)
			{
				Substitutions rSub = rList.get(i);
				if(rSub.isBound(v))
				{
					rSub.getBindingByVariable(v).setNode(y);
					v.setRLoop(false);
				}else
				{
					rSub.putIn(new Binding(v,y));
					v.setRLoop(false);
				}
			}
			flag = true;
		}
		v.setRLoop(false);
		return flag;
	}
	
	public Node vERe(VariableNode u,Substitutions r,Substitutions s)
	{
		//System.out.println("vere    >>>>> 1");
		Node z = null;
		Stack<VariableNode> path = source(u,r);
		VariableNode v = path.pop();
		if(! r.isBound(v))
		{
			//System.out.println("vere    >>>>> 2");
			z = v;
			r.putIn(new Binding(v,v)); // done
		}
		else
		{
			//System.out.println("vere    >>>>> 3");
			if(r.getBindingByVariable(v).getNode().getClass().getSimpleName().equals("BaseNode")
				|| r.getBindingByVariable(v).getNode().getClass().getSimpleName().equals("ClosedNode"))
			{
			//	System.out.println("vere    >>>>> 4");
				z = r.getBindingByVariable(v).getNode();
				if(! s.isBound(v))
					s.putIn(new Binding(v,z));
				s.getBindingByVariable(v).setNode(z);
				v.setSLoop(false);
				r.getBindingByVariable(v).setNode(v); // done
			}
			else
			{
				Node yy = r.getBindingByVariable(v).getNode();
				if(yy.getClass().getSimpleName().equals("PatternNode"))
				{
				//	System.out.println("vere    >>>>> 5");
					MolecularNode y = (MolecularNode) yy;
					r.getBindingByVariable(v).setNode(v); // done
					v.setSLoop(true);
					for(int i=0;i<path.size();i++)
					{
						path.get(i).setSLoop(true);
					}
					z = termVERe(y,r,s);
					if(z == null)
					{
						return null;
					}
					if(! s.isBound(v))
						s.putIn(new Binding(v,z));
					s.getBindingByVariable(v).setNode(z);
					v.setSLoop(false);
				}
				else
				{
					if(v.isSLoop())
					{
					//	System.out.println("vere    >>>>> 6");
						v.setSLoop(false);
						return null;
					}
					else
					{
						if(! s.isBound(v))
							z = v;
						else
							z = s.getBindingByVariable(v).getNode();
					}
				}
			}
		}
		for(int i=0;i<path.size();i++)
		{
			if(! s.isBound(path.get(i)))
				s.putIn(new Binding(path.get(i),z));
			s.getBindingByVariable(path.get(i)).setNode(z);
			v.setSLoop(false);
		}
		
		return z;
	}
	
	public MolecularNode termVERe(MolecularNode t,Substitutions r,Substitutions s)
	{
		//System.out.println("termvere    >>>>> 1");
		// list for pattern nodes substitutions
		LinkedList<LinkedList<Object>> temp = new LinkedList<LinkedList<Object>>();
		CableSet cs = t.getCableSet();
		for(int i=0;i<cs.size();i++)
		{
			Cable c = cs.getCable(i);
			Relation rel = c.getRelation();
			NodeSet ns = c.getNodeSet();
			if(ns.size() == 0)
			{
				LinkedList<Object> list = new LinkedList<Object>();
				list.add(rel);
				list.add(new NodeSet());
				temp.add(list);
				continue;
			}
			for(int j=0;j<ns.size();j++)
			{
				Node n = ns.getNode(j);
				if(n.getClass().getSimpleName().equals("BaseNode") 
						|| n.getClass().getSimpleName().equals("ClosedNode"))
				{
					LinkedList<Object> list = new LinkedList<Object>();
					list.add(rel);
					list.add(n);
					temp.add(list);
				}
				else
				{
					if(n.getClass().getSimpleName().equals("PatternNode"))
					{
						LinkedList<Object> list = new LinkedList<Object>();
						list.add(rel);
						list.add(termVERe((MolecularNode) n,r,s));
						temp.add(list);
					}
					else
					{
						if(n.getClass().getSimpleName().equals("VariableNode"))
						{
							VariableNode v = (VariableNode) n;
							// if done
							if(r.isBound(v) && r.getBindingByVariable(v).getNode().equals(v))
							{
								if(v.isSLoop())
								{
									return null;
								}
								else
								{
									// replace in t the subterm ti by s(ti)
									LinkedList<Object> list = new LinkedList<Object>();
									list.add(rel);
									list.add(s.getBindingByVariable(v).getNode());
									temp.add(list);
								}
							}
							else
							{
								if(r.isBound(v))
								{
									LinkedList<Object> list = new LinkedList<Object>();
									list.add(rel);
									list.add(vERe(v,r,s));
									temp.add(list);
								}
							}
						}
					}
				}
			}
		}
		// creating the array used in building
		Object[][] array = new Object[temp.size()][2];
		for(int i=0;i<array.length;i++)
		{
			LinkedList<Object> list = temp.get(i);
			array[i][0] = list.get(0);
			array[i][1] = list.get(1);
		}
		
		// building the substitution node
		MolecularNode molNode = null;
		try
		{
			molNode = build(array,t.getCableSet().getCaseFrame());
		} catch (CustomException e)
		{
			try
			{
				molNode = getMolecularNode(array);
			}catch (CustomException e1)
			{
				e1.printStackTrace();
			}
		}
		
		return molNode;
	}
	
   /**
	* Added by Mohamed Karam Gabr
	* 
	* Do the deduction process on the node n in the context c and return the 
	* resulting reports set. 
	* @param n MolecularNode
	* @param c Context
	* @return ReportSet 
	*/
	public ReportSet deduce(MolecularNode n,Context c)
	{
		QueuesProcessor qp=new QueuesProcessor();
		Request r=new Request(new Channel(null,null,c,null,true));
		n.getEntity().getProcess().setFirst(true);
		qp.addToLow(n.getEntity());
		n.getEntity().getProcess().setQueuesProcessor(qp);
		n.getEntity().getProcess().receiveRequest(r);
		qp.process();
		n.getEntity().getProcess().setFirst(false);
		return n.getEntity().getProcess().getSentReports();
	}

	/**
	 * infers nodes by path-based inference to get all nodes that have the given case frame
	 * in their cable sets
	 * 
	 * @param caseFrame a case frame to infer nodes implementing it
	 * @param context the context that propositions in the inference are asserted in
	 * @return a node set of all nodes that are inferred by path based inference
	 */
	@SuppressWarnings("unchecked")
	public NodeSet pathBasedInfer(CaseFrame caseFrame,Context context)
	{
		NodeSet nodes = new NodeSet();
		
		// loop over nodes
		Hashtable<String,Node> h = (Hashtable<String,Node>) this.nodes.clone();
		for(Enumeration<Node> e = h.elements();e.hasMoreElements();)
		{
			Node node = e.nextElement();
			
			// if it is not as asserted proposition
			if(! node.getEntity().getSuperClasses().contains("Proposition"))
				if(! node.getEntity().getClass().getSimpleName().equals("Proposition"))
					continue;
			
			Node n = this.pathBasedInfer(node,caseFrame,context);
			
			if(n != null)
				nodes.addNode(n);
		}
		
		return nodes;
	}
	
	/**
	 * @param array the array that contains pairs of paths and node sets
	 * @return the node set of nodes that we can start following those paths in the array
	 * from, in order to reach at least one node at each node set in all entries of the 
	 * array 
	 */
	public LinkedList<Object[]> find(Object[][] array,Context context)
	{
		return findIntersection(array,context,0);
	}
	
	/**
	 * @param array the array that contains pairs of paths and node sets
	 * @return the node set of non-variable nodes that we can start following those paths 
	 * in the array from, in order to reach at least one node at each node set in all
	 * entries of the array 
	 */
	public LinkedList<Object[]> findConstant(Object[][] array,Context context)
	{
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		LinkedList<Object[]> h = find(array,context);
		for(int i=0;i<h.size();i++)
		{
			Object[] o = h.get(i);
			Node n = (Node) o[0];
			if(n.getClass().getSimpleName().equals("BaseNode") || 
					n.getClass().getSimpleName().equals("ClosedNode"))
				result.add(o);
		}
		return result;
	}
	
	/**
	 * @param array the array that contains pairs of paths and node sets
	 * @return the node set of base nodes that we can start following those paths 
	 * in the array from, in order to reach at least one node at each node set in all
	 * entries of the array 
	 */
	public LinkedList<Object[]> findBase(Object[][] array,Context context)
	{
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		LinkedList<Object[]> h = find(array,context);
		for(int i=0;i<h.size();i++)
		{
			Object[] o = h.get(i);
			Node n = (Node) o[0];
			if(n.getClass().getSimpleName().equals("BaseNode"))
				result.add(o);
		}
		return result;
	}
	
	/**
	 * @param array the array that contains pairs of paths and node sets
	 * @return the node set of variable nodes that we can start following those paths 
	 * in the array from, in order to reach at least one node at each node set in all
	 * entries of the array 
	 */
	public LinkedList<Object[]> findVariable(Object[][] array,Context context)
	{
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		LinkedList<Object[]> h = find(array,context);
		for(int i=0;i<h.size();i++)
		{
			Object[] o = h.get(i);
			Node n = (Node) o[0];
			if(n.getClass().getSimpleName().equals("VariableNode"))
				result.add(o);
		}
		return result;
	}
	
	/**
	 * @param array the array that contains pairs of paths and node sets
	 * @return the node set of pattern nodes that we can start following those paths 
	 * in the array from, in order to reach at least one node at each node set in all
	 * entries of the array 
	 */
	public LinkedList<Object[]> findPattern(Object[][] array,Context context)
	{
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		LinkedList<Object[]> h = find(array,context);
		for(int i=0;i<h.size();i++)
		{
			Object[] o = h.get(i);
			Node n = (Node) o[0];
			if(n.getClass().getSimpleName().equals("PatternNode"))
				result.add(o);
		}
		return result;
	}
	
	/**
	 * @param path the path that can be followed to get to one of the nodes specified
	 * @param nodeSet the nodes that can be reached by following the path
	 * @return a node set of nodes that we can start following the path from in order to
	 * get to one of the nodes in the specified node set
	 */
	private LinkedList<Object[]> findUnion(Path path,NodeSet nodeSet,Context context)
	{
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		for(int i=0;i<nodeSet.size();i++)
		{
			LinkedList<Object[]> temp = path.followConverse(nodeSet.getNode(i),new PathTrace(),context);
			result.addAll(temp);
		}
		
		return result;
	}
	
	/**
	 * @param array the array that contains pairs of paths and node sets
	 * @param index the index of the array at which we should start traversing it
	 * @return the node set of nodes that we can start following those paths in the array
	 * from, in order to reach at least one node of node sets at each path-nodeset pair. 
	 */
	private LinkedList<Object[]> findIntersection(Object[][] array,Context context,int index)
	{
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		if(index == array.length)
			return result;
		
		Path path = (Path) array[index][0];
		NodeSet nodeSet = (NodeSet) array[index][1];
		
		if(index < array.length-1)
		{
			LinkedList<Object[]> list1 = findUnion(path,nodeSet,context);
			LinkedList<Object[]> list2 = findIntersection(array,context,++index);
			for(int i=0;i<list1.size();i++)
			{
				Object[] ob1 = list1.get(i);
				Node n1 = (Node) ob1[0];
				PathTrace pt1 = (PathTrace) ob1[1];
				for(int j=0;j<list2.size();j++)
				{
					Object[] ob2 = list2.get(j);
					Node n2 = (Node) ob2[0];
					PathTrace pt2 = (PathTrace) ob2[1];
					if(n1.equals(n2))
					{
						PathTrace pt = pt1.clone();
						pt.and(pt2.getPath());
						pt.addAllSupports(pt2.getSupports());
						Object[] o = {n1,pt};
						result.add(o);
					}
				}
			}
		}
		else
		{
			result.addAll(findUnion(path,nodeSet,context));
		}
		
		return result;
	}
	
	/**
	 * path-based infers a given case frame for a given node
	 * 
	 * @param node a node to path-based infer the given case frame for it
	 * @param caseFrame a case frame to infer for the given node
	 * @param context a context for propositions in the paths to be asserted in
	 * @return a node resulted from inferring the case frame from the given node 
	 * if succeeded, and null otherwise
	 */
	private Node pathBasedInfer(Node node,CaseFrame caseFrame,Context context)
	{
		//NodeSet ns = new NodeSet();
		
		LinkedList<Relation> relations = caseFrame.getRelations();
		
		LinkedList<LinkedList<Object[]>> list = new LinkedList<LinkedList<Object[]>>();

		// loop over relations to update the list of pair-lists
		for(int i=0;i<relations.size();i++)
		{
			// follow the path of the relation
			Relation r = relations.get(i);
			Path path;
			if(r.getPath() == null)
				path = new FUnitPath(r);
			else
				path = r.getPath();
			LinkedList<Object[]> l = path.follow(node,new PathTrace(),context);
			if(l.isEmpty() && r.getLimit()>0)
				break;
			if(l.size()<r.getLimit())
				break;
			list.add(l);
		}
		// if one of the resulted lists is not valid
		if(list.size()<relations.size())
			return null;
		
		if(node.getClass().getSuperclass().getSimpleName().equals("MolecularNode"))
		{
			// get all possible combinations of nodes reached by the following
			/*LinkedList<Object[][]> result = new LinkedList<Object[][]>();
			if(! list.isEmpty())
				for(int i=0;i<list.get(0).size();i++)
				{
					Object[][] x = new Object[list.size()][2];
					x[0] = list.get(0).get(i);
					result.add(x);
				}
			
			for(int i=1;i<list.size();i++)
			{
				LinkedList<Object[][]> temp = new LinkedList<Object[][]>(result);
				for(int j=1;j<list.get(i).size();j++)
				{
					for(int k=0;k<temp.size();k++)
					{
						result.add(temp.get(k).clone());
					}
				}
				for(int j=0;j<list.get(i).size();j++)
				{
					int count = j*temp.size();
					while(count < (j+1)*temp.size())
					{
						result.get(count)[i] = list.get(i).get(j);
						count++;
					}
				}
			}*/
			
			// filter result by removing non-valid cable sets (if the rest are not reducible to 0)
			/*for(int i=0;i<result.size();i++)
			{
				Object[][] x = result.get(i);
				LinkedList<Relation> cf = (LinkedList<Relation>) ((MolecularNode) node).getCableSet().getCaseFrame().getRelations().clone();
				LinkedList<Relation> y = new LinkedList<Relation>();
				for(int j=0;j<x.length;j++)
				{
					y.addAll(((PathTrace) x[j][1]).getFirst());
				}
				cf.removeAll(y);
				for(int j=0;j<cf.size();j++)
				{
					if(! ((cf.get(j).getLimit() == 0) && (cf.get(j).getAdjust().equals("reduce"))))
					{
						result.remove(i);
						i--;
						break;
					}
				}
			}*/
			LinkedList<Relation> cf = new LinkedList<Relation>(((MolecularNode) node).getCableSet().getCaseFrame().getRelations());
			LinkedList<Relation> y = new LinkedList<Relation>();
			for(int i=0;i<list.size();i++)
			{
				LinkedList<Object[]> l = list.get(i);
				for(int j=0;j<l.size();j++)
				{
					Object[] o = l.get(j);
					y.addAll(((PathTrace) o[1]).getFirst());
				}
			}
			cf.removeAll(y);
			for(int i=0;i<cf.size();i++)
			{
				if(! ((cf.get(i).getLimit() == 0) && (cf.get(i).getAdjust().equals("reduce"))))
				{
					return null;
				}
			}
			
			// build the result nodes
			/*for(int i=0;i<result.size();i++)
			{
				Object[][] x = result.get(i);
				Object[][] y = new Object[relations.size()][2];
				for(int j=0;j<y.length;j++)
				{
					y[j][0] = relations.get(j);
					y[j][1] = x[j][0];
				}
				try {
					Node n = this.build(y,caseFrame);
					ns.addNode(n);
					this.simulateParentNodes(n,node);
				} catch (CustomException e1)
				{
					try {
						ns.addNode(this.getMolecularNode(y));
					} catch (CustomException e2)
					{
						e2.printStackTrace();
					}
				}
			}*/
			LinkedList<Node> nodes = new LinkedList<Node>();
			LinkedList<Relation> rels = new LinkedList<Relation>();
			for(int i=0;i<list.size();i++)
			{
				LinkedList<Object[]> l = list.get(i);
				for(int j=0;j<l.size();j++)
				{
					rels.add(relations.get(i));
					nodes.add((Node) l.get(j)[0]);
				}
			}
			Object[][] array = new Object[rels.size()][2];
			for(int i=0;i<rels.size();i++)
			{
				array[i][0] = rels.get(i);
				array[i][1] = nodes.get(i);
			}
			try {
				Node n = this.build(array,caseFrame);
			//	this.simulateParentNodes(n,node);
				return n;
			} catch (CustomException e1)
			{
				try {
					return this.getMolecularNode(array);
				} catch (CustomException e2)
				{
					e2.printStackTrace();
				}
			}
		}
		else
		{
			// get all possible combinations of nodes reached by following paths
			
			/*LinkedList<Object[][]> result = new LinkedList<Object[][]>();
			if(! list.isEmpty())
				for(int i=0;i<list.get(0).size();i++)
				{
					Object[][] x = new Object[list.size()][2];
					x[0] = list.get(0).get(i);
					result.add(x);
				}
			
			for(int i=1;i<list.size();i++)
			{
				LinkedList<Object[][]> temp = new LinkedList<Object[][]>(result);
				for(int j=1;j<list.get(i).size();j++)
				{
					for(int k=0;k<temp.size();k++)
					{
						result.add(temp.get(k).clone());
					}
				}
				for(int j=0;j<list.get(i).size();j++)
				{
					int count = j*temp.size();
					while(count < (j+1)*temp.size())
					{
						result.get(count)[i] = list.get(i).get(j);
						count++;
					}
				}
			}*/
			
			LinkedList<Relation> cf = new LinkedList<Relation>(((MolecularNode) node).getCableSet().getCaseFrame().getRelations());
			LinkedList<Relation> y = new LinkedList<Relation>();
			for(int i=0;i<list.size();i++)
			{
				LinkedList<Object[]> l = list.get(i);
				for(int j=0;j<l.size();j++)
				{
					Object[] o = l.get(j);
					y.addAll(((PathTrace) o[1]).getFirst());
				}
			}
			cf.removeAll(y);
			for(int i=0;i<cf.size();i++)
			{
				if(! ((cf.get(i).getLimit() == 0) && (cf.get(i).getAdjust().equals("reduce"))))
				{
					return null;
				}
			}
			
			// build the result nodes
			/*for(int i=0;i<result.size();i++)
			{
				Object[][] x = result.get(i);
				Object[][] y = new Object[relations.size()][2];
				for(int j=0;j<y.length;j++)
				{
					y[j][0] = relations.get(j);
					y[j][1] = x[j][0];
				}
				try {
					Node n = this.build(y,caseFrame);
					ns.addNode(n);
					this.simulateParentNodes(n,node);
				} catch (CustomException e1)
				{
					try {
						ns.addNode(this.getMolecularNode(y));
					} catch (CustomException e2)
					{
						e2.printStackTrace();
					}
				}
			}*/
			LinkedList<Node> nodes = new LinkedList<Node>();
			LinkedList<Relation> rels = new LinkedList<Relation>();
			for(int i=0;i<list.size();i++)
			{
				LinkedList<Object[]> l = list.get(i);
				for(int j=0;j<l.size();j++)
				{
					rels.add(relations.get(i));
					nodes.add((Node) l.get(j)[0]);
				}
			}
			Object[][] array = new Object[rels.size()][2];
			for(int i=0;i<rels.size();i++)
			{
				array[i][0] = rels.get(i);
				array[i][1] = nodes.get(i);
			}
			try {
				Node n = this.build(array,caseFrame);
			//	this.simulateParentNodes(n,node);
				return n;
			} catch (CustomException e1)
			{
				try {
					return this.getMolecularNode(array);
				} catch (CustomException e2)
				{
					e2.printStackTrace();
				}
			}
			
		}
		return null;
	}
	
	/**
	 * creates copies of parent nodes of the old node, and those parent nodes dominate 
	 * the given node
	 * 
	 * @param node a Node to be dominated by copies of parent nodes of the old node
	 * @param oldNode a Node to simulate its parent nodes for the given node
	 */
	@SuppressWarnings("unused")
	private void simulateParentNodes(Node node,Node oldNode)
	{
		UpCableSet upCableSet = oldNode.getUpCableSet();
		// loop over up cables
		for(int i=0;i<upCableSet.size();i++)
		{
			UpCable uc = upCableSet.getUpCable(i);
			NodeSet ucns = uc.getNodeSet();
			// loop over parent nodes
			for(int j=0;j<ucns.size();j++)
			{
				MolecularNode mn = (MolecularNode) ucns.getNode(j);
				LinkedList<Relation> relations = new LinkedList<Relation>();
				LinkedList<Node> nodes = new LinkedList<Node>();
				CableSet cs = mn.getCableSet();
				// loop over cables for the parent nodes
				for(int x=0;x<cs.size();x++)
				{
					Cable c = cs.getCable(x);
					NodeSet cns = c.getNodeSet();
					// loop over child nodes of parent nodes
					for(int y=0;y<cns.size();y++)
					{
						Node n = cns.getNode(y);
						relations.add(c.getRelation());
						if(n.equals(oldNode))
							nodes.add(node);
						else
							nodes.add(n);
					}
				}
				// build the new node
				Object[][] array = new Object[relations.size()][2];
				for(int z=0;z<relations.size();z++)
				{
					array[z][0] = relations.get(z);
					array[z][1] = nodes.get(z);
				}
				Node newnode = null;
				try {
					newnode = this.build(array,cs.getCaseFrame());
				} catch (CustomException e)
				{
					try {
						newnode = this.getMolecularNode(array);
					} catch (CustomException e1)
					{
						e1.printStackTrace();
					}
				}
				// simulate parent nodes for the new node
				this.simulateParentNodes(newnode,mn);
			}
		}
	}
	
	/**
	 * creates a copy of the given molecular node in the first parameter with variables 
	 * that are shared between it and the other node are replace with new ones and returns 
	 * a substitutions of the replaced variables bound to the new ones along with the new 
	 * nodes
	 * 
	 * @param u a molecular node
	 * @param t a molecular node
	 * @return an array of Objects of size 3 where the first cell is the substitutions
	 * resulted from renaming the variables, the second cell is the copy of u with shared 
	 * variables replaced by new ones and the third cell is t
	 */
	@SuppressWarnings("unused")
	private Object[] renameSharedVariables(MolecularNode u,MolecularNode t)
	{
		Object[] o = new Object[3];
		o[1] = u;
		Substitutions result = new Substitutions();
		
		NodeSet uVariables = getAllVariables(u);
		NodeSet tVariables = getAllVariables(t);
		NodeSet shared = uVariables.Intersection(tVariables);
		
		if(! shared.isEmpty())
		{
			// rename variables
			for(int i=0;i<shared.size();i++)
			{
				VariableNode v = this.buildVariableNode();
				result.putIn(new Binding((VariableNode) shared.getNode(i),v));
			}
			MolecularNode newu = renameAndCopy(u,result);
			o[1] = newu;
		}
		o[0] = result;
		o[2] = t;
		
		return o;
	}
	
	/**
	 * creates a copy of the given molecular node by replacing the variables that are 
	 * bound in the given substitutions by the variables they are bound to
	 * 
	 * @param node a molecular node to create a copy for
	 * @param s a substitutions of shared variables bound to new variables
	 * @return a molecular node which is the one with variables replaced by the new 
	 * variables from the given substitutions
	 */
	private MolecularNode renameAndCopy(MolecularNode node,Substitutions s)
	{
		LinkedList<Relation> r = new LinkedList<Relation>();
		LinkedList<Node> rn = new LinkedList<Node>();
		for(int i=0;i<node.getCableSet().size();i++)
		{
			Cable c = node.getCableSet().getCable(i);
			NodeSet ns = c.getNodeSet();
			for(int j=0;j<ns.size();j++)
			{
				Node n = ns.getNode(j);
				if(n.getClass().getSimpleName().equals("VariableNode"))
				{
					if(s.isBound((VariableNode) n))
					{
						r.add(c.getRelation());
						rn.add(s.term((VariableNode) n));
					}
					else
					{
						r.add(c.getRelation());
						rn.add(n);
					}
				}
				else
				{
					if(n.getClass().getSuperclass().getSimpleName().equals("MolecularNode"))
					{
						r.add(c.getRelation());
						rn.add(renameAndCopy((MolecularNode) n,s));
					}
					else
					{
						r.add(c.getRelation());
						rn.add(n);
					}
				}
				
			}
		}
		Object[][] array = new Object[r.size()][2];
		for(int i=0;i<r.size();i++)
		{
			array[i][0] = r.get(i);
			array[i][1] = rn.get(i);
		}
		
		MolecularNode result = null;
		try {
			 result = build(array,node.getCableSet().getCaseFrame());
		} catch (CustomException e)
		{
			if(e.getMessage().equals("cable set already exists"))
			{
				try {
					result = getMolecularNode(array);
				} catch (CustomException e1)
				{
					e1.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * gets all the variable nodes dominated by the given molecular node
	 * 
	 * @param node a MolecularNode to get variables dominated by it
	 * @return a node set of variable nodes that are dominated by the given molecular node
	 */
	private NodeSet getAllVariables(MolecularNode node)
	{
		NodeSet result = new NodeSet();
		
		for(int i=0;i<node.getCableSet().size();i++)
		{
			Cable c = node.getCableSet().getCable(i);
			NodeSet ns = c.getNodeSet();
			for(int j=0;j<ns.size();j++)
			{
				Node n = ns.getNode(j);
				if(n.getClass().getSimpleName().equals("VariableNode"))
				{
					result.addNode(n);
				}
				if(n.getClass().getSuperclass().getSimpleName().equals("MolecularNode"))
				{
					result.addAll(getAllVariables((MolecularNode) n));
				}
			}
		}
		
		return result;
	}
	
	/**
	 * checks whether the given cable set already exists in the network or not
	 * 
	 * @param array a 2D array of Relation-Node pairs representing a cable set
	 * @return true if the CableSet exists, and false otherwise
	 */
	private boolean cableSetExists(Object[][] array)
	{
		int size = 0;
		for(int i=0;i<array.length;i++)
		{
			if(! array[i][1].getClass().getSimpleName().equals("NodeSet"))
				size++;
		}
		Object[][] temp = new Object[size][2];
		int counter = 0;
		for(int i=0;i<array.length;i++)
		{
			if(array[i][1].getClass().getSimpleName().equals("NodeSet"))
				continue;
			temp[counter][0] = new FUnitPath((Relation) array[i][0]);
			NodeSet ns1 = new NodeSet();
			ns1.addNode((Node) array[i][1]);
			temp[counter][1] = ns1;
			counter++;
		}
		LinkedList<Object[]> ns = this.find(temp,new Context());
		
		for(int j=0;j<ns.size();j++)
		{
			Object[] x = ns.get(j);
			MolecularNode n = (MolecularNode) x[0];
			for(int i=0;i<array.length;i++)
			{
				if(array[i][1].getClass().getSimpleName().equals("NodeSet"))
				{
					if(n.getCableSet().contains(((Relation) array[i][0]).getName()) && 
						n.getCableSet().getCable(((Relation) array[i][0]).getName()).getNodeSet().isEmpty())
						continue;
					else
					{
						ns.remove(j);
						j--;
					}
				}
			}
		}
		for(int i=0;i<ns.size();i++)
		{
			Object[] x = ns.get(i);
			MolecularNode n = (MolecularNode) x[0];
			int c = 0;
			for(int j=0;j<n.getCableSet().size();j++)
			{
				Cable cb = n.getCableSet().getCable(j);
				if(cb.getNodeSet().isEmpty())
					c++;
				else
					c += cb.getNodeSet().size();
			}
			if(c != array.length)
			{
				ns.remove(i);
				i--;
			}
		}
		
		return ns.size()==1;
	}
	
	/**
	 * checks whether the sizes of the node sets are valid according to the limit of the relations
	 * pointing to these node sets
	 * 
	 * @param array a 2D array of Relation-NodeSet pairs
	 * @return true if the sizes of the node sets are valid according to the limit of the relations,
	 * and false otherwise
	 */
	private boolean validNodeSetSize(Object[][] array)
	{
		for(int i=0;i<array.length;i++)
		{
			Relation r = (Relation) array[i][0];
			NodeSet ns = (NodeSet) array[i][1];
			if(r.getLimit() > ns.size())
				return false;
		}
		return true;
	}
	
	/**
	 * checks whether the given cable set is implementing the given case frame or not
	 * 
	 * @param array a 2D array of Relation-NodeSet pairs
	 * @param caseFrame a CaseFrame
	 * @return true if the cable set represented by the array is implementing the given case frame,
	 * and false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean followingCaseFrame(Object[][] array,CaseFrame caseFrame)
	{
		LinkedList<Relation> list = (LinkedList<Relation>) caseFrame.getRelations().clone();
		for(int i=0;i<array.length;i++)
		{
			Relation r = (Relation) array[i][0];
			if(! list.contains(r))
				return false;
			else
				list.remove(r);
		}
		if(! list.isEmpty())
			return false;
		
		return true;
	}
	
	/**
	 * this method determines whether the molecular node that contains a cable set 
	 * containing the relations and the nodes in the array should be a pattern node or not
	 * 
	 * @param array an n*2 array of relations and nodes each row consists of a relation
	 * and a node
	 * @return true if the molecular node that have this cable set of relations and nodes
	 * should be a pattern node and false if not
	 */
	private boolean isToBePattern(Object[][] array)
	{
		for(int i=0;i<array.length;i++)
		{
			if(array[i][1].getClass().getSimpleName().equals("NodeSet"))
				continue;
			Relation r = (Relation) array[i][0];
			Node node = (Node) array[i][1];
			if(node.getClass().getSimpleName().equals("VariableNode") && 
					! r.isQuantifier())
				return true;
			if(node.getClass().getSimpleName().equals("PatternNode"))
			{
				PatternNode patternNode = (PatternNode) node;
				LinkedList<VariableNode> varNodes = patternNode.getFreeVariables();
				for(int j=0;j<varNodes.size();j++)
				{
					VariableNode v = varNodes.get(j);
					boolean flag = false;
					for(int k=0;k<array.length;k++)
					{
						if(array[k][1].getClass().getSimpleName().equals("NodeSet"))
							continue;
						if(array[k][1].equals(v))
							flag = true;
					}
					if(! flag)
						return true;
				}
				
			}
		}
		return false;
	}

	/**
	 * @param array an n*2 array of relations and nodes.each row consists of two things
	 * a relation and a node pointed to by an arc labeled by this relation.
	 * @param caseFrame the case frame that should be included in the cable set
	 * in this molecular node
	 * @return the node that was just created
	 */
	@SuppressWarnings("unchecked")
	private ClosedNode createMolNode(Object[][] array,CaseFrame caseFrame)
	{
		// creating the node
		Object[][] relNodeSet = turnIntoRelNodeSet(array);
		LinkedList<Cable> cables = new LinkedList<Cable>();
		for(int i=0;i<relNodeSet.length;i++)
		{
			cables.add(new Cable((Relation) relNodeSet[i][0],(NodeSet) relNodeSet[i][1]));
		}
		CableSet cableSet = new CableSet(cables,caseFrame);
		String molName = getNextMolName();
		ClosedNode closedNode = new ClosedNode(molName,cableSet);
		try {
			Class c = Class.forName("sneps."+caseFrame.getSemanticClass());
			Constructor con = c.getConstructor(new Class[] {Node.class});
			Entity e = (Entity) con.newInstance(closedNode);
			closedNode.setEntity(e);
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return closedNode;
	}
	
	/**
	 * @param array an n*2 array of relations and nodes.each row consists of two things
	 * a relation and a node pointed to by an arc labeled by this relation.
	 * @param caseFrame the case frame that should be included in the cable set 
	 * of this node
	 * @return the node that was just created
	 */
	@SuppressWarnings("unchecked")
	private PatternNode createPatNode(Object[][] array,CaseFrame caseFrame)
	{
		// creating the node
		Object[][] relNodeSet = turnIntoRelNodeSet(array);
		LinkedList<Cable> cables = new LinkedList<Cable>();
		for(int i=0;i<relNodeSet.length;i++)
		{
			cables.add(new Cable((Relation) relNodeSet[i][0],(NodeSet) relNodeSet[i][1]));
		}
		CableSet cableSet = new CableSet(cables,caseFrame);
		String patName = getNextPatName();
		PatternNode patternNode = new PatternNode(patName,cableSet);
		try {
			Class c = Class.forName("sneps."+caseFrame.getSemanticClass());
			Constructor con = c.getConstructor(new Class[] {Node.class});
			Entity e = (Entity) con.newInstance(patternNode);
			patternNode.setEntity(e);
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return patternNode;
	}
	
	/**
	 * @param array an array of size n*2. each row of this array consists of a relation and 
	 * a node.
	 * @return an n*2 array of Objects each row consists of two things, a Relation and 
	 * a NodeSet that contains nodes that are pointed to by arcs labeled with this relation.
	 */
	private Object[][] turnIntoRelNodeSet(Object[][] array)
	{
		int relCount = 0;
		boolean exists = false;
		Object[][] tempResult = new Object[array.length][array[0].length];
		for(int i=0;i<array.length;i++)
		{
			Relation r = (Relation) array[i][0];
			NodeSet nos = new NodeSet();
			if(! array[i][1].getClass().getSimpleName().equals("NodeSet"))
			{
				nos.addNode((Node) array[i][1]);
			}
			for(int j=0;j<relCount;j++)
			{
				if(r.equals((Relation) tempResult[j][0]))
				{
					NodeSet ns = (NodeSet) tempResult[j][1];
					ns.addAll(nos);
					exists = true;
					break;
				}else
					exists = false;
			}
			if(! exists)
			{
				exists = false;
				NodeSet nodeSet = new NodeSet();
				nodeSet.addAll(nos);
				tempResult[relCount][0] = r;
				tempResult[relCount][1] = nodeSet;
				relCount++;
			}
		}
		
		Object[][] result = new Object[relCount][2];
		for(int i=0;i<relCount;i++)
		{
			result[i][0] = tempResult[i][0];
			result[i][1] = tempResult[i][1];
		}
		
		return result;
	}
	
	/**
	 * @return a String representing the closed node name that we can create with it
	 */
	private String getNextMolName()
	{
		molCounter++;
		String molName = "M";
		for(int i=0;i<userDefinedMolSuffix.size();i++)
		{
			if(userDefinedMolSuffix.get(i).intValue() == molCounter)
			{
				molCounter++;
				i=-1;
			}
		}
		molName += ""+molCounter;
		
		return molName;
	}
	
	/**
	 * @return a String representing the pattern node name that we can create with it
	 */
	private String getNextPatName()
	{
		patCounter++;
		String patName = "P";
		for(int i=0;i<userDefinedPatSuffix.size();i++)
		{
			if(userDefinedPatSuffix.get(i).intValue() == patCounter)
			{
				patCounter++;
				i=-1;
			}
		}
		patName += ""+patCounter;
		
		return patName;
	}
	
	/**
	 * @return a String representing the variable node name that we can create with it
	 */
	private String getNextVarName()
	{
		varCounter++;
		String varName = "V";
		for(int i=0;i<userDefinedVarSuffix.size();i++)
		{
			if(userDefinedVarSuffix.get(i).intValue() == varCounter)
			{
				varCounter++;
				i=-1;
			}
		}
		varName += ""+varCounter;
		
		return varName;
	}
	
	/**
	 * this method is used to get the number that follows the 'm' character if this 
	 * string is similar to the name of a closed node in order to keep track of the 
	 * names that the user used in defining nodes to maintain uniqueness of nodes names
	 * 
	 * @param identifier the name of the node
	 * @return the suffix of the identifier after the 'm' if it is similar to
	 * a closed node name but if it is not it returns -1
	 */
	private int isMolName(String identifier)
	{
		if(identifier.length()==1)
			return -1;
		if(identifier.charAt(0) != 'm' && identifier.charAt(0) != 'M')
			return -1;
		for(int i=1;i<identifier.length();i++)
		{
			if(! isInt(identifier.charAt(i)))
				return -1;
		}
		return Integer.parseInt(identifier.substring(1,identifier.length()));
	}
	
	/**
	 * this method is used to get the number that follows the 'p' character if this 
	 * string is similar to the name of a pattern node in order to keep track of the 
	 * names that the user used in defining nodes to maintain uniqueness of nodes names
	 * 
	 * @param identifier the name of the node
	 * @return the suffix of the identifier after the 'p' if it is similar to
	 * a pattern node name but if it is not it returns -1
	 */
	private int isPatName(String identifier)
	{
		if(identifier.length()==1)
			return -1;
		if(identifier.charAt(0) != 'p' && identifier.charAt(0) != 'P')
			return -1;
		for(int i=1;i<identifier.length();i++)
		{
			if(! isInt(identifier.charAt(i)))
				return -1;
		}
		return Integer.parseInt(identifier.substring(1,identifier.length()));
	}
	
	/**
	 * @param identifier a String to check whether it looks like variable name
	 * @return an int of the number after the char 'v' if the identifier looks
	 * like the variable name, and -1 otherwise
	 */
	private int isVarName(String identifier)
	{
		if(identifier.length()==1)
			return -1;
		if(identifier.charAt(0) != 'v' && identifier.charAt(0) != 'V')
			return -1;
		for(int i=1;i<identifier.length();i++)
		{
			if(! isInt(identifier.charAt(i)))
				return -1;
		}
		return Integer.parseInt(identifier.substring(1,identifier.length()));
	}
	
	/**
	 * @param c a char that we need to know if it is a number from '0' to '9' or not
	 * @return true if this char is a number and false if not
	 */
	private boolean isInt(char c)
	{
		switch(c)
		{
		case '0':;
		case '1':;
		case '2':;
		case '3':;
		case '4':;
		case '5':;
		case '6':;
		case '7':;
		case '8':;
		case '9':return true;
		default : return false;
		}
	}
	
	public static void main(String [] args)throws Exception
	{
		Network n = new Network();				
//		Relation r1 = new Relation("member","entity","reduce",0);
//		Relation r2 = new Relation("class","entity","reduce",0);
		Object[][] o4 = new Object[4][2];		   // creating the 2D arrays used in building nodes
		Object[][] o3 = new Object[3][2];
		Object[][] o2 = new Object[2][2];
		/*o[0][0] = r1;
		o[1][0] = r1;
		o[2][0] = r1;
		o[3][0] = r2;
		o[4][0] = r1;
		o[0][1] = new BaseNode("amr");
		o[1][1] = new BaseNode("moh");
		o[2][1] = new BaseNode("zay");
		o[0][1] = new BaseNode("amr");
		o[1][1] = new BaseNode("human");*/				    // creating a new network 
		Node x1 = n.buildVariableNode();                        // building variable nodes
		Node x2 = n.buildVariableNode();
		Node x3 = n.buildVariableNode();
		Node x4 = n.buildVariableNode();
		Node x5 = n.buildVariableNode();
		Node x6 = n.buildVariableNode();
		Node x7 = n.buildVariableNode();
		Node x8 = n.buildVariableNode();
		Node x9 = n.buildVariableNode();
		Node a = n.build("a");						                // building base node
		Relation rr1 = n.defineRelation("r1","Individual","none",0);
		Relation rr2 = n.defineRelation("r2","Act","none",0);	// defining relations
		Relation rr3 = n.defineRelation("r3","Act","none",0);
		Relation rr4 = n.defineRelation("r4","Act","none",0);
		LinkedList<Relation> l2 = new LinkedList<Relation>();
		l2.add(rr1);
		l2.add(rr2);
		LinkedList<Relation> l3 = new LinkedList<Relation>();
		l3.add(rr1);
		l3.add(rr2);
		l3.add(rr3);
		LinkedList<Relation> l4 = new LinkedList<Relation>();
		l4.add(rr1);
		l4.add(rr2);
		l4.add(rr3);
		l4.add(rr4);
		CaseFrame caseFrame2 = n.defineCaseFrame("Entity",l2);		  // defining case frames
		CaseFrame caseFrame3 = n.defineCaseFrame("Entity",l3);
		CaseFrame caseFrame4 = n.defineCaseFrame("Entity",l4);
		o3[0][0] = rr1;
		o3[1][0] = rr2;
		o3[2][0] = rr3;
		o3[0][1] = x1;
		o3[1][1] = x2;
		o3[2][1] = x3;
		Node h1 = n.build(o3,caseFrame3);                            // building molecular nodes
		o4[0][0] = rr1;
		o4[1][0] = rr2;
		o4[2][0] = rr3;
		o4[3][0] = rr3;
		o4[0][1] = x6;
		o4[1][1] = x7;
		o4[2][1] = x8;
		o4[3][1] = x9;
		Node h2 = n.build(o4,caseFrame3);
		o4[0][0] = rr1;
		o4[1][0] = rr2;
		o4[2][0] = rr3;
		o4[3][0] = rr4;
		o4[0][1] = h1;
		o4[1][1] = h2;
		o4[2][1] = x3;
		o4[3][1] = x6;
		PatternNode t = (PatternNode) n.build(o4,caseFrame4);
		o2[0][0] = rr1;
		o2[1][0] = rr2;
		o2[0][1] = x4;
		o2[1][1] = x5;
		Node g1 = n.build(o2,caseFrame2);
		o3[0][1] = g1;
		o3[1][1] = x1;
		o3[2][1] = x2;
		Node h3 = n.build(o3,caseFrame3);
		o4[0][0] = rr1;
		o4[1][0] = rr2;
		o4[2][0] = rr3;
		o4[3][0] = rr3;
		o4[0][1] = x7;
		o4[1][1] = x8;
		o4[2][1] = x9;
		o4[3][1] = x6;
		Node h4 = n.build(o4,caseFrame3);
		o2[0][1] = x5;
		o2[1][1] = a;
		Node g2 = n.build(o2,caseFrame2);
		o4[0][0] = rr1;
		o4[1][0] = rr2;
		o4[2][0] = rr3;
		o4[3][0] = rr4;
		o4[0][1] = h3;
		o4[1][1] = h4;
		o4[2][1] = g2;
		o4[3][1] = x5;
		PatternNode tdash = (PatternNode) n.build(o4,caseFrame4);
		/*LinkedList<Object[]> l = n.match(tdash);
		for(int i=0;i<l.size();i++)
		{
			Object[] o = l.get(i);
			System.out.println(((Node)o[0]).getIdentifier()+ "   ");
			for(int j=0;j<((Substitutions)o[1]).cardinality();j++)
			{
				Binding b = ((Substitutions)o[1]).getBinding(j);
				System.out.println(b.getVariable().getIdentifier()+" "+b.getNode().getIdentifier());
			}
			for(int j=0;j<((Substitutions)o[2]).cardinality();j++)
			{
				Binding b = ((Substitutions)o[2]).getBinding(j);
				System.out.println(b.getVariable().getIdentifier()+" "+b.getNode().getIdentifier());
			}
		}*/
		t.getClass();
		tdash.getClass();
	//	Object[] array = n.renameSharedVariables((MolecularNode)h1,(MolecularNode)h3);
	//	System.out.println(array[1]);
		/*n.simulateParentNodes(n.buildVariableNode(),a);
		for(Enumeration<Node> e = n.getNodes().elements();e.hasMoreElements();)
		{
			System.out.println(e.nextElement());
		}*/
		
		/*System.out.println(t);
		System.out.println(tdash);*/
	//	Object[] array = n.renameSharedVariables(t,tdash);
	//	array.getClass();
		/*System.out.println(array[0]);
		System.out.println(array[1]);
		System.out.println(array[2]);*/
		/*for(Enumeration<Node> e = n.getNodes().elements();e.hasMoreElements();)
		{
			System.out.println(e.nextElement());
		}
		System.out.println("-------------");
		n.removeNode(t);
		for(Enumeration<Node> e = n.getNodes().elements();e.hasMoreElements();)
		{
			System.out.println(e.nextElement());
		}*/
		
		Relation member = n.defineRelation("member","Entity","none",1);
		Relation clas = n.defineRelation("class","Entity","none",1);
		Relation sub = n.defineRelation("sub","Entity","none",1);
		Relation sup = n.defineRelation("super","Entity","none",1);
		Relation object = n.defineRelation("object","Entity","none",1);
		Relation sim = n.defineRelation("similarto","Entity","none",1);
		Relation temp = n.defineRelation("temp","Entity","reduce",0);
		Path b = new BUnitPath(sub);
		Path f = new FUnitPath(sup);
		Path fc = new FUnitPath(clas);
		LinkedList<Path> list = new LinkedList<Path>();
		list.add(b);
		list.add(f);
		Path c = new ComposePath(list);
		Path kc = new KStarPath(c);
		LinkedList<Path> list1 = new LinkedList<Path>();
		list1.add(fc);
		list1.add(kc);
		Path path = new ComposePath(list1);
		n.definePath(clas,path);
		Path b1 = new BUnitPath(sim);
		Path f1 = new FUnitPath(object);
		Path fc1 = new FUnitPath(member);
		LinkedList<Path> list2 = new LinkedList<Path>();
		list2.add(b1);
		list2.add(f1);
		Path c1 = new ComposePath(list2);
		Path kc1 = new KStarPath(c1);
		LinkedList<Path> list12 = new LinkedList<Path>();
		list12.add(fc1);
		list12.add(kc1);
		Path path1 = new ComposePath(list12);
		n.definePath(member,path1);
		LinkedList<Relation> l1 = new LinkedList<Relation>();
		l1.add(member);
		l1.add(clas);
		l1.add(temp);
		LinkedList<Relation> l21 = new LinkedList<Relation>();
		l21.add(sub);
		l21.add(sup);
		LinkedList<Relation> l211 = new LinkedList<Relation>();
		l211.add(object);
		l211.add(sim);
		LinkedList<Relation> l2111 = new LinkedList<Relation>();
		l2111.add(member);
		l2111.add(clas);
		CaseFrame mct = n.defineCaseFrame("Entity",l1);
		CaseFrame ss = n.defineCaseFrame("Entity",l21);
		CaseFrame os = n.defineCaseFrame("Entity",l211);
		CaseFrame mc = n.defineCaseFrame("Entity",l2111);
		Node fido = n.build("fido");
		Node dog = n.build("dog");
		Node animal = n.build("animal");
		Node creature = n.build("creature");
		Node lacy = n.build("lacy");
		Node marley = n.build("marley");
		Node tmp = n.build("temp");
		o3[0][0] = member;
		o3[1][0] = clas;
		o3[2][0] = temp;
		o3[0][1] = fido;
		o3[1][1] = dog;
		o3[2][1] = tmp;
		Node n1 = n.build(o3,mct);
		n1.getClass();
		o2[0][0] = sub;
		o2[1][0] = sup;
		o2[0][1] = dog;
		o2[1][1] = animal;
		Node n2 = n.build(o2,ss);
		n2.getClass();
		o2[0][0] = sub;
		o2[1][0] = sup;
		o2[0][1] = animal;
		o2[1][1] = creature;
		Node n3 = n.build(o2,ss);
		n3.getClass();
		o2[0][0] = object;
		o2[1][0] = sim;
		o2[0][1] = lacy;
		o2[1][1] = fido;
		Node n4 = n.build(o2,os);
		o2[0][0] = object;
		o2[1][0] = sim;
		o2[0][1] = marley;
		o2[1][1] = fido;
		Node n5 = n.build(o2,os);
		n4.getClass();
		n5.getClass();
		System.out.println(path1+ " "+path);
		NodeSet ns = n.pathBasedInfer(mc,new Context());
		System.out.println(ns);
		
		
		/*Substitutions r = new Substitutions();
		LinkedList<Substitutions> rr = new LinkedList<Substitutions>();
		rr.add(r);
		System.out.println(x1.getEntity().getClass().getSimpleName());
		if(n.hERe(t,tdash,rr,true)) 
		{
			System.out.println("> "+rr.size());
			for(int i=0;i<rr.size();i++)
			{
				Substitutions x = rr.get(i);
				for(int j=0;j<x.cardinality();j++)
				{
					System.out.print(((Node) x.getBinding(j).getVariable()).getIdentifier());
					System.out.println(" "+((Node) x.getBinding(j).getNode()).getIdentifier());
				}
				System.out.println("----");
			}
		}*/
		/*for(int i=0;i<rr.size();i++)
		{
			LinkedList<VariableNode> list1 = t.getFreeVariables();
			LinkedList<VariableNode> list2 = tdash.getFreeVariables();
			if(n.termVERe(t,rr.get(i),s) != null)
			{
				for(int j=0;j<list1.size();j++)
				{
					System.out.println(list1.get(j).getIdentifier()+" "+ n.vERe(list1.get(j),rr.get(i),s).getIdentifier());
				}
				for(int j=0;j<list2.size();j++)
				{
					System.out.println(list2.get(j).getIdentifier()+" "+ n.vERe(list2.get(j),rr.get(i),s).getIdentifier());
				}
			}
		}*/
		
		/*Path f = new FUnitPath(rr3);
		Path f4 = new FUnitPath(rr4);
		Path b = new BUnitPath(rr1);
		Path kf = new KStarPath(f);
		Path kb = new KStarPath(b);
		LinkedList<Path> l = new LinkedList<Path>();
		l.add(kb);
		l.add(kf);
	//	Path c = new ComposePath(l);
		LinkedList<Path> l21 = new LinkedList<Path>();
		l21.add(f4);
		l21.add(f);
		Path and = new AndPath(l21);
		LinkedList<Path> l1 = new LinkedList<Path>();
		l1.add(b);
		l1.add(and);
		Path or = new ComposePath(l1);
		NodeSet ns = new NodeSet();
		ns.addNode(x3);
		Object[][] array = new Object[1][2];
		array[0][0] = or;
		array[0][1] = ns;
	//	LinkedList<Object[]> result = n.find(array,new Context());
		LinkedList<Object[]> result = and.follow(t,new PathTrace(),new Context());
		for(int i=0;i<result.size();i++)
		{
			Object[] o = result.get(i);
			PathTrace pt = (PathTrace) o[1];
			System.out.println(((Node)o[0]).getIdentifier());
			System.out.println(" "+pt.getPath().toString());
			for(int j=0;j<pt.getFirst().size();j++)
			{
				System.out.println(" "+pt.getFirst().get(j));
			}
		}*/
		
		/*Hashtable<String,LinkedList<String>> h = new Hashtable<String, LinkedList<String>>();
		LinkedList<String> l = new LinkedList<String>();
		h.put("amr",l);
		h.get("amr").add("AMR");
		for(Enumeration<String> e = h.keys();e.hasMoreElements();)
			System.out.println(e.nextElement());*/
		/*NodeSet ns1 = new NodeSet();
		NodeSet ns2 = new NodeSet();
		NodeSet ns3 = new NodeSet();
		ns1.addNode(x1);
		ns2.addNode(x2);
		ns3.addNode(x3);
		LinkedList<NodeSet> lll = new LinkedList<NodeSet>();
		lll.add(ns1);
		lll.add(ns2);
		lll.add(ns3);
		for(int i=0;i<lll.size();i++)
		{
			System.out.println(lll.get(i).getNodes().getFirst().getIdentifier());
			if(lll.get(i).getNodes().getFirst().getIdentifier().equals("x2"))
			{
				lll.remove(i);
				i--;
			}
		}*/
		/*MolecularNode m = (MolecularNode) n.vERe((VariableNode) x1,r,s);
		MolecularNode mm = (MolecularNode) n.vERe((VariableNode) x2,r,s);
		MolecularNode mmm = (MolecularNode) n.vERe((VariableNode) x3,r,s);
		System.out.println(m +"  "+mm+"  "+mmm);
		System.out.println((n.vERe((VariableNode) x7,r,s)).getIdentifier());
		System.out.println((n.vERe((VariableNode) x6,r,s)).getIdentifier());
		System.out.println((n.vERe((VariableNode) x5,r,s)).getIdentifier());
		System.out.println((n.vERe((VariableNode) x4,r,s)).getIdentifier());
		System.out.println((n.vERe((VariableNode) x8,r,s)).getIdentifier());
		System.out.println(mm.getCableSet().getCables().get(0).getNodeSet().getNodes().getFirst().getIdentifier());
		System.out.println(mm.getCableSet().getCables().get(1).getNodeSet().getNodes().getFirst().getIdentifier());*/
		/*VariableNode v1 = new VariableNode("V1");
		VariableNode v2 = new VariableNode("V2");
		VariableNode v3 = new VariableNode("V3");
		VariableNode v4 = new VariableNode("V4");
		VariableNode v5 = new VariableNode("V5");
		Stack<VariableNode> s = new Stack<VariableNode>();
		s.push(v1);
		s.push(v2);
		s.push(v3);
		s.push(v4);
		s.push(v5);
		BaseNode b = new BaseNode("amr");
		Substitutions z = new Substitutions();
		z.putIn(new Binding(v1,v2));
		z.putIn(new Binding(v2,v1));
		z.putIn(new Binding(v3,v4));
		z.putIn(new Binding(v4,v5));
		z.putIn(new Binding(v5,v1));
		Stack<VariableNode> x = n.source(v3,z);
		for(int i=0;i<x.size();i++)
		{
			System.out.println(x.get(i).getIdentifier());
		}*/
	//	Node res = n.build(o,caseFrame);
		/*System.out.println(res.getIdentifier());
		System.out.println(n.getNodes().get(res.getIdentifier()).getIdentifier());
		System.out.println(((PatternNode) res1).getFreeVariables().size());*/
		/*for(Enumeration<Relation> e = n.relations.elements();e.hasMoreElements();)
			System.out.println("r:" +e.nextElement().getName());
		for(Enumeration<CaseFrame> e = n.caseFrames.elements();e.hasMoreElements();)
			System.out.println(e.nextElement().getId());
		n.undefineRelation("member");
		for(Enumeration<Relation> e = n.relations.elements();e.hasMoreElements();)
			System.out.println("r:" +e.nextElement().getName());
		for(Enumeration<CaseFrame> e = n.caseFrames.elements();e.hasMoreElements();)
			System.out.println(e.nextElement().getId());
		*/
		/*for(int i=0;i<oo.length;i++)
		{
			System.out.println(oo.length);
			Relation r = (Relation) oo[i][0];
			NodeSet ns = (NodeSet) oo[i][1];
			for(int j=0;j<ns.getNodes().size();j++)
			{
				Node node = ns.getNodes().get(j);
				System.out.println(r.getName()+ " " +node.getIdentifier());
			}
		}*/
		/*LinkedList<Relation> r = new LinkedList<Relation>();
		r.add(r1);
		r.add(r1);
		r.add(r1);
		r.addAll(r.subList(1,r.size()));
		for(int i=0;i<r.size();i++)
		{
			System.out.println(r.get(i).getName());
		}*/
	}
	
}
