package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassLimitsRuleTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void disablingCapsAlsoMakesZeroCapacityClassesUnlimitedAndSurvivesSaving()
    {
        TeamsManager manager = new TeamsManager();
        PlayerClass playerClass = mock(PlayerClass.class);
        Team team = mock(Team.class);
        when(playerClass.getClassLimit()).thenReturn(2);
        when(playerClass.getClassLimitPercent()).thenReturn(-1F);
        assertEquals(2, manager.getClassCapacity(team, playerClass));
        RoundRules rules = manager.getRoundRuleRuntime().rules();
        rules.set(RoundRule.DISABLE_CLASS_LIMITS, "true");
        assertEquals(ClassLimits.UNLIMITED, manager.getClassCapacity(team, playerClass));
        when(playerClass.getClassLimit()).thenReturn(0);
        when(playerClass.getClassLimitPercent()).thenReturn(0F);
        assertEquals(ClassLimits.UNLIMITED, manager.getClassCapacity(team, playerClass));
        assertTrue(RoundRules.load(rules.save()).enabled(RoundRule.DISABLE_CLASS_LIMITS));
        rules.set(RoundRule.DISABLE_CLASS_LIMITS, "false");
        assertEquals(0, manager.getClassCapacity(team, playerClass));
    }
}
