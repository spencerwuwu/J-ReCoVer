package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.mapred.Reporter;

public interface ReducerO<T1,T2,T3,T4> {
  public void reduce(T1 key, Iterator<T2> values,
      OutputCollector<T3,T4> oc1, Reporter reporter) throws IOException,InterruptedException;
}
