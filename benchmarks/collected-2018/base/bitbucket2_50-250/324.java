// https://searchcode.com/api/result/46075856/

package com.obs.vegquest.activity;

import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

public class ReviewTab extends TabActivity {

	String userId; 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.reviewtab);

		TabHost tabHost = getTabHost();

		// Tab for Photos
		TabSpec recentRevies = tabHost.newTabSpec("Recent Reviews");

		// setting Title and Icon for the Tab
		recentRevies.setIndicator("Recent Reviews");
		Intent photosIntent = new Intent(this, ReviewList.class);
		recentRevies.setContent(photosIntent);

		// Tab for Songs
		TabSpec myReviews = tabHost.newTabSpec("My Reviews");
		myReviews.setIndicator("My Reviews");
		Intent songsIntent = new Intent(this, MyReviews.class);
		myReviews.setContent(songsIntent); 

		tabHost.addTab(recentRevies); // AddingrecentRevies tab

		SharedPreferences settingsUserObject = getSharedPreferences("userObject", 0);
		if(settingsUserObject!=null) {
			if(settingsUserObject.getString("userId", null)!=null || settingsUserObject.getString("userName", null)!=null && settingsUserObject.getBoolean("isLoggedin", Boolean.FALSE) ) {
				tabHost.addTab(myReviews); // Adding myReviews tab
			} 
		}	

		/*for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
			     tabHost.getTabWidget().getChildAt(i).setPadding(10,15,15,15); 
			 } */

		int tabCount = tabHost.getTabWidget().getTabCount();
		for (int i = 0; i < tabCount; i++) {
			final View view = tabHost.getTabWidget().getChildTabViewAt(i);
			if ( view != null ) {
				// reduce height of the tab
				view.getLayoutParams().height *= 0.66;

				//  get title text view
				final View textView = view.findViewById(android.R.id.title);
				if ( textView instanceof TextView ) {
					// just in case check the type

					// center text
					((TextView) textView).setGravity(Gravity.CENTER);
					// wrap text
					((TextView) textView).setSingleLine(false);

					// explicitly set layout parameters
					textView.getLayoutParams().height = ViewGroup.LayoutParams.FILL_PARENT;
					textView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
				}
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.reviewtab, menu);
		return true;
	}

}

