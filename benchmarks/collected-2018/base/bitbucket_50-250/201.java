// https://searchcode.com/api/result/46838478/

package com.matrix;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.IntWritable;
//import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
//import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.Reducer;

/* Phase 1 reduction: For given node, divide edges as type A (incoming)
   or type B (outgoing).
   For each pair of edges, generate contracted edge
*/

public class P1Reducer extends Reducer<Text, IntWritable, Text, IntWritable> {

	public void reduce(WritableComparable key, Iterator values,
			OutputCollector output, Reporter reporter) throws IOException
    {
	Text outv = new Text(""); // Don't really need output values
	/* First split edges into A and B categories */
	LinkedList<GraphEdge> alist = new LinkedList<GraphEdge>();
	    LinkedList<GraphEdge> blist = new LinkedList<GraphEdge>();
	    while(values.hasNext()) {
		try {
		    GraphEdge e = new GraphEdge(values.next().toString());
		    if (e.tag.equals("A")) {
			alist.add(e);
		    } else {
			blist.add(e);
		    }
		} catch (BadGraphException e) {}
	    }
	    Iterator<GraphEdge> aset = alist.iterator();
	    // For each incoming edge
	    while(aset.hasNext()) {
		GraphEdge aedge = aset.next();
		// For each outgoing edge
		Iterator<GraphEdge> bset = blist.iterator();
		while (bset.hasNext()) {
		    GraphEdge bedge = bset.next();
		    GraphEdge newe = aedge.contractProd(bedge);
		    // Null would indicate invalid contraction
		    if (newe != null) {
			Text outk = new Text(newe.toString());
			output.collect(outk, outv);
		    }
		}
	    }
    }

	
}
