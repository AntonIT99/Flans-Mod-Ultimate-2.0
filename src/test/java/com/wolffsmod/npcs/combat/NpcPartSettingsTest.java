package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.PartBox;
import com.flansmodultimate.api.PartState;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.driveables.parts.PartBody;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NpcPartSettingsTest
{
    private static PartBody body()
    {
        Map<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        boxes.put(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(100F, -1F, 0F, -2F, 2F, 1F, 4F, 2F, 0F));
        boxes.put(EnumDriveablePart.TURRET, CollisionBox.inWorldUnits(40F, -1F, 1F, -1F, 2F, 1F, 2F, 3F, 0F));
        boxes.put(EnumDriveablePart.LEFT_TRACK, CollisionBox.inWorldUnits(20F, -1.5F, 0F, -2F, 0.5F, 0.5F, 4F, 1F, 0F));
        return new PartBody(null, boxes, Vec3.ZERO, Vec3.ZERO);
    }

    @Test
    void npcsSavedBeforeTheseSettingsUseTheModelsParts()
    {
        NpcPartSettings settings = new NpcPartSettings();
        settings.load(new CompoundTag());
        assertTrue(settings.isEmpty());
        assertTrue(settings.get("turret").enabled());
        assertNull(settings.get("turret").box());
        PartBody body = body();
        int before = body.signature();
        settings.apply(body);
        assertEquals(before, body.signature());
    }

    @Test
    void changesSurviveSavingAndAreAppliedToTheBody()
    {
        NpcPartSettings settings = new NpcPartSettings();
        settings.setEnabled("leftTrack", false);
        settings.setBox("Turret", new PartBox(new AABB(-2D, 1D, -2D, 2D, 3D, 2D), 90F, 6F));
        CompoundTag npc = new CompoundTag();
        npc.putInt("MaxHealth", 80);
        settings.save(npc);
        NpcPartSettings clone = new NpcPartSettings();
        clone.load(npc.copy());
        assertEquals(settings.signature(), clone.signature());
        assertEquals(80, npc.getInt("MaxHealth"));
        PartBody body = body();
        clone.apply(body);
        PartState turret = body.getPart("turret").orElseThrow();
        assertTrue(turret.custom());
        assertEquals(90F, turret.box().health(), 0F);
        assertEquals(6F, turret.box().penetrationResistance(), 0F);
        assertEquals(4D, turret.box().bounds().getXsize(), 1.0E-6D);
        assertFalse(body.getPart("leftTrack").orElseThrow().enabled());
        assertEquals(190F, body.getMaxHealth(), 1.0E-5F);
    }

    @Test
    void returningAPartToTheModelForgetsItAndClearingForgetsEverything()
    {
        NpcPartSettings settings = new NpcPartSettings();
        int untouched = settings.signature();
        settings.setBox("core", new PartBox(new AABB(0D, 0D, 0D, 1D, 1D, 1D), 5F, 1F));
        settings.setEnabled("core", false);
        assertNotEquals(untouched, settings.signature());
        settings.setBox("core", null);
        assertFalse(settings.isEmpty());
        settings.setEnabled("CORE", true);
        assertTrue(settings.isEmpty());
        assertEquals(untouched, settings.signature());
        settings.setEnabled("turret", false);
        PartBody body = body();
        settings.apply(body);
        settings.clear();
        settings.apply(body);
        assertTrue(body.getPart("turret").orElseThrow().enabled());
    }

    @Test
    void boxesAreKeptWithinTheLimitsAndBrokenSavesAreIgnored()
    {
        NpcPartSettings settings = new NpcPartSettings();
        settings.setBox("core", new PartBox(new AABB(-9000D, 0D, 0D, 9000D, 1D, 1D), Float.MAX_VALUE, Float.POSITIVE_INFINITY));
        PartBox box = settings.get("core").box();
        assertNotNull(box);
        assertEquals(-NpcPartSettings.MAX_EXTENT, box.bounds().minX, 0D);
        assertEquals(NpcPartSettings.MAX_EXTENT, box.bounds().getXsize(), 0D);
        assertEquals(NpcPartSettings.MAX_HEALTH, box.health(), 0F);
        assertEquals(0F, box.penetrationResistance(), 0F);
        CompoundTag root = new CompoundTag();
        settings.save(root);
        // A box that lost one of its corners is the model's again.
        root.getCompound(NpcPartSettings.NBT_KEY).getCompound("core").remove("MaxZ");
        settings.load(root);
        assertNull(settings.get("core").box());
    }
}
