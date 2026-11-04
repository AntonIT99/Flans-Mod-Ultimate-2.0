package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolHealingTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void unconfiguredToolsKeepInstantHealing()
    {
        LivingEntity target = target();
        ToolHealing.apply(tool("HealAmount 8"), target);
        verify(target).heal(8F);
        verify(target, never()).addEffect(any());
    }

    @Test
    void healStrengthSelectsRegenerationUsingHealAmountAsTicks()
    {
        LivingEntity target = target();
        ToolHealing.apply(tool("HealAmount 100", "HealStrength 2"), target);
        var effect = ArgumentCaptor.forClass(MobEffectInstance.class);
        verify(target).addEffect(effect.capture());
        assertEquals(MobEffects.REGENERATION, effect.getValue().getEffect());
        assertEquals(100, effect.getValue().getDuration());
        assertEquals(2, effect.getValue().getAmplifier());
        verify(target, never()).heal(anyFloat());
    }

    @Test
    void invalidDurationsCannotBecomeInfiniteRegenerationAndStrengthIsBounded()
    {
        LivingEntity target = target();
        ToolHealing.apply(tool("HealAmount 0", "HealStrength 2"), target);
        ToolHealing.apply(tool("HealAmount -1", "HealStrength 2"), target);
        verify(target, never()).addEffect(any());
        assertEquals(0, tool("HealStrength -1").getHealStrength());
        assertEquals(255, tool("HealStrength 1000").getHealStrength());
    }

    private static LivingEntity target()
    {
        LivingEntity target = mock(LivingEntity.class);
        when(target.level()).thenReturn(mock(ServerLevel.class));
        return target;
    }

    private static ToolType tool(String... lines)
    {
        ToolType type = new ToolType();
        type.load(new TypeFile("tool", EnumType.TOOL, new ContentPack("test", Path.of("build", "test-packs", "heal-strength")), List.of(lines)));
        return type;
    }
}
