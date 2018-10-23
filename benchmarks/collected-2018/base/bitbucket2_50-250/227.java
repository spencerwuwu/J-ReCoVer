// https://searchcode.com/api/result/47294919/

package ub.edu.tmm.huelamosolans.codec;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Video decoder.
 * 
 * @author albertohuelamosegura
 */
public class Decoder {

    /**
     * Input video.
     */
    protected Video input;
    /**
     * Output video.
     */
    protected Video output;
    /**
     * Tessel height.
     */
    protected int tHeight;
    /**
     * Tessel width.
     */
    protected int tWidth;
    /**
     * Base frame index.
     */
    protected int baseCount;

    /**
     * Sole constructor.
     * 
     * @param path
     *            Path where the input file is.
     * @throws IOException
     *             Thrown if the file does not exist or there is a
     *             problem reading it.
     * @throws ClassNotFoundException
     *             Thrown if the file is corrupt.
     */
    public Decoder(String path) throws IOException, ClassNotFoundException {
        setInput(path);
        output = new Video();
    }

    /**
     * Gets the output video.
     * 
     * @return The output video.
     */
    public Video getOutput() {
        return output;
    }

    private void setInput(String path) throws IOException,
            ClassNotFoundException {
        input = Video.loadNewVideoFromFile(path);
        tHeight = input.gettHeight();
        tWidth = input.gettWidth();
        baseCount = input.getBaseCount();
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
     * Inserts the a tessel in a frame.
     * 
     * @param frame
     *            Frame to insert the tessel.
     * @param tessel
     *            Tessel to insert.
     * @param v
     *            Insert information.
     */
    protected void replaceForTessel(BufferedImage frame, Tessel tessel,
            MatchVector v) {
        WritableRaster tmp = frame.getRaster();
        double[] pixels = new double[tessel.getRaster().getWidth()
                * tessel.getRaster().getHeight()
                * tessel.getRaster().getNumBands()];
        pixels = tessel.getRaster().getPixels(0, 0,
                tessel.getRaster().getWidth(), tessel.getRaster().getHeight(),
                pixels);
        tmp.setPixels(v.x, v.y, tessel.getRaster().getWidth(), tessel
                .getRaster().getHeight(), pixels);
        frame.setData(tmp);
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
     * Engages the decoding process.
     */
    public void decode() {
        BufferedImage frame, prevFrame;
        List<Tessel> tessels;
        List<MatchVector> vecs;
        Tessel tessel;
        // Recorrido de los marcos
        for (int nFrame = 0; nFrame < input.getFrames().size(); nFrame++) {
            frame = input.getFrame(nFrame);
            // Si se trata de un marco base se anade directamente. Si no, se
            // procesa.
            if (!isImageBase(nFrame)) {
                // Se buscan los vectores de ese marco y se recompone con las
                // teselas del marco anterior que marcan los vectores.
                prevFrame = input.getFrame(nFrame - 1);
                vecs = input.getVectorsForFrame(nFrame);
                if (vecs.size() > 0) {
                    tessels = tessellate(prevFrame.getRaster());
                    for (MatchVector v : vecs) {
                        tessel = tessels.get(v.nTessel);
                        replaceForTessel(frame, tessel, v);
                    }
                }
                output.addFrame(frame);
            } else {
                output.addFrame(frame);
            }
        }
    }
}

