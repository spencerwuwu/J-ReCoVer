// https://searchcode.com/api/result/97434046/

package org.android.activities;

import java.util.Calendar;
import java.util.HashMap;

import org.satsang.live.config.ConfigurationLive;
import org.satsang.live.config.Constants;
import org.satsang.util.ConfigUtil;
import org.satsang.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SatsangStartActivity extends Activity {
	static private final Logger Log = LoggerFactory.getLogger(SatsangStartActivity.class);
	
	private int timeDiffInSeconds;
	Calendar calendar;
	private ProgressBar pBar;
	private int leadTime = 0;
	private String timeFormat = "%02d:%02d:%02d";
	private String satsangStartTime;

	Typeface face;
	TextView TxtWelcome, TxtTimeRemaining;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_satsang_start);
		((MyApplication) getApplication()).setLocaleConfiguration();
		Log.trace("OnCrete");
		if(Constants.MODE_IS_LIVE) {
			Settings.System.putInt(this.getContentResolver(),Global.AUTO_TIME, 1);
			Settings.System.putInt(this.getContentResolver(),Global.AUTO_TIME_ZONE, 1);	
		}
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			try {
				timeDiffInSeconds = (int) extras.getInt("timeDiffInSeconds");
				Log.debug("Received time diff:" + timeDiffInSeconds);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		satsangStartTime = DateUtil.getFormattedTime(ConfigurationLive.getValue("satsang.default.play.time"));
		if (timeDiffInSeconds < 0) {
			setResult(RESULT_OK);
			finish();
		} else {
			Log.info("display UI...");
			timeDiffInSeconds--; // considering lag in processing reduce by one second

			leadTime = getProgressBarLeadTime() * 60;
			TxtWelcome = (TextView) findViewById(R.id.txt_welcome);
			// TxtWelcome.setText("    ");
			TxtWelcome.setText(getString(R.string.welcome));

			TxtTimeRemaining = (TextView) findViewById(R.id.txt_time_remaining);
			// TxtTimeRemaining.setText(" ");
			TxtTimeRemaining.setText(getString(R.string.Santasang));

			Typeface face = Typeface.createFromAsset(getAssets(), "fonts/DroidHindi.ttf");

			// show progress bar
			
			pBar = (ProgressBar) findViewById(R.id.time_progress);
			pBar.setMax(leadTime);
			pBar.setProgress(0);
			pBar.setEnabled(false);
			pBar.setVisibility(View.INVISIBLE);
			Log.debug("Set progressbar and max values for time diff: " + timeDiffInSeconds);
			mHandler.postDelayed(mUpdateTime, 1000);
		}

	}

	final private Handler mHandler = new Handler();
	Runnable mUpdateTime = new Runnable() {

		public void run() {
			updateTimeView();
		}
	};

	public void updateTimeView() {
		validateEndtime();
		timeDiffInSeconds--;
		long hours = timeDiffInSeconds / 3600;
		if (hours < 0)
			hours = -hours;
		long mins = timeDiffInSeconds / 60 % 60;
		if (mins < 0)
			mins = -mins;
		long seconds = timeDiffInSeconds / 1 % 60;
		if (seconds < 0)
			seconds = -seconds;
		String time = String.format(timeFormat, hours, mins, seconds);
		if (timeDiffInSeconds > leadTime) {
			TxtTimeRemaining.setText(getString(R.string.Santasang) + satsangStartTime + " " + getString(R.string.strtat)
					+ getString(R.string.remainingtime) + time);

			mHandler.postDelayed(mUpdateTime, 1000);
		} else if (timeDiffInSeconds <= leadTime) {
			if (timeDiffInSeconds <= 0) {
				setResult(RESULT_OK);
				mHandler.removeCallbacks(mUpdateTime);
				mUpdateTime=null;
				finish();
			}
			TxtTimeRemaining.setText(getString(R.string.startsantasang) + time);
			pBar.setVisibility(View.VISIBLE);
			pBar.setProgress(pBar.getProgress() + 1);
			mHandler.postDelayed(mUpdateTime, 1000);

		}

	}

	public void showProgress(String string) {
		pBar.setVisibility(View.VISIBLE);
	}

	public void stopProgress() {
		pBar.setVisibility(View.INVISIBLE);
		pBar.postInvalidate();
	}

	private void validateEndtime() {
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR);
		int min = cal.get(Calendar.MINUTE);
//		System.out.println("EndTime: " + hour + ":" + min + " & satsangStartTime: " + satsangStartTime);
		if (satsangStartTime.equalsIgnoreCase("0" + hour + ":" + min + "0")) {
			System.out.println("Start time equals current time>>>");
			setResult(RESULT_OK);
			mHandler.removeCallbacks(mUpdateTime);
			mUpdateTime=null;
			finish();
		}
	}

	
	/* Below method return the time in HH:MM:SS format */
	/*
	 * private String formatTime(int remaining){ long hours = remaining / 3600;
	 * if (hours < 0) hours = -hours; long mins = remaining / 60 % 60; if (mins
	 * < 0) mins = -mins; long seconds = remaining / 1 % 60; if(seconds < 0)
	 * seconds = -seconds; String time = String.format(timeFormat, hours, mins,
	 * seconds); return time; }
	 */

	public static int getProgressBarLeadTime() {

		int leadTime = 0;
		try {
			leadTime = Integer.valueOf(ConfigurationLive.getValue("local.splash.progressbar.leadtime"));
			return leadTime;
		} catch (Exception e) {
			Log.error("Exception:" + e.getMessage(), e);
		}
		Log.debug("progress bar lead time:" + leadTime * 60);
		return leadTime;
	}

	@Override
	public void onBackPressed() {
		return;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.trace("OnResume");
		((MyApplication) getApplication()).setLocaleConfiguration();
		// Settings.System.putInt(this.getContentResolver(),
		// Settings.System.AUTO_TIME, 1);// Resetting time to AutoTime
		// TODO: proble with calculation if manually date time is changed
		// calculation happens based on modified date time.
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		timeDiffInSeconds = ConfigUtil.getScheduleTimeFromConfiguration() * 60;
	}

}

