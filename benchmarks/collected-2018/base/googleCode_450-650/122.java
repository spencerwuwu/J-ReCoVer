// https://searchcode.com/api/result/13716206/

/*
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com), 
 * modifications by Nikolaus Moll
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */
package org.jowidgets.impl.layout.miglayout.common;

import java.beans.Beans;
import java.beans.ExceptionListener;
import java.beans.Introspector;
import java.beans.PersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.IdentityHashMap;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.jowidgets.impl.layout.miglayout.MigLayoutToolkit;

/**
 * A utility class that has only static helper methods.
 */
public final class LayoutUtil {
	public static final int MIN = 0;
	public static final int PREF = 1;
	public static final int MAX = 2;

	/**
	 * A substitute value for aa really large value. Integer.MAX_VALUE is not used since that means a lot of defensive code
	 * for potential overflow must exist in many places. This value is large enough for being unreasonable yet it is hard to
	 * overflow.
	 */
	public static final int INF = (Integer.MAX_VALUE >> 10) - 100; // To reduce likelihood of overflow errors when calculating.

	/**
	 * Tag int for a value that in considered "not set". Used as "null" element in int arrays.
	 */
	public static final int NOT_SET = Integer.MIN_VALUE + 12346; // Magic value...

	// Index for the different sizes

	private final boolean hasBeans;

	private volatile WeakHashMap<Object, String> crMap = null;
	private volatile WeakHashMap<Object, Boolean> dtMap = null; // The Containers that have design time. Value not used.
	private int eSz = 0;

	private ByteArrayOutputStream writeOutputStream = null;
	private byte[] readBuf = null;
	private final IdentityHashMap<Object, Object> serMap = new IdentityHashMap<Object, Object>(2);

	public LayoutUtil() {
		hasBeans = initializeHasBeans();
	}

	/**
	 * Returns the current version of MiG Layout.
	 * 
	 * @return The current version of MiG Layout. E.g. "3.6.3" or "4.0"
	 */
	public String getVersion() {
		return "4.0";
	}

	private boolean initializeHasBeans() {
		try {
			LayoutUtil.class.getClassLoader().loadClass("java.beans.Beans");
			return true;
		}
		catch (final ClassNotFoundException e) {
			return false;
		}
	}

	public boolean hasBeans() {
		return hasBeans;
	}

	/**
	 * Sets if design time is turned on for a Container in {@link IContainerWrapper}.
	 * 
	 * @param cw The container to set design time for. <code>null</code> is legal and can be used as
	 *            a key to turn on/off design time "in general". Note though that design time "in general" is
	 *            always on as long as there is at least one ContainerWrapper with design time.
	 *            <p>
	 *            <strong>If this method has not ever been called it will default to what <code>Beans.isDesignTime()</code>
	 *            returns.</strong> This means that if you call this method you indicate that you will take responsibility for the
	 *            design time value.
	 * @param b <code>true</code> means design time on.
	 */
	public void setDesignTime(final IContainerWrapper cw, final boolean b) {
		if (dtMap == null) {
			dtMap = new WeakHashMap<Object, Boolean>();
		}

		dtMap.put((cw != null ? cw.getComponent() : null), b);
	}

	/**
	 * Returns if design time is turned on for a Container in {@link IContainerWrapper}.
	 * 
	 * @param cw The container to set design time for. <code>null</code> is legal will return <code>true</code> if there is at
	 *            least one <code>ContainerWrapper</code> (or <code>null</code>) that have design time
	 *            turned on.
	 * @return If design time is set for <code>cw</code>.
	 */
	public boolean isDesignTime(IContainerWrapper cw) {
		if (dtMap == null) {
			return hasBeans && Beans.isDesignTime();
		}

		if (cw != null && dtMap.containsKey(cw.getComponent()) == false) {
			cw = null;
		}

		final Boolean b = dtMap.get(cw != null ? cw.getComponent() : null);
		return b != null && b;
	}

	/**
	 * The size of an empty row or columns in a grid during design time.
	 * 
	 * @return The number of pixels. Default is 15.
	 */
	public int getDesignTimeEmptySize() {
		return eSz;
	}

	/**
	 * The size of an empty row or columns in a grid during design time.
	 * 
	 * @param pixels The number of pixels. Default is 0 (it was 15 prior to v3.7.2, but since that meant different behaviour
	 *            under design time by default it was changed to be 0, same as non-design time). IDE vendors can still set it to
	 *            15 to
	 *            get the old behaviour.
	 */
	public void setDesignTimeEmptySize(final int pixels) {
		eSz = pixels;
	}

	/**
	 * Associates <code>con</code> with the creation string <code>s</code>. The <code>con</code> object should
	 * probably have an equals method that compares identities or <code>con</code> objects that .equals() will only
	 * be able to have <b>one</b> creation string.
	 * <p>
	 * If {@link LayoutUtil#isDesignTime(IContainerWrapper)} returns <code>false</code> the method does nothing.
	 * 
	 * @param con The object. if <code>null</code> the method does nothing.
	 * @param s The creation string. if <code>null</code> the method does nothing.
	 */
	void putCCString(final Object con, final String s) {
		if (s != null && con != null && isDesignTime(null)) {
			if (crMap == null) {
				crMap = new WeakHashMap<Object, String>(64);
			}

			crMap.put(con, s);
		}
	}

	/**
	 * Sets/add the persistence delegates to be used for a class.
	 * 
	 * @param c The class to set the registered deligate for.
	 * @param del The new delegate or <code>null</code> to erase to old one.
	 */
	synchronized void setDelegate(@SuppressWarnings("rawtypes") final Class c, final PersistenceDelegate del) {
		try {
			Introspector.getBeanInfo(c, Introspector.IGNORE_ALL_BEANINFO).getBeanDescriptor().setValue("persistenceDelegate", del);
		}
		catch (final Exception ignored) {
		}
	}

	/**
	 * Returns strings set with {@link #putCCString(Object, String)} or <code>null</code> if nothing is associated or
	 * {@link LayoutUtil#isDesignTime(IContainerWrapper)} returns <code>false</code>.
	 * 
	 * @param con The constrain object.
	 * @return The creation string or <code>null</code> if nothing is registered with the <code>con</code> object.
	 */
	String getCCString(final Object con) {
		return crMap != null ? crMap.get(con) : null;
	}

	void throwCC() {
		throw new IllegalStateException("setStoreConstraintData(true) must be set for strings to be saved.");
	}

	/**
	 * Takes a number on min/preferred/max sizes and resize constraints and returns the calculated sizes which sum should add up
	 * to <code>bounds</code>. Whether the sum
	 * will actually equal <code>bounds</code> is dependent om the pref/max sizes and resize constraints.
	 * 
	 * @param sizes [ix],[MIN][PREF][MAX]. Grid.CompWrap.NOT_SET will be treated as N/A or 0. A "[MIN][PREF][MAX]" array with null
	 *            elements will be interpreted as very flexible (no bounds)
	 *            but if the array itself is null it will not get any size.
	 * @param resConstr Elements can be <code>null</code> and the whole array can be <code>null</code>. <code>null</code> means
	 *            that the size will not be flexible at all.
	 *            Can have length less than <code>sizes</code> in which case the last element should be used for the elements
	 *            missing.
	 * @param defPushWeights If there is no grow weight for a resConstr the corresponding value of this array is used.
	 *            These forced resConstr will be grown last though and only if needed to fill to the bounds.
	 * @param startSizeType The initial size to use. E.g. {@link net.miginfocom.layout.LayoutUtil#MIN}.
	 * @param bounds To use for relative sizes.
	 * @return The sizes. Array length will match <code>sizes</code>.
	 */
	int[] calculateSerial(
		final int[][] sizes,
		final ResizeConstraint[] resConstr,
		final Float[] defPushWeights,
		final int startSizeType,
		final int bounds) {
		final float[] lengths = new float[sizes.length]; // heights/widths that are set
		float usedLength = 0.0f;

		// Give all preferred size to start with
		for (int i = 0; i < sizes.length; i++) {
			if (sizes[i] != null) {
				float len = sizes[i][startSizeType] != NOT_SET ? sizes[i][startSizeType] : 0;
				final int newSizeBounded = getBrokenBoundary(len, sizes[i][MIN], sizes[i][MAX]);
				if (newSizeBounded != NOT_SET) {
					len = newSizeBounded;
				}

				usedLength += len;
				lengths[i] = len;
			}
		}

		final int useLengthI = Math.round(usedLength);
		if (useLengthI != bounds && resConstr != null) {
			final boolean isGrow = useLengthI < bounds;

			// Create a Set with the available priorities
			final TreeSet<Integer> prioList = new TreeSet<Integer>();
			for (int i = 0; i < sizes.length; i++) {
				final ResizeConstraint resC = (ResizeConstraint) getIndexSafe(resConstr, i);
				if (resC != null) {
					prioList.add(isGrow ? resC.growPrio : resC.shrinkPrio);
				}
			}
			final Integer[] prioIntegers = prioList.toArray(new Integer[prioList.size()]);

			for (int force = 0; force <= ((isGrow && defPushWeights != null) ? 1 : 0); force++) { // Run twice if defGrow and the need for growing.
				for (int pr = prioIntegers.length - 1; pr >= 0; pr--) {
					final int curPrio = prioIntegers[pr];

					float totWeight = 0f;
					final Float[] resizeWeight = new Float[sizes.length];
					for (int i = 0; i < sizes.length; i++) {
						if (sizes[i] == null) {
							continue;
						}

						final ResizeConstraint resC = (ResizeConstraint) getIndexSafe(resConstr, i);
						if (resC != null) {
							final int prio = isGrow ? resC.growPrio : resC.shrinkPrio;

							if (curPrio == prio) {
								if (isGrow) {
									resizeWeight[i] = (force == 0 || resC.grow != null)
											? resC.grow : (defPushWeights[i < defPushWeights.length
													? i : defPushWeights.length - 1]);
								}
								else {
									resizeWeight[i] = resC.shrink;
								}
								if (resizeWeight[i] != null) {
									totWeight += resizeWeight[i];
								}
							}
						}
					}

					if (totWeight > 0f) {
						boolean hit;
						do {
							final float toChange = bounds - usedLength;
							hit = false;
							float changedWeight = 0f;
							for (int i = 0; i < sizes.length && totWeight > 0.0001f; i++) {

								final Float weight = resizeWeight[i];
								if (weight == null) {
									continue;
								}
								float sizeDelta = toChange * weight / totWeight;
								float newSize = lengths[i] + sizeDelta;

								if (sizes[i] != null) {
									final int newSizeBounded = getBrokenBoundary(newSize, sizes[i][MIN], sizes[i][MAX]);
									if (newSizeBounded != NOT_SET) {
										resizeWeight[i] = null;
										hit = true;
										changedWeight += weight;
										newSize = newSizeBounded;
										sizeDelta = newSize - lengths[i];
									}
								}

								lengths[i] = newSize;
								usedLength += sizeDelta;
							}
							totWeight -= changedWeight;
						}
						while (hit);
					}
				}
			}
		}
		return roundSizes(lengths);
	}

	Object getIndexSafe(final Object[] arr, final int ix) {
		return arr != null ? arr[ix < arr.length ? ix : arr.length - 1] : null;
	}

	/**
	 * Returns the broken boundary if <code>sz</code> is outside the boundaries <code>lower</code> or <code>upper</code>. If both
	 * boundaries
	 * are broken, the lower one is returned. If <code>sz</code> is &lt; 0 then <code>new Float(0f)</code> is returned so that no
	 * sizes can be
	 * negative.
	 * 
	 * @param sz The size to check
	 * @param lower The lower boundary (or <code>null</code> fo no boundary).
	 * @param upper The upper boundary (or <code>null</code> fo no boundary).
	 * @return The broken boundary.
	 */
	private int getBrokenBoundary(final float sz, final int lower, final int upper) {
		if (lower != NOT_SET) {
			if (sz < lower) {
				return lower;
			}
		}
		else if (sz < 0f) {
			return 0;
		}

		if (upper != NOT_SET && sz > upper) {
			return upper;
		}

		return NOT_SET;
	}

	static int sum(final int[] terms, final int start, final int len) {
		int s = 0;
		final int iSz = start + len;
		for (int i = start; i < iSz; i++) {
			s += terms[i];
		}
		return s;
	}

	static int sum(final int[] terms) {
		return sum(terms, 0, terms.length);
	}

	public int getSizeSafe(final int[] sizes, final int sizeType) {
		if (sizes == null || sizes[sizeType] == NOT_SET) {
			return sizeType == MAX ? LayoutUtil.INF : 0;
		}
		return sizes[sizeType];
	}

	BoundSize derive(final BoundSize bs, final UnitValue min, final UnitValue pref, final UnitValue max) {
		if (bs == null || bs.isUnset()) {
			return new BoundSize(min, pref, max, null);
		}

		return new BoundSize(min != null ? min : bs.getMin(), pref != null ? pref : bs.getPreferred(), max != null
				? max : bs.getMax(), bs.getGapPush(), null);
	}

	/**
	 * Returns if left-to-right orientation is used. If not set explicitly in the layout constraints the Locale
	 * of the <code>parent</code> is used.
	 * 
	 * @param lc The constraint if there is one. Can be <code>null</code>.
	 * @param container The parent that may be used to get the left-to-right if ffc does not specify this.
	 * @return If left-to-right orientation is currently used.
	 */
	public boolean isLeftToRight(final LC lc, final IContainerWrapper container) {
		if (lc != null && lc.getLeftToRight() != null) {
			return lc.getLeftToRight();
		}

		return container == null || container.isLeftToRight();
	}

	/**
	 * Round a number of float sizes into int sizes so that the total length match up
	 * 
	 * @param sizes The sizes to round
	 * @return An array of equal length as <code>sizes</code>.
	 */
	int[] roundSizes(final float[] sizes) {
		final int[] retInts = new int[sizes.length];
		float posD = 0;

		for (int i = 0; i < retInts.length; i++) {
			final int posI = (int) (posD + 0.5f);

			posD += sizes[i];

			retInts[i] = (int) (posD + 0.5f) - posI;
		}

		return retInts;
	}

	/**
	 * Safe equals. null == null, but null never equals anything else.
	 * 
	 * @param o1 The first object. May be <code>null</code>.
	 * @param o2 The second object. May be <code>null</code>.
	 * @return Returns <code>true</code> if <code>o1</code> and <code>o2</code> are equal (using .equals()) or both are
	 *         <code>null</code>.
	 */
	boolean equals(final Object o1, final Object o2) {
		return o1 == o2 || (o1 != null && o2 != null && o1.equals(o2));
	}

	//	static int getBaselineCorrect(Component comp)
	//	{
	//		Dimension pSize = comp.getPreferredSize();
	//		int baseline = comp.getBaseline(pSize.width, pSize.height);
	//		int nextBaseline = comp.getBaseline(pSize.width, pSize.height + 1);
	//
	//		// Amount to add to height when calculating where baseline
	//		// lands for a particular height:
	//		int padding = 0;
	//
	//		// Where the baseline is relative to the mid point
	//		int baselineOffset = baseline - pSize.height / 2;
	//		if (pSize.height % 2 == 0 && baseline != nextBaseline) {
	//			padding = 1;
	//		} else if (pSize.height % 2 == 1 && baseline == nextBaseline) {
	//			baselineOffset--;
	//			padding = 1;
	//		}
	//
	//		// The following calculates where the baseline lands for
	//		// the height z:
	//		return (pSize.height + padding) / 2 + baselineOffset;
	//	}

	/**
	 * Returns the inset for the side.
	 * 
	 * @param side top == 0, left == 1, bottom = 2, right = 3.
	 * @param getDefault If <code>true</code> the default insets will get retrieved if <code>lc</code> has none set.
	 * @return The inset for the side. Never <code>null</code>.
	 */
	UnitValue getInsets(final LC lc, final int side, final boolean getDefault) {
		final UnitValue[] i = lc.getInsets();
		return (i != null && i[side] != null) ? i[side] : (getDefault ? MigLayoutToolkit.getMigPlatformDefaults().getPanelInsets(
				side) : MigLayoutToolkit.getMigUnitValueToolkit().ZERO);
	}

	/**
	 * Writes the objet and CLOSES the stream. Uses the persistence delegate registered in this class.
	 * 
	 * @param os The stream to write to. Will be closed.
	 * @param o The object to be serialized.
	 * @param listener The listener to recieve the exeptions if there are any. If <code>null</code> not used.
	 */
	void writeXMLObject(final OutputStream os, final Object o, final ExceptionListener listener) {
		final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(LayoutUtil.class.getClassLoader());

		final XMLEncoder encoder = new XMLEncoder(os);

		if (listener != null) {
			encoder.setExceptionListener(listener);
		}

		encoder.writeObject(o);
		encoder.close(); // Must be closed to write.

		Thread.currentThread().setContextClassLoader(oldClassLoader);
	}

	/**
	 * Writes an object to XML.
	 * 
	 * @param out The boject out to write to. Will not be closed.
	 * @param o The object to write.
	 */
	public synchronized void writeAsXML(final ObjectOutput out, final Object o) throws IOException {
		if (writeOutputStream == null) {
			writeOutputStream = new ByteArrayOutputStream(16384);
		}

		writeOutputStream.reset();

		writeXMLObject(writeOutputStream, o, new ExceptionListener() {
			@Override
			public void exceptionThrown(final Exception e) {
				//CHECKSTYLE:OFF
				e.printStackTrace();
				//CHECKSTYLE:ON
			}
		});

		final byte[] buf = writeOutputStream.toByteArray();

		out.writeInt(buf.length);
		out.write(buf);
	}

	/**
	 * Reads an object from <code>in</code> using the
	 * 
	 * @param in The object input to read from.
	 * @return The object. Never <code>null</code>.
	 * @throws IOException If there was a problem saving as XML
	 */
	public synchronized Object readAsXML(final ObjectInput in) throws IOException {
		if (readBuf == null) {
			readBuf = new byte[16384];
		}

		final Thread cThread = Thread.currentThread();
		ClassLoader oldCL = null;

		try {
			oldCL = cThread.getContextClassLoader();
			cThread.setContextClassLoader(LayoutUtil.class.getClassLoader());
		}
		catch (final SecurityException ignored) {
		}

		Object o = null;
		try {
			final int length = in.readInt();
			if (length > readBuf.length) {
				readBuf = new byte[length];
			}

			in.readFully(readBuf, 0, length);

			o = new XMLDecoder(new ByteArrayInputStream(readBuf, 0, length)).readObject();

		}
		catch (final EOFException ignored) {
		}

		if (oldCL != null) {
			cThread.setContextClassLoader(oldCL);
		}

		return o;
	}

	/**
	 * Sets the serialized object and associates it with <code>caller</code>.
	 * 
	 * @param caller The object created <code>o</code>
	 * @param o The just serialized object.
	 */
	public void setSerializedObject(final Object caller, final Object o) {
		synchronized (serMap) {
			serMap.put(caller, o);
		}
	}

	/**
	 * Returns the serialized object that are associated with <code>caller</code>. It also removes it from the list.
	 * 
	 * @param caller The original creator of the object.
	 * @return The object.
	 */
	public Object getSerializedObject(final Object caller) {
		synchronized (serMap) {
			return serMap.remove(caller);
		}
	}
}

