package com.soumyadebnath.mobility.auth;

import java.util.Collections;
import java.util.List;

public class OidcTokenClaims {
    private final String subject;
    private final String issuer;
    private final String audience;
    private final long expirationTime;
    private final long notBeforeTime;
    private final String email;
    private final List<String> roles;

    public OidcTokenClaims(String subject, String issuer, String audience,
                           long expirationTime, long notBeforeTime,
                           String email, List<String> roles) {
        this.subject = subject;
        this.issuer = issuer;
        this.audience = audience;
        this.expirationTime = expirationTime;
        this.notBeforeTime = notBeforeTime;
        this.email = email;
        this.roles = roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
    }

    public String getSubject() { return subject; }
    public String getIssuer() { return issuer; }
    public String getAudience() { return audience; }
    public long getExpirationTime() { return expirationTime; }
    public long getNotBeforeTime() { return notBeforeTime; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
