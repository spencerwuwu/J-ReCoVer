// https://searchcode.com/api/result/2132967/

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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.FontFormatException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.pulpcore.tools.png.PNGWriter;

public class ConvertFontTask extends AbstractMojo {
    
    private static final long MAGIC = 0x70756c70666e740bL; // "pulpfnt" 0.11
    
    // For fonts with kerning pairs (work in progress)
    // Also need advance info?
    //private static final long MAGIC_0_12 = 0x70756c70666e740cL; // "pulpfnt" 0.12
    
    private File srcFile;
    private File destFile;
    private int optimizationLevel = PNGWriter.DEFAULT_OPTIMIZATION_LEVEL;
    
    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }
    
    public void setDestFile(File destFile) {
        this.destFile = destFile;
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
            create();
        }
        catch (FontFormatException ex) {
            throw new MojoExecutionException("Error creating font " + srcFile, ex);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Error creating font " + srcFile, ex);
        }
    }
    
    //
    //
    //
    
    private String chars;
    
    private String[] fontNames;
    private float fontSize;
    private int fontStyle;
    private int fontTracking;
    
    private int[] fontBearingLeft;
    private int[] fontBearingRight;
    private boolean hasBearing;
    
    private Paint backgroundPaint;
    private Paint fillPaint;
    private Paint strokePaint;
    private Stroke stroke;
    
    private float shadowX;
    private float shadowY;
    private Color shadowPaint;
    private Filter shadowBlur;
    
    private int numColors;
    
    private boolean monospace;
    private boolean monospaceNumerals;
    
    private boolean antiAlias;
    
    private final boolean useGlyphBearingInfo = true;
    
    // Kerning: Java 6 only (work in progress)
    private final boolean useKerningInfo = true;
    
    private FontRenderContext context;
    
    private void create() throws IOException, FontFormatException, MojoExecutionException {
        try {
            loadProperties(srcFile);
        }
        catch (NumberFormatException ex) {
            getLog().warn(ex);
            return;
        }
        
        createFont();
    }
    
    private void loadProperties(File file) throws IOException, NumberFormatException, MojoExecutionException {
        CoreProperties props = new CoreProperties();
        props.load(new FileInputStream(file));
        
        fontBearingLeft = new int[Character.MAX_VALUE];
        fontBearingRight = new int[Character.MAX_VALUE];
        hasBearing = false;
        
        List<Character> charList = new ArrayList<Character>();
        String[] charsets = props.getListProperty("chars");
        if (charsets == null || charsets.length == 0 || 
            charsets[0] == null || charsets[0].length() == 0)
        {
            // Default: basic Latin
            for (char ch = ' '; ch <= '~' ; ch++) {
                charList.add(ch);
            }
        }
        else {
            for (String charset : charsets) {
                if (charset == null || charset.length() == 0) {
                    charList.add(' ');
                }
                else if (charset.length() == 1) {
                    charList.add(charset.charAt(0));
                }
                else if (charset.length() == 3 && charset.charAt(1) == '-') {
                    char firstChar = charset.charAt(0);
                    char lastChar = charset.charAt(2);
                    for (char ch = firstChar; ch <= lastChar ; ch++) {
                        charList.add(ch);
                    }
                }
                else {
                    throw new MojoExecutionException("Unsupported charset description: " + charset);
                }
            }
        }
        
        // Sort the chars and remove duplicates
        Collections.sort(charList);
        chars = Character.toString(charList.get(0));
        for (int i = 1; i < charList.size(); i++) {
            if (charList.get(i) != chars.charAt(chars.length() - 1)) {
                chars += charList.get(i);
            }
        }
        
        fontNames = props.getListProperty("family", "dialog");
        fontSize = props.getFloatProperty("size", 16);
        fontTracking = props.getIntProperty("tracking", 0);
        fontStyle = Font.PLAIN;
        String styleString = props.getProperty("style", "plain").toLowerCase();
        if (styleString.contains("bold")) {
            fontStyle |= Font.BOLD;
        }
        
        if (styleString.contains("italic")) {
            fontStyle |= Font.ITALIC;
        }
        
        // Find any bearing
        for (int i = 0; i < chars.length(); i++) {
            char ch = chars.charAt(i);
            int bearingLeft = 0;
            int bearingRight = 0;
            if (props.hasProperty("kerning.left." + ch)) {
                getLog().warn("The kerning.left.* property is deprecated. Use bearing.left.*");
                bearingLeft = props.getIntProperty("kerning.left." + ch, 0);
            }
            if (props.hasProperty("bearing.left." + ch)) {
                bearingLeft = props.getIntProperty("bearing.left." + ch, 0);
            }
            
            if (props.hasProperty("kerning.right." + ch)) {
                getLog().warn("The kerning.right.* property is deprecated. Use bearing.right.*");
                bearingRight = props.getIntProperty("kerning.right." + ch, 0);
            }
            if (props.hasProperty("bearing.right." + ch)) {
                bearingRight = props.getIntProperty("bearing.right." + ch, 0);
            }
            
            if (bearingLeft != 0) {
                hasBearing = true;
                fontBearingLeft[ch] = bearingLeft;
            }
            
            if (bearingRight != 0) {
                hasBearing = true;
                fontBearingRight[ch] = bearingRight;
            }
        }
        
        backgroundPaint = props.getColorProperty("background.color", null);
        strokePaint = props.getColorProperty("stroke.color", null);
        Color fillColor1 = props.getColorProperty("color", Color.BLACK);
        Color fillColor2 = props.getColorProperty("gradient.color", null);
        
        fillPaint = fillColor1;
        if (fillColor2 != null) {
            float yOffset = props.getFloatProperty("gradient.offset", 0);
            fillPaint = new GradientPaint(
                0, -fontSize + yOffset,
                fillColor1,
                0, yOffset,
                fillColor2);
        }
        
        float strokeSize = props.getFloatProperty("stroke.size", 0);
        if (strokeSize > 0) {
            stroke = new BasicStroke(strokeSize);
        }
        else {
            stroke = null;
        }
        
        shadowX = props.getFloatProperty("shadow.x", 0);
        shadowY = props.getFloatProperty("shadow.y", 0);
        shadowPaint = props.getColorProperty("shadow.color", null);
        if (shadowPaint != null) {
            shadowBlur = new BlurFilter(props.getIntProperty("shadow.blur", 0),
                shadowPaint.getRGB());
        }
        
        numColors = props.getIntProperty("colors", 0);
        antiAlias = props.getProperty("antialias", "true").equals("true");
        monospace = props.getProperty("monospace", "false").equals("true");
        monospaceNumerals = props.getProperty("monospace.numerals", "false").equals("true");
        
        context = new FontRenderContext(new AffineTransform(), antiAlias, antiAlias);
        
        Iterator<String> i = props.getUnrequestedKeys();
        while (i.hasNext()) {
            throw new MojoExecutionException(file +
                " has unknown property \"" + i.next() + "\"");
        }
    }
    
    private void createFont() throws IOException, FontFormatException, MojoExecutionException {
        
        long startTime = System.nanoTime();
        
        // Create the AWT Font 
        String[] a =  
            GraphicsEnvironment.getLocalGraphicsEnvironment().
            getAvailableFontFamilyNames();
            
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i].toLowerCase();
        }
        List <String> available = Arrays.asList(a);
        
        Font font = new Font("Dialog", Font.PLAIN, 1);
        for (int i = 0; i < fontNames.length; i++) {
            String fontName = fontNames[i].trim();
            try {
                if (fontName.toLowerCase().endsWith(".ttf")) { 
                    font = Font.createFont(Font.TRUETYPE_FONT, new File(srcFile.getParentFile(), fontName));
                    break;
                }
                else if (available.contains(fontName.toLowerCase())) {
                    font = new Font(fontName, Font.PLAIN, 1);
                    break;
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        font = font.deriveFont(fontStyle, fontSize);
        
        // Determine the legal chars using this font
        StringBuffer legalChars = new StringBuffer(chars);
        for (int i = 0; i < legalChars.length(); i++) {
            char ch = legalChars.charAt(i);
            if (!font.canDisplay(ch)) {
                legalChars.deleteCharAt(i);
                i--;
            }
        }
        if (legalChars.length() == 0) {
            throw new IOException("No legal characters using font " + font.getFamily());
        }
        
        // Get the kerning info (work in progress)
        //if (useKerningInfo) {
        //    List<Kerning> pairs = getKerningPairs(font, legalChars.toString().toCharArray());
        //    for (Kerning pair : pairs) {
        //        log(pair.toString());
        //    }
        //}
        
        // Create the raster font image
        char firstChar = legalChars.charAt(0);
        char lastChar = legalChars.charAt(legalChars.length() - 1);
        int[] widths = new int[lastChar - firstChar + 1];
        BufferedImage image = renderFont(font, legalChars.toString(), widths);
        
        // Reduce the colors of the image
        if (numColors > 0) {
            int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            
            QuantizeARGB quantize = new QuantizeARGB(6);
            quantize.reduce(data, numColors);
        }
        
        // Write the font info
        ByteArrayOutputStream fontData = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(fontData);
        out.writeLong(MAGIC);
        out.writeChar(firstChar);
        out.writeChar(lastChar);
        out.writeShort((short)fontTracking);
        out.writeBoolean(hasBearing);
        int position = 0;
        for (int i = 0; i < widths.length; i++) {
            out.writeShort((short)position);
            position+=widths[i];
        }
        out.writeShort((short)position);
        if (hasBearing) {
            for (int i = firstChar; i <= lastChar; i++) {
                out.writeShort(fontBearingLeft[i]);
                out.writeShort(fontBearingRight[i]);
            }
        }
        out.close();
        
        // Save the image
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
        PNGWriter writer = new PNGWriter();
        writer.setOptimizationLevel(optimizationLevel);
        String imageDescription = writer.write(image, 0, 0, null, fontData.toByteArray(), out);
        out.close();
        
        // Log diagnostic info
        //long time = (System.nanoTime() - startTime) / 1000000;
        //log("Font PNG encoding: " + time + "ms", Project.MSG_INFO);
        String logDescription = destFile + " (" + font.getFamily() + " font, " + imageDescription + ")";
        getLog().info("Created: " + logDescription);
    }
    
    private BufferedImage renderFont(Font font, String legalChars, int[] widths) {
        
        char firstChar = legalChars.charAt(0);
        char lastChar = legalChars.charAt(legalChars.length() - 1);
        int maxWidth = 0;
        int minY = Integer.MAX_VALUE;
        int maxY = -1; 
        
        //long startTime = System.nanoTime();
        
        // Create char images
        BufferedImage[] charImages = new BufferedImage[legalChars.length()];
        for (int i = 0; i < legalChars.length(); i++) {
            char ch = legalChars.charAt(i);
            charImages[i] = drawChar(font, ch, fillPaint,
                    stroke, strokePaint, shadowPaint, shadowX, shadowY, shadowBlur);
        }
        
        //long time = (System.nanoTime() - startTime) / 1000000;
        //log("Font char images: " + time + "ms", Project.MSG_INFO);
        //startTime = System.nanoTime();
        
        // Find the character bounds
        Rectangle[] bounds = new Rectangle[legalChars.length()];
        for (int i = 0; i < legalChars.length(); i++) {
            char ch = legalChars.charAt(i);
            bounds[i] = calcBounds(charImages[i]);
            int w = bounds[i].width;
            int h = bounds[i].height;
            
            if (w > 0 && h > 0) {
                widths[ch - firstChar] = w;
                maxWidth = Math.max(maxWidth, w);
                minY = Math.min(minY, bounds[i].y);
                maxY = Math.max(maxY, bounds[i].y + bounds[i].height - 1);
            }
            else {
                // A space character - calculate later
                bounds[i] = null;
                charImages[i] = null;
            }
        }
        
        //time = (System.nanoTime() - startTime) / 1000000;
        //log("Font character bounds: " + time + "ms", Project.MSG_INFO);
        
        // Calculate width of monospace numerals 0 - 9
        int maxNumeralWidth = 0;
        if (!monospace && monospaceNumerals) {
            for (int ch = '0'; ch <= '9'; ch++) {
                if (legalChars.indexOf(ch) != -1) {
                    int w = widths[ch - firstChar];
                    maxNumeralWidth = Math.max(maxNumeralWidth, w);
                }
            }
        }
        
        // Calculate width of the whitespace characters
        for (int i = 0; i < legalChars.length(); i++) {
            if (bounds[i] == null) {
                char ch = legalChars.charAt(i);
                if (monospace) {
                    widths[ch - firstChar] = maxWidth;
                }
                else {
                    GlyphMetrics metrics = getMetrics(font, ch);
                    widths[ch - firstChar] = Math.round(metrics.getAdvance());
                }
            }
        }
        
        // Calculate final image dimensions
        int width = 0;
        int height = maxY - minY + 1;
        for (int i = 0; i < widths.length; i++) {
            char ch = (char)(i + firstChar);
            boolean isNumeral = (ch >= '0' && ch <= '9');
            
            if (monospace && widths[i] != 0) {
                width += maxWidth;
            }
            else if (monospaceNumerals && isNumeral && widths[i] != 0) {
                width += maxNumeralWidth;
            }
            else {
                width += widths[i];
            }
        }
        
        // Draw the final image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        Graphics2D g = image.createGraphics();
        setHighQuality(g);
        if (backgroundPaint != null) {
            g.setPaint(backgroundPaint);
            g.fillRect(0, 0, width, height);
        }
        
        for (int i = 0; i < legalChars.length(); i++) {
            char ch = legalChars.charAt(i);
            int xOffset = 0;
            boolean isNumeral = (ch >= '0' && ch <= '9');
            
            if (monospace) {
                xOffset += (maxWidth - widths[ch - firstChar]) / 2; 
                widths[ch - firstChar] = maxWidth;
            }
            else if (monospaceNumerals && isNumeral) {
                xOffset += (maxNumeralWidth - widths[ch - firstChar] + 1) / 2; 
                widths[ch - firstChar] = maxNumeralWidth;
                fontBearingLeft[ch] = 0;
                fontBearingRight[ch] = 0;
            }
            
            if (bounds[i] != null) {
                xOffset += x - bounds[i].x;
                
                if (useGlyphBearingInfo && !monospace && !(monospaceNumerals && isNumeral)) {
                    getBearing(font, ch, widths[ch - firstChar]);
                }

                g.drawImage(charImages[i], xOffset, -minY, null);
            }
            x += widths[ch - firstChar];
        }

        return image;
    }
    
    private void getBearing(Font font, char ch, int imageWidth) {
        // See http://developer.apple.com/documentation/Carbon/Conceptual/ATSUI_Concepts/art/glyphterms.gif
        // w = advance - lsb - rsb;
        GlyphMetrics metrics = getMetrics(font, ch);
        float w = (float)metrics.getBounds2D().getWidth();
        if (w > 0) {
            //System.out.println(ch + ": lsb=" + metrics.getLSB() + " rsb=" + metrics.getRSB());
            int lsb = (int)metrics.getLSB();
            float lsbDiff = metrics.getLSB() - lsb;
            int rsb = Math.round(metrics.getRSB() + lsbDiff);
            
            if (imageWidth - w >= 2) {
                //System.out.println(ch + ": w=" + w + " imageWidth=" + imageWidth);
                // Image is bigger due to strokes and/or shadow.
                // Adjust bearing on each side.
                float diff = imageWidth - w;
                lsb -= (int)Math.floor(diff/2);
                rsb -= (int)Math.ceil(diff/2);
            }
            
            if (lsb != 0 || rsb != 0) {
                hasBearing = true;
                fontBearingLeft[ch] += lsb;
                fontBearingRight[ch] += rsb;
            }
        }
    }
    
    private Rectangle calcBounds(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getWidth();
        int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        int boundX1 = image.getWidth() + 1; 
        int boundX2 = -1;
        int boundY1 = image.getHeight() + 1;
        int boundY2 = -1;
        
        int backgroundColor = 0;
        
        int offset = 0;
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                if (data[offset] != backgroundColor) {
                    boundX1 = Math.min(boundX1, i);
                    boundX2 = Math.max(boundX2, i);
                    boundY1 = Math.min(boundY1, j);
                    boundY2 = Math.max(boundY2, j);
                }
                offset++;
            }
        }
        
        return new Rectangle(boundX1, boundY1, boundX2 - boundX1 + 1, boundY2 - boundY1 + 1);
    }
    
    private BufferedImage drawChar(Font font, char ch,
        Paint fillPaint, Stroke stroke, Paint strokePaint,
        Paint shadowPaint, float shadowX, float shadowY, Filter shadowBlur)
    {
        // Get the bounds (make plently of room for the stroke, anti-aliasing, etc.)
        Rectangle2D bounds = font.getMaxCharBounds(context);
        int width = (int)Math.round(bounds.getWidth() * 3);
        int height = (int)Math.round(bounds.getHeight() * 3);
        
        // Create the shape (baseline in the middle of the image)
        int x = width / 3;
        int y = height / 2;
        Shape shape = font.createGlyphVector(context, Character.toString(ch)).getOutline(0, 0);
        
        // Create the image buffer
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        setHighQuality(g);
        g.translate(x, y);

        // Draw the shadow
        if (antiAlias && shadowPaint != null) {
            g.translate(shadowX, shadowY);
            g.setPaint(shadowPaint);
            g.fill(shape);
            if (stroke != null) {
                g.setStroke(stroke);
                g.draw(shape);
            }
            
            image = shadowBlur.filter(image);
            g = image.createGraphics();
            setHighQuality(g);
            g.translate(x, y);
        }
        
        // Draw the character
        if (antiAlias) {
            g.setPaint(fillPaint);
            g.fill(shape);
            if (stroke != null) {
                g.setPaint(strokePaint);
                g.setStroke(stroke);
                g.draw(shape);
            }
        }
        else {
            g.setPaint(fillPaint);
            g.setFont(font);
            g.drawString(Character.toString(ch), 0, 0);
        }
        
        return image;
    }
    
    private void setHighQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); 
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
            RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING,
            RenderingHints.VALUE_DITHER_DISABLE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);       
        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
       }
       else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_NORMALIZE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
       }
    }
    
    private GlyphMetrics getMetrics(Font font, char ch) {
        return font.createGlyphVector(context, Character.toString(ch)).getGlyphMetrics(0);
    }
    
    //
    // Kerning
    //
    
    private List<Kerning> getKerningPairs(Font font, char[] chars) {
        List<Kerning> pairs = new ArrayList<Kerning>();
        
        // Use reflection because TextAttribute.KERNING is Java 6 only.
        Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
        try {
            TextAttribute name = 
                (TextAttribute)TextAttribute.class.getField("KERNING").get(null);
            Object value = TextAttribute.class.getField("KERNING_ON").get(null);
            attributes.put(name, value);
            //attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        }
        catch (Exception ex) {
            // Assume Java 5: return empty list
            return pairs;
        }
        
        Font kerningFont = font.deriveFont(attributes);
        // Brute-force approach (there is no Java API to get kerning values)
        for (char prev : chars) {
            for (char next : chars) {
                float kerning = getKerning(kerningFont, prev, next);
                if (kerning != 0) {
                    pairs.add(new Kerning(prev, next, kerning));
                }
            }
        }
        return pairs;
    }
    
    /**
        Gets the kerning between two chars. Assumes the font has the KERNING_ON attribute but
        not LIGATURES_ON.
    */
    private float getKerning(Font font, char prev, char next) {
        
        GlyphVector glyphVector = font.layoutGlyphVector(context, new char[] { prev, next },
            0, 2, Font.LAYOUT_LEFT_TO_RIGHT);
        
		float advance = glyphVector.getGlyphMetrics(0).getAdvance();
        double x1 = glyphVector.getGlyphPosition(0).getX();
        double x2 = glyphVector.getGlyphPosition(1).getX();
		float xdif = (float)(x2 - x1);
		float kerning = xdif - advance;
        
		return kerning;
	}
    
    public static class Kerning {
        
        private final char prev;
        private final char next;
        private final float kerning;
        
        public Kerning(char prev, char next, float kerning) {
            this.prev = prev;
            this.next = next;
            this.kerning = kerning;
        }
        
        @Override
        public String toString() {
            return prev + "" + next + " (" + (int)prev + ", " + (int)next + "): " + kerning;
        }
    }
    
    //
    // Filters
    //
    
    public interface Filter {
        /**
            The filter may edit the contents of the BufferedImage
        */
        public BufferedImage filter(BufferedImage in);
    }
    
    public static class NoFilter implements Filter {
        public BufferedImage filter(BufferedImage in) {
            return in;
        }
    }

    /**
        3x3 box filter that blurs the alpha and copies the RGB.
        Nearly 2X faster that using ConvolveOp. 
    */
    public static class BlurFilter implements Filter {
        
        public static final int SIZE = 3;
        
        private final int passes;
        private final int rgb;
        
        public BlurFilter(int passes, int rgb) {
            this.passes = passes;
            this.rgb = 0xffffff & rgb;
        }
        
        public BufferedImage filter(BufferedImage in) {
            if (passes > 0) {
                if (in.getType() != BufferedImage.TYPE_INT_ARGB &&
                    in.getType() != BufferedImage.TYPE_INT_RGB)
                {
                    throw new IllegalArgumentException(
                        "Image type must be TYPE_INT_RGB or TYPE_INT_ARGB");
                }
                
                int imageWidth = in.getWidth();
                int imageHeight = in.getHeight();
                int[] srcData = ((DataBufferInt)in.getRaster().getDataBuffer()).getData();
                int[] temp1 = srcData;
                int[] temp2 = new int[imageWidth * imageHeight];
                
                for (int i = 0; i < passes; i++) {
                    filter(imageWidth, imageHeight, temp1, temp2);
                    int[] t = temp1;
                    temp1 = temp2;
                    temp2 = t;
                }
                
                if (temp1 != srcData) {
                    System.arraycopy(temp1, 0, srcData, 0, srcData.length);
                }
            }
            return in;
        }
        
        protected void filter(int imageWidth, int imageHeight, int[] srcData, int[] destData) {
            int xOffset = SIZE/2;
            int yOffset = SIZE/2;
            int offset;
            int startRowOffset = -yOffset * imageWidth;
            
            for (int y = yOffset; y < imageHeight-yOffset; y++) {
                offset = y * imageWidth + xOffset;
                for (int x = xOffset; x < imageWidth-xOffset; x++) {
                    int suma = 0;
                    //int sumr = 0;
                    //int sumg = 0;
                    //int sumb = 0;
                    int srcOffset = offset + startRowOffset - xOffset;
                    for (int j = 0; j < SIZE; j++) {
                        for (int i = 0; i < SIZE; i++) {
                            int argb = srcData[srcOffset + i];
                            suma += (argb >>> 24);
                            //sumr += ((argb >> 16) & 0xff);
                            //sumg += ((argb >> 8) & 0xff);
                            //sumb += (argb & 0xff);
                        }
                        srcOffset += imageWidth;
                    }
                    
                    // n / 9 = (n * 3641) >> 15
                    suma = (suma * 3641) >> 15;
                    //sumr = (sumr * 3641) >> 15;
                    //sumg = (sumg * 3641) >> 15;
                    //sumb = (sumb * 3641) >> 15;
                    
                    //destData[offset] = (suma << 24) | (sumr << 16) | (sumg << 8) | sumb;
                    destData[offset] = (suma << 24) | rgb;
                    offset++;
                }
            }
        }
    }
}
