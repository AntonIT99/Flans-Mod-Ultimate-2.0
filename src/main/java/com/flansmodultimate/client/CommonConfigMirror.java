package com.flansmodultimate.client;

import com.flansmodultimate.config.ConfigSpecValues;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketRequestCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;

/**
 * The common config of the server this client is connected to, as the server reported it, so the options
 * screen can show and edit settings the client's own config file has nothing to say about. Outside a world
 * there is no server and the client's own common config is the one that counts.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConfigMirror
{
    @Nullable
    private static volatile Map<String, Object> values;
    private static volatile boolean mayEdit;

    /** Asks the server for its common config. The answer arrives asynchronously. */
    public static void request()
    {
        if (Minecraft.getInstance().getConnection() == null)
            return;

        PacketHandler.sendToServer(new PacketRequestCommonConfig());
    }

    public static void accept(Map<String, Object> serverValues, boolean playerMayEdit)
    {
        values = serverValues;
        mayEdit = playerMayEdit;
    }

    public static void clear()
    {
        values = null;
        mayEdit = false;
    }

    /** Whether the server has answered yet. */
    public static boolean isReady()
    {
        return values != null;
    }

    /** Whether the server said this player may change its common config. */
    public static boolean mayEdit()
    {
        return mayEdit;
    }

    /**
     * The value in force for a common config entry: the server's while connected to one, otherwise the
     * client's own, which is the config a world started from here would use.
     */
    @Nullable
    public static Object get(ForgeConfigSpec.ConfigValue<?> value)
    {
        Map<String, Object> current = values;
        if (current == null)
            return value.get();

        Object mirrored = current.get(ConfigSpecValues.joinPath(value.getPath()));
        return mirrored == null ? value.get() : ConfigSpecValues.coerce(valueType(value), mirrored);
    }

    private static Class<?> valueType(ForgeConfigSpec.ConfigValue<?> value)
    {
        ForgeConfigSpec.ValueSpec spec = ModCommonConfig.configSpec.get(value.getPath());
        return spec == null ? Object.class : spec.getClazz();
    }

    /** Applies the change locally as well, so the screen keeps showing it until the server answers. */
    public static void expect(List<String> path, Object newValue)
    {
        Map<String, Object> current = values;
        if (current != null)
            current.put(ConfigSpecValues.joinPath(path), newValue);
    }
}
