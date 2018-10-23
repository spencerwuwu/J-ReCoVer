// https://searchcode.com/api/result/14622648/

/**
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package com.couchbase.client;


import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.protocol.views.DocsOperationImpl;
import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.protocol.views.NoDocsOperationImpl;
import com.couchbase.client.protocol.views.Paginator;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.ReducedOperationImpl;
import com.couchbase.client.protocol.views.RowError;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewOperation.ViewCallback;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.TestConfig;
import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * A CouchbaseClientTest.
 */
public class ViewTest {

  protected TestingClient client = null;
  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools";
  private static final Map<String, Object> ITEMS;
  public static final String DESIGN_DOC_W_REDUCE = "doc_with_view";
  public static final String DESIGN_DOC_WO_REDUCE = "doc_without_view";
  public static final String VIEW_NAME_W_REDUCE = "view_with_reduce";
  public static final String VIEW_NAME_WO_REDUCE = "view_without_reduce";

  static {
    ITEMS = new HashMap<String, Object>();
    int d = 0;
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        for (int k = 0; k < 5; k++, d++) {
          String type = new String(new char[] { (char) ('f' + i) });
          String small = (new Integer(j)).toString();
          String large = (new Integer(k)).toString();
          String doc = generateDoc(type, small, large);
          ITEMS.put("key" + d, doc);
        }
      }
    }
  }

  protected void initClient() throws Exception {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    client = new TestingClient(uris, "default", "");
  }

  @BeforeClass
  public static void before() throws Exception {
    // Create some design documents
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    TestingClient c = new TestingClient(uris, "default", "");
    String docUri = "/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_W_REDUCE;
    String view = "{\"language\":\"javascript\",\"views\":{\""
        + VIEW_NAME_W_REDUCE + "\":{\"map\":\"function (doc) {  "
        + "emit(doc.type, 1)}\",\"reduce\":\"_sum\" }}}";
    c.asyncHttpPut(docUri, view);

    docUri = "/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_WO_REDUCE;
    view = "{\"language\":\"javascript\",\"views\":{\"" + VIEW_NAME_WO_REDUCE
        + "\":{\"map\":\"function (doc) {  " + "emit(doc.type, 1)}\"}}}";
    for (Entry<String, Object> item : ITEMS.entrySet()) {
      assert c.set(item.getKey(), 0,
          (String) item.getValue()).get().booleanValue();
    }
    c.asyncHttpPut(docUri, view);
    c.shutdown();
    Thread.sleep(15000);
  }

  @Before
  public void beforeTest() throws Exception {
    initClient();
  }

  @After
  public void afterTest() throws Exception {
    // Shut down, start up, flush, and shut down again. Error tests have
    // unpredictable timing issues.
    client.shutdown();
    client = null;
  }

  @AfterClass
  public static void after() throws Exception {
    // Delete all design documents I created
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    TestingClient c = new TestingClient(uris, "default", "");
    String json = c.asyncHttpGet("/default/_design/"
        + TestingClient.MODE_PREFIX + DESIGN_DOC_W_REDUCE).get();
    String rev = (new JSONObject(json)).getString("_rev");
    c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_W_REDUCE + "?rev=" + rev).get();

    json = c.asyncHttpGet("/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_WO_REDUCE).get();
    rev = (new JSONObject(json)).getString("_rev");
    c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_WO_REDUCE + "?rev=" + rev).get();
    assert c.flush().get().booleanValue();
  }

  private static String generateDoc(String type, String small, String large) {
    return "{\"type\":\"" + type + "\"" + ",\"small range\":\"" + small + "\","
        + "\"large range\":\"" + large + "\"}";
  }

  @Test
  public void testAssertions() {
    boolean caught = false;
    try {
      assert false;
    } catch (AssertionError e) {
      caught = true;
    }
    assertTrue("Assertions are not enabled!", caught);
  }

  @Test
  public void testQueryWithDocs() {
    Query query = new Query();
    query.setReduce(false);
    query.setIncludeDocs(true);
    query.setStale(Stale.FALSE);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future = client.asyncQuery(view, query);
    ViewResponse response=null;
    try {
      response = future.get();
    } catch (ExecutionException ex) {
      Logger.getLogger(ViewTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InterruptedException ex) {
      Logger.getLogger(ViewTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    assert future.getStatus().isSuccess() : future.getStatus();

    Iterator<ViewRow> itr = response.iterator();
    while (itr.hasNext()) {
      ViewRow row = itr.next();
      if (ITEMS.containsKey(row.getId())) {
        assert ITEMS.get(row.getId()).equals(row.getDocument());
      }
    }
    assert ITEMS.size() == response.size() : future.getStatus().getMessage();
  }

  @Test
  public void testViewNoDocs() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query);
    assert future.getStatus().isSuccess() : future.getStatus();
    ViewResponse response = future.get();

    Iterator<ViewRow> itr = response.iterator();
    while (itr.hasNext()) {
      ViewRow row = itr.next();
      if (!ITEMS.containsKey(row.getId())) {
        assert false : ("Got an item that I shouldn't have gotten.");
      }
    }
    assert response.size() == ITEMS.size() : future.getStatus();
  }

  @Test
  public void testReduce() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    query.setStale(Stale.FALSE);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query);
    ViewResponse reduce = future.get();

    Iterator<ViewRow> itr = reduce.iterator();
    while (itr.hasNext()) {
      ViewRow row = itr.next();
      assert row.getKey() == null;
      assert Integer.valueOf(row.getValue()) == ITEMS.size()
          : future.getStatus();
    }
  }

  @Test
  public void testQuerySetDescending() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setDescending(true));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetEndKeyDocID() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setEndkeyDocID("an_id"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetGroup() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setGroup(true));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetGroupWithLevel() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setGroupLevel(1));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetInclusiveEnd() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setInclusiveEnd(true));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetKey() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setKey("a_key"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetLimit() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setLimit(10));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetRange() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRange("key0", "key2"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetRangeStart() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRangeStart("start"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetRangeEnd() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRangeEnd("end"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetSkip() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setSkip(0));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetStale() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setStale(Stale.OK));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetStartkeyDocID() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setStartkeyDocID("key0"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testQuerySetUpdateSeq() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setUpdateSeq(true));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testReduceWhenNoneExists() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    View view = client.getView(DESIGN_DOC_WO_REDUCE, VIEW_NAME_WO_REDUCE);
    try {
      client.asyncQuery(view, query);
    } catch (RuntimeException e) {
      return; // Pass, no reduce exists.
    }
    assert false : ("No view exists and this query still happened");
  }

  @Test
  public void testViewDocsWithErrors() throws Exception {
    HttpOperation op = new DocsOperationImpl(null, new ViewCallback() {
      @Override
      public void receivedStatus(OperationStatus status) {
        assert status.isSuccess();
      }

      @Override
      public void complete() {
        // Do nothing
      }

      @Override
      public void gotData(ViewResponse response) {
        assert response.getErrors().size() == 2;
        Iterator<RowError> row = response.getErrors().iterator();
        assert row.next().getFrom().equals("127.0.0.1:5984");
        assert response.size() == 0;
      }
    });
    HttpResponse response =
        new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
    String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from"
        + "\":\"127.0.0.1:5984\",\"reason\":\"Design document `_design/test"
        + "foobar` missing in database `test_db_b`.\"},{\"from\":\"http://"
        + "localhost:5984/_view_merge/\",\"reason\":\"Design document `"
        + "_design/testfoobar` missing in database `test_db_c`.\"}]}";
    StringEntity entity = new StringEntity(entityString);
    response.setEntity(entity);
    op.handleResponse(response);
  }

  @Test
  public void testViewNoDocsWithErrors() throws Exception {
    HttpOperation op = new NoDocsOperationImpl(null, new ViewCallback() {
      @Override
      public void receivedStatus(OperationStatus status) {
        assert status.isSuccess();
      }

      @Override
      public void complete() {
        // Do nothing
      }

      @Override
      public void gotData(ViewResponse response) {
        assert response.getErrors().size() == 2;
        Iterator<RowError> row = response.getErrors().iterator();
        assert row.next().getFrom().equals("127.0.0.1:5984");
        assert response.size() == 0;
      }
    });
    HttpResponse response =
        new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
    String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from"
        + "\":\"127.0.0.1:5984\",\"reason\":\"Design document `_design/test"
        + "foobar` missing in database `test_db_b`.\"},{\"from\":\"http://"
        + "localhost:5984/_view_merge/\",\"reason\":\"Design document `"
        + "_design/testfoobar` missing in database `test_db_c`.\"}]}";
    StringEntity entity = new StringEntity(entityString);
    response.setEntity(entity);
    op.handleResponse(response);
  }

  @Test
  public void testViewReducedWithErrors() throws Exception {
    HttpOperation op = new ReducedOperationImpl(null, new ViewCallback() {
      @Override
      public void receivedStatus(OperationStatus status) {
        assert status.isSuccess();
      }

      @Override
      public void complete() {
        // Do nothing
      }

      @Override
      public void gotData(ViewResponse response) {
        assert response.getErrors().size() == 2;
        Iterator<RowError> row = response.getErrors().iterator();
        assert row.next().getFrom().equals("127.0.0.1:5984");
        assert response.size() == 0;
      }
    });
    HttpResponse response =
        new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
    String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from"
        + "\":\"127.0.0.1:5984\",\"reason\":\"Design document `_design/test"
        + "foobar` missing in database `test_db_b`.\"},{\"from\":\"http://"
        + "localhost:5984/_view_merge/\",\"reason\":\"Design document `"
        + "_design/testfoobar` missing in database `test_db_c`.\"}]}";
    StringEntity entity = new StringEntity(entityString);
    response.setEntity(entity);
    op.handleResponse(response);
  }

  @Test
  public void testPaginationItemsModPageSizeNotZero() throws Exception {
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    Query query = new Query();
    query.setReduce(false);
    Paginator op = client.paginatedQuery(view, query, 10);

    int count = 0;
    while (op.hasNext()) {
      ViewRow row = op.next();
      if (!ITEMS.containsKey(row.getId())) {
        assert false : "Got bad key: " + row.getId() + " during pagination";
      }
      count++;
    }
    assert count == ITEMS.size() : "Got " + count + " items, wanted "
        + ITEMS.size();
  }

  @Test
  public void testPaginationItemsModPageSizeIsZero() throws Exception {
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    Query query = new Query();
    query.setReduce(false);
    Paginator op = client.paginatedQuery(view, query, 10);

    assert client.set("key125", 0,
        generateDoc("a", "b", "c")).get().booleanValue()
        : "Setting key key125 failed";
    assert client.set("key126", 0,
        generateDoc("a", "b", "c")).get().booleanValue()
        : "Setting key key126 failed";
    assert client.set("key127", 0,
        generateDoc("a", "b", "c")).get().booleanValue()
        : "Setting key key127 failed";

    int count = 0;
    while (op.hasNext()) {
      String key = op.next().getId();
      if (!ITEMS.containsKey(key)) {
        assert false : "Got bad key: " + key + " during pagination";
      }
      count++;
    }
    assert count == ITEMS.size() : "Got " + count + " items, wanted "
        + ITEMS.size();
    assert client.delete("key125").get().booleanValue()
        : "Deleteing key 125 failed";
    assert client.delete("key126").get().booleanValue()
        : "Deleteing key 125 failed";
    assert client.delete("key127").get().booleanValue()
        : "Deleteing key 125 failed";
    Thread.sleep(1000);
  }

  @Test
  public void testPaginationAndDeleteStartKey() throws Exception {
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    Query query = new Query();
    query.setReduce(false);
    query.setStale(Stale.FALSE);
    Paginator op = client.paginatedQuery(view, query, 10);

    int count = 0;
    while (op.hasNext()) {
      op.next();
      if (count == 5) {
        assert client.delete("key112").get().booleanValue()
            : "Deleteing key key112 failed";
        Thread.sleep(1000);
      }
      count++;
    }
    assert count == ITEMS.size() - 1 : "Got " + count + " items, wanted "
        + (ITEMS.size() - 1);
    assert client.set("key112", 0,
        generateDoc("a", "b", "c")).get().booleanValue()
        : "Adding key key112 failed";
  }
}

