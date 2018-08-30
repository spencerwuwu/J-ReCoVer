package statementResolver.tree;

import java.util.List;
import java.util.ArrayList;

public class Tree{
	
	public TreeNode mRoot;
	public Tree parrent;
	public List<Tree> children;
	
	public Tree(TreeNode node, Tree parrent) {
		mRoot = node;
		parrent = parrent;
		children = new ArrayList<Tree>();
	}
	
	
	public void addBranch(List<String> constraint, int executionOrder, int nextLine, boolean return_flag) {
		Tree newChild = new Tree(new TreeNode(), this);
		newChild.mRoot.setConstraint(constraint);
		newChild.mRoot.setExecutionOrder(executionOrder);
		newChild.mRoot.setNextLine(nextLine);
		newChild.mRoot.setReturnFlag(return_flag);
		children.add(newChild);
		
	}
	
	public void setParrent(Tree parrent) {
		parrent = parrent;
	}
	
	public Tree getParrent() {
		return parrent;
	}
	
	public boolean hasChildren() {
		if (children.isEmpty()) {return false;}
		else return true;
	}
	
	public List<Tree> getChildrenList(){
		return children;
	}
	
	public void print() {
		mRoot.printConstraint();
		mRoot.getState().printForm();
	}
	
}