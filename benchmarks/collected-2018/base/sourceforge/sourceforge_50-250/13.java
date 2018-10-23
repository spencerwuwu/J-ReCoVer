// https://searchcode.com/api/result/3409735/

package com.objectwave.utility;

import java.util.Vector;
import java.util.Enumeration;

/**
 * @author Dave Hoag
 * @version $Date: 2005/04/04 02:27:39 $ $Revision: 1.2 $
 */
public class StringManipulatorTest extends com.objectwave.test.UnitTestBaseImpl
{
	public StringManipulatorTest(){}
	public StringManipulatorTest(String test){
		super(test);
	}
    public static void main(String [ ]args)
    {
        com.objectwave.test.TestRunner.run(new StringManipulatorTest(), args);
    }
    Vector v = null;
    String stateString;
    /**
    */
    public void testEncryption() //com.objectwave.test.TestImpl context)
    {
        String originalString = "this is a PRIVATE string";
        String password = "password";
        String encrypted = StringManipulator.xorCrypto(originalString, password);
        testContext.assertEquals(originalString, StringManipulator.xorCrypto(encrypted, password));

        String passwordTwo = "zippity";
        encrypted = StringManipulator.xorCrypto("this is a PRIVATE string", passwordTwo);
        //These two should NOT be equal
        testContext.assertTrue(StringManipulator.xorCrypto(encrypted, password).equals(originalString) == false );
    }
    public void testExtractStrings() //com.objectwave.test.TestImpl context)
    {
        String list = "one,two,\"th','ree\",\"four,five\"";
        Vector v = com.objectwave.utility.StringManipulator.extractStringsDelimiter(list, ",");
        testContext.assertEquals(6, v.size());
        v = com.objectwave.utility.StringManipulator.extractStringsDelimiter(list, ',');
        testContext.assertEquals(4, v.size());
    }
    /**
    */
    public void testExtractStringsToString() //com.objectwave.test.TestImpl context)
    {
        String list = "one,two,\"th','ree\",\"four,five\"";
        this.v = StringManipulator.stringToVector(list);
        String result = StringManipulator.vectorToString(v);
        testContext.assertEquals(list, result);
    }
    /**
    */
    public void testPatternMatching() //com.objectwave.test.TestImpl context)
    {
        String patterns[] =  {"hi mom", "ji_mom", "hi_mom", "hi_moj", "h%mom", "hi%mum", "h%", "h%m_m", "hi mom!", "Hi Mom", "hi mom%", "hi mom_" };
        boolean expected[] = {true,     false,    true,     false,    true,    false,    true, true,    false,     false,    true,      false     };
        for (int i = 0; i < patterns.length; ++i)
        {
            testContext.assertTrue("Failed at index: " + i + " '" + patterns[i] + '\'',
                   StringManipulator.matchesPattern("hi mom", patterns[i]) == expected[i]);
        }
    }
    /**
    */
    public void testPatternMatching2() //com.objectwave.test.TestImpl context)
    {

        String  pats[]  = { "matchThis", "*", "mat*", "*Thi", "match*",
                            "*ch*", "match***s", "*ma*tch*is*", "ma?ch*" };
        String  strs[]  = { "matchThis", "", "cow", "matchThi", "matchThiss", "somematches" };
        boolean res[][] = {
                            {true,  false, false, false, false, false}, // pats[0], strs[0..n]
                            {true,  true,  true,  true,  true,  true},  // pats[1], strs[0..n]
                            {true,  false, false, true,  true,  false}, // pats[2], strs[0..n]
                            {false, false, false, true,  false, false}, // ...
                            {true,  false, false, true,  true,  false},
                            {true,  false, false, true,  true,  true},
                            {true,  false, false, false, true,  false},
                            {true,  false, false, false, true,  false},
                            {true,  false, false, true,  true,  false},
                          };

        int failures = 0;
        int tests = 0;

        for (int i=0; i < pats.length; ++i)
        {
            for (int j=0; j < strs.length; ++j)
            {
                if (StringManipulator.matchesPattern(strs[j], pats[i], '*', '?') != res[i][j])
                {
                    System.out.println("Failed test: pat \"" + pats[i] +
                                       "\", str \"" + strs[j] +
                                       "\" should have " +
                                       (res[i][j] ? "pass" : "fail") + "ed.");
                    ++failures;
                }
                ++tests;
            }
        }
//			System.out.println("Finished " + tests + " case(s): " + failures + " failure(s).");
        testContext.assertTrue("Failed " + failures + " pattern-match test(s)",
               failures==0);
    }
    /**
     */
    public void testReduceStrings() //com.objectwave.test.TestImpl context)
    {
        String strs[] = {   "total investment revenue",
                            "number of children",
                            "name of spouse",
                            "ttl. income",
                            "credit rating",
                            "average income estimates",
                            "total amount in bank",
                            "number at chapter 7"};
        int lengths[] = { 10, 8, 6, 7, 8, 4, 9, 6 };

        for (int i=0; i < strs.length; ++i)
        {
            int len = i<lengths.length ? lengths[i] : 10;
            testContext.assertTrue("String " + strs[i] + " failed to reduce to " + len, StringManipulator.reduceString(strs[i], len).length() <= len);
        }
    }
    /**
     */
    public void testVectorToStrings()//com.objectwave.test.TestImpl context)
    {
        Enumeration e = null;
        String strings[] = { "This", "That", "one,two,three",
                            ",comma first", "comma last," };
        Vector v = new Vector();
        for (int i=0; i < strings.length; ++i)
            v.addElement(strings[i]);

        String s = StringManipulator.vectorToString(v);
        Vector v2 = StringManipulator.stringToVector(s);
        testContext.assertTrue(strings.length == v2.size());
    }
}
