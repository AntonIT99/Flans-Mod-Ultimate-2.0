package com.flansmodultimate.common.types;

import com.flansmodultimate.common.guns.EnumMeleeDirection;
import com.flansmodultimate.common.guns.MeleeSwing;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GunTypeMeleeTest
{
    private static final IContentProvider PACK = new ContentPack("meleeTest", Path.of("build", "test-packs", "meleeTest"));

    @Test
    void directionalSwingsReadLabjacKeysAndFallBackToDefaults()
    {
        GunType gun = gun("ShortName meleeSword", "MeleeTime 8", "AddNode 0 0 0 0 0 0", "AddNode 1 0 0 0 0 0", "MeleeDamagePoint 16 0 0",
            "meleeLeft true", "MeleeLeftTime 5", "AddLeftNode 0 0 0 0 0 0", "AddLeftNode 0 0 -16 0 90 0", "MeleeLeftDamagePoint 32 0 0",
            "meleeRight true", "AddRightNode 0 0 16 0 -90 0",
            "meleeUp true");

        assertTrue(gun.hasDirectionalMelee());

        MeleeSwing left = gun.getMeleeSwing(EnumMeleeDirection.LEFT);
        assertEquals(EnumMeleeDirection.LEFT, left.direction());
        assertEquals(5, left.time());
        assertEquals(2, left.path().size());
        assertEquals(-1F, left.path().get(1).z, 1E-6F);
        assertEquals(90F, left.pathAngles().get(1).y, 1E-6F);
        assertEquals(2F, left.damagePoints().get(0).x, 1E-6F);

        // Without its own time and damage points, a swing uses the default ones
        MeleeSwing right = gun.getMeleeSwing(EnumMeleeDirection.RIGHT);
        assertEquals(EnumMeleeDirection.RIGHT, right.direction());
        assertEquals(8, right.time());
        assertEquals(1F, right.damagePoints().get(0).x, 1E-6F);

        // An overhead swing without nodes is ignored, so the default swing is used
        MeleeSwing up = gun.getMeleeSwing(EnumMeleeDirection.UP);
        assertEquals(EnumMeleeDirection.FORWARD, up.direction());
        assertEquals(8, up.time());
        assertEquals(2, up.path().size());
    }

    @Test
    void weaponsWithoutDirectionalSwingsKeepTheDefaultSwing()
    {
        GunType gun = gun("ShortName plainSword", "AddNode 0 0 0 0 0 0", "AddLeftNode 0 0 0 0 0 0");

        assertFalse(gun.hasDirectionalMelee());
        assertEquals(EnumMeleeDirection.FORWARD, gun.getMeleeSwing(EnumMeleeDirection.LEFT).direction());
        assertTrue(gun.getMeleeSwing(EnumMeleeDirection.FORWARD).hasPath());
    }

    @Test
    void shootMeleeWidensSpreadSlowsTheStrikeAndBlocks()
    {
        GunType gun = gun("ShortName gunSword", "Spread 2", "BulletSpeed 5", "shootMelee true");

        assertTrue(gun.isShootMelee());
        assertTrue(gun.isCanBlock());
        assertEquals(20F, gun.bulletSpread, 1E-6F);
        assertEquals(4F, gun.bulletSpeed, 1E-6F);
        assertTrue(gun.canHipFireWhileSprinting());
        assertEquals(100, gun.getBlockTime());
    }

    @Test
    void canBlockAndSprintingFollowExplicitDefinitions()
    {
        GunType gun = gun("ShortName stiffSword", "shootMelee true", "canBlock false", "HipFireWhileSprinting false", "BlockTime 40");

        assertFalse(gun.isCanBlock());
        assertFalse(gun.canHipFireWhileSprinting());
        assertEquals(40, gun.getBlockTime());
        assertFalse(gun("ShortName plainBlade").isCanBlock());
        assertTrue(gun("ShortName guardBlade", "CanBlock true").isCanBlock());
    }

    @Test
    void blockSettingsDefaultToTheLabjacTimedBlockAndReadLightsaberValues()
    {
        GunType timed = gun("ShortName timedBlade", "CanBlock true");
        assertFalse(timed.isHoldToBlock());
        assertEquals(0.4F, timed.getBlockMoveSpeed(), 1E-6F);
        assertEquals(0F, timed.getBlockDeflectChance(), 1E-6F);

        GunType saber = gun("ShortName testSaber", "CanBlock true", "HoldToBlock true", "BlockMoveSpeed 0.7", "BlockDeflectChance 0.75", "BlockStanceTranslate 0 0.2 0",
            "BlockStanceRotate 0 0 80");
        assertTrue(saber.isHoldToBlock());
        assertEquals(0.7F, saber.getBlockMoveSpeed(), 1E-6F);
        assertEquals(0.75F, saber.getBlockDeflectChance(), 1E-6F);
        assertEquals(0.2F, saber.getBlockStanceTranslate().y, 1E-6F);
        assertEquals(80F, saber.getBlockStanceRotate().z, 1E-6F);

        GunType clamped = gun("ShortName wildBlade", "CanBlock true", "BlockMoveSpeed 3", "BlockDeflectChance -1");
        assertEquals(1F, clamped.getBlockMoveSpeed(), 1E-6F);
        assertEquals(0F, clamped.getBlockDeflectChance(), 1E-6F);
    }

    @Test
    void meleeArmorPenetrationIsDisabledUnlessDeclared()
    {
        assertTrue(gun("ShortName dullBlade").getMeleeArmorPenetration() < 0F);
        assertEquals(12F, gun("ShortName labjacBlade", "swordArmorPen 12").getMeleeArmorPenetration(), 1E-6F);
        assertEquals(3F, gun("ShortName sharpBlade", "MeleeArmorPenetration 3").getMeleeArmorPenetration(), 1E-6F);
    }

    @Test
    void movementKeysSelectSwingsInLabjacPriority()
    {
        assertEquals(EnumMeleeDirection.LEFT, EnumMeleeDirection.fromMovement(true, true, true));
        assertEquals(EnumMeleeDirection.RIGHT, EnumMeleeDirection.fromMovement(false, true, true));
        assertEquals(EnumMeleeDirection.UP, EnumMeleeDirection.fromMovement(false, false, true));
        assertEquals(EnumMeleeDirection.FORWARD, EnumMeleeDirection.fromMovement(false, false, false));
        assertEquals(EnumMeleeDirection.FORWARD, EnumMeleeDirection.byOrdinal(99));
        assertEquals(EnumMeleeDirection.UP, EnumMeleeDirection.byOrdinal(EnumMeleeDirection.UP.ordinal()));
    }

    private static GunType gun(String... lines)
    {
        GunType gun = new GunType();
        gun.contentPack = PACK;
        gun.read(new TypeFile("meleeTest", EnumType.GUN, PACK, List.of(lines)));
        return gun;
    }
}
