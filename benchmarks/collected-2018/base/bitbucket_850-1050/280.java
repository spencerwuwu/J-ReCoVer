// https://searchcode.com/api/result/53148134/

/*
 * Copyright (c) 2007, Manuel D. Rossetti (rossetti@uark.edu)
 *
 * Contact:
 *	Manuel D. Rossetti, Ph.D., P.E.
 *	Department of Industrial Engineering
 *	University of Arkansas
 *	4207 Bell Engineering Center
 *	Fayetteville, AR 72701
 *	Phone: (479) 575-6756
 *	Email: rossetti@uark.edu
 *	Web: www.uark.edu/~rossetti
 *
 * This file is part of the JSL (a Java Simulation Library). The JSL is a framework
 * of Java classes that permit the easy development and execution of discrete event
 * simulation programs.
 *
 * The JSL is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * The JSL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the JSL (see file COPYING in the distribution);
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA, or see www.fsf.org
 *
 */
package jsl.utilities.random.distributions;

import jsl.modeling.JSLTooManyIterationsException;
import jsl.utilities.math.*;
import jsl.utilities.random.rng.RNStreamFactory;
import jsl.utilities.random.rng.RngIfc;

/** Models random variables that have gamma distribution
 *  For more information on the gamma distribution and its related functions, see
 *  "Object-Oriented Numerical Methods" by D. Besset
 *
 */
public class Gamma extends Distribution implements ContinuousDistributionIfc, LossFunctionDistributionIfc {

    public final static int DEFAULT_MAX_ITERATIONS = 5000;

    private double myShape; // alpha

    private double myScale;  // beta

    private double myNorm;

    private static double sqrt2Pi = Math.sqrt(2 * Math.PI);

    private static double[] coefficients = {76.18009172947146, -86.50532032941677, 24.01409824083091,
        -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5};

    /** Controls the maximum number of iterations when computing
     *  the CDF and inverse CDF via the incomplete gamma function
     *  and invChiSquareDistribution method
     *
     */
    private int myMaxIterations = DEFAULT_MAX_ITERATIONS;

    /**
     * Typical meaningful precision for numerical calculations.
     */
    private double myNumericalPrecision = JSLMath.getDefaultNumericalPrecision();

    /** Creates new Gamma with shape 1.0, scale 1.0
     */
    public Gamma() {
        this(1.0, 1.0, RNStreamFactory.getDefault().getStream());
    }

    /** Constructs a gamma distribution with
     * shape = parameters[0] and scale = parameters[1]
     * @param parameters An array with the shape and scale
     */
    public Gamma(double[] parameters) {
        this(parameters[0], parameters[1], RNStreamFactory.getDefault().getStream());
    }

    /** Constructs a gamma distribution with
     * shape = parameters[0] and scale = parameters[1]
     * @param parameters An array with the shape and scale
     * @param rng
     */
    public Gamma(double[] parameters, RngIfc rng) {
        this(parameters[0], parameters[1], rng);
    }

    /** Constructs a gamma distribution with supplied shape and scale
     *
     * @param shape The shape parameter of the distribution
     * @param scale The scale parameter of the distribution
     */
    public Gamma(double shape, double scale) {
        this(shape, scale, RNStreamFactory.getDefault().getStream());
    }

    /** Constructs a gamma distribution with supplied shape and scale
     *
     * @param shape The shape parameter of the distribution
     * @param scale The scale parameter of the distribution
     * @param rng a RngIfc
     */
    public Gamma(double shape, double scale, RngIfc rng) {
        super(rng);
        setNorm(scale, shape);
        setShape(shape);
        setScale(scale);
    }

    /** Returns a new instance of the random source with the same parameters
     *  but an independent generator
     *
     * @return
     */
    public final Gamma newInstance() {
        return (new Gamma(getParameters()));
    }

    /** Returns a new instance of the random source with the same parameters
     *  with the supplied RngIfc
     * @param rng
     * @return
     */
    public final Gamma newInstance(RngIfc rng) {
        return (new Gamma(getParameters(), rng));
    }

    /** Returns a new instance that will supply values based
     *  on antithetic U(0,1) when compared to this distribution
     *
     * @return
     */
    public final Gamma newAntitheticInstance() {
        RngIfc a = myRNG.newAntitheticInstance();
        return newInstance(a);
    }

    /** Sets the shape parameter
     * @param shape The shape parameter must > 0.0
     */
    public final void setShape(double shape) {
        if (shape <= 0) {
            throw new IllegalArgumentException("Shape parameter must be positive");
        }
        myShape = shape;
        setNorm(myScale, shape);
    }

    /** Sets the scale parameter
     * @param scale The scale parameter must be > 0.0
     */
    public final void setScale(double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale parameter must be positive");
        }
        myScale = scale;
        setNorm(scale, myShape);
    }

    /** Gets the shape
     * @return The shape parameter as a double
     */
    public double getShape() {
        return myShape;
    }

    /** Gets the scale parameter
     * @return The scale parameter as a double
     */
    public double getScale() {
        return myScale;
    }

    public double getMean() {
        return myShape * myScale;
    }

    public double getMoment2() {
        // theta is scale, alpha is shape
        return myScale * getMean() * (myShape + 1.0);
    }

    public double getMoment3() {
        return myScale * getMoment2() * (myShape + 2.0);
    }

    public double getMoment4() {
        return myScale * getMoment3() * (myShape + 3.0);
    }

    public double getMoment(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("The moment should be >= 1");
        }

        if (n == 1) {
            return getMean();
        }

        double y = Math.pow(myScale, n);
        double t = Gamma.gammaFunction(myShape + n);
        double b = Gamma.gammaFunction(myShape);

        return y * (t / b);
    }

    public double getVariance() {
        return myShape * myScale * myScale;
    }

    public double cdf(double x) {
        if (x <= 0) {
            return (0.0);
        }
        return (incompleteGammaFunction(x / myScale, myShape, myMaxIterations, myNumericalPrecision));
    }

    /** Provides the inverse cumulative distribution function for the distribution
     *  This is based on a numerical routine that computes the percentage points for the chi-squared distribution
     * @param p The probability to be evaluated for the inverse, p must be [0,1] or
     * an IllegalArgumentException is thrown
     * p = 0.0 returns 0.0
     * p = 1.0 returns Double.POSITIVE_INFINITY
     * 
     * @return The inverse cdf evaluated at p
     * @see Gamma.invChiSquareDistribution
     *
     */
    public double invCDF(double p) {
        if ((p < 0.0) || (p > 1.0)) {
            throw new IllegalArgumentException("Probability must be [0,1]");
        }

        if (p <= 0.0) {
            return 0.0;
        }

        if (p >= 1.0) {
            return Double.POSITIVE_INFINITY;
        }

        double x;
        /* ...special case: exponential distribution */
        if (myShape == 1.0) {
            x = -myScale * Math.log(1.0 - p);
            return (x);
        }
        /* ...compute the gamma(alpha, beta) inverse.                   *
         *    ...compute the chi-square inverse with 2*alpha degrees of *
         *       freedom, which is equivalent to gamma(alpha, 2).       */

        double v = 2.0 * myShape;
        double g = logGammaFunction(myShape);
        double chi2 = invChiSquareDistribution(p, v, g, myMaxIterations, myNumericalPrecision);

        /* ...transfer chi-square to gamma. */
        x = myScale * chi2 / 2.0;

        return (x);
    }

    public double pdf(double x) {
        if (x > 0.0) {
            //double norm = Math.log(myScale) * myShape + logGammaFunction(myShape);
            double norm = getNorm();
            return (Math.exp(Math.log(x) * (myShape - 1.0) - x / myScale - norm));
        } else {
            return (0.0);
        }
    }

    protected final void setNorm(double scale, double shape) {
        myNorm = Math.log(scale) * shape + logGammaFunction(shape);
    }

    protected final double getNorm() {
        return myNorm;
    }

    /** Gets the kurtosis of the distribution
     * @return the kurtosis
     */
    public final double getKurtosis() {
        return (6.0 / myShape);
    }

    /** Gets the skewness of the distribution
     * @return the skewness
     */
    public final double getSkewness() {
        return (2.0 / Math.sqrt(myShape));
    }

    /** Provides a random number via the standard acceptance rejection technique
     *  see Law & Kelton for the algorithm
     * @return double a random number distributed according to the receiver.
     */
    public final double randomViaAcceptanceRejection() {
        double r;

        if (myShape > 1) {
            r = randomForAlphaGreaterThan1();
        } else if (myShape < 1) {
            r = randomForAlphaLessThan1();
        } else {
            r = randomForAlphaEqual1();
        }

        return r * myScale;
    }

    /**
     * @return double
     */
    private final double randomForAlphaEqual1() {
        return -Math.log(1 - myRNG.randU01());
    }

    /**
     * @return double
     */
    private final double randomForAlphaGreaterThan1() {
        double u1, u2, v, y, z, w;
        double a = Math.sqrt(2 * myShape - 1);
        double b = myShape - Math.log(4.0);
        double q = myShape + 1 / a;
        double d = 1 + Math.log(4.5);
        while (true) {
            u1 = myRNG.randU01();
            u2 = myRNG.randU01();
            v = a * Math.log(u1 / (1 - u1));
            y = myShape * Math.exp(v);
            z = u1 * u1 * u2;
            w = b + q * v - y;
            if (w + d - 4.5 * z >= 0 || w >= Math.log(z)) {
                return y;
            }
        }
    }

    /**
     * @return double
     */
    private final double randomForAlphaLessThan1() {
        double p, y;
        double b = (Math.E + myShape) / Math.E;

        while (true) {
            p = myRNG.randU01() * b;
            if (p > 1) {
                y = -Math.log((b - p) / myShape);
                if (myRNG.randU01() <= Math.pow(y, myShape - 1)) {
                    return y;
                }
            }
            y = Math.pow(p, 1 / myShape);
            if (myRNG.randU01() <= Math.exp(-y)) {
                return y;
            }
        }
    }

    /** Algorithm AS 91   Appl. Statist. (1975) Vol.24, P.35
     *
     * To evaluate the percentage points of the chi-squared
     * probability distribution function.
     * The default maximum number of iterations is DEFAULT_MAX_ITERATIONS
     * The default numerical precision is JSLMath.getDefaultNumericalPrecision()
     * 
     * logGammaFunction(v/2.0))
     *
     * Incorporates the suggested changes in AS R85 (vol.40(1),
     * pp.233-5, 1991)
     *
     * Auxiliary routines required: PPND = AS 111 (or AS 241) and
     * GAMMAD = AS 239.
     *
     * @param p must lie in the range [0.0,1.0]
     * @param v must be positive, degrees of freedom
     * @param g must be supplied and should be equal to
     *
     * @return The percentange point
     */
    public static double invChiSquareDistribution(double p, double v, double g) {
        return (invChiSquareDistribution(p, v, g, DEFAULT_MAX_ITERATIONS, JSLMath.getDefaultNumericalPrecision()));
    }

    /** Algorithm AS 91   Appl. Statist. (1975) Vol.24, P.35
     *
     * To evaluate the percentage points of the chi-squared
     * probability distribution function.
     *
     * logGammaFunction(v/2.0))
     *
     * Incorporates the suggested changes in AS R85 (vol.40(1),
     * pp.233-5, 1991)
     *
     * Auxiliary routines required: PPND = AS 111 (or AS 241) and
     * GAMMAD = AS 239.
     *
     * @param p must lie in the range [0.0,1.0]
     * @param v must be positive, degrees of freedom
     * @param g must be supplied and should be equal to
     * @param maxIterations maximum number of iterations for series/continued fraction evaluation
     * @epm the numerical precision for convergence of series/continued fraction evaluation
     *
     * @return The percentange point
     */
    public static double invChiSquareDistribution(double p, double v, double g, int maxIterations, double eps) {
        if (maxIterations <= 0) {
            maxIterations = DEFAULT_MAX_ITERATIONS;
        }

        if (eps < JSLMath.getMachinePrecision()) {
            eps = JSLMath.getDefaultNumericalPrecision();
        }

        if ((p < 0.0) || (p > 1.0)) {
            throw new IllegalArgumentException("Probability must be [0,1]");
        }

        if (v <= 0) {
            throw new IllegalArgumentException("Degrees of Freedom must be >= 1");
        }

        int maxit = 500;
        double aa, e, ppch, half, one,
                two, three, six, c1, c2, c3, c4, c5, c6, c7,
                c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19,
                c20, c21, c22, c23, c24, c25, c26, c27, c28, c29, c30,
                c31, c32, c33, c34, c35, c36, c37, c38, a, b, c, ch, p1, p2,
                q, s1, s2, s3, s4, s5, s6, t, x, xx;

        aa = 0.6931471806;
        e = 0.0000005;
        half = 0.5;
        one = 1.0;
        two = 2.0;
        three = 3.0;
        six = 6.0;
        c1 = 0.01;
        c2 = 0.222222;
        c3 = 0.32;
        c4 = 0.4;
        c5 = 1.24;
        c6 = 2.2;
        c7 = 4.67;
        c8 = 6.66;
        c9 = 6.73;
        c10 = 13.32;
        c11 = 60.0;
        c12 = 70.0;
        c13 = 84.0;
        c14 = 105.0;
        c15 = 120.0;
        c16 = 127.0;
        c17 = 140.0;
        c18 = 1175.0;
        c19 = 210.0;
        c20 = 252.0;
        c21 = 2264.0;
        c22 = 294.0;
        c23 = 346.0;
        c24 = 420.0;
        c25 = 462.0;
        c26 = 606.0;
        c27 = 672.0;
        c28 = 707.0;
        c29 = 735.0;
        c30 = 889.0;
        c31 = 932.0;
        c32 = 966.0;
        c33 = 1141.0;
        c34 = 1182.0;
        c35 = 1278.0;
        c36 = 1740.0;
        c37 = 2520.0;
        c38 = 5040.0;

        xx = half * v;
        c = xx - one;

        /* ...starting approximation for small chi-squared */
        if (v >= (-c5 * Math.log(p))) {
            /*....starting approximation for v less than or equal to 0.32 */
            if (v > c3) {
                /* call to algorithm AS 111 - note that p has been tested above.
                AS 241 could be used as an alternative.   */
                x = Normal.stdNormalInvCDF(p);

                /* starting approximation using Wilson and Hilferty estimate */

                p1 = c2 / v;
                ch = v * Math.pow((x * Math.sqrt(p1) + one - p1), 3.0);

                /* starting approximation for p tending to 1  */

                if (ch > (c6 * v + six)) {
                    ch = -two * (Math.log(one - p) - c * Math.log(half * ch) + g);
                }
            } else {
                ch = c4;
                a = Math.log(one - p);
                do {
                    q = ch;
                    p1 = one + ch * (c7 + ch);
                    p2 = ch * (c9 + ch * (c8 + ch));
                    t = -half + (c7 + two * ch) / p1 - (c9 + ch * (c10
                            + three * ch)) / p2;
                    ch = ch - (one - Math.exp(a + g + half * ch + c * aa)
                            * p2 / p1) / t;
                } while (Math.abs(((double) q / ch - one)) > c1);
            }
        } else {
            ch = Math.pow((p * xx * Math.exp(g + xx * aa)), (one / xx));
            if (ch < e) {
                ppch = ch;
                return (ppch);
            }
        }

        /*....call to algorithm AS 239 and calculation of seven term
        Taylor series    */
        for (int i = 1; i <= maxit; i++) {
            q = ch;
            p1 = half * ch;
            p2 = p - incompleteGammaFunction(p1, xx, maxIterations, eps);

            t = p2 * Math.exp(xx * aa + g + p1 - c * Math.log(ch));
            b = t / ch;
            a = half * t - b * c;
            s1 = (c19 + a * (c17 + a * (c14 + a * (c13 + a * (c12
                    + c11 * a))))) / c24;
            s2 = (c24 + a * (c29 + a * (c32 + a * (c33 + c35
                    * a)))) / c37;
            s3 = (c19 + a * (c25 + a * (c28 + c31 * a))) / c37;
            s4 = (c20 + a * (c27 + c34 * a) + c * (c22 + a * (c30
                    + c36 * a))) / c38;
            s5 = (c13 + c21 * a + c * (c18 + c26 * a)) / c37;
            s6 = (c15 + c * (c23 + c16 * c)) / c38;
            ch = ch + t * (one + half * t * s1 - b * c * (s1 - b
                    * (s2 - b * (s3 - b * (s4 - b * (s5 - b * s6))))));
            if (Math.abs((double) q / ch - one) > e) {
                ppch = ch;
                return (ppch);
            }
        }
        ppch = ch;
        return (ppch);

    }

    /** Computes the gamma function at x
     * @return The value of the gamma function evaluated at x
     * @param x The value to be evaluated, x must be > 0
     */
    public static double gammaFunction(double x) {

        if (x <= 0.0) {
            throw new IllegalArgumentException("Argument must be > 0");
        }

        if (x <= 1.0) {
            return (gammaFunction(x + 1.0) / x);
        } else {
            return (Math.exp(leadingFactor(x)) * series(x) * sqrt2Pi / x);
        }

    }

    /** Computes the natural logarithm of the gamma function at x.  Useful
     * when x gets large.
     * @return The natural logarithm of the gamma function
     * @param x The value to be evaluated, x must be > 0
     */
    public static double logGammaFunction(double x) {

        if (x <= 0.0) {
            throw new IllegalArgumentException("Argument must be > 0");
        }

        if (x <= 1.0) {
            return (logGammaFunction(x + 1.0) - Math.log(x));
        } else {
            return (leadingFactor(x) + Math.log(series(x) * sqrt2Pi / x));
        }
    }

    /** Computes the incomplete gamma function at x
     * @return the value of the incomplete gamma function evaluated at x
     * @param x The value to be evaluated, must be > 0
     * @param alpha must be > 0
     * @param maxIterations maximum number of iterations for series/continued fraction evaluation
     * @epm the numerical precision for convergence of series/continued fraction evaluation
     */
    public static double incompleteGammaFunction(double x, double alpha, int maxIterations, double eps) {
        if (maxIterations <= 0) {
            maxIterations = DEFAULT_MAX_ITERATIONS;
        }

        if (eps < JSLMath.getMachinePrecision()) {
            eps = JSLMath.getDefaultNumericalPrecision();
        }

        if (x < 0.0) {
            throw new IllegalArgumentException("Argument x must be >= 0");
        }

        if (x == 0.0) {
            return (0.0);
        }

        if (alpha <= 0.0) {
            throw new IllegalArgumentException("Argument alpha must be > 0");
        }

        if (x < (alpha + 1.0)) {
            return (incGammaSeries(x, alpha, maxIterations, eps));  // use series expansion
        } else {
            return (1.0 - incGammaFraction(x, alpha, maxIterations, eps));  // use continued fraction
        }
    }

    /** Computes the digamma function using AS 103
     *
     * Reference:
     * Jose Bernardo,
     * Algorithm AS 103:
     * Psi ( Digamma ) Function,
     * Applied Statistics,
     * Volume 25, Number 3, 1976, pages 315-317.
     * @param x
     * @return
     */
    public static double diGammaFunction(double x) {
        if (x < 0.0) {
            throw new IllegalArgumentException("Argument x must be >= 0");
        }
        double c = 8.5;
        double d1 = -0.5772156649;
        double r;
        double s = 0.00001;
        double s3 = 0.08333333333;
        double s4 = 0.0083333333333;
        double s5 = 0.003968253968;
        double value;
        double y;
        y = x;
        value = 0.0;
        //
        //  Use approximation if argument <= S.
        //
        if (y <= s) {
            value = d1 - 1.0 / y;
            return value;
        }
        //
        //  Reduce to DIGAMA(X + N) where (X + N) >= C.
        //
        while (y < c) {
            value = value - 1.0 / y;
            y = y + 1.0;
        }
        //
        //  Use Stirling's (actually de Moivre's) expansion if argument > C.
        //
        r = 1.0 / y;
        value = value + Math.log(y) - 0.5 * r;
        r = r * r;
        value = value - r * (s3 - r * (s4 - r * s5));

        return value;
    }

    /** Computes the digamma function
     * Mark Johnson, 2nd September 2007
     *
     * Computes the ?&#x17D; (x) or digamma function, i.e., the derivative of the
     * log gamma function, using a series expansion.
     *
     * http://www.cog.brown.edu/~mj/code/digamma.c
     *
     * @param x
     * @return
     */
    public static double digamma(double x) {
        if (x < 0.0) {
            throw new IllegalArgumentException("Argument x must be >= 0");
        }
        double result = 0, xx, xx2, xx4;
        for (; x < 7; ++x) {
            result -= 1 / x;
        }
        x -= 1.0 / 2.0;
        xx = 1.0 / x;
        xx2 = xx * xx;
        xx4 = xx2 * xx2;
        result += Math.log(x) + (1. / 24.) * xx2 - (7.0 / 960.0) * xx4 + (31.0 / 8064.0) * xx4 * xx2 - (127.0 / 30720.0) * xx4 * xx4;
        return result;
    }

    /** Evaluates the incomple gamma series
     *
     * @param x
     * @param alpha
     * @param maxIterations
     * @param eps
     * @return
     */
    private static double incGammaSeries(double x, double alpha, int maxIterations, double eps) {
        int n;
        double sum, del, ap;

        if (x <= 0.0) {
            if (x < 0.0) {
                throw new IllegalArgumentException("Argument x must be >= 0");
            }
            return (0.0);
        } else {
            ap = alpha;
            del = 1.0 / alpha;
            sum = del;
            for (n = 1; n <= maxIterations; n++) {
                ap = ap + 1.0;
                del = del * x / ap;
                sum = sum + del;
                if ((Math.abs(del) / Math.abs(sum)) < eps) {
                    return (sum * Math.exp(-x + alpha * Math.log(x) - (logGammaFunction(alpha))));
                }
            }
        }

        throw new JSLTooManyIterationsException("Too many iterations in computing incomplete gamma function, increase max iterations via setMaxNumIterations()");
    }

    /** Evaluates the incomplete gamma fraction
     *
     * @param x
     * @param alpha
     * @param maxIterations
     * @param eps
     * @return
     */
    private static double incGammaFraction(double x, double alpha, int maxIterations, double eps) {
        int n;
        double gold = 0.0;
        double g;
        double fac = 1.0;
        double b1 = 1.0;
        double b0 = 0.0;
        double anf, ana, an, a1;
        double a0 = 1.0;

        a1 = x;

        for (n = 1; n <= maxIterations; n++) {
            an = (double) n;
            ana = an - alpha;
            a0 = (a1 + a0 * ana) * fac;
            b0 = (b1 + b0 * ana) * fac;
            anf = an * fac;
            a1 = x * a0 + anf * a1;
            b1 = x * b0 + anf * b1;
            if (!JSLMath.equal(a1, 0.0)) {// I think this is okay
                fac = 1.0 / a1;
                g = b1 * fac;
                if (Math.abs((g - gold) / g) < eps) {
                    return (Math.exp(-x + alpha * Math.log(x) - logGammaFunction(alpha)) * g);
                }
                gold = g;
            }
        }
        throw new JSLTooManyIterationsException("Too many iterations in computing incomplete gamma function, increase max iterations via setMaxNumIterations()");
    }

    /**
     * @return double		value of the series in Lanczos formula.
     * @param x double
     */
    private static double series(double x) {
        double answer = 1.000000000190015;
        double term = x;
        for (int i = 0; i < 6; i++) {
            term += 1;
            answer += coefficients[i] / term;
        }
        return answer;
    }

    private static double leadingFactor(double x) {
        double temp = x + 5.5;
        return Math.log(temp) * (x + 0.5) - temp;
    }

    /** Gets the maximum number of iterations for the gamma functions
     * 
     * @return
     */
    public final int getMaxNumIterations() {
        return (myMaxIterations);
    }

    /** Sets the maximum number of iterations for the gamma functions
     * 
     * @param iterations
     */
    public final void setMaxNumIterations(int iterations) {
        if (iterations < DEFAULT_MAX_ITERATIONS) {
            myMaxIterations = DEFAULT_MAX_ITERATIONS;
        } else {
            myMaxIterations = iterations;
        }
    }

    /** Gets the numerical precision used in computing the gamma functions
     * 
     * @return
     */
    public final double getNumericalPrecision() {
        return (myNumericalPrecision);
    }

    /** Sets the numerical precision used in computing the gamma functions
     * 
     * @param iterations
     */
    public final void setNumericalPrecision(double precision) {
        if (precision < JSLMath.getMachinePrecision()) {
            myNumericalPrecision = JSLMath.getDefaultNumericalPrecision();
        } else {
            myNumericalPrecision = precision;
        }
    }

    /** Sets the parameters for the distribution with
     * shape = parameters[0] and scale = parameters[1]
     *
     * @param parameters an array of doubles representing the parameters for
     * the distribution
     */
    public void setParameters(double[] parameters) {
        setShape(parameters[0]);
        setScale(parameters[1]);
    }

    /** Gets the parameters for the distribution
     *
     * @return Returns an array of the parameters for the distribution
     */
    public double[] getParameters() {
        double[] param = new double[2];
        param[0] = myShape;
        param[1] = myScale;
        return (param);
    }

    /** Computes the parameters (shape and scale) by matching to mean and variance
     *  element[0] = shape
     *  element[1] = scale
     * 
     * @param mean must be > 0
     * @param var must be > 0
     * @return
     */
    public static double[] getParametersFromMeanAndVariance(double mean, double var) {
        if (mean <= 0.0) {
            throw new IllegalArgumentException("The mean must be > 0");
        }
        if (var <= 0.0) {
            throw new IllegalArgumentException("The mean must be > 0");
        }

        double[] param = new double[2];

        double shape = (mean * mean) / (var);
        double scale = var / mean;

        param[0] = shape;
        param[1] = scale;

        return param;
    }

    public double firstOrderLossFunction(double x) {
        if (x <= 0.0) {
            return getMean() - x;
        }
        double mu = 1.0 / myScale;
        double g1 = ((myShape - mu * x) * complementaryCDF(x) + x * pdf(x)) / mu;
        return g1;
    }

    public double secondOrderLossFunction(double x) {
        if (x <= 0.0) {
            double m = getMean();
            double m2 = getVariance() + m * m;
            double f2 = 0.5 * (m2 - 2.0 * x * m + x * x);
            return f2;
        }
        double mu = 1.0 / myScale;
        double g2 = ((myShape - mu * x) * (myShape - mu * x) + myShape) * complementaryCDF(x);
        g2 = g2 + (myShape - mu * x + 1.0) * x * pdf(x);
        g2 = (0.5 * g2) / (mu * mu);
        return g2;
    }

    public static void main(String args[]) {
        double shape = 0.1;
        double scale = 8.0;
        Gamma g = new Gamma(shape, scale);
        System.out.println("shape (alpha) = " + g.getShape());
        System.out.println("scale (beta)  = " + g.getScale());
        System.out.println("mean = " + g.getMean());
        System.out.println("var = " + g.getVariance());
        double x = 9.0;
        System.out.println("g at  " + x + " = " + g.pdf(x));
        System.out.println("G at  " + x + " = " + g.cdf(x));
        System.out.println("G0 at  " + x + " = " + g.complementaryCDF(x));
        System.out.println("G1 at  " + x + " = " + g.firstOrderLossFunction(x));
        System.out.println("G2 at  " + x + " = " + g.secondOrderLossFunction(x));

        System.out.println("digamma(1) = " + Gamma.diGammaFunction(1.0));
        System.out.println("digamma(1) = " + Gamma.digamma(1.0));

        // test digamma function
        int n = 20;
        x = 0.0;
        double delta = 0.1;
        for (int i = 1; i <= n; i++) {
            x = x + delta;
            //System.out.println("digamma(x=" + x +") = " + Gamma.diGammaFunction(x));
            System.out.println("digamma(x=" + x + ") = " + Gamma.digamma(x));
        }
    }
}

