// https://searchcode.com/api/result/12454352/

package tools.kmer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import tools.aligning.LocalMutation;
import tools.aligning.LocalMutationList;
import tools.blast.blastM8Alignment;
import tools.blast.blastM8Parser;
import tools.fasta.FastaSeq;
import tools.fasta.fastaParser;
import tools.fastq.FastqSeq;
import tools.fastq.fastqParser;
import tools.gff.gffLine;
import tools.kmer.KmerSet_binary;
import tools.shoreMap.Mutation;
import tools.shoreMap.ShoreMapLine;

public class kmerUtils {

	private static HashMap<String, String> methodsHelp= new HashMap<String, String>();
	private final static String sep="\t";
	private final static String[] nucleotides= new String[]{"A","C","G","T","N"};
	
	public static void main(String[] args)throws Exception{
		//load helpMap
		methodsHelp.put("sortKmers", "sortKmers - sorts the kmers\n\targs = <kmer file> <kmer position> <bin size> <tmpPrefix>\n");
		methodsHelp.put("sortKmersLink", "sortKmersLink - sorts the kmers and adds a flag if it is linked to the previous read\n\targs = <kmer file> <kmer position> <max jump>\n");
		methodsHelp.put("generateKmers", "generateKmers - generates all kmers of length n\n\targs = <n>\n");
		methodsHelp.put("generateSeeds", "generateSeeds - generates seeds from a kmer file\n\targs = <kmerFile> <kmer position> <min seed size> <outPrefix>\n");
		methodsHelp.put("generateSeeds2", "generateSeeds2 - generates seeds from a kmer file and the contrast file\n\targs = <kmerFile> <contrast kmerFile> <kmer position> <min seed size> <outPrefix>\n");
		methodsHelp.put("generateSeeds3", "generateSeeds3 - generates seeds from a kmer file and the contrast file. finding starting points with l-mers\n\targs = <kmerFile> <contrast kmerFile> <kmer position> <min seed size> <lmerSize> <outPrefix>\n");
		methodsHelp.put("mapListMutationKmers", "mapListMutationKmers - takes a map.list and a list of mutations (Chr\\tposition\\tnewNuc) and prints two files, one with the kmers of size n to remove and one with the ones to add\n\targs = <map.list> <mutation file> <kmer size n> <outPrefix>\n");
		methodsHelp.put("kmerize", "kmerize - prints all kmers of size n in each sequence\n\targs = <fastqFile> <n>\n");
		methodsHelp.put("kmerizeEnd", "kmerizeEnd - prints the kmers in the end of a read missing when reducing from n to m\n\targs = <fastqFile> <n> <m>\n");
		methodsHelp.put("reduce", "reduce - reduces the kmers at pos in a file to length n\n\targs = <fastqFile> <pos> <n>\n");
		methodsHelp.put("extractGood", "extractGood - Takes a kmerFile with the kmers at pos (column count starts at 0), and extract all fastq seqs that contains at least min of the good kmers\n\targs = <kmerFile> <pos> <fastqFile> <min> <outPrefix>\n");
		methodsHelp.put("checkEnds", "checkEnds - Takes two seed files. Builds a hash with kmer-length n for both end and beginning for the first file and checks which kmers from the second file that matches this\n\targs = <kmerFile1> <kmerFile2> <kmer length> \n");
		methodsHelp.put("multiKmerMergePipe", "multiKmerMergePipe - takes a pipe of multiple sorted kmer-files and writes a table\n\targs = <number of samples>\n");
		methodsHelp.put("multiKmerMergeJellyfishPipe", "multiKmerMergeJellyfishPipe - takes a pipe of multiple tagged jellyfish kmer-files and writes a table\n\targs = <number of samples>\n");
		methodsHelp.put("blastSNPFilterPipe", "blastSNPFilterPipe - takes the output from blastn -outfmt '6 std qseq sseq' and filters for paired seeds\n\targs = <kmerSize> <tolerance>\n");
		methodsHelp.put("blastRevCompFilterPipe", "blastRevCompFilterPipe - takes the output from blastn -outfmt '6 std qlen slen' and filters for identical seeds. Prints the cleaned fasta file\n\targs = <fasta file>\n");
		methodsHelp.put("blastTransferAnnotationPipe", "blastTransferAnnotationPipe - takes the output from blastn -outfmt '6 std qlen slen' and the output from the SNP/InDel Filters (or another mutation annotation script) and Prints the transfered annotation\n\targs = <annotation file> <mutation column=12>\n");
		methodsHelp.put("blastTransferAnnotationToChrPipe", "blastTransferAnnotationToChrPipe - takes the output from blastn -outfmt '6 std' and the output from the SNP/InDel Filters (or another mutation annotation script) and Prints the transfered annotation\n\targs = <annotation file> <mutation column>\n");
		methodsHelp.put("blastMutationEMSAnnotationPipe", "blastMutationEMSAnnotationPipe - Adds a string of mutationDirections for EMS mutations\n\targs = <annotated blast file>\n");
		methodsHelp.put("blastRemoveRepetitive", "blastRemoveRepetitive(Z) - Takes two blast files from blastSNPFilterPipe and prints only those that are unique (only one hit in each) \n\targs = <annotated blast file1> <annotated blast file2> <out1> <out2>\n");
		methodsHelp.put("blastSeedExtensionPairingPipe", "blastSeedExtensionPairingPipe - takes the output from blastn -outfmt '6 std qlen slen' and a perfect match. If none exists the old sequence is printed. \n\targs = <query fasta file> <target fasta file>\n");
		methodsHelp.put("blastMutatedHits", "blastMutatedHits - takes the output from blastn -outfmt '6 std' and a file with the mutation annotation. Prints all hits that contains a mutation given the flexibility f. \n\targs = <blast file> <mutation annotation file> <flexibility>\n");
		methodsHelp.put("getGFF3lines", "getGFF3lines - prints lines overlapping the positions in column 2 (chr) and 3 (pos) of the input file. \n\targs = <position file> <GFF3 file>\n");
		
		//check which method to run
		if(args.length>0){
			if(args[0].equals("sortKmers")&&args.length==5){
				sortKmers(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]),args[4]);
			}else if(args[0].equals("sortKmersLink")&&args.length==4){
				sortKmersLink(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]));
			}else if(args[0].equals("generateKmers")&&args.length==2){
				generateKmers(Integer.parseInt(args[1]),"");
			}else if(args[0].equals("generateSeeds")&&args.length==5){
				generateSeeds(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]),args[4]);
			}else if(args[0].equals("generateSeeds2")&&args.length==6){
				generateSeeds2(args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),args[5]);
			}else if(args[0].equals("generateSeeds3")&&args.length==7){
				generateSeeds3(args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),Integer.parseInt(args[5]),args[6]);
			}else if(args[0].equals("mapListMutationKmers")&&args.length==5){
				mapListMutationKmers(args[1],args[2],Integer.parseInt(args[3]),args[4]);
			}else if(args[0].equals("reduce")&&args.length==4){
				reduce(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]));
			}else if(args[0].equals("kmerize")&&args.length==3){
				kmerize(args[1],Integer.parseInt(args[2]));
			}else if(args[0].equals("kmerizeEnd")&&args.length==4){
				kmerizeEnd(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]));
			}else if(args[0].equals("extractGood")&&args.length==6){
				extractGood(readKmers_revComp(args[1],Integer.parseInt(args[2])),args[3],Integer.parseInt(args[4]),args[5]);
			}else if(args[0].equals("checkEnds")&&args.length==4){
				checkEnds(args[1],args[2],Integer.parseInt(args[3]));
			}else if(args[0].equals("multiKmerMergePipe")&&args.length>1){
				multiKmerMergePipe(Integer.parseInt(args[1]));
			}else if(args[0].equals("multiKmerMergeJellyfishPipe")&&args.length>1){
				multiKmerMergeJellyfishPipe(Integer.parseInt(args[1]));
			}else if(args[0].equals("blastMutatedHits")&&args.length>3){
				blastMutationHits(new BufferedReader(new FileReader(args[1])),args[2],Integer.parseInt(args[3]));
			}else if(args[0].equals("blastSNPFilterPipe")&&args.length>2){
				blastSNPFilterPipe(new BufferedReader(new InputStreamReader(System.in)),Integer.parseInt(args[1]),Integer.parseInt(args[2]));
			}else if(args[0].equals("blastRemoveRepetitive")&&args.length>4){
				blastRemoveRepetitive(new BufferedReader(new FileReader(args[1])),new BufferedReader(new FileReader(args[2])),args[3],args[4]);
			}else if(args[0].equals("blastRemoveRepetitiveZ")&&args.length>4){
				BufferedReader in1=new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args[1]))));
				BufferedReader in2=new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args[2]))));
				blastRemoveRepetitive(in1,in2,args[3],args[4]);
			}else if(args[0].equals("blastRevCompFilterPipe")&&args.length>1){
				blastRevCompFilterPipe(new BufferedReader(new InputStreamReader(System.in)),args[1]);
			}else if(args[0].equals("blastSeedExtensionPairingPipe")&&args.length>1){
				blastSeedExtensionPairingPipe(new BufferedReader(new InputStreamReader(System.in)),args[1],args[2]);
			}else if(args[0].equals("blastTransferAnnotationPipe")&&args.length>1){
				blastTransferAnnotationPipe(new BufferedReader(new InputStreamReader(System.in)),args[1],12);
			}else if(args[0].equals("blastTransferAnnotationPipe")&&args.length>2){
				blastTransferAnnotationPipe(new BufferedReader(new InputStreamReader(System.in)),args[1],Integer.parseInt(args[2]));
			}else if(args[0].equals("blastTransferAnnotationToChrPipe")&&args.length>2){
				blastTransferAnnotationToChrPipe(new BufferedReader(new InputStreamReader(System.in)),args[1],Integer.parseInt(args[2]));
			}else if(args[0].equals("blastMutationEMSAnnotationPipe")&&args.length>0){
				blastMutationEMSAnnotationPipe(new BufferedReader(new InputStreamReader(System.in)));
			}else if(args[0].equals("getGFF3lines")&&args.length>2){
				getGFF3lines(args[1],args[2]);
			}else if(args[0].equals("method2")&&args.length>1){
				
			}else{
				System.err.println(printHelp(args[0]));
			}
		}else{
			System.err.println(printHelp());
		}
	}
	
	public static void getGFF3lines(String positionFile,String gffFile)throws Exception{
		HashMap<String, HashMap<Integer, ArrayList<String>>> positions= new HashMap<String, HashMap<Integer,ArrayList<String>>>();
		HashMap<Integer, ArrayList<String>> posTmp;
		ArrayList<String> names;
		gffLine gl;
		
		BufferedReader in= new BufferedReader(new FileReader(positionFile));
		String[] l;
		int pos;
		
		for(String s=in.readLine();s!=null;s=in.readLine()){
			l=s.split("\t");
			if(positions.containsKey(l[1])){
				posTmp= positions.get(l[1]);
			}else{
				posTmp= new HashMap<Integer, ArrayList<String>>();
				positions.put(l[1], posTmp);
			}
			pos=Integer.parseInt(l[2]);
			if(posTmp.containsKey(pos)){
				names=posTmp.get(pos);
			}else{
				names= new ArrayList<String>();
				posTmp.put(pos, names);
			}
			names.add(l[0]);
		}
		in.close();
		in= new BufferedReader(new FileReader(gffFile));
		String s=in.readLine();
		while(s.startsWith("#")){
			s=in.readLine();
		}
		gl=new gffLine(s);
		if(gl.getChr().startsWith("Chr") && !positions.containsKey(gl.getChr())){
			//shift the keys of the positions hash
			//will not work for chloroplast and such
			names= new ArrayList<String>(positions.keySet());
			for(String chr : names){
				posTmp=positions.remove(chr);
				positions.put("Chr"+chr, posTmp);
			}
		}
		
		
		
		for(;s!=null;s=in.readLine()){
			if(!s.startsWith("#")){
				gl=new gffLine(s);
				if(positions.containsKey(gl.getChr())){
					posTmp=positions.get(gl.getChr());
					//ugly coding ahead
					for (Integer position : posTmp.keySet()) {
						if(gl.containsPos(position)){
							for(String name : posTmp.get(position)){
								System.out.println(name+sep+gl.toString());
							}
						}
					}
				}
			}
		}
	}
	
	public static void blastMutationEMSAnnotationPipe(BufferedReader in)throws Exception{
		String[] l;
		LocalMutationList lml;
		
		
		for(String s=in.readLine();s!=null;s=in.readLine()){
			l=s.split("\t");
			lml= new LocalMutationList(l[1]);
			System.out.println(s+"\t"+lml.mutationListEMS());
		}
	}
	
	public static void blastMutationHits(BufferedReader blast, String annotationFile, int flexibility) throws Exception{
		BufferedReader in= new BufferedReader(new FileReader(annotationFile));
		HashMap<String, LocalMutationList> mutations= new HashMap<String, LocalMutationList>();
		blastM8Parser bp= new blastM8Parser(blast);
		blastM8Alignment ba;
		LocalMutationList lml;
		String[] l;
		for(String s=in.readLine();s!=null;s=in.readLine()){
			l=s.split("\t");
			mutations.put(l[0], new LocalMutationList(l[1]));
		}
		for(;bp.hasMore();){
			ba=bp.nextHit();
			if(mutations.containsKey(ba.qname)){
				lml=mutations.get(ba.qname);
				if(ba.qstart<ba.qend){
					if(lml.encapsulated(ba.qstart-flexibility, ba.qend+flexibility)){
						System.out.println(ba);
					}
				}else{
					if(lml.encapsulated(ba.qend-flexibility, ba.qstart+flexibility)){
						System.out.println(ba);
					}
				}
			}
		}
	}
	
	public static void blastSeedExtensionPairingPipe(BufferedReader in, String queryFastaFile, String targetFastaFile) throws Exception{
		HashMap<String, FastaSeq> querySeqs= new HashMap<String, FastaSeq>();
		fastaParser fp= new fastaParser(new BufferedReader(new FileReader(queryFastaFile)));
		FastaSeq fs;
		blastM8Alignment ba=new blastM8Alignment(),bestHit=new blastM8Alignment();
		HashMap<String,String> bestHits=new HashMap<String,String>();
		HashSet<String> printed= new HashSet<String>();
		int length=0,start1,start2,end1,end2,edgeCount,qlen,slen;
		String qname="";
		String[] l;
		boolean found=false;
		for (;fp.hasNext();){
			fs=fp.next();
			querySeqs.put(fs.getQname(), fs);
		}
		for(String s=in.readLine();s!=null;s=in.readLine()){
			ba=new blastM8Alignment(s);
			if(!qname.equals(ba.qname)){
				if(found){
					//save for later
					bestHits.put(bestHit.tname,bestHit.qname);
				}else if(length>0){
					System.out.println(querySeqs.get(ba.qname));
				}
				found=false;
				length=querySeqs.get(ba.qname).length();
				qname=ba.qname;
			}
			
			l=s.split("\t");
			qlen=Integer.parseInt(l[12]);
			slen=Integer.parseInt(l[13]);
			
			if(ba.identity==100 && slen>qlen && ba.alignment_length>qlen/2){
				
				if(ba.qstart<ba.qend){
					start1=ba.qstart;
					end1=ba.qend;
				}else{
					start1=ba.qend;
					end1=ba.qstart;
				}
				if(ba.tstart<ba.tend){
					start2=ba.tstart;
					end2=ba.tend;
				}else{
					start2=ba.tend;
					end2=ba.tstart;
				}
				edgeCount=0;
				if(end2==slen){
					++edgeCount;
				}
				if(start2==1){
					++edgeCount;
				}
				if(end1<qlen){
					--edgeCount;
				}
				if(start1>1){
					--edgeCount;
				}
				
				
				if (edgeCount>=0) {
					if (!found) {
						bestHit = new blastM8Alignment(ba);
						found = true;
					} else if (ba.e_value < bestHit.e_value) {
						//shouldn't happen???
						bestHit = new blastM8Alignment(ba);
					}
				}
			}
		}
		if(found){
			//besthit found in the last row
			bestHits.put(bestHit.tname,bestHit.qname);
		}
		
		
		fp= new fastaParser(new BufferedReader(new FileReader(targetFastaFile)));
		for(;fp.hasNext();){
			fs=fp.next();
			if(bestHits.containsKey(fs.getQname())){
				System.out.println(">"+bestHits.get(fs.getQname())+" "+fs.getQname()+"\n"+fs.getSeq());
				printed.add(bestHits.get(fs.getQname()));
			}
		}
		//print the original seed for the seeds without extension
		for (FastaSeq out : querySeqs.values()) {
			if(!printed.contains(out.getQname())){
				System.out.println(out);
			}
		}
	}
	
	public static void blastRemoveRepetitive(BufferedReader in1 ,BufferedReader in2,String outFile1,String outFile2)throws Exception{
		HashMap<String, String> set1= new HashMap<String, String>();
		HashSet<String> rep1= new HashSet<String>();
		BufferedWriter out1= new BufferedWriter(new FileWriter(outFile1)), out2= new BufferedWriter(new FileWriter(outFile2));
		blastM8Alignment ba;
		String repline,line,qname="",tname;
		boolean repetitive=false;
		
		for(String s=in1.readLine();s!=null;s=in1.readLine()){
			ba= new blastM8Alignment(s);
			if(!rep1.contains(ba.qname)){
				if(set1.containsKey(ba.qname)){
					set1.remove(ba.qname);
					rep1.add(ba.qname);
				}else{
					set1.put(ba.qname, s);
				}
			}
		}
		in1.close();
		line=in2.readLine();
		repline=line;
		qname=line.split("\t")[0];
		
		for(;line!=null;line=in2.readLine()){
			ba=new blastM8Alignment(line);
			if(qname.equals(ba.qname)){
				repetitive=true;
			}else{
				if(!repetitive){
					//check other direction
					tname=repline.split("\t")[1];
					if(!rep1.contains(tname)){
						out2.write(repline+"\n");
						if(set1.containsKey(tname)){
							out1.write(set1.get(tname)+"\n");
						}
					}
				}
				//restart
				repetitive=false;
				qname=ba.qname;
				repline=line;
			}
		}
		
		if(!repetitive){
			//check other direction
			tname=repline.split("\t")[1];
			if(!rep1.contains(tname)){
				out2.write(repline+"\n");
				if(set1.containsKey(tname)){
					out1.write(set1.get(tname)+"\n");
				}
			}
		}
		
		out1.close();
		out2.close();
	}
	
	public static void blastTransferAnnotationPipe(BufferedReader in, String ablastFile, final int mutPos)throws Exception{
		blastM8Alignment ba;
		BufferedReader blast= new BufferedReader(new FileReader(ablastFile));
		HashMap<String, LocalMutationList> mutations= new HashMap<String, LocalMutationList>();
		LocalMutationList lml;
		int start1,start2,end1,end2,edgeCount,qlen,slen;
		String[] l;
		//Gather mutations
		for(String s=blast.readLine();s!=null;s=blast.readLine()){
			l=s.split("\t");
			mutations.put(l[0], new LocalMutationList(l[mutPos]));
		}
		blast.close();
		//Parse new file
		for(String s=in.readLine();s!=null;s=in.readLine()){
			ba= new blastM8Alignment(s);
			if(ba.qname.equals(ba.tname) && mutations.containsKey(ba.tname) && ba.identity==100){
				//count edges
				l=s.split("\t");
				qlen=Integer.parseInt(l[12]);
				slen=Integer.parseInt(l[13]);
				if(ba.qstart<ba.qend){
					start1=ba.qstart;
					end1=ba.qend;
				}else{
					start1=ba.qend;
					end1=ba.qstart;
				}
				if(ba.tstart<ba.tend){
					start2=ba.tstart;
					end2=ba.tend;
				}else{
					start2=ba.tend;
					end2=ba.tstart;
				}
				edgeCount=0;
				if(end2==slen){
					++edgeCount;
				}
				if(start2==1){
					++edgeCount;
				}
				if(end1<qlen){
					--edgeCount;
				}
				if(start1>1){
					--edgeCount;
				}

				
				if (edgeCount>=0) {
					//shift mutations
					lml = mutations.get(ba.tname);
					if (ba.strand[0] == '-') {
						lml.shiftRevComp(ba.tend + ba.qstart);
					} else {
						lml.shift(ba.tstart - ba.qstart);
					}
					System.out.println(ba.tname + "\t" + lml);
				}
				
			}
		}
	}
	
	
	public static void blastTransferAnnotationToChrPipe(BufferedReader in, String ablastFile, final int mutPos)throws Exception{
		blastM8Alignment ba;
		BufferedReader blast= new BufferedReader(new FileReader(ablastFile));
		HashMap<String, LocalMutationList> mutations= new HashMap<String, LocalMutationList>();
		LocalMutationList lml;
		boolean contains;
//		int start1,start2,end1,end2,edgeCount,qlen,slen;
		String[] l;
		String chrName;
		//Gather mutations
		for(String s=blast.readLine();s!=null;s=blast.readLine()){
			l=s.split("\t");
			mutations.put(l[0], new LocalMutationList(l[mutPos]));
		}
		blast.close();
		//Parse new file
		for(String s=in.readLine();s!=null;s=in.readLine()){
			ba= new blastM8Alignment(s);
			if(mutations.containsKey(ba.qname)){
				//count edges
//				l=s.split("\t");
//				qlen=Integer.parseInt(l[12]);
//				slen=Integer.parseInt(l[13]);
//				if(ba.qstart<ba.qend){
//					start1=ba.qstart;
//					end1=ba.qend;
//				}else{
//					start1=ba.qend;
//					end1=ba.qstart;
//				}
//				if(ba.tstart<ba.tend){
//					start2=ba.tstart;
//					end2=ba.tend;
//				}else{
//					start2=ba.tend;
//					end2=ba.tstart;
//				}
//				edgeCount=0;
//				if(end2==slen){
//					++edgeCount;
//				}
//				if(start2==1){
//					++edgeCount;
//				}
//				if(end1<qlen){
//					--edgeCount;
//				}
//				if(start1>1){
//					--edgeCount;
//				}
//
//				
//				if (edgeCount>=0) {
				//shift mutations
				lml = new LocalMutationList(mutations.get(ba.qname));
				contains=false;
				for(LocalMutation lm : lml){
					if(ba.qstart<=lm.getPosition() && ba.qend>=lm.getPosition()){
						contains=true;
						break;
					}
				}
				
				if (contains) {
					if (ba.strand[0] == '-') {
						lml.shiftRevComp(ba.tend + ba.qstart);
					} else {
						lml.shift(ba.tstart - ba.qstart);
					}
					if(ba.tname.startsWith("Chr")){
						chrName= ba.tname.substring(3);
					}else{
						chrName= ba.tname;
					}
					for (LocalMutation lm : lml) {
						System.out.println(ba.qname + "\t" + chrName + "\t"
								+ lm.getPosition() + "\t" + lm.getRef() + "\t"
								+ lm.getMutation() +"\t1\t1\t1\t1\t1");
					}
				}
				
//				}
				
			}
		}
	}

	public static void blastRevCompFilterPipe(BufferedReader in, String faFile)throws Exception{
		blastM8Alignment ba;
		String[] l;
		String qname;
		boolean linkPrinted;
		int qlen,slen;
		HashSet<String> printed= new HashSet<String>();
		HashMap<String, HashSet<String>> links= new HashMap<String, HashSet<String>>();
		for(String s=in.readLine();s!=null;s=in.readLine()){
			ba= new blastM8Alignment(s);
			if(ba.identity==100 && !ba.qname.equals(ba.tname)){
				l=s.split("\t");
				qlen=Integer.parseInt(l[12]);
				slen=Integer.parseInt(l[13]);
				if(qlen==slen && ba.alignment_length==qlen){
					if(!links.containsKey(ba.qname)){
						links.put(ba.qname, new HashSet<String>());
					}
					links.get(ba.qname).add(ba.tname);
					if(!links.containsKey(ba.tname)){
						links.put(ba.tname, new HashSet<String>());
					}
					links.get(ba.tname).add(ba.qname);
				}
			}
		}
		
		fastaParser fp= new fastaParser(new BufferedReader(new FileReader(faFile)));
		FastaSeq fs;
		for(;fp.hasNext();){
			fs=fp.next();
			qname=fs.getQname();
			if(links.containsKey(qname)){
				linkPrinted=false;
				for(String name : links.get(qname)){
					if(printed.contains(name)){
						linkPrinted=true;
						break;
					}
				}
				
				if(linkPrinted){
					if(printed.add(qname)){
						System.out.println(fs.toString());
					}
				}else{
					printed.add(qname);
				}
			}else if(printed.add(qname)){
				System.out.println(fs.toString());
			}
		}
	}
	
	public static void blastSNPFilterPipe(BufferedReader in,final int kmerSize, final int tolerance)throws Exception{
		blastM8Alignment ba;
		String[] l;
		char[] qseq,sseq;
		LocalMutationList ml= new LocalMutationList();
		final int lengthCutoff= 2*(kmerSize-1)-tolerance-1;
//		System.out.println("#lengthCutoff: "+lengthCutoff);
//		int pos;
		for(String s=in.readLine();s!=null;s=in.readLine()){
			ba= new blastM8Alignment(s);
//			System.out.println("#" +s);
//			System.out.println("#filter-values: "+(ba.alignment_length-ba.mismatches)+"\t"+ba.mismatches+"\t"+ba.gap_openings);
			if(ba.alignment_length-ba.mismatches>lengthCutoff && ba.mismatches>0 && ba.gap_openings==0){
				//alignment contains mismatches and no gaps. The alignment is long enough to harbor these changes
				l=s.split("\t");
				qseq=l[12].toCharArray();
				sseq=l[13].toCharArray();
				ml.clear();
				for(int i=0;i<ba.alignment_length;++i){
					if(qseq[i]!=sseq[i]){
						ml.add(new LocalMutation(i+1,sseq[i],qseq[i]));
					}
				}
//				System.out.println("#nr of mismatches: "+mismatches.size());
				if(ml.get(0).getPosition()+(ba.alignment_length-ml.get(ml.size()-1).getPosition())>lengthCutoff){
					System.out.println(ba+"\t"+ml);
				}
			}
		}
	}
	
	public static void multiKmerMergePipe(final int size)throws Exception{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		ArrayList<String> samples= new ArrayList<String>(size);
		HashMap<String, String> counts= new HashMap<String, String>(size);
		final String zero=sep+"0";
		String s=in.readLine();
		String[] l=s.split("\t");
		String kmer=l[1];
		samples.add(l[2]);
		counts.put(l[2], l[0]);
		//pre-load
		for(s=in.readLine();s!=null;s=in.readLine()){
			l=s.split("\t");
			if(kmer.equals(l[1])){
				//store
				if(!samples.contains(l[2])){
					samples.add(l[2]);
				}
				counts.put(l[2], l[0]);
			}else{
				//print
				System.out.print(kmer);
				for (String sample : samples) {
					if(counts.containsKey(sample)){
						System.out.print(sep+counts.get(sample));
					}else{
						System.out.print(zero);
					}
				}
				for(int i=samples.size();i<size;++i){
					System.out.print(zero);
				}
				System.out.println();
				counts.clear();
				kmer=l[1];
				if(!samples.contains(l[2])){
					samples.add(l[2]);
				}
				counts.put(l[2], l[0]);
				if(samples.size()==size){
					break;
				}
			}
		}
		//main loop
		for(s=in.readLine();s!=null;s=in.readLine()){
			l=s.split("\t");
			if(kmer.equals(l[1])){
				counts.put(l[2], l[0]);
			}else{
				//print
				System.out.print(kmer);
				for (String sample : samples) {
					if(counts.containsKey(sample)){
						System.out.print(sep+counts.get(sample));
					}else{
						System.out.print(zero);
					}
				}
				System.out.println();
				counts.clear();
				kmer=l[1];
				counts.put(l[2], l[0]);
			}
		}
		//flush the last kmer
		System.out.print(kmer);
		for (String sample : samples) {
			if(counts.containsKey(sample)){
				System.out.print(sep+counts.get(sample));
			}else{
				System.out.print(zero);
			}
		}
		System.out.println();
	}

	public static void multiKmerMergeJellyfishPipe(final int size)throws Exception{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		HashMap<String, String[]> kmers= new HashMap<String, String[]>();

		final String zero=sep+"0";
		String s=in.readLine();
		String[] l=s.split("\t"),pair= new String[size];
		String cur=l[1];
		pair[Integer.parseInt(l[3])]=l[0];
		kmers.put(l[2], pair);
		
		for(s=in.readLine();s!=null;s=in.readLine()){
			l=s.split("\t");
			if(cur.equals(l[1])){
				if(kmers.containsKey(l[2])){
					kmers.get(l[2])[Integer.parseInt(l[3])]=l[0];
				}else{
					pair= new String[size];
					pair[Integer.parseInt(l[3])]=l[0];
					kmers.put(l[2], pair);
				}
			}else{
				//print
				for(String kmer : kmers.keySet()){
					pair=kmers.get(kmer);
					System.out.print(kmer);
					for(int i=0;i<size;++i){
						if(pair[i]==null){
							System.out.print(zero);
						}else{
							System.out.print(sep+pair[i]);
						}
					}
					System.out.println();
				}
				//restart
				kmers.clear();
				cur=l[1];
				pair= new String[size];
				pair[Integer.parseInt(l[3])]=l[0];
				kmers.put(l[2], pair);
			}
		}
		for(String kmer : kmers.keySet()){
			pair=kmers.get(kmer);
			System.out.print(kmer);
			for(int i=0;i<size;++i){
				if(pair[i]==null){
					System.out.print(zero);
				}else{
					System.out.print(sep+pair[i]);
				}
				System.out.println();
			}

		}
	}

	
	public static void checkEnds(String kmerFile1, String kmerFile2, final int length)throws Exception{
		HashMap<String, ArrayList<String[]>> starts= new HashMap<String, ArrayList<String[]>>(),ends= new HashMap<String, ArrayList<String[]>>();
		ArrayList<String[]> alstmp;
		fastaParser fp= new fastaParser(new BufferedReader(new FileReader(kmerFile1)));
		FastaSeq fs;
		String start, end;
		final int sizeLimit=100;
		
		
//		String kmer=s.split("\t")[pos];
//		
//		//prep the hashmaps with the data from the first file
//		final int length=kmer.length();
//		final int endStart=length-n;
//		start=kmer.substring(0, n);
//		end=kmer.substring(endStart);
//		alstmp=new ArrayList<String[]>();
//		alstmp.add(new String[] {end,kmer});
//		starts.put(start, alstmp);
//		alstmp=new ArrayList<String[]>();
//		alstmp.add(new String[] {start,kmer});
//		ends.put(end, alstmp);
		System.err.println("Parsing first file: "+kmerFile1);
		for(;fp.hasNext();){
			fs=fp.next();
			start=kmerFunctions.kmerToUse(fs.getSeq(0, length));
			end=kmerFunctions.kmerToUse(fs.getSeq(fs.length()-length, fs.length()));
			if(starts.containsKey(start)){
				alstmp=starts.get(start);
			}else{
				alstmp= new ArrayList<String[]>();
				starts.put(start, alstmp);
			}
			alstmp.add(new String[] {end,fs.getQname()});
			
			if(ends.containsKey(end)){
				alstmp=ends.get(end);
			}else{
				alstmp= new ArrayList<String[]>();
				ends.put(end, alstmp);
			}
			alstmp.add(new String[] {start,fs.getQname()});
		}
		
		//Analyse the data in the second file with respect to the hashmaps
		fp=new fastaParser(new BufferedReader(new FileReader(kmerFile2)));
		System.err.println("Parsing second file: "+kmerFile2);
		for(;fp.hasNext();){
			fs=fp.next();
			start=kmerFunctions.kmerToUse(fs.getSeq(0, length));
			end=kmerFunctions.kmerToUse(fs.getSeq(fs.length()-length, fs.length()));
			
			if(starts.containsKey(start)){
				alstmp=starts.get(start);
				if (alstmp.size()<sizeLimit) {
					for (String[] strings : alstmp) {
						System.out.println(strings[1] + sep + fs.getQname()
								+ sep + "ss");
					}
				}
			}
			if(starts.containsKey(end)){
				alstmp=starts.get(end);
				if (alstmp.size()<sizeLimit) {
					for (String[] strings : alstmp) {
						System.out.println(strings[1] + sep + fs.getQname()
								+ sep + "se");
					}
				}
			}
			if(ends.containsKey(start)){
				alstmp=ends.get(start);
				if (alstmp.size()<sizeLimit) {
					for (String[] strings : alstmp) {
						System.out.println(strings[1] + sep + fs.getQname()
								+ sep + "es");
					}
				}
			}
			if(ends.containsKey(end)){
				alstmp=ends.get(end);
				if (alstmp.size()<sizeLimit) {
					for (String[] strings : alstmp) {
						System.out.println(strings[1] + sep + fs.getQname()
								+ sep + "ee");
					}
				}
			}
		}
		System.err.println("Done!");
	}
	
	public static void extractGood(KmerSet_binary kmers,String fastqFile, int min, String outPrefix)throws Exception{
		BufferedWriter fastqout= new BufferedWriter(new FileWriter(outPrefix+"_cleaned.fastq"));
		BufferedWriter countout= new BufferedWriter(new FileWriter(outPrefix+"_count.csv"));
		fastqParser fqp= new fastqParser(new BufferedReader(new FileReader(fastqFile)), "");
		FastqSeq fqs;
//		int kmerSize=kmers.getKmerSize();
		int count;
		System.err.println("Parsing fastq file...");
		for(int i=0;fqp.hasNext();++i){
			if(i%10000==0){
				System.err.print("    "+i+"           \r");
			}
			fqs=fqp.next();
			count=kmers.count(fqs.getSeq());
//			for (String kmer : kmerizeString(fqs.getSeq(),kmerSize)) {
//				if(kmers.exists(kmer)){
//					++count;
//				}
//			}
			if(count>=min){
				fastqout.write(fqs.toString()+"\n");
				countout.write(fqs.getQname()+"\t"+count+"\n");
			}
		}
		
		fastqout.close();
		countout.close();
	}
	
//	private static ArrayList<String> kmerizeString(String seq, int length){
//		ArrayList<String> seqs = new ArrayList<String>();
//		if(seq.length()>=length){
//			String kmer=seq.substring(0, length);
//			seqs.add(kmerToUse(kmer));
//			for(int i=length;i<seq.length();i++){
//				kmer=kmer.substring(1)+seq.charAt(i);
//				seqs.add(kmerToUse(kmer));
//			}
//		}
//		return seqs;
//	}
	
	private static KmerSet_binary readKmers_revComp(String kmerCountFile, int pos)throws Exception{
		BufferedReader in= new BufferedReader(new FileReader(kmerCountFile));
		String[] l=in.readLine().split("\t");
		KmerSet_binary kmers= new KmerSet_binary(l[pos]);
		int line=1;
		for(String s=in.readLine();s!=null;s=in.readLine(),line++){
			if(line%10000==0)
				System.err.print("\t"+line+"   "+kmers.size()+"\r");
			l=s.split("\t");
			kmers.addSeq(l[pos]);
			kmers.addSeq(reverseComplementSeq(l[pos]));
		}
		
		return kmers;
	}
	
	public static void reduce(String kmerFile,int pos,int length)throws Exception{
		BufferedReader in= new BufferedReader(new FileReader(kmerFile));
		for(String s=in.readLine();s!=null;s=in.readLine()){
			System.out.println((new kmerData(pos, s)).reduce(length));
		}
	}
	
	private static String printHelp(){
		String help="";
		ArrayList<String> cmds= new ArrayList<String>(methodsHelp.keySet());
		Collections.sort(cmds);
		for (String s : cmds) {
			help+=methodsHelp.get(s);
		}
		
		return help;
	}
	
	private static String printHelp(String method){
		if(methodsHelp.containsKey(method)){
			return methodsHelp.get(method);
		}
		return printHelp();
	}
	
	protected static String reverseComplementSeq(String seq){
		String revCompSeq="";
		for(int i=0;i<seq.length();i++){
			revCompSeq= complement(seq.charAt(i))+revCompSeq;
		}
		return revCompSeq;
	}
	
	protected static String reverse(String seq){
		String rev="";
		for(int i=0;i<seq.length();i++){
			rev=seq.charAt(i)+rev;
		}
		return rev;
	}
	
	protected static String complement(String seq){
		String comp="";
		for(int i=0;i<seq.length();i++){
			comp=comp+complement(seq.charAt(i));
		}
		return comp;
	}
	
	private static char complement(char c){
		switch (c) {
		case 'A':
			return 'T';
		case 'T':
			return 'A';
		case 'G':
			return 'C';
		case 'C':
			return 'G';
		case 'N':
			return 'N';
		default:
			System.err.println("unknown nucleotide: "+c+", will return "+c);
			return c;
		}
	}
	
//	protected static String kmerToUse(String kmer){
//		String revKmer=reverseComplementSeq(kmer);
//		if(kmer.compareTo(revKmer)<0){
//			return kmer;
//		}else{
//			return revKmer;
//		}
//	}
	
	public static String kmerToUse(String kmer){
		String kmerRev= "";
		int j=kmer.length()-1,result;
		char c,r;
		for(int i=0;i<kmer.length();i++,j--){
			c=kmer.charAt(i);
			r=complement(kmer.charAt(j));
			if((result=c-r)!=0){
				if(result<0){
					return kmer;
				}else{
					kmerRev+=""+r;
					++i;
					break;
				}
			}else{
				kmerRev=r+kmerRev;
			}
		}
		for(;j>=0;j--){
			kmerRev+=complement(kmer.charAt(j))+"";
		}
		return kmerRev;
	}
	
	private static void printKmer(String kmer){
		System.out.println(kmerToUse(kmer));
	}
	
	public static void kmerize(String fastqFile, int n)throws Exception{
		fastqParser fqp=new fastqParser(new BufferedReader(new FileReader(fastqFile)),getPrefix(fastqFile));
		String seq;
		for(;fqp.hasNext();){
			seq=fqp.next().getSeq();
			for(int i=0,j=n;j<=seq.length();i++,j++){
				printKmer(seq.substring(i, j));
			}
		}
	}
	
	public static void kmerizeEnd(String fastqFile, int n, int m)throws Exception{
		if(m>=n){
			throw new Exception("m ("+m+") must be smaller than n ("+n+")");
		}
		fastqParser fqp=new fastqParser(new BufferedReader(new FileReader(fastqFile)),getPrefix(fastqFile));
		for(;fqp.hasNext();){
			String seq= fqp.next().getSeq();
			seq=seq.substring(seq.length()-n+1);
			for(int i=0,j=m;j<=seq.length();i++,j++){
				printKmer(seq.substring(i, j));
			}
		}
	}
	
	private static void mapListMutationKmers(String mapListFile,String mutationFile,int n, String outPrefix) throws Exception{
		BufferedReader mutationReader = new BufferedReader(new FileReader(mutationFile));
		HashMap<String, ArrayList<Mutation>> mutations= new HashMap<String, ArrayList<Mutation>>();
		Mutation mutation;
		for(String s=mutationReader.readLine();s!=null;s=mutationReader.readLine()){
			String l[]=s.split("\t");
			mutation=new Mutation(l[0], Integer.parseInt(l[1]), l[2].charAt(0));
			if(!mutations.containsKey(mutation.getChr())){
				mutations.put(mutation.getChr(), new ArrayList<Mutation>());
			}
			mutations.get(mutation.getChr()).add(mutation);
		}
		mutationReader.close();
		BufferedReader mapListReader = new BufferedReader(new FileReader(mapListFile));
		ShoreMapLine sml;
		BufferedWriter origWriter= new BufferedWriter(new FileWriter(outPrefix+"_orig.kmers"));
		BufferedWriter mutatedWriter= new BufferedWriter(new FileWriter(outPrefix+"_mutated.kmers"));
		for(String s= mapListReader.readLine();s!=null;s=mapListReader.readLine()){
			sml= new ShoreMapLine(s);
			if(mutations.containsKey(sml.getChr())){
				//check for mutation... this has to be rewritten if there are a lot of mutations or an unfiltered map.list
				String orig=sml.getSeq();
				String mutated=sml.getMutatedSeq(mutations.get(sml.getChr()));
				if(!orig.equals(mutated)){
					//kmerize
					for(int i=0,j=n;j<orig.length();i++,j++){
						origWriter.write(kmerToUse(orig.substring(i,j))+"\n");
						mutatedWriter.write(kmerToUse(mutated.substring(i, j))+"\n");
					}
				}
			}
		}
		mapListReader.close();
		origWriter.close();
		mutatedWriter.close();
	}
	
	private static void generateKmers(int n,String s)throws Exception{
		if(n==0){
			System.out.println(s);
		}else{
			for(int i=0;i<4;i++){
				generateKmers(n-1, nucleotides[i]+s);
			}
		}
	}
	
	private static void generateSeeds3(String kmerFile1, String kmerFile2, int pos, int minSeedSize, int lmer,String outPrefix)throws Exception{
		boolean verbose= false,added;
		BufferedReader in;
		HashMap<String,HashSet<kmerData>> kmers= new HashMap<String, HashSet<kmerData>>(50000000);
		HashSet<String> suffixes= new HashSet<String>(50000000);
		ArrayList<seedData> seeds= new ArrayList<seedData>();
		int counter=0;
		ArrayList<Character> alphabet= new ArrayList<Character>();
		String alp="ACGT";
		for(int i=0; i < alp.length();++i)
		{
			alphabet.add(alp.charAt(i));
		}
//		Entropy ent = new Entropy(alphabet);
		
		HashSet<kmerData> putativeStartPoints= new HashSet<kmerData>(),trueStartPoints= new HashSet<kmerData>();
		
		System.err.println("reading background...");
		kmerData kmer;
		
		KmerSet_binary lmers=new KmerSet_binary(lmer, 5500000);
		
		in= new BufferedReader(new FileReader(kmerFile2));
//		int count=0;
		for(String s= in.readLine();s!=null;s=in.readLine(),++counter){
			if(counter%10000==0){
				System.err.print("         "+counter+"\r");
			}
			kmer=new kmerData(pos, s);
			lmers.addSeq(kmer.getKmer());
//			if(count>10)
//				System.exit(0);
		}
		System.err.println("         "+counter);
		
		System.err.println("reading dataset ");
		counter=0;
		in= new BufferedReader(new FileReader(kmerFile1));
		for(String s=in.readLine();s!=null;s=in.readLine(),++counter){
			if(counter%10000==0){
				System.err.print("         "+counter+"\r");
			}
			kmer=new kmerData(pos, s);
//			verbose= kmer.getKmer().equals("AAGTTAAGGATTAGTAATCCGTTTTAGAAAG");
			if(verbose) System.err.println("exists...");
			if(!kmers.containsKey(kmer.prefix())){
				kmers.put(kmer.prefix(), new HashSet<kmerData>());
			}
			if(!kmers.containsKey(kmer.prefixRevComp())){
				kmers.put(kmer.prefixRevComp(), new HashSet<kmerData>());
			}
			kmers.get(kmer.prefix()).add(kmer);
			kmers.get(kmer.prefixRevComp()).add(kmer);
			suffixes.add(kmer.suffix());
			suffixes.add(kmer.suffixRevComp());
			
			//find kmers that contains an lmer from the other sample
			if(lmers.covered(kmer.getKmer())){
				if(verbose) System.err.println("added as possible start point");
				putativeStartPoints.add(kmer);
			}
//			System.exit(0);
		}
		lmers=new KmerSet_binary(lmer, false);
		System.err.println("         "+counter);
		verbose=false;
//		System.err.println(putativeStartPoints.size());
		
		System.err.println("finding startpoints");
		counter=0;
		//find the startPoints, which have unique start points
		for (kmerData kmerData : putativeStartPoints) {
			++counter;
			if(counter%10000==0){
				System.err.print("         "+counter+"\r");
			}
//			verbose= kmerData.getKmer().equals("AAGTTAAGGATTAGTAATCCGTTTTAGAAAG");
			if(verbose) System.err.println("is start");
			added=false;
			if(!suffixes.contains(kmerData.prefix())){
				if(verbose) System.err.println("added");
				seeds.add(new seedData(kmerData));
				added=true;
			}
			if(!suffixes.contains(kmerData.prefixRevComp())){
				if(verbose) System.err.println("added rev comp");
				seeds.add(new seedData(kmerData.revComp()));
				added=true;
			}
			if(added){
				trueStartPoints.add(kmerData);
//				System.out.println(kmerData.getKmer()+sep+ent.calcEntropy(kmerData.getKmer()));
			}
		}
		System.err.println("         "+counter);
		verbose=false;
		
//		System.err.println(seeds.size()+"\t"+trueStartPoints.size()+"\t"+putativeStartPoints.size());
		putativeStartPoints.clear();
		suffixes.clear();
		
		System.err.println("generating seeds ("+seeds.size()+")");
		counter=0;
		int seedCount=0;
		BufferedWriter out= new BufferedWriter(new FileWriter(outPrefix+".fa"));
		BufferedWriter outLong= new BufferedWriter(new FileWriter(outPrefix+"_long.txt"));
//		HashSet<kmerData> usedKmers=new HashSet<kmerData>();
		
		for (seedData initialSeed : seeds) {
			++counter;
			if(counter%1000==0){
				System.err.print("         "+counter+"\r");
			}
//			verbose= initialSeed.consensus().equals("AAGTTAAGGATTAGTAATCCGTTTTAGAAAG");
			if(verbose) System.err.println("extending");
			for (seedData finalSeed : generateSeeds_extend(initialSeed, kmers, trueStartPoints)) {
				if(verbose) System.err.println(finalSeed.consensus());
//				usedKmers.addAll(finalSeed.getKmers());
				if(finalSeed.size()>=minSeedSize){
					seedCount++;
					out.write(">"+seedCount+" "+finalSeed.size()+"\n"+finalSeed.consensus()+"\n");
					outLong.write(">"+seedCount+" "+finalSeed.size()+"\n"+finalSeed.toString()+"\n");
				}
			}
		}
		System.err.println("         "+counter);
		verbose=false;
		out.close();
		outLong.close();
		
//		BufferedWriter outKmer= new BufferedWriter(new FileWriter(outPrefix+"_used.kmerDiff"));
//		for (kmerData kmerData : usedKmers) {
//			outKmer.write(kmerData.toString()+"\n");
//		}
		
//		outKmer.close();
		System.err.println("Done!");

		
	}
	
	private static void generateSeeds2(String kmerFile1,String kmerFile2,int pos,int minSeedSize,String outPrefix)throws Exception{
		BufferedReader in;
		HashMap<String,HashSet<kmerData>> kmers= new HashMap<String, HashSet<kmerData>>();
		HashSet<String> suffixes= new HashSet<String>(1000000), prefixesOther= new HashSet<String>();
		ArrayList<seedData> seeds= new ArrayList<seedData>();
		
		HashSet<kmerData> startPoints= new HashSet<kmerData>();
		
		kmerData kmer;
		
		in=new BufferedReader(new FileReader(kmerFile2));
		for(String s= in.readLine();s!=null;s=in.readLine()){
			kmer=new kmerData(pos, s);
			prefixesOther.add(kmer.prefix());
			prefixesOther.add(kmer.prefixRevComp());
			prefixesOther.add(kmer.suffix());
			prefixesOther.add(kmer.suffixRevComp());
		}
		
		in= new BufferedReader(new FileReader(kmerFile1));
		for(String s=in.readLine();s!=null;s=in.readLine()){
			kmer=new kmerData(pos, s);
			if(!kmers.containsKey(kmer.prefix())){
				kmers.put(kmer.prefix(), new HashSet<kmerData>());
			}
			if(!kmers.containsKey(kmer.prefixRevComp())){
				kmers.put(kmer.prefixRevComp(), new HashSet<kmerData>());
			}
			kmers.get(kmer.prefix()).add(kmer);
			kmers.get(kmer.prefixRevComp()).add(kmer);
			suffixes.add(kmer.suffix());
			suffixes.add(kmer.suffixRevComp());
			if(prefixesOther.contains(kmer.prefix())){
				startPoints.add(kmer);
			}
			if(prefixesOther.contains(kmer.prefixRevComp())){
				startPoints.add(kmer.revComp());
			}
//			startPoints.add(kmer);
		}
		
		prefixesOther.clear();
		//Find starting points
//		in=new BufferedReader(new FileReader(kmerFile2));
//		for(String s= in.readLine();s!=null;s=in.readLine()){
//			kmer=new kmerData(pos, s);
//			if(kmers.containsKey(kmer.prefix())){
//				startPoints.addAll(kmers.get(kmer.prefix()));
//				
//			}
//			if(kmers.containsKey(kmer.prefixRevComp())){
//				for (kmerData kmerData : kmers.get(kmer.prefixRevComp())) {
//					startPoints.add(kmerData.revComp());
//				}
//				startPoints.addAll(kmers.get(kmer.prefixRevComp()));
//			}
//		}
		
		for (kmerData kmerData : startPoints) {
			seeds.add(new seedData(kmerData));
		}
		
//		Collections.sort(kmers);
		//Find seeds
		int seedCount=0;
		BufferedWriter out= new BufferedWriter(new FileWriter(outPrefix+".fa"));
		BufferedWriter outLong= new BufferedWriter(new FileWriter(outPrefix+"_long.txt"));
		HashSet<kmerData> usedKmers=new HashSet<kmerData>();
		
		for (seedData initialSeed : seeds) {
//			boolean print=(initialSeed.consensus().equals("CACCCTTACTCTCAGCTTCAACAAGTTTTTA")||initialSeed.consensus().equals("GATTAGCGCGGGTCTTCACGTAATAATCAGC"));
//			if(print) System.err.println(initialSeed.consensus());
			for (seedData finalSeed : generateSeeds_extend(initialSeed, kmers, startPoints)) {
//				if(print) System.err.println(finalSeed.toString());
				usedKmers.addAll(finalSeed.getKmers());
				if(finalSeed.size()>=minSeedSize){
					seedCount++;
					out.write(">"+seedCount+" "+finalSeed.size()+"\n"+finalSeed.consensus()+"\n");
					outLong.write(">"+seedCount+" "+finalSeed.size()+"\n"+finalSeed.toString()+"\n");
				}
			}
		}
		out.close();
		outLong.close();
		
		BufferedWriter outKmer= new BufferedWriter(new FileWriter(outPrefix+"_used.kmerDiff"));
		for (kmerData kmerData : usedKmers) {
			outKmer.write(kmerData.toString()+"\n");
		}
		
		outKmer.close();
//		return seeds;
		System.err.println("Done!");
	}
	
	private static ArrayList<seedData> generateSeeds_extend(seedData seed, HashMap<String,HashSet<kmerData>> kmers, HashSet<kmerData> endPoints){
		ArrayList<seedData> seeds= new ArrayList<seedData>(),nextSeeds, doneSeeds= new ArrayList<seedData>();
		boolean fin;
		seedData nextSeed;
		Entropy ent= new Entropy();
		String broken="complete";
		
		seeds.add(seed);
//		boolean print=(seed.consensus().equals("CACCCTTACTCTCAGCTTCAACAAGTTTTTA")||seed.consensus().equals("GATTAGCGCGGGTCTTCACGTAATAATCAGC"));
		int i;
		
		for(i=0;seeds.size()>0;++i){
			nextSeeds= new ArrayList<seedData>();
			for(seedData curSeed: seeds){
				//try to extend
//				if(print)System.err.println(curSeed.suffix());
				if(kmers.containsKey(curSeed.suffix())){
//					if(print)System.err.println("extend");
					fin=true;
					for (kmerData kmer : kmers.get(curSeed.suffix())) {
						nextSeed= new seedData(curSeed);
						if(nextSeed.addRight(kmer)){
							if(endPoints.contains(kmer)){
//								if(print)System.err.println("1");
								doneSeeds.add(nextSeed);
							}else{
								nextSeeds.add(nextSeed);
							}
							fin=false;
						}
					}
					if(fin){
//						if(print)System.err.println("2");
						doneSeeds.add(curSeed);
					}
				}else{
//					if(print)System.err.println("3");
					doneSeeds.add(curSeed);
				}
			}
			if(i>150 || nextSeeds.size()>1000){ //length cutoff, size cutoff
				broken="broke";
				doneSeeds.addAll(nextSeeds);
				nextSeeds.clear();
			}
			seeds.clear();
			seeds.addAll(nextSeeds);
		}
		System.out.println(broken+"\t"+seed.getKmers().get(0).getKmer()+"\t"+doneSeeds.size()+"\t"+i+"\t"+ent.calcEntropy(seed.getKmers().get(0).getKmer()));
		
		return doneSeeds;
	}
	
	private static void generateSeeds(String kmerFile,int pos,int minSeedSize,String outPrefix)throws Exception{
		BufferedReader in= new BufferedReader(new FileReader(kmerFile));
		HashMap<String,HashSet<kmerData>> kmers= new HashMap<String, HashSet<kmerData>>();
		HashSet<String> suffixes= new HashSet<String>(1000000);
		
		HashSet<kmerData> uniqueKmers= new HashSet<kmerData>();
		
		kmerData kmer;
		for(String s=in.readLine();s!=null;s=in.readLine()){
			kmer=new kmerData(pos, s);
			if(!kmers.containsKey(kmer.prefix())){
				kmers.put(kmer.prefix(), new HashSet<kmerData>());
			}
			if(!kmers.containsKey(kmer.prefixRevComp())){
				kmers.put(kmer.prefixRevComp(), new HashSet<kmerData>());
			}
			kmers.get(kmer.prefix()).add(kmer);
			kmers.get(kmer.prefixRevComp()).add(kmer);
			suffixes.add(kmer.suffix());
			suffixes.add(kmer.suffixRevComp());
			uniqueKmers.add(kmer);
		}
//		Collections.sort(kmers);
		//Find seeds
		int seedCount=0;
		ArrayList<seedData> seeds= new ArrayList<seedData>();
		BufferedWriter out= new BufferedWriter(new FileWriter(outPrefix+".fa"));
		BufferedWriter outLong= new BufferedWriter(new FileWriter(outPrefix+"_long.txt"));
		HashSet<kmerData> usedKmers;
		boolean found=true;
		System.err.println("Unique seeds...");
		while(found){
			found=false;			
			System.err.print("    "+kmers.size()+"\r");
			//find unique seeds
			seeds=new ArrayList<seedData>();
			for (kmerData kmerData : uniqueKmers) {
				if(!suffixes.contains(kmerData.prefix())){
					seeds.add(new seedData(kmerData));
				}
			}
			System.err.print("    "+kmers.size()+" "+seeds.size()+"      "+"\r");
			found=seeds.size()>0;
			//extend seeds
			usedKmers=new HashSet<kmerData>();
			for (seedData initialSeed : seeds) {
				for (seedData finalSeed : generateSeeds_extend(initialSeed, kmers)) {
					usedKmers.addAll(finalSeed.getKmers());
					if(finalSeed.size()>=minSeedSize){
						seedCount++;
						out.write(">"+seedCount+" "+finalSeed.size()+"\n"+finalSeed.consensus()+"\n");
						outLong.write(">"+seedCount+" "+finalSeed.size()+"\n"+finalSeed.toString()+"\n");
					}
				}
			}
			//clean
			uniqueKmers.removeAll(usedKmers);
			
			for (kmerData kmerData : usedKmers) {
				kmers.remove(kmerData.prefix());
				kmers.remove(kmerData.prefixRevComp());
			}
			//Rebuild the suffix list
			suffixes= new HashSet<String>(1000000);
			for (kmerData kmerData : uniqueKmers) {
				suffixes.add(kmerData.suffix());
				suffixes.add(kmerData.suffixRevComp());
			}
			//found=false;
			//kmers.clear();
		}
		System.err.println("Repetitive structures...");
		while(kmers.size()>0){
			HashSet<kmerData> onePrefix= new HashSet<kmerData>();
			for (HashSet<kmerData> tmp : kmers.values()) {
				onePrefix=new HashSet<kmerData>(tmp);
				break;
			}
			usedKmers= new HashSet<kmerData>();
			for (kmerData kmerData : onePrefix) {
				for(seedData finalSeed : generateSeeds_extend(new seedData(kmerData), kmers)){
					usedKmers.addAll(finalSeed.getKmers());
					if(finalSeed.size()>=minSeedSize){
						seedCount++;
						out.write(">"+seedCount+" "+finalSeed.size()+" cyclic\n"+finalSeed.consensus()+"\n");
						outLong.write(">"+seedCount+" "+finalSeed.size()+" cyclic\n"+finalSeed.toString()+"\n");
					}
				}
			}
			//clean
			uniqueKmers.removeAll(usedKmers);
			
			for (kmerData kmerData : usedKmers) {
				kmers.remove(kmerData.prefix());
				kmers.remove(kmerData.prefixRevComp());
			}
		}
		out.close();
		outLong.close();
//		return seeds;
		System.err.println("Done!");
	}
	
	private static ArrayList<seedData> generateSeeds_extend(seedData seed, HashMap<String,HashSet<kmerData>> kmers){
		return generateSeeds_extend(seed, kmers,new HashSet<kmerData>());
//		ArrayList<seedData> seeds= new ArrayList<seedData>(),nextSeeds, doneSeeds= new ArrayList<seedData>();
//		boolean fin;
//		seedData nextSeed;
//		
//		seeds.add(seed);
//		
//		for(int i=0;seeds.size()>0;++i){
//			if(i%10==0){
//				StringBuffer out=new StringBuffer(i+"  "+seeds.size()+"\r");
//				while(out.length()<20){
//					out.insert(0, ' ');
//				}
//				System.err.print(out);
//			}
//			nextSeeds= new ArrayList<seedData>();
//			for(seedData curSeed: seeds){
//				//try to extend
//				if(kmers.containsKey(curSeed.suffix())){
//					fin=true;
//					for (kmerData kmer : kmers.get(curSeed.suffix())) {
//						nextSeed= new seedData(curSeed);
//						if(nextSeed.addRight(kmer)){
//							nextSeeds.add(nextSeed);
//							fin=false;
//						}
//					}
//					if(fin){
//						doneSeeds.add(curSeed);
//					}
//				}else{
//					doneSeeds.add(curSeed);
//				}
//			}
//			if(i>150){
//				doneSeeds.addAll(nextSeeds);
//				nextSeeds.clear();
//			}
//			seeds.clear();
//			seeds.addAll(nextSeeds);
//		}
//		return doneSeeds;
	}
	
	
	private static void sortKmersLink(String kmerFile, int pos, int maxJump)throws Exception{
//		System.err.println("kmerFile:"+kmerFile+"\npos: "+pos+"\nmaxJump: "+maxJump);
		BufferedReader in= new BufferedReader(new FileReader(kmerFile));
		ArrayList<kmerData> kmers= new ArrayList<kmerData>();
//		System.err.println("Reading "+kmerFile);
//		int line=0;
		for(String s=in.readLine();s!=null;s=in.readLine()){
//			if(line%10000==0){
//				System.out.print("\t"+line+"\r");
//			}
			kmers.add(new kmerData(pos, s));
//			++line;
		}
//		System.err.println(line+"\nsorting...");
		Collections.sort(kmers);
//		System.err.println("printing and idenifying links");
		String lastKmer="XXXXXXXXXXX";
		while(lastKmer.length()<maxJump+1);{
			lastKmer+="X";
		}
		boolean linked;
		for (kmerData kmerData : kmers) {
			System.out.print(kmerData);
			linked=false;
			for(int i=1;i<=maxJump&&!linked;i++){
				linked=kmerData.getKmer().startsWith(lastKmer.substring(i));
			}
			if(linked){
				System.out.println(sep+"1");
			}else{
				System.out.println(sep+"0");
			}
			lastKmer=kmerData.getKmer();
		}
	}
	
	private static void sortKmers(String kmerFile,int pos, int binSize,String tmpPrefix)throws Exception{
		BufferedReader in= new BufferedReader(new FileReader(kmerFile));
		ArrayList<kmerData> kmers= new ArrayList<kmerData>();
		for(String s=in.readLine();s!=null;s=in.readLine()){
			kmers.add(new kmerData(pos, s));
			if(kmers.size()>=binSize){
				Collections.sort(kmers);
				//push to disk
				
			}
		}
		Collections.sort(kmers);
		//do mergeSort...
		
		for (kmerData kmerData : kmers) {
			System.out.println(kmerData);
		}
	}
	public static String getPrefix(String fastqFile)throws Exception{
		fastqParser fqp;
		FastqSeq fqs;
		String prefix="";
		boolean done=true;
		for (int i=6;i>-1&&done;i--){
			fqp= new fastqParser(new BufferedReader(new FileReader(fastqFile)),"");
			fqs=fqp.next();
			prefix=fqs.getQname().substring(0,i);
			done=true;
			for(int j=0;j<10&&fqp.hasNext();j++){
				fqs=fqp.next();
				if(!prefix.equals(fqs.getQname().subSequence(0, i))){
					done=false;
				}
			}
		}
		return prefix;
	}
}

class seedData{
	
	private ArrayList<kmerData> kmers;
	private ArrayList<Integer> strand;
	private String consensus;
	
	private seedData(){
		kmers= new ArrayList<kmerData>();
		strand= new ArrayList<Integer>();
		consensus="";
	}
	
	public seedData(kmerData first){
		this();
		kmers.add(first);
		strand.add(0);
		consensus=first.getKmer();
	}
	
	public seedData(seedData old){
		this();
		this.kmers.addAll(old.kmers);
		this.strand.addAll(old.strand);
		this.consensus=old.consensus;
	}
	
	public String suffix(int n){
//		System.err.println(n+";"+consensus.length());
		return consensus.substring(consensus.length()-n,consensus.length());
	}
	
	public String suffix(){
		return suffix(kmers.get(0).getKmer().length()-1);
	}
	
	public ArrayList<kmerData> getKmers(){
		return kmers;
	}
	
	public String consensus(){
		//presumes that the kmers are of the same length
//		String seq=kmers.get(0).getKmer();
//		int lastPos=seq.length()-1;
//		for(int i=1;i<kmers.size();i++){
//			seq+=""+kmers.get(i).getKmer().charAt(lastPos);
//		}
//		return seq;
		return consensus;
	}
	
	public boolean addRight(kmerData kd){
		if(this.contains(kd)){
			return false;
		}
		//same strand
		if(consensus.endsWith(kd.prefix())){
			kmers.add(kd);
			strand.add(0);
			consensus+=kd.tail();
			return true;
		}
		//opposite strand
		if(consensus.endsWith(kd.prefixRevComp())){
			kmers.add(kd);
			strand.add(1);
			consensus+=kmerUtils.complement(kd.head());
			return true;
		}
		return false;
	}
	
	public boolean addLeft(kmerData kd){
		if(this.contains(kd)){
			return false;
		}
		//same strand
		if(consensus.startsWith(kd.suffix())){
			kmers.add(0, kd);
			strand.add(0,0);
			consensus=kd.head()+consensus;
			return true;
		}
		//opposite strand
		if(consensus.startsWith(kd.suffixRevComp())){
			kmers.add(0, kd);
			strand.add(0,1);
			consensus=kmerUtils.complement(kd.tail())+consensus;
		}
		return false;
	}
	
//	public boolean addRight(kmerData ext){
//		if(extendRight(ext)&&!contains(ext)){
//			kmers.add(ext);
//			return true;
//		}
//		return false;
//	}
//	
//	public boolean addLeft(kmerData ext){
//		if(extendLeft(ext)&&!contains(ext)){
//			kmers.add(0, ext);
//			return true;
//		}
//		return false;
//	}
//	
//	public boolean extendRight(kmerData ext){
//		return kmers.get(kmers.size()-1).extendRight(ext);
//	}
//	
//	public boolean extendLeft(kmerData ext){
//		return kmers.get(0).extendLeft(ext);
//	}
	
	public boolean contains(kmerData kd){
		return kmers.contains(kd);
	}
	
	public boolean contains(seedData seed){
		return this.kmers.containsAll(seed.kmers);
	}
	
	public int size(){
		return kmers.size();
	}
	
	public String toString(){
		String out=consensus();
		for (int i=0;i<kmers.size();i++){
			if(strand.get(i)==0){
				out+="\n"+kmers.get(i).toString();
			}else{
				out+="\n"+kmers.get(i).toStringRevComp();
			}
		}
		
//		String lastSuffix=kmers.get(0).suffix();
//		kmerData kmer;
//		for(int i=1;i<kmers.size();i++){
//			kmer=kmers.get(i);
//			if(kmer.getKmer().startsWith(lastSuffix)){
//				out+="\n"+kmer.toString();
//			}else{
//				out+="\n"+kmer.toStringRevComp();
//			}
//			lastSuffix=kmer.suffix();
//		}
		return out;
	}
}

class kmerData implements Comparable<kmerData>,Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int pos;
	private int permutations;
	private String rep;
	private String kmerSeqRevComp;
	private String[] data;
	
	public kmerData(int pos, String s) throws Exception{
		this(pos, s.split("\t"));
	}
	
	public kmerData(int pos, String[] data) throws Exception {
		super();
		this.pos = pos;
		this.data = data;
		kmerSeqRevComp=kmerUtils.reverseComplementSeq(data[pos]);
		rep=data[pos];
		permutations=0;
		String tmp=rep;
		for(int i=0;i<rep.length();i++){
			tmp=tmp.substring(1)+tmp.charAt(0);
			if(tmp.compareTo(rep)<=0){
				rep=tmp;
				permutations=0;
			}else{
				permutations++;
			}
			if(data[pos].equals(tmp)){
				break;
			}
		}
	}
	
	public kmerData revComp()throws Exception{
		return new kmerData(pos, this.toStringRevComp());
	}
	
	public kmerData reduce(int n) throws Exception{
		String[] newData=new String[data.length];
		for(int i=0;i<data.length;i++){
			newData[i]=data[i];
		}
		newData[pos]=kmerUtils.kmerToUse(data[pos]);
		return new kmerData(pos,newData);
	}
	
	public String suffixRevComp(){
		return suffixRevComp(1);
	}
	
	public String suffixRevComp(int n){
		return getKmerRevComp().substring(n);
	}
	
	public String prefixRevComp(int n){
		return getKmerRevComp().substring(0, getKmer().length()-n);
	}
	
	public String prefixRevComp(){
		return prefixRevComp(1);
	}
	
	public String tail(){
		return ""+getKmer().charAt(getKmer().length()-1);
	}
	
	public String head(){
		return ""+getKmer().charAt(0);
	}
	
	public String suffix(int n){
		return getKmer().substring(n);
	}
	
	public String suffix(){
		return suffix(1);
	}
	
	public String prefix(int n){
		return getKmer().substring(0, getKmer().length()-n);
	}
	
	public String prefix(){
		return prefix(1);
	}
	
	private boolean extendLeft(String prefix){
		return getKmer().startsWith(prefix)||getKmerRevComp().startsWith(prefix);
	}
	
	public boolean extendLeft(kmerData prefix,int n){
		return extendLeft(prefix.suffix(n));
//		return startsWith(prefix.getKmer().substring(n));
	}
	
	public boolean extendLeft(kmerData prefix){
		return extendLeft(prefix,1);
	}
	
	private boolean extendRight(String suffix){
		return getKmer().endsWith(suffix)||getKmerRevComp().endsWith(suffix);
	}
	
	public boolean extendRight(kmerData suffix, int n){
		return extendRight(suffix.prefix(n));
		//return endsWith(suffix.getKmer().substring(0, suffix.getKmer().length()-n));
	}
	
	public boolean extendRight(kmerData suffix){
		return extendRight(suffix,1);
	}
	
	public String getKmer(){
		return data[pos];
	}
	
	private String getKmerRevComp(){
		return kmerSeqRevComp;
	}

	public int compareTo(kmerData o) {
		if(this.rep.equals(o.rep))
			return this.permutations-o.permutations;
		return this.rep.compareTo(o.rep);
	}
	
	public String toString(){
		return this.toString("\t");
	}
	
	public String toStringRevComp(){
		return toStringRevComp("\t");
	}
	
	public String toStringRevComp(String sep){
		String s="";
		if(pos==0){
			s=getKmerRevComp();
		}else{
			s=data[0];
		}
		for(int i=1;i<data.length;i++){
			if(i==pos){
				s+=sep+getKmerRevComp();
			}else{
				s+=sep+data[i];
			}
		}
		return s;
	}
	
	public String toString(String sep){
		String s= data[0];
		for(int i=1;i<data.length;i++){
			s+=sep+data[i];
		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
		result = prime * result + pos;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		kmerData other = (kmerData) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		if (pos != other.pos)
			return false;
		return true;
	}
	
	
}

