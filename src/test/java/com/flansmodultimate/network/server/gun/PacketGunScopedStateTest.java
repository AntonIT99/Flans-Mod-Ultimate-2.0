package com.flansmodultimate.network.server.gun;

import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PacketGunScopedStateTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void theScopeGivesAndTakesItsOwnNightVision()
    {
        ServerPlayer player = playerAimingNightVisionGun();
        UUID id = player.getUUID();
        try (var poses = mockStatic(GunArmPoses.class))
        {
            new PacketGunScopedState(true).handleServerSide(player, mock(ServerLevel.class));
            verify(player).addEffect(any());
            assertTrue(CommonEventHandler.getNightVisionPlayers().contains(id));

            new PacketGunScopedState(false).handleServerSide(player, mock(ServerLevel.class));
            verify(player).removeEffect(MobEffects.NIGHT_VISION);
            assertFalse(CommonEventHandler.getNightVisionPlayers().contains(id));
        }
        finally
        {
            CommonEventHandler.getNightVisionPlayers().remove(id);
        }
    }

    @Test
    void loweringTheScopeLeavesNightVisionFromElsewhere()
    {
        ServerPlayer player = playerAimingNightVisionGun();
        UUID id = player.getUUID();
        // A potion, say: the effect was there before the player raised the scope.
        when(player.hasEffect(MobEffects.NIGHT_VISION)).thenReturn(true);
        try (var poses = mockStatic(GunArmPoses.class))
        {
            new PacketGunScopedState(true).handleServerSide(player, mock(ServerLevel.class));
            new PacketGunScopedState(false).handleServerSide(player, mock(ServerLevel.class));

            verify(player, never()).addEffect(any());
            verify(player, never()).removeEffect(any());
            assertFalse(CommonEventHandler.getNightVisionPlayers().contains(id));
        }
        finally
        {
            CommonEventHandler.getNightVisionPlayers().remove(id);
        }
    }

    private static ServerPlayer playerAimingNightVisionGun()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        Inventory inventory = mock(Inventory.class);
        ItemStack stack = mock(ItemStack.class);
        GunItem gun = mock(GunItem.class);
        GunType type = mock(GunType.class);
        when(player.level()).thenReturn(level);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getSelected()).thenReturn(stack);
        when(stack.getItem()).thenReturn(gun);
        when(gun.getConfigType()).thenReturn(type);
        when(type.isAllowNightVision()).thenReturn(true);
        return player;
    }
}
