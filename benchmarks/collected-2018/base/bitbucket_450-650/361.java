// https://searchcode.com/api/result/131107594/

/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.plexiViewer.converter;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackProcessor;

import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.xmlbeans.XmlException;
import org.nrg.pipeline.xmlbeans.xnat.AbstractResource;
import org.nrg.pipeline.xmlbeans.xnat.DicomSeries;
import org.nrg.pipeline.xmlbeans.xnat.ImageResource;
import org.nrg.pipeline.xmlbeans.xnat.ImageScanData;
import org.nrg.pipeline.xmlbeans.xnat.Resource;
import org.nrg.pipeline.xmlbeans.xnat.ResourceCatalog;
import org.nrg.pipeline.xmlbeans.xnat.ResourceSeries;
import org.nrg.plexiViewer.io.IOHelper;
import org.nrg.plexiViewer.io.PlexiFileOpener;
import org.nrg.plexiViewer.lite.io.PlexiFileSaver;
import org.nrg.plexiViewer.lite.io.PlexiImageFile;
import org.nrg.plexiViewer.utils.ImageUtils;
import org.nrg.plexiViewer.utils.PlexiConstants;
import org.nrg.plexiViewer.utils.Transform.BitConverter;
import org.nrg.plexiViewer.utils.Transform.IntensitySetter;
import org.nrg.plexiViewer.utils.Transform.PlexiMontageMaker;
import org.nrg.xdat.bean.ArcProjectBean;
import org.nrg.xdat.bean.XnatAbstractresourceBean;
import org.nrg.xdat.bean.XnatDicomseriesBean;
import org.nrg.xdat.bean.XnatImageresourceBean;
import org.nrg.xdat.bean.XnatImageresourceseriesBean;
import org.nrg.xdat.bean.XnatImagescandataBean;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xdat.bean.XnatResourceBean;
import org.nrg.xdat.bean.XnatResourcecatalogBean;
import org.nrg.xdat.bean.XnatResourceseriesBean;
import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xnattools.service.WebServiceClient;
import org.nrg.xnattools.xml.XMLSearch;
import org.xml.sax.SAXException;


public class WebBasedQCImageCreator {
    String session;
    String xnatId;
    String host;
    String user;
    

    String pwd;
    
    String cachepath;
    String workflowid=null;
    
    String projectId;
    boolean raw = false;
    boolean radiologic = false;
    int exitStatus = 0;

    public WebBasedQCImageCreator(String args[]) {
        for(int i=0; i<args.length; i++){
            if (args[i].equalsIgnoreCase("-project") ) {
                if (i+1 < args.length) {
                    projectId=args[i+1];
                }
            }else  if (args[i].equalsIgnoreCase("-xnatId") ) {
                if (i+1 < args.length) {
                    xnatId=args[i+1];
                }
            }else   if (args[i].equalsIgnoreCase("-session") ) {
                if (i+1 < args.length) {
                    session=args[i+1];
                }
            }  else if (args[i].equalsIgnoreCase("-host") ) {
                if (i+1 < args.length) {
                    host=args[i+1];
                    if (host.endsWith("/"))  {
                    	host = host.substring(0, host.length() - 1);
                    }
                }
            }else if (args[i].equalsIgnoreCase("-u") ) {
                if (i+1 < args.length) {
                    user=args[i+1];
                }
            }else if (args[i].equalsIgnoreCase("-pwd") ) {
                if (i+1 < args.length) {
                    pwd=args[i+1];
                }
            }else if (args[i].equalsIgnoreCase("-raw") ) {
                raw = true;
            }else if (args[i].equalsIgnoreCase("-r") ) {
                radiologic = true; 
	        }else if (args[i].equalsIgnoreCase("-workflowid") ) {
                if (i+1 < args.length) {
                    workflowid=args[i+1];
                }
	        }
        }
        if (session == null || xnatId == null || host == null || user == null || pwd == null) {
            handleError();
        }
    }
    
    public void createQCImages() throws URISyntaxException, SAXException, ServiceException,MalformedURLException, RemoteException, IOException, XmlException {
    	//For the project that the session belongs to, get the ArchiveSpec document to figure out the 
    	//Thumbnail and the cachepath and the lores path
    	String uriString =  "REST/projects/" + projectId + "/archive_spec";
    	WebServiceClient webServiceClient = new WebServiceClient(host, user, pwd);
	    	ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	 webServiceClient.connect(uriString,out);
	    	InputStream inStream =   new ByteArrayInputStream(out.toByteArray());
	    	XDATXMLReader reader = new XDATXMLReader();
	        BaseElement base = reader.parse(inStream);
	        ArcProjectBean arcProject = (ArcProjectBean)base;
	    	cachepath = arcProject.getPaths().getCachepath();
	    	out.close(); inStream.close();
	        checkFolders();
	        if (raw)
	            createQCRawImages();
	        
    }


    private void checkFolders() {
       // checkFolders(tbpath);
        if (!cachepath.endsWith(session)) {
            cachepath += File.separator + session;
        }
        checkFolders(cachepath);
    }
    
    private void checkFolders(String path) {
        if (path == null) {
            handleError();
        }
        File tbFolder = new File(path);
        if (!tbFolder.exists()) {
            try {
                boolean success = tbFolder.mkdirs();
                if (!success) {
                    handleException("checkFolders","Failed to create " + path,null, null);
                }
            }catch(Exception e) {
                handleException("checkFolders","Unable to create thumbnail folder " + path,e, null);
            }
        }
    }
    
    private void handleException(String methodName, String msg, Exception e, String service_session) {
    	if (service_session != null) {
	    	try {
	        	WebServiceClient webServiceClient = new WebServiceClient(host, user, pwd);
	    		webServiceClient.closeServiceSession(service_session);
	    	}catch(Exception e1) {
	    		e.printStackTrace();
	    	}
    	}
        System.out.println(" QCImageCreator." + methodName +" encountered problem " + (e == null?"":e.getMessage()) + " \n MSG: " + msg);
        System.exit(exitStatus);
    }
    
    private void handleError() {
        System.out.println("Insufficient arguments");
        printUsage();
        System.exit(1);
        
    }

    protected void printUsage() {
      System.out.println("QCImageCreator OPTIONS:"); 
      System.out.println("\t\t-session <proejct mr-session id>");
      System.out.println("\t\t-project <proejct that the mr-session belongs to>");
      System.out.println("\t\t-xnatId <xnat mr-session id>");
      System.out.println("\t\t-host <xnat host>");
      System.out.println("\t\t-u <xnat username>");
      System.out.println("\t\t-pwd <xnat password>");
      System.out.println("\t\t-raw <create QC files for raw scans only>"); 
    }
  
    
    private XnatAbstractresourceBean getScanFile(XnatImagesessiondataBean imageession, XnatImagescandataBean scan) {
    	XnatAbstractresourceBean file = null;
    	String scanType = scan.getType();
    	String rawScanContentCode = scanType + "_RAW";
    	List files = scan.getFile();
    	if (files.size() > 0) {
	    	if (files.size() == 1) {
	        	file = (XnatAbstractresourceBean)files.get(0);
	        }else  {
	        	file = getFileByContent(scan, rawScanContentCode);
	        	if (file == null)
	        		file = getFileByContent(scan, "RAW");
	        }
    	}
    	return file;
    }
    
    private AbstractResource getFileByContent( ImageScanData mrScan, String content) {
    	AbstractResource rtn = null;
    	try {
				for (int i = 0; i < mrScan.sizeOfFileArray(); i++) {
					AbstractResource f = mrScan.getFileArray(i);
				    if (f.schemaType().getName().getLocalPart().equals(Resource.type.getName().getLocalPart())) {
				    	Resource resource = (Resource)f.changeType(Resource.type);
				    	if (resource.getContent().equals(content)) {
				    		rtn = f;
				    	}
				    }else if (f.schemaType().getName().getLocalPart().equals(ImageResource.type.getName().getLocalPart())) {
				    	ImageResource imageResource = (ImageResource)f.changeType(ImageResource.type);
				    	if (imageResource.getContent().equals(content)) {
				    		rtn = f;
				    	}
				    }else if (f.schemaType().getName().getLocalPart().equals(ResourceSeries.type.getName().getLocalPart())) {
				    	ResourceSeries resourceSeries = (ResourceSeries)f.changeType(ResourceSeries.type);
				    	if (resourceSeries.getContent().equals(content)) {
				    		rtn = f;
				    	}
				    }else if (f.schemaType().getName().getLocalPart().equals(DicomSeries.type.getName().getLocalPart())) {
				    	DicomSeries resourceSeries = (DicomSeries)f.changeType(DicomSeries.type);
				    	if (resourceSeries.getContent().equals(content)) {
				    		rtn = f;
				    	}
				    }else if (f.schemaType().getName().getLocalPart().equals(ResourceCatalog.type.getName().getLocalPart())) {
				    	ResourceCatalog resourceCat = (ResourceCatalog)f.changeType(ResourceCatalog.type);
				    	if (resourceCat.getContent().equals(content)) {
				    		rtn = f;
				    	}
				    }
					
				}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	return rtn;
    }
    
    
    private XnatAbstractresourceBean getFileByContent( XnatImagescandataBean scan, String content) {
    	XnatAbstractresourceBean rtn = null;
    	try {
    		List files = scan.getFile();
				for (int i = 0; i < files.size(); i++) {
					XnatAbstractresourceBean f = (XnatAbstractresourceBean)files.get(i);
				    if (f instanceof XnatResourceBean) {
				    	XnatResourceBean resource = (XnatResourceBean)f;
				    	if (resource.getContent().equals(content)) {
				    		rtn = f;
				    		break;
				    	}
				    }else if (f instanceof XnatImageresourceBean) {
				    	XnatImageresourceBean imageResource = (XnatImageresourceBean)f;
				    	if (imageResource.getContent().equals(content)) {
				    		rtn = f;
				    		break;
				    	}
				    }else if (f instanceof XnatResourceseriesBean) {
				    	XnatResourceseriesBean resourceSeries = (XnatResourceseriesBean)f;
				    	if (resourceSeries.getContent().equals(content)) {
				    		rtn = f;
				    		break;
				    	}
				    }else if (f instanceof XnatDicomseriesBean) {
				    	XnatDicomseriesBean resourceSeries = (XnatDicomseriesBean)f;
				    	if (resourceSeries.getContent().equals(content)) {
				    		rtn = f;
				    		break;
				    	}
				    }else if (f instanceof XnatResourcecatalogBean) {
				    	 XnatResourcecatalogBean resourceCat = ( XnatResourcecatalogBean)f;
				    	if (resourceCat.getContent().equals(content)) {
				    		rtn = f;
				    		break;
				    	}
				    }
					
				}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	return rtn;
    }
    
    protected void createQCRawImages() {
       WebServiceClient webserviceClient = null;
        String service_session = null;
        try {
            webserviceClient = new WebServiceClient(host, user, pwd);
            service_session = webserviceClient.createServiceSession();
            BaseElement baseElement = new XMLSearch(host,user,pwd).getBeanFromHost(xnatId, true);
        	XnatImagesessiondataBean  imageSessionBean  = (XnatImagesessiondataBean) baseElement;
            List scans  = imageSessionBean.getScans_scan();
            if (scans.size() > 0) {
                for (int i = 0; i < scans.size(); i++) {
                    XnatImagescandataBean scan = (XnatImagescandataBean)scans.get(i);
                    service_session = webserviceClient.refreshServiceSession(service_session);
                    List files = scan.getFile();
                        if (files.size() > 0) {
                            XnatAbstractresourceBean file = getScanFile(imageSessionBean, scan);
                            if (file == null) continue;
                            PlexiImageFile pf = new PlexiImageFile();
                            try {
                                pf = getPlexiImageFile(file, scan.getId());
                                if (file instanceof XnatImageresourceseriesBean) {
                                    XnatImageresourceseriesBean imageResourceSeries = (XnatImageresourceseriesBean)file;
                                    if (imageResourceSeries.getFormat().equalsIgnoreCase("IMA")) {
                                	pf.setPath(cachepath );
                                	 pf.setName(pf.getName()+".4dfp.img");
                                     pf.setXsiType(PlexiConstants.PLEXI_IMAGERESOURCE);
                                     pf.setFormat("IFH");
                                     try {
                                         if (pf.getPath().endsWith("/"))
                                             pf.setURIAsString(pf.getPath() + pf.getName() );
                                          else
                                              pf.setURIAsString(pf.getPath() + "/" + pf.getName());
                                     }catch(Exception e) {
                                         System.out.println("URI Exception " + pf.getPath() + " " + pf.getName());
                                     }
                                    }
                          
                                     }
                            }catch(Exception e) {
                                handleException("createQCRawImages","Unable to open imageresource images",e, service_session);
                            }
                            try {
                                ImagePlus baseimage = PlexiFileOpener.openBaseFile(pf, radiologic);
                                if (baseimage == null) {
                                    throw new Exception("Image File is null for scan " + scan.getId());
                                }

                                createThumbnail(baseimage, scan, service_session);
                                if (baseimage!=null) 
                                    baseimage.flush();
                                baseimage=null;
                            }catch(Exception e) {
                                System.out.println("QCImageCreator failed for session " + session + " scan " + scan.getId() + " CAUSE " + e.getMessage());
                                e.printStackTrace();
                                exitStatus = 2;
                            }
                        }
                    }
                }
        }catch(Exception e) {
            System.out.println("WebBasedQCImageCreator failed for session " + session + " CAUSE " + e.getMessage());
            e.printStackTrace();
            exitStatus = 1;
            handleException("CreateQCRawImages","",e, service_session == null ? "" : service_session);
        }finally {
            if (webserviceClient != null) {
                try {
                    webserviceClient.closeServiceSession(service_session);
                }catch(Exception e) {
                    e.printStackTrace();
                    exitStatus = 1;
                }
            }
        }
    }
    
     
    private ImagePlus getSnapshot(ImagePlus baseimage, boolean montage) {
    	ImagePlus rtn = null;
    

    	if (montage)
    		rtn =  createMontage(baseimage);
    	else {
            if (baseimage != null) {
                int sliceNo = 5;
                if (baseimage.getStackSize() ==1)  sliceNo = 1;
                else if (baseimage.getStackSize() < sliceNo) sliceNo = 2;
                baseimage.setSlice(sliceNo);
                baseimage.updateImage();
                baseimage.getProcessor().setColor(Color.WHITE);
                baseimage.getProcessor().setFont(new Font("Serif", Font.BOLD, 10));
                baseimage.getProcessor().drawString("Frame: " +sliceNo,baseimage.getWidth()-50,baseimage.getHeight()-5);
                baseimage.updateImage();
                rtn = baseimage;
            }
    	}
    	return rtn;
    }
    
    private void createThumbnail(ImagePlus baseimage, XnatImagescandataBean scan, String uri, String service_session) throws Exception {
    	ImagePlus snapshot = null;
    	if (scan.getType()!= null ) {
    		 if (!scan.getType().equalsIgnoreCase("BOLD")) {
    			 snapshot = getSnapshot(baseimage, true);
	         }else {
	        	 snapshot = getSnapshot(baseimage, false);
	         }
	     }else {
	    	 snapshot = getSnapshot(baseimage, true);
	     }

    	if (snapshot != null) {

    		BitConverter converter = new BitConverter();
             converter.convertTo8BitColor(snapshot);
            
 	    	String tbfilenameroot = xnatId + "_" + scan.getId()  +"_qc";
	    	PlexiFileSaver fs =  new PlexiFileSaver(snapshot.getImage());
	    	String fileName = tbfilenameroot+".gif";
	    	String filePath  = cachepath + File.separator + fileName; 
	    	boolean saved = fs.saveImageAsGif(filePath);
	    	if (!saved) throw new Exception ("Couldnt save file snapshot for scan " + scan.getId() + " at the location " + filePath);
	    	File targetFile = new File(filePath);
	    	postFile(uri, targetFile, service_session, "SNAPSHOT", "ORIGINAL");
	    	deleteFile(targetFile);
	    	filePath = generateScaledDownImage(snapshot, tbfilenameroot);
	    	if (filePath != null) {
	        	targetFile = new File(filePath);
	        	postFile(uri, targetFile, service_session, "SNAPSHOT", "THUMBNAIL");
	        	deleteFile(targetFile);
	    	}
	    	snapshot.flush();
    	}
    }
    
    private void deleteFile(File f) {
    	if (f!= null && f.exists())
    		f.delete();
    }
    
    private String generateScaledDownImage(ImagePlus baseimage, String tbfilenameroot) {
    	String rtn = null;
        StackProcessor tbproc = new StackProcessor(baseimage.getStack(), baseimage.getProcessor());
        ImageStack tb = tbproc.resize((int)baseimage.getWidth()/2,(int)baseimage.getHeight()/2);
        baseimage.setStack("",tb);    
        PlexiFileSaver fs  =  new PlexiFileSaver(baseimage.getImage());
        String filePath = cachepath + File.separator + tbfilenameroot+"_t.gif";
        boolean saved = fs.saveImageAsGif(filePath);
        if (saved)
        	rtn = cachepath + File.separator + tbfilenameroot+"_t.gif";
        return rtn;
    }
    
    private void postFile(String uri, File file, String service_session, String label, String content) throws  Exception{
		PostMethod filePost = new PostMethod(uri);
		String queryStringPrefix = "";
		if (workflowid != null) {
			queryStringPrefix = "event_id="+workflowid+"&";
		}
		filePost.setQueryString(queryStringPrefix + "overwrite=true&format=GIF&content=" + content);
    	try {
        	filePost.setRequestHeader("Cookie", "JSESSIONID="+service_session);
        	Part[] parts = { new FilePart(file.getName(), file) };
            filePost.setRequestEntity( new MultipartRequestEntity(parts, filePost.getParams()));
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            int status = client.executeMethod(filePost);
            if (status != HttpStatus.SC_OK) {
            	throw new Exception("Couldnt post file to " + uri);
            }
    	 }finally {
    		  filePost.releaseConnection();
    	 }
    }
    
    public void createThumbnail(ImagePlus baseimage, XnatImagescandataBean scan, String service_session) throws Exception {
		XnatResourcecatalogBean rscCatalog = new XnatResourcecatalogBean();
		rscCatalog.setLabel("SNAPSHOTS");
		rscCatalog.setContent("SNAPSHOTS");
		rscCatalog.setFormat("GIF");
		StringWriter stringWriter = new StringWriter();
		rscCatalog.toXML(stringWriter, true);
		
		String strURL = host + "/REST/experiments/" + xnatId + "/scans/" + scan.getId() + "/resources";

		PostMethod post = new PostMethod(strURL);
        post.setRequestHeader("Cookie", "JSESSIONID="+service_session);

        RequestEntity entity = new StringRequestEntity(stringWriter.toString(), "text/xml;charset=ISO-8859-1", null);
        post.setRequestEntity(entity);

        HttpClient httpclient = new HttpClient();
        String filesURI = "";
        // Execute request
        try {
        	int result = httpclient.executeMethod(post);
            System.out.println("Response status code: " + result);
        	if (result == HttpStatus.SC_OK) {
                filesURI = post.getResponseBodyAsString();
                createThumbnail(baseimage, scan, filesURI + "/files", service_session);
        	}else {
        		throw new Exception("Couldnt post to " + strURL +  " got a response " + filesURI);
        	}
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    
    private PlexiImageFile getPlexiImageFile(XnatAbstractresourceBean file,  String scanId) throws Exception{
        PlexiImageFile pf = new PlexiImageFile();
        if (file instanceof XnatImageresourceBean) {
            pf = IOHelper.getPlexiImageFileFromImageResource(file);
        }else if (file instanceof XnatImageresourceseriesBean) {
            pf =IOHelper.getPlexiImageFileFromImageResourceSeries(file, cachepath);
        }else if (file instanceof XnatDicomseriesBean) {
            pf = IOHelper.getPlexiImageFileFromDicomSeries(file, cachepath, session + "_" + scanId);
        }else if (file instanceof XnatResourcecatalogBean) {
            pf = IOHelper.getPlexiImageFileFromResourceCatalog(file, cachepath, session + "_" + scanId);
        }
        return pf;
    }
    
    


    private ImagePlus createMontage(ImagePlus image) {
        PlexiMontageMaker mm = new PlexiMontageMaker();
        int columns = 1;
        int rows = 1;
        if (image.getStackSize() == 1) {
        	columns = 1; rows =1;
        }else if (image.getStackSize() == 2){
        	rows = 1; columns = 2;
        }else if (image.getStackSize() == 3){
        	rows = 1; columns = 3;
        }else { // extract the nearest square
    		for(; columns*columns <= image.getStackSize() ; columns++ ) ;
    		columns--;
    		rows = columns;
        }
        //If there are too many frames then we reduce the grid size
        if (columns > 7)  {
        	columns = 7; rows = columns;
        }
        Hashtable attribs = ImageUtils.getSliceIncrement(image, columns*rows);

        
        int startslice = ((Integer)attribs.get("startslice")).intValue();
        int endslice = ((Integer)attribs.get("endslice")).intValue();
        int increment = ((Integer)attribs.get("increment")).intValue();
        IntensitySetter is = new IntensitySetter(image, true);
        is.autoAdjust(image, image.getProcessor());
       
        image= mm.makeMontage(image,columns,rows,0.5,startslice,endslice,increment,true,false);
        image.getProcessor().resetMinAndMax();
        return image;
    }
    
    /**
     * @return Returns the exitStatus.
     */
    public int getExitStatus() {
        return exitStatus;
    } 
    
    public static void main(String args[]) {
       // System.setProperty("java.awt.headless","true");
    	try {
	        WebBasedQCImageCreator qc = new WebBasedQCImageCreator(args);
	        qc.createQCImages();
	        System.out.println("All done");
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
        System.exit(0);
    }

   
}

