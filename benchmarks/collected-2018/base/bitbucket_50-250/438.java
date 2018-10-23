// https://searchcode.com/api/result/134046521/

package wmr.categories;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author shilad
 */
public class LocalCategoryComparer extends CategoryComparer {
    private final File inputPath;
    private BufferedReader reader;
    private final BufferedWriter writer;
    StringBuilder buffer = new StringBuilder();

    public LocalCategoryComparer(File path, BufferedWriter writer) {
        this.inputPath = path;
        this.writer = writer;
//        this.debuggingMode = true;
    }
    
    public void openFile() throws IOException {
        if (reader != null) {
            reader.close();
        }
        reader = new BufferedReader(new FileReader(inputPath));
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public void closeFile() throws IOException {
        if (reader != null) {
            reader.close();
        }
        reader = null;
    }

    public static void main(String args[]) throws IOException {
        String input = (args.length > 0)
                ? args[0]
                : "/Users/shilad/Documents/NetBeans/wikipedia-map-reduce/dat/all_cats.txt";
        String output = (args.length > 1)
                ? args[1]
                : "/Users/shilad/Documents/NetBeans/wikipedia-map-reduce/dat/test/page_sims.txt";
        BufferedWriter writer = new BufferedWriter(
                output.equals("stdout") 
                        ? new OutputStreamWriter(System.out)
                        : (new FileWriter(output)));
        LocalCategoryComparer lcc = new LocalCategoryComparer(new File(input), writer);
        lcc.prepareDataStructures();
        lcc.searchPages();
        writer.close();
    }

    @Override
    public void writeResults(CategoryRecord record, LinkedHashMap<Integer, Double> distances) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(record.getPageId()).append("\"\t\"");
        boolean isFirst = true;
        for (Map.Entry<Integer, Double> entry : distances.entrySet()) {
            if (!isFirst) {
                builder.append("|");
            }
            builder.append(entry.getKey()).append(",").append(entry.getValue());
            isFirst = false;
        }
        builder.append("\n");
        writer.write(builder.toString());
    }
}

