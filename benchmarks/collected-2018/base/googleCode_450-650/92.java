// https://searchcode.com/api/result/8220519/

/* 
 * PROJECT: NyARToolkit for Android SDK
 * --------------------------------------------------------------------------------
 * This work is based on the original ARToolKit developed by
 *   Hirokazu Kato
 *   Mark Billinghurst
 *   HITLab, University of Washington, Seattle
 * http://www.hitl.washington.edu/artoolkit/
 *
 * NyARToolkit for Android SDK
 *   Copyright (C)2010 NyARToolkit for Android team
 *   Copyright (C)2010 R.Iizuka(nyatla)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For further information please contact.
 *  http://sourceforge.jp/projects/nyartoolkit-and/
 *  
 * This work is based on the NyARToolKit developed by
 *  R.Iizuka (nyatla)
 *    http://nyatla.jp/nyatoolkit/
 * 
 * contributor(s)
 *  noritsuna
 */

package jp.androidgroup.nyartoolkit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import dev.agustin.BaseRenderer;

import jp.androidgroup.nyartoolkit.GLLib.ColorFloat;
import jp.androidgroup.nyartoolkit.model.ARModel;
import jp.androidgroup.nyartoolkit.view.GLBg;
import jp.androidgroup.nyartoolkit.view.GLSurfaceView;

import jp.nyatla.kGLModel.KGLException;
import jp.nyatla.kGLModel.KGLModelData;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

/**
 * Rendering 3DModels.
 * 
 * @author noritsuna
 *
 */
public class ModelRenderer extends BaseRenderer implements GLSurfaceView.Renderer 
{
	
	private static final int PATT_MAX = 2;
	private static final int MARKER_MAX = 8;
	
	private GLBg bg = null;
	private ARModel arModel;

	private Bitmap bgBitmap = null;
	
	private int bgTextureName = -1;
	private int found_markers;
	private int [] ar_code_index = new int[MARKER_MAX];
	private float [][] resultf = new float[MARKER_MAX][16];
	private float [] cameraRHf = new float[16];
	private boolean useRHfp = false;

	private boolean bgChangep = false;
	private boolean drawp = false;

	private float [] bgColor = new float[4];
	
	private boolean reloadTexturep;
	private boolean modelChangep;

	private Cube mCube = null;
	
	// metaseq
	private KGLModelData[] model = new KGLModelData[PATT_MAX];
	private AssetManager am;
	private String[] modelName = new String[PATT_MAX];
	private float[] modelScale = new float[PATT_MAX];

	public int mWidth;
	public int mHeight;

	public static final int MODEL_FLAG = 0x01;
	public static final int BG_FLAG = 0x02;
	public static final int ALL_FLAG = MODEL_FLAG | BG_FLAG;
	public int deleteFlags = MODEL_FLAG | BG_FLAG;

	public boolean checkDeleteAll() {
		return deleteFlags == ALL_FLAG;
	}
	

	public ModelRenderer(boolean useTranslucentBackground, ARModel arModel) {
        mTranslucentBackground = useTranslucentBackground;
        this.arModel = arModel;
        if (!mTranslucentBackground)
        	initBg();
		cameraReset();
		if (useTranslucentBackground) {
			bgColor[0] = 0;
			bgColor[1] = 0;
			bgColor[2] = 0;
			bgColor[3] = 0;
		} else {
			bgColor[0] = 1;
			bgColor[1] = 1;
			bgColor[2] = 1;
			bgColor[3] = 1;
		}
    }
	public ModelRenderer(boolean useTranslucentBackground, AssetManager am, String[] modelName, float[] modelScale) {
        mTranslucentBackground = useTranslucentBackground;
        this.am = am;
        if (!mTranslucentBackground)
        	initBg();
		cameraReset();
		if (useTranslucentBackground) {
			bgColor[0] = 0;
			bgColor[1] = 0;
			bgColor[2] = 0;
			bgColor[3] = 0;
		} else {
			bgColor[0] = 1;
			bgColor[1] = 1;
			bgColor[2] = 1;
			bgColor[3] = 1;
		}
		for (int i = 0; i < PATT_MAX; i++) {
	        this.modelName[i] = modelName[i];
			this.modelScale[i] = modelScale[i];
	        loadModel(modelName[i], modelScale[i]);
		}
    }

	
	public void setBgColor(int color) {
		bgColor[0] = ((color & 0x00FF0000) >> 16) / 255.0f;
		bgColor[1] = ((color & 0x0000FF00) >> 8) / 255.0f;
		bgColor[2] = (color & 0x000000FF) / 255.0f;
		bgColor[3] = (((color & 0xFF000000) >> 24) & 0xFF) / 255.0f;
	}
	public void setBgColor(float r, float g, float b, float a) {
		bgColor[0] = r;
		bgColor[1] = g;
		bgColor[2] = b;
		bgColor[3] = a;
	}
	public int getBgColorInt() {
		int r = new ColorFloat(bgColor[0], bgColor[1],
							   bgColor[2], bgColor[3]).toARGB();
		return r;
	}
	
	public void reloadTexture() {
		reloadTexturep = true;
        if (!mTranslucentBackground)
        	bgChangep = true;
	}

	public void loadModel(String fname, float scale) {
		modelChangep = true;
	}
	
	
	private Handler mainHandler;
	public void setMainHandler(Handler handler) {
		mainHandler = handler;
	}

	
	public void initModel(GL10 gl) {
		if (mainHandler != null) {
			mainHandler.sendMessage
				(mainHandler.obtainMessage
						(NyARToolkitAndroidActivity.SHOW_LOADING));
		}
		for (int i = 0; i < PATT_MAX; i++) {
			if (model[i] != null) {
				model[i].Clear(gl);
				model[i] = null;
				deleteFlags |= MODEL_FLAG;
			}
			if (modelName[i] != null) {
				try {
					model[i] = KGLModelData.createGLModel
						(gl, null, am, modelName[i], modelScale[i]);

				} catch (KGLException e) {
					Log.e("ModelRenderer", "KGLModelData error", e);
				}
				deleteFlags &= ~MODEL_FLAG;
			}
		}
		if (mainHandler != null) {
			mainHandler.sendMessage
				(mainHandler.obtainMessage
						(NyARToolkitAndroidActivity.HIDE_LOADING));
		}
		modelChangep = false;
	}

	// BG
	public void setBgBitmap(Bitmap bm) {
        if (mTranslucentBackground)
        	return;
		synchronized (this) {
			bgBitmap = bm;
		}
		bgChangep = true;
	}

	public void objectClear() {
		drawp = false;
	}

	
	private void loadBitmap(GL10 gl) {
		if (bgTextureName != -1) {
			bg.deleteTexture(gl, bgTextureName);
			bgTextureName = -1;
			deleteFlags |= BG_FLAG;
		}
		if (bgBitmap != null) {
			bgTextureName = bg.createTexture(gl);
			bg.loadTexture(gl, bgTextureName, bgBitmap);
			deleteFlags &= ~BG_FLAG;
		}
		bgChangep = false;
	}

	private void initBg() {
		if (bg == null) {
			bg = new GLBg();
		}
		bgChangep = true;
	}

	
	public float [] zoomV = new float[4];
	public float [] upOriV = { 0.0f, 1.0f, 0.0f, 0.0f };
	public float [] lookV = new float[4];
	public float [] camRmtx = new float[16];

	public float [] camV = new float[4];
	public float [] upV = new float[4];
	public float ratio;

	// Temporary
	private float [] mtx = new float[16];

	private boolean cameraChangep;
	
	private void cameraSetup(GL10 gl) {
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		// gl.glFrustumf(-ratio, ratio, -1, 1, 1, 1000);
		GLU.gluPerspective(gl, 45, ratio, 10.0f, 2000.0f);
		GLU.gluLookAt(gl,
					  camV[0], camV[1], camV[2],
					  lookV[0], lookV[1], lookV[2], 
					  upV[0], upV[1], upV[2]);
		cameraChangep = false;
	}

	private void cameraMake() {
		Matrix.setIdentityM(mtx, 0);
		Matrix.translateM(mtx, 0, lookV[0], lookV[1], lookV[2]);
		Matrix.multiplyMM(mtx, 0, camRmtx, 0, mtx, 0);
		Matrix.multiplyMV(camV, 0, mtx, 0, zoomV, 0);
		Matrix.multiplyMV(upV, 0, camRmtx, 0, upOriV, 0);
		cameraChangep = true;
	}

	public void cameraReset() {
		zoomV[0] = zoomV[1] = camV[0] = camV[1] = 0.0f;
		zoomV[2] = camV[2] = -500.0f;
		lookV[0] = lookV[1] = lookV[2] = 0.0f;
		upV[0] = upV[2] = 0.0f;
		upV[1] = 1.0f;
		Matrix.setIdentityM(camRmtx, 0);
		cameraChangep = true;
	}

	public void cameraRotate(float rot, float x, float y, float z,
							 float [] sMtx) {
		float [] vec = { x, y, z, 0 };
		Matrix.setIdentityM(mtx, 0);
		Matrix.rotateM(mtx, 0, rot, vec[0], vec[1], vec[2]);
		Matrix.multiplyMM(camRmtx, 0, sMtx, 0, mtx, 0);
		cameraMake();
	}

	public void cameraZoom(float z) {
		zoomV[2] += z;
		cameraMake();
	}

	public void cameraMove(float x, float y, float z) {
		float [] vec = { x, y, z, 0 };
		Matrix.multiplyMV(vec, 0, camRmtx, 0, vec, 0);
		for (int i = 0; i < 3; i++) {
			lookV[i] += vec[i];
		}
		cameraMake();
	}
	

    public void objectPointChanged(int found_markers, int [] ar_code_index, float[][] resultf, 
			   float[] cameraRHf) {
		synchronized (this) {
			this.found_markers = found_markers;
			for (int i = 0; i < MARKER_MAX; i++) {
				this.ar_code_index[i] = ar_code_index[i];
				System.arraycopy(resultf[i], 0, this.resultf[i], 0, 16);
			}
			System.arraycopy(cameraRHf, 0, this.cameraRHf, 0, 16);
		}
		useRHfp = true;
		drawp = true;
    }

	public void setDrawp(boolean dp) {
		drawp = dp;
	}

	// Light
	public boolean lightCamp = false;
	public boolean lightp = true;
	public boolean speLightp = false;
	
	float[] lightPos0 = { 1000, 1000, 1000, 0 };
	float[] lightPos1 = { 1000, 1000, 1000, 0 };
	float[] lightPos2 = { 1000, 1000, 1000, 0 };
	float[] lightDif = { 0.6f, 0.6f, 0.6f, 1 };
	float[] lightSpe = { 1.0f, 1.0f, 1.0f, 1 };
	float[] lightAmb = { 0.01f, 0.01f, 0.01f, 1 };

	private void lightSetup(GL10 gl) {
		if (lightCamp) {
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, camV, 0);
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDif, 0);
			gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, camV, 0);
			gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_AMBIENT, lightAmb, 0);
			if (speLightp) {
				gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_POSITION, camV, 0);
				gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_SPECULAR, lightSpe, 0);
			}
		} else {
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPos0, 0);
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDif, 0);
			gl.glLightfv(GL10.GL_LIGHT1,GL10.GL_POSITION, lightPos2, 0);
			gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_AMBIENT, lightAmb, 0);
			if (speLightp) {
				gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_POSITION, lightPos1, 0);
				gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_SPECULAR, lightSpe, 0);
			}
		}
		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glEnable(GL10.GL_LIGHT1);
		if (speLightp)
			gl.glEnable(GL10.GL_LIGHT2);
	}
	private void lightCleanup(GL10 gl) {
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_LIGHT0);
		gl.glDisable(GL10.GL_LIGHT1);
		gl.glDisable(GL10.GL_LIGHT2);
	}
    
    public void onDrawFrame(GL10 gl) {
		if (modelChangep) {
			mCube = new Cube();
			initModel(gl);
			reloadTexturep = false;
		} else if (reloadTexturep) {
	    	Log.d("ModelRenderer","in reloadTexturep:");
	    	for (int i = 0; i < PATT_MAX; i++) {
	    		if (model[i] != null)
	    			model[i].reloadTexture(gl);
	    	}
			reloadTexturep = false;
		}
		if (bgChangep) {
	    	Log.d("ModelRenderer","in loadBitmap:");
			loadBitmap(gl);
		}

		/*
		 * Usually, the first thing one might want to do is to clear
		 * the screen. The most efficient way of doing this is to use
		 * glClear().
		 */

		//gl.glClearColor(bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		// Bg
		if (bgBitmap != null && bgTextureName != -1) {
			// Log.i("Renderer", "bgTextureName: "+bgTextureName);
			bg.draw(gl, bgTextureName, bgBitmap);
		}
		if (drawp) {
			// camera 
			if (useRHfp) {
				gl.glMatrixMode(GL10.GL_PROJECTION);
				gl.glLoadMatrixf(cameraRHf, 0);
			} else if (cameraChangep) {
				cameraSetup(gl);
			}

 			if (mCube != null) {
 				int patt[] = new int[PATT_MAX];
 				for (int i = 0; i < found_markers; i++) {
 					gl.glMatrixMode(GL10.GL_MODELVIEW);
 					if (useRHfp) {
 						gl.glLoadMatrixf(resultf[i], 0);
 						// ????
 						gl.glTranslatef(0.0f, 0.0f, 0.0f);
 						// OpenGL????ARToolkit???
 						gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
 					} else {
 						gl.glLoadIdentity();
 					}
 					if (patt[ar_code_index[i]] != -1) {
 						Log.d("ModelRenderer", "onDrawFrame: " + i + ",model: "+ ar_code_index[i]);
 	 					patt[ar_code_index[i]] = -1;
 						if (lightp)
 							lightSetup(gl);		  
 						model[ar_code_index[i]].enables(gl, 1.0f);
 						model[ar_code_index[i]].draw(gl);
 						model[ar_code_index[i]].disables(gl);
 						if (lightp)
 							lightCleanup(gl);
 					} else {
 						Log.d("ModelRenderer", "onDrawFrame: " + i + ", "+ ar_code_index[i]);
 						gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
 						gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
 						mCube.draw(gl);
 					}
 				}
 			}
		} else {
 			gl.glMatrixMode(GL10.GL_PROJECTION);
 			gl.glLoadMatrixf(cameraRHf, 0);
 	 		gl.glEnable(GL10.GL_DEPTH_TEST);
		}
		makeFramerate();
    }


    private int mFrames = 0;
    private float mFramerate;
    private long mStartTime;

	public float getFramerate() {
		return mFramerate;
	}
	public float getStartTime() {
		return mStartTime;
	}
    private void makeFramerate() {
        long time = SystemClock.uptimeMillis();

		synchronized (this) {
			mFrames++;
			if (mStartTime == 0) {
				mStartTime = time;
			}
			if (time - mStartTime >= 1) {
				mFramerate = (float)(1000 * mFrames) 
					/ (float)(time - mStartTime);
				Log.d("ModelRenderer", "Framerate: " + mFramerate + " (" + (time - mStartTime) + "ms)");
				mFrames = 0;
				mStartTime = time;
			}
		}
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
		
		gl.glViewport(0, 0, width, height);

		/*
		 * Set our projection matrix. This doesn't have to be done
		 * each time we draw, but usually a new projection needs to
		 * be set when the viewport is resized.
		 */
		ratio = (float) width / height;
		cameraChangep = true;
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

		if (mTranslucentBackground) {
            gl.glClearColor(0,0,0,0);
        } else {
            gl.glClearColor(1,1,1,1);
        }

        bgTextureName = -1;
        for (int i = 0; i < PATT_MAX; i++) {
        	if (model[i] != null) {
        		model[i].resetTexture();
        	}
        }
		reloadTexture();
		cameraChangep = true;
    }
    private boolean mTranslucentBackground;
}

