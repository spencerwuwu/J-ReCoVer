// https://searchcode.com/api/result/13934985/

package com.vzaar;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

import com.vzaar.transport.VzaarTransport;
import com.vzaar.transport.VzaarTransportFactory;
import com.vzaar.transport.VzaarTransportResponse;

/**
 * vzaar API access. This is the main class for accessing vzaar through
 * their RESTful API.
 *
 * Some of the methods require authentication which is done using OAuth.
 * If you need to use these methods then ensure you create a Vzaar
 * object with your OAuth credentials. The OAuth token is typically
 * your username and the OAuth secret can be retrieved from your 
 * account page on <a target="_blank" href="http://vzaar.com">vzaar.com</a>.
 *
 * The default implementation uses commons HTTPClient 3.1 but it is
 * possible to use other HTTP libraries by implementing the 
 * {@link VzaarTransport} interface and registering as the default
 * factory in {@link VzaarTransportFactory}.
 *
 * @author Marc G Smith
 */
public class Vzaar 
{
	///////////////////////////////////////////////////////////////////////////
	// Public Constants ///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	/**
	 * The live vzaar API URL ("https://vzaar.com/"). This is the default URL.
	 */
	public final static String URL_LIVE = "https://vzaar.com/";
	
	
	/**
	 * The vzaar sandbox API URL ("https://sandbox.vzaar.com/"). 
	 */
	public final static String URL_SANDBOX = "https://sandbox.vzaar.com/";
	
	/**
	 * Video profile type enumerations. The video profile represents the
	 * size of the video to be encoded.
	 */
	public enum Profile {
		Small(1),
		Medium(2),
		Large(3),
		HighDefinition(4),
		Original(5);
		
		private final int value;
		private Profile(int value) { this.value = value; }
		public String toString() {return String.valueOf(value);}
		public static Profile toEnum(int value) {
			for(Profile en: values()) {
				if(en.value == value) return en;
			}
			throw new IllegalArgumentException(String.valueOf(value));
		};
	}

	///////////////////////////////////////////////////////////////////////////
	// Private Members ////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	private final static int HTTP_OK = 200;
	private String url;
	private boolean trustedOnly = true;
	private VzaarTransport transport;
	
	///////////////////////////////////////////////////////////////////////////
	// Public Methods /////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Default constructor. This creates a vzaar object to the live site
	 * without any authentication.
	 */
	public Vzaar() {
		this(URL_LIVE);
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor that allows a different destination to be specified such
	 * as the API sandbox.
	 * 
	 * @param url The API URL such as http://sandbox.vzaar.com/api/
	 */
	public Vzaar(String url) {
		this(url, null, null);
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Authenticated constructor. This creates a vzaar object to the live site
	 * with 2 party authentication.
	 * 
	 * @param oauthToken Your vzaar login name
	 * @param oauthSecret Your generated application token 
	 */
	public Vzaar( String oauthToken, String oauthSecret) {
		this(URL_LIVE, oauthToken, oauthSecret);
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Authenticated constructor. This creates a vzaar object to the live site
	 * with 2 party authentication and allows a different destination to be 
	 * specified such as the API sandbox.
	 * 
	 * @param url The API URL such as http://sandbox.vzaar.com/api/
	 * @param oauthToken Your vzaar login name
	 * @param oauthSecret Your generated application token 
	 */
	public Vzaar(String url, String oauthToken, String oauthSecret) {
		this.url = url;
		this.transport = VzaarTransportFactory.createDefaultTransport();
		this.transport.setUrl(url);
		this.transport.setOAuthTokens(oauthToken, oauthSecret);
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Get the API URL being used.
	 * 
	 * @return the API URL
	 */
	public String getUrl() {
		return url;
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Set the output stream for debug output if required. 
	 * 
	 * @param out the output stream for debug.
	 */
	public void setDebugStream(OutputStream out) {
		transport.setDebugStream(out);
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Set the connection factory to use SSLv3 only, i.e. no TLS. This
	 * is for an issue with production servers where SSL is failing. 
	 */
	public void setSsl3Only() {
		this.transport.setSsl3Only();
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Is the upload method accepting only trusted formats. If this is set
	 * to true then any formats that are not on the trusted list will throw
	 * an exception up front on the uploadVideo() method. Otherwise all
	 * accepted formats will be allowed.
	 * 
	 * @param trustedOnly true if only trusted formats should be accepted,
	 * 	false if untrusted as well as trusted should be accepted.
	 */
	public void acceptTrustedFormatsOnly(boolean trustedOnly) {
		this.trustedOnly = trustedOnly;
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Is the upload method accepting only trusted formats. If this is set
	 * to true then any formats that are not on the trusted list will throw
	 * an exception up front on the uploadVideo() method. Otherwise all
	 * accepted formats will be allowed.
	 * 
	 * @return true if only trusted formats are being accepted
	 */
	public boolean isAcceptingTrustedFormatsOnly() {
		return trustedOnly;
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Get a user's public details along with it's relevant metadata. 
	 * 
	 * Note: The user name must be used and not the email address.
	 * 
	 * @param username the vzaar login for that user.
	 * @return the user information
	 */
	public User getUserDetails(String username) 
		throws VzaarException 
	{
		String uri = username + ".xml";
		VzaarTransportResponse response = 
			transport.sendGetRequest(uri, null);
		
		if(response.getStatusCode() != HTTP_OK) {
			throw new VzaarException(getError(response));
		}
			
		Document document = XmlHelper.parseXml(response.getResponse());
		return new User(
			XmlHelper.getDouble(document, "version"),
			XmlHelper.getValue(document, "author_name"),
			XmlHelper.getInteger(document, "author_id"),
			XmlHelper.getValue(document, "author_url"),
			XmlHelper.getInteger(document, "author_account"),
			XmlHelper.getValue(document, "created_at"),
			XmlHelper.getInteger(document, "video_count"),
			XmlHelper.getInteger(document, "play_count"));
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Get the details and rights for an account type.
	 * 
	 * @param accountType the account type to fetch the details for
	 * @return the account type details 
	 */
	public AccountType getAccountType(int accountType) 
		throws VzaarException 
	{
		String uri = "accounts/" + String.valueOf(accountType) + ".xml";
		
		VzaarTransportResponse response = 
			transport.sendGetRequest(uri, null);
		
		if(response.getStatusCode() != HTTP_OK) {
			throw new VzaarException(getError(response));
		}
			
		Document document = XmlHelper.parseXml(response.getResponse());
		return new AccountType(
			XmlHelper.getDouble(document, "version"),
			XmlHelper.getInteger(document, "account_id"),
			XmlHelper.getValue(document, "title"),
			XmlHelper.getInteger(document, "monthly"),
			XmlHelper.getValue(document, "currency"),
			XmlHelper.getLong(document, "bandwidth"),
			XmlHelper.getBoolean(document, "borderless"),
			XmlHelper.getBoolean(document, "searchEnhancer"));
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Get a list of the user's active videos along with it's relevant 
	 * metadata.
	 * 
	 * @param request the video list request parameters.
	 * @return a list of videos for the request object 
	 */
	public List<Video> getVideoList(VideoListRequest request)
		throws VzaarException
	{
		String username = request.getUsername();
		if(username == null) {
			throw new VzaarException(
				"Required parameter 'username' is missing from request");
		}
		
		String uri = username + "/videos.xml";
		
		VzaarTransportResponse response = 
			transport.sendGetRequest(uri, request.getParameters());
		
		if(response.getStatusCode() != HTTP_OK) {
			throw new VzaarException(getError(response));
		}
		
		Integer count = request.getCount();
		List<Video> videoList = 
			new ArrayList<Video>(count != null ? count : 20);
		
		Document document = XmlHelper.parseXml(response.getResponse());
		NodeIterator iterator = XmlHelper.getNodes(document, "video");
		Node current = null;
		while((current = iterator.nextNode()) != null) {
			videoList.add(new Video(
				XmlHelper.getDouble(current, "version"),
				XmlHelper.getInteger(current, "id"),
				XmlHelper.getValue(current, "title"),
				XmlHelper.getValue(current, "description"),
				XmlHelper.getValue(current, "created_at"),
				XmlHelper.getValue(current, "url"),
				XmlHelper.getValue(current, "thumbnail_url"),
				XmlHelper.getInteger(current, "play_count"),
				XmlHelper.getValue(current, "author_name"),
				XmlHelper.getValue(current, "author_url"),
				XmlHelper.getInteger(current, "author_account"),
				XmlHelper.getInteger(current, "video_count"),
				XmlHelper.getDouble(current, "duration"),
				XmlHelper.getInteger(current, "width"),
				XmlHelper.getInteger(current, "height")));
		}
		
		return videoList;
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Get a video details including embed code.
	 * 
	 * @param request the video detail request parameters
	 * @return the video details 
	 */
	public VideoDetails getVideoDetails(VideoDetailsRequest request)
		throws VzaarException
	{
		int videoId = request.getVideoId();
		if(videoId <= 0) {
			throw new VzaarException(
				"Required video id is missing from request");
		}
		
		String uri = "videos/" + videoId + ".xml";

		VzaarTransportResponse response = 
			transport.sendGetRequest(uri, request.getParameters());

		if(response.getStatusCode() != HTTP_OK) {
			throw new VzaarException(getError(response));
		}
		
		Document document = XmlHelper.parseXml(response.getResponse());
		
		return new VideoDetails(
				XmlHelper.getValue(document, "type"),
				XmlHelper.getDouble(document, "version"),
				XmlHelper.getValue(document, "title"),
				XmlHelper.getValue(document, "description"),
				XmlHelper.getValue(document, "author_name"),
				XmlHelper.getValue(document, "author_url"),
				XmlHelper.getInteger(document, "author_account"),
				XmlHelper.getValue(document, "provider_name"),
				XmlHelper.getValue(document, "provider_url"),
				XmlHelper.getValue(document, "thumbnail_url"),
				XmlHelper.getInteger(document, "thumbnail_width"),
				XmlHelper.getInteger(document, "thumbnail_height"),
				XmlHelper.getValue(document, "framegrab_url"),
				XmlHelper.getInteger(document, "framegrab_width"),
				XmlHelper.getInteger(document, "framegrab_height"),
				XmlHelper.getValue(document, "html"),
				XmlHelper.getInteger(document, "width"),
				XmlHelper.getInteger(document, "height"),
				XmlHelper.getBoolean(document, "borderless"),
				XmlHelper.getDouble(document, "duration"),
				XmlHelper.getInteger(document, "video_status_id"));
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Test method for authentication.
	 * 
	 * @return the user name of the authenticated user
	 */
	public String whoAmI()
		throws VzaarException
	{
		String uri = "test/whoami";
	
		VzaarTransportResponse response = 
			transport.sendGetRequest(uri, null);
	
		if(response.getStatusCode() != HTTP_OK) {
			throw new VzaarException(getError(response));
		}
		
		Document document = XmlHelper.parseXml(response.getResponse());
		return XmlHelper.getValue(document, "login");
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Upload a video to vzaar. This is tripart request which fetches the 
	 * upload signature, uploads to S3 and then requests vzaar to process.
	 * 
	 * @param title the video title
	 * @param description the video description
	 * @param profile the size for the video to be encoded
	 * @param file the file to be uploaded
	 */
	public void uploadVideo(
		String title, String description, Profile profile, File file)
		throws VzaarException
	{
		uploadVideo(title, description, profile, file, null);
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Upload a video to vzaar. This is tri-part request which fetches the 
	 * upload signature, uploads to S3 and then requests vzaar to process.
	 * 
	 * This method is a convenience method to reduce the upload process to 
	 * a single call rather than having to make the three separate back end
	 * calls required to complete the transaction. 
	 * 
	 * The callback object is used to report progress of the upload.
	 * 
	 * @param title the video title
	 * @param description the video description
	 * @param profile the size for the video to be encoded
	 * @param file the file to be uploaded
	 * @param callback the progress callback object
	 * @return the video number for the video
	 */
	public int uploadVideo(
		String title, String description, Profile profile, File file,
		UploadProgressCallback callback)
		throws VzaarException
	{
		try {
			if(trustedOnly) {
				if(!AcceptedFileExtension.isTrustedFormat(file.getName())) {
					throw new VzaarException(
						"Video is not trusted and tested format");
				}
			}
			else {
				if(!AcceptedFileExtension.isAcceptedFormat(file.getName())) {
					throw new VzaarException(
						"Video is not of an accepted format");
				}
			}
			
			UploadSignature signature = getUploadSignature();
			upload(signature, file, callback);				
			int videoId =  processVideo(
				signature.getGuid(), title, description, profile);
			if(callback != null) callback.complete(file, videoId);
			return videoId;
		}
		catch(VzaarException ve) {
			if(callback != null) callback.error(file, ve.getMessage());
			throw ve;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Edit a video title and description.
	 * 
	 * @param videoId the video number for the video
	 * @param title the video title
	 * @param description the video description
	 */
	public void editVideo(int videoId, String title, String description)
		throws VzaarException
	{
		String uri = "videos/" + videoId + ".xml";

		StringBuffer xml = new StringBuffer();
	    Formatter formatter = new Formatter(xml);
	    formatter.format(
	    	XmlHelper.EDIT_VIDEO, 
	    	XmlEncoder.encode(title), 
	    	XmlEncoder.encode(description));
		
		VzaarTransportResponse response = 
			transport.sendPostXmlRequest(uri, xml.toString());
		
		if(response.getStatusCode() / 200 != 1) {
			throw new VzaarException(getError(response));
		}
		else {
			XmlHelper.parseXml(response.getResponse());
		}
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Delete a video from a users account.
	 * 
	 * @param videoId the video number for the video
	 */
	public void deleteVideo(int videoId)
		throws VzaarException
	{
		String uri = "videos/" + videoId + ".xml";
		
		VzaarTransportResponse response = 
			transport.sendPostXmlRequest(uri, XmlHelper.DELETE_VIDEO);
		
		if(response.getStatusCode() / 200 != 1) {
			throw new VzaarException(getError(response));
		}
		else {
			XmlHelper.parseXml(response.getResponse());
		}
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Process the video after it has been uploaded.
	 * 
	 * @param guid the guid to operate on
	 * @param title the title for the video
	 * @param description  the description for the video
	 * @param profile the size for the video to be encoded in. If not 
	 * 	specified, this will use the vzaar default or the user default (if set)
	 * @return the video number for the video
	 */
	public int processVideo(
		String guid, String title, String description, Profile profile)
		throws VzaarException
	{
		String uri = "videos";
		
		StringBuffer xml = new StringBuffer();
	    Formatter formatter = new Formatter(xml);
	    formatter.format(
	    	XmlHelper.PROCESS_VIDEO, 
	    	guid, 
	    	XmlEncoder.encode(title), 
	    	XmlEncoder.encode(description), 
	    	profile.toString());
		
		VzaarTransportResponse response = 
			transport.sendPostXmlRequest(uri, xml.toString());
		
		if(response.getStatusCode() / 200 != 1) {
			throw new VzaarException(getError(response));
		}

		Document document = XmlHelper.parseXml(response.getResponse());
		return Integer.parseInt(XmlHelper.getValue(document, "video"));
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Get a global uid for vzaar and signature to upload to S3.
	 * 
	 * @return the upload signature details.
	 */
	public UploadSignature getUploadSignature()
		throws VzaarException
	{
		String uri = "videos/signature";
		
		VzaarTransportResponse response = 
			transport.sendGetRequest(uri, null);
	
		if(response.getStatusCode() != HTTP_OK) {
			throw new VzaarException(getError(response));
		}
		
		Document document = XmlHelper.parseXml(response.getResponse());
		return new UploadSignature(
				XmlHelper.getValue(document, "guid"),
				XmlHelper.getValue(document, "key"),
				XmlHelper.getBoolean(document, "https"),
				XmlHelper.getValue(document, "acl"),
				XmlHelper.getValue(document, "bucket"),
				XmlHelper.getValue(document, "policy"),
				XmlHelper.getValue(document, "expirationdate"),
				XmlHelper.getValue(document, "accesskeyid"),
				XmlHelper.getValue(document, "signature"));
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Upload a file to S3. The upload signature must be fetched for each new
	 * video.
	 * 
	 * @param signature the signature returned from getUploadSignature()
	 * @param file the file to upload
	 * @param callback the progress callback or null if not required
	 */
	public void upload(
		UploadSignature signature, File file, 
		UploadProgressCallback callback)
		throws VzaarException
	{
		String url = "https://" + signature.getBucket() + ".s3.amazonaws.com/";
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("bucket", signature.getBucket());
		parameters.put("key", signature.getKey());
		parameters.put("AWSAccessKeyId", signature.getAccessKeyId());
		parameters.put("acl", signature.getAcl());
		parameters.put("policy", signature.getPolicy());
		parameters.put("signature", signature.getSignature());
		parameters.put("success_action_status", "201");
		
		VzaarTransportResponse response = 
			transport.uploadToS3(url, parameters, file, callback);
		
		if(response.getStatusCode() / 200 != 1) 
		{
			throw new VzaarException(
				"Upload to Amazon S3 returned the following error: " +
				"'" + getError(response) + "'");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// Private Methods ////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Extract the error string from the response. It will first check for 
	 * an error tag if there is an xml reponse, otherwise it will return
	 * the HTTP error status line. 
	 */
	private String getError(VzaarTransportResponse response) 
	{
		try {
			Document document = XmlHelper.parseXml(response.getResponse());
			String error = XmlHelper.getValue(document, "error");
			if(error != null) {
				return error;
			}

			error = XmlHelper.getValue(document, "Message");
			if(error != null) {
				return error;
			}
		}
		catch(VzaarException e) {
		}
		
		return response.getStatusLine();
	}

	///////////////////////////////////////////////////////////////////////////
}

