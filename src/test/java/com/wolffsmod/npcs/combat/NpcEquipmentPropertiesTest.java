package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcEquipmentPropertiesTest
{
    private final NpcWeaponOptions options = new NpcWeaponOptions();
    private final NpcCombatSettings combat = new NpcCombatSettings();

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private EntityNPCInterface npc(ItemStack mainHand, ItemStack offHand)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        DataStats stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(((NpcWeaponSettings) stats).wolffsmodnpcsCombat()).thenReturn(combat);
        npc.stats = stats;
        npc.inventory = new DataInventory(npc);
        when(npc.getMainHandItem()).thenReturn(mainHand);
        when(npc.getOffhandItem()).thenReturn(offHand);
        return npc;
    }

    private static EquippedWeaponProperties gun()
    {
        EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
        when(properties.ranged()).thenReturn(true);
        return properties;
    }

    @Test
    void anOffHandGunIsTheWeaponOnlyWithoutARangedWeaponInTheMainHand()
    {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack pistol = new ItemStack(Items.BLAZE_ROD);
        ItemStack rifle = new ItemStack(Items.STICK);
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            EquippedWeaponProperties gun = gun();
            equipment.when(() -> FlansEquipment.getWeaponProperties(pistol, ItemStack.EMPTY)).thenReturn(Optional.of(gun));
            equipment.when(() -> FlansEquipment.getWeaponProperties(rifle, ItemStack.EMPTY)).thenReturn(Optional.of(gun));
            EntityNPCInterface swordAndPistol = npc(sword, pistol);
            assertTrue(NpcEquipment.offHandPrimary(swordAndPistol));
            assertSame(pistol, NpcEquipment.rangedWeapon(swordAndPistol));
            assertTrue(NpcEquipment.weaponAuthority(swordAndPistol));
            // The gun is the NPC's only one, so it is not a second gun on top of it.
            assertFalse(NpcEquipment.offHandGun(swordAndPistol));
            EntityNPCInterface twoGuns = npc(rifle, pistol);
            assertFalse(NpcEquipment.offHandPrimary(twoGuns));
            assertSame(rifle, NpcEquipment.rangedWeapon(twoGuns));
            assertTrue(NpcEquipment.offHandGun(twoGuns));
            options.set(Feature.ITEM_WEAPONS, false);
            assertFalse(NpcEquipment.weaponAuthority(swordAndPistol));
        }
    }

    @Test
    void meleeIsTheItemsOnlyWithSomethingInTheMainHand()
    {
        assertFalse(NpcEquipment.meleeAuthority(npc(ItemStack.EMPTY, ItemStack.EMPTY)));
        EntityNPCInterface armed = npc(new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY);
        assertTrue(NpcEquipment.itemMelee(armed));
        // With every hit statistic its own, the NPC strikes as Custom NPCs does, while reach can still be the item's.
        combat.setCustom(EquipmentProperty.MELEE_DAMAGE, true);
        combat.setCustom(EquipmentProperty.MELEE_KNOCKBACK, true);
        combat.setCustom(EquipmentProperty.MELEE_EFFECT, true);
        assertFalse(NpcEquipment.itemMelee(armed));
        assertTrue(NpcEquipment.itemMelee(armed, EquipmentProperty.MELEE_REACH));
    }

    @Test
    void anOffHandMeleeWeaponChoosesMeleeInsteadOfTheProjectileSlotOnlyWhenOptedIn()
    {
        EntityNPCInterface npc = npc(ItemStack.EMPTY, new ItemStack(Items.IRON_SWORD));
        npc.inventory = mock(DataInventory.class);
        IItemStack projectile = mock(IItemStack.class);
        when(projectile.getMCItemStack()).thenReturn(new ItemStack(Items.ARROW));
        when(npc.inventory.getProjectile()).thenReturn(projectile);
        assertSame(projectile, NpcEquipment.combatProjectile(npc));
        options.set(Feature.MELEE_DUAL_WIELD, true);
        assertTrue(NpcEquipment.weaponAuthority(npc));
        assertFalse(NpcEquipment.fightsAtRange(npc));
        assertNull(NpcEquipment.combatProjectile(npc));
        options.set(Feature.ITEM_WEAPONS, false);
        assertSame(projectile, NpcEquipment.combatProjectile(npc));
    }

    @Test
    void eachKindOfHitIsReducedByArmorOnlyWhileItsResistanceIsInherited()
    {
        EntityNPCInterface npc = npc(ItemStack.EMPTY, ItemStack.EMPTY);
        for (NpcDamageCategory category : NpcDamageCategory.values())
            assertTrue(NpcEquipment.armorResists(npc, category), category.name());
        combat.setCustom(EquipmentProperty.ARMOR_MELEE, true);
        assertFalse(NpcEquipment.armorResists(npc, NpcDamageCategory.MELEE));
        assertTrue(NpcEquipment.armorResists(npc, NpcDamageCategory.PROJECTILE));
        // Damage of no resisted kind follows the switch alone.
        assertTrue(NpcEquipment.armorResists(npc, NpcDamageCategory.OTHER));
        options.set(Feature.ITEM_ARMOR, false);
        for (NpcDamageCategory category : NpcDamageCategory.values())
            assertFalse(NpcEquipment.armorResists(npc, category), category.name());
    }

    @Test
    void ownResistancesAreEditedAsTheShareAbsorbedAndClampedLikeTheSliders()
    {
        EntityNPCInterface npc = npc(ItemStack.EMPTY, ItemStack.EMPTY);
        npc.stats.resistances = new noppes.npcs.Resistances();
        assertEquals(net.minecraft.nbt.FloatTag.valueOf(0F), EquipmentValues.read(npc, EquipmentProperty.ARMOR_MELEE));
        EquipmentValues.write(npc, EquipmentProperty.ARMOR_MELEE, net.minecraft.nbt.FloatTag.valueOf(40F));
        assertEquals(1.4F, npc.stats.resistances.melee, 1.0E-6F);
        // Beyond immune is still immune, and below doubled damage still doubled.
        EquipmentValues.write(npc, EquipmentProperty.ARMOR_EXPLOSION, net.minecraft.nbt.FloatTag.valueOf(250F));
        assertEquals(2F, npc.stats.resistances.explosion, 1.0E-6F);
        EquipmentValues.write(npc, EquipmentProperty.ARMOR_PROJECTILE, net.minecraft.nbt.FloatTag.valueOf(-300F));
        assertEquals(0F, npc.stats.resistances.arrow, 1.0E-6F);
        assertEquals(EquipmentValues.Kind.NONE, EquipmentValues.kind(EquipmentProperty.FIRING_BURST));
        assertNull(EquipmentValues.read(npc, EquipmentProperty.MELEE_EFFECT));
    }

    @Test
    void theNpcsOwnRangedValuesReplaceOnlyTheStatisticsItKeeps()
    {
        EntityNPCInterface npc = npc(ItemStack.EMPTY, ItemStack.EMPTY);
        npc.stats.ranged = mock(noppes.npcs.entity.data.DataRanged.class);
        when(npc.stats.ranged.getStrength()).thenReturn(12);
        when(npc.stats.ranged.getAccuracy()).thenReturn(90);
        when(npc.stats.ranged.getSpeed()).thenReturn(35);
        when(npc.stats.ranged.getShotCount()).thenReturn(40);
        assertTrue(NpcEquipment.rangedOverrides(npc, InteractionHand.MAIN_HAND).isEmpty());
        combat.setCustom(EquipmentProperty.RANGED_DAMAGE, true);
        combat.setCustom(EquipmentProperty.RANGED_ACCURACY, true);
        combat.setCustom(EquipmentProperty.RANGED_SPEED, true);
        combat.setCustom(EquipmentProperty.RANGED_SHOT_COUNT, true);
        var overrides = NpcEquipment.rangedOverrides(npc, InteractionHand.MAIN_HAND);
        assertEquals(12D, overrides.damage().orElseThrow());
        assertEquals(2D, overrides.spread().orElseThrow(), 1.0E-9D);
        assertEquals(3.5D, overrides.speed().orElseThrow(), 1.0E-9D);
        // Custom NPCs never fires more than ten projectiles a shot.
        assertEquals(10, overrides.projectiles().orElseThrow());
        // Each hand chooses for itself, and both fire with the same NPC values when they keep their own.
        assertTrue(NpcEquipment.rangedOverrides(npc, InteractionHand.OFF_HAND).isEmpty());
        combat.setCustom(EquipmentProperty.OFF_HAND_DAMAGE, true);
        var offHand = NpcEquipment.rangedOverrides(npc, InteractionHand.OFF_HAND);
        assertEquals(12D, offHand.damage().orElseThrow());
        assertTrue(offHand.spread().isEmpty());
        assertEquals(EquipmentProperty.RANGED_DAMAGE, EquipmentProperty.OFF_HAND_DAMAGE.mainHand());
        assertEquals(EquipmentProperty.OFF_HAND_SOUND, EquipmentProperty.FIRING_SOUND.inHand(InteractionHand.OFF_HAND));
        // Without Use equipped weapons nothing is inherited, but the item does not fire either.
        options.set(Feature.ITEM_WEAPONS, false);
        assertFalse(NpcEquipment.inherits(npc, EquipmentProperty.RANGED_DAMAGE));
    }
}
