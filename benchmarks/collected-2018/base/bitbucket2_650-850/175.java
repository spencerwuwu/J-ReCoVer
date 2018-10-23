// https://searchcode.com/api/result/52685669/

// This file is part of OpenCabinet.
// Copyright 2008, Thilo Planz
//
// OpenCabinet is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// OpenCabinet is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with OpenCabinet.  If not, see <http://www.gnu.org/licenses/>.
// 
//
package net.sf.opencabinet.core.sql;

import static net.sf.opencabinet.core.sql.And.log;
import static net.sf.opencabinet.core.sql.Changeset.Update;
import static net.sf.opencabinet.core.sql.TableSchema.SQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class that takes care of maintaining the revision history of database
 * tables as they are being updated using Changesets.
 * 
 * <p>
 * For every database table there is a corresponding history table into which
 * snapshot data of the rows affected by the Changeset is replicated.
 * 
 * <p>
 * All this is organised by a global revision number, stored in the revision log
 * table.
 * 
 * <p>
 * OpenCabinet uses this class internally, so that there will usually be no need
 * to instantiate or access it directly.
 * 
 * @author Thilo Planz
 * @since 0.2
 * 
 */

public class HistoryTableManager implements SelectHistory {

	private AccessDataSourceByPrimaryKey ads;

	private Map<String, String> historyTables = new HashMap<String, String>();

	private String revTablename;

	private LockStrategy locking;

	private static final Lock threadLock = new ReentrantLock();

	public enum LockStrategy {
		THREAD_LOCK, SELECT_FOR_UPDATE, SELECT_FOR_UPDATE_NOWAIT
	}

	/**
	 * @param ads
	 *            the database to use
	 * @param revisionLogTableName
	 *            the name of the revision log table
	 * @param locking
	 *            the locking strategy to use for commits, can be null, if you
	 *            plan to only read
	 */

	public HistoryTableManager(AccessDataSourceByPrimaryKey ads,
			String revisionLogTableName, LockStrategy locking) {
		if (revisionLogTableName != null)
			revTablename = revisionLogTableName.toUpperCase();
		this.ads = ads;
		this.locking = locking;
	}

	/**
	 * 
	 * @param changes
	 * @throws SQLException
	 * @since 0.3
	 */
	public int commit(Changeset changes) throws SQLException {

		Integer baseRevision = changes.getBaseRevision();
		if (baseRevision == null)
			baseRevision = 0;

		// get all involved table schemas first
		// (to reduce connection pool contention later, when we
		// have out long-running connection)
		Map<String, TableSchema> tss = changes.checkTableSchema(ads);
		tss.put(revTablename, ads.getTableSchema(revTablename));

		// detect conflicts
		if (baseRevision > 0) {
			int checkedRevision = getLatestRevisionNumber();
			changes.checkLatestRevisionNumbers(this, baseRevision, tss);
			baseRevision = checkedRevision;
		}

		if (locking == null)
			throw SQLException(11, locking);

		Lock lock = null;
		Connection conn = null;
		try {

			Object[] rev;
			// synchronize the places where we potentially
			// have two connections open at the same time
			// (so that we cannot run into nasty deadlocks)
			synchronized (ads) {
				// locking
				switch (locking) {
				case THREAD_LOCK:
					threadLock.lock();
					lock = threadLock;
					break;
				case SELECT_FOR_UPDATE:
					conn = ads.getDataSource().getConnection();
					conn.setAutoCommit(false);
					changes.selectLockForUpdate(tss, conn, baseRevision, true);
					break;
				case SELECT_FOR_UPDATE_NOWAIT:
					// if it is okay to overwrite (baseRev=0), then still wait
					conn = ads.getDataSource().getConnection();
					conn.setAutoCommit(false);
					changes.selectLockForUpdate(tss, conn, baseRevision,
							baseRevision == 0);
					break;
				default:
					throw SQLException(11, locking);
				}

				// detect conflicts after lock
				if (baseRevision > 0) {
					int latest = getLatestRevisionNumber();
					if (latest > baseRevision)
						changes.checkLatestRevisionNumbers(this, baseRevision,
								tss);

				}

				rev = startCommit(changes);
			}

			// not using select-for-update? get the connection now
			if (conn == null) {
				conn = ads.getDataSource().getConnection();
				conn.setAutoCommit(false);
			}

			ads.apply(changes, conn);
			changes.applyHistoryTableChanges(conn, (Integer) rev[0], this, tss);

			// finish commit
			rev[1] = new Timestamp(System.currentTimeMillis());
			ads.apply(Update(revTablename, rev), conn);

			conn.commit();

			commits++;
			return (Integer) rev[0];
		} catch (SQLException e) {
			if (conn != null)
				conn.rollback();
			throw e;

		} catch (RuntimeException e) {
			if (conn != null)
				conn.rollback();
			throw e;
		} finally {
			if (lock != null)
				lock.unlock();
			if (conn != null) {
				conn.setAutoCommit(true);
				conn.close();
			}
		}
	}

	private long commits, startCommits, getRevisionNumber,
			getRevisionNumberTable, selectIntegers, selectStrings,
			selectObjects, selectRows, getUpdatedRows;

	protected Object[] makeRevisionMetaDataRow(Changeset changes)
			throws Exception {
		return changes.makeRevisionMetaDataRow();
	}

	private Object[] startCommit(Changeset changes) throws SQLException {

		try {
			Object[] data = makeRevisionMetaDataRow(changes);

			// optimistic insert (no locking), repeat on duplicate primary key
			// error
			Integer revNo = getNextRevisionNumber();

			data[0] = revNo;

			// " ( no integer not null primary key, upd_date varchar, upd_user
			// integer, upd_app integer, upd_module integer, upd_type integer,
			// tables integer, comments varchar, props integer )");
			new SingleRowInsert(revTablename, data)
					.execute(ads.getDataSource());
			log.fine("starting commit of revision " + revNo + " for changeset "
					+ changes);
			startCommits++;
			return data;
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw SQLException(15, e, e.toString());
		}

	}

	/**
	 * registers a mapping between base and history table names. History tables
	 * are used to store snapshots of older versions of the data in the base
	 * table.
	 * 
	 * @param tableName
	 * @param historyTableName
	 *            the history table for the table
	 */

	public synchronized void setHistoryTableName(String tableName,
			String historyTableName) {
		historyTables.put(tableName.toUpperCase(), historyTableName
				.toUpperCase());
	}

	/**
	 * queries the history table map for the name of a table's history table.
	 * 
	 * @param tableName
	 * @return null, if no history table name has been set
	 */
	public synchronized String getHistoryTableName(String tableName) {
		return historyTables.get(tableName.toUpperCase());
	}

	/**
	 * gets the latest revision number for the whole database
	 * 
	 * @return the latest revision number
	 * @throws SQLException
	 */
	public int getLatestRevisionNumber() throws SQLException {
		getRevisionNumber++;
		Integer revNo = new ScalarSelect("max(no)", revTablename)
				.addWhereClause(new WhereNotNull("upd_date")).selectInteger(
						ads.getDataSource());
		if (revNo == null)
			return 0;
		return revNo;
	}

	private Integer getNextRevisionNumber() throws SQLException {
		getRevisionNumber++;
		Integer revNo = new ScalarSelect("max(no)", revTablename)
				.selectInteger(ads.getDataSource());
		if (revNo == null)
			return 1;
		return revNo + 1;
	}

	/**
	 * gets the latest revision number for a given table. You can specify a
	 * primary key to get the revision for a single row, or leave it empty to
	 * get it for the whole table, or (if it is a composite primary key) limit
	 * the search to the rows that match a prefix for the given key
	 * 
	 * @return the latest revision number
	 * @throws SQLException
	 */
	public int getLatestRevisionNumber(String tableName,
			Object... primaryKeyValues) throws SQLException {
		return getLatestRevisionNumber(null, tableName, primaryKeyValues);

	}

	/**
	 * gets the latest revision number (up to the given revision number) for a
	 * given table. You can specify a primary key to get the revision for a
	 * single row, or leave it empty to get it for the whole table, or (if it is
	 * a composite primary key) limit the search to the rows that match a prefix
	 * for the given key
	 * 
	 * @return the latest revision number
	 * @throws SQLException
	 */
	public int getLatestRevisionNumber(Integer rev, String tableName,
			Object... primaryKeyValues) throws SQLException {
		getRevisionNumberTable++;
		String historyTable = getHistoryTableName(tableName);
		if (historyTable == null)
			throw SQLException(7, tableName);
		TableSchema ts = ads.getTableSchema(historyTable);
		ScalarSelect s = new ScalarSelect("max(oc$rev)", historyTable);
		for (int i = 0; i < primaryKeyValues.length; i++) {
			s.addWhereClause(new WhereIn(ts.getPrimaryKeyColumnName(i),
					primaryKeyValues[i]));
		}
		if (rev != null)
			s.addWhereClause(new WhereInRange("oc$rev", null, rev));
		Integer i = s.selectInteger(ads.getDataSource());
		if (i == null)
			return 0;
		return i;

	}

	/**
	 * returns a collection of database rows that were updated in the given
	 * revision.
	 * 
	 * <p>
	 * The returned rows include two meta-data-columns in addition to the base
	 * table's columns: The first column (OC$REV) is the revision number (an
	 * Integer), the second one is the delete flag (OC$DEL, an Integer), which
	 * will be null unless the row has been deleted in that revision, in which
	 * case it will be "1" (Integer "one").
	 * 
	 * <p>
	 * After the two meta-data columns come the base table columns (in base
	 * table order). For a deleted row, they will all be null (except for the
	 * primary key that is needed to identify the row).
	 * 
	 * @param tableName
	 * @param revision
	 * @throws SQLException
	 */

	public Collection<Object[]> getUpdatedRows(String tableName, int revision)
			throws SQLException {
		getUpdatedRows++;
		String historyTable = getHistoryTableName(tableName);
		if (historyTable == null)
			throw SQLException(7, tableName);
		Integer rev = revision;

		SingleTableSelect s = new SingleTableSelect("*", historyTable)
				.addWhereClause(new WhereIn("oc$rev", rev));
		TableSchema ts = ads.getTableSchema(tableName);

		Connection conn = ads.getDataSource().getConnection();
		try {
			PreparedStatement sql = s.prepareStatement(conn);
			try {
				ResultSet rs = sql.executeQuery();
				Collection<Object[]> result = new ArrayList<Object[]>();
				try {
					while (rs.next()) {
						Object[] row = new Object[ts.getColumnCount() + 2];
						row[0] = rev;
						for (int i = 1; i < row.length; i++) {
							row[i] = rs.getObject(i + 1);
						}
						result.add(row);
					}
					return result;
				} finally {
					rs.close();
				}
			} finally {
				sql.close();
			}
		} finally {
			conn.close();
		}

	}

	/**
	 * Selects a row as it was at the time of the specified revision. The result
	 * array also includes (in its first two columns) the latest revision number
	 * that changed the row, and the delete flag (see the getUpdatedRows
	 * method).
	 * 
	 * <p>
	 * Does not check if the specified revision actually exists.
	 * 
	 * @param rev
	 *            can be null (for latest revision in the repository)
	 * @param tableName
	 * @param primaryKeyValues
	 * @return an array containing the base table column values and OC$REV and
	 *         OC$DEL
	 * @throws SQLException
	 */
	public Object[] selectRowRevision(Integer rev, String tableName,
			Object... primaryKeyValues) throws SQLException {
		selectRows++;
		String historyTable = getHistoryTableName(tableName);
		if (historyTable == null)
			throw SQLException(7, tableName);

		TableSchema ts = ads.getTableSchema(tableName);
		SingleTableSelect s = new SingleTableSelect("*", historyTable)
				.addWhereClause(new WhereInRange("oc$rev", null, rev))
				.setOrderBy("oc$rev", false);
		ts.addPrimaryKeyWhereClause(s, primaryKeyValues);

		Connection conn = ads.getDataSource().getConnection();
		try {
			PreparedStatement sql = s.prepareStatement(conn);
			sql.setMaxRows(1);
			try {
				ResultSet rs = sql.executeQuery();
				try {
					if (!rs.next())
						return null;
					Object[] row = new Object[ts.getColumnCount() + 2];
					for (int i = 0; i < row.length; i++) {
						row[i] = rs.getObject(i + 1);
					}
					return row;
				} finally {
					rs.close();
				}
			} finally {
				sql.close();
			}
		} finally {
			conn.close();
		}
	}

	/**
	 * Selects a row as it was at the time of the specified revision. The result
	 * array does not include the meta data (only base table columns).
	 * 
	 * <p>
	 * Does not check if the specified revision actually exists.
	 * 
	 * @param rev
	 *            can be null (for latest revision in the repository)
	 * @param tableName
	 * @param primaryKeyValues
	 * @return an array containing only the base table column values
	 * @throws SQLException
	 */

	public Object[] selectRow(Integer rev, String tableName,
			Object... primaryKeyValues) throws SQLException {
		if (rev == null)
			return ads.selectRow(tableName, primaryKeyValues);
		Object[] revRow = selectRowRevision(rev, tableName, primaryKeyValues);
		if (revRow == null)
			return null;
		// OC$DEL
		if (revRow[1] != null)
			return null;
		Object[] dataRow = new Object[revRow.length - 2];
		System.arraycopy(revRow, 2, dataRow, 0, dataRow.length);
		return dataRow;
	}

	private ScalarSelect selectColumn(Integer rev, String tableName,
			String columnName, Object... primaryKeyValues) throws SQLException {
		String historyTable = getHistoryTableName(tableName);
		if (historyTable == null)
			throw SQLException(7, tableName);

		TableSchema ts = ads.getTableSchema(tableName);
		ScalarSelect s = new ScalarSelect(columnName, historyTable)
				.addWhereClause(new WhereInRange("oc$rev", null, rev));
		s.setOrderBy("oc$rev", false);
		ts.addPrimaryKeyWhereClause(s, primaryKeyValues);
		return s;
	}

	public Integer selectInteger(Integer rev, String tableName,
			String columnName, Object... primaryKeyValues) throws SQLException {
		if (rev == null)
			return ads.selectInteger(tableName, columnName, primaryKeyValues);
		selectIntegers++;
		return selectColumn(rev, tableName, columnName, primaryKeyValues)
				.selectFirstRowInteger(ads.getDataSource());
	}

	public Object selectObject(Integer rev, String tableName,
			String columnName, Object... primaryKeyValues) throws SQLException {
		if (rev == null)
			return ads.selectObject(tableName, columnName, primaryKeyValues);
		selectObjects++;
		return selectColumn(rev, tableName, columnName, primaryKeyValues)
				.selectFirstRowObject(ads.getDataSource());

	}

	public String selectString(Integer rev, String tableName,
			String columnName, Object... primaryKeyValues) throws SQLException {
		if (rev == null)
			return ads.selectString(tableName, columnName, primaryKeyValues);
		selectStrings++;
		return selectColumn(rev, tableName, columnName, primaryKeyValues)
				.selectFirstRowString(ads.getDataSource());

	}

	/**
	 * create a select statement that creates a snapshot view of the table at a
	 * given revision.
	 * 
	 * <p>
	 * This is implemented as a sub-select against the history table.
	 * 
	 * @param rev
	 * @param tableName
	 * @return an SQL statement that can be used to select from the snapshot
	 * @throws SQLException
	 */

	public SelectStatement selectAtRevision(int rev, String tableName)
			throws SQLException {
		String historyTable = getHistoryTableName(tableName);
		if (historyTable == null)
			throw SQLException(7, tableName);
		TableSchema ts = ads.getTableSchema(tableName);
		String[] keys = new String[ts.getPrimaryKeyLength()];
		StringBuilder joinedKeys = new StringBuilder(10 * keys.length);
		for (int i = 0; i < keys.length; i++) {
			keys[i] = ts.getPrimaryKeyColumnName(i);
			joinedKeys.append(keys[i]);
			joinedKeys.append(',');
		}
		// strip trailing comma
		String joined = joinedKeys.substring(0, joinedKeys.length() - 1);

		StringBuilder sb = new StringBuilder(200 + 10 * ts.getColumnCount());
		sb.append("select ");
		for (int i = 0; i < ts.getColumnCount(); i++) {
			sb.append("a1.");
			sb.append(ts.getColumnName(i));
			sb.append(",");
		}
		// strip trailing comma
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" from ");
		sb.append(historyTable);
		sb.append(" a1 join (select ");
		sb.append(joined);
		sb.append(", max(oc$rev) as oc$maxrev from ");
		sb.append(historyTable);
		sb.append(" where oc$rev <= ? group by ");
		sb.append(joined);
		sb.append(") a2 on (");
		for (String k : keys) {
			sb.append("a1.");
			sb.append(k);
			sb.append("=a2.");
			sb.append(k);
			sb.append(" and ");
		}
		sb.append("a1.oc$rev = a2.oc$maxrev) where a1.oc$del is null");

		return new GenericSelect(sb.toString(), rev);

	}

	/**
	 * creates (in the database) a history table for an existing base table.
	 * 
	 * The history table contains all the columns of the base table, plus two
	 * leading meta-data-columns: OC$REV is the revision number in which the row
	 * was updated (or inserted or deleted), OC$DEL is a flag to mark deleted
	 * rows. The primary key of the history tables is the original primary key
	 * plus OC$REV.
	 * 
	 * @param tablename
	 * @param historyTableName
	 * @throws SQLException
	 */
	public void createHistoryTable(String tablename, String historyTableName)
			throws SQLException {
		// get the column names and primary key info for the original table
		TableSchema ts = ads.getTableSchema(tablename);
		StringBuilder sb = new StringBuilder(200);
		sb.append("( oc$rev integer not null, oc$del integer, ");
		{
			int cols = ts.getColumnCount();
			for (int i = 0; i < cols; i++) {
				String col = ts.getColumnName(i);
				sb.append(col);
				sb.append(" ");
				String tN = ts.getColumnTypeName(i);
				sb.append(tN);
				int tc = ts.getColumnType(i);
				// set column sizes for char types
				if (tc == Types.CHAR || tc == Types.VARCHAR) {
					Integer l = ts.getColumnSize(i);
					if (l != null && l > 0) {
						sb.append("(");
						sb.append(l);
						sb.append(")");
					}
				}
				if (ts.isNullableColumn(i) == Boolean.FALSE) {
					sb.append(" not null");
				}
				sb.append(",");
			}
		}
		sb.append("primary key (");
		{
			int cols = ts.getPrimaryKeyLength();
			for (int i = 0; i < cols; i++) {
				sb.append(ts.getPrimaryKeyColumnName(i));
				sb.append(",");
			}
		}
		sb.append("oc$rev), foreign key (oc$rev) references ");
		sb.append(revTablename);
		sb.append("(no) )");

		tablename = tablename.toUpperCase();
		historyTableName = historyTableName.toUpperCase();

		Connection conn = ads.getDataSource().getConnection();
		try {
			Statement sql = conn.createStatement();
			try {
				sql.execute("create table " + historyTableName + sb.toString());
				log.info("created history table " + historyTableName + " for "
						+ tablename);
			} finally {
				sql.close();
			}
		} finally {
			conn.close();
		}

	}

	// -------- performance statistics --------------
	public Map<String, Long> getStats() {
		Map<String, Long> stats = new TreeMap<String, Long>();

		stats.put("commits", commits);
		stats.put("startCommits", startCommits);
		stats.put("getRevisionNumber", getRevisionNumber);
		stats.put("getRevisionNumberTable", getRevisionNumberTable);
		stats.put("selectIntegers", selectIntegers);
		stats.put("selectStrings", selectStrings);
		stats.put("selectObjects", selectObjects);
		stats.put("selectRows", selectRows);
		stats.put("getUpdatedRows", getUpdatedRows);

		return stats;
	}

}

