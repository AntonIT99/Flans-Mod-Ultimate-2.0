package com.flansmodultimate.common.command;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.ThrownGunLifetime;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator controls for recoverable thrown weapons, independent of teams rounds. */
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused") // Forge discovers the subscriber and handler through annotations.
public final class ThrownGunDespawnCommand
{
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("thrownGunDespawn").requires(source -> source.hasPermission(2)).executes(context -> report(context.getSource()))
            .then(Commands.argument("seconds", IntegerArgumentType.integer(0, Integer.MAX_VALUE / 20)).executes(context ->
            {
                ThrownGunLifetime.get(context.getSource().getLevel()).setSeconds(IntegerArgumentType.getInteger(context, "seconds"));
                return report(context.getSource());
            })).then(Commands.literal("reset").executes(context ->
            {
                ThrownGunLifetime.get(context.getSource().getLevel()).setSeconds(-1);
                return report(context.getSource());
            })));
    }

    @SuppressWarnings("SameReturnValue") // Brigadier commands return a success count.
    private static int report(CommandSourceStack source)
    {
        int seconds = ThrownGunLifetime.get(source.getLevel()).seconds();
        source.sendSuccess(() -> Component.literal(seconds == 0 ? "Recoverable thrown guns never despawn." : "Recoverable thrown guns despawn after " + seconds + " seconds stuck in the ground."),
            true);
        return 1;
    }
}
