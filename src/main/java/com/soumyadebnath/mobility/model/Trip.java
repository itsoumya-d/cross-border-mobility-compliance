package com.soumyadebnath.mobility.model;

import java.time.LocalDate;

public record Trip(
    String id,
    String employeeId,
    String originCountry,
    String destinationCountry,
    String purpose,
    LocalDate startDate,
    int durationDays,
    long annualSalaryCents
) {}
