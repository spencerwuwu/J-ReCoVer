// https://searchcode.com/api/result/59912198/

/*
 * Copyright (c) 2001-2008 David Soergel
 * 418 Richmond St., El Cerrito, CA  94530
 * dev@davidsoergel.com
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the author nor the names of any contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.davidsoergel.dsutils.math;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;


/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */

public class LongRational extends Number implements Comparable, Serializable
	{
	// ------------------------------ FIELDS ------------------------------

	// this should be final, but we want to allow Hessian serialization
	long numerator;
	long denominator;

	// for Hessian only
	public LongRational()
		{
		}

	// -------------------------- STATIC METHODS --------------------------

	/**
	 * Compare two long rational numbers, avoiding numerical overflow. Probably this method is well known or obvious to
	 * people who do this kind of thing a lot, but I'm pretty pleased that I figured it out from scratch :)  It works like
	 * this:
	 * <p/>
	 * assuming everything is positive,
	 * <pre>
	 *   (a/b) <=> (c/d)    ===   ad     - bc     <=> 0
	 * (ad-bc) <=> 0        ===   d(a-nc) - c(b-nd) <=> 0   for arbitrary n
	 *
	 * If one but not both of (a-nc) and (b-nd) is negative, then the solution is clear.
	 * If both are positive, then we iterate, having reduced the magnitude of the numbers involved (if n is well chosen).
	 * If both are negative, then we multiply them by -1 and reverse the order of the subtraction,
	 * thus restoring the functional form and guaranteeing everything positive again;
	 * then we can iterate as in the positive case.
	 *
	 * @return 1 if ab > cd, -1 if ab < cd, 0 if ab == cd.
	 */
	public static int overflowSafeCompare(@NotNull LongRational ab, @NotNull LongRational cd)
		{
		if (ab.equals(cd))
			{
			return 0;
			}

		long a = ab.numerator;
		long d = cd.denominator;
		long b = ab.denominator;
		long c = cd.numerator;

		while (true)// guaranteed to complete in finite time
			{
			// a,b,c,and d are all positive at this point

			// n is arbitrary; we want it as large as possible, up to a/c or b/d, but without the multiplication causing an overflow
			long n = a / c;// x will be the remainder
			long m = b / d;// y will be the remainder

			n = Math.min(n, m);

			n = n == 0 ? 1 : Math.min(Math.min(n, Long.MAX_VALUE / c), Long.MAX_VALUE / d);

			long x = a - (n * c);
			long y = b - (n * d);

			// REVIEW maybe this comparisons to 0 would be faster as a bit shift to examine the sign bit?  Ideally the compiler would catch that if true
			if (x > 0 && y < 0)
				{
				return 1;
				}
			else if (x < 0 && y > 0)
				{
				return -1;
				}
			else if (x < 0 && y < 0)// switch things around to make everything positive again
				{
				b = d;
				a = c;
				d = -y;
				c = -x;
				}
			else if (x > 0 && y > 0)// everything still positive
				{
				a = d;
				d = x;
				b = c;
				c = y;
				}
			else if (x == 0)
				{
				return Long.valueOf(-y).compareTo((long) 0);
				}
			else if (y == 0)
				{
				return Long.valueOf(x).compareTo((long) 0);
				}
			else
				{
				assert false;// the above conditions should cover all cases, yes?
				}
			}
		}

	public boolean equals(@Nullable Object o)
		{
		if (this == o)
			{
			return true;
			}
		if (o == null || getClass() != o.getClass())
			{
			return false;
			}

		@NotNull LongRational that = (LongRational) o;

		// note this depends on both LongRationals already being reduced, but that's OK since it's in the constructor
		if (denominator != that.denominator)
			{
			return false;
			}
		if (numerator != that.numerator)
			{
			return false;
			}

		return true;
		}

	/**
	 * Subtract two rational numbers, being careful to avoid numerical overflow if possible
	 *
	 * @param a
	 * @param b
	 */
	@NotNull
	private static LongRational minus(@NotNull LongRational a, @NotNull LongRational b)
		{
		// the GCD thing helps keep the numbers small and may avoid an overflow, but it's slower too
		long denominatorGCD = MathUtils.GCD(a.denominator, b.denominator);

		long aFactor = b.denominator / denominatorGCD;// the division should be precision-safe anyway
		long bFactor = a.denominator / denominatorGCD;// the division should be precision-safe anyway

		long denom = SafeIntegerArithmetic.mul(a.denominator, aFactor);// == Mul(b.denominator, bFactor)

		long aNum = SafeIntegerArithmetic.mul(a.numerator, aFactor);
		long bNum = SafeIntegerArithmetic.mul(b.numerator, bFactor);

		return new LongRational(aNum - bNum, denom);
		}

	@NotNull
	public static LongRational mediant(@NotNull LongRational a, @NotNull LongRational b)
		{
		return new LongRational(a.numerator + b.numerator, a.denominator + b.denominator);
		}

	// --------------------------- CONSTRUCTORS ---------------------------

	public LongRational(@NotNull Long numerator, @NotNull Long denominator)
		{
		this(numerator.longValue(), denominator.longValue());
		}

	public LongRational(final long numerator, final long denominator)
		{
		long num = numerator;
		long den = denominator;

		// don't bother with all that NaN nonsense
		if (den == 0)
			{
			// BAD why is there a special case for 1 / 0 ??
			if (num == 1)
				{
				den = 1;
				}
			else
				{
				throw new ArithmeticException("Division by zero when constructing new LongRational");
				}
			}
		//reduce();

		long gcd = MathUtils.GCD(num, den);
		this.numerator = num / gcd;
		this.denominator = den / gcd;
		}

/*	private void reduce()
		{
		long gcd = MathUtils.GCD(numerator, denominator);
		numerator /= gcd;
		denominator /= gcd;
		}
*/
	// --------------------- GETTER / SETTER METHODS ---------------------

	public long getDenominator()
		{
		return denominator;
		}

	public long getNumerator()
		{
		return numerator;
		}

	// ------------------------ CANONICAL METHODS ------------------------

	public int hashCode()
		{
		int result;
		result = (int) (numerator ^ (numerator >>> 32));
		result = 31 * result + (int) (denominator ^ (denominator >>> 32));
		return result;
		}

	@NotNull
	public String toString()
		{
		return "" + numerator + "/" + denominator;
		}

	// ------------------------ INTERFACE METHODS ------------------------


	// --------------------- Interface Comparable ---------------------

	/**
	 * Standard comparison method, with the caveat that LongRationals that differ by less than the resolution of double way
	 * not be correctly ordered.
	 *
	 * @param o
	 * @return
	 */
	public int compareTo(Object o)
		{
		@NotNull LongRational lro = (LongRational) o;
		int result = new Double(doubleValue())
				.compareTo(lro.doubleValue());// I assume this is faster than my algorithm below...??
		if (result == 0)
			{
			// the values are (nearly) equal, differing by less than the resolution of double
			// probably they are actually equal
			// I wonder how often in practice we'll see subtly different fractions?

			return overflowSafeCompare(this, lro);

			// faster/simpler not to try the subtraction method first

			/*
			try
				{
				return this.minus(lro).compareToZero();
				}
			catch (SafeIntegerArithmetic.IllegalArithArgsException e)
				{
				return overflowSafeCompare(this, lro);
				}
				*/
			}
		return result;
		//return (numerator / denominator) (lro.numerator / lro.denominator);
		}

	// -------------------------- OTHER METHODS --------------------------

	private int compareToZero()
		{
		if (numerator == 0)
			{
			return 0;
			}

		// assume denominator != 0;

		if (denominator > 0)
			{
			return numerator < 0 ? -1 : 1;// we know it's not 0
			}
		else
			{
			return numerator > 0 ? -1 : 1;
			}
		}

	public float floatValue()
		{
		return (float) doubleValue();
		}

	public double doubleValue()
		{
		return (double) numerator
		       / (double) denominator;//To change body of implemented methods use File | Settings | File Templates.
		}

	public int intValue()
		{
		return (int) doubleValue();
		}

	public long longValue()
		{
		return (long) doubleValue();
		}

	@NotNull
	public LongRational minus(@NotNull LongRational b)
		{
		return minus(this, b);
		}
	}

