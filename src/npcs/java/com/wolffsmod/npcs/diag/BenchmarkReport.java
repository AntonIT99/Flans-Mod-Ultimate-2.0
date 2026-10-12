package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.NpcsMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** The outcome of a benchmark run: a readable summary and the raw measurements, written side by side under the logs folder. */
public final class BenchmarkReport
{
    private static final String FOLDER = "logs/" + NpcsMod.MOD_ID + "-benchmarks";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final String kind;
    private final List<String> summary = new ArrayList<>();
    private final List<String> rows = new ArrayList<>();

    /**
     * @param kind
     *            "server" or "client", which names the files
     * @param header
     *            comma-separated column names of the raw measurements
     */
    public BenchmarkReport(String kind, String header)
    {
        this.kind = kind;
        rows.add(header);
    }

    public BenchmarkReport line(String text)
    {
        summary.add(text);
        return this;
    }

    public void row(String values)
    {
        rows.add(values);
    }

    public List<String> summary()
    {
        return List.copyOf(summary);
    }

    /** @return the summary file, or empty when the files could not be written */
    public Optional<Path> write()
    {
        try
        {
            Path folder = FMLPaths.GAMEDIR.get().resolve(FOLDER);
            Files.createDirectories(folder);
            String name = kind + "-" + LocalDateTime.now(ZoneId.systemDefault()).format(STAMP);
            Path text = folder.resolve(name + ".txt");
            Files.write(text, summary);
            Files.write(folder.resolve(name + ".csv"), rows);
            return Optional.of(text);
        }
        catch (IOException exception)
        {
            NpcsMod.log.warn("Could not write the {} benchmark report", kind, exception);
            return Optional.empty();
        }
    }
}
