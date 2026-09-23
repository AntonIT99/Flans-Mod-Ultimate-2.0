package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AmmoGroupTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "ammogroups"));

    private static BulletType bullet(String shortName, String... extraLines)
    {
        BulletType type = new BulletType();
        List<String> lines = new java.util.ArrayList<>();
        lines.add("ShortName " + shortName);
        lines.addAll(List.of(extraLines));
        type.read(new TypeFile(shortName, EnumType.BULLET, PACK, lines));
        return type;
    }

    @Test
    void firstMentionDeclaresTheGroupAndLaterMentionsJoinIt()
    {
        BulletType ball = bullet("agtBall", "AddToAmmoGroup AgtCalibreA");
        BulletType armorPiercing = bullet("agtAp", "AddToAmmoGroup agtcalibrea");

        ShootableType.AmmoGroup group = ShootableType.getAmmoGroup("AGTCALIBREA");
        assertNotNull(group);
        assertEquals("AgtCalibreA", group.getName());
        assertEquals(List.of(ball, armorPiercing), group.getMembers());
    }

    @Test
    void gunsPullInEveryAmmoItemOfTheGroup()
    {
        BulletType ball = bullet("agtGunBall", "AddToAmmoGroup AgtCalibreB");
        BulletType tracer = bullet("agtGunTracer", "AddToAmmoGroup AgtCalibreB");

        GunType gun = new GunType();
        gun.read(new TypeFile("agtGun", EnumType.GUN, PACK, List.of("ShortName agtGun", "UseAmmoGroup AgtCalibreB")));

        assertEquals(List.of(ball, tracer), gun.getAmmoTypes());
        assertEquals(ball, gun.getDefaultAmmo().orElse(null));
    }

    @Test
    void aaGunsPullInEveryAmmoItemOfTheGroup()
    {
        BulletType shell = bullet("agtAaShell", "AddToAmmoGroup AgtCalibreC");

        AAGunType aaGun = new AAGunType();
        aaGun.read(new TypeFile("agtAaGun", EnumType.AA_GUN, PACK, List.of("ShortName agtAaGun", "UseAmmoGroup AgtCalibreC")));

        assertEquals(List.of(shell), aaGun.getAmmoTypes());
    }

    @Test
    void driveablesPullInEveryBulletOfTheGroup()
    {
        BulletType shell = bullet("agtVehicleShell", "AddToAmmoGroup AgtCalibreD");

        VehicleType vehicle = new VehicleType();
        vehicle.read(new TypeFile("agtVehicle", EnumType.VEHICLE, PACK, List.of("ShortName agtVehicle", "Driver 0 0 0", "UseAmmoGroup AgtCalibreD")));

        assertEquals(List.of(shell), vehicle.getAmmoTypes());
        assertTrue(vehicle.isValidAmmo(shell));
    }

    @Test
    void ammoJoiningAfterAWeaponResolvedItsGroupIsStillPickedUp()
    {
        BulletType early = bullet("agtEarly", "AddToAmmoGroup AgtCalibreE");

        VehicleType vehicle = new VehicleType();
        vehicle.read(new TypeFile("agtLateVehicle", EnumType.VEHICLE, PACK, List.of("ShortName agtLateVehicle", "Driver 0 0 0", "UseAmmoGroup AgtCalibreE")));
        assertEquals(List.of(early), vehicle.getAmmoTypes());

        BulletType late = bullet("agtLate", "AddToAmmoGroup AgtCalibreE");
        assertEquals(List.of(early, late), vehicle.getAmmoTypes());
    }

    @Test
    void oneAmmoItemMayBelongToSeveralGroupsAndOneWeaponMayUseSeveral()
    {
        BulletType shared = bullet("agtShared", "AddToAmmoGroup AgtCalibreF", "AddToAmmoGroup AgtCalibreG");
        BulletType other = bullet("agtOther", "AddToAmmoGroup AgtCalibreG");

        GunType gun = new GunType();
        gun.read(new TypeFile("agtMultiGun", EnumType.GUN, PACK, List.of("ShortName agtMultiGun",
            "UseAmmoGroup AgtCalibreF", "UseAmmoGroup AgtCalibreG")));

        assertEquals(List.of(shared, other), gun.getAmmoTypes());
    }

    @Test
    void groupNamesMayContainSpacesAndIgnoreTrailingComments()
    {
        BulletType spaced = bullet("agtSpaced", "AddToAmmoGroup 7.62 x 39 mm // rifle round");

        ShootableType.AmmoGroup group = ShootableType.getAmmoGroup("7.62 X 39 MM");
        assertNotNull(group);
        assertEquals(List.of(spaced), group.getMembers());
    }

    @Test
    void unknownGroupContributesNoAmmo()
    {
        GunType gun = new GunType();
        gun.read(new TypeFile("agtUnknownGun", EnumType.GUN, PACK, List.of("ShortName agtUnknownGun", "UseAmmoGroup AgtNeverDeclared")));

        assertNull(ShootableType.getAmmoGroup("AgtNeverDeclared"));
        assertTrue(gun.getAmmoTypes().isEmpty());
    }
}
