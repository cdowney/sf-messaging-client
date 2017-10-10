package io.cdsoft.sf.messaging.api.consumer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cdsoft.sf.messaging.api.model.PushTopicEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class JacksonPushTopicEventConsumer<T> implements JsonEventConsumer {

    private final JavaType eventType;
    private final ObjectMapper objectMapper;

    public JacksonPushTopicEventConsumer(Class<T> payloadClass, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.eventType = objectMapper.getTypeFactory().constructParametricType(PushTopicEvent.class, payloadClass);
    }

    @Override
    public void accept(String json) {
        try {
            PushTopicEvent<T> event = objectMapper.readValue(json, eventType);
            handleEvent(event);
        } catch (IOException e) {
            log.debug("Failed to parse message {}", json);
            handleException(e, json, eventType);
        }
    }

    public abstract void handleEvent(PushTopicEvent<T> event);

    public void handleException(IOException exception, String json, JavaType eventType) {
        // nop
    }
}
