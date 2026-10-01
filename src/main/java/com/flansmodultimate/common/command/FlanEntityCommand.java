package com.flansmodultimate.common.command;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Administrative commands for finding and removing Flan entities. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanEntityCommand
{
    private static final int MAX_LISTED_ENTITIES = 100;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansmodultimate")
            .then(Commands.literal("entities")
                .requires(source -> source.hasPermission(2))
                .then(operation("list", false))
                .then(operation("remove", true))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> operation(
        String name, boolean remove)
    {
        var command = Commands.literal(name);
        addScopes(command, remove, false);
        if (remove)
        {
            var forceCommand = Commands.literal("force");
            addScopes(forceCommand, true, true);
            command.then(forceCommand);
        }
        return command;
    }

    private static void addScopes(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> command,
        boolean remove, boolean force)
    {
        var radius = Commands.argument("radius", DoubleArgumentType.doubleArg(0D));
        addKinds(radius, (context, kind) -> executeRadius(context, remove, force, kind));
        var dimension = Commands.argument("dimension", DimensionArgument.dimension());
        addKinds(dimension, (context, kind) -> executeDimension(context, remove, force, kind));
        var world = Commands.literal("world");
        addKinds(world, (context, kind) -> executeWorld(context, remove, force, kind));
        if (!remove)
        {
            radius.then(addKinds(Commands.literal("include_unloaded"),
                FlanEntityCommand::executeSavedRadius));
            dimension.then(addKinds(Commands.literal("include_unloaded"),
                FlanEntityCommand::executeSavedDimension));
            world.then(addKinds(Commands.literal("include_unloaded"),
                FlanEntityCommand::executeSavedWorld));
        }
        command.then(Commands.literal("radius").then(radius))
            .then(Commands.literal("dimension").then(dimension))
            .then(world);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addKinds(
        ArgumentBuilder<CommandSourceStack, ?> scope, KindCommand command)
    {
        scope.executes(context -> command.run(context, EntityKind.ALL));
        for (EntityKind kind : EntityKind.values())
        {
            scope.then(Commands.literal(kind.argument())
                .executes(context -> command.run(context, kind)));
        }
        return scope;
    }

    private static int executeRadius(CommandContext<CommandSourceStack> context, boolean remove, boolean force,
        EntityKind kind)
    {
        double radius = DoubleArgumentType.getDouble(context, "radius");
        Vec3 origin = context.getSource().getPosition();
        double radiusSquared = radius * radius;
        List<Entity> matches = findEntities(context.getSource().getLevel(), kind).stream()
            .filter(entity -> entity.distanceToSqr(origin) <= radiusSquared)
            .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
            .toList();
        return reportOrRemove(context.getSource(), matches, remove, force,
            "within " + format(radius) + " blocks", kind);
    }

    private static int executeDimension(CommandContext<CommandSourceStack> context, boolean remove, boolean force,
        EntityKind kind) throws CommandSyntaxException
    {
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        List<Entity> matches = findEntities(level, kind);
        matches.sort(Comparator.comparingDouble(Entity::getY));
        return reportOrRemove(context.getSource(), matches, remove, force,
            "in " + level.dimension().location(), kind);
    }

    private static int executeWorld(CommandContext<CommandSourceStack> context, boolean remove, boolean force,
        EntityKind kind)
    {
        List<Entity> matches = new ArrayList<>();
        for (ServerLevel level : context.getSource().getServer().getAllLevels())
            matches.addAll(findEntities(level, kind));
        matches.sort(Comparator.comparing(entity -> entity.level().dimension().location().toString()));
        return reportOrRemove(context.getSource(), matches, remove, force, "in all loaded dimensions", kind);
    }

    private static int executeSavedRadius(CommandContext<CommandSourceStack> context, EntityKind kind)
    {
        double radius = DoubleArgumentType.getDouble(context, "radius");
        return executeSaved(context.getSource(), context.getSource().getLevel(), context.getSource().getPosition(),
            radius, kind, "within " + format(radius) + " blocks, including unloaded chunks");
    }

    private static int executeSavedDimension(CommandContext<CommandSourceStack> context, EntityKind kind)
        throws CommandSyntaxException
    {
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        return executeSaved(context.getSource(), level, null, 0D, kind,
            "in " + level.dimension().location() + ", including unloaded chunks");
    }

    private static int executeSavedWorld(CommandContext<CommandSourceStack> context, EntityKind kind)
    {
        return executeSaved(context.getSource(), null, null, 0D, kind,
            "in all dimensions, including unloaded chunks");
    }

    private static int executeSaved(CommandSourceStack source, @Nullable ServerLevel onlyLevel,
        @Nullable Vec3 origin, double radius, EntityKind kind, String scope)
    {
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        List<SavedFlanEntityScanner.Source> directories = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels())
        {
            if (onlyLevel == null || onlyLevel == level)
                directories.add(new SavedFlanEntityScanner.Source(level.dimension().location(),
                    DimensionType.getStorageFolder(level.dimension(), worldRoot).resolve("entities")));
        }
        source.sendSuccess(() -> Component.literal("Scanning saved Flan entities in the background..."), false);
        CompletableFuture.supplyAsync(() -> SavedFlanEntityScanner.scan(directories))
            .whenComplete((result, error) -> server.execute(() -> {
                if (error != null)
                {
                    source.sendFailure(Component.literal("Could not scan saved entity regions: " + error.getMessage()));
                    return;
                }
                reportSaved(source, result, onlyLevel, origin, radius, kind, scope);
            }));
        return 1;
    }

    private static void reportSaved(CommandSourceStack source, SavedFlanEntityScanner.Result result,
        @Nullable ServerLevel onlyLevel, @Nullable Vec3 origin, double radius, EntityKind kind, String scope)
    {
        Map<ResourceLocation, ServerLevel> levels = new HashMap<>();
        for (ServerLevel level : source.getServer().getAllLevels())
            levels.put(level.dimension().location(), level);
        double radiusSquared = radius * radius;
        List<Entity> loaded = new ArrayList<>();
        for (ServerLevel level : levels.values())
        {
            if (onlyLevel != null && onlyLevel != level)
                continue;
            for (Entity entity : findEntities(level, kind))
            {
                if (origin == null || entity.distanceToSqr(origin) <= radiusSquared)
                    loaded.add(entity);
            }
        }
        loaded.sort(origin == null
            ? Comparator.comparing((Entity entity) -> entity.level().dimension().location().toString())
                .thenComparingDouble(Entity::getX).thenComparingDouble(Entity::getZ)
            : Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)));
        List<SavedFlanEntityScanner.Entry> unloaded = result.entries().stream()
            .filter(entry -> kind.matchesSaved(entry.id()))
            .filter(entry -> onlyLevel == null || entry.dimension().equals(onlyLevel.dimension().location()))
            .filter(entry -> origin == null || distanceSquared(entry, origin) <= radiusSquared)
            .filter(entry -> {
                ServerLevel level = levels.get(entry.dimension());
                return level != null && !level.getChunkSource().hasChunk(entry.chunkX(), entry.chunkZ());
            })
            .sorted(origin == null
                ? Comparator.comparing((SavedFlanEntityScanner.Entry entry) -> entry.dimension().toString())
                    .thenComparingDouble(SavedFlanEntityScanner.Entry::x)
                    .thenComparingDouble(SavedFlanEntityScanner.Entry::z)
                : Comparator.comparingDouble(entry -> distanceSquared(entry, origin)))
            .toList();
        int shown = Math.min(loaded.size() + unloaded.size(), MAX_LISTED_ENTITIES);
        for (int index = 0; index < shown; index++)
        {
            Component description = index < loaded.size()
                ? describe(loaded.get(index)) : describeSaved(unloaded.get(index - loaded.size()));
            source.sendSuccess(() -> description, false);
        }
        if (shown < loaded.size() + unloaded.size())
            source.sendSuccess(() -> Component.literal("... and " + (loaded.size() + unloaded.size() - shown)
                + " more (details limited to " + MAX_LISTED_ENTITIES + ")").withStyle(ChatFormatting.GRAY), false);
        Counts counts = count(loaded).plus(countSaved(unloaded));
        source.sendSuccess(() -> summary("Found", counts, scope, kind), false);
        if (result.failedChunks() > 0)
            source.sendFailure(Component.literal("Could not read " + result.failedChunks()
                + " saved region/chunk files; the unloaded count may be incomplete."));
    }

    private static double distanceSquared(SavedFlanEntityScanner.Entry entry, Vec3 origin)
    {
        double x = entry.x() - origin.x;
        double y = entry.y() - origin.y;
        double z = entry.z() - origin.z;
        return x * x + y * y + z * z;
    }

    private static List<Entity> findEntities(ServerLevel level, EntityKind kind)
    {
        List<Entity> matches = new ArrayList<>();
        for (Entity entity : level.getAllEntities())
        {
            if (kind.matches(entity))
                matches.add(entity);
        }
        return matches;
    }

    private static int reportOrRemove(CommandSourceStack source, List<Entity> matches, boolean remove, boolean force,
        String scope, EntityKind kind)
    {
        if (remove)
        {
            List<Entity> removable = force ? matches : matches.stream().filter(entity -> !isOccupied(entity)).toList();
            int skipped = matches.size() - removable.size();
            for (Entity entity : removable)
                discardWithoutDrops(entity);
            Counts removedCounts = count(removable);
            source.sendSuccess(() -> summary("Removed", removedCounts, scope, kind), true);
            if (skipped > 0)
                source.sendSuccess(() -> Component.literal("Skipped " + skipped + " occupied entities."), false);
            return removable.size();
        }

        Counts counts = count(matches);
        int shown = Math.min(matches.size(), MAX_LISTED_ENTITIES);
        for (int i = 0; i < shown; i++)
        {
            Entity entity = matches.get(i);
            source.sendSuccess(() -> describe(entity), false);
        }
        if (shown < matches.size())
        {
            int omitted = matches.size() - shown;
            source.sendSuccess(() -> Component.literal("... and " + omitted + " more (details limited to "
                + MAX_LISTED_ENTITIES + ")").withStyle(ChatFormatting.GRAY), false);
        }
        source.sendSuccess(() -> summary("Found", counts, scope, kind), false);
        return matches.size();
    }

    private static void discardWithoutDrops(Entity entity)
    {
        if (entity instanceof DeployedGun gun)
            gun.discardWithoutDrops();
        else if (entity instanceof AAGun gun)
            gun.discardWithoutDrops();
        else
            entity.discard();
    }

    private static boolean isOccupied(Entity entity)
    {
        if (entity instanceof Driveable driveable)
            return driveable.hasDriveableOccupant();
        return !entity.getPassengers().isEmpty();
    }

    private static Counts count(List<Entity> entities)
    {
        int vehicles = 0;
        int planes = 0;
        int mechas = 0;
        int otherDriveables = 0;
        int deployedGuns = 0;
        int aaGuns = 0;
        for (Entity entity : entities)
        {
            if (entity instanceof Vehicle)
                vehicles++;
            else if (entity instanceof Plane)
                planes++;
            else if (entity instanceof Mecha)
                mechas++;
            else if (entity instanceof Driveable)
                otherDriveables++;
            else if (entity instanceof DeployedGun)
                deployedGuns++;
            else if (entity instanceof AAGun)
                aaGuns++;
        }
        return new Counts(vehicles, planes, mechas, otherDriveables, deployedGuns, aaGuns);
    }

    private static Counts countSaved(List<SavedFlanEntityScanner.Entry> entries)
    {
        int vehicles = 0;
        int planes = 0;
        int mechas = 0;
        int deployedGuns = 0;
        int aaGuns = 0;
        for (SavedFlanEntityScanner.Entry entry : entries)
        {
            switch (entry.id())
            {
                case "flansmodultimate:vehicle" -> vehicles++;
                case "flansmodultimate:plane" -> planes++;
                case "flansmodultimate:mecha" -> mechas++;
                case "flansmodultimate:deployed_gun" -> deployedGuns++;
                case "flansmodultimate:aa_gun" -> aaGuns++;
                default -> { }
            }
        }
        return new Counts(vehicles, planes, mechas, 0, deployedGuns, aaGuns);
    }

    private static Component summary(String verb, Counts counts, String scope, EntityKind kind)
    {
        return Component.literal(verb + " " + counts.total() + " " + kind.label() + " " + scope + ": "
            + counts.vehicles() + " vehicles, " + counts.planes() + " planes, "
            + counts.mechas() + " mechas, " + counts.otherDriveables() + " other driveables, "
            + counts.deployedGuns() + " deployed guns, " + counts.aaGuns() + " AA guns");
    }

    private static Component describe(Entity entity)
    {
        String kind;
        String type;
        if (entity instanceof Vehicle driveable)
        {
            kind = "vehicle";
            type = driveable.getShortName();
        }
        else if (entity instanceof Plane driveable)
        {
            kind = "plane";
            type = driveable.getShortName();
        }
        else if (entity instanceof Mecha driveable)
        {
            kind = "mecha";
            type = driveable.getShortName();
        }
        else if (entity instanceof Driveable driveable)
        {
            kind = "driveable";
            type = driveable.getShortName();
        }
        else if (entity instanceof DeployedGun gun)
        {
            kind = "deployable gun";
            type = gun.getShortName();
        }
        else
        {
            AAGun gun = (AAGun)entity;
            kind = "AA gun";
            type = gun.getShortName();
        }
        String dimension = entity.level().dimension().location().toString();
        return Component.literal(kind + " " + type + " #" + entity.getId() + " at "
            + format(entity.getX()) + " " + format(entity.getY()) + " " + format(entity.getZ())
            + " in " + dimension).withStyle(ChatFormatting.GRAY);
    }

    private static Component describeSaved(SavedFlanEntityScanner.Entry entry)
    {
        String kind = switch (entry.id())
        {
            case "flansmodultimate:vehicle" -> "vehicle";
            case "flansmodultimate:plane" -> "plane";
            case "flansmodultimate:mecha" -> "mecha";
            case "flansmodultimate:deployed_gun" -> "deployable gun";
            case "flansmodultimate:aa_gun" -> "AA gun";
            default -> "entity";
        };
        return Component.literal(kind + " " + entry.type() + " at " + format(entry.x()) + " "
            + format(entry.y()) + " " + format(entry.z()) + " in " + entry.dimension()
            + " (saved in unloaded chunk)").withStyle(ChatFormatting.GRAY);
    }

    private static String format(double value)
    {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @FunctionalInterface
    private interface KindCommand
    {
        int run(CommandContext<CommandSourceStack> context, EntityKind kind) throws CommandSyntaxException;
    }

    private enum EntityKind
    {
        ALL("all", "Flan entities"),
        DRIVEABLES("driveables", "driveables"),
        VEHICLES("vehicles", "vehicles"),
        PLANES("planes", "planes"),
        MECHAS("mechas", "mechas"),
        AA_GUNS("aa_guns", "AA guns"),
        DEPLOYED_GUNS("deployed_guns", "deployed guns");

        private final String argument;
        private final String label;

        EntityKind(String argument, String label)
        {
            this.argument = argument;
            this.label = label;
        }

        private String argument()
        {
            return argument;
        }

        private String label()
        {
            return label;
        }

        private boolean matches(Entity entity)
        {
            return switch (this)
            {
                case ALL -> entity instanceof Driveable || entity instanceof DeployedGun || entity instanceof AAGun;
                case DRIVEABLES -> entity instanceof Driveable;
                case VEHICLES -> entity instanceof Vehicle;
                case PLANES -> entity instanceof Plane;
                case MECHAS -> entity instanceof Mecha;
                case AA_GUNS -> entity instanceof AAGun;
                case DEPLOYED_GUNS -> entity instanceof DeployedGun;
            };
        }

        private boolean matchesSaved(String id)
        {
            return switch (this)
            {
                case ALL -> true;
                case DRIVEABLES -> id.equals("flansmodultimate:vehicle")
                    || id.equals("flansmodultimate:plane") || id.equals("flansmodultimate:mecha");
                case VEHICLES -> id.equals("flansmodultimate:vehicle");
                case PLANES -> id.equals("flansmodultimate:plane");
                case MECHAS -> id.equals("flansmodultimate:mecha");
                case AA_GUNS -> id.equals("flansmodultimate:aa_gun");
                case DEPLOYED_GUNS -> id.equals("flansmodultimate:deployed_gun");
            };
        }
    }

    private record Counts(int vehicles, int planes, int mechas, int otherDriveables,
        int deployedGuns, int aaGuns)
    {
        private Counts plus(Counts other)
        {
            return new Counts(vehicles + other.vehicles, planes + other.planes, mechas + other.mechas,
                otherDriveables + other.otherDriveables, deployedGuns + other.deployedGuns, aaGuns + other.aaGuns);
        }

        private int total()
        {
            return vehicles + planes + mechas + otherDriveables + deployedGuns + aaGuns;
        }
    }
}
