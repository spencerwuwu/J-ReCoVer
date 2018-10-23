// https://searchcode.com/api/result/75772923/

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

package org.elasticsearch.search.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.PriorityQueue;
import org.elasticsearch.cache.recycler.CacheRecycler;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.XMaps;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.trove.ExtTIntArrayList;
import org.elasticsearch.common.util.concurrent.AtomicArray;
import org.elasticsearch.search.dfs.AggregatedDfs;
import org.elasticsearch.search.dfs.DfsSearchResult;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.InternalFacet;
import org.elasticsearch.search.facet.InternalFacets;
import org.elasticsearch.search.fetch.FetchSearchResult;
import org.elasticsearch.search.fetch.FetchSearchResultProvider;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.query.QuerySearchResult;
import org.elasticsearch.search.query.QuerySearchResultProvider;
import org.elasticsearch.search.suggest.Suggest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SearchPhaseController extends AbstractComponent {

    public static Ordering<AtomicArray.Entry<? extends QuerySearchResultProvider>> QUERY_RESULT_ORDERING = new Ordering<AtomicArray.Entry<? extends QuerySearchResultProvider>>() {
        @Override
        public int compare(@Nullable AtomicArray.Entry<? extends QuerySearchResultProvider> o1, @Nullable AtomicArray.Entry<? extends QuerySearchResultProvider> o2) {
            int i = o1.value.shardTarget().index().compareTo(o2.value.shardTarget().index());
            if (i == 0) {
                i = o1.value.shardTarget().shardId() - o2.value.shardTarget().shardId();
            }
            return i;
        }
    };

    public static final ScoreDoc[] EMPTY_DOCS = new ScoreDoc[0];

    private final CacheRecycler cacheRecycler;
    private final boolean optimizeSingleShard;

    @Inject
    public SearchPhaseController(Settings settings, CacheRecycler cacheRecycler) {
        super(settings);
        this.cacheRecycler = cacheRecycler;
        this.optimizeSingleShard = componentSettings.getAsBoolean("optimize_single_shard", true);
    }

    public boolean optimizeSingleShard() {
        return optimizeSingleShard;
    }

    public AggregatedDfs aggregateDfs(AtomicArray<DfsSearchResult> results) {
        Map<Term, TermStatistics> termStatistics = XMaps.newNoNullKeysMap();
        Map<String, CollectionStatistics> fieldStatistics = XMaps.newNoNullKeysMap();
        long aggMaxDoc = 0;
        for (AtomicArray.Entry<DfsSearchResult> lEntry : results.asList()) {
            final Term[] terms = lEntry.value.terms();
            final TermStatistics[] stats = lEntry.value.termStatistics();
            assert terms.length == stats.length;
            for (int i = 0; i < terms.length; i++) {
                assert terms[i] != null;
                TermStatistics existing = termStatistics.get(terms[i]);
                if (existing != null) {
                    assert terms[i].bytes().equals(existing.term());
                    // totalTermFrequency is an optional statistic we need to check if either one or both
                    // are set to -1 which means not present and then set it globally to -1
                    termStatistics.put(terms[i], new TermStatistics(existing.term(),
                            existing.docFreq() + stats[i].docFreq(),
                            optionalSum(existing.totalTermFreq(), stats[i].totalTermFreq())));
                } else {
                    termStatistics.put(terms[i], stats[i]);
                }

            }
            for (Map.Entry<String, CollectionStatistics> entry : lEntry.value.fieldStatistics().entrySet()) {
                assert entry.getKey() != null;
                CollectionStatistics existing = fieldStatistics.get(entry.getKey());
                if (existing != null) {
                    CollectionStatistics merged = new CollectionStatistics(
                            entry.getKey(), existing.maxDoc() + entry.getValue().maxDoc(),
                            optionalSum(existing.docCount(), entry.getValue().docCount()),
                            optionalSum(existing.sumTotalTermFreq(), entry.getValue().sumTotalTermFreq()),
                            optionalSum(existing.sumDocFreq(), entry.getValue().sumDocFreq())
                    );
                    fieldStatistics.put(entry.getKey(), merged);
                } else {
                    fieldStatistics.put(entry.getKey(), entry.getValue());
                }
            }
            aggMaxDoc += lEntry.value.maxDoc();
        }
        return new AggregatedDfs(termStatistics, fieldStatistics, aggMaxDoc);
    }

    private static long optionalSum(long left, long right) {
        return Math.min(left, right) == -1 ? -1 : left + right;
    }

    public ScoreDoc[] sortDocs(AtomicArray<? extends QuerySearchResultProvider> results1) {
        if (results1.asList().isEmpty()) {
            return EMPTY_DOCS;
        }

        if (optimizeSingleShard) {
            boolean canOptimize = false;
            QuerySearchResult result = null;
            int shardIndex = -1;
            if (results1.asList().size() == 1) {
                canOptimize = true;
                result = results1.asList().get(0).value.queryResult();
                shardIndex = results1.asList().get(0).index;
            } else {
                // lets see if we only got hits from a single shard, if so, we can optimize...
                for (AtomicArray.Entry<? extends QuerySearchResultProvider> entry : results1.asList()) {
                    if (entry.value.queryResult().topDocs().scoreDocs.length > 0) {
                        if (result != null) { // we already have one, can't really optimize
                            canOptimize = false;
                            break;
                        }
                        canOptimize = true;
                        result = entry.value.queryResult();
                        shardIndex = entry.index;
                    }
                }
            }
            if (canOptimize) {
                ScoreDoc[] scoreDocs = result.topDocs().scoreDocs;
                if (scoreDocs.length < result.from()) {
                    return EMPTY_DOCS;
                }
                int resultDocsSize = result.size();
                if ((scoreDocs.length - result.from()) < resultDocsSize) {
                    resultDocsSize = scoreDocs.length - result.from();
                }
                if (result.topDocs() instanceof TopFieldDocs) {
                    ScoreDoc[] docs = new ScoreDoc[resultDocsSize];
                    for (int i = 0; i < resultDocsSize; i++) {
                        ScoreDoc scoreDoc = scoreDocs[result.from() + i];
                        scoreDoc.shardIndex = shardIndex;
                        docs[i] = scoreDoc;
                    }
                    return docs;
                } else {
                    ScoreDoc[] docs = new ScoreDoc[resultDocsSize];
                    for (int i = 0; i < resultDocsSize; i++) {
                        ScoreDoc scoreDoc = scoreDocs[result.from() + i];
                        scoreDoc.shardIndex = shardIndex;
                        docs[i] = scoreDoc;
                    }
                    return docs;
                }
            }
        }

        List<? extends AtomicArray.Entry<? extends QuerySearchResultProvider>> results = QUERY_RESULT_ORDERING.sortedCopy(results1.asList());
        QuerySearchResultProvider firstResult = results.get(0).value;

        int totalNumDocs = 0;

        int queueSize = firstResult.queryResult().from() + firstResult.queryResult().size();
        if (firstResult.includeFetch()) {
            // if we did both query and fetch on the same go, we have fetched all the docs from each shards already, use them...
            // this is also important since we shortcut and fetch only docs from "from" and up to "size"
            queueSize *= results.size();
        }

        // we don't use TopDocs#merge here because with TopDocs#merge, when pagination, we need to ask for "from + size" topN
        // hits, which ends up creating a "from + size" ScoreDoc[], while in our implementation, we can actually get away with
        // just create "size" ScoreDoc (the reverse order in the queue). would be nice to improve TopDocs#merge to allow for
        // it in which case we won't need this logic...

        PriorityQueue queue;
        if (firstResult.queryResult().topDocs() instanceof TopFieldDocs) {
            // sorting, first if the type is a String, chance CUSTOM to STRING so we handle nulls properly (since our CUSTOM String sorting might return null)
            TopFieldDocs fieldDocs = (TopFieldDocs) firstResult.queryResult().topDocs();
            for (int i = 0; i < fieldDocs.fields.length; i++) {
                boolean allValuesAreNull = true;
                boolean resolvedField = false;
                for (AtomicArray.Entry<? extends QuerySearchResultProvider> entry : results) {
                    for (ScoreDoc doc : entry.value.queryResult().topDocs().scoreDocs) {
                        FieldDoc fDoc = (FieldDoc) doc;
                        if (fDoc.fields[i] != null) {
                            allValuesAreNull = false;
                            if (fDoc.fields[i] instanceof String) {
                                fieldDocs.fields[i] = new SortField(fieldDocs.fields[i].getField(), SortField.Type.STRING, fieldDocs.fields[i].getReverse());
                            }
                            resolvedField = true;
                            break;
                        }
                    }
                    if (resolvedField) {
                        break;
                    }
                }
                if (!resolvedField && allValuesAreNull && fieldDocs.fields[i].getField() != null) {
                    // we did not manage to resolve a field (and its not score or doc, which have no field), and all the fields are null (which can only happen for STRING), make it a STRING
                    fieldDocs.fields[i] = new SortField(fieldDocs.fields[i].getField(), SortField.Type.STRING, fieldDocs.fields[i].getReverse());
                }
            }
            queue = new ShardFieldDocSortedHitQueue(fieldDocs.fields, queueSize);

            // we need to accumulate for all and then filter the from
            for (AtomicArray.Entry<? extends QuerySearchResultProvider> entry : results) {
                QuerySearchResult result = entry.value.queryResult();
                ScoreDoc[] scoreDocs = result.topDocs().scoreDocs;
                totalNumDocs += scoreDocs.length;
                for (ScoreDoc doc : scoreDocs) {
                    doc.shardIndex = entry.index;
                    if (queue.insertWithOverflow(doc) == doc) {
                        // filled the queue, break
                        break;
                    }
                }
            }
        } else {
            queue = new ScoreDocQueue(queueSize); // we need to accumulate for all and then filter the from
            for (AtomicArray.Entry<? extends QuerySearchResultProvider> entry : results) {
                QuerySearchResult result = entry.value.queryResult();
                ScoreDoc[] scoreDocs = result.topDocs().scoreDocs;
                totalNumDocs += scoreDocs.length;
                for (ScoreDoc doc : scoreDocs) {
                    doc.shardIndex = entry.index;
                    if (queue.insertWithOverflow(doc) == doc) {
                        // filled the queue, break
                        break;
                    }
                }
            }

        }

        int resultDocsSize = firstResult.queryResult().size();
        if (firstResult.includeFetch()) {
            // if we did both query and fetch on the same go, we have fetched all the docs from each shards already, use them...
            resultDocsSize *= results.size();
        }
        if (totalNumDocs < queueSize) {
            resultDocsSize = totalNumDocs - firstResult.queryResult().from();
        }

        if (resultDocsSize <= 0) {
            return EMPTY_DOCS;
        }

        // we only pop the first, this handles "from" nicely since the "from" are down the queue
        // that we already fetched, so we are actually popping the "from" and up to "size"
        ScoreDoc[] shardDocs = new ScoreDoc[resultDocsSize];
        for (int i = resultDocsSize - 1; i >= 0; i--)      // put docs in array
            shardDocs[i] = (ScoreDoc) queue.pop();
        return shardDocs;
    }

    /**
     * Builds an array, with potential null elements, with docs to load.
     */
    public void fillDocIdsToLoad(AtomicArray<ExtTIntArrayList> docsIdsToLoad, ScoreDoc[] shardDocs) {
        for (ScoreDoc shardDoc : shardDocs) {
            ExtTIntArrayList list = docsIdsToLoad.get(shardDoc.shardIndex);
            if (list == null) {
                list = new ExtTIntArrayList(); // can't be shared!, uses unsafe on it later on
                docsIdsToLoad.set(shardDoc.shardIndex, list);
            }
            list.add(shardDoc.doc);
        }
    }

    public InternalSearchResponse merge(ScoreDoc[] sortedDocs, AtomicArray<? extends QuerySearchResultProvider> queryResults, AtomicArray<? extends FetchSearchResultProvider> fetchResults) {

        boolean sorted = false;
        int sortScoreIndex = -1;

        if (queryResults.asList().isEmpty()) {
            return InternalSearchResponse.EMPTY;
        }

        QuerySearchResult querySearchResult = queryResults.asList().get(0).value.queryResult();

        if (querySearchResult.topDocs() instanceof TopFieldDocs) {
            sorted = true;
            TopFieldDocs fieldDocs = (TopFieldDocs) querySearchResult.queryResult().topDocs();
            for (int i = 0; i < fieldDocs.fields.length; i++) {
                if (fieldDocs.fields[i].getType() == SortField.Type.SCORE) {
                    sortScoreIndex = i;
                }
            }
        }

        // merge facets
        InternalFacets facets = null;
        if (!queryResults.asList().isEmpty()) {
            // we rely on the fact that the order of facets is the same on all query results
            if (querySearchResult.facets() != null && querySearchResult.facets().facets() != null && !querySearchResult.facets().facets().isEmpty()) {
                List<Facet> aggregatedFacets = Lists.newArrayList();
                List<Facet> namedFacets = Lists.newArrayList();
                for (Facet facet : querySearchResult.facets()) {
                    // aggregate each facet name into a single list, and aggregate it
                    namedFacets.clear();
                    for (AtomicArray.Entry<? extends QuerySearchResultProvider> entry : queryResults.asList()) {
                        for (Facet facet1 : entry.value.queryResult().facets()) {
                            if (facet.getName().equals(facet1.getName())) {
                                namedFacets.add(facet1);
                            }
                        }
                    }
                    if (!namedFacets.isEmpty()) {
                        Facet aggregatedFacet = ((InternalFacet) namedFacets.get(0)).reduce(new InternalFacet.ReduceContext(cacheRecycler, namedFacets));
                        aggregatedFacets.add(aggregatedFacet);
                    }
                }
                facets = new InternalFacets(aggregatedFacets);
            }
        }

        // count the total (we use the query result provider here, since we might not get any hits (we scrolled past them))
        long totalHits = 0;
        float maxScore = Float.NEGATIVE_INFINITY;
        boolean timedOut = false;
        for (AtomicArray.Entry<? extends QuerySearchResultProvider> entry : queryResults.asList()) {
            QuerySearchResult result = entry.value.queryResult();
            if (result.searchTimedOut()) {
                timedOut = true;
            }
            totalHits += result.topDocs().totalHits;
            if (!Float.isNaN(result.topDocs().getMaxScore())) {
                maxScore = Math.max(maxScore, result.topDocs().getMaxScore());
            }
        }
        if (Float.isInfinite(maxScore)) {
            maxScore = Float.NaN;
        }

        // clean the fetch counter
        for (AtomicArray.Entry<? extends FetchSearchResultProvider> entry : fetchResults.asList()) {
            entry.value.fetchResult().initCounter();
        }

        // merge hits
        List<InternalSearchHit> hits = new ArrayList<InternalSearchHit>();
        if (!fetchResults.asList().isEmpty()) {
            for (ScoreDoc shardDoc : sortedDocs) {
                FetchSearchResultProvider fetchResultProvider = fetchResults.get(shardDoc.shardIndex);
                if (fetchResultProvider == null) {
                    continue;
                }
                FetchSearchResult fetchResult = fetchResultProvider.fetchResult();
                int index = fetchResult.counterGetAndIncrement();
                if (index < fetchResult.hits().internalHits().length) {
                    InternalSearchHit searchHit = fetchResult.hits().internalHits()[index];
                    searchHit.score(shardDoc.score);
                    searchHit.shard(fetchResult.shardTarget());

                    if (sorted) {
                        FieldDoc fieldDoc = (FieldDoc) shardDoc;
                        searchHit.sortValues(fieldDoc.fields);
                        if (sortScoreIndex != -1) {
                            searchHit.score(((Number) fieldDoc.fields[sortScoreIndex]).floatValue());
                        }
                    }

                    hits.add(searchHit);
                }
            }
        }

        // merge suggest results
        Suggest suggest = null;
        if (!queryResults.asList().isEmpty()) {
            final Map<String, List<Suggest.Suggestion>> groupedSuggestions = new HashMap<String, List<Suggest.Suggestion>>();
            boolean hasSuggestions = false;
            for (AtomicArray.Entry<? extends QuerySearchResultProvider> entry : queryResults.asList()) {
                Suggest shardResult = entry.value.queryResult().queryResult().suggest();

                if (shardResult == null) {
                    continue;
                }
                hasSuggestions = true;
                Suggest.group(groupedSuggestions, shardResult);
            }

            suggest = hasSuggestions ? new Suggest(Suggest.Fields.SUGGEST, Suggest.reduce(groupedSuggestions)) : null;
        }

        InternalSearchHits searchHits = new InternalSearchHits(hits.toArray(new InternalSearchHit[hits.size()]), totalHits, maxScore);
        return new InternalSearchResponse(searchHits, facets, suggest, timedOut);
    }

}

