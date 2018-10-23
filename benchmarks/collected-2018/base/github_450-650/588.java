// https://searchcode.com/api/result/111498036/

///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile: PDF.java,v $
//  Purpose:  Reader/Writer for Undefined files.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg Kurt Wegner
//  Version:  $Revision: 1.8 $
//            $Date: 2005/02/17 16:48:34 $
//            $Author: wegner $
//
// Copyright OELIB:          OpenEye Scientific Software, Santa Fe,
//                           U.S.A., 1999,2000,2001
// Copyright JOELIB/JOELib2: Dept. Computer Architecture, University of
//                           Tuebingen, Germany, 2001,2002,2003,2004,2005
// Copyright JOELIB/JOELib2: ALTANA PHARMA AG, Konstanz, Germany,
//                           2003,2004,2005
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
///////////////////////////////////////////////////////////////////////////////
package joelib2.io.types;

import joelib2.gui.render2D.Mol2Image;
import joelib2.gui.render2D.RenderHelper;
import joelib2.gui.render2D.Renderer2D;
import joelib2.gui.render2D.RenderingAtoms;

import joelib2.io.MoleculeFileIO;
import joelib2.io.MoleculeIOException;
import joelib2.io.PropertyWriter;

import joelib2.molecule.Molecule;

import joelib2.molecule.types.PairData;

import joelib2.smarts.SMARTSPatternMatcher;

import joelib2.util.iterator.PairDataIterator;

import wsi.ra.tool.BasicPropertyHolder;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;


/**
 * Writer for Portable Adobe Document Format (PDF) format with native descriptors (scalars).
 *
 * @.author     wegnerj
 * @.wikipedia  Portable Document Format
 * @.wikipedia  File format
 * @.license GPL
 * @.cvsversion    $Revision: 1.8 $, $Date: 2005/02/17 16:48:34 $
 */
public class PDF implements MoleculeFileIO, PropertyWriter
{
    //~ Static fields/initializers /////////////////////////////////////////////

    /**
     *  Obtain a suitable logger.
     */
    private static Category logger = Category.getInstance(
            "joelib2.io.types.PDF");
    private static final String description =
        "Portable Adobe Document Format (PDF) image";
    private static final String[] extensions = new String[]{"pdf"};
    private static final int DEFAULT_FONT_SIZE = 10;
    private static final int DEFAULT_FONT_OFFSET = 2;
    private static final int DEFAULT_BORDER = 20;
    private static final int WRITE_MAX_CHARACTERS = 200;

    //~ Instance fields ////////////////////////////////////////////////////////

    private Document document = new Document();
    private boolean firstMoleculeWritten = false;
    private int fontSize = DEFAULT_FONT_SIZE;
    private int fontSizeDelta = DEFAULT_FONT_OFFSET;
    private int pageBorder = DEFAULT_BORDER;
    private PdfWriter writer;

    //~ Methods ////////////////////////////////////////////////////////////////

    /**
     *  Description of the Method
     *
     * @exception  IOException  Description of the Exception
     */
    public void closeReader() throws IOException
    {
    }

    /**
     *  Description of the Method
     *
     * @exception  IOException  Description of the Exception
     */
    public void closeWriter() throws IOException
    {
        document.close();
    }

    public void initReader(InputStream is) throws IOException
    {
    }

    /**
     *  Description of the Method
     *
     * @param  os               Description of the Parameter
     * @exception  IOException  Description of the Exception
     */
    public void initWriter(OutputStream os) throws IOException
    {
        fontSize = BasicPropertyHolder.instance().getInt(this, "fontSize",
                DEFAULT_FONT_SIZE);
        fontSizeDelta = BasicPropertyHolder.instance().getInt(this,
                "fontOffset", DEFAULT_FONT_SIZE);
        pageBorder = BasicPropertyHolder.instance().getInt(this, "pageBorder",
                DEFAULT_FONT_SIZE);

        try
        {
            writer = PdfWriter.getInstance(document, os);
        }
        catch (DocumentException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public String inputDescription()
    {
        return null;
    }

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public String[] inputFileExtensions()
    {
        return null;
    }

    public String outputDescription()
    {
        return description;
    }

    public String[] outputFileExtensions()
    {
        return extensions;
    }

    /**
     *  Reads an molecule entry as (unparsed) <tt>String</tt> representation.
     *
     * @return                  <tt>null</tt> if the reader contains no more
     *      relevant data. Otherwise the <tt>String</tt> representation of the
     *      whole molecule entry is returned.
     * @exception  IOException  typical IOException
     */
    public String read() throws IOException
    {
        logger.error(
            "Reading PDF data as String representation is not implemented yet !!!");

        return null;
    }

    /**
     *  Description of the Method
     *
     * @param  mol                      Description of the Parameter
     * @return                          Description of the Return Value
     * @exception  IOException          Description of the Exception
     * @exception  MoleculeIOException  Description of the Exception
     */
    public synchronized boolean read(Molecule mol) throws IOException,
        MoleculeIOException
    {
        return read(mol, null);
    }

    /**
     *  Loads an molecule in MDL SD-MOL format and sets the title. If <tt>title
     *  </tt> is <tt>null</tt> the title line in the molecule file is used.
     *
     * @param  mol                      Description of the Parameter
     * @param  title                    Description of the Parameter
     * @return                          Description of the Return Value
     * @exception  IOException          Description of the Exception
     * @exception  MoleculeIOException  Description of the Exception
     */
    public synchronized boolean read(Molecule mol, String title)
        throws IOException, MoleculeIOException
    {
        return (true);
    }

    public boolean readable()
    {
        return false;
    }

    /**
     *  Description of the Method
     *
     * @return                  Description of the Return Value
     * @exception  IOException  Description of the Exception
     */
    public boolean skipReaderEntry() throws IOException
    {
        return true;
    }

    /**
     *  Description of the Method
     *
     * @param  mol              Description of the Parameter
     * @return                  Description of the Return Value
     * @exception  IOException  Description of the Exception
     */
    public boolean write(Molecule mol) throws IOException
    {
        return write(mol, null);
    }

    public boolean write(SMARTSPatternMatcher smarts, Molecule mol)
        throws IOException
    {
        return write(mol, null, true, null, smarts);
    }

    /**
     *  Description of the Method
     *
     * @param  mol              Description of the Parameter
     * @param  title            Description of the Parameter
     * @return                  Description of the Return Value
     * @exception  IOException  Description of the Exception
     */
    public boolean write(Molecule mol, String title) throws IOException
    {
        return write(mol, title, true, null);
    }

    /**
     *  Writes a molecule with his <tt>PairData</tt> .
     *
     * @param  mol              the molecule with additional data
     * @param  title            the molecule title or <tt>null</tt> if the title
     *      from the molecule should be used
     * @param  writePairData    if <tt>true</tt> then the additional molecule data
     *      is written
     * @param  attribs2write    Description of the Parameter
     * @return                  <tt>true</tt> if the molecule and the data has
     *      been succesfully written.
     * @exception  IOException  Description of the Exception
     */
    public boolean write(Molecule mol, String title, boolean writePairData,
        List attribs2write) throws IOException
    {
        return write(mol, title, writePairData, attribs2write, null);
    }

    /**
     *  Writes a molecule with his <tt>PairData</tt> .
     *
     * @param  mol              the molecule with additional data
     * @param  title            the molecule title or <tt>null</tt> if the title
     *      from the molecule should be used
     * @param  writePairData    if <tt>true</tt> then the additional molecule data
     *      is written
     * @param  attribs2write    Description of the Parameter
     * @return                  <tt>true</tt> if the molecule and the data has
     *      been succesfully written.
     * @exception  IOException  Description of the Exception
     */
    public boolean write(Molecule mol, String title, boolean writePairData,
        List attribs2write, SMARTSPatternMatcher smarts) throws IOException
    {
        if (firstMoleculeWritten == false)
        {
            document.open();
            firstMoleculeWritten = true;
        }

        Dimension d = new Dimension(Mol2Image.instance().getDefaultWidth(),
                Mol2Image.instance().getDefaultHeight());
        RenderingAtoms container = new RenderingAtoms();
        container.add(mol);

        RenderHelper.translateAllPositive(container);
        RenderHelper.scaleMolecule(container, d, 0.8);
        RenderHelper.center(container, d);

        Renderer2D renderer = new Renderer2D();

        //BaseFont helvetica = null;
        try
        {
            BaseFont.createFont("Helvetica", BaseFont.CP1252,
                BaseFont.NOT_EMBEDDED);
        }
        catch (DocumentException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        int w = d.width;
        int h = d.height;
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate(w, h);
        Graphics2D g2 = tp.createGraphics(w, h);
        g2.setStroke(new BasicStroke(0.1f));
        tp.setWidth(w);
        tp.setHeight(h);

        g2.setColor(renderer.getRenderer2DModel().getBackColor());
        g2.fillRect(0, 0, d.width, d.height);

        if (smarts != null)
        {
            renderer.selectSMARTSPatterns(container, smarts);
        }

        renderer.paintMolecule(container, g2);

        g2.dispose();

        ////cb.addTemplate(tp, 72, 720 - h);
        //cb.addTemplate(tp, 12, 720 - h);
        cb.addTemplate(tp, 0, document.getPageSize().height() - h);

        //     Mol2Image.instance().mol2image(mol);
        BaseFont bf = null;

        try
        {
            bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252,
                    BaseFont.NOT_EMBEDDED);
        }
        catch (DocumentException e2)
        {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        String string = "";

        //float myBorder = DEFAULT_BORDER;
        //float fontSize = 10;
        //float fontSizeDelta = DEFAULT_FONT_OFFSET;
        float hpos;

        if (writePairData)
        {
            PairData pairData;
            PairDataIterator gdit = mol.genericDataIterator();
            int index = 0;
            boolean firstPageWritten = false;

            List attributesV;

            if (attribs2write == null)
            {
                // write all descriptors
                attributesV = new Vector();

                //DescResult tmpPropResult;
                while (gdit.hasNext())
                {
                    pairData = gdit.nextPairData();

                    attributesV.add(pairData.getKey());
                }
            }
            else
            {
                attributesV = attribs2write;
            }

            // sort descriptors by attribute name
            String[] attributes = new String[attributesV.size()];

            for (int i = 0; i < attributesV.size(); i++)
            {
                attributes[i] = (String) attributesV.get(i);
            }

            Arrays.sort(attributes);

            // write them
            for (int i = 0; i < attributes.length; i++)
            {
                pairData = mol.getData(attributes[i]);
                string = pairData.getKey() + " = " + pairData.toString();

                // reduce too complex data
                string = string.replace('\n', ' ');
                string = string.substring(0,
                        Math.min(string.length(), WRITE_MAX_CHARACTERS));

                tp = cb.createTemplate(document.getPageSize().width() -
                        pageBorder, fontSize + fontSizeDelta);
                tp.setFontAndSize(bf, fontSize);
                tp.beginText();
                tp.setTextMatrix(0, fontSizeDelta);
                tp.showText(string);
                tp.endText();
                cb.setLineWidth(1f);
                tp.moveTo(0, 0);
                tp.lineTo(document.getPageSize().width() - (2 * pageBorder), 0);
                tp.stroke();

                if (firstPageWritten)
                {
                    hpos = document.getPageSize().height() -
                        ((fontSize + fontSizeDelta) * (index + 1));
                }
                else
                {
                    hpos = document.getPageSize().height() - h -
                        ((fontSize + fontSizeDelta) * (index + 1));
                }

                if (hpos < pageBorder)
                {
                    index = 1;
                    firstPageWritten = true;
                    hpos = document.getPageSize().height() -
                        ((fontSize + fontSizeDelta) * (index + 1));

                    try
                    {
                        document.newPage();
                    }
                    catch (DocumentException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                cb.addTemplate(tp, pageBorder, hpos);

                index++;
            }
        }

        try
        {
            document.newPage();
        }
        catch (DocumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return (true);
    }

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public boolean writeable()
    {
        return true;
    }
}

///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

