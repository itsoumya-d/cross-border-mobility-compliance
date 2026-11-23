package com.soumyadebnath.mobility.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class OidcTokenValidator {

    private final ObjectMapper objectMapper;
    private final long clockSkewSeconds;

    public OidcTokenValidator() {
        this(new ObjectMapper(), 60L);
    }

    public OidcTokenValidator(ObjectMapper objectMapper, long clockSkewSeconds) {
        this.objectMapper = objectMapper;
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public OidcTokenClaims validateAndExtract(String token, String expectedIssuer, String expectedAudience) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Bearer token must not be null or empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Malformed JWT: token must contain at least header and payload segments");
        }

        JsonNode payload;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            payload = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode token payload: " + e.getMessage(), e);
        }

        long now = Instant.now().getEpochSecond();

        long exp = payload.has("exp") ? payload.get("exp").asLong() : 0L;
        if (exp > 0 && now - clockSkewSeconds > exp) {
            throw new IllegalStateException("Token expired at " + exp + ", current time is " + now);
        }

        long nbf = payload.has("nbf") ? payload.get("nbf").asLong() : 0L;
        if (nbf > 0 && now + clockSkewSeconds < nbf) {
            throw new IllegalStateException("Token not valid before " + nbf + ", current time is " + now);
        }

        String iss = payload.has("iss") ? payload.get("iss").asText() : "";
        if (expectedIssuer != null && !expectedIssuer.isBlank() && !expectedIssuer.equals(iss)) {
            throw new IllegalStateException("Issuer mismatch: expected " + expectedIssuer + " but got " + iss);
        }

        String aud = payload.has("aud") ? payload.get("aud").asText() : "";
        if (expectedAudience != null && !expectedAudience.isBlank() && !expectedAudience.equals(aud)) {
            throw new IllegalStateException("Audience mismatch: expected " + expectedAudience + " but got " + aud);
        }

        String sub = payload.has("sub") ? payload.get("sub").asText() : "";
        String email = payload.has("email") ? payload.get("email").asText() : "";

        List<String> roles = new ArrayList<>();
        if (payload.has("roles") && payload.get("roles").isArray()) {
            for (JsonNode r : payload.get("roles")) {
                roles.add(r.asText());
            }
        } else if (payload.has("realm_access") && payload.get("realm_access").has("roles")) {
            for (JsonNode r : payload.get("realm_access").get("roles")) {
                roles.add(r.asText());
            }
        }

        return new OidcTokenClaims(sub, iss, aud, exp, nbf, email, roles);
    }
}
