package statementResolver.z3formatbuilder;

import statementResolver.color.Color;
import statementResolver.executionTree.ExecutionTreeNode;
import statementResolver.tree.*;
import java.util.HashMap;
import java.util.Map;
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
	List<ExecutionTreeNode> mFinalStates = new ArrayList<ExecutionTreeNode>();
	boolean usingNextBeforeLoop = false;
	File mFile;
	PrintWriter mOutput;
	
	public z3FormatBuilder() {
		typeTable = new HashMap<String, String>();
		mFile = new File("z3Format");
		
		try {
		    mFile.createNewFile();
		    System.out.println("Create file successfully");
		    System.out.println(mFile.getAbsolutePath());
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		try {
		    mOutput = new PrintWriter(mFile);
		}
		catch(FileNotFoundException f){
			f.printStackTrace();
		}
	}

	public z3FormatBuilder(Map<String, String> table, List<ExecutionTreeNode> beforeNodes, List<ExecutionTreeNode> interNodes, 
			String filename, boolean useNextFlag) {
		typeTable = table;
		mFinalStates.addAll(beforeNodes);
		mFinalStates.addAll(interNodes);
		usingNextBeforeLoop = useNextFlag;
		mFile = new File(filename);
		System.out.print("Using getNext before loop: ");
		System.out.print(usingNextBeforeLoop);
		System.out.print("\n");
		
		try {
		    mFile.createNewFile();
		    System.out.println("Create file successfully");
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		try {
		    mOutput = new PrintWriter(mFile);
		}
		catch(FileNotFoundException f){
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

		if (result.contains("unsat")) {
			return true;
		} else {
			return false;
		}

	}
	
	public void writeZ3Format() {
		Map<String, String>declareVars = new HashMap<String, String>();
		/*
		  initialize variable
		  initial version would be the same, but internal version(_1, _2) maybe not.
		  result formula wouldn't take variable with '$' into account.
		*/
		for(String s:typeTable.keySet()) {
			String type = typeTable.get(s);
			if (type == "int" | type == "byte" | type == "short" | type == "long") {
			    mOutput.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			    
			    //if(s.contains("$")) {
			    //mOutput.append("(declare-const "+s+"_0com0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1com0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2com0"+" Int"+")"+"\n");
			    //declareVars.put(s+"_0com0", "Int");
			    declareVars.put(s+"_1com0", "Int");
			    declareVars.put(s+"_2com0", "Int");
			    //}
			    
			}
			else if (type == "double" | type == "float" ) {
				mOutput.append("(declare-const "+s+"_0"+" Real"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Real"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Real"+")"+"\n");
			    declareVars.put(s+"_0", "Real");
			    declareVars.put(s+"_1", "Real");
			    declareVars.put(s+"_2", "Real");
			    
			    //if(s.contains("$")) {
			    //mOutput.append("(declare-const "+s+"_0com0"+" Real"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1com0"+" Real"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2com0"+" Real"+")"+"\n");
			    //declareVars.put(s+"_0com0", "Real");
			    declareVars.put(s+"_1com0", "Real");
			    declareVars.put(s+"_2com0", "Real");
			    //}
			    
				
			}
			else if (type == "boolean" ) {
				//use Int in Z3 would be easier
				mOutput.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Bool");
			    declareVars.put(s+"_1", "Bool");
			    declareVars.put(s+"_2", "Bool");
			    
			    //if(s.contains("$")) {
			    //mOutput.append("(declare-const "+s+"_0com0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1com0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2com0"+" Int"+")"+"\n");
			    //declareVars.put(s+"_0com0", "Bool");
			    declareVars.put(s+"_1com0", "Bool");
			    declareVars.put(s+"_2com0", "Bool");
			    //}
			    
			    
			}
			else if (type == "input type") {
				mOutput.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			    
			}
			else if (type == "before loop flag") {
				mOutput.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			}
			else if (type.contains("bld")) {
				mOutput.append("(declare-const "+s.replace("_v", "")+"_0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s.replace("_v", "")+"_1"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s.replace("_v", "")+"_2"+" Int"+")"+"\n");
			    declareVars.put(s.replace("_v", "")+"_0", "Int");
			    declareVars.put(s.replace("_v", "")+"_1", "Int");
			    declareVars.put(s.replace("_v", "")+"_2", "Int");
			}
			else if (type == "") {
				//deal with output
				mOutput.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    
			    mOutput.append("(declare-const "+s+"_0com0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1com0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2com0"+" Int"+")"+"\n");
			    
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			    
			    declareVars.put(s+"_0com0", "Int");
			    declareVars.put(s+"_1com0", "Int");
			    declareVars.put(s+"_2com0", "Int");
			    
			}
			else if (type.contains("Object")){
				mOutput.append("(declare-const "+s+"_0"+" Real"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Real"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Real"+")"+"\n");
			    declareVars.put(s+"_0", "Real");
			    declareVars.put(s+"_1", "Real");
			    declareVars.put(s+"_2", "Real");
			    System.out.println(s + " " + Color.ANSI_RED + typeTable.get(s) + " -> REAL" + Color.ANSI_RESET);
			}
			else if (type.contains("IntWritable") || type.contains("LongWritable")) {
				mOutput.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    mOutput.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			    System.out.println(s + " " + Color.ANSI_RED + typeTable.get(s) + " -> Int" + Color.ANSI_RESET);
			}
			else {
				// Not supported in z3 Format
			    System.out.println(s + " " + Color.ANSI_RED + typeTable.get(s) + Color.ANSI_RESET);
			}
		}
		mOutput.flush();
		
		String finalEquation = "";
		
		for(int i = 0; i < 2; i++) {
			String oneIter = "";
			for(ExecutionTreeNode node: mFinalStates) {

				//deal with constraints
				String constraintStr = "";
				for(String str : node.getConstraint()) {
					boolean addAND = false;
					
					if(str.charAt(0) == '!') {
						
						if(!constraintStr.equals("") ) {
							addAND = true;
							constraintStr = "(and "+constraintStr+" ";
						}

					    String[] strArray = str.split(" ");
					    //deal with variable version
					    /*
					    if(result.mRoot.getLocalVars().keySet().contains(strArray[3]) ) {
					    	strArray[3] = strArray[3]+"_"+String.valueOf(i);
					    }*/
					    
					    if(strArray[1].contains("_v")) {strArray[1] = strArray[1].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[3].contains("_v")) {strArray[3] = strArray[3].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[1].equals("input0")) {strArray[1] = strArray[1].replace("input0", "input0_"+String.valueOf(i));}
					    if(strArray[3].equals("input0")) {strArray[3] = strArray[3].replace("input0", "input0_"+String.valueOf(i));}
					    if(!strArray[1].contains("_") && node.getLocalVars().containsKey(strArray[1])) {
					    	strArray[1] = strArray[1].concat("_"+String.valueOf(i));
					    }
					    
					    if(strArray[2].equals("!=") ) {
					        constraintStr += "(= "+strArray[1]+" "+strArray[3]+")";
					    }
					    else if(strArray[2].equals("==") ){
					    	constraintStr += "(not (="+" "+strArray[1]+" "+strArray[3]+"))";
					    }
					    else {
					    	constraintStr += "(not ("+strArray[2]+" "+strArray[1]+" "+strArray[3]+"))";
					    }
					    
					    
					}
					else {
						if(!constraintStr.equals("") ) {
							addAND = true;
							constraintStr = "(and "+constraintStr+" ";
						}

						String[] strArray = str.split(" ");

						//deal with variable version
						/*
					    if(result.mRoot.getLocalVars().keySet().contains(strArray[2]) ) {
					    	strArray[2] = strArray[2]+"_"+String.valueOf(i);
					    }*/
					    
					    if(strArray[0].contains("_v")) {strArray[0] = strArray[0].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[2].contains("_v")) {strArray[2] = strArray[2].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[0].equals("input0")) {strArray[0] = strArray[0].replace("input0", "input0_"+String.valueOf(i));}
					    if(strArray[2].equals("input0")) {strArray[2] = strArray[2].replace("input0", "input0_"+String.valueOf(i));}
					    if(!strArray[0].contains("_") && node.getLocalVars().containsKey(strArray[0])) {
					    	strArray[0] = strArray[0].concat("_"+String.valueOf(i));
					    }
                        
					    
						if(strArray[1].equals("!=") ) {
							constraintStr += "(not (= "+strArray[0]+" "+strArray[2]+"))";
						}
						else if(strArray[1].equals("==") ){
							
					    	constraintStr += "(="+" "+strArray[0]+" "+strArray[2]+")";
					    }
					    else {
					    	constraintStr += "("+strArray[1]+" "+strArray[0]+" "+strArray[2]+")";
					    }
					}
					if (addAND == true) {
						constraintStr+=")";
					}
				}
				
				//deal with result
				
				String varStr = "";
				for (String var : node.getLocalVars().keySet()) {
					
					if(node.getLocalVars().get(var).contains("@parameter")
					   | node.getLocalVars().get(var).equals("")
					   | var.contains("$") ) {continue;}
					
					boolean addAND = false;
					
					if(declareVars.containsKey(var+"_"+String.valueOf(i+1))) {
						if(!varStr.equals("") ) {
							addAND = true;
							varStr = "(and "+varStr+" ";
						}
						
						String next = node.getLocalVars().get(var);
						if( next.contains("_v") ) {
							next = next.replace("_v", "_"+String.valueOf(i));
						}
						//assume only use one input each iter
						if( next.contains("input0") ) {
							next = next.replace("input0", "input0_"+String.valueOf(i));//has problem
					    } 
						varStr +="(= "+var+"_"+String.valueOf(i+1)+" "+next+")";
					    if(addAND == true) {
						    varStr = varStr + ")";
					    }
			        }
				}
				if(oneIter.equals("")) {
					oneIter = "(and "+constraintStr+" "+varStr+")";
				}
				else {
					oneIter ="(or "+oneIter+" "+"(and "+constraintStr+" "+varStr+")"+")";
				}
			}
			if(finalEquation.equals("")) {
				finalEquation = new String(oneIter);
			}
			else {
			    finalEquation = "(and "+finalEquation+" "+oneIter+")";
			}
		
	    }
		
		String compareEquation = new String(finalEquation);
		compareEquation = compareEquation.replace("input0_0", "temp");
		compareEquation = compareEquation.replace("input0_1", "input0_0");
		compareEquation = compareEquation.replace("temp", "input0_1");
		
		
		for(String var:declareVars.keySet()) {
			if(! var.contains("input") && ! var.contains("beforeLoop") && declareVars.containsKey(var+"com0") ) {
				compareEquation = compareEquation.replace(var, var+"com0");
			}
		}
	    
	    
	    mOutput.append("(assert "+finalEquation+")\n");
	    mOutput.append("(assert "+compareEquation+")\n");
	    
	    if(!usingNextBeforeLoop) {
	    	mOutput.append("(assert (= beforeLoop_0 1))\n");
	    }
	   
        mOutput.append("(assert (not (= output_2 output_2com0)) )\n");

		mOutput.append("(check-sat)\n");
		mOutput.flush();
	}
		
	
} 