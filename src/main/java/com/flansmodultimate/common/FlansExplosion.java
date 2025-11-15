package com.flansmodultimate.common;

import com.flansmodultimate.common.driveables.Seat;
import com.flansmodultimate.common.driveables.Wheel;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.PacketHitMarker;
import com.flansmodultimate.network.PacketParticle;
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
import java.util.function.Predicate;

public class FlansExplosion extends Explosion
{
    private static final double EXPLOSION_PARTICLE_RANGE = 150.0; 
    
    // Config
    private final boolean causesFire;
    private final boolean breaksBlocks;
    private final boolean canDamageSelf;
    private final DamageStats damage;

    // Core Context
    private final Level level;
    private final Vec3 center;
    private final float radius;
    private final float power;

    //TODO: integrate type in damage source (if possible)
    private final InfoType type; // type of Flan's Mod weapon causing explosion
    @Nullable
    private final LivingEntity causingEntity;
    private final Entity explosive;

    private final ExplosionDamageCalculator damageCalculator;
    private final List<BlockPos> affectedBlockPositions;
    private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();

    public FlansExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, ShootableType type, double x, double y, double z, boolean smoking, boolean canDamageSelf)
    {
        this(level, explosive, causingEntity, type, x, y, z, type.getExplosionRadius(), type.getExplosionPower(), type.getFireRadius() > 0, smoking, type.isExplosionBreaksBlocks(),
                type.getDamage(), type.getSmokeParticleCount(), type.getDebrisParticleCount(), canDamageSelf);
    }

    public FlansExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, GrenadeType type, double x, double y, double z, boolean canDamageSelf)
    {
        this(level, explosive, causingEntity, type, x, y, z, type.getExplosionRadius(), type.getExplosionPower(), type.getFireRadius() > 0, type.getSmokeRadius() > 0, type.isExplosionBreaksBlocks(),
                type.getDamage(), type.getSmokeParticleCount(), type.getDebrisParticleCount(), canDamageSelf);
    }

    public FlansExplosion(Level level, @Nullable Entity explosive, @Nullable LivingEntity causingEntity, InfoType type, double x, double y, double z, float explosionRadius, float explosionPower,
                          boolean causesFire, boolean smoking, boolean breaksBlocks, DamageStats damage, int smokeCount, int debrisCount, boolean canDamageSelf)
    {
        super(level, explosive, x, y, z, explosionRadius, causesFire, breaksBlocks ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP);

        this.level = level;
        this.explosive = explosive;
        this.causingEntity = causingEntity;
        this.type = type;

        this.center = new Vec3(x, y, z);
        this.radius = explosionRadius;
        this.power = explosionPower;

        this.causesFire = causesFire;
        this.breaksBlocks = breaksBlocks && TeamsManager.isExplosions();

        this.canDamageSelf = canDamageSelf;
        this.damage = damage;

        this.affectedBlockPositions = Lists.newArrayList();
        this.damageCalculator = (explosive == null) ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(explosive);

        if (!ForgeEventFactory.onExplosionStart(level, this))
        {
            explode();
            finalizeExplosion(smoking);
            // Custom Flan’s extra particles
            spawnParticles(smokeCount, debrisCount);
        }
    }

    /**
     * Does the first part of the explosion (destroy blocks)
     */
    @Override
    public void explode()
    {
        // Clear any previous results
        affectedBlockPositions.clear();
        getHitPlayers().clear();

        float largeRadius = radius * 2;

        // block raymarch (same idea as old j/k/l grid)
        if (breaksBlocks)
        {
            Set<BlockPos> set = new HashSet<>();

            for (int j = 0; j < largeRadius; ++j)
            {
                for (int k = 0; k < largeRadius; ++k)
                {
                    for (int l = 0; l < largeRadius; ++l)
                    {
                        if (j == 0 || j == largeRadius - 1 || k == 0 || k == largeRadius - 1 || l == 0 || l == largeRadius - 1)
                        {
                            double dx = (j / (largeRadius - 1.0F) * 2F - 1F);
                            double dy = (k / (largeRadius - 1.0F) * 2F - 1F);
                            double dz = (l / (largeRadius - 1.0F) * 2F - 1F);
                            double invLen = 1.0D / Math.sqrt(dx * dx + dy * dy + dz * dz);
                            dx *= invLen;
                            dy *= invLen;
                            dz *= invLen;

                            float explosionPower = this.power * radius * (0.7F + level.random.nextFloat() * 0.6F);
                            double px = center.x;
                            double py = center.y;
                            double pz = center.z;

                            // step outward
                            for (float step = 0.3F; explosionPower > 0.0F; explosionPower -= step * 0.75F)
                            {
                                BlockPos pos = BlockPos.containing(px, py, pz);
                                BlockState state = level.getBlockState(pos);
                                FluidState fluid = level.getFluidState(pos);

                                if (!state.isAir())
                                {
                                    // Use the explosion damage calculator (new API)
                                    float resistance = 0F;
                                    if (damageCalculator != null)
                                        resistance = damageCalculator.getBlockExplosionResistance(this, level, pos, state, fluid).orElse(0F);

                                    double distFactor = Math.sqrt(Math.pow(px - center.x, 2) + Math.pow(py - center.y, 2) + Math.pow(pz - center.z, 2));
                                    if (distFactor + 0.5 < radius)
                                    {
                                        explosionPower -= ((resistance + 0.3F) * step);
                                    }
                                    else
                                    {
                                        // If we're outside the radius, make it extremely difficult for the explosion to proceed.
                                        explosionPower -= (float) ((resistance + 0.3F) * step * Math.pow((distFactor - radius + 2), 3) * 20);
                                    }
                                }

                                if (explosionPower > 0.0F && (damageCalculator == null || damageCalculator.shouldBlockExplode(this, level, pos, state, explosionPower)))
                                {
                                    set.add(pos);
                                }

                                px += dx * step;
                                py += dy * step;
                                pz += dz * step;
                            }
                        }
                    }
                }
            }

            affectedBlockPositions.addAll(set);

            // entity damage & knockback (modern equivalents)
            float radius2 = radius * 2.0F;
            int minX = Mth.floor(center.x - radius2 - 1.0D);
            int maxX = Mth.floor(center.x + radius2 + 1.0D);
            int minY = Mth.floor(center.y - radius2 - 1.0D);
            int maxY = Mth.floor(center.y + radius2 + 1.0D);
            int minZ = Mth.floor(center.z - radius2 - 1.0D);
            int maxZ = Mth.floor(center.z + radius2 + 1.0D);

            AABB aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

            Predicate<Entity> selector = e -> !e.isRemoved() && !(e == this.explosive && !canDamageSelf) && !e.ignoreExplosion();
            List<Entity> entities = ModUtils.queryEntities(level, explosive, aabb, selector);

            // Forge hook (still exists)
            ForgeEventFactory.onExplosionDetonate(level, this, entities, radius2);

            for (Entity e : entities)
            {
                double distNorm = Math.sqrt(e.distanceToSqr(center)) / radius2;
                if (distNorm > 1.0D)
                    continue;

                double dx = e.getX() - center.x;
                double dy = e.getEyeY() - center.y;  // eye height (was posY + getEyeHeight() - y)
                double dz = e.getZ() - center.z;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len == 0.0D)
                    continue;

                dx /= len;
                dy /= len;
                dz /= len;
                double seen = Explosion.getSeenPercent(center, e);
                double impact = (1.0D - distNorm) * seen;

                if (impact <= 0)
                    continue;

                // vanilla base damage (7.0F scale, like 1.12.2)
                float damage = (float) ((impact * impact + impact) / 2.0D * 8.0D * radius2 + 1.0D);

                // === Flan’s multipliers ===
                damage *= this.damage.getDamageValue(e);

                if (e instanceof Wheel wheel)
                {
                    damage *= ModCommonConfigs.vehicleWheelSeatExplosionModifier.get();
                    damage *= this.damage.getDamageValue(wheel.getDriveable());
                }
                else if (e instanceof Seat seat)
                {
                    damage *= ModCommonConfigs.vehicleWheelSeatExplosionModifier.get();
                    damage *= this.damage.getDamageValue(seat.getDriveable());
                }

                if (damage > 0.5F)
                {
                    DamageSource damageSource = FlansDamageSources.createDamageSource(level, explosive, causingEntity, FlansDamageSources.FLANS_EXPLOSION);
                    boolean hurt = e.hurt(damageSource, damage);
                    if (hurt && causingEntity instanceof ServerPlayer serverPlayer)
                        PacketHandler.sendTo(new PacketHitMarker(false, 1.0F, true), serverPlayer);
                }

                // knockback (with enchantment reduction for living entities)
                double kb = impact;
                if (e instanceof LivingEntity living)
                {
                    kb = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, impact);
                }

                // Apply velocity
                e.setDeltaMovement(e.getDeltaMovement().add(dx * kb, dy * kb, dz * kb));
                e.hurtMarked = true;

                // Track per-player knockback, excluding spectators/flying creative (vanilla parity)
                if (e instanceof Player pl && !pl.isSpectator() && !(pl.getAbilities().flying && pl.getAbilities().instabuild))
                {
                    // store raw (impact) vector like your old map did
                    getHitPlayers().put(pl, new Vec3(dx * impact, dy * impact, dz * impact));
                }
            }
        }

        if (!breaksBlocks)
            affectedBlockPositions.clear();
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
            level.gameEvent(GameEvent.EXPLODE, BlockPos.containing(center), GameEvent.Context.of(explosive != null ? explosive : causingEntity));
        }

        // sound
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        // particles
        if (spawnParticles && level instanceof ServerLevel sl)
        {
            if (radius >= 2.0F && interactsWithBlocks())
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0.0);
            else
                sl.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0.0);
        }

        // block breaking & drops
        if (interactsWithBlocks())
        {
            for (BlockPos pos : getToBlow())
            {
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();

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
                        dx /= len;
                        dy /= len;
                        dz /= len;
                        double scale = 0.5D / (len / radius + 0.1D);
                        scale *= (level.random.nextDouble() * level.random.nextDouble() + 0.3D);
                        dx *= scale;
                        dy *= scale;
                        dz *= scale;

                        sl.sendParticles(ParticleTypes.EXPLOSION, (px + center.x) / 2.0D, (py + center.y) / 2.0D, (pz + center.z) / 2.0D, 1, dx, dy, dz, 0.0);
                        sl.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, dx, dy, dz, 0.0);
                    }
                }

                if (!state.isAir())
                {
                    if (state.canDropFromExplosion(level, pos, this))
                    {
                        BlockEntity be = level.getBlockEntity(pos);
                        Entity attacker = getIndirectSourceEntity();
                        Block.dropResources(state, level, pos, be, attacker, ItemStack.EMPTY);
                    }
                    state.onBlockExploded(level, pos, this);
                }
            }
        }

        // fire
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

    private void spawnParticles(int numSmoke, int numDebris)
    {
        float mod = radius * 0.1F;

        for (int smoke = 0; smoke < numSmoke; smoke++)
        {
            float smokeRand = (float) Math.random();

            if(smokeRand < 0.25)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.flare", center.x, center.y, center.z, (float)Math.random()*mod, (float)Math.random()*mod, (float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            } 
            else if (smokeRand > 0.25 && smokeRand < 0.5)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.flare", center.x, center.y, center.z, (float)Math.random()*mod, (float)Math.random()*mod, -(float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            } 
            else if (smokeRand > 0.5 && smokeRand < 0.75)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.flare", center.x, center.y, center.z, -(float)Math.random()*mod, (float)Math.random()*mod, -(float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            } 
            else if (smokeRand > 0.75)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.flare", center.x, center.y, center.z, -(float)Math.random()*mod, (float)Math.random()*mod, (float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            }
        }

        for (int debris = 0; debris < numDebris; debris++)
        {
            float smokeRand = (float) Math.random();

            if(smokeRand < 0.25)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.debris1", center.x, center.y, center.z, (float)Math.random()*mod, (float)Math.random()*mod, (float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            } 
            else if (smokeRand > 0.25 && smokeRand < 0.5)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.debris1", center.x, center.y, center.z, (float)Math.random()*mod, (float)Math.random()*mod, -(float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            } 
            else if (smokeRand > 0.5 && smokeRand < 0.75)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.debris1", center.x, center.y, center.z, -(float)Math.random()*mod, (float)Math.random()*mod, (float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            } 
            else if (smokeRand > 0.75)
            {
                PacketHandler.sendToAllAround(new PacketParticle("flansmod.debris1", center.x, center.y, center.z, -(float)Math.random()*mod, (float)Math.random()*mod, -(float)Math.random()*mod), center.x, center.y, center.z, EXPLOSION_PARTICLE_RANGE, level.dimension());
            }
        }
    }
}
