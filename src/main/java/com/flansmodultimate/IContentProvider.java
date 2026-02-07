package com.flansmodultimate;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static java.util.UUID.randomUUID;

public interface IContentProvider
{
    String getName();

    Path getPath();

    void update(String name, Path path);

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

    default String getRunId()
    {
        return randomUUID().toString();
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

    default Path getModelPath(String modelFullClassName, @Nullable FileSystem fs)
    {
        if (isArchive() && fs != null)
        {
            return fs.getPath("/" + modelFullClassName.replace(".", "/") + ".class");
        }
        return getPath().resolve(modelFullClassName.replace(".", "/") + ".class");
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
        return getPath().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    default boolean isZipFile()
    {
        return getPath().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }
}
