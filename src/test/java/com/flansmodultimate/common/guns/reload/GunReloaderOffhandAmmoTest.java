package com.flansmodultimate.common.guns.reload;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The offhand names the ammo a reload prefers, the way a bow prefers the arrows held there. It may only
 * reorder the ammo a reload searches, so anything it cannot decide has to leave the gun's own choice intact.
 */
class GunReloaderOffhandAmmoTest
{
    private static final String STORED = "buckshot";

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void compatibleOffhandAmmoNamesTheTypeTheReloadPrefers()
    {
        ShootableType slugs = type("slug", 1);
        ItemStack offhand = ammoStack(slugs, 4);

        assertEquals("slug", GunReloader.offhandPreferredAmmo(List.of(slugs), offhand, STORED));
    }

    @Test
    void anEmptyOffhandLeavesTheStoredPreferenceAlone()
    {
        ShootableType slugs = type("slug", 1);
        ItemStack offhand = mock(ItemStack.class);
        when(offhand.isEmpty()).thenReturn(true);

        assertEquals(STORED, GunReloader.offhandPreferredAmmo(List.of(slugs), offhand, STORED));
    }

    @Test
    void ammoTheGunDoesNotAcceptLeavesTheStoredPreferenceAlone()
    {
        ShootableType slugs = type("slug", 1);
        ShootableType rifleRounds = type("762x39", 1);
        ItemStack offhand = ammoStack(rifleRounds, 30);

        assertEquals(STORED, GunReloader.offhandPreferredAmmo(List.of(slugs), offhand, STORED));
    }

    @Test
    void anEmptiedMagazineInTheOffhandLeavesTheStoredPreferenceAlone()
    {
        ShootableType slugs = type("slug", 1);
        ItemStack offhand = ammoStack(slugs, 0);

        assertEquals(STORED, GunReloader.offhandPreferredAmmo(List.of(slugs), offhand, STORED));
    }

    /** A gun in the offhand holds no ammo, so a dual wielder's second gun cannot pick its own ammo type. */
    @Test
    void aSecondGunInTheOffhandLeavesTheStoredPreferenceAlone()
    {
        ShootableType slugs = type("slug", 1);
        ItemStack offhand = mock(ItemStack.class);
        when(offhand.isEmpty()).thenReturn(false);
        GunItem offhandGun = mock(GunItem.class);
        when(offhand.getItem()).thenReturn(offhandGun);

        assertEquals(STORED, GunReloader.offhandPreferredAmmo(List.of(slugs), offhand, STORED));
    }

    /** The ammo HUD asks the same question with no stored preference, to show only a choice actually in force. */
    @Test
    void theQuestionOfWhetherTheOffhandDecidesAtAllAnswersNullWhenItDoesNot()
    {
        ShootableType slugs = type("slug", 1);
        ItemStack compatible = ammoStack(slugs, 4);
        ItemStack empty = mock(ItemStack.class);
        when(empty.isEmpty()).thenReturn(true);

        assertEquals("slug", GunReloader.offhandPreferredAmmo(List.of(slugs), compatible, null));
        assertNull(GunReloader.offhandPreferredAmmo(List.of(slugs), empty, null));
    }

    private static ShootableType type(String shortName, int roundsPerItem)
    {
        ShootableType type = mock(BulletType.class);
        when(type.getOriginalShortName()).thenReturn(shortName);
        when(type.getRoundsPerItem()).thenReturn(roundsPerItem);
        return type;
    }

    private static ItemStack ammoStack(ShootableType type, int count)
    {
        ShootableItem item = mock(ShootableItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getItem()).thenReturn(item);
        when(stack.getCount()).thenReturn(count);
        return stack;
    }
}
