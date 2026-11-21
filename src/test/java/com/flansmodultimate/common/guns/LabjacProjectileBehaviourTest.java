package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Long-range damage, missile flight profile and projectile chunk loading ported from the Labjac fork. */
class LabjacProjectileBehaviourTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "labjac-projectiles"));

    @Test
    void longRangeDamageReplacesSoftTargetDamageOnlyAfterTheDelay()
    {
        BulletType type = bullet("DynamicBulletDelay 10", "DynamicDamage 0.4");
        assertEquals(1.5F, LongRangeDamage.authoredDamage(type, true, 1.5F, 10F));
        assertEquals(0.4F, LongRangeDamage.authoredDamage(type, true, 1.5F, 10.5F));
        assertEquals(1.5F, LongRangeDamage.authoredDamage(type, false, 1.5F, 50F));
    }

    @Test
    void kineticRoundsScaleByTheRatioToTheirAuthoredDamage()
    {
        BulletType type = bullet("DynamicBulletDelay 0", "DynamicDamage 0.5");
        assertEquals(0.25F, LongRangeDamage.kineticFactor(type, true, 2F, 1F), 1e-6F);
        assertEquals(1F, LongRangeDamage.kineticFactor(type, false, 2F, 1F));
        assertEquals(1F, LongRangeDamage.kineticFactor(type, true, 0F, 1F));
        assertEquals(1F, LongRangeDamage.kineticFactor(bullet(), true, 2F, 1000F));
    }

    @Test
    void onlyGuidedRoundsTwirl()
    {
        assertTrue(MissileFlightProfile.shouldTwirl(bullet("Missile"), false));
        assertTrue(MissileFlightProfile.shouldTwirl(bullet("ManualGuidance true"), false));
        assertTrue(MissileFlightProfile.shouldTwirl(bullet(), true));
        assertFalse(MissileFlightProfile.shouldTwirl(bullet(), false));
        assertFalse(MissileFlightProfile.shouldTwirl(bullet("Missile", "MissileTwirl false"), true));
        assertFalse(MissileFlightProfile.shouldTwirl(null, true));
    }

    @Test
    void twirlCombinesSteadyRollAndWobble()
    {
        assertEquals(0F, MissileFlightProfile.twirlDegrees(0F, 28F, 4F));
        assertEquals(280F + (float) Math.sin(4.2F) * 4F, MissileFlightProfile.twirlDegrees(10F, 28F, 4F), 1e-4F);
    }

    @Test
    void levelFlightJustAboveGroundSkims()
    {
        Vec3 level = new Vec3(2D, -0.05D, 0D);
        assertTrue(MissileFlightProfile.shouldSkim(0.12F, Direction.UP, true, 0.1D, level));
        assertFalse(MissileFlightProfile.shouldSkim(0.12F, Direction.UP, true, 0.2D, level), "too high to count as skimming");
        assertFalse(MissileFlightProfile.shouldSkim(0.12F, Direction.UP, true, 0D, level), "already at the surface");
        assertFalse(MissileFlightProfile.shouldSkim(0.12F, Direction.NORTH, true, 0.1D, level), "walls are struck");
        assertFalse(MissileFlightProfile.shouldSkim(0.12F, Direction.UP, false, 0.1D, level), "built surfaces are struck");
        assertFalse(MissileFlightProfile.shouldSkim(0F, Direction.UP, true, 0.1D, level), "disabled");
        assertFalse(MissileFlightProfile.shouldSkim(0.12F, Direction.UP, true, 0.1D, new Vec3(2D, -0.3D, 0D)), "dives strike");
        assertFalse(MissileFlightProfile.shouldSkim(0.12F, Direction.UP, true, 0.1D, new Vec3(0.3D, -0.2D, 0D)), "not level enough");
    }

    @Test
    void skimCancelsTheDescentWithASmallClimb()
    {
        Vec3 skimmed = MissileFlightProfile.skimVelocity(new Vec3(2D, -0.05D, 1D), 0.12F);
        assertEquals(2D, skimmed.x);
        assertEquals(0.03D, skimmed.y, 1e-6D);
        assertEquals(1D, skimmed.z);
        assertEquals(MissileFlightProfile.MAX_SKIM_LIFT, MissileFlightProfile.skimVelocity(Vec3.ZERO, 1F).y);
        assertEquals(0.1D, MissileFlightProfile.skimVelocity(new Vec3(1D, 0.1D, 0D), 0.12F).y, "a climb is kept");
    }

    @Test
    void chunkLimitsCountPlayersAndTheServer()
    {
        assertTrue(ProjectileChunkLoader.admits(true, 3, 10, 4, 64));
        assertFalse(ProjectileChunkLoader.admits(true, 4, 10, 4, 64));
        assertFalse(ProjectileChunkLoader.admits(true, 0, 64, 4, 64));
        assertTrue(ProjectileChunkLoader.admits(false, 99, 10, 4, 64), "ownerless rounds count only against the server");
        assertFalse(ProjectileChunkLoader.admits(false, 0, 0, 4, 0));
    }

    @Test
    void ticketsCoverThePathAheadWithoutGaps()
    {
        LongList standing = ProjectileChunkLoader.chunksAhead(new Vec3(8D, 64D, 8D), Vec3.ZERO, 10, 32);
        assertEquals(List.of(ChunkPos.asLong(0, 0)), standing);

        LongList fast = ProjectileChunkLoader.chunksAhead(new Vec3(8D, 64D, 8D), new Vec3(16D, 0D, 0D), 10, 32);
        assertEquals(11, fast.size());
        for (int x = 0; x <= 10; x++)
            assertTrue(fast.contains(ChunkPos.asLong(x, 0)), "chunk " + x);

        assertTrue(ProjectileChunkLoader.chunksAhead(Vec3.ZERO, new Vec3(1000D, 0D, 0D), 10, 32).size() <= 32);
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("labjac", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
