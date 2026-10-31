package com.wolffsmod.npcs.gametest;

import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;

/** Runs against the real Custom NPCs lifecycle and entities, without any client classes. */
@SuppressWarnings("java:S2187") // Minecraft discovers @GameTest methods; these are not JUnit tests.
@GameTestHolder("flansmodultimate")
@PrefixGameTestTemplate(false)
public class NpcServerGameTests
{
    @GameTest(template = "npc_empty")
    public static void saveDirectories(GameTestHelper helper)
    {
        helper.assertTrue(CustomNpcs.Server == helper.getLevel().getServer(), "Custom NPCs must own the running test server");
        helper.assertTrue(FactionController.instance != null && !FactionController.instance.factions.isEmpty(), "NPC factions must load during server startup");
        File world = helper.getLevel().getServer().getWorldPath(new LevelResource("customnpcs")).toFile();
        boolean externalSaving = CustomNpcs.EnableExternalSaving;
        try
        {
            CustomNpcs.EnableExternalSaving = false;
            helper.assertTrue(world.equals(CustomNpcs.getLevelSaveDirectory()), "NPC data must use the test world's directory");
            File nested = CustomNpcs.getLevelSaveDirectory("gametest/nested");
            helper.assertTrue(new File(world, "gametest/nested").equals(nested) && nested.isDirectory(), "NPC subdirectories must be created inside the test world");
            CustomNpcs.EnableExternalSaving = true;
            helper.assertTrue(CustomNpcs.Dir.equals(CustomNpcs.getLevelSaveDirectory()), "External saving must retain the configured NPC directory");
            helper.assertTrue(world.equals(CustomNpcs.getLevelSaveDirectory(null, true)), "World-specific saves must bypass external saving");
            helper.assertTrue(new File(world, "gametest/nested").equals(CustomNpcs.getLevelSaveDirectory("gametest/nested")), "Named saves must remain world-specific");
        }
        finally
        {
            CustomNpcs.EnableExternalSaving = externalSaving;
        }
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
