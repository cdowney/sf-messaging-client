package io.cdsoft.sf.messaging.api.config;

import lombok.Data;

@Data
public class ConnectionConfig {

    private String loginUrl = "https://login.salesforce.com";
    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;
    private String securityToken;
    private String apiVersion = "40.0";
    private Boolean enableKeepAlive = true;
    private Long keepAliveMinutes = 60L;
    private Long maxRetries = 3L;

}
