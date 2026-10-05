package com.flansmodultimate.content;

import com.flansmodultimate.util.FlansLog;
import java.io.IOException;

/** Remembers successful generation after the final archive swap, independently for client and data. */
final class ContentProcessingCache
{
    // 2: a fingerprint of the file stamps and the pack's path instead of every stamp.
    private static final int VERSION = 2;
    private record Entry(String source, int version, String files, String definitions,
                         boolean assets, boolean data) {}
    private final IContentProvider provider;
    private final String definitions;
    private final String current;
    private final Entry previous;

    ContentProcessingCache(IContentProvider provider, String definitions)
    {
        this.provider = provider;
        this.definitions = definitions;
        current = fingerprint(false);
        previous = ContentFileCache.read(ContentFileCache.Kind.GENERATION, key(), Entry.class);
    }

    boolean assetsCurrent()
    {
        return matches() && previous.assets();
    }

    boolean dataCurrent()
    {
        return matches() && previous.data();
    }

    private boolean matches()
    {
        return current != null && previous != null && previous.version() == VERSION
            && definitions.equals(previous.definitions()) && current.equals(previous.files());
    }

    void remember(boolean assetsGenerated, boolean dataGenerated, boolean assetsAttempted, boolean dataAttempted)
    {
        // A warm load does not even rewrite the cache file.
        if (!assetsAttempted && !dataAttempted && matches())
            return;
        // Generation has just written to the pack: what it holds now is walked again.
        String finished = fingerprint(true);
        if (finished != null)
            ContentFileCache.write(ContentFileCache.Kind.GENERATION, key(), new Entry(ContentFileCache.source(provider.getPath()),
                VERSION, finished, definitions,
                assetsAttempted ? assetsGenerated : assetsCurrent(), dataAttempted ? dataGenerated : dataCurrent()));
    }

    private String fingerprint(boolean afterWriting)
    {
        try
        {
            if (afterWriting && !provider.isArchive())
                return ContentFileCache.fingerprint(ContentFileCache.freshSnapshot(provider.getPath()));
            return ContentFileCache.fingerprint(ContentFileCache.snapshot(provider, provider.getPath()));
        }
        catch (IOException e)
        {
            FlansLog.log.warn("Could not validate content generation cache for '{}': {}", provider.getName(), e.toString());
            return null;
        }
    }

    private String key()
    {
        // JAR -> ZIP conversion keeps the same logical cache identity.
        java.nio.file.Path path = provider.getPath();
        if (provider.isArchive())
            path = path.resolveSibling(org.apache.commons.io.FilenameUtils.getBaseName(path.getFileName().toString()));
        return "generation:" + path.toAbsolutePath().normalize();
    }
}
