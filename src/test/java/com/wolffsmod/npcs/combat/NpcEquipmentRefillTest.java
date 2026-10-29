package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.item.ItemStackData;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcEquipmentRefillTest
{
    private static final class Inventory implements AutoCloseable
    {
        private final EntityNPCInterface npc = mock(EntityNPCInterface.class);
        private final MockedStatic<FlansEquipment> equipment = mockStatic(FlansEquipment.class);
        private final MockedStatic<NpcEquipment> options = mockStatic(NpcEquipment.class);

        private Inventory()
        {
            ServerLevel level = mock(ServerLevel.class);
            when(npc.level()).thenReturn(level);
            when(npc.isAlive()).thenReturn(true);
            npc.inventory = new DataInventory(npc);
            when(npc.getMainHandItem()).thenAnswer(call -> stack(npc.inventory.weapons.get(0)));
            options.when(() -> NpcEquipment.enabled(eq(npc), any())).thenReturn(true);
            // As by default, Flan ammunition in the projectile slot is loaded whether or not it fits.
            options.when(() -> NpcEquipment.enabled(npc, Feature.FITTING_SLOT_AMMO)).thenReturn(false);
        }

        private ItemStack weapon(String name, boolean throwable)
        {
            ItemStack stack = new ItemStack(Items.STICK);
            ItemStackData.update(stack, tag -> tag.putString("Weapon", name));
            EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
            when(properties.ranged()).thenReturn(true);
            when(properties.throwable()).thenReturn(throwable);
            equipment.when(() -> FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            return stack;
        }

        private IItemStack carried(int slot, ItemStack stack)
        {
            IItemStack entry = wrapped(stack);
            npc.inventory.drops.put(slot, entry);
            npc.inventory.dropchance.put(slot, 0.25F);
            return entry;
        }

        private void equipped(int slot, ItemStack stack)
        {
            npc.inventory.weapons.put(slot, wrapped(stack));
        }

        @Override
        public void close()
        {
            options.close();
            equipment.close();
        }
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static IItemStack wrapped(ItemStack stack)
    {
        IItemStack wrapper = mock(IItemStack.class);
        when(wrapper.getMCItemStack()).thenReturn(stack);
        return wrapper;
    }

    private static ItemStack stack(IItemStack wrapper)
    {
        return wrapper == null ? ItemStack.EMPTY : wrapper.getMCItemStack();
    }

    @Test
    void emptySlotsTakeTheFirstUsableGunAndItsAmmunitionWithoutCopyingInventory()
    {
        try (Inventory inventory = new Inventory())
        {
            ItemStack dry = inventory.weapon("dry", false);
            ItemStack gun = inventory.weapon("rifle", false);
            ItemStack ammunition = new ItemStack(Items.IRON_NUGGET, 4);
            inventory.carried(0, dry);
            IItemStack gunEntry = inventory.carried(2, gun);
            IItemStack ammoEntry = inventory.carried(5, ammunition);
            inventory.equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, ammunition)).thenReturn(true);
            assertTrue(NpcAmmunition.refillEmptySlots(inventory.npc));
            assertSame(gunEntry, inventory.npc.inventory.weapons.get(0));
            assertSame(ammoEntry, inventory.npc.inventory.weapons.get(1));
            assertFalse(inventory.npc.inventory.drops.containsKey(2));
            assertFalse(inventory.npc.inventory.drops.containsKey(5));
            assertFalse(inventory.npc.inventory.dropchance.containsKey(2));
            assertFalse(inventory.npc.inventory.dropchance.containsKey(5));
            assertSame(dry, stack(inventory.npc.inventory.drops.get(0)));
            assertEquals(4, ammunition.getCount());
            assertEquals("rifle", ItemStackData.copy(gun).getString("Weapon"));
            assertTrue(inventory.npc.updateClient);
            assertFalse(inventory.npc.updateAI);
        }
    }

    @Test
    void infiniteAmmunitionCopiesTheProjectileSlotOrElseTheGunsDefaultAmmunition()
    {
        try (Inventory inventory = new Inventory())
        {
            inventory.options.when(() -> NpcEquipment.enabled(inventory.npc, Feature.CONSUME_AMMO)).thenReturn(false);
            ItemStack gun = inventory.weapon("rifle", false);
            ItemStack slotted = new ItemStack(Items.IRON_NUGGET);
            ItemStack issued = new ItemStack(Items.GOLD_NUGGET, 16);
            inventory.equipped(1, slotted);
            inventory.equipment.when(() -> FlansEquipment.createDefaultAmmunition(gun)).thenReturn(Optional.of(issued));
            inventory.equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, issued)).thenReturn(true);
            assertSame(issued, NpcAmmunition.infiniteTemplate(inventory.npc, gun).orElseThrow());
            assertFalse(NpcAmmunition.nothingToLoad(inventory.npc, gun));

            // What the projectile slot holds may be loaded by choice, which the three-argument check decides.
            inventory.equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, slotted, true)).thenReturn(true);
            assertSame(slotted, NpcAmmunition.infiniteTemplate(inventory.npc, gun).orElseThrow());

            inventory.equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, slotted, true)).thenReturn(false);
            inventory.equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, issued)).thenReturn(false);
            assertTrue(NpcAmmunition.infiniteTemplate(inventory.npc, gun).isEmpty());
            assertTrue(NpcAmmunition.nothingToLoad(inventory.npc, gun));
        }
    }

    @Test
    void theProjectileSlotRefillsAThrowableWithoutReplacingTheHeldSwordOrShield()
    {
        try (Inventory inventory = new Inventory())
        {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            ItemStack shield = new ItemStack(Items.SHIELD);
            inventory.equipped(0, sword);
            inventory.equipped(2, shield);
            ItemStack javelin = inventory.weapon("javelin", true);
            IItemStack entry = inventory.carried(3, javelin);
            assertTrue(NpcAmmunition.refillEmptySlots(inventory.npc));
            assertSame(sword, inventory.npc.getMainHandItem());
            assertSame(shield, stack(inventory.npc.inventory.weapons.get(2)));
            assertSame(entry, inventory.npc.inventory.weapons.get(1));
            assertTrue(inventory.npc.inventory.drops.isEmpty());
        }
    }

    @Test
    void compatibleAmmunitionTakesPriorityOverAnEarlierThrowable()
    {
        try (Inventory inventory = new Inventory())
        {
            ItemStack gun = inventory.weapon("rifle", false);
            ItemStack ammunition = new ItemStack(Items.IRON_NUGGET);
            ItemStack javelin = inventory.weapon("javelin", true);
            inventory.equipped(0, gun);
            inventory.carried(0, javelin);
            IItemStack ammoEntry = inventory.carried(4, ammunition);
            inventory.equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, ammunition)).thenReturn(true);
            assertTrue(NpcAmmunition.refillEmptySlots(inventory.npc));
            assertSame(ammoEntry, inventory.npc.inventory.weapons.get(1));
            assertSame(javelin, stack(inventory.npc.inventory.drops.get(0)));
        }
    }

    @Test
    void aGunCanUseExistingProjectileAmmunitionAndALoadedGunNeedsNoSpareMagazine()
    {
        for (boolean loaded : new boolean[]{false, true})
        {
            try (Inventory inventory = new Inventory())
            {
                ItemStack gun = inventory.weapon("rifle", false);
                IItemStack entry = inventory.carried(1, gun);
                if (loaded)
                    inventory.equipment.when(() -> FlansEquipment.hasLoadedRound(gun)).thenReturn(true);
                else
                {
                    ItemStack ammunition = new ItemStack(Items.IRON_NUGGET);
                    inventory.equipped(1, ammunition);
                    inventory.equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, ammunition, true)).thenReturn(true);
                }
                assertTrue(NpcAmmunition.refillEmptySlots(inventory.npc));
                assertSame(entry, inventory.npc.inventory.weapons.get(0));
                assertTrue(inventory.npc.inventory.drops.isEmpty());
            }
        }
    }

    @Test
    void throwableWeaponsCanRefillBothEmptySlotsInInventoryOrder()
    {
        try (Inventory inventory = new Inventory())
        {
            ItemStack first = inventory.weapon("javelin", true);
            ItemStack second = inventory.weapon("pilum", true);
            inventory.carried(1, first);
            inventory.carried(3, second);
            assertTrue(NpcAmmunition.refillEmptySlots(inventory.npc));
            assertSame(first, inventory.npc.getMainHandItem());
            assertSame(second, stack(inventory.npc.inventory.weapons.get(1)));
            assertTrue(inventory.npc.inventory.drops.isEmpty());
        }
    }

    @Test
    void disabledConsumptionOrEquipmentAuthorityLeavesInventoryAlone()
    {
        for (Feature disabled : new Feature[]{Feature.CONSUME_AMMO, Feature.ITEM_WEAPONS})
        {
            try (Inventory inventory = new Inventory())
            {
                inventory.carried(0, inventory.weapon("javelin", true));
                inventory.options.when(() -> NpcEquipment.enabled(inventory.npc, disabled)).thenReturn(false);
                assertFalse(NpcAmmunition.refillEmptySlots(inventory.npc));
                assertTrue(inventory.npc.inventory.weapons.isEmpty());
                assertEquals(1, inventory.npc.inventory.drops.size());
            }
        }
    }

    @Test
    void dryOrDeployableGunsAndOrdinaryItemsCannotFillEmptySlots()
    {
        try (Inventory inventory = new Inventory())
        {
            ItemStack dry = inventory.weapon("dry", false);
            ItemStack deployable = inventory.weapon("deployable", false);
            inventory.carried(0, dry);
            inventory.carried(1, deployable);
            inventory.carried(2, new ItemStack(Items.BREAD));
            inventory.equipment.when(() -> FlansEquipment.hasLoadedRound(deployable)).thenReturn(true);
            inventory.equipment.when(() -> FlansEquipment.isDeployable(deployable)).thenReturn(true);
            assertFalse(NpcAmmunition.refillEmptySlots(inventory.npc));
            assertTrue(inventory.npc.inventory.weapons.isEmpty());
            assertEquals(3, inventory.npc.inventory.drops.size());
        }
    }

    // Plain JUnit must open the item registry that NeoForge's loader normally fills; NeoForge keeps its intrusive holders.
    @SuppressWarnings("deprecation")
    private static BulletItem registerAmmo(BulletType type)
    {
        MappedRegistry<Item> registry = (MappedRegistry<Item>) BuiltInRegistries.ITEM;
        registry.unfreeze();
        try
        {
            BulletItem item = new BulletItem(type);
            Registry.register(registry, ResourceLocation.fromNamespaceAndPath("flansmodultimate", "test_ammo_" + UUID.randomUUID()), item);
            return item;
        }
        finally
        {
            registry.freeze();
        }
    }

    @Test
    void finalMagazineRoundImmediatelyRefillsFromUsableReserves()
    {
        try (Inventory inventory = new Inventory())
        {
            BulletType type = mock(BulletType.class);
            when(type.getRoundsPerItem()).thenReturn(3);
            when(type.getMaxStackSize()).thenReturn(16);
            BulletItem item = registerAmmo(type);
            ItemStack used = new ItemStack(item);
            ShootableItem.setRoundsRemaining(used, 1);
            ItemStack exhausted = used.copy();
            ShootableItem.setRoundsRemaining(exhausted, 0);
            ItemStack reserve = new ItemStack(item);
            inventory.equipped(1, used);
            inventory.carried(0, exhausted);
            IItemStack replacement = inventory.carried(2, reserve);
            NpcAmmunition.consume(inventory.npc, used);
            assertTrue(used.isEmpty());
            assertSame(replacement, inventory.npc.inventory.weapons.get(1));
            assertEquals(3, ShootableItem.getTotalRounds(reserve));
            assertSame(exhausted, stack(inventory.npc.inventory.drops.get(0)));
            assertFalse(inventory.npc.inventory.drops.containsKey(2));
        }
    }

    @Test
    void stackedMagazinesConsumeExactlyTheirRoundsAndSingleRoundsStillShrinkNormally()
    {
        for (int rounds : new int[]{1, 3})
        {
            try (Inventory inventory = new Inventory())
            {
                BulletType type = mock(BulletType.class);
                when(type.getRoundsPerItem()).thenReturn(rounds);
                when(type.getMaxStackSize()).thenReturn(16);
                ItemStack used = new ItemStack(registerAmmo(type), 2);
                inventory.equipped(1, used);
                for (int remaining = 2 * rounds; remaining > 0; remaining--)
                {
                    assertTrue(FlansProjectiles.hasRoundsLeft(used));
                    assertEquals(remaining, ShootableItem.getTotalRounds(used));
                    NpcAmmunition.consume(inventory.npc, used);
                }
                assertFalse(FlansProjectiles.hasRoundsLeft(used));
                assertTrue(used.isEmpty());
                assertFalse(inventory.npc.inventory.weapons.containsKey(1));
            }
        }
    }

    @Test
    void anAlreadyExhaustedMagazineIsRetiredWithoutConsumingItsReplacement()
    {
        try (Inventory inventory = new Inventory())
        {
            BulletType type = mock(BulletType.class);
            when(type.getRoundsPerItem()).thenReturn(3);
            BulletItem item = registerAmmo(type);
            ItemStack used = new ItemStack(item);
            ShootableItem.setRoundsRemaining(used, 0);
            ItemStack reserve = new ItemStack(item);
            inventory.equipped(1, used);
            IItemStack replacement = inventory.carried(0, reserve);
            assertSame(reserve, NpcAmmunition.modelTemplate(inventory.npc));
            assertSame(replacement, inventory.npc.inventory.weapons.get(1));
            assertEquals(3, ShootableItem.getTotalRounds(reserve));
        }
    }
}
