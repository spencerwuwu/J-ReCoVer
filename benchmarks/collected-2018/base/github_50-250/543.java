// https://searchcode.com/api/result/114633318/

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

import com.google.common.base.Function;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.classifier.sequencelearning.hmm.HmmModel;
import org.apache.mahout.classifier.sequencelearning.hmm.HmmOnlineViterbi;
import org.apache.mahout.classifier.sequencelearning.hmm.LossyHmmSerializer;
import org.apache.mahout.math.list.IntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

/**
 * Performs all work for decoding hidden states from given sequence of observed variables for HMM using
 * {@link HmmOnlineViterbi} functionality.
 *
 * The logic is similar to {@link ForwardViterbiReducer} (since it's online algorithm there's only
 * forward pass). The reducer takes current chunk of observed variables and algorithm state from the previous step
 * and produces new algorithm state after decoding at the output.
 * Decoded sequence of hidden variables (possibly empty sequence if nothing could be decoded at the moment) is written
 * in the sequence file in the background.
 */
public class OnlineViterbiReducer extends Reducer<Text, ViterbiDataWritable, Text, ViterbiDataWritable> {
  private String outputPath;
  private HmmModel model;

  private static Logger log = LoggerFactory.getLogger(OnlineViterbiReducer.class);

  @Override
  public void setup(Context context) throws IOException {
    Configuration configuration = context.getConfiguration();

    outputPath = configuration.get("hmm.output");

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

  @Override
  public void reduce(Text key, Iterable<ViterbiDataWritable> inputs, Context context) throws IOException, InterruptedException {
    HmmOnlineViterbi onlineViterbi = null;
    ObservedSequenceWritable observations = null;

    for (ViterbiDataWritable input: inputs) {
      if (input.get() instanceof HmmOnlineViterbi)
        onlineViterbi = (HmmOnlineViterbi) input.get();
      else if (input.get() instanceof ObservedSequenceWritable)
        observations = (ObservedSequenceWritable) input.get();
    }

    if (onlineViterbi == null) {
      if (observations != null && observations.getChunkNumber() == 0) {
        log.info("No algorithm state was provided for " + key);
        log.info("Since it's first chunk, we initialize it");
        onlineViterbi = new HmmOnlineViterbi(model);
      }
      else throw new IllegalStateException("No algorithm state was provided");
    }

    if (observations == null) {
      throw new IllegalStateException("No observations was provided, but algorithm was not finished");
    }

    final int chunkNumber = observations.getChunkNumber();
    FileSystem fs = FileSystem.get(URI.create(outputPath), context.getConfiguration());

    if (outputPath == null)
      throw new IllegalStateException("No output path was provided");

    final SequenceFile.Writer writer = SequenceFile.createWriter(fs, context.getConfiguration(),
      new Path(outputPath + "/" + key, Integer.toString(chunkNumber)), IntWritable.class, HiddenSequenceWritable.class);

    final IntArrayList result = new IntArrayList(observations.getLength());
    onlineViterbi.setOutput(new Function<int[], Void>() {
          @Override
          public Void apply(int[] input) {
            for (int anInput : input) result.add(anInput);

            return null;
          }
        });

    onlineViterbi.setModel(model);
    onlineViterbi.process(new IntArrayList(observations.getData()).toList());

    if (observations.isLastChunk()) {
      log.info("Processing last chunk for " + key + ". Finishing at " + onlineViterbi.getPosition());
      onlineViterbi.finish();
    }
    else
      context.write(key, new ViterbiDataWritable(onlineViterbi));

    writer.append(new IntWritable(chunkNumber), new HiddenSequenceWritable(result));
    writer.close();
  }

  public static void main(String[] args) throws Exception {
    System.exit(ToolRunner.run(new Configuration(), new OnlineViterbiDriver(), args));
  }
}

