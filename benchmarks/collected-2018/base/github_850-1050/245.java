// https://searchcode.com/api/result/70837684/

package net.i2p.router.transport.ntcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.i2p.data.DataHelper;
import net.i2p.data.Hash;
import net.i2p.data.RouterAddress;
import net.i2p.data.RouterIdentity;
import net.i2p.data.RouterInfo;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.OutNetMessage;
import net.i2p.router.RouterContext;
import net.i2p.router.transport.CommSystemFacadeImpl;
import net.i2p.router.transport.Transport;
import net.i2p.router.transport.TransportBid;
import net.i2p.router.transport.TransportImpl;
import net.i2p.router.transport.crypto.DHSessionKeyBuilder;
import net.i2p.util.Addresses;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;
import net.i2p.util.Translate;

/**
 *  The NIO TCP transport
 */
public class NTCPTransport extends TransportImpl {
    private final Log _log;
    private final SharedBid _fastBid;
    private final SharedBid _slowBid;
    private final SharedBid _slowCostBid;
    /** save some conns for inbound */
    private final SharedBid _nearCapacityBid;
    private final SharedBid _nearCapacityCostBid;
    private final SharedBid _transientFail;
    private final Object _conLock;
    private final Map<Hash, NTCPConnection> _conByIdent;
    private NTCPAddress _myAddress;
    private final EventPumper _pumper;
    private final Reader _reader;
    private net.i2p.router.transport.ntcp.Writer _writer;
    /**
     * list of NTCPConnection of connections not yet established that we
     * want to remove on establishment or close on timeout
     */
    private final Set<NTCPConnection> _establishing;

    /** this is rarely if ever used, default is to bind to wildcard address */
    public static final String PROP_BIND_INTERFACE = "i2np.ntcp.bindInterface";

    private final NTCPSendFinisher _finisher;
    private final DHSessionKeyBuilder.Factory _dhFactory;
    private long _lastBadSkew;
    private static final long[] RATES = { 10*60*1000 };

    /**
     *  To prevent trouble. 1024 as of 0.9.4.
     *
     *  @since 0.9.3
     */
    private static final int MIN_PEER_PORT = 1024;

    // Opera doesn't have the char, TODO check UA
    //private static final String THINSP = "&thinsp;/&thinsp;";
    private static final String THINSP = " / ";

    public NTCPTransport(RouterContext ctx, DHSessionKeyBuilder.Factory dh) {
        super(ctx);
        _dhFactory = dh;
        _log = ctx.logManager().getLog(getClass());

        _context.statManager().createRateStat("ntcp.sendTime", "Total message lifetime when sent completely", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.sendQueueSize", "How many messages were ahead of the current one on the connection's queue when it was first added", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.receiveTime", "How long it takes to receive an inbound message", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.receiveSize", "How large the received message was", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.sendBacklogTime", "How long the head of the send queue has been waiting when we fail to add a new one to the queue (period is the number of messages queued)", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.failsafeWrites", "How many times do we need to proactively add in an extra nio write to a peer at any given failsafe pass?", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.failsafeCloses", "How many times do we need to proactively close an idle connection to a peer at any given failsafe pass?", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.failsafeInvalid", "How many times do we close a connection to a peer to work around a JVM bug?", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.accept", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.attemptBanlistedPeer", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.attemptUnreachablePeer", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.closeOnBacklog", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.connectFailedIOE", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.connectFailedInvalidPort", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.bidRejectedLocalAddress", "", "ntcp", RATES);
        //_context.statManager().createRateStat("ntcp.bidRejectedNoNTCPAddress", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.connectFailedTimeout", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.connectFailedTimeoutIOE", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.connectFailedUnresolved", "", "ntcp", RATES);
        //_context.statManager().createRateStat("ntcp.connectImmediate", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.connectSuccessful", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.corruptDecryptedI2NP", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.corruptI2NPCRC", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.corruptI2NPIME", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.corruptI2NPIOE", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.corruptMetaCRC", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.corruptSkew", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.corruptTooLargeI2NP", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.dontSendOnBacklog", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.inboundCheckConnection", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.inboundEstablished", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.inboundEstablishedDuplicate", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.infoMessageEnqueued", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.floodInfoMessageEnqueued", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidDH", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidHXY", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidHXxorBIH", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidInboundDFE", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidInboundIOE", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidInboundSignature", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidInboundSize", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidInboundSkew", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidSignature", "", "ntcp", RATES);
        //_context.statManager().createRateStat("ntcp.liveReadBufs", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.multipleCloseOnRemove", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.outboundEstablishFailed", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.outboundFailedIOEImmediate", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.invalidOutboundSkew", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.noBidTooLargeI2NP", "send size", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.queuedRecv", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.read", "", "ntcp", RATES);
        //_context.statManager().createRateStat("ntcp.readEOF", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.readError", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.receiveCorruptEstablishment", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.receiveMeta", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.registerConnect", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.throttledReadComplete", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.throttledWriteComplete", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.wantsQueuedWrite", "", "ntcp", RATES);
        //_context.statManager().createRateStat("ntcp.write", "", "ntcp", RATES);
        _context.statManager().createRateStat("ntcp.writeError", "", "ntcp", RATES);
        _establishing = new ConcurrentHashSet(16);
        _conLock = new Object();
        _conByIdent = new ConcurrentHashMap(64);

        _finisher = new NTCPSendFinisher(ctx, this);

        _pumper = new EventPumper(ctx, this);
        _reader = new Reader(ctx);
        _writer = new net.i2p.router.transport.ntcp.Writer(ctx);

        _fastBid = new SharedBid(25); // best
        _slowBid = new SharedBid(70); // better than ssu unestablished, but not better than ssu established
        _slowCostBid = new SharedBid(85);
        _nearCapacityBid = new SharedBid(90); // not better than ssu - save our conns for inbound
        _nearCapacityCostBid = new SharedBid(105);
        _transientFail = new SharedBid(TransportBid.TRANSIENT_FAIL);
    }

    void inboundEstablished(NTCPConnection con) {
        _context.statManager().addRateData("ntcp.inboundEstablished", 1);
        markReachable(con.getRemotePeer().calculateHash(), true);
        //_context.banlist().unbanlistRouter(con.getRemotePeer().calculateHash());
        NTCPConnection old;
        synchronized (_conLock) {
            old = _conByIdent.put(con.getRemotePeer().calculateHash(), con);
        }
        if (old != null) {
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Old connection closed: " + old + " replaced by " + con);
            _context.statManager().addRateData("ntcp.inboundEstablishedDuplicate", old.getUptime());
            old.close();
        }
    }

    protected void outboundMessageReady() {
        OutNetMessage msg = getNextMessage();
        if (msg != null) {
            RouterIdentity ident = msg.getTarget().getIdentity();
            Hash ih = ident.calculateHash();
            NTCPConnection con = null;
            boolean isNew = false;
            synchronized (_conLock) {
                con = _conByIdent.get(ih);
                if (con == null) {
                    isNew = true;
                    RouterAddress addr = msg.getTarget().getTargetAddress(STYLE);
                    if (addr != null) {
                        NTCPAddress naddr = new NTCPAddress(addr);
                        con = new NTCPConnection(_context, this, ident, naddr);
                        if (_log.shouldLog(Log.DEBUG))
                            _log.debug("Send on a new con: " + con + " at " + addr + " for " + ih.toBase64());
                        _conByIdent.put(ih, con);
                    } else {
                        _log.error("we bid on a peer who doesn't have an ntcp address? " + msg.getTarget());
                        return;
                    }
                }
            }
            if (isNew) {
                con.enqueueInfoMessage(); // enqueues a netDb store of our own info
                con.send(msg); // doesn't do anything yet, just enqueues it

                try {
                    SocketChannel channel = SocketChannel.open();
                    con.setChannel(channel);
                    channel.configureBlocking(false);
                    _pumper.registerConnect(con);
                } catch (IOException ioe) {
                    if (_log.shouldLog(Log.ERROR))
                        _log.error("Error opening a channel", ioe);
                    _context.statManager().addRateData("ntcp.outboundFailedIOEImmediate", 1);
                    con.close();
                }
            } else {
                con.send(msg);
            }
            /*
            NTCPConnection con = getCon(ident);
            remove the race here
            if (con != null) {
                //if (_log.shouldLog(Log.DEBUG))
                //    _log.debug("Send on an existing con: " + con);
                con.send(msg);
            } else {
                RouterAddress addr = msg.getTarget().getTargetAddress(STYLE);
                if (addr != null) {
                    NTCPAddress naddr = new NTCPAddress(addr);
                    con = new NTCPConnection(_context, this, ident, naddr);
                    Hash ih = ident.calculateHash();
                    if (_log.shouldLog(Log.DEBUG))
                        _log.debug("Send on a new con: " + con + " at " + addr + " for " + ih.toBase64());
                    NTCPConnection old = null;
                    synchronized (_conLock) {
                        old = (NTCPConnection)_conByIdent.put(ih, con);
                    }
                    if (old != null) {
                        if (_log.shouldLog(Log.WARN))
                            _log.warn("Multiple connections on out ready, closing " + old + " and keeping " + con);
                        old.close();
                    }
                    con.enqueueInfoMessage(); // enqueues a netDb store of our own info
                    con.send(msg); // doesn't do anything yet, just enqueues it

                    try {
                        SocketChannel channel = SocketChannel.open();
                        con.setChannel(channel);
                        channel.configureBlocking(false);
                        _pumper.registerConnect(con);
                    } catch (IOException ioe) {
                        if (_log.shouldLog(Log.ERROR))
                            _log.error("Error opening a channel", ioe);
                        con.close();
                    }
                } else {
                    con.close();
                }
            }
             */
        }
    }

    @Override
    public void afterSend(OutNetMessage msg, boolean sendSuccessful, boolean allowRequeue, long msToSend) {
        super.afterSend(msg, sendSuccessful, allowRequeue, msToSend);
    }

    public TransportBid bid(RouterInfo toAddress, long dataSize) {
        if (!isAlive())
            return null;
        if (dataSize > NTCPConnection.MAX_MSG_SIZE) {
            // let SSU deal with it
            _context.statManager().addRateData("ntcp.noBidTooLargeI2NP", dataSize);
            return null;
        }
        Hash peer = toAddress.getIdentity().calculateHash();
        if (_context.banlist().isBanlisted(peer, STYLE)) {
            // we aren't banlisted in general (since we are trying to get a bid), but we have
            // recently banlisted the peer on the NTCP transport, so don't try it
            _context.statManager().addRateData("ntcp.attemptBanlistedPeer", 1);
            return null;
        } else if (isUnreachable(peer)) {
            _context.statManager().addRateData("ntcp.attemptUnreachablePeer", 1);
            return null;
        }

        boolean established = isEstablished(toAddress.getIdentity());
        if (established) { // should we check the queue size?  nah, if its valid, use it
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("fast bid when trying to send to " + peer + " as its already established");
            return _fastBid;
        }
        RouterAddress addr = toAddress.getTargetAddress(STYLE);

        if (addr == null) {
            markUnreachable(peer);
            //_context.statManager().addRateData("ntcp.bidRejectedNoNTCPAddress", 1);
            //_context.banlist().banlistRouter(toAddress.getIdentity().calculateHash(), "No NTCP address", STYLE);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("no bid when trying to send to " + peer + " as they don't have an ntcp address");
            return null;
        }
        byte[] ip = addr.getIP();
        if ( (addr.getPort() < MIN_PEER_PORT) || (ip == null) ) {
            _context.statManager().addRateData("ntcp.connectFailedInvalidPort", 1);
            markUnreachable(peer);
            //_context.banlist().banlistRouter(toAddress.getIdentity().calculateHash(), "Invalid NTCP address", STYLE);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("no bid when trying to send to " + peer + " as they don't have a valid ntcp address");
            return null;
        }
        if (!isPubliclyRoutable(ip)) {
            if (! _context.getProperty("i2np.ntcp.allowLocal", "false").equals("true")) {
                _context.statManager().addRateData("ntcp.bidRejectedLocalAddress", 1);
                markUnreachable(peer);
                if (_log.shouldLog(Log.DEBUG))
                    _log.debug("no bid when trying to send to " + peer + " as they have a private ntcp address");
                return null;
            }
        }

        if (!allowConnection()) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("no bid when trying to send to " + peer + ", max connection limit reached");
            return _transientFail;
        }

        //if ( (_myAddress != null) && (_myAddress.equals(addr)) )
        //    return null; // dont talk to yourself

        if (_log.shouldLog(Log.DEBUG))
            _log.debug("slow bid when trying to send to " + peer);
        if (haveCapacity()) {
            if (addr.getCost() > NTCPAddress.DEFAULT_COST)
                return _slowCostBid;
            else
                return _slowBid;
        } else {
            if (addr.getCost() > NTCPAddress.DEFAULT_COST)
                return _nearCapacityCostBid;
            else
                return _nearCapacityBid;
        }
    }

    public boolean allowConnection() {
        return countActivePeers() < getMaxConnections();
    }

    /** queue up afterSend call, which can take some time w/ jobs, etc */
    void sendComplete(OutNetMessage msg) { _finisher.add(msg); }

    private boolean isEstablished(RouterIdentity peer) {
        return isEstablished(peer.calculateHash());
    }

    @Override
    public boolean isEstablished(Hash dest) {
            NTCPConnection con = _conByIdent.get(dest);
            return (con != null) && con.isEstablished() && !con.isClosed();
    }

    @Override
    public boolean isBacklogged(Hash dest) {
            NTCPConnection con = _conByIdent.get(dest);
            return (con != null) && con.isEstablished() && con.tooBacklogged();
    }

    void removeCon(NTCPConnection con) {
        NTCPConnection removed = null;
        RouterIdentity ident = con.getRemotePeer();
        if (ident != null) {
            synchronized (_conLock) {
                removed = _conByIdent.remove(ident.calculateHash());
            }
        }
        if ( (removed != null) && (removed != con) ) {// multiple cons, close 'em both
            if (_log.shouldLog(Log.WARN))
                _log.warn("Multiple connections on remove, closing " + removed + " (already closed " + con + ")");
            _context.statManager().addRateData("ntcp.multipleCloseOnRemove", removed.getUptime());
            removed.close();
        }
    }

    /**
     * How many peers can we talk to right now?
     *
     */
    @Override
    public int countActivePeers() { return _conByIdent.size(); }

    /**
     * How many peers are we actively sending messages to (this minute)
     */
    @Override
    public int countActiveSendPeers() {
        int active = 0;
        for (NTCPConnection con : _conByIdent.values()) {
                if ( (con.getTimeSinceSend() <= 60*1000) || (con.getTimeSinceReceive() <= 60*1000) )
                    active++;
        }
        return active;
    }

    /** @param skew in seconds */
    void setLastBadSkew(long skew) {
        _lastBadSkew = skew;
    }

    /**
     * Return our peer clock skews on this transport.
     * Vector composed of Long, each element representing a peer skew in seconds.
     */
    @Override
    public Vector<Long> getClockSkews() {
        Vector<Long> skews = new Vector();

        for (NTCPConnection con : _conByIdent.values()) {
            if (con.isEstablished())
                skews.addElement(Long.valueOf(con.getClockSkew()));
        }

        // If we don't have many peers, maybe it is because of a bad clock, so
        // return the last bad skew we got
        if (skews.size() < 5 && _lastBadSkew != 0)
            skews.addElement(Long.valueOf(_lastBadSkew));

        if (_log.shouldLog(Log.DEBUG))
            _log.debug("NTCP transport returning " + skews.size() + " peer clock skews.");
        return skews;
    }

    private static final int MIN_CONCURRENT_READERS = 2;  // unless < 32MB
    private static final int MIN_CONCURRENT_WRITERS = 2;  // unless < 32MB
    private static final int MAX_CONCURRENT_READERS = 4;
    private static final int MAX_CONCURRENT_WRITERS = 4;

    /**
     *  Called by TransportManager.
     *  Caller should stop the transport first, then
     *  verify stopped with isAlive()
     *  Unfortunately TransportManager doesn't do that, so we
     *  check here to prevent two pumpers.
     *  @return appears to be ignored by caller
     */
    public synchronized RouterAddress startListening() {
        // try once again to prevent two pumpers which is fatal
        if (_pumper.isAlive())
            return _myAddress != null ? _myAddress.toRouterAddress() : null;
        if (_log.shouldLog(Log.WARN)) _log.warn("Starting ntcp transport listening");

        startIt();
        configureLocalAddress();
        return bindAddress();
    }

    /**
     *  Only called by CSFI.
     *  Caller should stop the transport first, then
     *  verify stopped with isAlive()
     *  @return appears to be ignored by caller
     */
    public synchronized RouterAddress restartListening(RouterAddress addr) {
        // try once again to prevent two pumpers which is fatal
        // we could just return null since the return value is ignored
        if (_pumper.isAlive())
            return _myAddress != null ? _myAddress.toRouterAddress() : null;
        if (_log.shouldLog(Log.WARN)) _log.warn("Restarting ntcp transport listening");

        startIt();
        if (addr == null)
            _myAddress = null;
        else
            _myAddress = new NTCPAddress(addr);
        return bindAddress();
    }

    /**
     *  Start up. Caller must synchronize.
     *  @since 0.8.3
     */
    private void startIt() {
        _finisher.start();
        _pumper.startPumping();

        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory == Long.MAX_VALUE)
            maxMemory = 128*1024*1024l;
        int nr, nw;
        if (maxMemory < 32*1024*1024) {
            nr = nw = 1;
        } else if (maxMemory < 64*1024*1024) {
            nr = nw = 2;
        } else {
            nr = Math.max(MIN_CONCURRENT_READERS, Math.min(MAX_CONCURRENT_READERS, _context.bandwidthLimiter().getInboundKBytesPerSecond() / 20));
            nw = Math.max(MIN_CONCURRENT_WRITERS, Math.min(MAX_CONCURRENT_WRITERS, _context.bandwidthLimiter().getOutboundKBytesPerSecond() / 20));
        }
        _reader.startReading(nr);
        _writer.startWriting(nw);
    }

    public boolean isAlive() {
        return _pumper.isAlive();
    }

    /** call from synchronized method */
    private RouterAddress bindAddress() {
        if (_myAddress != null) {
            InetAddress bindToAddr = null;
            String bindTo = _context.getProperty(PROP_BIND_INTERFACE);

            if (bindTo == null) {
                // If we are configured with a fixed IP address,
                // AND it's one of our local interfaces,
                // bind only to that.
                boolean isFixed = _context.getProperty(CommSystemFacadeImpl.PROP_I2NP_NTCP_AUTO_IP, "true")
                                  .toLowerCase(Locale.US).equals("false");
                String fixedHost = _context.getProperty(CommSystemFacadeImpl.PROP_I2NP_NTCP_HOSTNAME);
                if (isFixed && fixedHost != null) {
                    try {
                        String testAddr = InetAddress.getByName(fixedHost).getHostAddress();
                        if (Addresses.getAddresses().contains(testAddr))
                            bindTo = testAddr;
                    } catch (UnknownHostException uhe) {}
                }
            }

            if (bindTo != null) {
                try {
                    bindToAddr = InetAddress.getByName(bindTo);
                } catch (UnknownHostException uhe) {
                    _log.log(Log.CRIT, "Invalid NTCP bind interface specified [" + bindTo + "]", uhe);
                    // this can be implemented later, just updates some stats
                    // see udp/UDPTransport.java
                    //setReachabilityStatus(CommSystemFacade.STATUS_HOSED);
                    return null;
                }
            }

            try {
                ServerSocketChannel chan = ServerSocketChannel.open();
                chan.configureBlocking(false);

                int port = _myAddress.getPort();
                if (port > 0 && port < 1024)
                    _log.logAlways(Log.WARN, "Specified NTCP port is " + port + ", ports lower than 1024 not recommended");
                InetSocketAddress addr = null;
                if(bindToAddr==null) {
                    addr = new InetSocketAddress(port);
                } else {
                    addr = new InetSocketAddress(bindToAddr, port);
                    if (_log.shouldLog(Log.WARN))
                        _log.warn("Binding only to " + bindToAddr);
                }
                chan.socket().bind(addr);
                if (_log.shouldLog(Log.INFO))
                    _log.info("Listening on " + addr);
                _pumper.register(chan);
            } catch (IOException ioe) {
                _log.error("Error listening", ioe);
            }
        } else {
            if (_log.shouldLog(Log.INFO))
                _log.info("Outbound NTCP connections only - no listener configured");
        }

        if (_myAddress != null) {
            RouterAddress rv = _myAddress.toRouterAddress();
            if (rv != null)
                replaceAddress(rv);
            return rv;
        } else {
            return null;
        }
    }

    Reader getReader() { return _reader; }
    net.i2p.router.transport.ntcp.Writer getWriter() { return _writer; }
    public String getStyle() { return STYLE; }
    EventPumper getPumper() { return _pumper; }

    /**
     *  @since 0.9
     */
    DHSessionKeyBuilder getDHBuilder() {
        return _dhFactory.getBuilder();
    }

    /**
     * how long from initial connection attempt (accept() or connect()) until
     * the con must be established to avoid premature close()ing
     */
    public static final int ESTABLISH_TIMEOUT = 10*1000;

    /** add us to the establishment timeout process */
    void establishing(NTCPConnection con) {
            _establishing.add(con);
    }

    /**
     * called in the EventPumper no more than once a second or so, closing
     * any unconnected/unestablished connections
     */
    void expireTimedOut() {
        int expired = 0;

            for (Iterator<NTCPConnection> iter = _establishing.iterator(); iter.hasNext(); ) {
                NTCPConnection con = iter.next();
                if (con.isClosed() || con.isEstablished()) {
                    iter.remove();
                } else if (con.getTimeSinceCreated() > ESTABLISH_TIMEOUT) {
                    iter.remove();
                    con.close();
                    expired++;
                }
            }

        if (expired > 0)
            _context.statManager().addRateData("ntcp.outboundEstablishFailed", expired);
    }

    //private boolean bindAllInterfaces() { return true; }

    private void configureLocalAddress() {
        RouterContext ctx = getContext();
        if (ctx == null) {
            System.err.println("NIO transport has no context?");
        } else {
            // this generally returns null -- see javadoc
            RouterAddress ra = CommSystemFacadeImpl.createNTCPAddress(ctx);
            if (ra != null) {
                NTCPAddress addr = new NTCPAddress(ra);
                if (addr.getPort() <= 0) {
                    _myAddress = null;
                    if (_log.shouldLog(Log.ERROR))
                        _log.error("NTCP address is outbound only, since the NTCP configuration is invalid");
                } else {
                    _myAddress = addr;
                    replaceAddress(ra);
                    if (_log.shouldLog(Log.INFO))
                        _log.info("NTCP address configured: " + _myAddress);
                }
            } else {
                if (_log.shouldLog(Log.INFO))
                    _log.info("NTCP address is outbound only");
            }
        }
    }

    /**
     *  If we didn't used to be forwarded, and we have an address,
     *  and we are configured to use UPnP, update our RouterAddress
     *
     *  Don't do anything now. If it fails, we don't know if it's
     *  because there is no firewall, or if the firewall rejected the request.
     *  So we just use the SSU reachability status
     *  to decide whether to enable inbound NTCP. SSU will have CSFI build a new
     *  NTCP address when it transitions to OK.
     */
    @Override
    public void forwardPortStatus(int port, int externalPort, boolean success, String reason) {
        if (_log.shouldLog(Log.WARN)) {
            if (success)
                _log.warn("UPnP has opened the NTCP port: " + port);
            else
                _log.warn("UPnP has failed to open the NTCP port: " + port + " reason: " + reason);
        }
    }

    @Override
    public int getRequestedPort() {
        // would be nice to do this here but we can't easily get to the UDP transport.getRequested_Port()
        // from here, so we do it in TransportManager.
        // if (Boolean.valueOf(_context.getProperty(CommSystemFacadeImpl.PROP_I2NP_NTCP_AUTO_PORT)).booleanValue())
        //    return foo;
        return _context.getProperty(CommSystemFacadeImpl.PROP_I2NP_NTCP_PORT, -1);
    }

    /**
     * Maybe we should trust UPnP here and report OK if it opened the port, but
     * for now we don't. Just go through and if we have one inbound connection,
     * we must be good. As we drop idle connections pretty quickly, this will
     * be fairly accurate.
     *
     * We have to be careful here because much of the router console code assumes
     * that the reachability status is really just the UDP status.
     */
    @Override
    public short getReachabilityStatus() { 
        if (isAlive() && _myAddress != null) {
                for (NTCPConnection con : _conByIdent.values()) {
                    if (con.isInbound())
                        return CommSystemFacade.STATUS_OK;
                }
        }
        return CommSystemFacade.STATUS_UNKNOWN;
    }

    /**
     *  This doesn't (completely) block, caller should check isAlive()
     *  before calling startListening() or restartListening()
     */
    public synchronized void stopListening() {
        if (_log.shouldLog(Log.WARN)) _log.warn("Stopping ntcp transport");
        _pumper.stopPumping();
        _writer.stopWriting();
        _reader.stopReading();
        _finisher.stop();
        Map cons = null;
        synchronized (_conLock) {
            cons = new HashMap(_conByIdent);
            _conByIdent.clear();
        }
        for (Iterator iter = cons.values().iterator(); iter.hasNext(); ) {
            NTCPConnection con = (NTCPConnection)iter.next();
            con.close();
        }
        NTCPConnection.releaseResources();
        // will this work?
        replaceAddress(null);
    }

    public static final String STYLE = "NTCP";

    public void renderStatusHTML(java.io.Writer out, int sortFlags) throws IOException {}

    @Override
    public void renderStatusHTML(java.io.Writer out, String urlBase, int sortFlags) throws IOException {
        TreeSet peers = new TreeSet(getComparator(sortFlags));
        peers.addAll(_conByIdent.values());

        long offsetTotal = 0;
        float bpsSend = 0;
        float bpsRecv = 0;
        long totalUptime = 0;
        long totalSend = 0;
        long totalRecv = 0;

        StringBuilder buf = new StringBuilder(512);
        buf.append("<h3 id=\"ntcpcon\">").append(_("NTCP connections")).append(": ").append(peers.size());
        buf.append(". ").append(_("Limit")).append(": ").append(getMaxConnections());
        buf.append(". ").append(_("Timeout")).append(": ").append(DataHelper.formatDuration2(_pumper.getIdleTimeout()));
        buf.append(".</h3>\n" +
                   "<table>\n" +
                   "<tr><th><a href=\"#def.peer\">").append(_("Peer")).append("</a></th>" +
                   "<th>").append(_("Dir")).append("</th>" +
                   "<th align=\"right\"><a href=\"#def.idle\">").append(_("Idle")).append("</a></th>" +
                   "<th align=\"right\"><a href=\"#def.rate\">").append(_("In/Out")).append("</a></th>" +
                   "<th align=\"right\"><a href=\"#def.up\">").append(_("Up")).append("</a></th>" +
                   "<th align=\"right\"><a href=\"#def.skew\">").append(_("Skew")).append("</a></th>" +
                   "<th align=\"right\"><a href=\"#def.send\">").append(_("TX")).append("</a></th>" +
                   "<th align=\"right\"><a href=\"#def.recv\">").append(_("RX")).append("</a></th>" +
                   "<th>").append(_("Out Queue")).append("</th>" +
                   "<th>").append(_("Backlogged?")).append("</th>" +
                   //"<th>").append(_("Reading?")).append("</th>" +
                   " </tr>\n");
        out.write(buf.toString());
        buf.setLength(0);
        for (Iterator iter = peers.iterator(); iter.hasNext(); ) {
            NTCPConnection con = (NTCPConnection)iter.next();
            buf.append("<tr><td class=\"cells\" align=\"left\" nowrap>");
            buf.append(_context.commSystem().renderPeerHTML(con.getRemotePeer().calculateHash()));
            //byte[] ip = getIP(con.getRemotePeer().calculateHash());
            //if (ip != null)
            //    buf.append(' ').append(_context.blocklist().toStr(ip));
            buf.append("</td><td class=\"cells\" align=\"center\">");
            if (con.isInbound())
                buf.append("<img src=\"/themes/console/images/inbound.png\" alt=\"Inbound\" title=\"").append(_("Inbound")).append("\"/>");
            else
                buf.append("<img src=\"/themes/console/images/outbound.png\" alt=\"Outbound\" title=\"").append(_("Outbound")).append("\"/>");
            buf.append("</td><td class=\"cells\" align=\"right\">");
            buf.append(DataHelper.formatDuration2(con.getTimeSinceReceive()));
            buf.append(THINSP).append(DataHelper.formatDuration2(con.getTimeSinceSend()));
            buf.append("</td><td class=\"cells\" align=\"right\">");
            if (con.getTimeSinceReceive() < 2*60*1000) {
                float r = con.getRecvRate();
                buf.append(formatRate(r / 1024));
                bpsRecv += r;
            } else {
                buf.append(formatRate(0));
            }
            buf.append(THINSP);
            if (con.getTimeSinceSend() < 2*60*1000) {
                float r = con.getSendRate();
                buf.append(formatRate(r / 1024));
                bpsSend += r;
            } else {
                buf.append(formatRate(0));
            }
            //buf.append(" K/s");
            buf.append("</td><td class=\"cells\" align=\"right\">").append(DataHelper.formatDuration2(con.getUptime()));
            totalUptime += con.getUptime();
            offsetTotal = offsetTotal + con.getClockSkew();
            buf.append("</td><td class=\"cells\" align=\"right\">").append(DataHelper.formatDuration2(1000 * con.getClockSkew()));
            buf.append("</td><td class=\"cells\" align=\"right\">").append(con.getMessagesSent());
            totalSend += con.getMessagesSent();
            buf.append("</td><td class=\"cells\" align=\"right\">").append(con.getMessagesReceived());
            totalRecv += con.getMessagesReceived();
            long outQueue = con.getOutboundQueueSize();
            buf.append("</td><td class=\"cells\" align=\"center\">").append(outQueue);
            buf.append("</td><td class=\"cells\" align=\"center\">");
            if (con.isBacklogged())
                buf.append("&#x2713;");
            else
                buf.append("&nbsp;");
            //long readTime = con.getReadTime();
            //if (readTime <= 0) {
            //    buf.append("</td> <td class=\"cells\" align=\"center\">0");
            //} else {
            //    buf.append("</td> <td class=\"cells\" align=\"center\">").append(DataHelper.formatDuration(readTime));
            //}
            buf.append("</td></tr>\n");
            out.write(buf.toString());
            buf.setLength(0);
        }

        if (!peers.isEmpty()) {
//            buf.append("<tr> <td colspan=\"11\"><hr></td></tr>\n");
            buf.append("<tr class=\"tablefooter\"><td align=\"center\"><b>").append(peers.size()).append(' ').append(_("peers")).append("</b></td><td>&nbsp;</td><td>&nbsp;");
            buf.append("</td><td align=\"center\"><b>").append(formatRate(bpsRecv/1024)).append(THINSP).append(formatRate(bpsSend/1024)).append("</b>");
            buf.append("</td><td align=\"center\"><b>").append(DataHelper.formatDuration2(totalUptime/peers.size()));
            buf.append("</b></td><td align=\"center\"><b>").append(DataHelper.formatDuration2(offsetTotal*1000/peers.size()));
            buf.append("</b></td><td align=\"center\"><b>").append(totalSend).append("</b></td><td align=\"center\"><b>").append(totalRecv);
            buf.append("</b></td><td>&nbsp;</td><td>&nbsp;</td></tr>\n");
        }

        buf.append("</table>\n");
        out.write(buf.toString());
        buf.setLength(0);
    }

    private static final NumberFormat _rateFmt = new DecimalFormat("#,##0.00");

    private static String formatRate(float rate) {
        synchronized (_rateFmt) { return _rateFmt.format(rate); }
    }

    private Comparator getComparator(int sortFlags) {
        Comparator rv = null;
        switch (Math.abs(sortFlags)) {
            default:
                rv = AlphaComparator.instance();
        }
        if (sortFlags < 0)
            rv = Collections.reverseOrder(rv);
        return rv;
    }

    private static class AlphaComparator extends PeerComparator {
        private static final AlphaComparator _instance = new AlphaComparator();
        public static final AlphaComparator instance() { return _instance; }
    }

    private static class PeerComparator implements Comparator<NTCPConnection> {
        public int compare(NTCPConnection l, NTCPConnection r) {
            if (l == null || r == null)
                throw new IllegalArgumentException();
            // base64 retains binary ordering
            // UM, no it doesn't, but close enough
            return l.getRemotePeer().calculateHash().toBase64().compareTo(r.getRemotePeer().calculateHash().toBase64());
        }
    }

    private static final String BUNDLE_NAME = "net.i2p.router.web.messages";

    /**
     *  Translate
     */
    private final String _(String s) {
        return Translate.getString(s, _context, BUNDLE_NAME);
    }

    /**
     * Cache the bid to reduce object churn
     */
    private class SharedBid extends TransportBid {
        public SharedBid(int ms) { super(); setLatencyMs(ms); }
        @Override
        public Transport getTransport() { return NTCPTransport.this; }
        @Override
        public String toString() { return "NTCP bid @ " + getLatencyMs(); }
    }
}

