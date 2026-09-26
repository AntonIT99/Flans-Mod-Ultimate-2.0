package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;

import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Developer tooling: measures where gun and driveable model muzzles are, so muzzle
 * flashes and shoot particles can be placed from the geometry the renderer draws
 * instead of guessed.
 *
 * <p>Run through {@code gradlew muzzleReport -PmodelSourceSet=<set> -PmodelPackage=<package>}.
 * Every gun and driveable model class under the package is constructed, which applies
 * its constructor-time {@code translateAll} and {@code flipAll}, then measured with
 * {@link ModelDriveable#measureMuzzle}. Output is two tab-separated tables.</p>
 *
 * <p>Gun columns are in model pixels, Y up, as the renderer sees them. The suggested
 * {@code animMuzzleFlashPoint} is for a flash centred on its own origin, such as
 * {@link ModelDefaultFlash}: the renderer scales by {@code flashScale} before
 * translating, so the point is the muzzle divided by 16 and by that scale. Driveable
 * rows add the muzzle in type-file coordinates, comparable with the authored
 * {@code BarrelPosition} and shoot points.</p>
 */
public final class MuzzleReport
{
    /** A measured muzzle further than this from the barrel attach point is flagged. */
    private static final double ATTACH_POINT_TOLERANCE = 2D;
    /** Matches the depth {@link ModelDriveable#measureMuzzle} centres the muzzle over. */
    private static final double MUZZLE_FACE_DEPTH = 3D;

    private MuzzleReport() {}

    public static void main(String[] args) throws IOException, URISyntaxException
    {
        if (args.length < 1)
            throw new IllegalArgumentException("Usage: MuzzleReport <package> [output file]");
        String packageName = args[0];
        List<String> classNames = findClasses(packageName);

        PrintStream out = args.length > 1 ? new PrintStream(Files.newOutputStream(Paths.get(args[1])), true, StandardCharsets.UTF_8) : System.out;
        List<String> gunRows = new ArrayList<>();
        List<String> driveableRows = new ArrayList<>();
        List<String> otherRows = new ArrayList<>();
        for (String className : classNames)
        {
            Object model;
            try
            {
                Class<?> type = Class.forName(className);
                if (Modifier.isAbstract(type.getModifiers())
                    || !(ModelGun.class.isAssignableFrom(type) || ModelDriveable.class.isAssignableFrom(type)))
                    continue;
                model = type.getConstructor().newInstance();
            }
            catch (Exception | LinkageError e)
            {
                otherRows.add(className + "\tfailed-to-construct\t" + e);
                continue;
            }
            if (model instanceof ModelGun gun)
                gunRows.add(measureGun(className, gun));
            else
                driveableRows.addAll(measureDriveable(className, (ModelDriveable) model));
        }

        out.println(String.join("\t", "model", "status", "muzzleSourceGroup", "muzzlePx", "bodyMuzzlePx",
            "muzzleFacePx", "barrelAttachPx", "attachDeltaPx", "declaredMuzzleFlashPoint",
            "declaredDefaultBarrelFlashPoint", "flashScale", "hasFlash", "suggestedAnimMuzzleFlashPoint", "notes"));
        gunRows.forEach(out::println);
        out.println();
        out.println(String.join("\t", "driveableModel", "weapon", "muzzleModelPx", "muzzleTypeFileCoords", "notes"));
        driveableRows.forEach(out::println);
        if (!otherRows.isEmpty())
        {
            out.println();
            otherRows.forEach(out::println);
        }
        if (out != System.out)
            out.close();
        System.err.println("Measured " + gunRows.size() + " gun and " + driveableRows.size() + " driveable weapon muzzles among "
            + classNames.size() + " classes under " + packageName + "; " + otherRows.size() + " failed to construct");
    }

    /**
     * Driveable weapon muzzles, in model pixels and in the type-file convention that
     * {@code BarrelPosition}, {@code ShootPoint*} and gun positions are written in.
     * Registered passenger guns are measured at scale 1, since the type's
     * {@code VehicleGunModelScale} is not known without the definition.
     */
    private static List<String> measureDriveable(String className, ModelDriveable model)
    {
        boolean plane = model instanceof ModelPlane;
        List<String> rows = new ArrayList<>();
        if (model instanceof ModelVehicle vehicle)
        {
            Vec3 barrel = vehicle.getPrimaryBarrelMuzzle();
            if (barrel != null)
                rows.add(driveableRow(className, "primaryBarrel", barrel, plane, ""));
        }
        model.gunModels.keySet().stream().sorted().forEach(name -> {
            Vec3 muzzle = model.getRegisteredGunMuzzle(name);
            if (muzzle != null)
                rows.add(driveableRow(className, "gun:" + name, muzzle, plane, "measured at VehicleGunModelScale 1"));
        });
        if (rows.isEmpty())
            rows.add(className + "\t\t\t\tno barrel or registered gun geometry");
        return rows;
    }

    private static String driveableRow(String className, String weapon, Vec3 muzzle, boolean plane, String notes)
    {
        Vector3f typeFile = LegacyDriveableCoordinates.modelPixelsToTypeFile(muzzle, plane);
        return String.join("\t", className, weapon, px(muzzle),
            String.format(Locale.ROOT, "%.2f %.2f %.2f", typeFile.x, typeFile.y, typeFile.z), notes);
    }

    private static String measureGun(String className, ModelGun model)
    {

        Map<String, ModelRendererTurbo[]> body = new LinkedHashMap<>();
        body.put("gunModel", model.gunModel);
        body.put("slideModel", model.slideModel);
        body.put("altslideModel", model.altslideModel);
        body.put("breakActionModel", model.breakActionModel);
        body.put("altbreakActionModel", model.altbreakActionModel);
        body.put("minigunBarrelModel", model.minigunBarrelModel);
        Map<String, ModelRendererTurbo[]> all = new LinkedHashMap<>(body);
        all.put("defaultBarrelModel", model.defaultBarrelModel);

        // Rotated parts at a gun's front are bipod legs, folded bayonets and slings far more
        // often than the bore, and the measurement reads them unrotated, so they are left out.
        Vec3 bodyMuzzle = measure(body, false);
        Vec3 muzzle = measure(all, false);
        Vec3 withRotated = measure(all, true);
        List<String> notes = new ArrayList<>();
        String source = "";
        if (muzzle != null)
        {
            for (Map.Entry<String, ModelRendererTurbo[]> group : all.entrySet())
            {
                Vec3 own = ModelDriveable.measureMuzzle(1F, unrotated(group.getValue()));
                if (own != null && Math.abs(own.x - muzzle.x) < 1.0E-4D)
                    source = source.isEmpty() ? group.getKey() : source + "+" + group.getKey();
            }
            if (withRotated != null && withRotated.x > muzzle.x + 0.5D)
                notes.add(String.format(Locale.ROOT, "rotated parts reach %.1f px further forward (bayonet, bipod or rotated barrel?)", withRotated.x - muzzle.x));
            if (bodyMuzzle != null && muzzle.x > bodyMuzzle.x + 0.5D)
                notes.add(String.format(Locale.ROOT, "default barrel reaches %.1f px past the body; with a barrel attachment the attachment model decides", muzzle.x - bodyMuzzle.x));
        }
        else if (withRotated != null)
        {
            notes.add("only rotated parts carry geometry; measure by hand");
        }

        Vector3f attach = model.getBarrelAttachPoint();
        Vec3 attachPx = attach == null ? null : new Vec3(attach.x * 16D, attach.y * 16D, attach.z * 16D);
        double attachDelta = bodyMuzzle == null || attachPx == null ? Double.NaN : bodyMuzzle.distanceTo(attachPx);
        float flashScale = model.getFlashScale() == 0F ? 1F : model.getFlashScale();

        String status;
        if (muzzle == null)
            status = "no-geometry";
        else if (attachPx == null || attachPx.lengthSqr() == 0D)
            status = "measured-no-attach-point";
        else if (attachDelta > ATTACH_POINT_TOLERANCE)
            status = "measured-attach-point-disagrees";
        else
            status = "measured-agrees";

        return String.join("	", className, status, source, px(muzzle), px(bodyMuzzle), faceSize(all, muzzle), px(attachPx),
            Double.isNaN(attachDelta) ? "" : String.format(Locale.ROOT, "%.2f", attachDelta),
            vector(model.getMuzzleFlashPoint()), vector(model.getDefaultBarrelFlashPoint()),
            String.format(Locale.ROOT, "%.3f", flashScale), Boolean.toString(model.isHasFlash()),
            muzzle == null ? "" : blocks(muzzle, flashScale), String.join("; ", notes));
    }

    /** Height and width of the unrotated geometry forming the muzzle face, the bore end a flash is sized against. */
    private static String faceSize(Map<String, ModelRendererTurbo[]> groups, Vec3 muzzle)
    {
        if (muzzle == null)
            return "";
        double[] face = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (ModelRendererTurbo[] group : groups.values())
        {
            for (ModelRendererTurbo part : unrotated(group))
            {
                double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
                if (!part.appendFaceBounds(bounds) || part.rotationPointX + bounds[3] < muzzle.x - MUZZLE_FACE_DEPTH)
                    continue;
                face[0] = Math.min(face[0], part.rotationPointY + bounds[1]);
                face[1] = Math.min(face[1], part.rotationPointZ + bounds[2]);
                face[2] = Math.max(face[2], part.rotationPointY + bounds[4]);
                face[3] = Math.max(face[3], part.rotationPointZ + bounds[5]);
            }
        }
        return face[0] == Double.POSITIVE_INFINITY ? "" : String.format(Locale.ROOT, "%.2f %.2f", face[2] - face[0], face[3] - face[1]);
    }

    private static Vec3 measure(Map<String, ModelRendererTurbo[]> groups, boolean includeRotated)
    {
        return ModelDriveable.measureMuzzle(1F, groups.values().stream()
            .map(group -> includeRotated ? group : unrotated(group))
            .toArray(ModelRendererTurbo[][]::new));
    }

    private static ModelRendererTurbo[] unrotated(ModelRendererTurbo[] parts)
    {
        if (parts == null)
            return new ModelRendererTurbo[0];
        return Arrays.stream(parts)
            .filter(part -> part != null && part.rotateAngleX == 0F && part.rotateAngleY == 0F && part.rotateAngleZ == 0F)
            .toArray(ModelRendererTurbo[]::new);
    }

    private static String blocks(Vec3 pixels, float flashScale)
    {
        return String.format(Locale.ROOT, "[%s,%s,%s]", round(pixels.x / 16D / flashScale),
            round(pixels.y / 16D / flashScale), round(pixels.z / 16D / flashScale));
    }

    private static String px(Vec3 vector)
    {
        return vector == null ? "" : String.format(Locale.ROOT, "%.3f %.3f %.3f", vector.x, vector.y, vector.z);
    }

    private static String vector(Vector3f vector)
    {
        return vector == null ? "" : String.format(Locale.ROOT, "%.5f %.5f %.5f", vector.x, vector.y, vector.z);
    }

    private static String round(double value)
    {
        String text = String.format(Locale.ROOT, "%.5f", value).replaceAll("0+$", "");
        return text.endsWith(".") ? text + "0" : text;
    }

    private static List<String> findClasses(String packageName) throws IOException, URISyntaxException
    {
        String path = packageName.replace('.', '/');
        List<String> names = new ArrayList<>();
        Enumeration<URL> roots = MuzzleReport.class.getClassLoader().getResources(path);
        while (roots.hasMoreElements())
        {
            URL root = roots.nextElement();
            if (!"file".equals(root.getProtocol()))
                continue;
            Path directory = Paths.get(root.toURI());
            // A case-insensitive file system finds the directory under any casing, but class names must match it.
            String realPath = directory.toRealPath().toString().replace('\\', '/');
            if (!realPath.endsWith(path))
                throw new IllegalArgumentException("Package casing differs from its directory " + realPath + "; pass the package exactly as declared.");
            try (Stream<Path> files = Files.walk(directory))
            {
                files.filter(file -> file.toString().endsWith(".class") && !file.getFileName().toString().contains("$"))
                    .forEach(file -> {
                        String relative = directory.relativize(file).toString().replace('\\', '/').replace('/', '.');
                        names.add(packageName + "." + relative.substring(0, relative.length() - ".class".length()));
                    });
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }
}
