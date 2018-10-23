// https://searchcode.com/api/result/58855698/

/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.tracker.meanshift;

import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.ImageMultiBand;
import georegression.struct.point.Point2D_F32;

import java.util.List;

/**
 * TODO fill
 *
 * @author Peter Abeles
 */
public class TrackerMeanShiftComaniciu2003<T extends ImageMultiBand> {

	// computes the histogram inside a rotated rectangle
	LocalWeightedHistogramRotRect<T> calcHistogram;
	// the key-frame histogram which is being compared again
	float keyHistogram[];

	RectangleRotate_F32 region = new RectangleRotate_F32();

	// maximum allowed mean-shift iterations
	int maxIterations;
	// minimum change stopping condition
	float minimumChange;

	// storage for the track region at different sizes
	RectangleRotate_F32 region0 = new RectangleRotate_F32();
	RectangleRotate_F32 region1 = new RectangleRotate_F32();
	RectangleRotate_F32 region2 = new RectangleRotate_F32();
	float histogram0[];
	float histogram1[];
	float histogram2[];

	// weighting factor for change in scale
	float gamma;

	// should it update the histogram after tracking?
	boolean updateHistogram;

	public TrackerMeanShiftComaniciu2003(boolean updateHistogram,
										 int maxIterations,
										 float minimumChange,
										 LocalWeightedHistogramRotRect<T> calcHistogram) {
		this.updateHistogram = updateHistogram;
		this.maxIterations = maxIterations;
		this.minimumChange = minimumChange;
		this.calcHistogram = calcHistogram;

		keyHistogram = new float[ calcHistogram.getHistogram().length ];
		if( updateHistogram ) {
			histogram0 = new float[ calcHistogram.getHistogram().length ];
			histogram1 = new float[ calcHistogram.getHistogram().length ];
			histogram2 = new float[ calcHistogram.getHistogram().length ];
		}
	}

	public void initialize( T image , RectangleRotate_F32 initial ) {
		this.region.set(initial);
		calcHistogram.computeHistogram(image,initial);
		System.arraycopy(calcHistogram.getHistogram(),0,keyHistogram,0,keyHistogram.length);
	}

	public void track( T image ) {

		// configure the different regions based on size
		region0.set( region );
		region1.set( region );
		region2.set( region );

		region0.width  *= 0.9;
		region0.height *= 0.9;

		region2.width  *= 1.1;
		region2.height *= 1.1;

		// perform mean-shift at the different sizes and compute their distance
		updateLocation(image,region0);
		double distance0 = distanceHistogram();
		if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram0,0,histogram0.length);
		updateLocation(image,region1);
		double distance1 = distanceHistogram();
		if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram1,0,histogram1.length);
		updateLocation(image,region2);
		double distance2 = distanceHistogram();
		if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram2,0,histogram2.length);

		RectangleRotate_F32 selected = null;
		float selectedHist[] = null;
		switch( selectBest(distance0,distance1,distance2)) {
			case 0: selected = region0; selectedHist = histogram0; break;
			case 1: selected = region1; selectedHist = histogram1; break;
			case 2: selected = region2; selectedHist = histogram2; break;
		}

		// Set region to the best scale, but reduce sensitivity by weighting it against the original size
		// equation 14
		float w = selected.width*gamma + (1-gamma)*region.width;
		float h = selected.height*gamma + (1-gamma)*region.height;

		region.set(selected);
		region.width = w;
		region.height = h;

		if( updateHistogram ) {
			System.arraycopy(selectedHist,0,keyHistogram,0,keyHistogram.length);
		}
	}

	private int selectBest( double a , double b , double c ) {
		if( a < b ) {
			if( a < c )
				return 0;
			else
				return 2;
		} else if( b < c ) {
			return 1;
		} else {
			return 2;
		}
	}

	/**
	 * Updates the region's location using the standard mean-shift algorithm
	 */
	protected void updateLocation( T image , RectangleRotate_F32 region ) {

		float beforeX = region.cx;
		float beforeY = region.cy;

		for( int i = 0; i < maxIterations; i++ ) {
			calcHistogram.computeHistogram(image,region);

			List<Point2D_F32> samples = calcHistogram.getSamplePts();
			int sampleHistIndex[] = calcHistogram.getSampleHistIndex();
			float histogram[] = calcHistogram.getHistogram();

			// Compute equation 13
			float meanX = 0;
			float meanY = 0;
			float totalWeight = 0;
			for( int j = 0; j < samples.size(); j++ ) {
				Point2D_F32 samplePt = samples.get(j);

				int histIndex = sampleHistIndex[j];

				float q = keyHistogram[histIndex];
				float p = histogram[histIndex];

				// compute the weight derived from the Bhattacharyya coefficient.  Equation 10.
				float w = (float)Math.sqrt(q/p);

				meanX += w*samplePt.x;
				meanY += w*samplePt.y;
				totalWeight += w;
			}
			meanX /= totalWeight;
			meanY /= totalWeight;

			// convert to image pixels
			calcHistogram.squareToImage(meanX, meanY, region);
			meanX = calcHistogram.imageX;
			meanY = calcHistogram.imageY;

			// see if the change is below the threshold
			boolean done = Math.abs(meanX-beforeX) <= minimumChange && Math.abs(meanY-beforeY) <= minimumChange;
			beforeX = meanX;
			beforeY = meanY;

			if( done ) {
				break;
			}
		}

		// save the results
		region.cx = beforeX;
		region.cy = beforeY;
	}

	/**
	 * Compute the distance between the two distributions using Bhattacharyya coefficient
	 * Equations 6 and 7.
	 * Must be called immediately after {@link #updateLocation}.
	 */
	protected double distanceHistogram() {
		double sumP = 0;
		int sampleHistIndex[] = calcHistogram.getSampleHistIndex();
		float histogram[] = calcHistogram.getHistogram();
		for( int j = 0; j < histogram.length; j++ ) {
			int histIndex = sampleHistIndex[j];
			float q = keyHistogram[histIndex];
			float p = histogram[histIndex];
			sumP +=  Math.sqrt(q*p);
		}
		return Math.sqrt(1-sumP);
	}
}

