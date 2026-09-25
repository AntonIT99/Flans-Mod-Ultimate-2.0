package com.flansmodultimate.common.explosions;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.ExplosionVehicleDamageResolver;
import com.flansmodultimate.common.driveables.armor.VehicleExplosionTarget;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Wheel;
import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketFlanExplosionBlockParticles;
import com.flansmodultimate.network.client.PacketFlanExplosionParticles;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.platform.PlatformEvents;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.flansmodultimate.util.ModUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlanExplosion extends Explosion
{
    protected static final double EXPLOSION_PARTICLE_RANGE = 256;
    protected static final float KNOCKBACK_MULTIPLAYER = 1F;
    /**
     * Largest crater carved in the same tick as the explosion. Anything bigger is handed to
     * {@link CraterCarver} to be carved over the following ticks.
     */
    protected static final float MAX_IMMEDIATE_CRATER_RADIUS = 24F;
    /** Upper bound on per-block burst particles in {@link #finalizeExplosion(boolean)}, independent of blocks destroyed. */
    protected static final int MAX_BLOCK_BURST_PARTICLES = 40;
    
    // Config
    protected final boolean causesFire;
    /** Whether the client draws this as a fire explosion; true for anything that ignites, and for burning vehicles. */
    protected final boolean fieryVisuals;
    protected final boolean breaksBlocks;
    protected final boolean canDamageSelf;

    // Core Context
    protected final Level level;
    protected final Vec3 center;
    protected final int smokeCount;
    protected final int debrisCount;
    protected final Stats stats;
    
    @Nullable
    protected final LivingEntity causingEntity;
    protected final Entity explosive;

    protected final ExplosionDamageCalculator damageCalculator;
    protected final List<BlockPos> affectedBlockPositions;
    protected final Map<Player, Vec3> hitPlayers = Maps.newHashMap();
    /** Set when the crater is too big for one tick; {@link #affectedBlockPositions} then only holds a sample of it. */
    @Nullable
    protected ExplosionCrater deferredCrater;

    /**
     * Stats of the Explosion
     * @param explosionRadius radius of main explosion visuals (particles) and block breaking
     * @param explosionPower power of breaking blocks within explosion radius
     * @param blastRadius radius of overpressure hurting entities (blast)
     * @param fragRadius radius of fragmentation damage
     * @param fragIntensity intensity of fragmentation
     * @param blastDamage max damage dealt to entities within blast radius
     * @param fragDamage max damage dealt to entities within frag radius
     */
    public record Stats(float explosionRadius, float explosionPower, float blastRadius, DamageStats blastDamage,
                        float fragRadius, float fragIntensity, DamageStats fragDamage, float explosiveMassKg)
    {
        public Stats(float explosionRadius, float explosionPower, float blastRadius, DamageStats blastDamage,
                     float fragRadius, float fragIntensity, DamageStats fragDamage)
        {
            this(explosionRadius, explosionPower, blastRadius, blastDamage,
                fragRadius, fragIntensity, fragDamage, 0F);
        }

        public Stats
        {
            if (!Float.isFinite(explosiveMassKg) || explosiveMassKg < 0F)
                explosiveMassKg = 0F;

            // Every radius is capped here, the one place all explosion statistics pass
            // through, so an extreme charge cannot ask the server to iterate a radius of
            // tens of thousands of blocks. The caps are a performance ceiling, not a
            // balance decision: the authored explosive mass stays honest, and a server
            // with the hardware for a larger detonation simply raises the config value.
            //
            // The two ceilings are deliberately separate because the work they bound is
            // not comparable. The crater radius drives the block-breaking ray march, which
            // is the expensive loop, so it gets the tighter maxExplosionRadius. The blast
            // and fragmentation radii only size an entity query, so they get the far more
            // generous maxBlastRadius and keep their full reach on conventional charges.
            float maxCraterRadius = (float) ModCommonConfig.maxExplosionRadius();
            if (Float.isFinite(maxCraterRadius) && maxCraterRadius > 0F)
                explosionRadius = Math.min(explosionRadius, maxCraterRadius);

            // How these radii grow with the charge, including the flattening that keeps a heavy
            // charge's damage radius plausible on Minecraft's scale, is ExplosionScaling's job:
            // it is applied where the radii are derived, so an authored legacy radius is taken
            // literally and only the caps below apply to it.
            float maxDamageRadius = (float) ModCommonConfig.maxBlastRadius();
            if (Float.isFinite(maxDamageRadius) && maxDamageRadius > 0F)
            {
                blastRadius = Math.min(blastRadius, maxDamageRadius);
                fragRadius = Math.min(fragRadius, maxDamageRadius);
            }
            // Ensure blastRadius >= explosionRadius
            if (blastRadius < explosionRadius)
                blastRadius = explosionRadius;
        }
    }

    public FlanExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, ShootableType type, double x, double y, double z, boolean canDamageSelf)
    {
        this(level, explosive, causingEntity, type, x, y, z, type.getExplosionStats(explosive), canDamageSelf);
    }

    private FlanExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity,
                          ShootableType type, double x, double y, double z, Stats stats, boolean canDamageSelf)
    {
        this(level, explosive, causingEntity, x, y, z, stats, type.getFireRadius() > 0, type.getFireRadius() > 0,
            shouldBreakBlocks(type, stats),
            type.getSmokeParticleCount(), type.getDebrisParticleCount(), canDamageSelf);
    }

    private static boolean shouldBreakBlocks(ShootableType type, Stats stats)
    {
        boolean globallyAllowed = FlansMod.teamsManager.isExplosionsBreakBlocks()
            && ModCommonConfig.get().explosionsBreakBlocks();
        boolean forcedNewExplosion = ModCommonConfig.get().forceNewExplosionsBreakBlocks()
            && stats.explosiveMassKg() > 0F;
        return globallyAllowed && (type.isExplosionBreaksBlocks() || forcedNewExplosion);
    }

    public FlanExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, double x, double y, double z, Stats stats, boolean causesFire, boolean fieryVisuals, boolean breaksBlocks, int smokeCount, int debrisCount, boolean canDamageSelf)
    {
        super(level, explosive, x, y, z, stats.explosionRadius, causesFire, breaksBlocks ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP);

        this.level = level;
        this.explosive = explosive;
        this.causingEntity = causingEntity;

        center = new Vec3(x, y, z);
        this.stats = stats;

        this.causesFire = causesFire;
        this.fieryVisuals = fieryVisuals || causesFire;
        this.breaksBlocks = breaksBlocks;
        this.smokeCount = smokeCount;
        this.debrisCount = debrisCount;
        this.canDamageSelf = canDamageSelf;

        affectedBlockPositions = Lists.newArrayList();
        damageCalculator = (explosive == null) ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(explosive);

        if (!PlatformEvents.onExplosionStart(level, this))
        {
            explode();
            finalizeExplosion(true);
        }
    }

    /**
     * Does the first part of the explosion (destroy blocks)
     */
    @Override
    public void explode()
    {
        doBreakBlocks();
        doHurtEntities();
    }

    /**
     * Does the second part of the explosion (sound, particles, drop spawn)
     */
    @Override
    public void finalizeExplosion(boolean spawnParticles)
    {
        if (level.isClientSide)
            return;

        ServerLevel sl = (ServerLevel) level;

        // Game event
        level.gameEvent(GameEvent.EXPLODE, BlockPos.containing(center),
            GameEvent.Context.of(explosive != null ? explosive : causingEntity));

        // Sound broadcast (server-side playSound with null player broadcasts)
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, ModCommonConfig.get().explosionSoundRange() / 16F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        // The vanilla emitter is a fixed size whatever the charge, so it only helps where the
        // explosion is at least as big as the puffs it scatters. Below that it was the single
        // loudest thing on screen for a round that carries a few grams of filler, and it drowned
        // out the scaled flash and fireball that PacketFlanExplosionParticles now sends instead.
        // A fire explosion skips it too: its fireball is fire throughout, and grey puffs would undo that.
        if (spawnParticles && stats.explosionRadius >= 2.0F && !fieryVisuals)
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0.0);

        if (interactsWithBlocks())
        {
            for (BlockPos pos : getToBlow())
                blowUpBlock(pos);
            for (BlockPos pos : getToBlow())
                maybeIgnite(pos);

            // An explosion event handler that emptied the block list vetoed the whole crater.
            if (deferredCrater != null && !getToBlow().isEmpty())
                CraterCarver.start(this, deferredCrater, deferredCrater.chunksRefusedBy(getToBlow()));
        }

        if (spawnParticles)
        {
            PacketHandler.sendToAllAround(new PacketFlanExplosionBlockParticles(center, stats.explosionRadius, sampleBlockBurstPositions(affectedBlockPositions)), center, Math.max(EXPLOSION_PARTICLE_RANGE, stats.explosionRadius), level.dimension());
            PacketHandler.sendToAllAround(new PacketFlanExplosionParticles(center, smokeCount, debrisCount,
                stats.blastRadius, stats.explosionRadius, stats.fragRadius, stats.fragIntensity, fieryVisuals),
                center, Math.max(EXPLOSION_PARTICLE_RANGE, stats.blastRadius), level.dimension());
        }
    }

    /**
     * A huge explosion can destroy thousands of blocks; sending one particle burst per block
     * would flood the network and the client's particle engine and buys nothing visually once
     * the craters are already packed with debris. Bigger explosions instead get a bounded
     * number of bursts scaled up in size client-side (see {@link PacketFlanExplosionBlockParticles}),
     * which reads as "one bigger blast" rather than "the same tiny burst, just more of them".
     */
    protected List<BlockPos> sampleBlockBurstPositions(List<BlockPos> positions)
    {
        if (positions.size() <= MAX_BLOCK_BURST_PARTICLES)
            return positions;

        List<BlockPos> sampled = new ArrayList<>(MAX_BLOCK_BURST_PARTICLES);
        float stride = positions.size() / (float) MAX_BLOCK_BURST_PARTICLES;
        for (int i = 0; i < MAX_BLOCK_BURST_PARTICLES; i++)
            sampled.add(positions.get(Mth.floor(i * stride)));
        return sampled;
    }

    @Override
    @NotNull
    public Map<Player, Vec3> getHitPlayers()
    {
        return hitPlayers;
    }

    @Override
    public void clearToBlow()
    {
        affectedBlockPositions.clear();
    }

    @Override
    @NotNull
    public List<BlockPos> getToBlow()
    {
        if (!breaksBlocks)
            return Collections.emptyList();
        return affectedBlockPositions;
    }

    protected void blowUpBlock(BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        if (state.isAir())
            return;

        if (ModCommonConfig.get().flanExplosionsDropBlocks() && state.canDropFromExplosion(level, pos, this))
        {
            BlockEntity be = level.getBlockEntity(pos);
            Entity attacker = getIndirectSourceEntity();
            Block.dropResources(state, level, pos, be, attacker, ItemStack.EMPTY);
        }

        state.onBlockExploded(level, pos, this);
    }

    protected void maybeIgnite(BlockPos pos)
    {
        if (causesFire
            && level.isEmptyBlock(pos)
            && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
            && level.random.nextInt(3) == 0)
        {
            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }
    }

    /**
     * Works out the crater; see {@link ExplosionCrater}. A crater small enough to carve at once
     * lists every block it breaks, as any explosion does. A bigger one lists only the first block
     * each ray hit, which still gives event handlers and the debris particles a fair picture of
     * it, and the rest is carved over the next ticks once the explosion is finalized.
     */
    protected void doBreakBlocks()
    {
        affectedBlockPositions.clear();
        deferredCrater = null;
        if (!breaksBlocks || !(level instanceof ServerLevel serverLevel) || stats.explosionRadius <= 0F)
            return;

        ExplosionCrater crater = ExplosionCrater.trace(serverLevel, this, damageCalculator, center, stats.explosionRadius, stats.explosionPower);
        if (stats.explosionRadius <= MAX_IMMEDIATE_CRATER_RADIUS)
        {
            for (SectionPos section : crater.sectionsByDistance())
                crater.collectSection(section, affectedBlockPositions::add);
        }
        else
        {
            affectedBlockPositions.addAll(crater.firstHits());
            deferredCrater = crater;
        }
    }

    protected void doHurtEntities()
    {
        hitPlayers.clear();

        List<Entity> entities = ModUtils.queryEntities(level, canDamageSelf ? null : explosive, getHurtEntitiesAabb(), e -> !EntityPlatform.ignoresExplosion(e, this));
        PlatformEvents.onExplosionDetonate(level, this, entities, stats.explosionRadius * 2F);

        Set<Driveable> handledDriveables = new HashSet<>();
        for (Entity e : entities)
        {
            Driveable driveable = driveableOf(e);
            if (driveable != null)
            {
                if (handledDriveables.add(driveable))
                    applyVehicleDamage(driveable);
                continue;
            }
            double distance = e.getEyePosition().distanceTo(center);

            // occlusion
            double seen = Explosion.getSeenPercent(center, e);
            // blast falloff
            double blastFalloff = getBlastFalloff(distance, stats.blastRadius());
            // blast damage
            double blastDamage = distance <= stats.blastRadius() ? getBlastDamage(e, seen, blastFalloff) : 0.0;
            // frag damage
            double fragDamage = distance <= stats.fragRadius() ? getFragDamage(e, seen, distance, stats.fragRadius(), stats.fragIntensity()) : 0.0;
            // final damage
            float explosionDamage = (float) (blastDamage + fragDamage);

            applyDamage(e, explosionDamage);
            applyKnockback(e, seen, blastFalloff);
        }
    }

    private void applyVehicleDamage(Driveable driveable)
    {
        if (driveable == null || driveable.getConfigType() == null)
            return;
        VehicleExplosionTarget target = driveable.resolveExplosionTarget(center).orElse(null);
        if (target == null)
            return;

        double distance = driveable.getEyePosition().distanceTo(center);
        double seen = Explosion.getSeenPercent(center, driveable);
        double blastFalloff = getBlastFalloff(distance, stats.blastRadius());
        float existingBlast = distance <= stats.blastRadius()
            ? (float) getBlastDamage(driveable, seen, blastFalloff) : 0F;
        float existingFragmentation = distance <= stats.fragRadius()
            ? (float) getFragDamage(driveable, seen, distance, stats.fragRadius(), stats.fragIntensity()) : 0F;

        ArmorPlate plate = driveable.getConfigType().getResolvedArmor()
            .plate(target.part(), target.facing()).authored();
        // A legacy explosive declares no explosive mass, so the pressure model has no charge to
        // work from and the vehicle would take nothing at all. Recover an equivalent charge from
        // the legacy crater radius and power so those definitions still threaten armour.
        float charge = stats.explosiveMassKg() > 0F ? stats.explosiveMassKg()
            : ExplosionVehicleDamageResolver.legacyTntEquivalentKg(
                stats.explosionRadius(), stats.explosionPower(),
                ModCommonConfig.get().newDamageSystemExplosiveRadiusReference());
        Float explosiveMass = charge > 0F ? charge : null;
        ExplosionVehicleDamageResolver.DamageChannels channels = ExplosionVehicleDamageResolver.resolve(
            plate.thicknessMm(), explosiveMass, target.distanceMeters(), existingBlast, existingFragmentation,
            ModCommonConfig.armoredBlastResistanceKPaPerMm(), ModCommonConfig.minimumBlastDistanceMeters());

        DamageSource source = FlanDamageSources.createDamageSource(
            level, explosive, causingEntity, FlanDamageSources.EXPLOSION);
        boolean hurt = driveable.damagePart(target.part(), channels.totalDamage(), source);
        if (hurt && causingEntity instanceof ServerPlayer player)
            PacketHandler.sendTo(new PacketHitMarker(false, 1.0F, true), player);
        applyKnockback(driveable, seen, blastFalloff);
    }

    @Nullable
    private static Driveable driveableOf(Entity entity)
    {
        if (entity instanceof Driveable driveable)
            return driveable;
        if (entity instanceof Wheel wheel)
            return wheel.getDriveable();
        if (entity instanceof Seat seat)
            return seat.getDriveable();
        return null;
    }

    protected AABB getHurtEntitiesAabb()
    {
        float queryRadius = Math.max(stats.blastRadius, stats.fragRadius) + 2.0F;

        int minX = Mth.floor(center.x - queryRadius - 1.0D);
        int maxX = Mth.floor(center.x + queryRadius + 1.0D);
        int minY = Mth.floor(center.y - queryRadius - 1.0D);
        int maxY = Mth.floor(center.y + queryRadius + 1.0D);
        int minZ = Mth.floor(center.z - queryRadius - 1.0D);
        int maxZ = Mth.floor(center.z + queryRadius + 1.0D);

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    protected double getBlastDamage(Entity e, double seen, double falloff)
    {
        return getBlastMaxDamage(e) * seen * Math.pow(falloff, 0.5);
    }

    protected static double getBlastFalloff(double distanceToEntity, double radius)
    {
        // normalized distance based on radius
        double normalizedDistance = distanceToEntity / Math.max(0.001, radius);

        // blast-like falloff based on radius
        double falloff = 1.0 / (1.0 + Math.pow(normalizedDistance * ModCommonConfig.get().newDamageSystemBlastFalloffSharpness(), 3.0));

        // soft cutoff so it’s ~0 at the edge of radius
        double edge = 1.0 - normalizedDistance;
        edge = Math.max(edge, 0.0);
        double cutoff = edge * edge; // edge^2 feels good; edge^3 is harsher
        falloff *= cutoff;

        return falloff;
    }

    protected double getBlastMaxDamage(Entity e)
    {
        Entity proxy = e;
        float extra = 1.0F;
        if (e instanceof Wheel wheel)
        {
            proxy = wheel.getDriveable();
            extra *= ModCommonConfig.get().vehicleWheelSeatExplosionModifier();
        }
        else if (e instanceof Seat seat)
        {
            proxy = seat.getDriveable();
            extra *= ModCommonConfig.get().vehicleWheelSeatExplosionModifier();
        }
        return stats.blastDamage.getDamageAgainstEntity(proxy) * extra;
    }

    protected double getFragDamage(Entity e, double seen, double distanceToEntity, double radius, double fragIntensity)
    {
        return getFragMaxDamage(e) * getFragHitChance(seen, distanceToEntity, radius, fragIntensity);
    }

    protected double getFragMaxDamage(Entity e)
    {
        Entity proxy = e;
        if (e instanceof Wheel wheel)
            proxy = wheel.getDriveable();
        else if (e instanceof Seat seat)
            proxy = seat.getDriveable();
        return stats.fragDamage.getDamageAgainstEntity(proxy);
    }

    /**
     * Computes the probability that an entity is hit by at least one meaningful fragment.
     *  fragIntensity = category knob (e.g. 0.8, 2.0, 4.0)
     */
    protected static double getFragHitChance(double seen, double distanceToEntity, double radius, double fragIntensity)
    {
        if (radius <= 0.0 || fragIntensity <= 0.0)
            return 0.0;
        if (distanceToEntity >= radius)
            return 0.0;

        // Normalize distance to [0, 1]
        double x = distanceToEntity / Math.max(0.001, radius);
        x = Math.max(0.0, Math.min(1.0, x));

        // How strongly cover reduces fragments (higher = more cover effectiveness)
        double lambda = getLambda(fragIntensity, seen, x);

        // Numerical safety: if lambda is huge, exp(-lambda) underflows to 0 anyway
        double pHit = 1.0 - Math.exp(-lambda);

        // Clamp to [0, 1]
        return Math.max(0.0, Math.min(1.0, pHit));
    }

    private static double getLambda(double fragIntensity, double seen, double x)
    {
        // Edge taper exponent (higher = sharper drop near the edge)
        final double beta = 1.5; // 1.0–2.5 typically
        // Prevents blow-up near x=0; also controls "point-blank always hits"
        final double eps = 0.02; // 0.01–0.05 typically

        // Occlusion factor: fragments are very sensitive to cover (2.0–4.0 typically)
        double occ = Math.pow(seen, 3.0);

        // Fragment areal density approximation:
        //  - 1/(x^2 + eps) gives the ~1/d^2 thinning
        //  - (1-x)^beta ensures near-edge goes to 0
        double density = Math.pow(1.0 - x, beta) / (x * x + eps);

        // Expected hits (lambda) then Poisson probability of >= 1 hit
        return fragIntensity * density * occ;
    }

    protected void applyDamage(Entity e, float damage)
    {
        if (damage < 0.1F)
            return;

        DamageSource src = FlanDamageSources.createDamageSource(level, explosive, causingEntity, FlanDamageSources.EXPLOSION);
        boolean hurt = e.hurt(src, damage);
        if (hurt && causingEntity instanceof ServerPlayer sp)
            PacketHandler.sendTo(new PacketHitMarker(false, 1.0F, true), sp);
    }

    protected void applyKnockback(Entity e, double seen, double falloff)
    {
        if (seen < 0.001 || falloff < 0.001)
            return;

        // normalized direction (for knockback)
        Vec3 direction = e.getEyePosition().subtract(center).normalize();

        // Knockback: also scaled-distance based
        double kb = falloff * seen * KNOCKBACK_MULTIPLAYER;
        if (e instanceof LivingEntity living)
            kb = EntityPlatform.explosionKnockback(living, kb);

        // Knockback vector
        Vec3 kbVec = direction.scale(kb);

        e.setDeltaMovement(e.getDeltaMovement().add(kbVec));
        e.hurtMarked = true;

        if (e instanceof Player pl && !pl.isSpectator() && !(pl.getAbilities().flying && pl.getAbilities().instabuild))
            hitPlayers.put(pl, kbVec);
    }
}
