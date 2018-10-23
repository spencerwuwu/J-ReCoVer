// https://searchcode.com/api/result/12209566/

package music;
//hdhd

/*************************************************************************
 *  http://www.cs.princeton.edu/introcs/92symbolic/BigRational.java.html
 *
 *  Compilation:  javac BigRational.java
 *  Execution:    java BigRational
 *
 *  Immutable ADT for arbitrarily large Rational numbers.
 *
 *  Invariants
 *  ----------
 *   -  gcd(num, den) = 1, i.e., rational number is in reduced form
 *   -  den >= 1, i.e., the denominator is always a positive integer
 *   -  0/1 is the unique representation of zero
 *
 *  % java BigRational
 *  5/6
 *  1
 *  1/120000000
 *  1073741789/12
 *  1
 *  841/961
 *  -1/3
 *  0
 *  true
 *  Exception in thread "main" java.lang.RuntimeException: Denominator is zero
 *
 *************************************************************************/

import java.math.BigInteger;
import java.util.regex.*;

public class BigRational implements Comparable<BigRational> {

    public final static BigRational ZERO = new BigRational(0);
    public final static BigRational ONE = new BigRational(1);
     public final static BigRational TWO = new BigRational(2);

    private BigInteger num;   // the numerator
    private BigInteger den;   // the denominator


    // create and initialize a new BigRational object
    public BigRational(int numerator, int denominator) {
        this(new BigInteger("" + numerator), new BigInteger("" + denominator));
    }

    // create and initialize a new BigRational object
    public BigRational(int numerator) {
        this(numerator, 1);
    }

    

    /**
     * This method takes any double
     * given and turns it into an unbounded
     * rational number.
     * Take a double and floor it to get things before decimal
     * Convert the double to a string "back" and get from the decimal to the end of the string.
     * Replace the decimal point in the string System.out.println(n +" "+d);"back" then count the digits.
     * Make a new number according to the digits in the back string. Turn the back into a number
     * Multiply the front * the new number made by cunting digits, then add this to the back number.
     *
     * @param double numeratorunded
     */

    

    //Turns a double (3.55) into 355/100 then reduces with init(*,*)
    private void takeDoubleString(String numerator)
    {
        
        String[] dubstr = numerator.split("\\.");
        String  back = dubstr[1];
            //Making the denominator
            int l = back.length();
            String denoms = "1";
            while(l>0)
            {
                l--;
                denoms+="0";
            }
            //Setting up our big rational to use BigInts
            BigInteger n =new BigInteger(back);
            BigInteger d = new BigInteger(denoms);
        //Get the whole number in front of the .
        if(dubstr[0].isEmpty())
        {     
            init(n,d);
        }
        else
        {
            BigInteger front = new BigInteger(dubstr[0]);
            n = front.multiply(d).add(n);
            init(n,d);
        }
    }

    // create and initialize a new BigRational object from a string, e.g., "-343/1273"
    public BigRational(String s) {
        String[] tokens = s.split("/");
        if (tokens.length == 2)
            init(new BigInteger(tokens[0]), new BigInteger(tokens[1]));
        else if (tokens.length == 1)
        {
            if(s.contains("."))
                takeDoubleString(s);
            else
                init(new BigInteger(tokens[0]), BigInteger.ONE);
        }
            
        else
            throw new RuntimeException("Parse error in BigRational");
    }


    // create and initialize a new BigRational object
     public BigRational(BigRational i) {
         //to prevent aliasing
        this.den = new BigInteger(i.den.toString());
        this.num = new BigInteger(i.num.toString());
     }


    public BigRational(BigInteger numerator, BigInteger denominator) {
        init(numerator, denominator);
    }

    

    private void init(BigInteger numerator, BigInteger denominator) {

        // deal with x / 0
        if (denominator.equals(BigInteger.ZERO)) {
           throw new RuntimeException("Denominator is zero");
        }

        // reduce fraction
        BigInteger g = numerator.gcd(denominator);
        num = numerator.divide(g);
        den = denominator.divide(g);

        // to ensure invariant that denominator is positive
        if (den.compareTo(BigInteger.ZERO) < 0) {
            den = den.negate();
            num = num.negate();
        }
    }

    // return string representation of (this)
    public String toString() {
        if (den.equals(BigInteger.ONE)) return num + "";
        else                            return num + "/" + den;
    }

    // return { -1, 0, + 1 } if a < b, a = b, or a > b
    public int compareTo(BigRational b) {
        BigRational a = this;
        return a.num.multiply(b.den).compareTo(a.den.multiply(b.num));
    }

    public static BigRational max(BigRational a,BigRational b)
    {
        if(a.compareTo(b)==-1)return b;
        else return a;
    }
    // is this BigRational negative, zero, or positive?
    public boolean isZero()     { return compareTo(ZERO) == 0; }
    public boolean isPositive() { return compareTo(ZERO)  > 0; }
    public boolean isNegative() { return compareTo(ZERO)  < 0; }
    public double toDouble()
    {
        if(this.isZero())return 0;
        return this.den.divide(this.num).doubleValue();
    }

    public int toInt()
    {        
        double d = Double.valueOf(this.den.toString());
        double n = Double.valueOf(this.num.toString());        
        return (int) Math.round(n/d);
    }

    // is this Rational object equal to y?
    public boolean equals(Object y) {
        if (y == this) return true;
        if (y == null) return false;
        if (y.getClass() != this.getClass()) return false;
        BigRational b = (BigRational) y;
        return compareTo(b) == 0;
    }

    // hashCode consistent with equals() and compareTo()
    public int hashCode() {
        return this.toString().hashCode();
    }


    // return a * b
    public BigRational times(BigRational b) {
        BigRational a = this;
        return new BigRational(a.num.multiply(b.num), a.den.multiply(b.den));
    }

    //return a^b
    public BigRational power(BigRational b) {
        BigRational a = this;
        System.out.println("____-----__++sBIG INT POWER--------");
        System.out.println(a.toDouble());
        System.out.println(b.toDouble());
        System.out.println(Math.pow(a.toDouble(),b.toDouble()));
        return new BigRational("k");
    }

    // return a + b
    public BigRational plus(BigRational b) {

        BigInteger numerator   = this.num.multiply(b.den).add(b.num.multiply(this.den));
        BigInteger denominator = this.den.multiply(b.den);
        return new BigRational(numerator, denominator);
    }

    // return -a
    public BigRational negate() {
        return new BigRational(num.negate(), den);
    }

    // return a - b
    public BigRational minus(BigRational b) {
        BigRational a = this;
        return a.plus(b.negate());
    }

    // return 1 / a
    public BigRational reciprocal() {
        return new BigRational(den, num);
    }

    // return a / b
    public BigRational divides(BigRational b) {
        BigRational a = this;
        return a.times(b.reciprocal());
    }

    public static void main(String[] args) {
        BigRational x, y, z ,c,cd,fg;

        // 1/2 + 1/3 = 5/6
        x = new BigRational("3.14");

        System.out.println(x);
        y = new BigRational("2.2");
        System.out.println(y);
        c = new BigRational(1, 3);
        z = x.plus(y).plus(c);
        System.out.println(z);
         cd = new BigRational("1/3");
        System.out.println(x);
        fg = new BigRational("2/3");
        fg = cd.plus(fg);
         System.out.println(fg);

        // 8/9 + 1/9 = 1
        x = new BigRational(8, 9);
        y = new BigRational(1, 9);
        z = x.plus(y);
        System.out.println(z);
         int x1 = new BigRational('4').toInt();
         int x2 = new BigRational("6").toInt();
         int x3 = new BigRational("3").toInt();
         System.out.println(x1+" "+x2+" "+x3);
    }
}

