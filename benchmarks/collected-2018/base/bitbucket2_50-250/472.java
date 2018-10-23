// https://searchcode.com/api/result/45117624/

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hedwig.server.topics;

import com.google.protobuf.ByteString;
import org.apache.hedwig.exceptions.PubSubException;
import org.apache.hedwig.protocol.PubSubProtocol;
import org.apache.hedwig.util.Callback;

import junit.framework.Assert;

import java.util.*;
import java.util.concurrent.SynchronousQueue;

import org.apache.hedwig.util.ConcurrencyUtils;
import org.apache.hedwig.util.HedwigSocketAddress;
import org.junit.Test;

/**
 * TODO: Add an integration test to check if the topics are released.
 */
public class TestTopicBasedLoadShedder {

    final protected SynchronousQueue<Boolean> statusQueue = new SynchronousQueue<Boolean>();
    private int myTopics = 10;
    private int numHubs = 10;
    private final PubSubProtocol.HubLoadData infiniteMaxLoad = PubSubProtocol.HubLoadData.newBuilder().setNumTopics(10000000).build();
    Map<HubInfo, HubLoad> mockLoadMap = new HashMap<HubInfo, HubLoad>();

    class MockTopicBasedLoadShedder extends TopicBasedLoadShedder {
        // This is set by the reduceLoadTo function.
        public HubLoad targetLoad;
        public MockTopicBasedLoadShedder(TopicManager tm, long numTopics,
                                         Double tolerancePercentage, PubSubProtocol.HubLoadData maxLoadToShed) {
            super(tm, numTopics, tolerancePercentage, maxLoadToShed);
        }
        @Override
        public void reduceLoadTo(HubLoad targetLoad, final Callback<Long> callback, final Object ctx) {
            this.targetLoad = targetLoad;
            // Indicates that we released these many topics.
            callback.operationFinished(ctx, targetLoad.toHubLoadData().getNumTopics());
        }
    }
    public Callback<Boolean> getShedLoadCallback(final MockTopicBasedLoadShedder ls, final HubLoad expected,
                                                 final Boolean shouldRelease, final Boolean shouldFail) {
        return new Callback<Boolean>() {
            @Override
            public void operationFinished(Object o, Boolean aBoolean) {
                Boolean status = false;
                status = (aBoolean == shouldRelease);
                if (shouldRelease) {
                    status &= (ls.targetLoad != null);
                    //Assert.assertNotNull(ls.targetLoad);
                    status &= (expected.numTopics == ls.targetLoad.numTopics);
                    //Assert.assertEquals(expected.toHubLoadData().getNumTopics(), ls.targetLoad.toHubLoadData().getNumTopics());
                }
                final Boolean statusToPut = status;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ConcurrencyUtils.put(statusQueue, statusToPut);
                    }
                }).start();
            }

            @Override
            public void operationFailed(Object o, PubSubException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ConcurrencyUtils.put(statusQueue, shouldFail);
                    }
                }).start();
            }
        };
    }

    private HubInfo getHubInfo(int hubNum) {
        return new HubInfo(new HedwigSocketAddress("myhub.testdomain.foo"+hubNum+":4080:4080"), 0);
    }

    private synchronized void initialize(int myTopics, int numHubs, int[] otherHubsLoad) {
        if (null != otherHubsLoad) {
            Assert.assertTrue(otherHubsLoad.length == numHubs - 1);
        }
        this.myTopics = myTopics;
        this.numHubs = numHubs;
        this.mockLoadMap.clear();
        this.mockLoadMap.put(getHubInfo(0), new HubLoad(this.myTopics));
        for (int i = 1; i < this.numHubs; i++) {
            this.mockLoadMap.put(getHubInfo(i), new HubLoad(otherHubsLoad[i-1]));
        }
    }

    private int[] getEqualLoadDistributionArray(int n, int load) {
        if (n == 0) {
            return null;
        }
        int[] retLoad = new int[n];
        Arrays.fill(retLoad, load);
        return retLoad;
    }

    @Test
    public synchronized  void testAllHubsSameTopics() throws Exception {
        // All hubs have the same number of topics. We should not release any topics even with a
        // tolerance of 0.0.
        initialize(10, 10, getEqualLoadDistributionArray(9, 10));
        MockTopicBasedLoadShedder tbls = new MockTopicBasedLoadShedder(null, 10, 0.0, infiniteMaxLoad);
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, null, false, false), null);
        Assert.assertTrue(statusQueue.take());
    }

    @Test
    public synchronized void testOneHubUnequalTopics() throws Exception {
        // The hub has 20 topics while the average is 11. Should reduce the load to 11.
        initialize(20, 10, getEqualLoadDistributionArray(9, 10));
        MockTopicBasedLoadShedder tbls = new MockTopicBasedLoadShedder(null, 10, 0.0, infiniteMaxLoad);
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, new HubLoad(11), true, false), null);
        Assert.assertTrue(statusQueue.take());
    }

    @Test
    public synchronized void testOneHubUnequalTopicsWithTolerance() throws Exception {
        // The hub has 20 topics and average is 11. Should still release as tolerance level of 50.0 is
        // breached. Should get down to average.
        initialize(20, 10, getEqualLoadDistributionArray(9, 10));
        MockTopicBasedLoadShedder tbls = new MockTopicBasedLoadShedder(null, 10, 50.0, infiniteMaxLoad);
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, new HubLoad(11), true, false), null);
        Assert.assertTrue(statusQueue.take());

        // A tolerance level of 100.0 should result in the hub not releasing topics.
        tbls = new MockTopicBasedLoadShedder(null, 10, 100.0, infiniteMaxLoad);
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, null, false, false), null);
        Assert.assertTrue(statusQueue.take());
    }

    @Test
    public synchronized void testMaxLoadShed() throws Exception {
        // The hub should not shed more than maxLoadShed topics.
        initialize(20, 10, getEqualLoadDistributionArray(9, 10));
        MockTopicBasedLoadShedder tbls = new MockTopicBasedLoadShedder(null, 10, 0.0, PubSubProtocol
                .HubLoadData.newBuilder().setNumTopics(5).build());
        // Our load should reduce to 15.
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, new HubLoad(15), true, false), null);
        Assert.assertTrue(statusQueue.take());

        // We should reduce to 11 even when maxLoadShed and average result in the same
        // values
        tbls = new MockTopicBasedLoadShedder(null, 10, 0.0, PubSubProtocol
                .HubLoadData.newBuilder().setNumTopics(9).build());
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, new HubLoad(11), true, false), null);
        Assert.assertTrue(statusQueue.take());
    }

    @Test
    public synchronized void testSingleHubLoadShed() throws Exception {
        // If this is the only hub in the cluster, it should not release any topics.
        initialize(20, 1, null);
        MockTopicBasedLoadShedder tbls = new MockTopicBasedLoadShedder(null, 10, 0.0, infiniteMaxLoad);
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, null, false, false), null);
        Assert.assertTrue(statusQueue.take());
    }

    @Test
    public synchronized void testUnderloadedClusterLoadShed() throws Exception {
        // Hold on to at least one topic while shedding load (if cluster is underloaded)
        initialize(5, 10, getEqualLoadDistributionArray(9, 0));
        MockTopicBasedLoadShedder tbls = new MockTopicBasedLoadShedder(null, 10, 0.0, infiniteMaxLoad);
        tbls.shedLoad(mockLoadMap, getShedLoadCallback(tbls, new HubLoad(1), true, false), null);
        Assert.assertTrue(statusQueue.take());
    }
}

