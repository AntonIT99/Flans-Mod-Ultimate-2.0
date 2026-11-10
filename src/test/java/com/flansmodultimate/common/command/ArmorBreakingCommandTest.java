package com.flansmodultimate.common.command;

import com.flansmodultimate.config.EnumBreakableArmor;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import net.minecraft.commands.CommandSourceStack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArmorBreakingCommandTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static CommandSourceStack source(boolean operator)
    {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(2)).thenReturn(operator);
        return source;
    }

    @ParameterizedTest
    @CsvSource({"off,UNBREAKABLE", "on,CONTENT_PACK", "unbreakable,UNBREAKABLE", "content_pack,CONTENT_PACK"})
    void eachWordSetsItsPolicy(String word, EnumBreakableArmor expected) throws CommandSyntaxException
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ArmorBreakingCommand.register(dispatcher);
        try (MockedStatic<ModCommonConfig> config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::breakableArmor).thenReturn(expected);

            assertEquals(1, dispatcher.execute("armorBreaking " + word, source(true)));

            config.verify(() -> ModCommonConfig.setBreakableArmor(expected));
        }
    }

    @ParameterizedTest
    @CsvSource({"off", "all_breakable"})
    void onlyOperatorsMayChangeIt(String word)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ArmorBreakingCommand.register(dispatcher);
        try (MockedStatic<ModCommonConfig> config = mockStatic(ModCommonConfig.class))
        {
            assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("armorBreaking " + word, source(false)));

            config.verify(() -> ModCommonConfig.setBreakableArmor(any()), never());
        }
    }
}
