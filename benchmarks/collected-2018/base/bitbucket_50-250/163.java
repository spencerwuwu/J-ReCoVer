// https://searchcode.com/api/result/48555490/

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

package eu.stratosphere.journalpaper.wordcount.pact;

import java.util.Iterator;
import java.util.StringTokenizer;

import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.contract.ReduceContract.Combinable;
import eu.stratosphere.pact.common.io.RecordOutputFormat;
import eu.stratosphere.pact.common.io.TextInputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.plan.PlanAssemblerDescription;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFields;
import eu.stratosphere.pact.common.stubs.StubAnnotation.OutCardBounds;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.common.type.base.PactString;

/**
 * Implements a word count which takes the input file and counts the number of
 * the occurrences of each word in the file.
 * 
 * @author Larysa, Moritz Kaufmann, Stephan Ewen
 */
public class WordCountWithJavaStringTokenizer implements PlanAssembler, PlanAssemblerDescription {

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

		private final PactInteger one = new PactInteger(1);

		@Override
		public void map(PactRecord record, Collector<PactRecord> collector) {
			// get the first field (as type PactString) from the record
			PactString line = record.getField(0, PactString.class);

			// normalize the line
//			AsciiUtils.replaceNonWordChars(line, ' ');
//			AsciiUtils.toLowerCase(line);

			StringTokenizer tokenizer = new StringTokenizer(line.getValue());
			// tokenize the line
			while (tokenizer.hasMoreTokens()) {
				// we emit a (word, 1) pair
				this.word.setValue(tokenizer.nextToken());
				this.outputRecord.setField(0, this.word);
				this.outputRecord.setField(1, this.one);
				collector.collect(this.outputRecord);
			}
		}
	}

	/**
	 * Sums up the counts for a certain given key. The counts are assumed to be at position <code>1</code> in the
	 * record. The other fields are not modified.
	 */
	@ConstantFields(fields = { 0 })
	@OutCardBounds(lowerBound = 1, upperBound = 1)
	@Combinable
	public static class CountWords extends ReduceStub {
		private final PactInteger cnt = new PactInteger();

		@Override
		public void reduce(Iterator<PactRecord> records, Collector<PactRecord> out) throws Exception {
			PactRecord element = new PactRecord();
			int sum = 0;
			while (records.hasNext()) {
				element = records.next();
				sum += element.getField(1, PactInteger.class).getValue();
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
	 * {@inheritDoc}
	 */
	@Override
	public Plan getPlan(String... args) {

		if (args.length != 3) {
			throw new RuntimeException("Usage: wordcount-job.jar [input] [output]");
		}

		// parse job parameters
		int noSubTasks = (args.length > 0 ? Integer.parseInt(args[0]) : 1);
		String input = (args.length > 1 ? args[1] : "");
		String output = (args.length > 2 ? args[2] : "");

		FileDataSource source = new FileDataSource(TextInputFormat.class, input, "Input Lines");
		source.setParameter(TextInputFormat.CHARSET_NAME, "ASCII");		// comment out this line for UTF-8 inputs
		MapContract mapper = MapContract.builder(TokenizeLine.class)
			.input(source)
			.name("Tokenize Lines")
			.build();
		ReduceContract reducer = new ReduceContract.Builder(CountWords.class, PactString.class, 0)
			.input(mapper)
			.name("Count Words")
			.build();
		FileDataSink out = new FileDataSink(RecordOutputFormat.class, output, reducer, "Word Counts");
		RecordOutputFormat.configureRecordFormat(out)
			.recordDelimiter('\n')
			.fieldDelimiter(' ')
			.lenient(true)
			.field(PactString.class, 0)
			.field(PactInteger.class, 1);

		Plan plan = new Plan(out, "WordCount Example (JST)");
		plan.setDefaultParallelism(noSubTasks);
		return plan;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Parameters: [noSubStasks] [input] [output]";
	}

}

