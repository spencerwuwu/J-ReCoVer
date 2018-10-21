package jRecover.optimize.state;

public class Condition {
	private String mCmp;
	private Variable mLhs;
	private Variable mRhs;
	private boolean mNegative;
	
	
	public Condition(String op, Variable lhs, Variable rhs, boolean negative) {
		mCmp = new String(op);
		mLhs = new Variable(lhs);
		mRhs = new Variable(rhs);
		mNegative = negative;
	}
	
	public StringBuffer getFormula(int stage, int round) {
		StringBuffer lhs = mLhs.getFormula(stage, round);
		StringBuffer rhs = mRhs.getFormula(stage, round);
		StringBuffer formula = new StringBuffer("");
		
		if (mCmp.equals("!=")) {
			formula.append("(not (= ").append(lhs).append(rhs).append("))");
		} else if (mCmp.equals("==")) {
			formula.append("(= ").append(lhs).append(rhs).append(")");
		} else {
			formula.append("(" + mCmp + " ").append(lhs).append(rhs).append(")");
		}
		
		if (mNegative) formula.insert(0, "(not ").append(")");
		
		return formula;
	}
}
