//https://searchcode.com/file/2505952/examples/world_development_indicators/src/main/java/com/mongodb/hadoop/examples/world_development/WorldDevIndicatorReducer.java#l-34

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;

public class context0_90_12_18 implements ReducerC<Text, DoubleWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, DoubleWritable> reducer=new context0_90_12_18();
			Tester<Text, DoubleWritable, Text, DoubleWritable> tester=new Tester<Text, DoubleWritable, Text, DoubleWritable>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce( final Text pCountryCode,
			final Iterable<DoubleWritable> pValues,
			final Context pContext )
					throws IOException, InterruptedException{
		double count = 0;
		double sum = 0;
		for ( final DoubleWritable value : pValues ){
			sum += value.get();
			count++;
		}

		final double avg = sum / count;

		pContext.write( pCountryCode, new DoubleWritable( avg ) );
	}

}
