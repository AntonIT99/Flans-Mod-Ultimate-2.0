package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.collision.DriveableCollisionProfile;
import com.flansmodultimate.common.driveables.damage.PartDamageKind;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

/** Immutable per-part/per-facing armour table used by runtime hits. */
public final class ResolvedVehicleArmor
{
    private final VehicleArmorSpec source;
    private final Map<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> plates;

    ResolvedVehicleArmor(VehicleArmorSpec source, Map<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> plates)
    {
        this.source = source == null ? VehicleArmorSpec.EMPTY : source;
        EnumMap<EnumDriveablePart, Map<EnumArmorFacing, ResolvedArmorPlate>> copy = new EnumMap<>(EnumDriveablePart.class);
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
        return source.partOverrides().containsKey(part) || DriveableCollisionProfile.isTurretMountedPart(part) && source.turret().containsKey(facing) || source.hull().containsKey(facing);
    }

    public ResolvedArmorPlate plate(EnumDriveablePart part, EnumArmorFacing facing)
    {
        EnumArmorFacing safeFacing = facing == null ? EnumArmorFacing.FRONT : facing;
        Map<EnumArmorFacing, ResolvedArmorPlate> byFacing = plates.get(part);
        return byFacing == null ? ResolvedArmorPlate.unarmoured(safeFacing) : byFacing.getOrDefault(safeFacing, ResolvedArmorPlate.unarmoured(safeFacing));
    }

    /**
     * The plate a part shows on a face once the armour parts already shot off are taken away: a hull part whose face
     * is carried by a destroyed armour part is open there.
     *
     * @param shotOff
     *            whether the given part has been destroyed
     */
    public ResolvedArmorPlate plate(EnumDriveablePart part, EnumArmorFacing facing, Predicate<EnumDriveablePart> shotOff)
    {
        EnumArmorFacing safeFacing = facing == null ? EnumArmorFacing.FRONT : facing;
        return isBreached(part, safeFacing, shotOff) ? ResolvedArmorPlate.unarmoured(safeFacing) : plate(part, safeFacing);
    }

    /**
     * The armour part that carries a hull face, or null for a face no such part carries. {@code frontalArmor},
     * {@code leftsideArmor} and {@code rightsideArmor} are the hull's plates on those faces, so the hull behind them
     * inherits the same {@code ArmorFrontMm}/{@code ArmorSideMm} plate.
     */
    @Nullable
    public static EnumDriveablePart coveringPart(@Nullable EnumArmorFacing facing)
    {
        if (facing == null)
            return null;
        return switch (facing)
        {
            case FRONT -> EnumDriveablePart.FRONTAL_ARMOR;
            case LEFT -> EnumDriveablePart.LEFTSIDE_ARMOR;
            case RIGHT -> EnumDriveablePart.RIGHTSIDE_ARMOR;
            default -> null;
        };
    }

    /**
     * Whether a part's face has lost its plate because the armour part carrying that face was shot off. Only a hull part
     * that inherits the hull plate is breached: a turret part, a part with its own {@code PartArmor} plate and the armour
     * parts themselves (skirts, ERA, spaced plates) keep theirs. A driveable without the covering part is never breached.
     */
    public boolean isBreached(@Nullable EnumDriveablePart part, @Nullable EnumArmorFacing facing, @Nullable Predicate<EnumDriveablePart> shotOff)
    {
        EnumDriveablePart cover = coveringPart(facing);
        if (part == null || cover == null || shotOff == null || part == cover || !plates.containsKey(cover))
            return false;
        if (source.partOverrides().containsKey(part) || DriveableCollisionProfile.isTurretMountedPart(part) || PartDamageKind.defaultFor(part) == PartDamageKind.ARMOR)
            return false;
        return shotOff.test(cover);
    }

    public ResolvedArmorHit resolveHit(EnumDriveablePart part, EnumArmorFacing facing, Vec3 projectileDirection, double maxImpactAngleDeg)
    {
        return resolveHit(part, facing, projectileDirection, maxImpactAngleDeg, false);
    }

    /**
     * @param shapedCharge
     *            resolve against the face's protection against HEAT instead of its kinetic value
     */
    public ResolvedArmorHit resolveHit(EnumDriveablePart part, EnumArmorFacing facing, Vec3 projectileDirection, double maxImpactAngleDeg, boolean shapedCharge)
    {
        return resolveHit(part, facing, projectileDirection, maxImpactAngleDeg, shapedCharge, null);
    }

    /**
     * @param shotOff
     *            whether a part has been destroyed, so a face whose armour part is gone meets no plate; null ignores
     *            destroyed parts
     */
    public ResolvedArmorHit resolveHit(EnumDriveablePart part, EnumArmorFacing facing, Vec3 projectileDirection, double maxImpactAngleDeg, boolean shapedCharge,
        @Nullable Predicate<EnumDriveablePart> shotOff)
    {
        EnumArmorFacing safeFacing = facing == null ? EnumArmorFacing.FRONT : facing;
        ResolvedArmorPlate plate = isBreached(part, safeFacing, shotOff) ? ResolvedArmorPlate.unarmoured(safeFacing) : plate(part, safeFacing);
        ArmorPlate authored = plate.authored();
        float thickness = authored.thicknessAgainst(shapedCharge);
        if (!Float.isFinite(thickness) || thickness <= 0F)
            return new ResolvedArmorHit(part, safeFacing, authored, plate.virtualNormal(), 0F, 0F);

        double safeMaxAngle = Double.isFinite(maxImpactAngleDeg) ? Mth.clamp(maxImpactAngleDeg, 0D, 89.9D) : 80D;
        double minimumCosine = Math.cos(Math.toRadians(safeMaxAngle));
        Vec3 direction = projectileDirection == null ? Vec3.ZERO : projectileDirection.normalize();
        double rawCosine = -direction.dot(plate.virtualNormal());
        double cosine = Mth.clamp(rawCosine, minimumCosine, 1D);
        double effective = thickness / cosine;
        if (!Double.isFinite(effective))
            effective = thickness / minimumCosine;
        float impactAngle = (float) Math.toDegrees(Math.acos(Mth.clamp(cosine, -1D, 1D)));
        return new ResolvedArmorHit(part, safeFacing, authored, plate.virtualNormal(), impactAngle, (float) effective);
    }
}
