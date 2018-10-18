package jRecover.optimize.state;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jRecover.color.Color;
import soot.Value;

public class State {
	protected Map<String, Variable> mLocalVars;
	protected int mNum;	// State number
	protected int mCommandLineNo;
	

	public State() {
		mLocalVars = new HashMap<String, Variable>();
		mNum = 0;
	}
	
	public State(State s) {
		mLocalVars = new HashMap<String, Variable>(s.getLocalVars());
		mNum = s.getNum();
		mCommandLineNo = s.getCommandLineNo();
	}
	
	public State(Map<String, Variable> in, int number, int no) {
		mLocalVars = new LinkedHashMap<String, Variable>();
		mLocalVars.putAll(in);
		mNum = number;
		mCommandLineNo = no;
	}
	
	public State clone() {
		return new State(mLocalVars, mNum, mCommandLineNo);
	}
	
	public int getCommandLineNo() {
		return mCommandLineNo;
	}

	public Map<String, Variable> getLocalVars(){
		return mLocalVars;
	}
	
	public void setLocalVars(Map<String, Variable> localVars) {
		mLocalVars.putAll(localVars);
	}
	
	public int getNum() {
		return mNum;
	}
	
	public void printForm() {
		log("+++++++++++++++++++++++");
		for (String var : mLocalVars.keySet()) {
			log("| " + var + ":\t" + mLocalVars.get(var).toString());
		}
		log("+++++++++++++++++++++++");
	}

	protected void log(String str) {
		System.out.println(str);
	}
}