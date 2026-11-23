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
 * Trust & Identity Auth Filter supporting API-key and OIDC Bearer tokens.
 * Validates OIDC JWT tokens and client credentials against security contracts.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/actuator/health");
    private final OidcTokenValidator tokenValidator;

    public ApiKeyAuthFilter(OidcTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (PUBLIC_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                String expectedIss = System.getenv("OIDC_ISSUER");
                String expectedAud = System.getenv("OIDC_AUDIENCE");
                OidcTokenClaims claims = tokenValidator.validateAndExtract(token, expectedIss, expectedAud);
                request.setAttribute("auth.claims", claims);
                request.setAttribute("auth.subject", claims.getSubject());
                filterChain.doFilter(request, response);
                return;
            } catch (Exception e) {
                response.setStatus(401);
                response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"" + e.getMessage() + "\"");
                response.getWriter().write("Unauthorized: Invalid Bearer token - " + e.getMessage());
                return;
            }
        }

        // Fallback to API Key when Bearer token is not supplied
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
