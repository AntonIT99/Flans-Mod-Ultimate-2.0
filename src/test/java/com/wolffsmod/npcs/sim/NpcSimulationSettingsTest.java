package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.network.VehicleStatePacket;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import static org.junit.jupiter.api.Assertions.*;

class NpcSimulationSettingsTest
{
    @Test
    void npcsSavedBeforeTheseSettingsKeepEveryDefault()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.ALIGN_TOLERANCE, 20);
        tuning.load(new CompoundTag());
        for (Value value : Value.values())
            assertEquals(value.fallback(), tuning.get(value), value.name());
        assertEquals(NpcTuning.Terrain.DEFAULT, tuning.terrain());
        assertEquals(NpcTuning.Sight.NORMAL, tuning.sight());
        assertEquals(NpcTuning.Tactic.CUSTOM_NPCS, tuning.tactic());
    }

    @Test
    void valuesAreClampedSavedOnlyWhenChangedAndSurviveCloning()
    {
        NpcTuning tuning = new NpcTuning();
        int untouched = tuning.signature();
        tuning.set(Value.ALIGN_TOLERANCE, 999);
        tuning.set(Value.WATER_SPEED, -4);
        tuning.set(Value.TERRAIN, NpcTuning.Terrain.WATERCRAFT.ordinal());
        tuning.set(Value.SIGHT, 99);
        assertEquals(Value.ALIGN_TOLERANCE.maximum(), tuning.get(Value.ALIGN_TOLERANCE));
        assertEquals(0, tuning.get(Value.WATER_SPEED));
        assertEquals(NpcTuning.Sight.SEE_AND_FIRE_THROUGH, tuning.sight());
        assertNotEquals(untouched, tuning.signature());
        CompoundTag root = new CompoundTag();
        root.putInt("MaxHealth", 80);
        tuning.save(root);
        CompoundTag saved = root.getCompound(NpcTuning.NBT_KEY);
        assertEquals(3, saved.size(), "only what differs from the defaults is written");
        saved.putInt("REMOVED_VALUE", 5);
        saved.putString(Value.FIRE_LIMIT.name(), "seven");
        NpcTuning clone = new NpcTuning();
        clone.load(root.copy());
        assertEquals(tuning.signature(), clone.signature());
        assertEquals(NpcTuning.Terrain.WATERCRAFT, clone.terrain());
        assertEquals(Value.FIRE_LIMIT.fallback(), clone.get(Value.FIRE_LIMIT));
        assertEquals(80, root.getInt("MaxHealth"));
    }

    @Test
    void landSpeedAllowsForWalkingSpeedGrowingWithTheSquare()
    {
        // A Custom NPCs walking speed of 5 settles near 2.7 blocks per second, so 27 tenths asks for no change.
        assertEquals(1F, NpcMobility.landFactor(27, 5), 0.01F);
        assertEquals(2F, NpcMobility.landFactor(54, 5), 0.02F);
        // Twice the walking speed is four times as fast, so the same setting asks for a quarter of the push.
        assertEquals(NpcMobility.landFactor(27, 5) / 4F, NpcMobility.landFactor(27, 10), 1.0E-4F);
        assertEquals(20F, NpcMobility.landFactor(600, 0));
    }

    @Test
    void waterSpeedSetsTheSpeedTheCraftSettlesAt()
    {
        float retention = NpcMobility.waterSlowDown(60, 0.25D, 0.8F);
        // Each tick the craft is pushed and keeps this share: it settles where push and loss balance.
        double push = 0.02D * 0.25D;
        double settled = push * retention / (1D - retention);
        assertEquals(60 / 200D, settled, 1.0E-4D);
        // A craft that is not trying to move brakes as any swimmer does.
        assertEquals(0.8F, NpcMobility.waterSlowDown(60, 0D, 0.8F));
        assertEquals(0.995F, NpcMobility.waterSlowDown(600, 1.0E-3D, 0.8F), 1.0E-6F);
    }

    @Test
    void pathSearchesReachTheirGoalPlusAMarginButNeverFurtherThanBefore()
    {
        assertEquals(64F, NpcMobility.searchRange(40D, 24, 512F));
        assertEquals((float) NpcMobility.MIN_SEARCH_RANGE, NpcMobility.searchRange(3D, 8, 512F));
        assertEquals(16F, NpcMobility.searchRange(3D, 8, 16F));
        assertEquals(512F, NpcMobility.searchRange(700D, 24, 512F));
    }

    @Test
    void searchIntervalsRiseEvenlyFromTheNearBandToTheFarthest()
    {
        assertEquals(10, NpcTargetSearch.interval(0, 10, 60));
        assertEquals(60, NpcTargetSearch.interval(5, 10, 60));
        assertEquals(30, NpcTargetSearch.interval(2, 10, 60));
        assertEquals(1, NpcTargetSearch.interval(3, 1, 1));
    }

    @Test
    void theStatePacketCarriesAnglesToHalfADegree()
    {
        VehicleStatePacket sent = new VehicleStatePacket(4711, -179.5F, 93.26F, -12.74F, new float[]{93.26F, -45F}, new float[]{-12.74F, 7.5F}, 300, 3);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        sent.write(buffer);
        VehicleStatePacket received = VehicleStatePacket.read(buffer);
        assertEquals(0, buffer.readableBytes());
        assertEquals(4711, received.entityId());
        assertEquals(-179.5F, received.hullYaw());
        assertEquals(93.5F, received.turretYaw());
        assertEquals(-12.5F, received.turretPitch());
        assertArrayEquals(new float[]{93.5F, -45F}, received.seatYaw());
        assertArrayEquals(new float[]{-12.5F, 7.5F}, received.seatPitch());
        assertEquals(300, received.shots());
        assertEquals(3, received.barrel());
    }
}
