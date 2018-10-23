// https://searchcode.com/api/result/114140924/

/***********************************************************************************************
CLASS:	      ErosionCanvas

FUNCTION:     This class does the visualization of the simulation.                            

DATE CREATED: 		August 2002
DATE LAST MODIFIED:	April 2003
***********************************************************************************************/
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.lang.*;

public class ErosionCanvas extends Canvas implements Runnable, MouseListener, MouseMotionListener
{
    SharedParameters sparams = new SharedParameters();
    
    private Image backing;   // For backing store
    private Graphics bg;
    private boolean backingImageSetup;

    private Image render;    // For rendering
    private int pixels [];
    private MemoryImageSource source;
    private boolean renderImageSetup;

    private Thread thread;   // The rendering thread

    private boolean newCanvasSize;  // Canvas has changed size
    private boolean newOrientation; // Projection direction has changed
    private boolean dataChange;
    private boolean setup;

    private int xSize, ySize;  // The size of the rendering canvas in pixels

    // Grid information

    private int gridxSize, gridySize;
    private float gridZ [];   // Data stored in a 1D grid for speed
    private int gridColor [];

    private int borderColor;

    // Grid rendering parameters

    private int xRenderStart, xRenderEnd, xRenderInc;
    private int yRenderStart, yRenderEnd, yRenderInc;

    private float [] screen1old, screen2old, screen1new, screen2new;
    private float [] world1old, world2old, world1new, world2new;
    private float [] v1, v2, normal;
    private float normalFlip;

    // Polygon clipping
    private final static boolean MOVE = false;
    private final static boolean DRAW = true;
    private final static int MAX_VERTEX_LIST_SIZE = 4;
    private float unclippedVertexX[], unclippedVertexY[],
	clippedVertexX[], clippedVertexY[];
    private int nVertex;

    // Polygon scan conversion
    private Edge edges[];
    private int nEdges;

    private Edge edgeList;
    private Edge activeEdgeList;

    // For debugging purposes
    Frame errs;
    TextArea msg;
    //static int count = 0;

    private int backgroundColor;
    private int drawingColor;

    private float aspect;     // The aspect ratio of the canvas
    private float heading, altitude;  // Viewing angles in radians
    
    private Xform clipToScreen;  
    // Holds the transformation from clipping coordinates
    // to device coordinates
    
    private Xform cameraToClip;
    // Holds the transformation from camera coordinates
    // to clipping coordinates
    
    private Xform cameraToScreen;
    // Holds the transformation from camera coordinates
    // to clipping coordinates
    
    private Xform worldToCamera;
    // Holds the transformations from world coordinates to 
    // camera coordinates

    private Xform worldToScreen;
    // Holds the composite transformation from world coordinates to 
    // screen coordinates.  This may be removed later.

    private Xform gridToWorld;
    // Holds the transformations from grid coordinates to 
    // world coordinates

    private Xform gridToScreen;
    // Holds the composite transformation from grid coordinates to 
    // screen coordinates

    public ErosionCanvas()
    {
	// Some initial values
	xSize = 200; ySize = 200;  // Just a random initial value

	backgroundColor = 0; // black 

	heading = -30.0f * (float)Math.PI / 180.0f;
	altitude = 30.0f * (float)Math.PI / 180.0f;

	// 3D transformation initialization
	clipToScreen = new Xform();
	cameraToClip = new Xform();
	cameraToScreen = new Xform();
	worldToCamera = new Xform();
	worldToScreen = new Xform();
	gridToWorld = new Xform();
	gridToScreen = new Xform();

	aspect = (float)xSize / ySize;

	calculateClipToScreen();
	calculateCameraToClip();
	Xform.mult(cameraToScreen, clipToScreen, cameraToClip);
	calculateWorldToCamera();
	Xform.mult(worldToScreen, cameraToScreen, worldToCamera);
	calculateGridToWorld();
	Xform.mult(gridToScreen, worldToScreen, gridToWorld);
	
	// Data grid initialization
	gridxSize = gridySize = 1;
	gridZ = new float [1];
	gridColor = new int[1];

	xRenderStart = 0; xRenderEnd = gridxSize; xRenderInc = 1;
	yRenderStart = gridySize - 1; yRenderEnd = -1; yRenderInc = -1;

	// Rendering parameter initialization
	screen1old = new float [3];
	screen2old = new float [3];
	screen1new = new float [3];
	screen2new = new float [3];
	
	world1old = new float [3];
	world2old = new float [3];
	world1new = new float [3];
	world2new = new float [3];

	v1 = new float [3];
	v2 = new float [3];
	normal = new float[3];
	
	// State flag initialization
	newCanvasSize = false;
	newOrientation = false;
	dataChange = false;

	backingImageSetup = false;
	renderImageSetup = false;
	setup = backingImageSetup && renderImageSetup;


	// Set up the clipping pipeline
	unclippedVertexX = new float [MAX_VERTEX_LIST_SIZE];
	unclippedVertexY = new float [MAX_VERTEX_LIST_SIZE];
	clippedVertexX = new float [MAX_VERTEX_LIST_SIZE * 2];
	clippedVertexY = new float [MAX_VERTEX_LIST_SIZE * 2];
	nVertex = 0;

	// Set up the scan conversion structures
	edges = new Edge [MAX_VERTEX_LIST_SIZE * 2];
	for(int i = 0; i < edges.length; i++)
	    edges[i] = new Edge();

	// Set up the new thread
	thread = new Thread(this);
    }

    //mouse listener implementation
    public void mousePressed(MouseEvent e) 
    	{}//end mousePressed

    public void mouseReleased(MouseEvent e)
    	{}//end mousePressed
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e)
    	{
	}

    //mouse motion listener
    public void mouseMoved(MouseEvent e)
    	{
	}
    //locates the tile as it is dragged across the canvas
    public void mouseDragged(MouseEvent e) 
    	{   
     	}//end mouseDragged


    public void start()
    {
	// Reduce priority of renderer so that UI takes precedence
	Thread current = Thread.currentThread();
	thread.setPriority(current.getPriority() - 1);
	
	thread.start();
    }

    public void update(Graphics g)
	{
	    // Don't clear background.  paint() will redraw entire canvas
	    paint(g);
	}

    public void paint(Graphics g)
	{
	    
	    //msg.append("paint(): width=" + getSize().width + " height=" +
	    //	       getSize().height + "\n");

	    if(setup)
		g.drawImage(backing, 0, 0, xSize, ySize, this);

	    if(getSize().width != xSize || getSize().height != ySize)
		{
		    // Size has changed
		    resizeCanvas(getSize().width, getSize().height);
		}
	}

    public synchronized void resizeCanvas(int xs, int ys)
    { 
	xSize = xs;
	ySize = ys;


	backing = createImage(xSize, ySize);
	// Put something in new image

	bg = backing.getGraphics();

	//msg.append("new backing area: " + xSize + ", " + ySize + "\n");

	// Some initialization code
	backingImageSetup = true;
	setup = backingImageSetup && renderImageSetup;

	// Start up the renderer
	newCanvasSize = true;

	notify();
    }

    public void setBackgroundColor(Color c)
    {
	backgroundColor = c.getRGB();
    }

    public void run()
    {

	while(true)
	    {
		if(upToDate())
		    {
			synchronized(this){
			    try {
				//msg.append("Gonna wait.\n");
				//msg.append("Altitude: " + altitude + "\n");
				wait();
			    }
			    catch (InterruptedException ie){}
			}
			continue;  // Recheck conditions
		    }

		// Render here

		if(newCanvasSize)
		    {
			// New rendering space needs to be allocated
			synchronized(this){
			    // New rendering area
			    newCanvasSize = false;
			    pixels = new int [xSize * ySize];
			    source = new MemoryImageSource(xSize, ySize,
							   pixels, 0, xSize);
			    source.setAnimated(true);
			    render = createImage(source);

			    // New rendering parameters
			    aspect = (float)xSize / ySize;
			    calculateClipToScreen();
			    calculateCameraToClip();
			    Xform.mult(cameraToScreen, 
				       clipToScreen, cameraToClip);
			    Xform.mult(worldToScreen, 
				       cameraToScreen, worldToCamera);
			    Xform.mult(gridToScreen,
				       worldToScreen, gridToWorld);
			    renderImageSetup = true;
			    setup = backingImageSetup && renderImageSetup;

			    //msg.append("new rendering area: " + xSize + ", " + ySize + "\n");
			}
		    }

		if(newOrientation)
		    {
			// Calculate new rendering parameters

			newOrientation = false;

			calculateWorldToCamera();
			Xform.mult(worldToScreen, 
				   cameraToScreen, worldToCamera);
			Xform.mult(gridToScreen,
				   worldToScreen, gridToWorld);
			setRenderOrder();
		    }

		dataChange = false;  
		//  Assume all changes to this point are caught

		// This thread can get here before drawing regions are set up.
		// Drawing regions can't be set up in constructor.  Info
		// from UI not available yet.
		if(!setup)
		    continue; 

		// Here's the picture	
		//msg.append("Drawing...\n");
		// Background
		for(int i = xSize * ySize - 1; i >= 0; i--)
		    pixels[i] = backgroundColor;

		render();

		// Refresh backing store
		source.newPixels(0, 0, xSize, ySize);
		bg.drawImage(render, 0, 0, xSize, ySize, this);
		//msg.append("calling repaint()\n");
		repaint();
	    }
    }

    private boolean upToDate()
    {
	//msg.append("upToDate(): " + count++ + "\n");
	if(newCanvasSize)
	    return false;
	if(newOrientation)
	    return false;
	if(dataChange)
	    return false;

	return true;
    }

    private void render()
    {
	int x, y;

	float z;

	// There are some speedup measures that can be taken here by 
	// reusing values from one grid element to another

	// Y direction is outer loop
	for(y = yRenderStart; y != yRenderEnd; y += yRenderInc)
	    {
		// Initialize trailing edge points
		z = gridZ[y * gridxSize + xRenderStart];
		screen1old[0] = screen1new[0] =
		    Xform.multRow(0, gridToScreen, xRenderStart, y, z);
		
		screen1old[1] = screen1new[1] =
		    Xform.multRow(1, gridToScreen, xRenderStart, y, z);
		
		world1old[0] = world1new[0] =
		    Xform.multRow(0, gridToWorld, xRenderStart, y, z);
		world1old[1] = world1new[1] = 
		    Xform.multRow(1, gridToWorld, xRenderStart, y, z);
		world1old[2] = world1new[2] = 
		    Xform.multRow(2, gridToWorld, xRenderStart, y, z);
		
		// Move one row along
		y += yRenderInc; // A temporary adjustment
		
		z = gridZ[y * gridxSize + xRenderStart];
		screen2old[0] = screen2new[0] = 
		    Xform.multRow(0, gridToScreen, xRenderStart, y, z);
		
		screen2old[1] = screen2new[1] = 
		    Xform.multRow(1, gridToScreen, xRenderStart, y, z);
		
		world2old[0] = world2new[0] =
		    Xform.multRow(0, gridToWorld, xRenderStart, y, z);
		world2old[1] = world2new[1] = 
		    Xform.multRow(1, gridToWorld, xRenderStart, y, z);
		world2old[2] = world2new[2] = 
		    Xform.multRow(2, gridToWorld, xRenderStart, y, z);
		
		y -= yRenderInc; // Correct the adjustment
		
		for(x = xRenderStart; x != xRenderEnd; )
		    {
			//msg.append("(" + y + ", " + x + "):\n");
			
			//  Move along one column
			x += xRenderInc;
			
			z = gridZ[y * gridxSize + x];
			screen1new[0] =
			    Xform.multRow(0, gridToScreen, x, y, z);
			
			screen1new[1] = 
			    Xform.multRow(1, gridToScreen, x, y, z);
			
			world1new[0] = 
			    Xform.multRow(0, gridToWorld, x, y, z);
			world1new[1] =
			    Xform.multRow(1, gridToWorld, x, y, z);
			world1new[2] = 
			    Xform.multRow(2, gridToWorld, x, y, z);

			// Move one row along
			y += yRenderInc;  // Temporary adjustment
			
			z = gridZ[y * gridxSize + x];
			screen2new[0] =
			    Xform.multRow(0, gridToScreen, x, y, z);
			
			screen2new[1] = 
			    Xform.multRow(1, gridToScreen, x, y, z);
			
			world2new[0] = 
			    Xform.multRow(0, gridToWorld, x, y, z);
			world2new[1] =
			    Xform.multRow(1, gridToWorld, x, y, z);
			world2new[2] = 
			    Xform.multRow(2, gridToWorld, x, y, z);

			
			y -= yRenderInc;  // Correct adjustment
			
			drawingColor = calculateShading(gridColor[y * gridxSize + x]);

			// Now draw quad -- CCW order not guaranteed yet
			
			quadClipDraw(screen1old[0], screen1old[1], MOVE);
			quadClipDraw(screen2old[0], screen2old[1], MOVE);
			quadClipDraw(screen2new[0], screen2new[1], MOVE);
			quadClipDraw(screen1new[0], screen1new[1], DRAW);
			
			// Save useful conversion work
			screen1old[0] = screen1new[0];
			screen1old[1] = screen1new[1];
			screen2old[0] = screen2new[0];
			screen2old[1] = screen2new[1];
			world1old[0] = world1new[0];
			world1old[1] = world1new[1];
			world1old[2] = world1new[2];
			world2old[0] = world2new[0];
			world2old[1] = world2new[1];
			world2old[2] = world2new[2];
		    }
		
		thread.yield(); // Play nice, let someone else compute.
	    }
	// Draw border here
	drawBorders();
    }

    private void drawBorders()
    {
	int x, y;

	float z;

	// Side
	x = xRenderEnd;
	y = yRenderStart;
	z = gridZ[y * gridxSize + x];

	// Initialize trailing edge points
	screen1old[0] = screen1new[0] = 
	    Xform.multRow(0, gridToScreen, x, y, 0.0f);
	screen1old[1] = screen1new[1] = 
	    Xform.multRow(1, gridToScreen, x, y, 0.0f);
	
	
	screen2old[0] = screen2new[0] = 
	    Xform.multRow(0, gridToScreen, x, y, z);
	screen2old[1] = screen2new[1] = 
	    Xform.multRow(1, gridToScreen, x, y, z);
	
	world1old[0] = world1new[0] =
	    Xform.multRow(0, gridToWorld, x, y, 0.0f);
	world1old[1] = world1new[1] = 
	    Xform.multRow(1, gridToWorld, x, y, 0.0f);
	world1old[2] = world1new[2] = 
	    Xform.multRow(2, gridToWorld, x, y, 0.0f);
		
	for(y = yRenderStart; y != yRenderEnd; )
	    {
		
		y += yRenderInc;
		
		z = gridZ[y * gridxSize + x];
		
		screen1new[0] = 
		    Xform.multRow(0, gridToScreen, x, y, 0.0f);
		screen1new[1] = 
		    Xform.multRow(1, gridToScreen, x, y, 0.0f);
		world1new[0] = 
		    Xform.multRow(0, gridToWorld, x, y, 0.0f);
		world1new[1] =
		    Xform.multRow(1, gridToWorld, x, y, 0.0f);
		world1new[2] = 
		    Xform.multRow(2, gridToWorld, x, y, 0.0f);
		
		screen2new[0] =
		    Xform.multRow(0, gridToScreen, x, y, z);
		screen2new[1] = 
		    Xform.multRow(1, gridToScreen, x, y, z);
		world2new[0] = 
		    Xform.multRow(0, gridToWorld, x, y, z);
		world2new[1] =
		    Xform.multRow(1, gridToWorld, x, y, z);
		world2new[2] = 
		    Xform.multRow(2, gridToWorld, x, y, z);

		//		drawingColor = calculateShading(gridColor[y * gridxSize + x]);
		drawingColor = calculateShading(borderColor);

		// Now draw quad -- CCW order not guaranteed yet
		
		quadClipDraw(screen1old[0], screen1old[1], MOVE);
		quadClipDraw(screen2old[0], screen2old[1], MOVE);
		quadClipDraw(screen2new[0], screen2new[1], MOVE);
		quadClipDraw(screen1new[0], screen1new[1], DRAW);
		
		// Save useful conversion work
		screen1old[0] = screen1new[0];
		screen1old[1] = screen1new[1];
		screen2old[0] = screen2new[0];
		screen2old[1] = screen2new[1];
		world1old[0] = world1new[0];
		world1old[1] = world1new[1];
		world1old[2] = world1new[2];
		world2old[0] = world2new[0];
		world2old[1] = world2new[1];
		world2old[2] = world2new[2];
	    }
	thread.yield();  // Play nice
	
	// Front
	x = xRenderStart;
	y = yRenderEnd;
	z = gridZ[y * gridxSize + x];
	
	// Initialize trailing edge points
	screen1old[0] = screen1new[0] = 
	    Xform.multRow(0, gridToScreen, x, y, 0.0f);
	screen1old[1] = screen1new[1] = 
	    Xform.multRow(1, gridToScreen, x, y, 0.0f);
	
	screen2old[0] = screen2new[0] = 
	    Xform.multRow(0, gridToScreen, x, y, z);
	screen2old[1] = screen2new[1] = 
	    Xform.multRow(1, gridToScreen, x, y, z);
	
	world1old[0] = world1new[0] =
	    Xform.multRow(0, gridToWorld, x, y, 0.0f);
	world1old[1] = world1new[1] = 
	    Xform.multRow(1, gridToWorld, x, y, 0.0f);
	world1old[2] = world1new[2] = 
	    Xform.multRow(2, gridToWorld, x, y, 0.0f);
		
	for(x = xRenderStart; x != xRenderEnd; )
	    {
		drawingColor = gridColor[y * gridxSize + x];
		
		x += xRenderInc;
		
		z = gridZ[y * gridxSize + x];
		
		screen1new[0] = 
		    Xform.multRow(0, gridToScreen, x, y, 0.0f);
		screen1new[1] = 
		    Xform.multRow(1, gridToScreen, x, y, 0.0f);
		world1new[0] = 
		    Xform.multRow(0, gridToWorld, x, y, 0.0f);
		world1new[1] =
		    Xform.multRow(1, gridToWorld, x, y, 0.0f);
		world1new[2] = 
		    Xform.multRow(2, gridToWorld, x, y, 0.0f);
		
		screen2new[0] =
		    Xform.multRow(0, gridToScreen, x, y, z);
		screen2new[1] = 
		    Xform.multRow(1, gridToScreen, x, y, z);
		world2new[0] = 
		    Xform.multRow(0, gridToWorld, x, y, z);
		world2new[1] =
		    Xform.multRow(1, gridToWorld, x, y, z);
		world2new[2] = 
		    Xform.multRow(2, gridToWorld, x, y, z);

		//		drawingColor = calculateShading(gridColor[y * gridxSize + x]);
		drawingColor = calculateShading(borderColor);

		// Now draw quad -- CCW order not guaranteed yet
		
		quadClipDraw(screen1old[0], screen1old[1], MOVE);
		quadClipDraw(screen2old[0], screen2old[1], MOVE);
		quadClipDraw(screen2new[0], screen2new[1], MOVE);
		quadClipDraw(screen1new[0], screen1new[1], DRAW);
		
		// Save useful conversion work
		screen1old[0] = screen1new[0];
		screen1old[1] = screen1new[1];
		screen2old[0] = screen2new[0];
		screen2old[1] = screen2new[1];
		world1old[0] = world1new[0];
		world1old[1] = world1new[1];
		world1old[2] = world1new[2];
		world2old[0] = world2new[0];
		world2old[1] = world2new[1];
		world2old[2] = world2new[2];
	    }
	thread.yield();  // Play nice
    }

    private int calculateShading(int surfaceColor)
    {
        // Ka = 0.3  Kd = 0.7
	// Light in (-1, 0, 1) direction
	

	// Form first vector
	v1[0] = world1new[0] - world1old[0];
	v1[1] = world1new[1] - world1old[1];
	v1[2] = world1new[2] - world1old[2];

	// Form second vector
	v2[0] = world2old[0] - world1old[0];
	v2[1] = world2old[1] - world1old[1];
	v2[2] = world2old[2] - world1old[2];

	// Form cross product
	normal[0] = v1[1] * v2[2] - v1[2] * v2[1];
	normal[1] = v2[0] * v1[2] - v1[0] * v2[2];
	normal[2] = v1[0] * v2[1] - v2[0] * v1[1];

	// Must normalize normal vector -- ouch!
	float factor = normal[0] * normal[0] 
	    + normal[1] * normal[1] + normal[2] * normal[2];
	
	factor = (float) Math.sqrt(factor);

	normal[0] /= factor; 
	//normal[1] /= factor; 
	normal[2] /= factor;

	// Hard wire to white for now

	// Dot product with normalized (-1, 0, 1)
	float dot = normal[0] * -0.70710678f + normal[2] * 0.70710678f;

	dot *= normalFlip;

	int r, g, b;
		r = (surfaceColor & 0x00ff0000) >> 16;
		g = (surfaceColor & 0x0000ff00) >> 8;
		b = (surfaceColor & 0x000000ff);

	if(dot < 0.0f) // Surface turned away from light
	    {
		// Ambient only
		r = (int)(0.3f * r);
		g = (int)(0.3f * g);
		b = (int)(0.3f * b);
	    }
	
	else
	    {
		// Ambient and diffuse
		r = (int)(0.3f * r + 0.7f * dot * r);
		g = (int)(0.3f * g + 0.7f * dot * g);
		b = (int)(0.3f * b + 0.7f * dot * b);
	    }

	return (0xff << 24) | (r << 16) | (g << 8) | b;
	
    }
    public synchronized void setHeading(int val)
    {
	// In the range of -180 to 180 degrees
	heading = (float)(val) * (float)Math.PI / 180.0f;
	//msg.append("Heading: " + val + "\n");
	newOrientation = true;
	notify();
    }

    public synchronized void setAltitude(int val)
    {
	// In the range of 0 to 90 degrees
	altitude = (float)(val) * (float)Math.PI / 180.0f;
	//msg.append("Altitude: " + val + "\n");
	newOrientation = true;
	notify();
    }

    public synchronized void setViewHeight(float height)
    {
	// Unused for now
	// Meant to record a mean data height so as to better center
	// the view
    }

    public synchronized void setGridSize(int rows, int columns)
    {
	// Sets up a new data grid
	gridySize = rows;
	gridxSize = columns;

	// Allocate a new data grid for rendering
	// Allocation must be done while lock is held on this object
	// otherwise calls to setData*() may occur before the allocation
	// is done
		
	//msg.append("setGridSize()\n");

	// Releasing arrays first will hopefully reduce memory fragmentation
	gridZ = null;
	gridColor = null;

	gridZ = new float [gridxSize * gridySize];
	gridColor = new int [gridxSize * gridySize];

	calculateGridToWorld();
	
	Xform.mult(gridToScreen, worldToScreen, gridToWorld);

	setRenderOrder();

	notify();
    }

    public void setDataHeight(int row, int col, float height)
    {
	// No error checking is done here --- inherently dangerous

	//msg.append("setDataHeight: " + row + ", " + col +  ": " + height + "\n");

	gridZ[row * gridxSize + col] = height;
	
	/*
	// Hack for testing
	float x, y;
	x = (col - 25) / 25.0f;
	y = (row - 25) / 25.0f;

	gridZ[row * gridxSize + col] = 35.0f - 20.0f * (x * x + y * y); 
	*/

	dataChange = true;
    }

    public void setDataColor(int row, int col, int r, int g, int b)
    {
	
	// No error checking is done here --- inherently dangerous

	gridColor[row * gridxSize + col] = (255 << 24) | (r << 16) 
	    | (g << 8) | b;
	dataChange = true;
    }

    public void setWallColor(int r, int g, int b)
    {
	// No error checking is done here

	borderColor = (255 << 24) | (r << 16) | (g << 8) | b;
    }

    public void redraw()
    {
	// Forces a rerendering

	synchronized(this)
	    {
		try{
		    notify();
		}
		catch(IllegalMonitorStateException imse) {}
	    }
    }

    private void setRenderOrder()
    {
	normalFlip = 1.0f;

	if(worldToCamera.get(0, 0) > 0.0f)  // cosine of heading
	    {
		// Y should decrease over rows
		yRenderStart = gridySize - 1; yRenderEnd = 0; yRenderInc = -1;
		normalFlip *= -1.0f;
	    }
	else
	    {
		// Y should increase over rows
		yRenderStart = 0; yRenderEnd = gridySize - 1; yRenderInc = 1;
	    }
	
	if(worldToCamera.get(0, 1) < 0.0f)  // -sine of heading
	    {
		// X should decrease over columns
		xRenderStart = gridxSize - 1; xRenderEnd = 0; xRenderInc = -1;
		normalFlip *= -1.0f;
	    }
	else
	    {
		// X should increase over columns
		xRenderStart = 0; xRenderEnd = gridxSize - 1; xRenderInc = 1;
	    }
    }

    private void quadClipDraw(float x_in, float y_in, boolean end_flag)
    {
	// Full clipping not really needed for this application
	// Just move vertices to edge

	
	//msg.append(nVertex + ": " + x_in + "  " + y_in + "\n");

	if(x_in < 0.0f)  x_in = 0.0f;
	if(x_in >= xSize) x_in = xSize;
	if(y_in < 0.0f)  y_in = 0.0f;
	if(y_in >= ySize) y_in = ySize;
	
	clippedVertexX[nVertex] = x_in;
	clippedVertexY[nVertex] = y_in;
	nVertex++;
	
	if(end_flag)
	    {
		// Draw it!
		scanConvert();

		// Reset vertex count for next quad
		nVertex = 0;
	    }
    }

    private void scanConvert()
    {
	//msg.append("scanConvert()\n");

	int scan;
	
	// Find screen extent in y

	float ymax;
	ymax = clippedVertexY[0];
	for(int i = 1; i < nVertex; i++)
	    {
		if(clippedVertexY[i] > ymax)
		    ymax = clippedVertexY[i];
	    }			

	activeEdgeList = null;

	if(!buildEdgeList())
	    return;  // No edges cross a scanline

	for(scan = edgeList.y; scan <= ymax; scan++)
	    {
		buildActiveList(scan);

		if(activeEdgeList != null)
		    {
			fillScan(scan);
			updateActiveList(scan);
			resortActiveList();
		    }
	    }
	
    }

    private void insertEdgeList(int index)
    {
	Edge p = edgeList, pred = null;
	Edge q = edges[index];

	// Step through list
	while(true)
	    {
		if(p == null)
		    break;  // End of list

		if(q.y < p.y)
		    break;  // We're here

		if(q.y == p.y  &&  q.x < p.x)
		    break;   // Also the proper position

		// Keep moving
		pred = p;
		p = p.next;
		
	    }

	if(pred == null)  // Beginning of list
	    {
		q.next = edgeList;
		edgeList = q;
	    }
	else
	    {
		q.next = p;
		pred.next = q;
	    }
    }

    
    private void makeEdgeRec(int lower, int upper, int index)
    {
	int i;
	float divisor, factor;

	divisor = clippedVertexY[upper] - clippedVertexY[lower];

	edges[index].dx = (clippedVertexX[upper] - clippedVertexX[lower]) 
	    / divisor;

	// Not doing color interpolation now

	// Find initial sample position value where edge intersects scanline
	factor = (float)Math.ceil(clippedVertexY[lower]) - clippedVertexY[lower];

	edges[index].x = clippedVertexX[lower] + factor * edges[index].dx;
	edges[index].y = (int)Math.ceil(clippedVertexY[lower]);

	// Determine last scanline for edge
	edges[index].yUpper = (int)Math.ceil(clippedVertexY[upper]) - 1;

	edges[index].next = null;

	insertEdgeList(index);
	
    }

    private boolean buildEdgeList()
    {
	int v1, v2;
	boolean crossScanline = false;

	nEdges = 0;
	edgeList = null;

	v1 = nVertex - 1;
	for(v2 = 0; v2 < nVertex; v2++)
	    {
		if(Math.ceil(clippedVertexY[v1]) != 
		   Math.ceil(clippedVertexY[v2]))
		    {
			crossScanline = true;
			if(clippedVertexY[v1] < clippedVertexY[v2])
			    // increasing edge
			    makeEdgeRec(v1, v2, nEdges);
			else
			    // decreasing edge
			    makeEdgeRec(v2, v1, nEdges);
			nEdges++;
		    }
		v1 = v2;
	    }
	return crossScanline; 
    }

    private void insertActiveEdgeList(Edge e)
    {
	Edge p = activeEdgeList, pred = null;

	// Step through list
	while(p != null && p.x < e.x)
	    {
		pred = p;
		p = p.next;
	    }

	if(pred == null)  // Beginning of list
	    {
		e.next = activeEdgeList;
		activeEdgeList = e;
	    }
	else
	    {
		e.next = p;
		pred.next = e;
	    }
    }

    private void buildActiveList(int scanline)
    {
	Edge temp;

	while(edgeList != null && edgeList.y == scanline)
	    {
		// Transfer the edges to the active list

		temp = edgeList.next;
		insertActiveEdgeList(edgeList);
		edgeList = temp;
	    }
    }

    private void fillScan(int scanline)
    {
	Edge p1, p2;
	int index, end, column;

	p1 = activeEdgeList;


	while(p1 != null)
	    {
		// Edges should occur in pairs

		p2 = p1.next;

		column = (int)Math.ceil(p1.x);
		end =   (int)Math.ceil(p2.x);

		if(column != end)
		    {
			// Span crosses a sample point

			// No attribute interpolation for now

			index = scanline * xSize + column;
			for( ; column < end; column++)
			    {
				pixels[index++] = drawingColor;
			    }
		    }
		p1 = p2.next;
	    }
    }

    private void updateActiveList(int scanline)
    {
	// Remove finished edges from active edge list and 
	// update the values along all other edges

	Edge p = activeEdgeList, pred = null;

	while(p != null)
	    {
		if(scanline >= p.yUpper)
		    {
			// Remove this edge from active consideration
			p = p.next;
			if(pred == null)
			    activeEdgeList = p;  // remove head of list
			else
			    pred.next = p;
			
		    }
		else
		    {
			// Update the attribute values
			p.x += p.dx;

			pred = p; 
			p = p.next;
		    }
	    }
    }

    private void resortActiveList()
    {
	// Rebuild list completely

	Edge p = activeEdgeList, q;

	activeEdgeList = null;

	while(p != null)
	    {
		q = p;
		p = p.next;
		insertActiveEdgeList(q);
	    }
    }

    private void drawSquare(int x1, int y1, int x2, int y2, int color)
    {
	int index, rowIndex;
	int temp;

	int h, w;  // Height and width


	h = y2 - y1; w = x2 - x1;
	index = rowIndex = y1 * xSize + x1;

	for(int j = h; j > 0; j--)
	    {
		for(int i = w; i > 0; i--)
		    {
			pixels[index++] = color;
		    }
		index = rowIndex += xSize;
	    }
    }

    private void calculateClipToScreen()
    {
	// Calculate the transformation from clipping coordinates to 
	// device coordinates
	
	clipToScreen.set(0, 0, xSize);
	clipToScreen.set(0, 3, -0.5f);
	clipToScreen.set(1, 1, -ySize);
	clipToScreen.set(1, 3, ySize - 0.5f);
    }

    private void calculateGridToWorld()
    {
	// Calculate the transformation from grid coordinates to 
	// world coordinates

	float scale = Math.max(gridxSize, gridySize);
	scale = 2.0f / scale;

	gridToWorld.set(0, 0, scale);
	gridToWorld.set(1, 1, scale);
	gridToWorld.set(2, 2, scale);
	gridToWorld.set(0, 3, -scale * gridxSize / 2);
	gridToWorld.set(1, 3, -scale * gridySize / 2);
	gridToWorld.set(2, 3, -scale * 25.0f);  
	// This last should change when the ability to set viewpoint 
	// height is added.
    }

    private void calculateCameraToClip()
    {
	// Calculate the transformation from camera coordinates to
	// clipping coordinates
	
	if(aspect > 1.0f)
	    {
		cameraToClip.set(0, 0, 1.0f / (2.0f * aspect));
		cameraToClip.set(1, 1, 0.5f);
	    }
	else
	    {
		cameraToClip.set(0, 0, 0.5f);
		cameraToClip.set(1, 1, aspect / 2.0f);
	    }
	cameraToClip.set(0, 3, 0.5f);
	cameraToClip.set(1, 3, 0.5f);
    }
    
    private void calculateWorldToCamera()
    {
	float cosHeading, sinHeading, cosAltitude, sinAltitude;
	
	// This is a composite transformation consisting of
	// 1) a rotation in the xy plane (heading  0-2PI radians)
	// 2) a rotation in the yz plane (altitude 0-PI/2 radians)
	// 3) a rotation in yz by -90 degrees to put into camera coordinates
	
	cosHeading = (float)Math.cos(heading);
	sinHeading = (float)Math.sin(heading);
	cosAltitude = (float)Math.cos(altitude);
	sinAltitude = (float)Math.sin(altitude);
	
	worldToCamera.set(0, 0, cosHeading);
	worldToCamera.set(0, 1, -sinHeading);
	worldToCamera.set(1, 0, sinAltitude * sinHeading);
	worldToCamera.set(1, 1, sinAltitude * cosHeading);
	worldToCamera.set(1, 2, cosAltitude);
	worldToCamera.set(2, 0, -cosAltitude * sinHeading);
	worldToCamera.set(2, 1, -cosAltitude * cosHeading);
	worldToCamera.set(2, 2, sinAltitude);
    }

}

class Xform
{
  // Matrix is assumed to be stored in row major order
  // 1-D array used for minor speedups
  
  float matrix[] = {1.0f, 0.0f, 0.0f, 0.0f, 
		    0.0f, 1.0f, 0.0f, 0.0f,
		    0.0f, 0.0f, 1.0f, 0.0f,
		    0.0f, 0.0f, 0.0f, 1.0f};
  
  
  public static void mult(Xform result, Xform op1, Xform op2)
  {
    // 4 by 4 matrix multiplication  - op1 * op2 goes into result
    
    // First row in result
    
    result.matrix[0 * 4 + 0] = 
      op1.matrix[0 * 4 + 0] * op2.matrix[0 * 4 + 0] +
      op1.matrix[0 * 4 + 1] * op2.matrix[1 * 4 + 0] +
      op1.matrix[0 * 4 + 2] * op2.matrix[2 * 4 + 0] +
      op1.matrix[0 * 4 + 3] * op2.matrix[3 * 4 + 0];
    
    result.matrix[0 * 4 + 1] = 
      op1.matrix[0 * 4 + 0] * op2.matrix[0 * 4 + 1] +
      op1.matrix[0 * 4 + 1] * op2.matrix[1 * 4 + 1] +
      op1.matrix[0 * 4 + 2] * op2.matrix[2 * 4 + 1] +
      op1.matrix[0 * 4 + 3] * op2.matrix[3 * 4 + 1];
    
    result.matrix[0 * 4 + 2] = 
      op1.matrix[0 * 4 + 0] * op2.matrix[0 * 4 + 2] +
      op1.matrix[0 * 4 + 1] * op2.matrix[1 * 4 + 2] +
      op1.matrix[0 * 4 + 2] * op2.matrix[2 * 4 + 2] +
      op1.matrix[0 * 4 + 3] * op2.matrix[3 * 4 + 2];
    
    result.matrix[0 * 4 + 3] = 
      op1.matrix[0 * 4 + 0] * op2.matrix[0 * 4 + 3] +
      op1.matrix[0 * 4 + 1] * op2.matrix[1 * 4 + 3] +
      op1.matrix[0 * 4 + 2] * op2.matrix[2 * 4 + 3] +
      op1.matrix[0 * 4 + 3] * op2.matrix[3 * 4 + 3];
    
    
    result.matrix[1 * 4 + 0] = 
      op1.matrix[1 * 4 + 0] * op2.matrix[0 * 4 + 0] +
      op1.matrix[1 * 4 + 1] * op2.matrix[1 * 4 + 0] +
      op1.matrix[1 * 4 + 2] * op2.matrix[2 * 4 + 0] +
      op1.matrix[1 * 4 + 3] * op2.matrix[3 * 4 + 0];
    
    result.matrix[1 * 4 + 1] = 
      op1.matrix[1 * 4 + 0] * op2.matrix[0 * 4 + 1] +
      op1.matrix[1 * 4 + 1] * op2.matrix[1 * 4 + 1] +
      op1.matrix[1 * 4 + 2] * op2.matrix[2 * 4 + 1] +
      op1.matrix[1 * 4 + 3] * op2.matrix[3 * 4 + 1];
    
    result.matrix[1 * 4 + 2] = 
      op1.matrix[1 * 4 + 0] * op2.matrix[0 * 4 + 2] +
      op1.matrix[1 * 4 + 1] * op2.matrix[1 * 4 + 2] +
      op1.matrix[1 * 4 + 2] * op2.matrix[2 * 4 + 2] +
      op1.matrix[1 * 4 + 3] * op2.matrix[3 * 4 + 2];
    
    result.matrix[1 * 4 + 3] = 
      op1.matrix[1 * 4 + 0] * op2.matrix[0 * 4 + 3] +
      op1.matrix[1 * 4 + 1] * op2.matrix[1 * 4 + 3] +
      op1.matrix[1 * 4 + 2] * op2.matrix[2 * 4 + 3] +
      op1.matrix[1 * 4 + 3] * op2.matrix[3 * 4 + 3];
    
    
    result.matrix[2 * 4 + 0] = 
      op1.matrix[2 * 4 + 0] * op2.matrix[0 * 4 + 0] +
      op1.matrix[2 * 4 + 1] * op2.matrix[1 * 4 + 0] +
      op1.matrix[2 * 4 + 2] * op2.matrix[2 * 4 + 0] +
      op1.matrix[2 * 4 + 3] * op2.matrix[3 * 4 + 0];
    
    result.matrix[2 * 4 + 1] = 
      op1.matrix[2 * 4 + 0] * op2.matrix[0 * 4 + 1] +
      op1.matrix[2 * 4 + 1] * op2.matrix[1 * 4 + 1] +
      op1.matrix[2 * 4 + 2] * op2.matrix[2 * 4 + 1] +
      op1.matrix[2 * 4 + 3] * op2.matrix[3 * 4 + 1];
    
    result.matrix[2 * 4 + 2] = 
      op1.matrix[2 * 4 + 0] * op2.matrix[0 * 4 + 2] +
      op1.matrix[2 * 4 + 1] * op2.matrix[1 * 4 + 2] +
      op1.matrix[2 * 4 + 2] * op2.matrix[2 * 4 + 2] +
      op1.matrix[2 * 4 + 3] * op2.matrix[3 * 4 + 2];
    
    result.matrix[2 * 4 + 3] = 
      op1.matrix[2 * 4 + 0] * op2.matrix[0 * 4 + 3] +
      op1.matrix[2 * 4 + 1] * op2.matrix[1 * 4 + 3] +
      op1.matrix[2 * 4 + 2] * op2.matrix[2 * 4 + 3] +
      op1.matrix[2 * 4 + 3] * op2.matrix[3 * 4 + 3];
    
    
    result.matrix[3 * 4 + 0] = 
      op1.matrix[3 * 4 + 0] * op2.matrix[0 * 4 + 0] +
      op1.matrix[3 * 4 + 1] * op2.matrix[1 * 4 + 0] +
      op1.matrix[3 * 4 + 2] * op2.matrix[2 * 4 + 0] +
      op1.matrix[3 * 4 + 3] * op2.matrix[3 * 4 + 0];
    
    result.matrix[3 * 4 + 1] = 
      op1.matrix[3 * 4 + 0] * op2.matrix[0 * 4 + 1] +
      op1.matrix[3 * 4 + 1] * op2.matrix[1 * 4 + 1] +
      op1.matrix[3 * 4 + 2] * op2.matrix[2 * 4 + 1] +
      op1.matrix[3 * 4 + 3] * op2.matrix[3 * 4 + 1];
    
    result.matrix[3 * 4 + 2] = 
      op1.matrix[3 * 4 + 0] * op2.matrix[0 * 4 + 2] +
      op1.matrix[3 * 4 + 1] * op2.matrix[1 * 4 + 2] +
      op1.matrix[3 * 4 + 2] * op2.matrix[2 * 4 + 2] +
      op1.matrix[3 * 4 + 3] * op2.matrix[3 * 4 + 2];
    
    result.matrix[3 * 4 + 3] = 
      op1.matrix[3 * 4 + 0] * op2.matrix[0 * 4 + 3] +
      op1.matrix[3 * 4 + 1] * op2.matrix[1 * 4 + 3] +
      op1.matrix[3 * 4 + 2] * op2.matrix[2 * 4 + 3] +
      op1.matrix[3 * 4 + 3] * op2.matrix[3 * 4 + 3];
    
  }

    public static float multRow(int row, Xform xform, float x, float y, float z)
    {
	return 
	    xform.matrix[row * 4 + 0] * x +
	    xform.matrix[row * 4 + 1] * y + 
	    xform.matrix[row * 4 + 2] * z +
	    xform.matrix[row * 4 + 3];
    }

  public static void rotateXY(Xform xform, float angle)
  {
    // Appends a rotation matrix in the xy plane to the 
    // right of the given transformation
    // Angle is in degrees

    angle *= (float)(Math.PI / 180.0);
    float cos = (float)Math.cos(angle);
    float sin = (float)Math.sin(angle);
    
    float a, b;  // Temporary holding variables
    
    // First row
    a = xform.matrix[0 * 4 + 0];
    b = xform.matrix[0 * 4 + 1];
    xform.matrix[0 * 4 + 0] = a * cos + b * sin;
    xform.matrix[0 * 4 + 1] = -a * sin + b * cos;
    
    // Second row
    a = xform.matrix[1 * 4 + 0];
    b = xform.matrix[1 * 4 + 1];
    xform.matrix[1 * 4 + 0] = a * cos + b * sin;
    xform.matrix[1 * 4 + 1] = -a * sin + b * cos;
    
    // Third row
    a = xform.matrix[2 * 4 + 0];
    b = xform.matrix[2 * 4 + 1];
    xform.matrix[2 * 4 + 0] = a * cos + b * sin;
    xform.matrix[2 * 4 + 1] = -a * sin + b * cos;
    
    // Fourth row
    a = xform.matrix[3 * 4 + 0];
    b = xform.matrix[3 * 4 + 1];
    xform.matrix[3 * 4 + 0] = a * cos + b * sin;
    xform.matrix[3 * 4 + 1] = -a * sin + b * cos;
    
  }
  
  public static void rotateYZ(Xform xform, float angle)
  {
    // Appends a rotation matrix in the yz plane to the 
    // right of the given transformation
    // Angle is in degrees
    
    angle *= (float)(Math.PI / 180.0);
    float cos = (float)Math.cos(angle);
    float sin = (float)Math.sin(angle);
    
    float a, b;  // Temporary holding variables
    
    // First row
    a = xform.matrix[0 * 4 + 1];
    b = xform.matrix[0 * 4 + 2];
    xform.matrix[0 * 4 + 1] = a * cos + b * sin;
    xform.matrix[0 * 4 + 2] = -a * sin + b * cos;
    
    // Second row
    a = xform.matrix[1 * 4 + 1];
    b = xform.matrix[1 * 4 + 2];
    xform.matrix[1 * 4 + 1] = a * cos + b * sin;
    xform.matrix[1 * 4 + 2] = -a * sin + b * cos;
    
    // Third row
    a = xform.matrix[2 * 4 + 1];
    b = xform.matrix[2 * 4 + 2];
    xform.matrix[2 * 4 + 1] = a * cos + b * sin;
    xform.matrix[2 * 4 + 2] = -a * sin + b * cos;
    
    // Fourth row
    a = xform.matrix[3 * 4 + 1];
    b = xform.matrix[3 * 4 + 2];
    xform.matrix[3 * 4 + 1] = a * cos + b * sin;
    xform.matrix[3 * 4 + 2] = -a * sin + b * cos;
    
  }
  
  public static void rotateZX(Xform xform, float angle)
  {
    // Appends a rotation matrix in the zx plane to the 
    // right of the given transformation
    // Angle is in degrees
    
    angle *= (float)(Math.PI / 180.0);
    float cos = (float)Math.cos(angle);
    float sin = (float)Math.sin(angle);
    
    float a, b;  // Temporary holding variables
    
    // First row
    a = xform.matrix[0 * 4 + 0];
    b = xform.matrix[0 * 4 + 2];
    xform.matrix[0 * 4 + 0] = a * cos - b * sin;
    xform.matrix[0 * 4 + 2] = a * sin + b * cos;
    
    // Second row
    a = xform.matrix[1 * 4 + 0];
    b = xform.matrix[1 * 4 + 2];
    xform.matrix[1 * 4 + 0] = a * cos - b * sin;
    xform.matrix[1 * 4 + 2] = a * sin + b * cos;
    
    // Third row
    a = xform.matrix[2 * 4 + 0];
    b = xform.matrix[2 * 4 + 2];
    xform.matrix[2 * 4 + 0] = a * cos - b * sin;
    xform.matrix[2 * 4 + 2] = a * sin + b * cos;
    
    // Fourth row
    a = xform.matrix[3 * 4 + 0];
    b = xform.matrix[3 * 4 + 2];
    xform.matrix[3 * 4 + 0] = a * cos - b * sin;
    xform.matrix[3 * 4 + 2] = a * sin + b * cos;
    
  }
  
  public float get(int row, int col)
  {
    // Return the matrix entry at the row and column
    
    return matrix[row * 4 + col];
  }
  
  public void set(int row, int col, float value)
  {
    // Set the matrix entry at the row and column
    
    matrix[row * 4 + col] = value;
  }
}

class Edge
{
    int yUpper; // Final scan line of edge
    int y;

    float x, r, g, b;
    float dx, dr, dg, db;
    Edge next;


    Edge()
    {
	x = 0.0f;
	y = 0;
	r = g = b = 255.0f;  // White

	dx = dr = dg = db = 0.0f;

	next = null;
    }

    /*  // For debugging
    void print(TextArea msg)
    {
	msg.append("x: " + x + " y: " + y + "\n");
	msg.append("dx: " + dx + "\n");
    }
    */
}

