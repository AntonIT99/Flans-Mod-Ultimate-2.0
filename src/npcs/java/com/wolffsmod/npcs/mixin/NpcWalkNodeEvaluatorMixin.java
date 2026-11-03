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

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

/**
 * Custom NPCs' path node evaluator.
 *
 * <ul>
 * <li>Terrain profile: a ground vehicle does not enter water deeper than it wades, and a watercraft accepts nothing but
 * water deep enough to float in.</li>
 * <li>Driveable paths: an NPC at the wheel of a Flan's vehicle or mecha searches a path as wide and high as the driveable,
 * rather than one it would fit along on foot, and climbs onto walls and ledges only where it cannot drive around them.</li>
 * </ul>
 */
@Mixin(value = NpcWalkNodeEvaluator.class, remap = false)
@SuppressWarnings({"UnresolvedMixinReference", "AddedMixinMembersNamePattern"})
public abstract class NpcWalkNodeEvaluatorMixin extends WalkNodeEvaluator
{
    /** Blocks ahead of a step up within which ground dropping away again makes it a wall or ledge rather than a slope. */
    @Unique
    private static final int WOLFFSMODNPCS_RIDGE_REACH = 2;
    /** Extra path cost of climbing onto a wall or ledge, in blocks of detour. */
    @Unique
    private static final float WOLFFSMODNPCS_RIDGE_MALUS = 12F;

    /** Whether the current search is for a driveable, read once as it is prepared. */
    @Unique
    private boolean wolffsmodnpcsDriveable;

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
        wolffsmodnpcsDriveable = false;
        NpcDrivers.pathSize(mob).ifPresent(size ->
        {
            wolffsmodnpcsDriveable = true;
            entityWidth = Math.max(entityWidth, size.width());
            entityDepth = Math.max(entityDepth, size.width());
            entityHeight = Math.max(entityHeight, size.height());
        });
    }

    /**
     * Makes a driveable's path step up onto a wall or ledge only where there is no way around: a climb onto ground that
     * drops away again within {@value #WOLFFSMODNPCS_RIDGE_REACH} blocks ahead costs as much as a detour of
     * {@value #WOLFFSMODNPCS_RIDGE_MALUS} blocks.
     */
    @Override
    public int getNeighbors(@NotNull Node[] neighbors, @NotNull Node node)
    {
        int count = super.getNeighbors(neighbors, node);
        if (!wolffsmodnpcsDriveable)
            return count;
        for (int index = 0; index < count; index++)
        {
            Node next = neighbors[index];
            if (next != null && next.y > node.y && wolffsmodnpcsRidge(next.x, next.y, next.z, Integer.signum(next.x - node.x), Integer.signum(next.z - node.z)))
                next.costMalus = Math.max(next.costMalus, WOLFFSMODNPCS_RIDGE_MALUS);
        }
        return count;
    }

    /** @return whether the ground under a node stepped up onto in the given direction drops back down shortly ahead */
    @Unique
    private boolean wolffsmodnpcsRidge(int x, int y, int z, int dx, int dz)
    {
        if (dx == 0 && dz == 0)
            return false;
        BlockPos.MutableBlockPos ground = new BlockPos.MutableBlockPos();
        for (int step = 1; step <= WOLFFSMODNPCS_RIDGE_REACH; step++)
        {
            ground.set(x + dx * step, y - 1, z + dz * step);
            if (level.getBlockState(ground).getCollisionShape(level, ground).isEmpty())
                return true;
        }
        return false;
    }
}
