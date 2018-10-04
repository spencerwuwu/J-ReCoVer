package jRecover;

import jRecover.Option;
import jRecover.soot.StatementResolver;

public class Main {
	public static void main(String[] args) {
		String javaInput = "";
		String classPath = "";
		String reducerClassname = "";
		Option op = new Option();
		String z3FileName = "z3_.txt";
		
		// Parsing arguments
		if (args.length >= 2) {
			javaInput = args[0];
			reducerClassname = args[1];
			if (args.length > 2) {
				int i = 2;
				while (i < args.length) {
					if (args[i].equals("-c")) {
						i++;
						if (i < args.length) {
							classPath = args[i];
							i++;
						}
						else {
							System.err.println(op.Warning);
							return;
						}
					} else {
						z3FileName = op.parse(args[i]);
						i++;
					}
				}
			}
			else if(args[0].equals("-h")) {
				System.out.println(op.Usage);
				return;
			}
		}

		if (javaInput.length() != 0 & reducerClassname.length() != 0) {
			StatementResolver SR = new StatementResolver();
			SR.run(javaInput, classPath, op, reducerClassname, z3FileName);
		} else {
			System.err.println(op.Warning);
		}
	}
}