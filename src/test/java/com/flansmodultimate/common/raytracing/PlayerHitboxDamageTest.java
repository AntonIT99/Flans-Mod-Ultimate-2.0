package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerHitboxDamageTest
{
    @Test
    void disablingHitZoneModifiersUsesUnitDamageEvenWithoutARound()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            for (EnumHitboxType type : EnumHitboxType.values())
                assertEquals(1F, PlayerHitbox.bodyPartDamageModifier(type));
            verify(manager, never()).isRoundRunning();
        }
    }

    @Test
    void enabledRetainsConfiguredMultipliersEvenWithoutARound()
    {
        try (var managers = mockStatic(TeamsManager.class); var configs = mockStatic(ModCommonConfig.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isBodyPartDamageModifiers()).thenReturn(true);
            CommonConfigSnapshot config = mock(CommonConfigSnapshot.class);
            configs.when(ModCommonConfig::get).thenReturn(config);
            when(config.headshotDamageModifier()).thenReturn(2F);
            when(config.chestshotDamageModifier()).thenReturn(1.1F);
            when(config.armshotDamageModifier()).thenReturn(0.75F);
            when(config.legshotModifier()).thenReturn(0.5F);
            assertEquals(ModCommonConfig.get().headshotDamageModifier(), PlayerHitbox.bodyPartDamageModifier(EnumHitboxType.HEAD));
            assertEquals(ModCommonConfig.get().chestshotDamageModifier(), PlayerHitbox.bodyPartDamageModifier(EnumHitboxType.BODY));
            assertEquals(ModCommonConfig.get().armshotDamageModifier(), PlayerHitbox.bodyPartDamageModifier(EnumHitboxType.LEFTARM));
            assertEquals(ModCommonConfig.get().armshotDamageModifier(), PlayerHitbox.bodyPartDamageModifier(EnumHitboxType.RIGHTARM));
            assertEquals(ModCommonConfig.get().legshotModifier(), PlayerHitbox.bodyPartDamageModifier(EnumHitboxType.LEGS));
            verify(manager, never()).isRoundRunning();
        }
    }
}
