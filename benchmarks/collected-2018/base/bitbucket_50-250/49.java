// https://searchcode.com/api/result/134046500/

package wmr.wmf;

import gnu.trove.map.hash.TLongIntHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import wmr.util.Utils;

/**
 * TODO:
 * - replace keyframe, prev, next with a single revision index.
 * - consider encoding diffs as a list of strings, or a single string to reduce space.
 * @author shilad
 */
public class WmfDiffReducer extends Reducer<Text, Text, Text, Text> {
    private static final Logger LOG = Logger.getLogger(WmfDiffReducer.class.getName());

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        try {
            context.progress();
            TLongIntHashMap fingerPrintRevs = new TLongIntHashMap();
            Map<String, Object> prevJsonObj = null;
            int revisionIndex = 0;
            for (Text val : values) {
                context.progress();

                // extract data from json
                Map<String, Object> jsonObj;
                try {
                    jsonObj = (Map<String, Object>) JSONValue.parseWithException(val.toString());
                } catch (ParseException ex) {
                    LOG.log(Level.SEVERE, "Json decoding failed", ex);
                    continue;
                }
                updateReduceJson(revisionIndex++, jsonObj, prevJsonObj, fingerPrintRevs);
                // write the previous json object - we now know the next id
                if (prevJsonObj != null) {
                    prevJsonObj.put("nextId", jsonObj.get("_id"));
                    writeReduceJson(prevJsonObj, context);
                }
                prevJsonObj = jsonObj;
            }
            // Catch the final revision
            writeReduceJson(prevJsonObj, context);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "reduce for " + key + " failed", e);
        }
    }

    private void updateReduceJson(int revisionIndex, Map<String, Object> jsonObj, Map<String, Object> prevJsonObj, TLongIntHashMap fingerPrintRevs) {
        String text = (String) jsonObj.get("text");
        Integer id = (Integer) jsonObj.get("_id");

        // add the diffs
        addRevDiffs(
                (prevJsonObj == null) ? "" : (String) prevJsonObj.get("text"),
                text, jsonObj);

        // set or update the keyframe
        if (revisionIndex % 100 == 0) {
            jsonObj.put("keyFrame", id);
        } else {
            assert (prevJsonObj != null);
            jsonObj.put("keyFrame", prevJsonObj.get("keyFrame"));
        }

        // set the previous pointer
        if (prevJsonObj != null) {
            jsonObj.put("prevId", prevJsonObj.get("_id"));
        }

        // check for reverts, update the hash mapping
        long fingerprint = Utils.longHashCode(text);
        if (fingerPrintRevs.containsKey(fingerprint)) {
            jsonObj.put("revert", true);
            jsonObj.put("revertedTo", fingerPrintRevs.get(fingerprint));
        } else {
            fingerPrintRevs.put(fingerprint, id);
        }
    }

    private void addRevDiffs(String prevText, String text, Map<String, Object> jsonObj) {
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<Diff> diffs = dmp.diff_main(prevText, text, false);

        // may be slightly non-optimal, but makes much more sense
        dmp.diff_cleanupSemantic(diffs);

        List<Map<String, Object>> diffJson = new ArrayList<Map<String, Object>>();
        int insertedBytes = 0;
        int deletedBytes = 0;
        int origLocation = 0;
        for (Diff d : diffs) {
            if (d.operation.equals(Operation.EQUAL)) {
                origLocation += d.text.length();
                continue;
            }
            Map<String, Object> diffRec = new HashMap<String, Object>();
            if (d.operation.equals(Operation.INSERT)) {
                diffRec.put("loc", origLocation);
                diffRec.put("op", "i");
                diffRec.put("text", d.text);
                insertedBytes += d.text.length();
            } else if (d.operation.equals(Operation.DELETE)) {
                diffRec.put("loc", origLocation);
                diffRec.put("op", "d");
                diffRec.put("text", d.text);
                origLocation += d.text.length();
                deletedBytes += d.text.length();
            } else {
                assert (false);
            }
            diffJson.add(diffRec);
        }
        jsonObj.put("diffs", diffJson);
        jsonObj.put("totalBytes", text.length());
        jsonObj.put("insertedBytes", insertedBytes);
        jsonObj.put("deletedBytes", deletedBytes);
        jsonObj.put("md5", Utils.md5(text));
    }   


    private void writeReduceJson(Map<String, Object> jsonObj, Reducer.Context context) throws IOException, InterruptedException {
        if (jsonObj != null) {
            // hide and remember the text
            String text = (String) jsonObj.get("text");
            if (!jsonObj.get("keyFrame").equals(jsonObj.get("_id"))) {
                jsonObj.remove("text");
            }
            String jsonStr = JSONValue.toJSONString(jsonObj);
            context.write(new Text(jsonStr), new Text(""));

            // restore the text to use it for diffs
            jsonObj.put("text", text);
        }
    }
}

