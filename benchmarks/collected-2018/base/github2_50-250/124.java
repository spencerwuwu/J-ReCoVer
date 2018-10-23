// https://searchcode.com/api/result/112051777/

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

package org.apache.mahout.clustering.fuzzykmeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.clustering.ClusterObservations;

public class FuzzyKMeansReducer extends Reducer<Text, ClusterObservations, Text, SoftCluster> {

  private final Map<String, SoftCluster> clusterMap = new HashMap<String, SoftCluster>();

  private FuzzyKMeansClusterer clusterer;

  @Override
  protected void reduce(Text key, Iterable<ClusterObservations> values, Context context) throws IOException, InterruptedException {
    SoftCluster cluster = clusterMap.get(key.toString());
    for (ClusterObservations value : values) {
      if (value.getCombinerState() == 0) { // escaped from combiner
        cluster.observe(value.getS1(), Math.pow(value.getS0(), clusterer.getM()));
      } else {
        cluster.observe(value);
      }
    }
    // force convergence calculation
    boolean converged = clusterer.computeConvergence(cluster);
    if (converged) {
      context.getCounter("Clustering", "Converged Clusters").increment(1);
    }
    context.write(new Text(cluster.getIdentifier()), cluster);
  }

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    Configuration conf = context.getConfiguration();
    clusterer = new FuzzyKMeansClusterer(conf);

    List<SoftCluster> clusters = new ArrayList<SoftCluster>();
    String clusterPath = conf.get(FuzzyKMeansConfigKeys.CLUSTER_PATH_KEY);
    if ((clusterPath != null) && (clusterPath.length() > 0)) {
      FuzzyKMeansUtil.configureWithClusterInfo(new Path(clusterPath), clusters);
      setClusterMap(clusters);
    }

    if (clusterMap.isEmpty()) {
      throw new IllegalStateException("Cluster is empty!!!");
    }
  }

  private void setClusterMap(List<SoftCluster> clusters) {
    clusterMap.clear();
    for (SoftCluster cluster : clusters) {
      clusterMap.put(cluster.getIdentifier(), cluster);
    }
    clusters.clear();
  }

  public void setup(List<SoftCluster> clusters, Configuration conf) {
    setClusterMap(clusters);
    clusterer = new FuzzyKMeansClusterer(conf);
  }

}

