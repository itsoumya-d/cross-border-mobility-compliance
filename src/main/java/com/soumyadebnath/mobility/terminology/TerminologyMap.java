package com.soumyadebnath.mobility.terminology;

import java.util.Map;

/**
 * Maps a client's internal vocabulary to WorkFlex canonical terms.
 * Addresses the G2 review finding that standardised icons/wording do not
 * match internal terminology.
 */
public final class TerminologyMap {

    private static final Map<String, String> MAP = Map.of(
        "WFA request", "Work-from-anywhere trip",
        "WFA", "Workation",
        "Business travel request", "Business trip",
        "Travel request", "Trip",
        "Compliance check", "Risk assessment",
        "Travel insurance", "Duty-of-care cover",
        "PWN", "Posted Worker Notification (PWN)",
        "A1 cert", "A1 / Certificate of Coverage (CoC)"
    );

    public static String toWorkflexTerm(String internalTerm) {
        if (internalTerm == null) {
            return null;
        }
        return MAP.getOrDefault(internalTerm, internalTerm);
    }

    public static Map<String, String> all() {
        return MAP;
    }
}
