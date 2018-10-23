// https://searchcode.com/api/result/3467064/

/*
 * BarcodeParam.java
 *
 * Created on October 10, 2006, 3:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2006 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import jjil.algorithm.*;
import jjil.core.RgbImage;
import jjil.j2me.RgbImageJ2me;
import barcode.DetectBarcode;
import barcode.ReadBarcode;

/**
 * @author webb
 */
public class BarcodeParam {
    /** Image cropped to the region where the barcode should be
     */
    RgbImage imageCropped;
    /** Image to display 
     */
    Image imageDisplay = null;
    /** Input image.
     */
    Image imageInput;
    /** 
     * Coordinates of barcode rectange in original cropped image.
     */
    int nTL, nTR, nBL, nBR;
    /** Whether or not barcode detection has been attempted.
     */
    boolean oAttempted = false;
    /** Where or not barcode was found
     */
    boolean oFound = false;
    /** Whether barcode detection was successful or not.
     */
    boolean oSuccessful = false;
    
    ReadBarcode rb = null;
    /** The detected barcode (if any).
     */
    String szBarcode;
    /** Creates a new instance of BarcodeParam
     */
    public BarcodeParam() {
        this.imageCropped = null;
    }
    
    public boolean getAttempted() {
        return this.oAttempted;
    }
    
    public String getBarcode() {
        return this.szBarcode;
    }
    
    public boolean getSuccessful() {
        return this.oSuccessful;
    }
    
    public void paint(Graphics g) {
        if (this.imageCropped != null) {
            if (this.oAttempted) {
                try {
                    if (this.imageDisplay == null && this.oFound) {
                        // reduce output image in size so it fits display
                        if (this.imageCropped.getWidth() > g.getClipWidth() ||
                            this.imageCropped.getHeight() > g.getClipHeight()) {
                            int nWidth = Math.min(this.imageCropped.getWidth(), 
                                    g.getClipWidth());
                            int nHeight = Math.min(this.imageCropped.getHeight(), 
                                    g.getClipHeight());
                            RgbShrink rs = new RgbShrink(nWidth, nHeight);
                            rs.push(this.imageCropped);
                            this.imageDisplay = RgbImageJ2me.toImage((RgbImage)rs.getFront());
                        } else {
                            // not necessary to reduce size
                            this.imageDisplay = RgbImageJ2me.toImage(this.imageCropped);
                            g.setColor(0, 0, 0);
                        }
                    } 
                    if (this.oSuccessful) {
                        g.drawImage(
                                this.imageDisplay, 
                                g.getClipX(), 
                                g.getClipY(), 
                                Graphics.TOP | Graphics.LEFT);
                        // draw approximate barcode rectangle
                        // calculate coordinates scaled according to displayed
                        // image
                        int nTL = this.nTL * this.imageDisplay.getWidth()
                            / this.imageCropped.getWidth();
                        int nTR = this.nTR * this.imageDisplay.getWidth()
                            / this.imageCropped.getWidth();
                        int nBL = this.nBL * this.imageDisplay.getWidth() 
                            / this.imageCropped.getWidth();
                        int nBR = this.nBR * this.imageDisplay.getWidth() 
                            / this.imageCropped.getWidth();
                        g.setColor(0, 255, 0); // Green
                        g.drawLine(
                                nTL + g.getClipX(), 
                                g.getClipY(), 
                                nBL + g.getClipX(), 
                                g.getClipY() + g.getClipHeight());
                        g.drawLine(
                                nTR + g.getClipX(), 
                                g.getClipY(), 
                                nBR + g.getClipX(), 
                                g.getClipY() + g.getClipHeight());
                        // draw the barcode string
                        // white background
                        int nHeight = g.getFont().getHeight();
                        g.setColor(255, 255, 255); // white
                        g.fillRect(
                                g.getClipX(), 
                                g.getClipY() + g.getClipHeight() - nHeight, 
                                g.getClipX() + g.getClipWidth(), 
                                g.getClipY() + g.getClipHeight());
                        // black text
                        g.setColor(0, 0, 0);
                        g.drawString(
                                " Read: " + rb.getCode(), 
                                g.getClipX(), 
                                g.getClipY() + g.getClipHeight(), 
                                Graphics.BOTTOM | Graphics.LEFT);
                    }
                } catch (jjil.core.Error e) {
                    e.printStackTrace();
                    jjil.j2me.Error eJ2me = new jjil.j2me.Error(e);
                    g.drawString(eJ2me.getLocalizedMessage(), 0, 0, 0);
                }
            } else {
            }
        }
    }
    
    public void push() {
        this.oAttempted = true;
        this.imageDisplay = null;
        this.imageCropped = RgbImageJ2me.toRgbImage(this.imageInput);
        DetectBarcode db = new DetectBarcode(20000);
        try {
            if (!db.push(this.imageCropped)) {
                /**
                 * Couldn't find the barcode. Tell the user.
                 */
                this.oFound = false;
                this.oSuccessful = false;
                this.szBarcode = null;
            } else {
                                this.oFound = true;
                this.rb = new ReadBarcode();
                this.rb.setRect(db.getRect());
                this.rb.push(this.imageCropped);
                if (!rb.getSuccessful()) {
                        /**
                         * Couldn't read the barcode.
                         */
                } else {
                    /**
                     * Read the barcode. Tell the user.
                     */
                    // calculate coordinates of barcode rectange in terms of
                    // original cropped image
                    this.nTL = db.getRect().getLeft() + this.rb.getLeftApproxPos();
                    this.nTR = db.getRect().getLeft() + this.rb.getRightApproxPos();
                    this.nBL = this.nTL + 
                            this.rb.getLeftApproxSlope() * this.imageCropped.getHeight() / 256;
                    this.nBR = this.nTR + 
                            this.rb.getRightApproxSlope() * this.imageCropped.getHeight() / 256;
                    this.oSuccessful = false;
                    this.szBarcode = null;
                    this.oSuccessful = true;
                    this.szBarcode = this.rb.getCode();
                }
            }
        } catch (jjil.core.Error e) {
            e.printStackTrace();
            jjil.j2me.Error eJ2me = new jjil.j2me.Error(e);
            /**
             * Report error somehow.
             */
            this.oSuccessful = false;
            this.szBarcode = null;
        }
     }
    
    public void reset() {
        this.imageCropped = null;
        this.imageDisplay = null;
        this.imageInput = null;
        this.oAttempted = false;
        this.oSuccessful = false;
    }
    
    public void setImage(Image imageInput) {
        this.oAttempted = false;
        this.imageInput = imageInput;       
    }
    
}

