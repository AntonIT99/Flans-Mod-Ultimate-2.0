package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.DriveableCollisionProfile;
import com.flansmodultimate.common.driveables.EnumDriveablePart;

import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Resolves override -> turret semantic -> hull semantic -> unarmoured once per type. */
public final class VehicleArmorResolver
{
    private VehicleArmorResolver() {}

    public static ResolvedVehicleArmor resolve(VehicleArmorSpec spec, Collection<EnumDriveablePart> parts)
    {
        VehicleArmorSpec source = spec == null ? VehicleArmorSpec.EMPTY : spec;
        EnumMap<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> result =
            new EnumMap<>(EnumDriveablePart.class);
        if (parts != null)
        {
            for (EnumDriveablePart part : parts)
            {
                if (part == null)
                    continue;
                EnumMap<EnumArmorFacing, ResolvedArmorPlate> facings = new EnumMap<>(EnumArmorFacing.class);
                ArmorPlate override = source.partOverrides().get(part);
                for (EnumArmorFacing facing : EnumArmorFacing.values())
                {
                    ArmorPlate authored = override;
                    if (authored == null && DriveableCollisionProfile.isTurretMountedPart(part))
                        authored = source.turret().get(facing);
                    if (authored == null)
                        authored = source.hull().get(facing);
                    if (authored == null)
                        authored = ArmorPlate.UNARMOURED;
                    facings.put(facing, new ResolvedArmorPlate(authored, virtualNormal(facing, authored.slopeDeg())));
                }
                result.put(part, Collections.unmodifiableMap(facings));
            }
        }
        return new ResolvedVehicleArmor(source, result);
    }

    /**
     * Rotates the nominal outward normal by the authored virtual slope. A line
     * such as {@code ArmorFrontMm 80 55} therefore gives a nominal head-on ray
     * a cosine of {@code cos(55 deg)} even when the collision box is vertical.
     */
    static Vec3 virtualNormal(EnumArmorFacing facing, float slopeDeg)
    {
        Vec3 normal = facing.outwardNormal();
        if (!Float.isFinite(slopeDeg) || slopeDeg == 0F)
            return normal;
        double radians = Math.toRadians(slopeDeg);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return switch (facing)
        {
            case FRONT, REAR -> new Vec3(normal.x,
                normal.y * cosine - normal.z * sine,
                normal.y * sine + normal.z * cosine).normalize();
            case LEFT, RIGHT -> new Vec3(
                normal.x * cosine - normal.y * sine,
                normal.x * sine + normal.y * cosine,
                normal.z).normalize();
            case TOP, BOTTOM -> new Vec3(normal.x,
                normal.y * cosine - normal.z * sine,
                normal.y * sine + normal.z * cosine).normalize();
        };
    }
}
