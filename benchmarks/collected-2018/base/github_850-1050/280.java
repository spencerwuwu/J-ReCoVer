// https://searchcode.com/api/result/7118789/

/*------------------------------------------------------------------------
* (The MIT License)
* 
* Copyright (c) 2008-2011 Rhomobile, Inc.
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
* 
* http://rhomobile.com
*------------------------------------------------------------------------*/

package rhomobile.mapview;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.rho.RhoClassFactory;
import com.rho.RhoEmptyLogger;
import com.rho.RhoLogger;
import com.rho.net.IHttpConnection;

import rhomobile.mapview.RhoMapField;

import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.util.Comparator;
//import net.rim.device.api.util.MathUtilities;
import net.rim.device.api.util.SimpleSortingVector;

public class GoogleMapField extends Field implements RhoMapField {

	private static final RhoLogger LOG = RhoLogger.RHO_STRIP_LOG ? new RhoEmptyLogger() : 
		new RhoLogger("GoogleMapField");
	
	// Configurable parameters
		
	// Size of workers pool (threads which actually fetch data from Google)
	private static final int WORKERS_POOL_SIZE = 1;
	// Maximum time for task executing by worker
	private static final long MAX_WORKER_TASK_TIME = 10000; // milliseconds
	// Interval of checking workers if they are finished
	private static final int WORKER_CHECK_INTERVAL = 500; // milliseconds
	// Maximum size of queue of waiting commands
	private static final int MAX_WAITING_QUEUE_SIZE = 20;
	
	// Image cache update interval
	private static final int CACHE_UPDATE_INTERVAL = 500; // milliseconds
	
	// Maximum size of image cache (number of images stored locally)
	private static final int MAX_IMAGE_CACHE_SIZE = 32;
	
	// Width of additional area appended to each tile at top, bottom, left and right
	private static final int ADDITIONAL_AREA_WIDTH = 16;
	// Size of own tiles
	private static final int TILE_SIZE = 300 - 2*ADDITIONAL_AREA_WIDTH;
	
	// Mode of decoding EncodedImage to bitmap
	private static final int DECODE_MODE = EncodedImage.DECODE_NATIVE |
		EncodedImage.DECODE_NO_DITHER | EncodedImage.DECODE_READONLY |
		EncodedImage.DECODE_ALPHA;

	// Static google parameters
	private static final int MIN_ZOOM = 0;
	private static final int MAX_ZOOM = 19;
	private static final int GOOGLE_TILE_SIZE = 256;
	private static final int MAX_GOOGLE_TILE_SIZE = 640;
	
	// Constants required to coordinates calculations
	private static final long MIN_LATITUDE = degreesToPixelsY(90, MAX_ZOOM);
	private static final long MAX_LATITUDE = degreesToPixelsY(-90, MAX_ZOOM);
	private static final long MAX_LONGITUDE = degreesToPixelsX(180, MAX_ZOOM);
	
	// DON'T CHANGE THIS CONSTANT!!!
	// This is maximum absolute value of sine ( == sin(85*PI/180) ) allowed by Merkator projection
	private static final double MAX_SIN = 0.99627207622;
	
	private static final double PI = Math.PI;
	
	//===============================================================================
	// Coordinates of center in pixels of maximum zoom level
	private long latitude = degreesToPixelsY(0, MAX_ZOOM);
	private long longitude = degreesToPixelsX(0, MAX_ZOOM);
	private int zoom = 0;
	
	private int tileSize;
	
	private int width;
	private int height;
	private String maptype;
	
	private class CachedImage {
		public EncodedImage image;
		public Bitmap bitmap;
		public long latitude;
		public long longitude;
		public int zoom;
		public String key;
		public long lastUsed;
		
		public CachedImage(EncodedImage img, long lat, long lon, int z) {
			image = img;
			bitmap = null;
			latitude = lat;
			longitude = lon;
			zoom = z;
			key = makeCacheKey(latitude, longitude, zoom);
			lastUsed = System.currentTimeMillis();
		}
	};
	
	private class ImageCache {
		private Hashtable hash;
		private SimpleSortingVector cvec;
		private SimpleSortingVector tvec;
		
		public ImageCache() {
			reinit();
		}
		
		private void reinit() {
			hash = new Hashtable();
			cvec = new SimpleSortingVector();
			cvec.setSortComparator(new ByCoordinatesComparator());
			cvec.setSort(true);
			tvec = new SimpleSortingVector();
			tvec.setSortComparator(new ByAccessTimeComparator());
			tvec.setSort(true);
		}
		
		public ImageCache clone() {
			ImageCache cloned = new ImageCache();
			for (Enumeration e = hash.elements(); e.hasMoreElements();)
				cloned.put((CachedImage)e.nextElement());
			return cloned;
		}
		
		public Enumeration sortedByCoordinates() {
			return cvec.elements();
		}
		
		public CachedImage get(String key) {
			return (CachedImage)hash.get(key);
		}
		
		public void put(CachedImage img) {
			put(img, true);
		}
		
		private void put(CachedImage img, boolean doCheck) {
			hash.put(img.key, img);
			cvec.addElement(img);
			tvec.addElement(img);
			if (doCheck) check();
		}
		
		private void check() {
			if (hash.size() < MAX_IMAGE_CACHE_SIZE)
				return;
			
			SimpleSortingVector vec = tvec;
			reinit();
			
			Enumeration e = vec.elements();
			while (e.hasMoreElements()) {
				CachedImage img = (CachedImage)e.nextElement();
				put(img, false);
			}
		}
	};
	
	private ImageCache cache = new ImageCache();
		
	private static abstract class MapCommand {
		public abstract String type();
		public abstract String description();
	}
	
	private static class MapFetchCommand extends MapCommand {
		
		public long latitude;
		public long longitude;
		public int zoom;
		public int width;
		public int height;
		public String maptype;
		public GoogleMapField mapField;
		
		public MapFetchCommand(long lat, long lon, int z, int w, int h,
				String m, GoogleMapField mf) {
			latitude = lat;
			longitude = lon;
			zoom = z;
			width = w;
			height = h;
			maptype = m;
			mapField = mf;
		}
		
		public boolean equals(Object c) {
			if (!(c instanceof MapFetchCommand))
				return false;
			MapFetchCommand cmd = (MapFetchCommand)c;
			return latitude == cmd.latitude && longitude == cmd.longitude &&
				zoom == cmd.zoom && width == cmd.width && maptype.equals(cmd.maptype) &&
				mapField == cmd.mapField;
		}
		
		public String type() {
			return "fetch";
		}
		
		public String description() {
			return "fetch:" + makeCoordDescription(latitude, longitude, zoom);
		}
	}
	
	private static class MapThread extends Thread {

		private static final String mapkey = "ABQIAAAA-X8Mm7F-7Nmz820lFEBHYxT2yXp_ZAY8_ufC3CFXhHIE1NvwkxSfNPZbryNEPHF-5PQKi9c7Fbdf-A";
		
		private static final int BLOCK_SIZE = 1024;
		
		private MapCommand processing = null;
		private long taskStartTime;
		private boolean active = true;
		
		// Return time of start current task. Return undefined value if no current task exists.
		public long startTime() {
			return taskStartTime;
		}
		
		public boolean isBusy() {
			synchronized (this) {
				return processing != null;
			}
		}
		
		public boolean isProcessing(MapCommand cmd) {
			synchronized (this) {
				return processing == null || cmd == null ? false : processing.equals(cmd);
			}
		}
		
		public void process(MapCommand cmd) {
			synchronized (this) {
				if (processing != null)
					throw new IllegalStateException();
				processing = cmd;
				if (processing != null)
					notify();
			}
		}
		
		public void stop() {
			active = false;
			interrupt();
		}
		
		private byte[] fetchData(String url) throws IOException 
		{
			int nTry = 0;
			byte[] data = null;
			do
			{
				IHttpConnection conn = null; 
				InputStream is = null;
				
				try
				{
					conn = RhoClassFactory.getNetworkAccess().connect(url,false);
					
					conn.setRequestMethod("GET");
					
					//conn.setRequestProperty("User-Agent", "Blackberry");
					//conn.setRequestProperty("Accept", "*/*");
					
					is = conn.openInputStream();
					
					int code = conn.getResponseCode();
					if (code/100 != 2)
						throw new IOException("Google map respond with " + code + " " + conn.getResponseMessage());
					
					int size = conn.getHeaderFieldInt("Content-Length", 0);
					data = new byte[size];
					if (size == 0)
						size = 1073741824; // 1Gb :)
					
					byte[] buf = new byte[BLOCK_SIZE];
					for (int offset = 0; offset < size;) {
						int n = is.read(buf, 0, BLOCK_SIZE);
						if (n <= 0)
							break;
						if (offset + n > data.length) {
							byte[] newData = new byte[offset + n];
							System.arraycopy(data, 0, newData, 0, data.length);
							data = newData;
						}
						System.arraycopy(buf, 0, data, offset, n);
						offset += n;
					}
					break;
					
				}catch(IOException exc)
				{
					String strError = exc.getMessage();
					LOG.INFO("openInputStream failed: " + strError);
					if ( strError != null && strError.indexOf("General socket error") >= 0)
					{
						LOG.INFO("Try connect one more time.");
					}else
						throw exc;
				}finally
				{
					if ( is != null )
						try{ is.close(); }catch(IOException e){}
	
					if ( conn != null )
						try{ conn.close(); }catch(IOException e){}
					
					is = null;
					conn = null;
				}
			}while( nTry <= 3 );
			
			return data;
		}
		
		private void processCommand(MapFetchCommand cmd) throws IOException {
			LOG.TRACE("Processing map fetch command (thread #" + hashCode() + "): " + cmd.description());
			
			// Process received command
			
			// Convert to degrees
			double latitude = pixelsToDegreesY(cmd.latitude, MAX_ZOOM);
			double longitude = pixelsToDegreesX(cmd.longitude, MAX_ZOOM);
			// Make url
			StringBuffer url = new StringBuffer();
			url.append("http://maps.google.com/maps/api/staticmap?");
			url.append("center=" + Double.toString(latitude) + "," + Double.toString(longitude));
			url.append("&zoom=" + cmd.zoom);
			url.append("&size=" + cmd.width + "x" + cmd.height);
			url.append("&maptype=" + cmd.maptype);
			url.append("&format=png&sensor=false");
			url.append("&mobile=" + (cmd.maptype.equals("roadmap") ? "true" : "false"));
			url.append("&key=" + mapkey);
			String finalUrl = url.toString();
		
			byte[] data = fetchData(finalUrl);
			
			EncodedImage img = EncodedImage.createEncodedImage(data, 0, data.length);
			img.setDecodeMode(DECODE_MODE);
			LOG.TRACE("Map request done, draw just received image");
			cmd.mapField.draw(cmd.latitude, cmd.longitude, cmd.zoom, img);
		}
		
		public void run() {
			try {
				while (active) {
					MapCommand cmd;
					synchronized (this) {
						processing = null;
						try {
							wait();
						} catch (InterruptedException e) {
							continue;
						}
						if (processing == null)
							continue;
						
						cmd = processing;
						taskStartTime = System.currentTimeMillis();
					}
					
					try {
						if (cmd instanceof MapFetchCommand)
							processCommand((MapFetchCommand)cmd);
						else
							LOG.INFO("Received unknown command: " + cmd.type() + ", ignore it");
					}
					catch (Exception e) {
						LOG.ERROR("Processing of map command failed", e);
					}
				}
			}
			catch (Exception e) {
				LOG.ERROR("Fatal error in map thread", e);
			}
			finally {
				LOG.INFO("Map thread exit");
			}
		}
		
	};
	
	private static class MapThreadPool extends Thread {

		private Vector queue = new Vector();
		private Vector threads = new Vector();
		
		public MapThreadPool() {
			setPriority(Thread.MIN_PRIORITY);
			for (int i = 0; i != WORKERS_POOL_SIZE; ++i) {
				Thread th = new MapThread();
				th.setPriority(Thread.MIN_PRIORITY);
				th.start();
				threads.addElement(th);
			}
		}
		
		public void send(MapCommand cmd) {
			LOG.TRACE("Send command: " + cmd.description());
			synchronized (queue) {
				queue.addElement(cmd);
				queue.notify();
			}
		}

		public void run() {
			Vector cmds;
			Vector waiting = new Vector();
			for(;;) {
				cmds = waiting;
				waiting = new Vector();
				
				synchronized (queue) {
					try {
						if (queue.isEmpty()) {
							if (cmds.isEmpty())
								queue.wait();
							else
								queue.wait(WORKER_CHECK_INTERVAL);
						}
					}
					catch (InterruptedException e) {
						LOG.ERROR(e);
						continue;
					}
					
					Enumeration e = queue.elements();
					while (e.hasMoreElements())
						cmds.addElement(e.nextElement());
					queue.removeAllElements();
				}
				
				boolean hasBusy = false;
				for (int ix = 0, clim = cmds.size(); ix != clim; ++ix) {
					MapCommand cmd = (MapCommand)cmds.elementAt(ix);
					LOG.TRACE("Look for thread to process command: " + (cmd == null ? "observe" : cmd.description()));
					
					boolean done = false;
					for (int i = 0, lim = threads.size(); !done && i < lim; ++i) {
						MapThread th = (MapThread)threads.elementAt(i);
						//LOG.TRACE("Receive fetch command #" + cmd.hashCode() + ", check if thread #" + th.hashCode() + " could process it...");
						if (th.isBusy()) {
							hasBusy = true;
							long maxTime = th.startTime() + MAX_WORKER_TASK_TIME;
							long curTime = System.currentTimeMillis();
							if (curTime > maxTime) {
								LOG.TRACE("Thread #" + th.hashCode() + " takes too much time to process command so interrupt it");
								// Send this command again
								MapCommand c = th.processing;
								th.stop();
								th = new MapThread();
								th.setPriority(Thread.MIN_PRIORITY);
								th.start();
								threads.setElementAt(th, i);
								th.process(c);
								done = false;
							}
							else if (th.isProcessing(cmd)) {
								//LOG.TRACE("Thread #" + th.hashCode() + " already processing given fetch command");
								done = true;
							}
							/*
							else
								LOG.TRACE("Thread #" + th.hashCode() + " is busy");
							*/
						}
						else {
							//LOG.TRACE("Thread #" + th.hashCode() + " is ready to process fetch command");
							th.process(cmd);
							done = true;
						}
					}
					
					if (!done && cmd != null) {
						LOG.TRACE("No threads ready to fetch data, waiting...");
						waiting.addElement(cmd);
					}
					else {
						LOG.TRACE("Thread found, command transferred there");
					}
				}
				
				if (!waiting.isEmpty()) {
					// Reduce waiting queue size to WAITING_FETCH_COMMAND_MAX,
					// removing most outdated commands (from begin of queue)
					for (int i = 0, lim = waiting.size() - MAX_WAITING_QUEUE_SIZE; i < lim; ++i)
						waiting.removeElementAt(0);
					LOG.TRACE("Waiting commands queue: " + waiting.size() + " elements");
				}
				
				if (hasBusy) {
					// Dummy element to get workers observable
					waiting.addElement(null);
				}
			}
		}
		
	};
	
	private static MapThreadPool fetchThreadPool = null;
	
	private long lastFetchCommandSent = 0;
	private CacheUpdate cacheUpdate;
	
	private class CacheUpdate extends Thread {
		
		private GoogleMapField mapField;
		private boolean active = true;
		
		public CacheUpdate(GoogleMapField mf) {
			mapField = mf;
		}
		
		public void stop() throws InterruptedException {
			LOG.TRACE("Stop cache update thread...");
			active = false;
			join();
		}
		
		public void run() {
			while (active) {
				try {
					try {
						Thread.sleep(CACHE_UPDATE_INTERVAL);
					} catch (InterruptedException e) {
						// Ignore
					}
					
					long curTime = System.currentTimeMillis();
					if (curTime + CACHE_UPDATE_INTERVAL < lastFetchCommandSent)
						continue;
					
					boolean fetchCommandWasSent = false;
					
					long ts = toMaxZoom(tileSize, zoom);
					long w = toMaxZoom(width, zoom);
					long h = toMaxZoom(height, zoom);
					
					int rts = tileSize + 2*ADDITIONAL_AREA_WIDTH;
					
					long tlLat = latitude - h/2;
					long tlLon = longitude - w/2;
					for (long lat = (tlLat/ts)*ts, latLim = tlLat + h + ts; lat < latLim; lat += ts) {
						for (long lon = (tlLon/ts)*ts, lonLim = tlLon + w + ts; lon < lonLim; lon += ts) {
							String key = makeCacheKey(lat, lon, zoom);
							MapFetchCommand cmd = null;
							synchronized (this) {
								CachedImage img = cache.get(key);
								if (img == null) {
									if (curTime - CACHE_UPDATE_INTERVAL > lastFetchCommandSent) {
										cmd = new MapFetchCommand(lat, lon, zoom,
												rts, rts, maptype, mapField);
										CachedImage dummy = new CachedImage(null, lat, lon, zoom);
										cache.put(dummy);
										fetchCommandWasSent = true;
									}
								}
								else {
									img.lastUsed = curTime;
								}
							}
							if (cmd != null)
								fetchThreadPool.send(cmd);
						}
					}
					
					if (fetchCommandWasSent)
						lastFetchCommandSent = curTime;
				}
				catch (Exception e) {
					LOG.ERROR("Cache update task failed", e);
				}
				catch (Throwable e) {
					LOG.ERROR("Cache update task failed", e);
					e.printStackTrace();
				}
			}
			LOG.TRACE("Cache update thread stopped");
		}
		
	};
	
	public GoogleMapField() {
		if (fetchThreadPool == null) {
			fetchThreadPool = new MapThreadPool();
			fetchThreadPool.start();
		}
		
		tileSize = TILE_SIZE;
		if (tileSize + 2*ADDITIONAL_AREA_WIDTH > MAX_GOOGLE_TILE_SIZE)
			tileSize = MAX_GOOGLE_TILE_SIZE - 2*ADDITIONAL_AREA_WIDTH;
		maptype = "roadmap";
		
		cacheUpdate = new CacheUpdate(this);
		cacheUpdate.start();
	}
	
	public void close() {
		try {
			cacheUpdate.stop();
		} catch (InterruptedException e) {
			LOG.ERROR("Stopping of cache update thread was interrupted", e);
		}
	}
	
	public Field getBBField() {
		return this;
	}
	
	protected void layout(int w, int h) {
		width = Math.min(width, w);
		height = Math.min(height, h);
		setExtent(width, height);
	}
	
	private String makeCacheKey(long lat, long lon, int z) {
		while (lon < 0) lon += MAX_LONGITUDE;
		while (lon > MAX_LONGITUDE) lon -= MAX_LONGITUDE;
		long x = lon/tileSize;
		long y = lat/tileSize;
		StringBuffer buf = new StringBuffer();
		buf.append(z);
		buf.append(';');
		buf.append(x);
		buf.append(';');
		buf.append(y);
		String key = buf.toString();
		return key;
	}
	
	private static String makeCoordDescription(long lat, long lon, int z) {
		double x = pixelsToDegreesX(lon, MAX_ZOOM);
		double y = pixelsToDegreesY(lat, MAX_ZOOM);
		return Double.toString(y) + ',' + Double.toString(x) + ";zoom=" + Integer.toString(z);
	}
	
	private void paintImage(Graphics graphics, CachedImage img) {
		// Temporary solution
		// TODO: implement drawing of images with different zoom levels
		if (img.zoom != zoom)
			return;
		
		long left = -toCurrentZoom(longitude - img.longitude, zoom);
		long top = -toCurrentZoom(latitude - img.latitude, zoom);
		
		if (img.zoom != zoom) {
			double x = MapTools.math_pow2d(img.zoom - zoom);
			int factor = Fixed32.tenThouToFP((int)(x*10000));
			img.image = img.image.scaleImage32(factor, factor);
			img.bitmap = null;
		}
		
		int imgWidth = img.image.getScaledWidth();
		int imgHeight = img.image.getScaledHeight();
		left += (width - imgWidth)/2;
		top += (height - imgHeight)/2;
		
		int w = width - (int)left;
		int h = height - (int)top;
		
		int maxW = width + tileSize + 2*ADDITIONAL_AREA_WIDTH;
		int maxH = height + tileSize + 2*ADDITIONAL_AREA_WIDTH;
		if (w < 0 || h < 0 || w > maxW || h > maxH) {
			// Image will not be displayed, free its bitmap and skip it
			img.bitmap = null;
			return;
		}
		
		if (img.bitmap == null)
			img.bitmap = img.image.getBitmap();
		graphics.drawBitmap((int)left, (int)top, w, h, img.bitmap, 0, 0);
	}
	
	protected void paint(Graphics graphics) {
		// Draw background
		for (int i = 1, lim = 2*Math.max(width, height); i < lim; i += 5) {
			graphics.drawLine(0, i, i, 0);
		}

		// Draw map tiles
		ImageCache imgCache;
		synchronized (this) {
			imgCache = cache.clone();
		}
		
		Enumeration e = imgCache.sortedByCoordinates();
		while (e.hasMoreElements()) {
			// Draw map
			CachedImage img = (CachedImage)e.nextElement();
			if (img.image == null)
				continue;
			
			paintImage(graphics, img);
		}
		
		/*
		// Draw pointer at center
		graphics.setColor(0x00000000); // Black
		// Draw black cross
		int xCenter = width/2;
		int yCenter = height/2;
		int delta = 10;
		int yTop = yCenter - delta;
		int yBottom = yCenter + delta;
		int xLeft = xCenter - delta;
		int xRight = xCenter + delta;
		graphics.drawLine(xCenter, yTop, xCenter, yBottom);
		graphics.drawLine(xLeft, yCenter, xRight, yCenter);
		*/
	}
	
	public void redraw() {
		invalidate();
	}
	
	public void draw(long lat, long lon, int z, EncodedImage img) {
		CachedImage newImage = new CachedImage(img, lat, lon, z);
		synchronized (this) {
			cache.put(newImage);
		}
		
		invalidate();
	}
	
	private static class ByCoordinatesComparator implements Comparator {
		
		public int compare (Object o1, Object o2) {
			CachedImage img1 = (CachedImage)o1;
			CachedImage img2 = (CachedImage)o2;
			if (img1.latitude < img2.latitude) return -1;
			if (img1.latitude > img2.latitude) return 1;
			if (img1.longitude < img2.longitude) return 1;
			if (img1.longitude > img2.longitude) return -1;
			if (img1.zoom < img2.zoom) return -1;
			if (img1.zoom > img2.zoom) return 1;
			return 0;
		}
		
	}
	
	private static class ByAccessTimeComparator implements Comparator {
		
		public int compare (Object o1, Object o2) {
			long l1 = ((CachedImage)o1).lastUsed;
			long l2 = ((CachedImage)o2).lastUsed;
			return l1 < l2 ? -1 : l1 > l2 ? 1 : 0;
		}
		
	};
	
	public int getPreferredWidth() {
		return width;
	}
	
	public int getPreferredHeight() {
		return height;
	}

	public void setPreferredSize(int w, int h) {
		width = w;
		height = h;
		setExtent(w, h);
	}
	
	private void validateCoordinates() {
		if (latitude < MIN_LATITUDE) latitude = MIN_LATITUDE;
		if (latitude > MAX_LATITUDE) latitude = MAX_LATITUDE;
	}
	
	public void moveTo(double lat, double lon) {
		latitude = degreesToPixelsY(lat, MAX_ZOOM);
		longitude = degreesToPixelsX(lon, MAX_ZOOM);
		validateCoordinates();
		
		lastFetchCommandSent = System.currentTimeMillis() + CACHE_UPDATE_INTERVAL;
	}

	public void move(int dx, int dy) {
		latitude += toMaxZoom(dy, zoom);
		longitude += toMaxZoom(dx, zoom);
		validateCoordinates();
		
		lastFetchCommandSent = System.currentTimeMillis() + CACHE_UPDATE_INTERVAL;
	}

	public int getMaxZoom() {
		return MAX_ZOOM;
	}

	public int getMinZoom() {
		return MIN_ZOOM;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int z) {
		zoom = z;
		if (zoom < MIN_ZOOM) zoom = MIN_ZOOM;
		if (zoom > MAX_ZOOM) zoom = MAX_ZOOM;
		lastFetchCommandSent = System.currentTimeMillis() + CACHE_UPDATE_INTERVAL;
	}
	
	public int calculateZoom(double latDelta, double lonDelta) {
		int zoom1 = calcZoom(latDelta, width);
		int zoom2 = calcZoom(lonDelta, height);
		return zoom1 < zoom2 ? zoom1 : zoom2;
	}
	
	public void setMapType(String type) {
		maptype = type;
		lastFetchCommandSent = 0;
	}
	
	private static int calcZoom(double degrees, int pixels) {
		double angleRatio = degrees*GOOGLE_TILE_SIZE/pixels;
		
		double twoInZoomExp = 360/angleRatio;
		int zoom = (int)MapTools.math_log2(twoInZoomExp);
		return zoom;
	}
	
	private static long toMaxZoom(long n, int zoom) {
		return n == 0 ? 0 : n*MapTools.math_pow2(MAX_ZOOM - zoom);
	}
	
	private static long toCurrentZoom(long coord, int zoom) {
		if (coord == 0) return 0;
		long pow = MapTools.math_pow2(MAX_ZOOM - zoom);
		return coord/pow;
	}

	private static long degreesToPixelsX(double n, int z) {
		while (n < -180.0) n += 360.0;
		while (n > 180.0) n -= 360.0;
		double angleRatio = 360d/MapTools.math_pow2(z);
		double val = (n + 180)*GOOGLE_TILE_SIZE/angleRatio;
		return (long)val;
	}
	
	private static long degreesToPixelsY(double n, int z) {
		// Google use Merkator projection so use it here
		double sin_phi = MapTools.math_sin(n*PI/180);
		// MAX_SIN - maximum value of sine allowed by Merkator projection
		// (~85.0 degrees of north latitude)
		if (sin_phi < -MAX_SIN) sin_phi = -MAX_SIN;
		if (sin_phi > MAX_SIN) sin_phi = MAX_SIN;
		
		double ath = MapTools.math_atanh(sin_phi);
		double val = GOOGLE_TILE_SIZE * MapTools.math_pow2(z) * (1 - ath/PI)/2;
		return (long)val;
	}
	
	private static double pixelsToDegreesX(long n, int z) {
		while (n < 0) n += MAX_LONGITUDE;
		while (n > MAX_LONGITUDE) n -= MAX_LONGITUDE;
		double angleRatio = 360d/MapTools.math_pow2(z);
		double val = n*angleRatio/GOOGLE_TILE_SIZE - 180.0;
		return val;
	}
	
	private static double pixelsToDegreesY(long n, int z) {
		// Revert calculation of Merkator projection
		double ath = PI - 2*PI*n/(GOOGLE_TILE_SIZE*MapTools.math_pow2(z));
		double th = MapTools.math_tanh(ath);
		double val = 180*MapTools.math_asin(th)/PI;
		return val;
	}
	
	public double getCenterLatitude() {
		return pixelsToDegreesY(latitude, MAX_ZOOM);
	}

	public double getCenterLongitude() {
		return pixelsToDegreesX(longitude, MAX_ZOOM);
	}

	public long toScreenCoordinateX(double n) {
		long v = degreesToPixelsX(n, zoom);
		long center = toCurrentZoom(longitude, zoom);
		long begin = center - width/2;
		return v - begin;
	}
	
	public long toScreenCoordinateY(double n) {
		long v = degreesToPixelsY(n, zoom);
		long center = toCurrentZoom(latitude, zoom);
		long begin = center - height/2;
		return v - begin;
	}
}

