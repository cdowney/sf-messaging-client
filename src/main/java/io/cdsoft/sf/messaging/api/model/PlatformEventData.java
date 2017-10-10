package io.cdsoft.sf.messaging.api.model;

import lombok.Data;

@Data
public class PlatformEventData<T> {

    private String schema;
    private Event event;
    private T payload;

    @Data
    public static class Event {
        private Long replayId;
    }
}
