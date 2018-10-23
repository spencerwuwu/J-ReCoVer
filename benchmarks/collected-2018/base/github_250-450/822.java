// https://searchcode.com/api/result/75773186/

/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.elasticsearch.search.facet.termsstats.longs;

import com.google.common.collect.ImmutableList;
import org.apache.lucene.util.CollectionUtil;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.HashedBytesArray;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.text.StringText;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.trove.ExtTLongObjectHashMap;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.termsstats.InternalTermsStatsFacet;

import java.io.IOException;
import java.util.*;

public class InternalTermsStatsLongFacet extends InternalTermsStatsFacet {

    private static final BytesReference STREAM_TYPE = new HashedBytesArray(Strings.toUTF8Bytes("lTS"));

    public static void registerStream() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static Stream STREAM = new Stream() {
        @Override
        public Facet readFacet(StreamInput in) throws IOException {
            return readTermsStatsFacet(in);
        }
    };

    @Override
    public BytesReference streamType() {
        return STREAM_TYPE;
    }

    public InternalTermsStatsLongFacet() {
    }

    public static class LongEntry implements Entry {

        long term;
        long count;
        long totalCount;
        double total;
        double min;
        double max;

        public LongEntry(long term, long count, long totalCount, double total, double min, double max) {
            this.term = term;
            this.count = count;
            this.totalCount = totalCount;
            this.total = total;
            this.min = min;
            this.max = max;
        }

        @Override
        public Text getTerm() {
            return new StringText(Long.toString(term));
        }

        @Override
        public Number getTermAsNumber() {
            return term;
        }

        @Override
        public long getCount() {
            return count;
        }

        @Override
        public long getTotalCount() {
            return this.totalCount;
        }

        @Override
        public double getMin() {
            return this.min;
        }

        @Override
        public double getMax() {
            return max;
        }

        @Override
        public double getTotal() {
            return total;
        }

        @Override
        public double getMean() {
            if (totalCount == 0) {
                return 0;
            }
            return total / totalCount;
        }

        @Override
        public int compareTo(Entry o) {
            LongEntry other = (LongEntry) o;
            return (term < other.term ? -1 : (term == other.term ? 0 : 1));
        }
    }

    int requiredSize;
    long missing;
    Collection<LongEntry> entries = ImmutableList.of();
    ComparatorType comparatorType;

    public InternalTermsStatsLongFacet(String name, ComparatorType comparatorType, int requiredSize, Collection<LongEntry> entries, long missing) {
        super(name);
        this.comparatorType = comparatorType;
        this.requiredSize = requiredSize;
        this.entries = entries;
        this.missing = missing;
    }

    @Override
    public List<LongEntry> getEntries() {
        if (!(entries instanceof List)) {
            entries = ImmutableList.copyOf(entries);
        }
        return (List<LongEntry>) entries;
    }

    List<LongEntry> mutableList() {
        if (!(entries instanceof List)) {
            entries = new ArrayList<LongEntry>(entries);
        }
        return (List<LongEntry>) entries;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Iterator<Entry> iterator() {
        return (Iterator) entries.iterator();
    }

    @Override
    public long getMissingCount() {
        return this.missing;
    }

    @Override
    public Facet reduce(ReduceContext context) {
        List<Facet> facets = context.facets();
        if (facets.size() == 1) {
            if (requiredSize == 0) {
                // we need to sort it here!
                InternalTermsStatsLongFacet tsFacet = (InternalTermsStatsLongFacet) facets.get(0);
                if (!tsFacet.entries.isEmpty()) {
                    List<LongEntry> entries = tsFacet.mutableList();
                    CollectionUtil.timSort(entries, comparatorType.comparator());
                }
            }
            return facets.get(0);
        }
        int missing = 0;
        ExtTLongObjectHashMap<LongEntry> map = context.cacheRecycler().popLongObjectMap();
        for (Facet facet : facets) {
            InternalTermsStatsLongFacet tsFacet = (InternalTermsStatsLongFacet) facet;
            missing += tsFacet.missing;
            for (Entry entry : tsFacet) {
                LongEntry longEntry = (LongEntry) entry;
                LongEntry current = map.get(longEntry.term);
                if (current != null) {
                    current.count += longEntry.count;
                    current.totalCount += longEntry.totalCount;
                    current.total += longEntry.total;
                    if (longEntry.min < current.min) {
                        current.min = longEntry.min;
                    }
                    if (longEntry.max > current.max) {
                        current.max = longEntry.max;
                    }
                } else {
                    map.put(longEntry.term, longEntry);
                }
            }
        }

        // sort
        if (requiredSize == 0) { // all terms
            LongEntry[] entries1 = map.values(new LongEntry[map.size()]);
            Arrays.sort(entries1, comparatorType.comparator());
            context.cacheRecycler().pushLongObjectMap(map);
            return new InternalTermsStatsLongFacet(getName(), comparatorType, requiredSize, Arrays.asList(entries1), missing);
        } else {
            Object[] values = map.internalValues();
            Arrays.sort(values, (Comparator) comparatorType.comparator());
            List<LongEntry> ordered = new ArrayList<LongEntry>(map.size());
            for (int i = 0; i < requiredSize; i++) {
                LongEntry value = (LongEntry) values[i];
                if (value == null) {
                    break;
                }
                ordered.add(value);
            }
            context.cacheRecycler().pushLongObjectMap(map);
            return new InternalTermsStatsLongFacet(getName(), comparatorType, requiredSize, ordered, missing);
        }
    }

    static final class Fields {
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString MISSING = new XContentBuilderString("missing");
        static final XContentBuilderString TERMS = new XContentBuilderString("terms");
        static final XContentBuilderString TERM = new XContentBuilderString("term");
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
        static final XContentBuilderString TOTAL_COUNT = new XContentBuilderString("total_count");
        static final XContentBuilderString MIN = new XContentBuilderString("min");
        static final XContentBuilderString MAX = new XContentBuilderString("max");
        static final XContentBuilderString TOTAL = new XContentBuilderString("total");
        static final XContentBuilderString MEAN = new XContentBuilderString("mean");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(getName());
        builder.field(Fields._TYPE, InternalTermsStatsFacet.TYPE);
        builder.field(Fields.MISSING, missing);
        builder.startArray(Fields.TERMS);
        for (Entry entry : entries) {
            builder.startObject();
            builder.field(Fields.TERM, ((LongEntry) entry).term);
            builder.field(Fields.COUNT, entry.getCount());
            builder.field(Fields.TOTAL_COUNT, entry.getTotalCount());
            builder.field(Fields.MIN, entry.getMin());
            builder.field(Fields.MAX, entry.getMax());
            builder.field(Fields.TOTAL, entry.getTotal());
            builder.field(Fields.MEAN, entry.getMean());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public static InternalTermsStatsLongFacet readTermsStatsFacet(StreamInput in) throws IOException {
        InternalTermsStatsLongFacet facet = new InternalTermsStatsLongFacet();
        facet.readFrom(in);
        return facet;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        comparatorType = ComparatorType.fromId(in.readByte());
        requiredSize = in.readVInt();
        missing = in.readVLong();

        int size = in.readVInt();
        entries = new ArrayList<LongEntry>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new LongEntry(in.readLong(), in.readVLong(), in.readVLong(), in.readDouble(), in.readDouble(), in.readDouble()));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeByte(comparatorType.id());
        out.writeVInt(requiredSize);
        out.writeVLong(missing);

        out.writeVInt(entries.size());
        for (Entry entry : entries) {
            out.writeLong(((LongEntry) entry).term);
            out.writeVLong(entry.getCount());
            out.writeVLong(entry.getTotalCount());
            out.writeDouble(entry.getTotal());
            out.writeDouble(entry.getMin());
            out.writeDouble(entry.getMax());
        }
    }
}
