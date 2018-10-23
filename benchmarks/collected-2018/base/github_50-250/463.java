// https://searchcode.com/api/result/73347337/

package edu.upenn.mkse212.pennbook.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/* writes ordered (best to worst) recommendations: node recommendation1,recommendation2...
 * Receives (node, rec1,value1,rec2,value2...) -- recommendation scores
 * and (node EXISTING;node2) -- existing nodes to ignore
 */
public class FinishReducer extends Reducer<Text,Text,Text,Text> 
{
	//store adsorption value and node for sorting
	private class Ranking implements Comparable {
		private String id;
		private double value;
		
		Ranking(String id, double value) {
			this.id=id;
			this.value = value;
		}
		
		public String getId() {
			return id;
		}

		@Override
		public int compareTo(Object arg0) {
			if (arg0 instanceof Ranking) {
				return (int)Math.round(1000000*(((Ranking) arg0).value - this.value ));
			}
			else
				return 0;
		}

		public double getValue() {
			// TODO Auto-generated method stub
			return value;
		}
	}
	
	//sort and concatenate recommended friends
	public String getSortedRecommendations(String ranksString,HashSet<String> existing)
	{
		StringBuffer sb = new StringBuffer();
		String[] els = ranksString.split(",");
		if (els.length<2)
			return "";
		ArrayList<Ranking> ranks = new ArrayList<Ranking>();
		for (int i=0; i<els.length-1; i+=2)
		{
			ranks.add(new Ranking(els[i],Double.parseDouble(els[i+1])));
		}
		
		Collections.sort(ranks);//sort according to adsorption score
		
		for (Ranking rank : ranks)
		{
			if (!existing.contains(stripPrefix(rank.getId()))) {//only include if not in existing friends
				System.out.println(rank.getValue());
				sb.append(rank.getId());
				sb.append(",");
			}
		}
		if (sb.length()>0)
			sb.deleteCharAt(sb.length()-1);//remove extra comma;
		System.out.println("-");
		
		return sb.toString();
			
	}
	
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException 
	{	
		ArrayList<Text> valueCopy = new ArrayList<Text>();
		
		HashSet<String> existing = new HashSet<String>();//recommendation ids to ignore (existing friends and self)
		if (!key.toString().split(":")[0].equals("user")) 
			return; //if the key isn't a user, ignore it
		existing.add(stripPrefix(key.toString()));//ignore self
		
		ArrayList<String> rankValues = new ArrayList<String>();
		
		for (Text val : values)
		{
			String[] valParts = val.toString().split(";");
			if (valParts[0].equals("EXISTING"))
					existing.add(stripPrefix(valParts[1]));
			else
				rankValues.add(val.toString());
		}
		
		String outValue = "";
		
		
		for (String rankValue : rankValues)
		{
			String[] valParts = rankValue.split(";");
			if (valParts.length==3)
				outValue = getSortedRecommendations(valParts[2], existing);
			else
				outValue ="";
			System.out.println(key.toString()+"/"+existing+"/"+valParts[2]);
		}
		if (rankValues.size()>1)
			outValue = "error";
		context.write(key, new Text(outValue));
	}
	
	public static String stripPrefix(String str)
	{
		String[] parts = str.split(":");
		if (parts.length>1) {
			return parts[1];
		}
		return parts[0];
	}
}

