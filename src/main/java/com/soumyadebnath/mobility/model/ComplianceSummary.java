package com.soumyadebnath.mobility.model;

import java.util.List;

public record ComplianceSummary(
    String tripId,
    boolean compliant,
    List<String> requiredDocuments,
    List<NextAction> nextActions,
    long estimatedCredits,
    String creditBreakdown
) {}
