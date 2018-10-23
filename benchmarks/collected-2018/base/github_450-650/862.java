// https://searchcode.com/api/result/104826361/

/*
 * jcurl java curling software framework http://www.jcurl.orgCopyright (C)
 * 2005-2009 M. Rohrmoser
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jcurl.math;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

import org.apache.commons.logging.Log;
import org.jcurl.core.impl.CurlerDenny;
import org.jcurl.core.log.JCLoggerFactory;

/**
 * Helper for convenient approximated Java2D drawing of arbitratry
 * {@link R1RNFunction}s with at least 2 dimensions.
 * 
 * @see Shaper
 * @see Shapeable
 * @author <a href="mailto:m@jcurl.org">M. Rohrmoser </a>
 * @version $Id$
 */
public abstract class ShaperUtils {

	private static final Log log = JCLoggerFactory.getLogger(ShaperUtils.class);

	/**
	 * Compute the control points and add one <a
	 * href="http://en.wikipedia.org/wiki/B%C3%A9zier_curve">Cubic Bezier Curve</a>
	 * to a {@link GeneralPath}. Does <b>no</b> initial
	 * {@link GeneralPath#moveTo(float, float)}.
	 * 
	 * <h3>Approximation algorithm</h3>
	 * <p>
	 * This ansatz uses no adaptive optimisation but the nature of curves as
	 * they're typical to curling:
	 * <ul>
	 * <li>continuous within [tmin:tmax] - at least C0, C1</li>
	 * <li>smoothly increasing curvature</li>
	 * <li>not meandering</li>
	 * </ul>
	 * So we use
	 * <ul>
	 * <li>the start- and endpoint of each interval als control points k0 and
	 * k3</li>
	 * <li>the directions (normalised velocities) in the control points k0 and
	 * k3</li>
	 * <li>a ratio 3:2:1 of the distances |k0-k1| : |k1-k2| : |k2-k3|</li>
	 * </ul>
	 * <p>
	 * This causes quite a computation - without iteration/recursion though, but
	 * 1 square root and many double multiplications - but this is well worth
	 * while as we can reduce the curve segments to draw significantly. One
	 * cubic bezier curve per seven meters curve length gives an error &lt; 2 mm
	 * (using {@link CurlerDenny} with 24s draw-to-tee and 1m curl)!
	 * </p>
	 * <p>
	 * TODO maybe re-use endpoint location and velocity. This can cause pain at
	 * C1 discontinuous t's (collissions).
	 * </p>
	 * <h3><a href="http://en.wikipedia.org/wiki/Maxima_(software)">Maxima</a>
	 * Solution</h3>
	 * 
	 * <pre>
	 * 	radsubstflag: true$
	 * 	k1_0 = k0_0 + l * v0_0;
	 * 	k1_1 = k0_1 + l * v0_1;
	 * 	k2_0 = k3_0 - n * v3_0;
	 * 	k2_1 = k3_1 - n * v3_1;
	 * 	l/n=a/c;
	 * 	((k2_0 - k1_0)*(k2_0 - k1_0) + (k2_1 - k1_1)*(k2_1 - k1_1)) / (n*n) = b*b / (c*c);
	 * 	solve([%th(6), %th(5), %th(4), %th(3), %th(2), %th(1)],[k1_0, k1_1, k2_0, k2_1, l, n]);
	 * 	factor(%);
	 * 	ratsimp(%);
	 * 	ratsubst(V0, v0_1&circ;2+v0_0&circ;2, %);
	 * 	ratsubst(V3, v3_1&circ;2+v3_0&circ;2, %);
	 * 	ratsubst(A, k0_1-k3_1, %);
	 * 	ratsubst(B, k0_0-k3_0, %);
	 * 	ratsubst(T, 2*a*c*v0_0*v3_0+a&circ;2*v0_1&circ;2+a&circ;2*v0_0&circ;2-b&circ;2, %);
	 * 	ratsubst(Q, c&circ;2*V3+a&circ;2*V0+T+2*a*c*v0_1*v3_1-a&circ;2*v0_1&circ;2-a&circ;2*v0_0&circ;2, %);
	 * 	ratsubst(W, B&circ;2*T+B&circ;2*(b&circ;2-Q)+c&circ;2*(v3_0&circ;2*B&circ;2-v3_0&circ;2*A&circ;2)-a&circ;2*v0_1&circ;2*B&circ;2+v3_1*(2*c&circ;2*v3_0*A*B
	 * 	+2*a*c*v0_0*A*B)+v0_1*(2*a*c*v3_0*A*B+2*a&circ;2*v0_0*A*B)-2*a*c*v0_0*v3_0*A&circ;2-a&circ;2*v0_0&circ;2*A&circ;2
	 * 	+b&circ;2*A&circ;2, %);
	 * 	expand(%);
	 * 	factor(%);
	 * 	ratsubst(R, c*v3_0*B+a*v0_0*B+c*v3_1*A+a*v0_1*A, %);
	 * </pre>
	 */
	static void curveTo(final R1RNFunction f, final double tmin,
			final double tmax, final GeneralPath gp, final float zoom) {
		final double eps = 1e-6;

		// first control point (startpoint). The same as gp.getCurrentPoint()
		final double k0_0 = f.at(tmin, 0, 0);
		final double k0_1 = f.at(tmin, 0, 1);
		// normalized startpoint velocity
		double v0_0 = f.at(tmin, 1, 0);
		double v0_1 = f.at(tmin, 1, 1);
		if (v0_0 * v0_0 + v0_1 * v0_1 < eps) {
			v0_0 = f.at(tmin + eps, 1, 0);
			v0_1 = f.at(tmin + eps, 1, 1);
		}
		double v = Math.sqrt(v0_0 * v0_0 + v0_1 * v0_1);
		v0_0 /= v;
		v0_1 /= v;

		// 4th control point (endpoint).
		final double k3_0 = f.at(tmax, 0, 0);
		final double k3_1 = f.at(tmax, 0, 1);
		// normalized endpoint velocity
		double v3_0 = f.at(tmax, 1, 0);
		double v3_1 = f.at(tmax, 1, 1);
		if (v3_0 * v3_0 + v3_1 * v3_1 < eps) {
			v3_0 = f.at(tmax - eps, 1, 0);
			v3_1 = f.at(tmax - eps, 1, 1);
		}
		v = Math.sqrt(v3_0 * v3_0 + v3_1 * v3_1);
		v3_0 /= v;
		v3_1 /= v;

		final double a = 3;
		final double b = 2;
		final double c = 1;
		final double V0 = v0_1 * v0_1 + v0_0 * v0_0;
		final double V3 = v3_1 * v3_1 + v3_0 * v3_0;
		final double A = k0_1 - k3_1;
		final double B = k0_0 - k3_0;
		final double T = 2 * a * c * v0_0 * v3_0 + a * a * v0_1 * v0_1 + a * a
				* v0_0 * v0_0 - b * b;
		final double Q = c * c * V3 + a * a * V0 + T + 2 * a * c * v0_1 * v3_1
				- a * a * v0_1 * v0_1 - a * a * v0_0 * v0_0;
		double W = B * B * T + B * B * (b * b - Q) + c * c
				* (v3_0 * v3_0 * B * B - v3_0 * v3_0 * A * A) - a * a * v0_1
				* v0_1 * B * B + v3_1 * 2 * c * c * v3_0 * A * B + 2 * a * c
				* v0_0 * A * B + v0_1
				* (2 * a * c * v3_0 * A * B + 2 * a * a * v0_0 * A * B) - 2 * a
				* c * v0_0 * v3_0 * A * A - a * a * v0_0 * v0_0 * A * A + b * b
				* A * A;
		if (W < 0) {
			if (log.isWarnEnabled()) {
				log.warn("Arithmetic trouble:");
				log.warn("v0=(" + v0_0 + ", " + v0_1 + ")");
				log.warn("v3=(" + v3_0 + ", " + v3_1 + ")");
				log.warn("V0=" + V0);
				log.warn("V3=" + V3);
				log.warn("A=" + A);
				log.warn("B=" + B);
				log.warn("T=" + T);
				log.warn("Q=" + Q);
				log.warn("W=" + W);
			}
			gp.moveTo(zoom * (float) k3_0, zoom * (float) k3_1);
			return;
		}
		W = Math.sqrt(W);
		final double R = c * v3_0 * B + a * v0_0 * B + c * v3_1 * A + a * v0_1
				* A;

		final double l, n;
		if (true) {
			final double F = (W + R) / Q;
			l = -a * F;
			n = -c * F;
		} else {
			final double F = (W - R) / Q;
			l = a * F;
			n = c * F;
		}
		if (Double.isNaN(l) || Double.isNaN(n)) {
			log.warn("v0=(" + v0_0 + ", " + v0_1 + ")");
			log.warn("v3=(" + v3_0 + ", " + v3_1 + ")");
			log.warn("V0=" + V0);
			log.warn("V3=" + V3);
			log.warn("A=" + A);
			log.warn("B=" + B);
			log.warn("T=" + T);
			log.warn("Q=" + Q);
			log.warn("W=" + W);
			log.warn("R=" + R);
		}

		final float k1_0 = (float) (k0_0 + l * v0_0);
		final float k1_1 = (float) (k0_1 + l * v0_1);
		final float k2_0 = (float) (k3_0 - n * v3_0);
		final float k2_1 = (float) (k3_1 - n * v3_1);
		if (log.isDebugEnabled())
			log.debug("(" + k1_0 + ", " + k1_1 + "), (" + k2_0 + ", " + k2_1
					+ "), (" + (float) k3_0 + ", " + (float) k3_1 + ")");
		gp.curveTo(zoom * k1_0, zoom * k1_1, zoom * k2_0, zoom * k2_1, zoom
				* (float) k3_0, zoom * (float) k3_1);
	}

	private static float interpolate(final float min, final float max,
			final float t, final Interpolator ip) {
		final float d = max - min;
		return min + d * ip.interpolate((t - min) / d);
	}

	/**
	 * Interpolate using <a
	 * href="http://en.wikipedia.org/wiki/B%C3%A9zier_curve">Cubic Bezier Curves</a>.
	 * <p>
	 * Computes the required intermediate <code>t</code> samples and delegates
	 * to {@link #curveTo(R1RNFunction, double, double, GeneralPath, float)} to
	 * compute the interpolating curve segments.
	 * </p>
	 * 
	 * @param src
	 *            the (at least 2-dimensional) curve. Higher dimensions are
	 *            ignored.
	 * @param min
	 *            the min input <code>t</code> to
	 *            {@link R1RNFunction#at(double, int, int)}
	 * @param max
	 *            the max input <code>t</code> to
	 *            {@link R1RNFunction#at(double, int, int)}
	 * @param curves
	 *            the number of interpolating cubic bezier curves - must be
	 *            &gt;= 1.
	 * @param zoom
	 *            graphics zoom factor (typically 1)
	 * @param ip
	 *            the {@link Interpolator} to get the intermediate
	 *            <code>t</code> sample values.
	 * @see #curveTo(R1RNFunction, double, double, GeneralPath, float)
	 */
	public static Shape interpolateCubic(final R1RNFunction src,
			final double min, final double max, final int curves,
			final float zoom, final Interpolator ip) {
		// setup
		if (curves < 1)
			throw new IllegalArgumentException(
					"Give me at least 1 (connect start + stop)");
		final float d = (float) (max - min);
		final GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO,
				3 * curves + 1); // +1 just to be sure...
		// start
		final float x = (float) src.at(min, 0, 0);
		final float y = (float) src.at(min, 0, 1);
		gp.moveTo(zoom * x, zoom * y);

		double told = min;
		// intermediate
		final int n = curves;
		for (int i = 1; i < n; i++) {
			final double t = min + d * ip.interpolate((float) i / n);
			curveTo(src, told, t, gp, zoom);
			told = t;
		}

		// stop
		curveTo(src, told, max, gp, zoom);
		return gp;
	}

	/**
	 * Interpolate using <a
	 * href="http://en.wikipedia.org/wiki/B%C3%A9zier_curve">Linear Bezier
	 * Curves</a>.
	 * <p>
	 * Computes the required intermediate <code>t</code> samples and delegates
	 * to {@link #lineTo(R1RNFunction, double, GeneralPath, float)} to compute
	 * the interpolating curve segments.
	 * </p>
	 * 
	 * @param src
	 *            the (at least 2-dimensional) curve. Higher dimensions are
	 *            ignored.
	 * @param min
	 *            the min input <code>t</code> to
	 *            {@link R1RNFunction#at(double, int, int)}
	 * @param max
	 *            the max input <code>t</code> to
	 *            {@link R1RNFunction#at(double, int, int)}
	 * @param curves
	 *            the number of line segments - must be &gt;= 1.
	 * @param zoom
	 *            graphics zoom factor (typically 1)
	 * @param ip
	 *            the {@link Interpolator} to get the intermediate sample
	 *            <code>t</code> values.
	 * @see #lineTo(R1RNFunction, double, GeneralPath, float)
	 */
	public static Shape interpolateLinear(final R1RNFunction src,
			final double min, final double max, final int curves,
			final float zoom, final Interpolator ip) {
		// setup
		if (curves < 1)
			throw new IllegalArgumentException(
					"Give me at least 1 (connect start + stop)");
		final float d = (float) (max - min);
		final GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO,
				curves + 1); // +1 just to be sure...
		// start
		final float x = (float) src.at(min, 0, 0);
		final float y = (float) src.at(min, 0, 1);
		gp.moveTo(zoom * x, zoom * y);

		// intermediate
		final int n = curves;
		for (int i = 1; i < n; i++) {
			final double t = min + d * ip.interpolate((float) i / n);
			lineTo(src, t, gp, zoom);
		}

		// stop
		lineTo(src, max, gp, zoom);
		return gp;
	}

	/**
	 * Interpolate using <a
	 * href="http://en.wikipedia.org/wiki/B%C3%A9zier_curve">Quadratic Bezier
	 * Curves</a>.
	 * <p>
	 * Computes the required intermediate <code>t</code> samples and delegates
	 * to {@link #quadTo(R1RNFunction, double, double, GeneralPath, float)} to
	 * compute the interpolating curve segments.
	 * </p>
	 * 
	 * @param src
	 *            the (2-dimensional) curve. Higher dimensions are ignored.
	 * @param min
	 *            the min input <code>t</code> to
	 *            {@link R1RNFunction#at(double, int, int)}
	 * @param max
	 *            the max input <code>t</code> to
	 *            {@link R1RNFunction#at(double, int, int)}
	 * @param curves
	 *            the number of line segments - must be &gt;= 1.
	 * @param zoom
	 *            graphics zoom factor (typically 1)
	 * @param ip
	 *            the {@link Interpolator} to get the intermediate sample
	 *            <code>t</code> values.
	 * @see #quadTo(R1RNFunction, double, double, GeneralPath, float)
	 */
	public static Shape interpolateQuadratic(final R1RNFunction src,
			final double min, final double max, final int curves,
			final float zoom, final Interpolator ip) {
		// setup
		if (curves < 1)
			throw new IllegalArgumentException(
					"Give me at least 1 (connect start + stop)");
		final float d = (float) (max - min);
		final GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO,
				2 * curves + 1); // +1 just to be sure...
		// start
		final float x = (float) src.at(min, 0, 0);
		final float y = (float) src.at(min, 0, 1);
		gp.moveTo(zoom * x, zoom * y);

		double told = min;
		// intermediate
		final int n = curves;
		for (int i = 1; i < n; i++) {
			final double t = min + d * ip.interpolate((float) i / n);
			quadTo(src, told, t, gp, zoom);
			told = t;
		}

		// stop
		quadTo(src, told, max, gp, zoom);
		return gp;
	}

	/**
	 * Add one <a href="http://en.wikipedia.org/wiki/B%C3%A9zier_curve">Linear
	 * Bezier Curve</a> to a {@link GeneralPath}. Does <b>no</b> initial
	 * {@link GeneralPath#moveTo(float, float)}.
	 * 
	 * <h3>Approximation algorithm</h3>
	 * <p>
	 * Just connect start- and endpoint.
	 * <p>
	 * TODO maybe re-use endpoint location and velocity. This can cause pain at
	 * C1 discontinuous t's (collissions).
	 * </p>
	 */
	static final void lineTo(final R1RNFunction f, final double tmax,
			final GeneralPath gp, final float zoom) {
		final float x = (float) f.at(tmax, 0, 0);
		final float y = (float) f.at(tmax, 0, 1);
		gp.lineTo(zoom * x, zoom * y);
	}

	/**
	 * Compute the control point and add one <a
	 * href="http://en.wikipedia.org/wiki/B%C3%A9zier_curve">Quadratic Bezier
	 * Curve</a> to a {@link GeneralPath}. Does <b>no</b> initial
	 * {@link GeneralPath#moveTo(float, float)}.
	 * 
	 * <h3>Approximation algorithm</h3>
	 * <p>
	 * This ansatz uses no adaptive optimisation but only
	 * <ul>
	 * <li>the start- and endpoint of each interval als control points k0 and
	 * k2</li>
	 * <li>the directions (normalised velocities) in the control points k0 and
	 * k2. The intersection is used as k1.</li>
	 * </ul>
	 * <p>
	 * TODO maybe re-use endpoint location and velocity. This can cause pain at
	 * C1 discontinuous t's (collissions).
	 * </p>
	 * <h3><a href="http://en.wikipedia.org/wiki/Maxima_(software)">Maxima</a>
	 * Solution</h3>
	 * 
	 * <pre>
	 * radsubstflag: true$
	 * k0_0 + l * v0_0 = k2_0 + m * v2_0;
	 * k0_1 + l * v0_1 = k2_1 + m * v2_1;
	 * solve([%th(2),%th(1)],[l,m]);
	 * subst(q, v0_1 * v2_0 - v0_0 * v2_1, %);
	 * subst(dx_0 + k0_0, k2_0, %);
	 * subst(dx_1 + k0_1, k2_1, %);
	 * ratsimp(%);
	 * </pre>
	 */
	static final void quadTo(final R1RNFunction f, final double tmin,
			final double tmax, final GeneralPath gp, final float zoom) {
		final double eps = 1e-6;

		// first control point (startpoint). The same as gp.getCurrentPoint()
		final double k0_0 = f.at(tmin, 0, 0);
		final double k0_1 = f.at(tmin, 0, 1);
		// startpoint velocity
		double v0_0 = f.at(tmin, 1, 0);
		double v0_1 = f.at(tmin, 1, 1);
		if (v0_0 * v0_0 + v0_1 * v0_1 < eps) {
			v0_0 = f.at(tmin + eps, 1, 0);
			v0_1 = f.at(tmin + eps, 1, 1);
		}

		// 3rd control point (endpoint).
		final double k2_0 = f.at(tmax, 0, 0);
		final double k2_1 = f.at(tmax, 0, 1);
		// endpoint velocity
		double v2_0 = f.at(tmax, 1, 0);
		double v2_1 = f.at(tmax, 1, 1);
		if (v2_0 * v2_0 + v2_1 * v2_1 < eps) {
			v2_0 = f.at(tmax - eps, 1, 0);
			v2_1 = f.at(tmax - eps, 1, 1);
		}

		// compute the 2nd control point
		final double dx_0 = k2_0 - k0_0;
		final double dx_1 = k2_1 - k0_1;
		final double q = v0_1 * v2_0 - v0_0 * v2_1;
		final double m = -(dx_0 * v0_1 - dx_1 * v0_0) / q;

		// 2nd control point is
		final float k1_0 = (float) (k2_0 + m * v2_0);
		final float k1_1 = (float) (k2_1 + m * v2_1);

		if (true)
			gp.quadTo(zoom * k1_0, zoom * k1_1, zoom * (float) k2_0, zoom
					* (float) k2_1);
		else {
			gp.lineTo(zoom * k1_0, zoom * k1_1);
			gp.lineTo(zoom * (float) k2_0, zoom * (float) k2_1);
		}
	}

	static String toString(final double[] arr) {
		final StringBuilder w = new StringBuilder();
		if (arr == null)
			w.append("null");
		else {
			boolean start = true;
			w.append("[");
			for (final double element : arr) {
				if (!start)
					w.append(" ");
				w.append(Double.toString(element));
				start = false;
			}
			w.append("]");
		}
		return w.toString();
	}
}

