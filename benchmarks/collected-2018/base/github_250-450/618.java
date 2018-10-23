// https://searchcode.com/api/result/73869860/

package com.teracode.android.common.images;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.teracode.android.common.application.ApplicationProvider;
import com.teracode.android.common.util.FileUtil;

/**
 * @author Fernando Perez
 * @author Luciano Rey
 */
public class ImageLoader {
	
	private static final String TAG = ImageLoader.class.getSimpleName();
	private static final ImageLoader INSTANCE = new ImageLoader();
	
	// **********************************************
	// *** Field's
	// **********************************************
	
	// REFACTORME: the simplest in-memory cache implementation. This should be replaced with something like
	// SoftReference or BitmapOptions.inPurgeable(since 1.6).
	private HashMap<String, Bitmap> cache = new HashMap<String, Bitmap>();
	private PhotosQueue photosQueue = new PhotosQueue();
	private ArrayList<PhotosLoader> loaders = new ArrayList<PhotosLoader>();
	
	// **********************************************
	// ***
	// **********************************************
	
	public static ImageLoader get() {
		return INSTANCE;
	}
	
	// **********************************************
	// ***
	// **********************************************
	
	private ImageLoader() {
		// Make the background thead low priority. This way it will not affect the UI performance.
		// photoLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);
		// FIXME This for has no sense
		for (int i = 0; i < 1; i++) {
			PhotosLoader loader = new PhotosLoader();
			loader.setName(PhotosLoader.class.getSimpleName() + "_" + i);
			loader.setPriority(Thread.MIN_PRIORITY);
			loader.start();
			loaders.add(loader);
		}
	}
	
	// **********************************************
	// ***
	// **********************************************
	
	public void displayImage(String url, RemoteImageView imageView) {
		if (cache.containsKey(url)) {
			imageView.setImageBitmap(cache.get(url));
		} else {
			queuePhoto(url, imageView);
			imageView.showStubImage();
		}
	}
	
	private void queuePhoto(String url, RemoteImageView imageView) {
		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		photosQueue.clean(imageView);
		PhotoToLoad photoToLoad = new PhotoToLoad(url, imageView);
		synchronized (photosQueue.photosToLoad) {
			photosQueue.photosToLoad.push(photoToLoad);
			photosQueue.photosToLoad.notifyAll();
		}
	}
	
	private Bitmap getBitmap(String url) {
		// I identify images by hashcode. Not a perfect solution, good for the demo.
		final File file = new File(ApplicationProvider.get().getActiveApplication().getCacheDirectory(), String.valueOf(url.hashCode()));
		
		// from SD cache
		if (file.exists()) {
			return decodeFile(file);
		}
		
		// from web
		InputStream is = null;
		OutputStream os = null;
		try {
			// make client for http.
			DefaultHttpClient client = new DefaultHttpClient();
			// client.setRedirectHandler(new DefaultRedirectHandler());
			
			// make request.
			HttpUriRequest request = new HttpGet(new URI(url));
			
			// execute request.
			HttpResponse httpResponse = client.execute(request);
			is = httpResponse.getEntity().getContent();
			os = new FileOutputStream(file);
			FileUtil.copyStream(is, os);
			return decodeFile(file);
		} catch (Exception ex) {
			Log.e(TAG, "", ex);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					Log.e(TAG, "Close output stream", e);
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(TAG, "Close input stream", e);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Decodes image and scales it to reduce memory consumption.
	 */
	private Bitmap decodeFile(File f) {
		try {
			// decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);
			
			// Find the correct scale value. It should be the power of 2.
			final int REQUIRED_SIZE = 70;
			int width_tmp = o.outWidth;
			int height_tmp = o.outHeight;
			int scale = 1;
			while (!((width_tmp / 2 < REQUIRED_SIZE) || (height_tmp / 2 < REQUIRED_SIZE))) {
				width_tmp /= 2;
				height_tmp /= 2;
				scale++;
			}
			
			// decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
			Log.d(TAG, "File [" + f.getPath() + "] not found.");
		}
		return null;
	}
	
	public void clearCache() {
		// clear memory cache
		cache.clear();
	}
	
	public void stopThread() {
		// photoLoaderThread.interrupt();
	}
	
	// **********************************************
	// ***
	// **********************************************
	
	/**
	 * Task for the queue.
	 * 
	 * @author Luciano Rey
	 */
	class PhotoToLoad implements Runnable {
		
		public String url;
		public RemoteImageView imageView;
		public Bitmap bitmap;
		
		public PhotoToLoad(String u, RemoteImageView i) {
			url = u;
			imageView = i;
		}
		
		/**
		 * Used to display bitmap in the UI thread.
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
			} else {
				imageView.showStubImage();
			}
		}
	}
	
	/**
	 * Stores list of photos to download.
	 * 
	 * @author Luciano Rey
	 */
	class PhotosQueue {
		
		private Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();
		
		/**
		 * Removes all instances of this ImageView.
		 * 
		 * @param image
		 */
		public void clean(ImageView image) {
			for (int j = 0; j < photosToLoad.size();) {
				if (photosToLoad.get(j).imageView == image) {
					photosToLoad.remove(j);
				} else {
					++j;
				}
			}
		}
	}
	
	/**
	 * @author Luciano Rey
	 */
	class PhotosLoader extends Thread {
		
		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {
				while (true) {
					// thread waits until there are any images to load in the queue.
					if (photosQueue.photosToLoad.size() == 0) {
						synchronized (photosQueue.photosToLoad) {
							photosQueue.photosToLoad.wait();
						}
					}
					
					if (photosQueue.photosToLoad.size() != 0) {
						PhotoToLoad photoToLoad = null;
						synchronized (photosQueue.photosToLoad) {
							if (!photosQueue.photosToLoad.isEmpty()) {
								photoToLoad = photosQueue.photosToLoad.pop();
								Log.d(TAG, "Thread [" + getName() + "] is downloading photo [" + photoToLoad.url + "]");
							}
						}
						if (photoToLoad != null) {
							Bitmap bmp = getBitmap(photoToLoad.url);
							if (bmp != null) {
								cache.put(photoToLoad.url, bmp);
								if (((String)photoToLoad.imageView.getTag()).equals(photoToLoad.url)) {
									photoToLoad.bitmap = bmp;
									Activity a = (Activity)photoToLoad.imageView.getContext();
									a.runOnUiThread(photoToLoad);
								}
							}
						}
					}
				}
			} catch (InterruptedException e) {
				Log.e(TAG, "", e);
			}
		}
	}
}

