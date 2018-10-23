// https://searchcode.com/api/result/104826758/

/*
 * jcurl java curling software framework http://www.jcurl.org Copyright (C)
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
package org.jcurl.core.impl;

import java.awt.geom.AffineTransform;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.logging.Log;
import org.jcurl.core.api.Collider;
import org.jcurl.core.api.CollissionDetector;
import org.jcurl.core.api.ComputedTrajectorySet;
import org.jcurl.core.api.Curler;
import org.jcurl.core.api.CurveRock;
import org.jcurl.core.api.CurveStore;
import org.jcurl.core.api.MutableObject;
import org.jcurl.core.api.Rock;
import org.jcurl.core.api.RockSet;
import org.jcurl.core.api.RockSetUtils;
import org.jcurl.core.api.StopDetector;
import org.jcurl.core.api.RockType.Pos;
import org.jcurl.core.api.RockType.Vel;
import org.jcurl.core.impl.CollissionStore.Tupel;
import org.jcurl.core.log.JCLoggerFactory;
import org.jcurl.math.MathVec;
import org.jcurl.math.R1RNFunction;

/**
 * Bring it all together and trigger computation.
 * <p>
 * Registers itself as listener to
 * {@link RockSet#addRockListener(ChangeListener)} for both - initial rock
 * locations and velocities to trigger recomputation.
 * </p>
 * <p>
 * TODO re-work the whole computation/update strategy to reduce the number of
 * curves recomputed for free rocks' moves.
 * </p>
 * 
 * @author <a href="mailto:m@jcurl.org">M. Rohrmoser </a>
 * @version $Id:CurveManager.java 682 2007-08-12 21:25:04Z mrohrmoser $
 */
public class CurveManager extends MutableObject implements ChangeListener,
		ComputedTrajectorySet, Serializable {

	// compute beyond a realistic amount of time
	private static final double _30 = 60.0;
	/** Time leap during a hit. */
	private static final double hitDt = 1e-6;
	private static final Log log = JCLoggerFactory
			.getLogger(CurveManager.class);
	private static final double NoSweep = 0;
	private static final long serialVersionUID = 7198540442889130378L;
	private static final StopDetector stopper = new NewtonStopDetector();
	private final Map<CharSequence, CharSequence> annotations = new HashMap<CharSequence, CharSequence>();
	/** Trajectory (Rock) Bitmask or -1 for "currently not suspended". */
	private transient int collected = -1;
	private Collider collider = null;
	private transient CollissionDetector collissionDetector = new BisectionCollissionDetector();
	private transient final CollissionStore collissionStore = new CollissionStore();
	private Curler curler = null;
	private transient final RockSet<Pos> currentPos = RockSetUtils.allHome();
	private transient double currentTime = 0;
	private transient final RockSet<Vel> currentVel = RockSet.allZero(null);
	private transient CurveStore curveStore = new CurveStoreImpl(stopper,
			RockSet.ROCKS_PER_SET);
	private final RockSet<Pos> initialPos = RockSetUtils.allHome();
	private final RockSet<Vel> initialSpeed = RockSet.allZero(null);
	// don't fire change events on intermediate updates. Use another variable.
	private final transient RockSet<Pos> tmpPos = RockSetUtils.allHome();
	// don't fire change events on intermediate updates. Use another variable.
	private final transient RockSet<Vel> tmpVel = RockSet.allZero(null);

	public CurveManager() {
		initialPos.addRockListener(this);
		initialSpeed.addRockListener(this);
	}

	/**
	 * Internal. Compute one rock curve segment and don't change internal state.
	 * 
	 * @param i16
	 *            which rock
	 * @param t0
	 *            starttime
	 * @param sweepFactor
	 * @return the new Curve in world coordinates.
	 */
	CurveRock<Pos> doComputeCurve(final int i16, final double t0,
			final RockSet<Pos> p, final RockSet<Vel> s, final double sweepFactor) {
		final Rock<Pos> x = p.getRock(i16);
		final Rock<Vel> v = s.getRock(i16);
		final CurveRock<Pos> wc;
		if (v.p().distanceSq(0, 0) == 0)
			wc = CurveStill.newInstance(x);
		else
			// Convert the initial angle from WC to RC.
			// TUNE 2x sqrt, 2x atan2 to 1x each?
			wc = new CurveTransformed<Pos>(curler.computeRc(x.getA()
					+ Math.atan2(v.getX(), v.getY()), MathVec.abs2D(v.p()), v
					.getA(), sweepFactor), CurveTransformed.createRc2Wc(x.p(),
					v.p(), null), t0);
		if (log.isDebugEnabled())
			log.debug(i16 + " " + wc);
		return wc;
	}

	/**
	 * Internal.
	 * 
	 * @return when is the next hit, which are the rocks involved.
	 */
	Tupel doGetNextHit() {
		return collissionStore.first();
	}

	/**
	 * Internal. Typically after a hit: Recompute the new curves and upcoming
	 * collission candidates.
	 * 
	 * @param hitMask
	 * @return bitmask of rocks with new curves
	 */
	int doRecomputeCurvesAndCollissionTimes(final int hitMask, double t0,
			final RockSet<Pos> cp, final RockSet<Vel> cv) {
		int computedMask = 0;
		// first compute the new curves:
		// TUNE Parallel
		for (int i16 = RockSet.ROCKS_PER_SET - 1; i16 >= 0; i16--) {
			if (!RockSet.isSet(hitMask, i16))
				continue;
			curveStore.add(i16, t0, doComputeCurve(i16, t0, cp, cv, NoSweep),
					_30);
			computedMask |= 1 << i16;
		}
		// then add all combinations of potential collissions
		t0 += hitDt;
		// TUNE Parallel
		for (int i16 = RockSet.ROCKS_PER_SET - 1; i16 >= 0; i16--) {
			if (!RockSet.isSet(computedMask, i16))
				continue;
			for (int j16 = RockSet.ROCKS_PER_SET - 1; j16 >= 0; j16--) {
				if (i16 == j16 || i16 > j16 && RockSet.isSet(computedMask, j16))
					continue;
				collissionStore.replace(i16, j16, collissionDetector.compute(
						t0, _30, curveStore.getCurve(i16), curveStore
								.getCurve(j16)));
			}
		}
		return computedMask;
	}

	/**
	 * Internal. Does not {@link RockSet#fireStateChanged()}!
	 * 
	 * @param currentTime
	 */
	void doUpdatePosAndVel(final double currentTime, final RockSet<Pos> cp,
			final RockSet<Vel> cv) {
		// TUNE Parallel
		for (int i = RockSet.ROCKS_PER_SET - 1; i >= 0; i--) {
			final R1RNFunction c = curveStore.getCurve(i);
			double x = c.at(currentTime, 0, 0);
			double y = c.at(currentTime, 0, 1);
			double a = c.at(currentTime, 0, 2);
			cp.getRock(i).setLocation(x, y, a);
			x = c.at(currentTime, 1, 0);
			y = c.at(currentTime, 1, 1);
			a = c.at(currentTime, 1, 2);
			cv.getRock(i).setLocation(x, y, a);
		}
	}

	@Override
	public boolean equals(final Object obj) {
		return false;
	}

	public Map<CharSequence, CharSequence> getAnnotations() {
		return annotations;
	}

	public Collider getCollider() {
		return collider;
	}

	public CollissionDetector getCollissionDetector() {
		return collissionDetector;
	}

	public Curler getCurler() {
		return curler;
	}

	public RockSet<Pos> getCurrentPos() {
		return currentPos;
	}

	public double getCurrentTime() {
		return currentTime;
	}

	public RockSet<Vel> getCurrentVel() {
		return currentVel;
	}

	public CurveStore getCurveStore() {
		return curveStore;
	}

	public RockSet<Pos> getInitialPos() {
		return initialPos;
	}

	public RockSet<Vel> getInitialVel() {
		return initialSpeed;
	}

	public boolean getSuspended() {
		return collected >= 0;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	protected Object readResolve() throws ObjectStreamException {
		final CurveManager m = new CurveManager();
		m.setSuspended(true);
		m.annotations.putAll(annotations);
		m.setCollider(getCollider());
		m.setCurler(getCurler());
		m.setInitialPos(getInitialPos());
		m.setInitialVel(getInitialVel());
		// m.setCurrentTime(this.getCurrentTime());
		m.setSuspended(false);
		return m;
	}

	private void recompute(final double currentTime, final boolean complete) {
		if (getSuspended())
			return;
		if (complete) {
			{
				// initial
				final double t0 = 0.0;
				// TUNE Parallel
				// initial curves:
				for (int i16 = RockSet.ROCKS_PER_SET - 1; i16 >= 0; i16--) {
					curveStore.reset(i16);
					curveStore.add(i16, t0, doComputeCurve(i16, t0, initialPos,
							initialSpeed, NoSweep), _30);
				}
				// initial collission detection:
				collissionStore.clear();
				// TUNE Parallel
				for (int i16 = RockSet.ROCKS_PER_SET - 1; i16 >= 0; i16--)
					for (int j16 = i16 - 1; j16 >= 0; j16--)
						// log.info("collissionDetect " + i + ", " + j);
						collissionStore.add(collissionDetector.compute(t0, _30,
								curveStore.getCurve(i16), curveStore
										.getCurve(j16)), i16, j16);
			}
			final AffineTransform m = new AffineTransform();
			// NaN-safe time range check (are we navigating known ground?):
			while (currentTime > doGetNextHit().t) {
				final Tupel nh = doGetNextHit();
				if (log.isDebugEnabled())
					log.debug(nh.a + " - " + nh.b + " : " + nh.t);
				doUpdatePosAndVel(nh.t, tmpPos, tmpVel);
				// compute collission(s);
				final int mask = collider.compute(tmpPos, tmpVel, m);
				if (mask == 0)
					break;
				doRecomputeCurvesAndCollissionTimes(mask, nh.t, tmpPos, tmpVel);
			}
		}
		doUpdatePosAndVel(currentTime, currentPos, currentVel);
	}

	/** Do NOT update currentXY */
	private void recomputeCurve(final int i16) {
		if (i16 < 0)
			return;
		// TODO find the first collission with this (old) curve involved from
		// the collissionstore.

		// TODO find the first collission with this (new) curve involved.

		// TODO if none -> return

		// TODO clear the collissionstore/curvestore until the earlier of the
		// two

	}

	/** Do NOT update currentXY */
	private void recomputeCurves(final int bitmask) {
		if (bitmask <= 0)
			return;
		for (int i16 = RockSet.ROCKS_PER_SET - 1; i16 >= 0; i16--) {
			if (!RockSet.isSet(bitmask, i16))
				continue;
			recomputeCurve(i16);
		}
		doUpdatePosAndVel(currentTime, currentPos, currentVel);
	}

	public void setCollider(final Collider collider) {
		final Collider old = this.collider;
		if (old == collider)
			return;
		propChange
				.firePropertyChange("collider", old, this.collider = collider);
	}

	public void setCollissionDetector(
			final CollissionDetector collissionDetector) {
		// FIXME currently use ONLY Bisection.
		// collissionDetector = new BisectionCollissionDetector();
		final CollissionDetector old = this.collissionDetector;
		if (old == collissionDetector)
			return;
		propChange.firePropertyChange("collissionDetector", old,
				this.collissionDetector = collissionDetector);
	}

	public void setCurler(final Curler curler) {
		final Curler old = this.curler;
		if (old == curler)
			return;
		propChange.firePropertyChange("curler", old, this.curler = curler);
	}

	public void setCurrentTime(final double currentTime) {
		final double old = this.currentTime;
		if (old == currentTime)
			return;
		this.currentTime = currentTime;
		if (this.currentTime > old)
			recompute(currentTime, true);
		propChange.firePropertyChange("currentTime", old, this.currentTime);
	}

	public void setCurveStore(final CurveStore curveStore) {
		this.curveStore = curveStore;
		recompute(currentTime, true);
		propChange
				.firePropertyChange("curveStore", this.curveStore, curveStore);
	}

	public void setInitialPos(final RockSet<Pos> initialPos) {
		this.initialPos.setLocation(initialPos);
	}

	public void setInitialVel(final RockSet<Vel> initialVel) {
		initialSpeed.setLocation(initialVel);
	}

	public void setSuspended(final boolean suspend) {
		final int old = collected;
		if (suspend) {
			if (collected < 0)
				collected = 0;
		} else {
			collected = -1;
			if (old <= 0)
				return;
			recompute(currentTime, true);
		}
	}

	/** One of the initial rocks changed either pos or vel */
	public void stateChanged(final ChangeEvent arg0) {
		final Object src = arg0 == null ? null : arg0.getSource();
		if (src == null || src == initialPos || src == initialSpeed)
			;// recompute(currentTime, true);
		else if (initialPos.findI16(src) >= 0) {
			log.debug("Startpos rock change");
			if (getSuspended())
				collected |= 1 << initialPos.findI16(src);
			else
				recompute(currentTime, true);
		} else if (initialSpeed.findI16(src) >= 0) {
			log.debug("Startvel rock change");
			if (getSuspended())
				collected |= 1 << initialSpeed.findI16(src);
			else
				recompute(currentTime, true);
		} else
			log.info(arg0);
	}
}
