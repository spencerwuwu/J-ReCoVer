package statementResolver.state;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;



import soot.Value;
import statementResolver.color.Color;

public class State {
	Map<String, String> mLocalVars;
	int mInputUsedIndex = 0;
	int mNum;	// State number
	String mInputCommand;
	int mCommandLineNo;
	

	public State() {
		this.mLocalVars = new HashMap<String, String>();
		this.mNum = 0;
	}
	
	public State(Map<String, String> in, int number, String comm, int no, int inputindex) {
		this.mLocalVars = new LinkedHashMap<String, String>();
		mLocalVars.putAll(in);
		this.mNum = number;
		this.mInputCommand = comm;
		this.mCommandLineNo = no;
		this.mInputUsedIndex = inputindex;
	}
	
	public void update(String v, String str) {
		this.mLocalVars.put(v, str);
	}
	
	/*
	public boolean have_looped() {
		return this.haveFirstLoop;
	}
	
	public void set_have_loooped(boolean looped) {
		haveFirstLoop = looped;
	}
	*/
	
	public int getCommandLineNo() {
		return mCommandLineNo;
	}
	public int getInputUsedIndex() {
		return mInputUsedIndex;
	}
	
	public void addInputUsedIndex() {
		mInputUsedIndex++;
	}
	
	public void setInputCommand(String s) {
		mInputCommand = s;
	}
	
	public Map<String, String> getLocalVars(){
		return mLocalVars;
	}
	
	public void setLocalVars(Map<String, String> localVars) {
		mLocalVars.putAll(localVars);
	}
	
	public void printForm() {
		System.out.println("+++++++++++++++++++++++");
		System.out.println("| no: " + this.mNum);
		for (String var : mLocalVars.keySet()) {
			System.out.println("| "+var + ":\t" + this.mLocalVars.get(var));
		}
		System.out.println("+++++++++++++++++++++++");
	}
}