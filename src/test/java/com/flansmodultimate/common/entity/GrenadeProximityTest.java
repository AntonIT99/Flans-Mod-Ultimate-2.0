package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.projectiles.IProximityTarget;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.util.ModUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class GrenadeProximityTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void vehicleMinesRecognizeLivingMachinesButLeaveInfantryAndDisabledTriggersAlone()
    {
        Shootable mine = mock(Shootable.class, CALLS_REAL_METHODS);
        GrenadeType type = mock(GrenadeType.class);
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity target = mock(LivingEntity.class, withSettings().extraInterfaces(IProximityTarget.class));
        doReturn(type).when(mine).getConfigType();
        doReturn(Optional.empty()).when(mine).getOwner();
        doReturn(new AABB(0D, 0D, 0D, 1D, 1D, 1D)).when(mine).getBoundingBox();
        doReturn(true).when(mine).handleEntityInProximityTriggerRange(level, target);
        when(type.getDriveableProximityTrigger()).thenReturn(3F);
        when(type.getLivingProximityTrigger()).thenReturn(-1F);
        when(target.distanceToSqr(mine)).thenReturn(0.5D);
        mine.tickCount = 100;
        try (var entities = mockStatic(ModUtils.class))
        {
            entities.when(() -> ModUtils.queryEntities(eq(level), eq(mine), any())).thenReturn(List.of(target));
            mine.handleDetonationConditions(level);
            verify(mine, never()).detonate(level);
            when(((IProximityTarget) target).flansmodultimateDriveableProximityTarget()).thenReturn(true);
            mine.handleDetonationConditions(level);
            verify(mine).detonate(level);
            clearInvocations(mine);
            when(type.getLivingProximityTrigger()).thenReturn(3F);
            when(type.getDriveableProximityTrigger()).thenReturn(-1F);
            mine.handleDetonationConditions(level);
            verify(mine, never()).detonate(level);
        }
    }

    @Test
    void anUnarmedMineDoesNotApplyTriggerDamageBeforeItsArmingDelay()
    {
        Shootable mine = mock(Shootable.class, CALLS_REAL_METHODS);
        GrenadeType type = mock(GrenadeType.class);
        ServerLevel level = mock(ServerLevel.class);
        doReturn(type).when(mine).getConfigType();
        when(type.getPrimeDelay()).thenReturn(60);
        mine.tickCount = 20;
        mine.handleDetonationConditions(level);
        verify(mine, never()).handleEntityInProximityTriggerRange(any(), any());
        verify(mine, never()).detonate(any());
    }
}
