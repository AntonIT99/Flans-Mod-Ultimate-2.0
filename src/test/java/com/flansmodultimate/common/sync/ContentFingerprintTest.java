package com.flansmodultimate.common.sync;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentFingerprintTest
{
    @Test
    void reportsNothingWhenBothSidesLoadedTheSamePacks()
    {
        Map<String, String> side = Map.of("Simple Parts", "aaa", "Modern Warfare", "bbb");

        assertTrue(ContentFingerprint.compare(side, side).isEmpty());
    }

    @Test
    void reportsAPackWhoseDefinitionsDiffer()
    {
        Map<String, EnumContentMismatch> mismatches = ContentFingerprint.compare(
            Map.of("Simple Parts", "aaa", "Modern Warfare", "bbb"),
            Map.of("Simple Parts", "aaa", "Modern Warfare", "changed"));

        assertEquals(Map.of("Modern Warfare", EnumContentMismatch.DIFFERENT), mismatches);
    }

    @Test
    void separatesAPackWeLackFromOneTheyLack()
    {
        Map<String, EnumContentMismatch> mismatches = ContentFingerprint.compare(
            Map.of("Simple Parts", "aaa", "Mine Only", "ccc"),
            Map.of("Simple Parts", "aaa", "Theirs Only", "ddd"));

        assertEquals(Map.of(
            "Theirs Only", EnumContentMismatch.MISSING,
            "Mine Only", EnumContentMismatch.UNKNOWN_TO_THEM), mismatches);
    }

    @Test
    void reportsEveryPackWhenASideLoadedNothing()
    {
        Map<String, EnumContentMismatch> mismatches = ContentFingerprint.compare(
            Map.of(),
            Map.of("Simple Parts", "aaa", "Modern Warfare", "bbb"));

        assertEquals(Map.of(
            "Simple Parts", EnumContentMismatch.MISSING,
            "Modern Warfare", EnumContentMismatch.MISSING), mismatches);
    }
}
