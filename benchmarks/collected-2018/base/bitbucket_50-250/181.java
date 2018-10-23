// https://searchcode.com/api/result/48555493/

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

package eu.stratosphere.journalpaper.trienum.pact;

import java.util.Iterator;

import eu.stratosphere.journalpaper.trienum.pact.io.EdgeInputFormat;
import eu.stratosphere.journalpaper.trienum.pact.io.EdgeOutputFormat;
import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.io.DelimitedInputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.plan.PlanAssemblerDescription;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactInteger;

public class RemoveDuplicateEdges implements PlanAssembler, PlanAssemblerDescription {

	public static final class CanonizeEdges extends MapStub {

		private PactInteger v1 = new PactInteger();

		private PactInteger v2 = new PactInteger();

		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.MapStub#map(eu.stratosphere.pact.common.type.PactRecord,
		 * eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void map(PactRecord record, Collector<PactRecord> out) throws Exception {
			this.v1 = record.getField(0, PactInteger.class);
			this.v2 = record.getField(1, PactInteger.class);

			if (this.v1.getValue() < this.v2.getValue()) {
				record.setField(0, this.v1);
				record.setField(1, this.v2);
			} else {
				record.setField(0, this.v2);
				record.setField(1, this.v1);
			}

			record.setNumFields(2);
			out.collect(record);
		}
	}

	public static final class RemoveDuplicates extends ReduceStub {

		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.ReduceStub#reduce(java.util.Iterator,
		 * eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void reduce(Iterator<PactRecord> values, Collector<PactRecord> out) throws Exception {
			if (values.hasNext()) {
				out.collect(values.next());
			}
		}
	}

	/**
	 * Assembles the Plan of the triangle enumeration example Pact program.
	 */
	@Override
	public Plan getPlan(String... args) {
		// parse job parameters
		final int noSubTasks = (args.length > 0 ? Integer.parseInt(args[0]) : 1);
		final String edgeInput = args.length > 1 ? args[1] : "";
		final String output = args.length > 2 ? args[2] : "";

		FileDataSource edges = new FileDataSource(EdgeInputFormat.class, edgeInput, "Input Edges");
		edges.setParameter(DelimitedInputFormat.RECORD_DELIMITER, "\n");
		edges.setDegreeOfParallelism(noSubTasks);

		// =========================== Triangle Enumeration ============================

		MapContract canonicEdge = MapContract.builder(CanonizeEdges.class)
				.input(edges)
				.name("Canonize edges")
				.build();

		ReduceContract removeDuplicates = ReduceContract.builder(RemoveDuplicates.class, PactInteger.class, 0)
				.keyField(PactInteger.class, 1)
				.input(canonicEdge)
				.name("Remove Duplicates")
				.build();

		FileDataSink triangles = new FileDataSink(EdgeOutputFormat.class, output, removeDuplicates, "Triangles");
		triangles.setDegreeOfParallelism(noSubTasks);

		return new Plan(triangles, "Remove Duplicate Edges");
	}

	/*
	 * (non-Javadoc)
	 * @see eu.stratosphere.pact.common.plan.PlanAssemblerDescription#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Parameters: [noSubStasks] [input file] [output file]";
	}
}

