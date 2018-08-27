package statementResolver.state;

import soot.Unit;

public class UnitSet {
	Unit u;
	int line;
	
	public UnitSet(Unit un, int num) {
		this.u = un;
		this.line = num;
	}
	
	public Unit get_unit() {
		return this.u;
	}
	
	public Integer get_line() {
		return this.line;
	}
	
}