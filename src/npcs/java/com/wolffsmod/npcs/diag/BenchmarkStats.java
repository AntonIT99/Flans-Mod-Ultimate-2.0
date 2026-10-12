package com.wolffsmod.npcs.diag;

import java.util.Arrays;
import java.util.Locale;

/** A growing list of measurements and the figures a benchmark reports about them. Not thread-safe. */
public final class BenchmarkStats
{
    private double[] samples = new double[256];
    private int count;
    private boolean sorted = true;

    public void add(double sample)
    {
        if (count == samples.length)
            samples = Arrays.copyOf(samples, count * 2);
        samples[count++] = sample;
        sorted = false;
    }

    public int count()
    {
        return count;
    }

    public double sum()
    {
        double sum = 0D;
        for (int index = 0; index < count; index++)
            sum += samples[index];
        return sum;
    }

    public double mean()
    {
        return count == 0 ? 0D : sum() / count;
    }

    public double max()
    {
        return count == 0 ? 0D : sorted()[count - 1];
    }

    /**
     * @param share
     *            from 0 to 1
     * @return the smallest measurement that at least this share of all measurements does not exceed; 0 without any
     */
    public double percentile(double share)
    {
        if (count == 0)
            return 0D;
        int rank = (int) Math.ceil(Math.max(0D, Math.min(1D, share)) * count);
        return sorted()[Math.max(0, Math.min(count - 1, rank - 1))];
    }

    /** @return the mean of the largest hundredth of the measurements: for frame times, what the "1% low" frame rate comes from */
    public double worstPercentMean()
    {
        if (count == 0)
            return 0D;
        double[] ordered = sorted();
        int worst = Math.max(1, count / 100);
        double sum = 0D;
        for (int index = count - worst; index < count; index++)
            sum += ordered[index];
        return sum / worst;
    }

    /** @return mean, median, 95th and 99th percentile and maximum, with the given unit */
    public String summary(String unit)
    {
        return String.format(Locale.ROOT, "mean %.3f %s, median %.3f, p95 %.3f, p99 %.3f, max %.3f", mean(), unit, percentile(0.5D), percentile(0.95D), percentile(0.99D), max());
    }

    private double[] sorted()
    {
        if (!sorted)
        {
            Arrays.sort(samples, 0, count);
            sorted = true;
        }
        return samples;
    }
}
