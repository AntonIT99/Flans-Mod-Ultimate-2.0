package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Server-authored explosion shake or near-miss impulse; no client can request an impulse for another player. */
@NoArgsConstructor
public class PacketCombatFeedback implements IClientPacket
{
    private boolean explosion;
    private float intensity;
    private float direction;

    public PacketCombatFeedback(boolean explosion, float intensity, float direction)
    {
        this.explosion = explosion;
        this.intensity = intensity;
        this.direction = direction;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(explosion);
        data.writeFloat(intensity);
        data.writeFloat(direction);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        explosion = data.readBoolean();
        intensity = data.readFloat();
        direction = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.addCombatFeedback(explosion, intensity, direction);
    }
}
