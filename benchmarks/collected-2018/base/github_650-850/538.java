// https://searchcode.com/api/result/70130134/

/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *      Sleepycat Software.  All rights reserved.
 *
 * $Id: EnvironmentParams.java,v 1.1.6.3.2.10 2006/10/25 20:29:32 ckaestne Exp $
 */

package com.sleepycat.je.config;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Javadoc for this public class is generated via the doc templates in the
 * doc_src directory.
 */
public class EnvironmentParams {

	/*
	 * The map of supported environment parameters where the key is parameter
	 * name and the data is the configuration parameter object. Put first,
	 * before any declarations of ConfigParams.
	 * 
	 * field is not final because of startup dependencies. do not access directly
	 */
	private static Map SUPPORTED_PARAMS;

	/*
	 * Environment
	 */
	public static final LongConfigParam MAX_MEMORY = new LongConfigParam(
			"je.maxMemory",
			null, // min
			null, // max
			new Long(0), // default uses je.maxMemoryPercent
			true, // mutable
			"# Specify the cache size in bytes, as an absolute number. The system\n"
					+ "# attempts to stay within this budget and will evict database\n"
					+ "# objects when it comes within a prescribed margin of the limit.\n"
					+ "# By default, this parameter is 0 and JE instead sizes the cache\n"
					+ "# proportionally to the memory available to the JVM, based on\n"
					+ "# je.maxMemoryPercent.");

	public static final IntConfigParam MAX_MEMORY_PERCENT = new IntConfigParam(
			"je.maxMemoryPercent",
			new Integer(1), // min
			new Integer(90), // max
			new Integer(60), // default
			true, // mutable
			"# By default, JE sizes the cache as a percentage of the maximum\n"
					+ "# memory available to the JVM. For example, if the JVM is\n"
					+ "# started with -Xmx128M, the cache size will be\n"
					+ "#           (je.maxMemoryPercent * 128M) / 100\n"
					+ "# Setting je.maxMemory to an non-zero value will override\n"
					+ "# je.maxMemoryPercent");

	public static final BooleanConfigParam ENV_RECOVERY = new BooleanConfigParam(
			"je.env.recovery", true, // default
			false,// mutable
			"# If true, an environment is created with recovery and the related\n"
					+ "# daemons threads enabled.");

	public static final BooleanConfigParam ENV_RECOVERY_FORCE_CHECKPOINT = new BooleanConfigParam(
			"je.env.recoveryForceCheckpoint", false, // default
			false,// mutable
			"# If true, a checkpoint is forced following recovery, even if the\n"
					+ "# log ends with a checkpoint.");

	public static final BooleanConfigParam ENV_RUN_INCOMPRESSOR = new BooleanConfigParam(
			"je.env.runINCompressor", true, // default
			true, // mutable
			"# If true, starts up the INCompressor.\n"
					+ "# This parameter is true by default");



	public static final BooleanConfigParam ENV_RUN_CLEANER = new BooleanConfigParam(
			"je.env.runCleaner", true, // default
			true, // mutable
			"# If true, starts up the cleaner.\n"
					+ "# This parameter is true by default");



	public static final BooleanConfigParam ENV_FORCED_YIELD = new BooleanConfigParam(
			"je.env.forcedYield", false, // default
			false,// mutable
			"# Debugging support: call Thread.yield() at strategic points.");


	public static final BooleanConfigParam ENV_INIT_LOCKING = new BooleanConfigParam(
			"je.env.isLocking", true, // default
			false,// mutable
			"# If true, create the environment with locking.");

	public static final BooleanConfigParam ENV_RDONLY = new BooleanConfigParam(
			"je.env.isReadOnly", false, // default
			false, // mutable
			"# If true, create the environment read only.");


	/*
	 * Database Logs
	 */
	/* default: 2k * NUM_LOG_BUFFERS */
	public static final int MIN_LOG_BUFFER_SIZE = 2048;

	private static final int NUM_LOG_BUFFERS_DEFAULT = 3;

	public static final long LOG_MEM_SIZE_MIN = NUM_LOG_BUFFERS_DEFAULT
			* MIN_LOG_BUFFER_SIZE;

	public static final String LOG_MEM_SIZE_MIN_STRING = Long
			.toString(LOG_MEM_SIZE_MIN);

	public static final LongConfigParam LOG_MEM_SIZE = new LongConfigParam(
			"je.log.totalBufferBytes", new Long(LOG_MEM_SIZE_MIN),// min
			null, // max
			new Long(0), // by default computed
			// from je.maxMemory
			false, // mutable
			"# The total memory taken by log buffers, in bytes. If 0, use\n"
					+ "# 7% of je.maxMemory");

	public static final IntConfigParam NUM_LOG_BUFFERS = new IntConfigParam(
			"je.log.numBuffers", new Integer(2), // min
			null, // max
			new Integer(NUM_LOG_BUFFERS_DEFAULT), // default
			false, // mutable
			"# The number of JE log buffers");

	public static final IntConfigParam LOG_BUFFER_MAX_SIZE = new IntConfigParam(
			"je.log.bufferSize", new Integer(1 << 10), // min
			null, // max
			new Integer(1 << 20), // default
			false, // mutable
			"# maximum starting size of a JE log buffer");

	public static final IntConfigParam LOG_FAULT_READ_SIZE = new IntConfigParam(
			"je.log.faultReadSize", new Integer(32), // min
			null, // max
			new Integer(2048), // default
			false, // mutable
			"# The buffer size for faulting in objects from disk, in bytes.");

	public static final IntConfigParam LOG_ITERATOR_READ_SIZE = new IntConfigParam(
			"je.log.iteratorReadSize",
			new Integer(128), // min
			null, // max
			new Integer(8192), // default
			false, // mutable
			"# The read buffer size for log iterators, which are used when\n"
					+ "# scanning the log during activities like log cleaning and\n"
					+ "# environment open, in bytes. This may grow as the system encounters\n"
					+ "# larger log entries");

	public static final IntConfigParam LOG_ITERATOR_MAX_SIZE = new IntConfigParam(
			"je.log.iteratorMaxSize",
			new Integer(128), // min
			null, // max
			new Integer(16777216), // default
			false, // mutable
			"# The maximum read buffer size for log iterators, which are used\n"
					+ "# when scanning the log during activities like log cleaning\n"
					+ "# and environment open, in bytes.");

	public static final LongConfigParam LOG_FILE_MAX = new LongConfigParam(
			"je.log.fileMax", new Long(1000000), // min
			new Long(4294967296L), // max
			new Long(10000000), // default
			false, // mutable
			"# The maximum size of each individual JE log file, in bytes.");


	public static final BooleanConfigParam LOG_MEMORY_ONLY = new BooleanConfigParam(
			"je.log.memOnly",
			false, // default
			false, // mutable
			"# If true, operates in an in-memory fashion without\n"
					+ "# flushing the log to disk. The system operates until it runs\n"
					+ "# out of memory, in which case a java.lang.OutOfMemory error is\n"
					+ "# thrown.");


	public static final LongConfigParam LOG_FSYNC_TIMEOUT = new LongConfigParam(
			"je.log.fsyncTimeout", new Long(10000L), // min
			null, // max
			new Long(500000L), // default
			false, // mutable
			"# Timeout limit for group file sync, in microseconds.");



	/*
	 * Tree
	 */
	public static final IntConfigParam NODE_MAX = new IntConfigParam(
			"je.nodeMaxEntries",
			new Integer(4), // min
			new Integer(32767), // max
			new Integer(128), // default
			false, // mutable
			"# The maximum number of entries in an internal btree node.\n"
					+ "# This can be set per-database using the DatabaseConfig object.");

	public static final IntConfigParam NODE_MAX_DUPTREE = new IntConfigParam(
			"je.nodeDupTreeMaxEntries",
			new Integer(4), // min
			new Integer(32767), // max
			new Integer(128), // default
			false, // mutable
			"# The maximum number of entries in an internal dup btree node.\n"
					+ "# This can be set per-database using the DatabaseConfig object.");

	public static final IntConfigParam BIN_MAX_DELTAS = new IntConfigParam(
			"je.tree.maxDelta", new Integer(0), // min
			new Integer(100), // max
			new Integer(10), // default
			false, // mutable
			"# After this many deltas, logs a full version.");

	public static final IntConfigParam BIN_DELTA_PERCENT = new IntConfigParam(
			"je.tree.binDelta", new Integer(0), // min
			new Integer(75), // max
			new Integer(25), // default
			false, // mutable
			"# If less than this percentage of entries are changed on a BIN,\n"
					+ "# logs a delta instead of a full version.");



	/*
	 * Evictor
	 */
	public static final LongConfigParam EVICTOR_EVICT_BYTES = new LongConfigParam(
			"je.evictor.evictBytes",
			new Long(1024), // min
			null, // max
			new Long(524288), // default
			false, // mutable
			"# When eviction happens, the evictor will push memory usage to this\n"
					+ "# number of bytes below je.maxMemory.  The default is 512KB and the\n"
					+ "# minimum is 1 KB (1024).");

	/* @deprecated As of 2.0, this is replaced by je.evictor.evictBytes */
	public static final IntConfigParam EVICTOR_USEMEM_FLOOR = new IntConfigParam(
			"je.evictor.useMemoryFloor", new Integer(50), // min
			new Integer(100), // max
			new Integer(95), // default
			false, // mutable
			"# When eviction happens, the evictor will push memory usage to this\n"
					+ "# percentage of je.maxMemory."
					+ "# (deprecated in favor of je.evictor.evictBytes");

	/* @deprecated As of 1.7.2, this is replaced by je.evictor.nodesPerScan */
	public static final IntConfigParam EVICTOR_NODE_SCAN_PERCENTAGE = new IntConfigParam(
			"je.evictor.nodeScanPercentage", new Integer(1), // min
			new Integer(100), // max
			new Integer(10), // default
			false, // mutable
			"# The evictor percentage of total nodes to scan per wakeup.\n"
					+ "# (deprecated in favor of je.evictor.nodesPerScan");

	/* @deprecated As of 1.7.2, 1 node is chosen per scan. */
	public static final IntConfigParam EVICTOR_EVICTION_BATCH_PERCENTAGE = new IntConfigParam(
			"je.evictor.evictionBatchPercentage", new Integer(1), // min
			new Integer(100), // max
			new Integer(10), // default
			false, // mutable
			"# The evictor percentage of scanned nodes to evict per wakeup.\n"
					+ "# (deprecated)");

	public static final IntConfigParam EVICTOR_NODES_PER_SCAN = new IntConfigParam(
			"je.evictor.nodesPerScan", new Integer(1), // min
			new Integer(1000), // max
			new Integer(10), // default
			false, // mutable
			"# The number of nodes in one evictor scan");


	public static final IntConfigParam EVICTOR_RETRY = new IntConfigParam(
			"je.evictor.deadlockRetry", new Integer(0), // min
			new Integer(Integer.MAX_VALUE),// max
			new Integer(3), // default
			false, // mutable
			"# The number of times to retry the evictor if it runs into a deadlock.");

	public static final BooleanConfigParam EVICTOR_LRU_ONLY = new BooleanConfigParam(
			"je.evictor.lruOnly",
			true, // default
			false,// mutable
			"# If true (the default), use an LRU-only policy to select nodes for\n"
					+ "# eviction.  If false, select by Btree level first, and then by LRU.");



	/*
	 * Cleaner
	 */
	public static final IntConfigParam CLEANER_MIN_UTILIZATION = new IntConfigParam(
			"je.cleaner.minUtilization", new Integer(0), // min
			new Integer(90), // max
			new Integer(50), // default
			true, // mutable
			"# The cleaner will keep the total disk space utilization percentage\n"
					+ "# above this value. The default is set to 50 percent.");

	public static final IntConfigParam CLEANER_MIN_FILE_UTILIZATION = new IntConfigParam(
			"je.cleaner.minFileUtilization",
			new Integer(0), // min
			new Integer(50), // max
			new Integer(5), // default
			true, // mutable
			"# A log file will be cleaned if its utilization percentage is below\n"
					+ "# this value, irrespective of total utilization. The default is\n"
					+ "# set to 5 percent.");

	public static final LongConfigParam CLEANER_BYTES_INTERVAL = new LongConfigParam(
			"je.cleaner.bytesInterval",
			new Long(0), // min
			new Long(Long.MAX_VALUE), // max
			new Long(0), // default
			true, // mutable
			"# The cleaner checks disk utilization every time we write this many\n"
					+ "# bytes to the log.  If zero (and by default) it is set to the\n"
					+ "# je.log.fileMax value divided by four.");

	public static final IntConfigParam CLEANER_DEADLOCK_RETRY = new IntConfigParam(
			"je.cleaner.deadlockRetry", new Integer(0), // min
			new Integer(Integer.MAX_VALUE),// max
			new Integer(3), // default
			true, // mutable
			"# The number of times to retry cleaning if a deadlock occurs.\n"
					+ "# The default is set to 3.");

	public static final LongConfigParam CLEANER_LOCK_TIMEOUT = new LongConfigParam(
			"je.cleaner.lockTimeout", new Long(0), // min
			new Long(4294967296L), // max
			new Long(500000L), // default
			true, // mutable
			"# The lock timeout for cleaner transactions in microseconds.\n"
					+ "# The default is set to 0.5 seconds.");

	public static final BooleanConfigParam CLEANER_REMOVE = new BooleanConfigParam(
			"je.cleaner.expunge",
			true, // default
			true, // mutable
			"# If true, the cleaner deletes log files after successful cleaning.\n"
					+ "# If false, the cleaner changes log file extensions to .DEL\n"
					+ "# instead of deleting them. The default is set to true.");

	/* @deprecated As of 1.7.1, no longer used. */
	public static final IntConfigParam CLEANER_MIN_FILES_TO_DELETE = new IntConfigParam(
			"je.cleaner.minFilesToDelete", new Integer(1), // min
			new Integer(1000000), // max
			new Integer(5), // default
			false, // mutable
			"# (deprecated, no longer used");

	/* @deprecated As of 2.0, no longer used. */
	public static final IntConfigParam CLEANER_RETRIES = new IntConfigParam(
			"je.cleaner.retries", new Integer(0), // min
			new Integer(1000), // max
			new Integer(10), // default
			false, // mutable
			"# (deprecated, no longer used");

	/* @deprecated As of 2.0, no longer used. */
	public static final IntConfigParam CLEANER_RESTART_RETRIES = new IntConfigParam(
			"je.cleaner.restartRetries", new Integer(0), // min
			new Integer(1000), // max
			new Integer(5), // default
			false, // mutable
			"# (deprecated, no longer used");

	public static final IntConfigParam CLEANER_MIN_AGE = new IntConfigParam(
			"je.cleaner.minAge",
			new Integer(1), // min
			new Integer(1000), // max
			new Integer(2), // default
			true, // mutable
			"# The minimum age of a file (number of files between it and the\n"
					+ "# active file) to qualify it for cleaning under any conditions.\n"
					+ "# The default is set to 2.");

	public static final BooleanConfigParam CLEANER_CLUSTER = new BooleanConfigParam(
			"je.cleaner.cluster",
			false, // default
			true, // mutable
			"# *** Experimental and may be removed in a future release. ***\n"
					+ "# If true, eviction and checkpointing will cluster records by key\n"
					+ "# value, migrating them from low utilization files if they are\n"
					+ "# resident.\n"
					+ "# The cluster and clusterAll properties may not both be set to true.");

	public static final BooleanConfigParam CLEANER_CLUSTER_ALL = new BooleanConfigParam(
			"je.cleaner.clusterAll",
			false, // default
			true, // mutable
			"# *** Experimental and may be removed in a future release. ***\n"
					+ "# If true, eviction and checkpointing will cluster records by key\n"
					+ "# value, migrating them from low utilization files whether or not\n"
					+ "# they are resident.\n"
					+ "# The cluster and clusterAll properties may not both be set to true.");

	public static final IntConfigParam CLEANER_MAX_BATCH_FILES = new IntConfigParam(
			"je.cleaner.maxBatchFiles",
			new Integer(0), // min
			new Integer(100000), // max
			new Integer(0), // default
			true, // mutable
			"# The maximum number of log files in the cleaner's backlog, or\n"
					+ "# zero if there is no limit.  Changing this property can impact the\n"
					+ "# performance of some out-of-memory applications.");

	public static final IntConfigParam CLEANER_READ_SIZE = new IntConfigParam(
			"je.cleaner.readSize", new Integer(128), // min
			null, // max
			new Integer(0), // default
			true, // mutable
			"# The read buffer size for cleaning.  If zero (the default), then\n"
					+ "# je.log.iteratorReadSize value is used.");

	public static final BooleanConfigParam CLEANER_TRACK_DETAIL = new BooleanConfigParam(
			"je.cleaner.trackDetail", true, // default
			false, // mutable
			"# If true, the cleaner tracks and stores detailed information that\n"
					+ "# is used to decrease the cost of cleaning.");


	public static final BooleanConfigParam CLEANER_RMW_FIX = new BooleanConfigParam(
			"je.cleaner.rmwFix",
			true, // default
			false, // mutable
			"# If true, detail information is discarded that was added by earlier\n"
					+ "# versions of JE if it may be invalid.  This may be set to false\n"
					+ "# for increased performance, but only if LockMode.RMW was never used.");

	public static final ConfigParam CLEANER_FORCE_CLEAN_FILES = new ConfigParam(
			"je.cleaner.forceCleanFiles",
			"", // default
			false, // mutable
			"# Specifies a list of files or file ranges to force clean.  This is\n"
					+ "# intended for use in forcing the cleaning of a large number of log\n"
					+ "# files.  File numbers are in hex and are comma separated or hyphen\n"
					+ "# separated to specify ranges, e.g.: '9,a,b-d' will clean 5 files.");

	public static final IntConfigParam CLEANER_THREADS = new IntConfigParam(
			"je.cleaner.threads",
			new Integer(1), // min
			null, // max
			new Integer(1), // default
			true, // mutable
			"# The number of threads allocated by the cleaner for log file\n"
					+ "# processing.  If the cleaner backlog becomes large, increase this\n"
					+ "# value.  The default is set to 1.");

	public static final IntConfigParam CLEANER_LOOK_AHEAD_CACHE_SIZE = new IntConfigParam(
			"je.cleaner.lookAheadCacheSize", new Integer(0), // min
			null, // max
			new Integer(8192), // default
			true, // mutable
			"# The look ahead cache size for cleaning in bytes.  Increasing this\n"
					+ "# value can reduce the number of Btree lookups.");

	/*
	 * Transactions
	 */
	public static final IntConfigParam N_LOCK_TABLES = new IntConfigParam(
			"je.lock.nLockTables",
			new Integer(1), // min
			new Integer(32767), // max
			new Integer(1), // default
			false, // mutable
			"# Number of Lock Tables.  Set this to a value other than 1 when\n"
					+ "# an application has multiple threads performing concurrent JE\n"
					+ "# operations.  It should be set to a prime number, and in general\n"
					+ "# not higher than the number of application threads performing JE\n"
					+ "# operations.");

	public static final LongConfigParam LOCK_TIMEOUT = new LongConfigParam(
			"je.lock.timeout", new Long(0), // min
			new Long(4294967296L), // max
			new Long(500000L), // default
			false, // mutable
			"# The lock timeout in microseconds.");

	public static final LongConfigParam TXN_TIMEOUT = new LongConfigParam(
			"je.txn.timeout", new Long(0), // min
			new Long(4294967296L), // max_value
			new Long(0), // default
			false, // mutable
			"# The transaction timeout, in microseconds. A value of 0 means no limit.");

	public static final BooleanConfigParam TXN_SERIALIZABLE_ISOLATION = new BooleanConfigParam(
			"je.txn.serializableIsolation",
			false, // default
			false, // mutable
			"# Transactions have the Serializable (Degree 3) isolation level.  The\n"
					+ "# default is false, which implies the Repeatable Read isolation level.");

	public static final BooleanConfigParam TXN_DEADLOCK_STACK_TRACE = new BooleanConfigParam(
			"je.txn.deadlockStackTrace",
			false, // default
			true, // mutable
			"# Set this parameter to true to add stacktrace information to deadlock\n"
					+ "# (lock timeout) exception messages.  The stack trace will show where\n"
					+ "# each lock was taken.  The default is false, and true should only be\n"
					+ "# used during debugging because of the added memory/processing cost.\n"
					+ "# This parameter is 'static' across all environments.");

	public static final BooleanConfigParam TXN_DUMPLOCKS = new BooleanConfigParam(
			"je.txn.dumpLocks", false, // default
			true, // mutable
			"# Dump the lock table when a lock timeout is encountered, for\n"
					+ "# debugging assistance.");

	/*
	 * Debug tracing system
	 */
	public static final BooleanConfigParam JE_LOGGING_FILE = new BooleanConfigParam(
			"java.util.logging.FileHandler.on", true, // default
			false, // mutable
			"# Use FileHandler in logging system.");

	public static final BooleanConfigParam JE_LOGGING_CONSOLE = new BooleanConfigParam(
			"java.util.logging.ConsoleHandler.on", false, // default
			false, // mutable
			"# Use ConsoleHandler in logging system.");

	public static final BooleanConfigParam JE_LOGGING_DBLOG = new BooleanConfigParam(
			"java.util.logging.DbLogHandler.on", true, // default
			false,// mutable
			"# Use DbLogHandler in logging system.");

	public static final IntConfigParam JE_LOGGING_FILE_LIMIT = new IntConfigParam(
			"java.util.logging.FileHandler.limit", new Integer(1000), // min
			new Integer(1000000000), // max
			new Integer(10000000), // default
			false, // mutable
			"# Log file limit for FileHandler.");

	public static final IntConfigParam JE_LOGGING_FILE_COUNT = new IntConfigParam(
			"java.util.logging.FileHandler.count", new Integer(1), // min
			null, // max
			new Integer(10), // default
			false, // mutable
			"# Log file count for FileHandler.");

	public static final ConfigParam JE_LOGGING_LEVEL = new ConfigParam(
			"java.util.logging.level",
			"FINEST",
			false, // mutable
			"# Trace messages equal and above this level will be logged.\n"
					+ "# Value should be one of the predefined java.util.logging.Level values");

	public static final ConfigParam JE_LOGGING_LEVEL_LOCKMGR = new ConfigParam(
			"java.util.logging.level.lockMgr",
			"FINE",
			false, // mutable
			"# Lock manager specific trace messages will be issued at this level.\n"
					+ "# Value should be one of the predefined java.util.logging.Level values");

	public static final ConfigParam JE_LOGGING_LEVEL_RECOVERY = new ConfigParam(
			"java.util.logging.level.recovery",
			"FINE",
			false, // mutable
			"# Recovery specific trace messages will be issued at this level.\n"
					+ "# Value should be one of the predefined java.util.logging.Level values");

	public static final ConfigParam JE_LOGGING_LEVEL_EVICTOR = new ConfigParam(
			"java.util.logging.level.evictor",
			"FINE",
			false, // mutable
			"# Evictor specific trace messages will be issued at this level.\n"
					+ "# Value should be one of the predefined java.util.logging.Level values");


	/*
	 * Create a sample je.properties file.
	 */
	public static void main(String argv[]) {
		if (argv.length != 1) {
			throw new IllegalArgumentException("Usage: EnvironmentParams "
					+ "<samplePropertyFile>");
		}

		try {
			FileWriter exampleFile = new FileWriter(new File(argv[0]));
			TreeSet paramNames = new TreeSet(getSupportedParams().keySet());
			Iterator iter = paramNames.iterator();
			exampleFile
					.write("####################################################\n"
							+ "# Example Berkeley DB, Java Edition property file\n"
							+ "# Each parameter is set to its default value\n"
							+ "####################################################\n\n");

			while (iter.hasNext()) {
				String paramName = (String) iter.next();
				ConfigParam param = (ConfigParam) getSupportedParams()
						.get(paramName);
				exampleFile.write(param.getDescription() + "\n");
				String extraDesc = param.getExtraDescription();
				if (extraDesc != null) {
					exampleFile.write(extraDesc + "\n");
				}
				exampleFile.write("#" + param.getName() + "="
						+ param.getDefault() + "\n# (mutable at run time: "
						+ param.isMutable() + ")\n\n");
			}
			exampleFile.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 * Add a configuration parameter to the set supported by an environment.
	 */
	static void addSupportedParam(ConfigParam param) {
		getSupportedParams().put(param.getName(), param);
	}
	
	
	
	
	
	
	
	//CHECKSUM ASPECT
	public static final BooleanConfigParam LOG_CHECKSUM_READ = new BooleanConfigParam(
			"je.log.checksumRead", true, // default
			false, // mutable
			"# If true, perform a checksum check when reading entries from log.");


	//CHUNKED NIO ASPECT
	public static final LongConfigParam LOG_CHUNKED_NIO = new LongConfigParam(
			"je.log.chunkedNIO", new Long(0L), // min
			new Long(1 << 26), // max (64M)
			new Long(0L), // default (no chunks)
			false, // mutable
			"# If non-0 (default is 0) break all IO into chunks of this size.\n"
					+ "# This setting is only used if je.log.useNIO=true.");

	public static Map getSupportedParams() {
		if (SUPPORTED_PARAMS==null)
			SUPPORTED_PARAMS = new HashMap();
		return SUPPORTED_PARAMS;
	}
	
}

