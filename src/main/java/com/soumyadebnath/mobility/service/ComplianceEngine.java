package com.soumyadebnath.mobility.service;

import com.soumyadebnath.mobility.compliance.CreditLedger;
import com.soumyadebnath.mobility.model.ComplianceSummary;
import com.soumyadebnath.mobility.model.NextAction;
import com.soumyadebnath.mobility.model.Trip;
import com.soumyadebnath.mobility.packs.CountryPackRegistry;
import com.soumyadebnath.mobility.packs.CountryRule;
import com.soumyadebnath.mobility.terminology.TerminologyMap;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Deterministic rule engine for cross-border mobility compliance.
 * Covers A1/CoC, Posted Worker Notification (PWN), Permanent Establishment
 * risk, visa/immigration, tax exposure, duty of care, and modular Country Packs (e.g. Singapore MOM/IRAS).
 */
@Service
public class ComplianceEngine {

    private static final Set<String> EU_PWN_DESTINATIONS = Set.of(
        "DE", "FR", "NL", "ES", "IT", "BE", "AT", "PL", "PT", "SE", "CH", "NO", "IE", "DK", "FI", "CZ", "HU", "LU", "BG", "HR", "RO", "SK", "SI", "LT", "LV", "EE", "MT", "CY", "GR", "GB"
    );

    private static final Set<String> VISA_DESTINATIONS = Set.of("US", "GB", "AE", "SG", "IN", "AU", "CA");

    private final CountryPackRegistry packRegistry;

    public ComplianceEngine() {
        this(new CountryPackRegistry());
    }

    public ComplianceEngine(CountryPackRegistry packRegistry) {
        this.packRegistry = packRegistry;
    }

    public ComplianceSummary assess(Trip trip) {
        boolean pwnRequired = EU_PWN_DESTINATIONS.contains(trip.destinationCountry())
            && !trip.originCountry().equals(trip.destinationCountry());
        boolean a1Required = !trip.originCountry().equals(trip.destinationCountry());
        boolean visaRequired = VISA_DESTINATIONS.contains(trip.destinationCountry())
            && !trip.originCountry().equals(trip.destinationCountry());
        boolean peRisk = trip.durationDays() > 183;
        boolean taxRisk = trip.durationDays() > 90;

        List<String> docs = new ArrayList<>();
        List<NextAction> actions = new ArrayList<>();
        LocalDate start = trip.startDate() != null ? trip.startDate() : LocalDate.now();

        if (a1Required) {
            docs.add("A1 / Certificate of Coverage");
            actions.add(new NextAction(
                "Get your A1 certificate",
                "Your employer files an A1 form so you keep social security in your home country while abroad. You will receive a PDF copy.",
                "A1 cert",
                TerminologyMap.toWorkflexTerm("A1 cert"),
                "HR",
                start.minusDays(5),
                true,
                "A1"
            ));
        }
        if (pwnRequired) {
            docs.add("Posted Worker Notification");
            actions.add(new NextAction(
                "Confirm your posted-worker details",
                "The destination country needs the client/host address, start date and agreed salary before you start work. WorkFlex files this for you.",
                "PWN",
                TerminologyMap.toWorkflexTerm("PWN"),
                "TRAVELER",
                start.minusDays(5),
                true,
                "PWN"
            ));
        }
        if (visaRequired) {
            docs.add("ETA / visa clearance");
            actions.add(new NextAction(
                "Check visa or ETA requirements",
                "Confirm whether a visa or electronic travel authorisation is needed for your destination and nationality, and upload the approval once granted.",
                "Travel visa check",
                "Visa & immigration",
                "TRAVELER",
                start.minusDays(14),
                true,
                "VISA"
            ));
        }

        // Modular Country Pack Evaluation (Singapore MOM / IRAS)
        if ("SG".equalsIgnoreCase(trip.destinationCountry())) {
            List<CountryRule> sgRules = packRegistry.getRules("SG");
            docs.add("Singapore Mobility Compliance Dossier");

            // Evaluate IRAS 60-day rule
            if (trip.durationDays() <= 60) {
                actions.add(new NextAction(
                    "IRAS 60-day tax exemption clearance",
                    "Under IRAS rules (https://www.iras.gov.sg/taxes/individual-income-tax/basics-of-individual-income-tax/tax-residency-and-tax-rates/individual-income-tax-rates), short-term employment of 60 days or less is exempt from Singapore employment tax.",
                    "IRAS short-term exemption",
                    "Tax exemption",
                    "SYSTEM",
                    start,
                    false,
                    "IRAS_60_EXEMPT"
                ));
            } else {
                actions.add(new NextAction(
                    "IRAS non-resident tax withholding review",
                    "Stays exceeding 60 days are subject to IRAS non-resident tax at 15% flat or progressive rates (https://www.iras.gov.sg/taxes/individual-income-tax/basics-of-individual-income-tax/tax-residency-and-tax-rates/individual-income-tax-rates).",
                    "IRAS tax withholding",
                    "Wage tax",
                    "HR",
                    start.minusDays(14),
                    true,
                    "IRAS_TAX_REVIEW"
                ));
            }

            // Evaluate MOM EP Salary threshold (SGD 5,600/month)
            double monthlySalarySgd = (trip.annualSalaryCents() / 100.0) / 12.0;
            if (monthlySalarySgd < 5600.0) {
                actions.add(new NextAction(
                    "MOM Employment Pass salary shortfall review",
                    "Monthly salary of SGD " + Math.round(monthlySalarySgd) + " is below the MOM EP threshold of SGD 5,600 (https://www.mom.gov.sg/passes-and-permits/employment-pass/eligibility).",
                    "EP salary threshold",
                    "Visa & immigration",
                    "HR",
                    start.minusDays(30),
                    true,
                    "MOM_EP_SALARY"
                ));
            }

            // COMPASS Points Assessment
            actions.add(new NextAction(
                "MOM COMPASS framework points verification",
                "Verify candidate scores at least 40 points on COMPASS criteria (https://www.mom.gov.sg/passes-and-permits/employment-pass/eligibility/compass).",
                "COMPASS verification",
                "Visa & immigration",
                "HR",
                start.minusDays(21),
                true,
                "MOM_COMPASS"
            ));
        }

        if (peRisk) {
            actions.add(new NextAction(
                "Permanent-establishment review",
                "A stay over 183 days can create a taxable presence for your employer. HR and tax review this before approval.",
                "PE risk",
                "Permanent establishment risk",
                "HR",
                start.minusDays(30),
                taxRisk,
                "PE_REVIEW"
            ));
        }
        if (taxRisk) {
            docs.add("Tax exposure note");
            actions.add(new NextAction(
                "Review tax exposure",
                "Extended trips can affect wage tax. Your employer checks payroll treatment for the destination.",
                "Tax check",
                "Wage tax",
                "HR",
                start.minusDays(30),
                false,
                "TAX_NOTE"
            ));
        }
        actions.add(new NextAction(
            "Travel insurance & duty-of-care cover",
            "Your trip includes travel insurance and 24/7 emergency contact details. Save the SOS number offline.",
            "Travel insurance",
            TerminologyMap.toWorkflexTerm("Travel insurance"),
            "SYSTEM",
            start,
            false,
            "INSURANCE"
        ));

        boolean compliant = actions.stream().filter(NextAction::required).count() <= 3;
        var price = CreditLedger.price(trip.durationDays(), pwnRequired, a1Required, visaRequired);
        return new ComplianceSummary(trip.id(), compliant, docs, actions, price.totalCredits(), price.explanation());
    }

    public CountryPackRegistry getPackRegistry() {
        return packRegistry;
    }
}
