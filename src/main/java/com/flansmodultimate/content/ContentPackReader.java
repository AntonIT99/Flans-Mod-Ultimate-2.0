package com.flansmodultimate.content;

import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.TextDecoding;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static com.flansmodultimate.content.ContentPackPaths.*;

/**
 * Reads and decodes the definition files of the content packs ahead of their registration.
 * <p>
 * Registration stays on the loading thread, one pack after the other. Meanwhile the workers read
 * the files of the pack being registered and of the next {@value #PACKS_AHEAD} packs, each pack's
 * files in parallel. Reading only produces values: it changes no shared state, so the packs load
 * exactly as they would on one thread.
 */
final class ContentPackReader
{
    /** Packs read ahead of the one being registered; the text of each is held until its turn. */
    static final int PACKS_AHEAD = 2;

    private static final Set<String> ALIAS_FILES = Set.of(ID_ALIAS_FILE, ARMOR_TEXTURES_ALIAS_FILE, GUI_TEXTURES_ALIAS_FILE, SKINS_TEXTURES_ALIAS_FILE);

    /**
     * @param typeFiles
     *            the definitions, in the order the pack lists them
     * @param aliasFiles
     *            the alias files present at the root of the pack
     */
    record Result(List<TypeFile> typeFiles, Set<String> aliasFiles)
    {}

    private record Source(Path file, String folderName, @Nullable EnumType type)
    {}

    private final List<IContentProvider> providers;
    private final ContentLoadingWorkers workers;
    private final List<ContentLoadingWorkers.Task<Result>> tasks;

    ContentPackReader(List<IContentProvider> providers, ContentLoadingWorkers workers)
    {
        this.providers = List.copyOf(providers);
        this.workers = workers;
        tasks = new ArrayList<>(this.providers.size());
    }

    /** Waits for the files of the pack, the next one in order, and starts reading the packs after it. */
    Result take(IContentProvider provider)
    {
        int index = indexOf(provider);
        if (index < 0)
            return read(provider, workers);
        int last = workers.isParallel() ? Math.min(providers.size() - 1, index + PACKS_AHEAD) : index;
        while (tasks.size() <= last)
        {
            IContentProvider next = providers.get(tasks.size());
            tasks.add(workers.submit(() -> read(next, workers)));
        }
        ContentLoadingWorkers.Task<Result> task = tasks.set(index, null);
        return task != null ? task.join() : read(provider, workers);
    }

    private int indexOf(IContentProvider provider)
    {
        // Identity: a pack converted from JAR to ZIP changes the path its equality depends on.
        for (int i = 0; i < providers.size(); i++)
            if (providers.get(i) == provider)
                return i;
        return -1;
    }

    static Result read(IContentProvider provider, ContentLoadingWorkers workers)
    {
        // The one walk of a folder pack that its later cache checks are cut from, done here on a worker.
        ContentFileCache.prefetch(provider);
        List<Source> sources = new ArrayList<>();
        Set<String> aliasFiles = new LinkedHashSet<>();
        List<String> typeFolders = EnumType.getFoldersList();
        List<TypeFile> typeFiles = List.of();

        // The archive stays open until every file has been read.
        try (DirectoryStream<Path> dirStream = FileUtils.createDirectoryStream(provider))
        {
            for (Path path : dirStream)
            {
                String name = path.getFileName().toString();
                if (Files.isDirectory(path))
                {
                    if (typeFolders.contains(name))
                        listTypeFolder(path, name, provider, sources);
                }
                else if (ALIAS_FILES.contains(name) && Files.isRegularFile(path))
                    aliasFiles.add(name);
            }

            typeFiles = workers.map(sources, source -> readTypeFile(source, provider)).stream().filter(Objects::nonNull).toList();
        }
        catch (IOException e)
        {
            FlansLog.log.error("Failed to load types in content pack '{}'", provider.getName(), e);
        }
        return new Result(typeFiles, aliasFiles);
    }

    private static void listTypeFolder(Path folder, String folderName, IContentProvider provider, List<Source> sources)
    {
        EnumType type = EnumType.getType(folderName).orElse(null);
        try (Stream<Path> walk = Files.walk(folder))
        {
            walk.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.TXT_EXTENSION))
                .forEach(file -> sources.add(new Source(file, folderName, type)));
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.error("Failed to read '{}' folder in content pack '{}'", folderName, provider.getName(), e);
        }
    }

    @Nullable
    private static TypeFile readTypeFile(Source source, IContentProvider provider)
    {
        try
        {
            List<String> lines = TextDecoding.readLines(source.file());
            stripBomIfPresent(lines);
            return new TypeFile(source.file().getFileName().toString(), source.type(), provider, lines);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Failed to read '{}/{}' in content pack '{}'", source.folderName(), source.file().getFileName(), provider.getName(), e);
            return null;
        }
    }

    private static void stripBomIfPresent(List<String> lines)
    {
        if (!lines.isEmpty() && !lines.get(0).isEmpty() && lines.get(0).charAt(0) == '\uFEFF')
            lines.set(0, lines.get(0).substring(1));
    }
}
