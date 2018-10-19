package jRecover.optimize.state;

import java.util.HashMap;
import java.util.Map;

public class Variable {
	// Value of this variable
	protected Map<String, Integer> mValue = new HashMap<String, Integer>();
	// Exists when 'mIsBinary := true'
	protected Map<String, Integer> mValueSub = new HashMap<String, Integer>();
	
	/*
	 * Name of value:
	 * xx_v		-> initial symbolic value
	 * tmp_xx_t -> tmp variable with dual list
	 * others	-> global variable or number
	 */

	protected boolean mIsBinary;
	protected String mOperator = "";
	
	public Variable(String initValue) {
		mIsBinary = false;
		mValue.put(initValue, 1);
	}
	
	public Variable(boolean isBinary, String operator, Map<String, Integer> list) {
		mIsBinary = isBinary;
		mOperator = new String(operator);
		mValue.putAll(list);
	}
	
	public Variable(boolean isBinary, String operator, Map<String, Integer> list, Map<String, Integer> listSub) {
		mIsBinary = isBinary;
		mOperator = new String(operator);
		mValue.putAll(list);
		mValueSub.putAll(listSub);
	}
	
	public Variable(Variable v) {
		mValue.putAll(v.getValue());
		mValueSub.putAll(v.getValueSub());
		mIsBinary = v.isBinary();
		if (mIsBinary) mOperator = new String(v.getOperator());
	}
	
	
	public Variable addVariable(Variable v1, Variable v2) {
		Variable result = new Variable(false, "+", 
				addOrSubList(false, v1.getValue(), v2.getValue()));
		return result;
	}
	
	public Variable subtractVariable(Variable v1, Variable v2) {
		Variable result = new Variable(false, "+", 
				addOrSubList(true, v1.getValue(), v2.getValue()));
		return result;
	}
	
	public Variable multipleVariable(Variable v1, Variable v2) {
		Variable result = new Variable(true, "mul", v1.getValue(), v2.getValue());
		return result;
	}
	
	public Variable divideVariable(Variable v1, Variable v2) {
		Variable result = new Variable(true, "div", v1.getValue(), v2.getValue());
		return result;
	}
	
	public Variable remainderVariable(Variable v1, Variable v2) {
		Variable result = new Variable(true, "rem", v1.getValue(), v2.getValue());
		return result;
	}
	
	public Map<String, Integer> addOrSubList(boolean isMinus, 
			Map<String, Integer> addend, Map<String, Integer> augend) {
		Map<String, Integer> result = new HashMap<String, Integer>(addend);
		for (String augendKey : augend.keySet()) {
			int value = augend.get(augendKey);
			if (isMinus) value = value * -1;
			
			if (result.containsKey(augendKey)) {
				result.put(augendKey, addend.get(augendKey) + value);
			} else  {
				result.put(augendKey, value);
			}
		}
		return result;
	}

	public Map<String, Integer> getValue() {
		return mValue;
	}

	public Map<String, Integer> getValueSub() {
		return mValueSub;
	}
	
	public boolean isBinary() {
		return mIsBinary;
	}
	
	public String getOperator() {
		return mOperator;
	}
	
	
	public String toString() {
		StringBuffer result = new StringBuffer("{") ;
		if (mIsBinary) result.append("Operator: \t" + mOperator + ", ");
		result.append("[");
		for (String vname : mValue.keySet()) {
			result.append(vname + ": " + mValue.get(vname) + ",");
		}
		if (mIsBinary) {
		result.append("[");
			for (String vname : mValueSub.keySet()) {
				result.append(vname + ": " + mValueSub.get(vname) + ",");
			}
		}
		result.append("]}");

		return result.toString();
	}
}
