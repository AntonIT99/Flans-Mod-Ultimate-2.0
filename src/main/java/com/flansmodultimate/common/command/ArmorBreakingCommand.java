package com.flansmodultimate.common.command;

import com.flansmodultimate.config.EnumBreakableArmor;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator control over the common config's {@code breakableArmor}: {@code off} is unbreakable, {@code on} follows the content packs. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArmorBreakingCommand
{
    public static final String NAME = "armorBreaking";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(NAME).requires(source -> source.hasPermission(2)).executes(context -> report(context.getSource()));
        for (EnumBreakableArmor mode : EnumBreakableArmor.values())
            command.then(mode(mode.word(), mode));
        dispatcher.register(command.then(mode("on", EnumBreakableArmor.CONTENT_PACK)).then(mode("off", EnumBreakableArmor.UNBREAKABLE)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> mode(String word, EnumBreakableArmor mode)
    {
        return Commands.literal(word).executes(context ->
        {
            ModCommonConfig.setBreakableArmor(mode);
            return report(context.getSource());
        });
    }

    @SuppressWarnings("SameReturnValue") // Brigadier commands return a success count.
    private static int report(CommandSourceStack source)
    {
        String message = switch (ModCommonConfig.breakableArmor())
        {
            case UNBREAKABLE -> "Flan's armor never breaks.";
            case CONTENT_PACK -> "Flan's armor breaks as each content pack defines.";
            case ALL_BREAKABLE -> "All Flan's armor can break, with a durability of " + ModCommonConfig.get().defaultArmorDurability() + ".";
        };
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }
}
