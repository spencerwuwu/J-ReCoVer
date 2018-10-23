// https://searchcode.com/api/result/102849178/

package com.newvem.sns.resource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.codehaus.jackson.node.ObjectNode;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.UpdateTableRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Topic;

/**
 * Represents a Minimalistic SNS Resource Template
 * 
 * @author aldrin
 */
@Path("/sns")
public class SNSResource extends BaseSNSResource {
	@Inject
	AmazonDynamoDB dynamoDB;

	@GET
	@Produces("text/plain")
	@Path("/test/" + ID_MASK)
	public String sendMessage(@PathParam("id") String tableId)
			throws Exception {
		PublishRequest publishRequest = new PublishRequest();

		publishRequest.setMessage("{ \"Trigger\":{ \"Dimensions\":[ { \"name\":\"TableName\", \"value\":" + tableId + " } ] } }");
		publishRequest.setSubject("Reduce Request");

		ListTopicsResult listTopics = snsClient.listTopics();

		for (Topic t : listTopics.getTopics()) {
			if (t.getTopicArn().endsWith(tableId)) {
				publishRequest.setTopicArn(t.getTopicArn());
				break;
			}
		}

		PublishResult result = snsClient.publish(publishRequest);

		return result.getMessageId();
	}

	@Override
	public void handleNotification(String endpointId, ObjectNode bodyNode)
			throws Exception {
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput();

		provisionedThroughput.setReadCapacityUnits(1L);
		provisionedThroughput.setWriteCapacityUnits(1L);

		ObjectNode messageData = (ObjectNode) objectMapper.readTree(bodyNode.get("Message").getTextValue());
		
		String tableName = messageData.get("Trigger").get("Dimensions").get(0).get("value").getTextValue();
		
		UpdateTableRequest updateTableRequest = new UpdateTableRequest()
				.withTableName(tableName)
				.withProvisionedThroughput(provisionedThroughput);

		try {
			dynamoDB.updateTable(updateTableRequest);
		} catch (Exception exc) {
			if (logger.isWarnEnabled())
				logger.warn("Error", exc);
			
			// Comment-out the next section when in production
			throw exc;
		}
	}
}

