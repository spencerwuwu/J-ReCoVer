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
	
	List<String> Constraint;
	State state;
	String branchInfo;
	int executionOrder;
	int nextLine;
	boolean return_flag = false;
	public boolean haveFirstLoop = false;
	

	
	public TreeNode() {
		state = null;
		executionOrder = 0;
		nextLine = 0;
		Constraint = new ArrayList<String>();
		
	}
	public TreeNode(List<String> constraint_list, State newState, int newOrder, int newNextLine, boolean newReturnFlag) {
		state = newState;
		Constraint = constraint_list;
		executionOrder = newOrder;
		nextLine = newNextLine;
		return_flag = newReturnFlag;
	}
	
	public void set_state(State state) {
		state = state;
	}
	
	public State get_state() {
		return state;
	}
	
	public Map<String, String> get_local_vars(){
		return get_state().get_local_vars();
	}
	
	public List<String> get_constraint(){
		return Constraint;
	}
	
	public void set_constraint(List<String> constraint_list) {
		Constraint = constraint_list;
	}
	
	public void add_constraint(String new_constraint) {
		Constraint.add(new_constraint);
	}
	
	public boolean get_return_flag() {
		return return_flag;
	}
	
	public void set_return_flag(boolean rf) {
		return_flag=rf;
	}
	
	public int get_next_line() {
		return nextLine;
	}
	
	public void set_next_line(int i) {
		nextLine=i;
	}
	
	public int get_execution_order() {
		return executionOrder;
	}
	
	public void set_execution_order(int order) {
		executionOrder=order;
	}
	
	public void set_branch_info(String info) {
		branchInfo = info;
	}
	
	public String get_branch_info() {
		return branchInfo;
	}
	
	public void print_constraint() {
		System.out.println("+++++++Constraints++++++++");
		for (String cons: Constraint){
			System.out.println("|"+cons);
		}
		//st.printForm();
		System.out.println("+++++++Constraints++++++++");
	}
}