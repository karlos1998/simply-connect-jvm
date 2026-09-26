package ovh.simplyconnect.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import ovh.simplyconnect.client.SimplyConnectClient;
import ovh.simplyconnect.spring.panel.SimplyConnectPanelAutoConfiguration;

class SimplyConnectAutoConfigurationTests {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SimplyConnectAutoConfiguration.class,
                    SimplyConnectPanelAutoConfiguration.class));

    @Test
    void configuresClientButKeepsPanelDisabledByDefault() {
        contextRunner.withPropertyValues("simply-connect.api-key=test-key").run(context -> {
            assertThat(context).hasSingleBean(SimplyConnectClient.class);
            assertThat(context).doesNotHaveBean("simplyConnectPanelController");
            assertThat(context).doesNotHaveBean("simplyConnectPanelSecurityFilter");
        });
    }

    @Test
    void requiresSeparateStrongPanelPassword() {
        contextRunner.withPropertyValues(
                        "simply-connect.api-key=test-key",
                        "simply-connect.panel.enabled=true",
                        "simply-connect.panel.password=short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void enablesDeveloperPanelExplicitly() {
        contextRunner.withPropertyValues(
                        "simply-connect.api-key=test-key",
                        "simply-connect.panel.enabled=true",
                        "simply-connect.panel.path=/developer/simply-connect",
                        "simply-connect.panel.password=a-separate-long-password")
                .run(context -> {
                    assertThat(context).hasSingleBean(SimplyConnectClient.class);
                    assertThat(context).hasBean("simplyConnectPanelController");
                    assertThat(context).hasBean("simplyConnectPanelSecurityFilter");
                });
    }
}

