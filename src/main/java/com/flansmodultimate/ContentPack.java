package com.flansmodultimate;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Getter
public class ContentPack implements IContentProvider
{
    private String name;
    private Path path;
    private final Path identityPath;
    private final UUID runId = UUID.randomUUID();

    public ContentPack(String name, Path path)
    {
        this.name = name;
        this.path = normalize(path);
        identityPath = getIdentityPath(this.path);
    }

    @Override
    public void update(String name, Path path)
    {
        this.name = name;
        this.path = normalize(path);
    }

    @Override
    public String getRunId()
    {
        return runId.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof ContentPack other))
            return false;

        return identityPath.equals(other.identityPath);
    }

    @Override
    public int hashCode()
    {
        return identityPath.hashCode();
    }

    @Override
    public String toString()
    {
        return name + " [" + path.toString() + "]";
    }

    private static Path normalize(Path path)
    {
        return path.toAbsolutePath().normalize();
    }

    private static Path getIdentityPath(Path path)
    {
        String lowerName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".jar") || lowerName.endsWith(".zip"))
            return path.getParent().resolve(FilenameUtils.getBaseName(path.getFileName().toString())).normalize();

        return path;
    }
}
