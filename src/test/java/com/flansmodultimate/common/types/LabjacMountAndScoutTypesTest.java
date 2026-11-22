package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Labjac mount morale, shell camera and scout round keys, through the real parser. */
class LabjacMountAndScoutTypesTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "test"));

    /** Sound keys register sound events, which need the game's registries. */
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void theTyrantsAndPlebeiansElephantCanPanic()
    {
        MechaType type = new MechaType();
        type.read(new TypeFile("testMecha", EnumType.MECHA, PACK, List.of("Driver 0 0 0", "canPanic true", "morale 50", "panicTime 15 ", "panicSound elephantRage",
            "runAmokSound elephantAttack")));
        assertTrue(type.isCanPanic());
        assertEquals(50, type.getMorale());
        assertEquals(15, type.getPanicTime());
        assertTrue(type.getPanicSound().endsWith("elephantrage"));
        assertTrue(type.getRunAmokSound().endsWith("elephantattack"));
    }

    @Test
    void mountsDoNotPanicUnlessAuthored()
    {
        MechaType type = new MechaType();
        type.read(new TypeFile("testMecha", EnumType.MECHA, PACK, List.of("Driver 0 0 0")));
        assertFalse(type.isCanPanic());
    }

    @Test
    void artilleryCanFollowItsShells()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("testVehicle", EnumType.VEHICLE, PACK, List.of("Driver 0 0 0", "hasMagicArtilleryMode true")));
        assertTrue(type.isMagicArtilleryMode());
    }

    @Test
    void scoutRoundsAreRead()
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("testBullet", EnumType.BULLET, PACK, List.of("scoutBullet true")));
        assertTrue(type.isScoutBullet());
    }
}
