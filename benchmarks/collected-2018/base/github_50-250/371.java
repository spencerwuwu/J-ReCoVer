// https://searchcode.com/api/result/76526378/

package Pagerank.PageRankIter;

import Pagerank.PageRankIter.PageRankIterDriver;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.io.Text;

public class PageRankIterReducer extends MapReduceBase implements Reducer {
	  private static void debug(String s) {
		  System.out.println(s);
	  }
  public void reduce(WritableComparable key, Iterator values,
                     OutputCollector output, Reporter reporter) throws IOException {
    double score = 0;
    String outLinks = "";
    Counters counters = PageRankIterDriver.counters;
//    final double n = 6552490;
    final double n = 4;
    final double damping = 0.15;
    debug(key.toString());
    while (values.hasNext()) {
      String curr = ((Text)values.next()).toString();
      System.out.println(key.toString() + ": Parseando " + curr);
      int colon = curr.indexOf(":");
      if ((colon > -1)) {
        try {
          outLinks = curr.substring(colon + 1);
          continue;
        } catch (Exception e) {
          ;
        }
      } else {
        System.out.println(key.toString() + " recebeu contribuicao de " + curr);
        score += Double.parseDouble(curr);
      }
    }
    double d = counters.getCounter(PageRankIterDriver.Link.LinksToNoOne);
    score += (1.e-12*d)/n;
    score = damping/n + (1.-damping)*score;
    String toEmit;
    toEmit = (new Double(score)).toString() + ":" + outLinks;
    String s = (new Long(counters.getCounter(PageRankIterDriver.Link.LinksToNoOne))).toString();
    output.collect(key, new Text(toEmit));
  }
}

