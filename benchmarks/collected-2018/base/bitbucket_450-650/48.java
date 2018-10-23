// https://searchcode.com/api/result/48555508/

/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.journalpaper.tpch.pact;

import java.util.Iterator;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.MatchContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.contract.ReduceContract.Combinable;
import eu.stratosphere.pact.common.io.RecordInputFormat;
import eu.stratosphere.pact.common.io.RecordOutputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.plan.PlanAssemblerDescription;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.MatchStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFields;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFieldsExcept;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFieldsFirstExcept;
import eu.stratosphere.pact.common.stubs.StubAnnotation.OutCardBounds;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactDouble;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.common.type.base.PactLong;
import eu.stratosphere.pact.common.type.base.PactString;
import eu.stratosphere.pact.common.type.base.parser.DecimalTextDoubleParser;
import eu.stratosphere.pact.common.type.base.parser.DecimalTextIntParser;
import eu.stratosphere.pact.common.type.base.parser.DecimalTextLongParser;
import eu.stratosphere.pact.common.type.base.parser.VarLengthStringParser;
import eu.stratosphere.pact.common.util.FieldSet;

/**
 * The TPC-H is a decision support benchmark on relational data.
 * Its documentation and the data generator (DBGEN) can be found
 * on http://www.tpc.org/tpch/. This implementation is tested with
 * the DB2 data format.
 * 
 * The PACT program implements a slightly modified version of the query 3 where 
 * the limit clause is omitted.
 * 
 * SELECT
 *     l.l_orderkey,
 *     SUM(l.l_extendedprice * (1 - l.l_discount)) AS revenue,
 *     o.o_orderdate,
 *     o.o_shippriority
 * FROM
 *     customer c
 *     JOIN orders o ON (c.c_custkey = o.o_custkey)
 *     JOIN lineitem l ON (l.l_orderkey = o.o_orderkey)
 * WHERE
 *     c.c_mktsegment = ':1'
 *     AND o.o_orderdate < ':2'
 *     AND l.l_shipdate > ':2'
 * GROUP BY
 *     l.l_orderkey,
 *     o.o_orderdate,
 *     o.o_shippriority
 * SORT BY
 *     revenue DESC,
 *     o_orderdate ASC;
 *     
 * Example command line arguments:
 *   
 *     8 hdfs://localhost:9000/input/tpch hdfs://localhost:9000/output
 */
public class TPCHQuery3 implements PlanAssembler, PlanAssemblerDescription {

	public static final String ORDERDATE_FILTER = "parameter.ORDERDATE_FILTER";
    public static final String SHIPDATE_FILTER = "parameter.SHIPDATE_FILTER";
    public static final String MKTSEGMENT_FILTER = "parameter.MKTSEGMENT_FILTER";

    /**
     * Map PACT implements the selection and projection on the orders table.
     */
    @ConstantFieldsExcept(fields={})
    @OutCardBounds(upperBound=1, lowerBound=0)
    public static class FilterO extends MapStub
    {
        private PactString orderdateFilter;            // filter literal for the year
        
        // reusable objects for the fields touched in the mapper
        private PactString orderDate = new PactString();
        
        /**
         * Reads the filter literals from the configuration.
         * 
         * @see eu.stratosphere.pact.common.stubs.Stub#open(eu.stratosphere.nephele.configuration.Configuration)
         */
        @Override
        public void open(Configuration parameters) {
            this.orderdateFilter = new PactString(parameters.getString(ORDERDATE_FILTER, "1995-03-15"));
        }
    
        /**
         * Filters the orders table by orderdate.
         *
         *  o_orderdate < "X"
         * 
         * Output Schema: 
         *   0:ORDERKEY,
         *   1:CUSTKEY,
         *   2:ORDERDATE,
         *   3:SHIPPRIORITY
         */
        @Override
        public void map(final PactRecord record, final Collector<PactRecord> out)
        {
        	this.orderDate = record.getField(2, this.orderDate);
            if (this.orderDate.compareTo(this.orderdateFilter) >= 0)
                return;
            
            out.collect(record);
        }
    }

    /**
     * Map PACT implements the selection and projection on the lineitems table.
     */
    @ConstantFieldsExcept(fields={1,2, 3})
    @OutCardBounds(upperBound=1, lowerBound=0)
    public static class FilterL extends MapStub
    {
        private PactString shipdateFilter;            // filter literal for the year
        
        // reusable objects for the fields touched in the mapper
        private PactString shipDate = new PactString();
        private PactDouble extPrice = new PactDouble();
        private PactDouble discount = new PactDouble();
        private PactDouble revenue = new PactDouble();
        
        /**
         * Reads the filter literals from the configuration.
         * 
         * @see eu.stratosphere.pact.common.stubs.Stub#open(eu.stratosphere.nephele.configuration.Configuration)
         */
        @Override
        public void open(Configuration parameters) {
            this.shipdateFilter = new PactString(parameters.getString(SHIPDATE_FILTER, "1995-03-15"));
        }
    
        /**
         * Filters the orders table by lineitem.
         *
         *  l_shipdate > "X"
         * 
         * Output Schema: 
         *   0:ORDERKEY, 
         *   1:REVENUE,
         */
        @Override
        public void map(final PactRecord record, final Collector<PactRecord> out)
        {
            this.shipDate = record.getField(3, this.shipDate);
            if (this.shipDate.compareTo(this.shipdateFilter) <= 0)
                return;
            
            this.extPrice = record.getField(1, this.extPrice);
            this.discount = record.getField(2, this.discount);
            this.revenue.setValue(this.extPrice.getValue() * (1 - this.discount.getValue()));

            record.setField(1, this.revenue);
            record.setNull(2);
            record.setNull(3);
    
            out.collect(record);
        }
    }

    /**
     * Map PACT implements the selection and projection on the customers table.
     */
    @ConstantFieldsExcept(fields={1})
    @OutCardBounds(upperBound=1, lowerBound=0)
    public static class FilterC extends MapStub
    {
        private PactString mktSegmentFilter;        // filter literal for the order priority
        
        // reusable objects for the fields touched in the mapper
        private PactString mktSegment = new PactString();
        
        /**
         * Reads the filter literals from the configuration.
         * 
         * @see eu.stratosphere.pact.common.stubs.Stub#open(eu.stratosphere.nephele.configuration.Configuration)
         */
        @Override
        public void open(Configuration parameters) {
            this.mktSegmentFilter = new PactString(parameters.getString(MKTSEGMENT_FILTER, "HOUSEHOLD"));
        }
    
        /**
         * Filters the customer table by mktsegment.
         *
         *  c_mktsegment = "X" 
         *  
         * Output Schema: 
         *   0:CUSTKEY
         */
        @Override
        public void map(final PactRecord record, final Collector<PactRecord> out)
        {
            this.mktSegment = record.getField(1, this.mktSegment);
            if (!this.mktSegment.equals(this.mktSegmentFilter))
                return;
            
            record.setNull(1);
            out.collect(record);
        }
    }

    /**
     * Match PACT realizes the join between the filtered Order and Customer tables.
     *
     */
    @ConstantFieldsFirstExcept(fields={})
    @OutCardBounds(lowerBound=1, upperBound=1)
    public static class JoinOC extends MatchStub
    {
        /**
         * Implements the join between LineItem and Order table on the order key.
         * 
         * Output Schema: 
         *   0:ORDERKEY,
         *   1:CUSTKEY,
         *   2:ORDERDATE,
         *   3:SHIPPRIORITY
         */
        @Override
        public void match(PactRecord order, PactRecord customer, Collector<PactRecord> out)
        {
//        	order.setNull(1); // we don't need custkey after that
            out.collect(order);
        }
    }

    /**
     * Match PACT realizes the join between the filtered LineItem and Order tables.
     *
     */
    @ConstantFieldsFirstExcept(fields={4})
    @OutCardBounds(lowerBound=1, upperBound=1)
    public static class JoinOLi extends MatchStub
    {
        /**
         * Implements the join between LineItem and Order table on the order key.
         * 
         * Output Schema: 
         *   0:ORDERKEY,
         *   1:CUSTKEY,
         *   2:ORDERDATE,
         *   3:SHIPPRIORITY,
         *   4:REVENUE
         */
        @Override
        public void match(PactRecord order, PactRecord lineitem, Collector<PactRecord> out)
        {
            order.setField(4, lineitem.getField(1, PactDouble.class));
            out.collect(order);
        }
    }

    /**
     * Reduce PACT implements the sum aggregation. 
     * The Combinable annotation is set as the partial sums can be calculated
     * already in the combiner
     *
     */
    @Combinable
    @ConstantFields(fields={})
    @OutCardBounds(upperBound=1, lowerBound=1)
    public static class AggLiO extends ReduceStub
    {
        private PactDouble extendedPrice = new PactDouble();
        private PactRecord aggregateRec = new PactRecord(4);
        
        /**
         * Implements the sum aggregation.
         * 
         * Output Schema: 
         *   0:ORDERKEY,
         *   1:ORDERDATE,
         *   2:SHIPPRIORITY,
         *   3:SUM(EXTENDEDPRICE)
         */
        @SuppressWarnings("null")
		@Override
        public void reduce(Iterator<PactRecord> values, Collector<PactRecord> out)
        {
            double partExtendedPriceSum = 0;

            PactRecord rec = null;
            while (values.hasNext()) {
                rec = values.next();
                partExtendedPriceSum += rec.getField(4, PactDouble.class).getValue();
            }
            this.extendedPrice.setValue(partExtendedPriceSum);

            this.aggregateRec.setField(0, rec.getField(0, PactLong.class));
            this.aggregateRec.setField(1, rec.getField(2, PactString.class));
            this.aggregateRec.setField(2, rec.getField(3, PactInteger.class));
            this.aggregateRec.setField(3, this.extendedPrice);
            
            out.collect(this.aggregateRec);
        }

        /**
         * Creates partial sums on the price attribute for each data batch.
         */
        @Override
        public void combine(Iterator<PactRecord> values, Collector<PactRecord> out)
        {
            reduce(values, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Plan getPlan(final String... args) 
    {
        // parse program parameters
        final int noSubtasks       = (args.length > 0 ? Integer.parseInt(args[0]) : 1);
        final String inputPath     = (args.length > 1 ? args[1] : "");
        final String outputPath    = (args.length > 2 ? args[2] : "");

        // create DataSourceContract for Orders input
        FileDataSource orders = new FileDataSource(RecordInputFormat.class, inputPath + "/orders.tbl", "Orders");
        RecordInputFormat.configureRecordFormat(orders)
            .recordDelimiter('\n')
            .fieldDelimiter('|')
            .field(DecimalTextLongParser.class, 0)        // order key
            .field(DecimalTextLongParser.class, 1)        // customer key
            .field(VarLengthStringParser.class, 4)        // order date
            .field(DecimalTextIntParser.class, 7);        // ship prio
        // compiler hints
        orders.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(0), 1);
        orders.getCompilerHints().setAvgBytesPerRecord(16);
        orders.getCompilerHints().setUniqueField(new FieldSet(0));

        // create DataSourceContract for LineItems input
        FileDataSource lineitems = new FileDataSource(RecordInputFormat.class, inputPath + "/lineitem.tbl", "LineItems");
        RecordInputFormat.configureRecordFormat(lineitems)
            .recordDelimiter('\n')
            .fieldDelimiter('|')
            .field(DecimalTextLongParser.class, 0)        // order key
            .field(DecimalTextDoubleParser.class, 5)      // extended price
            .field(DecimalTextDoubleParser.class, 6)      // discount
            .field(VarLengthStringParser.class, 10);      // shipdate
        // compiler hints    
        lineitems.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(0), 4);
        lineitems.getCompilerHints().setAvgBytesPerRecord(20);

        // create DataSourceContract for LineItems input
        FileDataSource customers = new FileDataSource(RecordInputFormat.class, inputPath + "/customer.tbl", "Customers");
        RecordInputFormat.configureRecordFormat(customers)
            .recordDelimiter('\n')
            .fieldDelimiter('|')
            .field(DecimalTextLongParser.class, 0)        // customer id
            .field(VarLengthStringParser.class, 6);       // market setgment
        // compiler hints    
        customers.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(0), 1);
        customers.getCompilerHints().setAvgBytesPerRecord(20);

        // create MapContract for filtering Orders tuples
        MapContract filterO = MapContract.builder(FilterO.class)
            .input(orders)
            .name("FilterO")
            .build();
        // filter configuration
        filterO.setParameter(ORDERDATE_FILTER, "1995-03-15");
        // compiler hints
        filterO.getCompilerHints().setAvgBytesPerRecord(16);
        filterO.getCompilerHints().setAvgRecordsEmittedPerStubCall(0.05f);
        filterO.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(0), 1);

        // create MapContract for filtering Lineitem tuples
        MapContract filterLi = MapContract.builder(FilterL.class)
            .input(lineitems)
            .name("FilterL")
            .build();
        // filter configuration
        filterLi.setParameter(SHIPDATE_FILTER, "1995-03-15");
        // compiler hints
        filterLi.getCompilerHints().setAvgBytesPerRecord(20);
        filterLi.getCompilerHints().setAvgRecordsEmittedPerStubCall(0.05f);
        filterLi.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(0), 1);

        // create MapContract for filtering Customer tuples
        MapContract filterC = MapContract.builder(FilterC.class)
            .input(customers)
            .name("FilterC")
            .build();
        // filter configuration
        filterC.setParameter(MKTSEGMENT_FILTER, "HOUSEHOLD");
        // compiler hints
        filterC.getCompilerHints().setAvgBytesPerRecord(20);
        filterC.getCompilerHints().setAvgRecordsEmittedPerStubCall(0.05f);
        filterC.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(0), 1);

        // create MatchContract for joining Orders and Customers
        MatchContract joinOC = MatchContract.builder(JoinOC.class, PactLong.class, 1, 0)
            .input1(filterO)
            .input2(filterC)
            .name("JoinOC")
            .build();
        // compiler hints
        joinOC.getCompilerHints().setAvgBytesPerRecord(28);
        joinOC.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(new int[]{0, 1}), 1);

        // create MatchContract for joining Orders and LineItems
        MatchContract joinOLi = MatchContract.builder(JoinOLi.class, PactLong.class, 0, 0)
            .input1(joinOC)
            .input2(filterLi)
            .name("JoinOLi")
            .build();
        // compiler hints
        joinOLi.getCompilerHints().setAvgBytesPerRecord(36);
        joinOLi.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(new int[]{0, 1}), 4);

        // create ReduceContract for aggregating the result
        ReduceContract aggLiO = ReduceContract.builder(AggLiO.class)
            .keyField(PactLong.class, 0)
            .keyField(PactString.class, 2)
            .keyField(PactInteger.class, 3)
            .input(joinOLi)
            .name("AggLio")
            .build();
        // compiler hints
        aggLiO.getCompilerHints().setAvgBytesPerRecord(30);
        aggLiO.getCompilerHints().setAvgRecordsEmittedPerStubCall(1.0f);
        aggLiO.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(new int[]{0, 1, 2}), 1);

        // TODO: add sorting after aggregation
        
        // create DataSinkContract for writing the result
        FileDataSink result = new FileDataSink(RecordOutputFormat.class, outputPath + "/tpch/q3/result.tbl", aggLiO, "Output");
        RecordOutputFormat.configureRecordFormat(result)
            .recordDelimiter('\n')
            .fieldDelimiter('|')
            .lenient(true)
            .field(PactLong.class, 0)
            .field(PactString.class, 1)
            .field(PactInteger.class, 2)
            .field(PactDouble.class, 3);
        
        // assemble the PACT plan
        Plan plan = new Plan(result, "TPCH Q3");
        plan.setDefaultParallelism(noSubtasks);
        return plan;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Parameters: [noSubStasks], [orders], [lineitem], [customer], [output]";
    }

}

