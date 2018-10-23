// https://searchcode.com/api/result/13445500/

package mw.mapreduce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mw.mapreduce.core.MWJob;
import mw.mapreduce.core.MWMapper;
import mw.mapreduce.core.MWReducer;
import mw.mapreduce.jobs.friendcount.MWFriendCountComparator;
import mw.mapreduce.jobs.friendcount.MWFriendCountJob;
import mw.mapreduce.jobs.friendsort.MWFriendSortComparator;
import mw.mapreduce.jobs.friendsort.MWFriendSortJob;
import mw.mapreduce.jobs.friendsort.MWFriendsortKeyListValuePair;
import mw.mapreduce.jobs.friendcount.MWFriendcountKeyListValuePair;
import mw.mapreduce.jobs.friendextract.MWFriendExtractJob;

public class MWMapReduce {

	public static int nMapper = 1;
	public static int nReducer = 1;
	
	
	@SuppressWarnings("unchecked")
	protected void friendextract(String inFile, String tmpPrefix, String outPrefix) throws IOException {
		
		MWJob mfact = new MWFriendExtractJob();
		List<MWMapper<String, String, String, String>> mappers = new ArrayList<MWMapper<String, String, String, String>>();
		List<MWReducer<String, String, String, String>> reducers = new ArrayList<MWReducer<String, String, String, String>>();
		
		
		for (int i = 0; i < nMapper; i++) {
			mappers.add(mfact.createMapper(inFile, nMapper, i, tmpPrefix+"_"+Integer.toString(i)+".dat"));
		}
		
		// map-schritt...
		ExecutorService es = Executors.newFixedThreadPool(nMapper);
		for (int i = 0; i < nMapper; i++) {
			es.execute(mappers.get(i));
		}
		es.shutdown();
		
		while (!es.isTerminated());
		
		// MERGE PHASE \\
		PriorityQueue<MWFriendcountKeyListValuePair> queue = new PriorityQueue<MWFriendcountKeyListValuePair>(10, new MWFriendCountComparator());
		
		BufferedWriter merged = new BufferedWriter( new FileWriter("merged.dat"));
		BufferedReader[] files = new BufferedReader[nMapper];
		
		for (int i = 0; i < nMapper; i++) {
			files[i] = new BufferedReader(new FileReader(tmpPrefix+"_"+Integer.toString(i)+".dat"));
			queue.add(new MWFriendcountKeyListValuePair(i, files[i].readLine()));
		}
		String compareKey = new String();
		
		while (!queue.isEmpty()) {
			
			MWFriendcountKeyListValuePair tmp = queue.poll();
			
			// Merge two consecutive MWKeyListValuePairs...
			if (!compareKey.equals(tmp.key)) {
				// We want no empty first line
				if (!compareKey.isEmpty()) merged.write("\n");
				merged.write(tmp.key + "~!~");
			} 
			for (String str: tmp.values) {
				merged.write(str + "~!~");
			}
			compareKey = tmp.key;
			
			String line = files[tmp.fileID].readLine();
			if(line != null) {
				queue.add(new MWFriendcountKeyListValuePair(tmp.fileID, line));
			}
		}
		
		
		for (int i = 0; i < nMapper; i++) {
			files[i].close();
		}
		merged.close();
		
		// REDUCTION PHASE \\
		
		for (int i = 0; i < nReducer; i++) {
			reducers.add(mfact.createReducer("merged.dat", nReducer, i, outPrefix+"_"+Integer.toString(i)+".dat"));
		}
		
		// reduce-schritt...
		es = Executors.newFixedThreadPool(nReducer);
		for (int i = 0; i < nReducer; i++) {
			es.execute(reducers.get(i));
		}
		es.shutdown();
		
		while (!es.isTerminated());
		
		// merge results
		merged = new BufferedWriter( new FileWriter(outPrefix + "_FINAL.dat"));
		
		for (int i = 0; i < nReducer; i++) {
			BufferedReader br = new BufferedReader(new FileReader(outPrefix+"_"+Integer.toString(i)+".dat"));
			
			while (true) {
				String line = br.readLine();
				if (line == null) break;
				String[] sa = line.split("~!~");
				if (!(sa.length == 2)) {
					System.out.println("Irgendwo ist sa.length != 2");
					continue;
				}
				merged.write(sa[0] + "\t" + sa[1] + "\n");
			}
			br.close();
		}
		
		merged.close();
	
	}
	
	@SuppressWarnings("unchecked")
	protected void friendcount(String inFile, String tmpPrefix, String outPrefix) throws IOException {

		MWJob mfact = new MWFriendCountJob();
		List<MWMapper<String, String, String, String>> mappers = new ArrayList<MWMapper<String, String, String, String>>();
		List<MWReducer<String, String, String, String>> reducers = new ArrayList<MWReducer<String, String, String, String>>();
		
		
		for (int i = 0; i < nMapper; i++) {
			mappers.add(mfact.createMapper(inFile, nMapper, i, tmpPrefix+"_"+Integer.toString(i)+".dat"));
		}
		
		// map-schritt...
		ExecutorService es = Executors.newFixedThreadPool(nMapper);
		for (int i = 0; i < nMapper; i++) {
			es.execute(mappers.get(i));
		}
		es.shutdown();
		
		while (!es.isTerminated());
		
		// MERGE PHASE \\
		PriorityQueue<MWFriendcountKeyListValuePair> queue = new PriorityQueue<MWFriendcountKeyListValuePair>(10, new MWFriendCountComparator());
		
		BufferedWriter merged = new BufferedWriter( new FileWriter("merged.dat"));
		BufferedReader[] files = new BufferedReader[nMapper];
		
		for (int i = 0; i < nMapper; i++) {
			files[i] = new BufferedReader(new FileReader(tmpPrefix+"_"+Integer.toString(i)+".dat"));
			queue.add(new MWFriendcountKeyListValuePair(i, files[i].readLine()));
		}
		String compareKey = new String();
		
		while (!queue.isEmpty()) {
			
			MWFriendcountKeyListValuePair tmp = queue.poll();
			
			// Merge two consecutive MWKeyListValuePairs...
			if (!compareKey.equals(tmp.key)) {
				// We want no empty first line
				if (!compareKey.isEmpty()) merged.write("\n");
				merged.write(tmp.key + "~!~");
			} 
			int cnt = 0;
			for (String str: tmp.values) {
				if (cnt < tmp.values.size())
					merged.write(str + "~!~");
				else
					merged.write(str);
				cnt++;
			}
		
			compareKey = tmp.key;
			
			String line = files[tmp.fileID].readLine();
			if(line != null) {
				queue.add(new MWFriendcountKeyListValuePair(tmp.fileID, line));
			}
		}
		
		
		for (int i = 0; i < nMapper; i++) {
			files[i].close();
		}
		merged.close();
		
		// REDUCTION PHASE \\
		
		for (int i = 0; i < nReducer; i++) {
			reducers.add(mfact.createReducer("merged.dat", nReducer, i, outPrefix+"_"+Integer.toString(i)+".dat"));
		}
		
		// reduce-schritt...
		es = Executors.newFixedThreadPool(nReducer);
		for (int i = 0; i < nReducer; i++) {
			es.execute(reducers.get(i));
		}
		es.shutdown();
		
		while (!es.isTerminated());
		
		// merge results
		merged = new BufferedWriter( new FileWriter(outPrefix + "_FINAL.dat"));
		
		for (int i = 0; i < nReducer; i++) {
			BufferedReader br = new BufferedReader(new FileReader(outPrefix+"_"+Integer.toString(i)+".dat"));
			
			while (true) {
				String line = br.readLine();
				if (line == null) break;
				String[] sa = line.split("~!~");
				
				assert (sa.length == 2);
				merged.write(sa[0] + "\t" + sa[1] + "\n");
			}
			br.close();
		}
		
		merged.close();
	}
	
	@SuppressWarnings("unchecked")
	protected void friendsort(String inFile, String tmpPrefix, String outPrefix) throws IOException {
		
		MWJob mfact = new MWFriendSortJob();
		List<MWMapper<String, String, String, String>> mappers = new ArrayList<MWMapper<String, String, String, String>>();
		List<MWReducer<String, String, String, String>> reducers = new ArrayList<MWReducer<String, String, String, String>>();
		
		
		for (int i = 0; i < nMapper; i++) {
			mappers.add(mfact.createMapper(inFile, nMapper, i, tmpPrefix+"_"+Integer.toString(i)+".dat"));
		}
		
		// map-schritt...
		ExecutorService es = Executors.newFixedThreadPool(nMapper);
		for (int i = 0; i < nMapper; i++) {
			es.execute(mappers.get(i));
		}
		es.shutdown();
		
		while (!es.isTerminated());
		
		// MERGE PHASE \\
		PriorityQueue<MWFriendsortKeyListValuePair> queue = new PriorityQueue<MWFriendsortKeyListValuePair>(10, new MWFriendSortComparator());
		
		BufferedWriter merged = new BufferedWriter( new FileWriter("merged.dat"));
		BufferedReader[] files = new BufferedReader[nMapper];
		
		for (int i = 0; i < nMapper; i++) {
			files[i] = new BufferedReader(new FileReader(tmpPrefix+"_"+Integer.toString(i)+".dat"));
			queue.add(new MWFriendsortKeyListValuePair(i, files[i].readLine()));
		}
		int compareKey = -1;
		
		while (!queue.isEmpty()) {
			
			MWFriendsortKeyListValuePair tmp = queue.poll();
			
			// Merge two consecutive MWKeyListValuePairs...
			if(compareKey != tmp.key) {
				// We want no empty first line
				if( compareKey != -1) merged.write("\n");
				merged.write(tmp.key + "~!~");
			} 
			for(String str: tmp.values) {
				merged.write(str + "~!~");
			}
			compareKey = tmp.key;
			
			String line = files[tmp.fileID].readLine();
			if(line != null) {
				queue.add(new MWFriendsortKeyListValuePair(tmp.fileID, line));
			}
		}
		
		
		for (int i = 0; i < nMapper; i++) {
			files[i].close();
		}
		merged.close();
		
		// REDUCTION PHASE \\
		
		for (int i = 0; i < nReducer; i++) {
			reducers.add(mfact.createReducer("merged.dat", nReducer, i, outPrefix+"_"+Integer.toString(i)+".dat"));
		}
		
		// reduce-schritt...
		es = Executors.newFixedThreadPool(nReducer);
		for (int i = 0; i < nReducer; i++) {
			es.execute(reducers.get(i));
		}
		es.shutdown();
		
		while (!es.isTerminated());
		
		// merge results
		merged = new BufferedWriter( new FileWriter(outPrefix + "_FINAL.dat"));
		
		for (int i = 0; i < nReducer; i++) {
			BufferedReader br = new BufferedReader(new FileReader(outPrefix+"_"+Integer.toString(i)+".dat"));
			
			while (true) {
				String line = br.readLine();
				if (line == null) break;
				String[] sa = line.split("~!~");
				assert (sa.length == 2);
				merged.write(sa[0] + "\t" + sa[1] + "\n");
			}
			br.close();
		}
		
		merged.close();
		
	}
	
	public static void main(String[] args) throws IOException {
		
		if (args.length != 4) {
			System.err.println("Richtig aufrufen!");
			return;
		}
		
		MWMapReduce mapreduce = new MWMapReduce();

		if (args[0].equals("friendsort")) {
			mapreduce.friendsort(args[1], args[2], args[3]);
		} else if (args[0].equals("friendextract")) {
			mapreduce.friendextract(args[1], args[2], args[3]);
		} else if (args[0].equals("friendcount")) {
			mapreduce.friendcount(args[1], args[2], args[3]);
		} else {
			System.err.println(args[0] + " gibts nicht");
			return;
		}
		
	}

}

