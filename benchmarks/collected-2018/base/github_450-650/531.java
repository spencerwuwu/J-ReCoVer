// https://searchcode.com/api/result/113076463/

/*
 *  BEAJoltConnection.java
 *
 *  Created on 10 September 2001, 11:42
 */

package org.jini.projects.athena.connects.jolt;


import java.sql.Connection;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import org.jini.projects.athena.command.dialect.Dialect;
import org.jini.projects.athena.connection.HostErrorHandler;
import org.jini.projects.athena.connects.jolt.chi.BEAJoltCall;
import org.jini.projects.athena.connects.jolt.syshandlers.JoltSysHandler;
import org.jini.projects.athena.exception.AthenaException;
import org.jini.projects.athena.exception.PooledConnectionException;
import org.jini.projects.athena.service.StatisticMonitor;

import bea.jolt.JoltRemoteService;
import bea.jolt.JoltSession;
import bea.jolt.JoltSessionAttributes;
import bea.jolt.JoltTransaction;

/**
 * System Connection class representing a connection to a BEA Jolt system. This
 * class handles talking to Jolt and then to Elink (Tuxedo).
 *
 * @author calum
 * @version 0.9community */
public class BEAJoltConnection implements org.jini.projects.athena.connection.SystemConnection {
    private static int Statref = 0;
    public String PersistentFile = null;
    JoltSession session;
    JoltSessionAttributes sattr;
    JoltRemoteService remoteCall;
    JoltTransaction trans;
    long MagicNumber = 0L;
    boolean packFrame = false;
    private Vector commands = new Vector();
    private String username = null;
    private String password = null;
    private String appPassword = null;
    private String hoststring = null;
    private String role = "org.jini.projects.athena";
    Logger l = Logger.getLogger("org.jini.projects.athena.connection");
    private boolean DEBUG = System.getProperty("org.jini.projects.athena.debug") != null ? true : false;;
    private boolean allocated = false;
    private boolean connected = false;
    private boolean canBeFreed = false;
    private boolean inTxn = false;
    private boolean autoAbort = false;
    private int timeoutsecs = System.getProperty("org.jini.projects.athena.connection.invoketimeout") != null ? Integer.parseInt(System.getProperty("org.jini.projects.athena.connection.invoketimeout")) : 60; // Default
    // to
    // one
    // minute
    private int ref = 0;
    private int numalloc = 0;
    private HostErrorHandler errHandler;

    /**
     * Creates new BEAJoltConnection Creates a new Connection with the given
     * parameters
     *
     * @exception PooledConnectionException
     *                        Description of Exception
     * @since @throws
     *             PooledConnectionException if an exception ocurs during
     *             initialization
     */
    public BEAJoltConnection() throws PooledConnectionException {
        try {
            this.username = System.getProperty("org.jini.projects.athena.connect.username");
            this.password = System.getProperty("org.jini.projects.athena.connect.password");
            this.appPassword = System.getProperty("org.jini.projects.athena.connect.apppassword");
            this.role = System.getProperty("org.jini.projects.athena.connect.role");
            this.hoststring = System.getProperty("org.jini.projects.athena.connect.host");
            sattr = new JoltSessionAttributes();
            ref = BEAJoltConnection.Statref;
            BEAJoltConnection.Statref++;
            sattr.setString(JoltSessionAttributes.APPADDRESS, hoststring);
            errHandler = new JoltSysHandler();
            sattr.setInt(JoltSessionAttributes.IDLETIMEOUT, 300);
            session = new JoltSession(sattr, username, role, password, appPassword);
            connected = true;
        } catch (Exception ex) {
            System.err.println("Indicating dropped Connection");
            allocated = false;
            connected = false;
            throw new PooledConnectionException(ex.getMessage());
        }
    }

    /**
     * Constructor for the BEAJoltConnection object
     *
     * @param ref
     *                   Description of Parameter
     * @exception PooledConnectionException
     *                        Description of Exception
     * @since
     */
    public BEAJoltConnection(int ref) throws PooledConnectionException {
        this();
        this.ref = ref;
    }

    /**
     * Sets the transaction flag to indicate active transaction status
     *
     * @param flag
     *                   The new transactionFlag value
     * @exception AthenaException
     *                        Description of Exception
     * @since
     */
    public void setTransactionFlag(boolean flag) throws AthenaException {
        if (flag) {
            if (DEBUG) {
                l.info("************IN A TRANSACTION NOW!!!!!!!!!");
            }
        } else if (DEBUG) {
            l.info("************CLOSING TRANSACTION NOW!!!!!!!!!");
        }
        inTxn = flag;
    }

    /**
     * Sets the filename reference for logging
     *
     * @param ref
     *                   The new reference value
     * @since
     */
    public void setReference(int ref) {
        PersistentFile = System.getProperty("org.jini.projects.athena.service.name") + "CONN" + ref + ".ser";
    }

    /**
     * Gets the filename to which the connection wil store it's txn recovery
     * information
     *
     * @return The persistentFileName value
     * @since
     */
    public String getPersistentFileName() {
        return this.PersistentFile;
    }

    /**
     * Gets the connection attribute of the BEAJoltConnection object -
     * <b>UNIMPLEMNTED </b>
     *
     * @return The connection value
     * @since
     */
    public Connection getConnection() {
        allocated = true;
        return null;
    }

    /**
     * Gets the connected status i.e. does this object represent an active
     * connection on the host
     *
     * @return The connected value
     * @since
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Invoke the command against the host
     *
     * @param command
     *                   Description of Parameter
     * @return Description of the Returned Value
     * @exception Exception
     *                        Description of Exception
     * @since
     */
    public Object issueCommand(Object command) throws Exception {
        Integer DMLreturnval = null;
        if (trans == null && this.inTxn) {
            l.finest("CREATING A TRANSACTION!!!");
            trans = new JoltTransaction(timeoutsecs, session);
        }
        try {
            commands.add(command);
            Vector table;
            l.finest("Command: [" + command.getClass().getName() + "]");
            if (command instanceof org.jini.projects.athena.command.Command) {
                org.jini.projects.athena.command.Command joltcomm = (org.jini.projects.athena.command.Command) command;
                l.finest("Obtained a remote call for [" + joltcomm.getCallName() + "]");
                remoteCall = new JoltRemoteService(joltcomm.getCallName(), session);
                BEAJoltCall jcall;
                l.finest("got a BEAJolt Command");
                if (joltcomm.getParameter("_DIALECT") != null) {
                    Dialect dialect = (Dialect) joltcomm.getParameter("_DIALECT");
                    joltcomm.removeParameter("_DIALECT");
                    jcall = new BEAJoltCall(remoteCall, joltcomm, dialect, trans);
                } else
                    jcall = new BEAJoltCall(remoteCall, joltcomm, trans);
                l.finest("Trans: " + (trans == null ? "NULL!" : "ALIVE"));
                Object returnObj = jcall.execute();
                if (returnObj instanceof HashMap) {
                    l.finest("Returning a HashMap");
                    HashMap retval = (HashMap) returnObj;
                    return retval;
                }
                if (returnObj instanceof Vector) {
                    //Assertion: A Vector of HashMaps i.e. a table block
                    l.finest("Returning a Vector");
                    return new org.jini.projects.athena.resultset.VofHResultSet((Vector) returnObj, jcall.getHeader());
                }
            }
        } catch (Exception ex) {
            this.autoAbort = true;
            StatisticMonitor.addFailure();
            System.err.println("Sending Error to HostHandler");
            if (this.errHandler != null) {
                errHandler.handleHostException(ex);
            }
            throw ex;
        }
        return DMLreturnval;
    }

    /**
     * Gets the allocated status
     *
     * @return The allocated value
     * @since
     */
    public boolean isAllocated() {
        return allocated;
    }

    /**
     * Invoke a command against the host system - <b>UNIMPLEMENTED </b>
     *
     * @param command
     *                   Description of Parameter
     * @param params
     *                   Description of Parameter
     * @return Description of the Returned Value
     * @exception Exception
     *                        Description of Exception
     * @since
     */
    public Object issueCommand(Object command, Object[] params) throws Exception {
        return null;
    }

    /**
     * Gets the auto-abort status
     *
     * @return The autoAbortSet value
     * @exception AthenaException
     *                        Description of Exception
     * @since
     */
    public boolean isAutoAbortSet() throws AthenaException {
        return autoAbort;
    }

    /**
     * Gets the systemCommand that can be used by clients to communicate with
     * the host
     *
     * @return The systemCommand value
     * @exception AthenaException
     *                        Description of Exception
     * @since
     */
    public org.jini.projects.athena.command.Command getSystemCommand() throws AthenaException {
        return new org.jini.projects.athena.command.StdCommand();
    }

    /**
     * Commit all effects of the current transaction
     *
     * @return Description of the Returned Value
     * @exception Exception
     *                        Description of Exception
     * @since
     */
    public synchronized boolean commit() throws Exception {
        System.out.println("System asking to commit: " + this.ref);
        int retries = 2;
        try {
            trans.commit();
            l.finer("Commit finished");
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            StatisticMonitor.addFailure();
            int i;
            for (i = 0; i < retries; i++) {
                try {
                    System.err.println("\t*****Retry: " + i + "*****");
                    if (reinitialize()) {
                        System.err.println("Transaction completed");
                        trans = null;
                        return true;
                    }
                } catch (Exception ex2) {
                    System.err.println("Transaction exception");
                    if (this.errHandler != null) {
                        errHandler.handleHostException(ex);
                    }
                }
            }
            if (i == retries) {
                System.err.println("Transaction could not be rolled forward");
                trans = null;
                return false;
            }
        }
        trans = null;
        l.finer("Transaction closed");
        //conn.commit();
        return true;
    }

    /**
     * Rollback all effects of any updates in the current transaction
     *
     * @return success of rollback
     * @exception Exception
     *                        Description of Exception
     * @since
     */
    public synchronized boolean rollback() throws Exception {
        try {
            l.finest("System asking to rollback: " + this.ref);
            if (trans != null)
                trans.rollback();
            else
                l.finest("\tNO TRANSACTION OPEN - clean");
            trans = null;

            //conn.rollback();
            return true;
        } catch (Exception ex) {
            StatisticMonitor.addFailure();
            if (this.errHandler != null) {
                errHandler.handleHostException(ex);
            }
            throw ex;
        }
    }

    /**
     * Connect to another datasource - Unimplemented
     *
     * @param connectionProperties
     *                   Description of Parameter
     * @since
     */
    public void connectTo(java.util.Properties connectionProperties) {
    }

    /**
     * Release the connection and make it available to other clients
     *
     * @since
     */
    public void release() {
        if (DEBUG) {
            System.out.println("Connection returned to pool");
        }
        synchronized (this) {
            allocated = false;
            numalloc--;
        }
        if (DEBUG) {
            System.out.println("Deallocated: " + this.ref);
        }
        //trans = new JoltTransaction(50,this.session);
    }

    /**
     * Returns whether the connection can be freed - i.e. removed and connected
     * to a non-default datasource <b>DO NOT USE </b>- Strictly speaking all
     * connections should be built as defaults.
     *
     * @return Description of the Returned Value
     *
     */
    public boolean canFree() {
        return canBeFreed;
    }

    /**
     * Marks the connection as allocated, so that no other clients can access
     * it
     *
     *
     */
    public synchronized void allocate() {
        if (DEBUG) {
            System.out.println("Allocated: " + this.ref);
        }
        allocated = true;
        numalloc++;
        commands.clear();
        if (numalloc > 1) {
            System.out.println("Allocated more than once!");
        }
    }

    /**
     * Closes the connections and ends the Jolt session. If the system is in a
     * txn, it will be rolled-back
     *
     * @exception AthenaException
     *                        Description of Exception
     *
     */
    public synchronized void close() throws AthenaException {
        try {
            if (DEBUG) {
                System.out.println("Rolling back Jolt Transaction");
            }
            if (trans != null) {
                trans.rollback();
            }
            if (DEBUG) {
                System.out.println("Closing transaction");
            }
            trans = null;
            session.endSession();
            this.connected = false;
        } catch (Exception ex) {
            StatisticMonitor.addFailure();
            if (this.errHandler != null) {
                errHandler.handleHostException(ex);
            }
            throw new AthenaException(ex);
        }
    }

    /**
     * Returns whether this connection is participating in a txn
     *
     * @return Description of the Returned Value
     * @exception AthenaException
     *                        Description of Exception
     *
     */
    public boolean inTransaction() throws AthenaException {
        return inTxn;
    }

    /**
     * Changes the auto-abort flag to allow new transactions
     *
     * @exception AthenaException
     *                        Description of Exception
     *
     */
    public void resetAutoAbort() throws AthenaException {
        autoAbort = false;
    }

    /**
     * Reconnects to Jolt if a connection is ad-hoc
     *
     * @exception AthenaException
     *                        Description of Exception
     *
     */
    public void reConnect() throws AthenaException {
        try {
            System.out.println("\t*****Reconnecting to Jolt now*****");
            session = new JoltSession(sattr, username, role, password, appPassword);
            this.trans = null;
            connected = true;
        } catch (Exception ex) {
            System.err.println("Indicating dropped Connection");
            allocated = false;
            connected = false;
            if (this.errHandler != null) {
                errHandler.handleHostException(ex);
            }
            throw new AthenaException(new PooledConnectionException(ex.getMessage()));
        }
    }

    /**
     * Reinitilises a failed transaction in the event that a commit was
     * requested but the commit failed to get to Jolt. Written to reduce data
     * inconsistency between systems
     *
     * @return Description of the Returned Value
     *
     */
    private synchronized boolean reinitialize() {
        Vector commsend = null;
        trans = null;
        if (DEBUG) {
            System.out.println("Restarting session");
        }
        session.endSession();
        session = null;
        session = new JoltSession(sattr, username, role, password, appPassword);
        if (DEBUG) {
            System.out.println("Session restarted");
            System.out.println("CREATING A TRANSACTION!!!");
        }
        trans = new JoltTransaction(timeoutsecs, session);
        try {
            commsend = (Vector) commands.clone();
            if (DEBUG) {
                System.out.println("Clone size" + commsend.size());
            }
        } catch (Exception ex) {
            System.err.println("clone exception");
        }
        commands.clear();
        try {
            for (int i = 0; i < commsend.size(); i++) {
                issueCommand(commsend.get(i));
                if (DEBUG) {
                    System.out.println("\t*****Command " + i + " re-issued*****");
                }
            }
            trans.commit();
            if (DEBUG) {
                System.out.println("\t*****Commited*****");
            }
            trans = null;
            return true;
        } catch (Exception ex) {
            System.err.println("\t*****Didn't commit*****");
            System.err.println("\t*****Err: " + ex.getMessage() + "*****");
            ex.printStackTrace();
            StatisticMonitor.addFailure();
            if (this.errHandler != null) {
                errHandler.handleHostException(ex);
            }
        }
        trans = null;
        return false;
    }

    public void setAutoAbort() {
        this.autoAbort = true;
    }

    public Object handleType(Object in) throws AthenaException {
        return in;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jini.projects.org.jini.projects.athena.connection.SystemConnection#getErrorHandler()
     */
    public HostErrorHandler getErrorHandler() {
        // TODO Auto-generated method stub
        return this.errHandler;
    }
}

