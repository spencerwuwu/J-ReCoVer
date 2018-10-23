// https://searchcode.com/api/result/42348349/

package com.volvo.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Process;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.androidcommons.util.BitmapUtil;
import com.androidcommons.util.DisplayUtil;
import com.volvo.android.app.R;
import com.volvo.android.app.VolvoApplication;
import com.volvo.android.log.Log;
import com.volvo.android.service.VolvoSyncService;
import com.volvo.android.view.WebImageView;

public class ImageLoader {

	public static String getUrlPrefix(final boolean local) {
		return (local ? "file:///android_asset/" : "");
	}

	private static final Log LOG = Log.getInstance();

	private final HashMap<String, SoftReference<Bitmap>> cache = new HashMap<String, SoftReference<Bitmap>>();
	private final File cacheDir;

	PhotosLoader photoLoaderThread = new PhotosLoader();
	PhotosQueue photosQueue = new PhotosQueue();

	final int stub_id = R.drawable.placeholder;

	public ImageLoader(final Context context) {
		photoLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);

		cacheDir = context.getCacheDir();
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
	}

	public void displayImage(final String uri, final BitmapCallback callback, final boolean compressionNeeded) {
		if (uri == null) {
			return;
		}
		String key = uri;
		if (compressionNeeded) {
			key = key + "+compressed";
		}

		if (cache.containsKey(key)) {
			final Bitmap bitmap = cache.get(key).get();
			if (bitmap != null) {
				callback.setBitmap(bitmap);
			} else {
				cache.remove(key);
				// try to retrieve
				queuePhoto(uri, callback, compressionNeeded);
			}
		} else {
			queuePhoto(uri, callback, compressionNeeded);
		}
	}

	public void displayImage(final String uri, final WebImageView webImageView, final boolean compressionNeeded) {
		if (uri == null) {
			return;
		}
		String key = uri;
		if (compressionNeeded) {
			key = key + "+compressed";
		}

		webImageView.showProgress();

		if (cache.containsKey(key)) {
			final Bitmap bitmap = cache.get(key).get();
			if (bitmap != null) {
				webImageView.setImageBitmap(bitmap);
				webImageView.showImage();
			} else {
				cache.remove(key);
				// try to retrieve
				queuePhoto(uri, webImageView, compressionNeeded);
			}
		} else {
			queuePhoto(uri, webImageView, compressionNeeded);
		}
	}

	public void freeImage(final String uri) {
		if (uri == null) {
			return;
		}
		final String key = uri;
		if (cache.containsKey(key)) {
			final Bitmap bitmap = cache.get(key).get();
			if (bitmap != null) {
				bitmap.recycle();
			}
			cache.remove(key);
		}
	}

	/**
	 * Loads image from: 1. Memory cache, 2. Disk cache, 3. Web url; Sets stub
	 * image and scales image by default
	 */
	public void displayImage(final String uri, final ImageView imageView, final boolean compressionNeeded) {
		displayImage(uri, imageView, true, compressionNeeded);
	}

	private void displayImage(final String uri, final ImageView imageView, final boolean useStub,
			final boolean compressionNeeded) {
		if (uri == null) {
			return;
		}
		String key = uri;
		if (compressionNeeded) {
			key = key + "+compressed";
		}

		if (cache.containsKey(key)) {
			final Bitmap bitmap = cache.get(key).get();
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
			} else {
				cache.remove(key);
				// try to retrieve
				queuePhoto(uri, imageView, compressionNeeded);
				if (useStub) {
					imageView.setImageResource(stub_id);
				}
			}
		} else {
			queuePhoto(uri, imageView, compressionNeeded);
			if (useStub) {
				imageView.setImageResource(stub_id);
			}
		}
	}

	public BitmapDrawable getImageDrawable(final String uri) {
		final BitmapDrawable result;
		final String key = uri;

		if (cache.containsKey(key)) {
			final Bitmap bitmap = cache.get(key).get();
			if (bitmap != null) {
				result = new BitmapDrawable(bitmap);
			} else {
				cache.remove(key);
				// try to retrieve
				result = new BitmapDrawable(downloadBitmap(uri, false));
			}
		} else {
			result = new BitmapDrawable(downloadBitmap(uri, false));
		}

		return result;
	}

	public void clearCache() {
		cache.clear();

		final File[] files = cacheDir.listFiles();
		for (final File f : files) {
			f.delete();
		}
	}

	public void stopThread() {
		photoLoaderThread.interrupt();
	}

	private void queuePhoto(final String url, final BitmapCallback callback, final boolean compressionNeeded) {
		// This Runnable may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		photosQueue.Clean(callback);
		final PhotoToLoad p = new PhotoToLoad(url, callback, compressionNeeded);
		synchronized (photosQueue.photosToLoad) {
			photosQueue.photosToLoad.push(p);
			photosQueue.photosToLoad.notifyAll();
		}

		// start thread if it's not started yet
		if (photoLoaderThread.getState() == Thread.State.NEW) {
			photoLoaderThread.start();
		}
	}

	private void queuePhoto(final String url, final WebImageView webImageView, final boolean compressionNeeded) {

		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		photosQueue.Clean(webImageView);
		final PhotoToLoad p = new PhotoToLoad(url, webImageView, compressionNeeded);
		synchronized (photosQueue.photosToLoad) {
			photosQueue.photosToLoad.push(p);
			photosQueue.photosToLoad.notifyAll();
		}

		// start thread if it's not started yet
		if (photoLoaderThread.getState() == Thread.State.NEW) {
			photoLoaderThread.start();
		}
	}

	private void queuePhoto(final String url, final ImageView imageView, final boolean compressionNeeded) {

		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		photosQueue.Clean(imageView);
		final PhotoToLoad p = new PhotoToLoad(url, imageView, compressionNeeded);
		synchronized (photosQueue.photosToLoad) {
			photosQueue.photosToLoad.push(p);
			photosQueue.photosToLoad.notifyAll();
		}

		// start thread if it's not started yet
		if (photoLoaderThread.getState() == Thread.State.NEW) {
			photoLoaderThread.start();
		}
	}

	public File getFile(final String url, final boolean compressed) {
		final String filename = String.valueOf(url.hashCode());
		final File file = new File(cacheDir, filename);

		final Bitmap bitmap = decodeFile(file, compressed);
		final File ret;
		if (bitmap != null) {
			ret = file;
			bitmap.recycle();
		} else {
			ret = null;
		}
		return ret;
	}

	private Bitmap downloadBitmap(final String url, final boolean compressionNeeded) {
		// I identify images by hashcode. Not a perfect solution
		final String filename = String.valueOf(url.hashCode());
		final File f = new File(cacheDir, filename);

		// from SD cache
		final Bitmap b = decodeFile(f, compressionNeeded);
		if (b != null)
			return b;

		// from web
		try {
			Bitmap bitmap = null;
			LOG.debug("ImageLoader start downloading " + url);
			// TODO: compress image before saving to File
			final VolvoApplication app = VolvoApplication.getInstance();
			final InputStream is = (app.isLocal() && !url.startsWith("http://") || url.startsWith(getUrlPrefix(true))) ? app
					.getLocalInputStream(url) : (url.startsWith("http://")) ? new URL(url).openStream() : new URL(
					VolvoSyncService.getEndPoint(app) + url).openStream();
			final OutputStream os = new FileOutputStream(f);
			IOUtil.copy(is, os);
			os.close();
			bitmap = decodeFile(f, compressionNeeded);
			return bitmap;
		} catch (final Exception e) {
			LOG.error(e);
			return null;
		}
	}

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(final File f, final boolean compressionNeeded) {
		try {
			int scale = 1;

			if (compressionNeeded) {
				// decode image size
				final BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(new FileInputStream(f), null, o);

				// Find the correct scale value. It should be the power of 2.
				final int REQUIRED_X_SIZE = 200;
				final int REQUIRED_Y_SIZE = 200;
				int width_tmp = o.outWidth, height_tmp = o.outHeight;

				while (true) {
					if (width_tmp / 2 < REQUIRED_X_SIZE || height_tmp / 2 < REQUIRED_Y_SIZE)
						break;
					width_tmp /= 2;
					height_tmp /= 2;
					// scale *= 2;
					scale <<= 1;
				}
				// scale=2;
			} else {
				final Context context = VolvoApplication.getInstance().getApplicationContext();
				final DisplayMetrics displayMetrics = DisplayUtil.getDisplayMetrics(context);
				final int size = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);

				final BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(new FileInputStream(f), null, o);

				scale = BitmapUtil.getSample(o, size, size);
			}

			final BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			// return BitmapFactory.decodeStream(new FileInputStream(f), null,
			// o2);
			// } catch (final FileNotFoundException e) {
			return BitmapUtil.decodeStream(new FileInputStream(f), null, o2);
		} catch (final IOException e) {
			// LOG.error(e);
		}
		return null;
	}

	// Task for the queue
	private class PhotoToLoad {
		public String url;
		public ImageView imageView;
		public WebImageView webImageView;
		public BitmapCallback callback;
		boolean compressionNeeded;

		public PhotoToLoad(final String u, final ImageView i, final boolean compressionNeeded) {
			url = u;
			imageView = i;
			this.compressionNeeded = compressionNeeded;
		}

		public PhotoToLoad(final String u, final WebImageView i, final boolean compressionNeeded) {
			url = u;
			webImageView = i;
			this.compressionNeeded = compressionNeeded;
		}

		public PhotoToLoad(final String u, final BitmapCallback callback, final boolean compressionNeeded) {
			url = u;
			this.callback = callback;
			this.compressionNeeded = compressionNeeded;
		}
	}

	// stores list of photos to download
	private class PhotosQueue {
		private final Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();

		// removes all instances of this ImageView
		public void Clean(final ImageView image) {
			for (int j = 0; j < photosToLoad.size();) {
				if (photosToLoad.get(j).imageView == image)
					photosToLoad.remove(j);
				else
					++j;
			}
		}

		// removes all instances of this ImageView
		public void Clean(final WebImageView image) {
			for (int j = 0; j < photosToLoad.size();) {
				if (photosToLoad.get(j).webImageView == image)
					photosToLoad.remove(j);
				else
					++j;
			}
		}

		// removes all instances of this Runnable
		public void Clean(final BitmapCallback callback) {
			for (int j = 0; j < photosToLoad.size();) {
				if (photosToLoad.get(j).callback == callback)
					photosToLoad.remove(j);
				else
					++j;
			}
		}

	}

	class PhotosLoader extends Thread {
		PhotosLoader() {
			this.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
		}

		@Override
		public void run() {
			try {
				while (true) {
					// thread waits until there are any images to load in the
					// queue
					if (photosQueue.photosToLoad.size() == 0)
						synchronized (photosQueue.photosToLoad) {
							photosQueue.photosToLoad.wait();
						}
					if (photosQueue.photosToLoad.size() != 0) {
						PhotoToLoad photoToLoad;
						synchronized (photosQueue.photosToLoad) {
							photoToLoad = photosQueue.photosToLoad.pop();
						}
						final Bitmap bmp = downloadBitmap(photoToLoad.url, photoToLoad.compressionNeeded);
						String key = photoToLoad.url;
						if (photoToLoad.compressionNeeded) {
							key = key + "+compressed";
						}
						cache.put(key, new SoftReference<Bitmap>(bmp));
						final ImageView imageView = photoToLoad.imageView;
						if (imageView != null) {
							final Object tag = imageView.getTag();
							if (tag != null && ((String) tag).equals(photoToLoad.url)) {
								final BitmapDisplayer bd = new BitmapDisplayer(bmp, imageView);
								final Activity a = (Activity) imageView.getContext();
								a.runOnUiThread(bd);
							}
						} else {
							final WebImageView webImageView = photoToLoad.webImageView;
							if (webImageView != null) {
								final Object tag = webImageView.getTag();
								if (tag != null && ((String) tag).equals(photoToLoad.url)) {
									final BitmapDisplayer bd = new BitmapDisplayer(bmp, webImageView);
									final Activity a = (Activity) webImageView.getContext();
									a.runOnUiThread(bd);
								}
							} else {
								final BitmapCallback callback = photoToLoad.callback;
								if (callback != null) {
									callback.setBitmap(bmp);
								}
							}
						}
					}
					if (Thread.interrupted()) {
						break;
					}
				}
			} catch (final InterruptedException e) {
				// allow thread to exit
			}
		}
	}

	public interface BitmapCallback {
		void setBitmap(Bitmap bitmap);
	}

	// Used to display bitmap in the UI thread
	private class BitmapDisplayer implements Runnable {
		private final Bitmap bitmap;
		private final ImageView imageView;
		private final WebImageView webImageView;

		public BitmapDisplayer(final Bitmap b, final ImageView i) {
			this(b, i, null);
		}

		public BitmapDisplayer(final Bitmap b, final WebImageView w) {
			this(b, null, w);
		}

		private BitmapDisplayer(final Bitmap b, final ImageView i, final WebImageView w) {
			bitmap = b;
			imageView = i;
			webImageView = w;
		}

		@Override
		public void run() {
			if (imageView != null) {
				if (bitmap != null)
					imageView.setImageBitmap(bitmap);
				else
					imageView.setImageResource(stub_id);
			} else if (webImageView != null) {
				if (bitmap != null) {
					webImageView.setImageBitmap(bitmap);
				}
				webImageView.showImage();
			}
		}
	}

}

