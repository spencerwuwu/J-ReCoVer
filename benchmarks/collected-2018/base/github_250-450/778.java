// https://searchcode.com/api/result/106194111/

// FibonacciHeap.java, created Sat Feb 12 16:37:26 2000 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package net.cscott.jutil;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
/**
 * A {@link FibonacciHeap} allows amortized O(1) time bounds for
 * create, insert, minimum, union, and decrease-key operations, and
 * amortized O(lg n) run times for extract-min and delete.
 * <p>
 * Implementation is based on the description in <i>Introduction to
 * Algorithms</i> by Cormen, Leiserson, and Riverst, in Chapter 21.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: FibonacciHeap.java,v 1.8 2006-10-30 19:58:05 cananian Exp $
 */
public class FibonacciHeap<K,V> extends AbstractHeap<K,V> {

    Node<K,V> min=null;
    /** Number of nodes in this heap. */
    int n=0;
    /** Maximum degree of any node */
    int D=0;
    final Comparator<Map.Entry<K,V>> c; // convenience field.
    
    /** Creates a new, empty {@link FibonacciHeap}, sorted according
     *  to its keys' natural order.  All keys inserted into the new
     *  map must implement the {@link Comparable} interface.
     *  O(1) time. */
    public FibonacciHeap() { this(Collections.<Map.Entry<K,V>>emptySet(), null); }
    /** Creates a new, empty {@link FibonacciHeap}, sorted according
     *  to the given {@link Comparator}.  O(1) time. */
    public FibonacciHeap(Comparator<K> c) { this(Collections.<Map.Entry<K,V>>emptySet(), c); }
    /** Constructs a new heap with the same entries as the specified
     *  {@link Heap}. O(n) time. */
    public FibonacciHeap(Heap<K,? extends V> h) {
	this(h.entries(), h.comparator());
    }
    /** Constructs a new heap from a collection of
     *  {@link java.util.Map.Entry}s and a key comparator. O(n) time. */
    public FibonacciHeap(Collection<? extends Map.Entry<? extends K,? extends V>> collection, Comparator<K> comparator) {
	super(comparator);
	c = entryComparator();
	for (Map.Entry<? extends K,? extends V> e : collection) {
	    insert(e.getKey(), e.getValue());
	}
    }

    /** Insert an entry into the heap. */
    public Map.Entry<K,V> insert(K key, V value) {
	Entry<K,V> e = new Entry<K,V>(key, value);
	insert(e);
	return e;
    }
    protected void insert(Map.Entry<K,V> me) {
	Node<K,V> x = new Node<K,V>((Entry<K,V>)me);
	// THESE ARE DONE IN THE CONSTRUCTOR.
	// x.degree=0; x.parent=x.child=null;
	// x.left = x.right = x;  x.mark = false;
	_concatenateListsContaining(x, min);
	if (min==null || c.compare(x.entry, min.entry) < 0)
	    min = x;
	n++; // increase size;
	// done.
    }
    public Map.Entry<K,V> minimum() {
	if (this.min==null) throw new java.util.NoSuchElementException();
	return this.min.entry;
    }
    public void union(FibonacciHeap<K,V> h) {
	// if you're comparing this to CLR, this=='h1' and h=='h2'
	_concatenateListsContaining(this.min, h.min);
	if (this.min==null || 
	    (h.min!=null && c.compare(h.min.entry, this.min.entry) < 0))
	    this.min = h.min;
	this.n += h.n;
	h.clear();
	// done.
    }
    public void union(Heap<? extends K,? extends V> h) {
	if (h instanceof FibonacciHeap &&
	    entryComparator().equals(((FibonacciHeap)h).entryComparator()))
	    // the unsafe cast below from K2 to K and V2 to V should really be
	    // safe if the entryComparators for the two Heaps are identical.
	    union((FibonacciHeap<? extends K,? extends V>)h);
	else super.union(h);
    }
    public Map.Entry<K,V> extractMinimum() {
	if (this.min==null) throw new java.util.NoSuchElementException();
	Node<K,V> z = this.min;
	Node<K,V> x = z.child;
	if (x!=null) do { // for each child x of z...
	    x.parent=null;
	    x=x.right;
	} while (x!=z.child);
	// add z's children to the root list.
	_concatenateListsContaining(z.child, z);
	// remove z from the root list.
	Node<K,V> zRight = z.right;
	_removeFromList(z);
	if (z==zRight) { // z was the only node on the root list
	    assert n==1;
	    min = null;
	} else {
	    assert n>1;
	    min = zRight;
	    _consolidate();
	}
	n--;
	return z.entry;
    }
    // reduce the number of trees in the fibonacci heap
    private void _consolidate() {
	ArrayList<Node<K,V>> A = new ArrayList<Node<K,V>>
	    (Collections.<Node<K,V>>nCopies(D(n)+1, null));
	// for each node w in the root list of H
	// (remove each node from the root list as we iterate)
	for (Node<K,V> w = min; w!=null; ) {
	    Node<K,V> x = w;
	    { // iterator
		Node<K,V> wR = w.right; 
		_removeFromList(w);
		w = (w==wR) ? null : wR;
	    }
	    int  d = x.degree;
	    while (A.get(d) != null) {
		Node<K,V> y = A.get(d);
		if (c.compare(x.entry, y.entry) > 0) {
		    Node<K,V> t=x; x=y; y=t; // exchange x and y
		}
		_link(y, x);
		A.set(d, null);
		d++;
	    }
	    A.set(d, x);
	}
	
	min = null;
	for (int i=0; i < A.size(); i++) {
	    if (A.get(i) != null) {
		// add A[i] to the root list of H (it already has no siblings)
		assert A.get(i).parent == null; // it is a root.
		assert A.get(i).left == A.get(i) && A.get(i).right == A.get(i);
		_concatenateListsContaining(min, A.get(i));
		if (min==null ||
		    c.compare(A.get(i).entry, min.entry) < 0)
		    min = A.get(i);
	    }
	}
    }
    private void _link(Node<K,V> y, Node<K,V> x) {
	// both x and y are roots.
	assert x.parent==null && y.parent==null;
	// y should already have been removed from root list.
	assert y.left == y && y.right == y;
	x.addChild(y);
	y.mark = false;
    }

    public void decreaseKey(Map.Entry<K,V> me, K newkey) {
	decreaseKey((Entry<K,V>)me, newkey, false);
    }
    // if 'delete' is true, newkey is effectively -infinity.
    private void decreaseKey(Entry<K,V> entry, K newkey, boolean delete) {
	if (!delete && keyComparator().compare(newkey, entry.getKey()) > 0)
	    throw new UnsupportedOperationException("New key is greater than "+
						    "current key.");
	setKey(entry, newkey);
	Node<K,V> x = entry.node;
	Node<K,V> y = x.parent;
	if (y!=null && (delete || c.compare(x.entry, y.entry) < 0)) {
	    _cut(x, y);
	    _cascadingCut(y);
	}
	if (delete || c.compare(x.entry, min.entry) < 0)
	    min = x;
	// ta-da!
    }
    private void _cut(Node<K,V> x, Node<K,V> y) {
	y.removeChild(x);
	_concatenateListsContaining(x, min);
	x.parent = null;
	x.mark = false;
    }
    private void _cascadingCut(Node<K,V> y) {
	Node<K,V> z = y.parent;
	if (z==null) return;
	if (y.mark == false) {
	    y.mark = true;
	} else {
	    _cut(y, z);
	    _cascadingCut(z);
	}
    }
    public void delete(Map.Entry<K,V> me) {
	decreaseKey((Entry<K,V>)me, null, true); // effectively key is -infinity
	extractMinimum();
	// now that wasn't too hard, was it?
    }

    public int size() { return n; }
    public void clear() { min=null; n=0; }

    public Collection<Map.Entry<K,V>> entries() {
	return new AbstractCollection<Map.Entry<K,V>>() {
	    public int size() { return n; }
	    public Iterator<Map.Entry<K,V>> iterator() {
		return new UnmodifiableIterator<Map.Entry<K,V>>() {
		    Node<K,V> next = min;
		    public boolean hasNext() { return next!=null; }
		    public Map.Entry<K,V> next() {
			if (next==null)
			    throw new java.util.NoSuchElementException();
			Node<K,V> n=next; next=successor(next); return n.entry;
		    }
		};
	    }
	};
    }
    /** Return the next node after the specified node in the iteration order.
     *  O(1) time. */
    private Node<K,V> successor(Node<K,V> n) {
	assert n!=null;
	if (n.child!=null) return n.child;
	do {
	Node<K,V> first = (n.parent==null) ? this.min : n.parent.child;
	assert first!=null && n.right!=null;
	if (n.right != first) return n.right;
	n=n.parent;
	} while (n!=null);
	return null;
    }

    /** The underlying {@link java.util.Map.Entry} representation. */
    static final class Entry<K,V> extends PairMapEntry<K,V> {
	Node<K,V> node;
	Entry(K key, V value) { super(key, value); }
	K _setKey(K key) { return super.setKey(key); }
    }
    // to implement updateKey, etc...
    protected final K setKey(Map.Entry<K,V> me, K newkey) {
	Entry<K,V> e = (Entry<K,V>) me;
	return e._setKey(newkey);
    }
    /** The underlying node representation for the fibonacci heap. */
    static final class Node<K,V> {
	Node<K,V> parent, child;
	Entry<K,V> entry;
	Node<K,V> left, right;
	int degree;
	boolean mark;
	/*-----------------------------*/
	Node(Entry<K,V> e) {
	    this.entry = e;
	    this.entry.node = this;
	    this.parent = this.child = null;
	    this.degree=0;
	    this.left = this.right = this;
	    this.mark = false;
	}
	void addChild(Node<K,V> c) {
	    assert c.left == c && c.right == c;
	    if (this.child==null) this.child = c;
	    else _concatenateListsContaining(this.child, c);
	    c.parent = this;
	    degree++;
	}
	void removeChild(Node<K,V> c) {
	    assert c.parent == this;
	    if (this.child==c) this.child=c.right;
	    _removeFromList(c);
	    c.parent = null;
	    degree--;
	    if (degree==0) this.child=null;
	}
	public String toString() {
	    return "<"+entry+", d:"+degree+", parent:"+_2s(parent)+
		", child:"+_2s(child)+", left:"+_2s(left)+
		", right:"+_2s(right)+", mark:"+mark+">";
	}
	private String _2s(Node<K,V> n) {return n==null?"(nil)":n.entry.toString();}
    }
    /** Concatenate two right/left lists together. */
    private static <K,V> void _concatenateListsContaining(Node<K,V> a, Node<K,V> b) {
	if (a==null || b==null) return; // nothing to link in.
	Node<K,V> c = a.right, d = b.left;
	a.right = b;
	b.left  = a;
	d.right = c;
	c.left  = d;
    }
    /** Remove a node from its left/right list. */
    private static <K,V> void _removeFromList(Node<K,V> a) {
	a.left.right = a.right;
	a.right.left = a.left;
	a.left = a.right = a;
    }
    /** Calculate the value of D(n) */
    private static int D(int n) {
	// calculate log-base-phi of n as (log n/log phi)
	double Dn = Math.log(n) / log_phi;
	return (int) Math.floor(Dn);
    }
    private final static double phi = (1 + Math.sqrt(5)) / 2.0;
    private final static double log_phi = Math.log(phi);

    /** Self-test method. */
    public static void main(String[] args) {
	{
	Heap<Integer,Integer> h = new FibonacciHeap<Integer,Integer>();
	assert h.size()==0 && h.isEmpty();
	// example from CLR, page 146/151
	h = new FibonacciHeap<Integer,Integer>
	    (new AbstractCollection<Map.Entry<Integer,Integer>>() {
	    int el[] = { -4, -1, -3, -2, -16, -9, -10, -14, -8, -7 };
	    public int size() { return el.length; }
	    public Iterator<Map.Entry<Integer,Integer>> iterator() {
		return new UnmodifiableIterator<Map.Entry<Integer,Integer>>() {
		    int i = 0;
		    public boolean hasNext() { return i<el.length; }
		    public Map.Entry<Integer,Integer> next() {
			Integer io = new Integer(el[i++]);
			return new PairMapEntry<Integer,Integer>(io, io);
		    }
		};
	    }
	}, null/* default comparator */);
	assert h.size()==10 && !h.isEmpty();
	assert h.minimum().getKey().equals(new Integer(-16));
	System.out.println(h);
	h.insert(new Integer(-15), new Integer(-15));
	assert h.size()==11 && !h.isEmpty();
	assert h.minimum().getKey().equals(new Integer(-16));
	System.out.println(h);
	// now verify that we'll get all the keys out in properly sorted order
	System.out.println(h.size());
	assert h.extractMinimum().getKey().equals(new Integer(-16));
	System.out.println(h.size());
	System.out.println(h);
	assert h.extractMinimum().getKey().equals(new Integer(-15));
	assert h.extractMinimum().getKey().equals(new Integer(-14));
	assert h.extractMinimum().getKey().equals(new Integer(-10));
	assert h.extractMinimum().getKey().equals(new Integer(-9));
	assert h.extractMinimum().getKey().equals(new Integer(-8));
	assert h.extractMinimum().getKey().equals(new Integer(-7));
	assert h.extractMinimum().getKey().equals(new Integer(-4));
	assert h.extractMinimum().getKey().equals(new Integer(-3));
	assert h.extractMinimum().getKey().equals(new Integer(-2));
	assert h.extractMinimum().getKey().equals(new Integer(-1));
	assert h.isEmpty() && h.size()==0;
	// okay, test delete and decreaseKey now.
	h.clear(); assert h.isEmpty() && h.size()==0;
	}{
	Heap<String,String> h = new FibonacciHeap<String,String>();
	assert h.isEmpty() && h.size()==0;

	ArrayList<Map.Entry<String,String>> mel =
	    new ArrayList<Map.Entry<String,String>>();
	mel.add(h.insert("C", "c1"));
	mel.add(h.insert("S", "s1"));
	mel.add(h.insert("A", "a"));
	mel.add(h.insert("S", "s2"));
	mel.add(h.insert("C", "c2"));
	mel.add(h.insert("O", "o"));
	mel.add(h.insert("T", "t1"));
	mel.add(h.insert("T", "t2"));
	mel.add(h.insert("Z", "z"));
	mel.add(h.insert("M", "m"));

	assert h.extractMinimum().getValue().equals("a");
	System.out.println("1: "+h);
	h.decreaseKey(mel.get(3), "B"); // s2
	System.out.println("2: "+h);
	assert h.extractMinimum().getValue().equals("s2");
	h.delete(mel.get(4)); // c2
	System.out.println("3: "+h);
	assert h.extractMinimum().getValue().equals("c1");
	System.out.println("4: "+h);
	// finally, test updateKey
	h.updateKey(mel.get(9), "P"); // m
	assert h.extractMinimum().getValue().equals("o");
	assert h.extractMinimum().getValue().equals("m");
	System.out.println("5: "+h);
	// DONE.
	System.out.println("PASSED.");
	}
    }
}

