package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.PartBox;
import com.flansmodultimate.api.PartState;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.network.PartStatePacket;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NpcPartsTest
{
    private static final PartBox BOX = new PartBox(new AABB(0D, 0D, 0D, 1D, 1D, 1D), 10F, 1F);

    private static PartBody tank()
    {
        Map<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        boxes.put(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(100F, -1F, 0F, -2F, 2F, 1F, 4F, 2F, 0F));
        boxes.put(EnumDriveablePart.TURRET, CollisionBox.inWorldUnits(60F, -1F, 1F, -1F, 2F, 1F, 2F, 3F, 0F));
        boxes.put(EnumDriveablePart.LEFT_TRACK, CollisionBox.inWorldUnits(20F, -1.5F, 0F, -2F, 0.5F, 0.5F, 4F, 1F, 0F));
        boxes.put(EnumDriveablePart.RIGHT_TRACK, CollisionBox.inWorldUnits(20F, 1F, 0F, -2F, 0.5F, 0.5F, 4F, 1F, 0F));
        return new PartBody(null, boxes, Vec3.ZERO, Vec3.ZERO);
    }

    private static List<PartState> parts(Object... namesAndBroken)
    {
        List<PartState> parts = new ArrayList<>();
        for (int index = 0; index < namesAndBroken.length; index += 2)
        {
            boolean broken = (Boolean) namesAndBroken[index + 1];
            parts.add(new PartState((String) namesAndBroken[index], "", BOX, BOX, false, true, false, broken ? 0F : 10F, broken, false));
        }
        return parts;
    }

    @Test
    void healingIsSharedOutByWhatEachPartIsMissingAndRebuildsBrokenPartsOnlyWhenAsked()
    {
        PartBody body = tank();
        body.damage("core", 50F, false);
        body.damage("turret", 30F, false);
        body.damage("leftTrack", 20F, false);
        // 100 hit points are missing; 40 fill two fifths of every gap.
        NpcParts.repair(body, 40F, true);
        assertEquals(70F, body.getPart("core").orElseThrow().health(), 1.0E-4F);
        assertEquals(42F, body.getPart("turret").orElseThrow().health(), 1.0E-4F);
        assertEquals(8F, body.getPart("leftTrack").orElseThrow().health(), 1.0E-4F);
        assertFalse(body.getPart("leftTrack").orElseThrow().destroyed());
        body.damage("rightTrack", 20F, false);
        NpcParts.repair(body, 1000F, false);
        assertTrue(body.getPart("rightTrack").orElseThrow().destroyed());
        assertEquals(180F, body.getHealth(), 1.0E-4F);
        NpcParts.repair(body, 1000F, true);
        assertEquals(200F, body.getHealth(), 1.0E-4F);
    }

    @Test
    void healthLostByOtherMeansComesOffEveryStandingPartAlikeAndBreaksNone()
    {
        PartBody body = tank();
        body.damage("leftTrack", 20F, false);
        NpcParts.shrink(body, 0.25F);
        assertEquals(25F, body.getPart("core").orElseThrow().health(), 1.0E-4F);
        assertEquals(15F, body.getPart("turret").orElseThrow().health(), 1.0E-4F);
        assertEquals(5F, body.getPart("rightTrack").orElseThrow().health(), 1.0E-4F);
        assertTrue(body.getPart("leftTrack").orElseThrow().destroyed());
        NpcParts.shrink(body, 0F);
        assertFalse(body.getPart("core").orElseThrow().destroyed());
    }

    @Test
    void brokenRunningGearSlowsAVehicleAndABrokenTurretSilencesItsGun()
    {
        assertEquals(NpcPartEffects.NONE, NpcPartEffects.of(parts("core", false, "leftTrack", false, "rightTrack", false, "turret", false), FlanModelKind.VEHICLE));
        NpcPartEffects oneTrack = NpcPartEffects.of(parts("leftTrack", true, "rightTrack", false, "turret", true), FlanModelKind.VEHICLE);
        assertEquals(0.5F, oneTrack.mobility(), 1.0E-6F);
        assertTrue(oneTrack.gunDisabled());
        assertEquals(0F, NpcPartEffects.of(parts("leftTrack", true, "rightTrack", true), FlanModelKind.VEHICLE).mobility(), 0F);
        assertEquals(0.75F, NpcPartEffects.of(parts("frontLeftWheel", true, "frontRightWheel", false, "backLeftWheel", false, "backRightWheel", false), FlanModelKind.VEHICLE).mobility(), 1.0E-6F);
        // Half the engine rooms cost four tenths of the speed, the bow another tenth.
        assertEquals(0.5F, NpcPartEffects.of(parts("engineRoom1", true, "engineRoom2", false, "bow", true), FlanModelKind.VEHICLE).mobility(), 1.0E-6F);
    }

    @Test
    void aircraftAreShotDownAndMechasLoseTheirLegsAndArms()
    {
        assertTrue(NpcPartEffects.of(parts("leftWing", true, "rightWing", false), FlanModelKind.PLANE).shotDown());
        assertTrue(NpcPartEffects.of(parts("blades", true), FlanModelKind.PLANE).shotDown());
        NpcPartEffects landingGear = NpcPartEffects.of(parts("coreWheel", true, "leftWing", false), FlanModelKind.PLANE);
        assertFalse(landingGear.shotDown());
        assertEquals(1F, landingGear.mobility(), 0F);
        // A ground vehicle that happens to carry a wing part is not an aircraft.
        assertFalse(NpcPartEffects.of(parts("leftWing", true), FlanModelKind.VEHICLE).shotDown());
        NpcPartEffects mecha = NpcPartEffects.of(parts("hips", true, "leftArm", true, "rightArm", false), FlanModelKind.MECHA);
        assertEquals(0F, mecha.mobility(), 0F);
        assertFalse(mecha.gunDisabled());
        assertTrue(NpcPartEffects.of(parts("leftArm", true, "rightArm", true), FlanModelKind.MECHA).gunDisabled());
    }

    @Test
    void partsThatCannotBreakOrAreLeftOutHaveNoEffect()
    {
        PartBox structural = new PartBox(new AABB(0D, 0D, 0D, 1D, 1D, 1D), 0F, 1F);
        List<PartState> parts = List.of(new PartState("turret", "", structural, structural, false, true, true, 0F, false, false),
            new PartState("leftTrack", "", BOX, BOX, false, false, false, 0F, true, false), new PartState("rightTrack", "", BOX, BOX, false, true, false, 10F, false, false));
        assertEquals(NpcPartEffects.NONE, NpcPartEffects.of(parts, FlanModelKind.VEHICLE));
    }

    @Test
    void partStateReachesAClientWhoseBoxesDifferAsSharesOfWhatEachPartHolds()
    {
        PartBody server = tank();
        server.damage("turret", 15F, false);
        server.damage("leftTrack", 20F, false);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        PartStatePacket.of(7, server).write(buffer);
        PartStatePacket received = PartStatePacket.read(buffer);
        assertEquals(7, received.entityId());
        PartBody client = tank();
        // The client has not yet heard that the NPC's own turret holds twice as much.
        client.setBox("turret", new PartBox(new AABB(-1D, 1D, -1D, 1D, 2D, 1D), 120F, 3F));
        received.applyTo(client);
        assertEquals(90F, client.getPart("turret").orElseThrow().health(), 1.0E-4F);
        assertTrue(client.getPart("leftTrack").orElseThrow().destroyed());
        assertEquals(100F, client.getPart("core").orElseThrow().health(), 1.0E-4F);
    }
}
