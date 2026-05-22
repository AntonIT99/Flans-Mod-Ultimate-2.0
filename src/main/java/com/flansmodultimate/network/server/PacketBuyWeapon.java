package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.GunBoxMenu;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketBuyWeapon implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketBuyWeapon> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "buy_weapon"));

    public static final StreamCodec<FriendlyByteBuf, PacketBuyWeapon> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketBuyWeapon decode(FriendlyByteBuf buf)
        {
            PacketBuyWeapon packet = new PacketBuyWeapon();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketBuyWeapon packet)
        {
            packet.encodeInto(buf);
        }
    };

    private BlockPos pos = BlockPos.ZERO;
    private String itemShortName = "";

    public PacketBuyWeapon(BlockPos pos, String itemShortName)
    {
        this.pos = pos;
        this.itemShortName = itemShortName;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
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
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
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