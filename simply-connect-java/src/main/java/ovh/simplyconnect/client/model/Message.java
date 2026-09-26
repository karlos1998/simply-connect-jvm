package ovh.simplyconnect.client.model;

import java.time.Instant;
import java.util.UUID;

/** A message summary returned by the External API. */
public record Message(
        UUID id,
        UUID conversationId,
        String direction,
        String channel,
        String remoteAddress,
        String body,
        Instant occurredAt,
        String status,
        SmsEndpoint endpoint) {}

