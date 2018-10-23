// https://searchcode.com/api/result/61172885/

package AssociationRule;

import com.mongodb.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: shlee322
 * Date: 11. 10. 13.
 * Time:  2:45
 * To change this template use File | Settings | File Templates.
 */
public class AssociationRuleReducer extends Reducer<Text, NullWritable, NullWritable, Text> {
    protected String inputDelimiter;
    protected String outputDelimiter;
    protected long rowCount;
    private Mongo mongo;
    private DB db;
    private DBCollection coll;

    @Override
	protected void setup(Context context) throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        outputDelimiter = conf.get("outputDelimiter");
        rowCount = conf.getLong("RowCount",1);

        mongo = new Mongo(conf.get("host"));
        db = mongo.getDB("association_rule");
        coll = db.getCollection("sanghyuck");
    }

    @Override
    protected void reduce(Text key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
        String xName = key.toString().substring(0, key.toString().lastIndexOf(outputDelimiter));
        String yName = key.toString().substring(key.toString().lastIndexOf(outputDelimiter) + 1);
        double xyCount = 0;
        double xCount = 0;
        double yCount = 0;

        BasicDBObject find = new BasicDBObject();
        find.put("name", xName);
        DBObject object = coll.findOne(find);
        if(object != null)
            xCount = (Long)object.get("count");

        find = new BasicDBObject();
        find.put("name", yName);
        object = coll.findOne(find);
        if(object != null)
            yCount = (Long)object.get("count");

        find = new BasicDBObject();
        find.put("name", String.format("%s%s%s",xName,outputDelimiter,yName));
        object = coll.findOne(find);
        if(object != null)
            xyCount = (Long)object.get("count");

        if(xCount == 0 || yCount == 0 || xyCount == 0) //db   
           return;

        double support = xyCount / rowCount;
        double confidence = support / (xCount / rowCount);
        double lift = confidence / (yCount  / rowCount);

        context.write(NullWritable.get(),
                new Text(String.format("%s%s%s%s%f%s%f%s%f",
                        xName,
                        outputDelimiter,
                        yName,
                        outputDelimiter,
                        support,
                        outputDelimiter,
                        confidence,
                        outputDelimiter,
                        lift))
        );



        //float

        //System.out.println(key.toString());
    }

    protected void cleanup(Context context
                         ) throws IOException, InterruptedException {
        mongo.close();
    }
}
