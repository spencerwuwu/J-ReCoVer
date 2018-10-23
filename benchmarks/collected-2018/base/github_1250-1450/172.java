// https://searchcode.com/api/result/17451166/

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.grizzly.http.jk.common;

import com.sun.grizzly.http.jk.core.JkChannel;
import com.sun.grizzly.http.jk.core.JkHandler;
import com.sun.grizzly.http.jk.core.Msg;
import com.sun.grizzly.http.jk.core.MsgContext;
import com.sun.grizzly.http.jk.core.WorkerEnv;
import com.sun.grizzly.http.jk.util.threads.ThreadPool;
import com.sun.grizzly.http.jk.util.threads.ThreadPoolRunnable;
import java.util.Set;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.net.URLEncoder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.commons.modeler.Registry;
import com.sun.grizzly.tcp.Request;
import com.sun.grizzly.tcp.RequestGroupInfo;
import com.sun.grizzly.tcp.RequestInfo;
import com.sun.grizzly.tcp.ActionCode;
import com.sun.grizzly.util.LoggerUtils;
import com.sun.grizzly.util.Utils;
import java.util.logging.Level;

/** 
 * Accept ( and send ) TCP messages.
 *
 * @author Costin Manolache
 * @author Bill Barker
 * jmx:mbean name="jk:service=ChannelNioSocket"
 *            description="Accept socket connections"
 * jmx:notification name="com.sun.grizzly.tcp.INVOKE
 * jmx:notification-handler name="org.apache.jk.JK_SEND_PACKET
 * jmx:notification-handler name="org.apache.jk.JK_RECEIVE_PACKET
 * jmx:notification-handler name="org.apache.jk.JK_FLUSH
 *
 * Jk can use multiple protocols/transports.
 * Various container adapters should load this object ( as a bean ),
 * set configurations and use it. Note that the connector will handle
 * all incoming protocols - it's not specific to ajp1x. The protocol
 * is abstracted by MsgContext/Message/Channel.
 *
 * A lot of the 'original' behavior is hardcoded - this uses Ajp13 wire protocol,
 * TCP, Ajp14 API etc.
 * As we add other protocols/transports/APIs this will change, the current goal
 * is to get the same level of functionality as in the original jk connector.
 *
 * XXX Make the 'message type' pluggable
 */
public class ChannelNioSocket extends JkHandler
        implements NotificationBroadcaster, JkChannel {

    private int startPort = 8009;
    private int maxPort = 8019; // 0 for backward compat.
    private int port = startPort;
    private InetAddress inet;
    private int serverTimeout = 0;
    private boolean tcpNoDelay = true; // nodelay to true by default
    private int linger = 100;
    private int socketTimeout = 0;
    private boolean nioIsBroken = false;
    private Selector selector = null;
    private int bufferSize = 8 * 1024;
    private int packetSize = 8 * 1024;
    private long requestCount = 0;
    /* Turning this to true will reduce the latency with about 20%.
    But it requires changes in tomcat to make sure client-requested
    flush() is honored ( on my test, I got 367->433 RPS and
    52->35ms average time with a simple servlet )
     */
    ThreadPool tp = ThreadPool.createThreadPool(true);

    /* ==================== Tcp socket options ==================== */
    /**
     * jmx:managed-constructor description="default constructor"
     */
    public ChannelNioSocket() {
        // This should be integrated with the  domain setup
    }

    public ThreadPool getThreadPool() {
        return tp;
    }

    public long getRequestCount() {
        return requestCount;
    }

    /** Set the port for the ajp13 channel.
     *  To support seemless load balancing and jni, we treat this
     *  as the 'base' port - we'll try up until we find one that is not
     *  used. We'll also provide the 'difference' to the main coyote
     *  handler - that will be our 'sessionID' and the position in
     *  the scoreboard and the suffix for the unix domain socket.
     *
     * jmx:managed-attribute description="Port to listen" access="READ_WRITE"
     */
    public void setPort(int port) {
        this.startPort = port;
        this.port = port;
        this.maxPort = port + 10;
    }

    public int getPort() {
        return port;
    }

    public void setAddress(InetAddress inet) {
        this.inet = inet;
    }

    public void setBufferSize(int bs) {
        if (bs > 8 * 1024) {
            bufferSize = bs;
        }
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setPacketSize(int ps) {
        if (ps < 8 * 1024) {
            ps = 8 * 1024;
        }
        packetSize = ps;
    }

    public int getPacketSize() {
        return packetSize;
    }

    /**
     * jmx:managed-attribute description="Bind on a specified address" access="READ_WRITE"
     */
    public void setAddress(String inet) {
        try {
            this.inet = InetAddress.getByName(inet);
        } catch (Exception ex) {
            LoggerUtils.getLogger().log(Level.SEVERE,"Error parsing " + inet, ex);
        }
    }

    public String getAddress() {
        if (inet != null) {
            return inet.toString();
        }
        return "/0.0.0.0";
    }

    /**
     * Sets the timeout in ms of the server sockets created by this
     * server. This method allows the developer to make servers
     * more or less responsive to having their server sockets
     * shut down.
     *
     * <p>By default this value is 1000ms.
     */
    public void setServerTimeout(int timeout) {
        this.serverTimeout = timeout;
    }

    public int getServerTimeout() {
        return serverTimeout;
    }

    public void setTcpNoDelay(boolean b) {
        tcpNoDelay = b;
    }

    public boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setSoLinger(int i) {
        linger = i;
    }

    public int getSoLinger() {
        return linger;
    }

    public void setSoTimeout(int i) {
        socketTimeout = i;
    }

    public int getSoTimeout() {
        return socketTimeout;
    }

    public void setMaxPort(int i) {
        maxPort = i;
    }

    public int getMaxPort() {
        return maxPort;
    }

    /** At startup we'll look for the first free port in the range.
    The difference between this port and the beggining of the range
    is the 'id'.
    This is usefull for lb cases ( less config ).
     */
    public int getInstanceId() {
        return port - startPort;
    }

    /** If set to false, the thread pool will be created in
     *  non-daemon mode, and will prevent main from exiting
     */
    public void setDaemon(boolean b) {
        tp.setDaemon(b);
    }

    public boolean getDaemon() {
        return tp.getDaemon();
    }

    public void setMaxThreads(int i) {
        if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
            LoggerUtils.getLogger().log(Level.FINEST, "Setting maxThreads " + i);
        }
        tp.setMaxThreads(i);
    }

    public void setMinSpareThreads(int i) {
        if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
            LoggerUtils.getLogger().log(Level.FINEST, "Setting minSpareThreads " + i);
        }
        tp.setMinSpareThreads(i);
    }

    public void setMaxSpareThreads(int i) {
        if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
            LoggerUtils.getLogger().log(Level.FINEST, "Setting maxSpareThreads " + i);
        }
        tp.setMaxSpareThreads(i);
    }

    public int getMaxThreads() {
        return tp.getMaxThreads();
    }

    public int getMinSpareThreads() {
        return tp.getMinSpareThreads();
    }

    public int getMaxSpareThreads() {
        return tp.getMaxSpareThreads();
    }

    public void setBacklog(int i) {
    }

    public void setNioIsBroken(boolean nib) {
        nioIsBroken = nib;
    }

    public boolean getNioIsBroken() {
        return nioIsBroken;
    }
    /* ==================== ==================== */
    ServerSocket sSocket;
    final int socketNote = 1;
    final int isNote = 2;
    final int osNote = 3;
    final int notifNote = 4;
    boolean paused = false;

    @Override
    public void pause() throws Exception {
        synchronized (this) {
            paused = true;
        }
    }

    @Override
    public void resume() {
        synchronized (this) {
            paused = false;
            notify();
        }
    }

    public void accept(MsgContext ep) throws IOException {
        if (sSocket == null) {
            return;
        }
        synchronized (this) {
            while (paused) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                    //Ignore, since can't happen
                }
            }
        }
        SocketChannel sc = sSocket.getChannel().accept();
        Socket s = sc.socket();
        ep.setNote(socketNote, s);
        if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
            LoggerUtils.getLogger().log(Level.FINEST, "Accepted socket " + s + " channel " + sc.isBlocking());
        }

        try {
            setSocketOptions(s);
        } catch (SocketException sex) {
            LoggerUtils.getLogger().log(Level.FINEST, "Error initializing Socket Options", sex);
        }

        requestCount++;

        sc.configureBlocking(false);
        InputStream is = new SocketInputStream(sc);
        OutputStream os = new SocketOutputStream(sc);
        ep.setNote(isNote, is);
        ep.setNote(osNote, os);
        ep.setControl(tp);
    }

    private void setSocketOptions(Socket s) throws SocketException {
        if (socketTimeout > 0) {
            s.setSoTimeout(socketTimeout);
        }

        s.setTcpNoDelay(tcpNoDelay); // set socket tcpnodelay state

        if (linger > 0) {
            s.setSoLinger(true, linger);
        }
    }

    public void resetCounters() {
        requestCount = 0;
    }

    /** Called after you change some fields at runtime using jmx.
    Experimental for now.
     */
    public void reinit() throws IOException {
        destroy();
        init();
    }

    /**
     * jmx:managed-operation
     */
    @Override
    public void init() throws IOException {
        // Find a port.
        if (startPort == 0) {
            port = 0;
            LoggerUtils.getLogger().info("JK: ajp13 disabling channelNioSocket");
            running = true;
            return;
        }
        if (maxPort < startPort) {
            maxPort = startPort;
        }
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        for (int i = startPort; i <= maxPort; i++) {
            try {
                InetSocketAddress iddr = null;
                if (inet == null) {
                    iddr = new InetSocketAddress(i);
                } else {
                    iddr = new InetSocketAddress(inet, i);
                }
                sSocket = ssc.socket();
                sSocket.bind(iddr);
                port = i;
                break;
            } catch (IOException ex) {

                LoggerUtils.getLogger().info("Port busy " + i + " " + ex.toString());
                sSocket = null;
            }
        }

        if (sSocket == null) {
            LoggerUtils.getLogger().log(Level.SEVERE,"Can't find free port " + startPort + " " + maxPort);
            return;
        }

        LoggerUtils.getLogger().info("JK: ajp13 listening on " + getAddress() + ":" + port);

        selector = Utils.openSelector();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        // If this is not the base port and we are the 'main' channleSocket and
        // SHM didn't already set the localId - we'll set the instance id
        if ("channelNioSocket".equals(name) &&
                port != startPort &&
                (wEnv.getLocalId() == 0)) {
            wEnv.setLocalId(port - startPort);
        }

        // XXX Reverse it -> this is a notification generator !!
        if (next == null && wEnv != null) {
            if (nextName != null) {
                setNext(wEnv.getHandler(nextName));
            }
            if (next == null) {
                next = wEnv.getHandler("dispatch");
            }
            if (next == null) {
                next = wEnv.getHandler("request");
            }
        }
        JMXRequestNote = wEnv.getNoteId(WorkerEnv.ENDPOINT_NOTE, "requestNote");
        running = true;

        // Run a thread that will accept connections.
        // XXX Try to find a thread first - not sure how...
        if (this.domain != null) {
            try {
                tpOName = new ObjectName(domain + ":type=ThreadPool,name=" +
                        getChannelName());

                Registry.getRegistry(null, null).registerComponent(tp, tpOName, null);

                rgOName = new ObjectName(domain + ":type=GlobalRequestProcessor,name=" + getChannelName());
                Registry.getRegistry(null, null).registerComponent(global, rgOName, null);
            } catch (Exception e) {
                LoggerUtils.getLogger().log(Level.SEVERE,"Can't register threadpool");
            }
        }

        tp.start();
        Poller pollAjp = new Poller();
        tp.runIt(pollAjp);
    }
    ObjectName tpOName;
    ObjectName rgOName;
    RequestGroupInfo global = new RequestGroupInfo();
    int JMXRequestNote;

    public void start() throws IOException {
        if (sSocket == null) {
            init();
        }
        resume();
    }

    public void stop() throws IOException {
        destroy();
    }

    public void registerRequest(Request req, MsgContext ep, int count) {
        if (this.domain != null) {
            try {
                RequestInfo rp = req.getRequestProcessor();
                rp.setGlobalProcessor(global);
                ObjectName roname = new ObjectName(getDomain() + ":type=RequestProcessor,worker=" +
                        getChannelName() + ",name=JkRequest" + count);
                ep.setNote(JMXRequestNote, roname);

                Registry.getRegistry(null, null).registerComponent(rp, roname, null);
            } catch (Exception ex) {
                LoggerUtils.getLogger().log(Level.WARNING,"Error registering request");
            }
        }
    }

    public void open(MsgContext ep) throws IOException {
    }

    public void close(MsgContext ep) throws IOException {
        Socket s = (Socket) ep.getNote(socketNote);
        SelectionKey key = s.getChannel().keyFor(selector);
        if (key != null) {
            key.cancel();
        }
        s.close();
    }

    @Override
    public void destroy() throws IOException {
        running = false;
        try {
            /* If we disabled the channel return */
            if (port == 0) {
                return;
            }
            tp.shutdown();

            selector.wakeup().close();
            sSocket.close(); // XXX?

            if (tpOName != null) {
                Registry.getRegistry(null, null).unregisterComponent(tpOName);
            }
            if (rgOName != null) {
                Registry.getRegistry(null, null).unregisterComponent(rgOName);
            }
        } catch (Exception e) {
            LoggerUtils.getLogger().info("Error shutting down the channel " + port + " " +
                    e.toString());
            if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
                LoggerUtils.getLogger().log(Level.FINEST, "Trace", e);
            }
        }
    }

    public int send(Msg msg, MsgContext ep)
            throws IOException {
        msg.end(); // Write the packet header
        byte buf[] = msg.getBuffer();
        int len = msg.getLen();

        if (LoggerUtils.getLogger().isLoggable(Level.FINE)) {
            LoggerUtils.getLogger().log(Level.FINE,"send() " + len + " " + buf[4]);
        }

        OutputStream os = (OutputStream) ep.getNote(osNote);
        os.write(buf, 0, len);
        return len;
    }

    public int flush(Msg msg, MsgContext ep)
            throws IOException {
        OutputStream os = (OutputStream) ep.getNote(osNote);
        os.flush();
        return 0;
    }

    public int receive(Msg msg, MsgContext ep)
            throws IOException {
        if (LoggerUtils.getLogger().isLoggable(Level.FINE)) {
            LoggerUtils.getLogger().log(Level.FINE,"receive() ");
        }

        byte buf[] = msg.getBuffer();
        int hlen = msg.getHeaderLength();

        // XXX If the length in the packet header doesn't agree with the
        // actual number of bytes read, it should probably return an error
        // value.  Also, callers of this method never use the length
        // returned -- should probably return true/false instead.

        int rd = this.read(ep, buf, 0, hlen);

        if (rd < 0) {
            // Most likely normal apache restart.
            // LoggerUtils.getLogger().log(Level.WARNING,"Wrong message " + rd );
            return rd;
        }

        msg.processHeader();

        /* After processing the header we know the body
        length
         */
        int blen = msg.getLen();

        // XXX check if enough space - it's assert()-ed !!!

        int total_read = 0;

        total_read = this.read(ep, buf, hlen, blen);

        if ((total_read <= 0) && (blen > 0)) {
            LoggerUtils.getLogger().log(Level.WARNING,"can't read body, waited #" + blen);
            return -1;
        }

        if (total_read != blen) {
            LoggerUtils.getLogger().log(Level.WARNING,"incomplete read, waited #" + blen +
                    " got only " + total_read);
            return -2;
        }

        return total_read;
    }

    /**
     * Read N bytes from the InputStream, and ensure we got them all
     * Under heavy load we could experience many fragmented packets
     * just read Unix Network Programming to recall that a call to
     * read didn't ensure you got all the data you want
     *
     * from read() Linux manual
     *
     * On success, the number of bytes read is returned (zero indicates end
     * of file),and the file position is advanced by this number.
     * It is not an error if this number is smaller than the number of bytes
     * requested; this may happen for example because fewer bytes
     * are actually available right now (maybe because we were close to
     * end-of-file, or because we are reading from a pipe, or  from  a
     * terminal),  or  because  read()  was interrupted by a signal.
     * On error, -1 is returned, and errno is set appropriately. In this
     * case it is left unspecified whether the file position (if any) changes.
     *
     **/
    public int read(MsgContext ep, byte[] b, int offset, int len)
            throws IOException {
        InputStream is = (InputStream) ep.getNote(isNote);
        int pos = 0;
        int got;

        while (pos < len) {
            try {
                got = is.read(b, pos + offset, len - pos);
            } catch (ClosedChannelException sex) {
                if (pos > 0) {
                    LoggerUtils.getLogger().log(Level.WARNING,"Error reading data after " + pos + "bytes", sex);
                } else {
                    LoggerUtils.getLogger().log(Level.FINEST, "Error reading data", sex);
                }
                got = -1;
            }
            if (LoggerUtils.getLogger().isLoggable(Level.FINE)) {
                LoggerUtils.getLogger().log(Level.FINE,"read() " + b + " " + (b == null ? 0 : b.length) + " " +
                        offset + " " + len + " = " + got);
            }

            // connection just closed by remote. 
            if (got <= 0) {
                // This happens periodically, as apache restarts
                // periodically.
                // It should be more gracefull ! - another feature for Ajp14
                // LoggerUtils.getLogger().log(Level.WARNING, "server has closed the current connection (-1)" );
                return -3;
            }

            pos += got;
        }
        return pos;
    }
    protected boolean running = true;

    /** Accept incoming connections, dispatch to the thread pool
     */
    void acceptConnections() {
        if (running) {
            try {
                MsgContext ep = createMsgContext();
                ep.setSource(this);
                ep.setWorkerEnv(wEnv);
                this.accept(ep);

                if (!running) {
                    return;
                }

                // Since this is a long-running connection, we don't care
                // about the small GC
                SocketConnection ajpConn =
                        new SocketConnection(ep);
                ajpConn.register(ep);
            } catch (Exception ex) {
                if (running) {
                    LoggerUtils.getLogger().log(Level.WARNING,"Exception executing accept", ex);
                }
            }
        }
    }

    // XXX This should become handleNotification
    @Override
    public int invoke(Msg msg, MsgContext ep) throws IOException {
        int type = ep.getType();

        switch (type) {
            case JkHandler.HANDLE_RECEIVE_PACKET:
                if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
                    LoggerUtils.getLogger().log(Level.FINEST, "RECEIVE_PACKET ?? ");
                }
                return receive(msg, ep);
            case JkHandler.HANDLE_SEND_PACKET:
                return send(msg, ep);
            case JkHandler.HANDLE_FLUSH:
                return flush(msg, ep);
        }

        if (LoggerUtils.getLogger().isLoggable(Level.FINE)) {
            LoggerUtils.getLogger().log(Level.FINE,"Call next " + type + " " + next);
        }

        // Send notification
        if (nSupport != null) {
            Notification notif = (Notification) ep.getNote(notifNote);
            if (notif == null) {
                notif = new Notification("channelNioSocket.message", ep, requestCount);
                ep.setNote(notifNote, notif);
            }
            nSupport.sendNotification(notif);
        }

        if (next != null) {
            return next.invoke(msg, ep);
        } else {
            LoggerUtils.getLogger().info("No next ");
        }

        return OK;
    }

    public boolean isSameAddress(MsgContext ep) {
        Socket s = (Socket) ep.getNote(socketNote);
        return isSameAddress(s.getLocalAddress(), s.getInetAddress());
    }

    public String getChannelName() {
        String encodedAddr = "";
        if (inet != null && !"0.0.0.0".equals(inet.getHostAddress())) {
            encodedAddr = getAddress();
            if (encodedAddr.startsWith("/")) {
                encodedAddr = encodedAddr.substring(1);
            }
            encodedAddr = URLEncoder.encode(encodedAddr) + "-";
        }
        return ("jk-" + encodedAddr + port);
    }

    /**
     * Return <code>true</code> if the specified client and server addresses
     * are the same.  This method works around a bug in the IBM 1.1.8 JVM on
     * Linux, where the address bytes are returned reversed in some
     * circumstances.
     *
     * @param server The server's InetAddress
     * @param client The client's InetAddress
     */
    public static boolean isSameAddress(InetAddress server, InetAddress client) {
        // Compare the byte array versions of the two addresses
        byte serverAddr[] = server.getAddress();
        byte clientAddr[] = client.getAddress();
        if (serverAddr.length != clientAddr.length) {
            return (false);
        }
        boolean match = true;
        for (int i = 0; i < serverAddr.length; i++) {
            if (serverAddr[i] != clientAddr[i]) {
                match = false;
                break;
            }
        }
        if (match) {
            return (true);
        }

        // Compare the reversed form of the two addresses
        for (int i = 0; i < serverAddr.length; i++) {
            if (serverAddr[i] != clientAddr[(serverAddr.length - 1) - i]) {
                return (false);
            }
        }
        return (true);
    }

    public void sendNewMessageNotification(Notification notification) {
        if (nSupport != null) {
            nSupport.sendNotification(notification);
        }
    }
    private NotificationBroadcasterSupport nSupport = null;

    public void addNotificationListener(NotificationListener listener,
            NotificationFilter filter,
            Object handback)
            throws IllegalArgumentException {
        if (nSupport == null) {
            nSupport = new NotificationBroadcasterSupport();
        }
        nSupport.addNotificationListener(listener, filter, handback);
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        if (nSupport != null) {
            nSupport.removeNotificationListener(listener);
        }
    }
    MBeanNotificationInfo notifInfo[] = new MBeanNotificationInfo[0];

    public void setNotificationInfo(MBeanNotificationInfo info[]) {
        this.notifInfo = info;
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return notifInfo;
    }

    protected class SocketConnection implements ThreadPoolRunnable {

        MsgContext ep;
        MsgAjp recv = new MsgAjp(packetSize);
        boolean inProgress = false;

        SocketConnection(MsgContext ep) {
            this.ep = ep;
        }

        public Object[] getInitData() {
            return null;
        }

        public void runIt(Object perTh[]) {
            if (!processConnection(ep)) {
                unregister(ep);
            }
        }

        public boolean isRunning() {
            return inProgress;
        }

        public void setFinished() {
            inProgress = false;
        }

        /** Process a single ajp connection.
         */
        boolean processConnection(MsgContext ep) {
            try {
                InputStream sis = (InputStream) ep.getNote(isNote);
                boolean haveInput = true;
                while (haveInput) {
                    if (!running || paused) {
                        return false;
                    }
                    int status = receive(recv, ep);
                    if (status <= 0) {
                        if (status == -3) {
                            LoggerUtils.getLogger().log(Level.FINEST, "server has been restarted or reset this connection");
                        } else {
                            LoggerUtils.getLogger().log(Level.WARNING,"Closing ajp connection " + status);
                        }
                        return false;
                    }
                    ep.setLong(MsgContext.TIMER_RECEIVED, System.currentTimeMillis());

                    ep.setType(0);
                    // Will call next
                    status = invoke(recv, ep);
                    if (status != JkHandler.OK) {
                        LoggerUtils.getLogger().log(Level.WARNING,"processCallbacks status " + status);
                        ep.action(ActionCode.ACTION_CLOSE, ep.getRequest().getResponse());
                        return false;
                    }
                    synchronized (this) {
                        synchronized (sis) {
                            haveInput = sis.available() > 0;
                        }
                        if (!haveInput) {
                            setFinished();
                        } else {
                            if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
                                LoggerUtils.getLogger().log(Level.FINEST, "KeepAlive: " + sis.available());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg != null && msg.indexOf("Connection reset") >= 0) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Server has been restarted or reset this connection");
                } else if (msg != null && msg.indexOf("Read timed out") >= 0) {
                    LoggerUtils.getLogger().log(Level.FINEST, "connection timeout reached");
                } else {
                    LoggerUtils.getLogger().log(Level.SEVERE,"Error, processing connection", ex);
                }
                return false;
            }
            return true;
        }

        synchronized void process(SelectionKey sk) {
            if (!sk.isValid()) {
                SocketInputStream sis = (SocketInputStream) ep.getNote(isNote);
                sis.closeIt();
                return;
            }
            if (sk.isReadable()) {
                SocketInputStream sis = (SocketInputStream) ep.getNote(isNote);
                boolean isok = sis.readAvailable();
                if (!inProgress) {
                    if (isok) {
                        if (sis.available() > 0 || !nioIsBroken) {
                            inProgress = true;
                            tp.runIt(this);
                        }
                    } else {
                        unregister(ep);
                        return;
                    }
                }
            }
            if (sk.isWritable()) {
                Object os = ep.getNote(osNote);
                synchronized (os) {
                    os.notify();
                }
            }
        }

        synchronized void unregister(MsgContext ep) {
            try {
                close(ep);
            } catch (Exception e) {
                LoggerUtils.getLogger().log(Level.SEVERE,"Error closing connection", e);
            }
            try {
                Request req = (Request) ep.getRequest();
                if (req != null) {
                    ObjectName roname = (ObjectName) ep.getNote(JMXRequestNote);
                    if (roname != null) {
                        Registry.getRegistry(null, null).unregisterComponent(roname);
                    }
                    req.getRequestProcessor().setGlobalProcessor(null);
                }
            } catch (Exception ee) {
                LoggerUtils.getLogger().log(Level.SEVERE,"Error, releasing connection", ee);
            }
        }

        void register(MsgContext ep) {
            Socket s = (Socket) ep.getNote(socketNote);
            try {
                s.getChannel().register(selector, SelectionKey.OP_READ, this);
            } catch (IOException iex) {
                LoggerUtils.getLogger().log(Level.SEVERE,"Unable to register connection", iex);
                unregister(ep);
            }
        }
    }

    protected class Poller implements ThreadPoolRunnable {

        Poller() {
        }

        public Object[] getInitData() {
            return null;
        }

        public void runIt(Object perTh[]) {
            while (running) {
                try {
                    int ns = selector.select(serverTimeout);
                    if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
                        LoggerUtils.getLogger().log(Level.FINEST, "Selecting " + ns + " channels");
                    }
                    if (ns > 0) {
                        Set sels = selector.selectedKeys();
                        Iterator it = sels.iterator();
                        while (it.hasNext()) {
                            SelectionKey sk = (SelectionKey) it.next();
                            if (sk.isAcceptable()) {
                                acceptConnections();
                            } else {
                                SocketConnection sc = (SocketConnection) sk.attachment();
                                sc.process(sk);
                            }
                            it.remove();
                        }
                    }
                } catch (ClosedSelectorException cse) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Selector is closed");
                    return;
                } catch (CancelledKeyException cke) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Key Cancelled", cke);
                } catch (IOException iex) {
                    LoggerUtils.getLogger().log(Level.WARNING,"IO Error in select", iex);
                } catch (Exception ex) {
                    LoggerUtils.getLogger().log(Level.WARNING,"Error processing select", ex);
                }
            }
        }
    }

    protected class SocketInputStream extends InputStream {

        final int BUFFER_SIZE = 8200;
        private ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        private SocketChannel channel;
        private boolean blocking = false;
        private boolean isClosed = false;
        private volatile boolean dataAvailable = false;

        SocketInputStream(SocketChannel channel) {
            this.channel = channel;
            buffer.limit(0);
        }

        @Override
        public int available() {
            return buffer.remaining();
        }

        @Override
        public void mark(int readlimit) {
            buffer.mark();
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void reset() {
            buffer.reset();
        }

        public synchronized int read() throws IOException {
            if (!checkAvailable(1)) {
                block(1);
            }
            return buffer.get();
        }

        private boolean checkAvailable(int nbyte) throws IOException {
            if (isClosed) {
                throw new ClosedChannelException();
            }
            return buffer.remaining() >= nbyte;
        }

        private int fill(int nbyte) throws IOException {
            int rem = nbyte;
            int read = 0;
            boolean eof = false;
            byte[] oldData = null;
            if (buffer.remaining() > 0) {
                // should rarely happen, so short-lived GC shouldn't hurt
                // as much as allocating a long-lived buffer for this
                if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Saving old buffer: " + buffer.remaining());
                }
                oldData = new byte[buffer.remaining()];
                buffer.get(oldData);
            }
            buffer.clear();
            if (oldData != null) {
                buffer.put(oldData);
            }
            while (rem > 0) {
                int count = channel.read(buffer);
                if (count < 0) {
                    eof = true;
                    break;
                } else if (count == 0) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Failed to recieve signaled read: ");
                    break;
                }
                read += count;
                rem -= count;
            }
            buffer.flip();
            return eof ? -1 : read;
        }

        synchronized boolean readAvailable() {
            if (blocking) {
                dataAvailable = true;
                notify();
            } else if (dataAvailable) {
                LoggerUtils.getLogger().log(Level.FINEST, "Race Condition");
            } else {
                int nr = 0;

                try {
                    nr = fill(1);
                } catch (ClosedChannelException cce) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Channel is closed", cce);
                    nr = -1;
                } catch (IOException iex) {
                    LoggerUtils.getLogger().log(Level.WARNING,"Exception processing read", iex);
                    nr = -1; // Can't handle this yet
                }
                if (nr < 0) {
                    closeIt();
                    return false;
                } else if (nr == 0) {
                    if (!nioIsBroken) {
                        dataAvailable = (buffer.remaining() <= 0);
                    }
                }
            }
            return true;
        }

        synchronized void closeIt() {
            isClosed = true;
            if (blocking) {
                notify();
            }
        }

        @Override
        public int read(byte[] data) throws IOException {
            return read(data, 0, data.length);
        }

        @Override
        public synchronized int read(byte[] data, int offset, int len) throws IOException {
            int olen = len;
            while (!checkAvailable(len)) {
                int avail = buffer.remaining();
                if (avail > 0) {
                    buffer.get(data, offset, avail);
                }
                len -= avail;
                offset += avail;
                block(len);
            }
            buffer.get(data, offset, len);
            return olen;
        }

        private void block(int len) throws IOException {
            if (len <= 0) {
                return;
            }
            if (!dataAvailable) {
                blocking = true;
                if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Waiting for " + len + " bytes to be available");
                }
                try {
                    wait(socketTimeout);
                } catch (InterruptedException iex) {
                    LoggerUtils.getLogger().log(Level.FINEST, "Interrupted", iex);
                }
                blocking = false;
            }
            if (dataAvailable) {
                dataAvailable = false;
                if (fill(len) < 0) {
                    isClosed = true;
                }
            } else if (!isClosed) {
                throw new SocketTimeoutException("Read request timed out");
            }
        }
    }

    protected class SocketOutputStream extends OutputStream {

        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        SocketChannel channel;

        SocketOutputStream(SocketChannel channel) {
            this.channel = channel;
        }

        public void write(int b) throws IOException {
            if (!checkAvailable(1)) {
                flush();
            }
            buffer.put((byte) b);
        }

        @Override
        public void write(byte[] data) throws IOException {
            write(data, 0, data.length);
        }

        @Override
        public void write(byte[] data, int offset, int len) throws IOException {
            if (!checkAvailable(len)) {
                flush();
            }
            buffer.put(data, offset, len);
        }

        @Override
        public void flush() throws IOException {
            buffer.flip();
            while (buffer.hasRemaining()) {
                int count = channel.write(buffer);
                if (count == 0) {
                    synchronized (this) {
                        SelectionKey key = channel.keyFor(selector);
                        key.interestOps(SelectionKey.OP_WRITE);
                        if (LoggerUtils.getLogger().isLoggable(Level.FINEST)) {
                            LoggerUtils.getLogger().log(Level.FINEST, "Blocking for channel write: " + buffer.remaining());
                        }
                        try {
                            wait();
                        } catch (InterruptedException iex) {
                            // ignore, since can't happen
                        }
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
            buffer.clear();
        }

        private boolean checkAvailable(int len) {
            return buffer.remaining() >= len;
        }
    }
}


