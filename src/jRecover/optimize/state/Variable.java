package jRecover.optimize.state;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Variable {
	// Value of this variable
	protected Map<String, Integer> mValue = new HashMap<String, Integer>();
	
	/*
	 * Name of value:
	 * xx_v		-> symbolic value
	 * others	-> number or formula
	 */
	
	public Variable(String initValue) {
		if (isNumber(initValue)) {
			if (isInteger(initValue))
				mValue.put("1", Integer.parseInt(initValue));
			else 
				mValue.put(initValue, 1);
		} else {
			if (initValue.length() != 0)
				mValue.put(initValue + "_v", 1);
			else
				mValue.put("0", 0);
		}
	}
	
	// Constructor for number
	public Variable(String value, boolean isInteger) {
		if (isInteger) {
			mValue.put("1", Integer.parseInt(value));
		} else {
			mValue.put(value, 1);
		}
	}
	
	public Variable(Map<String, Integer> list) {
		if (list != null) mValue.putAll(list);
	}
	public Variable(Variable v) {
		mValue.putAll(v.getValue());
	}

	public void updateAs(Variable v) {
		mValue.clear();
		mValue.putAll(v.getValue());
	}
	
	
	public Variable(String operator, Variable v1, Variable v2) {
		if (operator == "+") {
			mValue.putAll(v1.getValue());
			for (String key : v2.getValue().keySet()) {
				if (mValue.containsKey(key)) {
					int value = mValue.get(key) + v2.getValue().get(key);
					mValue.put(key, value);
				} else {
					int value = v2.getValue().get(key);
					mValue.put(key, value);
				}
			}
		} else if (operator == "-"){
			mValue.putAll(v1.getValue());
			for (String key : v2.getValue().keySet()) {
				if (mValue.containsKey(key)) {
					int value = mValue.get(key) - v2.getValue().get(key);
					mValue.put(key, value);
				} else {
					int value = -v2.getValue().get(key);
					mValue.put(key, value);
				}
			}
		} else if (operator == "*"){
			StringBuffer lhs = v1.getFormula();
			StringBuffer rhs = v2.getFormula();
			if (isInteger(lhs.toString())) {
				int mulend = Integer.parseInt(lhs.toString());
				for (String key : v2.getValue().keySet()) {
					mValue.put(key, v2.getValue().get(key) * mulend);
				}
			} else if (isInteger(rhs.toString())) {
				int mulend = Integer.parseInt(rhs.toString());
				for (String key : v1.getValue().keySet()) {
					mValue.put(key, v1.getValue().get(key) * mulend);
				}
			} else {
				mValue.put(lhs.insert(0, "(* ").append(" ").append(rhs).append(")").toString(), 1);
			}
		} else {
			StringBuffer lhs = v1.getFormula();
			StringBuffer rhs = v2.getFormula();
			lhs.insert(0, "(" + operator + " ").append(" ").append(rhs).append(")");
			mValue.put(lhs.toString(), 1);
		} 
	}

	public Map<String, Integer> getValue() {
		return mValue;
	}
 	
	public StringBuffer map2String(Map<String, Integer> list, int stage, int round) {
		StringBuffer var = new StringBuffer("");
		for (String key : list.keySet()) {
			if (list.get(key) == 0) continue;
			else {
				int value = list.get(key);
				if (!isNumber(key)
						&& !key.contains("hasNext")) {
					if (key.contains("_v")) key = key.replace("_v", "_" + stage + "_r" + round);
					else key = key + "_" + stage + "_r" + round;
				}
				if (var.length() == 0) {
					if (value == 1)
						var.append(key);
					else
						var.insert(0, "(* ").append(value).append(" " + key + ")");
				} else {
					if (value == 1)
						var.insert(0, "(+ ").append(" " + key + ")");
					else
						var.insert(0, "(+ ").append(" (* ").append(value).append(" " + key + "))");
				}
			}
		}
		if (var.length() == 0) var.append("0");
		
		return var;
	}

	public StringBuffer getFormula(int stage, int round) {
		return map2String(mValue, stage, round);
	}

	protected StringBuffer getFormula() {
		StringBuffer var = new StringBuffer("");
		for (String key : mValue.keySet()) {
			if (mValue.get(key) == 0) continue;
			else {
				int value = mValue.get(key);
				if (var.length() == 0) {
					if (value == 1)
						var.append(key);
					else
						var.insert(0, "(* ").append(value).append(" " + key + ")");
				} else {
					if (value == 1)
						var.insert(0, "(+ ").append(" " + key + ")");
					else
						var.insert(0, "(+ ").append(" (* ").append(value).append(" " + key + "))");
				}
			}
		}
		if (var.length() == 0) var.append("0");
		
		return var;
	}
	

	protected boolean isNumber(String value) {
		if (value == null || value.length() == 0) return false;
		if (value.length() > 10) {
			char a = value.charAt(2);
			if ((a > '9' || a < '0') && a != '.') return false;
		}
		Pattern p = Pattern.compile("^-?[0-9]+(\\.[0-9]+)?$");
		Matcher m = p.matcher(value);
		if (m.find()) {
			return true;
		}
		return false;
	}
	
	protected boolean isInteger(String value) {
		if (value == null || value.length() == 0 || value.length() >= 9) return false;
		if (value.length() > 10) {
			char a = value.charAt(2);
			if ((a > '9' || a < '0') && a != '.') return false;
		}
		Pattern p = Pattern.compile("^-?[0-9]*$");
		Matcher m = p.matcher(value);
		if (m.find()) {
			return true;
		}
		return false;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("{") ;
		result.append("[");
		for (String vname : mValue.keySet()) {
			result.append("'" + vname + "': " + mValue.get(vname) + ",");
		}
		result.append("]}");

		return result.toString();
	}
}
