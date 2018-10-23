// https://searchcode.com/api/result/130008941/

/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at usr/src/OPENSOLARIS.LICENSE
 * or http://www.opensolaris.org/os/licensing.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at usr/src/OPENSOLARIS.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright 2009 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */

package com.sun.jist.scsi.test;
import com.sun.jist.JISTData;
import com.sun.jist.JISTEnv;
import com.sun.jist.JISTLogic;
import com.sun.jist.scsi.SCSIData;
import com.sun.jist.scsi.test.SPCxxStress;
import java.io.BufferedWriter;

/**
 * JIST - ANSI T10 SCSI OPcode 1d "SEND DIAGNOSTIC" Stress Test Branch.
 * <p>
 * This class validates the following standards:
 * <ul>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Architecture Model 4</a>,
 *     (SAM-4), Revision 10, 22Mar2007.</li>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Architecture Model 3</a>,
 *     (SAM-3), Revision 14, 21Sep2004.</li>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Architecture Model 2</a>,
 *     (SAM-2), Revision 24, 12Sep2002.</li>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Architecture Model</a>,
 *     (SAM), Revision 18, 17Nov1995.</li>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Primary Commands 4</a>,
 *     (SPC-4), Revision 10, 21Apr2007.</li>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Primary Commands 3</a>,
 *     (SPC-3), Revision 23, 4May2005.</li>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Primary Commands 2</a>,
 *     (SPC-2), Revision 20, 18Jul2001.</li>
 * <li><a href="http://t10.org">ANSI T10 SCSI-3 Primary Commands</a>,
 *     (SPC), Revision 11a, 28Mar1997.</li>
 * <li><a href="http://t10.org">ANSI T10 Small Computer System Interface 2</a>,
 *     (SCSI-2), Revision 10L, 07Sep1993.</li>
 * </ul>
 * <p>
 * @author	Joel.Buckley@Sun.COM
 * @since	5.0
 */
public class SPC1dStress extends SPCxxStress {

/** Declare & Initialize "public final static" Constants. */

/**
 * SEND DIAGNOSTICS Command Descriptor Block & Data Buffer pairs
 * specific to Sun StorEdge 3510/3511 product family.
 */
private final static byte[][][] cdbdata = new byte[][][]
    {{{(byte)0x01d, (byte)0x010, (byte)0x000, (byte)0x000,
    (byte)0x030, (byte)0x000},
    {(byte)0x0c1, (byte)0x000, (byte)0x000, (byte)0x02c,
    (byte)0x001, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x000, (byte)0x000, (byte)0x018, (byte)0x000,
    (byte)0x001, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x000, (byte)0x00c, (byte)0x000, (byte)0x000,
    (byte)0x010, (byte)0x000, (byte)0x00a, (byte)0x020,
    (byte)0x0d0, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x0be, (byte)0x00e, (byte)0x000, (byte)0x000,
    (byte)0x03c, (byte)0x002, (byte)0x0b1, (byte)0x000,
    (byte)0x01f, (byte)0x030, (byte)0x000, (byte)0x000,
    (byte)0x0d0, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x000, (byte)0x000, (byte)0x000, (byte)0x000}},

    {{(byte)0x01d, (byte)0x010, (byte)0x000, (byte)0x000,
    (byte)0x020, (byte)0x000},
    {(byte)0x0c1, (byte)0x000, (byte)0x000, (byte)0x01c,
    (byte)0x001, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x000, (byte)0x000, (byte)0x018, (byte)0x000,
    (byte)0x003, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x001, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x0ca, (byte)0x0ac, (byte)0x00a, (byte)0x000,
    (byte)0x000, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x0d0, (byte)0x000, (byte)0x000, (byte)0x000}},

    {{(byte)0x01d, (byte)0x010, (byte)0x000, (byte)0x000,
    (byte)0x01c, (byte)0x000},
    {(byte)0x0c1, (byte)0x000, (byte)0x000, (byte)0x018,
    (byte)0x001, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x000, (byte)0x000, (byte)0x018, (byte)0x000,
    (byte)0x004, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x001, (byte)0x000, (byte)0x000, (byte)0x000,
    (byte)0x0ca, (byte)0x0ac, (byte)0x00a, (byte)0x000,
    (byte)0x000, (byte)0x000, (byte)0x000, (byte)0x000}}};

/** Declare "static" Class Variables. */

/** Declare non-"static" Instance Variables. */

/**
 * Number of sleep in sec each Thread runs the Stress Test.
 * <p>
 * Set with Java Environment Parameter "sleepSec=1".
 */
protected int sleepSec;

/** Thread Index Number. */
private int self;

/**
 * RECEIVE DIAGNOSTICS Command Descriptor Block specific to Sun StorEdge
 * 3510/3511 product family.
 */
private byte[] recDiagCdb;

/**
 * RECEIVE DIAGNOSTICS Data Buffer specific to Sun StorEdge
 * 3510/3511 product family.
 */
private byte[] recDiagData;

/** Initialize "static" Class Variables. */
static {
	/** Booleans. */

	/** I/O Paths. */

	/** Test Settings. */

	/** Internal Variables - long. */

	/** Internal Variables - int. */

	/** Internal Variables - short. */

	/** Internal Variables - byte. */

	/** Internal Variables - Arrays. */

	/** Internal Variables - String. */

	/** Internal Variables - misc. */
}

/** Initialize non-"static" Instance Variables. */
{
	/** Booleans. */

	/** I/O Paths. */

	/** Test Settings. */

	/** Internal Variables - long. */

	/** Internal Variables - int. */
	sleepSec = 1;

	/** Internal Variables - short. */

	/** Internal Variables - byte. */

	/** Internal Variables - Arrays. */
	recDiagCdb = new byte[] {
	    (byte)0x01c, (byte)0x000, (byte)0x000, (byte)0x000,
	    (byte)0x0f4, (byte)0x000};
	recDiagData = new byte[0x0f4];

	/** Internal Variables - String. */

	/** Internal Variables - misc. */
}

/**
 * Default Constructor to initialize the first thread.
 * <p>
 * "self" set to 0.  "numThreads", "numCycles", "minPLL", and "maxPLL"
 * are set from Java System Properties.
 */
public SPC1dStress() {
	tester			= this;
	self			= 0;
	sleepSec		= getIntProperty("sleepSec", 1);
	setBranch("SEND DIAGNOSTIC");
	scsiData.setOpcode(SCSIData.OP.getByte(getBranch()));
}

/**
 * Alternate Constructor Function called by first thread and subsequent
 * threads to generate "numThreads".
 * <p>
 * @param	self		This Threads identification Number.  Ranges
 *				from 0 thru (numThreads-1).
 * @param	numThreads	Number of Threads for this thread to start.
 * @param	numCycles	Number of Cycles for this thread to run.
 * @param	sleepSec	Number of Seconds to delay between cycles.
 */
public SPC1dStress(JISTEnv tester, int self, int numThreads, int numCycles,
    int sleepSec) {
	this.tester		= tester;
	this.self		= self;
	this.numThreads		= numThreads;
	this.numCycles		= numCycles;
	this.sleepSec		= sleepSec;
	setBranch("SEND DIAGNOSTIC");
	scsiData.setOpcode(SCSIData.OP.getByte(getBranch()));
	dupBufferedWriter(tester);
}

/**
 * Method used to detect offshoot Test Branches.
 * <p>
 * All methods with signature
 * "<FONT SIZE="-1"><CODE>public void test*(void)</CODE></FONT>" in this class
 * or this class' parent classes are automatically detected.
 * <p>
 * @see JISTLogic#getTree(String)
 * @see JISTLogic#getTree(String[])
 */
public static String[] getTree() {
	return getTree("com.sun.jist.scsi.test.SPC1dStress");
}

/**
 * Threading Function for Runnable Interface.  This is the main execution
 * engine of this class.  The method sets unassigned values to defaults,
 * starts up necessary child threads, prints a sub header line, and then
 * cycles thru SEND DIAGNOSTIC routines.
 * <p>
 * If minPLL, or maxPLL are not specified, then the default values of 1, or
 * 1024 are assigned, respectively.
 * <p>
 * If numThreads is greater than 1, then Fractal Logic is used to start the
 * threads approximately at the same time. Once Started, the threads
 * operate independently.
 * <p>
 * Once the numThreads has been reduced to 1, then a sub header record is
 * printed detailing the Parameter List Length (PLL) range.  From this point
 * an, all Test# entries will be prefixed with the "self" Thread Number.
 * <p>
 * Finally, numCycles of SEND DIAGNOSTIC will be issued of a random selected
 * number of bytes in the PLL range.
 */
public void run() {
	if (this != tester) {
		Thread.currentThread().setName(new StringBuilder("SubThread ")
		    .append(Integer.toHexString(self)).append(" ").toString());
		setUp();
	}

	setDefaults();
	startChildThreads();
	byte[][] cdbDataPair;

	tester.incTestThreads(1);
	startTest();
	while (running && tester.stressing && --numCycles >= 0) {
		cdbDataPair = getCdbDataPair();
		doIO("SE3510/SE3511 Stress Expecting Good Status Condition",
		    cdbDataPair[0], cdbDataPair[1], 0, 0);
		if (passfailBoolean) {
			doIO("to Retrieve Diagnostic Results",
			    recDiagCdb, recDiagData, 0, 0);
		}
		if (!tester.passfailBoolean) {
			tester.stressing = false;
		} else if (sleepSec > 0) {
			try {
				Thread.currentThread().sleep(sleepSec*1000);
			} catch (InterruptedException e) {
				tester.stressing = false;
			}
		}
	}
	tester.incTestThreads(-1);
	stopTest();
}

/**
 * PRIVATE method to set default values.
 */
private void setDefaults() {
	/* Boundry Controls */
	if (numThreads <= 0) {
		numThreads = 1;
	}
	if (numCycles <= -1) {
		numCycles = 1;
	}
	if (numCycles == 0) {
		numCycles = Integer.MAX_VALUE;
	}
	if (sleepSec <= -1) {
		sleepSec = 1;
	}
}

/**
 * PRIVATE method to start child threads and reduce ongoing memory use.
 */
private void startChildThreads() {
	int child;
	int childThreads;
	while (numThreads > 1) {
		child		= self + (numThreads/2);
		childThreads	= numThreads - (numThreads/2);
		numThreads	= numThreads/2;
		new Thread(new SPC1dStress(tester, child, childThreads,
		    numCycles, sleepSec)).start();
	}
}

/**
 * Get random SEND DIAGNOSTICS Command Descriptor Block & Data Buffer from
 * the provided array of CDB/DataBuffer pairs.
 * <p>
 * @return cdbDataPair Random SEND DIAGNOSTICS CDB & Data Buffer.
 */
protected  byte[][] getCdbDataPair() {
	return cdbdata[rand.nextInt(cdbdata.length)];
}

/**
 * {@link JISTData#JIST_MANDATORY Sun Mandatory} Test Branch to
 * Interogate SEND DIAGNOSTICS response to stress loading.
 * <p>
 * This test is specific to Sun StorEdge[tm] 3510/3511 product family.
 */
public void testStress() {
	setLeaf(getBranch(),
	    "Threaded Stress Test for Sun StorEdge[tm] 3510/3511");
	assertBypass("Peripheral Device Type", ((
	    scsiData.peripheralDeviceType) == 0x00d));
	assertBypass("Sun StorEdge[tm] Specific Test Branch",
	    (!scsiData.vendor.matches("SUN     ")));
	assertBypass("Sun StorEdge[tm] 3510/3511 Specific Test Branch",
	    (!scsiData.product.matches("StorEdge 351[01]   ")));
	run();
	assertTrue(passfailBoolean);
}

} /* Class End */

