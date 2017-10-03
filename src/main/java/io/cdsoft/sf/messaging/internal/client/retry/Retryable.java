package io.cdsoft.sf.messaging.internal.client.retry;

import io.cdsoft.sf.messaging.MessagingException;

@FunctionalInterface
public interface Retryable<T> {

    T run() throws MessagingException;

}
