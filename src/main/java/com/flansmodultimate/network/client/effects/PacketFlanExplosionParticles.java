package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.explosions.ExplosionVisuals;
import com.flansmodultimate.common.explosions.FragmentationModel;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
    /** Start clear of the brightest part of the fireball, without jumping far into the damage envelope. */
    private static final float FRAG_START_CRATER_SHARE = 0.65F;
    private static final float FRAG_START_RADIUS_SHARE = 0.2F;
    /** Keep the denser fragment spray fine-grained instead of firework-sized. */
    private static final float FRAG_SPARK_SCALE = 0.5F;

    /** How fast the smoke column climbs, per block of crater radius. Gravity arcs it over. */
    private static final float COLUMN_RISE_PER_RADIUS = 0.08F;
    /** How wide the foot of the column is, as a share of the crater radius. */
    private static final float COLUMN_FOOT_SHARE = 0.35F;

    private Vec3 position;
    private int numSmoke;
    private int numDebris;
    private float blastRadius;
    /** Charge-normalized visual reference radius, not the configured gameplay crater radius. */
    private float explosionRadius;
    private float fragRadius;
    private float fragIntensity;
    private double effectiveFragments;
    private FragmentationModel.Pattern fragPattern = FragmentationModel.Pattern.RADIAL;
    private Vec3 fragForward = Vec3.ZERO;
    /** Drawn as a fire explosion: the fireball burns orange throughout rather than cooling to grey. */
    private boolean fiery;

    // The constructor mirrors the wire fields of the packet.
    @SuppressWarnings("java:S107")
    public PacketFlanExplosionParticles(Vec3 position, int numSmoke, int numDebris, float blastRadius, float explosionRadius, float fragRadius, float fragIntensity, double effectiveFragments,
        FragmentationModel.Pattern fragPattern, Vec3 fragForward, boolean fiery)
    {
        this.position = position;
        this.numSmoke = numSmoke;
        this.numDebris = numDebris;
        this.blastRadius = blastRadius;
        this.explosionRadius = explosionRadius;
        this.fragRadius = fragRadius;
        this.fragIntensity = fragIntensity;
        this.effectiveFragments = effectiveFragments;
        this.fragPattern = fragPattern;
        this.fragForward = fragForward;
        this.fiery = fiery;
    }

    @Override
    public void encodeInto(PacketBuffer data)
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
        data.writeDouble(effectiveFragments);
        data.writeInt(fragPattern.ordinal());
        data.writeDouble(fragForward.x);
        data.writeDouble(fragForward.y);
        data.writeDouble(fragForward.z);
        data.writeBoolean(fiery);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numSmoke = data.readInt();
        numDebris = data.readInt();
        blastRadius = data.readFloat();
        explosionRadius = data.readFloat();
        fragRadius = data.readFloat();
        fragIntensity = data.readFloat();
        effectiveFragments = data.readDouble();
        int patternId = data.readInt();
        fragPattern = patternId >= 0 && patternId < FragmentationModel.Pattern.values().length ? FragmentationModel.Pattern.values()[patternId] : FragmentationModel.Pattern.RADIAL;
        fragForward = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        fiery = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        float lifetimeScale = ExplosionVisuals.lifetimeScale(explosionRadius);
        boolean groundBurst = isGroundBurst(level, position);

        spawnFlash();
        spawnFireball(level);

        // Keep burning ejecta compact and short-lived; fragmentation has its own envelope cues.
        float diameter = ExplosionVisuals.fireballDiameter(explosionRadius);
        float particleScale = Mth.clamp(diameter * 0.04F, 0.25F, 1.2F);
        float ejectaSpeed = Math.min(1.5F, diameter * 0.04F);
        spawn(level, FlanParticles.FM_FLARE, position, ExplosionVisuals.scaledCount(numSmoke, explosionRadius), ejectaSpeed, particleScale, lifetimeScale * 0.03F);
        spawn(level, FlanParticles.FM_DEBRIS_1, position, ExplosionVisuals.scaledCount(numDebris, explosionRadius), ejectaSpeed, particleScale, lifetimeScale);

        // The envelope cues go before the extra layers deliberately: they tell a player how far
        // the explosion can actually hurt them, so if a barrage exhausts the client's particle
        // budget it should be the decoration that thins out and not the information.
        spawnShockwave(level);
        spawnFragmentation(level, groundBurst);
        for (ExplosionVisuals.Layer layer : ExplosionVisuals.LAYERS)
            spawnLayer(level, layer, lifetimeScale, groundBurst);
        spawnSmokeColumn(level);

        // The afterglow, dust skirt, fireball stem and mushroom cap play out over the following
        // ticks, so they draw on later ticks' particle budgets rather than competing with the above.
        ClientHooks.RENDER.spawnExplosionSpectacle(position, explosionRadius, blastRadius, groundBurst, fiery);
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
        double spread = ExplosionVisuals.fireballDiameter(explosionRadius) * layer.spreadShare();
        double speed = ExplosionVisuals.fireballDiameter(explosionRadius) * layer.speedShare();

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

            ClientHooks.RENDER.spawnParticle(layer.particle(), at.x, at.y, at.z, dx * speed * jitter, vertical * speed * jitter, dz * speed * jitter,
                Math.min(1F, ExplosionVisuals.fireballDiameter(explosionRadius) * 0.12F), lifetimeScale * layer.lifetimeShare());
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
     * One brief burst, with the scatter included in its diameter. Particle lifetimes are shortened
     * to the hot-phase budget rather than replenishing animated fire for several seconds.
     * The staged smoke supplies the cooling cloud independently.
     */
    private void spawnFireball(Level level)
    {
        if (explosionRadius <= 0F)
            return;
        float diameter = ExplosionVisuals.fireballDiameter(explosionRadius);
        int hotTicks = ExplosionVisuals.fireballHotTicks(explosionRadius, fiery);
        String particle = hotTicks > 0 ? FlanParticles.FM_FIRE_EXPLOSION : FlanParticles.LARGE_EXPLODE;
        int duration = hotTicks > 0 ? hotTicks : ExplosionVisuals.fireballDurationTicks(explosionRadius);
        // Fire sprites live 8-11 ticks; the grey vanilla puff lives 6-9. Bound the final puff's life.
        float lifetime = duration / (hotTicks > 0 ? 11F : 9F);
        for (int i = 0; i < ExplosionVisuals.fireballCount(explosionRadius); i++)
        {
            double dx = (level.random.nextDouble() * 2D - 1D) * diameter * 0.15D;
            double dy = (level.random.nextDouble() * 2D - 1D) * diameter * 0.15D;
            double dz = (level.random.nextDouble() * 2D - 1D) * diameter * 0.15D;
            Vec3 at = place(level, dx, dy, dz);
            if (at != null)
                ClientHooks.RENDER.spawnParticle(particle, at.x, at.y, at.z, 0D, 0D, 0D, ExplosionVisuals.fireballScale(explosionRadius), lifetime);
        }
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

            ClientHooks.RENDER.spawnParticle(FlanParticles.FM_SMOKE, position.x + dx * start, position.y + blastRadius * SHOCKWAVE_HEIGHT_SHARE, position.z + dz * start, dx * speed * wobble,
                speed * 0.05D, dz * speed * wobble, 1.0F);
        }
    }

    /**
     * Sparks thrown out along the fragmentation envelope. They are the briefest layer here because
     * fragments genuinely arrive all at once: what is left a second later is smoke, not steel.
     */
    private void spawnFragmentation(Level level, boolean groundBurst)
    {
        int count = ExplosionVisuals.fragSparkCount(fragRadius, fragIntensity, effectiveFragments, ModClientConfig.get().fragSparkMultiplier, ModClientConfig.get().maxFragSparks);
        if (count <= 0)
            return;

        RandomSource random = level.random;
        float lifetimeScale = ExplosionVisuals.fragLifetimeScale(explosionRadius);
        double speed = fragRadius * FRAG_SPEED_PER_RADIUS;
        double start = Math.min(Math.max(0.35F, explosionRadius * FRAG_START_CRATER_SHARE), fragRadius * FRAG_START_RADIUS_SHARE);

        for (int i = 0; i < count; i++)
        {
            Vec3 direction = fragmentSparkDirection(random, groundBurst);
            double jitter = 0.5D + random.nextDouble();
            Vec3 origin = position.add(direction.scale(start));
            if (isBuried(level, origin))
                origin = position.add(0D, Math.max(0.5D, start), 0D);
            if (isBuried(level, origin))
                continue;

            ClientHooks.RENDER.spawnParticle(FlanParticles.FIREWORKS_SPARK, origin.x, origin.y, origin.z, direction.x * speed * jitter, direction.y * speed * jitter, direction.z * speed * jitter,
                FRAG_SPARK_SCALE, lifetimeScale);
        }
    }

    private Vec3 fragmentSparkDirection(RandomSource random, boolean groundBurst)
    {
        double angle = random.nextDouble() * Mth.TWO_PI;
        if (fragPattern == FragmentationModel.Pattern.RADIAL)
        {
            double vertical = (random.nextDouble() * 2D - 1D) * (1D - FRAG_UPWARD_BIAS) + FRAG_UPWARD_BIAS;
            if (groundBurst)
                vertical = Math.abs(vertical);
            double horizontal = Math.sqrt(Math.max(0D, 1D - vertical * vertical));
            return new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
        }
        if (fragPattern == FragmentationModel.Pattern.HORIZONTAL_BAND)
        {
            double elevation = Math.toRadians((random.nextDouble() * 2D - 1D) * 8D);
            return new Vec3(Math.cos(angle) * Math.cos(elevation), Math.sin(elevation), Math.sin(angle) * Math.cos(elevation));
        }
        Vec3 forward = fragForward.lengthSqr() > 1.0E-9D ? fragForward.normalize() : new Vec3(0D, 0D, 1D);
        if (fragPattern == FragmentationModel.Pattern.FORWARD_FAN)
        {
            Vec3 horizontalForward = new Vec3(forward.x, 0D, forward.z);
            horizontalForward = horizontalForward.lengthSqr() > 1.0E-9D ? horizontalForward.normalize() : new Vec3(0D, 0D, 1D);
            double yaw = Math.toRadians((random.nextDouble() * 2D - 1D) * 30D);
            double elevation = Math.toRadians((random.nextDouble() * 2D - 1D) * 8D);
            Vec3 right = new Vec3(-horizontalForward.z, 0D, horizontalForward.x);
            return horizontalForward.scale(Math.cos(yaw) * Math.cos(elevation)).add(right.scale(Math.sin(yaw) * Math.cos(elevation))).add(0D, Math.sin(elevation), 0D);
        }
        Vec3 right = forward.cross(new Vec3(0D, 1D, 0D));
        if (right.lengthSqr() < 1.0E-9D)
            right = new Vec3(1D, 0D, 0D);
        right = right.normalize();
        Vec3 up = forward.cross(right).normalize();
        double cosTheta = Math.cos(Math.toRadians(5D)) + random.nextDouble() * (1D - Math.cos(Math.toRadians(5D)));
        double sinTheta = Math.sqrt(1D - cosTheta * cosTheta);
        return forward.scale(cosTheta).add(right.scale(Math.cos(angle) * sinTheta)).add(up.scale(Math.sin(angle) * sinTheta));
    }

    /**
     * The column that stands over a heavy charge after the fireball has gone out. This is what
     * separates a demolition round from a tank shell at a glance, and it is why the heavy end of
     * the curve is allowed to linger: the crater is only visible from close up, but the column is
     * visible from across the map.
     */
    private void spawnSmokeColumn(Level level)
    {
        int count = ExplosionVisuals.smokeColumnCount(explosionRadius);
        if (count <= 0)
            return;

        RandomSource random = level.random;
        double rise = ExplosionVisuals.stagedSizingRadius(explosionRadius) * COLUMN_RISE_PER_RADIUS;
        double foot = ExplosionVisuals.stagedSizingRadius(explosionRadius) * COLUMN_FOOT_SHARE;
        float columnLifetime = ExplosionVisuals.lifetimeScale(explosionRadius);

        for (int i = 0; i < count; i++)
        {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = random.nextDouble() * foot;

            ClientHooks.RENDER.spawnParticle(FlanParticles.LARGE_SMOKE, position.x + Math.cos(angle) * distance, position.y, position.z + Math.sin(angle) * distance, Math.cos(angle) * rise * 0.15D,
                rise * (0.7D + random.nextDouble() * 0.6D), Math.sin(angle) * rise * 0.15D, 1.0F, columnLifetime);
        }
    }

    private void spawn(Level level, String particleType, Vec3 position, int count, float maxVelocity, float scale, float lifetimeScale)
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
