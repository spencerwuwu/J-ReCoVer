// https://searchcode.com/api/result/115994029/

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
package org.elasticsearch.search.aggregations.bucket.terms;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * Result of the {@link TermsAggregator} when the field is unmapped.
 */
public class UnmappedTerms extends InternalTerms<UnmappedTerms, UnmappedTerms.Bucket> {
    public static final String NAME = "umterms";

    /**
     * Concrete type that can't be built because Java needs a concrent type so {@link InternalTerms.Bucket} can have a self type but
     * {@linkplain UnmappedTerms} doesn't ever need to build it because it never returns any buckets.
     */
    protected abstract static class Bucket extends InternalTerms.Bucket<Bucket> {
        private Bucket(long docCount, InternalAggregations aggregations, boolean showDocCountError, long docCountError,
                DocValueFormat formatter) {
            super(docCount, aggregations, showDocCountError, docCountError, formatter);
        }
    }

    public UnmappedTerms(String name, Terms.Order order, int requiredSize, long minDocCount,
            List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) {
        super(name, order, requiredSize, minDocCount, pipelineAggregators, metaData);
    }

    /**
     * Read from a stream.
     */
    public UnmappedTerms(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    protected void writeTermTypeInfoTo(StreamOutput out) throws IOException {
        // Nothing to write
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public UnmappedTerms create(List<Bucket> buckets) {
        return new UnmappedTerms(name, order, requiredSize, minDocCount, pipelineAggregators(), metaData);
    }

    @Override
    public Bucket createBucket(InternalAggregations aggregations, Bucket prototype) {
        throw new UnsupportedOperationException("not supported for UnmappedTerms");
    }

    @Override
    protected UnmappedTerms create(String name, List<Bucket> buckets, long docCountError, long otherDocCount) {
        throw new UnsupportedOperationException("not supported for UnmappedTerms");
    }

    @Override
    public InternalAggregation doReduce(List<InternalAggregation> aggregations, ReduceContext reduceContext) {
        for (InternalAggregation agg : aggregations) {
            if (!(agg instanceof UnmappedTerms)) {
                return agg.reduce(aggregations, reduceContext);
            }
        }
        return this;
    }

    @Override
    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(InternalTerms.DOC_COUNT_ERROR_UPPER_BOUND_FIELD_NAME, 0);
        builder.field(SUM_OF_OTHER_DOC_COUNTS, 0);
        builder.startArray(CommonFields.BUCKETS).endArray();
        return builder;
    }

    @Override
    protected void setDocCountError(long docCountError) {
    }

    @Override
    protected int getShardSize() {
        return 0;
    }

    @Override
    public long getDocCountError() {
        return 0;
    }

    @Override
    public long getSumOfOtherDocCounts() {
        return 0;
    }

    @Override
    protected List<Bucket> getBucketsInternal() {
        return emptyList();
    }

    @Override
    public Bucket getBucketByKey(String term) {
        return null;
    }

    @Override
    protected Bucket[] createBucketsArray(int size) {
        return new Bucket[size];
    }
}

