package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.FlansMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** Explicit editor access; default-denied permission nodes can be granted by a server permission handler. */
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EditorPermissions
{
    public static final PermissionNode<Boolean> EDITOR = new PermissionNode<>(FlansMod.MOD_ID, "teams.editor", PermissionTypes.BOOLEAN, (player, uuid, contexts) -> false);

    private EditorPermissions()
    {}

    @SubscribeEvent
    @SuppressWarnings("unused") // Invoked by Forge's permission event bus.
    public static void register(PermissionGatherEvent.Nodes event)
    {
        event.addNodes(EDITOR);
    }

    @SuppressWarnings("PointlessBooleanExpression") // Keep boxed permission results null-safe, as required by Sonar java:S5411.
    public static boolean mayEdit(ServerPlayer player)
    {
        return player.hasPermissions(2) || player.getServer() != null && player.getServer().isSingleplayerOwner(player.getGameProfile())
            || Boolean.TRUE.equals(PermissionAPI.getPermission(player, EDITOR));
    }

    public static boolean mayEdit(CommandSourceStack source)
    {
        return source.getEntity() instanceof ServerPlayer player ? mayEdit(player) : source.hasPermission(2);
    }
}
