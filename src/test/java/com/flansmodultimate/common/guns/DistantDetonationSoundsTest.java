package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.explosions.FlanExplosion;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DistantDetonationSoundsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void sharedParserSupportsBothAmmunitionAndGrenadesWithOptionalDefaults()
    {
        for (ShootableType type : List.of(new BulletType(), new GrenadeType()))
        {
            assertTrue(type.getDistantDetonateSound().isBlank());
            assertEquals(900F, type.getDistantDetonateSoundRange());
            type.load(file("DistantDetonateSound distant_blast", "DistantDetonateSoundRange 700"));
            assertTrue(type.getDistantDetonateSound().endsWith("distant_blast"));
            assertEquals(700F, type.getDistantDetonateSoundRange());
            type.load(file("DistantDetonateSoundRange NaN"));
            assertEquals(900F, type.getDistantDetonateSoundRange());
        }
    }

    @Test
    void distantReportUsesADonutOutsideTheNearExplosionReach()
    {
        Level level = mock(Level.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        ShootableType type = mock(ShootableType.class);
        when(type.getDistantDetonateSound()).thenReturn("distant_blast");
        when(type.getDistantDetonateSoundRange()).thenReturn(900F);
        Vec3 position = new Vec3(10, 64, 20);
        try (var explosion = mockStatic(FlanExplosion.class); var packets = mockStatic(PacketHandler.class))
        {
            explosion.when(() -> FlanExplosion.soundRange(null)).thenReturn(50F);
            DistantDetonationSounds.send(level, type, position, null);
            packets.verify(() -> PacketHandler.sendToDonut(eq(Level.OVERWORLD), eq(position), eq(50D), eq(900D), any(PacketPlaySound.class)));
            packets.verifyNoMoreInteractions();
        }
    }

    @Test
    void absentSoundOrAnOuterRangeInsideTheNearRangeSendsNothing()
    {
        Level level = mock(Level.class);
        ShootableType type = mock(ShootableType.class);
        try (var explosion = mockStatic(FlanExplosion.class); var packets = mockStatic(PacketHandler.class))
        {
            DistantDetonationSounds.send(level, type, Vec3.ZERO, null);
            when(type.getDistantDetonateSound()).thenReturn("distant_blast");
            when(type.getDistantDetonateSoundRange()).thenReturn(50F);
            explosion.when(() -> FlanExplosion.soundRange(null)).thenReturn(50F);
            DistantDetonationSounds.send(level, type, Vec3.ZERO, null);
            when(type.getDistantDetonateSoundRange()).thenReturn(0F);
            DistantDetonationSounds.send(level, type, Vec3.ZERO, null);
            packets.verifyNoInteractions();
        }
    }

    private static TypeFile file(String... lines)
    {
        return new TypeFile("distant", EnumType.BULLET, new ContentPack("test", Path.of("build", "test-packs", "distant")), List.of(lines));
    }
}
