// https://searchcode.com/api/result/4877408/

package org.jcouchdb.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;


public class OptionsTestCase
{
    protected static Logger log = Logger.getLogger(OptionsTestCase.class);

    @Test
    public void option()
    {
        String query = new Options("foo",1).put("bar", "baz!").toQuery();
        log.debug(query);
        assertThat(query, startsWith("?"));
        assertThat(query, containsString("foo=1"));
        assertThat(query, containsString("bar=baz%21"));


        query = new Options("foo",1).put("bar", new ArrayList()).toQuery();
        log.debug(query);
        assertThat(query, startsWith("?"));
        assertThat(query, containsString("foo=1"));
        assertThat(query, containsString("bar=%5B%5D"));

        query = new Options().startKeyDocId("bar").toQuery();
        log.debug(query);
        assertThat(query, startsWith("?"));
        assertThat(query, containsString("startkey_docid=bar"));
    }
    
    @Test
    public void testDynamicAccess()
    {
        List<String> keys = Arrays.asList( 
            "key",
            "startkey",
            "endkey",
            "endkey_docid",
            "limit",
            "update",
            "descending",
            "skip",
            "group",
            "stale",
            "reduce",
            "include_docs");
        
        for (String key : keys)
        {
            String value = (String)new Options().put(key, "abc").get(key);
            boolean shouldBeEncoded = Options.JSON_ENCODED_OPTIONS.contains(key);
            assertThat(value, is( shouldBeEncoded ? "\"abc\"" : "abc"));
        }
    }
}

