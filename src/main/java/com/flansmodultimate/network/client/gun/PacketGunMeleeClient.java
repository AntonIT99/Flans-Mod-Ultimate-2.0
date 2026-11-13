package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.common.guns.EnumMeleeDirection;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

@NoArgsConstructor
public class PacketGunMeleeClient implements IClientPacket
{
    private UUID playerUUID;
    private InteractionHand hand;
    private EnumMeleeDirection direction = EnumMeleeDirection.FORWARD;

    public PacketGunMeleeClient(UUID playerUUID, InteractionHand hand, EnumMeleeDirection direction)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
        this.direction = direction;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(playerUUID);
        data.writeEnum(hand);
        data.writeVarInt(direction.ordinal());
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
        direction = EnumMeleeDirection.byOrdinal(data.readVarInt());
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Player meleePlayer = level.getPlayerByUUID(playerUUID);
        if (meleePlayer != null && meleePlayer.getItemInHand(hand).getItem() instanceof GunItem gunItem)
        {
            ClientHooks.GUN.meleeGunItem(gunItem, meleePlayer, hand, direction);
        }
    }
}
