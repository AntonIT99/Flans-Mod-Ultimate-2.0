package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcFlightPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Vanilla's path search through the air.
 *
 * <ul>
 * <li>An NPC aircraft, which keeps its height above the terrain, searches with a body no larger than
 * {@link NpcFlightPaths#MAX_BODY_WIDTH} by {@link NpcFlightPaths#MAX_BODY_HEIGHT} blocks: every step tests each block of
 * the body and those round it, which for an aircraft model takes the search seconds.</li>
 * </ul>
 */
@Mixin(FlyNodeEvaluator.class)
public abstract class FlyNodeEvaluatorMixin extends WalkNodeEvaluator
{
    @Inject(method = "prepare", at = @At("TAIL"))
    private void wolffsmodnpcsAircraftBody(PathNavigationRegion region, Mob mob, CallbackInfo callback)
    {
        if (!NpcFlightPaths.limitsBody(mob))
            return;
        entityWidth = Math.min(entityWidth, NpcFlightPaths.MAX_BODY_WIDTH);
        entityDepth = Math.min(entityDepth, NpcFlightPaths.MAX_BODY_WIDTH);
        entityHeight = Math.min(entityHeight, NpcFlightPaths.MAX_BODY_HEIGHT);
    }
}
