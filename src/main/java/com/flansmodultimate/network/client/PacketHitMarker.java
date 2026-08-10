package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketHitMarker implements IClientPacket
{
    private float penAmount = 1F;
    private boolean headshot = false;
    private boolean explosionHit = false;

    public PacketHitMarker(boolean head, float pen, boolean explosion)
    {
        headshot = head;
        penAmount = pen;
        explosionHit = explosion;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeBoolean(headshot);
        data.writeFloat(penAmount);
        data.writeBoolean(explosionHit);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        headshot = data.readBoolean();
        penAmount = data.readFloat();
        explosionHit = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.updateHitMarker(20, penAmount, headshot, explosionHit);
    }
}
