
package jRecover;

public class Option {

	public final static String Usage = 
			  " * Usage:"
			+ "     <*.jar> <classname> [option] \n\n"
			+ " * Options:\n"
			+ "    -h              help\n"
			+ "    -c classpath    Set classpath (Optional if you had packed all libs into the jar)\n"
			+ "    -j              Jimple mode, only output Jimple code\n"
			+ "    -s              Silence mode, print out less log\n"
			+ "    -o              Optimize mode for formula generation\n"
			+ " * Example:\n"
			+ "     $ java -jar j-recover.jar your_jar.jar reducer_classname\n"
			+ "   Jimple mode \n"
			+ "     $ java -jar j-recover.jar your_jar.jar reducer_classname -j\n";
	
	public final static String Warning = "Invalid input, use -h for help";
	public boolean cfg_flag;
	public boolean silence_flag;
	public boolean jimple_flag;
	public boolean optimize_flag;
	
	public Option() {
		cfg_flag = false;
		silence_flag = false;
		jimple_flag = false;
	}
	
	public void parse(String input) {
		if (input.equals("-j")) {
			this.jimple_flag = true;
		} else if (input.equals("-s")) {
			this.silence_flag = true;
		} else if (input.equals("-o")) {
			this.optimize_flag = true;
		}
	}
	
}