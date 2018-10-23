// https://searchcode.com/api/result/123635028/

package com.nutiteq.bbmaps;

import java.util.Vector;

import net.rim.device.api.database.DataTypeException;
import net.rim.device.api.database.Database;
import net.rim.device.api.database.DatabaseBindingException;
import net.rim.device.api.database.DatabaseException;
import net.rim.device.api.database.DatabaseFactory;
import net.rim.device.api.database.DatabaseIOException;
import net.rim.device.api.database.Row;
import net.rim.device.api.database.Statement;
import net.rim.device.api.io.URI;

import com.nutiteq.log.Log;


public class BBMBTileCacheDatabaseHelper{

	private Database db;
	
	  private static final String CACHE_TILE_TABLE = "cached_tiles";
	  private static final String KEY_ID = "id";
	  private static final String DATA_ID = "tile_data";

	  private static final String KEY_CACHE_KEY = "cache_key";
	  private static final String KEY_RESOURCE_SIZE = "resource_size";
	  private static final String KEY_USED_TIMESTAMP = "used_timestamp";

	  private static final String CREATE_CACHE_INDEX_TABLE = "CREATE TABLE "
	      + CACHE_TILE_TABLE
	      + " (id INTEGER PRIMARY KEY AUTOINCREMENT, cache_key TEXT NOT NULL, "
	      + "resource_size INTEGER NOT NULL, used_timestamp INTEGER NOT NULL,"
	      +	"tile_data BLOB)";
	  
	  private static final String CREATE_CACHE_INDEX_TABLE2 = "CREATE INDEX TILE_INDEX ON "
	      + CACHE_TILE_TABLE
	      + " ("
	      + KEY_CACHE_KEY
	      +")";
	  

	public boolean open(String dir) {
		try{
			Log.debug("opening: "+dir);
            URI myURI = URI.create(dir); 
            db = DatabaseFactory.open(myURI);
				if (db!=null){
					return true;
				}
			} 
			catch (Exception e){
				Log.error(e.getMessage());
			}
		return false;
	}
	
	  public boolean containsKey(final String cacheKey) {
		    final long start = System.currentTimeMillis();
		    
		      try {
				Statement st = db.createStatement("SELECT "+KEY_ID+" FROM "+CACHE_TILE_TABLE+" WHERE " + KEY_CACHE_KEY + " = '"+cacheKey+"'");

				    st.prepare();
				    net.rim.device.api.database.Cursor c = st.getCursor();
				    
				    Row r;
				    while(c.next()) 
				    {
				        r = c.getRow();
					    Log.debug("execution time " + (System.currentTimeMillis() - start));
				        return true;
				    }
				c.close();
			} catch (DatabaseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    return false;
		  }
	
	public byte[] get(String cacheKey){
		  final long start = System.currentTimeMillis();

	        try {
				Statement st = db.createStatement("SELECT "+DATA_ID+" FROM "+CACHE_TILE_TABLE+" WHERE " + KEY_CACHE_KEY + " = '"+cacheKey+"'");
				st.prepare();
				net.rim.device.api.database.Cursor c = st.getCursor();
				
				Row r;
				while(c.next()) // one time really
				{
				    r = c.getRow();
				    final byte[] data = r.getBlobBytes(0);
				    Log.debug("found in time " + (System.currentTimeMillis() - start));
				    c.close();
				    return data;
				}
              c.close();
              Log.debug("not found in "+ (System.currentTimeMillis() - start));
			} catch (DatabaseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DataTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
	}
	
	public void close(){
		try {
			if (db!=null){
				db.close();
			}
		} catch (DatabaseIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addToCache(final String cacheKey, final byte[] data,
      final int resourceSize, final int cacheSize) {
	  
	  try {
		final long start = System.currentTimeMillis();
		  // insert
		  Statement st = db.createStatement("INSERT INTO "+CACHE_TILE_TABLE+"("+KEY_CACHE_KEY+","+KEY_RESOURCE_SIZE+","+KEY_USED_TIMESTAMP+","+DATA_ID+") " +
		  "VALUES (?,?,?,?)");
		  st.prepare();

		  st.bind(1, cacheKey);
		  st.bind(2, resourceSize);
		  st.bind(3, System.currentTimeMillis());
		  st.bind(4, data);
		  st.execute();
		  st.reset();
		  st.close();

		
		Log.debug("time after insert " + (System.currentTimeMillis() - start));

		// check size
		Statement st2 = db.createStatement("SELECT SUM(resource_size) FROM "+CACHE_TILE_TABLE);
		st2.prepare();
		net.rim.device.api.database.Cursor c = st2.getCursor();
		
		Row r;
		int totalSize = 0;
		while(c.next()) // one time really
		{
		    r = c.getRow();
		    totalSize = r.getInteger(0);
		}
		c.close();
		st.close();
		Log.debug("maxSize = " + cacheSize + " currentSize = " + totalSize+" add time " + (System.currentTimeMillis() - start));
		if (totalSize < cacheSize) {
		  return;
		} else {
			reduceCacheSize(totalSize - cacheSize);
			Log.debug("time with reduce " + (System.currentTimeMillis() - start));
		  return;
		}
	} catch (DatabaseBindingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (DatabaseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (DataTypeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }


private void reduceCacheSize(final int bytesNeededToFree) {
	
    Vector removedFiles = new Vector();
	try {
		Statement st = db.createStatement("SELECT "+KEY_CACHE_KEY+","+KEY_RESOURCE_SIZE+" FROM "+CACHE_TILE_TABLE+" ORDER BY used_timestamp ASC");
		st.prepare();
		net.rim.device.api.database.Cursor c = st.getCursor();

		removedFiles = new Vector();

		int moreBytesNeeded = bytesNeededToFree;
		
		Row r;
		while(c.next() && moreBytesNeeded > 0) // one time really
		{
		    r = c.getRow();
		    removedFiles.addElement(r.getString(0));
		    moreBytesNeeded -= r.getInteger(1);
		}
		
		c.close();
		st.close();
	} catch (DatabaseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (DataTypeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
    deleteFilesFromIndex(removedFiles);
  }

  private void deleteFilesFromIndex(final Vector removedFiles) {
	  final long start = System.currentTimeMillis();

try {
	//    final String[] files = removedFiles.toArray(new String[removedFiles.size()]);
	    final StringBuffer whereClause = new StringBuffer();
	    
	    for (int i = 0; i < removedFiles.size(); i++) {
	      whereClause.append(KEY_CACHE_KEY).append(" = ").append(removedFiles.elementAt(i));
	      if (i != removedFiles.size() - 1) {
	        whereClause.append(" OR ");
	      }
	    }
	    String delete = "DELETE FROM "+CACHE_TILE_TABLE+" "+whereClause.toString();
	    Log.debug(delete);
	    Statement st = db.createStatement(delete);
	
	    st.prepare();
	    st.execute();
	    st.close();
} catch (DatabaseException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
    Log.debug("Needed to delete " + removedFiles.size() +" time " + (System.currentTimeMillis() - start));
  }

  public boolean create(String cacheName) {
	Log.debug("create db tables...");
	try {
		try
	       {
	           URI myURI = URI.create(cacheName); 
	           db = DatabaseFactory.create(myURI);
	       }
	       catch ( Exception e ) 
	       {         
	           System.out.println( e.getMessage() );
	           e.printStackTrace();
	           return false;
	       }
		
	Statement st = db.createStatement(CREATE_CACHE_INDEX_TABLE);
	st.prepare();
	st.execute();
	st.close();

	Statement st2 = db.createStatement(CREATE_CACHE_INDEX_TABLE2);
	st2.prepare();
	st2.execute();
	st2.close();

	
	} catch (DatabaseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return false;
	}
	return true;
}

}

