package com.soumyadebnath.mobility.protocol;

public record ComplianceStateProof(
    long sequenceNumber,
    String previousHash,
    String stateHash,
    long timestamp,
    String verificationDigest
) {
    public ComplianceStateProof {
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("sequenceNumber cannot be negative");
        }
        if (previousHash == null || previousHash.isBlank()) {
            throw new IllegalArgumentException("previousHash cannot be null or blank");
        }
        if (stateHash == null || stateHash.isBlank()) {
            throw new IllegalArgumentException("stateHash cannot be null or blank");
        }
        if (verificationDigest == null || verificationDigest.isBlank()) {
            throw new IllegalArgumentException("verificationDigest cannot be null or blank");
        }
    }
}
