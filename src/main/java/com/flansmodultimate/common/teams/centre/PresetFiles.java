package com.flansmodultimate.common.teams.centre;

import com.google.gson.*;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;
import java.util.TreeSet;

/** Bounded, versioned JSON configuration files shared by local transfers and server libraries. No world state. */
public final class PresetFiles
{
    private static final String FORMAT = "format";
    private static final String INT_ARRAY = "$intArray";
    private static final String LONG_ARRAY = "$longArray";
    private static final String BYTE_ARRAY = "$byteArray";
    public static final int MAX_BYTES = 256 * 1024;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private PresetFiles()
    {}

    public record Document(String name, CompoundTag data)
    {}

    public static void checkName(String name)
    {
        if (!name.matches("[a-z0-9_-]{1,32}"))
            throw new IllegalArgumentException("Preset names must contain 1–32 lower-case letters, digits, - or _");
    }

    public static String encode(ResourceLocation kind, String name, CompoundTag data)
    {
        checkName(name);
        JsonObject root = new JsonObject();
        root.addProperty(FORMAT, 1);
        root.addProperty("kind", kind.toString());
        root.addProperty("name", name);
        root.add("data", toJson(data));
        String text = JSON.toJson(root);
        checkSize(text);
        return text;
    }

    public static Document decode(ResourceLocation kind, String text)
    {
        checkSize(text);
        checkNesting(text);
        JsonObject root = JsonParser.parseString(text).getAsJsonObject();
        if (!root.keySet().equals(Set.of(FORMAT, "kind", "name", "data")) || !root.get(FORMAT).equals(new JsonPrimitive(1)) || !kind.toString().equals(root.get("kind").getAsString()))
            throw new IllegalArgumentException("Wrong preset type or unsupported file version");
        String name = root.get("name").getAsString();
        checkName(name);
        Tag data = fromJson(root.get("data"));
        if (!(data instanceof CompoundTag compound))
            throw new IllegalArgumentException("Preset data must be an object");
        return new Document(name, compound);
    }

    /** Skip quoted strings while bounding containers before Gson can recurse into an upload. */
    private static void checkNesting(String text)
    {
        int depth = 0;
        int i = 0;
        while (i < text.length())
        {
            char c = text.charAt(i);
            if (c == '"')
                i = stringEnd(text, i);
            else if ((c == '{' || c == '[') && ++depth > 32)
                throw new IllegalArgumentException("Preset nesting exceeds 32 levels");
            else if (c == '}' || c == ']')
                depth--;
            i++;
        }
    }

    private static int stringEnd(String text, int start)
    {
        int i = start + 1;
        while (i < text.length())
        {
            char c = text.charAt(i);
            if (c == '\\')
                i++;
            else if (c == '"')
                return i;
            i++;
        }
        throw new IllegalArgumentException("Unterminated preset string");
    }

    public static String read(Path path) throws IOException
    {
        // Read at most the limit plus one, even if the file grows while being read.
        try (var input = Files.newInputStream(path))
        {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES)
                throw new IOException("Preset exceeds 256 KiB");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static void write(Path path, String text, boolean replace) throws IOException
    {
        checkSize(text);
        Files.createDirectories(path.toAbsolutePath().getParent());
        if (!replace && Files.exists(path))
            throw new FileAlreadyExistsException(path.toString());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), ".preset-", ".tmp");
        try
        {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            if (replace)
            {
                try
                {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (AtomicMoveNotSupportedException ignored)
                {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            else
                Files.move(temporary, path);
        }
        finally
        {
            Files.deleteIfExists(temporary);
        }
    }

    private static void checkSize(String text)
    {
        if (text.length() > MAX_BYTES || text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Preset exceeds 256 KiB");
    }

    public static JsonElement toJson(Tag tag)
    {
        if (tag instanceof IntArrayTag || tag instanceof LongArrayTag || tag instanceof ByteArrayTag)
        {
            JsonArray values = new JsonArray();
            ((CollectionTag<?>) tag).forEach(value -> values.add(((NumericTag) value).getAsNumber()));
            JsonObject typed = new JsonObject();
            typed.add(arrayType(tag), values);
            return typed;
        }
        if (tag instanceof CompoundTag compound)
        {
            JsonObject object = new JsonObject();
            for (String key : new TreeSet<>(compound.getAllKeys()))
                object.add(key, toJson(compound.get(key)));
            return object;
        }
        if (tag instanceof ListTag list)
        {
            JsonArray array = new JsonArray();
            list.forEach(entry -> array.add(toJson(entry)));
            return array;
        }
        if (tag instanceof ByteTag number)
            return new JsonPrimitive(number.getAsByte() != 0);
        if (tag instanceof NumericTag number)
            return new JsonPrimitive(number.getAsNumber());
        if (tag instanceof StringTag)
            return new JsonPrimitive(tag.getAsString());
        throw new IllegalArgumentException("Unsupported preset value");
    }

    private static String arrayType(Tag tag)
    {
        if (tag instanceof IntArrayTag)
            return INT_ARRAY;
        if (tag instanceof LongArrayTag)
            return LONG_ARRAY;
        return BYTE_ARRAY;
    }

    private static Tag fromJson(JsonElement json)
    {
        if (json.isJsonObject())
            return fromObject(json.getAsJsonObject());
        if (json.isJsonArray())
        {
            ListTag list = new ListTag();
            for (JsonElement entry : json.getAsJsonArray())
            {
                if (!list.addTag(list.size(), fromJson(entry)))
                    throw new IllegalArgumentException("Preset lists must contain values of the same type");
            }
            return list;
        }
        if (json.isJsonPrimitive())
            return fromPrimitive(json.getAsJsonPrimitive());
        throw new IllegalArgumentException("Null is not a preset setting");
    }

    private static Tag fromObject(JsonObject object)
    {
        if (object.size() == 1)
        {
            if (object.has(INT_ARRAY))
                return new IntArrayTag(intArray(object.getAsJsonArray(INT_ARRAY)));
            if (object.has(LONG_ARRAY))
                return new LongArrayTag(longArray(object.getAsJsonArray(LONG_ARRAY)));
            if (object.has(BYTE_ARRAY))
                return new ByteArrayTag(byteArray(object.getAsJsonArray(BYTE_ARRAY)));
        }
        CompoundTag compound = new CompoundTag();
        object.entrySet().forEach(entry -> compound.put(entry.getKey(), fromJson(entry.getValue())));
        return compound;
    }

    private static Tag fromPrimitive(JsonPrimitive value)
    {
        if (value.isBoolean())
            return ByteTag.valueOf(value.getAsBoolean());
        if (value.isString())
            return StringTag.valueOf(value.getAsString());
        BigDecimal decimal = value.getAsBigDecimal();
        double number = decimal.doubleValue();
        if (!Double.isFinite(number))
            throw new IllegalArgumentException("Preset numbers must be finite");
        boolean integral = decimal.stripTrailingZeros().scale() <= 0;
        if (integral && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE)
            return IntTag.valueOf(decimal.intValueExact());
        if (integral && decimal.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0 && decimal.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0)
            return LongTag.valueOf(decimal.longValueExact());
        return DoubleTag.valueOf(number);
    }

    private static int[] intArray(JsonArray json)
    {
        int[] values = new int[json.size()];
        for (int i = 0; i < values.length; i++)
            values[i] = json.get(i).getAsBigDecimal().intValueExact();
        return values;
    }

    private static long[] longArray(JsonArray json)
    {
        long[] values = new long[json.size()];
        for (int i = 0; i < values.length; i++)
            values[i] = json.get(i).getAsBigDecimal().longValueExact();
        return values;
    }

    private static byte[] byteArray(JsonArray json)
    {
        byte[] values = new byte[json.size()];
        for (int i = 0; i < values.length; i++)
            values[i] = json.get(i).getAsBigDecimal().byteValueExact();
        return values;
    }
}
