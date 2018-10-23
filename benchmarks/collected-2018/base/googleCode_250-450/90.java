// https://searchcode.com/api/result/4293786/

package org.crank.web.validation;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.crank.annotations.design.AllowsConfigurationInjection;
import org.crank.annotations.design.ExpectsInjection;
import org.crank.annotations.design.Implements;
import org.crank.annotations.design.NeedsRefactoring;
import org.crank.core.TemplateUtils;
import org.crank.message.MessageSpecification;
import org.crank.web.contribution.Contribution;
import org.crank.web.contribution.SimpleScript;

/**
 * <p>
 * This class represents a JavaScript validation rule.
 * </p>
 * <p>
 * This class is the heart and soul of our client side validation framework.
 * </p>
 * 
 * <p>
 * It takes a validation contribution configured in the IoC container and uses
 * it as a template. 
 * 
 * It also looks up i18N validation error messages which can be configured
 * in the IoC container or passed via valiation meta-data (which can come from
 * annotations or properties). 
 * </p>
 * <p>
 * The i18N messages that you can configure are very flexible. 
 * You can pass an array of messageArgumentNames (via DI). 
 * These are the names of validation settings (e.g., min, max, length) 
 * that you want to use in your message. 
 * For example if this were set to ["min","max"] then the min and 
 * max validation rule parameters would be passed to the validation message. 
 * </p>
 * Thus if the validation message was: 
 * <code>
 * "{0} must be between {1} and {2}"
 * </code>
 * 
 * If the field name were age and the min value was 1 and 
 * the max value was 100 then the message would read:
 * <code>
 * "Age must be between 1 and 100"
 * </code>
 * 
 * @author Rick Hightower
 * 
 */
public class SimpleScriptValidator extends SimpleScript implements
        ValidatorTemplateContribution {

    /** Holds the message associated with this JavaScript validation rule. */
    private MessageSpecification messageSpecification;
    
    /** Holds configurable template arguments */
    private Map<String,Object> templateArguments; 

    /**
     * Holds message arguments for the validation rule.
     * 
     * These are the names of validation settings (e.g., min, max, length) that
     * you want to use in your message.
     * 
     * So for example if this were set to ["min","max"] then the min and max
     * parameters would be passed to the validation message.
     * 
     * Thus if the validation message was:
     * 
     * "{0} must be between {1} and {2}"
     * 
     * If the field name were age and the min value was 1 and the max value was
     * 100 then the message would read:
     * 
     * "Age must be between 1 and 100"
     * 
     */
    private String messageArgumentNames[] = new String[] {};

    /**
     * Adds the template we have configured to the browser page.
     */
    @Override
    @Implements(interfaceClass = Contribution.class)
    public void addToWriter(Writer writer) throws IOException {

        Map<String, Object> validationMetaData = buildTemplateArguments();

        /* Process the template. */
        String output = processTemplate(validationMetaData);

        /* Write out the output to the Browser. */
        writer.write(output);

        /* Clear the validation context. */
        validatorContextHolder.set(null);
    }

    /**
     * Builds the template arguments. 
     * @return
     */
    private Map<String, Object> buildTemplateArguments() {
        /* Holds arguments to template. */
        Map<String, Object> validationMetaData = null;

        /* Holds the name of the field being validated. */
        String fieldName;

        /*
         * If the validatorContext is set, use it to load the arguments and
         * field name.
         */
        if (validatorContextHolder.get() != null) {
            validationMetaData = validatorContextHolder.get().getValidationRuleMetaData();
            fieldName = validatorContextHolder.get().getFieldName();
        } else {
            validationMetaData = new HashMap<String, Object>();
            fieldName = "none";
        }
        
        

        /*
         * Build the validaiton messages and add them to the arguments that we
         * are going to pass to the template.
         */
        buildValidatorMessages(validationMetaData, fieldName);
        
        /* If they configured any template arguments, use them. */
        if (this.templateArguments!=null) {
            validationMetaData.putAll(this.templateArguments);
        }
        return validationMetaData;
    }

    /**
     * Process the Template.
     * 
     * @param arguments
     * @return
     */
    @NeedsRefactoring("Currently this always calls TemplateUtils, which is "
            + "something Scott and I cooked up (mostly Scott) many years ago. "
            + " It would be nice "
            + " if this were a plug-in point and we could plug-in any template " 
            + " mechanism we wanted i.e., Velocity, Freemarker, Groovy templates, " +
                    "etc. "
            + " We want to ship with a good-enough template mechanism to reduce "
            + " dependencies on other frameworks but allow the ability to use a " 
            + " real template library if needed. ")
    private String processTemplate(Map<String, Object> arguments) {

        /*
         * The orginal utility had a small bug that I fixed. In the course of
         * fixing said bug, I highly "optimized" the way a template is
         * processed. It would be good to do a little profiling of the old and
         * new version since this might be a performance bottle neck.
         */
        return TemplateUtils.newReplaceAll(this.getContributionText(), arguments);

        // This was my first whack at a cheap and dirty template. Highly
        // inneffcient.
        // I knew Scott had a more effecient version.
        // String template = this.contributionText;
        // //messageSpecification.createDetailMessage();
        // /* This is really inefficient... we should improve this. */
        // for (Map.Entry<String, Object> entry : arguments.entrySet()) {
        // template = template.replaceAll(
        // "\\{"+entry.getKey() +"\\}",
        // entry.getValue().toString());
        // }
        // return template;
    }

    private void buildValidatorMessages(Map<String, Object> validatorMetaData,
            String fieldName) {

        if (messageSpecification != null) {

            /*
             * Extract the argument values based on the argument names
             * (messageArgumentNames) that the developer configured.
             */
            Object[] args = new Object[messageArgumentNames.length];
            for (int index = 0; index < messageArgumentNames.length; index++) {
                args[index] = validatorMetaData.get(messageArgumentNames[index]);
            }

            /*
             * The detail message could have been passed as part of the
             * meta-data (property file entry or annotations). If the
             * detailMessage is in the meta data use it instead of the one
             * configured.
             */
            String detailMessage = (String) validatorMetaData.get("detailMessage");
            messageSpecification.setCurrentSubject(fieldName);

            /*
             * If the detail message is missing from the meta-data, then use the
             * detail message from the messageSpecification instead.
             */
            if (detailMessage == null || "".equals(detailMessage.trim())) {
                validatorMetaData.put("detailMessage", messageSpecification
                        .createDetailMessage(args));
            }
            /*
             * If the detail message from the meta-data is present, use it.
             */
            else {
                validatorMetaData.put("detailMessage", messageSpecification
                        .createMessage(detailMessage, null, args));
            }

            /* This does the same as above, except for the summary message. */
            String summaryMessage = (String) validatorMetaData.get("summaryMessage");
            messageSpecification.setCurrentSubject(fieldName);
            if (summaryMessage == null || "".equals(summaryMessage.trim())) {
                validatorMetaData.put("summaryMessage", messageSpecification
                        .createSummaryMessage(args));
            } else {
                validatorMetaData.put("summaryMessage", messageSpecification
                        .createMessage(summaryMessage, null, args));
            }

        }
        
    }

    /**
     * Holds the validator context as a thread local varaible owned by an
     * instance of this class.
     */
    private ThreadLocal<ValidatorContext> validatorContextHolder = new ThreadLocal<ValidatorContext>();

    /**
     * Places the validator context in the current instance for the current
     * thread.
     */
    public void placeValidatorContext(ValidatorContext validatorContext) {
        validatorContextHolder.set(validatorContext);

    }

    @ExpectsInjection
    public void setMessageSpecification(MessageSpecification messageSpecification) {
        this.messageSpecification = messageSpecification;
    }

    public void setMessageArgumentNames(String[] args) {
        this.messageArgumentNames = args;
    }

    @AllowsConfigurationInjection
    public void setTemplateArguments(Map<String, Object> templateArguments) {
        this.templateArguments = templateArguments;
    }

}

