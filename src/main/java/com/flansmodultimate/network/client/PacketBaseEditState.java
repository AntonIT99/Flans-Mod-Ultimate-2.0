package com.flansmodultimate.network.client;

import com.flansmodultimate.client.gui.TeamsBaseEditScreen;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Operator-only snapshot used to open the legacy base editor safely. */
@NoArgsConstructor
public final class PacketBaseEditState implements IClientPacket
{
    public record MapChoice(String id, String name) {}

    private UUID baseId = new UUID(0L, 0L);
    private String baseName = "";
    private String selectedMap = "";
    private int ownerId;
    private List<MapChoice> maps = List.of();

    public static PacketBaseEditState create(TeamsManager manager, ITeamBase base)
    {
        PacketBaseEditState packet = new PacketBaseEditState();
        packet.baseId = base.getObjectId();
        packet.baseName = base.getBaseName();
        packet.selectedMap = base.getMapId();
        packet.ownerId = base.getDefaultOwnerId();
        packet.maps = manager.getMaps().stream().filter(map -> map.getDimension().equals(base.getDimension()))
            .map(map -> new MapChoice(map.getShortName(), map.getName())).toList();
        return packet;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeUUID(baseId);
        data.writeUtf(baseName, 60);
        data.writeUtf(selectedMap, 128);
        data.writeVarInt(ownerId);
        data.writeCollection(maps, (buf, map) -> { buf.writeUtf(map.id(), 128); buf.writeUtf(map.name(), 128); });
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        baseId = data.readUUID();
        baseName = data.readUtf(60);
        selectedMap = data.readUtf(128);
        ownerId = data.readVarInt();
        maps = data.readList(buf -> new MapChoice(buf.readUtf(128), buf.readUtf(128)));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Minecraft.getInstance().gui.setScreen(new TeamsBaseEditScreen(this));
    }

    public UUID getBaseId() { return baseId; }
    public String getBaseName() { return baseName; }
    public String getSelectedMap() { return selectedMap; }
    public int getOwnerId() { return ownerId; }
    public List<MapChoice> getMaps() { return maps; }
}
