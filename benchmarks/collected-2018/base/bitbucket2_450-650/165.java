// https://searchcode.com/api/result/47125032/

/*
 * --------- BEGIN COPYRIGHT NOTICE ---------
 * Copyright 2002-2012 Extentech Inc.
 * Copyright 2013 Infoteria America Corp.
 * 
 * This file is part of OpenXLS.
 * 
 * OpenXLS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * OpenXLS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with OpenXLS.  If not, see
 * <http://www.gnu.org/licenses/>.
 * ---------- END COPYRIGHT NOTICE ----------
 */
package com.extentech.formats.XLS.charts;

import java.util.Arrays;
import java.util.HashMap;

import com.extentech.formats.XLS.Font;
import com.extentech.formats.XLS.FormatConstants;
import com.extentech.formats.XLS.MSODrawing;
import com.extentech.formats.XLS.XLSRecord;
import com.extentech.toolkit.ByteTools;
import com.extentech.toolkit.Logger;
import com.extentech.toolkit.StringTool;
/**
 * <b>Legend: Legend Type and Position (0x1015)</b>
 *  
 * 
 * 4	x		4		x position of upper-left corner -- MUST be ignored and the x1 field from the following Pos record MUST be used instead. 
 * 8	y		4		y position of upper-left corner -- MUST be ignored and the y1 field from the following Pos record MUST be used instead.
 * 12	dx		4		width in SPRC -- MUST be ignored and the x2 field from the following Pos record MUST be used instead.
 * 16	dy		4		height in SPRC -- MUST be ignored and the y2 field from the following Pos record MUST be used instead.
 * 20			1       Undefined and MUST be ignored.
 * 21	wSpacing1		Spacing (0= close, 1= medium, 2= open) (0x1= 40 twips==4 pts)
 * 22	grbit	2		Option Flags
 * 							
 * grbit Option Flags
 * bits		Mask
 *	0		01h		fAutoPostion	Automatic positioning (1= legend is docked)
 *  1		02h		fAutoSeries		Automatic series distribution
 *  2		04h		fAutoPosX		X positioning is automatic
 *  3		08h		fAutoPosY		Y positioning is automatic
 *  4		10h		fVert			1= vertical legend, 0= horizontal 
 *  5		20h		fWasDataTable	1= chart contains data table
 *  
 *  NOTES:
 *  A SPRC is a unit of measurement that is 1/4000th of the height or width of the chart 
 *  If the field is being used to specify a width or horizontal distance, the SPRC is 1/4000th 
 *  of the width of the chart.  If the field is being used to specify a height or vertical 
 *  distance, the SPRC is 1/4000th of the height of the chart.
 *  
 *   Sequence of records:
 *   ATTACHEDLABEL = TextDisp Begin Pos [FontX] [AlRuns] AI [FRAME] [ObjectLink] [DataLabExtContents] [CrtLayout12] [TEXTPROPS] [CRTMLFRT] End
 *   LD = Legend Begin Pos ATTACHEDLABEL [FRAME] [CrtLayout12] [TEXTPROPS] [CRTMLFRT] End
   
 *  
 */
public class Legend extends GenericChartObject implements ChartObject, ChartConstants {
	/** 
	* serialVersionUID
	*/
	private static final long serialVersionUID = -4041111720696805018L;
	protected int x_defunct=-1, y_defunct=-1, dx_defunct=-1, dy_defunct=-1;		// these vars are now defunct; see doc above + Pos/getSVG for coordinate info
	protected byte /*wType= -1, */wSpacing= -1;
	protected short grbit= -1;
	protected boolean fAutoPosition, fAutoSeries, fAutoPosX, fAutoPosY;
	protected boolean fVert, fWasDataTable;
	public static final int BOTTOM=0, CORNER= 1, TOP= 2, RIGHT= 3, LEFT= 4, NOT_DOCKED= 7;
	int[] legendCoords= null;
	
	public void init() {
		super.init();
        byte[] rkdata = this.getData();
        x_defunct= ByteTools.readInt(this.getBytesAt(0, 4));
        y_defunct= ByteTools.readInt(this.getBytesAt(4, 4));
        dx_defunct= ByteTools.readInt(this.getBytesAt(8, 4));
        dy_defunct= ByteTools.readInt(this.getBytesAt(12, 4));
        // unused:  wType= rkdata[16]; layout position is controled by CrtLayout12 record
		wSpacing= rkdata[17];
		grbit= ByteTools.readShort(rkdata[18], rkdata[19]);
		parseGrbit();
	}
	/**
	The following records and rules define the significant parts of a legend:

    The Legend record specifies the layout of the legend and specifies if the legend is automatically positioned.
    The Pos record, CrtLayout12 record, specify the position of the legend.
    The sequences of records that conform to the ATTACHEDLABEL (TextDisp ->Pos [FontX] [AlRuns] AI [FRAME] [ObjectLink] [DataLabExtContents] [CrtLayout12] [TEXTPROPS] [CRTMLFRT] ) 
    	and TEXTPROPS (RichTextStream|TextPropStream) rules specify the default text formatting for the legend entries. 
    	The Pos record of the attached label MUST be ignored. The ObjectLink record of the attached label MUST NOT exist. 
    	A series can specify formatting exceptions for individual legend entries.
    The sequence of records that conforms to the FRAME (Frame ->LineFormat AreaFormat [GELFRAME] [SHAPEPROPS]) rule specifies the fill and border formatting properties of the legend.
	 */
	
	protected void parseGrbit() {
		byte[] grbytes = ByteTools.shortToLEBytes((short)grbit);
		fAutoPosition= (((byte)grbytes[0] & 0x01) == 0x01);
		fAutoSeries	= (((byte)grbytes[0] & 0x02) == 0x02);
		fAutoPosX	= (((byte)grbytes[0] & 0x04) == 0x04);
		fAutoPosY	= (((byte)grbytes[0] & 0x08) == 0x08);
		fVert		= (((byte)grbytes[0] & 0x10) == 0x10);
		fWasDataTable= (((byte)grbytes[0] & 0x20) == 0x20);
	}
	
	
	public void setIsDataTable(boolean isDataTable) {
		fWasDataTable= isDataTable;
		grbit= ByteTools.updateGrBit(grbit, fWasDataTable, 5);
		updateRecord();
	}
	/* unused
	public void setwType(int type) {
		wType= (byte) type;
		updateRecord();
	}
	*/
	
	public void setVertical(boolean isVertical) {
		fVert= isVertical;
    	grbit= ByteTools.updateGrBit(grbit, fVert, 4);
    	updateRecord();		
	}
	
	public static Legend createDefaultLegend(com.extentech.formats.XLS.WorkBook book) {
		Legend l= (Legend) Legend.getPrototype();		
		Pos p= (Pos) Pos.getPrototype(Pos.TYPE_LEGEND);
		l.chartArr.add(p);
		TextDisp td= (TextDisp) TextDisp.getPrototype(ObjectLink.TYPE_TITLE, "", book);
		l.chartArr.add(td);
		return l;		
	}
	
	private void updateRecord() {
		System.arraycopy(ByteTools.cLongToLEBytes(x_defunct), 0, this.getData(), 0, 4);
		System.arraycopy(ByteTools.cLongToLEBytes(y_defunct), 0, this.getData(), 4, 4);
		System.arraycopy(ByteTools.cLongToLEBytes(dx_defunct), 0, this.getData(), 8, 4);
		System.arraycopy(ByteTools.cLongToLEBytes(dy_defunct), 0, this.getData(), 12, 4);
		// unused this.getData()[16]= wType;
		this.getData()[17]= wSpacing;
        byte[] b = ByteTools.shortToLEBytes(grbit);
        this.getData()[18] = b[0];
        this.getData()[19] = b[1];
	}
	
	private byte[] PROTOTYPE_BYTES = new byte[] {-11, 13, 0, 0, -72, 3, 0, 0, -111, 1, 0, 0, -31, 4, 0, 0, 3, 1, 31, 0};
    public static XLSRecord getPrototype() {
        Legend l= new Legend();
        l.setOpcode(LEGEND);
        l.setData(l.PROTOTYPE_BYTES);
        l.init();
        return l;
    }
    
     /** legend position:
      * controlled by CrtLayout12
     * 0= bottom, 1= corner, 2= top, 3= right, 4= left
     * @return
     */
    public short getLegendPosition() {
    	CrtLayout12 crt= (CrtLayout12) Chart.findRec(this.chartArr, CrtLayout12.class);
    	if (crt!=null)
    		return (short)crt.getLayout();
    	if (fVert || !fAutoPosition)
    		return RIGHT; // default
    	return BOTTOM;	// default if not vert
    }
    
    /**
     * return the legend position in string (OOXML) form
     * @return b, l, r, t, tr
     */
    public String getLegendPositionString() {
    	int lpos= getLegendPosition();
    	String[] pos= { "b", "tr", "t", "r", "l"};
    	if (lpos >=0 && lpos < pos.length)
    		return pos[lpos];
    	return "r"; 
    }
    
    /**
     * set Legend Positon:  one of:
     * 0= bottom, 1= corner, 2= top, 3= right, 4= left, 7= not docked
     * @param pos
     */
    public void setLegendPosition(short pos) {
    	CrtLayout12 crt= (CrtLayout12) Chart.findRec(this.chartArr, CrtLayout12.class);
    	if (crt!=null)
    		crt.setLayout(pos);
    }

    /**
     * retrieves the specific font for these legends, if set (null if not)
     * @return
     */
    private Fontx getLegendFont() {
    	TextDisp td= (TextDisp) Chart.findRec(chartArr, TextDisp.class);
    	if (td!=null)
    		return (Fontx) Chart.findRec(td.chartArr, Fontx.class);
    	return null;
    }
    
    /**
     * returns true if this legend is surrounded by a box (the default)
     * @return
     */
    public boolean hasBox() {
    	Frame f= (Frame) Chart.findRec(chartArr, Frame.class);
    	if (f!=null) {
    		return f.hasBox();
    	}
//    	return true; // the default
    	return false;    	
    }
    
    public void addBox() {
    	Frame f= (Frame) Chart.findRec(chartArr, Frame.class);
    	if (f==null) {    	
    		f= (Frame) Frame.getPrototype();
    		f.addBox(0, -1, -1);
    		this.chartArr.add(f);
    	}    	
    }
    /**
     * sets or turns off auto positioning
     * [BugTracker 2844]
     * @param auto
     */
    public void setAutoPosition(boolean auto) {
    	if (auto && !fAutoPosition) { 
    		// if setting to autosize/position and it wasn't currently set as so, 
    		// check Pos and Frame records (if present) as they also controls automatic positioning ((:
    		if (this.chartArr.size()>0) {
				try {
	    				Pos p= (Pos)this.chartArr.get(0);
	    				p.setAutosizeLegend();
	    				Frame f= (Frame) Chart.findRec(chartArr, Frame.class);	// find the first one
	    				if (f!=null) f.setAutosize();
				} catch (Exception e) {    				
				}
    		}
    	}
		fAutoPosition= auto;
		fAutoSeries	= auto;
		fAutoPosX	= auto;
		fAutoPosY	= auto;
		//if (wType==3 || wType==4 && auto)
			//fVert= true;
    	grbit= ByteTools.updateGrBit(grbit, fAutoPosition, 0);
    	grbit= ByteTools.updateGrBit(grbit, fAutoSeries, 1);
    	grbit= ByteTools.updateGrBit(grbit, fAutoPosX, 2);
    	grbit= ByteTools.updateGrBit(grbit, fAutoPosY, 3);
    	grbit= ByteTools.updateGrBit(grbit, fVert, 4);
    	this.updateRecord();
    }
    
    /**
     * a rough estimate of expanding legend dimensions of 
     * 1 normal entry 
     *  
     */
    public void incrementHeight(float h) {
	Pos p= (Pos) Chart.findRec(this.chartArr, Pos.class);
	int[] coords= p.getLegendCoords();	// x, y, w, h, fh, legendpos
	Font f= this.getFnt();
	int fh= 10;	// default
	if (f!=null)
	    fh=(int)(f.getFontHeightInPoints()*1.2);	// a little padding
	if (coords!=null) {
	    int z= coords[1]-(int)Math.ceil(Pos.convertToSPRC(fh/2, 0, h));
	    p.setY(z);
	}    	
    }

    /**
     * called up change of legend text to adjust width of the legend bounding box
     * 
     * @param chartMetrics
     * @param chartType		
     * @param legends		String[] text of legends (containing new legend text)
     */
    public void adjustWidth(HashMap<String, Double> chartMetrics, int chartType, String[] legends) {
	Pos p= (Pos) Chart.findRec(this.chartArr, Pos.class);
	int[] coords= p.getLegendCoords();	// x, y, w, h, fh, legendpos
	if (coords!=null) {
	    Font f= this.getFnt();
	    // legend position LEFT and RIGHT display each legend on a separate line (fVert==true)
	    // TOP and BOTTOM are displayed horizontally with symbols and spacing between entries (fVert==false)	    
	    int position= this.getLegendPosition();	
	    float cw= chartMetrics.get("canvasw").floatValue();
	    float x= (int)Math.ceil(Pos.convertFromSPRC(coords[0], cw, 0))-3;
	    float w= chartMetrics.get("w").floatValue();
	    
	    // calculate how much width the legends take up -- algorithm works well for about 80% of the cases ...
	    double legendsWidth= 0;
	    java.awt.Font jf= new java.awt.Font(f.getFontName(), f.getFontWeight(), (int)f.getFontHeightInPoints());
	    int extras= (chartType==ChartConstants.LINECHART|| chartType==ChartConstants.RADARCHART?15:5);	// pad for legend symbols, etc	-	
	    for (int i= 0; i < legends.length; i++) {
		if (fVert)
		    legendsWidth= Math.max(legendsWidth, StringTool.getApproximateStringWidth(jf, " " + legends[i] + " "));
		else  
		    legendsWidth+= StringTool.getApproximateStringWidth(jf, " " + legends[i] + " ") + extras;
	    }
	    if (!fVert)
		legendsWidth-= StringTool.getApproximateStringWidth(jf, " ");	// decrement one space
	    else
		legendsWidth += StringTool.getApproximateStringWidth(jf, " ");
	    
//	    System.out.println(this.getParentChart().toString() + String.format(": legend box x: %.1f legend box w: %.0f chart x: %.1f w: %.1f cw: %.1f font size: %.0f L.W: %.1f Auto? %b Vertical? %b",  
//		    x, (float)coords[2], chartMetrics.get("x"), w, cw, (float) jf.getSize(), legendsWidth, fAutoPosition, fVert));	    
	    p.setLegendW((int)legendsWidth);
	    if (((x + legendsWidth) > cw) || (position==Legend.RIGHT || position==Legend.CORNER)) {
		x= (float)(cw-(legendsWidth+5));
		if (x < 0) x= 0;
		int z= (int)Math.ceil(Pos.convertToSPRC(x, cw, 0));
		p.setX(z);			
	    } 

		
	   
	    /*
	    if (position==Legend.RIGHT || position==Legend.CORNER) {	// usual case
		totalWidth+= extras;
		if ((x+totalWidth) > cw) { // if legends will extend over edge of chart, must adjust either x or chart canvas width to accommodate legends
		    x= cw-totalWidth;
		} 
	    } 
//	    p.setLegendW((int)totalWidth);
	    
	    
	    
	    
	   else if (position==Legend.LEFT) {
		totalWidth+= extras;
		if (totalwidth > x)
		    double originalDist= x-w; 	// original distance between legend and edge of plot area
		    if ((w + totalWidth + originalDist) < cw) 
			;
		    
		    if (totalWidth > (cw-w)) {	// can fit in space between plot area and 
			//KSC: TESTINGs
			//System.out.println("Original Lcoords:  w" + w + " cw:" + cw + " coords:" + Arrays.toString(retcoords));				
			float newX= (int)Math.ceil(cw-w);
			if (originalDist > 0 && (w+x) > newX)
			    chartMetrics.put("w", newX-originalDist);
			//System.out.println("After Lcoords:  w" + w + " cw:" + cw + " coords:" + Arrays.toString(retcoords));				
		    } 
		}
	    } else if (totalWidth > cw) {	// legends are displayed horizontally, can't fit in canvas width
// TODO: must extend cw ?? wrap ???		
		/*	float overage= ((x+w+10)-cw);
		if (overage > 0)
		    chartMetrics.put("canvasw", chartMetrics.get("canvasw")+overage);* /
	    }*/
	}
    }
    
    /**
     * reset initial position of legend to accommodate nLines of legend text  
     */
    public void resetPos(double y, double h, double ch, int nLines) {
		Pos p= (Pos) Chart.findRec(this.chartArr, Pos.class);
/*		apparently just setting to 1/2 h works well!!  
 * 		Font f= this.getFnt();
		int fh= 10;	// default
		if (f!=null)
			fh=(int)(f.getFontHeightInPoints());
		fh=(int)(f.getFontHeightInPoints()*1.2);	// a little padding*/
		int z= (int)Math.ceil(Pos.convertToSPRC((float)(ch/2/*+((fh*nLines)/2)*/), 0, (float)ch));
		p.setY(z);
    	
    }
    
    /**
     * return the coordinates of the legend box in pixels
     * <br>An approximation at this point
     * @param chartMetrics maps chart coords in pixels x, y, w, h, canvasw, canvash, min, max
     * @param  fh			-- font height in points
     * @return	int[4] x, y, w, h
     */
	public int[] getCoords(int charttype, HashMap<String, Double> chartMetrics, String[] legends, java.awt.Font f) {
		// calcs are not 100% ****
		// space between legend entries = 40 twips = 1 twip equals one-twentieth of a printer's point
		Pos p= (Pos) Chart.findRec(this.chartArr, Pos.class);
		int[] coords= p.getLegendCoords();
		int[] retcoords= new int[6];
		int fh=(int)(f.getSize());	//*1.2);	// a little padding
		retcoords[4]= f.getSize();	// store font height
		retcoords[5]= this.getLegendPosition();	// store legend position
		
		boolean canMoveCW= false;
		if (coords!=null) {
			retcoords[0]= (int)Math.ceil(Pos.convertFromSPRC(coords[0], chartMetrics.get("canvasw").floatValue(), 0))-3;	
			retcoords[1]= (int)Math.ceil(Pos.convertFromSPRC(coords[1], 0, chartMetrics.get("canvash").floatValue()));
		} else { // happens upon OOXML			
			retcoords[0]= (int)(chartMetrics.get("w") + chartMetrics.get("x") + 20);	// start just after right side of plot
			retcoords[1]= chartMetrics.get("y").intValue() + (int)(chartMetrics.get("h")/4);
			coords= new int[4];
			canMoveCW= true;		
		}
		if (coords[2]!=0) {
			retcoords[2]= (int)(coords[2]*MSODrawing.PIXELCONVERSION);
			retcoords[2]+=3;	// pad slightly
		} else {
			double len= 0;
			for (int i= 0; i < legends.length; i++) {
				len= Math.max(len, StringTool.getApproximateStringWidth(f, legends[i]));					
			}
			retcoords[2]= (int)Math.ceil(len);	
			retcoords[2]+=15 + (charttype==ChartConstants.LINECHART|| charttype==ChartConstants.RADARCHART?25:5);	// pad for legend symbols, etc	-
			// if now legend box extends over edge reduce plot area width, not canvas width ... EXCEPT for OOXML; in those cases, extend CW
			if (!canMoveCW && ((retcoords[0]+retcoords[2]) > chartMetrics.get("canvasw").floatValue()))	{
				double cw= chartMetrics.get("canvasw");
				double w= chartMetrics.get("w");
// KSC: TESTING
//System.out.println("Original Lcoords:  w" + w + " cw:" + cw + " coords:" + Arrays.toString(retcoords));				
				double ldist= retcoords[0]-w; 	// original distance between legend and edge of plot area
				retcoords[0]= (int)Math.ceil(cw-retcoords[2]);
				if (ldist > 0 && (chartMetrics.get("w")+chartMetrics.get("x")) > retcoords[0])
					chartMetrics.put("w", retcoords[0]-ldist);
//System.out.println("After Lcoords:  w" + w + " cw:" + cw + " coords:" + Arrays.toString(retcoords));				
			}
				
		}
		if (canMoveCW) {
			float overage= ((retcoords[0]+retcoords[2]+10)-(chartMetrics.get("canvasw").floatValue()));
			if (overage > 0)
				chartMetrics.put("canvasw", chartMetrics.get("canvasw")+overage);
		}
		if (coords[3]!=0)
			retcoords[3]= (int)(coords[3]*MSODrawing.PIXELCONVERSION);
		else
			retcoords[3]= ((legends.length+2)*(fh+2));	
		return retcoords;
	}

	/**
	 * tries to get the best match
	 * @return
	 */
	public Font getFnt() {
		try {
			Fontx fx= getLegendFont();
			Font f= this.getParentChart().getWorkBook().getFont(fx.getIfnt());
			if (f!=null)
				return f;
			else	// shouldn't get here ...
				return this.getParentChart().getDefaultFont();			
		} catch (NullPointerException e) {
			// this actually doesn't get the actual font for the legend but can't find correct Fontx record! 
			return this.getParentChart().getDefaultFont();			
		}
	}
	
	
	public void getMetrics(HashMap<String, Double> chartMetrics, int chartType, ChartSeries s) {
		String[] legends= s.getLegends();
		if (legends==null || legends.length==0) return;
		
		Font f= getFnt();
		if (f!=null) {
		    legendCoords= this.getCoords(chartType, chartMetrics, legends, new java.awt.Font(f.getFontName(), f.getFontWeight(), (int)f.getFontHeightInPoints()));
		} else {	// can't find any font ... shouldn't really happen ...?
		    legendCoords= this.getCoords(chartType, chartMetrics, legends, new java.awt.Font("Arial", 400, 10));
		}
	}
	
	/**
	 * return the coordinates of the legend box, relative to the chart
	 * @return int[] coordinates x, y, w, h [fh, legendpos]
	 */
	public int[] getCoords(){
		return legendCoords;
	}
	
	
	/**
	 * returns the Data Legend Box svg for this chart
     * @param chartMetrics maps chart coords in pixels x, y, w, h, canvasw, canvash, min, max
	 * @return
	 */
	int XOFFSET= 12;
	public String getSVG(HashMap<String, Double> chartMetrics, ChartType chartobj, ChartSeries s) {
		StringBuffer svg= new StringBuffer();		
		// position information fro Pos record:
		/**
		 * 	legend			MDCHART						MDABS						The values x1 and y1 specify the horizontal and vertical offsets of the legend's upper-left corner, 
																			relative to the upper-left corner of the chart area, in SPRC. The values of x2 and y2 specify the 
																			width and height of the legend, in points.
			legend			MDCHART						MDPARENT					The values of x1 and y1 specify the horizontal and vertical offsets of the legend's upper-left corner, 
																			relative to the upper-left corner of the chart area, in SPRC. The values of x2 and y2 MUST be ignored. 
																			The size of the legend is determined by the application.
			legend			MDKTH						MDPARENT					The values of x1, y1, x2 and y2 MUST be ignored. The legend is located inside a data table.

		 */

		String[] legends= s.getLegends();
		String[] seriescolors= s.getSeriesBarColors();
		if (legends==null || legends.length==0) return "";
		
		if (legendCoords==null)
			this.getMetrics(chartMetrics, chartobj.getChartType(), s);
		String font;	// font svg
		int fh;	// font height
		Font f= getFnt();
		if (f!=null) {
			font= f.getSVG();
			font= "' " + /*"' vertical-align='bottom' " +*/ font;
			fh= (int)Math.ceil(f.getFontHeightInPoints());
		} else {	// can't find any font ... shouldn't really happen ...?
			font= "' " + "font-family='Arial' font-size='9pt'";
			fh= 10;			
		}
		// get legend info in order to get dimensions
		final int YOFFSET= legendCoords[3]/(legends.length);			
		
		int x= legendCoords[0];
		int y= legendCoords[1];
		int boxw= legendCoords[2];
		int boxh= legendCoords[3];
		
		svg.append("<g>\r\n");
		if (this.hasBox()) {
			svg.append("<rect x='" + x + "' y='" + y + 
					"' width='" + boxw + "' height='" + boxh +
					"' fill='#FFFFFF' fill-opacity='1' stroke='black' stroke-opacity='1' stroke-width='1' " +
					"stroke-linecap='butt' stroke-linejoint='miter' stroke-miterlimit='4' fill-rull='evenodd'" +
					"/>");
			x+=5;	// start of labels (offset from box)
			y+=(YOFFSET/3);	
		}
		if (chartobj.getChartType()==ChartConstants.BARCHART) {	// same as below except order is reversed
			// draw a little box in appropriate color
			int h= 8;	// box size
			for (int i= legends.length-1; i >= 0; i--) {
				svg.append("<rect x='" + x + "' y='" + y + "' width='" + h + "' height='" + h + "' fill='" + seriescolors[i] + 
						"' fill-opacity='1' stroke='black' stroke-opacity='1' stroke-width='1' stroke-linecap='butt' stroke-linejoint='miter' stroke-miterlimit='4' fill-rull='evenodd'" +
						"/>");
				svg.append("<text " + getScript("legend" + i) + " x='" + (x+XOFFSET) + "' y='" + (y+fh) + font + ">" + legends[i] + "</text>");
				y+=YOFFSET;
			}
		} else if (!(chartobj.getChartType()==ChartConstants.LINECHART || chartobj.getChartType()==ChartConstants.SCATTERCHART || chartobj.getChartType()==ChartConstants.RADARCHART || chartobj.getChartType()==ChartConstants.BUBBLECHART)) {
			// draw a little box in appropriate color
			int h= 8;	// box size
			for (int i= 0; i < legends.length; i++) {
				svg.append("<rect x='" + x + "' y='" + y + "' width='" + h + "' height='" + h + "' fill='" + seriescolors[i] + 
						"' fill-opacity='1' stroke='black' stroke-opacity='1' stroke-width='1' stroke-linecap='butt' stroke-linejoint='miter' stroke-miterlimit='4' fill-rull='evenodd'" +
						"/>");
				svg.append("<text "+ getScript("legend" + i) + " x='" + (x+XOFFSET) + "' y='" + (y+fh) + font + ">" + legends[i] + "</text>");
				y+=YOFFSET;
			}
		} else if (chartobj.getChartType()==BUBBLECHART) {
			// little circles
			for (int i= legends.length-1; i >= 0; i--) {
				svg.append("<circle cx='" + (x+3) + "' cy='" + (y+6) + "' r='5' stroke='black' stroke-width='1' fill='" + seriescolors[i] + "'/>");
				svg.append("<text " + getScript("legend" + i) + " x='" + (x+XOFFSET) + "' y='" + (y+fh) + font + ">" + legends[i] + "</text>");
				y+=YOFFSET;
			}
		} else {	// line-type charts/scatter charts/radar charts
			// lines w/markers if nec.
			int[] markers= chartobj.getMarkerFormats();	// markers, if any
			boolean haslines= true;
			if (chartobj.getChartType()==SCATTERCHART) 
				haslines= chartobj.getHasLines();		// lines, if any, on scatter plot
			svg.append(MarkerFormat.getMarkerSVGDefs()); // initial SVG necessary for markers			
			int w= 25;		// w of line + markers
			if (!haslines)
				w= 10;
			XOFFSET= w + 4;
			y+=2;	// a bit more padding
			for (int i= 0; i < legends.length; i++) {
				if (haslines)
					svg.append("<line x1='" + x + "' y1='" + (y+fh/2) + "' x2='" + (x+w) + "' y2='" + (y+fh/2) +
							"' stroke='" + seriescolors[i] + "' stroke-width='2'/>");
				if (markers[i]>0) 
					svg.append(MarkerFormat.getMarkerSVG(x+w/2-5, y+fh/2, seriescolors[i], markers[i]));
				
				svg.append("<text " + getScript("legend" + i)+ " x='" + (x+XOFFSET) + "' y='" + (y+fh) + font + ">" + legends[i] + "</text>");
				y+=YOFFSET;
			}
		}
		svg.append("</g>\r\n");		
		return svg.toString();
	}
}

