package io.cdsoft.sf.messaging.api.client;

import io.cdsoft.sf.messaging.MessagingException;
import io.cdsoft.sf.messaging.api.config.ConnectionConfig;
import io.cdsoft.sf.messaging.api.subscription.Subscription;
import io.cdsoft.sf.messaging.internal.client.auth.ManagedAuthClient;
import io.cdsoft.sf.messaging.internal.client.cometd.ManagedCometdClient;
import io.cdsoft.sf.messaging.internal.client.http.ManagedHttpClient;

public class MessagingClient {

    private final ManagedAuthClient authClient;
    private final ManagedHttpClient httpClient;
    private final ManagedCometdClient cometdClient;

    public MessagingClient(ConnectionConfig config) {
        this.httpClient = new ManagedHttpClient();
        this.authClient = new ManagedAuthClient(config, httpClient);
        this.cometdClient = new ManagedCometdClient(config, authClient, httpClient);
    }

    public void start() throws MessagingException {
        httpClient.doStart();
        authClient.doStart();
        cometdClient.doStart();
    }

    public void stop() throws MessagingException {
        cometdClient.doStop();
        authClient.doStop();
        httpClient.doStop();
    }

    public void addSubscription(Subscription subscription) /* throws MessagingException */ {
        cometdClient.addSubscription(subscription);
    }

    public void removeSubscriptions(Subscription subscription) throws MessagingException {
        cometdClient.removeSubscription(subscription.getChannelName());
    }
}
