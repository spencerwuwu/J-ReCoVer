// https://searchcode.com/api/result/114633332/

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
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.classifier.sequencelearning.hmm.HmmModel;
import org.apache.mahout.classifier.sequencelearning.hmm.LossyHmmSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

/**
 * Takes at the input {@link ObservedSequenceWritable} and {@link HiddenStateProbabilitiesWritable} from the previous step
 * and produces {@link HiddenStateProbabilitiesWritable} for the next step, by performing forward Viterbi pass
 */
class ForwardViterbiReducer extends Reducer<Text, ViterbiDataWritable, Text, ViterbiDataWritable> {
  private SequenceFile.Writer backpointersWriter;
  private HmmModel model;
  private ResultHandler resultHandler;

  public static interface ResultHandler {
    public void handle(String sequenceName, int[][] backpointers, int chunkNumber,
                       double[] hiddenStateProbabilities) throws IOException, InterruptedException;
  }

  private static Logger log = LoggerFactory.getLogger(ForwardViterbiReducer.class);

  public void setResultHandler(ResultHandler handler) {
    resultHandler = handler;
  }

  public HmmModel getModel() {
    return model;
  }

  public void setModel(HmmModel model) {
    this.model = model;
  }

  protected void setup(Reducer.Context context)
              throws IOException,
                     InterruptedException {
    Configuration configuration = context.getConfiguration();
    String backpointersPath = configuration.get("hmm.backpointers");
    FileSystem intermediateFileSystem = FileSystem.get(URI.create(backpointersPath),
      context.getConfiguration());

    if (backpointersPath != null)
      backpointersWriter = SequenceFile.createWriter(intermediateFileSystem,
        context.getConfiguration(), new Path(backpointersPath, context.getTaskAttemptID().toString()),
        Text.class, ViterbiDataWritable.class, SequenceFile.CompressionType.RECORD);

    String hmmModelFileName = configuration.get("hmm.model");
    log.info("Trying to load model with name " + hmmModelFileName);
    for (Path cachePath: DistributedCache.getLocalCacheFiles(configuration)) {
      if (cachePath.getName().endsWith(hmmModelFileName))
      {
        DataInputStream modelStream = new DataInputStream(new FileInputStream(cachePath.toString())) ;
        model = LossyHmmSerializer.deserialize(modelStream);
        log.info("Model loaded");
        modelStream.close();
        break;
      }
    }

    if (model == null)
      throw new IllegalStateException("Model " + hmmModelFileName + " was not loaded");
  }

  protected void cleanup(Reducer.Context context)
                throws IOException,
                       InterruptedException {
    if (backpointersWriter != null)
      backpointersWriter.close();
  }

  @Override
  public void reduce(final Text key, Iterable<ViterbiDataWritable> values,
                     final Context context) throws IOException, InterruptedException {
    log.debug("Reducing data for " + key.toString());
    Iterator<ViterbiDataWritable> iterator = values.iterator();
    double[] probabilities = null;
    int[] observations = null;
    int chunkNumber = -1;
    HiddenStateProbabilitiesWritable lastProbabilities;
    if (resultHandler == null) {
      resultHandler = new ResultHandler() {
        @Override
        public void handle(String sequenceName, int[][] backpointers, int chunkNumber, double[] hiddenStateProbabilities) throws IOException, InterruptedException {
          BackpointersWritable backpointersWritable = new BackpointersWritable(
            backpointers, chunkNumber);
          backpointersWriter.append(key, new ViterbiDataWritable(backpointersWritable));
          context.write(key, ViterbiDataWritable.fromInitialProbabilities(
            new HiddenStateProbabilitiesWritable(hiddenStateProbabilities)));
        }
      };
    }

    while (iterator.hasNext()) {
      ViterbiDataWritable data = iterator.next();
      Writable value = data.get();
      if (value instanceof HiddenStateProbabilitiesWritable) {
        lastProbabilities =  ((HiddenStateProbabilitiesWritable) value);
        probabilities = lastProbabilities.toProbabilityArray();
        log.debug("Successfully read probabilities from the previous step");
      }
      else if (value instanceof ObservedSequenceWritable) {
        observations = ((ObservedSequenceWritable) value).getData();
        chunkNumber = ((ObservedSequenceWritable) value).getChunkNumber();
        log.debug("Successfully read observations from the current step");
      }
      else
        throw new IOException("Unsupported Writable provided to the reducer");
    }

    if (observations == null) {
      log.debug("Seems like everything is processed already, skipping this sequence");
      return;
    }
    if (probabilities == null) {
      if (chunkNumber != 0)
        throw new IllegalStateException("No hidden state probabilities were provided, but chunk number is not 0");
      log.debug("Seems like it's first chunk, so defining probabilities to initial");
      probabilities = getInitialProbabilities(model, observations[0]);
    }

    if (chunkNumber < 0)
      throw new IllegalStateException("Chunk number was not initialized");

    log.info("Performing forward pass on " + key + "/" + chunkNumber);
    int[][] backpointers = forward(observations, model, probabilities);

    resultHandler.handle(key.toString(), backpointers, chunkNumber, probabilities);
  }

  private static double[] getInitialProbabilities(HmmModel model, int startObservation) {
    double[] probs = new double[model.getNrOfHiddenStates()];
    for (int h = 0; h < probs.length; ++h)
      probs[h] = Math.log(model.getInitialProbabilities().getQuick(h) + Double.MIN_VALUE) +
        Math.log(model.getEmissionMatrix().getQuick(h, startObservation));
    return probs;
  }

  private static double getTransitionProbability(HmmModel model, int i, int j) {
    return Math.log(model.getTransitionMatrix().getQuick(j, i) + Double.MIN_VALUE);
  }

  private static double getEmissionProbability(HmmModel model, int o, int h) {
    return Math.log(model.getEmissionMatrix().get(h, o) + Double.MIN_VALUE);
  }

  private static int[][] forward(int[] observations, HmmModel model, double[] probs) {
    double[] nextProbs = new double[model.getNrOfHiddenStates()];
    int[][] backpoints = new int[observations.length - 1][model.getNrOfHiddenStates()];

    for (int i = 1; i < observations.length; ++i) {
      for (int t = 0; t < model.getNrOfHiddenStates(); ++t) {
        int maxState = 0;
        double maxProb = -Double.MAX_VALUE;
        for (int h = 0; h < model.getNrOfHiddenStates(); ++h) {
          double currentProb = getTransitionProbability(model, t, h) + probs[h];
          if (maxProb < currentProb) {
            maxState = h;
            maxProb = currentProb;
          }
        }
        nextProbs[t] = maxProb + getEmissionProbability(model, observations[i], t);
        backpoints[i - 1][t] = maxState;
      }
      System.arraycopy(nextProbs, 0, probs, 0, probs.length);
    }

    return backpoints;
  }
}

