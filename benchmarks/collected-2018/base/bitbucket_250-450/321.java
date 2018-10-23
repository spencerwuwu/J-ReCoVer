// https://searchcode.com/api/result/92105652/


package server;

import common.ActionId;
import common.ActionPair;
import fileutil.FileInfo;
import fileutil.FilePair;
import fileutil.FileUtil;
import fileutil.Packet;
import fileutil.SmallFile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.swing.Timer;
import rmiClass.ServerControl;

/**
 *
 * @author
 */
public class ServerControlImpl extends UnicastRemoteObject implements ServerControl{
    private Server server;
    private HashMap<ActionId, Timer> timerMap = new HashMap<>(); // the map of timers to detect the transfer time out for each user
    final public static int TIME_OUT = 600000; // the time out is set to 10 min
    private HashMap<ActionId, ArrayList<SmallFile>> chunksMap = new HashMap<>(); // the map for the buffer holds the chunks transferred by each user
    private HashMap<ActionId, Integer> loadMap = new HashMap<>(); // the map for the remained load for each transfer action
    
    public ServerControlImpl(Server server) throws RemoteException {
        super();
        this.server = server;
    }
    
    /**
     * compare the general file info and see which parts of the file need to be updated
     * @return the info of the file parts that needs to be updated. 
     * For the filepair list inside the ActionPair: 
     * if length is 0, the file does not need to be updated
     * @throws RemoteException 
     */
    @Override
    public ActionPair checkFileInfo(String userName, FileInfo fileInfo) throws RemoteException {
        ActionId actionId = new ActionId(userName, new Date().getTime());
        ArrayList<FilePair> resultList = new ArrayList<>();
        File serverRecord = ( Paths.get(userName, ".metadata", fileInfo.getMD5Name()) ).toFile();
        boolean needSync = false; // if it needs to synchronize among server
        if ( serverRecord.exists() ) {
            try {
                /* lock the record */
                RandomAccessFile raf = new RandomAccessFile(serverRecord, "rw");
                FileLock lock = raf.getChannel().lock();
                /* read the general file info from the record */
                 FileInfo serverFileInfo = (FileInfo) FileUtil.readObjectFromFile(serverRecord, raf);
                /* compare the MD5 of each part */
                ArrayList<FilePair> serverChunkList = serverFileInfo.getChunkList();
                ArrayList<FilePair> clientChunkList = fileInfo.getChunkList();
                if (serverChunkList.size() > clientChunkList.size()) {
                    /* delete the extra chunks */
                    int serverListLength = serverChunkList.size();
                    for ( int i = clientChunkList.size(); i < serverListLength; i++ ) {
                        String chunkFileName = serverChunkList.remove(clientChunkList.size()).getChunkFileName();
                        Path chunkPath = Paths.get(userName, chunkFileName);
                        try {
                            Files.delete(chunkPath);
                        } catch (IOException ex) {
                            System.out.println("[Error] Fail to remove the extra parts of file: " + chunkPath);
                        }
                    }
                    /* update the info */
                    serverFileInfo.setChunkList(serverChunkList);
                    serverFileInfo.updateSyncTime(actionId.getActionTime());
                    FileUtil.writeObjectToFile(serverRecord, raf, serverFileInfo);
                    needSync = true;
                }
                for ( int i = 0; i < clientChunkList.size(); i++ ) {
                    try {
                        FilePair localfp = serverChunkList.get(i);
                        FilePair clientfp = clientChunkList.get(i);
                        if ( clientfp.equalsTo(localfp) == 1 ) {
                            resultList.add(clientfp);
                        }
                    } catch (IndexOutOfBoundsException indexEx) {
                        /* more chunks are in the client */
                        resultList.add(clientChunkList.get(i));
                    }
                }
                lock.release();
                raf.close();
                if (needSync) {
                    server.sendUpdate(userName, serverFileInfo, false);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            /* the file does not exist on the server, need to upload everything */
            resultList = new ArrayList<>(fileInfo.getChunkList());
            if (resultList.isEmpty()) {
                /*
                 * the file in the client is empty
                 * only create a record for it but not create any chunks and don't need to upload the file
                 */
                createEmptyRecord(userName, fileInfo, actionId.getActionTime());
            }
        }
        ActionPair actionPair = new ActionPair(resultList, actionId);
        /* prepare for the transfer */
        prepareTransfer(actionPair);
        return actionPair;
    }
    

    private void addLoad(int load) {
        synchronized(server.load) {
            server.load += load;
        }
    }
 
    private void reduceLoad(int load) {
        synchronized(server.load) {
            server.load -= load;
        }
    }
    /**
     * upload a chunk to the server
     * @param actionid
     * @param chunk
     * @param isLastChunk
     * @return the server time stamp, indicating the time when sync finishes. -1 if not finish yet
     * @throws RemoteException 
     */
    @Override
    public long uploadChunk(ActionId actionid, SmallFile chunk, boolean isLastChunk) throws RemoteException {
        ArrayList<SmallFile> list = chunksMap.get(actionid);
        list.add(chunk);
        chunksMap.put(actionid, list);
        finishOneChunk(actionid);
        long serverTime = -1;
        if (isLastChunk) {
            /* all chunks have been uploaded */
            serverTime = new Date().getTime();
            updateFile(actionid.getUser(), list, serverTime);
            finishTransfer(actionid);
        }
        return serverTime;
    }
    /**
     * reset the timeout timer and reduce the load after one chunk is transferred
     * @param actionid 
     */
    private void finishOneChunk(ActionId actionid) {
        timerMap.get(actionid).restart();
        reduceLoad(1);
        int load = loadMap.get(actionid);
        loadMap.put(actionid, --load);
    }
    /**
     * the whole transer operation is finished
     * stop the timeout timer and delete the entries in maps
     * @param actionid 
     */
    private void finishTransfer(ActionId actionid) {
        timerMap.get(actionid).stop();
        timerMap.remove(actionid);
        loadMap.remove(actionid);
        chunksMap.remove(actionid);
    }
    private void prepareTransfer(ActionPair actionPair) {
        int load = actionPair.getFilePairList().size();
        if (load == 0) {
            /* no transfer is needed */
            return;
        }
        ActionId actionId = actionPair.getActionId();
        ArrayList<SmallFile> list = new ArrayList<>(load);
        chunksMap.put(actionId, list);
        loadMap.put(actionId, load);
        addLoad(load);
        setupTimer(actionId);
    }
    
    private void setupTimer(final ActionId actionId) {
        Timer timer = new Timer(TIME_OUT, new ActionListener() {
           public void actionPerformed(ActionEvent evt) {
               /*
                * if time is out, the client may lose connection
                * clear the load and the buffer
                */
               reduceLoad(loadMap.get(actionId));
               loadMap.remove(actionId);
               chunksMap.remove(actionId);
           }
        });
        timer.setRepeats(false);
        timerMap.put(actionId, timer);
        timer.start();
    }
    
    private void updateFile(String userName, ArrayList<SmallFile> list, long serverTime) {
        SmallFile smallfile = list.get(0);
        File serverRecord = ( Paths.get(userName, ".metadata", smallfile.getRecordName()) ).toFile();
        
        try {
            /* lock the file */
            RandomAccessFile raf = new RandomAccessFile(serverRecord, "rw");
            FileLock lock = raf.getChannel().lock();
            for (int i = 0; i < list.size(); i++ ) {
                SmallFile sf = list.get(i);
                Path chunkPath = Paths.get(userName, sf.getFilePair().getChunkFileName());
                Files.write(chunkPath, sf.getData());
            }
            /* update the server record */
            updateRecord(serverRecord, raf, list, serverTime);
            FileInfo record = (FileInfo) FileUtil.readObjectFromFile(serverRecord, raf);
            lock.release();
            raf.close();
            server.sendUpdate(userName, record, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /**
     * create a server record for the file which length is 0 bytes.
     * @param userName
     * @param fileinfo
     * @param serverTime 
     */
    private void createEmptyRecord(String userName, FileInfo fileinfo, long serverTime) {
        File serverRecord = ( Paths.get(userName, ".metadata", fileinfo.getMD5Name()) ).toFile();
        fileinfo.updateSyncTime(serverTime);
        try {
            /* lock the file */
            RandomAccessFile raf = new RandomAccessFile(serverRecord, "rw");
            FileLock lock = raf.getChannel().lock();
            FileUtil.writeObjectToFile(serverRecord, raf, fileinfo);
            lock.release();
            raf.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        server.sendUpdate(userName, fileinfo, false);
    }
    /**
     * update the server record
     * @param recordFile
     * @param raf
     * @param list 
     */
    private void updateRecord(File recordFile, RandomAccessFile raf, ArrayList<SmallFile> list, long syncTime) {
        FileInfo record = null;
        if (recordFile.length() != 0) {
            /* the serverRecord already exists */
            record = (FileInfo) FileUtil.readObjectFromFile(recordFile, raf);
            ArrayList<FilePair> chunkList = record.getChunkList();
            for (int i = 0; i < list.size(); i++) {
                FilePair fp = list.get(i).getFilePair();
                boolean isNew = true;
                /*
                 * search the list in the record
                 * if exists, replace it
                 * otherwise add into the record
                 */
                for ( int j = 0; j < chunkList.size(); j++ ) {
                    if (chunkList.get(j).equalsTo(fp) == 1) {
                        chunkList.set(j, fp);
                        isNew = false;
                        break;
                    }
                }
                if (isNew) {
                    /* the filepair does not exist in the record */
                    chunkList.add(fp);
                }
            }
            record.setChunkList(chunkList);
            record.updateSyncTime(syncTime);
        } else {
            /* the server record doesn't exist, create a new record */
            ArrayList<FilePair> pairList = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                pairList.add(list.get(i).getFilePair());
            }
            record = new FileInfo(list.get(0).getSubPath(), pairList, syncTime);
        }
        FileUtil.writeObjectToFile(recordFile, raf, record);
        
    }

    @Override
    public void deleteFile(String userName, String fileNameHash) throws RemoteException {
        try {
            server.deleteLocalFile(userName, fileNameHash);
        } catch (Exception ex) {
            System.out.println("Fail to delete the file: " + fileNameHash);
        }
        server.sendUpdate(userName, new FileInfo(fileNameHash), true);
    }

    @Override
    public ArrayList<FileInfo> getRecord(String userName) throws RemoteException {
        File[] files = Paths.get(userName, ".metadata").toFile().listFiles();
        ArrayList<FileInfo> recordList = new ArrayList<>();
        try {
            for (int i = 0; i < files.length; i++) {
                RandomAccessFile raf = new RandomAccessFile(files[i], "rw");
                FileInfo record = (FileInfo) FileUtil.readObjectFromFile(files[i], raf);
                recordList.add(record);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return recordList;
    }

    @Override
    public Packet download(String userName, String subpathHash) throws RemoteException {
        ArrayDeque<byte[]> dataQueue = new ArrayDeque<>();
        /* check the file record to get chunks information */
        File serverRecord = Paths.get(userName, ".metadata", subpathHash).toFile();
        long syncTime = -1; // -1 means the record does not exist
        if (serverRecord.exists()) {
            try {
                RandomAccessFile raf = new RandomAccessFile(serverRecord, "rw");
                FileLock lock = raf.getChannel().lock();
                FileInfo record = (FileInfo) FileUtil.readObjectFromFile(serverRecord, raf);
                dataQueue = server.getChunks(userName, record);
                syncTime = record.getSyncTime();
                lock.release();
                raf.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new Packet(dataQueue, syncTime);
    }
    
}

