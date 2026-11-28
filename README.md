# Cross-Border Mobility Compliance

A production-oriented reference for global-mobility compliance: A1 / Certificate of Coverage (CoC), Posted Worker Notification (PWN), visa & immigration, permanent-establishment risk, tax exposure and duty of care — the exact dimensions WorkFlex models in its public compliance handbook.

## Stack (exact JD parity)

- **Backend:** Java 17, Spring Boot 3.3 (REST, validation, actuator), JUnit 5
- **Frontend:** Angular 17 (standalone components), TypeScript, HttpClient
- **API style:** OAuth2 client-credentials compatible (`/api/v1/status`, employees, trips, next-actions, compliance-summary) — mirrors the public WorkFlex Open-API (employee provisioning, trip creation, compliance documents)
- **Infra:** Dockerfile (Eclipse Temurin 17 JRE), GitHub Actions CI (backend-test + frontend-build)

## The `rea` fix — traveler next-action inbox

G2 reviewers (4.8/5, 27 reviews) consistently flag two UX gaps:

1. **Credit / charging logic is not intuitive** on first use.
2. **Next required action is not transparent** for first-time travelers; standardised wording does not match clients' internal terminology.

This repo ships a fix for both:

- `ComplianceEngine.assess()` returns an ordered `NextAction` list with plain-language title, internal → WorkFlex term mapping, owner, due date and document type.
- `TerminologyMap` maps internal vocabulary (`WFA`, `PWN`, `A1 cert`) to canonical WorkFlex terms.
- `CreditLedger.price()` returns a full transparent breakdown (base + compliance + documents) with an explanation string.
- Angular `TravelerChecklistComponent` renders the next-action inbox with badge, due date and required flag.

## Run

```bash
# Backend
gradle test --no-daemon
gradle bootRun --no-daemon
# Frontend
cd frontend && npm ci && npm run build
```

## Tests

- 5 engine/ledger/terminology unit tests
- 2 controller MockMvc tests (status + next-actions)
- 7 protocol unit tests (canonical hashing, proof validation, chain continuity, tamper detection)

## Verifiable Compliance Audit Protocol

To provide cryptographically verifiable compliance transitions without external dependencies, `ComplianceAuditProtocol` computes deterministic state commitments and generates verifiable execution proofs:

- **State Hashing:** Canonical SHA-256 digest over normalized employee identifier, normalized destination country, trip duration, and lexicographically sorted compliance actions.
- **State Proofs:** `ComplianceStateProof` links sequence numbers, previous state commitments, current state hashes, and timestamp with an HMAC-SHA256 verification digest.
- **Chain Verification:** `verifyChain` validates sequence continuity and cryptographic linking across multi-step evaluations.

### Runnable Example

```java
ComplianceAuditProtocol protocol = new ComplianceAuditProtocol();
String stateHash = protocol.computeStateHash("EMP-1001", "DE", 14, List.of("A1_CERTIFICATE", "POSTED_WORKER_NOTIFICATION"));
ComplianceStateProof proof = protocol.generateProof(0, ComplianceAuditProtocol.GENESIS_PREVIOUS_HASH, stateHash, System.currentTimeMillis() / 1000, secretKey);

boolean isValid = protocol.verifyProof(proof, secretKey);
// isValid == true
```

### Protocol Limits
- Verifies deterministic commitment consistency and chain continuity in-memory; external distributed consensus anchors are not implemented in this reference layer.

## Truthfulness boundary

Rule set is deterministic and illustrative — it does not file A1/PWN applications with real authorities and does not constitute legal advice. Production filing requires licensed compliance counsel and integration with national portals (e.g. EasyGov.swiss for Switzerland, Altinn for Norway).
