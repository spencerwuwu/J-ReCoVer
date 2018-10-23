// https://searchcode.com/api/result/12064165/

/*
  $Id: TernaryTreeDictionary.java 1509 2010-08-24 18:22:50Z marvin.addison $

  Copyright (C) 2003-2008 Virginia Tech.
  All rights reserved.

  SEE LICENSE FOR MORE INFORMATION

  Author:  Middleware Services
  Email:   middleware@vt.edu
  Version: $Revision: 1509 $
  Updated: $Date: 2010-08-24 20:22:50 +0200 (Tue, 24 Aug 2010) $
*/
package edu.vt.middleware.dictionary;

import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * <code>TernaryTreeDictionary</code> provides fast searching for dictionary
 * words using a ternary tree. The entire dictionary is stored in memory, so
 * heap size may need to be adjusted to accommodate large dictionaries. It is
 * highly recommended that sorted word lists be inserted using their median.
 * This helps to produce a balanced ternary tree which improves search time.
 * This class inherits the lower case property of the supplied word list.
 *
 * @author  Middleware Services
 * @version  $Revision: 1509 $ $Date: 2010-08-24 20:22:50 +0200 (Tue, 24 Aug 2010) $
 */

public class TernaryTreeDictionary implements Dictionary
{

  /** Ternary tree used for searching. */
  protected TernaryTree tree;


  /**
   * Creates a new balanced tree dictionary from the given {@link WordList}.
   * This constructor creates a balanced tree by inserting from the median
   * of the word list, which may require additional work depending on the
   * {@link WordList} implementation.
   *
   * @param  wordList  List of words used to back the dictionary.  This list is
   * used exclusively to initialize the internal {@link TernaryTree} used by
   * the dictionary, and may be safely discarded after dictionary creation.
   * <p>
   * <strong>NOTE</strong>
   * <p>
   * While using an unsorted word list produces correct results, it may
   * dramatically reduce search efficiency.  Using a sorted word list is
   * recommended.
   */
  public TernaryTreeDictionary(final WordList wordList)
  {
    this(wordList, true);
  }


  /**
   * Creates a new dictionary instance from the given {@link WordList}.
   *
   * @param  wordList  List of words used to back the dictionary.  This list is
   * used exclusively to initialize the internal {@link TernaryTree} used by
   * the dictionary, and may be safely discarded after dictionary creation.
   * <p>
   * <strong>NOTE</strong>
   * <p>
   * While using an unsorted word list produces correct results, it may
   * dramatically reduce search efficiency.  Using a sorted word list is
   * recommended.
   * @param  useMedian  Set to true to force creation of a balanced tree by
   * inserting into the tree from the median of the {@link WordList} outward.
   * Depending on the word list implementation, this may require additional work
   * to access the median element on each insert.
   */
  public TernaryTreeDictionary(final WordList wordList, final boolean useMedian)
  {
    // Respect case sensitivity of word list in ternary tree
    tree = new TernaryTree(wordList.getComparator().compare("A", "a") != 0);
    final Iterator<String> iterator;
    if (useMedian) {
      iterator = new MedianIterator(wordList);
    } else {
      iterator = new SequentialIterator(wordList);
    }
    while (iterator.hasNext()) {
      this.tree.insert(iterator.next());
    }
  }


  /**
   * Creates a dictionary that uses the given ternary tree for dictionary
   * searches.
   *
   * @param  tt  Ternary tree used to back dictionary.
   */
  public TernaryTreeDictionary(final TernaryTree tt)
  {
    this.tree = tt;
  }


  /** {@inheritDoc} */
  public boolean search(final String word)
  {
    return this.tree.search(word);
  }


  /**
   * This will return an array of strings which partially match the supplied
   * word. This search is case sensitive by default.
   * See {@link TernaryTree#partialSearch}.
   *
   * @param  word  <code>String</code> to search for
   *
   * @return  <code>String[]</code> - of matching words
   */
  public String[] partialSearch(final String word)
  {
    return this.tree.partialSearch(word);
  }


  /**
   * This will return an array of strings which are near to the supplied word by
   * the supplied distance. This search is case sensitive by default.
   * See {@link TernaryTree#nearSearch}.
   *
   * @param  word  <code>String</code> to search for
   * @param  distance  <code>int</code> for valid match
   *
   * @return  <code>String[]</code> - of matching words
   */
  public String[] nearSearch(final String word, final int distance)
  {
    return this.tree.nearSearch(word, distance);
  }


  /**
   * Returns the underlying ternary tree used by this dictionary.
   *
   * @return  <code>TernaryTree</code>
   */
  public TernaryTree getTernaryTree()
  {
    return this.tree;
  }


  /**
   * This provides command line access to a <code>TernaryTreeDictionary</code>.
   *
   * @param  args  <code>String[]</code>
   *
   * @throws  Exception  if an error occurs
   */
  public static void main(final String[] args)
    throws Exception
  {
    final List<RandomAccessFile> files = new ArrayList<RandomAccessFile>();
    try {
      if (args.length == 0) {
        throw new ArrayIndexOutOfBoundsException();
      }

      // dictionary operations
      boolean useMedian = false;
      boolean ignoreCase = false;
      boolean search = false;
      boolean partialSearch = false;
      boolean nearSearch = false;
      boolean print = false;

      // operation parameters
      String word = null;
      int distance = 0;

      for (int i = 0; i < args.length; i++) {
        if ("-m".equals(args[i])) {
          useMedian = true;
        } else if ("-ci".equals(args[i])) {
          ignoreCase = true;
        } else if ("-s".equals(args[i])) {
          search = true;
          word = args[++i];
        } else if ("-ps".equals(args[i])) {
          partialSearch = true;
          word = args[++i];
        } else if ("-ns".equals(args[i])) {
          nearSearch = true;
          word = args[++i];
          distance = Integer.parseInt(args[++i]);
        } else if ("-p".equals(args[i])) {
          print = true;
        } else if ("-h".equals(args[i])) {
          throw new ArrayIndexOutOfBoundsException();
        } else {
          files.add(new RandomAccessFile(args[i], "r"));
        }
      }

      // insert data
      final TernaryTreeDictionary dict = new TernaryTreeDictionary(
          new FilePointerWordList(
              files.toArray(new RandomAccessFile[files.size()]), ignoreCase),
          useMedian);

      // perform operation
      if (search) {
        if (dict.search(word)) {
          System.out.println(
            String.format("%s was found in this dictionary", word));
        } else {
          System.out.println(
            String.format("%s was not found in this dictionary", word));
        }
      } else if (partialSearch) {
        final String[] matches = dict.partialSearch(word);
        System.out.println(
          String.format(
            "Found %s matches for %s in this dictionary : %s",
            matches.length, word, Arrays.asList(matches)));
      } else if (nearSearch) {
        final String[] matches = dict.nearSearch(word, distance);
        System.out.println(
          String.format(
            "Found %s matches for %s in this dictionary at a distance of %s " +
            ": %s", matches.length, word, distance, Arrays.asList(matches)));
      } else if (print) {
        dict.getTernaryTree().print(new PrintWriter(System.out, true));
      } else {
        throw new ArrayIndexOutOfBoundsException();
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("Usage: java " +
        TernaryTreeDictionary.class.getName() + " \\");
      System.out.println(
        "       <dictionary1> <dictionary2> ... " +
        "<options> <operation> \\");
      System.out.println("");
      System.out.println("where <options> includes:");
      System.out.println("       -m (Insert dictionary using it's median) \\");
      System.out.println("       -ci (Make search case-insensitive) \\");
      System.out.println("");
      System.out.println("where <operation> includes:");
      System.out.println("       -s <word> (Search for a word) \\");
      System.out.println("       -ps <word> (Partial search for a word) \\");
      System.out.println("           (where word like '.a.a.a') \\");
      System.out.println(
        "       -ns <word> <distance> " +
        "(Near search for a word) \\");
      System.out.println(
        "       -p (Print the entire dictionary " + "in tree form) \\");
      System.out.println("       -h (Print this message) \\");
      System.exit(1);
    }
  }


  /**
   * Abstract base class for all internal word list iterators.
   *
   * @author Middleware
   * @version $Revision: 1509 $
   *
   */
  abstract static class AbstractWordListIterator implements Iterator<String>
  {
    /** Word list */
    protected WordList wordList;

    /** Index of next word in list. */
    protected int index;


    /** {@inheritDoc} */
    public void remove()
    {
      throw new UnsupportedOperationException("Remove not supported.");
    }
  }


  /**
   * Iterator implementation that iterates over a {@link WordList} by
   * incrementing an index from 0 to {@link WordList#size()} - 1.
   *
   * @author Middleware
   * @version $Revision: 1509 $
   *
   */
  static class SequentialIterator extends AbstractWordListIterator
  {
    /**
     * Creates a new sequential iterator over the given word list.
     *
     * @param  wl  Word list to iterate over.
     */
    public SequentialIterator(final WordList wl)
    {
      this.wordList = wl;
    }


    /** {@inheritDoc} */
    public boolean hasNext()
    {
      return index < this.wordList.size();
    }


    /** {@inheritDoc} */
    public String next()
    {
      return this.wordList.get(index++);
    }
  }


  /**
   * Iterator that iterates over a word list from the median outward to either
   * end.  In particular, for a word list of N elements whose median index is
   * M, and for each i such that M-i >= 0 and M+i < N, the M-i element is
   * visited  before the M+i element.
   *
   * @author Middleware
   * @version $Revision: 1509 $
   *
   */
  static class MedianIterator extends AbstractWordListIterator
  {
    /** Index of median element in given list */
    private final int median;

    /** Indicates direction of next item */
    private int sign;


    /**
     * Creates a new median iterator over the given word list.
     *
     * @param  wl  Word list to iterate over.
     */
    public MedianIterator(final WordList wl)
    {
      this.wordList = wl;
      this.median = wl.size() / 2;
    }


    /** {@inheritDoc} */
    public boolean hasNext()
    {
      final int n = this.wordList.size();
      final boolean result;
      if (sign > 0) {
        result = median + index < n;
      } else if (sign < 0) {
        result = median - index >= 0;
      } else {
        result = n > 0;
      }
      return result;
    }


    /** {@inheritDoc} */
    public String next()
    {
      final String next;
      if (sign > 0) {
        next = this.wordList.get(median + index);
        sign = -1;
        index++;
      } else if (sign < 0) {
        next = this.wordList.get(median - index);
        sign = 1;
      } else {
        next = this.wordList.get(median);
        sign = -1;
        index = 1;
      }
      return next;
    }
  }
}

