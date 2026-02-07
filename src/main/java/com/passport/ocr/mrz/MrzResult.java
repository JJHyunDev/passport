package com.passport.ocr.mrz;

public record MrzResult(
    String passportNumber,
    String fullName,
    String nationality,
    String dateOfBirth,
    String sex,
    String dateOfExpiry,
    String issuingCountry,
    String raw
) {}
