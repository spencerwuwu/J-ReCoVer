// https://searchcode.com/api/result/59484970/

package org.eid103.treasurehunt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

/**
 * <p>
 * Manages downloading and storage of hunts from the server. Call
 * {@link #loadHunts(SharedPreferences)} to load the list of hunts. Hints are
 * only downloaded when {@link #loadHunts} is called. Otherwise, hunts only have
 * an empty list of hints.
 * </p>
 * 
 * <p>
 * Since data is loaded in a separate thread, make sure to add a
 * {@link DataSetObserver} using {@link #addObserver(DataSetObserver)} to update
 * your view as data is being downloaded and read.
 * <p>
 * Hunts are loaded as JSON files from the specified {@link #TreasureHunt.DOMAIN} and
 * under the specified {@link #URL} using {@link JSONObject} and
 * {@link JSONArray}.
 * </p>
 * 
 * @author Eric Leong
 * @see {@link Hunt}
 */
public class HuntManager {

	/**
	 * Loads hunts and hints from the application's internal, offline database
	 * from a separate thread.
	 * 
	 * @author Eric Leong
	 * @see {@link SQLiteDatabase}, {@link HuntOpenHelper}
	 * @see {@link HuntManager}, {@link NetworkThread}
	 * @see {@link Hunt}, {@link Hint}
	 */
	private class DatabaseThread extends Thread {

		/**
		 * The index of the {@link Hunt} being loaded. <br />
		 * <code>-1</code> if a list of hunts is being loaded.
		 */
		private int huntId;

		/**
		 * The <code>SharedPreferences</code> file that stores the user's
		 * progress.
		 */
		private SharedPreferences settings;

		/**
		 * Creates a {@link DatabaseThread} that loads the hints belonging to
		 * the hunt with the given index.
		 * 
		 * @param huntId
		 *            the index of the hunt in the list of hunts
		 * 
		 * @see {@link Hunt}, {@link Hint}
		 */
		public DatabaseThread(int huntId) {
			this.huntId = huntId;
		}

		/**
		 * Creates a {@link DatabaseThread} that loads the hunts stored in the
		 * application's database. Progress is loaded from the
		 * <code>settings</code> file.
		 * 
		 * @param settings
		 *            the <code>SharedPreferences</code> object that stores the
		 *            user's progress
		 * 
		 * @see {@link SharedPreferences}
		 */
		public DatabaseThread(SharedPreferences settings) {
			huntId = -1; // a hunt is not being loaded
			this.settings = settings;
		}

		/**
		 * Loads a list of hints belonging to a specific hunt from the database.
		 */
		private void loadHint() {
			// create database opener to get this application's database
			final HuntOpenHelper huntOpenHelper = new HuntOpenHelper(context);
			// grab the database
			SQLiteDatabase sqliteDatabase = huntOpenHelper.getReadableDatabase();
			// grab all the hints in the hints table that belong to the current
			// hunt
			Cursor cursor = sqliteDatabase.query(HuntOpenHelper.HINT_TABLE_NAME, null,
					Hint.COLUMN_HUNT + "=" + huntId, null, null, null, "id ASC");
			// make sure there are items
			if (cursor.moveToFirst()) { // move to first element
				// iterate through the list of hints
				for (; !cursor.isAfterLast(); cursor.moveToNext()) {

					// create the hint with the row values
					final Hint hint = new Hint(huntId, cursor.getInt(cursor.getColumnIndex("id")),
							cursor.getString(cursor.getColumnIndex(Hint.COLUMN_HINT)),
							cursor.getString(cursor.getColumnIndex(Hint.COLUMN_DATA)),
							cursor.getString(cursor.getColumnIndex(Hint.COLUMN_TYPE)),
							new GeoPoint(
									cursor.getInt(cursor.getColumnIndex(Hint.COLUMN_LATITUDE)),
									cursor.getInt(cursor.getColumnIndex(Hint.COLUMN_LONGITUDE))));

					// finished?
					final boolean done = cursor.isLast();

					// post the new hunt to the current instance of HuntManager
					handler.post(new Runnable() {
						@Override
						public void run() {
							// post the progress status
							sInstance.loading = !done;
							// add the hint to the hunt's list of hints
							sInstance.add(huntId, hint);
						}
					});
					cursor.close();
				}
			}
			sqliteDatabase.close();
		}

		/**
		 * Loads a list of hunts from the database.
		 */
		private void loadHunt() {
			// create database opener to get this application's database
			final HuntOpenHelper huntOpenHelper = new HuntOpenHelper(context);
			// grab the database
			SQLiteDatabase sqliteDatabase = huntOpenHelper.getReadableDatabase();
			// grab all hunts in the hunts table
			Cursor cursor = sqliteDatabase.query(HuntOpenHelper.HUNT_TABLE_NAME, null, null, null,
					null, null, "id ASC");
			// make sure there are items
			if (cursor.moveToFirst()) { // move to first element
				// iterate through the list of hunts
				for (; !cursor.isAfterLast(); cursor.moveToNext()) {
					// get the current hunt's id
					int id = cursor.getInt(cursor.getColumnIndex("id"));
					// use the id to get the user's progress
					int currentHint = settings.getInt(Integer.toString(id), 0);

					// create the hunt using the current row's values
					final Hunt hunt = new Hunt(id, cursor.getString(cursor
							.getColumnIndex(Hunt.COLUMN_NAME)), cursor.getString(cursor
							.getColumnIndex(Hunt.COLUMN_CREATOR)), cursor.getString(cursor
							.getColumnIndex(Hunt.COLUMN_PUBDATE)), cursor.getInt(cursor
							.getColumnIndex(Hunt.COLUMN_POINTS)), currentHint);

					// finished?
					final boolean done = cursor.isLast();

					// post the new hunt to the current instance of HuntManager
					handler.post(new Runnable() {
						@Override
						public void run() {
							// post progress status
							sInstance.loading = !done;
							// add the hunt to the list of hunts
							sInstance.add(hunt);
						}
					});
				}
				cursor.close();
			}
			sqliteDatabase.close();
		}

		@Override
		public void run() {
			if (huntId >= 0) // a hunt is being loaded
				loadHint();
			else
				// load list of hunts
				loadHunt();
		}
	}

	/**
	 * This thread does the actual work of downloading and parsing data.
	 */
	private class NetworkThread extends Thread {

		private static final String TAG = "NetworkThread";

		/**
		 * Whether or not hint data is being downloaded.
		 */
		boolean hint;

		int huntId;

		/**
		 * Settings file used to store progress.
		 */
		SharedPreferences settings;

		/**
		 * Url to download data from. It should <b>not</b> include "http:".
		 */
		String url;

		/**
		 * Creates a network thread to download {@link Hint} data from the
		 * specified url (not necessarily under the predefined
		 * {@link HuntManager#URL}).
		 * 
		 * @param url
		 *            the url to the hint json file without the /json/ suffix,
		 *            which should <b>not</b> include "http:"
		 */
		public NetworkThread(int huntId) {
			this.url = URL + huntId + "/" + JSON;
			this.huntId = huntId;
			this.hint = true;
		}

		/**
		 * Creates a network thread with the specified progress file to download
		 * {@link Hunt} data from the specified url (not necessarily under the
		 * predefined {@link HuntManager#URL}).
		 * 
		 * @param settings
		 *            the progress file, which is used to update the current
		 *            hint of the <code>Hunt</code>
		 * @param url
		 *            the url to the list of hunts without the /json/ suffix,
		 *            which should <b>not</b> include "http:"
		 */
		public NetworkThread(SharedPreferences settings) {
			this.settings = settings;
			this.url = URL + JSON;
			this.hint = false;
		}

		/**
		 * Parses the JSON file of hints.
		 * 
		 * @param string
		 *            The string of downloaded data.
		 */
		private void parseHints(String string) {
			try {
				// interpret String as a JSON array
				JSONArray json = new JSONArray(string);
				for (int i = 0; i < json.length(); i++) {
					JSONObject jhint = json.getJSONObject(i);
					int pk = jhint.getInt("pk");

					// grab variables
					JSONObject jfields = jhint.getJSONObject("fields");
					String hintStr = jfields.getString(Hint.COLUMN_HINT);
					String data = jfields.getString(Hint.COLUMN_DATA);
					int location_latitudeE6 = jfields.getInt(Hint.COLUMN_LATITUDE);
					int location_longitudeE6 = jfields.getInt(Hint.COLUMN_LONGITUDE);
					String type = jfields.getString(Hint.COLUMN_TYPE);
					// index 0
					final int huntId = jfields.getInt(Hint.COLUMN_HUNT);

					// create hint object
					final Hint hint = new Hint(huntId, pk, hintStr, data, type, new GeoPoint(
							location_latitudeE6, location_longitudeE6));

					// finished?
					final boolean done = i >= json.length() - 1;

					// post the new hint to the current instance of HuntManager
					handler.post(new Runnable() {
						@Override
						public void run() {
							// post current progress state
							sInstance.loading = !done;
							// add hint to the database
							sInstance.store(huntId, hint);
							// add hint to the hunt's current list of hints
							sInstance.add(huntId, hint);
						}
					});
				}
			} catch (JSONException e) {
				Log.e(TAG, e.toString());
			}
		}

		/**
		 * Parses the JSON file of hunts.
		 * 
		 * @param string
		 *            The string of downloaded data.
		 */
		private void parseHunts(String string) {
			try {
				// interpret String as a JSON array
				JSONArray json = new JSONArray(string);
				for (int i = 0; i < json.length(); i++) {
					JSONObject jhunt = json.getJSONObject(i);
					int pk = jhunt.getInt("pk");

					// grab variables
					JSONObject jfields = jhunt.getJSONObject("fields");
					String name = jfields.getString(Hunt.COLUMN_NAME);
					String creator = jfields.getString(Hunt.COLUMN_CREATOR);
					int points = jfields.getInt(Hunt.COLUMN_POINTS);
					String pubDate = jfields.getString(Hunt.COLUMN_PUBDATE);

					// load how far the user currently is
					final int currentHint = settings.getInt(Integer.toString(pk), 0);

					// create hunt
					final Hunt hunt = new Hunt(pk, name, creator, pubDate, points, currentHint);

					// finished?
					final boolean done = i >= json.length() - 1;

					// post the new hunt to the current instance of HuntManager
					handler.post(new Runnable() {
						@Override
						public void run() {
							// post current state
							sInstance.loading = !done;
							// store hunt in database
							sInstance.store(hunt);
							// add it to the current list of hunts
							sInstance.add(hunt);
						}
					});
				}
			} catch (JSONException e) {
				Log.e(TAG, e.toString());
			}
		}

		@Override
		public void run() {
			// not sure if this is completely necessary
			Looper.prepare();

			url = String.format(url);
			// get data from the specified url
			URI uri;
			try {
				uri = new URI("http", url, null);
				HttpGet get = new HttpGet(uri); // construct a get request
				// construct a client
				DefaultHttpClient client = AccountManager.getInstance(context).getClient();
				// receive data from the server
				HttpResponse response = client.execute(get);
				HttpEntity entity = response.getEntity();
				// turn data stream into a string
				String str = convertStreamToString(entity.getContent());

				// a different function is called depending on what is being
				// downloaded
				if (hint)
					parseHints(str);
				else
					parseHunts(str);
			} catch (URISyntaxException e) {
				Toast.makeText(context, context.getString(R.string.error_connect),
						Toast.LENGTH_SHORT);
				Log.e(TAG, e.toString());
				// load from database instead
				if (hint)
					loadDatabaseHints(huntId);
				else
					loadDatabaseHunts(settings);
			} catch (ClientProtocolException e) {
				Toast.makeText(context, context.getString(R.string.error_connect),
						Toast.LENGTH_SHORT);
				Log.e(TAG, e.toString());
				// load from database instead
				if (hint)
					loadDatabaseHints(huntId);
				else
					loadDatabaseHunts(settings);
			} catch (IOException e) {
				Toast.makeText(context, context.getString(R.string.error_connect),
						Toast.LENGTH_SHORT);
				Log.e(TAG, e.toString());
				// load from database instead
				if (hint) {
					loadDatabaseHints(huntId);
				} else
					loadDatabaseHunts(settings);
			} catch (IllegalStateException e) {
				Toast.makeText(context, context.getString(R.string.error_connect),
						Toast.LENGTH_SHORT);
				Log.e(TAG, e.toString());
				// load from database instead
				if (hint) {
					loadDatabaseHints(huntId);
				} else
					loadDatabaseHunts(settings);
			}
		}
	}

	/**
	 * URL to append to load JSON.
	 */
	public static final String JSON = "json/";

	/**
	 * URL to media files.
	 */
	public static final String MEDIA = "http:" + TreasureHunt.SLASH_DOMAIN + "/media/";

	/**
	 * Holds the single instance of a HuntManager that is shared by the process.
	 */
	private static HuntManager sInstance;

	/**
	 * String for loading a hint extra.
	 */
	public static final String TREASUREHUNT_HINT_EXTRA = "org.eid103.treasurehunt.Hint";

	/**
	 * String for loading a hunt extra.
	 */
	public static final String TREASUREHUNT_HUNT_EXTRA = "org.eid103.treasurehunt.Hunt";

	/**
	 * String for loading a hunt index Intent extra.
	 */
	public static final String TREASUREHUNT_HUNTID_EXTRA = "huntid";

	/**
	 * URL to the JSON server.
	 */
	public static final String URL = TreasureHunt.SLASH_DOMAIN + "/hunts/";

	private static final String TAG = "HuntManager";

	/**
	 * Converts an {@link InputStream} to a <code>String</code> using a
	 * {@link BufferedReader} with a buffer of 8KB.
	 * 
	 * @param inputStream
	 *            the stream to convert into a string
	 * @return A string containing the contents of the input stream.
	 */
	public static final String convertStreamToString(InputStream inputStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 8 * 1024);
		String string = "";

		// read each line of the stream and append it to the string
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				string += line + "\n";
			}
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}

		// close the stream when done
		try {
			inputStream.close();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}

		return string;
	}

	/**
	 * Creates a HuntManager.
	 * 
	 * @param context
	 *            a reference to the local environment
	 * @return The HuntManager shared by the given process.
	 */
	public static HuntManager getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new HuntManager(context.getApplicationContext());
		}
		return sInstance;
	}

	public static final String locate(Context context, String type, String data) {
		// data location depends on type
		String dataLocation = "";
		if (type.equals(Hint.TYPE_IMAGE)) { // image
			dataLocation = MediaStore.Images.Media.DATA;
		} else if (type.equals(Hint.TYPE_AUDIO)) { // audio
			dataLocation = MediaStore.Audio.Media.DATA;
		} else if (type.equals(Hint.TYPE_VIDEO)) { // video
			dataLocation = MediaStore.Video.Media.DATA;
		}

		// get location of image file
		String[] proj = {};
		// use cursor to step through content
		Cursor cursor = context.getContentResolver().query(Uri.parse(data), proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(dataLocation);
		cursor.moveToFirst();
		// get location of file
		return cursor.getString(column_index);
	}

	public static final String locateImage(Context context, String data) {
		return locate(context, Hint.TYPE_IMAGE, data);
	}

	/**
	 * Stores the bitmap on the phone.
	 * 
	 * @param context
	 *            the application environment
	 * @param bitmap
	 *            the bitmap to store
	 * @param fileName
	 *            the name of the file to store it as
	 * @param huntId
	 *            the id of the hunt to store it under
	 * @return Where the bitmap was stored, <code>null</code> if it could not be
	 *         stored.
	 */
	public static final String store(Context context, Bitmap bitmap, String fileName, int huntId) {
		FileOutputStream fileOutputStream;
		try {
			// get the directory the bitmap should be stored in
			File dir = new File(context.getFilesDir(), Integer.toString(huntId));
			if (!dir.exists()) // create it if necessary
				dir.mkdir();
			// get the bitmap file
			dir = new File(dir, fileName);
			if (!dir.exists()) // create the file if it doesn't exist
				dir.createNewFile();

			// prepare the output stream
			fileOutputStream = new FileOutputStream(dir, false);
			// compress the file and store it
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
			// note the storage
			Log.i(TAG, "Image saved to " + dir.getAbsolutePath());

			fileOutputStream.close();
			return dir.getAbsolutePath();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		return null;
	}

	/**
	 * Stores the a scaled bitmap on the phone.
	 * 
	 * @param context
	 *            the application environment
	 * @param location
	 *            the location of the bitmap to store
	 * @param fileName
	 *            the name of the file to store it as
	 * @param huntId
	 *            the id of the hunt to store it under
	 * @param width
	 *            the desired width of the bitmap
	 * @param height
	 *            the desired height of the bitmap
	 * @return Where the bitmap was stored, <code>null</code> if it could not be
	 *         stored.
	 */
	public static final String store(Context context, String location, String fileName, int huntId,
			int width, int height) {

		// code from
		// http://stackoverflow.com/questions/4231817/quality-problems-when-resizing-an-image-at-runtime

		if (width < 1 || height < 1)
			return null;

		// Get the source image's dimensions
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(location, options);

		int srcWidth = options.outWidth;
		int srcHeight = options.outHeight;

		// Only scale if the source is big enough. This code is just trying
		// to fit a image into a certain width.
		if (width > srcWidth)
			width = srcWidth;

		// Calculate the correct inSampleSize/scale value. This helps reduce
		// memory use. It should be a power of 2
		// from:
		// http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/823966#823966
		int inSampleSize = 1;
		while (srcWidth / 2 > width) {
			srcWidth /= 2;
			srcHeight /= 2;
			inSampleSize *= 2;
		}

		// Decode with inSampleSize
		options.inJustDecodeBounds = false;
		options.inDither = false;
		options.inSampleSize = inSampleSize;
		options.inScaled = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap sampledSrcBitmap = BitmapFactory.decodeFile(location, options);

		Bitmap scaledBitmap = Bitmap.createScaledBitmap(sampledSrcBitmap, width, height, true);
		sampledSrcBitmap = null;

		return store(context, scaledBitmap, fileName, huntId);
	}

	/**
	 * The environment of the activity that started this
	 * <code>HuntManager</code>.
	 */
	private Context context;

	/**
	 * Post additions to hunts.
	 */
	private Handler handler = new Handler();

	private ArrayList<Hunt> hunts = new ArrayList<Hunt>();

	/**
	 * True if we are in the process of loading.
	 */
	private boolean loading;

	/**
	 * Observers interested in changes to the current search results.
	 */
	private ArrayList<WeakReference<DataSetObserver>> observers = new ArrayList<WeakReference<DataSetObserver>>();

	/**
	 * The database where downloaded data is stored and offline data is
	 * retrieved from.
	 */
	private SQLiteDatabase sqliteDatabase;

	/**
	 * Creates a <code>HuntManager</code> given the application's current
	 * environment.
	 * 
	 * @param context
	 *            the current environment of the activity that created this
	 *            <code>HuntManager</code>
	 */
	private HuntManager(Context context) {
		this.context = context;
	}

	/**
	 * Adds a hunt to the list of hunts.
	 * 
	 * @param hunt
	 *            the hunt to be added.
	 */
	protected void add(Hunt hunt) {
		hunts.add(hunt);
		notifyObservers();
	}

	/**
	 * Adds a hint to the given hunt.
	 * 
	 * @param huntId
	 *            the index of the hunt the hint is to be added to
	 * @param hint
	 *            the hint to be added
	 */
	protected void add(int huntId, Hint hint) {
		for (Hunt hunt : hunts)
			if (hunt.getPk() == huntId) {
				hunt.add(hint);
				notifyObservers();
				return;
			}
	}

	/**
	 * Adds an observer to be notified when the set of items held by this
	 * {@link HuntManager} changes.
	 */
	public void addObserver(DataSetObserver observer) {
		WeakReference<DataSetObserver> obs = new WeakReference<DataSetObserver>(observer);
		observers.add(obs);
	}

	/**
	 * Clear all downloaded content.
	 */
	public void clear() {
		hunts.clear();
		notifyObservers();
	}

	/**
	 * @return The {@link Hunt} with the given id.
	 */
	public Hunt find(int huntId) {
		for (Hunt hunt : hunts)
			if (hunt.getPk() == huntId)
				return hunt;
		return null;
	}

	/**
	 * @return The {@link Hunt} at this position in the list of hunts.
	 */
	public Hunt get(int position) {
		return hunts.get(position);
	}

	/**
	 * @return The current list of hunts.
	 */
	public List<Hunt> getHunts() {
		return hunts;
	}

	/**
	 * @return <code>True</code> if we are still loading content.
	 */
	public boolean isLoading() {
		return loading;
	}

	/**
	 * Loads a hunt's hints from the database.
	 * 
	 * @param huntId
	 *            the index of the hunt to be loaded
	 */
	private void loadDatabaseHints(int huntId) {
		Log.w(TAG, "Loading hints from Database.");
		loading = true;
		new DatabaseThread(huntId).start();
	}

	/**
	 * Loads the list of hunts from the database.
	 * 
	 * @param settings
	 *            the preferences file that stores the user's current progress
	 */
	private void loadDatabaseHunts(SharedPreferences settings) {
		Log.w(TAG, "Loading hunts from Database.");
		loading = true;
		new DatabaseThread(settings).start();
	}

	/**
	 * Load hints from the network.
	 * 
	 * @param huntId
	 *            the hunt to load hints from
	 */
	public void loadHints(int huntId) {
		loading = true;
		new NetworkThread(huntId).start();
	}

	/**
	 * Loads hunts from the network.
	 * 
	 * @param settings
	 *            The settings stored by the main activity.
	 */
	public void loadHunts(SharedPreferences settings) {
		loading = true;
		new NetworkThread(settings).start();
	}

	/**
	 * Called when something changes in our data set. Cleans up any weak
	 * references that are no longer valid along the way.
	 */
	private void notifyObservers() {
		final ArrayList<WeakReference<DataSetObserver>> observers = this.observers;

		// iterators are used to remove objects cleanly
		for (ListIterator<WeakReference<DataSetObserver>> li = observers.listIterator(); li
				.hasNext();) {
			WeakReference<DataSetObserver> weak = li.next();
			DataSetObserver obs = weak.get();
			if (obs != null) {
				obs.onChanged();
			} else {
				li.remove();
			}
		}
	}

	/**
	 * @return The number of items downloaded so far.
	 */
	public int size() {
		return hunts.size();
	}

	/**
	 * Stores the given hunt in the database.
	 * 
	 * @param hunt
	 *            the {@link Hunt} to be stored
	 */
	protected void store(Hunt hunt) {
		// make sure there is a databse to store into
		if (sqliteDatabase == null) {
			// create database opener to get this application's database
			final HuntOpenHelper huntOpenHelper = new HuntOpenHelper(context);
			// grab the database
			sqliteDatabase = huntOpenHelper.getWritableDatabase();
		}

		// attempts to grab the row in the database with the same name
		Cursor cursor = sqliteDatabase.query(HuntOpenHelper.HUNT_TABLE_NAME, null, Hunt.COLUMN_NAME
				+ "=\"" + hunt.getName() + "\"", null, null, null, "id ASC");

		// prepare values to be stored
		ContentValues contentValues = new ContentValues();
		contentValues.put(Hunt.COLUMN_NAME, hunt.getName());
		contentValues.put(Hunt.COLUMN_CREATOR, hunt.getCreator());
		contentValues.put(Hunt.COLUMN_PUBDATE, hunt.getPubDate());

		// update or insert values as appropriate
		if (cursor.getCount() <= 0) {
			sqliteDatabase.insert(HuntOpenHelper.HUNT_TABLE_NAME, null, contentValues);
		} else {
			sqliteDatabase.update(HuntOpenHelper.HUNT_TABLE_NAME, contentValues, null, null);
		}

		cursor.close();
	}

	/**
	 * Stores the given <code>Hint</code> in the database.
	 * 
	 * @param huntId
	 *            the index of the {@link Hunt} that needs hints attached to it
	 * @param hint
	 *            the {@link Hint} to be added
	 */
	protected void store(int huntId, Hint hint) {
		if (sqliteDatabase == null) { // make sure there is a database to store
										// into
			// create database opener to get this application's database
			final HuntOpenHelper huntOpenHelper = new HuntOpenHelper(context);
			// grab the database
			sqliteDatabase = huntOpenHelper.getWritableDatabase();
		}

		// attempts to grab the row in the database with the same name
		Cursor cursor = sqliteDatabase.query(HuntOpenHelper.HINT_TABLE_NAME, null, Hint.COLUMN_HINT
				+ "=\"" + hint.getHint() + "\"", null, null, null, "id ASC");

		// prepares values to be stored in the database
		ContentValues contentValues = new ContentValues();
		contentValues.put(Hint.COLUMN_HINT, hint.getHint());
		contentValues.put(Hint.COLUMN_HUNT, huntId + 1); // index 1
		contentValues.put(Hint.COLUMN_LONGITUDE, hint.getLocation().getLongitudeE6());
		contentValues.put(Hint.COLUMN_LATITUDE, hint.getLocation().getLatitudeE6());
		contentValues.put(Hint.COLUMN_TYPE, hint.getType());
		contentValues.put(Hint.COLUMN_DATA, hint.getData());

		// insert or add as appropriate
		if (cursor.getCount() <= 0) {
			sqliteDatabase.insert(HuntOpenHelper.HINT_TABLE_NAME, null, contentValues);
		} else {
			sqliteDatabase.update(HuntOpenHelper.HINT_TABLE_NAME, contentValues, null, null);
		}

		cursor.close();
	}
}

