package jRecover.optimize.executionTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jRecover.Option;
import jRecover.color.Color;
import jRecover.optimize.state.Condition;
import jRecover.optimize.state.UnitSet;
import jRecover.optimize.state.Variable;
import jRecover.optimize.z3FormatPipeline.Z3Pipeline;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.*;

public class ExecutionTree {
	private ExecutionTreeNode mRoot;
	private List<UnitSet> mUnits;
	private Map<Unit,Integer> mUnitIndexes;
	private int mEnterLoopLine = 0;
	private int mExitLoopLine = 0;
	private List<ExecutionTreeNode> mEndNodes = new ArrayList<ExecutionTreeNode>();
	private Map<String, String> mVarsType;

	private boolean mUseNextBeforeLoop = false;
	private boolean mBefore = true;
	private boolean mNoLoop = false;
	Option mOption;
	//private int mBeforeLoopDegree = 0;
	

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
		
		if (!mBefore) {
			mRoot.addConstraint("beforeLoop == 0");
			mRoot.addCondition(new Condition("!=", new Variable("beforeLoop"), new Variable("1"), false));
		} else {
			mRoot.addConstraint("beforeLoop == 1");
			mRoot.addCondition(new Condition("==", new Variable("beforeLoop"), new Variable("1"), false));
		}

		currentNodes.add(mRoot);
		while (!currentNodes.isEmpty()) {
			logAll("Concurrent nodes: " + new Integer(currentNodes.size()).toString());
			for (ExecutionTreeNode currentNode : currentNodes) {
				executeNode(currentNode, endNodes, newNodes);
			}
			
			currentNodes.clear();
			
			for (ExecutionTreeNode node : newNodes) {
				if (mBefore) {
					if (!node.getReturnFlag() && (mNoLoop || node.getNextLine() < mEnterLoopLine)) {
						currentNodes.add(node);
					} else {
						endNodes.add(node);
					}
				} else {
					if (!node.getReturnFlag() && node.getNextLine() < mExitLoopLine) {
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
		if (mOption.z3_mode) return;
		int index = 0;
		for (ExecutionTreeNode node : mEndNodes) {
			if (mBefore) {
				log("====== Before Loop" + String.valueOf(index + 1) + " ======");
			} else {
				log("====== Inter Loop" + String.valueOf(index + 1) + " ======");
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
			log(Color.ANSI_BLUE + "line '" + us.getUnit().toString() + "'" + Color.ANSI_RESET);
			
			boolean skip = false;

			if (determineUnitState == 1) {
				Unit newUnit = us.getUnit();
				if (newUnit instanceof AssignStmt) {
					List<ExecutionTreeNode> nodes = performAssignStmt(currentNode, newUnit);
					for (ExecutionTreeNode newNode : nodes) {
						newNode.setNextLine(currentNode.getNextLine() + 1);
						newNodes.add(newNode);
					}

				} else if (newUnit instanceof IdentityStmt){
					//ExecutionTreeNode newNode = performIdentityStmt(currentNode, newUnit);
					currentNode.setNextLine(currentNode.getNextLine() + 1);
					newNodes.add(currentNode);
				} else {
					log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
					skip = true;
				}
			} else if (determineUnitState == 2) {
				Unit newUnit=us.getUnit();
				if (newUnit instanceof IfStmt) {
					List<ExecutionTreeNode> newLeafNodes = performIfStmt(currentNode, newUnit, mUnitIndexes);
					if (newLeafNodes.size() > 1) log("Split the tree here.");
					for (ExecutionTreeNode node: newLeafNodes) {
						//currentNode.mChildren.add(node);
						newNodes.add(node);
					}

				} else if (newUnit instanceof GotoStmt) {
					newNodes.add(performGotoStmt(currentNode, newUnit, mUnitIndexes));

				} else {
					log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
					skip = true;
				}
			} else if (determineUnitState == 3) {
				log(Color.ANSI_GREEN + "return" + Color.ANSI_RESET);
				// Add this treenode to endNodes, waiting to print the result
			    currentNode.setReturnFlag(true);
			    endNodes.add(currentNode);

			} else if (determineUnitState == 4) {
				// Deal with specialinvoke and vritualinvoke
				if(us.getUnit().toString().contains("specialinvoke")) {
				    ExecutionTreeNode newLeaf = performSpecialInvoke(currentNode, us);
				    if (newLeaf != null) newNodes.add(newLeaf);

				} else if(us.getUnit().toString().contains("virtualinvoke") ) {
					ExecutionTreeNode newLeaf = performVirtualInvoke(currentNode, us);
					newNodes.add(newLeaf);

				} else {
					log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
					skip = true;
				}

			} else {
				log(Color.ANSI_RED + "Skip" + Color.ANSI_RESET);
				skip = true;
			}
			log("------------------------------------");

			if (skip) {
				currentNode.setNextLine(currentNode.getNextLine() + 1);
			    newNodes.add(currentNode);
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
	
	protected String parseExpon(String value) {
		return new Long(Double.valueOf(value).longValue()).toString();
	}
	
	protected boolean checkNumber(String value) {
		if (value.length() > 10) {
			char a = value.charAt(1);
			if ((a > '9' || a < '0') && a != '.') return false;
		}
		Pattern p = Pattern.compile("^-?[0-9]*(\\.[0-9]+(E[0-9]+)?F?)?L?$");
		Matcher m = p.matcher(value);
		if (m.find()) {
			return true;
		}
		return false;
	}
	
	protected Variable str2Var(String value, Map<String, Variable> vList) {
		if (checkNumber(value)) {
			if (value.contains("E")) return new Variable(parseExpon(value), false);
			// parse float 123L -> 123L
			if (value.contains("L")) return new Variable(value.split("L")[0], true);
			// parse float -123.2F -> -123.2
			if (value.contains("F")) return new Variable(value.split("F")[0], false);
			return new Variable(value, !value.contains("."));
		}
		if (vList.containsKey(value)) {
			return new Variable(vList.get(value));
		} else {
			if (value.contains("new ")) {
				return new Variable("null");
			} else {
				vList.put(value, new Variable(value));
				if (!mVarsType.containsKey(value)) {
					mVarsType.put(value, "double");
				}
			}
		}
		return new Variable(value);
	}

	protected List<ExecutionTreeNode> performAssignStmt(ExecutionTreeNode node, Unit u) {
		//adding flag to decide if it is in loop or not
		DefinitionStmt ds = (DefinitionStmt) u;
		Value var = ds.getLeftOp();
		// TODO: Should be able to handle more common cases
		Value assignment = ds.getRightOp();
		
		String ass_s = assignment.toString();
		
		List<ExecutionTreeNode> resultNodes = new ArrayList<ExecutionTreeNode>();
		
		// Handle ref type for *Writable
		String lhs = var.toString();
		// parse lhs if necessary
		// eg: r0.<reduce_test.autoGenerator: int lastKey> = $i0
		if (lhs.contains("r0.<")) {
			lhs = lhs.split("\\.<")[1].replace('>', ' ').split("\\s+")[2];
			if (!mVarsType.containsKey(lhs)) {
				String type = var.toString().split("\\.<")[1].split("\\s+")[1];
				mVarsType.put(lhs, type);
				node.setVar(lhs, new Variable(lhs));
			}
		}

		if (mVarsType.containsKey(lhs) && mVarsType.get(lhs).contains("Writable")) {
			if (ass_s.contains("new org.")) {
				node.setVar(lhs, new Variable("null"));
				log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + lhs + " -> " + "null");
			} else {
				// removing quotes, eg: (org.apache.hadoop.io.IntWritable) $r6 -> $r6
				ass_s = ass_s.replaceAll("\\(.*?\\)\\s+", "");
				// eg: <reduce_test.collector91_140_7_17: org.apache.hadoop.io.IntWritable SumValue> -> SumValue
				if (ass_s.contains("<")) {
					ass_s = ass_s.split("\\s+")[2].replace(">", "");
				}
				
				if (!node.getLocalVars().containsKey(ass_s)) {
					mVarsType.put(ass_s, mVarsType.get(lhs));
					Variable newValue = new Variable(ass_s);
					node.setVar(ass_s, newValue);
					node.setVar(lhs, newValue);
				} else {
					node.setVar(lhs, node.getLocalVars().get(ass_s));
				}
				log(Color.ANSI_CYAN + "assign: " + Color.ANSI_RESET + lhs + " -> " + ass_s);
			}
			resultNodes.add(node);
			return resultNodes;
		}
		
		// Normal assignment
		if (!ass_s.contains("Iterator")) {

			String target = lhs;

			// removing quotes, eg: (org.apache.hadoop.io.IntWritable) $r6 -> $r6
			ass_s = ass_s.replaceAll("\\(.*?\\)\\s+", "");

			if (ass_s.contains("virtualinvoke")) {
				if (ass_s.contains("compareTo")) {
					// $i0 = virtualinvoke r6.<org.apache.hadoop.io.LongWritable: int compareTo(org.apache.hadoop.io.LongWritable)>(r4)
					String valueL = ass_s.split("\\s+")[1].split("\\.")[0];
					String valueR = ass_s.split("\\>")[1].replace("(", "").replace(")", "");
					if (valueR.length() == 0) {
						ass_s = "1";
						node.setVar(target, new Variable(ass_s));
					} else {
						Variable lhsV = str2Var(valueL, node.getLocalVars());
						Variable rhsV = str2Var(valueR, node.getLocalVars());

						node.setVar(target, new Variable("-", lhsV, rhsV));
					}
					log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + lhs + " -> " +  valueL + " compareTo(" + valueR + ")");
					resultNodes.add(node);
					return resultNodes;
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
					Variable valueL = str2Var(params[0], node.getLocalVars());
					Variable valueR = str2Var(params[1], node.getLocalVars());
					if (ass_s.contains("max")) {
						ExecutionTreeNode ge = new ExecutionTreeNode(node);
						ge.addConstraint(params[0] + " >= " + params[1]);
						ge.addCondition(new Condition(">=", valueL, valueR, false));
						ge.setVar(target, valueL);
						ExecutionTreeNode lt = node;
						lt.addConstraint(params[0] + " < " + params[1]);
						lt.addCondition(new Condition("<", valueL, valueR, false));
						lt.setVar(target, valueR);
						resultNodes.add(ge);
						resultNodes.add(lt);
						ass_s = "max(" + params[0] + ", " + params[1] + ")";
						log("Split tree here");
					} else if (ass_s.contains("min")) {
						ExecutionTreeNode ge = new ExecutionTreeNode(node);
						ge.addConstraint(params[0] + " >= " + params[1]);
						ge.addCondition(new Condition(">=", valueL, valueR, false));
						ge.setVar(target, valueR);
						ExecutionTreeNode lt = node;
						lt.addConstraint(params[0] + "<" + params[1]);
						lt.addCondition(new Condition("<", valueL, valueR, false));
						lt.setVar(target, valueL);
						resultNodes.add(ge);
						resultNodes.add(lt);
						ass_s = "max(" + params[0] + ", " + params[1] + ")";
						log("Split tree here");
					}
					log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + lhs + " -> " + ass_s);
					return resultNodes;
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

			if (ass_s.length() == 0) ass_s = "0";
			else {
			// handling operations
				String[] tmp = ass_s.split(" ");
				if(tmp.length == 3) {
					Variable a = str2Var(tmp[0], node.getLocalVars());
					Variable b = str2Var(tmp[2], node.getLocalVars());
					if (tmp[1].contains("+")) {
						//node.setVar(target, new Variable("+", a, b));
						node.setVar(target, a.operate("+", b));
					} else if (tmp[1].contains("cmp") || tmp[1].contains("-")) {
						//node.setVar(target, new Variable("-", a, b));
						node.setVar(target, a.operate("-", b));
					} else if (tmp[1].contains("*")) {
						//node.setVar(target, new Variable("*", a, b));
						node.setVar(target, a.operate("*", b));
					} else if (tmp[1].contains("/")) {
						//node.setVar(target, new Variable("div", a, b));
						node.setVar(target, a.operate("div", b));
					} else if (tmp[1].contains("%")) {
						//node.setVar(target, new Variable("rem", a, b));
						node.setVar(target, a.operate("rem", b));
					}
				} else {
					node.setVar(target, str2Var(ass_s, node.getLocalVars()));
				}
			}

			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + lhs + " -> " + ass_s);
		}
		else { 
			// Handling iterator relative assignments
			if (ass_s.contains("hasNext()")) {
				log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + lhs + " -> " + "hasNext");
				node.setVar(lhs, new Variable("hasNext"));
			} else if (ass_s.contains("next()")) {
				if (mBefore) {
					//mBeforeLoopDegree += 1;
					if (!mUseNextBeforeLoop) {
						mUseNextBeforeLoop = true;
						node.setVar(lhs, new Variable("input0"));
						log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + lhs + " -> " + "input0");    
					} else {
						logAll("Does not support multiple input in one function body yet.");
					}
				} else {
					//use a new input
					mVarsType.put("input0", "input type");
					
					ass_s = "input0";
					node.setVar(lhs, new Variable(ass_s));
					log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + lhs + " -> " + ass_s);    
				}
		    }	
		}
		resultNodes.add(node);
		return resultNodes;
	}

	protected ExecutionTreeNode performIdentityStmt(ExecutionTreeNode node, Unit u) {
		DefinitionStmt ds = (DefinitionStmt) u;
		String var = ds.getLeftOp().toString();
		//Value assignment = ds.getRightOp();
		// Preserve only org.apache.hadoop.io.'IntWritable and marked it as parameter'
		//String assignment_tail = "@parameter."+assignment.toString().split("\\.(?=[^\\.]+$)")[1]; 
		
		log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + var + " -> " + var + "_v");
		//st.update(var.toString(), assignment_tail);
		node.setVar(var, new Variable(var));
		return node;
	}

	protected List<ExecutionTreeNode> performIfStmt(ExecutionTreeNode parent, Unit u, Map<Unit,Integer> unitIndexes) {
		IfStmt if_st = (IfStmt) u;
		Unit goto_target = if_st.getTargetBox().getUnit();
		Value condition = if_st.getCondition();
		ConditionExpr conditionStmt = (ConditionExpr)condition;
		
		log(Color.ANSI_GREEN + "goto " + Color.ANSI_RESET + 
				goto_target + Color.ANSI_GREEN + " when " + Color.ANSI_RESET + condition);

		List<ExecutionTreeNode> returnList = new ArrayList<ExecutionTreeNode>();
		String lhs = conditionStmt.getOp1().toString();
		String rhs =  conditionStmt.getOp2().toString();
		String op =  conditionStmt.getSymbol().toString().replaceAll("\\s+", "");
		if (parent.getLocalVars().get(lhs).getValue().containsKey("hasNext_v")) {
			parent.setNextLine(parent.getNextLine() + 1);
			returnList.add(parent);
	 	} else {
	 		ExecutionTreeNode ifBranch = new ExecutionTreeNode(parent);
	 		ExecutionTreeNode elseBranch = parent;
	 		
			ifBranch.setNextLine(unitIndexes.get(goto_target));
			elseBranch.setNextLine(parent.getNextLine() + 1);
			
			ifBranch.addConstraint(conditionStmt.toString());
			ifBranch.addCondition(new Condition(op, str2Var(lhs, parent.getLocalVars()), str2Var(rhs, parent.getLocalVars()), false));
			elseBranch.addConstraint("! " + conditionStmt.toString());
			elseBranch.addCondition(new Condition(op, str2Var(lhs, parent.getLocalVars()), str2Var(rhs, parent.getLocalVars()), true));
			
			if (unitIndexes.get(goto_target) <= parent.getNextLine()) ifBranch.setReturnFlag(true);
			
			try {
				if (new Z3Pipeline(ifBranch, mVarsType, mOption).getResult()) {
					returnList.add(ifBranch);
				} else {
					log("ifBranch discarded.");
				}
				if (new Z3Pipeline(elseBranch, mVarsType, mOption).getResult()) {
					returnList.add(elseBranch);
				} else {
					log("elseBranch discarded.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	 	}
		return returnList;
	}

	protected ExecutionTreeNode performGotoStmt(ExecutionTreeNode node, Unit u, Map<Unit,Integer> unitIndexes) {
		GotoStmt gt_st = (GotoStmt) u;
		Unit goto_target = gt_st.getTarget();
		
		if(unitIndexes.get(goto_target) > node.getNextLine()) {
		    log(Color.ANSI_GREEN + node.getNextLine() + " goto " + Color.ANSI_RESET + goto_target);
		    //node = new ExecutionTreeNode(null, st, 0, unitIndexes.get(goto_target), false);
		    node.setNextLine(unitIndexes.get(goto_target));
		}
		else {
			log(Color.ANSI_GREEN + node.getNextLine() + " goto " + Color.ANSI_RESET + unitIndexes.get(goto_target) + " (Loop back, terminate)");
			//node = new ExecutionTreeNode(null, st, 0, st.getCommandLineNo(), true);
		    node.setReturnFlag(true);
		}
		return node;
	}


	protected ExecutionTreeNode performVirtualInvoke(ExecutionTreeNode currentNode, UnitSet us) {
		
		// handling OutputCollector
		if(us.getUnit().toString().contains("OutputCollector") || us.getUnit().toString().contains("Context")) {
			String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
			String valueV = (us.getUnit().toString().split(">")[1]).split(",")[1];
			valueV = valueV.replace(")", "").replaceAll("\\s+", "");

			//newState.update(key, value);
			currentNode.setVar(key, str2Var(valueV, currentNode.getLocalVars()));
			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + key + " -> " + valueV);

			// Assign valueK and value from output.collect(valueK, value) to individual variables
			String valueK = (us.getUnit().toString().split(">")[1]).split(",")[0];
			valueK = valueK.replace("(", "").replaceAll("\\s+", "");

			String keyV = "outV" + currentNode.getNextLine();
			currentNode.setVar(keyV, str2Var(valueV, currentNode.getLocalVars()));
			mVarsType.put(keyV, "double");
			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + keyV + " -> " + valueV);

			if (mVarsType.containsKey(valueK)) {
				String varK = "outK" + currentNode.getNextLine();
				//value = valueReplace(valueK, lastEnv);
				//newState.update(varK, value);
				currentNode.setVar(varK, str2Var(valueK, currentNode.getLocalVars()));
				mVarsType.put(varK, mVarsType.get(valueK));
				log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + varK + " -> " + valueK);
			}
		// handling value set for ref of Writable
		} else if (us.getUnit().toString().contains("Writable") 
				&& us.getUnit().toString().contains("org.apache.hadoop.io")
				&& !us.getUnit().toString().contains("get")
				) {
			String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
			String value = us.getUnit().toString().split(">")[1];
			value = value.replace(")", "");
			value = value.replace("(", "");
			log(Color.ANSI_GREEN + "set: " + Color.ANSI_RESET + key + " -> " + value);

			currentNode.setRefVar(key, str2Var(value, currentNode.getLocalVars()));
		} else {
			String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
			String value = us.getUnit().toString().split(">")[1];
			value = value.replace(")", "");
			value = value.replace("(", "");

			if (value.length() == 0) {
				log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + key + " -> " + "remain");
			} else {
				log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + key + " -> " + value);
				//newState.update(key.toString() , value);
				currentNode.setVar(key, str2Var(value, currentNode.getLocalVars()));
			}
		}
		currentNode.setNextLine(currentNode.getNextLine() + 1);

		return currentNode;
	}
	
	protected ExecutionTreeNode performSpecialInvoke(ExecutionTreeNode currentNode, UnitSet us) {
		// specialinvoke $r5.<org.apache.hadoop.io.IntWritable: void <init>()>();
		currentNode.setNextLine(currentNode.getNextLine() + 1);
		if(us.getUnit().toString().contains("init")) {
			String key = (us.getUnit().toString().split("\\s+")[1]).split("\\.")[0];
			String value = us.getUnit().toString().split(">")[2];
			value = value.replace(")", "");
			value = value.replace("(", "");
			if (value.length() == 0) {
				value = "0";
			}
			
			log(Color.ANSI_GREEN + "assign: " + Color.ANSI_RESET + key + " -> " + value);
			
			currentNode.setVar(key, str2Var(value, currentNode.getLocalVars()));
			return currentNode;
		}				        
		return null;
	}

	public Map<String, String> getVarType() {
		return mVarsType;
	}
	
	public List<ExecutionTreeNode> getEndNodes() {
		return mEndNodes;
	}
	
	public void logAll(String str) {
		if (mOption.z3_mode) return;
		if (mOption.silence_flag) System.out.println("[  ETree]  " + str);
		else System.out.println(str);
	}
	
	public void log(String str) {
		if (mOption.z3_mode) return;
		if (!mOption.silence_flag) System.out.println(str);
	}

}
