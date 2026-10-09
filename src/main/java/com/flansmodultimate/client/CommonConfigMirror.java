package com.flansmodultimate.client;

import com.flansmodultimate.config.ConfigSpecValues;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.config.PacketRequestCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The common config of the server this client is connected to, as the server reported it, so the options
 * screen can show and edit settings the client's own config file has nothing to say about. Outside a world
 * there is no server and the client's own common config is the one that counts.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConfigMirror
{
    private static final AtomicReference<@Nullable Snapshot> state = new AtomicReference<>();

    private record Snapshot(Map<String, Object> values, boolean mayEdit)
    {}

    /** Asks the server for its common config. The answer arrives asynchronously. */
    public static void request()
    {
        if (Minecraft.getInstance().getConnection() == null)
            return;

        PacketHandler.sendToServer(new PacketRequestCommonConfig());
    }

    public static void accept(Map<String, Object> serverValues, boolean playerMayEdit)
    {
        state.set(new Snapshot(immutableCopy(serverValues), playerMayEdit));
    }

    public static void clear()
    {
        state.set(null);
    }

    /** Whether the server has answered yet. */
    public static boolean isReady()
    {
        return state.get() != null;
    }

    /** Whether the server said this player may change its common config. */
    public static boolean mayEdit()
    {
        Snapshot current = state.get();
        return current != null && current.mayEdit();
    }

    /**
     * The value in force for a common config entry: the server's while connected to one, otherwise the
     * client's own, which is the config a world started from here would use.
     */
    @Nullable
    public static Object get(ModConfigSpec.ConfigValue<?> value)
    {
        Snapshot snapshot = state.get();
        Map<String, Object> current = snapshot == null ? null : snapshot.values();
        if (current == null)
            return value.get();

        Object mirrored = current.get(ConfigSpecValues.joinPath(value.getPath()));
        return mirrored == null ? value.get() : ConfigSpecValues.coerce(valueType(value), mirrored);
    }

    private static Class<?> valueType(ModConfigSpec.ConfigValue<?> value)
    {
        ModConfigSpec.ValueSpec spec = ConfigSpecValues.valueSpec(ModCommonConfig.configSpec, value.getPath());
        return spec == null ? Object.class : spec.getClazz();
    }

    /** Applies the change locally as well, so the screen keeps showing it until the server answers. */
    public static void expect(List<String> path, Object newValue)
    {
        String key = ConfigSpecValues.joinPath(path);
        state.updateAndGet(current ->
        {
            if (current == null)
                return null;
            Map<String, Object> updated = new HashMap<>(current.values());
            updated.put(key, newValue);
            return new Snapshot(immutableCopy(updated), current.mayEdit());
        });
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> values)
    {
        return Map.copyOf(values);
    }
}
