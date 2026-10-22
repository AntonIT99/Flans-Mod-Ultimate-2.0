package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.common.entity.ThrownGun;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.GunItemHandler;
import com.flansmodultimate.common.types.EnumMovement;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.item.ItemStackData;
import org.junit.jupiter.api.*;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EquipmentSupportThrowTest
{
    private LivingEntity holder;
    private ServerLevel level;
    private ItemStack source;
    private GunType type;
    private ItemStack carried;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    void prepareHolder()
    {
        holder = mock(LivingEntity.class);
        level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(level.getServer()).thenReturn(server);
        when(server.isSameThread()).thenReturn(true);
        when(holder.level()).thenReturn(level);
        when(holder.isAlive()).thenReturn(true);
        when(holder.onGround()).thenReturn(true);
        when(holder.getDeltaMovement()).thenReturn(Vec3.ZERO);
        type = mock(GunType.class);
        when(type.isThrowable()).thenReturn(true);
        source = mock(ItemStack.class);
        GunItem item = mock(GunItem.class);
        GunItemHandler handler = mock(GunItemHandler.class);
        when(source.getItem()).thenReturn(item);
        when(item.getConfigType()).thenReturn(type);
        when(item.getGunItemHandler()).thenReturn(handler);
        when(handler.gunCanBeHandled(holder)).thenReturn(true);
        when(type.getBulletSpeed(source)).thenReturn(2.5F);
        when(type.getSpread(source, EnumMovement.NONE, false)).thenReturn(3F);
        when(type.getThrowDamage(source)).thenReturn(9F);
        carried = new ItemStack(Items.STICK);
        ItemStackData.update(carried, tag -> tag.putString("CustomWeaponData", "retained"));
        when(source.copyWithCount(1)).thenReturn(carried);
    }

    @Test
    void nativeThrowRetainsItsWeaponDamageVelocitySpreadAndOwnerWithoutConsumingInventory()
    {
        List<List<?>> arguments = new ArrayList<>();
        try (var projectiles = mockConstruction(ThrownGun.class, (projectile, context) -> arguments.add(context.arguments())))
        {
            when(level.addFreshEntity(any())).thenReturn(true);
            var shot = FlansEquipment.prepareThrow(holder, source).orElseThrow();
            assertEquals(2.5F, shot.speed());
            assertEquals(1, shot.launch(Vec3.ZERO, new Vec3(1D, 0D, 0D), false).size());
            ThrownGun projectile = projectiles.constructed().get(0);
            assertSame(holder, arguments.get(0).get(1));
            ItemStack weapon = (ItemStack) arguments.get(0).get(2);
            assertNotSame(carried, weapon);
            assertEquals("retained", ItemStackData.copy(weapon).getString("CustomWeaponData"));
            assertEquals(1, weapon.getCount());
            assertEquals(9F, arguments.get(0).get(3));
            assertEquals(AbstractArrow.Pickup.ALLOWED, projectile.pickup);
            verify(projectile).shoot(1D, 0D, 0D, 2.5F, 3F * ShootingHelper.ANGULAR_SPREAD_FACTOR / 0.0172275F);
            verify(source, never()).shrink(anyInt());
        }
    }

    @Test
    void failedSpawnsAndInvalidDirectionsDoNotReportASuccessfulThrow()
    {
        try (var projectiles = mockConstruction(ThrownGun.class))
        {
            var shot = FlansEquipment.prepareThrow(holder, source).orElseThrow();
            assertTrue(shot.launch(Vec3.ZERO, Vec3.ZERO, false).isEmpty());
            assertTrue(projectiles.constructed().isEmpty());
            assertTrue(shot.launch(Vec3.ZERO, new Vec3(1D, 0D, 0D), false).isEmpty());
            verify(source, never()).shrink(anyInt());
        }
        when(type.isThrowable()).thenReturn(false);
        assertTrue(FlansEquipment.prepareThrow(holder, source).isEmpty());
    }
}
