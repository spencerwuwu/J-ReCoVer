
package statementResolver.soot;

import com.google.common.base.Preconditions;

import statementResolver.Option;
import statementResolver.color.Color;
import statementResolver.state.State;
import statementResolver.state.StateUnitPair;
import statementResolver.state.UnitSet;
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

public class StatementResolver {

	private Map<String, Boolean> InputListUsed = new LinkedHashMap<String, Boolean>();
	private Map<String, String> LocalVars = new LinkedHashMap<String, String>();
	private List<State> States = new ArrayList<State>();

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
		
		int current_no = 0;
		int command_line_no = 1;

		System.out.println("=======================================");	
		
		for (JimpleBody body : this.get_colloctor_SceneBodies()) {
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
				Unit u = (Unit)gIt.next();	
				Units.add(new UnitSet(u, command_line_no));
				if (UnitIndexs.containsKey(u)) {
					UnitIndexs.put(u, command_line_no-1);
				}
				command_line_no++;
			}
			System.out.println("=======================================");	
			
			// Starting to analysis
			int iterator = 0;
			boolean return_flag = false;
			int deter_unit_state = 0;
			while (!return_flag && iterator < Units.size()) {
				UnitSet us = Units.get(iterator);
				deter_unit_state = deterUnit(us.getUnit());
				if (deter_unit_state == 1) {
					State st = handleUnit(us.getUnit(), current_no, us.getLine()).getState();
					States.add(st);
					System.out.println( Color.ANSI_BLUE+"line '" +us.getUnit().toString()+"'"+ Color.ANSI_RESET);
					current_no++;
					st.printForm();
					System.out.println("------------------------------------");
					iterator++;
				}
				else if (deter_unit_state == 2) {
					StateUnitPair su = handleUnit(us.getUnit(), current_no, us.getLine());
					States.add(su.getState());
					System.out.println(Color.ANSI_BLUE + "line '" + us.getUnit().toString() + "'"
							+ Color.ANSI_RESET);
					current_no++;
					su.getState().printForm();
					System.out.println("------------------------------------");
					if (su.getUnit() != null) {
						iterator = UnitIndexs.get(su.getUnit());
					}
					else iterator++;
					
				}
				else if (deter_unit_state == 3) {
					System.out.println(Color.ANSI_BLUE+"++++++++++++++++ Return +++++++++++++++++"+Color.ANSI_RESET);
					return_flag = true;
					break;
				}
				else if (deter_unit_state == 4) {
					System.out.println("ggininder");
					System.out.println(us.getUnit().toString());
					iterator++;
				}
				else {
					System.out.println(us.getUnit().toString());
					iterator++;
				}
			}
			
		} // end of main analysis
		
		if (op.cfg_flag) {
			// TODO
			/*
			BlockGraph blockGraph = new BriefBlockGraph(body);
			System.out.println(blockGraph);
			}
			*/
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
	
	 protected void generateVars(List<ValueBox> defBoxes){
		for (ValueBox d: defBoxes) {
			Value value = d.getValue();
			String str = d.getValue().toString();
			LocalVars.put(value.toString(), "");
			System.out.println("Variable " + str);
		}
		
		// Insert input to be X0, X1, X2
		for (int i = 0; i < 3; i++) {
			String name = "X" + i;
			String value = "x" + i;
			InputListUsed.put(name, false);
			LocalVars.put(name, value);
			System.out.println("Variable " + name);
		}
	 }
	
	// TODO: match all cases.
	// 0 : null
	// 1 : no unit return, eg: AssignStmt
	// 2 : going to a unit target, eg: GotoStmt
	// 3 : return
	// 4 : testing
	protected int deterUnit(Unit u) {
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
			return 0;
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
	
	protected StateUnitPair performAssignStmt(State st, Unit u) {
		DefinitionStmt ds = (DefinitionStmt) u;
		Value var = ds.getLeftOp();
		Value assignment = ds.getRightOp();
		String ass_s = assignment.toString();
		// Normal assignment
		if (!ass_s.contains("Iterator")) {
			String tmp = ass_s.replaceAll("\\(.*?\\)\\s+", ""); // removing quotes
			ass_s = tmp;
			if (ass_s.contains("int get")) {
				//tmp = ass_s.replace("virtualinvoke\\s+", "");
				tmp = ass_s.split("\\s+")[1];
				tmp = tmp.split("\\.")[0];
				ass_s = tmp;
			}
			for (String re_var: LocalVars.keySet()) {
				while (ass_s.contains(re_var)) {
					System.out.println("replace "+re_var+": " + LocalVars.get(re_var));
					tmp = ass_s.replace(re_var, LocalVars.get(re_var));
					ass_s = tmp;
				}
			}
			System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
			st.update(var.toString(), ass_s);
		}
		else { // Handling iterator relative assignments
			if (ass_s.contains("hasNext()")) {
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
				System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
				st.update(var.toString(), ass_s);
			}
			else if (ass_s.contains("next()")) {
				for(String in_var : InputListUsed.keySet()) {
					if (!InputListUsed.get(in_var)) {
						InputListUsed.put(in_var, true);
						ass_s = in_var;
						break;
					}
				}
				System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
				st.update(var.toString(), ass_s);
			}
		}
		StateUnitPair su = new StateUnitPair(st, null);
		return su;
	}

	protected StateUnitPair performGotoStmt(State st, Unit u) {
		GotoStmt gt_st = (GotoStmt) u;
		Unit goto_target = gt_st.getTarget();
		System.out.println(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + goto_target);
		StateUnitPair su = new StateUnitPair(st, goto_target);
		return su;
	}

	protected StateUnitPair performIfStmt(State st, Unit u) {
		IfStmt if_st = (IfStmt) u;
		Unit goto_target = if_st.getTargetBox().getUnit();
		Value condition = if_st.getCondition();
		System.out.println(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + goto_target + Color.ANSI_GREEN + " when " + Color.ANSI_RESET + condition);
		if (checkCondition(condition)) {
			StateUnitPair su = new StateUnitPair(st, goto_target);
			return su;
		} else {
			StateUnitPair su = new StateUnitPair(st, null);
			return su;
		}
	}

	protected StateUnitPair performIdentityStmt(State st, Unit u) {
		DefinitionStmt ds = (DefinitionStmt) u;
		Value var = ds.getLeftOp();
		Value assignment = ds.getRightOp();
		String assignment_tail = assignment.toString().split("\\.(?=[^\\.]+$)")[1]; // Preserve only org.apache.hadoop.io.'IntWritable'
		System.out.println(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + assignment_tail);
		st.update(var.toString(), assignment_tail);
		StateUnitPair su = new StateUnitPair(st, null);
		return su;
	}
	
	protected StateUnitPair handleUnit(Unit u, int num, int command_no) {
		System.out.println("++ no: " + num + ", line: " + command_no);
		State st = new State(LocalVars, num, u.toString(), command_no);

		if (u instanceof JLookupSwitchStmt) {
		}
		else if (u instanceof AssignStmt) {			
			return performAssignStmt(st, u); 
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
			return performGotoStmt(st, u);
		}
		else if (u instanceof NoSuchLocalException) {			
		}
		else if (u instanceof NullConstant) {			
		}
		else if (u instanceof IfStmt) {
			return performIfStmt(st, u);
		}
		else if (u instanceof IdentityStmt) {			
			return performIdentityStmt(st, u); 	
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
	}
	
	protected Boolean checkCondition(Value condition) {
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