// https://searchcode.com/api/result/64979797/

package thompson.core;

import java.util.*;
import java.math.*;

// Implementation of and extensions to various algorithms described in
// "Counting elements and geodesics in Thompson's group F" by Elder, Fuxy, and
// Rechintzer.
public class Sample {
  final static BigInteger BI_TWO = new BigInteger("2");
  final static BigInteger BI_FOUR = new BigInteger("4");

  private static ForestState[] updateLeft(ForestState state) {    
    ForestState[] nextStates;
    if (state.forestLabel == ForestLabel.L) {
      nextStates = new ForestState[7];
      nextStates[0] = new ForestState(ForestLabel.L, OfPointer.LEFT,  0);
      nextStates[1] = new ForestState(ForestLabel.N, OfPointer.LEFT,  1);
      nextStates[2] = new ForestState(ForestLabel.I, OfPointer.LEFT,  0);
      nextStates[3] = new ForestState(ForestLabel.N, OfPointer.RIGHT, 1);
      nextStates[4] = new ForestState(ForestLabel.I, OfPointer.RIGHT, 0);
      nextStates[5] = new ForestState(ForestLabel.R, OfPointer.RIGHT, 0);
      nextStates[6] = new ForestState(ForestLabel.X, OfPointer.RIGHT, 0);
    } else if ((state.forestLabel == ForestLabel.N) ||
               ((state.forestLabel == ForestLabel.I) && (state.excess > 0))) {
      nextStates = new ForestState[4];
      nextStates[0] = new ForestState(ForestLabel.N, OfPointer.LEFT, state.excess + 1);
      nextStates[1] = new ForestState(ForestLabel.N, OfPointer.LEFT, state.excess);
      nextStates[2] = new ForestState(ForestLabel.I, OfPointer.LEFT, state.excess);
      nextStates[3] = new ForestState(ForestLabel.I, OfPointer.LEFT, state.excess - 1);
    } else if ((state.forestLabel == ForestLabel.I) && (state.excess == 0)) {
      nextStates = new ForestState[3];
      nextStates[0] = new ForestState(ForestLabel.N, OfPointer.LEFT, 1);
      nextStates[1] = new ForestState(ForestLabel.I, OfPointer.LEFT, 0);
      nextStates[2] = new ForestState(ForestLabel.L, OfPointer.LEFT, 0);
    } else {
      nextStates = new ForestState[0];
    }
    return nextStates;
  }
  
  private static ForestState[] updateRight(ForestState state) {    
    ForestState[] nextStates;
    if (state.forestLabel == ForestLabel.R) {
      nextStates = new ForestState[2];
      nextStates[0] = new ForestState(ForestLabel.R, OfPointer.RIGHT, 0);
      nextStates[1] = new ForestState(ForestLabel.X, OfPointer.RIGHT, 0);
    } else if (state.forestLabel == ForestLabel.X) {
      nextStates = new ForestState[2];
      nextStates[0] = new ForestState(ForestLabel.N, OfPointer.RIGHT, 1);
      nextStates[1] = new ForestState(ForestLabel.I, OfPointer.RIGHT, 0);
    } else if ((state.forestLabel == ForestLabel.N) ||
              ((state.forestLabel == ForestLabel.I) && (state.excess > 0))) {
      nextStates = new ForestState[4];
      nextStates[0] = new ForestState(ForestLabel.N, OfPointer.RIGHT, state.excess + 1);
      nextStates[1] = new ForestState(ForestLabel.N, OfPointer.RIGHT, state.excess);
      nextStates[2] = new ForestState(ForestLabel.I, OfPointer.RIGHT, state.excess);
      nextStates[3] = new ForestState(ForestLabel.I, OfPointer.RIGHT, state.excess - 1);
    } else if ((state.forestLabel == ForestLabel.I) && (state.excess == 0)) {
      nextStates = new ForestState[4];
      nextStates[0] = new ForestState(ForestLabel.N, OfPointer.RIGHT, 1);
      nextStates[1] = new ForestState(ForestLabel.I, OfPointer.RIGHT, 0);
      nextStates[2] = new ForestState(ForestLabel.R, OfPointer.RIGHT, 0);
      nextStates[3] = new ForestState(ForestLabel.X, OfPointer.RIGHT, 0);
    } else {
      nextStates = new ForestState[0];
    }
    return nextStates;
  }
  
  public static int weight(ForestLabel labelA, ForestLabel labelB) {
    switch(labelA) {
      case I:
        switch(labelB) {
          case I: return 2;
          case N: return 4;
          case L: return 2;
          case R: return 1;
          case X: return 3;
        }
      case N:
        switch(labelB) {
          case I: return 4;
          case N: return 4;
          case L: return 2;
          case R: return 3;
          case X: return 3;
        }
      case L:
        switch(labelB) {
          case I: return 2;
          case N: return 2;
          case L: return 2;
          case R: return 1;
          case X: return 1;
        }
      case R:
        switch(labelB) {
          case I: return 1;
          case N: return 3;
          case L: return 1;
          case R: return 2;
          case X: return 2;
        }
      case X:
        switch(labelB) {
          case I: return 3;
          case N: return 3;
          case L: return 1;
          case R: return 2;
          case X: return 2;
        }
      default:
        throw new IllegalArgumentException();
    }
  }
    
  private static ArrayList<ForestKey> weightNKeys(HashMap<ForestKey,?> web, int n) {
    ArrayList<ForestKey> keys = new ArrayList<ForestKey>();
    for (ForestKey key : web.keySet()) {
      if (key.weight == n) {
        keys.add(key);
      }
    }
    return keys;
  }
  
  public static ArrayList<ForestKey> successorKeys(ForestKey fromKey) {
    ArrayList<ForestKey> toKeys = new ArrayList<ForestKey>();
    ForestState upperState = fromKey.upperState;
    ForestState lowerState = fromKey.lowerState;
    ForestState[] upperSet = (upperState.ofPointer == OfPointer.LEFT)  ? updateLeft(upperState) : updateRight(upperState);
    ForestState[] lowerSet = (lowerState.ofPointer == OfPointer.LEFT) ? updateLeft(lowerState) : updateRight(lowerState);
    for (int u = 0; u < upperSet.length; u++) {
      ForestState upperStateP = upperSet[u];
      for (int l = 0; l < lowerSet.length; l++) {
        ForestState lowerStateP = lowerSet[l];
        if (!((upperStateP.forestLabel == lowerStateP.forestLabel) &&
              (lowerStateP.forestLabel == ForestLabel.I) &&
              (upperState.forestLabel != ForestLabel.I) &&
              (lowerState.forestLabel != ForestLabel.I))) {
          int weightP = weight(upperStateP.forestLabel, lowerStateP.forestLabel);
          ForestKey toKey = new ForestKey(fromKey.weight + weightP, upperStateP, lowerStateP); 
          toKeys.add(toKey);
        }
      }
    }
    return toKeys;
  }

  // Returns an array indicating the number of unique forest diagrams for
  // weight 4, 5, 6, ..., maxWeight-2, maxWeight-1, and maxWeight.
  public static BigInteger[] countForestDiagrams(int maxWeight) {
    BigInteger[] counts = new BigInteger[maxWeight-3];
    HashMap<ForestKey,BigInteger> countWeb = new HashMap<ForestKey,BigInteger>();
    countWeb.put(
      new ForestKey(2, new ForestState(ForestLabel.L, OfPointer.LEFT, 0),
                       new ForestState(ForestLabel.L, OfPointer.LEFT, 0)),
      BigInteger.ONE);
    for (int n = 2; n < maxWeight; n++) {
      for (ForestKey fromKey : weightNKeys(countWeb, n)) {
        BigInteger fromCount = countWeb.get(fromKey);
        for (ForestKey toKey : successorKeys(fromKey)) {
          BigInteger toCount = countWeb.get(toKey);
          if (toCount == null) { toCount = BigInteger.ZERO; }
          BigInteger newCount = toCount.add(fromCount);
          countWeb.put(toKey, newCount);
        }
        countWeb.remove(fromKey);
      }
      if (n >= 3) {
        counts[n-3] = countWeb.get(
                        new ForestKey(n+1,
                          new ForestState(ForestLabel.R, OfPointer.RIGHT, 0),
                          new ForestState(ForestLabel.R, OfPointer.RIGHT, 0)));
      }
    }
    return counts;
  }
  
  // Returns an array indicating the number of unique elements of word length
  // 0, 1, 2, ..., maxLenth-2, maxLength-1, and maxLength.
  public static BigInteger[] countTreePairs(int maxLength) {
    BigInteger[] fdCounts = countForestDiagrams(maxLength+4);
    BigInteger[] tpCounts = new BigInteger[maxLength+1];
    for (int l = 0; l <= maxLength; l++) {
      if (l <= 1) {
        tpCounts[l] = fdCounts[l];
      } else if (l <= 3) {
        tpCounts[l] = fdCounts[l].subtract(BI_TWO.multiply(fdCounts[l-2]));
      } else {
        tpCounts[l] = fdCounts[l].subtract(BI_TWO.multiply(fdCounts[l-2])).add(fdCounts[l-4]);
      }
    }
    return tpCounts;
  }
  
  private static void addBackPointer(BackPointers backPointers, ForestKey backKey, BigInteger backCount) {
    backPointers.backPointers.add(new BackPointer(backKey, backCount));
    backPointers.totalBackCount = backPointers.totalBackCount.add(backCount);
  }
  
  // Returns a web modeling the elements of F with 'weight' (in the EFR sense)
  // of at most maxWeight. Such a model can be used to generate random elements
  // with word length of at most maxWeight-4 with choooseRandomTreePair.
  public static HashMap<ForestKey,BackPointers> modelForestDiagrams(int maxWeight) {
    HashMap<ForestKey,BackPointers> modelWeb = new HashMap<ForestKey,BackPointers>();
    modelWeb.put(
      new ForestKey(2, new ForestState(ForestLabel.L, OfPointer.LEFT, 0),
                       new ForestState(ForestLabel.L, OfPointer.LEFT, 0)),
      new BackPointers(BigInteger.ONE));
    for (int n = 2; n < maxWeight; n++) {
      for (ForestKey fromKey : weightNKeys(modelWeb, n)) {
        BackPointers fromPointers = modelWeb.get(fromKey);
        BigInteger fromCount = fromPointers.totalBackCount;
        for (ForestKey toKey : successorKeys(fromKey)) {
          BackPointers toPointers = modelWeb.get(toKey);
          if (toPointers == null) { toPointers = new BackPointers(BigInteger.ZERO); }
          addBackPointer(toPointers, fromKey, fromCount);
          modelWeb.put(toKey, toPointers);
        }
      }
    }
    return modelWeb;
  }
  
  private static ForestKey chooseBackKey(BackPointers backPointers, Random rand) {
    BackPointer chosen = null;
    BigInteger finger = Util.nextRandomBigInteger(backPointers.totalBackCount, rand);
    BigInteger at = BigInteger.ZERO;
    for (BackPointer backPointer : backPointers.backPointers) {
      at = at.add(backPointer.backCount);
      if (at.compareTo(finger) > 0) {
        chosen = backPointer;
        break;
      }
    }
    if (chosen == null) { throw new RuntimeException("unreachable"); }
    return chosen.backKey;
  }
  
  private static LinkedList<ForestKey> choosePath(HashMap<ForestKey,BackPointers> modelWeb, int weight) {
    ForestKey atKey = new ForestKey(weight, new ForestState(ForestLabel.R, OfPointer.RIGHT, 0),
                                            new ForestState(ForestLabel.R, OfPointer.RIGHT, 0));
    if (!modelWeb.containsKey(atKey)) {
      throw new IllegalArgumentException("Insufficiently deep model");
    }
    ForestKey rootKey = new ForestKey(2, new ForestState(ForestLabel.L, OfPointer.LEFT, 0),
                                         new ForestState(ForestLabel.L, OfPointer.LEFT, 0));
    Random rand = new Random();
    LinkedList<ForestKey> wordKeys = new LinkedList<ForestKey>();
    while (!atKey.equals(rootKey)) {
      wordKeys.addFirst(atKey);
      atKey = chooseBackKey(modelWeb.get(atKey), rand);
    }
    wordKeys.addFirst(rootKey);
    return wordKeys;  
  }

  // add a token to the chain that will eventually be passed to reifySubtree
  private static TreeToken appendForestState(TreeToken tailToken, ForestState forestState) {
    TreeToken newTail = new TreeToken(forestState.forestLabel, forestState.excess);
    int excessDiff = newTail.excess - tailToken.excess;
    if ((newTail.bigLabel == ForestLabel.N) && (excessDiff == 1)) {
      tailToken.nextLittleLabelIsN = true;
    } else if ((newTail.bigLabel == ForestLabel.N) && (excessDiff == 0)) {
      tailToken.nextLittleLabelIsN = false;
    } else if ((newTail.bigLabel == ForestLabel.I) && (excessDiff == -1)) {
      tailToken.nextLittleLabelIsN = false;
    } else if ((newTail.bigLabel == ForestLabel.I) && (excessDiff == 0)) {
      tailToken.nextLittleLabelIsN = true;
    } else {
      throw new RuntimeException(newTail.bigLabel + " " + excessDiff);
    }
    tailToken.nextToken = newTail;
    return newTail;
  }
 
  // destructively convert a chain of TreeTokens into the corresponding tree
  private static Node reifySubtree(TreeToken headToken) {
    while (!(headToken.nextToken == null)) {
      TreeToken atToken = headToken;
      TreeToken pairToken = null;
      while (pairToken == null) {
        if ((atToken.bigLabel == ForestLabel.N) &&
            (atToken.nextToken.bigLabel == ForestLabel.I)) {
          pairToken = atToken;
        } else {
          atToken = atToken.nextToken;
        }
      }
      Node parent = new Node();
      parent.setLeft(pairToken.node);
      parent.setRight(pairToken.nextToken.node);
      pairToken.node = parent;
      pairToken.bigLabel = (pairToken.nextLittleLabelIsN) ? ForestLabel.N :
                                                            ForestLabel.I;
      pairToken.nextLittleLabelIsN = pairToken.nextToken.nextLittleLabelIsN;
      pairToken.nextToken = pairToken.nextToken.nextToken;
    }
    return headToken.node;
  }
  
  private static ForestState getForestState(List<ForestKey> forestPath, int i, boolean upper) {
    return (upper ? forestPath.get(i).upperState : forestPath.get(i).lowerState);
  }

  private static ForestLabel getForestLabel(List<ForestKey> forestPath, int i, boolean upper) {
    return getForestState(forestPath, i, upper).forestLabel;
  }
  
  private static OfPointer getOfPointer(List<ForestKey> forestPath, int i, boolean upper) {
    return getForestState(forestPath, i, upper).ofPointer;
  }
  
  private static boolean isSubtreeLabel(ForestLabel label) {
    return ((label == ForestLabel.I) || (label == ForestLabel.N));
  }

  // Returns a single tree corresponding to either the upper or lower forest
  // encoded by the given path.
  private static Node reifyTree(List<ForestKey> forestPath, boolean upper) {
    // determine the number of keys marked as left of the pointer
    int numLeft = 0;
    for (int i = 0; i < forestPath.size(); i++) {
      if (getOfPointer(forestPath, i, upper) == OfPointer.LEFT) {
        numLeft++;
      }
    }
    // build the left side of the tree
    int at = 0;
    Node leftTree = new Node();
    while (at < numLeft) {
      // handle trivial, single-node subtrees
      if (!isSubtreeLabel(getForestLabel(forestPath, at, upper))) {
        Node parent = new Node();
        Node trivialSubtree = new Node();
        parent.setLeft(leftTree);
        parent.setRight(trivialSubtree);
        leftTree = parent;
        at++;
      // handle non-trival subtrees that need to be reified from NIni seqs
      } else {
        TreeToken headToken = new TreeToken();
        TreeToken atToken = headToken;
        ForestState atState = getForestState(forestPath, at, upper);
        while ((atState != null) && isSubtreeLabel(atState.forestLabel)) {
          atToken = appendForestState(atToken, atState);
          at++;
          atState = (at < numLeft) ?  getForestState(forestPath, at, upper) : null;
        }
        Node parent = new Node();
        Node subtree = reifySubtree(headToken);
        parent.setLeft(leftTree);
        parent.setRight(subtree);
        leftTree = parent;
        // if the next label after the subtree is an L, eat it
        if ((atState != null) && atState.forestLabel == ForestLabel.L) {
          at++;
        }
      }
    }
    // build the right side of the tree
    Node rightTree = new Node();
    Node rightTip = rightTree;
    while (at < forestPath.size()) {
      // handle trivial, single-node subtrees
      if (!isSubtreeLabel(getForestLabel(forestPath, at, upper))) {
        Node trivialSubtree = new Node();
        Node extension = new Node();
        rightTip.setLeft(trivialSubtree);
        rightTip.setRight(extension);
        rightTip = extension;
        at++;
      // handle non-trival subtrees that need to be reified from NIni seqs
      } else {
        TreeToken headToken = new TreeToken();
        TreeToken atToken = headToken;
        ForestState atState = getForestState(forestPath, at, upper);
        while ((atState != null) && isSubtreeLabel(atState.forestLabel)) {
          atToken = appendForestState(atToken, atState);
          at++;
          atState = (at < forestPath.size()) ?  getForestState(forestPath, at, upper) : null;
        }
        Node subtree = reifySubtree(headToken);
        Node extension = new Node();
        rightTip.setLeft(subtree);
        rightTip.setRight(extension);
        rightTip = extension;
        // if the next label after the subtree is an X or R, eat it
        if ((atState != null) &&
            ((atState.forestLabel == ForestLabel.X) ||
             (atState.forestLabel == ForestLabel.R))) {
          at++;
        }
      }
    }
    // we may need one extra node for a trailing L or R
    // such a node always goes on the right side of the tree
    ForestLabel lastLabel = getForestLabel(forestPath, forestPath.size()-1, upper);
    if ((lastLabel == ForestLabel.L) || (lastLabel == ForestLabel.R)) {
      Node trivialSubtree = new Node();
      Node extension = new Node();
      rightTip.setLeft(trivialSubtree);
      rightTip.setRight(extension);
    }
    // join the two halfs of the tree
    Node tree = new Node();
    tree.setLeft(leftTree);
    tree.setRight(rightTree);
    return tree;
  }

  public static void printForestPath(List<ForestKey> forestPath) {
    for (int i = 0; i < forestPath.size(); i++) {
      ForestKey forestKey = forestPath.get(i);
      System.out.printf("%s %s %s %s %s %s\n",
        forestKey.upperState.forestLabel,
        forestKey.upperState.ofPointer,
        forestKey.upperState.excess,
        forestKey.lowerState.forestLabel,
        forestKey.lowerState.ofPointer,
        forestKey.lowerState.excess);
    }
  }

  // Returns a TreePair corresponding to the element represented by the given
  // forest encoding.
  public static TreePair reifyTreePair(List<ForestKey> forestPath) {
    TreePair treePair = new TreePair(reifyTree(forestPath, false),
                                     reifyTree(forestPath, true));
    treePair.reduce();
    return treePair;
  }

  // Uses the given modelWeb, as generated by modelForestDiagrams, to produce
  // an element selected uniformly at random from all elements in F of the given
  // word length. Note that this requires a model built for weight at least
  // length+4.
  public static TreePair chooseTreePair(HashMap<ForestKey,BackPointers> modelWeb, int length) {
    int attempt = 0;
    while (true) {
      attempt++;
       LinkedList<ForestKey> forestPath = choosePath(modelWeb, length+4);
       forestPath.removeFirst();
       forestPath.removeLast();
       ForestKey leftKey = forestPath.peekFirst();
       ForestKey rightKey = forestPath.peekLast();
       if (!((leftKey.upperState.forestLabel == ForestLabel.L &&
              leftKey.lowerState.forestLabel == ForestLabel.L) ||
             (rightKey.upperState.forestLabel == ForestLabel.R &&
              rightKey.lowerState.forestLabel == ForestLabel.R))) {
         return reifyTreePair(forestPath);
       }
    }
  }
}

