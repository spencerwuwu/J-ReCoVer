// https://searchcode.com/api/result/132699050/

/**
 * @(#)Fraction.java
 *
 *
 * @author 
 * @version 1.00 2012/9/26
 */


public class Fraction {
	int numerator;
	int denumerator;
	public Fraction(){
	}
    public Fraction(int t, int m ) {
    	if(m != 0) {
    		numerator = t;
    		denumerator = m;
    	} else {
    		numerator = 1;
    		denumerator = 1;
    		System.out.println("system error");
    	}
    }
    public void display(){
    	System.out.print( numerator);
    	System.out.print("/");
    	System.out.println(denumerator);
    }
    public void reduce(){
	int x = Math.abs(numerator);
    	int y = Math.abs(denumerator);
    	while(x != y){
    		if(x > y) {
    			x = x%y;
    			if( x == 0) x = y;
    		}
    		else{
    			 y = y%x;
    			 if( y == 0) y = x;}
    		}
    	
    	numerator = numerator/x;
    	denumerator = denumerator/x;
    }
    public Fraction plus(Fraction f){
    	Fraction sum = new Fraction();
    	sum.numerator = numerator*f.denumerator + denumerator*f.numerator;
    	sum.denumerator = denumerator*f.denumerator;
    	sum.reduce();
    	return sum;
    }
    	
    
    
}
