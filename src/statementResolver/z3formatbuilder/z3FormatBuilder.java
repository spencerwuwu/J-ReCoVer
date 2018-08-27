package statementResolver.z3formatbuilder;

import statementResolver.tree.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class z3FormatBuilder {
	Map<String, String> typeTable;
	List<Tree> finalStateList;
	boolean usingNextBeforeLoop = false;
	File file;
	PrintWriter output;
	
	public z3FormatBuilder() {
		typeTable = new HashMap<String, String>();
		finalStateList = new ArrayList<Tree>();
		file = new File("z3Format");
		
		try {
		    file.createNewFile();
		    System.out.println("Create file successfully");
		    System.out.println(file.getAbsolutePath());
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		try {
		    output = new PrintWriter(file);
		}
		catch(FileNotFoundException f){
			f.printStackTrace();
		}
	}
	
	public z3FormatBuilder(Map<String, String>table, List<Tree>tree, String filename, boolean useNextFlag) {
		typeTable = table;
		finalStateList = tree;
		usingNextBeforeLoop = useNextFlag;
		file = new File(filename);
		System.out.print("Using getNext before loop: ");
		System.out.print(usingNextBeforeLoop);
		System.out.print("\n");
		
		try {
		    file.createNewFile();
		    System.out.println("Create file successfully");
		    System.out.println(file.getAbsolutePath());
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		try {
		    output = new PrintWriter(file);
		}
		catch(FileNotFoundException f){
			f.printStackTrace();
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
			if(typeTable.get(s) == "int" | typeTable.get(s) == "byte" | typeTable.get(s) == "short" | typeTable.get(s) == "long") {
			    output.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			    
			    //if(s.contains("$")) {
			    //output.append("(declare-const "+s+"_0com0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1com0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2com0"+" Int"+")"+"\n");
			    //declareVars.put(s+"_0com0", "Int");
			    declareVars.put(s+"_1com0", "Int");
			    declareVars.put(s+"_2com0", "Int");
			    //}
			    
			    
			    
			    
			    
			}
			else if (typeTable.get(s) == "double" | typeTable.get(s) == "float" ) {
				output.append("(declare-const "+s+"_0"+" Real"+")"+"\n");
			    output.append("(declare-const "+s+"_1"+" Real"+")"+"\n");
			    output.append("(declare-const "+s+"_2"+" Real"+")"+"\n");
			    declareVars.put(s+"_0", "Real");
			    declareVars.put(s+"_1", "Real");
			    declareVars.put(s+"_2", "Real");
			    
			    //if(s.contains("$")) {
			    //output.append("(declare-const "+s+"_0com0"+" Real"+")"+"\n");
			    output.append("(declare-const "+s+"_1com0"+" Real"+")"+"\n");
			    output.append("(declare-const "+s+"_2com0"+" Real"+")"+"\n");
			    //declareVars.put(s+"_0com0", "Real");
			    declareVars.put(s+"_1com0", "Real");
			    declareVars.put(s+"_2com0", "Real");
			    //}
			    
				
			}
			else if (typeTable.get(s) == "boolean" ) {
				//use Int in Z3 would be easier
				output.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Bool");
			    declareVars.put(s+"_1", "Bool");
			    declareVars.put(s+"_2", "Bool");
			    
			    //if(s.contains("$")) {
			    //output.append("(declare-const "+s+"_0com0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1com0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2com0"+" Int"+")"+"\n");
			    //declareVars.put(s+"_0com0", "Bool");
			    declareVars.put(s+"_1com0", "Bool");
			    declareVars.put(s+"_2com0", "Bool");
			    //}
			    
			    
			}
			else if (typeTable.get(s) == "input type") {
				output.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			    
			}
			else if (typeTable.get(s) == "before loop flag") {
				output.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			}
			
			
			else if (typeTable.get(s) == "") {
				//deal with output
				output.append("(declare-const "+s+"_0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2"+" Int"+")"+"\n");
			    
			    
			    output.append("(declare-const "+s+"_0com0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_1com0"+" Int"+")"+"\n");
			    output.append("(declare-const "+s+"_2com0"+" Int"+")"+"\n");
			    
			    
			    declareVars.put(s+"_0", "Int");
			    declareVars.put(s+"_1", "Int");
			    declareVars.put(s+"_2", "Int");
			    
			    declareVars.put(s+"_0com0", "Int");
			    declareVars.put(s+"_1com0", "Int");
			    declareVars.put(s+"_2com0", "Int");
			    
			    
			}
			else {
				System.out.println("Some datatypes aren't supported now.");
			}
		}
		output.flush();
		
		String finalEquation = "";
		
		for(int i = 0; i < 2; i++) {
			
			String oneIter = "";
			
			for(Tree result:finalStateList) {

				//deal with constraints
				String constraintStr = "";
				for(String str:result.root.get_constraint()) {
					boolean addAND = false;
					
					if(str.charAt(0) == '!') {
						
						if(!constraintStr.equals("") ) {
							addAND = true;
							constraintStr = "(and "+constraintStr+" ";
						}

					    String[] strArray = str.split(" ");
					    //deal with variable version
					    /*
					    if(result.root.get_local_vars().keySet().contains(strArray[3]) ) {
					    	strArray[3] = strArray[3]+"_"+String.valueOf(i);
					    }*/
					    
					    if(strArray[1].contains("_v")) {strArray[1] = strArray[1].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[3].contains("_v")) {strArray[3] = strArray[3].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[1].equals("input0")) {strArray[1] = strArray[1].replace("input0", "input0_"+String.valueOf(i));}
					    if(strArray[3].equals("input0")) {strArray[3] = strArray[3].replace("input0", "input0_"+String.valueOf(i));}
					    if(!strArray[1].contains("_") && result.root.get_local_vars().containsKey(strArray[1])) {
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
					    if(result.root.get_local_vars().keySet().contains(strArray[2]) ) {
					    	strArray[2] = strArray[2]+"_"+String.valueOf(i);
					    }*/
					    
					    if(strArray[0].contains("_v")) {strArray[0] = strArray[0].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[2].contains("_v")) {strArray[2] = strArray[2].replace("_v", "_"+String.valueOf(i));}
					    if(strArray[0].equals("input0")) {strArray[0] = strArray[0].replace("input0", "input0_"+String.valueOf(i));}
					    if(strArray[2].equals("input0")) {strArray[2] = strArray[2].replace("input0", "input0_"+String.valueOf(i));}
					    if(!strArray[0].contains("_") && result.root.get_local_vars().containsKey(strArray[0])) {
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
					if(addAND == true) {
						constraintStr+=")";
					}
				}
				
				//deal with result
				
				String varStr = "";
				for(String var:result.root.get_local_vars().keySet()) {
					
					if(result.root.get_local_vars().get(var).contains("@parameter") | 
					   result.root.get_local_vars().get(var).equals("") |
					   var.contains("$") ) {continue;}
					
					boolean addAND = false;
					
					if(declareVars.containsKey(var+"_"+String.valueOf(i+1))) {
						if(!varStr.equals("") ) {
							addAND = true;
							varStr = "(and "+varStr+" ";
						}
						
						String next = result.root.get_local_vars().get(var);
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
		
	    
	    
	    
	    output.append("(assert "+finalEquation+")\n");
	    output.append("(assert "+compareEquation+")\n");
	    
	    if(!usingNextBeforeLoop) {
	    	output.append("(assert (= beforeLoop_0 1))\n");
	    }
	   
        output.append("(assert (not (= output_2 output_2com0)) )\n");

		output.append("(check-sat)\n");
		output.flush();
		
	}
	
} 