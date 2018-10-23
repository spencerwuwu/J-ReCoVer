// https://searchcode.com/api/result/56326731/

package org.jmatrices.dbl.decomposition;

import org.jmatrices.dbl.Matrix;
import org.jmatrices.dbl.MatrixFactory;

/**
 * SingularValueDecomposition
 * <P>
 * For an m-by-n matrix A with m >= n, the singular value decomposition is
 * an m-by-n orthogonal matrix U, an n-by-n diagonal matrix S, and
 * an n-by-n orthogonal matrix V so that A = U*S*V'.
 * </p>
 * <P>
 * The singular values, sigma[k] = S[k][k], are ordered so that
 * sigma[0] >= sigma[1] >= ... >= sigma[n-1].
 * </p>
 * <P>
 * The singular value decompostion always exists, so the constructor will
 * never fail.  The matrix condition number and the effective numerical
 * rank can be computed from this decomposition.
 * </p>
 * <p/>
 * http://kwon3d.com/theory/jkinem/svd.html
 * http://web.mit.edu/be.400/www/SVD/Singular_Value_Decomposition.htm
 * http://www.matheverywhere.com/mei/courseware/mgm/svdB/
 * http://www.netlib.org/lapack/lug/node53.html
 * http://www.mppmu.mpg.de/~schieck/svd.pdf
 * http://www.cs.ut.ee/~toomas_l/linalg/lin2/node14.html
 * </p>
 * <p/>
 * A x X = B  <br/>
 * X = A<sup>-1</sup> x B <br/>
 * A = U x S x V<sup>T </sup> (Singular value decomposition of A)         <br/>
 * A-1 = V x S<sup>-1</sup> x U<sup>T</sup> (Generalized inverse or pseudoinverse of A)  <br/>
 * X = V x S<sup>-1</sup> x U<sup>T</sup> x B        <br/>
 * </p>
 * <p><font color="red">
 * The code is basically JAMA code with modifications made to fit in the scheme of things.
 * </font></p>
 * <p/>
 * @author ppurang
 * </p>
 * Date: 12.03.2004
 * Time: 23:09:45
 */
public final class SV {
    /* ------------------------
Class variables
* ------------------------ */

    /**
     * Arrays for internal storage of U and V.
     *
     * @serial internal storage of U.
     * @serial internal storage of V.
     */
    private MatrixAdaptor U, V;

    /**
     * Array for internal storage of singular values.
     *
     * @serial internal storage of singular values.
     */
    private MatrixAdaptor s;

    /**
     * Row and column dimensions.
     *
     * @serial row dimension.
     * @serial column dimension.
     */
    private int m, n;

/* ------------------------
   Constructor
 * ------------------------ */

    /**
     * Construct the singular value decomposition
     *
     * @param matrix Rectangular matrix
     */
   //todo wantu and wantv should be specifiable.
    //todo lazy
    //todo U and V don't seem to be in order...
    public SV(Matrix matrix) {
        // Derived from LINPACK code.
        // Initialize.
        MatrixAdaptor A = new MatrixAdaptor(MatrixFactory.getMutableMatrixClone(matrix));
        m = matrix.rows();          //changed
        n = matrix.cols();          //changed
        int nu = Math.min(m, n);
        //System.out.println("nu = " + nu);

        s = new MatrixAdaptor(MatrixFactory.getMatrix(Math.min(m + 1, n), 1, matrix));

        U = new MatrixAdaptor(MatrixFactory.getMatrix(m, nu, matrix));
        V = new MatrixAdaptor(MatrixFactory.getMatrix(n, n, matrix));

        MatrixAdaptor e = new MatrixAdaptor(MatrixFactory.getMatrix(n, 1, matrix));
        MatrixAdaptor work = new MatrixAdaptor(MatrixFactory.getMatrix(m, 1, matrix));

        boolean wantu = false;
        boolean wantv = false;

        // Reduce A to bidiagonal form, storing the diagonal elements
        // in s and the super-diagonal elements in e.

        int nct = Math.min(m - 1, n);
        //System.out.println("nct = " + nct);

        int nrt = Math.max(0, Math.min(n - 2, m));
        //System.out.println("nrt = " + nrt);

        //System.out.println("looping for k = 0 to k < " + Math.max(nct, nrt));
        for (int k = 0; k < Math.max(nct, nrt); k++) {
            if (k < nct) {

                // Compute the transformer for the k-th column and
                // place the k-th diagonal in s[k].
                // Compute 2-norm of k-th column without under/overflow.
                s.setValue(k, 0, 0);
                //System.out.println("Looping from i = " + k + " to i < " + m);

                for (int i = k; i < m; i++) {
                    //System.out.println("s.setValue(k,1,Util.hypot(s.getValue(k,1), A.getValue(i,k))) " + Util.hypot(s.getValue(k, 0), A.getValue(i, k)));
                    s.setValue(k, 0, Util.hypot(s.getValue(k, 0), A.getValue(i, k)));//changed
                }
                if (s.getValue(k, 0) != 0.0) {
                    //System.out.println("Entered s.getValue(k,1)");
                    if (A.getValue(k, k) < 0.0) {
                        //System.out.println("Entered A.getValue(k,k) < 0.0");
                        //System.out.println("  s.setValue(k,1,-s.getValue(k,1)); " + -s.getValue(k, 0));

                        s.setValue(k, 0, -s.getValue(k, 0));
                    }
                    for (int i = k; i < m; i++) {
                        //System.out.println("A.setValue(i,k) = " + A.getValue(i, k) / s.getValue(k, 0));
                        A.setValue(i, k, A.getValue(i, k) / s.getValue(k, 0));
                    }
                    //System.out.println("A.setValue(k,k) = " + (A.getValue(k, k) + 1.0));
                    A.setValue(k, k, A.getValue(k, k) + 1.0);
                }
                //System.out.println("s.setValue(k,0) = " + -s.getValue(k, 0) + " (k = " + k + ")");
                s.setValue(k, 0, -s.getValue(k, 0));
            }
            //System.out.println("Looping from j = " + (k + 1) + " to j < " + n);

            for (int j = k + 1; j < n; j++) {
                if ((k < nct) & (s.getValue(k, 0) != 0.0)) {
                    //System.out.println("Entered (k < nct) & (s[k] != 0.0)");

                    // Apply the transformer.

                    double t = 0;
                    for (int i = k; i < m; i++) {
                        t += A.getValue(i, k) * A.getValue(i, j);
                    }
                    t = -t / A.getValue(k,k);
                    for (int i = k; i < m; i++) {
                        //System.out.println("A(i,j) = " + A.getValue(i,j) + t * A.getValue(i,k));

                        A.setValue(i,j,A.getValue(i,j) + t * A.getValue(i,k));
                    }
                }

                // Place the k-th row of A into e for the
                // subsequent calculation of the row transformer.
                //System.out.println(" e["+j+"] = " + A.getValue(k,j));
                e.setValue(j,0,A.getValue(k,j));
            }
            if (wantu & (k < nct)) {
                //System.out.println("Entered wantu & (k < nct)");

                // Place the transformer in U for subsequent back
                // multiplication.

                for (int i = k; i < m; i++) {
                    //System.out.println("U.setValue(i,k,A.getValue(i,k)) = " + A.getValue(i,k));
                    U.setValue(i,k,A.getValue(i,k));
                }
            }
            if (k < nrt) {
                //System.out.println("Entered k < nrt");

                // Compute the k-th row transformer and place the
                // k-th super-diagonal in e[k].
                // Compute 2-norm without under/overflow.
                e.setValue(k,0,0);
                for (int i = k + 1; i < n; i++) {
                    //System.out.println("e[k] = Util.hypot(e[k], e[i])" + Util.hypot(e.getValue(k,0), e.getValue(i,0)));
                    e.setValue(k,0,Util.hypot(e.getValue(k,0), e.getValue(i,0)));
                }
                if (e.getValue(k,0) != 0.0) {
                    //System.out.println("Entered e.getValue(k,1) != 0.0 ");
                    if (e.getValue(k+1,0) < 0.0) {
                        //System.out.println("Entered e.getValue(k+1,1) < 0.0 ");
                        //System.out.println(" e.setValue(k,1,-e.getValue(k,1)) =  " + -e.getValue(k,0));
                        e.setValue(k,0,-e.getValue(k,0));
                    }
                    //System.out.println("Looping with i = " + (k + 1) + " to < " + n);
                    for (int i = k + 1; i < n; i++) {
                        //System.out.println("e.setValue(i,1,e.getValue(i,1)/e.getValue(k,1)) = " + e.getValue(i,0)/e.getValue(k,0));
                        e.setValue(i,0,e.getValue(i,0)/e.getValue(k,0));
                    }
                    //System.out.println("e.setValue(k+1, 1,  e.getValue(k+1,1) + 1.0) = " + (e.getValue(k+1,0) + 1.0));
                    e.setValue(k+1, 0,  e.getValue(k+1,0) + 1.0);
                }
                //System.out.println("e.setValue(k,1,-e.getValue(k,1)) = " + -e.getValue(k,0));
                e.setValue(k,0,-e.getValue(k,0));
                if ((k + 1 < m) & (e.getValue(k,0) != 0.0)) {
                    //System.out.println("Entered (k + 1 < m) & (e.getValue(k,1) != 0.0)");
                    // Apply the transformer.

                    for (int i = k + 1; i < m; i++) {
                        //System.out.println("setting work  " + i);
                        work.setValue(i,0,0.0);
                    }
                    for (int j = k + 1; j < n; j++) {
                        for (int i = k + 1; i < m; i++) {
                            //System.out.println("work.setValue(i,1,e.getValue(i,1) + (e.getValue(j,1) * A.getValue(i,j))) = " + (work.getValue(i,0) + (e.getValue(j,0) * A.getValue(i,0))));
                            work.setValue(i,0,work.getValue(i,0) + (e.getValue(j,0) * A.getValue(i,j)));
                        }
                    }
                    for (int j = k + 1; j < n; j++) {
                        double t = -e.getValue(j,0) / e.getValue(k + 1,0);
                        for (int i = k + 1; i < m; i++) {
                            //System.out.println("A.set(i,j) = " + A.getValue(i,j) + t * work.getValue(i,0));
                            A.setValue(i,j, A.getValue(i,j) + t * work.getValue(i,0));
                        }
                    }
                }
                if (wantv) {
                    //System.out.println("Entered wantv");

                    // Place the transformer in V for subsequent
                    // back multiplication.

                    for (int i = k + 1; i < n; i++) {
                        //System.out.println(" V.setValue(i,k,e.getValue(i,1)) = " + e.getValue(i,0) );
                        V.setValue(i,k,e.getValue(i,0));
                    }
                }
            }
        }

        // Set up the final bidiagonal matrix or order p.

        int p = Math.min(n, m + 1);
        //System.out.println("p = " + p);

        if (nct < n) {
            //System.out.println("Entered nct < n");
            //System.out.println("s.setValue(nct,1,A.getValue(nct,nct)) = " + A.getValue(nct,nct));
            //System.out.println(A);
            s.setValue(nct,0,A.getValue(nct,nct));
            //System.out.println(s);
        }
        if (m < p) {
            //System.out.println("Entered m < p");

            s.setValue(p-1,0,0.0);
        }
        if (nrt + 1 < p) {
            //System.out.println("Entered nrt + 1 < p");
            //System.out.println("e.setValue(nrt,1,A.getValue(nrt,p-1)) = " + A.getValue(nrt,p-1));
            e.setValue(nrt,0,A.getValue(nrt,p-1));
        }
        e.setValue(p-1,0,0.0);

        // If required, generate U.

        if (wantu) {
            //System.out.println("Entered wantu");
            //System.out.println("Looping for j = " + nct + " to j <  " + nu);
            for (int j = nct; j < nu; j++) {
                for (int i = 0; i < m; i++) {
                    U.setValue(i,j,0.0);
                }
                U.setValue(j,j,1.0);
            }
            //System.out.println("Looping for k = " + (nct - 1) + " to k >=  " + 0);
            for (int k = nct - 1; k >= 0; k--) {
                if (s.getValue(k,0) != 0.0) {
                    for (int j = k + 1; j < nu; j++) {
                        double t = 0;
                        for (int i = k; i < m; i++) {
                            t += U.getValue(i,k) * U.getValue(i,j);
                        }
                        t = -t / U.getValue(k,k);
                        for (int i = k; i < m; i++) {
                            U.setValue(i,j, U.getValue(i,j) + t * U.getValue(i,k));
                        }
                    }
                    for (int i = k; i < m; i++) {
                        U.setValue(i,k,-U.getValue(i,k));
                    }
                    U.setValue(k,k,U.getValue(k,k) + 1.0);
                    for (int i = 0; i < k - 1; i++) {
                        U.setValue(i,k,0.0);
                    }
                } else {
                    for (int i = 0; i < m; i++) {
                        U.setValue(i,k,0.0);
                    }
                    U.setValue(k,k,1.0);
                }
            }
        }

        // If required, generate V.

        if (wantv) {
            //System.out.println("Entering wantv");
            //System.out.println("Looping for k = " + (n - 1) + " to k >= 0");

            for (int k = n - 1; k >= 0; k--) {
                if ((k < nrt) & (e.getValue(k,0) != 0.0)) {
                    for (int j = k + 1; j < nu; j++) {
                        double t = 0;
                        for (int i = k + 1; i < n; i++) {
                            t += V.getValue(i,k)* V.getValue(i,j);
                        }
                        t = -t / V.getValue(k+1,k);
                        for (int i = k + 1; i < n; i++) {
                            V.setValue(i,j,V.getValue(i,j) + t * V.getValue(i,k));
                        }
                    }
                }
                for (int i = 0; i < n; i++) {
                    V.setValue(i,k,0.0);
                }
                V.setValue(k,k,1.0);
            }
        }

        // Main iteration loop for the singular values.
        int pp = p - 1;
        //System.out.println("pp = " + pp);
        int iter = 0;
        double eps = Math.pow(2.0, -52.0);
        while (p > 0) {
            //System.out.println("While p > 0 p= " + p);
            int k, kase;

            // Here is where a test for too many iterations would go.

            // This section of the program inspects for
            // negligible elements in the s and e arrays.  On
            // completion the variables kase and k are setValue as follows.

            // kase = 1     if s(p) and e[k-1] are negligible and k<p
            // kase = 2     if s(k) is negligible and k<p
            // kase = 3     if e[k-1] is negligible, k<p, and
            //              s(k), ..., s(p) are not negligible (qr step).
            // kase = 4     if e(p-1) is negligible (convergence).
            //System.out.println("Looping for k = " + (p - 2) + " k >= -1");
            for (k = p - 2; k >= -1; k--) {
                if (k == -1) {
                    //System.out.println("Entering k == -1 ");
                    break;
                }
                //System.out.println("Math.abs(e.getValue("+k+",1)) " + Math.abs(e.getValue(k,0)));
                //System.out.println("Math.abs(s.getValue("+k+",1)) " + Math.abs(s.getValue(k,0) ));
                //System.out.println("Math.abs(s.getValue("+(k+1)+",1)) " + Math.abs(s.getValue(k+1,0) ));

                if (Math.abs(e.getValue(k,0)) <= eps * (Math.abs(s.getValue(k,0) + Math.abs(s.getValue(k+1,0))))) {
                    //System.out.println("Entering Math.abs(e.getValue(k,1)) <= eps * (Math.abs(s.getValue(k,1) + Math.abs(s.getValue(k+1,1))))");
                    e.setValue(k,0,0.0);
                    break;
                } else {
                    //System.out.println("Entering else");

                }
            }
            if (k == p - 2) {
                //System.out.println("Entering k == p-2");
                kase = 4;
            } else {
                int ks;
                //System.out.println("Looping ks = " + (p - 1) + " to ks > = " + k);
                for (ks = p - 1; ks >= k; ks--) {
                    if (ks == k) {
                        //System.out.println("Entering ks == k");
                        break;
                    }
                    double t = (ks != p ? Math.abs(e.getValue(ks,0)) : 0.0) +
                            (ks != k + 1 ? Math.abs(e.getValue(ks-1,0)) : 0.0);
                    if (Math.abs(s.getValue(ks,0)) <= eps * t) {
                        s.setValue(ks,0,0.0);
                        break;
                    }
                }
                if (ks == k) {
                    kase = 3;
                } else if (ks == p - 1) {
                    kase = 1;
                } else {
                    kase = 2;
                    k = ks;
                }
            }
            k++;

            // Perform the task indicated by kase.

            switch (kase) {

                // Deflate negligible s(p).

                case 1:
                    {
                        double f = e.getValue(p-2,0);
                        e.setValue(p-2,0,0.0);
                        for (int j = p - 2; j >= k; j--) {
                            double t = Util.hypot(s.getValue(j,0), f);
                            double cs = s.getValue(j,0) / t;
                            double sn = f / t;
                            s.setValue(j,0,t);
                            if (j != k) {
                                f = -sn * e.getValue(j-1,0);
                                e.setValue(j-1,0,cs* e.getValue(j-1,0));
                            }
                            if (wantv) {
                                for (int i = 0; i < n; i++) {
                                    t = cs * V.getValue(i,j) + sn * V.getValue(i,p-1);
                                    V.setValue(i,p-1,-sn * V.getValue(i,j) + cs * V.getValue(i,p-1));
                                    V.setValue(i,j,t);
                                }
                            }
                        }
                    }
                    break;

                    // Split at negligible s(k).

                case 2:
                    {
                        double f = e.getValue(k-1,0);
                        e.setValue(k-1,0,0.0);
                        for (int j = k; j < p; j++) {
                            double t = Util.hypot(s.getValue(j,0), f);
                            double cs = s.getValue(j,0) / t;
                            double sn = f / t;
                            s.setValue(j,0,t);
                            f = -sn * e.getValue(j,0);
                            e.setValue(j,0,cs * e.getValue(j,0));
                            if (wantu) {
                                for (int i = 0; i < m; i++) {
                                    t = cs * U.getValue(i,j) + sn * U.getValue(i,k-1);
                                    U.setValue(i,k-1, -sn * U.getValue(i,j) + cs * U.getValue(i,k-1));
                                    U.setValue(i,j,t);
                                }
                            }
                        }
                    }
                    break;

                    // Perform one qr step.

                case 3:
                    {
                        //System.out.println("33333333 - Entered case 3");
                        // Calculate the shift.
                        double scale = Math.max(Math.max(Math.max(Math.max(Math.abs(s.getValue(p-1,0)), Math.abs(s.getValue(p-2,0))), Math.abs(e.getValue(p-2,0))),
                                Math.abs(s.getValue(k,0))), Math.abs(e.getValue(k,0)));
                        //System.out.println("scale = " + scale);

                        double sp = s.getValue(p-1,0) / scale;
                        ////System.out.println("sp = " + sp + "with s.getValue(p-1,1) =" + s[p - 1]);

                        double spm1 = s.getValue(p-2,0) / scale;
                        ////System.out.println("spm1 = " + spm1 + "with s.getValue(p-2,1) =" + s[p - 2]);

                        double epm1 = e.getValue(p-2,0) / scale;
                        ////System.out.println("epm1 = " + epm1 + "with e.getValue(p-2,1) =" + e[p - 2]);

                        double sk = s.getValue(k,0) / scale;
                        ////System.out.println("sk = " + sk + "with s.getValue(k,1) =" + s[k]);

                        double ek = e.getValue(k,0) / scale;
                        ////System.out.println("ek = " + sk + "with e.getValue(k,1) =" + e[k]);

                        double b = ((spm1 + sp) * (spm1 - sp) + epm1 * epm1) / 2.0;
                        ////System.out.println("b = " + b);

                        double c = (sp * epm1) * (sp * epm1);
                        ////System.out.println("c = " + c);

                        double shift = 0.0;
                        if ((b != 0.0) | (c != 0.0)) {
                            ////System.out.println("Entered (b != 0.0) | (c != 0.0)");
                            shift = Math.sqrt(b * b + c);
                            if (b < 0.0) {
                                shift = -shift;
                            }
                            shift = c / (b + shift);
                        }
                        //System.out.println("shift = " + shift);
                        double f = (sk + sp) * (sk - sp) + shift;
                        //System.out.println("f = " + f);
                        double g = sk * ek;
                        //System.out.println("g = " + g);

                        // Chase zeros.

                        for (int j = k; j < p - 1; j++) {
                            double t = Util.hypot(f, g);   //changed
                            double cs = f / t;
                            double sn = g / t;
                            if (j != k) {
                                //System.out.println("e.setValue(j-1,1,t) " + t);
                                e.setValue(j-1,0,t);
                            }
                            f = cs * s.getValue(j,0) + sn * e.getValue(j,0);
                            //System.out.println(" e.setValue(j,1,cs * e.getValue(j,1) - sn * s.getValue(j,1)); " + (cs * e.getValue(j,0) - sn * s.getValue(j,0)));

                            e.setValue(j,0,cs * e.getValue(j,0) - sn * s.getValue(j,0));
                            g = sn * s.getValue(j+1,0);
                            //System.out.println("s.setValue(j+1,0,cs * s.getValue(j+1,0));" + (cs * s.getValue(j+1,0)));
                            s.setValue(j+1,0,cs * s.getValue(j+1,0));
                            if (wantv) {
                                for (int i = 0; i < n; i++) {
                                    t = cs * V.getValue(i,j) + sn * V.getValue(i,j+1);
                                    V.setValue(i,j+1,-sn * V.getValue(i,j) + cs * V.getValue(i,j+1));
                                    V.setValue(i,j,t);
                                }
                            }
                            t = Util.hypot(f, g);    //changed
                            cs = f / t;
                            sn = g / t;
                            s.setValue(j,0,t);
                            f = cs * e.getValue(j,0) + sn * s.getValue(j+1,0);
                            s.setValue(j+1,0,-sn * e.getValue(j,0) + cs * s.getValue(j+1,0));
                            g = sn * e.getValue(j+1,0);
                            e.setValue(j+1,0,cs * e.getValue(j+1,0));
                            if (wantu && (j < m - 1)) {
                                for (int i = 0; i < m; i++) {
                                    t = cs * U.getValue(i,j) + sn * U.getValue(i,j+1);
                                    U.setValue(i,j+1,-sn * U.getValue(i,j) + cs * U.getValue(i,j+1));
                                    U.setValue(i,j,t);
                                }
                            }
                        }
                        e.setValue(p-2,0,f);
                        iter = iter + 1;
                    }
                    break;

                    // Convergence.

                case 4:
                    {
                        //System.out.println("44444444 - Entered case 4");
                        // Make the singular values positive.

                        if (s.getValue(k,0) <= 0.0) {
                            //System.out.println("Entered s.getValue(k,1) <= 0.0");
                            //System.out.println("s.setValue(k,0,(s.getValue(k,0) < 0.0 ? -s.getValue(k,0) : 0.0)) " + (s.getValue(k,0) < 0.0 ? -s.getValue(k,0) : 0.0));
                            s.setValue(k,0,(s.getValue(k,0) < 0.0 ? -s.getValue(k,0) : 0.0));
                            if (wantv) {
                                //System.out.println("Entered wantv");
                                //System.out.println("Looping for i = 0 to i <= " + pp);
                                for (int i = 0; i <= pp; i++) {
                                    //System.out.println("V.setValue(i,k,-V.getValue(i,k)) = " + -V.getValue(i,k));
                                    V.setValue(i,k,-V.getValue(i,k));
                                }
                            }
                        }

                        // Order the singular values.
                        //System.out.println("Entering while k (" + k + ") < pp (" + pp + ")");
                        while (k < pp) {
                            if (s.getValue(k,0) >= s.getValue(k+1,0)) {
                                //System.out.println("Entered s.getValue(k,1) >= s.getValue(k+1,1)");
                                break;
                            }
                            double t = s.getValue(k,0);
                            //System.out.println("s.setValue(k,1,s.getValue(k+1,1)) = " + s.getValue(k+1,0));
                            s.setValue(k,0,s.getValue(k+1,0));
                            //System.out.println("s.setValue(k+1,1,t) = " + t);
                            s.setValue(k+1,0,t);
                            if (wantv && (k < n - 1)) {
                                //System.out.println("Entered wantv && (k < n - 1");
                                for (int i = 0; i < n; i++) {
                                    t = V.getValue(i,k+1);
                                    //System.out.println("V.setValue(i,k+1,U.getValue(i,k)) = " + V.getValue(i,k));
                                    V.setValue(i,k+1,V.getValue(i,k));
                                    //System.out.println("V.setValue(i,k,t) = " + t);
                                    V.setValue(i,k,t);
                                }
                            }
                            if (wantu && (k < m - 1)) {
                                //System.out.println("Entered wantu && (k < m - 1)");
                                for (int i = 0; i < m; i++) {
                                    t = U.getValue(i,k+1);
                                    //System.out.println("U.setValue(i,k+1,U.getValue(i,k)) = " + U.getValue(i,k));
                                    U.setValue(i,k+1,U.getValue(i,k));
                                    //System.out.println("U.setValue(i,k,t) = " + t);
                                    U.setValue(i,k,t);
                                }
                            }
                            k++;
                        }
                        iter = 0;
                        p--;
                    }
                    break;
            }
        }
    }



/* ------------------------
   Public Methods
 * ------------------------ */

    /**
     * Return the left singular vectors
     *
     * @return U
     */

    public Matrix getU() {
        //return new Matrix(U,m,Math.min(m+1,n));
        return U.getAdaptee();
    }

    /**
     * Return the right singular vectors
     *
     * @return V
     */

    public Matrix getV() {
        return V.getAdaptee();
    }

    /**
     * Return the one-dimensional array of singular values
     *
     * @return diagonal of S.
     */

    public Matrix getSingularValues() {
        return s.getAdaptee();
    }

    /**
     * Return the diagonal matrix of singular values
     *
     * @return S
     */

   /*public Matrix getS() {

        double[][] S = new double[m][n]; //changed n to m
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                S[i][j] = 0.0;
            }
            S[i][i] = this.s[i];
        }
        return MatrixFactory.getMatrix(m, n, hint, S);  //changed n to m
    } */

    /**
     * Two norm
     *
     * @return max(S)
     */

    public double norm2() {
        return s.getValue(0,0);
    }

    /**
     * Two norm condition number
     *
     * @return max(S)/min(S)
     */

    public double cond() {
        return norm2() / s.getValue(Math.min(m, n)-1,0);
    }

    /**
     * Effective numerical matrix rank
     *
     * @return Number of nonnegligible singular values.
     */

    public int rank() {
        double eps = Math.pow(2.0, -52.0);
        double tol = Math.max(m, n) * s.getValue(0,0) * eps;
        int r = 0;
        for (int i = 0; i < s.rows(); i++) {
            if (s.getValue(i,0) > tol) {
                r++;
            }
        }
        return r;
    }


   /** public static void main(String[] args) {
        Matrix m = GaussSyntax.create("{0.73812929 0.89491979 0.60083804, 0.17653955 0.72832246 0.17583769, 0.47245871 0.57375655 0.35973716}");
        
    } */

}

