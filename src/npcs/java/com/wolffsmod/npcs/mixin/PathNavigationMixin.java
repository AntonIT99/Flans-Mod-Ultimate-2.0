package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.diag.NpcBudgets;
import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.diag.NpcWatch;
import com.wolffsmod.npcs.sim.NpcFlightPaths;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcPursuit;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

import java.util.Set;

/**
 * Every navigator an NPC may use: Custom NPCs' own on the ground, vanilla's in the air and in water.
 *
 * <ul>
 * <li>Diagnostics: while a benchmark or the profiler measures, every path search of an NPC is timed, the failed ones are
 * counted and the slow ones are recorded; an NPC whose live statistics an editor shows has its own searches counted. A
 * call that keeps its path or may not search is not a search.</li>
 * <li>Limits: with a path search limit set for the whole server, an NPC search over it finds no path this time and is
 * tried again later; one that has waited a second goes ahead regardless.</li>
 * <li>Aircraft: a plane, and a helicopter that sees its goal, take a straight path to it instead of searching, which is
 * not a search either; see {@link NpcFlightPaths}.</li>
 * <li>Efficient path searches in the air: as on the ground, a search reaches only as far as its goal plus the detour
 * margin, and a pursuit keeps its path for the retry interval. Custom NPCs' own ground navigator does this itself.</li>
 * </ul>
 */
@Mixin(PathNavigation.class)
// The (Object) casts are required: the mixin class itself is unrelated to the navigator subclasses until merged, which
// IntelliJ takes for an instanceof that can never hold.
@SuppressWarnings({"AddedMixinMembersNamePattern", "java:S1905", "ConstantValue"})
public abstract class PathNavigationMixin
{
    @Unique
    private static final String WOLFFSMODNPCS_CREATE_PATH = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;";
    @Unique
    private static final String WOLFFSMODNPCS_FIND_PATH = "Lnet/minecraft/world/level/pathfinder/PathFinder;findPath(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;Ljava/util/Set;FIF)Lnet/minecraft/world/level/pathfinder/Path;";

    @Shadow @Final
    protected Mob mob;
    @Shadow @Final
    protected Level level;
    @Shadow
    protected double speedModifier;
    @Shadow @Nullable
    protected Path path;
    /** Zero doubles as "not measuring": the clock never reads it in practice. */
    @Unique
    private long wolffsmodnpcsSearchStarted;
    /** Game time a path search was first put off by the server's limit, or {@link Long#MIN_VALUE} while none waits. */
    @Unique
    private long wolffsmodnpcsPathWaitingSince = Long.MIN_VALUE;
    /** Created on the first pursuit of an NPC in the air, so that other mobs' navigators carry none. */
    @Unique @Nullable
    private NpcPursuit wolffsmodnpcsPursuit;

    // Diagnostics

    @Inject(method = WOLFFSMODNPCS_CREATE_PATH, at = @At(value = "INVOKE", target = WOLFFSMODNPCS_FIND_PATH))
    private void wolffsmodnpcsSearchStarts(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, CallbackInfoReturnable<Path> callback)
    {
        boolean measured = !level.isClientSide && mob instanceof EntityNPCInterface && (NpcProfiler.server() || NpcWatch.watched(mob));
        wolffsmodnpcsSearchStarted = measured ? System.nanoTime() : 0L;
    }

    @Inject(method = WOLFFSMODNPCS_CREATE_PATH, at = @At("RETURN"))
    private void wolffsmodnpcsSearchEnds(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, CallbackInfoReturnable<Path> callback)
    {
        long started = wolffsmodnpcsSearchStarted;
        if (started == 0L)
            return;
        wolffsmodnpcsSearchStarted = 0L;
        long nanos = System.nanoTime() - started;
        Path found = callback.getReturnValue();
        NpcWatch.pathSearch(mob, nanos, found == null ? 0 : found.getNodeCount());
        if (!NpcProfiler.server())
            return;
        double nearest = Double.MAX_VALUE;
        for (BlockPos target : targets)
            nearest = Math.min(nearest, target.distToCenterSqr(mob.getX(), mob.getY(), mob.getZ()));
        NpcProfiler.pathSearch(level.getGameTime(), mob.getName().getString(), mob.getBlockX(), mob.getBlockY(), mob.getBlockZ(), targets.isEmpty() ? 0D : Math.sqrt(nearest), nanos, found != null);
    }

    // Limits

    @Redirect(method = WOLFFSMODNPCS_CREATE_PATH, at = @At(value = "INVOKE", target = WOLFFSMODNPCS_FIND_PATH))
    private Path wolffsmodnpcsLimitPathSearches(PathFinder finder, PathNavigationRegion region, Mob searcher, Set<BlockPos> targets, float followRange, int accuracy, float maxVisitedNodes)
    {
        if (searcher instanceof EntityNPCInterface && !level.isClientSide && level.getServer() != null)
        {
            Path direct = NpcFlightPaths.direct(searcher, targets);
            if (direct != null)
            {
                wolffsmodnpcsSearchStarted = 0L;
                return direct;
            }
            long now = level.getGameTime();
            long waited = wolffsmodnpcsPathWaitingSince == Long.MIN_VALUE ? 0L : now - wolffsmodnpcsPathWaitingSince;
            if (!NpcBudgets.pathSearch(level.getServer(), waited))
            {
                if (wolffsmodnpcsPathWaitingSince == Long.MIN_VALUE)
                    wolffsmodnpcsPathWaitingSince = now;
                // Not a search: nothing is timed or counted for it.
                wolffsmodnpcsSearchStarted = 0L;
                return null;
            }
            wolffsmodnpcsPathWaitingSince = Long.MIN_VALUE;
        }
        return finder.findPath(region, searcher, targets, followRange, accuracy, maxVisitedNodes);
    }

    // Efficient path searches in the air

    @ModifyVariable(method = WOLFFSMODNPCS_CREATE_PATH, at = @At("HEAD"), argsOnly = true)
    private float wolffsmodnpcsAirSearchRange(float followRange, Set<BlockPos> targets)
    {
        return wolffsmodnpcsFliesNpc() ? NpcMobility.searchRange(mob, targets, followRange) : followRange;
    }

    @Inject(method = "moveTo(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsAirPursuit(Entity entity, double speed, CallbackInfoReturnable<Boolean> callback)
    {
        if (!wolffsmodnpcsFliesNpc())
            return;
        if (wolffsmodnpcsPursuit == null)
            wolffsmodnpcsPursuit = new NpcPursuit();
        if (wolffsmodnpcsPursuit.keeps(mob, entity, level.getGameTime(), path != null && !path.isDone()))
        {
            speedModifier = speed;
            callback.setReturnValue(true);
        }
    }

    /** @return whether this is the navigator of an NPC in the air, on the server */
    @Unique
    private boolean wolffsmodnpcsFliesNpc()
    {
        return (Object) this instanceof FlyingPathNavigation && mob instanceof EntityNPCInterface && !level.isClientSide;
    }
}
