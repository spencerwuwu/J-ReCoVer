// https://searchcode.com/api/result/130553121/

/**************************************************************************
 * This file is part of Hypnotoad Live Wallaper.
 *
 *  Hypnotoad Live Wallaper is free software: you can redistribute it 
 *  and/or modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation, either version 3 of 
 *  the License, or (at your option) any later version.
 *
 *  Hypnotoad Live Wallaper is distributed in the hope that it will 
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  warranty of GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Hypnotoad Live Wallaper. If not, see <http://www.gnu.org/licenses/>.
 *************************************************************************/
package com.ancantus.HYPNOTOAD;

import java.io.File;
import com.ancantus.HYPNOTOAD.R;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class Hypnotoad_LW_Service extends WallpaperService
{
	@Override
	public Engine onCreateEngine() 
	{
		return new HypnotoadEngine();
	}
	
	//have to make the private class here because of some java security stuff, blegh
	private class HypnotoadEngine extends Engine
	{
		//threading stuff
		private final Handler handler = new Handler();
		
		private final Runnable updateAnimation = new Runnable() {
			@Override
			public void run() 
			{
				updateFrame();
			}
		};

		//global consts
		private final int TOTAL_FRAMES = 6;	//number of eye frames
		private final int ANIMATION_FRAMERATE = 100; //100 ms framerate for the toad
		private final int RELOAD_RETRIES = 10; //retry the bitmap reload up to 10 times before giving up
		
		//global vars (I don't like them all global, but its the easiest way to do it)
		private boolean visible = true;
		private boolean invalidateBackground = false;
		private boolean backgroundMissing = false;
		private boolean parallaxIsEnabled = true;
		private int frameCount;
		private int backgroundColor;
		private int baseX;
		private int baseY;
		private int eyeX;
		private int eyeY;
		private int backgroundX;
		private int backgroundY;
		private int bitmapLoadFailCount = 0;
		private double baseHeight;
		private double baseWidth;
		private double eyeWidth;
		private double eyeHeight;
		private double width;
		private double height;
		private float backgroundOffset = 1f;
		private float lastOffset = 0;
		private Paint bitmapBrush = new Paint(Paint.FILTER_BITMAP_FLAG);
		private Bitmap HYPNOTOAD; //even my conventions are hypnotized
		private Bitmap eyes1, eyes2, eyes3, eyes4, eyes5;
		private Bitmap backgroundPic = null;
		private String backgroundPath = null;
		
		//constructor() and send out the draw command
		public HypnotoadEngine()
		{
			//first load all the preferences from the shared preferences
			loadPreferences();
			
			BitmapFactory.Options bounds = new BitmapFactory.Options();
			bounds.inJustDecodeBounds = true;
			
			//load the bitmap (so that we don't load the base bitmap every draw
			BitmapFactory.decodeResource(getResources(), R.drawable.hypnotoad_base, bounds);
			baseHeight = bounds.outHeight;
			baseWidth = bounds.outWidth;
			
			//load the eye bitmaps (also pre-loaded to reduce computation while running)
			BitmapFactory.decodeResource(getResources(), R.drawable.eyes1, bounds);
			eyeHeight = bounds.outHeight;
			eyeWidth = bounds.outWidth;
			
			frameCount = 0;
			handler.post(updateAnimation);	//starts the animation update runner
		}
		
		//called on initialization and when visibility becomes true (so that setting the settings will refresh it)
		private void loadPreferences()
		{
			//load the preference object
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Hypnotoad_LW_Service.this);
			
			//set color of background from preferences
			switch(Integer.valueOf(prefs.getString("background_color", "0")))
			{
				case 0:
					backgroundColor = Color.BLACK;
					backgroundPic = null;
					backgroundPath = null;
					backgroundMissing = false;
					break;
				case 1:
					backgroundColor = Color.WHITE;
					backgroundPic = null;
					backgroundPath = null;
					backgroundMissing = false;
					break;
				case 2:
					//set the default black color (so alpha pictures do not cause overwriting and stuff)
					backgroundColor = Color.BLACK;
					
					//figure out how to check for picture existence
					if (!prefs.getString("pic_path","").equals(backgroundPath))
					{
						//check to make sure file exists
						File picFileTest = null;
						try
						{
							picFileTest = new File(prefs.getString("pic_path",""));
						}
						catch (Exception e) {}
						if (picFileTest != null && picFileTest.exists())
						{//if file exists, set it as the background and flag as the background changed
							backgroundPath = prefs.getString("pic_path","");
							invalidateBackground = true;
							backgroundMissing = false;
						}
						else	//if file doesn't exist, set background to black and continue
						{
							backgroundColor = Color.BLACK;
							backgroundPic = null;
							backgroundPath = null;
							backgroundMissing = true;
						}
					}
					break;
				default:
					backgroundColor = Color.BLACK;
					backgroundPic = null;
					backgroundPath = null;
					backgroundMissing = false;
					break;
			}
			
			//setting parallax enabled or disabled (disables/enables notifications on API that support it)
			parallaxIsEnabled = prefs.getBoolean("offset_notification_status", true);
			if (!parallaxIsEnabled)	//sets the background offset to the middle if the parallax is being disabled
			{
				backgroundOffset = 1f;
			}
			//this.setOffsetNotificationsEnabled(parallaxIsEnabled);	//cant use this because it is only supported on api 15
		}
		
		@Override	//update my visibility, handle surface thread
		public void onVisibilityChanged(boolean visibility) 
		{
			visible = visibility;
			
			//handle the enabling or disabling of the drawing message thread thing
			if (visible) 
			{
				//load the preferences if the view is becoming visible
				loadPreferences();
				
				//run function to load/update the background bitmap
				if (invalidateBackground)
				{
					updateBackgroundBitmap();
					invalidateBackground = false;
				}
				
				//startup the animation again
				handler.post(updateAnimation);
			} 
			else 
			{
				handler.removeCallbacks(updateAnimation);
			}
			
		}
		
		@Override	//stop any pending writes
		public void onSurfaceDestroyed(SurfaceHolder holder) 
		{
			super.onSurfaceDestroyed(holder);
			visible = false;
			handler.removeCallbacks(updateAnimation); //stops all pending writes
		}
		
		@Override	//when the surface changes, re-load all the bitmaps for the new size
		public void onSurfaceChanged(SurfaceHolder holder, int format, int w, int h) 
		{
			width = w;
			height = h;
			//if we need to scale the picture
			if (w < baseWidth || h < baseHeight)
			{		
				//scale the picture using my fancy scaling function, will scale to w or h depending on size fitting
				HYPNOTOAD = scaleBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hypnotoad_base), w, h);
				
				//scale the eyes so they fit the same resolution as the picture
				int scaledEyeWidth = (int)Math.round((HYPNOTOAD.getWidth()/baseWidth)*eyeWidth);
				int scaledEyeHeight = (int)Math.round((HYPNOTOAD.getHeight()/baseHeight)*eyeHeight);
				
				eyes1= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.eyes1), scaledEyeWidth, scaledEyeHeight, true);
				eyes2= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.eyes2), scaledEyeWidth, scaledEyeHeight, true);
				eyes3= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.eyes3), scaledEyeWidth, scaledEyeHeight, true);
				eyes4= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.eyes4), scaledEyeWidth, scaledEyeHeight, true);
				eyes5= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.eyes5), scaledEyeWidth, scaledEyeHeight, true);
			}
			else	//if there is no need for scaling...well don't scale
			{
				HYPNOTOAD = BitmapFactory.decodeResource(getResources(), R.drawable.hypnotoad_base);
				eyes1= BitmapFactory.decodeResource(getResources(), R.drawable.eyes1);
				eyes2= BitmapFactory.decodeResource(getResources(), R.drawable.eyes2);
				eyes3= BitmapFactory.decodeResource(getResources(), R.drawable.eyes3);
				eyes4= BitmapFactory.decodeResource(getResources(), R.drawable.eyes4);
				eyes5= BitmapFactory.decodeResource(getResources(), R.drawable.eyes5);
			}
			
			//calculate the placements of the toad
			baseX = (int)Math.round((w-HYPNOTOAD.getWidth())/2);
			baseY = (int)Math.round((h-HYPNOTOAD.getHeight())/2);
			eyeX = (int)(HYPNOTOAD.getWidth() * .074) + baseX;
			eyeY = (int)(HYPNOTOAD.getHeight() * .009) + baseY;
			
			//run function to load/update the background bitmap
			updateBackgroundBitmap();
			
			super.onSurfaceChanged(holder, format, w, h);
		}
		
		@Override	//handle background offset change
		public void onOffsetsChanged (float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset)
		{	
			//if parallax is enabled AND there is a valid background pic to parallax (aside: parallax, noun and verb) preview is hardcoded to middle as well
			if (parallaxIsEnabled && backgroundPic != null && !isPreview())
			{
				//work on using offset to slide background wallpaper behind the frog (goes from 0 to 1 depending on position, main == .5)
				backgroundOffset = xOffset * 2;
			
				//redraw the animation if we are visible(for the background changing) background offset spams the same number sometimes, so logic to limit that damage
				if (visible && lastOffset != backgroundOffset)
				{	
					draw();
					lastOffset = backgroundOffset;
				}
			}
			else	//default offset is the middle (used in the preview as well)
			{
				backgroundOffset = 1f;
			}
		}
		
		//main draw method. called on the threaded timer when the animation is updated, or when the background offset is changed
		private void draw()
		{
			SurfaceHolder holder = getSurfaceHolder();	//this contains the surface which in turn contains the canvas
			Canvas canvas = null;	//placeholder null, if it = null at the end of the try statement, try failed
			
			//check on each draw (before locking the canvas) if the background is missing and needs to be re-queried (and redraw the background)
			if (backgroundMissing)
			{
				loadPreferences();
				updateBackgroundBitmap();
			}
			try
			{
				canvas = holder.lockCanvas();
				
				//draw the toad frame (I have to redraw the whole screen EVERY frame. There really must be a better way to do it
				if (canvas != null)
				{	
					//set background (from setting above)
					canvas.drawColor(backgroundColor);
					
					if (backgroundPic != null)
					{
						canvas.drawBitmap(backgroundPic, backgroundX * backgroundOffset, backgroundY, bitmapBrush);
					}
					
					//draw the HYPNOTOAD
					canvas.drawBitmap(HYPNOTOAD, baseX, baseY, bitmapBrush);
					
					//now draw the bitmap depending on the frame (frame set in a different runnable)
					if (frameCount >= 1) { canvas.drawBitmap(eyes1, eyeX, eyeY, bitmapBrush); }
					if (frameCount >= 2) { canvas.drawBitmap(eyes2, eyeX, eyeY, bitmapBrush); }
					if (frameCount >= 3) { canvas.drawBitmap(eyes3, eyeX, eyeY, bitmapBrush); }
					if (frameCount >= 4) { canvas.drawBitmap(eyes4, eyeX, eyeY, bitmapBrush); }
					if (frameCount >= 5) { canvas.drawBitmap(eyes5, eyeX, eyeY, bitmapBrush); }
				}
			}
			finally
			{
				//if we have successfully locked and retrieved the canvas...
				if (canvas != null)
				{
					holder.unlockCanvasAndPost(canvas);	//post the changes we made, and unlock the canvas
				}
			}
		}
		
		//called by the runnable every time the animation should update (calls draw() afterwords to actually do the drawing)
		private void updateFrame()
		{
			//keep count and refresh after the frames are complete
			frameCount++;
			if (frameCount == TOTAL_FRAMES)
			{
				frameCount = 0;
			}
			
			//if the background failed to load, try reloading (up to $RELOAD_RETRIES times, then notify and fallback to default)
			if (bitmapLoadFailCount == RELOAD_RETRIES)
			{
				Toast toast = Toast.makeText(getApplicationContext(), "Chosen background bitmap is too large.", Toast.LENGTH_LONG);
				toast.show();	
				bitmapLoadFailCount = 0;
			}
			else if (bitmapLoadFailCount > 0)
			{
				updateBackgroundBitmap();
			}
			
			//redraw animation for the new frame
			draw();
			
			//post an update to the frame in 100ms (if were visible)
			handler.removeCallbacks(updateAnimation);
			if (visible)
			{
				handler.postDelayed(updateAnimation, ANIMATION_FRAMERATE);	//100ms is the framerate
			}
		}
		
		//scales bitmap so that it will fit in the given width and height
		private Bitmap scaleBitmap(Bitmap inputBitmap, double containingWidth, double containingHeight)
		{
			double bitmapWidth = inputBitmap.getWidth();
			double bitmapHeight = inputBitmap.getHeight();
			
			double scaledWidth, scaledHeight;
			
			if (containingWidth < bitmapWidth || containingHeight < bitmapHeight)
			{
				//if the width difference is greater than the height difference
				if (((bitmapWidth - containingWidth) > (bitmapHeight - containingHeight)) && containingWidth != 0)
				{
					scaledWidth = containingWidth;
					scaledHeight = (bitmapHeight/bitmapWidth)*scaledWidth;
				}
				else	//scale by height (default for background)
				{
					scaledHeight = containingHeight;
					scaledWidth = (bitmapWidth/bitmapHeight)*scaledHeight;
				}
				
				//sanity check to make sure new width and height are > 0 (Nov 28, 2012 bug)
				int newWidth = (int)Math.round(scaledWidth);
				int newHeight = (int)Math.round(scaledHeight);
				
				if (newWidth <= 0 || newHeight <=0)
				{
					//if the heights are invalid, spit the initial bitmap and be done with it (this should be a tranisant state).
					return inputBitmap;
				}
				return Bitmap.createScaledBitmap(inputBitmap, (int)Math.round(scaledWidth), (int)Math.round(scaledHeight), true);
			}
			else
			{
				return inputBitmap;
			}
		}
		
		//bitmap loading and scaling algroithm, will try twice to load the bitmap, and if the 2 attempts fails, will return null
		private Bitmap loadBitmap(String bitmapPath, double containingWidth, double containingHeight)
		{
			//try catch for any file-loading issues returns null which will just not show the background
			try 
			{
				//first pre-load the image, getting the bounds of the image without loading it
				BitmapFactory.Options bounds = new BitmapFactory.Options();
				bounds.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(bitmapPath, bounds);
			
				//because the bitmap scaling pull works best with powers of 2, get an intermediate bitmap close to the real size.
				int scale = 1; //this is the scale used for the real pull
			
				//will exit the while when were one power above the minimum scale (power 2 is more efficient CPU, but I need VM space)
				while (((bounds.outWidth/(scale*2) >= containingWidth) && containingWidth != 0) || bounds.outHeight/(scale*2) >= containingHeight)
				{
					scale *= 2;
				}
				
				//load the bitmap, scale it using the function above and send it out
				try
				{
					//Pull in the intermediate bitmap using the scale options
					BitmapFactory.Options scaledOpts = new BitmapFactory.Options();
					scaledOpts.inSampleSize = scale;
					
					return scaleBitmap(BitmapFactory.decodeFile(bitmapPath, scaledOpts), containingWidth, containingHeight);
				}
				//if we run out of memory, attemp it again, throwing efficency out the window, and just try to minimize the size as much as possible
				catch(OutOfMemoryError e)
				{
					//we ran out of memory, try and free up all that you can
					System.gc();
					
					//repeat the scaling again, but this time find the closest fit (throwing powers of 2 out the window, its less efficent but better on VM space)
					while (((bounds.outWidth/(scale + 1) >= containingWidth) && containingWidth != 0) || bounds.outHeight/(scale + 1) >= containingHeight)
					{
						scale += 1;
					}
					
					BitmapFactory.Options scaledOpts = new BitmapFactory.Options();
					scaledOpts.inSampleSize = scale;
					
					try
					{
						return scaleBitmap(BitmapFactory.decodeFile(bitmapPath, scaledOpts), containingWidth, containingHeight);
					}
					catch(OutOfMemoryError e1)	//bitmap is just too large, fail and return null
					{
						return null;
					}
				}
				
			}
			catch (Exception e) {}
			return null;
		}
		
		//loads and scales the background bitmap. bitmap now scales by height, width is left for scrolling
		private void updateBackgroundBitmap()
		{
			//load and scale the background bitmap if required
			if (backgroundPath != null)
			{
				//scaledBackgroundPic = scaleBitmap(backgroundPic, w, h);
				backgroundPic = null; //so GC can re-allocate if needed
				System.gc();
				backgroundPic = loadBitmap(backgroundPath, 0, height);	//zero in width means we scale only by height
				
				if (backgroundPic != null)	//check if load succeeded
				{
					bitmapLoadFailCount = 0;
					backgroundX = (int)Math.round((width-backgroundPic.getWidth())/2);
					backgroundY = (int)Math.round((height-backgroundPic.getHeight())/2);
				}
				else //if load failed, increment the counter
				{
					bitmapLoadFailCount++;
				}
			}
			else	//background pic = null is used in the code to revert to default black background
			{
				backgroundPic = null;
			}
		}
	}
}

