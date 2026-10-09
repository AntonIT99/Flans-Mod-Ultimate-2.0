package com.flansmodultimate.content;

import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

final class AliasFileManager implements AutoCloseable
{
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type type = new TypeToken<Map<String, String>>()
    {}.getType();

    @Nullable
    private FileSystem fs;
    private final String fileName;
    private final IContentProvider provider;

    AliasFileManager(String fileName, IContentProvider provider)
    {
        this.fileName = fileName;
        this.provider = provider;
    }

    public Optional<Map<String, String>> readFile()
    {
        Optional<FileSystem> fileSystem = Optional.ofNullable(FileUtils.createFileSystem(provider));
        Path file = fileSystem.map(fileSys -> fileSys.getPath("/" + fileName)).orElseGet(() -> provider.getPath().resolve(fileName));
        this.fs = fileSystem.orElse(null);

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
            FlansLog.log.error("Error reading {} in {}", file.getFileName(), provider.getPath(), e);
            return Optional.empty();
        }
    }

    public void writeToFile(Map<String, String> aliasMapping)
    {
        Path file = (provider.isArchive() ? provider.getExtractedPath() : provider.getPath()).resolve(fileName);

        try
        {
            String json = gson.toJson(aliasMapping);
            if (!Files.isRegularFile(file) || !Files.readString(file).equals(json))
                Files.writeString(file, json);
        }
        catch (Exception e)
        {
            FlansLog.log.error("Error writing to {} in {}", file.getFileName(), provider.getPath(), e);
        }
    }

    @Override
    public void close()
    {
        FileUtils.closeFileSystem(fs, provider);
    }

    static boolean shouldUpdateAliasMappingFile(String fileName, IContentProvider provider, @Nullable Map<String, String> aliasMapping)
    {
        if (aliasMapping == null)
            aliasMapping = Collections.emptyMap();

        try (AliasFileManager fileManager = new AliasFileManager(fileName, provider))
        {
            Optional<Map<String, String>> mapping = fileManager.readFile();
            return mapping.isEmpty() || !mapping.get().equals(aliasMapping);
        }
    }

    static void writeToAliasMappingFile(String fileName, IContentProvider provider, @Nullable Map<String, String> aliasMapping)
    {
        if (aliasMapping == null)
            aliasMapping = Collections.emptyMap();

        try (AliasFileManager fileManager = new AliasFileManager(fileName, provider))
        {
            fileManager.writeToFile(aliasMapping);
        }
    }
}
