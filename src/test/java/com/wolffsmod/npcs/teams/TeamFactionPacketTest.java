package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.TeamFactionPacket;
import io.netty.buffer.Unpooled;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamFactionPacketTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void creationAndApplicationRoundTrip()
    {
        for (TeamFactionPacket packet : List.of(new TeamFactionPacket(42, "team", "", false), new TeamFactionPacket(42, "team", "class", true)))
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try
            {
                packet.write(buffer);
                assertEquals(packet, TeamFactionPacket.read(buffer));
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
    }

    @Test
    void serverValidatesEditorPermissionsAndClassMembershipBeforeCreatingOrApplying()
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
        ITeam team = mock(ITeam.class);
        when(team.getName()).thenReturn("Test team");
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(playerClass.getShortName()).thenReturn("class");
        doReturn(List.of(playerClass)).when(team).getClasses();
        TeamFactions factions = mock(TeamFactions.class);
        try (var editor = mockStatic(NoppesUtilServer.class);
            var permissions = mockStatic(CustomNpcsPermissions.class);
            var api = mockStatic(FlansModApi.class);
            var data = mockStatic(TeamFactions.class);
            var relations = mockStatic(FactionRelations.class);
            var ignored = mockStatic(NpcNetwork.class))
        {
            // Creating a faction answers with the faction list, whose display names need the faction mixin, over the network.
            relations.when(FactionRelations::editable).thenReturn(List.of());
            editor.when(() -> NoppesUtilServer.getEditingNpc(player)).thenReturn(npc);
            api.when(() -> FlansModApi.getTeam("team")).thenReturn(Optional.of(team));
            data.when(() -> TeamFactions.get(player.server)).thenReturn(factions);
            TeamFactionPacket create = new TeamFactionPacket(42, "team", "", false);
            TeamFactionPacket apply = new TeamFactionPacket(42, "team", "class", true);
            create.handle(player);
            verifyNoInteractions(factions);
            permissions.when(() -> CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_FACTION)).thenReturn(true);
            apply.handle(player);
            verifyNoInteractions(factions);
            permissions.when(() -> CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_ADVANCED)).thenReturn(true);
            new TeamFactionPacket(99, "team", "class", true).handle(player);
            new TeamFactionPacket(42, "team", "foreignClass", true).handle(player);
            new TeamFactionPacket(42, "missingTeam", "", false).handle(player);
            verifyNoInteractions(factions);
            create.handle(player);
            verify(factions).getOrCreate(team);
            verify(factions, never()).apply(any(), any());
            apply.handle(player);
            verify(factions).apply(npc, team);
            when(wand.getItem()).thenReturn(Items.STICK);
            apply.handle(player);
            verifyNoMoreInteractions(factions);
        }
    }
}
