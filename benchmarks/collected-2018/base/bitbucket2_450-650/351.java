// https://searchcode.com/api/result/50051356/

package com.atlassian.confluence.extra.webdav.servlet.filter;

import com.atlassian.confluence.extra.webdav.servlet.filter.exceptions.FailedOperationException;
import com.atlassian.confluence.extra.webdav.util.UserAgentUtil;
import com.atlassian.confluence.extra.webdav.util.WebdavConstants;
import com.google.common.io.CharStreams;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.server.io.BoundedInputStream;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.UnLockMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class ReverseProxyFilter extends AbstractPrefixAwareFilter {
    private static final Logger logger = Logger.getLogger(ReverseProxyFilter.class);

    private static final String SERVER_HOST_NAME = "localhost";

    private String mountPointPrefix;

    private MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager;

    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        mountPointPrefix = StringUtils.defaultString(filterConfig.getInitParameter("mount-point-prefix"));
        multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
    }

    protected boolean handles(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String userAgenty = request.getHeader(WebdavConstants.HEADER_USER_AGENT);
        /* Only handle empty context root because Windows can only map a WebDAV location to a drive when the port is 80 and
         * there is no context path. 
         */
        return !UserAgentUtil.isOsxFinder(userAgenty) && StringUtils.isEmpty(request.getContextPath());
        /* Since only the OSX Finder cannot work with this proxy, we'll just handle anything that is not Finder.
         * This is required because MS Office sometimes send a Mozilla/XXX user agent header and when that happens and
         * this method returns false, shit happens.
         *
         * I'm open to any good suggestions.
         */
    }

    protected boolean hasHandledBefore(HttpServletRequest request) {
        return StringUtils.contains(request.getHeader("via"), SERVER_HOST_NAME);
    }

    protected String getTargetUri(HttpServletRequest request) {
        String originalUri = request.getRequestURI();

        if (StringUtils.startsWith(originalUri, mountPointPrefix)) {
            return new StringBuffer()
                    .append(request.getScheme())
                    .append("://")
                    .append(request.getHeader(WebdavConstants.HEADER_HOST))
                    .append(request.getContextPath())
                    .append(getPrefix())
                    .append(originalUri.substring(mountPointPrefix.length()))
                    .toString();
        }

        return originalUri;
    }

    private File getRequestBodyAsFile(HttpServletRequest request) throws IOException {
        File file = File.createTempFile("confluence.extra.webdav-requestbody-", null);
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new BufferedInputStream(request.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(file));

            IOUtils.copy(in, out);

            out.flush();

            return file;

        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    private HttpMethodBase getMethod(HttpServletRequest httpServletRequest) throws DavException, IOException {
        String targetUri = getTargetUri(httpServletRequest);
        WebdavRequest webdavRequest = new WebdavRequestImpl(httpServletRequest, null);

        final int methodCode = DavMethods.getMethodCode(httpServletRequest.getMethod());

        switch (methodCode) {
            case DavMethods.DAV_OPTIONS:
                return new OptionsMethod(targetUri);
            case DavMethods.DAV_MOVE:
                return new MoveMethod(targetUri,
                        httpServletRequest.getHeader(WebdavConstants.HEADER_DESTINATION),
                        webdavRequest.isOverwrite());
            case DavMethods.DAV_PUT:
                return new PutMethod(targetUri);
            case DavMethods.DAV_LOCK:
                return new LockMethod(targetUri, webdavRequest.getLockInfo());
            case DavMethods.DAV_UNLOCK:
                return new UnLockMethod(targetUri, webdavRequest.getLockToken());
            case DavMethods.DAV_PROPPATCH:
                return new PropPatchMethod(targetUri, webdavRequest.getPropPatchChangeList());
            case DavMethods.DAV_MKCOL:
                return new MkColMethod(targetUri);
            case DavMethods.DAV_COPY:
                return new CopyMethod(targetUri,
                        httpServletRequest.getHeader(WebdavConstants.HEADER_DESTINATION),
                        webdavRequest.isOverwrite(),
                        webdavRequest.getDepth(DavConstants.DEPTH_INFINITY) == DavConstants.DEPTH_0);
            case DavMethods.DAV_PROPFIND:
                return new PropFindMethod(
                        targetUri,
                        webdavRequest.getPropFindProperties(),
                        webdavRequest.getDepth(DavConstants.DEPTH_INFINITY)
                );
            case DavMethods.DAV_DELETE:
                return new DeleteMethod(targetUri);
            case DavMethods.DAV_GET:
                return new GetMethod(targetUri);
            default:
                return null;
        }
    }

    private void prepareProxiedMethodHeaders(HttpMethodBase method, HttpServletRequest request) {
        /* Add all headers besides Connection */
        for (Enumeration headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {
            String headerName = (String) headerNames.nextElement();

            if (!StringUtils.equalsIgnoreCase(WebdavConstants.HEADER_CONNECTION, headerName)) {
                for (Enumeration headerValues = request.getHeaders(headerName); headerValues.hasMoreElements(); ) {
                    method.setRequestHeader(headerName, (String) headerValues.nextElement());
                }
            }
        }
    }

    private void prepareProxiedMethodProxyHeaders(HttpMethodBase method, HttpServletRequest request) throws ServletException {
        /* Copied from j2ep */
        String originalVia = request.getHeader("via");
        StringBuffer via = new StringBuffer("");

        if (originalVia != null) {
            if (originalVia.indexOf(SERVER_HOST_NAME) != -1) {
                throw new ServletException("This proxy has already handled the request, will abort.");
            }
            via.append(originalVia).append(", ");
        }
        via.append(request.getProtocol()).append(" ").append(SERVER_HOST_NAME);

        method.setRequestHeader("via", via.toString());
        method.setRequestHeader("x-forwarded-for", request.getRemoteAddr());
        method.setRequestHeader("x-forwarded-host", request.getServerName());
        method.setRequestHeader("x-forwarded-server", SERVER_HOST_NAME);

        method.setRequestHeader("accept-encoding", "");
    }

    private File prepareProxiedMethodBody(EntityEnclosingMethod method, HttpServletRequest request) throws ServletException, IOException {
        File requestBodyFile = getRequestBodyAsFile(request);

        method.setRequestEntity(
                new InputStreamRequestEntity(
                        new BufferedInputStream(new FileInputStream(requestBodyFile)),
                        requestBodyFile.length(),
                        request.getContentType()
                )
        );

        return requestBodyFile;
    }

    private File[] prepareProxiedMethod(HttpMethodBase method, HttpServletRequest request) throws ServletException, IOException {
        prepareProxiedMethodHeaders(method, request);
        prepareProxiedMethodProxyHeaders(method, request);
        method.setFollowRedirects(false);

        if (method instanceof EntityEnclosingMethod) {
            File requestBodyFile = prepareProxiedMethodBody((EntityEnclosingMethod) method, request);
            return new File[]{requestBodyFile};
        }

        return new File[0];
    }

    private void prepareResponseHeaders(HttpMethodBase method, HttpServletResponse response) {
        Header[] headers = method.getResponseHeaders();

        if (null != headers) {
            for (int i = 0; i < headers.length; ++i) {
                String headerName = headers[i].getName();

                if (!StringUtils.equalsIgnoreCase("Connection", headerName)
                        && !StringUtils.equalsIgnoreCase(DavConstants.HEADER_CONTENT_LENGTH, headerName)) {
                    response.setHeader(headerName, headers[i].getValue());
                }
            }
        }
    }

    private void prepareResponseProxyHeaders(HttpMethodBase method, HttpServletResponse response) {
        /* Copied from j2ep */
        Header originalVia = method.getResponseHeader("via");
        StringBuffer via = new StringBuffer("");
        if (originalVia != null) {
            via.append(originalVia.getValue()).append(", ");
        }
        via.append(method.getStatusLine().getHttpVersion()).append(" ").append(SERVER_HOST_NAME);

        response.setHeader("via", via.toString());
    }

    private File getResponseBodyAsFile(HttpMethodBase method) throws IOException {
        //we'd like to avoid creation of tmp files if something went wrong
        //it will reduce disc IO operations during handling invalid requests
        //for example all non-authenticated responses won't cause creation (and then deletion) temp files on disk
        if (method.getStatusCode() == HttpStatus.SC_FORBIDDEN || method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            throw new FailedOperationException(method.getStatusCode(),
                    CharStreams.toString(
                            new InputStreamReader(new BoundedInputStream(method.getResponseBodyAsStream(), 5000))));
        }

        File responseTempFile = File.createTempFile("confluence.extra.webdav-responsebody-", null);
        InputStream in = null;
        OutputStream out = null;

        try {
            in = method.getResponseBodyAsStream();
            if (null != in) {
                in = new BufferedInputStream(in);
                out = new BufferedOutputStream(new FileOutputStream(responseTempFile));

                IOUtils.copy(in, out);
            }

            return responseTempFile;
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    private void prepareResponseContent(File tempResponseFile, HttpServletResponse response) throws IOException {
        flushResponseContent(new FileInputStream(tempResponseFile), response);
    }

    private void prepareResponseContent(String responseString, HttpServletResponse response) throws IOException {
        flushResponseContent(IOUtils.toInputStream(responseString), response);
    }

    private void flushResponseContent(InputStream responseInputStream, HttpServletResponse response) throws IOException {
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new BufferedInputStream(responseInputStream);
            out = new BufferedOutputStream(response.getOutputStream());

            IOUtils.copy(in, out);

        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(responseInputStream);
        }
    }

    private String getDocumentAsString(Document document) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("encoding", "UTF-8");
        transformer.setOutputProperty("indent", "yes");

        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

        return stringWriter.toString();
    }

    private File getResponseDocumentAsFile(DavMethod davMethod) throws IOException, TransformerException {
        File responseTempFile = File.createTempFile("confluence.extra.webdav-responsebody-", null);
        String responseXml = getDocumentAsString(davMethod.getResponseBodyAsDocument());
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new ByteArrayInputStream(responseXml.getBytes("UTF-8"));
            out = new BufferedOutputStream(new FileOutputStream(responseTempFile));

            IOUtils.copy(in, out);

            return responseTempFile;
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    private File[] prepareResponse(HttpMethodBase method, HttpServletResponse response) throws IOException, TransformerException {
        /* Whatever you do, do not reorder the statements here.
         * Otherwise, https://issues.apache.org/bugzilla/show_bug.cgi?id=32604 will bite your ass off and Alaskan king crabs will dance on your grave.
         */
        int statusCode = method.getStatusCode();

        response.setStatus(statusCode); /* This has to be first, don't know why. */

        prepareResponseProxyHeaders(method, response);
        prepareResponseHeaders(method, response);

        File tempResponseFile = null;
        String methodName = method.getName();

        try {
            if (method instanceof DavMethodBase
                    && !StringUtils.equalsIgnoreCase(DavMethods.METHOD_OPTIONS, methodName) /* DavMethods which have no XML response bodies */
                    && !StringUtils.equalsIgnoreCase(DavMethods.METHOD_PUT, methodName)
                    && !StringUtils.equalsIgnoreCase(DavMethods.METHOD_MOVE, methodName)
                    && !StringUtils.equalsIgnoreCase(DavMethods.METHOD_DELETE, methodName)
                    && !StringUtils.equalsIgnoreCase(DavMethods.METHOD_MKCOL, methodName)
                    && ((DavMethodBase) method).succeeded()) {
                DavMethodBase davMethod = (DavMethodBase) method;
                tempResponseFile = getResponseDocumentAsFile(davMethod);
            } else {
                tempResponseFile = getResponseBodyAsFile(method);
            }

            if (tempResponseFile.length() > 0) {
                prepareResponseContent(tempResponseFile, response);
            }

            response.setHeader(WebdavConstants.HEADER_CONTENT_LENGTH, String.valueOf(tempResponseFile.length()));
            response.setContentLength((int) tempResponseFile.length());
        } catch (FailedOperationException e) {
            String responseString = e.getResponseString();
            prepareResponseContent(responseString, response);
            response.setHeader(WebdavConstants.HEADER_CONTENT_LENGTH, String.valueOf(responseString.length()));
            response.setContentLength(responseString.length());
        }

        return tempResponseFile == null ? new File[]{} : new File[]{tempResponseFile};
    }

    public void doFilter(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            if (StringUtils.equalsIgnoreCase(httpServletRequest.getMethod(), DavMethods.METHOD_OPTIONS)
                    && StringUtils.equals("/", httpServletRequest.getRequestURI())) {
                httpServletResponse.addHeader("MS-Author-Via", WebdavConstants.HEADER_DAV); /* Make Windows don't think of us as NTLM aware */
                httpServletResponse.setStatus(HttpServletResponse.SC_OK); /* Just reply if OPTIONS and / */
                return;
            }

            if (!StringUtils.startsWith(httpServletRequest.getRequestURI(), mountPointPrefix)
                    || hasHandledBefore(httpServletRequest)) {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
                return;
            }

            HttpMethodBase proxiedMethod = getMethod(httpServletRequest);
            if (null != proxiedMethod) {
                List tempFilesCreated = new ArrayList();

                tempFilesCreated.addAll(
                        Arrays.asList(
                                prepareProxiedMethod(proxiedMethod, httpServletRequest)
                        )
                );

                try {
                    HttpClient httpClient = new HttpClient(multiThreadedHttpConnectionManager);

                    httpClient.getParams().setBooleanParameter(HttpClientParams.USE_EXPECT_CONTINUE, false);
                    httpClient.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

                    httpClient.executeMethod(proxiedMethod);

                    File[] responseTmpFiles = prepareResponse(proxiedMethod, httpServletResponse);
                    tempFilesCreated.addAll(Arrays.asList(responseTmpFiles));
                } catch (Exception ioe) {
                    throw new ServletException("IO error.", ioe);
                } finally {
                    proxiedMethod.releaseConnection();
                    for (Iterator i = tempFilesCreated.iterator(); i.hasNext(); ) {
                        File file = (File) i.next();
                        if (!file.delete())
                            logger.warn("Failed to remove temp file :" + file.getAbsolutePath());
                    }
                }
            } else {
                httpServletResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (DavException de) {
            throw new ServletException("Unexpected DavException", de);
        }
    }

    public void destroy() {
        try {
            multiThreadedHttpConnectionManager.shutdown();
        } finally {
            super.destroy();
        }
    }
}

