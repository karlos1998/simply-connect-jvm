package example;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
@RequestMapping("/mock/api/v1/external")
final class MockSimplyConnectApi {
    private static final UUID SMS_ENDPOINT = UUID.fromString("01994f44-755a-7d10-bb59-597ac6123d50");
    private static final UUID VOICE_ENDPOINT = UUID.fromString("01994f44-755a-7d10-bb59-597ac6123d70");
    private static final UUID FLOW_VERSION = UUID.fromString("01994f44-755a-7d10-bb59-597ac6123d80");

    @GetMapping("/endpoints")
    List<Map<String, Object>> endpoints() {
        return List.of(Map.of(
                "id", SMS_ENDPOINT,
                "name", "Demo Android Gateway",
                "phoneNumber", "+48500100200",
                "enabled", true));
    }

    @PostMapping("/messages")
    ResponseEntity<Map<String, Object>> sendSms(@RequestBody Map<String, Object> ignored) {
        return ResponseEntity.accepted().body(Map.of(
                "messageId", UUID.randomUUID(),
                "dispatchId", UUID.randomUUID(),
                "commandId", UUID.randomUUID(),
                "status", "QUEUED"));
    }

    @GetMapping("/messages")
    Map<String, Object> messages() {
        return Map.of(
                "items", List.of(Map.of(
                        "id", UUID.randomUUID(),
                        "conversationId", UUID.randomUUID(),
                        "direction", "INBOUND",
                        "channel", "SMS",
                        "remoteAddress", "+48500999888",
                        "body", "Dzień dobry, czy zamówienie jest już gotowe?",
                        "occurredAt", Instant.now().minusSeconds(180),
                        "status", "RECEIVED",
                        "endpoint", Map.of(
                                "id", SMS_ENDPOINT,
                                "name", "Demo Android Gateway",
                                "phoneNumber", "+48500100200",
                                "enabled", true))),
                "page", 0,
                "size", 25,
                "totalElements", 1,
                "totalPages", 1);
    }

    @GetMapping("/call-queue/endpoints")
    List<Map<String, Object>> callEndpoints() {
        return List.of(Map.of(
                "id", VOICE_ENDPOINT,
                "name", "Demo USB modem",
                "phoneNumber", "+48500100300",
                "gatewayId", UUID.fromString("01994f44-755a-7d10-bb59-597ac6123d71"),
                "gatewayName", "Desk modem",
                "gatewayStatus", "ONLINE"));
    }

    @GetMapping("/call-queue/flows")
    List<Map<String, Object>> flows() {
        return List.of(Map.of(
                "id", UUID.fromString("01994f44-755a-7d10-bb59-597ac6123d81"),
                "name", "Order confirmation",
                "description", "Asks the recipient to confirm with DTMF",
                "publishedVersionId", FLOW_VERSION,
                "updatedAt", Instant.now()));
    }

    @GetMapping("/call-queue")
    Map<String, Object> queue() {
        return Map.of("items", List.of(), "page", 0, "totalElements", 0, "totalPages", 0);
    }

    @PostMapping("/call-queue")
    ResponseEntity<Map<String, Object>> enqueueCall(@RequestBody Map<String, Object> request) {
        Instant now = Instant.now();
        return ResponseEntity.accepted().body(Map.ofEntries(
                Map.entry("id", UUID.randomUUID()),
                Map.entry("endpointId", request.get("endpointId")),
                Map.entry("flowVersionId", request.get("flowVersionId")),
                Map.entry("flowName", "Order confirmation"),
                Map.entry("flowVersion", 1),
                Map.entry("destination", request.get("destination")),
                Map.entry("source", "EXTERNAL_API"),
                Map.entry("status", "PENDING"),
                Map.entry("notBefore", now),
                Map.entry("timeZone", request.get("timeZone")),
                Map.entry("intervalSeconds", request.get("intervalSeconds")),
                Map.entry("createdAt", now),
                Map.entry("updatedAt", now)));
    }
}

