// https://searchcode.com/api/result/71330705/

package Mapred;

import java.io.IOException;

import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Reducer;

public class MyParserReducer extends
		Reducer<Text, CompositeValueFormat, Text, Text> {

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		context.write(new Text(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?> <All>"), null);
	}

	@Override
	protected void cleanup(Context context) throws IOException,
			InterruptedException {
		context.write(new Text("</All>"), null);
	}

	private Text outputKey = new Text();

	public void reduce(Text key, Iterable<CompositeValueFormat> values,
			Context context) throws IOException, InterruptedException {
		for (CompositeValueFormat value : values) {
			if (!value.getTweet().toString().equals("")) {
				outputKey.set(constructPropertyXml(key, value));
				context.write(outputKey, null);
			}
		}
	}

	public static String constructPropertyXml(Text name,
			CompositeValueFormat value) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Tweet><name>").append(name)
				.append("</name><Text Concerning=\"")
				.append(value.getConcerning()).append("\">")
				.append(value.getTweet()).append("</Text><Sentiment>")
				.append(value.getSentiment()).append("</Sentiment><Certainty>")
				.append(value.getCertainty()).append("</Certainty></Tweet>");
		return sb.toString();
	}

}

