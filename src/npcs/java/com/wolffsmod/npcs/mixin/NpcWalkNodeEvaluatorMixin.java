package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcMobility;
import noppes.npcs.ai.NpcWalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Custom NPCs' path node evaluator, taught the NPC's terrain profile: a ground vehicle does not enter water deeper than it
 * wades, and a watercraft accepts nothing but water deep enough to float in.
 */
@Mixin(value = NpcWalkNodeEvaluator.class, remap = false)
@SuppressWarnings({"UnresolvedMixinReference", "AddedMixinMembersNamePattern"})
public abstract class NpcWalkNodeEvaluatorMixin extends WalkNodeEvaluator
{
    @Inject(method = "getCachedBlockType", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("ConstantValue") // The pathfinding context is null between searches, whatever its declaration promises.
    private void wolffsmodnpcsTerrainProfile(Mob mob, int x, int y, int z, CallbackInfoReturnable<PathType> callback)
    {
        PathType type = callback.getReturnValue();
        if (wolffsmodnpcsOutsideSearch() || type == null)
            return;
        PathType filtered = NpcMobility.filter(mob, currentContext.level(), x, y, z, type);
        if (filtered != type)
            callback.setReturnValue(filtered);
    }

    /** The pathfinding context is only set while a search runs: Custom NPCs' navigator also asks between searches. */
    @Unique
    @SuppressWarnings("ConstantValue")
    private boolean wolffsmodnpcsOutsideSearch()
    {
        return currentContext == null;
    }
}
