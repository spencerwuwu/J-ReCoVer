package jRecover.stringBased.state;

public class Variable {
	private StringBuffer mFormula;
	
	public Variable(StringBuffer var) {
		mFormula = new StringBuffer(var);
	}

	public Variable(Variable v) {
		mFormula = new StringBuffer(v.getFormula());
	}
	
	public Variable(String var) {
		mFormula = new StringBuffer(var);
	}
	
	public void updateAs(Variable v) {
		mFormula = new StringBuffer(v.getFormula());
	}
	
	public StringBuffer getFormula() {
		return mFormula;
	}

	public Variable addVariable(Variable lhsV, Variable rhsV) {
		StringBuffer result = new StringBuffer(lhsV.getFormula());
		result.insert(0, "(+ ").append(" ").append(rhsV.getFormula()).append(")");
		return new Variable(result);
	}

	public Variable subtractVariable(Variable lhsV, Variable rhsV) {
		StringBuffer result = new StringBuffer(lhsV.getFormula());
		result.insert(0, "(- ").append(" ").append(rhsV.getFormula()).append(")");
		return new Variable(result);
	}

	public Variable multipleVariable(Variable lhsV, Variable rhsV) {
		StringBuffer result = new StringBuffer(lhsV.getFormula());
		result.insert(0, "(* ").append(" ").append(rhsV.getFormula()).append(")");
		return new Variable(result);
	}

	public Variable divideVariable(Variable lhsV, Variable rhsV) {
		StringBuffer result = new StringBuffer(lhsV.getFormula());
		result.insert(0, "(div ").append(" ").append(rhsV.getFormula()).append(")");
		return new Variable(result);
	}

	public Variable remainderVariable(Variable lhsV, Variable rhsV) {
		StringBuffer result = new StringBuffer(lhsV.getFormula());
		result.insert(0, "(rem ").append(" ").append(rhsV.getFormula()).append(")");
		return new Variable(result);
	}
	
	public Variable minMaxVariable(Variable lhsV, Variable rhsV, boolean isMax) {
		StringBuffer lhs = lhsV.getFormula();
		StringBuffer rhs = rhsV.getFormula();
		StringBuffer result = new StringBuffer("(ite (");
		if (isMax) {
			result.append(lhs).append("> ");
		} else {
			result.append(lhs).append("< ");
		}
		result.append(lhs).append(" ").append(rhs).append(") ").append(lhs).append(" ").append(rhs).append(")"); 
		
		return new Variable(result);
	}
	
	public String toString() {
		return mFormula.toString();
	}

}
