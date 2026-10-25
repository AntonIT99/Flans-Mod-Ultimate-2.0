package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.FlansMelee;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcMeleeTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @CsvSource({"true,5,20,0", "true,10,5,18", "false,5,20,18", "false,20,5,0"})
    void onlyTheEffectiveFastCadenceBypassesHurtImmunity(boolean itemDelay, int weaponDelay, int ownDelay, int immunityAtHit)
    {
        EntityNPCInterface npc = npc(ownDelay);
        Entity target = mock(Entity.class);
        when(target.isAlive()).thenReturn(true);
        target.invulnerableTime = 18;
        try (var equipment = mockStatic(NpcEquipment.class))
        {
            equipment.when(() -> NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_DELAY)).thenReturn(itemDelay);
            equipment.when(() -> NpcEquipment.meleeDelay(npc.getMainHandItem())).thenReturn(weaponDelay);
            when(target.hurt(any(DamageSource.class), eq(4F))).thenAnswer(call ->
            {
                assertEquals(immunityAtHit, target.invulnerableTime);
                // A successful vanilla hit starts a new immunity window, which must survive the strike.
                target.invulnerableTime = 20;
                return true;
            });

            assertTrue(NpcItemAttacks.melee(npc, target));
            assertEquals(20, target.invulnerableTime);
            verify(npc).setLastHurtMob(target);
        }
    }

    @Test
    void aRejectedFastStrikeDoesNotLeaveTheVictimVulnerable()
    {
        EntityNPCInterface npc = npc(5);
        Entity target = mock(Entity.class);
        when(target.isAlive()).thenReturn(true);
        target.invulnerableTime = 18;
        try (var equipment = mockStatic(NpcEquipment.class))
        {
            equipment.when(() -> NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_DELAY)).thenReturn(false);
            when(target.hurt(any(DamageSource.class), anyFloat())).thenAnswer(call ->
            {
                assertEquals(0, target.invulnerableTime);
                return false;
            });
            assertFalse(NpcItemAttacks.melee(npc, target));
            assertEquals(18, target.invulnerableTime);
            verify(npc, never()).setLastHurtMob(any());
        }
    }

    @Test
    void npcWeaponHitsCarryVehicleDamage()
    {
        GunType type = mock(GunType.class);
        GunItem gun = mock(GunItem.class);
        when(gun.getConfigType()).thenReturn(type);
        ItemStack weapon = mock(ItemStack.class);
        when(weapon.getItem()).thenReturn(gun);
        when(weapon.copy()).thenReturn(weapon);
        when(type.getMeleeDamage(weapon, false)).thenReturn(4F);
        when(type.getMeleeDamage(weapon, true)).thenReturn(24F);
        EntityNPCInterface npc = npc(20);
        when(npc.getMainHandItem()).thenReturn(weapon);
        when(npc.damageSources().mobAttack(npc)).thenReturn(new DamageSource(Holder.direct(new DamageType("mob", 0F)), npc));
        Entity target = mock(Entity.class);
        when(target.isAlive()).thenReturn(true);
        try (var equipment = mockStatic(NpcEquipment.class))
        {
            when(target.hurt(any(DamageSource.class), eq(4F))).thenAnswer(call ->
            {
                DamageSource source = call.getArgument(0);
                assertSame(npc, source.getEntity());
                assertEquals(24F, FlansMelee.againstVehicle(source, call.getArgument(1)));
                return false;
            });
            assertFalse(NpcItemAttacks.melee(npc, target));
            verify(target).hurt(any(DamageSource.class), eq(4F));
        }
    }

    @Test
    void aScriptCancelledStrikeNeverClearsHurtImmunity()
    {
        EntityNPCInterface npc = npc(5);
        LivingEntity target = mock(LivingEntity.class);
        when(target.isAlive()).thenReturn(true);
        target.invulnerableTime = 18;
        NpcAPI npcApi = mock(NpcAPI.class);
        try (var equipment = mockStatic(NpcEquipment.class); var events = mockStatic(EventHooks.class); var api = mockStatic(NpcAPI.class))
        {
            equipment.when(() -> NpcEquipment.inherits(npc, EquipmentProperty.MELEE_DAMAGE)).thenReturn(false);
            api.when(NpcAPI::Instance).thenReturn(npcApi);
            events.when(() -> EventHooks.onNPCAttacksMelee(eq(npc), any())).thenReturn(true);
            assertFalse(NpcItemAttacks.melee(npc, target));
            assertEquals(18, target.invulnerableTime);
            verify(target, never()).hurt(any(), anyFloat());
        }
    }

    private static EntityNPCInterface npc(int delay)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.getMainHandItem()).thenReturn(new ItemStack(Items.STICK));
        npc.stats = mock(DataStats.class);
        npc.stats.melee = mock(DataMelee.class);
        when(npc.stats.melee.getStrength()).thenReturn(4);
        when(npc.stats.melee.getDelay()).thenReturn(delay);
        DamageSources sources = mock(DamageSources.class);
        when(npc.damageSources()).thenReturn(sources);
        DamageSource source = mock(DamageSource.class);
        when(sources.mobAttack(npc)).thenReturn(source);
        return npc;
    }
}
