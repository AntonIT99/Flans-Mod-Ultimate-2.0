package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;

import java.util.EnumMap;
import java.util.Map;

/** Server-owned repair clocks. Real seat occupants supply the crew fractions used by ship damage control. */
public final class DriveableRepairs
{
    private final Map<EnumDriveablePart, Integer> clocks = new EnumMap<>(EnumDriveablePart.class);
    private int buoyancyClock;

    public void tick(Driveable driveable, boolean repairing, boolean damageControl)
    {
        if (driveable.level().isClientSide || driveable.getDriveableData() == null)
            return;
        boolean carrier = servicedByCarrier(driveable);
        boolean idle = Math.abs(driveable.getThrottle()) < 0.1F;
        float totalCrew = crewFraction(driveable, null);
        for (DriveablePart part : driveable.getDriveableData().getParts().values())
            tickPart(driveable, part, carrier, idle && (repairing || carrier), idle && damageControl, totalCrew);
        tickBuoyancy(driveable, totalCrew);
    }

    private void tickPart(Driveable driveable, DriveablePart part, boolean carrier, boolean repairing, boolean damageControl, float totalCrew)
    {
        EnumDriveablePart type = part.getType();
        if (part.isDestroyed() || !driveable.canRepairPart(type))
        {
            clocks.remove(type);
            return;
        }
        if (carrier)
            driveable.repairPart(type, 1F);
        boolean ship = damageControl && type != EnumDriveablePart.BUOYANCY;
        if (!(ship || repairing))
            return;
        int interval = ship ? DriveableRepairRules.shipInterval(crewFraction(driveable, type), totalCrew) : 21;
        int clock = clocks.getOrDefault(type, 0) + 1;
        if (clock == 15)
            driveable.repairPart(type, DriveableRepairRules.timedAmount(type, part.getMaxHealth(), ship));
        clocks.put(type, clock >= interval ? 0 : clock);
    }

    private void tickBuoyancy(Driveable driveable, float totalCrew)
    {
        var type = driveable.getConfigType();
        if (type == null || !type.isEpicShip())
            return;
        if (++buoyancyClock >= DriveableRepairRules.buoyancyInterval(totalCrew))
            buoyancyClock = 0;
        if (buoyancyClock == 10)
            driveable.repairPart(EnumDriveablePart.BUOYANCY, 1F);
    }

    private static float crewFraction(Driveable driveable, EnumDriveablePart part)
    {
        int capacity = 0;
        int occupied = 0;
        for (Seat seat : driveable.getSeats())
        {
            if (seat == null || seat.getSeatInfo() == null || part != null && seat.getSeatInfo().getPart() != part)
                continue;
            ++capacity;
            if (driveable.isPartIntact(seat.getSeatInfo().getPart()) && seat.getRiddenByEntity() != null)
                ++occupied;
        }
        return capacity == 0 ? 0F : (float) occupied / capacity;
    }

    private static boolean servicedByCarrier(Driveable driveable)
    {
        if (!(driveable instanceof Plane))
            return false;
        Driveable parent = Driveable.ofRiddenEntity(driveable.getVehicle());
        if (parent != null && parent != driveable && parent.canMountEntity())
            return true;
        if (Math.abs(driveable.getThrottle()) >= 0.1F)
            return false;
        return !driveable.level()
            .getEntitiesOfClass(Driveable.class, driveable.getBoundingBox().inflate(64D, 8D, 64D), candidate -> candidate != driveable && candidate.supportsCarrierService(driveable)).isEmpty();
    }
}
