// https://searchcode.com/api/result/49187286/

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
 * @(#)AsyncXmlHttpJBIProvider.java 
 *
 * Copyright 2004-2007 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * END_HEADER - DO NOT EDIT
 */

package com.sun.jbi.httpsoapbc.jaxwssupport;

import com.sun.jbi.httpsoapbc.Denormalizer;
import com.sun.jbi.httpsoapbc.Endpoint;
import com.sun.jbi.httpsoapbc.HttpSoapBindingLifeCycle;
import com.sun.jbi.httpsoapbc.HttpSoapComponentContext;
import com.sun.jbi.httpsoapbc.InboundMessageProcessor;
import com.sun.jbi.httpsoapbc.MessageExchangeSupport;
import com.sun.jbi.httpsoapbc.Normalizer;
import com.sun.jbi.httpsoapbc.OperationMetaData;
import com.sun.jbi.httpsoapbc.ReplyListener;
import com.sun.jbi.httpsoapbc.util.DebugLog;
import com.sun.jbi.internationalization.Messages;
import com.sun.jbi.nms.exchange.ExchangePattern;
import com.sun.xml.ws.api.server.AsyncProvider;
import com.sun.xml.ws.api.server.AsyncProviderCallback;

import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import javax.activation.DataSource;
import javax.annotation.Resource;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import net.java.hulp.measure.Probe;

/**
 * JAX-WS Provider implementation for use in Java SE. 
 *
 * Ties together JBI and JAX-WS: JAX-WS invokes this with data as a DataSource, 
 * which this class then normalizes before sending it to the NMR. 
 * When the response comes back from the NMR, the reply handling is triggered,
 * this denormalizes the message and calls back JAX-WS to proceed with the response.
 */
@WebServiceProvider
@ServiceMode(Service.Mode.MESSAGE)
public class AsyncXmlHttpJBIProvider implements AsyncProvider<javax.activation.DataSource>, ReplyListener {

    private static final Messages mMessages =
        Messages.getMessages(AsyncXmlHttpJBIProvider.class);
    private static final Logger mLogger =
        Messages.getLogger(AsyncXmlHttpJBIProvider.class);    
    
    private Endpoint targetEndpoint;
    private HttpSoapBindingLifeCycle lifeCycle = null;
    private ReplyListener replyListener; 

    /**
     * A mapping from the JBI message exchange ID to the request context
     */
    //TODO: find non-synchronized way of doing this
    Map exchangeIDToContext = Collections.synchronizedMap(new HashMap());    
    
    public AsyncXmlHttpJBIProvider(Endpoint endpoint) {
        targetEndpoint = endpoint;
        lifeCycle = (HttpSoapBindingLifeCycle) HttpSoapComponentContext.getInstance().getAssociatedLifeCycle();
        replyListener = this; 
    }    

    /**
     * Ptocess the Provider invoke
     * @param request the Provider reqeust
     * @return the provider response
     */
    public void invoke(DataSource request, AsyncProviderCallback asyncProviderCallback, WebServiceContext webserviceContext) {
        // TODO: reduce and remove logging after prototyping
        if (mLogger.isLoggable(Level.FINE)) {
            mLogger.log(Level.FINE, "Processing SOAPMessage received in AsyncProvider. WebServiceContext: " + webserviceContext);
        }
        
        try {
            // Determine HTTP request method
            MessageContext context = webserviceContext.getMessageContext();
            String reqMethod = (String) context.get(MessageContext.HTTP_REQUEST_METHOD);
            InboundMessageProcessor anInboundProcessor = new InboundMessageProcessor(getProcessorSupport(reqMethod).normalizer, this);
            anInboundProcessor.setInput(request);
            anInboundProcessor.setTargetEndpoint(targetEndpoint);
            anInboundProcessor.setMessageContext(context);
            String exchangeID = anInboundProcessor.execute(asyncProviderCallback);
        } catch (Exception e) {
            mLogger.log(Level.SEVERE,
                        mMessages.getString("HTTPBC-W00625.Exception_during_request_processing"),
                        e);
            asyncProviderCallback.sendError(e);
        }
       
    }
    
    /**
     * The inbound message processor will call us back in execute() once it knows the message exchange for the request.
     * @see ReplyListener
     */
    public void setMessageExchangeId(String messageExchangeId, Object clientContext) {
        exchangeIDToContext.put(messageExchangeId, clientContext);
    }    
    
    
    public void setMessageContextForCallback(Object obj1, Object obj2) {
        // do nothing
    }

    
    /**
     * Removes a message exchange ID and its associated call back context
     * @see ReplyListener
     */
    public void removeMessageExchangeId(String messageExchangeId) {
        exchangeIDToContext.remove(messageExchangeId);
    }    
    
    /**
     * Handle the reply available from JBI.
     */
    public void onReply(MessageExchange exchange) throws MessagingException {    

        DataSource response = null;    
        
        // MEP is complete, we do not expect any further replies. Remove from MessageExchangeSupport.
        // TODO: is this remove really needed? If so, document why            
        MessageExchangeSupport.removeReplyListener(exchange.getExchangeId());
        AsyncProviderCallback asyncProviderCallback = (AsyncProviderCallback) exchangeIDToContext.remove(exchange.getExchangeId());
        Probe denormalizationMeasurement = null;
        try {
            String pattern = exchange.getPattern().toString().trim();
        
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                // Next step in the message exchange ping-pong game.
                // Prcoess the reponse - whether out message or fualt
                if (ExchangePattern.isInOut(exchange)) {
                    InOut inout = (InOut) exchange;
                    NormalizedMessage outMsgOrFault = inout.getOutMessage();

                    if (outMsgOrFault == null) {
                        Fault aFault = exchange.getFault(); 
                        outMsgOrFault = aFault;
                    }

                    // TODO: it should be possible to disallow/disable this completely for compliance reasons
                    if (mLogger.isLoggable(Level.FINE)) {
                        if (outMsgOrFault != null) {
                            DebugLog.debugLog(mLogger, Level.FINE, "Denormalizing received msg", outMsgOrFault.getContent());
                        } else {
                            mLogger.log(Level.FINE, "Message received is empty.");
                        }
                    }

                    Map nameToMeta = targetEndpoint.getOperationNameToMetaData();
                    String operation = exchange.getOperation().getLocalPart();
                    OperationMetaData operationMetaData = (OperationMetaData) nameToMeta.get(operation);
                    if (operationMetaData == null) {
                        String error = mMessages.getString("HTTPBC-E00667.No_opmeta_for_operation",
                                    exchange.getOperation());
                        throw new MessagingException(error);
                    }

                    Denormalizer denormalizer = cReplyDenormalizer.get();
                    denormalizationMeasurement = Probe.info(getClass(),
                                                            targetEndpoint.getUniqueName(), 
                                                            HttpSoapBindingLifeCycle.PERF_CAT_DENORMALIZATION);
                    response = (DataSource) denormalizer.denormalize(outMsgOrFault, exchange, response, operationMetaData); 
                } else if (ExchangePattern.isInOnly(exchange)) {
                    // For one-way exchanges we do not have to provide a reponse, just call JAX-WS back with null
                    response = null;
                }
                if (mLogger.isLoggable(Level.FINE)) {
                    mLogger.log(Level.FINE, "Calling back JAX-WS asynchronously to proceed with response");
                }
                asyncProviderCallback.send(response);
                
            } else if (exchange.getStatus() == ExchangeStatus.DONE) {
                // The game is over; the partner component
                // is closing the MEP, and so should I.
                // For the following MEPs the following are true:
                //
                // Pattern  me.getRole() me Invariant
                // -------  ------       ------------
                // In       CONSUMER     getInMessage() != null && getError() == null
                // In-out   PROVIDER     getInMessage() != null && 
                //                       (getOutMessage() != null xor getFaultMessage() != null) &&
                //                       getError() == null
                //
                // (the invariant conditions with optional responses are
                // more complex.)
                if (ExchangePattern.isInOnly(exchange)) {
                    // One-way invoke successful, report 202 accepted
                    asyncProviderCallback.send(null);
                }
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                // The game is over; the partner component 
                // is abruptly closing the MEP, and so should I.
                // ME properties will reflect
                // the on-going state of the exchange before it
                // was ended by the partner abruptly.
                //
                // Pattern  me.getRole() me Invariant
                // -------  ------------ ------------
                // In       CONSUMER     getInMessage() != null
                // In-out   CONSUMER     getInMessage() != null
                // In-out   PROVIDER     getInMessage() != null &&
                //                       (getOutMessage() != null xor getFaultMessage() != null )
                Exception errorDetail = null;
                if (exchange.getError() != null) {
                    errorDetail = exchange.getError();
                    String errText = mMessages.getString("HTTPBC-E00720.Message_exchange_error", new Object[] {
                        targetEndpoint.getServiceName(),
                        targetEndpoint.getEndpointName(),
                        errorDetail.getLocalizedMessage(),
                    });
                    errorDetail = new Exception(errText, errorDetail);
                } else {
                    // Provider did not give details about the error, can only generate a very generic failure
                    // TODO: i18n
                    String errText = mMessages.getString("HTTPBC-E00721.Message_exchange_error_no_detail", new Object[] {
                        targetEndpoint.getServiceName(),
                        targetEndpoint.getEndpointName(),
                    });
                    errorDetail = new Exception(errText);
                }
                
                // For inout, convert to our fault format. We may want to consider if we leave this to JAX-WS.
                if (ExchangePattern.isInOut(exchange)) {
                    Denormalizer denormalizer = cReplyDenormalizer.get();
                    response = (DataSource) denormalizer.denormalizeException(errorDetail, response);
                    if (mLogger.isLoggable(Level.FINE)) {
                        mLogger.log(Level.FINE, "Responding to JAX-WS with a fault");            
                    }
                    asyncProviderCallback.send(response);
                } else {
                    if (mLogger.isLoggable(Level.FINE)) {
                        mLogger.log(Level.FINE, "Responding to JAX-WS with the following error", errorDetail);            
                    }
                    asyncProviderCallback.sendError(errorDetail);
                }
            }
            
        } catch (MessagingException e) {
            if (mLogger.isLoggable(Level.WARNING)) {
                mLogger.log(Level.WARNING, mMessages.getString("HTTPBC-E00800.Failed_to_process_reply_from_jbi"), e);
            }
            asyncProviderCallback.sendError(e);
            throw e;
        } finally {
            if (denormalizationMeasurement != null) {
                denormalizationMeasurement.end();
            }
        }
    }

    //TODO: BEST TO REMOVE ALL THREAD LOCAL ARTIFACTS    
    
    /**
     * Get the thread specific processor support
     * Beware: Do not use the processor support instances in a different thread than 
     * the one calling getProcessorSupport. 
     */
    ProcessorSupport getProcessorSupport(String requestMethod) throws MessagingException {
        // Get the processor support instances associated with the thread if present, create if not.
        
        Map<String, ProcessorSupport> methodToProcessor = processorSupport.get();
        ProcessorSupport currentProcSupport = methodToProcessor.get(requestMethod);
        if (currentProcSupport == null) {
            
            currentProcSupport = new ProcessorSupport();
            
            if ("POST".equals(requestMethod)) {
                currentProcSupport.normalizer = new JAXWSXmlHttpPostNormalizer();
            } else if ("GET".equals(requestMethod) || requestMethod != null) {
                currentProcSupport.normalizer = new JAXWSXmlHttpGetNormalizer();
            } else {
                String msg = mMessages.getString("HTTPBC-E00632.No_support_for_request_method", requestMethod);
                throw new MessagingException(msg);
            }
            
            currentProcSupport.denormalizer = new JAXWSXmlHttpDenormalizer();
            currentProcSupport.inboundProcessor = new InboundMessageProcessor(currentProcSupport.normalizer, this);
            methodToProcessor.put(requestMethod, currentProcSupport);
        }
        return currentProcSupport;
    }
    
    /**
     * Holds instances that are not thread safe
     */    
    private static ThreadLocal<Map<String, ProcessorSupport>> processorSupport = 
            new ThreadLocal<Map<String, ProcessorSupport>>() {
        protected Map<String, ProcessorSupport> initialValue() {
            return new HashMap<String, ProcessorSupport>();
        }
    };
    
    private static ThreadLocal<Denormalizer> cReplyDenormalizer = 
            new ThreadLocal<Denormalizer>() {
        protected Denormalizer initialValue() {
            return new JAXWSXmlHttpDenormalizer();
        }
    };

    /**
     * Holds instances that are not thread safe
     */
    static class ProcessorSupport {
        Normalizer normalizer;
        Denormalizer denormalizer;
        InboundMessageProcessor inboundProcessor;        
    }    
}

