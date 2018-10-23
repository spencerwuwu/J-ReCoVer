// https://searchcode.com/api/result/57977019/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class LehvensteinDistance {
	
	Map<Integer,ArrayList<String>> childhood;
	List<String> adulthood;
	// retirementIsConceptually a list, but in practice is more efficient with a single int
	int retirement;

	
	public LehvensteinDistance(){
		childhood = new HashMap<Integer,ArrayList<String>>();
		
		for (int i = 0; i < 26; i++) {
			childhood.put(i, new ArrayList<String>());
		}
//		System.out.println("EXCEPTION:" + childhood.size());
		adulthood = new ArrayList<String>();
		retirement=0;
	}
	
	public void addWord(String word){	
		childhood.get(word.length()).add(word);
	}
	
	public int getSize(){
		//the minus 1 is there because the initial word does not count
		return retirement-1;
	}
	
	void processSingleWord(final String word,int wordsStackSize){
		if(wordsStackSize<1 || wordsStackSize > 25){
			return;
		}
		
		
		ArrayList<String> childStack = childhood.get(wordsStackSize);
		
		int childSize =  childStack.size() ;
		
		for (int i = 0; i < childSize; i++) {
			String w = childStack.get(i);
			
			if(Lev.getLevenshteinDistance(word, w, 1) ==1){
				adulthood.add(childStack.remove(i));
				// when you remove someone from childhood, you need to: reduce childhood size, and look again in the same position
				childSize = childSize-1;
				i = i-1;
			}
		}
	}
	
	void friendsNetWork(String line){
		String word;
		int wordSize;
		adulthood.add(line);
		
		while(! adulthood.isEmpty()){
			word = adulthood.remove(0);
			wordSize = word.length();
			
			processSingleWord(word,wordSize-1);
			processSingleWord(word,wordSize);
			processSingleWord(word,wordSize+1);
			retirement++;
		}
	}
	
	public static void main(String[] args){
		long ini = System.currentTimeMillis();
		LehvensteinDistance ld = new LehvensteinDistance();
		String word = "hello";

		try{
//            File file = new File(args[0]);
			File file = new File("LehvensteinDistance.txt");
            BufferedReader in = new BufferedReader(new FileReader(file));

            String line;
            while ((line = in.readLine()) != null) {
            	ld.addWord(line);
            }
            
            ld.friendsNetWork(word);
            
            System.out.println(ld.getSize());
        }
        catch(Exception e){
        	System.out.println("EXCEPTION:" + e);
        }	
		System.out.println("time:" +  (System.currentTimeMillis() - ini));
	}

}

class Lev{
	
	public static int getLevenshteinDistance(CharSequence s, CharSequence t, final int threshold) {
	    if (s == null || t == null) {
	        throw new IllegalArgumentException("Strings must not be null");
	    }
	    if (threshold < 0) {
	        throw new IllegalArgumentException("Threshold must not be negative");
	    }

	    int n = s.length(); // length of s
	    int m = t.length(); // length of t

	    // if one string is empty, the edit distance is necessarily the length of the other
	    if (n == 0) {
	        return m <= threshold ? m : -1;
	    } else if (m == 0) {
	        return n <= threshold ? n : -1;
	    }

	    if (n > m) {
	        // swap the two strings to consume less memory
	        final CharSequence tmp = s;
	        s = t;
	        t = tmp;
	        n = m;
	        m = t.length();
	    }

	    int p[] = new int[n + 1]; // 'previous' cost array, horizontally
	    int d[] = new int[n + 1]; // cost array, horizontally
	    int _d[]; // placeholder to assist in swapping p and d

	    // fill in starting table values
	    final int boundary = Math.min(n, threshold) + 1;
	    for (int i = 0; i < boundary; i++) {
	        p[i] = i;
	    }
	    // these fills ensure that the value above the rightmost entry of our 
	    // stripe will be ignored in following loop iterations
	    Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
	    Arrays.fill(d, Integer.MAX_VALUE);

	    // iterates through t
	    for (int j = 1; j <= m; j++) {
	        final char t_j = t.charAt(j - 1); // jth character of t
	        d[0] = j;

	        // compute stripe indices, constrain to array size
	        final int min = Math.max(1, j - threshold);
	        final int max = Math.min(n, j + threshold);

	        // the stripe may lead off of the table if s and t are of different sizes
	        if (min > max) {
	            return -1;
	        }

	        // ignore entry left of leftmost
	        if (min > 1) {
	            d[min - 1] = Integer.MAX_VALUE;
	        }

	        // iterates through [min, max] in s
	        for (int i = min; i <= max; i++) {
	            if (s.charAt(i - 1) == t_j) {
	                // diagonally left and up
	                d[i] = p[i - 1];
	            } else {
	                // 1 + minimum of cell to the left, to the top, diagonally left and up
	                d[i] = 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
	            }
	        }

	        // copy current distance counts to 'previous row' distance counts
	        _d = p;
	        p = d;
	        d = _d;
	    }

	    // if p[n] is greater than the threshold, there's no guarantee on it being the correct
	    // distance
	    if (p[n] <= threshold) {
	        return p[n];
	    } else {
	        return -1;
	    }
	}

	
}

