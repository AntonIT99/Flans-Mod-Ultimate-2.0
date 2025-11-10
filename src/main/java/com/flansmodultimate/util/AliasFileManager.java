package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class AliasFileManager implements AutoCloseable
{
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type type = new TypeToken<Map<String, String>>() {}.getType();

    private FileSystem fs;
    private final String fileName;
    private final IContentProvider provider;

    public AliasFileManager(String fileName, IContentProvider provider)
    {
        this.fileName = fileName;
        this.provider = provider;
    }

    public Optional<Map<String, String>> readFile()
    {
        fs = FileUtils.createFileSystem(provider);
        Path file = (fs != null) ? fs.getPath("/" + fileName) : provider.getPath().resolve(fileName);

        if (!Files.exists(file))
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of(gson.fromJson(Files.readString(file), type));
        }
        catch (Exception e)
        {
            FlansMod.log.error("Error reading {} in {}", file.getFileName(), provider.getPath(), e);
            return Optional.empty();
        }
    }

    public void writeToFile(Map<String, String> aliasMapping)
    {
        Path file = (provider.isArchive() ? provider.getExtractedPath() : provider.getPath()).resolve(fileName);

        try
        {
            Files.writeString(file, gson.toJson(aliasMapping));
        }
        catch (Exception e)
        {
            FlansMod.log.error("Error writing to {} in {}", file.getFileName(), provider.getPath(), e);
        }
    }

    @Override
    public void close()
    {
        FileUtils.closeFileSystem(fs, provider);
    }
}
