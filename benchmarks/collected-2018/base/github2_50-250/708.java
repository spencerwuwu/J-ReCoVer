// https://searchcode.com/api/result/114633323/

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

package org.apache.mahout.classifier.sequencelearning.hmm.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.math.VarIntWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Takes as the input {@link VarIntWritable} (last decoded hidden state from the previous step) and
 * {@link BackpointersWritable}, and produces as the output {@link VarIntWritable} (last decoded hidden state
 * for the next step). Also it writes decoded {@link HiddenSequenceWritable} in the background
 */
class BackwardViterbiReducer extends Reducer<Text, ViterbiDataWritable, Text, ViterbiDataWritable> {
  private String path;

  private static final Logger log = LoggerFactory.getLogger(BackwardViterbiReducer.class);

  public static interface ResultHandler {
    public void handle(int[] decoded) throws IOException, InterruptedException;
  }

  private ResultHandler resultHandler;

  public void setResultHandler(ResultHandler handler) {
    resultHandler = handler;
  }

  @Override
  protected void setup(Reducer.Context context)
              throws IOException,
                     InterruptedException {
    path = context.getConfiguration().get("hmm.output");
  }

  @Override
  public void reduce(final Text key, Iterable<ViterbiDataWritable> values,
                     final Context context) throws IOException, InterruptedException {
    final Configuration configuration = context.getConfiguration();

    int[][] backpointers = null;
    int lastState = -1;
    int chunkNumber = -1;

    for (ViterbiDataWritable data: values) {
      if (data.get() instanceof BackpointersWritable) {
        backpointers = ((BackpointersWritable) data.get()).backpointers;
        chunkNumber = ((BackpointersWritable) data.get()).getChunkNumber();
      }
      else if (data.get() instanceof VarIntWritable) {
        lastState = ((VarIntWritable) data.get()).get();
      }
      else if (data.get() instanceof HiddenStateProbabilitiesWritable) {
        if (lastState == -1)
          lastState = ((HiddenStateProbabilitiesWritable) data.get()).getMostProbableState();
      }
      else {
        throw new IOException("Unsupported backward data provided");
      }
    }

    log.info("Performing backward Viterbi pass on " + key + " / " + chunkNumber);

    if (backpointers == null && lastState != -1) {
      log.info("No backpointers provided, but last state was computed from probabilities");
      context.write(key, new ViterbiDataWritable(lastState));
      return;
    }
    else if (backpointers == null)
      throw new IllegalStateException("Backpointers array was not provided to the reducer");

    if (lastState < 0)
      throw new IllegalStateException("Last state was not initialized");
    if (chunkNumber < 0)
      throw new IllegalStateException("Chunk number was not initialized");

    log.info("last state: " + lastState);
    int chunkLength = backpointers.length + 1;
    final int[] path = new int[chunkLength];
    path[chunkLength - 1] = lastState;
    for (int i = chunkLength-2; i >= 0; --i) {
      path[i] = backpointers[i][path[i+1]];
    }

    final String outputPath = this.path;
    final int chunk = chunkNumber;
    if (resultHandler == null) {
      resultHandler = new ResultHandler() {
        @Override
        public void handle(int[] decoded) throws IOException, InterruptedException {
          FileSystem fs = FileSystem.get(URI.create(outputPath), configuration);
          Path chunkPath = new Path(outputPath + "/" + key, String.valueOf(chunk));
          SequenceFile.Writer writer =  SequenceFile.createWriter(fs, configuration,
            chunkPath,
            IntWritable.class, HiddenSequenceWritable.class);

          writer.append(new IntWritable(chunk), new HiddenSequenceWritable(path));

          context.write(key, new ViterbiDataWritable(path[0]));
          writer.close();
          log.info("new last state: " + path[0]);
          log.info("Decoded path was written to: " + chunkPath);
        }
      };
    }

    resultHandler.handle(path);
  }
}

