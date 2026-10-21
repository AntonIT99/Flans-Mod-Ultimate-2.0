package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import org.jetbrains.annotations.Nullable;

/**
 * The broad kind of machine a driveable is, which decides how it dies: a tank cooks off, a ship sinks, an aircraft
 * goes in burning and a mecha collapses.
 */
public enum DriveableDamageClass
{
    /** A turreted or tracked armoured vehicle. */
    TANK,
    /** A wheeled car, truck or unarmoured ground vehicle. */
    GROUND,
    /** A boat or ship. */
    SHIP,
    /** A fixed-wing aircraft. */
    PLANE,
    /** A helicopter. */
    HELICOPTER,
    /** A walking mecha. */
    MECHA;

    public static DriveableDamageClass of(@Nullable DriveableType type)
    {
        if (type == null)
            return GROUND;
        boolean tank = type instanceof VehicleType vehicle && vehicle.isTank();
        boolean tracked = type.getHealth().containsKey(EnumDriveablePart.LEFT_TRACK) || type.getHealth().containsKey(EnumDriveablePart.RIGHT_TRACK);
        boolean turret = type.getHealth().containsKey(EnumDriveablePart.TURRET);
        return classify(type instanceof PlaneType, type.isHelicopter(), type instanceof MechaType, type.isShip(), tank || tracked, turret);
    }

    /**
     * @param plane
     *            whether the type flies as a plane or helicopter
     * @param helicopter
     *            whether it flies as a helicopter
     * @param mecha
     *            whether it walks
     * @param ship
     *            whether it only floats
     * @param tracked
     *            whether it is a tank or runs on tracks
     * @param turret
     *            whether it has a turret part
     */
    public static DriveableDamageClass classify(boolean plane, boolean helicopter, boolean mecha, boolean ship, boolean tracked, boolean turret)
    {
        if (mecha)
            return MECHA;
        if (ship)
            return SHIP;
        if (plane)
            return helicopter ? HELICOPTER : PLANE;
        return tracked || turret ? TANK : GROUND;
    }

    /** @return whether the machine is held up by the air, so its wreck falls and trails smoke as it does */
    public boolean flies()
    {
        return this == PLANE || this == HELICOPTER;
    }
}
