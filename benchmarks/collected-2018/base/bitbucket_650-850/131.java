// https://searchcode.com/api/result/55772696/

package org.openmrs.module.peersync.service.db;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dtree.openmrs.android.peersync.LocalUnitIdentifier;
import org.dtree.openmrs.db.adaptor.CursorAdaptor;
import org.dtree.openmrs.db.adaptor.DbAdaptor;
import org.dtree.openmrs.db.adaptor.ObjectValues;
import org.dtree.openmrs.db.sqlite.readers.CursorReader;
import org.dtree.openmrs.db.sqlite.readers.ObjectReader;
import org.dtree.openmrs.db.sqlite.schema.Table;
import org.dtree.openmrs.db.sqlite.schema.fields.sync.PeerSyncRecordField;
import org.dtree.openmrs.db.sqlite.schema.fields.sync.PeerSyncRecordLogField;
import org.dtree.openmrs.db.sqlite.schema.fields.sync.PeerSyncSettingsField;
import org.dtree.openmrs.db.sqlite.schema.fields.sync.PeerSyncUnitField;
import org.openmrs.Person;
import org.openmrs.module.peersync.PeerSyncException;
import org.openmrs.module.peersync.objects.PeerSyncConstants;
import org.openmrs.module.peersync.objects.PeerSyncJournalItem;
import org.openmrs.module.peersync.objects.PeerSyncRecord;
import org.openmrs.module.peersync.objects.PeerSyncRecordLog;
import org.openmrs.module.peersync.objects.PeerSyncSettings;
import org.openmrs.module.peersync.objects.PeerSyncState;
import org.openmrs.module.peersync.objects.PeerSyncUnit;
import org.openmrs.module.peersync.serialize.PeerSyncSerializer;
import org.openmrs.module.peersync.service.TimestampNormalizer;

public class PeerSyncQueries {

	private final static int BATCH_LIMIT = 100;// TODO: if performance issues
	// observed on phone, reduce
	// this number

	private final static String GET_ALL = "SELECT * FROM "
			+ Table.PEER_SYNC_RECORD.name()
			+ " ORDER BY date_created ASC, peer_sync_record_id ASC";

	private final static String GET_ALL_SYNCS = "SELECT * FROM "
			+ Table.PEER_SYNC_UNIT.name() + " ORDER BY date_created DESC";

	private final static String GET_BY_UUID = "SELECT * FROM "
			+ Table.PEER_SYNC_RECORD.name() + " WHERE UUID = ?";

	private final static String GET_BATCH_SINCE = "SELECT * FROM "
			+ Table.PEER_SYNC_RECORD.name()
			+ " WHERE date_created > DATEX "// TODO
			+ " ORDER BY date_created ASC, peer_sync_record_id ASC "
			+ " LIMIT " + BATCH_LIMIT;
	private final static String GET_TX_UUIDS_IN_SYNC = "(SELECT TRANSACTION_UUID FROM "
			+ Table.PEER_SYNC_RECORD_LOG.name() + " WHERE SYNC_UUID = ? )";

	private final static String GET_TXLOG = "SELECT * FROM "
			+ Table.PEER_SYNC_RECORD_LOG.name() + " WHERE transaction_uuid = ?";

	// TODO
	// DATEX
	// is
	// substituted
	private final static String GET_TX_RECORDS_LOG_SINCE = "("
			+ Table.PEER_SYNC_RECORD_LOG.name() + ".date_created > DATEX AND "
			+ " transaction_uuid NOT IN " + GET_TX_UUIDS_IN_SYNC + ")";

	// SELECT * FROM PEER_SYNC_RECORD, PEER_SYNC_RECORD_LOG
	// WHERE
	// (PEER_SYNC_RECORD.UUID = PEER_SYNC_RECORD_LOG.transaction_uuid)
	// AND
	// ( PEER_SYNC_RECORD_LOG.date_created > '1980-01-01 00:00:00' AND
	// transaction_uuid NOT IN (SELECT TRANSACTION_UUID FROM
	// PEER_SYNC_RECORD_LOG WHERE SYNC_UUID = ? ))
	// ORDER BY PEER_SYNC_RECORD_LOG.date_created ASC LIMIT 100
	private final static String GET_NON_SYNC_TX_SINCE = "SELECT * FROM "
			+ Table.PEER_SYNC_RECORD_LOG.name()
			+ ", "// IMPORTANT date_created needs to come from RECORD not LOG
			+ Table.PEER_SYNC_RECORD.name() + " WHERE "
			+ "("
			+ Table.PEER_SYNC_RECORD.name()
			+ ".UUID = "
			+ Table.PEER_SYNC_RECORD_LOG.name()
			+ ".transaction_uuid) AND "
			// sorting by date not sufficient since order not retained if more
			// than 1
			+ GET_TX_RECORDS_LOG_SINCE + "  ORDER BY "
			+ Table.PEER_SYNC_RECORD.name()
			+ ".date_created ASC, peer_sync_record_id ASC LIMIT " + BATCH_LIMIT;

	private final static String GET_CHANGES_SINCE = "SELECT * FROM "
			+ Table.PEER_SYNC_RECORD.name() + " WHERE date_created > ? "
			+ " ORDER BY date_created ASC ";

	private final static String GET_SYNCS_BY_UNIT = "SELECT * FROM "
			+ Table.PEER_SYNC_UNIT.name() + " WHERE unit_identifier = ? "
			+ " ORDER BY date_created DESC ";
	private final static String DELETE_SYNCS_FOR_UNIT = "DELETE FROM "
			+ Table.PEER_SYNC_UNIT.name() + " WHERE unit_identifier = '?' ";

	private final static String GET_SETTINGS = "SELECT * " + " FROM "
			+ Table.PEER_SYNC_SETTINGS.name();

	private DbAdaptor db;

	private PeerSyncSerializer xstream = new PeerSyncSerializer();

	private final Log log = LogFactory.getLog(getClass());

	public PeerSyncQueries(DbAdaptor adaptor) {
		this.db = adaptor;

	}

	/**
	 * 
	 * 
	 * Given the record of a transaction passed from a remote unit, execute it
	 * also locally
	 * 
	 * @param txRecord
	 *            Record of the transaction to be executed
	 * @param syncUuid
	 *            Unique identifier of the synchronization within which this
	 *            transaction was received
	 * @param unitIdentifier
	 */
	public void executeTransactionRecord(PeerSyncRecord txRecord,
			String syncUuid, String unitIdentifier) {
		executeTransactionRecord(txRecord, syncUuid, unitIdentifier, false);
	}

	public void executeTransactionRecord(PeerSyncRecord txRecord) {
		executeTransactionRecord(txRecord, null, null, true);
	}

	private void executeTransactionRecord(PeerSyncRecord txRecord,
			String syncUuid, String unitIdentifier, boolean initDB) {
		long t1 = System.currentTimeMillis();
		// a transaction record comprises a list of item
		String xml = txRecord.getXml();

		try {
			ObjectInputStream in = xstream.getInputStream(xml);
			int itemCount = in.readInt();

			log.info("executing tx record item count = " + itemCount);

			for (int i = 0; i < itemCount; i++) {
				PeerSyncJournalItem item = (PeerSyncJournalItem) in
						.readObject();
				try {
					executeJournalItem(item);
				} catch (Exception e) {

					ObjectValues values = (ObjectValues) xstream
							.deserializeObjectValues(item.getXml());

					log.error("START ITEM XML");
					for (String k : values.keySet()) {
						log.error("key: " + k + " value: " + values.get(k));
					}
					log.error("END ITEM XML");
					throw new PeerSyncException(e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new PeerSyncException(e);
		}

		if (!initDB) {
			// record the transaction has been executed
			txRecord.setPeerSyncRecordId(null);// must eliminate primary key
			// value
			// since this is auto_increment (not
			// needed?)
			db.insert(txRecord);

			// log the receipt of the transaction
			PeerSyncRecordLog txRecordLog = new PeerSyncRecordLog(txRecord
					.getUuid(), syncUuid, unitIdentifier);

			db.insert(txRecordLog);
			long t2 = System.currentTimeMillis();
			log.info("SYNC executed TX: " + (((t2 - t1) + 1) / 1000) + " "
					+ txRecord.getUuid());
		}
	}

	/**
	 * Execute an item within a transaction - a NEW, DELETE or UPDATE operation
	 * on the db
	 * 
	 * @param item
	 * 
	 * @see ObjectReader.readUuidsMap
	 */
	private void executeJournalItem(PeerSyncJournalItem item) {
		long t1 = System.currentTimeMillis();

		ObjectValues values = (ObjectValues) xstream
				.deserializeObjectValues(item.getXml());
		if (values == null)
			throw new PeerSyncException(
					"null object values following deserialization");
		ObjectValues uuidsMap = xstream
				.deserializeObjectValues(item.getUuids());

		long t2 = System.currentTimeMillis();

		String className = item.getType();
		Table table = Table.getTableForClass(className);
		log.info("exec item: " + item.getState() + " " + table.name());
		if (item.getState().equals(PeerSyncState.NEW.name())) {
			// fix local ids
			ObjectValues newValues = fixLocalIds(table, values, uuidsMap);
			/**
			 * 
			 * special case, if creating patient, id needs to be same as
			 * person_id which has same uuid
			 * 
			 * 
			 * 
			 */
			if (table.equals(Table.PATIENT))
				newValues = specialPatientIdFix(table, newValues);
			db.insert(table, newValues);
		} else if (item.getState().equals(PeerSyncState.DELETE.name())) {
			// String uuid = getUuidFromValues(values);
			// ObjectValues newValues = fixLocalIds(table, values, uuidsMap);
			// now also fix the idField
			if (Table.hasUuidField(table)) {
				Class javaType = table.getObjectClass();
				String uuid = values.get("UUID");
				// Object o = db.getByUuid(javaType, uuid);
				// Integer localId = ((OpenmrsObject) o).getId();
				Integer localId = db.getLocalIdForUuid(javaType, uuid);
				values.put(table.getIdField().name(), localId.toString());
			}
			Integer id = getIdFromValues(table.getIdField().name(), values);

			db.delete(table, id);
		} else if (item.getState().equals(PeerSyncState.UPDATE.name())) {
			log.info("UPDATE fixing local ids for update " + item.getUuid());
			ObjectValues newValues = fixLocalIds(table, values, uuidsMap);
			// now also fix the idField
			if (Table.hasUuidField(table)) {
				Class javaType = table.getObjectClass();
				String uuid = newValues.get("UUID");
				if (uuid == null)
					throw new PeerSyncException("no uuid in values: "
							+ table.name());
				Integer localId = db.getLocalIdForUuid(javaType, uuid);
				if (localId == null)
					throw new PeerSyncException("no uuid -> localId: "
							+ table.name() + " " + uuid);
				newValues.put(table.getIdField().name(), localId.toString());
			}
			log.info("UPDATE done fixing local ids for update "
					+ item.getUuid());
			Integer id = getIdFromValues(table.getIdField().name(), newValues);
			log.info("UPDATE Going to update id=" + id + " " + item.getUuid());
			db.update(table, newValues, id);
			log.info("UPDATE Done update " + item.getUuid());
		}
		long t3 = System.currentTimeMillis();
		log.info("Executed Journal Item: desz=" + (t2 - t1) + " tx: "
				+ (t3 - t2));

	}

	private Integer getIdFromValues(String idFieldName, ObjectValues values) {
		String idStr = (String) values.get(idFieldName);
		return new Integer(Integer.parseInt(idStr));
	}

	/**
	 * 
	 * patient_id has exceptional, non-independent relationship with person_id
	 * which must be maintained
	 * 
	 * 
	 * @param table
	 * @param values
	 * @return
	 */
	private ObjectValues specialPatientIdFix(Table table, ObjectValues values) {
		if (values == null)
			throw new PeerSyncException("null values parameter");
		ObjectValues newValues = values;
		String patientUuid = (String) values.get("UUID");
		Person person = (Person) db.getByUuid(Person.class, patientUuid);
		if (person == null)
			throw new PeerSyncException(
					"null Person from patientUuid when executing the specialPatientIdFix");
		newValues.put(table.getIdField().name(), person.getPersonId()
				.toString());
		return newValues;
	}

	/**
	 * 
	 * OpenMRS uses uuids and "local ids" to identify objects
	 * 
	 * Uuids are cross server, and are maintained during syncing local ids are
	 * integer identifiers for efficiency and are different between servers
	 * 
	 * So a transaction having person_id = 4 coming from a remote server must
	 * have that translated into the right id for this server
	 * 
	 * @param table
	 * @param values
	 * @param uuidsMap
	 * @return
	 */
	private ObjectValues fixLocalIds(Table table, ObjectValues values,
			HashMap<String, String> uuidsMap) {
		if (values == null)
			throw new PeerSyncException("null values parameter");

		ObjectValues newValues = values;

		Set<String> keys = uuidsMap.keySet();

		// for each field for which we have a uuid
		for (String fieldName : keys) {

			String uuid = uuidsMap.get(fieldName);
			log.info("UUID MAP : " + fieldName + "  " + uuid);
			if (uuid == null)
				throw new PeerSyncException("UUID cannot be null for "
						+ fieldName + " in uuidsMap");
			// which table to look in for the local id?
			// e.g. PERSON_ID in PERSON_NAME
			// find which table has this field name as the id field
			// For CREATOR field, we need to look in the USER table, so we need
			// the javatype
			Class javaType = Table.getSourceTableForField(table, fieldName);
			// now find row in this table with this uuid
			// does this table have a UUID field (TODO: check with OpenMRS
			// people which tables do)
			if (hasUuid(javaType)) {
				// TODO: is it ok to check whether UUID is a field in values?
				// will uuid ever be null if it exists?
				Integer localId = db.getLocalIdForUuid(javaType, uuid);

				// TODO: when would this be the case, if ever?
				// if new databases are seeded with initial Users with different
				// uuids (both id=1)
				if (localId != null) {
					String old = newValues.get(fieldName);

					newValues.put(fieldName, localId.toString());
					log.info("localid fixed: " + javaType + " from " + old
							+ " to " + localId);
				} else {
					// if (!Table.isSeededManually(table))
					// throw new DAOException(javaType
					// + " with uuid not found: " + uuid);
				}
			} else {
				log.info(javaType + " has no uuid");
			}
		}

		return newValues;
	}

	private boolean hasUuid(Class objectClass) {
		Table t = Table.getTableForClass(objectClass.getName());
		return Table.hasUuidField(t);

	}

	/**
	 * 
	 * Get a list of all transaction uuids, order by date_created (ascending)
	 * 
	 * @return
	 */
	public List<String> getAllTransactionUuids() {
		List<String> uuids = new ArrayList<String>();

		CursorAdaptor cursor = null;
		try {

			cursor = db.rawQuery(GET_ALL, new String[] {});
			int i = cursor.getColumnIndex("UUID");
			while (cursor.moveToNext()) {
				String uuid = cursor.getString(i);
				uuids.add(uuid);
			}
			cursor.close();
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return uuids;

	}

	public PeerSyncRecordLog getTxLog(String txUuid) {

		CursorAdaptor cursor = null;
		try {

			cursor = db.rawQuery(GET_TXLOG, new String[] { txUuid });

			if (cursor.moveToNext()) {
				CursorReader reader = new CursorReader();
				PeerSyncRecordLog txLog = new PeerSyncRecordLog();
				reader.readCursor(txLog, cursor, PeerSyncRecordLogField.class);
				return txLog;
			}
			cursor.close();
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	/**
	 * 
	 * Returns a transaction identified by uuid, or null if none such found
	 * 
	 * 
	 * @param uuid
	 * @return
	 */
	public PeerSyncRecord getTransactionByUUid(String uuid) {
		CursorAdaptor cursor = null;
		try {
			cursor = db.rawQuery(GET_BY_UUID, new String[] { uuid });
			if (cursor.getCount() == 1) {
				CursorReader reader = new CursorReader();
				cursor.moveToNext();
				PeerSyncRecord transaction = new PeerSyncRecord();
				reader.readCursor(transaction, cursor,
						PeerSyncRecordField.class);
				return transaction;
			}
			if (cursor.getCount() == 0) {
				return null;
			}
			throw new PeerSyncException("Duplicate transactions! " + uuid);

		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/**
	 * 
	 * Computes, for each relevant table, 3 integers: the number of records,
	 * number of hashes, and a checksum
	 * 
	 * @return
	 */
	public HashMap<String, Integer[]> getConfirmData() {
		HashMap<String, Integer[]> confirmData = new HashMap<String, Integer[]>();
		Table[] tables = Table.values();
		for (Table table : tables) {
			if (table.isDynamic()) {

				Integer[] data = getDataForTable(table);
				// if the hashcount for a table is not same as record count,
				// data is
				// no good
				if (data[PeerSyncConstants.DATA_HASHCOUNT].intValue() != data[PeerSyncConstants.DATA_RECORDCOUNT]
						.intValue()) {
					log.error("SYNC" + " " + db.getDatabaseName()
							+ " no good for synchronizing - table "
							+ table.name()
							+ " hashcode count not equal record count");
				}
				confirmData.put(table.name(), data);
			}

		}
		return confirmData;
	}

	private Integer[] getDataForTable(Table table) {
		Integer[] data = getHashSumForTable(table);
		data[PeerSyncConstants.DATA_RECORDCOUNT] = getRecordCount(table);
		return data;
	}

	private Integer[] getHashSumForTable(Table table) {
		Integer count = -1, sum = -1;
		CursorAdaptor cursor = null;
		try {
			cursor = db.rawQuery(
					"SELECT sum(hashcode) as sum, count(hashcode) as count FROM "
							+ Table.PEER_SYNC_HASH.name()
							+ " WHERE tablename = ?", new String[] { table
							.name() });
			if (cursor.moveToNext()) {
				sum = cursor.getInt(cursor.getColumnIndex("sum"));
				count = cursor.getInt(cursor.getColumnIndex("count"));
			}
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		Integer[] data = new Integer[3];
		data[PeerSyncConstants.DATA_HASHCOUNT] = count;
		data[PeerSyncConstants.DATA_HASHSUM] = sum;
		return data;
	}

	private Integer getRecordCount(Table table) {
		Integer count = -1;
		CursorAdaptor cursor = null;
		try {
			// if (table.equals(Table.PERSON)) {
			// cursor = db.rawQuery(PersonsQueries.GET_ALL, new String[] {});
			// return cursor.getCount();
			// } else {
			cursor = db.rawQuery("SELECT count(*) as count FROM "
					+ table.name(), new String[] {});
			if (cursor.moveToNext()) {
				count = cursor.getInt(cursor.getColumnIndex("count"));
			}
			// }
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return count;
	}

	/**
	 * 
	 * Handshake data refers to three strings:
	 * <ul>
	 * <li>Uuid of first transaction
	 * <li>Uuid of most recent transaction
	 * <li>Count of transactions
	 * </ul>
	 * 
	 * 
	 * 
	 * @return
	 */
	public String[] getHandshakeData() {

		List<PeerSyncRecord> txs = getAllTransactions();

		String first = txs.get(0).getUuid();
		String last = txs.get(txs.size() - 1).getUuid();

		String count = Integer.toString(txs.size());
		return new String[] { first, last, count };
	}

	/**
	 * 
	 * Get the records of successful syncs with specified unitIdentifier,
	 * date_created DESC
	 * 
	 * @param unitIdentifier
	 * @return
	 */
	public List<PeerSyncUnit> getSyncsByUnit(String unitIdentifier) {
		List<PeerSyncUnit> items = new ArrayList<PeerSyncUnit>();
		CursorAdaptor cursor = null;
		try {
			cursor = db.rawQuery(GET_SYNCS_BY_UNIT,
					new String[] { unitIdentifier });
			CursorReader reader = new CursorReader();
			while (cursor.moveToNext()) {
				PeerSyncUnit sync = new PeerSyncUnit();
				reader.readCursor(sync, cursor, PeerSyncUnitField.class);
				items.add(sync);
			}
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return items;
	}

	/**
	 * Given the identifier of a synchronization and a date, return all
	 * transaction records since that date which are not logged to have been
	 * received during the specified transaction
	 * 
	 * TODO: get from RecordLog rather than Record
	 * 
	 * 
	 * @param date
	 * @param syncUuid
	 * @return
	 */
	public List<PeerSyncRecord> getNonSyncTransactionsSince(Date date,
			String syncUuid) {
		List<PeerSyncRecord> items = new ArrayList<PeerSyncRecord>();
		// get all journal items since last sync
		String lastSynchronizedStr = new TimestampNormalizer().toString(date);
		CursorAdaptor cursor = null;
		try {
			// TODO: fix this - couldn't get query to work with dateString
			// substitution
			String sql = (GET_NON_SYNC_TX_SINCE.replace("DATEX", "'"
					+ lastSynchronizedStr + "'"));

			cursor = db.rawQuery(sql, new String[] { syncUuid });
			CursorReader reader = new CursorReader();
			while (cursor.moveToNext()) {
				PeerSyncRecord sync = new PeerSyncRecord();
				reader.readCursor(sync, cursor, PeerSyncRecordField.class);
				// log.info("Tx date created: "+sync.getDateCreated().toGMTString());
				items.add(sync);
			}
			cursor.close();
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return items;

	}

	/**
	 * 
	 * Get all transactions since specified date. Has a batch mode - if batch
	 * parameter is true, then size of list returned will be limited.
	 * 
	 * 
	 * @param date
	 * @param batch
	 * @return
	 */
	public List<PeerSyncRecord> getTransactionsSince(Date date, boolean batch) {
		List<PeerSyncRecord> items = new ArrayList<PeerSyncRecord>();
		// get all journal items since last sync
		String lastSynchronizedStr = new TimestampNormalizer().toString(date);
		CursorAdaptor cursor = null;
		try {
			// TODO: fix this - couldn't get query to work with dateString
			// substitution
			String sql = (batch ? GET_BATCH_SINCE.replace("DATEX", "'"
					+ lastSynchronizedStr + "'") : GET_CHANGES_SINCE);

			cursor = db.rawQuery(sql, new String[] {});
			CursorReader reader = new CursorReader();
			while (cursor.moveToNext()) {
				PeerSyncRecord sync = new PeerSyncRecord();
				reader.readCursor(sync, cursor, PeerSyncRecordField.class);
				items.add(sync);
			}
			cursor.close();
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return items;

	}

	public List<Integer> getAllHashcodes() {
		List<Integer> hashcodes = new ArrayList<Integer>();
		CursorAdaptor cursor = db.rawQuery(GET_ALL, new String[] {});
		while (cursor.moveToNext()) {
			String xml = cursor.getString(cursor
					.getColumnIndex(PeerSyncRecordField.XML.name()));
			hashcodes.add(new Integer(xml.hashCode()));
		}
		cursor.close();
		return hashcodes;
	}

	public List<PeerSyncRecord> getAllTransactions() {
		List<PeerSyncRecord> items = new ArrayList<PeerSyncRecord>();
		CursorAdaptor cursor = null;
		try {
			cursor = db.rawQuery(GET_ALL, new String[] {});
			CursorReader reader = new CursorReader();
			while (cursor.moveToNext()) {
				PeerSyncRecord sync = new PeerSyncRecord();
				reader.readCursor(sync, cursor, PeerSyncRecordField.class);
				items.add(sync);
			}
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			cursor.close();
		}

		return items;

	}

	public List<PeerSyncUnit> getAllSyncs() {
		List<PeerSyncUnit> items = new ArrayList<PeerSyncUnit>();
		CursorAdaptor cursor = null;
		try {
			cursor = db.rawQuery(GET_ALL_SYNCS, new String[] {});
			CursorReader reader = new CursorReader();
			while (cursor.moveToNext()) {
				PeerSyncUnit sync = new PeerSyncUnit();
				reader.readCursor(sync, cursor, PeerSyncUnitField.class);
				items.add(sync);
			}
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			cursor.close();
		}

		return items;

	}

	public void recordLastSync(String unitIdentifier) {
		// clear - should always be max 1 sync for any unit
		deleteSyncsForUnit(unitIdentifier);

		PeerSyncUnit unit = new PeerSyncUnit();
		unit.setDateCreated(new Date());
		unit.setUnitIdentifier(unitIdentifier);
		db.insert(unit);

	}

	private void deleteSyncsForUnit(String unitIdentifier) {
		String sql = DELETE_SYNCS_FOR_UNIT.replace("?", unitIdentifier);
		db.execSQL(sql);

	}

	public void setUnitIdentifier(String id) {
		PeerSyncSettings settings = getSyncSettings();
		settings.setUnitIdentifier(id);
		db.update(settings);
	}

	public PeerSyncSettings getSyncSettings() {
		PeerSyncSettings settings = new PeerSyncSettings();
		settings.setPeerSyncSettingsId(1);
		settings.setUnitIdentifier("default identifier");
		CursorAdaptor cursor = null;
		try {
			cursor = db.rawQuery(GET_SETTINGS, new String[] {});
			CursorReader reader = new CursorReader();
			if (cursor.moveToNext()) {
				reader
						.readCursor(settings, cursor,
								PeerSyncSettingsField.class);
			} else {
				// must be one settings row
				db.insert(settings);

			}
		} catch (Exception e) {
			throw new PeerSyncException(e);
		} finally {
			cursor.close();
		}
		return settings;
	}

}

