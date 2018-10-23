// https://searchcode.com/api/result/114443261/

package org.pbit.bigdata.replication;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.fs.*;

import java.io.IOException;
import java.util.*;
import java.io.DataInput;
import java.io.DataOutput;

public class MapReduce extends Configured implements Tool {
    
    public static int BLOCK_SIZE = 128;

    protected In i;
    protected Out o;
    
    public MapReduce(In i, Out o) {
	this.i = i;
	this.o = o;
    }

    public interface In {
	public Collection<String> getKeys();
	public String getValue(String key);
	public void remove(String key);
    }

    public interface Out {
	public void store(String key, String value);
    }

    public static class KVRecordReader extends RecordReader<Text, Text> {
	
	String currentKey;
	KVInputSplit split;

	public void close() throws IOException {
	}

	public Text getCurrentKey() {
	    return new Text(split.current());
	}

	public Text getCurrentValue() {	    
	    try {
		return new Text(split.value());
	    } catch (Exception e) {
		return null;
	    }
	}

	public float getProgress() {
	    return (float).5;
	}

	public void initialize(InputSplit split, TaskAttemptContext context) {
	    this.split = (KVInputSplit)split;
	}

	public boolean nextKeyValue() {
	    try {
		if (!split.hasNext()) return false;
		split.next();

		return true;
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    
	    return false;
	}
    }

    public static class KVRecordWriter extends RecordWriter<Text, Text> {
	
	java.util.Map<String, String> map;

	public void close(TaskAttemptContext context) {
	    store();
	    map = null;
	}

	public void write(Text key, Text value) {
	    if (map == null) map = new HashMap<String, String>();
	    else if (map.size() == BLOCK_SIZE) {
		store();
		map.clear();
	    }

	    map.put(key.toString(), value.toString());
	    KVInputFormat.getIn().remove(key.toString());
	}

	protected void store() {
	    if (map.size() > 0) {
		String record = genRecord();
		String key = genKey();

		System.out.println("---- Storing to cloud: " + key + " -> " + record);

		KVOutputFormat.getOut().store(key, record);
	    }
	}

	protected String genRecord() {
	    StringBuffer buf = new StringBuffer();
	    for (String val : map.values()) buf.append("\n" + val);

	    return buf.toString();
	}

	protected String genKey() {
	    return UUID.randomUUID().toString();
	}
    }

    public static class KVInputSplit extends InputSplit implements Writable {

	Iterator it;
	In i;
	String current;
	int skips;
	int pos;
	int length;

	public KVInputSplit() {
	}
    
	public KVInputSplit(In i , int pos) {
	    prepare(i, pos);
	}

	protected void prepare(In i, int pos) {
	    try {
		this.i = i;
		it = i.getKeys().iterator();
		for (int j = 0; it.hasNext() && (j < pos); j++) next();
		skips = 0;
		this.pos = pos;
		length = i.getKeys().size() - pos;
		if (length > BLOCK_SIZE) length = BLOCK_SIZE;
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}

	public void readFields(DataInput in) throws IOException {
	    int s = in.readInt();
	    int p = in.readInt();

	    prepare(KVInputFormat.getIn(), p);
	    
	    try {
		for (int i = 0; i < skips - 1; i++) next();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	
	public void write(DataOutput out) throws IOException {
	    out.writeInt(skips);
	    out.writeInt(pos);
	}

	public long getLength() {
	    return (long)length;
	}

	public String[] getLocations() {
	    return new String[]{"localhost"};
	}

	public String current() {
	    return current;
	}

	public String value() throws Exception {
	    return i.getValue(current);
	}

	public String next() throws Exception {
	    current = (String)it.next();
	    skips++;

	    return current;
	}

	public boolean hasNext() {
	    return (skips < length) && it.hasNext();
	}
    }

    public static class KVInputFormat extends InputFormat<Text, Text> {

	protected static In i;

	public static In getIn() {
	    return i;
	}

	public static void setIn(In ii) {
	    i = ii;
	}

	public RecordReader createRecordReader(InputSplit split,
					TaskAttemptContext context) {
	    return new KVRecordReader();
	}

	public List<InputSplit> getSplits(JobContext context) {
	    List<InputSplit> a = new ArrayList<InputSplit>();
	    KVInputSplit is = null;
	    int splits = (int)(i.getKeys().size() / BLOCK_SIZE);
	    if (i.getKeys().size() % BLOCK_SIZE > 0) splits++;
	    for (int j = 0; j < splits; j++) {
		is = new KVInputSplit(i, j * BLOCK_SIZE);
		a.add(is);
	    }
	    
	    return a;
	}
    }

    public static class KVOutputFormat extends OutputFormat<Text, Text> {
	
	protected static Out o;

	public static Out getOut() {
	    return o;
	}

	public static void setOut(Out oo) {
	    o = oo;
	}

	public void checkOutputSpecs(JobContext context) {
	}

	public OutputCommitter getOutputCommitter(TaskAttemptContext context)
	    throws IOException {
	    return new FileOutputCommitter(new Path("/home/pb/tmp/hadoop"),
					   context);
	}

	public RecordWriter getRecordWriter(TaskAttemptContext context) {
	    return new KVRecordWriter();
	}
    }

    public static class Map extends Mapper<Text, Text, Text, Text> {
	public void map(Text key, Text value, Context context)
	    throws IOException, InterruptedException {
	    context.write(key, value);
	}
    }

    public static class Reduce extends Reducer<Text, Text, Text, Text> {
	public void reduce(Text key, Iterable<String> values,
			   Context context) throws IOException, InterruptedException {
	}
    }

    protected int calcReducersNumber(In i) {
	int ret = (int)(i.getKeys().size() / BLOCK_SIZE);
	if (i.getKeys().size() % BLOCK_SIZE > 0) ret++;

	return ret;
    }

    public int run(String[] a) throws Exception {
	Job job = new Job();
    
	job.setJarByClass(MapReduce.class);
	job.setJobName("org.pbit.bigdata.mapreduce");

	job.setOutputKeyClass(Text.class);
	job.setOutputValueClass(Text.class);

	job.setMapperClass(Map.class);
	job.setCombinerClass(Reduce.class);
	job.setReducerClass(Reduce.class);

	KVInputFormat.setIn(i);
	KVOutputFormat.setOut(o);

	job.setInputFormatClass(KVInputFormat.class);
	job.setOutputFormatClass(KVOutputFormat.class);

	job.setNumReduceTasks(calcReducersNumber(i));
	
	return job.waitForCompletion(true) ? 1 : 0;
    }

    public static void main(String[] a) throws Exception {
	System.exit(ToolRunner.run(new MapReduce(new DB(),
						 new Cloud(a[0])),
				   a));
    }
}

