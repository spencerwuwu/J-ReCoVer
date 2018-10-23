// https://searchcode.com/api/result/136332425/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.goldengekko.gae.jds.service;

import com.goldengekko.gae.jds.client.JdsClient;
import com.goldengekko.gae.jds.security.JdsUserService;
import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.*;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author os
 */
public class JdsService {
    public static final String KIND_DBS = "_DBs";
    public static final String KIND_TYPES = "_Types";
    public static final String KIND_USERS = JdsClient.KIND_USERS;
    public static final String NAMESPACE_ADMIN = JdsClient.NAMESPACE_ADMIN;
    public static final String NAMESPACE_UBUD_TEST = JdsClient.NAMESPACE_UBUD_TEST;
    public static final String PROPERTY_NAMESPACE = "namespace";
    public static final String PROPERTY_ID = "_id";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_ROLES = "roles";
//    public static final String PROPERTY_SIBLINGS = "_sib";
    public static final String PROPERTY_SINGLE = "_single";
    public static final String PROPERTY_USERNAME = "username";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    
    public static final char DELIMITER_KIND = '.';
    
    static final Logger LOG = LoggerFactory.getLogger(JdsService.class);
    
    protected Object cast(String kind, String name, Object value) {
        if (null == value) {
            return null;
        }
        final String toType = getCastType(kind, name);
//            LOG.debug("cast {} to {}", name, toType);
        if (null != toType) {
            if (Key.class.getName().equals(toType)) {
                return KeyFactory.stringToKey((String)value);
            }
        }
        
        // do nothing, as we do not know of what type the property is
        return value;
    }
    
    protected Object castBack(String kind, String name, Object value) {
        if (null == value) {
            return null;
        }
        final String toType = getCastType(kind, name);
//            LOG.debug("castBack {} to {}", name, toType);
        if (null != toType) {
            if (Key.class.getName().equals(toType)) {
                return KeyFactory.keyToString((Key)value);
            }
        }
        
        // do nothing, as we do not know of what type the property is
        return value;
    }
    
    protected Map<String,Object> castBack(Entity entity, boolean populateID) {
        // entity properties immutable
        final Map<String, Object> props = new TreeMap<String, Object>();
        final String kind = entity.getKind();
        
        for (Entry<String,Object> entry : entity.getProperties().entrySet()) {
            props.put(entry.getKey(), castBack(kind, entry.getKey(), entry.getValue()));
        }

        final Key key = entity.getKey();
        if (populateID) {
            props.put(PROPERTY_ID, createId(key));
        }
        
        return props;
    }
    
    public boolean containsDocument(String kind, Object id) {
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        final Map<String, Object> q = new TreeMap<String, Object>();
        final Key key = createKey(null, kind, id);
        q.put(Entity.KEY_RESERVED_PROPERTY, key);
        final PreparedQuery pq = prepare(datastore, kind, null, true, q, null, false, null);
        final Entity doc = pq.asSingleEntity();
        return null != doc;
    }
    
    public Object createDocument(
            String kind,
            Map<String, Object> props) {
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction t = datastore.beginTransaction();
        try {
            final Key key = persist(datastore, t, null, kind, null, props, false);
            return createId(key);
        } finally {
            t.commit();
        }

    }
    
    protected static Key createKey(Key parentKey, String kind, Object id) {
        Key key;
        if (id instanceof String) {
            try {
                final Long lId = Long.parseLong((String) id);
                key = KeyFactory.createKey(parentKey, kind, lId);
            }
            catch (NumberFormatException sometimes) {
                key = KeyFactory.createKey(parentKey, kind, (String) id);
            }
        } else {
            key = KeyFactory.createKey(parentKey, kind, (Long)id);
        }
        return key;
    }
    
    protected static Object createId(Key key) {
        if (null == key) {
            return null;
        }
        final String name = key.getName();
        return null != name ? name : key.getId();
    }
    
    public Map<String,Object> createAdmin(String namespace, String username, String password) {
        return createUser(namespace, username, password, ROLE_USER, ROLE_ADMIN);
    }
    
    protected Map<String,Object> createUser(String namespace, String username, String password, String... roles) {
        final TreeMap<String, Object> user = new TreeMap<String, Object>();
        user.put(PROPERTY_USERNAME, username);
        user.put(PROPERTY_PASSWORD, password);
        user.put(PROPERTY_NAMESPACE, namespace);
        user.put(PROPERTY_ROLES, Arrays.asList(roles));
        createDocument(KIND_USERS, user);
        return user;
    }
    
    public Map<String,Object> createUser(String namespace, String username, String password) {
        return createUser(namespace, username, password, ROLE_USER);
    }
    
    protected static int deleteAncestors(DatastoreService datastore, Transaction t, Key ancestorKey, boolean excludeAnestor) {
        // delete all child entities
        final Query q = new Query();
        q.setAncestor(ancestorKey);
        q.setKeysOnly();
        // exclude the ancestor / document
        if (excludeAnestor) {
            q.addFilter(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.GREATER_THAN, ancestorKey);
        }

        final PreparedQuery pq = datastore.prepare(t, q);
        final Iterable<Entity> ancestors = pq.asQueryResultIterable();
        final List<Key> keys = getKeys(ancestors);
        datastore.delete(t, keys);
        return keys.size();
    }
    
    public boolean deleteDocument(String kind, Object id) {
        final Key key = createKey(null, kind, id);
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction t = datastore.beginTransaction();
        try {
            // do not exclude ancestor / document
            int ancestors = deleteAncestors(datastore, t, key, false);
            return 0 < ancestors;
        } finally {
            t.commit();
        }
    }
    
    /**
     * Retrieves the type a property should be persisted as.
     * Implement caching here!
     * @param kind
     * @param name
     * @return the class.getName() corresponding value
     */
    protected String getCastType(String kind, String name) {
        final Map<String,Object> propertyTypeMap = getDocument(KIND_TYPES, kind);
        if (null == propertyTypeMap) {
            return null;
        }
        return (String) propertyTypeMap.get(name);
    }

    public Map<String, Object> getDocument(
            String kind,
            Object id) {

        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Map<String, Object> props = load(datastore, kind, id);

        LOG.debug("getDocument({}/{}) -> {}", new Object[] {kind, id, props});
        return props;
    }

    public List<Map<String,Object>> getDocuments(String kind
            ) {
        
            final List<Map<String,Object>> props = load(kind);

            return props;
    }
    
    public void initialize() {
        // preserve current namespace (should be database name):
        final String currentNamespace = NamespaceManager.get();
        
        try {
            // namespace should be json-doc-store when loading users:
            NamespaceManager.set(JdsService.NAMESPACE_ADMIN);

            // create admin namespace
            final Map<String,Object> jsonDocStore = new TreeMap<String, Object>();
            jsonDocStore.put(PROPERTY_ID, NAMESPACE_ADMIN);
            createDocument(KIND_DBS, jsonDocStore);
            
            // create jds-test namespace
            final Map<String,Object> ubudTest = new TreeMap<String, Object>();
            ubudTest.put(PROPERTY_ID, NAMESPACE_UBUD_TEST);
            createDocument(KIND_DBS, ubudTest);
            
            // create admin for admin namespace
            Map<String,Object> admin = queryDocument(JdsService.KIND_USERS, 
                    JdsUserService.buildQueryByUsername("admin", NAMESPACE_ADMIN));
            if (null == admin) {
                admin = createAdmin(NAMESPACE_ADMIN, "admin", "topsecret");
            }
            
            // create user for ubud-test namespace
            Map<String,Object> ubudUser = queryDocument(JdsService.KIND_USERS, 
                    JdsUserService.buildQueryByUsername("admin", NAMESPACE_UBUD_TEST));
            if (null == ubudUser) {
                ubudUser = createUser(NAMESPACE_UBUD_TEST, "admin", "topsecret");
            }
            
            // create castBack for ubud-test / Person / employee to Key
            NamespaceManager.set(NAMESPACE_UBUD_TEST);
            final Map<String,Object> personCastMap = new HashMap<String,Object>();
            personCastMap.put(PROPERTY_ID, "Person");
            personCastMap.put("employee", Key.class.getName());
            createDocument(KIND_TYPES, personCastMap);
        }
        finally {
            // restore initial namespace:
            NamespaceManager.set(currentNamespace);
        }
    }
    
    /**
     * Extracts the direct children from the ancestors
     * @param ancestors list of ancestor entities
     * @param parentKey key of parent to children, can be null
     * @return the direct children to parent
     */
    protected static List<Entity> extractChildren(List<Entity> ancestors, Key parentKey) {
        final List<Entity> children = new ArrayList<Entity>();
        for (Entity e : ancestors) {
            if ((null == parentKey && null == e.getParent()) || (null != parentKey && parentKey.equals(e.getParent()))) {
                children.add(e);
            }
        }
        ancestors.removeAll(children);
        return children;
    }
    
    public static final List<Key> getKeys(Iterable<Entity> entities) {
        final ArrayList<Key> keys  = new ArrayList<Key>();
        for (Entity e : entities) {
            keys.add(e.getKey());
        }
        return keys;
    }

    /**
     * Loads an entire document, specified by its key
     * @param datastore
     * @param t
     * @param documentKey
     * @return a Map containing the document's properties
     */
    protected Map<String, Object> load(DatastoreService datastore, Key documentKey) {
        final Query q = new Query();
        
        q.setAncestor(documentKey);
        
        final PreparedQuery pq = datastore.prepare(q);
        final List<Entity> ancestors = pq.asList(FetchOptions.Builder.withDefaults());
        
        // find the document / ancestor
        final List<Entity> document = extractChildren(ancestors, null);
        
        if (document.isEmpty()) {
            return null;
        }
        
        final Map<String, Object> props = populate(document.get(0), ancestors, true);
        
        return props;
    }

    /**
     * Loads an entire document, specified by kind and id
     * @param datastore
     * @param t
     * @param kind
     * @param id
     * @return a Map containing the document's properties
     */
    protected Map<String, Object> load(DatastoreService datastore, String kind, Object id) {
        final Key ancestorKey = createKey(null, kind, id);
        return load(datastore, ancestorKey);
    }

    /**
     * Loads all documents, specified by kind
     * @param datastore
     * @param t
     * @param kind
     * @return a List of Map'd documents
     */
    protected List<Map<String, Object>> load(String kind) {
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        // find root docs keys only
        final Query q = new Query(kind, null);
        q.setKeysOnly();
        // Only ancestor queries are allowed inside transactions
        final PreparedQuery pq = datastore.prepare(q);
        
        final List<Entity> docs = pq.asList(FetchOptions.Builder.withDefaults());

        final List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>(docs.size());
        Map<String,Object> doc;
        for (Entity d : docs) {
            doc = load(datastore, d.getKey());
            if (null != doc) {
                returnValue.add(doc);
            }
        }
        
        return returnValue;
    }

    protected Map<String, Object> populate(Entity entity, List<Entity> ancestors, boolean populateID) {
        final Map<String, Object> props = castBack(entity, populateID);
        
        // add children
        final List<Entity> children = extractChildren(ancestors, entity.getKey());
        for (Entity c : children) {

            // do not include ID for child sections
            Map<String, Object> child = populate(c, ancestors, false);
            
            // this child an array item?
            if (Boolean.TRUE.equals(child.remove(PROPERTY_SINGLE))) {
                props.put(c.getKind(), child);
            }
            else {
                List<Map<String,Object>> siblings = (List<Map<String,Object>>) props.get(c.getKind());
                if (null == siblings) {
                    siblings = new ArrayList<Map<String,Object>>();
                    props.put(c.getKind(), siblings);
                }
                siblings.add(child);
            }
        }

        return props;
    }

    protected Key persist(DatastoreService datastore, Transaction t, Key parentKey, String kind, Object id, Map<String, Object> props, boolean overwrite) {
        Key key = null;
        Entity entity = null;

        // ID included in JSON object?
        if (null == id) {
            id = props.get(PROPERTY_ID);
        }

//        LOG.debug("persisting {}/{} ...", kind, id);
        if (null != id) {
            key = createKey(parentKey, kind, id);
            
            if (containsDocument(kind, id)) {
                LOG.debug("Entity already exists for {}, overwrite is {}", key, overwrite);

                // if found, should we overwrite?
                if (overwrite) {
                    // exclude ancestor / document
                    deleteAncestors(datastore, t, key, true);
                }
                else {
                    LOG.info("Entity already exists for {}/{}", kind, id);
                    return key;
                }
            } 

            entity = new Entity(key);
        } else {
            entity = new Entity(kind, parentKey);
        }

        final Map<String, Map<String, Object>> singles = new TreeMap<String, Map<String, Object>>();
        final Map<String, List<Map<String,Object>>> arrays = new TreeMap<String, List<Map<String,Object>>>();

        Object value;
        String name;
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            name = entry.getKey();
            if (!PROPERTY_ID.equals(name)) {
                value = entry.getValue();

                // inner Entity (non-array)
                if (value instanceof Map) {
                    singles.put(name, (Map<String, Object>) value);
                } // inner array of Entities
                else if (value instanceof List) {
                    List list = (List) value;
                    if (!list.isEmpty()) {
                        Object item = list.get(0);
                        if (item instanceof Map) {
                            arrays.put(name, list);
                        } else {
                            entity.setProperty(name, list);
                        }
                    }
                }
                else {
                    // castBack to Datastore-supported type?
                    value = cast(kind, name, value);
                    entity.setProperty(name, value);
                }
            }
        }

        // parent key has to be real before processing inner childs
        key = datastore.put(t, entity);
        LOG.debug("persisted {} -> {}", key, entity);

        for (Map.Entry<String, Map<String, Object>> entry : singles.entrySet()) {
            // singles must have the SINGLE flag set
            entry.getValue().put(PROPERTY_SINGLE, Boolean.TRUE);
            
            // generate ID for children
            persist(datastore, t, key, entry.getKey(), null, entry.getValue(), false);
        }

        for (Map.Entry<String, List<Map<String,Object>>> entry : arrays.entrySet()) {
            for (Map<String,Object> item : entry.getValue()) {
                    
                // generate ID for children
                persist(datastore, t, key, entry.getKey(), null, item, false);
            }
        }

        return key;
    }
    
    /**
     * Prepares a Query
     * @param datastore
     * @param kind
     * @param ancestorKey
     * @param keysOnly
     * @param q
     * @param orderBy property name to order by, use Entity.KEY_RESERVED_PROPERTY to order by id/name
     * @param ascending
     * @return 
     */
    protected PreparedQuery prepare(DatastoreService datastore, String kind, Key ancestorKey, 
            boolean keysOnly, Map<String,Object> q, String orderBy, boolean ascending,
            Map<String,Object> excludedChildFilters) {
        
        LOG.debug("prepare kind={} q={}", kind, q);
        
        // find root docs keys only
        final Query query = new Query(kind, ancestorKey);
        
        // keys only?
        if (keysOnly) {
            query.setKeysOnly();
        }

        // filter query:
        if (null != q) {
            for (Entry<String,Object> entry : q.entrySet()) {
                
                // only filter on properties, exclude child filters
                if (entry.getKey().contains("" + DELIMITER_KIND)) {
                    if (null != excludedChildFilters) {
                        excludedChildFilters.put(entry.getKey(), entry.getValue());
                    }
                }
                else {
                    Object value = cast(kind, entry.getKey(), entry.getValue());
                    query.addFilter(entry.getKey(), Query.FilterOperator.EQUAL, value);
                }
            }
        }

        // sort query?
        if (null != orderBy) {
            query.addSort(orderBy, ascending ? Query.SortDirection.ASCENDING : Query.SortDirection.DESCENDING);
        }
        
        // Only ancestor queries are allowed inside transactions
        final PreparedQuery pq = datastore.prepare(query);
        
        return pq;
    }
    
    /**
     * Queries multiple documents, specified by kind and q
     * @param datastore
     * @param t
     * @param kind
     * @param q query object
     * @return a List of Map'd documents
     */
    protected List<Map<String, Object>> query(DatastoreService datastore, String kind, Map<String,Object> q) {
        
        final Collection<Key> keys = queryForKeys(datastore, kind, q);

        final List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>(keys.size());
        Map<String,Object> doc;
        for (Key k : keys) {
            doc = load(datastore, k);
            if (null != doc) {
                returnValue.add(doc);
            }
        }
        
        LOG.debug("query -> [{}]", returnValue.size());

        return returnValue;
    }
    
    /**
     * First, query the root kind on properties; next, query children with most filters and reduce root keys.
     * @param datastore
     * @param t
     * @param kind
     * @param q
     * @return 
     */
    protected Collection<Key> queryForKeys(DatastoreService datastore, String kind, Map<String,Object> q) {
        final Map<String,Object> excludedChildFilters = new TreeMap<String,Object>();
        final PreparedQuery pq = prepare(datastore, kind, null, true, q, Entity.KEY_RESERVED_PROPERTY, true, excludedChildFilters);
        Collection<Entity> docs = pq.asList(FetchOptions.Builder.withDefaults());
        // query results are immutable?
//      List<Entity> docs = new ArrayList<Entity>(pq.asList(FetchOptions.Builder.withDefaults()));

        // map the docs
        final Map<Key,Entity> docMap = new TreeMap<Key,Entity>();
        for (Entity e : docs) {
            docMap.put(e.getKey(), e);
        }
        
        if (!docMap.isEmpty() && !excludedChildFilters.isEmpty()) {
            
            // child filtering
            String key, canonicalKind, childKind, name;
            // maps from canonical kind to filters
            final Map<String,Map<String,Object>> childFilters = new TreeMap<String,Map<String,Object>>();
            Map<String,Object> filters;
            for (Entry<String,Object> filter : excludedChildFilters.entrySet()) {
                key = filter.getKey();
                int endIndex = key.lastIndexOf(DELIMITER_KIND);
                if (0 < endIndex) {

                    // canonical kind
                    canonicalKind = key.substring(0, endIndex);
                    filters = childFilters.get(canonicalKind);
                    if (null == filters) {
                        filters = new TreeMap<String,Object>();
                        childFilters.put(canonicalKind, filters);
                    }

                    // last is the property
                    name = key.substring(endIndex+1);
                    filters.put(name, filter.getValue());
                }
            }

            // sort on map size first, then canonical kinds
            TreeSet<String> sortedKinds = new TreeSet<String>(new Comparator<String>() {
                public int compare(String s1, String s2) {
                    final Map m1 = childFilters.get(s1), m2 = childFilters.get(s2);
                    return m1.size() < m2.size() ? -1 : (m2.size() < m1.size() ? 1 : s1.compareTo(s2));
                }
            });
            sortedKinds.addAll(childFilters.keySet());

            for (String ck : sortedKinds) {
                childKind = ck;
                
                // strip canonical parents
                int endIndex = ck.lastIndexOf(DELIMITER_KIND);
                if (-1 < endIndex) {
                    childKind = ck.substring(endIndex+1);
                }
                filters = childFilters.get(ck);
                LOG.debug("child filtering on {}, filters={}", childKind, filters);
                
                final PreparedQuery cq = prepare(datastore, childKind, null, true, filters, null, false, null);
                final List<Key> matchingParents = new ArrayList<Key>();
                Key parentKey;
                for (Entity matchingChild : cq.asQueryResultIterable()) {
                    // get the root key
                    parentKey = matchingChild.getParent();
                    while (null != parentKey.getParent() && !docMap.containsKey(parentKey)) {
                        parentKey = parentKey.getParent();
                    }
                    
                    if (docMap.containsKey(parentKey)) {
                        matchingParents.add(parentKey);
                    }
                }
                
                // remove matchingParents-docs 
                // intersection of docs and matchingParents
                Collection<Key> intersection = CollectionUtils.intersection(docMap.keySet(), matchingParents);
                LOG.debug("intersection {}", intersection);
                Collection<Key> remove = CollectionUtils.subtract(docMap.keySet(), intersection);
                LOG.debug("remove {}", remove);
                for (Key k : remove) {
                    docMap.remove(k);
                }
                LOG.debug("docs {}", docMap.keySet());
                if (docMap.isEmpty()) {
                    break;
                }
            }
        }
        
        return docMap.keySet();
    }

    /**
     * Queries multiple documents, specified by kind and q
     * @param datastore
     * @param t
     * @param kind
     * @param q query object
     * @return a List of Map'd documents
     */
    protected Map<String, Object> querySingleEntity(DatastoreService datastore, String kind, Map<String,Object> q) {

        Collection<Key> docKeys = queryForKeys(datastore, kind, q);
        if (docKeys.isEmpty()) {
            return null;
        }
        if (1 < docKeys.size()) {
            throw new PreparedQuery.TooManyResultsException();
        }

        Key key = null;
        for (Key k : docKeys) {
            key = k;
        }
        final Map<String, Object> returnValue = load(datastore, key);
            
        LOG.debug("querySingleEntity -> {}", returnValue);

        return returnValue;
    }

    public Map<String, Object> queryDocument(
            String kind,
            Map<String, Object> q) {

        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Map<String, Object> props = querySingleEntity(datastore, kind, q);

        return props;
    }

    public List<Map<String, Object>> queryDocuments(
            String kind,
            Map<String, Object> q) {

        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        final List<Map<String, Object>> docs = query(datastore, kind, q);

        return docs;
    }

    public void updateDocument(String kind, Object id, Map<String, Object> props) {
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction t = datastore.beginTransaction();
        try {
            persist(datastore, t, null, kind, id, props, true);
        } finally {
            t.commit();
        }
    }

}

