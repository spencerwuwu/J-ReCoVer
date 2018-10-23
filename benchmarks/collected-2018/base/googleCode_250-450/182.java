// https://searchcode.com/api/result/13210455/

/* ======================================
 * JFreeChart : a free Java chart library
 * ======================================
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 * Project Lead:  David Gilbert (david.gilbert@object-refinery.com);
 *
 * (C) Copyright 2000-2003, by Object Refinery Limited and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * ------------------
 * XYBarRenderer.java
 * ------------------
 * (C) Copyright 2001-2003, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   Richard Atkinson;
 *                   Christian W. Zuckschwerdt;
 *                   Bill Kelemen;
 *
 * $Id: XYBarRenderer.java,v 1.8 2003/09/17 09:19:31 mungady Exp $
 *
 * Changes
 * -------
 * 13-Dec-2001 : Version 1, makes VerticalXYBarPlot class redundant (DG);
 * 23-Jan-2002 : Added DrawInfo parameter to drawItem(...) method (DG);
 * 09-Apr-2002 : Removed the translated zero from the drawItem method.  Override the initialise()
 *               method to calculate it (DG);
 * 24-May-2002 : Incorporated tooltips into chart entities (DG);
 * 25-Jun-2002 : Removed redundant import (DG);
 * 05-Aug-2002 : Small modification to drawItem method to support URLs for HTML image maps (RA);
 * 25-Mar-2003 : Implemented Serializable (DG);
 * 01-May-2003 : Modified drawItem(...) method signature (DG);
 * 30-Jul-2003 : Modified entity constructor (CZ);
 * 20-Aug-2003 : Implemented Cloneable and PublicCloneable (DG);
 * 24-Aug-2003 : Added null checks in drawItem (BK);
 * 16-Sep-2003 : Changed ChartRenderingInfo --> PlotRenderingInfo (DG);
 *
 */

package org.jfree.chart.renderer;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.jfree.chart.CrosshairInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.IntervalXYDataset;
import org.jfree.data.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.PublicCloneable;

/**
 * A renderer that draws bars on an {@link XYPlot} (requires an {@link IntervalXYDataset}).
 *
 * @author David Gilbert
 */
public class XYBarRenderer extends AbstractXYItemRenderer implements XYItemRenderer,
                                                                     Cloneable,
                                                                     PublicCloneable,
                                                                     Serializable {

    /** Percentage margin (to reduce the width of bars). */
    private double margin;

    /** A data value of zero translated to a Java2D value. */
    private double translatedRangeZero;

    /**
     * The default constructor.
     */
    public XYBarRenderer() {
        super();
        this.margin = 0.0;
    }

    /**
     * Constructs a new renderer.
     *
     * @param margin  the percentage amount to trim from the width of each bar.
     *
     */
    public XYBarRenderer(double margin) {
        super();
        this.margin = margin;
    }

    /**
     * Constructs a new renderer.
     *
     * @param margin  the percentage amount to trim from the width of each bar.
     * @param toolTipGenerator  the tool tip generator (<code>null</code> permitted).
     * @param urlGenerator  the URL generator (<code>null</code> permitted).
     *
     * @deprecated Use default constructor then set tooltip generator and URL generator.
     */
    public XYBarRenderer(double margin,
                         XYToolTipGenerator toolTipGenerator,
                         XYURLGenerator urlGenerator) {

        super(toolTipGenerator, urlGenerator);
        this.margin = margin;

    }

    /**
     * Sets the percentage amount by which the bars are trimmed.
     * <P>
     * Fires a property change event.
     *
     * @param margin  the new margin.
     */
    public void setMargin(double margin) {

        Double old = new Double(this.margin);
        this.margin = margin;
        firePropertyChanged("XYBarRenderer.margin", old, new Double(margin));

    }

    /**
     * Initialises the renderer.  Here we calculate the Java2D y-coordinate for zero, since all
     * the bars have their bases fixed at zero.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area inside the axes.
     * @param plot  the plot.
     * @param data  the data.
     * @param info  an optional info collection object to return data back to the caller.
     *
     * @return The number of dataset passes required by the renderer.
     */
    public int initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data,
                          PlotRenderingInfo info) {

        super.initialise(g2, dataArea, plot, data, info);
        ValueAxis rangeAxis = plot.getRangeAxis();
        this.translatedRangeZero = rangeAxis.translateValueToJava2D(0.0, dataArea,
                                                                    plot.getRangeAxisEdge());
        return 1;

    }

    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area within which the plot is being drawn.
     * @param info  collects information about the drawing.
     * @param plot  the plot (can be used to obtain standard color information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param crosshairInfo  collects information about crosshairs.
     * @param pass  the pass index.
     */
    public void drawItem(Graphics2D g2,
                         Rectangle2D dataArea,
                         PlotRenderingInfo info,
                         XYPlot plot, 
                         ValueAxis domainAxis, 
                         ValueAxis rangeAxis,
                         XYDataset dataset, 
                         int series, 
                         int item,
                         CrosshairInfo crosshairInfo,
                         int pass) {

        IntervalXYDataset intervalData = (IntervalXYDataset) dataset;

        Paint seriesPaint = getItemPaint(series, item);
        Paint seriesOutlinePaint = getSeriesOutlinePaint(series);

        Number valueNumber = intervalData.getYValue(series, item);
        if (valueNumber == null) {
            return;
        }

        double translatedValue = rangeAxis.translateValueToJava2D(valueNumber.doubleValue(),
                                                             dataArea, plot.getRangeAxisEdge());

        RectangleEdge location = plot.getDomainAxisEdge();
        Number startXNumber = intervalData.getStartXValue(series, item);
        if (startXNumber == null) {
            return;
        }
        double translatedStartX = domainAxis.translateValueToJava2D(startXNumber.doubleValue(),
                                                                    dataArea, location);

        Number endXNumber = intervalData.getEndXValue(series, item);
        if (endXNumber == null) {
            return;
        }
        double translatedEndX = domainAxis.translateValueToJava2D(endXNumber.doubleValue(),
                                                                  dataArea, location);

        double translatedWidth = Math.max(1, Math.abs(translatedEndX - translatedStartX));
        double translatedHeight = Math.abs(translatedValue - translatedRangeZero);

        if (margin > 0.0) {
            double cut = translatedWidth * margin;
            translatedWidth = translatedWidth - cut;
            translatedStartX = translatedStartX + cut / 2;
        }

        Rectangle2D bar = null;
        PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {

            bar = new Rectangle2D.Double(Math.min(this.translatedRangeZero, translatedValue),
                                         translatedEndX,
                                         translatedHeight, translatedWidth);
        }
        else if (orientation == PlotOrientation.VERTICAL) {

            bar = new Rectangle2D.Double(translatedStartX,
                                         Math.min(this.translatedRangeZero, translatedValue),
                                         translatedWidth, translatedHeight);

        }

        g2.setPaint(seriesPaint);
        g2.fill(bar);
        if (Math.abs(translatedEndX - translatedStartX) > 3) {
            g2.setStroke(getItemStroke(series, item));
            g2.setPaint(seriesOutlinePaint);
            g2.draw(bar);
        }

        // add an entity for the item...
        if (info != null) {
            EntityCollection entities = info.getOwner().getEntityCollection();
            if (entities != null) {
                String tip = null;
                if (getToolTipGenerator() != null) {
                    tip = getToolTipGenerator().generateToolTip(dataset, series, item);
                }
                String url = null;
                if (getURLGenerator() != null) {
                    url = getURLGenerator().generateURL(dataset, series, item);
                }
                XYItemEntity entity = new XYItemEntity(bar, dataset, series, item, tip, url);
                entities.addEntity(entity);
            }
        }

    }

    /**
     * Returns a clone of the renderer.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException  if the renderer cannot be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}

