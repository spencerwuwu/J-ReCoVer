package statementResolver.state;

import soot.Unit;

public class UnitSet {
	Unit u;
	int line;
	
	public UnitSet(Unit un, int num) {
		this.u = un;
		this.line = num;
	}
	
	public Unit getUnit() {
		return this.u;
	}
	
	public Integer getLine() {
		return this.line;
	}
	
}