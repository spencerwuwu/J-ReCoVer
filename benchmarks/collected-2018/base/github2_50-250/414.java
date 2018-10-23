// https://searchcode.com/api/result/71330739/

package Mapred;

import java.io.IOException;

import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Reducer;

public class SingleReducer extends
		Reducer<Text, CompositeValueFormatCombine, Text, Text> {

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

	public void reduce(Text key, Iterable<CompositeValueFormatCombine> values,
			Context context) throws IOException, InterruptedException {
		for (CompositeValueFormatCombine value : values) {
			if (!value.getTweet().toString().equals("")) {
				outputKey.set(constructPropertyXml(key, value));
				context.write(outputKey, null);
			}
		}
	}

	public static String constructPropertyXml(Text name,
			CompositeValueFormatCombine value) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Tweet><name>").append(name).append("</name><Concerning>");
		String Parties = value.getParties().toString();
		String People = value.getPeople().toString();
		String HashTags = value.getHashTags().toString();
		if (Parties.length()!= 0) {

			sb.append("<Parties>");
			String []parties = Parties.split("\n");
			for (String s : parties) {
				sb.append("<Party>").append(s).append("</Party>");
			}

			sb.append("</Parties>");
		}
		if (People.length() != 0) {

			sb.append("<Candidates>");
			
			String []people = People.split("\n");
			for (String s : people) {
				sb.append("<Candidate>").append(s).append("</Candidate>");
			}

			sb.append("</Candidates>");
		}
		if (HashTags.length() != 0) {

			sb.append("<HashTags>");
			
			String []hashTag= HashTags.split("\n");
			for (String s : hashTag) {
				sb.append("<HashTag>").append(s).append("</HashTag>");
			}

			sb.append("</HashTags>");
		}
		sb.append("></Concerning><Text ").append(value.getTweet())
				.append("</Text><Sentiment>").append(value.getSentiment())
				.append("</Sentiment><Certainty>").append(value.getCertainty())
				.append("</Certainty></Tweet>");
		return sb.toString();
	}

}

