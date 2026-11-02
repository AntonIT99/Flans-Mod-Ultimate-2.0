package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveableDamageEffectsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void retainedNpcWreckAgesWithoutReplayingAndCancelsOnModelRemovalOrRespawn()
    {
        Minecraft minecraft = mock(Minecraft.class);
        ClientLevel level = mock(ClientLevel.class);
        minecraft.level = level;
        LivingEntity npc = mock(LivingEntity.class);
        when(npc.level()).thenReturn(level);
        PartBody body = mock(PartBody.class);
        DriveableType type = mock(DriveableType.class);
        when(body.getType()).thenReturn(type);
        try (var clients = mockStatic(Minecraft.class);
            var bodies = mockStatic(PartBodies.class);
            var deaths = mockStatic(DestructionSequences.class);
            var emitters = mockStatic(DamageEffectEmitter.class);
            var ambient = mockStatic(VehicleAmbientEffects.class);
            var pieces = mockStatic(DetachedPieces.class))
        {
            clients.when(Minecraft::getInstance).thenReturn(minecraft);
            bodies.when(() -> PartBodies.wearers(level)).thenReturn(List.of(npc));
            bodies.when(() -> PartBodies.of(npc)).thenReturn(body);
            DriveableDamageEffects.clear();
            DriveableDamageEffects.tickClient();
            when(npc.isDeadOrDying()).thenReturn(true);
            bodies.when(() -> PartBodies.wearers(level)).thenReturn(List.of());
            when(level.getGameTime()).thenReturn(10L);
            DriveableDamageEffects.tickClient();
            when(level.getGameTime()).thenReturn(1410L);
            DriveableDamageEffects.tickClient();
            deaths.verify(() -> DestructionSequences.start(any(PartBodyEffectSource.class)), times(1));
            ArgumentCaptor<DamageEffectSource> emissions = ArgumentCaptor.forClass(DamageEffectSource.class);
            emitters.verify(() -> DamageEffectEmitter.tick(emissions.capture()), times(3));
            assertEquals(-1, emissions.getAllValues().get(0).wreckAge());
            assertEquals(0, emissions.getAllValues().get(1).wreckAge());
            assertEquals(PartBodyEffectSource.MAX_WRECK_EFFECT_TICKS, emissions.getValue().wreckAge());
            when(npc.isDeadOrDying()).thenReturn(false);
            bodies.when(() -> PartBodies.wearers(level)).thenReturn(List.of(npc));
            DriveableDamageEffects.tickClient();
            deaths.verify(() -> DestructionSequences.cancel(npc));
            bodies.when(() -> PartBodies.of(npc)).thenReturn(null);
            bodies.when(() -> PartBodies.wearers(level)).thenReturn(List.of());
            DriveableDamageEffects.tickClient();
            deaths.verify(() -> DestructionSequences.cancel(npc), times(2));
            DriveableDamageEffects.clear();
        }
    }
}
