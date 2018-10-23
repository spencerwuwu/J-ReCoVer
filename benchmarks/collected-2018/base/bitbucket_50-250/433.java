// https://searchcode.com/api/result/126660470/

package indexing.index.test;

import indexing.map.MapperMain;
import indexing.map.MapperOutput;
import indexing.reduce.ReducerMain;
import indexing.reduce.ReducerOutput;
import junit.framework.TestCase;

import java.io.*;

/**
 * Tests running the mapper and reducer to build an index on the whole input file.
 */
public class EndToEndTest extends TestCase {
  private static final int BUFFER_SIZE = 8*1024*1024;
  private static final String INPUT_FILE = "500k_names.txt";
  private static final String MAPPER_OUTPUT = "mapper.out";
  private static final String MAPPER_OUTPUT_SORTED = "mapper.out.sorted";
  private static final String REDUCER_OUTPUT = "index";

  public void testEnd2End() throws IOException {
    BufferedReader mapperInput = new BufferedReader(new FileReader(INPUT_FILE), BUFFER_SIZE);
    new MapperMain().run(mapperInput, new FileMapperOutput(MAPPER_OUTPUT));

 //   Process process = new ProcessBuilder().command("/bin/bash cat mapper.out | sort > mapper.out.sorted").start();

    BufferedReader reducerInput = new BufferedReader(new FileReader(MAPPER_OUTPUT_SORTED), BUFFER_SIZE);
    new ReducerMain().run(reducerInput, ReducerOutput.indexedFileOutput(REDUCER_OUTPUT));
  }

  private class FileMapperOutput extends MapperOutput {
    private final BufferedWriter writer;

    public FileMapperOutput(String filename) throws IOException {
      writer = new BufferedWriter(new FileWriter(filename), BUFFER_SIZE);
    }

    @Override
    public void write(String key, String value) throws IOException {
      writer.write(key + " " + value);
      writer.newLine();
    }

    @Override
    public void close() throws IOException {
      writer.close();
    }
  }
}

