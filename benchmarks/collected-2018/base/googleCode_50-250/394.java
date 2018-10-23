// https://searchcode.com/api/result/12092921/

/*
 * MBiContAssignMonitor.java
 * 
 * last update: 16.01.2010 by Stefan Saru
 * 
 * author:	Alec(panovici@elcom.pub.ro)
 * 
 * Obs:
 */
package engine;

/**
 * This is a half of a bidirectional continuous assign monitor.
 * It's used for inout ports (what else ??). Tha behaviour is
 * as if the wirez were connected through a tran (but this is
 * slightly faster than a bung on tran's for every port).
 * A complete inout assignement is made of a pair of MBiContAssign...
 * each acting in the other's opposite direction
 */
class MBiContAssignMonitor implements Monitor{

	WireSelection lValue[], rValue[];
	BitVector prevValue;
	byte[] prevStrengths;
	int length;

	/**
	 * The reduction is as if it were a tran:
	 */
	static byte [] reduceTable = Strength.reduceTable2;

	static MBiContAssignMonitor createNewMBiContAssignMonitor
	( WireSelection []lValue, WireSelection rValue)
	throws ParseException
	{
		WireSelection r[] = {rValue};
		return new MBiContAssignMonitor(lValue, r);
	}

	static MBiContAssignMonitor createNewMBiContAssignMonitor
	( WireSelection lValue, WireSelection[] rValue)
	throws ParseException
	{
		WireSelection l[] = {lValue};
		return new MBiContAssignMonitor(l, rValue);
	}

	static MBiContAssignMonitor createNewMBiContAssignMonitor
	( WireSelection lValue, WireSelection rValue)
	throws ParseException
	{
		WireSelection l[] = {lValue};
		WireSelection r[] = {rValue};
		return new MBiContAssignMonitor(l, r);
	}

	MBiContAssignMonitor (WireSelection [] lValue,
			WireSelection[] rValue)
			throws ParseException 
			{
		xConsole.debug("MBiContAssignMonitor: " + lValue + " <- " + rValue);
		this.lValue = lValue;
		this.rValue = rValue;
		length = 0;
		prevValue = BitVector.bX();

		for (int i = 0, n = rValue.length ;i < n; i++){
			rValue[i].addMonitor(this);
			length += rValue[i].length;
		}
		int length1 = 0;
		for (int i = 0, n = lValue.length; i < n; i++) {
			length1 += lValue[i].length;
			lValue[i].release();
		}
		if (length != length1)
			throw new ParseException("wrong expression lenghs");

		prevStrengths = new byte[length];
			}

	public synchronized void trigger() throws InterpretTimeException
	{
		xConsole.debug("MBiContAssign.trigger->");
		BitVector curValue = new BitVector(length-1, 0);
		int curStrengthIndex = 0;
		byte[] curStrengths = new byte[length];
		for (int i = 0, n = rValue.length, bit = length-1 ; i < n ; i++) {
			BitVector b = rValue[i].evaluate().getBits();
			rValue[i].getStrengths(curStrengths,
					curStrengthIndex);
			int msb = bit;
			bit -= b.n;
			curValue.attrib(b, msb, bit +1);
		}

		xConsole.debug("curValue is: " + curValue);
		if (xConsole.__debug__) {
			for(int j = 0; j < length; j++)
				xConsole.debug(Strength.strengthToString(curStrengths[j],
						curValue.getb(j)) + ", ");
			xConsole.debug("\b ]");
		}

		for (int i = 0; i < length ; i++)
			curStrengths[i] = Strength.reduce(curStrengths[i],
					reduceTable);

		boolean strengthsChanged = false;
		for(int i = 0; i < length; i++)
			if (prevStrengths[i] != curStrengths[i]) {
				strengthsChanged = true;
				break;
			}

		if (!curValue.equals(prevValue) || strengthsChanged) {

			prevValue = curValue;
			prevStrengths = curStrengths;

			int n = lValue.length;
			for (int i = 0; i < n; i++)
				lValue[i].reatach();

			for (int i = 0, bit = length-1 , strengthIndex = 0; i < n ; i++) {
				int msb = bit;
				int len = lValue[i].length();
				bit -= len;
				((WireSelection)lValue[i]).setStrengths(curStrengths, strengthIndex);
				lValue[i].assign(new BitVector(curValue, msb, bit + 1));
				strengthIndex += len;
			}

			for (int i = 0; i < n; i++)
				lValue[i].release();
		}
		xConsole.debug("<-MBiContAssign.trigger");
	}

	String toString( Object [] lValue) {
		String s = "{";
		for ( int i = 0, n = lValue.length;i < n; i++) {
			s += lValue[i];
			if (i < n-1) s += ", ";
		}
		return s + "}";
	}

	public String toString() {
		return toString(lValue) + "<=" + toString(rValue);
	}
}









