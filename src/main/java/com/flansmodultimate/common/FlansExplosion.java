package com.flansmodultimate.common;

import com.flansmodultimate.ModUtils;
import com.flansmodultimate.common.types.InfoType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FlansExplosion extends Explosion
{
    private final boolean causesFire;
    private final boolean breaksBlocks;
    private final Level level;
    private final double x;
    private final double y;
    private final double z;
    private final Vec3 center;
    @Nullable
    private final Player player;
    private final Entity explosive;
    private final float size;
    private final List<BlockPos> affectedBlockPositions;
    private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();
    private final Vec3 position;
    //TODO: integrate type in damage source (if meaningful)
    private final InfoType type; // type of Flan's Mod weapon causing explosion
    private final ExplosionDamageCalculator damageCalculator;

    public FlansExplosion(Level level, Entity entity, @Nullable Player player, InfoType type, double x, double y, double z, float size, boolean causesFire, boolean smoking, boolean breaksBlocks)
    {
        super(level, entity, x, y, z, size, causesFire, breaksBlocks ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP);
        this.affectedBlockPositions = Lists.newArrayList();
        this.level = level;
        this.player = player;
        this.size = size;
        this.x = x;
        this.y = y;
        this.z = z;
        this.center = new Vec3(x, y, z);
        this.causesFire = causesFire;
        //TODO Teams
        this.breaksBlocks = breaksBlocks; /*&& TeamsManager.explosions;*/
        this.position = new Vec3(this.x, this.y, this.z);
        this.type = type;
        this.explosive = entity;
        this.damageCalculator = (entity == null) ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(entity);

        if (!ForgeEventFactory.onExplosionStart(level, this))
        {
            explode();
            finalizeExplosion(smoking);
        }
    }

    //TODO: check code explode() and finalizeExplosion()

    /**
     * Does the first part of the explosion (destroy blocks)
     */
    @Override
    public void explode() {
        // Clear any previous results
        getToBlow().clear();
        getHitPlayers().clear();

        // ----- block raymarch (same idea as old j/k/l grid) -----
        if (breaksBlocks)
        {
            Set<BlockPos> set = new HashSet<>();

            for (int j = 0; j < 16; ++j)
            {
                for (int k = 0; k < 16; ++k)
                {
                    for (int l = 0; l < 16; ++l)
                    {
                        if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15)
                        {
                            double dx = (j / 15.0F * 2F - 1F);
                            double dy = (k / 15.0F * 2F - 1F);
                            double dz = (l / 15.0F * 2F - 1F);
                            double invLen = 1.0D / Math.sqrt(dx * dx + dy * dy + dz * dz);
                            dx *= invLen; dy *= invLen; dz *= invLen;

                            float power = size * (0.7F + level.random.nextFloat() * 0.6F);
                            double px = center.x;
                            double py = center.y;
                            double pz = center.z;

                            // step outward
                            while (power > 0.0F)
                            {
                                BlockPos pos = BlockPos.containing(px, py, pz);
                                BlockState state = level.getBlockState(pos);
                                FluidState fluid = level.getFluidState(pos);

                                if (!state.isAir())
                                {
                                    // Use the explosion damage calculator (new API)
                                    float resistance = 0F;
                                    if (damageCalculator != null)
                                    {
                                        Optional<Float> opt = damageCalculator.getBlockExplosionResistance(this, level, pos, state, fluid);
                                        resistance = opt.orElse(0F);
                                    }
                                    // vanilla subtracts (resistance + 0.3F) * 0.3F
                                    power -= (resistance + 0.3F) * 0.3F;
                                }

                                if (power > 0.0F && (damageCalculator == null || damageCalculator.shouldBlockExplode(this, level, pos, state, power)))
                                {
                                    set.add(pos);
                                }

                                px += dx * 0.3D;
                                py += dy * 0.3D;
                                pz += dz * 0.3D;
                                power -= 0.22500001F; // keep your falloff
                            }
                        }
                    }
                }
            }

            getToBlow().addAll(set);
        }

        // ----- entity damage & knockback (modern equivalents) -----
        float radius2 = size * 2.0F;
        int minX = Mth.floor(center.x - radius2 - 1.0D);
        int maxX = Mth.floor(center.x + radius2 + 1.0D);
        int minY = Mth.floor(center.y - radius2 - 1.0D);
        int maxY = Mth.floor(center.y + radius2 + 1.0D);
        int minZ = Mth.floor(center.z - radius2 - 1.0D);
        int maxZ = Mth.floor(center.z + radius2 + 1.0D);

        AABB aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Entity> entities = ModUtils.queryEntities(level, explosive, aabb, null);

        // Forge hook (still exists)
        ForgeEventFactory.onExplosionDetonate(level, this, entities, radius2);

        for (Entity e : entities)
        {
            if (e.ignoreExplosion())
                continue; // was isImmuneToExplosions()

            double distNorm = e.distanceToSqr(center) / (radius2 * radius2);
            if (distNorm > 1.0D)
                continue;

            double dx = e.getX() - center.x;
            double dy = e.getEyeY() - center.y;  // eye height (was posY + getEyeHeight() - y)
            double dz = e.getZ() - center.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len == 0.0D)
                continue;

            dx /= len; dy /= len; dz /= len;

            // exposure (was world.getBlockDensity(vec3d, entity.getEntityBoundingBox()))
            double exposure = Explosion.getSeenPercent(center, e);
            double impact = (1.0D - Math.sqrt(distNorm)) * exposure; // (1 - d12) * exposure

            if (impact <= 0) continue;

            // ----- damage -----
            double scaled = (impact * impact + impact) / 2.0D * 7.0D * radius2 + 1.0D;
            float damage = (float) scaled;

            DamageSource src;
            if (player != null)
            {
                // direct cause = explosive (could be null), attacker = player
                src = FlansDamageSources.createDamageSource(level, explosive, player, FlansDamageSources.FLANS_EXPLOSION);
            }
            else
            {
                // vanilla explosion source
                src = level.damageSources().explosion(this); // (explosion-aware)
            }

            e.hurt(src, damage);

            // ----- knockback (with enchantment reduction for living entities) -----
            double kb = impact;
            if (e instanceof LivingEntity living)
            {
                kb = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, impact);
            }

            e.setDeltaMovement(e.getDeltaMovement().add(dx * kb, dy * kb, dz * kb));

            if (e instanceof Player pl && !pl.isSpectator() && !(pl.getAbilities().flying && pl.getAbilities().instabuild))
            {
                // store raw (impact) vector like your old map did
                getHitPlayers().put(pl, new Vec3(dx * impact, dy * impact, dz * impact));
            }
        }
    }

    /**
     * Does the second part of the explosion (sound, particles, drop spawn)
     */
    @Override
    public void finalizeExplosion(boolean spawnParticles)
    {
        // Fire the game event once at the center
        if (level instanceof ServerLevel)
        {
            level.gameEvent(GameEvent.EXPLODE, BlockPos.containing(center), GameEvent.Context.of(explosive != null ? explosive : player));
        }

        // sound
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        // particles
        if (spawnParticles && level instanceof ServerLevel sl)
        {
            if (size >= 2.0F && interactsWithBlocks())
            {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                        center.x, center.y, center.z, 1, 0, 0, 0, 0.0);
            }
            else
            {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        center.x, center.y, center.z, 1, 0, 0, 0, 0.0);
            }
        }

        // block breaking & drops
        if (interactsWithBlocks())
        {
            for (BlockPos pos : getToBlow())
            {
                BlockState state = level.getBlockState(pos);
                if (state.isAir())
                    continue;

                // optional debris/smoke particles like your old snippet
                if (spawnParticles && level instanceof ServerLevel sl)
                {
                    double px = pos.getX() + level.random.nextDouble();
                    double py = pos.getY() + level.random.nextDouble();
                    double pz = pos.getZ() + level.random.nextDouble();
                    double dx = px - center.x;
                    double dy = py - center.y;
                    double dz = pz - center.z;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (len != 0.0D)
                    {
                        dx /= len; dy /= len; dz /= len;
                        double scale = 0.5D / (len / size + 0.1D);
                        scale *= (level.random.nextDouble() * level.random.nextDouble() + 0.3D);
                        dx *= scale; dy *= scale; dz *= scale;

                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                                (px + center.x) / 2.0D, (py + center.y) / 2.0D, (pz + center.z) / 2.0D,
                                1, dx, dy, dz, 0.0);
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                                px, py, pz, 1, dx, dy, dz, 0.0);
                    }
                }

                // Let the block handle its own explosion behavior (drops/updates).
                // This mirrors modern vanilla.
                state.onBlockExploded(level, pos, this);
            }
        }

        // fire
        if (causesFire)
        {
            RandomSource r = (level instanceof ServerLevel sl) ? sl.random : level.random;
            for (BlockPos pos : getToBlow())
            {
                if (level.isEmptyBlock(pos)
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && r.nextInt(3) == 0)
                {
                    level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                }
            }
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
        return affectedBlockPositions;
    }

    @Override
    @NotNull
    public Vec3 getPosition()
    {
        return position;
    }
}
