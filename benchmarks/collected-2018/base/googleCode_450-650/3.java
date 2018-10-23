// https://searchcode.com/api/result/13445830/

package arch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import exceptions.OutOfVocabularyException;

/**
 * The PhoneInventory handles storage and loading of (poly)phone inventories. It
 * is also used within the LexicalTree to model the contained words.
 * 
 * @author sikoried
 *
 */
public class PhoneInventory implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/** the roots of the phone hierarchy, mapped for convenient access */
	private HashMap<String, Polyphone> monophones = new HashMap<String, Polyphone>();
	
	/** container for all polyphones */
	private HashSet<Polyphone> allPhones = new HashSet<Polyphone>();
	
	/**
	 * Add all polyphones occurring in the given Lexicon
	 * @param lex
	 */
	public void addPolyphonesFromLexicon(Lexicon lex) {
		for (Lexicon.Entry e : lex.entries) {
			addPolyphones(Polyphone.extractPolyphonesFromWordTranscription(e.transcription));
		}
	}
	
	/**
	 * Clear the inventory for a clean start.
	 */
	public void clearPhoneInventory() {
		monophones.clear();
	}
	
	/**
	 * Add a single polyphone to the inventory, ignore if already present.
	 * Ensures the correct order within the hierarchy.
	 * 
	 * @param p Polyphone to insert.
	 */
	public void addPolyphone(Polyphone p) {
		if (allPhones.contains(p))
			return;

		// add it to the pool
		allPhones.add(p);
		
		// see if there is already a root node for the center phone
		Polyphone root = monophones.get(p.phone);
		
		if (root == null) {
			// wee, first root node for that phone
			monophones.put(p.phone, p);
		} else if (p.generalizes(root)) {
			// we need to replace the root
			monophones.put(p.phone, p);
			p.addChild(root);
		} else {
			// dig down to the right point; note that expansion goes by right first
			root.addChild(p);
		}	
	}
	
	/**
	 * Add a list of polyphones to the inventory.
	 * 
	 * @param ps Array of Polyphone instances
	 */
	public void addPolyphones(Polyphone [] ps) {
		for (Polyphone p : ps) 
			addPolyphone(p);
	}
	
	/**
	 * Rebuild the allPhones hash set by an iterative DFS
	 */
	private void rebuildAllPhones() {
		allPhones.clear();
		
		ArrayList<Polyphone> agenda = new ArrayList<Polyphone>();
		
		for (Polyphone p : monophones.values())
			agenda.add(p);
		
		while (agenda.size() > 0) {
			Polyphone p = agenda.remove(agenda.size() - 1);
			allPhones.add(p);
			for (Polyphone i : p.moreContext)
				agenda.add(i);
		}
		
		// call garbage collection, just in case...
		System.gc();
	}
	
	/**
	 * Prune the unneeded polyphones, i.e. extra "idle" links in the phonetic
	 * hierarchy.
	 */
	public void prunePhonemeHierarchy() {
		for (Polyphone p : monophones.values())
			p.pruneHierarchy();
		
		rebuildAllPhones();
	}

	/**
	 * Prune the phoneme hierarchy by their number of occurrence.
	 * 
	 * @param minOcc minimum number of occurrences of remaining polyphones
	 * @param lex Lexicon to transcribe the sentences
	 * @param sentenceFile file containing line-by-line sentences
	 */
	public void prunePhonemeHierarchyByOccurrence(int minOcc, Lexicon lex, String sentenceFile) 
		throws IOException, OutOfVocabularyException {
		// reset all occurrence counters
		for (Polyphone p : monophones.values())
			p.resetOccurrenceCounter();
		
		// read all sentences
		BufferedReader br = new BufferedReader(new FileReader(sentenceFile));
		
		String buf = null;
		while ((buf = br.readLine()) != null) {
			// split the string by whitespaces
			String [] words = buf.trim().split("\\s+");
			
			for (String w : words) {				
				// translate the word to transcription, get the polyphone sequence
				Polyphone [] trans = translateWord(lex.translate(w));
				
				// do the counting; don't forget to update the lesser contexts!
				for (Polyphone p : trans) {
					do {
						p.occurrences++;
					} while ((p = p.lessContext) != null);					
				}
			}
		}
		
		br.close();
		
		// now do the actual pruning
		for (Polyphone p : monophones.values()) {
			p.pruneHierarchyByOccurrence(minOcc);
			p.pruneHierarchy();
		}
		
		rebuildAllPhones();
	}
	
	/**
	 * Get the size of the phone inventory.
	 * 
	 * @return Size of the phone inventory.
	 */
	public int size() {
		return allPhones.size();
	}
	
	/**
	 * Retrieve the most general polyphone for the given phone
	 * @param phone
	 * @return
	 */
	public Polyphone getMonophone(String phone) {
		return monophones.get(phone);
	}
	
	/**
	 * Retrieve a certain polyphone
	 * @param phoneInContext e.g. S/n/o
	 * @return
	 */
	public Polyphone getPolyphone(String phoneInContext) {
		for (Polyphone p : allPhones)
			if (p.equals(phoneInContext))
				return p;
		
		return null;
	}
	
	/**
	 * Construct the Polyphone sequence for a given transcription. The polyphone 
	 * with the longest matching context will be used.
	 * 
	 * @param transcription word transcription including word boundaries at beginning and end!
	 * @return Polyphone sequence w/o word boundary markers
	 */
	public Polyphone [] translateWord(String [] transcription) {
		ArrayList<Polyphone> seq = new ArrayList<Polyphone>();
		
		// for each phoneme, get the polyphone with the most context
		for (int i = 1; i < transcription.length - 1; ++i) {
			// of course no models for boundaries...
			if (transcription[i].equals(Polyphone.SB) || transcription[i].equals(Polyphone.WB))
				continue;
			
			// retrieve monophone, then dig down
			Polyphone cand = monophones.get(transcription[i]);
			Polyphone oldc = null;
			
			// descend 
			while (cand.matchesTranscription(transcription, i)) {
				if (cand.moreContext.length == 0)
					break;
				
				// find the next candidate to dig down
				for (Polyphone p : cand.moreContext) {
					if (p.matchesTranscription(transcription, i)) {
						oldc = cand;
						cand = p;
						break;
					} else
						oldc = cand;
				}
				
				// detect if we're at the bottom
				if (oldc == cand)
					break;
			}
			
			seq.add(cand);
		}
		
		return seq.toArray(new Polyphone [seq.size()]);
	}
	
	/**
	 * Get a String representation of the phone inventory, including the polyphone
	 * hierarchy.
	 * @return the hierarchy in ASCII art
	 */
	public String hierarchyAsString() {
		StringBuffer sb = new StringBuffer();
		
		for (Polyphone p : monophones.values())
				sb.append(p.hierarchyAsString());

		return sb.toString();
	}
	
	/**
	 * Generate a .dot representation of the phoneme hierarchy for a visual check 
	 * @return
	 */
	public String hierarchyAsDotFormat(boolean includeHeader) {
		StringBuffer dot = new StringBuffer();
		
		if (includeHeader) {
			dot.append("digraph Polyphones {\n" +
				"ordering=out;\n" +
				"rankdir=LR;\n" +
				"node [shape=box];\n");
		}
		
		for (Polyphone p : monophones.values()) {
			dot.append(p.hierarchyAsDotFormat(false));
		}
		
		if (includeHeader)
			dot.append("}\n");
		
		return dot.toString();
	}
	
	/**
	 * Return information about this PhoneInventory.
	 */
	public String toString() {
		return "PhoneInventory with " + monophones.size() + " monophones and a total of " + allPhones.size() + " polyphones";
	}
	
	/**
	 * Reduce the phone inventory to a monophone ONLY inventory
	 */
	public void reduceToMonophone() {
		allPhones.clear();
		for (Polyphone p : monophones.values()) {
			p.moreContext = new Polyphone [0];
			p.lessContext = null;
			allPhones.add(p);
		}
	}
	
	/**
	 * Reduce the phone inventory to a mono- and biphone inventory
	 */
	public void reduceToBiphone() {
		// build up new allPhones set (integrated below)
		allPhones.clear();
		allPhones.addAll(monophones.values());
		
		// cut out all polyphones with larger context
		for (Polyphone p : monophones.values()) {
			ArrayList<Polyphone> reducedContext = new ArrayList<Polyphone>();
			for (Polyphone i : p.moreContext) {
				if (i.isBiphone()) {
					i.moreContext = new Polyphone [0];
					reducedContext.add(i);
				}
			}
			
			p.moreContext = reducedContext.toArray(new Polyphone [reducedContext.size()]);
			allPhones.addAll(reducedContext);
		}
	}
	
	/**
	 * Reduce the phone inventory to a mono-, bi and tri-phone inventory
	 */
	public void reduceToTriphone() {
		// build up new allPhones set (integrated below)
		allPhones.clear();
		allPhones.addAll(monophones.values());
		
		// cut out all polyphones with larger context
		for (Polyphone p : monophones.values()) {
			ArrayList<Polyphone> reducedContext1 = new ArrayList<Polyphone>();
			for (Polyphone i : p.moreContext) {
				// biphone: check for nested triphones
				if (i.isBiphone()) {
					ArrayList<Polyphone> reducedContext2 = new ArrayList<Polyphone>();
					for (Polyphone j : i.moreContext) {
						if (j.isTriphone()) {
							j.moreContext = new Polyphone [0];
							reducedContext2.add(j);
						}
					}
					i.moreContext = reducedContext2.toArray(new Polyphone [reducedContext2.size()]);
					allPhones.addAll(reducedContext2);
					reducedContext1.add(i);
				} else if (i.isTriphone()) {
					i.moreContext = new Polyphone [0];
					reducedContext1.add(i);
				}
			}
			
			p.moreContext = reducedContext1.toArray(new Polyphone [reducedContext1.size()]);
			allPhones.addAll(reducedContext1);
		}
	}
	
	public static final String synopsis = 
		"sikoried, 12-15-2009\n\n" +
		"Construct and/or prune a phone inventory using an alphabet, lexicon and\n" +
		"and training file.\n\n" +
		"usage: arch.PhoneInventory [options]\n" +
		"  --lexicon alphabet lexicon\n" +
		"    Use the given alphabet (one token per line) and lexicon (one pair per\n" +
		"    line: word[tab]transcription) for all actions.\n" +
		"  --construct" +
		"    Construct a phone inventory using the given alphabet and lexicon (--lexicon).\n" +
		"  --no-initial-pruning\n" +
		"    Do not apply pruning after hierarchy extension (strongly disadvised!)\n" +
		"  --load phone-inv-file\n" +
		"    Load a pre-compiled phone inventory from the given file.\n" +
		"  --save phone-inv-file\n" + 
		"    Save the final phone inventory to the given file.\n" +
		"\n" +
		"  --prune train-file min-occ\n" +
		"    Prune the phone inventory by counting the phone occurrences in the\n" +
		"    train-file (ASCII, one sentence per line) and removing phones that\n" +
		"    occure less than min-occ times. --lexicon is required.\n" +
		"  --reduce-1\n" +
		"    Reduce the phone inventory to mono-phones.\n" +
		"  --reduce-2\n" +
		"    Reduce the phone inventory to mono- and bi-phones.\n" +
		"  --reduce-3\n" +
		"    Reduce the phone inventory to mono-, bi- and tri-phones\n" +
		" \n" +
		"  --print-all\n" +
		"    Print a complete ASCII representation of the phone hierarchy.\n" +
		"  --print-phone\n" +
		"    Print the hierarchy for a given monophone (e.g. /t/)\n" +
		"  --dot-format\n" +
		"    Output .dot format instead of human readable ASCII\n" +
		"\n" +
		"  -h | --help\n" +
		"    Print this help text.";
	
	public static void main(String[] args) 
		throws IOException, ClassNotFoundException, OutOfVocabularyException {
		
		if (args.length < 2) {
			System.err.println(synopsis);
			System.exit(1);
		}
		
		String iFile = null;
		String aFile = null;
		String lFile = null;
		String oFile = null;
		String pFile = null;
		
		int minOcc = 0;
		int reduce = 0;
		
		boolean construct = false;
		boolean printAll = false;
		boolean noInitialPruning = false;
		boolean dotFormat = false;
		
		ArrayList<String> printMonos = new ArrayList<String>();
		
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("--lexicon")) {
				aFile = args[++i];
				lFile = args[++i];
			} else if (args[i].equals("--construct"))
				construct = true;
			else if (args[i].equals("--save"))
				oFile = args[++i];
			else if (args[i].equals("--load"))
				iFile = args[++i];
			else if (args[i].equals("--no-initial-pruning"))
				noInitialPruning = true;
			else if (args[i].equals("--prune")) {
				pFile = args[++i];
				minOcc = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--print-all"))
				printAll = true;
			else if (args[i].equals("--print-phone")) {
				// ignore potential '/'
				printMonos.add(args[++i].replaceAll("/", ""));
			} else if (args[i].equals("--dot-format"))
				dotFormat = true;
			else if (args[i].equals("--reduce-1"))
				reduce = 1;
			else if (args[i].equals("--reduce-2"))
				reduce = 2;
			else if (args[i].equals("--reduce-3"))
				reduce = 3;
			else if (args[i].equals("-h") || args[i].equals("--help")) {
				System.err.println(synopsis);
				System.exit(0);
			} else {
				System.err.println("Unknown parameter \"" + args[i] + "\". See --help");
				System.exit(1);
			}
		}
		
		if (iFile != null && construct)
			throw new IOException("--construct and --load are exclusive");
		
		if (printAll && printMonos.size() > 0)
			throw new IOException("--print-all and --print-phone are exclusive");
		
		Lexicon lex = null;
		PhoneInventory pi = null;
		
		if (iFile != null) {
			System.err.println("Loading phone inventory from '" + iFile + "'...");
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(iFile));
			pi = (PhoneInventory) ois.readObject();
			ois.close();
		} 
		
		if (aFile != null && lFile != null) {
			System.err.print("Loading lexicon...");
			lex = Lexicon.readLexiconFromFile(aFile, lFile);
			System.err.println("OK\n" + lex);
		}
		
		if (construct) {
			System.err.print("Constructing new phone inventory...");
			pi = new PhoneInventory();
			pi.addPolyphonesFromLexicon(lex);
			System.err.println("OK");
			if (!noInitialPruning) {
				System.err.print("Initial pruning...");
				pi.prunePhonemeHierarchy();
				System.err.println("OK");
			} else
				System.err.println("WARNING: no initial pruning -- tremendous hierarchy size expected!");
			
		}
		
		if (pi == null) {
			System.err.println("No phone inventory loaded. Bye.");
			System.exit(1);
		}
		
		System.err.println("Phone inventory ready: " + pi);
		
		// reduce if requested
		if (reduce == 1) {
			System.err.println("Reducing to mono-phone inventory...");
			pi.reduceToMonophone();
			System.err.println(pi);
		} else if (reduce == 2) {
			System.err.println("Reducing to bi-phone inventory...");
			pi.reduceToBiphone();
			System.err.println(pi);
		} else if (reduce == 3) {
			System.err.println("Reducing to tri-phone inventory...");
			pi.reduceToTriphone();
			System.err.println(pi);
		}
		
		// prune if requested
		if (pFile != null) {
			System.err.println("Pruning inventory using text data in '" + pFile + "'...");
			pi.prunePhonemeHierarchyByOccurrence(minOcc, lex, pFile);
			System.err.println(pi);
		}
		
		// print if requested
		if (printAll) 		
			System.out.println(dotFormat ? pi.hierarchyAsDotFormat(true) : pi.hierarchyAsString());
		
		for (String m : printMonos)
			System.out.println(dotFormat ? pi.getMonophone(m).hierarchyAsDotFormat(true) : pi.getMonophone(m).hierarchyAsString());
		
		if (oFile != null) {
			System.err.print("Writing inventory to file...");
		
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(oFile));
			oos.writeObject(pi);
			oos.close();
					
			System.err.println("OK");
		}
	}
}

