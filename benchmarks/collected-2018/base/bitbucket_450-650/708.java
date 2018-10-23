// https://searchcode.com/api/result/103262267/

/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.grouplens.lenskit.eval.traintest;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import org.apache.commons.lang3.tuple.Pair;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.data.snapshot.PreferenceSnapshot;
import org.grouplens.lenskit.eval.AbstractTask;
import org.grouplens.lenskit.eval.TaskExecutionException;
import org.grouplens.lenskit.eval.algorithm.AlgorithmInstance;
import org.grouplens.lenskit.eval.algorithm.ExternalAlgorithmInstance;
import org.grouplens.lenskit.eval.algorithm.LenskitAlgorithmInstance;
import org.grouplens.lenskit.eval.data.traintest.TTDataSet;
import org.grouplens.lenskit.eval.metrics.Metric;
import org.grouplens.lenskit.eval.metrics.TestUserMetric;
import org.grouplens.lenskit.symbols.Symbol;
import org.grouplens.lenskit.util.parallel.TaskGroupRunner;
import org.grouplens.lenskit.util.table.Table;
import org.grouplens.lenskit.util.table.TableBuilder;
import org.grouplens.lenskit.util.table.TableLayout;
import org.grouplens.lenskit.util.table.TableLayoutBuilder;
import org.grouplens.lenskit.util.table.writer.CSVWriter;
import org.grouplens.lenskit.util.table.writer.MultiplexedTableWriter;
import org.grouplens.lenskit.util.table.writer.TableWriter;
import org.grouplens.lenskit.util.table.writer.TableWriters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The command that run the algorithm instance and output the prediction result file and the evaluation result file
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TrainTestEvalTask extends AbstractTask<Table> {
    private static final Logger logger = LoggerFactory.getLogger(TrainTestEvalTask.class);

    private List<TTDataSet> dataSets;
    private List<AlgorithmInstance> algorithms;
    private List<TestUserMetric> metrics;
    private List<Pair<Symbol,String>> predictChannels;
    private boolean isolate;
    private File outputFile;
    private File userOutputFile;
    private File predictOutputFile;
    // default value for recommendation set size
    private int numRecs = 5;

    private int commonColumnCount;
    private TableLayout outputLayout;
    private TableLayout userLayout;
    private TableLayout predictLayout;

    private TableWriter output;
    private TableBuilder outputInMemory;
    private TableWriter userOutput;
    private TableWriter predictOutput;

    private Map<String, Integer> dataColumns;
    private Map<String, Integer> algoColumns;
    private List<TestUserMetric> predictMetrics;
    private TableLayout masterLayout;
    private List<ModelMetric> modelMetrics;


    public TrainTestEvalTask() {
        this("train-test");
    }

    public TrainTestEvalTask(String name) {
        super(name);
        dataSets = new LinkedList<TTDataSet>();
        algorithms = new LinkedList<AlgorithmInstance>();
        metrics = new LinkedList<TestUserMetric>();
        modelMetrics = new LinkedList<ModelMetric>();
        predictChannels = new LinkedList<Pair<Symbol, String>>();
        outputFile = new File("train-test-results.csv");
        isolate = false;
    }

    public TrainTestEvalTask addDataset(TTDataSet source) {
        dataSets.add(source);
        return this;
    }

    public TrainTestEvalTask addAlgorithm(LenskitAlgorithmInstance algorithm) {
        algorithms.add(algorithm);
        return this;
    }

    public TrainTestEvalTask addExternalAlgorithm(ExternalAlgorithmInstance algorithm) {
        algorithms.add(algorithm);
        return this;
    }

    public TrainTestEvalTask addMetric(TestUserMetric metric) {
        metrics.add(metric);
        return this;
    }

    public TrainTestEvalTask addMetric(Class<? extends TestUserMetric> metricClass) throws IllegalAccessException, InstantiationException {
        return addMetric(metricClass.newInstance());
    }

    /**
     * Add a metric that may write multiple columns per algorithm.
     * @param file The output file.
     * @param columns The column headers.
     * @param metric The metric function. It should return a list of table rows, each of which has
     *               entries corresponding to each column.
     * @return The command (for chaining).
     */
    public TrainTestEvalTask addMultiMetric(File file, List<String> columns, Function<Recommender,List<List<Object>>> metric) {
        modelMetrics.add(new FunctionMultiModelMetric(file, columns, metric));
        return this;
    }

    /**
     * Add a metric that takes some metric of an algorithm.
     * @param columns The column headers.
     * @param metric The metric function. It should return a list of table rows, each of which has
     *               entries corresponding to each column.
     * @return The command (for chaining).
     */
    public TrainTestEvalTask addMetric(List<String> columns, Function<Recommender,List<Object>> metric) {
        modelMetrics.add(new FunctionModelMetric(columns, metric));
        return this;
    }

    /**
     * Add a channel to be recorded with predict output.
     *
     * @param channelSym The channel to output.
     * @return The command (for chaining)
     * @see #addWritePredictionChannel
     */
    public TrainTestEvalTask addWritePredictionChannel(@Nonnull Symbol channelSym) {
        return addWritePredictionChannel(channelSym, null);
    }

    /**
     * Add a channel to be recorded with predict output.
     *
     * @param channelSym The channel to record, if present in the prediction output vector.
     * @param label   The column label. If {@code null}, the channel symbol's name is used.
     * @return The command (for chaining).
     * @see #setPredictOutput(File)
     */
    public TrainTestEvalTask addWritePredictionChannel(@Nonnull Symbol channelSym,
                                                       @Nullable String label) {
        Preconditions.checkNotNull(channelSym, "channel is null");
        if (label == null) {
            label = channelSym.getName();
        }
        Pair<Symbol, String> entry = Pair.of(channelSym, label);
        predictChannels.add(entry);
        return this;
    }

    public TrainTestEvalTask setOutput(File file) {
        outputFile = file;
        return this;
    }

    public TrainTestEvalTask setOutput(String fn) {
        return setOutput(new File(fn));
    }

    public TrainTestEvalTask setUserOutput(File file) {
        userOutputFile = file;
        return this;
    }

    public TrainTestEvalTask setUserOutput(String fn) {
        return setUserOutput(new File(fn));
    }

    public TrainTestEvalTask setPredictOutput(File file) {
        predictOutputFile = file;
        return this;
    }

    public TrainTestEvalTask setPredictOutput(String fn) {
        return setPredictOutput(new File(fn));
    }

    /**
     * Control whether the train-test evaluator will isolate data sets.  If set to {@code true},
     * then each data set will be run in turn, with no inter-data-set parallelism.  This can
     * reduce memory usage for some large runs, keeping the data from only a single data set in
     * memory at a time.  Otherwise (the default), individual algorithm/data-set runs may be freely
     * intermingled.
     *
     * @param iso Whether to isolate data sets.
     * @return The task (for chaining).
     */
    public TrainTestEvalTask setIsolate(boolean iso) {
        isolate = iso;
        return this;
    }

    List<TTDataSet> dataSources() {
        return dataSets;
    }

    List<AlgorithmInstance> getAlgorithms() {
        return algorithms;
    }

    List<TestUserMetric> getMetrics() {
        return metrics;
    }

    List<Pair<Symbol,String>> getPredictionChannels() {
        return predictChannels;
    }

    File getOutput() {
        return outputFile;
    }

    File getPredictOutput() {
        return predictOutputFile;
    }

    public int getNumRecs() {
        return numRecs;
    }

    public TrainTestEvalTask setNumRecs(int numRecs) {
        this.numRecs = numRecs;
        return this;
    }

    /**
     * Run the evaluation on the train test data source files
     *
     * @return The summary output table.
     * @throws org.grouplens.lenskit.eval.TaskExecutionException
     *          Failure of the evaluation
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public Table perform() throws TaskExecutionException {
        List<List<TrainTestEvalJob>> jobGroups = makeJobGroups();
        setupTableLayouts();

        logger.info("Starting evaluation");
        Closer closer = Closer.create();
        try {
            try {
                this.prepareEval(closer);
                try {
                    runEvaluations(jobGroups);
                } finally {
                    cleanUp();
                }
            } catch (Throwable th) {
                throw closer.rethrow(th, TaskExecutionException.class);
            } finally {
                closer.close();
            }
        } catch (IOException e) {
            throw new TaskExecutionException("I/O error", e);
        }

        return outputInMemory.build();
    }

    private void runEvaluations(List<List<TrainTestEvalJob>> jobGroups) throws TaskExecutionException {
        int nthreads = getProject().getConfig().getThreadCount();
        logger.info("Running evaluator with {} threads", nthreads);
        ExecutorService exec = Executors.newFixedThreadPool(nthreads);
        for (List<TrainTestEvalJob> group: jobGroups) {
            TaskGroupRunner runner = TaskGroupRunner.create(exec);
            runner.submitAll(group);
            try {
                runner.waitForAll();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof TrainTestJobException) {
                    cause = cause.getCause();
                }
                throw new TaskExecutionException(cause);
            } catch (TrainTestJobException e) {
                throw new TaskExecutionException(e);
            }
        }
    }

    List<List<TrainTestEvalJob>> makeJobGroups() {
        List<List<TrainTestEvalJob>> jobGroups = Lists.newArrayList();
        for (TTDataSet dataset : dataSets) {
            List<TrainTestEvalJob> jobs = makeJobs(dataset);
            jobGroups.add(jobs);
        }
        if (!isolate) {
            return Collections.singletonList(
                    (List<TrainTestEvalJob>) Lists.newArrayList(Iterables.concat(jobGroups)));
        } else {
            return jobGroups;
        }
    }

    private List<TrainTestEvalJob> makeJobs(TTDataSet data) {
        List<TrainTestEvalJob> jobs = Lists.newArrayListWithCapacity(algorithms.size());
        final Provider<PreferenceSnapshot> snap = SharedPreferenceSnapshot.provider(data);

        for (AlgorithmInstance algo: algorithms) {
            Function<TableWriter, TableWriter> prefix = prefixFunction(algo, data);
            TrainTestEvalJob job = new TrainTestEvalJob(
                    algo, metrics, modelMetrics, predictChannels, data, snap,
                    Suppliers.compose(prefix, outputTableSupplier()),
                    Suppliers.compose(prefix, userTableSupplier()),
                    Suppliers.compose(prefix, predictTableSupplier()),
                    numRecs);
            jobs.add(job);
        }
        return jobs;
    }

    private void setupTableLayouts() {
        TableLayoutBuilder master = new TableLayoutBuilder();
        layoutCommonColumns(master);
        masterLayout = master.build();

        commonColumnCount = master.getColumnCount();

        outputLayout = layoutAggregateOutput(master);
        userLayout = layoutUserTable(master);
        predictLayout = layoutPredictionTable(master);

        // FIXME This doesn't seem right in the face of top-N metrics
        predictMetrics = metrics;
    }

    /**
     * Get the master table layout.  This can be used by metrics to prefix their own tables.
     *
     * @return The master table layout. This layout must not be modified.
     */
    public TableLayout getMasterLayout() {
        return masterLayout;
    }

    private void layoutCommonColumns(TableLayoutBuilder master) {
        master.addColumn("Algorithm");
        dataColumns = new HashMap<String, Integer>();
        for (TTDataSet ds : dataSets) {
            for (String attr : ds.getAttributes().keySet()) {
                if (!dataColumns.containsKey(attr)) {
                    dataColumns.put(attr, master.getColumnCount());
                    master.addColumn(attr);
                }
            }
        }

        algoColumns = new HashMap<String, Integer>();
        for (AlgorithmInstance algo : algorithms) {
            for (String attr : algo.getAttributes().keySet()) {
                if (!algoColumns.containsKey(attr)) {
                    algoColumns.put(attr, master.getColumnCount());
                    master.addColumn(attr);
                }
            }
        }
    }

    private TableLayout layoutAggregateOutput(TableLayoutBuilder master) {
        TableLayoutBuilder output = master.clone();
        output.addColumn("BuildTime");
        output.addColumn("TestTime");

        for (ModelMetric ev: modelMetrics) {
            for (String c: ev.getColumnLabels()) {
                output.addColumn(c);
            }
        }

        for (TestUserMetric ev : metrics) {
            List<String> columnLabels = ev.getColumnLabels();
            if (columnLabels != null) {
                for (String c : columnLabels) {
                    output.addColumn(c);
                }
            }
        }

        return output.build();
    }

    private TableLayout layoutUserTable(TableLayoutBuilder master) {
        TableLayoutBuilder perUser = master.clone();
        perUser.addColumn("User");

        for (TestUserMetric ev : metrics) {
            List<String> userColumnLabels = ev.getUserColumnLabels();
            if (userColumnLabels != null) {
                for (String c : userColumnLabels) {
                    perUser.addColumn(c);
                }
            }
        }

        return perUser.build();
    }

    private TableLayout layoutPredictionTable(TableLayoutBuilder master) {
        TableLayoutBuilder eachPred = master.clone();
        eachPred.addColumn("User");
        eachPred.addColumn("Item");
        eachPred.addColumn("Rating");
        eachPred.addColumn("Prediction");
        for (Pair<Symbol,String> pair: predictChannels) {
            eachPred.addColumn(pair.getRight());
        }

        return eachPred.build();
    }

    /**
     * Prepare the evaluation by opening all outputs and initializing metrics.
     */
    private void prepareEval(Closer closer) throws IOException {
        logger.info("Starting evaluation");
        List<TableWriter> tableWriters = new ArrayList<TableWriter>();
        outputInMemory = new TableBuilder(outputLayout);
        tableWriters.add(outputInMemory);
        if (outputFile != null) {
            tableWriters.add(closer.register(CSVWriter.open(outputFile, outputLayout)));
        }
        output = new MultiplexedTableWriter(outputLayout, tableWriters);
        if (userOutputFile != null) {
            userOutput = closer.register(CSVWriter.open(userOutputFile, userLayout));
        }
        if (predictOutputFile != null) {
            predictOutput = closer.register(CSVWriter.open(predictOutputFile, predictLayout));
        }
        for (Metric<TrainTestEvalTask> metric : Iterables.concat(predictMetrics, modelMetrics)) {
            metric.startEvaluation(this);
        }
    }

    /**
     * Finalize metrics and close output files.
     */
    private void cleanUp() throws IOException {
        for (Metric<TrainTestEvalTask> metric : Iterables.concat(predictMetrics, modelMetrics)) {
            metric.finishEvaluation();
        }
        if (output == null) {
            throw new IllegalStateException("evaluation not running");
        }
        logger.info("Evaluation finished");
        output = null;
        userOutput = null;
        predictOutput = null;
    }

    /**
     * Get the evaluation's output table. Used by job groups to set up the
     * output for their jobs.
     *
     * @return A supplier for the table writer for this evaluation.
     * @throws IllegalStateException if the job has not been started or is
     *                               finished.
     */
    @Nonnull
    Supplier<TableWriter> outputTableSupplier() {
        return new Supplier<TableWriter>() {
            @Override
            public TableWriter get() {
                Preconditions.checkState(output != null, "evaluation not running");
                return output;
            }
        };
    }

    /**
     * Get the prediction output table.
     *
     * @return The table writer for the prediction output.
     */
    @Nonnull
    Supplier<TableWriter> predictTableSupplier() {
        return new Supplier<TableWriter>() {
            @Override
            public TableWriter get() {
                return predictOutput;
            }
        };
    }

    /**
     * Get the user output table.
     *
     * @return The table writer for the prediction output.
     */
    @Nonnull
    Supplier<TableWriter> userTableSupplier() {
        return new Supplier<TableWriter>() {
            @Override
            public TableWriter get() {
                return userOutput;
            }
        };
    }

    /**
     * Function version of {@link #prefixTable(TableWriter, org.grouplens.lenskit.eval.algorithm.AlgorithmInstance, TTDataSet)}. Intended
     * for use with {@link com.google.common.base.Suppliers#compose(com.google.common.base.Function, Supplier)}.
     */
    public Function<TableWriter, TableWriter> prefixFunction(
            final AlgorithmInstance algorithm,
            final TTDataSet dataSet) {
        return new Function<TableWriter, TableWriter>() {
            @Override
            public TableWriter apply(TableWriter base) {
                return prefixTable(base, algorithm, dataSet);
            }
        };
    }

    /**
     * Prefix a table for a particular algorithm and data set.
     *
     * @param base      The table to prefix.
     * @param algorithm The algorithm to prefix for.
     * @param dataSet   The data set to prefix for.
     * @return A prefixed table, suitable for outputting the results of evaluating
     *         {@code algorithm} on {@code dataSet}, or {@code null} if {@code base} is null.
     */
    public TableWriter prefixTable(TableWriter base,
                                   AlgorithmInstance algorithm, TTDataSet dataSet) {
        if (base == null) {
            return null;
        }

        Object[] prefix = new Object[commonColumnCount];
        prefix[0] = algorithm.getName();
        for (Map.Entry<String, Object> attr : dataSet.getAttributes().entrySet()) {
            int idx = dataColumns.get(attr.getKey());
            prefix[idx] = attr.getValue();
        }
        for (Map.Entry<String, Object> attr : algorithm.getAttributes().entrySet()) {
            int idx = algoColumns.get(attr.getKey());
            prefix[idx] = attr.getValue();
        }
        return TableWriters.prefixed(base, prefix);
    }
}

