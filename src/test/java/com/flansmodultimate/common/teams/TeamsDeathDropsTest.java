package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.ToolItem;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TeamsDeathDropsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void grenadesAndToolsFollowWeaponDropsWhileOrdinaryItemsRemain()
    {
        TeamsManager manager = mock(TeamsManager.class);
        ItemEntity grenade = drop(mock(GrenadeItem.class));
        ItemEntity bandage = drop(mock(ToolItem.class));
        ItemEntity ordinary = drop(mock(Item.class));
        for (TeamsManager.EnumWeaponDrop mode : TeamsManager.EnumWeaponDrop.values())
        {
            when(manager.getWeaponDrops()).thenReturn(mode);
            List<ItemEntity> drops = new ArrayList<>(List.of(grenade, bandage, ordinary));
            TeamsDeathDrops.apply(manager, drops);
            assertEquals(mode == TeamsManager.EnumWeaponDrop.DROPS ? List.of(grenade, bandage, ordinary) : List.of(ordinary), drops);
        }
    }

    private static ItemEntity drop(Item item)
    {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        ItemEntity entity = mock(ItemEntity.class);
        when(entity.getItem()).thenReturn(stack);
        return entity;
    }
}
