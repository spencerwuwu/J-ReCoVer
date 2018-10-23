// https://searchcode.com/api/result/38841617/

package biz.c24.io.hadoop;

/*
 * Copyright 2012 C24 Technologies.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.Assert.*;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.c24.io.fix42.ExecutionReport;
import biz.c24.io.hadoop.util.SimulatedCluster;
import biz.c24.test.IntegrationTest;

/**
 * End-to-end mapreduce test
 * 
 * @author Andrew Elmore
 *
 */
@Category(IntegrationTest.class)
public class ParseTest {

    private static Logger LOG = LoggerFactory.getLogger(ParseTest.class);

    private SimulatedCluster cluster = null;

    @Before
    public void setUp() throws Exception {
        cluster = new SimulatedCluster();
        cluster.initialise(10, 100);
    }

    @After
    public void teardown() {
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
    }

    private static String message = "8=FIX.4.29=19035=D49=Client56=TradingGateway34=452=20120601-09:36:26.63511=20120601-103623-48440481=TestAccount63=021=1111=055=MSFT54=160=20120601-10:36:26.63538=10000040=115=USD59=047=I10=072";

    public static class Mapper extends ComplexDataObjectMapper<Text, ExecutionReport, Text, IntWritable> {
        @Override
        protected void map(Text key, ExecutionReport value, Context context) throws IOException, InterruptedException {
            ExecutionReport msg = value;
            String outKey = msg.getName();
            context.write(new Text(outKey), new IntWritable(1));
        }
    }

    public static class Reducer extends org.apache.hadoop.mapreduce.Reducer<Text, IntWritable, Text, IntWritable> {
        @Override
        /**
         * Aggregates up occurrences of key
         */
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int count = 0;
            for (IntWritable value : values) {
                count += value.get();
            }
            LOG.debug("Reduced {} to {}", key, count);

            context.write(key, new IntWritable(count));
        }
    }

    @Test
    public void testMapReduce() throws IOException, InterruptedException, ClassNotFoundException {
        // Create test data
        cluster.writeFile("in/test1.msg", message);
        cluster.writeFile("in/test2.msg", message);

        // Set up Job configuration
        Configuration conf = cluster.getConfiguration();
        conf.set("c24.inputformat.element", "biz.c24.io.fix42.ExecutionReportElement");
        conf.set("c24.inputformat.startpattern", "^8=FIX.*$");

        // Create & setup Job
        Job job = new Job(conf, "Mapping Test");
        job.setInputFormatClass(ComplexDataObjectFileInputFormat.class);
        job.setJarByClass(ParseTest.class);
        job.setMapperClass(Mapper.class);
        job.setCombinerClass(Reducer.class);
        job.setReducerClass(Reducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        ComplexDataObjectFileInputFormat.addInputPath(job, new Path(cluster.getRootDir() + "/in"));
        FileOutputFormat.setOutputPath(job, new Path(cluster.getRootDir() + "/out"));

        job.submit();

        assertTrue(job.waitForCompletion(false));

        String out = cluster.readFile("out/part-r-00000").trim();
        assertThat(out, is("ExecutionReport\t2"));

    }
}

