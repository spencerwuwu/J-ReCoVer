// https://searchcode.com/api/result/114140927/

/***********************************************************************************************
CLASS:	      ErosionSim

FUNCTION:     ErosionSim is a class where the cellular automata algorithm is implemented. 
	      The class contains:
	      -Constructor: 
    		*ErosionSim(SharedParameters s, ErosionCanvas e) = it initilializes needed 
    			variables to be able to communicate with other parts of the applet.    		   
	      -Helping functions:
    		*public void start() = it sets up the thread that will run the algorithm
		*public void run() = what the thread will do while it is true. It will first
			reset the parameters and conditions, then it will check for ending
			conditions and if everything is alright, it will proceed to execute
			the algorithm.
			Algorithm inside run() = if the precipiton has just fallen, it starts
			the counter, and it gets a random cell in the grid. If it is first
			time, it will calculate the erosion percentages for the rest of the 
			simulation.		
			It will also check if tectonics is enabled and will proceed to apply
			it.
			Once those functions are executed, it calls a function to get the 8
			cells surrounding the randomly chosen one and calls a function to 
			apply diffusion.
			After checking that a wall was not hit, we continue in the while loop
			by calling the functions to search the lowest cell in the group, to
			calculate the corresponding erosion, to check the carrying capacity and 
			to draw the grid with the changes.
			The target coordinates move from the randomly chosen cell to the lowest
			cell and the loop continues.
	    	*private boolean bored() = to check the status of the thread
	    	*public synchronized void resume() = to get the thread going
    		*public void suspend() = to supend the thread
    		*private void reset() = to do the first set up of parameters and values
	    	*private void setColors() = to check for colors in the rainbow
	    	*private void resetSlope() = to check for slope in graph
    		*private void getstartingCell() = this functions gets an x,y random location in 
    			the topographic grid.
   		*private void getSurroundingCells() = this function gets the cells surrounding 
   			the randomly selected one.
		*public void geterosionValue() = get erosion according to parameters from applet.
		*public void applyDiffusion() = apply diffusion when first precipiton falls.
		*public void searchlowestCell() = search for lowest cell in 3x3 grid
		*public void calculateErosion() = apply the erosion according to erodibility 
			parameters
		*public void checkcarryingCapacity() = check the carrying capacity
		*public void applyTectonics() = apply tectonics if selected
		*public void cleanup() = set variables before each iteration
		
INPUT:	      User will select parameters.

DATE CREATED: 		August 2002
DATE LAST MODIFIED:	April 2003
***********************************************************************************************/
import java.lang.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

public class ErosionSim implements Runnable
    	{
    	SharedParameters gv;
    	ErosionCanvas ecanv;

	Thread thisthread;  // The simulation thread
	// variables for the surface object creation
    	static int BARMINHEIGHT = 20;
    	static int BARMINWIDTH = 2;
    	static int EDGES = 2;
    	double incrvertical = 0;
    	double incrhorizontal = 0;
    	double x, y, x1, y1 = 0;
	int newcolor[] = new int[3];
    	double slope;
	//to hold the grid objects
	SurfaceBar surfaceArray[][];
	//flags needed troughout the simulation
    	boolean keepgoing, firstTime;
    	boolean uiSetup;
    	boolean running;
	static boolean startNeeded = true;
	//flag for any bar found
	boolean anybars = false;
	//variables for diffusion, tectonics, erosion and capacity
    	static int minHeight, nextHeight, current;
	SurfaceBar barArray[] = new SurfaceBar[9];
	int xcoordArray[] = new int[9];
	int ycoordArray[] = new int[9];
	SurfaceBar barDiffusionArray[] = new SurfaceBar[4];
	int xcoordDiffusionArray[] = new int[4];
	int ycoordDiffusionArray[] = new int[4];
	int diffusesedimentx = 0;
	int diffusesedimenty = 0;
	int getsedimentx = 0;
	int getsedimenty = 0;
	int irand, jrand, pirand, pjrand;
	SurfaceBar temp;			
	double currentDiffusion = 0;
	double possibleDiffusion = 0;
	double diffusionPower = 0;
	int steps = 0;
	double heightDifference = 0;
	double heightDiff = 0;
	//coord variables to be used later
	int newx, newy, diffxint, diffyint;
	double targetBar = 0;
	double lowestBar = 0;
	static int counter = 0;
    	//for messages
    	static Label values = new Label("");
	// Erosion variables
	double erosion, rand, randxleft, randxright, randybottom, randytop, randerosion;
	double erosionPower, possibleErosion;
	double currentErosion = 0;
	double basicErosion = 0;
	ErosionColors colors = new ErosionColors();
	int resetColor = 0;
	int incrIndex = 0;
	int incrDiffusionIndex = 0;
	static double min = 20;
	static double max = 21;
		
    // Constructor	
    ErosionSim(SharedParameters s, ErosionCanvas e)
    	{
	gv = s;
	ecanv = e;
	uiSetup = false;
	thisthread = new Thread(this);
    	}

    // Start the thread
    public void start()
    	{
	// Reduce priority of renderer so that UI takes precedence
	Thread current = thisthread.currentThread();
	thisthread.setPriority(current.getPriority() - 1);
	thisthread.start();
    	}

    // Called by start()
    public void run()
    	{
	// Initialize all the data values
	reset();  
	while(true)
	    	{
		if(bored())
		    	{
			synchronized(this)
				{
			    	try 
			    		{
					wait();
				    	}
			    	catch (InterruptedException ie){}
				}
				continue;  // Recheck conditions
		    	}

		// Let's do something!
		if(gv.ITERATIONCOUNTER >= gv.TOTALYEARS)
		    	{
			// Time to stop -- all done
			running = false;
			gv.ROUTINESTARTED = false;
			continue;
		    	}

		// To get it started
		if(firstTime)
		    	{
			gv.ITERATIONCOUNTER = gv.ITERATIONCOUNTER + 1;
			counter = 0;
			// Call getstartingCell to get the first random bar
			getstartingCell();
		    	}
		// To re-calculate erosion values every time a change occurs
		if(gv.EROSIONNEEDED)
		    	{
			geterosionValue();
			gv.EROSIONNEEDED = false;
		    	}
				
		// To apply tectonics if selected
		if(gv.APPLYTECTONICS && firstTime)
		    	{
   		    	applyTectonics();
   			}

		// To look for the eight target surrounding cells every time it 
		// goes through this loop
		getSurroundingCells();
		
		//get erosion value according to applet parameters and apply diffusion
		if(firstTime && keepgoing)
		    {
			applyDiffusion();
			firstTime = false;
		    }
		
		//only apply these if getSurroundingCells was successful       
		if(keepgoing)
		    {
			//search for the lowest cell in the 3x3 grid
			searchlowestCell();
			
			//apply erosion according to erodibility parameters
			calculateErosion();	
			
			//check capacity
			checkcarryingCapacity();
														
			//set lowest bar as target
			jrand = newx; 
			irand = newy; 
		    }
		if((gv.BARSPROCESSED % 5000) == 0 || gv.ITERATIONCOUNTER == gv.ENDTIME)
			{
			setColors();

			ecanv.setWallColor(200, 180, 100);
			}
		ecanv.redraw();
		thisthread.yield();  // Play nice -- let someone else have some time
	    	}
    	}//end of run()

    //set color of surface
    private void setColors()
    	{
	//scale of colors ranging from blue to orange
	//blue meaning the lowest and orange meaning the highest
	//implementation of 4 color areas
	double lowestBar = BARMINHEIGHT;
	double highestBar = 0;
	for(int i = 0; i < gv.ROWS; i++)
		{
		for (int j = 0; j < gv.COLUMNS; j++)
			{
			double bar1 = surfaceArray[i][j].getsurfacefinalHeight();				
			if(bar1 > highestBar) 								
				{
				highestBar = bar1;
				}
			}//end of for loop
		}

	double difference = highestBar - lowestBar;
	double interpolation = difference / 1023;
	int colorIndex = 0;

	for(int x = 0; x < gv.ROWS; x++)
		{
		for(int y = 0; y < gv.COLUMNS; y++)
			{
			colorIndex = (int) ((surfaceArray[x][y].getsurfacefinalHeight() - lowestBar) / interpolation);
			if(colorIndex < 0)
				{
				colorIndex = 0;
				}
			if(colorIndex > 1022)
				{
				colorIndex = 1022;
				}
		        ecanv.setDataHeight(x, y, (float) surfaceArray[x][y].getsurfacefinalHeight());		    
			ecanv.setDataColor(x, y, colors.getColor1(colorIndex), colors.getColor2(colorIndex), colors.getColor3(colorIndex));
			}	
		}
    	}//end setColors


    //if thread gets lazy
    private boolean bored()
    	{
	// When this returns true, there is nothing constructive to do.
	if(running)
	    return false;

	return true;
    	}

    //to get thread going from where it was paused
    //there is checking if stuff needs to be reset
    public synchronized void resume()
    	{
	// This is called to start the simulation up (again)
	if(gv.OLDCOLUMNS != gv.COLUMNS || gv.OLDROWS != gv.ROWS)
		{
		gv.OLDCOLUMNS = gv.COLUMNS;
		gv.OLDROWS = gv.ROWS;
		gv.STARTALLOVER = true;	
		}
	if((int)(gv.ENDTIME/gv.TIMESTEP) != gv.TOTALYEARS)
		{
		gv.STARTALLOVER = true;	
		}
	if(gv.EROSIONNEEDED)
		{
		geterosionValue();	
		gv.EROSIONNEEDED = false;
		}	
	if(gv.STARTALLOVER)
		{
		reset(); // Maybe break this out later to separate button?
		gv.STARTALLOVER = false;
		}
	if(gv.OLDSLOPE != gv.SLOPE)
		{
		resetSlope();	
		}
	gv.ROUTINESTARTED = true;
	keepgoing = true;
	running = true;
	notify();
    	}

    //hold on
    public void suspend()
    	{
	// Hold on
	gv.ROUTINESTARTED = false;
	running = false;
    	}

    //reset values
    private void reset()
    	{
	// When this truly becomes a reset function, the next few lines
	// should go somewhere more appropriate	
	gv.EROSIONNEEDED = true;
	gv.ROUTINESTARTED = false;
        gv.TOTALYEARS = (int) (gv.ENDTIME / gv.TIMESTEP);
	gv.OLDCOLUMNS = gv.COLUMNS;
	gv.OLDROWS = gv.ROWS;
	if(!uiSetup)
	    	{
		values = gv.values;
	    	}
	//temporary change the cell size to width = 1 impact on erosion value
	gv.BARWIDTH = 1;
	surfaceArray = new SurfaceBar[gv.ROWS][gv.COLUMNS];
	ecanv.setGridSize(gv.ROWS, gv.COLUMNS);
	ecanv.setViewHeight(BARMINHEIGHT);

	x = y = x1 = y1 = 0;
	incrvertical = incrhorizontal = 0;
	newcolor[0] = 220;
	newcolor[1] = 180;
	newcolor[2] = 0;

	firstTime = true;
	startNeeded = true;
	gv.CARRYINGCAPACITY = 0;
	gv.STEPCOUNTER= 0;
	steps = 0;
	
	running = false;

	//set iteration counter to zero to start
	gv.ITERATIONCOUNTER = 0;
	gv.BARSPROCESSED = 0;
	gv.COLORCHANGE = 0;
	
	// create the surface
	for(int j = 0; j < gv.ROWS; j++)
	    	{
		for(int i = 0; i < gv.COLUMNS; i++)
		    	{			
			incrhorizontal = (BARMINWIDTH) * i;
			x1 = BARMINWIDTH;
			y1 = BARMINHEIGHT;
			slope = BARMINHEIGHT * gv.SLOPE * (j - 1) / gv.ROWS;

			if(j == 0)
			 	{
				surfaceArray[j][i] = 
			    		new SurfaceBar( x, y, 1, y1 - 1, slope, 
					    newcolor[0], 
					    newcolor[1], 
					    newcolor[2]);
				}
			else
				{
				surfaceArray[j][i] = 
			    		new SurfaceBar( x, y, x1, y1, slope, 
					    newcolor[0], 
					    newcolor[1], 
					    newcolor[2]);
				}
			surfaceArray[j][i].setfinalHeight();
			ecanv.setDataHeight(j, i, (float) surfaceArray[j][i].getsurfacefinalHeight());
		    	}//end for i
			incrvertical = incrvertical - 1;
	    }//end for j
	setColors();
  	}//end reset


    //when slope is resetted from applet
    private void resetSlope()
    	{
	gv.OLDSLOPE = gv.SLOPE;
	for(int j = 0; j < gv.ROWS; j++)
	    	{
		for(int i = 0; i < gv.COLUMNS; i++)
		    {			
		    double newSlope = BARMINHEIGHT * gv.SLOPE * (j - 1) / gv.ROWS;
		    surfaceArray[j][i].setSlope(newSlope);
		    surfaceArray[j][i].setfinalHeight();
		    ecanv.setDataHeight(j, i, (float) surfaceArray[j][i].getsurfacefinalHeight());		    
		    }
		  }			
    	}//end of resetSlope()

    //this function gets an x,y random location in the array
    private void getstartingCell()
    	{
	gv.ROUTINESTARTED = true;

	//get random values for first target at the beginnig of an iteration	
	//set index to 1+ off because of the walls
	jrand = (int) (0 + Math.random() * gv.ROWS);
	irand = (int) (0 + Math.random() * gv.COLUMNS);

	gv.STEPCOUNTER = gv.STEPCOUNTER + 1;
        }//end getstarting Cell
    
    //get erosion according to parameters from applet
    public void geterosionValue()
		{
		rand = 0;
		basicErosion = 0;
		//to get erosion value if no break point has been selected
		if(gv.XPOINT < 0 && gv.YPOINT < 0)
			{
			//if random erosion over basic is selected
			if(gv.RANDEROSION)
				{	
				//get a the random value
				basicErosion = gv.RANDVALUE;
				}
			else
				{
				basicErosion = gv.EROSION;	
				}
			//calculate the total erosion for the basic options
			//if random erosion is not clicked, rand equal zero
			randxleft = randxright = randybottom = randytop = -1;
			}
		else
			{
			gv.EROSION = 0;
			randxleft = gv.XRANDLEFT;
			randxright = gv.XRANDRIGHT;
			randybottom = gv.YRANDBOTTOM;
			randytop = gv.YRANDTOP;					
			}//end of erosion checking	
		gv.EROSIONNEEDED = false;
		}//end of geterosionValue()

   //this function gets the cells surrounding the randomly selected one
   private void getSurroundingCells()
	{		  
	int randx1 = jrand;
	int randy1 = irand;
	gv.BARSPROCESSED = gv.BARSPROCESSED + 1;
	keepgoing = true;
	cleanup();
	if(gv.ITERATIONCOUNTER == 0)
		{
		values.setText("");
		}
	
	if(gv.ITERATIONCOUNTER > 0)
		{	
		if((gv.BARSPROCESSED % 5000) == 0 || gv.ITERATIONCOUNTER == gv.ENDTIME)
			{
			values.setText("        Total Iterations: " + ((int) (gv.ENDTIME / gv.TIMESTEP)) + " - # iterations processed:" + gv.ITERATIONCOUNTER);
			steps = 0;
			}
		newx = newy = 0;	
		incrIndex = 0;
		incrDiffusionIndex = 0;
		//get 3x3 grid and closest neighbors
		//get 3x3 grid in vectors for later analysis
	     try
		{
		if(randx1 - 1 >= 0 && randx1 - 1 < gv.ROWS && randy1 - 1 >= 0 && randy1 - 1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1 - 1;
			ycoordArray[incrIndex] = randy1 - 1;			
			incrIndex++;
			}
		if(randx1 - 1 >= 0 && randx1 - 1 < gv.ROWS && randy1 >= 0 && randy1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1 - 1;
			ycoordArray[incrIndex] = randy1;			
			xcoordDiffusionArray[incrDiffusionIndex] = randx1 - 1;
			ycoordDiffusionArray[incrDiffusionIndex] = randy1;
			incrIndex++;
			incrDiffusionIndex++;
			}
		if(randx1 - 1 >= 0 && randx1 - 1 < gv.ROWS && randy1 + 1 >= 0 && randy1 + 1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1 - 1;
			ycoordArray[incrIndex] = randy1 + 1;			
			incrIndex++;
			}
		if(randx1 >= 0 && randx1 < gv.ROWS && randy1 - 1 >= 0 && randy1 - 1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1;
			ycoordArray[incrIndex] = randy1 - 1;			
			xcoordDiffusionArray[incrDiffusionIndex] = randx1;
			ycoordDiffusionArray[incrDiffusionIndex] = randy1 - 1;
			incrIndex++;
			incrDiffusionIndex++;
			}
		if(randx1 >= 0 && randx1 < gv.ROWS && randy1 >= 0 && randy1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1;
			ycoordArray[incrIndex] = randy1;			
			incrIndex++;
			}
		if(randx1 >= 0 && randx1 < gv.ROWS && randy1 + 1 >= 0 && randy1 + 1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1;
			ycoordArray[incrIndex] = randy1 + 1;			
			xcoordDiffusionArray[incrDiffusionIndex] = randx1;
			ycoordDiffusionArray[incrDiffusionIndex] = randy1 + 1;
			incrIndex++;
			incrDiffusionIndex++;
			}
		if(randx1 + 1 >= 0 && randx1 + 1 < gv.ROWS && randy1 - 1 >= 0 && randy1 - 1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1 + 1;
			ycoordArray[incrIndex] = randy1 - 1;			
			incrIndex++;
			}
		if(randx1 + 1 >= 0 && randx1 + 1 < gv.ROWS && randy1 >= 0 && randy1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1 + 1;
			ycoordArray[incrIndex] = randy1;			
			xcoordDiffusionArray[incrDiffusionIndex] = randx1 + 1;
			ycoordDiffusionArray[incrDiffusionIndex] = randy1;
			incrIndex++;
			incrDiffusionIndex++;
			}
		if(randx1 + 1 >= 0 && randx1 + 1 < gv.ROWS && randy1 + 1 >= 0 && randy1 + 1 < gv.COLUMNS)
			{
			xcoordArray[incrIndex] = randx1 + 1;
			ycoordArray[incrIndex] = randy1 + 1;			
			incrIndex++;
			}//end of getting 3x3 grid in vectors
		   }
		catch (ArrayIndexOutOfBoundsException aioobe)
		   {}	
		}//end of iteration and thread checking
	}//end of getsurroundingCells

	//apply diffusion when first precipiton falls
	public void applyDiffusion()
		{
		//get target coordinates in local variables
		int randrow1 = jrand;
		int randcolumn1 = irand;
		getsedimentx = -1;
		getsedimenty = -1;
		double sedimentTaken = 0;
				
		for(int t = 0; t < incrDiffusionIndex; t++)
			{
			//extract each one of the closest neighbors from vector (4 bars)
			int tempx = xcoordDiffusionArray[t];
			int tempy = ycoordDiffusionArray[t];
			diffusionPower = 0;
			possibleDiffusion = 1;
			currentDiffusion = 0;
			if(diffusesedimentx > -1 && diffusesedimenty > -1)
				{
				//compare it to with target cell
				//if neighbor is higher than target
				if(surfaceArray[tempx][tempy].getsurfacefinalHeight() > surfaceArray[randrow1][randcolumn1].getsurfacefinalHeight())
					{
					diffusesedimentx = tempx;
					diffusesedimenty = tempy;
					getsedimentx = randrow1;
					getsedimenty = randcolumn1;
					}
				else
					{
					diffusesedimentx = randrow1;
					diffusesedimenty = randcolumn1;
					getsedimentx = tempx;
					getsedimenty = tempy;
					}
				heightDifference = surfaceArray[diffusesedimentx][diffusesedimenty].getsurfacefinalHeight() - surfaceArray[getsedimentx][getsedimenty].getsurfacefinalHeight();
					//check if it is basic erosion value or advanced
					//there could be three different possibilities or erosion values for that bar
					if(gv.XPOINT < 0 && gv.YPOINT < 0)
						{								
						//if neighbor has sediment
						if (surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() > 0)
							{
							diffusionPower = heightDifference / (gv.BARWIDTH * gv.BARWIDTH);
							possibleDiffusion = basicErosion * 2 * diffusionPower;
							//if sediment is not enough
							if (possibleDiffusion > surfaceArray[diffusesedimentx][diffusesedimenty].getSediment())
								{
								currentDiffusion = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
								possibleDiffusion -= currentDiffusion;
								sedimentTaken = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();	
								surfaceArray[diffusesedimentx][diffusesedimenty].setSediment(-sedimentTaken);
								}
							else
								{
								currentDiffusion = possibleDiffusion;
								}
							}
						if ((surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() == 0) && (currentDiffusion != possibleDiffusion))
							{
							diffusionPower = (heightDifference - currentDiffusion) / (gv.BARWIDTH * gv.BARWIDTH);
							possibleDiffusion = basicErosion * diffusionPower;
							currentDiffusion = currentDiffusion + possibleDiffusion;
							}
						}

					if(gv.XPOINT >= 0)
						{
						if(randxleft >= 0 && randcolumn1 <= gv.XPOINT)
							{
							//if neighbor has sediment
							if (surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() > 0)
								{
								diffusionPower =  heightDifference / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randxleft * 2 * diffusionPower;
								//if sediment is not enough
								if (possibleDiffusion > surfaceArray[diffusesedimentx][diffusesedimenty].getSediment())
									{
									currentDiffusion = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
									possibleDiffusion -= currentDiffusion;	
									sedimentTaken = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
									surfaceArray[diffusesedimentx][diffusesedimenty].setSediment(-sedimentTaken);
									}
								else
									{
									currentDiffusion = possibleDiffusion;
									}
								}
							if ((surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() == 0) && currentDiffusion != possibleDiffusion)
								{
								diffusionPower = (heightDifference - currentDiffusion) / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randxleft * diffusionPower;
								currentDiffusion = currentDiffusion + possibleDiffusion;
								}
							}
						if(randxright >= 0 && randcolumn1 > gv.XPOINT)
							{
							//if neighbor has sediment
							if (surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() > 0)
								{
								diffusionPower = heightDifference / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randxright * 2 * diffusionPower;
								//if sediment is not enough
								if (possibleDiffusion > surfaceArray[diffusesedimentx][diffusesedimenty].getSediment())
									{
									currentDiffusion = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
									possibleDiffusion -= currentDiffusion;	
									sedimentTaken = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
									surfaceArray[diffusesedimentx][diffusesedimenty].setSediment(-sedimentTaken);						
									}
								else
									{
									currentDiffusion = possibleDiffusion;
									}
								}
							if ((surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() == 0) && currentDiffusion != possibleDiffusion)
								{
								diffusionPower = (heightDifference - currentDiffusion) / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randxright * diffusionPower;
								currentDiffusion = currentDiffusion + possibleDiffusion;
								}
							}
						}
					if(gv.YPOINT >= 0)
						{
						if(randybottom >= 0 && randrow1 <= gv.YPOINT)
							{
							//if neighbor has sediment
							if (surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() > 0)
								{
								diffusionPower = heightDifference / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randybottom * 2 * diffusionPower;
								//if sediment is not enough
								if (possibleDiffusion > surfaceArray[diffusesedimentx][diffusesedimenty].getSediment())
									{
									currentDiffusion = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
									possibleDiffusion -= currentDiffusion;	
									sedimentTaken = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
									surfaceArray[diffusesedimentx][diffusesedimenty].setSediment(-sedimentTaken);						
									}
								else
									{
									currentDiffusion = possibleDiffusion;
									}
								}
							if ((surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() == 0) && currentDiffusion != possibleDiffusion)
								{
								diffusionPower = (heightDifference - currentDiffusion) / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randybottom * diffusionPower;
								currentDiffusion = currentDiffusion + possibleDiffusion;
								}
							}	
						if(randytop >= 0 && randrow1 > gv.YPOINT)
							{
							//if neighbor has sediment
							if (surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() > 0)
								{
								diffusionPower = heightDifference / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randytop * 2 * diffusionPower;
								//if sediment is not enough
								if (possibleDiffusion > surfaceArray[diffusesedimentx][diffusesedimenty].getSediment())
									{
									currentDiffusion = surfaceArray[diffusesedimentx][diffusesedimenty].getSediment();
									possibleDiffusion -= currentDiffusion;	
									surfaceArray[diffusesedimentx][diffusesedimenty].setSediment(-surfaceArray[diffusesedimentx][diffusesedimenty].getSediment());						
									}
								else
									{
									currentDiffusion = possibleDiffusion;
									}
								}
							if ((surfaceArray[diffusesedimentx][diffusesedimenty].getSediment() == 0) && currentDiffusion != possibleDiffusion)
								{
								diffusionPower = (heightDifference - currentDiffusion) / (gv.BARWIDTH * gv.BARWIDTH);
								possibleDiffusion = randytop * diffusionPower;
								currentDiffusion = currentDiffusion + possibleDiffusion;
								}
							}
						}
					//only for front row
					surfaceArray[diffusesedimentx][diffusesedimenty].setErosion(currentDiffusion - sedimentTaken);
					if(randrow1 == 0)
						{
						currentDiffusion = currentDiffusion * 0.10;
						}
					surfaceArray[getsedimentx][getsedimenty].setSediment(currentDiffusion);
					}
			}//end of for loop			
		for(int t = 0; t < incrIndex; t++)
			{
			//fix height after diffusion
			surfaceArray[xcoordArray[t]][ycoordArray[t]].setfinalHeight();
			}
		}//end of applyDiffusion


	//search for lowest cell in 3x3 grid
	public void searchlowestCell()
		{
		//to do certain number of iterations 
		int randx1 = jrand;
		int randy1 = irand;
		current = 0;
		minHeight = current;
		int xcoord1 = 0;
		int ycoord1 = 0;
		int xcoord2 = 0;
		int ycoord2 = 0;
		
		//find lowest height in array
		for(nextHeight = current + 1; nextHeight < incrIndex; nextHeight++)
			{
			double bar2height = -1;
			double bar1height = -1;						
			xcoord2 = xcoordArray[nextHeight];
			ycoord2 = ycoordArray[nextHeight];
			xcoord1 = xcoordArray[minHeight];
			ycoord1 = ycoordArray[minHeight];

			bar2height = surfaceArray[xcoord2][ycoord2].getsurfacefinalHeight();				
			bar1height = surfaceArray[xcoord1][ycoord1].getsurfacefinalHeight();
			
			//if next bar is lower than current, change value of index
			if(bar2height < bar1height) 								
				{
				minHeight = nextHeight;
				}						
			}//end of for loop
		 

		if(xcoordArray[minHeight] >= 0 && ycoordArray[minHeight] >= 0)
			{	
			newx = xcoordArray[minHeight];
			newy = ycoordArray[minHeight];
						
	 		if (newx == gv.OLDX && newy == gv.OLDY)
	 			{
				cleanup();
				firstTime = true;
				keepgoing = false;
        			return;
				}
			else
				{
				gv.OLDX = randx1;
				gv.OLDY = randy1;						
				}
			if (surfaceArray[newx][newy].getsurfacefinalHeight() == surfaceArray[randx1][randy1].getsurfacefinalHeight())
        			{
				firstTime = true;
				keepgoing = false;
        			return;
        			}
			pjrand = randx1;
			pirand = randy1;
			}
		else
			{		
			firstTime = true;
			keepgoing = false;
	       		return;
			}//end if anybars
		}//end of searchlowestCell()

	//apply the erosion according to erodibility parameters
	public void calculateErosion()
		{
		//to do certain number of iterations 
		if(keepgoing)
			{
			int randx2 = jrand;
			int randy2 = irand;	
			//get the heights of both bars and calculate height difference
			gv.HEIGHTDIFFERENCE = heightDiff = surfaceArray[randx2][randy2].getsurfacefinalHeight() - surfaceArray[newx][newy].getsurfacefinalHeight();
			erosionPower = 0;
			possibleErosion = 1;
			currentErosion = 0;
			gv.SEDIMENT = 0;
			double sedimentTaken = 0;
			//calculate erosion of bar based on basic erosion (uniform or random)
			if(gv.XPOINT < 0 && gv.YPOINT < 0)
				{
				if(surfaceArray[randx2][randy2].getSediment() > 0)
					{
					erosionPower = heightDiff / (gv.BARWIDTH * gv.BARWIDTH);
					possibleErosion = (basicErosion * 2) * erosionPower;
					if(possibleErosion > surfaceArray[randx2][randy2].getSediment())
						{
						currentErosion = surfaceArray[randx2][randy2].getSediment();
						possibleErosion -= currentErosion;
						sedimentTaken = surfaceArray[randx2][randy2].getSediment();
						surfaceArray[randx2][randy2].setSediment(- sedimentTaken);
						}
					else
						{
						currentErosion = possibleErosion;
						}
					}
				if((surfaceArray[randx2][randy2].getSediment() == 0) && (currentErosion != possibleErosion))
					{
					erosionPower = (heightDiff - currentErosion) / (gv.BARWIDTH * gv.BARWIDTH);
					possibleErosion = basicErosion * erosionPower;
					currentErosion = currentErosion + possibleErosion;
					}				
				gv.SEDIMENT = currentErosion;
				//only for front row
				surfaceArray[randx2][randy2].setErosion(currentErosion  - sedimentTaken);
				if(newx == 0)
					{
					currentErosion = currentErosion * 0.10;
					}
				surfaceArray[newx][newy].setSediment(currentErosion);
				}
			//check y break point
			if(gv.YPOINT > -1 && gv.YPOINT <= gv.ROWS)
				{
				if(randx2 > gv.YPOINT)
					{
					if(surfaceArray[randx2][randy2].getSediment() > 0)
						{
						erosionPower = heightDiff / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = (randytop * 2) * erosionPower;
						if(possibleErosion > surfaceArray[randx2][randy2].getSediment())
							{
							currentErosion = surfaceArray[randx2][randy2].getSediment();
							erosionPower -= currentErosion;
							sedimentTaken = surfaceArray[randx2][randy2].getSediment();
							surfaceArray[randx2][randy2].setSediment(- sedimentTaken);
							}
						else
							{
							currentErosion = possibleErosion;	
							}
						}
				if(surfaceArray[randx2][randy2].getSediment() == 0 && (currentErosion != possibleErosion))
						{
						erosionPower = (heightDiff - currentErosion) / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = randytop * erosionPower;
						currentErosion = currentErosion + possibleErosion;
						}				
					}//end randy2 > gv.YPOINT
				
				if(randx2 <= gv.YPOINT)
					{
					if(surfaceArray[randx2][randy2].getSediment() > 0)
						{
						erosionPower = heightDiff / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = (randybottom * 2) * erosionPower;
						if(possibleErosion > surfaceArray[randx2][randy2].getSediment())
							{
							currentErosion = surfaceArray[randx2][randy2].getSediment();
							possibleErosion -= currentErosion;
							surfaceArray[randx2][randy2].setSediment(- surfaceArray[randx2][randy2].getSediment());
							}
						else
							{
							currentErosion = possibleErosion;	
							}
						}
				if(surfaceArray[randx2][randy2].getSediment() == 0 && (currentErosion != possibleErosion))
						{
						erosionPower = (heightDiff - currentErosion) / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = randybottom * erosionPower;
						currentErosion = currentErosion + possibleErosion;
						}
					}				
				gv.SEDIMENT = currentErosion;
				//only for front row
				surfaceArray[randx2][randy2].setErosion(currentErosion - sedimentTaken);
				if(newx == 0)
					{
					currentErosion = currentErosion * 0.10;
					}
				surfaceArray[newx][newy].setSediment(currentErosion);
				}//end check for y break point					
			//check x break point
			if(gv.XPOINT > -1 && gv.XPOINT <= gv.COLUMNS)
				{
				if(randy2 > gv.XPOINT)
					{
					if(surfaceArray[randx2][randy2].getSediment() > 0)
						{
						erosionPower = heightDiff / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = (randxright * 2) * erosionPower;
						if(possibleErosion > surfaceArray[randx2][randy2].getSediment())
							{
							currentErosion = surfaceArray[randx2][randy2].getSediment();
							possibleErosion -= currentErosion;
							sedimentTaken = surfaceArray[randx2][randy2].getSediment();
							surfaceArray[randx2][randy2].setSediment(- sedimentTaken);
							}
						else
							{
							currentErosion = possibleErosion;	
							}
						}				
				if(surfaceArray[randx2][randy2].getSediment() == 0 && (currentErosion != possibleErosion))
						{
						erosionPower = (heightDiff - currentErosion) / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = randxright * erosionPower;
						currentErosion = currentErosion + possibleErosion;
						}
					}			
				if(randy2 <= gv.XPOINT)
					{
					if(surfaceArray[randx2][randy2].getSediment() > 0)
						{
						erosionPower = heightDiff / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = (randxleft * 2) * erosionPower;
						if(possibleErosion > surfaceArray[randx2][randy2].getSediment())
							{
							currentErosion = surfaceArray[randx2][randy2].getSediment();
							possibleErosion -= currentErosion;
							sedimentTaken = surfaceArray[randx2][randy2].getSediment();
							surfaceArray[randx2][randy2].setSediment(- sedimentTaken);
							}
						else
							{
							currentErosion = possibleErosion;	
							}
						}
					if(surfaceArray[randx2][randy2].getSediment() == 0 && (currentErosion != possibleErosion))
						{
						erosionPower = (heightDiff - currentErosion) / (gv.BARWIDTH * gv.BARWIDTH);
						possibleErosion = randxleft * erosionPower;
						currentErosion = currentErosion + possibleErosion;
						}
					}				
				gv.SEDIMENT = currentErosion;
				//only for front row
				surfaceArray[randx2][randy2].setErosion(currentErosion - sedimentTaken);
				if(newx == 0)
					{
					currentErosion = currentErosion * 0.10;
					}
				surfaceArray[newx][newy].setSediment(currentErosion);
				}//end check for x break point
			surfaceArray[randx2][randy2].setfinalHeight();
			surfaceArray[newx][newy].setfinalHeight();
			}
		}//end of applyErosion

	//to see if erosion continues
	public void checkcarryingCapacity()
		{
		if(keepgoing)
			{
		double changeperStep = 0;
		//check default without changes in climate		
		if(gv.RAINFALLRATEDEFAULT > 0)
			{
			gv.CARRYINGCAPACITY = gv.RAINFALLRATEDEFAULT * gv.HEIGHTDIFFERENCE;			
			if(gv.SEDIMENT > gv.CARRYINGCAPACITY)
				{
				firstTime = true;
				keepgoing = false;
		        	return;
				}
			else
				{
				}
			}					
		//check when climate is increasing
		if(gv.RAININCREASELOW != 0 && gv.RAININCREASEHIGH != 0)
			{
			 double highlowdifference = gv.RAININCREASEHIGH - gv.RAININCREASELOW;
		 	 changeperStep = highlowdifference / (gv.ENDTIME / gv.TIMESTEP);

			 //this is just to reset the carrying capacity
			 if(startNeeded)
			 	{
			 	gv.CARRYINGCAPACITY = gv.RAININCREASELOW;
			 	startNeeded = false;
				}

		 	if(gv.STEPCOUNTER >= (int) gv.TIMESTEP)
		 		{
				gv.CARRYINGCAPACITY += changeperStep;
			 	gv.STEPCOUNTER = 1;	
		 		}
			if(gv.SEDIMENT > (gv.CARRYINGCAPACITY * gv.HEIGHTDIFFERENCE))
				{
				firstTime = true;
				keepgoing = false;
		        	return;
				}
			else
				{
				}	
			}//end increase 
		//check when climate is decreasing
		if(gv.RAINDECREASELOW != 0 && gv.RAINDECREASEHIGH != 0)
			{
			 double highlowdifference = gv.RAINDECREASEHIGH - gv.RAINDECREASELOW;
			 if(startNeeded)
			 	{
			 	gv.CARRYINGCAPACITY = gv.RAINDECREASEHIGH;
			 	startNeeded = false;
				}
		 	changeperStep = highlowdifference / (gv.ENDTIME / gv.TIMESTEP);

		 	if(gv.STEPCOUNTER > (int) gv.TIMESTEP)
		 		{
		 		gv.CARRYINGCAPACITY -= changeperStep;
		 		gv.STEPCOUNTER = 1;	
		 		}

			if(gv.SEDIMENT > (gv.CARRYINGCAPACITY * gv.HEIGHTDIFFERENCE))
				{
				firstTime = true;
				keepgoing = false;
		        	return;
				}
			}
			}//end of keepgoing
		}//end of check carrying Capacity

	//to see apply tectonics
	public void applyTectonics()
		{
		double tectbottom, tecttop, tectleft, tectright;
		tectbottom = tecttop = tectleft = tectright = 0;
		//calculate erosion of bar based on basic erosion (uniform or random
		//check y break point
		if(gv.TECTONICSYPOINT > -1 && gv.TECTONICSYPOINT <= gv.ROWS)
			{
			for(int trows = 0; trows < gv.ROWS; trows++)
				{
				for(int tcolumns = 0; tcolumns < gv.COLUMNS; tcolumns++)
					{
					if(trows <= gv.TECTONICSYPOINT)
						{
						tectbottom = (gv.TECTONICSYBOTTOM);
						surfaceArray[trows][tcolumns].setTectonics(tectbottom);	
						surfaceArray[trows][tcolumns].setfinalHeight();
						}							
					if(trows > gv.TECTONICSYPOINT)
						{
						tecttop = (gv.TECTONICSYTOP);
						surfaceArray[trows][tcolumns].setTectonics(tecttop);	
						surfaceArray[trows][tcolumns].setfinalHeight();
						counter++;
						}							
					}
				}
			}//end for break point at y


		//check x break point
		if(gv.TECTONICSXPOINT > -1 && gv.TECTONICSXPOINT < gv.COLUMNS)
			{
			for(int trows = 0; trows < gv.ROWS; trows++)
				{
				for(int tcolumns = 0; tcolumns < gv.COLUMNS; tcolumns++)
					{
					if(tcolumns <= gv.TECTONICSXPOINT)
						{
						tectleft = (gv.TECTONICSXLEFT);
						surfaceArray[trows][tcolumns].setTectonics(tectleft);	
						surfaceArray[trows][tcolumns].setfinalHeight();
						}							
					if(tcolumns > gv.TECTONICSXPOINT)
						{
						tectright = (gv.TECTONICSXRIGHT);
						surfaceArray[trows][tcolumns].setTectonics(tectright);	
						surfaceArray[trows][tcolumns].setfinalHeight();
						counter++;
						}
					}
				}
			}//end for break point at y
		}//end of applyTectonics

	//to start fresh
	public void cleanup()
		{
		for(int i = 0; i < 9; i++)
			{
			xcoordArray[i] = -1;
			ycoordArray[i] = -1;
			}
		for(int i = 0; i < 4; i++)
			{
			xcoordDiffusionArray[i] = -1;
			ycoordDiffusionArray[i] = -1;	
			}
    		gv.SEDIMENT = 0.0;			
		}
	}//end of ErosionSim		

/***********************************************************************************************
CLASS:	      SurfaceBar

FUNCTION:     This class represents the single unit that makes the topographic grid.
	      The class contains:
	      -Constructor: 
    	      *SurfaceBar() = default constructor
    	      *SurfaceBar(double x, double y, double x1, double y1, double slope, 
	       	int color1, int color2, int color3) = constructor usually called
	      -Helping functions: 
    		*double calculateRoughness() = calculate bar roughness
    		*double getRoughness() = get bar roughness
     		*double getSlope() = returns the slope of the bar
     		*void setSlope(double newSlope) = sets the height of the bar
     		*void setSediment(double sediment) = sets the sediment
     		*double getSediment() = returns the sediment
     		*void setErosion(double erosion) = sets the erosion
     		*double getErosion() = returns the erosion
     		*void setTectonics(double tectonicvalue) = sets the tectonic value
     		*double getTectonics() = gets the tectonic value
     		*double getsurfacefinalHeight() = returns value after erosion and 
    			sediment are applied
     		*double gety() = returns the width of the bar
     		*void sety(double newy) = sets the width of the bar
     		*double getx1() = returns the xbasePosition of the bar
     		*void setx1(double newx1) = sets the xbasePosition of the bar
     		*double gety1() = returns the ybasePosition of the bar 
    			(difference between y and y1 which gives the actual height)
     		*void sety1(double newy1) = sets the ybasePosition of the bar
    		*void setColor(int color1, int color2, int color3) = allows caller to set the color of the bar
    		*int getColor1() = allows caller to get color1
    		*int getColor2() = allows caller to get color2
    		*int getColor3() = allows caller to get color3

INPUT:        Nothing.                                                        

OUTPUT:       It allows for the creation of a rectangular object that will be used to create a
	      graph.
***********************************************************************************************/
//Begin SurfaceBar
class SurfaceBar
	{
    	double x, y, x1, y1, roughness, slope;
    	double width, erosion, sediment, tectonicvalue;
    	int barColor[] = new int[3];
    	double finalHeight = 0;
	
    	//default constructor
    	SurfaceBar()
    		{
		x = 1;
		y = 1;
		x1 = 1;
		y1 = 1;
		slope = 1;
		roughness = calculateRoughness();
		barColor[0] = 0;
		barColor[1] = 0;
		barColor[2] = 0;
		width = SharedParameters.BARWIDTH;
		erosion = sediment = tectonicvalue = 0;
    		}
	
    	//will be the constructor used most often, allows the caller to
    	//set the height, width, and color of the bar
    	SurfaceBar(double x, double y, double x1, double y1, double slope, 
	       int color1, int color2, int color3)
    		{
		this.roughness = calculateRoughness();
		this.slope = slope;
		this.x = x;
		this.y = y;
		this.x1 = x1;
		this.y1 = y1;
		if(color1 > 255)
	    		{
			barColor[0] = 245;
	    		}
		else
	    		{
			barColor[0] = color1;
	    		}
		if(color2 > 255)
	    		{
			barColor[1] = 150;
	    		}
		else
	    		{
			barColor[1] = color2;
	    		}
		if(color3 > 255)
	    		{
			barColor[2] = 0;
	    		}
		else
	    		{
			barColor[2] = color3;
	    		}
		width = SharedParameters.BARWIDTH;
		erosion = sediment = tectonicvalue = 0;
		finalHeight = y1;
		}
	

    	//calculate bar roughness
    	 double calculateRoughness()
    		{
		return -0.000005 + Math.random() * 0.000005;	
    		}
    
    	//get bar roughness
    	 double getRoughness()
    		{
		return roughness;	
    		}
    
    	//returns the slope of the bar
    	 double getSlope()
    		{
		return slope;			
    		}
    
    	//sets the height of the bar
    	 void setSlope(double newSlope)
    		{
		slope = newSlope;
    		}
    
    	//sets the sediment
    	 void setSediment(double sediment)
    		{
		this.sediment += sediment;
    		}
    
    	//returns the sediment
    	 double getSediment()
    		{
		return sediment;			
    		}
    
    	//sets the erosion
    	 void setErosion(double erosion)
    		{
		this.erosion += erosion;
    		}
    
    	//returns the erosion
    	 double getErosion()
    		{
		return erosion;			
    		}
    
    	//sets the tectonic value
    	 void setTectonics(double tectonicvalue)
		{
		this.tectonicvalue += tectonicvalue;
		}

    	//gets the tectonic value
    	 double getTectonics()
		{
		return tectonicvalue;
		}

    	 double getBarMin()
    		{
		return gety1();	
    		}
    	//returns basic height
    	 double getbasicHeight()
    		{
		return gety1() + getSlope() + getRoughness();	
    		}

	 //set final height
	 void setfinalHeight(int newHeight)
	 	{
		finalHeight = newHeight;			 	
		}

	 //set final height
	 void setfinalHeight()
	 	{
		finalHeight = gety1() + getSlope() + getRoughness() + getSediment() - getErosion() + getTectonics();			 	
		}

    	//returns value after erosion and sediment are applied
    	 double getsurfacefinalHeight()
    		{
		return finalHeight;
    		}
    
    	//returns the width of the bar
    	 double gety()
    		{
		return y;			
    		}
    
    	//sets the width of the bar
    	 void sety(double newy)
    		{
		y = newy;
    		}
    
    	//returns the xbasePosition of the bar
    	 double getx1()
    		{
		return x1;			
    		}
    
    	//sets the xbasePosition of the bar
    	 void setx1(double newx1)
    		{
		x1 = newx1;
    		}
    
    	//returns the ybasePosition of the bar (difference between y and y1 which gives the actual height)
    	 double gety1()
    		{
		return y1;			
    		}
    
    	//sets the ybasePosition of the bar
    	 void sety1(double newy1)
    		{
		y1= newy1;
    		}
    
    	//allows caller to set the color of the bar
    	void setColor(int color1, int color2, int color3)
    		{
		barColor[0] = color1;
		barColor[1] = color2;
		barColor[2] = color3;
    		}
    
    	//allows caller to get color1
    	int getColor1()
    		{
		return barColor[0];
    		}
    
    	//allows caller to get color2
    	int getColor2()
    		{
		return barColor[1];
    		}
    
    	//allows caller to get color3
    	int getColor3()
    		{
		return barColor[2];
    		}
}//end SurfaceBar class
	

