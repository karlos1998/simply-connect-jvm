package ovh.simplyconnect.spring;

import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the Simply Connect client and optional developer panel. */
@ConfigurationProperties("simply-connect")
public class SimplyConnectProperties {
    private String baseUrl = "https://api.simply-connect.ovh";
    private String apiKey;
    private UUID defaultSmsEndpointId;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration requestTimeout = Duration.ofSeconds(15);
    private final Panel panel = new Panel();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public UUID getDefaultSmsEndpointId() { return defaultSmsEndpointId; }
    public void setDefaultSmsEndpointId(UUID defaultSmsEndpointId) { this.defaultSmsEndpointId = defaultSmsEndpointId; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    public Panel getPanel() { return panel; }

    public static class Panel {
        private boolean enabled;
        private String path = "/simply-connect";
        private String password;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String normalizedPath() {
            String value = path == null || path.isBlank() ? "/simply-connect" : path.trim();
            if (!value.startsWith("/")) value = "/" + value;
            while (value.length() > 1 && value.endsWith("/")) value = value.substring(0, value.length() - 1);
            return value;
        }
    }
}

