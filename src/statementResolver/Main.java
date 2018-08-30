package statementResolver;

import statementResolver.Option;
import statementResolver.soot.StatementResolver;

public class Main {
	public static void main(String[] args) {
		String javaInput = "";
		String classPath = "";
		String reducerClassname = "";
		Option op = new Option();
		
		// Parsing arguments
		if (args.length >= 1) {
			if (args.length > 1) {
				javaInput = args[0];
				reducerClassname = args[1];
				int i = 1;
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
			else if(args[0].equals("-h")) {
				System.out.println(op.Usage);
				return;
			}
		}
			
		if (javaInput.length() != 0 & reducerClassname.length() != 0) {
			StatementResolver SR = new StatementResolver();
			SR.run(javaInput, classPath, op, reducerClassname);
		} else {
			System.err.println(op.Warning);
		}
	}
}