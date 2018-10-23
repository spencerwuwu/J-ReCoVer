// https://searchcode.com/api/result/132721731/

//##* This program is free software; you can use it, redistribute it and/or modify it
//##* under the terms of the GNU General Public License version 2 as published by the
//##* Free Software Foundation. The full text of the GNU General Public License
//##* version 2 can be found in the file named 'COPYING' that accompanies this
//##* program. This source code is (C)copyright Geoffrey French 2008-2010.
//##************************
package BritefuryJ.LSpace;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.datatransfer.DataFlavor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import BritefuryJ.Math.AABox2;
import BritefuryJ.Math.Point2;
import BritefuryJ.Math.Vector2;
import BritefuryJ.Math.Xform2;

public class ElementPreview
{
	public static final DataFlavor flavor = new DataFlavor( ElementPreview.class, DataFlavor.javaJVMLocalObjectMimeType );
	
	
	private static final double MAX_WIDTH = 256.0;
	private static final double MAX_HEIGHT = 384.0;
	private static final double MIN_SCALE = 0.25;
	
	
	private TexturePaint previewPaint;
	private Vector2 previewSize;
	private Point2 pos;
	private boolean success;
	private LSRootElement root;
	
	
	public ElementPreview(LSElement element)
	{
		AABox2 visibleSpaceBox = element.getVisibleSpaceBox();
		Xform2 elementToWindowXform = element.getLocalToRootXform();
		
		Point2 topLeft = visibleSpaceBox.getLower();
		Vector2 size = visibleSpaceBox.getSize();
		
		// The natural scale is the current scale factor applied to the element
		double scale = elementToWindowXform.scale;
		// Ensure that it cannot go above 1.0 - scale should REDUCE size only
		scale = Math.min( scale, 1.0 );
		
		Vector2 naturalSize = size.mul( scale );
		
		if ( scale > MIN_SCALE )
		{
			// We can scale down further if necessary
			if ( naturalSize.x > MAX_WIDTH  ||  naturalSize.y > MAX_HEIGHT )
			{
				// Compute a scale factor to scale the image to within the bounds of the maximum width and height
				double sx = MAX_WIDTH / naturalSize.x;
				double sy = MAX_HEIGHT / naturalSize.y;
				double s = Math.min( sx, sy );
				
				// Apply the scale factor
				scale *= s;
				
				// Ensure that it does not go below the mininum
				scale = Math.max( scale, MIN_SCALE );
			}
		}
		
		// Compute the size of the preview
		previewSize = size.mul( scale );
		// Clamp to within the maximum allowable size
		previewSize.x = Math.min( previewSize.x, MAX_WIDTH );
		previewSize.y = Math.min( previewSize.y, MAX_HEIGHT );
		
		
		// Compute the preview transform
		Xform2 previewXform = new Xform2( scale, topLeft.toVector2().negate() );
		
		// We now have a preview size and transform. Create a new buffered image for rendering the element
		GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gDev = gEnv.getDefaultScreenDevice();
		GraphicsConfiguration gConf = gDev.getDefaultConfiguration();
		
		BufferedImage previewImage = gConf.createCompatibleImage( (int)( previewSize.x + 0.5 ), (int)( previewSize.y + 0.5 ) );
		Graphics2D previewGraphics = previewImage.createGraphics();
		
		AffineTransform current = previewGraphics.getTransform();
		
		previewGraphics.setBackground( Color.WHITE );
		previewGraphics.clearRect( 0, 0, previewImage.getWidth(), previewImage.getHeight() );
		
		previewXform.apply( previewGraphics );
		
		AABox2 drawBox = new AABox2( topLeft.x, topLeft.y, topLeft.x + previewSize.x / scale, topLeft.y + previewSize.y / scale );
		element.handleDrawBackground( previewGraphics, drawBox );
		element.handleDraw( previewGraphics, drawBox );
		
		previewGraphics.setTransform( current );
		
		
		previewPaint = new TexturePaint( previewImage, new Rectangle2D.Double( 0.0, 0.0, previewSize.x, previewSize.y ) );
	}
	
	
	public void attachTo(LSRootElement root, Point2 pos, boolean success)
	{
		if ( this.root != null )
		{
			this.root.removeElementPreview( this );
		}
		
		this.root = root;
		this.pos = pos;
		this.success = success;
		
		if ( this.root != null )
		{
			this.root.addElementPreview( this );
		}
	}
	
	public void detach()
	{
		if ( this.root != null )
		{
			this.root.removeElementPreview( this );
		}
		
		this.root = null;
		this.pos = null;
	}
	
	
	protected void draw(Graphics2D graphics)
	{
		Paint prevPaint = graphics.getPaint();
		AffineTransform prevXform = graphics.getTransform();
		Composite prevComp = graphics.getComposite();
		
		graphics.translate( pos.x, pos.y );
		graphics.setPaint( previewPaint );
		graphics.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.75f ) );
		Shape shape = new Rectangle2D.Double( 0.0, 0.0, previewSize.x, previewSize.y );
		graphics.fill( shape );
		if ( !success )
		{
			graphics.setComposite( AlphaComposite.SrcOver );
			graphics.setPaint( new Color( 1.0f, 0.0f, 0.0f, 0.15f ) );
			graphics.fill( shape );
		}
		
		graphics.setPaint( prevPaint );
		graphics.setTransform( prevXform );
		graphics.setComposite( prevComp );
	}
}

