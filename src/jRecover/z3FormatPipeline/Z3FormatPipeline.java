package jRecover.z3FormatPipeline;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jRecover.Option;
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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class Z3FormatPipeline {
	Map<String, String> mTypeTable;
	List<String> mGlobalVariables = new ArrayList<String>();
	List<ExecutionTreeNode> mBeforeNodes = new ArrayList<ExecutionTreeNode>();
	List<ExecutionTreeNode> mInnerNodes = new ArrayList<ExecutionTreeNode>();
	Map<String, Boolean>mVariables = new HashMap<String, Boolean>();
	Map<String, Boolean>mOutputRelated;
	boolean mUsingNextBeforeLoop = false;
	
	PipedInputStream mStr2pipe = new PipedInputStream();
	OutputStream mPipe2zt = null;
	InputStream mZt2pipe = null;
	PipedOutputStream mOutput2str = new PipedOutputStream();
	List<String> mPipeContent = new LinkedList<String>();
	Option mOption = new Option();
	

	public Z3FormatPipeline(Map<String, String> table, List<ExecutionTreeNode> beforeNodes, List<ExecutionTreeNode> interNodes, 
			boolean useNextFlag, Map<String, Boolean> outputRelated, Option op) {
		mTypeTable = table;
		mBeforeNodes.addAll(beforeNodes);
		mInnerNodes.addAll(interNodes);
		mUsingNextBeforeLoop = useNextFlag;
		mOutputRelated = outputRelated;
		mOption = op;
		logAll("Use .next().get() before loop: " + mUsingNextBeforeLoop);
	}
	
	public boolean getResult() throws IOException {
		logAll("Total nodes: " + (mBeforeNodes.size()+mInnerNodes.size()));
		logAll("Total variables: " + mTypeTable.size());
		logAll("");

		logAll("Generating formula...");
		writeZ3Format();
		
		logAll("Starting z3...");
		String result = "";
		try {
			Process process = Runtime.getRuntime().exec("z3 -in");
			mZt2pipe = process.getInputStream();
			mPipe2zt = process.getOutputStream();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
		
		for (String formula : mPipeContent) {
			byte[] bytes = formula.getBytes();
			int read = 0;
			do {
				if (formula.length() - read > 1024) {
					mPipe2zt.write(bytes, read, 1024);
					read += 1024;
				} else {
					mPipe2zt.write(bytes, read, formula.length() - read);
					read = formula.length();
				}
			} while (read < formula.length());
		}
		mPipe2zt.close();
		
		int i = 0;
		while ((i = mZt2pipe.read()) != -1) {
			result = result + (char)i;
		}
		if (result.length() == 0) {
			System.err.println("Error in z3\n");
			return false;
		}

		System.out.println(result);
		if (result.contains("unsat")) {
			return true;
		} else {
			return false;
		}
	}
	
	protected void writeZ3Format() {
		variableTypeDeclare();
		
		logAll("Finshed variableDeclartion");
		int stage = 1;
		while (stage <= 2) {
			constructFormula(stage, 1);
			constructFormula(stage, 2);
			logAll("Finshed stage: " + stage);
			stage += 1;
		}
		//mPipeContent.add("(assert (not (= input0_1 input0_2)))\n");
		mPipeContent.add("(assert (not (= input0_1_r1 input0_2_r1)))\n");
		mPipeContent.add("(assert (= input0_1_r1 input0_2_r2))\n");
		mPipeContent.add("(assert (= input0_2_r1 input0_1_r2))\n");
		
		if (!mUsingNextBeforeLoop) {
			mPipeContent.add("(assert (= beforeLoop_1_r1 1))\n");
		}
		mPipeContent.add("(assert (= beforeLoop_1_r1 beforeLoop_1_r2))\n");
		mPipeContent.add("(assert (= beforeLoop_2_r1 1))\n");
		mPipeContent.add("(assert (= beforeLoop_2_r2 1))\n");

		StringBuffer finalAssertion = new StringBuffer("");
		boolean noVariable = true;
		for (String key : mVariables.keySet()) {
			if (key.contains("input")) continue;
			else if (key.contains("outK") || key.contains("outV")) {
				noVariable = false;
				if (finalAssertion.length() == 0) {
					finalAssertion.append("(or (not (= ").append(key).append("_1_r1 ")
					.append(key).append("_1_r2)) (not (= ").append(key).append("_2_r1 ").append(key).append("_2_r2)))\n"); 
				} else {
					finalAssertion.insert(0, "(or ").append("(or (not (= ").append(key).append("_1_r1 ")
					.append(key).append("_1_r2)) (not (= ").append(key).append("_2_r1 ").append(key).append("_2_r2))))\n"); 
				}
				continue;
			} else if (!mOutputRelated.get(key)) continue;

			noVariable = false;
			//if (mConditionRelated.get(key)) continue;
			if (finalAssertion.length() == 0) {
				finalAssertion.append("(not (= ").append(key).append("_2_r1 ").append(key).append("_2_r2))\n"); 
			} else {
				finalAssertion.insert(0, "(or ").append("(not (= ").append(key).append("_2_r1 ").append(key).append("_2_r2)))\n"); 
			}
		}
		if (noVariable) finalAssertion.append("(not (= 1 1))");
		mPipeContent.add(finalAssertion.insert(0, "(assert ").append(")\n").toString());

		mPipeContent.add("(check-sat)\n");

	}
	
	protected void constructFormula(int stage, int round) {
		// Generate formula for each variable in each round
		for (String key : mVariables.keySet()) {
			StringBuffer finalValue = new StringBuffer("");
			for (ExecutionTreeNode node : mBeforeNodes) {
				if (node.getLocalVars().get(key) == null) continue;
				StringBuffer value = combineValueCondition(node, key, stage, round);
				if (value.length() == 0) continue;
				
				if (finalValue.length() == 0) {
					finalValue.append(value);
				} else {
					finalValue.insert(0, "(or ").append(" ").append(value).append(")\n");
				}
			}
			for (ExecutionTreeNode node : mInnerNodes) {
				if (node.getLocalVars().get(key) == null) continue;
				StringBuffer value = combineValueCondition(node, key, stage, round);
				if (value.length() == 0) continue;

				if (finalValue.length() == 0) {
					finalValue.append(value);
				} else {
					finalValue.insert(0, "(or ").append(" ").append(value).append(")\n");
				}
			}
			if (finalValue.length() == 0) continue;
			
			//if (mOutputRelated.get(key)) {
				if (finalValue.lastIndexOf("hasNext") >= 0) {
					mOutputRelated.put(key, false);
					continue;
				}
				finalValue.insert(0, "(assert \n").append(")");
				mPipeContent.add(finalValue + "\n");
				//System.out.println(finalValue);
			//}
		}
		
	}
	
	protected StringBuffer combineValueCondition(ExecutionTreeNode node, String var, int stage, int round) {
		StringBuffer condition = generateConditions(node.getConstraint(), stage, round);
		String valueTokens[] = node.getLocalVars().get(var).split("\\s+");
		StringBuffer value = new StringBuffer("");
		for (String token : valueTokens) {
			if (token.contains("_v")) {
				token = token.replace("_v", "_" + (stage - 1) + "_r" + round);
			} else if (token.contains("input")) {
				token = token + "_" + stage + "_r" + round;
			}
			if (value.length() == 0) value.append(token);
			else value.append(" ").append(token);
		}
		if (value.length() == 0) return value;


		StringBuffer tmp = new StringBuffer("");
		tmp.append("(and ").append(condition).append(' ');
		tmp.append("(= ").append(var).append("_").append(stage).append("_r").append(round).append(' ').append(value).append(')');
		tmp.append(")\n");
		return tmp;
		//value = "(= " + var + "_" + stage + "_r" + round + " " + value + ")";
		//return "(and " + condition + " " + value + ")\n";
	}
	
	protected StringBuffer generateConditions(List<String> constraints, int stage, int round) {
		StringBuffer conditions = new StringBuffer("");
		for (String constraint : constraints) {
			String tokens[] = constraint.split("\\s+");
			String operation = "";
			StringBuffer lhs = new StringBuffer("");
			StringBuffer rhs = new StringBuffer("");
			StringBuffer condition = new StringBuffer("");
			boolean negative = false;
			if (tokens.length == 4 && tokens[0].equals("!")) {
				lhs.append(tokens[1]);
				operation = tokens[2];
				rhs.append(tokens[3]);
				negative = true;
			} else {
				lhs.append(tokens[0]);
				operation = tokens[1];
				rhs.append(tokens[2]);
			}
			
			if (lhs.toString().equals("beforeLoopDegree")) continue;
			
			for (String key : mVariables.keySet()) {
				if (lhs.toString().equals(key)) {
					lhs.append('_').append(stage).append("_r").append(round);
					break;
				}
			}
			for (String key : mVariables.keySet()) {
				if (rhs.toString().equals(key)) {
					rhs.append("_").append(stage).append("_r").append(round);
					break;
				}
			}

			if (operation.equals("!=")) {
				condition.append("(not (= ").append(lhs).append(" ").append(rhs).append("))");
			} else if (operation.equals("==")) {
				condition.append("(= ").append(lhs).append(" ").append(rhs).append(")");
			} else {
				condition.append("(").append(operation).append(" ").append(lhs).append(" ").append(rhs).append(")");
			}
			
			if (negative) {
				condition.insert(0, "(not ").append(")");
			}
			
			if (conditions.length() == 0) conditions = condition;
			else {
				conditions.insert(0, "(and ").append(condition).append(")");
			}
		}
		return conditions;
	}
	
	protected void variableTypeDeclare() {
		/*
		  initialize variable
		  initial version would be the same, but internal version(_1, _2) maybe not.
		  result formula wouldn't take variable with '$' into account.
		*/
		for(String variable : mTypeTable.keySet()) {
			String type = mTypeTable.get(variable);
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
			    log(variable + " " + Color.ANSI_RED + mTypeTable.get(variable) + " -> Int" + Color.ANSI_RESET);
				mVariables.put(var, false);
			} else {
				// Not supported in z3 Format
			    log(variable + " " + Color.ANSI_RED + mTypeTable.get(variable) + Color.ANSI_RESET);
			    continue;
			}
			int index = 0;
			while (index <= 2) {
				mPipeContent.add("(declare-const " + var + "_" + index + "_r1" + " " + type +")\n");
				mPipeContent.add("(declare-const " + var + "_" + index + "_r2" + " " + type +")\n");
				
			    index += 1;
			}
			if (!var.contains("input")) mPipeContent.add("(assert (= " + var + "_0_r1 " + var + "_0_r2" + "))\n");
		}
		mPipeContent.add("(declare-const null Int)\n");
		mPipeContent.add("(assert (= null 0))\n");
		
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
			mPipeContent.add("(declare-const " + element + " Int)\n");
		}
		
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
	
	protected void logAll(String str) {
		if (!mOption.silence_flag) System.out.println(str);
		else System.out.println("[ z3Pipe]  " + str);
	}
	
	protected void log(String str) {
		if (!mOption.silence_flag) System.out.println(str);
	}
		
	
} 