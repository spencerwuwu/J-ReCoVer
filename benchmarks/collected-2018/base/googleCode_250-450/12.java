// https://searchcode.com/api/result/2132969/

/*
    Copyright (c) 2007-2010, Interactive Pulp, LLC
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without 
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright 
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright 
          notice, this list of conditions and the following disclaimer in the 
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its 
          contributors may be used to endorse or promote products derived from 
          this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
*/

package org.pulpcore.tools.res;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.pulpcore.tools.png.PNGWriter;

public class ConvertImageTask extends AbstractMojo {
    
    private File srcFile;
    private File srcPropertyFile;
    private File destFile;
    private byte[] fontData;
    private int optimizationLevel = PNGWriter.DEFAULT_OPTIMIZATION_LEVEL;
    
    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }
    
    public void setSrcPropertyFile(File srcPropertyFile) {
        this.srcPropertyFile = srcPropertyFile;
    }
    
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }
    
    public void setFontData(byte[] fontData) {
        this.fontData = fontData;
    }
    
    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    }
    
    @Override
    public void execute() throws MojoExecutionException {
        if (srcFile == null) {
            throw new MojoExecutionException("The srcFile is not specified.");
        }
        if (destFile == null) {
            throw new MojoExecutionException("The destFile is not specified.");
        }
        if (optimizationLevel < 0 || optimizationLevel > PNGWriter.MAX_OPTIMIZATION_LEVEL) {
            throw new MojoExecutionException("Optimization level must be between 0 and " +
                PNGWriter.MAX_OPTIMIZATION_LEVEL + ".");
        }
                
        try {
            convert();
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Error converting image " + srcFile, ex);
        }
    }
    
    private void convert() throws IOException, MojoExecutionException {
        
        long startTime = System.nanoTime();
        
        String filename = srcFile.getName().toLowerCase();
        
        // Load the image 
        BufferedImage image;
        if (filename.endsWith(".svg") || filename.endsWith(".svgz")) {
            image = SVGRasterizer.rasterize(srcFile);
        }
        else {
            image = ImageIO.read(srcFile);
        }

        if (image == null) {
            throw new IOException("Couldn't convert " + srcFile);
        }
        
        // Get the image pixels
        int w = image.getWidth();
        int h = image.getHeight();
        int[] data = new int[w * h];
        image.getRGB(0, 0, w, h, data, 0, w);
        int hotspotX = 0;
        int hotspotY = 0;
        byte[] animData = null;
        
        // Load the optional properties file
        AnimProperties animProperties = AnimProperties.read(this, srcPropertyFile);
        if (animProperties != null) {
            hotspotX = animProperties.hotspotX;
            hotspotY = animProperties.hotspotY;
            animData = animProperties.createData();
            
            // Reduce the colors
            if (animProperties.numColors > 0) {
                
                //QuantizeRGB quantize = new QuantizeRGB(8);
                //quantize.reduce(data, animChunk.numColors);
                
                QuantizeARGB quantize = new QuantizeARGB(6);
                quantize.reduce(data, animProperties.numColors);
            }
        }
        
        // Write
        DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(
            new FileOutputStream(destFile)));
        PNGWriter writer = new PNGWriter();
        writer.setOptimizationLevel(optimizationLevel);
        String imageDescription = writer.write(w, h, data, hotspotX, hotspotY,
            animData, fontData, out);
            
        out.close();
        
        // Log diagnostic info
        long time = (System.nanoTime() - startTime) / 1000000;
        
        //String logDescription = srcFile + " (" + time + "ms): " + imageDescription;
        String logDescription = destFile + " (" + imageDescription;
        
        if (animProperties != null) {
            logDescription += ", " + animProperties;
        }
        
        logDescription += ")";
        
        getLog().info("Created: " + logDescription);
    }
    
    public static class AnimProperties {
        
        boolean hasAnimation;
        
        int numColors;
        
        int hotspotX;
        int hotspotY;
        
        int numFramesAcross;
        int numFramesDown;
        int[] frameSequence;
        int[] frameDuration;
        boolean loop;
        
        public static AnimProperties read(Mojo mojo, File propFile) throws IOException, MojoExecutionException {
            
            if (propFile == null || !propFile.exists()) {
                return null;
            }
            
            AnimProperties data = new AnimProperties();
            
            CoreProperties props = new CoreProperties();
            props.load(new FileInputStream(propFile));
            
            try {
                data.numColors = props.getIntProperty("colors", 0);
                data.hotspotX = props.getIntProperty("hotspot.x", 0);
                data.hotspotY = props.getIntProperty("hotspot.y", 0);
                
                data.numFramesAcross = props.getIntProperty("frames.across", 1);
                data.numFramesDown = props.getIntProperty("frames.down", 1);
                
                if (data.numFramesAcross > 1 || data.numFramesDown > 1) {
                    data.hasAnimation = true;
                    
                    data.loop = props.getProperty("loop", "false").equals("true");
                    
                    data.frameSequence = props.getIntListProperty("frame.sequence");
                    data.frameDuration = props.getIntListProperty("frame.duration");
                }
                
                Iterator<String> i = props.getUnrequestedKeys();
                while (i.hasNext()) {
                    throw new MojoExecutionException(propFile +
                        " has unknown property \"" + i.next() + "\"");
                }
            }
            catch (NumberFormatException ex) {
                mojo.getLog().warn(ex.getMessage());
                return null;
            }
            
            return data;
        }
        
        public byte[] createData() {
            
            if (!hasAnimation) {
                return null;
            }
            
            if (frameSequence == null) {
                frameSequence = new int[
                    numFramesAcross * numFramesDown];
                for (int i = 0; i < frameSequence.length; i++) {
                    frameSequence[i] = i;
                }
            }
            
            if (frameDuration == null) {
                frameDuration = new int[frameSequence.length];
                for (int i = 0; i < frameDuration.length; i++) {
                    frameDuration[i] = 100;
                }
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            try {
                out.writeShort(numFramesAcross);
                out.writeShort(numFramesDown);
                out.writeByte(loop?1:0);
                out.writeShort(frameSequence.length);
                for (int i = 0; i < frameSequence.length; i++) {
                    out.writeShort(frameSequence[i]);
                }
                
                for (int i = 0; i < frameSequence.length; i++) {
                    int index = Math.min(i, frameDuration.length - 1);
                    out.writeShort(frameDuration[index]);
                }
            }
            catch (IOException ex) {
                // Won't happen with a ByteArrayOutputStream
            }
            
            return bos.toByteArray();
        }
        
        private String toString(int[] list) {
            if (list == null) {
                return null;
            }
            
            String retVal = "";
            for (int i = 0; i < list.length; i++) {
                retVal += list[i];
                if (i != list.length - 1) {
                    retVal += ", ";
                }
            }
            
            return retVal;
        }
        
        @Override
        public String toString() {
            
            String retVal = "";
            
            if (hotspotX != 0 || hotspotY != 0) {
                retVal = "hotspot=(" + hotspotX + ", " + hotspotY + ")";
                if (hasAnimation) {
                    retVal += ", ";
                }
            }
            
            if (hasAnimation) {
                retVal += 
                    "frames=" + numFramesAcross + "x" + numFramesDown + 
                    ", sequence={" + toString(frameSequence) + "}" +
                    ", duration={" + toString(frameDuration) + "}" +
                    ", loop=" + (loop?"true":"false");
            }
            
            return retVal;
        }
    }
}
