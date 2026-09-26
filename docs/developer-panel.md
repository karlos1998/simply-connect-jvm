# Developer panel

The Spring Boot starter contains an optional, server-rendered developer cockpit. It is disabled by default and uses the same `SimplyConnectClient` bean as application code.

```yaml
simply-connect:
  api-key: ${SIMPLY_CONNECT_API_KEY}
  default-sms-endpoint-id: ${SIMPLY_CONNECT_SMS_ENDPOINT_ID}
  panel:
    enabled: true
    path: /simply-connect
    password: ${SIMPLY_CONNECT_PANEL_PASSWORD}
```

Open `/simply-connect` in the host application and sign in with the panel password. The password must contain at least 12 characters and must not equal the API key.

The panel can:

- list API-key-visible SMS and outbound voice endpoints;
- submit a test SMS with a fresh idempotency key;
- filter recent messages by phone, text, or endpoint;
- list published IVR flows and enqueue an outbound call;
- inspect the current outbound call queue;
- show equivalent Java, Kotlin, and Spring configuration examples.

Each feature degrades independently when the API key lacks its scope. The API key remains server-side and is never embedded in HTML.

## Security model

- The panel is opt-in and startup fails if its separate password is missing or weak.
- Authentication uses an application session with a 30-minute idle timeout.
- Mutating forms require a per-session CSRF token.
- Login attempts are rate limited per remote address.
- Panel responses disable caching, framing, MIME sniffing, referrers, and third-party content.
- Test SMS and queued calls are not retried automatically.

If the host application uses Spring Security, explicitly decide which authenticated users may reach the configured panel path. The starter does not weaken or replace host security rules.

Do not expose the panel publicly unless there is a concrete operational need. Prefer a VPN, private network, or an additional application-level access rule in production.

## Local demo

The repository includes a no-cost mock API. It never contacts a carrier:

```bash
./gradlew :examples:spring-boot-java:bootRun --args='--spring.profiles.active=demo'
```

Open `http://localhost:8080/simply-connect` and use `simply-connect-demo`.

