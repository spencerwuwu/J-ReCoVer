package jRecover.executionTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jRecover.state.State;

public class ExecutionTreeNode {
	//public List<ExecutionTreeNode> mChildren = new ArrayList<ExecutionTreeNode>();

	private List<String> mConstraint;
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
		mConstraint = new ArrayList<String>();
		mReturnFlag = false;
	}

	public ExecutionTreeNode(List<String> constraintList, State newState, int newOrder, int newNextLine, boolean newReturnFlag) {
		mConstraint = new ArrayList<String>();
		if (constraintList != null && !constraintList.isEmpty()) mConstraint.addAll(constraintList);
		mState = newState;
		mExecutionOrder = newOrder;
		mNextline = newNextLine;
		mReturnFlag = newReturnFlag;
	}

	public void setState(State state) {
		mState = state;
	}
	
	public State getState() {
		return mState;
	}
	
	public Map<String, String> getLocalVars(){
		return getState().getLocalVars();
	}
	
	public List<String> getConstraint(){
		return mConstraint;
	}
	
	public void setConstraint(List<String> constraintList) {
		mConstraint = new ArrayList<String>();
		if (constraintList != null && !constraintList.isEmpty()) mConstraint.addAll(constraintList);
	}
	
	public void addConstraint(String newConstraint) {
		mConstraint.add(newConstraint);
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
		System.out.println("++++++ Constraints +++++++");
		for (String cons: mConstraint){
			System.out.println("| " + cons);
		}
	}
	
	public void patchNullValue() {
		Map<String, String> vars = getLocalVars();
		Iterator it = vars.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = vars.get(key);
			System.out.println("'" + value + "'");
			if (value == null || value.length() == 0) vars.put(key, "0");
		}
	}
 	
	public void print() {
		printConstraint();
		getState().printForm();
	}

}
