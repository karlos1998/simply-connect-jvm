package ovh.simplyconnect.client.model;

import java.time.Instant;
import java.util.UUID;

/** Optional filters for message history. */
public record MessageQuery(
        String query,
        String direction,
        String status,
        UUID endpointId,
        Instant occurredFrom,
        Instant occurredTo,
        int page,
        int size) {

    public MessageQuery {
        if (page < 0) throw new IllegalArgumentException("page must be at least 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
    }

    public static MessageQuery firstPage() {
        return new MessageQuery(null, null, null, null, null, null, 0, 25);
    }
}

