package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.entity.ILivingVisualEffects;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketParticle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-authored, temporary ground splatters inspired by Labjac's death and overkill effects. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BloodEffects
{
    private static final String PARTICLE = "flansmodultimate:ground_blood";
    private static final double MAX_GROUND_DISTANCE = 8D;
    private static final double VIEW_DISTANCE = 64D;

    public static void onDamage(LivingEntity entity, float amount)
    {
        if (amount > 40F && !entity.isPassenger())
            spawn(entity, 7);
    }

    public static void onDeath(LivingEntity entity)
    {
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
