package statementResolver.tree;

import java.util.List;
import java.util.Map;

import soot.Unit;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import statementResolver.color.Color;
import statementResolver.state.State;
import statementResolver.state.StateUnitPair;
import statementResolver.state.UnitSet;



public class TreeNode{
	
	List<String> mConstraint;
	State mState;
	String mBranchInfo;
	int mExecutionOrder;
	int mNextline;
	boolean mReturnFlag = false;
	public boolean mHaveFirstLoop = false;
	

	
	public TreeNode() {
		mState = null;
		mExecutionOrder = 0;
		mNextline = 0;
		mConstraint = new ArrayList<String>();
		
	}
	public TreeNode(List<String> constraint_list, State newState, int newOrder, int newNextLine, boolean newReturnFlag) {
		mConstraint = constraint_list;
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
	
	public void setConstraint(List<String> constraint_list) {
		mConstraint = constraint_list;
	}
	
	public void addConstraint(String new_constraint) {
		mConstraint.add(new_constraint);
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
		System.out.println("+++++++Constraints++++++++");
		for (String cons: mConstraint){
			System.out.println("|"+cons);
		}
		//st.printForm();
		System.out.println("+++++++Constraints++++++++");
	}
}