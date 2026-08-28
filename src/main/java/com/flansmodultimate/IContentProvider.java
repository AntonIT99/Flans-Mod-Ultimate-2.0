package com.flansmodultimate;

import com.flansmodultimate.util.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public interface IContentProvider
{
    String getName();

    /**
     * Label used where a provider represents shared module-wide resources, such as texture
     * conflict reporting. Normal content packs use their own display name.
     */
    default String getConflictDisplayName()
    {
        return getName();
    }

    Path getPath();

    void update(String name, Path path);

    String getRunId();

    /**
     * Root containing legacy type folders and alias files.
     */
    default Path getContentRoot(@Nullable FileSystem fs)
    {
        if (isArchive())
            return fs != null ? fs.getPath("/") : getExtractedPath();
        return getPath();
    }

    /**
     * Immutable packaged providers already contain final assets and data and must never be rewritten.
     */
    default boolean isPreprocessed()
    {
        return false;
    }

    /** True for content bundled by the official-packs companion mod. */
    default boolean isOfficial()
    {
        return false;
    }

    /**
     * A packaged module may share one merged asset tree across several logical content providers.
     */
    default boolean shouldIndexAssetsForConflicts()
    {
        return true;
    }

    default Path getTempRoot()
    {
        if (!isArchive())
            throw new IllegalArgumentException("Content Pack is not an Archive");

        Path archive = getPath().toAbsolutePath().normalize();
        Path minecraftDir = archive.getParent().getParent();
        return minecraftDir.resolve(".flantemp");
    }

    default Path getExtractedPath()
    {
        if (!isArchive())
            throw new IllegalArgumentException("Content Pack is not an Archive");

        String packBase = FilenameUtils.getBaseName(getName());
        return getTempRoot().resolve(packBase + "__" + getRunId());
    }

    default Path getAssetsPath()
    {
        return getAssetsPath(null);
    }

    default Path getAssetsPath(FileSystem fs)
    {
        if (isArchive())
        {
            return (fs != null) ? fs.getPath("/assets").resolve(FlansMod.FLANSMOD_ID) : getExtractedPath().resolve("assets").resolve(FlansMod.FLANSMOD_ID);
        }
        return getPath().resolve("assets").resolve(FlansMod.FLANSMOD_ID);
    }

    /**
     * Legacy texture folders used by duplicate detection. Preprocessed providers may override this
     * to point at their final {@code assets/flansmod/textures} directory.
     */
    default Path getTextureSourcePath(@Nullable FileSystem fs)
    {
        return getAssetsPath(fs);
    }

    default Path getDataPath()
    {
        return getDataPath(null);
    }

    default Path getDataPath(FileSystem fs)
    {
        if (isArchive())
        {
            return (fs != null) ? fs.getPath("/data").resolve(FlansMod.FLANSMOD_ID) : getExtractedPath().resolve("data").resolve(FlansMod.FLANSMOD_ID);
        }
        return getPath().resolve("data").resolve(FlansMod.FLANSMOD_ID);
    }

    default Path getModelPath(String modelFullClassName, @Nullable FileSystem fs)
    {
        if (isArchive() && fs != null)
        {
            return fs.getPath("/" + modelFullClassName.replace(".", "/") + FileUtils.CLASS_EXTENSION);
        }
        return getPath().resolve(modelFullClassName.replace(".", "/") + FileUtils.CLASS_EXTENSION);
    }

    boolean equals(Object obj);

    int hashCode();

    default boolean isArchive()
    {
        return isJarFile() || isZipFile();
    }

    default boolean isDirectory()
    {
        return Files.isDirectory(getPath());
    }

    default boolean isJarFile()
    {
        return getPath().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.JAR_EXTENSION);
    }

    default boolean isZipFile()
    {
        return getPath().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.ZIP_EXTENSION);
    }
}
