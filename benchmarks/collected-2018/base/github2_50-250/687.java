// https://searchcode.com/api/result/112051071/

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.math.hadoop;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.math.function.Functions;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

public final class TimesSquaredJob {

  public static final String INPUT_VECTOR = "DistributedMatrix.times.inputVector";
  public static final String IS_SPARSE_OUTPUT = "DistributedMatrix.times.outputVector.sparse";
  public static final String OUTPUT_VECTOR_DIMENSION = "DistributedMatrix.times.output.dimension";

  public static final String OUTPUT_VECTOR_FILENAME = "DistributedMatrix.times.outputVector";

  private TimesSquaredJob() { }

  public static JobConf createTimesSquaredJobConf(Vector v, 
                                                  Path matrixInputPath, 
                                                  Path outputVectorPath) throws IOException {
    return createTimesSquaredJobConf(v,
                                     matrixInputPath,
                                     outputVectorPath,
                                     TimesSquaredMapper.class,
                                     VectorSummingReducer.class);
  }

  public static JobConf createTimesJobConf(Vector v,
                                           int outDim,
                                           Path matrixInputPath,
                                           Path outputVectorPath) throws IOException {
    return createTimesSquaredJobConf(v,
                                     outDim,
                                     matrixInputPath,
                                     outputVectorPath,
                                     TimesMapper.class,
                                     VectorSummingReducer.class);
  }


  public static JobConf createTimesSquaredJobConf(Vector v,
                                                  Path matrixInputPath,
                                                  Path outputVectorPathBase,
                                                  Class<? extends TimesSquaredMapper> mapClass,
                                                  Class<? extends VectorSummingReducer> redClass) throws IOException {
    return createTimesSquaredJobConf(v, v.size(), matrixInputPath, outputVectorPathBase, mapClass, redClass);
  }

  public static JobConf createTimesSquaredJobConf(Vector v,
                                                  int outputVectorDim,
                                                  Path matrixInputPath,
                                                  Path outputVectorPathBase,
                                                  Class<? extends TimesSquaredMapper> mapClass,
                                                  Class<? extends VectorSummingReducer> redClass) throws IOException {
    JobConf conf = new JobConf(TimesSquaredJob.class);
    conf.setJobName("TimesSquaredJob: " + matrixInputPath);
    FileSystem fs = FileSystem.get(conf);
    matrixInputPath = fs.makeQualified(matrixInputPath);
    outputVectorPathBase = fs.makeQualified(outputVectorPathBase);

    long now = System.nanoTime();
    Path inputVectorPath = new Path(outputVectorPathBase, INPUT_VECTOR + '/' + now);
    SequenceFile.Writer inputVectorPathWriter = new SequenceFile.Writer(fs,
            conf, inputVectorPath, NullWritable.class, VectorWritable.class);
    VectorWritable inputVW = new VectorWritable(v);
    inputVectorPathWriter.append(NullWritable.get(), inputVW);
    inputVectorPathWriter.close();
    URI ivpURI = inputVectorPath.toUri();
    DistributedCache.setCacheFiles(new URI[] {ivpURI}, conf);
    fs.deleteOnExit(inputVectorPath);

    conf.set(INPUT_VECTOR, ivpURI.toString());
    conf.setBoolean(IS_SPARSE_OUTPUT, !(v instanceof DenseVector));
    conf.setInt(OUTPUT_VECTOR_DIMENSION, outputVectorDim);
    FileInputFormat.addInputPath(conf, matrixInputPath);
    conf.setInputFormat(SequenceFileInputFormat.class);
    FileOutputFormat.setOutputPath(conf, new Path(outputVectorPathBase, OUTPUT_VECTOR_FILENAME));
    conf.setMapperClass(mapClass);
    conf.setMapOutputKeyClass(NullWritable.class);
    conf.setMapOutputValueClass(VectorWritable.class);
    conf.setReducerClass(redClass);
    conf.setCombinerClass(redClass);
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    conf.setOutputKeyClass(NullWritable.class);
    conf.setOutputValueClass(VectorWritable.class);
    return conf;
  }

  public static Vector retrieveTimesSquaredOutputVector(JobConf conf) throws IOException {
    Path outputPath = FileOutputFormat.getOutputPath(conf);
    FileSystem fs = FileSystem.get(conf);
    Path outputFile = new Path(outputPath, "part-00000");
    SequenceFile.Reader reader = new SequenceFile.Reader(fs, outputFile, conf);
    NullWritable n = NullWritable.get();
    VectorWritable v = new VectorWritable();
    reader.next(n,v);
    Vector vector = v.get();
    reader.close();
    fs.deleteOnExit(outputFile);
    return vector;
  }

  public static class TimesSquaredMapper<T extends WritableComparable> extends MapReduceBase
      implements Mapper<T,VectorWritable, NullWritable,VectorWritable> {

    Vector outputVector;
    OutputCollector<NullWritable,VectorWritable> out;
    private Vector inputVector;

    @Override
    public void configure(JobConf conf) {
      try {
        URI[] localFiles = DistributedCache.getCacheFiles(conf);
        Preconditions.checkArgument(localFiles != null && localFiles.length >= 1,
          "missing paths from the DistributedCache" );
        Path inputVectorPath = new Path(localFiles[0].getPath());
        FileSystem fs = inputVectorPath.getFileSystem(conf);

        SequenceFile.Reader reader = new SequenceFile.Reader(fs,
          inputVectorPath,
          conf);
        VectorWritable val = new VectorWritable();
        NullWritable nw = NullWritable.get();
        reader.next(nw, val);
        reader.close();
        inputVector = val.get();
        if (!(inputVector instanceof SequentialAccessSparseVector || inputVector instanceof DenseVector)) {
          inputVector = new SequentialAccessSparseVector(inputVector);
        }
        int outDim = conf.getInt(OUTPUT_VECTOR_DIMENSION, Integer.MAX_VALUE);
        outputVector = conf.getBoolean(IS_SPARSE_OUTPUT, false)
                     ? new RandomAccessSparseVector(outDim, 10)
                     : new DenseVector(outDim);
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
    }

    @Override
    public void map(T rowNum,
                    VectorWritable v,
                    OutputCollector<NullWritable,VectorWritable> out,
                    Reporter rep) throws IOException {
      this.out = out;
      double d = scale(v);
      if (d == 1.0) {
        outputVector.assign(v.get(), Functions.PLUS);
      } else if (d != 0.0) {
        outputVector.assign(v.get(), Functions.plusMult(d));
      }
    }

    protected double scale(VectorWritable v) {
      return v.get().dot(inputVector);
    }

    @Override
    public void close() throws IOException {
      out.collect(NullWritable.get(), new VectorWritable(outputVector));
    }

  }

  public static class TimesMapper extends TimesSquaredMapper<IntWritable> {
    @Override
    public void map(IntWritable rowNum,
                    VectorWritable v,
                    OutputCollector<NullWritable,VectorWritable> out,
                    Reporter rep) {
      this.out = out;
      double d = scale(v);
      if (d != 0.0) {
        outputVector.setQuick(rowNum.get(), d);
      }
    }
  }

  public static class VectorSummingReducer extends MapReduceBase
      implements Reducer<NullWritable,VectorWritable,NullWritable,VectorWritable> {

    private Vector outputVector;

    @Override
    public void configure(JobConf conf) {
      int outputDimension = conf.getInt(OUTPUT_VECTOR_DIMENSION, Integer.MAX_VALUE);
      outputVector = conf.getBoolean(IS_SPARSE_OUTPUT, false)
                   ? new RandomAccessSparseVector(outputDimension, 10)
                   : new DenseVector(outputDimension);
    }

    @Override
    public void reduce(NullWritable n,
                       Iterator<VectorWritable> vectors,
                       OutputCollector<NullWritable,VectorWritable> out,
                       Reporter reporter) throws IOException {
      while (vectors.hasNext()) {
        VectorWritable v = vectors.next();
        if (v != null) {
          v.get().addTo(outputVector);
        }
      }
      out.collect(NullWritable.get(), new VectorWritable(outputVector));
    }
  }

}

