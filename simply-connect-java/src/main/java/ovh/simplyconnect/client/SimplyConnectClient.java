package ovh.simplyconnect.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import ovh.simplyconnect.client.model.CallQueueEndpoint;
import ovh.simplyconnect.client.model.CallQueueItem;
import ovh.simplyconnect.client.model.CallQueuePage;
import ovh.simplyconnect.client.model.CallQueueQuery;
import ovh.simplyconnect.client.model.CallQueueRequest;
import ovh.simplyconnect.client.model.MessageDetails;
import ovh.simplyconnect.client.model.MessagePage;
import ovh.simplyconnect.client.model.MessageQuery;
import ovh.simplyconnect.client.model.PublishedCallFlow;
import ovh.simplyconnect.client.model.SmsEndpoint;
import ovh.simplyconnect.client.model.SmsSendRequest;
import ovh.simplyconnect.client.model.SmsSendResult;

/**
 * Synchronous Java 17 client for the Simply Connect External API.
 *
 * <p>The client is immutable and thread-safe. Mutating operations are never retried automatically;
 * callers retain control of their idempotency keys and request identifiers.
 */
public final class SimplyConnectClient {
    private static final TypeReference<List<SmsEndpoint>> SMS_ENDPOINTS = new TypeReference<>() {};
    private static final TypeReference<List<CallQueueEndpoint>> CALL_ENDPOINTS = new TypeReference<>() {};
    private static final TypeReference<List<PublishedCallFlow>> CALL_FLOWS = new TypeReference<>() {};

    private final URI externalApiBase;
    private final String apiKey;
    private final UUID defaultSmsEndpointId;
    private final Duration requestTimeout;
    private final HttpClient http;
    private final ObjectMapper json;

    private SimplyConnectClient(Builder builder) {
        this.externalApiBase = externalApiBase(builder.baseUrl);
        this.apiKey = requireSecret(builder.apiKey);
        this.defaultSmsEndpointId = builder.defaultSmsEndpointId;
        this.requestTimeout = Objects.requireNonNull(builder.requestTimeout, "requestTimeout");
        this.http = builder.httpClient == null
                ? HttpClient.newBuilder().connectTimeout(builder.connectTimeout).build()
                : builder.httpClient;
        this.json = builder.objectMapper == null ? defaultObjectMapper() : builder.objectMapper.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<SmsEndpoint> listSmsEndpoints() {
        return get("endpoints", SMS_ENDPOINTS);
    }

    public SmsSendResult sendSms(SmsSendRequest request, String idempotencyKey) {
        Objects.requireNonNull(request, "request");
        return post("messages", request, SmsSendResult.class, idempotencyKey);
    }

    public SmsSendResult sendSms(String to, String body, String idempotencyKey) {
        if (defaultSmsEndpointId == null) {
            throw new IllegalStateException("No default SMS endpoint is configured");
        }
        return sendSms(new SmsSendRequest(defaultSmsEndpointId, to, body), idempotencyKey);
    }

    public MessagePage listMessages(MessageQuery query) {
        Objects.requireNonNull(query, "query");
        QueryString parameters = new QueryString()
                .add("query", query.query())
                .add("direction", query.direction())
                .add("status", query.status())
                .add("endpointId", query.endpointId())
                .add("occurredFrom", query.occurredFrom())
                .add("occurredTo", query.occurredTo())
                .add("page", query.page())
                .add("size", query.size());
        return get("messages" + parameters, MessagePage.class);
    }

    public MessageDetails getMessage(UUID messageId) {
        Objects.requireNonNull(messageId, "messageId");
        return get("messages/" + messageId, MessageDetails.class);
    }

    public List<CallQueueEndpoint> listCallQueueEndpoints() {
        return get("call-queue/endpoints", CALL_ENDPOINTS);
    }

    public List<PublishedCallFlow> listPublishedCallFlows() {
        return get("call-queue/flows", CALL_FLOWS);
    }

    public CallQueuePage listCallQueue(CallQueueQuery query) {
        Objects.requireNonNull(query, "query");
        QueryString parameters = new QueryString()
                .add("endpointId", query.endpointId())
                .add("status", query.status())
                .add("source", query.source())
                .add("search", query.search())
                .add("from", query.from())
                .add("to", query.to())
                .add("page", query.page())
                .add("size", query.size())
                .add("sort", query.sort())
                .add("descending", query.descending());
        return get("call-queue" + parameters, CallQueuePage.class);
    }

    public CallQueueItem createCallQueueEntry(CallQueueRequest request) {
        Objects.requireNonNull(request, "request");
        return post("call-queue", request, CallQueueItem.class, null);
    }

    private <T> T get(String path, Class<T> responseType) {
        return execute(request(path).GET().build(), responseType);
    }

    private <T> T get(String path, TypeReference<T> responseType) {
        HttpResponse<String> response = send(request(path).GET().build());
        ensureSuccess(response);
        try {
            return json.readValue(response.body(), responseType);
        } catch (JsonProcessingException failure) {
            throw new SimplyConnectException("Simply Connect returned an unreadable response", failure);
        }
    }

    private <T> T post(String path, Object body, Class<T> responseType, String idempotencyKey) {
        HttpRequest.Builder request = request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(write(body)));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return execute(request.build(), responseType);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(externalApiBase.resolve(path))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("X-API-Key", apiKey)
                .header("User-Agent", "simply-connect-java/0.1");
    }

    private <T> T execute(HttpRequest request, Class<T> responseType) {
        HttpResponse<String> response = send(request);
        ensureSuccess(response);
        try {
            return json.readValue(response.body(), responseType);
        } catch (JsonProcessingException failure) {
            throw new SimplyConnectException("Simply Connect returned an unreadable response", failure);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new SimplyConnectException("Interrupted while calling Simply Connect", failure);
        } catch (IOException failure) {
            throw new SimplyConnectException("Could not reach Simply Connect", failure);
        }
    }

    private void ensureSuccess(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) return;
        String correlationId = response.headers().firstValue("X-Correlation-Id").orElse(null);
        String detail = problemDetail(response.body());
        throw new SimplyConnectException(
                "Simply Connect returned HTTP " + response.statusCode() + (detail == null ? "" : ": " + detail),
                response.statusCode(),
                correlationId,
                response.body());
    }

    private String problemDetail(String body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode detail = root.get("detail");
            return detail == null || detail.isNull() ? null : detail.asText();
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String write(Object body) {
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("Request cannot be serialized", failure);
        }
    }

    private static URI externalApiBase(String baseUrl) {
        String value = Objects.requireNonNull(baseUrl, "baseUrl").trim();
        if (!(value.startsWith("https://") || value.startsWith("http://"))) {
            throw new IllegalArgumentException("baseUrl must use http or https");
        }
        value = value.replaceAll("/+$", "");
        if (!value.endsWith("/api/v1/external")) {
            value += "/api/v1/external";
        }
        return URI.create(value + "/");
    }

    private static String requireSecret(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("apiKey must not be blank");
        return value;
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static final class Builder {
        private String baseUrl = "https://api.simply-connect.ovh";
        private String apiKey;
        private UUID defaultSmsEndpointId;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(15);
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

        private Builder() {}

        public Builder baseUrl(String value) {
            this.baseUrl = value;
            return this;
        }

        public Builder apiKey(String value) {
            this.apiKey = value;
            return this;
        }

        public Builder defaultSmsEndpointId(UUID value) {
            this.defaultSmsEndpointId = value;
            return this;
        }

        public Builder connectTimeout(Duration value) {
            this.connectTimeout = value;
            return this;
        }

        public Builder requestTimeout(Duration value) {
            this.requestTimeout = value;
            return this;
        }

        public Builder httpClient(HttpClient value) {
            this.httpClient = value;
            return this;
        }

        public Builder objectMapper(ObjectMapper value) {
            this.objectMapper = value;
            return this;
        }

        public SimplyConnectClient build() {
            return new SimplyConnectClient(this);
        }
    }

    private static final class QueryString {
        private final List<String> values = new ArrayList<>();

        QueryString add(String name, Object value) {
            if (value != null && (!(value instanceof String text) || !text.isBlank())) {
                values.add(encode(name) + "=" + encode(value.toString()));
            }
            return this;
        }

        @Override
        public String toString() {
            return values.isEmpty() ? "" : "?" + String.join("&", values);
        }

        private static String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }
}

