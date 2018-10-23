// https://searchcode.com/api/result/113114614/

/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.InitialContext;

import org.drools.compiler.kie.builder.impl.InternalKieContainer;
import org.drools.compiler.kie.builder.impl.InternalKieScanner;
import org.kie.api.KieServices;
import org.kie.api.builder.Message.Level;
import org.kie.api.builder.Results;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.KieServerEnvironment;
import org.kie.server.api.Version;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponse.ResponseType;
import org.kie.server.controller.api.KieServerController;
import org.kie.server.controller.api.model.KieServerSetup;
import org.kie.server.services.api.KieControllerNotConnectedException;
import org.kie.server.services.api.KieControllerNotDefinedException;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.controller.ControllerConnectRunnable;
import org.kie.server.services.impl.controller.DefaultRestControllerImpl;
import org.kie.server.services.impl.security.JACCIdentityProvider;
import org.kie.server.services.impl.storage.KieServerState;
import org.kie.server.services.impl.storage.KieServerStateRepository;
import org.kie.server.services.impl.storage.file.KieServerStateFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieServerImpl {

    private static final Logger             logger               = LoggerFactory.getLogger(KieServerImpl.class);

    private static final ServiceLoader<KieServerExtension> serverExtensions = ServiceLoader.load(KieServerExtension.class);

    private static final ServiceLoader<KieServerController> kieControllers = ServiceLoader.load(KieServerController.class);
    // TODO figure out how to get actual URL of the kie server
    private String kieServerLocation = System.getProperty(KieServerConstants.KIE_SERVER_LOCATION, "http://localhost:8230/kie-server/services/rest/server");

    private final KieServerRegistry context;
    private final ContainerManager containerManager;

    private final KieServerStateRepository repository;
    private volatile AtomicBoolean kieServerActive = new AtomicBoolean(false);

    public KieServerImpl() {
        this.repository = new KieServerStateFileRepository();

        this.context = new KieServerRegistryImpl();
        this.context.registerIdentityProvider(new JACCIdentityProvider());
        this.context.registerStateRepository(repository);

        this.containerManager = getContainerManager();

        KieServerState currentState = repository.load(KieServerEnvironment.getServerId());

        List<KieServerExtension> extensions = sortKnownExtensions();

        for (KieServerExtension extension : extensions) {
            if (!extension.isActive()) {
                continue;
            }
            try {
                extension.init(this, this.context);

                this.context.registerServerExtension(extension);

                logger.info("{} has been successfully registered as server extension", extension);
            } catch (Exception e) {
                logger.error("Error when initializing server extension of type {}", extension, e);
            }
        }
        kieServerActive.set(true);
        boolean readyToRun = false;
        KieServerController kieController = getController();
        // try to load container information from available controllers if any...
        KieServerInfo kieServerInfo = getInfoInternal();
        Set<KieContainerResource> containers = null;
        KieServerSetup kieServerSetup = null;
        try {
            kieServerSetup = kieController.connect(kieServerInfo);

            containers = kieServerSetup.getContainers();
            readyToRun = true;
        } catch (KieControllerNotDefinedException e) {
            // if no controllers use local storage
            containers = currentState.getContainers();
            kieServerSetup = new KieServerSetup();
            readyToRun = true;
        } catch (KieControllerNotConnectedException e) {
            // if controllers are defined but cannot be reached schedule connection and disable until it gets connection to one of them
            readyToRun = false;
            logger.warn("Unable to connect to any controllers, delaying container installation until connection can be established");
            Thread connectToControllerThread = new Thread(new ControllerConnectRunnable(kieServerActive,
                                                                                        kieController,
                                                                                        kieServerInfo,
                                                                                        currentState,
                                                                                        containerManager,
                                                                                        this), "KieServer-ControllerConnect");
            connectToControllerThread.start();

        }

        if (readyToRun) {
            containerManager.installContainers(this, containers, currentState, kieServerSetup);
        }
    }



    public KieServerRegistry getServerRegistry() { 
        return context;
    }


    public void destroy() {
        kieServerActive.set(false);
        // disconnect from controller
        KieServerController kieController = getController();
        kieController.disconnect(getInfoInternal());

        for (KieServerExtension extension : context.getServerExtensions()) {

            try {
                extension.destroy(this, this.context);

                this.context.unregisterServerExtension(extension);

                logger.info("{} has been successfully unregistered as server extension", extension);
            } catch (Exception e) {
                logger.error("Error when destroying server extension of type {}", extension, e);
            }
        }

    }


    public List<KieServerExtension> getServerExtensions() {
        return this.context.getServerExtensions();
    }

    protected KieServerInfo getInfoInternal() {
        Version version = KieServerEnvironment.getVersion();
        String serverId = KieServerEnvironment.getServerId();
        String serverName = KieServerEnvironment.getServerName();
        String versionStr = version != null ? version.toString() : "Unknown-Version";

        List<String> capabilities = new ArrayList<String>();
        for (KieServerExtension extension : context.getServerExtensions()) {
            capabilities.add(extension.getImplementedCapability());
        }

        return new KieServerInfo(serverId, serverName, versionStr, capabilities, kieServerLocation);

    }

    public ServiceResponse<KieServerInfo> getInfo() {
        try {
            KieServerInfo kieServerInfo = getInfoInternal();

            return new ServiceResponse<KieServerInfo>(ServiceResponse.ResponseType.SUCCESS, "Kie Server info", kieServerInfo);
        } catch (Exception e) {
            logger.error("Error retrieving server info:", e);
            return new ServiceResponse<KieServerInfo>(ServiceResponse.ResponseType.FAILURE, "Error retrieving kie server info: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieContainerResource> createContainer(String containerId, KieContainerResource container) {
        if (container == null || container.getReleaseId() == null) {
            logger.error("Error creating container. Release Id is null: " + container);
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Failed to create container " + containerId + ". Release Id is null: " + container + ".");
        }
        container.setContainerId(containerId);
        ReleaseId releaseId = container.getReleaseId();
        try {
            KieContainerInstanceImpl ci = new KieContainerInstanceImpl(containerId, KieContainerStatus.CREATING);
            KieContainerInstanceImpl previous = null;
            // have to synchronize on the ci or a concurrent call to dispose may create inconsistencies
            synchronized (ci) {
                previous = context.registerContainer(containerId, ci);
                if (previous == null) {
                    try {
                        KieServices ks = KieServices.Factory.get();
                        InternalKieContainer kieContainer = (InternalKieContainer) ks.newKieContainer(releaseId);
                        if (kieContainer != null) {
                            ci.setKieContainer(kieContainer);
                            logger.debug("Container {} (for release id {}) general initialization: DONE", containerId, releaseId);
                            // process server extensions
                            List<KieServerExtension> extensions = context.getServerExtensions();
                            for (KieServerExtension extension : extensions) {
                                extension.createContainer(containerId, ci, new HashMap<String, Object>());
                                logger.debug("Container {} (for release id {}) {} initialization: DONE", containerId, releaseId, extension);
                            }

                            ci.getResource().setStatus(KieContainerStatus.STARTED);
                            logger.info("Container {} (for release id {}) successfully started", containerId, releaseId);


                            // store the current state of the server
                            KieServerState currentState = repository.load(KieServerEnvironment.getServerId());
                            container.setStatus(KieContainerStatus.STARTED);
                            currentState.getContainers().add(container);

                            repository.store(KieServerEnvironment.getServerId(), currentState);

                            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " successfully deployed with module " + releaseId + ".", ci.getResource());
                        } else {
                            ci.getResource().setStatus(KieContainerStatus.FAILED);
                            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Failed to create container " + containerId + " with module " + releaseId + ".");
                        }
                    } catch (Exception e) {
                        logger.error("Error creating container '" + containerId + "' for module '" + releaseId + "'", e);
                        ci.getResource().setStatus(KieContainerStatus.FAILED);
                        return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Failed to create container " + containerId + " with module " + releaseId + ": " + e.getClass().getName() + ": " + e.getMessage());
                    }
                } else {
                    return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Container " + containerId + " already exists.", previous.getResource());
                }
            }
        } catch (Exception e) {
            logger.error("Error creating container '" + containerId + "' for module '" + releaseId + "'", e);
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Error creating container " + containerId +
                    " with module " + releaseId + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieContainerResourceList> listContainers() {
        try {
            List<KieContainerResource> containers = new ArrayList<KieContainerResource>();
            for (KieContainerInstanceImpl instance : context.getContainers()) {
                containers.add(instance.getResource());
            }
            KieContainerResourceList cil = new KieContainerResourceList(containers);
            return new ServiceResponse<KieContainerResourceList>(ServiceResponse.ResponseType.SUCCESS, "List of created containers", cil);
        } catch (Exception e) {
            logger.error("Error retrieving list of containers", e);
            return new ServiceResponse<KieContainerResourceList>(ServiceResponse.ResponseType.FAILURE, "Error listing containers: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieContainerResource> getContainerInfo(String id) {
        try {
            KieContainerInstanceImpl ci = context.getContainer(id);
            if (ci != null) {
                if( ci.getResource().getScanner() == null ) {
                    ci.getResource().setScanner( getScannerResource( ci ) );
                }
                return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.SUCCESS, "Info for container " + id, ci.getResource());
            }
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Container " + id + " is not instantiated.");
        } catch (Exception e) {
            logger.error("Error retrieving info for container '" + id + "'", e);
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Error retrieving container info: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<Void> disposeContainer(String containerId) {
        try {
            KieContainerInstanceImpl kci = (KieContainerInstanceImpl) context.unregisterContainer(containerId);
            if (kci != null) {
                synchronized (kci) {
                    kci.setStatus(KieContainerStatus.DISPOSING); // just in case
                    if (kci.getKieContainer() != null) {
                        List<KieServerExtension> disposedExtensions = new ArrayList<KieServerExtension>();
                        try {
                            // first attempt to dispose container on all extensions
                            logger.debug("Container {} (for release id {}) shutdown: In Progress", containerId, kci.getResource().getReleaseId());
                            // process server extensions
                            List<KieServerExtension> extensions = context.getServerExtensions();
                            for (KieServerExtension extension : extensions) {
                                extension.disposeContainer(containerId, kci, new HashMap<String, Object>());
                                logger.debug("Container {} (for release id {}) {} shutdown: DONE", containerId, kci.getResource().getReleaseId(), extension);
                                disposedExtensions.add(extension);
                            }

                        } catch (Exception e) {
                            logger.warn("Dispose of container {} failed, putting it back to started state by recreating container on {}", containerId, disposedExtensions);
                            // since the dispose fail rollback must take place to put it back to running state
                            for (KieServerExtension extension : disposedExtensions) {
                                extension.createContainer(containerId, kci, new HashMap<String, Object>());
                                logger.debug("Container {} (for release id {}) {} restart: DONE", containerId, kci.getResource().getReleaseId(), extension);
                            }

                            kci.setStatus(KieContainerStatus.STARTED);
                            context.registerContainer(containerId, kci);
                            logger.info("Container {} (for release id {}) STARTED after failed dispose", containerId, kci.getResource().getReleaseId());
                            return new ServiceResponse<Void>(ResponseType.FAILURE, "Container " + containerId +
                                    " failed to dispose, exception was raised: " + e.getClass().getName() + ": " + e.getMessage());
                        }
                        InternalKieContainer kieContainer = kci.getKieContainer();
                        kci.setKieContainer(null); // helps reduce concurrent access issues
                        // this may fail, but we already removed the container from the registry
                        kieContainer.dispose();
                        logger.info("Container {} (for release id {}) successfully stopped", containerId, kci.getResource().getReleaseId());

                        // store the current state of the server
                        KieServerState currentState = repository.load(KieServerEnvironment.getServerId());

                        List<KieContainerResource> containers = new ArrayList<KieContainerResource>();
                        for (KieContainerResource containerResource : currentState.getContainers()) {
                            if ( !containerId.equals(containerResource.getContainerId()) ) {
                                containers.add(containerResource);
                            }
                        }
                        currentState.setContainers(new HashSet<KieContainerResource>(containers));

                        repository.store(KieServerEnvironment.getServerId(), currentState);

                        return new ServiceResponse<Void>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " successfully disposed.");
                    } else {
                        return new ServiceResponse<Void>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " was not instantiated.");
                    }
                }
            } else {
                return new ServiceResponse<Void>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " was not instantiated.");
            }
        } catch (Exception e) {
            logger.error("Error disposing Container '" + containerId + "'", e);
            return new ServiceResponse<Void>(ServiceResponse.ResponseType.FAILURE, "Error disposing container " + containerId + ": " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieScannerResource> getScannerInfo(String id) {
        try {
            KieContainerInstanceImpl kci = context.getContainer(id);
            if (kci != null && kci.getKieContainer() != null) {
                KieScannerResource info = getScannerResource( kci );
                kci.getResource().setScanner( info );
                return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS, "Scanner info successfully retrieved", info);
            } else {
                return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                        "Unknown container " + id + ".");
            }
        } catch (Exception e) {
            logger.error("Error retrieving scanner info for container '" + id + "'.", e);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE, "Error retrieving scanner info for container '" + id + "': " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private KieScannerResource getScannerResource(KieContainerInstanceImpl kci) {
        InternalKieScanner scanner = kci.getScanner();
        KieScannerResource info = null;
        if (scanner != null) {
            info = new KieScannerResource(mapStatus(scanner.getStatus()), scanner.getPollingInterval());
        } else {
            info = new KieScannerResource( KieScannerStatus.DISPOSED);
        }
        return info;
    }

    public ServiceResponse<KieScannerResource> updateScanner(String id, KieScannerResource resource) {
        if (resource == null || resource.getStatus() == null) {
            logger.error("Error updating scanner for container " + id + ". Status is null: " + resource);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE, "Error updating scanner for container " + id + ". Status is null: " + resource);
        }
        KieScannerStatus status = resource.getStatus();
        try {
            KieContainerInstanceImpl kci = context.getContainer(id);
            if (kci != null && kci.getKieContainer() != null) {
                ServiceResponse<KieScannerResource> result = null;
                switch (status) {
                    case CREATED:
                        // create the scanner
                        result = createScanner(id, kci);
                        break;
                    case STARTED:
                        // start the scanner
                        result = startScanner(id, resource, kci);
                        break;
                    case STOPPED:
                        // stop the scanner
                        result = stopScanner(id, resource, kci);
                        break;
                    case SCANNING:
                        // scan now
                        result = scanNow(id, resource, kci);
                        break;
                    case DISPOSED:
                        // dispose
                        result = disposeScanner(id, resource, kci);
                        break;
                    default:
                        // error
                        result = new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                                "Unknown status '" + status + "' for scanner on container " + id + ".");
                        break;
                }
                kci.getResource().setScanner( result.getResult() ); // might be null, but that is ok
                return result;
            } else {
                return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                        "Unknown container " + id + ".");
            }
        } catch (Exception e) {
            logger.error("Error updating scanner for container '" + id + "': " + resource, e);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE, "Error updating scanner for container '" + id +
                    "': " + resource + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private ServiceResponse<KieScannerResource> startScanner(String id, KieScannerResource resource, KieContainerInstanceImpl kci) {
        if (kci.getScanner() == null) {
            ServiceResponse<KieScannerResource> response = createScanner(id, kci);
            if (ResponseType.FAILURE.equals(response.getType())) {
                return response;
            }
        }
        if (KieScannerStatus.STOPPED.equals(mapStatus(kci.getScanner().getStatus())) &&
                resource.getPollInterval() != null) {
            kci.getScanner().start(resource.getPollInterval());
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Kie scanner successfully created.",
                    getScannerResource(kci));
        } else if (!KieScannerStatus.STOPPED.equals(mapStatus(kci.getScanner().getStatus()))) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid kie scanner status: " + mapStatus(kci.getScanner().getStatus()),
                    getScannerResource(kci));
        } else if (resource.getPollInterval() == null) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid polling interval: " + resource.getPollInterval(),
                    getScannerResource(kci));
        }
        return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                "Unknown error starting scanner. Scanner was not started." + resource,
                getScannerResource(kci));
    }

    private ServiceResponse<KieScannerResource> stopScanner(String id, KieScannerResource resource, KieContainerInstanceImpl kci) {
        if (kci.getScanner() == null) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid call. Scanner is not instantiated. ",
                    getScannerResource(kci));
        }
        if (KieScannerStatus.STARTED.equals(mapStatus(kci.getScanner().getStatus())) ||
                KieScannerStatus.SCANNING.equals(mapStatus(kci.getScanner().getStatus()))) {
            kci.getScanner().stop();
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Kie scanner successfully stopped.",
                    getScannerResource(kci));
        } else {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid kie scanner status: " + mapStatus(kci.getScanner().getStatus()),
                    getScannerResource(kci));
        }
    }

    private ServiceResponse<KieScannerResource> scanNow(String id, KieScannerResource resource, KieContainerInstanceImpl kci) {
        if (kci.getScanner() == null) {
            createScanner( id, kci );
        }
        KieScannerStatus kss = mapStatus( kci.getScanner().getStatus() );
        if (KieScannerStatus.STOPPED.equals( kss ) || KieScannerStatus.CREATED.equals( kss ) || KieScannerStatus.STARTED.equals( kss )) {
            kci.getScanner().scanNow();
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Scan successfully executed.",
                    getScannerResource(kci));
        } else {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid kie scanner status: " + kss,
                    getScannerResource(kci));
        }
    }

    private ServiceResponse<KieScannerResource> disposeScanner(String id, KieScannerResource resource, KieContainerInstanceImpl kci) {
        if (kci.getScanner() == null) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Invalid call. Scanner already disposed. ",
                    getScannerResource(kci));
        }
        if (KieScannerStatus.STARTED.equals(mapStatus(kci.getScanner().getStatus())) ||
                KieScannerStatus.SCANNING.equals(mapStatus(kci.getScanner().getStatus()))) {
            ServiceResponse<KieScannerResource> response = stopScanner(id, resource, kci);
            if (ResponseType.FAILURE.equals(response.getType())) {
                return response;
            }
        }
        kci.getScanner().shutdown();
        kci.setScanner(null);
        return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                "Kie scanner successfully shutdown.",
                getScannerResource(kci));
    }

    private ServiceResponse<KieScannerResource> createScanner(String id, KieContainerInstanceImpl kci) {
        if (kci.getScanner() == null) {
            InternalKieScanner scanner = (InternalKieScanner) KieServices.Factory.get().newKieScanner(kci.getKieContainer());
            kci.setScanner(scanner);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Kie scanner successfully created.",
                    getScannerResource(kci));
        } else {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Error creating the scanner for container " + id + ". Scanner already exists.");

        }
    }

    public ServiceResponse<ReleaseId> getContainerReleaseId(String id) {
        try {
            KieContainerInstanceImpl ci = context.getContainer(id);
            if (ci != null) {
                return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.SUCCESS, "ReleaseId for container " + id, ci.getResource().getReleaseId());
            }
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Container " + id + " is not instantiated.");
        } catch (Exception e) {
            logger.error("Error retrieving releaseId for container '" + id + "'", e);
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error retrieving container releaseId: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<ReleaseId> updateContainerReleaseId(String id, ReleaseId releaseId) {
        if( releaseId == null ) {
            logger.error("Error updating releaseId for container '" + id + "'. ReleaseId is null.");
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error updating releaseId for container " + id + ". ReleaseId is null. ");
        }
        try {
            KieContainerInstanceImpl kci = context.getContainer(id);
            // the following code is subject to a concurrent call to dispose(), but the cost of synchronizing it
            // would likely not be worth it. At this point a decision was made to fail the execution if a concurrent 
            // call do dispose() is executed.
            if (kci != null && kci.getKieContainer() != null) {
                Results results = kci.getKieContainer().updateToVersion(releaseId);
                if (results.hasMessages(Level.ERROR)) {
                    logger.error("Error updating releaseId for container " + id + " to version " + releaseId + "\nMessages: " + results.getMessages());
                    return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error updating release id on container " + id + " to " + releaseId, kci.getResource().getReleaseId());
                } else {
                    return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.SUCCESS, "Release id successfully updated.", kci.getResource().getReleaseId());
                }
            } else {
                return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Container " + id + " is not instantiated.");
            }
        } catch (Exception e) {
            logger.error("Error updating releaseId for container '" + id + "'", e);
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error updating releaseId for container " + id + ": " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private KieScannerStatus mapStatus(InternalKieScanner.Status status) {
        switch (status) {
            case STARTING:
                return KieScannerStatus.CREATED;
            case RUNNING:
                return KieScannerStatus.STARTED;
            case SCANNING:
            case UPDATING:
                return KieScannerStatus.SCANNING;
            case STOPPED:
                return KieScannerStatus.STOPPED;
            case SHUTDOWN:
                return KieScannerStatus.DISPOSED;
            default:
                return KieScannerStatus.UNKNOWN;
        }
    }

    protected KieServerController getController() {
        KieServerController controller = new DefaultRestControllerImpl(context);
        Iterator<KieServerController> it = kieControllers.iterator();
        if (it != null && it.hasNext()) {
            controller = it.next();
        }

        return controller;
    }

    protected ContainerManager getContainerManager() {
        try {
            return InitialContext.doLookup("java:module/ContainerManagerEJB");
        } catch (Exception e) {
            logger.debug("Unable to find JEE version of ContainerManager suing default one");
            return new ContainerManager();
        }
    }

    protected List<KieServerExtension> sortKnownExtensions() {
        List<KieServerExtension> extensions = new ArrayList<KieServerExtension>();

        for (KieServerExtension extension : serverExtensions) {
            extensions.add(extension);
        }

        Collections.sort(extensions, new Comparator<KieServerExtension>() {
            @Override
            public int compare(KieServerExtension e1, KieServerExtension e2) {
                return e1.getStartOrder().compareTo(e2.getStartOrder());
            }
        });

        return extensions;
    }

    @Override
    public String toString() {
        return "KieServer{" +
                "id='" + KieServerEnvironment.getServerId() + '\'' +
                "name='" + KieServerEnvironment.getServerName() + '\'' +
                "version='" + KieServerEnvironment.getVersion() + '\'' +
                "location='" + kieServerLocation + '\'' +
                '}';
    }
}

