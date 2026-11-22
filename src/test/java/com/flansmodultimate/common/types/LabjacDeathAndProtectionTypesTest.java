package com.flansmodultimate.common.types;

import com.flansmodultimate.common.driveables.damage.DeathBlast;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Labjac wreck, death-blast, soft-kill protection and TV-guidance keys, through the real parser. */
class LabjacDeathAndProtectionTypesTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "test"));

    /** Sound keys register sound events, which need the game's registries. */
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void theTyrantsAndPlebeiansWreckKeysShortenTheServersLifetime()
    {
        VehicleType type = vehicle("LeaveWreck true", "WreckLifetime 2000", "WreckSmoke true");
        assertFalse(type.isNoWreck());
        assertTrue(type.isWreckSmoke());
        assertEquals(2000, type.wreckLifetimeTicks(6000));
        assertEquals(1200, type.wreckLifetimeTicks(1200), "the server's lifetime stays the ceiling");
        assertEquals(0, type.wreckLifetimeTicks(0), "the server can still turn wrecks off");
    }

    @Test
    void anUnauthoredLifetimeKeepsTheServers()
    {
        assertEquals(6000, vehicle().wreckLifetimeTicks(6000));
        assertEquals(6000, DriveableType.resolveWreckLifetime(6000, 0));
        assertFalse(vehicle("WreckSmoke false").isWreckSmoke());
    }

    @Test
    void deathBlastFlagsAreReadTogether()
    {
        assertEquals(EnumSet.of(DeathBlast.TANK, DeathBlast.SHIP), vehicle("Death True", "bigDeath True").getDeathBlasts());
        assertEquals(EnumSet.of(DeathBlast.NUCLEAR), vehicle("nuclearDeath True").getDeathBlasts());
        assertTrue(vehicle().getDeathBlasts().isEmpty());
    }

    @Test
    void passiveProtectionIsReadOnVehicles()
    {
        VehicleType shtora = vehicle("HasPassiveAPS true", "PassiveAPSCanForceAim true");
        assertTrue(shtora.isHasPassiveAPS());
        assertTrue(shtora.isPassiveAPSCanForceAim());
        assertFalse(vehicle().isHasPassiveAPS());
    }

    @Test
    void missilesCanBypassProtectionAndBeTvGuided()
    {
        assertTrue(bullet("BypassPassiveAPS true").isBypassPassiveAPS());
        assertTrue(bullet("PassiveAPSECCM true").isBypassPassiveAPS());
        assertTrue(bullet("TVguided true").isTvGuided());
        BulletType plain = bullet();
        assertFalse(plain.isBypassPassiveAPS());
        assertFalse(plain.isTvGuided());
    }

    private static VehicleType vehicle(String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        VehicleType type = new VehicleType();
        type.read(new TypeFile("testVehicle", EnumType.VEHICLE, PACK, definition));
        return type;
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("testBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
