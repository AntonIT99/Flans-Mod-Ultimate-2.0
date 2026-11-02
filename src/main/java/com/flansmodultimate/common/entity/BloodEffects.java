package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.entity.ILivingVisualEffects;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketParticle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/** Server-authored wounds and temporary ground splatters, shared by players and opt-in living entities. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BloodEffects
{
    private static final String PARTICLE = "flansmodultimate:ground_blood";
    private static final double MAX_GROUND_DISTANCE = 8D;
    private static final double VIEW_DISTANCE = 64D;
    private static final ThreadLocal<Wound> WOUND = new ThreadLocal<>();
    private static final Map<LivingEntity, Long> HEAVY_HITS = new WeakHashMap<>();

    private record Wound(LivingEntity entity, Vec3 position, Vec3 direction)
    {}

    /** Carries the traced wound through synchronous damage events, including nested damage from other mods. Server thread only. */
    public static boolean hurt(LivingEntity entity, Vec3 position, Vec3 direction, BooleanSupplier damage)
    {
        Wound previous = WOUND.get();
        WOUND.set(new Wound(entity, position, direction));
        try
        {
            return damage.getAsBoolean();
        }
        finally
        {
            if (previous == null)
                WOUND.remove();
            else
                WOUND.set(previous);
        }
    }

    /** Called only for final, uncancelled, positive damage. Blocked armour and shield hits cannot bleed. */
    public static void onDamage(LivingEntity entity, DamageSource source, float amount)
    {
        if (!Float.isFinite(amount) || amount <= 0F || !(entity.level() instanceof ServerLevel level) || !enabled(entity, TeamsManager.getInstance().isBloodEffects()))
            return;
        Wound traced = WOUND.get();
        boolean heavy = heavy(entity, source, amount);
        if (heavy || hasWound(entity, source, traced))
            airborne(entity, level, traced, heavy);
        if (heavy)
        {
            HEAVY_HITS.put(entity, level.getGameTime());
            spawn(entity, amount > 40F ? 7 : 3);
        }
    }

    private static boolean heavy(LivingEntity entity, DamageSource source, float amount)
    {
        return !entity.isPassenger() && (amount > 40F || source.getDirectEntity() instanceof Driveable || source.is(DamageTypeTags.IS_EXPLOSION) && amount >= entity.getHealth());
    }

    private static boolean hasWound(LivingEntity entity, DamageSource source, Wound traced)
    {
        return traced != null && traced.entity == entity || source.getDirectEntity() != null || source.getEntity() != null || source.is(DamageTypeTags.IS_PROJECTILE);
    }

    private static void airborne(LivingEntity entity, ServerLevel level, Wound traced, boolean heavy)
    {
        boolean positioned = traced != null && traced.entity == entity;
        Vec3 at = positioned ? traced.position : entity.position().add(0D, entity.getBbHeight() * 0.6D, 0D);
        Vec3 direction = positioned ? traced.direction.normalize().scale(0.08D) : Vec3.ZERO;
        int count = heavy ? 5 : 1;
        for (int i = 0; i < count; i++)
        {
            Vec3 motion = direction.add((entity.getRandom().nextDouble() - 0.5D) * 0.08D, heavy ? 0.12D : 0.02D, (entity.getRandom().nextDouble() - 0.5D) * 0.08D);
            PacketHandler.sendToAllAround(new PacketParticle(heavy ? FlanParticles.FM_OVERKILL : FlanParticles.FM_BLOOD, at.x, at.y, at.z, motion.x, motion.y, motion.z), at, VIEW_DISTANCE,
                level.dimension());
        }
    }

    public static void onDamage(LivingEntity entity, float amount)
    {
        if (amount > 40F && !entity.isPassenger())
            spawn(entity, 7);
    }

    public static void onDeath(LivingEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel))
            return;
        Long heavyTick = HEAVY_HITS.remove(entity);
        if (heavyTick == null || heavyTick != entity.level().getGameTime())
            spawn(entity, 1);
    }

    /** The server rule gates players and opt-in entities alike; machines and ordinary mobs do not opt in. */
    public static boolean enabled(LivingEntity entity, boolean ruleEnabled)
    {
        return ruleEnabled && !entity.isSpectator() && (entity instanceof Player || entity instanceof ILivingVisualEffects effects && effects.flansmodultimateBloodEffects());
    }

    private static void spawn(LivingEntity entity, int count)
    {
        if (!(entity.level() instanceof ServerLevel level) || !enabled(entity, TeamsManager.getInstance().isBloodEffects()))
            return;
        for (int i = 0; i < count; i++)
            spawnOne(entity, level);
    }

    private static void spawnOne(LivingEntity entity, ServerLevel level)
    {
        Vec3 start = entity.position().add((entity.getRandom().nextDouble() - 0.5D) * 1.2D, 0.5D, (entity.getRandom().nextDouble() - 0.5D) * 1.2D);
        Vec3 end = start.add(0D, -MAX_GROUND_DISTANCE, 0D);
        BlockPos block = BlockPos.containing(start);
        if (!level.getChunkSource().hasChunk(block.getX() >> 4, block.getZ() >> 4))
            return;
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP || !level.getFluidState(hit.getBlockPos()).isEmpty())
            return;
        Vec3 position = hit.getLocation().add(0D, 0.003D, 0D);
        PacketHandler.sendToAllAround(new PacketParticle(PARTICLE, position.x, position.y, position.z, 0D, 0D, 0D), position, VIEW_DISTANCE, level.dimension());
    }
}
