// https://searchcode.com/api/result/48555489/

/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.journalpaper.wordfrequency.pact;

import java.util.Iterator;
import java.util.LinkedList;

import eu.stratosphere.journalpaper.common.AsciiUtils;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.pact.common.contract.CrossContract;
import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.GenericDataSink;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.Order;
import eu.stratosphere.pact.common.contract.Ordering;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.contract.ReduceContract.Combinable;
import eu.stratosphere.pact.common.io.RecordOutputFormat;
import eu.stratosphere.pact.common.io.TextInputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.plan.PlanAssemblerDescription;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.CrossStub;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFields;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFieldsExcept;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFieldsFirst;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFieldsSecond;
import eu.stratosphere.pact.common.stubs.StubAnnotation.OutCardBounds;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactDouble;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.common.type.base.PactLong;
import eu.stratosphere.pact.common.type.base.PactString;

/**
 * Implements a word count extractor which takes an input file and outputs a
 * dictionary and the relative frequency of each word in the file in output
 * files (*.domain) and (*.distribution) that can be used as an input for the {@link wordcount-gen} generator.
 * 
 * @author Alexander Alexandrov
 * @author Marcus Leich
 * @author Larysa
 * @author Moritz Kaufmann
 * @author Stephan Ewen
 */
public class WordFrequencyExtractor implements PlanAssembler, PlanAssemblerDescription {

	/**
	 * Converts a PactRecord containing one string in to multiple string/integer pairs.
	 * The string is tokenized by whitespaces. For each token a new record is emitted,
	 * where the token is the first field and an Integer(1) is the second field.
	 */
	@ConstantFields(fields = {})
	@OutCardBounds(lowerBound = 0, upperBound = OutCardBounds.UNBOUNDED)
	public static class TokenizeLine extends MapStub {
		// initialize reusable mutable objects
		private final PactRecord outputRecord = new PactRecord();

		private final PactString word = new PactString();

		// we count with negative numbers, since Stratosphere only supports acending ordering, but we need most frequent
		// words
		private final PactLong one = new PactLong(-1);

		private final PactInteger minus = new PactInteger(-1);

		private final AsciiUtils.WhitespaceTokenizer tokenizer = new AsciiUtils.WhitespaceTokenizer();

		@Override
		public void map(PactRecord record, Collector<PactRecord> collector) {
			// get the first field (as type PactString) from the record
			PactString line = record.getField(0, PactString.class);

			// normalize the line
			AsciiUtils.replaceNonWordChars(line, ' ');
			AsciiUtils.toLowerCase(line);

			// tokenize the line
			this.tokenizer.setStringToTokenize(line);
			while (this.tokenizer.next(this.word)) {
				// we emit a (word, 1) pair only if its not a DNA sample
				if (!isDNASequence(this.word)) {
					this.outputRecord.setField(0, this.word);
					this.outputRecord.setField(1, this.one);
					this.outputRecord.setField(2, this.minus);
					collector.collect(this.outputRecord);
				}
			}
		}

		/**
		 * Checks if a word is a DNA sequence.
		 * 
		 * @param word
		 * @return
		 */
		private boolean isDNASequence(PactString word) {
			if (word.length() == 50 || word.length() == 60) {
				// count letters
				if (count(word, 'a') + count(word, 'g') + count(word, 't') + count(word, 'c') >= word.length()) {
					// invalid word
					return true;
				}
			}
			return false;
		}

		private int count(PactString word, char c) {
			int sum = 0;
			int idx = 0;
			while (idx < word.length()) {
				if (word.charAt(idx) == c) {
					sum++;
				}
				idx++;
			}
			return sum;
		}
	}

	/**
	 * Simply emits all records that have a value < MAXITEMS_PARAMETER at field 2.
	 */
	@ConstantFieldsExcept(fields = {})
	@OutCardBounds(lowerBound = 0, upperBound = OutCardBounds.UNBOUNDED)
	public static class Filter extends MapStub {

		public static final String MAXITEMS_PARAMETER = "filter_maxitems";

		private int maxitems;

		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			this.maxitems = parameters.getInteger(MAXITEMS_PARAMETER, 100000);
		}

		@Override
		public void map(PactRecord record, Collector<PactRecord> collector) {
			if (record.getField(2, PactInteger.class).getValue() < this.maxitems) {
				collector.collect(record);
			}
		}
	}

	/**
	 * Sums up the counts for a certain given key. The counts are assumed to be
	 * at position <code>1</code> in the record. The other fields are not
	 * modified.
	 */
	@ConstantFields(fields = { 0, 2 })
	@OutCardBounds(lowerBound = 1, upperBound = 1)
	@Combinable
	public static class SumField extends ReduceStub {

		private final PactLong cnt = new PactLong();

		private final PactRecord outputRecord = new PactRecord(3);

		@Override
		public void reduce(Iterator<PactRecord> records, Collector<PactRecord> out) throws Exception {
			PactRecord element = this.outputRecord;
			long sum = 0;
			while (records.hasNext()) {
				element = records.next();
				PactLong i = element.getField(1, PactLong.class);
				sum += i.getValue();
			}

			this.cnt.setValue(sum);
			element.setField(1, this.cnt);
			out.collect(element);
		}

		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.ReduceStub#combine(java.util.Iterator,
		 * eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void combine(Iterator<PactRecord> records, Collector<PactRecord> out) throws Exception {
			// the logic is the same as in the reduce function, so simply call the reduce method
			this.reduce(records, out);
		}
	}

	/**
	 * Sums up the counts for a certain given key. The counts are assumed to be
	 * at position <code>1</code> in the record. The other fields are not
	 * modified.
	 */
	@ConstantFieldsFirst(fields = { 0, 2 })
	@ConstantFieldsSecond(fields = { 0 })
	@OutCardBounds(lowerBound = 1, upperBound = 1)
	public static class NormalizeFrequency extends CrossStub {

		private final PactRecord outputRecord = new PactRecord(3);

		private final PactDouble outputFrequency = new PactDouble(0.0);

		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.CrossStub#cross(eu.stratosphere.pact.common.type.PactRecord,
		 * eu.stratosphere.pact.common.type.PactRecord, eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void cross(PactRecord wordCount, PactRecord totalWords, Collector<PactRecord> collector) {
			// compute relative frequency
			double f = ((double) wordCount.getField(1, PactLong.class).getValue())
				/ ((double) totalWords.getField(1, PactLong.class).getValue());
			this.outputFrequency.setValue(f);
			// create output record
			this.outputRecord.setField(0, wordCount.getField(0, PactString.class));
			this.outputRecord.setField(1, this.outputFrequency);
			this.outputRecord.setField(2, wordCount.getField(2, PactInteger.class));
			collector.collect(this.outputRecord);
		}
	}

	@ConstantFields(fields = { 0 })
	@OutCardBounds(lowerBound = 1, upperBound = 1)
	public static class SortingReducer extends ReduceStub {

		private PactInteger currentIndex = new PactInteger(0);

		private PactLong value = new PactLong(0);

		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			this.currentIndex.setValue(0);
		}

		@Override
		public void reduce(Iterator<PactRecord> records, Collector<PactRecord> out) throws Exception {
			PactRecord element = null;
			while (records.hasNext()) {
				element = records.next();
				// switch sign of counter back to positive
				this.value.setValue(-element.getField(1, PactLong.class).getValue());
				element.setField(1, this.value);
				element.setField(2, this.currentIndex);
				out.collect(element);
				this.currentIndex.setValue(this.currentIndex.getValue() + 1);
			}
		}
	}

	@ConstantFields(fields = {})
	@OutCardBounds(lowerBound = 1, upperBound = 1)
	public static class FormatDomainField extends MapStub {
		// initialize reusable mutable objects
		private final PactRecord outputRecord = new PactRecord(1);

		private final PactString outputValue = new PactString();

		@Override
		public void map(PactRecord record, Collector<PactRecord> collector) {
			int index = record.getField(2, PactInteger.class).getValue();
			String value = record.getField(0, PactString.class).getValue();
			this.outputValue.setValue(index + " ... \"" + value + "\"");
			this.outputRecord.setField(0, this.outputValue);
			collector.collect(this.outputRecord);
		}
	}

	@ConstantFields(fields = {})
	@OutCardBounds(lowerBound = 1, upperBound = 1)
	public static class FormatDistributionField extends MapStub {
		// initialize reusable mutable objects
		private final PactRecord outputRecord = new PactRecord(1);

		private final PactString outputValue = new PactString();

		@Override
		public void map(PactRecord record, Collector<PactRecord> collector) {
			long index = record.getField(2, PactInteger.class).getValue();
			double probability = record.getField(1, PactDouble.class).getValue();
			this.outputValue.setValue("p(X) = " + probability + " for X = { " + index + " }");
			this.outputRecord.setField(0, this.outputValue);
			collector.collect(this.outputRecord);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Plan getPlan(String... args) {

		// parse job parameters
		String inputPath = (args.length > 0 ? args[0] : "");
		String outputPath = (args.length > 1 ? args[1] : "");
		int maxItems = (args.length > 2 ? Integer.parseInt(args[2]) : 100000);
		int degreeOfParallelism = (args.length > 3 ? Integer.parseInt(args[3]) : 1);

		FileDataSource source = new FileDataSource(TextInputFormat.class, inputPath, "Input Lines");
		source.setParameter(TextInputFormat.CHARSET_NAME, "ASCII"); // comment out this line for UTF-8 inputs

		MapContract mapper = MapContract.builder(TokenizeLine.class)
				.input(source)
				.name("Tokenize Lines")
				.build();

		ReduceContract countWordFrequency = ReduceContract.builder(SumField.class)
				.input(mapper)
				.name("Count Words Frequency")
				.keyField(PactString.class, 0)
				.build();

		ReduceContract globalSort = ReduceContract.builder(SortingReducer.class, PactInteger.class, 2)
				.input(countWordFrequency)
				.name("Global Sort")
				.secondaryOrder(new Ordering(1, PactLong.class, Order.ASCENDING))
				.build();

		MapContract filter = MapContract.builder(Filter.class)
				.input(globalSort)
				.name("Filter")
				.build();
		filter.setParameter(Filter.MAXITEMS_PARAMETER, maxItems);

		ReduceContract countWordsTotal = ReduceContract.builder(SumField.class)
				.input(filter)
				.name("Count Words Total")
				.build();
		countWordsTotal.setDegreeOfParallelism(1);

		CrossContract normalizeFrequency = CrossContract.builder(NormalizeFrequency.class)
				.input1(filter)
				.input2(countWordsTotal)
				.name("Normalize Frequency")
				.build();
		normalizeFrequency.setDegreeOfParallelism(1);

		MapContract formatDomainField = MapContract.builder(FormatDomainField.class)
				.input(normalizeFrequency)
				.name("Format Domain Field")
				.build();
		formatDomainField.setDegreeOfParallelism(1);

		MapContract formatDistributionField = MapContract.builder(FormatDistributionField.class)
				.input(normalizeFrequency)
				.name("Format Distribution Field")
				.build();
		formatDistributionField.setDegreeOfParallelism(1);

		FileDataSink domainOutput = new FileDataSink(RecordOutputFormat.class, outputPath + ".domain", formatDomainField, "Domain");
		RecordOutputFormat.configureRecordFormat(domainOutput)
				.recordDelimiter('\n')
				.fieldDelimiter(' ')
				.lenient(true)
				.field(PactString.class, 0);
		domainOutput.setDegreeOfParallelism(1);

		FileDataSink frequencyOutput = new FileDataSink(RecordOutputFormat.class, outputPath + ".distribution", formatDistributionField, "Distribution");
		RecordOutputFormat.configureRecordFormat(frequencyOutput)
				.recordDelimiter('\n')
				.fieldDelimiter(' ')
				.lenient(true)
				.field(PactString.class, 0);
		frequencyOutput.setDegreeOfParallelism(1);

		LinkedList<GenericDataSink> dataSinks = new LinkedList<GenericDataSink>();
		dataSinks.add(domainOutput);
		dataSinks.add(frequencyOutput);

		Plan plan = new Plan(dataSinks, "Word Frequency Extractor");
		plan.setDefaultParallelism(degreeOfParallelism);

		return plan;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Parameters: [input] [output] [noOfTopWords] [noSubStasks]";
	}

}

