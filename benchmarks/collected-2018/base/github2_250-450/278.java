// https://searchcode.com/api/result/70797843/

/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.core.search.service.solr;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.locale.domain.Locale;
import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.core.catalog.dao.ProductDao;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.dynamic.DynamicSkuPricingService;
import org.broadleafcommerce.core.catalog.service.dynamic.SkuPricingConsiderationContext;
import org.broadleafcommerce.core.search.dao.FieldDao;
import org.broadleafcommerce.core.search.domain.Field;
import org.broadleafcommerce.core.search.domain.solr.FieldType;
import org.broadleafcommerce.core.util.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Responsible for building and rebuilding the Solr index
 * 
 * @author Andre Azzolini (apazzolini)
 */
@Service("blSolrIndexService")
public class SolrIndexServiceImpl implements SolrIndexService {
    private static final Log LOG = LogFactory.getLog(SolrIndexServiceImpl.class);

    @Value("${solr.index.product.pageSize}")
    protected int pageSize;

    @Resource(name = "blProductDao")
    protected ProductDao productDao;

    @Resource(name = "blFieldDao")
    protected FieldDao fieldDao;

    @Resource(name = "blLocaleService")
    protected LocaleService localeService;

    @Resource(name = "blSolrHelperService")
    protected SolrHelperService shs;

    @Resource(name = "blSolrSearchServiceExtensionManager")
    protected SolrSearchServiceExtensionListener extensionManager;

    @Override
    @SuppressWarnings("rawtypes")
    @Transactional("blTransactionManager")
    public void rebuildIndex() throws ServiceException, IOException {
        LOG.info("Rebuilding the solr index...");
        StopWatch s = new StopWatch();

        // If we are in single core mode, we have to delete the documents before reindexing
        if (SolrContext.isSingleCoreMode()) {
            deleteAllDocuments();
        }

        // Populate the reindex core with the necessary information
        BroadleafRequestContext savedContext = BroadleafRequestContext.getBroadleafRequestContext();
        HashMap savedPricing = SkuPricingConsiderationContext.getSkuPricingConsiderationContext();
        DynamicSkuPricingService savedPricingService = SkuPricingConsiderationContext.getSkuPricingService();
        try {
            Long numProducts = productDao.readCountAllActiveProducts(SystemTime.asDate());
            LOG.debug("There are " + numProducts + " total products");
            int page = 0;
            while ((page * pageSize) < numProducts) {
                buildIncrementalIndex(page, pageSize);
                page++;
            }
        } catch (ServiceException e) {
            throw e;
        } finally {
            // Restore the current context, regardless of whether an exception happened or not
            BroadleafRequestContext.setBroadleafRequestContext(savedContext);
            SkuPricingConsiderationContext.setSkuPricingConsiderationContext(savedPricing);
            SkuPricingConsiderationContext.setSkuPricingService(savedPricingService);
        }

        // Swap the active and the reindex cores
        shs.swapActiveCores();

        // If we are not in single core mode, we delete the documents for the unused core after swapping
        if (!SolrContext.isSingleCoreMode()) {
            deleteAllDocuments();
        }

        LOG.info(String.format("Finished building index in %s", s.toLapString()));
    }

    protected void deleteAllDocuments() throws ServiceException {
        try {
            String deleteQuery = shs.getNamespaceFieldName() + ":" + shs.getCurrentNamespace();
            LOG.debug("Deleting by query: " + deleteQuery);
            SolrContext.getReindexServer().deleteByQuery(deleteQuery);
            SolrContext.getReindexServer().commit();
        } catch (Exception e) {
            throw new ServiceException("Could not delete documents", e);
        }
    }

    protected void buildIncrementalIndex(int page, int pageSize) throws ServiceException {
        LOG.trace(String.format("Building index - page: [%s], pageSize: [%s]", page, pageSize));
        StopWatch s = new StopWatch();
        try {
            List<Product> products = readAllActiveProducts(page, pageSize);
            List<Field> fields = fieldDao.readAllProductFields();

            List<Locale> locales = getAllLocales();

            Collection<SolrInputDocument> documents = new ArrayList<SolrInputDocument>();
            for (Product product : products) {
                documents.add(buildDocument(product, fields, locales));
            }

            if (LOG.isTraceEnabled()) {
                for (SolrInputDocument document : documents) {
                    LOG.trace(document);
                }
            }

            if (documents != null && documents.size() > 0) {
                SolrContext.getReindexServer().add(documents);
                SolrContext.getReindexServer().commit();
            }
        } catch (SolrServerException e) {
            throw new ServiceException("Could not rebuild index", e);
        } catch (IOException e) {
            throw new ServiceException("Could not rebuild index", e);
        }

        LOG.trace(String.format("Built index - page: [%s], pageSize: [%s] in [%s]", page, pageSize, s.toLapString()));
    }

    /**
     * This method to read all active products will be slow if you have a large catalog. In this case, you will want to
     * read the products in a different manner. For example, if you know the fields that will be indexed, you can configure
     * a DAO object to only load those fields. You could also use a JDBC based DAO for even faster access. This default
     * implementation is only suitable for small catalogs.
     * 
     * @return the list of all active products to be used by the index building task
     */
    protected List<Product> readAllActiveProducts() {
        return productDao.readAllActiveProducts(SystemTime.asDate());
    }

    /**
     * This method to read active products utilizes paging to improve performance over {@link #readAllActiveProducts()}.
     * While not optimal, this will reduce the memory required to load large catalogs.
     * 
     * It could still be improved for specific implementations by only loading fields that will be indexed or by accessing
     * the database via direct JDBC (instead of Hibernate).
     * 
     * @return the list of all active products to be used by the index building task
     * @since 2.2.0
     */
    protected List<Product> readAllActiveProducts(int page, int pageSize) {
        return productDao.readAllActiveProducts(page, pageSize, SystemTime.asDate());
    }

    /**
     * @return a list of all possible locale prefixes to consider
     */
    protected List<Locale> getAllLocales() {
        return localeService.findAllLocales();
    }

    /**
     * Given a product, fields that relate to that product, and a list of locales and pricelists, builds a 
     * SolrInputDocument to be added to the Solr index.
     * 
     * @param product
     * @param fields
     * @param locales
     * @return the document
     */
    protected SolrInputDocument buildDocument(Product product, List<Field> fields, List<Locale> locales) {
        SolrInputDocument document = new SolrInputDocument();

        attachBasicDocumentFields(product, document);

        // Add data-driven user specified searchable fields
        List<String> addedProperties = new ArrayList<String>();
        Map<String, List<String>> copyFieldValues = new HashMap<String, List<String>>();

        for (Field field : fields) {
            try {
                // Index the searchable fields
                if (field.getSearchable()) {
                    for (FieldType sft : field.getSearchableFieldTypes()) {
                        Map<String, Object> propertyValues = getPropertyValues(product, field, sft, locales);

                        // Build out the field for every prefix
                        for (Entry<String, Object> entry : propertyValues.entrySet()) {
                            String prefix = entry.getKey();
                            prefix = StringUtils.isBlank(prefix) ? prefix : prefix + "_";

                            String solrPropertyName = shs.getPropertyNameForFieldSearchable(field, sft, prefix);
                            Object value = entry.getValue();

                            // Add the field to Solr to search directly against it
                            if (sft.equals(FieldType.PRICE) || field.getTranslatable() ||
                                    prefix.equals(shs.getDefaultLocalePrefix())) {
                                document.addField(solrPropertyName, value);
                                addedProperties.add(solrPropertyName);
                            }

                            // Add this field to the copyField so that we can search against its content generally
                            List<String> copyFieldValue = copyFieldValues.get(prefix);
                            if (copyFieldValue == null) {
                                copyFieldValue = new ArrayList<String>();
                                copyFieldValues.put(prefix, copyFieldValue);
                            }
                            copyFieldValue.add(value.toString());
                        }
                    }
                }

                // Index the faceted field type as well
                FieldType facetType = field.getFacetFieldType();
                if (facetType != null) {
                    Map<String, Object> propertyValues = getPropertyValues(product, field, facetType, locales);

                    // Build out the field for every prefix
                    for (Entry<String, Object> entry : propertyValues.entrySet()) {
                        String prefix = entry.getKey();
                        prefix = StringUtils.isBlank(prefix) ? prefix : prefix + "_";

                        String solrFacetPropertyName = shs.getPropertyNameForFieldFacet(field, prefix);
                        Object value = entry.getValue();

                        if (facetType.equals(FieldType.PRICE) || field.getTranslatable() ||
                                prefix.equals(shs.getDefaultLocalePrefix())) {
                            if (!addedProperties.contains(solrFacetPropertyName)) {
                                document.addField(solrFacetPropertyName, value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.trace("Could not get value for property[" + field.getQualifiedFieldName() + "] for product id["
                        + product.getId() + "]");
            }
        }

        for (Entry<String, List<String>> entry : copyFieldValues.entrySet()) {
            document.addField(shs.getSearchableFieldName(entry.getKey()), StringUtils.join(entry.getValue(), " "));
        }

        return document;
    }

    /**
     * Adds the ID, category, and explicitCategory fields for the product to the document
     * 
     * @param product
     * @param document
     */
    protected void attachBasicDocumentFields(Product product, SolrInputDocument document) {
        // Add the namespace and ID fields for this product
        document.addField(shs.getNamespaceFieldName(), shs.getCurrentNamespace());
        document.addField(shs.getIdFieldName(), product.getId());

        // The explicit categories are the ones defined by the product itself
        for (Category category : product.getAllParentCategories()) {
            document.addField(shs.getExplicitCategoryFieldName(), category.getId());

            String categorySortFieldName = shs.getCategorySortFieldName(category);
            int listIndex = category.getAllProducts().indexOf(product);
            document.addField(categorySortFieldName, listIndex);
        }

        // This is the entire tree of every category defined on the product
        Set<Category> fullCategoryHierarchy = new HashSet<Category>();
        for (Category category : product.getAllParentCategories()) {
            fullCategoryHierarchy.addAll(category.buildFullCategoryHierarchy(null));
        }
        for (Category category : fullCategoryHierarchy) {
            document.addField(shs.getCategoryFieldName(), category.getId());
        }
    }

    /**
     * Returns a map of prefix to value for the requested attributes. For example, if the requested field corresponds to
     * a Sku's description and the locales list has the en_US locale and the es_ES locale, the resulting map could be
     * 
     * { "en_US" : "A description",
     *   "es_ES" : "Una descripcion" }
     * 
     * @param product
     * @param field
     * @param isPriceField
     * @param prefix
     * @return the value of the property
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    protected Map<String, Object> getPropertyValues(Product product, Field field, FieldType fieldType,
            List<Locale> locales) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String propertyName = field.getPropertyName();
        if (propertyName.contains("productAttributes.")) {
            propertyName = convertToMappedProperty(propertyName, "productAttributes", "mappedProductAttributes");
        }

        Map<String, Object> values = new HashMap<String, Object>();

        if (fieldType.equals(FieldType.PRICE)) {
            Object propertyValue = PropertyUtils.getProperty(product, propertyName);
            values.put("", propertyValue);

            if (extensionManager != null) {
                extensionManager.addPriceFieldPropertyValues(product, field, values, propertyName);
            }
        } else {
            for (Locale locale : locales) {
                String localeCode = locale.getLocaleCode();

                // If the field isn't translatable, we want to use the default locale's property
                if (!field.getTranslatable() && !locale.getDefaultFlag()) {
                    locale = shs.getDefaultLocale();
                }

                // To fetch the appropriate translated property, we need to set the current request context's locale
                if (field.getTranslatable()) {
                    BroadleafRequestContext tempContext = new BroadleafRequestContext();
                    tempContext.setLocale(locale);
                    BroadleafRequestContext.setBroadleafRequestContext(tempContext);
                }

                Object propertyValue = PropertyUtils.getProperty(product, propertyName);
                values.put(localeCode, propertyValue);
            }
        }

        return values;
    }

    /**
     * Converts a propertyName to one that is able to reference inside a map. For example, consider the property
     * in Product that references a List<ProductAttribute>, "productAttributes". Also consider the utility method
     * in Product called "mappedProductAttributes", which returns a map of the ProductAttributes keyed by the name
     * property in the ProductAttribute. Given the parameters "productAttributes.heatRange", "productAttributes", 
     * "mappedProductAttributes" (which would represent a property called "productAttributes.heatRange" that 
     * references a specific ProductAttribute inside of a product whose "name" property is equal to "heatRange", 
     * this method will convert this property to mappedProductAttributes(heatRange).value, which is then usable 
     * by the standard beanutils PropertyUtils class to get the value.
     * 
     * @param propertyName
     * @param listPropertyName
     * @param mapPropertyName
     * @return the converted property name
     */
    protected String convertToMappedProperty(String propertyName, String listPropertyName, String mapPropertyName) {
        String[] splitName = StringUtils.split(propertyName, ".");
        StringBuilder convertedProperty = new StringBuilder();
        for (int i = 0; i < splitName.length; i++) {
            if (convertedProperty.length() > 0) {
                convertedProperty.append(".");
            }

            if (splitName[i].equals(listPropertyName)) {
                convertedProperty.append(mapPropertyName).append("(");
                convertedProperty.append(splitName[i + 1]).append(").value");
                i++;
            } else {
                convertedProperty.append(splitName[i]);
            }
        }
        return convertedProperty.toString();
    }

}

