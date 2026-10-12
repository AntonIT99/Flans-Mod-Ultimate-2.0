package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.sim.NpcMobility;
import noppes.npcs.ai.NpcGroundPathNavigator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Set;

/**
 * Custom NPCs' ground navigator, made affordable for long-range NPCs.
 *
 * <ul>
 * <li>A path search reaches as far as its goal needs plus a detour margin, instead of the NPC's whole aggro range: Custom NPCs
 * stores that range where vanilla keeps the search range, so a 512-block sniper otherwise searches a square kilometre.</li>
 * <li>A pursuit keeps its path for a few ticks, where the combat goals ask for a new one every tick while their target moves.
 * The first search of each NPC is offset, so a crowd does not search in the same tick.</li>
 * <li>While a benchmark or the profiler measures, every search is timed and the slow ones are recorded.</li>
 * </ul>
 */
@Mixin(value = NpcGroundPathNavigator.class, remap = false)
@SuppressWarnings("AddedMixinMembersNamePattern")
public abstract class NpcGroundPathNavigatorMixin extends GroundPathNavigation
{
    @Unique
    private long wolffsmodnpcsNextPursuit = Long.MIN_VALUE;
    @Unique
    private int wolffsmodnpcsPursued = -1;

    protected NpcGroundPathNavigatorMixin(Mob mob, Level level)
    {
        super(mob, level);
    }

    @Override
    public boolean moveTo(@NotNull Entity entity, double speed)
    {
        int interval = NpcMobility.pursuitInterval(mob);
        if (interval > 1)
        {
            long now = level.getGameTime();
            if (entity.getId() == wolffsmodnpcsPursued && now < wolffsmodnpcsNextPursuit && path != null && !path.isDone())
            {
                speedModifier = speed;
                return true;
            }
            // A new pursuit starts at a moment of the NPC's own; afterwards it keeps the interval.
            wolffsmodnpcsNextPursuit = now + interval + (entity.getId() == wolffsmodnpcsPursued ? 0 : Math.floorMod(mob.getId(), interval));
            wolffsmodnpcsPursued = entity.getId();
        }
        return super.moveTo(entity, speed);
    }

    @Nullable
    @Override
    protected Path createPath(@NotNull Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange)
    {
        float range = NpcMobility.searchRange(mob, targets, followRange);
        if (!NpcProfiler.server())
            return super.createPath(targets, regionOffset, offsetUpward, accuracy, range);
        long started = System.nanoTime();
        Path found = super.createPath(targets, regionOffset, offsetUpward, accuracy, range);
        double nearest = Double.MAX_VALUE;
        for (BlockPos target : targets)
            nearest = Math.min(nearest, target.distToCenterSqr(mob.getX(), mob.getY(), mob.getZ()));
        NpcProfiler.pathSearch(level.getGameTime(), mob.getName().getString(), mob.getBlockX(), mob.getBlockY(), mob.getBlockZ(), targets.isEmpty() ? 0D : Math.sqrt(nearest),
            System.nanoTime() - started, found != null);
        return found;
    }
}
