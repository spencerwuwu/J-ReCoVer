package statementResolver.executionTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import soot.Unit;
import statementResolver.state.UnitSet;
import statementResolver.tree.Tree;
import statementResolver.tree.TreeNode;

public class ExecutionTree {
	private ExecutionTreeNode mRoot;
	private List<UnitSet> mUnits;
	private Map<Unit,Integer> mUnitIndexes;
	private int mEnterLoopLine = 0;
	private int mExitLoopLine = 0;

	private boolean mUseNextBeforeLoop = false;
	private boolean mBefore = true;
	

	public ExecutionTree(ExecutionTreeNode node, List<UnitSet> units, Map<Unit, Integer> unitIndexes, int enterLoopLine, int exitLoopLine) {
		mRoot = node;
		mUnits = units;
		mUnitIndexes = unitIndexes;
		mEnterLoopLine = enterLoopLine;
		mExitLoopLine = exitLoopLine;
	}
	
	public void addRootConstraint(String constraint) {
		mRoot.addConstraint(constraint);
	}
	
	public void executeTree() {
		List<ExecutionTreeNode> currentNodes = new ArrayList<ExecutionTreeNode>();
		List<ExecutionTreeNode> newNodes = new ArrayList<ExecutionTreeNode>();
		List<ExecutionTreeNode> endNodes = new ArrayList<ExecutionTreeNode>();
		
		currentNodes.add(mRoot);
		while (!currentNodes.isEmpty()) {
			for(ExecutionTreeNode currentNode : currentNodes) {
				executeNode(currentNode, endNodes);
				for(ExecutionTreeNode newNode: currentNode.mChildren) {
					newNodes.add(newNode);
				}
			}
			
			currentNodes.clear();
			
			for(ExecutionTreeNode node:newNodes) {
				if (!node.getReturnFlag() && node.getNextLine() < mEnterLoopLine) {
				    currentNodes.add(node);
				} else {
					endNodes.add(node);
				}
				
			}
			newNodes.clear();
			
		}
	}
	
	private void executeNode(ExecutionTreeNode current, List<ExecutionTreeNode> endNodes) {
		
	}

}
