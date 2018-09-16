
package communicativeSolver;

public class Option {

	public final static String Usage = 
			  " * Usage:"
			+ "     <*.jar> <classname> [options] \n\n"
			+ " * Options:\n"
			+ "    -h              help\n"
			+ "    -c classpath    Set classpath (Optional if you had packed all libs into the jar)\n"
			+ "    -s              Silence mode, only output Jimple code\n"
			+ "    -g              Generate control flow graph\n"
			+ " * Example:\n"
			+ "     $ java -jar jsr.jar your_jar.jar reducer_classname\n"
			+ "   Slience mode \n"
			+ "     $ java -jar jsr.jar your_jar.jar reducer_classname -s\n";
	
	public final static String Warning = "Invalid input, use -h for help";
	public boolean cfg_flag;
	public boolean silence_flag;
	
	public Option() {
		cfg_flag = false;
	}
	
	public void parse(String input) {
		if (input.equals("-g")) {
			this.cfg_flag = true;
		}
		if (input.equals("-s")) {
			this.silence_flag = true;
		}
	}
	
}