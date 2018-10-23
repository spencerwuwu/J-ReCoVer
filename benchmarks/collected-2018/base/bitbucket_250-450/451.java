// https://searchcode.com/api/result/66619291/

/*
 * EPSWriter.java
 *
 * This file is part of GeomLab
 * Copyright (c) 2005 J. M. Spivey
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package plugins;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import funbase.Primitive;
import funbase.Primitive.PRIMITIVE;
import funbase.Value;
import funbase.Evaluator;

/** Picture context that outputs Encapsulated PostScript */
public class EPSWriter extends Stylus {
    private PrintWriter pr;

    public EPSWriter(float width, float height, float slider, Writer out) {
	super(slider);
	this.pr = new PrintWriter(out);

	pr.printf("%%!PS-Adobe-2.0 EPSF-1.2\n");
	pr.printf("%%%%BoundingBox: %d %d %d %d\n", 0, 0,
		  (int) Math.ceil(width), (int) Math.ceil(height));
	pr.printf("50 dict begin\n");
	
	for (String line : prelude) pr.println(line);
	
	pr.printf("/palette [\n");
	for (int i = 0; i < 4; i++) {
	    pr.printf("  { ");
	    writeColor(palette[i]);
	    pr.printf(" } bind\n");
	}
	pr.printf("] def\n");
    }

    private String prelude[] = {
	"0.4 setlinewidth",
	"1 setlinecap",
	"1 setlinejoin",

	// colidx stores the colour index when filling a tile
	"/colidx 0 def",
	
	// initmatrix is the initial transform matrix
	"/initmatrix matrix currentmatrix def",
	
	// mymatrix is the last transform to be set
	"/mymatrix matrix def",
	
	// unit square path
	"/unitsquare {",
	"  newpath 0 0 moveto 0 1 lineto 1 1 lineto 1 0 lineto closepath",
	"} bind def",

	// set a transform and save it in mymatrix
	"/settransform {",
	"  initmatrix setmatrix",
	"  mymatrix astore concat",
	"  mymatrix currentmatrix pop",
	"} bind def",

	// reuse the transform from mymatrix
	"/usetransform { mymatrix setmatrix } bind def",

	// restore the initial transform
	"/resettransform { initmatrix setmatrix } bind def",
	
	// fill an outline using a colour from the palette
	"/palettefill {",
	"  colidx 0 ge {",
	"    colidx add palette length mod",
	"    palette exch get exec fill",
	"  } { pop } ifelse",
	"} bind def"
    };

    @Override
    public void close() {
	pr.printf("showpage\n");
	pr.printf("end\n");
	pr.close();
    }

    private void writeColor(ColorValue color) {
	double r = color.rpart;
	if (color.gpart == r && color.bpart == r)
	    pr.printf("%.3f setgray", r);
	else
	    pr.printf("%.3f %.3f %.3f setrgbcolor", 
		      color.rpart, color.gpart, color.bpart);
    }

    private void writePolygon(Vec2D[] points, Tran2D t) {
	pr.printf("newpath\n");
	String cmd = "moveto";
	for (int i = 0; i < points.length; i++) {
	    Vec2D v = t.transform(points[i]);
	    pr.printf("  %.6f %.6f %s\n", v.x, v.y, cmd);
	    cmd = "lineto";
	}
    }

    private Tran2D currTrans = null;

    private void writeTransform(Tran2D t) {
	if (currTrans == t)
	    pr.printf("usetransform\n");
	else {
	    Vec2D u = t.getXaxis(), v = t.getYaxis(), r = t.getOrigin();
	    pr.printf("%.3f %.3f %.3f %.3f %.3f %.3f settransform\n",
		    u.x, u.y, v.x, v.y, r.x, r.y);
	    currTrans = t;
	}
    }
    
    @Override
    public void drawImage(Native.Image image) {
	// See Red Book, page 310
	int w = image.getWidth(), h = image.getHeight();
	pr.printf("/buf %d string def\n", 3*w);
	writeTransform(trans);
	pr.printf("%d %d 8\n", w, h);
	pr.printf("[ %d 0 0 %d 0 %d ]\n", w, -h, h);
	pr.printf("{ currentfile buf readhexstring pop }\n");
	pr.printf("false 3\n");
	pr.printf("colorimage\n");
	
	for (int y = 0; y < h; y++) {
	    for (int x = 0; x < w; x++) {
		if (x > 0 && x % 24 == 0) pr.printf("\n");
		pr.printf("%06x", image.getRGB(x, y) & 0xffffff);
	    }
	    pr.printf("\n");
	}
	pr.printf("resettransform\n");
    }

    @Override
    public void drawLine(Vec2D from, Vec2D to, ColorValue color) {
	writeColor(color); pr.printf("\n");
	Vec2D a = trans.transform(from), b = trans.transform(to);
	pr.printf("newpath %.3f %.3f moveto %.3f %.3f lineto stroke\n",
		a.x, a.y, b.x, b.y);
    }

    private Set<Integer> knownTiles = new HashSet<Integer>(50);

    /** Draw a tile.  Each kind of tile is saved as a pair of PostScript 
     * procedures so as to reduce the size of the file. */
    @Override
    public void drawTile(TilePicture tile, int layer, int col) {
	int id = tile.tileid;
	Vec2D strokes[][] = tile.strokes, outlines[][] = tile.outlines;
	Object colours[] = tile.colours;
	
	if (! knownTiles.contains(id)) {
	    /* Carefully apply the transform to the path but not to the
	     * pen used to draw it. */
	    pr.printf("/drawt%d { 0 setgray\n", id);
	    for (int i = 0; i < strokes.length; i++) {
		pr.printf("usetransform\n");
		writePolygon(strokes[i], Tran2D.identity);
		pr.printf("resettransform stroke\n");
	    }
	    pr.printf("} bind def\n");
	
	    pr.printf("/fillt%d { /colidx exch def\n", id);
	    for (int i = 0; i < outlines.length; i++) {
		pr.printf("usetransform\n");
		writePolygon(outlines[i], Tran2D.identity);
		pr.printf("resettransform\n");

		Object spec = colours[i];
		if (spec instanceof ColorValue) {
		    writeColor((ColorValue) spec);
		    pr.printf(" fill\n");
		} else if (spec instanceof Integer) {
		    pr.printf("%d palettefill\n", spec);
		}
	    }
	    pr.printf("} bind def\n");
	
	    knownTiles.add(id);
	}
	
	writeTransform(trans);

	switch (layer) {
	case Picture.DRAW:
	    pr.printf("drawt%d\n", id);
	    break;

	case Picture.FILL:
	    pr.printf("%d fillt%d\n", col, id);
	    break;
	}
    }

    @Override
    public void drawPath(TurtlePicture pic) {
	float R = TurtlePicture.R;
	float xmax = pic.xmax, xmin = pic.xmin;
	float ymax = pic.ymax, ymin = pic.ymin;
	Tran2D t1 =
	    trans.scale(1.0f/(xmax-xmin), 1.0f/(ymax-ymin))
		.translate(-xmin, -ymin);
	pr.printf("gsave\n");
	setStroke(2);
	writeTransform(t1);
	
	float x = 0, y = 0, dir = 0;

	pr.printf("newpath\n");
	pr.printf("  %.6f %.6f moveto\n", x, y);
	
	for (TurtlePicture.Command cmd : pic.commands) {
	    switch (cmd.kind) {
		case TurtlePicture.Command.LEFT: {
		    float a = cmd.arg;
		    float xc = x - R * Vec2D.sind(dir);
		    float yc = y + R * Vec2D.cosd(dir);
		    String op = (a >= 0 ? "arc" : "arcn");
		    pr.printf("  %.6f %.6f %.6f %.6f %.6f %s\n", 
			    xc, yc, R, dir-90, dir-90+a, op);
		    x = xc + R * Vec2D.sind(dir+a); 
		    y = yc - R * Vec2D.cosd(dir+a);
		    dir += a;
		    break;
		}
	
		case TurtlePicture.Command.RIGHT: {
		    float a = cmd.arg;
		    float xc = x + R * Vec2D.sind(dir);
		    float yc = y - R * Vec2D.cosd(dir);
		    String op = (a >= 0 ? "arcn" : "arc");
		    pr.printf("  %.6f %.6f %.6f %.6f %.6f %s\n", 
			    xc, yc, R, dir+90, dir+90-a, op);
		    x = xc - R * Vec2D.sind(dir-a); 
		    y = yc + R * Vec2D.cosd(dir-a);
		    dir -= a;
		    break;
		}
		    
		case TurtlePicture.Command.AHEAD:
		    x += cmd.arg * Vec2D.cosd(dir); 
		    y += cmd.arg * Vec2D.sind(dir);
		    pr.printf("  %.6f %.6f lineto\n", x, y);
		    break;
	    }
	}
	
	pr.printf("0 setgray\n");
	pr.printf("resettransform stroke\n");
	pr.printf("grestore\n");
    }

    @Override
    public void drawArc(Vec2D centre, float xrad, float yrad,
	    float start, float extent, ColorValue color) {
	writeColor(color); pr.printf("\n");
	writeTransform(trans.translate(centre.x, centre.y).scale(xrad, yrad));
	String op = (extent >= 0 ? "arc": "arcn");
	pr.printf("newpath 0 0 1 %.6f %.6f %s\n", start, start+extent, op);
	pr.printf("resettransform stroke\n");
    }

    @Override
    public void fillOval(Vec2D centre, float xrad, float yrad, 
			 ColorValue color) {
	writeColor(color); pr.printf("\n");
	writeTransform(trans.translate(centre.x, centre.y).scale(xrad, yrad));
	pr.printf("newpath 0 0 1 0 360 arc closepath fill resettransform\n");
    }

    @Override
    public void drawStroke(Vec2D[] stroke) {
	pr.printf("0 setgray\n");
	writePolygon(stroke, trans);
	pr.printf("stroke\n");
    }

    @Override
    public void fillOutline(Vec2D[] outline, ColorValue color) {
	writeColor(color); pr.printf("\n");
	
	if (outline == Picture.unitsquare) {
	    writeTransform(trans);
	    pr.printf("unitsquare fill resettransform\n");
	    return;
	}
	
	writePolygon(outline, trans);
	pr.printf("fill\n");
    }
    
    @Override
    public void setStroke(float width) {
	final float factor = 2.0f;
	pr.printf("%.3f setlinewidth\n", width/factor);
    }

    @Override
    public boolean isTiny(Tran2D t) {
	return t.isTiny(0.5f);
    }
    
    /** Save a picture as Encapsulated PostScript */
    @PRIMITIVE
    public static Value epswrite(Primitive prim, Value a0, Value a1, 
				 Value a2, Value a3, Value a4) {
	Stylus.Drawable pic = 
            prim.cast(Stylus.Drawable.class, a0, "a picture");
	String fname = prim.string(a1);
	float meanSize = (float) prim.number(a2);
	float slider = (float) prim.number(a3);
	float grey = (float) prim.number(a4);
	ColorValue background = ColorValue.getGrey(grey);

        pic.prerender(slider);

	/* The dimensions of the image are chosen to give
	 * the right aspect ratio, and so that the
	 * geometric mean of width and height is meanSize */
	float sqrtAspect = (float) Math.sqrt(pic.getAspect());
	float width = meanSize * sqrtAspect;
	float height = meanSize / sqrtAspect;		
		
	try {
	    final Writer out = 
		new BufferedWriter(new FileWriter(fname));
	    Stylus g = new EPSWriter(width, height, slider, out);
	    Tran2D t = Tran2D.scaling(width, height);
            pic.draw(g, t, background);
	    g.close();
	}
	catch (IOException e) {
	    Evaluator.error("#epswrite", e);
	}
	
	return Value.nil;		
    }
}

