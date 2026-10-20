package com.flansmodultimate.common.entity;

import com.flansmodultimate.apocalyse.common.entity.*;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApocalypseAutonomousEntityTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void guardShotsPointAtTargetsAcrossAllQuadrantsAndElevations() throws ReflectiveOperationException
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Guard guard = new Guard(world.level);
            for (Vec3 target : List.of(new Vec3(10D, 0D, 0D), new Vec3(-10D, 0D, 0D), new Vec3(0D, 0D, 10D), new Vec3(0D, 0D, -10D), new Vec3(10D, 8D, -7D), new Vec3(-3D, -5D, 10D),
                new Vec3(0D, 8D, 0D), new Vec3(0D, -8D, 0D)))
            {
                aim(guard, target);
                assertEquals(1D, target.normalize().dot(guard.shotDirection()), 1.0E-5D, "The torso's visible direction and actual gun direction must agree");
            }
        }
    }

    @Test
    void guardReleasesAndRepressesBurstTriggersInsteadOfFiringOnlyOneBurst() throws ReflectiveOperationException
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Guard guard = new Guard(world.level);
            DriveableData data = mock(DriveableData.class);
            ItemStack stack = mock(ItemStack.class);
            GunItem gun = mock(GunItem.class);
            GunType type = mock(GunType.class);
            when(data.getMechaAddon(any())).thenReturn(stack);
            when(stack.getItem()).thenReturn(gun);
            when(gun.getConfigType()).thenReturn(type);
            when(type.getFireMode(stack)).thenReturn(EnumFireMode.BURST);
            guard.equip(data);
            guard.tickCount = 2;
            assertEquals(DriveableInput.PRIMARY_FIRE, trigger(guard));
            guard.tickCount = 3;
            assertEquals(0, trigger(guard));
            guard.tickCount = 4;
            assertEquals(DriveableInput.PRIMARY_FIRE, trigger(guard));
            guard.burstActive = true;
            guard.tickCount = 5;
            assertEquals(DriveableInput.PRIMARY_FIRE, trigger(guard), "A new rising edge must not reset a burst that is still firing");
            guard.burstActive = false;
            guard.tickCount = 7;
            assertEquals(0, trigger(guard));
            guard.tickCount = 8;
            assertEquals(DriveableInput.PRIMARY_FIRE, trigger(guard));
        }
    }

    @Test
    void autopilotCountsAsAFlightControllerWithoutAnyCrew()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            assertTrue(new Flyby(world.level).controlled());
            assertFalse(new UnoccupiedPlane(world.level).controlled());
        }
    }

    @Test
    void nukeImpactsOnceAndExpiresAfterTheConfiguredVisualLifetime()
    {
        try (EntityTestWorld world = new EntityTestWorld(); var config = mockStatic(ModApocalypseConfig.class))
        {
            config.when(ModApocalypseConfig::apocalypseNukeDropsEnabled).thenReturn(true);
            config.when(ModApocalypseConfig::apocalypseNukeVisualTicks).thenReturn(3);
            // Registries are frozen by the bootstrap, so reuse a vanilla type instead of building a new one.
            @SuppressWarnings("unchecked")
            EntityType<NukeDropEntity> type = (EntityType<NukeDropEntity>) (EntityType<?>) EntityType.ARMOR_STAND;
            NukeDropEntity nuke = new NukeDropEntity(type, world.level);
            nuke.setPos(0D, 64D, 0D);
            nuke.setOnGround(true);
            world.tick(nuke);
            assertEquals(1, nuke.getExplodedTicks());
            assertTrue(nuke.noCulling);
            world.tick(nuke);
            assertEquals(2, nuke.getExplodedTicks());
            assertFalse(nuke.isRemoved());
            world.tick(nuke);
            world.tick(nuke);
            assertFalse(nuke.isRemoved());
            world.tick(nuke);
            assertTrue(nuke.isRemoved());
        }
    }

    // Exercise the private AI decisions while leaving the real driveable transforms and synchronized state intact.
    @SuppressWarnings("java:S3011")
    private static void aim(Guard guard, Vec3 delta) throws ReflectiveOperationException
    {
        var method = AiMechaEntity.class.getDeclaredMethod("aimAt", Vec3.class);
        method.setAccessible(true);
        method.invoke(guard, delta);
    }

    @SuppressWarnings("java:S3011") // Private trigger policy is tested through real mecha inventory state.
    private static int trigger(Guard guard) throws ReflectiveOperationException
    {
        var method = AiMechaEntity.class.getDeclaredMethod("triggerFor", boolean.class);
        method.setAccessible(true);
        return (int) method.invoke(guard, true);
    }

    private static class Guard extends AiMechaEntity
    {
        private boolean burstActive;

        Guard(Level level)
        {
            super(EntityType.ARMOR_STAND, level);
        }

        Vec3 shotDirection()
        {
            return aimedDirection(getTurretYaw(), getTurretPitch());
        }

        void equip(DriveableData data)
        {
            driveableData = data;
        }

        @Override
        protected boolean hasActiveHandGunBurst(boolean left)
        {
            return burstActive;
        }
    }

    private static class Flyby extends FlyByPlaneEntity
    {
        Flyby(Level level)
        {
            super(EntityType.ARMOR_STAND, level);
        }

        boolean controlled()
        {
            return hasFlightController();
        }
    }

    private static class UnoccupiedPlane extends Plane
    {
        UnoccupiedPlane(Level level)
        {
            super(EntityType.ARMOR_STAND, level);
        }

        boolean controlled()
        {
            return hasFlightController();
        }
    }
}
