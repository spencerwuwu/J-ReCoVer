// https://searchcode.com/api/result/3467024/

package jjil.algorithm.j2se;
/*
 * Gray8DetectHaarMultiScale.java
 *
 * Created on August 19, 2007, 7:33 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2007 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the Lesser GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;

import jjil.algorithm.ErrorCodes;
import jjil.algorithm.Gray8Crop;
import jjil.algorithm.Gray8RectStretch;
import jjil.algorithm.Gray8Shrink;
import jjil.core.Error;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;

/**
 * DetectHaar applies a Haar cascade at multiple locations and multiple scales
 * to an input Gray8Image. The result is a mask with the masked (non-Byte.MIN_VALUE)
 * locations indicating the areas where the feature was detected.<br>
 * The Haar cascade is applied at multiple scales, starting with the coarsest scale,
 * and working down to the finest scale. At each scale, the cascade is applied to
 * subimages spread across the image. If the cascade detects a feature, the area of
 * the mask corresponding to that subimage is set to Byte.MAX_VALUE. When a subimage
 * is to be tested, the mask is first examined to see if the central pixel in the
 * mask area corresponding to that subimage is masked. If it is, the subimage is 
 * skipped. When transitioning to a finer scale, the mask is stretched to the new
 * size. This results in areas where features have been detected at a coarser scale
 * not being re-searched at a finer scale.<br>
 * Gray8DetectHaarMultiScale is structured as a pipeline stage so push'ing an image
 * results in a new mask being available on getFront. The mask can be further processed
 * by doing connected component detection to determine the feature characteristics,
 * or the mask can be displayed in an overlay on the original image to show the
 * feature areas.
 * @author webb
 */
public class Gray8DetectHaarMultiScale extends PipelineStage {
    private HaarClassifierCascade hcc;
    // maximum scale is the largest factor the image is divided by
    private float fMaxScale = 10.0f;
    // minimum scale is the smallest factor the image is divided by
    private float fMinScale = 5.0f;
    // scale change is the change in scale from one search to the next
    // times 256
    private float fScaleChange = 1.2f;

    // nStep is the amount we divide the image size by to get the
    // number of pixels we step between detection. It is always at
    // least 1 so setting this number large gurantees detecting at
    // every pixel.
    private int nStep = 30;
    
    // rc is the collection of detected rectangles
    private RectCollection rc = new RectCollection();
    
    /**
     * Creates a new instance of Gray8DetectHaarMultiScale. The scale parameters correspond
     * to the size of a square area in the original input image that are averaged to
     * create a single pixel in the image used for detection. A scale factor of 1 would
     * do detection at full image resolution.
     * @param is Input stream containing the Haar cascade. This input stream is created
     * by the Haar2J2me program (run on a PC) from a Haar cascade that has been
     * trained using the OpenCV. See {http://sourceforge.net/projects/opencv} for
     * more information about the OpenCV. The Haar2J2me program should be available
     * wherever you got this code from.
     * @param fMinScale Minimum (finest) scale at which features will be detected.
     * @param fMaxScale Maximum (coarsest) scale at which features will be detected.
     * @throws jjil.core.Error if there is an error in the input file.
     * @throws java.io.IOException if there is an I/O error reading the input file.
     */
    public Gray8DetectHaarMultiScale(InputStream is, float fMinScale, float fMaxScale) 
    	throws jjil.core.Error, IOException
    {
        this.fMinScale = fMinScale;
        this.fMaxScale = fMaxScale;
        // load Haar classifier cascade
        InputStreamReader isr = new InputStreamReader(is);
        this.hcc = HaarClassifierCascade.fromStream(isr);
    }
    
    /**
     * Detect rectangles in imGray
     * @param imGray image to detect rectangles in
     * @throws jjil.core.Error if image is too small
     */
    public void detect(Gray8Image imGray) throws jjil.core.Error {
        this.rc = new RectCollection();
        
        if (imGray.getWidth() < this.hcc.getWidth() ||
            imGray.getHeight() < this.hcc.getHeight()) {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            ErrorCodes.IMAGE_TOO_SMALL,
                            imGray.toString(),
                            this.hcc.toString(),
                            null);
        }
        float fScale = Math.min(this.fMaxScale, 
                Math.min(((float)imGray.getWidth()) / ((float)this.hcc.getWidth()),
                ((float)imGray.getHeight()) / ((float)this.hcc.getHeight())));
        while (fScale >= this.fMinScale) {
            // shrink the input image
            int nTargetWidth = (int) (imGray.getWidth() / fScale);
            int nTargetHeight = (int) (imGray.getHeight() / fScale);
            int nStepHoriz = Math.max(1, nTargetWidth / this.nStep);
            int nStepVert = Math.max(1, nTargetHeight / this.nStep);
            Gray8Shrink gs = new Gray8Shrink(nTargetWidth, nTargetHeight);
            gs.push(imGray);
            Gray8Image imShrunk = (Gray8Image) gs.getFront();
            for (int i=0; i<imShrunk.getWidth()-this.hcc.getWidth(); i+=nStepHoriz) {
                // compute left coordinate in original image
                int nXPos = (i * imGray.getWidth()) / imShrunk.getWidth();
                for (int j=0; j<imShrunk.getHeight()-this.hcc.getHeight(); j+=nStepVert) {
                    // compute top coordinate of in original image
                    int nYPos = (j * imGray.getHeight()) / imShrunk.getHeight();
                    // compute rectangle center in original image
                    Point p = new Point(
                            nXPos + (this.hcc.getWidth() * imGray.getWidth()) / 
                                        imShrunk.getWidth() / 2, 
                            nYPos + (this.hcc.getHeight() * imGray.getHeight()) / 
                                        imShrunk.getHeight() / 2);
                    // check if this point has already been tested
                    if (rc.contains(p) == null) {
                        // no, crop image to this rectangle
                        Gray8Crop gcc = new Gray8Crop(
                                i, 
                                j, 
                                this.hcc.getWidth(), 
                                this.hcc.getHeight());
                        gcc.push(imShrunk);
                        // see if there's a face there
                        if (hcc.eval(gcc.getFront())) {
                            // there is, add scaled rectangle to rectangle list
                            Rectangle r = new Rectangle(
                                    nXPos, 
                                    nYPos, 
                                    (this.hcc.getWidth() * imGray.getWidth()) / 
                                        imShrunk.getWidth(), 
                                    (this.hcc.getHeight() * imGray.getHeight()) / 
                                        imShrunk.getHeight());
                            System.out.println("Found something " + r.toString());
                            this.rc.add(r);
                        }
                    }
                }
            }
            fScale = fScale / this.fScaleChange;
        }

    }
    
    /**
     * Apply multi-scale Haar cascade and prepare a mask image showing where features
     * were detected.
     * @param image Input Gray8Image.
     * @throws jjil.core.Error if the input is not a Gray8Image or is too small.
     */
         
    @Override
	public void push(Image image) throws jjil.core.Error
    {
        Gray8Image imGray;
        if (image instanceof Gray8Image) {
            imGray = (Gray8Image) image;
        } else {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
                            image.toString(),
                            null,
                            null);
        }
        
        this.detect(imGray);

        // Zero the mask
        Gray8Image imMask = new Gray8Image(
                imGray.getWidth(), 
                imGray.getHeight(), 
                Byte.MIN_VALUE);
        for (Enumeration<Rectangle> e = rc.elements(); e.hasMoreElements();) {
            Rectangle r = e.nextElement();
            Gray8Rect gr = new Gray8Rect(r, Byte.MAX_VALUE);
            gr.push(imMask);
            imMask = (Gray8Image) gr.getFront();
        }
        // Stretch imMask to original image size; this is the result
        Gray8RectStretch grs = new Gray8RectStretch(image.getWidth(), image.getHeight());
        grs.push(imMask);
        super.setOutput(grs.getFront());
    }
        
    /**
     * Get the rectangles where the detect returned true
     * @return an Enumeration giving the rectangles detected
     */
    public Enumeration<Rectangle> getDetectedRegions(){
        return this.rc.elements();
    }
    
    
    /**
     * Set minimum and maximum scale.
     * @param fMinScale The finest scale -- a scale factor of 1 corresponds to the full image resolution.
     * @param fMaxScale The coarsest scale. A scale factor equal to the image width (for a square
     * image) would mean the entire image is reduced to a single pixel.<br>
     * <B>Note.</B> The maximum scale actually used is the maximum of this 
     * number and the scale which would  reduce the image size to the smallest
     * size that the image used in the Haar cascade would fit inside.
     */
    public void setScale(int fMinScale, int fMaxScale) {
        this.fMinScale = fMinScale;
        this.fMaxScale = fMaxScale;
    }
    
    /**
     * Set step. We move the detection window by an amount equal to
     * this number divided into the image size. So if the value is n we
     * try n detections horizontally and vertically in the image.
     * We always step at least 1 so setting this number large gurantees 
     * detecting at every pixel.
     * @param nStep the new step value. Default is 30.
     */
    public void setStep(int nStep) {
        this.nStep = nStep;
    }
}

