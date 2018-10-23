// https://searchcode.com/api/result/70694386/

package indexing.reduce;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import indexing.common.Pair;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static indexing.common.Pair.pair;

/**
 * A reducer that counts the occurrences of each unique word in values and
 * outputs the counts for the key.
 */
public class Reducer {
  private ReducerOutput output;

  public Reducer(ReducerOutput output) {
    this.output = output;
  }

  /**
   * For each distinct word in values it counts how many times it's present and
   * outputs the key and a list of Pair<String, Integer> representing the count
   * for each word, sorted by count.
   *
   * @param key a word
   * @param values list of words that are on the same line as key
   * @throws IOException
   */
  public void reduce(String key, List<String> values) throws IOException {
    Multiset<String> wordCount = HashMultiset.create();

    for (String value : values) {
      wordCount.add(value);
    }

    output.write(key, toSortedListOfPairs(wordCount));
  }

  /**
   * Transforms the Multiset to a list of Pair<String, Integer> representing
   * the count for each word, sorted by count.
   *
   * @param wordCount Multiset with the word counts.
   * @return List<Pair<String, Integer>> 
   */
  private List<Pair<String, Integer>> toSortedListOfPairs(Multiset<String> wordCount) {
    List<Pair<String, Integer>> words = Lists.newArrayList();

    for (Multiset.Entry<String> entry : wordCount.entrySet()) {      
      words.add(pair(entry.getElement(), entry.getCount()));
    }

    Collections.sort(words, new PairComparator());
    return words;
  }

  private static class PairComparator implements Comparator<Pair<String, Integer>> {
    public int compare(Pair<String, Integer> a, Pair<String, Integer> b) {
      return (b.value.equals(a.value)) ? -b.key.compareTo(a.key) : b.value - a.value;
    }
  }
}

