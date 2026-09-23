package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.client.CommonConfigMirror;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketSetCommonConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec;

import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The common config. In a world it belongs to the server: values come from what the server reported and
 * changes are requests the server may refuse. Outside a world there is no server, so this edits the local
 * common config file, which is the one a world started from here would use.
 */
public class CommonConfigTarget implements ConfigTarget
{
    private final Map<List<String>, Object> pending = new LinkedHashMap<>();

    /** Whether a server, rather than this client's own config file, owns these settings right now. */
    private static boolean connectedToServer()
    {
        return Minecraft.getInstance().level != null;
    }

    @Override
    public ModConfigSpec spec()
    {
        return ModCommonConfig.configSpec;
    }

    @Override
    public Object get(ModConfigSpec.ConfigValue<?> value)
    {
        return connectedToServer() ? CommonConfigMirror.get(value) : value.get();
    }

    @Override
    public void set(ModConfigSpec.ConfigValue<?> value, Object newValue)
    {
        if (!connectedToServer())
        {
            ModCommonConfig.setRuntimeValue(value.getPath(), newValue);
            return;
        }

        // Keep showing the new value until the server answers with what it really applied
        CommonConfigMirror.expect(value.getPath(), newValue);
        PacketHandler.sendToServer(new PacketSetCommonConfigValue(value.getPath(), newValue));
    }

    @Override
    public void setWhileDragging(ModConfigSpec.ConfigValue<?> value, Object newValue)
    {
        pending.put(value.getPath(), newValue);
        if (connectedToServer())
            CommonConfigMirror.expect(value.getPath(), newValue);
    }

    @Override
    public void flush()
    {
        for (Map.Entry<List<String>, Object> entry : pending.entrySet())
        {
            if (connectedToServer())
                PacketHandler.sendToServer(new PacketSetCommonConfigValue(entry.getKey(), entry.getValue()));
            else
                ModCommonConfig.setRuntimeValue(entry.getKey(), entry.getValue());
        }
        pending.clear();
    }

    @Override
    public boolean editable()
    {
        return !connectedToServer() || CommonConfigMirror.mayEdit();
    }

    @Override
    public boolean ready()
    {
        return !connectedToServer() || CommonConfigMirror.isReady();
    }
}
