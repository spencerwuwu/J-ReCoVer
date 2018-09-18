package reduce_test;

import java.io.IOException;
import java.util.Iterator;

public interface ReducerC<T1,T2> {
  public void reduce(T1 key, Iterable<T2> values,
      Context context) throws IOException,InterruptedException;
}
