
package jRecover;

public class Option {

	public final static String Usage = 
			  " * Usage:"
			+ "     <*.jar> <classname> [option] \n\n"
			+ " * Options:\n"
			+ "    -h              help\n"
			+ "    -c classpath    Set classpath (Optional if you had packed all libs into the jar)\n"
			+ "    -j              Jimple mode, only output Jimple code\n"
			+ "    -g              Generate control flow graph\n"
			+ " * Example:\n"
			+ "     $ java -jar jsr.jar your_jar.jar reducer_classname\n"
			+ "   Slience mode \n"
			+ "     $ java -jar jsr.jar your_jar.jar reducer_classname -j\n";
	
	public final static String Warning = "Invalid input, use -h for help";
	public boolean cfg_flag;
	public boolean silence_flag;
	public boolean jimple_flag;
	
	public Option() {
		cfg_flag = false;
		silence_flag = false;
		jimple_flag = false;
	}
	
	public String parse(String input) {
		if (input.equals("-g")) {
			this.cfg_flag = true;
		} else if (input.equals("-j")) {
			this.jimple_flag = true;
		} else if (input.equals("-s")) {
			this.silence_flag = true;
		} else {
			return input;
		}
		return "z3_.txt";
	}
	
}