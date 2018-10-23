package jRecover.stringBased.state;

public class Variable {
	private StringBuffer mFormula;
	
	public Variable(StringBuffer var) {
		mFormula = new StringBuffer(var);
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

}
