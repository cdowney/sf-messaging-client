package io.cdsoft.sf.messaging.api.consumer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cdsoft.sf.messaging.api.model.PlatformEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class JacksonPlatformEventConsumer<T> implements JsonEventConsumer {

    private final JavaType eventType;
    private final ObjectMapper objectMapper;

    public JacksonPlatformEventConsumer(Class<T> payloadClass, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.eventType = objectMapper.getTypeFactory().constructParametricType(PlatformEvent.class, payloadClass);
    }

    @Override
    public void accept(String json) {
        try {
            PlatformEvent<T> event = objectMapper.readValue(json, eventType);
            handleEvent(event);
        } catch (IOException e) {
            log.debug("Failed to parse message {}", json);
            handleException(e, json, eventType);
        }
    }

    public abstract void handleEvent(PlatformEvent<T> event);

    public void handleException(IOException exception, String json, JavaType eventType) {
        // nop
    }
}
