package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.List;
import java.util.Objects;

/** Makes standalone commands discoverable under the mod root without changing their original behavior. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModCommandAliases
{
    public static final String ROOT = "flansmodultimate";

    /** Register after the standalone server commands so redirects see their complete trees. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        for (String command : List.of("craftingparts", "defaultammo", "digitalammo", "rearm", "tryclass", "tryteam", "teams", "ranks", "vehiclephysics"))
            registerAlias(dispatcher, command, command);
        registerAlias(dispatcher, "particle", "fmparticle");
        registerAlias(dispatcher, "physics", "flansphysics");
    }

    /** Also usable by client command registration, without loading client classes on the server. */
    public static void registerAlias(CommandDispatcher<CommandSourceStack> dispatcher, String name, String original)
    {
        var target = Objects.requireNonNull(dispatcher.getRoot().getChild(original), "Register the standalone command before its alias: " + original);
        var alias = Commands.literal(name).requires(target.getRequirement()).redirect(target);
        // Brigadier follows a redirect only when more input follows it; keep the no-argument action too.
        if (target.getCommand() != null)
            alias.executes(target.getCommand());
        dispatcher.register(Commands.literal(ROOT).then(alias));
    }
}
