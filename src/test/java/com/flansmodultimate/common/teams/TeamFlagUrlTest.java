package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class TeamFlagUrlTest
{
    @Test
    void tellsWebAddressesFromTextureLocations()
    {
        assertTrue(TeamFlagUrl.isUrl("https://example.com/red.png"));
        assertTrue(TeamFlagUrl.isUrl("  HTTP://example.com/red.png"));
        assertFalse(TeamFlagUrl.isUrl("flansmod:textures/skins/red.png"));
        assertFalse(TeamFlagUrl.isUrl("ftp://example.com/red.png"));
        assertFalse(TeamFlagUrl.isUrl(null));
    }

    @Test
    void parsesOnlyPlainWebAddressesWithAHost()
    {
        URI parsed = TeamFlagUrl.parse(" https://example.com/red.png?v=2 ");
        assertNotNull(parsed);
        assertEquals("example.com", parsed.getHost());
        assertNull(TeamFlagUrl.parse("https:///red.png"));
        assertNull(TeamFlagUrl.parse("https://user@example.com/red.png"));
        assertNull(TeamFlagUrl.parse("https://example.com/red flag.png"));
        assertNull(TeamFlagUrl.parse("file:///C:/red.png"));
        assertNull(TeamFlagUrl.parse("https://example.com/" + "a".repeat(TeamFlagUrl.MAX_LENGTH)));
    }
}
