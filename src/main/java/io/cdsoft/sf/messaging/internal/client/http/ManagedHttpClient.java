package io.cdsoft.sf.messaging.internal.client.http;

import io.cdsoft.sf.messaging.MessagingException;
import io.cdsoft.sf.messaging.internal.client.ManagedClient;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class ManagedHttpClient implements ManagedClient {

    private HttpClient httpClient;

    public ManagedHttpClient() {
        httpClient = new HttpClient(new SslContextFactory());
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void doStart() throws MessagingException {
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new MessagingException("Unable to start HTTP Client", e);
        }
    }

    @Override
    public void doStop() throws MessagingException {
        try {
            httpClient.stop();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MessagingException("Unable to stop HTTP Client", e);
        }
    }
}
