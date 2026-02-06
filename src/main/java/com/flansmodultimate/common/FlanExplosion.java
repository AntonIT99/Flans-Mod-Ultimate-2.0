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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlanExplosion extends Explosion
{
    protected static final double EXPLOSION_PARTICLE_RANGE = 256;
    protected static final float HALF_DAMAGE_RADIUS_FRACTION = 0.7F;
    protected static final float KNOCKBACK_MULTIPLAYER = 1F;
    
    // Config
    protected final boolean causesFire;
    protected final boolean breaksBlocks;
    protected final boolean canDamageSelf;
    protected final DamageStats damage;

    // Core Context
    protected final Level level;
    protected final Vec3 center;
    protected final float radius;
    protected final float power;
    protected final int smokeCount;
    protected final int debrisCount;
    
    @Nullable
    protected final LivingEntity causingEntity;
    protected final Entity explosive;

    protected final ExplosionDamageCalculator damageCalculator;
    protected final List<BlockPos> affectedBlockPositions;
    protected final Map<Player, Vec3> hitPlayers = Maps.newHashMap();

    public FlanExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, ShootableType type, double x, double y, double z, boolean canDamageSelf)
    {
        this(level, explosive, causingEntity, x, y, z, type.getExplosionRadius(), type.getExplosionPower(), type.getFireRadius() > 0,
            type.isExplosionBreaksBlocks() && ModCommonConfig.get().explosionsBreakBlocks() && FlansMod.teamsManager.isExplosionsBreakBlocks(),
            type.getExplosionDamage(), type.getSmokeParticleCount(), type.getDebrisParticleCount(), canDamageSelf);
    }

    public FlanExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, double x, double y, double z, float explosionRadius, float explosionPower,
                         boolean causesFire, boolean breaksBlocks, DamageStats damage, int smokeCount, int debrisCount, boolean canDamageSelf)
    {
        super(level, explosive, x, y, z, explosionRadius, causesFire, breaksBlocks ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP);

        this.level = level;
        this.explosive = explosive;
        this.causingEntity = causingEntity;

        center = new Vec3(x, y, z);
        radius = explosionRadius;
        power = explosionPower;

        this.causesFire = causesFire;
        this.breaksBlocks = breaksBlocks;

        this.canDamageSelf = canDamageSelf;
        this.damage = damage;
        this.smokeCount = smokeCount;
        this.debrisCount = debrisCount;

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
            if (radius >= 2.0F && interactsWithBlocks())
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

        if (spawnParticles && interactsWithBlocks() && !getToBlow().isEmpty())
            PacketHandler.sendToAllAround(new PacketFlanExplosionBlockParticles(center, radius, getToBlow()), center, Math.max(EXPLOSION_PARTICLE_RANGE, radius), level.dimension());

        PacketHandler.sendToAllAround(new PacketFlanExplosionParticles(center, smokeCount, debrisCount, radius), center, Math.max(EXPLOSION_PARTICLE_RANGE, radius), level.dimension());
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
        return affectedBlockPositions;
    }

    protected void doBreakBlocks()
    {
        affectedBlockPositions.clear();
        if (!breaksBlocks)
            return;

        // Prevent extreme CPU load when radius gets big
        int samples = Mth.clamp(Mth.ceil(radius * 2.0F), 16, 48);

        Set<BlockPos> toBlow = new HashSet<>();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        // Step size: smaller = more accurate, slower.
        float step = 0.3F;

        // A "ray energy" budget. Since you set power ∝ cbrt(W) and radius ∝ cbrt(W),
        // power * radius ∝ W^(2/3), which is already strongly scaling.
        float rayStartBudget = power * radius * (0.85F + level.random.nextFloat() * 0.3F);

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
                    for (float traveled = 0.0F; budget > 0.0F && traveled < radius; traveled += step)
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

        // Query range: use radius directly (and a bit extra for knockback/edge effects)
        float queryRadius = radius + 2.0F;

        int minX = Mth.floor(center.x - queryRadius - 1.0D);
        int maxX = Mth.floor(center.x + queryRadius + 1.0D);
        int minY = Mth.floor(center.y - queryRadius - 1.0D);
        int maxY = Mth.floor(center.y + queryRadius + 1.0D);
        int minZ = Mth.floor(center.z - queryRadius - 1.0D);
        int maxZ = Mth.floor(center.z + queryRadius + 1.0D);

        AABB aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        List<Entity> entities = ModUtils.queryEntities(level, canDamageSelf ? null : explosive, aabb, e -> !e.ignoreExplosion());

        ForgeEventFactory.onExplosionDetonate(level, this, entities, radius);

        for (Entity e : entities)
        {
            // distance from explosion center to entity (blocks)
            double dx = e.getX() - center.x;
            double dy = e.getEyeY() - center.y;
            double dz = e.getZ() - center.z;
            double r = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (r < 1.0e-6)
                return;

            // normalized direction (for knockback)
            double invR = 1.0 / r;
            double ndx = dx * invR;
            double ndy = dy * invR;
            double ndz = dz * invR;

            // normalized distance based on radius
            double u = r / Math.max(0.001, radius);
            if (u >= 1.5)
                return;

            // blast-like falloff based on radius
            double falloff = 1.0 / (1.0 + Math.pow(u / HALF_DAMAGE_RADIUS_FRACTION, 3.0));

            // soft cutoff so it’s ~0 at the edge of radius
            double edge = 1.0 - u;
            edge = Math.max(edge, 0.0);
            double cutoff = edge * edge; // edge^2 feels good; edge^3 is harsher
            falloff *= cutoff;

            // occlusion
            double seen = Explosion.getSeenPercent(center, e);
            if (seen <= 0.0)
                return;

            Entity proxy = e;
            float extra = 1.0F;

            if (e instanceof Wheel wheel)
            {
                proxy = wheel.getDriveable();
                extra *= (float) ModCommonConfig.get().vehicleWheelSeatExplosionModifier();
            }
            else if (e instanceof Seat seat)
            {
                proxy = seat.getDriveable();
                extra *= (float) ModCommonConfig.get().vehicleWheelSeatExplosionModifier();
            }

            float mult = damage.getDamageAgainstEntity(proxy);

            // Final damage
            float explosionDamage = (float) (damage.getDamage() * falloff * seen) * mult * extra;

            if (explosionDamage > 0.1F)
            {
                DamageSource src = FlanDamageSources.createDamageSource(level, explosive, causingEntity, FlanDamageSources.EXPLOSION);
                boolean hurt = e.hurt(src, explosionDamage);
                if (hurt && causingEntity instanceof ServerPlayer sp)
                    PacketHandler.sendTo(new PacketHitMarker(false, 1.0F, true), sp);
            }

            // Knockback: also scaled-distance based
            double kb = falloff * seen * KNOCKBACK_MULTIPLAYER;

            if (e instanceof LivingEntity living)
                kb = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, kb);

            e.setDeltaMovement(e.getDeltaMovement().add(ndx * kb, ndy * kb, ndz * kb));
            e.hurtMarked = true;

            if (e instanceof Player pl && !pl.isSpectator() && !(pl.getAbilities().flying && pl.getAbilities().instabuild))
                hitPlayers.put(pl, new Vec3(ndx * kb, ndy * kb, ndz * kb));
        }
    }
}
