package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.physics.ModPhysics;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Rotorcraft forces in the existing aircraft basis, integrated once per server tick. */
public final class HelicopterPhysics
{
    /** Keep the legacy helicopter's gravity and half-collective hover convention. */
    public static final double GRAVITY = 0.05D;
    private static final double ROTOR_POWER_EFFICIENCY = 0.7D;
    /** Blocks a tick of air flowing up through the disc that keeps an unpowered rotor at full speed. */
    static final double AUTOROTATION_INFLOW = 0.45D;
    /** Share of the airspeed along the disc that also drives a windmilling rotor. */
    static final double FORWARD_INFLOW_SHARE = 0.25D;
    /** Rotor speed a tick that the inflow adds at most, and that friction takes away without it. */
    static final float WINDMILL_SPOOL_UP = 0.03F;
    static final float WINDMILL_SPOOL_DOWN = 0.01F;
    /** Rotor speed a tick a full collective spends of the rotor's stored energy, as in a flare. */
    static final float FLARE_DRAIN = 0.025F;
    /** Fastest a windmilling rotor turns, a little above its governed speed with the collective down. */
    static final float MAX_WINDMILL_SPEED = 1.05F;
    /** Drag of a windmilling disc on the descent, which settles at about the inflow that keeps it turning. */
    static final double DISC_DRAG = 0.25D;

    private HelicopterPhysics()
    {}

    public record Performance(double maximumLift, double horizontalDrag, double verticalDrag, double terminalSpeed, double climbSpeed)
    {}

    /**
     * Lift requires mass, shaft power and rotor geometry together. Speed and climb
     * are independent drag calibrations. The installed engine part does not scale the
     * real-world lift. Missing groups retain the legacy thrust and Drag values;
     * wing area is never mistaken for a rotor disc.
     */
    public static Performance resolve(RealWorldVehicleSpec spec, float legacyThrust, float legacyDrag, double speedScale)
    {
        double maximumLift = GRAVITY + Math.max(0F, finite(legacyThrust)) * 0.5D;
        if (VehiclePhysicsUnits.isUsablePositive(spec.massKg()) && VehiclePhysicsUnits.isUsablePositive(spec.enginePowerKw()) && VehiclePhysicsUnits.isUsablePositive(spec.aircraft().rotorDiameterM()))
        {
            double radius = spec.aircraft().rotorDiameterM() * 0.5D;
            double area = Math.PI * radius * radius * spec.aircraft().effectiveRotorCount();
            double power = spec.enginePowerKw() * VehiclePhysicsUnits.WATTS_PER_KILOWATT * ROTOR_POWER_EFFICIENCY;
            // Ideal actuator-disc power: P = T^(3/2) / sqrt(2 rho A).
            double thrust = Math.pow(power * Math.sqrt(2D * VehiclePhysicsUnits.AIR_DENSITY * area), 2D / 3D);
            maximumLift = GRAVITY * Mth.clamp(thrust / (spec.massKg() * VehiclePhysicsUnits.STANDARD_GRAVITY), 0D, 2.5D);
        }
        else if (VehiclePhysicsUnits.isUsablePositive(spec.massKg()) && VehiclePhysicsUnits.isUsablePositive(spec.engineThrustKn()))
        {
            // Thrust-authored VTOL lift engines need no invented shaft power or disc.
            double thrust = spec.engineThrustKn() * VehiclePhysicsUnits.NEWTONS_PER_KILONEWTON;
            maximumLift = GRAVITY * Mth.clamp(thrust / (spec.massKg() * VehiclePhysicsUnits.STANDARD_GRAVITY), 0D, 2.5D);
        }
        double drag = LegacyPlanePhysics.drag(legacyDrag);
        // Convert legacy velocity multipliers to implicit drag coefficients.
        double horizontal = 1D - (1D - drag) / 5D;
        double terminal = VehiclePhysicsUnits.isUsablePositive(spec.maxSpeedKmh()) ? VehiclePhysicsUnits.kmhToBlocksPerTick(spec.maxSpeedKmh(), speedScale) : 0D;
        double climb = VehiclePhysicsUnits.isUsablePositive(spec.aircraft().climbRateMs()) ? VehiclePhysicsUnits.metresPerSecondToBlocksPerTick(spec.aircraft().climbRateMs(), speedScale) : 0D;
        return new Performance(maximumLift, 1D / Math.max(0.01D, horizontal) - 1D, 1D / Math.max(0.01D, drag) - 1D, terminal, climb);
    }

    /** Smoothly approach the requested rotor-speed fraction without overshooting it. */
    public static float spool(float current, float target)
    {
        float speed = Mth.clamp(finite(current), 0F, 1F);
        float requested = Mth.clamp(finite(target), 0F, 1F);
        return speed < requested ? Math.min(requested, speed + 1F / 60F) : Math.max(requested, speed - 1F / 100F);
    }

    public static double lift(Performance performance, float collective, float rotorSpeed, float intactFraction)
    {
        double lever = Mth.clamp(finite(collective), 0F, 1F);
        double hover = Math.min(GRAVITY, performance.maximumLift());
        double demand = lever <= 0.5D ? hover * lever * 2D : hover + (performance.maximumLift() - hover) * (lever * 2D - 1D);
        double rpm = Mth.clamp(finite(rotorSpeed), 0F, 1F);
        return demand * rpm * rpm * Mth.clamp(finite(intactFraction), 0F, 1F);
    }

    /**
     * The rotor speed of an unpowered rotor in autorotation. Air rising through the disc as the helicopter comes down
     * keeps the rotor turning without the engine; raising the collective spends that stored energy on lift, which is
     * how a pilot cushions the touchdown with a flare, but holding it up too long lets the rotor slow and the lift go.
     *
     * @param rotorSpeed
     *            the rotor speed last tick, as a share of its governed speed
     * @param descent
     *            blocks a tick the helicopter comes down, along its mast
     * @param airspeedAlongDisc
     *            blocks a tick the air flows across the disc
     * @param collective
     *            the collective lever from 0 to 1
     * @return the rotor speed this tick
     */
    public static float autorotate(float rotorSpeed, double descent, double airspeedAlongDisc, float collective)
    {
        float speed = Mth.clamp(finite(rotorSpeed), 0F, MAX_WINDMILL_SPEED);
        double inflow = Math.max(0D, finite(descent)) + FORWARD_INFLOW_SHARE * Math.max(0D, finite(airspeedAlongDisc));
        float target = (float) Mth.clamp(inflow / AUTOROTATION_INFLOW, 0D, MAX_WINDMILL_SPEED);
        speed = speed < target ? Math.min(target, speed + WINDMILL_SPOOL_UP) : Math.max(target, speed - WINDMILL_SPOOL_DOWN);
        float lever = Mth.clamp(finite(collective), 0F, 1F);
        return Math.max(0F, speed - FLARE_DRAIN * lever * lever * speed);
    }

    /**
     * Slows the descent of a helicopter whose rotor windmills: the disc meets the rising air like a parachute,
     * harder the faster it turns.
     *
     * @return the velocity with the disc's drag applied to its descent
     */
    public static Vec3 autorotationDrag(Vec3 velocity, float rotorSpeed)
    {
        if (velocity.y >= 0D)
            return velocity;
        double rpm = Mth.clamp(finite(rotorSpeed), 0F, MAX_WINDMILL_SPEED);
        double brake = Math.min(-velocity.y, DISC_DRAG * rpm * rpm * velocity.y * velocity.y);
        return new Vec3(velocity.x, velocity.y + brake, velocity.z);
    }

    /** Match the planes' default speed-linear response across the whole throttle range. */
    public static double horizontalSpeedFraction(float throttle)
    {
        return Mth.clamp(finite(throttle), 0D, 1D);
    }

    /** Tilt redirects lift, with collective-scaled translation and no automatic tilt compensation. */
    public static Vec3 step(Vec3 velocity, Vec3 up, Performance performance, float collective, float rotorSpeed, float intactFraction)
    {
        return step(velocity, up, performance, collective, rotorSpeed, intactFraction, 1D, 1D);
    }

    @SuppressWarnings("java:S107") // Pure physics step: every input is a separate scalar of the simulation
    public static Vec3 step(Vec3 velocity, Vec3 up, Performance performance, float collective, float rotorSpeed, float intactFraction, double gravityFactor, double dragFactor)
    {
        double gravity = ModPhysics.gravity(GRAVITY, gravityFactor);
        double lift = lift(performance, collective, rotorSpeed, intactFraction);
        // Collective limits translation as well as lift, for fine low-speed handling.
        double lever = horizontalSpeedFraction(collective);
        Vec3 accelerated = velocity.add(up.x * lift * lever * lever, up.y * lift - gravity, up.z * lift * lever * lever);
        double horizontalDrag = performance.horizontalDrag();
        if (performance.terminalSpeed() > 0D)
        {
            // At the published speed, drag balances the available horizontal
            // force while the remaining component supports the helicopter.
            double levelThrust = Math.sqrt(Math.max(0D, performance.maximumLift() * performance.maximumLift() - gravity * gravity));
            horizontalDrag = Math.max(0.001D, levelThrust / performance.terminalSpeed()) * velocity.horizontalDistance() / performance.terminalSpeed();
        }
        // Arrest drift when level; fade this assistance out by about 17 degrees
        // of tilt so published full-power cruise remains a force equilibrium.
        double levelBraking = 0.12D * (1D - Mth.clamp(up.horizontalDistance() / 0.3D, 0D, 1D));
        horizontalDrag = Math.max(horizontalDrag, levelBraking);
        double verticalDrag = Math.max(0.1D, performance.verticalDrag());
        if (performance.climbSpeed() > 0D)
            verticalDrag = Math.max(0.001D, (up.y * lift < gravity ? gravity : Math.max(0D, performance.maximumLift() - gravity)) / performance.climbSpeed());
        // Increase the response rate of slow climb calibrations without changing
        // their equilibrium speed. Apply the same gain to net force and drag.
        double verticalResponse = Math.max(1D, 0.1D / verticalDrag);
        horizontalDrag = ModPhysics.dragForce(horizontalDrag, dragFactor);
        verticalDrag = ModPhysics.dragForce(verticalDrag, dragFactor);
        Vec3 result = new Vec3(accelerated.x / (1D + horizontalDrag), (velocity.y + (up.y * lift - gravity) * verticalResponse) / (1D + verticalDrag * verticalResponse),
            accelerated.z / (1D + horizontalDrag));
        double fullSpeed = performance.terminalSpeed() > 0D ? performance.terminalSpeed() : performance.maximumLift() / Math.max(0.001D, performance.horizontalDrag());
        double targetSpeed = fullSpeed * lever;
        double horizontalSpeed = result.horizontalDistance();
        if (horizontalSpeed > targetSpeed)
        {
            // Bleed excess momentum smoothly when the pilot lowers the lever.
            double limitedSpeed = Math.min(horizontalSpeed, Math.max(targetSpeed, velocity.horizontalDistance() / 1.12D));
            double scale = limitedSpeed / horizontalSpeed;
            result = new Vec3(result.x * scale, result.y, result.z * scale);
        }
        return result;
    }

    /** Apply the authored speed before wrapping, including fractional and negative rotor ratios. */
    public static float rotorAngle(double previous, double current, float partialTick, float ratio)
    {
        if (!Double.isFinite(previous) || !Double.isFinite(current) || !Float.isFinite(ratio))
            return 0F;
        double phase = previous + (current - previous) * Mth.clamp(finite(partialTick), 0F, 1F);
        return (float) ((phase * ratio) % 360D);
    }

    private static float finite(float value)
    {
        return Float.isFinite(value) ? value : 0F;
    }

    private static double finite(double value)
    {
        return Double.isFinite(value) ? value : 0D;
    }
}
