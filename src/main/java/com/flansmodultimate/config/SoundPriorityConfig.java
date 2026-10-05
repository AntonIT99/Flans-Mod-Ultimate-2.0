package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/** Preserves the user's order and missing-pack placeholders without rewriting unchanged TOML. */
public final class SoundPriorityConfig
{
    public static final String KEY = "soundPackPriority";
    public static final String COMMENT = """
        Sound sources in priority order: FIRST entry has the highest priority. Restart after changing this list.
        New sources are appended automatically. Missing sources stay listed, so reinstalling preserves their priority.
        pack:<name> identifies a standalone pack (without .jar/.zip); mod:<id> identifies a packaged module.
        Packaged logical packs share their module's sound assets and therefore have one entry per module.
        This order controls sound event definitions, .ogg files and measured sound lengths only.
        It is enforced independently of Minecraft's ordinary resource-pack ordering for these Flan sounds.
        Server and clients should use the same order.""";

    private SoundPriorityConfig() {}

    public static List<String> reconcile(Object configured, List<String> discovered)
    {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (configured instanceof List<?> entries)
            for (Object entry : entries)
                if (entry instanceof String value && !value.isBlank())
                    result.add(value.trim());
        result.addAll(discovered);
        return List.copyOf(result);
    }

    public static List<String> synchronize(Path file, List<String> discovered) throws IOException
    {
        Files.createDirectories(file.getParent());
        try (CommentedFileConfig config = CommentedFileConfig.builder(file, TomlFormat.instance()).sync().build())
        {
            if (Files.isRegularFile(file))
                config.load();
            Object previous = config.get(KEY);
            List<String> order = reconcile(previous, discovered);
            if (!order.equals(previous) || !COMMENT.equals(config.getComment(KEY)))
            {
                config.set(KEY, order);
                config.setComment(KEY, COMMENT);
                saveIfChanged(config, file);
            }
            return order;
        }
    }

    static void saveIfChanged(CommentedFileConfig config, Path file) throws IOException
    {
        String text = TomlFormat.instance().createWriter().writeToString(config);
        if (!Files.isRegularFile(file) || !Files.readString(file).equals(text))
            config.save();
    }
}
