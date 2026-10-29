package com.flansmodultimate.common;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.server.gun.PacketGunInput;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.item.ItemStackData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerGunAuthorityTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void packetsCannotRepeatAHeldPressAndBriefPressesSurviveUntilTheTick()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        PlayerData data = PlayerData.getInstance(player);
        InteractionHand hand = InteractionHand.MAIN_HAND;
        PacketGunInput press = new PacketGunInput(true, false, hand);
        press.handleServerSide(player, level);
        assertTrue(data.hasShootPress(hand));
        data.finishGunInputTick();
        press.handleServerSide(player, level);
        assertFalse(data.hasShootPress(hand));
        assertFalse(data.hasShootPress(InteractionHand.OFF_HAND));
        new PacketGunInput(false, false, hand).handleServerSide(player, level);
        press.handleServerSide(player, level);
        new PacketGunInput(false, false, hand).handleServerSide(player, level);
        assertTrue(data.hasShootPress(hand));
        assertFalse(data.isShootKeyPressed(hand));
        data.finishGunInputTick();
        assertFalse(data.hasShootPress(hand));
    }

    @Test
    void bothHandsAndSameTypeSlotSwitchesApplyDelayWithoutAClientPacket()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        Inventory inventory = mock(Inventory.class);
        when(player.getInventory()).thenReturn(inventory);
        GunType type = mock(GunType.class);
        when(type.getSwitchDelay()).thenReturn(12F);
        GunItem item = mock(GunItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack gun = mock(ItemStack.class);
        ItemStack other = mock(ItemStack.class);
        when(gun.getItem()).thenReturn(item);
        when(other.getItem()).thenReturn(item);
        when(gun.getOrDefault(eq(DataComponents.CUSTOM_DATA), any())).thenReturn(CustomData.EMPTY);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(gun);
        when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(other);
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        data.updateHeldGuns(player);
        assertEquals(12F, data.getShootTime(InteractionHand.MAIN_HAND));
        assertEquals(12F, data.getShootTime(InteractionHand.OFF_HAND));
        data.setShootTime(InteractionHand.MAIN_HAND, 0F);
        data.setShootTime(InteractionHand.OFF_HAND, 0F);
        ItemStackData.update(gun, tag -> tag.putInt("Paint", 3));
        data.updateHeldGuns(player);
        assertEquals(0F, data.getShootTime(InteractionHand.MAIN_HAND));
        inventory.selected = 1;
        data.updateHeldGuns(player);
        assertEquals(12F, data.getShootTime(InteractionHand.MAIN_HAND));
        assertEquals(0F, data.getShootTime(InteractionHand.OFF_HAND));
        when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(ItemStack.EMPTY);
        data.updateHeldGuns(player);
        when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(other);
        data.setShootTime(InteractionHand.OFF_HAND, 30F);
        data.updateHeldGuns(player);
        assertEquals(30F, data.getShootTime(InteractionHand.OFF_HAND));
        data.setShootTime(InteractionHand.OFF_HAND, 0F);
        when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(new ItemStack(Items.STICK));
        data.updateHeldGuns(player);
        when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(other);
        data.updateHeldGuns(player);
        assertEquals(12F, data.getShootTime(InteractionHand.OFF_HAND));
        when(type.getSwitchDelay()).thenReturn(0F);
        data.setShootTime(InteractionHand.MAIN_HAND, -0.4F);
        inventory.selected = 2;
        data.updateHeldGuns(player);
        assertEquals(-0.4F, data.getShootTime(InteractionHand.MAIN_HAND));
    }
}
