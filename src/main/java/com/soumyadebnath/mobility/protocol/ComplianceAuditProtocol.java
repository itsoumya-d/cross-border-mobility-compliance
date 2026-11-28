package com.soumyadebnath.mobility.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ComplianceAuditProtocol {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    public String computeStateHash(String employeeId, String destinationCountry, int tripDays, List<String> requiredActions) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId cannot be null or blank");
        }
        if (destinationCountry == null || destinationCountry.isBlank()) {
            throw new IllegalArgumentException("destinationCountry cannot be null or blank");
        }
        if (tripDays < 0) {
            throw new IllegalArgumentException("tripDays cannot be negative");
        }

        List<String> actions = requiredActions != null ? requiredActions.stream().sorted().toList() : Collections.emptyList();
        String payload = String.format("emp=%s|dest=%s|days=%d|actions=%s",
                employeeId.trim(), destinationCountry.trim().toUpperCase(), tripDays, String.join(",", actions));

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public ComplianceStateProof generateProof(long sequenceNumber, String previousHash, String stateHash, long timestamp, String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("secretKey cannot be null or blank");
        }
        if (previousHash == null || previousHash.isBlank()) {
            throw new IllegalArgumentException("previousHash cannot be null or blank");
        }
        if (stateHash == null || stateHash.isBlank()) {
            throw new IllegalArgumentException("stateHash cannot be null or blank");
        }

        String digest = computeDigest(sequenceNumber, previousHash, stateHash, timestamp, secretKey);
        return new ComplianceStateProof(sequenceNumber, previousHash, stateHash, timestamp, digest);
    }

    public boolean verifyProof(ComplianceStateProof proof, String secretKey) {
        if (proof == null || secretKey == null || secretKey.isBlank()) {
            return false;
        }
        String expectedDigest = computeDigest(
                proof.sequenceNumber(), proof.previousHash(), proof.stateHash(), proof.timestamp(), secretKey);
        return MessageDigest.isEqual(
                expectedDigest.getBytes(StandardCharsets.UTF_8),
                proof.verificationDigest().getBytes(StandardCharsets.UTF_8));
    }

    public boolean verifyChain(List<ComplianceStateProof> chain, String secretKey) {
        if (chain == null || chain.isEmpty()) {
            return false;
        }

        String currentPrev = GENESIS_PREVIOUS_HASH;
        long expectedSeq = 0;

        for (ComplianceStateProof proof : chain) {
            if (proof.sequenceNumber() != expectedSeq) {
                return false;
            }
            if (!proof.previousHash().equalsIgnoreCase(currentPrev)) {
                return false;
            }
            if (!verifyProof(proof, secretKey)) {
                return false;
            }
            currentPrev = proof.stateHash();
            expectedSeq++;
        }
        return true;
    }

    private String computeDigest(long sequenceNumber, String previousHash, String stateHash, long timestamp, String secretKey) {
        String payload = String.format("seq=%d|prev=%s|state=%s|ts=%d", sequenceNumber, previousHash, stateHash, timestamp);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 digest", e);
        }
    }
}
