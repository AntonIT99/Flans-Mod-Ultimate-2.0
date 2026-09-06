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
    /** How long the fireball keeps burning at zero radius, before the linear term. */
    private static final float FIREBALL_BASE_DURATION_TICKS = 20.0F;
    /** Extra ticks of burning per block of crater radius. */
    private static final float FIREBALL_DURATION_PER_RADIUS = 1.0F;
    private static final int MAX_FIREBALL_DURATION_TICKS = 200;

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
        spawnFireball(position, explosionRadius);

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
     * How long it burns for is linear in the radius as well, but that duration is achieved by
     * replacing puffs as they expire rather than by stretching any one of them: a single sprite
     * told to live ten times longer just animates ten times slower. Each puff runs its own
     * animation at its natural speed, and fresh ones keep taking over until the time is up.
     */
    private void spawnFireball(Vec3 center, float explosionRadius)
    {
        if (explosionRadius <= 0F)
            return;

        float scale = Mth.clamp(explosionRadius * FIREBALL_SCALE_PER_RADIUS, 1.0F, MAX_FIREBALL_SCALE);
        int burstSize = Mth.clamp(Mth.ceil(explosionRadius / RADIUS_PER_FIREBALL), 1, MAX_FIREBALLS);
        int duration = Mth.clamp(Mth.ceil(FIREBALL_BASE_DURATION_TICKS + explosionRadius * FIREBALL_DURATION_PER_RADIUS),
            1, MAX_FIREBALL_DURATION_TICKS);

        double spread = explosionRadius * 0.3D;
        double drift = explosionRadius * 0.02D;

        ClientHooks.RENDER.spawnSustainedParticles(FlanParticles.LARGE_EXPLODE,
            center.x, center.y, center.z, spread, drift, scale, burstSize, duration);
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
