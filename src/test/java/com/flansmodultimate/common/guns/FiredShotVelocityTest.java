package com.flansmodultimate.common.guns;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The velocity a shot is actually fired with, which is also the velocity its kinetic damage and
 * penetration are derived from.
 *
 * <p>Precedence, highest first: a per-ammunition override declared by the weapon or the platform
 * carrying it, the round selected from a belt, the ammunition's own {@code MuzzleVelocity}, and
 * finally whatever the weapon itself supplies. Whichever wins is then scaled by the weapon's
 * attachment multiplier.
 */
class FiredShotVelocityTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "fired-shot"));

    @Test
    void aShellFlipsToItsOwnMuzzleVelocityInsteadOfTheVehiclesBulletSpeed()
    {
        // BulletSpeed 3 is the DriveableType default, so before this was fixed every shell flew at it.
        VehicleType vehicle = vehicle("BulletSpeed 3");
        BulletType shell = bullet("Mass 6800", "MuzzleVelocity 770");

        assertEquals(38.5F, shot(vehicle, shell, 3F).getMuzzleVelocity(), 1.0E-4F);
    }

    @Test
    void theVehicleVelocityIsOnlyTheFallback()
    {
        VehicleType vehicle = vehicle("BulletSpeed 3");
        BulletType shell = bullet("Mass 6800");

        assertEquals(3F, shot(vehicle, shell, 3F).getMuzzleVelocity(), 1.0E-4F);
    }

    @Test
    void anAmmoMuzzleVelocityOverrideTakesTheShellBack()
    {
        VehicleType vehicle = vehicle("BulletSpeed 3", "AmmoMuzzleVelocity syntheticShell 990");
        assertTrue(vehicle.getAmmoOverrides().asMap().containsKey(AmmoOverrides.key("syntheticShell")));
        BulletType shell = bullet("ShortName syntheticShell", "Mass 6800", "MuzzleVelocity 770");

        assertEquals(49.5F, shot(vehicle, shell, 3F).getMuzzleVelocity(), 1.0E-4F,
            "the vehicle's own override outranks the ammunition it names");
    }

    @Test
    void anOverrideForAnotherRoundLeavesThisOneAlone()
    {
        VehicleType vehicle = vehicle("BulletSpeed 3", "AmmoMuzzleVelocity someOtherShell 990");
        BulletType shell = bullet("ShortName syntheticShell", "Mass 6800", "MuzzleVelocity 770");

        assertEquals(38.5F, shot(vehicle, shell, 3F).getMuzzleVelocity(), 1.0E-4F);
    }

    @Test
    void theBeltPositionSelectsTheRoundsVelocity()
    {
        VehicleType vehicle = vehicle("BulletSpeed 3");
        BulletType belt = bullet("RoundsPerItem 2",
            "AddRound AP 1 162 0 800 45",
            "AddRound HE 1 135 0.016 835 0");

        assertEquals(40F, shot(vehicle, belt, 3F, 0).getMuzzleVelocity(), 1.0E-4F);
        assertEquals(41.75F, shot(vehicle, belt, 3F, 1).getMuzzleVelocity(), 1.0E-4F);
    }

    @Test
    void anAttachmentMultiplierStillScalesAmmunitionThatStatesItsOwnVelocity()
    {
        BulletType round = bullet("Mass 9", "MuzzleVelocity 400");
        FireableGun gun = new FireableGun(round, 1F, 0F, 3F, 1.25F, EnumSpreadPattern.CIRCLE);

        assertEquals(25F, new FiredShot(gun, round, null, null, 0).getMuzzleVelocity(), 1.0E-4F);
    }

    @Test
    void nothingDeclaringAVelocityMarksTheShotAsHitscan()
    {
        BulletType round = bullet("Mass 9");
        FireableGun gun = new FireableGun(round, 1F, 0F, 0F, EnumSpreadPattern.CIRCLE);
        FiredShot firedShot = new FiredShot(gun, round, null, null, 0);

        assertEquals(0F, firedShot.getMuzzleVelocity(false),
            "no declared velocity anywhere is what makes a weapon raytrace");
        assertEquals(BulletType.DEFAULT_BULLET_SPEED, firedShot.getMuzzleVelocity(), 1.0E-4F,
            "damage still needs a velocity, so the default stands in for it");
    }

    // ------------------------------------------------------------- fixtures

    private static FiredShot shot(VehicleType vehicle, BulletType bulletType, float weaponSpeed)
    {
        return shot(vehicle, bulletType, weaponSpeed, 0);
    }

    private static FiredShot shot(VehicleType vehicle, BulletType bulletType, float weaponSpeed, int magazinePosition)
    {
        FireableGun fireable = new FireableGun(vehicle, 1F, 0F, weaponSpeed, EnumSpreadPattern.CIRCLE);
        return new FiredShot(fireable, bulletType, null, null, magazinePosition);
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("syntheticShell", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }

    /** A driver line keeps the parser off its logging path, as in the other driveable type tests. */
    private static VehicleType vehicle(String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        TestVehicleType type = new TestVehicleType();
        type.readDefinition(new TypeFile("syntheticVehicle", EnumType.VEHICLE, PACK, definition));
        return type;
    }

    /** The parser entry point is protected, so the test reaches it through a subclass. */
    private static final class TestVehicleType extends VehicleType
    {
        private void readDefinition(TypeFile file)
        {
            read(file);
        }
    }
}
