// https://searchcode.com/api/result/58515099/

package rbi;
// Fall 2010 HW4 starter kit

import java.math.BigInteger;


public class RationalBigInteger_Feb {
    private BigInteger numerator, denominator;
    
    // no-argument constructor
    public RationalBigInteger_Feb()
    {
        numerator = new BigInteger("1");
        denominator = new BigInteger("1");
    }
    
    // one-argument constuctor, for String
    public RationalBigInteger_Feb( String theNumerator)
    {
        numerator = new BigInteger(theNumerator);
        denominator = new BigInteger("1");
    }
    // two-argument constructor, for two strings
    public RationalBigInteger_Feb( String theNumerator, String theDenominator )
    {
        numerator = new BigInteger(theNumerator);
        denominator = new BigInteger(theDenominator);
        //reduce();
    }
    
    // one-argument constructor, RaionalBigInteger
    public RationalBigInteger_Feb(RationalBigInteger_Feb rbi) {
        numerator = rbi.numerator; 
        denominator = rbi.denominator;
    }
    
    // two-argument constructor, for two BigIntegers
    public RationalBigInteger_Feb( BigInteger theNumerator, BigInteger theDenominator )
    {
        numerator = theNumerator;
        denominator = theDenominator;
        //reduce();
    }
    
    // add two RationalBigInteger_Feb numbers
    public RationalBigInteger_Feb add( RationalBigInteger_Feb right )
    {      
        BigInteger ad = numerator.multiply(right.denominator);
        BigInteger bc = denominator.multiply(right.numerator);
        BigInteger resultNumerator = ad.add(bc);
        BigInteger resultDenominator = 
            denominator.multiply(right.denominator);
        return new RationalBigInteger_Feb( resultNumerator, 
                resultDenominator );
    }
    
    
    // Stuff deleted here by ddm
    
    // multiply two RationalBigInteger_Feb numbers
    public RationalBigInteger_Feb multiply( RationalBigInteger_Feb right )
    {
        BigInteger resultNumerator = 
            numerator.multiply(right.numerator);
        BigInteger resultDenominator = 
            denominator.multiply(right.denominator);      
        return new RationalBigInteger_Feb( resultNumerator, 
                resultDenominator );
    }
    
    public RationalBigInteger_Feb divide( RationalBigInteger_Feb right )
    {
        BigInteger resultNumerator = 
            numerator.multiply(right.denominator);
        BigInteger resultDenominator = 
            denominator.multiply(right.numerator);      
        return new RationalBigInteger_Feb( resultNumerator, 
                resultDenominator );
    }
    // Stuff deleted here by ddm
    
   
    // Now set up methods to use a RationalBigInteger_Feb
    // on the left and a BigInteger on the right.
    
    // Add a BigInteger to a RationalBigInteger_Feb 
    public RationalBigInteger_Feb add( BigInteger right )
    {      
        BigInteger resultNumerator = 
            numerator.add(denominator.multiply(right));    
        return new RationalBigInteger_Feb( resultNumerator, 
                denominator);
    }
    
    // Stuff deleted here by ddm

    
    // Multiply a RationalBigInteger_Feb by a BigInteger
    public RationalBigInteger_Feb multiply(BigInteger right){
        BigInteger resultNumerator  
        = numerator.multiply(right);
        return new RationalBigInteger_Feb( resultNumerator, 
                denominator);
    }
    
    // Stuff deleted here by ddm
  
    // More stuff deleted; may be wrong
    
    // Stuff deleted here by ddm
    
    public RationalBigInteger_Feb inverse()
    {
    	return new RationalBigInteger_Feb(this.denominator, this.numerator);
    }

    public String toString(int numberDecPlaces) {
        BigInteger TEN = new BigInteger("10");
        BigInteger n = numerator;
        BigInteger d = denominator;
        int q = 0;
        String string;
        while (n.compareTo(d) > -1){
            q++;
            n = n.subtract(d);
        }
        string = Integer.toString(q);
        string += ".";
        n = n.multiply(TEN);
        for (int p = 0 ; p < numberDecPlaces; p++){
            q = 0;
            while (n.compareTo(d) > -1){
                q++;
                n = n.subtract(d);
            }
            string += Integer.toString(q);
            n = n.multiply(TEN);
        }
        return string;
    }
    
    public void printDecimalFormOfRBI (int numberDecPlaces)
    {
    	System.out.println(this.toString(numberDecPlaces));
    }
}
