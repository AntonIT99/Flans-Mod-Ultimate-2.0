package com.flansmodultimate.tooling.shootpoints;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.weapons.DerivedMuzzle;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import com.flansmodultimate.tooling.shootpoints.DefinitionFile.Line;
import com.flansmodultimate.tooling.shootpoints.Finding.Action;
import com.flansmodultimate.tooling.shootpoints.Finding.Kind;

import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Compares a definition's authored muzzles with the ones measured off its model,
 * rewrites the definition in memory to match the measurement, and scores each
 * value for how much it deserves a look by hand.
 *
 * <p>
 * Applies the same rules as {@code /flandebug shootpoint apply}: the primary
 * bank is moved only when it holds one plain point, each measured seat gun sets
 * that seat's {@code GunOrigin}, and each measured AA gun barrel sets its
 * {@code Barrel} line. Values are read the way {@code DriveableType} and
 * {@code AAGunType} read them.
 * </p>
 */
final class ShootPointPlanner
{
    /** Shoot points and GunOrigin are written to a tenth of a pixel, as the debug command prints them. */
    static final int SHOOT_POINT_DECIMALS = 1;
    /** Barrel lines are written to a hundredth of a pixel, as {@code AAGunType#roundBarrelPixels} keeps them. */
    static final int BARREL_DECIMALS = 2;
    private static final int MAX_AA_BARRELS = 16;
    private static final double MOUNTED_OFFSET_PX = Driveable.PASSENGER_GUN_MOUNTED_OFFSET * 16D;

    private ShootPointPlanner()
    {}

    // ------------------------------------------------------------ definitions

    record Seat(int id, float[] position, String gunType, String gunName, int lineIndex)
    {
        boolean mountsGun()
        {
            return !gunType.isBlank();
        }
    }

    /**
     * A point of the primary bank.
     *
     * @param offsetIndex
     *            the value index the offset starts at, where one would be written
     * @param offset
     *            the authored offset, or {@code null} when the line has none
     */
    record PrimaryPoint(Line line, float[] root, float[] offset, int offsetIndex, boolean pilotGun)
    {
        float[] muzzle()
        {
            return offset == null ? root : add(root, offset);
        }
    }

    record SeatVector(Line line, int seat, float[] value)
    {}

    record DriveableDefinition(String modelName, float modelScale, float vehicleGunModelScale, Map<Integer, Seat> seats, List<PrimaryPoint> primary, Map<Integer, SeatVector> gunOrigins,
        int lastGunOriginLine)
    {}

    record AAGunDefinition(String modelName, int numBarrels, boolean sentry, Map<Integer, Line> barrels, int lastBarrelLine, int numBarrelsLine)
    {}

    static DriveableDefinition readDriveable(DefinitionFile file)
    {
        Map<Integer, Seat> seats = new TreeMap<>();
        for (Line line : file.lines("Passenger"))
        {
            if (line.size() < 5)
                continue;
            try
            {
                int id = Integer.parseInt(line.value(0));
                for (int value = 5; value < Math.min(9, line.size()); value++)
                    Float.parseFloat(line.value(value));
                if (id > 0)
                    seats.put(id, new Seat(id, vector(line, 1), line.size() > 9 ? line.value(9) : "", line.size() > 10 ? line.value(10) : "", line.index()));
            }
            catch (RuntimeException ignored)
            {
                // DriveableType drops a seat it cannot parse.
            }
        }

        List<PrimaryPoint> primary = new ArrayList<>();
        for (Line line : file.lines("ShootPointPrimary"))
        {
            if (line.size() < 3)
                continue;
            try
            {
                float[] root = vector(line, 0);
                int cursor = 3;
                if (cursor < line.size() && EnumDriveablePart.getPart(line.value(cursor)) != null)
                    cursor++;
                boolean pilotGun = false;
                if (cursor < line.size() && !DefinitionFile.isFloat(line.value(cursor)))
                {
                    pilotGun = true;
                    cursor++;
                }
                float[] offset = line.size() >= cursor + 3 ? vector(line, cursor) : null;
                primary.add(new PrimaryPoint(line, root, offset, cursor, pilotGun));
            }
            catch (RuntimeException ignored)
            {
                // DriveableType drops a point it cannot parse.
            }
        }
        // DriveableType reads the legacy positions after the shoot points, into the same bank.
        for (String key : List.of("BombPosition", "BarrelPosition"))
        {
            for (Line line : file.lines(key))
            {
                if (line.size() < 3)
                    continue;
                try
                {
                    float[] offset = line.size() >= 6 ? vector(line, 3) : null;
                    primary.add(new PrimaryPoint(line, vector(line, 0), offset, 3, false));
                }
                catch (RuntimeException ignored)
                {
                    // DriveableType drops a position it cannot parse.
                }
            }
        }

        Map<Integer, SeatVector> gunOrigins = new TreeMap<>();
        int lastGunOriginLine = -1;
        for (Line line : file.lines("GunOrigin"))
        {
            if (line.size() < 4)
                continue;
            lastGunOriginLine = line.index();
            try
            {
                int seat = Integer.parseInt(line.value(0));
                gunOrigins.put(seat, new SeatVector(line, seat, vector(line, 1)));
            }
            catch (RuntimeException ignored)
            {
                // DriveableType skips a GunOrigin it cannot parse.
            }
        }

        return new DriveableDefinition(file.lastValue("Model"), floatValue(file, "ModelScale", 1F), floatValue(file, "VehicleGunModelScale", 1F), seats, primary, gunOrigins, lastGunOriginLine);
    }

    static AAGunDefinition readAAGun(DefinitionFile file)
    {
        int numBarrels = 1;
        String declared = file.lastValue("NumBarrels");
        if (declared != null)
        {
            try
            {
                numBarrels = Integer.parseInt(declared);
            }
            catch (NumberFormatException ignored)
            {
                // AAGunType keeps its default.
            }
        }
        numBarrels = Math.max(1, Math.min(MAX_AA_BARRELS, numBarrels));

        boolean sentry = booleanValue(file, "TargetMobs") || booleanValue(file, "TargetPlayers");
        boolean driveables = booleanValue(file, "TargetVehicles") || booleanValue(file, "TargetPlanes") || booleanValue(file, "TargetMechas");
        if (!file.lines("TargetDriveables").isEmpty())
            driveables = booleanValue(file, "TargetDriveables");
        sentry |= driveables;

        Map<Integer, Line> barrels = new TreeMap<>();
        int lastBarrelLine = -1;
        for (Line line : file.lines("Barrel"))
        {
            if (line.size() < 4)
                continue;
            try
            {
                float[] values = new float[line.size()];
                for (int i = 0; i < values.length; i++)
                    values[i] = Float.parseFloat(line.value(i));
                int id = (int) values[0];
                lastBarrelLine = line.index();
                if (id == values[0] && id >= 0 && id < numBarrels)
                    barrels.put(id, line);
            }
            catch (NumberFormatException ignored)
            {
                // AAGunType drops a Barrel line with a value that is not a number.
            }
        }
        List<Line> numBarrelsLines = file.lines("NumBarrels");
        return new AAGunDefinition(file.lastValue("Model"), numBarrels, sentry, barrels, lastBarrelLine, numBarrelsLines.isEmpty() ? -1 : numBarrelsLines.get(numBarrelsLines.size() - 1).index());
    }

    // --------------------------------------------------------------- planning

    /**
     * Rewrites a driveable definition to its measured muzzles.
     *
     * @param derived
     *            the model's measured muzzles, in type-file terms
     * @param planeFacing
     *            whether the definition is authored in the plane flight basis
     * @param vehicleModel
     *            whether the model has a primary barrel group to measure at all
     */
    static List<Finding> planDriveable(String name, DefinitionFile file, DriveableDefinition definition, List<DerivedMuzzle> derived, boolean planeFacing, boolean vehicleModel)
    {
        List<Finding> findings = new ArrayList<>();
        DerivedMuzzle barrel = derived.stream().filter(DerivedMuzzle::isBarrel).findFirst().orElse(null);
        List<PrimaryPoint> primary = definition.primary();
        if (barrel != null)
        {
            float[] measured = values(barrel.position());
            if (primary.size() == 1 && !primary.get(0).pilotGun())
                findings.add(planPrimary(name, file, primary.get(0), measured, planeFacing));
            else
            {
                Finding finding = new Finding(name, Kind.PRIMARY, "primary", null, measured, Double.NaN, Action.SKIPPED, SHOOT_POINT_DECIMALS);
                finding.flag(25,
                    primary.isEmpty()
                        ? "the model has a main gun but the definition declares no primary shoot point"
                        : primary.size() > 1
                            ? "the primary bank has " + primary.size() + " points, so the one measured barrel cannot be placed on them"
                            : "the primary point mounts its own gun, so it is left as authored");
                findings.add(finding);
            }
        }
        else if (vehicleModel && primary.stream().anyMatch(point -> point.line().key().equalsIgnoreCase("BarrelPosition")))
        {
            Finding finding = new Finding(name, Kind.PRIMARY, "primary", primary.get(0).muzzle(), null, Double.NaN, Action.SKIPPED, SHOOT_POINT_DECIMALS);
            finding.flag(15, "BarrelPosition is authored but the model has no barrel geometry to measure it against");
            findings.add(finding);
        }

        for (DerivedMuzzle muzzle : derived)
        {
            if (!muzzle.isBarrel())
                findings.add(planGunOrigin(name, file, definition, muzzle));
        }

        for (Seat seat : definition.seats().values())
        {
            if (!seat.mountsGun() || derived.stream().anyMatch(muzzle -> muzzle.seatIndex() == seat.id()))
                continue;
            Finding finding = new Finding(name, Kind.GUN_ORIGIN, "GunOrigin " + seat.id(), null, null, Double.NaN, Action.SKIPPED, SHOOT_POINT_DECIMALS);
            finding.flag(10,
                seat.gunName().isBlank()
                    ? "seat " + seat.id() + " mounts " + seat.gunType() + " but names no model gun, so its muzzle cannot be measured"
                    : "the model registers no gun named '" + seat.gunName() + "' for seat " + seat.id() + ", so its muzzle cannot be measured");
            findings.add(finding);
        }
        return findings;
    }

    private static Finding planPrimary(String name, DefinitionFile file, PrimaryPoint point, float[] measured, boolean planeFacing)
    {
        float[] authored = point.muzzle();
        boolean writeOffset = point.offset() != null;
        // The firing path reads root plus offset as the muzzle, so the root stays
        // where it was authored whenever an offset carries the difference.
        float[] replacement = writeOffset ? subtract(measured, point.root()) : measured;
        float[] current = writeOffset ? point.offset() : point.root();
        Finding finding = rewrite(name, file, Kind.PRIMARY, "primary (" + point.line().key() + ")", point.line(), writeOffset ? point.offsetIndex() : 0, current, replacement, authored, measured,
            distance(authored, measured), SHOOT_POINT_DECIMALS);
        scoreShootPoint(finding, planeFacing);
        double forward = planeFacing ? -1D : 1D;
        double behind = (authored[0] - measured[0]) * forward;
        if (behind > 8D)
            finding.flag(15, String.format(Locale.ROOT, "the measured muzzle is %.1f px behind the authored one: part of the barrel may not have been measured", behind));
        if (Math.abs(measured[2]) > 4F)
            finding.flag(10, String.format(Locale.ROOT, "the main gun measures %.1f px off the centreline", measured[2]));
        return finding;
    }

    private static Finding planGunOrigin(String name, DefinitionFile file, DriveableDefinition definition, DerivedMuzzle muzzle)
    {
        int seatIndex = muzzle.seatIndex();
        float[] measured = values(muzzle.position());
        SeatVector authored = definition.gunOrigins().get(seatIndex);
        Seat seat = definition.seats().get(seatIndex);
        String target = "GunOrigin " + seatIndex;
        Finding finding;
        if (authored != null)
        {
            finding = rewrite(name, file, Kind.GUN_ORIGIN, target, authored.line(), 1, authored.value(), measured, authored.value(), measured, distance(authored.value(), measured),
                SHOOT_POINT_DECIMALS);
        }
        else
        {
            String line = "GunOrigin " + seatIndex + " " + join(measured, SHOOT_POINT_DECIMALS);
            if (definition.lastGunOriginLine() >= 0)
                file.insertAfter(definition.lastGunOriginLine(), line);
            else if (seat != null)
                file.insertAfter(seat.lineIndex(), line);
            else
                file.append(line);
            finding = new Finding(name, Kind.GUN_ORIGIN, target, null, measured, Double.NaN, Action.ADD, SHOOT_POINT_DECIMALS);
            finding.flag(3, "no GunOrigin was authored for this seat; one is added");
        }
        scoreShootPoint(finding, false);

        if (seat != null)
        {
            float[] muzzleAtSeatHeight = {measured[0], (float) (measured[1] + MOUNTED_OFFSET_PX), measured[2]};
            double fromSeat = distance(muzzleAtSeatHeight, seat.position());
            if (fromSeat > 80D)
                finding.flag(30, String.format(Locale.ROOT, "the gun measures %.0f px from its gunner seat: is the right model gun registered for this seat?", fromSeat));
            else if (fromSeat > 48D)
                finding.flag(15, String.format(Locale.ROOT, "the gun measures %.0f px from its gunner seat", fromSeat));
            // Seats and GunOrigin share one frame in game, so a gun drawn across
            // the hull from its gunner means one of the two is mirrored.
            float seatLateral = seat.position()[2];
            if (Math.abs(seatLateral) >= 4F && Math.abs(measured[2]) >= 4F && Math.signum(seatLateral) != Math.signum(measured[2]))
                finding.flag(25,
                    "the gun measures on the other side from its gunner seat (seat lateral " + number(seatLateral) + ", gun " + number(measured[2]) + "): is the seat or the model gun mirrored?");
        }
        if (muzzle.pivot() != null && distance(values(muzzle.pivot()), measured) < 2D)
            finding.flag(25, "the muzzle measures within 2 px of the gun's aim pivot: the gun may be missing its barrel parts");
        return finding;
    }

    /**
     * Rewrites an AA gun definition's {@code Barrel} lines to its measured barrels.
     *
     * @param offsets
     *            each barrel's measured muzzle offset from the gun, in blocks, at rest
     */
    static List<Finding> planAAGun(String name, DefinitionFile file, AAGunDefinition definition, List<Vec3> offsets)
    {
        List<Finding> findings = new ArrayList<>();
        if (offsets.isEmpty())
        {
            findings.add(Finding.problem(name, Action.SKIPPED, 30, "the model has fewer than NumBarrels " + definition.numBarrels() + " barrel groups, or one of them is empty"));
            return findings;
        }

        int anchor = definition.lastBarrelLine() >= 0 ? definition.lastBarrelLine() : definition.numBarrelsLine();
        for (int barrel = 0; barrel < definition.numBarrels() && barrel < offsets.size(); barrel++)
        {
            Vector3f suggested = AAGunBarrelGeometry.legacyBarrelFor(offsets.get(barrel), definition.sentry());
            float[] measured = round(values(suggested), BARREL_DECIMALS);
            Line line = definition.barrels().get(barrel);
            String target = "Barrel " + barrel;
            Finding finding;
            if (line != null)
            {
                float[] authored = vector(line, 1);
                finding = rewrite(name, file, Kind.AA_BARREL, target, line, 1, authored, measured, authored, measured, barrelDistance(authored, measured), BARREL_DECIMALS);
            }
            else
            {
                String added = "Barrel " + barrel + " " + join(measured, BARREL_DECIMALS);
                if (anchor >= 0)
                    file.insertAfter(anchor, added);
                else
                    file.append(added);
                finding = new Finding(name, Kind.AA_BARREL, target, null, measured, Double.NaN, Action.ADD, BARREL_DECIMALS);
                finding.flag(3, "no Barrel line was authored for this barrel; one is added");
            }
            scoreDelta(finding);
            for (int other = 0; other < barrel; other++)
            {
                if (offsets.get(other).distanceTo(offsets.get(barrel)) * 16D < 0.5D)
                {
                    finding.flag(30, "measures on top of barrel " + other + ": do the model's barrel groups share geometry?");
                    break;
                }
            }
            findings.add(finding);
        }
        return findings;
    }

    // ---------------------------------------------------------------- editing

    private static Finding rewrite(String name, DefinitionFile file, Kind kind, String target, Line line, int firstValue, float[] current, float[] replacement, float[] authoredMuzzle,
        float[] measuredMuzzle, double deltaPx, int decimals)
    {
        // Compared as written, so a value the tool wrote reads back as unchanged
        // however close the measurement falls to a rounding boundary.
        boolean unchanged = true;
        for (int i = 0; i < 3; i++)
            unchanged &= DefinitionFile.formatNumber(current[i], decimals).equals(DefinitionFile.formatNumber(replacement[i], decimals));
        if (unchanged)
            return new Finding(name, kind, target, authoredMuzzle, measuredMuzzle, deltaPx, Action.UNCHANGED, decimals);
        if (!line.plain())
        {
            Finding finding = new Finding(name, kind, target, authoredMuzzle, measuredMuzzle, deltaPx, Action.SKIPPED, decimals);
            finding.flag(15, "line " + (line.index() + 1) + " uses bracketed or quoted values; edit it by hand");
            return finding;
        }
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            texts.add(DefinitionFile.formatNumber(replacement[i], decimals));
        file.replaceValues(line.index(), firstValue, texts);
        return new Finding(name, kind, target, authoredMuzzle, measuredMuzzle, deltaPx, Action.UPDATE, decimals);
    }

    // ---------------------------------------------------------------- scoring

    private static void scoreShootPoint(Finding finding, boolean planeFacing)
    {
        scoreDelta(finding);
        float[] authored = finding.authored;
        float[] measured = finding.measured;
        if (authored == null || measured == null)
            return;
        if (flipped(authored[2], measured[2], 2F))
            finding.flag(10,
                "the authored point is on the other side (lateral " + number(authored[2]) + " against a measured " + number(measured[2]) + "): a mirrored model or a lateral sign convention?");
        if (flipped(authored[0], measured[0], 4F))
            finding.flag(10,
                "the authored point is at the other end (forward " + number(authored[0]) + " against a measured " + number(measured[0]) + ")" + (planeFacing ? ": the plane facing convention?" : ""));
    }

    private static void scoreDelta(Finding finding)
    {
        double delta = finding.deltaPx;
        if (Double.isNaN(delta))
            return;
        if (delta >= 32D)
            finding.flag(10, String.format(Locale.ROOT, "moves %.1f px, over two blocks", delta));
        else if (delta >= 16D)
            finding.flag(6, String.format(Locale.ROOT, "moves %.1f px, over a block", delta));
        else if (delta >= 8D)
            finding.flag(3, String.format(Locale.ROOT, "moves %.1f px, half a block or more", delta));
        else if (delta >= 4D)
            finding.flag(1, String.format(Locale.ROOT, "moves %.1f px", delta));
    }

    /** Two values of about the same size on opposite sides of zero: one of them is mirrored. */
    private static boolean flipped(float authored, float measured, float minimum)
    {
        return Math.abs(authored) >= minimum && Math.abs(measured) >= minimum && Math.signum(authored) != Math.signum(measured)
            && Math.abs(authored + measured) <= Math.max(1.5F, 0.2F * Math.abs(measured));
    }

    // ---------------------------------------------------------------- helpers

    /** Where two Barrel lines put their muzzles apart, in pixels, at rest. */
    private static double barrelDistance(float[] first, float[] second)
    {
        Vec3 a = AAGunBarrelGeometry.legacyBarrelOffset(first[0], first[1], first[2], 0F, 0F);
        Vec3 b = AAGunBarrelGeometry.legacyBarrelOffset(second[0], second[1], second[2], 0F, 0F);
        return a.distanceTo(b) * 16D;
    }

    private static float[] vector(Line line, int start)
    {
        return new float[]{DefinitionFile.parseFloat(line.value(start)), DefinitionFile.parseFloat(line.value(start + 1)), DefinitionFile.parseFloat(line.value(start + 2))};
    }

    private static float floatValue(DefinitionFile file, String key, float fallback)
    {
        String value = file.lastValue(key);
        if (value == null)
            return fallback;
        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    /** A boolean read as {@code TypeReaderUtils} reads one: 1 and 0 count, anything else is false. */
    private static boolean booleanValue(DefinitionFile file, String key)
    {
        String value = file.lastValue(key);
        return value != null && (value.equals("1") || value.equalsIgnoreCase("true"));
    }

    private static float[] values(Vector3f vector)
    {
        return new float[]{vector.x, vector.y, vector.z};
    }

    private static float[] add(float[] first, float[] second)
    {
        return new float[]{first[0] + second[0], first[1] + second[1], first[2] + second[2]};
    }

    private static float[] subtract(float[] first, float[] second)
    {
        return new float[]{first[0] - second[0], first[1] - second[1], first[2] - second[2]};
    }

    private static float[] round(float[] values, int decimals)
    {
        float[] rounded = new float[values.length];
        for (int i = 0; i < values.length; i++)
            rounded[i] = Float.parseFloat(DefinitionFile.formatNumber(values[i], decimals));
        return rounded;
    }

    static double distance(float[] first, float[] second)
    {
        double x = first[0] - second[0];
        double y = first[1] - second[1];
        double z = first[2] - second[2];
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static String join(float[] values, int decimals)
    {
        return DefinitionFile.formatNumber(values[0], decimals) + " " + DefinitionFile.formatNumber(values[1], decimals) + " " + DefinitionFile.formatNumber(values[2], decimals);
    }

    private static String number(float value)
    {
        return DefinitionFile.formatNumber(value, SHOOT_POINT_DECIMALS);
    }
}
