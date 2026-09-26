package ovh.simplyconnect.client.model;

import java.time.Instant;

/** One item in a message delivery-status timeline. */
public record StatusEvent(
        String status,
        String source,
        String eventType,
        String errorCode,
        Instant occurredAt,
        Instant recordedAt) {}

