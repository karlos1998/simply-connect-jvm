package ovh.simplyconnect.client.model;

import java.util.List;

/** One page of queued calls. */
public record CallQueuePage(List<CallQueueItem> items, long totalElements, int totalPages, int page) {
    public CallQueuePage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

