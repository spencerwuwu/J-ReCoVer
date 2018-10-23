// https://searchcode.com/api/result/105975320/

// Copyright (c) 2014 Dustin Leavins
// See the file 'LICENSE' for copying permission.

package info.dustin_leavins.calculatord;

import java.math.BigInteger;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestFraction {

    @Test
    public void zeroTest() {
        Fraction zero = Fraction.ZERO;

        assertEquals(BigInteger.valueOf(0), zero.getNumerator());
        assertEquals(BigInteger.valueOf(1), zero.getDenominator());
    }

    @Test
    public void stringConstructor() {
        Fraction f;

        // Integer
        f = new Fraction("10");
        assertEquals(BigInteger.valueOf(10), f.getNumerator());
        assertEquals(BigInteger.valueOf(1), f.getDenominator());

        // Fraction, Positive, Less than 1
        f = new Fraction("0.1"); // 1/10
        assertEquals(BigInteger.valueOf(1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());

        // Fraction, Negative, Greater than -1
        f = new Fraction("-0.1"); // -1/10
        assertEquals(BigInteger.valueOf(-1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());

        // Invalid case
        f = new Fraction("test");
        assertEquals(BigInteger.valueOf(0), f.getNumerator());
        assertEquals(BigInteger.valueOf(1), f.getDenominator());

        // Invalid case
        f = new Fraction("10x");
        assertEquals(BigInteger.valueOf(10), f.getNumerator());
        assertEquals(BigInteger.valueOf(1), f.getDenominator());
    }

    @Test
    public void integerConstructor() {
        Fraction f;

        // Integer
        f = new Fraction(10,1);
        assertEquals(BigInteger.valueOf(10), f.getNumerator());
        assertEquals(BigInteger.valueOf(1), f.getDenominator());

        // Fraction, Positive, Less than 1
        f = new Fraction(1,10);
        assertEquals(BigInteger.valueOf(1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());

        // Fraction, Negative, Greater than -1
        f = new Fraction(-1,10);
        assertEquals(BigInteger.valueOf(-1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());

        // Fraction, Negative, greater than -1, negative denominator
        // Fraction should change the negative denominator to
        // positive and make the numerator negative.
        f = new Fraction(1,-10);
        assertEquals(BigInteger.valueOf(-1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());
    }

    @Test(expected=ArithmeticException.class)
    public void integerConstructorZeroDenominator() {
        new Fraction(1,0);
    }

    @Test
    public void longConstructor() {
        Fraction f;

        // Integer
        f = new Fraction(10L,1L);
        assertEquals(BigInteger.valueOf(10), f.getNumerator());
        assertEquals(BigInteger.valueOf(1), f.getDenominator());

        // Fraction, Positive, Less than 1
        f = new Fraction(1L,10L);
        assertEquals(BigInteger.valueOf(1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());

        // Fraction, Negative, Greater than -1
        f = new Fraction(-1L,10L);
        assertEquals(BigInteger.valueOf(-1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());

        // Fraction, Negative, greater than -1, negative denominator
        // Fraction should change the negative denominator to
        // positive and make the numerator negative.
        f = new Fraction(1L,-10L);
        assertEquals(BigInteger.valueOf(-1), f.getNumerator());
        assertEquals(BigInteger.valueOf(10), f.getDenominator());
    }

    @Test(expected=ArithmeticException.class)
    public void longConstructorZeroDenominator() {
        new Fraction(1L, 0L);
    }

    @Test
    public void testDoubleValue() {
        Fraction f = new Fraction("0.2");
        assertEquals(0.2D, f.doubleValue(), 0.00001);
    }

    @Test
    public void testAddition() {
        Fraction x;
        Fraction y;
        Fraction sum;

        // 1 + 3 = 4
        x = new Fraction(1,1);
        y = new Fraction(3,1);
        sum = new Fraction(4,1);
        assertEquals(sum, x.add(y));

        // -5/2 + 5/2 = 0/4
        x = new Fraction(-5,2);
        y = new Fraction(5,2);
        sum = new Fraction(0,4);
        assertEquals(sum, x.add(y));
    }

    @Test
    public void testSubtraction() {
        Fraction x;
        Fraction y;
        Fraction difference;

        // 5/2 - 5/2 = 0/4
        x = new Fraction(5,2);
        difference = new Fraction(0,4);
        assertEquals(difference, x.subtract(x));

        // 11 - 10 = 1
        x = new Fraction(11,1);
        y = new Fraction(10,1);
        difference = new Fraction(1,1);
        assertEquals(difference, x.subtract(y));
    }

    @Test
    public void testMultiply() {
        Fraction x;
        Fraction y;
        Fraction product;

        // (1/10) * -10 = (-10/10)
        x = new Fraction(1,10);
        y = new Fraction(-10,1);
        product = new Fraction(-10,10);
        assertEquals(product, x.multiply(y));
    }

    @Test
    public void testNegative() {
        Fraction f = new Fraction(9,3);
        Fraction negativeF = new Fraction(-9,3);

        assertEquals(negativeF, f.negative());
    }

    @Test
    public void testDivide() {
        Fraction x;
        Fraction y;
        Fraction quotient;

        // (1) / (-10) = (-1/10)
        x = new Fraction(1,1);
        y = new Fraction(-10,1);
        quotient = new Fraction(-1,10);
        assertEquals(quotient, x.divide(y));
    }

    @Test
    public void testInverse() {
        Fraction f = new Fraction(-1,99);
        Fraction result = new Fraction(-99,1);

        assertEquals(result, f.inverse());
    }

    @Test(expected=ArithmeticException.class)
    public void inverseInvalid() {
        new Fraction(0, 100).inverse();
    }

    @Test
    public void testEquals() {
        Fraction x = new Fraction(3,10);
        Fraction y = new Fraction("0.3");

        // Symmetry test - true
        assertTrue(x.equals(y));
        assertTrue(y.equals(x));

        // Transitive test - true
        Fraction z = new Fraction(3,10);
        assertTrue(y.equals(z));
        assertTrue(x.equals(z));
        assertTrue((x.equals(y) && y.equals(z)) == x.equals(z));

        // Symmetry test - false
        y = new Fraction(10,3);
        assertFalse(x.equals(y));
        assertFalse(y.equals(x));

        // null test
        assertFalse(x.equals(null));

        // Non-Fraction class test
        assertFalse(x.equals(BigInteger.valueOf(4)));
    }

    @Test
    public void reduce() {
        Fraction f = new Fraction(1,3);
        assertTrue(f.reduce().equals(f));

        f = new Fraction(2,6);
        assertEquals(new Fraction(1,3), f.reduce());

        f = new Fraction(6,2);
        assertEquals(new Fraction(3,1), f.reduce());

        f = new Fraction(-2, 6);
        assertEquals(new Fraction(-1, 3), f.reduce());
    }

    @Test
    public void testHashCode() {
        Fraction f1 = new Fraction(3,1);
        Fraction f2 = new Fraction("3");

        assertEquals(f1.hashCode(), f2.hashCode());
    }
}

