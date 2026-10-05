package com.flansmodultimate.common.driveables;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.client.model.ModelDriveable;
import com.flansmod.client.model.ModelMG;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.MuzzleMeasurements;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.ModelClassResolver;
import com.flansmodultimate.util.ModelClassResolver.ModelClassLocation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Measures the muzzles of driveable, AA-gun and deployable-gun models while the content is
 * loading, on a dedicated server as on a client, so the server fires from the
 * model's barrels on its own authority.
 *
 * <p>Legacy model classes only build geometry when they are constructed, and the
 * model framework resolves its rendering classes lazily, so the models load on a
 * dedicated server as they do on the client. A model that cannot be loaded, for
 * instance one whose constructor reaches client code, keeps its type as authored.</p>
 *
 * <p>A pack a mod ships from the mods folder is trusted as written: its driveables
 * keep their muzzles and its AA guns fire from their {@code Barrel} lines, elevated
 * round the measured pivot. A pack in the flan folder is not: its driveables have
 * their muzzles moved onto the measured ones by the rules of
 * {@code /flandebug shootpoint apply}, the primary bank only when it holds one
 * plain point and each seat gun the model registers, and its AA guns fire from
 * the measured muzzles. Either way the measured barrels take the place of the
 * ones a client used to report, and a twin or quad mount fired from one point
 * takes its model's barrels in turn, spread round that point. Deployable guns
 * always use a measurable model muzzle, with {@code PivotHeight} retained as
 * their compatibility fallback.</p>
 *
 * <p>Model classes are resolved with the client's default settings, whatever the
 * client configuration says, so a server and its clients measure the same class.</p>
 *
 * <p>The results are kept by {@link MuzzleMeasurementCache}: a restart on unchanged
 * content replays them instead of loading and measuring every model again. The
 * debug report on trusted packs only comes from a pass that actually measures.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelMuzzleMeasurement
{
    /** Moves shorter than this, in model pixels, are rounding rather than a misplaced muzzle. */
    private static final float MIN_CORRECTION_PIXELS = 0.1F;

    /**
     * Measures the models of {@code types}, or replays the last measurement when
     * none of the content it depends on has changed since.
     *
     * @param contentPacks           every loaded content pack, which the cached results are keyed on
     * @param correctFlanFolderPacks move the muzzles of the flan folder packs onto the measured ones
     */
    public static void measure(List<InfoType> types, List<IContentProvider> contentPacks, boolean correctFlanFolderPacks)
    {
        long startTime = System.currentTimeMillis();
        // Which lines are trusted follows from the pack and the option alone, so it is decided afresh every run.
        for (InfoType type : types)
        {
            if (type instanceof AAGunType aaGunType)
                aaGunType.setTrustBarrelLines(trustsDefinition(type, correctFlanFolderPacks));
        }

        String key = MuzzleMeasurementCache.key(contentPacks, correctFlanFolderPacks);
        MuzzleMeasurementCache.Results cached = MuzzleMeasurementCache.load(key);
        if (cached != null)
        {
            int moved = MuzzleMeasurementCache.apply(cached, types);
            FlansLog.log.info("Applied the muzzles measured on {} model(s) in {} ms, since no content pack changed: {} deployed gun(s), {} AA gun(s) with measured barrels, {} shoot point(s) of {} driveable(s) moved onto their model's muzzles, {} multi-barrel mount(s). Delete {} to measure them again.",
                cached.models, System.currentTimeMillis() - startTime, cached.deployedGuns.size(), cached.aaGuns.size(), moved,
                cached.driveables.size(), cached.mountCount(), MuzzleMeasurementCache.file());
            return;
        }

        Map<ModelClassLocation, Optional<Object>> models = new HashMap<>();
        Tally tally = new Tally();

        // Resolving and loading a model class opens its pack archive several times over.
        try (FileUtils.ArchiveFileSystemCache ignored = FileUtils.cacheArchiveFileSystems())
        {
            for (InfoType type : types)
                measure(type, correctFlanFolderPacks, models, tally);
        }

        tally.results.models = (int) models.values().stream().filter(Optional::isPresent).count();
        tally.results.failed = tally.failed;
        MuzzleMeasurementCache.save(key, tally.results);
        FlansLog.log.info("Measured {} model(s) in {} ms: {} deployed gun(s), {} AA gun(s) with measured barrels, {} shoot point(s) of {} driveable(s) moved onto their model's muzzles, {} multi-barrel mount(s), {} type(s) could not be measured.",
            tally.results.models, System.currentTimeMillis() - startTime,
            tally.deployedGuns, tally.aaGuns, tally.points, tally.driveables, tally.mounts, tally.failed);
    }

    private static final class Tally
    {
        private final MuzzleMeasurementCache.Results results = new MuzzleMeasurementCache.Results();
        private int deployedGuns;
        private int aaGuns;
        private int driveables;
        private int points;
        private int mounts;
        private int failed;
    }

    private static void measure(InfoType type, boolean correctFlanFolderPacks, Map<ModelClassLocation, Optional<Object>> models,
                                Tally tally)
    {
        boolean trusted = trustsDefinition(type, correctFlanFolderPacks);
        // AA guns are measured whether or not their lines are trusted: the pivot
        // elevates a trusted line, and the muzzle fills a barrel that has none.
        // Driveables are too: a trusted point keeps its place, but a twin or quad
        // mount still spreads its shots over the barrels the model draws.
        if (!(type instanceof AAGunType) && !(type instanceof DriveableType)
            && !(type instanceof GunType gunType && gunType.isDeployable()))
            return;

        String className = type instanceof GunType gunType
            ? gunType.resolveDeployableModelClassName() : type.resolveModelClassName();
        if (StringUtils.isBlank(className))
            return;
        Object model = loadModel(type, className, models);
        if (model == null)
        {
            tally.failed++;
            return;
        }

        try
        {
            if (type instanceof AAGunType aaGunType && model instanceof ModelAAGun aaGunModel)
                tally.aaGuns += measureAAGun(aaGunType, aaGunModel, tally.results) ? 1 : 0;
            else if (type instanceof GunType gunType && model instanceof ModelMG mgModel)
                tally.deployedGuns += measureDeployedGun(gunType, mgModel, tally.results) ? 1 : 0;
            else if (type instanceof DriveableType driveableType && model instanceof ModelDriveable driveableModel)
            {
                // Trusted points are only reported when they look mirrored, never moved.
                if (trusted && FlansLog.log.isDebugEnabled())
                    reportMirroredPoints(driveableType, driveableModel);
                else if (!trusted)
                {
                    int moved = correctDriveable(driveableType, driveableModel, tally.results);
                    tally.points += moved;
                    tally.driveables += moved > 0 ? 1 : 0;
                }
                // After the correction, so a moved point spreads round where it now is.
                tally.mounts += spreadBarrels(driveableType, driveableModel, tally.results);
            }
        }
        catch (Exception | LinkageError e)
        {
            tally.failed++;
            FlansLog.log.warn("Could not measure the muzzles of {}: {}", type, rootCause(e));
        }
    }

    /**
     * Whether a type's definition is taken as written. Packs a mod ships from the
     * mods folder, registered through {@code PackagedContentPackApi}, are built
     * against their models; packs dropped into the flan folder are not trusted
     * unless overriding their configured shoot points is switched off.
     */
    private static boolean trustsDefinition(InfoType type, boolean correctFlanFolderPacks)
    {
        return type.getContentPack().isPreprocessed() || !correctFlanFolderPacks;
    }

    /** Loads each model class once for every type that uses it, and drops them all after the pass. */
    @Nullable
    private static Object loadModel(InfoType type, String className, Map<ModelClassLocation, Optional<Object>> models)
    {
        ModelClassLocation location = ModelClassResolver.find(type.getContentPack(), className, true);
        return models.computeIfAbsent(location, key -> {
            try
            {
                return Optional.of(ModelClassResolver.instantiate(key, false));
            }
            catch (Exception | LinkageError e)
            {
                FlansLog.log.warn("Could not load model class {} to measure the muzzles of {}: {}", className, type, rootCause(e));
                return Optional.empty();
            }
        }).orElse(null);
    }

    /** The innermost cause, which is what a constructor that threw through reflection actually failed on. */
    private static String rootCause(Throwable throwable)
    {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause)
            cause = cause.getCause();
        StackTraceElement[] trace = cause.getStackTrace();
        return cause + (trace.length > 0 ? " at " + trace[0] : StringUtils.EMPTY);
    }

    private static boolean measureAAGun(AAGunType type, ModelAAGun model, MuzzleMeasurementCache.Results results)
    {
        ModelAAGun.BarrelOriginData data = model.getModelBarrelOriginData(type);
        if (data == null)
            return false;
        type.setMeasuredBarrels(data.pivots(), data.muzzles());
        if (!type.hasMeasuredBarrels())
            return false;
        MuzzleMeasurementCache.recordBarrels(results, type, data.pivots(), data.muzzles());
        return true;
    }

    private static boolean measureDeployedGun(GunType type, ModelMG model, MuzzleMeasurementCache.Results results)
    {
        ModelMG.MuzzleOriginData data = model.getModelMuzzleOriginData();
        if (data == null)
            return false;
        type.setMeasuredDeployableMuzzle(data.pivot(), data.muzzle());
        if (!type.hasMeasuredDeployableMuzzle())
            return false;
        MuzzleMeasurementCache.recordDeployedGunMuzzle(results, type, data.pivot(), data.muzzle());
        return true;
    }

    /** Moves a shoot point and records the move, so a later run on the same content can replay it. */
    private static boolean applyShootPoint(DriveableType type, boolean secondary, int index, Vector3f position,
                                           MuzzleMeasurementCache.Results results)
    {
        if (!type.applyMeasuredShootPoint(secondary, index, position))
            return false;
        MuzzleMeasurementCache.recordMove(results, type,
            new MuzzleMeasurementCache.Move(false, secondary, index, position.x, position.y, position.z));
        return true;
    }

    /** Moves a seat's {@code GunOrigin} and records the move. */
    private static boolean applyGunOrigin(DriveableType type, int seat, Vector3f position,
                                          MuzzleMeasurementCache.Results results)
    {
        if (!type.applyMeasuredGunOrigin(seat, position))
            return false;
        MuzzleMeasurementCache.recordMove(results, type,
            new MuzzleMeasurementCache.Move(true, false, seat, position.x, position.y, position.z));
        return true;
    }

    private static MuzzleMeasurements.DriveableInputs inputs(DriveableType type)
    {
        List<MuzzleMeasurements.SeatGun> seatGuns = new ArrayList<>();
        for (int seat = 1; seat <= type.getNumPassengers(); seat++)
        {
            SeatInfo info = type.getSeat(seat);
            if (info != null && info.getGunType() != null && StringUtils.isNotBlank(info.getGunName()))
                seatGuns.add(new MuzzleMeasurements.SeatGun(seat, info.getGunName()));
        }
        return new MuzzleMeasurements.DriveableInputs(type instanceof PlaneType, type.getModelScale(),
            type.getVehicleGunModelScale(), seatGuns);
    }

    /** @return how many muzzles were moved */
    private static int correctDriveable(DriveableType type, ModelDriveable model, MuzzleMeasurementCache.Results results)
    {
        MuzzleMeasurements.DriveableInputs inputs = inputs(type);
        List<String> moves = new ArrayList<>();

        // Mirrored points first, so the barrels below are matched on the side the author meant.
        for (boolean secondary : new boolean[] { false, true })
        {
            List<ShootPoint> points = type.shootPoints(secondary);
            for (int index = 0; index < points.size(); index++)
            {
                Vector3f muzzle = muzzlePixels(points.get(index));
                if (!MuzzleMeasurements.isMirroredMuzzle(model, inputs, muzzle))
                    continue;
                Vector3f mirrored = new Vector3f(muzzle.x, muzzle.y, -muzzle.z);
                if (applyShootPoint(type, secondary, index, mirrored, results))
                    moves.add(bankLabel(secondary, index) + " mirrored " + format(muzzle) + " -> " + format(mirrored));
            }
        }

        correctPrimaryBarrels(type, model, inputs, moves, results);

        for (DerivedMuzzle muzzle : MuzzleMeasurements.deriveMuzzles(model, inputs))
        {
            SeatInfo seat = muzzle.isBarrel() ? null : type.getSeat(muzzle.seatIndex());
            if (seat == null || seat.getGunType() == null || seat.getGunOrigin() == null)
                continue;
            Vector3f origin = seat.getGunOrigin();
            Vector3f authored = new Vector3f(origin.x * 16F, origin.y * 16F, origin.z * 16F);
            if (distance(authored, muzzle.position()) >= MIN_CORRECTION_PIXELS
                && applyGunOrigin(type, muzzle.seatIndex(), muzzle.position(), results))
                moves.add(muzzle.label() + " " + format(authored) + " -> " + format(muzzle.position()));
        }

        if (!moves.isEmpty() && FlansLog.log.isDebugEnabled())
            FlansLog.log.debug("Moved muzzles of {} onto its model: {}", type, String.join(", ", moves));
        return moves.size();
    }

    /**
     * Moves the primary bank onto the model's barrels. A bank of one plain point
     * takes the barrel nearest it; a bank with as many plain points as the model
     * has barrels, such as a quad mount's, pairs each point with a barrel, the
     * closest pairs first. A bank holding an {@code AddGun} is left alone: its
     * gun is not one of the barrels.
     */
    private static void correctPrimaryBarrels(DriveableType type, ModelDriveable model,
                                              MuzzleMeasurements.DriveableInputs inputs, List<String> moves,
                                              MuzzleMeasurementCache.Results results)
    {
        List<ShootPoint> primary = type.shootPoints(false);
        if (primary.isEmpty() || primary.stream().anyMatch(point -> point.getRootPos() instanceof PilotGun))
            return;

        if (primary.size() == 1)
        {
            Vector3f authored = muzzlePixels(primary.get(0));
            DerivedMuzzle barrel = MuzzleMeasurements.derivePrimaryBarrelNear(model, inputs, authored);
            if (barrel != null)
                moveShootPoint(type, 0, authored, barrel, moves, results);
            return;
        }

        List<DerivedMuzzle> barrels = MuzzleMeasurements.derivePrimaryBarrels(model, inputs);
        if (barrels.size() != primary.size())
            return;
        List<Vector3f> authored = primary.stream().map(ModelMuzzleMeasurement::muzzlePixels).toList();
        List<int[]> pairs = new ArrayList<>();
        for (int point = 0; point < authored.size(); point++)
        {
            for (int barrel = 0; barrel < barrels.size(); barrel++)
                pairs.add(new int[] { point, barrel });
        }
        pairs.sort(Comparator.comparingDouble(pair -> distance(authored.get(pair[0]), barrels.get(pair[1]).position())));
        boolean[] pointTaken = new boolean[authored.size()];
        boolean[] barrelTaken = new boolean[barrels.size()];
        for (int[] pair : pairs)
        {
            if (pointTaken[pair[0]] || barrelTaken[pair[1]])
                continue;
            pointTaken[pair[0]] = true;
            barrelTaken[pair[1]] = true;
            moveShootPoint(type, pair[0], authored.get(pair[0]), barrels.get(pair[1]), moves, results);
        }
    }

    /**
     * Spreads the shots of a twin or quad mount over the barrels the model draws:
     * the primary bank fired from one point on a multi-barrel main armament, as a
     * Flakpanzer's, and each seat gun with several barrels, as a bomber turret's.
     * The rate of fire is left alone; each shot just leaves from the next barrel.
     * A bank whose points stand apart was placed by its author and is left alone.
     *
     * @return how many mounts were given barrels
     */
    private static int spreadBarrels(DriveableType type, ModelDriveable model, MuzzleMeasurementCache.Results results)
    {
        MuzzleMeasurements.DriveableInputs inputs = inputs(type);
        List<String> spreads = new ArrayList<>();

        List<ShootPoint> primary = type.shootPoints(false);
        if (isOnePoint(primary))
        {
            List<Vector3f> barrels = MuzzleMeasurements.derivePrimaryBarrels(model, inputs).stream()
                .map(DerivedMuzzle::position).toList();
            List<Vector3f> offsets = MuzzleMeasurements.barrelSpread(barrels, muzzlePixels(primary.get(0)));
            for (int index = 0; index < primary.size() && !offsets.isEmpty(); index++)
            {
                // A point written twice fires twice a shot, as it always has; starting
                // each copy on a different barrel puts those shots on different tubes.
                List<Vector3f> staggered = new ArrayList<>(offsets);
                Collections.rotate(staggered, -(index * offsets.size() / primary.size()));
                if (type.applyMeasuredBarrels(false, index, staggered))
                    MuzzleMeasurementCache.recordSpread(results, type,
                        new MuzzleMeasurementCache.Spread(false, false, index, staggered));
            }
            if (!offsets.isEmpty())
                spreads.add("primary x" + offsets.size() + (primary.size() > 1 ? " over " + primary.size() + " points" : ""));
        }

        for (MuzzleMeasurements.SeatGun seatGun : inputs.seatGuns())
        {
            Vector3f origin = type.getSeat(seatGun.seatIndex()).getGunOrigin();
            List<Vector3f> offsets = MuzzleMeasurements.barrelSpread(
                MuzzleMeasurements.deriveSeatGunBarrels(model, inputs, seatGun),
                new Vector3f(origin.x * 16F, origin.y * 16F, origin.z * 16F));
            if (!offsets.isEmpty() && type.applyMeasuredGunBarrels(seatGun.seatIndex(), offsets))
            {
                MuzzleMeasurementCache.recordSpread(results, type,
                    new MuzzleMeasurementCache.Spread(true, false, seatGun.seatIndex(), offsets));
                spreads.add("seat " + seatGun.seatIndex() + " x" + offsets.size());
            }
        }

        if (!spreads.isEmpty() && FlansLog.log.isDebugEnabled())
            FlansLog.log.debug("Spread the shots of {} over its model's barrels: {}", type, String.join(", ", spreads));
        return spreads.size();
    }

    /** Whether a bank fires plain points that all stand on one muzzle, as one point or one written twice. */
    private static boolean isOnePoint(List<ShootPoint> points)
    {
        if (points.isEmpty() || points.stream().anyMatch(point -> point.getRootPos() instanceof PilotGun))
            return false;
        Vector3f first = muzzlePixels(points.get(0));
        return points.stream().allMatch(point -> distance(muzzlePixels(point), first) < MIN_CORRECTION_PIXELS);
    }

    private static void moveShootPoint(DriveableType type, int index, Vector3f authored, DerivedMuzzle barrel,
                                       List<String> moves, MuzzleMeasurementCache.Results results)
    {
        if (distance(authored, barrel.position()) >= MIN_CORRECTION_PIXELS
            && applyShootPoint(type, false, index, barrel.position(), results))
            moves.add(barrel.label() + " " + format(authored) + " -> " + format(barrel.position()));
    }

    /**
     * Logs the points of a trusted pack that look mirrored, without moving them,
     * so a pack whose definitions are fixed by hand can be checked against its models.
     */
    private static void reportMirroredPoints(DriveableType type, ModelDriveable model)
    {
        MuzzleMeasurements.DriveableInputs inputs = inputs(type);
        for (boolean secondary : new boolean[] { false, true })
        {
            List<ShootPoint> points = type.shootPoints(secondary);
            for (int index = 0; index < points.size(); index++)
            {
                Vector3f muzzle = muzzlePixels(points.get(index));
                if (MuzzleMeasurements.isMirroredMuzzle(model, inputs, muzzle))
                    FlansLog.log.debug("{} {} {} looks mirrored: the model has a barrel at {}", type,
                        bankLabel(secondary, index), format(muzzle), format(new Vector3f(muzzle.x, muzzle.y, -muzzle.z)));
            }
        }
    }

    /** The muzzle a shoot point fires from, root plus offset, in type-file pixels. */
    private static Vector3f muzzlePixels(ShootPoint point)
    {
        Vector3f root = point.getRootPos().getPosition();
        Vector3f offset = point.getOffPos();
        return new Vector3f((root.x + offset.x) * 16F, (root.y + offset.y) * 16F, (root.z + offset.z) * 16F);
    }

    private static String bankLabel(boolean secondary, int index)
    {
        return (secondary ? "secondary" : "primary") + " [" + index + "]";
    }

    private static float distance(Vector3f a, Vector3f b)
    {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float dz = a.z - b.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String format(Vector3f pixels)
    {
        return String.format(Locale.ROOT, "(%.1f %.1f %.1f)", pixels.x, pixels.y, pixels.z);
    }
}
