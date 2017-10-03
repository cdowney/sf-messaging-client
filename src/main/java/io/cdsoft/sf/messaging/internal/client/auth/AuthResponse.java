package io.cdsoft.sf.messaging.internal.client.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("instance_url")
    private String instanceUrl;

    @JsonProperty("issued_at")
    private Long issuedAt;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("token_type")
    private String tokenType;
}
