package ovh.simplyconnect.client.model;

import java.time.Instant;
import java.util.UUID;

/** A published IVR flow that can be placed in the call queue. */
public record PublishedCallFlow(
        UUID id,
        String name,
        String description,
        UUID publishedVersionId,
        Instant updatedAt) {}

