// https://searchcode.com/api/result/72055022/

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

package org.elasticsearch.search.facet;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public abstract class InternalFacet implements Facet, Streamable, ToXContent {

    private String facetName;

    /**
     * Here just for streams...
     */
    protected InternalFacet() {

    }

    protected InternalFacet(String facetName) {
        this.facetName = facetName;
    }

    public abstract String streamType();

    public abstract Facet reduce(List<Facet> facets);

    public static interface Stream {
        Facet readFacet(String type, StreamInput in) throws IOException;
    }

    public static class Streams {

        private static ImmutableMap<String, Stream> streams = ImmutableMap.of();

        public static synchronized void registerStream(Stream stream, String... types) {
            MapBuilder<String, Stream> uStreams = MapBuilder.newMapBuilder(streams);
            for (String type : types) {
                uStreams.put(type, stream);
            }
            streams = uStreams.immutableMap();
        }

        public static Stream stream(String type) {
            return streams.get(type);
        }
    }

    @Override
    public final String getName() {
        return facetName;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        facetName = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(facetName);
    }
}

