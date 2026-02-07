package com.flansmodultimate;

import lombok.Getter;

import java.nio.file.Path;
import java.util.UUID;

@Getter
public class ContentPack implements IContentProvider
{
    private String name;
    private Path path;
    private final UUID runId = UUID.randomUUID();

    public ContentPack(String name, Path path)
    {
        this.name = name;
        this.path = path.toAbsolutePath().normalize();
    }

    @Override
    public void update(String name, Path path)
    {
        this.name = name;
        this.path = path.toAbsolutePath().normalize();
    }

    @Override
    public String getRunId()
    {
        return runId.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof IContentProvider other)) return false;
        return path.equals(other.getPath().toAbsolutePath().normalize());
    }

    @Override
    public int hashCode()
    {
        return path.hashCode();
    }

    @Override
    public String toString()
    {
        return name + " [" + path.toString() + "]";
    }
}
