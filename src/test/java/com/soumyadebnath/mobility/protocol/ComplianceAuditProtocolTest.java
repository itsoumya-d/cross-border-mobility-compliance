package com.soumyadebnath.mobility.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceAuditProtocolTest {

    private ComplianceAuditProtocol protocol;
    private final String secretKey = "test-compliance-secret-key-at-least-32-bytes";

    @BeforeEach
    void setUp() {
        protocol = new ComplianceAuditProtocol();
    }

    @Test
    void testNormalProofGenerationAndVerification() {
        String stateHash = protocol.computeStateHash("EMP-1001", "DE", 14, List.of("A1_CERTIFICATE", "POSTED_WORKER_NOTIFICATION"));
        assertNotNull(stateHash);
        assertEquals(64, stateHash.length());

        ComplianceStateProof proof = protocol.generateProof(0, ComplianceAuditProtocol.GENESIS_PREVIOUS_HASH, stateHash, 1732784400L, secretKey);
        assertTrue(protocol.verifyProof(proof, secretKey));
    }

    @Test
    void testEmptyInputHandling() {
        assertThrows(IllegalArgumentException.class, () ->
                protocol.computeStateHash("", "FR", 10, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                protocol.computeStateHash("EMP-1002", null, 10, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                protocol.generateProof(0, ComplianceAuditProtocol.GENESIS_PREVIOUS_HASH, "state-hash", 1732784400L, ""));
    }

    @Test
    void testMalformedProofDigest() {
        String stateHash = protocol.computeStateHash("EMP-1003", "NL", 7, List.of("A1_CERTIFICATE"));
        ComplianceStateProof proof = protocol.generateProof(0, ComplianceAuditProtocol.GENESIS_PREVIOUS_HASH, stateHash, 1732784400L, secretKey);

        ComplianceStateProof malformedProof = new ComplianceStateProof(
                proof.sequenceNumber(), proof.previousHash(), proof.stateHash(), proof.timestamp(), "deadbeef00000000000000000000000000000000000000000000000000000000");

        assertFalse(protocol.verifyProof(malformedProof, secretKey));
        assertFalse(protocol.verifyProof(proof, "wrong-secret-key"));
    }

    @Test
    void testZeroDayBoundaryTrip() {
        String stateHashZero = protocol.computeStateHash("EMP-1004", "CH", 0, List.of());
        assertNotNull(stateHashZero);
        assertEquals(64, stateHashZero.length());

        assertThrows(IllegalArgumentException.class, () ->
                protocol.computeStateHash("EMP-1004", "CH", -1, List.of()));
    }

    @Test
    void testTamperedStateHashDetection() {
        String stateHash = protocol.computeStateHash("EMP-1005", "ES", 30, List.of("PWN"));
        ComplianceStateProof proof = protocol.generateProof(0, ComplianceAuditProtocol.GENESIS_PREVIOUS_HASH, stateHash, 1732784400L, secretKey);

        ComplianceStateProof tamperedProof = new ComplianceStateProof(
                proof.sequenceNumber(), proof.previousHash(), "1111111111111111111111111111111111111111111111111111111111111111", proof.timestamp(), proof.verificationDigest());

        assertFalse(protocol.verifyProof(tamperedProof, secretKey));
    }

    @Test
    void testDeterministicCommitmentInvariance() {
        String hash1 = protocol.computeStateHash("EMP-1006", "IT", 21, List.of("TAX_REVIEW", "A1_CERT"));
        String hash2 = protocol.computeStateHash("EMP-1006", "IT", 21, List.of("A1_CERT", "TAX_REVIEW"));

        assertEquals(hash1, hash2, "Action order must not alter canonical state hash");

        ComplianceStateProof proof1 = protocol.generateProof(1, hash1, hash2, 1732784400L, secretKey);
        ComplianceStateProof proof2 = protocol.generateProof(1, hash1, hash2, 1732784400L, secretKey);

        assertEquals(proof1.verificationDigest(), proof2.verificationDigest());
    }

    @Test
    void testMultiStepChainVerification() {
        String hash0 = protocol.computeStateHash("EMP-1007", "AT", 5, List.of("A1_CERTIFICATE"));
        ComplianceStateProof step0 = protocol.generateProof(0, ComplianceAuditProtocol.GENESIS_PREVIOUS_HASH, hash0, 1732784400L, secretKey);

        String hash1 = protocol.computeStateHash("EMP-1007", "AT", 10, List.of("A1_CERTIFICATE", "PWN"));
        ComplianceStateProof step1 = protocol.generateProof(1, hash0, hash1, 1732784500L, secretKey);

        assertTrue(protocol.verifyChain(List.of(step0, step1), secretKey));

        // Broken continuity
        ComplianceStateProof brokenStep1 = protocol.generateProof(1, "badprevioushash", hash1, 1732784500L, secretKey);
        assertFalse(protocol.verifyChain(List.of(step0, brokenStep1), secretKey));
    }
}
