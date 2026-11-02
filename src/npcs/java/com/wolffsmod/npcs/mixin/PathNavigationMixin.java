package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.diag.NpcBudgets;
import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.diag.NpcWatch;
import com.wolffsmod.npcs.sim.NpcFlightPaths;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcPathWorkers;
import com.wolffsmod.npcs.sim.NpcPursuit;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
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
 * <li>Worker searches: with {@link NpcPathWorkers} on, an NPC search is handed to a worker thread and its path taken up
 * on a later tick, so the NPC keeps the path it has instead of the server thread waiting for a new one. At most one
 * search of a navigator runs at a time, and one whose worker failed is searched on the server thread instead.</li>
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
    /**
     * This navigator's search on a worker thread, or null while none runs. At most one, so its path finder and node
     * evaluator are never used by two threads at once.
     */
    @Unique @Nullable
    private NpcPathWorkers.Search wolffsmodnpcsPathSearch;
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
        MinecraftServer server = level.getServer();
        if (!(searcher instanceof EntityNPCInterface) || level.isClientSide || server == null)
            return finder.findPath(region, searcher, targets, followRange, accuracy, maxVisitedNodes);
        Path direct = NpcFlightPaths.direct(searcher, targets);
        if (direct != null)
            return wolffsmodnpcsUntimed(direct);
        // A pursuit result must never become the path of a retreat requested while that search was running.
        if (wolffsmodnpcsPathSearch != null)
            return wolffsmodnpcsWorkerSearch(finder, region, searcher, targets, followRange, accuracy, maxVisitedNodes);
        if (!wolffsmodnpcsMaySearch(server))
            return wolffsmodnpcsUntimed(null);
        wolffsmodnpcsPathSearch = NpcPathWorkers.submit(finder, region, searcher, targets, followRange, accuracy, maxVisitedNodes);
        // A search handed to a worker is timed and counted when it is taken up, not now.
        return wolffsmodnpcsPathSearch != null ? wolffsmodnpcsUntimed(null) : finder.findPath(region, searcher, targets, followRange, accuracy, maxVisitedNodes);
    }

    /** Marks the call as no path search of this tick, so that nothing is timed, counted or recorded for it. */
    @Unique
    @Nullable
    private Path wolffsmodnpcsUntimed(@Nullable Path path)
    {
        wolffsmodnpcsSearchStarted = 0L;
        return path;
    }

    /**
     * @return the path of this navigator's worker search, null while it is still running and the NPC keeps the path it
     *         has, or a fresh search on the server thread when the worker failed
     */
    // Mirrors the path finder's own call, whose arguments a server-thread search has to pass on unchanged.
    @SuppressWarnings("java:S107")
    @Unique
    @Nullable
    private Path wolffsmodnpcsWorkerSearch(PathFinder finder, PathNavigationRegion region, Mob searcher, Set<BlockPos> targets, float followRange, int accuracy, float maxVisitedNodes)
    {
        NpcPathWorkers.Search running = wolffsmodnpcsPathSearch;
        if (running == null || !running.done())
            return wolffsmodnpcsUntimed(null);
        wolffsmodnpcsPathSearch = null;
        boolean precise = searcher instanceof com.wolffsmod.npcs.tactics.NpcTacticState.Holder holder
            && holder.wolffsmodnpcsTacticState().getSafety().getAction() != com.wolffsmod.npcs.tactics.NpcSafetyState.Action.NONE;
        if (!running.matches(targets, precise))
        {
            wolffsmodnpcsPathSearch = NpcPathWorkers.submit(finder, region, searcher, targets, followRange, accuracy, maxVisitedNodes);
            return wolffsmodnpcsPathSearch == null ? finder.findPath(region, searcher, targets, followRange, accuracy, maxVisitedNodes) : wolffsmodnpcsUntimed(null);
        }
        // A worker that failed leaves its search to the server thread, which is timed and counted as usual.
        return running.failed() ? finder.findPath(region, searcher, targets, followRange, accuracy, maxVisitedNodes) : wolffsmodnpcsUntimed(running.path());
    }

    /**
     * @return whether the server's path search limit lets this navigator search now; one that may not remembers since when
     *         it waits, and goes ahead once it has waited a second
     */
    @Unique
    private boolean wolffsmodnpcsMaySearch(MinecraftServer server)
    {
        long now = level.getGameTime();
        long waited = wolffsmodnpcsPathWaitingSince == Long.MIN_VALUE ? 0L : now - wolffsmodnpcsPathWaitingSince;
        if (NpcBudgets.pathSearch(server, waited))
        {
            wolffsmodnpcsPathWaitingSince = Long.MIN_VALUE;
            return true;
        }
        if (wolffsmodnpcsPathWaitingSince == Long.MIN_VALUE)
            wolffsmodnpcsPathWaitingSince = now;
        return false;
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
