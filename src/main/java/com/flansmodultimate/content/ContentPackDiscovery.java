package com.flansmodultimate.content;

import com.mojang.logging.LogUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** One deterministic selection of standalone packs for both type loading and resource discovery. */
final class ContentPackDiscovery
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private ContentPackDiscovery() {}

    static Map<String, Path> select(Path root, Set<Path> excluded) throws IOException
    {
        Map<String, Path> selected = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(root))
        {
            for (Path path : files.filter(file -> !excluded.contains(file.toAbsolutePath().normalize()))
                .filter(file -> priority(file) < 3)
                .sorted(Comparator.comparingInt(ContentPackDiscovery::priority)
                    .thenComparing(file -> file.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(file -> file.getFileName().toString())).toList())
            {
                String basename = Files.isDirectory(path) ? path.getFileName().toString()
                    : FilenameUtils.getBaseName(path.getFileName().toString());
                Path previous = selected.putIfAbsent(basename.toLowerCase(Locale.ROOT), path);
                if (previous != null)
                    LOGGER.warn("Ignoring duplicate content pack '{}'; selected '{}' (directory, then ZIP, then JAR).", path.getFileName(), previous.getFileName());
            }
        }
        Map<String, Path> byFilename = new LinkedHashMap<>();
        selected.values().stream().sorted(Comparator.comparing(path -> path.getFileName().toString()))
            .forEach(path -> byFilename.put(path.getFileName().toString(), path));
        return byFilename;
    }

    private static int priority(Path path)
    {
        if (Files.isDirectory(path))
            return 0;
        if (!Files.isRegularFile(path))
            return 3;
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".zip"))
            return 1;
        else if (name.endsWith(".jar"))
            return 2;
        else
            return 3;
    }
}
