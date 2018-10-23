// https://searchcode.com/api/result/134046502/

package wmr.tstampsorter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;

import wmr.util.LzmaCompressor;
import wmr.util.Utils;

public class TstampSorterReducer extends Reducer<Text, Text, Text, Text> {
    static final Logger LOG = Logger.getLogger(TstampSorterReducer.class.getName());
    
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
    	String pageId = getPageId(key);
    	char code = getFieldCode(key);
    	if (code != TstampSorterMain.KEY_LENGTH) {
    		LOG.warning("illegal first key in reducer: " + key);
    		return;
    	}
    	Iterator<Text> iter = values.iterator();
    	long length = Long.valueOf(iter.next().toString());
    	
    	LzmaCompressor pipe = null;
    	OutputStream out = null;
    	try {
    		pipe = new LzmaCompressor(length);
    		out = pipe.compress();
    		if (!processPage(key, iter, out)) {
    			return;		// failure
    		}
    		out.flush();
    		out.close();
    		pipe.cleanup();
    		byte[] compressed = pipe.getCompressed();
    		pipe = null;
    		context.write(new Text(pageId), new Text(Utils.escapeWhitespace(compressed)));
    	} catch(Exception e) {
			LOG.log(Level.WARNING, "Exception occurred during tstamp sorter reduce", e);
    	} finally {
    		try {
    			if (pipe != null) pipe.cleanup();
    			out.close();
    		} catch (Exception e) {
    			LOG.log(Level.WARNING, "Pipe cleanup failed", e);
    		}
    	}
    }

	private boolean processPage(Text key, Iterator<Text> values, OutputStream out)
			throws IOException {
		Text next = values.next();
		if (next == null || getFieldCode(key) != TstampSorterMain.KEY_HEADER) {
			LOG.warning("illegal second key in reducer: " + key);
			return false;
		}
		compressBytes(out, next);
		while (true) {
			next = values.next();
			if (next == null || getFieldCode(key) != TstampSorterMain.KEY_REVISION) {
				break;
			}
			compressBytes(out, next);
		}
		if (next == null || getFieldCode(key) != TstampSorterMain.KEY_FOOTER) {
			LOG.warning("illegal key in reducer (expected footer): " + key);
			return false;
		}
		compressBytes(out, next);
		if (values.hasNext()) {
			LOG.warning("extra unexpected values with key " + key + " (ignoring them)");
		}
		return true;
	}

	private void compressBytes(OutputStream compressor, Text next)
			throws IOException {
		byte[] bytes = Utils.unescape(next.getBytes(), next.getLength());
		compressor.write(bytes);
	}

    private String getPageId(Text key) {
    	int i = key.find(" ");
    	if (i < 0) {
    		throw new IllegalStateException("invalid key: " + key);
    	}
    	return key.toString().substring(0, i);
    }
    
    private char getFieldCode(Text key) {
    	int i = key.find(" ");
    	if (i < 0 || (i+1) >= key.getLength()) {
    		throw new IllegalStateException("invalid key: " + key);
    	}
    	return (char)key.charAt(i+1);
    }
    
    private String getTstamp(Text key) {
    	int i = key.find(" ");
    	if (i < 0 || (i+1) >= key.getLength()) {
    		throw new IllegalStateException("invalid key: " + key);
    	}
    	return key.toString().substring(i+2);
    }
}
