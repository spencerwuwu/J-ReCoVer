package statementResolver.soot;

import com.google.common.base.Preconditions;

import statementResolver.Option;
import statementResolver.color.Color;
import statementResolver.executionTree.ExecutionTree;
import statementResolver.executionTree.ExecutionTreeNode;
import statementResolver.state.State;
import statementResolver.state.StateUnitPair;
import statementResolver.state.UnitSet;
import statementResolver.state.VariableSet;
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

	private LinkedHashMap<String, String> mLocalVars = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> mVarsType = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, VariableSet> mVarSets = new LinkedHashMap<String, VariableSet>();
	private List<State> mStates = new ArrayList<State>();
	private int mEnterLoopLine = 0;
	private int mOutLoopLine = 0;
	private boolean mUseNextBeforeLoop = false;
	private boolean mBefore = true;
	

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

	public void run(String input, String classPath, Option option, String reducerClassname) {
		SootRunner runner = new SootRunner();
		runner.run(input, classPath);
		op = option;
		// Main analysis starts from here
		performAnalysis(reducerClassname);
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

	public void performAnalysis(String reducerClassname) {
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
		Set<JimpleBody> bodies = this.getCollectorSceneBodies(reducerClassname);
		if (bodies.size() == 0) {
			System.out.println("No reducer in classname");	
			System.out.println("=======================================");	
			return;
		}
		
		for (JimpleBody body : bodies) {
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
		
		List<UnitBox> unitBoxes = body.getUnitBoxes(true);
		
		List<UnitSet> units = new ArrayList<UnitSet>();
		Map<Unit,Integer> unitIndexes = new LinkedHashMap<Unit, Integer>();
		for (UnitBox ub: unitBoxes) {
			Unit u = ub.getUnit();
			unitIndexes.put(u, 0);				
		}
		
		System.out.println(Color.ANSI_BLUE+body.toString()+Color.ANSI_RESET);
		System.out.println("=======================================");			

		// Storing variables and detect main loop location
		List<ValueBox> defBoxes = body.getDefBoxes();
		generateVars(defBoxes);
		System.out.println("=======================================");	

		while (gIt.hasNext()) {
			Unit u = (Unit)gIt.next();	//getClass(): soot.jimple.internal.*Stmt		
			units.add(new UnitSet(u, command_line_no));
			if (unitIndexes.containsKey(u)) {
				unitIndexes.put(u, command_line_no-1);
			}
			command_line_no++;
		}
		System.out.println("=======================================");	
		// Detect where the loop starts
		detectLoop(graph, unitIndexes);
		
		// Starting to analysis
		System.out.println("Starting analysis");
	
		//traverse tree to find leaves and doAnalysis
		
		State initStateBefore = new State(mLocalVars, 0, null, 0, 0);
		State initStateInter = new State(mLocalVars, 0, null, mEnterLoopLine, 0);
		List<String> initConstraintBefore = new ArrayList<String>();
		List<String> initConstraintInter = new ArrayList<String>();
		
		Tree head = new Tree(new TreeNode(null, null, 0, 0, false), null);
		Tree beforeLoop = new Tree(new TreeNode(initConstraintBefore, initStateBefore, 0, 0, false), head);
		Tree interLoop = new Tree(new TreeNode(initConstraintInter, initStateInter, 0, mEnterLoopLine, false), head);
		
		
		head.children.add(beforeLoop);
		head.children.add(interLoop);
		interLoop.mRoot.getConstraint().add("beforeLoop != 0");
		beforeLoop.mRoot.getConstraint().add("beforeLoop == 0");
		
		// Symbolic execution before entering loop body
		// Use a flag('beforeLoop') to put the code before loop into loop(make it be executed only once)
		List<Tree> leavesOut = new ArrayList<Tree>();
		List<Tree> newLeavesOut = new ArrayList<Tree>();
		leavesOut.add(beforeLoop);
		List<Tree> toReturn = new ArrayList<Tree>();
		
		/*
		while(!leavesOut.isEmpty() ) {
			for(Tree tree : leavesOut) {
				doAnalysis(units, unitIndexes, tree, toReturn);
				for(Tree newtree: tree.children) {
					newLeavesOut.add(newtree);
				}
			}
			
			leavesOut.clear();
			
			for(Tree element:newLeavesOut) {
				if (!element.mRoot.getReturnFlag() && element.mRoot.getNextLine() < mEnterLoopLine) {
				    leavesOut.add(element);
				}
				else {toReturn.add(element);}
				
			}
			newLeavesOut.clear();
		}
		*/
		
 		mBefore = false; 
		
		//symbolic execution when enter loop body
		List<Tree> leaves = new ArrayList<Tree>();
		List<Tree> newLeaves = new ArrayList<Tree>();
		leaves.add(interLoop);
		List<Tree> returnLeaf = new ArrayList<Tree>();
		
		/*
		while(!leaves.isEmpty() ) {
			for(Tree tree : leaves) {
				doAnalysis(units, unitIndexes, tree, returnLeaf);
				for(Tree newtree: tree.children) {
					newLeaves.add(newtree);
				}
			}
			
			leaves.clear();
			
			for(Tree element:newLeaves) {
				if (!element.mRoot.getReturnFlag() && element.mRoot.getNextLine() < mOutLoopLine) {
				    leaves.add(element);
				}
				else{returnLeaf.add(element);}
			}
			newLeaves.clear();
		}
		
		
		//print the result before loop
		for (int i = 0; i < toReturn.size();i++) {
			System.out.println("======Before Loop"+String.valueOf(i+1)+"======");
			toReturn.get(i).mRoot.getLocalVars().put("beforeLoop", "1");
			toReturn.get(i).print();
			System.out.println("\n");
			
		}
		
		//print the result of loop body
		for (int i = 0; i < returnLeaf.size();i++) {
			System.out.println("======After Loop"+String.valueOf(i+1)+"======");
			returnLeaf.get(i).mRoot.getLocalVars().put("beforeLoop", "1");
			detectOutput(graph, returnLeaf.get(i) );
			returnLeaf.get(i).print();
			System.out.println("\n");
		}
		*/
		ExecutionTree beforeLoopTree = new ExecutionTree(
				new ExecutionTreeNode(initConstraintBefore, initStateBefore, 0, 0, false), units, 
				unitIndexes, mEnterLoopLine, mOutLoopLine, mVarsType, true);
		beforeLoopTree.addRootConstraint("beforeLoop == 0");
		beforeLoopTree.executeTree();
		for (Map.Entry<String, String> entry : beforeLoopTree.getVarType().entrySet()) {
			mVarsType.put(entry.getKey(), entry.getValue());
		}

		ExecutionTree interLoopTree = new ExecutionTree(
				new ExecutionTreeNode(initConstraintInter, initStateInter, 0, mEnterLoopLine, false), units, 
				unitIndexes, mEnterLoopLine, mOutLoopLine, mVarsType, false);
		interLoopTree.addRootConstraint("beforeLoop != 0");
		interLoopTree.executeTree();
		
		beforeLoopTree.print();
		interLoopTree.print();
		
		for (Map.Entry<String, String> entry : interLoopTree.getVarType().entrySet()) {
			mVarsType.put(entry.getKey(), entry.getValue());
		}
		
        
		List<ExecutionTreeNode> toWriteZ3 = new ArrayList<ExecutionTreeNode>();
		toWriteZ3.addAll(beforeLoopTree.getEndNodes());
		interLoopTree.getEndNodes().remove(0);
		toWriteZ3.addAll(interLoopTree.getEndNodes());
		z3FormatBuilder z3write = new z3FormatBuilder();
		z3write.init(mVarsType, beforeLoopTree.getEndNodes(), interLoopTree.getEndNodes(), "z3Format.txt", mUseNextBeforeLoop);
		z3write.writeZ3FormatNew();
		/*
		List<Tree> toWriteZ3 = new ArrayList<Tree>();
		toWriteZ3.addAll(toReturn);
		
		//delete the condition that doesn't enter the loop
		returnLeaf.remove(0);
		toWriteZ3.addAll(returnLeaf);
		z3FormatBuilder z3write = new z3FormatBuilder(mVarsType, toWriteZ3, "z3Format.txt", mUseNextBeforeLoop);
		z3write.writeZ3Format();
		*/
		
	}
	
	protected void doAnalysis(List<UnitSet> Units, Map<Unit,Integer> UnitIndexes, Tree leaf, List<Tree> returnLeaf) {
		
		//add a new state(new node to tree)
        //return a new treenode to adding to the tree
		//update the leaves
		if (!leaf.mRoot.getReturnFlag() && leaf.mRoot.getNextLine() < Units.size()) {
			UnitSet us = Units.get( leaf.mRoot.getNextLine() );
			
			//System.out.println(us.getUnit().getClass());
			
			int deter_unit_state = deter_unit(us.getUnit());
			
			if (deter_unit_state == 1) {
				Unit newUnit=us.getUnit();
				if (newUnit instanceof AssignStmt) {
					State newState = new State(leaf.mRoot.getLocalVars(), leaf.mRoot.getExecutionOrder(), 
							         newUnit.toString(), us.getLine(), leaf.mRoot.getState().getInputUsedIndex());
					TreeNode newLeafNode = performAssignStmt(newState, newUnit, leaf.mRoot.getLocalVars());
					newLeafNode.setConstraint(leaf.mRoot.getConstraint());
					newLeafNode.setExecutionOrder( leaf.mRoot.getExecutionOrder()+1 );
					newLeafNode.setNextLine( leaf.mRoot.getNextLine()+1 );
					newLeafNode.setReturnFlag( leaf.mRoot.getReturnFlag() );
					Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
					
					System.out.println( Color.ANSI_BLUE+"line '" +us.getUnit().toString()+"'"+ Color.ANSI_RESET);
					
					System.out.println("------------------------------------");
   
				}
				else if (newUnit instanceof IdentityStmt) {
					
					State newState = new State(leaf.mRoot.getLocalVars(), leaf.mRoot.getExecutionOrder(), 
							         newUnit.toString(), us.getLine(), leaf.mRoot.getState().getInputUsedIndex());
					TreeNode newLeafNode = performIdentityStmt(newState, newUnit);
					newLeafNode.setConstraint(leaf.mRoot.getConstraint());
					newLeafNode.setExecutionOrder( leaf.mRoot.getExecutionOrder()+1 );
					newLeafNode.setNextLine( leaf.mRoot.getNextLine()+1 );
					newLeafNode.setReturnFlag( leaf.mRoot.getReturnFlag() );
					Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
					
				    
					System.out.println( Color.ANSI_BLUE+"line '" +us.getUnit().toString()+"'"+ Color.ANSI_RESET);
					
					System.out.println("------------------------------------");
				}
			    
			    
			}
			
			else if (deter_unit_state == 2) {
				Unit newUnit=us.getUnit();
				if (newUnit instanceof IfStmt) {
					State newState1 = new State(leaf.mRoot.getLocalVars(), leaf.mRoot.getExecutionOrder(),
							          newUnit.toString(), us.getLine(), leaf.mRoot.getState().getInputUsedIndex());
					
					State newState2 = new State(leaf.mRoot.getLocalVars(), leaf.mRoot.getExecutionOrder(), 
							          newUnit.toString(), us.getLine(), leaf.mRoot.getState().getInputUsedIndex());

					List<TreeNode> newLeafNodes = performIfStmt(leaf, leaf.mRoot.getConstraint(), newState1, newState2, newUnit, UnitIndexes);
					//if branch
					newLeafNodes.get(0).setExecutionOrder( leaf.mRoot.getExecutionOrder()+1 );
					//nextLine has been set to 'goto_target' in performIfStmt
					newLeafNodes.get(0).setReturnFlag( leaf.mRoot.getReturnFlag() );
					newLeafNodes.get(0).setBranchInfo("IF branch from"+leaf.mRoot.getState().getCommandLineNo() );
					
					//else branch
					newLeafNodes.get(1).setExecutionOrder( leaf.mRoot.getExecutionOrder()+1 );
					newLeafNodes.get(1).setNextLine( leaf.mRoot.getNextLine()+1 );
					newLeafNodes.get(1).setReturnFlag( leaf.mRoot.getReturnFlag() );
					newLeafNodes.get(1).setBranchInfo("ELSE branch from"+leaf.mRoot.getState().getCommandLineNo() );

					
					Tree newTree1 = new Tree(newLeafNodes.get(0), leaf);
					Tree newTree2 = new Tree(newLeafNodes.get(1), leaf);
				    leaf.children.add(newTree1);
				    leaf.children.add(newTree2);
					
					System.out.println( Color.ANSI_BLUE+"line '" +us.getUnit().toString()+"'"+ Color.ANSI_RESET);
					System.out.println("Split the tree here.");
					
					System.out.println("------------------------------------");
   
				}
				else if (newUnit instanceof GotoStmt) {
					State newState = new State(leaf.mRoot.getLocalVars(), leaf.mRoot.getExecutionOrder(), 
							         newUnit.toString(), us.getLine(),leaf.mRoot.getState().getInputUsedIndex());
					TreeNode newLeafNode = performGotoStmt(newState, newUnit, UnitIndexes, Units);
					newLeafNode.setConstraint(leaf.mRoot.getConstraint());
					newLeafNode.setExecutionOrder( leaf.mRoot.getExecutionOrder()+1 );
					//'nextLine' has been set in performGotoStmt
					newLeafNode.setReturnFlag( leaf.mRoot.getReturnFlag() );
					Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
					
				    
					System.out.println( Color.ANSI_BLUE+"line '" +us.getUnit().toString()+"'"+ Color.ANSI_RESET);
					//newLeafNode.getState().printForm();
					System.out.println("------------------------------------");
				}
				
				
			}
			else if (deter_unit_state == 3) {
				System.out.println(Color.ANSI_BLUE+"++++++++++++++++ Return +++++++++++++++++"+Color.ANSI_RESET);
				// Add this treenode to returnlist, waiting to print the result
				returnLeaf.add(leaf);
				
				boolean newReturnFlag = true;
			    TreeNode newLeafNode = new TreeNode(leaf.mRoot.getConstraint(), leaf.mRoot.getState(), leaf.mRoot.getExecutionOrder(), leaf.mRoot.getNextLine(), newReturnFlag);
			    Tree newTree = new Tree(newLeafNode, leaf);
			    leaf.children.add(newTree);
				
			}
			
			//deal with invoke(determine output)
			
			else if (deter_unit_state == 4) {
				System.out.println( Color.ANSI_BLUE+"line '" +us.getUnit().toString()+"'"+ Color.ANSI_RESET);
				
				if(us.getUnit().toString().contains("virtualinvoke") ) {
				    int nextLine =  leaf.mRoot.getNextLine()+1 ;  //iterator++;
				    TreeNode newLeafNode = new TreeNode(leaf.mRoot.getConstraint(), leaf.mRoot.getState(), 
				    		                            leaf.mRoot.getExecutionOrder(), nextLine, leaf.mRoot.getReturnFlag());
				    
				    if(us.getUnit().toString().contains("OutputCollector") || us.getUnit().toString().contains("Context")) {
				        String value = (us.getUnit().toString().split(">")[1]).split(",")[1];
				        value = value.replace(")", "");
				        
                        for(String replace:leaf.mRoot.getLocalVars().keySet()) {
				        	
				        	if(value.contains(replace) ) {
				        		
				        		System.out.println("replace "+replace+":"+leaf.mRoot.getLocalVars().get(replace));
				        	    value = value.replace(replace, leaf.mRoot.getLocalVars().get(replace));
				        	}
				        }
				        newLeafNode.getState().update("output", value);
				    }
				    
				    Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
				}
				else if(us.getUnit().toString().contains("specialinvoke")) {
					int nextLine =  leaf.mRoot.getNextLine()+1 ;  //iterator++;
				    TreeNode newLeafNode = new TreeNode(leaf.mRoot.getConstraint(), leaf.mRoot.getState(), 
				    		                            leaf.mRoot.getExecutionOrder(), nextLine, leaf.mRoot.getReturnFlag());
				    if(us.getUnit().toString().contains("Writable") && us.getUnit().toString().contains("init")) {
				    	String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
				        String value = us.getUnit().toString().split(">")[2];
				        value = value.replace(")", "");
				        value = value.replace("(", "");
				        
				        for(String replace:leaf.mRoot.getLocalVars().keySet()) {
				        	
				        	if(value.contains(replace) ) {
				        		
				        		System.out.println("replace "+replace+":"+leaf.mRoot.getLocalVars().get(replace));
				        	    value = value.replace(replace, leaf.mRoot.getLocalVars().get(replace));
				        	}
				        }
				        newLeafNode.getState().update(key, value);
				    
				        
				    }				        
				    Tree newTree = new Tree(newLeafNode, leaf);
				    leaf.children.add(newTree);
				}
			}
			
			else {
				System.out.println( Color.ANSI_BLUE+"line '" +us.getUnit().toString()+"'"+ Color.ANSI_RESET);
				int nextLine =  leaf.mRoot.getNextLine()+1 ;  //iterator++;
				TreeNode newLeafNode = new TreeNode(leaf.mRoot.getConstraint(), leaf.mRoot.getState(), leaf.mRoot.getExecutionOrder(), nextLine, leaf.mRoot.getReturnFlag());
			    Tree newTree = new Tree(newLeafNode, leaf);
			    leaf.children.add(newTree);

			}
		}
	
    }
	
	protected void detectLoop(UnitGraph graph, Map<Unit, Integer> unitIndexes) {
		//only detect one output now
		int currentLine = 0;
		Iterator gIt = graph.iterator();
		while(gIt.hasNext()) {
			Unit u =(Unit)gIt.next();
			if(u instanceof GotoStmt) {
				GotoStmt gtStmt = (GotoStmt) u;
				Unit gotoTarget = gtStmt.getTarget();
				if(unitIndexes.get(gotoTarget) < currentLine) {
					mEnterLoopLine = unitIndexes.get(gotoTarget);
					mOutLoopLine = currentLine;
					System.out.println("loop from line: "+String.valueOf(mEnterLoopLine)+" to "+String.valueOf(mOutLoopLine));
					return;
				}
			}
			currentLine++;
		}
		
	}
	
	//not really need to use it
	protected void detectOutput(UnitGraph G, Tree tree) {
		Iterator iter = G.iterator();
		
		while(iter.hasNext()) {
			Unit u =(Unit)iter.next();
			if(u instanceof InvokeStmt) {
				
				if(u.toString().contains("virtualinvoke")) {
					if(u.toString().contains("OutputCollector") || u.toString().contains("Context")) {
						String value = (u.toString().split(">")[1]).split(",")[1];
				        value = value.replace(")", "");
                        for(String replace:tree.mRoot.getLocalVars().keySet()) {
				        	if(value.contains(replace) ) {
				        	    value = value.replace(replace, tree.mRoot.getLocalVars().get(replace));
				        	}
				        }
				        tree.mRoot.getLocalVars().put("output", value);
				        
					}
				}
				else if(u.toString().contains("specialinvoke")) {
					
				    if(u.toString().contains("Writable") && u.toString().contains("init")) {
				    	String key = (u.toString().split("\\s+")[1]).split("\\.")[0];
				    	String type = u.toString().split(">")[1];
				        String value = u.toString().split(">")[2];
				        value = value.replace(")", "");
				        value = value.replace("(", "");
				        
				        
				        for(String replace:tree.mRoot.getLocalVars().keySet()) {
				        	if(value.contains(replace) ) {
				        	    value = value.replace(replace, tree.mRoot.getLocalVars().get(replace));
				        	}
				        }
				        tree.mRoot.getLocalVars().put(key, value);    
				    }				        
				    
				}
			}
			
		}
	}
	
	
	// TODO: Upgrade to <String, VariableSet> instead of <String, String>
	 protected void generateVars(List<ValueBox> defBoxes){
		for (ValueBox d: defBoxes) {
			Value value = d.getValue();
			String type = value.getType().toString();
			String localVar = value.toString() + "_v";
			mVarsType.put(value.toString(), type);
			mLocalVars.put(value.toString(), localVar);
			System.out.println("Variable " + type + " " + localVar);
			mVarSets.put(value.toString(), new VariableSet(type, localVar));
		}
		// Insert some input (only one input now)
		mLocalVars.put("output", "");
		mVarsType.put("output", "");
		System.out.println("Variable output");
		mVarSets.put("output", new VariableSet());

		mLocalVars.put("beforeLoop", "bL_v");
		mVarsType.put("beforeLoop", "before loop flag");
		System.out.println("Variable boolean beforeLoop");
		mVarSets.put("beforeLoop", new VariableSet("before loop flag", "bL_v"));

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
		State st = new State(mLocalVars, num, u.toString(), command_no);

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
			
			// replace rhs with mLocalVars value
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
				System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
				st.update(var.toString(), "hasNext");
				
			}
			else if (ass_s.contains("next()")) {
				if(mBefore) {mUseNextBeforeLoop = true;}
				//use a new input
				mVarsType.put("input"+st.getInputUsedIndex(), "input type");
				
				ass_s="input"+st.getInputUsedIndex();
				st.addInputUsedIndex();
				st.update(var.toString(), ass_s);
				System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);    
		        }	
				
			
		}
		TreeNode newNode = new TreeNode(null, st, 0, 0, false);
		return newNode;
	}

	protected TreeNode performGotoStmt(State st, Unit u, Map<Unit,Integer> UnitIndexes, List<UnitSet> Units) {
		GotoStmt gt_st = (GotoStmt) u;
		Unit goto_target = gt_st.getTarget();
		TreeNode node;
		
		if( UnitIndexes.get(goto_target) > st.getCommandLineNo()  ) {
		    System.out.println(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + goto_target);
		    node = new TreeNode(null, st, 0, UnitIndexes.get(goto_target), false);
		}
		else {
			node = new TreeNode(null, st, 0, st.getCommandLineNo(), false);
			System.out.println(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + Units.get(st.getCommandLineNo()).getUnit() );
		}
		return node;
	}

	protected List<TreeNode> performIfStmt(Tree parrent, List<String> conditionBefore, State ifBranchState, State elseBranchState, Unit u, Map<Unit,Integer> UnitIndexes) {
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
		
		// replace with mLocalVars value
		Map<String, String> lastEnv = parrent.mRoot.getLocalVars();
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
		
				
		TreeNode ifBranch = new TreeNode(ifCondition, ifBranchState, 0, UnitIndexes.get(goto_target), false);
		TreeNode elseBranch = new TreeNode(elseCondition, elseBranchState, 0, 0, false);
		
		if(parrent.mRoot.getLocalVars().get(conditionStmt.getOp1().toString()) == "hasNext" ) {
			ifBranch.getLocalVars().put(conditionStmt.getOp1().toString(), "0");
			elseBranch.getLocalVars().put(conditionStmt.getOp1().toString(), "1");
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
		if (mLocalVars.containsKey(parts[0])) left = mLocalVars.get(parts[0]);
		String right = parts[2];
		if (mLocalVars.containsKey(parts[2])) left = mLocalVars.get(parts[2]);
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
	

	protected Set<JimpleBody> getCollectorSceneBodies(String reducerClassname) {
		Set<JimpleBody> bodies = new LinkedHashSet<JimpleBody>();
		for (SootClass sc : new LinkedList<SootClass>(Scene.v().getClasses())) {
			// Specify target class name here
			if (sc.resolvingLevel() >= SootClass.BODIES && sc.toString().contains(reducerClassname)) {
				for (SootMethod sm : sc.getMethods()) {			
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