package triggered.imageholder;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.beans.EventHandler;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;

public class HandlerTest {

    @Test
    public void testHandleEmptyEvent() throws IOException {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream eventStream = this.getClass().getResourceAsStream("EmptyS3EventInsideSqsEventInput.json");
        SQSEvent event = objectMapper.readValue(eventStream, SQSEvent.class);
        var handler = new Handler();

        // When
        var response = handler.handleRequest(event, new TestContext());

        // Then
        Assert.assertEquals("0 SQS messages has been processed.", response);
    }
}
