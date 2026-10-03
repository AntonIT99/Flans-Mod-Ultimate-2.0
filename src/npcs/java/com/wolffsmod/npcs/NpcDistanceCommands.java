package com.wolffsmod.npcs;

import com.flansmodultimate.api.IFlanNpcDistance;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Explicit soldier opt-in, independent of held weapons and selected skins. */
@EventBusSubscriber(modid = NpcsMod.MOD_ID)
public final class NpcDistanceCommands
{
    private NpcDistanceCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("flansnpcdistance")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                    int changed = 0;
                    for (Entity entity : EntityArgument.getEntities(context, "targets"))
                    {
                        if (entity instanceof IFlanNpcDistance npc)
                        {
                            npc.setFlanNpcDistanceOptIn(enabled);
                            changed++;
                        }
                    }
                    if (changed == 0)
                        context.getSource().sendFailure(Component.translatable("commands.flansmodultimate.npcdistance.none"));
                    else
                    {
                        int count = changed;
                        context.getSource().sendSuccess(() -> Component.translatable("commands.flansmodultimate.npcdistance.success", count, enabled), true);
                    }
                    return changed;
                }))));
    }
}
