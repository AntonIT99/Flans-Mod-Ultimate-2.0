package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.DriveableCollisionProfile;
import com.flansmodultimate.common.driveables.EnumDriveablePart;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable per-part/per-facing armour table used by runtime hits. */
public final class ResolvedVehicleArmor
{
    private final VehicleArmorSpec source;
    private final Map<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> plates;

    ResolvedVehicleArmor(VehicleArmorSpec source,
                         Map<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> plates)
    {
        this.source = source == null ? VehicleArmorSpec.EMPTY : source;
        EnumMap<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> copy =
            new EnumMap<>(EnumDriveablePart.class);
        if (plates != null)
            copy.putAll(plates);
        this.plates = Collections.unmodifiableMap(copy);
    }

    public VehicleArmorSpec source()
    {
        return source;
    }

    public Map<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> plates()
    {
        return plates;
    }

    public boolean isConfigured()
    {
        return !source.isEmpty();
    }

    /** Whether this face inherits an authored plate, including an explicit zero-thickness plate. */
    public boolean isPlateConfigured(EnumDriveablePart part, EnumArmorFacing facing)
    {
        if (part == null || facing == null || !plates.containsKey(part))
            return false;
        return source.partOverrides().containsKey(part)
            || DriveableCollisionProfile.isTurretMountedPart(part) && source.turret().containsKey(facing)
            || source.hull().containsKey(facing);
    }

    public ResolvedArmorPlate plate(EnumDriveablePart part, EnumArmorFacing facing)
    {
        EnumArmorFacing safeFacing = facing == null ? EnumArmorFacing.FRONT : facing;
        Map<EnumArmorFacing, ResolvedArmorPlate> byFacing = plates.get(part);
        return byFacing == null
            ? ResolvedArmorPlate.unarmoured(safeFacing)
            : byFacing.getOrDefault(safeFacing, ResolvedArmorPlate.unarmoured(safeFacing));
    }

    public ResolvedArmorHit resolveHit(EnumDriveablePart part, EnumArmorFacing facing,
                                       Vec3 projectileDirection, double maxImpactAngleDeg)
    {
        EnumArmorFacing safeFacing = facing == null ? EnumArmorFacing.FRONT : facing;
        ResolvedArmorPlate plate = plate(part, safeFacing);
        ArmorPlate authored = plate.authored();
        if (!authored.isArmoured())
            return new ResolvedArmorHit(part, safeFacing, authored, plate.virtualNormal(), 0F, 0F);

        double safeMaxAngle = Double.isFinite(maxImpactAngleDeg)
            ? Mth.clamp(maxImpactAngleDeg, 0D, 89.9D) : 80D;
        double minimumCosine = Math.cos(Math.toRadians(safeMaxAngle));
        Vec3 direction = projectileDirection == null ? Vec3.ZERO : projectileDirection.normalize();
        double rawCosine = -direction.dot(plate.virtualNormal());
        double cosine = Mth.clamp(rawCosine, minimumCosine, 1D);
        double effective = authored.thicknessMm() / cosine;
        if (!Double.isFinite(effective))
            effective = authored.thicknessMm() / minimumCosine;
        float impactAngle = (float) Math.toDegrees(Math.acos(Mth.clamp(cosine, -1D, 1D)));
        return new ResolvedArmorHit(part, safeFacing, authored, plate.virtualNormal(), impactAngle,
            (float) effective);
    }
}
