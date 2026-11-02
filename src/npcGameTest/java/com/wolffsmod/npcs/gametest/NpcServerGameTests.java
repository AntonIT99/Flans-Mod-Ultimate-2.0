package com.wolffsmod.npcs.gametest;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.tactics.NpcSafetyGoal;
import com.wolffsmod.npcs.tactics.NpcSafetyState;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import com.wolffsmod.npcs.teams.NpcDuties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;

/** Runs against the real Custom NPCs lifecycle and entities, without any client classes. */
@SuppressWarnings("java:S2187") // Minecraft discovers @GameTest methods; these are not JUnit tests.
@GameTestHolder("flansmodultimate")
@PrefixGameTestTemplate(false)
public class NpcServerGameTests
{
    @GameTest(template = "npc_empty", batch = "npc_safety_retreat")
    public static void ordinaryNpcRetreatsDuringCombatWithoutForgettingItsEnemy(GameTestHelper helper)
    {
        var npc = helper.spawn(CustomEntities.entityCustomNpc, 1, 1, 1);
        var enemy = helper.spawn(CustomEntities.entityCustomNpc, 2, 1, 1);
        enemy.setNoAi(true);
        npc.ais.onAttack = 0;
        npc.ais.returnToStart = false;
        npc.ais.setStartPos(npc.blockPosition().offset(-8, 0, 0));
        npc.setTarget(enemy);
        npc.setHealth(npc.getMaxHealth() * 0.1F);
        npc.tickCount = Math.floorMod(-npc.getId(), 20);
        NpcDuties.tick(npc);
        helper.assertTrue(NpcDuties.retreating(npc), "A normal NPC must enter health-based retreat without a Teams battle");
        NpcSafetyGoal goal = new NpcSafetyGoal(npc);
        helper.assertTrue(goal.canUse(), "Safety must interrupt combat while an enemy remains targeted");
        goal.start();
        goal.tick();
        helper.assertTrue(NpcTacticState.of(npc).getSafety().getAction() == NpcSafetyState.Action.RETREAT, "Retreat movement must own navigation");
        helper.assertTrue(npc.getTarget() == enemy, "Withdrawing must preserve the remembered firing target");
        helper.succeed();
    }

    @GameTest(template = "npc_empty", batch = "npc_safety_cover")
    public static void ordinaryNpcTakesCoverFromASecondAttacker(GameTestHelper helper)
    {
        for (int x = -4; x <= 10; x++)
            for (int z = -4; z <= 6; z++)
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        var npc = helper.spawn(CustomEntities.entityCustomNpc, 1, 1, 1);
        var target = helper.spawn(CustomEntities.entityCustomNpc, 1, 1, 4);
        var attacker = helper.spawn(CustomEntities.entityCustomNpc, 8, 1, 1);
        target.setNoAi(true);
        attacker.setNoAi(true);
        npc.ais.onAttack = 0;
        npc.ais.returnToStart = false;
        npc.setTarget(target);
        for (int z = -2; z <= 4; z++)
        {
            helper.setBlock(new BlockPos(3, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 2, z), Blocks.STONE);
        }
        long now = helper.getLevel().getGameTime();
        NpcTacticState.of(npc).threat(attacker, target, now, now + 100L);
        NpcSafetyGoal goal = new NpcSafetyGoal(npc);
        helper.assertTrue(goal.canUse(), "A second attacker's incoming fire must trigger cover inside ordinary firing range");
        helper.assertTrue(NpcTacticState.of(npc).getSafety().getAction() == NpcSafetyState.Action.HIDE, "The shared safety goal must choose cover");
        helper.assertTrue(npc.getTarget() == target, "Taking cover must not replace the current firing target");
        helper.succeed();
    }

    @GameTest(template = "npc_empty", batch = "npc_safety_settings")
    public static void ordinaryNpcCombatDefaultsAndOptOutsSurviveCloning(GameTestHelper helper)
    {
        var npc = helper.spawn(CustomEntities.entityCustomNpc, 1, 1, 1);
        npc.setNoAi(true);
        var options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        helper.assertTrue(options.enabled(Feature.TACTICAL_COVER) && options.enabled(Feature.VEHICLE_EVASION), "Ordinary NPC safety defaults must be enabled");
        options.set(Feature.TACTICAL_COVER, false);
        NpcTuning.of(npc).set(NpcTuning.Value.COMBAT_MOVEMENT, NpcTuning.MovementFreedom.HOLD.ordinal());
        CompoundTag saved = npc.saveWithoutId(new CompoundTag());
        EntityCustomNpc restored = new EntityCustomNpc(CustomEntities.entityCustomNpc, helper.getLevel());
        restored.load(saved);
        helper.assertFalse(((NpcWeaponSettings) restored.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.TACTICAL_COVER), "A clone must retain an explicit opt-out");
        helper.assertTrue(NpcTuning.of(restored).get(NpcTuning.Value.COMBAT_MOVEMENT) == NpcTuning.MovementFreedom.HOLD.ordinal(), "A clone must retain movement limits");
        helper.succeed();
    }

    @GameTest(template = "npc_empty")
    public static void saveDirectories(GameTestHelper helper)
    {
        helper.assertTrue(CustomNpcs.Server == helper.getLevel().getServer(), "Custom NPCs must own the running test server");
        helper.assertTrue(FactionController.instance != null && !FactionController.instance.factions.isEmpty(), "NPC factions must load during server startup");
        File world = helper.getLevel().getServer().getWorldPath(new LevelResource("customnpcs")).toFile();
        helper.assertTrue(world.equals(CustomNpcs.getLevelSaveDirectory()), "NPC data must use the test world's directory");
        File nested = CustomNpcs.getLevelSaveDirectory("gametest/nested");
        helper.assertTrue(new File(world, "gametest/nested").equals(nested) && nested.isDirectory(), "NPC subdirectories must be created inside the test world");
        helper.succeed();
    }

    @GameTest(template = "npc_empty")
    public static void attackSelectorTakesTheNpcsRanges(GameTestHelper helper)
    {
        var npc = helper.spawn(CustomEntities.entityCustomNpc, 1, 1, 1);
        var other = helper.spawn(CustomEntities.entityCustomNpc, 2, 1, 1);
        npc.setNoAi(true);
        other.setNoAi(true);
        // Loading the selector applies its mixin, which fails the load when one of its targets is missing.
        NPCAttackSelector selector = new NPCAttackSelector(npc);
        helper.assertFalse(selector.isEntityApplicable(npc), "An NPC must never select itself");
        helper.assertTrue(NpcWeaponRange.aggro(npc) == npc.stats.aggroRange, "Without a weapon range the NPC's own aggro range applies");
        selector.isEntityApplicable(other);
        helper.succeed();
    }

    @GameTest(template = "npc_empty")
    public static void npcTicksAndSaves(GameTestHelper helper)
    {
        var npc = helper.spawn(CustomEntities.entityCustomNpc, 1, 1, 1);
        npc.setNoAi(true);
        npc.setHealth(10.0F);
        helper.runAfterDelay(5, () ->
        {
            helper.assertTrue(npc.isAlive() && npc.tickCount > 0, "A real NPC must survive server ticks");
            CompoundTag saved = npc.saveWithoutId(new CompoundTag());
            EntityCustomNpc restored = new EntityCustomNpc(CustomEntities.entityCustomNpc, helper.getLevel());
            restored.load(saved);
            helper.assertTrue(restored.getHealth() == npc.getHealth(), "NPC health must survive an NBT round trip");
            helper.succeed();
        });
    }
}
