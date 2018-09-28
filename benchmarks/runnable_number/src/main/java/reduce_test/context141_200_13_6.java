//https://searchcode.com/file/96335780/src/TriangleFriendship.java#l-13

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context141_200_13_6 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context141_200_13_6();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(0), new LongWritable(-1), new LongWritable(4) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	  public void reduce(Text key, Iterable<LongWritable> values,
				Context context) throws IOException, InterruptedException {
			Iterator<LongWritable> iter = values.iterator();
			int sum = 0;
			while(iter.hasNext()) {
				sum += iter.next().get();
			}
			if(sum == 3) { // Output if it has 3 edges
				context.write(key, null);
			}
		}

}
