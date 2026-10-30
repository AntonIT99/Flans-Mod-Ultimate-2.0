package com.flansmodultimate.platform;

import net.minecraftforge.fml.loading.progress.ProgressMeter;
import net.minecraftforge.fml.loading.progress.StartupNotificationManager;

/**
 * Loader boundary for the early loading screen. Messages and bars only show while the early window is
 * displayed (mod construction and setup); elsewhere, and on a dedicated server, they are harmless no-ops.
 */
public final class PlatformLoadingProgress
{
    private static final int MAX_LABEL_LENGTH = 80;

    private PlatformLoadingProgress()
    {}

    public static void message(String message)
    {
        StartupNotificationManager.addModMessage(shorten(message));
    }

    public static Bar bar(String name, int steps)
    {
        return new Bar(StartupNotificationManager.addProgressBar(name, Math.max(1, steps)));
    }

    private static String shorten(String text)
    {
        return text.length() <= MAX_LABEL_LENGTH ? text : text.substring(0, MAX_LABEL_LENGTH - 3) + "...";
    }

    /** A progress bar on the early loading screen. */
    public static final class Bar implements AutoCloseable
    {
        private final ProgressMeter meter;

        private Bar(ProgressMeter meter)
        {
            this.meter = meter;
        }

        public void label(String label)
        {
            meter.label(shorten(label));
        }

        public void increment()
        {
            meter.increment();
        }

        @Override
        public void close()
        {
            meter.complete();
        }
    }
}
