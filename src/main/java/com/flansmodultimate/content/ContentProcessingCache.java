package com.flansmodultimate.content;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

/** Remembers successful generation after the final archive swap, independently for client and data. */
final class ContentProcessingCache
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VERSION = 1;
    private record Entry(int version, Map<String, ContentFileCache.Stamp> files, String definitions,
                         boolean assets, boolean data) {}
    private final IContentProvider provider;
    private final String definitions;
    private final Map<String, ContentFileCache.Stamp> current;
    private final Entry previous;

    ContentProcessingCache(IContentProvider provider, String definitions)
    {
        this.provider = provider;
        this.definitions = definitions;
        current = snapshot();
        previous = ContentFileCache.read(key(), Entry.class);
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
        Map<String, ContentFileCache.Stamp> finished = snapshot();
        if (finished != null)
            ContentFileCache.write(key(), new Entry(VERSION, finished, definitions,
                assetsAttempted ? assetsGenerated : assetsCurrent(), dataAttempted ? dataGenerated : dataCurrent()));
    }

    private Map<String, ContentFileCache.Stamp> snapshot()
    {
        try
        {
            return ContentFileCache.snapshot(provider, provider.getPath());
        }
        catch (IOException e)
        {
            LOGGER.warn("Could not validate content generation cache for '{}': {}", provider.getName(), e.toString());
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
