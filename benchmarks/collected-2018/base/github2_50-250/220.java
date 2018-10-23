// https://searchcode.com/api/result/110594992/

package com.github.emtrane;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MyReducer extends Reducer<Text, Text, Text, Text>
{

    static String regex = "^\\[[^\\]]*\\]";
    static Pattern patt = Pattern.compile(regex);

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
    {
        Double sum = Double.valueOf(0);

        String language ="";
        Date date;

        Long retweets = 0L;
        Long tweets = 0L;

        ObjectMapper mapper = new ObjectMapper();

        List<String> allTweets = new ArrayList<String>();
        for (Text value : values)
        {
            MapOutputValue output = mapper.readValue(value.toString(), MapOutputValue.class);

            tweets++;
            Double multiplier = output.getRetweet() ? 1 : 1.2;
            if(output.getRetweet()){
                retweets++;
            }
            Double score = multiplier * (1+Math.log10(Long.valueOf(output.getFollowers()) + 1));
            sum += score;
            language = output.getLanguage();

            allTweets.add(output.text);
        }

        // the key could also be NullWritable and the value could be any String/Text you want
        context.write(new Text(patt.matcher(key.toString()).replaceAll("")),
                new Text(sum.toString()+"\t"+tweets+"\t"+retweets+"\t"+language+"\t"+mapper.writeValueAsString(allTweets)));
    }
}


