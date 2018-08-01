package statementResolver.state;

import soot.Unit;

public class StateUnitPair {
	public State state;
	public Unit unit;
	
	public StateUnitPair() {
		this.state = null;
		this.unit = null;
	}
	public StateUnitPair(State s, Unit u) {
		this.state = s;
		this.unit = u;
	}
	public State getState() {
		return this.state;
	}
	public Unit getUnit() {
		return this.unit;
	}
}