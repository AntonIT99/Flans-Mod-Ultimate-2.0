package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.*;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.client.Minecraft;

/** Client handlers of the module's own packets. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcClientPackets
{
    public static void vehicleState(VehicleStatePacket packet)
    {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof EntityNPCInterface npc)
            NpcVehicleState.of(npc).receive(packet);
    }

    public static void partState(PartStatePacket packet)
    {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof EntityNPCInterface npc)
            NpcVehicleState.of(npc).getParts().receive(packet);
    }

    /** Opens the squad order screen, or shows the new state on it. */
    public static void botOrders(BotOrdersStatePacket packet)
    {
        if (Minecraft.getInstance().screen instanceof BotOrdersScreen screen)
            screen.update(packet);
        else if (packet.open())
            Minecraft.getInstance().setScreen(new BotOrdersScreen(packet));
    }

    /** Opens the bot editor, or shows the new state in it. */
    public static void botEditor(BotEditorStatePacket packet)
    {
        if (Minecraft.getInstance().screen instanceof BotEditorScreen screen)
            screen.update(packet);
        else
            Minecraft.getInstance().setScreen(new BotEditorScreen(packet));
    }

    public static void waypoints(BotWaypointsPacket packet)
    {
        BotWaypoints.receive(packet);
    }

    /** Gives the edited NPC the preset settings the server holds, and shows them on the Wolff's Mod tab. */
    public static void presetState(NpcPresetPacket packet)
    {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(packet.entityId()) instanceof EntityNPCInterface npc))
            return;
        packet.copyTo(npc);
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod tab)
            tab.presetsReceived(npc);
    }

    public static void serverSettings(ServerControlPacket packet)
    {
        ClientServerSettings.receive(packet);
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod tab)
            tab.init();
    }
}
