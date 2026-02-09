package com.flansmodultimate.common;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Wheel;
import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketFlanExplosionBlockParticles;
import com.flansmodultimate.network.client.PacketFlanExplosionParticles;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.util.ModUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlanExplosion extends Explosion
{
    protected static final double EXPLOSION_PARTICLE_RANGE = 256;
    protected static final float KNOCKBACK_MULTIPLAYER = 1F;
    
    // Config
    protected final boolean causesFire;
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
    public record Stats(float explosionRadius, float explosionPower, float blastRadius, DamageStats blastDamage, float fragRadius, float fragIntensity, DamageStats fragDamage)
    {
        public Stats
        {
            // Ensure blastRadius >= explosionRadius
            if (blastRadius < explosionRadius)
                blastRadius = explosionRadius;
        }
    }

    public FlanExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, ShootableType type, double x, double y, double z, boolean canDamageSelf)
    {
        this(level, explosive, causingEntity, x, y, z, type.getExplosionStats(explosive), type.getFireRadius() > 0,
            type.isExplosionBreaksBlocks() && FlansMod.teamsManager.isExplosionsBreakBlocks(),
            type.getSmokeParticleCount(), type.getDebrisParticleCount(), canDamageSelf);
    }

    public FlanExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, double x, double y, double z, Stats stats, boolean causesFire, boolean breaksBlocks, int smokeCount, int debrisCount, boolean canDamageSelf)
    {
        super(level, explosive, x, y, z, stats.explosionRadius, causesFire, breaksBlocks ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP);

        this.level = level;
        this.explosive = explosive;
        this.causingEntity = causingEntity;

        center = new Vec3(x, y, z);
        this.stats = stats;

        this.causesFire = causesFire;
        this.breaksBlocks = breaksBlocks;
        this.smokeCount = smokeCount;
        this.debrisCount = debrisCount;
        this.canDamageSelf = canDamageSelf;

        affectedBlockPositions = Lists.newArrayList();
        damageCalculator = (explosive == null) ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(explosive);

        if (!ForgeEventFactory.onExplosionStart(level, this))
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

        if (spawnParticles)
        {
            if (stats.explosionRadius >= 2.0F)
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0.0);
            else
                sl.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0.0);
        }

        if (interactsWithBlocks())
        {
            for (BlockPos pos : getToBlow())
            {
                BlockState state = level.getBlockState(pos);
                if (state.isAir())
                    continue;

                if (state.canDropFromExplosion(level, pos, this))
                {
                    BlockEntity be = level.getBlockEntity(pos);
                    Entity attacker = getIndirectSourceEntity();
                    Block.dropResources(state, level, pos, be, attacker, ItemStack.EMPTY);
                }

                state.onBlockExploded(level, pos, this);
            }
        }

        if (causesFire)
        {
            for (BlockPos pos : getToBlow())
            {
                if (level.isEmptyBlock(pos)
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && level.random.nextInt(3) == 0)
                {
                    level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                }
            }
        }

        if (spawnParticles)
        {
            PacketHandler.sendToAllAround(new PacketFlanExplosionBlockParticles(center, stats.explosionRadius, affectedBlockPositions), center, Math.max(EXPLOSION_PARTICLE_RANGE, stats.explosionRadius), level.dimension());
            PacketHandler.sendToAllAround(new PacketFlanExplosionParticles(center, smokeCount, debrisCount, stats.blastRadius), center, Math.max(EXPLOSION_PARTICLE_RANGE, stats.blastRadius), level.dimension());
        }
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

    protected void doBreakBlocks()
    {
        affectedBlockPositions.clear();

        // Prevent extreme CPU load when radius gets big
        int samples = Mth.clamp(Mth.ceil(stats.explosionRadius * 2.0F), 16, 48);

        Set<BlockPos> toBlow = new HashSet<>();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        // Step size: smaller = more accurate, slower.
        float step = 0.3F;

        // A "ray energy" budget. Since you set power ∝ cbrt(W) and radius ∝ cbrt(W),
        // power * radius ∝ W^(2/3), which is already strongly scaling.
        float rayStartBudget = stats.explosionPower * (0.7F + level.random.nextFloat() * 0.6F);

        for (int j = 0; j < samples; ++j)
        {
            for (int k = 0; k < samples; ++k)
            {
                for (int l = 0; l < samples; ++l)
                {
                    // only rays from the cube shell
                    if (j != 0 && j != samples - 1 && k != 0 && k != samples - 1 && l != 0 && l != samples - 1)
                        continue;

                    double dx = (j / (samples - 1.0D) * 2.0D - 1.0D);
                    double dy = (k / (samples - 1.0D) * 2.0D - 1.0D);
                    double dz = (l / (samples - 1.0D) * 2.0D - 1.0D);

                    double invLen = 1.0D / Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx *= invLen;
                    dy *= invLen;
                    dz *= invLen;

                    // Ray "energy" budget
                    float budget = rayStartBudget;

                    double px = center.x;
                    double py = center.y;
                    double pz = center.z;

                    // march until out of energy or out of radius
                    for (float traveled = 0.0F; budget > 0.0F && traveled < stats.explosionRadius; traveled += step)
                    {
                        int bx = Mth.floor(px);
                        int by = Mth.floor(py);
                        int bz = Mth.floor(pz);
                        mpos.set(bx, by, bz);

                        BlockState state = level.getBlockState(mpos);
                        FluidState fluid = level.getFluidState(mpos);

                        boolean isEmpty = state.isAir() && fluid.isEmpty();
                        if (!isEmpty)
                        {
                            float resistance = damageCalculator.getBlockExplosionResistance(this, level, mpos, state, fluid).orElse(0F);

                            budget -= (0.25F * step); // free-space attenuation
                            budget -= (resistance + 0.3F) * 0.35F; // material attenuation

                            if (budget > 0.0F && damageCalculator.shouldBlockExplode(this, level, mpos, state, budget))
                                toBlow.add(mpos.immutable());
                        }
                        else
                        {
                            // even in air, budget should decay a bit with distance
                            budget -= (0.25F * step);
                        }

                        px += dx * step;
                        py += dy * step;
                        pz += dz * step;
                    }
                }
            }
        }

        affectedBlockPositions.addAll(toBlow);
    }

    protected void doHurtEntities()
    {
        hitPlayers.clear();

        List<Entity> entities = ModUtils.queryEntities(level, canDamageSelf ? null : explosive, getHurtEntitiesAabb(), e -> !e.ignoreExplosion());
        ForgeEventFactory.onExplosionDetonate(level, this, entities, stats.explosionRadius * 2F);

        for (Entity e : entities)
        {
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
        double falloff = 1.0 / (1.0 + Math.pow(normalizedDistance * ModCommonConfig.get().newDamageSystemBlastToExplosionRadiusRatio(), 3.0));

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
            kb = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, kb);

        // Knockback vector
        Vec3 kbVec = direction.scale(kb);

        e.setDeltaMovement(e.getDeltaMovement().add(kbVec));
        e.hurtMarked = true;

        if (e instanceof Player pl && !pl.isSpectator() && !(pl.getAbilities().flying && pl.getAbilities().instabuild))
            hitPlayers.put(pl, kbVec);
    }
}
