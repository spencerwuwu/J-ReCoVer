// https://searchcode.com/api/result/126619780/

package com.advback.pinpoint;

import java.util.ArrayList;
import java.util.Date;

import com.advback.pinpoint.location.LocationDatabaseConstants;
import com.advback.pinpoint.location.LocationDbAdapter;
import com.advback.pinpoint.location.PhotoDatabaseConstants;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

public class DisplayLocation extends Activity {
	
	private LocationDbAdapter locationDbRow;
	private long locationId;
	
	
	public void onCreate(Bundle SavedInstanceState){
		super.onCreate(SavedInstanceState);
		setContentView(R.layout.display_location);
		
		//grab the location ID from the intent
		Intent sender = getIntent();
		locationId = sender.getLongExtra("location_id", 0);
		
		locationDbRow = new LocationDbAdapter(this);
		locationDbRow.openLocations();
		populateDisplayFields();
		locationDbRow.closeLocations();
		
		Gallery gallery = (Gallery)findViewById(R.id.location_gallery);
		gallery.setAdapter(new ImageAdapter(this, locationId));
	}
	
	private void populateDisplayFields(){
		//Get the current location
		Cursor location = locationDbRow.fetchLocationById(String.valueOf(locationId));
		
		//Should only be one result, but move to the first just in case there are duplicates
		location.moveToFirst();
		
		//Declare column IDs for database access
		int locationTitle = location.getColumnIndex(LocationDatabaseConstants.TITLE);
		int locationLatitude = location.getColumnIndex(LocationDatabaseConstants.LATITUDE);
		int locationLongitude = location.getColumnIndex(LocationDatabaseConstants.LONGITUDE);
		int locationCreated = location.getColumnIndex(LocationDatabaseConstants.CREATED_ON);
		int locationAccuracy = location.getColumnIndex(LocationDatabaseConstants.ACCURACY);
		int locationComment = location.getColumnIndex(LocationDatabaseConstants.COMMENT);
		
		//Set the location title
		TextView title = (TextView) findViewById(R.id.loc_name);
		title.setText(location.getString(locationTitle));
		
		//Set the location latitude
		TextView latitude = (TextView) findViewById(R.id.loc_lat);
		latitude.setText(location.getString(locationLatitude));
		
		//Set the location longitude
		TextView longitude = (TextView) findViewById(R.id.loc_lng);
		longitude.setText(location.getString(locationLongitude));
		
		//Set the location created on string, after converting it to a standard date
		TextView created = (TextView) findViewById(R.id.loc_time);
		Date timestamp = new Date((long)Long.valueOf(location.getString(locationCreated)) * 1000);
		created.setText(timestamp.toString());
		
		//Set the location accuracy
		TextView accuracy = (TextView) findViewById(R.id.loc_accuracy);
		accuracy.setText("Within " + location.getString(locationAccuracy));
		
		//Set the location comment
		TextView comment = (TextView) findViewById(R.id.loc_comment);
		comment.setText(location.getString(locationComment));
	}
	
	public class ImageAdapter extends BaseAdapter {
		private final  String PHOTO_DIR = 
				Environment.getExternalStorageDirectory() + "/pinpoint/pictures/";
		
	    int mGalleryItemBackground;
	    private Context mContext;
	    private long locationRowId;
	    private LocationDbAdapter locationDb;

	    private ArrayList<Bitmap> mImageIds;

	    public ImageAdapter(Context c, long locationId) {
	        mContext = c;
	        
	        this.locationRowId = locationId;
	        
	        mImageIds = new ArrayList<Bitmap>();
	        
	        locationDb = new LocationDbAdapter(c);
			locationDb.openLocations();
	        
	        populateLocationImages();
	        
	        locationDb.closeLocations();
	        
	        TypedArray attr = mContext.obtainStyledAttributes(R.styleable.LocationGallery);
	        mGalleryItemBackground = attr.getResourceId(
	                R.styleable.LocationGallery_android_galleryItemBackground, 0);
	        attr.recycle();
	    }

	    public int getCount() {
	        return mImageIds.size();
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView imageView = new ImageView(mContext);

	        imageView.setImageBitmap(mImageIds.get(position));
	        imageView.setLayoutParams(new Gallery.LayoutParams(150, 100));
	        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
	        imageView.setBackgroundResource(mGalleryItemBackground);

	        return imageView;
	    }
	    
	    private void populateLocationImages(){
	    	
	    	//We first need to get the actual Location Id (not the row ID, 
	    	//which we have).
	    	Cursor locationsCursor = locationDb.fetchLocationById(locationRowId + "");
	    	locationsCursor.moveToPosition(0);
	    	
	    	//Grab the current rows Location ID
			int locationIndex = locationsCursor.getColumnIndex(LocationDatabaseConstants.LOC_ID);
			String locationId = locationsCursor.getString(locationIndex);
	    	
	    	//OPen a connection to the photos database
			LocationDbAdapter photosAdapter = new LocationDbAdapter(mContext);
			photosAdapter.openPhotos();
			
			//Get all the photos (if any) for the current location
			Cursor locationsPhotos = photosAdapter.fetchLocationPhotosById(locationId);
			
			int photoIndex = locationsPhotos.getColumnIndex(PhotoDatabaseConstants.PHOTO_PATH);
			
			//Grab just the first photo
			locationsPhotos.moveToFirst();
			Log.d("Display location", String.valueOf(locationsPhotos.getColumnCount()));
			
			String firstPhoto = locationsPhotos.getString(photoIndex);
			
			if (!firstPhoto.equals("none")) {
				
				String photoPath = PHOTO_DIR + firstPhoto;
				
				//Set a large sample size to reduce memory useage when loading lots
				//of images
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 32;
				
				//Create bitmap from the path, and then scale it down to a smaller
				//size for display reasons
				Bitmap photoBitmap = BitmapFactory.decodeFile(photoPath, options);
				mImageIds.add(photoBitmap);
				
				//Add any remaining photos to the gallery in a similar fashion
				while(locationsPhotos.moveToNext()){
					String photo = locationsPhotos.getString(photoIndex);
					
					photoPath = PHOTO_DIR + photo;
					
					//Set a large sample size to reduce memory useage when loading lots
					//of images
					options = new BitmapFactory.Options();
					options.inSampleSize = 32;
					
					//Create bitmap from the path, and then scale it down to a smaller
					//size for display reasons
					photoBitmap = BitmapFactory.decodeFile(photoPath, options);
					mImageIds.add(photoBitmap);
				}
				
			}
	    }
	}

}
