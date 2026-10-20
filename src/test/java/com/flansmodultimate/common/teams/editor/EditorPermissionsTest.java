package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import net.minecraftforge.server.permission.PermissionAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class EditorPermissionsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void rejectsOrdinaryGuestsAndAcceptsExplicitGrantsOrOperators()
    {
        ServerPlayer guest = mock(ServerPlayer.class);
        try (MockedStatic<PermissionAPI> permissions = mockStatic(PermissionAPI.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(guest, EditorPermissions.EDITOR)).thenReturn(false);
            assertFalse(EditorPermissions.mayEdit(guest));
            permissions.when(() -> PermissionAPI.getPermission(guest, EditorPermissions.EDITOR)).thenReturn(true);
            assertTrue(EditorPermissions.mayEdit(guest));
            permissions.when(() -> PermissionAPI.getPermission(guest, EditorPermissions.EDITOR)).thenReturn(false);
            when(guest.hasPermissions(2)).thenReturn(true);
            assertTrue(EditorPermissions.mayEdit(guest));
        }
    }

    @Test
    void lanHostCanEditButLanGuestStillNeedsAGrant()
    {
        ServerPlayer host = mock(ServerPlayer.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(host.getServer()).thenReturn(server);
        when(server.isSingleplayerOwner(host.getGameProfile())).thenReturn(true);
        assertTrue(EditorPermissions.mayEdit(host));
        CommandSourceStack console = mock(CommandSourceStack.class);
        when(console.hasPermission(2)).thenReturn(true);
        assertTrue(EditorPermissions.mayEdit(console));
    }
}
