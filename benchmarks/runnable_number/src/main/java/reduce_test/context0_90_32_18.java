//https://searchcode.com/file/2222777/examples/src/main/java/org/apache/mahout/cf/taste/example/email/MailToRecReducer.java#l-27

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context0_90_32_18 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context0_90_32_18();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	  private boolean useCounts = true;
	  
	  public void reduce(Text key, Iterable<LongWritable> values, Context context)
			    throws IOException, InterruptedException {
			    if (useCounts) {
			      long sum = 0;
			      for (LongWritable value : values) {
			        sum++;
			      }
			      context.write(new Text(key.toString() + ',' + sum), null);
			    } else {
			      context.write(new Text(key.toString()), null);
			    }
			  }

}
