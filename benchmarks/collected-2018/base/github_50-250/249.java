// https://searchcode.com/api/result/73637529/

package io.ssc.dogfood.preprocess.pagerank;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.contract.FileDataSource;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.io.RecordOutputFormat;
import eu.stratosphere.pact.common.io.TextInputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactLong;
import eu.stratosphere.pact.common.type.base.PactString;
import io.ssc.dogfood.preprocess.BooleanValue;
import io.ssc.dogfood.preprocess.EdgeListInputFormat;
import io.ssc.dogfood.preprocess.Positions;

import java.util.Iterator;
import java.util.regex.Pattern;

public class CreateMarkedVertexList implements PlanAssembler {

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
      .name("UniqueVertexIDReducer")
      .input(edgeMap)
      .build();

    FileDataSink out = new FileDataSink(RecordOutputFormat.class, output, reducer, "Output");
    RecordOutputFormat.configureRecordFormat(out)
        .recordDelimiter('\n')
        .fieldDelimiter(' ')
        .lenient(true)
        .field(PactLong.class, Positions.VERTEX_ID)
        .field(PactString.class, 2);

    Plan plan = new Plan(out, "CreateMarkedVertexList");
    plan.setDefaultParallelism(noSubTasks);
    return plan;
  }

  public static class VertexIDMap extends MapStub {

    PactRecord result;

    @Override
    public void open(Configuration parameters) throws Exception {
      result = new PactRecord();
    }

    @Override
    public void map(PactRecord record, Collector<PactRecord> collector) throws Exception {
      result.setField(Positions.VERTEX_ID, record.getField(Positions.VERTEX_ID, PactLong.class));
      result.setField(Positions.COULD_BE_DANGLING, new BooleanValue(false));
      collector.collect(result);
      result.setField(Positions.VERTEX_ID, record.getField(Positions.ADJACENT_VERTEX_ID, PactLong.class));
      result.setField(Positions.COULD_BE_DANGLING, new BooleanValue(true));
      collector.collect(result);
    }
  }

  public static class UniqueVertexIDReducer extends ReduceStub {

    private PactRecord result;

    @Override
    public void open(Configuration parameters) throws Exception {
      result = new PactRecord();
    }

    @Override
    public void close() throws Exception {
      result = null;
    }

    @Override
    public void reduce(Iterator<PactRecord> records, Collector<PactRecord> collector) throws Exception {

      PactRecord first = records.next();
      result.setField(Positions.VERTEX_ID, first.getField(Positions.VERTEX_ID, PactLong.class));

      boolean couldBeDangling = true;

      couldBeDangling &= first.getField(Positions.COULD_BE_DANGLING, BooleanValue.class).get();

      while (records.hasNext()) {
        PactRecord record = records.next();
        couldBeDangling &= record.getField(Positions.COULD_BE_DANGLING, BooleanValue.class).get();
      }

      result.setField(2, new PactString(couldBeDangling ? "1" : ""));

      collector.collect(result);
    }
  }

}

