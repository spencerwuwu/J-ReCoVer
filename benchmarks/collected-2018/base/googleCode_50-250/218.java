// https://searchcode.com/api/result/12093009/

/*
 * TranGate.java
 * 
 * last update: 16.01.2010 by Stefan Saru
 * 
 * author:	Alec(panovici@elcom.pub.ro)
 * 
 * Obs:
 */
package engine;

import java.util.*;
import java.io.*;
import middle.*;

/**
 * ..
 */

class TranGate extends StdGate{

	BitVector lastOutput[];
	int lastControl;
	byte lastStrength[][];
	WireSelection port1, port2;
	Expression control;
	byte reduceTable[];
	boolean open;
	int enableCommand;

	TranGate(NameSpace parent,
			WireSelection port1, WireSelection port2,
			ModuleInstanceDescription id, byte type)
			throws ParseException
			{
		this(parent, null, port1, port2,
				null, id, type);
			}

	TranGate(NameSpace parent, Delay3 delay,
			WireSelection port1, WireSelection port2,
			Expression control, 
			ModuleInstanceDescription id, byte type)
			throws ParseException
			{
		super(parent, delay, type, id);
		this.port1 = port1;
		port1.release();
		this.port2 = port2;
		port2.release();
		this.control = control;

		Monitor m;
		m = new HalfMonitor(port1, this, 0);
		m = new HalfMonitor(port2, this, 1);
		if (control != null)
			m = new HalfMonitor(control, this, 2);
		xConsole.debug("type: " + type);
		reduceTable = ((type == StdGateDescription.rtran) ||
				(type == StdGateDescription.rtranif1) ||
				(type == StdGateDescription.rtranif0)) ? Strength.reduceTable1 :
					Strength.reduceTable2;

		enableCommand = (type == StdGateDescription.tranif1) ||
		(type == StdGateDescription.rtranif1) ? 1 : 0;

		lastUpdateTime = -1;

		if (control != null) {
			try {
				lastControl = control.evaluate().getBits().getb(0); 
			} catch (InterpretTimeException itex) {
				throw new ParseException (itex.toString());
			}
			open = lastControl == enableCommand;
		} else {
			open = true;
		}

		lastStrength = new byte[2][1];
		lastStrength[0][0] = lastStrength[1][0] = (byte)0xff;
		lastOutput = new BitVector[2];
		lastOutput[0] = lastOutput[1]  = BitVector.bX();
			}

	/**
	 * Makes the value from in to pass to out
	 * for the next scheduled update.
	 */
	void pass(WireSelection out, WireSelection in, int direction)
	throws InterpretTimeException
	{
		BitVector curValue = in.evaluate().getBits();
		byte curStrength;

		try {
			curStrength = (byte) in.getStrength(0);
		} catch (ClassCastException cex) {
			curStrength = Strength.defaultStrength;
		}

		xConsole.debug("strength: " + Integer.toHexString(curStrength));
		curStrength = Strength.reduce(curStrength, reduceTable);
		xConsole.debug("reduced strength: " + Integer.toHexString(curStrength));

		if (!curValue.equals(lastOutput[direction]) ||
				(curStrength != lastStrength[direction][0])) {
			lastStrength[direction][0] = curStrength;
			out.setStrengths(lastStrength[direction], 0);
			out.reatach();
			try {
				out.assign(lastOutput[direction] = curValue);
			} catch (InterpretTimeException itex) {
				xConsole.dumpStack(itex);
				throw itex;
			}
			out.release();
		}
	}

	public void trigger(int hint)throws InterpretTimeException {
		xConsole.debug("Tran: trigger: " + " hint: " + hint + "\n");
		switch(hint) {
		case 0: //pass from port1 to port2
			if (!open) return;
			pass(port2, port1, 0);
			break;
		case 1: //pass from port2 to port1
			if (!open) return;
			pass(port1, port2, 1);
			break;
		case 2: //control line has changed
			boolean newState =
				control.evaluate().getBits().getb(0) == enableCommand;
			xConsole.debug("new state: " + newState + "\n");
			if (newState != open) {
				open = newState;
				if (newState) {
					xConsole.debug("opening tran\n");
					pass(port2, port1, 0);
					pass(port1, port2, 1);
				} else {
					xConsole.debug("closing tran\n");
					port1.assign(lastOutput[1]);
					port2.assign(lastOutput[0]);
					return;
				}
			}
		}
	}
}

