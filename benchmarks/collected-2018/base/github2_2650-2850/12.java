// https://searchcode.com/api/result/70837233/

package net.i2p.router.transport.udp;

import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.i2p.data.DatabaseEntry;
import net.i2p.data.DataHelper;
import net.i2p.data.Hash;
import net.i2p.data.RouterAddress;
import net.i2p.data.RouterIdentity;
import net.i2p.data.RouterInfo;
import net.i2p.data.SessionKey;
import net.i2p.data.i2np.DatabaseStoreMessage;
import net.i2p.data.i2np.I2NPMessage;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.OutNetMessage;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.networkdb.kademlia.FloodfillNetworkDatabaseFacade;
import net.i2p.router.transport.Transport;
import net.i2p.router.transport.TransportBid;
import net.i2p.router.transport.TransportImpl;
import net.i2p.router.transport.crypto.DHSessionKeyBuilder;
import net.i2p.router.util.RandomIterator;
import net.i2p.util.Addresses;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;
import net.i2p.util.SimpleScheduler;
import net.i2p.util.SimpleTimer;
import net.i2p.util.SimpleTimer2;
import net.i2p.util.Translate;

/**
 *  The SSU transport
 */
public class UDPTransport extends TransportImpl implements TimedWeightedPriorityMessageQueue.FailedListener {
    private final Log _log;
    private UDPEndpoint _endpoint;
    private final Object _addDropLock = new Object();
    /** Peer (Hash) to PeerState */
    private final Map<Hash, PeerState> _peersByIdent;
    /** RemoteHostId to PeerState */
    private final Map<RemoteHostId, PeerState> _peersByRemoteHost;
    private PacketHandler _handler;
    private EstablishmentManager _establisher;
    private final MessageQueue _outboundMessages;
    private final OutboundMessageFragments _fragments;
    private final OutboundMessageFragments.ActiveThrottle _activeThrottle;
    private OutboundRefiller _refiller;
    private PacketPusher _pusher;
    private final InboundMessageFragments _inboundFragments;
    //private UDPFlooder _flooder;
    private PeerTestManager _testManager;
    private final IntroductionManager _introManager;
    private final ExpirePeerEvent _expireEvent;
    private final PeerTestEvent _testEvent;
    private final PacketBuilder _destroyBuilder;
    private short _reachabilityStatus;
    private long _reachabilityStatusLastUpdated;
    private long _introducersSelectedOn;
    private long _lastInboundReceivedOn;
    private final DHSessionKeyBuilder.Factory _dhFactory;
    private int _mtu;
    
    /** do we need to rebuild our external router address asap? */
    private boolean _needsRebuild;
    
    /** summary info to distribute */
    private RouterAddress _externalAddress;
    /** port number on which we can be reached, or -1 for error, or 0 for unset */
    private int _externalListenPort;
    /** IP address of externally reachable host, or null */
    private InetAddress _externalListenHost;
    /** introduction key */
    private SessionKey _introKey;
    
    /**
     *  List of RemoteHostId for peers whose packets we want to drop outright
     *  This is only for old network IDs (pre-0.6.1.10), so it isn't really used now.
     */
    private final Set<RemoteHostId> _dropList;
    
    private int _expireTimeout;

    /** last report from a peer of our IP */
    private Hash _lastFrom;
    private byte[] _lastOurIP;
    private int _lastOurPort;

    private static final int DROPLIST_PERIOD = 10*60*1000;
    public static final String STYLE = "SSU";
    public static final String PROP_INTERNAL_PORT = "i2np.udp.internalPort";

    /** now unused, we pick a random port
     *  @deprecated unused
     */
    public static final int DEFAULT_INTERNAL_PORT = 8887;

    /**
     *  To prevent trouble. 1024 as of 0.9.4.
     *
     *  @since 0.9.3
     */
    static final int MIN_PEER_PORT = 1024;

    /** Limits on port told to us by others,
     *  We should have an exception if it matches the existing low port.
     */
    private static final int MIN_EXTERNAL_PORT = 1024;
    private static final int MAX_EXTERNAL_PORT = 65535;

    /** define this to explicitly set an external IP address */
    public static final String PROP_EXTERNAL_HOST = "i2np.udp.host";
    /** define this to explicitly set an external port */
    public static final String PROP_EXTERNAL_PORT = "i2np.udp.port";
    /** 
     * If i2np.udp.preferred is set to "always", the UDP bids will always be under 
     * the bid from the TCP transport - even if a TCP connection already 
     * exists.  If it is set to "true",
     * it will prefer UDP unless no UDP session exists and a TCP connection 
     * already exists.
     * If it is set to "false" (the default),
     * it will prefer TCP unless no TCP session exists and a UDP connection 
     * already exists.
     */
    public static final String PROP_PREFER_UDP = "i2np.udp.preferred";
    private static final String DEFAULT_PREFER_UDP = "false";
    
    /** Override whether we will change our advertised port no matter what our peers tell us
     *  See getIsPortFixed() for default behaviour.
     */
    private static final String PROP_FIXED_PORT = "i2np.udp.fixedPort";

    /** allowed sources of address updates */
    public static final String PROP_SOURCES = "i2np.udp.addressSources";
    public static final String DEFAULT_SOURCES = "local,upnp,ssu";
    /** remember IP changes */
    public static final String PROP_IP= "i2np.lastIP";
    public static final String PROP_IP_CHANGE = "i2np.lastIPChange";
    public static final String PROP_LAPTOP_MODE = "i2np.laptopMode";

    /** do we require introducers, regardless of our status? */
    public static final String PROP_FORCE_INTRODUCERS = "i2np.udp.forceIntroducers";
    /** do we allow direct SSU connections, sans introducers?  */
    public static final String PROP_ALLOW_DIRECT = "i2np.udp.allowDirect";
    /** this is rarely if ever used, default is to bind to wildcard address */
    public static final String PROP_BIND_INTERFACE = "i2np.udp.bindInterface";
    /** override the "large" (max) MTU, default is PeerState.LARGE_MTU */
    private static final String PROP_DEFAULT_MTU = "i2np.udp.mtu";
        
    /** how many relays offered to us will we use at a time? */
    public static final int PUBLIC_RELAY_COUNT = 3;
    
    private static final boolean USE_PRIORITY = false;

    /** configure the priority queue with the given split points */
    private static final int PRIORITY_LIMITS[] = new int[] { 100, 200, 300, 400, 500, 1000 };
    /** configure the priority queue with the given weighting per priority group */
    private static final int PRIORITY_WEIGHT[] = new int[] { 1, 1, 1, 1, 1, 2 };

    /** should we flood all UDP peers with the configured rate? This is for testing only! */
    //private static final boolean SHOULD_FLOOD_PEERS = false;
    
    private static final int MAX_CONSECUTIVE_FAILED = 5;
    
    public static final int DEFAULT_COST = 5;
    private static final int TEST_FREQUENCY = 13*60*1000;
    static final long[] RATES = { 10*60*1000 };
    
    private static final int[] BID_VALUES = { 15, 20, 50, 65, 80, 95, 100, 115, TransportBid.TRANSIENT_FAIL };
    private static final int FAST_PREFERRED_BID = 0;
    private static final int SLOW_PREFERRED_BID = 1;
    private static final int FAST_BID = 2;
    private static final int SLOW_BID = 3;
    private static final int SLOWEST_BID = 4;
    private static final int SLOWEST_COST_BID = 5;
    private static final int NEAR_CAPACITY_BID = 6;
    private static final int NEAR_CAPACITY_COST_BID = 7;
    private static final int TRANSIENT_FAIL_BID = 8;
    private final TransportBid[] _cachedBid;

    // Opera doesn't have the char, TODO check UA
    //private static final String THINSP = "&thinsp;/&thinsp;";
    private static final String THINSP = " / ";

    public UDPTransport(RouterContext ctx, DHSessionKeyBuilder.Factory dh) {
        super(ctx);
        _dhFactory = dh;
        _log = ctx.logManager().getLog(UDPTransport.class);
        _peersByIdent = new ConcurrentHashMap(128);
        _peersByRemoteHost = new ConcurrentHashMap(128);
        _dropList = new ConcurrentHashSet(2);
        
        // See comments in DummyThrottle.java
        if (USE_PRIORITY) {
            TimedWeightedPriorityMessageQueue mq = new TimedWeightedPriorityMessageQueue(ctx, PRIORITY_LIMITS, PRIORITY_WEIGHT, this);
            _outboundMessages = mq;
            _activeThrottle = mq;
        } else {
            DummyThrottle mq = new DummyThrottle();
            _outboundMessages = null;
            _activeThrottle = mq;
        }

        _cachedBid = new SharedBid[BID_VALUES.length];
        for (int i = 0; i < BID_VALUES.length; i++) {
            _cachedBid[i] = new SharedBid(BID_VALUES[i]);
        }

        _destroyBuilder = new PacketBuilder(_context, this);
        _fragments = new OutboundMessageFragments(_context, this, _activeThrottle);
        _inboundFragments = new InboundMessageFragments(_context, _fragments, this);
        //if (SHOULD_FLOOD_PEERS)
        //    _flooder = new UDPFlooder(_context, this);
        _expireTimeout = EXPIRE_TIMEOUT;
        _expireEvent = new ExpirePeerEvent();
        _testEvent = new PeerTestEvent();
        _reachabilityStatus = CommSystemFacade.STATUS_UNKNOWN;
        _introManager = new IntroductionManager(_context, this);
        _introducersSelectedOn = -1;
        _lastInboundReceivedOn = -1;
        _mtu = PeerState.LARGE_MTU;
        _needsRebuild = true;
        
        _context.statManager().createRateStat("udp.alreadyConnected", "What is the lifetime of a reestablished session", "udp", RATES);
        _context.statManager().createRateStat("udp.droppedPeer", "How long ago did we receive from a dropped peer (duration == session lifetime", "udp", RATES);
        _context.statManager().createRateStat("udp.droppedPeerInactive", "How long ago did we receive from a dropped peer (duration == session lifetime)", "udp", RATES);
        _context.statManager().createRateStat("udp.statusOK", "How many times the peer test returned OK", "udp", RATES);
        _context.statManager().createRateStat("udp.statusDifferent", "How many times the peer test returned different IP/ports", "udp", RATES);
        _context.statManager().createRateStat("udp.statusReject", "How many times the peer test returned reject unsolicited", "udp", RATES);
        _context.statManager().createRateStat("udp.statusUnknown", "How many times the peer test returned an unknown result", "udp", RATES);
        _context.statManager().createRateStat("udp.addressTestInsteadOfUpdate", "How many times we fire off a peer test of ourselves instead of adjusting our own reachable address?", "udp", RATES);
        _context.statManager().createRateStat("udp.addressUpdated", "How many times we adjust our own reachable IP address", "udp", RATES);
        _context.statManager().createRateStat("udp.proactiveReestablish", "How long a session was idle for when we proactively reestablished it", "udp", RATES);
        _context.statManager().createRateStat("udp.dropPeerDroplist", "How many peers currently have their packets dropped outright when a new peer is added to the list?", "udp", RATES);
        _context.statManager().createRateStat("udp.dropPeerConsecutiveFailures", "How many consecutive failed sends to a peer did we attempt before giving up and reestablishing a new session (lifetime is inactivity perood)", "udp", RATES);
        // following are for PacketBuider
        _context.statManager().createRateStat("udp.packetAuthTime", "How long it takes to encrypt and MAC a packet for sending", "udp", RATES);
        _context.statManager().createRateStat("udp.packetAuthTimeSlow", "How long it takes to encrypt and MAC a packet for sending (when its slow)", "udp", RATES);

        _context.simpleScheduler().addPeriodicEvent(new PingIntroducers(), MIN_EXPIRE_TIMEOUT * 3 / 4);
    }
    
    public synchronized void startup() {
        _fragments.shutdown();
        if (_pusher != null)
            _pusher.shutdown();
        if (_handler != null) 
            _handler.shutdown();
        if (_endpoint != null)
            _endpoint.shutdown();
        if (_establisher != null)
            _establisher.shutdown();
        if (_refiller != null)
            _refiller.shutdown();
        _inboundFragments.shutdown();
        //if (_flooder != null)
        //    _flooder.shutdown();
        _introManager.reset();
        UDPPacket.clearCache();
        
        _introKey = new SessionKey(new byte[SessionKey.KEYSIZE_BYTES]);
        System.arraycopy(_context.routerHash().getData(), 0, _introKey.getData(), 0, SessionKey.KEYSIZE_BYTES);
        
        rebuildExternalAddress();

        // bind host
        String bindTo = _context.getProperty(PROP_BIND_INTERFACE);

        if (bindTo == null) {
            // If we are configured with a fixed IP address,
            // AND it's one of our local interfaces,
            // bind only to that.
            String fixedHost = _context.getProperty(PROP_EXTERNAL_HOST);
            if (fixedHost != null && fixedHost.length() > 0) {
                try {
                    String testAddr = InetAddress.getByName(fixedHost).getHostAddress();
                    if (Addresses.getAddresses().contains(testAddr))
                        bindTo = testAddr;
                } catch (UnknownHostException uhe) {}
            }
        }

        InetAddress bindToAddr = null;
        if (bindTo != null) {
            try {
                bindToAddr = InetAddress.getByName(bindTo);
            } catch (UnknownHostException uhe) {
                _log.log(Log.CRIT, "Invalid SSU bind interface specified [" + bindTo + "]", uhe);
                setReachabilityStatus(CommSystemFacade.STATUS_HOSED);
                return;
            }
        }
        
        // Requested bind port
        // This may be -1 or may not be honored if busy,
        // we will check below after starting up the endpoint.
        int port;
        int oldIPort = _context.getProperty(PROP_INTERNAL_PORT, -1);
        int oldEPort = _context.getProperty(PROP_EXTERNAL_PORT, -1);
        if (_externalListenPort <= 0) {
            // no explicit external port, so lets try an internal one
            if (oldIPort > 0)
                port = oldIPort;
            else
                port = oldEPort;
        } else {
            port = _externalListenPort;
        }
        if (bindToAddr != null && _log.shouldLog(Log.WARN))
            _log.warn("Binding only to " + bindToAddr);
        if (_log.shouldLog(Log.INFO))
            _log.info("Binding to the port: " + port);
        if (_endpoint == null) {
            _endpoint = new UDPEndpoint(_context, this, port, bindToAddr);
        } else {
            // todo, set bind address too
            _endpoint.setListenPort(port);
        }
        setMTU(bindToAddr);
        
        if (_establisher == null)
            _establisher = new EstablishmentManager(_context, this);
        
        if (_testManager == null)
            _testManager = new PeerTestManager(_context, this);
        
        if (_handler == null)
            _handler = new PacketHandler(_context, this, _endpoint, _establisher, _inboundFragments, _testManager, _introManager);
        
        // See comments in DummyThrottle.java
        if (USE_PRIORITY && _refiller == null)
            _refiller = new OutboundRefiller(_context, _fragments, _outboundMessages);
        
        //if (SHOULD_FLOOD_PEERS && _flooder == null)
        //    _flooder = new UDPFlooder(_context, this);
        
        // Startup the endpoint with the requested port, check the actual port, and
        // take action if it failed or was different than requested or it needs to be saved
        _endpoint.startup();
        int newPort = _endpoint.getListenPort();
        _externalListenPort = newPort;
        if (newPort <= 0) {
            _log.log(Log.CRIT, "Unable to open UDP port");
            setReachabilityStatus(CommSystemFacade.STATUS_HOSED);
            return;
        }
        if (newPort != port || newPort != oldIPort || newPort != oldEPort) {
            // attempt to use it as our external port - this will be overridden by
            // externalAddressReceived(...)
            Map<String, String> changes = new HashMap();
            changes.put(PROP_INTERNAL_PORT, Integer.toString(newPort));
            changes.put(PROP_EXTERNAL_PORT, Integer.toString(newPort));
            _context.router().saveConfig(changes, null);
        }

        _establisher.startup();
        _handler.startup();
        _fragments.startup();
        _inboundFragments.startup();
        _pusher = new PacketPusher(_context, _fragments, _endpoint.getSender());
        _pusher.startup();
        if (USE_PRIORITY)
            _refiller.startup();
        //if (SHOULD_FLOOD_PEERS)
        //    _flooder.startup();
        _expireEvent.setIsAlive(true);
        _testEvent.setIsAlive(true); // this queues it for 3-6 minutes in the future...
        _testEvent.reschedule(10*1000); // lets requeue it for Real Soon
    }
    
    public synchronized void shutdown() {
        destroyAll();
        if (_endpoint != null)
            _endpoint.shutdown();
        //if (_flooder != null)
        //    _flooder.shutdown();
        if (_refiller != null)
            _refiller.shutdown();
        if (_handler != null)
            _handler.shutdown();
        if (_pusher != null)
            _pusher.shutdown();
        _fragments.shutdown();
        if (_establisher != null)
            _establisher.shutdown();
        _inboundFragments.shutdown();
        _expireEvent.setIsAlive(false);
        _testEvent.setIsAlive(false);
        _peersByRemoteHost.clear();
        _peersByIdent.clear();
        _dropList.clear();
        _introManager.reset();
        UDPPacket.clearCache();
    }
    
    /**
     * Introduction key that people should use to contact us
     *
     */
    SessionKey getIntroKey() { return _introKey; }

    /** @deprecated unused */
    public int getLocalPort() {
        return _endpoint != null ? _endpoint.getListenPort() : -1;
    }

    public InetAddress getLocalAddress() { return _externalListenHost; }
    public int getExternalPort() { return _externalListenPort; }

    /**
     *  @return IP or null
     *  @since 0.9.2
     */
    byte[] getExternalIP() {
       InetAddress ia = _externalListenHost;
       if (ia == null)
           return null;
       return ia.getAddress();
    }

    /**
     *  _externalListenPort should always be set (by startup()) before this is called,
     *  so the returned value should be > 0
     */
    @Override
    public int getRequestedPort() {
        if (_externalListenPort > 0)
            return _externalListenPort;
        return _context.getProperty(PROP_INTERNAL_PORT, -1);
    }

    /**
     *  Set the MTU for the socket interface at addr.
     *  @param addr null ok
     *  @since 0.9.2
     */
    private void setMTU(InetAddress addr) {
        String p = _context.getProperty(PROP_DEFAULT_MTU);
        if (p != null) {
            try {
                _mtu = MTU.rectify(Integer.parseInt(p));
                return;
            } catch (NumberFormatException nfe) {}
        }
        int mtu = MTU.getMTU(addr);
        if (mtu <= 0)
            mtu = PeerState.LARGE_MTU;
        _mtu = mtu;
    }

    /**
     * The MTU for the socket interface.
     * To be used as the "large" MTU.
     * @return limited to range PeerState.MIN_MTU to PeerState.LARGE_MTU.
     * @since 0.9.2
     */
    int getMTU() {
        return _mtu;
    }

    /**
     * If we have received an inbound connection in the last 2 minutes, don't allow 
     * our IP to change.
     */
    private static final int ALLOW_IP_CHANGE_INTERVAL = 2*60*1000;
    
    void inboundConnectionReceived() { 
        // use OS clock since its an ordering thing, not a time thing
        _lastInboundReceivedOn = System.currentTimeMillis(); 
    }
    
    /**
     * From config, UPnP, local i/f, ...
     *
     * @param source used for logging only
     * @param ip publicly routable IPv4 only
     * @param port 0 if unknown
     */
    @Override
    public void externalAddressReceived(String source, byte[] ip, int port) {
        if (_log.shouldLog(Log.WARN))
            _log.warn("Received address: " + Addresses.toString(ip, port) + " from: " + source);
        if (explicitAddressSpecified())
            return;
        String sources = _context.getProperty(PROP_SOURCES, DEFAULT_SOURCES);
        if (!sources.contains(source))
            return;
        boolean changed = changeAddress(ip, port);
        // Assume if we have an interface with a public IP that we aren't firewalled.
        // If this is wrong, the peer test will figure it out and change the status.
        if (changed && source.equals(Transport.SOURCE_INTERFACE))
            setReachabilityStatus(CommSystemFacade.STATUS_OK);
    }

    /**
     *  Callback from UPnP.
     *  If we we have an IP address and UPnP claims success, believe it.
     *  If this is wrong, the peer test will figure it out and change the status.
     *  Don't do anything if UPnP claims failure.
     */
    @Override
    public void forwardPortStatus(int port, int externalPort, boolean success, String reason) {
        if (_log.shouldLog(Log.WARN)) {
            if (success)
                _log.warn("UPnP has opened the SSU port: " + port + " via external port: " + externalPort);
            else
                _log.warn("UPnP has failed to open the SSU port: " + port + " reason: " + reason);
        }
        if (success && _externalListenHost != null)
            setReachabilityStatus(CommSystemFacade.STATUS_OK);
    }

    /**
     * Someone we tried to contact gave us what they think our IP address is.
     * Right now, we just blindly trust them, changing our IP and port on a
     * whim.  this is not good ;)
     *
     * Slight enhancement - require two different peers in a row to agree
     *
     * Todo:
     *   - Much better tracking of troublemakers
     *   - Disable if we have good local address or UPnP
     *   - This gets harder if and when we publish multiple addresses, or IPv6
     * 
     * @param from Hash of inbound destination
     * @param ourIP publicly routable IPv4 only
     * @param ourPort >= 1024
     */
    void externalAddressReceived(Hash from, byte ourIP[], int ourPort) {
        boolean isValid = isValid(ourIP) &&
                          ((ourPort >= MIN_EXTERNAL_PORT && ourPort <= MAX_EXTERNAL_PORT) ||
                           ourPort == _externalListenPort || _externalListenPort <= 0);
        boolean explicitSpecified = explicitAddressSpecified();
        boolean inboundRecent = _lastInboundReceivedOn + ALLOW_IP_CHANGE_INTERVAL > System.currentTimeMillis();
        if (_log.shouldLog(Log.INFO))
            _log.info("External address received: " + Addresses.toString(ourIP, ourPort) + " from " 
                      + from + ", isValid? " + isValid + ", explicitSpecified? " + explicitSpecified 
                      + ", receivedInboundRecent? " + inboundRecent + " status " + _reachabilityStatus);
        
        if (explicitSpecified) 
            return;
        String sources = _context.getProperty(PROP_SOURCES, DEFAULT_SOURCES);
        if (!sources.contains("ssu"))
            return;
        
        if (!isValid) {
            // ignore them 
            if (_log.shouldLog(Log.ERROR))
                _log.error("The router " + from + " told us we have an invalid IP - " 
                           + Addresses.toString(ourIP, ourPort) + ".  Lets throw tomatoes at them");
            markUnreachable(from);
            //_context.banlist().banlistRouter(from, "They said we had an invalid IP", STYLE);
            return;
        } else if (inboundRecent && _externalListenPort > 0 && _externalListenHost != null) {
            // use OS clock since its an ordering thing, not a time thing
            // Note that this fails us if we switch from one IP to a second, then back to the first,
            // as some routers still have the first IP and will successfully connect,
            // leaving us thinking the second IP is still good.
            if (_log.shouldLog(Log.INFO))
                _log.info("Ignoring IP address suggestion, since we have received an inbound con recently");
        } else if (from.equals(_lastFrom) || !eq(_lastOurIP, _lastOurPort, ourIP, ourPort)) {
            _lastFrom = from;
            _lastOurIP = ourIP;
            _lastOurPort = ourPort;
            if (_log.shouldLog(Log.INFO))
                _log.info("The router " + from + " told us we have a new IP - " 
                           + Addresses.toString(ourIP, ourPort) + ".  Wait until somebody else tells us the same thing.");
        } else {
            if (_log.shouldLog(Log.INFO))
                _log.info(from + " and " + _lastFrom + " agree we have a new IP - " 
                           + Addresses.toString(ourIP, ourPort) + ".  Changing address.");
            _lastFrom = from;
            _lastOurIP = ourIP;
            _lastOurPort = ourPort;
            changeAddress(ourIP, ourPort);
        }
        
    }
    
    /**
     * @param ourPort >= 1024 or 0 for no change
     */
    private boolean changeAddress(byte ourIP[], int ourPort) {
        // this defaults to true when we are firewalled and false otherwise.
        boolean fixedPort = getIsPortFixed();
        boolean updated = false;
        boolean fireTest = false;

        if (_log.shouldLog(Log.INFO))
            _log.info("Change address? status = " + _reachabilityStatus +
                      " diff = " + (_context.clock().now() - _reachabilityStatusLastUpdated) +
                      " old = " + _externalListenHost + ':' + _externalListenPort +
                      " new = " + Addresses.toString(ourIP, ourPort));

            synchronized (this) {
                if ( (_externalListenHost == null) ||
                     (!eq(_externalListenHost.getAddress(), _externalListenPort, ourIP, ourPort)) ) {
                    // This prevents us from changing our IP when we are not firewalled
                    //if ( (_reachabilityStatus != CommSystemFacade.STATUS_OK) ||
                    //     (_externalListenHost == null) || (_externalListenPort <= 0) ||
                    //     (_context.clock().now() - _reachabilityStatusLastUpdated > 2*TEST_FREQUENCY) ) {
                        // they told us something different and our tests are either old or failing
                        try {
                            _externalListenHost = InetAddress.getByAddress(ourIP);
                            // fixed port defaults to true so we never do this
                            if (ourPort >= MIN_EXTERNAL_PORT && ourPort <= MAX_EXTERNAL_PORT && !fixedPort)
                                _externalListenPort = ourPort;
                            if (_log.shouldLog(Log.WARN))
                                _log.warn("Trying to change our external address to " +
                                          Addresses.toString(ourIP, _externalListenPort));
                            if (_externalListenPort > 0)  {
                                rebuildExternalAddress();
                                replaceAddress(_externalAddress);
                                updated = true;
                            }
                        } catch (UnknownHostException uhe) {
                            _externalListenHost = null;
                            if (_log.shouldLog(Log.WARN))
                                _log.warn("Error trying to change our external address to " +
                                          Addresses.toString(ourIP, ourPort), uhe);
                        }
                    //} else {
                    //    // they told us something different, but our tests are recent and positive,
                    //    // so lets test again
                    //    fireTest = true;
                    //    if (_log.shouldLog(Log.WARN))
                    //        _log.warn("Different address, but we're fine.. (" + _reachabilityStatus + ")");
                    //}
                } else {
                    // matched what we expect
                    if (_log.shouldLog(Log.INFO))
                        _log.info("Same address as the current one");
                }
            }

        if (fireTest) {
            _context.statManager().addRateData("udp.addressTestInsteadOfUpdate", 1);
        } else if (updated) {
            _context.statManager().addRateData("udp.addressUpdated", 1);
            Map<String, String> changes = new HashMap();
            if (!fixedPort)
                changes.put(PROP_EXTERNAL_PORT, Integer.toString(ourPort));
            // queue a country code lookup of the new IP
            _context.commSystem().queueLookup(ourIP);
            // store these for laptop-mode (change ident on restart... or every time... when IP changes)
            String oldIP = _context.getProperty(PROP_IP);
            if (!_externalListenHost.getHostAddress().equals(oldIP)) {
                long lastChanged = 0;
                long now = _context.clock().now();
                String lcs = _context.getProperty(PROP_IP_CHANGE);
                if (lcs != null) {
                    try {
                        lastChanged = Long.parseLong(lcs);
                    } catch (NumberFormatException nfe) {}
                }

                changes.put(PROP_IP, _externalListenHost.getHostAddress());
                changes.put(PROP_IP_CHANGE, Long.toString(now));
                _context.router().saveConfig(changes, null);

                // laptop mode
                // For now, only do this at startup
                if (oldIP != null &&
                    System.getProperty("wrapper.version") != null &&
                    _context.getBooleanProperty(PROP_LAPTOP_MODE) &&
                    now - lastChanged > 10*60*1000 &&
                    _context.router().getUptime() < 10*60*1000) {
                    _log.log(Log.CRIT, "IP changed, restarting with a new identity and port");
                    // this removes the UDP port config
                    _context.router().killKeys();
                    // do we need WrapperManager.signalStopped() like in ConfigServiceHandler ???
                    // without it, the wrapper complains "shutdown unexpectedly"
                    // but we can't have that dependency in the router
                    _context.router().shutdown(Router.EXIT_HARD_RESTART);
                    // doesn't return
                }
            } else if (!fixedPort) {
                // save PROP_EXTERNAL_PORT
                _context.router().saveConfig(changes, null);
            }
            _context.router().rebuildRouterInfo();
        }
        _testEvent.forceRun();
        _testEvent.reschedule(5*1000);
        return updated;
    }

    private static final boolean eq(byte laddr[], int lport, byte raddr[], int rport) {
        return (rport == lport) && DataHelper.eq(laddr, raddr);
    }
    
    /** @param addr may be null */
    public final boolean isValid(byte addr[]) {
        if (addr == null) return false;
        if (isPubliclyRoutable(addr)) 
            return true;
        return _context.getBooleanProperty("i2np.udp.allowLocal");
    }
    
    /**
     *  Was true before 0.9.2
     *  Now false if we need introducers (as perhaps that's why we need them,
     *  our firewall is changing our port), unless overridden by the property.
     *  We must have an accurate external port when firewalled, or else
     *  our signature of the SessionCreated packet will be invalid.
     */
    private boolean getIsPortFixed() {
        String prop = _context.getProperty(PROP_FIXED_PORT);
        if (prop != null)
            return Boolean.parseBoolean(prop);
        int status = getReachabilityStatus();
        return status != CommSystemFacade.STATUS_REJECT_UNSOLICITED;
    }

    /** 
     * get the state for the peer at the given remote host/port, or null 
     * if no state exists
     */
    PeerState getPeerState(RemoteHostId hostInfo) {
            return _peersByRemoteHost.get(hostInfo);
    }

    /** 
     *  Get the states for all peers at the given remote host, ignoring port.
     *  Used for a last-chance search for a peer that changed port, by PacketHandler.
     *  @since 0.9.3
     */
    List<PeerState> getPeerStatesByIP(RemoteHostId hostInfo) {
        List<PeerState> rv = new ArrayList(4);
        byte[] ip = hostInfo.getIP();
        if (ip != null) {
            for (PeerState ps : _peersByIdent.values()) {
                if (DataHelper.eq(ip, ps.getRemoteIP()))
                    rv.add(ps);
            }
        }
        return rv;
    }
    
    /** 
     * get the state for the peer with the given ident, or null 
     * if no state exists
     */
    PeerState getPeerState(Hash remotePeer) { 
            return _peersByIdent.get(remotePeer);
    }
    
    /** 
     *  Remove and add to peersByRemoteHost map
     *  @since 0.9.3
     */
    public void changePeerPort(PeerState peer, int newPort) {
        int oldPort;
        synchronized (_addDropLock) {
            oldPort = peer.getRemotePort();
            if (oldPort != newPort) {
                _peersByRemoteHost.remove(peer.getRemoteHostId());
                peer.changePort(newPort);
                _peersByRemoteHost.put(peer.getRemoteHostId(), peer);
            }
        }
        if (_log.shouldLog(Log.WARN) && oldPort != newPort)
            _log.warn("Changed port from " + oldPort + " to " + newPort + " for " + peer);
    }

    /**
     *  For IntroductionManager
     *  @return may be null if not started
     *  @since 0.9.2
     */
    EstablishmentManager getEstablisher() {
        return _establisher;
    }
    /**
     * Intercept RouterInfo entries received directly from a peer to inject them into
     * the PeersByCapacity listing.
     *
     */
    /*
    public void messageReceived(I2NPMessage inMsg, RouterIdentity remoteIdent, Hash remoteIdentHash, long msToReceive, int bytesReceived) {
        
        if (inMsg instanceof DatabaseStoreMessage) {
            DatabaseStoreMessage dsm = (DatabaseStoreMessage)inMsg;
            if (dsm.getValueType() == DatabaseStoreMessage.KEY_TYPE_ROUTERINFO) {
                Hash from = remoteIdentHash;
                if (from == null)
                    from = remoteIdent.getHash();

                if (from.equals(dsm.getKey())) {
                    // db info received directly from the peer - inject it into the peersByCapacity
                    RouterInfo info = dsm.getRouterInfo();
                    Set addresses = info.getAddresses();
                    for (Iterator iter = addresses.iterator(); iter.hasNext(); ) {
                        RouterAddress addr = (RouterAddress)iter.next();
                        if (!STYLE.equals(addr.getTransportStyle()))
                            continue;
                        Properties opts = addr.getOptions();
                        if ( (opts != null) && (info.isValid()) ) {
                            String capacities = opts.getProperty(UDPAddress.PROP_CAPACITY);
                            if (capacities != null) {
                                if (_log.shouldLog(Log.INFO))
                                    _log.info("Intercepting and storing the capacities for " + from.toBase64() + ": " + capacities);
                                PeerState peer = getPeerState(from);
                                for (int i = 0; i < capacities.length(); i++) {
                                    char capacity = capacities.charAt(i);
                                    int cap = capacity - 'A';
                                    if ( (cap < 0) || (cap >= _peersByCapacity.length) ) 
                                        continue;
                                    List peers = _peersByCapacity[cap];
                                    synchronized (peers) {
                                        if ( (peers.size() < MAX_PEERS_PER_CAPACITY) && (!peers.contains(peer)) )
                                            peers.add(peer);
                                    }
                                }
                            }
                        }
                        // this was an SSU address so we're done now
                        break;
                    }
                }
            }
        }
        super.messageReceived(inMsg, remoteIdent, remoteIdentHash, msToReceive, bytesReceived);
    }
    */
    
    /** 
     * add the peer info, returning true if it went in properly, false if
     * it was rejected (causes include peer ident already connected, or no
     * remote host info known
     *
     */
    boolean addRemotePeerState(PeerState peer) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Add remote peer state: " + peer);
        synchronized(_addDropLock) {
            return locked_addRemotePeerState(peer);
        }
    }

    private boolean locked_addRemotePeerState(PeerState peer) {
        Hash remotePeer = peer.getRemotePeer();
        long oldEstablishedOn = -1;
        PeerState oldPeer = null;
        if (remotePeer != null) {
            oldPeer = _peersByIdent.put(remotePeer, peer);
            if ( (oldPeer != null) && (oldPeer != peer) ) {
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Peer already connected (PBID): old=" + oldPeer + " new=" + peer);
                // transfer over the old state/inbound message fragments/etc
                peer.loadFrom(oldPeer);
                oldEstablishedOn = oldPeer.getKeyEstablishedTime();
            }
        }
        
        RemoteHostId remoteId = peer.getRemoteHostId();
        if (oldPeer != null) {
            oldPeer.dropOutbound();
            _introManager.remove(oldPeer);
            _expireEvent.remove(oldPeer);
            RemoteHostId oldID = oldPeer.getRemoteHostId();
            if (!remoteId.equals(oldID)) {
                // leak fix, remove old address
                if (_log.shouldLog(Log.WARN))
                    _log.warn(remotePeer + " changed address FROM " + oldID + " TO " + remoteId);
                PeerState oldPeer2 = _peersByRemoteHost.remove(oldID);
                // different ones in the two maps? shouldn't happen
                if (oldPeer2 != oldPeer && oldPeer2 != null) {
                    oldPeer2.dropOutbound();
                     _introManager.remove(oldPeer2);
                    _expireEvent.remove(oldPeer2);
                }
            }
        }

        // Should always be direct... except maybe for hidden mode?
        // or do we always know the IP by now?
        if (remoteId.getIP() == null && _log.shouldLog(Log.WARN))
            _log.warn("Add indirect: " + peer);

        // don't do this twice
        PeerState oldPeer2 = _peersByRemoteHost.put(remoteId, peer);
        if (oldPeer2 != null && oldPeer2 != peer && oldPeer2 != oldPeer) {
            // this shouldn't happen, should have been removed above
            if (_log.shouldLog(Log.WARN))
                _log.warn("Peer already connected (PBRH): old=" + oldPeer2 + " new=" + peer);
            // transfer over the old state/inbound message fragments/etc
            peer.loadFrom(oldPeer2);
            oldEstablishedOn = oldPeer2.getKeyEstablishedTime();
            oldPeer2.dropOutbound();
            _introManager.remove(oldPeer2);
            _expireEvent.remove(oldPeer2);
        }

        if (_log.shouldLog(Log.WARN) && _peersByIdent.size() != _peersByRemoteHost.size())
            _log.warn("Size Mismatch after add: " + peer
                       + " byIDsz = " + _peersByIdent.size()
                       + " byHostsz = " + _peersByRemoteHost.size());
        
        _activeThrottle.unchoke(peer.getRemotePeer());
        markReachable(peer.getRemotePeer(), peer.isInbound());
        //_context.banlist().unbanlistRouter(peer.getRemotePeer(), STYLE);

        //if (SHOULD_FLOOD_PEERS)
        //    _flooder.addPeer(peer);
        
        _expireEvent.add(peer);
        
        _introManager.add(peer);
        
        if (oldEstablishedOn > 0)
            _context.statManager().addRateData("udp.alreadyConnected", oldEstablishedOn, 0);
        
        if (needsRebuild())
            rebuildExternalAddress();
        
        if (getReachabilityStatus() != CommSystemFacade.STATUS_OK) {
            _testEvent.forceRun();
            _testEvent.reschedule(0);
        }
        return true;
    }
    
/*** infinite loop
    public RouterAddress getCurrentAddress() {
        if (needsRebuild())
            rebuildExternalAddress(false);
        return super.getCurrentAddress();
    }
***/
    
    @Override
    public void messageReceived(I2NPMessage inMsg, RouterIdentity remoteIdent, Hash remoteIdentHash, long msToReceive, int bytesReceived) {
        if (inMsg.getType() == DatabaseStoreMessage.MESSAGE_TYPE) {
            DatabaseStoreMessage dsm = (DatabaseStoreMessage)inMsg;
            DatabaseEntry entry = dsm.getEntry();
            if (entry == null)
                return;
            if (entry.getType() == DatabaseEntry.KEY_TYPE_ROUTERINFO &&
                ((RouterInfo) entry).getNetworkId() != Router.NETWORK_ID) {
                // this is pre-0.6.1.10, so it isn't going to happen any more

                /*
                if (remoteIdentHash != null) {
                    _context.banlist().banlistRouter(remoteIdentHash, "Sent us a peer from the wrong network");
                    dropPeer(remoteIdentHash);
                    if (_log.shouldLog(Log.ERROR))
                        _log.error("Dropping the peer " + remoteIdentHash
                                   + " because they are in the wrong net");
                } else if (remoteIdent != null) {
                    _context.banlist().banlistRouter(remoteIdent.calculateHash(), "Sent us a peer from the wrong network");
                    dropPeer(remoteIdent.calculateHash());
                    if (_log.shouldLog(Log.ERROR))
                        _log.error("Dropping the peer " + remoteIdent.calculateHash()
                                   + " because they are in the wrong net");
                }
                 */
                Hash peerHash = entry.getHash();
                PeerState peer = getPeerState(peerHash);
                if (peer != null) {
                    RemoteHostId remote = peer.getRemoteHostId();
                    _dropList.add(remote);
                    _context.statManager().addRateData("udp.dropPeerDroplist", 1);
                    _context.simpleScheduler().addEvent(new RemoveDropList(remote), DROPLIST_PERIOD);
                }
                markUnreachable(peerHash);
                _context.banlist().banlistRouter(peerHash, "Part of the wrong network, version = " + ((RouterInfo) entry).getOption("router.version"));
                //_context.banlist().banlistRouter(peerHash, "Part of the wrong network", STYLE);
                dropPeer(peerHash, false, "wrong network");
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Dropping the peer " + peerHash + " because they are in the wrong net: " + entry);
                return;
            } else {
                if (entry.getType() == DatabaseEntry.KEY_TYPE_ROUTERINFO) {
                    if (_log.shouldLog(Log.INFO))
                        _log.info("Received an RI from the same net");
                } else {
                    if (_log.shouldLog(Log.INFO))
                        _log.info("Received a leaseSet: " + dsm);
                }
            }
        } else {
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Received another message: " + inMsg.getClass().getName());
        }
        PeerState peer = getPeerState(remoteIdentHash);
        super.messageReceived(inMsg, remoteIdent, remoteIdentHash, msToReceive, bytesReceived);
        if (peer != null)
            peer.expireInboundMessages();
    }

    private class RemoveDropList implements SimpleTimer.TimedEvent {
        private final RemoteHostId _peer;
        public RemoveDropList(RemoteHostId peer) { _peer = peer; }
        public void timeReached() { 
            _dropList.remove(_peer);
        }
    }
    
    boolean isInDropList(RemoteHostId peer) { return _dropList.contains(peer); }
    
    void dropPeer(Hash peer, boolean shouldBanlist, String why) {
        PeerState state = getPeerState(peer);
        if (state != null)
            dropPeer(state, shouldBanlist, why);
    }

    void dropPeer(PeerState peer, boolean shouldBanlist, String why) {
        if (_log.shouldLog(Log.INFO)) {
            long now = _context.clock().now();
            StringBuilder buf = new StringBuilder(4096);
            long timeSinceSend = now - peer.getLastSendTime();
            long timeSinceRecv = now - peer.getLastReceiveTime();
            long timeSinceAck  = now - peer.getLastACKSend();
            long timeSinceSendOK = now - peer.getLastSendFullyTime();
            int consec = peer.getConsecutiveFailedSends();
            buf.append("Dropping remote peer: ").append(peer.toString()).append(" banlist? ").append(shouldBanlist);
            buf.append(" lifetime: ").append(now - peer.getKeyEstablishedTime());
            buf.append(" time since send/fully/recv/ack: ").append(timeSinceSend).append(" / ");
            buf.append(timeSinceSendOK).append(" / ");
            buf.append(timeSinceRecv).append(" / ").append(timeSinceAck);
            buf.append(" consec failures: ").append(consec);
            if (why != null)
                buf.append(" cause: ").append(why);
            /*
            buf.append("Existing peers: \n");
            synchronized (_peersByIdent) {
                for (Iterator iter = _peersByIdent.keySet().iterator(); iter.hasNext(); ) {
                    Hash c = (Hash)iter.next();
                    PeerState p = (PeerState)_peersByIdent.get(c);
                    if (c.equals(peer.getRemotePeer())) {
                        if (p != peer) {
                            buf.append(" SAME PEER, DIFFERENT STATE ");
                        } else {
                            buf.append(" same peer, same state ");
                        }
                    } else {
                        buf.append("Peer ").append(p.toString()).append(" ");
                    }
                    
                    buf.append(" lifetime: ").append(now - p.getKeyEstablishedTime());
                    
                    timeSinceSend = now - p.getLastSendTime();
                    timeSinceRecv = now - p.getLastReceiveTime();
                    timeSinceAck  = now - p.getLastACKSend();
                    
                    buf.append(" time since send/recv/ack: ").append(timeSinceSend).append(" / ");
                    buf.append(timeSinceRecv).append(" / ").append(timeSinceAck);
                    buf.append("\n");
                }
            }
             */
            _log.info(buf.toString(), new Exception("Dropped by"));
        }
        synchronized(_addDropLock) {
            locked_dropPeer(peer, shouldBanlist, why);
        }
        if (needsRebuild())
            rebuildExternalAddress();
    }

    private void locked_dropPeer(PeerState peer, boolean shouldBanlist, String why) {
        peer.dropOutbound();
        peer.expireInboundMessages();
        _introManager.remove(peer);
        _fragments.dropPeer(peer);
        
        PeerState altByIdent = null;
        if (peer.getRemotePeer() != null) {
            dropPeerCapacities(peer);
            
            if (shouldBanlist) {
                markUnreachable(peer.getRemotePeer());
                //_context.banlist().banlistRouter(peer.getRemotePeer(), "dropped after too many retries", STYLE);
            }
            long now = _context.clock().now();
            _context.statManager().addRateData("udp.droppedPeer", now - peer.getLastReceiveTime(), now - peer.getKeyEstablishedTime());
            altByIdent = _peersByIdent.remove(peer.getRemotePeer());
        }
        
        RemoteHostId remoteId = peer.getRemoteHostId();
        PeerState altByHost = _peersByRemoteHost.remove(remoteId);

        if (altByIdent != altByHost && _log.shouldLog(Log.WARN)) 
            _log.warn("Mismatch on remove, RHID = " + remoteId
                      + " byID = " + altByIdent
                      + " byHost = " + altByHost
                      + " byIDsz = " + _peersByIdent.size()
                      + " byHostsz = " + _peersByRemoteHost.size());
        
        // unchoke 'em, but just because we'll never talk again...
        _activeThrottle.unchoke(peer.getRemotePeer());
        
        //if (SHOULD_FLOOD_PEERS)
        //    _flooder.removePeer(peer);
        _expireEvent.remove(peer);
        
        // deal with races to make sure we drop the peers fully
        if ( (altByIdent != null) && (peer != altByIdent) ) locked_dropPeer(altByIdent, shouldBanlist, "recurse");
        if ( (altByHost != null) && (peer != altByHost) ) locked_dropPeer(altByHost, shouldBanlist, "recurse");
    }
    
    private boolean needsRebuild() {
        if (_needsRebuild) return true; // simple enough
        if (_context.router().isHidden()) return false;
        if (introducersRequired()) {
            RouterAddress addr = _externalAddress;
            UDPAddress ua = new UDPAddress(addr);
            int valid = 0;
            for (int i = 0; i < ua.getIntroducerCount(); i++) {
                // warning: this is only valid as long as we use the ident hash as their key.
                PeerState peer = getPeerState(Hash.create(ua.getIntroducerKey(i)));
                if (peer != null)
                    valid++;
            }
            if (valid >= PUBLIC_RELAY_COUNT) {
                // try to shift 'em around every 10 minutes or so
                if (_introducersSelectedOn < _context.clock().now() - 10*60*1000) {
                    if (_log.shouldLog(Log.WARN))
                        _log.warn("Our introducers are valid, but havent changed in a while, so lets rechoose");
                    return true;
                } else {
                    if (_log.shouldLog(Log.INFO))
                        _log.info("Our introducers are valid and haven't changed in a while");
                    return false;
                }
            } else {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Need more introducers (have " +valid + " need " + PUBLIC_RELAY_COUNT + ')');
                return true;
            }
        } else {
            boolean rv = (_externalListenHost == null) || (_externalListenPort <= 0);
            if (!rv) {
                RouterAddress addr = _externalAddress;
                UDPAddress ua = new UDPAddress(addr);
                if (ua.getIntroducerCount() > 0)
                    rv = true;  // status == ok and we don't actually need introducers, so rebuild
            }
            if (_log.shouldLog(Log.INFO)) {
                if (rv) {
                    _log.info("Need to initialize our direct SSU info (" + _externalListenHost + ":" + _externalListenPort + ")");
                } else {
                    RouterAddress addr = _externalAddress;
                    UDPAddress ua = new UDPAddress(addr);
                    if ( (ua.getPort() <= 0) || (ua.getHost() == null) ) {
                        _log.info("Our direct SSU info is initialized, but not used in our address yet");
                        rv = true;
                    } else {
                        //_log.info("Our direct SSU info is initialized");
                    }
                }
            }
            return rv;
        }
    }
    
    /**
     * Make sure we don't think this dropped peer is capable of doing anything anymore...
     *
     */
    private void dropPeerCapacities(PeerState peer) {
        /*
        RouterInfo info = _context.netDb().lookupRouterInfoLocally(peer.getRemotePeer());
        if (info != null) {
            String capacities = info.getOptions().getProperty(UDPAddress.PROP_CAPACITY);
            if (capacities != null) {
                for (int i = 0; i < capacities.length(); i++) {
                    char capacity = capacities.charAt(i);
                    int cap = capacity - 'A';
                    if ( (cap < 0) || (cap >= _peersByCapacity.length) ) 
                        continue;
                    List peers = _peersByCapacity[cap];
                    synchronized (peers) {
                        peers.remove(peer);
                    }
                }
            }
        }
         */
    }
    
    /**
     *  This sends it directly out, bypassing OutboundMessageFragments
     *  and the PacketPusher. The only queueing is for the bandwidth limiter.
     *  BLOCKING if OB queue is full.
     */
    void send(UDPPacket packet) { 
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Sending packet " + packet);
        _endpoint.send(packet); 
    }
    
    /**
     *  Send a session destroy message, bypassing OMF and PacketPusher.
     *  BLOCKING if OB queue is full.
     *
     *  @since 0.8.9
     */
    private void sendDestroy(PeerState peer) {
        // peer must be fully established
        if (peer.getCurrentCipherKey() == null)
            return;
        UDPPacket pkt = _destroyBuilder.buildSessionDestroyPacket(peer);
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Sending destroy to : " + peer);
        send(pkt);
    }

    /**
     *  Send a session destroy message to everybody.
     *  BLOCKING for at least 1 sec per 1K peers, more if BW is very low or if OB queue is full.
     *
     *  @since 0.8.9
     */
    private void destroyAll() {
        _endpoint.clearOutbound();
        int howMany = _peersByIdent.size();
        // use no more than 1/4 of configured bandwidth
        final int burst = 8;
        int pps = Math.max(48, (_context.bandwidthLimiter().getOutboundKBytesPerSecond() * 1000 / 4) /  48);
        int burstps = pps / burst;
        // max of 1000 pps
        int toSleep = Math.max(8, (1000 / burstps));
        int count = 0;
        if (_log.shouldLog(Log.WARN))
            _log.warn("Sending destroy to : " + howMany + " peers");
        for (PeerState peer : _peersByIdent.values()) {
            sendDestroy(peer);
            // 1000 per second * 48 bytes = 400 KBps
            if ((++count) % burst == 0) { 
                try {
                    Thread.sleep(toSleep);
                } catch (InterruptedException ie) {}
            }
        }
        toSleep = Math.min(howMany / 3, 750);
        if (toSleep > 0) {
            try {
                Thread.sleep(toSleep);
            } catch (InterruptedException ie) {}
        }
    }

    /** minimum active peers to maintain IP detection, etc. */
    private static final int MIN_PEERS = 3;
    /** minimum peers volunteering to be introducers if we need that */
    private static final int MIN_INTRODUCER_POOL = 5;

    public TransportBid bid(RouterInfo toAddress, long dataSize) {
        if (dataSize > OutboundMessageState.MAX_MSG_SIZE) {
            // NTCP max is lower, so msg will get dropped
            return null;
        }
        Hash to = toAddress.getIdentity().calculateHash();
        PeerState peer = getPeerState(to);
        if (peer != null) {
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("bidding on a message to an established peer: " + peer);
            if (preferUDP())
                return _cachedBid[FAST_PREFERRED_BID];
            else
                return _cachedBid[FAST_BID];
        } else {
            // If we don't have a port, all is lost
            if ( _reachabilityStatus == CommSystemFacade.STATUS_HOSED) {
                markUnreachable(to);
                return null;
            }

            // Validate his SSU address
            RouterAddress addr = toAddress.getTargetAddress(STYLE);
            if (addr == null) {
                markUnreachable(to);
                return null;
            }

            // don't do this - object churn parsing the whole thing
            //UDPAddress ua = new UDPAddress(addr);
            //if (ua.getIntroducerCount() <= 0) {
            if (addr.getOption("ihost0") == null) {
                byte[] ip = addr.getIP();
                int port = addr.getPort();
                if (ip == null || port < MIN_PEER_PORT ||
                    (!isValid(ip)) ||
                    Arrays.equals(ip, getExternalIP())) {
                    markUnreachable(to);
                    return null;
                }
            }
            if (!allowConnection())
                return _cachedBid[TRANSIENT_FAIL_BID];

            if (_log.shouldLog(Log.DEBUG))
                _log.debug("bidding on a message to an unestablished peer: " + to);

            // Try to maintain at least 3 peers so we can determine our IP address and
            // we have a selection to run peer tests with.
            // If we are firewalled, and we don't have enough peers that volunteered to
            // also introduce us, also bid aggressively so we are preferred over NTCP.
            // (Otherwise we only talk UDP to those that are firewalled, and we will
            // never get any introducers)
            int count = _peersByIdent.size();
            if (alwaysPreferUDP() || count < MIN_PEERS ||
                (introducersRequired() && _introManager.introducerCount() < MIN_INTRODUCER_POOL))
                return _cachedBid[SLOW_PREFERRED_BID];
            else if (preferUDP())
                return _cachedBid[SLOW_BID];
            else if (haveCapacity()) {
                if (addr.getCost() > DEFAULT_COST)
                    return _cachedBid[SLOWEST_COST_BID];
                else
                    return _cachedBid[SLOWEST_BID];
            } else {
                if (addr.getCost() > DEFAULT_COST)
                    return _cachedBid[NEAR_CAPACITY_COST_BID];
                else
                    return _cachedBid[NEAR_CAPACITY_BID];
            }
        }
    }

    private boolean preferUDP() {
        String pref = _context.getProperty(PROP_PREFER_UDP, DEFAULT_PREFER_UDP);
        return (pref != null) && ! "false".equals(pref);
    }
    
    private boolean alwaysPreferUDP() {
        String pref = _context.getProperty(PROP_PREFER_UDP, DEFAULT_PREFER_UDP);
        return (pref != null) && "always".equals(pref);
    }
    
    // We used to have MAX_IDLE_TIME = 5m, but this causes us to drop peers
    // and lose the old introducer tags, causing introduction fails,
    // so we keep the max time long to give the introducer keepalive code
    // in the IntroductionManager a chance to work.
    public static final int EXPIRE_TIMEOUT = 20*60*1000;
    private static final int MAX_IDLE_TIME = EXPIRE_TIMEOUT;
    public static final int MIN_EXPIRE_TIMEOUT = 6*60*1000;
    
    public String getStyle() { return STYLE; }

    @Override
    public void send(OutNetMessage msg) { 
        if (msg == null) return;
        if (msg.getTarget() == null) return;
        if (msg.getTarget().getIdentity() == null) return;
        if (_establisher == null) {
            failed(msg, "UDP not up yet");
            return;    
        }

        msg.timestamp("sending on UDP transport");
        Hash to = msg.getTarget().getIdentity().calculateHash();
        PeerState peer = getPeerState(to);
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Sending to " + (to != null ? to.toString() : ""));
        if (peer != null) {
            long lastSend = peer.getLastSendFullyTime();
            long lastRecv = peer.getLastReceiveTime();
            long now = _context.clock().now();
            int inboundActive = peer.expireInboundMessages();
            if ( (lastSend > 0) && (lastRecv > 0) ) {
                if ( (now - lastSend > MAX_IDLE_TIME) && 
                     (now - lastRecv > MAX_IDLE_TIME) && 
                     (peer.getConsecutiveFailedSends() > 0) &&
                     (inboundActive <= 0)) {
                    // peer is waaaay idle, drop the con and queue it up as a new con
                    dropPeer(peer, false, "proactive reconnection");
                    msg.timestamp("peer is really idle, dropping con and reestablishing");
                    if (_log.shouldLog(Log.DEBUG))
                        _log.debug("Proactive reestablish to " + to);
                    _establisher.establish(msg);
                    _context.statManager().addRateData("udp.proactiveReestablish", now-lastSend, now-peer.getKeyEstablishedTime());
                    return;
                }
            }
            msg.timestamp("enqueueing for an already established peer");
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Add to fragments for " + to);

            // See comments in DummyThrottle.java
            if (USE_PRIORITY)
                _outboundMessages.add(msg);
            else  // skip the priority queue and go straight to the active pool
                _fragments.add(msg);
        } else {
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Establish new connection to " + to);
            msg.timestamp("establishing a new connection");
            _establisher.establish(msg);
        }
    }

    /**
     *  Send only if established, otherwise fail immediately.
     *  Never queue with the establisher.
     *  @since 0.9.2
     */
    void sendIfEstablished(OutNetMessage msg) { 
        _fragments.add(msg);
    }

    /**
     *  "injected" message from the EstablishmentManager
     */
    void send(I2NPMessage msg, PeerState peer) {
        try {
            OutboundMessageState state = new OutboundMessageState(_context, msg, peer);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Injecting a data message to a new peer: " + peer);
            _fragments.add(state);
        } catch (IllegalArgumentException iae) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("Shouldnt happen", new Exception("I did it"));
        }
    }
    
    // we don't need the following, since we have our own queueing
    protected void outboundMessageReady() { throw new UnsupportedOperationException("Not used for UDP"); }
    
    public RouterAddress startListening() {
        startup();
        return _externalAddress;
    }
    
    public void stopListening() {
        shutdown();
        // will this work?
        _externalAddress = null;
        replaceAddress(null);
    }
    
    private boolean explicitAddressSpecified() {
        String h = _context.getProperty(PROP_EXTERNAL_HOST);
        // Bug in config.jsp prior to 0.7.14, sets an empty host config
        return h != null && h.length() > 0;
    }
    
    /**
     * Rebuild to get updated cost and introducers.
     * Do not tell the router (he is the one calling this)
     * @since 0.7.12
     */
    @Override
    public RouterAddress updateAddress() {
        rebuildExternalAddress(false);
        return getCurrentAddress();
    }

    private void rebuildExternalAddress() { rebuildExternalAddress(true); }

    private void rebuildExternalAddress(boolean allowRebuildRouterInfo) {
        // if the external port is specified, we want to use that to bind to even
        // if we don't know the external host.
        _externalListenPort = _context.getProperty(PROP_EXTERNAL_PORT, -1);
        
        if (explicitAddressSpecified()) {
            try {
                String host = _context.getProperty(PROP_EXTERNAL_HOST);
                _externalListenHost = InetAddress.getByName(host);
            } catch (UnknownHostException uhe) {
                _externalListenHost = null;
            }
        }
            
        if (_context.router().isHidden())
            return;
        
        Properties options = new Properties(); 
        boolean directIncluded = false;
        if ( allowDirectUDP() && (_externalListenPort > 0) && (_externalListenHost != null) && (isValid(_externalListenHost.getAddress())) ) {
            options.setProperty(UDPAddress.PROP_PORT, String.valueOf(_externalListenPort));
            options.setProperty(UDPAddress.PROP_HOST, _externalListenHost.getHostAddress());
            directIncluded = true;
        }
        
        boolean introducersRequired = introducersRequired();
        boolean introducersIncluded = false;
        if (introducersRequired || !directIncluded) {
            int found = _introManager.pickInbound(options, PUBLIC_RELAY_COUNT);
            if (found > 0) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Picked peers: " + found);
                _introducersSelectedOn = _context.clock().now();
                introducersIncluded = true;
            } else {
                // FIXME
                // maybe we should fail to publish an address at all in this case?
                // YES that would be better
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Need introducers but we don't know any");
            }
        }
        
        // if we have explicit external addresses, they had better be reachable
        if (introducersRequired)
            options.setProperty(UDPAddress.PROP_CAPACITY, ""+UDPAddress.CAPACITY_TESTING);
        else
            options.setProperty(UDPAddress.PROP_CAPACITY, ""+UDPAddress.CAPACITY_TESTING + UDPAddress.CAPACITY_INTRODUCER);

        // MTU since 0.9.2
        if (_mtu < PeerState.LARGE_MTU)
            options.setProperty(UDPAddress.PROP_MTU, Integer.toString(_mtu));

        if (directIncluded || introducersIncluded) {
            // This is called via TransportManager.configTransports() before startup(), prevent NPE
            if (_introKey != null)
                options.setProperty(UDPAddress.PROP_INTRO_KEY, _introKey.toBase64());

            RouterAddress addr = new RouterAddress();
            // SSU seems to regulate at about 85%, so make it a little higher.
            // If this is too low, both NTCP and SSU always have incremented cost and
            // the whole mechanism is not helpful.
            if (ADJUST_COST && !haveCapacity(91))
                addr.setCost(DEFAULT_COST + 1);
            else
                addr.setCost(DEFAULT_COST);
            //addr.setExpiration(null);
            addr.setTransportStyle(STYLE);
            addr.setOptions(options);

            boolean wantsRebuild = false;
            if ( (_externalAddress == null) || !(_externalAddress.equals(addr)) )
                wantsRebuild = true;

            RouterAddress oldAddress = _externalAddress;
            _externalAddress = addr;
            if (_log.shouldLog(Log.INFO))
                _log.info("Address rebuilt: " + addr);
            replaceAddress(addr, oldAddress);
            if (allowRebuildRouterInfo && wantsRebuild)
                _context.router().rebuildRouterInfo();
            _needsRebuild = false;
        } else {
            if (_log.shouldLog(Log.WARN))
                _log.warn("Wanted to rebuild my SSU address, but couldn't specify either the direct or indirect info (needs introducers? " 
                           + introducersRequired + ")", new Exception("source"));
            _needsRebuild = true;
        }
    }

    /**
     * Replace then tell NTCP that we changed.
     */
    @Override
    protected void replaceAddress(RouterAddress address) {
        super.replaceAddress(address);
        _context.commSystem().notifyReplaceAddress(address);
    }

    protected void replaceAddress(RouterAddress address, RouterAddress oldAddress) {
        replaceAddress(address);
        if (oldAddress != null) {
            UDPAddress old = new UDPAddress(oldAddress);
            InetAddress oldHost = old.getHostAddress();
            UDPAddress newAddr = new UDPAddress(address);
            InetAddress newHost = newAddr.getHostAddress();
            if ( (old.getPort() > 0) && (oldHost != null) && (isValid(oldHost.getAddress())) &&
                 (newAddr.getPort() > 0) && (newHost != null) && (isValid(newHost.getAddress())) ) {
                if ( (old.getPort() != newAddr.getPort()) || (!oldHost.equals(newHost)) ) {
                    // substantial data has changed, so if we are in 'dynamic keys' mode, restart the 
                    // router hard and regenerate a new identity
                    if (_context.getBooleanProperty(Router.PROP_DYNAMIC_KEYS)) {
                        if (_log.shouldLog(Log.ERROR))
                            _log.error("SSU address updated. new address: " 
                                       + newAddr.getHostAddress() + ":" + newAddr.getPort() + ", old address: " 
                                       + old.getHostAddress() + ":" + old.getPort());
                        // shutdown itself checks the DYNAMIC_KEYS flag, and if its set to true, deletes
                        // the keys
                        _context.router().shutdown(Router.EXIT_HARD_RESTART);              
                    }
                }
            }
        }
    }
    
    public boolean introducersRequired() {
        /******************
         *  Don't do this anymore, as we are removing the checkbox from the UI,
         *  and we rarely if ever see the problem of false negatives for firewall detection -
         *  it's usually false positives.
         ******************
        String forceIntroducers = _context.getProperty(PROP_FORCE_INTRODUCERS);
        if ( (forceIntroducers != null) && (Boolean.parseBoolean(forceIntroducers)) ) {
            if (_log.shouldLog(Log.INFO))
                _log.info("Force introducers specified");
            return true;
        }
        *******************/
        short status = getReachabilityStatus();
        switch (status) {
            case CommSystemFacade.STATUS_REJECT_UNSOLICITED:
            case CommSystemFacade.STATUS_DIFFERENT:
                if (_log.shouldLog(Log.DEBUG))
                    _log.debug("Require introducers, because our status is " + status);
                return true;
            default:
                if (!allowDirectUDP()) {
                    if (_log.shouldLog(Log.DEBUG))
                        _log.debug("Require introducers, because we do not allow direct UDP connections");
                    return true;
                }
                return false;
        }
    }
    
    /**
     *  For EstablishmentManager
     *  @since 0.9.3
     */
    boolean canIntroduce() {
        // we don't expect inbound connections when hidden, but it could happen
        // Don't offer if we are approaching max connections. While Relay Intros do not
        // count as connections, we have to keep the connection to this peer up longer if
        // we are offering introductions.
        return
            (!_context.router().isHidden()) &&
            (!introducersRequired()) &&
            haveCapacity() &&
            (!((FloodfillNetworkDatabaseFacade)_context.netDb()).floodfillEnabled()) &&
            _introManager.introducedCount() < IntroductionManager.MAX_OUTBOUND &&
            _introManager.introducedCount() < getMaxConnections() / 4;
    }

    /** default true */
    private boolean allowDirectUDP() {
        return _context.getBooleanPropertyDefaultTrue(PROP_ALLOW_DIRECT);
    }

    String getPacketHandlerStatus() {
        PacketHandler handler = _handler;
        if (handler != null)
            return handler.getHandlerStatus();
        else
            return "";
    }

    /** @since 0.8.8 */
    int getPacketHandlerCount() {
        PacketHandler handler = _handler;
        if (handler != null)
            return handler.getHandlerCount();
        else
            return 0;
    }

    public void failed(OutboundMessageState msg) { failed(msg, true); }

    void failed(OutboundMessageState msg, boolean allowPeerFailure) {
        if (msg == null) return;
        int consecutive = 0;
        OutNetMessage m = msg.getMessage();
        if ( allowPeerFailure && (msg.getPeer() != null) && 
             ( (msg.getMaxSends() >= OutboundMessageFragments.MAX_VOLLEYS) ||
               (msg.isExpired())) ) {
            //long recvDelay = _context.clock().now() - msg.getPeer().getLastReceiveTime();
            //long sendDelay = _context.clock().now() - msg.getPeer().getLastSendFullyTime();
            //if (m != null)
            //    m.timestamp("message failure - volleys = " + msg.getMaxSends() 
            //                + " lastReceived: " + recvDelay
            //                + " lastSentFully: " + sendDelay
            //                + " expired? " + msg.isExpired());
            consecutive = msg.getPeer().incrementConsecutiveFailedSends();
            if (_log.shouldLog(Log.INFO))
                _log.info("Consecutive failure #" + consecutive 
                          + " on " + msg.toString()
                          + " to " + msg.getPeer());
            if ( (_context.clock().now() - msg.getPeer().getLastSendFullyTime() <= 60*1000) || (consecutive < MAX_CONSECUTIVE_FAILED) ) {
                // ok, a few conseutive failures, but we /are/ getting through to them
            } else {
                _context.statManager().addRateData("udp.dropPeerConsecutiveFailures", consecutive, msg.getPeer().getInactivityTime());
                dropPeer(msg.getPeer(), false, "too many failures");
            }
            //if ( (consecutive > MAX_CONSECUTIVE_FAILED) && (msg.getPeer().getInactivityTime() > DROP_INACTIVITY_TIME))
            //    dropPeer(msg.getPeer(), false);
            //else if (consecutive > 2 * MAX_CONSECUTIVE_FAILED) // they're sending us data, but we cant reply?
            //    dropPeer(msg.getPeer(), false);
        }
        noteSend(msg, false);
        if (m != null)
            super.afterSend(m, false);
    }
    
    private void noteSend(OutboundMessageState msg, boolean successful) {
        // bail before we do all the work
        if (!_context.messageHistory().getDoLog())
            return;
        int pushCount = msg.getPushCount();
        int sends = msg.getMaxSends();
        boolean expired = msg.isExpired();
     
        OutNetMessage m = msg.getMessage();
        PeerState p = msg.getPeer();
        StringBuilder buf = new StringBuilder(64);
        buf.append(" lifetime: ").append(msg.getLifetime());
        buf.append(" sends: ").append(sends);
        buf.append(" pushes: ").append(pushCount);
        buf.append(" expired? ").append(expired);
        buf.append(" unacked: ").append(msg.getUnackedSize());
        if ( (p != null) && (!successful) ) {
            buf.append(" consec_failed: ").append(p.getConsecutiveFailedSends());
            long timeSinceSend = _context.clock().now() - p.getLastSendFullyTime();
            buf.append(" lastFullSend: ").append(timeSinceSend);
            long timeSinceRecv = _context.clock().now() - p.getLastReceiveTime();
            buf.append(" lastRecv: ").append(timeSinceRecv);
            buf.append(" xfer: ").append(p.getSendBps()).append("/").append(p.getReceiveBps());
            buf.append(" mtu: ").append(p.getMTU());
            buf.append(" rto: ").append(p.getRTO());
            buf.append(" sent: ").append(p.getMessagesSent()).append("/").append(p.getPacketsTransmitted());
            buf.append(" recv: ").append(p.getMessagesReceived()).append("/").append(p.getPacketsReceived());
            buf.append(" uptime: ").append(_context.clock().now()-p.getKeyEstablishedTime());
        }
        if ( (m != null) && (p != null) ) {
            _context.messageHistory().sendMessage(m.getMessageType(), msg.getMessageId(), m.getExpiration(), 
                                                  p.getRemotePeer(), successful, buf.toString());
        } else {
            _context.messageHistory().sendMessage("establish", msg.getMessageId(), -1, 
                                                  (p != null ? p.getRemotePeer() : null), successful, buf.toString());
        }
    }
    
    public void failed(OutNetMessage msg, String reason) {
        if (msg == null) return;
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending message failed: " + msg, new Exception("failed from"));
        
        if (_context.messageHistory().getDoLog())
            _context.messageHistory().sendMessage(msg.getMessageType(), msg.getMessageId(), msg.getExpiration(), 
                                              msg.getTarget().getIdentity().calculateHash(), false, reason);
        super.afterSend(msg, false);
    }

    public void succeeded(OutboundMessageState msg) {
        if (msg == null) return;
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Sending message succeeded: " + msg);
        noteSend(msg, true);
        OutNetMessage m = msg.getMessage();
        if (m != null)
            super.afterSend(m, true);
    }

    @Override
    public int countPeers() {
            return _peersByIdent.size();
    }

    @Override
    public int countActivePeers() {
        long now = _context.clock().now();
        int active = 0;
        for (Iterator<PeerState> iter = _peersByIdent.values().iterator(); iter.hasNext(); ) {
                PeerState peer = iter.next();
                if (now-peer.getLastReceiveTime() <= 5*60*1000)
                    active++;
            }
        return active;
    }
    
    @Override
    public int countActiveSendPeers() {
        long now = _context.clock().now();
        int active = 0;
        for (Iterator<PeerState> iter = _peersByIdent.values().iterator(); iter.hasNext(); ) {
                PeerState peer = iter.next();
                if (now-peer.getLastSendFullyTime() <= 1*60*1000)
                    active++;
            }
        return active;
    }
    
    @Override
    public boolean isEstablished(Hash dest) {
        return getPeerState(dest) != null;
    }

    /**
     *  @since 0.9.3
     */
    @Override
    public boolean isBacklogged(Hash dest) {
        PeerState peer =  _peersByIdent.get(dest);
        return peer != null && peer.isBacklogged();
    }

    public boolean allowConnection() {
            return _peersByIdent.size() < getMaxConnections();
    }

    /**
     * Return our peer clock skews on this transport.
     * Vector composed of Long, each element representing a peer skew in seconds.
     */
    @Override
    public Vector<Long> getClockSkews() {
        Vector<Long> skews = new Vector();

        // If our clock is way off, we may not have many (or any) successful connections,
        // so try hard in that case to return good data
        boolean includeEverybody = _context.router().getUptime() < 10*60*1000 || _peersByIdent.size() < 10;
        long now = _context.clock().now();
        for (PeerState peer : _peersByIdent.values()) {
            if ((!includeEverybody) && now - peer.getLastReceiveTime() > 5*60*1000)
                continue; // skip old peers
            if (peer.getRTT() > PeerState.INIT_RTT - 250)
                continue; // Big RTT makes for a poor calculation
            skews.addElement(Long.valueOf(peer.getClockSkew() / 1000));
        }
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("UDP transport returning " + skews.size() + " peer clock skews.");
        return skews;
    }
    
    /**
     *  @return a new DHSessionKeyBuilder
     *  @since 0.9
     */
    DHSessionKeyBuilder getDHBuilder() {
        return _dhFactory.getBuilder();
    }
    
    /**
     *  @return the factory
     *  @since 0.9.2
     */
    DHSessionKeyBuilder.Factory getDHFactory() {
        return _dhFactory;
    }

    private static final int FLAG_ALPHA = 0;
    private static final int FLAG_IDLE_IN = 1;
    private static final int FLAG_IDLE_OUT = 2;
    private static final int FLAG_RATE_IN = 3;
    private static final int FLAG_RATE_OUT = 4;
    private static final int FLAG_SKEW = 5;
    private static final int FLAG_CWND= 6;
    private static final int FLAG_SSTHRESH = 7;
    private static final int FLAG_RTT = 8;
    //private static final int FLAG_DEV = 9;
    private static final int FLAG_RTO = 10;
    private static final int FLAG_MTU = 11;
    private static final int FLAG_SEND = 12;
    private static final int FLAG_RECV = 13;
    private static final int FLAG_RESEND = 14;
    private static final int FLAG_DUP = 15;
    private static final int FLAG_UPTIME = 16;
    private static final int FLAG_DEBUG = 99;
    
    private static Comparator getComparator(int sortFlags) {
        Comparator rv = null;
        switch (Math.abs(sortFlags)) {
            case FLAG_IDLE_IN:
                rv = IdleInComparator.instance();
                break;
            case FLAG_IDLE_OUT:
                rv = IdleOutComparator.instance();
                break;
            case FLAG_RATE_IN:
                rv = RateInComparator.instance();
                break;
            case FLAG_RATE_OUT:
                rv = RateOutComparator.instance();
                break;
            case FLAG_UPTIME:
                rv = UptimeComparator.instance();
                break;
            case FLAG_SKEW:
                rv = SkewComparator.instance();
                break;
            case FLAG_CWND:
                rv = CwndComparator.instance();
                break;
            case FLAG_SSTHRESH:
                rv = SsthreshComparator.instance();
                break;
            case FLAG_RTT:
                rv = RTTComparator.instance();
                break;
            //case FLAG_DEV:
            //    rv = DevComparator.instance();
            //    break;
            case FLAG_RTO:
                rv = RTOComparator.instance();
                break;
            case FLAG_MTU:
                rv = MTUComparator.instance();
                break;
            case FLAG_SEND:
                rv = SendCountComparator.instance();
                break;
            case FLAG_RECV:
                rv = RecvCountComparator.instance();
                break;
            case FLAG_RESEND:
                rv = ResendComparator.instance();
                break;
            case FLAG_DUP:
                rv = DupComparator.instance();
                break;
            case FLAG_ALPHA:
            default:
                rv = AlphaComparator.instance();
                break;
        }
        if (sortFlags < 0)
            rv = Collections.reverseOrder(rv);
        return rv;
    }

    private static class AlphaComparator extends PeerComparator {
        private static final AlphaComparator _instance = new AlphaComparator();
        public static final AlphaComparator instance() { return _instance; }
    }
    private static class IdleInComparator extends PeerComparator {
        private static final IdleInComparator _instance = new IdleInComparator();
        public static final IdleInComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = r.getLastReceiveTime() - l.getLastReceiveTime();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    private static class IdleOutComparator extends PeerComparator {
        private static final IdleOutComparator _instance = new IdleOutComparator();
        public static final IdleOutComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = r.getLastSendTime() - l.getLastSendTime();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    private static class RateInComparator extends PeerComparator {
        private static final RateInComparator _instance = new RateInComparator();
        public static final RateInComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            int rv = l.getReceiveBps() - r.getReceiveBps();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return rv;
        }
    }
    private static class RateOutComparator extends PeerComparator {
        private static final RateOutComparator _instance = new RateOutComparator();
        public static final RateOutComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            int rv = l.getSendBps() - r.getSendBps();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return rv;
        }
    }
    private static class UptimeComparator extends PeerComparator {
        private static final UptimeComparator _instance = new UptimeComparator();
        public static final UptimeComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = r.getKeyEstablishedTime() - l.getKeyEstablishedTime();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    private static class SkewComparator extends PeerComparator {
        private static final SkewComparator _instance = new SkewComparator();
        public static final SkewComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = Math.abs(l.getClockSkew()) - Math.abs(r.getClockSkew());
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    private static class CwndComparator extends PeerComparator {
        private static final CwndComparator _instance = new CwndComparator();
        public static final CwndComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            int rv = l.getSendWindowBytes() - r.getSendWindowBytes();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return rv;
        }
    }
    private static class SsthreshComparator extends PeerComparator {
        private static final SsthreshComparator _instance = new SsthreshComparator();
        public static final SsthreshComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            int rv = l.getSlowStartThreshold() - r.getSlowStartThreshold();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return rv;
        }
    }
    private static class RTTComparator extends PeerComparator {
        private static final RTTComparator _instance = new RTTComparator();
        public static final RTTComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            int rv = l.getRTT() - r.getRTT();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return rv;
        }
    }

 /***
    private static class DevComparator extends PeerComparator {
        private static final DevComparator _instance = new DevComparator();
        public static final DevComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = l.getRTTDeviation() - r.getRTTDeviation();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
  ****/

    /** */
    private static class RTOComparator extends PeerComparator {
        private static final RTOComparator _instance = new RTOComparator();
        public static final RTOComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            int rv = l.getRTO() - r.getRTO();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return rv;
        }
    }
    private static class MTUComparator extends PeerComparator {
        private static final MTUComparator _instance = new MTUComparator();
        public static final MTUComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            int rv = l.getMTU() - r.getMTU();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return rv;
        }
    }
    private static class SendCountComparator extends PeerComparator {
        private static final SendCountComparator _instance = new SendCountComparator();
        public static final SendCountComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = l.getPacketsTransmitted() - r.getPacketsTransmitted();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    private static class RecvCountComparator extends PeerComparator {
        private static final RecvCountComparator _instance = new RecvCountComparator();
        public static final RecvCountComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = l.getPacketsReceived() - r.getPacketsReceived();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    private static class ResendComparator extends PeerComparator {
        private static final ResendComparator _instance = new ResendComparator();
        public static final ResendComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = l.getPacketsRetransmitted() - r.getPacketsRetransmitted();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    private static class DupComparator extends PeerComparator {
        private static final DupComparator _instance = new DupComparator();
        public static final DupComparator instance() { return _instance; }
        @Override
        public int compare(PeerState l, PeerState r) {
            long rv = l.getPacketsReceivedDuplicate() - r.getPacketsReceivedDuplicate();
            if (rv == 0) // fallback on alpha
                return super.compare(l, r);
            else
                return (int)rv;
        }
    }
    
    private static class PeerComparator implements Comparator<PeerState> {
        public int compare(PeerState l, PeerState r) {
            return DataHelper.compareTo(l.getRemotePeer().getData(), r.getRemotePeer().getData());
        }
    }

    private static void appendSortLinks(StringBuilder buf, String urlBase, int sortFlags, String descr, int ascending) {
        if (ascending == FLAG_ALPHA) {  // 0
            buf.append(" <a href=\"").append(urlBase).append("?sort=0" +
                       "#udpcon\" title=\"").append(descr).append("\"><img src=\"/themes/console/images/inbound.png\" alt=\"V\"></a>");
        } else if (sortFlags == ascending) {
            buf.append(" <a href=\"").append(urlBase).append("?sort=").append(0-ascending);
            buf.append("#udpcon\" title=\"").append(descr).append("\"><img src=\"/themes/console/images/inbound.png\" alt=\"V\"></a>" +
                       "<b><img src=\"/themes/console/images/outbound.png\" alt=\"^\"></b>");
        } else if (sortFlags == 0 - ascending) {
            buf.append(" <b><img src=\"/themes/console/images/inbound.png\" alt=\"V\"></b><a href=\"").append(urlBase).append("?sort=").append(ascending);
            buf.append("#udpcon\" title=\"").append(descr).append("\"><img src=\"/themes/console/images/outbound.png\" alt=\"^\"></a>");
        } else {
            buf.append(" <a href=\"").append(urlBase).append("?sort=").append(0-ascending);
            buf.append("#udpcon\" title=\"").append(descr).append("\"><img src=\"/themes/console/images/inbound.png\" alt=\"V\"></a>" +
                       "<a href=\"").append(urlBase).append("?sort=").append(ascending);
            buf.append("#udpcon\" title=\"").append(descr).append("\"><img src=\"/themes/console/images/outbound.png\" alt=\"^\"></a>");
        }
    }
    
    @Override
    public void renderStatusHTML(Writer out, String urlBase, int sortFlags) throws IOException {
        TreeSet<PeerState> peers = new TreeSet(getComparator(sortFlags));
        peers.addAll(_peersByIdent.values());
        long offsetTotal = 0;

        int bpsIn = 0;
        int bpsOut = 0;
        long uptimeMsTotal = 0;
        long cwinTotal = 0;
        long rttTotal = 0;
        long rtoTotal = 0;
        long sendTotal = 0;
        long recvTotal = 0;
        long resentTotal = 0;
        long dupRecvTotal = 0;
        int numPeers = 0;
        
        StringBuilder buf = new StringBuilder(512);
        buf.append("<h3 id=\"udpcon\">").append(_("UDP connections")).append(": ").append(peers.size());
        buf.append(". ").append(_("Limit")).append(": ").append(getMaxConnections());
        buf.append(". ").append(_("Timeout")).append(": ").append(DataHelper.formatDuration2(_expireTimeout));
        buf.append(".</h3>\n");
        buf.append("<table>\n");
        buf.append("<tr><th class=\"smallhead\" nowrap><a href=\"#def.peer\">").append(_("Peer")).append("</a><br>");
        if (sortFlags != FLAG_ALPHA)
            appendSortLinks(buf, urlBase, sortFlags, _("Sort by peer hash"), FLAG_ALPHA);
        buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.dir\" title=\"")
           .append(_("Direction/Introduction")).append("\">").append(_("Dir"))
           .append("</a></th><th class=\"smallhead\" nowrap><a href=\"#def.idle\">").append(_("Idle")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by idle inbound"), FLAG_IDLE_IN);
        buf.append(" / ");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by idle outbound"), FLAG_IDLE_OUT);
        buf.append("</th>");
        buf.append("<th class=\"smallhead\" nowrap><a href=\"#def.rate\">").append(_("In/Out")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by inbound rate"), FLAG_RATE_IN);
        buf.append(" / ");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by outbound rate"), FLAG_RATE_OUT);
        buf.append("</th>\n");
        buf.append("<th class=\"smallhead\" nowrap><a href=\"#def.up\">").append(_("Up")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by connection uptime"), FLAG_UPTIME);
        buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.skew\">").append(_("Skew")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by clock skew"), FLAG_SKEW);
        buf.append("</th>\n");
        buf.append("<th class=\"smallhead\" nowrap><a href=\"#def.cwnd\">CWND</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by congestion window"), FLAG_CWND);
        buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.ssthresh\">SST</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by slow start threshold"), FLAG_SSTHRESH);
        buf.append("</th>\n");
        buf.append("<th class=\"smallhead\" nowrap><a href=\"#def.rtt\">RTT</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by round trip time"), FLAG_RTT);
        //buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.dev\">").append(_("Dev")).append("</a><br>");
        //appendSortLinks(buf, urlBase, sortFlags, _("Sort by round trip time deviation"), FLAG_DEV);
        buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.rto\">RTO</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by retransmission timeout"), FLAG_RTO);
        buf.append("</th>\n");
        buf.append("<th class=\"smallhead\" nowrap><a href=\"#def.mtu\">MTU</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by outbound maximum transmit unit"), FLAG_MTU);
        buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.send\">").append(_("TX")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by packets sent"), FLAG_SEND);
        buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.recv\">").append(_("RX")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by packets received"), FLAG_RECV);
        buf.append("</th>\n");
        buf.append("<th class=\"smallhead\" nowrap><a href=\"#def.resent\">").append(_("Dup TX")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by packets retransmitted"), FLAG_RESEND);
        buf.append("</th><th class=\"smallhead\" nowrap><a href=\"#def.dupRecv\">").append(_("Dup RX")).append("</a><br>");
        appendSortLinks(buf, urlBase, sortFlags, _("Sort by packets received more than once"), FLAG_DUP);
        buf.append("</th></tr>\n");
        out.write(buf.toString());
        buf.setLength(0);
        long now = _context.clock().now();
        for (Iterator iter = peers.iterator(); iter.hasNext(); ) {
            PeerState peer = (PeerState)iter.next();
            if (now-peer.getLastReceiveTime() > 60*60*1000)
                continue; // don't include old peers
            
            buf.append("<tr><td class=\"cells\" align=\"left\" nowrap>");
            buf.append(_context.commSystem().renderPeerHTML(peer.getRemotePeer()));
            //byte ip[] = peer.getRemoteIP();
            //if (ip != null)
            //    buf.append(' ').append(_context.blocklist().toStr(ip));
            buf.append("</td><td class=\"cells\" nowrap align=\"left\">");
            if (peer.isInbound())
                buf.append("<img src=\"/themes/console/images/inbound.png\" alt=\"Inbound\" title=\"").append(_("Inbound")).append("\">");
            else
                buf.append("<img src=\"/themes/console/images/outbound.png\" alt=\"Outbound\" title=\"").append(_("Outbound")).append("\">");
            if (peer.getWeRelayToThemAs() > 0)
                buf.append("&nbsp;&nbsp;<img src=\"/themes/console/images/outbound.png\" height=\"8\" width=\"12\" alt=\"^\" title=\"").append(_("We offered to introduce them")).append("\">");
            if (peer.getTheyRelayToUsAs() > 0)
                buf.append("&nbsp;&nbsp;<img src=\"/themes/console/images/inbound.png\" height=\"8\" width=\"12\" alt=\"V\" title=\"").append(_("They offered to introduce us")).append("\">");
            
            boolean appended = false;
            if (_activeThrottle.isChoked(peer.getRemotePeer())) {
                buf.append("<br><i>").append(_("Choked")).append("</i>");
                appended = true;
            }
            int cfs = peer.getConsecutiveFailedSends();
            if (cfs > 0) {
                if (!appended) buf.append("<br>");
                buf.append(" <i>");
                if (cfs == 1)
                    buf.append(_("1 fail"));
                else
                    buf.append(_("{0} fails", cfs));
                buf.append("</i>");
                appended = true;
            }
            if (_context.banlist().isBanlisted(peer.getRemotePeer(), STYLE)) {
                if (!appended) buf.append("<br>");
                buf.append(" <i>").append(_("Banned")).append("</i>");
                appended = true;
            }
            //byte[] ip = getIP(peer.getRemotePeer());
            //if (ip != null)
            //    buf.append(' ').append(_context.blocklist().toStr(ip));
            buf.append("</td>");
            
            long idleIn = Math.max(now-peer.getLastReceiveTime(), 0);
            long idleOut = Math.max(now-peer.getLastSendTime(), 0);
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(DataHelper.formatDuration2(idleIn));
            buf.append(THINSP);
            buf.append(DataHelper.formatDuration2(idleOut));
            buf.append("</td>");
 
            int recvBps = (idleIn > 15*1000 ? 0 : peer.getReceiveBps());
            int sendBps = (idleOut > 15*1000 ? 0 : peer.getSendBps());
            
            buf.append("<td class=\"cells\" align=\"right\" nowrap>");
            buf.append(formatKBps(recvBps));
            buf.append(THINSP);
            buf.append(formatKBps(sendBps));
            //buf.append(" K/s");
            //buf.append(formatKBps(peer.getReceiveACKBps()));
            //buf.append("K/s/");
            //buf.append(formatKBps(peer.getSendACKBps()));
            //buf.append("K/s ");
            buf.append("</td>");

            long uptime = now - peer.getKeyEstablishedTime();
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(DataHelper.formatDuration2(uptime));
            buf.append("</td>");
            
            buf.append("<td class=\"cells\" align=\"right\">");
            long skew = peer.getClockSkew();
            buf.append(DataHelper.formatDuration2(skew));
            buf.append("</td>");
            offsetTotal = offsetTotal + skew;

            long sendWindow = peer.getSendWindowBytes();
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(sendWindow/1024);
            buf.append("K");
            buf.append(THINSP).append(peer.getConcurrentSends());
            buf.append(THINSP).append(peer.getConcurrentSendWindow());
            buf.append(THINSP).append(peer.getConsecutiveSendRejections());
            if (peer.isBacklogged())
                buf.append(' ').append(_("backlogged"));
            buf.append("</td>");

            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(peer.getSlowStartThreshold()/1024);
            buf.append("K</td>");

            int rtt = peer.getRTT();
            int rto = peer.getRTO();
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(DataHelper.formatDuration2(rtt));
            buf.append("</td>");
            
            //buf.append("<td class=\"cells\" align=\"right\">");
            //buf.append(DataHelper.formatDuration2(peer.getRTTDeviation()));
            //buf.append("</td>");

            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(DataHelper.formatDuration2(rto));
            buf.append("</td>");
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(peer.getMTU()).append(THINSP).append(peer.getReceiveMTU());
            
            //.append('/');
            //buf.append(peer.getMTUIncreases()).append('/');
            //buf.append(peer.getMTUDecreases());
            buf.append("</td>");
        
            long sent = peer.getPacketsTransmitted();
            long recv = peer.getPacketsReceived();
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(sent);
            buf.append("</td>");
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(recv);
            buf.append("</td>");
            
            //double sent = (double)peer.getPacketsPeriodTransmitted();
            //double sendLostPct = 0;
            //if (sent > 0)
            //    sendLostPct = (double)peer.getPacketsRetransmitted()/(sent);
            
            long resent = peer.getPacketsRetransmitted();
            long dupRecv = peer.getPacketsReceivedDuplicate();
            
            buf.append("<td class=\"cells\" align=\"right\">");
            //buf.append(formatPct(sendLostPct));
            buf.append(resent); // + "/" + peer.getPacketsPeriodRetransmitted() + "/" + sent);
            //buf.append(peer.getPacketRetransmissionRate());
            buf.append("</td>");
            
            buf.append("<td class=\"cells\" align=\"right\">");
            buf.append(dupRecv); //formatPct(recvDupPct));
            buf.append("</td>");

            buf.append("</tr>\n");
            out.write(buf.toString());
            buf.setLength(0);
            
            bpsIn += recvBps;
            bpsOut += sendBps;
        
            uptimeMsTotal += uptime;
            cwinTotal += sendWindow;
            rttTotal += rtt;
            rtoTotal += rto;
        
            sendTotal += sent;
            recvTotal += recv;
            resentTotal += resent;
            dupRecvTotal += dupRecv;
            
            numPeers++;
        }
        
//        buf.append("<tr><td colspan=\"16\"><hr></td></tr>\n");
        buf.append("<tr class=\"tablefooter\"><td colspan=\"3\" align=\"left\"><b>").append(_("SUMMARY")).append("</b></td>" +
                   "<td align=\"center\" nowrap><b>");
        buf.append(formatKBps(bpsIn)).append(THINSP).append(formatKBps(bpsOut));
        long x = numPeers > 0 ? uptimeMsTotal/numPeers : 0;
        buf.append("</b></td>" +
                   "<td align=\"center\"><b>").append(DataHelper.formatDuration2(x));
        x = numPeers > 0 ? offsetTotal/numPeers : 0;
        buf.append("</b></td><td align=\"center\"><b>").append(DataHelper.formatDuration2(x)).append("</b></td>\n" +
                   "<td align=\"center\"><b>");
        buf.append(numPeers > 0 ? cwinTotal/(numPeers*1024) + "K" : "0K");
        buf.append("</b></td><td>&nbsp;</td>\n" +
                   "<td align=\"center\"><b>");
        buf.append(numPeers > 0 ? DataHelper.formatDuration2(rttTotal/numPeers) : '0');
        //buf.append("</b></td><td>&nbsp;</td><td align=\"center\"><b>");
        buf.append("</b></td><td align=\"center\"><b>");
        buf.append(numPeers > 0 ? DataHelper.formatDuration2(rtoTotal/numPeers) : '0');
        buf.append("</b></td><td align=\"center\"><b>").append(_mtu).append("</b></td><td align=\"center\"><b>");
        buf.append(sendTotal).append("</b></td><td align=\"center\"><b>").append(recvTotal).append("</b></td>\n" +
                   "<td align=\"center\"><b>").append(resentTotal);
        buf.append("</b></td><td align=\"center\"><b>").append(dupRecvTotal).append("</b></td></tr>\n");
        if (sortFlags == FLAG_DEBUG) {
            buf.append("<tr><td colspan=\"16\">");
            buf.append("peersByIdent: ").append(_peersByIdent.size());
            buf.append(" peersByRemoteHost: ").append(_peersByRemoteHost.size());
            int dir = 0;
            int indir = 0;
            for (RemoteHostId rhi : _peersByRemoteHost.keySet()) {
                 if (rhi.getIP() != null)
                     dir++;
                 else
                     indir++;
            }
            buf.append(" pBRH direct: ").append(dir).append(" indirect: ").append(indir);
            buf.append("</td></tr>");
        }
        buf.append("</table>\n");

      /*****
        long bytesTransmitted = _context.bandwidthLimiter().getTotalAllocatedOutboundBytes();
        // NPE here early
        double averagePacketSize = _context.statManager().getRate("udp.sendPacketSize").getLifetimeAverageValue();
        // lifetime value, not just the retransmitted packets of current connections
        resentTotal = (long)_context.statManager().getRate("udp.packetsRetransmitted").getLifetimeEventCount();
        double nondupSent = ((double)bytesTransmitted - ((double)resentTotal)*averagePacketSize);
        double bwResent = (nondupSent <= 0 ? 0d : ((((double)resentTotal)*averagePacketSize) / nondupSent));
        buf.append("<h3>Percentage of bytes retransmitted (lifetime): ").append(formatPct(bwResent));
        buf.append("</h3><i>(Includes retransmission required by packet loss)</i>\n");
      *****/

        out.write(buf.toString());
        buf.setLength(0);
    }

    private static final DecimalFormat _fmt = new DecimalFormat("#,##0.00");
    private static final String formatKBps(int bps) {
        synchronized (_fmt) {
            return _fmt.format((float)bps/1024);
        }
    }
    private static final DecimalFormat _pctFmt = new DecimalFormat("#0.0%");
    private static final String BUNDLE_NAME = "net.i2p.router.web.messages";

    /**
     *  Translate
     */
    private final String _(String s) {
        return Translate.getString(s, _context, BUNDLE_NAME);
    }

    /**
     *  Translate
     */
    private final String _(String s, Object o) {
        return Translate.getString(s, o, _context, BUNDLE_NAME);
    }

    /*
     * Cache the bid to reduce object churn
     */
    private class SharedBid extends TransportBid {
        public SharedBid(int ms) { super(); setLatencyMs(ms); }
        @Override
        public Transport getTransport() { return UDPTransport.this; }
        @Override
        public String toString() { return "UDP bid @ " + getLatencyMs(); }
    }
    
    private class ExpirePeerEvent extends SimpleTimer2.TimedEvent {
        // TODO why have separate Set, just use _peersByIdent.values()
        private final Set<PeerState> _expirePeers;
        private final List<PeerState> _expireBuffer;
        private volatile boolean _alive;
        private int _runCount;
        // we've seen firewalls change ports after 40 seconds
        private static final long PING_FIREWALL_TIME = 30*1000;
        private static final long PING_FIREWALL_CUTOFF = PING_FIREWALL_TIME / 2;
        // ping 1/4 of the peers every loop
        private static final int SLICES = 4;
        private static final long SHORT_LOOP_TIME = PING_FIREWALL_CUTOFF / (SLICES + 1);
        private static final long LONG_LOOP_TIME = 25*1000;

        public ExpirePeerEvent() {
            super(_context.simpleTimer2());
            _expirePeers = new ConcurrentHashSet(128);
            _expireBuffer = new ArrayList();
        }

        public void timeReached() {
            // Increase allowed idle time if we are well under allowed connections, otherwise decrease
            if (haveCapacity())
                _expireTimeout = Math.min(_expireTimeout + 15*1000, EXPIRE_TIMEOUT);
            else
                _expireTimeout = Math.max(_expireTimeout - 45*1000, MIN_EXPIRE_TIMEOUT);
            long now = _context.clock().now();
            long shortInactivityCutoff = now - _expireTimeout;
            long longInactivityCutoff = now - EXPIRE_TIMEOUT;
            long pingCutoff = now - (2 * 60*60*1000);
            long pingFirewallCutoff = now - PING_FIREWALL_CUTOFF;
            boolean shouldPingFirewall = _reachabilityStatus != CommSystemFacade.STATUS_OK;
            boolean pingOneOnly = shouldPingFirewall && _externalListenPort == _endpoint.getListenPort();
            boolean shortLoop = shouldPingFirewall;
            _expireBuffer.clear();
            _runCount++;

                for (Iterator<PeerState> iter = _expirePeers.iterator(); iter.hasNext(); ) {
                    PeerState peer = iter.next();
                    long inactivityCutoff;
                    // if we offered to introduce them, or we used them as introducer in last 2 hours
                    if (peer.getWeRelayToThemAs() > 0 || peer.getIntroducerTime() > pingCutoff)
                        inactivityCutoff = longInactivityCutoff;
                    else
                        inactivityCutoff = shortInactivityCutoff;
                    if ( (peer.getLastReceiveTime() < inactivityCutoff) && (peer.getLastSendTime() < inactivityCutoff) ) {
                        _expireBuffer.add(peer);
                        iter.remove();
                    } else if (shouldPingFirewall &&
                               ((_runCount ^ peer.hashCode()) & (SLICES - 1)) == 0 &&
                               peer.getLastSendOrPingTime() < pingFirewallCutoff &&
                               peer.getLastReceiveTime() < pingFirewallCutoff) {
                        // ping if firewall is mapping the port to keep port the same...
                        // if the port changes we are screwed
                        if (_log.shouldLog(Log.DEBUG))
                            _log.debug("Pinging for firewall: " + peer);
                        // don't update or idle time won't be right and peer won't get dropped
                        // TODO if both sides are firewalled should only one ping
                        // or else session will stay open forever?
                        //peer.setLastSendTime(now);
                        send(_destroyBuilder.buildPing(peer));
                        peer.setLastPingTime(now);
                        // If external port is different, it may be changing the port for every
                        // session, so ping all of them. Otherwise only one.
                        if (pingOneOnly)
                            shouldPingFirewall = false;
		    }
                }

            for (PeerState peer : _expireBuffer) {
                sendDestroy(peer);
                dropPeer(peer, false, "idle too long");
            }
            _expireBuffer.clear();

            if (_alive)
                schedule(shortLoop ? SHORT_LOOP_TIME : LONG_LOOP_TIME);
        }

        public void add(PeerState peer) {
                _expirePeers.add(peer);
        }

        public void remove(PeerState peer) {
                _expirePeers.remove(peer);
        }

        public void setIsAlive(boolean isAlive) {
            _alive = isAlive;
            if (isAlive) {
                reschedule(LONG_LOOP_TIME);
            } else {
                cancel();
                _expirePeers.clear();
            }
        }
    }
    
    void setReachabilityStatus(short status) { 
        short old = _reachabilityStatus;
        long now = _context.clock().now();
        switch (status) {
            case CommSystemFacade.STATUS_OK:
                // TODO if OK but internal port != external port, should we have
                // a different status state? ...as we don't know if the TCP
                // port will be mapped the same way or not...
                // Right now, we assume it is and hope for the best for TCP.
                _context.statManager().addRateData("udp.statusOK", 1);
                _reachabilityStatus = status; 
                _reachabilityStatusLastUpdated = now;
                break;
            case CommSystemFacade.STATUS_DIFFERENT:
                _context.statManager().addRateData("udp.statusDifferent", 1);
                _reachabilityStatus = status; 
                _reachabilityStatusLastUpdated = now;
                break;
            case CommSystemFacade.STATUS_REJECT_UNSOLICITED:
                _context.statManager().addRateData("udp.statusReject", 1);
// if old != unsolicited && now - lastUpdated > STATUS_GRACE_PERIOD)
//
                // fall through...
            case CommSystemFacade.STATUS_DISCONNECTED:
            case CommSystemFacade.STATUS_HOSED:
                _reachabilityStatus = status; 
                _reachabilityStatusLastUpdated = now;
                break;
            case CommSystemFacade.STATUS_UNKNOWN:
            default:
                _context.statManager().addRateData("udp.statusUnknown", 1);
                //if (now - _reachabilityStatusLastUpdated < STATUS_GRACE_PERIOD) {
                //    _testEvent.forceRun();
                //    SimpleTimer.getInstance().addEvent(_testEvent, 5*1000);
                //} else {
                //    _reachabilityStatus = status;
                //    _reachabilityStatusLastUpdated = now;
                //}
                break;
        }
        if ( (status != old) && (status != CommSystemFacade.STATUS_UNKNOWN) ) {
            if (_log.shouldLog(Log.INFO))
                _log.info("Old status: " + old + " New status: " + status + " from: ", new Exception("traceback"));
            // Always rebuild when the status changes, even if our address hasn't changed,
            // as rebuildExternalAddress() calls replaceAddress() which calls CSFI.notifyReplaceAddress()
            // which will start up NTCP inbound when we transition to OK.
            // if (needsRebuild())
                rebuildExternalAddress();
        }
    }
    private static final String PROP_REACHABILITY_STATUS_OVERRIDE = "i2np.udp.status";

    @Override
    public short getReachabilityStatus() { 
        String override = _context.getProperty(PROP_REACHABILITY_STATUS_OVERRIDE);
        if (override == null)
            return _reachabilityStatus;
            
        if ("ok".equals(override))
            return CommSystemFacade.STATUS_OK;
        else if ("err-reject".equals(override))
            return CommSystemFacade.STATUS_REJECT_UNSOLICITED;
        else if ("err-different".equals(override))
            return CommSystemFacade.STATUS_DIFFERENT;
        
        return _reachabilityStatus; 
    }
    @Override
    public void recheckReachability() {
        _testEvent.runTest();
    }
    
    PeerState pickTestPeer(RemoteHostId dontInclude) {
        List<PeerState> peers = new ArrayList(_peersByIdent.values());
        for (Iterator<PeerState> iter = new RandomIterator(peers); iter.hasNext(); ) {
            PeerState peer = iter.next();
            if ( (dontInclude != null) && (dontInclude.equals(peer.getRemoteHostId())) )
                continue;
            RouterInfo peerInfo = _context.netDb().lookupRouterInfoLocally(peer.getRemotePeer());
            if (peerInfo == null)
                continue;
            RouterAddress addr = peerInfo.getTargetAddress(STYLE);
            if (addr == null)
                continue;
            byte[] ip = addr.getIP();
            if (ip == null)
                continue;
            if (DataHelper.eq(ip, 0, getExternalIP(), 0, 2))
                continue;
            return peer;
        }
        return null;
    }
    
    private boolean shouldTest() {
        return ! _context.router().isHidden();
        //String val = _context.getProperty(PROP_SHOULD_TEST);
        //return ( (val != null) && ("true".equals(val)) );
    }
    
    private class PeerTestEvent extends SimpleTimer2.TimedEvent {
        private volatile boolean _alive;
        /** when did we last test our reachability */
        private long _lastTested;
        private boolean _forceRun;

        PeerTestEvent() {
            super(_context.simpleTimer2());
        }
        
        public void timeReached() {
            if (shouldTest()) {
                long now = _context.clock().now();
                if ( (_forceRun) || (now - _lastTested >= TEST_FREQUENCY) ) {
                    runTest();
                }
            }
            if (_alive) {
                long delay = (TEST_FREQUENCY / 2) + _context.random().nextInt(TEST_FREQUENCY);
                if (delay <= 0)
                    throw new RuntimeException("wtf, delay is " + delay);
                schedule(delay);
            }
        }
        
        private void runTest() {
            PeerState bob = pickTestPeer(null);
            if (bob != null) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Running periodic test with bob = " + bob);
                _testManager.runTest(bob.getRemoteIPAddress(), bob.getRemotePort(), bob.getCurrentCipherKey(), bob.getCurrentMACKey());
                _lastTested = _context.clock().now();
                _forceRun = false;
                return;
            }
            
            if (_log.shouldLog(Log.WARN))
                _log.warn("Unable to run a periodic test, as there are no peers with the capacity required");
            _forceRun = false;
        }
        
        void forceRun() { _forceRun = true; }
        
        public void setIsAlive(boolean isAlive) {
            _alive = isAlive;
            if (isAlive) {
                long delay = _context.random().nextInt(2*TEST_FREQUENCY);
                reschedule(delay);
            } else {
                cancel();
            }
        }
    }
    
    /**
     *  Periodically ping the introducers, split out since we need to
     *  do it faster than we rebuild our address.
     *  @since 0.8.11
     */
    private class PingIntroducers implements SimpleTimer.TimedEvent {
        public void timeReached() {
            if (introducersRequired())
                _introManager.pingIntroducers();
        }
    }

/*******
    private static final String BADIPS[] = new String[] { "192.168.0.1", "127.0.0.1", "10.3.4.5", "172.16.3.4", "224.5.6.7" };
    private static final String GOODIPS[] = new String[] { "192.167.0.1", "126.0.0.1", "11.3.4.5", "172.15.3.4", "223.5.6.7" };
    public static void main(String args[]) {
        for (int i = 0; i < BADIPS.length; i++) {
            try { 
                InetAddress addr = InetAddress.getByName(BADIPS[i]);
                boolean routable = isPubliclyRoutable(addr.getAddress());
                System.out.println("Routable: " + routable + " (" + BADIPS[i] + ")");
            } catch (Exception e) { e.printStackTrace(); }
        }
        for (int i = 0; i < GOODIPS.length; i++) {
            try { 
                InetAddress addr = InetAddress.getByName(GOODIPS[i]);
                boolean routable = isPubliclyRoutable(addr.getAddress());
                System.out.println("Routable: " + routable + " (" + GOODIPS[i] + ")");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
*******/
}

