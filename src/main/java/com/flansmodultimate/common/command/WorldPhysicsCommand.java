package com.flansmodultimate.common.command;

import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator controls for the live, persisted Flan's physics multipliers. */
public final class WorldPhysicsCommand
{
    private WorldPhysicsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansphysics")
            .requires(source -> source.hasPermission(2))
            .executes(context -> report(context.getSource()))
            .then(Commands.literal("gravity")
                .executes(context -> show(context.getSource(), "Gravity", ModCommonConfig.gravityFactor()))
                .then(Commands.argument("factor", DoubleArgumentType.doubleArg(0D, 10D))
                    .executes(context -> set(context.getSource(), true,
                        DoubleArgumentType.getDouble(context, "factor")))))
            .then(Commands.literal("drag")
                .executes(context -> show(context.getSource(), "Drag", ModCommonConfig.dragFactor()))
                .then(Commands.argument("factor", DoubleArgumentType.doubleArg(0D, 10D))
                    .executes(context -> set(context.getSource(), false,
                        DoubleArgumentType.getDouble(context, "factor"))))));
    }

    private static int report(CommandSourceStack source)
    {
        source.sendSuccess(() -> Component.literal("Flan's gravity: " + ModCommonConfig.gravityFactor()
            + ", drag: " + ModCommonConfig.dragFactor()), false);
        return 1;
    }

    private static int show(CommandSourceStack source, String name, double value)
    {
        source.sendSuccess(() -> Component.literal(name + " factor: " + value), false);
        return 1;
    }

    private static int set(CommandSourceStack source, boolean gravity, double value)
    {
        if (gravity)
            ModCommonConfig.setGravityFactor(value);
        else
            ModCommonConfig.setDragFactor(value);
        double effective = gravity ? ModCommonConfig.gravityFactor() : ModCommonConfig.dragFactor();
        return show(source, gravity ? "Gravity" : "Drag", effective);
    }
}
