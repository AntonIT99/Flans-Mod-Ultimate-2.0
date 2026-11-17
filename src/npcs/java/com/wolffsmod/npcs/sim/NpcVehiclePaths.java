package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.api.driveables.HullFootprint;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * What a path search has to know to route an NPC that drives as a ground vehicle model as the vehicle it shows, rather
 * than as the walker its hitbox makes it: the room its hull needs, the steps it climbs, and what grades and soft ground
 * cost it. The decisions are kept free of the level, which the node evaluator reads for them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcVehiclePaths
{
    /** Blocks from the middle of the hull a search checks at most, as wide as the path of a driven vehicle. */
    static final double MAX_HALF_WIDTH = 2.5D;
    /** Blocks of height a search checks at most. */
    static final int MAX_HEIGHT = 3;
    /** Blocks beyond a step up that the ground is followed over to tell a slope from a single step. */
    static final int GRADE_REACH = 3;
    /** Most a grade or the surface adds to the cost of one node, in blocks of detour, so that a path still crosses them where nothing else leads on. */
    static final float MAX_GRADE_COST = 8F;
    static final float MAX_SURFACE_COST = 4F;
    /** Blocks ahead of a step up within which ground dropping away again makes it a wall or ledge rather than a slope. */
    static final int RIDGE_REACH = 2;
    /** Extra path cost of climbing onto a wall or ledge, in blocks of detour, so that a vehicle goes round one where it can. */
    public static final float RIDGE_MALUS = 12F;
    /** Share of its speed below which a vehicle is costed as if it kept this much. */
    private static final double MIN_SHARE = 0.1D;
    private static final double STEP_TOLERANCE = 1.0E-3D;
    /**
     * Extra path cost of a node at a place the vehicle got stuck at, in blocks of detour: a search goes round the place
     * where it can and still leads through it where nothing else leads on.
     */
    public static final float STUCK_COST = 16F;
    /** Ticks a search keeps clear of such a place. */
    static final int STUCK_TICKS = 600;
    /** Blocks around the place a search keeps clear of at least, and blocks above and below it that it reaches. */
    static final double MIN_STUCK_RADIUS = 2D;
    static final double STUCK_HEIGHT = 3D;
    /** Places a vehicle remembers at most; the oldest is forgotten for a new one. */
    static final int MAX_STUCK_PLACES = 4;

    /** A node evaluator that searches by a vehicle's routing, as the mixin makes Custom NPCs' one. */
    public interface Evaluator
    {
        /** @return the routing of the search last prepared */
        Routing wolffsmodnpcsVehicleRouting();

        /**
         * Hands the evaluator of a worker search the routing read on the server thread, so that the worker does not read
         * the NPC's settings, model and memory while the server may be changing them.
         */
        void wolffsmodnpcsPresetVehicleRouting(Routing routing);
    }

    /**
     * What a search is told about the vehicle it routes. Immutable, so that a worker thread may search by it.
     *
     * @param model
     *            how a ground vehicle model is routed; null for any other searcher
     * @param driven
     *            what grades and soft ground cost the ground vehicle the NPC sits at the wheel of; null while it drives none
     * @param stuck
     *            the places the vehicle got stuck at, which its path keeps clear of where it can
     */
    public record Routing(@Nullable Profile model, @Nullable Profile driven, List<StuckPlace> stuck)
    {
        public static final Routing NONE = new Routing(null, null, List.of());
    }

    /**
     * A place a vehicle ran into something its path search took for clear: a wreck, a parked vehicle, a gap narrower than
     * it looked.
     *
     * @param radius
     *            blocks around the place, across the ground, that a path keeps clear of
     * @param until
     *            game time from which the place is forgotten
     */
    public record StuckPlace(double x, double y, double z, double radius, long until)
    {
        /** @return whether a vehicle whose middle is at the position is at the place */
        public boolean covers(double atX, double atY, double atZ)
        {
            double dx = atX - x;
            double dz = atZ - z;
            return Math.abs(atY - y) <= STUCK_HEIGHT && dx * dx + dz * dz <= radius * radius;
        }
    }

    /**
     * Where a vehicle got stuck lately. Backing up and searching the path again finds the same path, since the search
     * sees nothing of what stopped the vehicle; told of the place, it finds the way round. One per NPC, server thread.
     */
    public static final class StuckPlaces
    {
        private final List<StuckPlace> places = new ArrayList<>();

        /** Remembers a place; one already known there is remembered anew instead. */
        public void add(double x, double y, double z, double radius, long now)
        {
            places.removeIf(place -> place.until() <= now || place.covers(x, y, z));
            if (places.size() >= MAX_STUCK_PLACES)
                places.remove(0);
            places.add(new StuckPlace(x, y, z, Math.max(MIN_STUCK_RADIUS, radius), now + STUCK_TICKS));
        }

        /** @return the places still remembered, as a list of its own that a worker thread may read */
        public List<StuckPlace> current(long now)
        {
            places.removeIf(place -> place.until() <= now);
            return places.isEmpty() ? List.of() : List.copyOf(places);
        }
    }

    /** @return blocks of detour that a node with the vehicle's middle at the position costs for lying at a place it got stuck at */
    public static float stuckCost(List<StuckPlace> places, double x, double y, double z)
    {
        for (StuckPlace place : places)
        {
            if (place.covers(x, y, z))
                return STUCK_COST;
        }
        return 0F;
    }

    /** Whether a block stops a hull: the level, as far as these decisions look at it. */
    @FunctionalInterface
    public interface Solids
    {
        boolean solid(int x, int y, int z);
    }

    /**
     * How one NPC's vehicle model is routed. Immutable, so that a worker thread may search by it.
     *
     * @param halfWidth
     *            blocks from the middle of the hull to its side
     * @param height
     *            blocks of height the hull needs
     * @param stepLimit
     *            highest rise of the ground the vehicle drives up, in blocks; infinite for one that jumps like a mob
     * @param climbBlocks
     *            whole blocks the vehicle drives over, which do not block its hull
     * @param slope
     *            how the vehicle bogs down on a grade; null when grades do not slow it
     * @param surface
     *            the definition the ground slows; null when it does not
     */
    public record Profile(double halfWidth, int height, double stepLimit, int climbBlocks, @Nullable GroundPropulsion slope, @Nullable IContentType surface)
    {
        /**
         * @param width
         *            blocks east and south of the position the search already checks for the NPC's own hitbox, which
         *            vanilla centres the NPC on
         * @return whether the hull does not fit with the NPC at the position: something higher than the vehicle drives
         *         over stands within its width, on either side
         */
        public boolean blocked(int x, int y, int z, int width, Solids solids)
        {
            double middle = width * 0.5D;
            int from = Mth.floor(middle - halfWidth);
            int to = Mth.ceil(middle + halfWidth) - 1;
            int top = Math.max(height, climbBlocks + 1);
            for (int dx = from; dx <= to; dx++)
            {
                for (int dz = from; dz <= to; dz++)
                {
                    if (dx >= 0 && dx < width && dz >= 0 && dz < width)
                        continue;
                    for (int dy = climbBlocks; dy < top; dy++)
                    {
                        if (solids.solid(x + dx, y + dy, z + dz))
                            return true;
                    }
                }
            }
            return false;
        }

        /** @return blocks around its start within which a search lets the hull overlap, so that a vehicle standing against a wall can still drive off */
        public int startAllowance()
        {
            return Mth.ceil(halfWidth) + 1;
        }

        /** @return whether the vehicle drives up ground rising so many blocks from one node to the next */
        public boolean climbs(double rise)
        {
            return rise <= stepLimit + STEP_TOLERANCE;
        }

        /** @return blocks of detour that driving a node up a grade of so many degrees is worth: the time the vehicle loses bogging down */
        public float gradeCost(double degrees)
        {
            return slope == null || degrees <= 0D ? 0F : cost(slope.climbFactor(degrees), MAX_GRADE_COST);
        }

        /**
         * A single block up is a curb to a vehicle and a staircase of them a steep hill, though each step looks the same from
         * one node to the next. The ground is therefore followed on in the direction of travel.
         *
         * @param x
         *            the node stepped up onto, where the vehicle's feet are
         * @param dx
         *            direction of travel, each -1, 0 or 1
         * @param firstRise
         *            blocks the ground rose onto that node
         * @return degrees of the grade from the node below over the ground ahead, as far as it keeps level or rising; ground
         *         higher than the vehicle drives up at once is a wall that ends the slope
         */
        public double grade(Solids solids, int x, int y, int z, int dx, int dz, double firstRise)
        {
            if (firstRise <= 0D || dx == 0 && dz == 0)
                return 0D;
            int feet = y;
            int run = 1;
            for (int step = 1; step <= GRADE_REACH; step++)
            {
                int ahead = x + dx * step;
                int across = z + dz * step;
                int up = 0;
                while (up <= climbBlocks && solids.solid(ahead, feet + up, across))
                    up++;
                // A wall ends the slope, and so does ground that drops away.
                if (up > climbBlocks || up == 0 && !solids.solid(ahead, feet - 1, across))
                    break;
                feet += up;
                run++;
            }
            return Math.toDegrees(Math.atan2(firstRise + feet - y, run * Math.sqrt((double) dx * dx + dz * dz)));
        }
    }

    /**
     * A vehicle steps up onto a wall as readily as onto a slope, which look the same from one node to the next; a wall is
     * told by the ground dropping away again behind it, where a hull would hang over the far edge.
     *
     * @param x
     *            the node stepped up onto, where the feet of the vehicle are
     * @param dx
     *            direction of travel, each -1, 0 or 1
     * @return whether the ground under the node drops back down within {@value #RIDGE_REACH} blocks ahead
     */
    public static boolean ridge(Solids solids, int x, int y, int z, int dx, int dz)
    {
        if (dx == 0 && dz == 0)
            return false;
        for (int step = 1; step <= RIDGE_REACH; step++)
        {
            if (!solids.solid(x + dx * step, y - 1, z + dz * step))
                return true;
        }
        return false;
    }

    /** @return blocks of detour that driving a node at a share of the vehicle's speed is worth */
    public static float surfaceCost(float speedFactor)
    {
        return cost(speedFactor, MAX_SURFACE_COST);
    }

    private static float cost(double share, float limit)
    {
        if (Double.isNaN(share) || share >= 1D)
            return 0F;
        return (float) Math.min(limit, 1D / Math.max(share, MIN_SHARE) - 1D);
    }

    /**
     * @return how the searches of this mob are routed: empty for anything but an NPC driving as a ground vehicle model
     *         with vehicle paths on. Reads the NPC's settings and model, so server thread.
     */
    public static Optional<Profile> of(Mob mob)
    {
        if (!(mob instanceof EntityNPCInterface npc) || npc.level().isClientSide)
            return Optional.empty();
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (!options.enabled(Feature.VEHICLE_PATHS) || NpcTuning.of(npc).terrain() == Terrain.WATERCRAFT)
            return Optional.empty();
        NpcVehicleState state = NpcVehicleState.of(npc);
        return state.groundVehicleModel().map(model ->
        {
            double scale = NpcModels.scale(npc);
            // The hull without turret and barrels, side to side: a tank is longer than it is wide, and turns to fit.
            Optional<HullFootprint> hull = FlansEntityTypes.getHullFootprint(model);
            double halfWidth = hull.map(footprint -> footprint.width() * 0.5D * scale).orElseGet(() -> state.reach(npc));
            double height = hull.map(footprint -> footprint.height() * scale).orElse((double) npc.getBbHeight());
            // One that jumps like a mob is held to no step: it gets up what a mob does.
            OptionalDouble step = NpcMobility.keepsWheelsDown(npc) ? NpcMobility.stepHeight(npc) : OptionalDouble.empty();
            GroundPropulsion slope = options.enabled(Feature.SLOPE_PHYSICS) ? NpcDriving.propulsion(npc, options).orElse(null) : null;
            return profile(halfWidth, height, step, slope, options.enabled(Feature.SURFACE_TRACTION) ? model : null);
        });
    }

    /**
     * @param driveable
     *            whether the mob sits at the wheel of a driveable, which is searched for by its size rather than by a model's
     *            profile
     * @return everything the searches of this mob are routed by. Reads the NPC's settings, model, driveable and memory, so
     *         server thread.
     */
    public static Routing routing(Mob mob, boolean driveable)
    {
        if (!(mob instanceof EntityNPCInterface npc) || npc.level().isClientSide)
            return Routing.NONE;
        Profile model = driveable ? null : of(mob).orElse(null);
        Profile driven = driveable ? NpcDrivers.drivenGroundVehicle(npc).map(NpcVehiclePaths::drivenCosts).orElse(null) : null;
        List<StuckPlace> stuck = NpcVehicleState.of(npc).getStuckPlaces().current(npc.level().getGameTime());
        return model == null && driven == null && stuck.isEmpty() ? Routing.NONE : new Routing(model, driven, stuck);
    }

    /**
     * A driven vehicle's path is searched as wide and high as the vehicle, so its profile holds no hull and no step of its
     * own: only what steep grades and soft ground slow that vehicle down by, as they do under a player.
     *
     * @return the costs of the definition; none for an engine of the legacy kind, which no grade slows, and on ground that
     *         does not slow the vehicle
     */
    static Profile drivenCosts(IContentType type)
    {
        return profile(0D, 1D, OptionalDouble.empty(), FlansEntityTypes.getGroundPropulsion(type).orElse(null), type);
    }

    static Profile profile(double halfWidth, double height, OptionalDouble step, @Nullable GroundPropulsion slope, @Nullable IContentType surface)
    {
        double limit = step.isPresent() ? step.getAsDouble() : Double.POSITIVE_INFINITY;
        return new Profile(Mth.clamp(halfWidth, 0D, MAX_HALF_WIDTH), Mth.clamp(Mth.floor(height + 1D), 1, MAX_HEIGHT), limit, step.isPresent() ? Math.max(1, Mth.floor(limit)) : 1, slope, surface);
    }
}
