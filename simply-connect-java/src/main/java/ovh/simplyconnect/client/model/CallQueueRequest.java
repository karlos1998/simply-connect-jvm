package ovh.simplyconnect.client.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** A request to enqueue one idempotent outbound IVR call. */
public record CallQueueRequest(
        UUID requestId,
        UUID endpointId,
        UUID flowVersionId,
        String destination,
        LocalDateTime scheduledFor,
        String timeZone,
        int intervalSeconds) {

    public CallQueueRequest {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(endpointId, "endpointId");
        Objects.requireNonNull(flowVersionId, "flowVersionId");
        if (destination == null || destination.isBlank() || destination.length() > 32) {
            throw new IllegalArgumentException("destination must contain 1 to 32 characters");
        }
        if (timeZone == null || timeZone.isBlank() || timeZone.length() > 80) {
            throw new IllegalArgumentException("timeZone must contain 1 to 80 characters");
        }
        if (intervalSeconds < 0 || intervalSeconds > 3600) {
            throw new IllegalArgumentException("intervalSeconds must be between 0 and 3600");
        }
    }
}

