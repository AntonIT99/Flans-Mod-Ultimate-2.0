package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcMobility;
import noppes.npcs.ai.NpcWalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Custom NPCs' path node evaluator.
 *
 * <ul>
 * <li>Terrain profile: a ground vehicle does not enter water deeper than it wades, and a watercraft accepts nothing but
 * water deep enough to float in.</li>
 * <li>Driveable paths: an NPC at the wheel of a Flan's vehicle or mecha searches a path as wide and high as the driveable,
 * rather than one it would fit along on foot.</li>
 * </ul>
 */
@Mixin(value = NpcWalkNodeEvaluator.class, remap = false)
@SuppressWarnings({"UnresolvedMixinReference", "AddedMixinMembersNamePattern"})
public abstract class NpcWalkNodeEvaluatorMixin extends WalkNodeEvaluator
{
    // Terrain profile

    @Inject(method = {"getCachedBlockType", "m_77567_"}, at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("ConstantValue") // IntelliJ trusts the region's declaration, which wolffsmodnpcsOutsideSearch explains.
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

    // Driveable paths

    @Override
    public void prepare(@NotNull PathNavigationRegion region, @NotNull Mob mob)
    {
        super.prepare(region, mob);
        NpcDrivers.pathSize(mob).ifPresent(size ->
        {
            entityWidth = Math.max(entityWidth, size.width());
            entityDepth = Math.max(entityDepth, size.width());
            entityHeight = Math.max(entityHeight, size.height());
        });
    }
}
