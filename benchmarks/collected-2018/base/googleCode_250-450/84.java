// https://searchcode.com/api/result/3466977/

/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the Lesser GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jjil.j2se.ocr;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import jjil.algorithm.Gray8OtsuThreshold;
import jjil.core.Point;
import jjil.core.Vec2;
import jjil.j2se.algorithm.CircularList;
import jjil.j2se.algorithm.Pair;

// TODO: Auto-generated Javadoc
/**
 * The Class Features transforms a list of edge points into a collection
 * of features, which can then be matched with a learnt character template
 * (PrototypesCollection) in the CharMatcher class.
 * 
 * @author webb
 */
public class Features extends ArrayList<Feature> {
	
	/** Computed serialVersionUID. */
	private static final long serialVersionUID = -5447662603546860723L;

	/** The maximum number of features allowed. This is a fixed value because we avoid using new during character match, in order to avoid garbage collection and optimize performance under Android. */
	private final static int MAX_FEATURES = 2000;
    
    /** The number of features (in the static list below) that are currently in use. */
    private static int snFeaturesUsed = 0;
    
    /** The static array of features, which are used to store the features measured from this character. The ArrayList of features created by this class are all in this static list. The list is static for performance optimization, to reduce garbage collection. */
    private static Feature[] srFeatures = new Feature[MAX_FEATURES];
    
	/** A class for computing the Otsu threshold. This is used when we have to split an unmatchable character into two parts so as to handle characters which accidentally touch. */
	private Gray8OtsuThreshold mGray8OtsuThreshold = 
		new Gray8OtsuThreshold(true, 256);
    
    /** The list of edge points, which define the boundary of the character. */
    private List<EdgePts> mleps;
    
    /** The mn components. */
    int mnComponents = 0;
    // Length of the outline
    /** The length of the outline. */
    int mnLength = 0;
    // These are the radius of gyration
    /** The mn rx inv. */
    int mnRxInv;
    
    /** The mn rx exp. */
    int mnRxExp;
    
    /** The mn ry inv. */
    int mnRyInv;
    
    /** The mn ry exp. */
    int mnRyExp;

    /** The position of the center of the outline. */
    Vec2 mvMean;

    /** The precomputed math, used to eliminate some lengthy computations. */
    static PrecomputeMath mPrecomputeMath = null;

    static {
    	for (int i=0; i<srFeatures.length; i++) {
    		srFeatures[i] = new Feature();
    	}
    }

    /**
     * Instantiates a new compute features.
     * 
     * @param leps the leps
     */
    public Features(List<EdgePts> leps) {
    	this.mnComponents = leps.size();
        this.mleps = leps;
        /* find this.mvMean.getX(), this.mvMean.getY() */
        Vec2 Sum = new Vec2(0, 0);
        int LengthSum = 0;
        for (CircularList<EdgePt> OutLine : leps) {
            if (OutLine.size() <= 1) {
                continue;
            }
            CircularList<EdgePt>.CircListIter li = OutLine.circListIterator();
            Point Last = li.getNext().getPos();
            while (li.hasNext()) {
                li.next();
                Point Norm = li.getNext().getPos();
                int n = 1;
                Vec2 Delta = new Vec2(Last, Norm);
                int Length = 0;
                try {
                    Length = Delta.length();
                } catch (jjil.core.Error ex) {
                }
                n = ((Length << 2) + Length + 32) >> 6;
                if (n != 0) {
                    Sum.add(((Last.getX()<<1)+Delta.getX())*Length,
                    		((Last.getY()<<1)+Delta.getY())*Length);
                    LengthSum += Length;
                }
                if (n != 0) {
                    Last = Norm;
                }
            }
        }
        if (LengthSum == 0) {
            return;
        }

        this.mnLength = LengthSum;
        this.mvMean = Sum.div(LengthSum).rsh(1);

        /* Find 2nd moments & radius of gyration */
        computeRadiusGyration(leps);

        /* extract character normalized features */
        computeFeatures(leps);
    }

    /**
     * Compute features. Transformas a list of EdgePts into features.
     * 
     * @param leps the list of EdgePts. Each EdgePts object is a closed
     * boundary (internal or external) of the character to be recognized.
     */
    private void computeFeatures(List<EdgePts> leps) {
        for (CircularList<EdgePt> Loop : leps) {
            /* Check for bad loops */
            if (Loop.size() <= 1) {
                return;
            }
            Point Last = null;
            for (ListIterator<EdgePt> li = Loop.loopIterator();
                    li.hasNext();) {
                EdgePt Segment = li.next();
                int LastX = (Segment.getPos().getX() - this.mvMean.getX()) *
                        this.mnRyInv;
                int LastY = (Segment.getPos().getY() - this.mvMean.getY()) *
                        this.mnRxInv;
                LastX >>= this.mnRyExp;
                LastY >>= this.mnRxExp;
                Point Norm = new Point(LastX, LastY);
                if (Last == null) {
                    Last = Norm;
                } else {
                    int n = 1;
                    Vec2 Delta = new Vec2(Last, Norm);
                    int Length = 0;
                    try {
                        Length = Delta.length();
                    } catch (jjil.core.Error ex) {
                    }
                    n = ((Length << 2) + Length + 32) >> 6;
                    if (n != 0) {
                        short Theta = mPrecomputeMath.TableLookup(Delta);
                        Vec2 d = Delta.lsh(8).div(n);
                        Vec2 pf = new Vec2(Last).lsh(8).add(d.clone().rsh(1));
                        for (int i = 0; i < n; i++) {
                        	// check to see if we're out of static feature
                        	// slots
                        	if (snFeaturesUsed == MAX_FEATURES) {
                        		return;
                        	}
                        	Feature f = srFeatures[snFeaturesUsed++];
                        	f.set((short) (pf.getX() >> 8),
                                    (short) ((pf.getY() >> 8)),
                                    Theta);
                            if (!this.add(f)) {
                                return;
                            }
                            pf.add(d);
                        }
                    }
                    if (n != 0) {              /* Throw away a point that is too close */
                        Last = Norm;
                    }
                }
            }
        }

    }

    /**
     * Compute the radius of gyration.
     * 
     * @param leps the leps
     */
    private void computeRadiusGyration(List<EdgePts> leps) {
        Vec2 vMeanShift = this.mvMean.clone().lsh(8);
        int nBLFeat = 0;
        Vec2 I = new Vec2(0, 0);
        for (CircularList<EdgePt> Outline : leps) {
            if (Outline.size() <= 1) {
                continue;
            }
            Point Last = null;
            for (ListIterator<EdgePt> li = Outline.loopIterator();
                    li.hasNext();) {
                EdgePt Segment = li.next();
                Point Norm = Segment.getPos().clone();
                if (Last == null) {
                    Last = Norm;
                } else {
                    int n = 1;
                    Vec2 Delta = new Vec2(Last, Norm);
                    int Length = 0;
                    try {
                        Length = Delta.length();
                    } catch (jjil.core.Error ex) {
                    }
                    n = ((Length << 2) + Length + 32) >> 6;
                    nBLFeat += n;
                    if (n != 0) {
                        Vec2 d = Delta.lsh(8).div(n);
                        Vec2 pf = new Vec2(Last).lsh(8).add(d.clone().rsh(1)).
                                sub(vMeanShift);
                        long lX = (long) pf.getX() * pf.getX() * n +
                                (long) pf.getX() * d.getX() * n * (n - 1) +
                                (long) d.getX() * d.getX() * n * (n - 1) * (2 * n - 1) / 6;
                        long lY = (long) pf.getY() * pf.getY() * n +
                                (long) pf.getY() * d.getY() * n * (n - 1) +
                                (long) d.getY() * d.getY() * n * (n - 1) * (2 * n - 1) / 6;
                        I.add((int) (lX >> 16), (int) (lY >> 16));
                        Last = Norm;
                    }
                }
            }
        }
        Pair<Vec2, Vec2> p = mPrecomputeMath.getResult(nBLFeat,
                I.getY() == 0 ? 1 : I.getY(), I.getX() == 0 ? 1 : I.getX());
        this.mnRxInv = p.getFirst().getX();
        this.mnRxExp = p.getFirst().getY();
        this.mnRyInv = p.getSecond().getX();
        this.mnRyExp = p.getSecond().getY();
    }
    
    /**
     * Gets the components.
     * 
     * @return the components
     */
    public int getComponents() {
    	return this.mnComponents;
    }

    /**
     * Gets the length.
     * 
     * @return the length
     */
    public int getLength() {
        return this.mnLength;
    }

    /**
     * Gets the xmean.
     * 
     * @return the xmean
     */
    public int getXmean() {
        return this.mvMean.getX();
    }

    /**
     * Gets the ymean.
     * 
     * @return the ymean
     */
    public int getYmean() {
        return this.mvMean.getY();
    }
    
    /**
     * Reset features.
     */
    static public void resetFeatures() {
    	snFeaturesUsed = 0;
    }

    /**
     * Sets the precompute math.
     * 
     * @param pm the new precompute math
     */
    public static void setPrecomputeMath(PrecomputeMath pm) {
        mPrecomputeMath = pm;
    }

    /**
     * Split this Features into two Features's, recomputing the features.
     * 
     * @param nX X position to split on. Points < this X will get put in the first
     * Features, points >= this X will get put in the second/
     * 
     * @return the pair< compute features, compute features>
     */
    private Pair<Features, Features> split(int nX) {
        List<EdgePts> lepsLeft = null, lepsRight = null;
        for (EdgePts eps : this.mleps) {
            Pair<List<EdgePts>, List<EdgePts>> pr = eps.split(nX);
            if (pr.getFirst() != null) {
                if (lepsLeft == null) {
                    lepsLeft = pr.getFirst();
                } else {
                    lepsLeft.addAll(pr.getFirst());
                }
            }
            if (pr.getSecond() != null) {
                if (lepsRight == null) {
                    lepsRight = pr.getSecond();
                } else {
                    lepsRight.addAll(pr.getSecond());
                }
            }
        }
        Features nfsLeft = null, nfsRight = null;
        nfsLeft = new Features(lepsLeft);
        if (nfsLeft.size() == 0) {
            nfsLeft = null;
        }
        nfsRight = new Features(lepsRight);
        if (nfsRight.size() == 0) {
            nfsRight = null;
        }
        return new Pair<Features,Features>(nfsLeft, nfsRight);
    }

    /**
     * Test and split.
     * 
     * @param nMinWidth the n min width
     * 
     * @return the pair< compute features, compute features>
     */
    public Pair<Features, Features> testAndSplit(int nMinWidth) {
        int nMinX = Integer.MAX_VALUE;
        int nMaxX = Integer.MIN_VALUE;
        for (EdgePts eps : this.mleps) {
            for (EdgePt ep : eps) {
                nMinX = Math.min(nMinX, ep.getPos().getX());
                nMaxX = Math.max(nMaxX, ep.getPos().getX());
            }
        }
        int rnHistogram[] = new int[nMaxX - nMinX + 1];
        for (EdgePts eps : this.mleps) {
            for (EdgePt ep : eps) {
                rnHistogram[ep.getPos().getX() - nMinX]++;
            }
        }
        int nSplitPoint = this.mGray8OtsuThreshold.calculateOtsuThreshold(rnHistogram);
        if (nSplitPoint > nMinWidth && nSplitPoint < rnHistogram.length-nMinWidth) {
            return this.split(nSplitPoint + nMinX);
        } else {
            return null;
        }
    }
}


