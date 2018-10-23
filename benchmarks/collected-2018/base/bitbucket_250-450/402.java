// https://searchcode.com/api/result/53644498/

/*
 *  CoreDesigner
 *  Copyright (C) 2007-2009 Christian Lins <christian.lins@web.de>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.coredesigner.print;

import org.coredesigner.Config;
import org.coredesigner.Log;
import org.coredesigner.gui.FormView;
import org.coredesigner.gui.MainFrame;
import org.coredesigner.gui.PageView;
import org.coredesigner.gui.edit.CanvasTextEditor;
import org.coredesigner.io.PDFExporter;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Compression;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import javax.swing.RepaintManager;
import org.coredesigner.io.PostScriptInputStream;

public class PrintDaemon
{

  public static int   PRINTER_DPI = 72;
  public static int   SCREEN_DPI  = 96;
  public static final float INCH      = 2.54f;
  public static final float A4_WIDTH  = 21.0f;
  public static final float A4_HEIGHT = 29.7f;
  
  private static PrintDaemon instance = new PrintDaemon();
  
  public static PrintDaemon inst()
  {
    return instance;
  }
  
  public static PrintRequestAttributeSet getDefaultPrintRequestAttributeSet()
  {
    PrintRequestAttributeSet prattrs = new HashPrintRequestAttributeSet();
    prattrs.add(MediaSizeName.ISO_A4);
    //prattrs.add(new PrinterResolution(PRINTER_DPI, PRINTER_DPI, PrinterResolution.DPI));
    return prattrs;
  }
  
  private Book        book        = new Book();
  private boolean     isPrinting  = false;
  private PageFormat  pageFormat  = new PageFormat();
  private DocPrintJob printJob    = null;
  private Map<Object, BufferedImage> pageCache = new HashMap<Object, BufferedImage>();

  private PrintDaemon()
  {
    Paper paper = pageFormat.getPaper();
    paper.setSize(A4_WIDTH / INCH * PRINTER_DPI, A4_HEIGHT / INCH * PRINTER_DPI);
    paper.setImageableArea(0.5 / INCH * PRINTER_DPI, 0.5 / INCH * PRINTER_DPI, (A4_WIDTH - 1.0) / INCH * PRINTER_DPI, (A4_HEIGHT - 1.0) / INCH * PRINTER_DPI);
    pageFormat.setPaper(paper);
  }

  /**
   * Renders to image. Does not enable and disable printing.
   * @param pv
   * @return
   * @throws java.lang.Exception
   */
  public static BufferedImage printToImage(PageView pv)
    throws Exception
  {
    BufferedImage img = new BufferedImage(
        (int)(A4_WIDTH / INCH * PRINTER_DPI),
        (int)(A4_HEIGHT / INCH * PRINTER_DPI), BufferedImage.TYPE_INT_RGB);

    Graphics2D g2 = img.createGraphics();
    pv.print(g2, PrintDaemon.inst().getPageFormat(), 0);

    return img;
  }

  /**
   * Prints a full page image to the printer.
   * @param img
   * @param attrib
   */
  public void printImage(BufferedImage img, PrintRequestAttributeSet attrib)
    throws PrintException
  {
    String name;
    PrinterName pname = (PrinterName)attrib.get(PrinterName.class);
    if(pname != null)
      name = pname.getName();
    else
      name = Config.inst().get(Config.PRINTER_SERVICE, PrintDaemon.getDefaultPrintService().getName());

    DocPrintJob printerJob = getPrintJob(name);
    SimpleDoc simpleDoc = new SimpleDoc(new ImagePrinter(img), DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
    printerJob.print(simpleDoc, attrib);
  }

  /**
   * Prints the given form to a memory PostScript document and sends it to
   * the printer. It has to be determined if this works with every printing
   * system.
   * @param fv
   */
  public static synchronized void printPS(FormView fv, PrintService service,
    PrintRequestAttributeSet aset)
    throws PrintException, UnsupportedEncodingException
  {
    // Create the PostScript input stream which actually renders the
    // pages to PostScript. This may take a second.
    PostScriptInputStream psStream = new PostScriptInputStream(fv);

    DocFlavor psInFormat = DocFlavor.INPUT_STREAM.POSTSCRIPT;
    Doc doc = new SimpleDoc(psStream, psInFormat, null);

    if(aset == null)
    {
      aset = getDefaultPrintRequestAttributeSet();
    }

    DocPrintJob docPrintJob = service.createPrintJob();
    docPrintJob.print(doc, aset);
  }

  public static File printToPDF(FormView fv)
    throws Exception
  {
    File file = File.createTempFile("fd2008print", ".pdf");
    //PDFExporter.export(fv, file);
    file.deleteOnExit();
    return file;
  }

  /**
   * 
   * @param page
   */
  public void addPage(Printable page)
  {
    this.book.append(page, pageFormat);
  }

  /**
   * Starts a new print job.
   * @param printerName
   * @return
   */
  public boolean beginPrinting(String printerName)
  {
    // We must finish editing before printing
    if(CanvasTextEditor.getInstance() != null)
      CanvasTextEditor.getInstance().finishEditing();
    
    // Disable double-buffering for better images
    RepaintManager.currentManager(MainFrame.getInstance()).setDoubleBufferingEnabled(false);

    // Begin printing
    isPrinting = true;

    // Book contains several pages (Printable). We collect all
    // Printables in one print job, to reduce pressure from printing
    // spooler when printing several copies.
    this.book  = new Book();
    if(printerName != null)
    {
      this.printJob = getPrintJob(printerName);
      if(this.printJob == null)
        return false;
    }
    return true;
  }

  /**
   * Stops building our printing job and flushes the Book instance to
   * the printer.
   * @throws javax.print.PrintException
   */
  public void finishPrinting(PrintRequestAttributeSet printRequestAttributes)
    throws PrintException
  {
    if(printJob != null)
    {
      // Print the book
      HashDocAttributeSet docAttrib = new HashDocAttributeSet();
      docAttrib.add(PrintQuality.NORMAL);
      docAttrib.add(Compression.DEFLATE);
      SimpleDoc doc = new SimpleDoc(this.book, DocFlavor.SERVICE_FORMATTED.PAGEABLE, docAttrib);
      this.printJob.print(doc, printRequestAttributes);
    }
    
    // Enable double-buffering for better screen images
    RepaintManager.currentManager(MainFrame.getInstance()).setDoubleBufferingEnabled(true);
    
    // Disable printing
    this.isPrinting = false;

    // Clear the cache
    this.pageCache.clear();
  }
  
  public boolean isPrinting()
  {
    return isPrinting;
  }
  
  public PageFormat getPageFormat()
  {
    return pageFormat;
  }

  public static DocPrintJob getPrintJob(String name)
  {
    return getPrintService(name).createPrintJob();
  }

  public static PrintService getPrintService(String name)
  {
    PrintServiceAttributeSet aset = new HashPrintServiceAttributeSet();
    aset.add(new PrinterName(name, null));
    PrintService[] pservices 
      = PrintServiceLookup.lookupPrintServices(
        DocFlavor.INPUT_STREAM.POSTSCRIPT, aset);
    
    if(pservices.length > 0)
    {
      return pservices[0];
    }
    else
    {
      pservices = PrintServiceLookup.lookupPrintServices(
        DocFlavor.INPUT_STREAM.PNG, aset);

      if(pservices.length > 0)
      {
        return pservices[0];
      }
      else
      {
        Log.get().info("Printer " + name + " could not be found. Using default.");
        return PrintServiceLookup.lookupDefaultPrintService();
      }
    }
  }
  
  /**
   * @return An array of all PrintServices available on this platform.
   */
  public static PrintService[] getPrintServices(DocFlavor flavor)
  {
    PrintServiceAttributeSet aset = new HashPrintServiceAttributeSet();
    return PrintServiceLookup.lookupPrintServices(flavor, aset);
  }
  
  public static PrintService getDefaultPrintService()
  {
    return PrintServiceLookup.lookupDefaultPrintService();
  }
  
  public void setPageFormat(PageFormat pageFormat)
  {
    this.pageFormat = pageFormat;
  }

  public float getMarginEast()
  {
    return (float)
      (pageFormat.getWidth() - pageFormat.getImageableWidth() - pageFormat.getImageableX())
        * INCH / PRINTER_DPI;
  }

  public float getMarginNorth()
  {
    return (float)this.pageFormat.getImageableY() * INCH / PRINTER_DPI;
  }

  public float getMarginSouth()
  {
    return (float)
      (pageFormat.getHeight() - pageFormat.getImageableHeight() - pageFormat.getImageableY())
        * INCH / PRINTER_DPI;
  }

  public float getMarginWest()
  {
    return (float)this.pageFormat.getImageableX() * INCH / PRINTER_DPI;
  }
  
  public void setMarginWest(float margin)
  {
    Paper paper = this.pageFormat.getPaper();
    
    float newX = margin / INCH * PRINTER_DPI;
    float newW = (A4_WIDTH - margin - getMarginEast()) / INCH * PRINTER_DPI;
    
    paper.setImageableArea(newX, paper.getImageableY(), newW, paper.getImageableHeight());
    
    this.pageFormat.setPaper(paper);
  }
  
  public void setMarginNorth(float margin)
  {
    Paper paper = this.pageFormat.getPaper();
    
    float newY = margin / INCH * PRINTER_DPI;
    float newH = (A4_HEIGHT - margin - getMarginSouth()) / INCH * PRINTER_DPI;
    
    paper.setImageableArea(paper.getImageableX(), newY, paper.getImageableWidth(), newH);
    
    this.pageFormat.setPaper(paper);   
  }
  
  public void setMarginSouth(float margin)
  {
    Paper paper = this.pageFormat.getPaper();
    
    paper.setImageableArea(
        paper.getImageableX(), 
        paper.getImageableY(), 
        paper.getImageableWidth(), 
        (A4_HEIGHT - margin - getMarginNorth()) / INCH * PRINTER_DPI);
    
    this.pageFormat.setPaper(paper);
  }
  
  public void setMarginEast(float margin)
  {
    Paper paper = this.pageFormat.getPaper();
    
    paper.setImageableArea(
        paper.getImageableX(), 
        paper.getImageableY(), 
        (A4_WIDTH - margin - getMarginWest()) / INCH * PRINTER_DPI, 
        paper.getImageableHeight());
    
    this.pageFormat.setPaper(paper);    
  }
}

