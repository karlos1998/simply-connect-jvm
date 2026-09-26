package ovh.simplyconnect.client.model;

import java.time.Instant;
import java.util.UUID;

/** Optional filters for the outbound call queue. */
public record CallQueueQuery(
        UUID endpointId,
        String status,
        String source,
        String search,
        Instant from,
        Instant to,
        int page,
        int size,
        String sort,
        boolean descending) {

    public CallQueueQuery {
        if (page < 0) throw new IllegalArgumentException("page must be at least 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        sort = sort == null || sort.isBlank() ? "notBefore" : sort;
    }

    public static CallQueueQuery firstPage() {
        return new CallQueueQuery(null, null, null, null, null, null, 0, 25, "notBefore", false);
    }
}

