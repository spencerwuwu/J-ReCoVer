// https://searchcode.com/api/result/7386455/

/*
 * FileCache.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package com.substanceofcode.map;

import com.substanceofcode.data.FileSystem;
import com.substanceofcode.data.Serializable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Image;

import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.view.Logger;
import java.util.Enumeration;

/**
 * Caches tiles to the filesystem. A byproduct of this is that tiles can be
 * downloaded via an external program on a pc and transfered across by loading
 * them onto a memory card
 *
 * @author gareth
 *
 */
public class FileCache implements TileCache, Runnable {

    private FileConnection Conn = null;
    private DataOutputStream streamOut = null;
    private DataInputStream streamIn = null;
    private Vector fileProcessQueue = new Vector();
    // private PrintStream streamPrint = null;
    private Thread cacheThread = null;
    private static final int THREADDELAY = 200;//

    private static final String cacheName = "MTEFileCache";
    private String fullPath = "";
    private String exportFolder = "";
    private long maxOffset=0;
    private String maxKey="";
    private long cachesize=0;
    // Default scope so it can be seen by the RMSCache
    Hashtable availableTileList = new Hashtable();

    public FileCache() {
        Logger.debug("FILE: FileCache ");
        exportFolder = Controller.getController().getSettings()
                .getExportFolder();

        fullPath = "file:///" + exportFolder + cacheName;
        Thread initThread = new Thread() {
            public void run() {
                initializeCache();
            }
        };
        //initThread.setPriority(Thread.MIN_PRIORITY);
        initThread.start();
        try {

            initThread.join();
        } catch (InterruptedException e1) {
            Logger.error("File: Error" + e1.getMessage());
            e1.printStackTrace();
        }
        cacheThread = new Thread(this);
        //cacheThread.setPriority(Thread.MIN_PRIORITY);
        cacheThread.start();
    }

    /**
     * Try to find a cache dir and if found, create a list of the files within
     * it. The files will be loaded only when they are requested
     *
     * 20090123: write a list of tiles and offsets to the RMS, then the next time
     * we initialize, load that list up, then check whether the last tile on the
     * list is still in the cache, if so then we have some confidence the list still
     * corresponds to the cache, just parse any extra tiles that may have been added
     * . If a tile is not found, invalidate the list and build a new one.
     */
    public void initializeCache() {
        long start,end;
        Logger.info("Initializing FileCache");
        start=System.currentTimeMillis();
        try {
            boolean rmsok = false;
            boolean reinit = true;
            try {
                Conn = (FileConnection) Connector.open(fullPath);
            } catch (IOException ex) {
                Logger.debug("File: failed to open " + fullPath);
            }

            if (Conn != null && !Conn.exists()) {
                // The file doesn't exist, we are done initializing
                Logger.debug("File: file does not exist");
                //create the file so we can start writing to it
                Conn.create();
                try {
                    FileSystem.getFileSystem().deleteFile("TileList");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                rmsok = readTileListFromRms();
                if (rmsok){
                    end=System.currentTimeMillis();
                    Logger.debug("Finished Initialisation in "+(end-start) +"ms");
                    Logger.debug("File: read tilelist from RMS OK");
                    Logger.warn("File: cachesize "+cachesize+" filesize "+Conn.fileSize());
                    if (cachesize == Conn.fileSize()) {
                        reinit = false;
                    }
                }
            }

            if (reinit) {
                availableTileList.clear();
                try {
                    Logger.info("Constructing FileCache from scratch");
                    {
                        streamIn = Conn.openDataInputStream();

                 //       Logger.debug("Conn.availableSize()=" + Conn.availableSize());
                        boolean reading = true;
                        while (reading) {
                            // There's no way of detecting the end of the stream
                            // short of getting an IOexception
                            try {
                                Tile t = Tile.getTile(streamIn);

                                Logger.debug("t is " + t.cacheKey + ", offset is "
                                        + t.offset);
                                if (t != null) {
                                    availableTileList.put(t.cacheKey,
                                            new Long(t.offset));
                                }
                            } catch (Exception ioe) {
                                reading = false;
                            }

                        }
                        Logger.debug("FILE: read " + availableTileList.size()
                                + " tiles");

                        streamIn.close();

                        streamIn = null;
                    }
                    storeTileListToRms();
                    end=System.currentTimeMillis();
                    Logger.debug("Finished Initialisation in "+(end-start) +"ms");
                } catch (IOException e) {
                    Logger.error("File: IOException: " + e.getMessage());
                    e.printStackTrace();
                } catch (SecurityException e) {
                    Logger.error("File: SecurityException: " + e.getMessage());
                }
            }
        }catch(Exception ex) {
            Logger.fatal("Error in FC.initializeCache() " + ex.getMessage());
        }
    }


    private boolean readTileListFromRms(){
        Logger.debug("File:readTileListFromRms");
         boolean result=false;
            Serializable x=
            new Serializable(){
                public String getMimeType(){return "";}
                public void serialize(DataOutputStream dos) throws IOException {
                 }
                public void unserialize(DataInputStream dis) throws IOException {
                    short len ;
                    byte[] bytes;
                    String key;
                    long offset;
                    int count=0;
                    Logger.debug("File:readTileListFromRms.unserialize");
                    boolean reading=true;
                    while (reading) {
                        try{
                            len = dis.readShort();
                            bytes = new byte[len];
                            dis.read(bytes, 0, len);
                            key = new String(bytes);
                            offset=dis.readLong();
                            if(offset>maxOffset){
                                maxOffset=offset;
                                maxKey=key;
                            }
                            if (key.equals("CACHESIZE")) {
                                cachesize = offset;
                            } else {
                                count++;
                                availableTileList.put(key, new Long(offset));
                            }
                        }catch(IOException io){
                            reading=false;
                        }
                    }
                    Logger.debug("File:read " +count+"tiles");
                }
            };
        try {
            if(FileSystem.getFileSystem().containsFile("TileList")){
                x.unserialize(FileSystem.getFileSystem().getFile("TileList"));
                result = true;
            }else{
                Logger.debug("TileList not found, will create");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private void storeTileListToRms(){
        Logger.debug("storeTileListToRms");
        long start,end;
        start=System.currentTimeMillis();
        try {
            FileSystem.getFileSystem().saveFile("TileList", new Serializable(){
                public String getMimeType(){return "tileList";}
                public void serialize(DataOutputStream dos) throws IOException {
                    Logger.debug("storeTileListToRms.serialize");
                    String key;
                     byte[] keyBytes;
                     long offset;
                     // store size to detect external changes
                     availableTileList.put("CACHESIZE", new Long(Conn.fileSize()));
                     Enumeration e =availableTileList.keys();
                     while(e.hasMoreElements()){
                      //   Logger.debug("serializing a tile");
                         key=  (String)(e.nextElement());
                     //    Logger.debug("key= "+key);
                         keyBytes = key.getBytes();
                        dos.writeShort(keyBytes.length);
                        dos.write(keyBytes);
                        offset= ((Long)availableTileList.get(key)).longValue();
                       // Logger.debug("offset= "+offset);
                        dos.writeLong(offset);
                     }
                 }
                public void unserialize(DataInputStream dis) throws IOException {}
            }, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        end=System.currentTimeMillis();
        Logger.debug("Wrote tileList to RMS in " +(end-start) +"ms");
    }

    public long getOffset(String name) {
        long offset = -1;
        if (availableTileList.containsKey(name)) {
            offset = ((Long) availableTileList.get(name)).longValue();
        }
        return offset;
    }

    /**
     * Take a Vector of tiles and attempt to serialize them all to the
     * filesystem
     *
     * @param tiles
     *                The vector of tiles to serialize
     * @return true if serialization was successful
     */
    /* Sony Ericsson JP-7 phones require the synchronization here to work */
    public synchronized boolean writeToFileCache(Vector tiles) {
        boolean result = false;
        String fullPath = "";
        String exportFolder = Controller.getController().getSettings()
                .getExportFolder();
        fullPath = "file:///" + exportFolder + cacheName;
        Logger.debug("tiles " + tiles.size());
        try {
            // ------------------------------------------------------------------
            // Create a FileConnection and if this is a new stream create the
            // file
            // ------------------------------------------------------------------

            // Logger.debug("FILE: path is " + fullPath);

            //Conn will be created once, in the initCache method
            // if Conn is subsequently lost, too bad, we won't try to recreate it

            //if (Conn == null) {
            //    Conn = (FileConnection) Connector.open(fullPath);
           // }
            try {
                // Create file
                if (Conn != null && !Conn.exists()) {
                    Conn.create();
                } else {
                    // Logger.debug("File: file already exists, skipping: "
                    // + fullPath);
                }

            } catch (IOException ex) {
                Logger.error("writeAllToFileCache: Unable to open file : "
                        + fullPath + ", Full details : " + ex.toString());
            }

            if (Conn != null && streamOut == null) {
                // open the steam at the end so we can append to the file

                OutputStream x = Conn.openOutputStream(Conn.fileSize());

                streamOut = new DataOutputStream(x);
            } else {
                // Logger.debug("streamOut is not null");
            }

            if (streamOut != null) {

                boolean firstTile = true;
                while (fileProcessQueue.size() > 0) {

                    Tile t = (Tile) fileProcessQueue.firstElement();
                    // buffer=t.getImageByteArray();
                    fileProcessQueue.removeElementAt(0);

                   //Only serialize tile we know are not already serialized...
                  if(!checkCache(t.cacheKey)){
                    //The first tile will not have the correct offset if the file is
                    //not empty, so we should give it the offset
                    if (firstTile) {
                        t.serialize(streamOut, Conn.fileSize());
                        firstTile=false;
                    } else {
                        t.serialize(streamOut);
                    }
                    // streamOut.write(buffer, 0, buffer.length);

                    streamOut.flush();

                    // Specifically keep the file OPEN, this should prevent too
                    // many
                    // Permission requests
                    // streamOut.close();
                    // outConn.close();
                    result = true;
                    availableTileList.put(t.cacheKey,
                            new Long(Tile.totalOffset));
                    Logger.debug("availableTileList size="
                            + availableTileList.size());
                    //Must do this to keep the rms copy up to date
                    //Maybe we can avoid doing this on every write, but then we may
                    //end up with unindexed tiles in the cache
                    storeTileListToRms();
                  }else{
                      Logger.debug("Not Writing tile, already serialized:" +t.cacheKey);
                  }
                }
                streamOut.close();
            } else {
                Logger.debug("File: output stream is null");
            }

            streamOut = null;

        } catch (IOException e) {
            Logger.debug("FILE: error:" + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public Image getImage(String name) {

        Image out = null;

        try {
            out = getTile(name).getImage();
        } catch (Exception e) {
            Logger.error("FileCache:" + e.getMessage());
            e.printStackTrace();
        }

        return out;
    }

    public Tile getTile(String name) {
        Tile t = null;
        boolean reading = true;
        if (checkCache(name)) {
            if (Conn != null) {
                try {
                    if (streamIn == null) {
                        InputStream x = Conn.openInputStream();
                        Logger.debug("Skipping " + getOffset(name)
                                + " bytes");
                        x.skip(getOffset(name));

                        streamIn = new DataInputStream(x);
                    }
                    if (streamIn != null) {

                        while (reading) { //do we still need this
                            try {
                                // Assuming that a concatenated bunch of tiles
                                // can
                                // be deserialized
                                // one at a time this way
                                t = Tile.getTile(streamIn);
                                if (t != null && t.cacheKey != null
                                        && t.cacheKey.equals(name)) {
                                    // Found the right tile
                                    break;
                                }
                                //counter++;
                            } catch (IOException e) {
                                Logger.debug("File IOException: Didn't find the tile...");
                                reading = false;
                                e.printStackTrace();
                            } catch (Exception e) {
                                Logger.debug("Didn't find the tile..." +e.getMessage());
                                reading = false;
                                e.printStackTrace();
                            }
                        }
                        streamIn.close();
                        streamIn = null;
                    }
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    Logger.error("File IOException" + name);
                    e1.printStackTrace();
                } catch (NullPointerException npe) {
                    Logger.debug("Caught NPE: name is " + name);
                }
            }else{
                Logger.error("Con was null. Initialisation error");
            }
        }
        return t;
    }

    public boolean checkCache(String name) {
        if (availableTileList.containsKey(name)) {

            return true;
        } else {
          //  Logger.debug("Didn't find tile in filecache: " + name);
            return false;
        }
    }

    private void addToQueue(Tile tile) {
        Logger.debug("FILE:Adding Tile to File queue");
        synchronized (fileProcessQueue) {
            if (!fileProcessQueue.contains(tile)) {
                fileProcessQueue.addElement(tile);
            }
        }
        Logger.debug("FILE: FILE queue size now " + fileProcessQueue.size());
    }

    /**
     * This version will write the whole list out as one file in order to reduce
     * the amount of times permission needs to be sought.
     */
    public void run() {
        Thread thisThread = Thread.currentThread();

        try {
            // Logger.debug("FILE:Initialized ok, now sleeping for 1sec");

            Thread.sleep(THREADDELAY);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (cacheThread == thisThread) {

            try {

                Thread.sleep(THREADDELAY);
            } catch (InterruptedException e) {
                // Logger.debug("FileCache:Thread was interrupted");


            }
            synchronized (fileProcessQueue) {

                try {
                    if (fileProcessQueue.size() > 0) {

                        Logger.debug("FILE: FILE queue size is:"
                                + fileProcessQueue.size());
                        try {
                            // Logger.debug("FILE: " + cacheName);

                            writeToFileCache(fileProcessQueue);

                        } catch (Exception e) {
                            Logger
                                    .error("FILE: Exception while writing tile to filesystem: "
                                            + e.getMessage());
                        }
                    } else {
                       //  Logger.debug("FILE: FILEProcessQueueEmpty, sleeping ");
                        Thread.sleep(1000);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void put(Tile tile) {
        addToQueue(tile);
    }

    /**
     * Checks that the cache contains only valid tile information. One of the
     * issues that can affect the file cache is being interrupted while writing
     * a tile out. This will often result in a foreshortened byte array. This
     * can be detected as the size of the array is written out immediately
     * before the byte array. Other things to check for are that the xyz ints
     * are all within the expected range (0-2^18ish) and the Strings are not
     * null. TODO: Implement this
     *
     * @return
     */
    private boolean verifyCacheIntegrity() {
        return true;
    }
}

