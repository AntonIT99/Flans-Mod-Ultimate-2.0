package com.flansmodultimate.network.server;

import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketSelectPaintjobTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void duplicateDyesArePaidInFullForBothWorkbenchAndPaintTable()
    {
        for (boolean table : new boolean[]{false, true})
        {
            ServerPlayer player = mock(ServerPlayer.class);
            Inventory inventory = new Inventory(player);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getAbilities()).thenReturn(new Abilities());
            GunType type = mock(GunType.class);
            GunItem item = mock(GunItem.class);
            when(item.getConfigType()).thenReturn(type);
            when(item.getPaintableType()).thenReturn(type);
            ItemStack gun = mock(ItemStack.class);
            when(gun.getItem()).thenReturn(item);
            Paintjob paint = mock(Paintjob.class);
            when(paint.getId()).thenReturn(1);
            when(paint.isAddToTables()).thenReturn(true);
            when(paint.getDyesNeeded()).thenReturn(List.of(new ItemStack(Items.RED_DYE), new ItemStack(Items.RED_DYE)));
            when(type.isAddAnyPaintjobToTables()).thenReturn(true);
            when(type.getPaintjobs()).thenReturn(Map.of(1, paint));
            when(type.getApplicablePaintjobs()).thenReturn(List.of(paint));
            ItemStack cans = new ItemStack(Items.RED_DYE);
            if (table)
            {
                PaintjobTableMenu menu = mock(PaintjobTableMenu.class);
                when(menu.getPaintableStack()).thenReturn(gun);
                when(menu.getPaintCanStack()).thenReturn(cans);
                player.containerMenu = menu;
            }
            else
            {
                GunWorkbenchMenu menu = mock(GunWorkbenchMenu.class);
                when(menu.getGunStack()).thenReturn(gun);
                player.containerMenu = menu;
                inventory.setItem(0, new ItemStack(Items.RED_DYE));
            }
            PacketSelectPaintjob request = new PacketSelectPaintjob(1);
            ServerLevel level = mock(ServerLevel.class);
            request.handleServerSide(player, level);
            verify(type, never()).applyPaintjobToStack(gun, paint);
            if (table)
            {
                assertEquals(1, cans.getCount());
                inventory.setItem(0, new ItemStack(Items.RED_DYE));
            }
            else
            {
                assertEquals(1, inventory.getItem(0).getCount());
                inventory.getItem(0).grow(1);
            }
            request.handleServerSide(player, level);
            verify(type).applyPaintjobToStack(gun, paint);
            assertTrue(inventory.getItem(0).isEmpty());
            if (table)
                assertTrue(cans.isEmpty());
        }
    }
}
