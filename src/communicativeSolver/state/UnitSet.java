package communicativeSolver.state;

import soot.Unit;

public class UnitSet {
	private Unit u;
	private int line;
	
	public UnitSet(Unit un, int num) {
		this.u = un;
		this.line = num;
	}
	
	public Unit getUnit() {
		return u;
	}
	
	public Integer getLine() {
		return line;
	}
	
}