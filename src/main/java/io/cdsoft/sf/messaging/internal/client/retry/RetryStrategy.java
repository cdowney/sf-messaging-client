package io.cdsoft.sf.messaging.internal.client.retry;

import io.cdsoft.sf.messaging.MessagingException;
import io.cdsoft.sf.messaging.api.config.ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RetryStrategy.class);
    private static final Long[] BACKOFF_MULTIPLIER = { 1L, 2L, 3L, 5L, 8L, 13L };
    private static final Long WAIT_MS = 1000L;

    private final ConnectionConfig connectionConfig;

    public RetryStrategy(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public synchronized <T> T exectue(Retryable<T> retryable) throws MessagingException {
        for (Integer retry = 0; retry <= connectionConfig.getMaxRetries(); ++retry) {
            try {
                Thread.sleep(retry * WAIT_MS * getBackoffMultiplier(retry));
                return retryable.run();
            } catch (InterruptedException e) {
                throw new MessagingException("Retry failed", e);
            } catch (Exception e) {
                LOG.warn("Retry failed, attempt: {} error: {}", retry, e);
            }
        }
        LOG.error("Max retries exceeded: {}", connectionConfig.getMaxRetries());
        throw new MessagingException("Max retries exceeded");
    }

    private Long getBackoffMultiplier(Integer retry) {
        if (retry >= BACKOFF_MULTIPLIER.length) {
            return BACKOFF_MULTIPLIER[BACKOFF_MULTIPLIER.length - 1];
        }
        return BACKOFF_MULTIPLIER[retry];
    }
}
