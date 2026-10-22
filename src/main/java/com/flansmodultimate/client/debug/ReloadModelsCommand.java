package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.model.ModelCache;
import com.mojang.brigadier.CommandDispatcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Recreates client model instances and their render caches without reloading resources. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReloadModelsCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansmodultimate").then(Commands.literal("reloadModels").executes(context ->
        {
            ModelCache.reload();
            context.getSource().sendSuccess(() -> Component.literal("Reloaded Flan model caches. Java changes must be compiled and HotSwapped first."), false);
            return 1;
        })));
    }
}
