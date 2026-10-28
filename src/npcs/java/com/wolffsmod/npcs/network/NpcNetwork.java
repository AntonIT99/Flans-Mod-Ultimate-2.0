package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.platform.network.NpcNetworkPlatform;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** The module's own packets. Everything else it sends rides on Custom NPCs' packets. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcNetwork
{
    /** Declares the module's packets; the order numbers them on Forge. */
    public static void register()
    {
        // The client handler class is only loaded when a client runs one of these handlers.
        NpcNetworkPlatform.toClient("vehicle_state", VehicleStatePacket.class, VehicleStatePacket::write, VehicleStatePacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::vehicleState);
        NpcNetworkPlatform.bidirectional("server_control", ServerControlPacket.class, ServerControlPacket::write, ServerControlPacket::read,
            () -> com.wolffsmod.npcs.client.NpcClientPackets::serverSettings, ServerControlPacket::handle);
        NpcNetworkPlatform.toServer("apply_loadout", ApplyLoadoutPacket.class, ApplyLoadoutPacket::write, ApplyLoadoutPacket::read, ApplyLoadoutPacket::handle);
        NpcNetworkPlatform.toClient("part_state", PartStatePacket.class, PartStatePacket::write, PartStatePacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::partState);
        NpcNetworkPlatform.toServer("bot_order", BotOrderPacket.class, BotOrderPacket::write, BotOrderPacket::read, BotOrderPacket::handle);
        NpcNetworkPlatform.toClient("bot_orders_state", BotOrdersStatePacket.class, BotOrdersStatePacket::write, BotOrdersStatePacket::read,
            () -> com.wolffsmod.npcs.client.NpcClientPackets::botOrders);
        NpcNetworkPlatform.toServer("bot_editor_action", BotEditorActionPacket.class, BotEditorActionPacket::write, BotEditorActionPacket::read, BotEditorActionPacket::handle);
        NpcNetworkPlatform.toClient("bot_editor_state", BotEditorStatePacket.class, BotEditorStatePacket::write, BotEditorStatePacket::read,
            () -> com.wolffsmod.npcs.client.NpcClientPackets::botEditor);
        NpcNetworkPlatform.toClient("bot_waypoints", BotWaypointsPacket.class, BotWaypointsPacket::write, BotWaypointsPacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::waypoints);
        NpcNetworkPlatform.toServer("crew_action", CrewActionPacket.class, CrewActionPacket::write, CrewActionPacket::read, CrewActionPacket::handle);
        NpcNetworkPlatform.bidirectional("npc_preset", NpcPresetPacket.class, NpcPresetPacket::write, NpcPresetPacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::presetState,
            NpcPresetPacket::handle);
        NpcNetworkPlatform.toClient("muzzle_flash", MuzzleFlashPacket.class, MuzzleFlashPacket::write, MuzzleFlashPacket::read, () -> com.wolffsmod.npcs.client.NpcMuzzleFlashRenderer::receive);
        NpcNetworkPlatform.toClient("npc_remote", NpcRemotePacket.class, NpcRemotePacket::write, NpcRemotePacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::remoteNpcs);
        NpcNetworkPlatform.bidirectional("soldier_name", SoldierNamePacket.class, SoldierNamePacket::write, SoldierNamePacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::soldierName,
            SoldierNamePacket::handle);
        NpcNetworkPlatform.toServer("team_faction", TeamFactionPacket.class, TeamFactionPacket::write, TeamFactionPacket::read, TeamFactionPacket::handle);
        NpcNetworkPlatform.bidirectional("faction_relations", FactionRelationsPacket.class, FactionRelationsPacket::write, FactionRelationsPacket::read,
            () -> com.wolffsmod.npcs.client.NpcClientPackets::factionRelations, FactionRelationsPacket::handle);
        NpcNetworkPlatform.toServer("faction_joining", FactionJoiningPacket.class, FactionJoiningPacket::write, FactionJoiningPacket::read, FactionJoiningPacket::handle);
        NpcNetworkPlatform.toClient("server_report", ServerReportPacket.class, ServerReportPacket::write, ServerReportPacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::serverReport);
        NpcNetworkPlatform.bidirectional("npc_inspect", NpcInspectPacket.class, NpcInspectPacket::write, NpcInspectPacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::inspection,
            NpcInspectPacket::handle);
        NpcNetworkPlatform.toClient("debug_label", DebugLabelPacket.class, DebugLabelPacket::write, DebugLabelPacket::read, () -> com.wolffsmod.npcs.client.NpcDebugLabels::receive);
        NpcNetworkPlatform.toClient("crew_result", CrewResultPacket.class, CrewResultPacket::write, CrewResultPacket::read, () -> com.wolffsmod.npcs.client.NpcClientPackets::crewResult);
        NpcNetworkPlatform.bidirectional("crew_clone_preview", CrewClonePreviewPacket.class, CrewClonePreviewPacket::write, CrewClonePreviewPacket::read,
            () -> com.wolffsmod.npcs.client.NpcClientPackets::crewClonePreview, CrewClonePreviewPacket::handle);
        NpcNetworkPlatform.toServer("gunner_trigger", GunnerTriggerPacket.class, GunnerTriggerPacket::write, GunnerTriggerPacket::read, GunnerTriggerPacket::handle);
    }

    public static void sendTo(ServerPlayer player, ServerReportPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendTo(ServerPlayer player, CrewResultPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendTo(ServerPlayer player, CrewClonePreviewPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendToServer(CrewClonePreviewPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendTo(ServerPlayer player, NpcInspectPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendToServer(NpcInspectPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendTo(ServerPlayer player, DebugLabelPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendTo(ServerPlayer player, ServerControlPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendTo(ServerPlayer player, NpcRemotePacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendToServer(ServerControlPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendTo(ServerPlayer player, NpcPresetPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendToServer(NpcPresetPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendTo(ServerPlayer player, BotOrdersStatePacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendTo(ServerPlayer player, BotWaypointsPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendTo(ServerPlayer player, BotEditorStatePacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendToServer(BotOrderPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToServer(BotEditorActionPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToServer(ApplyLoadoutPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToServer(TeamFactionPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToServer(FactionRelationsPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToServer(FactionJoiningPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendTo(ServerPlayer player, FactionRelationsPacket packet)
    {
        NpcNetworkPlatform.sendToPlayer(player, packet);
    }

    public static void sendToServer(SoldierNamePacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToServer(CrewActionPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToServer(GunnerTriggerPacket packet)
    {
        NpcNetworkPlatform.sendToServer(packet);
    }

    public static void sendToTracking(EntityNPCInterface npc, VehicleStatePacket packet)
    {
        NpcNetworkPlatform.sendToTracking(npc, packet);
    }

    public static void sendToTracking(EntityNPCInterface npc, PartStatePacket packet)
    {
        NpcNetworkPlatform.sendToTracking(npc, packet);
    }

    public static void sendToTracking(EntityNPCInterface npc, MuzzleFlashPacket packet)
    {
        NpcNetworkPlatform.sendToTracking(npc, packet);
    }

    /** A client that starts tracking an NPC needs its current state, not just the next change. */
    public static void startTracking(Entity target, ServerPlayer player)
    {
        if (target instanceof EntityNPCInterface npc && NpcVehicleState.of(npc).isActive())
        {
            NpcNetworkPlatform.sendToPlayer(player, NpcVehicleState.of(npc).packet(npc));
            NpcVehicleState.of(npc).getParts().packet(npc).ifPresent(packet -> NpcNetworkPlatform.sendToPlayer(player, packet));
        }
    }
}
