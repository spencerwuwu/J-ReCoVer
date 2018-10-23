// https://searchcode.com/api/result/70479067/

package ismat.informatica.trabalhoFinal;




import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
//import org.opencv.samples.colorblobdetect.ColorBlobDetector;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.R.bool;
import android.R.color;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.text.style.LineHeightSpan.WithDensity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

public class TrabalhoFinalActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    
    //BLUEETOOTH
    private static final int REQUEST_CONNECT_DEVICE = 2;
	public BluetoothSocket btSocket = null;
	public BluetoothAdapter mBluetoothAdapter = null;
	public OutputStream btOutputStream = null;
	public InputStream mmInputStream = null;
	boolean ligado = false;

    int REQUEST_ENABLE_BT = 0;
    
    //App Options
    public static final int     VIEW_MODE_RGBA  		= 0;
    public static final int     VIEW_MODE_GRAY  		= 1;
    public static final int     VIEW_MODE_CANNY 		= 2;
    public static final int     VIEW_MODE_SINGLE_LINE 	= 3;
    public static final int     VIEW_MODE_DOUBLE_LINE 	= 4;
    public static final int 	VIEW_MODE_BLUE_CIRCLE 	= 5;
    public static final int 	VIEW_MODE_RED_CIRCLE 	= 6;
    public static final int		VIEW_MODE_SMART_CIRCLE 	= 7;
    
    //Camera View Options
    private TrabalhoFinalView mOpenCvCameraView;
    //Color hsv values detection
    private TrabalhoFinalColorHsv hsvColorsValue;

    //Menu Items
    private List<Size> 	mResolutionList;
    private SubMenu 	mPreviewModes;
    private MenuItem[] 	mItemsPreview;
    private SubMenu 	mLaneTracking;
    private MenuItem[]  mItemsLaneTracking;
    private SubMenu 	mSmartPick;
    private MenuItem[]	mItemsSmartPick;
    private MenuItem[]	mResolutionMenuItems;
    private SubMenu 	mResolutionMenu;
    
    //Variables Mats
    private Mat                  mRgba;
    private Mat                  mGray;
    private Mat                  mIntermediateMat;
    private Mat					 roiLaneTrack;
    //Ball
    private Mat					 mHsv;
    private Mat					 mHsvThreshed;
    //Ball colors
    private Scalar               mBlobColorHsv;
    //cvScalar(100, 135, 135), cvScalar(140, 255, 255)
    private Scalar				 blueBallLower;
    private Scalar				 blueBallUpper;
    
    
    private Scalar				 redBallLower;
    private Scalar				 redBallUpper;
   //Circles
    Point 						 pt;
    int 						 radius;
    
    //Variables Sizes
    private int 				mRgbaRow;
    private int 				mRgbaCol;
    private int 				colorSelect=0;
    private int					found=0;//Used for get the count of cicles detected (3 frames)
    private int					findBlockedPath=0;
	

    //Lines Variables(Allocate here for speed purposes) If allocated in frames i loose 1 fps (Used on all algorithms)
    /*private  Mat lines;//Allocated in start preview
  
    private Mat leftLines;
    private Mat rightLines;
    private int threshold = 15;
    private int minLineSize = 20;
    private int lineGap = 20;
    private int rightLineNum=0;
    private int leftLineNum=0;
    private Point leftStart = new Point();
    private Point leftEnd = new Point();
    private Point rightStart = new Point();
    private Point rightEnd = new Point();
    private double tanLeft = 0.0;
    private double tanRight = 0.0;
    private double tanTemp;
    private double leftMaxLen = 0.0;
    private double rightMaxLen = 0.0;
    private double tempLen = 0.0;*/
    
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(TrabalhoFinalActivity.this);
                    mOpenCvCameraView.enableFpsMeter();
                    
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public TrabalhoFinalActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial3_surface_view);

        mOpenCvCameraView = (TrabalhoFinalView) findViewById(R.id.tutorial3_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    	//Starts With Rgba
    	mGray = new Mat();
        mRgba = new Mat(width,height,CvType.CV_8UC4);
        mRgbaRow = height;
        mRgbaCol = width;
        mIntermediateMat = new Mat();
        //Lane
        //roiLaneTrack = new Mat(height/2,width,CvType.CV_8UC4);
        
        //Ball
        mHsv = new Mat();
        mHsvThreshed = new Mat();
        //Ball Color
        if(hsvColorsValue==null)
        hsvColorsValue = new TrabalhoFinalColorHsv();
        mBlobColorHsv = new Scalar(255);
        //Ball circle
        pt= new Point();
       
    }

    public void onCameraViewStopped() {
    	
    	 if (mRgba != null)
             mRgba.release();
         if (mGray != null)
             mGray.release();
         if (mIntermediateMat != null)
             mIntermediateMat.release();

         mRgba = null;
         mGray = null;
         mIntermediateMat = null;
    	
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	//Start color space
    	mRgba = inputFrame.rgba();
    	
    	switch (mOpenCvCameraView.viewMode) {
    		
    	case VIEW_MODE_RGBA:
    		break;
    		
    	case VIEW_MODE_GRAY:
    		Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY);
    		mGray.copyTo(mRgba);
    		break;
    		
    	case VIEW_MODE_CANNY:
    		Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY,4);
    		Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
    		Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA,4);
    		break;
    	
    	case VIEW_MODE_DOUBLE_LINE:
    		
    		//Work with Half image to save resources(Care only about the road part of the image)
    		//roiLaneTrack = mRgba.submat(mRgbaRow/2, mRgbaRow, 0, mRgbaCol);
    		//roiLaneTrack(mRgba,new Rect(mRgbaRow/2, mRgbaRow, 0, mRgbaCol));
    			
    		//BAHHHHHHHHHHHHHHHHHHHHHHHH!!! ROI = x,y Start location of rectagle x1,y1 size of rectangle bahhhhhhhhhhhhhhh!!!
    		Rect roi = new Rect(0,mRgbaRow/2,mRgbaCol, (mRgbaRow/2));
    		roiLaneTrack = new Mat(mRgba, roi);
    		
    		//method that improves the contrast in an image, in order to stretch out the intensity range.(if enough resources)###### *Quality Filter
    		//Imgproc.equalizeHist(mGray, mGray);
    		
    		Imgproc.cvtColor(roiLaneTrack, mGray, Imgproc.COLOR_RGB2GRAY,4);
    		// Imgproc.pyrDown //Downsample Images
    		//Test Threshold in environment for better results
    		Imgproc.Canny(mGray, mIntermediateMat, 100,140);
    		
    		 Mat lines = new Mat();//Allocated in start preview
    		  
    	     Mat leftLines;
    	     Mat rightLines;
    	     int threshold = 15;
    	     int minLineSize = 20;
    	     int lineGap = 20;
    	     int rightLineNum=0;
    	     int leftLineNum=0;
    	     Point leftStart = new Point();
    	     Point leftEnd = new Point();
    	     Point rightStart = new Point();
    	     Point rightEnd = new Point();
    	     double tanLeft = 0.0;
    	     double tanRight = 0.0;
    	     double tanTemp = 0.0;;
    	     double leftMaxLen = 0.0;
    	     double rightMaxLen = 0.0;
    	     double tempLen = 0.0;	
    		
    		
    		
    		//Probabilistic Hought Transform
    		Imgproc.HoughLinesP(mIntermediateMat, lines, 7, Math.PI/180, threshold, minLineSize, lineGap);
    		
    		int matType = lines.type();
            leftLines = new Mat(lines.rows(),lines.cols(),matType);
            rightLines = new Mat(lines.rows(),lines.cols(),matType);
            
            for (int x = 0; x < lines.cols(); x++){
                  double[] vec = lines.get(0, x);
                  double x1 = vec[0], 
                         y1 = vec[1],
                         x2 = vec[2],
                         y2 = vec[3];
                  Point start = new Point(x1, y1);
                  Point end = new Point(x2, y2);
                  double tan = (y1 - y2)/(x1 - x2);
                  tempLen = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2);
                  if(tan<0 && tempLen>leftMaxLen){
                	  
                	  leftLines.put(0, leftLineNum, vec);
                	  leftMaxLen = tempLen;
                	  leftLineNum++;
                  }
                  if(tan>0 && tempLen>rightMaxLen){
                	  rightLines.put(0, rightLineNum, vec);
               		  rightMaxLen = tempLen;
               		  rightLineNum++;
                  }
            }
            Log.d("mycv", "num = " + lines.cols());
            for(int x=1; (x < 15)&&(leftLineNum-x)>=0;x++){
            	double[] vec = leftLines.get(0, leftLineNum-x);
                double x1 = vec[0], 
                       y1 = vec[1],
                       x2 = vec[2],
                       y2 = vec[3];
                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                tanTemp = (y1 - y2)/(x1 - x2);
                if(tanTemp < tanLeft){
              	  tanLeft = tanTemp;
              	  leftStart = start;
              	  leftEnd = end;
                }
            }
            for(int x=1; (x < 15)&&(rightLineNum-x)>=0;x++){
            	double[] vec = rightLines.get(0, rightLineNum-x);
                double x1 = vec[0], 
                       y1 = vec[1],
                       x2 = vec[2],
                       y2 = vec[3];
                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                tanTemp = (y1 - y2)/(x1 - x2);
                if(tanTemp > tanRight){
              	  tanRight = tanTemp;
              	  rightStart = start;
              	  rightEnd = end;
                }
            }
                       
            
            //Draws the resuslts of canny
            //Coment line to display in rgba
            //Imgproc.cvtColor(mIntermediateMat, roiLaneTrack, Imgproc.COLOR_GRAY2RGBA,4);
            
            
            
            Core.line(roiLaneTrack, leftStart, leftEnd, new Scalar(255,0,0), 3);    //Print the left lane
            Core.line(roiLaneTrack, rightStart, rightEnd, new Scalar(0,255,0), 3);    //Print the right lane
 	
            break;
    	case VIEW_MODE_SINGLE_LINE:
    		
    		break;
    		
    	case VIEW_MODE_BLUE_CIRCLE:
    		//Start tracking saved color
    		
    		
    		
    		Imgproc.cvtColor(mRgba, mHsv, Imgproc.COLOR_RGB2HSV_FULL);//RGB2HSV
        	
        	//Max Hue is 180 for RGB2HSV // // Holds the Blue  thresholded image 
        	//Core.inRange(mHsv, new Scalar(100, 100, 100), new Scalar(120, 255, 255), mHsvThreshed);
    		//Probably i should made 2 passages with cvtcolor #########################
    		
    		Core.inRange(mHsv, blueBallLower, blueBallUpper, mHsvThreshed);
    		//DownSamples
    		
    		//Smooth the image for better tracking
    		Imgproc.GaussianBlur(mHsvThreshed, mHsvThreshed,new org.opencv.core.Size(9,9),0.0);
    		
    		Mat circles = new Mat();
    		
    		int iCannyUpperThreshold = 70;//70
    		int iMinRadius = 10;
    		int iMaxRadius = (int) ((int)mHsvThreshed.rows()*0.5);
    		int iAccumulator = 155;//100 is good or 150
    		//http://docs.opencv.org/doc/tutorials/imgproc/imgtrans/hough_circle/hough_circle.html
    	//	Imgproc.Canny(mHsvThreshed, mHsvThreshed, 100, 140);
    		Imgproc.HoughCircles(mHsvThreshed, circles, Imgproc.CV_HOUGH_GRADIENT,1.0,mHsvThreshed.rows(),iCannyUpperThreshold,iAccumulator, iMinRadius, iMaxRadius);		
    		
    		
    		if (circles.cols() > 0)
    		    for (int x = 0; x < circles.cols(); x++) 
    		        {
    		        double vCircle[] = circles.get(0,x);

    		        if (vCircle == null )
    		            break;
    		        ;
	    		        
		    		        pt.x=Math.round(vCircle[0]);
		    		        pt.y= Math.round(vCircle[1]);
		    		        radius =(int)Math.round( vCircle[2]);
	    		        
	    		     // draw the found circle
	    		        Core.circle(mRgba, pt, radius, new Scalar(0,255,0), 2);
	    		        Core.circle(mRgba, pt, 3, new Scalar(0,0,255), 4);
	    		        
    		        }
    		
    			Imgproc.cvtColor(mHsvThreshed, mRgba, Imgproc.COLOR_GRAY2RGB, 0);
    		        
    		break;
    		
    		case VIEW_MODE_RED_CIRCLE:
        		redCircle();        		        
        		break;
        		
    		case VIEW_MODE_SMART_CIRCLE:
    			
    			
    			//if no blue circles are found in 3 consecutive frames keep searching(Reduce false positives)
    			if(found < 3 )
    			{
    			//find balls in upper part of the screen
    			Rect roiBall = new Rect(0,0,mRgbaCol, (mRgbaRow/2));
        		roiLaneTrack = new Mat(mRgba, roiBall);
        		
        		//first Detect Blue balls
        		
        		Imgproc.cvtColor(roiLaneTrack, mHsv, Imgproc.COLOR_RGB2HSV_FULL);//RGB2HSV
        		Core.inRange(mHsv, blueBallLower, blueBallUpper, mHsvThreshed);
        		Imgproc.GaussianBlur(mHsvThreshed, mHsvThreshed,new org.opencv.core.Size(9,9),0.0);
        		Mat circlesS = new Mat();  		
        		int iCannyUpperThresholdS = 70;
        		int iMinRadiusS = 10;
        		int iMaxRadiusS = (int) ((int)mHsvThreshed.rows()*0.9); //0.9 has here matrix has half size 
        		int iAccumulatorS = 150;//100 is good
        		//http://docs.opencv.org/doc/tutorials/imgproc/imgtrans/hough_circle/hough_circle.html
        	//	Imgproc.Canny(mHsvThreshed, mHsvThreshed, 100, 140);
        		Imgproc.HoughCircles(mHsvThreshed, circlesS, Imgproc.CV_HOUGH_GRADIENT,2.0,mHsvThreshed.rows(),iCannyUpperThresholdS,iAccumulatorS, iMinRadiusS, iMaxRadiusS);		
        		//Testing
        		//Imgproc.cvtColor(mHsvThreshed, roiLaneTrack, Imgproc.COLOR_GRAY2RGBA, 4);
        		if (circlesS.cols() > 0)
        		    for (int x = 0; x < circlesS.cols(); x++) 
        		        {
        		        double vCircle[] = circlesS.get(0,x);

		        		        if (vCircle == null )
		        		            break;
		        		        
    	    		        
    		    		        pt.x=Math.round(vCircle[0]);
    		    		        pt.y= Math.round(vCircle[1]);
    		    		        radius = (int) Math.round( vCircle[2]);    		    		        
	    	    		     // draw the found circle
	    	    		        Core.circle(roiLaneTrack, pt, radius, new Scalar(0,255,0), 2);//to rgba
	    	    		        Core.circle(roiLaneTrack, pt, 3, new Scalar(0,0,255), 4); //to rgba
	    	    		        found++;//Found a circle 
        		        }
        		//If found blue balls split screen in 2 (at least in 3 frames)
        		
        		
        		
    			}else if (found >= 3) // check if path is blocked
    			{
    				
    				//Detecting Blocked Paths
    				
    				Rect roiBallInterruptLeft = new Rect(0,0,mRgbaCol/2, 0);
            		Rect roiBallInterruptRight = new Rect(mRgbaCol/2,0,mRgbaCol, (mRgbaRow/2));
            		
            		Mat roiLaneTrackLeft = new Mat(roiLaneTrack,roiBallInterruptLeft);
            		Mat roiLaneTrackRight = new Mat(roiLaneTrack,roiBallInterruptRight);
    				
    				
            		
    				//Search for blocked paths also in 3 frames
    				findBlockedPath++;
    				
    				if(findBlockedPath==3)
    					found=0;    				
    			}
        		
        			
        			
        		
    		break;
    	}
    	

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	int idx = 0;
    	//Resolutions Buttons
        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(1, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
         }
        //Preview Options
        mPreviewModes = menu.addSubMenu("Preview Modes");
        mItemsPreview = new MenuItem[3];
        mItemsPreview[0] = mPreviewModes.add(2,0,Menu.NONE,"RGB");
        mItemsPreview[1] = mPreviewModes.add(2,1,Menu.NONE,"GRAY");
        mItemsPreview[2] = mPreviewModes.add(2,2,Menu.NONE,"CANNY");
        
        //Lane Tracking options
        mLaneTracking = menu.addSubMenu("Lane TRacking");
        mItemsLaneTracking = new MenuItem[2];
        mItemsLaneTracking[0] = mLaneTracking.add(3,0,Menu.NONE,"Double Line");
        mItemsLaneTracking[1] = mLaneTracking.add(3,1,Menu.NONE,"Single Line");
        
        //Objects Tracking options
        mSmartPick = menu.addSubMenu("Smart Pick");
        mItemsSmartPick = new MenuItem[3];
        mItemsSmartPick[0] = mSmartPick.add(4,0,Menu.NONE,"Blue Circles");
        mItemsSmartPick[1] = mSmartPick.add(4,1,Menu.NONE,"Red Circles");
        mItemsSmartPick[2] = mSmartPick.add(4,2,Menu.NONE,"Smart Circles Position");
        
        return true;
    }

    //Change Views
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        

        
        if (item.getGroupId() == 1)
        {
        	 int id = item.getItemId();
             Size resolution = mResolutionList.get(id);
             mOpenCvCameraView.setResolution(resolution);
             resolution = mOpenCvCameraView.getResolution();
             String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
             Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
             //If user change resolution change important variables
             onCameraViewStarted( resolution.width ,resolution.height );
        }
        else if (item.getGroupId() == 2)
        {
        	
        	int id = item.getItemId();
        	
        	switch(id)
        	{
        	case 0:
        		mOpenCvCameraView.viewMode = VIEW_MODE_RGBA;
        		Toast.makeText(this, "RGBA MODE", Toast.LENGTH_SHORT).show();
        		break;
        	case 1:
        		mOpenCvCameraView.viewMode = VIEW_MODE_GRAY;
        		Toast.makeText(this, "GRAY MODE", Toast.LENGTH_SHORT).show();
        		break;
        	case 2:
        		mOpenCvCameraView.viewMode = VIEW_MODE_CANNY;
        		Toast.makeText(this, "CANNY MODE", Toast.LENGTH_SHORT).show();
        		break;
        	}
           
        }
        else if (item.getGroupId() == 3)
        {
        	      	
        	int id = item.getItemId();
        	
        	switch(id)
        	{
        	case 0:
        		mOpenCvCameraView.viewMode = VIEW_MODE_DOUBLE_LINE;
        		Toast.makeText(this,"Dtetect Double Lines", Toast.LENGTH_SHORT).show();
        		break;
        	case 1:
        		mOpenCvCameraView.viewMode = VIEW_MODE_SINGLE_LINE;
        		Toast.makeText(this, "Detect Single Lines", Toast.LENGTH_SHORT).show();
        		break;
        	}
        	
        }
        else if (item.getGroupId() == 4)
        {

        	int id = item.getItemId();
        	
        	switch(id)
        	{
        	case 0:
        		mOpenCvCameraView.viewMode = VIEW_MODE_BLUE_CIRCLE;
        		Toast.makeText(this, "Detect Blue Ball", Toast.LENGTH_SHORT).show();
        		break;
        	case 1:
        		mOpenCvCameraView.viewMode = VIEW_MODE_RED_CIRCLE;
        		Toast.makeText(this, "Detect Red Ball", Toast.LENGTH_SHORT).show();
        		break;
        	case 2:
        		mOpenCvCameraView.viewMode = VIEW_MODE_SMART_CIRCLE;
        		Toast.makeText(this, "Smart Ball Position Detection", Toast.LENGTH_SHORT).show();
        		break;
        	}
        	
        }

        return true;
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		 	int cols = mRgba.cols();
	        int rows = mRgba.rows();
	        
	        
	       
	        
	        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
	        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
	        
	        int x = (int)event.getX() - xOffset;
	        int y = (int)event.getY() - yOffset;

	        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

	        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

	        Rect touchedRect = new Rect();

	        touchedRect.x = (x>4) ? x-4 : 0;
	        touchedRect.y = (y>4) ? y-4 : 0;

	        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
	        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

	        Mat touchedRegionRgba = mRgba.submat(touchedRect);

	        Mat touchedRegionHsv = new Mat();
	        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

	        // Calculate average color of touched region
	        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
	        int pointCount = touchedRect.width*touchedRect.height;
	        for (int i = 0; i < mBlobColorHsv.val.length; i++)
	            mBlobColorHsv.val[i] /= pointCount;

	        Log.i(TAG, "Touched hsv color: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
	                ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");

	        hsvColorsValue.setHsvColor(mBlobColorHsv);
	        touchedRegionRgba.release();
	        touchedRegionHsv.release();
	       
	        if(colorSelect==0)
	        {
		        blueBallLower = hsvColorsValue.getLowerBound();
	    		blueBallUpper = hsvColorsValue.getUppperBound();
	    		colorSelect = 1;
	    		 Toast.makeText(getApplicationContext(), "Blue Color adquired", Toast.LENGTH_SHORT).show();
	        }else{
	        	redBallLower = hsvColorsValue.getLowerBound();
				redBallUpper = hsvColorsValue.getUppperBound();
				colorSelect = 0;
				 Toast.makeText(getApplicationContext(), "Red Color adquired", Toast.LENGTH_SHORT).show();
	        }
	        return false; // don't need subsequent touch events

		
	}     
	//My simplifying functions
	
	//red color
	private void redCircle()
	{
		//Start tracking saved color
			
		Imgproc.cvtColor(mRgba, mHsv, Imgproc.COLOR_RGB2HSV_FULL);//RGB2HSV
    	
    	//Max Hue is 180 for RGB2HSV // // Holds the Blue  thresholded image 
    	//Core.inRange(mHsv, new Scalar(100, 100, 100), new Scalar(120, 255, 255), mHsvThreshed);
		//Probably i should made 2 passages with cvtcolor #########################
		Core.inRange(mHsv, redBallLower, redBallUpper, mHsvThreshed);
		//Smoth the image for better tracking
		Imgproc.GaussianBlur(mHsvThreshed, mHsvThreshed,new org.opencv.core.Size(11,11),0.0);
		
		//RESULTS VARY FROM DEVICE TO DEVICE + RESOLUTION FOR EACH DEVICE MINIMIUM RESOLUTION
		//Galaxy S3-> 70/10/mHsvThreshed.rows()*0.3/150
		//NEXUS  7 ->
		//NEXUS	 4 ->
		Mat circlesR = new Mat();
		
		int iCannyUpperThresholdR = 70;
		int iMinRadiusR = 10;
		int iMaxRadiusR = (int) ((int)mHsvThreshed.rows()*0.3);
		int iAccumulatorR = 150;//100 is good
		//http://docs.opencv.org/doc/tutorials/imgproc/imgtrans/hough_circle/hough_circle.html
	//	Imgproc.Canny(mHsvThreshed, mHsvThreshed, 100, 140);
		Imgproc.HoughCircles(mHsvThreshed, circlesR, Imgproc.CV_HOUGH_GRADIENT,2.0,mHsvThreshed.rows(),iCannyUpperThresholdR,iAccumulatorR, iMinRadiusR, iMaxRadiusR);		
		
		
		if (circlesR.cols() > 0)
		    for (int x = 0; x < circlesR.cols(); x++) 
		        {
		        double vCircle[] = circlesR.get(0,x);

		        if (vCircle == null )
		            break;
		        ;
    		        
	    		        pt.x=Math.round(vCircle[0]);
	    		        pt.y= Math.round(vCircle[1]);
	    		        radius =(int)Math.round( vCircle[2]);
    		        
	    		        //Imgproc.cvtColor(mHsvThreshed, mRgba, Imgproc.COLOR_GRAY2RGB, 0);
    		     // draw the found circle
    		        Core.circle(mRgba, pt, radius, new Scalar(0,255,0), 2);
    		        Core.circle(mRgba, pt, 3, new Scalar(0,0,255), 4);
    		        
		        }
		
		    //   Imgproc.cvtColor(mHsvThreshed, mRgba, Imgproc.COLOR_GRAY2RGB, 0);
	}
	
	//BLUEETOOTH STUFF
	/*
	##################################################################################################################
	####################### Metodos ##################################################################################
	##################################################################################################################
*/

	    /**
	     * 
	     *
	    public void Verificar_Bluetouth() {
	    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    	if (mBluetoothAdapter == null) {
	    		Toast.makeText(TrabalhoFinalActivity.this, "O seu dispositivo nao possui Bluetooth", Toast.LENGTH_SHORT).show();
	    	}
	    	else{
	    		if (!mBluetoothAdapter.isEnabled()) {
	    		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	    		    ligado = true;
	    		}
	    		else
	    			ligado = true;
	    	}
	    }
	    
	    private void Fechar_ligacao()  {
	        if (btSocket != null) {
	                try {btSocket.close();} catch (Exception e) {}
	                btSocket = null;              
	        }

	        if (btOutputStream != null) {
	            try {btOutputStream.close();} catch (Exception e) {}
	            btOutputStream = null;              
	       }
	        conectado = false;
	        metodo_controlo = 0;
	        Toast.makeText(TrabalhoFinalActivity.this, "Ligacao terminada", Toast.LENGTH_SHORT).show();
	    }
	    /*******************************************************/
	    /********************************************************/
	    
	    
	    
	    
	    
	    
	    
	    

		/*******************************************************/
		/************** Conectar ao dispositivo ****************/
		/*******************************************************
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (requestCode == REQUEST_CONNECT_DEVICE) {
				if (data != null) {
					getOutputStreamOfBTDevice(data.getStringExtra("device_address"));
				}
			}
		}

		private OutputStream getOutputStreamOfBTDevice(String address) {

			// Get local Bluetooth adapter
			if (address != null && address.length() > 0) {
				BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				BluetoothDevice mbtDevice = mBluetoothAdapter.getRemoteDevice(address);
				try {
					btSocket = mbtDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					btSocket.connect();
					// Get the BluetoothSocket output streams
					btOutputStream = btSocket.getOutputStream();
					mmInputStream = btSocket.getInputStream();
					conectado = true;
					Toast.makeText(this, "Ligacao estabelecida",Toast.LENGTH_SHORT).show();
					beginListenForData();
				} catch (IOException e) {
					conectado = false;
					Toast.makeText(this, "Falhou ao tentar ligar",Toast.LENGTH_SHORT).show();
					return btOutputStream;
				}
				
			}
			
			return btOutputStream;

		}	
		/*******************************************************/
		/*******************************************************/
	    
	    
	    
		
		
		/****************** Receber valores ********************/
		/*******************************************************
		void beginListenForData()
	    {
	        final Handler handler = new Handler(); 
	        final byte delimiter = 10; //This is the ASCII code for a newline character
	        
	        stopWorker = false;
	        readBufferPosition = 0;
	        readBuffer = new byte[1024];
	        workerThread = new Thread(new Runnable()
	        {
	            public void run()
	            {                
	               while(!Thread.currentThread().isInterrupted() && !stopWorker)
	               {
	                    try 
	                    {
	                        int bytesAvailable = mmInputStream.available();                        
	                        if(bytesAvailable > 0)
	                        {
	                            byte[] packetBytes = new byte[bytesAvailable];
	                            mmInputStream.read(packetBytes);
	                            for(int i=0;i<bytesAvailable;i++)
	                            {
	                                byte b = packetBytes[i];
	                                if(b == delimiter)
	                                {
	                                    byte[] encodedBytes = new byte[readBufferPosition];
	                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
	                                    final String data = new String(encodedBytes, "US-ASCII");
	                                    readBufferPosition = 0;
	                                    handler.post(new Runnable()
	                                    {
	                                        public void run()
	                                        {
	                                            myLabel.setText(data);
	                                        }
	                                    });
	                                }
	                                else
	                                {
	                                    readBuffer[readBufferPosition++] = b;
	                                }
	                            }
	                        }
	                    } 
	                    catch (IOException ex) 
	                    {
	                        stopWorker = true;
	                    }
	               }
	            }
	        });

	        workerThread.start();
	    }
		/*******************************************************/
		/*******************************************************/ 
	
	
}

