package com.flansmodultimate.common;

import com.flansmodultimate.FlansMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Server permissions for interacting with placed Flan entities. */
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlanEntityPermissions
{
    public static final PermissionNode<Boolean> DRIVEABLE_PICKUP = node("driveable.pickup");
    public static final PermissionNode<Boolean> DRIVEABLE_ENTER = node("driveable.enter");
    public static final PermissionNode<Boolean> DRIVEABLE_ATTACK = node("driveable.attack");
    public static final PermissionNode<Boolean> AA_GUN_PICKUP = node("aa_gun.pickup");
    public static final PermissionNode<Boolean> AA_GUN_ENTER = node("aa_gun.enter");
    public static final PermissionNode<Boolean> AA_GUN_ATTACK = node("aa_gun.attack");
    public static final PermissionNode<Boolean> DEPLOYED_GUN_PICKUP = node("deployed_gun.pickup");
    public static final PermissionNode<Boolean> DEPLOYED_GUN_ENTER = node("deployed_gun.enter");
    public static final PermissionNode<Boolean> DEPLOYED_GUN_ATTACK = node("deployed_gun.attack");

    private FlanEntityPermissions() {}

    private static PermissionNode<Boolean> node(String path)
    {
        return new PermissionNode<>(FlansMod.MOD_ID, path, PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> true);
    }

    @SubscribeEvent
    public static void register(PermissionGatherEvent.Nodes event)
    {
        event.addNodes(DRIVEABLE_PICKUP, DRIVEABLE_ENTER, DRIVEABLE_ATTACK,
            AA_GUN_PICKUP, AA_GUN_ENTER, AA_GUN_ATTACK,
            DEPLOYED_GUN_PICKUP, DEPLOYED_GUN_ENTER, DEPLOYED_GUN_ATTACK);
    }

    public static boolean allows(Player player, PermissionNode<Boolean> node)
    {
        return !(player instanceof ServerPlayer serverPlayer) || PermissionAPI.getPermission(serverPlayer, node);
    }
}
