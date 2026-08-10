package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

@NoArgsConstructor
public class PacketGunMuzzleFlash implements IClientPacket
{
    private UUID playerUUID;
    private InteractionHand hand;
    private String particleType;
    private float scale;
    private boolean showToShooter;

    public PacketGunMuzzleFlash(UUID playerUUID, InteractionHand hand, String particleType, float scale, boolean showToShooter)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
        this.particleType = particleType;
        this.scale = scale;
        this.showToShooter = showToShooter;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeUUID(playerUUID);
        data.writeEnum(hand);
        data.writeUtf(particleType);
        data.writeFloat(scale);
        data.writeBoolean(showToShooter);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
        particleType = data.readUtf();
        scale = data.readFloat();
        showToShooter = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.spawnMuzzleFlashParticle(playerUUID, hand, particleType, scale, showToShooter);
    }
}
