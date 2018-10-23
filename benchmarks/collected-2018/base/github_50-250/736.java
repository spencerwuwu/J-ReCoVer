// https://searchcode.com/api/result/73438034/

/*
 * HBase OutputFormat for Hadoop Streaming.
 *
 * Format:
 * put	<RowID>	<ColumnName>	<value>	(<timestamp>)
 * delete	<RowID>	<ColumnName>	(<timestamp>)
 *
 * Options:
 * -jobconf reduce.output.table=<TableName>
 * -jobconf reduce.output.binary=<true|false> (default: false)
 * -jobconf stream.reduce.output.field.separator=<Separator> (default: tab)
 *
 * Notice:
 * Run streaming with dammy -output option.
 */

package org.childtv.hadoop.hbase.mapred;

import java.util.Date;
import java.util.regex.Pattern;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.regionserver.DeleteTracker.DeleteCompare;
import org.apache.hadoop.io.Writable;

import com.sun.corba.se.impl.ior.WireObjectKeyTemplate;

public class ListTableOutputFormat extends TextTableOutputFormat {

    // defined at org.apache.hadoop.streaming.PipeMapRed
    public static final String SEPARATOR_KEY = "stream.reduce.output.field.separator";
    public static final String DEFAULT_SEPARATOR = "\t";

    private String separator;

    public void configure(JobConf job) {
        super.configure(job);
        separator = job.get(SEPARATOR_KEY);
        if (separator == null)
            separator = DEFAULT_SEPARATOR;
    }

    public Writable[] createBatchUpdates(String command, String argsString) {
        String[] args = argsString.split(Pattern.quote(separator), -1);
        Writable bu; //= new BatchUpdate(args[0]);
        try {
            if (command.toLowerCase().equals("put")) {
            	Put put = new Put(decodeValue(args[0]) ); 
            	bu = put;
                put(put, args);
            } else if (command.toLowerCase().equals("delete")) {
            	Delete delete = new Delete(decodeValue(args[0]));
            	bu = delete;
                delete(delete, args);
            } else {
                throw new RuntimeException();
            }
        } catch(Exception e) {
            System.err.println(String.format("ListTableOutputFormat - invalid reduce output: %s / %s", command, argsString));
            e.printStackTrace();
            // throw new RuntimeException(String.format("ListTableOutputFormat - invalid reduce output: %s / %s", command, argsString));
            return new Writable[0];
            
        }
        return new Writable[] { bu };
    }

    private void put(Put bu, String[] args) {
        if (args.length > 3) {
            String[] names = args[1].split(":", 2);
            bu.add(decodeColumnName(names[0]), decodeColumnName(names[1]), getTimestampString(args[3]),  decodeValue(args[2]));
        }
        else {
            // Verify this is default behavior
            String[] names = args[1].split(":", 2);
            bu.add(decodeColumnName(names[0]), decodeColumnName(names[1]), new Date().getTime(),  decodeValue(args[2]));
        }
    }

    private void delete(Delete bu, String[] args) {
        if (args.length > 2) { 
            String[] names = args[1].split(":", 2);
            bu.deleteColumns(decodeColumnName(names[0]), decodeColumnName(names[1]), getTimestampString(args[2]));	
        }
        else {
            String[] names = args[1].split(":", 2);
            bu.deleteColumn(decodeColumnName(names[0]), decodeColumnName(names[1]));
        }
        
    }

    private long getTimestampString(String ts) {
        try {
            return Long.parseLong(ts);
        } catch(NumberFormatException e) {
        	return new Date().getTime(); // TODO CHECK THAT THIS IS DEFAULT BEHAVIOR
        }
        
    }
    

}

