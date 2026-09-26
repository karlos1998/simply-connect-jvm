package ovh.simplyconnect.client.model;

import java.util.UUID;

/** An SMS endpoint visible to the API key. */
public record SmsEndpoint(UUID id, String name, String phoneNumber, boolean enabled) {}

