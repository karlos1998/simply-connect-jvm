package ovh.simplyconnect.spring.panel;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import ovh.simplyconnect.client.SimplyConnectClient;
import ovh.simplyconnect.client.SimplyConnectException;
import ovh.simplyconnect.client.model.CallQueueEndpoint;
import ovh.simplyconnect.client.model.CallQueuePage;
import ovh.simplyconnect.client.model.CallQueueQuery;
import ovh.simplyconnect.client.model.CallQueueRequest;
import ovh.simplyconnect.client.model.MessagePage;
import ovh.simplyconnect.client.model.MessageQuery;
import ovh.simplyconnect.client.model.PublishedCallFlow;
import ovh.simplyconnect.client.model.SmsEndpoint;
import ovh.simplyconnect.client.model.SmsSendRequest;
import ovh.simplyconnect.spring.SimplyConnectProperties;

@RestController
@RequestMapping("${simply-connect.panel.path:/simply-connect}")
final class SimplyConnectPanelController {
    private final SimplyConnectClient client;
    private final SimplyConnectProperties properties;
    private final PanelAccess access;

    SimplyConnectPanelController(
            SimplyConnectClient client,
            SimplyConnectProperties properties,
            PanelAccess access) {
        this.client = client;
        this.properties = properties;
        this.access = access;
    }

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> login(HttpServletRequest request, @RequestParam(required = false) String error) {
        if (access.isAuthenticated(request)) return redirect(panelPath(request));
        return html(PanelHtml.login(panelPath(request), error != null));
    }

    @PostMapping("/login")
    ResponseEntity<String> authenticate(HttpServletRequest request, @RequestParam String password) {
        if (!access.login(request, password)) {
            return ResponseEntity.status(401)
                    .contentType(MediaType.TEXT_HTML)
                    .body(PanelHtml.login(panelPath(request), true));
        }
        return redirect(panelPath(request));
    }

    @GetMapping(value = {"", "/"}, produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> dashboard(
            HttpServletRequest request,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) UUID endpointId) {
        Capability<List<SmsEndpoint>> smsEndpoints = load(client::listSmsEndpoints);
        Capability<MessagePage> messages = load(() -> client.listMessages(
                new MessageQuery(phone, null, null, endpointId, null, null, 0, 25)));
        Capability<List<CallQueueEndpoint>> callEndpoints = load(client::listCallQueueEndpoints);
        Capability<List<PublishedCallFlow>> flows = load(client::listPublishedCallFlows);
        Capability<CallQueuePage> queue = load(() -> client.listCallQueue(CallQueueQuery.firstPage()));

        return html(PanelHtml.dashboard(
                panelPath(request),
                access.csrfToken(request),
                properties,
                smsEndpoints,
                messages,
                callEndpoints,
                flows,
                queue,
                phone,
                endpointId,
                takeFlash(request)));
    }

    @PostMapping("/sms")
    ResponseEntity<String> sendSms(
            HttpServletRequest request,
            @RequestParam String csrf,
            @RequestParam UUID endpointId,
            @RequestParam String to,
            @RequestParam String body) {
        access.requireCsrf(request, csrf);
        try {
            String key = "panel-sms-" + UUID.randomUUID();
            var result = client.sendSms(new SmsSendRequest(endpointId, to, body), key);
            flash(request, "success", "SMS przyjęty do kolejki: " + result.messageId() + " (" + result.status() + ")");
        } catch (RuntimeException failure) {
            flash(request, "error", friendly(failure));
        }
        return redirect(panelPath(request));
    }

    @PostMapping("/call-queue")
    ResponseEntity<String> createCall(
            HttpServletRequest request,
            @RequestParam String csrf,
            @RequestParam UUID endpointId,
            @RequestParam UUID flowVersionId,
            @RequestParam String destination,
            @RequestParam(defaultValue = "Europe/Warsaw") String timeZone,
            @RequestParam(required = false) LocalDateTime scheduledFor,
            @RequestParam(defaultValue = "30") int intervalSeconds) {
        access.requireCsrf(request, csrf);
        try {
            var created = client.createCallQueueEntry(new CallQueueRequest(
                    UUID.randomUUID(), endpointId, flowVersionId, destination, scheduledFor, timeZone, intervalSeconds));
            flash(request, "success", "Połączenie dodane do kolejki: " + created.id() + " (" + created.status() + ")");
        } catch (RuntimeException failure) {
            flash(request, "error", friendly(failure));
        }
        return redirect(panelPath(request));
    }

    @PostMapping("/logout")
    ResponseEntity<String> logout(HttpServletRequest request, @RequestParam String csrf) {
        access.requireCsrf(request, csrf);
        access.logout(request);
        return redirect(panelPath(request) + "/login");
    }

    private String panelPath(HttpServletRequest request) {
        return request.getContextPath() + properties.getPanel().normalizedPath();
    }

    private static ResponseEntity<String> html(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
    }

    private static ResponseEntity<String> redirect(String path) {
        return ResponseEntity.status(303).header("Location", path).build();
    }

    private static <T> Capability<T> load(Supplier<T> action) {
        try {
            return new Capability<>(action.get(), null);
        } catch (RuntimeException failure) {
            return new Capability<>(null, friendly(failure));
        }
    }

    private static String friendly(RuntimeException failure) {
        if (failure instanceof SimplyConnectException apiFailure) {
            String message = apiFailure.statusCode() == 401
                    ? "Klucz nie ma wymaganego uprawnienia albo dostępu do endpointu."
                    : apiFailure.getMessage();
            if (apiFailure.correlationId() != null) message += " Correlation ID: " + apiFailure.correlationId();
            return message;
        }
        return failure.getMessage() == null ? "Operacja nie powiodła się." : failure.getMessage();
    }

    private static void flash(HttpServletRequest request, String type, String message) {
        request.getSession().setAttribute(SimplyConnectPanelController.class.getName() + ".flash", new Flash(type, message));
    }

    private static Flash takeFlash(HttpServletRequest request) {
        String key = SimplyConnectPanelController.class.getName() + ".flash";
        Object value = request.getSession().getAttribute(key);
        request.getSession().removeAttribute(key);
        return value instanceof Flash flash ? flash : null;
    }

    record Capability<T>(T value, String error) {
        boolean available() { return error == null; }
    }

    record Flash(String type, String message) {}
}

