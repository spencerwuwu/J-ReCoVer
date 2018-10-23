// https://searchcode.com/api/result/122845921/

package bartspot.games.gui;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import bartspot.games.utils.DrawImage;
import bartspot.games.utils.GameData;
import bartspot.games.utils.GameTextures;
import bartspot.games.utils.RandomImageList;

import android.R;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Handler;
import android.util.Log;
import java.util.concurrent.Semaphore;


/***
 * Copyright C (2011) Bartspot Software
 * 
 * @author Kevin Greenhaw
 * 
 * Class used to Draw the Bus on the screen. 
 * 
 * Public Interfaces provided by the class:
 * 
 * 
 */
@SuppressWarnings("unused")
class PARenderer implements GLSurfaceView.Renderer {
   private static final String TAG = PARenderer.class.getSimpleName();
   private Context         mContext;
   private GamePanel       pGame;
   private GameTextures    pGameTextures;
   private GameData        mData;
   private DrawImage       pBgImage;
   private RandomImageList pRandomImage;
   private static int      mGameLevel;
   private static GL10     mOgl;

   
   /***
	 * 
	 * @param context
	 * @param handler
	 * @param data
	 */
	public PARenderer (Context context, int width, int height) {
	   mContext = context;

      pBgImage = new DrawImage( mContext, width/2, height/2);

      pGameTextures   = GameTextures.getInstance();
      pRandomImage    = RandomImageList.getInstance();
	}
	
	
   /**
    * 
    * 
    */
   public void onStart(GameData data) {
      Log.i(TAG, "onStart Called");
   
      mData    = data;
      pGame    = new GamePanel( mContext, data);
      
      pGame.startGame(mData.playLevel);
   }
   
	/**
	 * 
	 * 
	 */
	public void onStop() {
	   Log.i(TAG, "onStop Called");
	   
	   if (pGame != null) {
	      pGame.stopGame();
	      pGame    = null;
	   }
	}
	
   /***
	 * 
	 * @param gl
	 * @param config
	 */
	public void onSurfaceCreated( GL10 gl, EGLConfig config ) {
      Log.i(TAG, "onSurfaceCreated Called");
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
      gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

      gl.glEnable(GL10.GL_BLEND);
      gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
      
      gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);   //Black Background

      gl.glEnable(GL10.GL_DEPTH_TEST);           //Enables Depth Testing
      gl.glShadeModel(GL10.GL_SMOOTH);           //Enable Smooth Shading
      gl.glEnable(GL10.GL_TEXTURE_2D);           //Enable Texture Mapping ( NEW )

      pGameTextures.loadGameTextures(gl, mContext);
      pBgImage.loadImageTexture(gl);
      
      mOgl = gl;
 	}

   
	/***
    * 
    * @param gl
    */
	public void onDrawFrame(GL10 gl) {
	   /*
       * By default, OpenGL enables features that improve quality
       * but reduce performance. One might want to tweak that
       * especially on software renderer.
       */
      gl.glDisable(GL10.GL_DITHER);

      /*
       * Usually, the first thing one might want to do is to clear
       * the screen. The most efficient way of doing this is to use
       * glClear().
       */
      gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

      if (pBgImage != null) {
         pBgImage.draw(gl, mData.width/2, mData.height/2);
      }
      
      if (pGame != null) {
         pGame.draw(gl, mData.xBuffer, mData.yBuffer);
      }      
	}
      
   
	/***
    * 
    * @param width
    * @param height
    */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
      Log.i(TAG, "onSurfaceChanged Called");

      float ratio = (float) width / height;
      
      // Sets the current view port to the new size.
      gl.glViewport(0, 0, width, height);
      // Select the projection matrix
      gl.glMatrixMode(GL10.GL_PROJECTION);
      // Reset the projection matrix
      gl.glLoadIdentity();
      
      // Calculate the aspect ratio of the window.  MUST USE: Ortho2D in 
      // so the screen touch coordinates match with the grid coordinates.
      GLU.gluOrtho2D(gl, 0, width, 0, height);
      
      // Select the modelview matrix
      gl.glMatrixMode(GL10.GL_MODELVIEW);
      // Reset the modelview matrix
      gl.glLoadIdentity();

	}

   
   /***
    * 
    * @param rotateSpeed
    */
   public void onTouchEvent(float x, float y) {
      // MAGIC NUMBER WARNING: The 50 is to adjust for the AdDisplay bar
      // but the 24 it's not clear where the 24 comes from (should be 32).
      int adBarOffset = 50;
      
      // UNCOMMENT THIS LINE FOR NO ADS BUILD
//      adBarOffset = 0;
      
      if ((adBarOffset == 0) || (mData.scaleFactor == 1)) {
         pGame.onTouchEvent(x - mData.xBuffer, y - (mData.yBuffer + adBarOffset));
      }
      else {
         pGame.onTouchEvent(x - mData.xBuffer, y - (mData.yBuffer + adBarOffset + 24));
      }
      onDrawFrame(mOgl);
   } 
}

