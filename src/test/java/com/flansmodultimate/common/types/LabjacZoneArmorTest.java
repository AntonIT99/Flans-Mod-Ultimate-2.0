package com.flansmodultimate.common.types;

import com.flansmodultimate.common.raytracing.EnumHitboxType;
import com.flansmodultimate.common.raytracing.ZoneArmor;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Labjac per-zone body armour, round penetration against it, and the driveable keys ported alongside. */
class LabjacZoneArmorTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "labjac-zone-armor"));

    @Test
    void armourReadsEveryZoneAndItsAliases()
    {
        ArmorType plate = armor("Type Boots", "bodyArmor 69", "backArmor 60", "armArmor 62", "legArmor 55", "neckArmor 40", "maskArmor 12", "helmetArmor 120");
        assertTrue(plate.isHasZoneArmor());
        assertEquals(69F, plate.getZoneArmor(ArmorZone.BODY));
        assertEquals(60F, plate.getZoneArmor(ArmorZone.BACK));
        assertEquals(62F, plate.getZoneArmor(ArmorZone.ARM));
        assertEquals(55F, plate.getZoneArmor(ArmorZone.LEG));
        assertEquals(40F, plate.getZoneArmor(ArmorZone.NAPE));
        assertEquals(12F, plate.getZoneArmor(ArmorZone.FACE));
        assertEquals(120F, plate.getZoneArmor(ArmorZone.HEAD));
    }

    @Test
    void armourWithoutZoneRatingsCoversNothing()
    {
        ArmorType plain = armor("Type Helmet", "bodyArmor -5", "headArmor NaN");
        assertFalse(plain.isHasZoneArmor());
        for (ArmorZone zone : ArmorZone.values())
            assertEquals(0F, plain.getZoneArmor(zone));
    }

    @Test
    void roundsReadPenetrationAliasesAndPenalties()
    {
        BulletType defaults = bullet();
        assertEquals(BulletType.DEFAULT_BODY_ARMOR_PENETRATION, defaults.getBodyArmorPenetration(0F));
        assertEquals(0F, defaults.getNonPenPenalty());
        assertEquals(0.01F, defaults.getBarelyPenPenalty());

        BulletType galil = bullet("bodyArmorPen 280", "dynamicBodyArmorPen 175", "dynamicBulletDelay 5", "nonPenPenalty 0.55", "barelyPenPenalty 0.8");
        assertEquals(280F, galil.getBodyArmorPenetration(5F));
        assertEquals(175F, galil.getBodyArmorPenetration(6F));
        assertEquals(0.55F, galil.getNonPenPenalty());
        assertEquals(0.8F, galil.getBarelyPenPenalty());

        assertEquals(90F, bullet("headPen 90").getBodyArmorPenetration(0F));
        assertEquals(300F, bullet("armPen 90", "bodyArmorPen 300").getBodyArmorPenetration(0F));
    }

    @Test
    void longRangePenetrationKeepsTheRoundsOwnWhenUnauthored()
    {
        BulletType type = bullet("bodyArmorPen 1000", "dynamicBulletDelay 2");
        assertEquals(1000F, type.getBodyArmorPenetration(50F));
        // Without a delay the long-range value is never used.
        assertEquals(300F, bullet("bodyArmorPen 300", "dynamicBodyArmorPen 10").getBodyArmorPenetration(10_000F));
    }

    @Test
    void penetrationBandsFollowTheFork()
    {
        assertEquals(ZoneArmor.Result.PENETRATED, ZoneArmor.resolve(100F, 0F));
        assertEquals(ZoneArmor.Result.NOT_PENETRATED, ZoneArmor.resolve(84F, 100F));
        assertEquals(ZoneArmor.Result.BARELY_PENETRATED, ZoneArmor.resolve(85F, 100F));
        assertEquals(ZoneArmor.Result.BARELY_PENETRATED, ZoneArmor.resolve(99.9F, 100F));
        assertEquals(ZoneArmor.Result.PENETRATED, ZoneArmor.resolve(100F, 100F));
        assertEquals(0.25F, ZoneArmor.damageFactor(ZoneArmor.Result.NOT_PENETRATED, 0.25F, 0.5F));
        assertEquals(0.5F, ZoneArmor.damageFactor(ZoneArmor.Result.BARELY_PENETRATED, 0.25F, 0.5F));
        assertEquals(1F, ZoneArmor.damageFactor(ZoneArmor.Result.PENETRATED, 0.25F, 0.5F));
    }

    @Test
    void hitboxesMapToZonesByDirectionAndHeight()
    {
        assertEquals(ArmorZone.HEAD, ArmorZone.of(EnumHitboxType.HEAD, true, false));
        assertEquals(ArmorZone.FACE, ArmorZone.of(EnumHitboxType.HEAD, false, true));
        assertEquals(ArmorZone.NAPE, ArmorZone.of(EnumHitboxType.HEAD, true, true));
        assertEquals(ArmorZone.BODY, ArmorZone.of(EnumHitboxType.BODY, false, false));
        assertEquals(ArmorZone.BACK, ArmorZone.of(EnumHitboxType.BODY, true, false));
        assertEquals(ArmorZone.ARM, ArmorZone.of(EnumHitboxType.LEFTARM, false, false));
        assertEquals(ArmorZone.LEG, ArmorZone.of(EnumHitboxType.LEGS, true, false));
        assertNull(ArmorZone.of(EnumHitboxType.RIGHTITEM, false, false));
    }

    @Test
    void driveableDeathRadiusResistanceWreckAndSeatInvincibility()
    {
        VehicleType tank = vehicle("isExplosionWhenDestroyedRadius 25", "explosionResistance 0.8", "LeaveWreck false",
            "Passenger 1 0 0 0 core", "invincible 0 false", "invincible 1 true");
        assertTrue(tank.isExplosionWhenDestroyed());
        assertEquals(25F, tank.getDeathExplosionRadius());
        assertEquals(0.8F, tank.getExplosionResistance());
        assertTrue(tank.isNoWreck());
        assertFalse(tank.getSeat(0).isInvincible());
        assertTrue(tank.getSeat(1).isInvincible());

        VehicleType plain = vehicle("LeaveWreck true", "DeathExplosionRadius 6", "isExplosionWhenDestroyedRadius 25", "explosionResistance -1");
        assertEquals(6F, plain.getDeathExplosionRadius());
        assertEquals(1F, plain.getExplosionResistance());
        assertFalse(plain.isNoWreck());
        assertFalse(vehicle().isExplosionWhenDestroyed());
    }

    private static ArmorType armor(String... lines)
    {
        ArmorType type = new ArmorType();
        type.load(new TypeFile("zone", EnumType.ARMOR, PACK, List.of(lines)));
        return type;
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("zone", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }

    private static VehicleType vehicle(String... lines)
    {
        List<String> all = new ArrayList<>(List.of("Driver 0 0 0"));
        all.addAll(List.of(lines));
        VehicleType type = new VehicleType();
        type.read(new TypeFile("zone", EnumType.VEHICLE, PACK, all));
        return type;
    }
}
