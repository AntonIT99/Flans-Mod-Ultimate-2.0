package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.projectiles.GrenadeProperties.Use;
import com.flansmodultimate.common.types.GrenadeType;
import org.junit.jupiter.api.Test;

import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GrenadePropertiesTest
{
    private static GrenadeType timedGrenade()
    {
        GrenadeType type = mock(GrenadeType.class);
        when(type.isCanThrow()).thenReturn(true);
        when(type.getFuse()).thenReturn(60);
        return type;
    }

    @Test
    void smokeParticlesOnAnExplosiveDoNotMakeItASmokeGrenade()
    {
        GrenadeType type = timedGrenade();
        when(type.getSmokeTime()).thenReturn(200);
        assertEquals(Use.SMOKE, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.getExplosiveMass()).thenReturn(0.1F);
        assertEquals(Use.EXPLOSIVE, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.getFireRadius()).thenReturn(3F);
        assertEquals(Use.INCENDIARY, ExternalProjectileSupport.grenadeProperties(type).use());
    }

    @Test
    void flashAndPersistentSmokeEffectsExposeTheirSafetyReach()
    {
        GrenadeType type = timedGrenade();
        when(type.isFlashBang()).thenReturn(true);
        when(type.getFlashRange()).thenReturn(8);
        assertEquals(Use.FLASH, ExternalProjectileSupport.grenadeProperties(type).use());
        assertEquals(8D, ExternalProjectileSupport.grenadeProperties(type).effectRadius());
        when(type.isFlashBang()).thenReturn(false);
        when(type.getSmokeTime()).thenReturn(200);
        when(type.getSmokeRadius()).thenReturn(6F);
        when(type.getSmokeEffects()).thenReturn(List.of(mock(MobEffectInstance.class)));
        assertEquals(Use.GAS, ExternalProjectileSupport.grenadeProperties(type).use());
        assertEquals(6D, ExternalProjectileSupport.grenadeProperties(type).effectRadius());
    }

    @Test
    void devicesRequiringPlacementOrExternalDetonationAreNeverOrdinaryGrenades()
    {
        GrenadeType type = timedGrenade();
        when(type.getExplosiveMass()).thenReturn(1F);
        when(type.getLivingProximityTrigger()).thenReturn(2F);
        assertEquals(Use.PLACEABLE, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.getLivingProximityTrigger()).thenReturn(0F);
        when(type.getDriveableProximityTrigger()).thenReturn(3F);
        assertEquals(Use.PLACEABLE, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.getDriveableProximityTrigger()).thenReturn(0F);
        when(type.isCanThrow()).thenReturn(false);
        assertEquals(Use.PLACEABLE, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.isCanThrow()).thenReturn(true);
        when(type.isRemote()).thenReturn(true);
        assertEquals(Use.REMOTE, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.isDeployableBag()).thenReturn(true);
        assertEquals(Use.UTILITY, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.isDeployableBag()).thenReturn(false);
        when(type.isRemote()).thenReturn(false);
        when(type.getFuse()).thenReturn(0);
        assertEquals(Use.UTILITY, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.isExplodeOnImpact()).thenReturn(true);
        assertEquals(Use.EXPLOSIVE, ExternalProjectileSupport.grenadeProperties(type).use());
        when(type.isStickToThrower()).thenReturn(true);
        assertEquals(Use.UTILITY, ExternalProjectileSupport.grenadeProperties(type).use());
    }
}
