package com.wolffsmod.npcs.drivers;

import com.wolffsmod.npcs.sim.MountMotion;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Brings a plane down at a site. It flies out to a point some way short of the site, on the heading it will land along,
 * turns in there and glides down a shallow slope to the site at a gentle speed, raises its nose just above the ground
 * with the throttle closed, and brakes once its wheels are down. A plane that overshoots the site goes round again. No
 * world access: the pilot hands in what it sees. One per pilot.
 */
final class PlaneLanding
{
    /** Blocks short of the site at which the final approach begins, and within which that point counts as reached. */
    static final double APPROACH_DISTANCE = 128D;
    static final double FIX_REACHED = 24D;
    /** Blocks above the site's ground a plane flies out to the start of its final approach at. */
    static final double APPROACH_HEIGHT = 20D;
    /** Blocks the glide falls per block flown along it: some 4.5 degrees. */
    static final double GLIDE_SLOPE = 0.08D;
    /** Blocks above its resting height at which a plane flares, and the nose-up it flares with, in degrees. */
    static final double FLARE_HEIGHT = 3D;
    static final float FLARE_PITCH = 4F;
    /** Blocks past the site after which a plane still in the air goes round again. */
    static final double OVERSHOOT = 48D;
    /** Steepest a plane climbs or descends on its glide, in degrees, and the steepest bank it lands with. */
    private static final float MAX_GLIDE_PITCH = 8F;
    private static final float MAX_BANK = 25F;
    private static final float BANK_PER_DEGREE = 1.2F;
    /** Blocks a site may move before the approach to it starts over. */
    private static final double SITE_MOVED = 16D;
    /** Blocks from the site within which a plane stops steering for it and holds its landing heading. */
    private static final double HOLD_HEADING = 12D;
    /** Throttle that holds the approach speed, and how much more per block a tick it is too slow. */
    private static final float GLIDE_THROTTLE = 0.5F;
    private static final float THROTTLE_PER_SPEED = 3F;
    /** Speed under which a plane on the ground has stopped, in blocks a tick. */
    private static final double STOPPED = 0.05D;

    /** How far along its landing a plane is. */
    enum Stage
    {
        APPROACH, FINAL, FLARE, ROLLOUT, LANDED
    }

    @Nullable
    private Stage stage;
    @Nullable
    private Vec3 site;
    /** The heading the plane lands along. */
    private float runway;

    /**
     * What the plane sees.
     *
     * @param heading
     *            where its nose points
     * @param height
     *            blocks its origin is above the ground under it
     * @param siteGround
     *            height of the ground at the site
     * @param resting
     *            blocks its origin is above level ground when it stands on its wheels
     * @param approachSpeed
     *            speed it glides down at, in blocks a tick
     */
    record View(Vec3 position, Vec3 velocity, float heading, boolean grounded, double height, double siteGround, double resting, double approachSpeed)
    {}

    /** What the plane is to do this tick: the attitude, heading and throttle to fly with, and whether to brake. */
    record Step(Stage stage, float pitch, float bank, float heading, float throttle, boolean brake)
    {}

    /** Whether a landing has begun and has not been given up. */
    boolean active()
    {
        return stage != null;
    }

    /** Gives the landing up, for a plane that has somewhere to go again. */
    void abort()
    {
        stage = null;
        site = null;
    }

    /** @return what to do this tick to land at the site */
    Step land(View view, Vec3 at)
    {
        Vec3 current = site;
        if (stage == null || current == null || horizontal(current, at) > SITE_MOVED)
        {
            current = at;
            site = at;
            stage = Stage.APPROACH;
            runway = horizontal(view.position(), at) > APPROACH_DISTANCE ? bearing(view.position(), at) : view.heading();
        }
        if (view.grounded())
        {
            stage = view.velocity().horizontalDistance() < STOPPED ? Stage.LANDED : Stage.ROLLOUT;
            return new Step(stage, 0F, 0F, runway, 0F, true);
        }
        Vec3 along = Vec3.directionFromRotation(0F, runway);
        Vec3 fix = current.subtract(along.scale(APPROACH_DISTANCE));
        double ahead = new Vec3(current.x - view.position().x, 0D, current.z - view.position().z).dot(along);
        if (stage == Stage.APPROACH && horizontal(view.position(), fix) < FIX_REACHED)
            stage = Stage.FINAL;
        else if (stage != Stage.APPROACH && ahead < -OVERSHOOT)
            stage = Stage.APPROACH;
        if (stage == Stage.FINAL && view.height() < view.resting() + FLARE_HEIGHT)
            stage = Stage.FLARE;
        return switch (stage)
        {
            case APPROACH -> fly(view, Stage.APPROACH, bearing(view.position(), fix), view.siteGround() + view.resting() + APPROACH_HEIGHT, 1F);
            case FINAL -> glide(view, current, ahead);
            // Bounced back into the air, it holds its flare.
            case FLARE, ROLLOUT, LANDED -> new Step(Stage.FLARE, FLARE_PITCH, 0F, runway, 0F, false);
        };
    }

    /** Glides down the slope to the site at the approach speed. */
    private Step glide(View view, Vec3 at, double ahead)
    {
        float heading = horizontal(view.position(), at) > HOLD_HEADING ? bearing(view.position(), at) : runway;
        double speed = view.velocity().length();
        float throttle = Mth.clamp(GLIDE_THROTTLE + (float) (view.approachSpeed() - speed) * THROTTLE_PER_SPEED, 0.1F, 1F);
        return fly(view, Stage.FINAL, heading, view.siteGround() + view.resting() + Math.max(0D, ahead) * GLIDE_SLOPE, throttle);
    }

    private static Step fly(View view, Stage stage, float heading, double height, float throttle)
    {
        float pitch = Mth.clamp(NpcPilot.holdHeight(view.position().y, view.velocity().y, height), -MAX_GLIDE_PITCH, MAX_GLIDE_PITCH);
        float bank = Mth.clamp(Mth.wrapDegrees(heading - view.heading()) * BANK_PER_DEGREE, -MAX_BANK, MAX_BANK);
        return new Step(stage, pitch, bank, heading, throttle, false);
    }

    private static float bearing(Vec3 from, Vec3 to)
    {
        return MountMotion.yaw(to.subtract(from));
    }

    private static double horizontal(Vec3 from, Vec3 to)
    {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }
}
