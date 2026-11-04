package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.api.driveables.CrashMode;
import com.flansmodultimate.api.driveables.CrashMotion;
import com.flansmodultimate.api.driveables.DriveableAttitude;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * How a crippled or dead aircraft comes down: as an airframe, not as a brick.
 *
 * <p>
 * An aircraft with its tail on keeps its nose into the wind, so it follows its own flight path down, and whatever is
 * left of its wings still bites while the air meets them head on, which bends that path into a dive rather than a
 * parabola. What it has lost decides the rest: a lost wing rolls it over toward the stump into a spiral, a lost tail
 * lets it settle into a flat spin that falls almost level, a helicopter without its tail rotor spins about its mast, and
 * one without its main rotor drops with its nose tucking under. Shared by real driveables and NPCs drawn as aircraft.
 * </p>
 *
 * <p>
 * Everything is in world space: the turn of a tick is a rotation vector, its axis by the right-hand rule and its length
 * in degrees, so each caller can apply it to whatever angles it keeps. Pure and deterministic, so a client predicting
 * the fall sees what the server does.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AircraftCrashPhysics
{
    /** Blocks a tick at which the air has full authority over the airframe's attitude. */
    static final double AERO_SPEED = 0.8D;
    /** Blocks a tick at which the wings make their full remaining lift. */
    static final double LIFT_SPEED = 1.2D;
    /** Share of gravity the wings of a falling aircraft may hold up at most, so that it always comes down. */
    static final double MAX_LIFT_SHARE = 0.7D;
    /** Share of the angle between nose and flight path the tail turns the nose through each tick. */
    static final double WEATHERVANE_GAIN = 0.12D;
    /** Fastest the tail turns the nose into the wind, in degrees a tick. */
    static final double MAX_WEATHERVANE = 6D;
    /** Degrees a tick a nose-heavy airframe without its structure pitches down on its own. */
    static final double DIVE_PITCH = 0.6D;
    /** Degrees a tick an aircraft that has lost a wing rolls toward the stump, at speed. */
    static final double SPIRAL_ROLL = 4D;
    /** Degrees a tick a flat spin and a mast spin turn at once settled. */
    static final double FLAT_SPIN_RATE = 14D;
    static final double MAST_SPIN_RATE = 18D;
    /** Degrees below the horizon a flat spin settles its nose at. */
    static final double FLAT_SPIN_NOSE = -12D;
    /** Degrees a tick a helicopter that has lost its rotor tucks its nose under, and how steep it ends up. */
    static final double ROTOR_LOSS_PITCH = 1.5D;
    static final double ROTOR_LOSS_STEEPEST = -0.6D;
    /** Share of the gap to the wanted turn closed each tick: the airframe's rotational inertia. */
    static final double RESPONSE = 0.15D;
    /** Turn a tumble keeps each tick. */
    static final double TUMBLE_RETENTION = 0.985D;
    /** Velocity kept each tick by the air, along the path and in a flat spin, which presents its whole belly to it. */
    static final double AIR_RETENTION = 0.985D;
    static final double FLAT_SPIN_HORIZONTAL_RETENTION = 0.93D;
    /** Fastest each kind of fall comes down, in blocks a tick. */
    static final double DIVE_TERMINAL = 2.4D;
    static final double FLAT_SPIN_TERMINAL = 0.75D;
    static final double ROTORCRAFT_TERMINAL = 1.4D;
    /** Share of the speed the remaining wing swings toward the nose each tick at most, as the flight path bends. */
    static final double PATH_ALIGNMENT = 0.05D;
    /** Degrees each angle is nudged by to measure how the attitude follows it, the damping of the solve, and its passes. */
    private static final double DIFFERENCE_DEGREES = 0.5D;
    private static final double DAMPING = 1.0E-9D;
    private static final int SOLVE_PASSES = 4;
    private static final double SOLVED = 1.0E-10D;

    /** Gives the attitude of a set of Euler angles, as the caller keeps them. */
    @FunctionalInterface
    public interface AttitudeOf
    {
        DriveableAttitude at(float yaw, float pitch, float roll);
    }

    /** Euler angles in the caller's own convention. */
    public record Angles(float yaw, float pitch, float roll)
    {}

    /**
     * Advances a falling aircraft by one tick.
     *
     * @param motion
     *            its motion last tick
     * @param attitude
     *            where it points now
     * @param mode
     *            how it comes down
     * @param lift
     *            the share of its lift it still has: its wings' for an aircraft, its rotor's for a rotorcraft
     * @param gravity
     *            the pull of gravity, in blocks a tick squared
     * @param mirrored
     *            which way an aircraft that spins without a reason of its own to choose spins: fixed for one aircraft,
     *            so it spins the same way for everyone watching
     * @return its motion this tick
     */
    public static CrashMotion step(CrashMotion motion, DriveableAttitude attitude, CrashMode mode, float lift, double gravity, boolean mirrored)
    {
        Vec3 velocity = finite(motion.velocity());
        Vec3 front = unit(attitude.front(), new Vec3(1D, 0D, 0D));
        Vec3 roof = unit(attitude.roof(), new Vec3(0D, 1D, 0D));
        Vec3 right = unit(front.cross(roof), new Vec3(0D, 0D, 1D));
        double speed = velocity.length();
        double air = Mth.clamp(speed / AERO_SPEED, 0D, 1D);
        double spinSign = mirrored ? -1D : 1D;

        Vec3 wanted = switch (mode)
        {
            case NONE -> Vec3.ZERO;
            case DIVE -> weathervane(front, velocity, air, 1D).add(right.scale(-DIVE_PITCH * noseRoom(front)));
            case SPIRAL_LEFT -> weathervane(front, velocity, air, 1D).add(front.scale(-SPIRAL_ROLL * (0.3D + 0.7D * air)));
            case SPIRAL_RIGHT -> weathervane(front, velocity, air, 1D).add(front.scale(SPIRAL_ROLL * (0.3D + 0.7D * air)));
            case FLAT_SPIN -> new Vec3(0D, FLAT_SPIN_RATE * spinSign, 0D).add(levelling(front, roof, right, FLAT_SPIN_NOSE));
            case MAST_SPIN -> roof.scale(MAST_SPIN_RATE * spinSign).add(levelling(front, roof, right, 0D));
            case ROTOR_LOSS -> weathervane(front, velocity, air, 0.3D)
                .add(right.scale(front.y > ROTOR_LOSS_STEEPEST ? -ROTOR_LOSS_PITCH : 0D))
                .add(front.scale(0.8D * spinSign));
            case TUMBLE -> null;
        };
        Vec3 previous = finite(motion.rotation());
        Vec3 rotation = wanted == null ? previous.scale(TUMBLE_RETENTION) : previous.add(wanted.subtract(previous).scale(RESPONSE));
        return new CrashMotion(fall(velocity, front, roof, mode, Mth.clamp(lift, 0F, 1F), gravity, air), rotation);
    }

    /** Turns the nose toward the flight path, as the tail does, harder the faster the air meets it. */
    private static Vec3 weathervane(Vec3 front, Vec3 velocity, double air, double gain)
    {
        double speed = velocity.length();
        if (speed < 1.0E-6D || air <= 0D)
            return Vec3.ZERO;
        Vec3 path = velocity.scale(1D / speed);
        Vec3 axis = front.cross(path);
        double sine = axis.length();
        if (sine < 1.0E-6D)
            return Vec3.ZERO;
        double angle = Math.toDegrees(Math.atan2(sine, front.dot(path)));
        double rate = Math.min(MAX_WEATHERVANE, angle * WEATHERVANE_GAIN * air * gain);
        return axis.scale(rate / sine);
    }

    /** @return how much further the nose may still go down before it points straight at the ground */
    private static double noseRoom(Vec3 front)
    {
        return Mth.clamp((front.y + 0.95D) / 0.95D, 0D, 1D);
    }

    /** Eases the nose to the given degrees above the horizon and the wings level, as a spin settles. */
    private static Vec3 levelling(Vec3 front, Vec3 roof, Vec3 right, double noseDegrees)
    {
        double elevation = Math.toDegrees(Math.asin(Mth.clamp(front.y, -1D, 1D)));
        // A turn about the right axis lifts the nose; one about the nose rolls the roof toward the right.
        double bank = Math.toDegrees(Math.atan2(-right.y, roof.y));
        return right.scale((noseDegrees - elevation) * 0.08D).add(front.scale(-bank * 0.06D));
    }

    private static Vec3 fall(Vec3 velocity, Vec3 front, Vec3 roof, CrashMode mode, float lift, double gravity, double air)
    {
        double speed = velocity.length();
        Vec3 bent = velocity;
        double rise = 0D;
        double terminal = DIVE_TERMINAL;
        double horizontal = AIR_RETENTION;
        switch (mode)
        {
            case FLAT_SPIN ->
            {
                terminal = FLAT_SPIN_TERMINAL;
                horizontal = FLAT_SPIN_HORIZONTAL_RETENTION;
            }
            case MAST_SPIN, ROTOR_LOSS ->
            {
                terminal = ROTORCRAFT_TERMINAL;
                // A rotor lifts along the mast whatever the airspeed.
                rise = lift * gravity * MAX_LIFT_SHARE * Math.max(0D, roof.y);
            }
            case TUMBLE ->
            {
                // Nothing flies any more.
            }
            default ->
            {
                if (speed > 1.0E-6D)
                {
                    double facing = Math.max(0D, front.dot(velocity.scale(1D / speed)));
                    double bite = lift * facing * facing;
                    double dynamic = Math.min(1D, speed * speed / (LIFT_SPEED * LIFT_SPEED));
                    rise = Math.min(MAX_LIFT_SHARE, bite * dynamic) * gravity * Math.abs(roof.y);
                    // The wing that still bites swings the path toward the nose.
                    double alignment = PATH_ALIGNMENT * bite * air;
                    bent = velocity.scale(1D - alignment).add(front.scale(speed * alignment));
                }
            }
        }
        double vertical = Math.max(-terminal, (bent.y + rise - gravity) * AIR_RETENTION);
        return new Vec3(bent.x * horizontal, vertical, bent.z * horizontal);
    }

    /**
     * Turns an attitude by a world-space rotation vector.
     *
     * @param rotation
     *            the axis, by the right-hand rule, its length the angle in degrees
     */
    public static DriveableAttitude rotate(DriveableAttitude attitude, Vec3 rotation)
    {
        double degrees = rotation.length();
        if (degrees < 1.0E-9D || !Double.isFinite(degrees))
            return attitude;
        Vec3 axis = rotation.scale(1D / degrees);
        double radians = Math.toRadians(degrees);
        return new DriveableAttitude(rotate(attitude.front(), axis, radians).normalize(), rotate(attitude.roof(), axis, radians).normalize());
    }

    /** Rodrigues' rotation of a vector about a unit axis. */
    private static Vec3 rotate(Vec3 vector, Vec3 axis, double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return vector.scale(cos).add(axis.cross(vector).scale(sin)).add(axis.scale(axis.dot(vector) * (1D - cos)));
    }

    /**
     * Finds the Euler angles, in the caller's convention, that turn the attitude they give by the rotation, so a caller
     * that keeps yaw, pitch and roll need not know how they compose. Solved by damped least squares from finite
     * differences, which stays well behaved where the angles themselves do not, near a vertical nose.
     *
     * @param attitudeOf
     *            the attitude any set of angles gives
     * @param current
     *            the angles now
     * @param rotation
     *            the world-space rotation vector to apply, in degrees
     */
    public static Angles turn(AttitudeOf attitudeOf, Angles current, Vec3 rotation)
    {
        if (rotation.lengthSqr() < 1.0E-12D || !Double.isFinite(rotation.lengthSqr()))
            return current;
        DriveableAttitude now = attitudeOf.at(current.yaw(), current.pitch(), current.roll());
        DriveableAttitude target = rotate(now, rotation);
        float yaw = current.yaw();
        float pitch = current.pitch();
        float roll = current.roll();
        // Gauss-Newton: one pass lands within a fraction of a degree for any turn a tick makes, the rest polish it.
        for (int pass = 0; pass < SOLVE_PASSES; pass++)
        {
            DriveableAttitude at = attitudeOf.at(yaw, pitch, roll);
            double[] error = difference(target, at);
            if (dot(error, error) < SOLVED)
                break;
            double[][] jacobian = new double[3][];
            jacobian[0] = derivative(attitudeOf, yaw, pitch, roll, 0);
            jacobian[1] = derivative(attitudeOf, yaw, pitch, roll, 1);
            jacobian[2] = derivative(attitudeOf, yaw, pitch, roll, 2);
            double[] step = solve(jacobian, error);
            yaw += (float) step[0];
            pitch += (float) step[1];
            roll += (float) step[2];
        }
        return new Angles(yaw, pitch, roll);
    }

    /** Central difference: how the attitude moves per degree of one angle. */
    private static double[] derivative(AttitudeOf attitudeOf, float yaw, float pitch, float roll, int angle)
    {
        float step = (float) DIFFERENCE_DEGREES;
        DriveableAttitude ahead = attitudeOf.at(yaw + (angle == 0 ? step : 0F), pitch + (angle == 1 ? step : 0F), roll + (angle == 2 ? step : 0F));
        DriveableAttitude behind = attitudeOf.at(yaw - (angle == 0 ? step : 0F), pitch - (angle == 1 ? step : 0F), roll - (angle == 2 ? step : 0F));
        double[] change = difference(ahead, behind);
        for (int i = 0; i < change.length; i++)
            change[i] /= 2D * step;
        return change;
    }

    private static double[] difference(DriveableAttitude a, DriveableAttitude b)
    {
        return new double[] {a.front().x - b.front().x, a.front().y - b.front().y, a.front().z - b.front().z, a.roof().x - b.roof().x, a.roof().y - b.roof().y, a.roof().z - b.roof().z};
    }

    /** Solves the damped normal equations (JᵀJ + λI) x = Jᵀe for three unknowns, J given column by column. */
    private static double[] solve(double[][] columns, double[] error)
    {
        double[][] normal = new double[3][3];
        double[] right = new double[3];
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
                normal[i][j] = dot(columns[i], columns[j]) + (i == j ? DAMPING : 0D);
            right[i] = dot(columns[i], error);
        }
        double determinant = determinant(normal);
        if (Math.abs(determinant) < 1.0E-18D)
            return new double[3];
        double[] result = new double[3];
        for (int column = 0; column < 3; column++)
        {
            double[][] replaced = new double[3][];
            for (int row = 0; row < 3; row++)
            {
                replaced[row] = normal[row].clone();
                replaced[row][column] = right[row];
            }
            result[column] = determinant(replaced) / determinant;
        }
        return result;
    }

    private static double dot(double[] a, double[] b)
    {
        double sum = 0D;
        for (int i = 0; i < a.length; i++)
            sum += a[i] * b[i];
        return sum;
    }

    private static double determinant(double[][] m)
    {
        return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    }

    private static Vec3 unit(Vec3 vector, Vec3 fallback)
    {
        double length = vector.length();
        return length < 1.0E-9D || !Double.isFinite(length) ? fallback : vector.scale(1D / length);
    }

    private static Vec3 finite(Vec3 vector)
    {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z) ? vector : Vec3.ZERO;
    }
}
