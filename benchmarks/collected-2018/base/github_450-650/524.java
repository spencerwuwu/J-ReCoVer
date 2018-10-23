// https://searchcode.com/api/result/112087170/

package roidatagraph;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.io.*;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion.Static;

import processing.core.PFont;
import dataholders.ActivityData;
import dataholders.DataStructure;
import dataholders.StaticData;
import dataholders.TimeStructure;

public class ROIDataGraph extends PApplet {

	// Variables that hold data from file
	TimeScale timeScale; 			// array of type long that holds the time of each reading from file
	Navigation navigation;
	ArrayList <Boolean> badData;	// array of type boolean that holds whether a reading contains bad data
	DataField[] fields;				// array of type DataField that holds the data for each reading
	ArrayList <ActivityData> activityData;
	ArrayList<Boolean> badActivityData;
	

	// Variables that hold display specs
	PFont font;															// font variable for displaying data to screen
	
	/*********
	 * START - STANDARD PROCESSING FUNCTIONS (set-up and draw
	 *********/

	// SET-UP FUNCTION: Read data from file, load and analyze
	public void setup() {

		// prepare the display
		size((int)(screenWidth), (int)(screenHeight)); 			
		background(StaticData.backColor);

		font = createFont("HelveticaNeue-CondensedBold-36.vlw", 36);
		timeScale = new TimeScale();
		navigation = new Navigation();
		badData = new ArrayList<Boolean>();
		activityData = new ArrayList<ActivityData>();
		badActivityData = new ArrayList<Boolean>();

		
		StaticData.barThickness = 4;
		StaticData.currentViewPosition = 0;
		StaticData.timeScaleLocation = 220;
		StaticData.dataLocation = StaticData.timeScaleLocation + 20;
		StaticData.navLocation = 0;
		StaticData.navHeight = 40;
		StaticData.navMargin = 20;
		StaticData.startTime = 0;
		StaticData.endTime = 0;

		String mainFilename = "/Users/julioterra/Documents/eclipse/eclipse_workspace/ROI_data_graph_v2.4/bin/data/logi-orig.txt";   // name of data file that will be read
		String mainDelimiter = ",";						// delimiter to be used for chopping up data
		int adjResolution = 50000;
		String actFilename = "/Users/julioterra/Documents/ITP/2010_(2)_Fall/rest of you/data files/sitting-session/act-log.txt";   // name of data file that will be read
		String actDelimiter = ",";						// delimiter to be used for chopping up data
		String actPath = "/Users/julioterra/Documents/ITP/2010_(2)_Fall/rest of you/data files/sitting-session/pictures/";
		
		loadMainData(mainFilename, mainDelimiter, adjResolution);								// LOAD MAIN DATA FILE
		activityData = loadData(actFilename, actDelimiter, actPath);		
		drawIt(StaticData.currentViewPosition);		// DISPLAY DATA: Call drawIt function to display graph	
	} // END SET-UP
	
	
	// DRAW FUNCTION
	public void draw() {
		if (StaticData.updateDisplay && mouseY < (StaticData.navLocation + StaticData.navHeight)) {
			StaticData.currentViewPosition = mouseX;
		}
		drawIt(StaticData.currentViewPosition);
	} // END DRAW FUNCTION

	/*********
	 * END - STANDARD PROCESSING FUNCTIONS (set-up and draw
	 *********/

	
	// LOAD MAIN DATA FUNCTION: function that loads the data from the main file
	public void loadMainData(String filename, String delimiter, int adjResolution) {
	/* NOTES:
	 * filename needs to include full path in order for BufferedReader to work
	 * the first line of data should include headers with titles for each field
 	 * if StaticData.timeStamped is set to false then adjResolution setting is ignored
 	 * if adjResolution is set to 0 or less then no time resolution adjustment is created
 	 * if data is StaticData.timeStamped is set to true then a TimeScale object is updated along with the fields array
	 */

	/* UPDATES TO MAKE
	 * move color assignment to different location within code (create a separate function and remove from data field constructor)
	 * change data structure so that each object contains one reading from each field (rather than have the data be field based)
	 */
		
		
		// Variables that hold file specs
		BufferedReader file2read;
		String currentLine;
		int totalReadings = 0;
		int validReadings = 0;						// Counter that holds number of valid readings
		int[] colors = { 0xff8a0000, 0xff009999, 0xffFF9900, 0xffffff00, 0xffff00ff, 0xff00ffff };   // array holds 6 colors
		
		// LOAD FILE IN A TRY / CATCH STATEMENT
		try{

   		// create bufferReader object and read first line of the datafile
			file2read = new BufferedReader (new FileReader(filename));
		
		// READ FIELD NAMES (first line of the data file): use field names and number to prepare data variables for reading file
			currentLine = file2read.readLine();
			String[] fieldNames = currentLine.split(delimiter);		// Read field names (assuming data located on first entry of each field)
			int numberOfFields = fieldNames.length;
			if (StaticData.timeStamped) numberOfFields--; 				// If data includes time stamp then reduce number of fields variable
			
		// INITIALIZE ARRAY: create the fields array and the dataField objects that it contains
			fields = new DataField[numberOfFields];			// Initialize fields array based on number of fields from the data	
			for (int fieldNumber = 0; fieldNumber < numberOfFields; fieldNumber++) {
				int sectionHeight = height/(numberOfFields+2);
				int yLocation = StaticData.dataLocation;
				int xLocation = 0;
				if (!StaticData.timeStamped) fields[fieldNumber] = new DataField(fieldNames[fieldNumber], colors[fieldNumber], xLocation, yLocation, StaticData.barThickness, sectionHeight, StaticData.displayOrder[fieldNumber]);		// if first field is not a timescale then start reading here
				else fields[fieldNumber] = new DataField(fieldNames[fieldNumber+1], colors[fieldNumber], xLocation, yLocation, StaticData.barThickness, sectionHeight, StaticData.displayOrder[fieldNumber]);				// otherwise, start at one field over
			}
	
		// DEBUG - PRINT TO CONSOLE: Print to message window number of entries and fields, and field names
			if (StaticData.timeStamped) println("Attempting to load data with " + numberOfFields + " fields plus a timestamp"); 
			else println("Attempting to load data with " + numberOfFields + " fields. Data has no timestamp"); 
			
		// READ DATA - read first line of data then loop through each additional life of the data file
		  currentLine = file2read.readLine();
		  while(currentLine != null) {
				totalReadings++;
				String[] fieldValues = currentLine.split(delimiter);
				// Check to make sure proper number of fields are present in each line
				if (fieldValues.length != fieldNames.length) {		
					badData.add(true);
					System.out.println("Bad number of fields on entry: " + totalReadings + " - " + currentLine);
					continue;  // jumps to next item in the dataLines array by skipping instructions below
				}
				// Loop through each data in each record and assign it to appropriate field object 
				for (int fieldNumber = 0; fieldNumber < fieldValues.length; fieldNumber++) {
					int thisValue = 0;
					try {
						// if data includes time stamp and this is the first field then record the time into the time array
						if (StaticData.timeStamped && fieldNumber == 0) { timeScale.add(Long.parseLong(fieldValues[0].trim()));
						println(Long.parseLong(fieldValues[0].trim()));}
						else {
							// record the value into the object for that field.
							thisValue = Integer.parseInt(fieldValues[fieldNumber].trim());	// convert value into an integer
							int whichField = fieldNumber;									// create variable to offset field number due to presence of time stamp
							if (StaticData.timeStamped) whichField--;						// offset field number if time stamp flag set to 
							fields[whichField].setValue(totalReadings-1, thisValue);		// add new value to field object at appropriate location
						}
					} catch (NumberFormatException e) {										// catch errors associated with converting string to integer value
						System.out.println("Couldn't parse line " + totalReadings + " " + currentLine);
						badData.add(true);
						continue;								// jump to process next field
					} catch (Exception e) {
						System.out.println("Something bad " + totalReadings + " " + currentLine + e);
						badData.add(true);
						continue;								// jump to process next field
					}
				}
				badData.add(false);
				validReadings++; 		// increase count of valid readings
			
				// ADJUST DATA RESOLUTION: every 50,000 readings (in case we have too many readings per second - for timestamped data only)
				if (StaticData.timeStamped && adjResolution > 0) {
					if (validReadings%adjResolution == 1) {
						ArrayList<Integer> averageIndex = timeScale.avgTime(StaticData.defaultTimeResolution);
						timeScale.resetOrigTime();
						for (int fieldNumber = 0; fieldNumber < fields.length; fieldNumber++) {
							fields[fieldNumber].averageValue(averageIndex);
							fields[fieldNumber].resetOrigValue();
						}
					}	
				}
				// load next line from the data file
				currentLine = file2read.readLine();
		  } // END While loop
		} catch(Exception e){
			println("error reading titles from file - more info: " + e + e.getMessage()); 
		}
		
		// FINAL ARRAY ADJUSTMENT - if data is time stamped then adjust array based on standard time resolution as set by default variable		
		if (StaticData.timeStamped && adjResolution > 0) {
			ArrayList<Integer> averageIndex = timeScale.avgTime(StaticData.defaultTimeResolution);
			timeScale.resetOrigTime();
			for (int fieldNumber = 0; fieldNumber < fields.length; fieldNumber++) {
				fields[fieldNumber].averageValue(averageIndex);
				fields[fieldNumber].resetOrigValue();
			}
		}	

		// DEBUG print stats regarding the data
		println("Number of valid readings equals: " + validReadings);	// number of valid readings
		for (int i = 0; i < fields.length; i++) {							// name of each field along with min and max values
			DataField thisField = fields[i];								
			println("Readings from " + thisField.fieldName + " range from " + thisField.origSmallestValueEver + " to " + thisField.origBiggestValueEver);
		}

	} // END LoadMainData Function

	
	public ArrayList<ActivityData> loadData(String filename, String delimiter, String imgPath) {
		/* NOTES:
		 * filename needs to include full path in order for BufferedReader to work
		 * the first line of data should include headers with titles for each field
		 */
		
		// Variable that is returned by the function
		ArrayList<ActivityData> activityData;
		activityData = new ArrayList<ActivityData>();

		// Variables that hold file specs
		BufferedReader file2read;
		String currentLine;
		int totalReadings = 0;
		int validReadings = 0;						// Counter that holds number of valid readings

		// LOAD FILE IN A TRY / CATCH STATEMENT
		try{

			// create bufferReader object and read first line of the datafile
			file2read = new BufferedReader (new FileReader(filename));
		
			// READ FIELD NAMES (first line of the data file): use field names and number to prepare data variables for reading file
			currentLine = file2read.readLine();
			String[] fieldNames = currentLine.split(delimiter);		// Read field names (assuming data located on first entry of each field)
			int numberOfFields = fieldNames.length;
		
			// DEBUG - PRINT TO CONSOLE: Print to message window number of entries and fields, and field names
			println("Attempting to load data with " + numberOfFields + " fields."); 

			currentLine = file2read.readLine();
			while(currentLine != null) {
					totalReadings++;			// increase total readings count
					String[] fieldValues = currentLine.split(delimiter);
					// Check to make sure proper number of fields are present in each line
					if (fieldValues.length != fieldNames.length) {		
						badActivityData.add(true);
						System.out.println("Bad number of fields on entry: " + totalReadings + " - " + currentLine);
						continue;  // jumps to next item in the dataLines array by skipping instructions below
					}
					try {
						long startTime = Integer.parseInt(fieldValues[0].trim());
						long endTime = Integer.parseInt(fieldValues[1].trim());
						String description = fieldValues[2].trim();
						PImage dataImg = loadImage(imgPath + fieldValues[3].trim());
						activityData.add(new ActivityData(startTime, endTime, description, dataImg));
					} catch (NumberFormatException e) {										// catch errors associated with converting string to integer value
							System.out.println("Couldn't parse line " + totalReadings + " " + currentLine);
							badActivityData.add(true);
							continue;								// jump to process next field
						} catch (Exception e) {
							System.out.println("Something bad " + totalReadings + " " + currentLine + e);
							badActivityData.add(true);
							continue;								// jump to process next field
						}
						badActivityData.add(false);
					validReadings++; 						// increase count of valid readings
					currentLine = file2read.readLine();		// load next line from the data file
			  } // END While loop
			} catch(Exception e){
				println("error reading titles from file - more info: " + e + e.getMessage()); 
			}

			// DEBUG print stats regarding the data
			println("Activity Data: Number of valid readings equals: " + validReadings);	// number of valid readings

		return activityData;
	}
	
	
		
		
		
		
	// CHANGE TIME RESOLUTION FUNCTION
	// Function that enables changing the time resolution temporarily change the time resolution temporarily
	public void changeTimeResolution(long _millis) {
		StaticData.adjTimeResolution = _millis;
		ArrayList<Integer> averageIndex = timeScale.avgTime(StaticData.adjTimeResolution);
		for (int fieldNumber = 0; fieldNumber < fields.length; fieldNumber++) {
			fields[fieldNumber].averageValue(averageIndex);
		}		
	}
	// END CHANGE TIME RESOLUTION FUNCTION

	
	// DRAWIT FUNCTION - Display graph with data
	public void drawIt(float _where) {
		/* 	PLANNED UPDATES * FOR DRAW FUNCTION *
		 ****	- Move draw functionality into the data fields function itself
		 *	- Add the layer of the activities that I was doing during this timeframe (what documents/people I was interacting with)
		 *	- Create a line version of the graph, which creates lines between the previous and current data point
		 *	- Create a legend along the bottom of the x-axis that contains the timescale (consider making it 1-pixel per second)
		 *	- Create location variables for key locations within the layout (e.g. title, origin of graph, etc - consider using PVectors)
		 */
		if (_where >= 0 && _where < width) {

			background(StaticData.backColor);
			smooth();
			
			// DETERMINE WHAT TO DRAW 
			float relMouseLocAcrossScreen = _where / (float) width;
			int dataOnScreen = (int) ((width - DataField.dataLegendPosition)/StaticData.barThickness);								// determine how much data is on the screen
			int dataNotOnScreen = (int) max(0, (fields[0].adjValues.size() - dataOnScreen));				// identifies how much data does not fit on screen using the "times" array
			int dataDisplayOffset = (int) (dataNotOnScreen * relMouseLocAcrossScreen);			// determines offset for display by multiplying the amount of data that can't fit on one screen by the relative location of the mouse
			int dataDisplayEnd = min((dataOnScreen + dataDisplayOffset), fields[0].adjValues.size()-1);	// makes sure that draw loop ends when end of screen or data set is reached

			// DATA DRAW LOOP 
			// draw data from each field, pass the dataDisplayOffset and dataDisplayEnd variables to control evolution
			for (int fieldNumber = 0; fieldNumber < fields.length; fieldNumber++) {
				fields[fieldNumber].drawField(dataDisplayOffset, dataDisplayEnd);
			}
			
			// TIME STAMP DRAW LOOP
			// identify what time period is currently being displayed 
			textFont(font, 12);				// set font type
			if(StaticData.timeStamped){
				timeScale.drawTimeLine(dataDisplayOffset, dataDisplayEnd);
			} else {
				fill(StaticData.timeStampColor);				// set color
				text("First Shown" + dataDisplayOffset, 0, height - 30);
				text("Last Shown " + dataDisplayEnd, width - textWidth("Last Shown " + dataDisplayEnd), height - 30);
			}

			// DRAW NAVIGATION
			navigation.drawNav();
		}
	}
	// END DRAWIT FUNCTION
	

	public void mousePressed() {
		StaticData.updateDisplay = true;
	}

	public void mouseReleased() {
		StaticData.updateDisplay = false;
	}
	
	
	
	/*************
	 * CREATE AND EXTEND CLASSES
	 *************/
	
	// DEFINE TIME SCALE CLASS - save time scale and process it
	public class TimeScale extends TimeStructure {
		
		public TimeScale () {
			super();
		}

		public void drawTimeLine(int dataDisplayOffset, int dataDisplayEnd) {
			// variables that holds where hours, minutes and seconds begin and end withing a time string
			int topTimeScaleLine = StaticData.timeScaleLocation-17;
			int bottomTimeScaleLine = StaticData.timeScaleLocation+5;
		
			// TIME STAMP DRAW
			// Draw top and bottom line around Time Stamps
			fill(StaticData.timeStampColor);
			stroke(StaticData.timeStampColor);
			strokeCap(SQUARE);
			strokeWeight(1);
			line(0, topTimeScaleLine, width, topTimeScaleLine);
			strokeWeight(1);
			line(0, bottomTimeScaleLine, width, bottomTimeScaleLine);
			textFont(font, 15);

			// Write Time Stamps to Screen
			for (int curReading = 10; curReading < timeScale.size(); curReading = curReading + 40) {
				Date d = new Date(timeScale.get(curReading));								// create date from the timestamp
				int location = 0 + ((curReading - dataDisplayOffset)*StaticData.barThickness);
				String time = d.toString();
				text(time.substring(StaticData.hoursBegin, StaticData.secondsEnd), location, StaticData.timeScaleLocation);		// display date			
	
			}	
		}
	} // END TimeScale Class

	
	// SUB-CLASS DEFINITION: NAVIGATION 
	// This class holds all of the elements associated to navigation of the data
	public class Navigation {
		
		public Navigation() {
		}

		public void drawNav() {
			int navColor = 230;
			smooth();

			// TOP Navigation Bar
			stroke(230);
			strokeCap(ROUND);
			strokeWeight(StaticData.barThickness);
			for (int i = StaticData.navMargin; i < width-StaticData.navMargin; i = i + StaticData.barThickness) {
				line(i, StaticData.navLocation, i, StaticData.navLocation+StaticData.navHeight);
			}
			fill(StaticData.timeStampColor);
			textFont(font, 16);
			text("timeline navigation", StaticData.navMargin+5, StaticData.navLocation+StaticData.navHeight-5);
		}
	} // END Navigation Class

	
	// SUB-CLASS DEFINITION: DataField Class - Extension of FieldData class
	public class DataField extends DataStructure {
		
		public DataField(String _fieldName, int _color, int xloc, int yloc, int _lineThickness, int _height, int _fieldNumber) {
			super(_fieldName, _color, xloc, yloc, _lineThickness, _height, _fieldNumber);
		}

		public void drawField(int dataDisplayOffset, int dataDisplayEnd) {
			int titleSize = 20;
			int fieldBackColor = myColor - 0xed000000;

			// datafield background 
			fill(fieldBackColor);
			noStroke();	
			rect(0, dataBarPosition.y-(dataBarDimension.y+2), width, dataBarDimension.y+4);

			// Data Field Graph
			strokeCap(ROUND);
			stroke(myColor);
			fill(myColor);
			// Data Draw Loop
			for (int curReading = dataDisplayOffset; curReading < dataDisplayEnd; curReading++) {
				int barXPos = (int) (dataBarPosition.x + ((curReading - dataDisplayOffset) * dataBarDimension.x));						// calculate x coordinate using the offset variable
				float barHeight = map(adjValues.get(curReading), adjSmallestValueEver, adjBiggestValueEver, 0, dataBarDimension.y);
				// use line approach
				strokeWeight(dataBarDimension.x);
				line(barXPos, (dataBarPosition.y-barHeight), barXPos, dataBarPosition.y);
			}

			// Data Field Name
			float textYpos = dataBarPosition.y - (fieldHeight*textDisplayLocationPercent);
			fill(StaticData.fieldTitleColor);														// set fill color
			textFont(font, titleSize);													// set font type and size
			text(fieldName + "      ", dataBarPosition.x, textYpos);							// write field names to screen

		}
	}  // END SUB-CLASS DEFINITION: DataFields

	

}

