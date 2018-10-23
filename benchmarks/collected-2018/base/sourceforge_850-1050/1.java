// https://searchcode.com/api/result/3409667/

/*
 *  Copyright (C) 2001 David Hoag
 *  ObjectWave Corporation
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *  For a full copy of the license see:
 *  http://www.opensource.org/licenses/lgpl-license.html
 */
package com.objectwave.persist.broker;
import com.objectwave.logging.MessageLog;
import com.objectwave.persist.*;
import com.objectwave.persist.sqlConstruction.*;

import java.sql.*;

import java.util.LinkedList;
import java.util.List;
/**
 *  A connection to the database. Used by the connection pool and the RDBBroker.
 *  A not null value for the system property ow.persistConnectionVerbose will
 *  result in details of the SQL being sent over this connection to be logged.
 *  There will only ever be one thread operating on a single instance of an
 *  RDBConnection. A violation of that assumption will destroy the thread safety
 *  of this class.
 *
 * @author  Dave Hoag
 * @version  $Id: RDBConnection.java,v 2.9 2005/02/14 03:09:27 dave_hoag Exp $
 */
public class RDBConnection
{
	/**
	 *  Description of the Field
	 */
	public static int sqlStatementCount = 0;
	protected final static String insrtProp = "ow.doInsert";
	protected final static String metricsProp = "ow.persistMetrics";
	protected static boolean verbose = false;
	protected static boolean doInsert = true;
	/**
	 *  If manageTransaction is false, no commit or rollback calls will ever be
	 *  made.
	 */
	protected static boolean manageTransaction = true;
	protected static boolean metrics = System.getProperty(metricsProp, "").equals("true");
	protected SqlConnectionFactory connectionSource;
	protected String userName, password, connectUrl;
	protected Connection connection = null;
	protected boolean inTransaction;
	protected boolean supportsTransaction;
	protected boolean needsExplicitBegin = false;
	protected Thread thread;
	protected RDBConnectionPool pool;
	protected ObjectPoolBroker poolBroker = null;
	protected final Object[] objArrays = new Object[100];
	// To reduce the amount of Object Arrays we allocate.
	protected GrinderResultSet lastResultSet;
	// This'll be a cache of sets of objects:
	// a SQLModifier, a PreparedStatement, and a Persistence class.
	//
	protected SqlStatementCacheItem[] cachedStatements = null;
	protected int cachedCount = 0;
	//If we are not using prepared statments, we reuse on statement throughout the transaction.
	protected Statement cachedUpdateStmt = null;
	/**
	 *  An RDBConnection is an abstraction of a single connection to a relational
	 *  database. A single Broker may have several connections (often called
	 *  connection pooling) to a single relational database. If the desire is to
	 *  have connections to multiple databases, this would be done via different
	 *  instances of an RDBBroker.
	 *
	 * @param  pool The pool of which this connection is a member.
	 * @param  connectUrl The URL by which this connection is going to connect to
	 *  the database.
	 * @param  userName The user name to use to log into the database.
	 * @param  password The password for the provided user name.
	 * @see  com.objectwave.persist.broker.RDBBroker
	 */
	RDBConnection(final RDBConnectionPool pool, final String connectUrl, final String userName, final String password)
	{
		initialize(pool, connectUrl, userName, password);
	}
	/**
	 *  Allow brokers to change the connection factory.
	 *
	 * @param  factory The new ConnectionSource value
	 */
	public void setConnectionSource(final SqlConnectionFactory factory)
	{
		connectionSource = factory;
	}
	/**
	 *  If the framework is using an object pool in conjunction with the database,
	 *  we need to support transactional access to that object pool.
	 *
	 * @param  broker The new ObjectPoolBroker value
	 */
	public void setObjectPoolBroker(final ObjectPoolBroker broker)
	{
		poolBroker = broker;
	}
	/**
	 *  The ability to support properties other than System.getProperties();
	 *
	 * @param  b BrokerPropertyIF The source from which to determine broker
	 *  properties.
	 */
	public void setBrokerPropertySource(final BrokerPropertySource b)
	{
		verbose = b.isVerbose();
		doInsert = System.getProperty(insrtProp, "true").equals("true");
		metrics = System.getProperty(metricsProp, "false").equals("true");
	}
	/**
	 * @param  t java.lang.Thread
	 */
	protected void setThread(Thread t)
	{
		thread = t;
	}
	/**
	 *  Change the connection to the new value.
	 *
	 * @param  newValue The new Connection value
	 */
	protected void setConnection(Connection newValue)
	{
		connection = newValue;
	}
	/**
	 *  Every connection may end up manipulating the ObjectPoolImpl. Since this needs
	 *  to be done in a transactional manner, the actual access to the pool is done
	 *  through the ObjectPoolBroker.
	 *
	 * @return  com.objectwave.persist.ObjectPoolBroker The associated object pool
	 *  broker.
	 */
	public ObjectPoolBroker getObjectPoolBroker()
	{
		return poolBroker;
	}
	/**
	 * @return  java.sql.DatabaseMetaData
	 * @exception  SQLException Description of Exception
	 * @author  Dave Hoag
	 */
	public DatabaseMetaData getDatabaseMetaData() throws SQLException
	{
		return connection.getMetaData();
	}
	/**
	 *  Get the thread that is currently related to this connection.
	 *
	 * @return  Thread Null if there is not related thread.
	 */
	public Thread getThread()
	{
		return thread;
	}
	/**
	 *  Gets the LastResultSet attribute of the RDBConnection object
	 *
	 * @return  The LastResultSet value
	 */
	public GrinderResultSet getLastResultSet()
	{
		return lastResultSet;
	}
	/**
	 * @return  The InTransaction value
	 * @author  Dave Hoag
	 */
	public boolean isInTransaction()
	{
		return inTransaction;
	}
	/**
	 * @return  The DefaultConnectionSource value
	 */
	protected SqlConnectionFactory getDefaultConnectionSource()
	{
		return new DefaultConnectionSource();
	}
	/**
	 * @return  java.lang.Object[]
	 * @author  Dave Hoag
	 */
	protected Object[] getObjectArrays()
	{
		return objArrays;
	}
	/**
	 *  The associated connection pool of which this connection is a member.
	 *
	 * @return  com.objectwave.persist.RDBConnectionPool
	 */
	protected RDBConnectionPool getPool()
	{
		return pool;
	}
	/**
	 *  Return the database connection if a connection has been established.
	 *
	 * @return  The actual db connection or null
	 */
	Connection getConnection()
	{
		return connection;
	}
	/**
	 *  If the database supports transactions this method would begin the
	 *  transaction.
	 *
	 * @exception  SQLException Description of Exception
	 * @author  Dave Hoag
	 */
	public void beginTransaction() throws SQLException
	{
		checkConnection();
		if(!supportsTransaction)
		{
			return;
		}

		if(needsExplicitBegin)
		{
			execSql("begin transaction");
		}

		if(verbose)
		{
			MessageLog.info(this, "IDThread: " + Thread.currentThread() + " : begin on " + this);
		}

		if(poolBroker != null)
		{
			try
			{
				poolBroker.beginTransaction();
			}
			catch(QueryException ex)
			{
			}
		}
		// setThread(Thread.currentThread()); Not necessary. The getConnection on connection pool took care of this.
		inTransaction = true;
	}
	/**
	 *  Issues a database Commit.
	 *
	 * @exception  SQLException Description of Exception
	 * @author  Dave Hoag
	 */
	public void commit() throws SQLException
	{
		if(verbose)
		{
			MessageLog.warn(this, " IDThread: " + Thread.currentThread() + " : Committing to connectUrl \"" + connectUrl + "\" on" + this);
		}

		if(supportsTransaction)
		{
			if(manageTransaction)
			{
				connection.commit();
			}
			inTransaction = false;
			if(cachedUpdateStmt != null)
			{
				cachedUpdateStmt.close();
				cachedUpdateStmt = null;
			}
			if(poolBroker != null)
			{
				try
				{
					poolBroker.commit();
				}
				catch(QueryException ex)
				{
				}
			}
			freeConnection();
		}
	}
	/**
	 *  Execute and finish. Dangerous. Don't use. Exposed for bad reasons.
	 *
	 * @param  aString Description of Parameter
	 * @exception  SQLException Description of Exception
	 */
	public void execSql(String aString) throws SQLException
	{
		try
		{
			//"Make sure we have a database connection."
			checkConnection();

			if(verbose)
			{
				MessageLog.debug(this, "ExecSql " + aString + "\non " + this);
			}

			long time = System.currentTimeMillis();
			Statement stmt = connection.createStatement();
			sqlStatementCount++;
			stmt.execute(aString);
			if(metrics)
			{
				pool.incrementSqlTime(System.currentTimeMillis() - time);
			}
			stmt.close();
			if(!inTransaction)
			{
				//Not in finally block since this may generate an exception

				if(supportsTransaction && manageTransaction)
				{
					connection.commit();
					//Since auto commit has been disabled
				}
			}
		}
		finally
		{
			//Free this up for other threads.
			if(!inTransaction)
			{
				freeConnection();
			}
		}
	}

	/**
	 *  Execute and finish. Dangerous. Don't use. Exposed for bad reasons.
	 *
	 * @param  aString Description of Parameter
	 * @param  numberOfColumns
	 * @return
	 * @exception  SQLException Description of Exception
	 */
	public List executeQuery(String aString, int numberOfColumns) throws SQLException
	{
		List result = new LinkedList();
		try
		{
			//"Make sure we have a database connection."
			checkConnection();

			if(verbose)
			{
				MessageLog.debug(this, "ExecSql " + aString + "\non " + this);
			}

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(aString);

			while(rs.next())
			{
				String[] nextArray = new String[numberOfColumns];
				for(int i = 1; i <= numberOfColumns; i++)
				{
					nextArray[i - 1] = rs.getString(i);
					MessageLog.debug(this, "Processing: " + nextArray[i - 1]);
				}
				result.add(nextArray);
			}
			stmt.close();
			if(!inTransaction)
			{
				//Not in finally block since this may generate an exception

				if(supportsTransaction && manageTransaction)
				{
					connection.commit();
					//Since auto commit has been disabled
				}
			}
		}
		finally
		{
			//Free this up for other threads.
			if(!inTransaction)
			{
				freeConnection();
			}
		}

		return result;
	}
	/**
	 *  Issues a database rollback.
	 *
	 * @exception  SQLException Description of Exception
	 * @author  Dave Hoag
	 */
	public void rollback() throws SQLException
	{
		if(connection == null)
		{
			return;
		}
		//Nothing we can do about this
		if(supportsTransaction)
		{
			if(!inTransaction)
			{
				freeConnection();
			}
			else
			{
				//			execSql(getRollbackTransaction());
				if(verbose)
				{
					MessageLog.info(this, "IDThread: " + Thread.currentThread() + " : Rollback connectUrl \"" + connectUrl + "\" on" + this);
				}
				if(manageTransaction)
				{
					connection.rollback();
				}
				inTransaction = false;
				if(cachedUpdateStmt != null)
				{
					cachedUpdateStmt.close();
					cachedUpdateStmt = null;
				}
				if(poolBroker != null)
				{
					try
					{
						poolBroker.rollback();
					}
					catch(QueryException ex)
					{
					}
				}
				freeConnection();
			}
		}
	}
	/**
	 *  Every connection is associated with a particular thread. This drops the
	 *  relationship between the connection and the thread. Once the pool has been
	 *  notified that the connection is free, this connection is free to be used by
	 *  an additional thread.
	 *
	 * @see  com.objectwave.persist.RDBConnectionPool
	 */
	public void freeConnection()
	{
		connectionSource.freeConnection(this, connection);
	}
	/**
	 *  Callback hook from connectionSource to allow vendor specific modification
	 *  to the connection object.
	 *
	 * @param  con
	 * @exception  SQLException
	 */
	public void alterVendorConnection(final java.sql.Connection con) throws SQLException
	{
		//by default, do nothing
	}
	/**
	 * @param  pool
	 * @param  connectUrl
	 * @param  userName
	 * @param  password
	 */
	public void initialize(final RDBConnectionPool pool, final String connectUrl, final String userName, final String password)
	{
		this.userName = userName;
		this.password = password;
		this.connectUrl = connectUrl;
		this.pool = pool;
		connectionSource = getDefaultConnectionSource();
	}
	/**
	 *  Only in the event of a very bad thing should this method be invoked. This
	 *  will attempt to clean up the database connection related attributes of this
	 *  object.
	 */
	protected void clearConnection()
	{
		if(verbose)
		{
			MessageLog.debug(this, "Clearing databse connection information.");
		}
		java.sql.Connection conn = getConnection();
		if(conn != null)
		{
			try
			{
				conn.close();
			}
			catch(SQLException ex)
			{
				MessageLog.debug(this, "Error when closing a bad connection " + ex);
			}
		}
		setConnection(null);
		//Invalidate this this connection.
		lastResultSet = null;
		cachedStatements = null;
		cachedCount = 0;
		cachedUpdateStmt = null;
		//Should this also clear out the thread, etc...
	}
	/**
	 *  Add a prepared statement to this connection's path.
	 *
	 * @param  modifier com.objectwave.persist.SQLModifier
	 * @param  stmt java.sql.PreparedStatement
	 * @param  persistenceClass The feature to be added to the Statement attribute
	 */
	protected void addStatement(final SQLModifier modifier, final Class persistenceClass, final PreparedStatement stmt)
	{
		modifier.setAvailableForPool(false);
		SqlStatementCacheItem cached = new SqlStatementCacheItem(modifier, stmt, persistenceClass);
		if(cachedStatements == null)
		{
			cachedStatements = new SqlStatementCacheItem[100];
			cachedCount = 0;
		}
		cachedStatements[cachedCount++] = cached;
		if(cachedCount == cachedStatements.length)
		{
			SqlStatementCacheItem[] tmpCachedStatements = new SqlStatementCacheItem[cachedStatements.length + 50];
			System.arraycopy(cachedStatements, 0, tmpCachedStatements, 0, cachedStatements.length);
			cachedStatements = tmpCachedStatements;
		}
	}
	/**
	 *  Disconnect from database if not already done.
	 *
	 * @exception  SQLException Description of Exception
	 */
	protected void finalize() throws SQLException
	{
		if(connection != null)
		{
			connection.close();
		}
	}
	/**
	 *  Used for executing any sql query statement.
	 *
	 * @param  sqlObj Description of Parameter
	 * @return  Description of the Returned Value
	 * @exception  SQLException Description of Exception
	 * @exception  QueryException Description of Exception
	 * @author  Dave Hoag
	 */
	public GrinderResultSet findExecSql(SQLAssembler sqlObj) throws SQLException, QueryException
	{
		return findExecSql(sqlObj, false);
	}
	/**
	 *  Used for executing any sql query statement.
	 *
	 * @param  sqlObj Description of Parameter
	 * @param  release Description of Parameter
	 * @return  Description of the Returned Value
	 * @exception  SQLException Description of Exception
	 * @exception  QueryException Description of Exception
	 */
	public GrinderResultSet findExecSql(SQLAssembler sqlObj, boolean release) throws SQLException, QueryException
	{
		try
		{
			//"Make sure we have a database connection."
			checkConnection();
			String aString = sqlObj.getSqlStatement().toString();
			MessageLog.debug(this, "IDThread: " + Thread.currentThread() + " : " + aString);

			// Create a Statement object so we can submit
			// SQL statements to the driver
			long time = System.currentTimeMillis();

			Statement stmt = null;
			GrinderResultSet gres = getLastResultSet();
			if(gres != null)
			{
				if(gres.isAvailable())
				{
					stmt = gres.getStatement();
//					System.out.println("Reusing statment! " +  gres + " " + Thread.currentThread());
					gres.setAvailable(false);
				}
				else
				{
					lastResultSet = null;
					gres.setDropped(true);
					gres = null;
					stmt = connection.createStatement();
				}
			}
			else
			{
				stmt = connection.createStatement();
			}
			//"Execute the statement."  // Submit a query, creating a ResultSet object
			try
			{
				ResultSet rs = null;
				try
				{
					rs = stmt.executeQuery(aString);
					//System.out.println( "OW QUERY = "+ aString );
				}
				catch(SQLException ex)
				{
					if(tryAgain(ex.getErrorCode()))
					{
						rs = stmt.executeQuery(aString);
					}
					else
					{
						throw ex;
					}
				}
				if(metrics)
				{
					pool.incrementSqlTime(System.currentTimeMillis() - time);
				}
				sqlStatementCount++;
				if(gres != null)
				{
					gres.setResultSet(rs);
					return gres;
				}
				else
				{
					lastResultSet = new GrinderResultSet(rs, stmt, this);
//					System.out.println("Creating new statment! " + lastResultSet + " " );
					return lastResultSet;
				}
			}
			catch(SQLException ex)
			{
				MessageLog.warn(this, "Exception SQL :'" + aString + "' Thread: " + Thread.currentThread(), ex);
				throw new QueryException(ex.toString(), ex, "Exception SQL :'" + aString + "' Thread: " + Thread.currentThread());
			}
		}
		finally
		{
			if(!inTransaction && release)
			{
				freeConnection();
			}
		}
	}
	/**
	 *  Execute java.sql.PreparedStatement code, perhaps creating the
	 *  PreparedStatement object in the process.
	 *
	 * @param  sqlObj
	 * @param  pObj
	 * @exception  SQLException
	 * @exception  QueryException
	 */
	public void preparedUpdateSql(SQLModifier sqlObj, final Persistence pObj) throws SQLException, QueryException
	{
		PreparedStatement stmt = null;
		try
		{
			//"Make sure we have a database connection."
			checkConnection();
			try
			{
				final Class pObjClass = pObj.getClass();
				// Create a Statement object so we can submit
				// SQL statements to the driver
				SqlStatementCacheItem modAndStmt = findStatement(sqlObj.getClass(), pObjClass);
				preSqlCall();
				if(modAndStmt != null)
				{
					stmt = reuseStatement(sqlObj, pObj, pObjClass, modAndStmt);
					executePrepStatement(stmt);
				}
				else
				{
					stmt = createStatement(sqlObj, pObj, pObjClass);
					executePrepStatement(stmt);
				}
				sqlStatementCount++;
				postSqlCall();
			}
			catch(SQLException ex)
			{
				MessageLog.warn(this, "Exception Prepared SqlUpdate: '" + sqlObj.getSqlStatement() + "' " + Thread.currentThread());
				throw ex;
			}
		}
		finally
		{
			if(!inTransaction)
			{
				//

				freeConnection();
			}
		}
	}
	/**
	 *  Issue an update statement.
	 *
	 * @param  sqlObj Description of Parameter
	 * @exception  QueryException Description of Exception
	 * @exception  SQLException Description of Exception
	 */
	public void updateExecSql(SQLAssembler sqlObj) throws QueryException, SQLException
	{
		Statement stmt = cachedUpdateStmt;
		try
		{
			//"Make sure we have a database connection."
			checkConnection();

			String aString = sqlObj.getSqlStatement().toString();

			MessageLog.debug(this, "IDThread: " + Thread.currentThread() + " : " + aString + "\non " + this + " TXN?" + inTransaction);

			// Create a Statement object so we can submit
			// SQL statements to the driver
//			long time = System.currentTimeMillis();
			if(!inTransaction)
			{
				stmt = connection.createStatement();
			}
			else
					if(stmt == null)
			{
				stmt = connection.createStatement();
				cachedUpdateStmt = stmt;
			}

			long time = System.currentTimeMillis();
			try
			{
				int count = 0;
				if(doInsert)
				{
					try
					{
						count = stmt.executeUpdate(aString);
					}
					catch(SQLException ex)
					{
						if(tryAgain(ex.getErrorCode()))
						{
							count = stmt.executeUpdate(aString);
						}
						else
						{
							throw ex;
						}
					}
				}
				if(metrics)
				{
					pool.incrementSqlTime(System.currentTimeMillis() - time);
				}
				sqlStatementCount++;
				MessageLog.debug(this, "Updated " + count + " rows.");
				//MessageLog.debug(this, String.valueOf(stmt.getUpdateCount()));
				postSqlCall();
			}
			catch(SQLException ex)
			{
				MessageLog.warn(this, "Exception SQLUpdate :'" + aString + "' Thread: " + Thread.currentThread());
				throw (SQLException) ex.fillInStackTrace();
			}
		}
		finally
		{
			if(!inTransaction)
			{
				freeConnection();
				if(stmt != null)
				{
					stmt.close();
					stmt = null;
					//Just to help the GC. Don't really care about exceptions in the close
				}
			}
		}
	}
	/**
	 *  There may be error codes that mean the connection should simply try the
	 *  statement a second time.
	 *
	 * @param  errorCode Description of Parameter
	 * @return  true if the sql statement should be tried a second time.
	 */
	protected boolean tryAgain(int errorCode)
	{
		return false;
	}
	/**
	 *  Usually a no op. This is a hook point for extending connections.
	 *
	 * @param  stmt java.sql.PreparedStatement
	 */
	protected void updateStatement(PreparedStatement stmt)
	{
		//
	}
	/**
	 * @param  sqlA Description of Parameter
	 * @return  Description of the Returned Value
	 * @exception  SQLException Description of Exception
	 * @exception  QueryException Description of Exception
	 */
	public int nextPrimaryKey(final SQLSelect sqlA) throws SQLException, QueryException
	{
		GrinderResultSet gres = findExecSql(sqlA, false);
		ResultSet rsA = gres.getResultSet();
		rsA.next();
		int resultA = rsA.getInt(1);
		gres.close();
		return resultA;
	}
	/**
	 *  Verify that there is already a connection to the relational database. If
	 *  not, establish one. There should only be 1 thread per connection.
	 *
	 * @return
	 * @exception  SQLException Description of Exception
	 */
	protected Connection checkConnection() throws SQLException
	{
		return connectionSource.checkConnection(this);
	}
	/**
	 *  Find a prepared statement in the local cache.
	 *
	 * @param  modifierClass Description of Parameter
	 * @param  persistenceClass Description of Parameter
	 * @return  Pair
	 */
	private final SqlStatementCacheItem findStatement(final Class modifierClass, final Class persistenceClass)
	{
		if(cachedStatements != null)
		{
			for(int i = 0; i < cachedCount; ++i)
			{
				SqlStatementCacheItem cachedStmt = cachedStatements[i];
				Class c = cachedStmt.getModifier().getClass();

				if(cachedStmt.getPersistenceClass().isAssignableFrom(persistenceClass) &&
						c.isAssignableFrom(modifierClass))
				{
					return cachedStmt;
				}
			}
		}
		return null;
	}
	/**
	 * @exception  SQLException Description of Exception
	 */
	private final void preSqlCall() throws SQLException
	{
		//nothing to do
	}
	/**
	 * @exception  SQLException Description of Exception
	 */
	private final void postSqlCall() throws SQLException
	{
		if(!inTransaction)
		{
			//Not in finally block since this may generate an exception

			if(supportsTransaction && manageTransaction)
			{
				if(verbose)
				{
					MessageLog.debug(this, "Forcing commit since we are not in a transaction on " + this + " Thread: " + Thread.currentThread());
				}
				connection.commit();
				//Since auto commit has been disabled
			}
		}
	}
	/**
	 *  Do the actual JDBC update. If it fails, and we know a simple retry will fix
	 *  the problem, we simply retry once.
	 *
	 * @param  stmt Description of Parameter
	 * @exception  SQLException Description of Exception
	 */
	private final void executePrepStatement(final PreparedStatement stmt) throws SQLException
	{
		long time = System.currentTimeMillis();
		int count = 0;
		if(doInsert)
		{
			try
			{
				count = stmt.executeUpdate();
			}
			catch(SQLException ex)
			{
				if(tryAgain(ex.getErrorCode()))
				{
					count = stmt.executeUpdate();
				}
				else
				{
					throw ex;
				}
			}
		}
		stmt.clearParameters();
		if(metrics)
		{
			pool.incrementSqlTime(System.currentTimeMillis() - time);
		}
		if(verbose)
		{
			MessageLog.debug(this, "Updated " + count + " rows on " + this + " Thread: " + Thread.currentThread());
		}
	}
	/**
	 *  Reuse an previously created prepared statment
	 *
	 * @param  sqlObj Description of Parameter
	 * @param  pObj Description of Parameter
	 * @param  pObjClass Description of Parameter
	 * @param  modAndStmt Description of Parameter
	 * @return  Description of the Returned Value
	 * @exception  SQLException Description of Exception
	 * @exception  QueryException Description of Exception
	 */
	private final PreparedStatement reuseStatement(final SQLModifier sqlObj, final Persistence pObj, final Class pObjClass, final SqlStatementCacheItem modAndStmt) throws SQLException, QueryException
	{
		if(verbose)
		{
			MessageLog.debug(this, String.valueOf(System.currentTimeMillis()) + " Using existing prepared statment on " + this + " Thread: " + Thread.currentThread() + " TXN?" + inTransaction);
			MessageLog.debug(this, "IDThread: " + Thread.currentThread() + " : " + sqlObj.getSqlStatement());
		}
		final PreparedStatement stmt = modAndStmt.getStatement();
		final SQLModifier mod = modAndStmt.getModifier();
		//
		// It's good to reuse the sqlObj since it minimizes the expense of initSqlTypes()
		//
		mod.copyValuesFrom(sqlObj);
		mod.bindValues(stmt, pObjClass, verbose);
		return stmt;
	}
	/**
	 *  Create a new PreparedStatement for the sql modifier (update or insert).
	 *
	 * @param  sqlObj Description of Parameter
	 * @param  pObj Description of Parameter
	 * @param  pObjClass Description of Parameter
	 * @return  Description of the Returned Value
	 * @exception  SQLException Description of Exception
	 * @exception  QueryException Description of Exception
	 */
	private final PreparedStatement createStatement(final SQLModifier sqlObj, final Persistence pObj, final Class pObjClass) throws SQLException, QueryException
	{
		if(verbose)
		{
			MessageLog.debug(this, "Creating new prepared statment on " + this + " Thread: " + Thread.currentThread() + " TXN?" + inTransaction);
			MessageLog.debug(this, "IDThread: " + Thread.currentThread() + " : " + sqlObj.getSqlStatement());
		}
		final PreparedStatement stmt = connection.prepareStatement(sqlObj.getPreparedString());
		updateStatement(stmt);
		addStatement(sqlObj, pObjClass, stmt);
		//Cache the statement so we don't create it again

		boolean origInTransaction = inTransaction;
		inTransaction = true;
		//Prevent the initSqlTypes method from freeing the connection.
		sqlObj.bindValues(stmt, pObjClass, verbose);
		inTransaction = origInTransaction;
		//Restore state back to actual value.

		return stmt;
		//Return the statment object good and ready to go
	}
}

