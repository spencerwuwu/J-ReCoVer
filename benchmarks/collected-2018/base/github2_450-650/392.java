// https://searchcode.com/api/result/94337031/

/*
 * Twidere - Twitter client for Android Copyright (C) 2012 Mariotaku Lee
 * <mariotaku.lee@gmail.com> This program is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.tweetlanes.android.core.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStorageState;
// import static org.mariotaku.twidere.util.Utils.getProxy;
// import static com.tweetlanes.android.core.util.parseURL;
// import static org.mariotaku.twidere.util.Utils.setIgnoreSSLError;

/**
 * Lazy image loader for {@link ListView} and {@link GridView} etc.</br> </br>
 * Inspired by <a href="https://github.com/thest1/LazyList">LazyList</a>, this
 * class has extra features like image loading/caching image to
 * /mnt/sdcard/Android/data/[package name]/cache features.</br> </br> Requires
 * Android 2.2, you can modify {@link Context#getExternalCacheDir()} to other to
 * support Android 2.1 and below.
 *
 * @author mariotaku
 */
public class LazyImageLoader {

    private final MemoryCache mMemoryCache;
    private final FileCache mFileCache;
    private final Map<ImageView, URL> mImageViews = Collections
            .synchronizedMap(new WeakHashMap<ImageView, URL>());
    private final ExecutorService mExecutorService;
    private final int mFallbackRes;
    private final int mRequiredWidth, mRequiredHeight;
    private final boolean mNoScale;
    private final Context mContext;
    private Proxy mProxy;

    public LazyImageLoader(Context context, String cache_dir_name,
                           int fallback_image_res, int required_width, int required_height,
                           boolean noScale, int mem_cache_capacity) {
        mContext = context;

        mMemoryCache = new MemoryCache(mem_cache_capacity);
        mFileCache = new FileCache(mContext, cache_dir_name);
        mExecutorService = Executors.newFixedThreadPool(5);
        mFallbackRes = fallback_image_res;
        mRequiredWidth = required_width % 2 == 0 ? required_width
                : required_width + 1;
        mRequiredHeight = required_height % 2 == 0 ? required_height
                : required_height + 1;
        mNoScale = noScale;
        mProxy = Util.getProxy(mContext);

    }

    public void clearCache() {
        Set<URL> clearedUrls = mMemoryCache.clear();
        mFileCache.clearUrls(clearedUrls);

        Set<URL> activeUrls = mMemoryCache.getActiveUrls();
        mFileCache.clearUnrecognisedFiles(activeUrls);
    }

    public void displayImage(String url, ImageView imageview) {
        displayImage(Util.parseURL(url), imageview);
    }

    void displayImage(URL url, ImageView imageview) {
        if (imageview == null) return;
        if (url == null) {
            imageview.setImageResource(mFallbackRes);
            return;
        }
        mImageViews.put(imageview, url);
        final Bitmap bitmap = mMemoryCache.get(url, mFileCache);
        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
        } else {
            queuePhoto(url, imageview);
            imageview.setImageResource(mFallbackRes);
        }
    }

    private static void copyStream(InputStream is, OutputStream os) {
        final int buffer_size = 1024;
        try {
            final byte[] bytes = new byte[buffer_size];
            int count = is.read(bytes, 0, buffer_size);
            while (count != -1) {
                os.write(bytes, 0, count);
                count = is.read(bytes, 0, buffer_size);
            }
        } catch (final IOException e) {
            // e.printStackTrace();
        }
    }

    // decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f) {
        InputStream enter = null;
        InputStream exit = null;
        try {
            // decode image size
            enter = new FileInputStream(f);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(enter, null, options);

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            if(!mNoScale)
            {
                int width_tmp = options.outWidth, height_tmp = options.outHeight;
                while (width_tmp / 2 >= mRequiredWidth
                        || height_tmp / 2 >= mRequiredHeight) {
                    width_tmp /= 2;
                    height_tmp /= 2;
                    scale *= 2;
                }
            }

            // decode with inSampleSize
            exit = new FileInputStream(f);
            final BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            final Bitmap bitmap = BitmapFactory.decodeStream(exit, null, o2);
            if (bitmap == null) {
                // The file is corrupted, so we remove it from cache.
                if (f.isFile()) {
                    f.delete();
                }
            }
            return bitmap;
        } catch (final FileNotFoundException e) {
            // e.printStackTrace();
        } finally {
            Util.closeQuietly(enter);
            Util.closeQuietly(exit);
        }
        return null;
    }

    private void queuePhoto(URL url, ImageView imageview) {
        final ImageToLoad p = new ImageToLoad(url, imageview);
        mExecutorService.submit(new ImageLoader(p));
    }

    boolean imageViewReused(ImageToLoad imagetoload) {
        final Object tag = mImageViews.get(imagetoload.imageview);
        return tag == null || !tag.equals(imagetoload.source);
    }

    // Used to display bitmap in the UI thread
    private class BitmapDisplayer implements Runnable {

        final Bitmap mBitmap;
        final ImageToLoad mImageToLoad;

        public BitmapDisplayer(Bitmap b, ImageToLoad p) {
            mBitmap = b;
            mImageToLoad = p;
        }

        @Override
        public final void run() {
            if (imageViewReused(mImageToLoad)) return;
            if (mBitmap != null) {
                mImageToLoad.imageview.setImageBitmap(mBitmap);
            } else {
                mImageToLoad.imageview.setImageResource(mFallbackRes);
            }
        }
    }

    private static class FileCache {

        private final String mCacheDirName;

        private File mCacheDir;
        private final Context mContext;

        public FileCache(Context context, String cache_dir_name) {
            mContext = context;
            mCacheDirName = cache_dir_name;
            init();
        }

        public File getFile(URL tag) {
            if (mCacheDir == null) return null;
            final String filename = getURLFilename(tag);
            if (filename == null) return null;
            return new File(mCacheDir, filename);
        }

        public void saveFile(Bitmap image, URL tag) {
            if (mCacheDir == null) return;
            final String filename = getURLFilename(tag);
            if (filename == null) return;
            final File file = new File(mCacheDir, filename);
            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();
            } catch (IOException e) {

            }
        }

        private void deleteFile(final URL tag) {
            if (mCacheDir == null) return;
            final File[] files = mCacheDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName() == getURLFilename(tag);
                }
            });
            if (files == null) return;
            for (final File f : files) {
                f.delete();
            }
        }

        public void clearUrls(Set<URL> urls) {
            for (URL url : urls) {
                deleteFile(url);
            }
        }

        public void clearUnrecognisedFiles(Set<URL> urls) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);

            final File[] files = mCacheDir.listFiles();
            if (files == null) return;
            for (final File f : files) {
                Date lastModDate = new Date(f.lastModified());
                if (lastModDate.before(cal.getTime())) {
                    boolean fileInCache = false;
                    for (URL url : urls) {
                        if (f.getName() == getURLFilename(url)) {
                            fileInCache = true;
                        }
                    }

                    if (!fileInCache) {
                        f.delete();
                    }
                }
            }
        }

        public void init() {
            /* Find the dir to save cached images. */
            if (getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mCacheDir = new File(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? mContext
                                .getExternalCacheDir()
                                : new File(getExternalStorageDirectory()
                                .getPath()
                                + "/Android/data/"
                                + mContext.getPackageName() + "/cache/"),
                        mCacheDirName);
            } else {
                mCacheDir = new File(mContext.getCacheDir(), mCacheDirName);
            }
            if (mCacheDir != null && !mCacheDir.exists()) {
                mCacheDir.mkdirs();
            }
        }

        private static String getURLFilename(URL url) {
            if (url == null) {
                return null;
            }
            return url.toString().replaceAll("[^a-zA-Z0-9]", "_");
        }

    }

    private class ImageLoader implements Runnable {

        private final ImageToLoad mImageToLoad;

        public ImageLoader(ImageToLoad imagetoload) {
            this.mImageToLoad = imagetoload;
        }

        private Bitmap getBitmap(URL url) {
            if (url == null) return null;
            final File f = mFileCache.getFile(url);

            // from SD cache
            final Bitmap b = decodeFile(f);
            if (b != null) return b;

            // from web
            return DownloadBitmapFromWeb(url, f, false);

        }

        private Bitmap DownloadBitmapFromWeb(URL url, File f, Boolean isRetry) {
            try {
                Bitmap bitmap;
                final HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection(mProxy);

                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                final InputStream is = conn.getInputStream();
                final OutputStream os = new FileOutputStream(f);
                copyStream(is, os);
                is.close();
                os.close();
                bitmap = decodeFile(f);

                final int bitmapBytes = bitmap.getByteCount();
                if (bitmapBytes == 0) {
                    if (isRetry) {
                        return null;
                    }
                    return DownloadBitmapFromWeb(url, f, true);
                }
                mFileCache.saveFile(bitmap, url);
                return bitmap;
            } catch (final FileNotFoundException e) {
                // Storage state may changed, so call FileCache.init() again.
                // e.printStackTrace();
                mFileCache.init();
            } catch (final IOException e) {
                // e.printStackTrace();
            }
            return null;
        }

        @Override
        public void run() {
            final Bitmap bmp = getBitmap(mImageToLoad.source);
            if (imageViewReused(mImageToLoad) || mImageToLoad.source == null)
                return;
            mMemoryCache.put(mImageToLoad.source, bmp);
            if (imageViewReused(mImageToLoad)) return;
            final BitmapDisplayer bd = new BitmapDisplayer(bmp, mImageToLoad);
            final Activity a = (Activity) mImageToLoad.imageview.getContext();
            a.runOnUiThread(bd);
        }
    }

    private static class ImageToLoad {

        public final URL source;
        public final ImageView imageview;

        public ImageToLoad(final URL source, final ImageView imageview) {
            this.source = source;
            this.imageview = imageview;
        }
    }

    public static class ExpiringBitmap {

        public final Bitmap image;
        public final Date expires;

        public ExpiringBitmap(final Bitmap image, final Date expires) {
            this.image = image;
            this.expires = expires;
        }
    }

    private static class MemoryCache {

        private final int mMaxCapacity;
        private final Map<URL, SoftReference<ExpiringBitmap>> mSoftCache;
        private final Map<URL, ExpiringBitmap> mHardCache;

        public MemoryCache(int max_capacity) {
            mMaxCapacity = max_capacity;
            mSoftCache = new ConcurrentHashMap<URL, SoftReference<ExpiringBitmap>>();
            mHardCache = new LinkedHashMap<URL, ExpiringBitmap>(mMaxCapacity / 3,
                    0.75f, true) {

                private static final long serialVersionUID = 1347795807259717646L;

                @Override
                protected boolean removeEldestEntry(
                        LinkedHashMap.Entry<URL, ExpiringBitmap> eldest) {
                    // Moves the last used item in the hard cache to the soft
                    // cache.
                    if (size() > mMaxCapacity) {
                        mSoftCache.put(eldest.getKey(),
                                new SoftReference<ExpiringBitmap>(eldest.getValue()));
                        return true;
                    } else
                        return false;
                }
            };
        }

        public Set<URL> clear() {
            Set<URL> clearedUrls = new HashSet<URL>();

            synchronized (mHardCache) {

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                Map<URL, ExpiringBitmap> copy = new HashMap<URL, ExpiringBitmap>(mHardCache);
                for (URL url : copy.keySet()) {

                    ExpiringBitmap bitmap = mHardCache.get(url);
                    if (bitmap != null) {
                        if (bitmap.expires.before(cal.getTime())) {
                            clearedUrls.add(url);
                        }
                    }
                }
                for (URL url : clearedUrls) {
                    mHardCache.remove(url);
                    mSoftCache.remove(url);
                }
            }

            return clearedUrls;
        }

        public Set<URL> getActiveUrls() {
            return new HashSet<URL>(mHardCache.keySet());
        }

        public Bitmap get(final URL url, final FileCache fileCache) {
            synchronized (mHardCache) {
                ExpiringBitmap bitmap = mHardCache.get(url);
                if (bitmap != null) {
                    if (bitmap.expires.before(new Date())) {
                        mHardCache.remove(url);
                        fileCache.deleteFile(url);
                    } else {
                        // Put bitmap on top of cache so it's purged last.
                        try {
                            mHardCache.remove(url);
                            mHardCache.put(url, bitmap);
                        } catch (Exception e) {

                        }
                        return bitmap.image;
                    }
                }
            }

            final SoftReference<ExpiringBitmap> bitmapRef = mSoftCache.get(url);
            if (bitmapRef != null) {
                final ExpiringBitmap bitmap = bitmapRef.get();
                if (bitmap != null)
                    return bitmap.image;
                else {
                    // Must have been collected by the Garbage Collector
                    // so we remove the bucket from the cache.
                    mSoftCache.remove(url);
                }
            }

            // Could not locate the bitmap in any of the caches, so we return
            // null.
            return null;

        }

        public void put(final URL url, final Bitmap bitmap) {
            if (url == null || bitmap == null) return;

            if (mHardCache.get(url) == null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, +1);
                mHardCache.put(url, new ExpiringBitmap(bitmap, cal.getTime()));
            }
        }
    }

}

