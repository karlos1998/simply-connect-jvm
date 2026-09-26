package ovh.simplyconnect.client.model;

import java.util.UUID;

/** An outbound voice endpoint visible to the API key. */
public record CallQueueEndpoint(
        UUID id,
        String name,
        String phoneNumber,
        UUID gatewayId,
        String gatewayName,
        String gatewayStatus) {}

