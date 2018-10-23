// https://searchcode.com/api/result/70130815/

/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *      Sleepycat Software.  All rights reserved.
 *
 * $Id: LogBufferBudget.java,v 1.1.2.1 2006/10/25 17:21:24 ckaestne Exp $
 */

package com.sleepycat.je.dbi;


import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.config.EnvironmentParams;

/**
 * MemoryBudget calculates the available memory for JE and how to apportion it
 * between cache and log buffers. It is meant to centralize all memory
 * calculations. Objects that ask for memory budgets should get settings from
 * this class, rather than using the configuration parameter values directly.
 */
public class LogBufferBudget implements EnvConfigObserver {
	
	public final static long MIN_MAX_MEMORY_SIZE = 96 * 1024;

	public final static String MIN_MAX_MEMORY_SIZE_STRING = Long
			.toString(MIN_MAX_MEMORY_SIZE);

	private final static long N_64MB = (1 << 26);
	

	

	/* Memory available to log buffers. */
	protected long logBufferBudget;

	protected EnvironmentImpl envImpl;

	protected long maxMemory;

	public LogBufferBudget(EnvironmentImpl envImpl, DbConfigManager configManager)
			throws DatabaseException {

		this.envImpl = envImpl;

		/* Request notification of mutable property changes. */
		envImpl.addConfigObserver(this);

		/* Peform first time budget initialization. */
		reset(configManager);
	}

	/**
	 * Respond to config updates.
	 */
	public void envConfigUpdate(DbConfigManager configManager)
			throws DatabaseException {

		/*
		 * Reinitialize the cache budget and the log buffer pool, in that order.
		 * Do not reset the log buffer pool if the log buffer budget hasn't
		 * changed, since that is expensive and may cause I/O.
		 */
		long oldLogBufferBudget = logBufferBudget;
		reset(configManager);
		if (oldLogBufferBudget != logBufferBudget) {
			envImpl.getLogManager().resetPool(configManager);
		}
	}

	/**
	 * Initialize at construction time and when the cache is resized.
	 */
	protected void reset(DbConfigManager configManager) throws DatabaseException {

		/*
		 * Calculate the total memory allotted to JE. 1. If je.maxMemory is
		 * specified, use that. Check that it's not more than the jvm memory. 2.
		 * Otherwise, take je.maxMemoryPercent * JVM max memory.
		 */
		long newMaxMemory = configManager.getLong(EnvironmentParams.MAX_MEMORY);
		long jvmMemory = getRuntimeMaxMemory();

		if (newMaxMemory != 0) {
			/* Application specified a cache size number, validate it. */
			if (jvmMemory < newMaxMemory) {
				throw new IllegalArgumentException(EnvironmentParams.MAX_MEMORY
						.getName()
						+ " has a value of "
						+ newMaxMemory
						+ " but the JVM is only configured for "
						+ jvmMemory
						+ ". Consider using je.maxMemoryPercent.");
			}
			if (newMaxMemory < MIN_MAX_MEMORY_SIZE) {
				throw new IllegalArgumentException(EnvironmentParams.MAX_MEMORY
						.getName()
						+ " is "
						+ newMaxMemory
						+ " which is less than the minimum: "
						+ MIN_MAX_MEMORY_SIZE);
			}
		} else {

			/*
			 * When no explicit cache size is specified and the JVM memory size
			 * is unknown, assume a default sized (64 MB) heap. This produces a
			 * reasonable cache size when no heap size is known.
			 */
			if (jvmMemory == Long.MAX_VALUE) {
				jvmMemory = N_64MB;
			}

			/* Use the configured percentage of the JVM memory size. */
			int maxMemoryPercent = configManager
					.getInt(EnvironmentParams.MAX_MEMORY_PERCENT);
			newMaxMemory = (maxMemoryPercent * jvmMemory) / 100;
		}

		/*
		 * Calculate the memory budget for log buffering. If the LOG_MEM_SIZE
		 * parameter is not set, start by using 7% (1/16th) of the cache size.
		 * If it is set, use that explicit setting.
		 * 
		 * No point in having more log buffers than the maximum size. If this
		 * starting point results in overly large log buffers, reduce the log
		 * buffer budget again.
		 */
		long newLogBufferBudget = configManager
				.getLong(EnvironmentParams.LOG_MEM_SIZE);
		if (newLogBufferBudget == 0) {
			newLogBufferBudget = newMaxMemory >> 4;
		} else if (newLogBufferBudget > newMaxMemory / 2) {
			newLogBufferBudget = newMaxMemory / 2;
		}

		/*
		 * We have a first pass at the log buffer budget. See what size log
		 * buffers result. Don't let them be too big, it would be a waste.
		 */
		
		
		int numBuffers = configManager
				.getInt(EnvironmentParams.NUM_LOG_BUFFERS);
		long startingBufferSize = newLogBufferBudget / numBuffers;
		
		int logBufferSize = configManager
				.getInt(EnvironmentParams.LOG_BUFFER_MAX_SIZE);
		if (startingBufferSize > logBufferSize) {
			startingBufferSize = logBufferSize;
			newLogBufferBudget = numBuffers * startingBufferSize;
		} else if (startingBufferSize < EnvironmentParams.MIN_LOG_BUFFER_SIZE) {
			startingBufferSize = EnvironmentParams.MIN_LOG_BUFFER_SIZE;
			newLogBufferBudget = numBuffers * startingBufferSize;
		}

		logBufferBudget = newLogBufferBudget;
		maxMemory = newMaxMemory;
	}

	/**
	 * Returns Runtime.maxMemory(), accounting for a MacOS bug. May return
	 * Long.MAX_VALUE if there is no inherent limit. Used by unit tests as well
	 * as by this class.
	 */
	public static long getRuntimeMaxMemory() {

		/* Runtime.maxMemory is unreliable on MacOS Java 1.4.2. */
		if ("Mac OS X".equals(System.getProperty("os.name"))) {
			String jvmVersion = System.getProperty("java.version");
			if (jvmVersion != null && jvmVersion.startsWith("1.4.2")) {
				return Long.MAX_VALUE; /* Undetermined heap size. */
			}
		}

		return Runtime.getRuntime().maxMemory();
	}

	
	public long getLogBufferBudget() {
		return logBufferBudget;
	}

}

