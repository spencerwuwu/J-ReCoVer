
package statementResolver.soot;

import com.google.common.base.Preconditions;

import statementResolver.Option;
import statementResolver.color.Color;
import statementResolver.state.State;
import statementResolver.state.StateUnitPair;
import statementResolver.state.UnitSet;
import statementResolver.tree.Tree;
import statementResolver.tree.TreeNode;
import statementResolver.z3formatbuilder.*;
import soot.Body;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowAnalysis;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class StatementResolver {

	private LinkedHashMap<String, Boolean> InputListUsed = new LinkedHashMap<String, Boolean>();
	private LinkedHashMap<String, String> LocalVars = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> varsType = new LinkedHashMap<String, String>();
	private List<State> States = new ArrayList<State>();
	private int enterLoopLine = 0;
	private int outLoopLine = 0;
	private boolean useNextBeforeLoop = false;
	private boolean before = true;
	

	private final List<String> resolvedClassNames;
	Option op = new Option();

	public StatementResolver() {
		this(new ArrayList<String>());
	}
	
	public StatementResolver(List<String> resolvedClassNames) {
		this.resolvedClassNames = resolvedClassNames;
		// first reset everything:
		soot.G.reset();
	}

	public void run(String input, String classPath, Option option) {
		SootRunner runner = new SootRunner();
		runner.run(input, classPath);
		op = option;
		// Main analysis starts from here
		performAnalysis();
	}

	private void addDefaultInitializers(SootMethod constructor, SootClass containingClass) {
		if (constructor.isConstructor()) {
			Preconditions.checkArgument(constructor.getDeclaringClass().equals(containingClass));
			JimpleBody jbody = (JimpleBody) constructor.retrieveActiveBody();

			Set<SootField> instanceFields = new LinkedHashSet<SootField>();
			for (SootField f : containingClass.getFields()) {
				if (!f.isStatic()) {
					instanceFields.add(f);
				}
			}
			for (ValueBox vb : jbody.getDefBoxes()) {
				if (vb.getValue() instanceof InstanceFieldRef) {
					Value base = ((InstanceFieldRef) vb.getValue()).getBase();
					soot.Type baseType = base.getType();
					if (baseType instanceof RefType && ((RefType) baseType).getSootClass().equals(containingClass)) {
						// remove the final fields that are initialized anyways
						// from
						// our staticFields set.
						SootField f = ((InstanceFieldRef) vb.getValue()).getField();
						if (f.isFinal()) {
							instanceFields.remove(f);
						}
					}
				}
			}

			Unit insertPos = null;

			for (Unit u : jbody.getUnits()) {
				if (u instanceof IdentityStmt) {
					insertPos = u;
				} else {
					break; // insert after the last IdentityStmt
				}
			}

		}
	}
	

	public SootClass getAssertionClass() {
		return Scene.v().getSootClass(SootRunner.assertionClassName);
	}

	public void performAnalysis() {
		List<SootClass> classes = new LinkedList<SootClass>(Scene.v().getClasses());
		for (SootClass sc : classes) {
			if (sc == getAssertionClass()) {
				continue; // no need to process this guy.
			}
			if (sc.resolvingLevel() >= SootClass.SIGNATURES && sc.isApplicationClass()) {
				for (SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						addDefaultInitializers(sm, sc);
					}

					Body body = sm.retrieveActiveBody();
					try {
						body.validate();
					} catch (RuntimeException e) {
						System.out.println("Unable to validate method body. Possible NullPointerException?");
						throw e;	
					}
				}
			}
		}

		System.out.println("=======================================");	
		
		for (JimpleBody body : this.get_colloctor_SceneBodies()) {
			if (op.silence_flag) silenceAnalysis(body);
			else completeAnalysis(body);
		}
		
		// TODO: Not really doing this tbh
		if (op.cfg_flag) {
			// BlockGraph blockGraph = new BriefBlockGraph(body);
			//     System.out.println(blockGraph);
			// }
			CFGToDotGraph cfgToDot = new CFGToDotGraph();
			int i = 0;
			for (JimpleBody body : this.getSceneBodies()) {
				DirectedGraph g = new CompleteUnitGraph(body);
				DotGraph dotGraph = cfgToDot.drawCFG(g, body);
				dotGraph.plot(i+".dot");
				i = i+1;
			}
		}
		
	}

	public void silenceAnalysis(JimpleBody body) {
		System.out.println(body.toString());
	}

	public void completeAnalysis(JimpleBody body) {
		int current_no = 0;
		int command_line_no = 1;
		UnitGraph graph = new ExceptionalUnitGraph(body);
		Iterator gIt = graph.iterator();
		
		
		List<UnitBox> UnitBoxes = body.getUnitBoxes(true);
		
		List<UnitSet> Units = new ArrayList<UnitSet>();
		Map<Unit,Integer> UnitIndexs = new LinkedHashMap<Unit, Integer>();
		for (UnitBox ub: UnitBoxes) {
			Unit u = ub.getUnit();
			UnitIndexs.put(u, 0);				
		}
		
		System.out.println(Color.ANSI_BLUE+body.toString()+Color.ANSI_RESET);
		System.out.println("=======================================");			

		// Storing variables
		List<ValueBox> defBoxes = body.getDefBoxes();
		generateVars(defBoxes);
		System.out.println("=======================================");	

		while (gIt.hasNext()) {
			Unit u = (Unit)gIt.next();	//getClass(): soot.jimple.internal.*Stmt		
			Units.add(new UnitSet(u, command_line_no));
			if (UnitIndexs.containsKey(u)) {
				UnitIndexs.put(u, command_line_no-1);
			}
			command_line_no++;
		}
		System.out.println("=======================================");	
		
		// Starting to analysis
		System.out.println("Starting analysis");
		//detect where is the loop
		detect_loop(graph, UnitIndexs);
	
		//traverse tree to find leaves and do_analysis
		
		State initState1 = new State(LocalVars, 0, null, 0, 0);
		State initState2 = new State(LocalVars, 0, null, enterLoopLine, 0);
		List<String> initConstraint1 = new ArrayList<String>();
		List<String> initConstraint2 = new ArrayList<String>();
		
		
		Tree Head = new Tree(new TreeNode(null, null, 0, 0,false), null);
		Tree beforeLoopPart = new Tree(new TreeNode(initConstraint1, initState1, 0, 0,false), Head);
		Tree enterLoopPart = new Tree(new TreeNode(initConstraint2, initState2, 0, enterLoopLine,false), Head);
		
		Head.children.add(beforeLoopPart);
		Head.children.add(enterLoopPart);
		enterLoopPart.root.get_constraint().add("beforeLoop != 0");
		beforeLoopPart.root.get_constraint().add("beforeLoop == 0");
		
		//symbolic execution when out of loop body
		//use a flag('beforeLoop') to put the code before loop into loop(make it be executed only once)
		List<Tree> leavesOut = new ArrayList<Tree>();
		List<Tree> newLeavesOut = new ArrayList<Tree>();
		leavesOut.add(beforeLoopPart);
		List<Tree> toReturn = new ArrayList<Tree>();
		
		while(!leavesOut.isEmpty() ) {
			for(Tree tree : leavesOut) {
				do_analysis(Units, UnitIndexs, tree, toReturn);
				for(Tree newtree: tree.children) {
					newLeavesOut.add(newtree);
				}
			}
			
			leavesOut.clear();
			
			for(Tree element:newLeavesOut) {
				if (!element.root.get_return_flag() && element.root.get_next_line() < enterLoopLine) {
				    leavesOut.add(element);
				}
				else {toReturn.add(element);}
				
			}
			newLeavesOut.clear();
		}
		
 		before = false; 
		
		//symbolic execution when enter loop body
		List<Tree> leaves = new ArrayList<Tree>();
		List<Tree> newLeaves = new ArrayList<Tree>();
		leaves.add(enterLoopPart);
		List<Tree> returnLeaf = new ArrayList<Tree>();
		
		while(!leaves.isEmpty() ) {
			for(Tree tree : leaves) {
				do_analysis(Units, UnitIndexs, tree, returnLeaf);
				for(Tree newtree: tree.children) {
					newLeaves.add(newtree);
				}
			}
			
			leaves.clear();
			
			for(Tree element:newLeaves) {
				if (!element.root.get_return_flag() && element.root.get_next_line() < outLoopLine) {
				    leaves.add(element);
				}
				else{returnLeaf.add(element);}
			}
			newLeaves.clear();
		}
		
		
		//print the result before loop
		for (int i = 0; i < toReturn.size();i++) {
			System.out.println("======Before Loop"+String.valueOf(i+1)+"======");
			toReturn.get(i).root.get_local_vars().put("beforeLoop", "1");
			toReturn.get(i).print();
			System.out.println("\n");
			
		}
		
		//print the result of loop body
		for (int i = 0; i < returnLeaf.size();i++) {
			System.out.println("======After Loop"+String.valueOf(i+1)+"======");
			returnLeaf.get(i).root.get_local_vars().put("beforeLoop", "1");
			detect_output(graph, returnLeaf.get(i) );
			returnLeaf.get(i).print();
			System.out.println("\n");
		}
        
		List<Tree> toWriteZ3 = new ArrayList<Tree>();
		toWriteZ3.addAll(toReturn);
		
		//delete the condition that doesn't enter the loop
		returnLeaf.remove(0);
		toWriteZ3.addAll(returnLeaf);
		z3FormatBuilder z3write = new z3FormatBuilder(varsType, toWriteZ3, "z3Format.txt", useNextBeforeLoop);
		z3write.writeZ3Format();
		
	}
	
	protected void do_analysis(List<UnitSet> Units, Map<Unit,Integer> UnitIndexs, Tree leaf, List<Tree> returnLeaf) {
		
		//add a new state(new node to tree)
        //return a new treenode to adding to the tree
		//update the leaves
		if (!leaf.root.get_return_flag() && leaf.root.get_next_line() < Units.size()) {
			UnitSet us = Units.get( leaf.root.get_next_line() );
			
			//System.out.println(us.get_unit().getClass());
			
			int deter_unit_state = deter_unit(us.get_unit());
			
			if (deter_unit_state == 1) {
				Unit newUnit=us.get_unit();
				if (newUnit instanceof AssignStmt) {
					State newState = new State(leaf.root.get_local_vars(), leaf.root.get_execution_order(), 
							         newUnit.toString(), us.get_line(), leaf.root.get_state().get_inputUsedIndex());
					TreeNode newLeafNode = performAssignStmt(newState, newUnit, leaf.root.get_local_vars());
					newLeafNode.set_constraint(leaf.root.get_constraint());
					newLeafNode.set_execution_order( leaf.root.get_execution_order()+1 );
					newLeafNode.set_next_line( leaf.root.get_next_line()+1 );
					newLeafNode.set_return_flag( leaf.root.get_return_flag() );
					Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
					
					System.out.println( Color.ANSI_BLUE+"line '" +us.get_unit().toString()+"'"+ Color.ANSI_RESET);
					
					System.out.println("------------------------------------");
   
				}
				else if (newUnit instanceof IdentityStmt) {
					
					State newState = new State(leaf.root.get_local_vars(), leaf.root.get_execution_order(), 
							         newUnit.toString(), us.get_line(), leaf.root.get_state().get_inputUsedIndex());
					TreeNode newLeafNode = performIdentityStmt(newState, newUnit);
					newLeafNode.set_constraint(leaf.root.get_constraint());
					newLeafNode.set_execution_order( leaf.root.get_execution_order()+1 );
					newLeafNode.set_next_line( leaf.root.get_next_line()+1 );
					newLeafNode.set_return_flag( leaf.root.get_return_flag() );
					Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
					
				    
					System.out.println( Color.ANSI_BLUE+"line '" +us.get_unit().toString()+"'"+ Color.ANSI_RESET);
					
					System.out.println("------------------------------------");
				}
			    
			    
			}
			
			else if (deter_unit_state == 2) {
				Unit newUnit=us.get_unit();
				if (newUnit instanceof IfStmt) {
					State newState1 = new State(leaf.root.get_local_vars(), leaf.root.get_execution_order(),
							          newUnit.toString(), us.get_line(), leaf.root.get_state().get_inputUsedIndex());
					
					State newState2 = new State(leaf.root.get_local_vars(), leaf.root.get_execution_order(), 
							          newUnit.toString(), us.get_line(), leaf.root.get_state().get_inputUsedIndex());

					List<TreeNode> newLeafNodes = performIfStmt(leaf, leaf.root.get_constraint(), newState1, newState2, newUnit, UnitIndexs);
					//if branch
					newLeafNodes.get(0).set_execution_order( leaf.root.get_execution_order()+1 );
					//nextLine has been set to 'goto_target' in performIfStmt
					newLeafNodes.get(0).set_return_flag( leaf.root.get_return_flag() );
					newLeafNodes.get(0).set_branch_info("IF branch from"+leaf.root.get_state().get_command_line_no() );
					
					//else branch
					newLeafNodes.get(1).set_execution_order( leaf.root.get_execution_order()+1 );
					newLeafNodes.get(1).set_next_line( leaf.root.get_next_line()+1 );
					newLeafNodes.get(1).set_return_flag( leaf.root.get_return_flag() );
					newLeafNodes.get(1).set_branch_info("ELSE branch from"+leaf.root.get_state().get_command_line_no() );

					
					Tree newTree1 = new Tree(newLeafNodes.get(0), leaf);
					Tree newTree2 = new Tree(newLeafNodes.get(1), leaf);
				    leaf.children.add(newTree1);
				    leaf.children.add(newTree2);
					
					System.out.println( Color.ANSI_BLUE+"line '" +us.get_unit().toString()+"'"+ Color.ANSI_RESET);
					System.out.println("Split the tree here.");
					
					System.out.println("------------------------------------");
   
				}
				else if (newUnit instanceof GotoStmt) {
					State newState = new State(leaf.root.get_local_vars(), leaf.root.get_execution_order(), 
							         newUnit.toString(), us.get_line(),leaf.root.get_state().get_inputUsedIndex());
					TreeNode newLeafNode = performGotoStmt(newState, newUnit, UnitIndexs, Units);
					newLeafNode.set_constraint(leaf.root.get_constraint());
					newLeafNode.set_execution_order( leaf.root.get_execution_order()+1 );
					//'nextLine' has been set in performGotoStmt
					newLeafNode.set_return_flag( leaf.root.get_return_flag() );
					Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
					
				    
					System.out.println( Color.ANSI_BLUE+"line '" +us.get_unit().toString()+"'"+ Color.ANSI_RESET);
					//newLeafNode.get_state().printForm();
					System.out.println("------------------------------------");
				}
				
				
			}
			else if (deter_unit_state == 3) {
				System.out.println(Color.ANSI_BLUE+"++++++++++++++++ Return +++++++++++++++++"+Color.ANSI_RESET);
				//adding this treenode to returnlist, waiting to print the result
				returnLeaf.add(leaf);
				
				boolean newReturnFlag = true;
			    TreeNode newLeafNode = new TreeNode(leaf.root.get_constraint(), leaf.root.get_state(), leaf.root.get_execution_order(), leaf.root.get_next_line(), newReturnFlag);
			    Tree newTree = new Tree(newLeafNode, leaf);
			    leaf.children.add(newTree);
				
			}
			
			//deal with invoke(determine output)
			
			else if (deter_unit_state == 4) {
				System.out.println( Color.ANSI_BLUE+"line '" +us.get_unit().toString()+"'"+ Color.ANSI_RESET);
				
				if(us.get_unit().toString().contains("virtualinvoke") ) {
				    int nextLine =  leaf.root.get_next_line()+1 ;  //iterator++;
				    TreeNode newLeafNode = new TreeNode(leaf.root.get_constraint(), leaf.root.get_state(), 
				    		                            leaf.root.get_execution_order(), nextLine, leaf.root.get_return_flag());
				    
				    if(us.get_unit().toString().contains("OutputCollector") || us.get_unit().toString().contains("Context")) {
				        String value = (us.get_unit().toString().split(">")[1]).split(",")[1];
				        value = value.replace(")", "");
				        
                        for(String replace:leaf.root.get_local_vars().keySet()) {
				        	
				        	if(value.contains(replace) ) {
				        		
				        		System.out.println("replace "+replace+":"+leaf.root.get_local_vars().get(replace));
				        	    value = value.replace(replace, leaf.root.get_local_vars().get(replace));
				        	}
				        }
				        newLeafNode.get_state().update("output", value);
				    }
				    
				    Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
				}
				else if(us.get_unit().toString().contains("specialinvoke")) {
					int nextLine =  leaf.root.get_next_line()+1 ;  //iterator++;
				    TreeNode newLeafNode = new TreeNode(leaf.root.get_constraint(), leaf.root.get_state(), 
				    		                            leaf.root.get_execution_order(), nextLine, leaf.root.get_return_flag());
				    if(us.get_unit().toString().contains("Writable") && us.get_unit().toString().contains("init")) {
				    	String key = (us.get_unit().toString().split("\\s+")[1]).split("\\.")[0];
				        String value = us.get_unit().toString().split(">")[2];
				        value = value.replace(")", "");
				        value = value.replace("(", "");
				        
				        for(String replace:leaf.root.get_local_vars().keySet()) {
				        	
				        	if(value.contains(replace) ) {
				        		
				        		System.out.println("replace "+replace+":"+leaf.root.get_local_vars().get(replace));
				        	    value = value.replace(replace, leaf.root.get_local_vars().get(replace));
				        	}
				        }
				        newLeafNode.get_state().update(key, value);
				    
				        
				    }				        
				    Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
				}
			}
			
			else {
				System.out.println( Color.ANSI_BLUE+"line '" +us.get_unit().toString()+"'"+ Color.ANSI_RESET);
				int nextLine =  leaf.root.get_next_line()+1 ;  //iterator++;
				TreeNode newLeafNode = new TreeNode(leaf.root.get_constraint(), leaf.root.get_state(), leaf.root.get_execution_order(), nextLine, leaf.root.get_return_flag());
			    Tree newTree = new Tree(newLeafNode, leaf);
			    leaf.children.add(newTree);

			}
		}
	
    }
	
	protected void detect_loop(UnitGraph G, Map<Unit, Integer> UnitIndexs) {
		//only detect one output now
		Iterator iter = G.iterator();
		int now = 0;
		while(iter.hasNext()) {
			Unit u =(Unit)iter.next();
			if(u instanceof GotoStmt) {
				GotoStmt gtStmt = (GotoStmt) u;
				Unit gotoTarget = gtStmt.getTarget();
				if(UnitIndexs.get(gotoTarget) < now) {
					enterLoopLine = UnitIndexs.get(gotoTarget);
					outLoopLine = now;
					System.out.println("loop from line: "+String.valueOf(enterLoopLine)+" to "+String.valueOf(outLoopLine));
					
				}
			}
			now++;
		}
		
	}
	
	//not really need to use it
	protected void detect_output(UnitGraph G, Tree tree) {
		Iterator iter = G.iterator();
		
		while(iter.hasNext()) {
			Unit u =(Unit)iter.next();
			if(u instanceof InvokeStmt) {
				
				if(u.toString().contains("virtualinvoke")) {
					if(u.toString().contains("OutputCollector") || u.toString().contains("Context")) {
						String value = (u.toString().split(">")[1]).split(",")[1];
				        value = value.replace(")", "");
                        for(String replace:tree.root.get_local_vars().keySet()) {
				        	if(value.contains(replace) ) {
				        	    value = value.replace(replace, tree.root.get_local_vars().get(replace));
				        	}
				        }
				        tree.root.get_local_vars().put("output", value);
				        
					}
				}
				else if(u.toString().contains("specialinvoke")) {
					
				    if(u.toString().contains("Writable") && u.toString().contains("init")) {
				    	String key = (u.toString().split("\\s+")[1]).split("\\.")[0];
				    	String type = u.toString().split(">")[1];
				        String value = u.toString().split(">")[2];
				        value = value.replace(")", "");
				        value = value.replace("(", "");
				        
				        
				        for(String replace:tree.root.get_local_vars().keySet()) {
				        	if(value.contains(replace) ) {
				        	    value = value.replace(replace, tree.root.get_local_vars().get(replace));
				        	}
				        }
				        tree.root.get_local_vars().put(key, value);    
				    }				        
				    
				}
			}
			
		}
	}
	
	
	// TODO: Upgrade to <String, VariableSet> instead of <String, String>
	 protected void generateVars(List<ValueBox> defBoxes){
		for (ValueBox d: defBoxes) {
			Value value = d.getValue();
			String str = value.getType().toString()+" "+d.getValue().toString();
			varsType.put(d.getValue().toString(), value.getType().toString());
			LocalVars.put(value.toString(), value.toString()+"_v");
			System.out.println("Variable " + str);
		}
		
		// Insert some input (only one input now)
		for (int i = 0; i < 1; i++) {
			String name = "Input" + i;
			String value = "input" + i;
			InputListUsed.put(name, false);
			varsType.put(name, "no need");
			LocalVars.put(name, value);
			System.out.println("Variable " + name);
		}
		LocalVars.put("output", "");
		varsType.put("output", "");
		LocalVars.put("beforeLoop", "bL_v");
		varsType.put("beforeLoop", "before loop flag");
		System.out.println("Variable output");
		System.out.println("Variable boolean beforeLoop");
	 }
	
	// TODO: Far from matching all cases.
	// 0 : null
	// 1 : no unit return, eg: AssignStmt
	// 2 : going to a unit target, eg: GotoStmt
	// 3 : return
	// 4 : testing
	protected int deter_unit(Unit u) {
		if (u instanceof JLookupSwitchStmt) {
			return 0;
		}
		else if (u instanceof AssignStmt) {
			return 1;
		}
		else if (u instanceof ArrayRef) {
			return 0;
		}
		else if (u instanceof BreakpointStmt) {
			return 0;
		}
		else if (u instanceof BinopExpr) {
			return 0;
		}
		else if (u instanceof CaughtExceptionRef) {
			return 0;
		}
		else if (u instanceof GotoStmt) {
			return 2;
		}
		else if (u instanceof NoSuchLocalException) {
			return 0;
		}
		else if (u instanceof NullConstant) {
			return 0;
		}
		else if (u instanceof IfStmt) {
			return 2;
		}
		else if (u instanceof IdentityStmt) {
			return 1;
		}
		else if (u instanceof InstanceOfExpr) {
			return 0;
		}
		else if (u instanceof JExitMonitorStmt) {
			return 0;
		}
		else if (u instanceof JInvokeStmt) {
			return 4;
		}
		else if (u instanceof ReturnStmt) {
			return 3;
		}
		else if (u instanceof TableSwitchStmt) {
			return 0;
		}
		else if (u instanceof ThrowStmt) {
			return 0;
		}
		else if (u instanceof ReturnVoidStmt) {
			return 3;
		}
		return 4;
	}
	
	//function handle_unit is not used now.
	/*
	protected StateUnitPair handle_unit(Unit u, int num, int command_no) {
		System.out.println("++ no: " + num + ", line: " + command_no);
		State st = new State(LocalVars, num, u.toString(), command_no);

		if (u instanceof JLookupSwitchStmt) {
		}
		else if (u instanceof AssignStmt) {			
			//return performAssignStmt(st, u); 
		}
		else if (u instanceof ArrayRef) {			
		}
		else if (u instanceof BreakpointStmt) {			
		}
		else if (u instanceof BinopExpr) {			
		}
		else if (u instanceof CaughtExceptionRef) {			
		}
		else if (u instanceof GotoStmt) {
			//return performGotoStmt(st, u);
		}
		else if (u instanceof NoSuchLocalException) {			
		}
		else if (u instanceof NullConstant) {			
		}
		else if (u instanceof IfStmt) {
			//return performIfStmt(st, u);
		}
		else if (u instanceof IdentityStmt) {			
			//return performIdentityStmt(st, u); 	
		}
		else if (u instanceof InstanceOfExpr) {		
		}
		else if (u instanceof JExitMonitorStmt) {			
		}
		else if (u instanceof JInvokeStmt) {			
		}
		else if (u instanceof ReturnStmt) {			
		}
		else if (u instanceof TableSwitchStmt) {			
		}
		else if (u instanceof ThrowStmt) {			
		}
		else if (u instanceof ReturnVoidStmt) {			
		}
		return null;
	}*/
	
	protected TreeNode performAssignStmt(State st, Unit u, Map<String,String> lastEnv) {
		//adding flag to decide if it is in loop or not
		DefinitionStmt ds = (DefinitionStmt) u;
		Value var = ds.getLeftOp();
		
		// TODO: Should be able to handle more common cases
		Value assignment = ds.getRightOp();
		
		
		
		String ass_s = assignment.toString();
		// Normal assignment
		if (!ass_s.contains("Iterator")) {
			// removing quotes, eg: (org.apache.hadoop.io.IntWritable) $r6 -> $r6
			ass_s = ass_s.replaceAll("\\(.*?\\)\\s+", "");

			// handle int get(), eg: virtualinvoke $r7.<org.apache.hadoop.io.IntWritable: int get()>() -> $r7
			if (ass_s.contains("get")) {
				ass_s = ass_s.split("\\s+")[1].split("\\.")[0];
			}

			// change to prefix
			String[] temp = ass_s.split(" ");
			if(temp.length == 3) {
				ass_s = "("+temp[1]+" "+temp[0]+" "+temp[2]+")";
			}
			
			// replace rhs with LocalVars value
			for (String re_var: lastEnv.keySet()) {
				if (ass_s.contains(re_var)) {
					System.out.println("replace "+re_var+": " + lastEnv.get(re_var));
					ass_s = ass_s.replace(re_var, lastEnv.get(re_var));
				
				}
			}
				
			System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
			
			st.update( var.toString() , ass_s );
				
			
		}
		else { 
			// Handling iterator relative assignments
			if (ass_s.contains("hasNext()")) {
				//no need to detect wheater it enter loop or not
				/*
				boolean flag = false;
				for(String in_var : InputListUsed.keySet()) {
					if (InputListUsed.get(in_var) == false) {
						flag = true;
						break;
					}
				}
				if (flag) {
					ass_s = "1";
				}
				else {
					ass_s = "0";
				}
				*/
				System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
	
				st.update(var.toString(), "hasNext");
				
			}
			else if (ass_s.contains("next()")) {
				/*
				for(String in_var : InputListUsed.keySet()) {
					if (!InputListUsed.get(in_var)) {
						InputListUsed.put(in_var, true);
						ass_s = in_var;
						
						inputUsedIndex++;
						InputListUsed.put("input"+String.valueOf(inputUsedIndex), false);
						break;
					}
				*/
				if(before) {useNextBeforeLoop = true;}
				
				//use a new input
				varsType.put("input"+st.get_inputUsedIndex(), "input type");
				
				ass_s="input"+st.get_inputUsedIndex();
				st.add_inputUsedIndex();
				st.update(var.toString(), ass_s);
				System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);    
		        }	
				
			
		}
		TreeNode newNode = new TreeNode(null, st, 0, 0, false);
		return newNode;
	}

	protected TreeNode performGotoStmt(State st, Unit u, Map<Unit,Integer> UnitIndexs, List<UnitSet> Units) {
		GotoStmt gt_st = (GotoStmt) u;
		Unit goto_target = gt_st.getTarget();
		TreeNode node;
		
		if( UnitIndexs.get(goto_target) > st.get_command_line_no()  ) {
		    System.out.println(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + goto_target);
		    node = new TreeNode(null, st, 0, UnitIndexs.get(goto_target), false);
		}
		else {
			node = new TreeNode(null, st, 0, st.get_command_line_no(), false);
			System.out.println(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + Units.get(st.get_command_line_no()).get_unit() );
		}
		return node;
	}

	protected List<TreeNode> performIfStmt(Tree parrent, List<String> conditionBefore, State ifBranchState, State elseBranchState, Unit u, Map<Unit,Integer> UnitIndexs) {
		IfStmt if_st = (IfStmt) u;
		Unit goto_target = if_st.getTargetBox().getUnit();
		Value condition = if_st.getCondition();
		ConditionExpr conditionStmt = (ConditionExpr)condition;
		
		
		System.out.println(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + goto_target + Color.ANSI_GREEN + " when " + Color.ANSI_RESET + condition);
		
		//split the tree
		
		List<String> ifCondition = new ArrayList<String>();
		List<String> elseCondition = new ArrayList<String>();
		for (String s: conditionBefore) {
			ifCondition.add(s);
			elseCondition.add(s);condition.toString();
		}
		
		String newIfCondition = condition.toString();
		String newElseCondition = "! "+condition.toString();
		
		// replace with LocalVars value
		Map<String, String> lastEnv = parrent.root.get_local_vars();
		for (String re_var: lastEnv.keySet()) {
			if (newIfCondition.contains(re_var) && lastEnv.get(re_var)!="hasNext" ) {
				System.out.println("replace "+re_var+": " + lastEnv.get(re_var));
				newIfCondition = newIfCondition.replace(re_var, lastEnv.get(re_var));
			
			}
			if (newElseCondition.contains(re_var) && lastEnv.get(re_var)!="hasNext" ) {
				System.out.println("replace "+re_var+": " + lastEnv.get(re_var));
				newElseCondition = newElseCondition.replace(re_var, lastEnv.get(re_var));
			
			}
		}

		ifCondition.add( newIfCondition );
		elseCondition.add( newElseCondition );
		
				
		TreeNode ifBranch = new TreeNode(ifCondition, ifBranchState, 0, UnitIndexs.get(goto_target), false);
		TreeNode elseBranch = new TreeNode(elseCondition, elseBranchState, 0, 0, false);
		
		if(parrent.root.get_local_vars().get(conditionStmt.getOp1().toString()) == "hasNext" ) {
			ifBranch.get_local_vars().put(conditionStmt.getOp1().toString(), "0");
			elseBranch.get_local_vars().put(conditionStmt.getOp1().toString(), "1");
		}
		
		
		List<TreeNode> returnList = new ArrayList<TreeNode>();
		returnList.add(ifBranch);
		returnList.add(elseBranch);
		return returnList;
	}

	protected TreeNode performIdentityStmt(State st, Unit u) {
		DefinitionStmt ds = (DefinitionStmt) u;
		Value var = ds.getLeftOp();
		Value assignment = ds.getRightOp();
		// Preserve only org.apache.hadoop.io.'IntWritable and marked it as parameter'
		String assignment_tail = "@parameter "+assignment.toString().split("\\.(?=[^\\.]+$)")[1]; 
		
		System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + assignment_tail);
		st.update(var.toString(), assignment_tail);
		TreeNode newNode = new TreeNode(null, st, 0, 0, false);
		return newNode;
	}
	
	/*
	protected Boolean checkCondition(Value condition) {
		// TODO: Currently only implements '=='
		String[] parts = condition.toString().split(" ");
		String left = parts[0];
		if (LocalVars.containsKey(parts[0])) left = LocalVars.get(parts[0]);
		String right = parts[2];
		if (LocalVars.containsKey(parts[2])) left = LocalVars.get(parts[2]);
		System.out.println("Compare {"+left+"}"+parts[1]+"{"+right+"}");
		if (parts[1].equals("==")) {
			if (left.equals(right)) return true;
			else return false;
		}
		else return true;
	}*/
    
	protected Set<JimpleBody> getSceneBodies() {
		Set<JimpleBody> bodies = new LinkedHashSet<JimpleBody>();
		for (SootClass sc : new LinkedList<SootClass>(Scene.v().getClasses())) {

			if (sc.resolvingLevel() >= SootClass.BODIES) {

				for (SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						bodies.add((JimpleBody) sm.retrieveActiveBody());
					}
				}
			}
		}
		return bodies;
	}
	

	protected Set<JimpleBody> get_colloctor_SceneBodies() {
		Set<JimpleBody> bodies = new LinkedHashSet<JimpleBody>();
		
		for (SootClass sc : new LinkedList<SootClass>(Scene.v().getClasses())) {
			
			// Specify target class name here
			if (sc.resolvingLevel() >= SootClass.BODIES && sc.toString().contains("collector0_90_1_7")) {

				for (SootMethod sm : sc.getMethods()) {			
					
					
					// Specify target function name here
					
					if (sm.isConcrete() && (sm.toString().contains("reduce("))) {
						System.out.println("method:"+sm.toString());
						
						JimpleBody body = (JimpleBody) sm.retrieveActiveBody();
						System.out.println("=======================================");			
						//System.out.println(sm.getName());
						bodies.add(body);
						break;
					}
					
				}
			}
		}
		return bodies;
	}
	
}