// https://searchcode.com/api/result/93047034/

package com.wq.util;

import com.sun.imageio.plugins.bmp.BMPImageReader;
import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.jpeg.JPEGImageReader;
import com.sun.imageio.plugins.png.PNGImageReader;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;

/**
 * :<br>
 * :
 *
 * @author Administrator
 */
public class ImageUtils {

    /**
     * 
     */
    public static String IMAGE_TYPE_GIF = "gif";// 
    public static String IMAGE_TYPE_JPG = "jpg";// 
    public static String IMAGE_TYPE_JPEG = "jpeg";// 
    public static String IMAGE_TYPE_BMP = "bmp";// Bitmap(),Windows
    public static String IMAGE_TYPE_PNG = "png";// 
    public static String IMAGE_TYPE_PSD = "psd";// PhotoshopPhotoshop

    /**
     * :
     *
     * @param args
     */
    public static void main(String[] args) {
        // 1-:
        // :
        ImageUtils.scale("e:/abc.jpg", "e:/abc_scale.jpg", 2, true);//OK
        // :
        ImageUtils.scale2("e:/abc.jpg", "e:/abc_scale2.jpg", 500, 300, true);//OK

        // 2-:
        // :
        ImageUtils.cut("e:/abc.jpg", "e:/abc_cut.jpg", 0, 0, 400, 400);//OK
        // :
        ImageUtils.cut2("e:/abc.jpg", "e:/", 2, 2);//OK
        // :
        ImageUtils.cut3("e:/abc.jpg", "e:/", 300, 300);//OK

        // 3-:
        ImageUtils.convert("e:/abc.jpg", "GIF", "e:/abc_convert.gif");//OK

        // 4-:
        ImageUtils.gray("e:/abc.jpg", "e:/abc_gray.jpg");//OK

        // 5-:
        // :
        ImageUtils.pressText("", "e:/abc.jpg", "e:/abc_pressText.jpg", "", Font.BOLD, Color.white, 80, 0, 0, 0.5f);//OK
        // :
        ImageUtils.pressText2("", "e:/abc.jpg", "e:/abc_pressText2.jpg", "", 36, Color.white, 80, 0, 0, 0.5f);//OK

        // 6-:
        ImageUtils.pressImage("e:/abc2.jpg", "e:/abc.jpg", "e:/abc_pressImage.jpg", 0, 0, 0.5f);//OK
    }

    /**
     * ()
     *
     * @param srcImageFile 
     * @param result       
     * @param scale        
     * @param flag         :true ; false ;
     */
    public final static void scale(String srcImageFile, String result,
                                   int scale, boolean flag) {
        try {
            BufferedImage src = ImageIO.read(new File(srcImageFile)); // 
            int width = src.getWidth(); // 
            int height = src.getHeight(); // 
            if (flag) {// 
                width = width * scale;
                height = height * scale;
            } else {// 
                width = width / scale;
                height = height / scale;
            }
            Image image = src.getScaledInstance(width, height,
                    Image.SCALE_DEFAULT);
            BufferedImage tag = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = tag.getGraphics();
            g.drawImage(image, 0, 0, null); // 
            g.dispose();
            ImageIO.write(tag, "JPEG", new File(result));// 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * 
     *
     * @param originalPic
     * @param bo
     * @return
     */
    public static BufferedImage getResizePicture(BufferedImage originalPic,
                                                 double bo) {
        // 
        int originalImageWidth = originalPic.getWidth();
        // 
        int originalImageHeight = originalPic.getHeight();

        // 
        int changedImageWidth = (int) (originalImageWidth * bo);
        // 
        int changedImageHeight = (int) (originalImageHeight * bo);

        // 
        BufferedImage changedImage = new BufferedImage(changedImageWidth,
                changedImageHeight, BufferedImage.TYPE_3BYTE_BGR
        );

        // double widthBo = (double) yourWidth / originalImageWidth;
        // double heightBo = (double) yourHeightheight / originalImageHeight;
        // 
        double widthBo = bo;
        // 
        double heightBo = bo;

        AffineTransform transform = new AffineTransform();
        transform.setToScale(widthBo, heightBo);

        // 
        AffineTransformOp ato = new AffineTransformOp(transform, null);
        ato.filter(originalPic, changedImage);
        // 
        return changedImage;
    }

    /**
     * ,
     * 
     *
     * @param originalPic
     * @param bo
     * @return
     */
    public static BufferedImage reduceImg(BufferedImage originalPic, double bo) {
        // 
        int originalImageWidth = originalPic.getWidth();
        // 
        int originalImageHeight = originalPic.getHeight();

        // 
        int changedImageWidth = (int) (originalImageWidth * bo);
        // 
        int changedImageHeight = (int) (originalImageHeight * bo);

        BufferedImage tag = new BufferedImage(changedImageWidth, changedImageHeight,
                BufferedImage.TYPE_INT_RGB);

        tag.getGraphics().drawImage(originalPic.getScaledInstance(changedImageWidth, changedImageHeight, Image.SCALE_SMOOTH), 0, 0, null);

        return tag;
    }

    /**
     * ,
     *
     * @param originalPic 
     * @param factor      
     * @return BufferedImage
     */
    public static BufferedImage reduce(BufferedImage originalPic, double factor) {
        BufferedImage source;
        try {
            source = originalPic;

            int sourceW = source.getWidth();
            int sourceH = source.getHeight();

            int w = (int) (sourceW / factor + 0.5);
            int h = (int) (sourceH / factor + 0.5);

            boolean hasAlpha = source.getColorModel().hasAlpha();
            int format = hasAlpha ? BufferedImage.TYPE_INT_ARGB
                    : BufferedImage.TYPE_INT_RGB;
//            System.out.println("w:" + w + "h:" + h);
            BufferedImage output = new BufferedImage(w, h, format);
            Graphics2D g = output.createGraphics();

            // Ask Java to use its best but slowest scaling
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(source, 0, 0, w, h, null);
            g.dispose();
            return output;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    /**
     * ()
     *
     * @param srcImageFile 
     * @param result       
     * @param height       
     * @param width        
     * @param bb           :true; false;
     */
    public final static void scale2(String srcImageFile, String result, int height, int width, boolean bb) {
        try {
            double ratio = 0.0; // 
            File f = new File(srcImageFile);
            BufferedImage bi = ImageIO.read(f);
            Image itemp = bi.getScaledInstance(width, height, bi.SCALE_SMOOTH);
            // 
            if ((bi.getHeight() > height) || (bi.getWidth() > width)) {
                if (bi.getHeight() > bi.getWidth()) {
                    ratio = (new Integer(height)).doubleValue()
                            / bi.getHeight();
                } else {
                    ratio = (new Integer(width)).doubleValue() / bi.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(AffineTransform
                        .getScaleInstance(ratio, ratio), null);
                itemp = op.filter(bi, null);
            }
            if (bb) {//
                BufferedImage image = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                if (width == itemp.getWidth(null))
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                else
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                g.dispose();
                itemp = image;
            }
            ImageIO.write((BufferedImage) itemp, "JPEG", new File(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ()
     *
     * @param srcImageFile 
     * @param result       
     * @param height       
     * @param width        
     * @param bb           :true; false;
     */
    public final static Image scale(String srcImageFile, String result, int height, int width, boolean bb) {
        try {
            double ratio = 0.0; // 
            File f = new File(srcImageFile);
            BufferedImage bi = ImageIO.read(f);
            Image itemp = bi.getScaledInstance(width, height, bi.SCALE_SMOOTH);
            // 
            if ((bi.getHeight() > height) || (bi.getWidth() > width)) {
                if (bi.getHeight() > bi.getWidth()) {
                    ratio = (new Integer(height)).doubleValue()
                            / bi.getHeight();
                } else {
                    ratio = (new Integer(width)).doubleValue() / bi.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(AffineTransform
                        .getScaleInstance(ratio, ratio), null);
                itemp = op.filter(bi, null);
            }
            if (bb) {//
                BufferedImage image = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                if (width == itemp.getWidth(null))
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                else
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                g.dispose();
                itemp = image;
            }
            return itemp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ()
     *
     * @param srcImageFile 
     * @param result       
     * @param x            X
     * @param y            Y
     * @param width        
     * @param height       
     */
    public final static void cut(String srcImageFile, String result,
                                 int x, int y, int width, int height) {
        try {
            // 
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            int srcWidth = bi.getHeight(); // 
            int srcHeight = bi.getWidth(); // 
            if (srcWidth > 0 && srcHeight > 0) {
                Image image = bi.getScaledInstance(srcWidth, srcHeight,
                        Image.SCALE_DEFAULT);
                // 
                // : CropImageFilter(int x,int y,int width,int height)
                ImageFilter cropFilter = new CropImageFilter(x, y, width, height);
                Image img = Toolkit.getDefaultToolkit().createImage(
                        new FilteredImageSource(image.getSource(),
                                cropFilter));
                BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics g = tag.getGraphics();
                g.drawImage(img, 0, 0, width, height, null); // 
                g.dispose();
                // 
                ImageIO.write(tag, "JPEG", new File(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ()
     *
     * @param srcImageFile 
     * @param descDir      
     * @param rows         2, [1, 20] 
     * @param cols         2, [1, 20] 
     */
    public final static void cut2(String srcImageFile, String descDir,
                                  int rows, int cols) {
        try {
            if (rows <= 0 || rows > 20) rows = 2; // 
            if (cols <= 0 || cols > 20) cols = 2; // 
            // 
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            int srcWidth = bi.getHeight(); // 
            int srcHeight = bi.getWidth(); // 
            if (srcWidth > 0 && srcHeight > 0) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                int destWidth = srcWidth; // 
                int destHeight = srcHeight; // 
                // 
                if (srcWidth % cols == 0) {
                    destWidth = srcWidth / cols;
                } else {
                    destWidth = (int) Math.floor(srcWidth / cols) + 1;
                }
                if (srcHeight % rows == 0) {
                    destHeight = srcHeight / rows;
                } else {
                    destHeight = (int) Math.floor(srcWidth / rows) + 1;
                }
                // 
                // :
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 
                        // : CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight,
                                destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(
                                new FilteredImageSource(image.getSource(),
                                        cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth,
                                destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        g.drawImage(img, 0, 0, null); // 
                        g.dispose();
                        // 
                        ImageIO.write(tag, "JPEG", new File(descDir
                                + "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ()
     *
     * @param srcImageFile 
     * @param descDir      
     * @param destWidth    200
     * @param destHeight   150
     */
    public final static void cut3(String srcImageFile, String descDir,
                                  int destWidth, int destHeight) {
        try {
            if (destWidth <= 0) destWidth = 200; // 
            if (destHeight <= 0) destHeight = 150; // 
            // 
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            int srcWidth = bi.getHeight(); // 
            int srcHeight = bi.getWidth(); // 
            if (srcWidth > destWidth && srcHeight > destHeight) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                int cols = 0; // 
                int rows = 0; // 
                // 
                if (srcWidth % destWidth == 0) {
                    cols = srcWidth / destWidth;
                } else {
                    cols = (int) Math.floor(srcWidth / destWidth) + 1;
                }
                if (srcHeight % destHeight == 0) {
                    rows = srcHeight / destHeight;
                } else {
                    rows = (int) Math.floor(srcHeight / destHeight) + 1;
                }
                // 
                // :
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 
                        // : CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight,
                                destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(
                                new FilteredImageSource(image.getSource(),
                                        cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth,
                                destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        g.drawImage(img, 0, 0, null); // 
                        g.dispose();
                        // 
                        ImageIO.write(tag, "JPEG", new File(descDir
                                + "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * :GIF->JPGGIF->PNGPNG->JPGPNG->GIF(X)BMP->PNG
     *
     * @param srcImageFile  
     * @param formatName     String:JPGJPEGGIF
     * @param destImageFile 
     */
    public final static void convert(String srcImageFile, String formatName, String destImageFile) {
        try {
            File f = new File(srcImageFile);
            f.canRead();
            f.canWrite();
            BufferedImage src = ImageIO.read(f);
            ImageIO.write(src, formatName, new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     *
     * @param srcImageFile  
     * @param destImageFile 
     */
    public final static void gray(String srcImageFile, String destImageFile) {
        try {
            BufferedImage src = ImageIO.read(new File(srcImageFile));
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ColorConvertOp op = new ColorConvertOp(cs, null);
            src = op.filter(src, null);
            ImageIO.write(src, "JPEG", new File(destImageFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     *
     * @param pressText     
     * @param srcImageFile  
     * @param destImageFile 
     * @param fontName      
     * @param fontStyle     
     * @param color         
     * @param fontSize      
     * @param x             
     * @param y             
     * @param alpha         :alpha  [0.0, 1.0] ()
     */
    public final static void pressText(String pressText,
                                       String srcImageFile, String destImageFile, String fontName,
                                       int fontStyle, Color color, int fontSize, int x,
                                       int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    alpha));
            // 
            g.drawString(pressText, (width - (getLength(pressText) * fontSize))
                    / 2 + x, (height - fontSize) / 2 + y);
            g.dispose();
            ImageIO.write((BufferedImage) image, "JPEG", new File(destImageFile));// 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     *
     * @param pressText     
     * @param srcImageFile  
     * @param destImageFile 
     * @param fontName      
     * @param fontStyle     
     * @param color         
     * @param fontSize      
     * @param x             
     * @param y             
     * @param alpha         :alpha  [0.0, 1.0] ()
     */
    public final static void pressText2(String pressText, String srcImageFile, String destImageFile,
                                        String fontName, int fontStyle, Color color, int fontSize, int x,
                                        int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    alpha));
            // 
            g.drawString(pressText, (width - (getLength(pressText) * fontSize))
                    / 2 + x, (height - fontSize) / 2 + y);
            g.dispose();
            ImageIO.write((BufferedImage) image, "JPEG", new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     *
     * @param pressImg      
     * @param srcImageFile  
     * @param destImageFile 
     * @param x              
     * @param y              
     * @param alpha         :alpha  [0.0, 1.0] ()
     */
    public final static void pressImage(String pressImg, String srcImageFile, String destImageFile,
                                        int x, int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int wideth = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(wideth, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, wideth, height, null);
            // 
            Image src_biao = ImageIO.read(new File(pressImg));
            int wideth_biao = src_biao.getWidth(null);
            int height_biao = src_biao.getHeight(null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    alpha));
            g.drawImage(src_biao, (wideth - wideth_biao) / 2,
                    (height - height_biao) / 2, wideth_biao, height_biao, null);
            // 
            g.dispose();
            ImageIO.write((BufferedImage) image, "JPEG", new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * text()
     *
     * @param text
     * @return
     */
    public final static int getLength(String text) {
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            if (new String(text.charAt(i) + "").getBytes().length > 1) {
                length += 2;
            } else {
                length += 1;
            }
        }
        return length / 2;
    }

    /**
     * 
     */
    public static void copy(Image image) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); //
        Transferable selection = new ImageTransferable(image);  //
        clipboard.setContents(selection, null);
    }

    /**
     * .
     */
    static class ImageTransferable implements Transferable {
        public ImageTransferable(Image image) {
            theImage = image;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.imageFlavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                return theImage;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        private Image theImage;
    }

    /**
     * Image  BufferedImage 
     *
     * @param image
     * @return
     */
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        //boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            /* if (hasAlpha) {
            transparency = Transparency.BITMASK;
            }*/

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                    image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            //int type = BufferedImage.TYPE_3BYTE_BGR;//by wang
            /*if (hasAlpha) {
            type = BufferedImage.TYPE_INT_ARGB;
            }*/
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    /**
     * 
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static String getImageType(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);

        int leng = fis.available();
        BufferedInputStream buff = new BufferedInputStream(fis);
        byte[] mapObj = new byte[leng];
        buff.read(mapObj, 0, leng);

        String type = "";
        ByteArrayInputStream bais = null;
        MemoryCacheImageInputStream mcis = null;
        try {
            bais = new ByteArrayInputStream(mapObj);
            mcis = new MemoryCacheImageInputStream(bais);
            Iterator itr = ImageIO.getImageReaders(mcis);
            while (itr.hasNext()) {
                ImageReader reader = (ImageReader) itr.next();
                if (reader instanceof GIFImageReader) {
                    type = "gif";
                } else if (reader instanceof JPEGImageReader) {
                    type = "jpg";
                } else if (reader instanceof PNGImageReader) {
                    type = "png";
                } else if (reader instanceof BMPImageReader) {
                    type = "bmp";
                }
            }
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException ioe) {

                }
            }

            if (mcis != null) {
                try {
                    mcis.close();
                } catch (IOException ioe) {

                }
            }
        }
        return type;
    }
}
