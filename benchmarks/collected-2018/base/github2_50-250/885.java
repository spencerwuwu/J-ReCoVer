// https://searchcode.com/api/result/95934156/


package no.priv.garshol.duke;

import java.io.IOException;

import org.xml.sax.SAXException;

import no.priv.garshol.duke.utils.CommandLineParser;

/**
 * INTERNAL: Shared code between the simplest command-line tools.
 * @since 1.2
 */
public abstract class AbstractCmdlineTool {
  protected Database database;
  protected Configuration config;
  private static final int DEFAULT_BATCH_SIZE = 40000;

  /**
   * These exact lines are shared between three different tools, so
   * they have been moved here to reduce code duplication.
   * @return The parsed command-line, with options removed.
   */
  public String[] init(String[] argv, int min, int max)
    throws IOException, SAXException {
    // parse command line
    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(min);
    parser.setMaximumArguments(max);
    parser.registerOption(new CommandLineParser.BooleanOption("reindex", 'I'));
    try {
      argv = parser.parse(argv);
    } catch (CommandLineParser.CommandLineParserException e) {
      System.err.println("ERROR: " + e.getMessage());
      usage();
      System.exit(1);
    }

    // do we need to reindex?
    boolean reindex = parser.getOptionState("reindex");
    
    // load configuration
    config = ConfigLoader.load(argv[0]);
    database = config.getDatabase(reindex); // overwrite iff reindex
    if (database.isInMemory())
      reindex = true; // no other way to do it in this case

    // reindex, if requested
    if (reindex)
      reindex(config, database);

    return argv;
  }

  protected abstract void usage();

  private static void reindex(Configuration config, Database database) {
    System.out.println("Reindexing all records...");
    Processor processor = new Processor(config, database);
    if (config.isDeduplicationMode())
      processor.index(config.getDataSources(), DEFAULT_BATCH_SIZE);
    else {
      processor.index(config.getDataSources(1), DEFAULT_BATCH_SIZE);
      processor.index(config.getDataSources(2), DEFAULT_BATCH_SIZE);
    }
  }
}
