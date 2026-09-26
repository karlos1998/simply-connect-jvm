package ovh.simplyconnect.client.model;

import java.time.Instant;
import java.util.UUID;

/** One persistent outbound IVR call queue entry. */
public record CallQueueItem(
        UUID id,
        UUID endpointId,
        UUID flowVersionId,
        String flowName,
        int flowVersion,
        String destination,
        String source,
        String status,
        Instant notBefore,
        String timeZone,
        int intervalSeconds,
        Instant createdAt,
        Instant updatedAt,
        UUID callId,
        String reason) {}

