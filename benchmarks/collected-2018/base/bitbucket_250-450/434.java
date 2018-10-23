// https://searchcode.com/api/result/61617225/

package game.engine.graphics;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;
import game.engine.generic.BaseLoop;
import game.engine.generic.BaseSystem;
import game.engine.graphics.helper.GLSurfaceView;
import game.engine.graphics.helper.GLSystem;
import game.engine.graphics.helper.GraphicsLibrary;
import game.engine.graphics.helper.Grid;
import game.engine.graphics.helper.SimpleTextureLibrary;
import game.engine.graphics.helper.Texture;
import game.engine.pool.ComponentPool;
import game.engine.utilities.AllocationGuard;
import game.zombie.GameActivity;
import game.zombie.R;

/**
 * This system draws render components via OpenGL.
 * 
 * @author kenrick
 * 
 */
public class SimpleRenderSystem extends BaseSystem<BaseRenderComponent>
		implements GLSurfaceView.Renderer {

	private static final String TAG = "SimpleRenderSystem";

	/** Loading screen stuff */
	private int width, height;
	private int loading_minx, loading_maxx, loading_miny, loading_maxy;
	private Texture loading_tex;
	private Texture loading_back_tex;
	private Texture loading_text_tex;
	private Texture background_tex;

	public boolean isLoading() {
		return current_loading_step <= loading_steps;
	}

	private static final int loading_steps = 6;
	private int current_loading_step;

	/**
	 * The current camera component describes what is drawn on the screen (and
	 * where).
	 */
	private CameraComponent camera;

	private Comparator<BaseRenderComponent> comparator;

	/**
	 * Initializes the render system. The surface/draw thread is not yet
	 * created!
	 * 
	 * @param context
	 * @param maxAttachedComponents
	 * @param loop
	 */
	public SimpleRenderSystem(Context context, int maxAttachedComponents,
			BaseLoop loop) {
		super(maxAttachedComponents, loop);
		mContext = context;
		mUseVerts = true;
		current_loading_step = 0;
		comparator = new Comparator<BaseRenderComponent>() {
			public int compare(BaseRenderComponent object1,
					BaseRenderComponent object2) {
				return Float.compare((float) object1.entity.z,
						(float) object2.entity.z);
			}
		};
		components.setComparator(comparator);
	}

	public void setCamera(CameraComponent camera) {
		this.camera = camera;
	}

	public CameraComponent getCamera() {
		return camera;
	}

	public void think(double dt) {
		if (components != null) {
			components.sort(false);
		}
	}

	/**
	 * A reference to the application context.
	 */
	private Context mContext;

	/**
	 * gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	 * gl.glMatrixMode(GL10.GL_MODELVIEW);
	 * 
	 * Determines the use of vertex arrays.
	 */
	private boolean mUseVerts;

	public int[] getConfigSpec() {
		// We don't need a depth buffer, and don't care about our
		// color depth.
		int[] configSpec = { EGL10.EGL_DEPTH_SIZE, 0, EGL10.EGL_NONE };
		return configSpec;
	}

	public void loadStuff(GL10 gl) {
		if (components == null) {
			return;
		}

		int components_count = components.getCount();
		int libraries_count = GraphicsLibrary.libraries.getCount();

		switch (current_loading_step) {
		case 0:
			break;
		case 1:
			// If we are using hardware buffers and the screen lost
			// context
			// then the buffer indexes that we recorded previously are
			// now
			// invalid. Forget them here and recreate them below.
			// Reset everything!
			for (int x = 0; x < components_count; x++)
				components.get(x).resetVariables();
			break;

		case 2:
			for (int x = 0; x < libraries_count; x++) {
				GraphicsLibrary.libraries.get(x).releaseResources(gl);
			}
			break;

		case 3:
			for (int x = 0; x < components_count; x++) {
				components.get(x).add();
			}
			break;

		case 4:
			GraphicsLibrary.preloadTextures();
			break;

		case 5:
			for (int x = 0; x < libraries_count; x++) {
				GraphicsLibrary.libraries.get(x).loadResources(mContext, gl);
			}
			break;

		case 6:
			int[] textures_to_delete = new int[] { loading_tex.name,
					loading_back_tex.name, background_tex.name,
					loading_text_tex.name };
			gl.glDeleteTextures(1, textures_to_delete, 0);

			loading_tex = null;
			loading_back_tex = null;
			loading_text_tex = null;
			background_tex = null;

			Runtime r = Runtime.getRuntime();
			r.gc();

			break;
		}

		current_loading_step++;
	}

	/** Draws all the render components. */
	public void drawFrame(GL10 gl, double dt) {
		if (current_loading_step <= loading_steps || loop.loading) {
			GLSystem.bindTexture(gl, GL10.GL_TEXTURE_2D, background_tex.name);
			((GL11Ext) gl).glDrawTexfOES((float) 0, (float) 0, (float) -1,
					(float) width, (float) height);

			GLSystem.bindTexture(gl, GL10.GL_TEXTURE_2D, loading_back_tex.name);
			((GL11Ext) gl).glDrawTexfOES((float) loading_minx - 2,
					(float) loading_miny - 2, (float) 0, (float) loading_maxx
							- loading_minx + 4, (float) loading_maxy
							- loading_miny + 4);

			GLSystem.bindTexture(gl, GL10.GL_TEXTURE_2D, loading_tex.name);
			((GL11Ext) gl).glDrawTexfOES((float) loading_minx,
					(float) loading_miny, (float) 0,
					(float) (loading_maxx - loading_minx)
							* current_loading_step / loading_steps,
					(float) loading_maxy - loading_miny);

			GLSystem.bindTexture(gl, GL10.GL_TEXTURE_2D, loading_text_tex.name);
			((GL11Ext) gl).glDrawTexfOES((float) (width / 2.0 - 64),
					(float) (height * 2.0 / 3.0), 0, 128, 16);

			loadStuff(gl);
		} else {
			if (components != null) {
				gl.glMatrixMode(GL10.GL_MODELVIEW);

				if (mUseVerts) {
					Grid.beginDrawing(gl, true, false);
				}

				synchronized (loop) {
					int components_count = components.getCount();
					for (int x = 0; x < components_count; x++)
						components.get(x).draw(gl, dt, camera);
				}

				if (mUseVerts) {
					Grid.endDrawing(gl);
				}
			}
		}
	}

	/**
	 * Called when the size of the window changes.
	 */
	public void sizeChanged(GL10 gl, int width, int height) {
		Log.v(TAG, "sizeChanged()");
		gl.glViewport(0, 0, width, height);

		/*
		 * Set our projection matrix. This doesn't have to be done each time we
		 * draw, but usually a new projection needs to be set when the viewport
		 * is resized.
		 */
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0.0f, width, 0.0f, height, 0.0f, 1.0f);

		gl.glShadeModel(GL10.GL_FLAT);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		loading_minx = (int) (width * 1.0 / 3.0);
		loading_maxx = (int) (width * 2.0 / 3.0);
		loading_miny = (int) (height * 1.0 / 3.0);
		loading_maxy = (int) (height * 1.0 / 3.0 + 18);
		this.width = width;
		this.height = height;
	}

	/**
	 * Called whenever the surface is created. This happens at startup, and may
	 * be called again at runtime if the device context is lost (the screen goes
	 * to sleep, etc). This function must fill the con +
	 * old_score.getOverallWidth() tents of vram with texture data and (when
	 * using VBOs) hardware vertex arrays.
	 */
	public void surfaceCreated(GL10 gl) {
		Log.v(TAG, "surfaceCreated()");

		/*
		 * Some one-time OpenGL initialization can be made here probably based
		 * on features of this particular context
		 */
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

		gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
		gl.glShadeModel(GL10.GL_FLAT);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glDepthMask(false);
		/*
		 * By default, OpenGL enables features that improve quality but reduce
		 * performance. One might want to tweak that especially on software
		 * renderer.
		 */
		gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_LIGHTING);

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		AllocationGuard.sGuardActive = false;
		loading_tex = new Texture();
		loading_tex.resource = R.drawable.loading;

		loading_back_tex = new Texture();
		loading_back_tex.resource = R.drawable.loading_back;

		loading_text_tex = new Texture();
		loading_text_tex.resource = R.drawable.loading_text;

		background_tex = new Texture();
		background_tex.resource = R.drawable.black;
		AllocationGuard.sGuardActive = true;

		GraphicsLibrary.simple_texture_lib
				.loadBitmap(mContext, gl, loading_tex);
		GraphicsLibrary.simple_texture_lib.loadBitmap(mContext, gl,
				loading_back_tex);
		GraphicsLibrary.simple_texture_lib.loadBitmap(mContext, gl,
				loading_text_tex);
		GraphicsLibrary.simple_texture_lib.loadBitmap(mContext, gl,
				background_tex);

		current_loading_step = 0;
	}

	/**
	 * Called when the rendering thread shuts down. This is a good place to
	 * release OpenGL ES resources.
	 * 
	 * @param gl
	 */
	public void shutdown(GL10 gl) {
		Log.v(TAG, "shutdown()");
		if (components != null) {
			int libraries_count = GraphicsLibrary.libraries.getCount();
			for (int x = 0; x < libraries_count; x++) {
				GraphicsLibrary.libraries.get(x).releaseResources(gl);
			}
			synchronized (loop) {
				int components_count = components.getCount();
				for (int x = 0; x < components_count; x++) {
					components.get(x).delete(gl);
				}
			}
		}
	}

	/**
	 * Sort the components based on z values.
	 */
	public void sortComponents() {
		if (components != null) {
			components.sort(false);
		}
	}

}

