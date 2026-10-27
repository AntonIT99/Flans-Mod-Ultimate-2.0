package com.flansmodultimate.common.command;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.mojang.brigadier.CommandDispatcher;
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
public final class RepairDriveableCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansmodultimate").then(Commands.literal("repair").requires(source -> source.hasPermission(2)).executes(context -> repairRidden(context.getSource()))
            .then(Commands.argument("target", EntityArgument.entity()).executes(context -> repair(context.getSource(), EntityArgument.getEntity(context, "target"))))));
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
        if (!(target instanceof Driveable driveable) || !driveable.repairAllParts())
        {
            source.sendFailure(Component.literal("Ride or select a surviving driveable to repair it"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Repaired all parts of ").append(driveable.getDisplayName()).append(" to 100%"), true);
        return 1;
    }
}
