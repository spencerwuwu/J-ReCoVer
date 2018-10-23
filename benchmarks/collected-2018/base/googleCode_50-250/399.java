// https://searchcode.com/api/result/13017607/

package streme.lang.data;

/**
 * Based on code by Robert Sedgewick and Kevin Wayne.
 * http://www.cs.princeton.edu/introcs/92symbolic/BigRational.java.html
 */
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class BigRational implements Comparable<BigRational>
{

  public static Object checkRatOrIntRemainder(BigInteger denominator, BigInteger[] rat)
  {
    if (rat[1].equals(BigInteger.ZERO))
    {
      return rat[0];
    }
    else
    {
      return new BigRational(rat[1], denominator);
    }
  }

  public static Object checkRatOrInt(BigRational rat)
  {
    if (rat.getDenominator().equals(BigInteger.ONE))
    {
      return rat.getNumerator();
    }
    else
    {
      return rat;
    }
  }

  public final static BigRational ZERO = new BigRational(0);
  private BigInteger num; // the numerator
  private BigInteger den; // the denominator

  // create and initialize a new BigRational object
  public BigRational(int numerator, int denominator)
  {
    this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  // create and initialize a new BigRational object
  public BigRational(int numerator)
  {
    this(numerator, 1);
  }

  // create and initialize a new BigRational object from a string, e.g., "-343/1273"
  public BigRational(String s)
  {
    String[] tokens = s.split("/");
    if (tokens.length == 2)
      init(new BigInteger(tokens[0]), new BigInteger(tokens[1]));
    else if (tokens.length == 1)
      init(new BigInteger(tokens[0]), BigInteger.ONE);
    else
      throw new NumberFormatException(s);
  }

  // create and initialize a new BigRational object
  public BigRational(BigInteger numerator, BigInteger denominator)
  {
    init(numerator, denominator);
  }

  public BigRational(BigInteger numerator)
  {
    init(numerator, BigInteger.ONE);
  }

  private void init(BigInteger numerator, BigInteger denominator)
  {
    // deal with x / 0
    if (denominator.equals(BigInteger.ZERO))
    {
      throw new NumberFormatException("Denominator is zero");
    }
    // reduce fraction
    BigInteger g = numerator.gcd(denominator);
    num = numerator.divide(g);
    den = denominator.divide(g);
    // to ensure invariant that denominator is positive
    if (den.compareTo(BigInteger.ZERO) < 0)
    {
      den = den.negate();
      num = num.negate();
    }
  }

  // return string representation of (this)
  public String toString()
  {
    if (den.equals(BigInteger.ONE))
      return num + "";
    else
      return num + "/" + den;
  }

  // return { -1, 0, + 1 } if a < b, a = b, or a > b
  public int compareTo(BigRational b)
  {
    BigRational a = this;
    return a.num.multiply(b.den).compareTo(a.den.multiply(b.num));
  }

  // is this BigRational negative, zero, or positive?
  public boolean isZero()
  {
    return compareTo(ZERO) == 0;
  }

  public boolean isPositive()
  {
    return compareTo(ZERO) > 0;
  }

  public boolean isNegative()
  {
    return compareTo(ZERO) < 0;
  }

  // is this Rational object equal to y?
  public boolean equals(Object y)
  {
    if (y == this)
      return true;
    if (y == null)
      return false;
    if (y.getClass() != this.getClass())
      return false;
    BigRational b = (BigRational) y;
    return compareTo(b) == 0;
  }

  // hashCode consistent with equals() and compareTo()
  public int hashCode()
  {
    return this.toString().hashCode();
  }

  // return a * b
  public BigRational multiply(BigRational b)
  {
    BigRational a = this;
    return new BigRational(a.num.multiply(b.num), a.den.multiply(b.den));
  }

  // return a + b
  public BigRational add(BigRational b)
  {
    BigRational a = this;
    BigInteger numerator = a.num.multiply(b.den).add(b.num.multiply(a.den));
    BigInteger denominator = a.den.multiply(b.den);
    return new BigRational(numerator, denominator);
  }

  // return -a
  public BigRational negate()
  {
    return new BigRational(num.negate(), den);
  }

  // return a - b
  public BigRational subtract(BigRational b)
  {
    BigRational a = this;
    return a.add(b.negate());
  }

  // return 1 / a
  public BigRational reciprocal()
  {
    return new BigRational(den, num);
  }

  // return a / b
  public BigRational divide(BigRational b)
  {
    BigRational a = this;
    return a.multiply(b.reciprocal());
  }

  public BigInteger getDenominator()
  {
    return den;
  }

  public BigInteger getNumerator()
  {
    return num;
  }

  public BigDecimal toBigDecimal()
  {
    return new BigDecimal(getNumerator()).divide(new BigDecimal(getDenominator()), 18, RoundingMode.HALF_UP);
  }
}

