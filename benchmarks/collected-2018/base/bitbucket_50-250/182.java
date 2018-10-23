// https://searchcode.com/api/result/48555494/

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.stratosphere.journalpaper.trienum.pact.io.EdgeInputFormat;
import eu.stratosphere.journalpaper.trienum.pact.io.TriangleOutputFormat;
import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.MatchContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.io.DelimitedInputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.plan.PlanAssemblerDescription;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.MatchStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFieldsExcept;
import eu.stratosphere.pact.common.stubs.StubAnnotation.ConstantFieldsFirstExcept;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactInteger;

public class EnumTriangles implements PlanAssembler, PlanAssemblerDescription {

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
//				record.setField(0, this.v1);
//				record.setField(1, this.v2);
			} else {
				record.setField(0, this.v2);
				record.setField(1, this.v1);
			}

			record.setNumFields(2);
			out.collect(record);
		}
	}

	public static final class BuildTriads extends ReduceStub {

		private PactInteger lowerVertex = new PactInteger();

		private PactInteger higherVertex = new PactInteger();

		private final List<Integer> seenNodes = new ArrayList<Integer>();

		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.ReduceStub#reduce(java.util.Iterator,
		 * eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void reduce(Iterator<PactRecord> values, Collector<PactRecord> out) throws Exception {
			// clear seen nodes
			this.seenNodes.clear();

			while (values.hasNext()) {
				// get current node from value
				PactRecord triad = values.next();
				int v1 = triad.getField(1, PactInteger.class).getValue();
				// pair the current node with all 'seen' nodes
				for (int v2 : this.seenNodes) {
					if (v1 < v2) {
						this.lowerVertex.setValue(v1);
						this.higherVertex.setValue(v2);
					} else {
						this.lowerVertex.setValue(v2);
						this.higherVertex.setValue(v1);
					}
					// set triad contents and output it
					triad.setField(1, this.lowerVertex);
					triad.setField(2, this.higherVertex);
					triad.setNumFields(3);
					out.collect(triad);
				}
				// add to seen nodes
				this.seenNodes.add(v1);
			}
		}
	}

	public static class CloseTriads extends MatchStub {
		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.MatchStub#match(eu.stratosphere.pact.common.type.PactRecord,
		 * eu.stratosphere.pact.common.type.PactRecord, eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void match(PactRecord value1, PactRecord value2, Collector<PactRecord> out) throws Exception {
			out.collect(value1);
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

		ReduceContract buildTriads = ReduceContract.builder(BuildTriads.class, PactInteger.class, 0)
				.input(canonicEdge)
				.name("Build Triads")
				.build();

		MatchContract closeTriads = MatchContract.builder(CloseTriads.class, PactInteger.class, 1, 0)
				.keyField(PactInteger.class, 2, 1)
				.input1(buildTriads)
				.input2(canonicEdge)
				.name("Close Triads")
				.build();
		closeTriads.setParameter("LOCAL_STRATEGY", "LOCAL_STRATEGY_HASH_BUILD_SECOND");
		closeTriads.setParameter("INPUT_LEFT_SHIP_STRATEGY", "SHIP_REPARTITION");
		closeTriads.setParameter("INPUT_RIGHT_SHIP_STRATEGY", "SHIP_REPARTITION");

		FileDataSink triangles = new FileDataSink(TriangleOutputFormat.class, output, closeTriads, "Triangles");

		Plan p = new Plan(triangles, "Enumerate Triangles");
		p.setDefaultParallelism(noSubTasks);
		return p;
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

