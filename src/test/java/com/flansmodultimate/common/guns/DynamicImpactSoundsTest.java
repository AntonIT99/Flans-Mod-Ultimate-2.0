package com.flansmodultimate.common.guns;

import com.flansmodultimate.FlansModSounds;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicImpactSoundsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void explicitFlagsControlMaterialLayerAndMissingFlagKeepsLegacyFallback()
    {
        assertTrue(bullet().usesMaterialImpactSounds());
        assertFalse(bullet("HitSound custom").usesMaterialImpactSounds());
        assertTrue(bullet("HitSound custom", "DynamicHitSoundEnable true").usesMaterialImpactSounds());
        assertFalse(bullet("DynamicHitSoundEnable false").usesMaterialImpactSounds());
        var impact = EquipmentSupport.impact(bullet("HitSoundEnable true", "HitSound custom", "DynamicHitSoundEnable true"));
        assertTrue(impact.materialImpactSounds());
        assertFalse(impact.blockHitSound().isBlank());
    }

    @Test
    void configuredAndMaterialSoundsAreBothSentOnceWhileDisabledHitsSendNothing()
    {
        Level level = mock(Level.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        BulletType type = mock(BulletType.class);
        when(type.isHitSoundEnable()).thenReturn(true);
        when(type.getHitSound()).thenReturn("custom");
        when(type.getHitSoundRange()).thenReturn(24F);
        when(type.usesMaterialImpactSounds()).thenReturn(true);
        BlockPos position = new BlockPos(1, 2, 3);
        try (var sounds = mockStatic(PacketPlaySound.class))
        {
            ShootingHelper.playImpactSound(level, position, Blocks.OAK_PLANKS.defaultBlockState(), type);
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(position.getCenter(), 24D, Level.OVERWORLD, "custom", true, null));
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(position.getCenter(), 24D, Level.OVERWORLD, FlansModSounds.SOUND_IMPACT_WOOD, true, null));
            sounds.verifyNoMoreInteractions();
            sounds.clearInvocations();
            when(type.isHitSoundEnable()).thenReturn(false);
            ShootingHelper.playImpactSound(level, position, Blocks.OAK_PLANKS.defaultBlockState(), type);
            sounds.verifyNoInteractions();
        }
    }

    @Test
    void identicalLayersDoNotDuplicateTheSameSound()
    {
        Level level = mock(Level.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        BulletType type = mock(BulletType.class);
        when(type.isHitSoundEnable()).thenReturn(true);
        when(type.getHitSound()).thenReturn(FlansModSounds.SOUND_IMPACT_WOOD);
        when(type.getHitSoundRange()).thenReturn(24F);
        when(type.usesMaterialImpactSounds()).thenReturn(true);
        try (var sounds = mockStatic(PacketPlaySound.class))
        {
            ShootingHelper.playImpactSound(level, BlockPos.ZERO, Blocks.OAK_PLANKS.defaultBlockState(), type);
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(BlockPos.ZERO.getCenter(), 24D, Level.OVERWORLD, FlansModSounds.SOUND_IMPACT_WOOD, true, null));
            sounds.verifyNoMoreInteractions();
        }
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("impact", EnumType.BULLET, new ContentPack("test", Path.of("build", "test-packs", "dynamic-impact")), List.of(lines)));
        return type;
    }
}
