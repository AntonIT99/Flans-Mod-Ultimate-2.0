package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundEraWindowTest
{
    @Test
    void ruleStoresTheCanonicalWindow()
    {
        assertEquals(RoundRule.Kind.ERA, RoundRule.ERA_WINDOW.getKind());
        assertEquals("", RoundRule.ERA_WINDOW.getDefaultValue());
        assertEquals("", RoundRule.ERA_WINDOW.validate("  "));
        assertEquals("between ww2 1946 match=introduction", RoundRule.ERA_WINDOW.validate(" BETWEEN WW2 1946 match=introduction undated=allow "));
    }

    @Test
    void ruleRejectsUnknownErasReversedBoundsAndDespawning()
    {
        assertThrows(IllegalArgumentException.class, () -> RoundRule.ERA_WINDOW.validate("equal Atlantis"));
        assertThrows(IllegalArgumentException.class, () -> RoundRule.ERA_WINDOW.validate("between 1946 1939"));
        assertThrows(IllegalArgumentException.class, () -> RoundRule.ERA_WINDOW.validate("equal WW2 despawn=on"));
        assertThrows(IllegalArgumentException.class, () -> RoundRule.ERA_WINDOW.validate("sometimes"));
    }

    @Test
    void savedRulesKeepTheWindowAndRoundsInheritIt()
    {
        RoundRules defaults = new RoundRules();
        defaults.set(RoundRule.ERA_WINDOW, "equal WW2");
        RoundRules round = new RoundRules();
        assertEquals("equal ww2", round.resolve(defaults).get(RoundRule.ERA_WINDOW));
        round.set(RoundRule.ERA_WINDOW, "until 1918");
        assertEquals("until 1918", RoundRules.load(round.save()).resolve(defaults).get(RoundRule.ERA_WINDOW));
    }

    @Test
    void parsedWindowsAreReusedAndUnreadableOnesIgnored()
    {
        assertNull(TeamsEraScope.parse(""));
        assertNotNull(TeamsEraScope.parse("equal WW2"));
        assertSame(TeamsEraScope.parse("equal WW2"), TeamsEraScope.parse("equal WW2"));
        assertNull(TeamsEraScope.parse("sometimes"));
    }
}
