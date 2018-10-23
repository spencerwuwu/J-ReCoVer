// https://searchcode.com/api/result/66853378/

package com.reliablerabbit.prettyprint;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Test suite for the PrettyPrint class by Mohammad El-Abid
 * @author Mohammad Typaldos [mohammad at reliablerabbit.com]
 */
public class PrettyPrintTest extends TestCase {

    /**
     * Where we will store the data that is received from "System.out"
     */
    private ByteArrayOutputStream outContent;
    /**
     * A variable used in all the suites to test the PrettyPrint class
     */
    private PrettyPrint prettyPrint;
    /**
     * The stream that will be wrap outContent
     */
    private PrintStream outStream;
    /**
     * The line separator of the OS, generally \n on *nix and \r\n and Windows
     */
    private static String lineSeparator = System.getProperty("line.separator");

    /**
     * Sets anything that used by most tests to reduce redundancy.
     * @throws Exception 
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupPrettyPrint();
    }

    /**
     * Sets up a fake PrintStream and hooks up a PrettyPrint on it
     */
    private void setupPrettyPrint() {
        outContent  = new ByteArrayOutputStream();
        outStream   = new PrintStream(outContent);
        prettyPrint = new PrettyPrint(outStream);
    }

    /**
     * Get the output from prettyPrint
     * @return A string holding the content printed
     */
    private String getOutput() {
        return outContent.toString();
    }

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PrettyPrintTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(PrettyPrintTest.class);
    }

    /**
     * Tests the print(String string) wrapper
     * @see PrettyPrint#print(java.lang.String)
     */
    public void testPrintString() {
        prettyPrint.print("A string");
        assertEquals("A string", getOutput());
    }

    /**
     * Tests the new line creator
     * @see PrettyPrint#newLine()
     */
    public void testNewLine() {
        prettyPrint.newLine();
        assertEquals(lineSeparator, getOutput());
    }

    /**
     * Tests the println(String string) wrapper
     * @see PrettyPrint#println(java.lang.String)
     */
    public void testPrintLine() {
        prettyPrint.println("Water drop");
        String expected = "Water drop" + lineSeparator;
        assertEquals(expected, getOutput());
    }

    /**
     * Tests printing a block of text wrapped in asterisk
     * @see PrettyPrint#blockPrint(java.lang.String[]) 
     */
    public void testBlockPrintDefault() {
        String[] lines = {"A Pretty Print Block Test", "Developed by Mohammad El-Abid"};
        prettyPrint.setBlockPadding(2);
        prettyPrint.blockPrint(lines);
        String expected =
                  "***********************************" + lineSeparator
                + "*    A Pretty Print Block Test    *" + lineSeparator
                + "*  Developed by Mohammad El-Abid  *" + lineSeparator
                + "***********************************" + lineSeparator;
        assertEquals(expected, getOutput());
    }

    /**
     * Tests the block print with a minimum length set.
     * @see PrettyPrint#blockPrint(java.lang.String[], int) 
     */
    public void testBlockPrintWithMin() {
        String[] lines = {"One", "Two"};
        prettyPrint.setBlockPadding(2);
        prettyPrint.blockPrint(lines, 5);
        String expected =
                  "***********" + lineSeparator
                + "*   One   *" + lineSeparator
                + "*   Two   *" + lineSeparator
                + "***********" + lineSeparator;
        assertEquals(expected, getOutput());
    }

    /**
     * Tests that the loop for printing a character x times is working correctly
     * @see PrettyPrint#printTimes(int, char)
     */
    public void testPrintTimes() {
        prettyPrint.printTimes(3, '-');
        assertEquals("---", getOutput());
    }

    /**
     * Test the getter for outputStream, useful for developers
     * @see PrettyPrint#getOutputStream() 
     */
    public void testGetOutputStream() {
        assertSame(prettyPrint.getOutputStream(), outStream);
    }

    /**
     * Test the block padding setter, it doesn't allow a negative value
     * @see PrettyPrint#setBlockPadding(int)
     */
    public void testSetBlockPadding() {
        assertTrue(prettyPrint.setBlockPadding(5));
        assertFalse(prettyPrint.setBlockPadding(-2));
    }

    /**
     * Test the centering text method
     * @see PrettyPrint#centerString(java.lang.String, int) 
     */
    public void testCenterText() {
        String shortString = prettyPrint.centerString("Five is a four letter word", 3);
        assertEquals("Five is a four letter word", shortString);

        String centeredString = prettyPrint.centerString("Ten", 10);
        assertEquals("    Ten   ", centeredString);
    }

    /**
     * Tests the method responsable for generating spacing and borders
     * @see PrettyPrint#generateRepeatingString(int, char) 
     */
    public void testGenerateRepeatingString() {
        String generated = prettyPrint.generateRepeatingString(5, '*');
        assertEquals("*****", generated);

        generated = prettyPrint.generateRepeatingString(3, '!');
        assertEquals("!!!", generated);
    }
}

