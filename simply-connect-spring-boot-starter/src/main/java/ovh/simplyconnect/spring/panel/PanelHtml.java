package ovh.simplyconnect.spring.panel;

import java.util.List;
import java.util.UUID;
import ovh.simplyconnect.client.model.CallQueueEndpoint;
import ovh.simplyconnect.client.model.CallQueuePage;
import ovh.simplyconnect.client.model.MessagePage;
import ovh.simplyconnect.client.model.PublishedCallFlow;
import ovh.simplyconnect.client.model.SmsEndpoint;
import ovh.simplyconnect.spring.SimplyConnectProperties;
import ovh.simplyconnect.spring.panel.SimplyConnectPanelController.Capability;
import ovh.simplyconnect.spring.panel.SimplyConnectPanelController.Flash;

final class PanelHtml {
    private PanelHtml() {}

    static String login(String path, boolean error) {
        String notice = error
                ? "<p class=\"notice error\">Nieprawidłowe hasło albo zbyt wiele prób. Spróbuj ponownie za chwilę.</p>"
                : "";
        return document("Logowanie", """
                <main class="login-shell">
                  <section class="login-card">
                    <div class="mark">SC</div>
                    <p class="kicker">Simply Connect · developer panel</p>
                    <h1>Wejdź do kokpitu integracji</h1>
                    <p class="lead">Hasło panelu jest osobne od klucza API i pozostaje po stronie aplikacji.</p>
                    %s
                    <form method="post" action="%s/login" class="stack">
                      <label>Hasło panelu<input type="password" name="password" autocomplete="current-password" required autofocus></label>
                      <button type="submit">Zaloguj się</button>
                    </form>
                  </section>
                </main>
                """.formatted(notice, e(path)));
    }

    static String dashboard(
            String path,
            String csrf,
            SimplyConnectProperties properties,
            Capability<List<SmsEndpoint>> smsEndpoints,
            Capability<MessagePage> messages,
            Capability<List<CallQueueEndpoint>> callEndpoints,
            Capability<List<PublishedCallFlow>> flows,
            Capability<CallQueuePage> queue,
            String phone,
            UUID endpointId,
            Flash flash) {
        StringBuilder body = new StringBuilder(32_000);
        body.append("<header><a class=\"brand\" href=\"").append(e(path)).append("\"><span>SC</span> Simply Connect</a>")
                .append("<nav><a href=\"#bramki\">Bramki</a><a href=\"#sms\">Test SMS</a><a href=\"#wiadomosci\">Wiadomości</a><a href=\"#kolejka\">Kolejka połączeń</a><a href=\"#kod\">Kod</a></nav>")
                .append("<form method=\"post\" action=\"").append(e(path)).append("/logout\"><input type=\"hidden\" name=\"csrf\" value=\"").append(e(csrf)).append("\"><button class=\"quiet\" type=\"submit\">Wyloguj</button></form></header>");

        body.append("<main class=\"shell\"><section class=\"hero\"><div><p class=\"kicker\">Developer cockpit</p><h1>Twoje API.<br><em>Widoczne od środka.</em></h1><p class=\"lead\">Sprawdź dostępne numery, prześledź wiadomości i bezpiecznie wywołaj te same operacje, których używa kod aplikacji.</p></div>")
                .append("<aside><span class=\"status-dot\"></span><div><strong>Panel aktywny</strong><small>Ścieżka ").append(e(properties.getPanel().normalizedPath())).append(" · sesja 30 min</small></div></aside></section>");

        if (flash != null) {
            body.append("<p class=\"notice ").append(e(flash.type())).append("\">").append(e(flash.message())).append("</p>");
        }

        body.append("<section class=\"metrics\">")
                .append(metric("SMS endpoints", count(smsEndpoints), smsEndpoints.available()))
                .append(metric("Wiadomości", messageCount(messages), messages.available()))
                .append(metric("Voice endpoints", count(callEndpoints), callEndpoints.available()))
                .append(metric("Flow IVR", count(flows), flows.available()))
                .append("</section>");

        body.append("<section id=\"bramki\" class=\"section\"><div class=\"section-head\"><div><p class=\"kicker\">01 · Dostęp</p><h2>Bramki widoczne dla klucza</h2></div><p>Lista jest już ograniczona przez scope’y i endpointy przypisane do klucza API.</p></div><div class=\"grid two\">");
        body.append(endpointPanel(smsEndpoints));
        body.append(callEndpointPanel(callEndpoints));
        body.append("</div></section>");

        body.append("<section id=\"sms\" class=\"section panel-section\"><div class=\"section-head\"><div><p class=\"kicker\">02 · Kontrolowany test</p><h2>Wyślij testowy SMS</h2></div><p>Każde kliknięcie dostaje nowy klucz idempotencji. Zacznij od własnego numeru.</p></div>");
        if (smsEndpoints.available() && !smsEndpoints.value().isEmpty()) {
            body.append("<form method=\"post\" action=\"").append(e(path)).append("/sms\" class=\"form-grid\"><input type=\"hidden\" name=\"csrf\" value=\"").append(e(csrf)).append("\">")
                    .append("<label>Bramka<select name=\"endpointId\" required>")
                    .append(options(smsEndpoints.value(), properties.getDefaultSmsEndpointId()))
                    .append("</select></label><label>Numer odbiorcy<input name=\"to\" placeholder=\"+48500100200\" maxlength=\"32\" required></label>")
                    .append("<label class=\"wide\">Treść<textarea name=\"body\" rows=\"4\" maxlength=\"5000\" required>Test Simply Connect z aplikacji developerskiej.</textarea></label>")
                    .append("<div class=\"wide form-action\"><small>Operacja używa publicznego POST /api/v1/external/messages.</small><button type=\"submit\">Dodaj SMS do kolejki</button></div></form>");
        } else {
            body.append(unavailable(smsEndpoints.error(), "Klucz potrzebuje scope’u SMS_SEND oraz dostępu do co najmniej jednego endpointu SMS."));
        }
        body.append("</section>");

        body.append(messagesSection(path, messages, smsEndpoints, phone, endpointId));
        body.append(callQueueSection(path, csrf, callEndpoints, flows, queue));
        body.append(codeSection(properties));
        body.append("</main><footer><span>Simply Connect JVM</span><span>Panel domyślnie wyłączony · klucz API nigdy nie trafia do przeglądarki</span></footer>");
        return document("Developer panel", body.toString());
    }

    private static String endpointPanel(Capability<List<SmsEndpoint>> capability) {
        if (!capability.available()) return cardError("Bramki SMS", capability.error());
        StringBuilder out = new StringBuilder("<article class=\"card\"><div class=\"card-title\"><h3>Bramki SMS</h3><span>SMS_SEND</span></div>");
        if (capability.value().isEmpty()) out.append("<p class=\"empty\">Klucz nie widzi żadnej bramki SMS.</p>");
        for (SmsEndpoint endpoint : capability.value()) {
            out.append("<div class=\"endpoint\"><i class=\"").append(endpoint.enabled() ? "online" : "offline").append("\"></i><div><strong>").append(e(endpoint.name())).append("</strong><small>").append(e(orDash(endpoint.phoneNumber()))).append("</small></div><code>").append(e(endpoint.id())).append("</code></div>");
        }
        return out.append("</article>").toString();
    }

    private static String callEndpointPanel(Capability<List<CallQueueEndpoint>> capability) {
        if (!capability.available()) return cardError("Bramki voice", capability.error());
        StringBuilder out = new StringBuilder("<article class=\"card\"><div class=\"card-title\"><h3>Bramki voice</h3><span>CALL_QUEUE_WRITE</span></div>");
        if (capability.value().isEmpty()) out.append("<p class=\"empty\">Klucz nie widzi żadnej bramki do połączeń wychodzących.</p>");
        for (CallQueueEndpoint endpoint : capability.value()) {
            out.append("<div class=\"endpoint\"><i class=\"").append("ONLINE".equals(endpoint.gatewayStatus()) ? "online" : "offline").append("\"></i><div><strong>").append(e(endpoint.name())).append("</strong><small>").append(e(orDash(endpoint.phoneNumber()))).append(" · ").append(e(endpoint.gatewayName())).append("</small></div><b>").append(e(endpoint.gatewayStatus())).append("</b></div>");
        }
        return out.append("</article>").toString();
    }

    private static String messagesSection(
            String path,
            Capability<MessagePage> messages,
            Capability<List<SmsEndpoint>> endpoints,
            String phone,
            UUID selectedEndpoint) {
        StringBuilder out = new StringBuilder("<section id=\"wiadomosci\" class=\"section\"><div class=\"section-head\"><div><p class=\"kicker\">03 · Historia</p><h2>Wiadomości z bramki</h2></div><p>Filtruj po numerze, fragmencie treści albo wybranej bramce. Najnowsze zdarzenia są pierwsze.</p></div>");
        out.append("<form method=\"get\" action=\"").append(e(path)).append("\" class=\"filters\"><input name=\"phone\" value=\"").append(e(phone)).append("\" placeholder=\"Numer lub tekst\">");
        if (endpoints.available()) {
            out.append("<select name=\"endpointId\"><option value=\"\">Wszystkie dostępne bramki</option>");
            for (SmsEndpoint endpoint : endpoints.value()) {
                out.append("<option value=\"").append(endpoint.id()).append("\"")
                        .append(endpoint.id().equals(selectedEndpoint) ? " selected" : "")
                        .append(">").append(e(endpoint.name())).append(" · ").append(e(orDash(endpoint.phoneNumber()))).append("</option>");
            }
            out.append("</select>");
        }
        out.append("<button type=\"submit\">Filtruj</button></form>");
        if (!messages.available()) {
            out.append(unavailable(messages.error(), "Klucz potrzebuje scope’u MESSAGES_READ."));
        } else if (messages.value().items().isEmpty()) {
            out.append("<p class=\"empty table-empty\">Brak wiadomości pasujących do filtra.</p>");
        } else {
            out.append("<div class=\"table-wrap\"><table><thead><tr><th>Czas</th><th>Kierunek</th><th>Numer</th><th>Treść</th><th>Status</th><th>Bramka</th></tr></thead><tbody>");
            messages.value().items().forEach(message -> out.append("<tr><td>").append(e(message.occurredAt())).append("</td><td><span class=\"pill\">").append(e(message.direction())).append("</span></td><td>").append(e(message.remoteAddress())).append("</td><td class=\"message-body\">").append(e(message.body())).append("</td><td>").append(e(message.status())).append("</td><td>").append(e(message.endpoint() == null ? null : message.endpoint().name())).append("</td></tr>"));
            out.append("</tbody></table></div><p class=\"table-meta\">").append(messages.value().totalElements()).append(" wyników · strona ").append(messages.value().page() + 1).append(" z ").append(Math.max(1, messages.value().totalPages())).append("</p>");
        }
        return out.append("</section>").toString();
    }

    private static String callQueueSection(
            String path,
            String csrf,
            Capability<List<CallQueueEndpoint>> endpoints,
            Capability<List<PublishedCallFlow>> flows,
            Capability<CallQueuePage> queue) {
        StringBuilder out = new StringBuilder("<section id=\"kolejka\" class=\"section panel-section\"><div class=\"section-head\"><div><p class=\"kicker\">04 · Voice / IVR</p><h2>Kolejka połączeń</h2></div><p>Dodaj jedno idempotentne połączenie do opublikowanego flow i obserwuj jego stan.</p></div>");
        if (endpoints.available() && flows.available() && !endpoints.value().isEmpty() && !flows.value().isEmpty()) {
            out.append("<form method=\"post\" action=\"").append(e(path)).append("/call-queue\" class=\"form-grid compact\"><input type=\"hidden\" name=\"csrf\" value=\"").append(e(csrf)).append("\"><label>Bramka<select name=\"endpointId\" required>");
            for (CallQueueEndpoint endpoint : endpoints.value()) out.append("<option value=\"").append(endpoint.id()).append("\">").append(e(endpoint.name())).append(" · ").append(e(orDash(endpoint.phoneNumber()))).append("</option>");
            out.append("</select></label><label>Flow IVR<select name=\"flowVersionId\" required>");
            for (PublishedCallFlow flow : flows.value()) out.append("<option value=\"").append(flow.publishedVersionId()).append("\">").append(e(flow.name())).append("</option>");
            out.append("</select></label><label>Numer docelowy<input name=\"destination\" placeholder=\"+48500100200\" required></label><label>Strefa czasowa<input name=\"timeZone\" value=\"Europe/Warsaw\" required></label><label>Termin (opcjonalnie)<input type=\"datetime-local\" name=\"scheduledFor\"></label><label>Przerwa po połączeniu<input type=\"number\" name=\"intervalSeconds\" value=\"30\" min=\"0\" max=\"3600\"></label><div class=\"wide form-action\"><small>Brak automatycznego ponowienia przy niejednoznacznym wyniku.</small><button type=\"submit\">Dodaj do kolejki</button></div></form>");
        } else {
            String error = !endpoints.available() ? endpoints.error() : !flows.available() ? flows.error() : null;
            out.append(unavailable(error, "Do dodawania połączeń potrzebujesz CALL_QUEUE_WRITE, bramki voice i opublikowanego flow IVR."));
        }
        out.append("<h3 class=\"subheading\">Ostatnie pozycje</h3>");
        if (!queue.available()) {
            out.append(unavailable(queue.error(), "Do podglądu kolejki potrzebujesz scope’u CALL_QUEUE_READ."));
        } else if (queue.value().items().isEmpty()) {
            out.append("<p class=\"empty table-empty\">Kolejka jest pusta.</p>");
        } else {
            out.append("<div class=\"table-wrap\"><table><thead><tr><th>Nie wcześniej niż</th><th>Numer</th><th>Flow</th><th>Status</th><th>Źródło</th><th>Powód</th></tr></thead><tbody>");
            queue.value().items().forEach(item -> out.append("<tr><td>").append(e(item.notBefore())).append("</td><td>").append(e(item.destination())).append("</td><td>").append(e(item.flowName())).append("</td><td><span class=\"pill\">").append(e(item.status())).append("</span></td><td>").append(e(item.source())).append("</td><td>").append(e(orDash(item.reason()))).append("</td></tr>"));
            out.append("</tbody></table></div>");
        }
        return out.append("</section>").toString();
    }

    private static String codeSection(SimplyConnectProperties properties) {
        String endpoint = properties.getDefaultSmsEndpointId() == null
                ? "UUID.fromString(\"twoj-endpoint-id\")"
                : "UUID.fromString(\"" + properties.getDefaultSmsEndpointId() + "\")";
        String java = """
                var client = SimplyConnectClient.builder()
                    .apiKey(System.getenv("SIMPLY_CONNECT_API_KEY"))
                    .defaultSmsEndpointId(%s)
                    .build();

                var result = client.sendSms(
                    "+48500100200",
                    "Twoje zamówienie jest gotowe.",
                    "order-1842-ready"
                );
                """.formatted(endpoint);
        String kotlin = """
                val client = SimplyConnectClient.builder()
                    .apiKey(System.getenv("SIMPLY_CONNECT_API_KEY"))
                    .defaultSmsEndpointId(UUID.fromString("twoj-endpoint-id"))
                    .build()

                val result = client.sendSms(
                    "+48500100200",
                    "Twoje zamówienie jest gotowe.",
                    "order-1842-ready"
                )
                """;
        String config = """
                simply-connect:
                  api-key: ${SIMPLY_CONNECT_API_KEY}
                  default-sms-endpoint-id: ${SIMPLY_CONNECT_SMS_ENDPOINT_ID}
                  panel:
                    enabled: true
                    path: /simply-connect
                    password: ${SIMPLY_CONNECT_PANEL_PASSWORD}
                """;
        return "<section id=\"kod\" class=\"section\"><div class=\"section-head\"><div><p class=\"kicker\">05 · Użycie w aplikacji</p><h2>Panel jest instrukcją, nie osobnym światem</h2></div><p>Te same operacje są dostępne przez typowany klient Java. Kotlin korzysta z niego bez adaptera.</p></div><div class=\"code-grid\">"
                + codeCard("Java 17", java) + codeCard("Kotlin", kotlin) + codeCard("application.yml", config) + "</div></section>";
    }

    private static String codeCard(String title, String code) {
        return "<article class=\"code-card\"><span>" + e(title) + "</span><pre><code>" + e(code) + "</code></pre></article>";
    }

    private static String options(List<SmsEndpoint> endpoints, UUID selected) {
        StringBuilder out = new StringBuilder();
        for (SmsEndpoint endpoint : endpoints) {
            out.append("<option value=\"").append(endpoint.id()).append("\"")
                    .append(endpoint.id().equals(selected) ? " selected" : "")
                    .append(endpoint.enabled() ? "" : " disabled")
                    .append(">").append(e(endpoint.name())).append(" · ").append(e(orDash(endpoint.phoneNumber())))
                    .append(endpoint.enabled() ? "" : " · wyłączona").append("</option>");
        }
        return out.toString();
    }

    private static String metric(String label, String value, boolean available) {
        return "<article><span>" + e(label) + "</span><strong>" + e(value) + "</strong><small>" + (available ? "dostępne" : "brak scope’u") + "</small></article>";
    }

    private static String count(Capability<? extends List<?>> capability) {
        return capability.available() ? Integer.toString(capability.value().size()) : "—";
    }

    private static String messageCount(Capability<MessagePage> capability) {
        return capability.available() ? Long.toString(capability.value().totalElements()) : "—";
    }

    private static String cardError(String title, String error) {
        return "<article class=\"card\"><div class=\"card-title\"><h3>" + e(title) + "</h3><span>Niedostępne</span></div>" + unavailable(error, "Sprawdź scope’y klucza API.") + "</article>";
    }

    private static String unavailable(String error, String help) {
        return "<div class=\"unavailable\"><strong>Ta funkcja nie jest dostępna dla obecnego klucza.</strong><p>" + e(help) + "</p>" + (error == null ? "" : "<small>" + e(error) + "</small>") + "</div>";
    }

    private static String document(String title, String body) {
        return "<!doctype html><html lang=\"pl\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + e(title) + " · Simply Connect</title><style>" + css() + "</style></head><body>" + body + "</body></html>";
    }

    private static String e(Object value) {
        if (value == null) return "";
        return value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String css() {
        return """
                :root{--ink:#102a2a;--muted:#667d78;--line:#dce7e3;--paper:#f4f7f5;--mint:#08a17f;--coral:#ff735d;--navy:#0a2324}*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--paper);color:var(--ink);font:15px/1.55 Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}a{color:inherit;text-decoration:none}button,input,select,textarea{font:inherit}header{min-height:70px;padding:0 max(24px,calc((100vw - 1180px)/2));display:flex;align-items:center;gap:30px;border-bottom:1px solid var(--line);background:rgba(244,247,245,.94);position:sticky;top:0;z-index:5;backdrop-filter:blur(14px)}.brand{display:flex;align-items:center;gap:10px;font-weight:850}.brand span,.mark{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;background:var(--ink);color:white;font-size:11px;letter-spacing:.5px}nav{display:flex;gap:22px;margin-left:auto;color:var(--muted);font-size:12px;font-weight:700}header form{margin:0}.quiet{padding:8px 12px;border:1px solid var(--line);border-radius:10px;background:white;color:var(--ink);cursor:pointer}.shell{max-width:1180px;margin:auto;padding:64px 24px 110px}.hero{display:grid;grid-template-columns:1fr 320px;align-items:end;gap:70px;padding:50px 0 70px}.kicker{margin:0 0 14px;color:#08765f;font-size:10px;font-weight:900;letter-spacing:1.1px;text-transform:uppercase}.hero h1{margin:0;font-size:clamp(48px,7vw,84px);line-height:.96;letter-spacing:-4px}.hero h1 em{color:var(--coral);font-family:Georgia,serif;font-weight:500}.lead{max-width:720px;margin:28px 0 0;color:var(--muted);font-size:18px}.hero aside{padding:24px;display:flex;align-items:center;gap:14px;border:1px solid var(--line);border-radius:20px;background:white;box-shadow:0 20px 55px rgba(16,42,42,.08)}.hero aside small{display:block;color:var(--muted);margin-top:3px}.status-dot{width:10px;height:10px;border-radius:50%;background:var(--mint);box-shadow:0 0 0 7px rgba(8,161,127,.1)}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:90px}.metrics article{padding:22px;border:1px solid var(--line);border-radius:18px;background:white}.metrics span,.metrics small{display:block;color:var(--muted);font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.65px}.metrics strong{display:block;margin:7px 0 4px;font-size:34px}.section{padding:84px 0;border-top:1px solid var(--line)}.section-head{display:grid;grid-template-columns:1fr minmax(260px,420px);gap:40px;align-items:end;margin-bottom:32px}.section h2{margin:0;font-size:clamp(34px,4vw,52px);line-height:1;letter-spacing:-2.2px}.section-head>p{margin:0;color:var(--muted)}.grid.two{display:grid;grid-template-columns:1fr 1fr;gap:16px}.card{padding:26px;border:1px solid var(--line);border-radius:22px;background:white}.card-title{display:flex;align-items:center;justify-content:space-between;gap:16px;padding-bottom:18px;border-bottom:1px solid var(--line)}.card-title h3{margin:0;font-size:20px}.card-title span{padding:6px 8px;border-radius:8px;background:#e7f6f2;color:#08765f;font-size:9px;font-weight:900}.endpoint{padding:17px 0;display:grid;grid-template-columns:10px minmax(120px,1fr) auto;align-items:center;gap:13px;border-bottom:1px solid #edf2f0}.endpoint:last-child{border:0}.endpoint i{width:7px;height:7px;border-radius:50%}.endpoint i.online{background:var(--mint)}.endpoint i.offline{background:#aebbb7}.endpoint strong,.endpoint small{display:block}.endpoint small{color:var(--muted);font-size:12px}.endpoint code{max-width:190px;overflow:hidden;text-overflow:ellipsis;color:var(--muted);font-size:10px}.endpoint b{font-size:9px}.panel-section{margin:0 -30px;padding:70px 30px;border:1px solid #bed4cd;border-radius:30px;background:#eaf2ef}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px;padding:26px;border:1px solid var(--line);border-radius:22px;background:white}.form-grid.compact{grid-template-columns:repeat(3,1fr)}label{display:grid;gap:8px;color:var(--muted);font-size:11px;font-weight:800}input,select,textarea{width:100%;padding:12px 13px;border:1px solid #cbdad5;border-radius:10px;background:#fbfdfc;color:var(--ink);outline:none}input:focus,select:focus,textarea:focus{border-color:var(--mint);box-shadow:0 0 0 3px rgba(8,161,127,.1)}textarea{resize:vertical}.wide{grid-column:1/-1}.form-action{display:flex;align-items:center;justify-content:space-between;gap:20px}.form-action small{color:var(--muted)}button{padding:12px 17px;border:0;border-radius:10px;background:var(--ink);color:white;font-weight:800;cursor:pointer}.notice{padding:15px 18px;border-radius:12px}.notice.success{background:#dff5ed;color:#075f4d}.notice.error{background:#ffe6e1;color:#8a2d20}.filters{display:grid;grid-template-columns:1fr 1fr auto;gap:10px;margin-bottom:18px}.table-wrap{overflow:auto;border:1px solid var(--line);border-radius:18px;background:white}table{width:100%;border-collapse:collapse;min-width:850px}th,td{padding:14px 16px;border-bottom:1px solid #e8efec;text-align:left;vertical-align:top;font-size:12px}th{background:#f8faf9;color:var(--muted);font-size:9px;text-transform:uppercase;letter-spacing:.7px}tr:last-child td{border:0}.message-body{max-width:320px}.pill{display:inline-flex;padding:4px 7px;border-radius:7px;background:#e8f3f0;color:#086d58;font-size:9px;font-weight:900}.table-meta{color:var(--muted);font-size:11px}.empty,.unavailable{color:var(--muted)}.table-empty,.unavailable{padding:22px;border:1px dashed #b8cbc5;border-radius:16px}.unavailable strong{color:var(--ink)}.unavailable p{margin:5px 0}.unavailable small{display:block;margin-top:8px}.subheading{margin:38px 0 16px;font-size:20px}.code-grid{display:grid;gap:15px}.code-card{overflow:hidden;border:1px solid #294344;border-radius:20px;background:var(--navy);color:#dff4ee}.code-card>span{display:block;padding:12px 16px;border-bottom:1px solid #294344;color:#8fc2b6;font-size:10px;font-weight:900;text-transform:uppercase;letter-spacing:.8px}.code-card pre{margin:0;padding:20px;overflow:auto;font:12px/1.7 ui-monospace,SFMono-Regular,Menlo,monospace}.login-shell{min-height:100vh;padding:24px;display:grid;place-items:center}.login-card{width:min(460px,100%);padding:38px;border:1px solid var(--line);border-radius:28px;background:white;box-shadow:0 30px 80px rgba(16,42,42,.12)}.login-card h1{margin:12px 0;font-size:38px;line-height:1;letter-spacing:-2px}.login-card .lead{font-size:14px;margin:0 0 24px}.stack{display:grid;gap:16px}.stack button{width:100%}footer{padding:28px max(24px,calc((100vw - 1180px)/2));display:flex;justify-content:space-between;gap:20px;border-top:1px solid var(--line);color:var(--muted);font-size:10px}@media(max-width:850px){header nav{display:none}.shell{padding-top:30px}.hero,.section-head{grid-template-columns:1fr}.hero{gap:30px}.metrics{grid-template-columns:1fr 1fr}.grid.two,.form-grid,.form-grid.compact{grid-template-columns:1fr}.panel-section{margin-left:0;margin-right:0}.form-action{align-items:stretch;flex-direction:column}.filters{grid-template-columns:1fr}.endpoint{grid-template-columns:10px 1fr}.endpoint code,.endpoint b{grid-column:2}.hero h1{letter-spacing:-2.5px}}@media(max-width:520px){.metrics{grid-template-columns:1fr}.shell{padding-left:16px;padding-right:16px}.login-card{padding:28px 22px}footer{flex-direction:column}}
                """;
    }
}

