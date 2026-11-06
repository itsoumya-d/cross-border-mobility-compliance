package com.soumyadebnath.mobility.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * API-key + OAuth2-client-credentials compatible guard.
 * Mirrors the public WorkFlex Open-API contract (POST /oauth/token,
 * GET /status) while allowing public health checks.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/actuator/health");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (PUBLIC_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        // In this reference implementation the key is optional for local
        // development but enforced when the MOBILITY_API_KEY env var is set.
        String expected = System.getenv("MOBILITY_API_KEY");
        String provided = request.getHeader("X-API-Key");
        if (expected != null && !expected.isBlank()) {
            if (provided == null || !expected.equals(provided)) {
                response.setStatus(401);
                response.getWriter().write("Unauthorized: X-API-Key required");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
