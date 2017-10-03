package io.cdsoft.sf.messaging.internal.client.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cdsoft.sf.messaging.MessagingException;
import io.cdsoft.sf.messaging.api.config.ConnectionConfig;
import io.cdsoft.sf.messaging.internal.client.ManagedClient;
import io.cdsoft.sf.messaging.internal.client.http.ManagedHttpClient;
import io.cdsoft.sf.messaging.internal.client.retry.RetryStrategy;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class ManagedAuthClient implements ManagedClient {

    private static final String COMETD_PATH = "/cometd/";
    private static final String OAUTH_TOKEN_PATH = "/services/oauth2/token";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ConnectionConfig config;
    private final HttpClient client;

    private AuthResponse authResponse;

    public ManagedAuthClient(ConnectionConfig config, ManagedHttpClient managedHttpClient) {
        this.config = config;
        this.client = managedHttpClient.getHttpClient();
    }

    @Override
    public void doStart() throws MessagingException {
        updateSalesforceToken();
    }

    public synchronized URL getCometdEndpoint() {
        try {
            return new URL(authResponse.getInstanceUrl() + COMETD_PATH + config.getApiVersion());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error building endpoint", e);
        }
    }

    public synchronized String getBearerToken() {
        return authResponse.getAccessToken();
    }

    private synchronized void updateSalesforceToken() {
        try {
            authResponse = new RetryStrategy(config).exectue(this::getAuthResponse);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to update token.", e);
        }
        if (config.getEnableKeepAlive()) {
            client.getScheduler().schedule(this::updateSalesforceToken, config.getKeepAliveMinutes(), TimeUnit.MINUTES);
        }
    }

    private synchronized AuthResponse getAuthResponse() throws MessagingException {

        try {
            ContentResponse response = client.newRequest(config.getLoginUrl() + OAUTH_TOKEN_PATH)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.asString())
                    .param("grant_type", "password")
                    .param("client_id", config.getClientId())
                    .param("client_secret", config.getClientSecret())
                    .param("username", config.getUserName())
                    .param("password", config.getPassword() + config.getSecurityToken())
                    .send();
            return objectMapper.readValue(response.getContentAsString(), AuthResponse.class);
        } catch (Throwable t) {
            throw new MessagingException("Failed to get AuthResponse from Salesforce", t);
        }
    }

}
