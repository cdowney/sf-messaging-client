package io.cdsoft.sf.messaging.api.subscription;

import io.cdsoft.sf.messaging.api.consumer.EventConsumer;
import lombok.ToString;

@ToString
public class PushTopicSubscription<T extends EventConsumer> extends Subscription {

    @Override
    public String getChannelName() {
        return "/topic/" + getTopic();
    }

    public PushTopicSubscription(String name, Long replayFrom, T consumer) {
        super(name, replayFrom, consumer);
    }
}
