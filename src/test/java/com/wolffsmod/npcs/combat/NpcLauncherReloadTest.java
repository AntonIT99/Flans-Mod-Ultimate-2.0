package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.WeaponKind;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    void reloadOverlapsCadenceInEitherHand(InteractionHand testedHand, double shotDelay, double reloadTime) throws ReflectiveOperationException
    {
        LivingEntity target = mock(LivingEntity.class);
        EntityNPCInterface npc = engagedNpc(target);
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
            options.when(() -> NpcEquipment.rangedWeapon(npc)).thenReturn(main);
            options.when(() -> NpcEquipment.ranged(npc)).thenReturn(true);
            options.when(() -> NpcEquipment.offHandGun(npc)).thenReturn(true);
            equipment.when(() -> FlansEquipment.getWeaponProperties(main, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            WeaponKind mainKind = new WeaponKind(properties.ranged(), properties.throwable(), properties.shield());
            equipment.when(() -> FlansEquipment.getWeaponKind(main)).thenReturn(Optional.of(mainKind));
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

    @Test
    void gunWithNothingToLoadDoesNotReloadAcrossEngagements() throws ReflectiveOperationException
    {
        LivingEntity target = mock(LivingEntity.class);
        EntityNPCInterface npc = engagedNpc(target);
        ItemStack gun = new ItemStack(Items.STICK);
        when(npc.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(gun);
        EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
        when(properties.reloadTime()).thenReturn(40D);
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
            options.when(() -> NpcEquipment.rangedWeapon(npc)).thenReturn(gun);
            options.when(() -> NpcEquipment.ranged(npc)).thenReturn(true);
            options.when(() -> NpcEquipment.flan(npc, InteractionHand.MAIN_HAND)).thenReturn(Optional.of(properties));
            equipment.when(() -> FlansEquipment.getWeaponProperties(gun, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            WeaponKind gunKind = new WeaponKind(properties.ranged(), properties.throwable(), properties.shield());
            equipment.when(() -> FlansEquipment.getWeaponKind(gun)).thenReturn(Optional.of(gunKind));
            ammunition.when(() -> NpcAmmunition.nothingToLoad(npc, gun)).thenReturn(true);
            NpcEquipmentRangedGoal goal = new NpcEquipmentRangedGoal(npc);
            // Each engagement restarts the goal, which forgets that the last reload failed.
            for (int engagement = 0; engagement < 5; engagement++)
            {
                for (int tick = 0; tick < 100; tick++)
                    goal.tick();
                goal.stop();
            }
            feedback.verify(() -> NpcReloadFeedback.reload(any(), any(), anyInt()), never());
            attacks.verify(() -> NpcItemAttacks.loadFlanGun(any(), any()), never());
            attacks.verify(() -> NpcItemAttacks.ranged(any(), any()), never());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"HIDDEN", "DEAD", "LOST", "MELEE", "DISARMED", "FLEE"})
    @SuppressWarnings("unused") // The ammunition mock only has to be open, so that the goal does not reach a real inventory.
    void aReloadFinishesOnceWhenTheEngagementEnds(String reason) throws ReflectiveOperationException
    {
        LivingEntity target = mock(LivingEntity.class);
        EntityNPCInterface npc = engagedNpc(target);
        Sensing sensing = npc.getSensing();
        ItemStack gun = new ItemStack(Items.STICK);
        when(npc.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(gun);
        EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
        when(properties.reloadTime()).thenReturn(40D);
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
            options.when(() -> NpcEquipment.rangedWeapon(npc)).thenReturn(gun);
            options.when(() -> NpcEquipment.ranged(npc)).thenReturn(true);
            options.when(() -> NpcEquipment.flan(npc, InteractionHand.MAIN_HAND)).thenReturn(Optional.of(properties));
            equipment.when(() -> FlansEquipment.getWeaponProperties(gun, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            WeaponKind gunKind = new WeaponKind(properties.ranged(), properties.throwable(), properties.shield());
            equipment.when(() -> FlansEquipment.getWeaponKind(gun)).thenReturn(Optional.of(gunKind));
            boolean[] loaded = {false};
            equipment.when(() -> FlansEquipment.hasLoadedRound(gun)).thenAnswer(call -> loaded[0]);
            attacks.when(() -> NpcItemAttacks.loadFlanGun(npc, InteractionHand.MAIN_HAND)).thenAnswer(call ->
            {
                loaded[0] = true;
                return true;
            });
            NpcEquipmentRangedGoal goal = new NpcEquipmentRangedGoal(npc);
            goal.tick();
            feedback.verify(() -> NpcReloadFeedback.reload(npc, InteractionHand.MAIN_HAND, 40));
            switch (reason)
            {
                case "DEAD" -> when(target.isAlive()).thenReturn(false);
                case "LOST" -> when(npc.getTarget()).thenReturn(null);
                case "MELEE" -> when(npc.isInRange(target, 0D)).thenReturn(true);
                case "DISARMED" -> options.when(() -> NpcEquipment.ranged(npc)).thenReturn(false);
                case "FLEE" -> npc.ais.onAttack = 1;
                default -> when(sensing.hasLineOfSight(target)).thenReturn(false);
            }
            goal.stop();
            assertTrue(goal.canUse(), "The reload keeps the goal ticking without a ranged engagement");
            for (int tick = 0; tick < 40; tick++)
                goal.tick();
            assertTrue(loaded[0]);
            feedback.verify(() -> NpcReloadFeedback.reload(npc, InteractionHand.MAIN_HAND, 40), times(1));
            attacks.verify(() -> NpcItemAttacks.loadFlanGun(npc, InteractionHand.MAIN_HAND), times(1));
            attacks.verify(() -> NpcItemAttacks.ranged(any(), any()), never());
        }
    }

    /** An NPC whose target is in range and in sight. */
    @SuppressWarnings("java:S3011") // Mockito skips construction; the goal also reads the NPC's final AI container.
    private static EntityNPCInterface engagedNpc(LivingEntity target) throws ReflectiveOperationException
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        var aiField = EntityNPCInterface.class.getField("ais");
        aiField.setAccessible(true);
        aiField.set(npc, new DataAI(npc));
        when(npc.getTarget()).thenReturn(target);
        LookControl look = mock(LookControl.class);
        when(npc.getLookControl()).thenReturn(look);
        PathNavigation navigation = mock(PathNavigation.class);
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
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        when(target.level()).thenReturn(level);
        when(target.isAlive()).thenReturn(true);
        return npc;
    }
}
