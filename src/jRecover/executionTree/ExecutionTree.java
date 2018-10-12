package jRecover.executionTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jRecover.Option;
import jRecover.color.Color;
import jRecover.state.State;
import jRecover.state.UnitSet;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class ExecutionTree {
	private ExecutionTreeNode mRoot;
	private List<UnitSet> mUnits;
	private Map<Unit,Integer> mUnitIndexes;
	private int mEnterLoopLine = 0;
	private int mExitLoopLine = 0;
	private List<ExecutionTreeNode> mEndNodes = new ArrayList<ExecutionTreeNode>();
	private Map<String, String> mVarsType;

	private boolean mUseNextBeforeLoop = false;
	private int mBeforeLoopDegree = 0;
	private boolean mBefore = true;
	private boolean mNoLoop = false;
	Option mOption;
	

	public ExecutionTree(ExecutionTreeNode node, List<UnitSet> units, Map<Unit, Integer> unitIndexes, 
			int enterLoopLine, int exitLoopLine, Map<String, String> varsType, boolean before, Option op) {
		mRoot = node;
		mUnits = units;
		mUnitIndexes = unitIndexes;
		mEnterLoopLine = enterLoopLine;
		mExitLoopLine = exitLoopLine;
		mBefore = before;
		mVarsType = varsType;
		mOption = op;
		if (mEnterLoopLine == 0 && mExitLoopLine == 0) {
			mNoLoop = true;
		}
	}
	
	public void addRootConstraint(String constraint) {
		mRoot.addConstraint(constraint);
	}
	
	public boolean useNextBeforeLoop() {
		return mUseNextBeforeLoop;
	}
	
	public void executeTree() {
		List<ExecutionTreeNode> currentNodes = new ArrayList<ExecutionTreeNode>();
		List<ExecutionTreeNode> newNodes = new ArrayList<ExecutionTreeNode>();
		List<ExecutionTreeNode> endNodes = new ArrayList<ExecutionTreeNode>();
		
		currentNodes.add(mRoot);
		while (!currentNodes.isEmpty()) {
			int count = 0;
			for (ExecutionTreeNode currentNode : currentNodes) {
				//System.out.println(count++);
				executeNode(currentNode, endNodes, newNodes);
				/*
				for(ExecutionTreeNode newNode: currentNode.mChildren) {
					newNodes.add(newNode);
				}
				*/
			}
			
			currentNodes.clear();
			
			for (ExecutionTreeNode node : newNodes) {
				if (mBefore) {
					if (!node.getReturnFlag() && (mNoLoop || node.getNextLine() < mEnterLoopLine)) {
						currentNodes.add(node);
					} else {
						if (mUseNextBeforeLoop) {
							node.addConstraint("beforeLoopDegree == " + mBeforeLoopDegree);
						}
						endNodes.add(node);
					}
				} else {
					if (!node.getReturnFlag() && node.getNextLine() < mExitLoopLine) {
					//if (!node.getReturnFlag()) {
						currentNodes.add(node);
					} else {
						endNodes.add(node);
					}
				}
			}
			newNodes.clear();
		}
		mEndNodes = endNodes;
	}
	
	public void print() {
		int index = 0;
		for (ExecutionTreeNode node : mEndNodes) {
			if (mBefore) {
				log("====== Before Loop" + String.valueOf(index + 1) + " ======");
			} else {
				log("====== Inter Loop" + String.valueOf(index + 1) + " ======");
			}
			if (!mBefore) {
				// detectOutput(node);
				node.getLocalVars().put("beforeLoop", "1");
			} else {
				node.getLocalVars().put("beforeLoop", "0");
			}
			if (!mOption.silence_flag) node.print();
			log("");
			index += 1;
		}
	}
	
	private void executeNode(ExecutionTreeNode currentNode, List<ExecutionTreeNode> endNodes, List<ExecutionTreeNode> newNodes) {
		if (!currentNode.getReturnFlag() && currentNode.getNextLine() < mUnits.size()) {
			UnitSet us = mUnits.get(currentNode.getNextLine());
			int determineUnitState = determineUnit(us.getUnit());
			//System.out.println(Color.ANSI_BLUE + "line '" + us.getUnit().toString() + "'" + Color.ANSI_RESET);
			log(Color.ANSI_BLUE + "line '" + us.getUnit().toString() + "'" + Color.ANSI_RESET);
			
			boolean skip = false;

			if (determineUnitState == 1) {
				Unit newUnit = us.getUnit();
				State newState = new State(currentNode.getLocalVars(), currentNode.getExecutionOrder(), 
								 newUnit.toString(), us.getLine(), currentNode.getState().getInputUsedIndex());
				if (newUnit instanceof AssignStmt) {
					List<State> assignStates = performAssignStmt(newState, newUnit, currentNode.getLocalVars());
					if (assignStates.size() == 1) {
						ExecutionTreeNode newLeaf = new ExecutionTreeNode(currentNode.getConstraint(), assignStates.get(0), 
								currentNode.getExecutionOrder() + 1, currentNode.getNextLine() + 1, currentNode.getReturnFlag());
						//currentNode.mChildren.add(newLeaf);
						newNodes.add(newLeaf);
					} else if (assignStates.size() == 2) {
						log("Does not seriously implement beforeLoop multiple stage");
						List<String> condition = new ArrayList<String>();
						condition.addAll(currentNode.getConstraint());
						ExecutionTreeNode endLeaf = new ExecutionTreeNode(condition, assignStates.get(0), 
								currentNode.getExecutionOrder(), currentNode.getNextLine(), currentNode.getReturnFlag());
						endLeaf.addConstraint("beforeLoopDegree == " + (mBeforeLoopDegree - 1));
						endNodes.add(endLeaf);
						ExecutionTreeNode newLeaf = new ExecutionTreeNode(currentNode.getConstraint(), assignStates.get(1), 
								currentNode.getExecutionOrder() + 1, currentNode.getNextLine() + 1, currentNode.getReturnFlag());
						//currentNode.mChildren.add(newLeaf);
						newNodes.add(newLeaf);
					}
				} else if (newUnit instanceof IdentityStmt){
					newState = performIdentityStmt(newState, newUnit);
					ExecutionTreeNode newLeaf = new ExecutionTreeNode(currentNode.getConstraint(), newState, 
							currentNode.getExecutionOrder() + 1, currentNode.getNextLine() + 1, currentNode.getReturnFlag());
					
					//currentNode.mChildren.add(newLeaf);
					newNodes.add(newLeaf);
				} else {
					log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
					skip = true;
				}

			} else if (determineUnitState == 2) {
				Unit newUnit=us.getUnit();
				if (newUnit instanceof IfStmt) {
					log("Split the tree here.");
					State newState1 = new State(currentNode.getLocalVars(), currentNode.getExecutionOrder(),
							          newUnit.toString(), us.getLine(), currentNode.getState().getInputUsedIndex());
					State newState2 = new State(currentNode.getLocalVars(), currentNode.getExecutionOrder(), 
							          newUnit.toString(), us.getLine(), currentNode.getState().getInputUsedIndex());
					List<ExecutionTreeNode> newLeafNodes = performIfStmt(currentNode, currentNode.getConstraint(), 
							newState1, newState2, newUnit, mUnitIndexes);
					for (ExecutionTreeNode node: newLeafNodes) {
						//currentNode.mChildren.add(node);
						newNodes.add(node);
					}

				} else if (newUnit instanceof GotoStmt) {
					State newState = new State(currentNode.getLocalVars(), currentNode.getExecutionOrder(), 
							         newUnit.toString(), us.getLine(),currentNode.getState().getInputUsedIndex());
					ExecutionTreeNode newLeaf = performGotoStmt(newState, newUnit, mUnitIndexes, mUnits);
					newLeaf.setConstraint(currentNode.getConstraint());
					newLeaf.setExecutionOrder(currentNode.getExecutionOrder() + 1);
					// 'nextLine' had been set in performGotoStmt
					//currentNode.mChildren.add(newLeaf);
					newNodes.add(newLeaf);

				} else {
					log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
					skip = true;
				}
			} else if (determineUnitState == 3) {
				log(Color.ANSI_GREEN + "return" + Color.ANSI_RESET);
				// Add this treenode to endNodes, waiting to print the result
				endNodes.add(currentNode);
				
				boolean newReturnFlag = true;
			    ExecutionTreeNode newLeaf = new ExecutionTreeNode(currentNode.getConstraint(), 
			    		currentNode.getState(), currentNode.getExecutionOrder(), currentNode.getNextLine(), newReturnFlag);
			    endNodes.add(newLeaf);

			} else if (determineUnitState == 4) {
				// Deal with specialinvoke and vritualinvoke
				if(us.getUnit().toString().contains("specialinvoke")) {
				    ExecutionTreeNode newLeaf = performSpecialInvoke(currentNode, us);
				    //if (newLeaf != null) currentNode.mChildren.add(newLeaf);
				    if (newLeaf != null) newNodes.add(newLeaf);

				} else if(us.getUnit().toString().contains("virtualinvoke") ) {
					ExecutionTreeNode newLeaf = performVirtualInvoke(currentNode, us);
					//currentNode.mChildren.add(newLeaf); 
					newNodes.add(newLeaf);

				} else {
					log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
					skip = true;
				}

			} else {
				log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
				/*
				ExecutionTreeNode newLeaf = new ExecutionTreeNode(currentNode.getConstraint(), currentNode.getState(),
						currentNode.getExecutionOrder(), currentNode.getNextLine() + 1, currentNode.getReturnFlag());
				currentNode.mChildren.add(newLeaf);
				*/
				skip = true;
			}
			log("------------------------------------");

			if (skip) {
			    ExecutionTreeNode newNode = new ExecutionTreeNode(currentNode.getConstraint(), 
			    		currentNode.getState(), currentNode.getExecutionOrder(), currentNode.getNextLine() + 1, currentNode.getReturnFlag());
			    //currentNode.mChildren.add(newNode);
			    newNodes.add(newNode);
			}
		}
	}

	// TODO: Far from matching all cases.
	// 0 : null
	// 1 : no unit return, eg: AssignStmt
	// 2 : going to a unit target, eg: GotoStmt
	// 3 : return
	// 4 : testing
	protected int determineUnit(Unit u) {
		if (u instanceof JLookupSwitchStmt) {
			return 0;
		}
		else if (u instanceof AssignStmt) {
			return 1;
		}
		else if (u instanceof ArrayRef) {
			return 0;
		}
		else if (u instanceof BreakpointStmt) {
			return 0;
		}
		else if (u instanceof BinopExpr) {
			return 0;
		}
		else if (u instanceof CaughtExceptionRef) {
			return 0;
		}
		else if (u instanceof GotoStmt) {
			return 2;
		}
		else if (u instanceof NoSuchLocalException) {
			return 0;
		}
		else if (u instanceof NullConstant) {
			return 0;
		}
		else if (u instanceof IfStmt) {
			return 2;
		}
		else if (u instanceof IdentityStmt) {
			return 1;
		}
		else if (u instanceof InstanceOfExpr) {
			return 0;
		}
		else if (u instanceof JExitMonitorStmt) {
			return 0;
		}
		else if (u instanceof JInvokeStmt) {
			return 4;
		}
		else if (u instanceof ReturnStmt) {
			return 3;
		}
		else if (u instanceof TableSwitchStmt) {
			return 0;
		}
		else if (u instanceof ThrowStmt) {
			return 0;
		}
		else if (u instanceof ReturnVoidStmt) {
			return 3;
		}
		return 4;
	}
	protected String parseLong(String value){
		String values[] = value.split("\\s+");

		// parse long 123L -> 123
		Pattern p = Pattern.compile("^[0-9]+L$");
		int i = 0;
		while (i < values.length) {
			Matcher m = p.matcher(values[i]);
			if (m.find()) {
				values[i] = values[i].split("L")[0];
			}
			i += 1;
		}
		String result = "";
		for (String str : values) {
			if (result.length() == 0) result = str;
			else result = result + " " + str;
		}
		
		return result;
	}
	
	protected String valueReplace(String value, Map<String, String>lastEnv){
		String values[] = value.split("\\s+");

		// parse long 123L -> 123
		Pattern p = Pattern.compile("^[0-9]+L$");
		int i = 0;
		while (i < values.length) {
			Matcher m = p.matcher(values[i]);
			if (m.find()) {
				values[i] = values[i].split("L")[0];
			}
			i += 1;
		}

		boolean endFlag = false;
		while (!endFlag) {
			endFlag = true;
			for (String var: lastEnv.keySet()) {
				int index = 0;
				while (index < values.length) {
					if (values[index].equals(var) && !lastEnv.get(var).contains("hasNext")) {
						values[index] = lastEnv.get(var);
						endFlag = false;
					}
					index += 1;
				}
			}
		}
		
		String newValue = values[0];
		int index = 1;
		while (index < values.length) {
			newValue = newValue + " " + values[index];
			index += 1;
		}
		
		return newValue;
	}

	protected List<State> performAssignStmt(State st, Unit u, Map<String,String> lastEnv) {
		//adding flag to decide if it is in loop or not
		DefinitionStmt ds = (DefinitionStmt) u;
		Value var = ds.getLeftOp();
		// TODO: Should be able to handle more common cases
		Value assignment = ds.getRightOp();
		
		String ass_s = assignment.toString();
		
		List<State> returnStates = new ArrayList<State>();
		// Normal assignment
		if (!ass_s.contains("Iterator")) {
			// removing quotes, eg: (org.apache.hadoop.io.IntWritable) $r6 -> $r6
			ass_s = ass_s.replaceAll("\\(.*?\\)\\s+", "");

			if (ass_s.contains("virtualinvoke")) {
				if (ass_s.contains("compareTo")) {
					// $i0 = virtualinvoke r6.<org.apache.hadoop.io.LongWritable: int compareTo(org.apache.hadoop.io.LongWritable)>(r4)
					String valueL = ass_s.split("\\s+")[1].split("\\.")[0];
					String valueR = ass_s.split("\\>")[1].replace("(", "").replace(")", "");
					if (valueR.length() == 0) ass_s = "1";
					else ass_s = valueL + " - " + valueR ;
				} else {
					// handle virtualinvoke, eg: virtualinvoke $r7.<org.apache.hadoop.io.IntWritable: int get()>() -> $r7
					ass_s = ass_s.split("\\s+")[1].split("\\.")[0];
				}
			}

			// handle staticinvoke, eg: staticinvoke <java.lang.Long: java.lang.Long valueOf(long)>(l0) -> l0
			if (ass_s.contains("staticinvoke")) {
				if (ass_s.contains("java.lang.Math")) {
					// i0 = staticinvoke <java.lang.Math: int max(int,int)>(i0, $i1);
					String params[] = ass_s.split(">")[1].replace("(", "").replace(")", "").split(",\\s+");
					if (ass_s.contains("max")) {
						ass_s = "(ite (> " + params[0] + " " + params[1] + " ) " + params[0] + " " + params[1] + " )";
					}
				} else {
					ass_s = ass_s.split(">")[1].replace("(", "").replace(")", "");
				}
			}

			// eg: $i3 = <reduce_test.context141_200_30_8: int k>
			if (ass_s.contains("<")) {
				ass_s = ass_s.split("\\s+")[2].replace(">", "");
			}
			
			// eg: set $z0 = <reduce_test.collector0_90_5_15: boolean $assertionsDisabled> -> 1
			// bypass assertion
			
			if (ass_s.contains("assertionsDisabled") || ass_s.contains("wasConfigured")) {
				ass_s = "1";
			}

			// change to prefix
			String[] tmp = ass_s.split(" ");
			/*
			if(tmp.length == 3) ass_s = "(" + tmp[1] + " " + tmp[0] + " " + tmp[2] + " )";
			*/
			if(tmp.length == 3) {
				if (!tmp[1].contains("cmp")) {
					if (tmp[1].equals("%")) {
						tmp[1] = "rem";
					} else if (tmp[1].equals("/")) {
						tmp[1] = "div";
					}
					ass_s = "(" + tmp[1] + " " + tmp[0] + " " + tmp[2] + " )";
				} else {
					//ass_s = "(ite (= " + tmp[0] + " " + tmp[2] + " ) 0 ((ite (< " + tmp[0] + " " + tmp[2] + " ) -1 1)))";
					ass_s = "(- " + tmp[0] + " " + tmp[2] + " )";
				}
			}
			
			if (ass_s.length() == 0) ass_s = "0";
			
			// replace rhs with mLocalVars value
			ass_s = valueReplace(ass_s, lastEnv);

			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
			st.update(var.toString(), ass_s);
			returnStates.add(st);
		}
		else { 
			// Handling iterator relative assignments
			if (ass_s.contains("hasNext()")) {
				log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);
				st.update(var.toString(), "hasNext");
				returnStates.add(st);
			} else if (ass_s.contains("next()")) {
				if (mBefore) {
					mBeforeLoopDegree += 1;
					if (!mUseNextBeforeLoop) {
						mUseNextBeforeLoop = true;
						mVarsType.put("input0", "input type");
						st.update(var.toString(), "input0");
						st.update("beforeLoopDegree", Integer.toString(mBeforeLoopDegree));
						log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + "input0");    
						returnStates.add(st);
					} else {
						returnStates.add(st);
						State newState = st.clone();
						String replaceStr = "bld_" + (mBeforeLoopDegree - 1) + "_v";
						for (String key : newState.getLocalVars().keySet()) {
							if (newState.getLocalVars().get(key).equals("input0")) {
								newState.update(key, replaceStr);
								log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + key + " -> " + replaceStr);    
							}
						}
						newState.update(var.toString(), "input0");
						mVarsType.put(replaceStr, "bld");
						newState.update("beforeLoopDegree", Integer.toString(mBeforeLoopDegree));
						log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + "input0");    
						returnStates.add(newState);
					}
				} else {
					//use a new input
					mVarsType.put("input" + st.getInputUsedIndex(), "input type");
					
					ass_s = "input" + st.getInputUsedIndex();
					//st.addInputUsedIndex();
					st.update(var.toString(), ass_s);
					log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + ass_s);    
					returnStates.add(st);
				}
		    }	
		}
		return returnStates;
	}

	protected State performIdentityStmt(State st, Unit u) {
		DefinitionStmt ds = (DefinitionStmt) u;
		Value var = ds.getLeftOp();
		Value assignment = ds.getRightOp();
		// Preserve only org.apache.hadoop.io.'IntWritable and marked it as parameter'
		String assignment_tail = "@parameter "+assignment.toString().split("\\.(?=[^\\.]+$)")[1]; 
		
		log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var.toString() + " -> " + assignment_tail);
		st.update(var.toString(), assignment_tail);
		return st;
	}

	protected List<ExecutionTreeNode> performIfStmt(ExecutionTreeNode parent, List<String> conditionBefore, 
			State ifBranchState, State elseBranchState, Unit u, Map<Unit,Integer> unitIndexes) {
		IfStmt if_st = (IfStmt) u;
		Unit goto_target = if_st.getTargetBox().getUnit();
		Value condition = if_st.getCondition();
		ConditionExpr conditionStmt = (ConditionExpr)condition;
		
		log(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + 
				goto_target + Color.ANSI_GREEN + " when " + Color.ANSI_RESET + condition);
		
		//split the tree
		List<String> ifCondition = new ArrayList<String>();
		List<String> elseCondition = new ArrayList<String>();
		for (String s: conditionBefore) {
			ifCondition.add(s);
			elseCondition.add(s);
		}
		
		String newIfCondition = condition.toString();
		String newElseCondition = "! " + condition.toString();
		
		// replace with mLocalVars value
		Map<String, String> lastEnv = parent.getLocalVars();
		
		// Won't set new branch for hasNext, automatically set as true
		//newIfCondition = valueReplace(newIfCondition, lastEnv).replace("_v", "");
		//newElseCondition = valueReplace(newElseCondition, lastEnv).replace("_v", "");
		newIfCondition = parseLong(newIfCondition).replace("_v", "");
		newElseCondition = parseLong(newElseCondition).replace("_v", "");

		if(parent.getLocalVars().get(conditionStmt.getOp1().toString()) == "hasNext" ) {
			ExecutionTreeNode elseBranch = new ExecutionTreeNode(elseCondition, elseBranchState, 
					parent.getExecutionOrder() + 1, parent.getNextLine() + 1, parent.getReturnFlag());
			//elseBranch.getLocalVars().put(conditionStmt.getOp1().toString(), "1");
			elseBranch.setBranchInfo("ELSE branch from" + parent.getState().getCommandLineNo() );
			List<ExecutionTreeNode> returnList = new ArrayList<ExecutionTreeNode>();
			returnList.add(elseBranch);
			log("Actually we didn't");
			return returnList;
		}
		ifCondition.add( newIfCondition );
		elseCondition.add( newElseCondition );
				
		ExecutionTreeNode ifBranch = new ExecutionTreeNode(ifCondition, ifBranchState, 
				parent.getExecutionOrder() + 1, unitIndexes.get(goto_target), parent.getReturnFlag());
		ExecutionTreeNode elseBranch = new ExecutionTreeNode(elseCondition, elseBranchState, 
				parent.getExecutionOrder() + 1, parent.getNextLine() + 1, parent.getReturnFlag());
		
		ifBranch.setBranchInfo("IF branch from" + parent.getState().getCommandLineNo() );
		elseBranch.setBranchInfo("ELSE branch from" + parent.getState().getCommandLineNo() );
		
		List<ExecutionTreeNode> returnList = new ArrayList<ExecutionTreeNode>();
		returnList.add(ifBranch);
		returnList.add(elseBranch);
		return returnList;
	}

	protected ExecutionTreeNode performGotoStmt(State st, Unit u, Map<Unit,Integer> unitIndexes, List<UnitSet> units) {
		GotoStmt gt_st = (GotoStmt) u;
		Unit goto_target = gt_st.getTarget();
		ExecutionTreeNode node;
		
		if(unitIndexes.get(goto_target) > st.getCommandLineNo()) {
		    //System.out.println(Color.ANSI_GREEN + st.getCommandLineNo() + " goto " + Color.ANSI_RESET + goto_target);
		    log(Color.ANSI_GREEN + st.getCommandLineNo() + " goto " + Color.ANSI_RESET + goto_target);
		    node = new ExecutionTreeNode(null, st, 0, unitIndexes.get(goto_target), false);
		}
		else {
			//System.out.println(Color.ANSI_GREEN +  st.getCommandLineNo() + " goto " + Color.ANSI_RESET + unitIndexes.get(goto_target) + " (Loop back, terminate)");
			log(Color.ANSI_GREEN + st.getCommandLineNo() + " goto " + Color.ANSI_RESET + unitIndexes.get(goto_target) + " (Loop back, terminate)");
			node = new ExecutionTreeNode(null, st, 0, st.getCommandLineNo(), true);
		}
		return node;
	}

	protected ExecutionTreeNode performVirtualInvoke(ExecutionTreeNode currentNode, UnitSet us) {
		State newState = currentNode.getState();
		Map<String, String>lastEnv = newState.getLocalVars();
		/*
		InvokeExpr ivk = ((Stmt) us.getUnit()).getInvokeExpr();
		JimpleBody body = (JimpleBody) ivk.getMethod().retrieveActiveBody();
		List<Unit> units = new ArrayList<Unit>();
		UnitGraph graph = new ExceptionalUnitGraph(body);
		Iterator gIt = graph.iterator();
		while (gIt.hasNext()) {
			Unit u = (Unit)gIt.next();	//getClass(): soot.jimple.internal.*Stmt		
			units.add(u);
		}
		for (Unit u : units) {
			System.out.println(u.toString());
		}
		*/
		
		// handling OutputCollector
		if(us.getUnit().toString().contains("OutputCollector") || us.getUnit().toString().contains("Context")) {
			String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
			String value = (us.getUnit().toString().split(">")[1]).split(",")[1];
			value = value.replace(")", "");
			
			value = valueReplace(value, lastEnv);

			newState.update(key, value);
			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + key + " -> " + value);
			newState.update("output", value);
			log(Color.ANSI_GREEN + "output: " + value + Color.ANSI_RESET);

		} else {
			String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
			String value = us.getUnit().toString().split(">")[1];
			value = value.replace(")", "");
			value = value.replace("(", "");

			value = valueReplace(value, lastEnv);

			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + us.getUnit().toString() + " -> " + value);
			newState.update(key.toString() , value);
		}
		ExecutionTreeNode newLeaf = new ExecutionTreeNode(currentNode.getConstraint(), newState, 
											currentNode.getExecutionOrder(), currentNode.getNextLine() + 1, currentNode.getReturnFlag());
		return newLeaf;

	}
	
	protected ExecutionTreeNode performSpecialInvoke(ExecutionTreeNode currentNode, UnitSet us) {
		ExecutionTreeNode newLeaf = new ExecutionTreeNode(currentNode.getConstraint(), currentNode.getState(), 
											currentNode.getExecutionOrder(), currentNode.getNextLine() + 1, currentNode.getReturnFlag());
		if(us.getUnit().toString().contains("init")) {
			String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
			String value = us.getUnit().toString().split(">")[2];
			value = value.replace(")", "");
			value = value.replace("(", "");
			if (value.length() == 0) {
				value = "0";
			}
			
			value = valueReplace(value, currentNode.getState().getLocalVars());
			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + key + " -> " + value);
			newLeaf.getState().update(key, value);
			
			return newLeaf;
		}				        
		return null;
	}

	public Map<String, String> getVarType() {
		return mVarsType;
	}
	
	// Does not affect the result, just to make the Symbolic State more reasonable
	protected void detectOutput(ExecutionTreeNode node) {
		for (UnitSet us : mUnits) {
			Unit u = us.getUnit();
			if (u instanceof InvokeStmt) {
				if(u.toString().contains("virtualinvoke")) {
					if(u.toString().contains("OutputCollector") || u.toString().contains("Context")) {
						String value = (u.toString().split(">")[1]).split(",")[1];
				        value = value.replace(")", "");
                        for (String replace: node.getLocalVars().keySet()) {
				        	if(value.contains(replace) ) {
				        	    value = value.replace(replace, node.getLocalVars().get(replace));
				        	}
				        }
				        node.getLocalVars().put("output", value);
					}
				}
				else if (u.toString().contains("specialinvoke")) {
				    if (u.toString().contains("Writable") && u.toString().contains("init")) {
				    	String key = (u.toString().split("\\s+")[1]).split("\\.")[0];
				    	String type = u.toString().split(">")[1];
				        String value = u.toString().split(">")[2];
				        value = value.replace(")", "");
				        value = value.replace("(", "");
				        
				        for (String replace : node.getLocalVars().keySet()) {
				        	if(value.contains(replace) ) {
				        	    value = value.replace(replace, node.getLocalVars().get(replace));
				        	}
				        }
				        node.getLocalVars().put(key, value);    
				    }				        
				    
				}
			}
		}
	}

	public List<ExecutionTreeNode> getEndNodes() {
		return mEndNodes;
	}
	
	public void logAll(String str) {
		if (mOption.silence_flag) System.out.println("[  ETree]  " + str);
		else System.out.println(str);
	}
	
	public void log(String str) {
		if (!mOption.silence_flag) System.out.println(str);
	}

}
