package com.flansmodultimate.network.client;

import com.flansmodultimate.ModClient;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

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
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeBoolean(headshot);
        data.writeFloat(penAmount);
        data.writeBoolean(explosionHit);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        headshot = data.readBoolean();
        penAmount = data.readFloat();
        explosionHit = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        ModClient.setHitMarkerTime(20);
        ModClient.setHitMarkerPenAmount(penAmount);
        ModClient.setHitMarkerHeadshot(headshot);
        ModClient.setHitMarkerExplosion(explosionHit);
    }
}
