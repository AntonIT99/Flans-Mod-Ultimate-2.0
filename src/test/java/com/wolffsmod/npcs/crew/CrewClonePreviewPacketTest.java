package com.wolffsmod.npcs.crew;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.network.CrewClonePreviewPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import io.netty.buffer.Unpooled;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrewClonePreviewPacketTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void onlyRepliesCarryEntityDataAndPreserveTheSelectionToken()
    {
        CompoundTag data = new CompoundTag();
        data.putString("Name", "Crewman");
        UUID token = UUID.randomUUID();
        for (boolean request : List.of(true, false))
        {
            CrewClonePreviewPacket packet = new CrewClonePreviewPacket(42, 3, "Crewman", token, request, data);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try
            {
                packet.write(buffer);
                CrewClonePreviewPacket decoded = CrewClonePreviewPacket.read(buffer);
                assertEquals(new CrewClonePreviewPacket(42, 3, "Crewman", token, request, request ? new CompoundTag() : data), decoded);
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
    }

    @Test
    void serverChecksTheEditorWandPermissionAndCloneMembershipBeforeReading()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(npc.level()).thenReturn(level);
        when(npc.getId()).thenReturn(42);
        when(npc.isAlive()).thenReturn(true);
        Inventory inventory = mock(Inventory.class);
        ItemStack wand = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getSelected()).thenReturn(wand);
        when(wand.getItem()).thenReturn(CustomItems.wand);
        ServerCloneController previous = ServerCloneController.Instance;
        ServerCloneController clones = mock(ServerCloneController.class);
        ServerCloneController.Instance = clones;
        when(clones.getClones(3)).thenReturn(List.of("Crewman"));
        CompoundTag data = new CompoundTag();
        data.putString("Name", "Crewman");
        when(clones.getCloneData(null, "Crewman", 3)).thenReturn(data);
        UUID token = UUID.randomUUID();
        CrewClonePreviewPacket request = CrewClonePreviewPacket.request(42, 3, "Crewman", token);
        try (var editor = mockStatic(NoppesUtilServer.class); var permissions = mockStatic(CustomNpcsPermissions.class); var network = mockStatic(NpcNetwork.class))
        {
            editor.when(() -> NoppesUtilServer.getEditingNpc(player)).thenReturn(npc);
            request.handle(player);
            verifyNoInteractions(clones);
            permissions.when(() -> CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_CLONE)).thenReturn(true);
            CrewClonePreviewPacket.request(99, 3, "Crewman", token).handle(player);
            verifyNoInteractions(clones);
            when(wand.getItem()).thenReturn(net.minecraft.world.item.Items.STICK);
            request.handle(player);
            verifyNoInteractions(clones);
            when(wand.getItem()).thenReturn(CustomItems.wand);
            CrewClonePreviewPacket.request(42, 0, "Crewman", token).handle(player);
            CrewClonePreviewPacket.request(42, CrewSource.MAX_CLONE_TAB + 1, "Crewman", token).handle(player);
            verifyNoInteractions(clones);
            CrewClonePreviewPacket.request(42, 3, "../secret", token).handle(player);
            verify(clones, never()).getCloneData(any(), anyString(), anyInt());
            new CrewClonePreviewPacket(42, 3, "Crewman", token, false, data).handle(player);
            verify(clones, never()).getCloneData(any(), anyString(), anyInt());
            request.handle(player);
            verify(clones).getCloneData(null, "Crewman", 3);
            network.verify(() -> NpcNetwork.sendTo(player, new CrewClonePreviewPacket(42, 3, "Crewman", token, false, data)));
            verify(clones, never()).spawn(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyString(), any());
        }
        finally
        {
            ServerCloneController.Instance = previous;
        }
    }
}
