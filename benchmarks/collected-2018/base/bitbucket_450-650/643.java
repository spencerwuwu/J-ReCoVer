// https://searchcode.com/api/result/49334749/

package com.fedorvlasov.lazylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import org.xbill.DNS.MBRecord;

import android.app.Activity;import com.google.analytics.tracking.android.TrackedActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.imomentousapp.R;

public class Loader {
    
    MemoryCache memoryCache=new MemoryCache();
    FileCache fileCache;
    private Map<ImageView  , String> imageViews=Collections.synchronizedMap(new WeakHashMap<ImageView  , String>());
    Context mContext;
    Activity activity;
    static Bitmap display;
    
    private static final float PHOTO_BORDER_WIDTH2 = 3.0f;
    private static final float PHOTO_BORDER_WIDTH = 2.0f;
    private static final int PHOTO_BORDER_COLOR2=0x00000000;
    private static final int PHOTO_BORDER_COLOR = 0xffffffff;

   private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final Paint sStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint sNumPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
    static {
        sNumPaint.setColor(Color.BLACK);
        sNumPaint.setTextSize(25);
        sNumPaint.setTextAlign(Align.RIGHT);
    }
    static {
           sStrokePaint.setStrokeWidth(PHOTO_BORDER_WIDTH);
           sStrokePaint.setStyle(Paint.Style.STROKE);
          // sStrokePaint.measureText("hello");
           sStrokePaint.setColor(PHOTO_BORDER_COLOR);

    }
    
   // ProgressBar bar;
    private Handler handler=new Handler();
    private Runnable run=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			updateText();
		}
	};

    
    public Loader(Context context){
        //Make the background thead low priority. This way it will not affect the UI performance
        photoLoaderThread.setPriority(Thread.NORM_PRIORITY-1);
        mContext=context;
        fileCache=new FileCache(context);
       // clearCache();
    }
    
    final int stub_id=R.color.white;
    public void DisplayImage(String url, Activity activity, ImageView imageView)
    {
   try{
	   this.activity=activity;
       
    	url=url.replaceAll("\"","");
    	url=url.substring(url.indexOf("\"")+1,url.length());
    	//url=url.replaceAll("\\","\"");
    	//url = URLEncoder.encode(url.toString(), "UTF-8");
        imageViews.put(imageView, url);
      //  bar=b;
        Bitmap bitmap=memoryCache.get(url);
        
        if(bitmap!=null){
        	bitmap=getNewBitmap(bitmap);
            imageView.setImageBitmap(bitmap);
           // bar.setVisibility(View.GONE);
        }
        else
        {
            queuePhoto(url, activity, imageView);
            imageView.setImageResource(stub_id);
            //bar.setVisibility(View.VISIBLE);
        }   
        
       // if(bitmap!=null){
        //	bitmap.recycle();
       // }
   }catch (Exception e) {
	// TODO: handle exception
	   e.printStackTrace();
}
    }
    
  
        
    private void queuePhoto(String url, Activity activity, ImageView imageView)
    {
        //This ImageView may be used for other images before. So there may be some old tasks in the queue. We need to discard them. 
        photosQueue.Clean(imageView);
        PhotoToLoad p=new PhotoToLoad(url, imageView);
        synchronized(photosQueue.photosToLoad){
            photosQueue.photosToLoad.push(p);
            photosQueue.photosToLoad.notifyAll();
        }
        
        //start thread if it's not started yet
        if(photoLoaderThread.getState()==Thread.State.NEW)
            photoLoaderThread.start();
    }
    
    private Bitmap getBitmap(String url) 
    {
        File f=fileCache.getFile(url);
        
        //from SD cache
        Bitmap b = decodeFile(f);
        if(b!=null)
            return b;
        
        //from web
        try {
        	
        	//url=url.replaceAll("http://ec-mt.imomentous.com/admintest//","http://ec-mt.imomentous.com/admintest/");
        	url=url.replaceAll(" ","%20");
            Bitmap bitmap=null;
            /*HttpGet httpRequest = new HttpGet(URI.create(url) );
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
            HttpEntity entity = response.getEntity();
            BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            InputStream instream = bufHttpEntity.getContent();*/
            URL urls = new URL(url);
            HttpURLConnection connection  = (HttpURLConnection) urls.openConnection();

            InputStream instream = connection.getInputStream();
           
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=110;
            
            BitmapFactory.Options opts=new BitmapFactory.Options();
            //opts.inJustDecodeBounds = true;
            int width_tmp=opts.outWidth, height_tmp=opts.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            
            //decode with inSampleSize
           
            
           
            opts.inDither=false;                     //Disable Dithering mode
            opts.inPurgeable=true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
            opts.inInputShareable=true;      
          //  opts.inSampleSize=scale;
            opts.inScaled = false;
            bitmap = BitmapFactory.decodeStream(instream,null,opts);
            //handler.post(run);
            return bitmap;
        } catch (Exception ex){
           ex.printStackTrace();
           return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);
            
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=110;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            
            if (o.outHeight > 110 || o.outWidth > 110) {
                scale = (int)Math.pow(2, (int) Math.round(Math.log(110 / 
                   (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }
            
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public String url;
        public ImageView imageView;
        public PhotoToLoad(String u, ImageView i){
            url=u; 
            imageView=i;
        }
    }
    
    PhotosQueue photosQueue=new PhotosQueue();
    
    public void stopThread()
    {
        photoLoaderThread.interrupt();
    }
    
    //stores list of photos to download
    class PhotosQueue
    {
        private Stack<PhotoToLoad> photosToLoad=new Stack<PhotoToLoad>();
        
        //removes all instances of this ImageView
        public void Clean(ImageView image)
        {
            for(int j=0 ;j<photosToLoad.size();){
                if(photosToLoad.get(j).imageView==image)
                    photosToLoad.remove(j);
                else
                    ++j;
            }
        }
    }
    
    class PhotosLoader extends Thread {
        public void run() {
            try {
                while(true)
                {
                    //thread waits until there are any images to load in the queue
                    if(photosQueue.photosToLoad.size()==0)
                        synchronized(photosQueue.photosToLoad){
                            photosQueue.photosToLoad.wait();
                        }
                    if(photosQueue.photosToLoad.size()!=0)
                    {
                        PhotoToLoad photoToLoad;
                        synchronized(photosQueue.photosToLoad){
                            photoToLoad=photosQueue.photosToLoad.pop();
                        }
                        Bitmap bmp=getBitmap(photoToLoad.url);
                        memoryCache.put(photoToLoad.url, bmp,null);
                        String tag=imageViews.get(photoToLoad.imageView);
                        if(tag!=null && tag.equals(photoToLoad.url)){
                        	
                        	//bar.setVisibility(View.GONE);
                        	//handler.post(run);
                            BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad.imageView);
                         
                            //Activity a=(Activity)mContext;
                           if(activity!=null)
                            activity.runOnUiThread(bd);
                        
                        }
                    }
                    if(Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
                //allow thread to exit
            }
        }
    }
    
    PhotosLoader photoLoaderThread=new PhotosLoader();
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        ImageView imageView;
        public BitmapDisplayer(Bitmap b, ImageView i){
        	
        	//handler.post(run);
        	 if(b!=null) {
             	bitmap=getNewBitmap(b);
             	bitmap=rotateAndFramezero(bitmap, 0);
        	 }
             else
             	bitmap=b;
        	 
        	 display=bitmap;
        	 
        	imageView=i;
      }
        public void run()
        {
        	//handler.post(run);
        	  if(bitmap!=null)
                  imageView.setImageBitmap(bitmap);
              else
                  imageView.setImageResource(stub_id);
        }
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }
    
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
    
    public Bitmap getNewBitmap(Bitmap bitmap){
     try{
    	 
    	 
    	 BitmapFactory.Options options = new BitmapFactory.Options(); 
    	 options.inPurgeable = true; 
    	 Bitmap bit=Bitmap.createScaledBitmap(bitmap,convertDpToPixel(150,mContext),convertDpToPixel(150,mContext), false);
    	 //bitmap.recycle()
    	 return bit;
     }catch (Exception e) {
		// TODO: handle exception
    	 return null;
	}
    }
    
    public static Bitmap getBitmap(){
    	return display;
    }
    
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
    
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
        if (width > height) {
            inSampleSize = Math.round((float)height / (float)reqHeight);
        } else {
            inSampleSize = Math.round((float)width / (float)reqWidth);
        }
    }
    return inSampleSize;
}
    
    public static int convertDpToPixel(float dp,Context context){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float px = dp * (metrics.densityDpi/160f);
	    return (int)px;
	}
    
    Bitmap rotateAndFramezero(Bitmap bitmap,float angle) {
        // final boolean positive = sRandom.nextFloat() >= 0.5f;
       //  final float angle = (ROTATION_ANGLE_MIN + sRandom.nextFloat() * ROTATION_ANGLE_EXTRA) *
         //        (positive ? 1.0f : -1.0f);
         final double radAngle = Math.toRadians(angle);

         final int bitmapWidth = bitmap.getWidth();
         final int bitmapHeight = bitmap.getHeight();

         final double cosAngle = Math.abs(Math.cos(radAngle));
         final double sinAngle = Math.abs(Math.sin(radAngle));

         final int strokedWidth = (int) (bitmapWidth + 2 * 0);
         final int strokedHeight = (int) (bitmapHeight + 2 * 0);

         final int width = (int) (strokedHeight * sinAngle + strokedWidth * cosAngle);
         final int height = (int) (strokedWidth * sinAngle + strokedHeight * cosAngle);

         final float x = (width - bitmapWidth) / 2.0f;
         final float y = (height - bitmapHeight) / 2.0f;

         final Bitmap decored = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

         final Canvas canvas = new Canvas(decored);

         canvas.rotate(angle, width / 2.0f, height / 2.0f);
         canvas.drawBitmap(bitmap, x, y, sPaint);
         canvas.drawRect(x, y, x + bitmapWidth, y + bitmapHeight, sStrokePaint);
      
         return decored;
     }
    
    public void updateText(){
    	//bar.setVisibility(View.GONE);
    }


}

