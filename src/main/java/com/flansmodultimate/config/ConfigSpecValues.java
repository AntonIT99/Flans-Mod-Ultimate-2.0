package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.logging.LogUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads, transfers and writes the entries of a config spec by path, without knowing what they mean. The
 * options screen uses it to show and edit the common config of the server it is connected to: the server
 * sends the values it has, the client sends back the one the player changed, and the server validates that
 * change against its own spec before applying it.
 *
 * <p>Only the types the screen can edit travel: booleans, whole and decimal numbers, and enums, which move
 * as their constant name. Free text and lists stay in the config file.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigSpecValues
{
    /** Its own logger: this runs before, and without, the mod class being loaded. */
    private static final Logger LOG = LogUtils.getLogger();
    private static final String PATH_SEPARATOR = ".";
    private static final byte TYPE_BOOLEAN = 0;
    private static final byte TYPE_INT = 1;
    private static final byte TYPE_DOUBLE = 2;
    private static final byte TYPE_STRING = 3;

    /** Every entry of the spec the options screen can edit, keyed by its dotted path. */
    public static Map<String, Object> collect(ForgeConfigSpec spec)
    {
        Map<String, Object> values = new LinkedHashMap<>();
        collect(spec.getValues(), values);
        return values;
    }

    private static void collect(UnmodifiableConfig config, Map<String, Object> values)
    {
        for (Object child : config.valueMap().values())
        {
            if (child instanceof UnmodifiableConfig section)
                collect(section, values);
            else if (child instanceof ForgeConfigSpec.ConfigValue<?> value && isTransferable(value.get()))
                values.put(joinPath(value.getPath()), wireValue(value.get()));
        }
    }

    private static boolean isTransferable(Object value)
    {
        return value instanceof Boolean || value instanceof Integer || value instanceof Double || value instanceof Enum<?>;
    }

    private static Object wireValue(Object value)
    {
        return value instanceof Enum<?> constant ? constant.name() : value;
    }

    public static String joinPath(List<String> path)
    {
        return String.join(PATH_SEPARATOR, path);
    }

    public static List<String> splitPath(String path)
    {
        return List.of(path.split("\\" + PATH_SEPARATOR));
    }

    /** The config value at the given path, or null when the spec has no such entry. */
    @Nullable
    public static ForgeConfigSpec.ConfigValue<?> find(ForgeConfigSpec spec, List<String> path)
    {
        Object entry = spec.getValues().get(path);
        return entry instanceof ForgeConfigSpec.ConfigValue<?> value ? value : null;
    }

    /**
     * Writes one value into the spec after checking that the spec accepts it, so a client cannot put a
     * value the server's own config would reject into it.
     *
     * @return whether the spec was changed
     */
    @SuppressWarnings("unchecked")
    public static boolean apply(ForgeConfigSpec spec, List<String> path, Object newValue)
    {
        ForgeConfigSpec.ValueSpec valueSpec = spec.get(path);
        ForgeConfigSpec.ConfigValue<?> value = find(spec, path);
        if (valueSpec == null || value == null || !spec.isLoaded())
        {
            LOG.warn("Ignoring a change of the unknown config entry {}", joinPath(path));
            return false;
        }

        Object corrected = coerce(valueSpec.getClazz(), newValue);
        if (corrected == null || !valueSpec.test(corrected))
        {
            LOG.warn("Ignoring the rejected value {} for the config entry {}", newValue, joinPath(path));
            return false;
        }

        if (corrected.equals(value.get()))
            return false;

        ((ForgeConfigSpec.ConfigValue<Object>) value).set(corrected);
        return true;
    }

    /** Turns a transferred value back into the type the spec expects, or null when it does not fit. */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object coerce(Class<?> type, Object value)
    {
        if (type.isEnum() && value instanceof String name)
        {
            try
            {
                return Enum.valueOf((Class<? extends Enum>) type, name);
            }
            catch (IllegalArgumentException e)
            {
                return null;
            }
        }

        if (type == Integer.class && value instanceof Number number)
            return number.intValue();

        if (type == Double.class && value instanceof Number number)
            return number.doubleValue();

        return type.isInstance(value) ? value : null;
    }

    public static void write(FriendlyByteBuf buf, Map<String, Object> values)
    {
        buf.writeVarInt(values.size());
        for (Map.Entry<String, Object> entry : values.entrySet())
        {
            buf.writeUtf(entry.getKey());
            writeValue(buf, entry.getValue());
        }
    }

    public static Map<String, Object> read(FriendlyByteBuf buf)
    {
        int size = buf.readVarInt();
        Map<String, Object> values = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++)
        {
            String path = buf.readUtf();
            values.put(path, readValue(buf));
        }
        return values;
    }

    public static void writeValue(FriendlyByteBuf buf, Object value)
    {
        if (value instanceof Boolean flag)
        {
            buf.writeByte(TYPE_BOOLEAN);
            buf.writeBoolean(flag);
        }
        else if (value instanceof Integer number)
        {
            buf.writeByte(TYPE_INT);
            buf.writeVarInt(number);
        }
        else if (value instanceof Double number)
        {
            buf.writeByte(TYPE_DOUBLE);
            buf.writeDouble(number);
        }
        else
        {
            buf.writeByte(TYPE_STRING);
            buf.writeUtf(String.valueOf(wireValue(value)));
        }
    }

    public static Object readValue(FriendlyByteBuf buf)
    {
        byte type = buf.readByte();
        return switch (type)
        {
            case TYPE_BOOLEAN -> buf.readBoolean();
            case TYPE_INT -> buf.readVarInt();
            case TYPE_DOUBLE -> buf.readDouble();
            case TYPE_STRING -> buf.readUtf();
            default -> throw new IllegalArgumentException("Unknown config value type " + type);
        };
    }

    /** The editable entries of the spec, as a list of paths, in declaration order. */
    public static List<String> paths(ForgeConfigSpec spec)
    {
        return new ArrayList<>(collect(spec).keySet());
    }
}
