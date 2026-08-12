package com.flansmodultimate;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable logical content pack stored inside a normal Forge mod.
 */
@Getter
public final class PackagedContentProvider implements IContentProvider
{
    private final String name;
    private final String conflictDisplayName;
    private final String packId;
    private final Path path;
    private final Path developmentContentRoot;
    private final Path developmentAssetsRoot;
    private final Path developmentModelsRoot;
    private final String archiveContentRoot;
    private final String archiveAssetsRoot;
    private final String archiveModelsRoot;
    private final boolean archiveBacked;
    private final boolean indexAssetsForConflicts;
    private final String runId = UUID.randomUUID().toString();

    PackagedContentProvider(String name, String conflictDisplayName, String packId, Path path,
                            Path developmentContentRoot, Path developmentAssetsRoot, Path developmentModelsRoot,
                            String archiveContentRoot, String archiveAssetsRoot, String archiveModelsRoot,
                            boolean archiveBacked, boolean indexAssetsForConflicts)
    {
        this.name = Objects.requireNonNull(name);
        this.conflictDisplayName = Objects.requireNonNull(conflictDisplayName);
        this.packId = Objects.requireNonNull(packId);
        this.path = path.toAbsolutePath().normalize();
        this.developmentContentRoot = Objects.requireNonNull(developmentContentRoot);
        this.developmentAssetsRoot = Objects.requireNonNull(developmentAssetsRoot);
        this.developmentModelsRoot = Objects.requireNonNull(developmentModelsRoot);
        this.archiveContentRoot = normalizeArchivePath(archiveContentRoot);
        this.archiveAssetsRoot = normalizeArchivePath(archiveAssetsRoot);
        this.archiveModelsRoot = normalizeArchivePath(archiveModelsRoot);
        this.archiveBacked = archiveBacked;
        this.indexAssetsForConflicts = indexAssetsForConflicts;
    }

    @Override
    public void update(String name, Path path)
    {
        throw new UnsupportedOperationException("Packaged content providers are immutable");
    }

    @Override
    public boolean isPreprocessed()
    {
        return true;
    }

    @Override
    public String getConflictDisplayName()
    {
        return conflictDisplayName;
    }

    /** Production providers reopen the installed JAR; development providers use Forge's resolved directories. */
    @Override
    public boolean isArchive()
    {
        return archiveBacked;
    }

    @Override
    public boolean isDirectory()
    {
        return !archiveBacked;
    }

    @Override
    public boolean shouldIndexAssetsForConflicts()
    {
        return indexAssetsForConflicts;
    }

    @Override
    public Path getContentRoot(@Nullable FileSystem fs)
    {
        return archiveBacked ? requireArchiveFileSystem(fs).getPath(archiveContentRoot) : developmentContentRoot;
    }

    @Override
    public Path getAssetsPath(@Nullable FileSystem fs)
    {
        return archiveBacked ? requireArchiveFileSystem(fs).getPath(archiveAssetsRoot) : developmentAssetsRoot;
    }

    @Override
    public Path getTextureSourcePath(@Nullable FileSystem fs)
    {
        return getAssetsPath(fs).resolve("textures");
    }

    @Override
    public Path getModelPath(String modelFullClassName, @Nullable FileSystem fs)
    {
        Path root = archiveBacked ? requireArchiveFileSystem(fs).getPath(archiveModelsRoot) : developmentModelsRoot;
        return root.resolve(modelFullClassName.replace('.', '/') + ".class");
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj || obj instanceof PackagedContentProvider other
            && path.equals(other.path) && packId.equals(other.packId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(path, packId);
    }

    @Override
    public String toString()
    {
        return name + " [" + packId + " in " + path + "]";
    }

    private static String normalizeArchivePath(String path)
    {
        return path.startsWith("/") ? path : "/" + path;
    }

    private FileSystem requireArchiveFileSystem(@Nullable FileSystem fs)
    {
        if (fs == null)
            throw new IllegalStateException("Production packaged provider requires an open JAR filesystem: " + this);
        return fs;
    }
}
