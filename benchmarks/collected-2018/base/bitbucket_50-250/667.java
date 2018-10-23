// https://searchcode.com/api/result/56841028/

/*
 * AdroitLogic UltraESB Enterprise Service Bus
 *
 * Copyright (c) 2010-2013 AdroitLogic Private Ltd. (http://adroitlogic.org). All Rights Reserved.
 *
 * GNU Affero General Public License Usage
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program (See LICENSE-AGPL.TXT).
 * If not, see http://www.gnu.org/licenses/agpl-3.0.html
 *
 * Commercial Usage
 *
 * Licensees holding valid UltraESB Commercial licenses may use this file in accordance with the UltraESB Commercial
 * License Agreement provided with the Software or, alternatively, in accordance with the terms contained in a written
 * agreement between you and AdroitLogic.
 *
 * If you are unsure which license is appropriate for your use, or have questions regarding the use of this file,
 * please contact AdroitLogic at info@adroitlogic.com
 */

package org.adroitlogic.ultraesb.core.template.impl;

import org.adroitlogic.ultraesb.api.BusRuntimeException;
import org.adroitlogic.ultraesb.api.Mediation;
import org.adroitlogic.ultraesb.api.Message;
import org.adroitlogic.ultraesb.api.template.MessageProperty;
import org.adroitlogic.ultraesb.api.template.TemplateInfo;
import org.adroitlogic.ultraesb.api.template.TemplateMethod;
import org.adroitlogic.ultraesb.core.MediationImpl;
import org.adroitlogic.ultraesb.core.MessageImpl;
import org.adroitlogic.ultraesb.core.config.AbstractProxyElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper to keep track of the template instance and takes the responsibility of parsing he template method parameters
 * extracted out from the message properties, into the method with type mapping
 *
 * @author Ruwan
 * @since 2.0.0
 */
public class TemplateCaller<T> extends AbstractProxyElement {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCaller.class);

    private String templateKey;
    private T instance;

    private Method method;
    private List<MethodParamInfo> methodParams = new ArrayList<MethodParamInfo>();
    private String resultProperty;

    public TemplateCaller(String templateKey, T instance) {
        this.templateKey = templateKey;
        this.instance = instance;
    }

    public void start() {

        if (State.STARTED.equals(state) || State.STARTING.equals(state)) {
            throwIllegalStateException("Endpoint " + id + " is already started or is being started");
        }

        state = State.STARTING;

        super.start();

        TemplateInfo info = TemplateRegistry.getInstance().getTemplate(templateKey);
        try {
            for (Method method : info.getTemplateClass().getMethods()) {
                TemplateMethod templateMethod = method.getAnnotation(TemplateMethod.class);
                if (templateMethod != null) {
                    Annotation[][] methodParamAnnotations = method.getParameterAnnotations();
                    int i = -1;
                    for (Class<?> type : method.getParameterTypes()) {
                        i++;
                        if (type.equals(Mediation.class) || type.equals(MediationImpl.class)) {
                            methodParams.add(MethodParamInfo.mediation());
                        } else if (type.equals(Message.class) || type.equals(MessageImpl.class)) {
                            methodParams.add(MethodParamInfo.message());
                        } else {
                            // this should be an annotated message property argument
                            Annotation[] paramAnnotations = methodParamAnnotations[i];
                            boolean paramAnnotated = false;
                            for (Annotation annotation : paramAnnotations) {
                                if (annotation instanceof MessageProperty) {
                                    methodParams.add(new MethodParamInfo(
                                            ((MessageProperty) annotation).value(), type));
                                    paramAnnotated = true;
                                    break;
                                }
                            }
                            if (!paramAnnotated) {
                                // this method cannot be invoked, as there are un mappable parameters
                                handleStartupError("Template method can only have @MessageProperty " +
                                        "arguments except for the Message and Mediation parameters");
                            }
                        }
                    }

                    String resultProperty = templateMethod.resultProperty();
                    if (!resultProperty.isEmpty()) {
                        this.resultProperty = resultProperty;
                    }

                    this.method = method;
                    break;
                }
            }

            if (method == null) {
                handleStartupError("No template method found annotated with " +
                        "@TemplateMethod in the mediation template " + info.getTemplateClass());
            }
        } catch (Exception e) {
            handleStartupError("Error in creating the template instance", e);
        }

        state = State.STARTED;
    }

    /**
     * Main method called by the mediation API to call/invoke the holding template
     * @param msg the message to be executed via the template
     * @param mediation the mediation implementation used in execution
     * @throws Exception in case of an error in processing the template
     */
    public void call(Message msg, Mediation mediation) throws Exception {
        Object[] paramValues = new Object[methodParams.size()];
        int i = 0;
        for (MethodParamInfo paramInfo : methodParams) {
            paramValues[i++] = paramInfo.findValue(msg, mediation);
        }
        Object result = method.invoke(instance, paramValues);
        if (resultProperty != null) {
            msg.addMessageProperty(resultProperty, result);
        }
    }

    public void stop() {
        methodParams.clear();
        method = null;
        instance = null;
    }

    /**
     * Defines the properties of the method parameters that are fed at run time. These are extracted at the
     * initialization time to reduce the overhead at the runtime
     */
    private static class MethodParamInfo {

        /** Whether this is a mediation parameter */
        boolean mediation;
        /** Whether this is a message parameter */
        boolean message;
        /** If it is not mediation or message the name of the message property which defines the paramerter */
        String name;
        /** Type of the parameter */
        Class<?> type;

        static MethodParamInfo mediation() {
            MethodParamInfo info = new MethodParamInfo();
            info.mediation = true;
            return info;
        }

        static MethodParamInfo message() {
            MethodParamInfo info = new MethodParamInfo();
            info.message = true;
            return info;
        }

        private MethodParamInfo() {}

        MethodParamInfo(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        Object findValue(Message msg, Mediation med) {
            if (mediation) {
                return med;
            } else if (message) {
                return msg;
            } else if (name != null && type != null) {
                return msg.getMessageProperty(name, type);
            } else {
                throw new IllegalArgumentException("Property should be one of mediation, " +
                        "message or a pair with name and a type");
            }
        }
    }

    private void handleStartupError(String message, Exception e) {
        state = State.FAILED;
        logger.error(message, e);
        throw new BusRuntimeException(message, e);
    }

    private void handleStartupError(String message) {
        state = State.FAILED;
        logger.error(message);
        throw new BusRuntimeException(message);
    }
}

