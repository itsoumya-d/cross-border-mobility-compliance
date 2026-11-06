package com.soumyadebnath.mobility.model;

import java.time.LocalDate;

public record NextAction(
    String title,
    String plainLanguage,
    String internalTerm,
    String workflexTerm,
    String owner,
    LocalDate dueDate,
    boolean required,
    String documentType
) {}
