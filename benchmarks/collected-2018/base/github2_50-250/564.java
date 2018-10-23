// https://searchcode.com/api/result/74993127/

package edu.uiowa.icts.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.CounterColumn;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReducerToCassandra extends Reducer<Text, Text, ByteBuffer, List<Mutation>>
{
	
    private static final Logger logger = LoggerFactory.getLogger(ReducerToCassandra.class);

    private static Text outputKey= new Text();

    private static Text outputColumn = new Text();

    protected void setup(org.apache.hadoop.mapreduce.Reducer.Context context) 
    {
    	logger.info("Starting Reducer");
        outputColumn.set("RAW_XML");
    }

    public void reduce(Text word, Iterable<Text> values, Context context) 
    {
    	//logger.info("PMID: " + word);

    	
        for (Text val : values) {
			try {
				context.write(ByteBufferUtil.bytes(word.toString()), Collections.singletonList(getMutation(val)));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.error("Error Message", e);
				//e.printStackTrace();
			} 
        }
        //logger.info("done: " + word);
    }

    private static Mutation getMutation(Text value)
    {
        
		Column c = new Column();
  
		c.setName(Arrays.copyOf(outputColumn.getBytes(), outputColumn.getLength()));
		
		c.setValue(ByteBufferUtil.bytes(value.toString()));
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

