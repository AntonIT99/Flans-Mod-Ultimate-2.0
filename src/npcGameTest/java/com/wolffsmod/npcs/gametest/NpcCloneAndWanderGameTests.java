package com.wolffsmod.npcs.gametest;

import com.wolffsmod.npcs.NpcsCommonConfig;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import noppes.npcs.CustomEntities;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.server.SPacketToolMobSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Reproduces reported clone and wandering problems through Custom NPCs' own clone store, spawner and AI. */
@SuppressWarnings("java:S2187") // Minecraft discovers @GameTest methods; these are not JUnit tests.
@GameTestHolder("flansmodultimate")
@PrefixGameTestTemplate(false)
public class NpcCloneAndWanderGameTests
{
    private static final int WANDERING = 1;

    @GameTest(template = "npc_empty", batch = "npc_clone_settings", timeoutTicks = 200)
    public static void clonedNpcKeepsEveryBehaviourSetting(GameTestHelper helper)
    {
        floor(helper, 12);
        EntityNPCInterface npc = helper.spawn(CustomEntities.entityCustomNpc, 1, 1, 1);
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        for (Feature feature : Feature.values())
            options.set(feature, !options.enabled(feature));
        NpcTuning tuning = NpcTuning.of(npc);
        for (Value value : Value.values())
            tuning.set(value, value.clamp(value.fallback() + 1) != value.fallback() ? value.clamp(value.fallback() + 1) : value.clamp(value.fallback() - 1));
        npc.ais.setMovingType(WANDERING);
        npc.ais.walkingRange = 17;
        npc.ais.canSprint = true;
        npc.stats.aggroRange = 37;
        // Expected values are read back from the original, since setters may clamp or couple values.
        boolean[] features = new boolean[Feature.values().length];
        for (Feature feature : Feature.values())
            features[feature.ordinal()] = options.enabled(feature);
        int[] values = new int[Value.values().length];
        for (Value value : Value.values())
            values[value.ordinal()] = tuning.get(value);

        // The cloner tool's path: the entity's full save, cleaned, stored by name, read back and spawned.
        CompoundTag captured = new CompoundTag();
        npc.save(captured);
        ServerCloneController.Instance.cleanTags(captured);
        ServerCloneController.Instance.addClone(captured, "gametest_clone", 9);
        CompoundTag stored = ServerCloneController.Instance.getCloneData(helper.getLevel().getServer().createCommandSourceStack(), "gametest_clone", 9);
        helper.assertTrue(stored != null, "The clone must be stored and read back");
        Vec3 at = helper.absoluteVec(new Vec3(6.5, 1, 6.5));
        Entity spawned = SPacketToolMobSpawner.spawnClone(stored, at.x, at.y, at.z, helper.getLevel());
        helper.assertTrue(spawned instanceof EntityNPCInterface, "The clone must spawn as an NPC, but was " + spawned);
        EntityNPCInterface clone = (EntityNPCInterface) spawned;
        List<String> atSpawn = differences(clone, features, values);
        helper.runAfterDelay(60, () ->
        {
            List<String> afterTicks = differences(clone, features, values);
            if (clone.ais.getMovingType() != WANDERING)
                afterTicks.add("ai.movingType=" + clone.ais.getMovingType());
            if (clone.ais.walkingRange != 17)
                afterTicks.add("ai.walkingRange=" + clone.ais.walkingRange);
            if (!clone.ais.canSprint)
                afterTicks.add("ai.canSprint=false");
            if (clone.stats.aggroRange != 37)
                afterTicks.add("stats.aggroRange=" + clone.stats.aggroRange);
            ServerCloneController.Instance.removeClone("gametest_clone", 9);
            helper.assertTrue(atSpawn.isEmpty() && afterTicks.isEmpty(), "Clone lost settings. At spawn: " + atSpawn + "; after 60 ticks: " + afterTicks);
            helper.succeed();
        });
    }

    /**
     * Wandering only starts on a tick vanilla lets goals start, which the far tick level of detail used to skip for NPCs
     * spawned on an odd server tick.
     */
    @GameTest(template = "npc_empty", batch = "npc_wander", timeoutTicks = 600)
    public static void wanderingNpcWalksAround(GameTestHelper helper)
    {
        floor(helper, 12);
        EntityNPCInterface npc = helper.spawn(CustomEntities.entityCustomNpc, 6, 1, 6);
        npc.ais.setMovingType(WANDERING);
        npc.ais.walkingRange = 8;
        npc.ais.movingPause = false;
        npc.ais.setStartPos(npc.blockPosition());
        npc.updateAI = true;
        Vec3 start = npc.position();
        double[] farthest = {0D};
        int[] navigating = {0};
        helper.onEachTick(() ->
        {
            // No player stands in a GameTest; vanilla would otherwise stop idle strolling after 100 ticks without one.
            npc.setNoActionTime(0);
            farthest[0] = Math.max(farthest[0], npc.position().distanceTo(start));
            if (!npc.getNavigation().isDone())
                navigating[0]++;
        });
        helper.succeedWhen(() -> helper.assertTrue(farthest[0] > 2D, "A wandering NPC must walk away from where it stands; farthest " + farthest[0] + " blocks, ticks with a path "
            + navigating[0] + ", async pathfinding " + NpcsCommonConfig.asyncPathfinding()));
    }

    /** A single move order gets no path while its worker search runs; the NPC must follow the path once it is found. */
    @GameTest(template = "npc_empty", batch = "npc_single_move", timeoutTicks = 200)
    public static void singleMoveOrderIsFollowed(GameTestHelper helper)
    {
        floor(helper, 12);
        EntityNPCInterface npc = helper.spawn(CustomEntities.entityCustomNpc, 2, 1, 6);
        npc.ais.returnToStart = false;
        Vec3 goal = helper.absoluteVec(new Vec3(9.5, 1, 6.5));
        helper.runAfterDelay(5, () -> npc.getNavigation().moveTo(goal.x, goal.y, goal.z, 1D));
        helper.succeedWhen(() -> helper.assertTrue(npc.position().distanceTo(goal) < 2D, "An NPC must walk a single move order, but is " + npc.position().distanceTo(goal)
            + " blocks short with async pathfinding " + NpcsCommonConfig.asyncPathfinding()));
    }

    private static List<String> differences(EntityNPCInterface clone, boolean[] features, int[] values)
    {
        List<String> differences = new ArrayList<>();
        NpcWeaponOptions options = ((NpcWeaponSettings) clone.stats).wolffsmodnpcsWeaponOptions();
        for (Feature feature : Feature.values())
            if (options.enabled(feature) != features[feature.ordinal()])
                differences.add(feature.name() + "=" + options.enabled(feature));
        NpcTuning tuning = NpcTuning.of(clone);
        for (Value value : Value.values())
            if (tuning.get(value) != values[value.ordinal()])
                differences.add(value.name() + "=" + tuning.get(value) + " (expected " + values[value.ordinal()] + ")");
        return differences;
    }

    private static void floor(GameTestHelper helper, int size)
    {
        // Batches share one test position, and the flotation tests leave a pool beyond the template's bounds.
        for (int x = -40; x <= 40; x++)
            for (int z = -20; z <= 20; z++)
                for (int y = 0; y <= 12; y++)
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        for (int x = -1; x <= size; x++)
            for (int z = -1; z <= size; z++)
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }
}
