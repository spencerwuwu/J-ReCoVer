// https://searchcode.com/api/result/100320224/

/**
 * e-Science Central
 * Copyright (C) 2008-2013 School of Computing Science, Newcastle University
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation at:
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, 5th Floor, Boston, MA 02110-1301, USA.
 */
package com.connexience.server.workflow.cloud.library;

import com.connexience.server.model.document.DocumentRecord;
import com.connexience.server.model.document.DocumentVersion;
import com.connexience.server.workflow.cloud.download.*;
import com.connexience.server.workflow.api.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import org.apache.log4j.*;

/**
 * This class maintains a list of the available services and dependencies installed
 * on the server.
 * @author nhgh
 */
public class ServiceLibrary {
    static Logger logger = Logger.getLogger(ServiceLibrary.class);
    
    /** Parent cloud workflow engine */
    private ServiceLibraryContainer parent;

    /** Libraries that have already been downloaded */
    //private ArrayList<CloudWorkflowServiceLibraryItem> libraryItems = new ArrayList<CloudWorkflowServiceLibraryItem>();
    private CopyOnWriteArrayList<CloudWorkflowServiceLibraryItem> libraryItems = new CopyOnWriteArrayList<>();
    
    /** Download manager */
    private DownloadManager downloadManager;

    /** Listeners */
    private CopyOnWriteArrayList<ServiceLibraryListener> listeners = new CopyOnWriteArrayList<>();

    /** Is this a runtime library or a development library. Runtime libraries have
     * all dependencies downloaded regardless of whether they are runtime only or not.
     * Development libraries only get non runtime dependencies downloaded */
    private boolean developmentEngine = false;

    /** Cache of information to reduce server calls */
    private LibraryInformationCache cache = new LibraryInformationCache();
    
    /** Create with a parent engine */
    public ServiceLibrary(ServiceLibraryContainer parent){
        this.parent = parent;
        downloadManager = new DownloadManager(this);
        logger.debug("Created service library in: " + parent.getLibraryDirectory().getPath());
    }

    /** Add a listener to this library */
    public void addServiceLibraryListener(ServiceLibraryListener listener){
        this.listeners.add(listener);
    }

    /** Remove a listener from this library */
    public synchronized void removeServiceLibraryListener(ServiceLibraryListener listener){
        this.listeners.remove(listener);
    }

    /** Get the size of the library */
    public int getLibrarySize(){
        return libraryItems.size();
    }

    /** Get a library item */
    public CloudWorkflowServiceLibraryItem getLibraryItem(int index){
        return libraryItems.get(index);
    }
    
    /** Notify listeners that the library contents have changed */
    private void notifyLibraryContentsChanged(){
        for(int i=0;i<listeners.size();i++){
            listeners.get(i).libraryContentsChanged(this);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        listeners.clear();
    }

    /** Is this engine being used in a development environment */
    public boolean isDevelopmentEngine() {
        return developmentEngine;
    }

    /** Set whether this engine is being used in a development environment */
    public void setDevelopmentEngine(boolean developmentEngine) {
        this.developmentEngine = developmentEngine;
    }

    /** Get the library directory */
    public File getLibraryDirectory(){
        return parent.getLibraryDirectory();
    }

    /** Flush the library directory */
    public synchronized void flushLibrary() throws IOException {
        logger.debug("Flushing service library");
        libraryItems.clear();
        File libDir = parent.getLibraryDirectory();
        emptyDirectory(libDir);
    }

    /** Empty a directory and recurse down */
    private synchronized void emptyDirectory(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException("Directory does not exists: '" + dir + "'");
        }

        File[] contents = dir.listFiles();
        for(int i=0;i<contents.length;i++){
            if(contents[i].isFile()){
                contents[i].delete();
            } else {
                emptyDirectory(contents[i]);
                contents[i].delete();
            }
        }
    }

    /** Prepare a library and notify the callback when it is done */
    public void prepareService(API apiLink, String serviceId, LibraryCallback callback, LibraryPreparationReport report, boolean fetchDependencies){
        logger.debug("Preparing service: " + serviceId + " InvocationID=" + report.getInvocationId());
        
        // Get the latest version of the service first
        DocumentRecord serviceDoc;
        String serviceVersionId;

        try {
            serviceDoc = getServiceById(serviceId, apiLink);
            if(serviceDoc!=null){
                serviceVersionId = getLatestDocumentVersionId(serviceDoc, apiLink);
                if(serviceVersionId!=null){
                    // Now do the download
                    prepareService(apiLink, serviceDoc.getId(), serviceVersionId, callback, report, fetchDependencies);
                    
                } else {
                    logger.error("No versions for service: "+ serviceId + " InvocationID=" + report.getInvocationId());
                    throw new Exception("No versions");
                }
            } else {
                logger.error("No such service: " + serviceId + " InvocationID=" + report.getInvocationId());
                throw new Exception("Cannot find service");
            }
        } catch (Exception e){
            logger.error("Error getting service data. InvocationID=" + report.getInvocationId(), e);
            callback.libraryPreparationFailed("Error getting service data: " + e.getMessage(), report);
        }
    }

    /** Prepare a dependency object and notify the callback when it is done */
    public void prepareDependency(API apiLink, String libraryName, int libraryVersion, LibraryCallback callback, LibraryPreparationReport report, boolean fetchDependencies){
        logger.debug("Preparing dependency: " + libraryName + " v" + libraryName + " InvocationID=" + report.getInvocationId());
        // Get the latest version first
        DocumentRecord libraryDoc;
        DocumentVersion libVersion = null;

        try {
            libraryDoc = getLibraryByName(libraryName, apiLink);
            report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Preparing library: " + libraryName);
            if(libraryDoc!=null){
                List<DocumentVersion> versions = apiLink.getDocumentVersions(libraryDoc);

                // Check for more
                if(versions.size()>1){
                    for(int i=1;i<versions.size();i++){
                        if(versions.get(i).getVersionNumber()==libraryVersion){
                            libVersion = versions.get(i);
                        }
                    }
                }

                if(libVersion!=null){
                    // See if we already have the file
                    CloudWorkflowServiceLibraryItem item = locateDependencyItem(libraryName, libVersion.getVersionNumber());
                    if(item!=null){
                        if(item.hasLatestVersionDependencies()){
                            report.addMessage(LibraryPreparationReport.ITEM_PRESENT, libraryName + " - checking latest versions");
                            DependencyFetcher fetcher = new DependencyFetcher(item, this, apiLink, report);
                            fetcher.fetchDependencies(callback);
                        } else {
                            report.addMessage(LibraryPreparationReport.ITEM_PRESENT, libraryName + " is already present and has no latest version dependencies");
                            callback.libraryReady(item, report);
                        }
                    } else {
                        report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, libraryName + " will be downloaded");
                        downloadManager.startDownload(apiLink, libraryDoc.getId(), libVersion.getId(), new DownloadCallbackTrigger(callback, apiLink, this, report), report);
                    }
                } else {
                    logger.error("No version: " + libraryVersion + " for: "+ libraryName + " InvocationID=" + report.getInvocationId());
                    report.addMessage(LibraryPreparationReport.ITEM_NOT_FOUND, "Version: " + libVersion + " of " + libraryName + " cannot be found");
                    callback.libraryPreparationFailed("No such version (" + libraryVersion + ") for library: " + libraryName, report);
                }

            } else {
                logger.error("Library: " + libraryName + " does not exist. InvocationID=" + report.getInvocationId());
                report.addMessage(LibraryPreparationReport.ITEM_NOT_FOUND, "Library " + libraryName + " does not exist");
                callback.libraryPreparationFailed("No such library: " + libraryName, report);
            }
        } catch (Exception e){
            logger.error("Error getting library data. InvocationID=" + report.getInvocationId(), e);
            report.addMessage(LibraryPreparationReport.ITEM_DOWNLOAD_FAILED, "Library preparation error: " + e.getMessage());
            callback.libraryPreparationFailed("Error getting library data: " + e.getMessage(), report);
        }

    }

    /** Get the latest version of a dependency object and notify the callback when it is done */
    public void prepareDependency(API apiLink, String libraryName, LibraryCallback callback, LibraryPreparationReport report, boolean fetchDependencies){
        logger.debug("Preparing latest version dependency: " + libraryName + " InvocationID=" + report.getInvocationId());
        // Get the latest version first
        DocumentRecord libraryDoc;
        String libVersionId;

        try {
            libraryDoc = getLibraryByName(libraryName, apiLink);
            report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Preparing library: " + libraryName);
            if(libraryDoc!=null){
                libVersionId = getLatestDocumentVersionId(libraryDoc, apiLink);

                // See if we already have the file
                CloudWorkflowServiceLibraryItem item = locateDependencyItem(libraryName, libVersionId);
                if(item!=null){
                    if(item.hasLatestVersionDependencies()){
                        report.addMessage(LibraryPreparationReport.ITEM_PRESENT, libraryName + " - checking latest versions");
                        DependencyFetcher fetcher = new DependencyFetcher(item, this, apiLink, report);
                        fetcher.fetchDependencies(callback);
                    } else {
                        report.addMessage(LibraryPreparationReport.ITEM_PRESENT, libraryName + " is already present and has no latest version dependencies");
                        callback.libraryReady(item, report);
                    }

                } else {
                    report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, libraryName + " will be downloaded");
                    downloadManager.startDownload(apiLink, libraryDoc.getId(), libVersionId, new DownloadCallbackTrigger(callback, apiLink, this, report), report);
                }
                
            } else {
                logger.error("No such library: " + libraryName + " InvocationID=" + report.getInvocationId());
                callback.libraryPreparationFailed("No such library: " + libraryName, report);
            }

        } catch (Exception e){
            logger.error("Error getting library data. InvocationID=" + report.getInvocationId());
            report.addMessage(LibraryPreparationReport.ITEM_DOWNLOAD_FAILED, "Library preparation error: " + e.getMessage());
            callback.libraryPreparationFailed("Error getting library data: " + e.getMessage(), report);
        }
    }
    
    /** Prepare a library and notify the callback when it is done */
    public void prepareService(API apiLink, String serviceId, String versionId, LibraryCallback callback, LibraryPreparationReport report, boolean fetchDependencies){
        logger.debug("Preparing service: " + serviceId + " v " + versionId + " for InvocationID=" + report.getInvocationId());
        CloudWorkflowServiceLibraryItem item = locateServiceItem(serviceId, versionId);
        if(item!=null){
            report.addMessage(LibraryPreparationReport.ITEM_PRESENT, "Service in library. Checking dependencies");
            if(fetchDependencies){
                if(item.hasLatestVersionDependencies()){
                    report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Checking latest versions");
                    DependencyFetcher fetcher = new DependencyFetcher(item, this, apiLink, report);
                    fetcher.fetchDependencies(callback);
                } else {
                    report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Service has no latest version dependencies");
                    callback.libraryReady(item, report);
                }
            } else {
                report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Not downloading dependencies");
                callback.libraryReady(item, report);
            }

        } else {
            // Start a download
            report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Service will be downloaded");
            downloadManager.startDownload(apiLink, serviceId, versionId, new DownloadCallbackTrigger(callback, apiLink, this, report), report);
        }
    }

    /** Locate an item by library name and version number */
    private CloudWorkflowServiceLibraryItem locateDependencyItem(String libraryName, int versionNumber) {
        Iterator<CloudWorkflowServiceLibraryItem> i = libraryItems.iterator();
        CloudWorkflowServiceLibraryItem library;

        while(i.hasNext()){
            library = i.next();
            if(library.getItemType()==CloudWorkflowServiceLibraryItem.LIBRARY_ITEM && library.matches(libraryName, versionNumber)){
                return library;
            }
        }
        return null;
    }
    
    /** Locate an item by library name and version number */
    private CloudWorkflowServiceLibraryItem locateDependencyItem(String libraryName, String versionId) {
        Iterator<CloudWorkflowServiceLibraryItem> i = libraryItems.iterator();
        CloudWorkflowServiceLibraryItem library;

        while(i.hasNext()){
            library = i.next();
            if(library.getItemType()==CloudWorkflowServiceLibraryItem.LIBRARY_ITEM && library.matchesWithVersionId(versionId)){
                return library;
            }
        }
        return null;
    }    

    /** Locate an item in the library */
    public CloudWorkflowServiceLibraryItem locateServiceItem(String serviceId, String versionId){
        Iterator<CloudWorkflowServiceLibraryItem> i = libraryItems.iterator();
        CloudWorkflowServiceLibraryItem library;

        while(i.hasNext()){
            library = i.next();
            if(library.matches(serviceId, versionId)){
                return library;
            }
        }
        return null;
    }

    /** Get a document using either the workflow invocation cache or the API directly */
    public DocumentRecord getServiceById(String id, API api) throws Exception {
        return cache.getServiceById(id, api);
    }

    /** Get a library by name using either the workflow invocation cache or the API directly */
    public DocumentRecord getLibraryByName(String name, API api) throws Exception {
        return cache.getLibraryByName(name, api);
    }

    /** Get the latest version of a document using either the workflow invocation cache or the API directly */
    public String getLatestDocumentVersionId(DocumentRecord doc, API api) throws Exception {
        return cache.getLatestDocumentVersionId(doc, api);
    }
    
    /** Get a refererence to the information cache */
    public LibraryInformationCache getInformationCache(){
        return cache;
    }

    /** Class to pass notification to a callback that a download has completed */
    private class DownloadCallbackTrigger implements LibraryDownloadListener {
        private LibraryCallback callback;
        private API apiLink;
        private ServiceLibrary library;
        private LibraryPreparationReport report;

        public DownloadCallbackTrigger(LibraryCallback callback, API apiLink, ServiceLibrary library, LibraryPreparationReport report) {
            this.callback = callback;
            this.apiLink = apiLink;
            this.library = library;
            this.report = report;
        }
 
        public synchronized void downloadComplete(final CloudWorkflowServiceLibraryItem item) {
            // Check dependencies
            if(item.getDependencyCount()==0){
                // No dependencies
                libraryItems.add(item);
                notifyLibraryContentsChanged();
                report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Download finished");
                callback.libraryReady(item, report);

            } else {
                // Need to get dependencies
                LibraryCallback cb = new LibraryCallback() {

                    public void libraryReady(CloudWorkflowServiceLibraryItem library, LibraryPreparationReport report) {
                        libraryItems.add(item);
                        notifyLibraryContentsChanged();
                        callback.libraryReady(library, report);
                    }

                    public void libraryPreparationFailed(String message, LibraryPreparationReport report) {
                        callback.libraryPreparationFailed(message, report);
                    }
                };

                report.addMessage(LibraryPreparationReport.INFORMATION_MESSAGE, "Downloading dependencies");
                DependencyFetcher fetcher = new DependencyFetcher(item, library, apiLink, report);
                fetcher.fetchDependencies(cb);
            }
        }

        public synchronized void downloadError(String message) {
            callback.libraryPreparationFailed(message, report);
        }
    }
}

