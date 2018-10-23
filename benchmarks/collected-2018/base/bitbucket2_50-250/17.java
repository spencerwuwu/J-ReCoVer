// https://searchcode.com/api/result/61172879/

package AssociationRule;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: shlee322
 * Date: 11. 10. 16.
 * Time: ei 6:40
 * To change this template use File | Settings | File Templates.
 */
public class SupportReducer extends Reducer<Text, NullWritable, NullWritable, NullWritable> {
    private Mongo mongo;
    private DB db;
    private DBCollection coll;

    @Override
	protected void setup(Context context) throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        mongo = new Mongo(conf.get("host"));
        db = mongo.getDB("association_rule");
        coll = db.getCollection("sanghyuck");
    }

    @Override
    protected void reduce(Text key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
        long count = 0;
        Iterator<NullWritable> iterator = values.iterator();
        while(iterator.hasNext())
        {
            iterator.next();
            ++count;
        }

        BasicDBObject object = new BasicDBObject();
        object.put("name", key.toString());
        object.put("count", count);

        coll.insert(object);

        //System.out.println(String.format("%s %d", key.toString(), count));
    }

    @Override
    protected void cleanup(Context context
                         ) throws IOException, InterruptedException {
        mongo.close();
    }


}
