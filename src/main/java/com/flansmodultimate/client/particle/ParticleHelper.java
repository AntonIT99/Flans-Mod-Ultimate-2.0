package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
    /** Ticks between waves of a sustained emission. */
    private static final int WAVE_INTERVAL_TICKS = 5;
    /**
     * Roughly the average life of a vanilla explosion puff, which picks its own lifetime in the
     * 18-82 tick range. Wave sizes are derived from it so a sustained emission holds a steady
     * number of puffs on screen instead of piling them up.
     */
    private static final float AVERAGE_PUFF_LIFETIME_TICKS = 34F;
    /** Simultaneous sustained emissions, so a barrage cannot stack unbounded emitters. */
    private static final int MAX_ACTIVE_EMITTERS = 16;

    private static final Map<String, Optional<ParticleOptions>> PARTICLE_OPTIONS_CACHE = new ConcurrentHashMap<>();
    private static final List<SustainedEmission> ACTIVE_EMISSIONS = new ArrayList<>();
    private static long particleBudgetTick = Long.MIN_VALUE;
    private static int particlesCreatedThisTick;

    public static void spawnFromString(String s, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        if (!shouldSpawn(x, y, z))
            return;

        String normalized = normalize(s);
        if (normalized == null)
        {
            warnCouldNotParse(s);
            return;
        }

        Optional<LegacyResourceRequest> legacyRequest = LegacyResourceRequest.parse(normalized, false);
        if (legacyRequest.isPresent())
        {
            if (!spawnLegacyResourceParticle(legacyRequest.get(), BlockPos.containing(x, y, z), x, y, z, vx, vy, vz, scale))
                warnCouldNotParse(s);
            return;
        }

        Optional<ParticleOptions> opt = PARTICLE_OPTIONS_CACHE.computeIfAbsent(normalized, ParticleHelper::toNamedOptions);
        if (opt.isEmpty())
        {
            warnCouldNotParse(s);
            return;
        }

        Particle particle = Minecraft.getInstance().particleEngine.createParticle(opt.get(), x, y, z, vx, vy, vz);
        scaleParticle(particle, scale);
    }

    /**
     * Emits {@code burstSize} particles now and then keeps topping them up for {@code durationTicks},
     * so the effect stays on screen for the whole duration while each individual particle still runs
     * its own animation at its own natural speed. Stretching one particle's lifetime instead would
     * slow its animation to a crawl; replacing it as it expires keeps the motion looking right.
     */
    public static void spawnSustained(String particleType, double x, double y, double z,
                                      double spread, double drift, float scale, int burstSize, int durationTicks)
    {
        emitWave(particleType, x, y, z, spread, drift, scale, burstSize);

        if (durationTicks <= WAVE_INTERVAL_TICKS || ACTIVE_EMISSIONS.size() >= MAX_ACTIVE_EMITTERS)
            return;

        // Sized so the steady-state count of live particles stays near the opening burst:
        // each wave replaces roughly what expired since the previous one.
        int waveSize = Math.max(1, Math.round(burstSize * WAVE_INTERVAL_TICKS / AVERAGE_PUFF_LIFETIME_TICKS));
        ACTIVE_EMISSIONS.add(new SustainedEmission(particleType, x, y, z, spread, drift, scale, waveSize, durationTicks));
    }

    /** Advances every sustained emission. Driven from the client tick. */
    public static void tick()
    {
        if (ACTIVE_EMISSIONS.isEmpty())
            return;

        if (Minecraft.getInstance().level == null)
        {
            ACTIVE_EMISSIONS.clear();
            return;
        }

        ACTIVE_EMISSIONS.removeIf(SustainedEmission::tick);
    }

    private static void emitWave(String particleType, double x, double y, double z,
                                 double spread, double drift, float scale, int count)
    {
        RandomSource random = Minecraft.getInstance().level == null
            ? RandomSource.create() : Minecraft.getInstance().level.random;

        for (int i = 0; i < count; i++)
        {
            double ox = x + random.nextGaussian() * spread;
            double oy = y + random.nextGaussian() * spread * 0.6D;
            double oz = z + random.nextGaussian() * spread;

            double vx = random.nextGaussian() * drift;
            double vy = Math.abs(random.nextGaussian()) * drift;
            double vz = random.nextGaussian() * drift;

            spawnFromString(particleType, ox, oy, oz, vx, vy, vz, scale);
        }
    }

    /** One in-flight sustained emission. */
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
        private int age;

        private SustainedEmission(String particleType, double x, double y, double z,
                                  double spread, double drift, float scale, int waveSize, int durationTicks)
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
        }

        /** @return true once this emission is finished and should be dropped */
        private boolean tick()
        {
            age++;
            if (age % WAVE_INTERVAL_TICKS == 0)
                emitWave(particleType, x, y, z, spread, drift, scale, waveSize);
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
        addParticle(LegacyBlockParticle.create(minecraft.level, state, sourcePos, variant, x, y, z, vx, vy, vz), scale);
    }

    private static boolean spawnLegacyResourceParticle(LegacyResourceRequest request, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale)
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
                addParticle(new LegacyItemParticle(level, stack.get(), x, y, z, vx, vy, vz), scale);
                return true;
            }
            case BLOCK_CRACK, BLOCK_DUST:
            {
                Optional<BlockState> state = ModUtils.getBlockState(request.resourceId());
                if (state.isEmpty())
                    return false;
                LegacyBlockParticle.Variant variant = request.kind() == LegacyResourceKind.BLOCK_DUST ? LegacyBlockParticle.Variant.DUST : LegacyBlockParticle.Variant.CRACK;
                addParticle(LegacyBlockParticle.create(level, state.get(), sourcePos, variant, x, y, z, vx, vy, vz), scale);
                return true;
            }
        }
        return false;
    }

    private static void addParticle(Particle particle, float scale)
    {
        if (particle == null)
            return;
        scaleParticle(particle, scale);
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static void scaleParticle(Particle particle, float scale)
    {
        if (particle != null && scale != 1.0F)
            particle.scale(scale);
    }

    private static boolean shouldSpawn(double x, double y, double z)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null)
            return false;

        ModClientConfig config = ModClientConfig.get();
        int renderDistance = config == null ? 128 : config.particleRenderDistance;
        int fullDensityDistance = config == null ? 32 : config.fullParticleDensityDistance;
        double distantDensity = config == null ? 0.25D : config.distantParticleDensity;
        int tickBudget = config == null ? 512 : config.maxFlansParticlesPerTick;

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double dx = x - camera.x;
        double dy = y - camera.y;
        double dz = z - camera.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double renderDistanceSquared = (double)renderDistance * renderDistance;
        if (distanceSquared > renderDistanceSquared)
            return false;

        if (distanceSquared > (double)fullDensityDistance * fullDensityDistance && renderDistance > fullDensityDistance)
        {
            double distance = Math.sqrt(distanceSquared);
            double progress = (distance - fullDensityDistance) / (renderDistance - fullDensityDistance);
            double density = 1D + (distantDensity - 1D) * progress;
            if (level.random.nextDouble() > density)
                return false;
        }

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
            case FlanParticles.MOB_SPELL -> Optional.of(ParticleTypes.ENTITY_EFFECT);
            case FlanParticles.MOB_SPELL_AMBIENT -> Optional.of(ParticleTypes.AMBIENT_ENTITY_EFFECT);
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
                .map(ForgeRegistries.PARTICLE_TYPES::getValue)
                .filter(ParticleOptions.class::isInstance)
                .map(ParticleOptions.class::cast);
        }

        return Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", s))
            .map(ForgeRegistries.PARTICLE_TYPES::getValue)
            .filter(ParticleOptions.class::isInstance)
            .map(ParticleOptions.class::cast);
    }
}
