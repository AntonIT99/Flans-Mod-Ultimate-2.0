package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponChoice;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotReadinessTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void casualtiesInjuriesEmptyWeaponsAndPinningReduceAssaultReadinessWithoutConsumingAmmo()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        when(npc.getMaxHealth()).thenReturn(20F);
        when(npc.getHealth()).thenReturn(20F);
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(new NpcTacticState());
        ItemStack gun = mock(ItemStack.class);
        EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
        try (var equipment = mockStatic(NpcEquipment.class);
            var weapons = mockStatic(FlansEquipment.class);
            var ammunition = mockStatic(NpcAmmunition.class);
            var choices = mockStatic(NpcWeaponChoice.class);
            var suppression = mockStatic(NpcSuppression.class))
        {
            equipment.when(() -> NpcEquipment.rangedWeapon(npc)).thenReturn(gun);
            weapons.when(() -> FlansEquipment.getWeaponProperties(gun, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            weapons.when(() -> FlansEquipment.hasLoadedRound(gun)).thenReturn(true);
            choices.when(() -> NpcWeaponChoice.antiArmor(npc, gun)).thenReturn(true);
            assertEquals(1D, BotReadiness.measure(List.of(npc), 1, BotRole.INFANTRY, 0L).readiness());
            assertEquals(0.25D, BotReadiness.measure(List.of(npc), 4, BotRole.INFANTRY, 0L).readiness());
            when(npc.getHealth()).thenReturn(10F);
            assertEquals(0.5D, BotReadiness.measure(List.of(npc), 1, BotRole.INFANTRY, 0L).readiness());
            weapons.when(() -> FlansEquipment.hasLoadedRound(gun)).thenReturn(false);
            ammunition.when(() -> NpcAmmunition.nothingToLoad(npc, gun)).thenReturn(true);
            assertEquals(0.075D, BotReadiness.measure(List.of(npc), 1, BotRole.INFANTRY, 0L).readiness(), 0.001D);
            suppression.when(() -> NpcSuppression.isPinned(npc)).thenReturn(true);
            var pinned = BotReadiness.measure(List.of(npc), 1, BotRole.INFANTRY, 0L);
            assertTrue(pinned.readiness() < 0.075D);
            assertTrue(pinned.antiArmour() > 0D, "a weakened anti-tank soldier retains its weapon suitability");
            ammunition.verify(() -> NpcAmmunition.load(any(), any()), never());
        }
    }
}
