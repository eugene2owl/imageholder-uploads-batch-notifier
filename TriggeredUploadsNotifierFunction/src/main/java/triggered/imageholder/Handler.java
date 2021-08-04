package triggered.imageholder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import triggered.imageholder.util.SnsNotificationTextBuilder;

import java.util.List;

/**
 * AWS Lambda Request Handler which
 * <ol>
 *     <li>receives messages from the SQS queue an an input value</li>
 *     <li>reads details about how to work with SNS from environment variables</li>
 *     <li>extracts meta-information of the (uploaded to AWS S3 bucket) files from the messages</li>
 *     <li>sends the notification about uploaded files to the SNS topic</li>
 * </ol>
 * The function is triggered automatically by the attached SQS queue.
 */
public class Handler implements RequestHandler<SQSEvent, String> {

    private final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");
    private final String REGION = System.getenv("REGION");
    private LambdaLogger logger;

    private final AmazonSNS snsClient = buildSnsClient();

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        logger = context.getLogger();
        var records = extractS3Records(event);
        buildAndPublishSnsMessageToTopic(records);
        return records.size() + " SQS messages has been processed.";
    }

    private List<S3EventNotificationRecord> extractS3Records(SQSEvent event) {
        var s3EventJsonBody = event.getRecords().get(0).getBody();
        var s3Event = S3EventNotification.parseJson(s3EventJsonBody);
        return s3Event.getRecords();
    }

    private void buildAndPublishSnsMessageToTopic(List<S3EventNotificationRecord> sqsMessages) {
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
