package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.api.driveables.AircraftDamage;
import com.flansmodultimate.api.driveables.AircraftMode;
import com.flansmodultimate.api.driveables.CrashMode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

/**
 * What an aircraft's damage costs it in the air, as it is worn down rather than only once a part is shot off: a holed
 * wing lifts less than the whole one and pulls the aircraft over toward itself, a shot-up tail answers the stick more
 * slowly, a hit engine loses power and misfires, and a holed wing tank leaks fuel. Shared by real driveables and NPCs
 * drawn as aircraft. Pure functions of {@link AircraftDamage}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AircraftDamageModel
{
    /** Share of a wing's lift lost as it is worn down to its last hit point. */
    static final float WING_LIFT_LOSS = 0.35F;
    /** Least share of its authority a tail still has just before it is shot off. */
    static final float MIN_TAIL_AUTHORITY = 0.35F;
    /** Share of the power lost by an engine worn down to its last hit point. */
    static final float ENGINE_POWER_LOSS = 0.55F;
    /** Engine health under which it starts to misfire, and the misfire chance a tick just before it fails. */
    static final float MISFIRE_HEALTH = 0.4F;
    private static final float MIN_MISFIRE_CHANCE = 0.02F;
    private static final float MAX_MISFIRE_CHANCE = 0.2F;
    /** Wing health under which its tank leaks, and the share of a full tank a second it leaks at worst, or once torn off. */
    static final float LEAK_HEALTH = 0.6F;
    static final float MAX_LEAK_PER_SECOND = 0.004F;
    static final float TORN_TANK_LEAK_PER_SECOND = 0.006F;
    /** Least share of its lift a holed main rotor still makes, and of its authority a holed tail rotor keeps. */
    static final float MIN_ROTOR_LIFT = 0.6F;

    /** @return the share of its lift one wing still makes */
    public static float wingLift(float health)
    {
        return health <= 0F ? 0F : 1F - (1F - health) * WING_LIFT_LOSS;
    }

    /** @return the share of the whole aircraft's lift its wings still make; nothing once the airframe is gone */
    public static float liftFraction(AircraftDamage damage)
    {
        if (damage.airframe() <= 0F)
            return 0F;
        return 0.5F * (wingLift(damage.leftWing()) + wingLift(damage.rightWing()));
    }

    /**
     * @return how much more the left wing lifts than the right, from -1 to 1: positive rolls the aircraft over to its
     *         right, the right wing down
     */
    public static float rollImbalance(AircraftDamage damage)
    {
        return wingLift(damage.leftWing()) - wingLift(damage.rightWing());
    }

    /** @return the share of their authority the elevators and rudder still have; none once the tail is gone */
    public static float tailAuthority(AircraftDamage damage)
    {
        return damage.tail() <= 0F ? 0F : MIN_TAIL_AUTHORITY + (1F - MIN_TAIL_AUTHORITY) * damage.tail();
    }

    /** @return the share of its power the engine still makes: little lost at first, more as it is worn down */
    public static float thrustFraction(AircraftDamage damage)
    {
        float health = damage.engine();
        if (health <= 0F)
            return 0F;
        float worn = 1F - health;
        return 1F - ENGINE_POWER_LOSS * worn * (float) Math.sqrt(worn);
    }

    /** @return the chance a tick that a badly hit engine misfires, coughing smoke and losing its note for a moment */
    public static float misfireChance(AircraftDamage damage)
    {
        float health = damage.engine();
        if (health >= MISFIRE_HEALTH || health <= 0F)
            return 0F;
        return Mth.lerp((MISFIRE_HEALTH - health) / MISFIRE_HEALTH, MIN_MISFIRE_CHANCE, MAX_MISFIRE_CHANCE);
    }

    /** @return the share of a full tank leaking away each second through holed or torn-off wings */
    public static float fuelLeakPerSecond(AircraftDamage damage)
    {
        return wingLeak(damage.leftWing()) + wingLeak(damage.rightWing());
    }

    private static float wingLeak(float health)
    {
        if (health <= 0F)
            return TORN_TANK_LEAK_PER_SECOND;
        if (health >= LEAK_HEALTH)
            return 0F;
        return MAX_LEAK_PER_SECOND * (LEAK_HEALTH - health) / LEAK_HEALTH;
    }

    /** @return the share of its lift a rotorcraft's main rotor still makes */
    public static float rotorLift(AircraftDamage damage)
    {
        return damage.rotor() <= 0F ? 0F : MIN_ROTOR_LIFT + (1F - MIN_ROTOR_LIFT) * damage.rotor();
    }

    /** @return the share of its authority the tail rotor still has; none once the tail rotor or the boom carrying it is gone */
    public static float tailRotorAuthority(AircraftDamage damage)
    {
        float health = Math.min(damage.tail(), damage.tailRotor());
        return health <= 0F ? 0F : MIN_TAIL_AUTHORITY + (1F - MIN_TAIL_AUTHORITY) * health;
    }

    /** @return how hard the airframe shakes, from 0 to 1, for the pilot's view and the cockpit warnings */
    public static float vibration(AircraftDamage damage, AircraftMode mode)
    {
        float engine = 1F - damage.engine();
        float structure = Math.max(1F - damage.airframe(), Math.abs(rollImbalance(damage)) * 2F);
        float rotor = mode == AircraftMode.ROTORCRAFT ? 1F - damage.rotor() : 0F;
        return Mth.clamp(Math.max(engine * engine, Math.max(structure, rotor)), 0F, 1F);
    }

    /** @return how the aircraft comes down with what it has lost, or {@link CrashMode#NONE} while it still flies */
    public static CrashMode crashMode(AircraftDamage damage, AircraftMode mode)
    {
        if (mode == AircraftMode.ROTORCRAFT)
            return rotorcraftCrashMode(damage);
        return wingedCrashMode(damage, mode);
    }

    private static CrashMode rotorcraftCrashMode(AircraftDamage damage)
    {
        if (damage.rotor() <= 0F || damage.airframe() <= 0F)
            return CrashMode.ROTOR_LOSS;
        if (damage.tail() <= 0F || damage.tailRotor() <= 0F)
            return CrashMode.MAST_SPIN;
        return CrashMode.NONE;
    }

    private static CrashMode wingedCrashMode(AircraftDamage damage, AircraftMode mode)
    {
        if (damage.airframe() <= 0F)
            return CrashMode.DIVE;
        if (mode == AircraftMode.SIX_DOF)
            return CrashMode.NONE;
        boolean left = damage.leftWing() <= 0F;
        boolean right = damage.rightWing() <= 0F;
        boolean tail = damage.tail() <= 0F;
        if (left && right || (left || right) && tail)
            return CrashMode.TUMBLE;
        if (left)
            return CrashMode.SPIRAL_LEFT;
        if (right)
            return CrashMode.SPIRAL_RIGHT;
        return tail ? CrashMode.FLAT_SPIN : CrashMode.NONE;
    }

    /** @return how the aircraft comes down once nobody flies it any more: as its damage says, or a dive with nothing lost */
    public static CrashMode wreckMode(AircraftDamage damage, AircraftMode mode)
    {
        CrashMode crash = crashMode(damage, mode);
        if (crash.falling())
            return crash;
        return mode == AircraftMode.ROTORCRAFT ? CrashMode.ROTOR_LOSS : CrashMode.DIVE;
    }
}
