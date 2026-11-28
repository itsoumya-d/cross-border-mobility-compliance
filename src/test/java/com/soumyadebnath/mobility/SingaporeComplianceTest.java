package com.soumyadebnath.mobility;

import com.soumyadebnath.mobility.model.Trip;
import com.soumyadebnath.mobility.packs.CountryPackRegistry;
import com.soumyadebnath.mobility.packs.CountryRule;
import com.soumyadebnath.mobility.service.ComplianceEngine;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SingaporeComplianceTest {

    private final ComplianceEngine engine = new ComplianceEngine();
    private final CountryPackRegistry registry = engine.getPackRegistry();

    @Test
    void testSingaporeEPSalaryQualifyingThreshold() {
        // Source: https://www.mom.gov.sg/passes-and-permits/employment-pass/eligibility
        List<CountryRule> rules = registry.getRules("SG");
        CountryRule salaryRule = rules.stream()
            .filter(r -> r.id().equals("SG-MOM-EP-SALARY-001"))
            .findFirst()
            .orElseThrow();

        assertEquals(5600.0, salaryRule.thresholdValue());
        assertEquals("https://www.mom.gov.sg/passes-and-permits/employment-pass/eligibility", salaryRule.sourceUrl());

        // Trip with salary >= SGD 5,600 / month (SGD 72,000 / year = 7,200,000 cents) -> no salary shortfall
        Trip eligibleTrip = new Trip("t-sg-1", "e-1", "DE", "SG", "Relocation", LocalDate.now().plusDays(20), 30, 7200000L);
        var eligibleSummary = engine.assess(eligibleTrip);
        assertTrue(eligibleSummary.nextActions().stream().noneMatch(a -> a.documentType().equals("MOM_EP_SALARY")));

        // Trip with salary < SGD 5,600 / month (SGD 48,000 / year = 4,800,000 cents) -> triggers salary shortfall action
        Trip shortfallTrip = new Trip("t-sg-2", "e-2", "DE", "SG", "Assignment", LocalDate.now().plusDays(20), 30, 4800000L);
        var shortfallSummary = engine.assess(shortfallTrip);
        assertTrue(shortfallSummary.nextActions().stream().anyMatch(a -> a.documentType().equals("MOM_EP_SALARY")));
    }

    @Test
    void testSingaporeCOMPASSSufficientPoints() {
        // Source: https://www.mom.gov.sg/passes-and-permits/employment-pass/eligibility/compass
        List<CountryRule> rules = registry.getRules("SG");
        CountryRule compassRule = rules.stream()
            .filter(r -> r.id().equals("SG-MOM-COMPASS-POINTS-002"))
            .findFirst()
            .orElseThrow();

        assertEquals(40.0, compassRule.thresholdValue());
        assertEquals("https://www.mom.gov.sg/passes-and-permits/employment-pass/eligibility/compass", compassRule.sourceUrl());

        Trip trip = new Trip("t-sg-3", "e-1", "US", "SG", "Transfer", LocalDate.now().plusDays(15), 45, 9000000L);
        var summary = engine.assess(trip);
        assertTrue(summary.nextActions().stream().anyMatch(a -> a.documentType().equals("MOM_COMPASS")));
    }

    @Test
    void testSingaporeIRASShortTermStayTaxExemption() {
        // Source: https://www.iras.gov.sg/taxes/individual-income-tax/basics-of-individual-income-tax/tax-residency-and-tax-rates/individual-income-tax-rates
        List<CountryRule> rules = registry.getRules("SG");
        CountryRule taxRule = rules.stream()
            .filter(r -> r.id().equals("SG-IRAS-TAX-EXPOSURE-003"))
            .findFirst()
            .orElseThrow();

        assertEquals(60.0, taxRule.thresholdValue());
        assertEquals("https://www.iras.gov.sg/taxes/individual-income-tax/basics-of-individual-income-tax/tax-residency-and-tax-rates/individual-income-tax-rates", taxRule.sourceUrl());

        // Trip of 30 days <= 60 days -> exempt
        Trip shortTrip = new Trip("t-sg-4", "e-1", "DE", "SG", "Project visit", LocalDate.now().plusDays(10), 30, 8500000L);
        var summary = engine.assess(shortTrip);
        assertTrue(summary.nextActions().stream().anyMatch(a -> a.documentType().equals("IRAS_60_EXEMPT")));
        assertTrue(summary.nextActions().stream().noneMatch(a -> a.documentType().equals("IRAS_TAX_REVIEW")));
    }

    @Test
    void testSingaporeIRASExtendedStayTaxExposure() {
        // Source: https://www.iras.gov.sg/taxes/individual-income-tax/basics-of-individual-income-tax/tax-residency-and-tax-rates/individual-income-tax-rates
        // Trip of 90 days > 60 days -> triggers non-resident tax review
        Trip extendedTrip = new Trip("t-sg-5", "e-1", "DE", "SG", "Extended assignment", LocalDate.now().plusDays(10), 90, 8500000L);
        var summary = engine.assess(extendedTrip);
        assertTrue(summary.nextActions().stream().anyMatch(a -> a.documentType().equals("IRAS_TAX_REVIEW")));
        assertTrue(summary.nextActions().stream().noneMatch(a -> a.documentType().equals("IRAS_60_EXEMPT")));
    }

    @Test
    void testUnsupportedCountryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> registry.getRules("XX"));
    }
}
