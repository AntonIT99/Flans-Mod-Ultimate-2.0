package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.guns.GunSoundPermissions.Kind;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;

class GunSoundPermissionsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void onlyAcceptedActionsAuthorizeFiniteSoundsForTheSameHandAndStack()
    {
        GunSoundPermissions permissions = new GunSoundPermissions();
        ItemStack gun = new ItemStack(Items.STICK);
        InteractionHand hand = InteractionHand.MAIN_HAND;
        assertFalse(permissions.take(hand, Kind.ACTION, gun, 1));
        permissions.shot(hand, gun, 2);
        assertFalse(permissions.take(InteractionHand.OFF_HAND, Kind.ACTION, gun, 3));
        assertFalse(permissions.take(hand, Kind.ACTION, gun.copy(), 3));
        assertFalse(permissions.take(hand, Kind.INSERT, gun, 3));
        assertTrue(permissions.take(hand, Kind.ACTION, gun, 3));
        assertFalse(permissions.take(hand, Kind.ACTION, gun, 4));
        permissions.shot(hand, gun, 5);
        permissions.clear(hand);
        assertFalse(permissions.take(hand, Kind.ACTION, gun, 6));
    }

    @Test
    void reloadInsertionsHaveABudgetAndExpireAfterAnimationCompletion()
    {
        GunSoundPermissions permissions = new GunSoundPermissions();
        ItemStack gun = new ItemStack(Items.STICK);
        InteractionHand hand = InteractionHand.OFF_HAND;
        permissions.reload(hand, gun, 10, 30F, 2);
        for (long tick = 11; tick <= 14; tick++)
            assertTrue(permissions.take(hand, Kind.INSERT, gun, tick));
        assertFalse(permissions.take(hand, Kind.INSERT, gun, 15));
        permissions.reload(hand, gun, 20, 30F, 2);
        assertFalse(permissions.take(hand, Kind.INSERT, gun, 91));
        permissions.reload(hand, gun, 100, 30F, 2);
        assertTrue(permissions.take(hand, Kind.INSERT, gun, 101));
        assertTrue(permissions.take(hand, Kind.INSERT, gun, 101));
        assertFalse(permissions.take(hand, Kind.INSERT, gun, 101));
        assertTrue(permissions.take(hand, Kind.INSERT, gun, 102));
    }
}
