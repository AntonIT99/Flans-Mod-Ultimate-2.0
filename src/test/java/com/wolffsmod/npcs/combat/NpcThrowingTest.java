package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.projectiles.DetonationRadii;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Throwing;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcThrowingTest
{
    private final NpcWeaponOptions options = new NpcWeaponOptions();
    private final NpcTuning tuning = new NpcTuning();

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private EntityNPCInterface npc()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        DataStats stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(((NpcWeaponSettings) stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        npc.stats = stats;
        npc.inventory = new DataInventory(npc);
        when(npc.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        return npc;
    }

    private static IItemStack wrapped(ItemStack stack)
    {
        IItemStack wrapper = mock(IItemStack.class);
        when(wrapper.getMCItemStack()).thenReturn(stack);
        return wrapper;
    }

    private static ItemStack grenade(MockedStatic<FlansProjectiles> projectiles, int count)
    {
        ItemStack stack = new ItemStack(Items.FIRE_CHARGE, count);
        projectiles.when(() -> FlansProjectiles.isGrenade(stack)).thenReturn(true);
        projectiles.when(() -> FlansProjectiles.isProjectile(stack)).thenReturn(true);
        return stack;
    }

    @Test
    void onlyTheOccasionalStrategiesThrowBesideOtherAttacksAndOnlyMainWeaponReachesIntoTheInventory()
    {
        for (Throwing strategy : Throwing.values())
        {
            assertEquals(strategy == Throwing.FREQUENT || strategy == Throwing.MODERATE || strategy == Throwing.SPARING || strategy == Throwing.CAUTIOUS, strategy.occasional(), strategy.name());
            assertEquals(strategy == Throwing.MAIN_WEAPON || strategy == Throwing.RANGED_SLOT, strategy.mainWeapon(true), strategy.name());
            assertEquals(strategy == Throwing.MAIN_WEAPON, strategy.mainWeapon(false), strategy.name());
        }
        NpcTuning defaults = new NpcTuning();
        assertEquals(Throwing.MODERATE, defaults.throwing(Value.GRENADE_USE));
        assertEquals(Throwing.MAIN_WEAPON, defaults.throwing(Value.THROWN_WEAPON_USE));
        assertEquals(Throwing.MAIN_WEAPON, defaults.throwing(Value.NATIVE_THROWABLE_USE));
    }

    @Test
    void aGrenadeThrownNowAndThenLeavesCustomNpcsWithNothingToFireWhileTheOldStrategyKeepsTheSlot()
    {
        try (var projectiles = mockStatic(FlansProjectiles.class))
        {
            EntityNPCInterface npc = npc();
            ItemStack slotted = grenade(projectiles, 1);
            npc.inventory.weapons.put(1, wrapped(slotted));
            assertTrue(NpcThrowing.mainProjectile(npc).isEmpty());
            assertTrue(NpcThrowing.mainThrownWeapon(npc).isEmpty());
            assertTrue(NpcThrowing.throwsOccasionally(npc));

            tuning.set(Value.GRENADE_USE, Throwing.RANGED_SLOT.ordinal());
            assertSame(slotted, NpcThrowing.mainProjectile(npc));
            assertSame(slotted, NpcThrowing.mainThrownWeapon(npc));

            // With the switch off the slot is Custom NPCs' projectile whatever the strategy.
            tuning.set(Value.GRENADE_USE, Throwing.MODERATE.ordinal());
            options.set(Feature.GRENADES, false);
            assertSame(slotted, NpcThrowing.mainProjectile(npc));
        }
    }

    @Test
    void mainWeaponTakesAGrenadeFromTheInventoryOnlyWhenTheProjectileSlotHasNone()
    {
        try (var projectiles = mockStatic(FlansProjectiles.class))
        {
            EntityNPCInterface npc = npc();
            ItemStack carried = grenade(projectiles, 2);
            npc.inventory.drops.put(4, wrapped(carried));
            tuning.set(Value.GRENADE_USE, Throwing.RANGED_SLOT.ordinal());
            assertTrue(NpcThrowing.mainThrownWeapon(npc).isEmpty());
            tuning.set(Value.GRENADE_USE, Throwing.MAIN_WEAPON.ordinal());
            assertSame(carried, NpcThrowing.mainThrownWeapon(npc));
            assertSame(carried, NpcThrowing.mainProjectile(npc));

            ItemStack bullet = new ItemStack(Items.IRON_NUGGET);
            projectiles.when(() -> FlansProjectiles.isProjectile(bullet)).thenReturn(true);
            npc.inventory.weapons.put(1, wrapped(bullet));
            // A round in the projectile slot is fired without a gun, before anything the inventory holds.
            assertSame(bullet, NpcThrowing.mainProjectile(npc));
        }
    }

    @Test
    void aGrenadeIsOnlyThrownFromBeyondItsReach()
    {
        try (var projectiles = mockStatic(FlansProjectiles.class))
        {
            ItemStack small = grenade(projectiles, 1);
            ItemStack frag = grenade(projectiles, 1);
            projectiles.when(() -> FlansProjectiles.getDetonationRadii(frag)).thenReturn(Optional.of(new DetonationRadii(3D, 5D, 16D, 0D)));
            // Without any reach the thrower still keeps 4 blocks off; half the fragments' 16 plus 2 makes 10.
            assertEquals(4D, NpcThrowing.safeDistance(small), 1.0E-9D);
            assertEquals(10D, NpcThrowing.safeDistance(frag), 1.0E-9D);
            // Cautious keeps out of the whole fragment radius with a wider margin, and at least 6 blocks off.
            assertEquals(6D, NpcThrowing.cautiousDistance(small), 1.0E-9D);
            assertEquals(19D, NpcThrowing.cautiousDistance(frag), 1.0E-9D);

            EntityNPCInterface npc = npc();
            LivingEntity target = mock(LivingEntity.class);
            when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
            when(target.getEyePosition()).thenReturn(new Vec3(8D, 0D, 0D));
            assertFalse(NpcThrowing.tooClose(npc, target, small));
            assertTrue(NpcThrowing.tooClose(npc, target, frag));
        }
    }

    @Test
    void onlyHarmfulNativeThrowablesAreTakenFromTheInventory()
    {
        assertTrue(NpcThrowing.offensive(new ItemStack(Items.FIRE_CHARGE)));
        assertFalse(NpcThrowing.offensive(new ItemStack(Items.EGG)));
        assertFalse(NpcThrowing.offensive(new ItemStack(Items.EXPERIENCE_BOTTLE)));
        assertFalse(NpcThrowing.offensive(new ItemStack(Items.SPLASH_POTION)));
        assertFalse(NpcThrowing.offensive(new ItemStack(Items.FIREWORK_ROCKET)));
        assertEquals(0D, NpcThrowing.hazard(new ItemStack(Items.SPLASH_POTION)), 1.0E-9D);
    }

    @Test
    void cooldownsAreSpreadByAQuarterAndOnlyOccasionalStrategiesHaveOne()
    {
        EntityNPCInterface npc = npc();
        when(npc.getRandom()).thenReturn(RandomSource.create(7L));
        for (int i = 0; i < 50; i++)
        {
            int cooldown = NpcThrowing.cooldown(npc, NpcThrowing.Kind.GRENADE);
            assertTrue(cooldown >= 225 && cooldown <= 375, Integer.toString(cooldown));
        }
        tuning.set(Value.THROWN_WEAPON_USE, Throwing.MAIN_WEAPON.ordinal());
        assertEquals(0, NpcThrowing.cooldown(npc, NpcThrowing.Kind.THROWN_WEAPON));
    }

    @Test
    void aUsedUpProjectileSlotTakesTheNextStackOfTheSameItemAndAnEmptiedInventorySlotIsCleared()
    {
        EntityNPCInterface npc = npc();
        ItemStack slotted = new ItemStack(Items.SNOWBALL);
        ItemStack other = new ItemStack(Items.EGG, 3);
        ItemStack spare = new ItemStack(Items.SNOWBALL, 5);
        IItemStack spareEntry = wrapped(spare);
        npc.inventory.weapons.put(1, wrapped(slotted));
        npc.inventory.drops.put(0, wrapped(other));
        npc.inventory.drops.put(2, spareEntry);
        npc.inventory.dropchance.put(2, 0.5F);
        NpcAmmunition.consume(npc, slotted);
        assertSame(spareEntry, npc.inventory.weapons.get(1));
        assertFalse(npc.inventory.drops.containsKey(2));
        assertFalse(npc.inventory.dropchance.containsKey(2));
        assertTrue(npc.updateClient);

        ItemStack last = new ItemStack(Items.EGG);
        npc.inventory.drops.put(5, wrapped(last));
        NpcAmmunition.consume(npc, last);
        assertFalse(npc.inventory.drops.containsKey(5));
        NpcAmmunition.consume(npc, other);
        assertEquals(2, other.getCount());
        assertTrue(npc.inventory.drops.containsKey(0));
    }
}
