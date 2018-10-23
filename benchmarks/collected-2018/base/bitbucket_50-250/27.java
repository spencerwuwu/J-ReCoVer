// https://searchcode.com/api/result/58515101/

// Fall 2010 HW4 starter kit
// By Daniel D. McCracken
// with additions from
// Group 9
// by Chaya Wigder
// Febin Moideen
// and Jonathan Do

package hw4;

import java.math.BigInteger;

public class RationalBigInteger {
	private BigInteger numerator, denominator;

	// no-argument constructor
	public RationalBigInteger() {
		numerator = new BigInteger("1");
		denominator = new BigInteger("1");
	}

	// one-argument constuctor, for String
	public RationalBigInteger(String theNumerator) {
		numerator = new BigInteger(theNumerator);
		denominator = new BigInteger("1");
	}

	// two-argument constructor, for two strings
	public RationalBigInteger(String theNumerator, String theDenominator) {
		numerator = new BigInteger(theNumerator);
		denominator = new BigInteger(theDenominator);
		reduce();
	}

	// one-argument constructor, RaionalBigInteger
	public RationalBigInteger(RationalBigInteger rbi) {
		numerator = rbi.numerator;
		denominator = rbi.denominator;
	}

	// two-argument constructor, for two BigIntegers
	public RationalBigInteger(BigInteger theNumerator, BigInteger theDenominator) {
		numerator = theNumerator;
		denominator = theDenominator;
		reduce();
	}

	// reduce fractions
	public void reduce() {

	}

	// add two RationalBigInteger numbers
	public RationalBigInteger add(RationalBigInteger right) {
		BigInteger ad = numerator.multiply(right.denominator);
		BigInteger bc = denominator.multiply(right.numerator);
		BigInteger resultNumerator = ad.add(bc);
		BigInteger resultDenominator = denominator.multiply(right.denominator);
		return new RationalBigInteger(resultNumerator, resultDenominator);
	}

	// Stuff deleted here by ddm

	// multiply two RationalBigInteger numbers
	public RationalBigInteger multiply(RationalBigInteger right) {
		BigInteger resultNumerator = numerator.multiply(right.numerator);
		BigInteger resultDenominator = denominator.multiply(right.denominator);
		return new RationalBigInteger(resultNumerator, resultDenominator);
	}

	// Stuff deleted here by ddm

	// Now set up methods to use a RationalBigInteger
	// on the left and a BigInteger on the right.

	// Add a BigInteger to a RationalBigInteger
	public RationalBigInteger add(BigInteger right) {
		BigInteger resultNumerator = numerator.add(denominator.multiply(right));
		return new RationalBigInteger(resultNumerator, denominator);
	}

	// Stuff deleted here by ddm

	// Multiply a RationalBigInteger by a BigInteger
	public RationalBigInteger multiply(BigInteger right) {
		BigInteger resultNumerator = numerator.multiply(right);
		return new RationalBigInteger(resultNumerator, denominator);
	}

	public RationalBigInteger divide(BigInteger right) {
    	BigInteger resultDenominator = denominator.multiply(right);
    	return new RationalBigInteger(numerator, resultDenominator);
    }
	
	public RationalBigInteger divide(RationalBigInteger right) {
		BigInteger resultNumerator = numerator.multiply(right.denominator);
		BigInteger resultDenominator = denominator.multiply(right.numerator);
		return new RationalBigInteger(resultNumerator, resultDenominator);
	}
	
	public void reciprocal() {
		BigInteger temp = numerator;
		numerator = denominator;
		denominator = temp;
	}

	// Stuff deleted here by ddm

	// More stuff deleted; may be wrong

	// Stuff deleted here by ddm

	public String toString(int numberDecPlaces) {
		BigInteger TEN = new BigInteger("10");
		BigInteger n = numerator;
		BigInteger d = denominator;
		int q = 0;
		String string;
		while (n.compareTo(d) > -1) {
			q++;
			n = n.subtract(d);
		}
		string = Integer.toString(q);
		string += ".";
		n = n.multiply(TEN);
		for (int p = 0; p < numberDecPlaces; p++) {
			q = 0;
			while (n.compareTo(d) > -1) {
				q++;
				n = n.subtract(d);
			}
			string += Integer.toString(q);
			n = n.multiply(TEN);
		}
		return string;
	}
}
