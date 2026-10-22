package com.wolffsmod.npcs.diag;

import net.minecraft.util.Mth;

import java.util.Objects;

/** Duration, warm-up and a plain-text label of one diagnostic session; no gameplay settings are changed. */
public record BenchmarkOptions(int seconds, int warmUpSeconds, String label)
{
    public static final int MAX_WARM_UP_SECONDS = 60;
    private static final int MAX_LABEL_LENGTH = 120;

    public BenchmarkOptions
    {
        seconds = Mth.clamp(seconds, ServerBenchmark.MIN_SECONDS, ServerBenchmark.MAX_SECONDS);
        warmUpSeconds = Mth.clamp(warmUpSeconds, 0, MAX_WARM_UP_SECONDS);
        label = cleanLabel(label);
    }

    /** Keeps a label on one line in chat and reports, including when it came from a non-command caller. */
    public static String cleanLabel(String label)
    {
        String clean = Objects.requireNonNullElse(label, "").replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", " ").strip();
        return clean.codePointCount(0, clean.length()) <= MAX_LABEL_LENGTH ? clean : clean.substring(0, clean.offsetByCodePoints(0, MAX_LABEL_LENGTH));
    }

    /** A bounded filename component; the human-readable label is retained separately in the text report. */
    public String fileLabel()
    {
        String safe = label.replaceAll("[^a-zA-Z0-9_-]+", "-");
        int first = 0;
        int last = safe.length();
        while (first < last && safe.charAt(first) == '-')
            first++;
        while (last > first && safe.charAt(last - 1) == '-')
            last--;
        return safe.substring(first, Math.min(first + 40, last));
    }
}
