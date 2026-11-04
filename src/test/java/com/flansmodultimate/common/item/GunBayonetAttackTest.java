package com.flansmodultimate.common.item;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.teams.TeamsSpawnProtection;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GunBayonetAttackTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("java:S3011") // Frozen test registries require injecting the type without constructing an item.
    void bayonetUsesVirtualAuthoredAmmoWithoutReadingOrChangingMagazinesAndRespectsCooldown() throws ReflectiveOperationException
    {
        GunType type = mock(GunType.class);
        GunItem item = mock(GunItem.class);
        var typeField = GunItem.class.getDeclaredField("configType");
        typeField.setAccessible(true);
        typeField.set(item, type);
        ItemStack stack = mock(ItemStack.class);
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        ShootableType ammo = mock(ShootableType.class);
        when(type.isBayonetMode(stack)).thenReturn(true);
        when(type.isUsableByPlayers()).thenReturn(true);
        when(type.getBayonetAmmo(stack)).thenReturn(Optional.of(ammo));
        when(type.getShootDelay(stack)).thenReturn(17F);
        when(player.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        GunItemHandler handler = spy(new GunItemHandler(item));
        doReturn(true).when(handler).gunCanBeHandled(player);
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        try (var shooting = mockStatic(ShootingHelper.class); var packets = mockStatic(PacketHandler.class); var protection = mockStatic(TeamsSpawnProtection.class))
        {
            handler.doBayonetAttack(level, player, data, stack, InteractionHand.MAIN_HAND);
            shooting.verify(() -> ShootingHelper.fireGun(eq(level), eq(player), eq(type), eq(ammo), eq(stack), same(ItemStack.EMPTY), same(ItemStack.EMPTY), any()));
            assertEquals(17F, data.getShootTime(InteractionHand.MAIN_HAND));
            handler.doBayonetAttack(level, player, data, stack, InteractionHand.MAIN_HAND);
            shooting.verifyNoMoreInteractions();
            verify(item, never()).getAmmoItemStack(any(), anyInt(), any());
            verify(item, never()).setBulletItemStack(any(), any(), anyInt(), any());
        }
    }
}
