package io.cdsoft.sf.messaging.internal.client;

import io.cdsoft.sf.messaging.MessagingException;

public interface ManagedClient {

    default void doStart() throws MessagingException {
    }

    default void doStop() throws MessagingException {
    }

}
