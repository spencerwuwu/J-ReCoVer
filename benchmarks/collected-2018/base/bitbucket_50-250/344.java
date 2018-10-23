// https://searchcode.com/api/result/126660475/

package indexing.reduce;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import indexing.common.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static indexing.common.StopWatch.start;

/**
 * Entry point for the reducer.
 */
public class ReducerMain {
  /**
   * Reads "key value" lines from input assuming that they are sorted by key,
   * builds a list of all values for a key and runs Reducer for them.
   */
  public void run(BufferedReader input, ReducerOutput output) throws IOException {
    Reducer reducer = new Reducer(output);

    KeyValues keyValues = new KeyValues();
    String line;
    while ((line = input.readLine()) != null) {
      List<String> words = Lists.newArrayList(Splitter
          .on(CharMatcher.WHITESPACE)
          .omitEmptyStrings()
          .split(line));

      // all the value for a key have been read, reduce them
      if (keyValues.isNewKey(words.get(0))) {
        reducer.reduce(keyValues.key, keyValues.values);
        keyValues = new KeyValues();
      }

      keyValues.add(words.get(0), words.get(1));
    }

    reducer.reduce(keyValues.key, keyValues.values);
    output.close();
  }

  public static void main(String [] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Usage : ReducerMain <output file>");
    }

    StopWatch stopWatch = start("Index build");

    new ReducerMain().run(new BufferedReader(new InputStreamReader(System.in)),
        ReducerOutput.indexedFileOutput(args[0]));

    stopWatch.stop();
  }

  /**
   * Utility class that helps collect all the values for a key.
   */
  private static class KeyValues {
    private List<String> values = new ArrayList<String>();
    private String key = "";

    public void add(String key, String value) {
      this.key = key;
      this.values.add(value);
    }

    public boolean isNewKey(String key) {
      return !this.key.isEmpty() && !this.key.equals(key);
    }
  }  
}

