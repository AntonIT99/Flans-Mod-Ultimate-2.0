package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcDualMeleeTest
{
    private final NpcWeaponOptions options = new NpcWeaponOptions();
    private final NpcCombatSettings combat = new NpcCombatSettings();
    private final NpcMeleeState state = new NpcMeleeState();
    private final ItemStack sword = new ItemStack(Items.IRON_SWORD);
    private final ItemStack axe = new ItemStack(Items.IRON_AXE);

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void defaultAndLegacySavesKeepMainHandMelee()
    {
        options.load(new CompoundTag());
        EntityNPCInterface npc = npc(sword, axe);
        Entity target = target();
        assertFalse(options.enabled(Feature.MELEE_DUAL_WIELD));
        assertTrue(NpcItemAttacks.melee(npc, target));
        assertTrue(NpcItemAttacks.melee(npc, target));
        verify(target, times(2)).hurt(any(DamageSource.class), eq(6F));
        assertEquals(InteractionHand.MAIN_HAND, NpcEquipment.meleeHand(npc));

        options.set(Feature.MELEE_DUAL_WIELD, true);
        CompoundTag saved = new CompoundTag();
        options.save(saved);
        NpcWeaponOptions restored = new NpcWeaponOptions();
        restored.load(saved);
        assertTrue(restored.enabled(Feature.MELEE_DUAL_WIELD));
    }

    @Test
    void alternatingStrikesUseSeparateDamageAndFullRecoveryWithoutCombiningWeapons()
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        EntityNPCInterface npc = npc(sword, axe);
        Entity target = target();
        assertSame(sword, NpcEquipment.meleeWeapon(npc));
        assertEquals(13, NpcEquipment.meleeDelay(NpcEquipment.meleeWeapon(npc)));
        assertTrue(NpcItemAttacks.melee(npc, target));
        assertSame(axe, NpcEquipment.meleeWeapon(npc));
        assertEquals(23, NpcEquipment.meleeDelay(NpcEquipment.meleeWeapon(npc)));
        assertTrue(NpcItemAttacks.melee(npc, target));
        assertSame(sword, NpcEquipment.meleeWeapon(npc));
        var order = inOrder(target);
        order.verify(target).hurt(any(DamageSource.class), eq(6F));
        order.verify(target).hurt(any(DamageSource.class), eq(9F));
    }

    @ParameterizedTest
    @CsvSource({"5,true,0", "4,true,16", "10,true,10", "5,false,15"})
    void slowHandCanFollowARecoveredFastHitWithoutClearingOtherImmunity(int elapsed, boolean sameVictim, int immunityAtHit)
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        ItemStack fast = sword.copy();
        fast.addAttributeModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier("test damage", 5D, AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
        fast.addAttributeModifier(Attributes.ATTACK_SPEED, new AttributeModifier("test speed", 0D, AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
        EntityNPCInterface npc = npc(fast, axe);
        Entity first = target();
        when(first.getId()).thenReturn(42);
        assertEquals(5, NpcEquipment.meleeDelay(fast));
        assertTrue(NpcItemAttacks.melee(npc, first));
        Entity next = sameVictim ? first : target();
        if (!sameVictim)
            when(next.getId()).thenReturn(77);
        next.invulnerableTime = 20 - elapsed;
        when(npc.level().getGameTime()).thenReturn((long) elapsed);
        when(next.hurt(any(), anyFloat())).thenAnswer(call ->
        {
            assertEquals(immunityAtHit, next.invulnerableTime);
            return true;
        });
        assertTrue(NpcItemAttacks.melee(npc, next));
    }

    @Test
    void emptyHandsShieldsAndNonWeaponsNeverBecomeTheSecondAttack()
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        state.finish(InteractionHand.MAIN_HAND);
        assertEquals(InteractionHand.MAIN_HAND, NpcEquipment.meleeHand(npc(sword, ItemStack.EMPTY)));
        assertEquals(InteractionHand.MAIN_HAND, NpcEquipment.meleeHand(npc(sword, new ItemStack(Items.SHIELD))));
        assertEquals(InteractionHand.MAIN_HAND, NpcEquipment.meleeHand(npc(sword, new ItemStack(Items.SNOWBALL))));
        assertEquals(InteractionHand.MAIN_HAND, NpcEquipment.meleeHand(npc(sword, new ItemStack(Items.STICK))));
        assertEquals(InteractionHand.OFF_HAND, NpcEquipment.meleeHand(npc(ItemStack.EMPTY, axe)));
        assertEquals(InteractionHand.OFF_HAND, NpcEquipment.meleeHand(npc(new ItemStack(Items.SHIELD), axe)));
        assertFalse(NpcEquipment.meleeAuthority(npc(new ItemStack(Items.STICK), new ItemStack(Items.SHIELD))));
        options.set(Feature.ITEM_WEAPONS, false);
        assertEquals(InteractionHand.MAIN_HAND, NpcEquipment.meleeHand(npc(sword, axe)));
    }

    @Test
    void oneOffHandWeaponKeepsAttackingWithoutAnEmptyHandPunchBetweenHits()
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        EntityNPCInterface npc = npc(ItemStack.EMPTY, axe);
        Entity target = target();
        assertTrue(NpcEquipment.meleeAuthority(npc));
        assertTrue(NpcItemAttacks.melee(npc, target));
        assertTrue(NpcItemAttacks.melee(npc, target));
        verify(target, times(2)).hurt(any(DamageSource.class), eq(9F));
    }

    @Test
    void flanMeleePropertiesIncludeGunMeleeButNeverTurnAShieldIntoAWeapon()
    {
        ItemStack gun = new ItemStack(Items.BLAZE_ROD);
        EquippedWeaponProperties gunProperties = mock(EquippedWeaponProperties.class);
        when(gunProperties.meleeDamage()).thenReturn(4D);
        EquippedWeaponProperties shieldProperties = mock(EquippedWeaponProperties.class);
        when(shieldProperties.shield()).thenReturn(true);
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.getWeaponProperties(gun, ItemStack.EMPTY)).thenReturn(Optional.of(gunProperties));
            equipment.when(() -> FlansEquipment.getWeaponProperties(sword, ItemStack.EMPTY)).thenReturn(Optional.of(shieldProperties));
            assertTrue(NpcEquipment.meleeWeapon(gun));
            // Even a shield with damaging item attributes must be left to blocking.
            assertFalse(NpcEquipment.meleeWeapon(sword));
        }
    }

    @Test
    void customMeleeValuesApplyToBothHandsWhileTheNativePathStillAlternates()
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        for (EquipmentProperty property : EquipmentProperty.Group.MELEE.properties())
            combat.setCustom(property, true);
        EntityNPCInterface npc = npc(sword, axe);
        Entity target = target();
        assertTrue(NpcEquipment.itemMelee(npc));
        assertFalse(NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_DELAY));
        assertTrue(NpcItemAttacks.melee(npc, target));
        assertEquals(InteractionHand.OFF_HAND, NpcEquipment.meleeHand(npc));
        assertTrue(NpcItemAttacks.melee(npc, target));
        verify(target, times(2)).hurt(any(DamageSource.class), eq(4F));
    }

    @Test
    void rejectedAndScriptCancelledStrikesConsumeTheirTurnWithoutExtraDamage()
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        EntityNPCInterface npc = npc(sword, axe);
        Entity rejected = target();
        when(rejected.hurt(any(), anyFloat())).thenReturn(false);
        assertFalse(NpcItemAttacks.melee(npc, rejected));
        assertEquals(InteractionHand.OFF_HAND, NpcEquipment.meleeHand(npc));

        LivingEntity cancelled = mock(LivingEntity.class);
        when(cancelled.isAlive()).thenReturn(true);
        when(cancelled.getMobType()).thenReturn(MobType.UNDEFINED);
        NpcAPI npcApi = mock(NpcAPI.class);
        try (var events = mockStatic(EventHooks.class); var api = mockStatic(NpcAPI.class))
        {
            api.when(NpcAPI::Instance).thenReturn(npcApi);
            events.when(() -> EventHooks.onNPCAttacksMelee(eq(npc), any())).thenReturn(true);
            assertFalse(NpcItemAttacks.melee(npc, cancelled));
            assertEquals(InteractionHand.MAIN_HAND, NpcEquipment.meleeHand(npc));
            verify(cancelled, never()).hurt(any(), anyFloat());
        }
    }

    @Test
    void offHandStrikeUsesItsEnchantmentsAndWearsOnlyItsOwnStack()
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        sword.enchant(Enchantments.KNOCKBACK, 2);
        axe.enchant(Enchantments.KNOCKBACK, 1);
        axe.enchant(Enchantments.SHARPNESS, 2);
        axe.enchant(Enchantments.FIRE_ASPECT, 2);
        EntityNPCInterface npc = npc(sword, axe);
        state.finish(InteractionHand.MAIN_HAND);
        LivingEntity target = mock(LivingEntity.class);
        when(target.isAlive()).thenReturn(true);
        when(target.getMobType()).thenReturn(MobType.UNDEFINED);
        when(target.getAllSlots()).thenReturn(List.of());
        when(target.hurt(any(), anyFloat())).thenReturn(true);
        NpcAPI npcApi = mock(NpcAPI.class);
        try (var events = mockStatic(EventHooks.class); var api = mockStatic(NpcAPI.class))
        {
            api.when(NpcAPI::Instance).thenReturn(npcApi);
            events.when(() -> EventHooks.onNPCAttacksMelee(eq(npc), any())).thenReturn(false);
            assertTrue(NpcItemAttacks.melee(npc, target));
        }
        verify(target).hurt(any(DamageSource.class), eq(10.5F));
        verify(target).knockback(0.5D, 0D, -1D);
        verify(target).setSecondsOnFire(8);
        verify(npc).setItemSlot(EquipmentSlot.OFFHAND, axe);
        verify(npc, never()).setItemSlot(eq(EquipmentSlot.MAINHAND), any());
        assertEquals(2, axe.getDamageValue());
        assertEquals(0, sword.getDamageValue());
    }

    @Test
    void flanMeleeFeedbackNamesOnlyTheStrikingHand()
    {
        options.set(Feature.MELEE_DUAL_WIELD, true);
        EntityNPCInterface npc = npc(sword, axe);
        EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.getWeaponProperties(sword, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            equipment.when(() -> FlansEquipment.getWeaponProperties(axe, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            NpcItemAttacks.meleeFeedback(npc);
            assertTrue(NpcItemAttacks.melee(npc, target()));
            NpcItemAttacks.meleeFeedback(npc);
            equipment.verify(() -> FlansEquipment.animateMelee(npc, InteractionHand.MAIN_HAND));
            equipment.verify(() -> FlansEquipment.animateMelee(npc, InteractionHand.OFF_HAND));
            equipment.verify(() -> FlansEquipment.playMeleeSound(npc, InteractionHand.MAIN_HAND));
            equipment.verify(() -> FlansEquipment.playMeleeSound(npc, InteractionHand.OFF_HAND));
        }
    }

    @Test
    void breakFeedbackFollowsTheAttackWithoutRedirectingArmorBreaks()
    {
        state.begin(InteractionHand.OFF_HAND);
        assertEquals(EquipmentSlot.OFFHAND, state.breakSlot(EquipmentSlot.MAINHAND));
        assertEquals(EquipmentSlot.HEAD, state.breakSlot(EquipmentSlot.HEAD));
        state.finish(InteractionHand.OFF_HAND);
        assertEquals(EquipmentSlot.MAINHAND, state.breakSlot(EquipmentSlot.MAINHAND));
    }

    private EntityNPCInterface npc(ItemStack main, ItemStack off)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcMeleeState.Holder.class));
        when(((NpcMeleeState.Holder) npc).wolffsmodnpcsMeleeState()).thenReturn(state);
        DataStats stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(((NpcWeaponSettings) stats).wolffsmodnpcsCombat()).thenReturn(combat);
        npc.stats = stats;
        npc.stats.melee = mock(DataMelee.class);
        when(npc.stats.melee.getStrength()).thenReturn(4);
        when(npc.stats.melee.getDelay()).thenReturn(20);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.getMainHandItem()).thenReturn(main);
        when(npc.getOffhandItem()).thenReturn(off);
        when(npc.getArmorSlots()).thenReturn(List.of());
        when(npc.getRandom()).thenReturn(RandomSource.create(1));
        DamageSources sources = mock(DamageSources.class);
        DamageSource damage = mock(DamageSource.class);
        when(npc.damageSources()).thenReturn(sources);
        when(sources.mobAttack(npc)).thenReturn(damage);
        return npc;
    }

    private static Entity target()
    {
        Entity target = mock(Entity.class);
        when(target.isAlive()).thenReturn(true);
        when(target.hurt(any(), anyFloat())).thenReturn(true);
        return target;
    }
}
