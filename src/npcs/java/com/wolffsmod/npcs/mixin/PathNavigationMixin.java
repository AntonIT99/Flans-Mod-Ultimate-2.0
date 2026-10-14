package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.diag.NpcProfiler;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Set;

/**
 * Every navigator an NPC may use: Custom NPCs' own on the ground, vanilla's in the air and in water.
 *
 * <ul>
 * <li>Diagnostics: while a benchmark or the profiler measures, every path search of an NPC is timed, the failed ones are
 * counted and the slow ones are recorded. A call that keeps its path or may not search is not a search.</li>
 * </ul>
 */
@Mixin(PathNavigation.class)
@SuppressWarnings("AddedMixinMembersNamePattern")
public abstract class PathNavigationMixin
{
    @Unique
    private static final String WOLFFSMODNPCS_CREATE_PATH = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;";

    @Shadow @Final
    protected Mob mob;
    @Shadow @Final
    protected Level level;
    /** Zero doubles as "not measuring": the clock never reads it in practice. */
    @Unique
    private long wolffsmodnpcsSearchStarted;

    // Diagnostics

    @Inject(method = WOLFFSMODNPCS_CREATE_PATH, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/pathfinder/PathFinder;findPath(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;Ljava/util/Set;FIF)Lnet/minecraft/world/level/pathfinder/Path;"))
    private void wolffsmodnpcsSearchStarts(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, CallbackInfoReturnable<Path> callback)
    {
        wolffsmodnpcsSearchStarted = NpcProfiler.server() && mob instanceof EntityNPCInterface ? System.nanoTime() : 0L;
    }

    @Inject(method = WOLFFSMODNPCS_CREATE_PATH, at = @At("RETURN"))
    private void wolffsmodnpcsSearchEnds(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, CallbackInfoReturnable<Path> callback)
    {
        long started = wolffsmodnpcsSearchStarted;
        if (started == 0L)
            return;
        wolffsmodnpcsSearchStarted = 0L;
        long nanos = System.nanoTime() - started;
        double nearest = Double.MAX_VALUE;
        for (BlockPos target : targets)
            nearest = Math.min(nearest, target.distToCenterSqr(mob.getX(), mob.getY(), mob.getZ()));
        NpcProfiler.pathSearch(level.getGameTime(), mob.getName().getString(), mob.getBlockX(), mob.getBlockY(), mob.getBlockZ(), targets.isEmpty() ? 0D : Math.sqrt(nearest), nanos,
            callback.getReturnValue() != null);
    }
}
