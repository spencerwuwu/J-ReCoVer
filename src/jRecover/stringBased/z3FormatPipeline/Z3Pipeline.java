package jRecover.stringBased.z3FormatPipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jRecover.Option;
import jRecover.stringBased.executionTree.ExecutionTreeNode;

public class Z3Pipeline {
	Map<String, String> mTypeTable;
	private List<StringBuffer> mConditions;
	PipedInputStream mStr2pipe = new PipedInputStream();
	OutputStream mPipe2zt = null;
	InputStream mZt2pipe = null;
	PipedOutputStream mOutput2str = new PipedOutputStream();
	List<String> mPipeContent = new LinkedList<String>();
	Option mOption;
	
	public Z3Pipeline (ExecutionTreeNode node, Map<String, String> typeTable, Option option) {
		mTypeTable = typeTable;
		mConditions = node.getConditions();
		mOption = option;
	}
	
	
	public boolean getResult() throws IOException {
		log("Checking condition satisfiability...");
		variableTypeDeclare();
		for (StringBuffer condition : mConditions) {
			mPipeContent.add("(assert " + condition + " )\n");
		}
		mPipeContent.add("(check-sat)\n");

		String result = "";
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

		if (result.contains("unsat")) {
			return false;
		} else {
			return true;
		}
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
			} else if (type == "input type") {
				type = "Real";
			} else if (type == "double" | type == "float" ) {
				type = "Real";
			} else if (type == "boolean") {
				type = "Int";
			} else if (type.contains("beforeLoop")) {
				type = "Int";
			} else if (type.contains("Object")){
				type = "Real";
			} else if (type.contains("IntWritable") || type.contains("LongWritable")) {
				type = "Int";
			} else if (type.contains("DoubleWritable") || type.contains("FloatWritable")) {
				type = "Real";
			} else {
				// Not supported in z3 Format
			    continue;
			}
			mPipeContent.add("(declare-const " + var + "_v" + " " + type +")\n");
		}
		mPipeContent.add("(declare-const null Int)\n");
		mPipeContent.add("(assert (= null 0))\n");
		
	}
	protected void constructFormula() {

	}

	protected void logAll(String str) {
		if (!mOption.silence_flag) System.out.println(str);
		else System.out.println("[ z3Pipe]  " + str);
	}
	
	protected void log(String str) {
		if (!mOption.silence_flag) System.out.println(str);
	}
}
