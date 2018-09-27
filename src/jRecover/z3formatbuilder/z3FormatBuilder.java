package jRecover.z3formatbuilder;

import java.util.HashMap;
import java.util.Map;

import jRecover.color.Color;
import jRecover.executionTree.ExecutionTreeNode;

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class z3FormatBuilder {
	Map<String, String> typeTable;
	List<String> mGlobalVariables = new ArrayList<String>();
	List<ExecutionTreeNode> mBeforeNodes = new ArrayList<ExecutionTreeNode>();
	List<ExecutionTreeNode> mInnerNodes = new ArrayList<ExecutionTreeNode>();
	Map<String, Boolean>mVariables = new HashMap<String, Boolean>();
	Map<String, Boolean>mOutputRelated;
	boolean mUsingNextBeforeLoop = false;
	File mFile;
	PrintWriter mOutput;
	
	public z3FormatBuilder() {
		typeTable = new HashMap<String, String>();
		mFile = new File("z3Format");
		
		try {
		    mFile.createNewFile();
		    System.out.println("Create file successfully");
		    System.out.println(mFile.getAbsolutePath());
		} catch(IOException ex) {
			ex.printStackTrace();
		}
		
		try {
		    mOutput = new PrintWriter(mFile);
		} catch(FileNotFoundException f){
			f.printStackTrace();
		}
	}

	public z3FormatBuilder(Map<String, String> table, List<ExecutionTreeNode> beforeNodes, List<ExecutionTreeNode> interNodes, 
			String filename, boolean useNextFlag, Map<String, Boolean> outputRelated) {
		typeTable = table;
		mBeforeNodes.addAll(beforeNodes);
		mInnerNodes.addAll(interNodes);
		mUsingNextBeforeLoop = useNextFlag;
		mOutputRelated = outputRelated;
		mFile = new File(filename);
		System.out.print("Using getNext before loop: ");
		System.out.print(mUsingNextBeforeLoop);
		System.out.print("\n");
		
		try {
		    mFile.createNewFile();
		    System.out.println("Create file successfully");
		} catch(IOException ex) {
			ex.printStackTrace();
		}
		
		try {
		    mOutput = new PrintWriter(mFile);
		} catch(FileNotFoundException f){
			f.printStackTrace();
		}
	}
	
	public boolean getResult() {
		writeZ3Format();

		Process process = null;
		try {
			process = new ProcessBuilder("z3", mFile.getAbsolutePath()).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line = null, result = null;
		System.out.println("z3 Result: ");
		try {
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				result = line;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n");

		if (result == null) {
			System.err.println("Error in z3\n");
			return false;
		}

		if (result.contains("unsat")) {
			return true;
		} else {
			return false;
		}
	}
	
	public void writeZ3Format() {
		variableTypeDeclare();
		
		int stage = 1;
		while (stage <= 2) {
			constructFormula(stage, 1);
			constructFormula(stage, 2);
			stage += 1;
		}
		//mOutput.append("(assert (not (= input0_1 input0_2)))\n");
		mOutput.append("(assert (not (= input0_1_r1 input0_2_r1)))\n");
		mOutput.append("(assert (= input0_1_r1 input0_2_r2))\n");
		mOutput.append("(assert (= input0_2_r1 input0_1_r2))\n");
		
		if (!mUsingNextBeforeLoop) {
			mOutput.append("(assert (= beforeLoop_1_r1 1))\n");
		}
		mOutput.append("(assert (= beforeLoop_1_r1 beforeLoop_1_r2))\n");
		mOutput.append("(assert (= beforeLoop_2_r1 1))\n");
		mOutput.append("(assert (= beforeLoop_2_r2 1))\n");

		String finalAssertion = "";
		boolean noVariable = true;
		for (String key : mVariables.keySet()) {
			if (key.contains("input")) continue;
			if (!mOutputRelated.get(key)) continue;
			noVariable = false;
			//if (mConditionRelated.get(key)) continue;
			if (finalAssertion.length() == 0) {
				finalAssertion = "(not (= " + key + "_2_r1 " + key + "_2_r2))\n"; 
			} else {
				finalAssertion = "(or " + finalAssertion + "(not (= " + key + "_2_r1 " + key + "_2_r2)))\n"; 
			}
		}
		if (noVariable) finalAssertion = "(not (= 1 1))";
		mOutput.append("(assert " + finalAssertion + ")\n");

		mOutput.append("(check-sat)\n");
		mOutput.flush();

	}
	
	protected void constructFormula(int stage, int round) {
		// Generate formula for each variable in each round
		for (String key : mVariables.keySet()) {
			String finalValue = "";
			for (ExecutionTreeNode node : mBeforeNodes) {
				if (node.getLocalVars().get(key) == null) continue;
				String value = combineValueCondition(node, key, stage, round);
				if (value.length() == 0) continue;
				
				if (finalValue.length() == 0) {
					finalValue = value;
				} else {
					finalValue = "(or " + finalValue + " " + value + ")\n";
				}
			}
			for (ExecutionTreeNode node : mInnerNodes) {
				if (node.getLocalVars().get(key) == null) continue;
				String value = combineValueCondition(node, key, stage, round);
				if (value.length() == 0) continue;

				if (finalValue.length() == 0) {
					finalValue = value;
				} else {
					finalValue = "(or " + finalValue + " " + value + ")\n";
				}
			}
			if (finalValue.length() == 0) continue;
			
			//if (mOutputRelated.get(key)) {
				if (finalValue.contains("hasNext")) {
					mOutputRelated.put(key, false);
					continue;
				}
				finalValue = "(assert \n" + finalValue + ")";
				mOutput.append(finalValue + "\n");
			//}
		}
		
		mOutput.flush();
	}
	
	protected String combineValueCondition(ExecutionTreeNode node, String var, int stage, int round) {
		String condition = generateConditions(node.getConstraint(), stage, round);
		String valueTokens[] = node.getLocalVars().get(var).split("\\s+");
		String value = "";
		for (String token : valueTokens) {
			if (token.contains("_v")) {
				token = token.replace("_v", "_" + (stage - 1) + "_r" + round);
			} else if (token.contains("input")) {
				token = token + "_" + stage + "_r" + round;
			}
			if (value.length() == 0) value = token;
			else value = value + " " + token;
		}
		if (value.length() == 0) return "";

		value = "(= " + var + "_" + stage + "_r" + round + " " + value + ")";
		return "(and " + condition + " " + value + ")\n";
	}
	
	protected String generateConditions(List<String> constraints, int stage, int round) {
		String conditions = "";
		for (String constraint : constraints) {
			String tokens[] = constraint.split("\\s+");
			String operation = "";
			String lhs = "";
			String rhs = "";
			String condition = "";
			boolean negative = false;
			if (tokens.length == 4 && tokens[0].equals("!")) {
				lhs = tokens[1];
				operation = tokens[2];
				rhs = tokens[3];
				negative = true;
			} else {
				lhs = tokens[0];
				operation = tokens[1];
				rhs = tokens[2];
			}
			
			if (lhs.equals("beforeLoopDegree")) continue;
			
			for (String key : mVariables.keySet()) {
				if (lhs.equals(key)) {
					lhs = lhs + "_" + stage + "_r" + round;
					break;
				}
			}
			for (String key : mVariables.keySet()) {
				if (rhs.equals(key)) {
					rhs = rhs + "_" + stage + "_r" + round;
					break;
				}
			}

			if (operation.equals("!=")) {
				condition = "(not (= " + lhs + " " + rhs + "))";
			} else if (operation.equals("==")) {
				condition = "(= " + lhs + " " + rhs + ")";
			} else {
				condition = "(" + operation + " " + lhs + " " + rhs + ")";
			}
			
			if (negative) {
				condition = "(not " + condition + ")";
			}
			
			if (conditions.length() == 0) conditions = condition;
			else conditions = "(and " + conditions + " " + condition + ")";
		}
		return conditions;
	}
	
	public void variableTypeDeclare() {
		/*
		  initialize variable
		  initial version would be the same, but internal version(_1, _2) maybe not.
		  result formula wouldn't take variable with '$' into account.
		*/
		for(String variable : typeTable.keySet()) {
			String type = typeTable.get(variable);
			String var = variable.replace("_v", "");

			if (type == "int" || type == "byte" || type == "short" || type == "long"
					|| type == "input type" || type == "before loop flag") {
				type = "Int";
				mVariables.put(var, false);
			} else if (type == "double" | type == "float" ) {
				type = "Real";
				mVariables.put(var, false);
			} else if (type == "boolean" ) {
				type = "Int";
				mVariables.put(var, false);
			} else if (type.contains("beforeLoopDegree")) {
				type = "Int";
				mVariables.put(var, false);
			} else if (type.contains("beforeLoop")) {
				type = "Int";
				mVariables.put(var, false);
			} else if (type == "") {
				//deal with output
				type = "Int";
				mVariables.put(var, false);
			} else if (type.contains("Object")){
				type = "Real";
				mVariables.put(var, false);
			} else if (type.contains("IntWritable") || type.contains("LongWritable")) {
				type = "Int";
			    System.out.println(variable + " " + Color.ANSI_RED + typeTable.get(variable) + " -> Int" + Color.ANSI_RESET);
				mVariables.put(var, false);
			} else {
				// Not supported in z3 Format
			    System.out.println(variable + " " + Color.ANSI_RED + typeTable.get(variable) + Color.ANSI_RESET);
			    continue;
			}
			int index = 0;
			while (index <= 2) {
				mOutput.append("(declare-const " + var + "_" + index + "_r1" + " " + type +")\n");
				mOutput.append("(declare-const " + var + "_" + index + "_r2" + " " + type +")\n");
				
			    index += 1;
			}
			if (!var.contains("input")) mOutput.append("(assert (= " + var + "_0_r1 " + var + "_0_r2" + "))\n");
		}
		mOutput.append("(declare-const null Int)\n");
		mOutput.append("(assert (= null 0))\n");
		
		// Declare z3 variable for global variable
		for (ExecutionTreeNode node : mBeforeNodes) {
			for (String value : node.getLocalVars().values()) catchGlobalVariable(node.getLocalVars(), value);
			for (String value : node.getConstraint()) catchGlobalVariable(node.getLocalVars(), value);
		}

		for (ExecutionTreeNode node : mInnerNodes) {
			for (String value : node.getLocalVars().values()) catchGlobalVariable(node.getLocalVars(), value);
			for (String value : node.getConstraint()) catchGlobalVariable(node.getLocalVars(), value);
		}
		
		for (String element : mGlobalVariables) {
			mOutput.append("(declare-const " + element + " Int)\n");
		}
		
		mOutput.flush();
	}

	
	protected void catchGlobalVariable(Map<String, String> vars, String value) {
		String valueSet[] = value.split("\\s+");
		for (String element : valueSet) {
			if (!element.contains("(") 
					&& !element.contains(")") 
					&& !element.contains("_v")
					&& !element.contains("input")
					&& !element.contains("!")
					&& !element.contains("=")
					&& !element.contains("<")
					&& !element.contains(">")
					&& !element.contains("+")
					&& !element.contains("-")
					&& !element.contains("*")
					&& !element.contains("/")
					&& !element.contains("mod")
					&& !element.contains("null")
					&& !element.matches("-?[0-9]*\\.?[0-9]*")
					&& !vars.containsKey(element)) {
				if (!mGlobalVariables.contains(element)) mGlobalVariables.add(element);
			}
		}
	}
		
	
} 