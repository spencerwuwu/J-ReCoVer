// https://searchcode.com/api/result/74993207/

package edu.uiowa.icts.jobs.MeshTermList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uiowa.icts.util.Defaults;
import edu.uiowa.icts.util.Mask;

public class MyReducer extends Reducer<Text, Text, ByteBuffer, List<Mutation>>
{
	
    private static final Logger logger = LoggerFactory.getLogger(MyReducer.class);

 
    private static Mask m  = new Mask();
    
    private HashMap<Text, String> maskedValues = new HashMap<Text, String>();
    
    protected void setup(Context context) 
    {
    	
    	
    	logger.info("Starting Reducer");
  
    }
    
    
    /*
     * 
     * MAP  K: PMID, V: RAW_XML
     * Emit:
     * K: word, V: [PMID]
     * REDUCE K: word, V: [PMID]  // We can only have one Reduce task.
     * Emit:
     * K: mask(word) V: [cassandra mutation] 
     *  
     */

    public void reduce(Text word, Iterable<Text> values, Context context) 
    {
    	if (!"No Mesh Terms".equals(word.toString())) {
    		
	    	
	    	String maskedValue = getMaskedVal(word);
	    	ByteBuffer termKey = ByteBufferUtil.bytes(maskedValue.toString());
	    	List<Mutation> mutList = new ArrayList<Mutation>();
	    	int counter=0;
	        for (Text pmid : values) {
	        	mutList.add(getPmidMutation(pmid));
	        	counter++;
	        }
	        mutList.add(getMutation(new Text("term"), word));
	        mutList.add(getMutation(new Text("exists"), 1L));
	        mutList.add(getCountMutation(counter));
				try {
					context.write(termKey, mutList);		
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.error("Error Message", e);
					//e.printStackTrace();
				} 
	        
	    	logger.info("Term: " + word + "[" + counter +"]");
    	}
  
        //logger.info("done: " + word);
    }

    
    private String getMaskedVal(Text in) {
//    	if (maskedValues.containsKey(in))
//    		return maskedValues.get(in);
    	String next = UUID.randomUUID().toString();
    	//maskedValues.put(in,next);
    	return next;
    }
    

//    private static Mutation getTermMutation(Text value)
//    {
//        
//		Column c = new Column();
//  
//		c.setName("term".getBytes());
//		
//		c.setValue(ByteBufferUtil.bytes(value.toString()));
//		c.setTimestamp(System.currentTimeMillis());
//
//		Mutation m = new Mutation();
//		m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
//		m.column_or_supercolumn.setColumn(c);
//		return m;
//		
//    }


    private static Mutation getMutation(Text colName, Text value)
    {
        
		Column c = new Column();
  
		c.setName(colName.getBytes());
		
		c.setValue(ByteBufferUtil.bytes(value.toString()));
		c.setTimestamp(System.currentTimeMillis());

		Mutation m = new Mutation();
		m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
		m.column_or_supercolumn.setColumn(c);
		return m;
		
    }

    private static Mutation getMutation(Text colName, Long value)
    {
        
		Column c = new Column();
  
		c.setName(colName.getBytes());
		
		c.setValue(ByteBufferUtil.bytes(value));
		c.setTimestamp(System.currentTimeMillis());

		Mutation m = new Mutation();
		m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
		m.column_or_supercolumn.setColumn(c);
		return m;
		
    }

    private static Mutation getCountMutation(int value)
    {
        
		Column c = new Column();
  
		c.setName(Defaults.PUB_COUNT_COL_NAME.getBytes());
		
		c.setValue(ByteBufferUtil.bytes(Long.valueOf(value)));
		c.setTimestamp(System.currentTimeMillis());

		Mutation m = new Mutation();
		m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
		m.column_or_supercolumn.setColumn(c);
		return m;
		
    }
    
    
    private static Mutation getPmidMutation(Text pmid)
    {
        
		Column c = new Column();
  
		c.setName(pmid.getBytes());
		
		c.setValue(ByteBufferUtil.bytes("1".toString()));
		c.setTimestamp(System.currentTimeMillis());

		Mutation m = new Mutation();
		m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
		m.column_or_supercolumn.setColumn(c);
		return m;
		
    }
    
    

}

