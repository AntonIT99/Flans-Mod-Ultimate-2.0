package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.gui.TeamsSpawnerBaseScreen;
import com.flansmodultimate.common.block.entity.TeamBannerBlockEntity;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.teams.PacketBannerEditAction;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Base choices for the operator's banner assignment screen. */
@NoArgsConstructor
public final class PacketBannerEditState implements IClientPacket
{
    private BlockPos pos = BlockPos.ZERO;
    private UUID objectId = new UUID(0L, 0L);
    private UUID baseId;
    private List<PacketSpawnerEditState.BaseChoice> bases = List.of();

    public static PacketBannerEditState of(TeamBannerBlockEntity banner)
    {
        PacketBannerEditState packet = new PacketBannerEditState();
        packet.pos = banner.getBlockPos();
        packet.objectId = banner.getObjectId();
        packet.baseId = banner.getBaseId();
        packet.bases = TeamsManager.getInstance().getBases(banner.getDimension()).stream()
            .map(base -> new PacketSpawnerEditState.BaseChoice(base.getObjectId(), base.getBaseName(), base.getMapId(), BlockPos.containing(base.getTeamObjectPosition()))).toList();
        return packet;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBlockPos(pos);
        data.writeUUID(objectId);
        data.writeBoolean(baseId != null);
        if (baseId != null)
            data.writeUUID(baseId);
        data.writeCollection(bases, (buffer, base) ->
        {
            buffer.writeUUID(base.id());
            buffer.writeUtf(base.name());
            buffer.writeUtf(base.map());
            buffer.writeBlockPos(base.position());
        });
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        pos = data.readBlockPos();
        objectId = data.readUUID();
        baseId = data.readBoolean() ? data.readUUID() : null;
        bases = data.readList(buffer -> new PacketSpawnerEditState.BaseChoice(buffer.readUUID(), buffer.readUtf(), buffer.readUtf(), buffer.readBlockPos()));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Minecraft.getInstance()
            .setScreen(new TeamsSpawnerBaseScreen(null, bases, baseId, choice -> PacketHandler.sendToServer(new PacketBannerEditAction(pos, objectId, choice == null ? null : choice.id()))));
    }
}
