// https://searchcode.com/api/result/2132965/

package org.pulpcore.tools.res;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
    I don't know where I got this code from or how much I previously edited it (1999).
    
    This appears to be the Popularity algorithm.
    
    On 1/20/2005:
     - Added reduce() methods
     - Changed from Vector to List
*/
class QuantizeRGB {
    
    
    private static class RGBColor {
        int red, green, blue;
        int dist;
        
        public RGBColor(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    
    private class Division {
        public BitSet bitSet;
        public int changeRed, changeGreen, changeBlue;
        public int numPixels;
        public int numColors;

        public int numColorsToPick;

        public Division() {
            int divisionSize = QuantizeRGB.this.divisionSize;
            bitSet = new BitSet(divisionSize * divisionSize * divisionSize);
            numPixels = 0;
            numColors = 0;
            changeRed = 0;
            changeBlue = 0;
            changeGreen = 0;
            numColorsToPick = -1; // -1 means "all"
        }

        //returns true if the color is new
        public boolean addColor(int red, int green, int blue) {
            int offset = getOffset(red, green, blue);
            boolean newColor = !bitSet.get(offset);


            bitSet.set(offset);

            changeRed += red;
            changeGreen += green;
            changeBlue += blue;

            numPixels++;

            if (newColor) {
                numColors++;
            }

            return newColor;
        }

        public boolean colorExists(int red, int green, int blue) {
            return bitSet.get(getOffset(red, green, blue));
        }

        private int getOffset(int red, int green, int blue) {
            int divisionSizeBits = QuantizeRGB.this.divisionSizeBits;

            return (red << (divisionSizeBits << 1)) |
                   (green << divisionSizeBits) |
                   blue;
        }
    }

    private static final int BITS_PER_CHANNEL = 8; // currently up to 8 is supported
    private static final int CHANNEL_SIZE = 1 << BITS_PER_CHANNEL;
    private static final int CHANNEL_SIZE_MASK = CHANNEL_SIZE - 1;

    private int numDivisionsBits;
    private int numDivisions;
    private int divisionSize;
    private int divisionSizeBits;
    private int divisionSizeMask;

    
    public QuantizeRGB() {
        this(BITS_PER_CHANNEL >> 1);
    }

    
    public QuantizeRGB(int numDivisionsBits) {
        if (numDivisionsBits < 1 || numDivisionsBits > BITS_PER_CHANNEL) {
            throw new IndexOutOfBoundsException("Division: " + numDivisionsBits);
        }

        this.numDivisionsBits = numDivisionsBits;
        numDivisions = 1 << numDivisionsBits;
        divisionSizeBits = BITS_PER_CHANNEL - numDivisionsBits;
        divisionSize = 1 << divisionSizeBits;
        divisionSizeMask = divisionSize - 1;
    }
    

    public int[] createPalette(int[] pixels, int maxNumColors) {
        //Division[] table = new Division[numDivisions * numDivisions * numDivisions];
        Map<Integer, Division> table = new HashMap<Integer, Division>();
        int numColors = 0;
        int numUsedDivisions = 0;

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int red = (pixel >> (BITS_PER_CHANNEL << 1)) & CHANNEL_SIZE_MASK;
            int green = (pixel >> BITS_PER_CHANNEL) & CHANNEL_SIZE_MASK;
            int blue = pixel & CHANNEL_SIZE_MASK;
            int redDivision = red >> divisionSizeBits;
            int greenDivision = green >> divisionSizeBits;
            int blueDivision = blue >> divisionSizeBits;
            int redOffset = red & divisionSizeMask;
            int greenOffset = green & divisionSizeMask;
            int blueOffset = blue & divisionSizeMask;
            int tableOffset = (redDivision << (numDivisionsBits << 1)) |
                              (greenDivision << numDivisionsBits) |
                              blueDivision;
                              
            Division div = table.get(tableOffset);

            if (div == null) {
                div = new Division();
                table.put(tableOffset, div);
                numUsedDivisions++;
            }

            if (div.addColor(redOffset, greenOffset, blueOffset)) {
                numColors++;
            }

        }

        int[] palette;

        if (numColors <= maxNumColors) {
            palette = new int[numColors];
            getColors(table, palette);
            return palette;
        }
        else {

            // sort available divisions by numPixels/numColors ratio, from largest to smallest
            float totalRatio = 0;
            List<Division> sortedTable = new ArrayList<Division>(numUsedDivisions);

            for (Division div : table.values()) {

                if (div != null) {
                    float sortValue = (float)div.numPixels / (float)div.numColors;
                    totalRatio += sortValue;
                    boolean done = false;

                    for (int j = 0; j < sortedTable.size() && !done; j++) {
                        Division d = sortedTable.get(j);

                        if (sortValue > (float)d.numPixels / (float)d.numColors) {
                            sortedTable.add(j, div);
                            done = true;
                        }
                    }

                    if (!done) {
                        sortedTable.add(div);
                    }
                }
            }

            // figure out how many colors each division will have
            int numColorsToPick = maxNumColors;

            for (int i = 0; i < sortedTable.size(); i++) {
                Division div = sortedTable.get(i);

                if (numColorsToPick > 0) {
                    float sortValue = (float)div.numPixels / (float)div.numColors;
                    div.numColorsToPick = Math.round(sortValue * numColorsToPick / totalRatio);

                    if (div.numColorsToPick > div.numColors) {
                        div.numColorsToPick = div.numColors;
                    }

                    if (div.numColorsToPick > numColorsToPick) {
                        div.numColorsToPick = numColorsToPick;
                    }

                    numColorsToPick -= div.numColorsToPick;
                    totalRatio -= sortValue;
                }
                else {
                    div.numColorsToPick = 0;
                }
            }

            if (numColorsToPick > 0) {
                System.out.println("Wanted to pick " + maxNumColors +
                                   " colors, but only picked " + (maxNumColors - numColorsToPick));
            }

            palette = new int[maxNumColors];
            getColors(table, palette);
            return palette;
        }

        
    }

    
    private void getColors(Map<Integer, Division> table, int[] palette) {
        int paletteOffset = 0;
        int offset = 0;

        for (int red = 0; red < CHANNEL_SIZE; red += divisionSize) {
            
            for (int green = 0; green < CHANNEL_SIZE; green += divisionSize) {
                
                for (int blue = 0; blue < CHANNEL_SIZE; blue += divisionSize) {
                    
                    Division div = table.get(offset);

                    if (div != null && div.numColors > 0) {
                        
                        if (div.numColorsToPick == -1 ||
                                div.numColorsToPick == div.numColors) 
                        {
                            for (int redOffset = 0; redOffset < divisionSize; redOffset++) {
                                for (int greenOffset = 0; greenOffset < divisionSize; greenOffset++) {
                                    for (int blueOffset = 0; blueOffset < divisionSize; blueOffset++) {
                                        if (div.colorExists(redOffset, greenOffset, blueOffset)) {
                                            palette[paletteOffset++] =
                                                (((red + redOffset) & CHANNEL_SIZE_MASK) << (BITS_PER_CHANNEL << 1)) |
                                                (((green + greenOffset) & CHANNEL_SIZE_MASK) << BITS_PER_CHANNEL) |
                                                (((blue + blueOffset) & CHANNEL_SIZE_MASK));
                                        }
                                    }
                                }
                            }
                        }

                        /*else if (div.numColorsToPick == 1)
                        {
                              //pick the median color
                              int newRed   = red   + Math.round((float)div.changeRed/div.numPixels);
                              int newGreen = green + Math.round((float)div.changeGreen/div.numPixels);
                              int newBlue  = blue  + Math.round((float)div.changeBlue/div.numPixels);
                              // make sure it's between 0 and 255
                              newRed   = Math.max(0,Math.min(newRed,255));
                              newGreen = Math.max(0,Math.min(newGreen,255));
                              newBlue  = Math.max(0,Math.min(newBlue,255));
                              //System.out.println("Midpoint: (" +
                              //  red + "," + green + "," + blue + ")");
                              //System.out.println("  Median: (" +
                              //  newRed + "," + newGreen + "," + newBlue + ")");
                              palette[paletteOffset++] = (byte)newRed;
                              palette[paletteOffset++] = (byte)newGreen;
                              palette[paletteOffset++] = (byte)newBlue;
                        }*/
                        else if (div.numColorsToPick > 0) {
                            // for now, pick the colors closest to the median color
                            int redMedian = div.changeRed / div.numPixels;
                            int greenMedian = div.changeGreen / div.numPixels;
                            int blueMedian = div.changeBlue / div.numPixels;
                            List<RGBColor> sortedColors = new ArrayList<RGBColor>(div.numColorsToPick);

                            for (int redOffset = 0; redOffset < divisionSize; redOffset++) {
                                for (int greenOffset = 0; greenOffset < divisionSize; greenOffset++) {
                                    for (int blueOffset = 0; blueOffset < divisionSize; blueOffset++) {
                                        if (div.colorExists(redOffset, greenOffset, blueOffset)) {
                                            RGBColor thisColor = new RGBColor(redOffset, greenOffset, blueOffset);
                                            thisColor.dist = sqr(redOffset - redMedian) +
                                                             sqr(greenOffset - greenMedian) +
                                                             sqr(blueOffset - blueMedian);
                                            boolean done = false;

                                            for (int i = 0; i < sortedColors.size() && !done; i++) {
                                                RGBColor compareColor = sortedColors.get(i);

                                                if (thisColor.dist < compareColor.dist) {
                                                    sortedColors.add(i, thisColor);
                                                    done = true;
                                                }
                                            }

                                            if (!done) {
                                                sortedColors.add(thisColor);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int i = 0; i < div.numColorsToPick; i++) {
                                RGBColor color = sortedColors.get(i);
                                palette[paletteOffset++] =
                                    (Math.max(0, Math.min(red + color.red, 255)) << (BITS_PER_CHANNEL << 1)) |
                                    (Math.max(0, Math.min(green + color.green, 255)) << BITS_PER_CHANNEL) |
                                    (Math.max(0, Math.min(blue + color.blue, 255)));
                            }
                        }
                    }

                    offset++;
                }
            }
        }
    }

    
    private static int sqr(int value) {
        return value*value;
    }

    
    public byte[] nearestColorMatch(int[] pixels, int[] palette) {
        byte[] indexedPixels = new byte[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            indexedPixels[i] = (byte)nearestColorMatch(pixels[i], palette);
        }

        return indexedPixels;
    }
    
    
    // slow
    private int nearestColorMatch(int pixel, int[] palette) {
        
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        
        int minDist = Integer.MAX_VALUE;
        int minIndex = 0;
        
        for (int i = 0; i < palette.length; i++) {
            int p = palette[i];
            int r2 = (p >> 16) & 0xff;
            int g2 = (p >> 8) & 0xff;
            int b2 = p & 0xff;
            
            int dist = sqr(r2 - r) + sqr(g2 - g) + sqr(b2 - b);
            
            if (dist < minDist) {
                minDist = dist;
                minIndex = i;
            }
        }
        
        return minIndex;
    }
    
    
    public void reduce(int[] pixels, int maxNumColors) {
        reduce(pixels, createPalette(pixels, maxNumColors));
    }
    
    
    public void reduce(int[] pixels, int[] palette) {

        for (int i = 0; i < pixels.length; i++) {
            int originalColor = pixels[i];
            
            int newColor = palette[nearestColorMatch(originalColor, palette)];
            
            // apply alpha from original color
            pixels[i] = (0xff000000 & originalColor) | (newColor & 0x00ffffff);

        }

        
    }
}

