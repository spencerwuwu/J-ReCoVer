package jRecover.optimize.executionTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jRecover.optimize.state.Condition;
import jRecover.optimize.state.Variable;

public class ExecutionTreeNode {
	private List<Condition> mConditions;
	private List<String> mConstraints;
	protected Map<String, Variable> mLocalVars;
	//private State mState;
	private String mBranchInfo;
	private int mNextline;
	private boolean mReturnFlag = false;
	public boolean mHaveFirstLoop = false;

	public ExecutionTreeNode() {
		//mState = null;
		mNextline = 0;
		mConditions = new ArrayList<Condition>();
		mConstraints = new ArrayList<String>();
		mLocalVars = new HashMap<String, Variable>();
		mReturnFlag = false;
	}

	public ExecutionTreeNode(List<Condition> conditions, List<String> constraints, Map<String, Variable> vars, int newNextLine, boolean newReturnFlag) {
		mConditions = new ArrayList<Condition>();
		if (conditions != null && !conditions.isEmpty()) {
			mConditions.addAll(conditions);
		}
		
		mConstraints = new ArrayList<String>();
		if (constraints != null && !constraints.isEmpty()) mConstraints.addAll(constraints);
		//mState = new State(newState);
		mLocalVars = new HashMap<String, Variable>(vars);

		mNextline = newNextLine;
		mReturnFlag = newReturnFlag;
	}

	public ExecutionTreeNode(ExecutionTreeNode node) {
		if (node.getConditions() != null) {
			mConditions = new ArrayList<Condition>();
			for (Condition cond : node.getConditions()) {
				mConditions.add(cond);
			}
		} else
			mConditions = new ArrayList<Condition>();

		if (node.getConstraints() != null) {
			mConstraints = new ArrayList<String>();
			for (String cons : node.getConstraints()) {
				mConstraints.add(cons);
			}
		} else
			mConstraints = new ArrayList<String>();
		
		// mState = new State(newState);
		if (node.getLocalVars() != null) {
			mLocalVars = new HashMap<String, Variable>();
			Map<String, Variable> nodeV = node.getLocalVars();
			for (String key : nodeV.keySet()) {
				mLocalVars.put(key, new Variable(nodeV.get(key)));
			}
		} else
			mLocalVars = new HashMap<String, Variable>();

		mNextline = node.getNextLine();
		mReturnFlag = node.getReturnFlag();
	}

	public Map<String, Variable> getLocalVars(){
		return mLocalVars;
	}
	
	public void setVar(String name, Variable var) {
		mLocalVars.put(name, var);
	}

	public void setRefVar(String name, Variable var) {
		mLocalVars.get(name).updateAs(var);
	}
	
	public List<Condition> getConditions(){
		return mConditions;
	}
	
	public List<String> getConstraints(){
		return mConstraints;
	}
	
	public void setConditions(List<Condition> constraintList) {
		mConditions = new ArrayList<Condition>();
		if (constraintList != null && !constraintList.isEmpty()) mConditions.addAll(constraintList);
	}
	
	public void addCondition(Condition newConstraint) {
		mConditions.add(newConstraint);
	}

	public void addConstraint(String constraint) {
		mConstraints.add(constraint);
	}
	
	public boolean getReturnFlag() {
		return mReturnFlag;
	}
	
	public void setReturnFlag(boolean rf) {
		mReturnFlag = rf;
	}
	
	public int getNextLine() {
		return mNextline;
	}
	
	public void setNextLine(int i) {
		mNextline = i;
	}
	
	public void setBranchInfo(String info) {
		mBranchInfo = info;
	}
	
	public String getBranchInfo() {
		return mBranchInfo;
	}
	
	public void printConstraint() {
		log("++++++ Constraints +++++++");
		for (String cons: mConstraints){
			log("| " + cons);
		}
	}
	
	public void print() {
		printConstraint();
		printForm();
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
