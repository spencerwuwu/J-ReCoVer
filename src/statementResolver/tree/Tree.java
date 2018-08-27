package statementResolver.tree;

import java.util.List;
import java.util.ArrayList;

public class Tree{
	
	public TreeNode root;
	public Tree parrent;
	public List<Tree> children;
	
	public Tree(TreeNode node, Tree parrent) {
		root = node;
		parrent = parrent;
		children = new ArrayList<Tree>();
	}
	
	
	public void add_branch(List<String> constraint, int executionOrder, int nextLine, boolean return_flag) {
		Tree newChild = new Tree(new TreeNode(), this);
		newChild.root.set_constraint(constraint);
		newChild.root.set_execution_order(executionOrder);
		newChild.root.set_next_line(nextLine);
		newChild.root.set_return_flag(return_flag);
		children.add(newChild);
		
	}
	
	public void set_parrent(Tree parrent) {
		parrent = parrent;
	}
	
	public Tree get_parrent() {
		return parrent;
	}
	
	public boolean has_children() {
		if (children.isEmpty()) {return false;}
		else return true;
	}
	
	public List<Tree> get_children_list(){
		return children;
	}
	
	public void print() {
		root.print_constraint();
		root.get_state().printForm();
	}
	
}