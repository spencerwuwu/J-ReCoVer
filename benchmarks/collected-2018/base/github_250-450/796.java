// https://searchcode.com/api/result/72056063/

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

package org.elasticsearch.search.facet.terms.longs;

import com.google.common.collect.ImmutableList;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.hash.TLongIntHashMap;
import org.elasticsearch.common.CacheRecycler;
import org.elasticsearch.common.collect.BoundedTreeSet;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.text.StringText;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.terms.InternalTermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class InternalLongTermsFacet extends InternalTermsFacet {

    private static final String STREAM_TYPE = "lTerms";

    public static void registerStream() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static Stream STREAM = new Stream() {
        @Override
        public Facet readFacet(String type, StreamInput in) throws IOException {
            return readTermsFacet(in);
        }
    };

    @Override
    public String streamType() {
        return STREAM_TYPE;
    }

    public static class LongEntry implements Entry {

        long term;
        int count;

        public LongEntry(long term, int count) {
            this.term = term;
            this.count = count;
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
        public int getCount() {
            return count;
        }

        @Override
        public int compareTo(Entry o) {
            long anotherVal = ((LongEntry) o).term;
            if (term < anotherVal) {
                return -1;
            }
            if (term == anotherVal) {
                int i = count - o.getCount();
                if (i == 0) {
                    i = System.identityHashCode(this) - System.identityHashCode(o);
                }
                return i;
            }
            return 1;
        }
    }

    int requiredSize;
    long missing;
    long total;
    Collection<LongEntry> entries = ImmutableList.of();
    ComparatorType comparatorType;

    InternalLongTermsFacet() {
    }

    public InternalLongTermsFacet(String name, ComparatorType comparatorType, int requiredSize, Collection<LongEntry> entries, long missing, long total) {
        super(name);
        this.comparatorType = comparatorType;
        this.requiredSize = requiredSize;
        this.entries = entries;
        this.missing = missing;
        this.total = total;
    }

    @Override
    public List<LongEntry> getEntries() {
        if (!(entries instanceof List)) {
            entries = ImmutableList.copyOf(entries);
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
    public long getTotalCount() {
        return this.total;
    }

    @Override
    public long getOtherCount() {
        long other = total;
        for (Entry entry : entries) {
            other -= entry.getCount();
        }
        return other;
    }

    @Override
    public Facet reduce(List<Facet> facets) {
        if (facets.size() == 1) {
            return facets.get(0);
        }
        InternalLongTermsFacet first = (InternalLongTermsFacet) facets.get(0);
        TLongIntHashMap aggregated = CacheRecycler.popLongIntMap();
        long missing = 0;
        long total = 0;
        for (Facet facet : facets) {
            InternalLongTermsFacet mFacet = (InternalLongTermsFacet) facet;
            missing += mFacet.getMissingCount();
            total += mFacet.getTotalCount();
            for (LongEntry entry : mFacet.entries) {
                aggregated.adjustOrPutValue(entry.term, entry.getCount(), entry.getCount());
            }
        }

        BoundedTreeSet<LongEntry> ordered = new BoundedTreeSet<LongEntry>(first.comparatorType.comparator(), first.requiredSize);
        for (TLongIntIterator it = aggregated.iterator(); it.hasNext(); ) {
            it.advance();
            ordered.add(new LongEntry(it.key(), it.value()));
        }
        first.entries = ordered;
        first.missing = missing;
        first.total = total;

        CacheRecycler.pushLongIntMap(aggregated);

        return first;
    }

    static final class Fields {
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString MISSING = new XContentBuilderString("missing");
        static final XContentBuilderString TOTAL = new XContentBuilderString("total");
        static final XContentBuilderString OTHER = new XContentBuilderString("other");
        static final XContentBuilderString TERMS = new XContentBuilderString("terms");
        static final XContentBuilderString TERM = new XContentBuilderString("term");
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(getName());
        builder.field(Fields._TYPE, TermsFacet.TYPE);
        builder.field(Fields.MISSING, missing);
        builder.field(Fields.TOTAL, total);
        builder.field(Fields.OTHER, getOtherCount());
        builder.startArray(Fields.TERMS);
        for (LongEntry entry : entries) {
            builder.startObject();
            builder.field(Fields.TERM, entry.term);
            builder.field(Fields.COUNT, entry.getCount());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public static InternalLongTermsFacet readTermsFacet(StreamInput in) throws IOException {
        InternalLongTermsFacet facet = new InternalLongTermsFacet();
        facet.readFrom(in);
        return facet;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        comparatorType = ComparatorType.fromId(in.readByte());
        requiredSize = in.readVInt();
        missing = in.readVLong();
        total = in.readVLong();

        int size = in.readVInt();
        entries = new ArrayList<LongEntry>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new LongEntry(in.readLong(), in.readVInt()));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeByte(comparatorType.id());
        out.writeVInt(requiredSize);
        out.writeVLong(missing);
        out.writeVLong(total);

        out.writeVInt(entries.size());
        for (LongEntry entry : entries) {
            out.writeLong(entry.term);
            out.writeVInt(entry.getCount());
        }
    }
}
