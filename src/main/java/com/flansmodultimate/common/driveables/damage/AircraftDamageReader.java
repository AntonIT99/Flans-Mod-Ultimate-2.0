package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.api.driveables.AircraftDamage;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Reads an aircraft's damage off its parts by their names alone, for bodies that have no propeller layout to go by,
 * such as an NPC drawn as an aircraft: the engine is its engine rooms, and the tail rotor goes with the tail.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AircraftDamageReader
{
    /** @return the damage of the aircraft the parts belong to */
    public static AircraftDamage of(Iterable<DriveablePart> parts)
    {
        Map<EnumDriveablePart, Float> shares = new EnumMap<>(EnumDriveablePart.class);
        float engine = 0F;
        int rooms = 0;
        for (DriveablePart part : parts)
        {
            if (part.getMaxHealth() <= 0F)
                continue;
            float share = part.isDestroyed() ? 0F : Math.max(0F, Math.min(1F, part.getHealth() / part.getMaxHealth()));
            shares.put(part.getType(), share);
            if (EnumDriveablePart.getEngineRooms().contains(part.getType()))
            {
                engine += share;
                ++rooms;
            }
        }
        return new AircraftDamage(shares.getOrDefault(EnumDriveablePart.LEFT_WING, 1F), shares.getOrDefault(EnumDriveablePart.RIGHT_WING, 1F),
            shares.getOrDefault(EnumDriveablePart.TAIL, 1F), shares.getOrDefault(EnumDriveablePart.AIRFRAME, 1F), rooms == 0 ? 1F : engine / rooms,
            shares.getOrDefault(EnumDriveablePart.BLADES, 1F), 1F);
    }
}
