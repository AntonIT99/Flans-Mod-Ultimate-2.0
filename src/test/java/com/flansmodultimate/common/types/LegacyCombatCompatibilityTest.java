package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyCombatCompatibilityTest
{
    private static final ContentPack PACK = new ContentPack("test", Path.of("build", "test-packs", "legacy-combat"));

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static VehicleType vehicle(String... lines)
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("test", EnumType.VEHICLE, PACK, List.of(lines)));
        return type;
    }

    @Test
    void authoredBarrelsOverrideMeasurementsAndUseBlockSpacing()
    {
        VehicleType type = vehicle("Passenger 1 0 0 0 core", "GunOrigin 1 16 32 48", "barrels 1 3", "barrelSpread 1 2");
        var seat = type.getSeat(1);
        seat.setGunBarrels(List.of(new Vector3f(0, 0, -1), new Vector3f(0, 0, 1)));
        assertEquals(3, seat.getGunBarrelCount());
        assertEquals(1F, seat.getGunOrigin(0).z);
        assertEquals(3F, seat.getGunOrigin(1).z);
        assertEquals(5F, seat.getGunOrigin(2).z);
        assertEquals(1F, seat.getGunOrigin(0).x);
    }

    @Test
    void unsetBarrelsRetainMeasuredGeometryAndAliasesAreAccepted()
    {
        var seat = vehicle("Passenger 1 0 0 0 core").getSeat(1);
        seat.setGunBarrels(List.of(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()));
        assertEquals(4, seat.getGunBarrelCount());
        var authored = vehicle("Passenger 1 0 0 0 core", "SeatGuns 1 2", "SeatGunSpread 1 3").getSeat(1);
        assertEquals(2, authored.getGunBarrelCount());
        assertEquals(3F, authored.getBarrelSpread());
    }

    @Test
    void passengerZoomEnablesUsableOpticsAndExplicitSightZoomsWin()
    {
        var optics = vehicle("Passenger 1 0 0 0 core", "PassengerZoom 1 3").getSeat(1).getOptics();
        assertTrue(optics.available());
        assertEquals(3F, optics.zoom(0));
        optics = vehicle("Passenger 1 0 0 0 core", "SeatGunsightZoom 1 4 8", "PassengerZoom 1 3").getSeat(1).getOptics();
        assertEquals(4F, optics.zoom(0));
        assertEquals(8F, optics.zoom(1));
        assertFalse(vehicle("Passenger 1 0 0 0 core", "PassengerHasScope 1 false", "PassengerZoom 1 3").getSeat(1).getOptics().available());
    }

    @Test
    void passengerParticlesKeepOffsetsVelocitiesAndEnableGateRegardlessOfLineOrder()
    {
        VehicleType type = vehicle("Passenger 1 0 0 0 core", "PassengerShootParticles 1 smoke 3 2 0.1 0.2 0.3", "PassengerHasParticles 1 true");
        var particles = type.getShootParticlesPassenger(1);
        assertEquals(List.of(new DriveableType.ShootParticle("smoke", 0.1F, 0.2F, 0.3F, 3F, 2F)), particles);
        assertTrue(vehicle("Passenger 1 0 0 0 core", "PassengerShootParticles 1 smoke 3 2 0 0 0", "PassengerHasParticles 1 false").getShootParticlesPassenger(1).isEmpty());
        assertEquals(Vec3.ZERO, new WeaponMuzzle.Particle("smoke", Vec3.ZERO).offset());
    }

    @Test
    void legacyRecoilEnablesKickAndModernSettingsTakePriority()
    {
        assertEquals(new DriveableType.ScreenShake(2F, 0.18F), vehicle("Primary shell", "cannonRecoil 2").screenShake(false));
        assertEquals(new DriveableType.ScreenShake(0.5F, 0.18F), vehicle("Primary shell", "cannonRecoil 2", "FancyScreenShakePrimaryIntensity 0.5").screenShake(false));
        assertNull(vehicle("Primary shell", "cannonRecoil 2", "FancyScreenShake false").screenShake(false));
    }

    @Test
    void bayonetModeRequiresEquippedGripAndSecondaryMode()
    {
        AttachmentType grip = new AttachmentType();
        grip.read(new TypeFile("bayonet", EnumType.ATTACHMENT, PACK, List.of("Bayonet true", "BayonetMeleeTime 17")));
        GunType gun = spy(new GunType());
        doReturn(grip).when(gun).getGrip(ItemStack.EMPTY);
        doReturn(false).when(gun).getSecondaryFire(ItemStack.EMPTY);
        assertTrue(gun.canToggleSecondaryFire(ItemStack.EMPTY));
        assertFalse(gun.isBayonetMode(ItemStack.EMPTY));
        doReturn(true).when(gun).getSecondaryFire(ItemStack.EMPTY);
        assertTrue(gun.isBayonetMode(ItemStack.EMPTY));
        assertEquals(17, gun.getMeleeTime(ItemStack.EMPTY));
        doReturn(null).when(gun).getGrip(ItemStack.EMPTY);
        assertFalse(gun.isBayonetMode(ItemStack.EMPTY));
    }

    @Test
    void sirenAndNavigationAreOptIn()
    {
        PlaneType plane = new PlaneType();
        plane.read(new TypeFile("plane", EnumType.PLANE, PACK, List.of("StukaSound siren", "StukaSpeed 220", "StukaSoundLength 30", "StukaSoundRange 100")));
        assertEquals("siren", plane.getStukaSound());
        assertEquals(220F, plane.getStukaSpeed());
        assertEquals(30, plane.getStukaSoundLength());
        assertEquals(100, plane.getStukaSoundRange());
        BulletType bullet = new BulletType();
        assertFalse(bullet.isProportionalNavigation());
        bullet.read(new TypeFile("missile", EnumType.BULLET, PACK, List.of("ProportionalNavigation true", "ProNavGain 4")));
        assertTrue(bullet.isProportionalNavigation());
        assertEquals(4F, bullet.getProNavGain());
    }
}
