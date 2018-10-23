// https://searchcode.com/api/result/102573200/

package zeroone3010.geogpxparser.cachelistparsers;

import zeroone3010.geogpxparser.CacheType;
import zeroone3010.geogpxparser.Geocache;
import zeroone3010.geogpxparser.tabular.CellData;
import zeroone3010.geogpxparser.tabular.TableData;
import zeroone3010.geogpxparser.tabular.TableRow;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Parses cache group statistics from the given list of caches: the number of
 * caches and different cache types each group has.
 *
 * @author Ville Saalo (http://coord.info/PR32K8V)
 */
public abstract class AbstractCacheGroupStatsParser implements ICachesToTabularDataParser {

    private Map<String, Group> groups = new LinkedHashMap<>();

    abstract String getTableId();

    abstract String getTableGroupColumnTitle();

    abstract CellData createTableGroupColumnRowContent(Group group);

    abstract String getCacheGroupKey(Geocache cache);

    /**
     * Returns a table with data about cache groups.
     *
     * @param caches A List of Geocache objects
     * @return A table that can be saved into a file in various formats
     */
    @Override
    public final TableData getTabularInfo(final List<Geocache> caches) {

        // Parse cache group info into a map:
        for (final Geocache cache : caches) {
            final String cacheGroupKey = getCacheGroupKey(cache);
            Optional.ofNullable(cacheGroupKey).ifPresent(cgk -> addCacheToGroup(cgk, cache));
        }

        final TableData result = new TableData(getTableId());

        // Create titles:
        final TableRow headerRow = new TableRow(true);
        headerRow.addCell(new CellData(getTableGroupColumnTitle()));
        headerRow.addCell(new CellData("Number of caches"));
        headerRow.addCell(new CellData("Number of cache types"));
        for (final CacheType cacheType : CacheType.values()) {
            headerRow.addCell(new CellData(cacheType.name()));
        }
        result.addRow(headerRow);

        // Create data rows:
        for (final Group group : groups.values()) {
            final TableRow dataRow = new TableRow(false);
            dataRow.addCell(createTableGroupColumnRowContent(group));
            dataRow.addCell(new CellData(String.valueOf(group.getTotalNumberOfCaches())));
            dataRow.addCell(new CellData(String.valueOf(group.getNumberOfCacheTypes())));

            final Map<CacheType, Collection<Geocache>> cacheMap = group.getCaches();

            for (final Entry<CacheType, Collection<Geocache>> entry : cacheMap.entrySet()) {
                dataRow.addCell(new CellData(String.valueOf(entry.getValue().size())));
            }
            result.addRow(dataRow);
        }

        return result;
    }

    private void addCacheToGroup(final String groupName, final Geocache cache) {
        groups.merge(groupName, new Group(groupName), (existing, x) -> existing);
        groups.get(groupName).addCache(cache);
    }

    /**
     * Represents a group of geocaches. Keeps track of the name of the group and
     * the different cache types it has.
     */
    final class Group {

        private final String name;
        private final Map<CacheType, Collection<Geocache>> caches = new LinkedHashMap<>();

        public Group(final String groupName) {
            name = groupName;
            for (CacheType cacheType : CacheType.values()) {
                caches.put(cacheType, new LinkedHashSet<>());
            }
        }

        public void addCache(final Geocache cache) {
            caches.get(cache.getType()).add(cache);
        }

        public int getTotalNumberOfCaches() {
            return caches.values().stream().mapToInt(Collection::size).reduce(0, Integer::sum);
        }

        public int getNumberOfCacheTypes() {
            return (int) caches.values().stream().filter(val -> val.size() > 0).count();
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        public String getName() {
            return name;
        }

        public Map<CacheType, Collection<Geocache>> getCaches() {
            return caches;
        }

        @Override
        public boolean equals(final Object o) {
            if (o != null && !(o instanceof Group)) {
                return false;
            }

            return name != null && name.equals(((Group) o).getName());
        }
    }
}

