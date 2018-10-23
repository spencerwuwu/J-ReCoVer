// https://searchcode.com/api/result/67745685/

package ua.org.enishlabs.demetra.genetic.distributed;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import ua.org.enishlabs.demetra.App;
import ua.org.enishlabs.demetra.GlobalConfig;
import ua.org.enishlabs.demetra.genetic.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author EniSh
 *         Date: 12.04.12
 */
public class DistributedPopulationChallenger extends Configured implements Tool, PopulationChallenger {

    private Path in = new Path("temp/in.txt");
    private Path out = new Path("temp/out");
    private final Configuration fsConfiguration = new Configuration();
    private static final TrainingSetProvider trainingSetProvider = new TrainingSetProvider();

    @Override
    public List<ChromosomeRate> challenge(List<Chromosome> population) {
        writePopulationToHDFS(population);

        try {
            ToolRunner.run(fsConfiguration, this, GlobalConfig.programArgs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return readRateFromHDFS();
    }

    private void writePopulationToHDFS(List<Chromosome> population) {
        try {
            FileSystem fs = FileSystem.get(fsConfiguration);
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fs.create(in, true)));

            for (Chromosome chromosome : population) {
                br.write(chromosome.toStream() + "\n");
            }

            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ChromosomeRate> readRateFromHDFS() {
        try {
            final List<ChromosomeRate> rates = new ArrayList<ChromosomeRate>();

            final FileSystem fs = FileSystem.get(fsConfiguration);
            for (Path path : FileUtil.stat2Paths(fs.listStatus(out))) {
                if (path.getName().startsWith("part-r-")) {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(path)));

                    final String[] split = reader.readLine().split(" ");
                    final ArrayList<ActivationFunction> activationFunctions = new ArrayList<ActivationFunction>();
                    final Integer n = Integer.valueOf(split[5]);
                    for (int i = 6; i < n+6; i++) {
                        activationFunctions.add(ActivationFunctionFactory.resolveFunctionByName(split[i]));
                    }
                    rates.add( new ChromosomeRate(new Chromosome(Integer.valueOf(split[3]), Integer.valueOf(split[4]), activationFunctions), Double.valueOf(split[1])));

                    reader.close();
                }
            }

            fs.delete(out, true);
            fs.delete(in, false);
            fs.close();

            return rates;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        final Configuration conf = getConf();
        Job job = new Job(conf, "ua.org.enishlabs.demetra");
        job.setJarByClass(DistributedPopulationChallenger.class);

        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        job.setMapperClass(MapClass.class);
        job.setReducerClass(Reduce.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.waitForCompletion(true);
        return 0;
    }

    public static class MapClass extends Mapper<LongWritable, Text, Text, Text> {

        private static final Trainer trainer;
        private static final OrganizmBuilder organizmBuilder;

        static {
            final BasicMLDataSet dataSet = trainingSetProvider.load();
            trainer = new Trainer(dataSet);
            organizmBuilder = new OrganizmBuilder(dataSet.getInputSize(), dataSet.getIdealSize());
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            final String[] params = value.toString().split(" ");

            final ArrayList<ActivationFunction> activationFunctions = new ArrayList<ActivationFunction>();
            for (int i = 4; i < params.length; i++) {
                activationFunctions.add(ActivationFunctionFactory.resolveFunctionByName(params[i]));
            }
            final Chromosome chromosome = new Chromosome(Integer.valueOf(params[1]), Integer.valueOf(params[2]), activationFunctions);
            final BasicNetwork network = organizmBuilder.build(chromosome);

            final double error = trainer.train(network);

            context.write(new Text(new ChromosomeRate(chromosome, error, network).toStream()), new Text(""));

        }
    }

    public static class Reduce extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                context.write(key, value);
            }
        }
    }
}

