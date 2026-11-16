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
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

import java.util.Optional;

/**
 * Custom NPCs' path node evaluator.
 *
 * <ul>
 * <li>Terrain profile: a ground vehicle does not enter water deeper than it wades, and a watercraft accepts nothing but
 * water deep enough to float in.</li>
 * <li>Driveable paths: an NPC at the wheel of a Flan's vehicle or mecha searches a path as wide and high as the driveable,
 * rather than one it would fit along on foot, and climbs onto walls and ledges only where it cannot drive around them.</li>
 * <li>Vehicle paths: an NPC driving as a ground vehicle model searches a path its hull fits along on either side of it,
 * takes no step higher than the vehicle climbs, and pays for steep grades and soft ground what they slow it down by; see
 * {@link NpcVehiclePaths}. A worker search is handed the profile the server thread read.</li>
 * </ul>
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
    /** How the current search routes a ground vehicle model; null for any other searcher. */
    @Unique
    @Nullable
    private NpcVehiclePaths.Profile wolffsmodnpcsVehiclePath;
    /** Whether the profile was handed over for a worker search, which then does not read the NPC itself. */
    @Unique
    private boolean wolffsmodnpcsVehiclePathPreset;
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

    @Inject(method = {"getCachedBlockType", "m_77567_"}, at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("ConstantValue") // IntelliJ trusts the region's declaration, which wolffsmodnpcsOutsideSearch explains.
    private void wolffsmodnpcsTerrainProfile(Mob mob, int x, int y, int z, CallbackInfoReturnable<BlockPathTypes> callback)
    {
        BlockPathTypes type = callback.getReturnValue();
        if (wolffsmodnpcsOutsideSearch() || type == null)
            return;
        BlockPathTypes filtered = NpcMobility.filter(mob, level, x, y, z, type);
        if (wolffsmodnpcsHullBlocked(x, y, z, filtered))
            filtered = BlockPathTypes.BLOCKED;
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
        Optional<NpcDrivers.PathSize> driven = NpcDrivers.pathSize(mob);
        wolffsmodnpcsDriveable = driven.isPresent();
        driven.ifPresent(size ->
        {
            entityWidth = Math.max(entityWidth, size.width());
            entityDepth = Math.max(entityDepth, size.width());
            entityHeight = Math.max(entityHeight, size.height());
        });
        wolffsmodnpcsHullRoom.clear();
        if (!wolffsmodnpcsVehiclePathPreset)
            wolffsmodnpcsVehiclePath = wolffsmodnpcsDriveable ? null : NpcVehiclePaths.of(mob).orElse(null);
        NpcVehiclePaths.Profile profile = wolffsmodnpcsVehiclePath;
        if (profile != null)
        {
            entityHeight = Math.max(entityHeight, profile.height());
            wolffsmodnpcsStartX = mob.getBlockX();
            wolffsmodnpcsStartZ = mob.getBlockZ();
        }
    }

    /**
     * Makes a driveable's path step up onto a wall or ledge only where there is no way around: a climb onto ground that
     * drops away again within {@value NpcVehiclePaths#RIDGE_REACH} blocks ahead costs as much as a detour of
     * {@value NpcVehiclePaths#RIDGE_MALUS} blocks.
     */
    @Override
    public int getNeighbors(@NotNull Node[] neighbors, @NotNull Node node)
    {
        int count = super.getNeighbors(neighbors, node);
        NpcVehiclePaths.Profile profile = wolffsmodnpcsVehiclePath;
        if (profile != null)
            return wolffsmodnpcsVehicleNeighbors(profile, neighbors, node, count);
        if (!wolffsmodnpcsDriveable)
            return count;
        for (int index = 0; index < count; index++)
        {
            Node next = neighbors[index];
            if (next != null && next.y > node.y && NpcVehiclePaths.ridge(this::wolffsmodnpcsSolid, next.x, next.y, next.z, Integer.signum(next.x - node.x), Integer.signum(next.z - node.z)))
                next.costMalus = Math.max(next.costMalus, NpcVehiclePaths.RIDGE_MALUS);
        }
        return count;
    }

    // Vehicle paths

    @Override
    @Nullable
    public NpcVehiclePaths.Profile wolffsmodnpcsVehiclePath()
    {
        return wolffsmodnpcsVehiclePath;
    }

    @Override
    public void wolffsmodnpcsPresetVehiclePath(@Nullable NpcVehiclePaths.Profile profile)
    {
        wolffsmodnpcsVehiclePath = profile;
        wolffsmodnpcsVehiclePathPreset = true;
    }

    /** @return whether a position the search would otherwise stand the NPC on leaves its vehicle's hull no room */
    @Unique
    private boolean wolffsmodnpcsHullBlocked(int x, int y, int z, BlockPathTypes type)
    {
        NpcVehiclePaths.Profile profile = wolffsmodnpcsVehiclePath;
        // Open air is dropped through to the ground below, which is asked about in its turn.
        if (profile == null || type == BlockPathTypes.BLOCKED || type == BlockPathTypes.OPEN)
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
        float under = FlansEntityTypes.getSurfaceSpeedFactor(profile.surface(), level.getBlockState(wolffsmodnpcsProbe.set(node.x, node.y - 1, node.z)));
        float in = FlansEntityTypes.getSurfaceSpeedFactor(profile.surface(), level.getBlockState(wolffsmodnpcsProbe.set(node.x, node.y, node.z)));
        return NpcVehiclePaths.surfaceCost(Math.min(under, in));
    }
}
