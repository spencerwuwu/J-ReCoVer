// https://searchcode.com/api/result/13096444/

/*
 * Created on May 25, 2004
 *
 * Paros and its related class files.
 * 
 * Paros is an HTTP/HTTPS proxy for assessing web application security.
 * Copyright (C) 2003-2004 Chinotec Technologies Company
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Clarified Artistic License
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Clarified Artistic License for more details.
 * 
 * You should have received a copy of the Clarified Artistic License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.parosproxy.paros.core.proxy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.parosproxy.paros.db.RecordHistory;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.ConnectionParam;
import org.parosproxy.paros.network.HttpBody;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpInputStream;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpOutputStream;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.parosproxy.paros.network.HttpSender;
import org.parosproxy.paros.network.HttpUtil;


class ProxyThread implements Runnable {

	private static final String	CONNECT_HTTP_200 = "HTTP/1.1 200 Connection established\r\nProxy-connection: Keep-alive\r\n\r\n";
    
	private static Log log = LogFactory.getLog(ProxyThread.class);
    
	// change httpSender to static to be shared among proxies to reuse keep-alive connections

	protected ProxyServer parentServer = null;
	protected ProxyParam proxyParam = null;
	protected ConnectionParam connectionParam = null;
	protected Thread thread = null;
	protected Socket inSocket	= null;
	protected Socket outSocket = null;
	protected HttpInputStream httpIn = null;
	protected HttpOutputStream httpOut = null;
	protected ProxyThread originProcess = this;
	
	private HttpSender 		httpSender = null;
	private Object semaphore = this;
	private static Object semaphoreSingleton = new Object();
    
    private static Vector<Thread> proxyThreadList = new Vector<Thread>();
    
	ProxyThread(ProxyServer server, Socket socket) {

		inSocket = socket;
    	try {
			inSocket.setTcpNoDelay(true);
    		inSocket.setSoTimeout(60000);
		} catch (SocketException e) {
			// ZAP: Log exceptions
			log.warn(e.getMessage(), e);
		}

		thread = new Thread(this);
		thread.setDaemon(true);
		thread.setPriority(Thread.NORM_PRIORITY-1);
		parentServer = server;
		proxyParam = parentServer.getProxyParam();
		connectionParam = parentServer.getConnectionParam();
		
	}

	public void start() {
		thread.start();
        
	}

	private void beginSSL() throws IOException {

        boolean isSecure = true;
        HttpRequestHeader firstHeader = null;

        inSocket = HttpSender.getSSLConnector().createTunnelServerSocket(inSocket);
        
        httpIn = new HttpInputStream(inSocket);
        httpOut = new HttpOutputStream(inSocket.getOutputStream());
        
        firstHeader = httpIn.readRequestHeader(isSecure);
        processHttp(firstHeader, isSecure);
    }
	
	public void run() {
        proxyThreadList.add(thread);
		boolean isSecure = this instanceof ProxyThreadSSL;
		HttpRequestHeader firstHeader = null;
		
		try {
			httpIn = new HttpInputStream(inSocket);
			httpOut = new HttpOutputStream(inSocket.getOutputStream());
			
			firstHeader = httpIn.readRequestHeader(isSecure);
            
			if (firstHeader.getMethod().equalsIgnoreCase(HttpRequestHeader.CONNECT)) {
				httpOut.write(CONNECT_HTTP_200);
                httpOut.flush();
				
                beginSSL();
                //processForwardPort();
				
			} else {
				processHttp(firstHeader, isSecure);
			}
		} catch (IOException e) {
		    log.warn(e.getMessage(), e);

		} finally {
            proxyThreadList.remove(thread);
			disconnect();
		}
	}
	
	protected void processHttp(HttpRequestHeader requestHeader, boolean isSecure) throws IOException {

		HttpBody reqBody = null;
		boolean isFirstRequest = true;
		HttpMessage msg = null;
        
        if (isRecursive(requestHeader)) {
            throw new IOException("Recursive request to proxy itself stopped.");
        }
        
        // reduce socket timeout after first read
        inSocket.setSoTimeout(2500);

		do {

			if (isFirstRequest) {
				isFirstRequest = false;
			} else {
			    try {
			        requestHeader = httpIn.readRequestHeader(isSecure);

			    } catch (SocketTimeoutException e) {
			    	// ZAP: Log the exception
		        	log.error(e.getMessage(), e);
			        return;
			    }
			}

			msg = new HttpMessage();
			msg.setRequestHeader(requestHeader);
			
			if (msg.getRequestHeader().getContentLength() > 0) {
				reqBody		= httpIn.readBody(requestHeader);
				msg.setRequestBody(reqBody);
			}
            
			modifyHeader(msg);
            
            if (isProcessCache(msg)) {
                continue;
            }
          
//            System.out.println("send required: " + msg.getRequestHeader().getURI().toString());
            
			if (parentServer.isSerialize()) {
			    semaphore = semaphoreSingleton;
			} else {
			    semaphore = this;
			}
			
			synchronized (semaphore) {
			    
			    notifyListenerRequestSend(msg);
			    
			    
			    try {

//					bug occur where response cannot be processed by various listener
//			        first so streaming feature was disabled		        
//					getHttpSender().sendAndReceive(msg, httpOut, buffer);

					getHttpSender().sendAndReceive(msg);
			        notifyListenerResponseReceive(msg);
			        
			        // write out response header and body
			        httpOut.write(msg.getResponseHeader());
		            httpOut.flush();
			        
			        if (msg.getResponseBody().length() > 0) {
			            httpOut.write(msg.getResponseBody().getBytes());
			            httpOut.flush();
			        }
			        
//			        notifyWrittenToForwardProxy();
			        
			    } catch (IOException e) {
			        throw e;
			    }
			}	// release semaphore
            

	    } while (!isConnectionClose(msg) && !inSocket.isClosed());
    }
	
	private boolean isConnectionClose(HttpMessage msg) {
		
		if (msg == null || msg.getResponseHeader().isEmpty()) {
		    return true;
		}
		
		if (msg.getRequestHeader().isConnectionClose()) {
		    return true;
		}
				
		if (msg.getResponseHeader().isConnectionClose()) {
		    return true;
		}
        
        if (msg.getResponseHeader().getContentLength() == -1 && msg.getResponseBody().length() > 0) {
            // no length and body > 0 must terminate otherwise cannot there is no way for client browser to know the length.
            // terminate early can give better response by client.
            return true;
        }
		
		return false;
	}
	
	protected void disconnect() {
		try {
            if (httpIn != null) {
                httpIn.close();
            }
        } catch (Exception e) {
        	// ZAP: Log exceptions
			log.warn(e.getMessage(), e);
        }
        
        try {
            if (httpOut != null) {
                httpOut.close();
            }
        } catch (Exception e) {
        	// ZAP: Log exceptions
			log.warn(e.getMessage(), e);
        }
		HttpUtil.closeSocket(inSocket);
        
		if (httpSender != null) {
            httpSender.shutdown();
        }

	}
	
	/**
	 * Go through each observers to process a request in each observers.
	 * The method can be modified in each observers.
	 * @param httpMessage
	 */
	private void notifyListenerRequestSend(HttpMessage httpMessage) {
		ProxyListener listener = null;
		List<ProxyListener> listenerList = parentServer.getListenerList();
		for (int i=0;i<listenerList.size();i++) {
			listener = listenerList.get(i);
			try {
			    listener.onHttpRequestSend(httpMessage);
			} catch (Exception e) {
				// ZAP: Log exceptions
				log.warn(e.getMessage(), e);
			}
		}	
	}

	/**
	 * Go thru each observers and process the http message in each observers.
	 * The msg can be changed by each observers.
	 * @param msg
	 */
	private void notifyListenerResponseReceive(HttpMessage httpMessage) {
		ProxyListener listener = null;
		List<ProxyListener> listenerList = parentServer.getListenerList();
		for (int i=0;i<listenerList.size();i++) {
			listener = listenerList.get(i);
			try {
			    listener.onHttpResponseReceive(httpMessage);
			} catch (Exception e) {
				// ZAP: Log exceptions
				log.warn(e.getMessage(), e);
			}
		}
	}
	
	private boolean isRecursive(HttpRequestHeader header) {
        boolean isRecursive = false;
        try {
            URI uri = header.getURI();
            if (uri.getHost().equals(proxyParam.getProxyIp())) {
                if (uri.getPort() == proxyParam.getProxyPort()) {
                    isRecursive = true;
                }
            }
        } catch (Exception e) {
        	// ZAP: Log exceptions
			log.warn(e.getMessage(), e);
        }
        return isRecursive;
    }
	    
    private static final Pattern remove_gzip1 = Pattern.compile("(gzip|deflate|compress|x-gzip|x-compress)[^,]*,?\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern remove_gzip2 = Pattern.compile("[,]\\z", Pattern.CASE_INSENSITIVE);
    
    private void modifyHeader(HttpMessage msg) {
        String encoding = msg.getRequestHeader().getHeader(HttpHeader.ACCEPT_ENCODING);
        if (encoding == null) {
            return;
        }
        
        encoding = remove_gzip1.matcher(encoding).replaceAll("");
        encoding = remove_gzip2.matcher(encoding).replaceAll("");
        // avoid returning gzip encoding
        
        if (encoding.length() == 0) {
            encoding = null;
        }
        msg.getRequestHeader().setHeader(HttpHeader.ACCEPT_ENCODING,encoding);

    }
    
	protected HttpSender getHttpSender() {

	    if (httpSender == null) {
		    httpSender = new HttpSender(connectionParam, true);
		}

	    return httpSender;
	}

    static boolean isAnyProxyThreadRunning() {
        return !proxyThreadList.isEmpty();
    }
    
    protected boolean isProcessCache(HttpMessage msg) throws IOException {
        if (!parentServer.isEnableCacheProcessing()) {
            return false;
        }
        
        if (parentServer.getCacheProcessingList().isEmpty()) {
            return false;
        }
        
        CacheProcessingItem item = (CacheProcessingItem) parentServer.getCacheProcessingList().get(0);
        if (msg.equals(item.message)) {
            HttpMessage newMsg = item.message.cloneAll();
            msg.setResponseHeader(newMsg.getResponseHeader());
            msg.setResponseBody(newMsg.getResponseBody());

            httpOut.write(msg.getResponseHeader());
            httpOut.flush();

            if (msg.getResponseBody().length() > 0) {
                httpOut.write(msg.getResponseBody().getBytes());
                httpOut.flush();

            }
            
            return true;
            
        } else {

            try {
                RecordHistory history = Model.getSingleton().getDb().getTableHistory().getHistoryCache(item.reference, msg);
                if (history == null) {
                    return false;
                }
                
                msg.setResponseHeader(history.getHttpMessage().getResponseHeader());
                msg.setResponseBody(history.getHttpMessage().getResponseBody());

                httpOut.write(msg.getResponseHeader());
                httpOut.flush();

                if (msg.getResponseBody().length() > 0) {
                    httpOut.write(msg.getResponseBody().getBytes());                    
                    httpOut.flush();

                }
                
                return true;
                
            } catch (Exception e) {
                return true;
            }        
        }  
    }
}

