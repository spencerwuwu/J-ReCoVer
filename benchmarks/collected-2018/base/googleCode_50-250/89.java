// https://searchcode.com/api/result/12092992/

/*
 * MContAssignMonitor.java
 * 
 * last update: 16.01.2010 by Stefan Saru
 * 
 * author:	Alec(panovici@elcom.pub.ro)
 * 
 * Obs:
 */

package engine;

/**
 * Used for internal linking between wirez.
 * It propagates strength and cannot be delayed
 * (i.e. acts like a tran/2 ).
 */
class MContAssignMonitor implements Monitor, Executable{

	LeftValue[] lValue;
	Expression[] rValue;
	BitVector prevValue, curValue;
	byte[] newStrengths;
	boolean active;
	int length;

	/**
	 * If this monitor should reduce strengths,
	 * this contains the reducing table
	 */
	byte [] reduceTable;

	static MContAssignMonitor createNewMContAssignMonitor(LeftValue []lValue,
			Expression rValue) {
		Expression r[] = {rValue};
		return new MContAssignMonitor(lValue, r);
	}

	static MContAssignMonitor createNewMContAssignMonitor(LeftValue lValue,
			Expression[] rValue) {
		LeftValue l[] = {lValue};
		return new MContAssignMonitor(l, rValue);
	}

	static MContAssignMonitor createNewMContAssignMonitor (LeftValue lValue,
			Expression rValue) {
		LeftValue l[] = {lValue};
		Expression r[] = {rValue};
		return new MContAssignMonitor(l, r);
	}

	MContAssignMonitor (LeftValue[] lValue,
			Expression[] rValue) {
		xConsole.debug("MContAssignMonitor: " + lValue + " <- " + rValue);
		active = false;
		this.lValue = lValue;
		this.rValue = rValue;
		length = 0;
		curValue = prevValue = BitVector.bX();
		for (int i = 0, n = rValue.length ;i < n; i++){
			rValue[i].addMonitor(this);
			length += rValue[i].length;
		}
		reduceTable = null;
		newStrengths = new byte[length];
	}

	/**
	 * Sets the strength reduction table
	 */
	void setReduceTable(byte table[]) {
		reduceTable = table;
	}

	/**
	 * Note:
	 * If triggered multiple times per time-unit, it will execute
	 * only once, assigning the last update value.
	 */
	public synchronized void trigger()
	throws InterpretTimeException {
		xConsole.debug("MContAssign.trigger->");
		curValue = new BitVector(length-1, 0);
		int curStrengthIndex = 0;
		byte[] oldStrengths = (byte []) newStrengths.clone();
		for (int i = 0, n = rValue.length, bit = length-1 ; i < n ; i++) {
			BitVector b = rValue[i].evaluate().getBits();
			try {
				((WireSelection) rValue[i]).getStrengths(newStrengths,
						curStrengthIndex);
			} catch(ClassCastException cex) {
				byte ds = Strength.defaultStrength;
				for(int nn = curStrengthIndex + rValue[i].length;
				curStrengthIndex < nn; curStrengthIndex++)
					newStrengths[curStrengthIndex] = ds; 
			}
			int msb = bit;
			bit -= b.n;
			curValue.attrib(b, msb, bit +1);
		}
		xConsole.debug("curValue is: " + curValue);
		if (xConsole.__debug__) {
			for(int j = 0; j < length; j++)
				xConsole.debug(Strength.strengthToString(newStrengths[j],
						curValue.getb(j)) + ", ");
			xConsole.debug("\b ]");
		}

		if (reduceTable != null) {  //perform strength reduction if needed
			for (int i = 0; i < length ; i++)
				newStrengths[i] = Strength.reduce(newStrengths[i],
						reduceTable);
		}

		boolean strengthsChanged = false;
		for(int i = 0; i < length; i++)
			if (oldStrengths[i] != newStrengths[i]) {
				strengthsChanged = true;
				break;
			}

		if (!curValue.equals(prevValue) || strengthsChanged) {

			prevValue = curValue;
			if (!active) { //group as many modifications as
				//we can per single execution 
				xConsole.trace("triggering continuous assignement" +
						toString(lValue) + " <<< " + curValue);
				Time.addFinisher(0, new GenericInstruction(this));
				active = true;
			}
		}
		xConsole.debug("<-MContAssign.trigger");
	}

	String toString( Object [] lValue) {
		String s = "{";
		for ( int i = 0, n = lValue.length;i < n; i++) {
			s += lValue[i];
			if (i < n-1) s += ", ";
		}
		return s + "}";
	}

	public void execute ()
	throws InterpretTimeException
	{
		xConsole.debug("MContAssign.attrib:");
		xConsole.debug(toString() +  " <<< " + curValue);
		for (int i = 0, n = lValue.length, bit = length-1 , strengthIndex = 0; i < n ; i++) {
			int msb = bit;
			int len = lValue[i].length();
			bit -= len;
			((WireSelection)lValue[i]).setStrengths(newStrengths, strengthIndex);
			lValue[i].assign(new BitVector(curValue, msb, bit + 1));
			strengthIndex += len;
		}
		active = false;
	}

	public String toString() {
		return toString(lValue) + "<=" + toString(rValue);
	}
}









