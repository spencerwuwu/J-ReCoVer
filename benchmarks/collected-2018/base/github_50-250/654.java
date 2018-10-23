// https://searchcode.com/api/result/69348018/

/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.mahout.clustering.kmeans;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.matrix.AbstractVector;

public class KMeansCombiner extends MapReduceBase implements
    Reducer<Text, Text, Text, Text> {

  @Override
  public void reduce(Text key, Iterator<Text> values,
      OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
    Cluster cluster = new Cluster(key.toString());
    while (values.hasNext()) {
      String[] numPointnValue = values.next().toString().split("\t");
      cluster.addPoints(Integer.parseInt(numPointnValue[0].trim()),
          AbstractVector.decodeVector(numPointnValue[1].trim()));
    }
    output.collect(key, new Text(cluster.getNumPoints() + "\t"
        + cluster.getPointTotal().asFormatString()));
  }

  @Override
  public void configure(JobConf job) {
    super.configure(job);
    Cluster.configure(job);
  }

}

