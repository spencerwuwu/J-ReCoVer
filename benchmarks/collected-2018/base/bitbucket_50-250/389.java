// https://searchcode.com/api/result/62934276/

/*----------------------------------------------------------------------------*
 * This file is part of Pitaya.                                               *
 * Copyright (C) 2012-2013 Osman KOCAK <kocakosm@gmail.com>                   *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify it    *
 * under the terms of the GNU Lesser General Public License as published by   *
 * the Free Software Foundation, either version 3 of the License, or (at your *
 * option) any later version.                                                 *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public     *
 * License for more details.                                                  *
 * You should have received a copy of the GNU Lesser General Public License   *
 * along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *----------------------------------------------------------------------------*/

package org.pitaya.util;

import static org.pitaya.util.Fraction.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.junit.Test;

/**
 * {@link Fraction}'s unit tests.
 *
 * @author Osman KOCAK
 */
public final class FractionTest
{
	@Test
	public void testValueOfInt()
	{
		assertEquals(ONE, valueOf(1));
		assertEquals(ONE.negate(), valueOf(-1));
	}

	@Test
	public void testValueOfInteger()
	{
		assertEquals(ONE, valueOf(Integer.valueOf(1)));
		assertEquals(ONE.negate(), valueOf(Integer.valueOf(-1)));
	}

	@Test
	public void testValueOfBigInteger()
	{
		assertEquals(ONE, valueOf(BigInteger.ONE));
		assertEquals(ONE.negate(), valueOf(BigInteger.ONE.negate()));
	}

	@Test
	public void testValueOfString()
	{
		assertEquals(ONE, valueOf("   1 "));
		assertEquals(ONE_THIRD, valueOf("1/3"));
		assertEquals(ONE_THIRD, valueOf("  -1   /   -3"));
		assertEquals(ONE_THIRD.negate(), valueOf("1/-3"));
	}

	@Test
	public void testInstanciation()
	{
		assertEquals(ONE_QUARTER, new Fraction(1, 4));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInstanciationWithInvalidDenominator()
	{
		new Fraction(42, 0);
	}

	@Test
	public void testAddition()
	{
		assertEquals(ONE, ONE_HALF.plus(TWO_QUARTERS));
		assertEquals(ONE, ONE_THIRD.add(TWO_THIRDS));
		assertEquals(ZERO, ONE_THIRD.plus(ONE_THIRD.negate()));
	}

	@Test
	public void testSubtraction()
	{
		assertEquals(ZERO, ONE_HALF.minus(TWO_QUARTERS));
		assertEquals(ONE_THIRD.negate(), ONE_THIRD.minus(TWO_THIRDS));
		assertEquals(ONE, ONE_THIRD.subtract(TWO_THIRDS.negate()));
	}

	@Test
	public void testMultiplication()
	{
		assertEquals(ONE_QUARTER, ONE_HALF.times(TWO_QUARTERS));
		assertEquals(ONE, ONE_HALF.multiply(valueOf(2)));
		assertEquals(ONE.negate(), ONE_THIRD.times(valueOf(-3)));
	}

	@Test
	public void testDivision()
	{
		assertEquals(ONE_QUARTER, ONE_HALF.over(valueOf(2)));
		assertEquals(THREE_QUARTERS, ONE_HALF.divide(TWO_THIRDS));
		assertEquals(valueOf(-2), ONE.over(ONE_HALF.negate()));
	}

	@Test(expected = ArithmeticException.class)
	public void testDivisionByZero()
	{
		ONE.over(ZERO);
	}

	@Test
	public void testPower()
	{
		assertEquals(ONE_QUARTER, ONE_HALF.pow(2));
		assertEquals(valueOf("1/27"), ONE_THIRD.pow(3));
		assertEquals(ONE_QUARTER, ONE_HALF.negate().pow(2));
	}

	@Test(expected=ArithmeticException.class)
	public void testPowerWithNegativeExponent()
	{
		ONE_QUARTER.pow(-3);
	}

	@Test
	public void testAbs()
	{
		assertEquals(ONE_HALF, valueOf("-1/2").abs());
		assertEquals(ONE_HALF, valueOf("1/-2").abs());
		assertEquals(ONE_HALF, valueOf("1/2").abs());
	}

	@Test
	public void testInvert()
	{
		assertEquals(valueOf(2), ONE_HALF.invert());
		assertEquals(valueOf(-3), ONE_THIRD.negate().invert());
	}

	@Test
	public void testNegate()
	{
		assertEquals(valueOf("-1/3"), ONE_THIRD.negate());
		assertEquals(ONE_THIRD, valueOf("1/-3").negate());
	}

	@Test
	public void testReduce()
	{
		assertEquals(1, TWO_QUARTERS.reduce().numerator().intValue());
		assertEquals(2, TWO_QUARTERS.reduce().denominator().intValue());
	}

	@Test
	public void testSignum()
	{
		assertEquals(0, ZERO.signum());
		assertEquals(1, TWO_QUARTERS.signum());
		assertEquals(-1, TWO_THIRDS.negate().signum());
	}

	@Test
	public void testNumerator()
	{
		Fraction f = valueOf("42 / 16");
		assertEquals(42, f.numerator().intValue());
	}

	@Test
	public void testDenominator()
	{
		Fraction f = valueOf("42 / 16");
		assertEquals(16, f.denominator().intValue());
	}

	@Test
	public void testBigDecimalValue()
	{
		BigDecimal one = BigDecimal.valueOf(1L);
		BigDecimal three = BigDecimal.valueOf(3L);
		BigDecimal oneThird = one.divide(three, MathContext.DECIMAL128);
		assertEquals(oneThird, ONE_THIRD.bigDecimalValue());
	}

	@Test
	public void testIntValue()
	{
		assertEquals(0, ONE_THIRD.intValue());
	}

	@Test
	public void testLongValue()
	{
		assertEquals(0L, ONE_THIRD.longValue());
	}

	@Test
	public void testFloatValue()
	{
		assertTrue((float)(1.0 / 3.0) == ONE_THIRD.floatValue());
	}

	@Test
	public void testDoubleValue()
	{
		assertTrue(1.0 / 3.0 == ONE_THIRD.doubleValue());
	}

	@Test
	public void testCompareTo()
	{
		assertEquals(0, ONE_HALF.compareTo(TWO_QUARTERS));
		assertTrue(ONE_HALF.compareTo(ONE_THIRD) > 0);
		assertTrue(ONE_HALF.compareTo(TWO_THIRDS) < 0);
		assertTrue(ONE_HALF.negate().compareTo(ONE_THIRD) < 0);
	}

	@Test
	public void testToString()
	{
		assertEquals("2 / 4", TWO_QUARTERS.toString());
		assertEquals("-2 / 3", TWO_THIRDS.negate().toString());
	}

	@Test
	public void testEqualsAndHashCode()
	{
		assertEquals(ONE, ONE);
		assertEquals(ONE.hashCode(), ONE.hashCode());
		assertFalse(ONE.equals((Fraction)null));
		assertEquals(ONE_HALF, TWO_QUARTERS);
		assertEquals(ONE_HALF.hashCode(), TWO_QUARTERS.hashCode());
		assertFalse(ONE_HALF.equals(ONE_THIRD));
		assertFalse(valueOf(2).equals(TWO_THIRDS));
	}
}

