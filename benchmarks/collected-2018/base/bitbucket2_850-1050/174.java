// https://searchcode.com/api/result/38949972/

package pharoslabut.logger.analyzer.cbl;


abstract class Base_Optimizer {

    public abstract Optimizer_result optimize(double[] xstart, Base_data data);
    protected int maxiter;                       
    protected double TOL2 = 1.0e-4; 
    protected double TOL3 = 1.0e-8; 
    final static protected double EPS = 2.22044604925e-16;
    public int niter;
    public int nfeval;
    public int ngradfeval;

    // Stopping criterion 1: stop if max number of iterations is exceeded
    public void setTerminationMaxIter(int _maxiter){
        maxiter=_maxiter;
    }

    // Stopping criterion 2: stop if iterations make no more progress
    //     max_i |x(i)-x_old{i}| < TOL2 AND |f-f_old| < TOL3
    public void setTerminationNoMoreProgress(double _min_delta_x, double _min_delta_f){
        TOL2=_min_delta_x;
        TOL3=_min_delta_f;
    }

}

class Optimizer_result {

    public double[] x;         // solution x
    public int niter;           // number of iterations
    public int nfeval;          // number of function evaluations
    public int ngradfeval;      // number of gradient evaluations
    public double res;          // objective function at minimum
}

// ==========================================================================================
// ==========================================================================================
abstract class Linesearchbased_Optimizer extends Base_Optimizer {
    // nested class for return argument of minbrack (Java = no pointers!)

    class minbrack_return {
        double br_min;
        double br_mid;
        double br_max;
    }

    class linemin_return {
        double x;
        double fx;
    }

    // finds scalar minimum x of d-dim function data along line pt+x*dir
    public linemin_return linemin(Base_data data, double[] pt, double[] dir, double fpt, double precision) {
        // Value of golden section (1 + sqrt(5))/2.0
        double phi = 1.6180339887499;
        double cphi = 1 - 1 / phi;
        double TOL = Math.sqrt(EPS);	// Maximal fractional precision
        double TINY = 1.0e-10;         // Can't use fractional precision when minimum is at 0

        double w, v, x, e, d, u, fx, fu, fv, fw, xm, tol1, r, p, q;
        int iter;
        int max_iter = 100;

        linemin_return result = new linemin_return();

        // Bracket the minimum
        minbrack_return ret = minbrack(data, 0.0, 1.0, fpt, pt, dir);

        // Use Brent's algorithm to find minimum
        // Initialise the points and function values
        w = ret.br_mid;   	// Where second from minimum is
        v = ret.br_mid;   	// Previous value of w
        x = v;   	// Where current minimum is
        e = 0.0; 	// Distance moved on step before last
        d = 0;
        
        fx = lineeval(data, x, pt, dir);
        fv = fx;
        fw = fx;


        for (iter = 1; iter < max_iter; iter++) {
            xm = 0.5 * (ret.br_min + ret.br_max);  // Middle of bracket
            // Make sure that tolerance is big enough
            tol1 = TOL * Math.abs(x) + TINY;
            // Decide termination on absolute precision required by linemin 
            //   quasi-Newton allows us to use lower precision during line-search
            if (Math.abs(x - xm) <= precision & ret.br_max - ret.br_min < 4 * TOL2) {
                result.fx = fx;
                result.x = x;
                return result;
            }


            // Check if step before last was big enough to try a parabolic step.
            // Note that this will fail on first iteration, which must be a golden
            // section step.
            if (Math.abs(e) > tol1) {
                // Construct a trial parabolic fit through x, v and w
                r = (fx - fv) * (x - w);
                q = (fx - fw) * (x - v);
                p = (x - v) * q - (x - w) * r;
                q = 2.0 * (q - r);
                if (q > 0.0) {
                    p = -p;
                }

                q = Math.abs(q);

                // Test if the parabolic fit is OK
                if (Math.abs(p) >= Math.abs(0.5 * q * e) | p <= q * (ret.br_min - x) | p >= q * (ret.br_max - x)) {
                    // No it isn't, so take a golden section step
                    if (x >= xm) {
                        e = ret.br_min - x;
                    } else {
                        e = ret.br_max - x;
                    }

                    d = cphi * e;
                } else {
                    // Yes it is, so take the parabolic step
                    e = d;
                    d = p / q;
                    u = x + d;
                    if (u - ret.br_min < 2 * tol1 | ret.br_max - u < 2 * tol1) //d = Math.sign(xm-x)*tol1;
                    {
                        d = ((xm - x) >= 0 ? 1 : -1) * tol1;
                    }
                }
            } else {
                // Step before last not big enough, so take a golden section step
                if (x >= xm) {
                    e = ret.br_min - x;
                } else {
                    e = ret.br_max - x;
                }

                d = cphi * e;
            }

            // Make sure that step is big enough
            if (Math.abs(d) >= tol1) {
                u = x + d;
            } else //u = x + Math.sign(d)*tol1;
            {
                u = x + (d >= 0 ? 1 : -1) * tol1;
            }

            // Evaluate function at u
            fu = lineeval(data, u, pt, dir);

            // Reorganise bracket
            if (fu <= fx) {
                if (u >= x) {
                    ret.br_min = x;
                } else {
                    ret.br_max = x;
                }
                v = w;
                w = x;
                x = u;
                fv = fw;
                fw = fx;
                fx = fu;
            } else {
                if (u < x) {
                    ret.br_min = u;
                } else {
                    ret.br_max = u;
                }


                if (fu <= fw | w == x) {
                    v = w;
                    w = u;
                    fv = fw;
                    fw = fu;
                } else if (fu <= fv | v == x | v == w) {
                    v = u;
                    fv = fu;
                }
            }
            //if (display == 1)
            //	fprintf(1, 'Cycle %4d  Error %11.6f\n', n, fx);
            //end
        }// for iter

        result.fx = fx;
        result.x = x;
        return result;
    }

    // evaluate data.compute_f in (pt+x*dir)
    public double lineeval(Base_data data, double x, double[] pt, double[] dir) {
        int i;
        double[] farg = new double[data.nVariables];
        for (i = 0; i < data.nVariables; i++) {
            farg[i] = pt[i] + x * dir[i];
        }
        nfeval++; // increment number of total function evaluations
        return (data.compute_f(farg));
    }

    public minbrack_return minbrack(Base_data data, double a, double b, double fa, double[] pt, double[] dir) {
        double phi = 1.6180339887499; // the golden section
        double TINY = 1e-10;
        double max_step = 10.0;

        double c, u, fb, fc, fu = 0, r, q, ulimit;
        boolean bracket_found;

        minbrack_return ret = new minbrack_return();

        // fb = eval f(pt + b.*dir)
        fb = lineeval(data, b, pt, dir);

        // Assume that we know going from a to b is downhill initially
        //   (usually because gradf(a) < 0).
        if (fb > fa) {
            // Minimum must lie between a and b: do golden section until we find point
            //		low enough to be middle of bracket
            c = b;
            b = a + (c - a) / phi;

            // fb = eval f(pt + b.*dir)
            fb = lineeval(data, b, pt, dir);
            while (fb > fa) {
                c = b;
                b = a + (c - a) / phi;
                // fb = eval f(pt + b.*dir)
                fb = lineeval(data, b, pt, dir);
            }
        } else {
            // There is a valid bracket upper bound greater than b
            c = b + phi * (b - a);
            // fc = eval f(pt + c.*dir)
            fc = lineeval(data, c, pt, dir);
            bracket_found = false;

            while (fb > fa) {
                // Do a quadratic interpolation (i.e. to minimum of quadratic)
                r = (b - a) * (fb - fc);
                q = (b - c) * (fb - fa);
                u = b - ((b - c) * q - (b - a) * r)
                        / (2.0 * ((q - r) >= 0 ? 1 : -1) * Math.max(Math.abs(q - r), TINY));
                ulimit = b + max_step * (c - b);

                if ((b - u) * (u - c) > 0.0) {
                    // Interpolant lies between b and c

                    // fu = eval f(pt + u.*dir)
                    fu = lineeval(data, u, pt, dir);
                    if (fu < fc) {
                        // Have a minimum between b and c
                        ret.br_min = b;
                        ret.br_mid = u;
                        ret.br_max = c;
                        return ret;
                    } else if (fu > fb) {
                        // Have a minimum between a and u
                        ret.br_min = a;
                        ret.br_mid = c;
                        ret.br_max = u;
                        return ret;
                    }

                    // Quadratic interpolation didn't give a bracket, so take a golden step
                    u = c + phi * (c - b);

                } else if ((c - u) * (u - ulimit) > 0.0) {
                    // Interpolant lies between c and limit

                    // fu = eval f(pt + u.*dir)
                    fu = lineeval(data, u, pt, dir);

                    if (fu < fc) {
                        // Move bracket along, then take golden ratio step
                        b = c;
                        c = u;
                        u = c + phi * (c - b);
                    } else {
                        bracket_found = true;
                    }

                } else if ((u - ulimit) * (ulimit - c) >= 0.0) {
                    // Limit parabolic u to maximum value
                    u = ulimit;
                } else {
                    // Reject parabolic u and use golden section step
                    u = c + phi * (b - a);
                }

                if (bracket_found == false) {
                    // fu = eval f(pt + u.*dir)
                    fu = lineeval(data, u, pt, dir);
                }

                a = b;
                b = c;
                c = u;
                fa = fb;
                fb = fc;
                fc = fu;


            } // end while
        }// end bracket found

        ret.br_mid = b;
        if (a < c) {
            ret.br_min = a;
            ret.br_max = c;
        } else {
            ret.br_min = c;
            ret.br_max = a;
        }

        return ret;

    } // end function
}

/**
 * Liang's comments:
 * 
 * Here's a Wikipedia description of QuasiNewton-optimizer:
 * 
 * In optimization, quasi-Newton methods (also known as variable metric methods)
 * are algorithms for finding local maxima and minima of functions. Quasi-Newton 
 * methods are based on Newton's method to find the stationary point of a 
 * function, where the gradient is 0. Newton's method assumes that the function 
 * can be locally approximated as a quadratic in the region around the optimum, 
 * and use the first and second derivatives (gradient and Hessian) to find the 
 * stationary point.
 * 
 * See: http://en.wikipedia.org/wiki/Quasi-Newton_method
 *
 */
class QuasiNewton_Optimizer extends Linesearchbased_Optimizer {
    // constructor

	/**
	 * Liang's comment: 
	 * 
	 * This is called by Driver.Phase2_range(...).  
	 * maxiter is initialized to be 200 (the optimizer will run 200 times optimizing the 
	 * xstart data.
	 * 
	 * @param maxiter The maximum number of iterations of this optimizer.
	 */
    QuasiNewton_Optimizer(int maxiter) {
        this.maxiter = maxiter;
    }

    // routine that does the optimization
    public Optimizer_result optimize(double[] xstart, Base_data data) {
        double[] x, xold, gradnew, gradold, p, v, u, Gv;
        double fnew, fold, gTp, maxdelta, vTp, norm2v, norm2p, vGv;
        int i, j, iter, nParam;
        double[][] hessinv;

        linemin_return lret;

        // don't need very precise line search
        double linemin_precision = 1e-2;

        Optimizer_result result = new Optimizer_result();

        // n = number of variables
        nParam = data.nVariables;

        // Minimal fractional change in f from Newton step: otherwise do a line search
        double min_frac_change = 1e-4;

        x = new double[nParam];  // Liang: An array of all the variables
        xold = new double[nParam];
        gradnew = new double[nParam];
        gradold = new double[nParam];
        p = new double[nParam];
        v = new double[nParam];
        u = new double[nParam];
        Gv = new double[nParam];

        hessinv = new double[nParam][nParam];

        //	Copy xstart to x
        System.arraycopy(xstart, 0, x, 0, nParam);

        // fnew = feval(f, x, varargin{:});
        fnew = data.compute_f(x);
        nfeval = 1;

        //gradnew = feval(gradf, x, varargin{:});
        data.compute_fgrad(x, gradnew);
        ngradfeval = 1;

        // p=-gradnew (Initial search direction)
        for (i = 0; i < nParam; i++) {
            p[i] = -gradnew[i];
        }

        // Initialize inverse Hessian to be the identity matrix
        for (i = 0; i < nParam; i++) {
            for (j = 0; j < nParam; j++) {
                if (i == j) {
                    hessinv[i][j] = 1;
                } else {
                    hessinv[i][j] = 0;
                }
            }
        }

        for (iter = 1; iter <= maxiter; iter++) {
            // xold = x
            System.arraycopy(x, 0, xold, 0, nParam);
            fold = fnew;

            // gradold = gradnew
            System.arraycopy(gradnew, 0, gradold, 0, nParam);

            // x=xold+p
            for (i = 0; i < nParam; i++) {
                x[i] = xold[i] + p[i];
            }

            // fnew = feval(f, x, varargin{:});
            fnew = data.compute_f(x);
            nfeval++;

            // This shouldn't occur, but rest of code depends on p being downhill
            // gTp=dot(gradnew,p)
            for (i = 0, gTp = 0; i < nParam; i++) {
                gTp += gradnew[i] * p[i];
            }
            if (gTp > 0) {
                gTp = -gTp;
                for (i = 0; i < nParam; i++) {
                    p[i] = -p[i]; //p = -p;
                }
                System.out.println("Search direction uphill in quasi Newton");
            }

            // Does the Newton step reduce the function value sufficiently?
            if (fnew >= fold + min_frac_change * gTp) {
                // No it doesn't
                // Minimize along current search direction: must be less than Newton step

                // find min f along line xold+alpha*p
                lret = linemin(data, xold, p, fold, linemin_precision);


                // Correct x and fnew to be the actual search point we have found
                // x = xold + lret.x * p;
                for (i = 0; i < nParam; i++) {
                    x[i] = xold[i] + lret.x * p[i];
                }

                //p = x - xold;
                for (i = 0; i < nParam; i++) {
                    p[i] = x[i] - xold[i];
                }

                fnew = lret.fx;
            }

            // Check for termination
            //    find maxdelta=max(abs(x-xold))
            for (i = 0, maxdelta = 0; i < nParam; i++) {
                if (Math.abs(x[i] - xold[i]) > maxdelta) {
                    maxdelta = Math.abs(x[i] - xold[i]);
                }
            }
            //System.out.printf("Evaluate: max |x(i)-xold(i)| = %10.9f    |f-fold|=%10.9f \n",maxdelta,Math.abs(fnew-fold));
            // Stopping criterion
            if (maxdelta < TOL2 & Math.abs(fnew - fold) < TOL3) {     
                break;
            }

            //gradnew = feval(gradf, x, varargin{:});
            data.compute_fgrad(x, gradnew);
            ngradfeval++;

            // v=gradnew-gradold
            for (i = 0; i < nParam; i++) {
                v[i] = gradnew[i] - gradold[i];
            }

            //vTp=dot(v,p)
            for (i = 0, vTp = 0; i < nParam; i++) {
                vTp += v[i] * p[i];
            }

            // compute norm2v, norm2p
            for (i = 0, norm2v = 0, norm2p = 0; i < nParam; i++) {
                norm2v += v[i] * v[i];
                norm2p += p[i] * p[i];
            }
            // Skip update to inverse Hessian if fac not sufficiently positive
            if (vTp * vTp > EPS * norm2v * norm2p) {
                // Gv = (hessinv*v')';
                for (i = 0; i < nParam; i++) {
                    for (j = 0, Gv[i] = 0; j < nParam; j++) {
                        Gv[i] += hessinv[i][j] * v[j];
                    }
                }

                // vGv = sum(v.*Gv);
                for (i = 0, vGv = 0; i < nParam; i++) {
                    vGv += v[i] * Gv[i];
                }

                // u = p./vdotp - Gv./vGv;
                for (i = 0; i < nParam; i++) {
                    u[i] = p[i] / vTp - Gv[i] / vGv;
                }

                // Use BFGS update rule
                // hessinv = hessinv + (p'*p)/vdotp - (Gv'*Gv)/vGv + vGv*(u'*u);
                for (i = 0; i < nParam; i++) {
                    for (j = 0; j < nParam; j++) {
                        hessinv[i][j] = hessinv[i][j] + p[i] * p[j] / vTp - Gv[i] * Gv[j] / vGv
                                + vGv * u[i] * u[j];
                    }
                }
            }


            //  p = -(hessinv * gradnew')';
            for (i = 0; i < nParam; i++) {
                for (j = 0, p[i] = 0; j < nParam; j++) {
                    p[i] += -hessinv[i][j] * gradnew[j];
                }
            }

            // System.out.println("Cycle " + iter + "  Function " + fnew + " maxdelta "+maxdelta+"|f-fold|"+Math.abs(fnew-fold));

        }// for iter

        // Write results
        result.x = x;
        result.nfeval = nfeval;
        result.ngradfeval = ngradfeval;
        result.niter = iter;
        result.res = fnew;   //return current functionvalue
        return result;

    }
}

/**
 * Liang's comment: "CG" might stand for "Conjugate Gradient"....
 * I commented it out because it's not being used.
 */
//class CG_Optimizer extends Linesearchbased_Optimizer {
//
//    // constructor
//    CG_Optimizer(int maxiter) {
//        this.maxiter = maxiter;
//    }
//
//    // routine that does the optimization
//    public Optimizer_result optimize(double[] xstart, Base_data data) {
//
//        double[] x, xold, gradnew, gradold, d, line_sd;
//        double fnew, gg, fold, gTd, gamma, normd, maxdelta;
//        int i, iter, nParam;
//
//        linemin_return lret;
//
//        double linemin_precision = 1e-4;
//
//        Optimizer_result result = new Optimizer_result();
//
//        // n = number of variables
//        nParam = data.nVariables;
//
//        x = new double[nParam];
//        xold = new double[nParam];
//        gradnew = new double[nParam];
//        gradold = new double[nParam];
//        d = new double[nParam];
//        line_sd = new double[nParam];
//
//        //	Copy xstart to x
//        System.arraycopy(xstart, 0, x, 0, nParam);
//
//
//        // fnew = feval(f, x, varargin{:});
//        fnew = data.compute_f(x);
//        nfeval = 1;
//
//        //gradnew = feval(gradf, x, varargin{:});
//        data.compute_fgrad(x, gradnew);
//        ngradfeval = 1;
//
//        // d=-gradnew (Initial search direction)
//        for (i = 0; i < nParam; i++) {
//            d[i] = -gradnew[i];
//        }
//
//        for (iter = 1; iter <= maxiter; iter++) {
//
//            // xold = x;
//            System.arraycopy(x, 0, xold, 0, nParam);
//            fold = fnew;
//
//            //gradold = gradnew;
//            System.arraycopy(gradnew, 0, gradold, 0, nParam);
//
//            //gg = gradold*gradold';
//            for (i = 0, gg = 0; i < nParam; i++) {
//                gg += gradold[i] * gradold[i];
//            }
//
//            // If the gradient is zero then we are done. return x with functionvalue fnew
//            if (gg == 0.0) {
//                break;
//            }
//
//            // This shouldn't occur, but rest of code depends on d being downhill
//            // gTd=dot(gradnew,d)
//            for (i = 0, gTd = 0; i < nParam; i++) {
//                gTd += gradnew[i] * d[i];
//            }
//            if (gTd > 0) {
//                for (i = 0; i < nParam; i++) {
//                    d[i] = -d[i]; //d = -d;
//                }
//                // System.out.println("Search direction uphill in cg");
//            }
//
//
//            //line_sd = d./norm(d);
//            for (i = 0, normd = 0; i < nParam; i++) {
//                normd += d[i] * d[i];
//            }
//            normd = Math.sqrt(normd);
//            for (i = 0; i < nParam; i++) {
//                line_sd[i] = d[i] / normd;
//            }
//
//            // find min f along line xold+alpha*line_sd
//            lret = linemin(data, xold, line_sd, fold, linemin_precision);
//
//            // Set x and fnew to be the actual search point we have found
//            // x = xold + lmin * line_sd;
//            for (i = 0; i < nParam; i++) {
//                x[i] = xold[i] + lret.x * line_sd[i];
//            }
//
//            fnew = lret.fx;
//
//            // Check for termination
//            //    find maxdelta=max(abs(x-xold))
//            for (i = 0, maxdelta = 0; i < nParam; i++) {
//                if (Math.abs(x[i] - xold[i]) > maxdelta) {
//                    maxdelta = Math.abs(x[i] - xold[i]);
//                }
//            }
//            if (maxdelta < TOL2 & Math.abs(fnew - fold) < TOL3) {
//                break;
//            }
//
//            //gradnew = feval(gradf, x, varargin{:});
//            data.compute_fgrad(x, gradnew);
//            ngradfeval++;
//
//            // Use Polak-Ribiere formula to update search direction
//
//            // gamma = ((gradnew - gradold)*(gradnew)')/gg;
//            for (i = 0, gamma = 0; i < nParam; i++) {
//                gamma += (gradnew[i] - gradold[i]) * gradnew[i];
//            }
//            gamma /= gg;
//
//            //d = (d .* gamma) - gradnew;
//            for (i = 0; i < nParam; i++) {
//                d[i] = d[i] * gamma - gradnew[i];
//            }
//
//            // System.out.println("Cycle " + iter + "   Function " + fnew);
//
//        }// end for iter<maxiter
//
//
//
//
//        // Write results
//        result.x = x;
//        result.nfeval = nfeval;
//        result.ngradfeval = ngradfeval;
//        result.niter = iter;
//        result.res = fnew;   //return current functionvalue
//        return result;
//    }
//}






/**
 * Liang's comment:
 * 
 * SCG = Scaled Conjugate Gradient, a supervised learning algorithm.
 * 
 * I commented it out because it is never used...
 */
//class SCG_Optimizer extends Base_Optimizer {
//
//    // constructor
//    SCG_Optimizer(int maxiter) {
//        this.maxiter = maxiter;
//    }
//
//    public Optimizer_result optimize(double[] xstart, Base_data data) {
//        // Scaled CG related
//        int nParam;
//        int iter;
//        double res;
//        int nsuccess;
//        boolean success;
//        double fold;
//        double fnow;
//        double fnew;
//        double sigma0;
//        double sigma;
//        double mu;
//        double alpha;
//        double delta;
//        double Delta;
//        double theta;
//        double kappa;
//        double beta;
//        double betamin;
//        double betamax;
//        double gamma;
//        double[] gradnew;
//        double[] gradold;
//        double[] d;
//        double[] gplus;
//        double[] xplus;
//        double[] xnew;
//        double[] x;
//
//        int i;
//        double maxd, sum;
//
//        Optimizer_result result = new Optimizer_result();
//
//
//        // n = number of variables
//        nParam = data.nVariables;
//
//        x = new double[nParam];
//        gradnew = new double[nParam];
//        gradold = new double[nParam];
//        d = new double[nParam];
//        gplus = new double[nParam];
//        xplus = new double[nParam];
//        xnew = new double[nParam];
//
//        //	Copy xstart to x
//        System.arraycopy(xstart, 0, x, 0, nParam);
//
//        sigma0 = 1.0e-4;
//
//        //
//        // Scaled Conjugated Gradients (Moller, 93; Bishop 95,06)
//        //
//
//
//        // Startwert der Iteration ist in x
//
//        //fold=eval_f(x);  // Evaluiere zu minimierende Funktion in x (hier rX)
//        fold = data.compute_f(x);
//        fnow = fold;
//        nfeval = 1;
//
//        //eval_gradf(x,gradnew); // Evaluiere Gradient der zu minim Funktion in x
//        data.compute_fgrad(x, gradnew);
//        ngradfeval = 1;
//
//
//        // gradold=gradnew
//        // memcpy ( gradold,gradnew,nParam*sizeof ( double ) );
//        System.arraycopy(gradnew, 0, gradold, 0, nParam);
//
//
//        // d=-gradnew (Initial search direction)
//        for (i = 0; i < nParam; i++) {
//            d[i] = -gradnew[i];
//        }
//
//        success = true;
//        nsuccess = 0;
//        beta = 1.0;
//        betamin = 1.0e-15;
//        betamax = 1.0e100;//100;
//        iter = 1;
//
//        theta = kappa = mu = 0;
//
//        //
//        // Main optimization loop
//        //
//        while (iter <= maxiter) {
//
//            // Calculate first and second directional derivatives
//            if (success) {
//                //mu=d*gradnew';
//                mu = 0;
//                for (i = 0; i < nParam; i++) {
//                    mu = mu + gradnew[i] * d[i];
//                }
//                if (mu >= 0) {
//                    //d=-gradnew;
//                    for (i = 0; i < nParam; i++) {
//                        d[i] = -gradnew[i];
//                    }
//                    //mu=d*gradnew';
//                    mu = 0;
//                    for (i = 0; i < nParam; i++) {
//                        mu = mu + gradnew[i] * d[i];
//                    }
//                }
//                // kappa=d*d';
//                kappa = 0;
//                for (i = 0; i < nParam; i++) {
//                    kappa = kappa + d[i] * d[i];
//                }
//
//                if (kappa < EPS) {
//                    res = fnow;
//                    break;
//                }
//
//                sigma = sigma0 / Math.sqrt(kappa);
//
//                //xplus=x+sigma*d; ( cblas_daxpy y=ax+y)
//                for (i = 0; i < nParam; i++) {
//                    xplus[i] = x[i] + sigma * d[i];
//                }
//
//
//                //eval_gradf(xplus,gplus); // Evaluiere Gradient der zu minim Funktion in xplus
//                data.compute_fgrad(xplus, gplus);
//                ngradfeval++;
//
//                //theta=(d*(gplus'-gradnew'))/sigma;
//                theta = 0;
//                for (i = 0; i < nParam; i++) {
//                    theta = theta + d[i] * (gplus[i] - gradnew[i]);
//                }
//                theta = theta / sigma;
//            }
//
//            // Increase effective curvature and evaluate step size alpha
//            delta = theta + beta * kappa;
//            if (delta <= 0) {
//                delta = beta * kappa;
//                beta = beta - theta / kappa;
//            }
//            alpha = -mu / delta;
//
//            // Calculate the comparison ratio
//            //xnew=x+alpha*d;
//            for (i = 0; i < nParam; i++) {
//                xnew[i] = x[i] + alpha * d[i];
//            }
//
//
//
//            //fnew=eval_f(xnew); // Evaluiere zu minimierende Funktion in xnew
//            fnew = data.compute_f(xnew);
//            nfeval++;
//
//            Delta = 2 * (fnew - fold) / (alpha * mu);
//            if (Delta >= 0) {
//                success = true;
//                nsuccess++;
//                fnow = fnew;
//                //x=xnew
//                //memcpy ( rX,xnew,nParam*sizeof ( double ) );
//                System.arraycopy(xnew, 0, x, 0, nParam);
//            } else {
//                success = false;
//                fnow = fold;
//            }
//
//            /*		printf ( "SCG: Iter %4d  Res %11.6f  Beta %e \n",iter,fnow,beta );*/
//            // System.out.println("SCG: Iter=" + iter + "   Res=" + fnow + "  Beta=" + beta);
//
//            if (success) {
//                for (i = 0, maxd = 0; i < nParam; i++) {
//                    if (Math.abs(d[i]) > maxd) {
//                        maxd = Math.abs(d[i]);
//                    }
//                }
//
//                //if max(abs(alpha*d)) < TOL2 & max(abs(fnew-fold)) < TOL3)
//                if (Math.abs(alpha * maxd) < TOL2 && Math.abs(fnew - fold) < TOL3) {
//                    res = fnew;
//                    System.out.println("Stop1 \n");
//                    break;
//                } else {
//                    // Update variables for new position
//                    fold = fnew;
//                    //gradold=gradnew;
//                    //memcpy ( gradold,gradnew,nParam*sizeof ( double ) );
//                    System.arraycopy(gradnew, 0, gradold, 0, nParam);
//                    //eval_gradf(x,gradnew); // Evaluiere Gradient der zu minim Funktion in x
//                    data.compute_fgrad(x, gradnew);
//                    ngradfeval++;
//
//                    // If the gradient is zero then we are done
//                    for (i = 0, sum = 0; i < nParam; i++) {
//                        sum = sum + gradnew[i] * gradnew[i];
//                    }
//                    if (Math.abs(sum) < EPS) {
//                        res = fnew;
//                        System.out.println("Stop2\n");
//                        break;
//                    }
//                }
//            }
//
//
//            // Adjust beta according to comparison ratio
//            if (Delta < 0.25) //beta=min(4.0*beta,betamax);
//            {
//                beta = (4.0 * beta < betamax ? 4.0 * beta : betamax);
//            }
//
//            if (Delta > 0.75) //beta=max(0.5*beta,betamin);
//            {
//                beta = (0.5 * beta > betamin ? 0.5 * beta : betamin);
//            }
//
//            // Update search direction using Polak-Ribiere formula, or re-start
//            // in direction of negative gradient after nparams steps.
//            if (nsuccess == nParam) {
//                //d=-gradnew;
//                for (i = 0; i < nParam; i++) {
//                    d[i] = -gradnew[i];
//                }
//                nsuccess = 0;
//            } else {
//                if (success) {
//                    //gamma=(gradold-gradnew)*gradnew'/(mu);
//                    for (i = 0, gamma = 0; i < nParam; i++) {
//                        gamma = gamma + (gradold[i] - gradnew[i]) * gradnew[i];
//                    }
//                    gamma = gamma / mu;
//
//                    //d=gamma*d-gradnew;
//                    for (i = 0; i < nParam; i++) {
//                        d[i] = gamma * d[i] - gradnew[i];
//                    }
//                }
//            }
//
//            iter++;
//
//        }//while iter<MAXITER
//
//
//        // Write results
//        result.x = x;
//        result.nfeval = nfeval;
//        result.ngradfeval = ngradfeval;
//        result.niter = iter;
//        result.res = fnow;   //return current functionvalue
//
//        return result;
//    }
//} // end SCG_Optimizer

