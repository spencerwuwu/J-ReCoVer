// https://searchcode.com/api/result/64979734/

package thompson.core;

import java.util.*;

// Represents an element of F as a pair of trees, in particular as described
// in "Combinatorial Properties of Thompson's Group F" by Cleary and Taback.
public class TreePair {
  public Node minusRoot, plusRoot;

  public TreePair(Node minusRoot, Node plusRoot) {
    if ((minusRoot == null) || (plusRoot == null)) {
      throw new IllegalArgumentException();
    }
    this.minusRoot = minusRoot;
    this.plusRoot = plusRoot;
  }

  public TreePair copy() {
    return new TreePair(this.minusRoot.copy(), this.plusRoot.copy());
  }
  
  public String toString() {
    return "[" + minusRoot.toString() + "|" + plusRoot.toString() + "]"; 
  }
  
  public int hashCode() {
    return this.toString().hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof TreePair)) {
      return false;
    } else {
      TreePair treePair = (TreePair) obj;
      return (this.minusRoot.equals(treePair.minusRoot) &&
              this.plusRoot.equals(treePair.plusRoot));
    }
  }

  // Returns an element in generator-exponent form corresponding directly to
  // the recieving tree-pair instance. The classic leaf-exponent calculation is
  // used. Note that the returned GenExp is not necessarily in 'unique' normal
  // form; see GenExp#toUniqueNormalForm.
  public GenExp toNormalForm() {
    // size the product
    int numTerms = 0;
    for (Node leaf : this.plusRoot.leaves()) {
      if (leaf.exponent() > 0) { numTerms++; }
    }
    for (Node leaf : this.minusRoot.leaves()) {
      if (leaf.exponent() > 0 ) { numTerms++; }
    }
    int[] gens = new int[numTerms];
    int[] exps = new int[numTerms];
    
    // fill the product
    int i = 0;
    ArrayList<Node> leaves = this.plusRoot.leaves();
    int numLeaves = leaves.size();
    for (int index = 0; index < numLeaves; index++) {
      int exponent = leaves.get(index).exponent();
      if (exponent > 0) {
        gens[i] = index;
        exps[i] = exponent;
        i++;
      }
    }
    leaves = this.minusRoot.leaves();
    for (int index = numLeaves - 1; index >= 0; index--) {
      int exponent = leaves.get(index).exponent();
      if (exponent > 0) {
        gens[i] = index;
        exps[i] = -exponent;
        i++;
      }
    }
    return new GenExp(gens, exps);
  }

  private static Node fromTermStraight(int base, int exponent) {
    Node root = new Node();
    Node tail = root;
    for (int i = 0; i < base + exponent + 1; i++) {
      tail.setLeft(new Node());
      tail.setRight(new Node());
      tail = tail.right;
    }
    return root;
  }

  private static Node fromTermBent(int base, int exponent) {
    Node root = new Node();
    root.setLeft(new Node());
    root.setRight(new Node());
    Node tail = root;    
    for (int i = 0; i < base; i++) {
      tail = tail.right;
      tail.setLeft(new Node());
      tail.setRight(new Node());
    }
    for (int i = 0; i < exponent; i++) {
      tail = tail.left;
      tail.setLeft(new Node());
      tail.setRight(new Node());
    }
    return root;
  }

  // Return a tree pair for an element with a single term described by the
  // given base and exponent.
  public static TreePair fromTerm(int base, int exponent) {
    if (base < 0) {
      throw new IllegalArgumentException();
    }
    if (exponent == 0) {
      return new TreePair(new Node(), new Node());
    } else if (exponent < 0) {
      return new TreePair(fromTermBent(base, -exponent),
                          fromTermStraight(base, -exponent));
    } else {
      return new TreePair(fromTermStraight(base, exponent),
                          fromTermBent(base, exponent));
    }
  }

  // Returns the contribution to the word length of the given pair of caret
  // types.
  public static int contribution(CaretType minusType, CaretType plusType) {
    if ((minusType == CaretType.L0) || (plusType == CaretType.L0)) {
      if (!((minusType == CaretType.L0) && (plusType == CaretType.L0))) {
        throw new IllegalArgumentException();
      }
      return 0;
    } else {
      switch(minusType) {
        case R0:
          switch(plusType) {
            case R0:  return 0;
            case RNI: return 2;
            case RI:  return 2;
            case LL:  return 1;
            case I0:  return 1;
            case IR:  return 3;
          }
        case RNI:
          switch(plusType) {
            case R0:  return 2;
            case RNI: return 2;
            case RI:  return 2;
            case LL:  return 1;
            case I0:  return 1;
            case IR:  return 3;
          }
        case RI:
          switch(plusType) {
            case R0:  return 2;
            case RNI: return 2;
            case RI:  return 2;
            case LL:  return 1;
            case I0:  return 3;
            case IR:  return 3;
          }
        case LL:
          switch(plusType) {
            case R0:  return 1;
            case RNI: return 1;
            case RI:  return 1;
            case LL:  return 2;
            case I0:  return 2;
            case IR:  return 2;
          }
        case I0:
          switch(plusType) {
            case R0:  return 1;
            case RNI: return 1;
            case RI:  return 3;
            case LL:  return 2;
            case I0:  return 2;
            case IR:  return 4;
          }
        case IR:
          switch(plusType) {
            case R0:  return 3;
            case RNI: return 3;
            case RI:  return 3;
            case LL:  return 2;
            case I0:  return 4;
            case IR:  return 4;
          }
        default:
          throw new IllegalArgumentException();
      }
    }
  }
  
  // Returns the world length with respect to the {x_0,x_1} generating set
  public int wordLength() {
    CaretType[] minusTypes = minusRoot.caretTypes();
    CaretType[] plusTypes  = plusRoot.caretTypes();
    int numCarets = minusTypes.length;
    int length = 0;
    for (int i = 0; i < numCarets; i++) {
      length += contribution(minusTypes[i], plusTypes[i]);
    }
    return length;
  }

  // Eliminates common carrots between the tree pairs.
  public void reduce() {
    boolean passNeeded = true;
    while (passNeeded) {
      passNeeded = false;
      ArrayList<Node> minusLeaves = this.minusRoot.leaves();
      ArrayList<Node> plusLeaves  = this.plusRoot.leaves();
      int numLeaves = minusLeaves.size();
      for (int i = 0; i < numLeaves - 1; i++) {
        Node minusA = minusLeaves.get(i);
        Node minusB = minusLeaves.get(i+1);
        if (minusA.parent == minusB.parent) {
          Node plusA = plusLeaves.get(i);
          Node plusB = plusLeaves.get(i+1);
          if (plusA.parent == plusB.parent) {
            minusA.parent.prune();
            plusA.parent.prune();
            passNeeded = true;
          }
        }
      }
    }
  }
  
  private static void unifyFrom(Node plus, ArrayList<Node> plusComplements,
                                Node minus, ArrayList<Node> minusComplements) {
    if (plus.isLeaf() && minus.isLeaf()) {
      return;
    } else if (plus.isCaret() && minus.isCaret()) {
      unifyFrom(plus.left, plusComplements, minus.left, minusComplements);
      unifyFrom(plus.right, plusComplements, minus.right, minusComplements);
    } else if (plus.isLeaf() && minus.isCaret()) {
      plus.replace(minus.copy());
      plusComplements.get(plus.leafIndex).replace(minus.copy());
    } else {
      minus.replace(plus.copy());
      minusComplements.get(minus.leafIndex).replace(plus.copy());
    }
  }

  // Modifies the tree pairs so that the plus tree of the left is the same
  // as the minus tree of the right.
  private static void unify(TreePair treePairLeft, TreePair treePairRight) {
    Node plus = treePairLeft.plusRoot;
    Node minus = treePairRight.minusRoot;
    plus.indexLeaves();
    minus.indexLeaves();
    ArrayList<Node> plusComplements = treePairLeft.minusRoot.leaves();
    ArrayList<Node> minusComplements = treePairRight.plusRoot.leaves();
    unifyFrom(plus, plusComplements, minus, minusComplements);
  }
  
  // Returns the algebriac inverse of the reciever.
  public TreePair invert() {
    return new TreePair(this.plusRoot, this.minusRoot);
  }

  // Returns the algebraic product of the two given tree pairs.
  public static TreePair multiply(TreePair f, TreePair g) {
    TreePair fCopy = f.copy();
    TreePair gCopy = g.copy();
    unify(gCopy, fCopy);
    TreePair product = new TreePair(gCopy.minusRoot, fCopy.plusRoot);
    product.reduce();
    return product;
  }

  // Returns the product of all of the given factors.
  public static TreePair product(TreePair[] factors) {
    int numFactors = factors.length;
    TreePair accum = factors[0];
    for (int i = 1; i < numFactors; i++) {
      accum = multiply(accum, factors[i]);
    }
    return accum;
  }
}

