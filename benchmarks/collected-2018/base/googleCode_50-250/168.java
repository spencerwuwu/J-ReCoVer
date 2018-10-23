// https://searchcode.com/api/result/4849800/

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

import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.ResourceRecord;
import com.google.speedtracer.hintletengine.client.HintletCacheUtils;
import com.google.speedtracer.hintletengine.client.HintletNetworkResources;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;
import com.google.speedtracer.shared.EventRecordType;

/**
 * Detect Gwt. Check code splitting possibility and make sure selection script is non-cacheable.
 */
public final class HintletGwtDetect extends HintletRule {

  // Emit hintlets based on the size of the initial download
  private static final int CODE_SPLIT_INFO_SIZE_THRESHOLD = 250000;
  private static final int CODE_SPLIT_WARNING_SIZE_THRESHOLD = 500000;
  private static final int CODE_SPLIT_CRITICAL_SIZE_THRESHOLD = 1000000;

  // If we find the selection script (nocache.js file) and then strong name fetch from the same
  // domain, we consider current web session running a GWT app.
  private static class State {
    // We check network events for nocache.js after we see one strong name fetch.
    // Thus we need to save the network events for nocache.js.
    public ResourceRecord nocacheJsResourceFinishEvent = null;
    public NetworkResource nocacheJsSavedNeworkResource = null;
    public boolean analyzedNoCacheJsEvents = false;
  }

  private State state = new State();

  public HintletGwtDetect() {
  }

  public HintletGwtDetect(HintletOnHintListener onHint) {
    setOnHintCallback(onHint);
  }
  
  @Override
  public String getHintletName() {
    return "GWT Application Detection";
  }

  @Override
  public void onEventRecord(EventRecord eventRecord) {
    if (eventRecord.getType() == EventRecordType.TAB_CHANGED) {
      // Reset state after a page transition
      state = new State();
      return;
    }

    if (eventRecord.getType() != EventRecordType.RESOURCE_FINISH) {
      return;
    }

    ResourceRecord resourceFinishEvent = eventRecord.cast();
    NetworkResource savedNeworkResource =
        HintletNetworkResources.getInstance().getResourceData(resourceFinishEvent.getRequestId());
    if (savedNeworkResource == null) {
      return;
    }

    String url = savedNeworkResource.getUrl();
    if (url.endsWith("nocache.js")) {
      state.nocacheJsResourceFinishEvent = resourceFinishEvent;
      state.nocacheJsSavedNeworkResource = savedNeworkResource;
      return;
    }

    // TODO(zundel): Take into account the time to download/parse the strong
    // name to suggest code splitting opportunity. Be sure to omit Script Eval time.
    if (isStrongName(url) && state.nocacheJsResourceFinishEvent != null) {
      // is gwt app
      if (!state.analyzedNoCacheJsEvents) {
        analyzeNoCacheJS(state.nocacheJsResourceFinishEvent, state.nocacheJsSavedNeworkResource);
        state.analyzedNoCacheJsEvents = true; // analyze events for fetching nocache.js once.
      }
      // may have multiple strong name fetches. analyze each one
      analyzeDownloadSize(resourceFinishEvent, savedNeworkResource);
    }
  }

  /**
   * .nocache.js file should be set as non-cacheable
   * 
   * @param resourceRecord current RESOURCE_FINISH event
   * @param savedNeworkResource
   */
  private void analyzeNoCacheJS(ResourceRecord resourceRecord, NetworkResource savedNeworkResource) {
    if (!HintletCacheUtils.isExplicitlyNonCacheable(savedNeworkResource.getResponseHeaders(),
        savedNeworkResource.getUrl(), savedNeworkResource.getStatusCode())) {
      addHint(getHintletName(), savedNeworkResource.getResponseReceivedTime(),
          "GWT selection script '.nocache.js' file should be set as non-cacheable", 
          resourceRecord.getSequence(), HintRecord.SEVERITY_CRITICAL);
    }
  }

  /**
   * Analyze download size and suggest code splitting if applicable
   * 
   * @param resourceRecord current RESOURCE_FINISH event
   * @param savedNeworkResource
   */
  private void analyzeDownloadSize(ResourceRecord resourceRecord,
      NetworkResource savedNeworkResource) {
    int size = savedNeworkResource.getDataLength();
    int severity = 0;
    if (size > CODE_SPLIT_CRITICAL_SIZE_THRESHOLD) {
      severity = HintRecord.SEVERITY_CRITICAL;
    } else if (size > CODE_SPLIT_WARNING_SIZE_THRESHOLD) {
      severity = HintRecord.SEVERITY_WARNING;
    } else if (size > CODE_SPLIT_INFO_SIZE_THRESHOLD) {
      severity = HintRecord.SEVERITY_INFO;
    } else {
      return;
    }

    addHint(getHintletName(), savedNeworkResource.getResponseReceivedTime(),
        "The size of the initial GWT download (" + savedNeworkResource.getUrl() + ") is " + size
            + " bytes.  Consider using GWT.runAsync() code splitting"
            + " and the Compile Report to reduce the size of the initial download.", 
            resourceRecord.getSequence(), severity);
  }

  /**
   * 32-char hash plus
   * ".cache.html, e.g. "http://www.abcde.com/bio/8E82EC6A261B0BE8394B9AC1BB68A7A9.cache.html"
   */
  private native static boolean isStrongName(String url) /*-{
    return (url.search("[0-9A-F]{32}\.cache\.html$") >= 0);
  }-*/;

}

