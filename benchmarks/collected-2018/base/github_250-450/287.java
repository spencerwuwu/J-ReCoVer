// https://searchcode.com/api/result/94191680/

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.search.aggregations.bucket;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregator.SubAggCollectionMode;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.range.date.DateRange;
import org.elasticsearch.search.aggregations.bucket.range.ipv4.IPv4Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertSearchResponse;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests making sure that the reduce is propagated to all aggregations in the hierarchy when executing on a single shard
 * These tests are based on the date histogram in combination of min_doc_count=0. In order for the date histogram to
 * compute empty buckets, its {@code reduce()} method must be called. So by adding the date histogram under other buckets,
 * we can make sure that the reduce is properly propagated by checking that empty buckets were created.
 */
@ElasticsearchIntegrationTest.SuiteScopeTest
public class ShardReduceTests extends ElasticsearchIntegrationTest {

    private IndexRequestBuilder indexDoc(String date, int value) throws Exception {
        return client().prepareIndex("idx", "type").setSource(jsonBuilder()
                .startObject()
                .field("value", value)
                .field("ip", "10.0.0." + value)
                .field("location", GeoHashUtils.encode(52, 5, 12))
                .field("date", date)
                .field("term-l", 1)
                .field("term-d", 1.5)
                .field("term-s", "term")
                .startObject("nested")
                .field("date", date)
                .endObject()
                .endObject());
    }

    @Override
    public void setupSuiteScopeCluster() throws Exception {
        assertAcked(prepareCreate("idx")
                .addMapping("type", "nested", "type=nested", "ip", "type=ip", "location", "type=geo_point"));

        indexRandom(true,
                indexDoc("2014-01-01", 1),
                indexDoc("2014-01-02", 2),
                indexDoc("2014-01-04", 3));
        ensureSearchable();
    }

    @Test
    public void testGlobal() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(global("global")
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Global global = response.getAggregations().get("global");
        DateHistogram histo = global.getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testFilter() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(filter("filter").filter(FilterBuilders.matchAllFilter())
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Filter filter = response.getAggregations().get("filter");
        DateHistogram histo = filter.getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testMissing() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(missing("missing").field("foobar")
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Missing missing = response.getAggregations().get("missing");
        DateHistogram histo = missing.getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testGlobalWithFilterWithMissing() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(global("global")
                        .subAggregation(filter("filter").filter(FilterBuilders.matchAllFilter())
                                .subAggregation(missing("missing").field("foobar")
                                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))))
                .execute().actionGet();

        assertSearchResponse(response);

        Global global = response.getAggregations().get("global");
        Filter filter = global.getAggregations().get("filter");
        Missing missing = filter.getAggregations().get("missing");
        DateHistogram histo = missing.getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testNested() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(nested("nested").path("nested")
                        .subAggregation(dateHistogram("histo").field("nested.date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Nested nested = response.getAggregations().get("nested");
        DateHistogram histo = nested.getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testStringTerms() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(terms("terms").field("term-s")
                        .collectMode(randomFrom(SubAggCollectionMode.values()))
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Terms terms = response.getAggregations().get("terms");
        DateHistogram histo = terms.getBucketByKey("term").getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testLongTerms() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(terms("terms").field("term-l")
                        .collectMode(randomFrom(SubAggCollectionMode.values()))
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Terms terms = response.getAggregations().get("terms");
        DateHistogram histo = terms.getBucketByKey("1").getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testDoubleTerms() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(terms("terms").field("term-d")
                        .collectMode(randomFrom(SubAggCollectionMode.values()))
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Terms terms = response.getAggregations().get("terms");
        DateHistogram histo = terms.getBucketByKey("1.5").getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testRange() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(range("range").field("value").addRange("r1", 0, 10)
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Range range = response.getAggregations().get("range");
        DateHistogram histo = range.getBucketByKey("r1").getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testDateRange() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(dateRange("range").field("date").addRange("r1", "2014-01-01", "2014-01-10")
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        DateRange range = response.getAggregations().get("range");
        DateHistogram histo = range.getBucketByKey("r1").getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testIpRange() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(ipRange("range").field("ip").addRange("r1", "10.0.0.1", "10.0.0.10")
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        IPv4Range range = response.getAggregations().get("range");
        DateHistogram histo = range.getBucketByKey("r1").getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testHistogram() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(histogram("topHisto").field("value").interval(5)
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        Histogram topHisto = response.getAggregations().get("topHisto");
        DateHistogram histo = topHisto.getBucketByKey(0).getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    @Test
    public void testDateHistogram() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(dateHistogram("topHisto").field("date").interval(DateHistogram.Interval.MONTH)
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        DateHistogram topHisto = response.getAggregations().get("topHisto");
        DateHistogram histo = topHisto.getBuckets().iterator().next().getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));

    }

    @Test
    public void testGeoHashGrid() throws Exception {

        SearchResponse response = client().prepareSearch("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(geohashGrid("grid").field("location")
                        .subAggregation(dateHistogram("histo").field("date").interval(DateHistogram.Interval.DAY).minDocCount(0)))
                .execute().actionGet();

        assertSearchResponse(response);

        GeoHashGrid grid = response.getAggregations().get("grid");
        DateHistogram histo = grid.getBuckets().iterator().next().getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }


}

