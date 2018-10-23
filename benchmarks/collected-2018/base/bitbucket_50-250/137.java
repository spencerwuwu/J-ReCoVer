// https://searchcode.com/api/result/127939900/

package info.infostor.sample;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class ImageProcess extends Configured implements Tool {

	public static class ImageProcessMap extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, IntWritable> {

		static Configuration config = new Configuration();

		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text("image");

		FileSystem hdfs = null;

		public ImageProcessMap() {
			try {
				hdfs = FileSystem.get(config);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			String line = value.toString();

			Path srcFile = new Path("/imageout/" + line + ".jpg");

			InputStream inputStream = hdfs.open(srcFile);
			BufferedImage img = ImageIO.read(inputStream);
			if (img == null) {
				System.out.println("PATH = " + srcFile.getName());
				return;
			}
			BufferedImage newImg = ImageUtils.resizeImageWithHint(img,
					img.getType());
			Path outFile = new Path("/imagescale1/" + line + ".jpg");

			OutputStream outputStream = hdfs.create(outFile);
			ImageIO.write(newImg, "jpg", outputStream);
			output.collect(word, one);
		}

	}

	public static class ImageProcessReduce extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			int sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}
			output.collect(key, new IntWritable(sum));
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), WordCount.class);
		conf.setNumTasksToExecutePerJvm(-1);
		
		conf.setNumMapTasks(10);
		
		conf.setJobName("imageGenerator");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		conf.setMapperClass(ImageProcessMap.class);
		conf.setCombinerClass(ImageProcessReduce.class);
		conf.setReducerClass(ImageProcessReduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		JobClient.runJob(conf);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new ImageProcess(), args);
		System.exit(res);
	}

}

