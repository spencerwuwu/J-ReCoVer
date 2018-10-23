// https://searchcode.com/api/result/8523116/

/*
 * File: ImageViewActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */
package org.hfoss.posit;

import org.hfoss.posit.provider.PositDbHelper;
import org.hfoss.posit.utilities.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;

public class ImageViewActivity extends Activity {
	private int mPosition;
	private Find mFind = null;
	public static final int CONFIRM_DELETE_DIALOG = 0;
	private Cursor mCursor;
	private static final String TAG = "ImageViewActivity";

	private ImageView mIV;
	private Bitmap mBm;

	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private Intent intent;

	/**
	 * A new gesture detector and listener to listen for the swipe motions
	 */
	GestureDetector gestureDetector = new GestureDetector(new MyGestureDetector());
	OnTouchListener gestureListener = new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if (gestureDetector.onTouchEvent(event)) {
				return true;
			}
			return false;
		}
	};

	/**
	 * The onCreate().
	 */
	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 intent = getIntent();
	 }

	 
	 /* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		 mBm = (Bitmap)intent.getExtras().get("bitmap");
		 if (mBm != null) {
			 setContentView(R.layout.image_view);
			 mIV = (ImageView)findViewById(R.id.photo_big);
			 mIV.setImageBitmap(mBm);
		 } else {
			 String action = intent.getAction();
			 setResult(RESULT_OK,intent);
			 if(action.equals(this.getString(R.string.delete_find_image))) {
				 showDialog(CONFIRM_DELETE_DIALOG);
			 }

			 //The position, and find are passed as extras
			 mPosition = intent.getIntExtra("position",-1);
			 mFind = new Find(this, intent.getLongExtra("findId",-1));
			 mCursor = mFind.getImages();
			 mCursor.moveToPosition(mPosition);

			 /*
			  * This gets the Uri of the image in position n in some find.
			  * In the photo table on the DB, each photo has both a unique
			  * id and the id of the find with which it's associated.  In the 
			  * set of photos for a find, the one with the lowest unique id
			  * is position 0, the next lowest is position 1, etc.  This is 
			  * also the order that the photos show up in our scrolly gallery 
			  * thing on the Find view.
			  */
			 Uri data = mFind.getImageUriByPosition(mFind.getId(), mPosition);

			 setContentView(R.layout.image_view);
			 mIV = (ImageView)findViewById(R.id.photo_big);
			 mIV.setImageURI(data);

			 /*
			  * If the position is greater than 0, then it should have a button
			  * to scroll to the left.
			  */
			 if (mPosition > 0) {
				 final Button leftButton = (Button)findViewById(R.id.photo_left);
				 leftButton.setVisibility(0);
				 leftButton.setOnClickListener(new View.OnClickListener() {
					 public void onClick(View v) {

						 /*
						  * This section might be nice to use the intent filter Phil mentioned
						  * since a new activity is started each time the image is changed
						  */
						 Intent intent = new Intent(ImageViewActivity.this, ImageViewActivity.class);
						 intent.setAction(Intent.ACTION_VIEW);
						 //scroll to the left by subtracting 1 from the current position
						 intent.putExtra("position",mPosition-1);
						 intent.putExtra("findId", mFind.getId());
						 finish();
						 startActivity(intent);
					 }
				 });
			 }

			 /*
			  * Same thing as above, just with moving to the right
			  */
			 if(mPosition<mCursor.getCount()-1) {
				 final Button rightButton = (Button)findViewById(R.id.photo_right);
				 rightButton.setVisibility(0);
				 rightButton.setOnClickListener(new View.OnClickListener() {
					 public void onClick(View v) {	       
						 Intent intent = new Intent(ImageViewActivity.this,ImageViewActivity.class);
						 intent.setAction(Intent.ACTION_VIEW);
						 //scroll to the right by adding 1 to the current position
						 intent.putExtra("position",mPosition+1);
						 intent.putExtra("findId", mFind.getId());
						 finish();
						 startActivity(intent);        
					 }
				 });
			 }
		 }

	}



// Commented out to not let users delete photos from the phone
	
//	@Override
//	 public boolean onCreateOptionsMenu(Menu menu) {
//		 if(mBm==null) {
//			 MenuInflater inflater = getMenuInflater();
//			 inflater.inflate(R.menu.image_view_menu, menu);
//		 }
//		 return true;
//	 }
//
//	 /**
//	  * There is only one menu item, and it deletes the image
//	  */
//	 @Override
//	 public boolean onMenuItemSelected(int featureId, MenuItem item) {
//		 switch (item.getItemId()) {
//		 
//		 case R.id.delete_image_view_menu_item:
//			 showDialog(CONFIRM_DELETE_DIALOG);
//			 break;
//			 
//		 default: 
//			 return false;
//		 }
//		 return true;
//	 }

	 @Override
	 protected Dialog onCreateDialog(int id) {
		 final Intent intent = new Intent(ImageViewActivity.this, FindActivity.class);
		 intent.putExtra(PositDbHelper.FINDS_GUID, mFind.getId());
		 intent.setAction(Intent.ACTION_EDIT);
		 setResult(RESULT_OK,intent);
		 setResult(RESULT_OK);

		 switch(id) {
		 
		 case CONFIRM_DELETE_DIALOG:
			 return new AlertDialog.Builder(this)
			 .setIcon(R.drawable.alert_dialog_icon)
			 .setTitle(R.string.alert_dialog_delete_image)
			 .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				 public void onClick(DialogInterface dialog, int whichButton) {
					 // User clicked OK so do some stuff 
					 if (mFind.deleteImageByPosition(mPosition)) { // Assumes find was instantiated in onCreate        		
						 Utils.showToast(ImageViewActivity.this, R.string.deleted_from_database);	
						 finishActivity(ListFindsActivity.FIND_FROM_LIST);
						 finish();
						 //FindActivity fa = FindActivity.newInstance();
						 startActivityForResult(intent,FindActivity.STATE_EDIT);
					 } else {
						 Utils.showToast(ImageViewActivity.this, R.string.delete_failed);
					 }
				 }
			 }).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				 public void onClick(DialogInterface dialog, int whichButton) {
					 /* User clicked Cancel so do nothing */
				 }
			 }).create();
			 
		 default:
			 return null;
		 }
	 }

	 /**
	  * This is another method to reduce the number of activities
	  * on the stack, just like in FindActivity.
	  */
	 @Override
	 public boolean onKeyDown(int keyCode, KeyEvent event) {
		 if(keyCode==KeyEvent.KEYCODE_BACK && mBm==null) {
			 final Intent intent = new Intent(ImageViewActivity.this, FindActivity.class);
			 intent.putExtra(PositDbHelper.FINDS_ID, mFind.getId());
			 intent.setAction(Intent.ACTION_EDIT);
			 setResult(RESULT_OK,intent);
			 setResult(RESULT_OK);

			 startActivityForResult(intent,FindActivity.STATE_EDIT);
			 finish();
			 return true;
		 }
		 return super.onKeyDown(keyCode, event);
	 }

	 /*
	  * inner class to detect the fling gesture
	  */
	 class MyGestureDetector extends SimpleOnGestureListener {
		 /**
		  * @param e1 is the first motion event, I assume it's the point where you first touched
		  * @param e2 is the second motion event, I assume it's the point where you took your finger off
		  * @param velocityX is the speed at which you moved your finger from e1 to e2 in the x direction in pixels per second
		  * @param velocityY is the speed at which you moved your finger from e1 to e2 in the y direction in pixels per second
		  */
		 @Override
		 public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			 if (mCursor != null) {
				 try {
					 //since we fling horizontally in this case, ignore it if the y coordinates are too far apart
					 if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) 
						 return false;
					 // Right to left fling
					 if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
						 Log.d(TAG, "Right to left fling");
						 //Same thing as above with the buttons. We can probably
						 //move this into a new method
						 if(mPosition<mCursor.getCount()-1) {
							 Intent intent = new Intent(ImageViewActivity.this,ImageViewActivity.class);
							 intent.setAction(Intent.ACTION_VIEW);
							 intent.putExtra("position",mPosition+1);
							 intent.putExtra("findId", mFind.getId());
							 finish();
							 startActivity(intent);
						 }
					 } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
						 Log.d(TAG, "Left to right fling");
						 if(mPosition>0) {
							 Intent intent = new Intent(ImageViewActivity.this,ImageViewActivity.class);
							 intent.setAction(Intent.ACTION_VIEW);
							 intent.putExtra("position",mPosition-1);
							 intent.putExtra("findId", mFind.getId());
							 finishActivity(ListFindsActivity.FIND_FROM_LIST);
							 finish();
							 startActivity(intent);
						 }
					 }
				 }
				 catch (Exception e) {
					 Log.e(TAG, e.toString());
				 }
			 }
			 return false;
		 }
	 }
	 
	 @Override
	 public boolean onTouchEvent(MotionEvent event) {
		 if (gestureDetector.onTouchEvent(event)) {
			 return true;
		 } else {
			 return false;
		 }
	 }

	 @Override
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		 super.onActivityResult(requestCode, resultCode, data);
		 if (resultCode == RESULT_CANCELED)
			 return;
		 switch (requestCode) {
		 case ListFindsActivity.FIND_FROM_LIST:
			 //finish();
			 startActivity(data);
		 }
	 }

	/* (non-Javadoc)
	 * @see android.app.Activity#finish()
	 */
	@Override
	public void finish() {
		super.finish();
		if (mCursor != null) mCursor.close();
	} 
	 
	 
}
