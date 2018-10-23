// https://searchcode.com/api/result/11440015/

package com.stanfy.app.service;

import java.io.File;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.stanfy.DebugFlags;
import com.stanfy.app.Application;
import com.stanfy.images.ImagesDAO;
import com.stanfy.images.ImagesManager;
import com.stanfy.images.ImagesManagerContext;
import com.stanfy.images.model.CachedImage;
import com.stanfy.utils.AppUtils;

/**
 * Application service that helps to perform different auxiliary tasks.
 * <ul>
 *   <li>images cache cleanup</li>
 * </ul>
 * @author Roman Mazur (Stanfy - http://www.stanfy.com)
 */
public class ToolsApplicationService extends IntentService {

  /** Maximum images cache size (defult value). */
  public static final long DEFAULT_MAX_IMAGES_CACHE_SIZE = 10 * 1024 * 1024;

  /** Images cache cleanup. */
  public static final String ACTION_IMAGES_CACHE_CLEANUP = "com.stanfy.images.CLEANUP";

  /** Max cache size key. */
  public static final String EXTRA_MAX_CACHE_SIZE = "max_cache_s";

  /** Logging tag. */
  private static final String TAG = "ToolsService";
  /** Debug flag. */
  private static final boolean DEBUG = DebugFlags.DEBUG_SERVICES;

  public ToolsApplicationService() {
    super("tools-service");
  }

  /**
   * Delete the file and check whether it's possible to delete parents, delete if possible.
   * @param f file to delete
   */
  protected static void deleteFileWithParent(final File f) {
    final boolean res = f.delete();
    if (!res) { return; }
    final File parent = f.getParentFile();
    if (parent != null) { deleteFileWithParent(parent); }
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    final String action = intent.getAction();
    if (action == null) { return; }

    if (ACTION_IMAGES_CACHE_CLEANUP.equals(action)) {
      cleanupImagesCache(intent.getLongExtra(EXTRA_MAX_CACHE_SIZE, DEFAULT_MAX_IMAGES_CACHE_SIZE));
    }

  }

  /** @return images context instance */
  protected ImagesManagerContext<?> getImagesContext() { return ((Application)getApplication()).getImagesContext(); }

  /** @return cached image instance */
  protected CachedImage createCachedImage() { return new CachedImage(); }

  /**
   * @param image cached image instance
   * @param c cursor instance
   */
  protected void readCachedImage(final CachedImage image, final Cursor c) {
    CachedImage.Contract.fromCursor(c, image);
  }

  /**
   * Clean old image cache entries.
   * @param maxCacheSize maximum cache size
   */
  protected void cleanupImagesCache(final long maxCacheSize) {
    if (DEBUG) { Log.i(TAG, "Start images cleanup"); }
    final ImagesManagerContext<?> iContext = getImagesContext();
    if (iContext == null) { return; }
    final long time = System.currentTimeMillis();
    final ImagesDAO<?> dao = iContext.getImagesDAO();

    final ImagesManager<?> manager = iContext.getImagesManager();
    final CachedImage image = createCachedImage();
    final Context context = getApplicationContext();

    final Cursor c = dao.getOldImages(time);
    if (c != null) {
      if (DEBUG) { Log.i(TAG, "Have images to clean " + c.getCount()); }
      try {
        while (c.moveToNext()) {
          readCachedImage(image, c);
          manager.clearCache(context, image.getPath(), image.getUrl());
        }
      } finally {
        c.close();
      }
      final int count = dao.deleteOldImages(time);
      if (DEBUG) { Log.i(TAG, "Deleted " + count + "images"); }
    }

    long dirSize = AppUtils.sizeOfDirectory(manager.getImageDir(context));
    if (DEBUG) { Log.i(TAG, "Cache size: " + dirSize); }
    if (dirSize <= maxCacheSize) { return; }
    if (DEBUG) { Log.i(TAG, "Cache is too large"); }
    final Cursor candidates = dao.getLessUsedImages();
    if (candidates == null) {
      if (DEBUG) { Log.i(TAG, "Nothing to delete"); }
      return;
    }

    int count = 0, defaultTypeCount = 0;
    try {
      while (dirSize > maxCacheSize && candidates.moveToNext()) {
        readCachedImage(image, candidates);
        dirSize -= manager.clearCache(context, image.getPath(), image.getUrl());
        ++count;
        if (image.getType() == 0) { ++defaultTypeCount; }
        dao.deleteImage(image.getId());
      }
    } catch (final Exception e) {
      Log.e(TAG, "Cannot reduce cache size", e);
    } finally {
      candidates.close();
    }
    if (DEBUG) { Log.i(TAG, "Removed " + count + " images (default type - " + defaultTypeCount + "), cache size: " + dirSize); }
  }

}

