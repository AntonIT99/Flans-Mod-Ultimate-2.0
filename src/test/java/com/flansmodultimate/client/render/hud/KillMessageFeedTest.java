package com.flansmodultimate.client.render.hud;

import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.ChatFormatting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class KillMessageFeedTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @AfterEach
    void clear()
    {
        KillMessageFeed.clear();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 6, 20})
    void keepsOnlyTheNewestLinesImmediately(int limit)
    {
        try (var config = mockStatic(ModClientConfig.class))
        {
            config.when(ModClientConfig::killFeedLines).thenReturn(limit);
            for (int i = 0; i < limit + 2; i++)
                add(i);

            var messages = KillMessageFeed.getMessages();
            assertEquals(limit, messages.size());
            for (int i = 0; i < limit; i++)
            {
                assertEquals("killer" + (i + 2), messages.get(i).getKiller().getString());
                assertEquals(limit - i - 1, messages.get(i).getLine());
            }
        }
    }

    @Test
    void loweringTheLimitTrimsExistingMessagesWithoutAnotherKill()
    {
        try (var config = mockStatic(ModClientConfig.class))
        {
            config.when(ModClientConfig::killFeedLines).thenReturn(6);
            for (int i = 0; i < 6; i++)
                add(i);
            config.when(ModClientConfig::killFeedLines).thenReturn(3);

            assertEquals(3, KillMessageFeed.getMessages().size());
            assertEquals("killer3", KillMessageFeed.getMessages().get(0).getKiller().getString());
            config.when(ModClientConfig::killFeedLines).thenReturn(6);
            assertEquals(3, KillMessageFeed.getMessages().size());
        }
    }

    private static void add(int index)
    {
        KillMessageFeed.add(new KillMessageData(false, "missing_test_weapon", "killer" + index, ChatFormatting.WHITE, "victim", ChatFormatting.WHITE));
    }
}
