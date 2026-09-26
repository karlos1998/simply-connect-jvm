package ovh.simplyconnect.spring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ovh.simplyconnect.client.SimplyConnectClient;

/** Spring Boot auto-configuration for the Simply Connect client. */
@AutoConfiguration
@ConditionalOnClass(SimplyConnectClient.class)
@EnableConfigurationProperties(SimplyConnectProperties.class)
public class SimplyConnectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "simply-connect", name = "api-key")
    SimplyConnectClient simplyConnectClient(SimplyConnectProperties properties) {
        return SimplyConnectClient.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .defaultSmsEndpointId(properties.getDefaultSmsEndpointId())
                .connectTimeout(properties.getConnectTimeout())
                .requestTimeout(properties.getRequestTimeout())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "simply-connect.panel", name = "enabled", havingValue = "true")
    PanelConfigurationGuard simplyConnectPanelConfigurationGuard(SimplyConnectProperties properties) {
        return new PanelConfigurationGuard(properties);
    }

    static final class PanelConfigurationGuard {
        PanelConfigurationGuard(SimplyConnectProperties properties) {
            String password = properties.getPanel().getPassword();
            if (password == null || password.length() < 12) {
                throw new IllegalStateException(
                        "simply-connect.panel.password must contain at least 12 characters when the panel is enabled");
            }
            if (MessageDigest.isEqual(
                    password.getBytes(StandardCharsets.UTF_8),
                    Objects.toString(properties.getApiKey(), "").getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalStateException("The developer panel password must not be the API key");
            }
        }
    }
}

