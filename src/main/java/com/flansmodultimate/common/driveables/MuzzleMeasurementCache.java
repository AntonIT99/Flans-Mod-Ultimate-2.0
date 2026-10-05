package com.flansmodultimate.common.driveables;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.client.model.ModelDriveable;
import com.flansmod.client.model.ModelMG;
import com.flansmod.client.model.ModelMecha;
import com.flansmod.client.model.ModelPlane;
import com.flansmod.client.model.ModelVehicle;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.MuzzleMeasurements;
import com.flansmodultimate.common.entity.AAGunBarrelGeometry;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.PlatformPaths;
import com.flansmodultimate.util.ClassLoaderUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.ModelClassResolver;
import com.flansmodultimate.util.ModCachePaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Keeps the results of {@link ModelMuzzleMeasurement} between runs, so a client
 * or server restarted on the same content skips loading and measuring every model.
 *
 * <p>The cache is coarse on purpose: one key covers every content pack, the
 * user's categories, the option deciding which packs are corrected, and the code
 * that parses types and measures models. Any change to any of them measures
 * everything again; an unchanged restart takes every result from the file. The
 * results are replayed onto freshly read types, which come out of the same files
 * exactly as they did when the results were recorded.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MuzzleMeasurementCache
{
    /** Raised whenever what is recorded, or how it is replayed, changes. */
    private static final int FORMAT_VERSION = 3;
    private static final String FILE_NAME = "muzzle-measurements.json";
    private static final Gson GSON = new GsonBuilder().create();

    /** The code whose changes change what a measurement returns. */
    private static final List<Class<?>> MEASURING_CODE = List.of(
        ModelMuzzleMeasurement.class, MuzzleMeasurements.class, ModelRendererTurbo.class, ModelBase.class,
        ModelDriveable.class, ModelVehicle.class, ModelPlane.class, ModelMecha.class, ModelAAGun.class, ModelMG.class,
        LegacyDriveableCoordinates.class, AAGunBarrelGeometry.class, ClassLoaderUtils.class, ModelClassResolver.class,
        InfoType.class, DriveableType.class, AAGunType.class, GunType.class, TypeFile.class, ShootPoint.class, SeatInfo.class);

    /** What one measurement pass did, in the order it did it. */
    static final class Results
    {
        final Map<String, Muzzle> deployedGuns = new TreeMap<>();
        final Map<String, Barrels> aaGuns = new TreeMap<>();
        final Map<String, List<Move>> driveables = new TreeMap<>();
        final Map<String, List<Spread>> mounts = new TreeMap<>();
        int models;
        int failed;

        int mountCount()
        {
            return mounts.values().stream().mapToInt(List::size).sum();
        }
    }

    /** Measured AA gun barrel pivots and muzzles, in model pixels. */
    record Barrels(double[][] pivots, double[][] muzzles) {}

    /** Measured deployable-gun pivot and muzzle, in model pixels. */
    record Muzzle(double[] pivot, double[] muzzle) {}

    /** One shoot point or {@code GunOrigin} moved onto the model, in type-file pixels. */
    record Move(boolean gunOrigin, boolean secondary, int index, float x, float y, float z) {}

    /** The barrels of one shoot point or seat gun, as offsets from its muzzle in type-file pixels. */
    record Spread(boolean gunOrigin, boolean secondary, int index, float[][] offsets)
    {
        Spread(boolean gunOrigin, boolean secondary, int index, List<Vector3f> offsets)
        {
            this(gunOrigin, secondary, index,
                offsets.stream().map(offset -> new float[] { offset.x, offset.y, offset.z }).toArray(float[][]::new));
        }

        List<Vector3f> offsetVectors()
        {
            return Stream.of(offsets).map(offset -> new Vector3f(offset[0], offset[1], offset[2])).toList();
        }
    }

    private record Stored(int version, String key, Results results) {}

    static String typeKey(InfoType type)
    {
        // The shortname as the pack writes it: unique within its pack, and known before aliases are resolved.
        return type.getContentPack().getName() + "/" + type.getOriginalShortName();
    }

    static void recordBarrels(Results results, AAGunType type, Vec3[] pivots, Vec3[] muzzles)
    {
        results.aaGuns.put(typeKey(type), new Barrels(toArrays(pivots), toArrays(muzzles)));
    }

    static void recordDeployedGunMuzzle(Results results, GunType type, Vec3 pivot, Vec3 muzzle)
    {
        results.deployedGuns.put(typeKey(type), new Muzzle(toArray(pivot), toArray(muzzle)));
    }

    static void recordMove(Results results, DriveableType type, Move move)
    {
        results.driveables.computeIfAbsent(typeKey(type), ignored -> new ArrayList<>()).add(move);
    }

    static void recordSpread(Results results, DriveableType type, Spread spread)
    {
        results.mounts.computeIfAbsent(typeKey(type), ignored -> new ArrayList<>()).add(spread);
    }

    /**
     * Applies stored results to types read from the same content they were measured on.
     *
     * @return how many shoot points and {@code GunOrigin}s were moved
     */
    static int apply(Results results, List<InfoType> types)
    {
        int moved = 0;
        for (InfoType type : types)
        {
            String key = typeKey(type);
            if (type instanceof GunType gun && results.deployedGuns.containsKey(key))
            {
                Muzzle muzzle = results.deployedGuns.get(key);
                gun.setMeasuredDeployableMuzzle(toVector(muzzle.pivot()), toVector(muzzle.muzzle()));
            }
            else if (type instanceof AAGunType aaGun && results.aaGuns.containsKey(key))
            {
                Barrels barrels = results.aaGuns.get(key);
                aaGun.setMeasuredBarrels(toVectors(barrels.pivots()), toVectors(barrels.muzzles()));
            }
            else if (type instanceof DriveableType driveable)
            {
                for (Move move : results.driveables.getOrDefault(key, List.of()))
                {
                    Vector3f position = new Vector3f(move.x(), move.y(), move.z());
                    boolean applied = move.gunOrigin()
                        ? driveable.applyMeasuredGunOrigin(move.index(), position)
                        : driveable.applyMeasuredShootPoint(move.secondary(), move.index(), position);
                    moved += applied ? 1 : 0;
                }
                for (Spread spread : results.mounts.getOrDefault(key, List.of()))
                {
                    if (spread.gunOrigin())
                        driveable.applyMeasuredGunBarrels(spread.index(), spread.offsetVectors());
                    else
                        driveable.applyMeasuredBarrels(spread.secondary(), spread.index(), spread.offsetVectors());
                }
            }
        }
        return moved;
    }

    /** The results stored under {@code key}, or {@code null} when there are none or they were stored for other content. */
    @Nullable
    static Results load(String key)
    {
        Path file = file();
        if (!Files.isRegularFile(file))
            return null;
        try
        {
            return fromJson(Files.readString(file, StandardCharsets.UTF_8), key);
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.warn("Ignoring unreadable muzzle measurement cache {}: {}", file, e.toString());
            return null;
        }
    }

    static void save(String key, Results results)
    {
        Path file = file();
        try
        {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(temp, toJson(key, results), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.warn("Could not write the muzzle measurement cache {}: {}", file, e.toString());
        }
    }

    static String toJson(String key, Results results)
    {
        return GSON.toJson(new Stored(FORMAT_VERSION, key, results));
    }

    /** The results in {@code json}, or {@code null} when they were stored for another key or format. */
    @Nullable
    static Results fromJson(String json, String key)
    {
        Stored stored = GSON.fromJson(json, Stored.class);
        return stored != null && stored.version() == FORMAT_VERSION && key.equals(stored.key()) ? stored.results() : null;
    }

    static Path file()
    {
        return ModCachePaths.root().resolve(FILE_NAME);
    }

    /**
     * A digest of everything a measurement depends on: every content pack, the
     * user's categories, which packs are corrected, and the measuring code.
     */
    static String key(List<IContentProvider> contentPacks, boolean correctFlanFolderPacks)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "format " + FORMAT_VERSION + " correct " + correctFlanFolderPacks
                + " defaultCategories " + ContentLoadingConfig.isUseDefaultCategories());
            for (Class<?> code : MEASURING_CODE)
                update(digest, code.getName(), classBytes(code));
            // Mods register their packs from their constructors, which run in parallel,
            // so the packs are taken in an order of their own rather than the load order.
            List<IContentProvider> packs = contentPacks.stream()
                .sorted(Comparator.comparing(IContentProvider::getName).thenComparing(pack -> pack.getPath().toString()))
                .toList();
            // Logical packs packaged in one mod share its jar or folder, which is read once.
            Set<Path> fingerprinted = new HashSet<>();
            for (IContentProvider pack : packs)
            {
                update(digest, "pack " + pack.getName() + " " + pack.isPreprocessed());
                if (fingerprinted.add(pack.getPath()))
                    fingerprint(digest, pack.getPath(), false);
                // A packaged pack in development keeps its model classes outside its content root.
                Path models = modelRoot(pack);
                if (models != null && !models.startsWith(pack.getPath()) && fingerprinted.add(models))
                    fingerprint(digest, models, false);
            }
            fingerprint(digest, PlatformPaths.configDir().resolve(FlansMod.MOD_ID), true);
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException | IOException | RuntimeException e)
        {
            // Without a key nothing can be trusted, so every run measures again.
            FlansLog.log.warn("Could not fingerprint the content for the muzzle measurement cache: {}", e.toString());
            return "unkeyed-" + System.nanoTime();
        }
    }

    @Nullable
    private static Path modelRoot(IContentProvider pack)
    {
        try
        {
            return pack.getModelPath("Model", null).getParent();
        }
        catch (RuntimeException e)
        {
            // An archive-backed pack reads its models from the archive, which its own fingerprint covers.
            return null;
        }
    }

    /**
     * An archive by its size and time; a folder by the name and content of every
     * definition and model file in it, and of every category file when
     * {@code categories} is set. Content rather than time, because startup rewrites
     * some of them, such as the default categories, unchanged.
     */
    private static void fingerprint(MessageDigest digest, Path path, boolean categories) throws IOException
    {
        if (Files.isRegularFile(path))
        {
            update(digest, path.getFileName() + " " + Files.size(path) + " " + Files.getLastModifiedTime(path).toMillis());
            return;
        }
        if (!Files.isDirectory(path))
            return;
        try (Stream<Path> files = Files.walk(path))
        {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList())
            {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".zip") || name.endsWith(".jar"))
                    update(digest, path.relativize(file) + " " + Files.size(file) + " " + Files.getLastModifiedTime(file).toMillis());
                else if (name.endsWith(".txt") || name.endsWith(".class") || name.endsWith(".java")
                    || categories && name.endsWith(".json"))
                    update(digest, path.relativize(file).toString(), Files.readAllBytes(file));
            }
        }
    }

    private static byte[] classBytes(Class<?> type)
    {
        try (InputStream in = type.getResourceAsStream(type.getSimpleName() + ".class"))
        {
            return in == null ? new byte[0] : in.readAllBytes();
        }
        catch (IOException e)
        {
            return new byte[0];
        }
    }

    private static void update(MessageDigest digest, String text, byte... bytes)
    {
        digest.update(text.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(bytes);
    }

    private static double[][] toArrays(Vec3[] vectors)
    {
        double[][] arrays = new double[vectors.length][];
        for (int i = 0; i < vectors.length; i++)
            arrays[i] = new double[] { vectors[i].x, vectors[i].y, vectors[i].z };
        return arrays;
    }

    private static double[] toArray(Vec3 vector)
    {
        return new double[] { vector.x, vector.y, vector.z };
    }

    private static Vec3[] toVectors(double[][] arrays)
    {
        Vec3[] vectors = new Vec3[arrays.length];
        for (int i = 0; i < arrays.length; i++)
            vectors[i] = new Vec3(arrays[i][0], arrays[i][1], arrays[i][2]);
        return vectors;
    }

    private static Vec3 toVector(double[] array)
    {
        return new Vec3(array[0], array[1], array[2]);
    }
}
