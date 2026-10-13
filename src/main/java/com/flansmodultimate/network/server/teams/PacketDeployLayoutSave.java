package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** The deployment map layout an operator arranged in the edit mode of the deployment screen. */
@NoArgsConstructor
public final class PacketDeployLayoutSave implements IServerPacket
{
    private String mapId = "";
    private TeamsMap.ImageCrop crop = TeamsMap.ImageCrop.FULL;
    private List<DeployLayoutEntry> entries = List.of();

    public PacketDeployLayoutSave(String mapId, TeamsMap.ImageCrop crop, List<DeployLayoutEntry> entries)
    {
        this.mapId = mapId;
        this.crop = crop;
        this.entries = List.copyOf(entries);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUtf(mapId, 128);
        data.writeFloat(crop.minU());
        data.writeFloat(crop.minV());
        data.writeFloat(crop.maxU());
        data.writeFloat(crop.maxV());
        data.writeVarInt(Math.min(entries.size(), TeamsMap.MAX_DEPLOY_LAYOUT_ENTRIES));
        for (int i = 0; i < Math.min(entries.size(), TeamsMap.MAX_DEPLOY_LAYOUT_ENTRIES); i++)
        {
            DeployLayoutEntry entry = entries.get(i);
            data.writeUtf(entry.key(), DeployLayoutEntry.MAX_KEY_LENGTH);
            data.writeFloat(entry.x());
            data.writeFloat(entry.y());
        }
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        mapId = data.readUtf(128);
        crop = TeamsMap.ImageCrop.normalised(data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat());
        int count = Math.min(data.readVarInt(), TeamsMap.MAX_DEPLOY_LAYOUT_ENTRIES);
        List<DeployLayoutEntry> read = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++)
            read.add(new DeployLayoutEntry(data.readUtf(DeployLayoutEntry.MAX_KEY_LENGTH), data.readFloat(), data.readFloat()));
        entries = List.copyOf(read);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (ItemOpStick.canUse(player))
            TeamsManager.getInstance().saveDeployLayout(mapId, entries, crop);
    }
}
