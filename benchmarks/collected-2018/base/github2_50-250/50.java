// https://searchcode.com/api/result/107367169/

package org.apache.nutch.crawl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.nutch.crawl.Generator.SelectorEntry;
import org.apache.nutch.fetcher.Fetcher.FetcherMapper;
import org.apache.nutch.storage.Mark;
import org.apache.nutch.storage.WebTableRow;
import org.apache.nutch.storage.mapreduce.RowReducer;
import org.apache.nutch.util.hbase.TableUtil;

/** Reduce class for generate
 * 
 * The #reduce() method write a random integer to all generated URLs. This random
 * number is then used by {@link FetcherMapper}.
 *
 */
public class GeneratorReducer
extends RowReducer<SelectorEntry, WebTableRow, String, WebTableRow> {

  private long limit;
  private long maxPerHost;
  private long count = 0;
  private Map<String, Integer> hostCountMap = new HashMap<String, Integer>();
  private byte[] crawlIdRaw;

  @Override
  protected void reduce(SelectorEntry key, Iterable<WebTableRow> values,
      Context context) throws IOException, InterruptedException {
    for (WebTableRow row : values) {
      if (maxPerHost > 0) {
        String host = key.host;
        Integer hostCount = hostCountMap.get(host);
        if (hostCount == null) {
          hostCountMap.put(host, 0);
          hostCount = 0;
        }
        if (hostCount > maxPerHost) {
          return;
        }
        hostCountMap.put(host, hostCount + 1);
      }
      if (count >= limit) {
        return;
      }
      
      Mark.GENERATE_MARK.putMark(row, crawlIdRaw);
      context.write(TableUtil.reverseUrl(key.url), row);
      count++;
    }
  }

  @Override
  protected void setup(Context context)
      throws IOException, InterruptedException {
    Configuration conf = context.getConfiguration();
    long totalLimit = conf.getLong(Generator.CRAWL_TOP_N, Long.MAX_VALUE);
    if (totalLimit == Long.MAX_VALUE) {
      limit = Long.MAX_VALUE; 
    } else {
      limit = totalLimit / context.getNumReduceTasks();
    }
    maxPerHost = conf.getLong(Generator.GENERATE_MAX_PER_HOST, -1);
    crawlIdRaw = Bytes.toBytes(conf.get(Generator.CRAWL_ID));
  }
  
}
