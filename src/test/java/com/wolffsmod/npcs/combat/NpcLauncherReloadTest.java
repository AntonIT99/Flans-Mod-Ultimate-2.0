package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NpcLauncherReloadTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @CsvSource({"MAIN_HAND,80,80", "MAIN_HAND,120,80", "MAIN_HAND,40,80", "OFF_HAND,80,80", "OFF_HAND,120,80", "OFF_HAND,40,80"})
    void reloadOverlapsCadenceInEitherHand(InteractionHand testedHand, double shotDelay, double reloadTime)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        when(npc.getTarget()).thenReturn(target);
        LookControl look = mock(LookControl.class);
        PathNavigation navigation = mock(PathNavigation.class);
        when(npc.getLookControl()).thenReturn(look);
        when(npc.getNavigation()).thenReturn(navigation);
        Sensing sensing = mock(Sensing.class);
        when(npc.getSensing()).thenReturn(sensing);
        when(sensing.hasLineOfSight(target)).thenReturn(true);
        when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
        when(target.getEyePosition()).thenReturn(new Vec3(12D, 0D, 0D));
        npc.stats = mock(DataStats.class);
        npc.stats.ranged = mock(DataRanged.class);
        when(npc.stats.ranged.getRange()).thenReturn(32);
        when(npc.isInRange(target, 32D)).thenReturn(true);
        ItemStack main = new ItemStack(Items.STICK);
        ItemStack off = new ItemStack(Items.BLAZE_ROD);
        when(npc.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(main);
        when(npc.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(off);
        when(npc.getOffhandItem()).thenReturn(off);
        EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
        when(properties.reloadTime()).thenReturn(reloadTime);
        var loaded = new EnumMap<InteractionHand, Boolean>(InteractionHand.class);
        var shots = new EnumMap<InteractionHand, List<Integer>>(InteractionHand.class);
        int[] tick = {0};
        try (var equipment = mockStatic(FlansEquipment.class);
            var options = mockStatic(NpcEquipment.class);
            var vehicles = mockStatic(NpcVehicleState.class);
            var attacks = mockStatic(NpcItemAttacks.class);
            var ammunition = mockStatic(NpcAmmunition.class);
            var feedback = mockStatic(NpcReloadFeedback.class);
            var tactics = mockStatic(NpcTacticState.class))
        {
            NpcVehicleState vehicle = mock(NpcVehicleState.class, RETURNS_DEEP_STUBS);
            vehicles.when(() -> NpcVehicleState.of(npc)).thenReturn(vehicle);
            tactics.when(() -> NpcTacticState.of(npc)).thenReturn(new NpcTacticState());
            Level level = mock(Level.class);
            when(npc.level()).thenReturn(level);
            options.when(() -> NpcEquipment.rangedWeapon(npc)).thenReturn(main);
            options.when(() -> NpcEquipment.offHandGun(npc)).thenReturn(true);
            equipment.when(() -> FlansEquipment.getWeaponProperties(main, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            for (InteractionHand hand : InteractionHand.values())
            {
                ItemStack weapon = hand == InteractionHand.MAIN_HAND ? main : off;
                loaded.put(hand, true);
                shots.put(hand, new ArrayList<>());
                options.when(() -> NpcEquipment.flan(npc, hand)).thenReturn(Optional.of(properties));
                options.when(() -> NpcEquipment.shotDelay(weapon)).thenReturn(shotDelay);
                equipment.when(() -> FlansEquipment.hasLoadedRound(weapon)).thenAnswer(call -> loaded.get(hand));
                attacks.when(() -> NpcItemAttacks.loadFlanGun(npc, hand)).thenAnswer(call ->
                {
                    loaded.put(hand, true);
                    return true;
                });
                var fire = attacks.when(() ->
                {
                    if (hand == InteractionHand.MAIN_HAND)
                        NpcItemAttacks.ranged(npc, target);
                    else
                        NpcItemAttacks.fireFlan(npc, target, hand);
                });
                fire.thenAnswer(call ->
                {
                    assertTrue(loaded.get(hand));
                    loaded.put(hand, false);
                    shots.get(hand).add(tick[0]);
                    return true;
                });
            }
            NpcEquipmentRangedGoal goal = new NpcEquipmentRangedGoal(npc);
            for (; tick[0] < 400; tick[0]++)
                goal.tick();
            List<Integer> fired = shots.get(testedHand);
            assertTrue(fired.size() >= 3);
            for (int i = 1; i < fired.size(); i++)
            {
                int interval = fired.get(i) - fired.get(i - 1);
                assertEquals((int) Math.max(shotDelay, reloadTime), interval);
            }
            feedback.verify(() -> NpcReloadFeedback.reload(npc, testedHand, (int) reloadTime), atLeast(2));
        }
    }
}
