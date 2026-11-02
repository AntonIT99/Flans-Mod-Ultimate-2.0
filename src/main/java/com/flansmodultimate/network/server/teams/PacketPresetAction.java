package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.centre.*;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Bounded operator preset intent, validated on the server. */
@NoArgsConstructor
public final class PacketPresetAction implements IServerPacket
{
    private int request = 0;
    private ResourceLocation kind = SettingsPresets.TEAMS;
    private String action = "list";
    private String name = "";
    private String target = "";
    private String json = "";
    private boolean replace = false;
    public PacketPresetAction(int request, ResourceLocation kind, String action, String name, String target, String json, boolean replace)
    {
        this.request = request;
        this.kind = kind;
        this.action = action;
        this.name = name;
        this.target = target;
        this.json = json;
        this.replace = replace;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeInt(request);
        data.writeResourceLocation(kind);
        data.writeUtf(action, 32);
        data.writeUtf(name, 32);
        data.writeUtf(target, 32);
        data.writeUtf(json, PresetFiles.MAX_BYTES);
        data.writeBoolean(replace);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        request = data.readInt();
        kind = data.readResourceLocation();
        action = data.readUtf(32);
        name = data.readUtf(32);
        target = data.readUtf(32);
        json = data.readUtf(PresetFiles.MAX_BYTES);
        replace = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        SettingsPresets.handle(player, request, new SettingsPresets.Request(kind, action, name, target, json, replace));
    }
}
