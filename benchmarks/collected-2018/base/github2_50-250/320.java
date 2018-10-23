// https://searchcode.com/api/result/75864613/

package com.sohu.adrd.data.summary;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputCommitter;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import org.apache.hadoop.mapred.lib.MultipleOutputs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.Tool;


/**
 * Hadoop Task Base Processor author : wang chao
 */
public abstract class MRProcessorOld implements Tool {

	protected JobConf _conf = new JobConf(this.getClass().getSimpleName());
	protected Options _options = new Options();
	
	protected String _input = null;
	
	protected String _output = null;
	protected String _jarpath = null;
	protected String _numReduce = null;
	
	protected static final Log LOG = LogFactory.getLog(MRProcessorOld.class.getName());

	public MRProcessorOld() {
		setupOptions();
	}

	public int run(String[] args) throws Exception {
		parseArgv(args);
		setJars();
		
		Path input = new Path(_input);
		
		
		_conf.setJarByClass(this.getClass());
		_conf.setMapOutputKeyClass(Text.class);
		_conf.setMapOutputValueClass(Text.class);
		_conf.setOutputKeyClass(Text.class);
		_conf.setOutputValueClass(Text.class);
		_conf.setInputFormat(SequenceFileInputFormat.class);
		//_conf.setOutputFormat(TextOutputFormat.class);
		//_conf.setOutputFormat(SequenceFileOutputFormat.class);
		
		if (_numReduce != null) {
			int numReduce = Integer.parseInt(_numReduce);
			_conf.setNumReduceTasks(numReduce);
		} else {
			_conf.setNumReduceTasks(0);
		}

		FileInputFormat.addInputPath(_conf, input);
//		FileInputFormat.setInputPathFilter(_conf, InputPathFilter.class);
		
		Path outputPath = new Path(_output);
		FileSystem fs = FileSystem.get(outputPath.toUri(), _conf);
		if (fs.exists(outputPath))
			fs.delete(outputPath, true);
		FileOutputFormat.setOutputPath(_conf, outputPath);
		
		MultipleOutputs.addNamedOutput(_conf, "PvDel", TextOutputFormat.class, Text.class, Text.class);
		MultipleOutputs.addNamedOutput(_conf, "AdDel", TextOutputFormat.class, Text.class, Text.class);
//		MultipleOutputs.addNamedOutput(_conf, "check_info", TextOutputFormat.class, Text.class, Text.class);
		
		configJob(_conf);
		JobClient.runJob(_conf);
		return 0;
	}

	public JobConf getConf() {
		return _conf;
	}

	public void setConf(JobConf conf) {
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
	
	protected abstract void configJob(JobConf _conf);

	@SuppressWarnings("static-access")
	private Option createBoolOption(String name, String desc) {
		return OptionBuilder.withDescription(desc).create(name);
	}
	
	private Option createOption(String name, String desc, String argName,
			int max, boolean required) {
		return OptionBuilder.withArgName(argName).hasArgs(max).withDescription(desc).isRequired(required).create(name);
	}

	protected void setupOptions() {
		Option input = createOption("input","Input Path", "path", 1, true);
		Option jarpath = createOption("jarpath", "jar directory.", "path", 1, false);
		Option numReduce = createOption("numReduce", "Optional.", "spec", 1, false);
		Option output = createOption("output", "DFS output directory for the Reduce step", "path", 1, true);
		Option help = createBoolOption("help", "print this help message");
		
		_options.addOption(input).addOption(output).addOption(numReduce).addOption(help).addOption(jarpath);
	}

	protected void parseArgv(String[] args) throws ParseException {
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
			_jarpath = cmdLine.getOptionValue("jarpath");
			_numReduce = cmdLine.getOptionValue("numReduce");
			_output = cmdLine.getOptionValue("output");
		} else {
			exitUsage(true);
		}
		if (_output == null) {
			fail("Required argument: -output <path>");
		}
		if (_jarpath == null) {
			_jarpath="/root/lib/";
		}
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
	
	
	protected static void exitUsage(boolean generic){
		if (generic) {
			GenericOptionsParser.printGenericCommandUsage(System.out);
		}
		fail("");
	}

	private static void fail(String message) {
		System.err.println(message);
		System.exit(-1);
	}
}

