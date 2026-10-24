package com.flansmodultimate.common.driveables.parts;

import com.flansmodultimate.api.*;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.PartBodyHit;
import com.flansmodultimate.common.types.DriveableType;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartBodyTest
{
    private static final Vec3 ORIGIN = new Vec3(10D, 64D, 10D);

    /** A hull two blocks wide and four long, a pod on its right side, a turret box ahead of the pivot and a wing with its wheel. */
    private static PartBody body()
    {
        Map<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        boxes.put(EnumDriveablePart.RIGHT_SKIRT, box(50F, 2F, 0F, -0.5F, 1F, 1F, 1F));
        boxes.put(EnumDriveablePart.CORE, box(100F, -1F, 0F, -2F, 2F, 1F, 4F));
        boxes.put(EnumDriveablePart.TURRET, box(40F, -0.25F, 1F, -3F, 0.5F, 0.5F, 1F));
        boxes.put(EnumDriveablePart.LEFT_WING, box(30F, -6F, 0F, -1F, 2F, 0.5F, 1F));
        boxes.put(EnumDriveablePart.LEFT_WING_WHEEL, box(10F, -5F, -1F, -1F, 0.5F, 0.5F, 0.5F));
        PartBody body = new PartBody(null, boxes, Vec3.ZERO, Vec3.ZERO);
        body.setPose(new PartBodyPose(ORIGIN, 0F, 1F, MountAim.REST));
        return body;
    }

    private static CollisionBox box(float health, float x, float y, float z, float width, float height, float depth)
    {
        return CollisionBox.inWorldUnits(health, x, y, z, width, height, depth, 2F, 0F);
    }

    private static PartBodyHit first(PartBody body, Vec3 origin, Vec3 motion)
    {
        List<BulletHit> hits = body.trace(null, origin, motion);
        assertFalse(hits.isEmpty(), "expected a hit");
        return (PartBodyHit) hits.get(0);
    }

    /** Where the drawn model puts a point of its preview frame: scaled, then turned by 180 degrees less its yaw. */
    private static Vec3 drawn(Vec3 point, PartBodyPose pose)
    {
        return pose.origin().add(point.scale(pose.scale()).yRot((float) Math.toRadians(180F - pose.yaw())));
    }

    @Test
    void aBodyFacingSouthIsStruckOnTheFaceTurnedToTheRound()
    {
        PartBody body = body();
        // The model faces -Z in its own frame, which a yaw of 0 turns south.
        PartBodyHit front = first(body, new Vec3(10D, 64.5D, 20D), new Vec3(0D, 0D, -10D));
        assertEquals(EnumDriveablePart.CORE, front.getPart());
        assertEquals(EnumArmorFacing.FRONT, front.getFacing());
        assertEquals(0.8F, front.getIntersectTime(), 1.0E-5F);
        assertEquals(12D, front.getHitPosition().z, 1.0E-5D);
        assertEquals(EnumArmorFacing.REAR, first(body, new Vec3(10D, 64.5D, 0D), new Vec3(0D, 0D, 10D)).getFacing());
        assertEquals(EnumArmorFacing.TOP, first(body, new Vec3(10D, 70D, 10D), new Vec3(0D, -10D, 0D)).getFacing());
        // Facing south, east is to the left.
        assertEquals(EnumArmorFacing.LEFT, first(body, new Vec3(11.5D, 64.5D, 11.5D), new Vec3(-1D, 0D, 0D)).getFacing());
    }

    @Test
    void boxesStandWhereTheModelIsDrawnAtAnyYawAndScale()
    {
        PartBody body = body();
        for (float yaw : new float[]{0F, 37F, 90F, 180F, -120F})
        {
            for (float scale : new float[]{1F, 1.5F, 0.4F})
            {
                PartBodyPose pose = new PartBodyPose(ORIGIN, yaw, scale, MountAim.REST);
                body.setPose(pose);
                // The pod sits on the right of the model only: a mirrored or misturned frame would miss it.
                Vec3 top = drawn(new Vec3(2.5D, 1D, 0D), pose);
                PartBodyHit hit = first(body, top.add(0D, 5D, 0D), new Vec3(0D, -10D, 0D));
                assertEquals(EnumDriveablePart.RIGHT_SKIRT, hit.getPart(), "yaw " + yaw + " scale " + scale);
                assertEquals(top.y, hit.getHitPosition().y, 1.0E-4D);
                Vec3 mirrored = drawn(new Vec3(-2.5D, 1D, 0D), pose);
                assertTrue(body.trace(null, mirrored.add(0D, 5D, 0D), new Vec3(0D, -3.9D, 0D)).isEmpty(), "yaw " + yaw + " scale " + scale);
            }
        }
    }

    @Test
    void turretBoxesTurnWithTheTurretToTheRight()
    {
        PartBody body = body();
        PartBodyPose rest = new PartBodyPose(ORIGIN, 0F, 1F, MountAim.REST);
        assertEquals(EnumDriveablePart.TURRET, first(body, drawn(new Vec3(0D, 6D, -2.5D), rest), new Vec3(0D, -10D, 0D)).getPart());
        PartBodyPose turned = new PartBodyPose(ORIGIN, 0F, 1F, new MountAim(90F, 0F));
        body.setPose(turned);
        // Ahead of the pivot at rest, to the right of it once the turret has turned right.
        assertEquals(EnumDriveablePart.TURRET, first(body, drawn(new Vec3(2.5D, 6D, 0D), turned), new Vec3(0D, -10D, 0D)).getPart());
        assertTrue(body.trace(null, drawn(new Vec3(0D, 6D, -2.5D), turned), new Vec3(0D, -4.4D, 0D)).isEmpty());
    }

    @Test
    void aBrokenPartLetsRoundsThroughUnlessBrokenPartsStaySolid()
    {
        PartBody body = body();
        Vec3 above = new Vec3(7.5D, 70D, 10D);
        Vec3 down = new Vec3(0D, -10D, 0D);
        assertEquals(EnumDriveablePart.RIGHT_SKIRT, first(body, above, down).getPart());
        assertEquals(50F, body.damage("rightSkirt", 80F, false), 1.0E-5F);
        assertTrue(body.getPart("rightSkirt").orElseThrow().destroyed());
        assertTrue(body.trace(null, above, down).isEmpty());
        // Its box can still be pointed at, as the debug readout does.
        assertEquals(EnumDriveablePart.RIGHT_SKIRT, ((PartBodyHit) body.trace(null, above, down, true).get(0)).getPart());
        body.setBrokenPartsSolid(true);
        assertEquals(EnumDriveablePart.RIGHT_SKIRT, first(body, above, down).getPart());
        // A broken part takes nothing more.
        assertEquals(0F, body.damage("rightSkirt", 10F, false), 0F);
    }

    @Test
    void aBrokenPartTakesAlongWhatHangsOnItAndTheCoreTakesEverything()
    {
        PartBody body = body();
        assertEquals(230F, body.getMaxHealth(), 1.0E-5F);
        // The wing takes its wheel with it.
        assertEquals(40F, body.damage("LEFTWING", 30F, false), 1.0E-5F);
        assertTrue(body.getPart("leftWingWheel").orElseThrow().destroyed());
        assertEquals(190F, body.getHealth(), 1.0E-5F);
        assertEquals(25F, body.damage("core", 25F, false), 1.0E-5F);
        assertTrue(body.isIntact(EnumDriveablePart.CORE));
        assertEquals(165F, body.damage("core", 500F, false), 1.0E-5F);
        assertEquals(0F, body.getHealth(), 0F);
        assertTrue(body.getParts().stream().allMatch(PartState::destroyed));
    }

    @Test
    void hitPointsSurviveSavingAndRaisingThemPutsAPartBack()
    {
        PartBody body = body();
        body.damage("turret", 15F, false);
        body.damage("rightSkirt", 60F, false);
        PartBody loaded = body();
        loaded.loadState(body.saveState());
        assertEquals(25F, loaded.getPart("turret").orElseThrow().health(), 1.0E-5F);
        assertTrue(loaded.getPart("rightSkirt").orElseThrow().destroyed());
        assertEquals(body.signature(), loaded.signature());
        loaded.setHealth("rightSkirt", 5F);
        assertFalse(loaded.getPart("rightSkirt").orElseThrow().destroyed());
        loaded.restore();
        assertEquals(loaded.getMaxHealth(), loaded.getHealth(), 0F);
        assertNotEquals(body.signature(), loaded.signature());
    }

    @Test
    void anNpcsOwnBoxReplacesTheDefinitionsAndKeepsTheShareOfHitPointsLeft()
    {
        PartBody body = body();
        body.damage("turret", 10F, false);
        body.setBox("turret", new PartBox(new AABB(4D, 0D, 4D, 5D, 1D, 5D), 80F, 7F));
        PartState turret = body.getPart("turret").orElseThrow();
        assertTrue(turret.custom());
        assertEquals(60F, turret.health(), 1.0E-4F);
        assertEquals(40F, turret.definition().health(), 0F);
        assertEquals(7F, body.penetrationResistance(EnumDriveablePart.TURRET), 0F);
        assertEquals(270F, body.getMaxHealth(), 1.0E-4F);
        body.setBox("turret", null);
        assertFalse(body.getPart("turret").orElseThrow().custom());
        assertEquals(30F, body.getPart("turret").orElseThrow().health(), 1.0E-4F);
    }

    @Test
    void aDisabledPartIsNeitherHitNorCounted()
    {
        PartBody body = body();
        body.setEnabled("rightSkirt", false);
        assertTrue(body.trace(null, new Vec3(7.5D, 70D, 10D), new Vec3(0D, -10D, 0D)).isEmpty());
        assertEquals(180F, body.getMaxHealth(), 1.0E-5F);
        assertEquals(0F, body.damage("rightSkirt", 10F, false), 0F);
        assertFalse(body.getPart("rightSkirt").orElseThrow().enabled());
        assertEquals("core", body.getParts().get(0).name());
        assertEquals(5, body.getParts().size());
    }

    @Test
    void aBlastFindsTheNearestSurfaceOfEveryStandingPart()
    {
        PartBody body = body();
        // West of the hull, where the pod on the model's right is nearest.
        List<VehicleExplosionTarget> targets = body.explosionTargets(new Vec3(5D, 64.5D, 10D));
        assertEquals(EnumDriveablePart.RIGHT_SKIRT, targets.get(0).part());
        assertEquals(EnumArmorFacing.RIGHT, targets.get(0).facing());
        assertEquals(2D, targets.get(0).distanceMeters(), 1.0E-5D);
        assertEquals(7D, targets.get(0).surfaceWorldPosition().x, 1.0E-5D);
        body.damage("rightSkirt", 50F, false);
        assertTrue(body.explosionTargets(new Vec3(5D, 64.5D, 10D)).stream().noneMatch(target -> target.part() == EnumDriveablePart.RIGHT_SKIRT));
    }

    @Test
    void theReachCoversEveryBoxAtTheScaleTheBodyIsDrawnAt()
    {
        PartBody body = body();
        double reach = body.reach();
        assertTrue(reach >= 6D && reach < 7D, "reach " + reach);
        body.setPose(new PartBodyPose(ORIGIN, 0F, 2F, MountAim.REST));
        assertEquals(reach * 2D, body.reach(), 1.0E-9D);
    }

    @Test
    void brokenPartsAreNoLongerDrawnButSwitchedOffAndBoxlessOnesAre()
    {
        PartBody body = body();
        assertTrue(body.isDrawn(EnumDriveablePart.TURRET));
        body.damage("turret", 1000F, false);
        assertFalse(body.isDrawn(EnumDriveablePart.TURRET), "a broken part is shot off the model");
        body.setEnabled("rightSkirt", false);
        assertTrue(body.isDrawn(EnumDriveablePart.RIGHT_SKIRT), "a part switched off as a hitbox is still drawn");
        assertTrue(body.isDrawn(EnumDriveablePart.BLADES), "a part the body has no box for is drawn as the model has it");
        assertTrue(body.enabledParts().stream().noneMatch(part -> part.getType() == EnumDriveablePart.RIGHT_SKIRT));
        assertEquals(EnumDriveablePart.CORE, body.enabledParts().get(0).getType(), "the core comes first");
        assertNull(body.partState(EnumDriveablePart.BLADES));
    }

    @Test
    void pointsInAPartStandWhereItsBoxIsDrawn()
    {
        PartBody body = body();
        Vec3 centre = body.partPointToWorld(Objects.requireNonNull(body.partState(EnumDriveablePart.CORE)), 0.5D, 0.5D, 0.5D);
        assertEquals(ORIGIN.x, centre.x, 1.0E-6D);
        assertEquals(ORIGIN.y + 0.5D, centre.y, 1.0E-6D);
        assertEquals(ORIGIN.z, centre.z, 1.0E-6D);
        Vec3 bottom = body.partPointToWorld(Objects.requireNonNull(body.partState(EnumDriveablePart.CORE)), 0.5D, 0D, 0.5D);
        assertEquals(ORIGIN.y, bottom.y, 1.0E-6D);
    }

    @Test
    void armorOverridesChangeKineticHeatAndBlastPlatesWithoutChangingTheModelOrOtherParts()
    {
        DriveableType type = mock(DriveableType.class);
        VehicleArmorSpec spec = new VehicleArmorSpec(Map.of(EnumArmorFacing.FRONT, new ArmorPlate(40F, 30F, 80F)), Map.of(EnumArmorFacing.FRONT, new ArmorPlate(100F, 20F, 200F)), Map.of());
        ResolvedVehicleArmor model = VehicleArmorResolver.resolve(spec, List.of(EnumDriveablePart.CORE, EnumDriveablePart.TURRET));
        when(type.getResolvedArmor()).thenReturn(model);
        Map<EnumDriveablePart, CollisionBox> boxes = Map.of(EnumDriveablePart.CORE, box(100F, -1F, 0F, -2F, 2F, 1F, 4F), EnumDriveablePart.TURRET, box(40F, -1F, 1F, -1F, 2F, 1F, 2F));
        PartBody body = new PartBody(type, boxes, Vec3.ZERO, Vec3.ZERO);
        int original = body.signature();
        assertEquals(new ArmorPlate(100F, 20F, 200F), body.armor().plate(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT).authored());
        assertEquals(new PartArmor(40F, 80F), body.getModelArmor("core").orElseThrow());
        assertEquals(new PartArmor(100F, 200F), body.getModelArmor("TURRET").orElseThrow());
        assertTrue(body.getModelArmor("missing").isEmpty());
        body.setArmor("turret", new PartArmor(120F, 600F));
        assertEquals(new PartArmor(100F, 200F), body.getModelArmor("turret").orElseThrow());
        assertNotEquals(original, body.signature());
        for (EnumArmorFacing face : EnumArmorFacing.values())
        {
            assertEquals(new ArmorPlate(120F, 0F, 600F), body.armor().plate(EnumDriveablePart.TURRET, face).authored());
            Vec3 direction = face.outwardNormal().scale(-1D);
            assertEquals(120F, body.armor().resolveHit(EnumDriveablePart.TURRET, face, direction, 80D, false).effectiveArmorMm(), 0.001F);
            assertEquals(600F, body.armor().resolveHit(EnumDriveablePart.TURRET, face, direction, 80D, true).effectiveArmorMm(), 0.001F);
        }
        assertEquals(new ArmorPlate(40F, 30F, 80F), body.armor().plate(EnumDriveablePart.CORE, EnumArmorFacing.FRONT).authored());
        assertEquals(new ArmorPlate(100F, 20F, 200F), model.plate(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT).authored());
        body.setArmor("turret", new PartArmor(0F, 0F));
        assertEquals(ArmorPlate.UNARMOURED, body.armor().plate(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT).authored());
        body.setArmor("turret", null);
        assertEquals(original, body.signature());
        assertEquals(model.plate(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT), body.armor().plate(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT));
        body.setArmor("missing", new PartArmor(20F, 40F));
        assertTrue(body.getArmor("missing").isEmpty());
        assertEquals(original, body.signature());
    }
}
