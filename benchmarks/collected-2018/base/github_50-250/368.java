// https://searchcode.com/api/result/73637493/

package io.ssc.dogfood.preprocess;

import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.io.RecordOutputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactLong;

import java.util.Iterator;

public class Uniquify implements PlanAssembler {

  public Plan getPlan(String... args) {
    int noSubTasks   = (args.length > 0 ? Integer.parseInt(args[0]) : 1);
    String dataInput = (args.length > 1 ? args[1] : "");
    String output    = (args.length > 2 ? args[2] : "");

    FileDataSource source = new FileDataSource(EdgeListInputFormat.class, dataInput, "EdgeListInput");

    MapContract edgeMap = MapContract.builder(VertexIDMap.class)
        .input(source)
        .build();

    ReduceContract reducer = new ReduceContract.Builder(UniqueVertexIDReducer.class, PactLong.class,
        Positions.VERTEX_ID)
        .input(edgeMap)
        .build();

    FileDataSink out = new FileDataSink(RecordOutputFormat.class, output, reducer, "AdjacencyList");
    RecordOutputFormat.configureRecordFormat(out)
        .recordDelimiter('\n')
        .fieldDelimiter(' ')
        .lenient(true)
        .field(PactLong.class, Positions.VERTEX_ID);

    Plan plan = new Plan(out);
    plan.setDefaultParallelism(noSubTasks);
    return plan;
  }

  public static class VertexIDMap extends MapStub {

    PactRecord result = new PactRecord();

    @Override
    public void map(PactRecord record, Collector<PactRecord> collector) throws Exception {
      result.setField(Positions.VERTEX_ID, record.getField(Positions.VERTEX_ID, PactLong.class));
      collector.collect(result);
      result.setField(Positions.VERTEX_ID, record.getField(Positions.ADJACENT_VERTEX_ID, PactLong.class));
      collector.collect(result);
    }
  }

  public static class UniqueVertexIDReducer extends ReduceStub {

    private PactRecord result = new PactRecord();

    @Override
    public void reduce(Iterator<PactRecord> records, Collector<PactRecord> collector) throws Exception {

      PactRecord first = records.next();
      result.setField(Positions.VERTEX_ID, first.getField(Positions.VERTEX_ID, PactLong.class));

      collector.collect(result);
    }
  }

}

