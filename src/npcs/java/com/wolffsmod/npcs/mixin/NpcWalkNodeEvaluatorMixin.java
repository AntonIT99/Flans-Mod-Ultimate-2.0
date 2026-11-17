package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcVehiclePaths;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import noppes.npcs.ai.NpcWalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

import java.util.List;
import java.util.Optional;

/**
 * Custom NPCs' path node evaluator.
 *
 * <ul>
 * <li>Terrain profile: a ground vehicle does not enter water deeper than it wades, and a watercraft accepts nothing but
 * water deep enough to float in.</li>
 * <li>Driveable paths: an NPC at the wheel of a Flan's vehicle or mecha searches a path as wide and high as the driveable,
 * rather than one it would fit along on foot, and climbs onto walls and ledges only where it cannot drive around them. A
 * ground vehicle pays for steep grades and soft ground what they slow it down by, as it would under a player.</li>
 * <li>Vehicle paths: an NPC driving as a ground vehicle model searches a path its hull fits along on either side of it,
 * takes no step higher than the vehicle climbs, and pays for steep grades and soft ground what they slow it down by; see
 * {@link NpcVehiclePaths}.</li>
 * <li>Stuck places: a path keeps clear of where the vehicle lately got stuck, where it can, so that the search after
 * backing up finds the way round instead of the same path again.</li>
 * </ul>
 *
 * <p>
 * A worker search is handed the routing the server thread read.
 * </p>
 */
@Mixin(value = NpcWalkNodeEvaluator.class, remap = false)
@SuppressWarnings({"UnresolvedMixinReference", "AddedMixinMembersNamePattern"})
public abstract class NpcWalkNodeEvaluatorMixin extends WalkNodeEvaluator implements NpcVehiclePaths.Evaluator
{
    @Unique
    private static final byte WOLFFSMODNPCS_HULL_FITS = 1;
    @Unique
    private static final byte WOLFFSMODNPCS_HULL_BLOCKED = 2;

    /** Whether the current search is for a driveable, read once as it is prepared. */
    @Unique
    private boolean wolffsmodnpcsDriveable;
    /** How the current search routes a vehicle: a model's profile, a driven vehicle's costs and the places it got stuck at. */
    @Unique
    private NpcVehiclePaths.Routing wolffsmodnpcsRouting = NpcVehiclePaths.Routing.NONE;
    /** Whether the routing was handed over for a worker search, which then does not read the NPC itself. */
    @Unique
    private boolean wolffsmodnpcsRoutingPreset;
    /** Where the search started: the hull may overlap blocks around it, so that a vehicle against a wall can drive off. */
    @Unique
    private int wolffsmodnpcsStartX;
    @Unique
    private int wolffsmodnpcsStartZ;
    /** Whether the hull fits at the positions asked about since the search was prepared; a position costs dozens of block reads. */
    @Unique
    private final Long2ByteOpenHashMap wolffsmodnpcsHullRoom = new Long2ByteOpenHashMap();
    @Unique
    private final BlockPos.MutableBlockPos wolffsmodnpcsProbe = new BlockPos.MutableBlockPos();

    // Terrain profile

    @Inject(method = "getCachedBlockType", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("ConstantValue") // The pathfinding context is null between searches, whatever its declaration promises.
    private void wolffsmodnpcsTerrainProfile(Mob mob, int x, int y, int z, CallbackInfoReturnable<PathType> callback)
    {
        PathType type = callback.getReturnValue();
        if (wolffsmodnpcsOutsideSearch() || type == null)
            return;
        PathType filtered = NpcMobility.filter(mob, currentContext.level(), x, y, z, type);
        if (wolffsmodnpcsHullBlocked(x, y, z, filtered))
            filtered = PathType.BLOCKED;
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

    // Driveable paths

    @Override
    public void prepare(@NotNull PathNavigationRegion region, @NotNull Mob mob)
    {
        super.prepare(region, mob);
        Optional<NpcDrivers.PathSize> driven = NpcDrivers.pathSize(mob);
        wolffsmodnpcsDriveable = driven.isPresent();
        driven.ifPresent(size ->
        {
            entityWidth = Math.max(entityWidth, size.width());
            entityDepth = Math.max(entityDepth, size.width());
            entityHeight = Math.max(entityHeight, size.height());
        });
        wolffsmodnpcsHullRoom.clear();
        if (!wolffsmodnpcsRoutingPreset)
            wolffsmodnpcsRouting = NpcVehiclePaths.routing(mob, wolffsmodnpcsDriveable);
        NpcVehiclePaths.Profile profile = wolffsmodnpcsRouting.model();
        if (profile != null)
        {
            entityHeight = Math.max(entityHeight, profile.height());
            wolffsmodnpcsStartX = mob.getBlockX();
            wolffsmodnpcsStartZ = mob.getBlockZ();
        }
    }

    @Override
    public int getNeighbors(@NotNull Node[] neighbors, @NotNull Node node)
    {
        int count = super.getNeighbors(neighbors, node);
        NpcVehiclePaths.Routing routing = wolffsmodnpcsRouting;
        if (routing.model() != null)
            count = wolffsmodnpcsVehicleNeighbors(routing.model(), neighbors, node, count);
        else if (wolffsmodnpcsDriveable)
            wolffsmodnpcsDriveableNeighbors(routing.driven(), neighbors, node, count);
        if (!routing.stuck().isEmpty())
            wolffsmodnpcsAvoidStuckPlaces(routing.stuck(), neighbors, count);
        return count;
    }

    /**
     * Makes a driveable's path step up onto a wall or ledge only where there is no way around: a climb onto ground that
     * drops away again within {@value NpcVehiclePaths#RIDGE_REACH} blocks ahead costs as much as a detour of
     * {@value NpcVehiclePaths#RIDGE_MALUS} blocks. A ground vehicle's path is also as dear as the grade up to a node and
     * the ground on it slow that vehicle down, so that it takes the road round a steep hill or a bog where one leads on.
     *
     * @param costs
     *            what grades and soft ground cost the vehicle; null for a driveable they do not slow
     */
    @Unique
    private void wolffsmodnpcsDriveableNeighbors(@Nullable NpcVehiclePaths.Profile costs, Node[] neighbors, Node node, int count)
    {
        double floor = costs == null ? 0D : getFloorLevel(wolffsmodnpcsProbe.set(node.x, node.y, node.z));
        for (int index = 0; index < count; index++)
        {
            Node next = neighbors[index];
            if (next == null)
                continue;
            float cost = costs == null ? 0F : wolffsmodnpcsSurfaceCost(costs, next);
            if (next.y > node.y)
                cost = wolffsmodnpcsClimbCost(costs, node, next, floor, cost);
            if (cost > 0F)
                next.costMalus = Math.max(next.costMalus, cost);
        }
    }

    /**
     * @param floor
     *            level of the ground at the node climbed from
     * @param cost
     *            what the ground on the node climbed onto costs
     * @return that cost with the grade up to the node added, and no less than a wall or ledge costs where it is one
     */
    @Unique
    private float wolffsmodnpcsClimbCost(@Nullable NpcVehiclePaths.Profile costs, Node node, Node next, double floor, float cost)
    {
        int dx = Integer.signum(next.x - node.x);
        int dz = Integer.signum(next.z - node.z);
        float climbed = cost;
        if (costs != null)
            climbed += costs.gradeCost(costs.grade(this::wolffsmodnpcsSolid, next.x, next.y, next.z, dx, dz, getFloorLevel(wolffsmodnpcsProbe.set(next.x, next.y, next.z)) - floor));
        return NpcVehiclePaths.ridge(this::wolffsmodnpcsSolid, next.x, next.y, next.z, dx, dz) ? Math.max(climbed, NpcVehiclePaths.RIDGE_MALUS) : climbed;
    }

    // Stuck places

    /**
     * Makes the nodes at a place the vehicle got stuck at as dear as a detour of {@value NpcVehiclePaths#STUCK_COST}
     * blocks each. A node stands for the corner of the room searched for, so the middle of that room is what is measured.
     */
    @Unique
    private void wolffsmodnpcsAvoidStuckPlaces(List<NpcVehiclePaths.StuckPlace> places, Node[] neighbors, int count)
    {
        double middle = entityWidth * 0.5D;
        for (int index = 0; index < count; index++)
        {
            Node next = neighbors[index];
            if (next == null)
                continue;
            float cost = NpcVehiclePaths.stuckCost(places, next.x + middle, next.y, next.z + middle);
            if (cost > 0F)
                next.costMalus = Math.max(next.costMalus, Math.max(0F, mob.getPathfindingMalus(next.type)) + cost);
        }
    }

    // Vehicle paths

    @Override
    public NpcVehiclePaths.Routing wolffsmodnpcsVehicleRouting()
    {
        return wolffsmodnpcsRouting;
    }

    @Override
    public void wolffsmodnpcsPresetVehicleRouting(NpcVehiclePaths.Routing routing)
    {
        wolffsmodnpcsRouting = routing;
        wolffsmodnpcsRoutingPreset = true;
    }

    /** @return whether a position the search would otherwise stand the NPC on leaves its vehicle's hull no room */
    @Unique
    private boolean wolffsmodnpcsHullBlocked(int x, int y, int z, PathType type)
    {
        NpcVehiclePaths.Profile profile = wolffsmodnpcsRouting.model();
        // Open air is dropped through to the ground below, which is asked about in its turn.
        if (profile == null || type == PathType.BLOCKED || type == PathType.OPEN)
            return false;
        int allowance = profile.startAllowance();
        if (Math.abs(x - wolffsmodnpcsStartX) <= allowance && Math.abs(z - wolffsmodnpcsStartZ) <= allowance)
            return false;
        long key = BlockPos.asLong(x, y, z);
        byte known = wolffsmodnpcsHullRoom.get(key);
        if (known == 0)
        {
            known = profile.blocked(x, y, z, entityWidth, this::wolffsmodnpcsSolid) ? WOLFFSMODNPCS_HULL_BLOCKED : WOLFFSMODNPCS_HULL_FITS;
            wolffsmodnpcsHullRoom.put(key, known);
        }
        return known == WOLFFSMODNPCS_HULL_BLOCKED;
    }

    @Unique
    private boolean wolffsmodnpcsSolid(int x, int y, int z)
    {
        wolffsmodnpcsProbe.set(x, y, z);
        CollisionGetter level = currentContext.level();
        return !level.getBlockState(wolffsmodnpcsProbe).getCollisionShape(level, wolffsmodnpcsProbe).isEmpty();
    }

    /**
     * Drops the neighbours the vehicle cannot drive up onto, and makes the others as dear as the grade up to them and the
     * ground on them slow the vehicle down. A step up onto a wall or ledge, which the hull would hang over, costs a
     * detour of {@value NpcVehiclePaths#RIDGE_MALUS} blocks besides, as it does a driven vehicle. A node keeps the
     * dearest way onto it, as it does its other costs.
     *
     * @return how many neighbours are left
     */
    @Unique
    private int wolffsmodnpcsVehicleNeighbors(NpcVehiclePaths.Profile profile, Node[] neighbors, Node node, int count)
    {
        double floor = getFloorLevel(wolffsmodnpcsProbe.set(node.x, node.y, node.z));
        int kept = 0;
        for (int index = 0; index < count; index++)
        {
            Node next = neighbors[index];
            double rise = getFloorLevel(wolffsmodnpcsProbe.set(next.x, next.y, next.z)) - floor;
            if (!profile.climbs(rise))
                continue;
            float cost = wolffsmodnpcsSurfaceCost(profile, next);
            if (rise > 0D)
            {
                int dx = Integer.signum(next.x - node.x);
                int dz = Integer.signum(next.z - node.z);
                cost += profile.gradeCost(profile.grade(this::wolffsmodnpcsSolid, next.x, next.y, next.z, dx, dz, rise));
                if (next.y > node.y && NpcVehiclePaths.ridge(this::wolffsmodnpcsSolid, next.x, next.y, next.z, dx, dz))
                    cost = Math.max(cost, NpcVehiclePaths.RIDGE_MALUS);
            }
            if (cost > 0F)
                next.costMalus = Math.max(next.costMalus, Math.max(0F, mob.getPathfindingMalus(next.type)) + cost);
            neighbors[kept++] = next;
        }
        return kept;
    }

    /** @return what the ground of a node costs: the block stood on, or one stood in, such as a layer of snow */
    @Unique
    private float wolffsmodnpcsSurfaceCost(NpcVehiclePaths.Profile profile, Node node)
    {
        if (profile.surface() == null)
            return 0F;
        float under = FlansEntityTypes.getSurfaceSpeedFactor(profile.surface(), currentContext.getBlockState(wolffsmodnpcsProbe.set(node.x, node.y - 1, node.z)));
        float in = FlansEntityTypes.getSurfaceSpeedFactor(profile.surface(), currentContext.getBlockState(wolffsmodnpcsProbe.set(node.x, node.y, node.z)));
        return NpcVehiclePaths.surfaceCost(Math.min(under, in));
    }
}
