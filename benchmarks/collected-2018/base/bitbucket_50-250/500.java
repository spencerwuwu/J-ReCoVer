// https://searchcode.com/api/result/119949995/

/**
 * Dynadraw GLSurfaceView - View/Controller for the Application
 */
package com.gmail.rallen.dynadraw;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

class DynadrawSurfaceView extends GLSurfaceView implements SensorEventListener {

    private DynadrawRenderer     mRenderer = null;
    private ScaleGestureDetector mScaleDetector = null;
    private SensorManager        mSensorManager = null;
    private Sensor               mAccelerometer;
    private boolean              mMouseIsDown = false;
    private DataSmoother         mAccelMagnitudeWhileDown = null;

    public DynadrawSurfaceView(Context context, DynadrawModel m, Dynadraw d, Handler h, SensorManager sm) {
        super(context);
        Log.d("Dynadraw","DynadrawSurfaceView()");

        // Turn on error-checking and logging -- Warning: Sloooooooowwwwww.
        //setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        // FIXME on release. Just turn on glErrorChecks
        //setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
        setEGLContextClientVersion(2); // OpenGL ES 2.0
        mRenderer = new DynadrawRenderer(m,d,h);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mSensorManager = sm;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelMagnitudeWhileDown = new DataSmoother(5);
    }

    public void onResume() {
        super.onResume();
        Log.d("Dynadraw","DynadrawSurfaceView.onResume()");
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);//FASTEST);//NORMAL);
    }

    public void onPause() {
        super.onPause();
        Log.d("Dynadraw","DynadrawSurfaceView.onPause()");
        mSensorManager.unregisterListener(this);
    }

    public void getBitmapStart() {
        queueEvent(new Runnable(){
            public void run() {
                mRenderer.startGetBitmap();
            }});
    }
    public void reset() {
        queueEvent(new Runnable(){
            public void run() {
                mRenderer.reset();
            }});
    }

    @Override 
    public boolean onTouchEvent(MotionEvent e) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(e);

        final float x = e.getX();
        final float y = e.getY(); // if pass e.getY() in Runnable calls, they are off by ~20 pixels.

        switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (!mScaleDetector.isInProgress()) {
                mMouseIsDown = true;
                mAccelMagnitudeWhileDown.reset();
                queueEvent(new Runnable() {
                    public void run() {
                        mRenderer.onMouseDown(x,y);
                    }});
            }
            break;
        case MotionEvent.ACTION_UP:
            if (!mScaleDetector.isInProgress()) {
                mMouseIsDown = false;
                queueEvent(new Runnable() {
                    public void run() {
                        mRenderer.onMouseUp(x,y,mAccelMagnitudeWhileDown.value());
                    }});
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (!mScaleDetector.isInProgress()) {
                queueEvent(new Runnable() {
                    public void run() {
                        mRenderer.onMouseMoved(x,y);
                    }});
            }
            break;
        }
        return true;
    }

    // Scale Events
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.v("Dynadraw","ScaleListener.onScale()");
            final float s = detector.getScaleFactor();
            final float x = detector.getFocusX();
            final float y = detector.getFocusY();
            queueEvent(new Runnable() {
                public void run() {
                    mRenderer.onScale(s,x,y,false);
                }});            
            return true;
        }
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d("Dynadraw","ScaleListener.onScaleBegin()");
            final float s = detector.getScaleFactor();
            final float x = detector.getFocusX();
            final float y = detector.getFocusY();
            queueEvent(new Runnable() {
                public void run() {
                    mRenderer.onScale(s,x,y,true);
                }});            
            return true;
        }
    }

    // Accelerometer events
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // nothing to do so far.
    }

    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //long currTime = System.currentTimeMillis();
            float [] v = event.values;
            float magnitude = FloatMath.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
            magnitude = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
            if(mMouseIsDown) {
                float smoothed_mag = mAccelMagnitudeWhileDown.smooth(magnitude);
                Log.v("Dynadraw","onSensorChanged! sm="+smoothed_mag+", m="+magnitude+", "+v[0]+", "+v[1]+", "+v[2]);
            } else {
                if(magnitude > 0.2f) // reduce noise a bit
                    Log.v("Dynadraw","onSensorChanged!>.2 "+magnitude+", "+v[0]+", "+v[1]+", "+v[2]);                
            }
        } 
    }

    // I'm not sure if I want to override the normal "onSaveInstanceState"
    public void mySaveInstanceState(Bundle savedInstanceState) {
        Log.d("Dynadraw","DynadrawSurfaceView.mySaveInstanceState()");
        mRenderer.mySaveInstanceState(savedInstanceState);
    }
    // I'm not sure if I want to override the normal "onRestoreInstanceState"
    public void myRestoreInstanceState(Bundle savedInstanceState) {
        Log.d("Dynadraw","DynadrawSurfaceView.myRestoreInstanceState()");
        mRenderer.myRestoreInstanceState(savedInstanceState);
    }
}

