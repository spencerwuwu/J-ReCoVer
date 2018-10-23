// https://searchcode.com/api/result/47294918/

package ub.edu.tmm.huelamosolans.codec;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import tsm.practiques.pTSM;

/**
 * Video encoder.
 * 
 * @author albertohuelamosegura
 */
public class Encoder {

    /**
     * Image input sequence.
     */
    protected List<pTSM> input;
    /**
     * HDS video output sequence.
     */
    protected Video output;

    /**
     * Tessel width.
     */
    protected int tWidth;
    /**
     * Tessel height.
     */
    protected int tHeight;
    /**
     * Base frame index.
     */
    protected int baseCount;

    /**
     * Search displacement distance in pixels.
     */
    protected int displacement;
    /**
     * Search spiral turns number.
     */
    protected int nTurns;
    /**
     * Tolerance to compare two tessels.
     */
    protected double averageTolerance;

    /**
     * Sole constructor.
     * 
     * @param images
     *            Input image sequence.
     * @param tWidth
     *            Tessel width.
     * @param tHeight
     *            Tessel height.
     * @param baseCount
     *            Base frame index.
     * @param displacement
     *            Search displacement distance in pixels.
     * @param nTurns
     *            Search spiral turns number.
     * @param averageTolerance
     *            Tolerance to compare two tessels.
     */
    public Encoder(List<pTSM> images, int tWidth, int tHeight, int baseCount,
            int displacement, int nTurns, double averageTolerance) {
        input = images;
        this.tWidth = tWidth;
        this.tHeight = tHeight;
        this.baseCount = baseCount;
        output = new Video();
        this.displacement = displacement;
        this.nTurns = nTurns;
        this.averageTolerance = averageTolerance;
    }

    /**
     * Gets the input image sequence.
     * 
     * @return The input image sequence.
     */
    public List<pTSM> getInput() {
        return input;
    }

    /**
     * Sets a new input image sequence.
     * 
     * @param input
     *            A new input image sequence.
     */
    public void setInput(List<pTSM> input) {
        this.input = input;
    }

    /**
     * Gets the tessel width.
     * 
     * @return The tessel width.
     */
    public int gettWidth() {
        return tWidth;
    }

    /**
     * Sets the tessel width.
     * 
     * @param tWidth
     *            The tessel width.
     */
    public void settWidth(int tWidth) {
        this.tWidth = tWidth;
    }

    /**
     * Gets the tessel height.
     * 
     * @return The tessel height.
     */
    public int gettHeight() {
        return tHeight;
    }

    /**
     * Sets the tessel height.
     * 
     * @param tHeight
     *            The tessel height.
     */
    public void settHeight(int tHeight) {
        this.tHeight = tHeight;
    }

    /**
     * Gets the base image index.
     * 
     * @return The base image index.
     */
    public int getBaseCount() {
        return baseCount;
    }

    /**
     * Sets the image base index.
     * 
     * @param baseCount
     *            The base image index.
     */
    public void setBaseCount(int baseCount) {
        this.baseCount = baseCount;
    }

    /**
     * Gets the output video.
     * 
     * @return The output video.
     */
    public Video getOutput() {
        return output;
    }

    /**
     * Gets the search displacement.
     * 
     * @return The search displacement.
     */
    public int getDisplacement() {
        return displacement;
    }

    /**
     * Sets the search displacement.
     * 
     * @param displacement
     *            The search displacement.
     */
    public void setDisplacement(int displacement) {
        this.displacement = displacement;
    }

    /**
     * Gets the max number of search spiral turns.
     * 
     * @return The max number of search spiral turns.
     */
    public int getnTurns() {
        return nTurns;
    }

    /**
     * Sets the max number of search spiral turns.
     * 
     * @param nTurns
     *            The max number of search spiral turns.
     */
    public void setnTurns(int nTurns) {
        this.nTurns = nTurns;
    }

    /**
     * Gets the tolerance to compare two tessels.
     * 
     * @return Tolerance to compare two tessels.
     */
    public double getAverageTolerance() {
        return averageTolerance;
    }

    /**
     * Sets the tolerance to compare two tessels.
     * 
     * @param averageTolerance
     *            The tolerance to compare two tessels.
     */
    public void setAverageTolerance(double averageTolerance) {
        this.averageTolerance = averageTolerance;
    }

    /**
     * Search a same tessel in a frame.
     * 
     * @param frame
     *            Frame that contains or not the tessel.
     * @param tessel
     *            Tessel to search.
     * @param nTessel
     *            Number of the tessel.
     * @return MatchVector with the information of the search result. Null if
     *         nothing was found.
     */
    protected MatchVector searchMatches(WritableRaster frame, Tessel tessel,
            int nTessel) {
        // En primer lugar, se compara directamente la tesela con el trozo de
        // marco en el que estaba al principio y se determina si es igual o no.
        MatchVector ret;
        if (match(frame.createWritableChild(tessel.getX(), tessel.getY(),
                tessel.getRaster().getWidth(), tessel.getRaster().getHeight(),
                0, 0, null), tessel.getRaster())) {
            ret = new MatchVector(tessel.getX(), tessel.getY(),
                    tessel.getnTessel());
            return ret;
        }

        // Si no es igual, se inicia una busqueda en espiral desde donde estaba
        // situada la tesela en el otro marco.
        // La busqueda termina cuando se han dado un numero maximo de vueltas o
        // cuando se encuentra una coincidencia.
        int x = tessel.getX();
        int y = tessel.getY();

        for (int i = 1; i <= nTurns; i++) {
            // bajar
            y = tessel.getY();
            y = y + i * displacement;
            if (!(y + tHeight > frame.getHeight())) {
                if (match(
                        frame.createWritableChild(x, y, tessel.getRaster()
                                .getWidth(), tessel.getRaster().getHeight(), 0,
                                0, null), tessel.getRaster())) {
                    ret = new MatchVector(x, y, tessel.getnTessel());
                    return ret;
                }
            } else {
                y = y - i * displacement;
            }
            // derecha
            x = tessel.getX();
            x = x + i * displacement;
            if (!(x + tWidth > frame.getWidth())) {
                if (match(
                        frame.createWritableChild(x, y, tessel.getRaster()
                                .getWidth(), tessel.getRaster().getHeight(), 0,
                                0, null), tessel.getRaster())) {
                    ret = new MatchVector(x, y, tessel.getnTessel());
                    return ret;
                }
            } else {
                x = x - i * displacement;
            }
            // subir
            y = tessel.getY();
            y = y - i * displacement;
            if (y > 0) {
                if (match(
                        frame.createWritableChild(x, y, tessel.getRaster()
                                .getWidth(), tessel.getRaster().getHeight(), 0,
                                0, null), tessel.getRaster())) {
                    ret = new MatchVector(x, y, tessel.getnTessel());
                    return ret;
                }
            } else {
                y = y + i * displacement;
            }
            // izquierda
            x = tessel.getX();
            x = x - i * displacement;
            if (x > 0) {
                if (match(
                        frame.createWritableChild(x, y, tessel.getRaster()
                                .getWidth(), tessel.getRaster().getHeight(), 0,
                                0, null), tessel.getRaster())) {
                    ret = new MatchVector(x, y, tessel.getnTessel());
                    return ret;
                }
            } else {
                x = x + i * displacement;
            }
        }
        return null;
    }

    /**
     * Search a same tessel in a frame.
     * 
     * @param frame
     *            Frame that contains or not the tessel.
     * @param tessel
     *            Tessel to search.
     * @param nTessel
     *            Number of the tessel.
     * @return MatchVector with the information of the search result. Null if
     *         nothing was found.
     */
    protected MatchVector searchMatches(pTSM frame, Tessel tessel, int nTessel) {
        return searchMatches(frame.getBufImg().getRaster(), tessel, nTessel);
    }

    /**
     * Determines if two images are similar.
     * 
     * @param t1
     *            First image raster.
     * @param t2
     *            Second image raster.
     * @return True if are similar, false if not.
     */
    protected boolean match(WritableRaster t1, WritableRaster t2) {
        // Se obtienen los pixels de cada canal de cada tesela y se hace la
        // media
        // del mismo canal para las dos teselas.

        int nBands1 = t1.getNumBands();
        int size1 = t1.getHeight() * t1.getWidth();
        double[] pixels1 = new double[t1.getHeight() * t1.getWidth() * nBands1];
        pixels1 = t1.getPixels(0, 0, t1.getWidth(), t1.getHeight(), pixels1);

        int nBands2 = t2.getNumBands();
        int size2 = t2.getHeight() * t2.getWidth();
        double[] pixels2 = new double[t2.getHeight() * t2.getWidth() * nBands2];
        pixels2 = t2.getPixels(0, 0, t2.getWidth(), t2.getHeight(), pixels2);

        int minBands;
        if (nBands1 <= nBands2) {
            minBands = nBands1;
        } else {
            minBands = nBands2;
        }

        double p1[], p2[];
        for (int i = 0; i < minBands; i++) {
            p1 = new double[t1.getHeight() * t1.getWidth()];
            for (int j = 0; j < size1; j++) {
                p1[j] = pixels1[j * nBands1 + i];
            }
            p2 = new double[t2.getHeight() * t2.getWidth()];
            for (int j = 0; j < size2; j++) {
                p2[j] = pixels2[j * nBands2 + i];
            }
            if (Math.abs(average(p1) - average(p2)) > averageTolerance) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the average value of a set of pixels.
     * 
     * @param pixels
     *            Set of pixels.
     * @return Average value.
     */
    protected double average(double[] pixels) {
        double ret = 0;
        for (int i = 0; i < pixels.length; i++) {
            ret += pixels[i];
        }
        return ret / pixels.length;
    }

    /**
     * Tesselates a frame.
     * 
     * @param raster
     *            Frame raster to tessellate.
     * @return A list of tessels.
     */
    protected List<Tessel> tessellate(WritableRaster raster) {
        List<Tessel> ret = new ArrayList<Tessel>();
        int width = raster.getWidth();
        int height = raster.getHeight();

        int x, y;
        int nT = 0;
        for (y = 0; y < height; y += tHeight) {
            for (x = 0; x < width; x += tWidth) {
                try {
                    ret.add(new Tessel(nT, raster.createWritableChild(x, y,
                            tWidth, tHeight, 0, 0, null), x, y));
                    // Si la tesela se pasa de los limites del marco se reduce
                    // para
                    // abarcar solo hasta los margenes.
                } catch (RasterFormatException ex) {
                    int cWidth = tWidth, cHeight = tHeight;
                    if (x + tWidth > width) {
                        cWidth = width - x;
                    }
                    if (y + tHeight > height) {
                        cHeight = height - y;
                    }
                    ret.add(new Tessel(nT, raster.createWritableChild(x, y,
                            cWidth, cHeight, 0, 0, null), x, y));
                }
                nT++;
            }
        }
        return ret;
    }

    /**
     * Tesselates a frame.
     * 
     * @param frame
     *            Image to tessellate.
     * @return A list of tessels.
     */
    protected List<Tessel> tessellate(pTSM frame) {
        return tessellate(frame.getBufImg().getRaster());
    }

    /**
     * Sets to black a frame portion.
     * 
     * @param frame
     *            Frame to compensate.
     * @param tessel
     *            Frame portion to compensate.
     * @param match
     *            Match vector with information.
     * @return The compensate frame.
     */
    protected BufferedImage compensate(BufferedImage frame, Tessel tessel,
            MatchVector match) {
        BufferedImage ret = frame;
        WritableRaster frameRaster = ret.getRaster();

        double[] pix = new double[frameRaster.getNumBands()];
        for (int i = 0; i < pix.length; i++) {
            pix[i] = 0.0;
        }

        for (int x = match.x; x < match.x + tessel.getRaster().getWidth(); x++) {
            for (int y = match.y; y < match.y + tessel.getRaster().getHeight(); y++) {
                frameRaster.setPixel(x, y, pix);
            }
        }
        ret.setData(frameRaster);
        return ret;
    }

    /**
     * Sets to black a frame portion.
     * 
     * @param frame
     *            Frame to compensate.
     * @param tessel
     *            Frame portion to compensate.
     * @param match
     *            Match vector with information.
     * @return The compensate frame.
     */
    protected BufferedImage compensate(pTSM frame, Tessel tessel,
            MatchVector match) {
        return compensate(frame.getBufImg(), tessel, match);
    }

    /**
     * Determines if an frame is a base frame.
     * 
     * @param n
     *            Frame index.
     * @return True if the index is a base frame index. False if not.
     */
    protected boolean isImageBase(int n) {
        if (n % baseCount == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Engages the encoding process.
     */
    public void encode() {
        pTSM frame, prevFrame;
        Tessel tessel;
        MatchVector match;
        BufferedImage compFrame;
        List<Tessel> tessels;
        // Recorrido de los frames
        for (int i = 0; i < input.size(); i++) {
            frame = input.get(i);
            // Si no es marco base, procesarlo.
            // Si lo es, anadirlo directamente a la salida.
            if (!isImageBase(i)) {
                // Teselar el marco previo
                prevFrame = input.get(i - 1);
                tessels = tessellate(prevFrame);
                compFrame = frame.getBufImg();
                // Buscar coincidencias para cada tesela del marco previo en el
                // marco actual.
                for (int j = 0; j < tessels.size(); j++) {
                    tessel = tessels.get(j);
                    // Busqueda
                    match = searchMatches(frame, tessel, j);
                    // Compensacion
                    if (match != null) {
                        // Anadir a la salida el vector de coincidencia.
                        match.nFrame = i;
                        compFrame = compensate(compFrame, tessel, match);
                        output.addVector(match);
                    }
                }
                // Anadir el marco compensado
                output.addFrame(compFrame);
            } else {
                output.addFrame(frame.getBufImg());
            }
        }
    }

}

