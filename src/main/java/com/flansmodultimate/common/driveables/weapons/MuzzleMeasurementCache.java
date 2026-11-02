package com.flansmodultimate.common.driveables.weapons;

import com.flansmod.client.model.*;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.MuzzleMeasurements;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.PlatformPaths;
import com.flansmodultimate.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Keeps the results of {@link ModelMuzzleMeasurement} between runs, so a client
 * or server restarted on the same content skips loading and measuring every model.
 *
 * <p>
 * Each pack keeps its own results, keyed on its category-resolved definitions and
 * any seat-gun definition providers. Model sources and measurement code remain
 * shared dependencies because a model can load classes from another pack.
 * Fingerprints describe content rather than build timestamps.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MuzzleMeasurementCache
{
    /** Raised whenever what is recorded, or how it is replayed, changes. */
    private static final int FORMAT_VERSION = 5;
    private static final String FILE_NAME = "muzzle-measurements.json";
    private static final Gson GSON = new GsonBuilder().create();

    /** The code whose changes change what a measurement returns. */
    private static final List<Class<?>> MEASURING_CODE = List.of(ModelMuzzleMeasurement.class, MuzzleMeasurements.class, ModelRendererTurbo.class, ModelBase.class, ModelDriveable.class,
        ModelVehicle.class, ModelPlane.class, ModelMecha.class, ModelAAGun.class, ModelMG.class, LegacyDriveableCoordinates.class, AAGunBarrelGeometry.class, ClassLoaderUtils.class,
        ModelClassResolver.class, ModelResourceResolver.class, InfoType.class, DriveableType.class, PlaneType.class, VehicleType.class, MechaType.class, AAGunType.class, GunType.class, TypeFile.class,
        ShootPoint.class, SeatInfo.class);

    /** What one measurement pass did, in the order it did it. */
    static final class Results
    {
        final Map<String, Muzzle> deployedGuns = new TreeMap<>();
        final Map<String, Barrels> aaGuns = new TreeMap<>();
        final Map<String, List<Move>> driveables = new TreeMap<>();
        final Map<String, List<Spread>> mounts = new TreeMap<>();
        final Map<String, List<Pivot>> pivots = new TreeMap<>();
        int models;
        int failed;

        int mountCount()
        {
            return mounts.values().stream().mapToInt(List::size).sum();
        }

    }

    /** Measured AA gun barrel pivots and muzzles, in model pixels. */
    // Plain data carrier that is never compared or used as a key.
    @SuppressWarnings("java:S6218")
    record Barrels(double[][] pivots, double[][] muzzles)
    {}

    /** Measured deployable-gun pivot and muzzle, in model pixels. */
    // Plain data carrier that is never compared or used as a key.
    @SuppressWarnings("java:S6218")
    record Muzzle(double[] pivot, double[] muzzle)
    {}

    /** One shoot point or {@code GunOrigin} moved onto the model, in type-file pixels. */
    record Move(boolean gunOrigin, boolean secondary, int index, float x, float y, float z)
    {}

    /** The point one seat gun turns round as it aims, in type-file pixels. */
    record Pivot(int seat, float x, float y, float z)
    {}

    /** The barrels of one shoot point or seat gun, as offsets from its muzzle in type-file pixels. */
    // Plain data carrier; Gson reads secondary from JSON even though current measurements only spread primary banks.
    @SuppressWarnings({"java:S6218", "SameParameterValue"})
    record Spread(boolean gunOrigin, boolean secondary, int index, float[][] offsets)
    {
        Spread(boolean gunOrigin, boolean secondary, int index, List<Vector3f> offsets)
        {
            this(gunOrigin, secondary, index, offsets.stream().map(offset -> new float[]{offset.x, offset.y, offset.z}).toArray(float[][]::new));
        }

        List<Vector3f> offsetVectors()
        {
            return Stream.of(offsets).map(offset -> new Vector3f(offset[0], offset[1], offset[2])).toList();
        }
    }

    record Entry(Map<String, String> inputs, Results results)
    {}

    record FileHash(long size, String modified, String hash)
    {}

    static final class State
    {
        final Map<String, Entry> packs = new TreeMap<>();
        final Map<String, FileHash> files = new TreeMap<>();
    }

    // Gson also invokes the constructor with versions read from older cache files.
    @SuppressWarnings("SameParameterValue")
    private record Stored(int version, State state)
    {}

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

    static void recordPivot(Results results, DriveableType type, Pivot pivot)
    {
        results.pivots.computeIfAbsent(typeKey(type), ignored -> new ArrayList<>()).add(pivot);
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
                moved += applyDriveable(results, key, driveable);
        }
        return moved;
    }

    /** @return how many shoot points and {@code GunOrigin}s of the driveable were moved */
    private static int applyDriveable(Results results, String key, DriveableType driveable)
    {
        int moved = 0;
        for (Move move : results.driveables.getOrDefault(key, List.of()))
        {
            Vector3f position = new Vector3f(move.x(), move.y(), move.z());
            boolean applied = move.gunOrigin() ? driveable.applyMeasuredGunOrigin(move.index(), position) : driveable.applyMeasuredShootPoint(move.secondary(), move.index(), position);
            moved += applied ? 1 : 0;
        }
        for (Spread spread : results.mounts.getOrDefault(key, List.of()))
        {
            if (spread.gunOrigin())
                driveable.applyMeasuredGunBarrels(spread.index(), spread.offsetVectors());
            else
                driveable.applyMeasuredBarrels(spread.secondary(), spread.index(), spread.offsetVectors());
        }
        for (Pivot pivot : results.pivots.getOrDefault(key, List.of()))
            driveable.applyMeasuredGunPivot(pivot.seat(), new Vector3f(pivot.x(), pivot.y(), pivot.z()));
        return moved;
    }

    /** Read once per startup; an old format is deliberately remeasured once. */
    static State load()
    {
        Path file = file();
        if (!Files.isRegularFile(file))
        {
            FlansLog.log.info("No muzzle measurement cache at {}; measuring models once.", file);
            return new State();
        }
        try
        {
            return fromJson(Files.readString(file, StandardCharsets.UTF_8));
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.warn("Ignoring unreadable muzzle measurement cache {}: {}", file, e.toString());
            return new State();
        }
    }

    static void save(State state)
    {
        Path file = file();
        try
        {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(temp, toJson(state), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.warn("Could not write the muzzle measurement cache {}: {}", file, e.toString());
        }
    }

    static String toJson(State state)
    {
        return GSON.toJson(new Stored(FORMAT_VERSION, state));
    }

    // Gson can overwrite initialized final fields with explicit JSON nulls.
    @SuppressWarnings("ConstantValue")
    static State fromJson(String json)
    {
        Stored stored = GSON.fromJson(json, Stored.class);
        if (stored == null || stored.version() != FORMAT_VERSION || stored.state() == null)
        {
            FlansLog.log.info("Muzzle measurement cache format changed; measuring models once.");
            return new State();
        }
        if (stored.state().packs == null || stored.state().files == null || stored.state().packs.values().stream().anyMatch(entry -> entry == null || entry.inputs() == null || entry.results() == null)
            || stored.state().files.values().stream().anyMatch(hash -> hash == null || hash.modified() == null || hash.hash() == null))
            throw new IllegalArgumentException("Incomplete muzzle measurement cache");
        return stored.state();
    }

    @Nullable
    static Results matching(@Nullable Entry stored, Map<String, String> inputs)
    {
        return stored != null && inputs.equals(stored.inputs()) ? stored.results() : null;
    }

    /** Names of changed dependencies, rather than an opaque digest mismatch. */
    static List<String> changedInputs(@Nullable Entry stored, Map<String, String> inputs)
    {
        if (stored == null)
            return List.of("no stored measurements");
        Set<String> names = new TreeSet<>(inputs.keySet());
        names.addAll(stored.inputs().keySet());
        return names.stream().filter(name -> !Objects.equals(inputs.get(name), stored.inputs().get(name))).toList();
    }

    static Path file()
    {
        return ModCachePaths.root().resolve(FILE_NAME);
    }

    /** Definition digests already include applied categories and aliases. */
    static Map<IContentProvider, Map<String, String>> inputs(List<IContentProvider> contentPacks, Map<IContentProvider, String> definitionDigests, List<InfoType> types, boolean correctFlanFolderPacks,
        State state)
    {
        Map<IContentProvider, Map<String, String>> result = new LinkedHashMap<>();
        try
        {
            List<IContentProvider> packs = sortedPacks(contentPacks);
            Map<String, String> shared = sharedInputs(packs, correctFlanFolderPacks, state);
            Map<IContentProvider, String> definitions = new HashMap<>();
            for (IContentProvider pack : packs)
                definitions.put(pack, definitionInput(pack, definitionDigests, state));
            Map<IContentProvider, Set<IContentProvider>> dependencies = seatGunDependencies(types);
            for (IContentProvider pack : packs)
            {
                Map<String, String> inputs = new TreeMap<>(shared);
                inputs.put("definitions " + pack.getName(), definitions.get(pack));
                for (IContentProvider dependency : dependencies.getOrDefault(pack, Set.of()))
                    inputs.put("seat gun definitions " + dependency.getName(), definitions.get(dependency));
                result.put(pack, inputs);
            }
        }
        catch (IOException | RuntimeException e)
        {
            // Never replay stale measurements when an input cannot be read.
            FlansLog.log.warn("Could not fingerprint content for the muzzle measurement cache: {}", e.toString());
            result.clear();
            for (IContentProvider pack : contentPacks)
                result.put(pack, Map.of("unreadable inputs", UUID.randomUUID().toString()));
        }
        return result;
    }

    private static Map<String, String> sharedInputs(List<IContentProvider> packs, boolean correctFlanFolderPacks, State state) throws IOException
    {
        Map<String, String> shared = new TreeMap<>();
        shared.put("options", "correct " + correctFlanFolderPacks + " defaultCategories " + ContentLoadingConfig.isUseDefaultCategories());
        for (Class<?> code : MEASURING_CODE)
        {
            MessageDigest digest = newDigest();
            update(digest, code.getName(), classBytes(code));
            shared.put("code " + code.getName(), hex(digest));
        }
        shared.put("pack providers", packs.stream().map(pack -> pack.getName() + " " + pack.isPreprocessed() + " " + pack.getModelSourceId()).toList().toString());
        Map<Path, String> modelDigests = new HashMap<>();
        for (IContentProvider pack : packs)
        {
            // Several logical packs can share one archive or compiled model tree.
            Path models = pack.isArchive() ? pack.getPath() : modelRoot(pack);
            if (models != null)
                shared.put("models " + models, modelInput(models, modelDigests, state));
            if (!pack.isArchive())
            {
                Path resources = pack.getAssetsPath().resolve("models");
                shared.put("model resources " + resources, modelInput(resources, modelDigests, state));
            }
        }
        // Legacy imported models may also read loose OBJ files outside their pack.
        for (String root : List.of("resources/models", "resources/mod/models", "Flan", "flan"))
        {
            MessageDigest resources = newDigest();
            fingerprintMeshes(resources, PlatformPaths.gameDir().resolve(root), state);
            shared.put("loose model resources " + root, hex(resources));
        }
        return shared;
    }

    private static String modelInput(Path models, Map<Path, String> modelDigests, State state) throws IOException
    {
        String known = modelDigests.get(models);
        if (known != null)
            return known;
        MessageDigest digest = newDigest();
        fingerprint(digest, models, false, false, state);
        String hash = hex(digest);
        modelDigests.put(models, hash);
        return hash;
    }

    private static String definitionInput(IContentProvider pack, Map<IContentProvider, String> definitionDigests, State state) throws IOException
    {
        String known = definitionDigests.get(pack);
        if (known != null)
            return known;
        MessageDigest digest = newDigest();
        fingerprint(digest, pack.getPath(), true, false, state);
        fingerprint(digest, PlatformPaths.configDir().resolve(FlansMod.MOD_ID), true, true, state);
        return hex(digest);
    }

    private static Map<IContentProvider, Set<IContentProvider>> seatGunDependencies(List<InfoType> types)
    {
        Map<IContentProvider, Set<IContentProvider>> dependencies = new HashMap<>();
        for (InfoType type : types)
        {
            if (!(type instanceof DriveableType driveable))
                continue;
            for (int seat = 1; seat <= driveable.getNumPassengers(); seat++)
            {
                SeatInfo info = driveable.getSeat(seat);
                if (info != null && info.getGunType() != null)
                    dependencies.computeIfAbsent(type.getContentPack(), ignored -> new HashSet<>()).add(info.getGunType().getContentPack());
            }
        }
        return dependencies;
    }

    private static List<IContentProvider> sortedPacks(List<IContentProvider> packs)
    {
        return packs.stream().sorted(Comparator.comparing(IContentProvider::getName).thenComparing(pack -> pack.getPath().toString())).toList();
    }

    @Nullable
    private static Path modelRoot(IContentProvider pack)
    {
        return pack.getModelPath("Model", null).getParent();
    }

    /** ZIP central-directory checksums avoid decompressing models on warm runs. */
    static void fingerprint(MessageDigest digest, Path path, boolean definitions, boolean categories, State state) throws IOException
    {
        if (Files.isRegularFile(path))
        {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            String key = path.toAbsolutePath().normalize() + "!entries " + definitions + " " + categories;
            FileHash old = state.files.get(key);
            if (sameStamp(old, attributes))
            {
                update(digest, "archive " + old.hash());
                return;
            }
            MessageDigest entriesDigest = newDigest();
            try (ZipFile archive = new ZipFile(path.toFile()))
            {
                List<? extends ZipEntry> entries = archive.stream().filter(entry -> !entry.isDirectory() && relevant(entry.getName(), definitions, categories))
                    .sorted(Comparator.comparing(ZipEntry::getName)).toList();
                for (ZipEntry entry : entries)
                    update(entriesDigest, entry.getName() + " " + entry.getSize() + " " + entry.getCrc());
            }
            String hash = hex(entriesDigest);
            state.files.put(key, new FileHash(attributes.size(), attributes.lastModifiedTime().toString(), hash));
            update(digest, "archive " + hash);
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
                {
                    update(digest, path.relativize(file).toString());
                    fingerprint(digest, file, definitions, categories, state);
                }
                else if (relevant(name, definitions, categories))
                    update(digest, path.relativize(file) + " " + fileHash(file, state));
            }
        }
    }

    private static boolean relevant(String name, boolean definitions, boolean categories)
    {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".class") || lower.endsWith(".java") || mesh(lower) || definitions && lower.endsWith(".txt") || categories && lower.endsWith(".json");
    }

    private static boolean mesh(String name)
    {
        return name.endsWith(".obj") || name.endsWith(".mtl");
    }

    private static void fingerprintMeshes(MessageDigest digest, Path root, State state) throws IOException
    {
        if (!Files.isDirectory(root))
            return;
        try (Stream<Path> files = Files.walk(root))
        {
            for (Path file : files.filter(Files::isRegularFile).filter(file -> mesh(file.toString().toLowerCase(Locale.ROOT))).sorted().toList())
                update(digest, root.relativize(file) + " " + fileHash(file, state));
        }
    }

    static String fileHash(Path file, State state) throws IOException
    {
        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        String key = file.toAbsolutePath().normalize().toString();
        String modified = attributes.lastModifiedTime().toString();
        FileHash old = state.files.get(key);
        if (sameStamp(old, attributes))
            return old.hash();
        MessageDigest digest = newDigest();
        try (InputStream in = Files.newInputStream(file))
        {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1)
                digest.update(buffer, 0, length);
        }
        String hash = hex(digest);
        state.files.put(key, new FileHash(attributes.size(), modified, hash));
        return hash;
    }

    private static boolean sameStamp(@Nullable FileHash old, BasicFileAttributes attributes)
    {
        return old != null && old.size() == attributes.size() && old.modified().equals(attributes.lastModifiedTime().toString());
    }

    private static MessageDigest newDigest()
    {
        try
        {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(MessageDigest digest)
    {
        return HexFormat.of().formatHex(digest.digest());
    }

    private static byte[] classBytes(Class<?> type) throws IOException
    {
        try (InputStream in = type.getResourceAsStream(type.getSimpleName() + ".class"))
        {
            if (in == null)
                throw new IOException("Missing bytecode for " + type.getName());
            return in.readAllBytes();
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
            arrays[i] = new double[]{vectors[i].x, vectors[i].y, vectors[i].z};
        return arrays;
    }

    private static double[] toArray(Vec3 vector)
    {
        return new double[]{vector.x, vector.y, vector.z};
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
