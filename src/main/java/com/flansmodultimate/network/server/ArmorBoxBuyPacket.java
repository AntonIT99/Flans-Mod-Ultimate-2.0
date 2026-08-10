package com.flansmodultimate.network.server;

import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class ArmorBoxBuyPacket implements IServerPacket
{
    private BlockPos pos = BlockPos.ZERO;
    private int pageIndex;
    private int pieceIndex;

    public ArmorBoxBuyPacket(BlockPos pos, int pageIndex, int pieceIndex)
    {
        this.pos = pos;
        this.pageIndex = pageIndex;
        this.pieceIndex = pieceIndex;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeBlockPos(pos);
        data.writeVarInt(pageIndex);
        data.writeVarInt(pieceIndex);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        pos = data.readBlockPos();
        pageIndex = data.readVarInt();
        pieceIndex = data.readVarInt();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!(player.containerMenu instanceof ArmorBoxMenu menu))
            return;

        if (!menu.getPos().equals(pos) || !menu.stillValid(player))
            return;

        menu.getBlock().buyArmorServer(pageIndex, pieceIndex, player);
        menu.broadcastChanges();
    }
}
