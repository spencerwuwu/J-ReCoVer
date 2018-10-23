// https://searchcode.com/api/result/4849954/

/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.speedtracer.hintletengine.client.rules;

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder;

import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceDataReceived;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceSendRequest;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceFinish;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceReceiveResponse;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createNetworkDataRecieved;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createTabChanged;

/**
 * Tests {@link HintletGwtDetect}.
 */
public class HintletGwtDetectTests extends GWTTestCase {

  private HintletRule rule;
  private HintletTestCase test;
  
  @Override
  protected void gwtSetUp() {
    rule = new HintletGwtDetect();
    test = HintletTestCase.getHintletTestCase();
  }
  
  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testNonCacheableNoHint() {
    test.setInputs(getInputs(true, 1000, 1000));
    HintletTestHelper.runTest(rule, test);
  }

  public void testNonCacheableWithHints() {
    test.setInputs(getInputs(false, 1000, 1000));
    
    String hintDescription =
        "GWT selection script '.nocache.js' file should be set as non-cacheable";
    test.addExpectedHint(HintRecord.create(
        rule.getHintletName(), 6, HintRecord.SEVERITY_CRITICAL, hintDescription, 12));
    
    HintletTestHelper.runTest(rule, test);
  }

  public void testDownloadSizeWithHints() {
    test.setInputs(getInputs(true, 1342730, 1342730));
    
    String hint1Description = "The size of the initial GWT download"
      + " (https:/www.efgh.com/gwt.publichome/8E82EC6A261B0BE8394B9AC1BB68A7A9.cache.html)"
      + " is 1342730 bytes.  Consider using GWT.runAsync() code splitting and the Compile Report to"
      + " reduce the size of the initial download.";
    test.addExpectedHint(HintRecord.create(
        rule.getHintletName(), 14, HintRecord.SEVERITY_CRITICAL, hint1Description, 18));

    String hint2Description = "The size of the initial GWT download"
      + " (https:/www.efgh.com/gwt.publichome/9E82AC6A261B0BE8394B9AC1BB68A7AE.cache.html)"
      + " is 1342730 bytes.  Consider using GWT.runAsync() code splitting and the Compile Report to" 
      + " reduce the size of the initial download.";
    test.addExpectedHint(HintRecord.create(
        rule.getHintletName(), 20, HintRecord.SEVERITY_CRITICAL, hint2Description, 24));
    
    HintletTestHelper.runTest(rule, test);
  }
  
  private static EventRecord createNetworkResponseReceived(String identifier, int sequence, String date,
      String expires, String cacheControl) {
    NetworkResponseReceivedEventBuilder builder =
        new NetworkResponseReceivedEventBuilder(identifier, sequence, sequence);
    builder.setResponseFromDiskCache(false).setResponseStatus(200);

    if (date != null) {
      builder.setResponseHeaderDate(date);
    }

    if (expires != null) {
      builder.setResponseHeaderExpires(expires);
    }

    if (cacheControl != null) {
      builder.setResponseHeaderCacheControl(cacheControl);
    }

    return builder.getEvent();
  }

  /**
   * Get a sequence of events. When {@code selectionScriptNonCacheable} is {@code false},
   * {@code nocache.js} is NOT explicitly non-cacheable and a hint will be fired. When
   * {@code strongNameDataLength1} or {@code strongNameDataLength2} is large enough, strong name
   * fetches will trigger hint for large download size.
   * 
   * @param selectionScriptNonCacheable Selection script is explicitly non-cacheable if
   *          {@code selectionScriptNonCacheable} is true
   * @param strongNameDataLength1 the data length of the first string name fetch.
   * @param strongNameDataLength2 the data length of the second string name fetch.
   * @return
   */
  private static JSOArray<EventRecord> getInputs(boolean selectionScriptNonCacheable,
      int strongNameDataLength1, int strongNameDataLength2) {
    final String hostPageId = "1";
    final String hostPageUrl = "https://www.efgh.com/index.html";
    final String imageId = "2";
    final String imageUrl = "https://www.efgh.com/log.png";
    final String selectionScriptId = "3";
    final String selectionScriptUrl =
        "https://www.efgh.com/gwt.publichome/gwt.publichome.nocache.js";
    String selectionScriptDate = null;
    String selectionScriptExpires = null;
    String selectionScriptCacheControl = null;
    if (selectionScriptNonCacheable) {
      selectionScriptDate = "Wed, 20 Jul 2011 14:04:21 GMT";
      selectionScriptExpires = "Wed, 20 Jul 2011 14:04:21 GMT";
      selectionScriptCacheControl = "no-cache";
    }
    final String strongNameID1 = "4";
    final String strongNameUrl1 =
        "https:/www.efgh.com/gwt.publichome/8E82EC6A261B0BE8394B9AC1BB68A7A9.cache.html";
    final String strongNameID2 = "5";
    final String strongNameUrl2 =
        "https:/www.efgh.com/gwt.publichome/9E82AC6A261B0BE8394B9AC1BB68A7AE.cache.html";
    final String strongNameDate = "Wed, 20 Jul 2011 14:04:22 GMT";
    final String strongNameExpires = "Thu, 19 Jul 2012 14:04:22 GMT";
    final String strongNameCacheControl =
        "public,max-age=31536000,post-check=31536000,pre-check=31536000";
    
    int sequence = 1;
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createTabChanged("https:/www.efgh.com", sequence++));
    // Host page
    inputs.push(createResourceSendRequest(hostPageId, hostPageUrl, sequence++));
    inputs.push(createResourceReceiveResponse(hostPageId, sequence++, "text/html"));
    inputs.push(createResourceFinish(hostPageId, sequence++));
    // GWT selection script
    inputs.push(createResourceSendRequest(selectionScriptId, selectionScriptUrl, sequence++));
    inputs.push(createResourceReceiveResponse(selectionScriptId, sequence++, "application/x-javascript"));
    // GWT selection script. Set explicitly non-cacheable here
    inputs.push(createNetworkResponseReceived(selectionScriptId, sequence++, selectionScriptDate,
        selectionScriptExpires, selectionScriptCacheControl));
    // An unrelated interleaved request, just as in real life
    inputs.push(createResourceSendRequest(imageId, imageUrl, sequence++));
    inputs.push(createResourceReceiveResponse(imageId, sequence++, "image/png"));
    inputs.push(createResourceFinish(imageId, sequence++));
    // Back to GWT selection script
    inputs.push(createResourceDataReceived(selectionScriptId, sequence++));
    inputs.push(createResourceFinish(selectionScriptId, sequence++));
    // Strong name fetch 1
    inputs.push(createResourceSendRequest(strongNameID1, strongNameUrl1, sequence++));
    inputs.push(createResourceReceiveResponse(strongNameID1, sequence++, "text/html"));
    inputs.push(createNetworkResponseReceived(strongNameID1, sequence++, strongNameDate,
        strongNameExpires, strongNameCacheControl));
    inputs.push(createResourceDataReceived(strongNameID1, sequence++));
    inputs.push(createNetworkDataRecieved(strongNameID1, sequence++, strongNameDataLength1));
    inputs.push(createResourceFinish(strongNameID1, sequence++));
    // Strong name fetch 2
    inputs.push(createResourceSendRequest(strongNameID2, strongNameUrl2, sequence++));
    inputs.push(createResourceReceiveResponse(strongNameID2, sequence++, "text/html"));
    inputs.push(createNetworkResponseReceived(strongNameID2, sequence++, strongNameDate,
        strongNameExpires, strongNameCacheControl));
    inputs.push(createResourceDataReceived(strongNameID2, sequence++));
    inputs.push(createNetworkDataRecieved(strongNameID2, sequence++, strongNameDataLength2));
    inputs.push(createResourceFinish(strongNameID2, sequence++));
    return inputs;
  }

}

