package com.flansmodultimate.client.model;

import com.flansmod.client.model.*;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.weapons.DerivedMuzzle;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns muzzles measured off a loaded model into the type-file values that fire
 * from them: the primary {@code ShootPointPrimary}/{@code BarrelPosition}, each
 * seat's {@code GunOrigin}, and the AA gun muzzle offsets that
 * {@link AAGunBarrelGeometry#legacyBarrelFor} turns into {@code Barrel} lines.
 *
 * <p>
 * Takes the few type values it needs as plain inputs rather than a loaded
 * type, so the {@code /flandebug shootpoint} command and the offline
 * {@code shootPointSync} tooling derive exactly the same values.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MuzzleMeasurements
{
    /** A passenger seat mounting a gun, and the model part name its gun is registered under. */
    public record SeatGun(int seatIndex, @NotNull String gunName)
    {}

    /**
     * The type values the derivation reads.
     *
     * @param planeFacing
     *            whether the type is authored in the plane flight basis
     * @param modelScale
     *            the type's {@code ModelScale}
     * @param vehicleGunModelScale
     *            the type's {@code VehicleGunModelScale}
     * @param seatGuns
     *            the passenger seats that mount a gun, by seat index
     */
    public record DriveableInputs(boolean planeFacing, double modelScale, float vehicleGunModelScale, @NotNull List<SeatGun> seatGuns)
    {}

    /**
     * The muzzles of a driveable model, in type-file units and convention.
     *
     * <p>
     * The primary barrel comes first, with seat index {@code -1}, then one
     * entry per seat gun the model registers, as its {@code GunOrigin}.
     * </p>
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
                derived.add(new DerivedMuzzle(-1, "barrel", LegacyDriveableCoordinates.modelPixelsToTypeFile(barrel.scale(modelScale), inputs.planeFacing())));
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
            derived.add(new DerivedMuzzle(seatGun.seatIndex(), "seat " + seatGun.seatIndex() + " (" + seatGun.gunName() + ")", position,
                pivot == null ? null : seatGunPointToGunOrigin(pivot.scale(16D * modelScale), inputs.planeFacing())));
        }
        return List.copyOf(derived);
    }

    /**
     * The primary barrel's muzzle in type-file terms, measured on the tube nearest
     * {@code hint} when the model's main armament ends in several tubes, or
     * {@code null} when the model has no primary barrel.
     *
     * @param hint
     *            the point, in type-file pixels, the type fires its primary weapon from now
     */
    @Nullable
    public static DerivedMuzzle derivePrimaryBarrelNear(@NotNull ModelDriveable model, @NotNull DriveableInputs inputs, @NotNull Vector3f hint)
    {
        if (!(model instanceof ModelVehicle vehicleModel))
            return null;
        Vec3 barrel = vehicleModel.getPrimaryBarrelMuzzleNear(typeFileToModelPixels(hint, inputs));
        return barrel == null ? null : new DerivedMuzzle(-1, "barrel", toTypeFile(barrel, inputs));
    }

    /**
     * The muzzle of every tube of the primary armament in type-file terms: one for
     * a single gun, four for a quad mount, none when the model has no primary barrel.
     */
    public static List<DerivedMuzzle> derivePrimaryBarrels(@NotNull ModelDriveable model, @NotNull DriveableInputs inputs)
    {
        if (!(model instanceof ModelVehicle vehicleModel))
            return List.of();
        List<Vec3> barrels = vehicleModel.getPrimaryBarrelMuzzles();
        List<DerivedMuzzle> derived = new ArrayList<>();
        for (int barrel = 0; barrel < barrels.size(); barrel++)
            derived.add(new DerivedMuzzle(-1, barrels.size() == 1 ? "barrel" : "barrel " + barrel, toTypeFile(barrels.get(barrel), inputs)));
        return List.copyOf(derived);
    }

    /**
     * The muzzle of every barrel of a seat's registered gun in {@code GunOrigin}
     * terms, at rest: two for a twin mount, one for a single gun, none when the
     * model does not register the gun.
     */
    public static List<Vector3f> deriveSeatGunBarrels(@NotNull ModelDriveable model, @NotNull DriveableInputs inputs, @NotNull SeatGun seatGun)
    {
        double modelScale = Math.max(1.0E-4D, inputs.modelScale());
        float gunScale = Math.max(0.001F, inputs.vehicleGunModelScale());
        return model.getRegisteredGunMuzzles(seatGun.gunName(), gunScale).stream().map(muzzle -> seatGunPointToGunOrigin(muzzle.scale(modelScale), inputs.planeFacing())).toList();
    }

    /**
     * Each barrel of a multi-barrel mount relative to the point a type fires the
     * mount from, in type-file pixels, or an empty list for fewer than two barrels.
     *
     * <p>
     * The spread is kept round the authored point rather than moving it: a pack
     * trusted as written keeps its muzzle, and a corrected one has already been
     * moved onto the model. A point on one barrel, as a twin authored on its left
     * gun, is that barrel; any other point stands for the whole mount and sits at
     * the middle of its barrels.
     * </p>
     *
     * @param barrels
     *            each barrel's muzzle, in type-file pixels
     * @param authored
     *            the point the type fires the mount from, in the same terms
     */
    public static List<Vector3f> barrelSpread(@NotNull List<Vector3f> barrels, @NotNull Vector3f authored)
    {
        if (barrels.size() < 2)
            return List.of();

        // Barrels differ in length, and a trusted point need not reach the muzzle,
        // so only the lateral and vertical position says which barrel a point is on.
        double spacing = Double.POSITIVE_INFINITY;
        for (int a = 0; a < barrels.size(); a++)
        {
            for (int b = a + 1; b < barrels.size(); b++)
                spacing = Math.min(spacing, crossDistance(barrels.get(a), barrels.get(b)));
        }
        Vector3f anchor = null;
        for (Vector3f barrel : barrels)
        {
            if (crossDistance(barrel, authored) < spacing * 0.5D)
                anchor = barrel;
        }
        if (anchor == null)
        {
            anchor = new Vector3f();
            for (Vector3f barrel : barrels)
                Vector3f.add(anchor, barrel, anchor);
            anchor.scale(1F / barrels.size());
        }

        List<Vector3f> offsets = new ArrayList<>();
        for (Vector3f barrel : barrels)
            offsets.add(Vector3f.sub(barrel, anchor, null));
        return List.copyOf(offsets);
    }

    private static double crossDistance(Vector3f a, Vector3f b)
    {
        return Math.hypot(a.y - b.y, a.z - b.z);
    }

    /** A gun tube ending within this many model pixels of a point is the gun it fires from. */
    private static final double ON_TUBE = 2.5D;
    /** The nearest tube end to a mirrored point is at least this far, in model pixels... */
    private static final double OFF_TUBE = 4D;
    /** ...and this many times further than the one its mirror image sits on. */
    private static final double OFF_TUBE_RATIO = 3D;
    /** Points closer to the centreline than this, in type-file pixels, have no side to be on. */
    private static final double CENTRELINE = 1D;

    /**
     * Whether a muzzle was written on the wrong side of the model: a gun tube ends
     * at its mirror image across the centreline, while none ends anywhere near the
     * point itself. Pack authors who read coordinates off their modelling tool,
     * before the constructor flips the model, wrote such points with Z reversed;
     * the mistake only shows once the shot is seen leaving the other side of the
     * turret. A point with no tube near either side is left alone: there is
     * nothing to tell which side is meant.
     *
     * @param muzzle
     *            the point the type fires from, in type-file pixels
     */
    public static boolean isMirroredMuzzle(@NotNull ModelDriveable model, @NotNull DriveableInputs inputs, @NotNull Vector3f muzzle)
    {
        if (Math.abs(muzzle.z) < CENTRELINE)
            return false;
        Vec3 authored = typeFileToModelPixels(muzzle, inputs);
        Vec3 mirrored = new Vec3(authored.x, authored.y, -authored.z);
        // Vehicle models face +X; aircraft models face the other way.
        double onMirror = model.distanceToTubeEnd(mirrored, inputs.planeFacing());
        double onAuthored = model.distanceToTubeEnd(authored, inputs.planeFacing());
        return onMirror <= ON_TUBE && onAuthored >= Math.max(OFF_TUBE, OFF_TUBE_RATIO * onMirror);
    }

    /** A type-file point, in pixels, as the unscaled model pixels it is drawn at. */
    public static Vec3 typeFileToModelPixels(@NotNull Vector3f typeFile, @NotNull DriveableInputs inputs)
    {
        double modelScale = Math.max(1.0E-4D, inputs.modelScale());
        return new Vec3(inputs.planeFacing() ? -typeFile.x : typeFile.x, typeFile.y, typeFile.z).scale(1D / modelScale);
    }

    private static Vector3f toTypeFile(Vec3 modelPixels, DriveableInputs inputs)
    {
        double modelScale = Math.max(1.0E-4D, inputs.modelScale());
        return LegacyDriveableCoordinates.modelPixelsToTypeFile(modelPixels.scale(modelScale), inputs.planeFacing());
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
