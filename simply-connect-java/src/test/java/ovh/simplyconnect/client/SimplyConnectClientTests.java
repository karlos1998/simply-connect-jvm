package ovh.simplyconnect.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ovh.simplyconnect.client.model.MessageQuery;
import ovh.simplyconnect.client.model.SmsSendRequest;

class SimplyConnectClientTests {
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void listsEndpointsWithApiKey() {
        AtomicReference<String> apiKey = new AtomicReference<>();
        server.createContext("/api/v1/external/endpoints", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            respond(exchange, 200, """
                    [{"id":"01994f44-755a-7d10-bb59-597ac6123d50","name":"Support","phoneNumber":"+48500100200","enabled":true}]
                    """);
        });

        var client = client();
        var endpoints = client.listSmsEndpoints();

        assertEquals("msc_live_test", apiKey.get());
        assertEquals(1, endpoints.size());
        assertEquals("Support", endpoints.get(0).name());
    }

    @Test
    void sendsIdempotentSmsWithoutAutomaticRetry() {
        UUID endpointId = UUID.fromString("01994f44-755a-7d10-bb59-597ac6123d50");
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        server.createContext("/api/v1/external/messages", exchange -> {
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            respond(exchange, 202, """
                    {"messageId":"01994f44-755a-7d10-bb59-597ac6123d5f","dispatchId":"01994f44-755a-7d10-bb59-597ac6123d60","commandId":"01994f44-755a-7d10-bb59-597ac6123d61","status":"QUEUED"}
                    """);
        });

        var result = client().sendSms(new SmsSendRequest(endpointId, "+48500100200", "Hello"), "order-42");

        assertEquals("order-42", idempotencyKey.get());
        assertEquals("QUEUED", result.status());
    }

    @Test
    void buildsEncodedMessageFilters() {
        AtomicReference<String> query = new AtomicReference<>();
        server.createContext("/api/v1/external/messages", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, """
                    {"items":[],"page":0,"size":25,"totalElements":0,"totalPages":0}
                    """);
        });

        client().listMessages(new MessageQuery("+48 500", "INBOUND", null, null, null, null, 0, 25));

        assertEquals("query=%2B48%20500&direction=INBOUND&page=0&size=25", query.get());
    }

    @Test
    void exposesProblemDetailsAndCorrelationId() {
        server.createContext("/api/v1/external/endpoints", exchange -> {
            exchange.getResponseHeaders().add("X-Correlation-Id", "corr-123");
            respond(exchange, 401, "{\"title\":\"Unauthorized\",\"detail\":\"API key is invalid\"}");
        });

        SimplyConnectException failure = assertThrows(SimplyConnectException.class, () -> client().listSmsEndpoints());

        assertEquals(401, failure.statusCode());
        assertEquals("corr-123", failure.correlationId());
        assertEquals("Simply Connect returned HTTP 401: API key is invalid", failure.getMessage());
    }

    private SimplyConnectClient client() {
        return SimplyConnectClient.builder().baseUrl(baseUrl).apiKey("msc_live_test").build();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
