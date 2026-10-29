package com.wolffsmod.npcs.combat;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class NpcProjectileDamageTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void removesVanillaScalingWhileKeepingProjectileTypeAndShooter()
    {
        Entity projectile = mock(Entity.class);
        LivingEntity shooter = mock(LivingEntity.class);
        Holder<DamageType> type = Holder.direct(new DamageType("thrown", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
        DamageSource vanilla = new DamageSource(type, projectile, shooter);
        DamageSource npc = new NpcProjectileDamage(vanilla);

        assertTrue(vanilla.scalesWithDifficulty());
        assertFalse(npc.scalesWithDifficulty());
        assertSame(type, npc.typeHolder());
        assertSame(projectile, npc.getDirectEntity());
        assertSame(shooter, npc.getEntity());
        assertEquals(vanilla.getMsgId(), npc.getMsgId());
        assertEquals(vanilla.getFoodExhaustion(), npc.getFoodExhaustion());
    }
}
