// https://searchcode.com/api/result/115994530/

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
package org.elasticsearch.search.aggregations.metrics.stats.extended;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.metrics.stats.InternalStats;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class InternalExtendedStats extends InternalStats implements ExtendedStats {
    enum Metrics {

        count, sum, min, max, avg, sum_of_squares, variance, std_deviation, std_upper, std_lower;

        public static Metrics resolve(String name) {
            return Metrics.valueOf(name);
        }
    }

    private final double sumOfSqrs;
    private final double sigma;

    public InternalExtendedStats(String name, long count, double sum, double min, double max, double sumOfSqrs, double sigma,
            DocValueFormat formatter, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) {
        super(name, count, sum, min, max, formatter, pipelineAggregators, metaData);
        this.sumOfSqrs = sumOfSqrs;
        this.sigma = sigma;
    }

    /**
     * Read from a stream.
     */
    public InternalExtendedStats(StreamInput in) throws IOException {
        super(in);
        sumOfSqrs = in.readDouble();
        sigma = in.readDouble();
    }

    @Override
    protected void writeOtherStatsTo(StreamOutput out) throws IOException {
        out.writeDouble(sumOfSqrs);
        out.writeDouble(sigma);
    }

    @Override
    public String getWriteableName() {
        return ExtendedStatsAggregationBuilder.NAME;
    }

    @Override
    public double value(String name) {
        if ("sum_of_squares".equals(name)) {
            return sumOfSqrs;
        }
        if ("variance".equals(name)) {
            return getVariance();
        }
        if ("std_deviation".equals(name)) {
            return getStdDeviation();
        }
        if ("std_upper".equals(name)) {
            return getStdDeviationBound(Bounds.UPPER);
        }
        if ("std_lower".equals(name)) {
            return getStdDeviationBound(Bounds.LOWER);
        }
        return super.value(name);
    }

    @Override
    public double getSumOfSquares() {
        return sumOfSqrs;
    }

    @Override
    public double getVariance() {
        return (sumOfSqrs - ((sum * sum) / count)) / count;
    }

    @Override
    public double getStdDeviation() {
        return Math.sqrt(getVariance());
    }

    @Override
    public double getStdDeviationBound(Bounds bound) {
        if (bound.equals(Bounds.UPPER)) {
            return getAvg() + (getStdDeviation() * sigma);
        } else {
            return getAvg() - (getStdDeviation() * sigma);
        }
    }

    @Override
    public String getSumOfSquaresAsString() {
        return valueAsString(Metrics.sum_of_squares.name());
    }

    @Override
    public String getVarianceAsString() {
        return valueAsString(Metrics.variance.name());
    }

    @Override
    public String getStdDeviationAsString() {
        return valueAsString(Metrics.std_deviation.name());
    }

    @Override
    public String getStdDeviationBoundAsString(Bounds bound) {
        return bound == Bounds.UPPER ? valueAsString(Metrics.std_upper.name()) : valueAsString(Metrics.std_lower.name());
    }

    @Override
    public InternalExtendedStats doReduce(List<InternalAggregation> aggregations, ReduceContext reduceContext) {
        double sumOfSqrs = 0;
        for (InternalAggregation aggregation : aggregations) {
            InternalExtendedStats stats = (InternalExtendedStats) aggregation;
            if (stats.sigma != sigma) {
                throw new IllegalStateException("Cannot reduce other stats aggregations that have a different sigma");
            }
            sumOfSqrs += stats.getSumOfSquares();
        }
        final InternalStats stats = super.doReduce(aggregations, reduceContext);
        return new InternalExtendedStats(name, stats.getCount(), stats.getSum(), stats.getMin(), stats.getMax(), sumOfSqrs, sigma,
                format, pipelineAggregators(), getMetaData());
    }

    static class Fields {
        public static final String SUM_OF_SQRS = "sum_of_squares";
        public static final String SUM_OF_SQRS_AS_STRING = "sum_of_squares_as_string";
        public static final String VARIANCE = "variance";
        public static final String VARIANCE_AS_STRING = "variance_as_string";
        public static final String STD_DEVIATION = "std_deviation";
        public static final String STD_DEVIATION_AS_STRING = "std_deviation_as_string";
        public static final String STD_DEVIATION_BOUNDS = "std_deviation_bounds";
        public static final String STD_DEVIATION_BOUNDS_AS_STRING = "std_deviation_bounds_as_string";
        public static final String UPPER = "upper";
        public static final String LOWER = "lower";

    }

    @Override
    protected XContentBuilder otherStatsToXCotent(XContentBuilder builder, Params params) throws IOException {
        builder.field(Fields.SUM_OF_SQRS, count != 0 ? sumOfSqrs : null);
        builder.field(Fields.VARIANCE, count != 0 ? getVariance() : null);
        builder.field(Fields.STD_DEVIATION, count != 0 ? getStdDeviation() : null);
        builder.startObject(Fields.STD_DEVIATION_BOUNDS)
                .field(Fields.UPPER, count != 0 ? getStdDeviationBound(Bounds.UPPER) : null)
                .field(Fields.LOWER, count != 0 ? getStdDeviationBound(Bounds.LOWER) : null)
                .endObject();

        if (count != 0 && format != DocValueFormat.RAW) {
            builder.field(Fields.SUM_OF_SQRS_AS_STRING, format.format(sumOfSqrs));
            builder.field(Fields.VARIANCE_AS_STRING, format.format(getVariance()));
            builder.field(Fields.STD_DEVIATION_AS_STRING, getStdDeviationAsString());

            builder.startObject(Fields.STD_DEVIATION_BOUNDS_AS_STRING)
                    .field(Fields.UPPER, getStdDeviationBoundAsString(Bounds.UPPER))
                    .field(Fields.LOWER, getStdDeviationBoundAsString(Bounds.LOWER))
                    .endObject();

        }
        return builder;
    }
}

