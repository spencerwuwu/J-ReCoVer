// https://searchcode.com/api/result/13906384/

package mx.uacam.fdi.io.simplex.resolvedor.mate;

/* File: Simplex.java          */
/* Copyright (C) 1997 K. Ikeda */
public class RationalNumber {

    public int numerator;
    public int denominator;

    public RationalNumber() {
        this(0);
    }

    public RationalNumber(int numerator) {
        this(numerator, 1);
    }

    public RationalNumber(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
        reduce();
    }

    public RationalNumber(String aString) {
        int k = aString.indexOf('/');
        if (k > 0) {
            denominator = Integer.valueOf(aString.substring(k + 1)).intValue();
            aString = aString.substring(0, k);
        } else {
            denominator = 1;
        }
        numerator = Integer.valueOf(aString).intValue();
        reduce();
    }

    public int euclid(int a, int b) {
        int q, r;

        if (a < 0) {
            a = -a;
        }
        if (b < 0) {
            b = -b;
        }
        if (b == 0) {
            if (a == 0) {
                return -1;
            } else {
                return a;
            }
        }
        for (;;) {
            q = a / b;
            r = a % b;
            if (r == 0) {
                break;
            }
            a = b;
            b = r;
        }
        return b;
    }

    public boolean reduce() {
        int c;

        if ((c = euclid(numerator, denominator)) < 0) {
            return false;
        }
        if (denominator < 0) {
            c *= -1;
        }
        numerator /= c;
        denominator /= c;
        return true;
    }

    public void set(int numerator) {
        this.numerator = numerator;
        this.denominator = 1;
    }

    public void set(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public void set(RationalNumber aRationaNumeber) {
        numerator = aRationaNumeber.numerator;
        denominator = aRationaNumeber.denominator;
    }

    public void mul(RationalNumber aRationalNumber) {
        aRationalNumber.reduce();
        RationalNumber aa = new RationalNumber(numerator, aRationalNumber.denominator);
        RationalNumber bb = new RationalNumber(aRationalNumber.numerator, denominator);
        aa.reduce();
        bb.reduce();
        numerator = aa.numerator * bb.numerator;
        denominator = aa.denominator * bb.denominator;
    }

    public void div(RationalNumber aRationalNumber) {
        aRationalNumber.reduce();
        RationalNumber aa = new RationalNumber(numerator, aRationalNumber.numerator);
        RationalNumber bb = new RationalNumber(aRationalNumber.denominator, denominator);
        aa.reduce();
        bb.reduce();
        numerator = aa.numerator * bb.numerator;
        denominator = aa.denominator * bb.denominator;
    }

    public void inv() {
        int swap = numerator;
        numerator = denominator;
        denominator = swap;
        reduce();
    }

    public boolean plus(RationalNumber aRationalNumber) {
        int c, x, y;

        c = euclid(denominator, aRationalNumber.denominator);
        if (c < 0) {
            return false;
        }
        if ((x = aRationalNumber.denominator / c * numerator + denominator / c * aRationalNumber.numerator) == 0) {
            x = 0;
            y = 1;
        } else {
            y = denominator / c * aRationalNumber.denominator;
        }
        numerator = x;
        denominator = y;
        this.reduce();
        return true;
    }

    public boolean minus(RationalNumber aRationalNumber) {
        int c, x, y;

        c = euclid(denominator, aRationalNumber.denominator);
        if (c < 0) {
            return false;
        }
        if ((x = aRationalNumber.denominator / c * numerator - denominator / c * aRationalNumber.numerator) == 0) {
            x = 0;
            y = 1;
        } else {
            y = denominator / c * aRationalNumber.denominator;
        }
        numerator = x;
        denominator = y;
        this.reduce();
        return true;
    }

    public boolean gt(RationalNumber aRationalNumber) {
        RationalNumber c = new RationalNumber(numerator, denominator);
        c.minus(aRationalNumber);
        return c.numerator > 0;
    }

    public boolean ge(RationalNumber aRationalNumber) {
        RationalNumber c = new RationalNumber(numerator, denominator);
        c.minus(aRationalNumber);
        return c.numerator >= 0;
    }

    public boolean eq(RationalNumber aRationalNumber) {
        RationalNumber c = new RationalNumber(numerator, denominator);
        c.minus(aRationalNumber);
        return c.numerator == 0;
    }

    public boolean le(RationalNumber aRationalNumber) {
        RationalNumber c = new RationalNumber(numerator, denominator);
        c.minus(aRationalNumber);
        return c.numerator <= 0;
    }

    public boolean lt(RationalNumber aRationalNumber) {
        RationalNumber c = new RationalNumber(numerator, denominator);
        c.minus(aRationalNumber);
        return c.numerator < 0;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
}

