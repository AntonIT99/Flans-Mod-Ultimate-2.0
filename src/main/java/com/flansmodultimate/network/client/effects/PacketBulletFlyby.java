package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.SoundNameCodec;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A server-confirmed near-miss sound at the closest point of the projectile's travelled segment. */
@NoArgsConstructor
public class PacketBulletFlyby implements IClientPacket
{
    private String sound;
    private Vec3 position;

    public PacketBulletFlyby(String sound, Vec3 position)
    {
        this.sound = sound;
        this.position = position;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        SoundNameCodec.write(data, sound);
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        sound = SoundNameCodec.read(data);
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.playBulletFlyby(sound, position);
    }
}
