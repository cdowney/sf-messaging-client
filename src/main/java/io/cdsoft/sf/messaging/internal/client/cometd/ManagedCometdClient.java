package io.cdsoft.sf.messaging.internal.client.cometd;

import io.cdsoft.sf.messaging.MessagingException;
import io.cdsoft.sf.messaging.api.config.ConnectionConfig;
import io.cdsoft.sf.messaging.api.consumer.EventConsumer;
import io.cdsoft.sf.messaging.api.consumer.JacksonPlatformEventConsumer;
import io.cdsoft.sf.messaging.api.consumer.JacksonPushTopicEventConsumer;
import io.cdsoft.sf.messaging.api.consumer.JsonEventConsumer;
import io.cdsoft.sf.messaging.api.consumer.MapEventConsumer;
import io.cdsoft.sf.messaging.api.subscription.Subscription;
import io.cdsoft.sf.messaging.internal.client.ManagedClient;
import io.cdsoft.sf.messaging.internal.client.auth.ManagedAuthClient;
import io.cdsoft.sf.messaging.internal.client.http.ManagedHttpClient;
import io.cdsoft.sf.messaging.internal.client.retry.RetryStrategy;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.cometd.bayeux.Channel.META_CONNECT;
import static org.cometd.bayeux.Channel.META_DISCONNECT;
import static org.cometd.bayeux.Channel.META_HANDSHAKE;

public class ManagedCometdClient implements ManagedClient {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedCometdClient.class);

    private static final Long CONNECT_TIMEOUT = 30L;
    private static final TimeUnit TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;
    private static final Integer MAX_NETWORK_DELAY = 15000;
    private static final Integer MAX_BUFFER_SIZE = 1048576;
    private static final ReplayExtension REPLAY_EXTENSION = new ReplayExtension();

    private final ConnectionConfig config;
    private final ManagedAuthClient authClient;
    private final ManagedHttpClient httpClient;
    private final ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private BayeuxClient bayeuxClient;

    private ClientSessionChannel.MessageListener handshakeListener = (channel, message) -> {
        LOG.debug("Meta-message [{}]: {}", channel.getChannelId(), message);
        if (!message.isSuccessful()) {
            restartClient();
        }
    };

    private ClientSessionChannel.MessageListener connectListener = (channel, message) -> {
        LOG.debug("Meta-message [{}]: {}", channel.getChannelId(), message);
        if (message.isSuccessful()) {
            resubscribe();
        }
    };

    private ClientSessionChannel.MessageListener disconnectListener = (channel, message) -> {
        LOG.debug("Meta-message [{}]: {}", channel.getChannelId(), message);
        restartClient();
    };

    public ManagedCometdClient(ConnectionConfig config, ManagedAuthClient authClient, ManagedHttpClient httpClient) {
        this.config = config;
        this.authClient = authClient;
        this.httpClient = httpClient;
    }

    @Override
    public void doStart() throws MessagingException {
        new RetryStrategy(config).exectue(this::connect);
    }

    @Override
    public void doStop() throws MessagingException {
        disconnect();
    }

    public void addSubscription(Subscription subscription) {
        if (bayeuxClient.isDisconnected()) {
            throw new IllegalStateException(
                    String.format("Connector[%s] has not been started", authClient.getCometdEndpoint()));
        }

        LOG.info("Subscribing to channel: {}", subscription.getChannelName());
        REPLAY_EXTENSION.addOrUpdateChannelReplayId(subscription.getTopic(), subscription.getReplayFrom());

        ClientSessionChannel channel = bayeuxClient.getChannel(subscription.getChannelName());
        EventConsumer<?> consumer = subscription.getConsumer();

        channel.subscribe(
                // On message listener
                (c, message) -> {
                    LOG.trace("Subscription-message [{}]: {}", c.getChannelId(), message);
                    if (consumer instanceof JsonEventConsumer) {
                        ((JsonEventConsumer) consumer).accept(message.getJSON());
                    } else if (consumer instanceof MapEventConsumer) {
                        ((MapEventConsumer) consumer).accept(message.getDataAsMap());
                    } else if (consumer instanceof JacksonPlatformEventConsumer) {
                        ((JacksonPlatformEventConsumer) consumer).accept(message.getJSON());
                    } else if (consumer instanceof JacksonPushTopicEventConsumer) {
                        ((JacksonPushTopicEventConsumer) consumer).accept(message.getJSON());
                    }
                },
                // On subscription listener
                (c, message) -> {
                    if (message.isSuccessful()) {
                        LOG.info("Successfully Subscribed to channel: {} {}", subscription.getChannelName(), c.getChannelId());
                        subscriptions.put(subscription.getChannelName(), subscription);
                    } else {
                        LOG.error("Unable to subscribe to subscription {} : {}", subscription, message);
                    }
                });
    }

    public void removeSubscription(String name) {
        if (bayeuxClient.isDisconnected()) {
            throw new IllegalStateException(
                    String.format("Connector[%s] has not been started", authClient.getCometdEndpoint()));
        }

        // TODO: remove from REPLAY_EXTENSIONS?

        Subscription subscription = subscriptions.get(name);

        if (subscription == null) {
            LOG.warn("Subscription with name {} does not exist", name);
        } else {
            bayeuxClient.getChannel(subscription.getChannelName()).unsubscribe();
        }
    }

    private BayeuxClient createClient() {
        Map<String, Object> longPollingOptions = new HashMap<String, Object>() {
            {
                put(ClientTransport.MAX_NETWORK_DELAY_OPTION, MAX_NETWORK_DELAY);
                put(ClientTransport.MAX_MESSAGE_SIZE_OPTION, MAX_BUFFER_SIZE);
            }
        };

        LongPollingTransport transport = new LongPollingTransport(longPollingOptions, httpClient.getHttpClient()) {
            @Override
            protected void customize(Request request) {
                super.customize(request);
                request.getHeaders().put(HttpHeader.AUTHORIZATION, "OAuth " + authClient.getBearerToken());
            }
        };

        BayeuxClient client = new BayeuxClient(authClient.getCometdEndpoint().toExternalForm(), transport);

        // added eagerly to check for support during handshake
        client.addExtension(REPLAY_EXTENSION);

        return client;
    }

    private Boolean connect() throws MessagingException {
        subscriptions.clear();

        bayeuxClient = createClient();

        bayeuxClient.getChannel(META_HANDSHAKE).addListener(handshakeListener);
        bayeuxClient.getChannel(META_CONNECT).addListener(connectListener);
        bayeuxClient.getChannel(META_DISCONNECT).addListener(disconnectListener);

        bayeuxClient.handshake();
        if (!bayeuxClient.waitFor(MILLISECONDS.convert(CONNECT_TIMEOUT, TIMEOUT_TIME_UNIT), BayeuxClient.State.CONNECTED)) {
            throw new MessagingException("Timeout connecting to " + authClient.getCometdEndpoint());
        }
        return true;
    }

    private void disconnect() {
        bayeuxClient.getChannel(META_HANDSHAKE).removeListener(handshakeListener);
        bayeuxClient.getChannel(META_CONNECT).removeListener(connectListener);
        bayeuxClient.getChannel(META_DISCONNECT).removeListener(disconnectListener);

        bayeuxClient.disconnect();

        bayeuxClient = null;
    }

    private synchronized void restartClient() {
        disconnect();
        try {
            new RetryStrategy(config).exectue(this::connect);
        } catch (Exception e) {
            LOG.error("Unable to restart client: {}", e);
            throw new RuntimeException("Unable to restart client.", e);
        }
    }

    private synchronized void resubscribe() {
        LOG.trace("Refreshing subscriptions to channels on reconnect");

        for (Map.Entry<String, Subscription> entry : subscriptions.entrySet()) {
            Subscription subscription = entry.getValue();
            ClientSessionChannel channel = bayeuxClient.getChannel(subscription.getChannelName());
            if (channel.getSubscribers().size() == 0) {
                LOG.debug("Re-subscribing to channel: [{}] because no subscribers exist.", subscription.getChannelName());
                addSubscription(subscription);
            }
        }
    }

}
