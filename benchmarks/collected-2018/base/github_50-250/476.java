// https://searchcode.com/api/result/73347325/

package edu.upenn.mkse212.pennbook.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/*
 * takes the set of node,adsorp. values for each node from two directories and emits 
 * the maximum of all differences between corresponding pairs of adsorption values
 */
public class DiffReducer extends Reducer<Text,Text,IntWritable,DoubleWritable>
{
	public static final double D = 0.15;
	public void reduce(Text key, Iterable<Text> rankTexts, Context context) throws IOException, InterruptedException 
	{
		HashMap<String,Double> ranks0 = new HashMap<String,Double>();
		HashMap<String,Double> ranks1 = new HashMap<String,Double>();
		int j=0;
		for (Text rankText : rankTexts)
		{
			String[] ranksValues = rankText.toString().split(",");
			for (int i=0; i<ranksValues.length-1;i+=2)
			{
				if (j==0) ranks0.put(ranksValues[i], Double.parseDouble(ranksValues[i+1]));
				if (j==1) ranks1.put(ranksValues[i], Double.parseDouble(ranksValues[i+1]));
			}
			j++;
		}
		
		ArrayList<Double> diffs = new ArrayList<Double>();
		for (String node : ranks0.keySet())
		{
			if (ranks1.containsKey(node)) {
				diffs.add(Math.abs( ranks0.get(node)-ranks1.get(node) ));
				ranks1.remove(node);
			}
			else
				diffs.add(Math.abs(ranks0.get(node)));
		}
		
		for (String node:ranks1.keySet()) //for remaining nodes in ranks1
		{
			diffs.add(Math.abs(ranks1.get(node)));
		}
		
		Collections.sort(diffs);
		
		context.write(new IntWritable(1), new DoubleWritable(diffs.get(diffs.size()-1)));//emit largest diff for this node
		//key is 1 for all lines
	}
}

