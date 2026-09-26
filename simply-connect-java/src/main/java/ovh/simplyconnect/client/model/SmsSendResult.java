package ovh.simplyconnect.client.model;

import java.util.UUID;

/** Durable identifiers returned after an SMS is accepted. */
public record SmsSendResult(UUID messageId, UUID dispatchId, UUID commandId, String status) {}

