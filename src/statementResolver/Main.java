
package statementResolver;


import statementResolver.Option;
import statementResolver.soot.StatementResolver;

public class Main {

	public static void main(String[] args) {
		
		String javaInput = "";
		String classPath = "";
		Option op = new Option();
		
		if (args.length > 0) {
			if (args[0].equals("-h")) {
				System.out.println(op.Usage);
			}
			else {
				javaInput = args[0];
				
				if (args.length > 1) {
					int i = 1;
					while (i < args.length) {
						// parse classPath
						if (args[i].equals("-c")) {
							i++;
							if (i < args.length) {
								classPath = args[i];
								i++;
							}
							else {
								System.err.println(op.Warning);
							}
						}
						else {
							op.parse(args[i]);
							i++;
						}
					}
				}
				
				StatementResolver SR = new StatementResolver();
				SR.run(javaInput, classPath, op);
			}
			
		} else {
			System.err.println(op.Warning);
		}
	}
	
}