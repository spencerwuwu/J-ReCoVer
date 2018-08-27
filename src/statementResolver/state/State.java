package statementResolver.state;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;



import soot.Value;
import statementResolver.color.Color;

public class State {
	Map<String, String> local_vars;
	int inputUsedIndex = 0;
	int num;	// State number
	String input_command;
	int command_line_no;
	

	public State() {
		this.local_vars = new HashMap<String, String>();
		this.num = 0;
	}
	
	public State(Map<String, String> in, int number, String comm, int no, int inputindex) {
		this.local_vars = new LinkedHashMap<String, String>();
		local_vars.putAll(in);
		this.num = number;
		this.input_command = comm;
		this.command_line_no = no;
		this.inputUsedIndex = inputindex;
	}
	
	public void update(String v, String str) {
		this.local_vars.put(v, str);
	}
	
	/*
	public boolean have_looped() {
		return this.haveFirstLoop;
	}
	
	public void set_have_loooped(boolean looped) {
		haveFirstLoop = looped;
	}
	*/
	
	public int get_command_line_no() {
		return command_line_no;
	}
	public int get_inputUsedIndex() {
		return inputUsedIndex;
	}
	
	public void add_inputUsedIndex() {
		inputUsedIndex++;
	}
	
	public void set_input_command(String s) {
		input_command = s;
	}
	
	public Map<String, String> get_local_vars(){
		return local_vars;
	}
	
	public void set_local_vars(Map<String, String> localVars) {
		local_vars.putAll(localVars);
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