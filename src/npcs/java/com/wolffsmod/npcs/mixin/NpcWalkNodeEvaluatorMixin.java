package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcMobility;
import noppes.npcs.ai.NpcWalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Custom NPCs' path node evaluator, taught the NPC's terrain profile: a ground vehicle does not enter water deeper than it
 * wades, and a watercraft accepts nothing but water deep enough to float in.
 */
@Mixin(value = NpcWalkNodeEvaluator.class, remap = false)
@SuppressWarnings({"UnresolvedMixinReference", "AddedMixinMembersNamePattern"})
public abstract class NpcWalkNodeEvaluatorMixin extends WalkNodeEvaluator
{
    @Inject(method = {"getCachedBlockType", "m_77567_"}, at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsTerrainProfile(Mob mob, int x, int y, int z, CallbackInfoReturnable<BlockPathTypes> callback)
    {
        BlockPathTypes type = callback.getReturnValue();
        if (wolffsmodnpcsOutsideSearch() || type == null)
            return;
        BlockPathTypes filtered = NpcMobility.filter(mob, level, x, y, z, type);
        if (filtered != type)
            callback.setReturnValue(filtered);
    }

    /** The region is only set while a search runs, whatever its declaration promises: Custom NPCs' navigator also asks between searches. */
    @Unique
    @SuppressWarnings("ConstantValue")
    private boolean wolffsmodnpcsOutsideSearch()
    {
        return level == null;
    }
}
