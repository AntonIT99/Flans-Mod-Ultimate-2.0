package com.wolffsmod.npcs.gametest;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Places ships on open water the way their item does and checks that they settle at their model waterline.
 * The Rainfire5 cases need Rainfire5's Arsenal Battle Squadrons in the game test run's {@code flan} folder.
 */
@SuppressWarnings("java:S2187") // Minecraft discovers @GameTest methods; these are not JUnit tests.
@GameTestHolder("flansmodultimate")
@PrefixGameTestTemplate(false)
public class ShipFlotationGameTests
{
    private static final int SURFACE_BLOCK = 12;
    private static final int SETTLE_TICKS = 240;

    /** Quarter-scale model whose researched 4.7 m draft is deeper than its whole hull. */
    @GameTest(template = "npc_empty", batch = "ship_flotation_destroyer", timeoutTicks = 400)
    public static void downscaledDestroyerKeepsItsDeckAboveWater(GameTestHelper helper)
    {
        settles(helper, "hmsJervis", true);
    }

    @GameTest(template = "npc_empty", batch = "ship_flotation_submarine", timeoutTicks = 400)
    public static void downscaledSubmarineKeepsItsDeckAboveWater(GameTestHelper helper)
    {
        settles(helper, "ussBarb", true);
    }

    /** Drawn to scale: the researched draft must stand. */
    @GameTest(template = "npc_empty", batch = "ship_flotation_to_scale", timeoutTicks = 400)
    public static void toScaleBoatKeepsItsResearchedDraft(GameTestHelper helper)
    {
        settles(helper, "S100", false);
    }

    /** A ship authored as a plane: its flight model applies gravity before flotation runs. */
    @GameTest(template = "npc_empty", batch = "ship_flotation_plane", timeoutTicks = 400)
    public static void planeTypeShipFloats(GameTestHelper helper)
    {
        settles(helper, "Ships_Plane_PrinzEugen_1A", false);
    }

    private static void settles(GameTestHelper helper, String shortName, boolean draftCapped)
    {
        InfoType info = InfoType.getInfoTypes().values().stream().filter(candidate -> candidate.getShortName().equalsIgnoreCase(shortName)).findFirst().orElse(null);
        if (info == null || !(info.getItem() instanceof DriveableItem<?, ?> item))
        {
            helper.fail("Missing driveable " + shortName + "; Rainfire5 ships need Battle Squadrons in run/gametest/flan");
            return;
        }
        DriveableType type = item.getConfigType();
        Float draft = type.getResolvedPhysics().draftM();
        Float modelDraft = type.getModelDraftM();
        if (draftCapped)
            helper.assertTrue(draft != null && modelDraft != null && modelDraft < draft, shortName + " must cap its draft " + draft + " to the model, got " + modelDraft);
        else
            helper.assertTrue(draft == null || draft.equals(modelDraft), shortName + " must keep its draft " + draft + ", got " + modelDraft);

        for (int x = -36; x <= 36; x++)
        {
            for (int z = -16; z <= 16; z++)
            {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= SURFACE_BLOCK; y++)
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
            }
        }
        BlockPos top = helper.absolutePos(new BlockPos(0, SURFACE_BLOCK, 0));
        double surfaceY = top.getY() + helper.getLevel().getFluidState(top).getHeight(helper.getLevel(), top);
        // As DriveableItem places on a water block: one block above it, plus FloatOffset and YOffset.
        Vec3 spawn = helper.absoluteVec(new Vec3(0.5D, SURFACE_BLOCK + 1D, 0.5D));
        Driveable ship = item.spawnDriveable(helper.getLevel(), spawn.x, spawn.y + type.getFloatOffset() + type.getYOffset(), spawn.z, 0F, null, null);
        if (ship == null)
        {
            helper.fail("Could not place " + shortName);
            return;
        }
        helper.runAfterDelay(SETTLE_TICKS, () ->
        {
            double height = ship.getY() - surfaceY;
            double expected = type.getFloatingHeight();
            System.out.printf(Locale.ROOT, "[ShipFlotation] %s draft=%s model=%s height=%.3f expected=%.3f vy=%.4f%n", shortName, draft, modelDraft, height, expected, ship.getDeltaMovement().y);
            helper.assertTrue(!ship.isRemoved(), shortName + " was removed");
            helper.assertTrue(Math.abs(height - expected) < 0.3D, String.format(Locale.ROOT, "%s must settle at %.2f above the surface, rode at %.2f", shortName, expected, height));
            helper.assertTrue(Math.abs(ship.getDeltaMovement().y) < 0.01D, shortName + " must have settled, still moving at " + ship.getDeltaMovement().y);
            helper.succeed();
        });
    }
}
