package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.Blocks;

import static org.junit.jupiter.api.Assertions.*;

class SurfaceTractionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void classifiesVanillaGround()
    {
        assertEquals(SurfaceTraction.Kind.FIRM, SurfaceTraction.classify(Blocks.STONE.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.FIRM, SurfaceTraction.classify(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.FIRM, SurfaceTraction.classify(Blocks.DIRT_PATH.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.FIRM, SurfaceTraction.classify(Blocks.AIR.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.LOOSE, SurfaceTraction.classify(Blocks.SAND.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.LOOSE, SurfaceTraction.classify(Blocks.RED_SAND.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.LOOSE, SurfaceTraction.classify(Blocks.GRAVEL.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.LOOSE, SurfaceTraction.classify(Blocks.SNOW.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.LOOSE, SurfaceTraction.classify(Blocks.SNOW_BLOCK.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.LOOSE, SurfaceTraction.classify(Blocks.FARMLAND.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SOFT, SurfaceTraction.classify(Blocks.MUD.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SOFT, SurfaceTraction.classify(Blocks.SOUL_SAND.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SOFT, SurfaceTraction.classify(Blocks.SOUL_SOIL.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SOFT, SurfaceTraction.classify(Blocks.POWDER_SNOW.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SLICK, SurfaceTraction.classify(Blocks.ICE.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SLICK, SurfaceTraction.classify(Blocks.PACKED_ICE.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SLICK, SurfaceTraction.classify(Blocks.BLUE_ICE.defaultBlockState()));
        assertEquals(SurfaceTraction.Kind.SLICK, SurfaceTraction.classify(Blocks.SLIME_BLOCK.defaultBlockState()));
    }

    @Test
    void firmGroundLeavesEveryLayoutUntouched()
    {
        for (EnumDriveType driveType : EnumDriveType.values())
            assertEquals(SurfaceTraction.Sample.FIRM, SurfaceTraction.sample(SurfaceTraction.Kind.FIRM, driveType));
    }

    @Test
    void tracksAndAllWheelDriveRecoverPartOfThePenalty()
    {
        for (SurfaceTraction.Kind kind : new SurfaceTraction.Kind[]{SurfaceTraction.Kind.LOOSE, SurfaceTraction.Kind.SOFT})
        {
            SurfaceTraction.Sample wheeled = SurfaceTraction.sample(kind, EnumDriveType.RWD);
            SurfaceTraction.Sample allWheel = SurfaceTraction.sample(kind, EnumDriveType.AWD);
            SurfaceTraction.Sample tracked = SurfaceTraction.sample(kind, EnumDriveType.TRACKED);
            assertTrue(wheeled.speedFactor() < allWheel.speedFactor());
            assertTrue(allWheel.speedFactor() < tracked.speedFactor());
            assertTrue(tracked.speedFactor() < 1F);
            assertTrue(wheeled.grip() < tracked.grip());
        }
    }

    @Test
    void softGroundSlowsMoreThanLooseGround()
    {
        assertTrue(SurfaceTraction.sample(SurfaceTraction.Kind.SOFT, EnumDriveType.RWD).speedFactor()
            < SurfaceTraction.sample(SurfaceTraction.Kind.LOOSE, EnumDriveType.RWD).speedFactor());
    }

    @Test
    void iceCostsGripButNoSpeedAndTracksBarelyHelp()
    {
        SurfaceTraction.Sample wheeled = SurfaceTraction.sample(SurfaceTraction.Kind.SLICK, EnumDriveType.RWD);
        SurfaceTraction.Sample tracked = SurfaceTraction.sample(SurfaceTraction.Kind.SLICK, EnumDriveType.TRACKED);
        assertEquals(1F, wheeled.speedFactor(), 1.0E-6F);
        assertTrue(wheeled.grip() < 0.5F);
        assertTrue(tracked.grip() < 0.5F);
        assertTrue(tracked.grip() > wheeled.grip());
    }

    @Test
    void averagingAndSteeringStayBounded()
    {
        assertEquals(SurfaceTraction.Sample.FIRM, SurfaceTraction.average(0D, 0D, 0));
        SurfaceTraction.Sample averaged = SurfaceTraction.average(0.5D + 1D, 0.6D + 1D, 2);
        assertEquals(0.75F, averaged.grip(), 1.0E-6F);
        assertEquals(0.8F, averaged.speedFactor(), 1.0E-6F);
        assertEquals(1F, SurfaceTraction.steeringFactor(1F), 1.0E-6F);
        assertEquals(0.5F, SurfaceTraction.steeringFactor(0F), 1.0E-6F);
        assertTrue(SurfaceTraction.steeringFactor(0.3F) > 0.5F);
    }

    @Test
    void lowGripBogsTheClimberDownSooner()
    {
        float firm = GroundSlopePhysics.propulsionFactor(25F, 1F, 0.02F, EnumDriveType.TRACKED, 1F);
        float slick = GroundSlopePhysics.propulsionFactor(25F, 1F, 0.02F, EnumDriveType.TRACKED, 0.4F);
        assertTrue(slick < firm);
        assertTrue(slick < 1F);
    }
}
