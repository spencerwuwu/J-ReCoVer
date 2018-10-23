// https://searchcode.com/api/result/12763939/

// (c) Copyright 2009 Cloudera, Inc.
// Hadoop 0.20.1 API Updated by Marcello de Sales (marcello.desales@gmail.com)

package tfidf;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * WordCountsForDocsReducer counts the number of documents in the
 */
public class WordsInCorpusTFIDFReducer extends Reducer<Text, Text, Text, Text> {

    private static final DecimalFormat DF = new DecimalFormat("###.########");

    private Text wordAtDocument = new Text();
    
    private Text tfidfCounts = new Text();
    
    public WordsInCorpusTFIDFReducer() {
    }

    /**
     * @param key is the key of the mapper
     * @param values are all the values aggregated during the mapping phase
     * @param context contains the context of the job run 
     * PRE-CONDITION: receive a list of <word, ["doc1=n1/N1", "doc2=n2/N2"]> 
     * POST-CONDITION: <"word@doc1#D, [n+n, N, TF-IDF]">, <"word2@a.txt, 5/13">, <"word2@b.txt, 0/13"> empty case
     */
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        // get the number of documents indirectly from the file-system
        int numberOfDocumentsInCorpus = context.getConfiguration().getInt("numberOfDocsInCorpus", 0);
        // total frequency of this word
        int numberOfDocumentsInCorpusWhereKeyAppears = 0;
        Map<String, String> tempFrequencies = new HashMap<String, String>();
        for (Text val : values) {
            String[] documentAndFrequencies = val.toString().split("=");
            if (Integer.parseInt(documentAndFrequencies[1].split("/")[0]) > 0) { // in case the counter of the words is > 0
                numberOfDocumentsInCorpusWhereKeyAppears++;
            }
            tempFrequencies.put(documentAndFrequencies[0], documentAndFrequencies[1]);
        }
        for (String document : tempFrequencies.keySet()) {
            String[] wordFrequenceAndTotalWords = tempFrequencies.get(document).split("/");

            //Term frequency is the quocient of the number occurrances of the term in document and the total number of terms in document
            double tf = Double.valueOf(Double.valueOf(wordFrequenceAndTotalWords[0])
                    / Double.valueOf(wordFrequenceAndTotalWords[1]));
            
            //interse document frequency quocient between the number of docs in corpus and number of docs the term appears
            // Normalize the value in case the number of appearances are 0, use 1.
            double idf = Math.log10((double) numberOfDocumentsInCorpus / 
               (double) (numberOfDocumentsInCorpusWhereKeyAppears + (numberOfDocumentsInCorpusWhereKeyAppears == 0 ? 1 : 0)));

            double tfIdf = tf * idf;

            this.wordAtDocument.set(key + "@" + document);
            this.tfidfCounts.set("[" + numberOfDocumentsInCorpusWhereKeyAppears + "/"
                    + numberOfDocumentsInCorpus + " , " + wordFrequenceAndTotalWords[0] + "/"
                    + wordFrequenceAndTotalWords[1] + " , " + DF.format(tfIdf) + "]");
            
            context.write(this.wordAtDocument, this.tfidfCounts);
        }
    }
}

