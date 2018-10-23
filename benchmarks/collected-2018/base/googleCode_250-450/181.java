// https://searchcode.com/api/result/13210441/

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
 * ---------------------------
 * ClusteredXYBarRenderer.java
 * ---------------------------
 * (C) Copyright 2003, by Paolo Cova and Contributors.
 *
 * Original Author:  Paolo Cova;
 * Contributor(s):   David Gilbert (for Object Refinery Limited);
 *                   Christian W. Zuckschwerdt;
 *
 * $Id: ClusteredXYBarRenderer.java,v 1.13 2003/09/24 10:27:28 mungady Exp $
 *
 * Changes
 * -------
 * 24-Jan-2003 : Version 1, contributed by Paolo Cova (DG);
 * 25-Mar-2003 : Implemented Serializable (DG);
 * 01-May-2003 : Modified drawItem(...) method signature (DG);
 * 30-Jul-2003 : Modified entity constructor (CZ);
 * 20-Aug-2003 : Implemented Cloneable and PublicCloneable (DG);
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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.IntervalXYDataset;
import org.jfree.data.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.PublicCloneable;

/**
 * An extension of {@link XYBarRenderer} that displays bars for different
 * series values at the same x next to each other. The assumption here is
 * that for each x (time or else) there is a y value for each series. If
 * this is not the case, there will be spaces between bars for a given x.
 *
 * @author Paolo Cova
 */
public class ClusteredXYBarRenderer extends XYBarRenderer implements Cloneable,
                                                                     PublicCloneable,
                                                                     Serializable {

    /** Percentage margin (to reduce the width of bars). */
    private double margin;

    /** A data value of zero translated to a Java2D value. */
    private double translatedRangeZero;

    /** Determines whether bar center should be interval start. */
    private boolean centerBarAtStartValue;

    /**
     * Default constructor. Bar margin is set to 0.0.
    */
    public ClusteredXYBarRenderer() {
        this(0.0, false);
    }

    /**
    * Constructs a new XY clustered bar renderer.
    *
    * @param margin the percentage amount to trim from the width of each bar.
    * @param centerBarAtStartValue If true, bars will be centered on the start of the time period.
    */
    public ClusteredXYBarRenderer(double margin, boolean centerBarAtStartValue) {
        super(margin);
        this.margin = margin;
        this.centerBarAtStartValue = centerBarAtStartValue;
    }

    /**
    * Initialises the renderer. Here we calculate the Java2D y-coordinate for zero, since all
    * the bars have their bases fixed at zero. Copied from superclass to
    * initialize local variables.
    *
    * @param g2 the graphics device.
    * @param dataArea the area inside the axes.
    * @param plot the plot.
    * @param data the data.
    * @param info an optional info collection object to return data back to the caller.
    *
    * @return The number of passes required by the renderer.
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
     * Sets the margin.
     *
     * @param margin  the margin.
     */
    public void setMargin(double margin) {
        this.margin = margin;
        super.setMargin(margin);
    }

    /**
     * Draws the visual representation of a single data item. This method
     * is mostly copied from the superclass, the change is that in the
     * calculated space for a singe bar we draw bars for each series next to
     * each other. The width of each bar is the available width divided by
     * the number of series. Bars for each series are drawn in order left to
     * right.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area within which the plot is being drawn.
     * @param info  collects information about the drawing.
     * @param plot  the plot (can be used to obtain standard color information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param series  the series index.
     * @param item  the item index.
     * @param crosshairInfo  collects information about crosshairs.
     * @param pass  the pass index.
     */
    public void drawItem(Graphics2D g2,
                         Rectangle2D dataArea,
                         PlotRenderingInfo info,
                         XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
                         XYDataset dataset, int series, int item,
                         CrosshairInfo crosshairInfo,
                         int pass) {

        IntervalXYDataset intervalData = (IntervalXYDataset) dataset;

        Paint seriesPaint = getItemPaint(series, item);
        Paint seriesOutlinePaint = getItemOutlinePaint(series, item);

        double y = intervalData.getYValue(series, item).doubleValue();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
        double translatedY = rangeAxis.translateValueToJava2D(y, dataArea, yAxisLocation);

        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        double x1 = intervalData.getStartXValue(series, item).doubleValue();
        double translatedX1 = domainAxis.translateValueToJava2D(x1, dataArea, xAxisLocation);

        double x2 = intervalData.getEndXValue(series, item).doubleValue();
        double translatedX2 = domainAxis.translateValueToJava2D(x2, dataArea, xAxisLocation);

        double translatedWidth = Math.max(1, Math.abs(translatedX2 - translatedX1));
        double translatedHeight = Math.abs(translatedY - translatedRangeZero);

        if (centerBarAtStartValue) {
            translatedX1 -= translatedWidth / 2;
        }

        if (margin > 0.0) {
            double cut = translatedWidth * margin;
            translatedWidth = translatedWidth - cut;
            translatedX1 = translatedX1 + cut / 2;
        }

        int numSeries = dataset.getSeriesCount();
        double seriesBarWidth = translatedWidth / numSeries;

        Rectangle2D bar = null;
        PlotOrientation orientation = plot.getOrientation();        
        if (orientation == PlotOrientation.HORIZONTAL) {
            bar = new Rectangle2D.Double(Math.min(this.translatedRangeZero, translatedY),
                                         translatedX1 - seriesBarWidth * (numSeries - series),
                                         translatedHeight, seriesBarWidth);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
        
            bar = new Rectangle2D.Double(translatedX1 + seriesBarWidth * series,
                                         Math.min(this.translatedRangeZero, translatedY),
                                         seriesBarWidth, translatedHeight);

        }
        g2.setPaint(seriesPaint);
        g2.fill(bar);
        if (Math.abs(translatedX2 - translatedX1) > 3) {
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


