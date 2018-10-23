// https://searchcode.com/api/result/43248401/

/*
 * BEGIN_HEADER - DO NOT EDIT
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://open-jbi-components.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://open-jbi-components.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */

/*
 * @(#)ResponsePoller.java 
 *
 * Copyright 2004-2007 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * END_HEADER - DO NOT EDIT
 */
package com.sun.jbi.ftpbc;

import com.sun.jbi.ftpbc.connection.Connection;
import com.sun.jbi.ftpbc.connection.ConnectionPool;
import com.sun.jbi.ftpbc.ftp.FtpFileConfigConstants;
import com.sun.jbi.ftpbc.ftp.FtpInterface;
import com.sun.jbi.ftpbc.ftp.FtpFileClient;
import com.sun.jbi.ftpbc.ftp.FtpFileProvider;
import com.sun.jbi.ftpbc.extensions.FTPAddress;
import com.sun.jbi.ftpbc.extensions.FTPMessageExtension;
import com.sun.jbi.ftpbc.extensions.FTPTransfer;
import com.sun.jbi.ftpbc.extensions.FTPTransferExtension;
import com.sun.jbi.ftpbc.extensions.ProxyAddressURL;
import com.sun.jbi.ftpbc.ftp.FtpFileConfiguration;
import com.sun.jbi.ftpbc.ftp.FtpFileTransferNamesAndCommands;
import com.sun.jbi.ftpbc.ftp.connection.FTPBCConnectionManager;
import com.sun.jbi.ftpbc.ftp.exception.FtpFileException;
import com.sun.jbi.ftpbc.persistence.FTPBCPersistStore;
import com.sun.jbi.ftpbc.util.AlertsUtil;
import com.sun.jbi.ftpbc.util.FTPInputStreamWrapper;
import com.sun.jbi.ftpbc.util.NMPropertyUtil;
import com.sun.jbi.ftpbc.util.Utils;

import com.sun.jbi.bindings.synchronization.CompositeLockRegistry;
import com.sun.jbi.bindings.synchronization.CompositeLock;
import com.sun.jbi.alerter.NotificationEvent;
import com.sun.jbi.common.qos.messaging.MessagingChannel;
import com.sun.jbi.internationalization.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jbi.messaging.*;
import javax.xml.namespace.QName;

import net.java.hulp.measure.Probe;

/*******************************************************************************
 * This class polling a message from a remote location,
 * and send it back to NMR as response.
 *
 * LifeCycle notes:
 *
 * ResponsePoller instances are associated with the OutboundMessageProcessor that
 * started it, ResponsePoller keeps a reference to the OutboundMessageProcessor,
 * and will periodically check OutboundMessageProcessor.isRequestedToStop() to break
 * out of the response polling loop in the run() method.
 * i.e. when a OutboundMessageProcessor is stoped, so does its associated pollers.
 *
 * so far, there is no cap on the number of pollers an outbound msg processor can spawn off,
 * but a configuration parameter could be introduced for this later.
 *
 * Note, the QoS for inbound messaging recovery is not supported in the response
 * poller, due to the unavailability of the context when the poller was terminated,
 * response poller was polling for a response as part of a synchronous request/response
 * processing, it could be triggerred by a bpel invoke, e.g.
 * 
 * when the runtime crashed, for example, and restarted, the message exchange and 
 * the context of message exchange etc. are no longer available, hence make it difficult
 * to resume the request/response processing at the point where it is terminated.
 * 
 * @author jfu
 *
 ******************************************************************************/
public class ResponsePoller implements Runnable {

    private static Messages mMessages =
            Messages.getMessages(ResponsePoller.class);
    private static Logger mLogger = Messages.getLogger(ResponsePoller.class);
    private MessagingChannel mChannel;
    private FTPNormalizer mNormalizer = null;
    private InOut mInOut;
    private FTPTransferExtension mExtElem;
    private FTPAddress mFtpAddress;
    private QName mOperation;
    private Endpoint mEndpoint;
    // the outbound processor that spin off this response
    // poller, there can be many response pollers
    // spinned off from one outbound processor.
    private OutboundMessageProcessor mOBProc;
    private RuntimeConfiguration mRtCfg;
    private ProxyAddressURL mProxy;
    // the UUID passed in when the response poller is spawn
    // when this is NULL, then poll the target using whatever
    // target spec has;
    private String mUUID;
    private AtomicBoolean bTimedout;
    private AtomicBoolean bFatalError;
    private Timer mTimer;
    // for enforcing invoke timeout of the response poller

    public ResponsePoller(OutboundMessageProcessor obProc,
            MessagingChannel channel,
            InOut inout,
            FTPTransferExtension extElem,
            FTPAddress address,
            Endpoint endpoint,
            RuntimeConfiguration runtimeConfig,
            ProxyAddressURL proxy,
            QName operation,
            String uuid) throws Exception {
        mOBProc = obProc;
        mChannel = channel;
        mInOut = inout;
        mExtElem = extElem;
        mFtpAddress = address;
        mEndpoint = endpoint;
        mOperation = operation;
        mProxy = proxy;
        mRtCfg = runtimeConfig;
        mNormalizer = new FTPNormalizer();
        mUUID = uuid; // UUID used for request / response correlate
        bTimedout = new AtomicBoolean();
        bFatalError = new AtomicBoolean();
        mTimer = new Timer();
    }

    /**********************************************************************
     * A response poller thread will poll the expected
     * response at a fixed interval (5000 ms) until:
     * 
     * (1) the expected response polled, the message is normalized, send into NMR, the poller terminates
     * (2) the timeout expired, poller terminates
     * (3) the associated outbound processor terminated, log info and the poller terminates
     * 
     *********************************************************************/
    public void run() {
        FtpInterface ftp = null;
        FtpFileClient client = null;
        FtpFileProvider provider = null;
        FtpFileConfiguration config = null;

        Connection connection = null;
        Semaphore sema = null;
        String targetDir = null;
        String connKey = null;
        String semaKey = null;
        Properties params = null;

        try {
            mTimer.schedule(new FlagResponsePollerTimedOutTask(), mRtCfg.getInvokeTimeout());
        } catch (IllegalArgumentException iae) {
            // the delay value might be bad
            reportError(mInOut,
                    mEndpoint,
                    FTPBCComponentContext.FAULTCODE_SERVER,
                    iae,
                    "FTPBC-E004071",
                    "FTPBC-E004071.Exception_Sched_Resp_Poller_Timeout",
                    new Object[]{
                        mEndpoint.getUniqueName(),
                        mFtpAddress.toString(),
                        mExtElem.toString(),
                        mUUID,
                        mRtCfg.getInvokeTimeout(),
                        iae.getLocalizedMessage()
                    });

            // exit this response poller
            return;
        } catch (IllegalStateException ise) {
            // the timer already canceled, e.g.
            reportError(mInOut,
                    mEndpoint,
                    FTPBCComponentContext.FAULTCODE_SERVER,
                    ise,
                    "FTPBC-E004071",
                    "FTPBC-E004071.Exception_Sched_Resp_Poller_Timeout",
                    new Object[]{
                        mEndpoint.getUniqueName(),
                        mFtpAddress.toString(),
                        mExtElem.toString(),
                        mUUID,
                        mRtCfg.getInvokeTimeout(),
                        ise.getLocalizedMessage()
                    });

            // exit this response poller
            return;
        }

        try {
            params = new Properties();
            FtpClientParameterGenerator.createProperties(false,
                    params,
                    mFtpAddress,
                    mExtElem,
                    mProxy,
                    mOperation,
                    mUUID, // uuid must be available if messageCorrelate is enabled
                    true, // must be consumer doing a sync invoke - polling response
                    mMessages,
                    mLogger);
            params.put(FtpFileConfigConstants.C_P_FTP_PASSIVE_ON, mRtCfg.getUsePassiveFTP() != null && mRtCfg.getUsePassiveFTP().booleanValue() ? "Yes" : "No");
        } catch (Exception ex) {
            if (mTimer != null) {
                mTimer.cancel();
            }
            reportError(mInOut,
                    mEndpoint,
                    FTPBCComponentContext.FAULTCODE_SERVER,
                    ex,
                    "FTPBC-E004072",
                    "FTPBC-E004072.Exception_Setting_Resp_Poller",
                    new Object[]{
                        mEndpoint.getUniqueName(),
                        mFtpAddress.toString(),
                        mExtElem.toString(),
                        ex.getLocalizedMessage()
                    });

            return;
        }

        // pass in the connection configuration parameters
        params.put(ConnectionPool.POOL_MIN_SIZE, mRtCfg.getConnectionPoolMinSize());
        params.put(ConnectionPool.POOL_MAX_SIZE, mRtCfg.getConnectionPoolMaxSize());
        params.put(Connection.CONN_MAX_IDEL_TIMEOUT, mRtCfg.getConnectionMaxIdleTimeout());

        do {

            try {
                connection = FTPBCConnectionManager.getConnection(params);
            } catch (Exception ex) {
                // stop polling response
                if (mTimer != null) {
                    mTimer.cancel();
                }
                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        ex,
                        "FTPBC-E004073",
                        "FTPBC-E004073.Exception_Getting_Connection_From_Pool",
                        new Object[]{
                            mEndpoint.getUniqueName(),
                            mFtpAddress.toString(),
                            mExtElem.toString(),
                            FtpFileConfiguration.getKey(params),
                            ex.getLocalizedMessage()
                        });
                // break out the poller thread
                break;
            }

            if ((ftp = (FtpInterface) connection.getClientObject()) == null) {
                if (mTimer != null) {
                    mTimer.cancel();
                }
                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        null,
                        "FTPBC-E004074",
                        "FTPBC-E004074.[ALERT].FTP_Interface_Not_Available",
                        new Object[]{
                            this.getClass().getName(),
                            FtpFileConfiguration.getKey(params)
                        });
                // break out the poller thread
                break;
            }

            if ((client = ftp.getClient()) == null) {
                if (mTimer != null) {
                    mTimer.cancel();
                }
                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        null,
                        "FTPBC-E004075",
                        "FTPBC-E004075.[ALERT].FTP_Client_Not_Available",
                        new Object[]{
                            this.getClass().getName(),
                            FtpFileConfiguration.getKey(params)
                        });
                // break out the poller thread
                break;
            }

            if ((provider = ftp.getProvider()) == null) {
                if (mTimer != null) {
                    mTimer.cancel();
                }
                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        null,
                        "FTPBC-E004076",
                        "FTPBC-E004076.[ALERT].FTP_Provider_Not_Available",
                        new Object[]{
                            this.getClass().getName(),
                            FtpFileConfiguration.getKey(params)
                        });
                // break out the poller thread
                break;
            }

            if ((config = ftp.getConfiguration()) == null) {
                if (mTimer != null) {
                    mTimer.cancel();
                }
                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        null,
                        "FTPBC-E004084",
                        "FTPBC-E004084.[ALERT].FTP_Config_Not_Available",
                        new Object[]{
                            this.getClass().getName(),
                            FtpFileConfiguration.getKey(params)
                        });
                // break out the poller thread
                break;
            }

            client.setWarningOff(true);
            provider.setWarningOff(true);

            if (mExtElem != null && mExtElem instanceof FTPTransfer) {
                // ftp:transfer still uses regex as receiveFrom value
                provider.setUseRegexAsMatcher(true);
            }

            // also enforce invoke timeout here
            // i.e. we won't allow a response poller
            // thread polling for ever;

            targetDir = config.getTargetDirectoryName();
            connKey = connection.getKey();
            // when target dir is a pattern, the synch range covers all derived dirs
            semaKey = connKey.concat(targetDir);
            // obtain the semaphore associated
            // with the target directory
            // to be polled
            sema = RemoteTargetSemaphoreManager.get(semaKey);

            try {
                // list target directory
                // should be synchronized among
                // concurrent pollers to ensure
                // a consistent snapshot of the
                // entries, the semaphore serves
                // the purpose.
                sema.acquire();
            } catch (InterruptedException ex) {
                // this thread is interrupted
                if (mTimer != null) {
                    mTimer.cancel();
                }

                if (connection != null) {
                    try {
                        FTPBCConnectionManager.returnConnection(connection);
                    } catch (Exception ex2) {
                        if (mLogger.isLoggable(Level.WARNING)) {
                            mLogger.log(Level.WARNING,
                                    "FTPBC-E004080.[ALERT].Exception_return_connection",
                                    new Object[]{
                                        connKey,
                                        ex2.getLocalizedMessage()
                                    });
                        }
                    }
                }

                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        null,
                        "FTPBC-E004078",
                        "FTPBC-E004078.[ALERT].Thread_Interrupted_Sema_Acquire",
                        new Object[]{
                            Thread.currentThread().getName(),
                            semaKey
                        });
                // break out the poller thread
                break;
            }
            //
            // ENTER CRITICAL REGION
            //
            FtpFileTransferNamesAndCommands tnc = null;
            String dir = null;
            String file = null;
            InputStream is = null;

            try {
                if (!client.isConnected()) {
                    client.connect();
                }
                /***************************************
                 ** if pre transfer operation != NONE
                 ** (it is either COPY or RENAME), then after 
                 ** the pre operation is done, release the semaphore,
                 ** all the subsequent operation: transfer + post 
                 ** operation is performed against the
                 ** moved or copied file.
                 ***************************************/
                tnc = (FtpFileTransferNamesAndCommands) client.getResolvedNamesForGet();

                // try to find the target file
                tnc.resolveTargetLocation();
                file = tnc.getTargetFileName();
                dir = tnc.getTargetDirectoryName();

                if (file != null && file.trim().length() > 0) {
                    // target found :
                    // further perform the PRE transfer operation
                    // if there is one
                    client.doPreTransferGet(tnc);

                    if (config.isPreTransferCommandRename() || config.isPreTransferCommandCopy()) {
                        // the target is moved or copied to a dedicated
                        // location for further processing, from this point
                        // on, the semaphore can be released.

                        // END CRITICAL REGION
                        sema.release();
                        // null the sema so that in finally the sema won't
                        // be released again
                        sema = null;
                        dir = tnc.getPreDirectoryName();
                        file = tnc.getPreFileName();
                        if (file == null || file.trim().length() == 0) {
                            // pre transfer operation specified
                            // but can not resolve the destination 
                            // for the pre operation, this is permenent,
                            // throw exception so that it can be handled
                            // at caller's level
                            throw new Exception(mMessages.getString("FTPBC-E004081.Error_Resolve_Pre_Op_Dest",
                                    new Object[]{
                                        config.getPreTransferCommand(),
                                        Thread.currentThread().getName(),
                                        config.getPreDirectoryName(),
                                        config.getPreDirectoryNameIsPattern(),
                                        config.getPreFileName(),
                                        config.getPreFileNameIsPattern()
                                    }));
                        }
                    }
                    // if original file is NOT copied or moved, then the critical region 
                    // will continue until the transfer operation and post transfer operation
                    // completes

                    if ((is = provider.retrieveFileStream(dir, file)) != null) {
                        boolean normalizeOK = false;
                        boolean setNMpropOK = false;
                        FTPInputStreamWrapper inStream = new FTPInputStreamWrapper(is);
                        Probe normMeasure = Probe.info(getClass(), mEndpoint.getUniqueName(), FTPBindingLifeCycle.PERF_CAT_NORMALIZATION);
                        NormalizedMessage nmsg = null;
                        Exception exception = null;

                        try {
                            try {
                                nmsg = mNormalizer.normalize(mInOut,
                                        mOperation,
                                        mEndpoint,
                                        mExtElem,
                                        inStream,
                                        true,
                                        false);
                                normalizeOK = true;
                            } catch (IOException ex) {
                                // IO exception
                                exception = ex;
                            } catch (Exception ex2) {
                                // mostly caused by malformed message, payload, encoder problem
                                exception = ex2;
                            } finally {
                                if (normMeasure != null) {
                                    normMeasure.end();
                                }
                                // since, the whole content is already loaded
                                // close the inStream
                                try {
                                    inStream.close();
                                } catch (Exception e) {
                                    // ignore
                                }

                            }

                            postProcessingNormalization(mUUID, provider, config, exception, tnc);

                            // do post get operation
                            doPostOperation(ftp, tnc, mExtElem);
                            //client.doPostTransferGet(tnc);

                            // pass nm props to the return message
                            if (mRtCfg.getEnableNMProps().booleanValue()) {
                                // extract ftpbc specific NM properties info
                                // from address and extElement
                                Map nmProps = NMPropertyUtil.extractNMProperties(mFtpAddress, mExtElem);
                                NMPropertyUtil.mergeNMPropertiesResolvedParams4Get(tnc, nmProps, mExtElem instanceof FTPTransfer);
                                NMPropertyUtil.setNMProperties(nmsg, nmProps);
                            }

                            setNMpropOK = true;

                            mInOut.setOutMessage(nmsg);
                            mChannel.send(mInOut);
                            mEndpoint.getEndpointStatus().incrementSentReplies();
                            if (mLogger.isLoggable(Level.FINE)) {
                                mLogger.log(Level.FINE, mMessages.getString("FTPBC-R004009.RMP_Response_received_and_send_on_as_output_4_invoke"));
                            }
                        } catch (Exception e) {
                            if (mTimer != null) {
                                mTimer.cancel();
                                mTimer = null;
                            }

                            if (!normalizeOK) {
                                reportError(mInOut,
                                        mEndpoint,
                                        FTPBCComponentContext.FAULTCODE_SERVER,
                                        e,
                                        "FTPBC-E004069",
                                        "FTPBC-E004069.[ALERT].Exception_Normalize_Response_Resp_Poller",
                                        new Object[]{mEndpoint.getUniqueName(), mFtpAddress.toString(), mExtElem.toString(), mUUID, e.getMessage()});
                            } else if (!setNMpropOK) {
                                reportError(mInOut,
                                        mEndpoint,
                                        FTPBCComponentContext.FAULTCODE_SERVER,
                                        e,
                                        "FTPBC-E004068",
                                        "FTPBC-E004068.[ALERT].Exception_when_applying_NM_props",
                                        new Object[]{mEndpoint.getUniqueName(), mFtpAddress.toString(), mExtElem.toString(), mUUID, e.getMessage()});
                            } else {
                                reportError(mInOut,
                                        mEndpoint,
                                        FTPBCComponentContext.FAULTCODE_SERVER,
                                        e,
                                        "FTPBC-E004094",
                                        "FTPBC-E004094.[ALERT].Exception_Send_Msg_NMR_Resp_Poller",
                                        new Object[]{mEndpoint.getUniqueName(), mFtpAddress.toString(), mExtElem.toString(), mUUID, e.getMessage()});
                            }
                        }
                        break;
                    } else {
                        throw new IOException("Failed to obtain input stream from target file...");
                    }
                }
            } catch (Exception ex) {
                bFatalError.set(true);
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }

                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        ex,
                        "FTPBC-E004051",
                        "FTPBC-E004051.RMP_Unexpected_exception",
                        new Object[]{
                            ex.getLocalizedMessage()
                        });

                if (!client.isConnected()) {
                    // assume something bad happened, 
                    // discard the connection
                    try {
                        connection.discard();
                    } catch (Exception ex2) {
                        // ignore
                        }
                    connection = null;
                }
            } finally {
                // EXIT CRITICL REGION if not yet
                if (sema != null) {
                    sema.release();
                }
                if (connection != null) {
                    try {
                        client.disconnect();
                    } catch (Exception ex) {
                        // ignore
                        }
                    try {
                        FTPBCConnectionManager.returnConnection(connection);
                    } catch (Exception ex2) {
                        // ignore but log warning
                        if (mLogger.isLoggable(Level.WARNING)) {
                            mLogger.log(Level.WARNING,
                                    "FTPBC-E004080.[ALERT].Exception_return_connection",
                                    new Object[]{
                                        connKey,
                                        ex2.getLocalizedMessage()
                                    });
                        }
                    }
                }
            }

            if (bFatalError.get()) {
                // break out the poller loop
                break;
            }

            if (bTimedout.get()) {
                // time out expired, need to break out of the loop
                // and log warning and emit alert
                reportError(mInOut,
                        mEndpoint,
                        FTPBCComponentContext.FAULTCODE_SERVER,
                        null,
                        "FTPBC-R004010",
                        "FTPBC-R004010.RMP_Poller_timed_out",
                        new Object[]{
                            mRtCfg.getInvokeTimeout()
                        });

                // break out the poller loop
                break;
            }

            try {
                Thread.currentThread().sleep(((FTPTransferExtension) mExtElem).getPollInterval() < 1000 ? 1000 : ((FTPTransferExtension) mExtElem).getPollInterval());
            } catch (InterruptedException ex) {
                // log as debug info
                if (mLogger.isLoggable(Level.FINE)) {
                    mLogger.log(Level.FINE,
                            "FTPBC-D004010.Thread_Interrupted_Sleep",
                            new Object[]{
                                Thread.currentThread().getName(),
                                mEndpoint.getUniqueName()
                            });
                }
            }
            // if the associated outbound processor
            // has been requested to stop, so does the response pollers
            // for pending synchronized response.
        } while (!mOBProc.isRequestedToStop());

        if (mLogger.isLoggable(Level.FINE)) {
            mLogger.log(Level.FINE, mMessages.getString("FTPBC-D004009.RMP_Poller_shutdown"));
        }
        if (mLogger.isLoggable(Level.INFO)) {
            mLogger.log(Level.INFO, mMessages.getString("FTPBC-R004011.RMP_Complete_processing"));
        }
    }

    private void doPostOperation(FtpInterface ftp, FtpFileTransferNamesAndCommands tnc, FTPTransferExtension extElem) throws FtpFileException, IOException {
        String workingDirectoryName = null;
        String workingFileName = null;

        if (ftp.getConfiguration().isPreTransferCommandRename()) {
            workingDirectoryName = tnc.getPreDirectoryName();
            workingFileName = tnc.getPreFileName();
        } else {
            workingDirectoryName = tnc.getTargetDirectoryName();
            workingFileName = tnc.getTargetFileName();
        }

        // No qualified file is available to get. Nothing needs to do.
        if (workingDirectoryName.length() == 0 &&
                workingFileName.length() == 0) {
            return;
        }

        // 'None'
        if (tnc.getPostTransferCommand().equalsIgnoreCase(FtpFileConfiguration.CMD_NONE)) {
            return;
        }

        // 'Delete'
        if (tnc.getPostTransferCommand().equalsIgnoreCase(FtpFileConfiguration.CMD_DELETE)) {
            if (!ftp.getProvider().deleteFile(workingDirectoryName, workingFileName)) {
                String msg = mMessages.getString("FTPBC-E004095.ERR_EXT_FTP_ACTION_FAIL",
                        new Object[]{
                            "ResponsePoller:::doPostOperation()",
                            "delete",
                            ftp.getProvider().getReplyString()
                        });
                throw new FtpFileException(msg);
            }
        } else if (tnc.getPostTransferCommand().equalsIgnoreCase(FtpFileConfiguration.CMD_RENAME)) {
            // 'Rename'
            // note: for ftp rename function, if the target file exists,
            //       different ftp servers behave differently.
            //       For UNIX ftp server, the target file just is overwritten without extra message.
            //       For NT ftp server, we'll fail and get exception.
            //       Now we don't do extra work for this, we don't want to define unified behavior,
            //       we just follow the native behavior of the corresponding ftp server.
            if (workingDirectoryName.equals(tnc.getPostDirectoryName()) &&
                    workingFileName.equals(tnc.getPostFileName())) {
                if (mLogger.isLoggable(Level.WARNING)) {
                    mLogger.log(Level.WARNING, mMessages.getString("FTPBC-W004100.WRN_EXT_FTP_OP_TO_SELF", new Object[]{
                                "ResponsePoller:::doPostOperation()",
                                tnc.getPostTransferCommand()}));
                }
            } else {
                String destDir = tnc.getPostDirectoryName();
                String destFile = tnc.getPostFileName();
                if (extElem instanceof FTPMessageExtension) {
                    if (!ftp.getConfiguration().getTargetFileNameIsPattern()) {
                        // literal target, to avoid rename collision
                        // add UUID suffix
                        destFile = destFile.concat(UUID.randomUUID().toString());
                    }
                }
                if (ftp.getProvider().archiveFile(
                        workingDirectoryName,
                        workingFileName,
                        destDir,
                        destFile)) {
                } else {
                    String msg = mMessages.getString("FTPBC-E004095.ERR_EXT_FTP_ACTION_FAIL",
                            new Object[]{
                                "ResponsePoller:::doPostOperation()",
                                "rename",
                                ftp.getProvider().getReplyString()
                            });
                    throw new FtpFileException(msg);
                }
            }
        }
    }

    class FlagResponsePollerTimedOutTask extends TimerTask {

        public void run() {
            bTimedout.set(true);
        }
    }

    /**
     * helper
     * @param me
     * @param e
     * @param msg
     * @param faultCode
     * @param faultDetail
     * @throws MessagingException
     */
    private void setError(MessageExchange me, Throwable e, String msg,
            String faultCode, String faultDetail) throws MessagingException {
        me.setError(new Exception(msg, e));
        me.setProperty(FTPBCComponentContext.PROP_FAULTCODE, faultCode);
        me.setProperty(FTPBCComponentContext.PROP_FAULTSTRING, msg);
        me.setProperty(FTPBCComponentContext.PROP_FAULTACTOR, FTPBindingLifeCycle.SHORT_DISPLAY_NAME);
        me.setProperty(FTPBCComponentContext.PROP_FAULTDETAIL, faultDetail);
        me.setStatus(ExchangeStatus.ERROR); // MessagingException can be thrown here, but me has been populated with other properties
    }

    private void reportError(MessageExchange mex, Endpoint endpoint, String faultCode, Throwable exp, String msgCode, String msgKey, Object[] msgParms) {
        String msg = mMessages.getString(
                msgKey, msgParms);

        String detail = Utils.getStackTrace(exp);

        // log error including the stack of the original throwable
        if (mLogger.isLoggable(Level.SEVERE)) {
            mLogger.log(Level.SEVERE, msg + "\r\n" + detail);
        }

        // send alert, do not include stack trace for alert
        // to reduce the traffic
        AlertsUtil.getAlerter().critical(
                msg,
                FTPBindingLifeCycle.SHORT_DISPLAY_NAME,
                endpoint != null ? endpoint.getServiceUnitID() : "NULL ENDPOINT",
                AlertsUtil.getServerType(),
                AlertsUtil.COMPONENT_TYPE_BINDING,
                NotificationEvent.OPERATIONAL_STATE_RUNNING,
                NotificationEvent.EVENT_TYPE_ALERT,
                msgCode);

        try {
            setError(mex, exp, msg, faultCode, detail);
        } catch (MessagingException me) {
            msg = mMessages.getString(
                    "FTPBC-E004067.[ALERT].Exception_when_create_fault",
                    new Object[]{me.getLocalizedMessage()});
            if (mLogger.isLoggable(Level.SEVERE)) {
                mLogger.log(Level.SEVERE, msg + "\r\n" + Utils.getStackTrace(me));
            }
            AlertsUtil.getAlerter().critical(
                    msg,
                    FTPBindingLifeCycle.SHORT_DISPLAY_NAME,
                    endpoint != null ? endpoint.getServiceUnitID() : "NULL ENDPOINT",
                    AlertsUtil.getServerType(),
                    AlertsUtil.COMPONENT_TYPE_BINDING,
                    NotificationEvent.OPERATIONAL_STATE_RUNNING,
                    NotificationEvent.EVENT_TYPE_ALERT,
                    "FTPBC-E004067");
            if (endpoint != null) {
                endpoint.getEndpointStatus().incrementSentErrors();
            }
            return;
        }

        try {
            mChannel.send(mex);
        } catch (MessagingException me) {
            msg = mMessages.getString(
                    "FTPBC-E004061.[ALERT].Exception_when_send_error",
                    new Object[]{me.getLocalizedMessage()});
            if (mLogger.isLoggable(Level.SEVERE)) {
                mLogger.log(Level.SEVERE, msg + "\r\n" + Utils.getStackTrace(me));
            }
            AlertsUtil.getAlerter().critical(
                    msg,
                    FTPBindingLifeCycle.SHORT_DISPLAY_NAME,
                    endpoint != null ? endpoint.getServiceUnitID() : "NULL ENDPOINT",
                    AlertsUtil.getServerType(),
                    AlertsUtil.COMPONENT_TYPE_BINDING,
                    NotificationEvent.OPERATIONAL_STATE_RUNNING,
                    NotificationEvent.EVENT_TYPE_ALERT,
                    "FTPBC-E004061");
        } finally {
            if (endpoint != null) {
                endpoint.getEndpointStatus().incrementSentErrors();
            }
        }
    }

    private void postProcessingNormalization(String uniqMsgID,
            FtpFileProvider provider,
            FtpFileConfiguration config,
            Exception exception,
            FtpFileTransferNamesAndCommands tnc)
            throws FtpFileException, IOException, Exception {
        if (exception != null) {
            if (exception instanceof IOException) {
                // IO problem
                // best effort to bring FTP channel to a sync state
                try {
                    provider.completePendingCommand();
                } catch (Exception ex2) {
                    // ignore
                    ex2.printStackTrace();
                }
                throw new FtpFileException("IO Errro while normalizing message, e=" + exception.getLocalizedMessage(), exception);
            } else {
                // malformed message etc
                // do not subject to recovery
                if (!provider.completePendingCommand()) {
                    throw new FtpFileException(mMessages.getString("FTPBC-E004082.Error_Complete_Pending_FTP_Cmd",
                            new Object[]{
                                provider.getReplyCode(),
                                provider.getReplyString()
                            }));
                }
                // then mark the malformed message as error
                // only when it is in inbound staging area
                if (config.isPreTransferCommandRename() || config.isPreTransferCommandCopy()) {
                    if (!provider.archiveFile(tnc.getPreDirectoryName(),
                            tnc.getPreFileName(),
                            tnc.getPreDirectoryName(),
                            tnc.getPreFileName().concat(FTPBCPersistStore.MESSAGE_ERROR_LOG_SUFFIX))) {
                        // log error
                        String msg = mMessages.getString("FTPBC-E004091.[ALERT].ERR_EXT_FTP_ACTION_FAIL",
                                new Object[]{
                                    "mark as malformed message",
                                    "rename",
                                    provider.getReplyString()
                                });
                        if (mLogger.isLoggable(Level.SEVERE)) {
                            mLogger.log(Level.SEVERE, msg);
                        }
                        throw new FtpFileException(msg);
                    } else {
                        logAndAlert(Level.SEVERE,
                                "FTPBC-E004093",
                                "FTPBC-E004093.[ALERT].MALFORMED_MSG_WHEN_NORMALIZATION",
                                new Object[]{
                                    this.getClass().getName(),
                                    uniqMsgID,
                                    tnc.getPreDirectoryName(),
                                    tnc.getPreFileName().concat(FTPBCPersistStore.MESSAGE_ERROR_LOG_SUFFIX)
                                },
                                null);
                    }
                }
                // save error info for this message exchange
                try {
                    CompositeLock l = CompositeLockRegistry.get(mEndpoint.getOperationUUID(mOperation));
                    Utils.saveMessageNormalizationFailureInfo(((FTPBCPersistStore) l.getPersistStore()).getMessageErrorDir4Norm(true), tnc, uniqMsgID, exception);
                } catch (Exception ex) {
                    // just log the error, continue with the regular error handling
                    if (mLogger.isLoggable(Level.SEVERE)) {
                        mLogger.log(Level.SEVERE,
                                "FTPBC-E004090.Exception_Saving_Malformed_Message_Info",
                                new Object[]{
                                    "normalization",
                                    uniqMsgID,
                                    ex.getLocalizedMessage()});
                    }
                }
                // continue the normal error handling
                throw exception;
            }
        } else {
            if (!provider.completePendingCommand()) {
                throw new FtpFileException(mMessages.getString("FTPBC-E004082.Error_Complete_Pending_FTP_Cmd",
                        new Object[]{
                            provider.getReplyCode(),
                            provider.getReplyString()
                        }));
            }
        }
    }

    private void logAndAlert(Level logLevel, String keyCode, String key, Object[] parms, Exception ex) {
        String msg = parms != null ? mMessages.getString(key, parms) : mMessages.getString(key);
        if (mLogger.isLoggable(logLevel)) {
            if (ex != null) {
                mLogger.log(logLevel, msg, ex);
            } else {
                mLogger.log(logLevel, msg);
            }
        }
        AlertsUtil.getAlerter().critical(msg,
                FTPBindingLifeCycle.SHORT_DISPLAY_NAME,
                mEndpoint.getUniqueName(),
                AlertsUtil.getServerType(),
                AlertsUtil.COMPONENT_TYPE_BINDING,
                NotificationEvent.OPERATIONAL_STATE_RUNNING,
                NotificationEvent.EVENT_TYPE_ALERT,
                keyCode);
    }
}

