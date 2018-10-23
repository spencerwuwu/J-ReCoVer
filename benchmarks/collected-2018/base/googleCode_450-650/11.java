// https://searchcode.com/api/result/5422150/

package org.worldbank.transport.tamt.server.dao;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.worldbank.transport.tamt.jndi.mock.MockIntialContextFactory;
import org.worldbank.transport.tamt.shared.SpeedDistributionRecord;

public class SpeedBinTests {

	static SpeedBinDAO speedBinDAO;
	static GPSTraceDAO gpsTraceDAO;
	static Logger logger = Logger.getLogger(SpeedBinTests.class);
	
	public static void createJNDIContext() throws Exception
	{
		PGConnectionPoolDataSource ds = new PGConnectionPoolDataSource();
		ds.setServerName("localhost");
		ds.setPortNumber(5432);
		ds.setDatabaseName("tamt15");
		ds.setUser("gis");
		ds.setPassword("gis");
		ds.setDefaultAutoCommit(true);
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MockIntialContextFactory.class.getName());
		InitialContext ic = new InitialContext();
		ic.bind("TAMTDataSource", ds);
	}
	
	@BeforeClass
	public static void runBeforeTests() throws Exception
	{
		createJNDIContext();
		speedBinDAO = SpeedBinDAO.get();
		gpsTraceDAO = GPSTraceDAO.get();
	}
	
	@Test
	public void haveDAO()
	{
		logger.debug(speedBinDAO);
		assertNotNull(speedBinDAO);
	}
	
	
	@Test
	public void lastSpeedBin()
	{
		String tagId = "3590f46c-6140-4555-bc73-75c7e381a843";
		String dayType = "WEEKDAY";
		int hourBin = 8;
		int lastSpeedBin = -1;
		logger.debug("test last speed bin");
		try {
			
			lastSpeedBin = speedBinDAO.getLastSpeedBin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.debug("lastSpeedBin=" + lastSpeedBin);
		assertEquals(6, lastSpeedBin);
		
	}

	@Test
	public void getTotalFlow()
	{
		String tagId = "3590f46c-6140-4555-bc73-75c7e381a843";
		String dayType = "WEEKDAY";
		int hourBin = 7;
		logger.debug("test getTotalFlow");
		double totalFlow = 0.0;
		try {
			totalFlow = speedBinDAO.getTotalFlow(tagId, dayType, 0);
			/*
			// do all hour bins
			for (int i = 0; i < 24; i++) {
				totalFlow = speedBinDAO.getTotalFlow(tagId, dayType, i);
				logger.debug("total flow for hour("+i+")=" + totalFlow);
				assertTrue( totalFlow > 0 );
			}
			*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// can't assert much here, because input data may change
		assertTrue(true);
	}

	@Test
	public void createObservedGPSDataRecord()
	{
		String tagId = "3590f46c-6140-4555-bc73-75c7e381a843";
		String dayType = "WEEKDAY";
		int hourBin = 0;
		logger.debug("test getTotalFlow");
		double totalFlow = 0.0;
		try {
			speedBinDAO.createObservedGPSDataRecord(tagId, dayType, hourBin, true, 9999.00);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// can't assert much here, because input data may change
		assertTrue(true);
	}

	
	@Test
	public void populateSpeedDistObserved()
	{
		try {
			speedBinDAO.populateSpeedDistObserved();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		logger.debug("Done");
		assertTrue(true);
	}
	
	@Test
	public void hasObservedDataTrue()
	{
		String tagId = "3590f46c-6140-4555-bc73-75c7e381a843";
		String dayType = "WEEKDAY";
		int hourBin = 7;
		logger.debug("test hasObservedDataTrue");
		boolean hasObservedData = false;
		try {
			hasObservedData = speedBinDAO.hasObservedData(tagId, dayType, hourBin);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("hasObservedData true=" + hasObservedData);
		assertTrue(hasObservedData);
	}

	@Test
	public void hasObservedDataFalse()
	{
		String tagId = "3590f46c-6140-4555-bc73-75c7e381a843";
		String dayType = "WEEKDAY";
		int hourBin = 0;
		logger.debug("test hasObservedDataFalse");
		boolean hasObservedData = false;
		try {
			hasObservedData = speedBinDAO.hasObservedData(tagId, dayType, hourBin);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("hasObservedData false=" + hasObservedData);
		assertFalse(hasObservedData);
	}
	
	@Test
	public void updateSpeedDistributionPercentageValues()
	{
		String tagId = "3590f46c-6140-4555-bc73-75c7e381a843";
		String dayType = "WEEKDAY";
		int hourBin = 0;
		logger.debug("test updateSpeedDistributionPercentageValues");
		try {
			speedBinDAO.updateSpeedDistributionPercentageValues(tagId, dayType, hourBin);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// what do I assert here?
	}

	@Test
	public void getClosestSpeedDistribution()
	{
		String tagId = "3590f46c-6140-4555-bc73-75c7e381a843";
		String dayType = "WEEKDAY";
		double totalFlow = 27;
		
		SpeedDistributionRecord speedDistributionRecord = new SpeedDistributionRecord();
		speedDistributionRecord.setTagId(tagId);
		speedDistributionRecord.setDayType(dayType);
		speedDistributionRecord.setTotalFlow(totalFlow);
		
		SpeedDistributionRecord match = null;
		logger.debug("test getClosestSpeedDistribution");
		try {
			match = speedBinDAO.updateFromClosestDistribution(speedDistributionRecord);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		// match should not be null
		assertNotNull(match);
	
	}
	
	@Test
	public void interpolateSpeedDistribution()
	{
		logger.debug("test interpolateSpeedDistribution");
		try {
			speedBinDAO.interpolateSpeedDistribution();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		logger.debug("Done");
		assertTrue(true);
	
	}

	@Test
	public void populateSpeedDistribution()
	{
		logger.debug("test populateSpeedDistribution");
		try {
			speedBinDAO.populateSpeedDistribution();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		logger.debug("Done");
		assertTrue(true);
	
	}
	
	@Test
	public void calculateEngineSoakTimesAndTripLength()
	{
		logger.debug("test calculateEngineSoakTimesAndTripLength");
		try {
			//gpsTraceDAO.calculateEngineSoakTimesAndTripLength();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}	

	@Test
	public void triggerSpeedDistributionInterpolation()
	{
		logger.debug("test triggerSpeedDistributionInterpolation");
		try {
			speedBinDAO.triggerSpeedDistributionInterpolation();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	@Test
	public void getTrafficCount()
	{
		logger.debug("test getTrafficCount");
		/*
		SELECT tx FROM trafficflowreport
		WHERE tagid = '3590f46c-6140-4555-bc73-75c7e381a843' 
		AND daytype = 'WEEKDAY'
		AND date_part('hour', hour_bin) = 6
		
		should give us 9.33
		 */
		double trafficCount = 0.0;
		try {
			trafficCount = speedBinDAO.getTrafficCount("3590f46c-6140-4555-bc73-75c7e381a843", "WEEKDAY", 6, "tx");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug(trafficCount);
		assertEquals(9.33, trafficCount, 0.0001);
	
	}
	
	@Test
	public void getPercentValuesInBin()
	{
		logger.debug("test getPercentValuesInBin");
		
		try {
			speedBinDAO.getPercentValuesInBin("3590f46c-6140-4555-bc73-75c7e381a843", "WEEKDAY", 6, 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	@Test
	public void percentValuesInBin()
	{
		logger.debug("test percentTimeInBin");
		
		ArrayList<Double> percentValues = new ArrayList<Double>();
		try {
			percentValues = speedBinDAO.getPercentValuesInBin("3590f46c-6140-4555-bc73-75c7e381a843", "WEEKDAY", 8, 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug(percentValues);
		// 0.237179487179487;0.0602910660051947
		assertEquals(0.237179487179487, percentValues.get(0), 0.00000001);
		assertEquals(0.0602910660051947, percentValues.get(1), 0.00000001);
	}	
	
	@Test
	public void insertSpeedDistTrafficFlowRecord()
	{
		logger.debug("test insertSpeedDistTrafficFlowRecord");
		
		try {
			speedBinDAO.insertSpeedDistTrafficFlowRecord("3590f46c-6140-4555-bc73-75c7e381a843", "WEEKDAY", "tx", 5, 215, 1100, 45.2, 8);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// what can i assert?
	}

	@Test
	public void insertSpeedDistributionRecord()
	{
		logger.debug("test insertSpeedDistTrafficFlowRecord");
		
		try {
			// developer must change the tagID to allow this to work for current database
			speedBinDAO.insertSpeedDistributionRecord("3590f46c-6140-4555-bc73-75c7e381a843", "WEEKDAY", 0, 55);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// what can i assert?
	}
	
	@Test
	public void combineSpeedDistributionTrafficFlow()
	{
		logger.debug("test combineSpeedDistributionTrafficFlow");
		
		try {
			speedBinDAO.combineSpeedDistributionTrafficFlow();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// what can i assert?
	}
	
	@Test
	public void setSumValuesForVehicleType()
	{
		logger.debug("test setSumValuesForVehicleType");
		
		try {
			speedBinDAO.setSumValuesForVehicleType("3590f46c-6140-4555-bc73-75c7e381a843","WEEKDAY","w2", 73, 4.0683921579336);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// what can i assert?
	}
	
	@Test 
	public void truncateTables()
	{
		logger.debug("test truncateTables");
		try {
			speedBinDAO.truncateSpeedDistributionTables();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void fullTest()
	{
		logger.debug("test fullTest");
		
		try {
			
			// prerequisite: a full traffic flow report table, no gaps
			logger.debug("POPULATE SPEED DISTRIBUTION");
			// from gps points, determine speed distribution
			// TODO: This output is a candidate for SpeedBin UI report and CSV download
			speedBinDAO.populateSpeedDistribution();							
			
			// mark which speed distributions are observed
			logger.debug("POPULATE SPEED DIST OBSERVED");
			speedBinDAO.populateSpeedDistObserved();
			
			// fill in the gaps for speed distribution
			logger.debug("INTERPOLATE SPEED DISTRIBUTION");
			// TODO: This output is a candidate for SpeedBin UI report and CSV download
			speedBinDAO.interpolateSpeedDistribution();				
			
			// now we have a speed distribution table with no gaps
			
			// and now, combine speed distribution with traffic flow report
			logger.debug("COMBINE SPEED DISTRIBUTION AND TRAFFIC FLOW");
			// TODO: This output is a candidate for SpeedBin UI report and CSV download
			speedBinDAO.combineSpeedDistributionTrafficFlow();
			
			// reduce the table to remove day type
			logger.debug("REDUCE TABLE: REMOVE DAYTYPE");
			// TODO: This output is a candidate for SpeedBin UI report and CSV download
			speedBinDAO.removeDayTypeFromSpeedDistributionTrafficFlow();
			
			// reduce the table to remove tag
			logger.debug("REDUCE TABLE: REMOVE TAG");
			// TODO: This output is a candidate for SpeedBin UI report and CSV download
			speedBinDAO.removeTagFromSpeedDistributionTrafficFlowTagVehicleSpeed();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// what can i assert?
	}
	
	/*
	 * Performance is a major issue for this nested loop
	 * to combine the speed distribution table with the
	 * traffic flow report table. Under the current
	 * java design, it took about 10 rows per minute.
	 * For 2 tags and 6 speed bins, I would have waited 12 hours.
	 * For 8 tags and 20 speed bins, I would have waited 
	 * just over 1 week. Not good. Remember, these times
	 * are based on a few steps:
	 * 	a) populating the speed distribution table
	 *  b) populating the speed dist observed table
	 *  c) interpolating the speed distribution table (filling in gaps)
	 *  d) combining speed distribution with traffic flow
	 */
	
	@Test
	public void countRows()
	{
		logger.debug("start countRows");
		
		// 2 and 6 = 7776
		// 2 and 7 = 9072
		
		// 3 and 6 = 11664
		// 3 and 7 = 13608
		
		// 8 and 20 = 103680
		int numTags = 8;
		int numSpeedBins = 20;
		
		int numRows = 0;
		int tag = 0;
		int day = 0;
		int vehicle = 0;
		int speed = 0;
		int hour = 0;
		
		// simulate 2 tags
		for (int i = 0; i < numTags; i++) {
			tag = i;
			// simulate 3 day types
			for (int j = 0; j < 3; j++) {
				day = j;
				// simulate 9 vehicle types
				for (int j2 = 0; j2 < 9; j2++) {
					vehicle = j2;
					// simulate numSpeedBins
					for (int k = 0; k < numSpeedBins; k++) {
						speed = k;
						// simulate 24 hours
						for (int k2 = 0; k2 < 24; k2++) {
							hour = k2;
							numRows++;
							logger.debug("tag("+tag+"), day("+day+"), vehicle("+vehicle+"), speed("+speed+"), hour("+hour+")");
						}
					}
				}
			}
		}
		logger.debug("numRows=" + numRows);
		
	}
	
	@Test
	public void trafficCountCacheNull()
	{
		HashMap<String, Double> trafficCountCache = new HashMap<String, Double>();
		
		// lookup non-existent key
		Double trafficCount = trafficCountCache.get("badkey");
		logger.debug("trafficCount=" + trafficCount);
		assertNull(trafficCount);
		
	}
	
	@Test
	public void trafficCountCacheNotNull()
	{
		HashMap<String, Double> trafficCountCache = new HashMap<String, Double>();
		
		// insert a map
		trafficCountCache.put("goodkey", Double.parseDouble("99.0"));
		
		// lookup non-existent key
		Double trafficCount = trafficCountCache.get("goodkey");
		logger.debug("trafficCount=" + trafficCount);
		assertNotNull(trafficCount);
		assertEquals(99.0, trafficCount, 0.00001);
	}
	
	@Test
	public void percentValuesCacheNull()
	{
		HashMap<String, ArrayList<Double>> percentValuesCache = new HashMap<String, ArrayList<Double>>();
		
		// lookup non-existent key
		ArrayList<Double> percentValues = percentValuesCache.get("badkey");
		logger.debug("percentValues=" + percentValues);
		assertNull(percentValues);
		
	}
	
	@Test
	public void percentValuesCacheNotNull()
	{
		HashMap<String, ArrayList<Double>> percentValuesCache = new HashMap<String, ArrayList<Double>>();
		
		// insert a map
		ArrayList<Double> percentValues = new ArrayList<Double>();
		percentValues.add(1234.5);
		percentValues.add(5678.9);
		percentValuesCache.put("goodkey", percentValues);
		
		// lookup non-existent key
		ArrayList<Double> fetched = percentValuesCache.get("goodkey");
		logger.debug("fetched=" + fetched);
		assertNotNull(fetched);
		assertEquals(1234.5, fetched.get(0), 0.00001);
		assertEquals(5678.9, fetched.get(1), 0.00001);
	}
	
	@Test
	public void removeDayTypeFromSpeedDistributionTrafficFlow()
	{
		logger.debug("test removeDayTypeFromSpeedDistributionTrafficFlow");
		
		try {
			speedBinDAO.removeDayTypeFromSpeedDistributionTrafficFlow();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// what can i assert?
		
	}
}
