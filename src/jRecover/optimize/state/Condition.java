package jRecover.optimize.state;

public class Condition {
	private String mCmp;
	private Variable mLhs;
	private Variable mRhs;
	private boolean mNegative;
	
	
	public Condition(String op, Variable lhs, Variable rhs, boolean negative) {
		mCmp = op;
		mLhs = lhs;
		mRhs = rhs;
		mNegative = negative;
	}

	public String getFormula(int stage, int round) {
		StringBuffer lhs = mLhs.getFormula(stage, round);
		StringBuffer rhs = mRhs.getFormula(stage, round);
		StringBuffer formula = new StringBuffer("");

		if (mCmp.contains("!=")) {
			formula.append("(not (= ").append(lhs).append(" ").append(rhs).append("))");
		} else if (mCmp.contains("==")) {
			formula.append("(= ").append(lhs).append(" ").append(rhs).append(")");
		} else {
			formula.append("(" + mCmp + " ").append(lhs).append(" ").append(rhs).append(")");
		}
		
		if (mNegative) formula.insert(0, "(not ").append(")");
		return formula.toString();
	}
	
	public String getFormula() {
		StringBuffer lhs = mLhs.getFormula();
		StringBuffer rhs = mRhs.getFormula();
		StringBuffer formula = new StringBuffer("");

		if (mCmp.contains("!=")) {
			formula.append("(not (= ").append(lhs).append(" ").append(rhs).append("))");
		} else if (mCmp.contains("==")) {
			formula.append("(= ").append(lhs).append(" ").append(rhs).append(")");
		} else {
			formula.append("(" + mCmp + " ").append(lhs).append(" ").append(rhs).append(")");
		}
		
		if (mNegative) formula.insert(0, "(not ").append(")");
		return formula.toString();
	}
}
