package io.cdsoft.sf.messaging.internal.client.cometd;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSession.Extension.Adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReplayExtension extends Adapter {
    private static final String EXTENSION_NAME = "replay";
    private final ConcurrentMap<String, Long> dataMap = new ConcurrentHashMap<>();
    private final AtomicBoolean supported = new AtomicBoolean();

    public void addChannelReplayId(final String channelName, final long replayId) {
        dataMap.put(channelName, replayId);
    }

    @Override
    public boolean rcv(ClientSession session, Message.Mutable message) {
        final Object value = message.get(EXTENSION_NAME);

        final Long replayId;
        if (value instanceof Long) {
            replayId = (Long) value;
        } else if (value instanceof Number) {
            replayId = ((Number) value).longValue();
        } else {
            replayId = null;
        }

        if (this.supported.get() && replayId != null) {
            try {
                dataMap.put(message.getChannel(), replayId);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rcvMeta(ClientSession session, Message.Mutable message) {
        switch (message.getChannel()) {
        case Channel.META_HANDSHAKE:
            Map<String, Object> ext = message.getExt(false);
            this.supported.set(ext != null && Boolean.TRUE.equals(ext.get(EXTENSION_NAME)));
            break;
        default:
            break;
        }
        return true;
    }

    @Override
    public boolean sendMeta(ClientSession session, Message.Mutable message) {
        switch (message.getChannel()) {
        case Channel.META_HANDSHAKE:
            message.getExt(true).put(EXTENSION_NAME, Boolean.TRUE);
            break;
        case Channel.META_SUBSCRIBE:
            if (supported.get()) {
                message.getExt(true).put(EXTENSION_NAME, dataMap);
            }
            break;
        default:
            break;
        }
        return true;
    }
}
