package jRecover.optimize.state;

import java.util.HashMap;
import java.util.Map;

public class Variable {
	// Reference name of this variable
	protected String mName;	
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
	
	public Variable(String name, String initValue) {
		mName = new String(name);
		mIsBinary = false;
		mValue.put(initValue, 0);
	}
	
	public Variable(String name, boolean isBinary, String operator, Map<String, Integer> list) {
		mName = new String(name);
		mIsBinary = isBinary;
		mOperator = new String(operator);
		mValue.putAll(list);
	}
	
	public Variable(String name, boolean isBinary, String operator, Map<String, Integer> list, Map<String, Integer> listSub) {
		mName = new String(name);
		mIsBinary = isBinary;
		mOperator = new String(operator);
		mValue.putAll(list);
		mValueSub.putAll(listSub);
	}
	
	public Variable(Variable v) {
		mName = new String(v.getName());
		mValue.putAll(v.getValue());
		mValueSub.putAll(v.getValueSub());
		mIsBinary = v.isBinary();
		if (mIsBinary) mOperator = new String(v.getOperator());
	}
	
	public Variable clone(String name) {
		Variable v = new Variable(mName, mIsBinary, mOperator, mValue);
		return v;
	}
	
	public Variable add(String vname, Variable v1, Variable v2) {
		Variable result = new Variable(vname, false, "+", 
				addOrSubList(false, v1.getValue(), v2.getValue()));
		return result;
	}
	
	public Variable subtract(String vname, Variable v1, Variable v2) {
		Variable result = new Variable(vname, false, "+", 
				addOrSubList(true, v1.getValue(), v2.getValue()));
		return result;
	}
	
	public Variable multiple(String vname, Variable v1, Variable v2) {
		Variable result = new Variable(vname, true, "mul", v1.getValue(), v2.getValue());
		return result;
	}
	
	public Variable divide(String vname, Variable v1, Variable v2) {
		Variable result = new Variable(vname, true, "div", v1.getValue(), v2.getValue());
		return result;
	}
	
	public Variable remainder(String vname, Variable v1, Variable v2) {
		Variable result = new Variable(vname, true, "rem", v1.getValue(), v2.getValue());
		return result;
	}
	
	public Map<String, Integer> addOrSubList(boolean isMinus, 
			Map<String, Integer> addend, Map<String, Integer> augend) {
		Map<String, Integer> list = new HashMap<String, Integer>(addend);
		for (String augendKey : augend.keySet()) {
			int value = augend.get(augendKey);
			if (isMinus) value = value * -1;
			
			if (addend.containsKey(augendKey)) {
				addend.put(augendKey, addend.get(augendKey) + value);
			} else  {
				addend.put(augendKey, value);
			}
		}
		return list;
	}

	public Map<String, Integer> getValue() {
		return mValue;
	}

	public Map<String, Integer> getValueSub() {
		return mValueSub;
	}
	
	public String getName() {
		return mName;
	}
	
	public boolean isBinary() {
		return mIsBinary;
	}
	
	public String getOperator() {
		return mOperator;
	}
	
	
	public String toString() {
		StringBuffer result = new StringBuffer("{") ;
		result.append("Name: " + mName + ", ");
		if (mIsBinary) result.append("Operator: \t" + mOperator + ", ");
		result.append("[");
		for (String vname : mValue.keySet()) {
			result.append(vname + ": " + mValue.get(vname) + ",");
		}
		result.append("]}\n");

		return result.toString();
	}
}
