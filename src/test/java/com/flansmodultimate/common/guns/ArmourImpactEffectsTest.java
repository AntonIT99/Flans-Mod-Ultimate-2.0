package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.driveables.armor.ResolvedArmorHit;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketArmourImpact;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArmourImpactEffectsTest
{
    @Test
    void rifleRoundsStaySubtleAndLargeShellsStayBounded()
    {
        assertTrue(ArmourImpactEffects.scale(9F) < ArmourImpactEffects.scale(1000F));
        assertTrue(ArmourImpactEffects.scale(1000F) < ArmourImpactEffects.scale(10000F));
        assertEquals(ArmourImpactEffects.scale(100000F), ArmourImpactEffects.scale(Float.MAX_VALUE));
        for (float invalid : new float[]{0F, -1F, Float.NaN, Float.POSITIVE_INFINITY})
            assertEquals(0.2F, ArmourImpactEffects.scale(invalid));
    }

    @Test
    void sparksReflectOutwardAndKeepTheirGlancingComponent()
    {
        Vec3 normal = new Vec3(0D, 1D, 0D);
        Vec3 headOn = ArmourImpactEffects.reflectedDirection(new Vec3(0D, -1D, 0D), normal);
        assertEquals(0D, headOn.distanceTo(normal), 1.0E-9D);
        Vec3 glancing = ArmourImpactEffects.reflectedDirection(new Vec3(1D, -0.1D, 0D), normal);
        assertTrue(glancing.x > 0.9D);
        assertTrue(glancing.dot(normal) > 0D);
        assertEquals(1D, glancing.length(), 1.0E-9D);
    }

    @Test
    void onlyConfirmedArmouredContactsSendFeedback() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        Level level = mock(Level.class);
        Vec3 incoming = new Vec3(0D, 0D, 1D);
        Vec3 normal = new Vec3(0D, 0D, -1D);
        ResolvedArmorHit plate = new ResolvedArmorHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, new ArmorPlate(50F, 0F), normal, 0F, 50F);
        ResolvedArmorHit bare = new ResolvedArmorHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, ArmorPlate.UNARMOURED, normal, 0F, 0F);
        try (var packets = mockStatic(PacketHandler.class); var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::entityHitParticleRange).thenReturn(64D);
            ArmourImpactEffects.send(level, Vec3.ZERO, incoming, normal, 1000F, bare, false);
            ArmourImpactEffects.send(level, Vec3.ZERO, Vec3.ZERO, normal, 1000F, plate, true);
            packets.verifyNoInteractions();
            ArmourImpactEffects.send(level, Vec3.ZERO, incoming, normal, 1000F, plate, true);
            ArmourImpactEffects.send(level, Vec3.ZERO, incoming, normal, 1000F, plate, false);
            packets.verify(() -> PacketHandler.sendToAllAround(any(PacketArmourImpact.class), eq(Vec3.ZERO), eq(64D), any()), times(2));
            packets.verifyNoMoreInteractions();
        }
    }
}
