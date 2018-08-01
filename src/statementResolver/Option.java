
package statementResolver;

public class Option {

	public final static String Usage = "usage: Input [options] \n"
			+ "* Input: class/jar/directory \n"
			+ "* Options:\n"
			+ "    -h               help \n"
			+ "    -c class_path    Set classpath (Optional for jar file) \n"
			+ "    -g               Generate control flow graph \n"
			+ "* Example:\n"
			+ "    Analysis jar file \n"
			+ "    $ java -jar jsr.jar your_jar_file.jar\n"
			+ "    Analysis directory \n"
			+ "    $ java -jar jsr.jar input_path/ -c classpath/ \n";
	
	public final static String Warning = "Invalid input, use -h for help";
	public boolean cfg_flag;
	
	public Option() {
		cfg_flag = false;
	}
	
	public void parse(String input) {
		if (input.equals("-g")) {
			this.cfg_flag = true;
		}
	}
	
}