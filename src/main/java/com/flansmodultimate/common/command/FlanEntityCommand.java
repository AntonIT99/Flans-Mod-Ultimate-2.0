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

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Administrative commands for finding and removing Flan entities. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanEntityCommand
{
    private static final int MAX_LISTED_ENTITIES = 100;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansmodultimate")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("entities")
                .then(operation("list", false))
                .then(operation("remove", true))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> operation(
        String name, boolean remove)
    {
        return Commands.literal(name)
            .then(Commands.literal("radius")
                .then(addKinds(Commands.argument("radius", DoubleArgumentType.doubleArg(0D)),
                    (context, kind) -> executeRadius(context, remove, kind))))
            .then(Commands.literal("dimension")
                .then(addKinds(Commands.argument("dimension", DimensionArgument.dimension()),
                    (context, kind) -> executeDimension(context, remove, kind))))
            .then(addKinds(Commands.literal("world"),
                (context, kind) -> executeWorld(context, remove, kind)));
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

    private static int executeRadius(CommandContext<CommandSourceStack> context, boolean remove,
        EntityKind kind)
    {
        double radius = DoubleArgumentType.getDouble(context, "radius");
        Vec3 origin = context.getSource().getPosition();
        double radiusSquared = radius * radius;
        List<Entity> matches = findEntities(context.getSource().getLevel(), kind).stream()
            .filter(entity -> entity.distanceToSqr(origin) <= radiusSquared)
            .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
            .toList();
        return reportOrRemove(context.getSource(), matches, remove,
            "within " + format(radius) + " blocks", kind);
    }

    private static int executeDimension(CommandContext<CommandSourceStack> context, boolean remove,
        EntityKind kind) throws CommandSyntaxException
    {
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        List<Entity> matches = findEntities(level, kind);
        matches.sort(Comparator.comparingDouble(Entity::getY));
        return reportOrRemove(context.getSource(), matches, remove,
            "in " + level.dimension().location(), kind);
    }

    private static int executeWorld(CommandContext<CommandSourceStack> context, boolean remove,
        EntityKind kind)
    {
        List<Entity> matches = new ArrayList<>();
        for (ServerLevel level : context.getSource().getServer().getAllLevels())
            matches.addAll(findEntities(level, kind));
        matches.sort(Comparator.comparing(entity -> entity.level().dimension().location().toString()));
        return reportOrRemove(context.getSource(), matches, remove, "in all loaded dimensions", kind);
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

    private static int reportOrRemove(CommandSourceStack source, List<Entity> matches, boolean remove,
        String scope, EntityKind kind)
    {
        Counts counts = count(matches);
        if (remove)
        {
            for (Entity entity : matches)
                discardWithoutDrops(entity);
            source.sendSuccess(() -> summary("Removed", counts, scope, kind), true);
            return matches.size();
        }

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
    }

    private record Counts(int vehicles, int planes, int mechas, int otherDriveables,
        int deployedGuns, int aaGuns)
    {
        private int total()
        {
            return vehicles + planes + mechas + otherDriveables + deployedGuns + aaGuns;
        }
    }
}
