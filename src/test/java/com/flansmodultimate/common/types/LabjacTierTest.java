package com.flansmodultimate.common.types;

import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LabjacTierTest
{
    private static final ContentPack PACK = new ContentPack("test", Path.of("build", "test-packs", "tier"));

    @Test
    void readsTierOnAnyType()
    {
        BulletType untiered = new BulletType();
        untiered.read(new TypeFile("untiered", EnumType.BULLET, PACK, List.of("ShortName untiered")));
        assertTrue(Float.isNaN(untiered.getTier()));

        BulletType tiered = new BulletType();
        tiered.read(new TypeFile("tiered", EnumType.BULLET, PACK, List.of("ShortName tiered", "Tier 1.5")));
        assertEquals(1.5F, tiered.getTier());

        VehicleType vehicle = new VehicleType();
        vehicle.load(new TypeFile("tieredVehicle", EnumType.VEHICLE, PACK, List.of("ShortName tieredVehicle", "Tier -0.5")));
        assertEquals(-0.5F, vehicle.getTier());
    }

    @Test
    void explosionPushScalesAgainstTheForkDefault()
    {
        VehicleType plain = new VehicleType();
        plain.load(new TypeFile("plainPush", EnumType.VEHICLE, PACK, List.of("ShortName plainPush")));
        assertEquals(1F, plain.getExplosionPushFactor());

        VehicleType submarine = new VehicleType();
        submarine.load(new TypeFile("subPush", EnumType.VEHICLE, PACK, List.of("ShortName subPush", "explosionPush 0.5")));
        assertEquals(2F, submarine.getExplosionPushFactor());
    }

    @Test
    void formatsTiersAsPacksWriteThem()
    {
        assertEquals("1", IFlanItem.formatTier(1F));
        assertEquals("1.5", IFlanItem.formatTier(1.5F));
        assertEquals("-0.25", IFlanItem.formatTier(-0.25F));
    }
}
