package jRecover.soot;

import com.google.common.base.Preconditions;

import jRecover.Option;
import jRecover.color.Color;
import jRecover.executionTree.ExecutionTree;
import jRecover.executionTree.ExecutionTreeNode;
import jRecover.state.State;
import jRecover.state.UnitSet;
import jRecover.z3FormatPipeline.Z3FormatPipeline;
import jRecover.z3formatbuilder.*;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptimizeResolver {

	private LinkedHashMap<String, String> mLocalVars = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> mVarsType = new LinkedHashMap<String, String>();
	Map<String, Boolean>mOutputRelated = new LinkedHashMap<String, Boolean>();
	Map<String, Boolean>mConditionRelated = new LinkedHashMap<String, Boolean>();
	private int mEnterLoopLine = 0;
	private int mOutLoopLine = 0;
	private boolean mUseNextBeforeLoop = false;
	private boolean mNoLoop = false;
	

	private final List<String> resolvedClassNames;
	Option mOption = new Option();

	public OptimizeResolver() {
		this(new ArrayList<String>());
	}
	
	public OptimizeResolver(List<String> resolvedClassNames) {
		this.resolvedClassNames = resolvedClassNames;
		// first reset everything:
		soot.G.reset();
	}

	public void run(String input, String classPath, Option option, String reducerClassname) {
		SootRunner runner = new SootRunner();
		runner.run(input, classPath);
		mOption = option;
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
			if (mOption.jimple_flag) jimpleAnalysis(body);
			else {
				try {
					completeAnalysis(body);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// TODO: Not really doing this tbh
		if (mOption.cfg_flag) {
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

	public void jimpleAnalysis(JimpleBody body) {
		System.out.println(body.toString());
	}

	public void completeAnalysis(JimpleBody body) throws IOException {
		logAll("Initializing..");

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
		
		log(Color.ANSI_BLUE + body.toString() + Color.ANSI_RESET);
		log("=======================================");			

		// Storing variables and detect main loop location
		List<ValueBox> defBoxes = body.getDefBoxes();
		generateVars(defBoxes);
		log("=======================================");	

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
				if (!mOption.silence_flag) System.err.print(Color.ANSI_RED + unit.toString() 
				+ Color.ANSI_RESET + " contains string operations or assertions \n");
				//return;
			}
		}

		// Detect where the loop starts
		detectLoop(graph, unitIndexes);
		
		checkOutputRelated(units);

		log("====== Output Related ======");
		for (String key : mOutputRelated.keySet()) {
			//log(key + ": \t" + mOutputRelated.get(key) + "/" + mConditionRelated.get(key));
			log(key + ": \t" + mOutputRelated.get(key));
		}
		log("======================================");
		
		// Starting to analysis
		logAll("Start analyzing");
		
	
		//traverse tree to find leaves and doAnalysis
		State initStateBefore = new State(mLocalVars, 0, null, 0, 0);
		State initStateInter = new State(mLocalVars, 0, null, mEnterLoopLine, 0);
		List<String> initConstraintBefore = new ArrayList<String>();
		List<String> initConstraintInter = new ArrayList<String>();
		
		ExecutionTree beforeLoopTree = new ExecutionTree(
				new ExecutionTreeNode(initConstraintBefore, initStateBefore, 0, 0, false), units, 
				unitIndexes, mEnterLoopLine, mOutLoopLine, mVarsType, true, mOption);
		beforeLoopTree.addRootConstraint("beforeLoop == 0");
		beforeLoopTree.executeTree();
		for (Map.Entry<String, String> entry : beforeLoopTree.getVarType().entrySet()) {
			mVarsType.put(entry.getKey(), entry.getValue());
		}
		mUseNextBeforeLoop = beforeLoopTree.useNextBeforeLoop();
		logAll("beforeLoop finished");

		ExecutionTree innerLoopTree = new ExecutionTree(
				new ExecutionTreeNode(initConstraintInter, initStateInter, 0, mEnterLoopLine, false), units, 
				unitIndexes, mEnterLoopLine, mOutLoopLine, mVarsType, false, mOption);
		innerLoopTree.addRootConstraint("beforeLoop != 0");
		innerLoopTree.executeTree();
		
		beforeLoopTree.print();
		innerLoopTree.print();
		logAll("innerLoop finished");
		
		for (Map.Entry<String, String> entry : innerLoopTree.getVarType().entrySet()) {
			mVarsType.put(entry.getKey(), entry.getValue());
		}
		
		logAll("Starting z3 builder...\n");
        
		List<ExecutionTreeNode> toWriteZ3 = new ArrayList<ExecutionTreeNode>();
		toWriteZ3.addAll(beforeLoopTree.getEndNodes());
		toWriteZ3.addAll(innerLoopTree.getEndNodes());
		Z3FormatPipeline z3Builder = new Z3FormatPipeline(mVarsType, 
				beforeLoopTree.getEndNodes(), innerLoopTree.getEndNodes(), mUseNextBeforeLoop, mOutputRelated, mOption, mNoLoop);
		/*
		z3FormatBuilder z3Builder = new z3FormatBuilder(mVarsType, 
				beforeLoopTree.getEndNodes(), interLoopTree.getEndNodes(), "z3", mUseNextBeforeLoop, mOutputRelated);
				*/

		if (z3Builder.getResult()) {
			System.out.println("RESULT: Proved to be commutative");
		} else {
			System.out.println("RESULT: CANNOT prove to be commutative");
		}

	}
	
	protected void checkOutputRelated(List<UnitSet> units) {
		List<Unit> unitList = new ArrayList<Unit>();

		for (String key : mLocalVars.keySet()) {
			mOutputRelated.put(key, false);
			mConditionRelated.put(key, false);
		}
		if (mNoLoop) return;
		int index = units.size() - 1;
		while (index > mOutLoopLine) {
			unitList.add(units.get(index).getUnit());
			index -= 1;
		}
		
		index = units.size();
		for (Unit unit : unitList) {

			if (unit instanceof AssignStmt) {
				parseAssignment(unit, index);

			} else if (unit instanceof IfStmt) {
				IfStmt if_st = (IfStmt) unit;
				Value condition = if_st.getCondition();
				String vars[] = condition.toString().split("\\s+");
				for (String var : vars) {
					if (mOutputRelated.containsKey(var)) {
						mOutputRelated.put(var, true);
						mConditionRelated.put(var, true);
					}
				}

			} else if(unit.toString().contains("virtualinvoke")) {
				parseVirtualinvoke(unit, index);

			} else if(unit.toString().contains("specialinvoke")) {
				if(unit.toString().contains("init")) {
					String var = (unit.toString().split("\\s+")[1]).split("\\.")[0];
					String value = unit.toString().split(">")[2];
					value = value.replace(")", "");
					value = value.replace("(", "");
					if (value.length() == 0) {
						index -= 1;
						continue;
					}
					
					if (mOutputRelated.containsKey(var) && mOutputRelated.get(var)) {
						mOutputRelated.put(value, true);
					}
				}

			}
			index -= 1;
		}
		
		/*
		if (emphsisTail) return;

		List<Unit> unitList2 = new ArrayList<Unit>();
		index = 0;
		while (index < units.size()) {
			unitList2.add(units.get(index).getUnit());
			index += 1;
		}
		for (Unit unit : unitList2) {
			if (unit instanceof AssignStmt) {
				DefinitionStmt ds = (DefinitionStmt) unit;
				String var = ds.getLeftOp().toString();
				String ass_s = ds.getRightOp().toString();
				if (!ass_s.contains("Iterator") && !ass_s.contains("invoke")) {
					if (ass_s.contains("<")) {
						ass_s = ass_s.split("\\s+")[2].replace(">", "");
					}
					String ass[] = ass_s.split("\\s+");
					if (ass.length <= 1) continue;
					if (ass.length == 2) {
						if (ass[0].equals("(long)")
								|| ass[0].equals("(int)")
								|| ass[0].equals("(float)")
								||  ass[0].equals("(double)")) {
							continue;
						}
					}
					for (String value : ass) {
						if (mOutputRelated.containsKey(value) && mOutputRelated.get(value)) {
							mOutputRelated.put(var, true);
							break;
						}
					}
				}
			}
		}
		*/

	}
	
	protected void parseAssignment(Unit unit, int currentLine) {
		DefinitionStmt ds = (DefinitionStmt) unit;
		String var = ds.getLeftOp().toString();
		String ass_s = ds.getRightOp().toString();
		if (!ass_s.contains("Iterator")) {
			// removing quotes, eg: (org.apache.hadoop.io.IntWritable) $r6 -> $r6
			ass_s = ass_s.replaceAll("\\(.*?\\)\\s+", "");
			// handle virtualinvoke, eg: virtualinvoke $r7.<org.apache.hadoop.io.IntWritable: int get()>() -> $r7
			if (ass_s.contains("virtualinvoke")) {
				ass_s = ass_s.split("\\s+")[1].split("\\.")[0];
				if (mOutputRelated.containsKey(ass_s)) mOutputRelated.put(ass_s, true);
				return;
			}
			// handle staticinvoke, eg: staticinvoke <java.lang.Long: java.lang.Long valueOf(long)>(l0) -> l0
			if (ass_s.contains("staticinvoke")) {
				ass_s = ass_s.split(">")[1].replace("(", "").replace(")", "");
				if (mOutputRelated.containsKey(ass_s)) mOutputRelated.put(ass_s, true);
				return;
			}
			
			// eg: $i3 = <reduce_test.context141_200_30_8: int k>
			if (ass_s.contains("<")) {
				ass_s = ass_s.split("\\s+")[2].replace(">", "");
			}
			String ass[] = ass_s.split("\\s+");
			
			// Continue if assignment is being operated (exclude directly assignment of input)
			if (ass.length <= 1) return;
			//if (mOutputRelated.containsKey(var)) mOutputRelated.put(var, true);
			for (String str : ass) {
				if (mOutputRelated.containsKey(var) && mOutputRelated.get(var))
					if (mOutputRelated.containsKey(str)) mOutputRelated.put(str, true);
			}
		}
	}
	
	protected boolean parseVirtualinvoke(Unit unit, int currentLine) {
		if(unit.toString().contains("OutputCollector") || unit.toString().contains("Context")) {
			String key = (unit.toString().split(">")[1]).split(",")[0];
			String value = (unit.toString().split(">")[1]).split(",")[1];
			key = key.replace("(", "");
			value = value.replace(")", "");
			value = value.replace(" ", "");
			mOutputRelated.put(value, true);
			mOutputRelated.put(key, true);
			if (currentLine > mOutLoopLine) return true;
			else return false;
		} else {
			String key = (unit.toString().split("\\s+")[1]).split("\\.")[0];
			if (mOutputRelated.containsKey(key) && mOutputRelated.get(key)) {
				String value = unit.toString().split(">")[1];
				value = value.replace(")", "");
				value = value.replace("(", "");
				mOutputRelated.put(value, true);
			}
		}
		return false;
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
				         (mEnterLoopLine == 0 || unitIndexes.get(gotoTarget) <= mEnterLoopLine)) {
					mEnterLoopLine = unitIndexes.get(gotoTarget);
					mOutLoopLine = currentLine;
				}
			}
			currentLine++;
		}
		log("loop from line: " + mEnterLoopLine + " to " + mOutLoopLine);
		if (mEnterLoopLine == 0 && mOutLoopLine == 0) {
			logAll("No Loop Detected!");
			logAll("");
			mNoLoop = true;
		}
		
	}
	
	 protected void generateVars(List<ValueBox> defBoxes){
		for (ValueBox d: defBoxes) {
			Value value = d.getValue();
			String valueName = value.toString();
			// Parse <reduce_test.context141_200_30_8: k> -> k
			if (valueName.contains("<")) {
				valueName = valueName.split("\\s+")[1].replaceAll(">", "");
			}
			String type = value.getType().toString();
			String localVar = valueName + "_v";
			mVarsType.put(valueName, type);
			mLocalVars.put(valueName, localVar);
			log("Variable " + type + " " + localVar);
		}
		// Insert some input (only one input now)
		mLocalVars.put("output", "0");
		mVarsType.put("output", "");
		log("Variable output");

		mLocalVars.put("beforeLoop", "bL_v");
		mVarsType.put("beforeLoop", "beforeLoop");
		log("Variable boolean beforeLoop");
		
		mLocalVars.put("beforeLoopDegree", "0");
		mVarsType.put("beforeLoopDegree", "beforeLoopDegree");
		log("Variable integer beforeLoopDegree");

		mVarsType.put("input0", "input type");

		mOutputRelated.put("beforeLoop", true);
		mOutputRelated.put("beforeLoopDegree", true);
		mConditionRelated.put("beforeLoop", true);
		mConditionRelated.put("beforeLoopDegree", true);
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

	public void logAll(String str) {
		if (!mOption.silence_flag) System.out.println(str);
		else System.out.println("[  StatR]  " + str);
	}
	
	public void log(String str) {
		if (!mOption.silence_flag) System.out.println(str);
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
