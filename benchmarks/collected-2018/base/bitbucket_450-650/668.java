// https://searchcode.com/api/result/51595627/

/*
 *  Copyright 2011 Shawn Thomas O'Neil
 *
 *  This file is part of Hapler.
 *
 *  Hapler is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Hapler is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Hapler.  If not, see <http://www.gnu.org/licenses/>.
 */

package hapler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 *
 * @author soneil
 */
public class IOHandler {

	OptionSet options;
	String helpText;
	float version;

	/*
	 * Changelog:
	 * 1.01 : added ability to have ~'s in TIGR formatted sequenes, so that --allow-gaps (true and split) can be used with them (even though only split is currently supported))
	 * 1.5 : internal logic cleanup
	 *      : added 'auto' option for --random-repetitions : added --max-repetitions option
	 *      : added minimum-chimerism consensusHumanReadable reconstruction
	 *      : fixed an error in the 454 SNP caller where SNPs could be called even if the majority vote was a gap
	 * 1.51 : Fixed bug with parsing SAM format, if cigar string is "*", the read is skipped.
	 * 1.52 : Adjusted the output format to be more parsable, hopefully
	 *      : added an optional --human-readable option to decrease output size for large datasets
	 *      : fixed a bug where the program would crash when given an alignment with no SNPs
	 *      : code cleanup and fixes
	 * 1.53 : tweaked the output format (again) for ease of description and parsing
	 *      : added custom snp list loading (finally)
	 *      : removed the --allow-gaps option; by default all gaps are split and all mate-pair information is ignored,
	 *          and and warning is output when this is detected.
	 * 1.54 : fixed a small bug in the 454 snp caller where overlapping homopolymer run errors might be called as SNPs
	 * 1.55 : fixed a bug in reading ground truth alignments 
	 *      : added internal book-keeping for allele counts to SNPs
	 *      : Fixed a bug in the SAM parser: sam files are 1-indexed, hapler is 0 indexed
	 *      : Added two new columns of output when using --ground-truth option
	 * 1.60 : Added a new SNP caller, which has been set as the default: --binomial
	 */

	public IOHandler() {
		options = null;
		version = 1.60f;
		helpText =
"This is Hapler, version " + version + ". Hapler is a tool for producing " + System.getProperty("line.separator") +
"robust haplotype consensus regions given multiple alignments (assemblies, " +System.getProperty("line.separator") +
"really) of genetically diverse sequence data. Hapler compares each sequence " +System.getProperty("line.separator") +
"to every other, and groups sequences together into sets that don't have any " +System.getProperty("line.separator") +
"conflicts (minimum coloring of the sequence 'conflict graph'). This can be " +System.getProperty("line.separator") +
"done in O(n^3) time, because Hapler assumes that sequences contain no gaps " +System.getProperty("line.separator") +
"(e.g., it ignores mate-pair information. Future versions may allow gaps at " +System.getProperty("line.separator") +
"the risk of producing incorrect haplotypes.) Because such a minimum coloring " +System.getProperty("line.separator") +
"is usually not unique, Hapler allows the user to select many pseudo-random " +System.getProperty("line.separator") +
"colorings (using the --random-repetitions parameter, default 10) and only " +System.getProperty("line.separator") +
"keep haplotype groupings which are common to all. In practice this drastically " +System.getProperty("line.separator") +
"increases the correctness of results." +System.getProperty("line.separator") +
System.getProperty("line.separator") +
" Example Usage: cat TIGR_alignment.tigr | java -jar Hapler.jar" +System.getProperty("line.separator") +
" Example Usage 2: java -jar Hapler.jar --input SAM_alignment.sam --alignment-type sam --snp-caller 454 --random-repetitions 100" +System.getProperty("line.separator") +
System.getProperty("line.separator") +
"Hapler works on any alphabet (RNA, DNA, Protein, ...). '-' characters are " +System.getProperty("line.separator") +
"treated as conflict-causing alleles, '~' characters are treated as unknown " +System.getProperty("line.separator") +
"gaps (that are not, by default, allowed.)";
	}


	public void execAndPrintSummaryStats() throws Exception {
		this.printInfoKey();

		ArrayList<MultipleAlignment> alignmentList = parseAlignments(options);
		this.computeAndAddSNPs(options, alignmentList);

		boolean useEpsilon = parseEpsilon(options);
		int numReps = parseNumReps(options);
		int maxReps = parseMaxReps(options);
		boolean computeRecons = parseComputeRecons(options);
		boolean humanReadable = parseHumanReadable(options);
		MultipleAlignment groundTruthAlignment = getGroundTruthAlignment(options);


		for(int i = 0; i < alignmentList.size(); i++) {
			MultipleAlignment alignment = alignmentList.get(i);

			Haplotyper hapler = new Haplotyper(alignment, useEpsilon, numReps, maxReps);
			System.err.println(alignment.getName() + ": Assembling haplotypes...");
			hapler.execute();
			System.err.println(alignment.getName() + ": Finished assembling haplotypes...");

			boolean hasSNPs = false;
			if(alignment.numSNPs() > 0) hasSNPs = true;

			if(computeRecons) {
				if(!hasSNPs) System.out.println("#!WARNING: alignment " + alignment.getName() + " has no SNPs. The Hapler consensus will equal the majority vote consensus.");
				printAndEvaluateHaplerConsensus(alignment, groundTruthAlignment);
				printAndEvaluateMajorityVoteConsensus(alignment, groundTruthAlignment, hasSNPs);
				if(options.has("evaluate-contigs")) {
					printAndEvaluateGivenContigs(alignment, groundTruthAlignment);
				}
			}

			this.printSummaryInfo(hapler, alignment, groundTruthAlignment, humanReadable);


			if(options.has("show-alignments")) {
				System.out.println();
				System.out.println();
				System.out.println(hapler.toString());
				System.out.println();
			}

		}

	}

	private MultipleAlignment getGroundTruthAlignment(OptionSet options) throws Exception {
		MultipleAlignment groundTruthAlignment = null;
		if(options.has("ground-truth")) {
			ArrayList<Sequence> groundTruthSequences = null;
			ArrayList<HaplotypeBlock> groundTruthHaploBlocks = new ArrayList<HaplotypeBlock>();

			groundTruthAlignment = constructSingleAlignmentFromFasta((String)options.valueOf("ground-truth"), "GroundTruth");
			groundTruthSequences = groundTruthAlignment.getSequences();
			HaplotypeBlock newBlock = new HaplotypeBlock(groundTruthAlignment);
			newBlock.setName("Block0");
			for(Sequence seq: groundTruthSequences) {
				Haplotype newHap = new Haplotype(groundTruthAlignment);
				newHap.addSequence(seq);
				newHap.setName(seq.getName());
				newHap.setHaploBlock(newBlock);
				newBlock.addHaplotype(newHap);
			}
			groundTruthHaploBlocks.add(newBlock);
			groundTruthAlignment.setHaploBlocks(groundTruthHaploBlocks);
		}
		return groundTruthAlignment;
	}


	public void printAndEvaluateHaplerConsensus(MultipleAlignment alignment, MultipleAlignment groundTruthAlignment) throws Exception {
		System.err.println(alignment.getName() + ": Computing and evaluating Hapler consensus...");
		ArrayList<HaplotypeBlock> haploBlocks = alignment.getHaploBlocks();
		Reconstructor reconstructor = new Reconstructor();
		// reconHaps is the hapler consensusHumanReadable reconstruction, which minimizes various things
		ReconstructedConsensus reconHaps = reconstructor.computeReconstructedConsensus(alignment);
		// reconGroundTruthEval evaluates the reconHaps consensusHumanReadable against ground truth

		StringBuilder sb = new StringBuilder();

		sb.append(">" + alignment.getName() + "_H");
		sb.append("\t" + alignment.getName());
		sb.append("\tHapler");
		sb.append(constructConsensusStats(alignment, groundTruthAlignment, reconHaps));

		System.out.println(sb.toString());

	}


	public String constructConsensusStats(MultipleAlignment alignment, MultipleAlignment groundTruthAlignment, ReconstructedConsensus reconHaps) throws Exception {
		StringBuilder sb = new StringBuilder();


		sb.append("\t" + (reconHaps.getCrossOverPoints().size() - 1));
		sb.append("\t" + reconHaps.getSNPAlleleCoverage());
		sb.append("\t" + reconHaps.getUniqueHapsUsed().size());
		sb.append("\t" + reconHaps.hapsUsedAsString(true));
		sb.append("\t" + reconHaps.getConsensusString());


		// output a consensusHumanReadable
		if(groundTruthAlignment != null) {
			ArrayList<HaplotypeBlock> groundTruthHaploBlocks = groundTruthAlignment.getHaploBlocks();
			Reconstructor reconstructor = new Reconstructor();
			String reconString = reconHaps.getConsensusString();

			ReconstructedConsensus reconGroundTruthEval = reconstructor.evaluateConsensus(groundTruthAlignment, reconString, false);
			sb.append("\t" + (reconGroundTruthEval.getCrossOverPoints().size() - 1));
			sb.append("\t" + reconGroundTruthEval.getUniqueHapsUsed().size());
			sb.append("\t" + reconGroundTruthEval.hapsUsedAsString(false));

		}
		return sb.toString();
	}


	public void printAndEvaluateMajorityVoteConsensus(MultipleAlignment alignment, MultipleAlignment groundTruthAlignment, boolean hasSNPs) throws Exception {
		System.err.println(alignment.getName() + ": Computing and evaluating Majority Vote consensus...");
		ArrayList<HaplotypeBlock> haploBlocks = alignment.getHaploBlocks();
		Reconstructor reconstructor = new Reconstructor();

		String majVote = alignment.majorityVoteConsensus();
		ReconstructedConsensus reconMajEval = reconstructor.evaluateConsensus(alignment, majVote, false);

		StringBuilder sb = new StringBuilder();

		sb.append(">" + alignment.getName() + "_M");
		sb.append("\t" + alignment.getName());
		sb.append("\tMajVote");
		sb.append(constructConsensusStats(alignment, groundTruthAlignment, reconMajEval));


		System.out.println(sb.toString());
	}


	public void printAndEvaluateGivenContigs(MultipleAlignment alignment, MultipleAlignment groundTruthAlignment) throws Exception {
		System.err.println(alignment.getName() + ": Computing and evaluating the given contig consensus...");
		ArrayList<HaplotypeBlock> haploBlocks = alignment.getHaploBlocks();
		Reconstructor reconstructor = new Reconstructor();

		String givenConsensus = alignment.getGivenConsensus();
		ReconstructedConsensus reconGivenEval = reconstructor.evaluateConsensus(alignment, givenConsensus, true);

		StringBuilder sb = new StringBuilder();

		sb.append(">" + alignment.getName() + "_C");
		sb.append("\t" + alignment.getName());
		sb.append("\tContig");
		sb.append(constructConsensusStats(alignment, groundTruthAlignment, reconGivenEval));

		System.out.println(sb.toString());

	}



	/**
	 * Returns a multiple alignment based on the sequences described in the --ground-truth fasta file.
	 * This includes sequences and snps (called with the simple snp caller)
	 * @return
	 * @throws Exception
	 */
	public MultipleAlignment constructSingleAlignmentFromFasta(String fastaFile, String name) throws Exception {
		AbstractSNPListCreator groundTruthSnpCaller = new BasicAlignmentSNPListCreator();
		ArrayList<MultipleAlignment> alignmentList = new FastaMultipleAlignmentParser().openFile(fastaFile);
		if(alignmentList.size() == 0) {
			throw new Exception("I'm attempting to reconstruct an alignment from a fasta file named " + fastaFile + ", but the parser didn't return anything. Guru meditation error?");
		}
		MultipleAlignment newAlignment = alignmentList.get(0);
		ArrayList<SNP> groundTruthSnpList = groundTruthSnpCaller.computeSNPList(newAlignment);
		newAlignment.addSNPs(groundTruthSnpList);
		newAlignment.setName(name);

		return newAlignment;

	}

	public void printInfoKey() {
		System.out.println("## > Col 1: Unique consensus sequence identifier (the alignment named prepended to an H, M, or C)");
		System.out.println("## > Col 2: Alignment Name");
		System.out.println("## > Col 3: Consensus type (Hapler, MajVote, or evaluated Contig)");
		System.out.println("## > Col 4: Minimized number of crossovers necessary as reconstructed from Hapler haplotype regions");
		System.out.println("## > Col 5: Maximized total support of SNP alleles from Hapler haplotype regions used (maximized secondarily to minimizing crossovers; summation over SNPs of the number of reads (in haplotypes crossed into) agreeing with those SNPs in the consensus)");
		System.out.println("## > Col 6: Minimized number of unique Hapler haplotype regions used (minimized secondary to 1) minimizing number of crossovers and 2) maximizing support)");
		System.out.println("## > Col 7: Hapler haplotype regions used to create consensus optimizing above (start;averageCoverage)");
		System.out.println("## > Col 8: Consensus Sequence");
		System.out.println("## > Col 9: (if using --ground-truth) Minimized number of crossovers necessary as reconstruced from ground truth haplotypes");
		System.out.println("## > Col 10: (if using --ground-truth) Minimized number of unique ground truth haplotypes used (minimized secondarily to minimizing crossovers; [note that coverage will always = number of snps when computing against ground truth, as each ground truth haplotype consists of a single sequence])");
		System.out.println("## > Col 11: (if using --ground-truth) Ground truth haplotypes used to create consensus optimizing above (start)");


		System.out.println("## Col 1: Unique haplotype region identifier");
		System.out.println("## Col 2: Multiple Alignment Name");
		System.out.println("## Col 3: Multiple Alignment Length");
		System.out.println("## Col 4: Haplotype Block Number within current Multiple Alignment ('U' For the Universal Block)");
		System.out.println("## Col 5: Minimum number of haplotypes this block can support (ie, if --num-repetitions is 1, this number will equal the number of haplotypes returned)");
		System.out.println("## Col 6: Number of repetitions completed (will vary by haplotype block if --num-repetitions is 'auto', else will be equal to --num-repetitions. Will always be less than --max-repetitions.)");
		System.out.println("## Col 7: Haplotype Number withing current Haplotype Block ('U' for the Universal Haplotype)");
		System.out.println("## Col 8: Number Non-Redundant Sequences in Haplotype");
		System.out.println("## Col 9: Number Redundant Sequences in Haplotype (Sequences whose SNP positions are subsets of another sequence's SNP positions.)");
		System.out.println("## Col 10: Number of defined SNPs covered by Haplotype");
		//System.out.println("##Col 10: (Unused) Number of SNPs which are inconsisten within this haplotype: this will be 0 unless --allow-gaps true is set and sequences are gapped [e.g. mate-pairs].) Currently unused.");
		System.out.println("## Col 11: Number of defined SNPs not covered by Haplotype");
		System.out.println("## Col 12: Start position of Haplotype (0-indexed, -1 if no sequences are in the haplotype [usually only true for the Universal Haplotype])");
		System.out.println("## Col 13: End position of Haplotype (0-indexed, -1 if no sequences are in the haplotype [usually only true for the Universal Haplotype])");
		System.out.println("## Col 14: Length of Haplotype (End Position - Start Position + 1, unless no sequences are in the haplotype, in which case 0)");
		System.out.println("## Col 15: Number of 'pieces' the Haplotype is in (Just because two sequences are in the same haplotype doesn't mean they agree at any SNP position, or even overlap. A 'piece' is defined as a connected component in the 'agrees at a SNP position' graph.)");
		System.out.println("## Col 16: Average sequence coverage of the haplotype, from first non-'~' position to last non-'~' position, including redundant sequences. (Note that if a haplotype is in more than one 'piece,' some columns may not be covered at all, and hence have column coverage of 0.)");
		System.out.println("## Col 17: Total number of bases in this haplotype covering SNP positions.");
		System.out.println("## Col 18: Coverage of this haplotype (number of non-'~' characters in the consensus)");
		System.out.println("## Col 19: Haplotype Consensus, showing only SNP positions.");
		System.out.println("## Col 20: Haplotype Consensus (majority vote of entire alignment at non-SNP positions, majority (unanimous) vote of SNP loci within haplotype)");
		System.out.println("## Col 21: Reads in Haplotype (+ denotes redundant read, number in ()'s is start of sequence in alignment)");
		System.out.println("## Col 22: (if using --ground-truth) Number of ground-truth sequences that the haplotype consensus exactly matches (where ~ chars can match anything)");
		System.out.println("## Col 23: (if using --ground-truth) Number of mismatches (at non-~ characters) between the haplotype consensus and each ground truth sequence.");
		System.out.println("## Col 24: (if using --ground-truth) Minimum mismatch count of column 23.");
		System.out.println("## Col 25: (if using --ground-truth) Ground truth haplotype achieving the minimum mismatch count of column 23.");
	}

	public void printSummaryInfo(Haplotyper hapler, MultipleAlignment alignment, MultipleAlignment groundTruthAlignment, boolean humanReadable) {
		System.err.println("Printing summary info...");

		ArrayList<SNP> snpList = alignment.getSNPs();
		StringBuilder condensedSNPsb = new StringBuilder();
		for(int j = 0; j < snpList.size(); j++) {
			condensedSNPsb.append("*");
		}
		String consensedSNPString = condensedSNPsb.toString();

		if(humanReadable) {
			System.out.println("#*" + alignment.getName() + "_SNPs\t" + alignment.getName() + "\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t" + consensedSNPString + "\t" + alignment.SNPsString() + "\t" + "CalledSNPs:"+alignment.SNPsAsString());
		}

		ArrayList<HaplotypeBlock> haploBlocks = hapler.getHaplotypeBlocks();
		for(int i = 0; i < haploBlocks.size(); i++) {
			HaplotypeBlock blocki = haploBlocks.get(i);
			ArrayList<Haplotype> haps = blocki.getHaplotypes();
			for(int j = 0; j < haps.size(); j++) {
				Haplotype hapj = haps.get(j);
				StringBuilder sb = new StringBuilder();
				sb.append("@");
				if(blocki.getName().equals("U")) sb.append("@");

				sb.append(hapj.fullName() + "\t");
				sb.append(alignment.getName() + "\t");
				sb.append(alignment.getLength() + "\t");
				sb.append(blocki.getName() + "\t");
				sb.append(blocki.getMinColors() + "\t");
				sb.append(blocki.getRepsToFinish() + "\t");
				sb.append(hapj.getName() + "\t");
				sb.append(hapj.numNonRedundantSequences() + "\t");
				sb.append(hapj.numRedundantSequences() + "\t");
				sb.append(hapj.numSNPsCovered() + "\t");
				sb.append(snpList.size() - hapj.numSNPsCovered() + "\t");
				sb.append(hapj.startPos() + "\t");
				sb.append(hapj.endPos() + "\t");
				sb.append(hapj.length() + "\t");
				sb.append(hapj.numPieces() + "\t");
				sb.append(String.format("%.2f", hapj.averageCoverage()) + "\t");
				sb.append(hapj.coverageOfAllSNPs() + "\t");
				sb.append(hapj.numBasesCovered() + "\t");
				sb.append(hapj.getConsensusAtAllSNPs() + "\t");
				if(humanReadable) {
					sb.append(hapj.consensusHumanReadable() + "\t");
				}
				else {
					sb.append(hapj.consensusTrimmed() + "\t");
				}
				sb.append(hapj.sequenceNamesAsString());

				if(groundTruthAlignment != null) {
					ArrayList<Sequence> groundTruthSequences = groundTruthAlignment.getSequences();
					HashMap<Sequence, Integer> misMatchCounts = hapj.mismatchCounts(groundTruthSequences);
					int perfectMatches = 0;
					for(Sequence seq : misMatchCounts.keySet()) {
						if(misMatchCounts.get(seq) == 0) perfectMatches = perfectMatches + 1;
					}
					sb.append("\t" + perfectMatches + "\t");
					int minMismatch = Integer.MAX_VALUE;
					String minMismatchName = "";
					for(Sequence seq : misMatchCounts.keySet()) {
						sb.append(seq.getName() + "(" + misMatchCounts.get(seq) + ")" + ",");
						if(misMatchCounts.get(seq) < minMismatch) {
							minMismatch = misMatchCounts.get(seq);
							minMismatchName = seq.getName();
						}
					}
					sb.deleteCharAt(sb.length()-1);

					sb.append("\t" + minMismatch + "\t" + minMismatchName);
				}


				System.out.println(sb.toString());
			}
		}
	}

	public int parseOptions(String[] argv) {
		OptionParser optionParser = new OptionParser();
		optionParser.accepts("input").withRequiredArg().defaultsTo("-").describedAs("Input file to read. If not specified, or specified as -, read from standard input.");
		optionParser.accepts("alignment-type").withRequiredArg().defaultsTo("tigr").describedAs("Multiple Alignment type: tigr or ...");
		//optionParser.accepts("allow-gaps").withOptionalArg().defaultsTo("false").describedAs("One of false, split, true. The default option, false, ensures that no gaps (unknown bases, such as those between base pairs) are allowed. If sequences do have haps, you may either split sequences at gaps into seperate sequences (e.g., ignoring mate-pair information), or allow gaps in sequences. If gaps area allowed, the algorithm _may_ produce incorrect results, in the form of internally inconsistent haplotypes. These will be marked, in this case.");
		optionParser.accepts("help").withOptionalArg().describedAs("Show help text.");
		optionParser.accepts("maximize-one-read-haps").withOptionalArg().defaultsTo("true").describedAs("Maximize the number of haplotypes containing only one non-redundant sequence: true or false. Defaults to true.");
		optionParser.accepts("random-repetitions").withOptionalArg().defaultsTo("auto").describedAs("Number of times to randomize sequence order and re-run the bipartite matching/coloring; the comonalities amongst colorings are kept, thus this option controls the conservativeness of the haplotype predictions. If 'auto' (the default) is used, the coloring will be repeated until no functionally new colorings are discovered for 10 repetitions or 10% of repetitions, whichever is smaller.");
		optionParser.accepts("max-repetitions").withOptionalArg().defaultsTo("100").describedAs("Maximum number of repetitions that will be run, regardless of --random-repetitions.");
		optionParser.accepts("binomial-error-rate").withOptionalArg().defaultsTo("0.005").describedAs("The sequencing error rate to assume when calling SNPs with the binomial SNP caller.");
		optionParser.accepts("binomial-alpha").withOptionalArg().defaultsTo("0.05").describedAs("The alpha value to use when calling SNPs with the binomial SNP caller. Significance is determined on a per-SNP basis, and is multiply corrected by the number of tests run (which equals the length of the alignment).");
		optionParser.accepts("max-repetitions").withOptionalArg().defaultsTo("100").describedAs("Maximum number of repetitions that will be run, regardless of --random-repetitions.");
		optionParser.accepts("snp-caller").withRequiredArg().defaultsTo("simple").describedAs("SNP caller to use: simple, simplestrict, 454 or binomial. Defaults to binomial.");
		optionParser.accepts("snp-list").withRequiredArg().describedAs("File containing a column of integers, where each integer specifies a position which should be considered a SNP in the multiple alignment (0 indexed). Overrides --snp_caller.");
		optionParser.accepts("ground-truth").withOptionalArg().describedAs("Given a fasta file of the actual, full haplotypes the data was drawn from, hapler will output extra data describing how each haplotype reconstruction matches them.");
		optionParser.accepts("evaluate-contigs").withOptionalArg().describedAs("Given a fasta file of contigs with ids matching multiple alignment names in the input, reports how 'good' that contig is, similar to how the consensus is reconstructed and evaluated. It is assumed the start of the contigs corresponds to the start of the alignment (position 0).");
		optionParser.accepts("compute-reconstructions").withOptionalArg().defaultsTo("true").describedAs("If this is set to 'false,' no consensus reconstructions will be computed. This can save computational time.");
		optionParser.accepts("human-readable").withOptionalArg().defaultsTo("true").describedAs("If this is true, the output is more or less human readable (haplotype regions are aligned against each other), if this is false, this is not the case, which can drastically reduce the amount of characters that need to be output");
		optionParser.accepts("show-alignments").withOptionalArg().describedAs("Outputs the alignments in human-friendly format.");
		optionParser.accepts("version").withOptionalArg().describedAs("Show version information.");

		options = optionParser.parse(argv);



		if (options.has("help")) {
			try {
				System.out.println(helpText);
				System.out.println();
				System.out.println();
				optionParser.printHelpOn(System.out);
				return -1;
			} catch (Exception e) {
				//do nothing. jerk.
			}
		}
		else if(options.has("version")) {
			//System.out.println("Hapler version " + version );
			FunStuff fun = new FunStuff();
			System.out.println(fun.face2(version));
			return -1;
		}

		return 0;
	}

	private ArrayList<MultipleAlignment> parseAlignments(OptionSet options) throws Exception {
		// Make a new multiple alignment alignmentParser and have it parse the input

		System.err.println("Parsing input...");

		AbstractMultipleAlignmentParser alignmentParser = null;



		String parserType = (String) options.valueOf("alignment-type");
		if (parserType.compareTo("tigr") == 0) {
			alignmentParser = new TIGRMultipleAlignmentParser();
		}
		else if(parserType.compareTo("sam") == 0) {
			alignmentParser = new SAMMultipleAlignmentParser();
		}

		/*String allowGaps = (String)options.valueOf("allow-gaps");
		if(allowGaps.compareTo("false") != 0 && allowGaps.compareTo("split") != 0) {
			throw new Exception("Sorry, only 'false' and 'split' are currently allowed for the '--allow-gaps' option. Allowing for gaps (at the user's own risk!) will be implimented in future versions.");
		}*/

		String input = (String)options.valueOf("input");
		if(input.compareTo("-") == 0) {
			System.err.println("Waiting for input on standard in... see --help for help.");
		}


		// parse the multiple alignment
		ArrayList<MultipleAlignment> alignmentList = alignmentParser.openFile(input);

		// parse the contigs for evaluation
		if(options.has("evaluate-contigs")) {
			String evaluateContigsFileName = (String)options.valueOf("evaluate-contigs");
			FastaParser parser = new FastaParser();
			HashMap<String, Sequence> idsToSeqs = parser.openFile(evaluateContigsFileName);
			// associate the contigs with the alignments... yeah, I know this is like O(n^2)... TODO
			for(String id : idsToSeqs.keySet()) {
				String consensus = idsToSeqs.get(id).sequenceToString(true);
				for(MultipleAlignment alignment : alignmentList) {
					if(id.equals(alignment.getName())) {
						alignment.setGivenConsensus(consensus);
					}
				}
			}
		}
		System.err.println("Input Parsed...");

		return alignmentList;
	}

	public void computeAndAddSNPs(OptionSet options, ArrayList<MultipleAlignment> alignmentList) throws Exception {

		AbstractSNPListCreator snpListCreator = null;
		if (options.has("snp-list")) {
			// TODO: Impliment this...
			String snpFile = (String)options.valueOf("snp-list");
			snpListCreator = new UserGenSNPListCreator(snpFile);
		}
		else {
			String snpCaller = (String) options.valueOf("snp-caller");
			if (snpCaller.compareTo("454") == 0) { // create a new simple or 454 creator as
				snpListCreator = new Basic454SNPListCreator();
			}
			else if(snpCaller.compareTo("simplestrict") == 0) {
				snpListCreator = new BasicStrictAlignmentSNPListCreator();
			}
			else if(snpCaller.compareTo("binomial") == 0) {
				String errorString = (String)options.valueOf("binomial-error-rate");
				String alphaString = (String)options.valueOf("binomial-alpha");
				snpListCreator = new BinomialSNPListCreator(Double.parseDouble(errorString), Double.parseDouble(alphaString));
			}
			else {
				// create a simple caller by default
				snpListCreator = new BasicAlignmentSNPListCreator();
			}
		}

		for(int i = 0; i < alignmentList.size(); i++) {
			MultipleAlignment alignment = alignmentList.get(i);
			System.err.println(alignment.getName() + ": Calling SNPs...");
			ArrayList<SNP> snpList = snpListCreator.computeSNPList(alignment);
			int numBiAllelic = 0;
			for(int j = 0; j < snpList.size(); j++) {
				SNP snpj = snpList.get(j);
				if(snpj.isBiAllelic()) {
					numBiAllelic = numBiAllelic + 1;
				}
			}
			System.err.println(alignment.getName() + ": Called " + snpList.size() + " SNPs, " + numBiAllelic + " are bi-allelic");
			alignment.addSNPs(snpList);
		}
	}

	private boolean parseComputeRecons(OptionSet options) {
		boolean computeRecons = true;
		if(options.has("compute-reconstructions")) {
			String computeReconString = (String) options.valueOf("compute-reconstructions");
			if(computeReconString.equals("false")) {
				computeRecons = false;
			}
		}
		return computeRecons;
	}

	private boolean parseHumanReadable(OptionSet options) {
		boolean humanReadable = true;
		if(options.has("human-readable")) {
			String computeReconString = (String) options.valueOf("human-readable");
			if(computeReconString.equals("false")) {
				humanReadable = false;
			}
		}
		return humanReadable;
	}

	public boolean parseEpsilon(OptionSet options) {
		// Decide whether to use the trick of maximizing the single read haps
		boolean useEpsilon = true;
		if(options.has("maximize-one-read-haps")) {
			String useEpsilonString = (String)options.valueOf("maximize-one-read-haps");
			if(useEpsilonString.compareTo("false") == 0 || useEpsilonString.compareTo("f") == 0) {
				useEpsilon = false;
			}
		}
		return useEpsilon;
	}

	private int parseNumReps(OptionSet options) {
		int numReps = -1; // default to auto
		if(options.has("random-repetitions")) {
			String randomReps = (String)options.valueOf("random-repetitions");
			if(randomReps.compareTo("auto") == 0) {
				numReps = -1;
			}
			else {
				numReps = Integer.parseInt(randomReps);
			}
		}
		return numReps;
	}

	private int parseMaxReps(OptionSet options) {
		int maxReps = 100;
		if(options.has("max-repetitions")) {
			String maxRepsString = (String)options.valueOf("max-repetitions");
			maxReps = Integer.parseInt(maxRepsString);
		}
		return maxReps;
	}
}
