// https://searchcode.com/api/result/75864626/

package com.sohu.adrd.data.summary;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.zebra.mapreduce.TableInputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;

import com.sohu.adrd.data.mapreduce.InputPathFilter;
import com.twitter.elephantbird.mapreduce.input.MapReduceInputFormatWrapper;
import com.twitter.elephantbird.mapreduce.output.RCFileOutputFormat;



/**
 * 
 * @author Su Hanchen hanchensu@sohu-inc.com
 *
 */
public abstract class MRProcessor implements Tool {

	protected Configuration _conf = new Configuration();
	protected Options _options = new Options();
	protected String _input = null;
	protected String _output = null;
	protected String _times = null;
	protected String _numReduce = null;
	protected String _jarpath = null;
	protected Boolean _input_zebra = false;
	protected Boolean _input_seq = false;
	protected Boolean _output_seq = false;
	
	private FileSystem client = null;
	protected static final Log LOG = LogFactory.getLog(MRProcessor.class.getName());

	public MRProcessor() {
		setupOptions();
	}

	@Override
	public int run(String[] args) throws Exception {
		parseArgv(args);	
		setJars();
	
		Job job = new Job(_conf, this.getClass().getSimpleName());
//		RCFileOutputFormat.setColumnNumber(_conf, 2);
		
		job.setJarByClass(this.getClass());

		// these four configuration can be overwritted in configJob()
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		FileInputFormat.setInputPathFilter(job, InputPathFilter.class);

		if(_input_zebra)
		{
			job.setInputFormatClass(TableInputFormat.class);
			List<Path> input = new ArrayList<Path>();
			
			for(String path: _input.split(",")) {
				for(String sub : pathExpand(path).split(",")) { 
					input.add(new Path(sub));
				}
			}
			
			Path[] inputpaths = readFolder(input, _conf);
			System.out.println("Add Input Path");
			for(Path path: inputpaths) {
				System.out.println(path);
			}
			TableInputFormat.setInputPaths(job, inputpaths);
		}
		else if(_input_seq)
		{
			job.setInputFormatClass(SequenceFileInputFormat.class);
			for(String sub:_input.split(",")) {
				String expand = pathExpand(sub);
				System.out.println("Add Input Path: "+expand);
				FileInputFormat.addInputPaths(job, expand);
			}
		}
		
		
		if (_output_seq) 
		{
			job.setOutputFormatClass(SequenceFileOutputFormat.class);
		} 
		else 
		{
			job.setOutputFormatClass(TextOutputFormat.class);
		}
		Path outputPath = new Path(_output);
		FileSystem fs = FileSystem.get(outputPath.toUri(), _conf);
		if (fs.exists(outputPath))
			fs.delete(outputPath, true);
		FileOutputFormat.setOutputPath(job, outputPath);

		if (_numReduce != null) {
			int numReduce = Integer.parseInt(_numReduce);
			job.setNumReduceTasks(numReduce);
		} else {
			job.setNumReduceTasks(0);
		}

		configJob(job);

		// List non-default properties to the terminal and exit.
		// Configuration.main(null);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	@Override
	public Configuration getConf() {
		// TODO Auto-generated method stub
		return _conf;
	}

	@Override
	public void setConf(Configuration conf) {
		// TODO Auto-generated method stub
		_conf = conf;

	}

	public void setJars() throws IOException {
		File libFile = new File(_jarpath);
		String libFiles[] = libFile.list();
		for (String name : libFiles) {
			String path = "";
			if (name.endsWith(".jar")) {
				path = "file://" + _jarpath + name;
				addTmpJar(new Path(path), _conf);
			}
		}
	}

	/**
	 * Implement this function to specify your task's unique configuration
	 * 
	 * @param job
	 */
	protected abstract void configJob(Job job);

	@SuppressWarnings("static-access")
	private Option createOption(String name, String desc, String argName,
			int max, boolean required) {
		return OptionBuilder.withArgName(argName).hasArgs(max).withDescription(
				desc).isRequired(required).create(name);
	}

	@SuppressWarnings("static-access")
	private Option createBoolOption(String name, String desc) {
		return OptionBuilder.withDescription(desc).create(name);
	}

	protected void setupOptions() {
		Option input = createOption("input", "DFS input file(s) for the Map step", "path", Integer.MAX_VALUE, true);
		Option output = createOption("output", "DFS output directory for the Reduce step", "path", 1, true);
		Option times = createOption("times", "input the date", "path", Integer.MAX_VALUE, true);
		Option numReduce = createOption("numReduce", "Optional.", "spec", 1, false);
		Option jarpath = createOption("jarpath", "jar directory.", "path", 1, false);
		Option input_seq = createBoolOption("input_seq", "is input sequenced file");
		Option output_seq = createBoolOption("output_seq", "should output sequenced");
		Option input_zebra = createBoolOption("input_zebra", "is input is zebra format file");
		Option help = createBoolOption("help", "print this help message");
		_options.addOption(input).addOption(output).addOption(times).addOption(numReduce).addOption(jarpath).addOption(input_zebra).addOption(help).addOption(input_seq).addOption(output_seq);
	}
 
	protected void parseArgv(String[] args) {
		CommandLine cmdLine = null;
		try {
			cmdLine = new BasicParser().parse(_options, args);
		} catch (Exception oe) {
			exitUsage(true);
		}

		if (cmdLine != null) {
			if (cmdLine.hasOption("help")) {
				exitUsage(true);
			}
			_input = cmdLine.getOptionValue("input");
			_output = cmdLine.getOptionValue("output");
			_times = cmdLine.getOptionValue("times");
			_numReduce = cmdLine.getOptionValue("numReduce");
			_jarpath = cmdLine.getOptionValue("jarpath");
			_input_zebra = cmdLine.hasOption("input_zebra") ? true : false;
			_input_seq = cmdLine.hasOption("input_seq") ? true : false;
			_output_seq = cmdLine.hasOption("output_seq") ? true : false;
		} else {
			exitUsage(true);
		}

		if (_input == null) {
			fail("Required argument: -input <path>");
		}
		if (_output == null) {
			fail("Required argument: -output <path>");
		}
		if (_jarpath == null) {
			_jarpath="/root/lib/";
		}
	}
	
	

	protected static void exitUsage(boolean generic) {
		System.out.println("Usage: $HADOOP_HOME/bin/hadoop jar jarFile Launcher Processor [Options]");
		System.out.println("Options:");
		System.out.println("  -input    <path>     inputs, seperated by comma");
		System.out.println("  -output   <path>     output directory");
		System.out.println("  -times   <string>     times, yyyy/mm/dd, seperated by comma");
		System.out.println("  -numReduce <num>  optional");
		System.out.println("  -input_zebra inputs is zebra format, optional");
		System.out.println("  -input_seq inputs is sequenced, optional");
		System.out.println("  -output_seq outputs should be sequenced, optional");
		System.out.println("  -help");

		if (generic) {
			GenericOptionsParser.printGenericCommandUsage(System.out);
		}
		fail("");
	}

	protected static void addTmpJar(Path jarPath, Configuration conf) throws IOException {
		System.setProperty("path.separator", ":");
		FileSystem fs = FileSystem.get(jarPath.toUri(), conf);
		String newJarPath = jarPath.makeQualified(fs).toString();
		String tmpjars = conf.get("tmpjars");
		if (tmpjars == null || tmpjars.length() == 0) {
			conf.set("tmpjars", newJarPath);
		} else {
			conf.set("tmpjars", tmpjars + "," + newJarPath);
		}
	}

	protected static Path[] readFolder(List<Path> paths, Configuration conf) throws IOException {
		List<Path> pathList = new ArrayList<Path>();
		for(Path path : paths) {
			FileSystem fs = FileSystem.get(path.toUri(), conf);
			FileStatus[] fileStatusArray = fs.listStatus(path);
			for (FileStatus fileStatus : fileStatusArray) {
				if (!fs.isFile(fileStatus.getPath())) {
					if (fileStatus.getPath().getName().contains("SUB")) {
						pathList.add(new Path(fileStatus.getPath().toString()));
					}
				}
			}
		}
		Path[] res = new Path[pathList.size()];
		int i = 0;
		for (Path p : pathList) {
			res[i] = p;
			i++;
		}
		return res;
	}
	
	private String pathExpand(String path) throws IOException {
		if(path.endsWith("/")) path = path.substring(0, path.length()-1);
		
		if(path.contains("/user/aalog/shc") || path.contains("/user/aalog/new-sessionlog") || path.contains("/user/aalog/sessionlog")) {
			String paths = "";
			for(String time : _times.split(",")) {
				paths += path+"/" + time+","; 
			}
			return paths.substring(0, paths.length()-1);
		}
		
		if (client == null) client = FileSystem.get(_conf);
		StringBuilder sb = new StringBuilder();
		
		FileStatus[] subs = client.listStatus(new Path(path));
		if (subs == null) {
			System.out.println("subs null  ");
		}
		
		for (FileStatus sub : subs) {
			for(String time : _times.split(",")) {
				String subpart = path + "/" + sub.getPath().getName() + "/" + time;
				FileStatus subfstatus = null;
				try {
					subfstatus = client.getFileStatus(new Path(subpart));
				} catch (Exception e) {
				}
				if (subfstatus == null) continue;
				sb.append(subpart).append(",");
			}
		}
		
		String paths = sb.toString();
		return paths.substring(0,paths.length()-1);
	} 
	
	private static void fail(String message) {
		System.err.println(message);
		System.exit(-1);
	}
}

