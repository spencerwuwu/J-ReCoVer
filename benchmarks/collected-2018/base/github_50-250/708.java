// https://searchcode.com/api/result/2223264/

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

package org.apache.mahout.classifier.bayes.mapreduce.bayes;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.common.StringTuple;

/**
 * Can also be used as a local Combiner beacuse only two values should be there inside the values
 */
public class BayesThetaNormalizerReducer extends MapReduceBase implements
    Reducer<StringTuple,DoubleWritable,StringTuple,DoubleWritable> {

  @Override
  public void reduce(StringTuple key,
                     Iterator<DoubleWritable> values,
                     OutputCollector<StringTuple,DoubleWritable> output,
                     Reporter reporter) throws IOException {
    // Key is label,word, value is the number of times we've seen this label word per local node. Output is the same
    double weightSumPerLabel = 0.0;
    
    while (values.hasNext()) {
      reporter.setStatus("Bayes Theta Normalizer Reducer: " + key);
      weightSumPerLabel += values.next().get();
    }
    reporter.setStatus("Bayes Theta Normalizer Reducer: " + key + " => " + weightSumPerLabel);
    output.collect(key, new DoubleWritable(weightSumPerLabel));
    
  }

}

