package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.api.commandcentre.ISettingsPresets;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.mojang.brigadier.CommandDispatcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsPresetsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void importsOnlySaveAfterValidationAndNeverApply() throws Exception
    {
        ISettingsPresets library = mock(ISettingsPresets.class);
        CompoundTag settings = new CompoundTag();
        settings.putInt("count", 12);
        String json = PresetFiles.encode(SettingsPresets.TEAMS, "example", settings);
        SettingsPresets.execute(SettingsPresets.TEAMS, library, "import", "example", "", json, false);
        var order = inOrder(library);
        order.verify(library).validate(settings);
        order.verify(library).save("example", settings, false);
        verify(library, never()).apply(any(), anyString());
        reset(library);
        doThrow(new IllegalArgumentException("invalid")).when(library).validate(any());
        assertThrows(IllegalArgumentException.class, () -> SettingsPresets.execute(SettingsPresets.TEAMS, library, "import", "example", "", json, true));
        verify(library, never()).save(anyString(), any(), anyBoolean());
    }

    @Test
    void networkAndConsoleRequestsRequireOperatorPermission() throws Exception
    {
        ISettingsPresets library = mock(ISettingsPresets.class);
        ResourceLocation kind = ResourceLocation.fromNamespaceAndPath("test", "presets");
        SettingsPresets.register(kind, library);
        ServerPlayer player = mock(ServerPlayer.class);
        SettingsPresets.handle(player, 1, new SettingsPresets.Request(kind, "remove", "example", "", "", true));
        verifyNoInteractions(library);
        CommandDispatcher<CommandSourceStack> commands = new CommandDispatcher<>();
        commands.register(Commands.literal("teams").then(SettingsPresets.commands()));
        CommandSourceStack source = mock(CommandSourceStack.class);
        assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class, () -> commands.execute("teams preset test:presets list", source));
        verifyNoInteractions(library);
        when(source.hasPermission(2)).thenReturn(true);
        when(library.names()).thenReturn(List.of("example"));
        assertEquals(1, commands.execute("teams preset test:presets list", source));
        verify(source).sendSuccess(any(), eq(false));
    }
}
