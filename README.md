# Simply Connect JVM

Official Java 17 client and Spring Boot starter for the [Simply Connect External API](https://api.simply-connect.ovh/api/external/docs). Kotlin uses the same null-safe records and fluent builder without a separate client.

## Modules

- `ovh.simplyconnect:simply-connect-java` — dependency-light Java client.
- `ovh.simplyconnect:simply-connect-spring-boot-starter` — auto-configuration plus an optional developer panel.

## Java

```java
var client = SimplyConnectClient.builder()
    .apiKey(System.getenv("SIMPLY_CONNECT_API_KEY"))
    .defaultSmsEndpointId(UUID.fromString(System.getenv("SIMPLY_CONNECT_SMS_ENDPOINT_ID")))
    .build();

var result = client.sendSms(
    "+48500100200",
    "Your order is ready.",
    "order-1842-ready"
);
```

Mutating operations are never retried automatically. Reuse an SMS idempotency key only for the same logical request. Call queue submissions carry their own `requestId`.

## Spring Boot

Add JitPack once to dependency resolution:

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then install the starter from the public repository — no GitHub token is required:

```gradle
implementation("com.github.karlos1998.simply-connect-jvm:simply-connect-spring-boot-starter:v0.1.0")
```

```yaml
simply-connect:
  api-key: ${SIMPLY_CONNECT_API_KEY}
  default-sms-endpoint-id: ${SIMPLY_CONNECT_SMS_ENDPOINT_ID}
```

The starter provides a `SimplyConnectClient` bean. See [the developer panel guide](docs/developer-panel.md) for the opt-in cockpit with endpoint discovery, test SMS, message history, IVR flow discovery, and the outbound call queue.

For applications without Spring Boot, install the core module instead:

```gradle
implementation("com.github.karlos1998.simply-connect-jvm:simply-connect-java:v0.1.0")
```

Every release is also published to GitHub Packages under the `ovh.simplyconnect` group for teams that already authenticate Maven against GitHub.

## API-key scopes

| Feature | Scope |
| --- | --- |
| SMS endpoint discovery and send | `SMS_SEND` |
| Message history and delivery details | `MESSAGES_READ` |
| Call queue listing | `CALL_QUEUE_READ` |
| Voice endpoint/flow discovery and queue submission | `CALL_QUEUE_WRITE` |

Endpoint restrictions configured in Simply Connect are respected by every SDK and panel operation.

## Build and local demo

```bash
./gradlew check
./gradlew :examples:spring-boot-java:bootRun --args='--spring.profiles.active=demo'
```

The demo uses an in-process mock API and cannot send a real SMS or place a call.

## Compatibility

- Java 17+
- Spring Boot 3.5 and 4.x (starter)
- Kotlin/JVM targeting Java 17+

## License

MIT
