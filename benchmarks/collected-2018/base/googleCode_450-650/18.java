// https://searchcode.com/api/result/12451968/

package jeme.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * Represents a real number in Scheme. A real number is made up of a numerator
 * and denominator.
 * 
 * @author Erik Silkensen (silkense@colorado.edu)
 * @version Jun 20, 2009
 */
public class SchemeReal extends SchemeComplex 
{
    /**
     * 
     */
    public static final SchemeReal E = new SchemeReal(new BigDecimal(Math.E));
    
    /**
     * 
     */
    public static final SchemeReal PI = new SchemeReal(new BigDecimal(Math.PI));
    
    private static final int SCALE = 16;
    
    private BigDecimal inexactValue;
    
    private SchemeInteger numerator, denominator;
    
    /**
     * Subclasses are allowed to take responsibility of initializing themselves
     * by using default constructors all of the way up the tower.
     */
    protected SchemeReal() 
    {

    }

    /**
     * Creates a real number with the specified numerator and denominator. The
     * number is automatically reduced.
     * 
     * @param numerator  the numerator of this number
     * @param denominator  the denominator of this number
     */
    public SchemeReal(SchemeInteger numerator, SchemeInteger denominator) 
    {
        setNumerator(numerator);
        setDenominator(denominator);
        setRealPart(this);
        setImagPart(SchemeInteger.ZERO);
        reduce();
    }
    
    /**
     * Creates a real number with the specified value. The numerator and 
     * denominator are set according to the specified value, by parsing the 
     * String representation of the BigDecimal created with the value.
     * 
     * @param value  the value of this number
     */
    public SchemeReal(String value)
    {
        this(value, value.indexOf(".") == -1);
    }
    
    /**
     * 
     * @param value
     * @param exact
     */
    public SchemeReal(String value, boolean exact)
    {
        this(new BigDecimal(value), exact);
    }

    /**
     * Creates a real number with the specified value. The numerator and 
     * denominator are set according to the specified value, by parsing the 
     * String representation of the BigDecimal.
     * 
     * @param value  the value of this number
     */
    public SchemeReal(BigDecimal value) 
    {
        this(value, value.toPlainString().indexOf(".") == -1);
    }
    
    /**
     * 
     * @param value
     * @param exact
     */
    public SchemeReal(BigDecimal value, boolean exact)
    {
        String rep = value.toPlainString();
        if (rep.indexOf(".") != -1) {
            BigInteger numValue = new BigInteger(rep.replaceAll("[.]", ""));
            int den = rep.length() - rep.indexOf(".") - 1;
            BigInteger denValue = new BigDecimal("1e" + den).toBigInteger();
            setNumerator(new SchemeInteger(numValue, exact));
            setDenominator(new SchemeInteger(denValue));
        } else {
            setNumerator(new SchemeInteger(new BigInteger(rep), exact));
            setDenominator(SchemeInteger.ONE);
        }
        setInexactValue(value);
        setRealPart(this);
        setImagPart(SchemeInteger.ZERO);
        reduce();
    }
    
    /**
     * Returns whether or not this number is real.
     * 
     * @return <code>true</code>
     */
    public boolean isReal() 
    {
        return true;
    }

    /**
     * Returns whether or not this number is rational.  A number is currently
     * considered rational if and only if its imaginary part is zero.
     * 
     * @return  <code>true</code> if this number is rational, else 
     *     <code>false</code>
     */
    public boolean isRational() 
    {
        return getImagPart().isZero();
    }

    /**
     * Returns the numerator of this number.
     * 
     * @return  the numerator of this number
     */
    public SchemeInteger getNumerator() 
    {
        return numerator;
    }

    /**
     * Sets the numerator of this number.
     * 
     * @param numerator  the numerator of this number
     */
    protected void setNumerator(SchemeInteger numerator) 
    {
        this.numerator = numerator;
    }

    /**
     * Returns the denominator of this number.  If the denominator is currently
     * uninitialized (i.e. <code>null</code>), then it is set to one and 
     * returned.
     * 
     * @return  the denominator of this number
     */
    public SchemeInteger getDenominator() 
    {
        if (denominator == null) {
            setDenominator(SchemeInteger.ONE);
        }
        return denominator;
    }

    /**
     * Sets the denominator of this number.  It is an error to set the 
     * denominator to zero.
     * 
     * @param denominator  the denominator of this number
     * @throws  ArithmeticException  if the denominator is zero
     */
    protected void setDenominator(SchemeInteger denominator) 
    {
        if (denominator.isZero()) {
            throw new ArithmeticException("division by zero");
        }
        this.denominator = denominator;
    }

    /**
     * Sets the inexact value of this number.
     * 
     * @param value  the inexact value of this number
     */
    protected void setInexactValue(BigDecimal value) 
    {
        this.inexactValue = value;
    }

    /**
     * Returns the inexact value of this number.  If the inexact value has not
     * yet been calculated, then it is computed and returned.
     * 
     * @return  the inexact value of this number
     */
    public BigDecimal getInexactValue() 
    {
        if (inexactValue == null) {
            BigDecimal numerator = new BigDecimal(getNumerator().getValue());
            BigDecimal denominator = new BigDecimal(getDenominator().getValue());
            inexactValue = numerator.divide(denominator, MathContext.DECIMAL64);
        }
        return inexactValue;
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#isZero()
     */
    public boolean isZero() 
    {
        return getNumerator().isZero();
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#isOne()
     */
    public boolean isOne() 
    {
        return getNumerator().isOne() && getDenominator().isOne();
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#isExact()
     */
    public boolean isExact() 
    {
        return getNumerator().isExact() && getDenominator().isExact();
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#toString()
     */
    public String toString() 
    {
        if (!isExact()) {
            BigDecimal ival = getInexactValue();
            if (ival.scale() > SCALE) {
                ival = ival.setScale(SCALE, BigDecimal.ROUND_HALF_EVEN);
            }
            return ival.toString();
        } else if (getDenominator().isOne()) {
            return getNumerator().toString();
        } else {
            return getNumerator() + "/" + getDenominator();
        }
    }

    /**
     * Reduces this number to its lowest terms.
     */
    protected void reduce() 
    {
        BigInteger numerator = getNumerator().getValue();
        BigInteger denominator = getDenominator().getValue();
        BigInteger gcd = numerator.gcd(denominator);

        getNumerator().setValue(numerator.divide(gcd));
        getDenominator().setValue(denominator.divide(gcd));
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#add(jeme.math.SchemeNumber)
     */
    public SchemeNumber add(SchemeNumber augend) 
    {
        if (augend instanceof SchemeReal) {
            SchemeReal real = (SchemeReal) augend;
            SchemeInteger a = getNumerator();
            SchemeInteger b = getDenominator();
            SchemeInteger c = real.getNumerator();
            SchemeInteger d = real.getDenominator();
            SchemeInteger ad = (SchemeInteger) a.multiply(d);
            SchemeInteger bc = (SchemeInteger) b.multiply(c);

            SchemeInteger numerator = (SchemeInteger) ad.add(bc);
            SchemeInteger denominator = (SchemeInteger) b.multiply(d);        
        
            return new SchemeReal(numerator, denominator);
        } else if (augend instanceof SchemeComplex) {
            return super.add(augend);
        } else {
            throw new IllegalArgumentException("augend must be real");
        }
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#subtract(jeme.math.SchemeNumber)
     */
    public SchemeNumber subtract(SchemeNumber subtrahend) 
    {
        if (subtrahend instanceof SchemeReal) {
            SchemeReal real = (SchemeReal) subtrahend;
            SchemeInteger a = getNumerator();
            SchemeInteger b = getDenominator();
            SchemeInteger c = real.getNumerator();
            SchemeInteger d = real.getDenominator();
            SchemeInteger ad = (SchemeInteger) a.multiply(d);
            SchemeInteger bc = (SchemeInteger) b.multiply(c);
            
            SchemeInteger numerator = (SchemeInteger) ad.subtract(bc);
            SchemeInteger denominator = (SchemeInteger) b.multiply(d);

            return new SchemeReal(numerator, denominator);
        } else if (subtrahend instanceof SchemeComplex) {
            return super.subtract(subtrahend);
        } else {
            throw new IllegalArgumentException("subtrahend must be real");
        }
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#multiply(jeme.math.SchemeNumber)
     */
    public SchemeNumber multiply(SchemeNumber multiplicand) 
    {
        if (multiplicand instanceof SchemeReal) {
            SchemeReal real = (SchemeReal) multiplicand;
            SchemeInteger a = getNumerator();
            SchemeInteger b = getDenominator();
            SchemeInteger c = real.getNumerator();
            SchemeInteger d = real.getDenominator();
            
            SchemeInteger numerator = (SchemeInteger) a.multiply(c);
            SchemeInteger denominator = (SchemeInteger) b.multiply(d);
            
            return new SchemeReal(numerator, denominator);
        } else if (multiplicand instanceof SchemeComplex) {
            return super.multiply(multiplicand);
        } else {
            throw new IllegalArgumentException("multiplicand must be real");
        }
    }

    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#divide(jeme.math.SchemeNumber)
     */
    public SchemeNumber divide(SchemeNumber divisor) 
    {
        if (divisor instanceof SchemeReal) {
            SchemeReal real = (SchemeReal) divisor;
            SchemeInteger a = getNumerator();
            SchemeInteger b = getDenominator();
            SchemeInteger c = real.getNumerator();
            SchemeInteger d = real.getDenominator();

            SchemeInteger numerator = (SchemeInteger) a.multiply(d);
            SchemeInteger denominator = (SchemeInteger) b.multiply(c);
            
            return new SchemeReal(numerator, denominator);
        } else if (divisor instanceof SchemeComplex) {
            return super.divide(divisor);
        } else {
            throw new IllegalArgumentException("divisor must be real");
        }
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#log()
     */
    public SchemeNumber log()
    {
        double value = Math.log(Math.abs(getInexactValue().doubleValue()));
        
        SchemeReal ln = new SchemeReal(new BigDecimal(value));
        
        if (getInexactValue().compareTo(BigDecimal.ZERO) == -1) {
            return new SchemeComplex(ln, SchemeReal.PI);
        } else {
            return ln;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#sqrt()
     */
    public SchemeNumber sqrt()
    {
        double ival = getInexactValue().doubleValue();
        double value = Math.sqrt(Math.abs(ival));
        
        SchemeReal root = new SchemeReal(new BigDecimal(value));
        
        if (ival < 0) {
            return new SchemeComplex(SchemeInteger.ZERO, root);
        } else {
            return root;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#sin()
     */
    public SchemeNumber sin()
    {
        double value = Math.sin(getInexactValue().doubleValue());
        
        return new SchemeReal(new BigDecimal(value));
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#cos()
     */
    public SchemeNumber cos()
    {
        double value = Math.cos(getInexactValue().doubleValue());
        
        return new SchemeReal(new BigDecimal(value));
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#atan(jeme.math.SchemeNumber)
     */
    public SchemeNumber atan2(SchemeNumber y)
    {
        if (!(y instanceof SchemeReal)) {
            throw new IllegalArgumentException("atan2 expects two reals");
        }
        
        double a = getInexactValue().doubleValue();
        double b = ((SchemeReal) y).getInexactValue().doubleValue();
        
        BigDecimal value = new BigDecimal("" + Math.atan2(a, b));
        
        return new SchemeReal(value);
    }

    /**
     * Returns a new <code>SchemeComplex</code> version of this number.
     * 
     * @return  a new <code>SchemeComplex</code> version of this number
     */
    public SchemeComplex toComplex() 
    {
        return new SchemeComplex(this, SchemeInteger.ZERO);
    }

    /**
     * Returns a real version of this number.
     * 
     * @return  <code>this</code>
     */
    public SchemeReal toReal() 
    {
        return this;
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#negate()
     */
    public SchemeNumber negate()
    {
        if (isExact()) {
            SchemeInteger numerator = (SchemeInteger) getNumerator().negate();
            return new SchemeReal(numerator, getDenominator());
        } else {
            return new SchemeReal(getInexactValue().negate());
        }
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#equals(java.lang.Object)
     */
    public boolean equals(Object other)
    {
        if (other instanceof SchemeReal) {
            SchemeReal real = (SchemeReal) other;
            if (isExact()) {
                return getDenominator().equals(real.getDenominator()) &&
                    getNumerator().equals(real.getNumerator());
            } else {
                return getInexactValue().equals(real.getInexactValue());
            }
        } else if (other instanceof SchemeComplex) {
            return toComplex().equals(other);
        }
        
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see jeme.math.SchemeComplex#compareTo(jeme.math.SchemeNumber)
     */
    public int compareTo(SchemeNumber other)
    {
        if (other instanceof SchemeReal) {
            SchemeReal real = (SchemeReal) other;
            if (isExact()) {
                SchemeInteger lhs = (SchemeInteger) 
                    getNumerator().multiply(real.getDenominator());
                SchemeInteger rhs = (SchemeInteger) 
                    getDenominator().multiply(real.getNumerator());
                return lhs.compareTo(rhs);
            } else {
                return getInexactValue().compareTo(real.getInexactValue());
            }
        } else {
            throw new IllegalArgumentException("other must be real");
        }
    }
}

