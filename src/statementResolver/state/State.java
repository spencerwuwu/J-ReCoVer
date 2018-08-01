package statementResolver.state;

import java.util.HashMap;
import java.util.Map;

import soot.Value;
import statementResolver.color.Color;

public class State {
	Map<String, String> local_vars;
	int num;	// State number
	String input_command;
	int command_line_no;

	public State() {
		this.local_vars = new HashMap<String, String>();
		this.num = 0;
	}
	
	public State(Map<String, String> in, int number, String comm, int no) {
		this.local_vars = in;
		this.num = number;
		this.input_command = comm;
		this.command_line_no = no;
	}
	
	public void update(String v, String str) {
		this.local_vars.put(v, str);
	}
	
	public void printForm() {
		System.out.println("+++++++++++++++++++++++");
		System.out.println("| no: " + this.num);
		for (String var : local_vars.keySet()) {
			System.out.println("| "+var + ":\t" + this.local_vars.get(var));
		}
		System.out.println("+++++++++++++++++++++++");
	}
}