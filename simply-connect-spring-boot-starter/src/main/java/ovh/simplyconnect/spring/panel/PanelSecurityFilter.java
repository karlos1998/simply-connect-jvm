package ovh.simplyconnect.spring.panel;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

final class PanelSecurityFilter extends OncePerRequestFilter {
    private final String panelPath;
    private final PanelAccess access;

    PanelSecurityFilter(String panelPath, PanelAccess access) {
        this.panelPath = panelPath;
        this.access = access;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        secure(response);
        String applicationPath = request.getRequestURI().substring(request.getContextPath().length());
        boolean loginRoute = applicationPath.equals(panelPath + "/login");
        if (!loginRoute && !access.isAuthenticated(request)) {
            response.sendRedirect(request.getContextPath() + panelPath + "/login");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static void secure(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
    }
}

