# Salesforce Messaging Client

Salesforce Messaging Client, is a java client used to subscribe to enterprise messages (Platform Events & Streaming API Push Topics).

##### Reference Links:

[Platform Events Developer Guide](https://developer.salesforce.com/docs/atlas.en-us.platform_events.meta/platform_events/platform_events_intro.htm)

[Platform Events Trailhead](https://trailhead.salesforce.com/modules/platform_events_basics)

[Streaming API Developer Guide](https://developer.salesforce.com/docs/atlas.en-us.api_streaming.meta/api_streaming/code_sample_auth_oauth.htm)


## Getting Started

To include the client in your maven project, add the following dependency to your `pom.xml` file:
```
<dependency>
   <groupId>io.cdsoft</groupId>
   <artifactId>sf-messaging-client</artifactId>
   <version>1.0.1</version>
</dependency>
```

### Prerequisites

You will need a Salesforce sandbox. [Get a free account](https://developer.salesforce.com/signup)


### Configuration

To configure the client use the `ConnectionConfig`.

```java
public class ConnectionConfig {

    private String loginUrl = "https://login.salesforce.com";  // Salesforce login URL https://login.salesforce.com or https://test.salesforce.com or your custom domain name
    private String clientId;                                   // Salesforce OAuth connected app client ID
    private String clientSecret;                               // Salesforce OAuth connected app client secret
    private String userName;                                   // Salesforce username
    private String password;                                   // Salesforce password
    private String securityToken;                              // Salesforce security token for username
    private String apiVersion = "40.0";                        // Salesforce API version
    private Boolean enableKeepAlive = true;                    // This will periodically login to extend the Salesforce session
    private Long keepAliveMinutes = 60L;                       // Number of minutes between keep alive   logins
    private Long maxRetries = 3L;                              // Number of retries to login/connect when an exception/error occurs

}
```

## Usage

To use the client:

```java
import io.cdsoft.sf.messaging.api.client.MessagingClient;
import io.cdsoft.sf.messaging.api.config.ConnectionConfig;
import io.cdsoft.sf.messaging.api.consumer.JsonEventConsumer;
import io.cdsoft.sf.messaging.api.subscription.PlatformEventSubscription;
import io.cdsoft.sf.messaging.api.subscription.Subscription;

public class Client {

    public static void main(String[] args) throws Exception {

        // Set up ConnectionConfig instance with your values
        ConnectionConfig config = new ConnectionConfig();
        config.setLoginUrl("");
        config.setClientId("");
        config.setClientSecret("");
        config.setUserName("");
        config.setPassword("");
        config.setSecurityToken("");

        // Create an instance of the MessagingClient
        MessagingClient client = new MessagingClient(config);

        // Start the client
        client.start();

        // Create a message consumer
        JsonEventConsumer consumer = message -> System.out.println("Received message: " + message);

        // Create a Platform Event Subscription
        // Param 1: The paltform event API name
        // Param 2: Replay events from (-2 = earliest, -1 = tip, or specific event ID)
        // Param 2: An instance of EventConsumer (Either JsonEventConsumer or MapEventConsumer)
        Subscription subscription = new PlatformEventSubscription("Some_Event__e", -1L, consumer);

        // Subscribe to the event
        client.addSubscription(subscription);
    }
}

```

## Built With

* [Maven](https://maven.apache.org/)
* [CometD](https://cometd.org/)


## Versioning

Usse [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/cdowney/sf-messaging-client/tags). 


## License

This project is licensed under the Apache 2.0 License - see the [LICENSE.md](LICENSE.md) file for details.
