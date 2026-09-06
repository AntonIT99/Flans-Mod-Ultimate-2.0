package com.flansmodultimate.network.client;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@NoArgsConstructor
public class PacketFlanExplosionBlockParticles implements IClientPacket
{
    private Vec3 center;
    private float radius;
    private long[] blockPosLongs;

    public PacketFlanExplosionBlockParticles(Vec3 center, float radius, List<BlockPos> positions)
    {
        this.center = center;
        this.radius = radius;

        this.blockPosLongs = new long[positions.size()];
        for (int i = 0; i < positions.size(); i++)
            this.blockPosLongs[i] = positions.get(i).asLong();
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(center.x);
        data.writeDouble(center.y);
        data.writeDouble(center.z);
        data.writeFloat(radius);

        data.writeVarInt(blockPosLongs.length);
        for (long l : blockPosLongs)
            data.writeLong(l);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        center = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        radius = data.readFloat();

        int n = data.readVarInt();
        blockPosLongs = new long[n];
        for (int i = 0; i < n; i++)
            blockPosLongs[i] = data.readLong();
    }

    /** Below this crater radius, block bursts render at their original vanilla-TNT-like size. */
    private static final float MIN_SCALE_RADIUS = 4.0F;
    /** Crater radius at which block bursts reach their largest size. */
    private static final float MAX_SCALE_RADIUS = 48.0F;
    private static final float MAX_PARTICLE_SCALE = 5.0F;
    /** Above this crater radius, block bursts switch to the bigger smoke variant. */
    private static final float BIG_SMOKE_RADIUS = 16.0F;
    /** Burst life at the smallest size, roughly the vanilla puff, extended in step with the scale. */
    private static final float BASE_LIFETIME_TICKS = 20.0F;

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        // Fewer, bigger bursts read as "one big blast" instead of "the same tiny burst,
        // just more of them" - the burst count is already bounded server-side (see
        // FlanExplosion.sampleBlockBurstPositions), so scale up their size here to match
        // how much crater they are meant to represent.
        float t = Mth.clamp((radius - MIN_SCALE_RADIUS) / (MAX_SCALE_RADIUS - MIN_SCALE_RADIUS), 0.0F, 1.0F);
        float particleScale = 1.0F + t * (MAX_PARTICLE_SCALE - 1.0F);
        String smokeParticle = radius >= BIG_SMOKE_RADIUS ? FlanParticles.FM_BIG_SMOKE : FlanParticles.SMOKE;
        // Life tracks size for the same reason the fireball's does: a burst several blocks
        // across that pops out in the vanilla puff's lifetime reads as a flicker. The
        // explosion sprite animates off age/lifetime, so this plays out slower rather than
        // holding a frame.
        int lifetime = Mth.ceil(BASE_LIFETIME_TICKS * particleScale);

        for (long l : blockPosLongs)
        {
            BlockPos pos = BlockPos.of(l);
            spawnBlockBurst(level, center, pos, radius, particleScale, smokeParticle, lifetime);
        }
    }

    private void spawnBlockBurst(Level level, Vec3 center, BlockPos pos, float radius, float particleScale, String smokeParticle, int lifetime)
    {
        double px = pos.getX() + level.random.nextDouble();
        double py = pos.getY() + level.random.nextDouble();
        double pz = pos.getZ() + level.random.nextDouble();

        double dx = px - center.x;
        double dy = py - center.y;
        double dz = pz - center.z;

        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6)
            return;

        dx /= len; dy /= len; dz /= len;

        // No hard clamp to the crater radius: debris naturally overshoots it a little,
        // which reads as fragments being flung out rather than smoke stopping dead at
        // an invisible wall.
        double scale = 0.5D / (len / radius + 0.1D);
        scale *= (level.random.nextDouble() * level.random.nextDouble() + 0.3D);

        double vx = dx * scale;
        double vy = dy * scale;
        double vz = dz * scale;

        // Jittered per burst so the crater thins out gradually instead of clearing on one tick.
        int jittered = Math.max(1, Mth.ceil(lifetime * (0.75F + level.random.nextFloat() * 0.5F)));
        ClientHooks.RENDER.spawnParticle(FlanParticles.LARGE_EXPLODE, (px + center.x) / 2.0D, (py + center.y) / 2.0D, (pz + center.z) / 2.0D, vx, vy, vz, particleScale, jittered);
        ClientHooks.RENDER.spawnParticle(smokeParticle, px, py, pz, vx, vy, vz, particleScale, jittered);
    }
}
