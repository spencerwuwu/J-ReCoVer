// https://searchcode.com/api/result/43773122/

package edu.cmu.cs.ark.sage.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.ParseException;
import com.martiansoftware.jsap.Switch;

import edu.cmu.cs.ark.sage.SAGE;
import edu.cmu.cs.ark.sage.effects.Effect;
import edu.cmu.cs.ark.sage.effects.EffectSet;
import edu.cmu.cs.ark.sage.effects.NamedEffect;
import edu.cmu.cs.ark.sage.features.Feature;
import edu.cmu.cs.ark.sage.features.FeatureVector;
import edu.cmu.cs.ark.sage.features.UnigramFeature;
import edu.cmu.cs.ark.yc.config.AppConfig;
import edu.cmu.cs.ark.yc.config.stringparsers.KeyValueParser;
import edu.cmu.cs.ark.yc.utils.types.Pair;

/**
 * Runs {@link SAGE} without any latent variables (i.e topics).
 * <p>
 * It takes as input a text file describing the documents to run {@link SAGE} on. These files can come in two different formats:
 * <ol>
 * <li><i>Documents format</i> - One document per line with two tab separated columns per line. The first column is space-delimited effects while the second column is space-delimited words in the documents.<br>
 * Each line looks like <code>effect1 effect2 ... effectN&lt;TAB&gt;word1 word2 ... wordN</code>.</li>
 * <li><i>Word-counts format</i> - One document per line with two tab separated columns per line. The first column is space-delimited effects while the second column is of the form <code>word:count</code> describing the content of the document.<br>
 * Each line looks like <code>effect1 effect2 ... effectN&lt;TAB&gt;word1:count1 word2:count2 ... wordN:countN</code>. Counts need not be integers.</li>
 * </ol>
 * <p>
 * Note that the effect name <code>background</code> is reserved. By default, the background effects are automatically added to all documents so they are <b>NOT</b> required and should <b>NOT</b> be used. If used (same format as any other document), it will replace the default {@link SupervisedSAGE} calculated background frequencies. Keep these frequencies in real space (not log-space); they will be log-ged by {@link SupervisedSAGE}.
 * <p>
 * <b>Description of parameters</b>
 * <ul>
 * <li><code>--config-file &lt;config-file&gt;</code> All the parameters below can be specified and placed in a separate file. This makes it easier to various parameters without having to retype them.</li>
 * <li><code>--input-counts &lt;counts-file&gt;</code> File containing documents in documents format.</li>
 * <li><code>--input-docs &lt;docs-file&gt;</code> File containing documents in word-counts format.</li>
 * <li><code>-O|--output &lt;sage-file&gt;</code> Write SAGE output to this file. Defaults to <code>output.sage</code></li>
 * <li><code>-L|--save-ll &lt;ll-file&gt;</code> Appends log likelihood at each iteration to this file. Useful to keep track of optimization progress.</li>
 * <li><code>-1|--l1-weight &lt;effect_regex=l1_weight&gt;</code> Set L1 regularization weight for effects whose name matches the regular expression. Default weight is 1.0. You might need backslash escaping to avoid shell auto replacements.</li>
 * <li><code>-2|--l2-weight) &lt;effect_regex=l2_weight&gt;</code> Set L2 regularization weight for effects whose name matches the regular expression. Default weight is 0.0. You might need backslash escaping to avoid shell auto replacements.</li>
 * <li><code>-i|--iterations &lt;N&gt;</code> Number of iterations to do optimization for. Defaults to infinity.</li>
 * <li><code>--no-overwrite</code> By default, SAGE only saves the highest log-likelihood vectors. Use this switch to save output at every iteration. Iteration number will be appended to filename.</li>
 * <li><code>--random-init</code> Initialize eta values to random Gaussian values. Defaults to initalization from origin.</li>
 * </ul>
 * <p>
 * <b>{@link SupervisedSAGE} calculated background effects</b> The background log-frequencies are computed by adding up counts of all words that appeared in the corpus and dividing it by the number of documents (i.e average word appearance frequency per document). After which it is log-ged and the largest log-count is deducted from every word so that everything is negative (or zero). This is to encourage the {@link SAGE} vectors to be positive at the end.
 * <p>
 * <b>Tips for better results</b>
 * <ul>
 * <li>Normalize documents by sentences, i.e the count of words in a sentence sum up to 1.</li>
 * <li>Preprocessing by removing stop words and rare words can reduce the size of the vocabulary, increasing performance significantly.</li>
 * <li>Having a separate effect for each document (document-specific effect) may help reduce the pressure on the cross-document effects having to explain the document-specific stuff too much.</li>
 * </ul>
 * <p>
 * <b>Output format</b> Output will consist of one line per {@link SAGE} effect vector. Each line will have the form <code>effect_name&lt;TAB&gt;word1:eta1 word2:eta2 ... wordN:etaN</code>.
 * 
 * @author Yanchuan Sim
 * @version 0.1
 */
public final class SupervisedSAGE
{
  /**
   * Default L1 regularization weight.
   */
  private static double DEFAULT_L1_REGULARIZATION = 1.0;

  /**
   * Default L2 regularization weight.
   */
  private static double DEFAULT_L2_REGULARIZATION = 0.0;

  /**
   * Nobody can inherit me! hoho
   */
  private SupervisedSAGE()
  {
  }

  /**
   * Run with <code>-h</code> argument to see usage information.
   * 
   * @param args
   *          Arguments to pass in to the app.
   * @throws JSAPException
   * @throws IOException
   * @throws NumberFormatException
   */
  @SuppressWarnings("rawtypes")
  public static void main(String[] args) throws JSAPException, NumberFormatException, IOException
  {
    // set up app parameters with JSAP
    AppConfig app = new AppConfig("SupervisedSAGE", "Run SAGE without any latent variables.", true, true);

    app.registerParameter(new FlaggedOption("input-counts", AppConfig.FILE_PARSER.setMustBeFile(true).setMustExist(true), AppConfig.NO_DEFAULT, false, AppConfig.NO_SHORTFLAG, "input-counts", "Read documents from file (word:count format).").setUsageName("counts-file"));
    app.registerParameter(new FlaggedOption("input-docs", AppConfig.FILE_PARSER.setMustBeFile(true).setMustExist(true), AppConfig.NO_DEFAULT, false, AppConfig.NO_SHORTFLAG, "input-docs", "Read documents from file (space-separated words).").setUsageName("docs-file"));
    app.registerParameter(new FlaggedOption("output", AppConfig.FILE_PARSER.setMustExist(false), "output.sage", false, 'O', "output", "Write SAGE output here (default: output.sage).").setUsageName("sage-file"));
    app.registerParameter(new FlaggedOption("save-ll", AppConfig.FILE_PARSER.setMustExist(false), JSAP.NO_DEFAULT, false, 'L', "save-ll", "Appends log likelihood at each iteration here (default: none).").setUsageName("ll-file"));
    app.registerParameter(new FlaggedOption("l1-weight", AppConfig.KEYVALUE_PARSER, AppConfig.NO_DEFAULT, false, '1', "l1-weight", "Set L1 regularization weight for effects whose name matches the regular expression. Default weight is " + DEFAULT_L1_REGULARIZATION + ".").setAllowMultipleDeclarations(true).setUsageName("effect_regex=l1_weight"));
    app.registerParameter(new FlaggedOption("l2-weight", AppConfig.KEYVALUE_PARSER, AppConfig.NO_DEFAULT, false, '2', "l2-weight", "Set L2 regularization weight for effects whose name matches the regular expression. Default weight is " + DEFAULT_L2_REGULARIZATION + ".").setAllowMultipleDeclarations(true).setUsageName("effect_regex=l2_weight"));
    app.registerParameter(new FlaggedOption("iterations", AppConfig.POSITIVE_INTEGER_PARSER, JSAP.NO_DEFAULT, false, 'i', "iterations", "Number of iterations to optimize for (default: infinity).").setUsageName("N"));
    app.registerParameter(new Switch("no-overwrite", AppConfig.NO_SHORTFLAG, "no-overwrite", "By default, SAGE only saves the highest log-likelihood vectors. Use this switch to save output at every iteration. Iteration number will be appended to filename."));
    app.registerParameter(new Switch("random-init", AppConfig.NO_SHORTFLAG, "random-init", "Initialize eta values to random Gaussian values (default: false)."));

    JSAPResult result = app.parse(args);
    if (!result.contains("input-counts") && !result.contains("input-docs"))
      result.addException("input", new Exception("No input arguments specified. Use -h/--help argument for more usage information."));

    if (result.contains("input-counts") && result.contains("input-docs"))
      result.addException("input", new Exception("Can only specify either --input-counts or --input-docs!"));

    if (!result.success())
    {
      for (java.util.Iterator errs = result.getErrorMessageIterator(); errs.hasNext();)
        System.err.println("Error: " + errs.next());

      // System.err.println("\nUsage: java " + SupervisedSAGE.class.getName());
      // System.err.println("            " + app.getUsage() + "\n" + app.getHelp());
      System.exit(1);
    }

    // actual SAGE stuff
    SAGE S = null;
    if (result.contains("input-counts"))
      S = createSAGEFromWordCounts(result.getFile("input-counts"), result.getBoolean("random-init"));
    else if (result.contains("input-docs"))
      S = createSAGEFromDocuments(result.getFile("input-docs"), result.getBoolean("random-init"));
    updateEffectRegularization(S, KeyValueParser.getKeyValueArray("l1-weight", result), KeyValueParser.getKeyValueArray("l2-weight", result));

    // display effect information
    System.out.println("SAGE effects and their regularization weights");
    List<Effect> effect_order = new ArrayList<>(S.getEffectList());
    Collections.sort(effect_order);
    for (Effect e : effect_order)
    {
      if (e.isFixed())
        continue;

      System.out.format("  %s", e.getDescription());
      if (e.getL1Regularizer() > 0.0)
        System.out.format("\tL1=%f", e.getL1Regularizer());
      if (e.getL2Regularizer() > 0.0)
        System.out.format("\tL2=%f", e.getL2Regularizer());
      System.out.println();
    }

    System.out.format("Will optimize for %s iterations...\n", result.contains("iterations") ? result.getInt("iterations") : "inifinite");

    double best_ll = -Double.MAX_VALUE;
    double ll_prev = 0.0;
    for (int iter = 1; iter <= (result.contains("iterations") ? result.getInt("iterations") : Integer.MAX_VALUE); iter++)
    {
      System.out.print("Iteration " + iter + ": ");

      Collections.shuffle(effect_order);
      int i = 1;
      long start_time = System.nanoTime();
      S.prepareOptimization();
      for (Effect eff : effect_order)
      {
        System.out.print(i++ % 5 == 0 ? Integer.toString(i - 1) : ".");
        S.optimizeEta(eff);
      }

      double ll = S.computeLogLikelihood();

      if (ll > best_ll)
      {
        best_ll = ll;

        File file = result.getBoolean("no-overwrite") ? new File(result.getFile("output").getAbsolutePath() + iter) : result.getFile("output");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        S.print(new PrintWriter(bw), false);
        bw.close();

        System.out.format(" [SAVED]");
      }

      // compute delta log likelihood
      double ll_delta = 0;
      if (ll_prev < 0)
        ll_delta = (ll - ll_prev) / FastMath.abs(ll_prev);
      ll_prev = ll;

      // write out log likelihood
      if (result.getFile("save-ll") != null)
      {
        BufferedWriter bw_ll = new BufferedWriter(new FileWriter(result.getFile("save-ll"), true));
        bw_ll.write(String.format("%d\t%f\n", iter, ll));
        bw_ll.flush();
        bw_ll.close();
      }

      System.out.format(" done! (took %.2f seconds)\n  log-likelihood=%.2f (delta=%.3f%%, best so far=%.2f)", (System.nanoTime() - start_time) * 1e-9, ll, ll_delta * 100.0, best_ll);
      System.out.println();
      System.out.println();
    }
  }

  /**
   * Create {@link SAGE} object from document-style format.
   * 
   * @param file
   *          {@link File} object referring to input file.
   * @param random_init
   *          Whether to randomly initialize the eta vectors.
   * @return {@link SAGE} object ready to go.
   * @throws IOException
   * @throws NumberFormatException
   */
  public static SAGE createSAGEFromDocuments(File file, boolean random_init) throws NumberFormatException, IOException
  {
    SAGE S = new SAGE();
    Effect background = new Effect();
    S.addEffect(background);

    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    String line;

    FeatureVector background_bow = null;
    List<Pair<EffectSet, FeatureVector>> docs = new ArrayList<>();
    double tokens_count = 0.0;

    while ((line = br.readLine()) != null)
    {
      String[] fields = line.split("\t");
      EffectSet doc_effects = new EffectSet();

      String[] effects_fields = fields[0].split(" ");
      boolean is_background_effect = false;

      doc_effects.add(background);

      for (String effects_field : effects_fields)
      {
        if (effects_field.isEmpty())
          continue;

        if (effects_field.equals("background"))
        {
          is_background_effect = true;
          continue;
        }

        NamedEffect e = new NamedEffect(effects_field, DEFAULT_L1_REGULARIZATION, DEFAULT_L2_REGULARIZATION);
        S.addEffect(e);
        doc_effects.add(e);
      }
      if (!is_background_effect)
        S.addEffectSet(doc_effects);

      String[] word_fields = fields[1].split(" ");
      FeatureVector doc = new FeatureVector();

      for (String w : word_fields)
      {
        UnigramFeature t = new UnigramFeature(w);
        S.addFeature(t);
        doc.increment(t, 1.0);
        tokens_count += 1;
      }
      if (is_background_effect)
        background_bow = doc;
      else
        docs.add(new Pair<EffectSet, FeatureVector>(doc_effects, doc));
    }
    br.close();

    S.initialize(random_init);
    setBackgroundEta(S, background, background_bow, docs);

    for (Pair<EffectSet, FeatureVector> p : docs)
      S.incrementFeatureVector(p.getA(), p.getB());

    System.out.format("%d effects and %d effects sets found in %s.\n", S.getEffectList().size(), S.getEffectSetList().size(), file.getName());
    System.out.format("%d documents, %f tokens, %d types found.\n", docs.size(), tokens_count, S.getFeatureList().size());

    return S;
  }

  /**
   * Create {@link SAGE} object from word-count-style format.
   * 
   * @param file
   *          {@link File} object referring to input file.
   * @param random_init
   *          Whether to randomly initialize the eta vectors.
   * @return {@link SAGE} object ready to go.
   * @throws IOException
   */
  public static SAGE createSAGEFromWordCounts(File file, boolean random_init) throws IOException
  {
    SAGE S = new SAGE();
    Effect background = new Effect();
    S.addEffect(background);

    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    String line;

    FeatureVector background_bow = null;
    List<Pair<EffectSet, FeatureVector>> docs = new ArrayList<>();
    double tokens_count = 0.0;

    while ((line = br.readLine()) != null)
    {
      line = line.trim();
      if (line.isEmpty())
        continue;

      String[] fields = line.split("\t", 2);
      EffectSet doc_effects = new EffectSet();

      String[] effects_fields = fields[0].split(" ");
      boolean is_background_effect = false;

      doc_effects.add(background);

      for (String effects_field : effects_fields)
      {
        if (effects_field.isEmpty())
          continue;

        if (effects_field.equals("background"))
        {
          is_background_effect = true;
          continue;
        }

        NamedEffect e = new NamedEffect(effects_field, DEFAULT_L1_REGULARIZATION, DEFAULT_L2_REGULARIZATION);
        S.addEffect(e);
        doc_effects.add(e);
      }
      if (!is_background_effect)
        S.addEffectSet(doc_effects);

      String[] wc_fields = fields[1].split(" ");
      FeatureVector doc = new FeatureVector();

      for (String wc : wc_fields)
      {
        String[] wc_arr = wc.split(":");
        UnigramFeature t = new UnigramFeature(wc_arr[0]);
        S.addFeature(t);
        doc.increment(t, Double.parseDouble(wc_arr[1]));
        tokens_count += Double.parseDouble(wc_arr[1]);
      }

      if (is_background_effect)
        background_bow = doc;
      else
        docs.add(new Pair<EffectSet, FeatureVector>(doc_effects, doc));
    }
    br.close();

    S.initialize(random_init);
    setBackgroundEta(S, background, background_bow, docs);

    for (Pair<EffectSet, FeatureVector> p : docs)
      S.incrementFeatureVector(p.getA(), p.getB());

    System.out.format("%d effects and %d effects sets found in %s.\n", S.getEffectList().size(), S.getEffectSetList().size(), file.getName());
    System.out.format("%d documents, %f tokens, %d types found.\n", docs.size(), tokens_count, S.getFeatureList().size());

    return S;
  }

  /**
   * Sets {@link SAGE}'s background <code>eta</code> vector.
   * 
   * @param S
   *          {@link SAGE} object.
   * @param background
   *          The background {@link Effect}.
   * @param background_bow
   *          The background bag of words as specified from input. May be <code>null</code> if not specified in input.
   * @param docs
   *          List of documents parsed from input file.
   */
  public static void setBackgroundEta(SAGE S, Effect background, FeatureVector background_bow, List<Pair<EffectSet, FeatureVector>> docs)
  {
    double[] bg_feature = new double[S.getFeatureList().size()];
    if (background_bow != null)
    {
      for (Entry<Feature, Double> e : background_bow.entrySet())
        bg_feature[S.findFeature(e.getKey())] = FastMath.log(e.getValue());
    }
    else
    {
      for (Pair<EffectSet, FeatureVector> p : docs)
        for (Entry<Feature, Double> e : p.getB().entrySet())
          bg_feature[S.findFeature(e.getKey())] += e.getValue();

      for (int i = 0; i < bg_feature.length; i++)
        bg_feature[i] = FastMath.log(bg_feature[i]) - FastMath.log(docs.size());

      double max_bg = StatUtils.max(bg_feature);
      for (int i = 0; i < bg_feature.length; i++)
        bg_feature[i] -= max_bg;
    }
    S.setEta(background, bg_feature);
  }

  /**
   * Sets the regularization parameters from parameters passed to the application.
   * 
   * @param S
   *          {@link SAGE} object.
   * @param l1weights
   *          List of L1 weights key-value pairs.
   * @param l2weights
   *          List of L2 weights key-value pairs.
   * @throws ParseException
   */
  private static void updateEffectRegularization(SAGE S, SimpleEntry<String, Double>[] l1weights, SimpleEntry<String, Double>[] l2weights) throws ParseException
  {
    for (SimpleEntry<String, Double> se : l1weights)
    {
      if (se.getValue() <= 0)
        throw new ParseException("Effect \"" + se.getKey() + "\" L1 weight should be > 0.");

      Pattern p = Pattern.compile(se.getKey());
      boolean found = false;
      for (Effect e : S.getEffectList())
      {
        if (p.matcher(e.getDescription()).matches())
        {
          found = true;
          e.setL1Regularizer(se.getValue());
        }
      }
      if (!found)
        System.err.println("Warning: No effects were found to match pattern \"" + se.getKey() + "\".");
    }

    for (SimpleEntry<String, Double> se : l2weights)
    {
      if (se.getValue() <= 0)
        throw new ParseException("Effect \"" + se.getKey() + "\" L2 weight should be > 0.");

      Pattern p = Pattern.compile(se.getKey());
      boolean found = false;
      for (Effect e : S.getEffectList())
      {
        if (p.matcher(e.getDescription()).matches())
        {
          found = true;
          e.setL2Regularizer(se.getValue());
        }
      }
      if (!found)
        System.err.println("Warning: No effects were found to match pattern \"" + se.getKey() + "\".");
    }
  }
}

