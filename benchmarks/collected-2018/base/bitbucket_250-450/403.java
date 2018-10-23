// https://searchcode.com/api/result/53872816/

/* program to solve problem stated at http://cs.brown.edu/courses/cs019/2009/assignments/nile
 * for cs019 at brown university, fall 09.
 * general thoughts for improvement:
 *  do a better job parsing arguments... the main function is kind of ugly
 *  reduce code duplication in the ranking algorithms (they are really similar!)
 *  come up with a more useful datatype to return from the ranking, so they arent just strings or reusing other types
 * @dbpatterson 17 Sept 2009  */

public class Nile {
    private static void fail(String message) {
	System.out.println(message);
	System.exit(1);
    }
    private static String usage =  "Usage:\n java Nile directory\n or\n java Nile directory \"Book Identifier\"\n or \n java Nile test this code";
    public static void main(String[] args) {
	// Parse arguments
	if (args.length == 0) {
	    fail("Need at least one argument. ".concat(usage));
	} else if ((args.length == 3) && args[0].equals("test") && args[1].equals("this") && args[2].equals("code")) {
	    runTests();
	} else {
	    String[] files = NileIO.getDirectoryContents(args[0]);
	    if (files == null)
		fail("Unable to get the contents of the directory. Perhaps it was typed incorrectly?");
	    
	    // Read in the data
	    FileReader[] frs = new FileReader[files.length];
	    for (int n=0; n<files.length; n++)
		frs[n] = new FileReader(files[n]);
	    
	    PairHolder pairs = new PairHolder();
	    pairs.load(frs);

	    if (pairs.size() == 0)
		fail("No Pairs Found in Files. Either the directory did not contain files with contents, or each had only one book listed (which is not valid based on the spec).");

	    // Set up ranker
	    NileRanker ranker = new NileRanker(pairs);
	    
	    // Perform requested operation
	    if (args.length == 1) { // most popular pairs
		PairHolder top = ranker.topPairs();
		if (top.size() > 0) { // hopefully redundant error checking
		    System.out.println(top.get(0).count());
		    System.out.println(top);
		} else {
		    fail("No Pairs Were Returned by NileRanker, which means something went very wrong in the ranking.");
		}
	    }
	    else if (args.length == 2) { // most popular matches
		int topcount = 0;
		String book = args[1];
		
		String[] topmatches = ranker.topMatches(book);
		
		if (topmatches.length > 0) { // so the book exists in the system
		    for (int n=0; n<topmatches.length; n++) {
			System.out.println(topmatches[n]);
		    }
		} else {
		    fail("Book Not Found. Are you sure you typed it correctly?");
		}
	    } else {
		fail(usage);
	    }
	}
    }

    // TESTING
    private static int testCount = 0;
    private static int passCount = 0;
    private static int failCount = 0;
    private static String[] fails = new String[100];
    private static void check(String name, boolean test) {
	testCount++;
	if (test) {
	    passCount++;
	} else {
	    fails[failCount++] = name;
	}
    }
    private static void runTests() {
	// These are only unit tests. in addition to doing this, we need whole program level testing, which is provided by NileTests.hs (need GHC >= v 6.10, run it using runghc NileTests.hs)
	// Pair
	Pair p = new Pair("a","b");
	check("Pair() and Pair.count()",p.count() == 1);
	check("Pair.toString()",p.toString().equals("a+b"));
	check("Pair.contains() - true",p.contains("b"));
	check("Pair.contains() - false", !p.contains("c"));
	check("Pair.other() - value",p.other("a").equals("b"));
	check("Pair.other() - non-existent",p.other("").equals("a"));
	Pair q = new Pair("a","b");
	check("Pair.compareAdd() - same",((p.compareAdd(q) == true) && (p.count() == 2)));
	p = new Pair("a","b");
	Pair r = new Pair("b","a");
	check("Pair.compareAdd() - same, but diff order", ((p.compareAdd(r) == true) && (p.count() == 2)));
	p = new Pair("a","b");
	Pair s = new Pair("c","d");
	check("Pair.compareAdd() - diff", ((p.compareAdd(s) == false) && (p.count() == 1)));

	// PairHolder
	PairHolder pairs = new PairHolder();
	check("PairHolder() and PairHolder.size()", pairs.size() == 0);
	check("PairHolder.load() -- not possible to test", false); //untestable, but important to remind of
	pairs.addPair(p);
	check("PairHolder.addPair() and PairHolder.get() - new", (pairs.size() == 1 && pairs.get(0).other("").equals("a") && pairs.get(0).other("a").equals("b")));
	pairs.addPair(q);
	pairs.addPair(r);
	check("PairHolder.addPair() - duplicates", (pairs.size() == 1 && pairs.get(0).count() == 3));
	pairs.addPair(s);
	check("PairHolder.addPair() - different", (pairs.size() == 2 && pairs.get(1).other("").equals("c") && pairs.get(1).other("c").equals("d")));
	check("PairHolder.toString", pairs.toString().equals("a+b\nc+d"));

	// NileRanker
	pairs.addPair(new Pair("a","d"));
	pairs.addPair(new Pair("d","a"));
	pairs.addPair(new Pair("a","d"));
	pairs.addPair(new Pair("a","e"));
	NileRanker ranker = new NileRanker(pairs);
	check("NileRanker.topPairs()", ranker.topPairs().toString().equals("a+b\na+d") && ranker.topPairs().get(0).count() == 3);
	String[] tms = ranker.topMatches("a");
	check("NileRanker.topMatches()", tms[0].equals("3") && tms[1].equals("b") && tms[2].equals("d"));

	// Output report
	System.out.println("Passed " + passCount + " out of " + testCount + " tests.");
	if (failCount != 0) {
	    System.out.println("Failing tests were:");
	    for (int n=0; n<failCount; n++)
		System.out.println(fails[n]);
	}
    }
}

class FileReader {
    private String[] items;
    public FileReader(String filename) {
	items = NileIO.getFileContents(filename);
	if (items == null) // file inaccessible
	    items = new String[0]; // empty and inaccessible are identical for our purposes.
    }
    public int size() {
	return items.length;
    }
    public String get(int number) {
	return items[number]; // will throw an exception if you try to access too high, which is okay behavior,
    }                         // as there doesn't seem to be anything else to do (but throw an exception).
    public String toString() {
	String out = "";
	for(int n=0; n<size(); n++)
	    out = out.concat(items[n]).concat("\n");
	return out;
    }
}

class PairHolder {
    private int count;
    private Pair[] pairs;
    public PairHolder() {
	count = 0;
	pairs = new Pair[0];
    }
    public void load(FileReader[] filereaders) {
	for (int n=0; n<filereaders.length; n++) {
	    FileReader fr = filereaders[n];
	    /* This algorithm is probably worth clarifying. Since we want to get all pairs, 
	       but not duplicate any and not create pairs with ourself, the easiest way is 
	       (I think) to borrow from math, combinations kind of. 
	       It should create N!/(N-2)! pairs, and the incremental method can be sort of 
	       thought of visually as the upper triangle part of a box (with k going down 
	       the side, and l going across). Thus each is paired with another w/o dups or 
	       pairing with self. 
	       But perhaps the code is actually clearer than the explanation. 
	     */
	    for (int k=0; k < fr.size(); k++)
		for (int l=k+1; l < fr.size(); l++)
		    addPair(new Pair(fr.get(k),fr.get(l)));
	}
    }
    public String toString() {
	String out = "";
	for(int n=0;n<count;n++)
	    out = out.concat(pairs[n].toString()).concat("\n");
	if (out.length() > 0) // so if we toString an empty holder, we dont try to
	    out = out.substring(0,out.length()-1); // strip off the extraneous newline.	
	return out;
    }
    public void addPair(Pair p) {
	if (pairs.length == count) { // means we are out of space, alloc some more.
	    Pair[] tmp = new Pair[pairs.length + 10]; // arbitrary decision of ten. seems okay for now.
	    System.arraycopy(pairs, 0, tmp, 0, pairs.length);
	    pairs = tmp;
	}
	boolean same = false;
	for (int n=0; n < size(); n++) {
	    same = get(n).compareAdd(p);
	    if (same)
	    	break; // dont bother wasting cycles checking the rest
	}
	if (!same)
	    pairs[count++] = p;
    }
    public int size() {
	return count;
    }
    public Pair get(int n) {
	return pairs[n];
    }
}

class Pair {
    private String a;
    private String b;
    private int count;
    public Pair(String x, String y) {
	a = x;
	b = y;
	count = 1;
    }
    public boolean compareAdd(Pair p) {
	String u = p.other("");
	String v = p.other(u);
	if ((a.equals(u) && b.equals(v)) || (a.equals(v) && b.equals(u))) { // equal, just inc count.
	    count++;
	    return true; // to let caller know that the pair already exists
	} else {
	    return false;
	}
    }
    public int count() {
	return count;
    }
    public String toString() {
	return a + "+" + b;
    }
    public boolean contains(String name) {
	return (a.equals(name) || b.equals(name));
    }
    public String other(String name) {
	if (a.equals(name)) {
	    return b;
	} else { // this is sort of an ugly choice, but useful, because it allows you to get stuff
	    return a; // out of the pair quickly, which you need to compare one to another.
	}
    }
}

class NileRanker {
    private PairHolder pairs;
    public NileRanker(PairHolder p) {
	pairs = p;
    }
    public PairHolder topPairs() {
	int topcount = 0;
	PairHolder top = new PairHolder();
	/* basically the way this works is it runs through assuming that the highest reached count is the top, until it is proven otherwise at which point it drops all those already collected and continues collecting using the higher count.
	 */
	for(int n=0; n<pairs.size(); n++) {
	    Pair x = pairs.get(n);
	    if (x.count() > topcount) {
		top = new PairHolder(); // get rid of the old ones, they werent the top
		top.addPair(x);
		topcount = x.count();
	    } else if (x.count() == topcount) { // add it to the list
		top.addPair(x);
	    } else {
		// do nothing
	    }
	}
	return top;
    }
    public String[] topMatches(String name) {
	int topcount = 0;
	String[] top = new String[pairs.size()]; // the upper bound of possible pairs.
	int number = 0;
	/* this is unfortunately similar to the code above, but different enough I'm not sure how to factor out the commonalities.
	 */
	for(int n=0; n<pairs.size(); n++) {
	    Pair x = pairs.get(n);
	    if (x.contains(name)) {
		if (x.count() > topcount) {
		    top = new String[pairs.size()]; // get rid of the old ones, they werent the top
		    number = 0;
		    top[number++] = x.other(name);
		    topcount = x.count();
		} else if (x.count() == topcount) { // add it to the list
		    top[number++] = x.other(name);
		} else {
		    // do nothing
		}
	    } else {
		// do nothing 
	    }
	}
	// stick the count at the beginning, and trim off extra
	if (topcount > 0) {
	    String[] tmp = new String[number + 1];
	    String[] tc = { Integer.toString(topcount) };
	    System.arraycopy(tc, 0, tmp, 0, 1);
	    System.arraycopy(top, 0, tmp, 1, number);
	    top = tmp;
	} else {
	    top = new String[0]; // trim off the 'extra'
	}
	return top;
    }
}

