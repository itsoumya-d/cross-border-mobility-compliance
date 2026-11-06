package com.soumyadebnath.mobility.service;

import com.soumyadebnath.mobility.compliance.CreditLedger;
import com.soumyadebnath.mobility.model.ComplianceSummary;
import com.soumyadebnath.mobility.model.NextAction;
import com.soumyadebnath.mobility.model.Trip;
import com.soumyadebnath.mobility.terminology.TerminologyMap;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Deterministic rule engine for cross-border mobility compliance.
 * Covers A1/CoC, Posted Worker Notification (PWN), Permanent Establishment
 * risk, visa/immigration, tax exposure and duty of care — the same
 * dimensions WorkFlex models in its public compliance handbook.
 */
@Service
public class ComplianceEngine {

    private static final Set<String> EU_PWN_DESTINATIONS = Set.of(
        "DE", "FR", "NL", "ES", "IT", "BE", "AT", "PL", "PT", "SE", "CH", "NO", "IE", "DK", "FI", "CZ", "HU", "LU", "BG", "HR", "RO", "SK", "SI", "LT", "LV", "EE", "MT", "CY", "GR", "GB"
    );

    private static final Set<String> VISA_DESTINATIONS = Set.of("US", "GB", "AE", "SG", "IN", "AU", "CA");

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
}
