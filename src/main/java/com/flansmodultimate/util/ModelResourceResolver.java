package com.flansmodultimate.util;

import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.PlatformPaths;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.stream.Stream;

/** Resolves legacy model resources without depending on client rendering or an extracted archive. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelResourceResolver
{
    private static final ThreadLocal<IContentProvider> CURRENT_PACK = new ThreadLocal<>();
    private static final String[] LEGACY_ROOTS = {"resources/models", "resources/mod/models", "Flan", "flan"};

    /** Binds resources to the pack supplying the model class, including during its static initialization. */
    public static Scope useContentPack(IContentProvider provider)
    {
        Scope scope = new Scope(CURRENT_PACK.get());
        CURRENT_PACK.set(provider);
        return scope;
    }

    @Nullable
    public static Resource open(String name, String[] extensions) throws IOException
    {
        String relative = name.replace('\\', '/').replaceFirst("^/+", "");
        IContentProvider provider = CURRENT_PACK.get();
        if (provider != null)
        {
            Resource resource = openInPack(provider, relative, extensions);
            if (resource != null)
                return resource;
        }

        for (String root : LEGACY_ROOTS)
        {
            Path path = find(PlatformPaths.gameDir().resolve(root), relative, extensions);
            if (path != null)
                return new Resource(path, null, null);
        }
        return null;
    }

    @Nullable
    private static Resource openInPack(IContentProvider provider, String relative, String[] extensions) throws IOException
    {
        // Old Java models hardcode the original pack folder before assets/flansmod/models.
        // The supplying provider is authoritative even when its archive has since been renamed.
        String assetMarker = "assets/flansmod/";
        int assetStart = relative.toLowerCase(Locale.ROOT).indexOf(assetMarker);
        String assetRelative = assetStart >= 0 ? relative.substring(assetStart + assetMarker.length()) : relative.startsWith("models/") ? relative : "models/" + relative;
        FileSystem fs = FileUtils.createFileSystem(provider);
        if (provider.isArchive() && fs == null)
            return null;
        boolean retained = false;
        try
        {
            Path path = find(provider.getAssetsPath(fs), assetRelative, extensions);
            if (path == null)
                return null;
            retained = true;
            return new Resource(path, fs, provider);
        }
        finally
        {
            if (!retained)
                FileUtils.closeFileSystem(fs, provider);
        }
    }

    @Nullable
    private static Path find(Path root, String relative, String[] extensions) throws IOException
    {
        for (String extension : extensions)
        {
            String file = relative.toLowerCase(Locale.ROOT).endsWith("." + extension) ? relative : relative + "." + extension;
            Path candidate = root.resolve(file).normalize();
            if (!candidate.startsWith(root.normalize()))
                continue;
            if (Files.isRegularFile(candidate))
                return candidate;

            Path matched = findIgnoringCase(root, candidate);
            if (matched != null)
                return matched;
        }
        return null;
    }

    /** ZIP entries are case-sensitive; legacy authored names often differ from normalized assets. */
    @Nullable
    private static Path findIgnoringCase(Path root, Path candidate) throws IOException
    {
        Path current = root;
        for (Path component : root.normalize().relativize(candidate))
        {
            if (!Files.isDirectory(current))
                return null;
            String segment = component.toString();
            Path exact = current.resolve(segment);
            if (Files.exists(exact))
                current = exact;
            else
            {
                try (Stream<Path> children = Files.list(current))
                {
                    current = children.filter(child -> child.getFileName().toString().equalsIgnoreCase(segment)).sorted().findFirst().orElse(null);
                    if (current == null)
                        return null;
                }
            }
        }
        return Files.isRegularFile(current) ? current : null;
    }

    public record Resource(Path path, @Nullable FileSystem fileSystem, @Nullable IContentProvider provider) implements AutoCloseable
    {
        @Override
        public void close()
        {
            if (provider != null)
                FileUtils.closeFileSystem(fileSystem, provider);
        }
    }

    public static final class Scope implements AutoCloseable
    {
        private final IContentProvider previous;

        private Scope(@Nullable IContentProvider previous)
        {
            this.previous = previous;
        }

        @Override
        public void close()
        {
            if (previous == null)
                CURRENT_PACK.remove();
            else
                CURRENT_PACK.set(previous);
        }
    }
}
