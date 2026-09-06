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
    /** Blast radius at which flare/debris particles reach their largest size. */
    private static final float MAX_SCALE_RADIUS = 60.0F;
    private static final float MAX_PARTICLE_SCALE = 4.0F;

    /** Fireball quad size per block of crater radius - the linear term. */
    private static final float FIREBALL_SCALE_PER_RADIUS = 0.5F;
    /**
     * Ceiling on the linear term. At the default maxExplosionRadius of 128 the fireball scales
     * linearly over the whole range and only just reaches this, so the clamp bites only when a
     * server raises that config well past its default rather than shaping ordinary explosions.
     */
    private static final float MAX_FIREBALL_SCALE = 64.0F;
    /** One fireball sprite per this many blocks of crater radius. */
    private static final float RADIUS_PER_FIREBALL = 4.0F;
    private static final int MAX_FIREBALLS = 12;
    /** Fireball life at zero radius, before the linear term. Roughly the vanilla puff. */
    private static final float FIREBALL_BASE_LIFETIME_TICKS = 20.0F;
    /** Extra ticks of fireball life per block of crater radius. */
    private static final float FIREBALL_LIFETIME_PER_RADIUS = 1.0F;
    private static final int MAX_FIREBALL_LIFETIME_TICKS = 200;

    private Vec3 position;
    private int numSmoke;
    private int numDebris;
    private float blastRadius;
    private float explosionRadius;

    public PacketFlanExplosionParticles(Vec3 position, int numSmoke, int numDebris, float blastRadius, float explosionRadius)
    {
        this.position = position;
        this.numSmoke = numSmoke;
        this.numDebris = numDebris;
        this.blastRadius = blastRadius;
        this.explosionRadius = explosionRadius;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeInt(numSmoke);
        data.writeInt(numDebris);
        data.writeFloat(blastRadius);
        data.writeFloat(explosionRadius);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numSmoke = data.readInt();
        numDebris = data.readInt();
        blastRadius = data.readFloat();
        explosionRadius = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        spawnFireball(level, position, explosionRadius);

        // Bigger blasts get bigger flares and debris rather than just more of them, both for
        // the visual read (a huge charge should look huge, not just noisy) and because the
        // particle count still comes from the content pack's flat FlareParticleCount /
        // DebrisParticleCount, which does not scale with the blast on its own.
        float t = Mth.clamp(blastRadius / MAX_SCALE_RADIUS, 0.0F, 1.0F);
        float particleScale = 1.0F + t * (MAX_PARTICLE_SCALE - 1.0F);

        spawn(level, FlanParticles.FM_FLARE, position, numSmoke, blastRadius * 0.12F, particleScale);
        spawn(level, FlanParticles.FM_DEBRIS_1, position, numDebris, blastRadius * 0.12F, particleScale);
    }

    /**
     * The classic Minecraft explosion puff, blown up to match the charge. Its size is linear in
     * the crater radius, which is what makes a heavy charge actually read as heavy: the vanilla
     * emitter is a fixed size no matter the yield, so without this a 100 block detonation looks
     * exactly like a stick of TNT viewed from further away. A handful of sprites are scattered
     * through the crater rather than one pinned to the centre, so the fireball keeps some depth
     * instead of reading as a single flat billboard.
     * <p>
     * Life scales linearly with the radius too. A sprite tens of blocks across that vanishes in
     * the vanilla puff's a second or so reads as a flicker rather than a detonation, and because
     * the explosion particle picks its animation frame from age/lifetime, a longer life plays the
     * same frames out slowly - the fireball billows and dissipates instead of blinking out.
     */
    private void spawnFireball(Level level, Vec3 center, float explosionRadius)
    {
        if (explosionRadius <= 0F)
            return;

        float scale = Mth.clamp(explosionRadius * FIREBALL_SCALE_PER_RADIUS, 1.0F, MAX_FIREBALL_SCALE);
        int count = Mth.clamp(Mth.ceil(explosionRadius / RADIUS_PER_FIREBALL), 1, MAX_FIREBALLS);
        int lifetime = Mth.clamp(Mth.ceil(FIREBALL_BASE_LIFETIME_TICKS + explosionRadius * FIREBALL_LIFETIME_PER_RADIUS),
            1, MAX_FIREBALL_LIFETIME_TICKS);

        double spread = explosionRadius * 0.3D;
        double drift = explosionRadius * 0.02D;

        for (int i = 0; i < count; i++)
        {
            // The first sprite always sits dead centre so there is a core to the fireball
            // however few of them the radius earns.
            double ox = i == 0 ? center.x : center.x + level.random.nextGaussian() * spread;
            double oy = i == 0 ? center.y : center.y + level.random.nextGaussian() * spread * 0.6D;
            double oz = i == 0 ? center.z : center.z + level.random.nextGaussian() * spread;

            double vx = level.random.nextGaussian() * drift;
            double vy = Math.abs(level.random.nextGaussian()) * drift;
            double vz = level.random.nextGaussian() * drift;

            // Jittered per sprite so the cluster thins out gradually instead of the whole
            // fireball disappearing on one tick.
            int jittered = Math.max(1, Mth.ceil(lifetime * (0.75F + level.random.nextFloat() * 0.5F)));
            ClientHooks.RENDER.spawnParticle(FlanParticles.LARGE_EXPLODE, ox, oy, oz, vx, vy, vz, scale, jittered);
        }
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
