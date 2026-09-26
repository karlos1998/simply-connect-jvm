package ovh.simplyconnect.client.model;

import java.util.Objects;
import java.util.UUID;

/** A request to enqueue one SMS. */
public record SmsSendRequest(UUID endpointId, String to, String body) {
    public SmsSendRequest {
        Objects.requireNonNull(endpointId, "endpointId");
        requireText(to, "to", 32);
        requireText(body, "body", 5000);
    }

    private static void requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
    }
}

