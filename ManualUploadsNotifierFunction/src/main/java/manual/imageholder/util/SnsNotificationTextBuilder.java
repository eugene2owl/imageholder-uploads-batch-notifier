package manual.imageholder.util;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.sqs.model.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Util class which builds SNS notification text based on given SQS messages.
 */
public class SnsNotificationTextBuilder {

    public static String buildTextAboutUploadedImages(List<Message> messages, LambdaLogger logger) {
        var message = messages.stream()
                .map(it -> SnsNotificationTextBuilder.buildTextAboutUploadedImages(it, logger))
                .collect(Collectors.joining());
        logger.log("Built message text for SNS:\n" + message);
        return message;
    }

    private static String buildTextAboutUploadedImages(Message message, LambdaLogger logger) {
        try {
            var s3Object = S3EventNotification.parseJson(message.getBody())
                    .getRecords()
                    .get(0)
                    .getS3()
                    .getObject();
            return buildTextAboutUploadedImages(s3Object.getKey(), s3Object.getSizeAsLong().toString());
        } catch (Exception e) {
            logger.log("Error while parsing SQS message: " + message);
            throw new RuntimeException(e.getMessage());
        }
    }

    private static String buildTextAboutUploadedImages(String name, String size) {
        return String.format("The image has been uploaded. Name: '%s'. Size: '%s'.\n", name, size);
    }
}
