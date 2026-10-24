package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/** Overview of the main mod's command roots and their detailed help. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModHelpCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansmodultimate").then(Commands.literal("help").executes(context -> help(context.getSource()))));
    }

    private static int help(CommandSourceStack source)
    {
        source.sendSuccess(() -> Component.literal("Flan's Mod Ultimate commands (click a help command to fill the chat):").withStyle(ChatFormatting.GOLD), false);
        entry(source, "/flansmodultimate", "Attachments, debug, help and operator entity administration", "/help flansmodultimate");
        entry(source, "/teams, /flansteams, /team", "Teams, rounds, maps and loadouts", "/teams help");
        entry(source, "/tryclass", "Try a player class (operator)", "/help tryclass");
        entry(source, "/tryteam", "Try team equipment (operator)", "/help tryteam");
        entry(source, "/craftingparts", "Give crafting ingredients for the held item (operator)", "/help craftingparts");
        entry(source, "/defaultammo", "Give default ammunition (operator)", "/help defaultammo");
        entry(source, "/rearm", "Rearm a driveable or mounted gun (operator)", "/help rearm");
        entry(source, "/digitalammo", "Manage digital ammunition (operator)", "/help digitalammo");
        entry(source, "/fmparticle", "Spawn mod particles (operator)", "/help fmparticle");
        entry(source, "/flansdebug, /flansmodultimate debug", "Edit hitboxes and shoot points; control vehicle collision (operator)", "/help flansdebug");
        entry(source, "/vehiclephysics", "Report vehicle physics (operator)", "/help vehiclephysics");
        entry(source, "/flansphysics", "Inspect and change dimension physics (operator)", "/help flansphysics");
        source.sendSuccess(() -> Component.literal("/flansrenderstats - Client-only render diagnostics: start, stop, reset, bridge [indices]").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("/flansmodultimate reloadModels - Client-only model cache reload; HotSwap Java changes first").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("/flansdebug aimoffset [x y z | reset | clear] - Client-only held-gun aim offset preview in model units").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Operator commands require permission level 2; their detailed help is available to operators.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static void entry(CommandSourceStack source, String roots, String description, String helpCommand)
    {
        source.sendSuccess(
            () -> Component.literal(roots).withStyle(ChatFormatting.AQUA).append(Component.literal(" - " + description + "; ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(helpCommand).withStyle(style -> style.withColor(ChatFormatting.YELLOW).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, helpCommand)))),
            false);
    }
}
