package io.cdsoft.sf.messaging.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cdsoft.sf.messaging.internal.client.deserializer.OffsetDateTimeDeserializer;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PushTopicEventData<T> {

    private Event event;
    private T sObject;

    @Data
    public static class Event {
        @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
        private OffsetDateTime createdDate;
        private Long replayId;
        private String type;
    }
}
