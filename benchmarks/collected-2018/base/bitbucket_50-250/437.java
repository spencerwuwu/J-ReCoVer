// https://searchcode.com/api/result/133128376/

package net.sf.jtmt.clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simulated Annealing description from Text Mining Application Programming, ch 8.
 * 1) Get initial set of clusters and set initial temperature parameter T.
 * 2) Repeat until temperature is reduced to the minimum.
 *    2.1) Run loop x times.
 *         2.1.1) Find a new set of clusters by altering the membership of some
 *                documents.
 *         2.1.2) Compare the difference between the values of the new and old
 *                set of clusters. If there is an improvement, accept the new 
 *                set of clusters, otherwise accept the new set of clusters with
 *                probability p.
 *    2.2) Reduce the temperature based on the cooling schedule.
 * 3) Return the final set of clusters.
 * 
 * @author Sujit Pal
 * @version $Revision$
 */
public class SimulatedAnnealingClusterer {

  private final Log log = LogFactory.getLog(getClass());

  private boolean randomizeDocs;
  private double initialTemperature;
  private double finalTemperature;
  private double downhillProbabilityCutoff;
  private int numberOfLoops;
  
  public void setRandomizeDocs(boolean randomizeDocs) {
    this.randomizeDocs = randomizeDocs;
  }
  
  public void setInitialTemperature(double initialTemperature) {
    this.initialTemperature = initialTemperature;
  }
  
  public void setFinalTemperature(double finalTemperature) {
    this.finalTemperature = finalTemperature;
  }
  
  public void setDownhillProbabilityCutoff(double downhillProbabilityCutoff) {
    this.downhillProbabilityCutoff = downhillProbabilityCutoff;
  }
  
  public void setNumberOfLoops(int numberOfLoops) {
    this.numberOfLoops = numberOfLoops;
  }
  
  public List<Cluster> cluster(DocumentCollection collection) {
    // 1) Get initial set of clusters... 
    int numDocs = collection.size();
    int numClusters = (int) Math.floor(Math.sqrt(numDocs));
    List<Cluster> clusters = new ArrayList<Cluster>();
    for (int i = 0; i < numClusters; i++) {
      clusters.add(new Cluster("C" + i));
    }
    // ...and set initial temperature parameter T.
    double temperature = initialTemperature;
    // Randomly assign documents to the k clusters.
    if (randomizeDocs) {
      collection.shuffle();
    }
    for (int i = 0; i < numDocs; i++) {
      int targetCluster = i % numClusters;
      clusters.get(targetCluster).addDocument(collection.getDocumentNameAt(i),
        collection.getDocument(collection.getDocumentNameAt(i)));
    }
    log.debug("..Initial clusters: " + clusters.toString());
    // 2) Repeat until temperature is reduced to the minimum.
    while (temperature > finalTemperature) {
      double previousAverageRadius = 0.0D;
      List<Cluster> prevClusters = new ArrayList<Cluster>();
      // 2.1) Run loop NUM_LOOP times.
      for (int loop = 0; loop < numberOfLoops; loop++) {
        // 2.1.1) Find a new set of clusters by altering the membership of some
        //        documents.
        // pick two clusters at random
        List<Integer> randomClusterIds = getRandomClusterIds(clusters);
        // pick two documents out of the clusters at random
        List<String> randomDocumentNames = 
          getRandomDocumentNames(collection, randomClusterIds, clusters);
        // exchange the two random documents among the random clusters.
        clusters.get(randomClusterIds.get(0)).removeDocument(randomDocumentNames.get(0));
        clusters.get(randomClusterIds.get(0)).addDocument(
          randomDocumentNames.get(1), collection.getDocument(randomDocumentNames.get(1)));
        clusters.get(randomClusterIds.get(1)).removeDocument(randomDocumentNames.get(1));
        clusters.get(randomClusterIds.get(1)).addDocument(
          randomDocumentNames.get(0), collection.getDocument(randomDocumentNames.get(0)));
        // 2.1.2) Compare the difference between the values of the new and old
        //        set of clusters. If there is an improvement, accept the new 
        //        set of clusters, otherwise accept the new set of clusters with
        //        probability p.
        log.debug("..Intermediate clusters: " + clusters.toString());
        double averageRadius = getAverageRadius(clusters);
        if (averageRadius > previousAverageRadius) {
          // possible downhill move, calculate the probability of it being 
          // accepted
          double probability = 
            Math.exp((previousAverageRadius - averageRadius)/temperature);
          if (probability < downhillProbabilityCutoff) {
            // go back to the cluster before the changes
            clusters.clear();
            clusters.addAll(prevClusters);
            continue;
          }
        }
        prevClusters.clear();
        prevClusters.addAll(clusters);
        previousAverageRadius = averageRadius;
      }
      // 2.2) Reduce the temperature based on the cooling schedule.
      temperature = temperature / 10;
    }
    // 3) Return the final set of clusters.
    return clusters;
  }

  private List<Integer> getRandomClusterIds(List<Cluster> clusters) {
    IdGenerator clusterIdGenerator = new IdGenerator(clusters.size());
    List<Integer> randomClusterIds = new ArrayList<Integer>();
    for (int i = 0; i < 2; i++) {
      randomClusterIds.add(clusterIdGenerator.getNextId());
    }
    return randomClusterIds;
  }

  private List<String> getRandomDocumentNames(DocumentCollection collection, 
      List<Integer> randomClusterIds, List<Cluster> clusters) {
    List<String> randomDocumentNames = new ArrayList<String>();
    for (Integer randomClusterId : randomClusterIds) {
      Cluster randomCluster = clusters.get(randomClusterId);
      IdGenerator documentIdGenerator = new IdGenerator(randomCluster.size());
      randomDocumentNames.add(
        randomCluster.getDocumentName(documentIdGenerator.getNextId()));
    }
    return randomDocumentNames;
  }

  private double getAverageRadius(List<Cluster> clusters) {
    double score = 0.0D;
    for (Cluster cluster : clusters) {
      score += cluster.getRadius();
    }
    return (score / clusters.size());
  }
}

