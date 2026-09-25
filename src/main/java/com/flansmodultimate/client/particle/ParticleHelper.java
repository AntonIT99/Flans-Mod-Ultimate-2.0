package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.client.ParticlePlatform;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleHelper
{
    /** Simultaneous sustained emissions, so a barrage cannot stack unbounded emitters. */
    private static final int MAX_ACTIVE_EMITTERS = 16;
    /** Smoke-launcher shells in flight at once, the cap the server used to keep per vehicle. */
    private static final int MAX_SMOKE_SHELLS = 64;
    /** Reported when no particle was created, so a caller cannot mistake it for a lifetime. */
    private static final int NO_PARTICLE = -1;
    /**
     * Wave interval used when the opening wave produced nothing to measure, roughly the life of a
     * vanilla explosion puff. That happens when the burst was culled for distance or ran into the
     * per-tick particle budget, which a barrage can do; the emission still has to register or the
     * whole effect is lost rather than just its first wave.
     */
    private static final int FALLBACK_WAVE_INTERVAL_TICKS = 8;

    private static final Map<String, Optional<ParticleOptions>> PARTICLE_OPTIONS_CACHE = new ConcurrentHashMap<>();
    private static final List<SustainedEmission> ACTIVE_EMISSIONS = new ArrayList<>();
    private static final List<SmokeShell> SMOKE_SHELLS = new ArrayList<>();
    private static long particleBudgetTick = Long.MIN_VALUE;
    private static int particlesCreatedThisTick;

    /**
     * Spawns a sub-particle from inside another particle's tick, honouring this mod's particle
     * render distance rather than vanilla's.
     * <p>
     * {@code ClientLevel.addParticle} routes through {@code LevelRenderer.addParticleInternal},
     * which discards anything more than 32 blocks from the camera. That budget suits an ambient
     * torch flame, but several of this mod's particles are only a controller whose whole visible
     * effect is the sub-particles it emits - the vehicle smoke launcher is the clearest case, as
     * its cloud is entirely the exhaust that {@code BigSmokeParticle} gives off - so under vanilla's
     * cull a smoke screen simply did not exist beyond 32 blocks, which is well inside the range it
     * is deployed at. Going straight to the particle engine lifts that, and {@link #shouldSpawn}
     * still applies the configured render distance, the distance density falloff and the per-tick
     * budget, so the cost stays bounded by the client's own settings instead of a fixed number.
     */
    public static void spawnSubParticle(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz)
    {
        if (shouldSpawn(x, y, z))
            Minecraft.getInstance().particleEngine.createParticle(options, x, y, z, vx, vy, vz);
    }

    /** As {@link #spawnSubParticle(ParticleOptions, double, double, double, double, double, double)}, at rest. */
    public static void spawnSubParticle(ParticleOptions options, double x, double y, double z)
    {
        spawnSubParticle(options, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    public static void spawnFromString(String s, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        spawnFromString(s, x, y, z, vx, vy, vz, scale, 1.0F);
    }

    public static void spawnFromString(String s, double x, double y, double z, double vx, double vy, double vz, float scale, float lifetimeScale)
    {
        spawnMeasured(s, x, y, z, vx, vy, vz, scale, lifetimeScale);
    }

    /**
     * Spawns one particle and reports the lifetime it ended up with, which is what lets a sustained
     * emission pace its waves against the particle actually in play instead of an assumed figure.
     *
     * @return the particle's lifetime in ticks, or {@link #NO_PARTICLE} when none was created
     */
    private static int spawnMeasured(String s, double x, double y, double z, double vx, double vy, double vz, float scale, float lifetimeScale)
    {
        if (!shouldSpawn(x, y, z))
            return NO_PARTICLE;

        String normalized = normalize(s);
        if (normalized == null)
        {
            warnCouldNotParse(s);
            return NO_PARTICLE;
        }

        Optional<LegacyResourceRequest> legacyRequest = LegacyResourceRequest.parse(normalized, false);
        if (legacyRequest.isPresent())
        {
            if (!spawnLegacyResourceParticle(legacyRequest.get(), BlockPos.containing(x, y, z), x, y, z, vx, vy, vz, scale, lifetimeScale))
                warnCouldNotParse(s);
            return NO_PARTICLE;
        }

        // Legacy entity-effect particles take their colour from the velocity, so they are not cached by name.
        Optional<ParticleOptions> opt = switch (normalized)
        {
            case FlanParticles.MOB_SPELL -> Optional.of(ParticlePlatform.entityEffect(false, vx, vy, vz));
            case FlanParticles.MOB_SPELL_AMBIENT -> Optional.of(ParticlePlatform.entityEffect(true, vx, vy, vz));
            default -> PARTICLE_OPTIONS_CACHE.computeIfAbsent(normalized, ParticleHelper::toNamedOptions);
        };
        if (opt.isEmpty())
        {
            warnCouldNotParse(s);
            return NO_PARTICLE;
        }

        Particle particle = Minecraft.getInstance().particleEngine.createParticle(opt.get(), x, y, z, vx, vy, vz);
        if (particle == null)
            return NO_PARTICLE;

        scaleParticle(particle, scale);
        return scaleLifetime(particle, lifetimeScale);
    }

    /**
     * Emits {@code burstSize} particles now and then keeps topping them up for {@code durationTicks},
     * so the effect stays on screen for the whole duration while each individual particle still runs
     * its own animation at its own natural speed. Stretching one particle's lifetime instead would
     * slow its animation to a crawl; replacing it as it expires keeps the motion looking right.
     */
    public static void spawnSustained(String particleType, double x, double y, double z,
                                      double spread, double drift, float scale, int burstSize,
                                      int durationTicks, float lifetimeScale)
    {
        int puffLifetime = emitWave(particleType, x, y, z, spread, drift, scale, burstSize, lifetimeScale);
        if (ACTIVE_EMISSIONS.size() >= MAX_ACTIVE_EMITTERS)
            return;

        // Each wave replaces the one before it as its puffs die, so the interval is the lifetime
        // the particles actually got rather than an assumed one. The vanilla explosion puff lives
        // well under ten ticks, so pacing waves against a guess either thins the fireball out to
        // nothing or piles particles up; asking the particle itself cannot drift either way.
        int waveInterval = puffLifetime > 0 ? puffLifetime : FALLBACK_WAVE_INTERVAL_TICKS;
        if (durationTicks <= waveInterval)
            return;

        ACTIVE_EMISSIONS.add(new SustainedEmission(particleType, x, y, z, spread, drift, scale,
            burstSize, durationTicks, waveInterval, lifetimeScale));
    }

    /**
     * Fires a vehicle smoke-launcher shell. It trails smoke as it flies and bursts into a cloud
     * once its fuse runs out, all simulated here from the single launch the server sends.
     */
    public static void launchSmokeShell(double x, double y, double z, double vx, double vy, double vz, int fuseTicks)
    {
        if (SMOKE_SHELLS.size() < MAX_SMOKE_SHELLS)
            SMOKE_SHELLS.add(new SmokeShell(x, y, z, vx, vy, vz, fuseTicks));
    }

    /** Advances every sustained emission, smoke shell and staged explosion. Driven from the client tick. */
    public static void tick()
    {
        ExplosionSpectacle.tick();
        if (ACTIVE_EMISSIONS.isEmpty() && SMOKE_SHELLS.isEmpty())
            return;

        if (Minecraft.getInstance().level == null)
        {
            ACTIVE_EMISSIONS.clear();
            SMOKE_SHELLS.clear();
            return;
        }

        ACTIVE_EMISSIONS.removeIf(SustainedEmission::tick);
        SMOKE_SHELLS.removeIf(SmokeShell::tick);
    }

    /** @return the longest lifetime any particle of this wave got, or {@link #NO_PARTICLE} if none spawned */
    private static int emitWave(String particleType, double x, double y, double z,
                                double spread, double drift, float scale, int count, float lifetimeScale)
    {
        RandomSource random = Minecraft.getInstance().level == null
            ? RandomSource.create() : Minecraft.getInstance().level.random;

        int longestLifetime = NO_PARTICLE;
        for (int i = 0; i < count; i++)
        {
            double ox = x + random.nextGaussian() * spread;
            double oy = y + random.nextGaussian() * spread * 0.6D;
            double oz = z + random.nextGaussian() * spread;

            double vx = random.nextGaussian() * drift;
            double vy = Math.abs(random.nextGaussian()) * drift;
            double vz = random.nextGaussian() * drift;

            longestLifetime = Math.max(longestLifetime,
                spawnMeasured(particleType, ox, oy, oz, vx, vy, vz, scale, lifetimeScale));
        }
        return longestLifetime;
    }

    /** One in-flight sustained emission. */
    /** The flight the server used to simulate for each shell: gravity, drag, a puff every third tick. */
    private static final class SmokeShell
    {
        private static final double GRAVITY = 0.04D;
        private static final double DRAG = 0.99D;
        private static final int TRAIL_INTERVAL_TICKS = 3;

        private double x;
        private double y;
        private double z;
        private double vx;
        private double vy;
        private double vz;
        private int ticks;

        private SmokeShell(double x, double y, double z, double vx, double vy, double vz, int ticks)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.ticks = ticks;
        }

        /** @return true once the shell has burst and should be dropped */
        private boolean tick()
        {
            x += vx;
            y += vy;
            z += vz;
            vx *= DRAG;
            vy = (vy - GRAVITY) * DRAG;
            vz *= DRAG;
            if (--ticks > 0)
            {
                if (ticks % TRAIL_INTERVAL_TICKS == 0)
                    spawnFromString(FlanParticles.FM_SMOKE, x, y, z, 0D, 0D, 0D, 1F);
                return false;
            }
            spawnFromString(FlanParticles.FM_SMOKE_BURST, x, y, z, 0D, 0D, 0D, 1F);
            spawnFromString(FlanParticles.FM_BIG_SMOKE, x, y, z, 0D, 0D, 0D, 1F);
            return true;
        }
    }

    private static final class SustainedEmission
    {
        private final String particleType;
        private final double x;
        private final double y;
        private final double z;
        private final double spread;
        private final double drift;
        private final float scale;
        private final int waveSize;
        private final int durationTicks;
        private final int waveIntervalTicks;
        private final float lifetimeScale;
        private int age;

        private SustainedEmission(String particleType, double x, double y, double z,
                                  double spread, double drift, float scale, int waveSize,
                                  int durationTicks, int waveIntervalTicks, float lifetimeScale)
        {
            this.particleType = particleType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.spread = spread;
            this.drift = drift;
            this.scale = scale;
            this.waveSize = waveSize;
            this.durationTicks = durationTicks;
            this.waveIntervalTicks = waveIntervalTicks;
            this.lifetimeScale = lifetimeScale;
        }

        /** @return true once this emission is finished and should be dropped */
        private boolean tick()
        {
            age++;
            if (age % waveIntervalTicks == 0)
                emitWave(particleType, x, y, z, spread, drift, scale, waveSize, lifetimeScale);
            return age >= durationTicks;
        }
    }

    public static void spawnFromString(String s, BlockState state, BlockPos sourcePos,
                                       double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        if (!shouldSpawn(x, y, z))
            return;

        String normalized = normalize(s);
        if (normalized == null)
        {
            warnCouldNotParse(s);
            return;
        }

        Optional<LegacyResourceRequest> request = LegacyResourceRequest.parse(normalized, true);
        if (request.isEmpty() || request.get().kind() == LegacyResourceKind.ICON_CRACK)
        {
            warnCouldNotParse(s);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;

        LegacyBlockParticle.Variant variant = request.get().kind() == LegacyResourceKind.BLOCK_DUST ? LegacyBlockParticle.Variant.DUST : LegacyBlockParticle.Variant.CRACK;
        addParticle(LegacyBlockParticle.create(minecraft.level, state, sourcePos, variant, x, y, z, vx, vy, vz), scale, 1.0F);
    }

    private static boolean spawnLegacyResourceParticle(LegacyResourceRequest request, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale, float lifetimeScale)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return true;

        ClientLevel level = minecraft.level;
        switch (request.kind())
        {
            case ICON_CRACK:
            {
                Optional<ItemStack> stack = getLegacyItemStack(request.resourceId());
                if (stack.isEmpty())
                    return false;
                addParticle(new LegacyItemParticle(level, stack.get(), x, y, z, vx, vy, vz), scale, lifetimeScale);
                return true;
            }
            case BLOCK_CRACK, BLOCK_DUST:
            {
                Optional<BlockState> state = ModUtils.getBlockState(request.resourceId());
                if (state.isEmpty())
                    return false;
                LegacyBlockParticle.Variant variant = request.kind() == LegacyResourceKind.BLOCK_DUST ? LegacyBlockParticle.Variant.DUST : LegacyBlockParticle.Variant.CRACK;
                addParticle(LegacyBlockParticle.create(level, state.get(), sourcePos, variant, x, y, z, vx, vy, vz), scale, lifetimeScale);
                return true;
            }
        }
        return false;
    }

    private static void addParticle(Particle particle, float scale, float lifetimeScale)
    {
        if (particle == null)
            return;
        scaleParticle(particle, scale);
        scaleLifetime(particle, lifetimeScale);
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static void scaleParticle(Particle particle, float scale)
    {
        if (particle != null && scale != 1.0F)
            particle.scale(scale);
    }

    /**
     * Shortens or extends how long a particle lives without touching how fast it animates. A sprite
     * told to live ten times longer by stretching its own animation just crawls; cutting its
     * lifetime short instead simply ends it early, which is exactly how a brief detonation should
     * clear. The floor of one tick keeps a heavily shortened particle visible for a frame rather
     * than having it vanish before it is ever drawn.
     *
     * @return the lifetime the particle ended up with, in ticks
     */
    private static int scaleLifetime(Particle particle, float lifetimeScale)
    {
        if (Float.isFinite(lifetimeScale) && lifetimeScale > 0F && lifetimeScale != 1.0F)
            particle.setLifetime(Math.max(1, Math.round(particle.getLifetime() * lifetimeScale)));
        return particle.getLifetime();
    }

    /**
     * How far an ordinary Flan's Mod particle is drawn at a {@code particleDistanceScale} of one.
     * Larger effects declare their own, longer natural distance; the config scales every one of
     * them alike rather than capping them all at one number.
     */
    public static final double DEFAULT_PARTICLE_RANGE = 128.0D;

    private static boolean shouldSpawn(double x, double y, double z)
    {
        return shouldSpawn(x, y, z, DEFAULT_PARTICLE_RANGE, true);
    }

    /**
     * As {@link #shouldSpawn}, for a particle large enough to be a landmark: an explosion's rising
     * column or mushroom cap is meant to be seen from across the map, which is exactly the range at
     * which ordinary particles are dropped. Such a particle has {@code landmarkRange} as its natural
     * distance, never less than an ordinary particle's, and is not thinned with distance - a handful
     * of puffs tens of blocks wide is not the swarm of distant specks that thinning exists to remove,
     * and thinning it leaves a column with holes in it. The per-tick budget still applies.
     */
    static boolean shouldSpawnLandmark(double x, double y, double z, double landmarkRange)
    {
        return shouldSpawn(x, y, z, Math.max(DEFAULT_PARTICLE_RANGE, landmarkRange), false);
    }

    /**
     * Whether a particle at this position should be created.
     *
     * @param naturalRange      how far this kind of effect is drawn at a distance scale of one
     * @param thinWithDistance  whether the effect loses density toward the edge of its range
     */
    private static boolean shouldSpawn(double x, double y, double z, double naturalRange, boolean thinWithDistance)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null)
            return false;

        ModClientConfig config = ModClientConfig.get();
        double scale = config == null ? 1.0D : config.particleDistanceScale;
        double fullDensityShare = config == null ? 0.25D : config.fullParticleDensityShare;
        double distantDensity = config == null ? 0.25D : config.distantParticleDensity;

        double range = naturalRange * scale;
        double distanceSquared = distanceSquaredToCamera(minecraft, x, y, z);
        if (distanceSquared > range * range)
            return false;

        double fullDensityDistance = range * fullDensityShare;
        if (thinWithDistance && distanceSquared > fullDensityDistance * fullDensityDistance && range > fullDensityDistance)
        {
            double distance = Math.sqrt(distanceSquared);
            double progress = (distance - fullDensityDistance) / (range - fullDensityDistance);
            double density = 1D + (distantDensity - 1D) * progress;
            if (level.random.nextDouble() > density)
                return false;
        }

        return takeFromBudget(level, config);
    }

    private static double distanceSquaredToCamera(Minecraft minecraft, double x, double y, double z)
    {
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double dx = x - camera.x;
        double dy = y - camera.y;
        double dz = z - camera.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean takeFromBudget(ClientLevel level, @Nullable ModClientConfig config)
    {
        int tickBudget = config == null ? 512 : config.maxFlansParticlesPerTick;
        long gameTime = level.getGameTime();
        if (particleBudgetTick != gameTime)
        {
            particleBudgetTick = gameTime;
            particlesCreatedThisTick = 0;
        }
        if (particlesCreatedThisTick >= tickBudget)
            return false;

        particlesCreatedThisTick++;
        return true;
    }

    private static Optional<ItemStack> getLegacyItemStack(String resourceId)
    {
        Optional<ItemStack> exact = ModUtils.getItemStack(resourceId);
        if (exact.isPresent())
            return exact;

        // Legacy syntax optionally appended a numeric damage/metadata value.
        // Trying the complete ID first keeps modern IDs containing underscores
        // unambiguous (for example minecraft:iron_sword).
        int separator = resourceId.lastIndexOf('_');
        if (separator <= 0 || separator == resourceId.length() - 1)
            return Optional.empty();

        try
        {
            int damage = Integer.parseInt(resourceId.substring(separator + 1));
            return ModUtils.getItemStack(resourceId.substring(0, separator)).map(stack ->
            {
                stack.setDamageValue(damage);
                return stack;
            });
        }
        catch (NumberFormatException ignored)
        {
            return Optional.empty();
        }
    }

    /**
     * Accepts both the full content pack name and its short form, so "flare" spawns
     * the same particle as "flansmod.flare".
     */
    @Nullable
    private static String normalize(String raw)
    {
        return FlanParticles.resolve(raw).orElse(null);
    }

    private static void warnCouldNotParse(String s)
    {
        FlansMod.log.warn("Could not parse particle options from string: '{}'", s);
    }

    private enum LegacyResourceKind
    {
        ICON_CRACK(FlanParticles.ICON_CRACK),
        BLOCK_CRACK(FlanParticles.BLOCK_CRACK),
        BLOCK_DUST(FlanParticles.BLOCK_DUST);

        private final String commandName;

        LegacyResourceKind(String commandName)
        {
            this.commandName = commandName;
        }
    }

    private record LegacyResourceRequest(LegacyResourceKind kind, String resourceId)
    {
        private static Optional<LegacyResourceRequest> parse(String s, boolean allowBareName)
        {
            for (LegacyResourceKind kind : LegacyResourceKind.values())
            {
                if (allowBareName && s.equals(kind.commandName))
                    return Optional.of(new LegacyResourceRequest(kind, ""));

                String prefix = kind.commandName + "_";
                if (s.startsWith(prefix) && s.length() > prefix.length())
                    return Optional.of(new LegacyResourceRequest(kind, s.substring(prefix.length())));
            }
            return Optional.empty();
        }
    }

    private static Optional<ParticleOptions> toNamedOptions(String s)
    {
        return switch (s)
        {
            case FlanParticles.FM_AFTERBURN -> Optional.of(FlansMod.afterburnParticle.get());
            case FlanParticles.FM_BIG_SMOKE -> Optional.of(FlansMod.bigSmokeParticle.get());
            case FlanParticles.FM_DEBRIS_1 -> Optional.of(FlansMod.debris1Particle.get());
            case FlanParticles.FM_FLARE -> Optional.of(FlansMod.flareParticle.get());
            case FlanParticles.FM_FLASH -> Optional.of(FlansMod.flashParticle.get());
            case FlanParticles.FM_FLAME -> Optional.of(FlansMod.fmFlameParticle.get());
            case FlanParticles.FM_TRACER -> Optional.of(FlansMod.fmTracerParticle.get());
            case FlanParticles.FM_TRACER_GREEN -> Optional.of(FlansMod.fmTracerGreenParticle.get());
            case FlanParticles.FM_TRACER_RED -> Optional.of(FlansMod.fmTracerRedParticle.get());
            case FlanParticles.FM_MUZZLE_FLASH -> Optional.of(FlansMod.fmMuzzleFlashParticle.get());
            case FlanParticles.FM_ROCKET_EXHAUST -> Optional.of(FlansMod.rocketExhaustParticle.get());
            case FlanParticles.FM_SMOKE -> Optional.of(FlansMod.fmSmokeParticle.get());
            case FlanParticles.FM_SMOKE_BURST -> Optional.of(FlansMod.smokeBurstParticle.get());
            case FlanParticles.FM_SMOKER, FlanParticles.FM_SMOKER_1 -> Optional.of(FlansMod.smokeGrenadeParticle.get());
            case FlanParticles.EXPLODE -> Optional.of(FlansMod.explodeParticle.get());
            case FlanParticles.RED_DUST -> Optional.of(new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1.0F));
            case FlanParticles.HUGE_EXPLOSION -> Optional.of(ParticleTypes.EXPLOSION_EMITTER);
            case FlanParticles.LARGE_EXPLODE -> Optional.of(ParticleTypes.EXPLOSION);
            case FlanParticles.FIREWORKS_SPARK -> Optional.of(ParticleTypes.FIREWORK);
            case FlanParticles.BUBBLE -> Optional.of(ParticleTypes.BUBBLE);
            case FlanParticles.SPLASH -> Optional.of(ParticleTypes.SPLASH);
            case FlanParticles.WAKE -> Optional.of(ParticleTypes.FISHING);
            case FlanParticles.DROP ->  Optional.of(ParticleTypes.FALLING_WATER); // or RAIN?
            case FlanParticles.DRIP_WATER -> Optional.of(ParticleTypes.DRIPPING_WATER);
            case FlanParticles.SUSPENDED -> Optional.of(ParticleTypes.UNDERWATER);
            case FlanParticles.DEPTH_SUSPEND -> Optional.of(ParticleTypes.UNDERWATER); // actually removed, no true equivalent
            case FlanParticles.TOWN_AURA -> Optional.of(ParticleTypes.ASH); // or MYCELIUM?
            case FlanParticles.CRIT -> Optional.of(ParticleTypes.CRIT);
            case FlanParticles.MAGIC_CRIT -> Optional.of(ParticleTypes.ENCHANTED_HIT);
            case FlanParticles.SMOKE -> Optional.of(ParticleTypes.SMOKE);
            case FlanParticles.LARGE_SMOKE -> Optional.of(ParticleTypes.LARGE_SMOKE);
            case FlanParticles.SPELL -> Optional.of(ParticleTypes.EFFECT);
            case FlanParticles.INSTANT_SPELL -> Optional.of(ParticleTypes.INSTANT_EFFECT);
            case FlanParticles.WITCH_MAGIC -> Optional.of(ParticleTypes.WITCH);
            case FlanParticles.DRIP_LAVA -> Optional.of(ParticleTypes.DRIPPING_LAVA);
            case FlanParticles.ANGRY_VILLAGER -> Optional.of(ParticleTypes.ANGRY_VILLAGER);
            case FlanParticles.HAPPY_VILLAGER -> Optional.of(ParticleTypes.HAPPY_VILLAGER);
            case FlanParticles.NOTE -> Optional.of(ParticleTypes.NOTE);
            case FlanParticles.PORTAL -> Optional.of(ParticleTypes.PORTAL);
            case FlanParticles.ENCHANTMENT_TABLE -> Optional.of(ParticleTypes.ENCHANT);
            case FlanParticles.FLAME -> Optional.of(ParticleTypes.FLAME);
            case FlanParticles.LAVA -> Optional.of(ParticleTypes.LAVA);
            case FlanParticles.CLOUD -> Optional.of(ParticleTypes.CLOUD);
            case FlanParticles.SNOWBALL_POOF -> Optional.of(ParticleTypes.ITEM_SNOWBALL);
            case FlanParticles.SNOW_SHOVEL -> Optional.of(ParticleTypes.POOF);
            case FlanParticles.SLIME -> Optional.of(ParticleTypes.ITEM_SLIME);
            case FlanParticles.HEART -> Optional.of(ParticleTypes.HEART);
            case FlanParticles.BARRIER -> Optional.of(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()));
            case FlanParticles.DROPLET -> Optional.of(ParticleTypes.FALLING_WATER);
            case FlanParticles.MOB_APPEARANCE -> Optional.of(ParticleTypes.ELDER_GUARDIAN);
            case FlanParticles.DRAGON_BREATH -> Optional.of(ParticleTypes.DRAGON_BREATH);
            case FlanParticles.END_ROD -> Optional.of(ParticleTypes.END_ROD);
            case FlanParticles.DAMAGE_INDICATOR -> Optional.of(ParticleTypes.DAMAGE_INDICATOR);
            case FlanParticles.SWEEP_ATTACK -> Optional.of(ParticleTypes.SWEEP_ATTACK);
            case FlanParticles.FALLING_DUST -> Optional.of(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()));
            case FlanParticles.SPIT -> Optional.of(ParticleTypes.SPIT);
            case FlanParticles.TOTEM -> Optional.of(ParticleTypes.TOTEM_OF_UNDYING);
            default -> toRegisteredParticleOptions(s);
        };
    }

    private static Optional<ParticleOptions> toRegisteredParticleOptions(String s)
    {
        // Registered particle IDs
        if (s.contains(":"))
        {
            return Optional.ofNullable(ResourceLocation.tryParse(s))
                .map(BuiltInRegistries.PARTICLE_TYPE::get)
                .filter(ParticleOptions.class::isInstance)
                .map(ParticleOptions.class::cast);
        }

        return Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", s))
            .map(BuiltInRegistries.PARTICLE_TYPE::get)
            .filter(ParticleOptions.class::isInstance)
            .map(ParticleOptions.class::cast);
    }
}
