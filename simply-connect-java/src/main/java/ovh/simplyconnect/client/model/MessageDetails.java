package ovh.simplyconnect.client.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Message details and delivery history. */
public record MessageDetails(
        UUID id,
        UUID conversationId,
        String direction,
        String channel,
        String remoteAddress,
        String body,
        Instant occurredAt,
        Instant createdAt,
        String status,
        SmsEndpoint endpoint,
        List<StatusEvent> statusHistory) {
    public MessageDetails {
        statusHistory = statusHistory == null ? List.of() : List.copyOf(statusHistory);
    }
}

