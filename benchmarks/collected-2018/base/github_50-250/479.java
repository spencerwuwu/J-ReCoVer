// https://searchcode.com/api/result/73637466/

package io.ssc.dogfood.preprocess;

import eu.stratosphere.pact.common.contract.Contract;
import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.io.RecordOutputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactLong;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class EdgeListToAdjacencyList implements PlanAssembler {

  public Plan getPlan(String... args) {
    int noSubTasks   = args.length > 0 ? Integer.parseInt(args[0]) : 1;
    String dataInput = args.length > 1 ? args[1] : "";
    String output    = args.length > 2 ? args[2] : "";

    boolean symmetrify = args.length > 3 ? Boolean.parseBoolean(args[3]) : false;

    FileDataSource source = new FileDataSource(EdgeListInputFormat.class, dataInput, "EdgeListInput");

    ReduceContract reducer = new ReduceContract.Builder(ToAdjacencyListReducer.class, PactLong.class,
        Positions.VERTEX_ID)
        .input(source)
        .name("ToAdjacencyListReducer")
        .build();

    Contract lastPact = reducer;

    if (symmetrify) {
      lastPact = MapContract.builder(SymmetrifyMap.class)
          .input(reducer)
          .name("SymmetrifyMap")
          .build();
    }

    FileDataSink out = new FileDataSink(RecordOutputFormat.class, output, lastPact, "AdjacencyList");
    RecordOutputFormat.configureRecordFormat(out)
        .recordDelimiter('\n')
        .fieldDelimiter(' ')
        .lenient(true)
        .field(PactLong.class, Positions.VERTEX_ID)
        .field(PactLongArray.class, Positions.ADJACENT_VERTICES);

    Plan plan = new Plan(out, "EdgeListToAdjacencyList");
    plan.setDefaultParallelism(noSubTasks);
    return plan;
  }

  public static class ToAdjacencyListReducer extends ReduceStub {

    private PactRecord record = new PactRecord();

    @Override
    public void reduce(Iterator<PactRecord> records, Collector<PactRecord> collector) throws Exception {

      PactRecord first = records.next();
      record.setField(Positions.VERTEX_ID, first.getField(Positions.VERTEX_ID, PactLong.class));

      Set<Long> neighbors = new HashSet<Long>(100);
      neighbors.add(first.getField(Positions.ADJACENT_VERTEX_ID, PactLong.class).getValue());

      while (records.hasNext()) {
        long neighborId = records.next().getField(Positions.ADJACENT_VERTEX_ID, PactLong.class).getValue();
        neighbors.add(neighborId);
      }

      record.setField(Positions.ADJACENT_VERTICES, new PactLongArray(neighbors));

      collector.collect(record);
    }
  }
}

