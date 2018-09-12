package statementResolver.soot;

import com.google.common.base.Preconditions;

import statementResolver.Option;
import statementResolver.color.Color;
import statementResolver.executionTree.ExecutionTree;
import statementResolver.executionTree.ExecutionTreeNode;
import statementResolver.state.State;
import statementResolver.state.UnitSet;
import statementResolver.state.VariableSet;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatementResolver {

	private LinkedHashMap<String, String> mLocalVars = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> mVarsType = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, VariableSet> mVarSets = new LinkedHashMap<String, VariableSet>();
	private int mEnterLoopLine = 0;
	private int mOutLoopLine = 0;
	private boolean mUseNextBeforeLoop = false;
	

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
		
		// Does not support String 
		for(UnitSet us : units) {
			String unit = us.getUnit().toString();
			if (unit.contains("String")) {
				System.err.println("Currently support no String operation");
				return;
			}
		}
		
		// Detect where the loop starts
		detectLoop(graph, unitIndexes);
		
		// Starting to analysis
		System.out.println("Starting analysis");
	
		//traverse tree to find leaves and doAnalysis
		State initStateBefore = new State(mLocalVars, 0, null, 0, 0);
		State initStateInter = new State(mLocalVars, 0, null, mEnterLoopLine, 0);
		List<String> initConstraintBefore = new ArrayList<String>();
		List<String> initConstraintInter = new ArrayList<String>();
		
		ExecutionTree beforeLoopTree = new ExecutionTree(
				new ExecutionTreeNode(initConstraintBefore, initStateBefore, 0, 0, false), units, 
				unitIndexes, mEnterLoopLine, mOutLoopLine, mVarsType, true);
		beforeLoopTree.addRootConstraint("beforeLoop == 0");
		beforeLoopTree.executeTree();
		for (Map.Entry<String, String> entry : beforeLoopTree.getVarType().entrySet()) {
			mVarsType.put(entry.getKey(), entry.getValue());
		}
		mUseNextBeforeLoop = beforeLoopTree.useNextBeforeLoop();

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
		/*
		if (interLoopTree.getEndNodes().size() > 0) {
			interLoopTree.getEndNodes().remove(0);
		}
		*/
		toWriteZ3.addAll(interLoopTree.getEndNodes());
		z3FormatBuilder z3Builder = new z3FormatBuilder(mVarsType, 
				beforeLoopTree.getEndNodes(), interLoopTree.getEndNodes(), "z3Format.txt", mUseNextBeforeLoop);
		if (z3Builder.getResult()) {
			System.out.println("RESULT: Prove your reducer to be communicative");
		} else {
			System.out.println("RESULT: Prove your reducer to be NOT communicative");
		}

	}
	
	protected void detectLoop(UnitGraph graph, Map<Unit, Integer> unitIndexes) {
		//only detect one output now
		int currentLine = 0;
		Iterator gIt = graph.iterator();
		while(gIt.hasNext()) {
			Unit u = (Unit)gIt.next();
			if(u instanceof GotoStmt) {
				GotoStmt gtStmt = (GotoStmt) u;
				Unit gotoTarget = gtStmt.getTarget();
				//if(unitIndexes.get(gotoTarget) < currentLine) {
				if(unitIndexes.get(gotoTarget) < currentLine && 
				         (mEnterLoopLine == 0 || unitIndexes.get(gotoTarget) < mEnterLoopLine)) {
					mEnterLoopLine = unitIndexes.get(gotoTarget);
					mOutLoopLine = currentLine;
				}
			}
			currentLine++;
		}
		// System.out.println("loop from line: " + mEnterLoopLine + " to " + mOutLoopLine);
		System.out.println("loop started from line: " + mEnterLoopLine);
		
	}
	
	 protected void generateVars(List<ValueBox> defBoxes){
		for (ValueBox d: defBoxes) {
			Value value = d.getValue();
			String type = value.getType().toString();
			String localVar = value.toString() + "_v";
			mVarsType.put(value.toString(), type);
			mLocalVars.put(value.toString(), localVar);
			mVarSets.put(value.toString(), new VariableSet(type, localVar));
			System.out.println("Variable " + type + " " + localVar);
		}
		// Insert some input (only one input now)
		mLocalVars.put("output", "");
		mVarsType.put("output", "");
		System.out.println("Variable output");

		mLocalVars.put("beforeLoop", "bL_v");
		mVarsType.put("beforeLoop", "boolean");
		System.out.println("Variable boolean beforeLoop");
		
		mLocalVars.put("beforeLoopDegree", "bLD_v");
		mVarsType.put("beforeLoopDegree", "int");
		mVarsType.put("bLD", "int");
		System.out.println("Variable integer beforeLoopDegree");
	 }
    
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