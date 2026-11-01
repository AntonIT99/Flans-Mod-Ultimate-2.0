package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.names.NpcDisplayNames;
import com.wolffsmod.npcs.names.SoldierNames;
import com.wolffsmod.npcs.network.*;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client handlers of the module's own packets. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcClientPackets
{
    public static void crewResult(CrewResultPacket packet)
    {
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod screen)
            screen.crewResult(packet);
    }

    public static void factionRelations(FactionRelationsPacket packet)
    {
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod screen)
            screen.factionRelations(packet);
    }

    public static void crewClonePreview(CrewClonePreviewPacket packet)
    {
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod screen && screen.getSubGui() instanceof CrewSelectionMenu menu)
            menu.receive(packet);
    }

    /** Updates the editor's display as well as the tracked NPC, so later editor saves retain the server-generated name. */
    public static void soldierName(SoldierNamePacket packet)
    {
        var level = Minecraft.getInstance().level;
        if (!packet.state() || level == null || !(level.getEntity(packet.entityId()) instanceof EntityCustomNpc npc))
            return;
        int generator = SoldierNames.generatorId(packet.nameList());
        if (!SoldierNames.acceptsAssociation(packet.nameList()) || !npc.modelData.extra.getString(FlanSoldierSelection.TEAM_KEY).equals(packet.team())
            || !npc.modelData.extra.getString(FlanSoldierSelection.CLASS_KEY).equals(packet.playerClass()))
            return;
        npc.display.setMarkovGeneratorId(generator);
        npc.display.setMarkovGender(packet.gender());
        NpcDisplayNames.literalName(npc.display, packet.name());
        NpcNetwork.sendToServer(packet);
    }

    public static void remoteNpcs(NpcRemotePacket packet)
    {
        if (Minecraft.getInstance().screen instanceof NpcRemoteReceiver receiver)
            receiver.wolffsmodnpcsReceive(packet);
    }

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

    /** Opens the bot editor, or shows the new state in it, also while one of its pickers is open over it. */
    public static void botEditor(BotEditorStatePacket packet)
    {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof BotPickerScreen picker)
            screen = picker.parent();
        if (screen instanceof BotEditorScreen editor)
            editor.update(packet);
        else
            Minecraft.getInstance().setScreen(new BotEditorScreen(packet));
    }

    public static void botClones(BotClonesPacket packet)
    {
        if (Minecraft.getInstance().screen instanceof BotClonePicker picker)
            picker.receive(packet);
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
        NpcPresetControls.receive(npc, packet);
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod tab)
            tab.presetsReceived(npc);
        else if (Minecraft.getInstance().screen instanceof noppes.npcs.client.gui.model.GuiCreationEntities picker && picker.npc == npc)
            picker.init();
    }

    public static void serverSettings(ServerControlPacket packet)
    {
        ClientServerSettings.receive(packet);
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod tab)
            tab.init();
    }

    public static void serverReport(ServerReportPacket packet)
    {
        ClientServerSettings.receive(packet);
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod tab)
            tab.init();
    }

    /** Shows what the server found of the NPC on the Diagnosis page, if the tab still edits it. */
    public static void inspection(NpcInspectPacket packet)
    {
        NpcInspectionView.receive(packet);
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod tab)
            tab.inspected(packet.entityId());
    }
}
