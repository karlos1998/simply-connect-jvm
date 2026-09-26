package ovh.simplyconnect.spring.panel;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;
import ovh.simplyconnect.client.SimplyConnectClient;
import ovh.simplyconnect.spring.SimplyConnectProperties;

/** Auto-configuration for the opt-in developer panel. */
@AutoConfiguration(after = ovh.simplyconnect.spring.SimplyConnectAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(prefix = "simply-connect.panel", name = "enabled", havingValue = "true")
public class SimplyConnectPanelAutoConfiguration {

    @Bean
    PanelAccess panelAccess(SimplyConnectProperties properties) {
        return new PanelAccess(properties.getPanel().getPassword());
    }

    @Bean
    SimplyConnectPanelController simplyConnectPanelController(
            SimplyConnectClient client,
            SimplyConnectProperties properties,
            PanelAccess access) {
        return new SimplyConnectPanelController(client, properties, access);
    }

    @Bean
    FilterRegistrationBean<PanelSecurityFilter> simplyConnectPanelSecurityFilter(
            SimplyConnectProperties properties, PanelAccess access) {
        var registration = new FilterRegistrationBean<>(
                new PanelSecurityFilter(properties.getPanel().normalizedPath(), access));
        registration.setName("simplyConnectPanelSecurityFilter");
        registration.addUrlPatterns(properties.getPanel().normalizedPath() + "/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}

