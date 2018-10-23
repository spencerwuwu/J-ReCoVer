// https://searchcode.com/api/result/123726232/

/**
 * Copyright 2011 Steffen Opel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.utoolity.bamboo.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.utoolity.atlassian.dry.AmazonWebServiceClientFactory;
import net.utoolity.bamboo.plugins.aws.AWS;
import net.utoolity.bamboo.plugins.aws.CloudFormation;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.variable.CustomVariableContextImpl;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class CloudFormationTask extends CustomVariableContextImpl implements TaskType
{
    private static final String VARIABLE_PREFIX = "custom.aws."; // TODO: move to base class once available
    private static final String VARIABLE_RESOURCES = VARIABLE_PREFIX + "cfn.stack.resources";
    private static final String VARIABLE_OUTPUTS = VARIABLE_PREFIX + "cfn.stack.outputs";
    private static final String STACK_STATUS_NO_SUCH_STACK = "NO_SUCH_STACK";
    private static final String STACK_REASON_NO_SUCH_STACK = "Stack has been deleted";
    @SuppressWarnings("unused")
    private static final Set<String> STACK_STATUS_COMPLETE_SET = ImmutableSet
            .<String> builder()
            .add(StackStatus.CREATE_COMPLETE.toString(), StackStatus.DELETE_COMPLETE.toString(),
                    StackStatus.ROLLBACK_COMPLETE.toString(), StackStatus.UPDATE_COMPLETE.toString(),
                    StackStatus.UPDATE_ROLLBACK_COMPLETE.toString()).build();
    private static final Set<String> STACK_STATUS_FAILED_SET = ImmutableSet
            .<String> builder()
            .add(StackStatus.CREATE_FAILED.toString(), StackStatus.DELETE_FAILED.toString(),
                    StackStatus.ROLLBACK_FAILED.toString(), StackStatus.UPDATE_ROLLBACK_FAILED.toString()).build();
    private static final Set<String> STACK_STATUS_IN_PROGRESS_SET = ImmutableSet
            .<String> builder()
            .add(StackStatus.CREATE_IN_PROGRESS.toString(), StackStatus.DELETE_IN_PROGRESS.toString(),
                    StackStatus.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS.toString(),
                    StackStatus.ROLLBACK_IN_PROGRESS.toString(),
                    StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString(),
                    StackStatus.UPDATE_IN_PROGRESS.toString(), StackStatus.UPDATE_ROLLBACK_IN_PROGRESS.toString())
            .build();
    private static final Set<String> BAMBOO_SUCCESS_SET = ImmutableSet
            .<String> builder()
            .add(StackStatus.CREATE_COMPLETE.toString(), StackStatus.DELETE_COMPLETE.toString(),
                    StackStatus.UPDATE_COMPLETE.toString(), STACK_STATUS_NO_SUCH_STACK).build();
    private static final Set<String> BAMBOO_FAILED_SET = ImmutableSet.<String> builder()
            .addAll(STACK_STATUS_FAILED_SET)
            .add(StackStatus.ROLLBACK_COMPLETE.toString(), StackStatus.UPDATE_ROLLBACK_COMPLETE.toString()).build();
    private int maxErrorRetry = AWS.DEFAULT_MAX_ERROR_RETRY;
    private long awaitTransitionInterval = AWSTaskConfigurator.DEFAULT_AWAIT_TRANSITION_INTERVAL;

    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException, AmazonServiceException,
            AmazonClientException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.create(taskContext);
        final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
        final Map<String, String> variablesMap = this.getVariables(taskContext.getBuildContext());

        // KLUDGE: allow tasks saved by versions < 1.4.0 to be executed without enforcing an edit.
        final String stackTransition = configurationMap.containsKey(CloudFormationTaskConfigurator.STACK_TRANSITION) ? configurationMap
                .get(CloudFormationTaskConfigurator.STACK_TRANSITION) : configurationMap
                .get(CloudFormationTaskConfigurator.STACK_ACTION);
        final String stackRegion = AWSTaskConfigurator.extractRegionFromConfigurationMap(configurationMap,
                CloudFormationTaskConfigurator.STACK_REGION);
        final String stackName = configurationMap.get(CloudFormationTaskConfigurator.STACK_NAME);
        final AWSCredentials credentials = new BasicAWSCredentials(
                configurationMap.get(AWSTaskConfigurator.ACCESS_KEY),
                configurationMap.get(AWSTaskConfigurator.SECRET_KEY));
        maxErrorRetry = (int) AWSTaskConfigurator.getAsLong(variablesMap, AWSTaskConfigurator.MAX_ERROR_RETRY,
                AWS.DEFAULT_MAX_ERROR_RETRY);
        awaitTransitionInterval = AWSTaskConfigurator.getAsLong(variablesMap,
                AWSTaskConfigurator.AWAIT_TRANSITION_INTERVAL, AWSTaskConfigurator.DEFAULT_AWAIT_TRANSITION_INTERVAL);
        final ClientConfiguration clientConfiguration = new ClientConfiguration().withMaxErrorRetry(maxErrorRetry);
        final String endpoint = CloudFormation.ENDPOINT_MAP.get(stackRegion);
        // No use going on without an endpoint
        if (null == endpoint)
        {
            buildLogger.addErrorLogEntry("Failed to retrieve endpoint for specified region: " + stackRegion);
            taskResultBuilder.failedWithError();
        }
        else
        {
            try
            {
                buildLogger.addBuildLogEntry("Configuring client with maxErrorRetry=" + maxErrorRetry
                        + " and awaitTransitionInterval=" + awaitTransitionInterval);
                AmazonWebServiceClientFactory clientFactory = new AmazonWebServiceClientFactory();
                AmazonCloudFormation cloudFormation = clientFactory.createClient(AmazonCloudFormationClient.class,
                        credentials, clientConfiguration);
                buildLogger.addBuildLogEntry("Selecting endpoint " + endpoint + " (" + stackRegion + ")");
                cloudFormation.setEndpoint(endpoint);

                if (CloudFormationTaskConfigurator.CREATE_TRANSITION.equals(stackTransition))
                {
                    final String templateSource = configurationMap.get(CloudFormationTaskConfigurator.TEMPLATE_SOURCE);
                    final String templateParameters = configurationMap
                            .get(CloudFormationTaskConfigurator.TEMPLATE_PARAMETERS);
                    final String notificationARN = configurationMap.get(CloudFormationTaskConfigurator.SNS_TOPIC);
                    final Set<String> notificationARNs = StringUtils.isEmpty(notificationARN) ? ImmutableSet
                            .<String> builder().build() : ImmutableSet.<String> builder().add(notificationARN).build();
                    final Integer creationTimeout = CloudFormationTaskConfigurator
                            .tryParsePositiveInteger(configurationMap
                                    .get(CloudFormationTaskConfigurator.CREATION_TIMEOUT));
                    final Boolean enableRollback = configurationMap
                            .getAsBoolean(CloudFormationTaskConfigurator.ENABLE_ROLLBACK);
                    final Boolean enableIAM = configurationMap.getAsBoolean(CloudFormationTaskConfigurator.ENABLE_IAM);

                    buildLogger.addBuildLogEntry("Selected template source is " + templateSource);
                    if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_URL.equals(templateSource))
                    {
                        final String templateURL = configurationMap.get(CloudFormationTaskConfigurator.TEMPLATE_URL);
                        buildLogger.addBuildLogEntry("Selected template URL is " + templateURL);
                        final String transitionResult = createStack(stackName, templateSource, templateURL,
                                templateParameters, notificationARNs, creationTimeout, enableRollback, enableIAM,
                                cloudFormation, buildLogger);
                        determineTaskResult(transitionResult, taskResultBuilder, buildLogger);
                    }
                    else if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_BODY.equals(templateSource))
                    {
                        final String templateBody = configurationMap.get(CloudFormationTaskConfigurator.TEMPLATE_BODY);
                        final String transitionResult = createStack(stackName, templateSource, templateBody,
                                templateParameters, notificationARNs, creationTimeout, enableRollback, enableIAM,
                                cloudFormation, buildLogger);
                        determineTaskResult(transitionResult, taskResultBuilder, buildLogger);
                    }
                    else
                    {
                        buildLogger.addErrorLogEntry(AWSTaskConfigurator.CONTACT_SUPPORT);
                        taskResultBuilder.failedWithError();
                    }
                }
                else if (CloudFormationTaskConfigurator.UPDATE_TRANSITION.equals(stackTransition))
                {
                    final String templateSource = configurationMap.get(CloudFormationTaskConfigurator.TEMPLATE_SOURCE);
                    final String templateParameters = configurationMap
                            .get(CloudFormationTaskConfigurator.TEMPLATE_PARAMETERS);
                    final Boolean enableIAM = configurationMap.getAsBoolean(CloudFormationTaskConfigurator.ENABLE_IAM);

                    buildLogger.addBuildLogEntry("Selected template source is " + templateSource);
                    if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_URL.equals(templateSource))
                    {
                        final String templateURL = configurationMap.get(CloudFormationTaskConfigurator.TEMPLATE_URL);
                        buildLogger.addBuildLogEntry("Selected template URL is " + templateURL);
                        final String transitionResult = updateStack(stackName, templateSource, templateURL,
                                templateParameters, enableIAM, cloudFormation, buildLogger);
                        determineTaskResult(transitionResult, taskResultBuilder, buildLogger);
                    }
                    else if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_BODY.equals(templateSource))
                    {
                        final String templateBody = configurationMap.get(CloudFormationTaskConfigurator.TEMPLATE_BODY);
                        final String transitionResult = updateStack(stackName, templateSource, templateBody,
                                templateParameters, enableIAM, cloudFormation, buildLogger);
                        determineTaskResult(transitionResult, taskResultBuilder, buildLogger);
                    }
                    else
                    {
                        buildLogger.addErrorLogEntry(AWSTaskConfigurator.CONTACT_SUPPORT);
                        taskResultBuilder.failedWithError();
                    }
                }
                else if (CloudFormationTaskConfigurator.DELETE_TRANSITION.equals(stackTransition))
                {
                    final String transitionResult = deleteStack(stackName, cloudFormation, buildLogger);
                    determineTaskResult(transitionResult, taskResultBuilder, buildLogger);
                }
                else
                {
                    buildLogger.addErrorLogEntry(AWSTaskConfigurator.CONTACT_SUPPORT);
                    taskResultBuilder.failedWithError();
                }
            }
            catch (AmazonServiceException ase)
            {
                buildLogger.addErrorLogEntry("Stack request rejected by AWS!", ase);
                taskResultBuilder.failedWithError();

            }
            catch (AmazonClientException ace)
            {
                buildLogger.addErrorLogEntry("Failed to communicate with AWS!", ace);
                taskResultBuilder.failedWithError();
            }
            catch (Exception e)
            {
                buildLogger.addErrorLogEntry("Failed to fetch resource from AWS!", e);
                taskResultBuilder.failedWithError();
            }
        }

        return taskResultBuilder.build();
    }

    /**
     * @param stackName
     * @param templateSourceType
     * @param templateSourceValue
     * @param templateParameters
     * @param notificationARNs
     * @param creationTimeout
     * @param enableRollback
     * @param enableIAM
     * @param cloudFormation
     * @param buildLogger
     * @return String
     * @throws AmazonServiceException
     * @throws AmazonClientException
     * @throws Exception
     */
    private String createStack(final String stackName, final String templateSourceType,
            final String templateSourceValue, final String templateParameters,
            final Collection<String> notificationARNs, final Integer timeoutInMinutes, final Boolean enableRollback,
            final Boolean enableIAM, AmazonCloudFormation cloudFormation, final BuildLogger buildLogger)
            throws AmazonServiceException, AmazonClientException, Exception
    {
        CreateStackRequest createRequest = new CreateStackRequest().withStackName(stackName)
                .withParameters(parseParameters(templateParameters)).withDisableRollback(!enableRollback);

        if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_URL.equals(templateSourceType))
        {
            createRequest.setTemplateURL(templateSourceValue);
        }
        else if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_BODY.equals(templateSourceType))
        {
            createRequest.setTemplateBody(templateSourceValue);
        }
        createRequest.setNotificationARNs(notificationARNs);
        if (null != timeoutInMinutes)
        {
            createRequest.setTimeoutInMinutes(timeoutInMinutes);
        }
        if (enableIAM)
        {
            createRequest.withCapabilities("CAPABILITY_IAM");
        }
        buildLogger.addBuildLogEntry("Creating stack '" + createRequest.getStackName() + "':");
        cloudFormation.createStack(createRequest);

        // Wait for the stack to be created
        return waitForTransitionCompletion(cloudFormation, stackName, buildLogger);
    }

    /**
     * @param stackName
     * @param templateSourceType
     * @param templateSourceValue
     * @param templateParameters
     * @param enableIAM
     * @param cloudFormation
     * @param buildLogger
     * @return String
     * @throws AmazonServiceException
     * @throws AmazonClientException
     * @throws Exception
     */
    private String updateStack(final String stackName, final String templateSourceType,
            final String templateSourceValue, final String templateParameters, final Boolean enableIAM,
            AmazonCloudFormation cloudFormation, final BuildLogger buildLogger) throws AmazonServiceException,
            AmazonClientException, Exception
    {
        UpdateStackRequest updateRequest = new UpdateStackRequest().withStackName(stackName).withParameters(
                parseParameters(templateParameters));

        if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_URL.equals(templateSourceType))
        {
            updateRequest.setTemplateURL(templateSourceValue);
        }
        else if (CloudFormationTaskConfigurator.TEMPLATE_SOURCE_BODY.equals(templateSourceType))
        {
            updateRequest.setTemplateBody(templateSourceValue);
        }
        if (enableIAM)
        {
            updateRequest.withCapabilities("CAPABILITY_IAM");
        }
        buildLogger.addBuildLogEntry("Updating stack '" + updateRequest.getStackName() + "':");
        cloudFormation.updateStack(updateRequest);

        // Wait for the stack to be created
        return waitForTransitionCompletion(cloudFormation, stackName, buildLogger);
    }

    /**
     * @param stackName
     * @param templateBody
     * @param cloudFormation
     * @param buildLogger
     * @return String
     * @throws AmazonServiceException
     * @throws AmazonClientException
     * @throws Exception
     */
    private String deleteStack(final String stackName, AmazonCloudFormation cloudFormation,
            final BuildLogger buildLogger) throws AmazonServiceException, AmazonClientException, Exception
    {
        // Delete the stack
        DeleteStackRequest deleteRequest = new DeleteStackRequest();
        deleteRequest.setStackName(stackName);
        buildLogger.addBuildLogEntry("Deleting stack '" + deleteRequest.getStackName() + "':");
        cloudFormation.deleteStack(deleteRequest);

        // Wait for the stack to be deleted
        return waitForTransitionCompletion(cloudFormation, stackName, buildLogger);
    }

    /**
     * @param transitionResult
     * @param taskResultBuilder
     * @param buildLogger
     */
    private void determineTaskResult(final String transitionResult, final TaskResultBuilder taskResultBuilder,
            final BuildLogger buildLogger)
    {
        if (BAMBOO_SUCCESS_SET.contains(transitionResult))
        {
            taskResultBuilder.success();
        }
        else if (BAMBOO_FAILED_SET.contains(transitionResult))
        {
            taskResultBuilder.failed();
        }
        else
        {
            buildLogger.addErrorLogEntry(AWSTaskConfigurator.CONTACT_SUPPORT);
            taskResultBuilder.failedWithError();
        }
    }

    /**
     * @param stack
     * @param buildLogger
     * @throws AmazonClientException
     * @throws AmazonServiceException
     */
    private void describeOutputs(final Stack stack, final BuildLogger buildLogger)
    {
        if (StackStatus.CREATE_COMPLETE.toString().equals(stack.getStackStatus()))
        {
            List<Output> outputs = stack.getOutputs();
            List<String> outputKeys = new ArrayList<String>(outputs.size());
            buildLogger.addBuildLogEntry("Stack '" + stack.getStackName() + "' generated "
                    + new Integer(outputs.size()).toString() + " outputs:");
            for (Output output : outputs)
            {
                buildLogger
                        .addBuildLogEntry("\tKey: " + output.getOutputKey() + " | Value: " + output.getOutputValue());
                // NOTE: the initial version lacked the desired custom variable prefix and is retained for
                // compatibility!
                this.addCustomData(output.getOutputKey(), output.getOutputValue());
                this.addCustomData(VARIABLE_PREFIX + output.getOutputKey(), output.getOutputValue());
                outputKeys.add(output.getOutputKey());
            }
            this.addCustomData(VARIABLE_OUTPUTS, Joiner.on(";").join(outputKeys));
        }
    }

    /**
     * @param stack
     * @param buildLogger
     * @throws AmazonClientException
     * @throws AmazonServiceException
     */
    private void describeResources(final String stackName, AmazonCloudFormation cloudFormation,
            final BuildLogger buildLogger)
    {
        DescribeStackResourcesRequest describeStackResourcesRequest = new DescribeStackResourcesRequest();
        describeStackResourcesRequest.setStackName(stackName);
        DescribeStackResourcesResult describeStackResourcesResult = cloudFormation
                .describeStackResources(describeStackResourcesRequest);

        List<StackResource> stackResources = describeStackResourcesResult.getStackResources();
        List<String> resourceIds = new ArrayList<String>(stackResources.size());
        buildLogger.addBuildLogEntry("Task affects " + new Integer(stackResources.size()).toString() + " resources:");
        for (StackResource resource : stackResources)
        {
            buildLogger.addBuildLogEntry("\tId: " + resource.getPhysicalResourceId() + " | Status: "
                    + resource.getResourceStatus());
            this.addCustomData(VARIABLE_PREFIX + resource.getPhysicalResourceId(), resource.getResourceStatus());
            resourceIds.add(resource.getPhysicalResourceId());
        }
        this.addCustomData(VARIABLE_RESOURCES, Joiner.on(";").join(resourceIds));
    }

    /**
     * Wait for a stack to complete transitioning (i.e. status not being in STACK_STATUS_IN_PROGRESS_SET or the stack no
     * longer existing).
     * 
     * @param cloudFormation
     * @param stackName
     * @param BuildLogger
     * @throws Exception
     */
    public String waitForTransitionCompletion(AmazonCloudFormation cloudFormation, String stackName,
            BuildLogger buildLogger) throws Exception
    {
        DescribeStacksRequest describeRequest = new DescribeStacksRequest();
        describeRequest.setStackName(stackName);
        Boolean transitionCompleted = false;
        String stackStatus = "Unknown";
        String stackReason = "Unspecified";
        Stack transitionedStack = null;
        String nextToken = null;
        Set<String> processedEventIds = new HashSet<String>();

        while (!transitionCompleted)
        {
            // REVIEW: contrary to the AWS CloudFormation sample describeStacks() throws an exception once the stack is
            // deleted rather than returning an empty list.
            try
            {
                List<Stack> stacks = cloudFormation.describeStacks(describeRequest).getStacks();
                if (stacks.isEmpty())
                {
                    transitionCompleted = true;
                }
                else
                {
                    for (Stack stack : stacks)
                    {
                        DescribeStackEventsRequest stackEventsRequest = new DescribeStackEventsRequest().withStackName(
                                stackName).withNextToken(nextToken);
                        DescribeStackEventsResult stackEventsResult = cloudFormation
                                .describeStackEvents(stackEventsRequest);
                        nextToken = stackEventsResult.getNextToken();
                        List<StackEvent> stackEventsView = Lists.reverse(stackEventsResult.getStackEvents());
                        for (StackEvent stackEvent : stackEventsView)
                        {
                            // REVIEW: currently all states the stack has been in since creation are reported for
                            // update and delete requests as well, which might be skipped eventually to reduce log
                            // noise? [e.g. transitionStarted.isBefore(stackEvent.getTimestamp().getTime()]

                            // Has this event already been processed once?
                            if (processedEventIds.add(stackEvent.getEventId()))
                            { // no
                                buildLogger.addBuildLogEntry("... '"
                                        + stackEvent.getLogicalResourceId()
                                        + "' entered status "
                                        + stackEvent.getResourceStatus()
                                        + " ("
                                        + new DateTime(stackEvent.getTimestamp()).toString(ISODateTimeFormat
                                                .basicDateTimeNoMillis().withZone(DateTimeZone.UTC)) + ") ...");
                            }
                        }
                        if (!STACK_STATUS_IN_PROGRESS_SET.contains(stack.getStackStatus()))
                        {
                            transitionCompleted = true;
                            transitionedStack = stack;
                        }
                    }
                }
            }
            catch (AmazonServiceException ase)
            {
                // KLUDGE: see REVIEW above for the reasoning behind this.
                if (ase.getMessage().equals("Stack:" + stackName + " does not exist"))
                {
                    transitionCompleted = true;
                }
                else
                {
                    buildLogger.addErrorLogEntry("Failed to describe stack '" + stackName + "'!", ase);
                    throw ase;
                }
            }

            // Sleep until transition has completed.
            if (!transitionCompleted)
            {
                Thread.sleep(awaitTransitionInterval);
            }
        }

        if (null == transitionedStack)
        {
            stackStatus = STACK_STATUS_NO_SUCH_STACK;
            stackReason = STACK_REASON_NO_SUCH_STACK;
            buildLogger.addBuildLogEntry("Transition of stack '" + stackName + "' completed with status " + stackStatus
                    + " (" + stackReason + ").");
        }
        else
        {
            stackStatus = transitionedStack.getStackStatus();
            stackReason = transitionedStack.getStackStatusReason();
            buildLogger.addBuildLogEntry("Transition of stack '" + stackName + "' completed with status " + stackStatus
                    + " (" + stackReason + ").");
            describeOutputs(transitionedStack, buildLogger);
            describeResources(stackName, cloudFormation, buildLogger);
        }

        return stackStatus;
    }

    public List<Parameter> parseParameters(String templateParameters)
    {
        List<Parameter> result = new Vector<Parameter>();

        if (StringUtils.isNotEmpty(templateParameters))
        {
            for (String templateParameter : templateParameters.split(";"))
            {
                String[] token = templateParameter.split("=");
                Parameter parameter = new Parameter().withParameterKey(token[0].trim()).withParameterValue(
                        token[1].trim());
                result.add(parameter);
            }
        }

        return result;
    }
}

