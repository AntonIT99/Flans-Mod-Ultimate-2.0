package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReloadPouchSupportTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void specializedPouchesRequireTheirAuthoredWeaponKind()
    {
        for (String[] pair : List.of(new String[]{"hasBowPouch", "Bow"}, new String[]{"hasOldGunPouch", "OldGun"},
            new String[]{"hasGunPouch", "RepeatingGun"}, new String[]{"hasHeavyPouch", "Heavy"}))
        {
            ArmorType armor = armor(pair[0] + " true", "reloadMultiplier 0.9");
            assertFalse(ReloadPouchSupport.matches(armor, gun()));
            assertTrue(ReloadPouchSupport.matches(armor, gun(pair[1] + " true")));
        }
        assertTrue(ReloadPouchSupport.matches(armor("hasMagPouch true"), gun()));
        assertFalse(ReloadPouchSupport.matches(armor("reloadMultiplier 0.5"), gun()));
    }

    @Test
    void fastestMatchingPouchWinsAndRemovalRestoresDefault()
    {
        LivingEntity holder = mock(LivingEntity.class);
        GunItem item = mock(GunItem.class);
        when(item.getConfigType()).thenReturn(gun());
        ItemStack weapon = mock(ItemStack.class);
        when(weapon.getItem()).thenReturn(item);
        ItemStack fast = worn(armor("hasPouch true", "reloadMultiplier 0.7"));
        ItemStack slow = worn(armor("hasPouch true", "pouchMultiplier 1.2"));
        when(holder.getArmorSlots()).thenReturn(List.of(fast, slow));
        assertEquals(0.7F, ReloadPouchSupport.multiplier(holder, weapon));
        when(holder.getArmorSlots()).thenReturn(List.of(slow, fast));
        assertEquals(0.7F, ReloadPouchSupport.multiplier(holder, weapon));
        when(holder.getArmorSlots()).thenReturn(List.of(slow));
        assertEquals(1.2F, ReloadPouchSupport.multiplier(holder, weapon));
        when(holder.getArmorSlots()).thenReturn(List.of());
        assertEquals(1F, ReloadPouchSupport.multiplier(holder, weapon));
        assertEquals(1F, ReloadPouchSupport.multiplier(holder, ItemStack.EMPTY));
    }

    @Test
    void invalidMultipliersCannotCreateInstantOrNonfiniteReloads()
    {
        for (String value : List.of("NaN", "Infinity", "0", "-1"))
            assertEquals(1F, armor("reloadMultiplier " + value).getReloadMultiplier());
    }

    private static ItemStack worn(ArmorType type)
    {
        CustomArmorItem item = mock(CustomArmorItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        return stack;
    }

    private static GunType gun(String... lines)
    {
        GunType type = new GunType();
        type.load(file(EnumType.GUN, lines));
        return type;
    }

    private static ArmorType armor(String... lines)
    {
        ArmorType type = new ArmorType();
        type.load(file(EnumType.ARMOR, lines));
        return type;
    }

    private static TypeFile file(EnumType kind, String... lines)
    {
        return new TypeFile("pouch", kind, new ContentPack("test", Path.of("build", "test-packs", "pouches")), List.of(lines));
    }
}
