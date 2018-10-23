// https://searchcode.com/api/result/11398119/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 *
 * @author Andrew
 */
public class NTSCRenderer extends Renderer {

    private int offset = 0;
    private int scanline = 0;
    private static final boolean VHS = false;
    //hm, if I downsampled these perfectly to 4Fsc i could get rid of matrix deode
    //altogether...
    private final static byte[][] colorphases = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},//0x00
        {1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0},//0x01
        {1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1},//0x02
        {1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1},//0x03
        {1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1},//0x04
        {1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1},//0x05
        {1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1},//0x06
        {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},//0x07
        {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0},//0x08
        {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0},//0x09
        {0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},//0x0A
        {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},//0x0B
        {0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},//0x0C
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},//0x0D
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},//0x0E
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};//0x0F
    private final static double[][] lumas = genlumas();
    private final static double[][] coloremph = {
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {0.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f},//X
        {1.0f, 1.0f, 1.0f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 1.0f, 1.0f, 1.0f},//Y
        {0.7f, 1.0f, 1.0f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f},//XY
        {0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.7f},//Z
        {0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 1.0f, 1.0f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f},//XZ
        {0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 1.0f, 1.0f, 0.7f},//YZ
        {0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f}};//XYZ
    //private final static double sync = -0.359f;
    private int frames = 0;
    private final double[] i_filter = new double[12], q_filter = new double[12];
    final double[] sample = new double[2728];
    private final static int[][] colortbl = genColorCorrectTbl();

    public NTSCRenderer() {

        int hue = -290;
        double col_adjust = 2.2;

        for (int j = 0; j < 12; ++j) {
            double angle = Math.PI * ((hue + (j << 8)) / (12 * 128.0) - 33.0 / 180);
            i_filter[j] = -col_adjust * Math.cos(angle);
            q_filter[j] = col_adjust * Math.sin(angle);
        }

    }

    public static int[][] genColorCorrectTbl() {
        int[][] corr = new int[3][256];
        //double gamma = 1.2;
        double brightness = 0;
        double contrast = 1.05;
        for (int i = 0; i < 256; ++i) {
            double br = (i * contrast - (128 * contrast) + 128 + brightness) / 255.;
            //corr[2][i] = clamp((int) (255 * Math.pow(br, gamma)));
            corr[2][i] = clamp((int) (255 * (1.4 * Math.pow(br, 1.6) + -0.4 * Math.pow(br, 4))));
            //poked around in excel to get this gamma curve.
            corr[1][i] = corr[2][i] << 8;
            corr[0][i] = (corr[2][i] << 16) | 0xff000000;
        }
        return corr;
    }

    public static double[][] genlumas() {
        double[][] premultlumas = {
            {0.397f, 0.681f, 1.0f, 1.0f},
            //0x00    0x10    0x20    0x30
            {-0.117f, 0.000f, 0.308f, 0.715f}};
//        double gamma = 1.4;
//        for (int i = 0; i < premultlumas.length; ++i) {
//            for (int j = 0; j < premultlumas[i].length; ++j) {
//                double d = premultlumas[i][j];
//                if (d > 0) {
//                    premultlumas[i][j] = Math.pow(d, gamma);
//                }
//            }
//        }
        return premultlumas;
    }

    public final double[] ntsc_encode(int[] nescolors, int pxloffset, int bgcolor, boolean dotcrawl) {
        //part one of the process. creates a 2728 pxl array of doubles representing
        //ntsc version of scanline passed to it. Meant to be called 240x a frame

        //todo:
        //-make this encode an entire frame at a time
        //-add emphasis bits back
        //-reduce # of array lookups (precalc. what is necessary)
        //-fix dot crawl pattern (it's backwards from real NES I think)

        //first of all, increment scanline numbers and get the offset for this line.
        ++scanline;
        if (scanline > 239) {
            scanline = 0;
            ++frames;
            offset = ((frames & 1) == 0 && dotcrawl) ? 6 : 0;
        }
        offset = (offset + 4) % 12; //3 line dot crawl
        //offset = (offset + 6) % 12; //2 line dot crawl it couldve had
        int i, col;
        //luminance portion of nes color is bits 4-6, chrominance part is bits 1-3
        //they are both used as the index into various tables
        //the chroma generator chops between 2 different voltages from luma table 
        //at a constant rate but shifted phase.
//        for (i = 0; i < 200; ++i) {
//            sample[i] = sync; //sync and front porch are not used by decoder, so commented out
//        }
//        for (int i = 200; i < 232; ++i) {
//            sample[i] = lumas[1][1]; //black : color 1D
//        }
//        for (i = 232; i < 352; ++i) {
//            sample[i] = lumas[colorphases[8][(i + offset) % 12]][1]; //colorburst = color 0x18;
//        }
//        for (int i = 352; i < 400; ++i) {
//            sample[i] = lumas[1][1]; //black : color 1D
//        }
        for (i = 400; i < 520; ++i) {
            sample[i] = lumas[colorphases[bgcolor & 0xf][(i + offset) % 12]][(bgcolor & 0x30) >> 4];
        }
        for (i = 520; i < 2568; ++i) {
            col = nescolors[(((i - 520) >> 3)) + pxloffset];
            if ((col & 0xf) > 0xd) {
                col = 0x0f;
            }
            sample[i] = lumas[
                    colorphases[col & 0xf][(i + offset) % 12]][(col & 0x30) >> 4] * coloremph[(col & 0x1c0) >> 6][(i + offset) % 12];
        }

        for (i = 2568; i < 2656; ++i) {
            sample[i] = lumas[colorphases[bgcolor & 0xf][(i + offset) % 12]][(bgcolor & 0x30) >> 4];
        }
//        for (int i = 2656; i < 2720; ++i) {
//            sample[i] = lumas[1][1]; //black : color 1D
//        }
        return sample;
    }
    public final static double chroma_filterfreq = 3579000., pixel_rate = 42950000.;
    public final static double iffq = chroma_filterfreq * 0.6,
            qffq = chroma_filterfreq * .4;
    public double[] chroma = new double[2728];
    public double[] luma = new double[2728];
    final double[] eye = new double[2728];
    final double[] queue = new double[2728];
    private final static int coldelay = 8;

    public final void ntsc_decode(final double[] ntsc, final int[] frame, int frameoff) {
        //decodes one scan line of ntsc video and outputs as rgb packed in int
        //uses the cheap TV method, which is filtering the chroma from the luma w/o
        //combing or buffering previous lines

        res_filter(ntsc, chroma);
        //chroma = ntsc;
        //sub_filter(luma,ntsc,chroma);
        //luma = chroma;


        //ch_filter(ntsc, luma);
        //el_filter(ntsc, luma);
        //biquad_filter(ntsc, luma);
        //bs_filter(ntsc, luma);
        box_filter(ntsc, luma, 12);
        //cap_filter(luma,luma,3580000);
        //luma = ntsc;
        int cbst;


        //find color burst
        switch (offset) {
            case 10:
                cbst = 242;
                break;
            case 2:
                cbst = 250;
                break;
            case 6:
                cbst = 246;
                break;
            case 4:
                cbst = 248;
                break;
            case 8:
                cbst = 244;
                break;
            case 0:
            default:
                cbst = 240;
                break;
        }
//        for (cbst = 240; cbst < 260; ++cbst) {
//            if (chroma[cbst] >= 0.4) {
//                break;
//            }
//        }
        int x = 492;
//        if (VHS) {
//            //vhs picture effect
//            if (scanline <= 60) {
//                x -= 120 / (scanline + 1);
//                cbst += 20 / (scanline + 1);
//            }
//            cbst += ((Math.random() < 0.02) ? Math.round(Math.random() * 2 - 1) : 0);
//        }

        int j = 0;
        for (int i = (cbst - coldelay); i < 2620; ++i, ++j, ++cbst, j %= 12) {
            //matrix decode the color diff signals;
            eye[i] = i_filter[j] * chroma[cbst];
            queue[i] = q_filter[j] * chroma[cbst];
        }
//        box_filter(eye,eye,20);
//        box_filter(queue,queue,20);
        cap_filter(eye, eye, iffq);
        cap_filter(queue, queue, qffq);
//        sah_filter(eye, eye);
//        sah_filter(queue, queue);
        //random picture jitter of 1 subpixel. helps surprisingly much with
        //color banding in dark blue (which itself happens because of the
        //chroma filters)
        //x += (int) (Math.random() * 4 - 2);
        for (int i = 0; i < frame_w; ++i) {
            frame[i + frameoff] =
                    ((luma[++x] <= 0) ? 0xff000000 : colortbl[0][clamp((int) (iqm[0][0] * luma[x] + iqm[0][1] * eye[x] + iqm[0][2] * queue[x]))])
                    | ((luma[++x] <= 0) ? 0xff000000 : colortbl[1][clamp((int) (iqm[1][0] * luma[x] + iqm[1][1] * eye[x] + iqm[1][2] * queue[x]))])
                    | ((luma[++x] <= 0) ? 0xff000000 : colortbl[2][clamp((int) (iqm[2][0] * luma[x] + iqm[2][1] * eye[x] + iqm[2][2] * queue[x]))]);
        }
    }
    private final static int[][] iqm = {{255, -249, 159}, {255, 70, -166}, {255, 283, 436}};

    public static int clamp(final int a) {
        return (a != (a & 0xff)) ? ((a < 0) ? 0 : 255) : a;
    }
    public final static int frame_w = 704;
    int[] out = new int[frame_w];
    int[] frame = new int[frame_w * 240];

    @Override
    public BufferedImage render(int[] nespixels, int bgcolor, boolean dotcrawl) {

        for (int line = 0; line < 240; ++line) {
            ntsc_decode(ntsc_encode(nespixels, line * 256, bgcolor, dotcrawl), frame, line * frame_w);
        }
        BufferedImage i = getImageFromArray(frame, frame_w * 8, frame_w, 224);
//        Kernel kernel = new Kernel(1, 3,
//                new float[]{.25f,
//                    .5f,
//                    .25f,});
//        BufferedImageOp op = new ConvolveOp(kernel);
//        return op.filter(i, null); //blur
        return i;
    }
    double[] xv = new double[5];
    double[] yv = new double[5];
    private final static double[] a = {2.143505E-4, 8.566037E-4,
        1.284906E-4, 8.566037E-4, 9.726342E-4},
            b = {3.425455, -4.479272, 2.643718, -5.933269E-1};
    
    double hold = 0;
    public final void sah_filter(final double[] in, final double[] out) {
        for (int i = 0; i < in.length; ++i) {
            if (Math.abs(in[i]) > 0.1) {
                hold = in[i];
            }
            out[i] = hold;
        }
    }

    public final void ch_filter(final double[] filter_in, final double[] filter_out) {
        //does a 4 pole chebychev filter with r = 0.05
        //that's a 2.14 mhz lowpass. this may be a little TOO much blur.
        //try boosting later but for now this gets rid of almost all the chroma.
        for (int i = 358; i < 2656; ++i) {
            filter_out[i] = a[0] * filter_in[i]
                    + a[1] * filter_in[i - 1]
                    + a[2] * filter_in[i - 2]
                    + a[3] * filter_in[i - 3]
                    + a[4] * filter_in[i - 4]
                    + b[0] * filter_out[i - 1]
                    + b[1] * filter_out[i - 2]
                    + b[2] * filter_out[i - 3]
                    + b[3] * filter_out[i - 4];
        }
    }
    final static double PI = 3.1415926f;
    final static double r = 0.4; //<-controls gain, and thus selectivity.
    //mess with it some later.
    final static double aone = 2 * r * Math.cos(2 * PI * 1 / 12.);
    final static double atwo = -Math.pow(r, 2);
    final static double bzero = (1 - Math.pow(r, 2)) / 2.;
    //no b1 coeff
    final static double btwo = -bzero;
    //this paper uses a for feedback coeffs and b for
    //new stuff from the original sample array.

    public final void res_filter(final double[] in, final double[] out) {
        //coefficients from http://www.music.mcgill.ca/~gary/307/week2/filters.html
        for (int i = 200; i < 2656; ++i) {
            out[i] = out[i - 1] * aone + out[i - 2] * atwo
                    + in[i] * bzero + in[i - 2] * btwo;
        }
    }

    public final void box_filter(final double[] in, final double[] out, final int order) {
        int l = in.length;
        double accum = 0;
        for (int i = 0; i < order; ++i) {
            accum += in[i];
        }
        for (int i = order; i < l; ++i) {
            accum = accum + in[i] - in[i - order];
            out[i] = accum / order;
        }
    }

    public final void cap_filter(final double[] in, final double[] out, final double rc) {
        //yet another low pass filter
        //rc is 1/the time constant of the RC system, t is the time step duration
        //use like cap_filter(ntsc,luma,chroma_filterfreq *.632,1/pixel_rate);
        //code stolen from a seventies Byte magazine
        //not nearly good enough for video work, or a real tv either
        //they would've at least used an RCI lowpass
        double b = 0;
        final double xp = Math.exp(-rc * t);
        for (int i = 200; i < 2656; ++i) {
            b *= xp;
            b = rc * t * (in[i]) + b;
            out[i] = b;
        }
    }
    double rc = 0;
    double t = 1 / pixel_rate;

    public final void sub_filter(final double[] luma, final double[] ntsc, final double[] chroma) {
        for (int i = 200; i < 2700; ++i) {
            luma[i] = ntsc[i] + chroma[i - 1];
        }
    }
}

