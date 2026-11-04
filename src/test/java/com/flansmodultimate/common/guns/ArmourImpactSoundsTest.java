package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.guns.penetration.PenetrationCalculator;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.List;

import static com.flansmodultimate.common.guns.ArmourImpactSounds.Outcome.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArmourImpactSoundsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void soundBandsRespectTheCurrentSolverAtBoundaryValues()
    {
        assertEquals(RICOCHET, ArmourImpactSounds.outcome(PenetrationCalculator.resolve(90F, 100F)));
        assertEquals(MINOR, ArmourImpactSounds.outcome(PenetrationCalculator.resolve(91F, 100F)));
        assertEquals(PENETRATION, ArmourImpactSounds.outcome(PenetrationCalculator.resolve(100F, 100F)));
        assertEquals(PENETRATION, ArmourImpactSounds.outcome(PenetrationCalculator.resolve(300F, 100F)));
        assertEquals(OVERPENETRATION, ArmourImpactSounds.outcome(PenetrationCalculator.resolve(301F, 100F)));
    }

    @Test
    void configuredSoundsUseTheImpactLocationDimensionAndOutcomeRange()
    {
        Level level = mock(Level.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        BulletType type = bullet("ricochetSound bounce", "minorPenSound minor", "penetrateSound full", "overPenSound over", "bounceSoundRange 29");
        Vec3 impact = new Vec3(1, 2, 3);
        try (var sounds = mockStatic(PacketPlaySound.class))
        {
            ArmourImpactSounds.send(level, impact, type, PenetrationCalculator.resolve(50F, 100F));
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(impact, 29D, Level.OVERWORLD, type.getRicochetSound(), true, null));
            ArmourImpactSounds.send(level, impact, type, PenetrationCalculator.resolve(95F, 100F));
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(impact, 35D, Level.OVERWORLD, type.getMinorPenSound(), true, null));
            ArmourImpactSounds.send(level, impact, type, PenetrationCalculator.resolve(200F, 100F));
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(impact, 50D, Level.OVERWORLD, type.getPenetrateSound(), true, null));
            ArmourImpactSounds.send(level, impact, type, PenetrationCalculator.resolve(400F, 100F));
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(impact, 50D, Level.OVERWORLD, type.getOverPenSound(), true, null));
            sounds.verifyNoMoreInteractions();
        }
    }

    @Test
    void unauthoredSoundsAndUnarmouredHitsRemainSilent()
    {
        Level level = mock(Level.class);
        try (var sounds = mockStatic(PacketPlaySound.class))
        {
            ArmourImpactSounds.send(level, Vec3.ZERO, bullet(), PenetrationCalculator.resolve(50F, 100F));
            ArmourImpactSounds.send(level, Vec3.ZERO, bullet("overPenSound over"), PenetrationCalculator.resolve(400F, 0F));
            sounds.verifyNoInteractions();
        }
        assertEquals(25F, bullet("bounceSoundRange NaN").getBounceSoundRange());
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("sounds", EnumType.BULLET, new ContentPack("test", Path.of("build", "test-packs", "impact-sounds")), List.of(lines)));
        return type;
    }
}
