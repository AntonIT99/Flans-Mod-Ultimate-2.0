package com.flansmodultimate.network.client;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.explosions.ExplosionVisuals;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The spectacle of a detonation, layered so that what a player sees matches what the explosion
 * actually does.
 * <p>
 * Every layer is sized by {@link ExplosionVisuals} from the explosion's own radii, so a .50 cal HE
 * round flashes and is gone while a demolition charge flares, throws fragments out along its
 * fragmentation envelope, drives a wave across its blast envelope and leaves a column standing.
 * The three radii are deliberately given separate visuals rather than all being drawn from the
 * crater: the blast and fragmentation radii are where an explosion is dangerous, and a player who
 * can see how far they reach can judge cover.
 */
@NoArgsConstructor
public class PacketFlanExplosionParticles implements IClientPacket
{
    /** Blast radius at which flare/debris particles reach their largest size. */
    private static final float MAX_SCALE_RADIUS = 60.0F;
    private static final float MAX_PARTICLE_SCALE = 4.0F;

    /** Where the shockwave ring starts, as a share of the blast radius it sweeps out to. */
    private static final float SHOCKWAVE_START_SHARE = 0.2F;
    /**
     * How long the ring has to get where it is going. The ring particle fades itself out over
     * roughly this many ticks whatever lifetime it is given, so the speed is sized against the
     * window in which it is actually visible - and the ring is deliberately left out of the
     * explosion's lifetime scaling for the same reason, a passing wave being brief by nature.
     */
    private static final float SHOCKWAVE_TRAVEL_TICKS = 10.0F;
    /** How far above the blast centre the ring is laid, as a share of the blast radius. */
    private static final float SHOCKWAVE_HEIGHT_SHARE = 0.04F;

    /** Fragment speed, in blocks per tick per block of fragmentation radius. */
    private static final float FRAG_SPEED_PER_RADIUS = 0.09F;
    /** Fragments are thrown slightly upwards on average rather than purely sideways. */
    private static final float FRAG_UPWARD_BIAS = 0.35F;

    /** How fast the smoke column climbs, per block of crater radius. Gravity arcs it over. */
    private static final float COLUMN_RISE_PER_RADIUS = 0.08F;
    /** How wide the foot of the column is, as a share of the crater radius. */
    private static final float COLUMN_FOOT_SHARE = 0.35F;

    private Vec3 position;
    private int numSmoke;
    private int numDebris;
    private float blastRadius;
    private float explosionRadius;
    private float fragRadius;
    private float fragIntensity;

    public PacketFlanExplosionParticles(Vec3 position, int numSmoke, int numDebris, float blastRadius,
                                        float explosionRadius, float fragRadius, float fragIntensity)
    {
        this.position = position;
        this.numSmoke = numSmoke;
        this.numDebris = numDebris;
        this.blastRadius = blastRadius;
        this.explosionRadius = explosionRadius;
        this.fragRadius = fragRadius;
        this.fragIntensity = fragIntensity;
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
        data.writeFloat(fragRadius);
        data.writeFloat(fragIntensity);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numSmoke = data.readInt();
        numDebris = data.readInt();
        blastRadius = data.readFloat();
        explosionRadius = data.readFloat();
        fragRadius = data.readFloat();
        fragIntensity = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        float lifetimeScale = ExplosionVisuals.lifetimeScale(explosionRadius);
        boolean groundBurst = isGroundBurst(level, position);

        spawnFlash();
        spawnFireball(lifetimeScale);

        // Bigger blasts get bigger flares and debris rather than just more of them, both for
        // the visual read (a huge charge should look huge, not just noisy) and because the
        // particle count still comes from the content pack's flat FlareParticleCount /
        // DebrisParticleCount, which does not scale with the blast on its own. The counts are
        // then grown for the charge too, so a heavy round is not limited to whatever number a
        // pack author happened to pick for a hand grenade.
        float t = Mth.clamp(blastRadius / MAX_SCALE_RADIUS, 0.0F, 1.0F);
        float particleScale = 1.0F + t * (MAX_PARTICLE_SCALE - 1.0F);

        spawn(level, FlanParticles.FM_FLARE, position, ExplosionVisuals.scaledCount(numSmoke, explosionRadius),
            blastRadius * 0.12F, particleScale, lifetimeScale);
        spawn(level, FlanParticles.FM_DEBRIS_1, position, ExplosionVisuals.scaledCount(numDebris, explosionRadius),
            blastRadius * 0.12F, particleScale, lifetimeScale);

        // The envelope cues go before the extra layers deliberately: they tell a player how far
        // the explosion can actually hurt them, so if a barrage exhausts the client's particle
        // budget it should be the decoration that thins out and not the information.
        spawnShockwave(level);
        spawnFragmentation(level, groundBurst);
        for (ExplosionVisuals.Layer layer : ExplosionVisuals.LAYERS)
            spawnLayer(level, layer, lifetimeScale, groundBurst);
        spawnSmokeColumn(level, lifetimeScale);
    }

    /**
     * One of the extra particle types a large detonation gets around its fireball. The fireball
     * itself is the right look already; this is the variety around it, and a single loop drives
     * every layer from the table in {@link ExplosionVisuals#LAYERS} so the whole set can be
     * retuned in one place rather than spread over a method each.
     */
    private void spawnLayer(Level level, ExplosionVisuals.Layer layer, float lifetimeScale, boolean groundBurst)
    {
        int count = layer.count(explosionRadius);
        if (count <= 0)
            return;

        RandomSource random = level.random;
        double spread = explosionRadius * layer.spreadShare();
        double speed = explosionRadius * layer.speedShare();

        for (int i = 0; i < count; i++)
        {
            // One direction shapes both where the particle starts and where it goes, so each layer
            // reads as a burst out of the centre rather than as a cloud that happens to drift.
            double angle = random.nextDouble() * Mth.TWO_PI;
            double vertical = (random.nextDouble() * 2.0D - 1.0D) * (1.0D - layer.upwardBias()) + layer.upwardBias();
            if (groundBurst)
                vertical = Math.abs(vertical);
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
            double dx = Math.cos(angle) * horizontal;
            double dz = Math.sin(angle) * horizontal;

            // Cube root so the scatter fills the sphere evenly instead of bunching at its centre.
            double distance = spread * Math.cbrt(random.nextDouble());
            double jitter = 0.5D + random.nextDouble();

            Vec3 at = place(level, dx * distance, vertical * distance, dz * distance);
            if (at == null)
                continue;

            ClientHooks.RENDER.spawnParticle(layer.particle(), at.x, at.y, at.z,
                dx * speed * jitter, vertical * speed * jitter, dz * speed * jitter,
                1.0F, lifetimeScale * layer.lifetimeShare());
        }
    }

    /**
     * Whether this explosion went off against the ground rather than in open air.
     * <p>
     * It matters because a ground burst has nothing below it to throw anything into. Scattering a
     * full sphere of particles around a round that struck the floor buries about half of them
     * inside terrain, where the block faces in front of them hide them completely - which is why a
     * shot at the ground showed noticeably less than the same shot into the air. Folding the
     * downward half of every layer upwards spends those particles where they can be seen, and is
     * also what actually happens: a charge against a surface vents upwards.
     */
    private static boolean isGroundBurst(Level level, Vec3 centre)
    {
        BlockPos below = BlockPos.containing(centre.x, centre.y - 0.5D, centre.z);
        return level.getBlockState(below).isSolidRender(level, below);
    }

    /**
     * Where to actually put a particle the explosion wanted at this offset from its centre, or
     * {@code null} if there is nowhere it would be seen.
     * <p>
     * {@link #isGroundBurst} handles the common case, but an explosion against a wall, under an
     * overhang or inside a doorway can still place particles inside blocks. Mirroring a buried
     * offset to the opposite side of the centre usually puts it back in the open air, and giving up
     * on the few that are buried either way costs nothing, since those would not have been drawn.
     */
    @Nullable
    private Vec3 place(Level level, double dx, double dy, double dz)
    {
        Vec3 wanted = position.add(dx, dy, dz);
        if (!isBuried(level, wanted))
            return wanted;

        Vec3 mirrored = position.subtract(dx, dy, dz);
        return isBuried(level, mirrored) ? null : mirrored;
    }

    private static boolean isBuried(Level level, Vec3 at)
    {
        BlockPos pos = BlockPos.containing(at);
        return level.getBlockState(pos).isSolidRender(level, pos);
    }

    /**
     * A single bright flash at the seat of the detonation. Every explosion gets one whatever its
     * size, because the flash is the part that is genuinely instantaneous: on a small round it is
     * most of what there is to see, and on a heavy one it is what announces the fireball.
     */
    private void spawnFlash()
    {
        float scale = ExplosionVisuals.flashScale(explosionRadius);
        if (scale <= 0F)
            return;
        ClientHooks.RENDER.spawnParticle(FlanParticles.FM_FLASH, position.x, position.y, position.z, 0D, 0D, 0D, scale);
    }

    /**
     * The classic Minecraft explosion puff, blown up to match the charge. Its size is linear in
     * the crater radius, which is what makes a heavy charge actually read as heavy: the vanilla
     * emitter is a fixed size no matter the yield, so without this a 100 block detonation looks
     * exactly like a stick of TNT viewed from further away. A handful of sprites are scattered
     * through the crater rather than one pinned to the centre, so the fireball keeps some depth
     * instead of reading as a single flat billboard.
     * <p>
     * How long it burns for comes from the charge as well, but that duration is achieved by
     * replacing puffs as they expire rather than by stretching any one of them: a single sprite
     * told to live ten times longer just animates ten times slower. Each puff runs its own
     * animation at its natural speed, and fresh ones keep taking over until the time is up. A
     * small round asks for a duration under one puff's life, which leaves it a single burst.
     */
    private void spawnFireball(float lifetimeScale)
    {
        if (explosionRadius <= 0F)
            return;

        double spread = explosionRadius * 0.3D;
        double drift = explosionRadius * 0.02D;

        ClientHooks.RENDER.spawnSustainedParticles(FlanParticles.LARGE_EXPLODE,
            position.x, position.y, position.z, spread, drift,
            ExplosionVisuals.fireballScale(explosionRadius),
            ExplosionVisuals.fireballCount(explosionRadius),
            ExplosionVisuals.fireballDurationTicks(explosionRadius), lifetimeScale);
    }

    /**
     * A ring of dust driven outwards across the overpressure envelope, so the radius that knocks
     * you down and the radius you can see are the same radius. It is laid flat just off the ground
     * and travels outwards rather than billowing up, which is how a ground burst reads from the
     * outside - and it is the only cue a player gets that a charge reaches well past its crater.
     */
    private void spawnShockwave(Level level)
    {
        int count = ExplosionVisuals.shockwaveCount(blastRadius);
        if (count <= 0)
            return;

        RandomSource random = level.random;
        double speed = blastRadius * (1.0F - SHOCKWAVE_START_SHARE) / SHOCKWAVE_TRAVEL_TICKS;
        double start = blastRadius * SHOCKWAVE_START_SHARE;

        for (int i = 0; i < count; i++)
        {
            // Evenly spaced with a little jitter, so the ring stays a ring without looking stamped.
            double angle = (i + random.nextDouble() * 0.6D - 0.3D) / count * Mth.TWO_PI;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double wobble = 0.8D + random.nextDouble() * 0.4D;

            ClientHooks.RENDER.spawnParticle(FlanParticles.FM_SMOKE,
                position.x + dx * start, position.y + blastRadius * SHOCKWAVE_HEIGHT_SHARE, position.z + dz * start,
                dx * speed * wobble, speed * 0.05D, dz * speed * wobble, 1.0F);
        }
    }

    /**
     * Sparks thrown out along the fragmentation envelope. They are the briefest layer here because
     * fragments genuinely arrive all at once: what is left a second later is smoke, not steel.
     */
    private void spawnFragmentation(Level level, boolean groundBurst)
    {
        int count = ExplosionVisuals.fragSparkCount(fragRadius, fragIntensity);
        if (count <= 0)
            return;

        RandomSource random = level.random;
        float lifetimeScale = ExplosionVisuals.fragLifetimeScale(explosionRadius);
        double speed = fragRadius * FRAG_SPEED_PER_RADIUS;

        for (int i = 0; i < count; i++)
        {
            // Thrown over a sphere biased upwards, and entirely upwards off the ground: fragments
            // aimed into the dirt are spent on sparks that are never drawn.
            double angle = random.nextDouble() * Mth.TWO_PI;
            double vertical = (random.nextDouble() * 2.0D - 1.0D) * (1.0D - FRAG_UPWARD_BIAS) + FRAG_UPWARD_BIAS;
            if (groundBurst)
                vertical = Math.abs(vertical);
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
            double jitter = 0.5D + random.nextDouble();

            ClientHooks.RENDER.spawnParticle(FlanParticles.FIREWORKS_SPARK,
                position.x, position.y, position.z,
                Math.cos(angle) * horizontal * speed * jitter, vertical * speed * jitter,
                Math.sin(angle) * horizontal * speed * jitter, 1.0F, lifetimeScale);
        }
    }

    /**
     * The column that stands over a heavy charge after the fireball has gone out. This is what
     * separates a demolition round from a tank shell at a glance, and it is why the heavy end of
     * the curve is allowed to linger: the crater is only visible from close up, but the column is
     * visible from across the map.
     */
    private void spawnSmokeColumn(Level level, float lifetimeScale)
    {
        int count = ExplosionVisuals.smokeColumnCount(explosionRadius);
        if (count <= 0)
            return;

        RandomSource random = level.random;
        double rise = explosionRadius * COLUMN_RISE_PER_RADIUS;
        double foot = explosionRadius * COLUMN_FOOT_SHARE;
        // The column source is already the longest-lived particle in the explosion - fifteen
        // seconds of smoke on its own - and it only exists on charges the scale wants to linger,
        // so it is allowed to be cut short but never stretched past what it was authored for.
        float columnLifetime = Math.min(lifetimeScale, 1.0F);

        for (int i = 0; i < count; i++)
        {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = random.nextDouble() * foot;

            ClientHooks.RENDER.spawnParticle(FlanParticles.FM_BIG_SMOKE,
                position.x + Math.cos(angle) * distance, position.y, position.z + Math.sin(angle) * distance,
                Math.cos(angle) * rise * 0.15D, rise * (0.7D + random.nextDouble() * 0.6D),
                Math.sin(angle) * rise * 0.15D, 1.0F, columnLifetime);
        }
    }

    private void spawn(Level level, String particleType, Vec3 position, int count, float maxVelocity,
                       float scale, float lifetimeScale)
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
            ClientHooks.RENDER.spawnParticle(particleType, position.x, position.y, position.z, vx, vy, vz, scale, lifetimeScale);
        }
    }
}
