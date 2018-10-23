// https://searchcode.com/api/result/14182609/

package timing;

//-------------------------------------------------------
//An adaptation of Doug Lea's Fraction.java with
//BigInteger (instead of long) numerator and denominator
//-------------------------------------------------------

import java.math.BigDecimal;
import java.math.BigInteger;
import java.io.Serializable;

public class BigFraction implements Cloneable, Comparable, Serializable {
	protected final BigInteger numerator_;
	protected final BigInteger denominator_;
	
	//-----------------
	// Accessor methods
	//-----------------
	
	public BigInteger numerator () {
		return numerator_;
	}
	
	public BigInteger denominator () {
		return denominator_;
	}
	
	//-------------
	// Constructors
	//-------------
	
	public BigFraction (BigInteger num, BigInteger den) {
		 // Reduce to lowest terms
		 boolean numNonnegative = gteq (num, BigInteger.ZERO);
		 boolean denNonnegative = gteq (den, BigInteger.ZERO);
		 BigInteger a = numNonnegative? num : num.negate ();
		 BigInteger b = denNonnegative? den : den.negate ();
		 BigInteger g = a.gcd (b);
		 if (numNonnegative == denNonnegative) {
			 numerator_ = a.divide (g);
		 } else {
			 numerator_ = a.negate ().divide (g);
		 }
		 denominator_ = b.divide (g);
	}
	
	public BigFraction (BigFraction f) {
		numerator_ = f.numerator();
		denominator_ = f.denominator();
	}
	
	public BigFraction (String s) {
		this (new BigInteger (s.substring (0, s.indexOf ('/'))),
				new BigInteger (s.substring (s.indexOf ('/') + 1)));
	}
	
	public BigFraction (long num, long den) {
		this (new BigInteger (Long.toString (num)),
				new BigInteger (Long.toString (den)));
	}
	
	//------------------
	// Override toString
	//------------------
	
	public String toString () {
		return numerator ().toString () + "/" +
			denominator ().toString ();
	}
	
	//--------------------------------
	// Required to implement Cloneable
	//--------------------------------
	
	public Object clone () {
		return new BigFraction (this);
	}
	
	//----------------------------
	// Utility comparison routines
	//----------------------------
	
	private boolean gt (BigInteger x, BigInteger y) {
		return x.compareTo (y) > 0;
	}
	
	private boolean gteq (BigInteger x, BigInteger y) {
		return x.compareTo (y) >= 0;
	}
	
	private boolean lt (BigInteger x, BigInteger y) {
		return x.compareTo (y) < 0;
	}
	
	private boolean lteq (BigInteger x, BigInteger y) {
		return x.compareTo (y) <= 0;
	}
	
	//------------
	// Get minimum
	//------------
	
	public BigFraction min (BigFraction val) {
		if (compareTo (val) <= 0) { return this; } 
		else { return val; }
	}
	
	//------------
	// Get maximum
	//------------
	
	public BigFraction max (BigFraction val) {
		if (compareTo (val) > 0) { return this; } 
		else { return val;
	 }
	}
	
	//-------------------------------------------------------
	// Convert to BigDecimal
	// Rounding mode is any of BigDecimal.ROUND_xxx constants
	//-------------------------------------------------------
	
	public BigDecimal asBigDecimal (int scale, int roundingMode) {
		BigDecimal num = new BigDecimal (numerator ());
		BigDecimal den = new BigDecimal (denominator ());
		return num.divide (den, scale, roundingMode);
	}
	
	//------------------
	// Get negated value
	//------------------
	
	public BigFraction negate () {
		return new BigFraction (numerator ().negate (), denominator ());
	}
	
	//---------------------------
	// Get multiplicative inverse
	//---------------------------
	
	public BigFraction inverse () {
		return new BigFraction (denominator (), numerator ());
	}
	
	//----
	// Add
	//----
	
	public BigFraction add (BigFraction b) {
		BigInteger an = numerator ();
		BigInteger ad = denominator ();
		BigInteger bn = b.numerator ();
		BigInteger bd = b.denominator ();
		return new BigFraction (an.multiply (bd).add (bn.multiply (ad)), ad.multiply (bd));
	}
	
	public BigFraction add (BigInteger n) {
		return add (new BigFraction (n, BigInteger.ONE));
	}
	
	public BigFraction add (long n) {
		return add (new BigInteger (Long.toString (n)));
	}
	
	//---------
	// Subtract
	//---------
	
	public BigFraction subtract (BigFraction b) {
		BigInteger an = numerator();
		BigInteger ad = denominator();
		BigInteger bn = b.numerator();
		BigInteger bd = b.denominator();
		return new BigFraction(an.multiply (bd).subtract (bn.multiply (ad)), ad.multiply (bd));
	}
	
	public BigFraction subtract (BigInteger n) {
		return subtract (new BigFraction (n, BigInteger.ONE));
	}
	
	public BigFraction subtract (long n) {
		return subtract (new BigInteger (Long.toString (n)));
	}
	
	//---------
	// Multiply
	//---------
	
	public BigFraction multiply (BigFraction b) {
		BigInteger an = numerator();
		BigInteger ad = denominator();
		BigInteger bn = b.numerator();
		BigInteger bd = b.denominator();
		return new BigFraction (an.multiply (bn), ad.multiply (bd));
	}
	
	public BigFraction multiply (BigInteger n) {
		return multiply (new BigFraction (n, BigInteger.ONE));
	}
	
	public BigFraction multiply (long n) {
		return multiply (new BigInteger (Long.toString (n)));
	}
	
	//-------
	// Divide
	//-------
	
	public BigFraction divide (BigFraction b) {
		BigInteger an = numerator ();
		BigInteger ad = denominator ();
		BigInteger bn = b.numerator ();
		BigInteger bd = b.denominator ();
		return new BigFraction (an.multiply (bd), ad.multiply (bn));
	}
	
	public BigFraction divide (BigInteger n) {
		return divide (new BigFraction (n, BigInteger.ONE));
	}
	
	public BigFraction divide (long n) {
		return divide (new BigInteger (Long.toString (n)));
	}
	
	//---------------------------------
	// Required to implement Comparable
	//---------------------------------
	
	public int compareTo (Object other) {
		BigFraction b = (BigFraction) (other);
		BigInteger an = numerator ();
		BigInteger ad = denominator ();
		BigInteger bn = b.numerator ();
		BigInteger bd = b.denominator ();
		BigInteger left = an.multiply (bd);
		BigInteger right = bn.multiply (ad);
		if (lt (left, right)) { return -1; }
		if (left.equals (right)) { return 0; }
		else { return 1; }
	}
	
	public int compareTo (BigInteger n) {
		Object obj = new BigFraction (n, BigInteger.ONE);
		return compareTo (obj);
	}
	
	//----------------
	// Override equals
	//----------------
	
	public boolean equals (Object other) {
		return compareTo ((BigFraction) other) == 0;
	}
	
	public boolean equals (BigInteger n) {
		return compareTo (n) == 0;
	}
	
	public boolean equals (long n) {
		return equals (new BigInteger (Long.toString (n)));
	}
	
	//------------------
	// Override hashCode
	//------------------
	
	public int hashCode() {
		int num = numerator().intValue ();
		int den = denominator ().intValue ();
		return num ^ den;
	}
	
	//-----
	// Test
	//----- 
	
	public static void main (String[] args) {
		BigFraction f1, f2, f3;
	
		// Start out with a big fraction whose numerator is 3 followed by 500 zeros
		// and whose denominator is 6 followed by 500 zeros
	
		StringBuffer sb = new StringBuffer ("3");
		for (int i = 0; i < 500; i++) {
			sb.append ("0");
		}
		BigInteger bi1 = new BigInteger (sb.toString ());
		sb.setCharAt (0, '6');
		BigInteger bi2 = new BigInteger (sb.toString ());
		f1 = new BigFraction (bi1, bi2);
		System.out.println (f1);
	
		// Add 2/8 to it
	
		f2 = new BigFraction (2, 8);
		System.out.println (f2);
		f3 = f1.add (f2);
		System.out.println (f3);
	
		// Express result as decimal
		System.out.println (f3.asBigDecimal (2, BigDecimal.ROUND_UNNECESSARY));
	
		// Subtract 16/64 from result
		System.out.println (f3.subtract (new BigFraction ("16/64")));
	
		// Divide result by result * negative inverse of result (i.e. -1)
		System.out.println (f3.divide (f3.multiply (f3.inverse ().negate ())));
	}
}



