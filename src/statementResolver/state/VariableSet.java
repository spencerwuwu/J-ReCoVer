package statementResolver.state;

import java.util.LinkedHashMap;

public class VariableSet {
	private String mVarValue;
	private String mTypeValue;
	public Type mType = Type.UNKNOWN;
	private Boolean isList = false;

	public VariableSet() {
		mVarValue = "";
		mTypeValue = "";
	}

	public VariableSet(String value, String type) {
		mVarValue = value;
		mTypeValue = type;
		if (type.contains("int") | type.contains("byte") | type.contains("short") | type.contains("long")) {
			mType = Type.INT;
		} else if (type.contains("float") | type.contains("double")) {
			mType = Type.REAL;
		} else if (type.contains("boolean")) {
			mType = Type.BOOL;
		}
	}

	public String getValue() {
		return mVarValue;
	}

	public String getType() {
		return mTypeValue;
	}
}
