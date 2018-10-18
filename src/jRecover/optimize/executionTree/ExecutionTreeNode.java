package jRecover.optimize.executionTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jRecover.optimize.state.Condition;
import jRecover.optimize.state.State;
import jRecover.optimize.state.Variable;

public class ExecutionTreeNode {
	private List<Condition> mConditions;
	private State mState;
	private String mBranchInfo;
	private int mExecutionOrder;
	private int mNextline;
	private boolean mReturnFlag = false;
	public boolean mHaveFirstLoop = false;

	public ExecutionTreeNode() {
		mState = null;
		mExecutionOrder = 0;
		mNextline = 0;
		mConditions = new ArrayList<Condition>();
		mReturnFlag = false;
	}

	public ExecutionTreeNode(List<Condition> conditions, State newState, int newOrder, int newNextLine, boolean newReturnFlag) {
		mConditions = new ArrayList<Condition>();
		if (conditions != null && !conditions.isEmpty()) mConditions.addAll(conditions);
		mState = new State(newState);
		mExecutionOrder = newOrder;
		mNextline = newNextLine;
		mReturnFlag = newReturnFlag;
	}

	public void setState(State state) {
		mState = new State(state);
	}
	
	public State getState() {
		return mState;
	}
	
	public Map<String, Variable> getLocalVars(){
		return getState().getLocalVars();
	}
	
	public List<Condition> getConditions(){
		return mConditions;
	}
	
	public void setConditions(List<String> constraintList) {
		mConditions = new ArrayList<Condition>();
		if (constraintList != null && !constraintList.isEmpty()) mConditions.addAll(constraintList);
	}
	
	public void addConstraint(Condition newConstraint) {
		mConditions.add(newConstraint);
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
	
	public int getExecutionOrder() {
		return mExecutionOrder;
	}
	
	public void setExecutionOrder(int order) {
		mExecutionOrder = order;
	}
	
	public void setBranchInfo(String info) {
		mBranchInfo = info;
	}
	
	public String getBranchInfo() {
		return mBranchInfo;
	}
	
	public void printConstraint() {
		log("++++++ Constraints +++++++");
		for (String cons: mConstraint){
			log("| " + cons);
		}
	}
	
	public void print() {
		printConstraint();
		getState().printForm();
	}

	protected void log(String str) {
		System.out.println(str);
	}

}
