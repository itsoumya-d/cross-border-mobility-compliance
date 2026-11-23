package com.soumyadebnath.mobility.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OidcTokenValidatorTest {

    private OidcTokenValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new OidcTokenValidator(objectMapper, 30L);
    }

    private String createTestJwt(Map<String, Object> payloadMap) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = objectMapper.writeValueAsString(payloadMap);
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".mockSignatureBytes";
    }

    @Test
    void validatesValidTokenAndExtractsClaims() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = Map.of(
                "sub", "user-uuid-123",
                "iss", "https://identity.auth.internal",
                "aud", "mobility-compliance-api",
                "exp", now + 3600,
                "nbf", now - 60,
                "email", "devops.engineer@internal.net",
                "roles", List.of("DEVOPS", "SECURITY_ADMIN")
        );
        String jwt = createTestJwt(claims);

        OidcTokenClaims parsed = validator.validateAndExtract(jwt, "https://identity.auth.internal", "mobility-compliance-api");

        assertNotNull(parsed);
        assertEquals("user-uuid-123", parsed.getSubject());
        assertEquals("devops.engineer@internal.net", parsed.getEmail());
        assertTrue(parsed.hasRole("DEVOPS"));
        assertTrue(parsed.hasRole("SECURITY_ADMIN"));
        assertFalse(parsed.hasRole("UNKNOWN_ROLE"));
    }

    @Test
    void extractsKeycloakRealmAccessRoles() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = Map.of(
                "sub", "keycloak-user-789",
                "exp", now + 1800,
                "realm_access", Map.of("roles", List.of("trust-officer", "auditor"))
        );
        String jwt = createTestJwt(claims);

        OidcTokenClaims parsed = validator.validateAndExtract(jwt, null, null);

        assertNotNull(parsed);
        assertEquals("keycloak-user-789", parsed.getSubject());
        assertTrue(parsed.hasRole("trust-officer"));
        assertTrue(parsed.hasRole("auditor"));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = Map.of(
                "sub", "user-expired",
                "exp", now - 600 // Expired 10 minutes ago
        );
        String jwt = createTestJwt(claims);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.validateAndExtract(jwt, null, null));
        assertTrue(ex.getMessage().contains("Token expired"));
    }

    @Test
    void rejectsIssuerMismatch() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = Map.of(
                "sub", "user-1",
                "iss", "https://untrusted-idp.com",
                "exp", now + 1000
        );
        String jwt = createTestJwt(claims);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.validateAndExtract(jwt, "https://trusted-idp.com", null));
        assertTrue(ex.getMessage().contains("Issuer mismatch"));
    }

    @Test
    void rejectsAudienceMismatch() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = Map.of(
                "sub", "user-1",
                "aud", "wrong-audience",
                "exp", now + 1000
        );
        String jwt = createTestJwt(claims);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.validateAndExtract(jwt, null, "expected-compliance-api"));
        assertTrue(ex.getMessage().contains("Audience mismatch"));
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateAndExtract("not-a-jwt", null, null));
        assertThrows(IllegalArgumentException.class, () -> validator.validateAndExtract("", null, null));
    }
}
