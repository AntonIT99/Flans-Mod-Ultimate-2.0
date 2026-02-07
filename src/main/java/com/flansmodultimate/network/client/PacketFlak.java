package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketFlak implements IClientPacket
{
    /** Position of this flak */
    private Vec3 position;
    /** Num particles */
    private int numParticles;
    /** Particle type */
    private String particleType;

    public PacketFlak(Vec3 position, int n, String s)
    {
        this.position = position;
        numParticles = n;
        particleType = s;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeInt(numParticles);
        data.writeUtf(particleType);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numParticles = data.readInt();
        particleType = data.readUtf();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        for (int i = 0; i < numParticles; i++)
        {
            double ox = position.x + level.random.nextGaussian();
            double oy = position.y + level.random.nextGaussian();
            double oz = position.z + level.random.nextGaussian();
            double vx = level.random.nextGaussian() / 20.0;
            double vy = level.random.nextGaussian() / 20.0;
            double vz = level.random.nextGaussian() / 20.0;

            ClientHooks.RENDER.spawnParticle(particleType, ox, oy, oz, vx, vy, vz, 1F);
        }
    }
}
