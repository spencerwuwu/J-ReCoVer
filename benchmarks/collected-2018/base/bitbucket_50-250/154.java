// https://searchcode.com/api/result/60445148/

package com.chine.pagerank.mapreduce.calpagerank;

import java.io.IOException;

import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;

public class PageRankCalReducer extends
		Reducer<Text, Text, Text, Text> {
	
	private static final float damping = 0.85F;
	
	public static final String FILE_COUNT_KEY = "fileinput.count";
	
	@Override
	public void reduce(Text key, Iterable<Text> values, Context context)
		throws IOException, InterruptedException {
		
		boolean isExistingWikiPage = false;
        String[] split;
        float sumShareOtherPageRanks = 0;
        String links = "";
        String pageWithRank;
        
        // For each otherPage: 
        // - check control characters
        // - calculate pageRank share <rank> / count(<links>)
        // - add the share to sumShareOtherPageRanks
        for(Text value: values){
            pageWithRank = value.toString();
            
            if(pageWithRank.equals("!")) {
                isExistingWikiPage = true;
                continue;
            }
            
            if(pageWithRank.startsWith("|")){
                links = "\t"+pageWithRank.substring(1);
                continue;
            }

            split = pageWithRank.split("\\t");
            
            float pageRank = Float.valueOf(split[1]);
            int countOutLinks = Integer.valueOf(split[2]);
            
            sumShareOtherPageRanks += (pageRank/countOutLinks);
        }

        if(!isExistingWikiPage) return;
        
        int pageCount = Integer.valueOf(context.getConfiguration().get(FILE_COUNT_KEY));
        float newRank = damping * sumShareOtherPageRanks + (1-damping) / pageCount;

        context.write(key, new Text(newRank + links));
	}
}

