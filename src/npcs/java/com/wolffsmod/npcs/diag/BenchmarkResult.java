package com.wolffsmod.npcs.diag;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * The headline figures of one finished server benchmark, kept so the Server page can set the latest run beside the one
 * before it.
 *
 * @param label
 *            the session label, empty for an unnamed run
 * @param ticks
 *            ticks measured
 * @param figures
 *            one value per {@link Figure}, in its order
 */
public record BenchmarkResult(String label, int ticks, boolean aborted, List<Double> figures)
{
    private static final int MAX_LABEL = 64;
    /** More figures than any version sends, so a corrupt packet cannot ask for a huge list. */
    private static final int MAX_FIGURES = 64;

    /** A figure of a run, and whether a smaller value is the better one. */
    public enum Figure
    {
        TICK_MEAN(true), TICK_P95(true), NPC_MILLIS(true), NPC_SHARE(true), NPCS_TICKING(false), PATHS_PER_TICK(true), PATH_MILLIS(true), TARGET_MILLIS(true);

        private final boolean lowerIsBetter;

        Figure(boolean lowerIsBetter)
        {
            this.lowerIsBetter = lowerIsBetter;
        }

        public boolean lowerIsBetter()
        {
            return lowerIsBetter;
        }
    }

    public double figure(Figure figure)
    {
        return figure.ordinal() < figures.size() ? figures.get(figure.ordinal()) : 0D;
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(label, MAX_LABEL);
        buffer.writeVarInt(ticks);
        buffer.writeBoolean(aborted);
        buffer.writeVarInt(figures.size());
        figures.forEach(buffer::writeDouble);
    }

    public static BenchmarkResult read(FriendlyByteBuf buffer)
    {
        String label = buffer.readUtf(MAX_LABEL);
        int ticks = buffer.readVarInt();
        boolean aborted = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_FIGURES)
            throw new IllegalArgumentException("Too many benchmark figures: " + count);
        List<Double> figures = new ArrayList<>(count);
        for (int index = 0; index < count; index++)
            figures.add(buffer.readDouble());
        return new BenchmarkResult(label, ticks, aborted, List.copyOf(figures));
    }
}
