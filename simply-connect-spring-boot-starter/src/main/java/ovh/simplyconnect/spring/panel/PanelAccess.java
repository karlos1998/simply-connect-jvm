package ovh.simplyconnect.spring.panel;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PanelAccess {
    private static final String AUTHENTICATED = PanelAccess.class.getName() + ".authenticated";
    private static final String CSRF = PanelAccess.class.getName() + ".csrf";
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 300;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] expectedPassword;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    PanelAccess(String password) {
        this.expectedPassword = password.getBytes(StandardCharsets.UTF_8);
    }

    boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(AUTHENTICATED));
    }

    boolean login(HttpServletRequest request, String password) {
        String key = clientKey(request);
        long now = Instant.now().getEpochSecond();
        Attempts current = attempts.compute(key, (ignored, previous) -> previous == null || previous.expiresAt < now
                ? new Attempts(0, now + WINDOW_SECONDS)
                : previous);
        if (current.count >= MAX_ATTEMPTS) return false;

        boolean matches = MessageDigest.isEqual(
                expectedPassword,
                (password == null ? "" : password).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            attempts.put(key, new Attempts(current.count + 1, current.expiresAt));
            if (attempts.size() > 1000) attempts.clear();
            return false;
        }

        attempts.remove(key);
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(AUTHENTICATED, true);
        session.setAttribute(CSRF, randomToken());
        session.setMaxInactiveInterval(30 * 60);
        return true;
    }

    void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    String csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) throw new PanelAccessDeniedException();
        Object value = session.getAttribute(CSRF);
        if (!(value instanceof String token)) throw new PanelAccessDeniedException();
        return token;
    }

    void requireCsrf(HttpServletRequest request, String supplied) {
        String expected = csrfToken(request);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                (supplied == null ? "" : supplied).getBytes(StandardCharsets.UTF_8))) {
            throw new PanelAccessDeniedException();
        }
    }

    private static String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static String randomToken() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private record Attempts(int count, long expiresAt) {}

    static final class PanelAccessDeniedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}

