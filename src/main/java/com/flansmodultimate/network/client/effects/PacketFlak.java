package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.ParticleNameCodec;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    /** Non-negative only for a persistent projectile smoke source, not a one-off flak burst. */
    private int smokeSourceId = -1;
    private float smokeRadius = 5F;

    public PacketFlak(Vec3 position, int n, String s)
    {
        this.position = position;
        numParticles = n;
        particleType = s;
    }

    public PacketFlak(Vec3 position, int n, String s, int smokeSourceId)
    {
        this(position, n, s);
        this.smokeSourceId = smokeSourceId;
    }

    public PacketFlak(Vec3 position, int n, String s, int smokeSourceId, float smokeRadius)
    {
        this(position, n, s, smokeSourceId);
        this.smokeRadius = smokeRadius;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeInt(numParticles);
        ParticleNameCodec.write(data, particleType);
        data.writeInt(smokeSourceId);
        if (smokeSourceId >= 0)
            data.writeFloat(smokeRadius);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numParticles = data.readInt();
        particleType = ParticleNameCodec.read(data);
        smokeSourceId = data.readInt();
        if (smokeSourceId >= 0)
            smokeRadius = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (smokeSourceId >= 0)
        {
            ClientHooks.RENDER.spawnSmokeCloud(smokeSourceId, position, numParticles, particleType, smokeRadius);
            return;
        }

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
