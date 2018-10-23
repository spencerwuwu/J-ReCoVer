// https://searchcode.com/api/result/75773219/

/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.facet.histogram;

import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;
import org.elasticsearch.cache.recycler.CacheRecycler;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.HashedBytesArray;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class InternalCountHistogramFacet extends InternalHistogramFacet {

    private static final BytesReference STREAM_TYPE = new HashedBytesArray(Strings.toUTF8Bytes("cHistogram"));

    public static void registerStreams() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static Stream STREAM = new Stream() {
        @Override
        public Facet readFacet(StreamInput in) throws IOException {
            return readHistogramFacet(in);
        }
    };

    @Override
    public BytesReference streamType() {
        return STREAM_TYPE;
    }


    /**
     * A histogram entry representing a single entry within the result of a histogram facet.
     */
    public static class CountEntry implements Entry {
        private final long key;
        private final long count;

        public CountEntry(long key, long count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public long getKey() {
            return key;
        }

        @Override
        public long getCount() {
            return count;
        }

        @Override
        public double getTotal() {
            return Double.NaN;
        }

        @Override
        public long getTotalCount() {
            return 0;
        }

        @Override
        public double getMean() {
            return Double.NaN;
        }

        @Override
        public double getMin() {
            return Double.NaN;
        }

        @Override
        public double getMax() {
            return Double.NaN;
        }
    }

    ComparatorType comparatorType;
    TLongLongHashMap counts;
    CacheRecycler cacheRecycler;
    CountEntry[] entries = null;

    private InternalCountHistogramFacet() {
    }

    public InternalCountHistogramFacet(String name, ComparatorType comparatorType, TLongLongHashMap counts, CacheRecycler cacheRecycler) {
        super(name);
        this.comparatorType = comparatorType;
        this.counts = counts;
        this.cacheRecycler = cacheRecycler;
    }

    public InternalCountHistogramFacet(String name, ComparatorType comparatorType, CountEntry[] entries) {
        super(name);
        this.comparatorType = comparatorType;
        this.entries = entries;
    }

    @Override
    public List<CountEntry> getEntries() {
        return Arrays.asList(entries);
    }

    @Override
    public Iterator<Entry> iterator() {
        return (Iterator) getEntries().iterator();
    }

    void releaseCache() {
        if (cacheRecycler != null) {
            cacheRecycler.pushLongLongMap(counts);
            cacheRecycler = null;
            counts = null;
        }
    }

    @Override
    public Facet reduce(ReduceContext context) {
        List<Facet> facets = context.facets();
        if (facets.size() == 1) {
            // need to sort here...
            InternalCountHistogramFacet histoFacet = (InternalCountHistogramFacet) facets.get(0);
            if (histoFacet.entries == null) {
                histoFacet.entries = new CountEntry[histoFacet.counts.size()];
                int i = 0;
                for (TLongLongIterator it = histoFacet.counts.iterator(); it.hasNext(); ) {
                    it.advance();
                    histoFacet.entries[i++] = new CountEntry(it.key(), it.value());
                }
            }
            Arrays.sort(histoFacet.entries, histoFacet.comparatorType.comparator());
            histoFacet.releaseCache();
            return facets.get(0);
        }

        TLongLongHashMap counts = context.cacheRecycler().popLongLongMap();
        for (Facet facet : facets) {
            InternalCountHistogramFacet histoFacet = (InternalCountHistogramFacet) facet;
            if (histoFacet.entries != null) {
                for (Entry entry : histoFacet.entries) {
                    counts.adjustOrPutValue(entry.getKey(), entry.getCount(), entry.getCount());
                }
            } else {
                for (TLongLongIterator it = histoFacet.counts.iterator(); it.hasNext(); ) {
                    it.advance();
                    counts.adjustOrPutValue(it.key(), it.value(), it.value());
                }
            }
            histoFacet.releaseCache();
        }
        CountEntry[] entries = new CountEntry[counts.size()];
        int i = 0;
        for (TLongLongIterator it = counts.iterator(); it.hasNext(); ) {
            it.advance();
            entries[i++] = new CountEntry(it.key(), it.value());
        }
        context.cacheRecycler().pushLongLongMap(counts);

        Arrays.sort(entries, comparatorType.comparator());

        return new InternalCountHistogramFacet(getName(), comparatorType, entries);
    }

    static final class Fields {
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString ENTRIES = new XContentBuilderString("entries");
        static final XContentBuilderString KEY = new XContentBuilderString("key");
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(getName());
        builder.field(Fields._TYPE, HistogramFacet.TYPE);
        builder.startArray(Fields.ENTRIES);
        for (Entry entry : entries) {
            builder.startObject();
            builder.field(Fields.KEY, entry.getKey());
            builder.field(Fields.COUNT, entry.getCount());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public static InternalCountHistogramFacet readHistogramFacet(StreamInput in) throws IOException {
        InternalCountHistogramFacet facet = new InternalCountHistogramFacet();
        facet.readFrom(in);
        return facet;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        comparatorType = ComparatorType.fromId(in.readByte());
        int size = in.readVInt();
        entries = new CountEntry[size];
        for (int i = 0; i < size; i++) {
            entries[i] = new CountEntry(in.readLong(), in.readVLong());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeByte(comparatorType.id());
        if (entries != null) {
            out.writeVInt(entries.length);
            for (CountEntry entry : entries) {
                out.writeLong(entry.getKey());
                out.writeVLong(entry.getCount());
            }
        } else {
            // optimize the write, since we know we have the same buckets as keys
            out.writeVInt(counts.size());
            for (TLongLongIterator it = counts.iterator(); it.hasNext(); ) {
                it.advance();
                out.writeLong(it.key());
                out.writeVLong(it.value());
            }
        }
        releaseCache();
    }
}
