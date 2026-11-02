package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.gui.centre.SettingsPresetsScreen;
import com.flansmodultimate.common.teams.centre.*;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.PacketIO;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

/** Bounded operator preset reply, matched to the requesting screen. */
@NoArgsConstructor
public final class PacketPresetResult implements IClientPacket
{
    private int request = 0;
    private boolean failed = false;
    private String json = "";
    private List<String> names = List.of();
    private List<String> builtIns = List.of();
    private List<Component> preview = List.of();
    private Component message = Component.empty();
    public PacketPresetResult(int request, boolean failed, String json, List<String> names, List<String> builtIns, List<Component> preview, Component message)
    {
        this.request = request;
        this.failed = failed;
        this.json = json;
        this.names = List.copyOf(names);
        this.builtIns = List.copyOf(builtIns);
        this.preview = List.copyOf(preview);
        this.message = message;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeInt(request);
        data.writeBoolean(failed);
        data.writeUtf(json, PresetFiles.MAX_BYTES);
        if (names.size() > 2048)
            throw new IllegalArgumentException("Too many preset entries");
        data.writeVarInt(names.size());
        names.forEach(value -> data.writeUtf(value, 32));
        data.writeVarInt(builtIns.size());
        builtIns.forEach(value -> data.writeUtf(value, 32));
        if (preview.size() > 512)
            throw new IllegalArgumentException("Too many preset entries");
        data.writeVarInt(preview.size());
        preview.forEach(value -> PacketIO.writeComponent(data, value));
        PacketIO.writeComponent(data, message);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        request = data.readInt();
        failed = data.readBoolean();
        json = data.readUtf(PresetFiles.MAX_BYTES);
        int namesCount = data.readVarInt();
        if (namesCount < 0 || namesCount > 2048)
            throw new IllegalArgumentException("Too many preset entries");
        names = new ArrayList<>();
        for (int i = 0; i < namesCount; i++)
            names.add(data.readUtf(32));
        int builtInsCount = data.readVarInt();
        if (builtInsCount < 0 || builtInsCount > 2048)
            throw new IllegalArgumentException("Too many preset entries");
        builtIns = new ArrayList<>();
        for (int i = 0; i < builtInsCount; i++)
            builtIns.add(data.readUtf(32));
        int previewCount = data.readVarInt();
        if (previewCount < 0 || previewCount > 512)
            throw new IllegalArgumentException("Too many preset entries");
        preview = new ArrayList<>();
        for (int i = 0; i < previewCount; i++)
            preview.add(PacketIO.readComponent(data));
        message = PacketIO.readComponent(data);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        SettingsPresetsScreen.accept(request, failed, json, names, builtIns, preview, message);
    }
}
