package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.api.driveables.HullFootprint;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

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

    /** A node evaluator that searches by a vehicle profile, as the mixin makes Custom NPCs' one. */
    public interface Evaluator
    {
        /** @return the profile of the search last prepared; null when it was no ground vehicle model's */
        @Nullable
        Profile wolffsmodnpcsVehiclePath();

        /**
         * Hands the evaluator of a worker search the profile read on the server thread, so that the worker does not read
         * the NPC's settings and model while the server may be changing them.
         */
        void wolffsmodnpcsPresetVehiclePath(@Nullable Profile profile);
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

    static Profile profile(double halfWidth, double height, OptionalDouble step, @Nullable GroundPropulsion slope, @Nullable IContentType surface)
    {
        double limit = step.isPresent() ? step.getAsDouble() : Double.POSITIVE_INFINITY;
        return new Profile(Mth.clamp(halfWidth, 0D, MAX_HALF_WIDTH), Mth.clamp(Mth.floor(height + 1D), 1, MAX_HEIGHT), limit, step.isPresent() ? Math.max(1, Mth.floor(limit)) : 1, slope, surface);
    }
}
