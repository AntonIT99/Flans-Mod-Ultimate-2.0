package com.flansmodultimate.network.client;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketFlanExplosionParticles implements IClientPacket
{
    private Vec3 position;
    private int numSmoke;
    private int numDebris;
    private float radius;

    public PacketFlanExplosionParticles(Vec3 position, int numSmoke, int numDebris, float radius)
    {
        this.position = position;
        this.numSmoke = numSmoke;
        this.numDebris = numDebris;
        this.radius = radius;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeInt(numSmoke);
        data.writeInt(numDebris);
        data.writeFloat(radius);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numSmoke = data.readInt();
        numDebris = data.readInt();
        radius = data.readFloat();
    }

    /** Blast radius at which flare/debris particles reach their largest size. */
    private static final float MAX_SCALE_RADIUS = 60.0F;
    private static final float MAX_PARTICLE_SCALE = 4.0F;

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        // Bigger blasts get bigger flares and debris rather than just more of them, both for
        // the visual read (a huge charge should look huge, not just noisy) and because the
        // particle count still comes from the content pack's flat FlareParticleCount /
        // DebrisParticleCount, which does not scale with the blast on its own.
        float t = Mth.clamp(radius / MAX_SCALE_RADIUS, 0.0F, 1.0F);
        float particleScale = 1.0F + t * (MAX_PARTICLE_SCALE - 1.0F);

        spawn(level, FlanParticles.FM_FLARE, position, numSmoke, radius * 0.12F, particleScale);
        spawn(level, FlanParticles.FM_DEBRIS_1, position, numDebris, radius * 0.12F, particleScale);
    }

    private void spawn(Level level, String particleType, Vec3 position, int count, float maxVelocity, float scale)
    {
        for (int i = 0; i < count; i++)
        {
            // Individually randomised overshoot so particles are not strictly contained to the
            // blast radius: most stay well inside it, but a realistic minority of fragments and
            // embers fly out further, tapering off toward the edge rather than stopping dead.
            float overshoot = 0.4F + level.random.nextFloat() * level.random.nextFloat() * 1.6F;
            float vx = (level.random.nextFloat() * 2.0F - 1.0F) * maxVelocity * overshoot;
            float vy = level.random.nextFloat() * maxVelocity * overshoot;
            float vz = (level.random.nextFloat() * 2.0F - 1.0F) * maxVelocity * overshoot;
            ClientHooks.RENDER.spawnParticle(particleType, position.x, position.y, position.z, vx, vy, vz, scale);
        }
    }
}
