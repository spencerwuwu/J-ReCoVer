// https://searchcode.com/api/result/2223664/

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
package org.apache.mahout.clustering.lda;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.common.IntPairWritable;

import com.google.common.base.Preconditions;

/**
 * A very simple reducer which simply logSums the input doubles and outputs a new double for sufficient
 * statistics, and sums log likelihoods.
 */
public class LDAReducer extends Reducer<IntPairWritable,DoubleWritable,IntPairWritable,DoubleWritable> {
  
  @Override
  public void reduce(IntPairWritable topicWord,
                     Iterable<DoubleWritable> values,
                     Context context) throws java.io.IOException, InterruptedException {
    
    // sum likelihoods
    if (topicWord.getSecond() == LDADriver.LOG_LIKELIHOOD_KEY) {
      double accum = 0.0;
      for (DoubleWritable vw : values) {
        double v = vw.get();
        Preconditions.checkArgument(!Double.isNaN(v),
                                    "Found NaN for topic=(%d,%d)", topicWord.getFirst(), topicWord.getSecond());
        accum += v;
      }
      context.write(topicWord, new DoubleWritable(accum));
    } else { // log sum sufficient statistics.
      double accum = Double.NEGATIVE_INFINITY;
      for (DoubleWritable vw : values) {
        double v = vw.get();
        Preconditions.checkArgument(!Double.isNaN(v),
                                    "Found NaN for topic = (%d,%d)", topicWord.getFirst(), topicWord.getSecond());
        accum = LDAUtil.logSum(accum, v);
        Preconditions.checkArgument(!Double.isNaN(accum),
                                    "Accumulated NaN for topic = (%d,%d)", topicWord.getFirst(), topicWord.getSecond());
      }
      context.write(topicWord, new DoubleWritable(accum));
    }
  }
}

