// https://searchcode.com/api/result/53556864/

package calebr.gltest;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * 
 * @author calebr
 */
public class Renderer implements GLSurfaceView.Renderer {
    public Renderer(Context context, int testCaseNo,
    		int columns, int rows) {
    	this.columns = columns;
    	this.rows = rows;
    	
    	this.testCaseNo = testCaseNo;
    	this.context = context;
    	
    	triangles = new TrianglesMatrix();
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);

        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_FASTEST);

        gl.glClearColor(0f, 0f, 0f, 1);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        /*
         * Create our texture. This has to be done each time the
         * surface is created.
         */

        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);
        
        Bitmap bitmap = BitmapFactory.decodeResource(
        		context.getResources(), R.drawable.texture);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    public void openGLSetup(GL10 gl) {
    	/*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);

        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
                GL10.GL_MODULATE);

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        // 
        
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        /* Location of camera: (0, 0, 5), look-at point: (0, 0, 0), 
         * up-vector: (0, 1, 0). */
        GLU.gluLookAt(gl,
        		0f, 0f, 5f,
        		0f, 0f, 0f,
        		0f, 1f, 0f);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glActiveTexture(GL10.GL_TEXTURE0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
    }
    
    public void onDrawFrame(GL10 gl) {
    	openGLSetup(gl);

    	/* Check when stop test. */
    	if (allFrames == 120) {
    		Log.i("GLTest", "--------------------------------------");
    		Log.i("GLTest", "- Average time for " + columns * rows + 
    				" triangles: " + (elapsedTime/120) + "ms.");
    		Log.i("GLTest", "--------------------------------------");
    		System.exit(0);
    	} else
    		allFrames++;
    	
        /* Textures on off switcher. */
        if (textureSwitcher >= 15 /* frames */) {
        	textureSwitcher = 0;
        	useTextures = !useTextures;
        } else
        	textureSwitcher++;
        
        start = System.currentTimeMillis();
        
        /* Tests. */
        switch (testCaseNo) {
        case 0: runTestCaseOne(gl, useTextures, columns, rows); break;
        case 1: runTestCaseTwo(gl, useTextures, columns, rows); break;
        default: break;
        }
        
        stop = System.currentTimeMillis();
        elapsedTime += stop-start;
    }
    
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        float ratio = (float) w / h;
        
        gl.glViewport(0, 0, w, h);
        
        /* Use perspective projection. */
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        
        /* The viewing volume. */
        gl.glFrustumf(-ratio, ratio, -1, 1, 0, 7);
    }

    /** Tests. */ 
    private void runTestCaseOne(GL10 gl, boolean useTextures,
    		int columns, int rows) {
    	triangles.draw(gl, useTextures, columns, rows);
    }
    
    /* This is done with JNI. */
    private native void runTestCaseTwo(GL10 gl, boolean useTextures,
    		int coulmns, int rows);
    
    /* Private variables. */
    private long start, stop, elapsedTime;
    private Context context;
    private int testCaseNo;
    
    private int allFrames = 0;
    private boolean useTextures = true;
    private int textureSwitcher = 0;
    
    private int columns, rows;
    
    private int mTextureID;
    private TrianglesMatrix triangles;
    
    /* Shared library loading. */
    static {
    	try {
    		System.loadLibrary("gltest");
    	} catch (UnsatisfiedLinkError err) {
    		Log.e("GLTest", "Error while loading lib: " + err.getMessage());
    	}
    }
}

