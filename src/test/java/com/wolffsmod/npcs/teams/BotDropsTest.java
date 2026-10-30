package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

class BotDropsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void onlyBotsDropWhatTheTeamsDropRulesLetAPlayerDrop()
    {
        EntityNPCInterface bot = mock(EntityNPCInterface.class);
        EntityNPCInterface other = mock(EntityNPCInterface.class);
        Level level = mock(Level.class);
        when(bot.level()).thenReturn(level);
        when(other.level()).thenReturn(level);
        List<ItemEntity> drops = new ArrayList<>();
        try (var tags = mockStatic(BotTag.class); var teams = mockStatic(FlansTeams.class))
        {
            tags.when(() -> BotTag.isBot(bot)).thenReturn(true);
            TeamsBots.entityDrops(bot, drops);
            TeamsBots.entityDrops(other, drops);
            teams.verify(() -> FlansTeams.applyDeathDropRules(drops), times(1));
        }
    }
}
