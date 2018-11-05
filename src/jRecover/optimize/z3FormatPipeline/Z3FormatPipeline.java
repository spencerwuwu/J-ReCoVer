package jRecover.optimize.z3FormatPipeline;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jRecover.Option;
import jRecover.color.Color;
import jRecover.optimize.executionTree.ExecutionTreeNode;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Z3FormatPipeline {
	Map<String, String> mTypeTable;
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
	Boolean mNoLoop = false;
	

	public Z3FormatPipeline(Map<String, String> table, List<ExecutionTreeNode> beforeNodes, List<ExecutionTreeNode> interNodes, 
			boolean useNextFlag, Map<String, Boolean> outputRelated, Option op, Boolean noLoop) {
		mTypeTable = table;
		mBeforeNodes.addAll(beforeNodes);
		mInnerNodes.addAll(interNodes);
		mUsingNextBeforeLoop = useNextFlag;
		mOutputRelated = outputRelated;
		mOption = op;
		mNoLoop = noLoop;
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

		if (mOption.z3_mode) {
			for (String formula : mPipeContent) {
				System.out.print(formula);
			}
			return true;
		}
		try {
			Process process = Runtime.getRuntime().exec("z3 -in");
			mZt2pipe = process.getInputStream();
			mPipe2zt = process.getOutputStream();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
		
		for (String formula : mPipeContent) {
			//System.out.print(formula);
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

		//mPipeContent.add("(assert (not (= input0_0_r1 input0_1_r1)))\n");
		mPipeContent.add("(assert (= input0_0_r1 input0_1_r2))\n");
		mPipeContent.add("(assert (= input0_1_r1 input0_0_r2))\n");
		
		mPipeContent.add("(assert (= beforeLoop_0_r1 beforeLoop_0_r2))\n");
		mPipeContent.add("(assert (= beforeLoop_1_r1 beforeLoop_1_r2))\n");
		
		if (mNoLoop) {
			mPipeContent.add("(assert (= beforeLoop_0_r1 1))\n");
			mPipeContent.add("(assert (= beforeLoop_1_r1 1))\n");
		} else {
			if (!mUsingNextBeforeLoop) {
				mPipeContent.add("(assert (= beforeLoop_0_r1 0))\n");
			}
			mPipeContent.add("(assert (= beforeLoop_1_r1 0))\n");
		}

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
			if (key.contains("beforeLoop")) continue;
			StringBuffer finalValue = new StringBuffer("");
			StringBuffer beforeValue = new StringBuffer("");
			StringBuffer innerValue = new StringBuffer("");
			for (ExecutionTreeNode node : mBeforeNodes) {
				if (node.getLocalVars().get(key) == null) continue;
				StringBuffer value = combineValueCondition(node, key, stage, round);
				if (value.length() == 0) continue;
				
				if (beforeValue.length() == 0) {
					beforeValue.append(value);
				} else {
					beforeValue.insert(0, "(or ").append(" ").append(value).append(")\n");
				}
			}
			if (!mNoLoop) {
				for (ExecutionTreeNode node : mInnerNodes) {
					if (node.getLocalVars().get(key) == null) continue;
					StringBuffer value = combineValueCondition(node, key, stage, round);
					if (value.length() == 0) continue;

					if (innerValue.length() == 0) {
						innerValue.append(value);
					} else {
						innerValue.insert(0, "(or ").append(" ").append(value).append(")\n");
					}
				}
			}
			if (beforeValue.length() == 0 && innerValue.length() == 0) continue;
			else {
				if (beforeValue.length() == 0) beforeValue.append("(= beforeLoop_").append(stage - 1).append("_r").append(round).append(" 1)");
				if (innerValue.length() == 0) innerValue.append("(not (= beforeLoop_").append(stage - 1).append("_r").append(round).append(" 1))");
				finalValue.append("(or ").append(beforeValue).append(" ").append(innerValue).append(")\n");
			}
			
			//if (mOutputRelated.get(key)) {
				if (finalValue.lastIndexOf("hasNext") >= 0) {
					mOutputRelated.put(key, false);
					continue;
				}
				finalValue.insert(0, "(assert \n").append(")");
				mPipeContent.add(finalValue + "\n");
			//}
		}
		
	}
	
	protected StringBuffer combineValueCondition(ExecutionTreeNode node, String var, int stage, int round) {
		StringBuffer condition = generateConditions(node.getConditions(), stage - 1, round);
		StringBuffer variable = node.getLocalVars().get(var).getFormula(stage - 1, round);

		StringBuffer tmp = new StringBuffer("");
		tmp.append("(and ").append(condition).append(' ');
		tmp.append("(= ").append(var).append("_").append(stage).append("_r").append(round).append(' ').append(variable).append(')');
		tmp.append(")\n");
		return tmp;
		//value = "(= " + var + "_" + stage + "_r" + round + " " + value + ")";
		//return "(and " + condition + " " + value + ")\n";
	}
	
	/*
	protected StringBuffer generateConditions(List<Condition> cList, int stage, int round) {
		StringBuffer conditions = new StringBuffer("");
		for (Condition condition : cList) {
			if (conditions.length() == 0) {
				conditions.append(condition.getFormula(stage, round));
			} else {
				conditions.insert(0, "(and ").append(condition.getFormula(stage, round)).append(")");
			}
		}
		return conditions;
	}
	*/
	protected StringBuffer generateConditions(List<String> cList, int stage, int round) {
		StringBuffer conditions = new StringBuffer("");
		for (String condition : cList) {
			if (conditions.length() == 0) {
				conditions.append(condition.replace("_v", ("_" + stage + "_r" +  round)));
			} else {
				conditions.insert(0, "(and ").append(condition.replace("_v", ("_" + stage + "_r" +  round))).append(")");
			}
		}
		return conditions;
	}
	
	protected void variableTypeDeclare() {
		/*
		  initialize variable
		*/
		for (String variable : mTypeTable.keySet()) {
			String type = mTypeTable.get(variable);
			String var = variable.replace("_v", "");

			if (type == "int" || type == "byte" || type == "short" || type == "long") {
				type = "Int";
				mVariables.put(var, false);
			} else if (type == "input type") {
				type = "Real";
				mVariables.put(var, false);
			} else if (type == "double" | type == "float" ) {
				type = "Real";
				mVariables.put(var, false);
			} else if (type == "boolean") {
				type = "Int";
				mVariables.put(var, false);
			} else if (type.contains("beforeLoop")) {
				type = "Int";
				mVariables.put(var, false);
			} else if (type.contains("Object")){
				type = "Real";
				mVariables.put(var, false);
			} else if (type.contains("IntWritable") || type.contains("LongWritable")) {
				type = "Int";
			    log(variable + " " + Color.ANSI_RED + mTypeTable.get(variable) + " -> Int" + Color.ANSI_RESET);
				mVariables.put(var, false);
			} else if (type.contains("DoubleWritable") || type.contains("FloatWritable")) {
				type = "Real";
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
		
	}

	
	
	protected void logAll(String str) {
		if (mOption.z3_mode) return;
		if (!mOption.silence_flag) System.out.println(str);
		else System.out.println("[ z3Pipe]  " + str);
	}
	
	protected void log(String str) {
		if (mOption.z3_mode) return;
		if (!mOption.silence_flag) System.out.println(str);
	}
		
	
} 
