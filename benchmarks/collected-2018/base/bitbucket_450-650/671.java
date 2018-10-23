// https://searchcode.com/api/result/52546584/

package net.theroyalwe.wtfism_android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.*;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MyActivity extends Activity implements SurfaceHolder.Callback
{


    private static String TAG = "wtfism";
    byte[] pictureData;
    Matrix matrix = new Matrix();


    EditText nameText;// = (EditText)findViewById(R.id.name);
    EditText descriptionText;// = (EditText)findViewById(R.id.description);
    Button addContainerButton;// = (Button)findViewById(R.id.addContainer);
    Button addThingButton;// = (Button)findViewById(R.id.addThing);
    Spinner containerDropDown;// = (Spinner)findViewById(R.id.conatinerDropDown);
//    AutoCompleteTextView locationText;
//    AutoCompleteTextView containerText;
//    AutoCompleteTextView thingText;



    //a variable to store a reference to the Image View at the main.xml file
    private ImageView iv_image;
    //a variable to store a reference to the Surface View at the main.xml file
    private SurfaceView sv;

    //a bitmap to display the captured image
    Bitmap bmp;
    Bitmap rotatedBmp;

    BitmapFactory.Options options = new BitmapFactory.Options();

    //Camera variables
    //a surface holder
    private SurfaceHolder sHolder;
    //a variable to control the camera
    private Camera mCamera;
    //the camera parameters
    private Parameters parameters;

    private ArrayList<WtfContainer> containers = new ArrayList<WtfContainer>();
    private ArrayList<WtfLocation> locations = new ArrayList<WtfLocation>();
    private ArrayList<WtfThing> things = new ArrayList<WtfThing>();
    private ArrayList<WtfThingInstance> thingInstances = new ArrayList<WtfThingInstance>();


    HashMap<String, Object> allWtf = new HashMap<String, Object>();



    private ArrayList<String> conatinerDropDownList = new ArrayList<String>();
//    final Spinner containerDropDown;// = (Spinner)findViewById(R.id.conatinerDropDown);

//    EditText nameText;// = (EditText)findViewById(R.id.name);
//    EditText descriptionText;// = (EditText)findViewById(R.id.description);
//    Button addContainerButton;// = (Button)findViewById(R.id.addContainer);
//    Button addThingButton;// = (Button)findViewById(R.id.addThing);

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);



        containerDropDown = (Spinner)findViewById(R.id.conatinerDropDown);
//        containerDropDown.setDropDownViewResource(android.R.layout.simple_spinner_item);

        nameText = (EditText)findViewById(R.id.name);
        descriptionText = (EditText)findViewById(R.id.description);
        addContainerButton = (Button)findViewById(R.id.addContainer);
        addThingButton = (Button)findViewById(R.id.addThing);


        nameText.setText("foo");
        descriptionText.setText("HUH?");

        //get the Image View at the main.xml file
        iv_image = (ImageView) findViewById(R.id.imageView);

        //get the Surface View at the main.xml file
        sv = (SurfaceView) findViewById(R.id.surfaceView);

        //Get a surface
        sHolder = sv.getHolder();

        //add the callback interface methods defined below as the Surface View callbacks
        sHolder.addCallback(this);

        //tells Android that this surface will have its data constantly replaced
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        /*locationText = (AutoCompleteTextView)findViewById(R.id.location);
        containerText = (AutoCompleteTextView)findViewById(R.id.container);
        thingText = (AutoCompleteTextView)findViewById(R.id.thing);*/


        options.inSampleSize = 5;   //reduce the image by 1/5 its original size


        matrix.postRotate(90);


        sv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mCamera.takePicture(null, null, mCall);
                //mCamera.stopPreview();
                //mCamera.startPreview();
            }
        });

        addContainerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new submitNewTask().execute("container");

            }
        });

        addThingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new submitNewTask().execute("thing");
            }
        });

        Button clearNameButton = (Button)findViewById(R.id.clearNameText);
        clearNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nameText.setText("");
            }
        });

        Button clearDescriptionButton = (Button)findViewById(R.id.clearDescriptionText);
        clearDescriptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                descriptionText.setText("");
            }
        });
        /*Button clearLocationButton = (Button)findViewById(R.id.clearLocationText);
        clearLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationText.setText("");
            }
        }); */



         Log.v(TAG, "WTF fucking work goddamn it!");
        new getAllTask().execute("containers");



    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3)
    {
        Log.v(TAG, "surfaceChanged");
        //get camera parameters
        parameters = mCamera.getParameters();
        //parameters.set("orientation", "portrait");
          mCamera.setDisplayOrientation(90);
        //parameters.set("rotation", 90);
        //parameters.setRotation(90);

        //set camera parameters
        mCamera.setParameters(parameters);
        mCamera.startPreview();




    }




    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        // The Surface has been created, acquire the camera and tell it where
        // to draw the preview.
        mCamera = Camera.open();

        Log.v(TAG, "I guess I opened the camera...");

        try {

            mCamera.setPreviewDisplay(holder);

        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            finish();
            return;
        }



    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        //stop the preview
        mCamera.stopPreview();
        //release the camera
        mCamera.release();
        //unbind the camera from this object
        mCamera = null;
    }



    //sets what code should be executed after the picture is taken
    Camera.PictureCallback mCall = new Camera.PictureCallback()
    {
        @Override
        public void onPictureTaken(byte[] data, Camera camera)
        {
            mCamera.stopPreview();
            //decode the data obtained by the camera into a Bitmap



            if(bmp != null){
                bmp.recycle();

            }
            if(rotatedBmp != null){
                rotatedBmp.recycle();
            }


            //a very large, unscalled image
            //bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);



            int w = bmp.getWidth();
            int h = bmp.getHeight();
            Log.v(TAG, Integer.toString(w)+":"+Integer.toString(h));
            //iv_image.setImageBitmap(bmp);

            rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true);


            //set the iv_image
            iv_image.setImageBitmap(rotatedBmp);
            //iv_image.setMaxWidth(89);
            //iv_image.setMaxHeight(89);



            mCamera.startPreview();
        }
    };





    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.v(TAG, "OnShutter called!");
        }
    };





    private void updateUI(){
        populateDropDownList();
        containerDropDown.setVisibility(View.VISIBLE);
    }

    private void populateDropDownList(){

        for(WtfContainer c:containers){
            Log.v(TAG, "Adding "+c.getName());
              conatinerDropDownList.add(c.getName());
        }
        ArrayAdapter<String> containerSpinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, conatinerDropDownList);
        containerSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        containerDropDown.setAdapter(containerSpinnerArrayAdapter);

    }
    private class getAllTask extends AsyncTask<String, Integer, Boolean>{

        @Override
        protected void onPostExecute(Boolean result){
            //update dropdown list...
             //Log.v(TAG, containers.toArray());
             populateDropDownList();

        }

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.v(TAG, params[0]);
            String type = params[0];
            try {
                if(type.equals("all")){

                    allWtf = new WtfismApi().getAllWtfObjects();
                    containers = (ArrayList<WtfContainer>)allWtf.get("containers");
                    locations = (ArrayList<WtfLocation>)allWtf.get("locations");
                    things = (ArrayList<WtfThing>)allWtf.get("things");
                    thingInstances = (ArrayList<WtfThingInstance>)allWtf.get("thinginstances");
                }
                if(type.equals("containers")){

                    containers = new WtfismApi().getAllContainers();
                }




            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }






            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }



    }
    /*
    private class subitNewTask extends AsyncTask<String, Integer, Boolean>{

        @Override
        protected void onPostExecute(Boolean result){
            //update dropdown list...
            //Log.v(TAG, containers.toArray());
            populateDropDownList();

        }

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.v(TAG, params[0]);
            String type = params[0];
            try {
                if(type.equals("containers")){

                    //get the name, desription, use 0 for location
                    int location = 0;

                    String description = descriptionText.getText().toString();
                    String name = nameText.getText().toString();


                    new WtfismApi().createNewContainer(location, description, name, );
                }




            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }






            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }



    } */


    private class submitNewTask extends AsyncTask<String, Integer, Boolean>{
        String name;
        String description;
        Integer location;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();


        @Override
        protected void onPostExecute(Boolean result){

        }

        @Override
        protected void onPreExecute(){

            location = 0;

            description = descriptionText.getText().toString();
            name = nameText.getText().toString();
            rotatedBmp.compress(Bitmap.CompressFormat.PNG, 90, byteArrayOutputStream);
            pictureData = null;
            pictureData = byteArrayOutputStream.toByteArray();

        }

        @Override
        protected Boolean doInBackground(String... strings) {
            String type = strings[0];
            try {
                if(type.equals("container")){
                    new WtfismApi().createNewContainer(location, description, name, pictureData);
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }








}
