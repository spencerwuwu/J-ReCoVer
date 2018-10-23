// https://searchcode.com/api/result/14175814/

package com.google.code.kss.core.util;

import com.google.code.kss.core.model.ui.wireframesketcher.Widgets;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Kernel;
import java.awt.image.ConvolveOp;

/*
 * Source: http://www.componenthouse.com/article-20
 */
public class ImageUtil {

	public static String reduce(String file, Widgets w, float quality) throws Exception {
		if (file == null) throw new Exception("Input file for image reduction can not be NULL or empty.");
		if (w == null) throw new Exception("Widget for image reduction can not be NULL or empty.");
		
		File fin = new File(ClassLoader.getSystemResource(file).toURI());
		File fout = new File(getReducedFileName(w));
		resize(fin, fout, w.getMeasuredWidth(), quality);
		
		return fout.toPath().toString();
	}
	
	public static String getReducedFileName(Widgets w) {
		String fileName = "r" + CommonUtil.getName(w.getSrc());
		return fileName;
	}
	
	public static void resize(File originalFile, File resizedFile,
			int newWidth, float quality) throws IOException {

		if (!(quality >= 0 && quality <= 1)) {
			throw new IllegalArgumentException(
					"Quality has to be between 0 and 1");
		}

		ImageIcon ii = new ImageIcon(originalFile.getCanonicalPath());
		Image i = ii.getImage();
		Image resizedImage = null;

		int iWidth = i.getWidth(null);
		int iHeight = i.getHeight(null);

		if (iWidth > iHeight) {
			resizedImage = i.getScaledInstance(newWidth, (newWidth * iHeight)
					/ iWidth, Image.SCALE_SMOOTH);
		} else {
			resizedImage = i.getScaledInstance((newWidth * iWidth) / iHeight,
					newWidth, Image.SCALE_SMOOTH);
		}

		// This code ensures that all the pixels in the image are loaded.
		Image temp = new ImageIcon(resizedImage).getImage();

		// Create the buffered image.
		BufferedImage bufferedImage = new BufferedImage(temp.getWidth(null),
				temp.getHeight(null), BufferedImage.TYPE_INT_RGB);

		// Copy image to buffered image.
		Graphics g = bufferedImage.createGraphics();

		// Clear background and paint the image.
		g.setColor(Color.white);
		g.fillRect(0, 0, temp.getWidth(null), temp.getHeight(null));
		g.drawImage(temp, 0, 0, null);
		g.dispose();

		// Soften.
		float softenFactor = 0.05f;
		float[] softenArray = { 0, softenFactor, 0, softenFactor,
				1 - (softenFactor * 4), softenFactor, 0, softenFactor, 0 };
		Kernel kernel = new Kernel(3, 3, softenArray);
		ConvolveOp cOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		bufferedImage = cOp.filter(bufferedImage, null);

		// Write the jpeg to a file.
		FileOutputStream out = new FileOutputStream(resizedFile);

		// Encodes image as a JPEG data stream
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);

		JPEGEncodeParam param = encoder
				.getDefaultJPEGEncodeParam(bufferedImage);

		param.setQuality(quality, true);

		encoder.setJPEGEncodeParam(param);
		encoder.encode(bufferedImage);
	}

	// Example usage
	public static void main(String[] args) throws IOException {
		File originalImage = new File(
				"C:/Documents and Settings/ADMIN/Workspaces/Eclipse 3.5 Java/KSSToolkit/img/lua/SampleFiOSLeftPanel.png");
		resize(
				originalImage,
				new File(
						"C:/Documents and Settings/ADMIN/Workspaces/Eclipse 3.5 Java/KSSToolkit/img/lua/SampleFiOSLeftPanel1.png"),
				400, 0.7f);
		resize(
				originalImage,
				new File(
						"C:/Documents and Settings/ADMIN/Workspaces/Eclipse 3.5 Java/KSSToolkit/img/lua/SampleFiOSLeftPanel2.png"),
				400, 1f);
	}

}

