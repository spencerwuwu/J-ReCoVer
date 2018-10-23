// https://searchcode.com/api/result/74993317/

package edu.uiowa.icts.hadoop;


/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.hadoop.ColumnFamilyInputFormat;
import org.apache.cassandra.hadoop.ColumnFamilyOutputFormat;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.CounterColumn;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import edu.uiowa.icts.medline.Citation;
import edu.uiowa.icts.util.Combinations;
import edu.uiowa.medline.xml.MedlineCitation;

/**
 * This counts the occurrences of words in ColumnFamily Standard1, that has a single column (that we care about)
 * "text" containing a sequence of words.
 *
 * For each word, we output the total number of occurrences across all texts.
 *
 * When outputting to Cassandra, we write the word counts as a {word, count} column/value pair,
 * with a row key equal to the name of the source column we read the words from.
 */
public class MedlineMeshCount extends Configured implements Tool
{
	
	
    private static final Logger logger = LoggerFactory.getLogger(MedlineMeshCount.class);

    static final String KEYSPACE = "MEDLINE";
    static final String COLUMN_FAMILY = "MedlineCitation";

    static final String OUTPUT_REDUCER_VAR = "output_reducer";
    static final String OUTPUT_DEFAULT = "cassandra"; // or "filesystem"
    static final String OUTPUT_COLUMN_FAMILY = "output_words";
    private static final String OUTPUT_PATH_PREFIX = "/tmp/word_count";

    private static final String CONF_COLUMN_NAME = "columnname";
    
    private static JAXBContext jContext;
    private static Unmarshaller unmarshaller;
    
    public static void main(String[] args) throws Exception
    {
        // Let ToolRunner handle generic command-line options
        ToolRunner.run(new Configuration(), new MedlineMeshCount(), args);
        System.exit(0);
    }

    public static class TokenizerMapper extends Mapper<ByteBuffer, SortedMap<ByteBuffer, IColumn>, Text, IntWritable>
    {
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        private ByteBuffer sourceColumn;

        protected void setup(org.apache.hadoop.mapreduce.Mapper.Context context)
        		throws IOException, InterruptedException
        {
            sourceColumn = ByteBufferUtil.bytes(context.getConfiguration().get(CONF_COLUMN_NAME));
            try {
				jContext=JAXBContext.newInstance("edu.uiowa.medline.xml");
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				logger.error("Error Message", e);
				//e.printStackTrace();
			} 
			
			try {
				unmarshaller = jContext.createUnmarshaller() ;
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				logger.error("Error Message", e);
				//e.printStackTrace();
			} 
			
        }

        public void map(ByteBuffer key, SortedMap<ByteBuffer, IColumn> columns, Context context) throws IOException, InterruptedException
        {
        	Citation citation = null;
        	IColumn column = columns.get(sourceColumn);
            if (column == null)
                return;
            String value = ByteBufferUtil.string(column.value());
            
            InputStream is = new ByteArrayInputStream(value.getBytes());
			
			Reader reader = new InputStreamReader(is,"UTF-8");
			InputSource is1 = new InputSource(reader);
			is1.setEncoding("UTF-8");
			
            
            
			try {
				
				
				citation = new Citation((MedlineCitation)unmarshaller.unmarshal(is1));
				//logger.debug("Citation: " + citation.getPmid() + " [" + citation.getArticleTitle() + "]");
//				String text = citation.getArticleTitle().toLowerCase();
//				text = text.replaceAll("[^A-Za-z0-9 ]", "");
//	            StringTokenizer itr = new StringTokenizer( text);
//	            
				List<String> terms = citation.getMinorMeshHeading();
				Combinations c  = new Combinations();
				List<String> combinations = c.getCombinations(terms,2,3);
				for (String term : combinations) {
					word.set( term);
					context.write(word, one);
				}
//				
//	            while (itr.hasMoreTokens())
//	            {
//	                word.set(itr.nextToken());
//	                context.write(word, one);
//	            }				
			} catch (java.lang.NullPointerException npe) {
				//logger.debug("PMID: " + citation.getPmid());
				logger.debug("no meshterms");
				
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				logger.debug("parse error");
				//e.printStackTrace();
			}
            
            
                    }
    }

    public static class ReducerToFilesystem extends Reducer<Text, IntWritable, Text, IntWritable>
    {
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            int sum = 0;
            for (IntWritable val : values)
                sum += val.get();
            context.write(key, new IntWritable(sum));
        }
    }

    public static class ReducerToCassandra extends Reducer<Text, IntWritable, ByteBuffer, List<Mutation>>
    {
        private ByteBuffer outputKey;

        protected void setup(org.apache.hadoop.mapreduce.Reducer.Context context)
        throws IOException, InterruptedException
        {
            outputKey = ByteBufferUtil.bytes("word_count_22");
        }

        public void reduce(Text word, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            int sum = 0;
            for (IntWritable val : values)
                sum += val.get();
            if (sum >= 1) {
            	//context.write(outputKey, Collections.singletonList(getMutation(word, sum)));
            	context.write(outputKey, Collections.singletonList(getMutation(word, sum)));
            }
        }

        private static Mutation getMutation(Text word, int sum)
        {
            Column c = new Column();
            
            c.setName(Arrays.copyOf(word.getBytes(), word.getLength()));
            c.setValue(ByteBufferUtil.bytes(String.valueOf(sum)));
            c.setTimestamp(System.currentTimeMillis());

            Mutation m = new Mutation();
            m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m.column_or_supercolumn.setColumn(c);
            return m;
        }
        
        
        private static Mutation getCounterMutation(Text word, int sum) {
        	CounterColumn counter = new CounterColumn();
        	counter.setName(Arrays.copyOf(word.getBytes(), word.getLength()));
        	counter.setValue(Long.valueOf(sum));
        	Mutation m = new Mutation();
            m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m.column_or_supercolumn.setCounter_column(counter);
            return m;
        }
    }

    public int run(String[] args) throws Exception
    {
    	
		try {
				
			String columnName = "RAW_XML";
			getConf().set(CONF_COLUMN_NAME, columnName );
			
			logger.debug("Starting wordCount");
			Job job = new Job(getConf(), "wordcount");
            job.setJarByClass(MedlineMeshCount.class);
            job.setMapperClass(TokenizerMapper.class);
            
            
//            job.setCombinerClass(ReducerToFilesystem.class);
//            job.setReducerClass(ReducerToFilesystem.class);
//            job.setOutputKeyClass(Text.class);
//            job.setOutputValueClass(IntWritable.class);
//            FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH_PREFIX));
            
//
      //      job.setCombinerClass(ReducerToCassandra.class);
            job.setNumReduceTasks(12);
	    	job.setReducerClass(ReducerToCassandra.class);
	    	job.setMapOutputKeyClass(Text.class);
	    	job.setMapOutputValueClass(IntWritable.class);
	    	job.setOutputKeyClass(ByteBuffer.class);
	    	job.setOutputValueClass(List.class);

	    	job.setOutputFormatClass(ColumnFamilyOutputFormat.class);

            ConfigHelper.setOutputColumnFamily(job.getConfiguration(), KEYSPACE, OUTPUT_COLUMN_FAMILY);
            job.setInputFormatClass(ColumnFamilyInputFormat.class);
            ConfigHelper.setRpcPort(job.getConfiguration(), "9160");
            //org.apache.cassandra.dht.LocalPartitioner
	        ConfigHelper.setInitialAddress(job.getConfiguration(), "localhost");
	        ConfigHelper.setPartitioner(job.getConfiguration(), "org.apache.cassandra.dht.RandomPartitioner");
	        ConfigHelper.setInputColumnFamily(job.getConfiguration(), KEYSPACE, COLUMN_FAMILY);
	        
	        
	        SlicePredicate predicate = new SlicePredicate().setColumn_names(Arrays.asList(ByteBufferUtil.bytes(columnName)));
//	        SliceRange slice_range = new SliceRange();
//	        slice_range.setStart(ByteBufferUtil.bytes(startPoint));
//	        slice_range.setFinish(ByteBufferUtil.bytes(endPoint));
//	        
//	        predicate.setSlice_range(slice_range);
	        ConfigHelper.setInputSlicePredicate(job.getConfiguration(), predicate);

          	job.waitForCompletion(true);
          	logger.debug("Done wordCount");
		
		} catch (Exception e) {
			logger.error("Error", e);
		}
        return 0;
    }
    
    
    
    
    
}
