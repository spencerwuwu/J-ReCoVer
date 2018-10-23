// https://searchcode.com/api/result/62663675/

/*******************************************************************************
 * FILENAME:    TypeCheck.java
 * DESCRIPTION: Type Checking Parser
 * AUTHOR:      James Matthew Welch [JMW]
 * SCHOOL:      Arizona State University
 * CLASS:       CSE340: Principles of Programming Languages
 * INSTRUCTOR:  Dr. Toni Farley
 * SECTION:     27199
 * TERM:        Spring 2012
 ******************************************************************************/
/* :::Program Description:::
 * This project will implement a type-checking algorithm based on a provided 
 * language description. The Type Checker will read in a program stored in a 
 * text file, and output the type-checking results. It will use the ideas from 
 * Hindley-Milner type checking to infer types of expressions, and determine 
 * the type correctness of the program. The procedures will process expressions
 * using the correct order of operations, and keep track of intermediate types
 * during processing. This program is written in Java using jre7. */

import java.util.*;// Scanner, Map, Vector
import java.io.*;

public class TypeCheck {

	// shared/global data structures
	private static Map<String, String> symbolTable = new HashMap<String, String>();
	private static Map<String, Integer> arraySizesMap = new HashMap<String, Integer>();
	private static boolean debugMode = false;
	private static HashSet<String> validTypes = new HashSet<String>();
	
	public static void main(String[] args) {
		// read the program input file in from the command line
		String filename;
		if(args.length > 0){
			filename = args[0];
		}else{
			filename = "input.txt";
		}
		
		// read in all of the lines from the input "source code" file into a vector of strings
		// do this first for program stability - read first then parse
		// Inefficient, but try/catch is smaller
		Vector<String> inputFileLines = new Vector<String>();
		String strLine;
		try{
			if(debugMode)
				System.out.printf("Reading in a file \"%s\"...\n", filename);

			FileInputStream inFile = new FileInputStream(filename);
			DataInputStream inData = new DataInputStream(inFile);
			BufferedReader inBuffer = new BufferedReader(new InputStreamReader(inData));

			while (  ( strLine = inBuffer.readLine() ) != null  ) {
				if (!strLine.isEmpty()) {// skip lines that are empty
					inputFileLines.add(strLine);
				}
			}
			if(debugMode)
				System.out.printf("File read of \"%s\" successful!\n", filename);

		}catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	
		// make a set of the valid type names to check scanned tokens against
		validTypes.add("bool");
		validTypes.add("int");
		validTypes.add("double");
		validTypes.add("string");
		validTypes.add("boolarr");
		validTypes.add("intarr");
		validTypes.add("doublearr");
		validTypes.add("stringarr");
			
		String token, strDataType, identifier;
		// parse through each line in the input file
		int lineCounter = 0, arrayIndex;
		StringTokenizer tokenedString;
		int LHSArraySize = 0;
		while(!inputFileLines.isEmpty()){
			if(debugMode) System.out.println("Line(" + (lineCounter+1) + "): \"" + inputFileLines.firstElement() + "\"");
			
			strLine = inputFileLines.firstElement();
			tokenedString = new StringTokenizer(strLine);
			arrayIndex = 0;
			LHSArraySize  = 0;
			
			// get the first token in the string.  
			token = tokenedString.nextToken();
			
			// check if it's an array
			if(token.contains("[")){
				// this is an array, strip brackets & parse array index
				arrayIndex = findArrayIndex(token);
				token = stripArrayBrackets(token);
			}
			
			// check if a type or not.  if so, parse as declaration, if not parse as definition
			if(validTypes.contains(token)){// strDataType is a valid data type
				strDataType = token;
				if(arrayIndex > 0)
					strDataType = strDataType + "arr";
				if(debugMode) System.out.println("\tTYPE:\"" + strDataType + "\"");
				// ensure more tokens on line
				if (tokenedString.hasMoreTokens()) {
					// add declaration to map, if array, add size to array-map
					identifier = tokenedString.nextToken();
					if(debugMode) System.out.println("\tDECLARATION:: \"" + identifier + "\""); 
					
					// check the token against regex to verify it's valid
					if(isValidIdentifier(identifier)){
						// place token in map with appropriate token type
						symbolTable.put(identifier, strDataType);
						if(debugMode) System.out.println("\t\tAdded to identifier map as \""+ strDataType + "\" type");
						
						//add to array map if array
						if(arrayIndex > 0){
							arraySizesMap.put(identifier, arrayIndex);
							if(debugMode) System.out.println("\t\tAdded to arraySizes map with size [" + arrayIndex + "]");
						}
					}
				}
			}else{// line doesn't lead with a type declaration, so it must be a definition/expression 
				
				// reassign first token in definition as LHS of expression
				String LHSToken = token;
				if(debugMode) System.out.println("\tEXPRESSION:: \"" + LHSToken + "\""); 
				
				// retrieve the type string from the map
				String LHSType = symbolTable.get(LHSToken);
				if(LHSType.contains("arr")){
					// this is whole-array math e.g. a = a + b, where they're whole arrays
					if(arrayIndex == 0){
						// array type, look up size
						LHSArraySize = arraySizesMap.get(LHSToken); 
						LHSType = LHSType + LHSArraySize;
					}else{
						// this is an array reference (e.g. a[1])
						LHSType = LHSType.substring(0,LHSType.indexOf("arr"));
					}
				}
				
				// check for equals, this is not required by the assignment, but it's a good idea anyway
				token = tokenedString.nextToken();
				if(!token.equals("=") ){// make sure "=" is present
					PrintError(4,"No \"=\" sign present after assignment variable");
					// increment counter to next line
					lineCounter++;
					// remove the line from the vector of lines
					inputFileLines.remove(0);
					continue;
				}
				
				/////parse RHS of the assignment/////
				// 	convert tokened string to vector
				Vector<String> RHS = convertTokenedStringToVector(tokenedString);
				
				// reduce the expression on the RHS to a single type
				String RHSType = getRHSType(RHS);
								
				// check LHS DataType against RHS DataType
				if(LHSType.equals(RHSType)){
					// no errors, typical situation
					if(LHSType.contains("arr")){
						int index = LHSType.indexOf("arr");
						LHSType = LHSType.substring(0, index);
					}
					System.out.println(LHSType);
				}else if(RHSType.equals("Error1") || RHSType.equals("Error2")){
					// do nothing since error was caught in getRHSType()
				}else if(LHSType.equals("double") && RHSType.equals("int") ){
					// widening conversion on assignment, type is double
					System.out.println(LHSType);
				}else if(LHSType.contains("doublearr") && RHSType.contains("intarr")){
					// check for double array and int array
					int index = LHSType.indexOf("arr");
					LHSType = LHSType.substring(0, index);
					System.out.println(LHSType);
					/* KNOWN BUG: if array types are compared, should produce an 
					 * array type, but the assignment did not describe the output 
					 * for this case so just printing the simple-type of the array */ 
				}else{
					PrintError(3, LHSToken);
				}
			}
			// increment counter to next line
			lineCounter++;
			// remove the line from the vector of lines
			inputFileLines.remove(0);
		}
		// end of program
	}

	private static String getRHSType(Vector<String> RHS){
		String retVal = "double";
		// reduce arrays to types 
		RHS = replaceArrayRefs(RHS);
		if(debugMode) System.out.println("RHS::ArrayRefs replaced: " + RHS.toString());

		// reduce remaining identifiers to types
		RHS = replaceIdentifiers(RHS);
		if(debugMode) System.out.println("RHS::Identifiers replaced: " + RHS.toString());

		// reduce mult/div
		RHS = reduceMultDiv(RHS);
		if(debugMode) System.out.println("RHS::Mult/Div reduced: " + RHS.toString());
		
		// reduce add/sub
		RHS = reduceAddSub(RHS);
		if(debugMode) System.out.println("RHS::Add+Sub reduced: " + RHS.toString());

		// reduce <,>,=,>=,<=
		RHS = reduceEquality(RHS);
		if(debugMode) System.out.println("RHS::Equality reduced: " + RHS.toString());

		if(RHS.size() == 1){
			retVal = RHS.get(0);
		}
		return retVal;
	}

	// step 1, replace array references with their types
	private static Vector<String> replaceArrayRefs(Vector<String> RHS){
		String token, tokenOrig;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			tokenOrig = token;
			
			// check if it's an array
			if(token.contains("[")){
				// this is an array reference, strip brackets & parse array index
				token = stripArrayBrackets(token);
				if(isValidIdentifier(token)){
					if(symbolTable.containsKey( token ) ){
						RHS.remove(i);
						String dataType = symbolTable.get(token);
						if( dataType.contains("arr") ){
							dataType = dataType.substring(0,dataType.indexOf("arr"));
						}
						if(debugMode) System.out.println("\tTOKEN:\"" + tokenOrig + "\" replaced with \""+ dataType +"\"");
						RHS.insertElementAt(dataType, i);
					}else{
						PrintError(1, tokenOrig);
						RHS.clear();
						RHS.add("Error1");
					}
				}
			}
		}
		return RHS;
	}
	
	// step 2, replace identifiers with their types
	private static Vector<String> replaceIdentifiers(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(!validTypes.contains(token)){// ensure not already been replaced as arrayRef
				if(isValidIdentifier(token)){
					if(symbolTable.containsKey( token ) ){
						RHS.remove(i);
						String dataType = symbolTable.get(token);
						if(dataType.contains("arr"))
							dataType = dataType + arraySizesMap.get(token);
						if(debugMode) System.out.println("\tTOKEN:\"" + token + "\" replaced with \""+ dataType +"\"");
						RHS.insertElementAt(dataType, i);
					}else{
						PrintError(1, token);
						RHS.clear();
						RHS.add("Error1");
					}
				}
			}
		}
		return RHS;
	}

	// step 3: replace multiplicatoin.division with their resulting types
	private static Vector<String> reduceMultDiv(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(isMultSymbol(token)){
				String Lfactor = RHS.elementAt(i-1);
				String Rfactor = RHS.elementAt(i+1);
				
				if(Lfactor.equals(Rfactor)){
					token = Lfactor;
					// replace all three with type
				}else if( (Lfactor.equals("double") && Rfactor.equals("int")) || 
						(Lfactor.equals("int") && Rfactor.equals("double")) )
				{	// double-int math is always widening
						token = "double";
				}else{
					// error on expression type (2), clear and return error
					PrintError(2, Lfactor + ", " + Rfactor);
					RHS.clear();
					RHS.add("Error2");
					return RHS;
				}
				RHS.setElementAt(token, i-1);
				RHS.remove(i+1);
				RHS.remove(i);
			}
		}
		return RHS;
	}
	
	// step 4: replace addition.subtration with their resulting types
	private static Vector<String> reduceAddSub(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(isAddSymbol(token)){
				String augend = RHS.elementAt(i-1);
				String addend = RHS.elementAt(i+1);
				
				if(augend.equals(addend)){
					token = augend;
					// replace all three with type
				}else if(   (augend.equals("double") && addend.equals("int") )||
						(augend.equals("int")&& addend.equals("double") ) ){
					// double-int math, always widening
					token = "double";
				}else{
					// error on expression type (2), clear and return error
					PrintError(2, augend + ", " + addend);
					RHS.clear();
					RHS.add("Error2");
					return RHS;
				}
				RHS.setElementAt(token, i-1);
				RHS.remove(i+1);
				RHS.remove(i);
			}
		}
		return RHS;
	}
	
	// step 5: replace equality symbols with their types
	private static Vector<String> reduceEquality(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(isEqualitySymbol(token)){
				String LHSType = RHS.elementAt(i-1);
				String RHSType = RHS.elementAt(i+1);
				
				if(LHSType.equals(RHSType)){
					token = "bool";
					// replace all three with type
				}else if(LHSType.equals("double") && RHSType.equals("int") ||
						LHSType.equals("int") && RHSType.equals("double")){
					// double-int compare, always widening
					token = "bool";
				}else{
					// error condition, clear and return error
					PrintError(2, LHSType + ", " + RHSType);
					RHS.clear();
					RHS.add("Error2");
					return RHS;
				}
				RHS.setElementAt(token, i-1);
				RHS.remove(i+1);
				RHS.remove(i);
			}
		}
		return RHS;
	}
	
	private static boolean isEqualitySymbol(String token){
		boolean retVal = false;
		if(token.matches("==|>=|>|<=|<"))
			retVal = true;
		return retVal;
	}
	
	private static boolean isAddSymbol(String token){
		boolean retVal = false;
		if(token.equals("+") || token.equals("-"))
			retVal = true;
		return retVal;
	}

	private static boolean isMultSymbol(String token){
		boolean retVal = false;
		if(token.equals("*") || token.equals("/"))
			retVal = true;
		return retVal;
	}
	
	private static boolean isValidIdentifier(String token){
		boolean retVal = false;
		if(token.matches("[a-zA-Z]+"))
			retVal = true;
		return retVal;
	}
	
	private static String stripArrayBrackets(String token){
		// strip token of brackets
		token = token.substring(0, token.indexOf("["));
		return token;
	}
	
	private static void PrintError(int errorNum, String message){
		if(debugMode){message = ("(\"" + message + "\")");}
		else message = "";
		
		switch (errorNum){
		case 1: 
			System.out.println("ERROR 1: Undeclared identifier in expression" + message);
			break;
		case 2: 
			System.out.println("ERROR 2: Type mismatch in expression" + message);
			break;
		case 3: 
			System.out.println("ERROR 3: Type mismatch in assignment" + message);
			break;
		default: 
			System.out.println("ERROR 4: General formatting error" + message);
		}
	}
	
	private static int findArrayIndex(String token){
		int LBindex = token.indexOf("[");
		int RBindex = token.indexOf("]");
		int retVal = 0;
		try{
			// the String to int conversion happens here
			retVal = Integer.parseInt(token.substring(LBindex+1, RBindex));
			// print out the value after the conversion
			if(debugMode) System.out.println("\tarray index = " + retVal);

		} catch (NumberFormatException nfe)
		{
			System.out.println("NumberFormatException: " + nfe.getMessage());
		}
		return retVal;
	}
	
	private static Vector<String> convertTokenedStringToVector(StringTokenizer tokenedString){
		Vector<String> RHS = new Vector<String>();
		while(tokenedString.hasMoreTokens()){
			/* build RHS putting tokens into a vector to send to 
			 * OrderOfOperations parsing functions */
			RHS.add(tokenedString.nextToken());
		}
		return RHS;
	}
}


