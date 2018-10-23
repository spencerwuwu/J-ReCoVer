// https://searchcode.com/api/result/13909656/

/*
 * 
 * See copyright file
 *  
 */
package com.jmupdf.print;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Locale;

import javax.print.CancelablePrintJob;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

import com.jmupdf.enums.DocumentType;
import com.jmupdf.enums.ImageType;
import com.jmupdf.exceptions.DocException;
import com.jmupdf.exceptions.DocSecurityException;
import com.jmupdf.exceptions.PageException;
import com.jmupdf.interfaces.Document;
import com.jmupdf.interfaces.Page;
import com.jmupdf.page.PageRect;
import com.jmupdf.pdf.PdfDocument;
import com.jmupdf.tiles.TileCache;
import com.jmupdf.tiles.TiledImage;
import com.jmupdf.xps.XpsDocument;

/*
 * TODO: Needs reworking. Will focus on printing for next release
 * ---------------------------------------------------------------------
 *   1. Provide more Adobe like printing features for headless printing.
 *   2. Provide a nice custom dialog for interactive printing (like Adobe's :-)).
 *   3. ?? 
 */

/**
 * Print a document
 * 
 * @author Pedro J Rivera
 *
 */
public class PrintServices implements Printable, PrintJobListener, Runnable {
	private Document document;
	private Page page;
	private DocPrintJob job;
	private Doc simpleDoc;
	private HashPrintRequestAttributeSet printAttributeSet;
	
	private boolean printJobDone = false;
	private int currentPage = 0;
	private ImageType colorSpace;
	private int rotate;
	
	private boolean isCustomResolution = false;
	private float zoomFactor = 1f;
	
	private boolean pureBlackWhite = false;
	private boolean ditherBlackWhite = false;
	
	// TODO: private boolean center = false;
	private boolean stretch = false;
	private boolean autoRotate = false;

	private final PrintQuality defaultPrintQuality = PrintQuality.NORMAL;
	private final MediaSizeName defualtMediaSize = MediaSizeName.NA_LETTER;
	private final Chromaticity defaultChromacity = Chromaticity.MONOCHROME; 
	private OrientationRequested defaultOrientation = OrientationRequested.PORTRAIT; 
	
	/**
	 * Print a document
	 * @param document
	 * @param password
	 * @throws DocException
	 * @throws DocSecurityException
	 */
	public PrintServices(String document, String password) throws DocException, DocSecurityException {
		this.document = new PdfDocument(document, password);
	}

	/**
	 * Print a document
	 * @param document
	 * @throws DocException 
	 */
	public PrintServices(Document document) throws DocException {
		if (document.getType() == DocumentType.DOC_PDF) {
			this.document = ((PdfDocument)document).clone();		
		} else if (document.getType() == DocumentType.DOC_XPS) {
			this.document = ((XpsDocument)document).clone();
		}
		if(this.document == null) {
			throw new DocException("Could not open document for printing");
		}
	}
	
	/**
	 * Print a document
	 * @param document
	 * @throws DocException 
	 */
	public PrintServices(PdfDocument document) throws DocException {
		this.document = document.clone();		
		if(this.document == null) {
			throw new DocException("Could not open document for printing");
		}
	}

	/**
	 * Print a document
	 * @param document
	 * @throws DocException 
	 */
	public PrintServices(XpsDocument document) throws DocException {
		this.document = document.clone();		
		if(this.document == null) {
			throw new DocException("Could not open document for printing");
		}
	}
	
	/**
	 * Print a document
	 * @param jobName
	 * @param printer
	 */
	public void print(String jobName, String printer, boolean showDialog) {
		print(jobName, printer, 1, 1, document.getPageCount(), defualtMediaSize, defaultPrintQuality, defaultChromacity, defaultOrientation, showDialog);
	}

	/**
	 * Print a document
	 * @param jobName
	 * @param printer
	 * @param startPage
	 * @param endPage
	 * @param showDialog
	 */
	public void print(String jobName, String printer, int startPage, int endPage, boolean showDialog) {
		print(jobName, printer, 1, startPage, endPage, defualtMediaSize, defaultPrintQuality, defaultChromacity, defaultOrientation, showDialog);
	}

	/**
	 * Print a document
	 * @param jobName
	 * @param printer
	 * @param copies
	 */
	public void print(String jobName, String printer, int copies, boolean showDialog) {
		print(jobName, printer, copies, 1, document.getPageCount(), defualtMediaSize, defaultPrintQuality, defaultChromacity, defaultOrientation, showDialog);
	}
	
	/**
	 * Print a document
	 * @param jobName
	 * @param printer
	 * @param copies
	 * @param startPage
	 * @param endPage
	 * @param media
	 * @param quality
	 * @param color
	 * @param showDialog
	 */
	public void print(String jobName, String printer, int copies, int startPage, int endPage, MediaSizeName media, PrintQuality quality, Chromaticity color, OrientationRequested orientation, boolean showDialog) {
		try {

            HashDocAttributeSet docAttributeSet = new HashDocAttributeSet();
        	simpleDoc = new SimpleDoc(this, DocFlavor.SERVICE_FORMATTED.PRINTABLE, docAttributeSet);

			printAttributeSet = new HashPrintRequestAttributeSet();
            printAttributeSet.add(new Copies(copies));
            printAttributeSet.add(new JobName(jobName, Locale.getDefault()));
            printAttributeSet.add(new PageRanges(startPage, endPage));            
            printAttributeSet.add(media);
            printAttributeSet.add(quality);
            printAttributeSet.add(orientation);
            
            // Set margins to full page
            MediaSize mediaSize = MediaSize.getMediaSizeForName(media);
            float[] size = mediaSize.getSize(MediaSize.INCH);
            printAttributeSet.add(new MediaPrintableArea(0, 0, size[0], size[1], MediaPrintableArea.INCH));
            docAttributeSet.add(new MediaPrintableArea(0, 0, size[0], size[1], MediaPrintableArea.INCH));

            // Set default printer
            PrintService ps = getPrinterService(printer);
            
            // Determine if printer supports color printing
            if (color.equals(Chromaticity.COLOR)) {
	            Chromaticity[] o = (Chromaticity[])ps.getSupportedAttributeValues(Chromaticity.class, DocFlavor.SERVICE_FORMATTED.PRINTABLE, printAttributeSet);
	            boolean found = false;
	            for (int i=0; i<o.length; i++) {
	            	if (o[i].equals(Chromaticity.COLOR)) {
	            		found = true;
	            		break;
	            	}
	            }
	            if (!found) {
	            	color = Chromaticity.MONOCHROME;
	            }
            }
            printAttributeSet.add(color);

            // Show print dialog
            if (showDialog) {            	
            	PrinterJob pJob = PrinterJob.getPrinterJob();
            	pJob.setPrintService(ps);            	
            	if (!pJob.printDialog(printAttributeSet)) {
            		dispose();
            		return;
            	}
            	ps = pJob.getPrintService();
            }

            // Get selected page orientation then set it back to portrait.
            // Rotation will be set when rendering image. 
            OrientationRequested o = (OrientationRequested)printAttributeSet.get(OrientationRequested.class);
            printAttributeSet.add(OrientationRequested.PORTRAIT);
            if (o.equals(OrientationRequested.PORTRAIT)) {
            	rotate = Page.PAGE_ROTATE_NONE;
            } else {
            	rotate = Page.PAGE_ROTATE_90;
            }

            // Set default color space
            Chromaticity c = (Chromaticity)printAttributeSet.get(Chromaticity.class);
            if (c.equals(Chromaticity.MONOCHROME)) {
            	if (pureBlackWhite) {
            		if (ditherBlackWhite) {
            			colorSpace = ImageType.IMAGE_TYPE_BINARY_DITHER;
            		} else {
            			colorSpace = ImageType.IMAGE_TYPE_BINARY;
            		}
            	} else {
            		colorSpace = ImageType.IMAGE_TYPE_GRAY;
            	}
            } else {
            	colorSpace = ImageType.IMAGE_TYPE_RGB;
            }

        	job = ps.createPrintJob();
            job.addPrintJobListener(this);

            // Run in a separate thread
            Thread t = new Thread(this);
            t.start();

		} catch (PrinterException e) {
			dispose();
		}
	}

	/**
	 * Print page
	 */
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

		currentPage = pageIndex + 1;

		if (currentPage > document.getPageCount() || printJobDone) {
			return NO_SUCH_PAGE;
		}

		// Get graphics
        Graphics2D g2 = (Graphics2D) graphics;

        // Set background color
        g2.setBackground(Color.WHITE);
        
        // Load page, and load it only once
		if (page == null || currentPage != page.getPageNumber()) {
			if (page != null) {
				page.dispose();
			}
			try {
				page = document.getPage(currentPage);
			} catch (PageException e) {
				return NO_SUCH_PAGE;
			}
			if (!isCustomResolution()) {
				float z = (float)page.getWidth() / (float)pageFormat.getWidth();
				zoomFactor = (float)g2.getTransform().getScaleX();
				if (z > 1) {
					zoomFactor /= z;
				}
			}	        	
		}

		/* Setup page region */
		/* TODO: Add page centering, etc. */

		PageRect rect = page.getBoundBox();
		int rotation = rotate;
		
		// Page format ...
		int px = 0; 
		int py = 0; 
		int pw = (int)pageFormat.getWidth(); 
		int ph = (int)pageFormat.getHeight();

		/*
		 * Auto rotate page?
		 */
		if (isAutoRotate()) {
			if (rect.getWidth() > rect.getHeight()) {
				rotation = Page.PAGE_ROTATE_90;
			} else {
				rotation = Page.PAGE_ROTATE_NONE;
			}
			/* Consider internal rotation */
			if (page.getRotation() == Page.PAGE_ROTATE_90) {
				rotation = Page.PAGE_ROTATE_270;
			} else if(page.getRotation() == Page.PAGE_ROTATE_270) {
				rotation = Page.PAGE_ROTATE_90;
			}
			rect = rect.rotate(rect, rotation);
		}
		
		/*
		 * Print as is?
		 */
		if (!isAutoRotate()) {
			
			/* Consider internal rotation */
			if (page.getRotation() != Page.PAGE_ROTATE_NONE) {
				
				/* Landscape */
				if (rotation == Page.PAGE_ROTATE_90) {
					rotation = 270;
					PageRect pr = rect.rotate(rect, Page.PAGE_ROTATE_90);
					pw = pr.getWidth(); 
					ph = pr.getHeight();
					
				/* Portrait */		
				} else {  
					rotation = 0;
					PageRect pr = rect.rotate(rect, Page.PAGE_ROTATE_90);
					pw = pr.getWidth(); 
					ph = pr.getHeight();					
				}
			
			/* Normal processing */
			} else {
				if (rotation == Page.PAGE_ROTATE_90) {
					PageRect pr = rect.rotate(rect, Page.PAGE_ROTATE_NONE);
					pw = pr.getWidth(); 
					ph = pr.getHeight();
				}
			}

		}

		/*
		 * Stretch?
		 */
		if (isStretch()) {
			pw = (int)pageFormat.getWidth(); 
			ph = (int)pageFormat.getHeight();
		}

		g2.setClip(px, py, pw, ph);
		
		/* 
		 * Printing will occur in tiles to reduce the amount of memory needed
		 * when rendering a page. Tiles will be in strips so we only have to deal
		 * with row height.
		 * 
		 * If memory is at a premium tileH can be reduced to 256.
		 */
		TileCache tc;
		int tileH = 512;
        int gx = px;
        int gy = py;
        int gw = pw;
        int gh = 0;        
        int th = tileH;
        int tw = rect.rotate(rect, rotation).scale(zoomFactor).getWidth();

        /* Create tile cache */
		tc = new TileCache(page, colorSpace, rotation, zoomFactor, tw, th);
        
		int totalTiles = tc.getTiles().size();
		
		/* Adjust tile height if source is greater than target */
		if (rect.getHeight() > ph) {			
			if (totalTiles > 1) {
				gh = ph / tc.getTiles().size();
				int firstTileH = tc.getTiles().get(0).getHeight();
				int lastTileH = tc.getTiles().get(totalTiles-1).getHeight();				
				if (lastTileH < firstTileH) {
					float spread = (lastTileH/zoomFactor) / (tc.getTiles().size()-1);
					gh = (int)((ph / (tc.getTiles().size())) + spread);
				}
			}
		} else {
			gh = (int)(tileH / zoomFactor);
			if (isStretch()) {
				int spread = ph - rect.getHeight();
				gh += spread / tc.getTiles().size();
			}
		}
		
		/* Render all tiles and send to printer */
		for (TiledImage tile : tc.getTiles()) {
			tile.render();
			if (totalTiles-1 == tile.getTileY()) {
				if ((tile.getHeight()/zoomFactor) < gh) {
					gh = (int)(tile.getHeight()/zoomFactor);
				}
			}
	        g2.drawImage(tile.getImage(), gx, gy, gw, gh, null);
	        gy += gh;
	        tile.dispose();
		}
		
		tc.dispose();
		g2.dispose();
		
		return PAGE_EXISTS;
	}
	
	/**
	 * Cancel print job
	 */
	public void cancel() {
		CancelablePrintJob cj = (CancelablePrintJob)job;
		try {
			cj.cancel();
		} catch (PrintException e) {}
	}

	/**
	 * Get auto rotate flag
	 * @return
	 */
	public boolean isAutoRotate() {
		return autoRotate;
	}
	
	/**
	 * Set whether print job should auto rotate pages before printing
	 * @param autoRotate
	 */
	public void setAutoRotate(boolean autoRotate) {
		this.autoRotate = autoRotate;
	}
	
	/**
	 * Set a custom resolution for printing. By
	 * default the resolution is determined by the 
	 * PrintQuality setting (Draft/Normal/High).
	 * What the exact resolution is depends on the 
	 * physical printer capabilities. <br> <br>
	 * 
	 * To disable custom resolution pass in a 0.
	 * 
	 * @param resolution
	 */
	public void setCustomResolution(int resolution) {
		if (resolution > 0) {
			isCustomResolution = true;
			zoomFactor = resolution / 72;
		} else {
			isCustomResolution = false;
			zoomFactor = 0;
		}
	}

	/**
	 * Return true if custom resolution has been set.
	 * @return
	 */
	public boolean isCustomResolution() {
		return isCustomResolution;
	}
	
	/**
	 * Set if contents should be stretched to fit printable area.
	 * Default value is false.
	 * 
	 * @param stretch
	 */
	public void setStretching(boolean stretch) {
		this.stretch = stretch;
	}
	
	/**
	 * Return true if image should be stretched
	 * @return
	 */
	public boolean isStretch() {
		return stretch;
	}
	
	/**
	 * Set printed page to a black and white page with optional dithering.
	 * The default for monochrome printing is gray scale.
	 * @param printBlackWhite
	 * @param dither
	 */
	public void setPureBlackWhite(boolean printBlackWhite, boolean dither) {
		pureBlackWhite = printBlackWhite;
		ditherBlackWhite = dither;
	}

	/**
	 * Get current page number
	 * @return
	 */
	public int getCurrentPage() {
		return currentPage;
	}
	
	/**
	 * Determine if job has completed
	 * @return
	 */
	public boolean isPrintJobDone() {
		return printJobDone;
	}
	
	/**
	 * Cleanup resources
	 */
	private void dispose() {            
       	document.dispose();
       	printJobDone = true;
	}
	
    /**
     * Get the proper printer service, if not found
	 * use the default printer.
     * @param printer
     * @return
     */
	private PrintService getPrinterService(String printer) {
		PrintService[] printServices = PrinterJob.lookupPrintServices();
		PrintService printService = null;
    	int i;
    	
    	if (printer.trim().length() == 0) {
    		printer = getDefaultPrinter();
    	}
    	
    	for (i = 0; i < printServices.length; i++) {
    		if (printServices[i].getName().trim().toLowerCase().equals(printer.trim().toLowerCase())) {
    			printService = printServices[i];
    			break;
    		}
    	}
    	
    	if (printService == null) {
    		printService = PrintServiceLookup.lookupDefaultPrintService();
    	}
    	
    	return printService;
	}
	
	/**
	 * Get the default printer
	 */
	private String getDefaultPrinter() {	
		return PrintServiceLookup.lookupDefaultPrintService().getName();
	}
	
	// ----------------------------------------------------
	// Runnable
	// ----------------------------------------------------

	
	/**
	 * Run print job
	 */
	public void run() {
    	try {
			job.print(simpleDoc, printAttributeSet);
			waitForPrintJobDone();
		} catch (PrintException e) {
		} finally {
			dispose();
		}	
	}
	
	
	// ----------------------------------------------------
	// PrintJobListener
	// ----------------------------------------------------
	
	
	public void printDataTransferCompleted(PrintJobEvent pje) {}
	public void printJobRequiresAttention(PrintJobEvent pje) {}
	
	public void printJobCompleted(PrintJobEvent pje) {
		printJobDone();
	}

	public void printJobFailed(PrintJobEvent pje) {
		printJobDone();
	}

	public void printJobCanceled(PrintJobEvent pje) {
		printJobDone();		
	}

	public void printJobNoMoreEvents(PrintJobEvent pje) {
		printJobDone();
	}

	private void printJobDone() {
		synchronized (this) {
			printJobDone = true;
			notify();
		}
	}

    public synchronized void waitForPrintJobDone() {
        try {
            while (!printJobDone) {
                wait();
            }
        } catch (InterruptedException e) {}
    }
	
}

