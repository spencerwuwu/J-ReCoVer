// https://searchcode.com/api/result/61172870/

package Rank;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: shlee322
 * Date: 11. 10. 9.
 * Time:  10:31
 * To change this template use File | Settings | File Templates.
 */
public class RankReducer extends Reducer<Text, Text, NullWritable, Text> {
    protected String inputDelimiter;
    protected String outputDelimiter;
    protected int top;
    protected int column;

    @Override
	protected void setup(Context context) throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        inputDelimiter = conf.get("inputDelimiter");
        outputDelimiter = conf.get("outputDelimiter");
        top = conf.getInt("top", 0);
        column = conf.getInt("column", 0);
    }

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        TreeMap<Integer, String> Map = getRankMap(values);

        int count = 0;
        for(java.util.Map.Entry<Integer, String> value : Map.entrySet())
        {
            ++count;

            context.write(
                    NullWritable.get(),
                    new Text(getResultString(count, value.getValue()))
            );

            if(top != 0 && count >= top)
                break;
        }
    }

    protected TreeMap<Integer, String> getRankMap(Iterable<Text> values)
    {
        TreeMap<Integer, String> Map = new TreeMap<Integer, String>();

        Iterator<Text> valueIterator = values.iterator();
        while(valueIterator.hasNext())
        {
            String data = valueIterator.next().toString();
            int InputDelimiterIndex = data.indexOf(inputDelimiter);
            Map.put(
                    Integer.parseInt(data.substring(0, InputDelimiterIndex)),
                    data.substring(InputDelimiterIndex + 1)
            );

        }

        return Map;
    }

    protected String getResultString(int rank, String value)
    {
        String[] src = safeArray(split(value));
        StringBuffer res = new StringBuffer();

        //rank  
        append(src, res, 0, column);

        //rank 
        res.append(rank);
        res.append(outputDelimiter);

        //rank   
        append(src, res, column, src.length);

        return res.substring(0, res.length() - outputDelimiter.length());
    }


    //    ,   
    protected String[] split(String s) {
        String[] r = s.split(inputDelimiter);
        if (s.substring(s.length() - inputDelimiter.length()).equals(inputDelimiter)) {
            String[] temp = new String[r.length + 1];
            System.arraycopy(r, 0, temp, 0, r.length);
            temp[r.length] = "";
            return temp;
        }

        return r;
    }

    protected void append(String[] src, StringBuffer res, int start, int end)
    {
        for(int i=start; i<end; i++)
        {
            res.append(src[i]);
            res.append(outputDelimiter);
        }
    }

    //        .
    protected String[] safeArray(String[] src)
    {
        if(src.length >= column + 1)
            return src;

        String[] temp = new String[column];
        System.arraycopy(src, 0, temp, 0, src.length);
        for(int i=src.length; i<temp.length; i++)
            temp[i] = "";
        return temp;
    }
}

