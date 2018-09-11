package statementResolver.state;

public class VariableSet {
	private Type mType = Type.UNKNOWN;
	private String mVarType;		// Type in String
	private String mLocalVar;		// Name + "_v"
	private boolean mIsNull = true;
	
	public VariableSet(String varType, String localVar) {
		mVarType = varType;
		mLocalVar = localVar;
		determineType(varType);
	}
	
	private void determineType(String varType) {
		if (varType.contains("boolean")) {
			mType = Type.BOOL;
		} else if (varType.contains("LongWritable") || varType.contains("long")
				|| varType.contains("IntWritable") || varType.contains("int")) {
			mType = Type.INT;
		} else if (varType.contains("double") || varType.contains("float")) {
			mType = Type.REAL;
		}
	}
	
	public String typeToString() {
		switch(mType) {
		case BOOL:
			return "boolean";
		case INT:
			return "int";
		case REAL:
			return "real";
		case UNKNOWN:
		default:
			return "unknown";
		}
	}
	
	public Type getType() {
		return mType;
	}
	
	public String getLocalVar() {
		return mLocalVar;
	}
}
