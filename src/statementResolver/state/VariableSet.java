package statementResolver.state;

import java.util.LinkedHashMap;

public class VariableSet {
	private String value;
	private LinkedHashMap<String, Boolean> valueSet = new LinkedHashMap<String, Boolean>();
	private Type type = Type.UNKNOWN;
	private Boolean isList = false;
}
