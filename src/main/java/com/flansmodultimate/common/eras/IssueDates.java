package com.flansmodultimate.common.eras;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.platform.PlatformPaths;
import com.flansmodultimate.util.FlansLog;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Where the server's eras and date overrides live, and the one place that says when an item was in service: a server
 * override first, then the item's own {@code IssueDate} and {@code RetirementDate}, which categories may already have
 * set or replaced.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IssueDates
{
    public static final String ERAS_FILE = "eras.json";
    public static final String OVERRIDES_FILE = "issue-dates.json";
    private static final long MAX_ERAS_BYTES = 256L * 1024;

    private static final AtomicReference<EraCatalog> eras = new AtomicReference<>(EraCatalog.defaults());
    private static final AtomicReference<IssueDateOverrides> overrides = new AtomicReference<>(new IssueDateOverrides(Path.of(OVERRIDES_FILE)));

    /** Loads both files from the mod's config folder. Called before content packs are read, so their eras can be checked. */
    public static void loadAll()
    {
        Path directory = PlatformPaths.configDir().resolve(FlansMod.MOD_ID);
        eras.set(loadEras(directory.resolve(ERAS_FILE)));
        IssueDateOverrides loaded = new IssueDateOverrides(directory.resolve(OVERRIDES_FILE));
        loaded.load().forEach(problem -> FlansLog.log.warn("{}", problem));
        overrides.set(loaded);
    }

    public static EraCatalog eras()
    {
        return eras.get();
    }

    public static IssueDateOverrides overrides()
    {
        return overrides.get();
    }

    /** When the item was in service, or {@code null} when nothing dates it. */
    @Nullable
    public static ServicePeriod periodOf(InfoType type)
    {
        return overrides.get().get(type.getShortName()).orElse(type.getServicePeriod());
    }

    /**
     * Reads the era catalog, writing the defaults first when the file does not exist. An unreadable file falls back to
     * the defaults and is left as it is, so the owner can fix it.
     */
    static EraCatalog loadEras(Path file)
    {
        try
        {
            if (!Files.exists(file))
            {
                Path parent = file.toAbsolutePath().getParent();
                if (parent != null)
                    Files.createDirectories(parent);
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(EraCatalog.defaults().toJson()), StandardCharsets.UTF_8);
                return EraCatalog.defaults();
            }
            if (Files.size(file) > MAX_ERAS_BYTES)
                throw new IOException("the file exceeds " + MAX_ERAS_BYTES + " bytes");
            EraCatalog.Loaded loaded = EraCatalog.fromJson(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject());
            loaded.problems().forEach(problem -> FlansLog.log.warn("{}: {}", file.getFileName(), problem));
            return loaded.catalog();
        }
        catch (IOException | RuntimeException exception)
        {
            FlansLog.log.error("Could not read {}; using the default eras", file.toAbsolutePath(), exception);
            return EraCatalog.defaults();
        }
    }
}
