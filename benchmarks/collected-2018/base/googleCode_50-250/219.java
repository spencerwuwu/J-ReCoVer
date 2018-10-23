// https://searchcode.com/api/result/12093024/

/*
 * MOSGate.java
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
 * guess..
 */

class MOSGate extends StdGate implements Monitor, Executable{

	BitVector lastInput, lastOutput;
	byte lastStrength[];
	Expression input;
	LeftValue output;
	byte strength;

	/**
	 * true if this is an r?mos
	 */
	boolean reduceStrength;

	static final byte strengthTable[] =
	{0, 0, 1, 1,
		0, 0, 2, 2,
		0, 0, 0, 0,
		0, 0, 0, 0
	};

	MOSGate(NameSpace parent, Delay3 delay,
			LeftValue output, Expression input,
			ModuleInstanceDescription id, byte type)
			throws ParseException
			{
		super(parent, delay, type, id);
		this.output = output;
		this.input = input;
		input.addMonitor(this);
		reduceStrength = (type == StdGateDescription.rnmos) ||
		(type == StdGateDescription.rpmos);
		lastUpdateTime = -1;
		try {
			lastInput = input.evaluate().getBits();
		} catch (InterpretTimeException itex) {
			throw new ParseException (itex.toString());
		}
		lastStrength = new byte[1];
		lastStrength[0] = (byte)0xff;
		lastOutput = BitVector.bZ();
			}

	public void trigger()throws InterpretTimeException {

		BitVector curInput = input.evaluate().getBits();
		xConsole.debug("MOS: trigger: lastInput: " + lastInput +
				" curInput: " + curInput);
		lastInput = curInput;    
		BitVector curOutput = (BitVector) curInput.duplicate();
		curOutput.genericReduction1(truthTables[type]);

		byte curStrength;
		try {
			curStrength = (byte)((WireSelection)input).getStrength(1);
		} catch (ClassCastException cex) {
			curStrength = Strength.defaultStrength;
		}

		xConsole.debug("strength: " + Integer.toHexString(curStrength));

		curStrength = Strength.reduce(curStrength,
				reduceStrength ? Strength.reduceTable1 :
					Strength.reduceTable2);

		xConsole.debug("in: " + ((curInput.getb(1) <<2) | curInput.getb(0)) +
				" curStrength: " + Integer.toHexString(curStrength) + '\n');
		switch (strengthTable[(curInput.getb(1) <<2) | curInput.getb(0)]) {
		case 1: //L
			curStrength =
				Strength.getStrength(Strength.highz,
						Strength.getMax0Strength(curStrength));
			break;
		case 2: //H
			curStrength =
				Strength.getStrength(Strength.highz,
						Strength.getMax1Strength(curStrength));
		}

		xConsole.debug("output: " +
				Strength.strengthToString(curStrength,
						curOutput.getb(0)));
		if (curOutput.equals(lastOutput) &&
				(curStrength == lastStrength[0]))
			return;
		lastStrength[0] = curStrength;
		int iDelay = Wire.computeTransitionDelayFor(lastOutput, curOutput, delay);
		lastOutput = curOutput;

		lastUpdateTime = Time.oClock() + iDelay;
		Time.addFinisher(iDelay, new GenericInstruction(this));
	}

	public void execute ()throws InterpretTimeException {
		if (lastUpdateTime < Time.oClock()) return; //not now
		xConsole.trace(this + ": " + output + " <<< " + lastOutput);
		((WireSelection)output).setStrengths(lastStrength, 0);
		output.assign(lastOutput);
	}
}




