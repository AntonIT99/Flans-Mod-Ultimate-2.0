package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.tactics.NpcInfantryAirDefense;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NpcSmallArmsArmorTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @EnumSource(InteractionHand.class)
    void heldGunsCheckTheLoadedBeltRoundAgainstKnownVehicleArmor(InteractionHand hand)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity tank = mock(LivingEntity.class);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(tank.level()).thenReturn(level);
        when(tank.isAlive()).thenReturn(true);
        when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
        when(tank.getEyePosition()).thenReturn(new Vec3(0D, 0D, 30D));
        ItemStack weapon = new ItemStack(Items.STICK);
        ItemStack ammunition = new ItemStack(Items.IRON_NUGGET);
        when(npc.getItemInHand(hand)).thenReturn(weapon);
        try (var equipment = mockStatic(FlansEquipment.class);
            var projectiles = mockStatic(FlansProjectiles.class);
            var options = mockStatic(NpcEquipment.class);
            var threats = mockStatic(NpcVehicleThreats.class);
            var crew = mockStatic(NpcCrew.class);
            var airDefense = mockStatic(NpcInfantryAirDefense.class);
            var blastSafety = mockStatic(NpcBlastSafety.class);
            var skill = mockStatic(NpcSkill.class))
        {
            equipment.when(() -> FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY)).thenReturn(Optional.of(mock(EquippedWeaponProperties.class)));
            equipment.when(() -> FlansEquipment.getLoadedAmmunition(weapon)).thenReturn(Optional.of(ammunition));
            equipment.when(() -> FlansEquipment.getLoadedRoundIndex(weapon)).thenReturn(3);
            equipment.when(() -> FlansEquipment.fire(eq(npc), eq(hand), any(), any(), anyBoolean(), anyBoolean(), any())).thenReturn(true);
            options.when(() -> NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.ADAPTIVE_VEHICLE_COMBAT)).thenReturn(true);
            options.when(() -> NpcEquipment.inherits(npc, EquipmentProperty.FIRING_SOUND.inHand(hand))).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.groundVehicle(tank)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.platform(tank)).thenReturn(tank);
            threats.when(() -> NpcVehicleThreats.protection(eq(tank), eq(Vec3.ZERO), anyString())).thenReturn(Optional.of(new PartArmor(80F, 80F)));
            skill.when(() -> NpcSkill.aimError(npc, tank)).thenReturn(NpcSkill.AimError.NONE);
            projectiles.when(() -> FlansProjectiles.getArmorPerformance(ammunition, 3)).thenReturn(Optional.of(new AmmunitionArmor(20D, false, 12D)));

            assertFalse(NpcItemAttacks.fireFlan(npc, tank, hand));
            equipment.verify(() -> FlansEquipment.fire(eq(npc), eq(hand), any(), any(), anyBoolean(), anyBoolean(), any()), never());

            projectiles.when(() -> FlansProjectiles.getArmorPerformance(ammunition, 3)).thenReturn(Optional.of(new AmmunitionArmor(100D, false, 12D)));
            assertTrue(NpcItemAttacks.fireFlan(npc, tank, hand), "a penetrating round in the same belt may fire");

            projectiles.when(() -> FlansProjectiles.getArmorPerformance(ammunition, 3)).thenReturn(Optional.empty());
            assertTrue(NpcItemAttacks.fireFlan(npc, tank, hand), "unknown ammunition remains eligible");

            projectiles.when(() -> FlansProjectiles.getArmorPerformance(ammunition, 3)).thenReturn(Optional.of(new AmmunitionArmor(20D, false, 12D)));
            options.when(() -> NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.ADAPTIVE_VEHICLE_COMBAT)).thenReturn(false);
            assertTrue(NpcItemAttacks.fireFlan(npc, tank, hand), "the existing combat toggle still controls the firing policy");
        }
    }
}
