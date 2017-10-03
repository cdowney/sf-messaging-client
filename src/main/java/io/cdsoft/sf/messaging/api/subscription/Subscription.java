package io.cdsoft.sf.messaging.api.subscription;

import io.cdsoft.sf.messaging.api.consumer.EventConsumer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public abstract class Subscription<T extends EventConsumer> {

    private String topic;
    private Long replayFrom = -1L;
    private T consumer;

    public abstract String getChannelName();
}