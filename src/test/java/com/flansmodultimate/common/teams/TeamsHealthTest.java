package com.flansmodultimate.common.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsHealthTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void overridesAndResetPreserveWoundsAndNativeAttributes()
    {
        AttributeInstance health = attribute(20D);
        health.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath("othermod", "health"), 0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        LivingEntity entity = entity(health, 15F);
        TeamsHealth.apply(entity, 80);
        assertEquals(80F, entity.getMaxHealth(), 0.001F);
        assertEquals(40F, entity.getHealth(), 0.001F);
        assertEquals(20D, health.getBaseValue());
        TeamsHealth.apply(entity, 40);
        assertEquals(20F, entity.getHealth(), 0.001F);
        TeamsHealth.apply(entity, 0);
        assertEquals(30F, entity.getMaxHealth(), 0.001F);
        assertEquals(15F, entity.getHealth(), 0.001F);
        assertEquals(1, health.getModifiers().size());
    }

    @Test
    void keepsOverrideAcrossAttributeChangesAndSaveLoad()
    {
        AttributeInstance health = attribute(20D);
        LivingEntity entity = entity(health, 10F);
        TeamsHealth.apply(entity, 60);
        CompoundTag saved = health.save();
        AttributeInstance restored = attribute(20D);
        restored.load(saved);
        LivingEntity loaded = entity(restored, 30F);
        TeamsHealth.apply(loaded, 60);
        assertEquals(30F, loaded.getHealth());
        restored.setBaseValue(40D);
        TeamsHealth.apply(loaded, 60);
        assertEquals(60F, loaded.getMaxHealth(), 0.001F);
        assertEquals(15F, loaded.getHealth(), 0.001F);
        TeamsHealth.apply(loaded, 0);
        assertEquals(40F, loaded.getMaxHealth());
        assertEquals(10F, loaded.getHealth(), 0.001F);
    }

    @Test
    void zeroLeavesNativeHealthAloneAndDeadEntitiesStayDead()
    {
        AttributeInstance health = attribute(20D);
        LivingEntity entity = entity(health, 9F);
        TeamsHealth.apply(entity, 0);
        assertEquals(9F, entity.getHealth());
        assertTrue(health.getModifiers().isEmpty());
        LivingEntity dead = entity(health, 0F);
        TeamsHealth.apply(dead, 100);
        assertEquals(0F, dead.getHealth());
        verify(dead, never()).setHealth(anyFloat());
    }

    private static AttributeInstance attribute(double base)
    {
        AttributeInstance health = new AttributeInstance(Attributes.MAX_HEALTH, ignored ->
        {
        });
        health.setBaseValue(base);
        return health;
    }

    private static LivingEntity entity(AttributeInstance health, float current)
    {
        LivingEntity entity = mock(LivingEntity.class);
        AtomicReference<Float> hp = new AtomicReference<>(current);
        ServerLevel level = mock(ServerLevel.class);
        when(entity.level()).thenReturn(level);
        when(entity.getAttribute(Attributes.MAX_HEALTH)).thenReturn(health);
        when(entity.getMaxHealth()).thenAnswer(ignored -> (float) health.getValue());
        when(entity.getHealth()).thenAnswer(ignored -> hp.get());
        when(entity.isAlive()).thenAnswer(ignored -> hp.get() > 0F);
        doAnswer(call ->
        {
            hp.set(call.getArgument(0));
            return null;
        }).when(entity).setHealth(anyFloat());
        return entity;
    }
}
