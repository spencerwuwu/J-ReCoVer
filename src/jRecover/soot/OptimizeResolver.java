package jRecover.soot;

import com.google.common.base.Preconditions;

import jRecover.Option;
import jRecover.color.Color;
import jRecover.optimize.executionTree.ExecutionTree;
import jRecover.optimize.executionTree.ExecutionTreeNode;
import jRecover.optimize.state.Variable;
import jRecover.optimize.z3FormatPipeline.Z3FormatPipeline;
import jRecover.optimize.state.UnitSet;
import soot.Body;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
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

	private LinkedHashMap<String, Variable> mLocalVars = new LinkedHashMap<String, Variable>();
	private LinkedHashMap<String, String> mVarsType = new LinkedHashMap<String, String>();
	Map<String, Boolean>mOutputRelated = new LinkedHashMap<String, Boolean>();
	private int mEnterLoopLine = 0;
	private int mOutLoopLine = 0;
	private boolean mUseNextBeforeLoop = false;
	private boolean mNoLoop = false;
	

	Option mOption = new Option();

	public OptimizeResolver() {
		this(new ArrayList<String>());
	}
	
	public OptimizeResolver(List<String> resolvedClassNames) {
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
					e.printStackTrace();
				}
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
			log(key + ": \t" + mOutputRelated.get(key));
		}
		log("======================================");
		
		// Starting to analysis
		logAll("Start analyzing");
		
	
		//traverse tree to find leaves and doAnalysis
		ExecutionTreeNode beforeNode = new ExecutionTreeNode(null, null, mLocalVars, 0, false); 
		ExecutionTreeNode innerNode = new ExecutionTreeNode(null, null, mLocalVars, mEnterLoopLine, false); 

		ExecutionTree beforeLoopTree = new ExecutionTree(beforeNode, units, unitIndexes, 
				mEnterLoopLine, mOutLoopLine, mVarsType, true, mOption);

		beforeLoopTree.executeTree();
		for (Map.Entry<String, String> entry : beforeLoopTree.getVarType().entrySet()) {
			mVarsType.put(entry.getKey(), entry.getValue());
		}
		mUseNextBeforeLoop = beforeLoopTree.useNextBeforeLoop();
		logAll("beforeLoop finished");

		ExecutionTree innerLoopTree = new ExecutionTree(innerNode, units, unitIndexes, 
				mEnterLoopLine, mOutLoopLine, mVarsType, false, mOption);
		innerLoopTree.executeTree();
		
		beforeLoopTree.print();
		innerLoopTree.print();
		logAll("innerLoop finished");
		
		for (Map.Entry<String, String> entry : innerLoopTree.getVarType().entrySet()) {
			mVarsType.put(entry.getKey(), entry.getValue());
		}

		logAll("Starting z3 builder...\n");

		Z3FormatPipeline z3Builder = new Z3FormatPipeline(mVarsType, 
				beforeLoopTree.getEndNodes(), innerLoopTree.getEndNodes(), mUseNextBeforeLoop, mOutputRelated, mOption, mNoLoop);

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
			mLocalVars.put(valueName, new Variable(localVar));
			log("Variable " + type + " " + localVar);
		}
		// Insert some input (only one input now)
		//mLocalVars.put("output", "0");
		//mVarsType.put("output", "");
		//log("Variable output");

		mLocalVars.put("beforeLoop", new Variable("beforeLoop_v"));
		mVarsType.put("beforeLoop", "int");
		log("Variable boolean beforeLoop");
		
		mVarsType.put("input0", "input type");

		mOutputRelated.put("beforeLoop", true);
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
