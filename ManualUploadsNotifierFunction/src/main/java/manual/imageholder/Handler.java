package manual.imageholder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import manual.imageholder.util.SnsNotificationTextBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS Lambda Request Handler which
 * <ol>
 *     <li>does not process input value</li>
 *     <li>reads details about how to work with SNS and SQS from environment variables</li>
 *     <li>receives messages from the SQS queue</li>
 *     <li>extracts meta-information of the (uploaded to AWS S3 bucket) files from the messages</li>
 *     <li>sends the notification about uploaded files to the SNS topic</li>
 * </ol>
 * The function needs to be invoked manually by any of those:
 * <ul>
 *     <li>AWS Console (Lambda service)</li>
 *     <li>web application which uses AWS SDK to work with AWS Lambda client</li>
 *     <li>HTTP request to the attached AWS API Gateway endpoint</li>
 *     <li>AWS CloudWatch/EventBridge scheduled event</li>
 * </ul>
 */
public class Handler implements RequestHandler<Object, String> {

    private final String SQS_QUEUE_NAME = System.getenv("SQS_QUEUE_NAME");
    private final Integer SQS_MAX_NUMBER_OF_MESSAGES = 10;
    private final Integer SQS_WAIT_TIME_SECONDS = Integer.valueOf(System.getenv("SQS_WAIT_TIME_SECONDS"));
    private final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");
    private final String REGION = System.getenv("REGION");
    private LambdaLogger logger;

    private final AmazonSNS snsClient = buildSnsClient();
    private final AmazonSQS sqsClient = buildSqsClient();

    @Override
    public String handleRequest(Object input, Context context) {
        logger = context.getLogger();
        var sqsMessages = retrieveAndDeleteSqsMessagesFromQueue();
        buildAndPublishSnsMessageToTopic(sqsMessages);
        return sqsMessages.size() + " SQS messages has been processed. (version 1)";
    }

    private List<Message> retrieveAndDeleteSqsMessagesFromQueue() {
        var sqsReceiveRequest = buildSqsReceiveMessageRequest();
        var sqsMessages = receiveSqsMessages(sqsReceiveRequest);
        if (!sqsMessages.isEmpty()) {
            var sqsDeleteRequest = buildSqsDeleteMessagesRequest(sqsMessages);
            deleteSqsMessages(sqsDeleteRequest);
        }
        return sqsMessages;
    }

    private AmazonSQS buildSqsClient() {
        return AmazonSQSClientBuilder
                .standard()
                .withRegion(REGION)
                .build();
    }

    private ReceiveMessageRequest buildSqsReceiveMessageRequest() {
        return new ReceiveMessageRequest()
                .withMaxNumberOfMessages(SQS_MAX_NUMBER_OF_MESSAGES)
                .withQueueUrl(SQS_QUEUE_NAME)
                .withWaitTimeSeconds(SQS_WAIT_TIME_SECONDS);
    }

    private List<Message> receiveSqsMessages(ReceiveMessageRequest request) {
        var messages = sqsClient.receiveMessage(request).getMessages();
        logger.log("Received " + messages.size() + " messages from SQS.");
        return messages;
    }

    private DeleteMessageBatchRequest buildSqsDeleteMessagesRequest(List<Message> messages) {
        var entries = messages.stream().map(this::buildSqsDeleteEntry).collect(Collectors.toList());
        return new DeleteMessageBatchRequest()
                .withQueueUrl(SQS_QUEUE_NAME)
                .withEntries(entries);
    }

    private void deleteSqsMessages(DeleteMessageBatchRequest request) {
        var result = sqsClient.deleteMessageBatch(request);
        logger.log("Successfully deleted " + result.getSuccessful().size() + " messages from SQS.");
        if (!result.getFailed().isEmpty()) {
            logger.log("Failed to delete " + result.getFailed().size() + " more messages from SQS.");
        }
    }

    private DeleteMessageBatchRequestEntry buildSqsDeleteEntry(Message message) {
        return new DeleteMessageBatchRequestEntry()
                .withId(message.getMessageId())
                .withReceiptHandle(message.getReceiptHandle());
    }

    private void buildAndPublishSnsMessageToTopic(List<Message> sqsMessages) {
        if (!sqsMessages.isEmpty()) {
            var snsMessageText = SnsNotificationTextBuilder.buildTextAboutUploadedImages(sqsMessages, logger);
            var snsPublishRequest = buildSnsPublishRequest(snsMessageText);
            publishSnsMessage(snsPublishRequest);
        }
    }

    private AmazonSNS buildSnsClient() {
        return AmazonSNSClientBuilder
                .standard()
                .withRegion(REGION)
                .build();
    }

    private PublishRequest buildSnsPublishRequest(String message) {
        return new PublishRequest()
                .withMessage(message)
                .withTopicArn(SNS_TOPIC_ARN);
    }

    public void publishSnsMessage(PublishRequest request) {
        try {
            var result = snsClient.publish(request);
            logger.log("SNS message has been sent. Status is " + result.getSdkHttpMetadata().getHttpStatusCode());
        } catch (Exception e) {
            logger.log("Error while sending message to SNS: " + request.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
