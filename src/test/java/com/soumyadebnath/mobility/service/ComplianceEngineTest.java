package com.soumyadebnath.mobility.service;

import com.soumyadebnath.mobility.compliance.CreditLedger;
import com.soumyadebnath.mobility.model.Trip;
import com.soumyadebnath.mobility.terminology.TerminologyMap;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceEngineTest {

    private final ComplianceEngine engine = new ComplianceEngine();

    @Test
    void intraEuTripRequiresA1AndPwnWithNextActions() {
        Trip trip = new Trip("t1", "e1", "DE", "FR", "Business trip", LocalDate.now().plusDays(10), 14, 9500000L);
        var summary = engine.assess(trip);
        assertTrue(summary.requiredDocuments().contains("A1 / Certificate of Coverage"));
        assertTrue(summary.requiredDocuments().contains("Posted Worker Notification"));
        assertFalse(summary.nextActions().isEmpty());
        assertTrue(summary.nextActions().stream().anyMatch(a -> a.documentType().equals("A1")));
        assertTrue(summary.nextActions().stream().anyMatch(a -> a.documentType().equals("PWN")));
    }

    @Test
    void domesticTripHasNoA1OrPwn() {
        Trip trip = new Trip("t2", "e1", "DE", "DE", "Domestic", LocalDate.now().plusDays(10), 7, 9500000L);
        var summary = engine.assess(trip);
        assertFalse(summary.requiredDocuments().contains("A1 / Certificate of Coverage"));
        assertFalse(summary.requiredDocuments().contains("Posted Worker Notification"));
    }

    @Test
    void longTripFlagsPermanentEstablishmentRisk() {
        Trip trip = new Trip("t3", "e1", "DE", "NL", "Assignment", LocalDate.now().plusDays(10), 210, 9500000L);
        var summary = engine.assess(trip);
        assertTrue(summary.nextActions().stream().anyMatch(a -> a.documentType().equals("PE_REVIEW")));
    }

    @Test
    void creditLedgerIsTransparent() {
        var price = CreditLedger.price(14, true, true, false);
        assertTrue(price.totalCredits() > 0);
        assertTrue(price.explanation().contains("Base"));
        assertTrue(price.explanation().contains("documents"));
    }

    @Test
    void terminologyMappingResolvesInternalTerms() {
        assertEquals("Workation", TerminologyMap.toWorkflexTerm("WFA"));
        assertEquals("Business trip", TerminologyMap.toWorkflexTerm("Business travel request"));
        assertEquals("Custom term", TerminologyMap.toWorkflexTerm("Custom term"));
    }
}
