package com.wolffsmod.npcs.client;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.SoldierNamePacket;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;

import static org.mockito.Mockito.*;

class NpcClientNamesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("IgnoreResultOfCall") // Mockito records the static Minecraft.getInstance method reference for stubbing.
    void serverReplyUpdatesEditorGenderAndAcknowledgesTheResult()
    {
        Minecraft minecraft = mock(Minecraft.class);
        minecraft.level = mock(ClientLevel.class);
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        npc.display = mock(DataDisplay.class);
        npc.modelData = mock(ModelData.class);
        npc.modelData.extra = new CompoundTag();
        npc.modelData.extra.putString(FlanSoldierSelection.TEAM_KEY, "team");
        npc.modelData.extra.putString(FlanSoldierSelection.CLASS_KEY, "class");
        when(minecraft.level.getEntity(42)).thenReturn(npc);
        SoldierNamePacket packet = new SoldierNamePacket(42, "team", "class", true, "wolffsmodnpcs:french", "Louis Martin", 1);
        try (var client = mockStatic(Minecraft.class); var network = mockStatic(NpcNetwork.class))
        {
            client.when(Minecraft::getInstance).thenReturn(minecraft);
            NpcClientPackets.soldierName(packet);
            verify(npc.display).setMarkovGeneratorId(70);
            verify(npc.display).setMarkovGender(1);
            verify(npc.display).setName("Louis Martin");
            network.verify(() -> NpcNetwork.sendToServer(packet));
        }
    }
}
