// https://searchcode.com/api/result/100320171/

/**
 * e-Science Central Copyright (C) 2008-2013 School of Computing Science,
 * Newcastle University
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation at: http://www.gnu.org/licenses/gpl-2.0.html
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, 5th Floor, Boston, MA 02110-1301, USA.
 */
package com.connexience.server.workflow.cloud;

import com.connexience.server.workflow.api.ApiProvider;
import com.connexience.server.workflow.api.downloaders.HttpDownloader;
import com.connexience.server.workflow.api.uploaders.HttpUploader;
import com.connexience.server.model.workflow.WorkflowInvocationMessage;
import com.connexience.server.util.RegistryUtil;
import com.connexience.server.util.SerializationUtils;
import com.connexience.server.util.provenance.PerformanceLoggerClient;
import com.connexience.server.util.provenance.ProvenanceLoggerClient;
import com.connexience.server.workflow.cloud.execution.CloudWorkflowExecutionEngine;
import com.connexience.server.workflow.cloud.execution.CloudWorkflowExecutionEngineListener;
import com.connexience.server.workflow.cloud.library.ServiceLibrary;
import com.connexience.server.workflow.cloud.library.ServiceLibraryContainer;
import com.connexience.server.workflow.cloud.library.installer.InstallerException;
import com.connexience.server.workflow.cloud.library.installer.UserManager;
import com.connexience.server.workflow.cloud.library.installer.UserManagerFactory;
import com.connexience.server.workflow.engine.WorkflowInvocation;
import com.connexience.server.workflow.util.SigarData;
import org.apache.log4j.Logger;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.pipeline.core.xmlstorage.XmlDataStore;
import org.pipeline.core.xmlstorage.prefs.PreferenceManager;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;

/**
 * This class provides the top level engine that executes workflows and
 * dynamically downloads services and dependencies from the central server.
 *
 * @author hugo
 */
public class CloudWorkflowEngine implements CloudWorkflowExecutionEngineListener, MessageListener, ServiceLibraryContainer, WorkflowJMSListener {

    static Logger logger = Logger.getLogger(CloudWorkflowEngine.class);

    /**
     * Global Engine object
     */
    public static CloudWorkflowEngine SINGLETON = null;

    /**
     * API Provider that creates the appropriate type of API link objects to
     * talk to the central server. These are typically RMI links for normal
     * operation within the cloud, but can be RPC backed links when the engine
     * is being used as a debugging environment in the rich client.
     */
    private ApiProvider apiProvider;
    /**
     * Cloud execution engine that runs the actual workflows
     */
    private CloudWorkflowExecutionEngine engine;
    /**
     * Location to unpack services and dependencies to
     */
    private File libraryDirectory;
    /**
     * JMS Message connection
     */
    private Connection connection = null;
    /**
     * JMS Session
     */
    private Session session = null;
    /**
     * JMS Workflow message topic
     */
    private Queue workflowQueue = null;
    /**
     * JMS Workflow message consumer
     */
    private MessageConsumer consumer = null;
    /**
     * Library of installed services and dependencies
     */
    private ServiceLibrary serviceLibrary;
    /**
     * IS the JMS Thread attached
     */
    private volatile boolean jmsAttached = false;
    /**
     * Thread to re-attach JMS
     */
    private JMSAttachThread jmsAttacher = null;
    /**
     * Control JMS attacher
     */
    private JMSAttachThread controlAttacher = null;
    /**
     * Thread to attach to the updates queue
     */
    private JMSAttachThread updatesAttacher = null;
    /**
     * Control message receiver
     */
    private WorkflowControlJMSReceiver controlReceiver = null;
    /**
     * Has the detachJMS method been called
     */
    private boolean detachJmsCalled = false;

    /**
     * Is this engine running in single VM / workflow mode
     */
    private boolean singleVMPerWorkflowMode = false;

    /**
     * Timeout for waiting for the single VM to start
     */
    private int singleVMCreationTimeout = 60;

    private boolean debuggerAllowed = false;
    private boolean singleVMDebuggingSuspended = false;

    private boolean ipDetectedFromServer = false;
    private String serverSuppliedIP = "";
    
    /**
     * Global performance logging thread
     */
    private PerformanceLoggerThread performanceLogger;
    
    /** Loads from default location */
    public CloudWorkflowEngine() throws RemoteException, IOException {
        CloudWorkflowEngine.SINGLETON = this;
        logger.debug("Loading config from default location");
        Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
        loadProperties(null);
        initEngine();
    }

    /** Loads from specific location */
    public CloudWorkflowEngine(String propertiesRootPath) throws RemoteException, IOException {
        CloudWorkflowEngine.SINGLETON = this;
        boolean fileConfig = true;
        URL u = null;
        try {
            u = new URL(propertiesRootPath);
            String configPath = u.toExternalForm() + "/workflow/config";
            u = new URL(configPath);
            fileConfig = false;
        } catch (Exception e){
            fileConfig = true;
        }
        
        if(fileConfig){
            logger.debug("Loading config from path: " + propertiesRootPath);
            loadProperties(propertiesRootPath);            
            Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
        } else {
            logger.debug("Loading config from server: " + u);
            
            loadPropertiesFromUrl(u);
            Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
        }

        initEngine();
    }

    /** Setup the engine. Properties must have been loaded prior to calling this */
    private void initEngine() throws RemoteException, IOException {
            
        // Gather system data if needed
        if (PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("GatherSystemData", true)) {
            SigarData.SYSTEM_DATA.initialise();
        }

        File libraryDir = new File(PreferenceManager.getSystemPropertyGroup("Engine").stringValue("ServiceLibraryDir",
                System.getProperty("user.home") + File.pathSeparator + "/workflow/library")).getAbsoluteFile();
        checkAndCreateDir(libraryDir, "Creating library directory:");
        setLibraryDirectory(libraryDir);

        singleVMPerWorkflowMode = PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("SingleVMPerWorkflow", false);
        singleVMCreationTimeout = PreferenceManager.getSystemPropertyGroup("Engine").intValue("SingleVMCreationTimeout", 60);
        singleVMDebuggingSuspended = PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("SingleVMDebuggingSuspended", false);

        apiProvider = new ApiProvider();

        apiProvider.setHostName(PreferenceManager.getSystemPropertyGroup("Engine").stringValue("APIHost", "localhost"));
        apiProvider.setHttpPort(PreferenceManager.getSystemPropertyGroup("Engine").intValue("APIPort", 8080));
        apiProvider.setServerContext(PreferenceManager.getSystemPropertyGroup("Engine").stringValue("APIContext", "/workflow"));
        apiProvider.setUseRmi(PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("AllowRMI", true));
        apiProvider.setRmiRegistryPort(PreferenceManager.getSystemPropertyGroup("Engine").intValue("RMIRegistryPort", 2199));
        apiProvider.setUseJMS(PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("UseJMSToSendUpdates", true));

        // Try and work out our IP address
        try {
            serverSuppliedIP = PreferenceManager.getIpAddressFromConfigServer(apiProvider.getHostName(), apiProvider.getHttpPort());
            ipDetectedFromServer = true;
            logger.debug("Server supplied an IP address of: " + serverSuppliedIP);
        } catch (Exception e){
            ipDetectedFromServer = false;
            logger.debug("Server didn't supply an IP address: " + e.getMessage());
        }
        
        serviceLibrary = new ServiceLibrary(this);
        File invocationDir = new File(PreferenceManager.getSystemPropertyGroup("Workflow").stringValue("InvocationStorageDir",
                System.getProperty("user.home") + File.pathSeparator + "/workflow/invocations"));
        checkAndCreateDir(invocationDir, "Creating invocations directory:");
        engine = new CloudWorkflowExecutionEngine(this, invocationDir.getAbsolutePath(), serviceLibrary, apiProvider, this, "IP:" + getServerIp());
        apiProvider.setEngine(engine.getExecutionEngine());
        serviceLibrary.getInformationCache().setEnabled(PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("EnableInformationCache", true));

        engine.setMaxVmSize(PreferenceManager.getSystemPropertyGroup("Workflow").intValue("DefaultVMSize", 256));
        engine.setPermGenSize(PreferenceManager.getSystemPropertyGroup("Workflow").intValue("DefaultMaxPermSize", 256));
        engine.setMaxConcurrentServiceInvocations(PreferenceManager.getSystemPropertyGroup("Workflow").intValue("MaxConcurrentServiceInvocations", 4));
        engine.setMaxConcurrentWorkflows(PreferenceManager.getSystemPropertyGroup("Workflow").intValue("MaxWorkflowCount", 10));
        engine.addCloudWorkflowExecutionEngineListner(this);
        debuggerAllowed = PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("AllowDebugger", false);
        engine.setDebuggingAllowed(debuggerAllowed);

        // Setup engine security features
        boolean enforceWorkflowSeparation = PreferenceManager.getSystemPropertyGroup("Security").booleanValue("EnableWorkflowIsolation", false);
        if (enforceWorkflowSeparation) {
            engine.setWorkflowSeparationEnforced(true);
            String usernamePrefix = PreferenceManager.getSystemPropertyGroup("Security").stringValue("InvocationUserPrefix", "wfuser");
            engine.getInvocationUserMap().setup(usernamePrefix, engine.getMaxConcurrentWorkflows());
            String engineGroupName = PreferenceManager.getSystemPropertyGroup("Security").stringValue("EngineGroup", "staff");
            engine.getInvocationUserMap().setEngineGroupName(engineGroupName);
            engine.setInvocationUserProcessTerminationEnabled(PreferenceManager.getSystemPropertyGroup("Security").booleanValue("KillInvocationUserProcesses", true));

        } else {
            engine.setWorkflowSeparationEnforced(false);
        }

        // Set up RMI names
        if(PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("UserServerSuppliedIPForRMIRegistration", true)){
            logger.debug("Setting RMI hostname to: " + getServerIp());
            System.setProperty("java.rmi.server.hostname", getServerIp());
        } else {
            logger.debug("Using default RMI hostname of: " + System.getProperty("java.rmi.server.hostname"));
        }
        // Workflow lock receiver
        controlReceiver = new WorkflowControlJMSReceiver(engine);

        String tmpPath = PreferenceManager.getSystemPropertyGroup("Engine").stringValue("TemporaryDir",
                System.getProperty("user.home") + File.pathSeparator + "/tmp");
        HttpUploader.setTemporaryDir(tmpPath);
        HttpDownloader.setTemporaryDir(tmpPath);

        performanceLogger = new PerformanceLoggerThread(this);

        performanceLogger.filterConstant = PreferenceManager.getSystemPropertyGroup("Performance").doubleValue("EngineDataFilterConstant", 0.8);
        performanceLogger.alwaysSend = PreferenceManager.getSystemPropertyGroup("Performance").booleanValue("SendEngineDataWhenIdle", true);
        performanceLogger.sampleRate = PreferenceManager.getSystemPropertyGroup("Performance").intValue("EngineDataSampleInterval", 500);
        performanceLogger.sendInterval = PreferenceManager.getSystemPropertyGroup("Performance").intValue("EngineDataSendInterval", 5000);

        if (PreferenceManager.getSystemPropertyGroup("Performance").booleanValue("Enabled", true)) {
            performanceLogger.start();
        }        
    }
    public boolean isSingleVMPerWorkflowMode() {
        return singleVMPerWorkflowMode;
    }

    public int getSingleVMCreationTimeout() {
        return singleVMCreationTimeout;
    }

    public boolean isDebuggerAllowed() {
        return debuggerAllowed;
    }

    public boolean isSingleVMDebuggingSuspended() {
        return singleVMDebuggingSuspended;
    }

    /**
     * Get the server IP address
     */
    public String getServerIp() {
        String ip;
        if (PreferenceManager.getSystemPropertyGroup("Engine").booleanValue("OverrideDetectedLocalIP", false)) {
            ip = PreferenceManager.getSystemPropertyGroup("Engine").stringValue("OverriddenIP", "127.0.0.1");
        } else {
            // Did we get one on startup from config server
            if(ipDetectedFromServer){
                ip = serverSuppliedIP;
            } else {
                try {
                    ip = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception e) {
                    logger.error("Cannot access IP address information: " + e.getMessage(), e);
                    logger.warn("Used localhost(127.0.0.1) for IP address");
                    return "127.0.0.1";
                }
            }
        }
        return ip;
    }

    /**
     * Set up the system properties
     */
    private void createDefaultProperties() {
        // Properties that define the workflow execution behaviour
        PreferenceManager.getSystemPropertyGroup("Workflow").add("MaxWorkflowCount", 10, "Maximum number of workflow jobs this engine will take from the queue at any time");
        PreferenceManager.getSystemPropertyGroup("Workflow").add("InvocationStorageDir", "/workflow/invocations", "Directory to use as workflow scratch space");
        PreferenceManager.getSystemPropertyGroup("Workflow").add("MaxConcurrentServiceInvocations", 4, "Maximum number of concurrent workflow services that are allowed to execute at any time");
        PreferenceManager.getSystemPropertyGroup("Workflow").add("DefaultVMSize", 256, "Maximum JavaVM size in megabytes");
        PreferenceManager.getSystemPropertyGroup("Workflow").add("DefaultMaxPermSize", 256, "Maximum Java PermGen size in megabytes");

        // Properties that define the general engine behaviour
        PreferenceManager.getSystemPropertyGroup("Engine").add("ServiceLibraryDir", "/workflow/library", "Directory to store downloaded services in");
        PreferenceManager.getSystemPropertyGroup("Engine").add("TemporaryDir", "/workflow/temp", "Directory to store temporary files; if unset a system dependent default temporary-file directory will be used");
        PreferenceManager.getSystemPropertyGroup("Engine").add("AllowDebugger", false, "Are workflow blocks allowed to operate in Java debugging mode");
        PreferenceManager.getSystemPropertyGroup("Engine").add("OverrideDetectedLocalIP", false, "Override the detected local IP address when setting the host ID in workflow invocations");
        PreferenceManager.getSystemPropertyGroup("Engine").add("OverriddenIP", "127.0.0.1", "IP address to use when setting the host ID in workflow invocations");
        PreferenceManager.getSystemPropertyGroup("Engine").add("APIHost", "localhost", "Name of the server providing API service");
        PreferenceManager.getSystemPropertyGroup("Engine").add("APIPort", 8080, "Port on the server providing API service");
        PreferenceManager.getSystemPropertyGroup("Engine").add("APIContext", "/workflow", "Base URL on the server providing API service");
        PreferenceManager.getSystemPropertyGroup("Engine").add("AllowRMI", true, "Set to allow the server to attempt RMI communications");
        PreferenceManager.getSystemPropertyGroup("Engine").add("UserServerSuppliedIPForRMIRegistration", true, "Set the rmi hostname property to match the IP address supplied by the server");
        
        PreferenceManager.getSystemPropertyGroup("Engine").add("RMIRegistryPort", 2199, "Port of the RMI registry on the server");
        PreferenceManager.getSystemPropertyGroup("Engine").add("GatherSystemData", true, "Should the engine attempt to gather system data");
        PreferenceManager.getSystemPropertyGroup("Engine").add("UseJMSToSendUpdates", true, "Should the API clients use JMS to send non-blocking updates where possible");
        PreferenceManager.getSystemPropertyGroup("Engine").add("EnableInformationCache", true, "Should the engine enable the various information caches to reduce server calls");
        PreferenceManager.getSystemPropertyGroup("Engine").add("SingleVMPerWorkflow", true, "Should the engine run all of a workflows services in a single VM");
        PreferenceManager.getSystemPropertyGroup("Engine").add("SingleVMCreationTimeout", 60, "Length of time (in seconds) to wait for the single service VM to start");
        PreferenceManager.getSystemPropertyGroup("Engine").add("SingleVMDebuggingSuspended", false, "Should the 'single VM' process suspend waiting for a debugger to connect");

        // Properties that define security settings for engine
        PreferenceManager.getSystemPropertyGroup("Security").add("EnableWorkflowIsolation", false, "Should workflow invocations be isolated from each other");
        PreferenceManager.getSystemPropertyGroup("Security").add("EngineGroup", "staff", "Group that the workflow engine uses to access invocation directories");
        PreferenceManager.getSystemPropertyGroup("Security").add("InvocationUserPrefix", "wfuser", "Prefix to use when creating workflow user names");
        PreferenceManager.getSystemPropertyGroup("Security").add("InvocationUserIDStart", 1000, "Prefix to use when creating workflow user names");
        PreferenceManager.getSystemPropertyGroup("Security").add("InvocationGroup", "wfusers", "Group name for invocation users");
        PreferenceManager.getSystemPropertyGroup("Security").add("InvocationGroupID", 400, "Unix group GID for invocation user primary group");
        PreferenceManager.getSystemPropertyGroup("Security").add("KillInvocationUserProcesses", true, "Should all of the invocation users processes be killed when an invocation finishes");

        // Properties that define the JMS communications
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSQueue", "Workflow", "Name of the JMS queue that this engine listens to");
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSServer", "localhost", "JMS server address");
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSPort", 5445, "JMS communication port");
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSUser", "connexience", "JMS user");
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSPassword", "1234", "JMS password");
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSControlMessageTopic", "WorkflowControl");
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSManagerMessageQueue", "WorkflowManagerQueue", "Queue used to send back status messages");
        PreferenceManager.getSystemPropertyGroup("JMS").add("JMSConsumerWindowSize", 0);

        // Performance logging
        PerformanceLoggerClient.createDefaultProperties();

        // Provenance logging
        ProvenanceLoggerClient.createDefaultProperties();
    }

    /**
     * Load all of the properties from a URL
     */
    private void loadPropertiesFromUrl(URL propertiesUrl){
        createDefaultProperties();
        if(!PreferenceManager.loadPropertiesFromConfigServer(propertiesUrl, "engine", getServerIp(), "engine.xml")){
            logger.error("Could not load config from server");
            System.exit(1);
        }
    }
    
    /**
     * Load all of the properties from the engine.xml file
     */
    private void loadProperties(String rootPath) {
        createDefaultProperties();
        File propertiesFile = new File(
                (rootPath != null ? rootPath : System.getProperty("user.home") + File.separator + ".inkspot")
                + File.separator + "engine.xml");

        if (!PreferenceManager.loadPropertiesFromFile(propertiesFile)) {
            createDefaultProperties();
            PreferenceManager.saveProperties();
        } else {
            PreferenceManager.saveProperties();
        }
    }

    /**
     * Get the actual execution engine
     */
    public CloudWorkflowExecutionEngine getExecutionEngine() {
        return engine;
    }

    /**
     * Get the library directory
     */
    public File getLibraryDirectory() {
        return libraryDirectory;
    }

    /**
     * Set the library directory
     */
    public void setLibraryDirectory(File libraryDirectory) {
        this.libraryDirectory = libraryDirectory;
    }

    @Override
    public void engineShutdownSignalReceived(CloudWorkflowExecutionEngine source, boolean interactive) {
        logger.debug("Engine shutdown signal received");
        detachJms();
        PreferenceManager.saveProperties();

        // Unregister from RMI registry
        try {
            RegistryUtil.unregisterFromRegistry("CloudWorkflowEngine");
        } catch (Exception e) {
            logger.error("Error unregistering control RMI", e);
        }

        // Unregister from RMI registry
        try {
            RegistryUtil.unregisterFromRegistry("APIBroker");
        } catch (Exception e) {
            logger.error("Error unregistering APIBroker RMI", e);
        }

        // Unregister from RMI registry
        try {
            RegistryUtil.unregisterFromRegistry("ProcessNotifier");
        } catch (Exception e) {
            logger.error("Error unregistering process notifier");
        }

        // Stop the logger thread
        performanceLogger.terminate();

        // Exit
        if (!interactive) {
            logger.debug("Exiting");
            System.exit(0);
        }
    }

    public void invocationFinished(WorkflowInvocation invocation) {
    }

    @Override
    public void invocationStarted(WorkflowInvocation invocation) {
    }

    /**
     * Wait until the queue gets down to size
     */
    private void waitForQueueToShrink() {
        int size = engine.getJobQueueSize();
        if (size >= engine.getMaxConcurrentWorkflows()) {
            while (engine.getJobQueueSize() >= engine.getMaxConcurrentWorkflows() && detachJmsCalled == false) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    logger.error("Thread sleep error", e);
                }
            }
        }
    }

    /**
     * Process a JMS workflow start message
     */
    public void onMessage(Message message) {
        logger.debug("JMS Message received");
        if (detachJmsCalled == false && message instanceof BytesMessage) {
            BytesMessage bm = (BytesMessage) message;
            try {
                bm.reset();
                byte[] data = new byte[(int) bm.getBodyLength()];
                bm.readBytes(data);
                Object payload = SerializationUtils.deserialize(data);
                if (payload instanceof WorkflowInvocationMessage) {
                    WorkflowInvocationMessage invocationMessage = (WorkflowInvocationMessage) payload;
                    logger.debug("Workflow Invocation message found for invocation. InvocationID=" + invocationMessage.getInvocationId());
                    engine.startWorkflow(invocationMessage);
                    message.acknowledge();
                }
            } catch (JMSException jmse) {
                logger.error("JMS Exception", jmse);
            } catch (IOException ioe) {
                logger.error("IO Exception", ioe);
            } catch (ClassNotFoundException cnfe) {
                logger.error("Class not found", cnfe);
            }
        } else {
            logger.debug("JMS Message rejected");
        }
        waitForQueueToShrink();
    }

    /**
     * Start the JMS Attachement thread
     */
    public void startJMSAttacherThread(String hostname, int port, String user, String password, String queueName, String lockTopicName, String managerQueueName, Integer bufferSize) {
        // Start the workflow queue attacher thread
        jmsAttacher = new JMSAttachThread(this, hostname, port, user, password, queueName, bufferSize);
        jmsAttacher.start();

        // Start the workflow lock topic attacher thread
        controlAttacher = new JMSAttachThread(controlReceiver, hostname, port, user, password, lockTopicName, bufferSize);
        controlAttacher.start();
        controlReceiver.setJmsAttacher(controlAttacher);

        // Start the API updates queue
        if (apiProvider.isUseJMS()) {
            updatesAttacher = new JMSAttachThread(apiProvider.getJmsHelper(), hostname, port, user, password, managerQueueName, bufferSize);
            updatesAttacher.start();
            apiProvider.getJmsHelper().setJmsAttacher(updatesAttacher);
        }
    }

    public JMSAttachThread getAttacherThread() {
        return jmsAttacher;
    }

    public boolean isJmsAttached() {
        return jmsAttached;
    }

    /**
     * Attach to the JMS server
     */
    public synchronized void attachJms(String hostname, int port, String user, String password, String queueName, Integer bufferSize) throws Exception {
        jmsAttached = true;
        try {
            logger.debug("Attaching JMS to: " + hostname + ":" + port + " on queue: " + queueName);
            Map<String, Object> params = new HashMap<>();
            params.put(TransportConstants.HOST_PROP_NAME, hostname);
            params.put(TransportConstants.PORT_PROP_NAME, port);

            TransportConfiguration configuration = new TransportConfiguration(NettyConnectorFactory.class.getName(), params);
            HornetQConnectionFactory factory = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, configuration);
            if (bufferSize != null) {
                logger.debug("Consumer window size: " + bufferSize);
                factory.setConsumerWindowSize(bufferSize.intValue());
            } else {
                logger.debug("Default consumer window size");
            }

            connection = factory.createConnection(user, password);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            workflowQueue = session.createQueue(queueName);
            consumer = session.createConsumer(workflowQueue);
            consumer.setMessageListener(this);
            connection.setExceptionListener(new ExceptionListener() {
                @Override
                public void onException(JMSException jmse) {
                    if (jmse.getErrorCode().equals("DISCONNECT")) {
                        logger.debug("JMS Detached");
                        jmsAttached = false;
                        jmsAttacher.setInterval(10000);
                    } else {
                        logger.error("Unrecognised JMS Error code: " + jmse.getErrorCode());
                    }
                }
            });
            connection.start();
            logger.debug("JMS Attached: " + queueName);
        } catch (Exception e) {
            jmsAttached = false;
            throw e;
        }
    }

    /**
     * Close the JMS Connection
     */
    public void detachJms() {
        try {
            detachJmsCalled = true;
            logger.debug("Detaching JMS: Workflow");
            jmsAttacher.stop();
            connection.stop();
            connection.close();
            logger.debug("JMS Detached: Workflow");

            // Detach the updates queue if connected
            if (apiProvider.isUseJMS() && updatesAttacher != null) {
                updatesAttacher.stop();
                apiProvider.getJmsHelper().detachJms();
            }
            jmsAttached = false;

            controlReceiver.detachJms();

        } catch (Exception e) {
            logger.error("Error detaching JMS: " + e.getMessage());
        }
    }

    /**
     * Set the API Provider
     */
    public void setApiProvider(ApiProvider apiProvider) {
        this.apiProvider = apiProvider;
    }

    /**
     * Get the API Provider
     */
    public ApiProvider getApiProvider() {
        return apiProvider;
    }

    //////////////////////////
    // 
    // Argument parsing region
    //
    private static class Option {

        public String Name;
        public Boolean WithValue;
        public Boolean Mandatory;
        public String Value;
        public String DefaultValue;
        public String HelpMessage;

        public Option(String name, String helpMessage) {
            Name = name;
            WithValue = false; // if no default value is provided, this must be an option without value
            Mandatory = false; // if this is an option without value, there's no point in it being mandatory
            HelpMessage = helpMessage;
        }

        public Option(String name, String defaultValue, Boolean mandatory, String helpMessage) {
            Name = name;
            DefaultValue = defaultValue;
            WithValue = true;
            Mandatory = mandatory;
            HelpMessage = helpMessage;
        }
    }

    private final static int OptHelp = 0;
    private final static int OptConfigPath = 1;
    private final static Option[] Options = {
        new Option("-help", "Prints the usage information"),
        new Option("-configPath", System.getProperty("user.home") + File.separator + ".inkspot", false, "Indicates the path where user preferences are stored")
    };

    private static void parseArgs(String[] args) {
        Option needsValue = null;
        Boolean readValue = false;

        for (String inputArg : args) {
            if (readValue) {
                needsValue.Value = inputArg;
                readValue = false;
                needsValue = null;
            } else {
                Boolean unknownArg = true;
                for (Option opt : Options) {
                    if (inputArg.equals(opt.Name)) {
                        unknownArg = false;

                        if (opt.WithValue) {
                            readValue = true;
                            needsValue = opt;
                            break;
                        } else {
                            opt.Value = "T"; // indicates that the option is present
                        }
                    }
                }

                if (unknownArg) {
                    throw new RuntimeException("Invalid argument: " + inputArg);
                }
            }
        }

        if (readValue) {
            throw new RuntimeException("Missing value for argument: " + needsValue.Name + "; " + needsValue.HelpMessage);
        }

        for (Option opt : Options) {
            if (opt.Value == null || opt.Value == "") {
                if (opt.Mandatory) {
                    throw new RuntimeException("Missing argument: " + opt.Name + "; " + opt.HelpMessage);
                } else {
                    opt.Value = opt.DefaultValue;
                }
            }
        }
    }

    private static void printHelp() {
        // to be implemented
    }

    //
    /////////////////////////////////////
    /**
     * Entry point for cloud workflow engine
     */
    public static void main(String[] args) {
        try {
            Logger.getRootLogger().setLevel(Level.OFF);
            parseArgs(args);

            if (Options[OptHelp].Value != null) {
                printHelp();
                System.exit(0);
            }
            
            CloudWorkflowEngine container = new CloudWorkflowEngine(Options[OptConfigPath].Value);

            RegistryUtil.registerToRegistry("CloudWorkflowEngine", container.getExecutionEngine(), true);
            RegistryUtil.registerToRegistry("APIBroker", container.getExecutionEngine().getExecutionEngine(), true);
            RegistryUtil.registerToRegistry("ProcessNotifier", container.getExecutionEngine().getInvocationManager(), true);

            try {
                container.getExecutionEngine().getServiceLibrary().flushLibrary();
            } catch (Exception e) {
                logger.error("Error emptying library directory", e);
            }

            // Add users to the base system if user level security is enabled.  This will run each workflow as a different user
            //TODO: Check with Hugo that this is the best place for this code
            if (container.getExecutionEngine().isWorkflowSeparationEnforced()) {
                container.createUsers();
            }

            XmlDataStore jmsStore = PreferenceManager.getSystemPropertyGroup("JMS");
            String hostname = jmsStore.stringValue("JMSServer", "localhost");
            int port = jmsStore.intValue("JMSPort", 5445);
            String user = jmsStore.stringValue("JMSUser", "connexience");
            String password = jmsStore.stringValue("JMSPassword", "1234");
            String queueName = jmsStore.stringValue("JMSQueue", "Workflow");
            String topicName = jmsStore.stringValue("JMSControlMessageTopic", "WorkflowControl");
            String managerQueueName = jmsStore.stringValue("JMSManagerMessageQueue", "WorkflowManagerQueue");

            // buffer size 'null' means the default value
            Integer bufSize = jmsStore.propertyExists("JMSConsumerWindowSize") ? jmsStore.intValue("JMSConsumerWindowSize", 0) : null;
            container.startJMSAttacherThread(hostname, port, user, password, queueName, topicName, managerQueueName, bufSize);

        } catch (Exception e) {
            logger.error("Error attaching to JMS", e);
            System.exit(1);
        }
    }

    private void createUsers() {
        try {
            int concurrentWorkflowInvocations = engine.getMaxConcurrentWorkflows();
            String baseUserName = PreferenceManager.getSystemPropertyGroup("Security").stringValue("InvocationUserPrefix", "wfuser");
            int baseUID = PreferenceManager.getSystemPropertyGroup("Security").intValue("InvocationUserIDStart", 1000);
            String groupName = PreferenceManager.getSystemPropertyGroup("Security").stringValue("InvocationGroup", "wfusers");
            int groupID = PreferenceManager.getSystemPropertyGroup("Security").intValue("InvocationGroupID", 400);

            for (int i = 1; i <= concurrentWorkflowInvocations + 1; i++) //create one more than necessary
            {
                UserManager userManager = UserManagerFactory.newInstance();
                if (!userManager.userExists(baseUserName + i)) {
                    boolean success = userManager.createUser(baseUserName + i, baseUID + i, groupName, groupID);
                    logger.debug("Created user: " + baseUserName + i);
                    if (!success) {
                        logger.error("Unable to add user " + baseUserName + i);
                    }
                }
            }
        } catch (InstallerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks whether a directory exists, creates one if needed and checks
     * whether or not it is writable.
     *
     * @param path -- the path to check
     * @param errorMessagePrefix -- the prefix to customise the error message
     * with.
     * @throws IOException if the directory cannot be written to.
     */
    private void checkAndCreateDir(File path, String messagePrefix)
            throws IOException {
        if (!path.exists()) {
            if (!path.mkdirs()) {
                throw new IOException(messagePrefix + " cannot create directory '" + path + "'");
            }
        }
        if (!path.canWrite()) {
            throw new IOException(messagePrefix + " write access denied to path '" + path + "'");
        }

        logger.info(messagePrefix + " OK.");
    }

    /**
     * Shutdown thread
     */
    public class ShutdownThread extends Thread {

        /**
         * Engine being managed
         */
        private CloudWorkflowEngine engine;

        public ShutdownThread(CloudWorkflowEngine engine) {
            this.engine = engine;
        }

        @Override
        public void run() {
            try {
                logger.debug("Performing engine shutdown");
                if (engine.getExecutionEngine() != null) {
                    engine.getExecutionEngine().interactiveShutdown();
                }
            } catch (Exception e) {
                logger.error("Error shutting down workflow engine: " + e.getMessage(), e);
            }
        }
    }
}

