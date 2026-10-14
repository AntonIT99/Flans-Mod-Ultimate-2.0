package com.wolffsmod.npcs.soldier;

import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlanSoldierLoadoutTest
{
    /**
     * Starting items are named after what they are, so a test reads like the class definition it stands for: "rifleGun"
     * is loaded with "rifleAmmo", and two guns of the same name are the same weapon.
     */
    private static final Items<String> RULES = new Items<>()
    {
        @Override
        public Kind kind(String item)
        {
            if (item.endsWith("Ammo"))
                return Kind.AMMUNITION;
            if (item.endsWith("Gun"))
                return Kind.GUN;
            if (item.endsWith("Shield"))
                return Kind.SHIELD;
            return item.endsWith("Grenade") ? Kind.THROWABLE : Kind.OTHER;
        }

        @Override
        public boolean sameGun(String first, String second)
        {
            return first.equals(second);
        }

        @Override
        public boolean loads(String gun, String ammunition)
        {
            return ammunition.equals(gun.replace("Gun", "Ammo"));
        }
    };

    private static Slots<String> assign(String... items)
    {
        return FlanSoldierLoadout.assign(List.of(items), RULES);
    }

    @Test
    void theFirstGunIsHeldAndADifferentSecondGunStaysInTheInventory()
    {
        assertEquals(new Slots<>("rifleGun", null, "rifleAmmo", List.of("pistolGun", "pistolAmmo", "fragGrenade", "bandage")),
            assign("rifleGun", "rifleAmmo", "pistolGun", "pistolAmmo", "fragGrenade", "bandage"));
        assertEquals(new Slots<>("rifleGun", null, null, List.of("bandage", "pistolGun")), assign("bandage", "rifleGun", "pistolGun"));
    }

    @Test
    void theOffHandTakesASecondGunOfTheSameKindOrAShield()
    {
        assertEquals(new Slots<>("pistolGun", "pistolGun", "pistolAmmo", List.of("rifleGun", "pistolAmmo")), assign("pistolGun", "rifleGun", "pistolGun", "pistolAmmo", "pistolAmmo"));
        assertEquals(new Slots<>("pistolGun", "riotShield", null, List.of("rifleGun")), assign("pistolGun", "rifleGun", "riotShield"));
        // Whichever comes first takes the hand; the other is carried.
        assertEquals(new Slots<>("pistolGun", "riotShield", null, List.of("pistolGun")), assign("pistolGun", "riotShield", "pistolGun"));
        assertEquals(new Slots<>(null, "riotShield", null, List.of("knife")), assign("riotShield", "knife"));
    }

    @Test
    void theProjectileSlotTakesAmmunitionTheHeldGunLoadsAndOtherwiseAThrowable()
    {
        // Ammunition for another gun cannot reload the held one.
        assertEquals(new Slots<>("rifleGun", null, "rifleAmmo", List.of("pistolAmmo", "fragGrenade", "rifleAmmo")), assign("pistolAmmo", "rifleGun", "fragGrenade", "rifleAmmo", "rifleAmmo"));
        assertEquals(new Slots<>("rifleGun", null, "fragGrenade", List.of("pistolAmmo", "smokeGrenade")), assign("rifleGun", "pistolAmmo", "fragGrenade", "smokeGrenade"));
        // Without a gun nothing is reloaded, so the ammunition is only carried.
        assertEquals(new Slots<>(null, null, "fragGrenade", List.of("knife", "rifleAmmo")), assign("knife", "fragGrenade", "rifleAmmo"));
    }

    @Test
    void everythingElseIsCarriedInOrderUpToTheInventorySize()
    {
        assertEquals(new Slots<>(null, null, null, List.of()), assign());
        assertEquals(new Slots<>(null, null, null, List.of("knife", "bandage")), assign("knife", "bandage"));
        List<String> items = new ArrayList<>(List.of("rifleGun"));
        for (int i = 0; i < NpcAmmunition.INVENTORY_SLOTS + 3; i++)
            items.add("item" + i);
        assertEquals(items.subList(1, NpcAmmunition.INVENTORY_SLOTS + 1), FlanSoldierLoadout.assign(items, RULES).inventory());
    }
}
