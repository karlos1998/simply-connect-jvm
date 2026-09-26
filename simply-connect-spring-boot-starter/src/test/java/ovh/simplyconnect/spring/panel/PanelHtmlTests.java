package ovh.simplyconnect.spring.panel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PanelHtmlTests {
    @Test
    void escapesPathAndNeverRendersPasswordOrApiKeyOnLogin() {
        String html = PanelHtml.login("/dev\" onmouseover=\"alert(1)", true);

        assertThat(html).contains("&quot;").doesNotContain("onmouseover=\"alert(1)");
        assertThat(html).doesNotContain("X-API-Key");
    }
}

