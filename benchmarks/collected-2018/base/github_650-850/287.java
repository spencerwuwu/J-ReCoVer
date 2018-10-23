// https://searchcode.com/api/result/16701184/

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.tools4j.config.internal.core.hbase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.deephacks.tools4j.config.internal.core.hbase.HBeanTable.FetchType;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Encapsulate hbase byte manipulations of beans.   
 * 
 * Each unique rowkey follows the following format:
 * - 2 bytes schema short-id, sid 
 * - 4 byte instance short-id, iid
 * 
 * Short ids are given by the UniqueIds class.
 * 
 * Each row contains the following columns:
 * - PROP_COLUMN_FAMILY, bean properties 
 *    Qualifier: PROP_COLUMN_FAMILY.
 *    Value: A kryo serialized byte array consisting of all properties.
 * 
 * - REF_COLUMN_FAMILY, bean references
 *    Qualifier: sid+pid of the bean
 *    Value: a compressed byte array of iids, each 4 bytes long. 
 *           In order to reduce the amount of property data stored, we convert 
 *           the properties into a string matrix. The first element is the 
 *           property name followed by its values. 
 *    
 * - PRED_COLUMN_FAMILY, predecessor refering to this bean
 *    Qualifier: sid of the predecessor
 *    Value: a compressed byte array of iids, each 4 bytes long.
 *    
 * The reason for splitting qualifier and value for references is effeciency,
 * save memory and processing time. 
 * 
 * Having a lot of references should not be a problem unless qualifier 
 * reaches above 10MB, around 10000000 bytes / 4 bytes =~ 2 500 000 references.
 */
public final class HBeanRow {
    /** Hbase table that store beans. */
    public static final byte[] BEAN_TABLE = "bean".getBytes();
    /** number of bytes allocated for schema name and property ids */
    public static final int SID_WIDTH = 2;
    /** table storing short ids for schema */
    public static final byte[] SID_TABLE = "sid".getBytes();
    /** number of bytes allocated for instance id */
    public static final int IID_WIDTH = 4;
    /** table storing short ids for schema */
    public static final byte[] IID_TABLE = "iid".getBytes();
    /** Properties for each bean is serialized into one column familiy. */
    public static final byte[] PROP_COLUMN_FAMILY = "p".getBytes();
    /** References for each bean are stored as compacted iid's, qualified by sid+pid */
    public static final byte[] REF_COLUMN_FAMILY = "r".getBytes();
    /** Predecessor beans are stored as compacted iid's, qualified by sid */
    public static final byte[] PRED_COLUMN_FAMILY = "pr".getBytes();
    /** Dummy column that enables empty rows/beans. */
    public static final byte[] DUMMY_COLUMN_FAMILY = "d".getBytes();
    /** long value representation of the rowkey of a bean */
    private final long id;
    /** lookup name to id and vice verse */
    private final UniqueIds uids;
    /** KeyValue for storing all properties of a bean. */
    private KeyValue properties;
    /** Each KeyValue stores references per property name. */
    private Set<KeyValue> references = new HashSet<KeyValue>();
    /** Each KeyValue stores predecessors per bean type. */
    private Set<KeyValue> predecessors = new HashSet<KeyValue>();

    public HBeanRow(KeyValue[] kvs, UniqueIds uids) {
        this.id = HBeanBytes.getId(kvs.length == 0 ? null : kvs[0].getRow());
        this.uids = uids;
        for (int i = 0; i < kvs.length; i++) {
            if (HBeanBytes.isProperty(kvs[i])) {
                properties = kvs[i];
            } else if (HBeanBytes.isReference(kvs[i])) {
                references.add(kvs[i]);
            } else if (HBeanBytes.isPredecessor(kvs[i])) {
                predecessors.add(kvs[i]);
            }
        }
    }

    /**
     * Construct a empty row from a row key.
     */
    public HBeanRow(byte[] rowkey, UniqueIds uids) {
        this.id = HBeanBytes.getId(rowkey);
        this.uids = uids;
    }

    /**
     * Construct a empty row from a bean id.
     */
    public HBeanRow(BeanId id, UniqueIds uids) {
        byte[] rowkey = HBeanBytes.getRowKey(id, uids);
        this.id = HBeanBytes.getId(rowkey);
        this.uids = uids;
    }

    /**
     * Construct a row with properties and references initalized from the bean. 
     */
    public HBeanRow(Bean bean, UniqueIds uids) {
        byte[] rowkey = HBeanBytes.getRowKey(bean.getId(), uids);
        this.id = HBeanBytes.getId(rowkey);
        this.uids = uids;
        this.references = HBeanBytes.getReferences(rowkey, bean, uids);
        final String[][] properties = HBeanBytes.getProperties(bean);
        this.properties = HBeanBytes.getPropertiesKeyValue(rowkey, properties);
    }

    /**
     * @return long reperesentation of the row key. 
     */
    public long getId() {
        return id;
    }

    /**
     * @return convert the id into a bean id.
     */
    public BeanId getBeanId() {
        return HBeanBytes.getBeanId(getId(), uids);
    }

    /**
     * @return return the row key converted from the id.
     */
    public byte[] getRowKey() {
        return HBeanBytes.getRowKey(id);
    }

    /**
     * @return convert this row's references into a set of empty rows.
     */
    public Set<HBeanRow> getReferenceRows() {
        return HBeanBytes.getReferences(references, uids);
    }

    /**
     * This operation is functionally identical with merging two beans. The only
     * difference is that the bean merges into the row binary form (not the other
     * way around).
     * 
     * @param bean merge this row with the provided bean. 
     */
    public void merge(Bean bean) {
        HBeanBytes.merge(this, bean, uids);
    }

    /**
     * This operation is functionally identical with setting one bean on the another, i.e.
     * replacement. The only difference is that the bean is set into the row binary 
     * form (not the other way around).
     * 
     * @param bean
     */
    public void set(Bean bean) {
        HBeanBytes.set(this, bean, uids);
    }

    /**
     * The value must be deserialized with kryo.
     * 
     * @return return this row's properties in KeyValue form.
     */
    public KeyValue getProperties() {
        return properties;
    }

    /**
     * The key consists of sid+pid and value is a compressed byte array of iids.
     * 
     * @return return this row's references in KeyValue form.
     */
    public Set<KeyValue> getReferences() {
        return references;
    }

    /**
     * The key is sid and value is a compressed byte array of iids.
     * 
     * @return this row's predecessors in KeyValue form.
     */
    public Set<KeyValue> getPredecessors() {
        return predecessors;
    }

    /**
     * @return convert this row into a bean.
     */
    public Bean getBean() {
        return HBeanBytes.getBean(this, uids);
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HBeanRow)) {
            return false;
        }
        HBeanRow other = (HBeanRow) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return getBean().toString();
    }

    /**
     * HBeanRowCollector is responsible for collecting references when traversing 
     * references eagerly and detecting circular references.
     */
    public static final class HBeanRowCollector {
        /** the inital rows that was queried for */
        private final Set<HBeanRow> inital;
        /** collected references  */
        private final Set<HBeanRow> references = new HashSet<HBeanRow>();

        /**
         * We need to keep track of the intial query that was made. 
         */
        public HBeanRowCollector(final Set<HBeanRow> rows) {
            this.inital = rows;
        }

        public void addReferences(Set<HBeanRow> refs) {
            references.addAll(refs);
        }

        /**
         * Filter out the rows that we have not yet visited.
         */
        public Set<HBeanRow> filterUnvisted(Set<HBeanRow> rows) {
            Set<HBeanRow> unvisted = new HashSet<HBeanRow>();
            for (HBeanRow row : rows) {
                if (!references.contains(row)) {
                    unvisted.add(row);
                }
            }
            return unvisted;
        }

        /**
         * Convert the collected rows into a hierarchy of beans where
         * all references are initalized.
         */
        public List<Bean> getBeans() {
            Map<BeanId, Bean> referenceMap = new HashMap<BeanId, Bean>();
            List<Bean> result = new ArrayList<Bean>();

            for (HBeanRow row : inital) {
                Bean bean = row.getBean();
                result.add(bean);
                referenceMap.put(bean.getId(), bean);
            }
            for (HBeanRow row : references) {
                Bean bean = row.getBean();
                referenceMap.put(bean.getId(), bean);
            }
            for (Bean bean : referenceMap.values()) {
                for (BeanId id : bean.getReferences()) {
                    id.setBean(referenceMap.get(id));
                }
            }
            return result;
        }
    }

    /**
     * Holder class for lookups of iid and sid.
     */
    public static class UniqueIds {
        private UniqueId uiid;
        private UniqueId usid;

        public UniqueIds(UniqueId uiid, UniqueId usid) {
            this.uiid = uiid;
            this.usid = usid;
        }

        public UniqueId getUsid() {
            return usid;
        }

        public UniqueId getUiid() {
            return uiid;
        }

    }

    /**
     * Helper class for manging bytes related to how a bean is stored in hbase.
     */
    public static final class HBeanBytes {

        /** Kryo is used for serializing properties */
        private static final Kryo kryo = new Kryo();

        public static HTable getBeanTable(Configuration conf) {
            try {
                return new HTable(conf, BEAN_TABLE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Create the unique lookup tables.
         * @param conf HBase configuration.
         * @return a holder for sid and iid lookup.
         */
        public static UniqueIds createUids(Configuration conf) {
            UniqueId usid = new UniqueId(SID_TABLE, SID_WIDTH, conf, true);
            UniqueId uiid = new UniqueId(IID_TABLE, IID_WIDTH, conf, true);
            return new UniqueIds(uiid, usid);
        }

        /**
         * Convert a long row key id into a bean id. 
         */
        public static BeanId getBeanId(final long id, UniqueIds uids) {
            final byte[] rowkey = getRowKey(id);
            final byte[] sid = new byte[] { rowkey[0], rowkey[1] };
            final byte[] iid = new byte[] { rowkey[2], rowkey[3], rowkey[4], rowkey[5] };
            final String schemaName = uids.getUsid().getName(sid);
            final String instanceId = uids.getUiid().getName(iid);
            return BeanId.create(instanceId, schemaName);
        }

        public static BeanId getBeanId(final byte[] rowkey, UniqueIds uids) {
            return getBeanId(getId(rowkey), uids);
        }

        /**
         * Convert the row key to a long id.
         * 
         * @param rowkey 6 byte row key.
         * @return long representation of the row key.
         */
        public static long getId(final byte[] rowkey) {
            return (rowkey[0] & 0xFFL) << 40 | (rowkey[1] & 0xFFL) << 32
                    | (rowkey[2] & 0xFFL) << 24 | (rowkey[3] & 0xFFL) << 16
                    | (rowkey[4] & 0xFFL) << 8 | (rowkey[5] & 0xFFL) << 0;
        }

        /**
         * The bean rowkey is stored as 6 bytes, but it is represented as a big-endian 
         * 8-byte long. 
         */
        public static byte[] getRowKey(final long id) {
            final byte[] b = new byte[6];
            b[0] = (byte) (id >>> 40);
            b[1] = (byte) (id >>> 32);
            b[2] = (byte) (id >>> 24);
            b[3] = (byte) (id >>> 16);
            b[4] = (byte) (id >>> 8);
            b[5] = (byte) (id >>> 0);
            return b;
        }

        /**
         * Extract sid from schema name. 
         */
        public static byte[] extractSidPrefix(final String schemaName, UniqueIds uids) {
            return uids.getUsid().getId(schemaName);
        }

        /**
         * Get the hbase rowkey of some bean id. 
         */
        public static byte[] getRowKey(final BeanId id, final UniqueIds uids) {
            final byte[] iid = uids.getUiid().getId(id.getInstanceId());
            final byte[] sid = uids.getUsid().getId(id.getSchemaName());
            final byte[] rowkey = new byte[sid.length + iid.length];
            System.arraycopy(sid, 0, rowkey, 0, sid.length);
            System.arraycopy(iid, 0, rowkey, sid.length, iid.length);
            return rowkey;
        }

        /**
         * Set references on a bean using a set of key values containing references.
         */
        public static void setReferences(Bean bean, Set<KeyValue> refs, UniqueIds uids) {
            for (KeyValue ref : refs) {
                final byte[] sidpid = ref.getQualifier();
                final byte[] iids = ref.getValue();
                final byte[] sid = new byte[] { sidpid[0], sidpid[1] };
                final byte[] pid = new byte[] { sidpid[2], sidpid[3] };

                final String schemaName = uids.getUsid().getName(sid);
                final String propertyName = uids.getUsid().getName(pid);

                for (int i = 0; i < iids.length; i += IID_WIDTH) {
                    final byte[] iid = new byte[] { iids[i], iids[i + 1], iids[i + 2], iids[i + 3] };
                    final String instanceId = uids.getUiid().getName(iid);
                    bean.addReference(propertyName, BeanId.create(instanceId, schemaName));
                }
            }
        }

        /**
         * Extract bean references into key value form, ready to be stored in a row. 
         */
        public static Set<KeyValue> getReferences(final byte[] rowkey, final Bean bean,
                final UniqueIds uids) {
            Set<KeyValue> references = new HashSet<KeyValue>();

            for (String propertyName : bean.getReferenceNames()) {
                List<BeanId> refs = bean.getReference(propertyName);
                if (refs == null || refs.size() == 0) {
                    continue;
                }
                KeyValue kv = getReferenceKeyValue(rowkey, propertyName, refs, uids);
                references.add(kv);
            }
            return references;
        }

        /**
         *  Get a particular type of references identified by propertyName into key value
         *  form.
         */
        public static KeyValue getReferenceKeyValue(byte[] rowkey, String propertyName,
                List<BeanId> refs, UniqueIds uids) {
            final byte[] pid = uids.getUsid().getId(propertyName);
            final byte[] sid = uids.getUsid().getId(refs.get(0).getSchemaName());
            final byte[] qual = new byte[] { sid[0], sid[1], pid[0], pid[1] };
            final byte[] iids = getIids2(refs, uids);
            return new KeyValue(rowkey, REF_COLUMN_FAMILY, qual, iids);
        }

        /**
         * Convert a row into a bean.
         * @param row to be converted
         * @param uids lookup
         * @return a bean
         */
        public static Bean getBean(HBeanRow row, UniqueIds uids) {
            final BeanId id = getBeanId(row.getId(), uids);
            final Bean bean = Bean.create(id);
            setProperties(bean, getProperties(row.getProperties()));
            setReferences(bean, row.getReferences(), uids);
            return bean;
        }

        /**
         * Convert a raw set of properties into binary form. 
         */
        public static KeyValue getPropertiesKeyValue(final byte[] rowkey, String[][] properties) {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final Output out = new Output(bytes);
            try {
                kryo.writeObject(out, properties);
            } finally {
                out.close();
                try {
                    bytes.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            return new KeyValue(rowkey, PROP_COLUMN_FAMILY, PROP_COLUMN_FAMILY, bytes.toByteArray());
        }

        /**
         * In order to reduce the amount of property data stored, we convert the
         * properties into a String matrix. The first element is the property name
         * followed by its values. 
         * 
         * @param bean Bean to get properties from.
         * @return String matrix
         */
        public static String[][] getProperties(final Bean bean) {
            final List<String> propertyNames = bean.getPropertyNames();
            final int psize = propertyNames.size();
            final String[][] properties = new String[psize][];
            for (int i = 0; i < psize; i++) {
                final String propertyName = propertyNames.get(i);
                final List<String> values = bean.getValues(propertyName);
                final int vsize = values.size();
                properties[i] = new String[vsize + 1];
                properties[i][0] = propertyName;
                for (int j = 0; j < vsize; j++) {
                    properties[i][j + 1] = values.get(j);
                }
            }
            return properties;
        }

        /**
         * @param props properties in key value form.
         * @return Properties converted into string matrix form. 
         */
        public static String[][] getProperties(final KeyValue props) {
            final Input in = new Input(props.getValue());
            try {
                return kryo.readObject(in, String[][].class);
            } finally {
                in.close();
            }
        }

        /**
         * Set a string matrix of properties on a particular bean.
         * 
         * @param bean Bean to set properties on.
         * @param properties in string matrix form.
         */
        public static void setProperties(final Bean bean, final String[][] properties) {
            for (int i = 0; i < properties.length; i++) {
                if (properties[i].length < 2) {
                    continue;
                }
                for (int j = 0; j < properties[i].length - 1; j++) {
                    bean.addProperty(properties[i][0], properties[i][j + 1]);
                }
            }
        }

        public Multimap<byte[], byte[]> getPredecessors(Map<String, List<String>> predecessors,
                final UniqueIds uids) {
            final Multimap<byte[], byte[]> bytes = ArrayListMultimap.create();

            for (String schemaName : predecessors.keySet()) {
                final byte[] sid = uids.getUsid().getId(schemaName);
                new ArrayList<String>();
                bytes.put(sid, getIids(predecessors.get(schemaName), uids));
            }
            return bytes;
        }

        /**
         * Compress a set of instances ids into a byte array where each id consist of 
         * 4 bytes each. 
         */
        public static byte[] getIids(final List<String> ids, final UniqueIds uids) {
            final int size = ids.size();
            final byte[] iids = new byte[IID_WIDTH * size];
            for (int i = 0; i < size; i++) {
                final String instanceId = ids.get(i);
                final byte[] iid = uids.getUiid().getId(instanceId);
                System.arraycopy(iid, 0, iids, i * IID_WIDTH, IID_WIDTH);
            }
            return iids;
        }

        /**
         * Second version of getIids that takes a set of bean ids instead. 
         */
        public static byte[] getIids2(final List<BeanId> ids, final UniqueIds uids) {
            final AbstractList<String> list = new AbstractList<String>() {

                @Override
                public String get(int index) {
                    return ids.get(index).getInstanceId();
                }

                @Override
                public int size() {
                    return ids.size();
                }
            };
            return getIids(list, uids);
        }

        public static Set<HBeanRow> getReferences(Set<KeyValue> references, UniqueIds uids) {
            final Set<HBeanRow> rows = new HashSet<HBeanRow>();
            for (KeyValue ref : references) {
                byte[] sid = ref.getQualifier();
                byte[] iids = ref.getValue();
                for (int i = 0; i < iids.length; i += IID_WIDTH) {
                    final byte[] rowkey = new byte[] { sid[0], sid[1], iids[i], iids[i + 1],
                            iids[i + 2], iids[i + 3] };
                    rows.add(new HBeanRow(rowkey, uids));
                }
            }
            return rows;
        }

        /**
         * This operation is functionally identical with merging two beans. The only
         * difference is that the bean merges into the row binary form (not the other
         * way around).
         */
        public static void merge(HBeanRow org, Bean bean, UniqueIds uids) {
            final byte[] rowkey = org.getRowKey();

            // merge properties
            HashMap<String, String[]> props = new HashMap<String, String[]>();
            for (String[] p : getProperties(org.getProperties())) {
                props.put(p[0], Arrays.copyOfRange(p, 1, p.length));
            }
            for (String[] p : getProperties(bean)) {
                props.put(p[0], Arrays.copyOfRange(p, 1, p.length));
            }
            String[][] mergedProps = new String[props.size()][];
            int n = 0;
            for (String propertyName : props.keySet()) {
                String[] values = props.get(propertyName);
                String[] merged = new String[values.length + 1];
                merged[0] = propertyName;
                System.arraycopy(values, 0, merged, 1, values.length);
                mergedProps[n++] = merged;
            }
            org.properties = getPropertiesKeyValue(rowkey, mergedProps);
            // merge references
            Set<KeyValue> mergedRefs = new HashSet<KeyValue>();
            for (KeyValue kv : org.references) {
                final String propertyName = getPropertyName(kv, uids);
                final List<BeanId> refs = bean.getReference(propertyName);
                KeyValue mergekv = null;
                if (refs == null) {
                    mergekv = kv.deepCopy();
                } else {
                    mergekv = getReferenceKeyValue(rowkey, propertyName, refs, uids);
                }
                mergedRefs.add(mergekv);
            }
            org.references = mergedRefs;

        }

        /**
         * This operation is functionally identical with setting one bean on the another, i.e.
         * replacement. The only difference is that the bean is set into the row binary 
         * form (not the other way around).
         */
        public static void set(HBeanRow org, Bean bean, UniqueIds uids) {
            final byte[] rowkey = org.getRowKey();
            // overwrite row properties
            String[][] props = getProperties(bean);
            org.properties = getPropertiesKeyValue(rowkey, props);
            // overwrite row references and nullify existing ones
            Set<KeyValue> setRefs = new HashSet<KeyValue>();
            for (KeyValue kv : org.references) {
                final String propertyName = getPropertyName(kv, uids);
                final List<BeanId> refs = bean.getReference(propertyName);
                KeyValue setkv = null;
                if (refs == null) {
                    setkv = new KeyValue(rowkey, kv.getQualifier(), null);
                } else {
                    setkv = getReferenceKeyValue(rowkey, propertyName, refs, uids);
                }
                setRefs.add(setkv);
            }
        }

        /**
         * Extract the property name from a key value.
         */
        private static String getPropertyName(KeyValue kv, UniqueIds uids) {
            final byte[] qualifier = kv.getQualifier();
            final byte[] pid = new byte[] { qualifier[2], qualifier[3] };
            final String propertyName = uids.getUsid().getName(pid);
            return propertyName;
        }

        /**
         * If this key value is of property familiy type. 
         */
        public static boolean isProperty(KeyValue kv) {
            if (Bytes.equals(kv.getFamily(), PROP_COLUMN_FAMILY)) {
                return true;
            }
            return false;
        }

        /**
         * If this key value is of reference familiy type. 
         */
        public static boolean isReference(KeyValue kv) {
            if (Bytes.equals(kv.getFamily(), REF_COLUMN_FAMILY)) {
                return true;
            }
            return false;
        }

        /**
         * If this key value is of predecessor familiy type. 
         */
        public static boolean isPredecessor(KeyValue kv) {
            if (Bytes.equals(kv.getFamily(), PRED_COLUMN_FAMILY)) {
                return true;
            }
            return false;
        }

        /**
         * Add column filter based fetch type on operation Get or Scan. 
         */
        public static void setColumns(Object op, FetchType... column) {
            ArrayList<byte[]> columns = new ArrayList<byte[]>();
            columns.add(DUMMY_COLUMN_FAMILY);
            if (column.length == 0) {
                // default behaviour
                columns.add(PROP_COLUMN_FAMILY);
                columns.add(REF_COLUMN_FAMILY);
            } else if (FetchType.KEY_ONLY == column[0]) {
                // only IID_FAMILY
                final FilterList list = new FilterList();
                list.addFilter(new FirstKeyOnlyFilter());
                if (op instanceof Scan) {
                    ((Scan) op).setFilter(list);
                } else if (op instanceof Get) {
                    ((Get) op).setFilter(list);
                }
            }
            for (byte[] familiy : columns) {
                if (op instanceof Scan) {
                    ((Scan) op).addFamily(familiy);
                } else if (op instanceof Get) {
                    ((Get) op).addFamily(familiy);
                }
            }
        }
    }

}
