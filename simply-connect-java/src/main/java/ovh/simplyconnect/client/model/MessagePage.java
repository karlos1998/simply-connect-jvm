package ovh.simplyconnect.client.model;

import java.util.List;

/** One page of messages. */
public record MessagePage(List<Message> items, int page, int size, long totalElements, int totalPages) {
    public MessagePage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

