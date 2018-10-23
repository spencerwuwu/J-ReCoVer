// https://searchcode.com/api/result/123690627/

/*
###########################################################################
# qiWorkbench - an extensible platform for seismic interpretation
# This program module Copyright (C) 2006  BHP Billiton Petroleum
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License Version 2 as published
# by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
# details.
#
# You should have received a copy of the GNU General Public License along
# with this program; if not, write to the Free Software Foundation, Inc.,
# 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA or visit the
# link http://www.gnu.org/licenses/gpl.txt.
#
# PLEASE NOTE:  One or more BHP Billiton patents or patent applications may
# cover this software or its use. BHP Billiton hereby grants a limited,
# personal, irrevocable, perpetual, royalty-free license under such patents
# but only for things you are allowed to do under the terms of the General
# Public License.  All other rights are expressly reserved.
#
# To contact BHP Billiton about this software you can e-mail
# info@qiworkbench.org or visit http://qiworkbench.org to learn more.
###########################################################################
 */
package com.bhpb.qiworkbench.client.util;

import com.bhpb.qiworkbench.api.IQiWorkbenchMsg;
import com.bhpb.qiworkbench.QiWorkbenchMsg;
import com.bhpb.qiworkbench.compAPI.MsgUtils;
import com.bhpb.qiworkbench.compAPI.QIWConstants;
import com.bhpb.qiworkbench.client.SocketManager;
import com.bhpb.qiworkbench.client.SocketManager.BoundIOSocket;
import com.bhpb.qiworkbench.messageFramework.DispatcherConnector;
import com.bhpb.qiworkbench.messageFramework.MessageDispatcher;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

/**
 * Read byte data from a client-side socket channel, which may be one used 
 * before and is therefore established or a new one which must be established,
 * i.e., connected to the port assigned by the server.
 *
 * @author Gil Hansen
 * @author Woody Folsom - Created several new private methods to reduce complexity
 *  of read() method, implemented writing to the FileSystem and returning the
 *  filename rather than returning the binary data.
 *
 * @version 1.0
 */
public class ReadDataFromSocket {
    private static final Logger logger = Logger.getLogger(ReadDataFromSocket.class.getName());
    
    public static final String SOCKET_READ_SUCCESSFUL_PREFIX = "filename=";
    private static final int BUF_SIZE = 64 * 1024;
    private static final String NOMINAL_STATUS = "";
    final int WAIT_TIME = 1000; //milliseconds

    // Port assigned by the server
    private int serverPort = 0;
    private SocketChannel clientSocketChannel = null;
    private String serverURL = "";
    //Selector for the socket used to read a buffer at a time.
    private Selector selector = null;
    
    /** Manager to get an available socket channel used to communicate with the server */
    SocketManager socketManager;

    /**
     * Constructor
     * @param data The URL of the server writing the byte data..
     * @param serverPort The port assigned by the server.
     */
    public ReadDataFromSocket(String serverURL, int serverPort) {
        this.serverURL = serverURL;
        this.serverPort = serverPort;
        
        socketManager = SocketManager.getInstance();
    }

    /**
     * Read byte data from a socket channel to an output stream; normally a 
     * temp file. Connect to the port assigned by the server and read the byte 
     * data the server is sending. The end-of-data is marked by the EOS constant,
     * a valid IEEE NaN (4 bytes).
     * <p>
     * NOTE: This method is only used by the bhpViewer. 
     * @param ostream Output stream to send the byte data sent by the server
     * over the socket channel.
     * @return empty string if read successful; otherwise, reason why read
     * failed.
     */
    public String read(FileOutputStream ostream) {
        //NOTE: establishSocketChannel was called when setupSocketChannel was //called by the server IOService
        String channelStatus = readFromChannel(selector, ostream, QIWConstants.RESPONSE_TIMEOUT);

        return channelStatus;
    }

    private void registerSelector() throws IOException {
        selector = Selector.open();
        clientSocketChannel.register(selector, SelectionKey.OP_READ);
                
        long startTime = System.currentTimeMillis();
        int n = selector.select(5*WAIT_TIME);

        while (n == 0 && System.currentTimeMillis() - startTime < QIWConstants.RESPONSE_TIMEOUT) {
            n = selector.select(QIWConstants.RESPONSE_TIMEOUT);
        }

        if (n == 0) {
            //Notify server finished with server port it assigned
            sendSocketNotification(serverPort);
            socketManager.releaseSocket(clientSocketChannel, selector, serverURL, serverPort);
            throw new IOException("Client socket channel not ready for IO operations");
        }
    }

    /**
     * Returns true if and only if the status string is empty.
     */
    public boolean isStatusNominal(String status) {
        return NOMINAL_STATUS.equals(status);
    }

    /**
	 * Create a client socket channel and connect to it. Also, get its registered selector. The socket channel may be one that was previously used.
     * This method should be invoked via <code>establishSocketChannel(int timeout)</code>.
     * If called directly it may throw the indicated exceptions if the SocketChannel
     *  is not immediately available.
     *
     * CAVEAT: This method consumes InterruptedExceptions and may therefore return
     * after the SocketChannel is opened but before the connection is complete.
     */
    private String establishSocketChannel() throws ClosedChannelException, IOException {
        String socketChannelStatus = NOMINAL_STATUS;

        //Create a nonblocking socket channel for the specified host name and port
        BoundIOSocket boundIOSocket = socketManager.acquireSocketChannel(serverURL, serverPort);
        clientSocketChannel = boundIOSocket.getClientSocketChannel();
        selector = boundIOSocket.getClientChannelSelector();
        if (clientSocketChannel == null) {
            socketChannelStatus = "Failed to aquire a socket channel";
            return socketChannelStatus;
        }
        
        //If reusing a socket channel, it is already connected to the
        //server socket, it is registered and its associated selector is not null.
        if (clientSocketChannel.isConnected()) {
            logger.info("ReadDataFromSocket::establishSocketChannel: Reusing a registered client socket channel on port "+clientSocketChannel.socket().getLocalPort());
			return socketChannelStatus;
		}

        // Send a connection request to the server
        String hostname = "";
        //remove 'http://' from URL
        int k = serverURL.indexOf("http://");
        if (k != -1) {
            hostname = serverURL.substring(7);
        }
        //remove port, if any, on URL (e.g., :8080)
        k = hostname.lastIndexOf(':');
        if (k != -1) { // fix for QIWB-37, exception when running on beta PUC
            hostname = hostname.substring(0, k);
        }

        logger.info("Binding InetSocketAddress: " + hostname + ", " + serverPort);
        InetSocketAddress addr = new InetSocketAddress(hostname, serverPort);
        int resolveAddrCount = 0;
        while (addr.isUnresolved() && resolveAddrCount < 120) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                logger.info("While waiting to resolve address: " + addr.toString() + ", caught: " + ie.getMessage());
            } finally {
                resolveAddrCount++;
            }
        }

        if (addr.isUnresolved()) {
            socketChannelStatus = "Failed to resolve address: " + addr.toString() + " for port " + serverPort + " after 30 seconds";
            return socketChannelStatus;
        } else {
            logger.info("Address resolved for port #" + serverPort);
        }

        logger.info("Connecting to socket on port " + serverPort + "...");
        boolean connected = false;
        try {
            connected = clientSocketChannel.connect(addr);
        } catch (ConnectionPendingException cpe) {
            //Another non-blocking channel is in the process of connecting,
            //i.e., a concurrent connection is in progress. Try and finish
            //connection
            logger.info("Concurrent connection pending. Wait until socket connects.");
/*
            boolean connectionPending = true;
            while (connectionPending) {
                logger.info("Concurrent connection pending. Wait until socket connects.");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
                //Eventually, socket will connect
                if (clientSocketChannel.isConnected()) break;

//                try {
//                    connected = clientSocketChannel.connect(addr);
//                } catch (ConnectionPendingException cpe2) {
//                    continue;
//                }

                connectionPending = false;
            }
*/
        }

        //if the connection was not completed, then attempt to complete it
        if (!connected) {
            long startTime = System.currentTimeMillis();
            boolean connectionCompleted = false;
            do {
                try {
                    //wait a bit before finishing the connection
                    Thread.sleep(250);

                    connectionCompleted = clientSocketChannel.finishConnect();
                    if (connectionCompleted) {
                        logger.info("Finished connecting to socket on port: " + serverPort);
                        break;
                    }
                } catch (InterruptedException ie) {
                } catch (IOException ioe) {
                    logger.info("IOException trying to connect to socket on port " + serverPort + ": " + ioe.getMessage());
                }
                if ((System.currentTimeMillis() - startTime) >= 30000) {
                    socketChannelStatus = "Failed to open socket channel on port " + serverPort + " after 30 seconds";
                    break;
                }
            } while (!connectionCompleted);
        }

        return socketChannelStatus;
    }

    private String readFromChannel(Selector sel, FileOutputStream tempOutputStream, int timeout) {
        String channelStatus = NOMINAL_STATUS;
        logger.info("Reading data from socket on port #" + serverPort + ", allocating array of size: " + BUF_SIZE);
        byte[] bytes1;
        byte[] bytes2;
        try {
            bytes1 = new byte[BUF_SIZE];
            bytes2 = new byte[BUF_SIZE];
        } catch (Error e) {
            System.gc();
            logger.severe("Caught error while allocating byte array in readFromChannel.");
            throw e;
        }
        ByteBuffer buf1 = ByteBuffer.wrap(bytes1);
        ByteBuffer buf2 = ByteBuffer.wrap(bytes2);
//        buf1.clear(); buf2.clear();
        ByteBuffer buf = buf1;
        //primary buffer is where data is read into; other is secondary which
        //may be empty if there is only 1 buffer's worth data to read
        int primBuf = 1;
        //size of alternate buffer; 0 if empty
        int altBufSize = 0;
        logger.info("Beginning to read data from clientSocketChannel...");

        long totalBytesRead = 0;
        boolean splitEOS = false;
		//Number of EOS bytes expected in next buffer
        int splitCnt = 0;
        try {
            boolean finishedReading = false;
            while (!finishedReading) {
                //Read bytes into the buffer
                int bytesRead = clientSocketChannel.read(buf);				
                //wait for bytes to be read
                if (bytesRead == 0) {
                    waitAndLogInterrupt(timeout);
                    continue;
                } else {
                    //check for EOS and remove
                    //Note: EOS may be split across two consecutive buffers
                    if (splitEOS && bytesRead <= 3) { //EOS split, remainder in buffer
						logger.info("partial EOS; splitCnt="+splitCnt);
                        //Double check remainder of EOS in buffer
                        if (splitCnt == 3) {
                            if (buf.get(bytesRead-3) == QIWConstants.EOS_BYTE2 &&
                                buf.get(bytesRead-2) == QIWConstants.EOS_BYTE3 &&
                                buf.get(bytesRead-1) == QIWConstants.EOS_BYTE4) {
                                //Data is in secondary buffer. Write it out less
                                //its part of EOS
								totalBytesRead += altBufSize - 1;
                                channelStatus = writeBufToTempFile(tempOutputStream, altBufSize-1, primBuf==1 ? buf2 : buf1);
                                break;
                            }
                        } else
                        if (splitCnt == 2) {
                            if (buf.get(bytesRead-2) == QIWConstants.EOS_BYTE3 &&
                                buf.get(bytesRead-1) == QIWConstants.EOS_BYTE4) {
                                //Data is in secondary buffer. Write it out less
                                //its part of EOS
								totalBytesRead += altBufSize - 2;
                                channelStatus = writeBufToTempFile(tempOutputStream, altBufSize-2, primBuf==1 ? buf2 : buf1);
                                break;
                            }
                        } else
                        if (splitCnt == 1) {
                            if (buf.get(bytesRead-1) == QIWConstants.EOS_BYTE4) {
                                //Data is in secondary buffer. Write it out less
                                //its part of EOS
								totalBytesRead += altBufSize - 3;
                                channelStatus = writeBufToTempFile(tempOutputStream, altBufSize-3, primBuf==1 ? buf2 : buf1);
                                break;
                            }
                        }
						logger.info("INTERNAL ERROR: fraction of EOS doesn't match");
                        //??Possible to read <=-3 bytes that are not rest of EOS
                    }
                    
                    //If more than 3 bytes were read, detection of partial EOS was false
                    //Write out the secondary buffer, but then check if primary
                    //buffer contains all or part of EOS
                    if (splitEOS && bytesRead > 3) {
						logger.info("false EOS; splitCnt="+splitCnt);
                        splitEOS = false;
                        splitCnt = 0;
                    }
                    
                    //Check if EOS in primary buffer
                    if (buf.get(bytesRead-4) == QIWConstants.EOS_BYTE1 &&
                        buf.get(bytesRead-3) == QIWConstants.EOS_BYTE2 &&
                        buf.get(bytesRead-2) == QIWConstants.EOS_BYTE3 &&
                        buf.get(bytesRead-1) == QIWConstants.EOS_BYTE4) {
                        bytesRead -= Integer.SIZE/8;

                        //Write out the secondary buffer, if not empty
                        if (altBufSize != 0) {
                            totalBytesRead += altBufSize;
                            channelStatus = writeBufToTempFile(tempOutputStream, altBufSize, primBuf==1 ? buf2 : buf1);
                        }

                        //Write out the primary buffer
                        totalBytesRead += bytesRead;
                        channelStatus = writeBufToTempFile(tempOutputStream, bytesRead, primBuf==1 ? buf1 : buf2);
                        
                        break;
                    }
                    
                    //check if EOS POSSIBLY split, i.e., partial EOS in primary buffer
                    if (buf.get(bytesRead-3) == QIWConstants.EOS_BYTE1 &&
                        buf.get(bytesRead-2) == QIWConstants.EOS_BYTE2 &&
                        buf.get(bytesRead-1) == QIWConstants.EOS_BYTE3) {
                        splitEOS = true;
                        splitCnt = 1;
                    } else
                    if (buf.get(bytesRead-2) == QIWConstants.EOS_BYTE1 &&
                        buf.get(bytesRead-1) == QIWConstants.EOS_BYTE2) {
                        splitEOS = true;
                        splitCnt = 2;
                    } else
                    if (buf.get(bytesRead-1) == QIWConstants.EOS_BYTE1) {
                        splitEOS = true;
                        splitCnt = 3;
                    }
                    
                    //Write out the secondary buffer, if not empty
                    if (altBufSize != 0) {
                        totalBytesRead += altBufSize;
                        channelStatus = writeBufToTempFile(tempOutputStream, altBufSize, primBuf==1 ? buf2 : buf1);
                    }
                    //Set up buffer for next read. Make secondary buffer primary
                    buf = primBuf==1 ? buf2 : buf1;
                    primBuf = primBuf==1 ? 2 : 1;
                    altBufSize = bytesRead;
                }
            }
        } catch (IOException ioe) {
            channelStatus = ioe.getMessage();
        } finally {
            logger.info("readFromChannel: totalBytesRead="+totalBytesRead+" on port # "+clientSocketChannel.socket().getLocalPort());
            if (!isStatusNominal(channelStatus)) {
                channelStatus = channelStatus + "; ";
            }
            //NOTE: It is not necessary to notify the server the socket is
            //no longer needed nor release the client socked for reuse. Both 
            //will be done when the caller calls teardownSocketChannel().

            if (tempOutputStream != null) {
                try {
                    tempOutputStream.close();
                } catch (IOException ioe) {
                    logger.warning("While attempting to close temp FileOutputStream, caught: " + ioe.getMessage());
                }
            }
        }
        logger.info("ReadFromChannel complete, returning channelStatus: '" + channelStatus + "'.");
        return channelStatus;
    }
    
    /**
     * Write ByteBuffer to temp file.
     * @param tempoutputStream Temp file to write out byte buffer
     * @param len Number of bytes to write out
     * @param buf Byte buffer to be written out
     * @return Empty string if no write exception; otherwise, exception message
     */
    private String writeBufToTempFile(FileOutputStream tempOutputStream, int len, ByteBuffer buf) {
        String status = "";
        byte[] chunkbuffer = new byte[len];

        //Extract data from byte buffer
        try {
            //buf.position(0);
            buf.flip();
            buf.get(chunkbuffer);
            buf.clear();
        } catch (BufferUnderflowException bue) {
            logger.info("writeBufToTempFile: Cannot get bytes out of ByteBuffer:"+bue.getMessage());
           chunkbuffer = null; // clue the GC
            return "Tried to get " + len + " from ByteBuffer but caught: " + bue.getMessage();
        }

        //Write data to temp file
        try {
            tempOutputStream.write(chunkbuffer);
            tempOutputStream.flush();
        } catch (IOException ioe) {
            logger.info("writeBufToTempFile: error writing to temp file: "+ioe.getMessage());
            status = ioe.getMessage();
        }
        chunkbuffer = null; // clue the GC
            
        return status;
    }

    /**
     * Set up a socket channel for reading. A convenience method.
     * Connect to the port assigned by the server.
     *
     * @return empty string if read successful; otherwise, reason why read
     * failed.
     */
    public String setupSocketChannel() {
        String channelStatus = establishSocketChannel(QIWConstants.RESPONSE_TIMEOUT);
        
        if (clientSocketChannel.isRegistered()) {
            return channelStatus;
        }

        if (isStatusNominal(channelStatus)) {
            try {
                //create a readiness selector for the socket channel.
                registerSelector();
            } catch (IOException ioe) {
                channelStatus = ioe.getMessage();
                logger.warning("Caught exception while attempting to register selector for open channel on port: " + serverPort + ", " + channelStatus);
            }
            
            if (selector == null) {
                channelStatus = "Cannot read data from socket because selector is null";
            }
        }
        return channelStatus;
    }

    /**
     * Tear down a socket channel used for reading a buffer at a time. A 
     * convenience method. Release if for reuse. Notify the server the assigned
     * server port can be reused.
     */
    public void teardownSocketChannel() {
        //Notify server finished with server port it assigned
        sendSocketNotification(serverPort);
        socketManager.releaseSocket(clientSocketChannel, selector, serverURL, serverPort);
    }
    
    /**
     * Notify the server the assigned socked is no longer needed.
     * @param serverPort Assigned server port that is now available for reuse.
     */
    private void sendSocketNotification(int serverPort) {
        String msgDispCID = MessageDispatcher.getInstance().getCID();
        String servletDispCID = MessageDispatcher.getInstance().getRuntimeServletDispDesc().getCID();
        IQiWorkbenchMsg request = new QiWorkbenchMsg(msgDispCID, servletDispCID, QIWConstants.CMD_MSG, QIWConstants.RELEASE_SERVER_SOCKET_CMD, MsgUtils.genMsgID(), QIWConstants.STRING_TYPE, String.valueOf(serverPort));
		logger.info("Client finished with server port "+serverPort+". Releasing  for reuse.");
        IQiWorkbenchMsg response = DispatcherConnector.getInstance().sendRequestMsg(request);
    }

    /**
     * Method for use by geoIO, which consumes socket input one buffer at a time..
     * Note: Prior to usage, the client must invoke {@link setupSocketChannel()}
     * and after all the data has been read, invoke {@link teardownSocketChannel()}
     * Note 2: This method blocks for up to 10 seconds while waiting for the selector
     * to get keys.
     *
     * @param buf ByteBuffer intended to be backed by a direct memory byte array.  See {@link ByteBuffer} in the Java 1.5 SDK JavaDocs.
     * @return the number of bytes read, possibly zero, or -1 if the channel has reached end-of-stream.
     */
    public int read(ByteBuffer buf) throws IOException {
        //NOTE: establishSocketChannel was called when setupSocketChannel was called

        return clientSocketChannel.read(buf);
    }

    private void waitAndLogInterrupt(int timeout) {
        try {
            //Changed wait time from 400ms --> 100ms Woody Folsom 1/10/8
            Thread.sleep(Math.min(100, timeout / 20));
        } catch (InterruptedException ie) {
            logger.warning("While waiting for socket data, caught Exception: " + ie.getMessage());
        }
    }

    /**
     * Repeatedly calls no-arg establishSocketChannel(), catching any IOExceptions or
     * ClosedChannelExceptions thrown. The current thread sleeps for 1/20 of the
     * timeout value between invocations of socketChannel().
     *
     * @param timeout the maximum amount of time to attempt to open the SocketChannel (100-60,000 ms).
     * @return the SocketChannel status returned by no-arg establishSocketChannel()
     */
    private String establishSocketChannel(int timeout) {
        String socketChannelStatus = NOMINAL_STATUS;
        String errorMessage = "Unable to open socket channel: ";

        if (timeout < 100 || timeout > QIWConstants.RESPONSE_TIMEOUT) {
            throw new IllegalArgumentException("Timout must be greater than 0 and <= " + QIWConstants.RESPONSE_TIMEOUT + " ms");
        }
        int nAttempts = 0;
        boolean exceptionCaught;
        long elapsedTime = 0;
        long startTime = System.currentTimeMillis();
        do {
            exceptionCaught = false;
            try {
                nAttempts++;
                logger.info("Making attempt #" + nAttempts + " to open SocketChannel on port " + serverPort);
                socketChannelStatus = establishSocketChannel();
                //if established socket connection, finished
                if (socketChannelStatus.equals(NOMINAL_STATUS)) {
                    break;
                }
            } catch (ClosedChannelException cce) {
                socketChannelStatus = errorMessage + cce.getMessage();
                exceptionCaught = true;
            } catch (IOException ioe) {
                socketChannelStatus = errorMessage + ioe.getMessage();
                exceptionCaught = true;
            }

            if (exceptionCaught) {
                try {
                    Thread.sleep(Math.min(250, timeout / 20));
                } catch (InterruptedException ie) {
                    logger.info("Caught " + ie.getMessage());
                }
            }
            elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime >= timeout) {
                break;
            }
        } while (exceptionCaught);

        return socketChannelStatus;
    }
}

