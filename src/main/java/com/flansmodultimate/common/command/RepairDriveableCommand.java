package com.flansmodultimate.common.command;

import com.flansmodultimate.api.parts.IFlanRepairTarget;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** Operator repair of a ridden driveable or an explicitly selected entity. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused") // Brigadier reaches the registered command callbacks through its command tree.
public final class RepairDriveableCommand
{
    private static final String TARGET = "target";
    private static final String PART = "part";
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansmodultimate")
            .then(Commands.literal("repair").requires(source -> source.hasPermission(2)).executes(context -> repairRidden(context.getSource()))
                .then(Commands.literal("start").then(Commands.argument(TARGET, EntityArgument.entity()).executes(context -> start(context.getSource(), EntityArgument.getEntity(context, TARGET)))))
                .then(Commands.argument(TARGET, EntityArgument.entity()).executes(context -> repair(context.getSource(), EntityArgument.getEntity(context, TARGET))))));
        dispatcher.register(Commands.literal("flansmodultimate")
            .then(Commands.literal("rebuild").then(Commands.argument(TARGET, EntityArgument.entity()).then(Commands.argument(PART, StringArgumentType.word()).executes(context ->
            {
                Entity target = EntityArgument.getEntity(context, TARGET);
                boolean rebuilt = target instanceof IFlanRepairTarget repairable
                    && repairable.flansmodultimateRebuild(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, PART));
                if (!rebuilt)
                    context.getSource().sendFailure(Component.literal("Move within 8 blocks of a surviving vehicle NPC and carry its part's required materials; parent parts must be intact"));
                if (rebuilt)
                    context.getSource().sendSuccess(() -> Component.literal("Rebuilt part of ").append(target.getDisplayName()), false);
                return rebuilt ? 1 : 0;
            })))));
    }

    private static int repairRidden(CommandSourceStack source) throws CommandSyntaxException
    {
        Entity vehicle = source.getPlayerOrException().getVehicle();
        return repair(source, vehicle);
    }

    private static int repair(CommandSourceStack source, Entity target)
    {
        if (target instanceof Seat seat)
            target = seat.getDriveable();
        boolean repaired = target instanceof Driveable driveable ? driveable.repairAllParts() : target instanceof IFlanRepairTarget repairable && repairable.flansmodultimateRestore();
        if (!repaired)
        {
            source.sendFailure(Component.literal("Ride or select a surviving driveable to repair it"));
            return 0;
        }
        Entity repairedTarget = target;
        source.sendSuccess(() -> Component.literal("Repaired all parts of ").append(repairedTarget.getDisplayName()).append(" to 100%"), true);
        return 1;
    }

    private static int start(CommandSourceStack source, Entity target)
    {
        if (!(target instanceof IFlanRepairTarget repairable) || !repairable.flansmodultimateStartRepair())
        {
            source.sendFailure(Component.literal("Select a surviving, stationary vehicle NPC; aircraft must be landed"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Started repair session for ").append(target.getDisplayName()), true);
        return 1;
    }
}
