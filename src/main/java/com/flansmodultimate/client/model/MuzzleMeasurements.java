package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.client.model.ModelDriveable;
import com.flansmod.client.model.ModelVehicle;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.DerivedMuzzle;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.entity.AAGunBarrelGeometry;
import com.flansmodultimate.common.entity.Driveable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns muzzles measured off a loaded model into the type-file values that fire
 * from them: the primary {@code ShootPointPrimary}/{@code BarrelPosition}, each
 * seat's {@code GunOrigin}, and the AA gun muzzle offsets that
 * {@link AAGunBarrelGeometry#legacyBarrelFor} turns into {@code Barrel} lines.
 *
 * <p>Takes the few type values it needs as plain inputs rather than a loaded
 * type, so the {@code /flandebug shootpoint} command and the offline
 * {@code shootPointSync} tooling derive exactly the same values.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MuzzleMeasurements
{
    /** A passenger seat mounting a gun, and the model part name its gun is registered under. */
    public record SeatGun(int seatIndex, @NotNull String gunName) {}

    /**
     * The type values the derivation reads.
     *
     * @param planeFacing          whether the type is authored in the plane flight basis
     * @param modelScale           the type's {@code ModelScale}
     * @param vehicleGunModelScale the type's {@code VehicleGunModelScale}
     * @param seatGuns             the passenger seats that mount a gun, by seat index
     */
    public record DriveableInputs(boolean planeFacing, double modelScale, float vehicleGunModelScale,
                                  @NotNull List<SeatGun> seatGuns) {}

    /**
     * The muzzles of a driveable model, in type-file units and convention.
     *
     * <p>The primary barrel comes first, with seat index {@code -1}, then one
     * entry per seat gun the model registers, as its {@code GunOrigin}.</p>
     */
    public static List<DerivedMuzzle> deriveMuzzles(@NotNull ModelDriveable model, @NotNull DriveableInputs inputs)
    {
        // The renderer scales the whole model by ModelScale, while type-file
        // points are not scaled, so the measured tip has to be scaled here.
        double modelScale = Math.max(1.0E-4D, inputs.modelScale());
        float gunScale = Math.max(0.001F, inputs.vehicleGunModelScale());
        List<DerivedMuzzle> derived = new ArrayList<>();
        if (model instanceof ModelVehicle vehicleModel)
        {
            Vec3 barrel = vehicleModel.getPrimaryBarrelMuzzle();
            if (barrel != null)
                derived.add(new DerivedMuzzle(-1, "barrel",
                    LegacyDriveableCoordinates.modelPixelsToTypeFile(barrel.scale(modelScale), inputs.planeFacing())));
        }

        for (SeatGun seatGun : inputs.seatGuns())
        {
            Vec3 muzzle = model.getRegisteredGunMuzzle(seatGun.gunName(), gunScale);
            if (muzzle == null)
                continue;
            // GunOrigin is the muzzle at rest, lifted by the legacy mounted-gunner
            // offset when the round is spawned. Subtracting that here makes the
            // suggested value land the shot on the measured barrel tip. Firing
            // carries it round the gun's pivot as the gun aims, so the rest-pose
            // measurement holds at every aim.
            Vector3f position = seatGunPointToGunOrigin(muzzle.scale(modelScale), inputs.planeFacing());
            Vec3 pivot = model.getRegisteredGunAimPivot(seatGun.gunName(), gunScale);
            derived.add(new DerivedMuzzle(seatGun.seatIndex(),
                "seat " + seatGun.seatIndex() + " (" + seatGun.gunName() + ")", position,
                pivot == null ? null : seatGunPointToGunOrigin(pivot.scale(16D * modelScale), inputs.planeFacing())));
        }
        return List.copyOf(derived);
    }

    /**
     * Offsets, in blocks, of an AA gun model's barrel muzzles from the gun's
     * position at rest, or an empty list when the model has fewer barrel groups
     * than {@code numBarrels} or one of them carries no geometry.
     */
    public static List<Vec3> deriveAAGunBarrelOffsets(@NotNull ModelAAGun model, int numBarrels)
    {
        ModelAAGun.BarrelOriginData data = model.getModelBarrelOriginData(numBarrels);
        if (data == null)
            return List.of();
        List<Vec3> offsets = new ArrayList<>();
        for (int barrel = 0; barrel < data.pivots().length; barrel++)
            offsets.add(AAGunBarrelGeometry.modelBarrelOffset(data.pivots()[barrel], data.muzzles()[barrel], 0F, 0F));
        return List.copyOf(offsets);
    }

    /** A scaled seat-gun model point in GunOrigin terms: type-file pixels, less the mounted offset. */
    private static Vector3f seatGunPointToGunOrigin(Vec3 modelPixels, boolean planeFacing)
    {
        Vector3f position = LegacyDriveableCoordinates.modelPixelsToTypeFile(modelPixels, planeFacing);
        position.y -= (float) (Driveable.PASSENGER_GUN_MOUNTED_OFFSET * 16D);
        return position;
    }
}
