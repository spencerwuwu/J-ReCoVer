// https://searchcode.com/api/result/100362534/

package com.github.projectflink.testPlan;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.Plan;
import org.apache.flink.api.common.Program;
import org.apache.flink.api.common.ProgramDescription;
import org.apache.flink.api.common.accumulators.IntCounter;
import org.apache.flink.api.common.accumulators.LongCounter;
import org.apache.flink.api.java.record.functions.CoGroupFunction;
import org.apache.flink.api.java.record.functions.CrossFunction;
import org.apache.flink.api.java.record.functions.JoinFunction;
import org.apache.flink.api.java.record.functions.MapFunction;
import org.apache.flink.api.java.record.functions.ReduceFunction;
import org.apache.flink.api.java.record.io.CsvInputFormat;
import org.apache.flink.api.java.record.io.CsvOutputFormat;
import org.apache.flink.api.java.record.io.FileOutputFormat;
import org.apache.flink.api.java.record.io.TextInputFormat;
import org.apache.flink.api.java.record.io.avro.AvroRecordInputFormat;
import org.apache.flink.api.java.record.operators.BulkIteration;
import org.apache.flink.api.java.record.operators.CoGroupOperator;
import org.apache.flink.api.java.record.operators.DeltaIteration;
import org.apache.flink.api.java.record.operators.FileDataSink;
import org.apache.flink.api.java.record.operators.FileDataSource;
import org.apache.flink.api.java.record.operators.JoinOperator;
import org.apache.flink.api.java.record.operators.MapOperator;
import org.apache.flink.api.java.record.operators.ReduceOperator;
import org.apache.flink.client.LocalExecutor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.hadoopcompatibility.mapred.record.datatypes.WritableWrapper;
import org.apache.flink.types.BooleanValue;
import org.apache.flink.types.DoubleValue;
import org.apache.flink.types.FloatValue;
import org.apache.flink.types.IntValue;
import org.apache.flink.types.LongValue;
import org.apache.flink.types.Record;
import org.apache.flink.types.StringValue;
import org.apache.flink.util.Collector;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class LargeTestPlan implements Program, ProgramDescription {
	private static final long serialVersionUID = 1L;
	
	public static String customer;
	public static String lineitem;
	public static String nation;
	public static String orders;
	public static String region;
	public static String orderAvroFile;
	public static String outputTableDirectory;
	public static String sequenceFileInput;

	public static int maxBulkIterations;
	public static int dop;

	// paths (without file:// or hdfs://)
	public static String outputAccumulatorsPath;
	public static String outputKeylessReducerPath;
	public static String outputOrderKeysPath;
	public static String outputOrderAvroPath;
	public static String ordersPath;

	// ---------------------- Parameter Examples ----------------------
	// ----> For local testing:
	// file:///home/twalthr/repo/test/stratosphere-fulltest/TPC-H/generated_SF0.001/customer.tbl 
	// file:///home/twalthr/repo/test/stratosphere-fulltest/TPC-H/generated_SF0.001/lineitem.tbl 
    // file:///home/twalthr/repo/test/stratosphere-fulltest/TPC-H/generated_SF0.001/nation.tbl 
	// file:///home/twalthr/repo/test/stratosphere-fulltest/TPC-H/generated_SF0.001/orders.tbl 
	// file:///home/twalthr/repo/test/stratosphere-fulltest/TPC-H/generated_SF0.001/region.tbl 
	// file:///home/twalthr/repo/test/stratosphere-fulltest/out/ordersAvro.avro 
	// file:///home/twalthr/repo/test/seq
	// file:///home/twalthr/repo/test/stratosphere-fulltest/out 
	// 10000
	// 26
	// /home/twalthr/repo/test/stratosphere-fulltest/TPC-H/generated_SF0.001/orders.tbl 
	// /home/twalthr/repo/test/stratosphere-fulltest/out/intermediate-accumulator.txt 
	// /home/twalthr/repo/test/stratosphere-fulltest/out/intermediate-keylessreducer.txt 
	// /home/twalthr/repo/test/stratosphere-fulltest/out/ordersAvro.avro
	// ----> For cluster testing:
	// ./bin/stratosphere run -j /home/twalthr/testjob-0.1-SNAPSHOT.jar -c org.apache.flink.test.testPlan.LargeTestPlan -a hdfs:///user/twalthr/customer.tbl hdfs:///user/twalthr/lineitem.tbl hdfs:///user/twalthr/nation.tbl hdfs:///user/twalthr/orders.tbl hdfs:///user/twalthr/region.tbl hdfs:///user/twalthr/ordersAvro.avro aseqfilea hdfs:///user/twalthr/out 1500 26
	
	public static void main(String[] args) throws Exception {

		LargeTestPlan largeTestPlan = new LargeTestPlan();

		// generate only avro file
		if (args.length == 2) {
			ordersPath = args[0];
			outputOrderAvroPath = args[1];
		}
		// for testing purposes
		// path = standard java File path
		else if (args.length >= 13) {
			customer = args[0];
			lineitem = args[1];
			nation = args[2];
			orders = args[3];
			region = args[4];
			orderAvroFile = args[5];
			sequenceFileInput = args[6];
			outputTableDirectory = args[7];
			maxBulkIterations = Integer.valueOf(args[8]);
			// paths (without file:// or hdfs://)
			ordersPath = args[9];
			outputAccumulatorsPath = args[10];
			outputKeylessReducerPath = args[11];
			outputOrderAvroPath = args[12];
		}
		// error
		else {
			System.err.println(largeTestPlan.getDescription());
			System.exit(1);
		}

		// Generate file for avro test
		DatumWriter<Order> orderDatumWriter = new SpecificDatumWriter<Order>(Order.class);
		DataFileWriter<Order> dataFileWriter = new DataFileWriter<Order>(orderDatumWriter);
		dataFileWriter.create(Order.getClassSchema(), new File(outputOrderAvroPath));
		Scanner s = new Scanner(new File(ordersPath));
		while (s.hasNextLine()) {
			@SuppressWarnings("resource")
			Scanner lineScanner = new Scanner(s.nextLine()).useDelimiter("\\|");

			Order o = new Order();
			o.setOOrderkey(lineScanner.nextInt());
			o.setOCustkey(lineScanner.nextInt());
			o.setOOrderstatus(lineScanner.next());
			o.setOTotalprice(lineScanner.nextFloat());
			o.setOOrderdate(lineScanner.next());
			o.setOOrderpriority(lineScanner.next());
			o.setOClerk(lineScanner.next());
			o.setOShipproprity(lineScanner.nextInt());
			o.setOComment(lineScanner.next());
			dataFileWriter.append(o);
			lineScanner.close();
		}
		dataFileWriter.flush();
		s.close();
		dataFileWriter.close();
		
		// do not run job, only build avro files
		if(args.length == 2) return;

		// Create plan and execute
		Plan plan = largeTestPlan.getPlan();
		plan.setDefaultParallelism(3);

		JobExecutionResult result = LocalExecutor.execute(plan);
		// System.out.println(LocalExecutor.optimizerPlanAsJSON(plan));
		// System.exit(0);

		PrintWriter out = new PrintWriter(outputAccumulatorsPath);
		out.println(result.getAccumulatorResult("count-american-customers"));
		out.println(result.getAccumulatorResult("count-europe-customers"));
		out.println(result.getAccumulatorResult("count-rest-customers"));
		out.close();

		// BEGIN: TEST 8 - only for DOP 1
		if (plan.getDefaultParallelism() == 1) {
			int counter = (Integer) result.getAccumulatorResult("count-rest-customers");
			Scanner scanner = new Scanner(new File(outputKeylessReducerPath));
			int counter2 = scanner.nextInt();
			scanner.close();

			if (counter != counter2)
				throw new Exception("TEST 8 FAILED: Keyless Reducer and Accumulator count different");
		}
		// END: TEST 8
	}

	@Override
	public Plan getPlan(String... args) {

		if (args.length < 9 && customer == null) {
			System.err.println(this.getDescription());
			System.exit(1);
		} else if (args.length == 10) {
			customer = args[0];
			lineitem = args[1];
			nation = args[2];
			orders = args[3];
			region = args[4];
			orderAvroFile = args[5];
			sequenceFileInput = args[6];
			outputTableDirectory = args[7];
			maxBulkIterations = Integer.valueOf(args[8]);
			dop = Integer.valueOf(args[9]);
		}

		// Read TPC-H data from .tbl-files		
		// (supplier, part and partsupp not implemented yet)
		FileDataSource customerSource = new FileDataSource(new CsvInputFormat(), customer, "customer");
		CsvInputFormat.configureRecordFormat(customerSource).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0)
				.field(StringValue.class, 1).field(StringValue.class, 2).field(IntValue.class, 3).field(StringValue.class, 4)
				.field(DoubleValue.class, 5).field(StringValue.class, 6).field(StringValue.class, 7);

		FileDataSource lineitemSource = new FileDataSource(new CsvInputFormat(), lineitem, "lineitem");
		CsvInputFormat.configureRecordFormat(lineitemSource).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0)
				.field(IntValue.class, 1).field(IntValue.class, 2).field(IntValue.class, 3).field(IntValue.class, 4)
				.field(FloatValue.class, 5).field(FloatValue.class, 6).field(FloatValue.class, 7).field(StringValue.class, 8)
				.field(StringValue.class, 9).field(StringValue.class, 10).field(StringValue.class, 11).field(StringValue.class, 12)
				.field(StringValue.class, 13).field(StringValue.class, 14).field(StringValue.class, 15);

		FileDataSource nationSource = new FileDataSource(new CsvInputFormat(), nation, "nation");
		CsvInputFormat.configureRecordFormat(nationSource).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0)
				.field(StringValue.class, 1).field(IntValue.class, 2).field(StringValue.class, 3);

		FileDataSource ordersSource = new FileDataSource(new CsvInputFormat(), orders, "orders");
		CsvInputFormat.configureRecordFormat(ordersSource).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0)
				.field(IntValue.class, 1).field(StringValue.class, 2).field(DoubleValue.class, 3).field(StringValue.class, 4)
				.field(StringValue.class, 5).field(StringValue.class, 6).field(IntValue.class, 7).field(StringValue.class, 8);

		FileDataSource regionSource = new FileDataSource(new CsvInputFormat(), region, "region");
		CsvInputFormat.configureRecordFormat(regionSource).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0)
				.field(StringValue.class, 1).field(StringValue.class, 2);

		// BEGIN: TEST 1 - Usage of Join, Map, KeylessReducer, CsvOutputFormat, CoGroup

		// Join fields of customer and nation
		JoinOperator customerWithNation = JoinOperator.builder(JoinFields.class, IntValue.class, 3, 0).input1(customerSource)
				.input2(nationSource).name("T1 Join: Customer with Nation").build();
		joinQuickFix(customerWithNation);

		// Join fields of customerWithNation and region
		JoinOperator customerWithNationRegion = JoinOperator.builder(JoinFields.class, IntValue.class, 10, 0).input1(customerWithNation)
				.input2(regionSource).name("T1 Join: Customer with Nation and Region").build();
		joinQuickFix(customerWithNationRegion);

		// Split the customers by regions
		MapOperator customersInAmerica = MapOperator.builder(new FilterRegion("AMERICA")).input
				(customerWithNationRegion).name("T1 Map: Customers in America").build();
		MapOperator customersInEurope = MapOperator.builder(new FilterRegion("EUROPE")).input
				(customerWithNationRegion).name("T1 Map: Customers in Europe").build();
		MapOperator customersInOtherRegions = MapOperator.builder(FilterRegionOthers.class).input
				(customerWithNationRegion).name("T1 Map: Customers in other regions").build();

		// Count customers of other regions
		ReduceOperator countCustomersOfOtherRegion = ReduceOperator.builder(ReduceCounter.class).input
				(customersInOtherRegions).name("T1 Reduce: Count customers of other region").build();

		// Save keyless reducer results
		FileDataSink resultKR = new FileDataSink(new CsvOutputFormat(),
				outputTableDirectory + "/intermediate-keylessreducer.txt", "T1: Result keyless reducer");
		resultKR.addInput(countCustomersOfOtherRegion);
		CsvOutputFormat.configureRecordFormat(resultKR).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0);

		// Union again and filter customer fields
		MapOperator unionOfRegions = MapOperator.builder(FilterCustomerFields.class).name("T1 Map: Union of regions")
				.input(customersInAmerica, customersInEurope, customersInOtherRegions).build();

		// Save test results to disk
		FileDataSink test1Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test1.tbl",
				"T1: Result Test 1");
		test1Sink.addInput(unionOfRegions);
		CsvOutputFormat.configureRecordFormat(test1Sink).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0)
				.field(StringValue.class, 1).field(StringValue.class, 2).field(IntValue.class, 3).field(StringValue.class, 4)
				.field(DoubleValue.class, 5).field(StringValue.class, 6).field(StringValue.class, 7);

		// Test: Compare to input source
		CoGroupOperator testCustomerIdentity1 = CoGroupOperator.builder(CoGroupTestIdentity.class, IntValue.class, 0,
				0).name("T1 CoGroup: Compare customers' identities")
				.input1(customerSource).input2(unionOfRegions).build();

		// END: TEST 1

		// BEGIN: TEST 2 - Usage of Join, Reduce, Map, Cross, CoGroup

		// Collect customers keys from customers that ever placed orders
		JoinOperator customersWithOrders = JoinOperator.builder(CollectCustomerKeysWithOrders.class, IntValue.class, 0, 0)
				.input1(lineitemSource).input2(ordersSource).name("T2 Join: customers with orders").build();
		joinQuickFix(customersWithOrders);
		ReduceOperator removeDuplicates = ReduceOperator.builder(RemoveDuplicates.class, IntValue.class, 0).input(customersWithOrders)
				.name("T2 Reduce: Remove duplicates").build();

		// Cross LineItems and Orders
	//	CrossOperator lineitemsWithOrders = CrossOperator.builder(CrossJoinFields.class).input1(lineitemSource).input2(ordersSource)
	//			.name("T2 Cross: Line items with orders").build();

		// Filter customer key
	//	MapOperator customerKeyWithOrders2 = MapOperator.builder(FilterCustomerKeyFromLineItemsOrders.class).input(lineitemsWithOrders)
	//			.name("T2 Map: Customer key with orders 2").build();
		JoinOperator customerKeyWithOrders2 = JoinOperator.builder(new JoinFunction() {
			private static final long serialVersionUID = 1L;
			private Record outRec = new Record();
			@Override
			public void join(Record value1, Record value2, Collector<Record> out)
					throws Exception {
				// pass o_custkey
				outRec.setField(0, value2.getField(1, IntValue.class));
				out.collect(outRec);
			}
		}, IntValue.class, 0, 0).input1(lineitemSource).input2(ordersSource).build();
		
		ReduceOperator removeDuplicates2 = ReduceOperator.builder(RemoveDuplicates.class, IntValue.class, 0).input(customerKeyWithOrders2)
				.name("T2 Reduce: Remove duplicates 2").build();

		// Save test results to disk
		FileDataSink test2Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test2.tbl",
				"T2: Result Test 2");
		test2Sink.addInput(removeDuplicates2);
		CsvOutputFormat.configureRecordFormat(test2Sink).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0);

		// Test: Compare customer keys
		CoGroupOperator testCustomerIdentity2 = CoGroupOperator.builder(CoGroupTestIdentity.class, IntValue.class, 0, 0).name("TEST 2")
				.input1(removeDuplicates).input2(removeDuplicates2).name("T2 CoGroup: Compare customers' identities")
				.build();

		// END: TEST 2

		// BEGIN: TEST 3 - Usage of Delta Iterations to determine customers with no orders
		DeltaIteration iteration = new DeltaIteration(0);
		iteration.setMaximumNumberOfIterations(10000); // Exception otherwise

		// Add a flag field to each customer (initial value: false)
		MapOperator customersWithFlag = MapOperator.builder(AddFlag.class).input(customerSource).name("T3 Map: " +
				"Customer with flag").build();

		iteration.setInitialSolutionSet(customersWithFlag);
		iteration.setInitialWorkset(customersWithFlag);

		// As input for each iteration
		// Exception otherwise
		JoinOperator iterationInput = JoinOperator.builder(WorkSolutionSetJoin.class, IntValue.class, 0, 0).name("JOIN ITERATION")
				.input1(iteration.getWorkset()).input2(iteration.getSolutionSet()).name("T3 Join: Iteration " +
								"input").build();

		// Pick one customer from working set
		ReduceOperator oneCustomer = ReduceOperator.builder(PickOneRecord.class).input(iterationInput).name("T3 " +
				"Reduce: One customer").build();

		// Determine all customers from input with no orders (in this case:
		// check if the picked customer has no orders)
		CoGroupOperator customerWithNoOrders = CoGroupOperator.builder(CustomersWithNoOrders.class, IntValue.class, 0, 1)
				.input1(oneCustomer).input2(ordersSource).name("T3 CoGroup: Customers with no orders").build();

		// Set the flag for the customer with no order
		MapOperator customerWithSetFlag = MapOperator.builder(SetFlag.class).input(customerWithNoOrders).name("T3 " +
				"Map: Customers with set flag").build();

		// Set changed customers (delta)
		iteration.setSolutionSetDelta(customerWithSetFlag);

		// Remove checked customer from previous working set
		CoGroupOperator filteredWorkset = CoGroupOperator.builder(RemoveCheckedCustomer.class, IntValue.class, 0, 0)
				.input1(iteration.getWorkset()).input2(oneCustomer).name("T3 CoGroup: Filtered workset").build();

		// Define workset for next iteration
		iteration.setNextWorkset(filteredWorkset);

		// Remove unflagged customer
		MapOperator filteredFlaggedSolutionSet = MapOperator.builder(FilterFlaggedCustomers.class).input(iteration)
				.name("T3 Map: filtered flagged solution set").build();

		// Extract only the customer keys
		MapOperator customerKeysWithNoOrders = MapOperator.builder(FilterFirstFieldIntKey.class).input
				(filteredFlaggedSolutionSet).name("T3 Map: Customer keys with no orders").build();

		// Save the customers without orders in file
		FileDataSink test3Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test3.tbl",
				"T3: Result Test 3");
		test3Sink.addInput(customerKeysWithNoOrders);
		CsvOutputFormat.configureRecordFormat(test3Sink).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0);

		// Union all customers WITH orders from previous test with all customers WITHOUT orders
		MapOperator unionCustomers = MapOperator.builder(IdentityMapper.class).input(customerKeysWithNoOrders, testCustomerIdentity2)
				.name("T3 Map: Union customers").build();

		// Filter for customers keys of test 1
		MapOperator allCustomerKeys = MapOperator.builder(FilterFirstFieldIntKey.class).input(testCustomerIdentity1)
				.name("T3 Map: All customers' keys").build();

		// Test if unionCustomers contains all customers again
		CoGroupOperator testCustomerIdentity3 = CoGroupOperator.builder(CoGroupTestIdentity.class, IntValue.class, 0, 0)
				.input1(unionCustomers).input2(allCustomerKeys).name("T3 CoGroup: Test customers' identities").build();
		// END: TEST 3

		// BEGIN: TEST 4 - Usage of TextInputFormat

		// Get all order keys by joining with all customers that placed orders from previous test
		JoinOperator allOrderKeys = JoinOperator.builder(OrderKeysFromCustomerKeys.class, IntValue.class, 0, 1)
				.input1(testCustomerIdentity3).input2(ordersSource).name("T4 Join: All order keys").build();

		// Get the string lines of the orders file
		FileDataSource ordersTextInputSource = new FileDataSource(new TextInputFormat(), orders,
				"T4 Source: Orders text input");

		// Extract order keys out of string lines
		MapOperator stringExtractKeys = MapOperator.builder(ExtractKeysFromTextInput.class).input
				(ordersTextInputSource).name("T4 Map: String extract keys").build();

		// Save the orders in file
		FileDataSink test4Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test4.tbl",
				"T4: Result Test 4");
		test4Sink.addInput(stringExtractKeys);
		CsvOutputFormat.configureRecordFormat(test4Sink).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0);

		// Test if extracted values are correct
		CoGroupOperator testOrderIdentity = CoGroupOperator.builder(CoGroupTestIdentity.class, IntValue.class, 0, 0)
				.input1(allOrderKeys).input2(stringExtractKeys).name("T4 CoGroup: Test orders' identity").build();

		// END: TEST 4

		// BEGIN: TEST 5 - Usage of AvroInputFormat

		// extract orders from avro file
		FileDataSource ordersAvroInputSource = new FileDataSource(new AvroRecordInputFormat(), orderAvroFile,
				"T5 Source: Orders avro input");

		// Extract keys
		MapOperator extractKeys = MapOperator.builder(FilterFirstFieldIntKey.class).input(ordersAvroInputSource)
				.name("T5 Map: Extract keys").build();

		// Save the order keys in file
		FileDataSink test5Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test5.tbl",
				"T5: Result Test 5");
		test5Sink.addInput(extractKeys);
		CsvOutputFormat.configureRecordFormat(test5Sink).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0);

		CoGroupOperator testOrderIdentity2 = CoGroupOperator.builder(CoGroupTestIdentity.class, IntValue.class, 0, 0).name("TEST 5")
				.input1(testOrderIdentity).input2(extractKeys).name("T5 CoGroup: Test orders' identity").build();

		// END: TEST 5

		// BEGIN: TEST 6 - date count

		// Count different order dates
		MapOperator orderDateCountMap = MapOperator.builder(OrderDateCountMap.class).input(ordersAvroInputSource)
				.name("T6 Map: Order data count map").build();

		// Sum up
		ReduceOperator orderDateCountReduce = ReduceOperator.builder(OrderDateCountReduce.class).keyField(StringValue.class, 0)
				.input(orderDateCountMap).name("T6 Reduce: Order date count reduce").build();

		// Save the orders in file
		FileDataSink test6Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test6.tbl",
				"T6: Result Test 6");
		test6Sink.addInput(orderDateCountReduce);
		CsvOutputFormat.configureRecordFormat(test6Sink).recordDelimiter('\n').fieldDelimiter('|').field(StringValue.class, 0)
				.field(IntValue.class, 1);

		// do the same with the original orders file

		// Count different order dates
		MapOperator orderDateCountMap2 = MapOperator.builder(OrderDateCountMap.class).input(ordersSource).name("T6 " +
				"Map: Order data count map 2").build();

		// Sum up
		ReduceOperator orderDateCountReduce2 = ReduceOperator.builder(OrderDateCountReduce.class).keyField(StringValue.class, 0)
				.input(orderDateCountMap2).name("T6 Reduce: Order data count reduce 2").build();

		// Check if date count is correct
		CoGroupOperator testOrderIdentity3 = CoGroupOperator.builder(CoGroupTestIdentity.class, StringValue.class, 0,
				0).name("T6 CoGroup: Test orders' identity")
				.input1(orderDateCountReduce).input2(orderDateCountReduce2).build();

		// END: TEST 6

		// BEGIN: TEST 7

		// Sum up counts
		ReduceOperator sumUp = ReduceOperator.builder(SumUpDateCounts.class).input(testOrderIdentity3).name("T7 " +
				"Reduce: Sum up").build();

		// Count all orders
		ReduceOperator orderCount = ReduceOperator.builder(ReduceCounter.class).input(testOrderIdentity2).name("T7 " +
				"Reduce: Order count").build();

		// Check if the values are equal
		CoGroupOperator testCountOrdersIdentity = CoGroupOperator.builder(CoGroupTestIdentity.class, IntValue.class, 0, 0).name("TEST 7")
				.input1(sumUp).input2(orderCount).name("T7 CoGroup: Test count orders' identity").build();

		// Write count to disk
		FileDataSink test7Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test7.tbl",
				"T7: Result Test 7");
		test7Sink.addInput(testCountOrdersIdentity);
		CsvOutputFormat.configureRecordFormat(test7Sink).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0);

		// END: TEST 7

		//		// BEGIN: TEST 8 - HadoopDataSource with SequenceFile
		//		JobConf jobConf = new JobConf();
		//		FileInputFormat.addInputPath(jobConf, new Path(sequenceFileInput));
		//		//  with Stratosphere type converter
		//		HadoopDataSource<LongWritable, Text> hdsrc = new HadoopDataSource<LongWritable, Text>(new SequenceFileInputFormat<LongWritable, Text>(), jobConf, "Sequencefile");
		//		MapOperator checkHDsrc = MapOperator.builder(CheckHadoop.class).input(hdsrc).name("Check HDSrc output").build();
		//
		//		HadoopDataSource<LongWritable, Text> hdsrcWrapperConverter = new HadoopDataSource<LongWritable, Text>(new SequenceFileInputFormat<LongWritable, Text>(), jobConf,
		//				"Sequencefile", new WritableWrapperConverter<LongWritable, Text>());
		//		MapOperator checkHDsrcWrapperConverter = MapOperator.builder(CheckHadoopWrapper.class).input(hdsrcWrapperConverter)
		//				.name("Check HDSrc output").build();
		//		// END: TEST 8
		//
		//		// don't use this for serious output.
		//		FileDataSink fakeSink = new FileDataSink(FailOutOutputFormat.class, "file:///tmp/fakeOut", "fake out");
		//		fakeSink.addInput(checkHDsrc);
		//		fakeSink.addInput(checkHDsrcWrapperConverter);

		// BEGIN: TEST 9 - Usage of Broadcast Variables

		// Join Customer and Nation using Broadcast Variables
		MapOperator broadcastJoinNation = MapOperator.builder(BroadcastJoinNation.class).setBroadcastVariable("nations", nationSource)
				.input(customerSource).name("T9 Map: Broadcast join nation").build();

		// Join Customer, Nation and Region using Broadcast Variables
		MapOperator broadcastJoinRegion = MapOperator.builder(BroadcastJoinRegion.class).setBroadcastVariable("regions", regionSource)
				.input(broadcastJoinNation).name("T9 Map: broadcast join region").build();

		CoGroupOperator testEquality = CoGroupOperator.builder(FieldEqualityTest.class, IntValue.class, 0, 0).name("TEST 9")
				.input1(customerWithNationRegion).input2(broadcastJoinRegion).name("T9 CoGroup: Test equality").build();

		MapOperator customerFields = MapOperator.builder(FilterCustomerFields.class).input(testEquality).name("T9 " +
				"Map: Customer fields").build();

		// Save test results to disk
		FileDataSink test9Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test9.tbl",
				"T9: Result Test 9");
		test9Sink.addInput(customerFields);
		CsvOutputFormat.configureRecordFormat(test9Sink).recordDelimiter('\n').fieldDelimiter('|').field(IntValue.class, 0)
				.field(StringValue.class, 1).field(StringValue.class, 2).field(IntValue.class, 3).field(StringValue.class, 4)
				.field(DoubleValue.class, 5).field(StringValue.class, 6).field(StringValue.class, 7);

		// END: TEST 9

		// BEGIN: TEST 10 - Usage of BulkIterations and Broadcast Variables

		// the partial solution is the record with the currently highest found total price
		// the total price field in the partial solution increases from iteration step to iteration step until it converges
		BulkIteration bulkIteration = new BulkIteration();
		bulkIteration.setMaximumNumberOfIterations(maxBulkIterations);

		// pick the first price for use as highest price
		ReduceOperator firstPrice = ReduceOperator.builder(PickOneRecord.class).input(ordersSource).name("T10 Reduce:" +
				"First price").build();
		bulkIteration.setInput(firstPrice);

		// begin of iteration step

		// Determine the higher price
		ReduceOperator higherPrice = ReduceOperator.builder(TakeFirstHigherPrice.class)
				.setBroadcastVariable("currently_highest_price", bulkIteration.getPartialSolution()).input
						(ordersSource).name("T10 Reduce: Higher price").build();

		bulkIteration.setNextPartialSolution(higherPrice);

		// determine maximum total price
		ReduceOperator orderWithMaxPrice = ReduceOperator.builder(MaximumReducer.class).input(ordersSource).name
				("T10 Reduce: Order with max price").build();

		CoGroupOperator testOrderIdentity4 = CoGroupOperator.builder(CoGroupTestIdentity.class, IntValue.class, 0, 0).
				input1(orderWithMaxPrice).input2(bulkIteration).name("T10 CoGroup: Test order identity").build();

		// Save the order keys in file
		FileDataSink test10Sink = new FileDataSink(new CsvOutputFormat(), outputTableDirectory + "/Test10.tbl",
				"T10: Result Test 10");
		test10Sink.addInput(testOrderIdentity4);
		CsvOutputFormat.configureRecordFormat(test10Sink).recordDelimiter('\n').fieldDelimiter('|').field(DoubleValue.class, 3);

		// END: TEST 10

		Plan p = new Plan(resultKR, "Large Test Plan");
		p.addDataSink(test1Sink);
		p.addDataSink(test2Sink);
		p.addDataSink(test3Sink);
		p.addDataSink(test4Sink);
		p.addDataSink(test5Sink);
		p.addDataSink(test6Sink);
		p.addDataSink(test7Sink);
		p.addDataSink(test9Sink);
		p.addDataSink(test10Sink);
		p.setDefaultParallelism(dop);
		return p;
	}

	@Override
	public String getDescription() {
		return "Parameters: [customer] [lineitem] [nation] [orders] [region] [orderAvroFile] [outputTableDirectory]";
	}

	// Quick fix for Join bug
	private void joinQuickFix(JoinOperator j) {
		//j.setParameter(PactCompiler.HINT_LOCAL_STRATEGY, PactCompiler.HINT_LOCAL_STRATEGY_MERGE);
	}

	public static class FailOutOutputFormat extends FileOutputFormat {
		public void writeRecord(Record record) throws IOException {
			throw new RuntimeException("it is not expected to write anything to that sink. Possible bug?");
		}

	}

	public static class CheckHadoop extends MapFunction {
		private static final long serialVersionUID = 1L;
		LongCounter cnt;

		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			cnt = getRuntimeContext().getLongCounter("Hadoop Sequencefile KV Counter");
		}

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			cnt.add(1L);
			LongValue key = record.getField(0, LongValue.class);
			StringValue val = record.getField(1, StringValue.class);
			if (!Long.toString(key.getValue()).equals(val.getValue().split("-")[0])) {
				throw new RuntimeException("KV typle's key does not match with value");
			}
			// we do not collect the output!
		}
	}

	public static class CheckHadoopWrapper extends MapFunction {
		private static final long serialVersionUID = 1L;
		LongCounter cnt;
		boolean collecting = false;
		public CheckHadoopWrapper() {
			
		}
		
		public CheckHadoopWrapper(boolean collecting) {
			this.collecting = collecting;
		}
		
		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			cnt = getRuntimeContext().getLongCounter("Hadoop Sequencefile KV Counter (Wrapper)");
		}

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			cnt.add(1L);
			LongWritable key = (LongWritable) record.getField(0, WritableWrapper.class).value();
			Text value = (Text) record.getField(1, WritableWrapper.class).value();
			String k = Long.toString(key.get());
			String v = value.toString().split("-")[0];
			if (!k.equals(v)) {
				throw new RuntimeException("KV typle's key does not match with value");
			}
			if(collecting) {
				Record r = new Record();
				r.addField(new LongValue(key.get()));
				r.addField(new StringValue(value.toString()));
				out.collect(r);
			}
		}
	}

	// Joins the fields of two record into one record
	public static class JoinFields extends JoinFunction {

		@Override
		public void join(Record r1, Record r2, Collector<Record> out) throws Exception {

			Record newRecord = new Record(r1.getNumFields() + r2.getNumFields());

			int[] r1Positions = new int[r1.getNumFields()];
			for (int i = 0; i < r1Positions.length; ++i) {
				r1Positions[i] = i;
			}
			newRecord.copyFrom(r1, r1Positions, r1Positions);

			int[] r2Positions = new int[r2.getNumFields()];
			int[] targetR2Positions = new int[r2.getNumFields()];
			for (int i = 0; i < r2Positions.length; ++i) {
				r2Positions[i] = i;
				targetR2Positions[i] = i + r1Positions.length;
			}
			newRecord.copyFrom(r2, r2Positions, targetR2Positions);

			out.collect(newRecord);
		}

	}

	// Filter for region "AMERICA"
	public static class FilterRegion extends MapFunction {

		private IntCounter numLines = new IntCounter();
		final String regionName;

		public FilterRegion(String rN) {
			this.regionName = rN;
		}

		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			getRuntimeContext().addAccumulator("count-american-customers", this.numLines);
		}

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			if (record.getField(13, StringValue.class).toString().equals(regionName)) {
				out.collect(record);
				this.numLines.add(1);
			}
		}

	}

	// Filter for regions other than "AMERICA" and "EUROPE"
	public static class FilterRegionOthers extends MapFunction {

		private IntCounter numLines = new IntCounter();

		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			getRuntimeContext().addAccumulator("count-rest-customers", this.numLines);
		}

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			if (!record.getField(13, StringValue.class).toString().equals("AMERICA")
					&& !record.getField(13, StringValue.class).toString().equals("EUROPE")) {
				out.collect(record);
				this.numLines.add(1);
			}

		}

	}

	// Extract customer fields out of customer-nation-region record
	public static class FilterCustomerFields extends MapFunction {

		@Override
		public void map(Record cnr, Collector<Record> out) throws Exception {
			Record newRecord = new Record(8);
			int[] positions = new int[8];
			for (int i = 0; i < positions.length; ++i) {
				positions[i] = i;
			}
			newRecord.copyFrom(cnr, positions, positions);
			out.collect(newRecord);
		}

	}

	// Test if each key has an equivalent key and fields of both inputs are equals
	public static class CoGroupTestIdentity extends CoGroupFunction {

		@Override
		public void coGroup(Iterator<Record> records1, Iterator<Record> records2, Collector<Record> out) throws Exception {

			int count1 = 0;
			Record lastR1 = null;
			while (records1.hasNext()) {
				lastR1 = records1.next();
				count1++;
			}

			int count2 = 0;
			Record lastR2 = null;
			while (records2.hasNext()) {
				lastR2 = records2.next();
				count2++;
			}

			if (count1 != 1 || count2 != 1) {				
				throw new Exception(getRuntimeContext().getTaskName()+" FAILED: The count of the two inputs do not match: " + count1 + " / " + count2+"\n"+
				((lastR1!=null)?"LAST R1: "+lastR1.getField(0, IntValue.class):"NO LAST R1.")
				+"\n"+
				((lastR2!=null)?"LAST R2: "+lastR2.getField(0, IntValue.class):"NO LAST R2."));
			}

			if (lastR1.getNumFields() != lastR2.getNumFields()) {
				throw new Exception(getRuntimeContext().getTaskName()+" FAILED: The number of fields of the two inputs do not match: " + lastR1.getNumFields() + " / "
						+ lastR2.getNumFields());
			}
			out.collect(lastR2);
		}

	}

	// Join LineItems with Orders, collect all customer keys with orders
	// (records from LineItems is are not used, result contains duplicates)
	public static class CollectCustomerKeysWithOrders extends JoinFunction {

		@Override
		public void join(Record l, Record o, Collector<Record> out) throws Exception {
			out.collect(new Record(o.getField(1, IntValue.class)));
		}

	}

	// Removes duplicate keys
	public static class RemoveDuplicates extends ReduceFunction {

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {
			Record record = records.next();
			out.collect(record);
		}
	}

	// Crosses two input streams and returns records with merged fields
	public static class CrossJoinFields extends CrossFunction {

		@Override
		public Record cross(Record r1, Record r2) throws Exception {
			Record newRecord = new Record(r1.getNumFields() + r2.getNumFields());

			int[] r1Positions = new int[r1.getNumFields()];
			for (int i = 0; i < r1Positions.length; ++i) {
				r1Positions[i] = i;
			}
			newRecord.copyFrom(r1, r1Positions, r1Positions);

			int[] r2Positions = new int[r2.getNumFields()];
			int[] targetR2Positions = new int[r2.getNumFields()];
			for (int i = 0; i < r2Positions.length; ++i) {
				r2Positions[i] = i;
				targetR2Positions[i] = i + r1Positions.length;
			}
			newRecord.copyFrom(r2, r2Positions, targetR2Positions);

			return newRecord;
		}

	}

	// Filters the customer key from the LineItem-Order records
	public static class FilterCustomerKeyFromLineItemsOrders extends MapFunction {

		@Override
		public void map(Record lo, Collector<Record> out) throws Exception {
			if (lo.getField(0, IntValue.class).getValue() == lo.getField(16, IntValue.class).getValue()) {
				out.collect(new Record(lo.getField(17, IntValue.class)));
			}

		}

	}

	// Counts the input records
	public static class ReduceCounter extends ReduceFunction {

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {

			int counter = 0;

			while (records.hasNext()) {
				records.next();
				counter++;
			}
			out.collect(new Record(new IntValue(counter)));
		}

	}

	// Gets all order keys of a customer key
	public static class OrderKeysFromCustomerKeys extends JoinFunction {

		@Override
		public void join(Record c, Record o, Collector<Record> out) throws Exception {
			out.collect(new Record(o.getField(0, IntValue.class)));
		}
	}

	// Parses the first key from a string line
	public static class ExtractKeysFromTextInput extends MapFunction {

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			String line = record.getField(0, StringValue.class).getValue();
			@SuppressWarnings("resource")
			Scanner s = new Scanner(line).useDelimiter("\\|");
			int orderKey = s.nextInt();
			out.collect(new Record(new IntValue(orderKey)));
			s.close();
		}

	}

	// Creates string/integer pairs of order dates
	public static class OrderDateCountMap extends MapFunction {

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			out.collect(new Record(record.getField(4, StringValue.class), new IntValue(1)));
		}

	}

	// Sums up the counts for a certain given order date 
	public static class OrderDateCountReduce extends ReduceFunction {

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {
			Record element = null;
			int sum = 0;
			while (records.hasNext()) {
				element = records.next();
				int cnt = element.getField(1, IntValue.class).getValue();
				sum += cnt;
			}

			element.setField(1, new IntValue(sum));
			out.collect(element);
		}
	}

	// Sum up all date counts
	public static class SumUpDateCounts extends ReduceFunction {

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {
			int count = 0;
			while (records.hasNext()) {
				count += records.next().getField(1, IntValue.class).getValue();
			}
			out.collect(new Record(new IntValue(count)));
		}
	}

	// Join which directly outputs the Workset (only necessary to fulfill iteration constraints)
	public static class WorkSolutionSetJoin extends JoinFunction {

		@Override
		public void join(Record worksetC, Record solutionC, Collector<Record> out) throws Exception {
			out.collect(worksetC);
		}

	}

	// Outputs the first record of the input stream
	public static class PickOneRecord extends ReduceFunction {

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {
			if (records.hasNext()) {
				out.collect(records.next());
			}
			while (records.hasNext())
				records.next();
		}

	}

	// Returns only Customers that have no matching Order
	public static class CustomersWithNoOrders extends CoGroupFunction {

		@Override
		public void coGroup(Iterator<Record> c, Iterator<Record> o, Collector<Record> out) throws Exception {

			// if no order is present output customer
			if (c.hasNext() && !o.hasNext()) {
				out.collect(c.next());
			}
		}

	}

	// Adds a flag field to each record.
	public static class AddFlag extends MapFunction {

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			record.addField(new BooleanValue(false));
			out.collect(record);
		}

	}

	// Sets the last (Boolean) flag to "true".
	public static class SetFlag extends MapFunction {

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			record.setField(record.getNumFields() - 1, new BooleanValue(true));
			out.collect(record);
		}

	}

	// Only return customers that are not in input2
	public static class RemoveCheckedCustomer extends CoGroupFunction {

		@Override
		public void coGroup(Iterator<Record> workingSet, Iterator<Record> checkedCustomer, Collector<Record> out) throws Exception {
			if (!checkedCustomer.hasNext()) {
				while (workingSet.hasNext())
					out.collect(workingSet.next());
			}
		}

	}

	// Returns all customers with set flag
	public static class FilterFlaggedCustomers extends MapFunction {

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			if (record.getField(record.getNumFields() - 1, BooleanValue.class).getValue()) {
				out.collect(record);
			}
		}

	}

	// Returns only the first integer field as record
	public static class FilterFirstFieldIntKey extends MapFunction {

		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			out.collect(new Record(record.getField(0, IntValue.class)));
		}
	}

	// Dummy mapper. For testing purposes.
	public static class IdentityMapper extends MapFunction {
		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			out.collect(record);
		}
	}

	// Joins customer with a nation records
	public static class BroadcastJoinNation extends MapFunction {
		@Override
		public void map(Record customer, Collector<Record> out) throws Exception {

			Collection<Record> nations = getRuntimeContext().getBroadcastVariable("nations");

			for (Record nation : nations) {
				int nationKey = nation.getField(0, IntValue.class).getValue();
				int customerNationKey = customer.getField(3, IntValue.class).getValue();

				if (nationKey == customerNationKey) {

					Record newRecord = new Record(customer.getNumFields() + nation.getNumFields());

					int[] r1Positions = new int[customer.getNumFields()];
					for (int i = 0; i < r1Positions.length; ++i) {
						r1Positions[i] = i;
					}
					newRecord.copyFrom(customer, r1Positions, r1Positions);

					int[] r2Positions = new int[nation.getNumFields()];
					int[] targetR2Positions = new int[nation.getNumFields()];
					for (int i = 0; i < r2Positions.length; ++i) {
						r2Positions[i] = i;
						targetR2Positions[i] = i + r1Positions.length;
					}
					newRecord.copyFrom(nation, r2Positions, targetR2Positions);

					out.collect(newRecord);
				}
			}

		}
	}

	// Joins customer-nation with a region records
	public static class BroadcastJoinRegion extends MapFunction {
		@Override
		public void map(Record customerNation, Collector<Record> out) throws Exception {

			Collection<Record> regions = getRuntimeContext().getBroadcastVariable("regions");

			for (Record region : regions) {
				int regionKey = region.getField(0, IntValue.class).getValue();
				int customerNationRegionKey = customerNation.getField(10, IntValue.class).getValue();

				if (regionKey == customerNationRegionKey) {

					Record newRecord = new Record(customerNation.getNumFields() + region.getNumFields());

					int[] r1Positions = new int[customerNation.getNumFields()];
					for (int i = 0; i < r1Positions.length; ++i) {
						r1Positions[i] = i;
					}
					newRecord.copyFrom(customerNation, r1Positions, r1Positions);

					int[] r2Positions = new int[region.getNumFields()];
					int[] targetR2Positions = new int[region.getNumFields()];
					for (int i = 0; i < r2Positions.length; ++i) {
						r2Positions[i] = i;
						targetR2Positions[i] = i + r1Positions.length;
					}
					newRecord.copyFrom(region, r2Positions, targetR2Positions);

					out.collect(newRecord);
				}
			}

		}
	}

	// Checks the equality of some fields of customer-nation-region record
	public static class FieldEqualityTest extends CoGroupFunction {

		@Override
		public void coGroup(Iterator<Record> records1, Iterator<Record> records2, Collector<Record> out) throws Exception {
			Record r1 = null;
			Record r2 = null;

			boolean failed = false;
			while (records1.hasNext() && records2.hasNext()) {
				r1 = records1.next();
				r2 = records2.next();

				// check customer name equality
				if (!r1.getField(1, StringValue.class).getValue().equals(r2.getField(1, StringValue.class).getValue())) {
					failed = true;
				}

				// check nation name equality
				if (!r1.getField(9, StringValue.class).getValue().equals(r2.getField(9, StringValue.class).getValue())) {
					failed = true;
				}

				// check region name equality
				if (!r1.getField(13, StringValue.class).getValue().equals(r2.getField(13, StringValue.class).getValue())) {
					failed = true;
				}

				out.collect(r2);
			}
			if (records1.hasNext() != records2.hasNext())
				failed = true;

			if (failed)
				throw new Exception("TEST FAILED: The records seem not to be equal.");
		}
	}

	public static class TakeFirstHigherPrice extends ReduceFunction {

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {
			
			Collection<Record> vars = getRuntimeContext().getBroadcastVariable("currently_highest_price");
			Iterator<Record> iterator = vars.iterator();
			
			// Prevent bug in Iteration maxIteration+1
			if(!iterator.hasNext()) {
				return;
			}
			Record currHighestRecord = iterator.next();
			
			
			double currHighest = currHighestRecord.getField(3, DoubleValue.class).getValue();

			Record i = null;
			boolean collected = false;
			while (records.hasNext()) {
				i = records.next();
				double totalPrice = i.getField(3, DoubleValue.class).getValue();

				if (totalPrice > currHighest) {
					out.collect(i);
					collected = true;
					break;
				}
			}
			if (!collected) {
				out.collect(currHighestRecord.createCopy());
			}

			// Quick fix for bug
			while(records.hasNext()) records.next();
		}

	}

	public static class MaximumReducer extends ReduceFunction {

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {
			double max = Double.MIN_VALUE;
			Record maxRecord = null;

			while (records.hasNext()) {
				Record r = records.next();
				double value = r.getField(3, DoubleValue.class).getValue();
				if (max < value) {
					max = value;
					maxRecord = r.createCopy();
				}
			}
			out.collect(maxRecord);
		}
	}

}

