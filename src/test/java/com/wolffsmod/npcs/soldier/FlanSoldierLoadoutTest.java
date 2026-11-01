package com.wolffsmod.npcs.soldier;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.WeaponKind;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout.Kind;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout.Slots;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FlanSoldierLoadoutTest
{
    /**
     * Starting items are named after what they are, so a test reads like the class definition it stands for: "rifleGun"
     * is loaded with "rifleAmmo", and two guns of the same name are the same weapon.
     */
    private static final FlanSoldierLoadout.Items<String> RULES = new FlanSoldierLoadout.Items<>()
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
            if (List.of("knife", "gladius", "spear", "sword", "bow", "moddedWeapon").contains(item))
                return Kind.WEAPON;
            if (item.equals("javelin") || item.equals("pilum"))
                return Kind.THROWN_WEAPON;
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
            return ammunition.equals(gun.equals("bow") ? "arrowAmmo" : gun.replace("Gun", "Ammo"));
        }
    };

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static Slots<String> assign(String... items)
    {
        return FlanSoldierLoadout.assign(List.of(items), RULES);
    }

    @Test
    void realVanillaWeaponsAreEligibleAndOrdinaryItemsAreCarried()
    {
        for (var item : List.of(Items.IRON_SWORD, Items.IRON_AXE, Items.BOW, Items.CROSSBOW, Items.TRIDENT))
            assertEquals(Kind.WEAPON, FlanSoldierLoadout.kind(new ItemStack(item)));
        assertEquals(Kind.SHIELD, FlanSoldierLoadout.kind(new ItemStack(Items.SHIELD)));
        assertEquals(Kind.OTHER, FlanSoldierLoadout.kind(new ItemStack(Items.BREAD)));
        for (var item : List.of(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW, Items.FIREWORK_ROCKET))
            assertEquals(Kind.AMMUNITION, FlanSoldierLoadout.kind(new ItemStack(item)));
    }

    @Test
    void flanMeleePropertiesEquipWeaponsButDeployablesStayInInventory()
    {
        ItemStack stack = new ItemStack(Items.STICK);
        EquippedWeaponProperties melee = mock(EquippedWeaponProperties.class);
        when(melee.meleeDamage()).thenReturn(7D);
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY)).thenReturn(Optional.of(melee));
            WeaponKind stackKind = new WeaponKind(melee.ranged(), melee.throwable(), melee.shield());
            equipment.when(() -> FlansEquipment.getWeaponKind(stack)).thenReturn(Optional.of(stackKind));
            assertEquals(Kind.WEAPON, FlanSoldierLoadout.kind(stack));
            equipment.when(() -> FlansEquipment.isDeployable(stack)).thenReturn(true);
            assertEquals(Kind.OTHER, FlanSoldierLoadout.kind(stack));
        }
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
        assertEquals(new Slots<>("knife", "riotShield", null, List.of()), assign("riotShield", "knife"));
    }

    @Test
    void theProjectileSlotTakesAmmunitionTheHeldGunLoadsAndOtherwiseAThrowable()
    {
        // Ammunition for another gun cannot reload the held one.
        assertEquals(new Slots<>("rifleGun", null, "rifleAmmo", List.of("pistolAmmo", "fragGrenade", "rifleAmmo")), assign("pistolAmmo", "rifleGun", "fragGrenade", "rifleAmmo", "rifleAmmo"));
        assertEquals(new Slots<>("rifleGun", null, "fragGrenade", List.of("pistolAmmo", "smokeGrenade")), assign("rifleGun", "pistolAmmo", "fragGrenade", "smokeGrenade"));
        // Without a gun nothing is reloaded, so the ammunition is only carried.
        assertEquals(new Slots<>("knife", null, "fragGrenade", List.of("rifleAmmo")), assign("knife", "fragGrenade", "rifleAmmo"));
        // A bow shoots from the projectile slot as a gun reloads from it.
        assertEquals(new Slots<>("bow", null, "arrowAmmo", List.of("knife", "arrowAmmo")), assign("bow", "knife", "arrowAmmo", "arrowAmmo"));
        assertEquals(new Slots<>("bow", null, "arrowAmmo", List.of("fragGrenade")), assign("fragGrenade", "bow", "arrowAmmo"));
    }

    @Test
    void addItemOrderChoosesBetweenFlanMeleeVanillaModdedWeaponsAndGuns()
    {
        assertEquals(new Slots<>("gladius", "scutumShield", "pilumGrenade", List.of("spear")), assign("gladius", "scutumShield", "pilumGrenade", "spear"));
        assertEquals(new Slots<>("spear", "scutumShield", null, List.of("gladius")), assign("spear", "scutumShield", "gladius"));
        for (String weapon : List.of("sword", "bow", "moddedWeapon", "gladius"))
        {
            assertEquals(new Slots<>(weapon, "riotShield", null, List.of("bandage", "rifleGun", "rifleAmmo")), assign("bandage", weapon, "rifleGun", "rifleAmmo", "riotShield"));
            assertEquals(new Slots<>("rifleGun", null, "rifleAmmo", List.of(weapon)), assign("rifleGun", weapon, "rifleAmmo"));
        }
        assertEquals(new Slots<>("bow", null, null, List.of("bow")), assign("bow", "bow"));
    }

    @Test
    void throwableGunsPreferTheProjectileSlotAndAdditionalOnesCanBeHeld()
    {
        assertEquals(new Slots<>("gladius", "scutumShield", "pilum", List.of("pilum")), assign("gladius", "scutumShield", "pilum", "pilum"));
        assertEquals(new Slots<>("gladius", "scutumShield", "javelin", List.of()), assign("javelin", "scutumShield", "gladius"));
        assertEquals(new Slots<>("javelin", "scutumShield", "javelin", List.of("gladius")), assign("javelin", "scutumShield", "javelin", "gladius"));
        assertEquals(new Slots<>(null, null, "javelin", List.of()), assign("javelin"));
        assertEquals(new Slots<>("javelin", null, "fragGrenade", List.of()), assign("fragGrenade", "javelin"));
    }

    @Test
    void everythingElseIsCarriedInOrderUpToTheInventorySize()
    {
        assertEquals(new Slots<>(null, null, null, List.of()), assign());
        assertEquals(new Slots<>(null, null, null, List.of("bandage", "food")), assign("bandage", "food"));
        List<String> items = new ArrayList<>(List.of("rifleGun"));
        for (int i = 0; i < NpcAmmunition.INVENTORY_SLOTS + 3; i++)
            items.add("item" + i);
        assertEquals(items.subList(1, NpcAmmunition.INVENTORY_SLOTS + 1), FlanSoldierLoadout.assign(items, RULES).inventory());
    }
}
