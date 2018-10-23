// https://searchcode.com/api/result/3887527/

/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package org.gbif.checklistbank.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.checklistbank.Constants;
import org.gbif.checklistbank.cache.CacheUtils;
import org.gbif.checklistbank.cache.NameStringCache;
import org.gbif.checklistbank.cache.NameUsageCache;
import org.gbif.checklistbank.fuzzy.FuzzyNameMatcherFactory;
import org.gbif.checklistbank.model.lite.NameStringLite;
import org.gbif.checklistbank.model.lite.NameUsageLite;
import org.gbif.checklistbank.service.TermService;
import org.gbif.checklistbank.service.impl.PgSqlBaseService;
import org.gbif.checklistbank.utils.RankUtil;
import org.gbif.ecat.fuzzy.FuzzyNameMatch;
import org.gbif.ecat.fuzzy.FuzzyNameMatcher;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.parser.NameParser;
import org.gbif.ecat.parser.UnparsableException;
import org.gbif.ecat.utils.LoggingUtils;
import org.gbif.ecat.utils.PrimitiveUtils;
import org.gbif.ecat.voc.Rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POTENTIAL IDEAS FOR IMPROVING
 * 1 simple author matching
 * 2 compare name stems of the higher ranks instead of full name? Zingiber, Zingiberaceae, Zingiberales all have
 * Zingiber as the stem
 * 3 also look for kingdoms in phylum and phylum in class. Sometimes these are correctly placed differently
 * 4 correct higher classification if suffices are obviously wrong? An order ending in *ales in a family should be
 * swapped?
 * 5 add better datasets - molluscs and palaeolontological in particular: FaunaEuropaea will solve the Arion
 * lusitanicus
 * issue
 *
 * @author markus
 */
@Singleton
public class NubLookup {

  private class CountReporter extends Thread {

    @Override
    public void run() {

      boolean interrupted = false;
      while (!interrupted) {
        log.info("Lookups: [{}] queries, [{}] straight, [{}] fuzzy hits, [{}] hail marys, [{}] misses [{}]",
          new Object[] {nameLookups.longValue(), luceneQueries.longValue(), straightHits.longValue(),
            fuzzyHits.longValue(), hailMarys.longValue(), misses.longValue()});
        long straightPct = Math.round(straightHits.doubleValue() * 100 / luceneQueries.doubleValue());
        long fuzzyPct = Math.round(fuzzyHits.doubleValue() * 100 / luceneQueries.doubleValue());
        long hitPct = Math.round((nameLookups.doubleValue() - misses.doubleValue()) * 100 / nameLookups.doubleValue());
        log.info("Percentage of lucene straight hit [{}%]", straightPct);
        log.info("Percentage of lucene fuzzy    hit [{}%]", fuzzyPct);
        log.info("Percentage successful lookups     [{}%]", hitPct);
        try {
          Thread.sleep(60000);
        } catch (InterruptedException e) {
          log.info("Reporter thread interrupted, exiting");
          interrupted = true;
        }
      }

    }
  }

  // Count reporter stats
  private final AtomicLong nameLookups = new AtomicLong(1);
  private final AtomicLong luceneQueries = new AtomicLong(1);
  private final AtomicLong straightHits = new AtomicLong(0);
  private final AtomicLong fuzzyHits = new AtomicLong(0);
  private final AtomicLong hailMarys = new AtomicLong(0);
  private final AtomicLong misses = new AtomicLong(0);
  private CountReporter reporterThread;

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final TermService termService;
  private final PgSqlBaseService psqlService;
  private final ClassificationMatcher matcher;
  private final NameParser parser;
  private final FuzzyNameMatcherFactory fuzzyFactory;

  private FuzzyNameMatcher fuzzy;
  private NameStringCache nsCache;
  private NameUsageCache nuCache;
  private TIntObjectHashMap<TIntHashSet> nameUsageByNameId;

  private Map<String, NameUsageMatch> hackMap = Maps.newHashMap();

  private static final Rank[] HIGHER_QUERY_RANK = new Rank[6];
  static {
    HIGHER_QUERY_RANK[0] = Rank.GENUS;
    HIGHER_QUERY_RANK[1] = Rank.FAMILY;
    HIGHER_QUERY_RANK[2] = Rank.ORDER;
    HIGHER_QUERY_RANK[3] = Rank.CLASS;
    HIGHER_QUERY_RANK[4] = Rank.PHYLUM;
    HIGHER_QUERY_RANK[5] = Rank.KINGDOM;
  }

  @Inject
  public NubLookup(HigherTaxaLookup synonyms, PgSqlBaseService baseService,
    TermService termService, NameParser parser, FuzzyNameMatcherFactory fuzzyFactory) {
    this.matcher = new ClassificationMatcher(synonyms);
    this.termService = termService;
    this.psqlService = baseService;
    this.parser = parser;
    this.fuzzyFactory = fuzzyFactory;
    loadNubData();
    initHackMap();
    reporterThread = new CountReporter();
    reporterThread.start();
  }

  public NubLookup(HigherTaxaLookup synonyms, TermService termService, NameParser parser,
    NameStringCache nc, NameUsageCache nu, FuzzyNameMatcherFactory fuzzyFactory) {
    this.matcher = new ClassificationMatcher(synonyms);
    this.termService = termService;
    this.psqlService = null;
    this.parser = parser;
    this.fuzzyFactory = fuzzyFactory;
    initCaches(nc, nu);
    initHackMap();
  }

  private void initHackMap(){
    log.debug("Add entries to hackmap ...");
    addLookupResultHack("Radiolaria", 7);
  }

  private void addLookupResultHack(String lookupName, Integer uid){
    if (uid == null || Strings.isNullOrEmpty(lookupName)){
      return;
    }
    NameUsageLite usage = nuCache.get(uid);
    if (usage == null){
      return;
    }

    NameUsageMatch r = new NameUsageMatch();
    r.setUsageKey(uid);
    r.setScientificName(getUsageName(uid));
    r.setRank(RankUtil.toApi(usage.rankFk));

    setHigherTaxon(r, usage.kingdomFk, Rank.KINGDOM);
    setHigherTaxon(r, usage.phylumFk, Rank.PHYLUM);
    setHigherTaxon(r, usage.classFk, Rank.CLASS);
    setHigherTaxon(r, usage.orderFk, Rank.ORDER);
    setHigherTaxon(r, usage.familyFk, Rank.FAMILY);
    setHigherTaxon(r, usage.genusFk, Rank.GENUS);
    setHigherTaxon(r, usage.speciesFk, Rank.SPECIES);

    log.debug("Adding hack result {} for {}", r.getUsageKey(), lookupName);

    hackMap.put(lookupName.toLowerCase(), r);
  }
  private void setHigherTaxon(NameUsageMatch r, int uid, Rank rank){
    Integer id = PrimitiveUtils.toInteger(uid);
    if (id != null){
      org.gbif.api.vocabulary.Rank apiRank = RankUtil.toApi(rank);
      ClassificationUtils.setHigherRankKey(r, apiRank, id);
      ClassificationUtils.setHigherRank(r, apiRank, getUsageName(id));
    }
  }
  private String getUsageName(Integer uid){
    if (uid != null && uid >= 0 ){
      NameUsageLite usage = nuCache.get(uid);
      if (usage != null){
        NameStringLite name = nsCache.get(usage.nameFk);
        if (name != null){
          return name.scientificName;
        }
      }
    }
    return null;
  }

  private NameUsageMatch extractBestMatch(ClassifiedName query, Collection<FuzzyNameMatch> matches) {
    Map<ClassifiedName, Integer> ratings = getMatchRatings(query, matches);
    // assemble best match
    int max = -999999999;
    boolean equalMatchExists = false;
    ClassifiedName best = null;
    for (ClassifiedName ref : ratings.keySet()) {
      int confidence = ratings.get(ref);
      // only use matches with a confidence above zero
      // below its unreliable and might have too many higher classification problems
      if (confidence >= 0) {
        if (best == null || max < confidence) {
          best = ref;
          max = confidence;
          equalMatchExists = false;
        } else if (max == confidence) {
          equalMatchExists = true;
        }
      }
    }
    if (best == null) {
      return null;
    }
    // dont return anything if we have equally well matching results that we cant disambiguate
    if (equalMatchExists) {
      log.debug("Unable to distinguish multiple matches with equal similarity for query: [{}]",
        query.getScientificName());
      return null;
    }
    // dont return bad matches, only ones we trust
    if (max < 0) {
      log.debug("Ignore unreliable match for query: [{}]", query.getScientificName());
      return null;
    }

    // boost results with a single match to pick from
    if (ratings.size() == 1) {
      max += 5;
    }

    NameUsageMatch result = build(best);
    // derive confidence from matching rate
    result.setConfidence(max);

    return result;
  }

  private NameUsageMatch build(ClassifiedName source){
    NameUsageMatch m = new NameUsageMatch();
    m.setClazz(source.getC());
    m.setClassKey(PrimitiveUtils.toInteger(source.getCid()));
    m.setFamily(source.getF());
    m.setFamilyKey(PrimitiveUtils.toInteger(source.getFid()));
    m.setGenus(source.getG());
    m.setGenusKey(PrimitiveUtils.toInteger(source.getGid()));
    m.setKingdom(source.getK());
    m.setKingdomKey(PrimitiveUtils.toInteger(source.getKid()));
    m.setOrder(source.getO());
    m.setOrderKey(PrimitiveUtils.toInteger(source.getOid()));
    m.setPhylum(source.getP());
    m.setPhylumKey(PrimitiveUtils.toInteger(source.getPid()));
    m.setRank(RankUtil.toApi(source.getRank()));
    m.setSpecies(source.getS());
    m.setSpeciesKey(PrimitiveUtils.toInteger(source.getSid()));
    m.setScientificName(source.getScientificName());
    m.setSynonym(source.isSynonym());
    m.setUsageKey(PrimitiveUtils.toInteger(source.getUsageId()));
    return m;
  }
  private Map<ClassifiedName, Integer> getMatchRatings(ClassifiedName query, Collection<FuzzyNameMatch> matches) {
    // lookup usages for the given name ids
    TIntObjectHashMap<FuzzyNameMatch> fuzzyUsages = new TIntObjectHashMap<FuzzyNameMatch>();
    for (FuzzyNameMatch n : matches) {
      for (int usageId : nameUsageByNameId.get(n.id).toArray()) {
        fuzzyUsages.put(usageId, n);
      }
    }

    // convert usage ids to classified names
    List<ClassifiedName> names = Lists.newArrayList();
    TIntObjectIterator<FuzzyNameMatch> iter = fuzzyUsages.iterator();
    while (iter.hasNext()) {
      iter.advance();
      int usageID = iter.key();
      ClassifiedName n = usage2param(usageID);
      if (n != null) {
        names.add(n);
      }
    }

    // rate matches, reusing the homonym informer with its name usage/string
    // cache and classification comparison routines
    // comparisons are done on provided candidates, not only homonyms
    Map<ClassifiedName, Integer> classificationRatings = matcher.matchClassifications(query, names);

    // add name string similarity to rating
    // give straight binomial matches a huge boost!
    Map<ClassifiedName, Integer> matchRating = Maps.newHashMap();
    for (Map.Entry<ClassifiedName, Integer> classifiedNameIntegerEntry : classificationRatings.entrySet()) {
      // start with the rating for the higher classification match
      int confidence = classifiedNameIntegerEntry.getValue();
      FuzzyNameMatch match = fuzzyUsages.get(classifiedNameIntegerEntry.getKey().getUsageId());
      // rate the fuzzyness of the match.
      // straight matches get 25, fuzzy ones 25 less for each character difference
      if (match.similarity >= 1.0f) {
        // straight match
        confidence += 25;
        // binomial straight match? That is pretty trustworthy
        if (match.name.contains(" ")) {
          confidence += 30;
        }
      } else {
        // fuzzy - be careful!
        confidence += 5;
        confidence -= 20 * convertSimilarityToEditDistance(match.name, match.similarity);
        // binomial at least? That is slightly more trustworthy
        if (match.name.contains(" ")) {
          confidence += 10;
        }
      }

      matchRating.put(classifiedNameIntegerEntry.getKey(), confidence);
    }

    return matchRating;
  }

  /**
   * @param name       string that is the similarity is based on
   * @param similarity 0 to 1 with 1 being identical (returns 0)
   *
   * @return the number of chars needed to be replaced to resemble the similarity, zero or greater
   */
  protected static int convertSimilarityToEditDistance(String name, float similarity) {
    // reduce by -0.0001f to avoid floating point rounding troubles
    return (int) Math.ceil(name.length() * (100.0f * (1.0f - similarity - 0.0001f)) / 100);
  }

  /**
   * Init caches using passed name & usage caches that have to be consistent, i.e. all names referenced by usages need
   * to exist!
   */
  private void initCaches(NameStringCache nsCache, NameUsageCache nuCache) {
    this.nsCache = nsCache;
    log.info("Using name string cache with [{}] names", nsCache.size());

    this.nuCache = nuCache;
    log.info("Using nub usage cache with [{}] usages", nuCache.size());

    log.debug("Setting up lookup maps ...");
    // fill usage by name id cache
    nameUsageByNameId = new TIntObjectHashMap<TIntHashSet>();
    TIntObjectIterator<NameUsageLite> iter = nuCache.iterator();
    while (iter.hasNext()) {
      iter.advance();
      NameUsageLite u = iter.value();
      if (!nameUsageByNameId.contains(u.nameFk)) {
        nameUsageByNameId.put(u.nameFk, new TIntHashSet());
      }
      nameUsageByNameId.get(u.nameFk).add(u.id);
    }

    // fuzzy matcher
    log.debug("Creating fuzzy matcher...");
    List<NameStringLite> nubNames = new ArrayList<NameStringLite>();
    TIntObjectIterator<NameUsageLite> nubUsageIter = nuCache.iterator();
    while (nubUsageIter.hasNext()) {
      nubUsageIter.advance();
      NameUsageLite u = nubUsageIter.value();
      NameStringLite n = nsCache.get(u.nameFk);
      if ( n== null || Strings.isNullOrEmpty(NameStringLite.buildCanonicalName(n))){
        String x = NameStringLite.buildCanonicalName(n);
      }
      nubNames.add(nsCache.get(u.nameFk));
    }
    fuzzy = fuzzyFactory.build(nubNames);

    log.info("Successfully initialised nub lookup");
  }

  /**
   * If class was initialised with a db service, (re)load all nub data for the lookup.
   * If it was initiliased with a cache given, raise an exception.
   *
   * @throws IllegalStateException if no baseService is present
   */
  public void loadNubData() {
    long start = System.currentTimeMillis();

    // to save memory first clear anythign existing!
    log.debug("Clear existing caches ...");
    nsCache=null;
    nuCache=null;
    nameUsageByNameId=null;
    fuzzy=null;

    log.info("Loading nub lookup caches ...");
    // fill name string cache
    NameStringCache nsc = new NameStringCache(psqlService, Constants.NUB_CHECKLIST_ID);
    log.info("Loaded name string cache with [{}] names in [{}]", nsc.size(), LoggingUtils.timeSince(start));
    long start2 = System.currentTimeMillis();

    // fill usage cache
    NameUsageCache nuc = new NameUsageCache(psqlService, true, false);
    log.info("Loaded nub usage cache with [{}] usages in [{}]", nuc.size(), LoggingUtils.timeSince(start2));

    initCaches(nsc, nuc);
  }
  /**
   * In case of equally rated canonical matches we return empty right now.
   * In case of fuzzy matches only with no higher classification given it is empty too.
   * TODO: improve matching and try further authorship matching in the following order:
   * 1) full authors
   * 2) year
   * 3) author initial
   * If still equal matches exist return null
   *
   */
  public NameUsageMatch matchNub(ClassifiedName query) {
    log.debug(">> matchNub");
    log.debug("Looking up sci name [{}]", query.getScientificName());
    // parse query name to its canonical form
    ParsedName<?> cn = null;
    try {
      cn = parser.parse(query.getScientificName());
    } catch (UnparsableException e) {
      // hybrid names, virus names & blacklisted ones - dont provide any parsed name
      log.warn("Unparsable [{}] name [{}]", e.type, query.getScientificName());
      cn = null;
    }

    return matchNub(query, cn);
  }

  /**
   * @param queryParsedName null if it cant be parsed
   */
  public NameUsageMatch matchNub(ClassifiedName originalQuery, @Nullable ParsedName<?> queryParsedName) {
    nameLookups.incrementAndGet();

    long start = System.currentTimeMillis();

    // test if name has an abbreviated genus
    if (queryParsedName != null
      && originalQuery.getG() != null
      && queryParsedName.genusOrAbove != null
      && originalQuery.getG().length() > 1) {
      if (queryParsedName.genusOrAbove.length() == 2
        && queryParsedName.genusOrAbove.charAt(1) == '.'
        && queryParsedName.genusOrAbove.charAt(0) == originalQuery.getG().charAt(0)
        || queryParsedName.genusOrAbove.length() == 1 && queryParsedName.genusOrAbove.charAt(0) == originalQuery.getG()
        .charAt(0)) {
        queryParsedName.genusOrAbove = originalQuery.getG();
      }
    }
    // keep this query for later - now we are free to modify the original query
    ClassifiedName query = new ClassifiedName(originalQuery);

    // use canonical name for matching if available
    if (queryParsedName != null) {
      query.setScientificName(queryParsedName.canonicalName());
    }
    log.debug("Nub lookup for [{}]", query.getScientificName());

    // first try matching against scientific name
    NameUsageMatch result = queryScientificName(query, 0.8f, false);

    // include species or genus only matches ?
    if (result == null) {
      // no or bad match, include fuzzy matches
      // find potential fuzzy matches, add them to straight matches
      if (queryParsedName != null && queryParsedName.genusOrAbove != null) {
        if (queryParsedName.specificEpithet != null) {
          // species
          String species = queryParsedName.canonicalSpeciesName();
          // only match if species name isnt the same as the former canonical
          if (!species.equalsIgnoreCase(query.getScientificName())) {
            log.debug("Match against species [{}]", species);
            query.setScientificName(species);
            query.setRank(Rank.SPECIES);
            result = queryScientificName(query, 0.8f, true);
            query.setS(null);
          }
        }
        // try genus match if no result still
        if (result == null) {
          if (!queryParsedName.genusOrAbove.equalsIgnoreCase(query.getScientificName())) {
            log.debug("Match against genus [{}]", queryParsedName.genusOrAbove);
            query.setScientificName(queryParsedName.genusOrAbove);
            // we're not sure if this is really a genus, so dont set the rank
            // we get non species names sometimes like "Chaetognatha eyecount" that refer to a phylum called
            // "Chaetognatha"
            query.setRank(null);
            // only allow straight matches
            result = queryScientificName(query, 1.0f, true);
          }
        }
      }

      if (result == null) {
        hailMarys.incrementAndGet();
        // last resort - try higher ranks genus-kingdom
        for (Rank rank : HIGHER_QUERY_RANK) {
          String name = query.getByRank(rank);
          if (!StringUtils.isEmpty(name)) {
            log.debug("Match against [{}] [{}]", rank.name(), name);
            query.setScientificName(name);
            query.setRank(rank);
            // only allow straight matches
            result = queryScientificName(query, 1.0f, true);
            if (result != null) {
              break;
            }
          }
          // remove this rank from query for next higher rank
          query.setByRank(rank, null);
        }
      }

      // if finally we cant find anything, return empty result object - but not null!
      if (result == null) {
        misses.incrementAndGet();
        result = new NameUsageMatch();
        result.setConfidence(100);
        result.setMatchType(NameUsageMatch.MatchType.NONE);
      }

    }


    log.debug("Lookup result in [{}]", LoggingUtils.timeSince(start));
    return result;
  }

  /**
   * Manual map of lookup results to fix issue
   * http://dev.gbif.org/issues/browse/CLB-81
   * TODO: remove this hack once the lookup algorithm deals with interrank homonyms
   * @param query
   * @return
   */
  private NameUsageMatch queryHackMap(ClassifiedName query) {
    if (query.getScientificName() != null && hackMap.containsKey(query.getScientificName().toLowerCase())){
      NameUsageMatch result = hackMap.get(query.getScientificName().toLowerCase());
      result.setConfidence(100);
      result.setMatchType(NameUsageMatch.MatchType.EXACT);
      return result;
    }
    return null;
  }

  private NameUsageMatch queryScientificName(ClassifiedName query, float similarity, boolean higherMatch) {
    String name = query.getScientificName();

    // check our "hackmap" for manual entries
    NameUsageMatch result = queryHackMap(query);
    if (result != null){
      log.debug("Manual match found against [{}]", name);
      return result;
    }

    luceneQueries.incrementAndGet();

    // try straight matching first
    List<FuzzyNameMatch> candidateNames = fuzzy.straightMatch(name);
    int straightSize = candidateNames.size();
    log.debug("[{}] straight matches against [{}]", straightSize, name);
    result = extractBestMatch(query, candidateNames);

    if (result != null) {
      result.setMatchType(NameUsageMatch.MatchType.EXACT);
      straightHits.incrementAndGet();
    } else {
      // no straight matches or very low confidence - try fuzzy ones
      // ... but only if the query has some classification to verify the results with
      if (query.hasClassification()) {
        candidateNames.addAll(fuzzy.fuzzyMatch(name, similarity));
        log.debug("[{}] fuzzy matches against [{}]", candidateNames.size() - straightSize, name);
        result = extractBestMatch(query, candidateNames);
        if (result != null) {
          result.setMatchType(NameUsageMatch.MatchType.FUZZY);
          fuzzyHits.incrementAndGet();
        }
      } else {
        log.debug("Don't do fuzzy matching, query contains no classification");
      }
    }

    if (result != null && higherMatch){
      result.setMatchType(NameUsageMatch.MatchType.HIGHERRANK);
    }

    return result;
  }

  private ClassifiedName usage2param(int usageID) {
    return CacheUtils.usage2classifiedName(usageID, nuCache, nsCache, termService);
  }
}

