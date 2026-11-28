package com.soumyadebnath.mobility.packs;

public record CountryRule(
    String id,
    String decision,
    String metric,
    double thresholdValue,
    String category,
    String sourceUrl,
    String checkedOn,
    String description
) {}
