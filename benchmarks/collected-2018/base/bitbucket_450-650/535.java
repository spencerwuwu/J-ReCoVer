// https://searchcode.com/api/result/35507488/

package com.kiwipedia.nzfauna;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class NZFaunaActivity extends Activity {

	//private boolean sort = false; 
	public static HashMap<Integer, String[]> list; 
	public static HashMap<String, Integer> speciesHashMap; 
	public static ArrayList<Integer> idNameMap, idSpeciesMap; 
	public static ProgressDialog pDialog; 

	private TableLayout tl; 
	private Cursor mCursor; 
	private DbAdapter mDbHelper; 
	private final static String nameOrder = "name", speciesOrder = "species", 
			sourceCol = "source", video1Col = "video1", video2Col = "video2", 
			video3Col = "video3", idCol = "_id";
	private String key; 
	private AssetManager assets; 
	private OnClickListener clickL; 
	private BitmapFactory.Options o = new BitmapFactory.Options(); 
	private Bitmap image; 
	private Handler progressH, tableH; 
	private Context mContext; 
	private Activity mActivity; 

	// Shake features
	private static final int FORCE_THRESHOLD = 350;
	private static final int TIME_THRESHOLD = 100;
	private static final int SHAKE_TIMEOUT = 500;
	private static final int SHAKE_DURATION = 1000;
	private static final int SHAKE_COUNT = 3;

	private SensorManager mSensorMgr;
	private float mLastX=-1.0f, mLastY=-1.0f, mLastZ=-1.0f;
	private long mLastTime;
	private int mShakeCount = 0;
	private long mLastShake;
	private long mLastForce;
	private final SensorEventListener mSensorListener = new SensorEventListener() {

		public void onSensorChanged(SensorEvent se) {
			long now = System.currentTimeMillis();

			if ((now - mLastForce) > SHAKE_TIMEOUT) {
				mShakeCount = 0;
			}

			if ((now - mLastTime) > TIME_THRESHOLD) {
				long diff = now - mLastTime;
				float speed = Math.abs(se.values[SensorManager.DATA_X] + 
						se.values[SensorManager.DATA_Y] + 
						se.values[SensorManager.DATA_Z] - 
						mLastX - mLastY - mLastZ) / diff * 10000;
				if (speed > FORCE_THRESHOLD) {
					if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
						Random random = new Random(); 
						String id = "" + random.nextInt(list.size()); 
						while(id.length() < 3) {
							id = "0" + id;
						}
						callDisplay(id); 
						mLastShake = now;
						mShakeCount = 0;
					}
					mLastForce = now;
				}
				mLastTime = now;
				mLastX = se.values[SensorManager.DATA_X];
				mLastY = se.values[SensorManager.DATA_Y];
				mLastZ = se.values[SensorManager.DATA_Z];
			}
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	SharedPreferences nzfauna; 

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.main); 

		mDbHelper = new DbAdapter(this); 
		assets = getAssets(); 
		nzfauna = getBaseContext().getSharedPreferences("nzfauna", MODE_PRIVATE); 
		list = new HashMap<Integer, String[]>(); 
		idSpeciesMap = new ArrayList<Integer>(); 
		idNameMap = new ArrayList<Integer>(); 
		speciesHashMap = new HashMap<String, Integer>(); 
		progressH = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				pDialog.dismiss();
			}
		}; 
		tableH = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				populateTable(key); 
			}
		}; 
		mContext = this; 
		mActivity = this; 

		// prepare and read database
		mDbHelper.open(); 
		retrieve();

		// set to reduce image memory usage
		o.inSampleSize = 1; 

		// prepare sensor for shake events
		mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE); 

		Splash.pHandler.handleMessage(Splash.pHandler.obtainMessage()); 
	}

	@Override
	protected void onStart() {
		super.onStart();
		pDialog = ProgressDialog.show(mContext, "", "Loading...");

		clickL = new OnClickListener() {

			public void onClick(View v) {
				pDialog = ProgressDialog.show(mContext, "", "Loading..."); 
				if(v.getId() == R.id.alphaBtn) {
					key = nameOrder; 
					populateDismiss(); 
				} else if (v.getId() == R.id.speciesBtn) {
					key = speciesOrder; 
					populateDismiss(); 
				} else { 
					String id = "" + v.getId(); 
					while(id.length() < 3) {
						id = "0" + id;
					}
					callDisplay(id); 
				}
			}
		};

		// setting up actions for button press
		Button alphaBtn = (Button) findViewById(R.id.alphaBtn); 
		alphaBtn.setOnClickListener(clickL); 
		Button speciesBtn = (Button) findViewById(R.id.speciesBtn); 
		speciesBtn.setOnClickListener(clickL); 

		// set up table layout and other details for listing
		tl = (TableLayout) findViewById(R.id.table);
		tl.setScrollBarStyle(TableLayout.SCROLLBARS_OUTSIDE_OVERLAY); 
		tl.setScrollbarFadingEnabled(true); 
	}

	private void retrieve() {
		final HashMap<String, Integer> order = new HashMap<String, Integer>(); 
		// sorting out by name order
		try {
			mCursor = mDbHelper.fetchAll(0, nameOrder); 
		} catch (Exception e) {

		}
		if(mCursor == null) {
			Log.e("NZFaunaActivity.java", "mCursor = null"); 
		} else {
			startManagingCursor(mCursor); 
			for(int i = 0; i < mCursor.getCount(); i++) {
				mCursor.moveToPosition(i); 
				String[] item = new String[7] ; 
				item[0] = mCursor.getString(mCursor.getColumnIndex(idCol)); 
				item[1] = mCursor.getString(mCursor.getColumnIndex(nameOrder)); 
				item[2] = mCursor.getString(mCursor.getColumnIndex(sourceCol)); 
				item[3] = mCursor.getString(mCursor.getColumnIndex(speciesOrder)); 
				item[4] = mCursor.getString(mCursor.getColumnIndex(video1Col)); 
				item[5] = mCursor.getString(mCursor.getColumnIndex(video2Col)); 
				item[6] = mCursor.getString(mCursor.getColumnIndex(video3Col)); 
				idNameMap.add(i); 
				order.put(item[0], i); 
				list.put(i, item); 
			}
			mCursor.moveToFirst(); 

		}
		// sorting out by species order
		new Thread(new Runnable() {

			public void run() {
				try {
					mCursor = mDbHelper.fetchAll(0, speciesOrder); 
				} catch (Exception e) {

				}
				if(mCursor == null) {
					Log.e("NZFaunaActivity.java", "mCursor = null"); 
				} else {
					for(int i = 0; i < mCursor.getCount(); i++) {
						mCursor.moveToPosition(i); 
						String id = mCursor.getString(mCursor.getColumnIndex(idCol)); 
						int row = order.get(id); 
						speciesHashMap.put(id, i); 
						idSpeciesMap.add(row); 
					}
					mCursor.moveToFirst(); 

				}
			}
		}).start(); 
	}

	private void populateTable(String key) {
		String species = ""; 
		ArrayList<Integer> order = null; 
		if(key.equalsIgnoreCase(nameOrder)) {
			this.key = nameOrder; 
			order = idNameMap; 
		} else if(key.equalsIgnoreCase(speciesOrder)) {
			this.key = speciesOrder; 
			order = idSpeciesMap; 
		}
		tl.removeAllViews(); 

		// get width of table layout
		TableLayout tl = (TableLayout) findViewById(R.id.table); 
		int halfWidth = (tl.getWidth() / 2); 

		// iterate through values to produce table
		for(int i = 0, sid = 999; i < order.size(); ) {
			// create table inputs with the details from the data retrieve
			TableRow tr = new TableRow(getBaseContext()); 
			tl.setStretchAllColumns(true); 
			tl.addView(tr, new TableLayout.LayoutParams(
					TableLayout.LayoutParams.MATCH_PARENT, 
					TableLayout.LayoutParams.WRAP_CONTENT)); 

			boolean speciesChange = false; 
			while(tr.getChildCount() < 2 && i < list.size()) {
				String[] item = list.get(order.get(i)); 
				RelativeLayout rl = new RelativeLayout(this); 
				if(key.equalsIgnoreCase(speciesOrder) && 
						!species.equalsIgnoreCase(item[3])) {
					speciesChange = true; 
					species = item[3]; 
					// setup a new button for the species category
					ImageView iv = new ImageView(this); 
					iv.setAdjustViewBounds(true); 
					iv.setClickable(true); 
					iv.setBackgroundResource(R.drawable.dropshadow); 
					iv.setPadding(15, 15, 15, 15); 
					try {
						InputStream is = assets.open("species/" + item[3] + ".png"); 
						image = BitmapFactory.decodeStream(is, null, o); 
						iv.setImageBitmap(image); 
						is.close(); 
					} catch (IOException e) {

					} 
					iv.setScaleType(ScaleType.FIT_CENTER); 
					iv.setId(sid); 
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
							RelativeLayout.LayoutParams.MATCH_PARENT, 
							halfWidth); 
					rl.addView(iv, lp); 
					TextView tv = new TextView(this); 
					tv.setText(item[3]); 
					tv.setTextColor(Color.GRAY); 
					lp = new RelativeLayout.LayoutParams(
							RelativeLayout.LayoutParams.MATCH_PARENT, 
							RelativeLayout.LayoutParams.MATCH_PARENT); 
					lp.setMargins(0, 0, 0, 5); 
					lp.addRule(RelativeLayout.BELOW, iv.getId()); 
					rl.addView(tv, lp); 
					tv.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM); 
					sid--; 
				} else {
					setupImageButton(rl, item, key, halfWidth); 
				}
				tr.addView(rl, new TableRow.LayoutParams(
						halfWidth, 
						TableRow.LayoutParams.MATCH_PARENT)); 
				if(!speciesChange){
					i++; 
				}
				speciesChange = false; 
			}
		}
	}

	private void setupImageButton(RelativeLayout rl, String[] item, String key, 
			int height) {
		ImageView iv = new ImageView(this); 
		iv.setAdjustViewBounds(true); 
		iv.setClickable(true); 
		iv.setBackgroundResource(R.drawable.dropshadow); 
		iv.setPadding(15, 15, 15, 15); 
		try {
			InputStream is = assets.open("animals/" + item[0] + "-100.png"); 
			image = BitmapFactory.decodeStream(is, null, o); 
			iv.setImageBitmap(image); 
			is.close(); 
		} catch (IOException e) {

		} 
		iv.setScaleType(ScaleType.FIT_CENTER); 
		iv.setId(Integer.parseInt(item[0])); 
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT, 
				height); 
		rl.addView(iv, lp); 
		TextView tv = new TextView(this); 
		tv.setText(item[1]); 
		tv.setTextColor(Color.BLACK); 
		lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT, 
				RelativeLayout.LayoutParams.MATCH_PARENT); 
		lp.setMargins(0, 0, 0, 5); 
		lp.addRule(RelativeLayout.BELOW, iv.getId()); 
		rl.addView(tv, lp); 
		tv.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM); 
		iv.setOnClickListener(clickL); 
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// clear the existing menu if there is one and inflate it 
		// with a new menu layout
		menu.clear(); 
		MenuInflater inflater = getMenuInflater(); 
		/*if(sort) {
			inflater.inflate(R.menu.sortmenu, menu);
			sort = false; 
		} else {*/
		inflater.inflate(R.menu.option, menu); 
		//}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.quit:
			mCursor.close(); 
			mDbHelper.close(); 
			finish();
			break; 
			/*case R.id.alphabetical: 
			// sort the display in alphabetical order
			sort(nameOrder); 
			break; 
		case R.id.species: 
			// sort the display in species order
			sort(speciesOrder); 
			break; */
		}
		return true; 
	}

	private void callDisplay(String id) {
		// call new intent and update intent with id and details
		Intent intent = new Intent(getBaseContext(), Display.class); 
		for(int i = Integer.parseInt(id); i >= 0; i--) {
			// check to ensure no index out of bound
			if(i > list.size()) {
				i = list.size() - 1;
			}
			String[] item = list.get(i); 
			if(item[0].equalsIgnoreCase(id)) {
				nzfauna.edit().putString("id", id).commit(); 
				nzfauna.edit().putString("name", item[1]).commit(); 
				nzfauna.edit().putString("source", item[2]).commit(); 
				nzfauna.edit().putString("video1", item[4]).commit(); 
				nzfauna.edit().putString("video2", item[5]).commit(); 
				nzfauna.edit().putString("video3", item[6]).commit(); 
				nzfauna.edit().putString("key", key).commit(); 
				int minus = -1, plus = -1; 
				if(key.equalsIgnoreCase(nameOrder)) {
					try{
						minus = idNameMap.get(i - 1); 
					} catch(Exception e) {
						minus = -1; 
					}
					try{
						plus = idNameMap.get(i + 1); 
					} catch(Exception e) {
						plus = -1; 
					}
				} else if(key.equalsIgnoreCase(speciesOrder)) {
					int row = speciesHashMap.get(id); 
					try{
						minus = idSpeciesMap.get(row - 1); 
					} catch(Exception e) {
						minus = -1; 
					}
					try{
						plus = idSpeciesMap.get(row + 1); 
					} catch(Exception e) {
						plus = -1; 
					}
				}
				if(minus != -1) {
					nzfauna.edit().putInt("previous", minus).commit(); 
				} else {
					nzfauna.edit().putInt("previous", 0).commit(); 
				}
				if(plus != -1) {
					nzfauna.edit().putInt("next", plus).commit(); 
				} else if(key.equalsIgnoreCase(nameOrder)){ 
					nzfauna.edit().putInt("next", 
							idNameMap.get((idNameMap.size() - 1))).commit(); 
				} else if(key.equalsIgnoreCase(speciesOrder)){ 
					nzfauna.edit().putInt("next", 
							idSpeciesMap.get((idSpeciesMap.size() - 1))).commit(); 
				}
				break; 
			}
		}
		Handler h = new Handler(); 
		h.post(new Runnable() {

			public void run() {
				progressH.handleMessage(progressH.obtainMessage()); 
			}
		}); 
		startActivity(intent); 
	}

	@Override
	protected void onPause() {
		mSensorMgr.unregisterListener(mSensorListener);
		super.onPause();
	}

	@Override
	protected void onResume() {
		mSensorMgr.registerListener(mSensorListener, 
				mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_UI); 
		super.onResume();

		key = nameOrder; 
		populateDismiss(); 
	}

	private void populateDismiss() {

		new Thread(new Runnable() {

			public void run() {
				mActivity.runOnUiThread(new Runnable() {

					public void run() {
						populateTable(key); 
						pDialog.dismiss();
					}
				}); 
			}
		}).start();
	}
}
