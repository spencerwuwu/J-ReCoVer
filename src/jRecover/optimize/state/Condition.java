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
}
