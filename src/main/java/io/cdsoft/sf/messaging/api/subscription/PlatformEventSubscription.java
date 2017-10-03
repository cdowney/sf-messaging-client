package io.cdsoft.sf.messaging.api.subscription;

import io.cdsoft.sf.messaging.api.consumer.EventConsumer;
import lombok.ToString;

@ToString
public class PlatformEventSubscription<T extends EventConsumer> extends Subscription {

    @Override
    public String getChannelName() {
        return "/event/" + getTopic();
    }

    public PlatformEventSubscription(String name, Long replayFrom, T consumer) {
        super(name, replayFrom, consumer);
    }
}
