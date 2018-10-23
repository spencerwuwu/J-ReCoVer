// https://searchcode.com/api/result/45290986/

package expFormula;
import java.math.*;
import java.lang.Math;

/**
 * The NumberValue class is a combination of an infinite precision rational number
 * class combined with a standard double data type.  A NumberValue object can be in
 * one of two states, exact or approximate.  When the object is in exact mode the 
 * value is stored as an infinite precision rational number and if the state is 
 * approximate the value is stored as a double precision real number.  When doing 
 * arithmetic operations on two NumberValue objects if they are both exact the 
 * result will be exact but if either one or both are approximate then the result
 * will be approximate.   
 * @author Don Spickler
 * @version 1.1
 */
public class NumberValue implements Cloneable 
{
	private boolean exact;
	private BigInteger num = null;
	private BigInteger den = null;
	private double val;              //  Maybe change to a BigDecimal?
	
	/**
	 * Constructor that takes in the numerator n and denominator d as strings 
	 * representing integers and produces an exact rational number.
	 * @param n the numerator in string form.
	 * @param d the denominator in string form.
	 */
	public NumberValue(final String n, final String d)
	{
		exact = true;
		num = new BigInteger(n);
		den = new BigInteger(d);
		val = 0;
		reduce();
	}
	
	/**
	 * Constructor that takes in the numerator n and denominator d as long integers 
	 * and produces an exact rational number.
	 * @param n the numerator.
	 * @param d the denominator.
	 */
	public NumberValue(final long n, final long d)
	{
		exact = true;
		num = new BigInteger(""+n);
		den = new BigInteger(""+d);
		val = 0;
		reduce();
	}

	/**
	 * Constructor that takes in a string representing a decimal number and creates
	 * an approximate double from the string.
	 * @param v the decimal number in string form. 
	 */
	public NumberValue(final String v)
	{
		exact = false;
		num = null;
		den = null;
		val = Double.valueOf(v);
	}

	/**
	 * Constructor that takes in a numerator n and denominator d, both BigIntegers, 
	 * and produces an exact rational number from them. 
	 * @param n the numerator, BigInteger type.
	 * @param d the denominator, BigInteger type.
	 */
	public NumberValue(final BigInteger n, final BigInteger d)
	{
		exact = true;
		num = new BigInteger(n.toString());
		den = new BigInteger(d.toString());
		val = 0;
		reduce();
	}
	
	/**
	 * Constructor that takes in a double and produces an approximate real number.
	 * @param v the decimal real number in double type. 
	 */
	public NumberValue(final double v)
	{
		exact = false;
		num = null;
		den = null;
		val = v;
	}

	/**
	 * Determines if the number is in exact mode or approximate mode.
	 * @return true if the number is in exact mode and false otherwise. 
	 */
	public boolean isExact()
	{
		return exact;
	}
	
	/**
	 * Creates an exact 0.
	 * @return NumberValue type storing 0 in exact form. 
	 */
	public static NumberValue makeZero()
	{
		return new NumberValue("0", "1");
	}

	/**
	 * Creates an exact 1.
	 * @return NumberValue type storing 1 in exact form.
	 */
	public static NumberValue makeOne()
	{
		return new NumberValue("1", "1");
	}

	/**
	 * Creates an exact -1.
	 * @return NumberValue type storing -1 in exact form.
	 */
	public static NumberValue makeMinusOne()
	{
		return new NumberValue("-1", "1");
	}

	/**
	 * Determines if the value is positive.
	 * @return true if the value is positive and false otherwise.
	 */
	public boolean isPos()
	{
		boolean retval = false;
		
		if (isExact())
		{
			reduce();
			if (num.compareTo(BigInteger.ZERO) > 0)
				retval = true;
		}
		else
		{
			retval = (val > 0);
		}
		
		return retval;
	}

	/**
	 * Determines if the value is negative.
	 * @return true if the value is negative and false otherwise.
	 */
	public boolean isNeg()
	{
		return !isPos();
	}

	/**
	 * Determines if the value is zero.
	 * @return true if the value is zero and false otherwise. 
	 */
	public boolean isZero()
	{
		if (exact)
		{
			reduce();
			if ((num.compareTo(BigInteger.ZERO) == 0) && (den.compareTo(BigInteger.ONE) == 0))
				return true;
		}
		else
			if (val == 0)
				return true;

		return false;
	}

	/**
	 * Determines if the value is zero under the given NumericTolerances.
	 * @param tol the input NumericTolerances.
	 * @return true if the value is zero under the given NumericTolerances 
	 * and false otherwise.
	 */
	public boolean isZero(NumericTolerances tol)
	{
		if (exact)
		{
			reduce();
			if ((num.compareTo(BigInteger.ZERO) == 0) && (den.compareTo(BigInteger.ONE) == 0))
				return true;
		}
		else
		{
			if (tol.getUseZeroTolerance())
			{
				if (Math.abs(val) < tol.getZeroTolerance())
					return true;
			}
			else
			{
				if (val == 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * Determines if the value is 1.
	 * @return true if the value is 1 and false otherwise.  
	 */
	public boolean isOne()
	{
		if (exact)
		{
			reduce();			
			if ((num.compareTo(BigInteger.ONE) == 0) && (den.compareTo(BigInteger.ONE) == 0))
				return true;
		}
		else
			if (val == 1)
				return true;

		return false;
	}

	/**
	 * Determines if the value is 1 under the given NumericTolerances.
	 * @param tol  the input NumericTolerances.
	 * @return true if the value is 1 under the given NumericTolerances 
	 * and false otherwise..
	 */
	public boolean isOne(NumericTolerances tol)
	{
		if (exact)
		{
			reduce();			
			if ((num.compareTo(BigInteger.ONE) == 0) && (den.compareTo(BigInteger.ONE) == 0))
				return true;
		}
		else
		{
			if (tol.getUseEqualityTolerance())
			{
				if (Math.abs(val - 1) < tol.getEqualityTolerance())
					return true;
			}
			else
			{
				if (val == 1)
					return true;
			}			
		}
		return false;
	}

	/**
	 * Determines if the value is -1.
	 * @return true if the value is -1 and false otherwise.  
	 */
	public boolean isMinusOne()
	{
		if (exact)
		{
			reduce();			
			if ((num.compareTo(BigInteger.ONE.negate()) == 0) && (den.compareTo(BigInteger.ONE) == 0))
				return true;
		}
		else
			if (val == -1)
				return true;

		return false;
	}

	/**
	 * Determines if the value is -1 under the given NumericTolerances.
	 * @param tol  the input NumericTolerances.
	 * @return true if the value is -1 under the given NumericTolerances 
	 * and false otherwise..
	 */
	public boolean isMinusOne(NumericTolerances tol)
	{
		if (exact)
		{
			reduce();			
			if ((num.compareTo(BigInteger.ONE.negate()) == 0) && (den.compareTo(BigInteger.ONE) == 0))
				return true;
		}
		else
		{
			if (tol.getUseEqualityTolerance())
			{
				if (Math.abs(val + 1) < tol.getEqualityTolerance())
					return true;
			}
			else
			{
				if (val == -1)
					return true;
			}			
		}
		return false;
	}

	/**
	 * Determines if the number is an exact integer.  So the number must be in exact 
	 * mode and an integer.  
	 * @return true if the number is an exact integer and false otherwise.
	 */
	public boolean isInteger()
	{
		if (exact)
		{
			reduce();
			if (den.compareTo(BigInteger.ONE) == 0)
				return true;
		}
		return false;
	}

	/**
	 * If the number is -1, 0, or 1 under the given NumericTolerances the result is 
	 * an exact -1, 0, or 1.  If the number is not -1, 0, or 1 a copy of the number 
	 * is returned. 
	 * @param tol  the input NumericTolerances.
	 * @return If the number is -1, 0, or 1 under the given NumericTolerances the result is 
	 * an exact -1, 0, or 1.  If the number is not -1, 0, or 1 a copy of the number 
	 * is returned. 
	 */
	public NumberValue AdjustByTolerances(NumericTolerances tol)
	{
		NumberValue retval = this.clone();
		if (retval.isZero(tol))
			retval = NumberValue.makeZero();
		if (retval.isOne(tol))
			retval = NumberValue.makeOne();
		if (retval.isMinusOne(tol))
			retval = NumberValue.makeMinusOne();
			
		return retval;
	}

	/**
	 * If the value is within the tolerance of an integer that exact integer is 
	 * returned.  Otherwise a copy of the number is returned.
	 * @param tol the input NumericTolerances.
	 * @return If the value is within the tolerance of an integer that exact integer 
	 * is returned.  Otherwise a copy of the number is returned.
	 */
	public NumberValue AdjustToInteger(NumericTolerances tol)
	{
		NumberValue retval = this.clone();
		
		if (retval.isExact()) return retval;
		
		try
		{
			double apval = approx();
			long roundval = Math.round(apval);
			if (Math.abs(apval - roundval) < tol.getEqualityTolerance())
				return new NumberValue(roundval, 1);
		}
		catch (Exception ex) { }
					
		return retval;
	}
	
	/**
	 * Adds the current number to n and returns the result. 
	 * @param n the number to add to the current number. 
	 * @return this + n
	 * @throws ArithmeticException
	 */
	public NumberValue add(NumberValue n)
	{
		NumberValue retval = null;
		
		try
		{
			if (exact && n.isExact())
			{
				retval = new NumberValue((num.multiply(n.getDen())).add(den.multiply(n.getNum())), den.multiply(n.getDen()));
				retval.reduce();
			}
			else
				retval = new NumberValue(approx() + n.approx());
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: add method numeric exception.");
		}

		return retval;
	}

	/**
	 * Subtracts n from the current number and returns the result. 
	 * @param n the number to be subtracted from the current number. 
	 * @return this - n
	 * @throws ArithmeticException
	 */
	public NumberValue subtract(NumberValue n)
	{
		NumberValue retval = null;
		
		try
		{
			if (exact && n.isExact())
			{
				retval = new NumberValue((num.multiply(n.getDen())).subtract(den.multiply(n.getNum())), den.multiply(n.getDen()));
				retval.reduce();
			}
			else
				retval = new NumberValue(approx() - n.approx());
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: subtract method numeric exception.");
		}

		return retval;
	}

	/**
	 * Multiplies n times the current number and returns the result. 
	 * @param n the number to be multiplied by the current number. 
	 * @return this * n
	 * @throws ArithmeticException
	 */
	public NumberValue multiply(NumberValue n)
	{
		NumberValue retval = null;
		
		try
		{
			if (exact && n.isExact())
			{
				retval = new NumberValue(num.multiply(n.getNum()), den.multiply(n.getDen()));
				retval.reduce();
			}
			else
				retval = new NumberValue(approx() * n.approx());
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: multiply method numeric exception.");
		}

		return retval;
	}

	/**
	 * Divides n into the current number and returns the result. 
	 * @param n the number to be divided into the current number. 
	 * @return this / n
	 * @throws ArithmeticException
	 */
	public NumberValue divide(NumberValue n)
	{
		NumberValue retval = null;
		
		try
		{
			if (exact && n.isExact())
			{
				retval = new NumberValue(num.multiply(n.getDen()), den.multiply(n.getNum()));
				retval.reduce();
			}
			else
				retval = new NumberValue(approx() / n.approx());
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: divide method numeric exception.");
		}

		return retval;
	}

	/**
	 * Multiplies the current number by -1 and returns the result.
	 * @return -this
	 * @throws ArithmeticException
	 */
	public NumberValue negate()
	{
		NumberValue retval = null;
		
		try
		{
			if (exact)
			{
				NumberValue negOne = new NumberValue("-1", "1");
				retval = negOne.multiply(this);
				retval.reduce();
			}
			else
				retval = new NumberValue(-1.0*approx());
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: negate method numeric exception.");
		}

		return retval;
	}

	/**
	 * Inverts the current number and returns the result.
	 * @return 1/this
	 * @throws ArithmeticException
	 */
	public NumberValue invert()
	{
		NumberValue retval = null;
		
		try
		{
			if (exact)
			{
				retval = new NumberValue(den, num);
				retval.reduce();
				if (retval.getDen().compareTo(BigInteger.ZERO) == 0) 
					throw new ArithmeticException("Division by zero.");
			}
			else
				retval = new NumberValue(1.0/approx());
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: invert method numeric exception.");
		}

		return retval;
	}

	/**
	 * Raises the current number to the integer power of p.
	 * @param p the power to be used.
	 * @return this ^ p
	 * @throws ArithmeticException
	 */
	public NumberValue pow(int p)
	{
		NumberValue retval = new NumberValue("1", "1");
		NumberValue base = clone();

		if (isZero() && (p > 0))
			return new NumberValue("0", "1");
		else if (isZero() && (p <= 0))
			throw new ArithmeticException("Division by zero.");
		
		if (isOne())
			return new NumberValue("1", "1");
		
		if (p < 0)
		{
			base.invert();
			p = -p;
		}
	
		try
		{
			if (exact)
			{
				for (int i = 0; i < p; i++)
					retval = retval.multiply(base); 
			}
			else
				retval = new NumberValue(Math.pow(approx(), (double)p));
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: pow(int) method numeric exception.");
		}

		return retval;
	}
	
	/**
	 * Raises the current number to the double power of p.
	 * @param p the power to be used.
	 * @return this ^ p
	 * @throws ArithmeticException
	 */
	public NumberValue pow(double p)
	{
		try
		{
			return new NumberValue(Math.pow(approx(), p));
		} catch (Exception e)
		{
			throw new ArithmeticException("NumberValue: pow(double) method numeric exception.");
		}
	}
		
	/**
	 * Returns a double holding an approximation of the current value.
	 * @return The approximation of the current value.
	 */
	public double approx()
	{
		double retval;
		
		if (exact)
			retval = num.doubleValue()/den.doubleValue();
		else
			retval = val;
		
		return retval;
	}
	
	/**
	 * If the value is in exact mode this will reduce the fraction to lowest terms.
	 */
	public void reduce()
	{
		if (exact)
		{
			BigInteger gcdval = num.gcd(den);
			num = num.divide(gcdval);
			den = den.divide(gcdval);
			if (den.compareTo(BigInteger.ZERO) == -1)
			{
				BigInteger negOne = new BigInteger("-1");
				num = num.multiply(negOne);
				den = den.multiply(negOne);
			}
		}
	}

	/**
	 * Determines if the current value is negative or not. 
	 * @return true if the current number is negative and false otherwise.
	 */
	public boolean isNegative()
	{
		return !isPos();
	}

	/**
	 * Determines if the current value is positive or not. 
	 * @return true if the current number is positive and false otherwise.
	 */
	public boolean isPositive()
	{
		return isPos();
	}
	
	/**
	 * Determines if the current value is less than the input number n.
	 * @param n NumberValue to be compared to the current number.
	 * @return true if the current value is less than n and false otherwise.
	 */
	public boolean lt(NumberValue n)
	{
		return (approx() < n.approx());
	}	

	/**
	 * Determines if the current value is less than or equal to the input number n.
	 * @param n NumberValue to be compared to the current number.
	 * @return true if the current value is less than or equal to n and false otherwise.
	 */
	public boolean le(NumberValue n)
	{
		return ((approx() < n.approx()) || equals(n));
	}	

	/**
	 * Determines if the current value is greater than the input number n.
	 * @param n NumberValue to be compared to the current number.
	 * @return true if the current value is greater than n and false otherwise.
	 */
	public boolean gt(NumberValue n)
	{
		return (approx() < n.approx());
	}	

	/**
	 * Determines if the current value is greater than or equal to the input number n.
	 * @param n NumberValue to be compared to the current number.
	 * @return true if the current value is greater than or equal to n and false otherwise.
	 */
	public boolean ge(NumberValue n)
	{
		return ((approx() > n.approx()) || equals(n));
	}	

	/**
	 * Determines if the current value is equal to the input number n.
	 * @param n NumberValue to be compared to the current number.
	 * @return true if the current value is equal to n and false otherwise.
	 */
	public boolean equals(NumberValue n)
	{
		if (exact && n.isExact())
		{
			reduce();
			n.reduce();
			if ((num.compareTo(n.getNum()) == 0) && (den.compareTo(n.getDen()) == 0))
				return true;
			else
				return false;
		}
		else
			return (approx() == n.approx());
	}	
	
	/**
	 * Returns the numerator of the rational number stored in the current number.
	 * @return The numerator of the rational number stored in the current number.
	 */
	public BigInteger getNum()
	{
		return num;
	}

	/**
	 * Returns the denominator of the rational number stored in the current number.
	 * @return The denominator of the rational number stored in the current number.
	 */
	public BigInteger getDen()
	{
		return den;	
	}

	/**
	 * Creates and returns a copy of the current number.
	 * @return A NumberValue object that is a copy of the current number.
	 */
	public synchronized NumberValue clone()
	{
		if (exact)
			return new NumberValue(num, den);

		return new NumberValue(val);
	}	

	/**
	 * Returns a string representing the value of the current number.  If the
	 * number is in exact mode then the string will represent a rational number
	 * and if the current number is in approximate form the method will return a 
	 * decimal number.
	 * @return A string representing the value of the current number.  If the
	 * number is in exact mode then the string will represent a rational number
	 * and if the current number is in approximate form the method will return a 
	 * decimal number.
	 */
	public String toString()
	{
		reduce();
		String retstr = "";
		if (exact)
		{
			if (den.compareTo(BigInteger.ONE) == 0)
				retstr = num.toString();
			else
				retstr = num.toString() + "/" + den.toString();
		}
		else
			retstr = "" + val;
			
		return retstr;
	}

	/**
	 * Returns the LaTeX code for the current number.
	 * @return The LaTeX code for the current number.
	 */
	public String toLaTeXString()
	{
		reduce();
		String retstr = "";
		if (exact)
		{
			if (den.compareTo(BigInteger.ONE) == 0)
				retstr = num.toString();
			else
			{
				if (isPos())
					retstr = " \\frac{" + num.toString() + "}{" + den.toString() + "} ";
				else
					retstr = " - \\frac{" + num.negate().toString() + "}{" + den.toString() + "} ";
			}
		}
		else
			retstr = "" + val;
			
		return retstr;
	}

	/**
	 * Calculates n! and returns the result as a NumberValue object.
	 * @param n the number to be factorialed.
	 * @return The result of n! as a NumberValue object.
	 */
	public static NumberValue factorial(long n)
	{
		if ((n == 0) || (n == 1)) return NumberValue.makeOne();
		return (new NumberValue(n, 1)).multiply(factorial(n-1));
	}

}
