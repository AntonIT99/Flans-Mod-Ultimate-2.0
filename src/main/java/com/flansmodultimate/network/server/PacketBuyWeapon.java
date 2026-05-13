package com.flansmodultimate.network.server;

import com.flansmodultimate.common.inventory.GunBoxMenu;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketBuyWeapon implements IServerPacket
{
    private BlockPos pos = BlockPos.ZERO;
    private String itemShortName = "";

    public PacketBuyWeapon(BlockPos pos, String itemShortName)
    {
        this.pos = pos;
        this.itemShortName = itemShortName;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeBlockPos(pos);
        data.writeUtf(itemShortName);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        pos = data.readBlockPos();
        itemShortName = data.readUtf();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!(player.containerMenu instanceof GunBoxMenu menu))
            return;

        if (!menu.getPos().equals(pos) || !menu.stillValid(player))
            return;

        InfoType item = InfoType.getInfoType(itemShortName);
        if (item == null)
            return;

        menu.getBlock().buyGunServer(item, player);
        menu.broadcastChanges();
    }
}
