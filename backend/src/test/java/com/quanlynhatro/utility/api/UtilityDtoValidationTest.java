package com.quanlynhatro.utility.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

class UtilityDtoValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void tearDown() {
        validator = null;
    }

    @Test
    void utilityReadingRequiresYearMonthPeriodAndNonNegativeValues() {
        UtilityReadingCreateRequest request =
                new UtilityReadingCreateRequest(
                        1L,
                        UtilityType.ELECTRICITY,
                        "2026-13",
                        new BigDecimal("10.000"),
                        new BigDecimal("9.000"),
                        LocalDate.of(2026, 10, 1),
                        null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void validUtilityReadingRequestPassesFieldValidation() {
        UtilityReadingCreateRequest request =
                new UtilityReadingCreateRequest(
                        1L,
                        UtilityType.WATER,
                        "2026-10",
                        new BigDecimal("10.000"),
                        new BigDecimal("12.500"),
                        LocalDate.of(2026, 10, 31),
                        "monthly reading");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void feeConfigRejectsNegativePriceAndMissingCode() {
        FeeConfigCreateRequest request =
                new FeeConfigCreateRequest(
                        "",
                        "Electricity",
                        "kWh",
                        new BigDecimal("-1.00"),
                        FeePricingMode.PER_USAGE,
                        LocalDate.of(2026, 1, 1),
                        null,
                        true);

        assertFalse(validator.validate(request).isEmpty());
    }
}
