package com.flansmodultimate.tooling.shootpoints;

import com.flansmod.client.model.*;
import com.flansmodultimate.client.model.MuzzleMeasurements;
import com.flansmodultimate.common.driveables.weapons.DerivedMuzzle;
import com.flansmodultimate.tooling.shootpoints.Finding.Action;
import com.flansmodultimate.tooling.shootpoints.Finding.Kind;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Developer tooling: writes the muzzles measured off each model into the tracked
 * type files, the way {@code /flandebug shootpoint apply} moves them for one
 * session, and reports the values most worth checking by hand.
 *
 * <p>
 * Run through {@code gradlew shootPointSync [-Pwrite] [-Pfilter=<text>] [-PreportFile=<path>]}.
 * Without {@code -Pwrite} nothing is written and the report says what would change.
 * The tool never fails the build: a definition it cannot read or measure becomes a
 * report entry instead.
 * </p>
 *
 * <p>
 * Arguments: {@code --pack-set <name> <resources dir> <class dirs>} for every source
 * set holding content packs, then optionally {@code --write}, {@code --filter <text>}
 * and {@code --report <file>}. Every set's classes share one loader, as the modules'
 * classes share the mod class path in game, and a model is resolved as
 * {@code InfoType#findModelClass} resolves it for a pack that ships no class files.
 * </p>
 */
public final class ShootPointSync
{
    private static final List<String> DRIVEABLE_FOLDERS = List.of("vehicles", "planes", "mechas");
    private static final String AA_GUN_FOLDER = "aaguns";
    /** Findings scoring at least this lead the report. */
    private static final int CHECK_FIRST_SCORE = 15;

    private ShootPointSync()
    {}

    private record PackSet(String name, Path resources, List<Path> classDirs)
    {}

    private record Options(List<PackSet> sets, boolean write, String filter, Path report)
    {}

    public static void main(String[] args)
    {
        try
        {
            run(parse(args));
        }
        catch (Throwable throwable)
        {
            // Tooling reports, it never breaks a build.
            System.err.println("shootPointSync stopped early: " + throwable);
            throwable.printStackTrace();
        }
    }

    private static Options parse(String[] args)
    {
        List<PackSet> sets = new ArrayList<>();
        boolean write = false;
        String filter = "";
        Path report = Paths.get("build", "reports", "shootPointSync", "shoot-point-sync.md");
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--pack-set" -> {
                    List<Path> classDirs = Arrays.stream(args[i + 3].split(File.pathSeparator)).filter(entry -> !entry.isBlank()).map(Paths::get).toList();
                    sets.add(new PackSet(args[i + 1], Paths.get(args[i + 2]), classDirs));
                    i += 3;
                }
                case "--write" -> write = true;
                case "--filter" -> filter = args[++i].toLowerCase(Locale.ROOT);
                case "--report" -> report = Paths.get(args[++i]);
                default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }
        return new Options(sets, write, filter, report);
    }

    private static void run(Options options) throws IOException
    {
        List<URL> urls = new ArrayList<>();
        for (PackSet set : options.sets())
        {
            for (Path dir : set.classDirs())
                urls.add(dir.toUri().toURL());
        }
        List<Finding> findings = new ArrayList<>();
        int definitions = 0;
        int written = 0;
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ShootPointSync.class.getClassLoader()))
        {
            for (PackSet set : options.sets())
            {
                Path content = set.resources().resolve("flans_content");
                for (Path pack : directories(content))
                {
                    for (String folder : concat(DRIVEABLE_FOLDERS, AA_GUN_FOLDER))
                    {
                        for (Path path : textFiles(pack.resolve("definitions").resolve(folder)))
                        {
                            String name = set.name() + ": " + content.relativize(path).toString().replace('\\', '/');
                            if (!options.filter().isEmpty() && !name.toLowerCase(Locale.ROOT).contains(options.filter()))
                                continue;
                            definitions++;
                            if (process(name, path, folder, loader, options.write(), findings))
                                written++;
                        }
                    }
                }
            }
        }

        flagSiblingDisagreements(findings);
        Path report = options.report().toAbsolutePath();
        Files.createDirectories(report.getParent());
        Files.writeString(report, render(findings, definitions, written, options), StandardCharsets.UTF_8);
        printSummary(findings, definitions, written, options.write(), report);
    }

    /** @return whether the definition was written back */
    private static boolean process(String name, Path path, String folder, ClassLoader loader, boolean write, List<Finding> findings)
    {
        List<Finding> own = new ArrayList<>();
        try
        {
            DefinitionFile file = DefinitionFile.read(path);
            boolean aaGun = folder.equals(AA_GUN_FOLDER);
            String modelName = file.lastValue("Model");
            String className = modelClassName(modelName);
            if (className == null)
            {
                own.add(Finding.problem(name, Action.SKIPPED, 5, "no Model is declared, so there is nothing to measure"));
                findings.addAll(own);
                return false;
            }
            Object model = construct(className, loader, name, own);
            if (model == null)
            {
                findings.addAll(own);
                return false;
            }

            if (aaGun)
            {
                if (model instanceof ModelAAGun aaGunModel)
                {
                    ShootPointPlanner.AAGunDefinition definition = ShootPointPlanner.readAAGun(file);
                    own.addAll(ShootPointPlanner.planAAGun(name, file, definition, MuzzleMeasurements.deriveAAGunBarrelOffsets(aaGunModel, definition.numBarrels())));
                }
                else
                    own.add(Finding.problem(name, Action.SKIPPED, 40, className + " is not an AA gun model"));
            }
            else if (model instanceof ModelDriveable driveableModel)
            {
                boolean planeFacing = folder.equals("planes");
                ShootPointPlanner.DriveableDefinition definition = ShootPointPlanner.readDriveable(file);
                List<MuzzleMeasurements.SeatGun> seatGuns = new ArrayList<>();
                for (ShootPointPlanner.Seat seat : definition.seats().values())
                {
                    if (seat.mountsGun() && !seat.gunName().isBlank())
                        seatGuns.add(new MuzzleMeasurements.SeatGun(seat.id(), seat.gunName()));
                }
                List<DerivedMuzzle> derived = MuzzleMeasurements.deriveMuzzles(driveableModel,
                    new MuzzleMeasurements.DriveableInputs(planeFacing, definition.modelScale(), definition.vehicleGunModelScale(), seatGuns));
                own.addAll(ShootPointPlanner.planDriveable(name, file, definition, derived, planeFacing, driveableModel instanceof ModelVehicle));
                if (own.isEmpty())
                    own.add(Finding.problem(name, Action.UNCHANGED, 0, "nothing to measure: no main gun barrel and no registered seat guns"));
            }
            else
                own.add(Finding.problem(name, Action.SKIPPED, 40, className + " is not a driveable model"));

            own.forEach(finding -> finding.modelClass = className);
            findings.addAll(own);
            if (write && file.isModified())
            {
                file.write(path);
                return true;
            }
            return false;
        }
        catch (Throwable throwable)
        {
            findings.addAll(own);
            findings.add(Finding.problem(name, Action.FAILED, 50, "could not be processed: " + describe(throwable)));
            return false;
        }
    }

    /**
     * The class a {@code Model} entry names, as {@code InfoType#findModelClass} resolves it
     * for a pack that ships no class files and no {@code redirect.info}, as every tracked pack does.
     */
    static String modelClassName(String modelName)
    {
        if (modelName == null || modelName.isBlank() || modelName.equalsIgnoreCase("null") || modelName.equalsIgnoreCase("none"))
            return null;
        String[] split = modelName.split("\\.");
        if (split.length == 1)
            return "com.flansmod.client.model.Model" + modelName;
        if (split[0].equals("jamespostmodernweapons"))
            split[0] = "modernweapons";
        String packageName = String.join(".", Arrays.copyOf(split, split.length - 1));
        return "com.flansmod.client.model." + packageName + ".Model" + split[split.length - 1];
    }

    private static Object construct(String className, ClassLoader loader, String name, List<Finding> findings)
    {
        try
        {
            return Class.forName(className, true, loader).getConstructor().newInstance();
        }
        catch (ClassNotFoundException | NoClassDefFoundError e)
        {
            findings.add(Finding.problem(name, Action.SKIPPED, 50, "model class " + className + " was not found"));
        }
        catch (Exception | LinkageError e)
        {
            findings.add(Finding.problem(name, Action.FAILED, 50, "model class " + className + " could not be constructed: " + describe(e)));
        }
        return null;
    }

    /**
     * Sibling definitions, such as the variants of one gun, share a model and so
     * measure the same. Where their authored values disagree, at least one was
     * wrong, and a hand check should look at all of them.
     */
    private static void flagSiblingDisagreements(List<Finding> findings)
    {
        Map<String, List<Finding>> groups = new LinkedHashMap<>();
        for (Finding finding : findings)
        {
            if (finding.kind != Kind.MODEL && finding.authored != null && !finding.modelClass.isEmpty())
                groups.computeIfAbsent(finding.modelClass + "|" + finding.target, ignored -> new ArrayList<>()).add(finding);
        }
        for (List<Finding> group : groups.values())
        {
            double spread = 0D;
            for (Finding first : group)
            {
                for (Finding second : group)
                    spread = Math.max(spread, ShootPointPlanner.distance(first.authored, second.authored));
            }
            if (spread <= 1D)
                continue;
            for (Finding finding : group)
                finding.flag(5, String.format(Locale.ROOT, "%d definitions share this model and author this value up to %.1f px apart", group.size(), spread));
        }
    }

    // ---------------------------------------------------------------- report

    private static String render(List<Finding> findings, int definitions, int written, Options options)
    {
        StringWriter text = new StringWriter();
        PrintWriter out = new PrintWriter(text);
        out.println("# Shoot point sync");
        out.println();
        out.println("Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            + (options.write() ? ", definitions written." : ", report only: nothing was written. Run with `-Pwrite` to apply.")
            + (options.filter().isEmpty() ? "" : " Filter: `" + options.filter() + "`."));
        out.println();
        out.println("Values are model pixels in type-file convention; AA gun barrels are `Barrel` line values, " + "with Δ measured between the muzzles they put the round at.");
        out.println();
        out.println("| Definitions | " + label(options.write(), "Updated", "Would update") + " | " + label(options.write(), "Added", "Would add") + " | Unchanged | Skipped | Failed | Files "
            + (options.write() ? "written" : "to write") + " |");
        out.println("|---:|---:|---:|---:|---:|---:|---:|");
        out.println("| " + definitions + " | " + count(findings, Action.UPDATE) + " | " + count(findings, Action.ADD) + " | " + count(findings, Action.UNCHANGED) + " | "
            + count(findings, Action.SKIPPED) + " | " + count(findings, Action.FAILED) + " | " + (options.write() ? written : filesToWrite(findings)) + " |");
        out.println();

        List<Finding> checkFirst = findings.stream().filter(finding -> finding.score >= CHECK_FIRST_SCORE)
            .sorted(Comparator.comparingInt((Finding finding) -> -finding.score).thenComparing(finding -> finding.definition)).toList();
        out.println("## Check first");
        out.println();
        out.println("Authored values are not trusted, so a large move alone scores little. Most of the score comes "
            + "from signs that the measurement itself may be wrong (a gun far from or across the hull from its "
            + "gunner, a muzzle on its pivot, barrels on top of each other, a main gun measured short or off "
            + "centre) and from values the tool could not place. Higher is more suspicious; everything from " + CHECK_FIRST_SCORE + " up is listed.");
        out.println();
        if (checkFirst.isEmpty())
            out.println("Nothing scored " + CHECK_FIRST_SCORE + " or more.");
        else
            table(out, checkFirst, true);
        out.println();

        out.println("## Every value");
        out.println();
        List<Finding> all = findings.stream().filter(finding -> !(finding.kind == Kind.MODEL && finding.action == Action.UNCHANGED))
            .sorted(Comparator.comparing((Finding finding) -> finding.definition).thenComparing(finding -> finding.target)).toList();
        table(out, all, false);
        out.println();

        List<String> nothing = findings.stream().filter(finding -> finding.kind == Kind.MODEL && finding.action == Action.UNCHANGED).map(finding -> finding.definition).sorted().toList();
        if (!nothing.isEmpty())
        {
            out.println("## Nothing to measure");
            out.println();
            out.println("The model has no main gun barrel and no registered seat guns; their shoot points mount " + "their own guns and were left as authored.");
            out.println();
            nothing.forEach(definition -> out.println("- `" + definition + "`"));
        }
        out.flush();
        return text.toString();
    }

    private static void table(PrintWriter out, List<Finding> findings, boolean withScore)
    {
        out.println((withScore ? "| Score " : "") + "| Definition | Value | Action | Authored | Measured | Δ px | Why |");
        out.println((withScore ? "|---:" : "") + "|---|---|---|---|---|---:|---|");
        for (Finding finding : findings)
        {
            out.println((withScore ? "| " + finding.score + " " : "") + "| `" + finding.definition + "` | " + finding.target + " | " + finding.action.name().toLowerCase(Locale.ROOT) + " | "
                + finding.authoredText() + " | " + finding.measuredText() + " | " + (Double.isNaN(finding.deltaPx) ? "" : String.format(Locale.ROOT, "%.1f", finding.deltaPx)) + " | "
                + String.join("; ", finding.reasons).replace("|", "\\|") + " |");
        }
    }

    private static void printSummary(List<Finding> findings, int definitions, int written, boolean write, Path report)
    {
        System.out.println("shootPointSync: " + definitions + " definitions, " + count(findings, Action.UPDATE) + (write ? " updated, " : " to update, ") + count(findings, Action.ADD)
            + (write ? " added, " : " to add, ") + count(findings, Action.SKIPPED) + " skipped, " + count(findings, Action.FAILED) + " failed; "
            + (write ? written + " files written" : filesToWrite(findings) + " files would change (run with -Pwrite)"));
        findings.stream().filter(finding -> finding.score >= CHECK_FIRST_SCORE).sorted(Comparator.comparingInt(finding -> -finding.score)).limit(10)
            .forEach(finding -> System.out.println("  [" + finding.score + "] " + finding.definition + " " + finding.target + ": " + String.join("; ", finding.reasons)));
        System.out.println("Report: " + report);
    }

    private static long count(List<Finding> findings, Action action)
    {
        return findings.stream().filter(finding -> finding.action == action).count();
    }

    private static long filesToWrite(List<Finding> findings)
    {
        return findings.stream().filter(Finding::changesFile).map(finding -> finding.definition).distinct().count();
    }

    private static String label(boolean write, String done, String planned)
    {
        return write ? done : planned;
    }

    private static String describe(Throwable throwable)
    {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root)
            root = root.getCause();
        return root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage());
    }

    private static List<Path> directories(Path parent) throws IOException
    {
        if (!Files.isDirectory(parent))
            return List.of();
        try (Stream<Path> children = Files.list(parent))
        {
            return children.filter(Files::isDirectory).sorted().toList();
        }
    }

    private static List<Path> textFiles(Path folder) throws IOException
    {
        if (!Files.isDirectory(folder))
            return List.of();
        try (Stream<Path> children = Files.list(folder))
        {
            return children.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")).sorted().toList();
        }
    }

    private static List<String> concat(List<String> first, String last)
    {
        List<String> all = new ArrayList<>(first);
        all.add(last);
        return all;
    }
}
