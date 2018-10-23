// https://searchcode.com/api/result/4293822/

package org.crank.validation.readers;

import java.io.Serializable;
import java.util.*;

import org.crank.annotations.design.AllowsConfigurationInjection;
import org.crank.annotations.design.Implements;
import org.crank.annotations.design.NeedsRefactoring;
import org.crank.core.AnnotationData;
import org.crank.core.AnnotationUtils;
import org.crank.validation.ValidatorMetaData;
import org.crank.validation.ValidatorMetaDataReader;

/**
 * 
 * <p>
 * <b>AnnotationValidatorMetaDataReader</b> reads validation meta-data from
 * annotations.
 * </p>
 * 
 * <p>
 * This class reads a annotation as follows: You pass in the base package of the
 * annotatoins it defaults to "org.crank.annotations.validation". It then takes
 * the <code>name</code> of the <code>ValidatorMetaData</code> and
 * captilalizes the first letter. Thus if you pass the package
 * "com.mycompany.annotations", and
 * <code>ValidatorMetaData.name = "required"</code>, then it will look for an
 * annotation called com.mycompany.annotations.Required. The idea behind this is
 * that you can use annotation without polluting your model classes with Crank
 * annotations.
 * </p>
 * 
 * <p>
 * The parent class that owns the annotation should have annotation as follows:
 * 
 * <pre>
 *   @Required @Length (min=10, max=100)
 *   public String getFirstName(){...
 *   
 *   @Required @Range (min=10, max=100) 
 *   public void setAge() {...
 * </pre>
 * 
 * The <b>firstName</b> corresponds to a property of the Foo class. The
 * <b>firstName</b> is associated with the validation rules <b>required</b>
 * and <b>length</b>. The <b>length</b> validation rule states the minimum and
 * maximum allowed number of characters with the <b>min</b> and <b>max</b>
 * parameters.
 * </p>
 * 
 * <p>
 * Two different frameworks read this meta-data (curently). Our validation
 * framework, which is mostly geared towards server-side validation and our
 * client-side JavaScript framework, which is geared towards producing
 * client-side JavaScript.
 * </p>
 * @author Rick Hightower
 * 
 */
public class AnnotationValidatorMetaDataReader implements ValidatorMetaDataReader, Serializable {
	private static final long serialVersionUID = 1L;

	/** Holds a cache of meta-data to reduce parsing with regex and to avoid
     * reflection. 
     * Since this could get hit by multiple threads. I made it threadsafe.
     * */
    private static Map<String, List<ValidatorMetaData>> metaDataCache = 
        Collections.synchronizedMap(new HashMap<String, List<ValidatorMetaData>>());

    /** Holds a list of pacakges that contain annotations that we will process.
     * If the annotation package is not in this list, it will not be processed.
     */
    private Set<String> validationAnnotationPackages = new HashSet<String>();
    {
        /* By default, we only process our own annotions. */
        validationAnnotationPackages.add("org.crank.annotations.validation");
    }

    /**
     * Read the meta-data from annotations. This copies the meta-data
     * from the annotations into a POJO. It first checks the meta-data cache,
     * if the meta data is not found in the cache it then reads it from the 
     * class.
     * 
     * @param clazz The class that contains the annotations.
     * @param propertyName The name of the property that we are reading 
     * the annotation meta-data from.
     */
    @Implements(interfaceClass=ValidatorMetaDataReader.class)
    public List<ValidatorMetaData> readMetaData(Class<?> clazz, String propertyName) {

        /* Generate a key to the cache based on the classname and the propertyName. */
        String propertyKey = clazz.getName() + "." + propertyName;

        /* Look up the validation meta data in the cache. */
        List<ValidatorMetaData> validatorMetaDataList = metaDataCache.get(propertyKey);

        /* If the meta-data was not found, then generate it. */
        if (validatorMetaDataList == null) { // if not found
            validatorMetaDataList = extractValidatorMetaData(clazz, propertyName, validatorMetaDataList);
            /* Put it in the cache to avoid the processing in the future.
             * Design notes: The processing does a lot of reflection, there
             * is no need to do this each time.
             */
            metaDataCache.put(propertyKey, validatorMetaDataList);            
        }

        return validatorMetaDataList;

    }

    /**
     * Extract Validator Meta Data.
     * @param clazz class
     * @param propertyName property name
     * @param validatorMetaDataList validatorMetaDataList
     * @return validator meta data
     */
    private List<ValidatorMetaData> extractValidatorMetaData(Class<?> clazz, String propertyName, List<ValidatorMetaData> validatorMetaDataList) {
        /* If the meta-data was not found, then generate it. */
        if (validatorMetaDataList == null) { // if not found
            /* Read the annotations from the class based on the property name. */
            Collection<AnnotationData> annotations = AnnotationUtils.getAnnotationDataForFieldAndProperty(clazz, propertyName, this.validationAnnotationPackages);

            /* Extract the POJO based meta-data from the annotations. */
            validatorMetaDataList =
                extractMetaDataFromAnnotations(annotations);

        }
        return validatorMetaDataList;
    }

    /**
     * Extract meta-data from the annotationData we collected thus far.
     * @param annotations The annotationData (preprocessed annotations).
     * @return list of validation meta data.
     */
    private List<ValidatorMetaData> extractMetaDataFromAnnotations(
            Collection<AnnotationData> annotations) {
        List<ValidatorMetaData> list = new ArrayList<ValidatorMetaData>();

        for (AnnotationData annotationData : annotations) {
            ValidatorMetaData validatorMetaData = convertAnnotationDataToValidatorMetaData(annotationData);
            list.add(validatorMetaData);
        }

        return list;
    }

    /**
     * Converts an AnnotationData into a ValidatorMetaData POJO.
     * @param annotationData    annotationData
     * @return validator meta data
     */
    @NeedsRefactoring("This method shows we are calling annotationData.getValues a lot. " +
            "Therefore, we must cache the results of getValues as the annoationData is static " +
            "per property per class. ")
    private ValidatorMetaData convertAnnotationDataToValidatorMetaData(
            AnnotationData annotationData) {
        
        ValidatorMetaData metaData = new ValidatorMetaData();
        metaData.setName(annotationData.getName());

        /* INNEFFECIENT... FIX THIS... see @NeedRefactoring at 
         * getValues and above.*/
        metaData.setProperties(annotationData.getValues());

        return metaData;
    }

    /** We allow a set of validation annotation packages to be configured.
     *  @param validationAnnotationPackages validationAnnotationPackages
     */
    @AllowsConfigurationInjection
    public void setValidationAnnotationPackages(Set<String> validationAnnotationPackages) {
        this.validationAnnotationPackages = validationAnnotationPackages;
    }

}

