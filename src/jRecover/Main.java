package jRecover;

import jRecover.Option;
import jRecover.soot.OptimizeResolver;
import jRecover.soot.StatementResolver;
import jRecover.soot.StringBasedResolver;

public class Main {
	public static void main(String[] args) {
		String javaInput = "";
		String classPath = "";
		String reducerClassname = "";
		Option op = new Option();
		
		// Parsing arguments
		if(args[0].equals("-h")) {
			System.out.println(op.Usage);
			return;
		}
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
						op.parse(args[i]);
						i++;
					}
				}
			}
		}

		if (javaInput.length() != 0 & reducerClassname.length() != 0) {
			if (!op.optimize_flag) {
				StringBasedResolver SR = new StringBasedResolver();
				SR.run(javaInput, classPath, op, reducerClassname);
			} else {
				OptimizeResolver SR = new OptimizeResolver();
				SR.run(javaInput, classPath, op, reducerClassname);
			}
		} else {
			System.err.println(op.Warning);
		}
	}
}