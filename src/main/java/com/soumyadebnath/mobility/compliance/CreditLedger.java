package com.soumyadebnath.mobility.compliance;

/**
 * Transparent per-trip credit breakdown.
 * Addresses the G2 review finding that the credit/charging logic is not
 * intuitive at first use.
 */
public final class CreditLedger {

    public record Breakdown(long baseCredits, long complianceCredits, long documentsCredits, long totalCredits, String explanation) {}

    public static Breakdown price(int durationDays, boolean pwnRequired, boolean a1Required, boolean visaRequired) {
        long base = 40;
        long compliance = 15;
        long documents = (a1Required ? 8 : 0) + (pwnRequired ? 12 : 0) + (visaRequired ? 10 : 0);
        if (durationDays > 90) {
            compliance += 5; // long-term / assignment review
        }
        long total = base + compliance + documents;
        String explanation = String.format(
            "Base %d + compliance %d + documents %d (A1 %s, PWN %s, visa %s) = %d credits",
            base, compliance, documents,
            a1Required ? "8" : "0", pwnRequired ? "12" : "0", visaRequired ? "10" : "0",
            total
        );
        return new Breakdown(base, compliance, documents, total, explanation);
    }
}
